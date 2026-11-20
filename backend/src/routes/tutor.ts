import type {
  ApiTutorGroundedSource,
  ApiTutorRequest,
  ApiTutorResponse,
} from '../../../shared-contracts/types';
import { sha256Hex } from '../crypto/auth';
import { getChapterContent, listChapters, safeJsonParse } from '../db/repository';
import { HttpError, jsonEnvelope, jsonError, type Env } from '../env';
import { PRIVATE_NO_STORE_CACHE_CONTROL } from '../middleware/cache';
import { enforceRateLimit, getClientIp } from '../middleware/security';
import {
  currentUtcDayYmd,
  getCapacityPolicy,
  getTodayUsageSnapshot,
  recordUsageDelta,
} from '../services/usageTelemetry';
import { resolveStudentFromRequest } from './studentAuthSync';

/**
 * Curriculum-grounded AI Personal Tutor endpoint (`POST /v1/tutor/ask`).
 * Free-tier Cost & Quota Protections:
 * - Provider API keys live exclusively in Worker secrets (`GEMINI_API_KEY` or `AI_PROVIDER_API_KEY`).
 * - Response cache (`ai_tutor_cache`) deduplicates identical syllabus questions with 0 AI calls.
 * - Per-student daily cap + global daily free-tier cap + critical threshold auto-degradation.
 * - Concise token limit (`maxOutputTokens: 700`) and grounded D1 context.
 * - Graceful fallback HTTP 429 / 503 messages keeping offline Notes, MCQs, and Flashcards unaffected.
 */
export async function handleTutorRoutes(
  request: Request,
  path: string,
  env: Env,
  headers: Record<string, string>,
  fetchFn: typeof fetch = fetch
): Promise<Response | null> {
  const method = request.method.toUpperCase();
  if (method !== 'POST' || path !== '/v1/tutor/ask') {
    return null;
  }

  const privateHeaders = {
    ...headers,
    'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
  };

  const body = (await request.json().catch(() => null)) as
    | (Partial<ApiTutorRequest> & { guestDeviceId?: string })
    | null;
  if (!body || !body.prompt?.trim()) {
    throw new HttpError(400, 'INVALID_TUTOR_PROMPT', 'A non-empty question or prompt is required.');
  }

  const classLevel = [9, 10, 11, 12].includes(Number(body.classLevel)) ? Number(body.classLevel) : 9;
  const boardCode = body.boardCode || 'KPK';
  const language = body.language === 'ur' ? 'ur' : 'en';
  const action = body.action || 'ask';
  const rawPrompt = body.prompt.trim().slice(0, 1200);
  const normalizedPrompt = rawPrompt.toLowerCase().replace(/\s+/g, ' ');

  const apiKey = (env.GEMINI_API_KEY || env.AI_PROVIDER_API_KEY || '').trim();
  if (!apiKey) {
    return jsonError(
      503,
      'AI_PROVIDER_UNCONFIGURED',
      'AI Tutor is paused because GEMINI_API_KEY is not configured. Offline chapter Notes, Formulas, MCQs, and Flashcards remain fully available.',
      env,
      privateHeaders
    );
  }

  const policy = await getCapacityPolicy(env.DB);
  const now = new Date();
  const nowIso = now.toISOString();
  const todayYmd = currentUtcDayYmd(now);

  // 1. Zero-cost D1 semantic/prompt cache lookup before consuming burst or quota budget
  const cacheKey = await sha256Hex(
    `${boardCode}|${classLevel}|${body.subjectId || ''}|${body.chapterId || ''}|${action}|${language}|${normalizedPrompt}`
  );

  try {
    const cachedRow = await env.DB.prepare(
      `SELECT answer, model, grounded_sources_json
       FROM ai_tutor_cache
       WHERE cache_key = ? AND expires_at_iso > ?
       LIMIT 1`
    )
      .bind(cacheKey, nowIso)
      .first<{ answer: string; model: string; grounded_sources_json: string }>();

    if (cachedRow) {
      await env.DB.prepare(
        'UPDATE ai_tutor_cache SET hit_count = hit_count + 1 WHERE cache_key = ?'
      )
        .bind(cacheKey)
        .run()
        .catch(() => undefined);

      recordUsageDelta({ aiCacheHits: 1 });
      const cachedResp: ApiTutorResponse = {
        available: true,
        provider: 'gemini',
        model: cachedRow.model,
        answer: cachedRow.answer,
        groundedSources: safeJsonParse<ApiTutorGroundedSource[]>(
          cachedRow.grounded_sources_json,
          []
        ),
        generatedAtIso: nowIso,
      };
      return jsonEnvelope(cachedResp, env, 200, {
        ...privateHeaders,
        'X-Imtihan-Ai-Cache': 'HIT',
      });
    }
  } catch {
    // Cache read failure falls through safely to live evaluation.
  }

  // 2. Resolve student key safely for per-student quota & burst throttling
  const resolvedStudent = await resolveStudentFromRequest(request, env).catch(() => null);
  const studentKey =
    resolvedStudent?.studentId ||
    (body.guestDeviceId?.trim() ? `guest:${body.guestDeviceId.trim()}` : `ip:${getClientIp(request)}`);

  await enforceRateLimit(env, `tutor-burst:${studentKey}`, 6, 60_000);

  // 3. Enforce Global Daily Free-Tier Cap & Auto-Degrade Threshold
  const usage = await getTodayUsageSnapshot(env.DB, todayYmd);
  const globalCriticalLimit = policy.autoDegradeAiAtCritical
    ? Math.floor((policy.aiTutorDailyGlobalCap * policy.criticalThresholdPct) / 100)
    : policy.aiTutorDailyGlobalCap;

  if (policy.aiTutorDailyGlobalCap <= 0 || usage.aiCallsToday >= globalCriticalLimit) {
    recordUsageDelta({ aiBlockedQuota: 1 });
    return jsonError(
      429,
      'AI_GLOBAL_QUOTA_REACHED',
      'AI Tutor has paused temporarily to protect the daily free-tier quota. Continue studying seamlessly with offline Chapter Notes, MCQs, and Flashcards.',
      env,
      privateHeaders
    );
  }

  // 4. Enforce Per-Student Daily Cap
  const studentRow = await env.DB.prepare(
    'SELECT request_count FROM ai_student_daily_usage WHERE student_key = ? AND day_ymd = ? LIMIT 1'
  )
    .bind(studentKey, todayYmd)
    .first<{ request_count: number }>();

  if (
    policy.aiTutorDailyPerStudentCap <= 0 ||
    (studentRow?.request_count ?? 0) >= policy.aiTutorDailyPerStudentCap
  ) {
    recordUsageDelta({ aiBlockedQuota: 1 });
    return jsonError(
      429,
      'AI_STUDENT_QUOTA_REACHED',
      `Daily limit of ${policy.aiTutorDailyPerStudentCap} AI Tutor questions reached for today. Review Chapter Notes, Solved Examples, or Mistake Master while your quota resets at UTC midnight.`,
      env,
      privateHeaders
    );
  }

  // 5. Build grounded D1 context from published notes/chapters
  const groundedSources: ApiTutorGroundedSource[] = [];
  const contextSnippets: string[] = [];

  if (body.chapterId) {
    const content = await getChapterContent(env.DB, body.chapterId, 'detailed_notes', true);
    if (content) {
      for (const blk of content.blocks.slice(0, 8)) {
        if (blk.type === 'heading' || blk.type === 'subheading' || blk.type === 'paragraph') {
          contextSnippets.push(blk.text);
        } else if (blk.type === 'definition') {
          contextSnippets.push(
            `Definition (${blk.term}): ${blk.definition}${blk.formulaHint ? ` [${blk.formulaHint}]` : ''}`
          );
          groundedSources.push({ type: 'definition', title: blk.term, referenceId: blk.id });
        } else if (blk.type === 'math_expression') {
          contextSnippets.push(`Formula: ${blk.plainNotation} (${blk.caption || ''})`);
          groundedSources.push({
            type: 'formula',
            title: blk.caption || blk.plainNotation,
            referenceId: blk.id,
          });
        } else if (blk.type === 'exam_point') {
          contextSnippets.push(`Board Exam Points: ${blk.points.join(' | ')}`);
        } else if (blk.type === 'solved_question') {
          contextSnippets.push(`Solved Question: ${blk.question} -> ${blk.answerSteps.join(' ')}`);
        }
      }
      groundedSources.unshift({
        type: 'chapter_note',
        title: `Published ${content.version} notes (${content.authorOrSource})`,
        referenceId: content.chapterId,
      });
    }
  } else if (body.subjectId) {
    const chapters = await listChapters(env.DB, body.subjectId);
    const published = chapters.filter((c) => c.publishStatus === 'published').slice(0, 6);
    for (const ch of published) {
      contextSnippets.push(`Ch ${ch.chapterNumber} ${ch.title}: ${ch.subtitle}`);
      groundedSources.push({
        type: 'syllabus',
        title: `Ch ${ch.chapterNumber}: ${ch.title}`,
        referenceId: ch.id,
      });
    }
  }

  const model = (env.AI_PROVIDER_MODEL || 'gemini-4-hydrogen').trim();
  const baseUrl = (
    env.AI_PROVIDER_BASE_URL || 'https://generativelanguage.googleapis.com/v1beta'
  ).replace(/\/+$/, '');
  const endpoint = `${baseUrl}/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(apiKey)}`;

  const actionGuidance: Record<ApiTutorRequest['action'], string> = {
    ask: 'Answer the student question accurately and clearly with board-exam clarity.',
    explain_simple: 'Explain this concept in simple everyday language suitable for a KPK Board student.',
    step_by_step: 'Provide a numbered step-by-step derivation or numerical solution with units.',
    important_questions:
      'List the most likely KPK BISE short and long board exam questions for this topic with brief answer hints.',
    quick_summary: 'Summarize the key definitions, formulas, and high-yield exam points concisely.',
  };

  const langNote =
    language === 'ur'
      ? 'Respond in clear Urdu (with standard English scientific/mathematical symbols where helpful).'
      : 'Respond in clear English.';

  const systemPrompt = [
    `You are the Imtihan Pro Curriculum Tutor for ${boardCode} Board Class ${classLevel} students in Khyber Pakhtunkhwa, Pakistan.`,
    langNote,
    actionGuidance[action] || actionGuidance.ask,
    contextSnippets.length > 0
      ? `Ground your explanation strictly in the verified syllabus context below:\n${contextSnippets.join('\n')}`
      : 'If no chapter notes are attached, state assumptions clearly and adhere to standard KPK Textbook Board curriculum.',
  ].join('\n\n');

  const upstream = await fetchFn(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [
        {
          role: 'user',
          parts: [{ text: `${systemPrompt}\n\nStudent Request:\n${rawPrompt}` }],
        },
      ],
      generationConfig: {
        temperature: 0.35,
        maxOutputTokens: 700,
      },
    }),
  });

  if (upstream.status === 429) {
    recordUsageDelta({ aiBlockedQuota: 1 });
    return jsonError(
      429,
      'AI_UPSTREAM_QUOTA',
      'Gemini free-tier rate limit reached momentarily. Use offline Chapter Notes & Flashcards or try again in a minute.',
      env,
      privateHeaders
    );
  }

  if (!upstream.ok) {
    const errText = await upstream.text().catch(() => '');
    throw new HttpError(
      502,
      'AI_PROVIDER_ERROR',
      `AI Tutor provider returned HTTP ${upstream.status}: ${errText.slice(0, 160)}`
    );
  }

  const payload = (await upstream.json()) as {
    candidates?: Array<{
      content?: { parts?: Array<{ text?: string }> };
    }>;
  };
  const answer =
    payload.candidates?.[0]?.content?.parts
      ?.map((p) => p.text || '')
      .join('\n')
      .trim() || '';

  if (!answer) {
    throw new HttpError(502, 'AI_EMPTY_RESPONSE', 'AI Tutor provider returned an empty response.');
  }

  const expiresAtIso = new Date(now.getTime() + policy.aiCacheTtlHours * 3600_000).toISOString();

  await env.DB.batch([
    env.DB.prepare(
      `INSERT INTO ai_tutor_cache (
        cache_key, board_code, class_level, subject_id, chapter_id,
        action, language, normalized_prompt, answer, model,
        grounded_sources_json, hit_count, created_at_iso, expires_at_iso
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
      ON CONFLICT(cache_key) DO UPDATE SET
        answer = excluded.answer,
        model = excluded.model,
        grounded_sources_json = excluded.grounded_sources_json,
        expires_at_iso = excluded.expires_at_iso`
    ).bind(
      cacheKey,
      boardCode,
      classLevel,
      body.subjectId ?? null,
      body.chapterId ?? null,
      action,
      language,
      normalizedPrompt,
      answer,
      model,
      JSON.stringify(groundedSources),
      nowIso,
      expiresAtIso
    ),
    env.DB.prepare(
      `INSERT INTO ai_student_daily_usage (student_key, day_ymd, request_count, updated_at_iso)
       VALUES (?, ?, 1, ?)
       ON CONFLICT(student_key, day_ymd) DO UPDATE SET
         request_count = request_count + 1,
         updated_at_iso = excluded.updated_at_iso`
    ).bind(studentKey, todayYmd, nowIso),
  ]);

  recordUsageDelta({ aiRequests: 1 });

  const resp: ApiTutorResponse = {
    available: true,
    provider: 'gemini',
    model,
    answer,
    groundedSources,
    generatedAtIso: nowIso,
  };

  return jsonEnvelope(resp, env, 200, {
    ...privateHeaders,
    'X-Imtihan-Ai-Cache': 'MISS',
  });
}
