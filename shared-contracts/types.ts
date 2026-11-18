/**
 * Imtihan Pro — Shared Stage 2 Cloudflare API Contracts
 * Pure TypeScript definitions shared between Backend Worker (imtihan-pro-api),
 * Admin Dashboard, Documentation, and mirrored in Android Kotlin ApiModels.kt.
 *
 * Infrastructure targets:
 * - Cloudflare Worker: imtihan-pro-api
 * - Cloudflare D1: imtihan-pro-db
 * - Backblaze B2 Bucket: imtihan-pro-assets (provider-neutral objectKey in contracts)
 */

export type ClassLevel = 9 | 10 | 11 | 12;

export type BoardCode = 'KPK' | 'FEDERAL' | 'PUNJAB' | 'SINDH' | 'BALOCHISTAN';

export type KpkSubBoardCode =
  | 'PESHAWAR'
  | 'MARDAN'
  | 'SWAT'
  | 'ABBOTTABAD'
  | 'KOHAT'
  | 'BANNU'
  | 'DI_KHAN'
  | 'MALAKAND';

export type StudyGroupCode =
  | 'SCIENCE_BIOLOGY'
  | 'SCIENCE_CS'
  | 'ARTS_HUMANITIES'
  | 'PRE_MEDICAL'
  | 'PRE_ENGINEERING'
  | 'ICS'
  | 'FA'
  | 'ICOM';

export type LanguageCode = 'en' | 'ur';

export type DifficultyLevel = 'easy' | 'medium' | 'hard' | 'mixed';

export type ExamModeType =
  | 'chapter_test'
  | 'topic_test'
  | 'subject_test'
  | 'full_mock_exam'
  | 'diagnostic_test';

export type ContentCoverageStatus = 'active_seed' | 'structure_ready' | 'planned';

export type PublishStatus = 'draft' | 'in_review' | 'approved' | 'published' | 'rejected';

export type SubjectIconKey =
  | 'physics'
  | 'chemistry'
  | 'math'
  | 'biology'
  | 'english'
  | 'urdu'
  | 'cs'
  | 'islamiat';

export interface ApiKpkSubBoard {
  code: KpkSubBoardCode;
  name: string;
  headquarters: string;
}

export interface ApiStudyGroupOption {
  code: StudyGroupCode;
  label: string;
  applicableClasses: ClassLevel[];
  description: string;
}

export interface ApiBoard {
  id: string;
  code: BoardCode;
  name: string;
  region: string;
  isPrimaryInitialFocus: boolean;
  isActive: boolean;
  supportedClasses: ClassLevel[];
  subBoards?: ApiKpkSubBoard[];
}

export interface ApiAcademicClass {
  id: string;
  level: ClassLevel;
  label: string;
  boardId: string;
  boardCode: BoardCode;
  academicYear: string;
  coverageStatus: ContentCoverageStatus;
  subjectCount: number;
  publishedChaptersCount: number;
  notes: string;
}

export interface ApiSubject {
  id: string;
  classId: string;
  classLevel: ClassLevel;
  boardCode: BoardCode;
  code: string;
  name: string;
  iconKey: SubjectIconKey;
  accentHex: string;
  totalChapters: number;
  publishedChapters: number;
  isOfflinePackAvailable: boolean;
  coverageStatus: ContentCoverageStatus;
  updatedAtIso: string;
  studyGroups?: StudyGroupCode[];
  description?: string;
  textbookPdfId?: string | null;
  pairingSchemeSummary?: string;
  offlinePackSizeMb?: number;
}

export interface ApiTopic {
  id: string;
  chapterId: string;
  orderIndex: number;
  title: string;
  estimatedMinutes: number;
}

export interface ApiChapter {
  id: string;
  subjectId: string;
  chapterNumber: number;
  title: string;
  subtitle: string;
  topics: ApiTopic[];
  currentVersion: string;
  publishStatus: PublishStatus;
  hasDetailedNotes: boolean;
  hasRevisionNotes: boolean;
  mcqCount: number;
  solvedQuestionsCount: number;
  isLockedByDefault: boolean;
  estimatedSizeKb: number;
  updatedAtIso: string;
  boardCode?: BoardCode;
  classLevel?: ClassLevel;
  curriculumVersion?: string;
  difficultyTag?: 'easy' | 'medium' | 'hard';
  sourceReference?: string;
  reviewedByName?: string | null;
  titleUrdu?: string;
  subtitleUrdu?: string;
  boardWeightageMarks?: number;
  pairingHint?: string;
  nightBeforeSummary?: string;
}

export type ContentBlockType =
  | 'heading'
  | 'subheading'
  | 'paragraph'
  | 'math_expression'
  | 'table'
  | 'list'
  | 'definition'
  | 'example'
  | 'diagram'
  | 'exam_point'
  | 'mcq'
  | 'solved_question';

export interface ContentBlockBase {
  id: string;
  orderIndex: number;
  type: ContentBlockType;
  topicId?: string;
}

export interface HeadingBlock extends ContentBlockBase {
  type: 'heading' | 'subheading';
  text: string;
  sectionCode?: string;
}

export interface ParagraphBlock extends ContentBlockBase {
  type: 'paragraph';
  text: string;
}

export interface MathExpressionBlock extends ContentBlockBase {
  type: 'math_expression';
  latex: string;
  plainNotation: string;
  caption?: string;
}

export interface TableBlock extends ContentBlockBase {
  type: 'table';
  title?: string;
  headers: string[];
  rows: string[][];
}

export interface ListBlock extends ContentBlockBase {
  type: 'list';
  ordered: boolean;
  title?: string;
  items: string[];
}

export interface DefinitionBlock extends ContentBlockBase {
  type: 'definition';
  term: string;
  definition: string;
  formulaHint?: string;
}

export interface ExampleBlock extends ContentBlockBase {
  type: 'example';
  title: string;
  problemStatement: string;
  steps: string[];
  finalAnswer: string;
}

export interface DiagramBlock extends ContentBlockBase {
  type: 'diagram';
  title: string;
  caption: string;
  assetKey: string;
  altText: string;
  aspectRatio: '16:9' | '4:3' | '1:1';
}

export interface ExamPointBlock extends ContentBlockBase {
  type: 'exam_point';
  title: string;
  points: string[];
  boardFrequency?: 'High' | 'Medium' | 'Standard';
}

export interface McqOption {
  id: 'A' | 'B' | 'C' | 'D';
  text: string;
}

export interface McqBlock extends ContentBlockBase {
  type: 'mcq';
  question: string;
  options: McqOption[];
  correctOptionId: 'A' | 'B' | 'C' | 'D';
  explanation: string;
  difficulty: 'easy' | 'medium' | 'hard';
}

export interface SolvedQuestionBlock extends ContentBlockBase {
  type: 'solved_question';
  questionType: 'short' | 'long' | 'numerical';
  marks: number;
  question: string;
  answerSteps: string[];
  yearReference?: string;
}

export type EducationalContentBlock =
  | HeadingBlock
  | ParagraphBlock
  | MathExpressionBlock
  | TableBlock
  | ListBlock
  | DefinitionBlock
  | ExampleBlock
  | DiagramBlock
  | ExamPointBlock
  | McqBlock
  | SolvedQuestionBlock;

export interface ApiEducationalContent {
  id: string;
  chapterId: string;
  mode: 'detailed_notes' | 'revision_notes' | 'solved_questions';
  version: string;
  publishStatus: PublishStatus;
  language: LanguageCode;
  blocks: EducationalContentBlock[];
  authorOrSource: string;
  updatedAtIso: string;
}

export interface ApiMcqItem {
  id: string;
  chapterId: string;
  subjectId: string;
  classLevel: ClassLevel;
  question: string;
  options: McqOption[];
  correctOptionId: 'A' | 'B' | 'C' | 'D';
  explanation: string;
  difficulty: 'easy' | 'medium' | 'hard';
  topicTitle: string;
  pastPaperYear?: number;
  publishStatus?: PublishStatus;
  sourceReference?: string;
  questionUrdu?: string;
  optionsUrdu?: McqOption[];
  explanationUrdu?: string;
}

export interface ApiQuizSubmissionRequest {
  sessionId: string;
  studentId: string | null; // null in guest-first mode
  guestDeviceId: string;
  subjectId: string;
  chapterId: string | null;
  answers: Array<{
    mcqId: string;
    selectedOptionId: 'A' | 'B' | 'C' | 'D' | null;
    markedForReview: boolean;
  }>;
  elapsedSeconds: number;
  submittedAtIso: string;
  examMode?: ExamModeType;
  marksPerMcq?: number;
  negativeMarkingPenalty?: number;
  passPercentage?: number;
  patternId?: string | null;
  topicTitle?: string | null;
}

export interface ApiQuizResultResponse {
  submissionId: string;
  totalQuestions: number;
  correctCount: number;
  incorrectCount: number;
  unansweredCount: number;
  percentage: number;
  elapsedSeconds: number;
  perQuestionFeedback: Array<{
    mcqId: string;
    isCorrect: boolean;
    correctOptionId: 'A' | 'B' | 'C' | 'D';
    explanation: string;
  }>;
  marksObtained?: number;
  totalMarks?: number;
  passed?: boolean;
  passPercentage?: number;
  xpEarned?: number;
  topicBreakdown?: Array<{
    topicTitle: string;
    correct: number;
    total: number;
    accuracyPercent: number;
  }>;
}

export interface ApiExamPattern {
  id: string;
  title: string;
  mode: ExamModeType;
  classLevel: ClassLevel;
  boardCode: BoardCode;
  subjectId: string | null;
  questionCount: number;
  durationMinutes: number;
  marksPerMcq: number;
  negativeMarkingPenalty: number;
  passPercentage: number;
  includesShortQuestionsCount: number;
  includesLongQuestionsCount: number;
  sectionSummary: string;
}

export interface ApiExamStartRequest {
  patternId?: string;
  mode: ExamModeType;
  boardCode: BoardCode;
  classLevel: ClassLevel;
  subjectId?: string | null;
  chapterId?: string | null;
  topicTitle?: string | null;
  difficulty?: DifficultyLevel;
  questionCount?: number;
  durationMinutes?: number;
  marksPerMcq?: number;
  negativeMarkingPenalty?: number;
}

export interface ApiSubjectivePrompt {
  id: string;
  questionType: 'short' | 'long' | 'numerical';
  marks: number;
  question: string;
  answerSteps: string[];
  yearReference?: string;
}

export interface ApiExamSessionPayload {
  sessionId: string;
  mode: ExamModeType;
  title: string;
  subjectId: string;
  chapterId: string | null;
  durationMinutes: number;
  marksPerMcq: number;
  negativeMarkingPenalty: number;
  passPercentage: number;
  mcqs: ApiMcqItem[];
  subjectivePrompts: ApiSubjectivePrompt[];
}

export interface ApiQuizHistoryItem {
  id: string;
  sessionId: string;
  subjectId: string;
  subjectName: string;
  chapterId: string | null;
  chapterTitle: string | null;
  examMode: ExamModeType;
  totalQuestions: number;
  correctCount: number;
  incorrectCount: number;
  unansweredCount: number;
  percentage: number;
  marksObtained: number;
  totalMarks: number;
  passed: boolean;
  elapsedSeconds: number;
  submittedAtIso: string;
}

export interface ApiPastPaper {
  id: string;
  boardCode: BoardCode;
  classLevel: ClassLevel;
  subjectId: string;
  subjectName: string;
  year: number;
  session: 'Annual' | 'Supplementary';
  isSolved: boolean;
  pdfMetadataId: string;
  fileSizeMb: number;
  pageCount: number;
  downloadCount: number;
}

export interface ApiPdfMetadata {
  id: string;
  title: string;
  category: 'past_paper' | 'notes_pack' | 'syllabus' | 'textbook';
  classLevel: ClassLevel;
  boardCode: BoardCode;
  subjectId: string;
  objectKey: string;
  fileSizeBytes: number;
  pageCount: number;
  checksumSha256: string;
  offlineCacheable: boolean;
  version: string;
}

export interface ApiChapterProgressItem {
  chapterId: string;
  subjectId?: string;
  completionPercent: number;
  lastBlockIndex: number;
  isCompleted?: boolean;
  lastAccessedAtIso: string;
}

export interface ApiStudentProgress {
  localProfileId: string;
  googleUid: string | null;
  isGuestMode: boolean;
  selectedClass: ClassLevel;
  selectedBoard: BoardCode;
  selectedSubBoard?: KpkSubBoardCode | null;
  studyGroup?: StudyGroupCode | null;
  language?: LanguageCode;
  studyHoursTotal: number;
  chaptersCompletedCount: number;
  overallProgressPercent: number;
  dailyGoalPercent: number;
  dailyTargetMinutes?: number;
  examCountdownDateIso?: string | null;
  diagnosticCompleted?: boolean;
  xpTotal?: number;
  currentStreakDays?: number;
  chapterProgress: ApiChapterProgressItem[];
  lastSyncedAtIso: string | null;
}

export interface ApiAuthSession {
  mode: 'guest' | 'google_authenticated';
  guestDeviceId: string;
  token?: string;
  user: {
    id: string;
    displayName: string;
    email: string | null;
    avatarUrl: string | null;
    role: 'student' | 'reviewer' | 'admin' | 'super_admin';
  } | null;
  accessTokenExpiresAtIso: string | null;
}

export interface StudentGuestAuthRequest {
  guestDeviceId: string;
  displayName?: string;
  selectedClass?: ClassLevel;
  selectedBoard?: BoardCode;
  selectedSubBoard?: KpkSubBoardCode | null;
  studyGroup?: StudyGroupCode | null;
  language?: LanguageCode;
}

export interface StudentGoogleExchangeRequest {
  idToken: string;
  guestDeviceId: string;
  selectedClass?: ClassLevel;
  selectedBoard?: BoardCode;
  selectedSubBoard?: KpkSubBoardCode | null;
  studyGroup?: StudyGroupCode | null;
  language?: LanguageCode;
}

export interface StudentProgressSyncRequest {
  guestDeviceId: string;
  selectedClass: ClassLevel;
  selectedBoard: BoardCode;
  selectedSubBoard?: KpkSubBoardCode | null;
  studyGroup?: StudyGroupCode | null;
  language?: LanguageCode;
  studyHoursTotal: number;
  dailyGoalPercent: number;
  dailyTargetMinutes?: number;
  examCountdownDateIso?: string | null;
  diagnosticCompleted?: boolean;
  chapterProgress: ApiChapterProgressItem[];
  clientSyncedAtIso: string;
}

export interface ApiSearchResults {
  query: string;
  subjects: ApiSubject[];
  chapters: ApiChapter[];
  topics: Array<ApiTopic & { subjectId: string; chapterTitle: string }>;
  mcqs: ApiMcqItem[];
  pdfs: ApiPdfMetadata[];
}

export interface ApiStudentBookmark {
  id: string;
  itemType: 'chapter' | 'note_block' | 'mcq' | 'solved_question' | 'pdf';
  itemId: string;
  chapterId?: string | null;
  subjectId?: string | null;
  title: string;
  subtitle?: string;
  createdAtIso: string;
}

export interface ApiMistakeRecord {
  id: string;
  mcqId: string;
  subjectId: string;
  chapterId: string;
  topicTitle: string;
  question: string;
  lastWrongOptionId: 'A' | 'B' | 'C' | 'D' | null;
  correctOptionId: 'A' | 'B' | 'C' | 'D';
  wrongCount: number;
  mastered: boolean;
  lastAttemptedAtIso: string;
  mcq?: ApiMcqItem;
}

export interface ApiFlashcard {
  id: string;
  subjectId: string;
  chapterId: string;
  front: string;
  back: string;
  formulaHint?: string;
  topicTitle: string;
  intervalDays: number;
  easeFactor: number;
  repetitions: number;
  nextReviewAtIso: string;
  lastReviewedAtIso: string | null;
  frontUrdu?: string;
  backUrdu?: string;
}

export interface ApiFlashcardReviewRequest {
  flashcardId: string;
  quality: 0 | 1 | 2 | 3 | 4 | 5;
  reviewedAtIso?: string;
}

export interface ApiWeaknessItem {
  chapterId: string;
  chapterTitle: string;
  subjectId: string;
  subjectName: string;
  accuracyPercent: number | null;
  attemptsCount: number;
  completionPercent: number;
  reason: string;
  priority: 'high' | 'medium' | 'watch';
}

export interface ApiRevisionRecommendation {
  id: string;
  chapterId: string;
  subjectId: string;
  title: string;
  reason: string;
  actionType: 'review_mistakes' | 'revise_notes' | 'flashcards' | 'take_test';
}

export interface ApiSmartInsightsResponse {
  readinessPercent: number | null;
  readinessStatus: 'insufficient_data' | 'building' | 'on_track' | 'exam_ready';
  readinessMessage: string;
  diagnosticCompleted: boolean;
  overallAccuracyPercent: number | null;
  totalQuizAttempts: number;
  masteredChaptersCount: number;
  weakChapters: ApiWeaknessItem[];
  subjectAnalytics: Array<{
    subjectId: string;
    subjectName: string;
    masteryPercent: number | null;
    quizAveragePercent: number | null;
    chaptersCompleted: number;
    totalChapters: number;
    attemptsCount: number;
  }>;
  revisionRecommendations: ApiRevisionRecommendation[];
  dueFlashcardsCount: number;
  activeMistakesCount: number;
}

export interface ApiTutorRequest {
  chapterId?: string | null;
  subjectId?: string | null;
  classLevel: ClassLevel;
  boardCode: BoardCode;
  language: LanguageCode;
  action: 'ask' | 'explain_simple' | 'step_by_step' | 'important_questions' | 'quick_summary';
  prompt: string;
}

export interface ApiTutorGroundedSource {
  type: 'chapter_note' | 'definition' | 'formula' | 'syllabus';
  title: string;
  referenceId: string;
}

export interface ApiTutorResponse {
  available: boolean;
  provider: string;
  model: string | null;
  answer: string | null;
  groundedSources: ApiTutorGroundedSource[];
  unavailableReason?: string;
  generatedAtIso: string;
}

export interface ApiStudyPlanTask {
  id: string;
  title: string;
  subjectId: string | null;
  chapterId: string | null;
  plannedDateIso: string;
  estimatedMinutes: number;
  taskType: 'study' | 'revision' | 'quiz' | 'past_paper' | 'flashcards';
  completed: boolean;
  completedAtIso: string | null;
}

export interface ApiStudyPlannerState {
  dailyTargetMinutes: number;
  examCountdownDateIso: string | null;
  remindersEnabled: boolean;
  reminderHourLocal: number;
  currentStreakDays: number;
  longestStreakDays: number;
  minutesLoggedToday: number;
  tasks: ApiStudyPlanTask[];
}

export interface ApiAchievement {
  id: string;
  code: string;
  title: string;
  description: string;
  xpReward: number;
  unlocked: boolean;
  unlockedAtIso: string | null;
}

export interface ApiChallenge {
  id: string;
  title: string;
  description: string;
  cadence: 'daily' | 'weekly';
  targetCount: number;
  currentCount: number;
  xpReward: number;
  completed: boolean;
  expiresAtIso: string;
}

export interface ApiLeaderboardEntry {
  rank: number;
  aliasHandle: string;
  boardCode: BoardCode;
  classLevel: ClassLevel;
  xpTotal: number;
  streakDays: number;
  isCurrentUser: boolean;
}

export interface ApiMotivationSummary {
  xpTotal: number;
  level: number;
  nextLevelXp: number;
  currentStreakDays: number;
  leaderboardOptIn: boolean;
  aliasHandle: string | null;
  achievements: ApiAchievement[];
  challenges: ApiChallenge[];
  leaderboard: ApiLeaderboardEntry[];
}

export interface ApiFeedbackReport {
  id: string;
  studentId?: string | null;
  guestDeviceId?: string;
  category: 'content_error' | 'mcq_answer_dispute' | 'app_bug' | 'feature_suggestion';
  subjectId?: string | null;
  chapterId?: string | null;
  mcqId?: string | null;
  title: string;
  description: string;
  status: 'open' | 'triaged' | 'resolved' | 'dismissed';
  adminResolutionNote?: string;
  createdAtIso: string;
  updatedAtIso: string;
}

export interface ApiAdPlacementConfig {
  enabled: boolean;
  bannerOnHome: boolean;
  bannerOnPapers: boolean;
  suppressDuringQuizAndExam: boolean;
  suppressDuringNotesReading: boolean;
  dailyBannerCap: number;
  providerNote: string;
}

export interface ApiPublicRuntimeConfig {
  defaultBoardCode: BoardCode;
  defaultClassLevel: ClassLevel;
  kpkSubBoards: ApiKpkSubBoard[];
  studyGroups: ApiStudyGroupOption[];
  adPlacement: ApiAdPlacementConfig;
  aiTutorConfigured: boolean;
}

export interface AdminUserProfile {
  id: string;
  email: string;
  displayName: string;
  role: 'reviewer' | 'admin' | 'super_admin';
  createdAtIso: string;
  lastLoginAtIso: string | null;
}

export interface AdminBootstrapStatusResponse {
  hasAdmin: boolean;
  bootstrapConfigured: boolean;
}

export interface AdminBootstrapRequest {
  bootstrapSecret: string;
  email: string;
  displayName: string;
  password: string;
}

export interface AdminLoginRequest {
  email: string;
  password: string;
}

export interface AdminAuthResponse {
  token: string;
  expiresAtIso: string;
  admin: AdminUserProfile;
}

export interface ApiAiAgentSubmission {
  id: string;
  agentName: string;
  modelIdentifier: string;
  targetClass: ClassLevel;
  boardCode: BoardCode;
  subjectName: string;
  chapterId?: string | null;
  chapterTitle: string;
  submissionType: 'detailed_notes' | 'revision_notes' | 'mcq_batch' | 'solved_questions';
  blocksCount: number;
  schemaValidationPassed: boolean;
  validationWarnings: string[];
  status: 'queued' | 'in_review' | 'approved' | 'revision_requested' | 'rejected';
  submittedAtIso: string;
  reviewerNotes: string;
  blocks?: EducationalContentBlock[];
  mcqs?: Array<{
    question: string;
    options: McqOption[];
    correctOptionId: 'A' | 'B' | 'C' | 'D';
    explanation: string;
    difficulty: 'easy' | 'medium' | 'hard';
    topicTitle?: string;
  }>;
}

export interface AgentIngestSubmissionRequest {
  agentName: string;
  modelIdentifier: string;
  targetClass: ClassLevel;
  boardCode: BoardCode;
  subjectId: string;
  chapterId: string | null;
  chapterTitle: string;
  submissionType: 'detailed_notes' | 'revision_notes' | 'mcq_batch' | 'solved_questions';
  language?: LanguageCode;
  blocks?: EducationalContentBlock[];
  mcqs?: Array<{
    question: string;
    options: McqOption[];
    correctOptionId: 'A' | 'B' | 'C' | 'D';
    explanation: string;
    difficulty: 'easy' | 'medium' | 'hard';
    topicTitle?: string;
  }>;
  pdfMetadata?: {
    title: string;
    year: number;
    session: 'Annual' | 'Supplementary';
    isSolved: boolean;
    objectKey: string;
    pageCount: number;
  };
}

export interface AgentIngestSubmissionResponse {
  submissionId: string;
  status: 'queued';
  blocksCount: number;
  schemaValidationPassed: boolean;
  validationWarnings: string[];
  duplicateDetected: boolean;
}

export interface ApiAdminAuditItem {
  id: string;
  actorName: string;
  action: string;
  targetEntity: string;
  environmentMode: 'stage_1_mock' | 'cloudflare_stage_2';
  timestampIso: string;
}

export interface AdminSubjectWritePayload {
  classId: string;
  classLevel: ClassLevel;
  boardCode: BoardCode;
  code: string;
  name: string;
  iconKey: SubjectIconKey;
  accentHex: string;
  totalChapters: number;
  publishedChapters: number;
  isOfflinePackAvailable: boolean;
  coverageStatus: ContentCoverageStatus;
  studyGroups?: StudyGroupCode[];
  description?: string;
}

export interface AdminChapterWritePayload {
  subjectId: string;
  chapterNumber: number;
  title: string;
  subtitle: string;
  topics: ApiTopic[];
  currentVersion: string;
  publishStatus: PublishStatus;
  hasDetailedNotes: boolean;
  hasRevisionNotes: boolean;
  mcqCount: number;
  solvedQuestionsCount: number;
  isLockedByDefault: boolean;
  estimatedSizeKb: number;
  curriculumVersion?: string;
  difficultyTag?: 'easy' | 'medium' | 'hard';
  sourceReference?: string;
  titleUrdu?: string;
  subtitleUrdu?: string;
  boardWeightageMarks?: number;
  pairingHint?: string;
  nightBeforeSummary?: string;
}

export interface AdminMcqWritePayload {
  chapterId: string;
  subjectId: string;
  classLevel: ClassLevel;
  question: string;
  options: McqOption[];
  correctOptionId: 'A' | 'B' | 'C' | 'D';
  explanation: string;
  difficulty: 'easy' | 'medium' | 'hard';
  topicTitle: string;
  pastPaperYear?: number | null;
  publishStatus?: PublishStatus;
  sourceReference?: string;
}

export interface AdminAnalyticsResponse {
  available: boolean;
  reason?: string;
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

export interface AdminInfrastructureStatusResponse {
  workerName: string;
  workerReachable: boolean;
  apiVersion: string;
  d1: {
    databaseName: string;
    databaseId: string;
    connected: boolean;
    latencyMs: number | null;
    tableCount: number | null;
  };
  b2: {
    bucketName: string;
    bucketId: string;
    endpoint: string;
    configured: boolean;
    reachable: boolean | null;
    message: string;
  };
  googleAuth: {
    clientIdConfigured: boolean;
  };
  agentIngest: {
    secretConfigured: boolean;
  };
  aiTutor?: {
    configured: boolean;
    provider: string;
  };
  checkedAtIso: string;
}

export interface SignedDownloadUrlResponse {
  pdfId: string;
  objectKey: string;
  downloadUrl: string;
  presignedB2Url: string | null;
  expiresInSeconds: number;
}

export interface ApiEnvelope<T> {
  ok: boolean;
  data: T;
  meta: {
    apiVersion: string;
    source: 'stage_1_mock' | 'cloudflare_stage_2';
    timestampIso: string;
  };
  error?: {
    code: string;
    message: string;
  };
}

export interface FreeTierAllowances {
  workersRequestsPerDay: number;
  workersCpuMsPerInvocation: number;
  workersMemoryMb: number;
  workersSubrequestsPerInvocation: number;
  d1RowsReadPerDay: number;
  d1RowsWrittenPerDay: number;
  d1MaxDatabaseSizeMb: number;
  d1AccountStorageGb: number;
  d1QueriesPerInvocation: number;
  b2FreeStorageGb: number;
  b2ConservativeDailyDownloadGb: number;
  b2ClassDConsiderationDailyCap: number;
  geminiConservativeFreeRpd: number;
  geminiConservativeFreeRpm: number;
}

export interface CapacityPolicyConfig {
  warningThresholdPct: number;
  highThresholdPct: number;
  criticalThresholdPct: number;
  aiTutorDailyGlobalCap: number;
  aiTutorDailyPerStudentCap: number;
  aiCacheTtlHours: number;
  b2DailyDownloadSoftCapMb: number;
  d1DailyReadSoftCap: number;
  d1DailyWriteSoftCap: number;
  workerDailyRequestSoftCap: number;
  autoDegradeAiAtCritical: boolean;
  autoBlockUncachedPdfAtCritical: boolean;
}

export interface UsageSnapshotMetrics {
  dayYmd: string;
  workerRequestsToday: number;
  edgeOrHttpCacheHitsToday: number;
  d1RowsReadEstToday: number;
  d1RowsWrittenEstToday: number;
  d1DatabaseSizeBytes: number;
  b2StorageBytes: number;
  b2DailyDownloadBytes: number;
  b2DailyDownloadCount: number;
  aiCallsToday: number;
  aiCacheHitsToday: number;
  aiBlockedQuotaToday: number;
  apiErrorCountToday: number;
  avgLatencyMs: number;
  telemetryNote: string;
}

export interface CapacityAlertItem {
  resource:
    | 'worker_requests'
    | 'd1_reads'
    | 'd1_writes'
    | 'd1_storage'
    | 'b2_storage'
    | 'b2_downloads'
    | 'ai_tutor';
  label: string;
  used: number;
  limit: number;
  unit: string;
  utilizationPct: number;
  severity: 'nominal' | 'warning' | 'high' | 'critical';
  mitigation: string;
}

export interface CapacityProjectionMetrics {
  workerRequestsPerDay: number;
  d1RowsReadPerDay: number;
  d1RowsWrittenPerDay: number;
  monthlyStorageGrowthMb: number;
  b2DailyDownloadGb: number;
  peakRequestsPerSecond: number;
  estimatedPeakConcurrentUsers: number;
  aiCallsPerDay: number;
}

export interface CapacityScaleResult {
  dailyActiveStudents: number;
  isTheoreticalEstimate: true;
  metrics: CapacityProjectionMetrics;
  utilizationPct: {
    workerRequests: number;
    d1Reads: number;
    d1Writes: number;
    b2Download: number;
    aiDaily: number;
  };
  status: 'within_free_tier' | 'near_limit' | 'exceeds_free_tier';
  bottlenecks: string[];
}

export interface AdminCapacityDashboardResponse {
  limits: FreeTierAllowances;
  policy: CapacityPolicyConfig;
  usage: UsageSnapshotMetrics;
  alerts: CapacityAlertItem[];
  remainingCapacity: {
    workerRequestsRemaining: number;
    d1RowsReadRemaining: number;
    d1RowsWrittenRemaining: number;
    d1StorageMbRemaining: number;
    b2StorageGbRemaining: number;
    b2DailyDownloadMbRemaining: number;
    aiCallsRemaining: number;
  };
  dauProjections: CapacityScaleResult[];
  safeFallbackActive: {
    aiTutorPausedByCap: boolean;
    pdfStreamingGuardedByCap: boolean;
  };
}

