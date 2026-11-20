import React, { useMemo, useState } from 'react';
import { Plus, Search, Trash2 } from 'lucide-react';
import type { ApiChapter, ApiSubject, BoardCode, ClassLevel, StudyGroupCode } from '../../shared-contracts/types';
import { STUDY_GROUPS_CATALOG } from '../../shared-contracts/rules';
import type { AdminConnectionMode, AdminDataset, ViewLoadState } from '../types/admin';
import { StageMockNotice, StateGate, StatusIndicator } from '../components/StateBanner';

interface AcademicProps {
  mode: 'academic' | 'classes' | 'subjects' | 'chapters';
  dataset: AdminDataset;
  connectionMode: AdminConnectionMode;
  onCreateSubject: (input: Omit<ApiSubject, 'id' | 'updatedAtIso'>) => Promise<void> | void;
  onDeleteSubject: (subjectId: string) => Promise<void> | void;
  onCreateChapter: (input: Omit<ApiChapter, 'id' | 'updatedAtIso'>) => Promise<void> | void;
  onDeleteChapter: (chapterId: string) => Promise<void> | void;
  onOpenStudioForChapter: (chapterId: string) => void;
}

export function AcademicManagementPages({
  mode,
  dataset,
  connectionMode,
  onCreateSubject,
  onDeleteSubject,
  onCreateChapter,
  onDeleteChapter,
  onOpenStudioForChapter,
}: AcademicProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedClass, setSelectedClass] = useState<ClassLevel | 'all'>('all');
  const [selectedBoard, setSelectedBoard] = useState<BoardCode | 'all'>('all');
  const [busy, setBusy] = useState(false);

  const [showSubjectModal, setShowSubjectModal] = useState(false);
  const [subName, setSubName] = useState('');
  const [subCode, setSubCode] = useState('');
  const [subClass, setSubClass] = useState<ClassLevel>(11);
  const [subBoard, setSubBoard] = useState<BoardCode>('KPK');
  const [subChaptersCount, setSubChaptersCount] = useState(10);
  const [subStudyGroups, setSubStudyGroups] = useState<StudyGroupCode[]>(['PRE_MEDICAL', 'PRE_ENGINEERING']);
  const [subError, setSubError] = useState('');

  const [showChapterModal, setShowChapterModal] = useState(false);
  const [chSubjectId, setChSubjectId] = useState(dataset.subjects[0]?.id ?? '');
  const [chNumber, setChNumber] = useState(1);
  const [chTitle, setChTitle] = useState('');
  const [chSubtitle, setChSubtitle] = useState('');
  const [chDifficulty, setChDifficulty] = useState<'easy' | 'medium' | 'hard'>('medium');
  const [chSourceRef, setChSourceRef] = useState('KPK Textbook Board Peshawar');
  const [chTitleUrdu, setChTitleUrdu] = useState('');
  const [chBoardWeightage, setChBoardWeightage] = useState(8);
  const [chPairingHint, setChPairingHint] = useState('');
  const [chNightBefore, setChNightBefore] = useState('');
  const [chError, setChError] = useState('');

  const filteredSubjects = useMemo(() => {
    return dataset.subjects.filter((sub) => {
      const matchesClass = selectedClass === 'all' || sub.classLevel === selectedClass;
      const matchesBoard = selectedBoard === 'all' || sub.boardCode === selectedBoard;
      const matchesSearch =
        !searchQuery.trim() ||
        sub.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        sub.code.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesClass && matchesBoard && matchesSearch;
    });
  }, [dataset.subjects, selectedClass, selectedBoard, searchQuery]);

  const filteredChapters = useMemo(() => {
    return dataset.chapters.filter((ch) => {
      const parentSub = dataset.subjects.find((s) => s.id === ch.subjectId);
      const matchesClass =
        selectedClass === 'all' || parentSub?.classLevel === selectedClass;
      const matchesSearch =
        !searchQuery.trim() ||
        ch.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (parentSub?.name.toLowerCase().includes(searchQuery.toLowerCase()) ?? false);
      return matchesClass && matchesSearch;
    });
  }, [dataset.chapters, dataset.subjects, selectedClass, searchQuery]);

  const handleSubjectSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subName.trim() || !subCode.trim()) {
      setSubError('Subject name and code are required.');
      return;
    }
    setBusy(true);
    setSubError('');
    try {
      await onCreateSubject({
        classId: `cls_${subBoard.toLowerCase()}_${subClass}`,
        classLevel: subClass,
        boardCode: subBoard,
        code: subCode.trim().toUpperCase(),
        name: subName.trim(),
        iconKey: 'physics',
        accentHex: '#3B82F6',
        totalChapters: subChaptersCount,
        publishedChapters: 0,
        isOfflinePackAvailable: false,
        coverageStatus: 'structure_ready',
        studyGroups: subStudyGroups,
      });
      setSubName('');
      setSubCode('');
      setShowSubjectModal(false);
    } catch (err) {
      setSubError(err instanceof Error ? err.message : 'Failed to create subject.');
    } finally {
      setBusy(false);
    }
  };

  const handleChapterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const targetSubId = chSubjectId || dataset.subjects[0]?.id || '';
    if (!chTitle.trim() || !targetSubId) {
      setChError('Subject and chapter title are required.');
      return;
    }
    setBusy(true);
    setChError('');
    try {
      await onCreateChapter({
        subjectId: targetSubId,
        chapterNumber: chNumber,
        title: chTitle.trim(),
        subtitle: chSubtitle.trim() || 'Draft curriculum unit',
        topics: [],
        currentVersion: 'v0.1-draft',
        publishStatus: 'draft',
        hasDetailedNotes: false,
        hasRevisionNotes: false,
        mcqCount: 0,
        solvedQuestionsCount: 0,
        isLockedByDefault: false,
        estimatedSizeKb: 120,
        curriculumVersion: 'KPK-2026',
        difficultyTag: chDifficulty,
        sourceReference: chSourceRef.trim() || 'KPK Textbook Board Peshawar',
        titleUrdu: chTitleUrdu.trim() || undefined,
        boardWeightageMarks: chBoardWeightage > 0 ? chBoardWeightage : undefined,
        pairingHint: chPairingHint.trim() || undefined,
        nightBeforeSummary: chNightBefore.trim() || undefined,
      });
      setChTitle('');
      setChSubtitle('');
      setChTitleUrdu('');
      setChPairingHint('');
      setChNightBefore('');
      setShowChapterModal(false);
    } catch (err) {
      setChError(err instanceof Error ? err.message : 'Failed to create chapter.');
    } finally {
      setBusy(false);
    }
  };

  const effectiveState: ViewLoadState =
    (mode === 'subjects' && filteredSubjects.length === 0) ||
    (mode === 'chapters' && filteredChapters.length === 0)
      ? 'empty'
      : 'ready';

  return (
    <div className="space-y-6">
      <StageMockNotice
        title="Academic Schema & Multi-Board Architecture"
        message={
          connectionMode === 'live_api'
            ? 'Subjects and chapters created or deleted here persist directly in Cloudflare D1 (imtihan-pro-db).'
            : 'KPK is active as the primary initial curriculum focus across Classes 9–12. Running in Offline Preview mode.'
        }
      />

      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="relative">
            <Search className="h-4 w-4 absolute left-3 top-1/2 -translate-y-1/2 text-[#64748B] dark:text-[#7A93C2]" />
            <input
              type="search"
              aria-label="Search curriculum records"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Filter by title or code..."
              className="h-10 pl-9 pr-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] placeholder:text-[#64748B] dark:border-[#24407A] dark:bg-[#0D1B38] dark:text-white dark:placeholder:text-[#7A93C2] focus:outline-2 focus:outline-[#3B82F6]"
            />
          </div>

          {(mode === 'subjects' || mode === 'chapters') && (
            <select
              aria-label="Filter by class"
              value={selectedClass}
              onChange={(e) =>
                setSelectedClass(
                  e.target.value === 'all' ? 'all' : (Number(e.target.value) as ClassLevel)
                )
              }
              className="h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#0D1B38] dark:text-white"
            >
              <option value="all">All Classes (9–12)</option>
              <option value="9">Class 9</option>
              <option value="10">Class 10</option>
              <option value="11">Class 11</option>
              <option value="12">Class 12</option>
            </select>
          )}

          {mode === 'subjects' && (
            <select
              aria-label="Filter by board"
              value={selectedBoard}
              onChange={(e) => setSelectedBoard(e.target.value as BoardCode | 'all')}
              className="h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#0D1B38] dark:text-white"
            >
              <option value="all">All Boards</option>
              <option value="KPK">KPK Board</option>
              <option value="FEDERAL">Federal Board</option>
              <option value="PUNJAB">Punjab Board</option>
            </select>
          )}
        </div>

        <div className="flex items-center gap-2">
          {mode === 'subjects' && (
            <button
              type="button"
              onClick={() => setShowSubjectModal(true)}
              className="min-h-[40px] px-4 inline-flex items-center gap-2 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] dark:hover:bg-[#3B82F6] text-white text-xs font-semibold whitespace-nowrap"
            >
              <Plus className="h-4 w-4" />
              Add Subject
            </button>
          )}

          {mode === 'chapters' && (
            <button
              type="button"
              onClick={() => setShowChapterModal(true)}
              className="min-h-[40px] px-4 inline-flex items-center gap-2 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] dark:hover:bg-[#3B82F6] text-white text-xs font-semibold whitespace-nowrap"
            >
              <Plus className="h-4 w-4" />
              Add Chapter
            </button>
          )}
        </div>
      </div>

      <StateGate
        state={effectiveState}
        emptyTitle="No matching curriculum records"
        emptyDescription="Clear active filters or create a new subject/chapter."
        emptyActionLabel="Reset Filters"
        onEmptyAction={() => {
          setSearchQuery('');
          setSelectedClass('all');
          setSelectedBoard('all');
        }}
      >
        {mode === 'academic' && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {dataset.boards.map((board) => (
              <div
                key={board.id}
                className="rounded-xl border border-[#CBD8EE] bg-white p-5 dark:border-[#223B70] dark:bg-[#0D1B38]"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-mono text-xs font-semibold text-[#1D4ED8] dark:text-[#60A5FA]">
                    {board.code}
                  </span>
                  {board.isActive ? (
                    <StatusIndicator label="Active Focus" tone="success" />
                  ) : (
                    <StatusIndicator label="Multi-Board Ready" tone="info" />
                  )}
                </div>
                <h2 className="mt-2 font-display text-base font-bold text-[#0F172A] dark:text-white">
                  {board.name}
                </h2>
                <p className="mt-1 text-xs text-[#475569] dark:text-[#94A8CE]">
                  {board.region}
                </p>
                <div className="mt-4 pt-3 border-t border-[#E2E8F0] dark:border-[#1B3263] text-xs text-[#64748B] dark:text-[#8FA7D1]">
                  Supported Classes: {board.supportedClasses.join(', ')}
                </div>
              </div>
            ))}
          </div>
        )}

        {mode === 'classes' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {dataset.classes.map((cls) => (
              <div
                key={cls.id}
                className="rounded-xl border border-[#CBD8EE] bg-white p-5 dark:border-[#223B70] dark:bg-[#0D1B38]"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-display text-lg font-bold text-[#0F172A] dark:text-white">
                    {cls.label}
                  </span>
                  {cls.isPublicAvailable ? (
                    <StatusIndicator label="Live v1.0 Launch" tone="success" />
                  ) : cls.coverageStatus === 'active_seed' ? (
                    <StatusIndicator label="Active Seed" tone="info" />
                  ) : (
                    <StatusIndicator label="Prepared (Hidden in v1.0)" tone="neutral" />
                  )}
                </div>
                <div className="mt-1 text-xs text-[#64748B] dark:text-[#8FA7D1]">
                  {cls.boardCode} · Academic Year {cls.academicYear}
                </div>
                <p className="mt-3 text-xs leading-relaxed text-[#334155] dark:text-[#C5D5F2]">
                  {cls.notes}
                </p>
                <div className="mt-4 grid grid-cols-2 gap-3 pt-3 border-t border-[#E2E8F0] dark:border-[#1B3263] text-xs">
                  <div>
                    <div className="text-[#64748B] dark:text-[#7A93C2]">Mapped Subjects</div>
                    <div className="font-semibold tabular-nums text-[#0F172A] dark:text-white">
                      {cls.subjectCount}
                    </div>
                  </div>
                  <div>
                    <div className="text-[#64748B] dark:text-[#7A93C2]">Published Chapters</div>
                    <div className="font-semibold tabular-nums text-[#0F172A] dark:text-white">
                      {cls.publishedChaptersCount}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {mode === 'subjects' && (
          <div className="rounded-xl border border-[#CBD8EE] bg-white overflow-x-auto dark:border-[#223B70] dark:bg-[#0D1B38]">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] text-xs font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
                <tr>
                  <th className="px-5 py-3">Code</th>
                  <th className="px-5 py-3">Subject</th>
                  <th className="px-5 py-3">Class / Board</th>
                  <th className="px-5 py-3 text-right">Chapters (Published / Total)</th>
                  <th className="px-5 py-3">Coverage</th>
                  <th className="px-5 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
                {filteredSubjects.map((sub) => (
                  <tr
                    key={sub.id}
                    className="hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60"
                  >
                    <td className="px-5 py-3.5 font-mono text-xs text-[#1D4ED8] dark:text-[#60A5FA]">
                      {sub.code}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="font-medium text-[#0F172A] dark:text-white">{sub.name}</div>
                      {sub.studyGroups && sub.studyGroups.length > 0 && (
                        <div className="text-[11px] text-[#64748B] dark:text-[#8FA7D1]">
                          {sub.studyGroups.join(' · ')}
                        </div>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-xs text-[#475569] dark:text-[#94A8CE]">
                      Class {sub.classLevel} · {sub.boardCode}
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                      {sub.publishedChapters} / {sub.totalChapters}
                    </td>
                    <td className="px-5 py-3.5">
                      {sub.coverageStatus === 'active_seed' ? (
                        <StatusIndicator label="Active Seed" tone="success" />
                      ) : (
                        <StatusIndicator label="Structure Ready" tone="info" />
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <button
                        type="button"
                        aria-label={`Delete subject ${sub.name}`}
                        onClick={() => void onDeleteSubject(sub.id)}
                        className="p-1.5 rounded text-[#64748B] hover:text-[#DC2626] dark:text-[#8FA7D1] dark:hover:text-[#F87171]"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {mode === 'chapters' && (
          <div className="rounded-xl border border-[#CBD8EE] bg-white overflow-x-auto dark:border-[#223B70] dark:bg-[#0D1B38]">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] text-xs font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
                <tr>
                  <th className="px-5 py-3">Chapter</th>
                  <th className="px-5 py-3">Subject</th>
                  <th className="px-5 py-3">Version</th>
                  <th className="px-5 py-3 text-right">MCQs</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
                {filteredChapters.map((ch) => {
                  const sub = dataset.subjects.find((s) => s.id === ch.subjectId);
                  return (
                    <tr
                      key={ch.id}
                      className="hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60"
                    >
                      <td className="px-5 py-3.5">
                        <div className="font-medium text-[#0F172A] dark:text-white">
                          {ch.chapterNumber}. {ch.title}
                          {ch.boardWeightageMarks ? (
                            <span className="ml-2 font-mono text-[11px] text-[#1D4ED8] dark:text-[#60A5FA]">
                              · {ch.boardWeightageMarks} Marks BISE
                            </span>
                          ) : null}
                        </div>
                        <div className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
                          {ch.subtitle}
                          {ch.titleUrdu ? <span className="ml-2">· {ch.titleUrdu}</span> : null}
                        </div>
                      </td>
                      <td className="px-5 py-3.5 text-xs text-[#475569] dark:text-[#94A8CE]">
                        {sub ? `${sub.name} (Class ${sub.classLevel})` : ch.subjectId}
                      </td>
                      <td className="px-5 py-3.5 font-mono text-xs text-[#1E293B] dark:text-[#DCE8FC]">
                        {ch.currentVersion}
                      </td>
                      <td className="px-5 py-3.5 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                        {ch.mcqCount}
                      </td>
                      <td className="px-5 py-3.5">
                        {ch.publishStatus === 'published' ? (
                          <StatusIndicator label="Published" tone="success" />
                        ) : ch.publishStatus === 'in_review' ? (
                          <StatusIndicator label="In Review" tone="warning" />
                        ) : (
                          <StatusIndicator label="Draft" tone="neutral" />
                        )}
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        <div className="inline-flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => onOpenStudioForChapter(ch.id)}
                            className="text-xs font-semibold text-[#1D4ED8] hover:underline dark:text-[#60A5FA] whitespace-nowrap"
                          >
                            Edit in Studio
                          </button>
                          <button
                            type="button"
                            aria-label={`Delete chapter ${ch.title}`}
                            onClick={() => void onDeleteChapter(ch.id)}
                            className="p-1 rounded text-[#64748B] hover:text-[#DC2626] dark:text-[#8FA7D1] dark:hover:text-[#F87171]"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </StateGate>

      {showSubjectModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="add-subject-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
        >
          <form
            onSubmit={handleSubjectSubmit}
            className="w-full max-w-md rounded-xl border border-[#CBD8EE] bg-white p-6 space-y-4 dark:border-[#24407A] dark:bg-[#0D1B38]"
          >
            <h2 id="add-subject-title" className="font-display text-lg font-bold text-[#0F172A] dark:text-white">
              Create Subject
            </h2>
            {subError && (
              <div className="text-xs font-medium text-[#DC2626] dark:text-[#F87171]">
                {subError}
              </div>
            )}
            <div>
              <label htmlFor="sub-name" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Subject Name
              </label>
              <input
                id="sub-name"
                type="text"
                value={subName}
                onChange={(e) => setSubName(e.target.value)}
                placeholder="e.g., Computer Science"
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label htmlFor="sub-code" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Code
                </label>
                <input
                  id="sub-code"
                  type="text"
                  value={subCode}
                  onChange={(e) => setSubCode(e.target.value)}
                  placeholder="CS-10"
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
              <div>
                <label htmlFor="sub-class" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Class
                </label>
                <select
                  id="sub-class"
                  value={subClass}
                  onChange={(e) => setSubClass(Number(e.target.value) as ClassLevel)}
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                >
                  <option value={9}>Class 9</option>
                  <option value={10}>Class 10</option>
                  <option value={11}>Class 11</option>
                  <option value={12}>Class 12</option>
                </select>
              </div>
              <div>
                <label htmlFor="sub-board" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Board
                </label>
                <select
                  id="sub-board"
                  value={subBoard}
                  onChange={(e) => setSubBoard(e.target.value as BoardCode)}
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                >
                  <option value="KPK">KPK</option>
                  <option value="FEDERAL">FEDERAL</option>
                  <option value="PUNJAB">PUNJAB</option>
                </select>
              </div>
            </div>
            <div>
              <label htmlFor="sub-ch-count" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Planned Chapters
              </label>
              <input
                id="sub-ch-count"
                type="number"
                min={1}
                max={30}
                value={subChaptersCount}
                onChange={(e) => setSubChaptersCount(Number(e.target.value))}
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />
            </div>
            <div>
              <div className="text-xs font-medium text-[#334155] dark:text-[#B5C6E6] mb-1.5">
                Applicable Study Groups
              </div>
              <div className="grid grid-cols-2 gap-1.5 max-h-28 overflow-y-auto text-xs text-[#334155] dark:text-[#C5D5F2]">
                {STUDY_GROUPS_CATALOG.map((grp) => {
                  const checked = subStudyGroups.includes(grp.code);
                  return (
                    <label key={grp.code} className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={(e) =>
                          setSubStudyGroups((prev) =>
                            e.target.checked
                              ? Array.from(new Set([...prev, grp.code]))
                              : prev.filter((c) => c !== grp.code)
                          )
                        }
                      />
                      <span className="truncate">{grp.label}</span>
                    </label>
                  );
                })}
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowSubjectModal(false)}
                className="min-h-[40px] px-4 rounded-lg border border-[#CBD8EE] text-xs font-semibold text-[#334155] dark:border-[#24407A] dark:text-[#DCE8FC]"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={busy}
                className="min-h-[40px] px-4 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] text-white text-xs font-semibold disabled:opacity-60"
              >
                {busy ? 'Saving...' : 'Create Subject'}
              </button>
            </div>
          </form>
        </div>
      )}

      {showChapterModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="add-chapter-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
        >
          <form
            onSubmit={handleChapterSubmit}
            className="w-full max-w-md rounded-xl border border-[#CBD8EE] bg-white p-6 space-y-4 dark:border-[#24407A] dark:bg-[#0D1B38]"
          >
            <h2 id="add-chapter-title" className="font-display text-lg font-bold text-[#0F172A] dark:text-white">
              Create Chapter Draft
            </h2>
            {chError && (
              <div className="text-xs font-medium text-[#DC2626] dark:text-[#F87171]">
                {chError}
              </div>
            )}
            <div>
              <label htmlFor="ch-subject" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Parent Subject
              </label>
              <select
                id="ch-subject"
                value={chSubjectId || dataset.subjects[0]?.id || ''}
                onChange={(e) => setChSubjectId(e.target.value)}
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              >
                {dataset.subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} — Class {s.classLevel} ({s.boardCode})
                  </option>
                ))}
              </select>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label htmlFor="ch-num" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Number
                </label>
                <input
                  id="ch-num"
                  type="number"
                  min={1}
                  value={chNumber}
                  onChange={(e) => setChNumber(Number(e.target.value))}
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
              <div className="col-span-2">
                <label htmlFor="ch-title" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Chapter Title
                </label>
                <input
                  id="ch-title"
                  type="text"
                  value={chTitle}
                  onChange={(e) => setChTitle(e.target.value)}
                  placeholder="e.g., Electrostatics"
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
            </div>
            <div>
              <label htmlFor="ch-sub" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Short Summary
              </label>
              <input
                id="ch-sub"
                type="text"
                value={chSubtitle}
                onChange={(e) => setChSubtitle(e.target.value)}
                placeholder="Coulomb’s law, electric field, and capacitors"
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="ch-diff" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Difficulty Tag
                </label>
                <select
                  id="ch-diff"
                  value={chDifficulty}
                  onChange={(e) => setChDifficulty(e.target.value as 'easy' | 'medium' | 'hard')}
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                >
                  <option value="easy">Easy</option>
                  <option value="medium">Medium</option>
                  <option value="hard">Hard</option>
                </select>
              </div>
              <div>
                <label htmlFor="ch-src" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Source Reference
                </label>
                <input
                  id="ch-src"
                  type="text"
                  value={chSourceRef}
                  onChange={(e) => setChSourceRef(e.target.value)}
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="col-span-2">
                <label htmlFor="ch-urdu" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Urdu Title (Optional)
                </label>
                <input
                  id="ch-urdu"
                  type="text"
                  dir="rtl"
                  value={chTitleUrdu}
                  onChange={(e) => setChTitleUrdu(e.target.value)}
                  placeholder="اردو عنوان (اختیاری)"
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
              <div>
                <label htmlFor="ch-marks" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  BISE Marks
                </label>
                <input
                  id="ch-marks"
                  type="number"
                  min={0}
                  max={30}
                  value={chBoardWeightage}
                  onChange={(e) => setChBoardWeightage(Number(e.target.value))}
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
            </div>
            <div>
              <label htmlFor="ch-pairing" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Pairing Scheme Hint (Optional)
              </label>
              <input
                id="ch-pairing"
                type="text"
                value={chPairingHint}
                onChange={(e) => setChPairingHint(e.target.value)}
                placeholder="e.g., 2 MCQs + 1 Short + Paired Long Numerical"
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />
            </div>
            <div>
              <label htmlFor="ch-night" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Night-Before Exam Summary (Optional)
              </label>
              <textarea
                id="ch-night"
                rows={2}
                value={chNightBefore}
                onChange={(e) => setChNightBefore(e.target.value)}
                placeholder="60-second revision bullets for the night before BISE exam..."
                className="mt-1 w-full p-2.5 rounded-lg border border-[#CBD8EE] bg-white text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowChapterModal(false)}
                className="min-h-[40px] px-4 rounded-lg border border-[#CBD8EE] text-xs font-semibold text-[#334155] dark:border-[#24407A] dark:text-[#DCE8FC]"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={busy}
                className="min-h-[40px] px-4 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] text-white text-xs font-semibold disabled:opacity-60"
              >
                {busy ? 'Saving...' : 'Create Chapter'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
