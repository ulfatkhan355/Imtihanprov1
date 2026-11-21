import test from 'node:test';
import assert from 'node:assert/strict';
import { handleWorkerRequest } from '../backend/src/index';
import { gradeQuizSubmission } from '../shared-contracts/rules';
import { createTestD1Env } from './helpers/sqliteD1';

async function jsonReq(
  env: ReturnType<typeof createTestD1Env>['env'],
  path: string,
  init: RequestInit = {},
  fetchFn?: typeof fetch
) {
  const req = new Request(`https://imtihan-pro-api.workers.dev${path}`, init);
  const res = await handleWorkerRequest(req, env, undefined, { fetchFn, skipRateLimit: true });
  const body = (await res.json()) as {
    ok: boolean;
    data: any;
    error?: { code: string; message: string };
  };
  return { status: res.status, headers: res.headers, body };
}

test('Google guest merge transfers bookmarks, flashcard reviews, and feedback without double-counting XP on replay', async () => {
  const { db, env } = createTestD1Env({
    GOOGLE_OAUTH_CLIENT_ID: 'test-google-client.apps.googleusercontent.com',
  });

  const mockFetch: typeof fetch = async (input) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    if (url.startsWith('https://oauth2.googleapis.com/tokeninfo')) {
      return new Response(
        JSON.stringify({
          sub: 'google_uid_merge_01',
          aud: 'test-google-client.apps.googleusercontent.com',
          email: 'zainab.kpk@example.com',
          email_verified: 'true',
          name: 'Zainab Khan',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      );
    }
    throw new Error(`Unexpected fetch URL: ${url}`);
  };

  // Create Google-linked account on device A
  const firstExchange = await jsonReq(
    env,
    '/v1/auth/google/exchange',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'device-a-primary',
        idToken: 'valid-google-id-token-device-a-123456',
      }),
    },
    mockFetch
  );
  assert.equal(firstExchange.status, 200);
  const googleStudentId = firstExchange.body.data.user.id as string;

  // Create separate guest session on device B with bookmark, flashcard review, XP, and feedback
  const guestAuth = await jsonReq(env, '/v1/auth/guest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      guestDeviceId: 'device-b-guest',
      displayName: 'Guest Zainab',
      selectedBoard: 'KPK',
      selectedClass: 11,
    }),
  });
  assert.equal(guestAuth.status, 200);
  const guestToken = guestAuth.body.data.token as string;
  const guestHeaders = { 'Content-Type': 'application/json', Authorization: `Bearer ${guestToken}` };

  const bmk = await jsonReq(env, '/v1/students/me/bookmarks', {
    method: 'POST',
    headers: guestHeaders,
    body: JSON.stringify({
      itemType: 'chapter',
      itemId: 'ch_phy11_01',
      chapterId: 'ch_phy11_01',
      subjectId: 'sub_kpk11_phy',
      title: 'Measurement',
      subtitle: 'Chapter 1 Revision Note',
    }),
  });
  assert.equal(bmk.status, 201);

  const fcBad = await jsonReq(env, '/v1/students/me/flashcards/review', {
    method: 'POST',
    headers: guestHeaders,
    body: JSON.stringify({ flashcardId: 'fc_nonexistent_id', quality: 5 }),
  });
  assert.equal(fcBad.status, 404);

  const fc = await jsonReq(env, '/v1/students/me/flashcards/review', {
    method: 'POST',
    headers: guestHeaders,
    body: JSON.stringify({ flashcardId: 'fc_phy11_01_1', quality: 5 }),
  });
  assert.equal(fc.status, 200);

  const fb = await jsonReq(env, '/v1/feedback', {
    method: 'POST',
    headers: guestHeaders,
    body: JSON.stringify({
      guestDeviceId: 'device-b-guest',
      category: 'content_error',
      title: 'Typo check',
      description: 'Check unit symbol on page 3',
    }),
  });
  assert.equal(fb.status, 201);

  const guestRowBefore = db
    .prepare('SELECT id, xp_total FROM students WHERE guest_device_id = ?')
    .get('device-b-guest') as { id: string; xp_total: number };
  assert.ok(guestRowBefore.xp_total > 0);

  // Merge guest device B into Google account
  const mergeRes = await jsonReq(
    env,
    '/v1/auth/google/exchange',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'device-b-guest',
        idToken: 'valid-google-id-token-device-b-123456',
      }),
    },
    mockFetch
  );
  assert.equal(mergeRes.status, 200);
  assert.equal(mergeRes.body.data.user.id, googleStudentId);

  // Bookmarks, flashcard reviews, feedback transferred cleanly
  const googleAuthHeaders = { Authorization: `Bearer ${mergeRes.body.data.token}` };
  const mergedBookmarks = await jsonReq(env, '/v1/students/me/bookmarks', {
    headers: googleAuthHeaders,
  });
  assert.equal(mergedBookmarks.status, 200);
  assert.equal(mergedBookmarks.body.data.length, 1);
  assert.equal(mergedBookmarks.body.data[0].subtitle, 'Chapter 1 Revision Note');

  const feedbackCount = db
    .prepare('SELECT COUNT(*) as c FROM feedback_reports WHERE student_id = ?')
    .get(googleStudentId) as { c: number };
  assert.equal(feedbackCount.c, 1);

  const googleRowAfter = db
    .prepare('SELECT xp_total, guest_device_id FROM students WHERE id = ?')
    .get(googleStudentId) as { xp_total: number; guest_device_id: string };
  assert.equal(googleRowAfter.guest_device_id, 'device-b-guest');

  // Replaying Google exchange with the same device does NOT double XP
  const replayRes = await jsonReq(
    env,
    '/v1/auth/google/exchange',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'device-b-guest',
        idToken: 'valid-google-id-token-replay-123456',
      }),
    },
    mockFetch
  );
  assert.equal(replayRes.status, 200);
  const googleRowReplay = db
    .prepare('SELECT xp_total FROM students WHERE id = ?')
    .get(googleStudentId) as { xp_total: number };
  assert.equal(googleRowReplay.xp_total, googleRowAfter.xp_total);
});

test('Worker PDF Range + SHA-256 If-Range returns 206 and does not forward SHA-256 If-Range header to B2', async () => {
  const { db, env } = createTestD1Env();
  const samplePdf = new TextEncoder().encode('%PDF-1.4\n0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ%%EOF');
  const shaHex = 'a'.repeat(64);
  const nowIso = new Date().toISOString();
  db.prepare(
    `INSERT OR REPLACE INTO pdf_assets (
      id, title, category, class_level, board_code, subject_id, object_key,
      file_size_bytes, page_count, checksum_sha256, offline_cacheable, version, created_at_iso, updated_at_iso
    ) VALUES (?, ?, 'textbook', 11, 'KPK', 'sub_kpk11_phy', ?, ?, 1, ?, 0, 'v1', ?, ?)`
  ).run('pdf_range_qa_1', 'QA Range Test PDF', 'kpk/class-11/physics/qa-range.pdf', samplePdf.byteLength, shaHex, nowIso, nowIso);

  let seenUpstreamIfRange: string | null = null;
  let seenUpstreamRange: string | null = null;

  const mockFetch: typeof fetch = async (_input, init) => {
    const hdrs = new Headers(init?.headers);
    seenUpstreamIfRange = hdrs.get('If-Range');
    seenUpstreamRange = hdrs.get('Range');
    const slice = samplePdf.slice(0, 10);
    return new Response(slice, {
      status: 206,
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Range': `bytes 0-9/${samplePdf.byteLength}`,
        'Content-Length': String(slice.byteLength),
      },
    });
  };

  const req = new Request('https://imtihan-pro-api.workers.dev/v1/pdfs/pdf_range_qa_1/download?mode=stream', {
    method: 'GET',
    headers: {
      Range: 'bytes=0-9',
      'If-Range': `"${shaHex}"`,
    },
  });
  const res = await handleWorkerRequest(req, env, undefined, { fetchFn: mockFetch, skipRateLimit: true });
  assert.equal(res.status, 206);
  assert.equal(seenUpstreamRange, 'bytes=0-9');
  assert.equal(seenUpstreamIfRange, null);
  assert.equal(res.headers.get('Content-Range'), `bytes 0-9/${samplePdf.byteLength}`);
  assert.equal((await res.arrayBuffer()).byteLength, 10);
});

test('Quiz submission deduplicates duplicate mcqId answers and enforces 409 on duplicate sessionId', async () => {
  const { env } = createTestD1Env();

  const deduped = gradeQuizSubmission(
    [
      {
        id: 'q1',
        chapterId: 'ch_1',
        subjectId: 'sub_1',
        classLevel: 11,
        question: 'Unit of force?',
        options: [
          { id: 'A', text: 'Newton' },
          { id: 'B', text: 'Joule' },
          { id: 'C', text: 'Watt' },
          { id: 'D', text: 'Pascal' },
        ],
        correctOptionId: 'A',
        explanation: 'Force is measured in Newtons.',
        difficulty: 'easy',
        topicTitle: 'Units',
      },
    ],
    {
      sessionId: 'unit_dedup',
      studentId: null,
      guestDeviceId: 'dev_unit_dedup',
      subjectId: 'sub_1',
      chapterId: 'ch_1',
      answers: [
        { mcqId: 'q1', selectedOptionId: 'A', markedForReview: false },
        { mcqId: 'q1', selectedOptionId: 'A', markedForReview: false },
        { mcqId: 'q1', selectedOptionId: 'A', markedForReview: false },
      ],
      elapsedSeconds: 10,
      submittedAtIso: new Date().toISOString(),
    }
  );
  assert.equal(deduped.totalQuestions, 1);
  assert.equal(deduped.correctCount, 1);
  assert.equal(deduped.xpEarned, 30); // 10 for 1 correct + 20 pass bonus

  const guest = await jsonReq(env, '/v1/auth/guest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ guestDeviceId: 'quiz-dedup-dev', displayName: 'Bilal' }),
  });
  const auth = { 'Content-Type': 'application/json', Authorization: `Bearer ${guest.body.data.token}` };

  const startRes = await jsonReq(env, '/v1/exams/start', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({
      mode: 'chapter_test',
      boardCode: 'KPK',
      classLevel: 11,
      subjectId: 'sub_kpk11_phy',
      chapterId: 'ch_phy11_01',
      questionCount: 2,
    }),
  });
  assert.equal(startRes.status, 200);
  const sessionId = startRes.body.data.sessionId as string;
  const firstMcqId = startRes.body.data.mcqs[0].id as string;

  // Send same mcqId 3 times in answers payload
  const sub1 = await jsonReq(env, '/v1/quizzes/submissions', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({
      sessionId,
      guestDeviceId: 'quiz-dedup-dev',
      subjectId: 'sub_kpk11_phy',
      chapterId: 'ch_phy11_01',
      elapsedSeconds: 30,
      answers: [
        { mcqId: firstMcqId, selectedOptionId: 'A', markedForReview: false },
        { mcqId: firstMcqId, selectedOptionId: 'A', markedForReview: false },
        { mcqId: firstMcqId, selectedOptionId: 'B', markedForReview: false },
      ],
    }),
  });
  assert.equal(sub1.status, 201);
  assert.equal(sub1.body.data.totalQuestions, 1);

  // Re-submitting same sessionId is rejected with 409
  const sub2 = await jsonReq(env, '/v1/quizzes/submissions', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({
      sessionId,
      guestDeviceId: 'quiz-dedup-dev',
      subjectId: 'sub_kpk11_phy',
      chapterId: 'ch_phy11_01',
      elapsedSeconds: 30,
      answers: [{ mcqId: firstMcqId, selectedOptionId: 'A', markedForReview: false }],
    }),
  });
  assert.equal(sub2.status, 409);
  assert.equal(sub2.body.error?.code, 'QUIZ_ALREADY_SUBMITTED');
});

test('Mistake resolve replay and planner task completion replay grant XP/minutes once; PUT planner preserves countdown', async () => {
  const { db, env } = createTestD1Env();

  const guest = await jsonReq(env, '/v1/auth/guest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ guestDeviceId: 'xp-guard-dev', displayName: 'Sana' }),
  });
  const studentId = guest.body.data.user.id as string;
  const auth = { 'Content-Type': 'application/json', Authorization: `Bearer ${guest.body.data.token}` };

  const seededMcq = db
    .prepare('SELECT id, subject_id, chapter_id, correct_option_id FROM mcqs LIMIT 1')
    .get() as { id: string; subject_id: string; chapter_id: string; correct_option_id: string };
  assert.ok(seededMcq?.id);

  // Seed one active mistake
  db.prepare(
    `INSERT INTO student_mistakes (
      id, student_id, mcq_id, subject_id, chapter_id, topic_title,
      question, last_wrong_option_id, correct_option_id, wrong_count, mastered, last_attempted_at_iso
    ) VALUES (?, ?, ?, ?, ?, 'Units', 'Q?', 'B', ?, 1, 0, ?)`
  ).run('msk_test_1', studentId, seededMcq.id, seededMcq.subject_id, seededMcq.chapter_id, seededMcq.correct_option_id, new Date().toISOString());

  const r1 = await jsonReq(env, `/v1/students/me/mistakes/${seededMcq.id}/resolve`, {
    method: 'POST',
    headers: auth,
  });
  assert.equal(r1.status, 200);
  const xpAfter1 = (db.prepare('SELECT xp_total FROM students WHERE id = ?').get(studentId) as { xp_total: number }).xp_total;
  assert.equal(xpAfter1, 15);

  // Replay resolve must NOT add another +15 XP
  const r2 = await jsonReq(env, `/v1/students/me/mistakes/${seededMcq.id}/resolve`, {
    method: 'POST',
    headers: auth,
  });
  assert.equal(r2.status, 200);
  const xpAfter2 = (db.prepare('SELECT xp_total FROM students WHERE id = ?').get(studentId) as { xp_total: number }).xp_total;
  assert.equal(xpAfter2, 15);

  // Planner task custom ID preserved + completion idempotent + PUT omitting examCountdownDateIso preserves date
  const setDate = await jsonReq(env, '/v1/students/me/planner', {
    method: 'PUT',
    headers: auth,
    body: JSON.stringify({ dailyTargetMinutes: 60, examCountdownDateIso: '2027-05-15' }),
  });
  assert.equal(setDate.status, 200);

  const updateTargetOnly = await jsonReq(env, '/v1/students/me/planner', {
    method: 'PUT',
    headers: auth,
    body: JSON.stringify({ dailyTargetMinutes: 90 }),
  });
  assert.equal(updateTargetOnly.status, 200);

  const plannerAfterPut = await jsonReq(env, '/v1/students/me/planner', { headers: auth });
  assert.equal(plannerAfterPut.body.data.dailyTargetMinutes, 90);
  assert.equal(plannerAfterPut.body.data.examCountdownDateIso, '2027-05-15');

  const taskCreate = await jsonReq(env, '/v1/students/me/planner/tasks', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({
      id: 'tsk_client_abc123',
      title: 'Revise Vectors',
      estimatedMinutes: 25,
      taskType: 'revision',
    }),
  });
  assert.equal(taskCreate.status, 201);
  assert.equal(taskCreate.body.data.id, 'tsk_client_abc123');

  const p1 = await jsonReq(env, '/v1/students/me/planner/tasks/tsk_client_abc123', {
    method: 'PATCH',
    headers: auth,
    body: JSON.stringify({ completed: true }),
  });
  assert.equal(p1.status, 200);

  const p2 = await jsonReq(env, '/v1/students/me/planner/tasks/tsk_client_abc123', {
    method: 'PATCH',
    headers: auth,
    body: JSON.stringify({ completed: true }),
  });
  assert.equal(p2.status, 200);

  const xpAfterPlanner = (db.prepare('SELECT xp_total FROM students WHERE id = ?').get(studentId) as { xp_total: number }).xp_total;
  assert.equal(xpAfterPlanner, 30); // 15 from mistake + 15 from single task completion

  const todayYmd = new Date().toISOString().slice(0, 10);
  const minsRow = db
    .prepare('SELECT minutes_logged FROM student_activity_days WHERE student_id = ? AND day_ymd = ?')
    .get(studentId, todayYmd) as { minutes_logged: number };
  assert.equal(minsRow.minutes_logged, 25);
});

test('AI submission promote updates chapter notes flag and rejects replay with 409 ALREADY_PROMOTED', async () => {
  const { db, env } = createTestD1Env();

  const boot = await jsonReq(env, '/v1/admin/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bootstrapSecret: 'test-bootstrap-secret-1234',
      email: 'qa-admin@imtihan.pk',
      displayName: 'QA Admin',
      password: 'StrongPassword123!',
    }),
  });
  assert.equal(boot.status, 201);
  const adminToken = boot.body.data.token as string;
  const adminHeaders = { 'Content-Type': 'application/json', Authorization: `Bearer ${adminToken}` };

  const nowIso = new Date().toISOString();
  db.prepare(
    `INSERT INTO ai_agent_submissions (
      id, agent_name, model_identifier, target_class, board_code,
      subject_id, subject_name, chapter_id, chapter_title, submission_type,
      payload_json, payload_sha256, blocks_count, schema_validation_passed,
      validation_warnings_json, status, reviewer_notes,
      submitted_at_iso, updated_at_iso
    ) VALUES (?, 'qa-agent', 'gemini-pro', 11, 'KPK', 'sub_kpk11_phy', 'Physics', 'ch_phy11_01', 'Measurement', 'detailed_notes', ?, ?, 1, 1, '[]', 'approved', '', ?, ?)`
  ).run(
    'aisub_qa_01',
    JSON.stringify({
      blocks: [{ id: 'blk_qa_1', orderIndex: 1, type: 'paragraph', text: 'Promoted block body' }],
    }),
    'b'.repeat(64),
    nowIso,
    nowIso
  );

  const promote1 = await jsonReq(env, '/v1/admin/ai-submissions/aisub_qa_01/promote', {
    method: 'POST',
    headers: adminHeaders,
  });
  assert.equal(promote1.status, 200);
  assert.ok(promote1.body.data.promotedContentId);

  const chFlag = db
    .prepare('SELECT has_detailed_notes FROM chapters WHERE id = ?')
    .get('ch_phy11_01') as { has_detailed_notes: number };
  assert.equal(chFlag.has_detailed_notes, 1);

  const promote2 = await jsonReq(env, '/v1/admin/ai-submissions/aisub_qa_01/promote', {
    method: 'POST',
    headers: adminHeaders,
  });
  assert.equal(promote2.status, 409);
  assert.equal(promote2.body.error?.code, 'ALREADY_PROMOTED');
});
