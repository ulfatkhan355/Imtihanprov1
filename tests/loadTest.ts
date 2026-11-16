import fs from 'node:fs';
import path from 'node:path';
import { performance } from 'node:perf_hooks';
import { handleWorkerRequest } from '../backend/src/index';
import { resetInMemoryRateLimits } from '../backend/src/middleware/security';
import { resetInMemoryTelemetryBuffer } from '../backend/src/services/usageTelemetry';
import type { IsolatedLoadTestSnapshot, IsolatedLoadTestStageSnapshot } from '../admin/data/latestLoadTestSnapshot';
import { createTestD1Env } from './helpers/sqliteD1';

const args = process.argv.slice(2);
const writeSnapshot = args.includes('--write-snapshot');
const targetUrlArg = process.env.LOAD_TEST_TARGET_URL?.trim();
const allowNonLocal = args.includes('--allow-non-local');

if (targetUrlArg) {
  const parsed = new URL(targetUrlArg);
  const isLocalHost =
    parsed.hostname === 'localhost' ||
    parsed.hostname === '127.0.0.1' ||
    parsed.hostname === '[::1]';
  if (!isLocalHost && !allowNonLocal) {
    console.error(
      `REFUSED: LOAD_TEST_TARGET_URL (${targetUrlArg}) points to a remote host. Never load-test live free-tier Cloudflare production without explicit --allow-non-local opt-in.`
    );
    process.exit(1);
  }
}

function percentile(sorted: number[], pct: number): number {
  if (sorted.length === 0) return 0;
  const idx = Math.min(sorted.length - 1, Math.max(0, Math.ceil((pct / 100) * sorted.length) - 1));
  return Number(sorted[idx].toFixed(2));
}

async function runConcurrencyStage(concurrency: number): Promise<IsolatedLoadTestStageSnapshot> {
  resetInMemoryRateLimits();
  resetInMemoryTelemetryBuffer();
  const { env } = createTestD1Env();

  // Instrument D1 prepare() & batch() calls so SQL query count is genuinely measured per stage
  let sqlQueriesExecuted = 0;
  const origPrepare = env.DB.prepare.bind(env.DB);
  const origBatch = env.DB.batch.bind(env.DB);
  env.DB.prepare = (q: string) => {
    sqlQueriesExecuted += 1;
    return origPrepare(q);
  };
  env.DB.batch = (stmts) => {
    sqlQueriesExecuted += stmts.length;
    return origBatch(stmts);
  };

  let requestsSent = 0;
  let errorsCount = 0;
  let cache304Hits = 0;
  const latenciesMs: number[] = [];

  async function dispatch(pathname: string, init: RequestInit = {}, ipSuffix = 1): Promise<Response> {
    const t0 = performance.now();
    const res = targetUrlArg
      ? await fetch(`${targetUrlArg.replace(/\/+$/, '')}${pathname}`, init)
      : await handleWorkerRequest(
          new Request(`https://local-harness.imtihan.test${pathname}`, {
            ...init,
            headers: {
              'CF-Connecting-IP': `10.10.${Math.floor(ipSuffix / 200)}.${(ipSuffix % 200) + 1}`,
              ...(init.headers as Record<string, string> | undefined),
            },
          }),
          env
        );
    const dt = performance.now() - t0;
    requestsSent += 1;
    latenciesMs.push(dt);
    if (res.status === 304) cache304Hits += 1;
    if (res.status >= 400) errorsCount += 1;
    return res;
  }

  // Prime ETag on public catalog (realistic CDN/OkHttp client behavior)
  const warmSubj = await dispatch('/v1/subjects?board=KPK&classLevel=9', {}, 0);
  const subjectsEtag = warmSubj.headers.get('ETag') || '';
  const warmNotes = await dispatch('/v1/chapters/ch_phy9_01/content?mode=detailed_notes', {}, 0);
  const notesEtag = warmNotes.headers.get('ETag') || '';

  const startStage = performance.now();

  await Promise.all(
    Array.from({ length: concurrency }, async (_, idx) => {
      const deviceId = `load-stu-${concurrency}-${idx}`;
      // 1. Guest registration
      const guestRes = await dispatch(
        '/v1/auth/guest',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            guestDeviceId: deviceId,
            selectedBoard: 'KPK',
            selectedClass: 9,
            studyGroup: 'SCIENCE_BIOLOGY',
          }),
        },
        idx + 1
      );
      const guestJson = (await guestRes.json()) as { ok: boolean; data?: { token?: string } };
      const token = guestJson.data?.token || '';
      const auth = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

      // 2. Conditional cached catalog & notes reads (simulate OkHttp ETag validation)
      await dispatch(
        '/v1/subjects?board=KPK&classLevel=9',
        { headers: subjectsEtag ? { 'If-None-Match': subjectsEtag } : {} },
        idx + 1
      );
      await dispatch(
        '/v1/chapters/ch_phy9_01/content?mode=detailed_notes',
        { headers: notesEtag ? { 'If-None-Match': notesEtag } : {} },
        idx + 1
      );

      // 3. Exam start + quiz submission + insights fetch
      const examRes = await dispatch(
        '/v1/exams/start',
        {
          method: 'POST',
          headers: auth,
          body: JSON.stringify({
            mode: 'chapter_test',
            boardCode: 'KPK',
            classLevel: 9,
            subjectId: 'sub_kpk9_phy',
            chapterId: 'ch_phy9_01',
            questionCount: 5,
          }),
        },
        idx + 1
      );
      const examJson = (await examRes.json()) as {
        ok: boolean;
        data?: { sessionId?: string; mcqs?: Array<{ id: string }> };
      };
      const mcqs = examJson.data?.mcqs || [];
      if (mcqs.length > 0) {
        await dispatch(
          '/v1/quizzes/submissions',
          {
            method: 'POST',
            headers: auth,
            body: JSON.stringify({
              sessionId: examJson.data?.sessionId || `sess_${idx}`,
              studentId: null,
              guestDeviceId: deviceId,
              subjectId: 'sub_kpk9_phy',
              chapterId: 'ch_phy9_01',
              answers: mcqs.map((m) => ({
                mcqId: m.id,
                selectedOptionId: 'A',
                markedForReview: false,
              })),
              elapsedSeconds: 45,
              submittedAtIso: new Date().toISOString(),
            }),
          },
          idx + 1
        );
      }

      await dispatch('/v1/students/me/insights', { headers: auth }, idx + 1);
    })
  );

  const durationMs = Math.max(1, Number((performance.now() - startStage).toFixed(2)));
  const sortedLatencies = [...latenciesMs].sort((a, b) => a - b);

  return {
    concurrency,
    studentsSimulated: concurrency,
    requestsSent,
    errorsCount,
    cache304Hits,
    cache304RatePct: Number(((cache304Hits / Math.max(1, requestsSent)) * 100).toFixed(1)),
    sqlQueriesExecuted,
    sqlAvgPerRequest: Number((sqlQueriesExecuted / Math.max(1, requestsSent)).toFixed(2)),
    durationMs,
    throughputReqPerSec: Number(((requestsSent / durationMs) * 1000).toFixed(1)),
    p50LatencyMs: percentile(sortedLatencies, 50),
    p95LatencyMs: percentile(sortedLatencies, 95),
    p99LatencyMs: percentile(sortedLatencies, 99),
  };
}

async function main() {
  const concurrencyStages = [10, 25, 50, 100];
  const results: IsolatedLoadTestStageSnapshot[] = [];

  console.log('Imtihan Pro — Isolated Progressive Load Test');
  console.log(
    `Target: ${targetUrlArg ? targetUrlArg : 'In-Process Worker Router + Isolated SQLite D1 Emulator (Production Untouched)'}`
  );
  console.log('');

  for (const c of concurrencyStages) {
    const stage = await runConcurrencyStage(c);
    results.push(stage);
    console.log(
      `[Concurrency=${String(stage.concurrency).padStart(3)}] reqs=${stage.requestsSent} | throughput=${stage.throughputReqPerSec} req/s | 304_hit=${stage.cache304RatePct}% | sql/req=${stage.sqlAvgPerRequest} | p50=${stage.p50LatencyMs}ms p95=${stage.p95LatencyMs}ms p99=${stage.p99LatencyMs}ms | errors=${stage.errorsCount}`
    );
  }

  const snapshot: IsolatedLoadTestSnapshot = {
    measured: true,
    environmentLabel: targetUrlArg
      ? `Local HTTP Server (${targetUrlArg})`
      : 'Isolated Local SQLite + Worker Router (In-Process Benchmark)',
    disclaimer:
      'Measured in an isolated local test environment (not live Cloudflare edge network or production D1). Use these benchmarks strictly to verify router latency overhead, ETag 304 hit efficiency, SQL queries/request, and error-free concurrency.',
    recordedAtIso: new Date().toISOString(),
    stages: results,
  };

  if (writeSnapshot) {
    const outPath = path.resolve(process.cwd(), 'admin/data/latestLoadTestSnapshot.ts');
    const fileContent = `export interface IsolatedLoadTestStageSnapshot {
  concurrency: number;
  studentsSimulated: number;
  requestsSent: number;
  errorsCount: number;
  cache304Hits: number;
  cache304RatePct: number;
  sqlQueriesExecuted: number;
  sqlAvgPerRequest: number;
  durationMs: number;
  throughputReqPerSec: number;
  p50LatencyMs: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
}

export interface IsolatedLoadTestSnapshot {
  measured: boolean;
  environmentLabel: string;
  disclaimer: string;
  recordedAtIso: string | null;
  stages: IsolatedLoadTestStageSnapshot[];
}

export const LATEST_LOAD_TEST_SNAPSHOT: IsolatedLoadTestSnapshot = ${JSON.stringify(snapshot, null, 2)};
`;
    fs.writeFileSync(outPath, fileContent, 'utf8');
    console.log(`\nSaved measured snapshot to admin/data/latestLoadTestSnapshot.ts`);
  }

  const totalErrors = results.reduce((acc, s) => acc + s.errorsCount, 0);
  if (totalErrors > 0) {
    console.error(`\nFAILED: Observed ${totalErrors} error responses during isolated load stages.`);
    process.exit(1);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
