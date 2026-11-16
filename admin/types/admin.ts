import type {
  AdminUserProfile,
  ApiAcademicClass,
  ApiAdPlacementConfig,
  ApiAdminAuditItem,
  ApiAiAgentSubmission,
  ApiBoard,
  ApiChapter,
  ApiEducationalContent,
  ApiFeedbackReport,
  ApiMcqItem,
  ApiPastPaper,
  ApiPdfMetadata,
  ApiSubject,
  BoardCode,
  ClassLevel,
} from '../../shared-contracts/types';

export type AdminSectionId =
  | 'overview'
  | 'academic'
  | 'classes'
  | 'subjects'
  | 'chapters'
  | 'content-studio'
  | 'ai-submissions'
  | 'review-queue'
  | 'pdf-library'
  | 'students'
  | 'feedback'
  | 'analytics'
  | 'infrastructure'
  | 'capacity'
  | 'settings';

export type AdminConnectionMode = 'live_api' | 'offline_preview';

export interface AdminStudentRecord {
  id: string;
  displayName: string;
  accountType: 'guest' | 'google_synced';
  classLevel: ClassLevel;
  boardCode: BoardCode;
  chaptersCompleted: number;
  quizAveragePercent: number;
  offlinePacksCount: number;
  lastActiveIso: string;
}

export interface AdminSettingsState {
  apiBaseUrl: string;
  guestFirstMandatory: boolean;
  allowOptionalGoogleSync: boolean;
  defaultBoardCode: BoardCode;
  googleOAuthClientIdPlaceholder: string;
  offlineMaxCacheMb: number;
  adPlacement: ApiAdPlacementConfig;
}

export interface AdminAnalyticsSnapshot {
  available?: boolean;
  subjectAccuracy: Array<{
    subjectName: string;
    classLevel: ClassLevel;
    averageAccuracyPercent: number | null;
    attemptsCount: number;
  }>;
  weeklyStudyDistribution: Array<{
    dayLabel: string;
    activeLearners: number;
    quizSubmissions: number;
  }>;
}

export interface AdminDataset {
  boards: ApiBoard[];
  classes: ApiAcademicClass[];
  subjects: ApiSubject[];
  chapters: ApiChapter[];
  contents: ApiEducationalContent[];
  mcqs?: ApiMcqItem[];
  aiSubmissions: ApiAiAgentSubmission[];
  pastPapers: ApiPastPaper[];
  pdfs: ApiPdfMetadata[];
  students: AdminStudentRecord[];
  auditLog: ApiAdminAuditItem[];
  analytics: AdminAnalyticsSnapshot;
  feedbackReports?: ApiFeedbackReport[];
}

export interface AdminSessionState {
  token: string;
  expiresAtIso: string;
  admin: AdminUserProfile;
}

export type ViewLoadState = 'ready' | 'loading' | 'error' | 'empty';
