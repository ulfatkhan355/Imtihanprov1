import React, { useMemo, useState } from 'react';
import { CheckCircle2, Search } from 'lucide-react';
import type { ApiFeedbackReport } from '../../shared-contracts/types';
import type { AdminConnectionMode, AdminDataset } from '../types/admin';
import { StageMockNotice, StatusIndicator } from '../components/StateBanner';

interface FeedbackTriagePageProps {
  dataset: AdminDataset;
  connectionMode: AdminConnectionMode;
  onTriageFeedback: (
    feedbackId: string,
    status: ApiFeedbackReport['status'],
    adminResolutionNote: string
  ) => Promise<void> | void;
}

export function FeedbackTriagePage({
  dataset,
  connectionMode,
  onTriageFeedback,
}: FeedbackTriagePageProps) {
  const reports = dataset.feedbackReports ?? [];
  const [statusFilter, setStatusFilter] = useState<ApiFeedbackReport['status'] | 'all'>('all');
  const [search, setSearch] = useState('');
  const [notesById, setNotesById] = useState<Record<string, string>>({});
  const [busyId, setBusyId] = useState<string | null>(null);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const filtered = useMemo(() => {
    return reports.filter((r) => {
      const matchesStatus = statusFilter === 'all' || r.status === statusFilter;
      const matchesSearch =
        !search.trim() ||
        r.title.toLowerCase().includes(search.toLowerCase()) ||
        r.description.toLowerCase().includes(search.toLowerCase());
      return matchesStatus && matchesSearch;
    });
  }, [reports, statusFilter, search]);

  const handleAction = async (
    report: ApiFeedbackReport,
    nextStatus: ApiFeedbackReport['status']
  ) => {
    setBusyId(report.id);
    setNotice(null);
    try {
      const note = notesById[report.id] ?? report.adminResolutionNote ?? '';
      await onTriageFeedback(report.id, nextStatus, note);
      setNotice({ type: 'success', text: `Report "${report.title}" marked as ${nextStatus}.` });
    } catch (err) {
      setNotice({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to update feedback report.',
      });
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-6">
      <StageMockNotice
        title="Student Error Reports & Content Feedback Triage"
        message={
          connectionMode === 'live_api'
            ? 'Student disputes on MCQs, notes typos, and bug reports sync from Android directly into D1 feedback_reports.'
            : 'Offline Preview mode: triage decisions update the local preview queue.'
        }
      />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="relative">
          <Search className="h-4 w-4 absolute left-3 top-1/2 -translate-y-1/2 text-[#64748B] dark:text-[#7A93C2]" />
          <input
            type="search"
            aria-label="Search feedback reports"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search report title or detail..."
            className="h-10 pl-9 pr-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#0D1B38] dark:text-white"
          />
        </div>

        <div className="flex items-center gap-1 p-1 rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] dark:border-[#223B70] dark:bg-[#091530] overflow-x-auto">
          {(['all', 'open', 'triaged', 'resolved', 'dismissed'] as const).map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded text-xs font-medium capitalize whitespace-nowrap shrink-0 ${
                statusFilter === st
                  ? 'bg-[#1D4ED8] text-white dark:bg-[#2563EB]'
                  : 'text-[#475569] hover:text-[#0F172A] dark:text-[#94A8CE] dark:hover:text-white'
              }`}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {notice && (
        <div
          role={notice.type === 'error' ? 'alert' : 'status'}
          className={`rounded-xl border px-4 py-3 text-xs font-medium flex items-center gap-2 ${
            notice.type === 'success'
              ? 'border-[#16A34A]/40 bg-[#F0FDF4] text-[#166534] dark:border-[#22C55E]/40 dark:bg-[#14532D]/30 dark:text-[#BBF7D0]'
              : 'border-[#DC2626]/40 bg-[#FEF2F2] text-[#991B1B] dark:border-[#EF4444]/40 dark:bg-[#450A0A]/40 dark:text-[#FECACA]'
          }`}
        >
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{notice.text}</span>
        </div>
      )}

      {filtered.length === 0 ? (
        <div className="rounded-xl border border-[#CBD8EE] bg-white p-8 text-center text-sm text-[#64748B] dark:border-[#223B70] dark:bg-[#0D1B38] dark:text-[#8FA7D1]">
          No student feedback reports match the active filter.
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((r) => {
            const noteVal = notesById[r.id] ?? r.adminResolutionNote ?? '';
            const tone =
              r.status === 'resolved'
                ? 'success'
                : r.status === 'open'
                  ? 'danger'
                  : r.status === 'triaged'
                    ? 'warning'
                    : 'neutral';
            return (
              <article
                key={r.id}
                className="rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-3 dark:border-[#223B70] dark:bg-[#0D1B38]"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="flex flex-wrap items-center gap-2 text-xs text-[#64748B] dark:text-[#8FA7D1]">
                      <span className="font-mono text-[#1D4ED8] dark:text-[#60A5FA]">
                        {r.category.replace(/_/g, ' ')}
                      </span>
                      <span aria-hidden="true">·</span>
                      <span className="tabular-nums">{r.createdAtIso.slice(0, 10)}</span>
                      {r.chapterId && (
                        <>
                          <span aria-hidden="true">·</span>
                          <span className="font-mono">{r.chapterId}</span>
                        </>
                      )}
                    </div>
                    <h2 className="mt-1 font-display text-base font-semibold text-[#0F172A] dark:text-white">
                      {r.title}
                    </h2>
                  </div>
                  <StatusIndicator label={r.status} tone={tone} />
                </div>

                <p className="text-sm text-[#334155] dark:text-[#C5D5F2]">{r.description}</p>

                <div className="pt-3 border-t border-[#E2E8F0] dark:border-[#1B3263] flex flex-col md:flex-row md:items-center gap-3">
                  <input
                    type="text"
                    aria-label={`Resolution note for ${r.title}`}
                    value={noteVal}
                    onChange={(e) =>
                      setNotesById((prev) => ({ ...prev, [r.id]: e.target.value }))
                    }
                    placeholder="Add editorial resolution note..."
                    className="flex-1 h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                  />
                  <div className="flex flex-wrap items-center gap-2 shrink-0">
                    <button
                      type="button"
                      disabled={busyId === r.id}
                      onClick={() => void handleAction(r, 'triaged')}
                      className="min-h-[36px] px-3 rounded-lg border border-[#CBD8EE] text-xs font-semibold text-[#1E293B] hover:bg-[#F8FAFE] dark:border-[#24407A] dark:text-[#DCE8FC] dark:hover:bg-[#11234A] whitespace-nowrap disabled:opacity-60"
                    >
                      Mark Triaged
                    </button>
                    <button
                      type="button"
                      disabled={busyId === r.id}
                      onClick={() => void handleAction(r, 'resolved')}
                      className="min-h-[36px] px-3 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] text-white text-xs font-semibold whitespace-nowrap disabled:opacity-60"
                    >
                      Resolve
                    </button>
                    <button
                      type="button"
                      disabled={busyId === r.id}
                      onClick={() => void handleAction(r, 'dismissed')}
                      className="min-h-[36px] px-3 rounded-lg border border-[#FCA5A5] text-xs font-semibold text-[#B91C1C] hover:bg-[#FEF2F2] dark:border-[#7F1D1D] dark:text-[#FCA5A5] dark:hover:bg-[#2A1215] whitespace-nowrap disabled:opacity-60"
                    >
                      Dismiss
                    </button>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
