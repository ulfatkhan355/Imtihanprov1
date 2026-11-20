import {
  getStudyGroupsForClass,
  gradeQuizSubmission,
  KPK_SUB_BOARDS,
  STUDY_GROUPS_CATALOG,
} from '../../../shared-contracts/rules';
import type {
  ApiEducationalContent,
  ApiExamSessionPayload,
  ApiExamStartRequest,
  ApiPublicRuntimeConfig,
  ApiQuizSubmissionRequest,
  ClassLevel,
  SignedDownloadUrlResponse,
  StudyGroupCode,
} from '../../../shared-contracts/types';
import {
  getAdPlacementPolicy,
  getChapterContent,
  getPdfById,
  listAcademicClasses,
  listBoards,
  listChapterMcqs,
  listChapters,
  listExamPatterns,
  listMcqsFiltered,
  listPastPapers,
  listPdfMetadata,
  listSubjects,
  listSubjectivePromptsForExam,
  safeJsonParse,
  searchCatalog,
  stripMcqAnswerKey,
} from '../db/repository';
import { HttpError, jsonEnvelope, type D1PreparedStatement, type Env } from '../env';
import {
  buildCacheableEnvelopeResponse,
  ifNoneMatchMatches,
  PRIVATE_NO_STORE_CACHE_CONTROL,
  tryMatchWorkerEdgeCache,
  tryPutWorkerEdgeCache,
} from '../middleware/cache';
import { getCapacityPolicy, getTodayUsageSnapshot, recordUsageDelta } from '../services/usageTelemetry';
import {
  BackblazeB2Client,
  generateB2PresignedGetUrl,
  isB2Configured,
  resolveB2Config,
} from '../storage/backblazeB2';
import { recordStudentActivityDay, resolveStudentFromRequest } from './studentAuthSync';

async function respondWithPublicEdgeCache<T>(
  request: Request,
  env: Env,
  headers: Record<string, string>,
  ttlSeconds: number,
  loader: () => Promise<T>
): Promise<Response> {
  const edgeHit = await tryMatchWorkerEdgeCache(request);
  if (edgeHit) {
    recordUsageDelta({ edgeCacheHits: 1 });
    const cachedEtag = edgeHit.headers.get('ETag') || '';
    const mergedHeaders: Record<string, string> = {
      ...headers,
      'Cache-Control': edgeHit.headers.get('Cache-Control') || `public, max-age=300, s-maxage=${ttlSeconds}`,
      ...(cachedEtag ? { ETag: cachedEtag } : {}),
      'X-Imtihan-Cache': 'EDGE-HIT',
    };
    if (cachedEtag && ifNoneMatchMatches(request, cachedEtag)) {
      return new Response(null, { status: 304, headers: mergedHeaders });
    }
    return new Response(edgeHit.body, {
      status: 200,
      headers: {
        ...mergedHeaders,
        'Content-Type': 'application/json; charset=utf-8',
      },
    });
  }

  const data = await loader();
  const { response, notModified } = await buildCacheableEnvelopeResponse(
    request,
    data,
    env,
    headers,
    ttlSeconds
  );
  if (notModified) {
    recordUsageDelta({ edgeCacheHits: 1 });
    return response;
  }
  await tryPutWorkerEdgeCache(request, response);
  return response;
}

export async function handlePublicStudentRoutes(
  request: Request,
  path: string,
  url: URL,
  env: Env,
  headers: Record<string, string>,
  fetchFn: typeof fetch = fetch
): Promise<Response | null> {
  const method = request.method.toUpperCase();

  if (method === 'GET' && path === '/v1/health') {
    const start = Date.now();
    await env.DB.prepare('SELECT 1 AS ok').first();
    const latencyMs = Date.now() - start;
    const b2 = resolveB2Config(env);
    return jsonEnvelope(
      {
        status: 'healthy',
        worker: 'imtihan-pro-api',
        d1Connected: true,
        d1LatencyMs: latencyMs,
        b2Bucket: b2.bucketName,
        b2Configured: isB2Configured(env),
      },
      env,
      200,
      { ...headers, 'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL }
    );
  }

  if (method === 'GET' && path === '/v1/config') {
    return respondWithPublicEdgeCache(request, env, headers, 1800, async () => {
      const adPlacement = await getAdPlacementPolicy(env.DB);
      const aiTutorConfigured = Boolean((env.GEMINI_API_KEY || env.AI_PROVIDER_API_KEY || '').trim());
      const cfg: ApiPublicRuntimeConfig = {
        defaultBoardCode: 'KPK',
        defaultClassLevel: 11,
        availableClassLevels: [11],
        defaultStudyGroup: 'PRE_MEDICAL',
        kpkSubBoards: KPK_SUB_BOARDS,
        studyGroups: STUDY_GROUPS_CATALOG,
        adPlacement,
        aiTutorConfigured,
      };
      return cfg;
    });
  }

  if (method === 'GET' && path === '/v1/boards') {
    return respondWithPublicEdgeCache(request, env, headers, 86400, () => listBoards(env.DB));
  }

  if (method === 'GET' && path === '/v1/study-groups') {
    return respondWithPublicEdgeCache(request, env, headers, 86400, async () => {
      const clsRaw = url.searchParams.get('classLevel');
      const cls = clsRaw ? Number.parseInt(clsRaw, 10) : null;
      if (cls && [9, 10, 11, 12].includes(cls)) {
        return getStudyGroupsForClass(cls as ClassLevel);
      }
      return STUDY_GROUPS_CATALOG;
    });
  }

  if (method === 'GET' && path === '/v1/classes') {
    const board = url.searchParams.get('board') || undefined;
    return respondWithPublicEdgeCache(request, env, headers, 3600, () =>
      listAcademicClasses(env.DB, board)
    );
  }

  if (method === 'GET' && path === '/v1/subjects') {
    const boardCode = url.searchParams.get('board') || undefined;
    const clsRaw = url.searchParams.get('classLevel');
    const classLevel = clsRaw ? Number.parseInt(clsRaw, 10) : undefined;
    const studyGroup = (url.searchParams.get('studyGroup') || undefined) as StudyGroupCode | undefined;
    return respondWithPublicEdgeCache(request, env, headers, 3600, () =>
      listSubjects(env.DB, {
        boardCode,
        classLevel: classLevel && Number.isFinite(classLevel) ? classLevel : undefined,
        studyGroup,
      })
    );
  }

  if (method === 'GET' && path === '/v1/search') {
    const query = url.searchParams.get('q') || '';
    const boardCode = url.searchParams.get('board') || undefined;
    const clsRaw = url.searchParams.get('classLevel');
    const classLevel = clsRaw ? Number.parseInt(clsRaw, 10) : undefined;
    return respondWithPublicEdgeCache(request, env, headers, 600, () =>
      searchCatalog(env.DB, query, {
        boardCode,
        classLevel: classLevel && Number.isFinite(classLevel) ? classLevel : undefined,
      })
    );
  }

  const subjectChaptersMatch = path.match(/^\/v1\/subjects\/([^/]+)\/chapters$/);
  if (method === 'GET' && subjectChaptersMatch) {
    const subjectId = decodeURIComponent(subjectChaptersMatch[1]);
    return respondWithPublicEdgeCache(request, env, headers, 3600, () =>
      listChapters(env.DB, subjectId)
    );
  }

  const chapterContentMatch = path.match(/^\/v1\/chapters\/([^/]+)\/content$/);
  if (method === 'GET' && chapterContentMatch) {
    const chapterId = decodeURIComponent(chapterContentMatch[1]);
    const modeParam = (url.searchParams.get('mode') || 'detailed_notes') as ApiEducationalContent['mode'];
    if (!['detailed_notes', 'revision_notes', 'solved_questions'].includes(modeParam)) {
      throw new HttpError(400, 'INVALID_MODE', 'Content mode must be detailed_notes, revision_notes, or solved_questions.');
    }

    return respondWithPublicEdgeCache(request, env, headers, 3600, async () => {
      let content = await getChapterContent(env.DB, chapterId, modeParam, true);
      if (!content && modeParam === 'revision_notes') {
        const detailed = await getChapterContent(env.DB, chapterId, 'detailed_notes', true);
        if (detailed) {
          content = {
            ...detailed,
            mode: 'revision_notes',
            blocks: detailed.blocks.filter(
              (b) =>
                b.type === 'heading' ||
                b.type === 'definition' ||
                b.type === 'math_expression' ||
                b.type === 'exam_point'
            ),
          };
        }
      }
      if (!content && modeParam === 'solved_questions') {
        const detailed = await getChapterContent(env.DB, chapterId, 'detailed_notes', true);
        if (detailed) {
          content = {
            ...detailed,
            mode: 'solved_questions',
            blocks: detailed.blocks.filter((b) => b.type === 'solved_question' || b.type === 'example'),
          };
        }
      }

      if (!content) {
        throw new HttpError(404, 'CONTENT_NOT_PUBLISHED', `No published content available for chapter ${chapterId}.`);
      }
      return content;
    });
  }

  const chapterMcqsMatch = path.match(/^\/v1\/chapters\/([^/]+)\/mcqs$/);
  if (method === 'GET' && chapterMcqsMatch) {
    const chapterId = decodeURIComponent(chapterMcqsMatch[1]);
    const difficulty = url.searchParams.get('difficulty') || undefined;
    const topicTitle = url.searchParams.get('topic') || undefined;
    const limitRaw = Number.parseInt(url.searchParams.get('limit') || '100', 10);
    const offsetRaw = Number.parseInt(url.searchParams.get('offset') || '0', 10);
    const limit = Number.isFinite(limitRaw) ? limitRaw : 100;
    const offset = Number.isFinite(offsetRaw) ? offsetRaw : 0;

    return respondWithPublicEdgeCache(request, env, headers, 3600, () =>
      difficulty || topicTitle
        ? listMcqsFiltered(env.DB, { chapterId, difficulty, topicTitle, limit, offset })
        : listChapterMcqs(env.DB, chapterId, false, limit, offset)
    );
  }

  if (method === 'GET' && path === '/v1/exams/patterns') {
    const boardCode = url.searchParams.get('board') || undefined;
    const clsRaw = url.searchParams.get('classLevel');
    const subjectId = url.searchParams.get('subjectId') || undefined;
    return respondWithPublicEdgeCache(request, env, headers, 3600, () =>
      listExamPatterns(env.DB, {
        boardCode,
        classLevel: clsRaw ? Number.parseInt(clsRaw, 10) : undefined,
        subjectId,
      })
    );
  }

  if (method === 'POST' && path === '/v1/exams/start') {
    const body = (await request.json().catch(() => null)) as ApiExamStartRequest | null;
    if (!body || !body.mode || !body.classLevel || !body.boardCode) {
      throw new HttpError(400, 'INVALID_EXAM_REQUEST', 'mode, boardCode, and classLevel are required.');
    }

    const patterns = await listExamPatterns(env.DB, {
      boardCode: body.boardCode,
      classLevel: body.classLevel,
    });
    const matchedPattern = body.patternId ? patterns.find((p) => p.id === body.patternId) : undefined;

    const questionLimit = Math.max(
      1,
      Math.min(50, body.questionCount ?? matchedPattern?.questionCount ?? 10)
    );
    const durationMinutes = Math.max(
      3,
      Math.min(180, body.durationMinutes ?? matchedPattern?.durationMinutes ?? Math.max(5, questionLimit))
    );
    const marksPerMcq = body.marksPerMcq ?? matchedPattern?.marksPerMcq ?? 1;
    const negativeMarkingPenalty =
      body.negativeMarkingPenalty ?? matchedPattern?.negativeMarkingPenalty ?? 0;
    const passPercentage = matchedPattern?.passPercentage ?? 40;

    const mcqs = await listMcqsFiltered(env.DB, {
      boardCode: body.boardCode,
      classLevel: body.classLevel,
      subjectId: body.subjectId ?? matchedPattern?.subjectId ?? null,
      chapterId: body.chapterId ?? null,
      topicTitle: body.topicTitle ?? null,
      difficulty: body.difficulty ?? 'mixed',
      limit: questionLimit,
    });

    if (mcqs.length === 0) {
      throw new HttpError(404, 'NO_QUESTIONS_AVAILABLE', 'No published MCQs match the selected exam scope.');
    }

    const subjectiveCount =
      (matchedPattern?.includesShortQuestionsCount ?? (body.mode === 'full_mock_exam' ? 3 : 0)) +
      (matchedPattern?.includesLongQuestionsCount ?? (body.mode === 'full_mock_exam' ? 1 : 0));

    const subjectivePrompts =
      subjectiveCount > 0
        ? await listSubjectivePromptsForExam(
            env.DB,
            {
              subjectId: body.subjectId ?? matchedPattern?.subjectId ?? null,
              chapterId: body.chapterId ?? null,
              classLevel: body.classLevel,
            },
            subjectiveCount
          )
        : [];

    const sessionId = `exam_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
    const nowIso = new Date().toISOString();
    const resolvedStudent = await resolveStudentFromRequest(request, env).catch(() => null);

    await env.DB.prepare(
      `INSERT INTO exam_sessions (
        id, student_id, guest_device_id, mode, board_code, class_level,
        subject_id, chapter_id, mcq_ids_json, marks_per_mcq,
        negative_marking_penalty, pass_percentage, started_at_iso, submitted_at_iso
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)`
    )
      .bind(
        sessionId,
        resolvedStudent?.studentId ?? null,
        resolvedStudent?.guestDeviceId ?? null,
        body.mode,
        body.boardCode,
        body.classLevel,
        body.subjectId || mcqs[0].subjectId,
        body.chapterId ?? null,
        JSON.stringify(mcqs.map((m) => m.id)),
        marksPerMcq,
        negativeMarkingPenalty,
        passPercentage,
        nowIso
      )
      .run();

    const sessionPayload: ApiExamSessionPayload = {
      sessionId,
      mode: body.mode,
      title:
        matchedPattern?.title ||
        `${body.boardCode} Class ${body.classLevel} ${body.mode.replace(/_/g, ' ')}`,
      subjectId: body.subjectId || mcqs[0].subjectId,
      chapterId: body.chapterId ?? null,
      durationMinutes,
      marksPerMcq,
      negativeMarkingPenalty,
      passPercentage,
      mcqs: mcqs.map(stripMcqAnswerKey),
      subjectivePrompts,
    };

    return jsonEnvelope(sessionPayload, env, 200, {
      ...headers,
      'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
    });
  }

  if (method === 'POST' && path === '/v1/quizzes/submissions') {
    const body = (await request.json().catch(() => null)) as ApiQuizSubmissionRequest | null;
    if (!body || !body.sessionId || !body.guestDeviceId || !Array.isArray(body.answers)) {
      throw new HttpError(400, 'INVALID_QUIZ_SUBMISSION', 'sessionId, guestDeviceId, and answers array are required.');
    }

    const storedSession = await env.DB.prepare(
      `SELECT id, subject_id, chapter_id, mode, mcq_ids_json, marks_per_mcq, negative_marking_penalty, pass_percentage
       FROM exam_sessions WHERE id = ? LIMIT 1`
    )
      .bind(body.sessionId)
      .first<{
        id: string;
        subject_id: string | null;
        chapter_id: string | null;
        mode: string;
        mcq_ids_json: string;
        marks_per_mcq: number;
        negative_marking_penalty: number;
        pass_percentage: number;
      }>();

    const sessionMcqIds = storedSession
      ? safeJsonParse<string[]>(storedSession.mcq_ids_json, [])
      : [];
    const answerIds = Array.from(
      new Set([
        ...sessionMcqIds,
        ...body.answers.map((a) => a.mcqId).filter(Boolean),
      ])
    );
    const mcqs =
      answerIds.length > 0
        ? await listMcqsFiltered(env.DB, { ids: answerIds, includeUnpublished: true })
        : body.chapterId
          ? await listChapterMcqs(env.DB, body.chapterId)
          : [];

    const authoritativeSubmission: ApiQuizSubmissionRequest = storedSession
      ? {
          ...body,
          subjectId: body.subjectId || storedSession.subject_id || '',
          chapterId: body.chapterId ?? storedSession.chapter_id ?? null,
          examMode: (storedSession.mode as ApiQuizSubmissionRequest['examMode']) || body.examMode,
          marksPerMcq: Number(storedSession.marks_per_mcq ?? body.marksPerMcq ?? 1),
          negativeMarkingPenalty: Number(
            storedSession.negative_marking_penalty ?? body.negativeMarkingPenalty ?? 0
          ),
          passPercentage: Number(storedSession.pass_percentage ?? body.passPercentage ?? 40),
        }
      : body;

    const result = gradeQuizSubmission(mcqs, authoritativeSubmission);
    const submittedAtIso = body.submittedAtIso || new Date().toISOString();
    const examMode = authoritativeSubmission.examMode || 'chapter_test';

    // Resolve or auto-register guest student profile for mistakes/XP/history tracking
    const reqForStudent = new Request(request.url, {
      headers: new Headers([
        ...Array.from(request.headers.entries()),
        ...(request.headers.has('X-Guest-Device-Id')
          ? []
          : [['X-Guest-Device-Id', body.guestDeviceId.trim()] as [string, string]]),
      ]),
    });
    const resolvedStudent = await resolveStudentFromRequest(reqForStudent, env).catch(() => null);
    const effectiveStudentId = body.studentId ?? resolvedStudent?.studentId ?? null;

    await env.DB.prepare(
      `INSERT INTO quiz_submissions (
        id, session_id, student_id, guest_device_id, subject_id, chapter_id,
        total_questions, correct_count, incorrect_count, unanswered_count,
        percentage, elapsed_seconds, answers_json, submitted_at_iso,
        exam_mode, marks_obtained, total_marks, passed
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
      .bind(
        result.submissionId,
        body.sessionId,
        effectiveStudentId,
        body.guestDeviceId,
        authoritativeSubmission.subjectId,
        authoritativeSubmission.chapterId ?? null,
        result.totalQuestions,
        result.correctCount,
        result.incorrectCount,
        result.unansweredCount,
        result.percentage,
        result.elapsedSeconds,
        JSON.stringify(body.answers),
        submittedAtIso,
        examMode,
        result.marksObtained ?? result.correctCount,
        result.totalMarks ?? result.totalQuestions,
        result.passed ? 1 : 0
      )
      .run();

    if (storedSession) {
      await env.DB.prepare('UPDATE exam_sessions SET submitted_at_iso = ? WHERE id = ?')
        .bind(submittedAtIso, storedSession.id)
        .run();
    }

    // Update Mistake Master, XP, Streaks & Achievements for resolved student using D1 batching
    if (effectiveStudentId) {
      const mcqMap = new Map(mcqs.map((m) => [m.id, m]));
      const statements: D1PreparedStatement[] = [];

      for (const fb of result.perQuestionFeedback) {
        const item = mcqMap.get(fb.mcqId);
        if (!item) continue;
        const ans = body.answers.find((a) => a.mcqId === fb.mcqId);
        if (fb.isCorrect) {
          statements.push(
            env.DB.prepare(
              'UPDATE student_mistakes SET mastered = 1, last_attempted_at_iso = ? WHERE student_id = ? AND mcq_id = ?'
            ).bind(submittedAtIso, effectiveStudentId, fb.mcqId)
          );
        } else {
          const mistakeId = `msk_${crypto.randomUUID().replace(/-/g, '').slice(0, 10)}`;
          statements.push(
            env.DB.prepare(
              `INSERT INTO student_mistakes (
                id, student_id, mcq_id, subject_id, chapter_id, topic_title,
                question, last_wrong_option_id, correct_option_id, wrong_count,
                mastered, last_attempted_at_iso
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)
              ON CONFLICT(student_id, mcq_id) DO UPDATE SET
                last_wrong_option_id = excluded.last_wrong_option_id,
                wrong_count = wrong_count + 1,
                mastered = 0,
                last_attempted_at_iso = excluded.last_attempted_at_iso`
            ).bind(
              mistakeId,
              effectiveStudentId,
              fb.mcqId,
              item.subjectId,
              item.chapterId,
              item.topicTitle || '',
              item.question,
              ans?.selectedOptionId ?? null,
              item.correctOptionId ?? fb.correctOptionId,
              submittedAtIso
            )
          );
        }
      }

      const xpEarned = result.xpEarned ?? result.correctCount * 10;
      const diagFlag = examMode === 'diagnostic_test' ? 1 : 0;
      statements.push(
        env.DB.prepare(
          `UPDATE students SET
            xp_total = xp_total + ?,
            diagnostic_completed = MAX(diagnostic_completed, ?),
            last_active_iso = ?,
            updated_at_iso = ?
           WHERE id = ?`
        ).bind(xpEarned, diagFlag, submittedAtIso, submittedAtIso, effectiveStudentId)
      );

      statements.push(
        env.DB.prepare(
          `INSERT OR IGNORE INTO student_achievements (student_id, achievement_code, unlocked_at_iso) VALUES (?, 'first_quiz', ?)`
        ).bind(effectiveStudentId, submittedAtIso)
      );

      if (result.totalQuestions >= 3 && result.percentage === 100) {
        statements.push(
          env.DB.prepare(
            `INSERT OR IGNORE INTO student_achievements (student_id, achievement_code, unlocked_at_iso) VALUES (?, 'perfect_score', ?)`
          ).bind(effectiveStudentId, submittedAtIso)
        );
      }

      if (statements.length > 0) {
        await env.DB.batch(statements);
      }

      const loggedMins = Math.max(1, Math.round(result.elapsedSeconds / 60));
      await recordStudentActivityDay(
        env,
        effectiveStudentId,
        { minutes: loggedMins, quizzes: 1 },
        submittedAtIso
      );
    }

    return jsonEnvelope(result, env, 201, {
      ...headers,
      'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
    });
  }

  if (method === 'GET' && path === '/v1/past-papers') {
    const clsRaw = url.searchParams.get('classLevel');
    const boardCode = url.searchParams.get('board') || undefined;
    const solvedRaw = url.searchParams.get('isSolved');
    const isSolved =
      solvedRaw === 'true' ? true : solvedRaw === 'false' ? false : null;
    const limitRaw = Number.parseInt(url.searchParams.get('limit') || '100', 10);
    const offsetRaw = Number.parseInt(url.searchParams.get('offset') || '0', 10);
    return respondWithPublicEdgeCache(request, env, headers, 3600, () =>
      listPastPapers(env.DB, {
        classLevel: clsRaw ? Number.parseInt(clsRaw, 10) : undefined,
        boardCode,
        isSolved,
        limit: Number.isFinite(limitRaw) ? limitRaw : 100,
        offset: Number.isFinite(offsetRaw) ? offsetRaw : 0,
      })
    );
  }

  if (method === 'GET' && path === '/v1/pdfs') {
    const categoryRaw = url.searchParams.get('category') || undefined;
    const clsRaw = url.searchParams.get('classLevel');
    const boardCode = url.searchParams.get('board') || undefined;
    const subjectId = url.searchParams.get('subjectId') || undefined;
    const limitRaw = Number.parseInt(url.searchParams.get('limit') || '100', 10);
    const offsetRaw = Number.parseInt(url.searchParams.get('offset') || '0', 10);
    const category =
      categoryRaw === 'past_paper' ||
      categoryRaw === 'notes_pack' ||
      categoryRaw === 'syllabus' ||
      categoryRaw === 'textbook'
        ? categoryRaw
        : undefined;
    return respondWithPublicEdgeCache(request, env, headers, 3600, () =>
      listPdfMetadata(env.DB, {
        category,
        classLevel: clsRaw ? Number.parseInt(clsRaw, 10) : undefined,
        boardCode,
        subjectId,
        limit: Number.isFinite(limitRaw) ? limitRaw : 100,
        offset: Number.isFinite(offsetRaw) ? offsetRaw : 0,
      })
    );
  }

  const pdfMetaMatch = path.match(/^\/v1\/pdfs\/([^/]+)\/metadata$/);
  if (method === 'GET' && pdfMetaMatch) {
    const pdfId = decodeURIComponent(pdfMetaMatch[1]);
    return respondWithPublicEdgeCache(request, env, headers, 3600, async () => {
      const pdf = await getPdfById(env.DB, pdfId);
      if (!pdf) {
        throw new HttpError(404, 'PDF_NOT_FOUND', `PDF metadata ${pdfId} not found.`);
      }
      return pdf;
    });
  }

  const pdfDownloadMatch = path.match(/^\/v1\/pdfs\/([^/]+)\/download$/);
  if (method === 'GET' && pdfDownloadMatch) {
    const pdfId = decodeURIComponent(pdfDownloadMatch[1]);
    const pdf = await getPdfById(env.DB, pdfId);
    if (!pdf) {
      throw new HttpError(404, 'PDF_NOT_FOUND', `PDF metadata ${pdfId} not found.`);
    }

    const pdfEtag = pdf.checksumSha256 ? `"${pdf.checksumSha256}"` : `W/"${pdf.id}-${pdf.version}"`;
    if (ifNoneMatchMatches(request, pdfEtag)) {
      recordUsageDelta({ edgeCacheHits: 1 });
      return new Response(null, {
        status: 304,
        headers: {
          ...headers,
          ETag: pdfEtag,
          'Cache-Control': 'public, max-age=3600, s-maxage=86400',
        },
      });
    }

    // Free-tier Backblaze B2 daily egress protection
    const [policy, usage] = await Promise.all([
      getCapacityPolicy(env.DB),
      getTodayUsageSnapshot(env.DB),
    ]);
    const projectedDailyMb = (usage.b2DailyDownloadBytes + pdf.fileSizeBytes) / (1024 * 1024);
    if (policy.autoBlockUncachedPdfAtCritical && projectedDailyMb > policy.b2DailyDownloadSoftCapMb) {
      throw new HttpError(
        429,
        'B2_DAILY_BANDWIDTH_CAP',
        `Daily Backblaze B2 download soft cap (${policy.b2DailyDownloadSoftCapMb} MB) reached to protect zero-cost free tier. Previously cached offline PDFs remain available.`
      );
    }

    const mode = url.searchParams.get('mode') || 'stream';
    if (mode === 'presigned') {
      const presignedB2Url = await generateB2PresignedGetUrl(pdf.objectKey, env, 900);
      recordUsageDelta({ b2DownloadBytes: pdf.fileSizeBytes, b2DownloadCount: 1 });
      const payload: SignedDownloadUrlResponse = {
        pdfId: pdf.id,
        objectKey: pdf.objectKey,
        downloadUrl: `${url.origin}/v1/pdfs/${encodeURIComponent(pdf.id)}/download`,
        presignedB2Url,
        expiresInSeconds: 900,
      };
      return jsonEnvelope(payload, env, 200, {
        ...headers,
        'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
      });
    }

    const b2Client = new BackblazeB2Client(env, fetchFn);
    const upstream = await b2Client.getObject(pdf.objectKey);

    recordUsageDelta({ b2DownloadBytes: pdf.fileSizeBytes, b2DownloadCount: 1 });
    await env.DB.prepare(
      'UPDATE past_papers SET download_count = download_count + 1 WHERE pdf_metadata_id = ?'
    )
      .bind(pdf.id)
      .run();

    const fileName = pdf.objectKey.split('/').pop() || `${pdf.id}.pdf`;
    return new Response(upstream.body, {
      status: 200,
      headers: {
        ...headers,
        'Content-Type': 'application/pdf',
        'Content-Disposition': `inline; filename="${fileName.replace(/"/g, '')}"`,
        ETag: pdfEtag,
        'Cache-Control': 'public, max-age=3600, s-maxage=86400',
        ...(upstream.headers.get('Content-Length')
          ? { 'Content-Length': upstream.headers.get('Content-Length') as string }
          : {}),
      },
    });
  }

  return null;
}
