import test from 'node:test';
import assert from 'node:assert/strict';
import { handleWorkerRequest } from '../backend/src/index';
import { createTestD1Env } from './helpers/sqliteD1';

async function jsonReq(
  env: ReturnType<typeof createTestD1Env>['env'],
  path: string,
  init: RequestInit = {},
  options: { skipRateLimit?: boolean } = { skipRateLimit: true }
) {
  const req = new Request(`https://imtihan-pro-api.workers.dev${path}`, init);
  const res = await handleWorkerRequest(req, env, undefined, options);
  const body = (await res.json()) as {
    ok: boolean;
    data: any;
    error?: { code: string; message: string };
  };
  return { status: res.status, headers: res.headers, body };
}

test('public health, boards, classes, subjects, chapters & published-only content rules', async () => {
  const { env } = createTestD1Env();

  const health = await jsonReq(env, '/v1/health');
  assert.equal(health.status, 200);
  assert.equal(health.body.data.d1Connected, true);
  assert.equal(health.body.data.b2Bucket, 'imtihan-pro-assets');

  const subjects = await jsonReq(env, '/v1/subjects?board=KPK&classLevel=10');
  assert.equal(subjects.status, 200);
  assert.ok(subjects.body.data.length >= 6);

  // Published chapter returns detailed_notes cleanly
  const published = await jsonReq(env, '/v1/chapters/ch_phy10_01/content?mode=detailed_notes');
  assert.equal(published.status, 200);
  assert.equal(published.body.data.publishStatus, 'published');
  assert.ok(published.body.data.blocks.length >= 3);

  // Draft/unpublished chapter returns 404 CONTENT_NOT_PUBLISHED
  const draft = await jsonReq(env, '/v1/chapters/ch_phy10_04/content?mode=detailed_notes');
  assert.equal(draft.status, 404);
  assert.equal(draft.body.error?.code, 'CONTENT_NOT_PUBLISHED');
});

test('admin bootstrap, PBKDF2 login, RBAC enforcement, and second-bootstrap lock', async () => {
  const { env } = createTestD1Env();

  const statusBefore = await jsonReq(env, '/v1/admin/bootstrap-status');
  assert.equal(statusBefore.status, 200);
  assert.equal(statusBefore.body.data.hasAdmin, false);

  // Reject invalid bootstrap secret
  const badSecret = await jsonReq(env, '/v1/admin/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bootstrapSecret: 'wrong-secret',
      email: 'admin@imtihan.pk',
      displayName: 'Lead Admin',
      password: 'StrongPassword123!',
    }),
  });
  assert.equal(badSecret.status, 403);

  // Reject weak password
  const weakPw = await jsonReq(env, '/v1/admin/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bootstrapSecret: 'test-bootstrap-secret-1234',
      email: 'admin@imtihan.pk',
      displayName: 'Lead Admin',
      password: 'short',
    }),
  });
  assert.equal(weakPw.status, 400);

  // Valid bootstrap creates first super_admin and mints token
  const bootstrapped = await jsonReq(env, '/v1/admin/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bootstrapSecret: 'test-bootstrap-secret-1234',
      email: 'admin@imtihan.pk',
      displayName: 'Lead Admin',
      password: 'StrongPassword123!',
    }),
  });
  assert.equal(bootstrapped.status, 201);
  assert.ok(bootstrapped.body.data.token);
  assert.equal(bootstrapped.body.data.admin.role, 'super_admin');

  // Second bootstrap is locked out with 409
  const second = await jsonReq(env, '/v1/admin/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bootstrapSecret: 'test-bootstrap-secret-1234',
      email: 'hacker@imtihan.pk',
      displayName: 'Attacker',
      password: 'AnotherStrongPassword123!',
    }),
  });
  assert.equal(second.status, 409);

  // Unauthenticated admin request fails with 401
  const unauth = await jsonReq(env, '/v1/admin/overview');
  assert.equal(unauth.status, 401);

  // Student bearer token is rejected on admin endpoints with 403
  const guestAuth = await jsonReq(env, '/v1/auth/guest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ guestDeviceId: 'guest-dev-1', selectedClass: 10, selectedBoard: 'KPK' }),
  });
  assert.equal(guestAuth.status, 200);
  const studentForbidden = await jsonReq(env, '/v1/admin/overview', {
    headers: { Authorization: `Bearer ${guestAuth.body.data.token}` },
  });
  assert.equal(studentForbidden.status, 403);

  // Wrong admin password rejected
  const badLogin = await jsonReq(env, '/v1/admin/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@imtihan.pk', password: 'WrongPassword!' }),
  });
  assert.equal(badLogin.status, 401);

  // Valid admin login succeeds
  const login = await jsonReq(env, '/v1/admin/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@imtihan.pk', password: 'StrongPassword123!' }),
  });
  assert.equal(login.status, 200);
  assert.ok(login.body.data.token);
});

test('admin subject/chapter CRUD + content draft save + publish version updates public endpoint', async () => {
  const { env } = createTestD1Env();
  const boot = await jsonReq(env, '/v1/admin/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bootstrapSecret: 'test-bootstrap-secret-1234',
      email: 'admin@imtihan.pk',
      displayName: 'Editor',
      password: 'StrongPassword123!',
    }),
  });
  const token = boot.body.data.token as string;
  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };

  // Create subject (auto-resolving classId for KPK Class 11)
  const subRes = await jsonReq(env, '/v1/admin/subjects', {
    method: 'POST',
    headers: authHeaders,
    body: JSON.stringify({
      classLevel: 11,
      boardCode: 'KPK',
      code: 'CS-11',
      name: 'Computer Science (HSSC-I)',
      iconKey: 'cs',
      totalChapters: 8,
    }),
  });
  assert.equal(subRes.status, 201);
  const subId = subRes.body.data.id as string;

  // Create chapter
  const chRes = await jsonReq(env, '/v1/admin/chapters', {
    method: 'POST',
    headers: authHeaders,
    body: JSON.stringify({
      subjectId: subId,
      chapterNumber: 1,
      title: 'Operating Systems Overview',
      subtitle: 'Processes, threads & memory management',
      isLockedByDefault: false,
    }),
  });
  assert.equal(chRes.status, 201);
  const chId = chRes.body.data.id as string;

  // Save draft -> public endpoint still returns 404 until published
  const draftRes = await jsonReq(env, '/v1/admin/content/drafts', {
    method: 'POST',
    headers: authHeaders,
    body: JSON.stringify({
      chapterId: chId,
      mode: 'detailed_notes',
      blocks: [
        { id: 'b1', orderIndex: 1, type: 'heading', sectionCode: '1.1', text: 'Kernel & Shell', isSubheading: false },
        { id: 'b2', orderIndex: 2, type: 'paragraph', text: 'An operating system manages hardware resources.' },
      ],
    }),
  });
  assert.equal(draftRes.status, 201);
  assert.equal(draftRes.body.data.publishStatus, 'draft');
  const contentId = draftRes.body.data.id as string;

  const beforePublish = await jsonReq(env, `/v1/chapters/${chId}/content?mode=detailed_notes`);
  assert.equal(beforePublish.status, 404);

  // Publish version
  const pubRes = await jsonReq(env, `/v1/admin/content/${contentId}/publish`, {
    method: 'POST',
    headers: authHeaders,
    body: JSON.stringify({}),
  });
  assert.equal(pubRes.status, 200);
  assert.equal(pubRes.body.data.publishStatus, 'published');
  assert.equal(pubRes.body.data.version, 'v0.2.0');

  const afterPublish = await jsonReq(env, `/v1/chapters/${chId}/content?mode=detailed_notes`);
  assert.equal(afterPublish.status, 200);
  assert.equal(afterPublish.body.data.blocks.length, 2);
});

test('AI agent ingest requires secret, detects duplicates, cannot self-publish, and promotes to draft on admin approval', async () => {
  const { env } = createTestD1Env();

  // Reject wrong secret with 403
  const unauthAgent = await jsonReq(env, '/v1/agent/submissions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Agent-Secret': 'wrong' },
    body: JSON.stringify({}),
  });
  assert.equal(unauthAgent.status, 403);

  const payload = {
    agentName: 'syllabus-agent-v2',
    modelIdentifier: 'gemini-2.5-pro',
    boardCode: 'KPK',
    targetClass: 10,
    subjectId: 'sub_kpk10_phy',
    chapterTitle: 'Geometrical Optics',
    chapterId: 'ch_phy10_03',
    submissionType: 'detailed_notes',
    blocks: [
      { id: 'ag_1', orderIndex: 1, type: 'heading', sectionCode: '12.1', text: 'Spherical Mirrors', isSubheading: false },
      { id: 'ag_2', orderIndex: 2, type: 'math_expression', latex: '1/f = 1/p + 1/q', plainNotation: '1/f = 1/p + 1/q', caption: 'Mirror equation' },
    ],
  };

  const ingested = await jsonReq(env, '/v1/agent/submissions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Agent-Secret': 'test-agent-ingest-secret-5678',
    },
    body: JSON.stringify(payload),
  });
  assert.equal(ingested.status, 201);
  assert.equal(ingested.body.data.status, 'queued');
  assert.equal(ingested.body.data.duplicateDetected, false);
  const subId = ingested.body.data.submissionId as string;

  // Duplicate payload flags duplicateDetected
  const dup = await jsonReq(env, '/v1/agent/submissions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Agent-Secret': 'test-agent-ingest-secret-5678',
    },
    body: JSON.stringify(payload),
  });
  assert.equal(dup.status, 201);
  assert.equal(dup.body.data.duplicateDetected, true);

  // Admin approves & promotes to draft
  const boot = await jsonReq(env, '/v1/admin/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bootstrapSecret: 'test-bootstrap-secret-1234',
      email: 'admin@imtihan.pk',
      displayName: 'Reviewer',
      password: 'StrongPassword123!',
    }),
  });
  const token = boot.body.data.token as string;

  const approveRes = await jsonReq(env, `/v1/admin/ai-submissions/${subId}/decision`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ status: 'approved', reviewerNotes: 'Verified equations cleanly.' }),
  });
  assert.equal(approveRes.status, 200);
  assert.equal(approveRes.body.data.status, 'approved');

  const promoteRes = await jsonReq(env, `/v1/admin/ai-submissions/${subId}/promote`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  });
  assert.equal(promoteRes.status, 200);
  assert.ok(promoteRes.body.data.promotedContentId);
});

test('rate limiter enforces 429 when burst threshold is exceeded', async () => {
  const { env } = createTestD1Env();
  let hit429 = false;
  for (let i = 0; i < 35; i++) {
    const res = await jsonReq(
      env,
      '/v1/agent/submissions',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Agent-Secret': 'bad',
          'CF-Connecting-IP': '203.0.113.10',
        },
        body: '{}',
      },
      { skipRateLimit: false }
    );
    if (res.status === 429) {
      hit429 = true;
      assert.equal(res.body.error?.code, 'RATE_LIMITED');
      break;
    }
  }
  assert.equal(hit429, true);
});
