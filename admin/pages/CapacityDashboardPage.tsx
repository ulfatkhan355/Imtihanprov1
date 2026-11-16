import React, { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  Database,
  HardDrive,
  RefreshCw,
  RotateCcw,
  Save,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import type {
  AdminCapacityDashboardResponse,
  CapacityPolicyConfig,
  CapacityScaleResult,
} from '../../shared-contracts/types';
import {
  buildStandardCapacityMatrix,
  DEFAULT_CAPACITY_ASSUMPTIONS,
  STANDARD_DAU_TIERS,
  type CapacityAssumptionProfile,
} from '../../shared-contracts/capacity';
import { LATEST_LOAD_TEST_SNAPSHOT } from '../data/latestLoadTestSnapshot';
import type { AdminConnectionMode } from '../types/admin';
import { StageMockNotice, StatusIndicator } from '../components/StateBanner';

interface CapacityDashboardPageProps {
  dashboard: AdminCapacityDashboardResponse;
  connectionMode: AdminConnectionMode;
  onRefresh: () => Promise<void> | void;
  onSavePolicy: (policy: Partial<CapacityPolicyConfig>) => Promise<void> | void;
}

function formatNumber(n: number): string {
  return new Intl.NumberFormat('en-US').format(Math.round(n));
}

function statusBadgeTone(
  severity: 'nominal' | 'warning' | 'high' | 'critical'
): 'success' | 'warning' | 'danger' {
  if (severity === 'critical') return 'danger';
  if (severity === 'warning' || severity === 'high') return 'warning';
  return 'success';
}

function scaleStatusLabel(status: CapacityScaleResult['status']): {
  label: string;
  tone: 'success' | 'warning' | 'danger';
} {
  if (status === 'exceeds_free_tier') return { label: 'Exceeds Free Tier', tone: 'danger' };
  if (status === 'near_limit') return { label: 'Near Threshold (>=70%)', tone: 'warning' };
  return { label: 'Within Free Tier', tone: 'success' };
}

export function CapacityDashboardPage({
  dashboard,
  connectionMode,
  onRefresh,
  onSavePolicy,
}: CapacityDashboardPageProps) {
  const [refreshing, setRefreshing] = useState(false);
  const [savingPolicy, setSavingPolicy] = useState(false);
  const [policyNotice, setPolicyNotice] = useState<{
    type: 'success' | 'error';
    text: string;
  } | null>(null);

  const [draftPolicy, setDraftPolicy] = useState<CapacityPolicyConfig>(dashboard.policy);
  const [selectedDau, setSelectedDau] = useState<number>(1000);
  const [assumptions, setAssumptions] = useState<CapacityAssumptionProfile>(
    DEFAULT_CAPACITY_ASSUMPTIONS
  );

  useEffect(() => {
    setDraftPolicy(dashboard.policy);
  }, [dashboard.policy]);

  const simulatedMatrix = useMemo(
    () => buildStandardCapacityMatrix(assumptions),
    [assumptions]
  );

  const selectedTierProjection = useMemo(
    () =>
      simulatedMatrix.find((row) => row.dailyActiveStudents === selectedDau) ??
      simulatedMatrix[0],
    [simulatedMatrix, selectedDau]
  );

  const elevatedAlerts = useMemo(
    () => dashboard.alerts.filter((a) => a.severity !== 'nominal'),
    [dashboard.alerts]
  );

  const { usage, limits, remainingCapacity, safeFallbackActive } = dashboard;

  const usageCards = [
    {
      label: 'Worker Requests Today',
      value: `${formatNumber(usage.workerRequestsToday)} / ${formatNumber(limits.workersRequestsPerDay)}`,
      detail: `${formatNumber(remainingCapacity.workerRequestsRemaining)} remaining · ${formatNumber(usage.edgeOrHttpCacheHitsToday)} Edge/304 cache hits`,
      icon: Activity,
    },
    {
      label: 'D1 Rows Read (Est. Today)',
      value: `${formatNumber(usage.d1RowsReadEstToday)} / ${formatNumber(limits.d1RowsReadPerDay)}`,
      detail: `${formatNumber(remainingCapacity.d1RowsReadRemaining)} rows headroom · 0 reads on Edge 304`,
      icon: Database,
    },
    {
      label: 'D1 Rows Written (Est. Today)',
      value: `${formatNumber(usage.d1RowsWrittenEstToday)} / ${formatNumber(limits.d1RowsWrittenPerDay)}`,
      detail: `${formatNumber(remainingCapacity.d1RowsWrittenRemaining)} writes remaining · batched & delta-skipped`,
      icon: Database,
    },
    {
      label: 'D1 Database Size',
      value: `${(usage.d1DatabaseSizeBytes / (1024 * 1024)).toFixed(2)} / ${limits.d1MaxDatabaseSizeMb} MB`,
      detail: `${remainingCapacity.d1StorageMbRemaining.toFixed(2)} MB remaining on 500 MB free DB ceiling`,
      icon: HardDrive,
    },
    {
      label: 'Backblaze B2 Storage & Egress',
      value: `${(usage.b2StorageBytes / (1024 * 1024 * 1024)).toFixed(3)} / ${limits.b2FreeStorageGb} GB`,
      detail: `Daily download: ${(usage.b2DailyDownloadBytes / (1024 * 1024)).toFixed(1)} MB / ${limits.b2ConservativeDailyDownloadGb * 1024} MB cap`,
      icon: HardDrive,
    },
    {
      label: 'AI Tutor & Cache Efficiency',
      value: `${formatNumber(usage.aiCallsToday)} / ${formatNumber(dashboard.policy.aiTutorDailyGlobalCap)}`,
      detail: `${formatNumber(usage.aiCacheHitsToday)} D1 cache hits · ${formatNumber(usage.aiBlockedQuotaToday)} blocked · ${usage.avgLatencyMs} ms avg latency`,
      icon: Sparkles,
    },
  ];

  const handleRefreshClick = async () => {
    setRefreshing(true);
    try {
      await onRefresh();
    } finally {
      setRefreshing(false);
    }
  };

  const handlePolicySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPolicyNotice(null);

    if (
      draftPolicy.warningThresholdPct >= draftPolicy.highThresholdPct ||
      draftPolicy.highThresholdPct >= draftPolicy.criticalThresholdPct
    ) {
      setPolicyNotice({
        type: 'error',
        text: 'Thresholds must follow Warning < High < Critical (e.g., 70 < 85 < 95).',
      });
      return;
    }

    setSavingPolicy(true);
    try {
      await onSavePolicy(draftPolicy);
      setPolicyNotice({
        type: 'success',
        text:
          connectionMode === 'live_api'
            ? 'Capacity & cost-protection policy saved to D1 app_settings.'
            : 'Capacity & cost-protection policy updated in Offline UI Preview.',
      });
    } catch (err) {
      setPolicyNotice({
        type: 'error',
        text: err instanceof Error ? err.message : 'Unable to save capacity policy.',
      });
    } finally {
      setSavingPolicy(false);
    }
  };

  return (
    <div className="space-y-6">
      <StageMockNotice
        title={
          connectionMode === 'live_api'
            ? 'Live Free-Tier Usage Telemetry & Cost Protection'
            : 'Free-Tier Capacity Planner (Offline UI Preview)'
        }
        message={usage.telemetryNote}
      />

      {/* Section A: Active Threshold Alerts (70/85/95%) & Safe Fallbacks */}
      <section className="rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
              Free-Tier Quota Alerts & Safe Fallback Guards
            </h2>
            <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
              Thresholds: Warning at {dashboard.policy.warningThresholdPct}% · High at{' '}
              {dashboard.policy.highThresholdPct}% · Critical auto-protection at{' '}
              {dashboard.policy.criticalThresholdPct}%
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <StatusIndicator
              label={
                safeFallbackActive.aiTutorPausedByCap
                  ? 'AI Tutor: Paused by Daily Cap'
                  : 'AI Tutor Guard: Ready'
              }
              tone={safeFallbackActive.aiTutorPausedByCap ? 'warning' : 'success'}
            />
            <StatusIndicator
              label={
                safeFallbackActive.pdfStreamingGuardedByCap
                  ? 'PDF Egress: Soft-Capped'
                  : 'PDF Streaming: Open'
              }
              tone={safeFallbackActive.pdfStreamingGuardedByCap ? 'warning' : 'success'}
            />
            <button
              type="button"
              onClick={() => void handleRefreshClick()}
              disabled={refreshing}
              className="min-h-[36px] px-3 inline-flex items-center gap-1.5 rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] text-xs font-semibold text-[#1E293B] hover:bg-[#EDF2FA] disabled:opacity-60 dark:border-[#24407A] dark:bg-[#102145] dark:text-[#DCE8FC] dark:hover:bg-[#172D5C]"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${refreshing ? 'animate-spin' : ''}`} />
              <span>{refreshing ? 'Refreshing...' : 'Refresh Telemetry'}</span>
            </button>
          </div>
        </div>

        {elevatedAlerts.length > 0 && (
          <div
            role="status"
            className="rounded-lg border border-[#FCD34D] bg-[#FFFBEB] px-4 py-3 text-xs text-[#92400E] dark:border-[#78350F] dark:bg-[#291D0B] dark:text-[#FDE68A]"
          >
            <span className="font-semibold">
              {elevatedAlerts.length} resource(s) above {dashboard.policy.warningThresholdPct}% threshold:{' '}
            </span>
            <span>
              {elevatedAlerts.map((a) => `${a.label} (${a.utilizationPct}%)`).join(', ')}.
              Core offline study (Notes, MCQs, Past Papers cached locally) remains unaffected.
            </span>
          </div>
        )}

        <div className="md:hidden divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
          {dashboard.alerts.map((alert) => (
            <div key={alert.resource} className="py-3 space-y-1.5 text-xs">
              <div className="flex items-center justify-between gap-2">
                <span className="font-semibold text-[#0F172A] dark:text-white">{alert.label}</span>
                <StatusIndicator
                  label={
                    alert.severity === 'nominal'
                      ? 'Nominal'
                      : alert.severity === 'warning'
                        ? 'Warning (>=70%)'
                        : alert.severity === 'high'
                          ? 'High (>=85%)'
                          : 'Critical (>=95%)'
                  }
                  tone={statusBadgeTone(alert.severity)}
                />
              </div>
              <div className="flex items-center justify-between tabular-nums text-[#334155] dark:text-[#DCE8FC]">
                <span>
                  {alert.used} / {alert.limit} {alert.unit}
                </span>
                <span className="font-semibold">{alert.utilizationPct}%</span>
              </div>
              <p className="text-[#64748B] dark:text-[#8FA7D1]">{alert.mitigation}</p>
            </div>
          ))}
        </div>

        <div className="hidden md:block overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
              <tr>
                <th className="px-4 py-2.5">Resource</th>
                <th className="px-4 py-2.5 text-right">Used / Allowance</th>
                <th className="px-4 py-2.5 text-right">Utilization</th>
                <th className="px-4 py-2.5">State</th>
                <th className="px-4 py-2.5">Mitigation Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
              {dashboard.alerts.map((alert) => (
                <tr key={alert.resource} className="hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60">
                  <td className="px-4 py-3 font-medium text-[#0F172A] dark:text-white whitespace-nowrap">
                    {alert.label}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC] whitespace-nowrap">
                    {alert.used} / {alert.limit} {alert.unit}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums font-semibold text-[#0F172A] dark:text-white">
                    {alert.utilizationPct}%
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <StatusIndicator
                      label={
                        alert.severity === 'nominal'
                          ? 'Nominal'
                          : alert.severity === 'warning'
                            ? 'Warning (>=70%)'
                            : alert.severity === 'high'
                              ? 'High (>=85%)'
                              : 'Critical (>=95%)'
                      }
                      tone={statusBadgeTone(alert.severity)}
                    />
                  </td>
                  <td className="px-4 py-3 text-[#475569] dark:text-[#94A8CE]">
                    {alert.mitigation}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Section B: Live Infrastructure Usage & Remaining Headroom */}
      <section aria-label="Free-tier usage telemetry" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {usageCards.map((card) => {
          const Icon = card.icon;
          return (
            <div
              key={card.label}
              className="rounded-xl border border-[#CBD8EE] bg-white p-5 dark:border-[#223B70] dark:bg-[#0D1B38]"
            >
              <div className="flex items-center justify-between text-xs font-medium text-[#475569] dark:text-[#94A8CE]">
                <span>{card.label}</span>
                <Icon className="h-4 w-4 text-[#1D4ED8] dark:text-[#60A5FA]" />
              </div>
              <div className="mt-2 font-display text-xl font-bold tabular-nums text-[#0F172A] dark:text-white">
                {card.value}
              </div>
              <div className="mt-1.5 text-xs text-[#64748B] dark:text-[#8FA7D1]">
                {card.detail}
              </div>
            </div>
          );
        })}
      </section>

      {/* Section C: Configurable Student Capacity Simulator (300 - 5,000 DAU) */}
      <section className="rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-5 dark:border-[#223B70] dark:bg-[#0D1B38]">
        <div className="space-y-3">
          <div>
            <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
              Student Capacity Model Simulator (300 – 5,000 DAU)
            </h2>
            <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
              Theoretical mathematical projections based on configurable caching, write-batching, and AI cache ratios — not measured live Cloudflare production concurrency.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-1.5">
            {STANDARD_DAU_TIERS.map((dau) => (
              <button
                key={dau}
                type="button"
                onClick={() => setSelectedDau(dau)}
                className={`min-h-[34px] px-3 rounded-lg text-xs font-semibold tabular-nums transition-colors ${
                  selectedDau === dau
                    ? 'bg-[#1D4ED8] text-white dark:bg-[#2563EB]'
                    : 'border border-[#CBD8EE] bg-[#F8FAFE] text-[#334155] hover:bg-[#EDF2FA] dark:border-[#24407A] dark:bg-[#102145] dark:text-[#DCE8FC] dark:hover:bg-[#172D5C]'
                }`}
              >
                {formatNumber(dau)} DAU
              </button>
            ))}
            <button
              type="button"
              onClick={() => setAssumptions(DEFAULT_CAPACITY_ASSUMPTIONS)}
              className="min-h-[34px] px-2.5 inline-flex items-center gap-1 rounded-lg border border-[#CBD8EE] text-xs font-medium text-[#475569] hover:text-[#0F172A] dark:border-[#24407A] dark:text-[#94A8CE] dark:hover:text-white"
              title="Reset model assumptions to defaults"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              <span>Reset</span>
            </button>
          </div>
        </div>

        {/* Adjustable Assumption Sliders */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 rounded-xl border border-[#E2E8F0] bg-[#F8FAFE] p-4 dark:border-[#1B3263] dark:bg-[#091530]">
          <label className="block space-y-1.5 text-xs">
            <div className="flex justify-between font-medium text-[#1E293B] dark:text-[#DCE8FC]">
              <span>CDN / 304 Cache Hit</span>
              <span className="tabular-nums font-semibold">
                {Math.round(assumptions.publicCacheHitRatio * 100)}%
              </span>
            </div>
            <input
              type="range"
              min={0}
              max={95}
              step={5}
              value={Math.round(assumptions.publicCacheHitRatio * 100)}
              onChange={(e) =>
                setAssumptions((prev) => ({
                  ...prev,
                  publicCacheHitRatio: Number(e.target.value) / 100,
                }))
              }
              className="w-full accent-[#2563EB]"
            />
          </label>

          <label className="block space-y-1.5 text-xs">
            <div className="flex justify-between font-medium text-[#1E293B] dark:text-[#DCE8FC]">
              <span>PDF Downloads / Student / Day</span>
              <span className="tabular-nums font-semibold">
                {assumptions.pdfDownloadsPerStudentPerDay.toFixed(2)}
              </span>
            </div>
            <input
              type="range"
              min={5}
              max={100}
              step={5}
              value={Math.round(assumptions.pdfDownloadsPerStudentPerDay * 100)}
              onChange={(e) =>
                setAssumptions((prev) => ({
                  ...prev,
                  pdfDownloadsPerStudentPerDay: Number(e.target.value) / 100,
                }))
              }
              className="w-full accent-[#2563EB]"
            />
          </label>

          <label className="block space-y-1.5 text-xs">
            <div className="flex justify-between font-medium text-[#1E293B] dark:text-[#DCE8FC]">
              <span>AI Tutor Usage / Student</span>
              <span className="tabular-nums font-semibold">
                {Math.round(assumptions.aiTutorUsageRatePerStudent * 100)}%
              </span>
            </div>
            <input
              type="range"
              min={0}
              max={100}
              step={5}
              value={Math.round(assumptions.aiTutorUsageRatePerStudent * 100)}
              onChange={(e) =>
                setAssumptions((prev) => ({
                  ...prev,
                  aiTutorUsageRatePerStudent: Number(e.target.value) / 100,
                }))
              }
              className="w-full accent-[#2563EB]"
            />
          </label>

          <label className="block space-y-1.5 text-xs">
            <div className="flex justify-between font-medium text-[#1E293B] dark:text-[#DCE8FC]">
              <span>AI D1 Response Cache Hit</span>
              <span className="tabular-nums font-semibold">
                {Math.round(assumptions.aiCacheHitRatio * 100)}%
              </span>
            </div>
            <input
              type="range"
              min={0}
              max={90}
              step={5}
              value={Math.round(assumptions.aiCacheHitRatio * 100)}
              onChange={(e) =>
                setAssumptions((prev) => ({
                  ...prev,
                  aiCacheHitRatio: Number(e.target.value) / 100,
                }))
              }
              className="w-full accent-[#2563EB]"
            />
          </label>
        </div>

        {/* Highlighted Selected Tier Summary */}
        <div className="rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] px-4 py-3 text-xs flex flex-col md:flex-row md:items-center justify-between gap-2 dark:border-[#24407A] dark:bg-[#0E1E40]">
          <div className="space-y-0.5">
            <div className="font-semibold text-[#0F172A] dark:text-white">
              Selected Tier ({formatNumber(selectedTierProjection.dailyActiveStudents)} DAU · Theoretical Projection):{' '}
              <span className="tabular-nums">
                {formatNumber(selectedTierProjection.metrics.workerRequestsPerDay)} Worker req/day,{' '}
                {formatNumber(selectedTierProjection.metrics.d1RowsReadPerDay)} D1 reads/day,{' '}
                {formatNumber(selectedTierProjection.metrics.d1RowsWrittenPerDay)} D1 writes/day,{' '}
                {selectedTierProjection.metrics.b2DailyDownloadGb} GB/day B2
              </span>
            </div>
            <div className="text-[#475569] dark:text-[#94A8CE]">
              {selectedTierProjection.bottlenecks.length > 0
                ? `Primary bottlenecks: ${selectedTierProjection.bottlenecks.join(' · ')}`
                : 'No free-tier ceilings breached at this configuration.'}
            </div>
          </div>
          <div className="shrink-0">
            <StatusIndicator
              label={scaleStatusLabel(selectedTierProjection.status).label}
              tone={scaleStatusLabel(selectedTierProjection.status).tone}
            />
          </div>
        </div>

        {/* Full 300-5000 DAU Theoretical Matrix */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
              <tr>
                <th className="px-3.5 py-2.5">DAU Tier</th>
                <th className="px-3.5 py-2.5 text-right">Worker Req/Day</th>
                <th className="px-3.5 py-2.5 text-right">D1 Reads/Day</th>
                <th className="px-3.5 py-2.5 text-right">D1 Writes/Day</th>
                <th className="px-3.5 py-2.5 text-right">B2 Egress/Day</th>
                <th className="px-3.5 py-2.5 text-right">AI Calls/Day</th>
                <th className="px-3.5 py-2.5 text-right">Peak Conc.</th>
                <th className="px-3.5 py-2.5">Free-Tier Verdict</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
              {simulatedMatrix.map((row) => {
                const verdict = scaleStatusLabel(row.status);
                const isSelected = row.dailyActiveStudents === selectedDau;
                return (
                  <tr
                    key={row.dailyActiveStudents}
                    onClick={() => setSelectedDau(row.dailyActiveStudents)}
                    className={`cursor-pointer transition-colors ${
                      isSelected
                        ? 'bg-[#EDF3FD] dark:bg-[#132954]'
                        : 'hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60'
                    }`}
                  >
                    <td className="px-3.5 py-3 font-semibold tabular-nums text-[#0F172A] dark:text-white whitespace-nowrap">
                      {formatNumber(row.dailyActiveStudents)} DAU
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC] whitespace-nowrap">
                      {formatNumber(row.metrics.workerRequestsPerDay)}{' '}
                      <span className="text-[#64748B] dark:text-[#8FA7D1]">
                        ({row.utilizationPct.workerRequests}%)
                      </span>
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC] whitespace-nowrap">
                      {formatNumber(row.metrics.d1RowsReadPerDay)}{' '}
                      <span className="text-[#64748B] dark:text-[#8FA7D1]">
                        ({row.utilizationPct.d1Reads}%)
                      </span>
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC] whitespace-nowrap">
                      {formatNumber(row.metrics.d1RowsWrittenPerDay)}{' '}
                      <span className="text-[#64748B] dark:text-[#8FA7D1]">
                        ({row.utilizationPct.d1Writes}%)
                      </span>
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC] whitespace-nowrap">
                      {row.metrics.b2DailyDownloadGb} GB{' '}
                      <span className="text-[#64748B] dark:text-[#8FA7D1]">
                        ({row.utilizationPct.b2Download}%)
                      </span>
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC] whitespace-nowrap">
                      {formatNumber(row.metrics.aiCallsPerDay)}
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC] whitespace-nowrap">
                      ~{row.metrics.estimatedPeakConcurrentUsers} ({row.metrics.peakRequestsPerSecond}/s)
                    </td>
                    <td className="px-3.5 py-3 whitespace-nowrap">
                      <StatusIndicator label={verdict.label} tone={verdict.tone} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>

      {/* Section D: Isolated Local Load-Test Benchmark Evidence */}
      <section className="rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
              Measured Progressive Load-Test Evidence ({LATEST_LOAD_TEST_SNAPSHOT.environmentLabel})
            </h2>
            <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
              {LATEST_LOAD_TEST_SNAPSHOT.disclaimer}
            </p>
          </div>
          {LATEST_LOAD_TEST_SNAPSHOT.recordedAtIso && (
            <span className="text-xs tabular-nums text-[#475569] dark:text-[#94A8CE]">
              Recorded: {LATEST_LOAD_TEST_SNAPSHOT.recordedAtIso.slice(0, 19).replace('T', ' ')} UTC
            </span>
          )}
        </div>

        {!LATEST_LOAD_TEST_SNAPSHOT.measured || LATEST_LOAD_TEST_SNAPSHOT.stages.length === 0 ? (
          <div className="rounded-lg border border-[#E2E8F0] bg-[#F8FAFE] p-4 text-xs text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#94A8CE]">
            No local load-test snapshot recorded yet. Execute{' '}
            <code className="font-mono text-[#0F172A] dark:text-white">./scripts/run-load-test.sh</code>{' '}
            to run progressive concurrency stages (10, 25, 50, 100 simulated students) against an isolated in-memory SQLite + Worker router and save verified telemetry here.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
                <tr>
                  <th className="px-3.5 py-2.5">Concurrency</th>
                  <th className="px-3.5 py-2.5 text-right">Requests</th>
                  <th className="px-3.5 py-2.5 text-right">Throughput</th>
                  <th className="px-3.5 py-2.5 text-right">304 Cache Hit</th>
                  <th className="px-3.5 py-2.5 text-right">SQL Queries/Req</th>
                  <th className="px-3.5 py-2.5 text-right">p50 / p95 / p99 Latency</th>
                  <th className="px-3.5 py-2.5 text-right">Errors</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
                {LATEST_LOAD_TEST_SNAPSHOT.stages.map((st) => (
                  <tr key={st.concurrency} className="hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60">
                    <td className="px-3.5 py-3 font-semibold tabular-nums text-[#0F172A] dark:text-white">
                      {st.concurrency} parallel students
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                      {formatNumber(st.requestsSent)}
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums font-medium text-[#0F172A] dark:text-white">
                      {st.throughputReqPerSec} req/s
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                      {st.cache304RatePct}% ({st.cache304Hits})
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                      {st.sqlAvgPerRequest}
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                      {st.p50LatencyMs} / {st.p95LatencyMs} / {st.p99LatencyMs} ms
                    </td>
                    <td className="px-3.5 py-3 text-right tabular-nums">
                      <StatusIndicator
                        label={String(st.errorsCount)}
                        tone={st.errorsCount === 0 ? 'success' : 'danger'}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Section E: Cost & Quota Protection Policy Form */}
      <form
        onSubmit={(e) => void handlePolicySubmit(e)}
        className="rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-5 dark:border-[#223B70] dark:bg-[#0D1B38]"
      >
        <div className="flex items-start gap-2.5">
          <ShieldCheck className="h-5 w-5 shrink-0 mt-0.5 text-[#1D4ED8] dark:text-[#60A5FA]" />
          <div>
            <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
              Cost & Quota Protection Controls
            </h2>
            <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
              Configure alert percentages, AI Tutor global/student caps, cache TTL, and automated critical safeguards.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>Warning Alert Threshold (%)</span>
            <input
              type="number"
              min={40}
              max={95}
              value={draftPolicy.warningThresholdPct}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, warningThresholdPct: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>

          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>High Alert Threshold (%)</span>
            <input
              type="number"
              min={50}
              max={98}
              value={draftPolicy.highThresholdPct}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, highThresholdPct: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>

          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>Critical Safeguard Threshold (%)</span>
            <input
              type="number"
              min={60}
              max={100}
              value={draftPolicy.criticalThresholdPct}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, criticalThresholdPct: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>

          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>AI Tutor Global Daily Cap (Calls/Day)</span>
            <input
              type="number"
              min={0}
              max={5000}
              value={draftPolicy.aiTutorDailyGlobalCap}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, aiTutorDailyGlobalCap: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>

          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>AI Tutor Per-Student Cap (Calls/Day)</span>
            <input
              type="number"
              min={0}
              max={100}
              value={draftPolicy.aiTutorDailyPerStudentCap}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, aiTutorDailyPerStudentCap: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>

          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>AI Response Cache TTL (Hours)</span>
            <input
              type="number"
              min={1}
              max={720}
              value={draftPolicy.aiCacheTtlHours}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, aiCacheTtlHours: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>

          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>B2 Daily Download Soft Cap (MB)</span>
            <input
              type="number"
              min={50}
              max={10240}
              value={draftPolicy.b2DailyDownloadSoftCapMb}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, b2DailyDownloadSoftCapMb: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>

          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>D1 Daily Write Soft Cap (Rows)</span>
            <input
              type="number"
              min={1000}
              max={100000}
              value={draftPolicy.d1DailyWriteSoftCap}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, d1DailyWriteSoftCap: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>

          <label className="block space-y-1 text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
            <span>Worker Daily Request Soft Cap</span>
            <input
              type="number"
              min={1000}
              max={100000}
              value={draftPolicy.workerDailyRequestSoftCap}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, workerDailyRequestSoftCap: Number(e.target.value) }))
              }
              className="w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#08132B] dark:text-white"
            />
          </label>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
          <label className="flex items-start gap-2.5 text-xs text-[#1E293B] dark:text-[#DCE8FC] cursor-pointer">
            <input
              type="checkbox"
              checked={draftPolicy.autoDegradeAiAtCritical}
              onChange={(e) =>
                setDraftPolicy((p) => ({ ...p, autoDegradeAiAtCritical: e.target.checked }))
              }
              className="mt-0.5 h-4 w-4 rounded accent-[#2563EB]"
            />
            <span>
              <strong className="font-semibold">Auto-degrade AI Tutor at Critical/Global Cap</strong>
              <span className="block text-[#64748B] dark:text-[#8FA7D1]">
                Returns honest 429 offline guidance while keeping cached AI hits, Notes, MCQs, and Flashcards running.
              </span>
            </span>
          </label>

          <label className="flex items-start gap-2.5 text-xs text-[#1E293B] dark:text-[#DCE8FC] cursor-pointer">
            <input
              type="checkbox"
              checked={draftPolicy.autoBlockUncachedPdfAtCritical}
              onChange={(e) =>
                setDraftPolicy((p) => ({
                  ...p,
                  autoBlockUncachedPdfAtCritical: e.target.checked,
                }))
              }
              className="mt-0.5 h-4 w-4 rounded accent-[#2563EB]"
            />
            <span>
              <strong className="font-semibold">Protect Backblaze B2 Egress at Soft Cap</strong>
              <span className="block text-[#64748B] dark:text-[#8FA7D1]">
                Continues serving 304 cached PDFs while pausing uncached downloads past {draftPolicy.b2DailyDownloadSoftCapMb} MB/day.
              </span>
            </span>
          </label>
        </div>

        {policyNotice && (
          <div
            role="status"
            className={`rounded-lg px-4 py-2.5 text-xs border ${
              policyNotice.type === 'success'
                ? 'border-[#86EFAC] bg-[#F0FDF4] text-[#166534] dark:border-[#14532D] dark:bg-[#0D2618] dark:text-[#86EFAC]'
                : 'border-[#FCA5A5] bg-[#FEF2F2] text-[#991B1B] dark:border-[#7F1D1D] dark:bg-[#2A1215] dark:text-[#FECACA]'
            }`}
          >
            {policyNotice.text}
          </div>
        )}

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={savingPolicy}
            className="min-h-[40px] px-4 inline-flex items-center gap-2 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] disabled:opacity-60 dark:bg-[#2563EB] dark:hover:bg-[#3B82F6] text-white text-xs font-semibold"
          >
            <Save className="h-4 w-4" />
            <span>{savingPolicy ? 'Saving Policy...' : 'Save Quota Protection Policy'}</span>
          </button>
        </div>
      </form>
    </div>
  );
}
