/**
 * Imtihan Pro — Configurable Stage 2 Cloudflare API Endpoint Map
 * Base URL is injected via configuration/DataStore/env; never hardcoded with secrets.
 */

export const DEFAULT_API_CONFIG = {
  baseUrl: 'http://localhost:8787/v1',
  timeoutMs: 15000,
  stage: 'STAGE_2_CLOUDFLARE_WORKER' as const,
};

export const API_ENDPOINTS = {
  // Public Student Content & Discovery
  health: '/health',
  config: '/config',
  boards: '/boards',
  studyGroups: '/study-groups',
  classes: '/classes',
  subjects: '/subjects',
  search: '/search',
  chaptersBySubject: (subjectId: string) => `/subjects/${encodeURIComponent(subjectId)}/chapters`,
  chapterContent: (chapterId: string, mode: string) =>
    `/chapters/${encodeURIComponent(chapterId)}/content?mode=${encodeURIComponent(mode)}`,
  chapterMcqs: (chapterId: string) => `/chapters/${encodeURIComponent(chapterId)}/mcqs`,
  submitQuiz: '/quizzes/submissions',
  examPatterns: '/exams/patterns',
  examStart: '/exams/start',
  quizHistory: '/students/me/quizzes',
  pastPapers: '/past-papers',
  pdfs: '/pdfs',
  pdfMetadata: (pdfId: string) => `/pdfs/${encodeURIComponent(pdfId)}/metadata`,
  pdfDownloadUrl: (pdfId: string) => `/pdfs/${encodeURIComponent(pdfId)}/download`,

  // Student auth, progress & smart learning sync
  authGuest: '/auth/guest',
  authGoogleExchange: '/auth/google/exchange',
  authSession: '/auth/session',
  studentProgress: '/students/me/progress',
  syncGuestProgress: '/students/me/progress/sync',
  studentBookmarks: '/students/me/bookmarks',
  studentBookmarkDelete: (id: string) => `/students/me/bookmarks/${encodeURIComponent(id)}`,
  studentMistakes: '/students/me/mistakes',
  studentMistakeResolve: (mcqId: string) => `/students/me/mistakes/${encodeURIComponent(mcqId)}/resolve`,
  studentFlashcards: '/students/me/flashcards',
  studentFlashcardReview: '/students/me/flashcards/review',
  studentInsights: '/students/me/insights',
  studentPlanner: '/students/me/planner',
  studentPlannerTasks: '/students/me/planner/tasks',
  studentPlannerTaskById: (taskId: string) => `/students/me/planner/tasks/${encodeURIComponent(taskId)}`,
  studentMotivation: '/students/me/motivation',
  studentLeaderboardOptIn: '/students/me/leaderboard-opt-in',
  studentFeedback: '/feedback',

  // AI Tutor (server-side grounded provider)
  tutorAsk: '/tutor/ask',

  // AI Agent restricted ingestion
  agentSubmissions: '/agent/submissions',

  // Admin auth & management
  adminBootstrapStatus: '/admin/bootstrap-status',
  adminBootstrap: '/admin/bootstrap',
  adminLogin: '/admin/login',
  adminLogout: '/admin/logout',
  adminOverview: '/admin/overview',
  adminSubjects: '/admin/subjects',
  adminSubjectById: (subjectId: string) => `/admin/subjects/${encodeURIComponent(subjectId)}`,
  adminChapters: '/admin/chapters',
  adminChapterById: (chapterId: string) => `/admin/chapters/${encodeURIComponent(chapterId)}`,
  adminMcqs: '/admin/mcqs',
  adminMcqById: (mcqId: string) => `/admin/mcqs/${encodeURIComponent(mcqId)}`,
  adminAiSubmissions: '/admin/ai-submissions',
  adminReviewDecision: (submissionId: string) =>
    `/admin/ai-submissions/${encodeURIComponent(submissionId)}/decision`,
  adminPromoteSubmission: (submissionId: string) =>
    `/admin/ai-submissions/${encodeURIComponent(submissionId)}/promote`,
  adminContentDraft: '/admin/content/drafts',
  adminContentStatus: (contentId: string) =>
    `/admin/content/${encodeURIComponent(contentId)}/status`,
  adminPublishVersion: (contentId: string) =>
    `/admin/content/${encodeURIComponent(contentId)}/publish`,
  adminPdfs: '/admin/pdfs',
  adminPdfById: (pdfId: string) => `/admin/pdfs/${encodeURIComponent(pdfId)}`,
  adminStudents: '/admin/students',
  adminAnalytics: '/admin/analytics',
  adminFeedback: '/admin/feedback',
  adminFeedbackById: (feedbackId: string) => `/admin/feedback/${encodeURIComponent(feedbackId)}`,
  adminInfrastructure: '/admin/infrastructure',
  adminCapacity: '/admin/capacity',
  adminCapacityPolicy: '/admin/capacity-policy',
  adminSettings: '/admin/settings',
  adminUsers: '/admin/users',
  adminUserRole: (adminId: string) => `/admin/users/${encodeURIComponent(adminId)}/role`,
} as const;
