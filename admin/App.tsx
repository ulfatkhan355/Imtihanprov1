import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type {
  AdminBootstrapStatusResponse,
  AdminCapacityDashboardResponse,
  AdminInfrastructureStatusResponse,
  AdminMcqWritePayload,
  ApiAiAgentSubmission,
  ApiChapter,
  ApiFeedbackReport,
  ApiSubject,
  CapacityPolicyConfig,
  EducationalContentBlock,
  PublishStatus,
} from '../shared-contracts/types';
import { AdminAuthGate } from './components/AdminAuthGate';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import {
  CloudflareAdminApiClient,
  loadStoredAdminSession,
  persistAdminSession,
  resolveInitialApiBaseUrl,
  Stage1MockAdminRepository,
} from './services/adminApiService';
import type {
  AdminConnectionMode,
  AdminDataset,
  AdminSectionId,
  AdminSessionState,
  AdminSettingsState,
} from './types/admin';
import { OverviewPage } from './pages/OverviewPage';
import { AcademicManagementPages } from './pages/AcademicManagementPages';
import { ContentStudioPage } from './pages/ContentStudioPage';
import { SubmissionsAndQueuePages } from './pages/SubmissionsAndQueuePages';
import {
  LibraryAndStudentsPages,
  type AdminPdfSubmitPayload,
} from './pages/LibraryAndStudentsPages';
import { FeedbackTriagePage } from './pages/FeedbackTriagePage';
import { AnalyticsInfraSettingsPages } from './pages/AnalyticsInfraSettingsPages';
import { CapacityDashboardPage } from './pages/CapacityDashboardPage';
import { INITIAL_ADMIN_SETTINGS } from './data/mockSeedData';

const mockRepository = new Stage1MockAdminRepository();

export default function AdminApp() {
  const [apiClient] = useState(
    () => new CloudflareAdminApiClient(resolveInitialApiBaseUrl(), loadStoredAdminSession()?.token ?? null)
  );
  const [connectionMode, setConnectionMode] = useState<AdminConnectionMode>('live_api');
  const [session, setSession] = useState<AdminSessionState | null>(() => loadStoredAdminSession());
  const [bootstrapStatus, setBootstrapStatus] = useState<AdminBootstrapStatusResponse | null>(null);
  const [authConnectionError, setAuthConnectionError] = useState<string | null>(null);
  const [authBusy, setAuthBusy] = useState(false);

  const [liveDataset, setLiveDataset] = useState<AdminDataset | null>(null);
  const [liveSettings, setLiveSettings] = useState<AdminSettingsState>(() => ({
    ...INITIAL_ADMIN_SETTINGS,
    apiBaseUrl: resolveInitialApiBaseUrl(),
  }));
  const [infraStatus, setInfraStatus] = useState<AdminInfrastructureStatusResponse | null>(null);
  const [capacityDashboard, setCapacityDashboard] =
    useState<AdminCapacityDashboardResponse | null>(null);

  const [activeSection, setActiveSection] = useState<AdminSectionId>('overview');
  const [isDark, setIsDark] = useState<boolean>(() => {
    if (typeof document !== 'undefined') {
      return document.documentElement.classList.contains('dark');
    }
    return true;
  });
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [selectedChapterId, setSelectedChapterId] = useState<string>('ch_phy9_01');
  const [mockRevisionTick, setMockRevisionTick] = useState(0);

  const refreshMock = () => setMockRevisionTick((n) => n + 1);

  const loadBootstrapCheck = useCallback(async () => {
    setAuthBusy(true);
    setAuthConnectionError(null);
    try {
      const status = await apiClient.getBootstrapStatus();
      setBootstrapStatus(status);
    } catch (err) {
      setBootstrapStatus(null);
      setAuthConnectionError(
        err instanceof Error ? err.message : 'Unable to reach Cloudflare Worker API.'
      );
    } finally {
      setAuthBusy(false);
    }
  }, [apiClient]);

  const loadLiveDashboard = useCallback(async () => {
    const [overview, policy, infra, capacity] = await Promise.all([
      apiClient.fetchOverview(),
      apiClient.fetchSettingsPolicy().catch(() => ({} as Partial<AdminSettingsState>)),
      apiClient.fetchInfrastructureStatus().catch(() => null),
      apiClient.fetchCapacityDashboard().catch(() => null),
    ]);
    setLiveDataset(overview);
    setLiveSettings((prev) => ({
      ...prev,
      ...policy,
      adPlacement: {
        ...prev.adPlacement,
        ...(policy.adPlacement ?? {}),
      },
      apiBaseUrl: apiClient.getBaseUrl(),
    }));
    if (infra) setInfraStatus(infra);
    if (capacity) setCapacityDashboard(capacity);
    if (overview.chapters.length > 0) {
      setSelectedChapterId((curr) =>
        overview.chapters.some((c) => c.id === curr) ? curr : overview.chapters[0].id
      );
    }
  }, [apiClient]);

  useEffect(() => {
    if (connectionMode !== 'live_api') return;
    if (session) {
      loadLiveDashboard().catch((err) => {
        // Expired or invalid session falls back cleanly to login gate
        if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) {
          persistAdminSession(null);
          apiClient.setToken(null);
          setSession(null);
          void loadBootstrapCheck();
        } else {
          setAuthConnectionError(err instanceof Error ? err.message : 'Failed to load dashboard.');
        }
      });
    } else {
      void loadBootstrapCheck();
    }
  }, [connectionMode, session, apiClient, loadBootstrapCheck, loadLiveDashboard]);

  useEffect(() => {
    if (connectionMode === 'live_api' && session && activeSection === 'capacity') {
      void apiClient
        .fetchCapacityDashboard()
        .then((res) => setCapacityDashboard(res))
        .catch(() => null);
    }
  }, [connectionMode, session, activeSection, apiClient]);

  const dataset =
    connectionMode === 'live_api' && liveDataset ? liveDataset : mockRepository.getDataset();
  const settings =
    connectionMode === 'live_api' ? liveSettings : mockRepository.getSettings();
  const effectiveCapacityDashboard = useMemo(() => {
    void mockRevisionTick;
    if (connectionMode === 'live_api' && capacityDashboard) {
      return capacityDashboard;
    }
    return mockRepository.getOfflineCapacityDashboard();
  }, [connectionMode, capacityDashboard, mockRevisionTick]);

  const pendingReviewCount = useMemo(
    () =>
      dataset.aiSubmissions.filter(
        (s) => s.status === 'queued' || s.status === 'in_review'
      ).length,
    [dataset.aiSubmissions]
  );

  const openFeedbackCount = useMemo(
    () =>
      (dataset.feedbackReports ?? []).filter(
        (f) => f.status === 'open' || f.status === 'triaged'
      ).length,
    [dataset.feedbackReports]
  );

  const activeChapterId = useMemo(
    () =>
      dataset.chapters.some((c) => c.id === selectedChapterId)
        ? selectedChapterId
        : (dataset.chapters[0]?.id ?? selectedChapterId),
    [dataset.chapters, selectedChapterId]
  );

  const toggleTheme = () => {
    const nextDark = !isDark;
    setIsDark(nextDark);
    if (typeof document !== 'undefined') {
      document.documentElement.classList.toggle('dark', nextDark);
    }
  };

  const handleBootstrap = async (params: {
    bootstrapSecret: string;
    email: string;
    displayName: string;
    password: string;
  }): Promise<string | null> => {
    setAuthBusy(true);
    try {
      const resp = await apiClient.bootstrapFirstAdmin(params);
      const nextSession: AdminSessionState = {
        token: resp.token,
        expiresAtIso: resp.expiresAtIso,
        admin: resp.admin,
      };
      apiClient.setToken(resp.token);
      persistAdminSession(nextSession);
      setSession(nextSession);
      return null;
    } catch (err) {
      return err instanceof Error ? err.message : 'Bootstrap failed.';
    } finally {
      setAuthBusy(false);
    }
  };

  const handleLogin = async (email: string, password: string): Promise<string | null> => {
    setAuthBusy(true);
    try {
      const resp = await apiClient.login({ email, password });
      const nextSession: AdminSessionState = {
        token: resp.token,
        expiresAtIso: resp.expiresAtIso,
        admin: resp.admin,
      };
      apiClient.setToken(resp.token);
      persistAdminSession(nextSession);
      setSession(nextSession);
      return null;
    } catch (err) {
      return err instanceof Error ? err.message : 'Login failed.';
    } finally {
      setAuthBusy(false);
    }
  };

  const handleSignOut = () => {
    persistAdminSession(null);
    apiClient.setToken(null);
    setSession(null);
    setLiveDataset(null);
  };

  if (connectionMode === 'live_api' && (!session || !liveDataset)) {
    return (
      <AdminAuthGate
        apiBaseUrl={apiClient.getBaseUrl()}
        bootstrapStatus={bootstrapStatus}
        connectionError={authConnectionError}
        busy={authBusy}
        onUpdateApiUrl={(url) => {
          apiClient.setBaseUrl(url);
          setLiveSettings((prev) => ({ ...prev, apiBaseUrl: url }));
          void loadBootstrapCheck();
        }}
        onRetryConnection={() => void loadBootstrapCheck()}
        onBootstrap={handleBootstrap}
        onLogin={handleLogin}
        onEnterOfflinePreview={() => setConnectionMode('offline_preview')}
      />
    );
  }

  const handleCreateSubject = async (input: Omit<ApiSubject, 'id' | 'updatedAtIso'>) => {
    if (connectionMode === 'live_api') {
      await apiClient.createSubject(input);
      await loadLiveDashboard();
    } else {
      mockRepository.createSubject(input);
      refreshMock();
    }
  };

  const handleDeleteSubject = async (subjectId: string) => {
    if (connectionMode === 'live_api') {
      await apiClient.deleteSubject(subjectId);
      await loadLiveDashboard();
    } else {
      const ds = mockRepository.getDataset();
      ds.subjects = ds.subjects.filter((s) => s.id !== subjectId);
      refreshMock();
    }
  };

  const handleCreateChapter = async (input: Omit<ApiChapter, 'id' | 'updatedAtIso'>) => {
    if (connectionMode === 'live_api') {
      await apiClient.createChapter(input);
      await loadLiveDashboard();
    } else {
      mockRepository.createChapter(input);
      refreshMock();
    }
  };

  const handleDeleteChapter = async (chapterId: string) => {
    if (connectionMode === 'live_api') {
      await apiClient.deleteChapter(chapterId);
      await loadLiveDashboard();
    } else {
      const ds = mockRepository.getDataset();
      ds.chapters = ds.chapters.filter((c) => c.id !== chapterId);
      refreshMock();
    }
  };

  const handleSaveBlocks = async (chapterId: string, blocks: EducationalContentBlock[]) => {
    if (connectionMode === 'live_api') {
      try {
        const saved = await apiClient.saveContentDraft(chapterId, blocks);
        await loadLiveDashboard();
        return { ok: true, errors: [], contentId: saved.id };
      } catch (err) {
        return {
          ok: false,
          errors: [err instanceof Error ? err.message : 'Failed to save draft.'],
        };
      }
    }
    const res = mockRepository.saveChapterBlocks(chapterId, blocks);
    refreshMock();
    return res;
  };

  const handlePublishContent = async (contentId: string) => {
    if (connectionMode === 'live_api') {
      try {
        const res = await apiClient.publishContentVersion(contentId);
        await loadLiveDashboard();
        return { ok: true, version: res.version };
      } catch (err) {
        return { ok: false, error: err instanceof Error ? err.message : 'Publish failed.' };
      }
    }
    const res = mockRepository.transitionContentStatus(contentId, 'published');
    refreshMock();
    return res.ok ? { ok: true, version: 'v1.5-preview' } : { ok: false, error: res.error };
  };

  const handleTransitionContentStatus = async (contentId: string, status: PublishStatus) => {
    if (connectionMode === 'live_api') {
      try {
        await apiClient.transitionContentStatus(contentId, status);
        await loadLiveDashboard();
        return { ok: true };
      } catch (err) {
        return { ok: false, error: err instanceof Error ? err.message : 'Status update failed.' };
      }
    }
    const res = mockRepository.transitionContentStatus(contentId, status);
    refreshMock();
    return res;
  };

  const handleCreateMcq = async (payload: AdminMcqWritePayload) => {
    if (connectionMode === 'live_api') {
      await apiClient.createMcq(payload);
      await loadLiveDashboard();
    } else {
      mockRepository.createMcq(payload);
      refreshMock();
    }
  };

  const handleDeleteMcq = async (mcqId: string) => {
    if (connectionMode === 'live_api') {
      await apiClient.deleteMcq(mcqId);
      await loadLiveDashboard();
    } else {
      mockRepository.deleteMcq(mcqId);
      refreshMock();
    }
  };

  const handleTriageFeedback = async (
    feedbackId: string,
    status: ApiFeedbackReport['status'],
    adminResolutionNote: string
  ) => {
    if (connectionMode === 'live_api') {
      await apiClient.triageFeedback(feedbackId, status, adminResolutionNote);
      await loadLiveDashboard();
    } else {
      mockRepository.triageFeedback(feedbackId, status, adminResolutionNote);
      refreshMock();
    }
  };

  const handleTransitionSubmission = async (
    submissionId: string,
    nextStatus: ApiAiAgentSubmission['status'],
    reviewerNotes: string
  ) => {
    if (connectionMode === 'live_api') {
      try {
        await apiClient.decideAiSubmission(submissionId, nextStatus, reviewerNotes);
        await loadLiveDashboard();
        return { ok: true };
      } catch (err) {
        return { ok: false, error: err instanceof Error ? err.message : 'Failed to update status.' };
      }
    }
    const res = mockRepository.transitionAiSubmission(submissionId, nextStatus, reviewerNotes);
    refreshMock();
    return res;
  };

  const handlePromoteSubmission = async (submissionId: string) => {
    try {
      await apiClient.promoteAiSubmission(submissionId);
      await loadLiveDashboard();
      return { ok: true };
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Promotion failed.' };
    }
  };

  const handleAddPdfPaper = async (
    paper: AdminPdfSubmitPayload,
    title: string,
    file?: File | null
  ) => {
    if (connectionMode === 'live_api') {
      if (!file) throw new Error('Select a PDF file before uploading.');
      await apiClient.uploadPdfFile({
        file,
        title,
        subjectId: paper.subjectId,
        year: paper.year,
        session: paper.session,
        isSolved: paper.isSolved,
        pageCount: paper.pageCount,
        category: paper.category,
      });
      await loadLiveDashboard();
    } else {
      const subject = dataset.subjects.find((s) => s.id === paper.subjectId) ?? dataset.subjects[0];
      if (!subject) throw new Error('Select a valid subject.');
      mockRepository.addCategorizedPdfRecord({
        category: paper.category,
        title,
        subject,
        year: paper.year,
        session: paper.session,
        isSolved: paper.isSolved,
        fileSizeMb: paper.fileSizeMb,
        pageCount: paper.pageCount,
      });
      refreshMock();
    }
  };

  const handleDeletePdf = async (pdfId: string) => {
    if (connectionMode === 'live_api') {
      await apiClient.deletePdf(pdfId);
      await loadLiveDashboard();
    } else {
      const ds = mockRepository.getDataset();
      ds.pdfs = ds.pdfs.filter((p) => p.id !== pdfId);
      ds.pastPapers = ds.pastPapers.filter((pp) => pp.pdfMetadataId !== pdfId);
      refreshMock();
    }
  };

  const handleSaveSettings = async (next: AdminSettingsState) => {
    if (connectionMode === 'live_api') {
      apiClient.setBaseUrl(next.apiBaseUrl);
      await apiClient.saveSettingsPolicy(next);
      setLiveSettings(next);
      await loadLiveDashboard();
    } else {
      mockRepository.saveSettings(next);
      refreshMock();
    }
  };

  const handleRefreshCapacity = async () => {
    if (connectionMode === 'live_api') {
      const fresh = await apiClient.fetchCapacityDashboard();
      setCapacityDashboard(fresh);
    } else {
      refreshMock();
    }
  };

  const handleSaveCapacityPolicy = async (policy: Partial<CapacityPolicyConfig>) => {
    if (connectionMode === 'live_api') {
      await apiClient.saveCapacityPolicy(policy);
      const fresh = await apiClient.fetchCapacityDashboard();
      setCapacityDashboard(fresh);
    } else {
      mockRepository.saveOfflineCapacityPolicy(policy);
      refreshMock();
    }
  };

  return (
    <div className="min-h-screen flex bg-[#F4F7FC] text-[#0F172A] dark:bg-[#060E21] dark:text-[#F0F6FF]">
      <Sidebar
        activeSection={activeSection}
        onSelectSection={setActiveSection}
        pendingReviewCount={pendingReviewCount}
        openFeedbackCount={openFeedbackCount}
        mobileOpen={mobileMenuOpen}
        onCloseMobile={() => setMobileMenuOpen(false)}
        connectionMode={connectionMode}
        session={session}
        onSignOut={handleSignOut}
        onSwitchToLiveAuth={() => {
          setConnectionMode('live_api');
          void loadBootstrapCheck();
        }}
      />

      <div className="flex-1 min-w-0 flex flex-col">
        <TopBar
          activeSection={activeSection}
          isDark={isDark}
          onToggleTheme={toggleTheme}
          onOpenMobileMenu={() => setMobileMenuOpen(true)}
          onQuickAction={() => setActiveSection('content-studio')}
        />

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-[1440px] w-full mx-auto">
          {activeSection === 'overview' && (
            <OverviewPage
              dataset={dataset}
              connectionMode={connectionMode}
              onNavigate={setActiveSection}
            />
          )}

          {(activeSection === 'academic' ||
            activeSection === 'classes' ||
            activeSection === 'subjects' ||
            activeSection === 'chapters') && (
            <AcademicManagementPages
              mode={activeSection}
              dataset={dataset}
              connectionMode={connectionMode}
              onCreateSubject={handleCreateSubject}
              onDeleteSubject={handleDeleteSubject}
              onCreateChapter={handleCreateChapter}
              onDeleteChapter={handleDeleteChapter}
              onOpenStudioForChapter={(chapterId) => {
                setSelectedChapterId(chapterId);
                setActiveSection('content-studio');
              }}
            />
          )}

          {activeSection === 'content-studio' && (
            <ContentStudioPage
              dataset={dataset}
              connectionMode={connectionMode}
              selectedChapterId={activeChapterId}
              onSelectChapter={setSelectedChapterId}
              onSaveBlocks={handleSaveBlocks}
              onTransitionContentStatus={handleTransitionContentStatus}
              onPublishContent={handlePublishContent}
              onCreateMcq={handleCreateMcq}
              onDeleteMcq={handleDeleteMcq}
            />
          )}

          {(activeSection === 'ai-submissions' || activeSection === 'review-queue') && (
            <SubmissionsAndQueuePages
              mode={activeSection}
              dataset={dataset}
              connectionMode={connectionMode}
              onTransition={handleTransitionSubmission}
              onPromoteApproved={handlePromoteSubmission}
            />
          )}

          {(activeSection === 'pdf-library' || activeSection === 'students') && (
            <LibraryAndStudentsPages
              mode={activeSection}
              dataset={dataset}
              connectionMode={connectionMode}
              onAddPdfPaper={handleAddPdfPaper}
              onDeletePdf={handleDeletePdf}
            />
          )}

          {activeSection === 'feedback' && (
            <FeedbackTriagePage
              dataset={dataset}
              connectionMode={connectionMode}
              onTriageFeedback={handleTriageFeedback}
            />
          )}

          {(activeSection === 'analytics' ||
            activeSection === 'infrastructure' ||
            activeSection === 'settings') && (
            <AnalyticsInfraSettingsPages
              mode={activeSection}
              dataset={dataset}
              connectionMode={connectionMode}
              settings={settings}
              infraStatus={infraStatus}
              onRefreshInfra={async () => {
                const res = await apiClient.fetchInfrastructureStatus();
                setInfraStatus(res);
              }}
              onSaveSettings={handleSaveSettings}
            />
          )}

          {activeSection === 'capacity' && (
            <CapacityDashboardPage
              dashboard={effectiveCapacityDashboard}
              connectionMode={connectionMode}
              onRefresh={handleRefreshCapacity}
              onSavePolicy={handleSaveCapacityPolicy}
            />
          )}
        </main>
      </div>
    </div>
  );
}
