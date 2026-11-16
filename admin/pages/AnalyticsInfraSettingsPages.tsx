import React, { useEffect, useState } from 'react';
import { CheckCircle2, CloudOff, RefreshCw, Save, Server } from 'lucide-react';
import type { AdminInfrastructureStatusResponse, BoardCode } from '../../shared-contracts/types';
import { validateApiBaseUrl } from '../../shared-contracts/rules';
import type { AdminConnectionMode, AdminDataset, AdminSettingsState } from '../types/admin';
import { StageMockNotice, StatusIndicator } from '../components/StateBanner';

interface AnalyticsInfraSettingsProps {
  mode: 'analytics' | 'infrastructure' | 'settings';
  dataset: AdminDataset;
  connectionMode: AdminConnectionMode;
  settings: AdminSettingsState;
  infraStatus: AdminInfrastructureStatusResponse | null;
  onRefreshInfra?: () => Promise<void>;
  onSaveSettings: (next: AdminSettingsState) => Promise<void> | void;
}

export function AnalyticsInfraSettingsPages({
  mode,
  dataset,
  connectionMode,
  settings,
  infraStatus,
  onRefreshInfra,
  onSaveSettings,
}: AnalyticsInfraSettingsProps) {
  const [draftSettings, setDraftSettings] = useState<AdminSettingsState>(settings);
  const [analyticsClassFilter, setAnalyticsClassFilter] = useState<'all' | 9 | 10 | 11 | 12>('all');
  const [busy, setBusy] = useState(false);
  const [settingsMsg, setSettingsMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(
    null
  );

  useEffect(() => {
    setDraftSettings(settings);
  }, [settings]);

  if (mode === 'analytics') {
    const maxAttempts = Math.max(
      ...dataset.analytics.weeklyStudyDistribution.map((s) => s.quizSubmissions),
      1
    );
    const filteredSubjectAccuracy = dataset.analytics.subjectAccuracy.filter(
      (row) => analyticsClassFilter === 'all' || row.classLevel === analyticsClassFilter
    );

    return (
      <div className="space-y-6">
        <StageMockNotice
          title={connectionMode === 'live_api' ? 'Live D1 Quiz & Study Analytics' : 'Offline Preview Analytics'}
          message={
            connectionMode === 'live_api'
              ? 'Aggregated directly from quiz_submissions and student_chapter_progress in D1. Subjects with zero submissions show "Unavailable" rather than fabricated percentages.'
              : 'Displaying offline preview analytics fixtures.'
          }
        />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <section className="rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
                  MCQ Accuracy by Subject (KPK Classes 9–12)
                </h2>
                <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                  Average student quiz score and recorded attempts
                </p>
              </div>
              <div className="flex items-center gap-1 p-1 rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] dark:border-[#223B70] dark:bg-[#091530]">
                {(['all', 9, 10, 11, 12] as const).map((lvl) => (
                  <button
                    key={String(lvl)}
                    type="button"
                    onClick={() => setAnalyticsClassFilter(lvl)}
                    className={`px-2.5 py-1 rounded text-xs font-medium whitespace-nowrap ${
                      analyticsClassFilter === lvl
                        ? 'bg-[#1D4ED8] text-white dark:bg-[#2563EB]'
                        : 'text-[#475569] hover:text-[#0F172A] dark:text-[#94A8CE] dark:hover:text-white'
                    }`}
                  >
                    {lvl === 'all' ? 'All' : `C${lvl}`}
                  </button>
                ))}
              </div>
            </div>

            {filteredSubjectAccuracy.length === 0 ? (
              <div className="py-8 text-center text-xs text-[#64748B] dark:text-[#8FA7D1]">
                No subjects seeded for the selected class filter yet.
              </div>
            ) : (
              <div className="space-y-3.5">
                {filteredSubjectAccuracy.map((row) => (
                  <div key={`${row.classLevel}_${row.subjectName}`} className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-medium text-[#0F172A] dark:text-white">
                        Class {row.classLevel} · {row.subjectName}
                      </span>
                      <span className="tabular-nums text-[#475569] dark:text-[#94A8CE]">
                        {row.averageAccuracyPercent === null
                          ? `Unavailable · ${row.attemptsCount} attempts`
                          : `${row.averageAccuracyPercent}% · ${row.attemptsCount} attempts`}
                      </span>
                    </div>
                    <div className="h-2.5 w-full rounded-full bg-[#E2E8F0] dark:bg-[#162C59] overflow-hidden">
                      <div
                        className="h-full rounded-full bg-[#2563EB] dark:bg-[#3B82F6]"
                        style={{ width: `${row.averageAccuracyPercent ?? 0}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
            <div>
              <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
                Recent Quiz Submission Volume
              </h2>
              <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                Daily quiz submissions recorded in D1
              </p>
            </div>

            {dataset.analytics.weeklyStudyDistribution.length === 0 ? (
              <div className="h-44 flex items-center justify-center text-xs text-[#64748B] dark:text-[#8FA7D1]">
                No quiz submissions recorded in D1 yet.
              </div>
            ) : (
              <div className="grid grid-cols-7 gap-2 items-end h-44 pt-4">
                {dataset.analytics.weeklyStudyDistribution.map((day) => {
                  const heightPct = Math.max(15, Math.round((day.quizSubmissions / maxAttempts) * 100));
                  return (
                    <div key={day.dayLabel} className="flex flex-col items-center gap-2 h-full justify-end">
                      <span className="text-[11px] tabular-nums text-[#475569] dark:text-[#94A8CE]">
                        {day.quizSubmissions}
                      </span>
                      <div
                        className="w-full max-w-[32px] rounded-t-md bg-[#1D4ED8] dark:bg-[#3B82F6]"
                        style={{ height: `${heightPct}%` }}
                      />
                      <span className="text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                        {day.dayLabel}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </div>
    );
  }

  if (mode === 'infrastructure') {
    const infraRows = infraStatus
      ? [
          {
            component: `Cloudflare Worker (${infraStatus.workerName})`,
            scope: `API v${infraStatus.apiVersion}`,
            status: infraStatus.workerReachable ? 'Reachable' : 'Unreachable',
            tone: (infraStatus.workerReachable ? 'success' : 'danger') as 'success' | 'danger',
            detail: `Checked ${infraStatus.checkedAtIso.slice(0, 19).replace('T', ' ')} UTC`,
          },
          {
            component: `Cloudflare D1 (${infraStatus.d1.databaseName})`,
            scope: `ID: ${infraStatus.d1.databaseId}`,
            status: infraStatus.d1.connected ? 'Connected' : 'Disconnected',
            tone: (infraStatus.d1.connected ? 'success' : 'danger') as 'success' | 'danger',
            detail:
              infraStatus.d1.latencyMs !== null
                ? `${infraStatus.d1.latencyMs} ms · ${infraStatus.d1.tableCount ?? 0} tables`
                : 'D1 probe failed',
          },
          {
            component: `Backblaze B2 (${infraStatus.b2.bucketName})`,
            scope: `ID: ${infraStatus.b2.bucketId} · ${infraStatus.b2.endpoint}`,
            status: !infraStatus.b2.configured
              ? 'Missing Secrets'
              : infraStatus.b2.reachable
                ? 'Bucket Reachable'
                : 'Check Credentials',
            tone: (infraStatus.b2.reachable
              ? 'success'
              : infraStatus.b2.configured
                ? 'warning'
                : 'warning') as 'success' | 'warning',
            detail: infraStatus.b2.message,
          },
          {
            component: 'Google Sign-In & Agent Secrets',
            scope: 'GOOGLE_OAUTH_CLIENT_ID / AGENT_INGEST_SECRET',
            status: infraStatus.agentIngest.secretConfigured ? 'Agent Key Set' : 'Agent Key Missing',
            tone: (infraStatus.agentIngest.secretConfigured ? 'success' : 'warning') as
              | 'success'
              | 'warning',
            detail: `Google Client ID: ${infraStatus.googleAuth.clientIdConfigured ? 'Configured' : 'Not Configured'}`,
          },
          {
            component: 'Grounded AI Tutor Provider',
            scope: `GEMINI_API_KEY (${infraStatus.aiTutor?.provider ?? 'gemini'})`,
            status: infraStatus.aiTutor?.configured ? 'Configured' : 'Unconfigured (Optional)',
            tone: (infraStatus.aiTutor?.configured ? 'success' : 'info') as 'success' | 'info',
            detail: infraStatus.aiTutor?.configured
              ? 'Server-side Gemini syllabus tutor active with chapter grounding.'
              : 'Optional GEMINI_API_KEY unset — /v1/tutor/ask returns honest 503 unavailable.',
          },
        ]
      : [
          {
            component: 'Cloudflare Workers API Gateway (imtihan-pro-api)',
            scope: settings.apiBaseUrl,
            status: connectionMode === 'live_api' ? 'Probing...' : 'Offline Preview',
            tone: 'info' as const,
            detail: 'Connect to live Worker to inspect D1 latency and Backblaze B2 bucket reachability.',
          },
          {
            component: 'Cloudflare D1 Database (imtihan-pro-db)',
            scope: 'ID: 079128c1-203b-477b-8c4b-d6b34daf8bbe',
            status: 'Configured Binding',
            tone: 'info' as const,
            detail: 'Apply migrations with wrangler d1 migrations apply imtihan-pro-db',
          },
          {
            component: 'Backblaze B2 Storage (imtihan-pro-assets)',
            scope: 'Bucket ID: 1bdf9ec6cd50dbdfad180110 · s3.eu-central-003.backblazeb2.com',
            status: 'S3 Client Ready',
            tone: 'info' as const,
            detail: 'Set B2_KEY_ID and B2_APPLICATION_KEY Worker secrets before live uploads.',
          },
          {
            component: 'Grounded AI Tutor Provider (GEMINI_API_KEY)',
            scope: 'Optional Worker Secret (server-side proxy only)',
            status: 'Offline Preview',
            tone: 'neutral' as const,
            detail: 'Set GEMINI_API_KEY in Wrangler secrets or backend/.dev.vars to activate AI Tutor.',
          },
        ];

    return (
      <div className="space-y-6">
        <div className="rounded-xl border border-[#CBD8EE] bg-white p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
          <div className="flex items-start gap-3">
            {connectionMode === 'live_api' ? (
              <Server className="h-5 w-5 shrink-0 mt-0.5 text-[#1D4ED8] dark:text-[#60A5FA]" />
            ) : (
              <CloudOff className="h-5 w-5 shrink-0 mt-0.5 text-[#1D4ED8] dark:text-[#60A5FA]" />
            )}
            <div>
              <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
                Cloudflare Worker, D1 &amp; Backblaze B2 Status
              </h2>
              <p className="mt-1 text-xs leading-relaxed text-[#475569] dark:text-[#94A8CE]">
                Bound to Worker <code className="font-mono">imtihan-pro-api</code>, D1{' '}
                <code className="font-mono">imtihan-pro-db</code>, and Backblaze B2{' '}
                <code className="font-mono">imtihan-pro-assets</code> (
                <code className="font-mono">s3.eu-central-003.backblazeb2.com</code>).
              </p>
            </div>
          </div>
          {onRefreshInfra && connectionMode === 'live_api' && (
            <button
              type="button"
              onClick={() => void onRefreshInfra()}
              className="min-h-[38px] px-3.5 inline-flex items-center gap-2 rounded-lg border border-[#CBD8EE] text-xs font-semibold text-[#1E293B] hover:bg-[#F8FAFE] dark:border-[#24407A] dark:text-white dark:hover:bg-[#11234A] whitespace-nowrap shrink-0"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Refresh Live Probe
            </button>
          )}
        </div>

        <div className="rounded-xl border border-[#CBD8EE] bg-white overflow-x-auto dark:border-[#223B70] dark:bg-[#0D1B38]">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] text-xs font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
              <tr>
                <th className="px-5 py-3">Infrastructure Resource</th>
                <th className="px-5 py-3">Binding / Scope</th>
                <th className="px-5 py-3 whitespace-nowrap">Live Status</th>
                <th className="px-5 py-3">Diagnostic Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
              {infraRows.map((item) => (
                <tr key={item.component}>
                  <td className="px-5 py-3.5 font-medium text-[#0F172A] dark:text-white">
                    {item.component}
                  </td>
                  <td className="px-5 py-3.5 font-mono text-xs text-[#475569] dark:text-[#94A8CE]">
                    {item.scope}
                  </td>
                  <td className="px-5 py-3.5 whitespace-nowrap">
                    <StatusIndicator label={item.status} tone={item.tone} />
                  </td>
                  <td className="px-5 py-3.5 text-xs text-[#334155] dark:text-[#C5D5F2]">
                    {item.detail}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  const handleSaveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    const check = validateApiBaseUrl(draftSettings.apiBaseUrl);
    if (!check.valid) {
      setSettingsMsg({ type: 'error', text: check.reason ?? 'Invalid API base URL.' });
      return;
    }
    setBusy(true);
    try {
      await onSaveSettings(draftSettings);
      setSettingsMsg({
        type: 'success',
        text:
          connectionMode === 'live_api'
            ? 'Policy settings persisted to D1 and API Base URL saved locally.'
            : 'Configuration saved in Offline Preview session.',
      });
    } catch (err) {
      setSettingsMsg({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save settings.',
      });
    } finally {
      setBusy(false);
    }
  };

  const adPlacement = draftSettings.adPlacement;

  return (
    <div className="space-y-6">
      <StageMockNotice
        title="Environment-Safe Configuration & Ethical Ad Guardrails"
        message="Worker secrets (B2_KEY_ID, B2_APPLICATION_KEY, ADMIN_SESSION_SECRET, AGENT_INGEST_SECRET, GEMINI_API_KEY) remain strictly server-side in Wrangler secrets or backend/.dev.vars."
      />

      <form
        onSubmit={handleSaveSettings}
        className="rounded-xl border border-[#CBD8EE] bg-white p-6 space-y-6 dark:border-[#223B70] dark:bg-[#0D1B38]"
      >
        <div className="space-y-4">
          <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
            API &amp; Access Policies
          </h2>

          {settingsMsg && (
            <div
              role="status"
              className={`rounded-lg border px-4 py-2.5 text-xs font-medium flex items-center gap-2 ${
                settingsMsg.type === 'success'
                  ? 'border-[#16A34A]/40 bg-[#F0FDF4] text-[#166534] dark:border-[#22C55E]/40 dark:bg-[#14532D]/30 dark:text-[#BBF7D0]'
                  : 'border-[#DC2626]/40 bg-[#FEF2F2] text-[#991B1B] dark:border-[#EF4444]/40 dark:bg-[#450A0A]/40 dark:text-[#FECACA]'
              }`}
            >
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              <span>{settingsMsg.text}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="cfg-api-url" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Cloudflare Worker API Base URL
              </label>
              <input
                id="cfg-api-url"
                type="url"
                value={draftSettings.apiBaseUrl}
                onChange={(e) =>
                  setDraftSettings((prev) => ({ ...prev, apiBaseUrl: e.target.value }))
                }
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white font-mono text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />
            </div>

            <div>
              <label htmlFor="cfg-default-board" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Default Curriculum Board
              </label>
              <select
                id="cfg-default-board"
                value={draftSettings.defaultBoardCode}
                onChange={(e) =>
                  setDraftSettings((prev) => ({
                    ...prev,
                    defaultBoardCode: e.target.value as BoardCode,
                  }))
                }
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              >
                <option value="KPK">KPK (Primary Initial Focus)</option>
                <option value="FEDERAL">Federal</option>
                <option value="PUNJAB">Punjab</option>
              </select>
            </div>
          </div>

          <div className="space-y-2.5 pt-1">
            <label className="flex items-center gap-2.5 text-sm text-[#1E293B] dark:text-[#DCE8FC]">
              <input
                type="checkbox"
                checked={draftSettings.guestFirstMandatory}
                onChange={(e) =>
                  setDraftSettings((prev) => ({ ...prev, guestFirstMandatory: e.target.checked }))
                }
              />
              Enforce Guest-First Access (students learn &amp; store Room progress without sign-in)
            </label>
            <label className="flex items-center gap-2.5 text-sm text-[#1E293B] dark:text-[#DCE8FC]">
              <input
                type="checkbox"
                checked={draftSettings.allowOptionalGoogleSync}
                onChange={(e) =>
                  setDraftSettings((prev) => ({
                    ...prev,
                    allowOptionalGoogleSync: e.target.checked,
                  }))
                }
              />
              Allow Optional Google Sign-In for Cloud Progress Synchronization
            </label>
          </div>
        </div>

        <div className="pt-5 border-t border-[#E2E8F0] dark:border-[#1B3263] space-y-4">
          <h3 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
            Ethical Ad Placement Policy
          </h3>
          <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
            Non-intrusive study banners are off by default. Focus-critical surfaces (Quizzes, Mock Exams, Chapter Notes, PDF Reader) are permanently ad-free.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2.5">
              <label className="flex items-center gap-2.5 text-sm text-[#1E293B] dark:text-[#DCE8FC]">
                <input
                  type="checkbox"
                  checked={adPlacement.enabled}
                  onChange={(e) =>
                    setDraftSettings((prev) => ({
                      ...prev,
                      adPlacement: { ...prev.adPlacement, enabled: e.target.checked },
                    }))
                  }
                />
                Enable non-intrusive study banners globally
              </label>
              <label className="flex items-center gap-2.5 text-sm text-[#1E293B] dark:text-[#DCE8FC]">
                <input
                  type="checkbox"
                  disabled={!adPlacement.enabled}
                  checked={adPlacement.bannerOnHome}
                  onChange={(e) =>
                    setDraftSettings((prev) => ({
                      ...prev,
                      adPlacement: { ...prev.adPlacement, bannerOnHome: e.target.checked },
                    }))
                  }
                />
                Allow banner slot on Home dashboard
              </label>
              <label className="flex items-center gap-2.5 text-sm text-[#1E293B] dark:text-[#DCE8FC]">
                <input
                  type="checkbox"
                  disabled={!adPlacement.enabled}
                  checked={adPlacement.bannerOnPapers}
                  onChange={(e) =>
                    setDraftSettings((prev) => ({
                      ...prev,
                      adPlacement: { ...prev.adPlacement, bannerOnPapers: e.target.checked },
                    }))
                  }
                />
                Allow banner slot on Past Papers list
              </label>
            </div>

            <div className="space-y-3">
              <div>
                <label htmlFor="cfg-ad-cap" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Daily Banner Impression Cap per Student
                </label>
                <input
                  id="cfg-ad-cap"
                  type="number"
                  min={0}
                  max={20}
                  value={adPlacement.dailyBannerCap}
                  onChange={(e) =>
                    setDraftSettings((prev) => ({
                      ...prev,
                      adPlacement: {
                        ...prev.adPlacement,
                        dailyBannerCap: Math.max(0, Number(e.target.value)),
                      },
                    }))
                  }
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm tabular-nums text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
              <div>
                <label htmlFor="cfg-ad-note" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Ad Provider / Governance Note
                </label>
                <input
                  id="cfg-ad-note"
                  type="text"
                  value={adPlacement.providerNote}
                  onChange={(e) =>
                    setDraftSettings((prev) => ({
                      ...prev,
                      adPlacement: { ...prev.adPlacement, providerNote: e.target.value },
                    }))
                  }
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] px-4 py-3 text-xs text-[#334155] dark:border-[#1E3566] dark:bg-[#091530] dark:text-[#94A8CE] space-y-1">
            <div>
              <strong className="text-[#0F172A] dark:text-white">Hardcoded Zero-Distraction Guardrail:</strong>{' '}
              Quiz &amp; Mock Exam suppression = <span className="font-mono">true</span> · Notes &amp; PDF Reading suppression = <span className="font-mono">true</span>.
            </div>
          </div>
        </div>

        <div className="pt-2">
          <button
            type="submit"
            disabled={busy}
            className="min-h-[40px] px-4 inline-flex items-center gap-2 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] text-white text-xs font-semibold disabled:opacity-60"
          >
            <Save className="h-4 w-4" />
            {busy ? 'Saving...' : 'Save Settings'}
          </button>
        </div>
      </form>
    </div>
  );
}
