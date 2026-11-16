import type {
  CapacityAlertItem,
  CapacityPolicyConfig,
  CapacityProjectionMetrics,
  CapacityScaleResult,
  FreeTierAllowances,
  UsageSnapshotMetrics,
} from './types';

/**
 * Verified official Free-Tier Allowances for Imtihan Pro's zero-budget stack.
 * Sources verified against Cloudflare Workers, Cloudflare D1, Backblaze B2, and Google AI Studio docs.
 */
export const OFFICIAL_FREE_TIER_LIMITS: FreeTierAllowances = {
  workersRequestsPerDay: 100_000,
  workersCpuMsPerInvocation: 10,
  workersMemoryMb: 128,
  workersSubrequestsPerInvocation: 50,
  d1RowsReadPerDay: 5_000_000,
  d1RowsWrittenPerDay: 100_000,
  d1MaxDatabaseSizeMb: 500,
  d1AccountStorageGb: 5,
  d1QueriesPerInvocation: 50,
  b2FreeStorageGb: 10,
  b2ConservativeDailyDownloadGb: 1, // Conservative daily budget (modern B2 PAYG allows 3x monthly avg storage free; legacy free tier 1 GB/day)
  b2ClassDConsiderationDailyCap: 2_500,
  geminiConservativeFreeRpd: 1_500, // Dynamic per project in AI Studio Rate Limits; 1,500 RPD reference ceiling
  geminiConservativeFreeRpm: 15,
};

export const DEFAULT_CAPACITY_POLICY: CapacityPolicyConfig = {
  warningThresholdPct: 70,
  highThresholdPct: 85,
  criticalThresholdPct: 95,
  aiTutorDailyGlobalCap: 400,
  aiTutorDailyPerStudentCap: 10,
  aiCacheTtlHours: 168, // 7 days
  b2DailyDownloadSoftCapMb: 950,
  d1DailyReadSoftCap: 4_500_000,
  d1DailyWriteSoftCap: 90_000,
  workerDailyRequestSoftCap: 90_000,
  autoDegradeAiAtCritical: true,
  autoBlockUncachedPdfAtCritical: true,
};

export interface CapacityAssumptionProfile {
  /** Total HTTP requests per active student/day before CDN/OkHttp caching */
  rawApiRequestsPerStudent: number;
  /** Fraction of curriculum/note GETs satisfied by OkHttp/CDN 304 or edge cache (0..0.95) */
  publicCacheHitRatio: number;
  /** Average D1 rows scanned/read per uncached Worker request */
  avgD1ReadsPerWorkerHit: number;
  /** Average D1 rows written per active student/day after batching & delta-skipping */
  avgD1WritesPerStudent: number;
  /** Active PDF download requests per student/day (after Room/filesDir local PDF caching) */
  pdfDownloadsPerStudentPerDay: number;
  /** Average PDF size in MB (compressed KPK past paper / chapter pack) */
  avgPdfSizeMb: number;
  /** Fraction of daily active students hitting AI Tutor uncached */
  aiTutorUsageRatePerStudent: number;
  /** AI semantic cache hit ratio (0..0.9) */
  aiCacheHitRatio: number;
  /** Peak-hour burst multiplier relative to uniform 24h traffic (e.g. evening study 7-10 PM PKT) */
  peakEveningShareRatio: number;
}

export const DEFAULT_CAPACITY_ASSUMPTIONS: CapacityAssumptionProfile = {
  rawApiRequestsPerStudent: 28,
  publicCacheHitRatio: 0.65,
  avgD1ReadsPerWorkerHit: 42,
  avgD1WritesPerStudent: 14,
  pdfDownloadsPerStudentPerDay: 0.25,
  avgPdfSizeMb: 1.4,
  aiTutorUsageRatePerStudent: 0.25,
  aiCacheHitRatio: 0.45,
  peakEveningShareRatio: 0.35, // ~35% of daily traffic during 3-hour evening window
};

export const STANDARD_DAU_TIERS = [300, 500, 1000, 2000, 5000] as const;

export function estimateCapacityForDau(
  dailyActiveStudents: number,
  assumptions: CapacityAssumptionProfile = DEFAULT_CAPACITY_ASSUMPTIONS,
  limits: FreeTierAllowances = OFFICIAL_FREE_TIER_LIMITS
): CapacityScaleResult {
  const dau = Math.max(1, Math.round(dailyActiveStudents));
  const hitRatio = Math.max(0, Math.min(0.95, assumptions.publicCacheHitRatio));

  // Roughly 78% of student calls are cacheable GETs (subjects, chapters, notes, MCQs, papers)
  const cacheableShare = 0.78;
  const cachedGetsSaved = Math.round(dau * assumptions.rawApiRequestsPerStudent * cacheableShare * hitRatio);
  const workerRequestsPerDay = Math.max(
    dau,
    Math.round(dau * assumptions.rawApiRequestsPerStudent - cachedGetsSaved)
  );

  // Cacheable GETs hitting Cloudflare Edge Cache / 304 consume 0 D1 reads; only origin hits hit D1
  const d1RowsReadPerDay = Math.round(workerRequestsPerDay * assumptions.avgD1ReadsPerWorkerHit);
  const d1RowsWrittenPerDay = Math.round(dau * assumptions.avgD1WritesPerStudent);

  // Storage growth per active student (~2.5 KB/month across progress, quizzes, mistakes, tasks)
  const monthlyStorageGrowthMb = Number(((dau * 2.5) / 1024 + 0.5).toFixed(2));

  // Backblaze B2 daily download bandwidth after local Room/disk PDF caching
  const b2DailyDownloadGb = Number(
    ((dau * assumptions.pdfDownloadsPerStudentPerDay * assumptions.avgPdfSizeMb) / 1024).toFixed(3)
  );

  // Peak evening 3-hour window => requests/sec + approximate concurrent active learners (~12 min session)
  const peakWindowRequests = workerRequestsPerDay * assumptions.peakEveningShareRatio;
  const peakRequestsPerSecond = Number((peakWindowRequests / (3 * 3600)).toFixed(2));
  const estimatedPeakConcurrentUsers = Math.max(1, Math.round((dau * 0.35 * 12) / 180));

  // AI consumption after cache
  const aiRequestsRaw = dau * assumptions.aiTutorUsageRatePerStudent;
  const aiCallsPerDay = Math.round(aiRequestsRaw * (1 - Math.max(0, Math.min(0.95, assumptions.aiCacheHitRatio))));

  const metrics: CapacityProjectionMetrics = {
    workerRequestsPerDay,
    d1RowsReadPerDay,
    d1RowsWrittenPerDay,
    monthlyStorageGrowthMb,
    b2DailyDownloadGb,
    peakRequestsPerSecond,
    estimatedPeakConcurrentUsers,
    aiCallsPerDay,
  };

  const utilizationPct = {
    workerRequests: Number(((workerRequestsPerDay / limits.workersRequestsPerDay) * 100).toFixed(1)),
    d1Reads: Number(((d1RowsReadPerDay / limits.d1RowsReadPerDay) * 100).toFixed(1)),
    d1Writes: Number(((d1RowsWrittenPerDay / limits.d1RowsWrittenPerDay) * 100).toFixed(1)),
    b2Download: Number(((b2DailyDownloadGb / limits.b2ConservativeDailyDownloadGb) * 100).toFixed(1)),
    aiDaily: Number(((aiCallsPerDay / limits.geminiConservativeFreeRpd) * 100).toFixed(1)),
  };

  const bottlenecks: string[] = [];
  if (utilizationPct.workerRequests >= 95) {
    bottlenecks.push(`Worker requests (${utilizationPct.workerRequests}% of 100k/day)`);
  }
  if (utilizationPct.d1Writes >= 95) {
    bottlenecks.push(`D1 writes (${utilizationPct.d1Writes}% of 100k/day)`);
  }
  if (utilizationPct.d1Reads >= 95) {
    bottlenecks.push(`D1 reads (${utilizationPct.d1Reads}% of 5M/day)`);
  }
  if (utilizationPct.b2Download >= 95) {
    bottlenecks.push(`B2 conservative 1 GB/day download (${utilizationPct.b2Download}%)`);
  }

  const maxUtil = Math.max(
    utilizationPct.workerRequests,
    utilizationPct.d1Reads,
    utilizationPct.d1Writes,
    utilizationPct.b2Download
  );

  const status: CapacityScaleResult['status'] =
    maxUtil >= 100 ? 'exceeds_free_tier' : maxUtil >= 70 ? 'near_limit' : 'within_free_tier';

  return {
    dailyActiveStudents: dau,
    isTheoreticalEstimate: true,
    metrics,
    utilizationPct,
    status,
    bottlenecks,
  };
}

export function buildStandardCapacityMatrix(
  assumptions: CapacityAssumptionProfile = DEFAULT_CAPACITY_ASSUMPTIONS
): CapacityScaleResult[] {
  return STANDARD_DAU_TIERS.map((dau) => estimateCapacityForDau(dau, assumptions));
}

export function evaluateQuotaAlerts(
  usage: UsageSnapshotMetrics,
  policy: CapacityPolicyConfig = DEFAULT_CAPACITY_POLICY,
  limits: FreeTierAllowances = OFFICIAL_FREE_TIER_LIMITS
): CapacityAlertItem[] {
  const candidates: Array<{
    resource: CapacityAlertItem['resource'];
    label: string;
    used: number;
    limit: number;
    unit: string;
    mitigation: string;
  }> = [
    {
      resource: 'worker_requests',
      label: 'Cloudflare Worker Daily Requests',
      used: usage.workerRequestsToday,
      limit: limits.workersRequestsPerDay,
      unit: 'requests',
      mitigation: 'Increase public Edge Cache TTL & Room offline sync interval; avoid dashboard polling.',
    },
    {
      resource: 'd1_reads',
      label: 'Cloudflare D1 Rows Read (Today)',
      used: usage.d1RowsReadEstToday,
      limit: limits.d1RowsReadPerDay,
      unit: 'rows',
      mitigation: 'Rely on Edge/304 curriculum caching and tighten search pagination limits.',
    },
    {
      resource: 'd1_writes',
      label: 'Cloudflare D1 Rows Written (Today)',
      used: usage.d1RowsWrittenEstToday,
      limit: limits.d1RowsWrittenPerDay,
      unit: 'rows',
      mitigation: 'Deduplicate unchanged progress syncs and batch quiz writes.',
    },
    {
      resource: 'd1_storage',
      label: 'Cloudflare D1 Database Size',
      used: Number((usage.d1DatabaseSizeBytes / (1024 * 1024)).toFixed(2)),
      limit: limits.d1MaxDatabaseSizeMb,
      unit: 'MB',
      mitigation: 'Prune expired rate_limits/ai_tutor_cache rows and keep binaries strictly in B2.',
    },
    {
      resource: 'b2_storage',
      label: 'Backblaze B2 Bucket Storage',
      used: Number((usage.b2StorageBytes / (1024 * 1024 * 1024)).toFixed(3)),
      limit: limits.b2FreeStorageGb,
      unit: 'GB',
      mitigation: 'Compress uploaded PDFs (target <= 3 MB per paper) and remove superseded drafts.',
    },
    {
      resource: 'b2_downloads',
      label: 'Backblaze B2 Daily Downloads',
      used: Number((usage.b2DailyDownloadBytes / (1024 * 1024)).toFixed(1)),
      limit: limits.b2ConservativeDailyDownloadGb * 1024,
      unit: 'MB',
      mitigation: 'Promote Cloudflare Bandwidth Alliance / CDN caching or pause uncached PDF streams at critical cap.',
    },
    {
      resource: 'ai_tutor',
      label: 'AI Tutor Daily Calls (Gemini Free)',
      used: usage.aiCallsToday,
      limit: policy.aiTutorDailyGlobalCap,
      unit: 'calls',
      mitigation: 'Serve cached answers from ai_tutor_cache and degrade cleanly to 503 offline notes.',
    },
  ];

  const alerts: CapacityAlertItem[] = [];
  for (const c of candidates) {
    const pct = c.limit > 0 ? Number(((c.used / c.limit) * 100).toFixed(1)) : 0;
    let severity: CapacityAlertItem['severity'] = 'nominal';
    if (pct >= policy.criticalThresholdPct) {
      severity = 'critical';
    } else if (pct >= policy.highThresholdPct) {
      severity = 'high';
    } else if (pct >= policy.warningThresholdPct) {
      severity = 'warning';
    }
    alerts.push({
      resource: c.resource,
      label: c.label,
      used: c.used,
      limit: c.limit,
      unit: c.unit,
      utilizationPct: pct,
      severity,
      mitigation: c.mitigation,
    });
  }
  return alerts;
}
