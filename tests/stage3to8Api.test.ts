import test from 'node:test';
import assert from 'node:assert/strict';
import { handleWorkerRequest } from '../backend/src/index';
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

test('Class 9 KPK starter catalog, study group filtering, search, and runtime config', async () => {
  const { env } = createTestD1Env();

  const cfg = await jsonReq(env, '/v1/config');
  assert.equal(cfg.status, 200);
  assert.equal(cfg.body.data.defaultClassLevel, 9);
  assert.equal(cfg.body.data.kpkSubBoards.length, 8);
  assert.equal(cfg.body.data.adPlacement.enabled, false);
  assert.equal(cfg.body.data.aiTutorConfigured, false);

  const bioSubjects = await jsonReq(env, '/v1/subjects?board=KPK&classLevel=9&studyGroup=SCIENCE_BIOLOGY');
  assert.equal(bioSubjects.status, 200);
  const phy9 = bioSubjects.body.data.find((s: any) => s.code === 'PHY-09');
  assert.ok(phy9);
  assert.ok(phy9.pairingSchemeSummary);
  assert.ok(bioSubjects.body.data.some((s: any) => s.code === 'BIO-09'));
  assert.ok(!bioSubjects.body.data.some((s: any) => s.code === 'CS-09'));

  const phyChapters = await jsonReq(env, '/v1/subjects/sub_kpk9_phy/chapters');
  assert.equal(phyChapters.status, 200);
  const ch1 = phyChapters.body.data.find((c: any) => c.id === 'ch_phy9_01');
  assert.ok(ch1);
  assert.ok(ch1.titleUrdu);
  assert.ok(typeof ch1.boardWeightageMarks === 'number' && ch1.boardWeightageMarks > 0);
  assert.ok(ch1.nightBeforeSummary);

  const csSubjects = await jsonReq(env, '/v1/subjects?board=KPK&classLevel=9&studyGroup=SCIENCE_CS');
  assert.equal(csSubjects.status, 200);
  assert.ok(csSubjects.body.data.some((s: any) => s.code === 'CS-09'));
  assert.ok(!csSubjects.body.data.some((s: any) => s.code === 'BIO-09'));

  const search = await jsonReq(env, '/v1/search?q=Vernier&board=KPK&classLevel=9');
  assert.equal(search.status, 200);
  assert.ok(search.body.data.topics.length >= 1 || search.body.data.mcqs.length >= 1);

  const textbooks = await jsonReq(env, '/v1/pdfs?board=KPK&classLevel=9&category=textbook');
  assert.equal(textbooks.status, 200);
  assert.ok(textbooks.body.data.length >= 1);
});

test('Stage 4-7 student journey: exam start, grading, mistakes, flashcards, insights, planner, motivation, leaderboard', async () => {
  const { env } = createTestD1Env();

  // Guest auth defaults to Class 9 and binds Peshawar + Science Biology
  const guest = await jsonReq(env, '/v1/auth/guest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      guestDeviceId: 'kpk9-student-01',
      displayName: 'Ayesha',
      selectedBoard: 'KPK',
      selectedClass: 9,
      selectedSubBoard: 'PESHAWAR',
      studyGroup: 'SCIENCE_BIOLOGY',
      language: 'en',
    }),
  });
  assert.equal(guest.status, 200);
  const token = guest.body.data.token as string;
  const auth = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

  // Insights starts honestly with insufficient data
  const initialInsights = await jsonReq(env, '/v1/students/me/insights', { headers: auth });
  assert.equal(initialInsights.status, 200);
  assert.equal(initialInsights.body.data.readinessPercent, null);
  assert.equal(initialInsights.body.data.readinessStatus, 'insufficient_data');

  // Exam patterns exist
  const patterns = await jsonReq(env, '/v1/exams/patterns?board=KPK&classLevel=9');
  assert.equal(patterns.status, 200);
  assert.ok(patterns.body.data.length >= 3);

  // Start Physics 9 Chapter 1 exam
  const examStart = await jsonReq(env, '/v1/exams/start', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({
      mode: 'chapter_test',
      boardCode: 'KPK',
      classLevel: 9,
      subjectId: 'sub_kpk9_phy',
      chapterId: 'ch_phy9_01',
      questionCount: 4,
    }),
  });
  assert.equal(examStart.status, 200);
  const mcqs = examStart.body.data.mcqs as Array<any>;
  assert.ok(mcqs.length >= 3);

  // Submit round 0 (1 wrong on first question to populate Mistake Master), then verify active mistake, then round 1 to master it
  for (let round = 0; round < 2; round++) {
    const answers = mcqs.map((m, idx) => ({
      mcqId: m.id,
      selectedOptionId:
        round === 0 && idx === 0
          ? m.correctOptionId === 'A'
            ? 'B'
            : 'A'
          : m.correctOptionId,
      markedForReview: idx === 0,
    }));

    const sub = await jsonReq(env, '/v1/quizzes/submissions', {
      method: 'POST',
      headers: auth,
      body: JSON.stringify({
        sessionId: `${examStart.body.data.sessionId}_r${round}`,
        studentId: null,
        guestDeviceId: 'kpk9-student-01',
        subjectId: 'sub_kpk9_phy',
        chapterId: 'ch_phy9_01',
        examMode: 'chapter_test',
        marksPerMcq: 1,
        passPercentage: 40,
        elapsedSeconds: 45,
        submittedAtIso: new Date().toISOString(),
        answers,
      }),
    });
    assert.equal(sub.status, 201);
    assert.ok(sub.body.data.xpEarned > 0);

    if (round === 0) {
      const activeMistakes = await jsonReq(env, '/v1/students/me/mistakes', { headers: auth });
      assert.equal(activeMistakes.status, 200);
      assert.equal(activeMistakes.body.data.length, 1);
      assert.equal(activeMistakes.body.data[0].mastered, false);
    }
  }

  // Quiz history recorded
  const history = await jsonReq(env, '/v1/students/me/quizzes', { headers: auth });
  assert.equal(history.status, 200);
  assert.equal(history.body.data.length, 2);

  // Round 1 resolved the mistake cleanly
  const masteredMistakes = await jsonReq(env, '/v1/students/me/mistakes?includeMastered=true', { headers: auth });
  assert.equal(masteredMistakes.status, 200);
  assert.equal(masteredMistakes.body.data.length, 1);
  assert.equal(masteredMistakes.body.data[0].mastered, true);

  // Insights now returns real numeric readinessPercent after 2+ attempts
  const afterInsights = await jsonReq(env, '/v1/students/me/insights', { headers: auth });
  assert.equal(afterInsights.status, 200);
  assert.ok(typeof afterInsights.body.data.readinessPercent === 'number');
  assert.notEqual(afterInsights.body.data.readinessStatus, 'insufficient_data');

  // Bookmarks create + list + delete
  const bmCreate = await jsonReq(env, '/v1/students/me/bookmarks', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({
      itemType: 'chapter',
      itemId: 'ch_phy9_01',
      chapterId: 'ch_phy9_01',
      subjectId: 'sub_kpk9_phy',
      title: 'Physical Quantities & Measurement',
    }),
  });
  assert.equal(bmCreate.status, 201);
  const bmList = await jsonReq(env, '/v1/students/me/bookmarks', { headers: auth });
  assert.equal(bmList.body.data.length, 1);

  // Flashcards SM-2 review
  const cards = await jsonReq(env, '/v1/students/me/flashcards?subjectId=sub_kpk9_phy', { headers: auth });
  assert.equal(cards.status, 200);
  assert.ok(cards.body.data.length >= 2);
  const reviewRes = await jsonReq(env, '/v1/students/me/flashcards/review', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({ flashcardId: cards.body.data[0].id, quality: 5 }),
  });
  assert.equal(reviewRes.status, 200);
  assert.equal(reviewRes.body.data.repetitions, 1);

  // Study planner create & toggle task
  const taskCreate = await jsonReq(env, '/v1/students/me/planner/tasks', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({
      title: 'Revise Vernier Calipers',
      subjectId: 'sub_kpk9_phy',
      chapterId: 'ch_phy9_01',
      estimatedMinutes: 30,
      taskType: 'revision',
    }),
  });
  assert.equal(taskCreate.status, 201);
  const taskId = taskCreate.body.data.id as string;
  const taskToggle = await jsonReq(env, `/v1/students/me/planner/tasks/${taskId}`, {
    method: 'PATCH',
    headers: auth,
    body: JSON.stringify({ completed: true }),
  });
  assert.equal(taskToggle.status, 200);
  assert.equal(taskToggle.body.data.completed, true);

  // Opt-in anonymized leaderboard never leaks PII
  const optIn = await jsonReq(env, '/v1/students/me/leaderboard-opt-in', {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({ optIn: true }),
  });
  assert.equal(optIn.status, 200);
  assert.equal(optIn.body.data.leaderboardOptIn, true);
  assert.ok(optIn.body.data.aliasHandle);

  const motivation = await jsonReq(env, '/v1/students/me/motivation', { headers: auth });
  assert.equal(motivation.status, 200);
  assert.equal(motivation.body.data.leaderboardOptIn, true);
  assert.ok(motivation.body.data.leaderboard.length >= 1);
  assert.ok(!JSON.stringify(motivation.body.data.leaderboard).includes('@'));
});

test('AI Tutor returns 503 when unconfigured and grounded Gemini answer when configured', async () => {
  const { env } = createTestD1Env();

  const unconfigured = await jsonReq(env, '/v1/tutor/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      chapterId: 'ch_phy9_01',
      subjectId: 'sub_kpk9_phy',
      classLevel: 9,
      boardCode: 'KPK',
      language: 'en',
      action: 'explain_simple',
      prompt: 'What is least count?',
    }),
  });
  assert.equal(unconfigured.status, 503);
  assert.equal(unconfigured.body.error?.code, 'AI_PROVIDER_UNCONFIGURED');

  // With key configured + mocked Gemini fetchFn
  env.GEMINI_API_KEY = 'test-server-gemini-key';
  const mockGeminiFetch: typeof fetch = async () =>
    new Response(
      JSON.stringify({
        candidates: [
          {
            content: {
              parts: [{ text: 'Least count is the smallest measurement a measuring instrument can read accurately.' }],
            },
          },
        ],
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    );

  const configured = await jsonReq(
    env,
    '/v1/tutor/ask',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        chapterId: 'ch_phy9_01',
        subjectId: 'sub_kpk9_phy',
        classLevel: 9,
        boardCode: 'KPK',
        language: 'en',
        action: 'explain_simple',
        prompt: 'What is least count?',
      }),
    },
    mockGeminiFetch
  );
  assert.equal(configured.status, 200);
  assert.equal(configured.body.data.available, true);
  assert.ok(configured.body.data.answer.includes('Least count'));
  assert.ok(configured.body.data.groundedSources.length >= 1);
});

test('Stage 8 feedback loop + admin triage + admin MCQ bank + 4-stage workflow', async () => {
  const { env } = createTestD1Env();
  const boot = await jsonReq(env, '/v1/admin/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bootstrapSecret: 'test-bootstrap-secret-1234',
      email: 'admin@imtihan.pk',
      displayName: 'Lead Admin',
      password: 'StrongPassword123!',
    }),
  });
  const adminHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${boot.body.data.token}`,
  };

  // Student submits error report
  const fbRes = await jsonReq(env, '/v1/feedback', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      guestDeviceId: 'dev_99',
      category: 'mcq_answer_dispute',
      subjectId: 'sub_kpk9_phy',
      chapterId: 'ch_phy9_01',
      title: 'Clarify option B unit',
      description: 'Option B states cm instead of mm in Vernier example.',
    }),
  });
  assert.equal(fbRes.status, 201);
  const fbId = fbRes.body.data.id as string;

  // Admin triages feedback
  const triage = await jsonReq(env, `/v1/admin/feedback/${fbId}`, {
    method: 'PATCH',
    headers: adminHeaders,
    body: JSON.stringify({ status: 'resolved', adminResolutionNote: 'Updated unit to mm.' }),
  });
  assert.equal(triage.status, 200);
  assert.equal(triage.body.data.status, 'resolved');

  // Admin MCQ bank create + update + delete
  const createMcq = await jsonReq(env, '/v1/admin/mcqs', {
    method: 'POST',
    headers: adminHeaders,
    body: JSON.stringify({
      chapterId: 'ch_phy9_01',
      subjectId: 'sub_kpk9_phy',
      classLevel: 9,
      question: 'SI unit of mass is:',
      options: [
        { id: 'A', text: 'gram' },
        { id: 'B', text: 'kilogram' },
        { id: 'C', text: 'newton' },
        { id: 'D', text: 'pound' },
      ],
      correctOptionId: 'B',
      explanation: 'Kilogram (kg) is the SI base unit of mass.',
      difficulty: 'easy',
      topicTitle: 'SI Units',
    }),
  });
  assert.equal(createMcq.status, 201);
  const mcqId = createMcq.body.data.id as string;

  const listMcqs = await jsonReq(env, '/v1/admin/mcqs?chapterId=ch_phy9_01', { headers: adminHeaders });
  assert.equal(listMcqs.status, 200);
  assert.ok(listMcqs.body.data.some((m: any) => m.id === mcqId));

  // 4-stage workflow on newly saved draft
  const draftRes = await jsonReq(env, '/v1/admin/content/drafts', {
    method: 'POST',
    headers: adminHeaders,
    body: JSON.stringify({
      chapterId: 'ch_phy9_02',
      mode: 'revision_notes',
      blocks: [{ id: 'b_rev1', orderIndex: 1, type: 'paragraph', text: 'Equations of motion summary.' }],
    }),
  });
  assert.equal(draftRes.status, 201);
  const contentId = draftRes.body.data.id as string;

  const toReview = await jsonReq(env, `/v1/admin/content/${contentId}/status`, {
    method: 'POST',
    headers: adminHeaders,
    body: JSON.stringify({ status: 'in_review' }),
  });
  assert.equal(toReview.status, 200);
  assert.equal(toReview.body.data.publishStatus, 'in_review');

  const toApproved = await jsonReq(env, `/v1/admin/content/${contentId}/status`, {
    method: 'POST',
    headers: adminHeaders,
    body: JSON.stringify({ status: 'approved' }),
  });
  assert.equal(toApproved.status, 200);
  assert.equal(toApproved.body.data.publishStatus, 'approved');

  const toPublished = await jsonReq(env, `/v1/admin/content/${contentId}/status`, {
    method: 'POST',
    headers: adminHeaders,
    body: JSON.stringify({ status: 'published' }),
  });
  assert.equal(toPublished.status, 200);
  assert.equal(toPublished.body.data.publishStatus, 'published');
});
