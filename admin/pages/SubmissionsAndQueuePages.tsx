import React, { useMemo, useState } from 'react';
import { ArrowUpRight, CheckCircle2, RotateCcw, XCircle } from 'lucide-react';
import type { ApiAiAgentSubmission } from '../../shared-contracts/types';
import type { AdminConnectionMode, AdminDataset } from '../types/admin';
import { StageMockNotice, StatusIndicator } from '../components/StateBanner';

interface SubmissionsProps {
  mode: 'ai-submissions' | 'review-queue';
  dataset: AdminDataset;
  connectionMode: AdminConnectionMode;
  onTransition: (
    submissionId: string,
    nextStatus: ApiAiAgentSubmission['status'],
    reviewerNotes: string
  ) => Promise<{ ok: boolean; error?: string }> | { ok: boolean; error?: string };
  onPromoteApproved?: (submissionId: string) => Promise<{ ok: boolean; error?: string }>;
}

export function SubmissionsAndQueuePages({
  mode,
  dataset,
  connectionMode,
  onTransition,
  onPromoteApproved,
}: SubmissionsProps) {
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedId, setSelectedId] = useState<string>(dataset.aiSubmissions[0]?.id ?? '');
  const [notesDraft, setNotesDraft] = useState<string>('');
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  const visibleSubmissions = useMemo(() => {
    return dataset.aiSubmissions.filter((item) => {
      if (mode === 'review-queue' && statusFilter === 'all') {
        return item.status === 'queued' || item.status === 'in_review' || item.status === 'revision_requested';
      }
      if (statusFilter === 'all') return true;
      return item.status === statusFilter;
    });
  }, [dataset.aiSubmissions, mode, statusFilter]);

  const activeItem =
    visibleSubmissions.find((s) => s.id === selectedId) ?? visibleSubmissions[0] ?? null;

  const renderStatus = (status: ApiAiAgentSubmission['status']) => {
    switch (status) {
      case 'approved':
        return <StatusIndicator label="Approved" tone="success" />;
      case 'in_review':
        return <StatusIndicator label="In Review" tone="info" />;
      case 'queued':
        return <StatusIndicator label="Queued" tone="warning" />;
      case 'revision_requested':
        return <StatusIndicator label="Revision Requested" tone="warning" />;
      case 'rejected':
        return <StatusIndicator label="Rejected" tone="danger" />;
    }
  };

  const triggerDecision = async (nextStatus: ApiAiAgentSubmission['status']) => {
    if (!activeItem) return;
    setBusy(true);
    try {
      const res = await onTransition(
        activeItem.id,
        nextStatus,
        notesDraft.trim() || activeItem.reviewerNotes || `Marked ${nextStatus} during editorial review.`
      );
      if (res.ok) {
        setFeedback(`Updated "${activeItem.chapterTitle}" to ${nextStatus}.`);
        setNotesDraft('');
      } else if (res.error) {
        setFeedback(res.error);
      }
    } finally {
      setBusy(false);
    }
  };

  const triggerPromote = async () => {
    if (!activeItem || !onPromoteApproved) return;
    setBusy(true);
    try {
      const res = await onPromoteApproved(activeItem.id);
      if (res.ok) {
        setFeedback(`Promoted "${activeItem.chapterTitle}" into Chapter Drafts / MCQ Bank.`);
      } else if (res.error) {
        setFeedback(res.error);
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      <StageMockNotice
        title="Editorial Verification Pipeline"
        message={
          connectionMode === 'live_api'
            ? 'AI agent submissions arrive via POST /v1/agent/submissions with X-Agent-Secret, enter the review queue safely, and require human approval + promotion before publishing.'
            : 'Offline Preview mode: inspect queue workflows and test state transitions locally.'
        }
      />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-1 p-1 rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] dark:border-[#223B70] dark:bg-[#091530]">
          {['all', 'queued', 'in_review', 'revision_requested', 'approved', 'rejected'].map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded text-xs font-medium capitalize transition-colors whitespace-nowrap ${
                statusFilter === st
                  ? 'bg-[#1D4ED8] text-white dark:bg-[#2563EB]'
                  : 'text-[#475569] hover:text-[#0F172A] dark:text-[#94A8CE] dark:hover:text-white'
              }`}
            >
              {st.replace('_', ' ')}
            </button>
          ))}
        </div>

        {feedback && (
          <div role="status" className="text-xs font-medium text-[#1D4ED8] dark:text-[#60A5FA]">
            {feedback}
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
        <div className="xl:col-span-7 rounded-xl border border-[#CBD8EE] bg-white overflow-x-auto dark:border-[#223B70] dark:bg-[#0D1B38]">
          {visibleSubmissions.length === 0 ? (
            <div className="p-8 text-center text-sm text-[#475569] dark:text-[#94A8CE]">
              No submissions match the selected filter.
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] text-xs font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
                <tr>
                  <th className="px-4 py-3">Submission</th>
                  <th className="px-4 py-3">Schema</th>
                  <th className="px-4 py-3 text-right">Items</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
                {visibleSubmissions.map((sub) => {
                  const selected = activeItem?.id === sub.id;
                  return (
                    <tr
                      key={sub.id}
                      onClick={() => setSelectedId(sub.id)}
                      className={`cursor-pointer transition-colors ${
                        selected
                          ? 'bg-[#EFF6FF] dark:bg-[#132957]'
                          : 'hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60'
                      }`}
                    >
                      <td className="px-4 py-3.5">
                        <div className="font-medium text-[#0F172A] dark:text-white">
                          {sub.chapterTitle}
                        </div>
                        <div className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                          {sub.agentName} · Class {sub.targetClass} {sub.subjectName} · {sub.submissionType}
                        </div>
                      </td>
                      <td className="px-4 py-3.5">
                        {sub.schemaValidationPassed ? (
                          <StatusIndicator label="Valid" tone="success" />
                        ) : (
                          <StatusIndicator label="Needs Fix" tone="danger" />
                        )}
                      </td>
                      <td className="px-4 py-3.5 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                        {sub.blocksCount}
                      </td>
                      <td className="px-4 py-3.5">{renderStatus(sub.status)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        <aside className="xl:col-span-5 rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
          {activeItem ? (
            <>
              <div className="flex items-start justify-between gap-2">
                <div>
                  <div className="text-xs font-mono text-[#1D4ED8] dark:text-[#60A5FA]">
                    {activeItem.id} · {activeItem.modelIdentifier}
                  </div>
                  <h2 className="mt-1 font-display text-base font-bold text-[#0F172A] dark:text-white">
                    {activeItem.chapterTitle}
                  </h2>
                </div>
                {renderStatus(activeItem.status)}
              </div>

              <div className="text-xs text-[#475569] dark:text-[#94A8CE]">
                Board: {activeItem.boardCode} · Class {activeItem.targetClass} · Submitted{' '}
                <span className="tabular-nums">{activeItem.submittedAtIso.slice(0, 10)}</span>
              </div>

              {activeItem.validationWarnings.length > 0 && (
                <div className="rounded-lg border border-[#F59E0B]/40 bg-[#FFFBEB] p-3 text-xs text-[#92400E] dark:border-[#B45309]/50 dark:bg-[#2B210B] dark:text-[#FEF3C7]">
                  <div className="font-semibold">Schema / Quality Warnings</div>
                  <ul className="mt-1 list-disc pl-4 space-y-1">
                    {activeItem.validationWarnings.map((w, i) => (
                      <li key={i}>{w}</li>
                    ))}
                  </ul>
                </div>
              )}

              <div>
                <label
                  htmlFor="reviewer-notes"
                  className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]"
                >
                  Reviewer Notes
                </label>
                <textarea
                  id="reviewer-notes"
                  rows={3}
                  value={notesDraft}
                  onChange={(e) => setNotesDraft(e.target.value)}
                  placeholder={activeItem.reviewerNotes || 'Add editorial verification notes...'}
                  className="mt-1 w-full p-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>

              <div className="flex flex-wrap gap-2 pt-1">
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void triggerDecision('approved')}
                  className="min-h-[40px] px-3.5 inline-flex items-center gap-1.5 rounded-lg bg-[#16A34A] hover:bg-[#15803D] text-white text-xs font-semibold disabled:opacity-60"
                >
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  Approve
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void triggerDecision('revision_requested')}
                  className="min-h-[40px] px-3.5 inline-flex items-center gap-1.5 rounded-lg border border-[#D97706] text-[#B45309] hover:bg-[#FEF3C7] dark:text-[#FBBF24] dark:hover:bg-[#2B210B] text-xs font-semibold disabled:opacity-60"
                >
                  <RotateCcw className="h-3.5 w-3.5" />
                  Request Revision
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void triggerDecision('rejected')}
                  className="min-h-[40px] px-3.5 inline-flex items-center gap-1.5 rounded-lg border border-[#DC2626] text-[#DC2626] hover:bg-[#FEF2F2] dark:text-[#F87171] dark:hover:bg-[#2A1215] text-xs font-semibold disabled:opacity-60"
                >
                  <XCircle className="h-3.5 w-3.5" />
                  Reject
                </button>

                {connectionMode === 'live_api' &&
                  activeItem.status === 'approved' &&
                  onPromoteApproved && (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => void triggerPromote()}
                      className="min-h-[40px] px-3.5 inline-flex items-center gap-1.5 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] text-white text-xs font-semibold disabled:opacity-60"
                    >
                      <ArrowUpRight className="h-3.5 w-3.5" />
                      Promote to Draft
                    </button>
                  )}
              </div>
            </>
          ) : (
            <div className="text-sm text-[#475569] dark:text-[#94A8CE]">
              Select a submission from the queue to inspect validation checks and record an editorial decision.
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
