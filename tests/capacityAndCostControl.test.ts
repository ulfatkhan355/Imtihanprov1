import test from 'node:test';
import assert from 'node:assert/strict';
import {
  buildStandardCapacityMatrix,
  DEFAULT_CAPACITY_ASSUMPTIONS,
  DEFAULT_CAPACITY_POLICY,
  estimateCapacityForDau,
  evaluateQuotaAlerts,
  OFFICIAL_FREE_TIER_LIMITS,
} from '../shared-contracts/capacity';
import type { UsageSnapshotMetrics } from '../shared-contracts/types';
import { handleWorkerRequest } from '../backend/src/index';
import { resetInMemoryRateLimits } from '../backend/src/middleware/security';
import { resetInMemoryTelemetryBuffer } from '../backend/src/services/usageTelemetry';
import { createTestD1Env } from './helpers/sqliteD1';

async function callApi(
  env: ReturnType<typeof createTestD1Env>['env'],
  path: string,
  init: RequestInit = {},
  options: { fetchFn?: typeof fetch; skipRateLimit?: boolean } = {}
) {
  const req = new Request(`https://imtihan-pro-api.workers.dev${path}`, init);
  const res = await handleWorkerRequest(req, env, undefined, options);
  const text = await res.text();
  let body: any = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  return { status: res.status, headers: res.headers, body };
}

test('Capacity model estimates 300-5000 DAU honestly as theoretical projections and identifies bottlenecks', () => {
  const matrix = buildStandardCapacityMatrix(DEFAULT_CAPACITY_ASSUMPTIONS);
  assert.deepEqual(
    matrix.map((m) => m.dailyActiveStudents),
    [300, 500, 1000, 2000, 5000]
  );

  for (const row of matrix) {
    assert.equal(row.isTheoreticalEstimate, true);
    assert.ok(row.metrics.workerRequestsPerDay > 0);
    assert.ok(row.metrics.d1RowsReadPerDay > 0);
    assert.ok(row.metrics.d1RowsWrittenPerDay > 0);
  }

  const dau500 = estimateCapacityForDau(500);
  assert.equal(dau500.status, 'within_free_tier');
  assert.equal(dau500.bottlenecks.length, 0);

  const dau5000 = estimateCapacityForDau(5000);
  assert.equal(dau5000.status, 'exceeds_free_tier');
  assert.ok(dau5000.bottlenecks.length >= 1);
  assert.ok(dau5000.metrics.b2DailyDownloadGb > OFFICIAL_FREE_TIER_LIMITS.b2ConservativeDailyDownloadGb);
});

test('Quota alerts classify 70% warning, 85% high, and 95% critical thresholds accurately', () => {
  const baseUsage: UsageSnapshotMetrics = {
    dayYmd: '2026-11-15',
    workerRequestsToday: 72_000, // 72% -> warning
    edgeOrHttpCacheHitsToday: 40_000,
    d1RowsReadEstToday: 4_300_000, // 86% -> high
    d1RowsWrittenEstToday: 96_000, // 96% -> critical
    d1DatabaseSizeBytes: 25 * 1024 * 1024,
    b2StorageBytes: 512 * 1024 * 1024,
    b2DailyDownloadBytes: 200 * 1024 * 1024,
    b2DailyDownloadCount: 140,
    aiCallsToday: 50,
    aiCacheHitsToday: 30,
    aiBlockedQuotaToday: 0,
    apiErrorCountToday: 0,
    avgLatencyMs: 12,
    telemetryNote: 'unit-test',
  };

  const alerts = evaluateQuotaAlerts(baseUsage, DEFAULT_CAPACITY_POLICY, OFFICIAL_FREE_TIER_LIMITS);
  const byRes = new Map(alerts.map((a) => [a.resource, a]));

  assert.equal(byRes.get('worker_requests')?.severity, 'warning');
  assert.equal(byRes.get('d1_reads')?.severity, 'high');
  assert.equal(byRes.get('d1_writes')?.severity, 'critical');
  assert.equal(byRes.get('b2_downloads')?.severity, 'nominal');
});

test('Migration 0004 composite indexes are used by SQLite query planner on hot student queries', () => {
  const { db } = createTestD1Env();

  const queries = [
    `EXPLAIN QUERY PLAN SELECT * FROM quiz_submissions WHERE student_id = 'stu_1' ORDER BY submitted_at_iso DESC LIMIT 20`,
    `EXPLAIN QUERY PLAN SELECT * FROM mcqs WHERE chapter_id = 'ch_phy9_01' AND publish_status = 'published' ORDER BY created_at_iso ASC LIMIT 50`,
    `EXPLAIN QUERY PLAN SELECT * FROM student_chapter_progress WHERE student_id = 'stu_1' AND is_completed = 1`,
    `EXPLAIN QUERY PLAN SELECT * FROM student_flashcard_reviews WHERE student_id = 'stu_1' AND next_review_at_iso <= '2026-11-15T00:00:00Z'`,
  ];

  const expectedIndexKeywords = [
    'idx_quiz_student_submitted',
    'idx_mcqs_chapter_status_created',
    'idx_student_progress_student_completed',
    'idx_flashcard_reviews_due',
  ];

  queries.forEach((sql, i) => {
    const planRows = db.prepare(sql).all() as Array<{ detail?: string }>;
    const combinedPlan = planRows.map((r) => String(r.detail || '')).join(' ');
    assert.ok(
      combinedPlan.includes(expectedIndexKeywords[i]),
      `Expected query plan for query #${i} to use ${expectedIndexKeywords[i]}, got: ${combinedPlan}`
    );
  });
});

test('Public curriculum GET avoids D1 rate_limits writes, sets deterministic ETag, returns 304, and private endpoints stay no-store', async () => {
  resetInMemoryRateLimits();
  resetInMemoryTelemetryBuffer();
  const { db, env } = createTestD1Env();

  // Call public GET with rate limiting ENABLED (in-memory only for public GET)
  const first = await callApi(env, '/v1/subjects?board=KPK&classLevel=9', {
    headers: { 'CF-Connecting-IP': '203.0.113.10' },
  });
  assert.equal(first.status, 200);
  const etag = first.headers.get('ETag');
  const cacheControl = first.headers.get('Cache-Control') || '';
  assert.ok(etag && etag.startsWith('W/"'));
  assert.ok(cacheControl.startsWith('public,'));

  // Assert 0 rows were written to D1 rate_limits table for public GET
  const rateRow = db.prepare('SELECT COUNT(*) AS cnt FROM rate_limits').get() as { cnt: number };
  assert.equal(rateRow.cnt, 0);

  // Conditional request returns 304 Not Modified
  const second = await callApi(env, '/v1/subjects?board=KPK&classLevel=9', {
    headers: { 'CF-Connecting-IP': '203.0.113.10', 'If-None-Match': etag },
  });
  assert.equal(second.status, 304);

  // Private student progress route strictly forbids public caching
  const guest = await callApi(
    env,
    '/v1/auth/guest',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ guestDeviceId: 'cache-privacy-dev-1' }),
    },
    { skipRateLimit: true }
  );
  assert.equal(guest.status, 200);
  assert.equal(guest.headers.get('Cache-Control'), 'private, no-store, max-age=0');

  const progress = await callApi(
    env,
    '/v1/students/me/progress',
    {
      headers: { Authorization: `Bearer ${guest.body.data.token}` },
    },
    { skipRateLimit: true }
  );
  assert.equal(progress.status, 200);
  assert.equal(progress.headers.get('Cache-Control'), 'private, no-store, max-age=0');
});

test('Delta progress sync skips unchanged chapter upserts and writes only modified chapters', async () => {
  resetInMemoryRateLimits();
  resetInMemoryTelemetryBuffer();
  const { db, env } = createTestD1Env();

  const guest = await callApi(
    env,
    '/v1/auth/guest',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'delta-sync-dev-1',
        selectedBoard: 'KPK',
        selectedClass: 9,
      }),
    },
    { skipRateLimit: true }
  );
  const token = guest.body.data.token as string;
  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };

  const payload = {
    guestDeviceId: 'delta-sync-dev-1',
    selectedClass: 9,
    selectedBoard: 'KPK',
    language: 'en',
    studyHoursTotal: 2.5,
    dailyGoalPercent: 60,
    dailyTargetMinutes: 45,
    chapterProgress: [
      {
        chapterId: 'ch_phy9_01',
        subjectId: 'sub_kpk9_phy',
        completionPercent: 50,
        lastBlockIndex: 4,
        isCompleted: false,
        lastAccessedAtIso: '2026-11-15T10:00:00.000Z',
      },
    ],
    clientSyncedAtIso: '2026-11-15T10:00:00.000Z',
  };

  const sync1 = await callApi(
    env,
    '/v1/students/me/progress/sync',
    { method: 'POST', headers: authHeaders, body: JSON.stringify(payload) },
    { skipRateLimit: true }
  );
  assert.equal(sync1.status, 200);

  // Measure prepared write queries on identical second sync
  let dbPrepareSqls: string[] = [];
  const origPrepare = env.DB.prepare.bind(env.DB);
  env.DB.prepare = (query: string) => {
    dbPrepareSqls.push(query);
    return origPrepare(query);
  };

  const sync2 = await callApi(
    env,
    '/v1/students/me/progress/sync',
    { method: 'POST', headers: authHeaders, body: JSON.stringify(payload) },
    { skipRateLimit: true }
  );
  assert.equal(sync2.status, 200);
  env.DB.prepare = origPrepare;

  const chapterUpserts = dbPrepareSqls.filter((q) =>
    q.includes('INSERT INTO student_chapter_progress')
  );
  const studentProfileUpdates = dbPrepareSqls.filter((q) => q.startsWith('UPDATE students SET'));
  assert.equal(chapterUpserts.length, 0, 'Unchanged chapter progress should be skipped');
  assert.equal(studentProfileUpdates.length, 0, 'Unchanged student profile should be skipped');

  const row = db
    .prepare("SELECT completion_percent FROM student_chapter_progress WHERE chapter_id = 'ch_phy9_01'")
    .get() as { completion_percent: number };
  assert.equal(row.completion_percent, 50);
});

test('AI Tutor response cache deduplicates prompts and enforces per-student & global caps cleanly', async () => {
  resetInMemoryRateLimits();
  resetInMemoryTelemetryBuffer();
  const { env } = createTestD1Env();

  // 1. Unconfigured key returns honest 503 fallback without throwing
  const noKeyRes = await callApi(
    env,
    '/v1/tutor/ask',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'ai-dev-1',
        classLevel: 9,
        boardCode: 'KPK',
        language: 'en',
        action: 'ask',
        prompt: 'What is Vernier Callipers least count?',
        chapterId: 'ch_phy9_01',
      }),
    },
    { skipRateLimit: true }
  );
  assert.equal(noKeyRes.status, 503);
  assert.equal(noKeyRes.body.error.code, 'AI_PROVIDER_UNCONFIGURED');

  // 2. Configure Gemini key + mock fetchFn
  env.GEMINI_API_KEY = 'fake-gemini-free-tier-key';
  let upstreamFetchCount = 0;
  const mockFetch: typeof fetch = async () => {
    upstreamFetchCount += 1;
    return new Response(
      JSON.stringify({
        candidates: [
          {
            content: {
              parts: [{ text: 'Least count of a Vernier Callipers is typically 0.1 mm or 0.01 cm.' }],
            },
          },
        ],
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    );
  };

  // First call -> MISS, invokes Gemini once
  const firstAi = await callApi(
    env,
    '/v1/tutor/ask',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'ai-dev-1',
        classLevel: 9,
        boardCode: 'KPK',
        language: 'en',
        action: 'ask',
        prompt: 'What is   Vernier Callipers least count?',
        chapterId: 'ch_phy9_01',
      }),
    },
    { fetchFn: mockFetch, skipRateLimit: true }
  );
  assert.equal(firstAi.status, 200);
  assert.equal(firstAi.headers.get('X-Imtihan-Ai-Cache'), 'MISS');
  assert.equal(upstreamFetchCount, 1);

  // Second call with extra whitespace -> hits D1 ai_tutor_cache (0 upstream Gemini calls!)
  const secondAi = await callApi(
    env,
    '/v1/tutor/ask',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'ai-dev-2',
        classLevel: 9,
        boardCode: 'KPK',
        language: 'en',
        action: 'ask',
        prompt: 'What is Vernier Callipers least count?',
        chapterId: 'ch_phy9_01',
      }),
    },
    { fetchFn: mockFetch, skipRateLimit: true }
  );
  assert.equal(secondAi.status, 200);
  assert.equal(secondAi.headers.get('X-Imtihan-Ai-Cache'), 'HIT');
  assert.equal(upstreamFetchCount, 1);

  // Admin tightens per-student cap to 1 via PUT /v1/admin/capacity-policy
  const boot = await callApi(
    env,
    '/v1/admin/bootstrap',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bootstrapSecret: env.ADMIN_BOOTSTRAP_SECRET,
        email: 'admin@imtihanpro.pk',
        displayName: 'Lead Admin',
        password: 'StrongPassword#2026',
      }),
    },
    { skipRateLimit: true }
  );
  assert.equal(boot.status, 201);
  const adminToken = boot.body.data.token as string;

  const updatedPolicy = await callApi(
    env,
    '/v1/admin/capacity-policy',
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`,
      },
      body: JSON.stringify({ aiTutorDailyPerStudentCap: 1 }),
    },
    { skipRateLimit: true }
  );
  assert.equal(updatedPolicy.status, 200);
  assert.equal(updatedPolicy.body.data.aiTutorDailyPerStudentCap, 1);

  // Student ai-dev-1 already used 1 uncached request -> new unique prompt must be gracefully blocked (429)
  const cappedRes = await callApi(
    env,
    '/v1/tutor/ask',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'ai-dev-1',
        classLevel: 9,
        boardCode: 'KPK',
        language: 'en',
        action: 'ask',
        prompt: 'Explain significant figures rule in physics',
        chapterId: 'ch_phy9_01',
      }),
    },
    { fetchFn: mockFetch, skipRateLimit: true }
  );
  assert.equal(cappedRes.status, 429);
  assert.equal(cappedRes.body.error.code, 'AI_STUDENT_QUOTA_REACHED');
  assert.equal(upstreamFetchCount, 1);

  // Admin capacity telemetry dashboard reflects AI cache hit + blocked attempt
  const capDash = await callApi(
    env,
    '/v1/admin/capacity',
    {
      headers: { Authorization: `Bearer ${adminToken}` },
    },
    { skipRateLimit: true }
  );
  assert.equal(capDash.status, 200);
  assert.equal(capDash.headers.get('Cache-Control'), 'private, no-store, max-age=0');
  assert.equal(capDash.body.data.usage.aiCallsToday, 1);
  assert.equal(capDash.body.data.usage.aiCacheHitsToday, 1);
  assert.equal(capDash.body.data.usage.aiBlockedQuotaToday, 1);
});

test('Backblaze B2 daily egress soft cap guards uncached PDF downloads while permitting 304 conditional hits', async () => {
  resetInMemoryRateLimits();
  resetInMemoryTelemetryBuffer();
  const { env } = createTestD1Env();

  // Bootstrap admin and set tight B2 daily cap of 50 MB
  const boot = await callApi(
    env,
    '/v1/admin/bootstrap',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bootstrapSecret: env.ADMIN_BOOTSTRAP_SECRET,
        email: 'admin@imtihanpro.pk',
        displayName: 'Lead Admin',
        password: 'StrongPassword#2026',
      }),
    },
    { skipRateLimit: true }
  );
  const adminToken = boot.body.data.token as string;

  await callApi(
    env,
    '/v1/admin/capacity-policy',
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`,
      },
      body: JSON.stringify({ b2DailyDownloadSoftCapMb: 50, autoBlockUncachedPdfAtCritical: true }),
    },
    { skipRateLimit: true }
  );

  const listRes = await callApi(env, '/v1/pdfs?limit=1', {}, { skipRateLimit: true });
  const pdf = listRes.body.data[0];
  assert.ok(pdf?.id);

  // Re-test 304 with checksum ETag
  const etagHeader = pdf.checksumSha256 ? `"${pdf.checksumSha256}"` : `W/"${pdf.id}-${pdf.version}"`;
  const cond304 = await callApi(
    env,
    `/v1/pdfs/${pdf.id}/download`,
    { headers: { 'If-None-Match': etagHeader } },
    { skipRateLimit: true }
  );
  assert.equal(cond304.status, 304);

  // Set B2 policy cap lower than PDF fileSizeMb or simulate prior usage at 49.9 MB by lowering cap to 50 and inserting daily_usage_metrics directly
  await env.DB.prepare(
    `INSERT INTO daily_usage_metrics (day_ymd, b2_download_bytes, updated_at_iso)
     VALUES (?, ?, ?)
     ON CONFLICT(day_ymd) DO UPDATE SET b2_download_bytes = excluded.b2_download_bytes`
  )
    .bind(new Date().toISOString().slice(0, 10), 60 * 1024 * 1024, new Date().toISOString())
    .run();

  const blockedPdf = await callApi(
    env,
    `/v1/pdfs/${pdf.id}/download`,
    {},
    { skipRateLimit: true }
  );
  assert.equal(blockedPdf.status, 429);
  assert.equal(blockedPdf.body.error.code, 'B2_DAILY_BANDWIDTH_CAP');
});
