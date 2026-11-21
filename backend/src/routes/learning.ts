import {
  anonymizeLeaderboardHandle,
  calculateSm2NextReview,
  computeHonestExamReadiness,
  detectWeakChapters,
} from '../../../shared-contracts/rules';
import type {
  ApiAchievement,
  ApiChallenge,
  ApiFeedbackReport,
  ApiFlashcard,
  ApiFlashcardReviewRequest,
  ApiLeaderboardEntry,
  ApiMistakeRecord,
  ApiMotivationSummary,
  ApiQuizHistoryItem,
  ApiRevisionRecommendation,
  ApiSmartInsightsResponse,
  ApiStudentBookmark,
  ApiStudyPlanTask,
  ApiStudyPlannerState,
  BoardCode,
  ClassLevel,
} from '../../../shared-contracts/types';
import {
  listChapters,
  listMcqsFiltered,
  listSubjects,
} from '../db/repository';
import { HttpError, jsonEnvelope, type Env } from '../env';
import { PRIVATE_NO_STORE_CACHE_CONTROL } from '../middleware/cache';
import { recordStudentActivityDay, resolveStudentFromRequest } from './studentAuthSync';

const ACHIEVEMENT_DEFINITIONS: Array<{
  code: string;
  title: string;
  description: string;
  xpReward: number;
}> = [
  { code: 'first_quiz', title: 'First Practice Attempt', description: 'Submitted your first practice quiz.', xpReward: 25 },
  { code: 'perfect_score', title: '100% Concept Mastery', description: 'Scored 100% on a quiz with 3+ questions.', xpReward: 50 },
  { code: 'flashcard_reviewer', title: 'Spaced Repetition Starter', description: 'Reviewed flashcards using SM-2 memory scheduling.', xpReward: 20 },
  { code: 'mistake_conqueror', title: 'Mistake Master', description: 'Resolved a missed question in Mistake Master.', xpReward: 30 },
  { code: 'streak_3', title: '3-Day Study Streak', description: 'Studied on 3 consecutive days.', xpReward: 40 },
];

export async function handleLearningRoutes(
  request: Request,
  path: string,
  url: URL,
  env: Env,
  headers: Record<string, string>
): Promise<Response | null> {
  const method = request.method.toUpperCase();

  // Stage 8: Student Content Error & Feedback Submission (works with or without formal session, attaches student if provided)
  if (method === 'POST' && path === '/v1/feedback') {
    const body = (await request.json().catch(() => null)) as {
      category?: ApiFeedbackReport['category'];
      subjectId?: string | null;
      chapterId?: string | null;
      mcqId?: string | null;
      title?: string;
      description?: string;
      guestDeviceId?: string;
    } | null;

    if (!body || !body.title?.trim() || !body.description?.trim()) {
      throw new HttpError(400, 'INVALID_FEEDBACK', 'title and description are required.');
    }

    const category = body.category || 'content_error';
    if (!['content_error', 'mcq_answer_dispute', 'app_bug', 'feature_suggestion'].includes(category)) {
      throw new HttpError(400, 'INVALID_FEEDBACK_CATEGORY', 'Unsupported feedback category.');
    }

    const resolved = await resolveStudentFromRequest(request, env).catch(() => null);
    const id = `fb_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
    const nowIso = new Date().toISOString();

    await env.DB.prepare(
      `INSERT INTO feedback_reports (
        id, student_id, guest_device_id, category, subject_id, chapter_id,
        mcq_id, title, description, status, admin_resolution_note, created_at_iso, updated_at_iso
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'open', '', ?, ?)`
    )
      .bind(
        id,
        resolved?.studentId ?? null,
        body.guestDeviceId?.trim() || resolved?.guestDeviceId || null,
        category,
        body.subjectId ?? null,
        body.chapterId ?? null,
        body.mcqId ?? null,
        body.title.trim(),
        body.description.trim(),
        nowIso,
        nowIso
      )
      .run();

    const created: ApiFeedbackReport = {
      id,
      studentId: resolved?.studentId ?? null,
      guestDeviceId: body.guestDeviceId?.trim() || resolved?.guestDeviceId,
      category,
      subjectId: body.subjectId ?? null,
      chapterId: body.chapterId ?? null,
      mcqId: body.mcqId ?? null,
      title: body.title.trim(),
      description: body.description.trim(),
      status: 'open',
      adminResolutionNote: '',
      createdAtIso: nowIso,
      updatedAtIso: nowIso,
    };
    return jsonEnvelope(created, env, 201, headers);
  }

  // Require resolved student identity (Bearer or X-Guest-Device-Id) for /v1/students/me/* routes below
  if (!path.startsWith('/v1/students/me/')) {
    return null;
  }

  // Let /v1/students/me/progress and /v1/students/me/progress/sync stay handled by studentAuthSync.ts
  if (path === '/v1/students/me/progress' || path === '/v1/students/me/progress/sync') {
    return null;
  }

  const resolved = await resolveStudentFromRequest(request, env);
  const privateHeaders = {
    ...headers,
    'Cache-Control': PRIVATE_NO_STORE_CACHE_CONTROL,
  };

  // Stage 4: Persisted Quiz History
  if (method === 'GET' && path === '/v1/students/me/quizzes') {
    const limit = Math.max(1, Math.min(100, Number.parseInt(url.searchParams.get('limit') || '50', 10) || 50));
    const offset = Math.max(0, Number.parseInt(url.searchParams.get('offset') || '0', 10) || 0);
    const res = await env.DB.prepare(
      `SELECT q.id, q.session_id, q.subject_id, q.chapter_id, q.exam_mode,
              q.total_questions, q.correct_count, q.incorrect_count, q.unanswered_count,
              q.percentage, q.marks_obtained, q.total_marks, q.passed,
              q.elapsed_seconds, q.submitted_at_iso,
              COALESCE(s.name, 'Subject') AS subject_name,
              ch.title AS chapter_title
       FROM quiz_submissions q
       LEFT JOIN subjects s ON s.id = q.subject_id
       LEFT JOIN chapters ch ON ch.id = q.chapter_id
       WHERE q.student_id = ? OR q.guest_device_id = ?
       ORDER BY q.submitted_at_iso DESC
       LIMIT ? OFFSET ?`
    )
      .bind(resolved.studentId, resolved.guestDeviceId, limit, offset)
      .all<{
        id: string;
        session_id: string;
        subject_id: string;
        subject_name: string;
        chapter_id: string | null;
        chapter_title: string | null;
        exam_mode: ApiQuizHistoryItem['examMode'];
        total_questions: number;
        correct_count: number;
        incorrect_count: number;
        unanswered_count: number;
        percentage: number;
        marks_obtained: number;
        total_marks: number;
        passed: number;
        elapsed_seconds: number;
        submitted_at_iso: string;
      }>();

    const history: ApiQuizHistoryItem[] = res.results.map((r) => ({
      id: r.id,
      sessionId: r.session_id,
      subjectId: r.subject_id,
      subjectName: r.subject_name,
      chapterId: r.chapter_id,
      chapterTitle: r.chapter_title,
      examMode: r.exam_mode || 'chapter_test',
      totalQuestions: Number(r.total_questions),
      correctCount: Number(r.correct_count),
      incorrectCount: Number(r.incorrect_count),
      unansweredCount: Number(r.unanswered_count),
      percentage: Number(r.percentage),
      marksObtained: Number(r.marks_obtained || r.correct_count),
      totalMarks: Number(r.total_marks || r.total_questions),
      passed: r.passed === 1,
      elapsedSeconds: Number(r.elapsed_seconds),
      submittedAtIso: r.submitted_at_iso,
    }));

    return jsonEnvelope(history, env, 200, privateHeaders);
  }

  // Stage 3 & 5: Persistent Bookmarks / Saved Questions
  if (method === 'GET' && path === '/v1/students/me/bookmarks') {
    const res = await env.DB.prepare(
      'SELECT * FROM student_bookmarks WHERE student_id = ? ORDER BY created_at_iso DESC LIMIT 100'
    )
      .bind(resolved.studentId)
      .all<{
        id: string;
        item_type: ApiStudentBookmark['itemType'];
        item_id: string;
        chapter_id: string | null;
        subject_id: string | null;
        title: string;
        subtitle: string;
        created_at_iso: string;
      }>();

    const items: ApiStudentBookmark[] = res.results.map((r) => ({
      id: r.id,
      itemType: r.item_type,
      itemId: r.item_id,
      chapterId: r.chapter_id,
      subjectId: r.subject_id,
      title: r.title,
      subtitle: r.subtitle,
      createdAtIso: r.created_at_iso,
    }));
    return jsonEnvelope(items, env, 200, privateHeaders);
  }

  if (method === 'POST' && path === '/v1/students/me/bookmarks') {
    const body = (await request.json().catch(() => null)) as Partial<ApiStudentBookmark> | null;
    if (!body || !body.itemType || !body.itemId || !body.title?.trim()) {
      throw new HttpError(400, 'INVALID_BOOKMARK', 'itemType, itemId, and title are required.');
    }
    const id = `bmk_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
    const nowIso = new Date().toISOString();
    await env.DB.prepare(
      `INSERT INTO student_bookmarks (id, student_id, item_type, item_id, chapter_id, subject_id, title, subtitle, created_at_iso)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(student_id, item_type, item_id) DO UPDATE SET
         title = excluded.title,
         subtitle = excluded.subtitle,
         created_at_iso = excluded.created_at_iso`
    )
      .bind(
        id,
        resolved.studentId,
        body.itemType,
        body.itemId,
        body.chapterId ?? null,
        body.subjectId ?? null,
        body.title.trim(),
        body.subtitle?.trim() || '',
        nowIso
      )
      .run();

    const row = await env.DB.prepare(
      'SELECT * FROM student_bookmarks WHERE student_id = ? AND item_type = ? AND item_id = ? LIMIT 1'
    )
      .bind(resolved.studentId, body.itemType, body.itemId)
      .first<{
        id: string;
        item_type: ApiStudentBookmark['itemType'];
        item_id: string;
        chapter_id: string | null;
        subject_id: string | null;
        title: string;
        subtitle: string;
        created_at_iso: string;
      }>();

    return jsonEnvelope(
      row
        ? {
            id: row.id,
            itemType: row.item_type,
            itemId: row.item_id,
            chapterId: row.chapter_id,
            subjectId: row.subject_id,
            title: row.title,
            subtitle: row.subtitle,
            createdAtIso: row.created_at_iso,
          }
        : null,
      env,
      201,
      privateHeaders
    );
  }

  const bookmarkDeleteMatch = path.match(/^\/v1\/students\/me\/bookmarks\/([^/]+)$/);
  if (method === 'DELETE' && bookmarkDeleteMatch) {
    const bookmarkId = decodeURIComponent(bookmarkDeleteMatch[1]);
    await env.DB.prepare(
      'DELETE FROM student_bookmarks WHERE student_id = ? AND (id = ? OR item_id = ?)'
    )
      .bind(resolved.studentId, bookmarkId, bookmarkId)
      .run();
    return jsonEnvelope({ deleted: true, id: bookmarkId }, env, 200, privateHeaders);
  }

  // Stage 5: Mistake Master
  if (method === 'GET' && path === '/v1/students/me/mistakes') {
    const includeMastered = url.searchParams.get('includeMastered') === 'true';
    const limit = Math.max(1, Math.min(100, Number.parseInt(url.searchParams.get('limit') || '60', 10) || 60));
    const offset = Math.max(0, Number.parseInt(url.searchParams.get('offset') || '0', 10) || 0);
    const sql = includeMastered
      ? 'SELECT * FROM student_mistakes WHERE student_id = ? ORDER BY mastered ASC, last_attempted_at_iso DESC LIMIT ? OFFSET ?'
      : 'SELECT * FROM student_mistakes WHERE student_id = ? AND mastered = 0 ORDER BY last_attempted_at_iso DESC LIMIT ? OFFSET ?';
    const rows = await env.DB.prepare(sql)
      .bind(resolved.studentId, limit, offset)
      .all<{
        id: string;
        mcq_id: string;
        subject_id: string;
        chapter_id: string;
        topic_title: string;
        question: string;
        last_wrong_option_id: 'A' | 'B' | 'C' | 'D' | null;
        correct_option_id: 'A' | 'B' | 'C' | 'D';
        wrong_count: number;
        mastered: number;
        last_attempted_at_iso: string;
      }>();

    const mcqIds = rows.results.map((r) => r.mcq_id);
    const fullMcqs = mcqIds.length > 0 ? await listMcqsFiltered(env.DB, { ids: mcqIds, includeUnpublished: true }) : [];
    const mcqMap = new Map(fullMcqs.map((m) => [m.id, m]));

    const records: ApiMistakeRecord[] = rows.results.map((r) => ({
      id: r.id,
      mcqId: r.mcq_id,
      subjectId: r.subject_id,
      chapterId: r.chapter_id,
      topicTitle: r.topic_title,
      question: r.question,
      lastWrongOptionId: r.last_wrong_option_id,
      correctOptionId: r.correct_option_id,
      wrongCount: Number(r.wrong_count),
      mastered: r.mastered === 1,
      lastAttemptedAtIso: r.last_attempted_at_iso,
      mcq: mcqMap.get(r.mcq_id),
    }));
    return jsonEnvelope(records, env, 200, privateHeaders);
  }

  const mistakeResolveMatch = path.match(/^\/v1\/students\/me\/mistakes\/([^/]+)\/resolve$/);
  if (method === 'POST' && mistakeResolveMatch) {
    const mcqId = decodeURIComponent(mistakeResolveMatch[1]);
    const nowIso = new Date().toISOString();
    const res = await env.DB.prepare(
      'UPDATE student_mistakes SET mastered = 1, last_attempted_at_iso = ? WHERE student_id = ? AND mcq_id = ? AND mastered = 0'
    )
      .bind(nowIso, resolved.studentId, mcqId)
      .run();

    if ((res.meta.changes ?? 0) > 0) {
      await env.DB.batch([
        env.DB.prepare(
          `INSERT OR IGNORE INTO student_achievements (student_id, achievement_code, unlocked_at_iso) VALUES (?, 'mistake_conqueror', ?)`
        ).bind(resolved.studentId, nowIso),
        env.DB.prepare('UPDATE students SET xp_total = xp_total + 15 WHERE id = ?').bind(resolved.studentId),
      ]);
    }
    return jsonEnvelope({ mcqId, mastered: true }, env, 200, privateHeaders);
  }

  // Stage 5: Flashcards & SM-2 Spaced Repetition
  if (method === 'GET' && path === '/v1/students/me/flashcards') {
    const chapterId = url.searchParams.get('chapterId') || undefined;
    const subjectId = url.searchParams.get('subjectId') || undefined;
    const classRaw = url.searchParams.get('classLevel');
    const dueOnly = url.searchParams.get('dueOnly') === 'true';

    const clauses: string[] = [];
    const params: unknown[] = [resolved.studentId];
    if (chapterId) {
      clauses.push('f.chapter_id = ?');
      params.push(chapterId);
    }
    if (subjectId) {
      clauses.push('f.subject_id = ?');
      params.push(subjectId);
    }
    if (classRaw) {
      clauses.push('s.class_level = ?');
      params.push(Number.parseInt(classRaw, 10));
    }
    const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
    const res = await env.DB.prepare(
      `SELECT f.*, r.interval_days, r.ease_factor, r.repetitions, r.next_review_at_iso, r.last_reviewed_at_iso
       FROM flashcards f
       JOIN subjects s ON s.id = f.subject_id
       LEFT JOIN student_flashcard_reviews r ON r.flashcard_id = f.id AND r.student_id = ?
       ${where}
       ORDER BY COALESCE(r.next_review_at_iso, '1970-01-01T00:00:00Z') ASC
       LIMIT 100`
    )
      .bind(...params)
      .all<{
        id: string;
        subject_id: string;
        chapter_id: string;
        front: string;
        back: string;
        formula_hint: string | null;
        topic_title: string;
        interval_days: number | null;
        ease_factor: number | null;
        repetitions: number | null;
        next_review_at_iso: string | null;
        last_reviewed_at_iso: string | null;
        front_urdu?: string;
        back_urdu?: string;
      }>();

    const nowIso = new Date().toISOString();
    let cards: ApiFlashcard[] = res.results.map((r) => ({
      id: r.id,
      subjectId: r.subject_id,
      chapterId: r.chapter_id,
      front: r.front,
      back: r.back,
      formulaHint: r.formula_hint ?? undefined,
      topicTitle: r.topic_title,
      intervalDays: Number(r.interval_days ?? 1),
      easeFactor: Number(r.ease_factor ?? 2.5),
      repetitions: Number(r.repetitions ?? 0),
      nextReviewAtIso: r.next_review_at_iso ?? nowIso,
      lastReviewedAtIso: r.last_reviewed_at_iso ?? null,
      frontUrdu: r.front_urdu || undefined,
      backUrdu: r.back_urdu || undefined,
    }));

    if (dueOnly) {
      cards = cards.filter((c) => c.nextReviewAtIso <= nowIso);
    }

    return jsonEnvelope(cards, env, 200, privateHeaders);
  }

  if (method === 'POST' && path === '/v1/students/me/flashcards/review') {
    const body = (await request.json().catch(() => null)) as ApiFlashcardReviewRequest | null;
    if (!body || !body.flashcardId || typeof body.quality !== 'number' || body.quality < 0 || body.quality > 5) {
      throw new HttpError(400, 'INVALID_REVIEW', 'flashcardId and quality (0..5) are required.');
    }

    const flashcardExists = await env.DB.prepare(
      'SELECT id FROM flashcards WHERE id = ? LIMIT 1'
    )
      .bind(body.flashcardId)
      .first<{ id: string }>();
    if (!flashcardExists) {
      throw new HttpError(404, 'FLASHCARD_NOT_FOUND', `Flashcard ${body.flashcardId} not found.`);
    }

    const existing = await env.DB.prepare(
      'SELECT interval_days, ease_factor, repetitions FROM student_flashcard_reviews WHERE student_id = ? AND flashcard_id = ? LIMIT 1'
    )
      .bind(resolved.studentId, body.flashcardId)
      .first<{ interval_days: number; ease_factor: number; repetitions: number }>();

    const nowIso = body.reviewedAtIso || new Date().toISOString();
    const next = calculateSm2NextReview(
      {
        intervalDays: existing?.interval_days ?? 1,
        easeFactor: existing?.ease_factor ?? 2.5,
        repetitions: existing?.repetitions ?? 0,
      },
      body.quality,
      nowIso
    );

    await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO student_flashcard_reviews (
          student_id, flashcard_id, interval_days, ease_factor, repetitions, next_review_at_iso, last_reviewed_at_iso
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(student_id, flashcard_id) DO UPDATE SET
          interval_days = excluded.interval_days,
          ease_factor = excluded.ease_factor,
          repetitions = excluded.repetitions,
          next_review_at_iso = excluded.next_review_at_iso,
          last_reviewed_at_iso = excluded.last_reviewed_at_iso`
      ).bind(
        resolved.studentId,
        body.flashcardId,
        next.intervalDays,
        next.easeFactor,
        next.repetitions,
        next.nextReviewAtIso,
        nowIso
      ),
      env.DB.prepare(
        `INSERT OR IGNORE INTO student_achievements (student_id, achievement_code, unlocked_at_iso) VALUES (?, 'flashcard_reviewer', ?)`
      ).bind(resolved.studentId, nowIso),
      env.DB.prepare('UPDATE students SET xp_total = xp_total + 5 WHERE id = ?').bind(resolved.studentId),
    ]);

    await recordStudentActivityDay(env, resolved.studentId, { minutes: 2 }, nowIso);

    return jsonEnvelope({ flashcardId: body.flashcardId, ...next, lastReviewedAtIso: nowIso }, env, 200, privateHeaders);
  }

  // Stage 5: Honest Smart Insights & Analytics
  if (method === 'GET' && path === '/v1/students/me/insights') {
    const st = await env.DB.prepare(
      'SELECT selected_class, selected_board, diagnostic_completed FROM students WHERE id = ? LIMIT 1'
    )
      .bind(resolved.studentId)
      .first<{ selected_class: ClassLevel; selected_board: BoardCode; diagnostic_completed: number }>();

    const classLevel = st?.selected_class ?? 9;
    const boardCode = st?.selected_board ?? 'KPK';

    const [subjects, allChapters] = await Promise.all([
      listSubjects(env.DB, { boardCode, classLevel }),
      listChapters(env.DB),
    ]);
    const subjectsById = new Map(subjects.map((s) => [s.id, s]));
    const chaptersForClass = allChapters.filter((c) => subjectsById.has(c.subjectId));

    const progRows = await env.DB.prepare(
      'SELECT chapter_id, subject_id, completion_percent, last_block_index, is_completed, last_accessed_at_iso FROM student_chapter_progress WHERE student_id = ?'
    )
      .bind(resolved.studentId)
      .all<{
        chapter_id: string;
        subject_id: string;
        completion_percent: number;
        last_block_index: number;
        is_completed: number;
        last_accessed_at_iso: string;
      }>();
    const progressByChapterId = new Map(
      progRows.results.map((r) => [
        r.chapter_id,
        {
          chapterId: r.chapter_id,
          subjectId: r.subject_id,
          completionPercent: Number(r.completion_percent),
          lastBlockIndex: Number(r.last_block_index),
          isCompleted: r.is_completed === 1,
          lastAccessedAtIso: r.last_accessed_at_iso,
        },
      ])
    );

    const quizRows = await env.DB.prepare(
      'SELECT subject_id, chapter_id, total_questions, percentage FROM quiz_submissions WHERE student_id = ? OR guest_device_id = ? ORDER BY submitted_at_iso DESC LIMIT 200'
    )
      .bind(resolved.studentId, resolved.guestDeviceId)
      .all<{ subject_id: string; chapter_id: string | null; total_questions: number; percentage: number }>();

    const totalQuizAttempts = quizRows.results.length;
    const totalQuestionsAnswered = quizRows.results.reduce((a, b) => a + Number(b.total_questions), 0);
    const overallAccuracyPercent =
      totalQuizAttempts === 0
        ? null
        : Math.round(quizRows.results.reduce((a, b) => a + Number(b.percentage), 0) / totalQuizAttempts);

    const quizStatsByChapterId = new Map<string, { attemptsCount: number; averagePercent: number }>();
    const chapterAgg = new Map<string, { sum: number; cnt: number }>();
    for (const q of quizRows.results) {
      if (!q.chapter_id) continue;
      const cur = chapterAgg.get(q.chapter_id) ?? { sum: 0, cnt: 0 };
      cur.sum += Number(q.percentage);
      cur.cnt += 1;
      chapterAgg.set(q.chapter_id, cur);
    }
    for (const [chId, v] of chapterAgg.entries()) {
      quizStatsByChapterId.set(chId, { attemptsCount: v.cnt, averagePercent: v.sum / v.cnt });
    }

    const mistakeRows = await env.DB.prepare(
      'SELECT chapter_id, COUNT(*) AS cnt FROM student_mistakes WHERE student_id = ? AND mastered = 0 GROUP BY chapter_id'
    )
      .bind(resolved.studentId)
      .all<{ chapter_id: string; cnt: number }>();
    const activeMistakesByChapterId = new Map(mistakeRows.results.map((r) => [r.chapter_id, Number(r.cnt)]));
    const activeMistakesCount = mistakeRows.results.reduce((a, b) => a + Number(b.cnt), 0);

    const nowIso = new Date().toISOString();
    const dueCardsRes = await env.DB.prepare(
      `SELECT COUNT(*) AS cnt
       FROM flashcards f
       JOIN subjects s ON s.id = f.subject_id
       LEFT JOIN student_flashcard_reviews r ON r.flashcard_id = f.id AND r.student_id = ?
       WHERE s.class_level = ? AND COALESCE(r.next_review_at_iso, ?) <= ?`
    )
      .bind(resolved.studentId, classLevel, nowIso, nowIso)
      .first<{ cnt: number }>();

    const publishedChapters = chaptersForClass.filter((c) => c.publishStatus === 'published');
    const completedChaptersCount = Array.from(progressByChapterId.values()).filter((p) => p.isCompleted).length;

    const readiness = computeHonestExamReadiness({
      quizAttemptsCount: totalQuizAttempts,
      totalQuestionsAnswered,
      overallAccuracyPercent,
      completedChaptersCount,
      totalPublishedChaptersCount: publishedChapters.length,
      activeMistakesCount,
    });

    const weakChapters = detectWeakChapters({
      chapters: chaptersForClass,
      subjectsById,
      progressByChapterId,
      quizStatsByChapterId,
      activeMistakesByChapterId,
    });

    const subjectAnalytics = subjects.map((s) => {
      const subQuizzes = quizRows.results.filter((q) => q.subject_id === s.id);
      const subChProg = progRows.results.filter((p) => p.subject_id === s.id);
      const subCompleted = subChProg.filter((p) => p.is_completed === 1).length;
      const quizAveragePercent =
        subQuizzes.length === 0
          ? null
          : Math.round(subQuizzes.reduce((a, b) => a + Number(b.percentage), 0) / subQuizzes.length);
      const noteAvg =
        subChProg.length === 0
          ? 0
          : Math.round(subChProg.reduce((a, b) => a + Number(b.completion_percent), 0) / subChProg.length);
      const masteryPercent =
        quizAveragePercent !== null
          ? Math.round(quizAveragePercent * 0.65 + noteAvg * 0.35)
          : subChProg.length > 0
            ? noteAvg
            : null;
      return {
        subjectId: s.id,
        subjectName: s.name,
        masteryPercent,
        quizAveragePercent,
        chaptersCompleted: subCompleted,
        totalChapters: s.totalChapters,
        attemptsCount: subQuizzes.length,
      };
    });

    const revisionRecommendations: ApiRevisionRecommendation[] = [];
    if (activeMistakesCount > 0 && mistakeRows.results[0]) {
      const ch = chaptersForClass.find((c) => c.id === mistakeRows.results[0].chapter_id);
      if (ch) {
        revisionRecommendations.push({
          id: `rec_msk_${ch.id}`,
          chapterId: ch.id,
          subjectId: ch.subjectId,
          title: `Clear ${activeMistakesCount} missed question(s) in ${ch.title}`,
          reason: 'Re-attempting missed MCQs strengthens weak exam concepts.',
          actionType: 'review_mistakes',
        });
      }
    }
    for (const w of weakChapters.slice(0, 3)) {
      revisionRecommendations.push({
        id: `rec_wk_${w.chapterId}`,
        chapterId: w.chapterId,
        subjectId: w.subjectId,
        title: w.attemptsCount > 0 ? `Revise ${w.chapterTitle}` : `Start ${w.chapterTitle}`,
        reason: w.reason,
        actionType: w.attemptsCount > 0 ? 'revise_notes' : 'take_test',
      });
    }

    const insights: ApiSmartInsightsResponse = {
      readinessPercent: readiness.readinessPercent,
      readinessStatus: readiness.readinessStatus,
      readinessMessage: readiness.readinessMessage,
      diagnosticCompleted: st?.diagnostic_completed === 1,
      overallAccuracyPercent,
      totalQuizAttempts,
      masteredChaptersCount: completedChaptersCount,
      weakChapters,
      subjectAnalytics,
      revisionRecommendations,
      dueFlashcardsCount: Number(dueCardsRes?.cnt ?? 0),
      activeMistakesCount,
    };

    return jsonEnvelope(insights, env, 200, privateHeaders);
  }

  // Stage 6: Study Planner & Daily Targets
  if (method === 'GET' && path === '/v1/students/me/planner') {
    const st = await env.DB.prepare(
      `SELECT daily_target_minutes, exam_countdown_date_iso, current_streak_days, longest_streak_days
       FROM students WHERE id = ? LIMIT 1`
    )
      .bind(resolved.studentId)
      .first<{
        daily_target_minutes: number;
        exam_countdown_date_iso: string | null;
        current_streak_days: number;
        longest_streak_days: number;
      }>();

    const todayYmd = new Date().toISOString().slice(0, 10);
    const todayAct = await env.DB.prepare(
      'SELECT minutes_logged FROM student_activity_days WHERE student_id = ? AND day_ymd = ? LIMIT 1'
    )
      .bind(resolved.studentId, todayYmd)
      .first<{ minutes_logged: number }>();

    const taskRows = await env.DB.prepare(
      'SELECT * FROM student_study_tasks WHERE student_id = ? ORDER BY completed ASC, planned_date_iso ASC LIMIT 100'
    )
      .bind(resolved.studentId)
      .all<{
        id: string;
        title: string;
        subject_id: string | null;
        chapter_id: string | null;
        planned_date_iso: string;
        estimated_minutes: number;
        task_type: ApiStudyPlanTask['taskType'];
        completed: number;
        completed_at_iso: string | null;
      }>();

    const planner: ApiStudyPlannerState = {
      dailyTargetMinutes: Number(st?.daily_target_minutes ?? 60),
      examCountdownDateIso: st?.exam_countdown_date_iso ?? null,
      remindersEnabled: false,
      reminderHourLocal: 18,
      currentStreakDays: Number(st?.current_streak_days ?? 0),
      longestStreakDays: Number(st?.longest_streak_days ?? 0),
      minutesLoggedToday: Number(todayAct?.minutes_logged ?? 0),
      tasks: taskRows.results.map((r) => ({
        id: r.id,
        title: r.title,
        subjectId: r.subject_id,
        chapterId: r.chapter_id,
        plannedDateIso: r.planned_date_iso,
        estimatedMinutes: Number(r.estimated_minutes),
        taskType: r.task_type,
        completed: r.completed === 1,
        completedAtIso: r.completed_at_iso,
      })),
    };
    return jsonEnvelope(planner, env, 200, privateHeaders);
  }

  if (method === 'PUT' && path === '/v1/students/me/planner') {
    const body = (await request.json().catch(() => null)) as {
      dailyTargetMinutes?: number;
      examCountdownDateIso?: string | null;
    } | null;
    if (!body) throw new HttpError(400, 'INVALID_PLANNER_CONFIG', 'JSON payload required.');
    const nowIso = new Date().toISOString();
    const hasCountdownField = Object.prototype.hasOwnProperty.call(body, 'examCountdownDateIso');
    await env.DB.prepare(
      `UPDATE students SET
        daily_target_minutes = COALESCE(?, daily_target_minutes),
        exam_countdown_date_iso = CASE WHEN ? = 1 THEN ? ELSE exam_countdown_date_iso END,
        updated_at_iso = ?
       WHERE id = ?`
    )
      .bind(
        body.dailyTargetMinutes ? Math.max(10, Math.min(480, body.dailyTargetMinutes)) : null,
        hasCountdownField ? 1 : 0,
        body.examCountdownDateIso ?? null,
        nowIso,
        resolved.studentId
      )
      .run();
    return jsonEnvelope({ updated: true }, env, 200, privateHeaders);
  }

  if (method === 'POST' && path === '/v1/students/me/planner/tasks') {
    const body = (await request.json().catch(() => null)) as Partial<ApiStudyPlanTask> | null;
    if (!body || !body.title?.trim()) {
      throw new HttpError(400, 'INVALID_TASK', 'title is required.');
    }
    const requestedId =
      typeof body.id === 'string' && /^[A-Za-z0-9_-]{6,64}$/.test(body.id.trim())
        ? body.id.trim()
        : null;
    let id = requestedId || `tsk_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
    if (requestedId) {
      const existingOwner = await env.DB.prepare(
        'SELECT student_id FROM student_study_tasks WHERE id = ? LIMIT 1'
      )
        .bind(requestedId)
        .first<{ student_id: string }>();
      if (existingOwner && existingOwner.student_id !== resolved.studentId) {
        id = `tsk_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
      }
    }
    const nowIso = new Date().toISOString();
    const plannedDateIso = body.plannedDateIso || nowIso.slice(0, 10);
    const estimatedMinutes = Math.max(5, Math.min(240, Number(body.estimatedMinutes ?? 30)));
    const taskType = body.taskType || 'study';

    await env.DB.prepare(
      `INSERT INTO student_study_tasks (
        id, student_id, title, subject_id, chapter_id, planned_date_iso,
        estimated_minutes, task_type, completed, completed_at_iso, created_at_iso
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, NULL, ?)
      ON CONFLICT(id) DO UPDATE SET
        title = excluded.title,
        subject_id = excluded.subject_id,
        chapter_id = excluded.chapter_id,
        planned_date_iso = excluded.planned_date_iso,
        estimated_minutes = excluded.estimated_minutes,
        task_type = excluded.task_type`
    )
      .bind(
        id,
        resolved.studentId,
        body.title.trim(),
        body.subjectId ?? null,
        body.chapterId ?? null,
        plannedDateIso,
        estimatedMinutes,
        taskType,
        nowIso
      )
      .run();

    const created: ApiStudyPlanTask = {
      id,
      title: body.title.trim(),
      subjectId: body.subjectId ?? null,
      chapterId: body.chapterId ?? null,
      plannedDateIso,
      estimatedMinutes,
      taskType,
      completed: false,
      completedAtIso: null,
    };
    return jsonEnvelope(created, env, 201, privateHeaders);
  }

  const taskIdMatch = path.match(/^\/v1\/students\/me\/planner\/tasks\/([^/]+)$/);
  if (taskIdMatch && method === 'PATCH') {
    const taskId = decodeURIComponent(taskIdMatch[1]);
    const body = (await request.json().catch(() => null)) as { completed?: boolean } | null;
    if (!body || typeof body.completed !== 'boolean') {
      throw new HttpError(400, 'INVALID_TASK_PATCH', 'completed boolean is required.');
    }
    const nowIso = new Date().toISOString();
    const task = await env.DB.prepare(
      'SELECT estimated_minutes, completed FROM student_study_tasks WHERE id = ? AND student_id = ? LIMIT 1'
    )
      .bind(taskId, resolved.studentId)
      .first<{ estimated_minutes: number; completed: number }>();

    if (!task) throw new HttpError(404, 'TASK_NOT_FOUND', `Study task ${taskId} not found.`);

    await env.DB.prepare(
      'UPDATE student_study_tasks SET completed = ?, completed_at_iso = ? WHERE id = ? AND student_id = ?'
    )
      .bind(body.completed ? 1 : 0, body.completed ? nowIso : null, taskId, resolved.studentId)
      .run();

    const wasCompleted = task.completed === 1;
    if (!wasCompleted && body.completed) {
      await recordStudentActivityDay(
        env,
        resolved.studentId,
        { minutes: Number(task.estimated_minutes || 20) },
        nowIso
      );
      await env.DB.prepare('UPDATE students SET xp_total = xp_total + 15 WHERE id = ?')
        .bind(resolved.studentId)
        .run();
    }
    return jsonEnvelope({ id: taskId, completed: body.completed }, env, 200, privateHeaders);
  }

  if (taskIdMatch && method === 'DELETE') {
    const taskId = decodeURIComponent(taskIdMatch[1]);
    await env.DB.prepare('DELETE FROM student_study_tasks WHERE id = ? AND student_id = ?')
      .bind(taskId, resolved.studentId)
      .run();
    return jsonEnvelope({ deleted: true, id: taskId }, env, 200, privateHeaders);
  }

  // Stage 7: Motivation, XP, Challenges & Privacy-Safe Opt-In Leaderboard
  if (method === 'GET' && path === '/v1/students/me/motivation') {
    const st = await env.DB.prepare(
      `SELECT id, display_name, guest_device_id, selected_class, selected_board,
              xp_total, current_streak_days, leaderboard_opt_in, alias_handle
       FROM students WHERE id = ? LIMIT 1`
    )
      .bind(resolved.studentId)
      .first<{
        id: string;
        display_name: string;
        guest_device_id: string;
        selected_class: ClassLevel;
        selected_board: BoardCode;
        xp_total: number;
        current_streak_days: number;
        leaderboard_opt_in: number;
        alias_handle: string | null;
      }>();

    const xpTotal = Number(st?.xp_total ?? 0);
    const level = Math.floor(xpTotal / 150) + 1;
    const nextLevelXp = level * 150;

    const unlockedRows = await env.DB.prepare(
      'SELECT achievement_code, unlocked_at_iso FROM student_achievements WHERE student_id = ?'
    )
      .bind(resolved.studentId)
      .all<{ achievement_code: string; unlocked_at_iso: string }>();
    const unlockedMap = new Map(unlockedRows.results.map((r) => [r.achievement_code, r.unlocked_at_iso]));

    const achievements: ApiAchievement[] = ACHIEVEMENT_DEFINITIONS.map((d) => ({
      id: d.code,
      code: d.code,
      title: d.title,
      description: d.description,
      xpReward: d.xpReward,
      unlocked: unlockedMap.has(d.code),
      unlockedAtIso: unlockedMap.get(d.code) ?? null,
    }));

    const todayYmd = new Date().toISOString().slice(0, 10);
    const todayAct = await env.DB.prepare(
      'SELECT quizzes_completed, minutes_logged FROM student_activity_days WHERE student_id = ? AND day_ymd = ? LIMIT 1'
    )
      .bind(resolved.studentId, todayYmd)
      .first<{ quizzes_completed: number; minutes_logged: number }>();

    const weekStartIso = new Date(Date.now() - 7 * 86_400_000).toISOString().slice(0, 10);
    const weekAct = await env.DB.prepare(
      'SELECT COALESCE(SUM(quizzes_completed), 0) AS qcnt FROM student_activity_days WHERE student_id = ? AND day_ymd >= ?'
    )
      .bind(resolved.studentId, weekStartIso)
      .first<{ qcnt: number }>();

    const endOfDayIso = `${todayYmd}T23:59:59Z`;
    const endOfWeekIso = new Date(Date.now() + 7 * 86_400_000).toISOString();
    const dailyQuizzes = Number(todayAct?.quizzes_completed ?? 0);
    const weeklyQuizzes = Number(weekAct?.qcnt ?? 0);

    const challenges: ApiChallenge[] = [
      {
        id: 'chg_daily_quiz',
        title: 'Daily Practice Sprint',
        description: 'Complete at least 1 chapter or topic test today.',
        cadence: 'daily',
        targetCount: 1,
        currentCount: Math.min(1, dailyQuizzes),
        xpReward: 25,
        completed: dailyQuizzes >= 1,
        expiresAtIso: endOfDayIso,
      },
      {
        id: 'chg_weekly_5',
        title: 'Weekly KPK Board Marathon',
        description: 'Complete 5 quizzes or mock tests this week.',
        cadence: 'weekly',
        targetCount: 5,
        currentCount: Math.min(5, weeklyQuizzes),
        xpReward: 100,
        completed: weeklyQuizzes >= 5,
        expiresAtIso: endOfWeekIso,
      },
    ];

    let leaderboard: ApiLeaderboardEntry[] = [];
    if (st?.leaderboard_opt_in === 1) {
      const lbRows = await env.DB.prepare(
        `SELECT id, alias_handle, display_name, guest_device_id, selected_board, selected_class, xp_total, current_streak_days
         FROM students
         WHERE leaderboard_opt_in = 1 AND selected_class = ? AND selected_board = ?
         ORDER BY xp_total DESC
         LIMIT 25`
      )
        .bind(st.selected_class, st.selected_board)
        .all<{
          id: string;
          alias_handle: string | null;
          display_name: string;
          guest_device_id: string;
          selected_board: BoardCode;
          selected_class: ClassLevel;
          xp_total: number;
          current_streak_days: number;
        }>();

      leaderboard = lbRows.results.map((r, idx) => ({
        rank: idx + 1,
        aliasHandle: r.alias_handle || anonymizeLeaderboardHandle(r.display_name, r.guest_device_id),
        boardCode: r.selected_board,
        classLevel: r.selected_class,
        xpTotal: Number(r.xp_total),
        streakDays: Number(r.current_streak_days),
        isCurrentUser: r.id === resolved.studentId,
      }));
    }

    const summary: ApiMotivationSummary = {
      xpTotal,
      level,
      nextLevelXp,
      currentStreakDays: Number(st?.current_streak_days ?? 0),
      leaderboardOptIn: st?.leaderboard_opt_in === 1,
      aliasHandle:
        st?.alias_handle || anonymizeLeaderboardHandle(st?.display_name, resolved.guestDeviceId),
      achievements,
      challenges,
      leaderboard,
    };
    return jsonEnvelope(summary, env, 200, privateHeaders);
  }

  if (method === 'POST' && path === '/v1/students/me/leaderboard-opt-in') {
    const body = (await request.json().catch(() => null)) as {
      optIn?: boolean;
      aliasHandle?: string;
    } | null;
    if (!body || typeof body.optIn !== 'boolean') {
      throw new HttpError(400, 'INVALID_OPT_IN', 'optIn boolean is required.');
    }

    const st = await env.DB.prepare('SELECT display_name, guest_device_id FROM students WHERE id = ?')
      .bind(resolved.studentId)
      .first<{ display_name: string; guest_device_id: string }>();

    const sanitizedAlias = body.aliasHandle?.trim()
      ? body.aliasHandle.trim().replace(/@.*/, '').replace(/[^a-zA-Z0-9_-]/g, '').slice(0, 18)
      : anonymizeLeaderboardHandle(st?.display_name, resolved.guestDeviceId);

    await env.DB.prepare(
      'UPDATE students SET leaderboard_opt_in = ?, alias_handle = ?, updated_at_iso = ? WHERE id = ?'
    )
      .bind(body.optIn ? 1 : 0, sanitizedAlias || 'Scholar', new Date().toISOString(), resolved.studentId)
      .run();

    return jsonEnvelope(
      { leaderboardOptIn: body.optIn, aliasHandle: sanitizedAlias },
      env,
      200,
      privateHeaders
    );
  }

  return null;
}
