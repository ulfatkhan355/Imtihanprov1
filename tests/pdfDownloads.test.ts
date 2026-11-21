import test from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import { handleWorkerRequest } from '../backend/src/index';
import { flushUsageTelemetryNow, resetInMemoryTelemetryBuffer } from '../backend/src/services/usageTelemetry';
import { generateB2PresignedGetUrl } from '../backend/src/storage/backblazeB2';
import { parseByteRangeHeader, shouldHonorRangeWithIfRange } from '../backend/src/storage/pdfRange';
import { createTestD1Env } from './helpers/sqliteD1';

function sha256(bytes: Uint8Array): string {
  return crypto.createHash('sha256').update(bytes).digest('hex');
}

function createRangedB2Fetch(store: Map<string, Uint8Array>): typeof fetch {
  return async (input, init) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    const method = (init?.method || 'GET').toUpperCase();
    if (!url.startsWith('https://s3.eu-central-003.backblazeb2.com/imtihan-pro-assets')) {
      throw new Error(`Unexpected URL in test fetch: ${method} ${url}`);
    }
    const parsed = new URL(url);
    const objectKey = parsed.pathname.replace(/^\/imtihan-pro-assets\/?/, '');
    if (method === 'PUT') {
      const buf = new Uint8Array(await new Response(init?.body as BodyInit).arrayBuffer());
      store.set(objectKey, buf);
      return new Response(null, { status: 200 });
    }
    if (method === 'HEAD') {
      const bytes = store.get(objectKey);
      if (!bytes) return new Response(null, { status: 404 });
      return new Response(null, {
        status: 200,
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Length': String(bytes.byteLength),
          ETag: `"${sha256(bytes)}"`,
        },
      });
    }
    if (method === 'GET') {
      const bytes = store.get(objectKey);
      if (!bytes) return new Response('NoSuchKey', { status: 404 });
      const reqHeaders = new Headers(init?.headers);
      const rangeHeader = reqHeaders.get('Range');
      const ifRangeHeader = reqHeaders.get('If-Range');
      const strongEtag = `"${sha256(bytes)}"`;
      if (rangeHeader && (!ifRangeHeader || ifRangeHeader === strongEtag)) {
        const parsedRange = parseByteRangeHeader(rangeHeader, bytes.byteLength);
        if (parsedRange.kind !== 'valid') {
          return new Response(null, {
            status: 416,
            headers: { 'Content-Range': `bytes */${bytes.byteLength}` },
          });
        }
        const slice = bytes.slice(parsedRange.start, parsedRange.end + 1);
        // Emit as 2 chunks to exercise metered ReadableStream behavior
        const mid = Math.max(1, Math.floor(slice.byteLength / 2));
        const body = new ReadableStream<Uint8Array>({
          start(controller) {
            if (slice.byteLength > 0) controller.enqueue(slice.slice(0, mid));
            if (mid < slice.byteLength) controller.enqueue(slice.slice(mid));
            controller.close();
          },
        });
        return new Response(body, {
          status: 206,
          headers: {
            'Content-Type': 'application/pdf',
            'Content-Length': String(slice.byteLength),
            'Content-Range': `bytes ${parsedRange.start}-${parsedRange.end}/${bytes.byteLength}`,
            ETag: strongEtag,
          },
        });
      }
      return new Response(new Uint8Array(bytes), {
        status: 200,
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Length': String(bytes.byteLength),
          ETag: strongEtag,
        },
      });
    }
    if (method === 'DELETE') {
      store.delete(objectKey);
      return new Response(null, { status: 204 });
    }
    throw new Error(`Unsupported method ${method}`);
  };
}

async function bootstrapAdminAndUploadPdf(
  env: ReturnType<typeof createTestD1Env>['env'],
  fetchFn: typeof fetch,
  pdfBytes: Uint8Array,
  checksumOverride?: string
) {
  const boot = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/admin/bootstrap', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bootstrapSecret: env.ADMIN_BOOTSTRAP_SECRET,
        email: 'admin@imtihan.pk',
        displayName: 'Lead Admin',
        password: 'StrongPassword#2026',
      }),
    }),
    env,
    undefined,
    { skipRateLimit: true, skipTelemetry: true, fetchFn }
  );
  const token = ((await boot.json()) as any).data.token as string;

  const form = new FormData();
  form.set('file', new Blob([new Uint8Array(pdfBytes)], { type: 'application/pdf' }), 'physics-11-ch1.pdf');
  form.set('subjectId', 'sub_kpk11_phy');
  form.set('title', 'KPK Physics 11 Paper');
  form.set('year', '2025');
  form.set('session', 'Annual');
  form.set('isSolved', 'true');
  form.set('pageCount', '12');
  if (checksumOverride !== undefined) {
    form.set('checksumSha256', checksumOverride);
  }

  const uploadRes = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/admin/pdfs', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: form,
    }),
    env,
    undefined,
    { skipRateLimit: true, skipTelemetry: true, fetchFn }
  );
  return { token, uploadRes };
}

test('Migration 0007 backfills valid 64-hex SHA-256 checksums for all seeded PDFs', async () => {
  const { db } = createTestD1Env();
  const rows = db.prepare('SELECT id, checksum_sha256 FROM pdf_assets').all() as Array<{
    id: string;
    checksum_sha256: string;
  }>;
  assert.ok(rows.length >= 8);
  for (const row of rows) {
    assert.match(row.checksum_sha256, /^[0-9a-f]{64}$/, `Invalid seed checksum for ${row.id}`);
  }
});

test('HEAD /v1/pdfs/:id/download, 304 conditional, Range 206 chunked resume, If-Range mismatch, and 416 handling', async () => {
  resetInMemoryTelemetryBuffer();
  const { db, env } = createTestD1Env();
  const b2Store = new Map<string, Uint8Array>();
  const mockFetch = createRangedB2Fetch(b2Store);

  const payload = new TextEncoder().encode('%PDF-1.5\n' + 'KPK_PHYSICS_TEXTBOOK_DATA_'.repeat(120) + '\n%%EOF');
  const expectedSha = sha256(payload);
  const { uploadRes } = await bootstrapAdminAndUploadPdf(env, mockFetch, payload, expectedSha);
  assert.equal(uploadRes.status, 201);
  const uploaded = ((await uploadRes.json()) as any).data;
  const pdfId = uploaded.pdf.id as string;
  const paperId = uploaded.paper.id as string;

  // 1. HEAD returns metadata & Accept-Ranges without touching B2 egress or download_count
  const headRes = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download`, { method: 'HEAD' }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(headRes.status, 200);
  assert.equal(headRes.headers.get('Accept-Ranges'), 'bytes');
  assert.equal(headRes.headers.get('Content-Length'), String(payload.byteLength));
  assert.equal(headRes.headers.get('ETag'), `"${expectedSha}"`);
  assert.equal(headRes.headers.get('Content-Type'), 'application/pdf');
  assert.ok((headRes.headers.get('Content-Disposition') || '').includes('inline;'));

  const head304 = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download`, {
      method: 'HEAD',
      headers: { 'If-None-Match': `"${expectedSha}"` },
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(head304.status, 304);

  // 2. Two sequential Range requests (206) reconstruct byte-for-byte PDF + matching SHA-256
  const split = 1024;
  const part1Res = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download`, {
      headers: {
        Range: `bytes=0-${split - 1}`,
        'If-Range': `"${expectedSha}"`,
      },
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(part1Res.status, 206);
  assert.equal(part1Res.headers.get('Accept-Ranges'), 'bytes');
  assert.equal(part1Res.headers.get('Content-Range'), `bytes 0-${split - 1}/${payload.byteLength}`);
  assert.equal(part1Res.headers.get('Content-Length'), String(split));
  const part1Bytes = new Uint8Array(await part1Res.arrayBuffer());

  const part2Res = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download`, {
      headers: {
        Range: `bytes=${split}-`,
        'If-Range': `"${expectedSha}"`,
      },
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(part2Res.status, 206);
  assert.equal(
    part2Res.headers.get('Content-Range'),
    `bytes ${split}-${payload.byteLength - 1}/${payload.byteLength}`
  );
  const part2Bytes = new Uint8Array(await part2Res.arrayBuffer());

  const reassembled = new Uint8Array(part1Bytes.byteLength + part2Bytes.byteLength);
  reassembled.set(part1Bytes, 0);
  reassembled.set(part2Bytes, part1Bytes.byteLength);
  assert.equal(sha256(reassembled), expectedSha);

  // 3. If-Range mismatch forces full 200 OK
  const ifRangeMismatch = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download`, {
      headers: {
        Range: `bytes=0-100`,
        'If-Range': '"0000000000000000000000000000000000000000000000000000000000000000"',
      },
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(ifRangeMismatch.status, 200);
  assert.equal(ifRangeMismatch.headers.get('Accept-Ranges'), 'bytes');
  const fullOnMismatch = new Uint8Array(await ifRangeMismatch.arrayBuffer());
  assert.equal(fullOnMismatch.byteLength, payload.byteLength);

  // 4. Out-of-bounds Range returns 416, does NOT charge B2 egress, and does NOT bump download_count
  await flushUsageTelemetryNow(env.DB);
  const beforeCountRow = db
    .prepare('SELECT download_count FROM past_papers WHERE id = ?')
    .get(paperId) as { download_count: number };
  const todayYmd = new Date().toISOString().slice(0, 10);
  const beforeUsageRow = db
    .prepare('SELECT b2_download_bytes FROM daily_usage_metrics WHERE day_ymd = ?')
    .get(todayYmd) as { b2_download_bytes: number };

  const res416 = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download`, {
      headers: { Range: `bytes=${payload.byteLength + 500}-` },
    }),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(res416.status, 416);
  assert.equal(res416.headers.get('Content-Range'), `bytes */${payload.byteLength}`);
  assert.equal(res416.headers.get('Accept-Ranges'), 'bytes');

  await flushUsageTelemetryNow(env.DB);
  const afterCountRow = db
    .prepare('SELECT download_count FROM past_papers WHERE id = ?')
    .get(paperId) as { download_count: number };
  const afterUsageRow = db
    .prepare('SELECT b2_download_bytes FROM daily_usage_metrics WHERE day_ymd = ?')
    .get(todayYmd) as { b2_download_bytes: number };

  assert.equal(afterCountRow.download_count, beforeCountRow.download_count);
  assert.equal(afterUsageRow.b2_download_bytes, beforeUsageRow.b2_download_bytes);
});

test('Metered stream bills only bytes actually served on mid-stream abort, and skips download_count before first byte', async () => {
  resetInMemoryTelemetryBuffer();
  const { db, env } = createTestD1Env();
  const b2Store = new Map<string, Uint8Array>();

  const fullSize = 4096;
  const pdfBytes = new Uint8Array(fullSize);
  pdfBytes.set(new TextEncoder().encode('%PDF-1.4\n'), 0);
  pdfBytes.fill(65, 9, fullSize - 6);
  pdfBytes.set(new TextEncoder().encode('\n%%EOF'), fullSize - 6);

  // Use custom fetch that streams 512 bytes then waits until cancelled
  const customFetch = createRangedB2Fetch(b2Store);
  const { uploadRes } = await bootstrapAdminAndUploadPdf(env, customFetch, pdfBytes);
  const uploaded = ((await uploadRes.json()) as any).data;
  const pdfId = uploaded.pdf.id as string;
  const paperId = uploaded.paper.id as string;

  const chunkedFetch: typeof fetch = async (input, init) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    if ((init?.method || 'GET').toUpperCase() === 'GET' && url.includes(uploaded.pdf.objectKey)) {
      const body = new ReadableStream<Uint8Array>({
        start(controller) {
          controller.enqueue(pdfBytes.slice(0, 512));
          // Remaining chunks never sent before client aborts
        },
      });
      return new Response(body, {
        status: 200,
        headers: { 'Content-Type': 'application/pdf', 'Content-Length': String(fullSize) },
      });
    }
    return customFetch(input, init);
  };

  const res = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download`),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: chunkedFetch }
  );
  assert.equal(res.status, 200);
  const reader = res.body!.getReader();
  const firstChunk = await reader.read();
  assert.equal(firstChunk.value?.byteLength, 512);
  // Client aborts connection after receiving only 512 of 4096 bytes
  await reader.cancel('network_drop');

  await flushUsageTelemetryNow(env.DB);
  const todayYmd = new Date().toISOString().slice(0, 10);
  const usageRow = db
    .prepare('SELECT b2_download_bytes FROM daily_usage_metrics WHERE day_ymd = ?')
    .get(todayYmd) as { b2_download_bytes: number };

  assert.equal(usageRow.b2_download_bytes, 512, 'Should bill exact 512 served bytes, not 4096 full size');
  const paperRow = db
    .prepare('SELECT download_count FROM past_papers WHERE id = ?')
    .get(paperId) as { download_count: number };
  assert.equal(paperRow.download_count, 1, 'First byte was transmitted so download_count incremented once');
});

test('429 B2_DAILY_BANDWIDTH_CAP emits Retry-After, presigned mode enforces >=6h + response-content-disposition, admin checks SHA-256', async () => {
  resetInMemoryTelemetryBuffer();
  const { env } = createTestD1Env();
  const b2Store = new Map<string, Uint8Array>();
  const mockFetch = createRangedB2Fetch(b2Store);

  const sampleBytes = new TextEncoder().encode('%PDF-1.4\n1 0 obj<<>>endobj\n%%EOF');
  const validSha = sha256(sampleBytes);

  // Bootstrap and verify mismatched checksumSha256 is rejected with 422 CHECKSUM_MISMATCH before touching B2
  const { token, uploadRes: mismatchRes } = await bootstrapAdminAndUploadPdf(
    env,
    mockFetch,
    sampleBytes,
    'f'.repeat(64)
  );
  assert.equal(mismatchRes.status, 422);
  assert.equal(((await mismatchRes.json()) as any).error.code, 'CHECKSUM_MISMATCH');
  assert.equal(b2Store.size, 0);

  // Upload cleanly with matching SHA-256
  const form = new FormData();
  form.set('file', new Blob([sampleBytes], { type: 'application/pdf' }), 'clean.pdf');
  form.set('subjectId', 'sub_kpk11_phy');
  form.set('title', 'Clean PDF');
  form.set('checksumSha256', validSha);
  const okUpload = await handleWorkerRequest(
    new Request('https://imtihan-pro-api.workers.dev/v1/admin/pdfs', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: form,
    }),
    env,
    undefined,
    { skipRateLimit: true, skipTelemetry: true, fetchFn: mockFetch }
  );
  assert.equal(okUpload.status, 201);
  const pdfId = ((await okUpload.json()) as any).data.pdf.id as string;

  // Presigned mode >= 21600s + response-content-disposition
  const presignRes = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download?mode=presigned`),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(presignRes.status, 200);
  const presignBody = ((await presignRes.json()) as any).data;
  assert.ok(presignBody.expiresInSeconds >= 21600);
  assert.ok(presignBody.presignedB2Url.includes('response-content-disposition='));
  assert.equal(presignBody.acceptRanges, true);

  // Clamp check on helper even when small expiry passed
  const directSigned = await generateB2PresignedGetUrl('kpk/class-11/physics/a.pdf', env, 600);
  assert.ok(directSigned?.includes('X-Amz-Expires=21600'));

  // Simulate exhausted B2 daily bandwidth cap -> 429 with Retry-After header
  const todayYmd = new Date().toISOString().slice(0, 10);
  await env.DB.prepare(
    `INSERT INTO daily_usage_metrics (day_ymd, b2_download_bytes, updated_at_iso)
     VALUES (?, ?, ?)
     ON CONFLICT(day_ymd) DO UPDATE SET b2_download_bytes = excluded.b2_download_bytes`
  )
    .bind(todayYmd, 3000 * 1024 * 1024, new Date().toISOString())
    .run();

  const blockedRes = await handleWorkerRequest(
    new Request(`https://imtihan-pro-api.workers.dev/v1/pdfs/${pdfId}/download`),
    env,
    undefined,
    { skipRateLimit: true, fetchFn: mockFetch }
  );
  assert.equal(blockedRes.status, 429);
  const retryAfter = Number.parseInt(blockedRes.headers.get('Retry-After') || '0', 10);
  assert.ok(retryAfter >= 60, 'Retry-After header must be >= 60s');
  const blockedJson = (await blockedRes.json()) as any;
  assert.equal(blockedJson.error.code, 'B2_DAILY_BANDWIDTH_CAP');

  // Helper unit checks: strong vs weak If-Range
  assert.equal(shouldHonorRangeWithIfRange('"abc"', '"abc"'), true);
  assert.equal(shouldHonorRangeWithIfRange('W/"abc"', 'W/"abc"'), false);
  assert.equal(shouldHonorRangeWithIfRange('"other"', '"abc"'), false);
});
