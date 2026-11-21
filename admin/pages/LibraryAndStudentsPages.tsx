import React, { useMemo, useState } from 'react';
import { FilePlus2, Search, Trash2, Upload } from 'lucide-react';
import type { ApiPastPaper, ApiPdfMetadata, ClassLevel } from '../../shared-contracts/types';
import type { AdminConnectionMode, AdminDataset } from '../types/admin';
import { StageMockNotice, StatusIndicator } from '../components/StateBanner';

export interface AdminPdfSubmitPayload extends Omit<ApiPastPaper, 'id' | 'pdfMetadataId' | 'downloadCount'> {
  category: ApiPdfMetadata['category'];
}

interface LibraryAndStudentsProps {
  mode: 'pdf-library' | 'students';
  dataset: AdminDataset;
  connectionMode: AdminConnectionMode;
  onAddPdfPaper: (
    paper: AdminPdfSubmitPayload,
    title: string,
    file?: File | null
  ) => Promise<void> | void;
  onDeletePdf?: (pdfId: string) => Promise<void> | void;
}

const PDF_CATEGORY_LABELS: Record<ApiPdfMetadata['category'], string> = {
  past_paper: 'Past Paper',
  textbook: 'Textbook',
  notes_pack: 'Notes Pack',
  syllabus: 'Syllabus',
};

export function LibraryAndStudentsPages({
  mode,
  dataset,
  connectionMode,
  onAddPdfPaper,
  onDeletePdf,
}: LibraryAndStudentsProps) {
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<'all' | ApiPdfMetadata['category']>('all');
  const [studentFilter, setStudentFilter] = useState<'all' | 'guest' | 'google_synced'>('all');

  const [showAddPdf, setShowAddPdf] = useState(false);
  const [pdfCategory, setPdfCategory] = useState<ApiPdfMetadata['category']>('past_paper');
  const [pdfTitle, setPdfTitle] = useState('');
  const [pdfSubjectId, setPdfSubjectId] = useState(dataset.subjects[0]?.id ?? '');
  const [pdfYear, setPdfYear] = useState(2025);
  const [pdfSolved, setPdfSolved] = useState(false);
  const [pdfPageCount, setPdfPageCount] = useState(10);
  const [pdfSizeMb, setPdfSizeMb] = useState(2.1);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [libraryError, setLibraryError] = useState<string | null>(null);
  const [deletingPdfId, setDeletingPdfId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const filteredPdfs = useMemo(() => {
    return dataset.pdfs.filter((pdf) => {
      const matchesCat = categoryFilter === 'all' || pdf.category === categoryFilter;
      if (!matchesCat) return false;
      if (!search.trim()) return true;
      const q = search.toLowerCase();
      const sub = dataset.subjects.find((s) => s.id === pdf.subjectId);
      const pp = dataset.pastPapers.find((p) => p.pdfMetadataId === pdf.id);
      return (
        pdf.title.toLowerCase().includes(q) ||
        pdf.objectKey.toLowerCase().includes(q) ||
        (sub?.name.toLowerCase().includes(q) ?? false) ||
        (pp ? String(pp.year).includes(q) : false)
      );
    });
  }, [dataset.pdfs, dataset.subjects, dataset.pastPapers, categoryFilter, search]);

  const filteredStudents = useMemo(() => {
    return dataset.students.filter((st) => {
      const matchesType = studentFilter === 'all' || st.accountType === studentFilter;
      const matchesSearch =
        !search.trim() ||
        st.displayName.toLowerCase().includes(search.toLowerCase()) ||
        String(st.classLevel).includes(search);
      return matchesType && matchesSearch;
    });
  }, [dataset.students, studentFilter, search]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0] ?? null;
    setSelectedFile(f);
    if (f) {
      setPdfSizeMb(Number((f.size / (1024 * 1024)).toFixed(2)) || 0.1);
      if (!pdfTitle.trim()) {
        setPdfTitle(f.name.replace(/\.pdf$/i, ''));
      }
    }
  };

  const handleAddPdf = async (e: React.FormEvent) => {
    e.preventDefault();
    setUploadError(null);
    const targetSubId = pdfSubjectId || dataset.subjects[0]?.id || '';
    const sub = dataset.subjects.find((s) => s.id === targetSubId);
    if (!pdfTitle.trim() || !sub) {
      setUploadError('Title and subject are required.');
      return;
    }
    if (connectionMode === 'live_api' && !selectedFile) {
      setUploadError('Select a real .pdf file to upload to Backblaze B2 (imtihan-pro-assets).');
      return;
    }

    setBusy(true);
    try {
      await onAddPdfPaper(
        {
          category: pdfCategory,
          boardCode: sub.boardCode,
          classLevel: sub.classLevel as ClassLevel,
          subjectId: sub.id,
          subjectName: sub.name,
          year: pdfYear,
          session: 'Annual',
          isSolved: pdfCategory === 'past_paper' ? pdfSolved : false,
          fileSizeMb: pdfSizeMb,
          pageCount: pdfPageCount,
        },
        pdfTitle.trim(),
        selectedFile
      );
      setPdfTitle('');
      setSelectedFile(null);
      setShowAddPdf(false);
    } catch (err) {
      setUploadError(err instanceof Error ? err.message : 'PDF upload failed.');
    } finally {
      setBusy(false);
    }
  };

  const handleDeletePdfRow = async (pdfId: string) => {
    if (!onDeletePdf) return;
    setDeletingPdfId(pdfId);
    setLibraryError(null);
    try {
      await onDeletePdf(pdfId);
    } catch (err) {
      setLibraryError(err instanceof Error ? err.message : 'Failed to delete PDF resource.');
    } finally {
      setDeletingPdfId(null);
    }
  };

  if (mode === 'pdf-library') {
    return (
      <div className="space-y-6">
        <StageMockNotice
          title="PDF Library & Backblaze B2 Object Storage (imtihan-pro-assets)"
          message={
            connectionMode === 'live_api'
              ? 'Supports textbooks, board past papers, notes packs, and pairing schemes. Uploaded PDFs (%PDF-, <=50 MB) store in Backblaze B2 and index by objectKey in Cloudflare D1.'
              : 'Offline Preview mode: browse textbooks, past papers, notes packs, and syllabi or register preview records.'
          }
        />

        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div className="flex flex-col sm:flex-row sm:items-center gap-3">
            <div className="relative">
              <Search className="h-4 w-4 absolute left-3 top-1/2 -translate-y-1/2 text-[#64748B] dark:text-[#7A93C2]" />
              <input
                type="search"
                aria-label="Search PDFs"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search title, subject, or year..."
                className="h-10 pl-9 pr-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#0D1B38] dark:text-white"
              />
            </div>

            <div className="flex items-center gap-1 p-1 rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] dark:border-[#223B70] dark:bg-[#091530] overflow-x-auto">
              {(['all', 'textbook', 'past_paper', 'notes_pack', 'syllabus'] as const).map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={() => setCategoryFilter(cat)}
                  className={`px-3 py-1.5 rounded text-xs font-medium whitespace-nowrap shrink-0 ${
                    categoryFilter === cat
                      ? 'bg-[#1D4ED8] text-white dark:bg-[#2563EB]'
                      : 'text-[#475569] hover:text-[#0F172A] dark:text-[#94A8CE] dark:hover:text-white'
                  }`}
                >
                  {cat === 'all' ? 'All PDFs' : PDF_CATEGORY_LABELS[cat]}
                </button>
              ))}
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setUploadError(null);
              setShowAddPdf(true);
            }}
            className="min-h-[40px] px-4 inline-flex items-center justify-center gap-2 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] text-white text-xs font-semibold whitespace-nowrap shrink-0"
          >
            {connectionMode === 'live_api' ? <Upload className="h-4 w-4" /> : <FilePlus2 className="h-4 w-4" />}
            {connectionMode === 'live_api' ? 'Upload PDF to B2' : 'Register PDF (Preview)'}
          </button>
        </div>

        {libraryError && (
          <div
            role="alert"
            className="rounded-xl border border-[#DC2626]/40 bg-[#FEF2F2] px-4 py-3 text-xs font-medium text-[#991B1B] dark:border-[#EF4444]/40 dark:bg-[#450A0A]/40 dark:text-[#FECACA] flex items-center justify-between gap-3"
          >
            <span>{libraryError}</span>
            <button
              type="button"
              onClick={() => setLibraryError(null)}
              className="underline font-semibold shrink-0"
            >
              Dismiss
            </button>
          </div>
        )}

        <div className="rounded-xl border border-[#CBD8EE] bg-white overflow-x-auto dark:border-[#223B70] dark:bg-[#0D1B38]">
          {filteredPdfs.length === 0 ? (
            <div className="p-8 text-center text-sm text-[#64748B] dark:text-[#8FA7D1]">
              No PDF resources match the active filter.
            </div>
          ) : (
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] text-xs font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
                <tr>
                  <th className="px-5 py-3">Document Title &amp; B2 Object Key</th>
                  <th className="px-5 py-3 whitespace-nowrap">Category</th>
                  <th className="px-5 py-3 whitespace-nowrap">Board / Class</th>
                  <th className="px-5 py-3 whitespace-nowrap">Details</th>
                  <th className="px-5 py-3 text-right whitespace-nowrap">Size</th>
                  <th className="px-5 py-3 text-right whitespace-nowrap">Pages</th>
                  <th className="px-5 py-3 text-right whitespace-nowrap">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
                {filteredPdfs.map((pdf) => {
                  const sub = dataset.subjects.find((s) => s.id === pdf.subjectId);
                  const pp = dataset.pastPapers.find((p) => p.pdfMetadataId === pdf.id);
                  const sizeMb = pdf.fileSizeBytes / (1024 * 1024);
                  return (
                    <tr
                      key={pdf.id}
                      className="hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60"
                    >
                      <td className="px-5 py-3.5">
                        <div className="font-medium text-[#0F172A] dark:text-white">
                          {pdf.title}
                        </div>
                        <div className="font-mono text-[11px] text-[#64748B] dark:text-[#7A93C2]">
                          {pdf.objectKey}
                        </div>
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap text-xs font-medium text-[#1E293B] dark:text-[#DCE8FC]">
                        {PDF_CATEGORY_LABELS[pdf.category]}
                      </td>
                      <td className="px-5 py-3.5 text-xs whitespace-nowrap text-[#475569] dark:text-[#94A8CE]">
                        {pdf.boardCode} · Class {pdf.classLevel}{sub ? ` · ${sub.name}` : ''}
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap">
                        {pp ? (
                          pp.isSolved ? (
                            <StatusIndicator label={`${pp.year} Solved`} tone="success" />
                          ) : (
                            <StatusIndicator label={`${pp.year} Original`} tone="info" />
                          )
                        ) : (
                          <StatusIndicator
                            label={pdf.offlineCacheable ? 'Offline Ready' : 'Online'}
                            tone={pdf.category === 'textbook' ? 'info' : 'neutral'}
                          />
                        )}
                      </td>
                      <td className="px-5 py-3.5 text-right tabular-nums whitespace-nowrap text-xs text-[#1E293B] dark:text-[#DCE8FC]">
                        {sizeMb.toFixed(2)} MB
                      </td>
                      <td className="px-5 py-3.5 text-right tabular-nums whitespace-nowrap text-xs text-[#1E293B] dark:text-[#DCE8FC]">
                        {pdf.pageCount}
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        {onDeletePdf && (
                          <button
                            type="button"
                            disabled={deletingPdfId === pdf.id}
                            aria-label={`Delete PDF ${pdf.title}`}
                            onClick={() => void handleDeletePdfRow(pdf.id)}
                            className="p-1.5 rounded text-[#64748B] hover:text-[#DC2626] dark:text-[#8FA7D1] dark:hover:text-[#F87171] disabled:opacity-50"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        {showAddPdf && (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-pdf-title"
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
          >
            <form
              onSubmit={handleAddPdf}
              className="w-full max-w-md rounded-xl border border-[#CBD8EE] bg-white p-6 space-y-4 dark:border-[#24407A] dark:bg-[#0D1B38]"
            >
              <h2 id="add-pdf-title" className="font-display text-lg font-bold text-[#0F172A] dark:text-white">
                {connectionMode === 'live_api'
                  ? 'Upload Curriculum PDF to Backblaze B2'
                  : 'Register Curriculum PDF (Preview)'}
              </h2>
              {uploadError && (
                <div className="rounded-lg border border-[#FCA5A5] bg-[#FEF2F2] px-3 py-2 text-xs text-[#991B1B] dark:border-[#7F1D1D] dark:bg-[#2A1215] dark:text-[#FECACA]">
                  {uploadError}
                </div>
              )}
              {connectionMode === 'live_api' && (
                <div>
                  <label htmlFor="pdf-file" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                    PDF Binary File (.pdf, max 50 MB)
                  </label>
                  <input
                    id="pdf-file"
                    type="file"
                    accept="application/pdf,.pdf"
                    required
                    onChange={handleFileChange}
                    className="mt-1 w-full text-xs text-[#334155] dark:text-[#B5C6E6] file:mr-3 file:py-2 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-[#1D4ED8] file:text-white"
                  />
                </div>
              )}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label htmlFor="pdf-category" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                    Category
                  </label>
                  <select
                    id="pdf-category"
                    value={pdfCategory}
                    onChange={(e) => setPdfCategory(e.target.value as ApiPdfMetadata['category'])}
                    className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                  >
                    <option value="past_paper">Past Paper</option>
                    <option value="textbook">Textbook</option>
                    <option value="notes_pack">Notes Pack</option>
                    <option value="syllabus">Syllabus / Pairing</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="pdf-sub" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                    Subject
                  </label>
                  <select
                    id="pdf-sub"
                    value={pdfSubjectId || dataset.subjects[0]?.id || ''}
                    onChange={(e) => setPdfSubjectId(e.target.value)}
                    className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                  >
                    {dataset.subjects.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} (Class {s.classLevel})
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div>
                <label htmlFor="pdf-title" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                  Display Title
                </label>
                <input
                  id="pdf-title"
                  type="text"
                  required
                  value={pdfTitle}
                  onChange={(e) => setPdfTitle(e.target.value)}
                  placeholder="KPK Class 9 Physics — Textbook Unit 1-9"
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                {pdfCategory === 'past_paper' && (
                  <div>
                    <label htmlFor="pdf-year" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                      Exam Year
                    </label>
                    <input
                      id="pdf-year"
                      type="number"
                      min={2010}
                      max={2030}
                      value={pdfYear}
                      onChange={(e) => setPdfYear(Number(e.target.value))}
                      className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                    />
                  </div>
                )}
                <div className={pdfCategory === 'past_paper' ? '' : 'col-span-2'}>
                  <label htmlFor="pdf-pages" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                    Page Count
                  </label>
                  <input
                    id="pdf-pages"
                    type="number"
                    min={1}
                    max={500}
                    value={pdfPageCount}
                    onChange={(e) => setPdfPageCount(Number(e.target.value))}
                    className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                  />
                </div>
              </div>
              {pdfCategory === 'past_paper' && (
                <label className="flex items-center gap-2 text-xs text-[#334155] dark:text-[#B5C6E6]">
                  <input
                    type="checkbox"
                    checked={pdfSolved}
                    onChange={(e) => setPdfSolved(e.target.checked)}
                  />
                  Includes step-by-step solved solutions
                </label>
              )}
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddPdf(false)}
                  className="min-h-[40px] px-4 rounded-lg border border-[#CBD8EE] text-xs font-semibold text-[#334155] dark:border-[#24407A] dark:text-[#DCE8FC]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={busy}
                  className="min-h-[40px] px-4 rounded-lg bg-[#1D4ED8] text-white text-xs font-semibold disabled:opacity-60"
                >
                  {busy ? 'Uploading...' : connectionMode === 'live_api' ? 'Upload to B2' : 'Save Metadata'}
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <StageMockNotice
        title="Guest-First Learner Directory"
        message={
          connectionMode === 'live_api'
            ? 'Students start immediately in guest mode on Android and only link Google accounts optionally for cross-device sync.'
            : 'Offline Preview mode: showing sample learner records.'
        }
      />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="relative">
          <Search className="h-4 w-4 absolute left-3 top-1/2 -translate-y-1/2 text-[#64748B] dark:text-[#7A93C2]" />
          <input
            type="search"
            aria-label="Search students"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search learner or class..."
            className="h-10 pl-9 pr-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#0D1B38] dark:text-white"
          />
        </div>

        <div className="flex items-center gap-1 p-1 rounded-lg border border-[#CBD8EE] bg-[#F8FAFE] dark:border-[#223B70] dark:bg-[#091530]">
          {(['all', 'guest', 'google_synced'] as const).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setStudentFilter(t)}
              className={`px-3 py-1.5 rounded text-xs font-medium capitalize ${
                studentFilter === t
                  ? 'bg-[#1D4ED8] text-white dark:bg-[#2563EB]'
                  : 'text-[#475569] hover:text-[#0F172A] dark:text-[#94A8CE] dark:hover:text-white'
              }`}
            >
              {t === 'google_synced' ? 'Google Synced' : t}
            </button>
          ))}
        </div>
      </div>

      <div className="rounded-xl border border-[#CBD8EE] bg-white overflow-x-auto dark:border-[#223B70] dark:bg-[#0D1B38]">
        {filteredStudents.length === 0 ? (
          <div className="p-8 text-center text-sm text-[#64748B] dark:text-[#8FA7D1]">
            No student sessions synced yet. Learners will appear once the Android app calls /v1/auth/guest or /v1/auth/google/exchange.
          </div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-[#E2E8F0] bg-[#F8FAFE] text-xs font-semibold text-[#475569] dark:border-[#1B3263] dark:bg-[#091530] dark:text-[#8FA7D1]">
              <tr>
                <th className="px-5 py-3">Learner</th>
                <th className="px-5 py-3">Class / Board</th>
                <th className="px-5 py-3">Account Mode</th>
                <th className="px-5 py-3 text-right">Chapters</th>
                <th className="px-5 py-3 text-right">Quiz Avg</th>
                <th className="px-5 py-3 text-right">Last Active</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1B3263]">
              {filteredStudents.map((st) => (
                <tr key={st.id} className="hover:bg-[#F8FAFE] dark:hover:bg-[#112247]/60">
                  <td className="px-5 py-3.5 font-medium text-[#0F172A] dark:text-white">
                    {st.displayName}
                  </td>
                  <td className="px-5 py-3.5 text-xs text-[#475569] dark:text-[#94A8CE]">
                    Class {st.classLevel} · {st.boardCode}
                  </td>
                  <td className="px-5 py-3.5">
                    {st.accountType === 'google_synced' ? (
                      <StatusIndicator label="Google Synced" tone="success" />
                    ) : (
                      <StatusIndicator label="Guest Local" tone="info" />
                    )}
                  </td>
                  <td className="px-5 py-3.5 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                    {st.chaptersCompleted}
                  </td>
                  <td className="px-5 py-3.5 text-right tabular-nums text-[#1E293B] dark:text-[#DCE8FC]">
                    {st.quizAveragePercent}%
                  </td>
                  <td className="px-5 py-3.5 text-right tabular-nums text-xs text-[#475569] dark:text-[#94A8CE]">
                    {st.lastActiveIso.slice(0, 10)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
