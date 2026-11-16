import type {
  AdminAuthResponse,
  AdminBootstrapRequest,
  AdminBootstrapStatusResponse,
  AdminCapacityDashboardResponse,
  AdminChapterWritePayload,
  AdminInfrastructureStatusResponse,
  AdminLoginRequest,
  AdminMcqWritePayload,
  AdminSubjectWritePayload,
  ApiAiAgentSubmission,
  ApiChapter,
  ApiEducationalContent,
  ApiEnvelope,
  ApiFeedbackReport,
  ApiMcqItem,
  ApiPastPaper,
  ApiPdfMetadata,
  ApiSubject,
  CapacityPolicyConfig,
  EducationalContentBlock,
  PublishStatus,
  UsageSnapshotMetrics,
} from '../../shared-contracts/types';
import {
  buildStandardCapacityMatrix,
  DEFAULT_CAPACITY_POLICY,
  evaluateQuotaAlerts,
  OFFICIAL_FREE_TIER_LIMITS,
} from '../../shared-contracts/capacity';
import { API_ENDPOINTS, DEFAULT_API_CONFIG } from '../../shared-contracts/endpoints';
import {
  canTransitionPublishStatus,
  canTransitionSubmissionStatus,
  validateContentBlock,
} from '../../shared-contracts/rules';
import type { AdminDataset, AdminSessionState, AdminSettingsState } from '../types/admin';
import { INITIAL_ADMIN_DATASET, INITIAL_ADMIN_SETTINGS } from '../data/mockSeedData';

/**
 * Repository interface decoupling the Admin Dashboard UI from Stage 1 mock data
 * while preserving deterministic offline verification tests.
 */
export interface IAdminRepository {
  getDataset(): AdminDataset;
  getSettings(): AdminSettingsState;
  saveSettings(next: AdminSettingsState): AdminSettingsState;
  createSubject(input: Omit<ApiSubject, 'id' | 'updatedAtIso'>): ApiSubject;
  createChapter(input: Omit<ApiChapter, 'id' | 'updatedAtIso'>): ApiChapter;
  saveChapterBlocks(chapterId: string, blocks: EducationalContentBlock[]): {
    ok: boolean;
    errors: string[];
  };
  transitionAiSubmission(
    submissionId: string,
    nextStatus: ApiAiAgentSubmission['status'],
    reviewerNotes: string
  ): { ok: boolean; error?: string };
  addPdfPaperRecord(
    paper: Omit<ApiPastPaper, 'id' | 'pdfMetadataId' | 'downloadCount'>,
    title: string
  ): { paper: ApiPastPaper; pdf: ApiPdfMetadata };
}

export class Stage1MockAdminRepository implements IAdminRepository {
  private dataset: AdminDataset;
  private settings: AdminSettingsState;
  private capacityPolicy: CapacityPolicyConfig;

  constructor(
    initialDataset: AdminDataset = structuredClone(INITIAL_ADMIN_DATASET),
    initialSettings: AdminSettingsState = structuredClone(INITIAL_ADMIN_SETTINGS),
    initialCapacityPolicy: CapacityPolicyConfig = structuredClone(DEFAULT_CAPACITY_POLICY)
  ) {
    this.dataset = initialDataset;
    this.settings = initialSettings;
    this.capacityPolicy = initialCapacityPolicy;
  }

  getDataset(): AdminDataset {
    return this.dataset;
  }

  getSettings(): AdminSettingsState {
    return this.settings;
  }

  saveSettings(next: AdminSettingsState): AdminSettingsState {
    this.settings = { ...next };
    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Preview Operator',
      action: 'Updated local preview settings',
      targetEntity: next.apiBaseUrl,
      environmentMode: 'stage_1_mock',
      timestampIso: new Date().toISOString(),
    });
    return this.settings;
  }

  createSubject(input: Omit<ApiSubject, 'id' | 'updatedAtIso'>): ApiSubject {
    const created: ApiSubject = {
      ...input,
      id: `sub_${input.boardCode.toLowerCase()}${input.classLevel}_${Date.now()}`,
      updatedAtIso: new Date().toISOString(),
    };
    this.dataset.subjects = [created, ...this.dataset.subjects];
    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Preview Operator',
      action: 'Created subject draft (offline preview)',
      targetEntity: `${created.name} (Class ${created.classLevel})`,
      environmentMode: 'stage_1_mock',
      timestampIso: new Date().toISOString(),
    });
    return created;
  }

  createChapter(input: Omit<ApiChapter, 'id' | 'updatedAtIso'>): ApiChapter {
    const created: ApiChapter = {
      ...input,
      id: `ch_${Date.now()}`,
      updatedAtIso: new Date().toISOString(),
    };
    this.dataset.chapters = [created, ...this.dataset.chapters];
    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Preview Operator',
      action: 'Created chapter draft (offline preview)',
      targetEntity: `Ch ${created.chapterNumber}: ${created.title}`,
      environmentMode: 'stage_1_mock',
      timestampIso: new Date().toISOString(),
    });
    return created;
  }

  saveChapterBlocks(
    chapterId: string,
    blocks: EducationalContentBlock[]
  ): { ok: boolean; errors: string[]; contentId?: string } {
    const allErrors = blocks.flatMap((b) => validateContentBlock(b));
    if (allErrors.length > 0) {
      return { ok: false, errors: allErrors };
    }

    const existing = this.dataset.contents.find(
      (c) => c.chapterId === chapterId && c.mode === 'detailed_notes'
    );
    const nowIso = new Date().toISOString();
    let contentId = existing?.id;
    if (existing) {
      existing.blocks = blocks.map((b, idx) => ({ ...b, orderIndex: idx + 1 }));
      existing.updatedAtIso = nowIso;
      existing.publishStatus = 'draft';
    } else {
      contentId = `cnt_${chapterId}_${Date.now()}`;
      this.dataset.contents.push({
        id: contentId,
        chapterId,
        mode: 'detailed_notes',
        version: 'v0.1-draft',
        publishStatus: 'draft',
        language: 'en',
        authorOrSource: 'Admin Content Studio (Preview)',
        updatedAtIso: nowIso,
        blocks: blocks.map((b, idx) => ({ ...b, orderIndex: idx + 1 })),
      });
    }

    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Content Editor',
      action: 'Saved content blocks (offline preview)',
      targetEntity: `${chapterId} (${blocks.length} blocks)`,
      environmentMode: 'stage_1_mock',
      timestampIso: nowIso,
    });
    return { ok: true, errors: [], contentId };
  }

  transitionAiSubmission(
    submissionId: string,
    nextStatus: ApiAiAgentSubmission['status'],
    reviewerNotes: string
  ): { ok: boolean; error?: string } {
    const target = this.dataset.aiSubmissions.find((s) => s.id === submissionId);
    if (!target) {
      return { ok: false, error: 'Submission record not found.' };
    }
    if (!canTransitionSubmissionStatus(target.status, nextStatus)) {
      return {
        ok: false,
        error: `Cannot move submission from ${target.status} to ${nextStatus}.`,
      };
    }
    this.dataset.aiSubmissions = this.dataset.aiSubmissions.map((s) =>
      s.id === submissionId ? { ...s, status: nextStatus, reviewerNotes } : s
    );

    this.dataset.auditLog = [
      {
        id: `aud_${Date.now()}`,
        actorName: 'Content Reviewer',
        action: `Marked AI submission as ${nextStatus} (offline preview)`,
        targetEntity: target.chapterTitle,
        environmentMode: 'stage_1_mock',
        timestampIso: new Date().toISOString(),
      },
      ...this.dataset.auditLog,
    ];
    return { ok: true };
  }

  addPdfPaperRecord(
    paper: Omit<ApiPastPaper, 'id' | 'pdfMetadataId' | 'downloadCount'>,
    title: string
  ): { paper: ApiPastPaper; pdf: ApiPdfMetadata } {
    const idSuffix = Date.now();
    const pdfId = `pdf_${idSuffix}`;
    const createdPdf: ApiPdfMetadata = {
      id: pdfId,
      title,
      category: 'past_paper',
      classLevel: paper.classLevel,
      boardCode: paper.boardCode,
      subjectId: paper.subjectId,
      objectKey: `${paper.boardCode.toLowerCase()}/class-${paper.classLevel}/${paper.subjectName.toLowerCase()}/${paper.year}-${paper.session.toLowerCase()}.pdf`,
      fileSizeBytes: Math.round(paper.fileSizeMb * 1024 * 1024),
      pageCount: paper.pageCount,
      checksumSha256: `preview_sha256_${idSuffix}`,
      offlineCacheable: true,
      version: '1.0',
    };
    const createdPaper: ApiPastPaper = {
      ...paper,
      id: `pp_${idSuffix}`,
      pdfMetadataId: pdfId,
      downloadCount: 0,
    };

    this.dataset.pdfs = [createdPdf, ...this.dataset.pdfs];
    this.dataset.pastPapers = [createdPaper, ...this.dataset.pastPapers];
    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Preview Operator',
      action: 'Registered PDF metadata record (offline preview)',
      targetEntity: title,
      environmentMode: 'stage_1_mock',
      timestampIso: new Date().toISOString(),
    });

    return { paper: createdPaper, pdf: createdPdf };
  }

  addCategorizedPdfRecord(params: {
    category: ApiPdfMetadata['category'];
    title: string;
    subject: ApiSubject;
    year: number;
    session: 'Annual' | 'Supplementary';
    isSolved: boolean;
    fileSizeMb: number;
    pageCount: number;
  }): ApiPdfMetadata {
    const idSuffix = Date.now();
    const pdfId = `pdf_${idSuffix}`;
    const createdPdf: ApiPdfMetadata = {
      id: pdfId,
      title: params.title,
      category: params.category,
      classLevel: params.subject.classLevel,
      boardCode: params.subject.boardCode,
      subjectId: params.subject.id,
      objectKey: `${params.subject.boardCode.toLowerCase()}/class-${params.subject.classLevel}/${params.category}/${idSuffix}.pdf`,
      fileSizeBytes: Math.round(params.fileSizeMb * 1024 * 1024),
      pageCount: params.pageCount,
      checksumSha256: `preview_sha256_${idSuffix}`,
      offlineCacheable: true,
      version: '1.0',
    };
    this.dataset.pdfs = [createdPdf, ...this.dataset.pdfs];
    if (params.category === 'past_paper') {
      this.dataset.pastPapers = [
        {
          id: `pp_${idSuffix}`,
          boardCode: params.subject.boardCode,
          classLevel: params.subject.classLevel,
          subjectId: params.subject.id,
          subjectName: params.subject.name,
          year: params.year,
          session: params.session,
          isSolved: params.isSolved,
          pdfMetadataId: pdfId,
          fileSizeMb: params.fileSizeMb,
          pageCount: params.pageCount,
          downloadCount: 0,
        },
        ...this.dataset.pastPapers,
      ];
    }
    this.dataset.auditLog.unshift({
      id: `aud_${idSuffix}`,
      actorName: 'Preview Operator',
      action: `Registered ${params.category} PDF (offline preview)`,
      targetEntity: params.title,
      environmentMode: 'stage_1_mock',
      timestampIso: new Date().toISOString(),
    });
    return createdPdf;
  }

  transitionContentStatus(contentId: string, nextStatus: PublishStatus): { ok: boolean; error?: string } {
    const target = this.dataset.contents.find((c) => c.id === contentId);
    if (!target) return { ok: false, error: 'Content version not found.' };
    if (!canTransitionPublishStatus(target.publishStatus, nextStatus)) {
      return {
        ok: false,
        error: `Cannot transition from ${target.publishStatus} to ${nextStatus}.`,
      };
    }
    const nowIso = new Date().toISOString();
    target.publishStatus = nextStatus;
    target.updatedAtIso = nowIso;
    this.dataset.chapters = this.dataset.chapters.map((ch) =>
      ch.id === target.chapterId ? { ...ch, publishStatus: nextStatus, updatedAtIso: nowIso } : ch
    );
    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Preview Reviewer',
      action: `Transitioned content status to ${nextStatus}`,
      targetEntity: contentId,
      environmentMode: 'stage_1_mock',
      timestampIso: nowIso,
    });
    return { ok: true };
  }

  createMcq(payload: AdminMcqWritePayload): ApiMcqItem {
    const created: ApiMcqItem = {
      id: `mcq_${Date.now()}`,
      chapterId: payload.chapterId,
      subjectId: payload.subjectId,
      classLevel: payload.classLevel,
      question: payload.question,
      options: payload.options,
      correctOptionId: payload.correctOptionId,
      explanation: payload.explanation,
      difficulty: payload.difficulty,
      topicTitle: payload.topicTitle,
      pastPaperYear: payload.pastPaperYear ?? undefined,
      publishStatus: payload.publishStatus ?? 'published',
      sourceReference: payload.sourceReference ?? 'Admin Content Studio',
    };
    this.dataset.mcqs = [created, ...(this.dataset.mcqs ?? [])];
    this.dataset.chapters = this.dataset.chapters.map((ch) =>
      ch.id === payload.chapterId ? { ...ch, mcqCount: ch.mcqCount + 1 } : ch
    );
    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Preview Editor',
      action: 'Added MCQ to Question Bank',
      targetEntity: created.question.slice(0, 60),
      environmentMode: 'stage_1_mock',
      timestampIso: new Date().toISOString(),
    });
    return created;
  }

  deleteMcq(mcqId: string): void {
    const existing = (this.dataset.mcqs ?? []).find((m) => m.id === mcqId);
    this.dataset.mcqs = (this.dataset.mcqs ?? []).filter((m) => m.id !== mcqId);
    if (existing) {
      this.dataset.chapters = this.dataset.chapters.map((ch) =>
        ch.id === existing.chapterId ? { ...ch, mcqCount: Math.max(0, ch.mcqCount - 1) } : ch
      );
    }
  }

  triageFeedback(
    feedbackId: string,
    status: ApiFeedbackReport['status'],
    adminResolutionNote: string
  ): void {
    const nowIso = new Date().toISOString();
    this.dataset.feedbackReports = (this.dataset.feedbackReports ?? []).map((f) =>
      f.id === feedbackId ? { ...f, status, adminResolutionNote, updatedAtIso: nowIso } : f
    );
    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Preview Reviewer',
      action: `Marked feedback report ${status}`,
      targetEntity: feedbackId,
      environmentMode: 'stage_1_mock',
      timestampIso: nowIso,
    });
  }

  getOfflineCapacityDashboard(): AdminCapacityDashboardResponse {
    const b2StorageBytes = this.dataset.pdfs.reduce(
      (sum, pdf) => sum + Math.max(0, Number(pdf.fileSizeBytes || 0)),
      0
    );
    const usage: UsageSnapshotMetrics = {
      dayYmd: new Date().toISOString().slice(0, 10),
      workerRequestsToday: 0,
      edgeOrHttpCacheHitsToday: 0,
      d1RowsReadEstToday: 0,
      d1RowsWrittenEstToday: 0,
      d1DatabaseSizeBytes: 0,
      b2StorageBytes,
      b2DailyDownloadBytes: 0,
      b2DailyDownloadCount: 0,
      aiCallsToday: 0,
      aiCacheHitsToday: 0,
      aiBlockedQuotaToday: 0,
      apiErrorCountToday: 0,
      avgLatencyMs: 0,
      telemetryNote:
        'Offline UI Preview mode — live Worker telemetry counters show 0 until connected to imtihan-pro-api; B2 storage reflects seeded pdf_assets bytes and DAU simulator runs live.',
    };
    const limits = OFFICIAL_FREE_TIER_LIMITS;
    const alerts = evaluateQuotaAlerts(usage, this.capacityPolicy, limits);

    return {
      limits,
      policy: { ...this.capacityPolicy },
      usage,
      alerts,
      remainingCapacity: {
        workerRequestsRemaining: limits.workersRequestsPerDay,
        d1RowsReadRemaining: limits.d1RowsReadPerDay,
        d1RowsWrittenRemaining: limits.d1RowsWrittenPerDay,
        d1StorageMbRemaining: limits.d1MaxDatabaseSizeMb,
        b2StorageGbRemaining: Number(
          Math.max(0, limits.b2FreeStorageGb - b2StorageBytes / (1024 * 1024 * 1024)).toFixed(3)
        ),
        b2DailyDownloadMbRemaining: limits.b2ConservativeDailyDownloadGb * 1024,
        aiCallsRemaining: this.capacityPolicy.aiTutorDailyGlobalCap,
      },
      dauProjections: buildStandardCapacityMatrix(),
      safeFallbackActive: {
        aiTutorPausedByCap: false,
        pdfStreamingGuardedByCap: false,
      },
    };
  }

  saveOfflineCapacityPolicy(next: Partial<CapacityPolicyConfig>): CapacityPolicyConfig {
    this.capacityPolicy = {
      ...this.capacityPolicy,
      ...next,
    };
    this.dataset.auditLog.unshift({
      id: `aud_${Date.now()}`,
      actorName: 'Preview Operator',
      action: 'Updated capacity & cost-protection policy (offline preview)',
      targetEntity: 'capacity_policy',
      environmentMode: 'stage_1_mock',
      timestampIso: new Date().toISOString(),
    });
    return { ...this.capacityPolicy };
  }
}

const STORAGE_API_URL_KEY = 'imtihan_admin_api_base_url';
const STORAGE_ADMIN_SESSION_KEY = 'imtihan_admin_session_json';

export function resolveInitialApiBaseUrl(): string {
  const envUrl =
    typeof import.meta !== 'undefined' &&
    (import.meta as unknown as { env?: Record<string, string | undefined> }).env?.VITE_API_BASE_URL;
  if (typeof window !== 'undefined') {
    const stored = window.localStorage.getItem(STORAGE_API_URL_KEY);
    if (stored && stored.trim()) return stored.trim().replace(/\/+$/, '');
  }
  if (envUrl && envUrl.trim()) return envUrl.trim().replace(/\/+$/, '');
  return DEFAULT_API_CONFIG.baseUrl.replace(/\/+$/, '');
}

export function loadStoredAdminSession(): AdminSessionState | null {
  if (typeof window === 'undefined') return null;
  const raw = window.sessionStorage.getItem(STORAGE_ADMIN_SESSION_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as AdminSessionState;
    if (!parsed.token || !parsed.expiresAtIso) return null;
    if (Date.parse(parsed.expiresAtIso) <= Date.now()) {
      window.sessionStorage.removeItem(STORAGE_ADMIN_SESSION_KEY);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function persistAdminSession(session: AdminSessionState | null): void {
  if (typeof window === 'undefined') return;
  if (!session) {
    window.sessionStorage.removeItem(STORAGE_ADMIN_SESSION_KEY);
  } else {
    window.sessionStorage.setItem(STORAGE_ADMIN_SESSION_KEY, JSON.stringify(session));
  }
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

/**
 * Live Stage 2 HTTP client communicating with Cloudflare Worker (`imtihan-pro-api`).
 */
export class CloudflareAdminApiClient {
  private baseUrl: string;
  private token: string | null;

  constructor(baseUrl = resolveInitialApiBaseUrl(), token: string | null = null) {
    this.baseUrl = baseUrl.replace(/\/+$/, '');
    this.token = token;
  }

  getBaseUrl(): string {
    return this.baseUrl;
  }

  setBaseUrl(url: string): void {
    this.baseUrl = url.trim().replace(/\/+$/, '');
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(STORAGE_API_URL_KEY, this.baseUrl);
    }
  }

  setToken(token: string | null): void {
    this.token = token;
  }

  private async request<T>(
    endpointPath: string,
    init: RequestInit = {},
    isMultipart = false
  ): Promise<T> {
    const headers = new Headers(init.headers);
    if (!isMultipart && init.body && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }
    if (this.token) {
      headers.set('Authorization', `Bearer ${this.token}`);
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), DEFAULT_API_CONFIG.timeoutMs);

    try {
      const response = await fetch(`${this.baseUrl}${endpointPath}`, {
        ...init,
        headers,
        signal: controller.signal,
      });

      const text = await response.text();
      let json: ApiEnvelope<T> | null = null;
      try {
        json = text ? (JSON.parse(text) as ApiEnvelope<T>) : null;
      } catch {
        throw new ApiClientError(
          response.status,
          'INVALID_RESPONSE',
          `Worker returned non-JSON HTTP ${response.status}.`
        );
      }

      if (!response.ok || !json?.ok) {
        throw new ApiClientError(
          response.status,
          json?.error?.code || 'HTTP_ERROR',
          json?.error?.message || `Request failed with HTTP ${response.status}.`
        );
      }

      return json.data;
    } catch (err) {
      if (err instanceof ApiClientError) throw err;
      if (err instanceof DOMException && err.name === 'AbortError') {
        throw new ApiClientError(408, 'TIMEOUT', 'Request timed out reaching Worker API.');
      }
      throw new ApiClientError(
        0,
        'NETWORK_UNREACHABLE',
        err instanceof Error ? err.message : 'Unable to reach Cloudflare Worker API.'
      );
    } finally {
      clearTimeout(timer);
    }
  }

  checkHealth(): Promise<{ status: string; worker: string; d1Connected: boolean; b2Configured: boolean }> {
    return this.request(API_ENDPOINTS.health);
  }

  getBootstrapStatus(): Promise<AdminBootstrapStatusResponse> {
    return this.request(API_ENDPOINTS.adminBootstrapStatus);
  }

  bootstrapFirstAdmin(payload: AdminBootstrapRequest): Promise<AdminAuthResponse> {
    return this.request(API_ENDPOINTS.adminBootstrap, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  login(payload: AdminLoginRequest): Promise<AdminAuthResponse> {
    return this.request(API_ENDPOINTS.adminLogin, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  fetchOverview(): Promise<AdminDataset> {
    return this.request(API_ENDPOINTS.adminOverview);
  }

  createSubject(payload: AdminSubjectWritePayload): Promise<ApiSubject> {
    return this.request(API_ENDPOINTS.adminSubjects, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  deleteSubject(subjectId: string): Promise<{ deleted: boolean; id: string }> {
    return this.request(API_ENDPOINTS.adminSubjectById(subjectId), {
      method: 'DELETE',
    });
  }

  createChapter(payload: AdminChapterWritePayload): Promise<ApiChapter> {
    return this.request(API_ENDPOINTS.adminChapters, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  deleteChapter(chapterId: string): Promise<{ deleted: boolean; id: string }> {
    return this.request(API_ENDPOINTS.adminChapterById(chapterId), {
      method: 'DELETE',
    });
  }

  saveContentDraft(
    chapterId: string,
    blocks: EducationalContentBlock[],
    mode: ApiEducationalContent['mode'] = 'detailed_notes'
  ): Promise<ApiEducationalContent> {
    return this.request(API_ENDPOINTS.adminContentDraft, {
      method: 'POST',
      body: JSON.stringify({ chapterId, mode, blocks }),
    });
  }

  transitionContentStatus(
    contentId: string,
    status: PublishStatus
  ): Promise<{ contentId: string; publishStatus: PublishStatus; updatedAtIso: string }> {
    return this.request(API_ENDPOINTS.adminContentStatus(contentId), {
      method: 'POST',
      body: JSON.stringify({ status }),
    });
  }

  createMcq(payload: AdminMcqWritePayload): Promise<ApiMcqItem> {
    return this.request(API_ENDPOINTS.adminMcqs, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  deleteMcq(mcqId: string): Promise<{ deleted: boolean; id: string }> {
    return this.request(API_ENDPOINTS.adminMcqById(mcqId), { method: 'DELETE' });
  }

  triageFeedback(
    feedbackId: string,
    status: ApiFeedbackReport['status'],
    adminResolutionNote: string
  ): Promise<{ id: string; status: ApiFeedbackReport['status']; updatedAtIso: string }> {
    return this.request(API_ENDPOINTS.adminFeedbackById(feedbackId), {
      method: 'PATCH',
      body: JSON.stringify({ status, adminResolutionNote }),
    });
  }

  publishContentVersion(
    contentId: string
  ): Promise<{ contentId: string; chapterId: string; version: string; publishStatus: 'published' }> {
    return this.request(API_ENDPOINTS.adminPublishVersion(contentId), {
      method: 'POST',
    });
  }

  decideAiSubmission(
    submissionId: string,
    status: ApiAiAgentSubmission['status'],
    reviewerNotes: string
  ): Promise<{ submissionId: string; status: ApiAiAgentSubmission['status'] }> {
    return this.request(API_ENDPOINTS.adminReviewDecision(submissionId), {
      method: 'POST',
      body: JSON.stringify({ status, reviewerNotes }),
    });
  }

  promoteAiSubmission(
    submissionId: string
  ): Promise<{ submissionId: string; targetChapterId: string; promotedContentId: string | null }> {
    return this.request(API_ENDPOINTS.adminPromoteSubmission(submissionId), {
      method: 'POST',
    });
  }

  uploadPdfFile(params: {
    file: File;
    title: string;
    subjectId: string;
    year: number;
    session: 'Annual' | 'Supplementary';
    isSolved: boolean;
    pageCount: number;
    category?: ApiPdfMetadata['category'];
  }): Promise<{ pdf: ApiPdfMetadata; paper: ApiPastPaper }> {
    const form = new FormData();
    form.append('file', params.file);
    form.append('title', params.title);
    form.append('subjectId', params.subjectId);
    form.append('year', String(params.year));
    form.append('session', params.session);
    form.append('isSolved', String(params.isSolved));
    form.append('pageCount', String(params.pageCount));
    form.append('category', params.category || 'past_paper');

    return this.request(
      API_ENDPOINTS.adminPdfs,
      { method: 'POST', body: form },
      true
    );
  }

  deletePdf(pdfId: string): Promise<{ deleted: boolean; id: string }> {
    return this.request(API_ENDPOINTS.adminPdfById(pdfId), { method: 'DELETE' });
  }

  fetchInfrastructureStatus(): Promise<AdminInfrastructureStatusResponse> {
    return this.request(API_ENDPOINTS.adminInfrastructure);
  }

  fetchSettingsPolicy(): Promise<Partial<AdminSettingsState>> {
    return this.request(API_ENDPOINTS.adminSettings);
  }

  saveSettingsPolicy(settings: AdminSettingsState): Promise<AdminSettingsState> {
    return this.request(API_ENDPOINTS.adminSettings, {
      method: 'PUT',
      body: JSON.stringify({
        guestFirstMandatory: settings.guestFirstMandatory,
        allowOptionalGoogleSync: settings.allowOptionalGoogleSync,
        defaultBoardCode: settings.defaultBoardCode,
        offlineMaxCacheMb: settings.offlineMaxCacheMb,
        adPlacement: settings.adPlacement,
      }),
    });
  }

  fetchCapacityDashboard(): Promise<AdminCapacityDashboardResponse> {
    return this.request(API_ENDPOINTS.adminCapacity);
  }

  saveCapacityPolicy(policy: Partial<CapacityPolicyConfig>): Promise<CapacityPolicyConfig> {
    return this.request(API_ENDPOINTS.adminCapacityPolicy, {
      method: 'PUT',
      body: JSON.stringify(policy),
    });
  }
}
