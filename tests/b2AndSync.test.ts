import test from 'node:test';
import assert from 'node:assert/strict';
import { handleWorkerRequest } from '../backend/src/index';
import {
  buildB2ObjectKey,
  generateB2PresignedGetUrl,
  resolveB2Config,
  signB2Request,
  validatePdfUploadBytes,
} from '../backend/src/storage/backblazeB2';
import { createTestD1Env } from './helpers/sqliteD1';

test('Backblaze B2 SigV4 signing, presigned URL generation, and PDF magic-byte validation', async () => {
  const { env } = createTestD1Env();
  const cfg = resolveB2Config(env);

  const key = buildB2ObjectKey('KPK', 10, 'Physics', '2024 Annual Paper.pdf');
  assert.equal(key, 'kpk/class-10/physics/2024-annual-paper.pdf');

  const signed = await signB2Request({
    method: 'PUT',
    objectKey: key,
    cfg,
    payloadSha256Hex: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    contentType: 'application/pdf',
    now: new Date('2026-11-14T12:00:00Z'),
  });
  assert.ok(signed.url.startsWith('https://s3.eu-central-003.backblazeb2.com/imtihan-pro-assets/'));
  assert.ok(signed.headers.Authorization.startsWith('AWS4-HMAC-SHA256 Credential=test-b2-key-id/20261114/eu-central-003/s3/aws4_request'));
  assert.equal(signed.headers['x-amz-date'], '20261114T120000Z');

  const presigned = await generateB2PresignedGetUrl(key, env, 900, new Date('2026-11-14T12:00:00Z'));
  assert.ok(presigned);
  assert.ok(presigned!.includes('X-Amz-Algorithm=AWS4-HMAC-SHA256'));
  assert.ok(presigned!.includes('X-Amz-Signature='));

  const invalidPdf = validatePdfUploadBytes(new TextEncoder().encode('HELLO WORLD'), 'application/pdf', 'fake.pdf');
  assert.equal(invalidPdf.valid, false);

  const validPdf = validatePdfUploadBytes(new TextEncoder().encode('%PDF-1.5\n%%EOF'), 'application/pdf', 'real.pdf');
  assert.equal(validPdf.valid, true);
});

test('Admin PDF upload streams to mocked Backblaze B2, saves D1 metadata, streams download, and cleans up on delete', async () => {
  const { env } = createTestD1Env();
  const b2Store = new Map<string, Uint8Array>();

  const mockFetch: typeof fetch = async (input, init) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    const method = (init?.method || 'GET').toUpperCase();

    if (url.startsWith('https://s3.eu-central-003.backblazeb2.com/imtihan-pro-assets')) {
      const parsed = new URL(url);
      const objectPath = parsed.pathname.replace(/^\/imtihan-pro-assets\/?/, '');
      if (method === 'PUT') {
        const buf = new Uint8Array(await new Response(init?.body as BodyInit).arrayBuffer());
        b2Store.set(objectPath, buf);
        return new Response(null, { status: 200 });
      }
      if (method === 'GET') {
        if (!objectPath) {
          return new Response('<ListBucketResult></ListBucketResult>', { status: 200 });
        }
        const bytes = b2Store.get(objectPath);
        if (!bytes) {
          return new Response('NoSuchKey', { status: 404 });
        }
        return new Response(new Uint8Array(bytes), {
          status: 200,
          headers: { 'Content-Type': 'application/pdf', 'Content-Length': String(bytes.byteLength) },
        });
      }
      if (method === 'DELETE') {
        b2Store.delete(objectPath);
        return new Response(null, { status: 204 });
      }
    }
    throw new Error(`Unexpected external fetch in test: ${method} ${url}`);
  };

  const bootRes = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/admin/bootstrap', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bootstrapSecret: 'test-bootstrap-secret-1234',
        email: 'admin@imtihan.pk',
        displayName: 'B2 Admin',
        password: 'StrongPassword123!',
      }),
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  const token = ((await bootRes.json()) as any).data.token as string;

  // Reject non-PDF file upload cleanly (422 INVALID_PDF_FILE)
  const badForm = new FormData();
  badForm.set('file', new Blob(['NOT_A_PDF'], { type: 'text/plain' }), 'notes.txt');
  badForm.set('subjectId', 'sub_kpk10_phy');
  badForm.set('title', 'Bad File');
  badForm.set('year', '2024');
  const badUploadRes = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/admin/pdfs', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: badForm,
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(badUploadRes.status, 422);

  // Upload real PDF bytes to B2 + D1
  const pdfBytes = new TextEncoder().encode('%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF');
  const goodForm = new FormData();
  goodForm.set('file', new Blob([pdfBytes], { type: 'application/pdf' }), 'physics-2024-solved.pdf');
  goodForm.set('subjectId', 'sub_kpk10_phy');
  goodForm.set('title', 'KPK Physics 10 Solved 2024');
  goodForm.set('year', '2024');
  goodForm.set('session', 'Annual');
  goodForm.set('isSolved', 'true');
  goodForm.set('pageCount', '12');

  const uploadRes = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/admin/pdfs', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: goodForm,
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(uploadRes.status, 201);
  const uploadData = ((await uploadRes.json()) as any).data;
  const pdfId = uploadData.pdf.id as string;
  const objectKey = uploadData.pdf.objectKey as string;
  assert.ok(objectKey.startsWith('kpk/class-10/physics/2024-annual-'));
  assert.ok(objectKey.endsWith('.pdf'));
  assert.ok(b2Store.has(objectKey));

  // Stream download via Worker proxy
  const downloadRes = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download?mode=stream`),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(downloadRes.status, 200);
  assert.equal(downloadRes.headers.get('Content-Type'), 'application/pdf');
  const downloadedText = await downloadRes.text();
  assert.ok(downloadedText.startsWith('%PDF-1.4'));

  // Delete cleans up both B2 and D1
  const delRes = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/admin/pdfs/${pdfId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(delRes.status, 200);
  assert.equal(b2Store.has(objectKey), false);
});

test('Guest-first progress sync merges monotonically and Google ID token exchange links existing guest progress', async () => {
  const { env } = createTestD1Env();

  const validToken = 'valid-google-id-token-0123456789';
  const mockGoogleFetch: typeof fetch = async (input) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    if (url.startsWith('https://oauth2.googleapis.com/tokeninfo')) {
      const parsed = new URL(url);
      const idToken = parsed.searchParams.get('id_token');
      if (idToken === validToken) {
        return new Response(
          JSON.stringify({
            sub: 'google-uid-999',
            email: 'student@example.com',
            email_verified: 'true',
            name: 'Ayesha Khan',
            aud: '1234567890-testclient.apps.googleusercontent.com',
            exp: String(Math.floor(Date.now() / 1000) + 3600),
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        );
      }
      return new Response(JSON.stringify({ error_description: 'Invalid Value' }), { status: 400 });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  };

  // 1. Guest session & local chapter progress sync
  const sync1 = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/students/me/progress/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'guest-phone-abc',
        selectedClass: 10,
        selectedBoard: 'KPK',
        studyHoursTotal: 1.5,
        dailyGoalPercent: 70,
        chapterProgress: [
          {
            chapterId: 'ch_phy10_01',
            subjectId: 'sub_kpk10_phy',
            completionPercent: 80,
            lastBlockIndex: 6,
            isCompleted: false,
            lastAccessedAtIso: '2026-11-14T10:00:00Z',
          },
        ],
      }),
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockGoogleFetch }
  );
  assert.equal(sync1.status, 200);
  assert.equal(((await sync1.json()) as any).data.chapterProgress[0].completionPercent, 80);

  // 2. Older stale client sending lower 40% cannot regress 80% in D1
  const syncStale = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/students/me/progress/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        guestDeviceId: 'guest-phone-abc',
        selectedClass: 10,
        selectedBoard: 'KPK',
        studyHoursTotal: 1.0,
        dailyGoalPercent: 70,
        chapterProgress: [
          {
            chapterId: 'ch_phy10_01',
            subjectId: 'sub_kpk10_phy',
            completionPercent: 40,
            lastBlockIndex: 2,
            isCompleted: false,
            lastAccessedAtIso: '2026-11-14T08:00:00Z',
          },
        ],
      }),
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockGoogleFetch }
  );
  assert.equal(((await syncStale.json()) as any).data.chapterProgress[0].completionPercent, 80);

  // 3. Invalid Google ID token rejected with 401
  const badGoogle = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/auth/google/exchange', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken: 'bad-token-with-length-over-20', guestDeviceId: 'guest-phone-abc' }),
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockGoogleFetch }
  );
  assert.equal(badGoogle.status, 401);

  // 4. Valid Google ID token links existing guest profile & retains 80% progress
  const goodGoogle = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/auth/google/exchange', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken: validToken, guestDeviceId: 'guest-phone-abc' }),
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockGoogleFetch }
  );
  assert.equal(goodGoogle.status, 200);
  const googleSession = ((await goodGoogle.json()) as any).data;
  assert.equal(googleSession.mode, 'google_authenticated');
  assert.equal(googleSession.user.email, 'student@example.com');

  const progressAfterLink = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/students/me/progress', {
      headers: { Authorization: `Bearer ${googleSession.token}` },
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockGoogleFetch }
  );
  assert.equal(progressAfterLink.status, 200);
  assert.equal(((await progressAfterLink.json()) as any).data.chapterProgress[0].completionPercent, 80);
});
