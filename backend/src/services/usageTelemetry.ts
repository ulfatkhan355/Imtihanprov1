import {
  DEFAULT_CAPACITY_POLICY,
  evaluateQuotaAlerts,
  OFFICIAL_FREE_TIER_LIMITS,
} from '../../../shared-contracts/capacity';
import type {
  AdminCapacityDashboardResponse,
  CapacityPolicyConfig,
  UsageSnapshotMetrics,
} from '../../../shared-contracts/types';
import { buildStandardCapacityMatrix } from '../../../shared-contracts/capacity';
import { safeJsonParse } from '../db/repository';
import type { D1Database, Env } from '../env';

export interface TelemetryDelta {
  workerRequests?: number;
  edgeCacheHits?: number;
  d1ReadsEst?: number;
  d1WritesEst?: number;
  b2DownloadBytes?: number;
  b2DownloadCount?: number;
  b2UploadBytes?: number;
  aiRequests?: number;
  aiCacheHits?: number;
  aiBlockedQuota?: number;
  errorCount?: number;
  latencyMs?: number;
}

interface PendingBucket {
  workerRequests: number;
  edgeCacheHits: number;
  d1ReadsEst: number;
  d1WritesEst: number;
  b2DownloadBytes: number;
  b2DownloadCount: number;
  b2UploadBytes: number;
  aiRequests: number;
  aiCacheHits: number;
  aiBlockedQuota: number;
  errorCount: number;
  latencySumMs: number;
  latencySamples: number;
}

function emptyBucket(): PendingBucket {
  return {
    workerRequests: 0,
    edgeCacheHits: 0,
    d1ReadsEst: 0,
    d1WritesEst: 0,
    b2DownloadBytes: 0,
    b2DownloadCount: 0,
    b2UploadBytes: 0,
    aiRequests: 0,
    aiCacheHits: 0,
    aiBlockedQuota: 0,
    errorCount: 0,
    latencySumMs: 0,
    latencySamples: 0,
  };
}

// Buffered per-isolate deltas keyed by UTC day to minimize D1 row writes on the free tier.
const pendingByDay = new Map<string, PendingBucket>();
let lastFlushAtMs = 0;

export function currentUtcDayYmd(now: Date = new Date()): string {
  return now.toISOString().slice(0, 10);
}

export function resetInMemoryTelemetryBuffer(): void {
  pendingByDay.clear();
  lastFlushAtMs = 0;
}

export function recordUsageDelta(delta: TelemetryDelta, now: Date = new Date()): void {
  const dayYmd = currentUtcDayYmd(now);
  const bucket = pendingByDay.get(dayYmd) ?? emptyBucket();
  bucket.workerRequests += delta.workerRequests ?? 0;
  bucket.edgeCacheHits += delta.edgeCacheHits ?? 0;
  bucket.d1ReadsEst += delta.d1ReadsEst ?? 0;
  bucket.d1WritesEst += delta.d1WritesEst ?? 0;
  bucket.b2DownloadBytes += delta.b2DownloadBytes ?? 0;
  bucket.b2DownloadCount += delta.b2DownloadCount ?? 0;
  bucket.b2UploadBytes += delta.b2UploadBytes ?? 0;
  bucket.aiRequests += delta.aiRequests ?? 0;
  bucket.aiCacheHits += delta.aiCacheHits ?? 0;
  bucket.aiBlockedQuota += delta.aiBlockedQuota ?? 0;
  bucket.errorCount += delta.errorCount ?? 0;
  if (typeof delta.latencyMs === 'number' && Number.isFinite(delta.latencyMs)) {
    bucket.latencySumMs += Math.max(0, Math.round(delta.latencyMs));
    bucket.latencySamples += 1;
  }
  pendingByDay.set(dayYmd, bucket);
}

export function shouldFlushUsageBuffer(nowMs: number = Date.now()): boolean {
  if (pendingByDay.size === 0) return false;
  let totalReqs = 0;
  let forceEvent = false;
  for (const b of pendingByDay.values()) {
    totalReqs += b.workerRequests;
    if (b.b2DownloadBytes > 0 || b.b2UploadBytes > 0 || b.aiRequests > 0 || b.aiBlockedQuota > 0) {
      forceEvent = true;
    }
  }
  return forceEvent || totalReqs >= 20 || nowMs - lastFlushAtMs >= 30_000;
}

export async function flushUsageTelemetryNow(db: D1Database): Promise<void> {
  if (pendingByDay.size === 0) return;
  const entries = Array.from(pendingByDay.entries());
  pendingByDay.clear();
  lastFlushAtMs = Date.now();
  const nowIso = new Date().toISOString();

  try {
    const statements = entries.map(([dayYmd, b]) =>
      db
        .prepare(
          `INSERT INTO daily_usage_metrics (
            day_ymd, worker_requests, edge_cache_hits, d1_reads_est, d1_writes_est,
            b2_download_bytes, b2_download_count, b2_upload_bytes,
            ai_requests, ai_cache_hits, ai_blocked_quota,
            error_count, latency_sum_ms, latency_samples, updated_at_iso
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT(day_ymd) DO UPDATE SET
            worker_requests = worker_requests + excluded.worker_requests,
            edge_cache_hits = edge_cache_hits + excluded.edge_cache_hits,
            d1_reads_est = d1_reads_est + excluded.d1_reads_est,
            d1_writes_est = d1_writes_est + excluded.d1_writes_est,
            b2_download_bytes = b2_download_bytes + excluded.b2_download_bytes,
            b2_download_count = b2_download_count + excluded.b2_download_count,
            b2_upload_bytes = b2_upload_bytes + excluded.b2_upload_bytes,
            ai_requests = ai_requests + excluded.ai_requests,
            ai_cache_hits = ai_cache_hits + excluded.ai_cache_hits,
            ai_blocked_quota = ai_blocked_quota + excluded.ai_blocked_quota,
            error_count = error_count + excluded.error_count,
            latency_sum_ms = latency_sum_ms + excluded.latency_sum_ms,
            latency_samples = latency_samples + excluded.latency_samples,
            updated_at_iso = excluded.updated_at_iso`
        )
        .bind(
          dayYmd,
          b.workerRequests,
          b.edgeCacheHits,
          b.d1ReadsEst,
          b.d1WritesEst + 1, // Include this batch telemetry row write
          b.b2DownloadBytes,
          b.b2DownloadCount,
          b.b2UploadBytes,
          b.aiRequests,
          b.aiCacheHits,
          b.aiBlockedQuota,
          b.errorCount,
          b.latencySumMs,
          b.latencySamples,
          nowIso
        )
    );
    await db.batch(statements);
  } catch {
    // Telemetry is best-effort and must never crash core educational flows.
  }
}

export async function getCapacityPolicy(db: D1Database): Promise<CapacityPolicyConfig> {
  try {
    const row = await db
      .prepare("SELECT setting_json FROM app_settings WHERE setting_key = 'capacity_policy' LIMIT 1")
      .first<{ setting_json: string }>();
    const parsed = safeJsonParse<Partial<CapacityPolicyConfig>>(row?.setting_json, {});
    return {
      ...DEFAULT_CAPACITY_POLICY,
      ...parsed,
    };
  } catch {
    return DEFAULT_CAPACITY_POLICY;
  }
}

export async function saveCapacityPolicy(
  db: D1Database,
  next: Partial<CapacityPolicyConfig>
): Promise<CapacityPolicyConfig> {
  const current = await getCapacityPolicy(db);
  const merged: CapacityPolicyConfig = {
    warningThresholdPct: clampInt(next.warningThresholdPct ?? current.warningThresholdPct, 40, 95),
    highThresholdPct: clampInt(next.highThresholdPct ?? current.highThresholdPct, 50, 98),
    criticalThresholdPct: clampInt(next.criticalThresholdPct ?? current.criticalThresholdPct, 60, 100),
    aiTutorDailyGlobalCap: clampInt(next.aiTutorDailyGlobalCap ?? current.aiTutorDailyGlobalCap, 0, 5000),
    aiTutorDailyPerStudentCap: clampInt(next.aiTutorDailyPerStudentCap ?? current.aiTutorDailyPerStudentCap, 0, 100),
    aiCacheTtlHours: clampInt(next.aiCacheTtlHours ?? current.aiCacheTtlHours, 1, 720),
    b2DailyDownloadSoftCapMb: clampInt(next.b2DailyDownloadSoftCapMb ?? current.b2DailyDownloadSoftCapMb, 50, 10240),
    d1DailyReadSoftCap: clampInt(next.d1DailyReadSoftCap ?? current.d1DailyReadSoftCap, 100_000, 5_000_000),
    d1DailyWriteSoftCap: clampInt(next.d1DailyWriteSoftCap ?? current.d1DailyWriteSoftCap, 1_000, 100_000),
    workerDailyRequestSoftCap: clampInt(next.workerDailyRequestSoftCap ?? current.workerDailyRequestSoftCap, 1_000, 100_000),
    autoDegradeAiAtCritical: next.autoDegradeAiAtCritical ?? current.autoDegradeAiAtCritical,
    autoBlockUncachedPdfAtCritical:
      next.autoBlockUncachedPdfAtCritical ?? current.autoBlockUncachedPdfAtCritical,
  };

  await db
    .prepare(
      `INSERT INTO app_settings (setting_key, setting_json, updated_at_iso)
       VALUES ('capacity_policy', ?, ?)
       ON CONFLICT(setting_key) DO UPDATE SET
         setting_json = excluded.setting_json,
         updated_at_iso = excluded.updated_at_iso`
    )
    .bind(JSON.stringify(merged), new Date().toISOString())
    .run();

  return merged;
}

export async function getTodayUsageSnapshot(db: D1Database, dayYmd = currentUtcDayYmd()): Promise<UsageSnapshotMetrics> {
  await flushUsageTelemetryNow(db);

  let row: {
    worker_requests: number;
    edge_cache_hits: number;
    d1_reads_est: number;
    d1_writes_est: number;
    b2_download_bytes: number;
    b2_download_count: number;
    ai_requests: number;
    ai_cache_hits: number;
    ai_blocked_quota: number;
    error_count: number;
    latency_sum_ms: number;
    latency_samples: number;
  } | null = null;

  try {
    row = await db
      .prepare('SELECT * FROM daily_usage_metrics WHERE day_ymd = ? LIMIT 1')
      .bind(dayYmd)
      .first();
  } catch {
    row = null;
  }

  let d1DatabaseSizeBytes = 0;
  try {
    const sizeRow = await db
      .prepare(
        'SELECT (SELECT page_count FROM pragma_page_count()) * (SELECT page_size FROM pragma_page_size()) AS size_bytes'
      )
      .first<{ size_bytes: number }>();
    d1DatabaseSizeBytes = Number(sizeRow?.size_bytes ?? 0);
  } catch {
    d1DatabaseSizeBytes = 0;
  }

  let b2StorageBytes = 0;
  try {
    const b2Row = await db
      .prepare('SELECT COALESCE(SUM(file_size_bytes), 0) AS total_bytes FROM pdf_assets')
      .first<{ total_bytes: number }>();
    b2StorageBytes = Number(b2Row?.total_bytes ?? 0);
  } catch {
    b2StorageBytes = 0;
  }

  const samples = Number(row?.latency_samples ?? 0);
  const avgLatencyMs = samples > 0 ? Math.round(Number(row?.latency_sum_ms ?? 0) / samples) : 0;

  return {
    dayYmd,
    workerRequestsToday: Number(row?.worker_requests ?? 0),
    edgeOrHttpCacheHitsToday: Number(row?.edge_cache_hits ?? 0),
    d1RowsReadEstToday: Number(row?.d1_reads_est ?? 0),
    d1RowsWrittenEstToday: Number(row?.d1_writes_est ?? 0),
    d1DatabaseSizeBytes,
    b2StorageBytes,
    b2DailyDownloadBytes: Number(row?.b2_download_bytes ?? 0),
    b2DailyDownloadCount: Number(row?.b2_download_count ?? 0),
    aiCallsToday: Number(row?.ai_requests ?? 0),
    aiCacheHitsToday: Number(row?.ai_cache_hits ?? 0),
    aiBlockedQuotaToday: Number(row?.ai_blocked_quota ?? 0),
    apiErrorCountToday: Number(row?.error_count ?? 0),
    avgLatencyMs,
    telemetryNote:
      'Tracked via buffered Worker daily_usage_metrics (flushes every 20 reqs or 30s) + live D1 pragma page size & pdf_assets sum.',
  };
}

export async function buildAdminCapacityDashboard(env: Env): Promise<AdminCapacityDashboardResponse> {
  const [policy, usage] = await Promise.all([
    getCapacityPolicy(env.DB),
    getTodayUsageSnapshot(env.DB),
  ]);
  const limits = OFFICIAL_FREE_TIER_LIMITS;
  const alerts = evaluateQuotaAlerts(usage, policy, limits);

  const b2DailyMbUsed = usage.b2DailyDownloadBytes / (1024 * 1024);
  const aiCallsRemaining = Math.max(0, policy.aiTutorDailyGlobalCap - usage.aiCallsToday);
  const b2DailyDownloadMbRemaining = Math.max(
    0,
    Number((limits.b2ConservativeDailyDownloadGb * 1024 - b2DailyMbUsed).toFixed(1))
  );

  const aiCritical =
    policy.autoDegradeAiAtCritical &&
    (usage.aiCallsToday >= policy.aiTutorDailyGlobalCap ||
      (policy.aiTutorDailyGlobalCap > 0 &&
        (usage.aiCallsToday / policy.aiTutorDailyGlobalCap) * 100 >= policy.criticalThresholdPct));

  const b2Critical =
    policy.autoBlockUncachedPdfAtCritical && b2DailyMbUsed >= policy.b2DailyDownloadSoftCapMb;

  return {
    limits,
    policy,
    usage,
    alerts,
    remainingCapacity: {
      workerRequestsRemaining: Math.max(0, limits.workersRequestsPerDay - usage.workerRequestsToday),
      d1RowsReadRemaining: Math.max(0, limits.d1RowsReadPerDay - usage.d1RowsReadEstToday),
      d1RowsWrittenRemaining: Math.max(0, limits.d1RowsWrittenPerDay - usage.d1RowsWrittenEstToday),
      d1StorageMbRemaining: Math.max(
        0,
        Number((limits.d1MaxDatabaseSizeMb - usage.d1DatabaseSizeBytes / (1024 * 1024)).toFixed(2))
      ),
      b2StorageGbRemaining: Math.max(
        0,
        Number((limits.b2FreeStorageGb - usage.b2StorageBytes / (1024 * 1024 * 1024)).toFixed(3))
      ),
      b2DailyDownloadMbRemaining,
      aiCallsRemaining,
    },
    dauProjections: buildStandardCapacityMatrix(),
    safeFallbackActive: {
      aiTutorPausedByCap: aiCritical,
      pdfStreamingGuardedByCap: b2Critical,
    },
  };
}

function clampInt(val: number, min: number, max: number): number {
  if (!Number.isFinite(val)) return min;
  return Math.max(min, Math.min(max, Math.round(val)));
}
