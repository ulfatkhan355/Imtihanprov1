import React from 'react';
import { ArrowRight, BookOpen, CheckSquare, FileText, Layers } from 'lucide-react';
import type { AdminConnectionMode, AdminDataset, AdminSectionId } from '../types/admin';
import { StageMockNotice, StatusIndicator } from '../components/StateBanner';

interface OverviewPageProps {
  dataset: AdminDataset;
  connectionMode: AdminConnectionMode;
  onNavigate: (section: AdminSectionId) => void;
}

export function OverviewPage({ dataset, connectionMode, onNavigate }: OverviewPageProps) {
  const publishedChapters = dataset.chapters.filter(
    (c) => c.publishStatus === 'published'
  ).length;
  const pendingReviews = dataset.aiSubmissions.filter(
    (s) => s.status === 'queued' || s.status === 'in_review'
  ).length;

  const kpis = [
    {
      label: 'Active Subjects',
      value: dataset.subjects.filter((s) => s.coverageStatus === 'active_seed').length,
      detail: `${dataset.subjects.length} total mapped across Classes 9–12`,
      icon: BookOpen,
      target: 'subjects' as AdminSectionId,
    },
    {
      label: 'Published Chapters',
      value: publishedChapters,
      detail: `${dataset.chapters.length} chapters in curriculum workspace`,
      icon: Layers,
      target: 'chapters' as AdminSectionId,
    },
    {
      label: 'Pending Review Queue',
      value: pendingReviews,
      detail: `${dataset.aiSubmissions.length} total AI submissions`,
      icon: CheckSquare,
      target: 'review-queue' as AdminSectionId,
    },
    {
      label: 'PDF Assets',
      value: dataset.pdfs.length,
      detail: 'Backblaze B2 (imtihan-pro-assets) catalog',
      icon: FileText,
      target: 'pdf-library' as AdminSectionId,
    },
  ];

  return (
    <div className="space-y-6">
      <StageMockNotice
        title={
          connectionMode === 'live_api'
            ? 'Live Cloudflare Worker + D1 + Backblaze B2 Workspace'
            : 'Offline UI Preview Mode'
        }
        message={
          connectionMode === 'live_api'
            ? 'Metrics and audit logs below reflect real D1 database records from imtihan-pro-db.'
            : 'Showing deterministic offline preview data because no live Worker session is active. Connect to imtihan-pro-api from the sidebar to manage real D1 & B2 records.'
        }
      />

      <section aria-label="Key curriculum metrics" className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {kpis.map((kpi) => {
          const Icon = kpi.icon;
          return (
            <button
              key={kpi.label}
              type="button"
              onClick={() => onNavigate(kpi.target)}
              className="text-left rounded-xl border border-[#CBD8EE] bg-white p-5 hover:border-[#3B82F6] transition-colors dark:border-[#223B70] dark:bg-[#0D1B38] dark:hover:border-[#3B82F6] focus-visible:outline-2 focus-visible:outline-[#3B82F6]"
            >
              <div className="flex items-center justify-between text-xs font-medium text-[#475569] dark:text-[#94A8CE]">
                <span>{kpi.label}</span>
                <Icon className="h-4 w-4 text-[#1D4ED8] dark:text-[#60A5FA]" />
              </div>
              <div className="mt-2 font-display text-3xl font-bold tabular-nums text-[#0F172A] dark:text-white">
                {kpi.value}
              </div>
              <div className="mt-1.5 text-xs text-[#64748B] dark:text-[#8FA7D1]">
                {kpi.detail}
              </div>
            </button>
          );
        })}
      </section>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <section className="xl:col-span-2 rounded-xl border border-[#CBD8EE] bg-white dark:border-[#223B70] dark:bg-[#0D1B38]">
          <div className="flex items-center justify-between px-5 py-4 border-b border-[#E2E8F0] dark:border-[#1B3263]">
            <div>
              <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
                KPK Curriculum Coverage (Classes 9–12)
              </h2>
              <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                Content readiness across all four supported secondary and higher-secondary classes
              </p>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('classes')}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#1D4ED8] hover:underline dark:text-[#60A5FA] whitespace-nowrap"
            >
              <span>Manage Classes</span>
              <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] text-xs font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
                <tr>
                  <th className="px-5 py-3">Class</th>
                  <th className="px-5 py-3">Board</th>
                  <th className="px-5 py-3 text-right">Subjects</th>
                  <th className="px-5 py-3 text-right">Published Chapters</th>
                  <th className="px-5 py-3">Readiness</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
                {dataset.classes.map((cls) => (
                  <tr
                    key={cls.id}
                    className="hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60 transition-colors"
                  >
                    <td className="px-5 py-3.5 font-medium text-[#0F172A] dark:text-white">
                      {cls.label}
                    </td>
                    <td className="px-5 py-3.5 text-xs text-[#475569] dark:text-[#94A8CE]">
                      {cls.boardCode}
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                      {cls.subjectCount}
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                      {cls.publishedChaptersCount}
                    </td>
                    <td className="px-5 py-3.5">
                      {cls.coverageStatus === 'active_seed' ? (
                        <StatusIndicator label="Active Seed" tone="success" />
                      ) : (
                        <StatusIndicator label="Structure Ready" tone="info" />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="rounded-xl border border-[#CBD8EE] bg-white dark:border-[#223B70] dark:bg-[#0D1B38]">
          <div className="px-5 py-4 border-b border-[#E2E8F0] dark:border-[#1B3263]">
            <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
              Recent Audit Activity
            </h2>
            <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
              {connectionMode === 'live_api' ? 'Live D1 audit trail' : 'Offline preview session log'}
            </p>
          </div>
          {dataset.auditLog.length === 0 ? (
            <div className="px-5 py-8 text-xs text-[#64748B] dark:text-[#8FA7D1]">
              No audit events recorded yet.
            </div>
          ) : (
            <ul className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
              {dataset.auditLog.slice(0, 6).map((entry) => (
                <li key={entry.id} className="px-5 py-3.5">
                  <div className="text-xs font-semibold text-[#0F172A] dark:text-white">
                    {entry.action}
                  </div>
                  <div className="mt-0.5 text-xs text-[#475569] dark:text-[#94A8CE]">
                    {entry.targetEntity}
                  </div>
                  <div className="mt-1 text-[11px] tabular-nums text-[#64748B] dark:text-[#7A93C2]">
                    {entry.actorName} · {entry.timestampIso.slice(0, 10)}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
