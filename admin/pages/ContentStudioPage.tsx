import React, { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Plus, Rocket, Save, Send, ShieldCheck, Trash2 } from 'lucide-react';
import type {
  AdminMcqWritePayload,
  ContentBlockType,
  EducationalContentBlock,
  PublishStatus,
} from '../../shared-contracts/types';
import type { AdminConnectionMode, AdminDataset } from '../types/admin';
import { ContentBlockPreview } from '../components/ContentBlockPreview';
import { StageMockNotice, StatusIndicator } from '../components/StateBanner';

interface ContentStudioPageProps {
  dataset: AdminDataset;
  connectionMode: AdminConnectionMode;
  selectedChapterId: string;
  onSelectChapter: (chapterId: string) => void;
  onSaveBlocks: (
    chapterId: string,
    blocks: EducationalContentBlock[]
  ) => Promise<{ ok: boolean; errors: string[]; contentId?: string }> | { ok: boolean; errors: string[]; contentId?: string };
  onTransitionContentStatus?: (
    contentId: string,
    status: PublishStatus
  ) => Promise<{ ok: boolean; error?: string }>;
  onPublishContent: (contentId: string) => Promise<{ ok: boolean; version?: string; error?: string }>;
  onCreateMcq?: (payload: AdminMcqWritePayload) => Promise<void> | void;
  onDeleteMcq?: (mcqId: string) => Promise<void> | void;
}

function selectPreferredChapterContent(
  contents: AdminDataset['contents'],
  chapterId: string
) {
  const candidates = contents.filter((c) => c.chapterId === chapterId && c.publishStatus !== 'rejected');
  if (candidates.length === 0) return contents.find((c) => c.chapterId === chapterId);
  const rankStatus = (s: PublishStatus) =>
    s === 'draft' ? 4 : s === 'in_review' ? 3 : s === 'approved' ? 2 : s === 'published' ? 1 : 0;
  return [...candidates].sort((a, b) => {
    const typeDelta = (b.mode === 'detailed_notes' ? 1 : 0) - (a.mode === 'detailed_notes' ? 1 : 0);
    if (typeDelta !== 0) return typeDelta;
    const statusDelta = rankStatus(b.publishStatus) - rankStatus(a.publishStatus);
    if (statusDelta !== 0) return statusDelta;
    return (b.updatedAtIso || '').localeCompare(a.updatedAtIso || '');
  })[0];
}

export function ContentStudioPage({
  dataset,
  connectionMode,
  selectedChapterId,
  onSelectChapter,
  onSaveBlocks,
  onTransitionContentStatus,
  onPublishContent,
  onCreateMcq,
  onDeleteMcq,
}: ContentStudioPageProps) {
  const activeContent = selectPreferredChapterContent(dataset.contents, selectedChapterId);
  const activeChapter = dataset.chapters.find((c) => c.id === selectedChapterId);
  const activeSubject = dataset.subjects.find((s) => s.id === activeChapter?.subjectId);

  const [studioTab, setStudioTab] = useState<'blocks' | 'mcqs'>('blocks');
  const [blocks, setBlocks] = useState<EducationalContentBlock[]>(activeContent?.blocks ?? []);
  const [latestContentId, setLatestContentId] = useState<string | undefined>(activeContent?.id);
  const [newBlockType, setNewBlockType] = useState<ContentBlockType>('paragraph');
  const [newHeadingOrText, setNewHeadingOrText] = useState('');
  const [newSecondaryText, setNewSecondaryText] = useState('');
  const [busy, setBusy] = useState(false);
  const [saveMessage, setSaveMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(
    null
  );

  // MCQ Question Bank Editor State
  const [mcqQuestion, setMcqQuestion] = useState('');
  const [optA, setOptA] = useState('');
  const [optB, setOptB] = useState('');
  const [optC, setOptC] = useState('');
  const [optD, setOptD] = useState('');
  const [correctOpt, setCorrectOpt] = useState<'A' | 'B' | 'C' | 'D'>('A');
  const [mcqDifficulty, setMcqDifficulty] = useState<'easy' | 'medium' | 'hard'>('medium');
  const [mcqTopic, setMcqTopic] = useState('');
  const [mcqExplanation, setMcqExplanation] = useState('');

  useEffect(() => {
    const found = selectPreferredChapterContent(dataset.contents, selectedChapterId);
    setBlocks(found?.blocks ?? []);
    setLatestContentId(found?.id);
    setSaveMessage(null);
  }, [selectedChapterId, dataset.contents]);

  const chapterMcqs = useMemo(() => {
    const all = dataset.mcqs ?? [];
    return all.filter((m) => m.chapterId === selectedChapterId);
  }, [dataset.mcqs, selectedChapterId]);

  const handleAddBlock = (e: React.FormEvent) => {
    e.preventDefault();
    const id = `blk_${Date.now()}`;
    const orderIndex = blocks.length + 1;
    const primary = newHeadingOrText.trim();
    if (!primary) {
      setSaveMessage({ type: 'error', text: 'Enter content text before adding a block.' });
      return;
    }

    let created: EducationalContentBlock;
    switch (newBlockType) {
      case 'heading':
      case 'subheading':
        created = { id, orderIndex, type: newBlockType, text: primary };
        break;
      case 'math_expression':
        created = {
          id,
          orderIndex,
          type: 'math_expression',
          latex: primary,
          plainNotation: newSecondaryText.trim() || primary,
          caption: 'Custom formula block',
        };
        break;
      case 'definition':
        created = {
          id,
          orderIndex,
          type: 'definition',
          term: primary,
          definition: newSecondaryText.trim() || 'Definition details to be finalized by reviewer.',
        };
        break;
      case 'exam_point':
        created = {
          id,
          orderIndex,
          type: 'exam_point',
          title: 'Board Exam Point',
          boardFrequency: 'High',
          points: [primary],
        };
        break;
      case 'list':
        created = {
          id,
          orderIndex,
          type: 'list',
          ordered: false,
          title: 'Key Points',
          items: primary.split(';').map((s) => s.trim()).filter(Boolean),
        };
        break;
      case 'table':
        created = {
          id,
          orderIndex,
          type: 'table',
          title: primary,
          headers: ['Quantity', 'Symbol', 'SI Unit'],
          rows: [['Frequency', 'f', 'Hz']],
        };
        break;
      case 'example':
        created = {
          id,
          orderIndex,
          type: 'example',
          title: 'Worked Example',
          problemStatement: primary,
          steps: [newSecondaryText.trim() || 'Step 1: Substitute given values into formula.'],
          finalAnswer: 'See solution steps',
        };
        break;
      case 'diagram':
        created = {
          id,
          orderIndex,
          type: 'diagram',
          title: primary,
          caption: newSecondaryText.trim() || 'Conceptual diagram asset key in Backblaze B2.',
          assetKey: `diagram_${Date.now()}`,
          altText: primary,
          aspectRatio: '16:9',
        };
        break;
      case 'mcq':
        created = {
          id,
          orderIndex,
          type: 'mcq',
          question: primary,
          options: [
            { id: 'A', text: newSecondaryText.trim() || 'Option A' },
            { id: 'B', text: 'Option B' },
            { id: 'C', text: 'Option C' },
            { id: 'D', text: 'Option D' },
          ],
          correctOptionId: 'A',
          explanation: 'Draft MCQ added from Content Studio.',
          difficulty: 'medium',
        };
        break;
      case 'solved_question':
        created = {
          id,
          orderIndex,
          type: 'solved_question',
          questionType: 'short',
          marks: 4,
          question: primary,
          answerSteps: [newSecondaryText.trim() || 'Draft answer step.'],
        };
        break;
      default:
        created = { id, orderIndex, type: 'paragraph', text: primary };
    }

    setBlocks((prev) => [...prev, created]);
    setNewHeadingOrText('');
    setNewSecondaryText('');
    setSaveMessage(null);
  };

  const handleSave = async () => {
    setBusy(true);
    try {
      const res = await onSaveBlocks(selectedChapterId, blocks);
      if (res.ok) {
        if (res.contentId) setLatestContentId(res.contentId);
        setSaveMessage({
          type: 'success',
          text:
            connectionMode === 'live_api'
              ? `Saved ${blocks.length} structured blocks to D1 draft (${res.contentId ?? 'draft'}).`
              : `Saved ${blocks.length} structured blocks in Offline Preview.`,
        });
      } else {
        setSaveMessage({
          type: 'error',
          text: res.errors[0] ?? 'Validation failed.',
        });
      }
    } finally {
      setBusy(false);
    }
  };

  const handleWorkflowStep = async (nextStatus: PublishStatus) => {
    if (!latestContentId || !onTransitionContentStatus) return;
    setBusy(true);
    try {
      const res = await onTransitionContentStatus(latestContentId, nextStatus);
      if (res.ok) {
        setSaveMessage({
          type: 'success',
          text: `Content status moved to "${nextStatus.replace(/_/g, ' ')}".`,
        });
      } else {
        setSaveMessage({
          type: 'error',
          text: res.error ?? 'Transition failed.',
        });
      }
    } finally {
      setBusy(false);
    }
  };

  const handlePublish = async () => {
    if (!latestContentId) {
      setSaveMessage({ type: 'error', text: 'Save a draft first before publishing a version.' });
      return;
    }
    setBusy(true);
    try {
      const res = await onPublishContent(latestContentId);
      if (res.ok) {
        setSaveMessage({
          type: 'success',
          text: `Published version ${res.version ?? ''} — now live for Android students.`,
        });
      } else {
        setSaveMessage({
          type: 'error',
          text: res.error ?? 'Failed to publish version.',
        });
      }
    } finally {
      setBusy(false);
    }
  };

  const handleCreateMcqSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!onCreateMcq || !activeChapter) return;
    if (!mcqQuestion.trim() || !optA.trim() || !optB.trim() || !optC.trim() || !optD.trim()) {
      setSaveMessage({ type: 'error', text: 'Enter MCQ question text and all 4 options (A–D).' });
      return;
    }
    setBusy(true);
    try {
      await onCreateMcq({
        chapterId: activeChapter.id,
        subjectId: activeChapter.subjectId,
        classLevel: activeSubject?.classLevel ?? 9,
        question: mcqQuestion.trim(),
        options: [
          { id: 'A', text: optA.trim() },
          { id: 'B', text: optB.trim() },
          { id: 'C', text: optC.trim() },
          { id: 'D', text: optD.trim() },
        ],
        correctOptionId: correctOpt,
        explanation: mcqExplanation.trim() || 'Verified KPK curriculum answer.',
        difficulty: mcqDifficulty,
        topicTitle: mcqTopic.trim() || activeChapter.title,
        publishStatus: 'published',
        sourceReference: 'Admin MCQ Studio',
      });
      setMcqQuestion('');
      setOptA('');
      setOptB('');
      setOptC('');
      setOptD('');
      setMcqTopic('');
      setMcqExplanation('');
      setSaveMessage({ type: 'success', text: `MCQ added to "${activeChapter.title}" bank.` });
    } catch (err) {
      setSaveMessage({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to create MCQ.',
      });
    } finally {
      setBusy(false);
    }
  };

  const handleDeleteMcqItem = async (mcqId: string) => {
    if (!onDeleteMcq) return;
    setBusy(true);
    try {
      await onDeleteMcq(mcqId);
      setSaveMessage({ type: 'success', text: 'MCQ removed from chapter bank.' });
    } catch (err) {
      setSaveMessage({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete MCQ.',
      });
    } finally {
      setBusy(false);
    }
  };

  const publishStatus = activeContent?.publishStatus ?? 'draft';

  return (
    <div className="space-y-6">
      <StageMockNotice
        title="Structured Content Workflow & MCQ Question Bank"
        message={
          connectionMode === 'live_api'
            ? 'Supports Draft -> In Review -> Approved -> Published editorial workflow plus dedicated chapter MCQ bank CRUD in D1.'
            : 'Offline Preview mode: edits update local preview state cleanly.'
        }
      />

      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 rounded-xl border border-[#CBD8EE] bg-white p-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <label htmlFor="studio-chapter" className="block text-xs font-medium text-[#64748B] dark:text-[#8FA7D1]">
              Active Chapter Workspace
            </label>
            {activeContent && (
              <StatusIndicator
                label={`${activeContent.version} · ${activeContent.publishStatus.replace(/_/g, ' ')}`}
                tone={
                  activeContent.publishStatus === 'published'
                    ? 'success'
                    : activeContent.publishStatus === 'approved'
                      ? 'info'
                      : 'warning'
                }
              />
            )}
          </div>
          <select
            id="studio-chapter"
            value={selectedChapterId}
            onChange={(e) => onSelectChapter(e.target.value)}
            className="mt-1 w-full max-w-md h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm font-medium text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
          >
            {dataset.chapters.map((ch) => (
              <option key={ch.id} value={ch.id}>
                Ch {ch.chapterNumber}: {ch.title} ({ch.currentVersion})
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <div className="flex items-center gap-1 p-1 rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] dark:border-[#223B70] dark:bg-[#091530]">
            <button
              type="button"
              onClick={() => setStudioTab('blocks')}
              className={`px-3 py-1.5 rounded text-xs font-semibold whitespace-nowrap ${
                studioTab === 'blocks'
                  ? 'bg-[#1D4ED8] text-white dark:bg-[#2563EB]'
                  : 'text-[#475569] dark:text-[#94A8CE]'
              }`}
            >
              Chapter Blocks
            </button>
            <button
              type="button"
              onClick={() => setStudioTab('mcqs')}
              className={`px-3 py-1.5 rounded text-xs font-semibold whitespace-nowrap tabular-nums ${
                studioTab === 'mcqs'
                  ? 'bg-[#1D4ED8] text-white dark:bg-[#2563EB]'
                  : 'text-[#475569] dark:text-[#94A8CE]'
              }`}
            >
              MCQ Bank ({chapterMcqs.length})
            </button>
          </div>

          {studioTab === 'blocks' && (
            <>
              <button
                type="button"
                disabled={busy}
                onClick={() => void handleSave()}
                className="min-h-[40px] px-3.5 inline-flex items-center gap-1.5 rounded-lg border border-[#1D4ED8] text-[#1D4ED8] hover:bg-[#EFF6FF] dark:border-[#3B82F6] dark:text-[#60A5FA] dark:hover:bg-[#102145] text-xs font-semibold whitespace-nowrap disabled:opacity-60"
              >
                <Save className="h-4 w-4" />
                Save Draft
              </button>

              {onTransitionContentStatus && latestContentId && publishStatus === 'draft' && (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void handleWorkflowStep('in_review')}
                  className="min-h-[40px] px-3.5 inline-flex items-center gap-1.5 rounded-lg border border-[#D97706] text-[#B45309] hover:bg-[#FFFBEB] dark:border-[#F59E0B] dark:text-[#FBBF24] dark:hover:bg-[#1E293B] text-xs font-semibold whitespace-nowrap disabled:opacity-60"
                >
                  <Send className="h-4 w-4" />
                  Submit Review
                </button>
              )}

              {onTransitionContentStatus && latestContentId && publishStatus === 'in_review' && (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void handleWorkflowStep('approved')}
                  className="min-h-[40px] px-3.5 inline-flex items-center gap-1.5 rounded-lg border border-[#16A34A] text-[#15803D] hover:bg-[#F0FDF4] dark:border-[#22C55E] dark:text-[#4ADE80] dark:hover:bg-[#14532D]/30 text-xs font-semibold whitespace-nowrap disabled:opacity-60"
                >
                  <ShieldCheck className="h-4 w-4" />
                  Approve
                </button>
              )}

              <button
                type="button"
                disabled={busy || !latestContentId}
                onClick={() => void handlePublish()}
                className="min-h-[40px] px-3.5 inline-flex items-center gap-1.5 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] dark:hover:bg-[#3B82F6] text-white text-xs font-semibold whitespace-nowrap disabled:opacity-60"
              >
                <Rocket className="h-4 w-4" />
                Publish
              </button>
            </>
          )}
        </div>
      </div>

      {saveMessage && (
        <div
          role="status"
          className={`rounded-xl border px-4 py-3 text-xs font-medium flex items-center gap-2 ${
            saveMessage.type === 'success'
              ? 'border-[#16A34A]/40 bg-[#F0FDF4] text-[#166534] dark:border-[#22C55E]/40 dark:bg-[#14532D]/30 dark:text-[#BBF7D0]'
              : 'border-[#DC2626]/40 bg-[#FEF2F2] text-[#991B1B] dark:border-[#EF4444]/40 dark:bg-[#450A0A]/40 dark:text-[#FECACA]'
          }`}
        >
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{saveMessage.text}</span>
        </div>
      )}

      {studioTab === 'blocks' ? (
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
          <section className="xl:col-span-5 rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-5 dark:border-[#223B70] dark:bg-[#0D1B38]">
            <div>
              <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
                Add Structured Block
              </h2>
              <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                Supports all 11 Android learning workspace block types
              </p>
            </div>

            <form onSubmit={handleAddBlock} className="space-y-3">
              <div>
                <label htmlFor="block-type" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Block Type
                </label>
                <select
                  id="block-type"
                  value={newBlockType}
                  onChange={(e) => setNewBlockType(e.target.value as ContentBlockType)}
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                >
                  <option value="heading">Heading</option>
                  <option value="subheading">Subheading</option>
                  <option value="paragraph">Paragraph</option>
                  <option value="math_expression">Mathematical Expression</option>
                  <option value="definition">Important Definition</option>
                  <option value="exam_point">Highlighted Exam Point</option>
                  <option value="table">Structured Table</option>
                  <option value="list">List (semicolon-separated)</option>
                  <option value="example">Worked Example</option>
                  <option value="diagram">Diagram / Image Asset</option>
                  <option value="mcq">Inline MCQ</option>
                  <option value="solved_question">Solved Question</option>
                </select>
              </div>

              <div>
                <label htmlFor="block-primary" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Primary Content / Title / Question
                </label>
                <textarea
                  id="block-primary"
                  rows={2}
                  value={newHeadingOrText}
                  onChange={(e) => setNewHeadingOrText(e.target.value)}
                  placeholder="Enter block text, formula, term, or question..."
                  className="mt-1 w-full p-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>

              <div>
                <label htmlFor="block-secondary" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Secondary Detail (Plain Notation / Definition / Answer Step)
                </label>
                <input
                  id="block-secondary"
                  type="text"
                  value={newSecondaryText}
                  onChange={(e) => setNewSecondaryText(e.target.value)}
                  placeholder="Optional depending on block type"
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>

              <button
                type="submit"
                className="w-full min-h-[40px] inline-flex items-center justify-center gap-2 rounded-lg border border-[#1D4ED8] text-[#1D4ED8] hover:bg-[#1D4ED8]/10 dark:border-[#3B82F6] dark:text-[#60A5FA] dark:hover:bg-[#3B82F6]/15 text-xs font-semibold"
              >
                <Plus className="h-4 w-4" />
                Append Block to Chapter
              </button>
            </form>

            <div className="pt-4 border-t border-[#E2E8F0] dark:border-[#1B3263]">
              <div className="text-xs font-semibold text-[#0F172A] dark:text-white mb-2">
                Block Sequence ({blocks.length})
              </div>
              <ul className="space-y-1.5 max-h-64 overflow-y-auto">
                {blocks.map((b, idx) => (
                  <li
                    key={b.id}
                    className="flex items-center justify-between gap-2 rounded-lg border border-[#E2E8F0] px-3 py-2 text-xs dark:border-[#1E3566]"
                  >
                    <span className="truncate text-[#334155] dark:text-[#C5D5F2]">
                      <span className="font-mono text-[#1D4ED8] dark:text-[#60A5FA] mr-2 tabular-nums">
                        {idx + 1}. {b.type}
                      </span>
                    </span>
                    <button
                      type="button"
                      aria-label={`Remove block ${idx + 1} (${b.type})`}
                      onClick={() => setBlocks((prev) => prev.filter((item) => item.id !== b.id))}
                      className="p-1 text-[#64748B] hover:text-[#DC2626] dark:text-[#8FA7D1] dark:hover:text-[#F87171]"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          </section>

          <section className="xl:col-span-7 rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
            <div className="flex items-center justify-between border-b border-[#E2E8F0] pb-3 dark:border-[#1B3263]">
              <div>
                <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
                  Student Workspace Live Preview
                </h2>
                <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                  Rendering of reusable chapter blocks matching Android layout
                </p>
              </div>
              <span className="font-mono text-xs text-[#64748B] dark:text-[#7A93C2] tabular-nums">
                {blocks.length} blocks
              </span>
            </div>

            <ContentBlockPreview blocks={blocks} />
          </section>
        </div>
      ) : (
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
          <section className="xl:col-span-5 rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
            <div>
              <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
                Add MCQ to Chapter Bank
              </h2>
              <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                Directly feeds Practice Tests, Mock Exams, and Mistake Master
              </p>
            </div>

            <form onSubmit={handleCreateMcqSubmit} className="space-y-3">
              <div>
                <label htmlFor="mcq-question" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Question Prompt
                </label>
                <textarea
                  id="mcq-question"
                  rows={2}
                  value={mcqQuestion}
                  onChange={(e) => setMcqQuestion(e.target.value)}
                  placeholder="Enter KPK Board multiple-choice question..."
                  className="mt-1 w-full p-2.5 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-2.5">
                <input
                  type="text"
                  aria-label="Option A"
                  value={optA}
                  onChange={(e) => setOptA(e.target.value)}
                  placeholder="Option A"
                  className="h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
                <input
                  type="text"
                  aria-label="Option B"
                  value={optB}
                  onChange={(e) => setOptB(e.target.value)}
                  placeholder="Option B"
                  className="h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
                <input
                  type="text"
                  aria-label="Option C"
                  value={optC}
                  onChange={(e) => setOptC(e.target.value)}
                  placeholder="Option C"
                  className="h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
                <input
                  type="text"
                  aria-label="Option D"
                  value={optD}
                  onChange={(e) => setOptD(e.target.value)}
                  placeholder="Option D"
                  className="h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label htmlFor="mcq-correct" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                    Correct Option
                  </label>
                  <select
                    id="mcq-correct"
                    value={correctOpt}
                    onChange={(e) => setCorrectOpt(e.target.value as 'A' | 'B' | 'C' | 'D')}
                    className="mt-1 w-full h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                  >
                    <option value="A">Option A</option>
                    <option value="B">Option B</option>
                    <option value="C">Option C</option>
                    <option value="D">Option D</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="mcq-diff" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                    Difficulty
                  </label>
                  <select
                    id="mcq-diff"
                    value={mcqDifficulty}
                    onChange={(e) => setMcqDifficulty(e.target.value as 'easy' | 'medium' | 'hard')}
                    className="mt-1 w-full h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                  >
                    <option value="easy">Easy</option>
                    <option value="medium">Medium</option>
                    <option value="hard">Hard</option>
                  </select>
                </div>
              </div>

              <input
                type="text"
                aria-label="Topic title"
                value={mcqTopic}
                onChange={(e) => setMcqTopic(e.target.value)}
                placeholder="Topic tag (e.g., Vernier Callipers)"
                className="w-full h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />

              <input
                type="text"
                aria-label="Explanation"
                value={mcqExplanation}
                onChange={(e) => setMcqExplanation(e.target.value)}
                placeholder="Step-by-step explanation for student review"
                className="w-full h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />

              <button
                type="submit"
                disabled={busy || !onCreateMcq}
                className="w-full min-h-[40px] inline-flex items-center justify-center gap-2 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] text-white text-xs font-semibold disabled:opacity-60"
              >
                <Plus className="h-4 w-4" />
                Save MCQ to Bank
              </button>
            </form>
          </section>

          <section className="xl:col-span-7 rounded-xl border border-[#CBD8EE] bg-white p-5 space-y-4 dark:border-[#223B70] dark:bg-[#0D1B38]">
            <div className="flex items-center justify-between border-b border-[#E2E8F0] pb-3 dark:border-[#1B3263]">
              <div>
                <h2 className="font-display text-base font-semibold text-[#0F172A] dark:text-white">
                  Published &amp; Draft Chapter MCQs
                </h2>
                <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                  {activeChapter ? `Ch ${activeChapter.chapterNumber}: ${activeChapter.title}` : ''}
                </p>
              </div>
              <span className="font-mono text-xs text-[#64748B] dark:text-[#7A93C2] tabular-nums">
                {chapterMcqs.length} items
              </span>
            </div>

            {chapterMcqs.length === 0 ? (
              <div className="py-8 text-center text-xs text-[#64748B] dark:text-[#8FA7D1]">
                No standalone MCQs recorded in this chapter yet. Add one using the form on the left.
              </div>
            ) : (
              <ul className="space-y-3">
                {chapterMcqs.map((m) => (
                  <li
                    key={m.id}
                    className="rounded-lg border border-[#E2E8F0] p-4 dark:border-[#1E3566] space-y-2"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="text-[11px] text-[#64748B] dark:text-[#8FA7D1]">
                          {m.topicTitle} · {m.difficulty} · Correct: Option {m.correctOptionId}
                        </div>
                        <div className="mt-1 text-sm font-medium text-[#0F172A] dark:text-white">
                          {m.question}
                        </div>
                      </div>
                      {onDeleteMcq && (
                        <button
                          type="button"
                          disabled={busy}
                          aria-label={`Delete MCQ ${m.id}`}
                          onClick={() => void handleDeleteMcqItem(m.id)}
                          className="p-1 text-[#64748B] hover:text-[#DC2626] dark:text-[#8FA7D1] dark:hover:text-[#F87171] disabled:opacity-50"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                    <div className="grid grid-cols-2 gap-1.5 text-xs text-[#475569] dark:text-[#94A8CE]">
                      {m.options.map((opt) => (
                        <div
                          key={opt.id}
                          className={
                            opt.id === m.correctOptionId
                              ? 'font-semibold text-[#15803D] dark:text-[#4ADE80]'
                              : ''
                          }
                        >
                          {opt.id}. {opt.text}
                        </div>
                      ))}
                    </div>
                    {m.explanation && (
                      <div className="text-xs text-[#64748B] dark:text-[#8FA7D1] pt-1 border-t border-[#E2E8F0] dark:border-[#1B3263]">
                        {m.explanation}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
