# Imtihan Pro — API Contracts (`imtihan-pro-api`)

All endpoints target Cloudflare Worker `imtihan-pro-api` via configurable `API_BASE_URL` (`http://localhost:8787/v1` locally, or `https://imtihan-pro-api.<subdomain>.workers.dev/v1`). Persistence binds to Cloudflare D1 (`imtihan-pro-db`, ID `079128c1-203b-477b-8c4b-d6b34daf8bbe`) and Backblaze B2 (`imtihan-pro-assets`, bucket ID `1bdf9ec6cd50dbdfad180110`, endpoint `https://s3.eu-central-003.backblazeb2.com`).

All JSON responses follow `ApiEnvelope<T>` (`{ ok, data, meta, error? }`) defined in `shared-contracts/types.ts`.

---

## 1. Public Runtime Config & KPK Curriculum Discovery
- `GET /v1/health` -> Worker version, D1 connectivity & latency, Backblaze B2 configuration health (`objectStorageProvider: "backblaze-b2"`), and AI Tutor provider availability (`tutorConfigured`).
- `GET /v1/config` -> `ApiRuntimeConfig` (`adConfig` gating flags, default KPK Class 9 & BISE Peshawar metadata, feature toggles).
- `GET /v1/boards` -> `ApiBoard[]` (includes 8 KPK BISE sub-boards: Peshawar, Mardan, Swat, Abbottabad, Malakand, Kohat, Bannu, DI Khan).
- `GET /v1/study-groups?classLevel=9` -> `ApiStudyGroupOption[]` (`SCIENCE_BIOLOGY`, `SCIENCE_COMPUTER`, plus Class 11–12 Pre-Medical / Pre-Engineering / ICS / Humanities groups).
- `GET /v1/classes?board=KPK` -> `ApiAcademicClass[]` (Classes 9–12, marking Class 9 as primary starter class).
- `GET /v1/subjects?board=KPK&classLevel=9&studyGroup=SCIENCE_BIOLOGY` -> `ApiSubject[]` filtered cleanly by study group.
- `GET /v1/subjects/:subjectId/chapters` -> `ApiChapter[]` (includes `difficultyTag`, `curriculumVersion`, `sourceReference`, `reviewedByName`, `mcqCount`, `topicTags`).
- `GET /v1/chapters/:chapterId/content?mode=detailed_notes|revision_notes|solved_questions` -> `ApiEducationalContent` *(published versions only; returns 404 for unpublished drafts, including `worked_example` and `solved_question` blocks)*.
- `GET /v1/chapters/:chapterId/mcqs` -> `ApiMcqItem[]` *(published MCQs only)*.
- `GET /v1/search?q=<query>&board=KPK&classLevel=9&studyGroup=...` -> `ApiSearchResult[]` across subjects, chapters, notes/solved content, and PDFs.

## 2. Practice, Mock Exams & Grading
- `GET /v1/exams/patterns?board=KPK&classLevel=9` -> `ApiExamPattern[]` (KPK Board paper patterns with Section A MCQ count/marks/duration/negative marking + Section B/C subjective prompt templates).
- `POST /v1/exams/start` (`ApiStartExamRequest`: `{ boardCode, classLevel, mode: 'chapter_test'|'subject_mock'|'board_mock'|'diagnostic'|'mistake_retry', subjectId?, chapterId?, questionCount? }`) -> `ApiGeneratedExamPaper` (timed question set + pattern metadata).
- `POST /v1/quizzes/submissions` (`ApiQuizSubmissionRequest`: `{ chapterId?, subjectId?, examMode, patternId?, negativeMarkingPerWrong?, answers[], timeTakenSeconds }`) -> `ApiQuizResultResponse` *(grades marks with negative marking, pass/fail, XP awarded, topic accuracy breakdown; records wrong answers into `student_mistakes` and awards streak/achievements if authenticated)*.

## 3. Past Papers & Backblaze B2 PDF Library
- `GET /v1/past-papers?board=KPK&classLevel=9&isSolved=true|false` -> `ApiPastPaper[]`
- `GET /v1/pdfs?board=KPK&classLevel=9&category=past_paper|textbook|notes_pack|syllabus` -> `ApiPdfDirectoryItem[]`
- `GET /v1/pdfs/:pdfId/metadata` -> `ApiPdfMetadata` *(uses provider-neutral `objectKey` mapped to B2 `imtihan-pro-assets`)*
- `GET /v1/pdfs/:pdfId/download?mode=stream|presigned` -> Binary `application/pdf` stream from Backblaze B2 or JSON `SignedDownloadUrlResponse`

## 4. Guest-First Auth, Progress & Smart Learning (`/v1/students/me/*`)
- `POST /v1/auth/guest` (`StudentGuestAuthRequest` with `selectedSubBoard`, `studyGroup`, `language`) -> `ApiAuthSession`
- `POST /v1/auth/google/exchange` (`StudentGoogleExchangeRequest`) -> `ApiAuthSession` *(verifies ID token via Google `tokeninfo` + `GOOGLE_OAUTH_CLIENT_ID` and preserves/merges existing guest profile)*
- `GET /v1/auth/session` -> `ApiAuthSession`
- `GET /v1/students/me/progress` -> `ApiStudentProgress`
- `POST /v1/students/me/progress/sync` (`StudentProgressSyncRequest`) -> `ApiStudentProgress` *(monotonic progress + preferences merge)*
- `GET /v1/students/me/quizzes` -> `ApiQuizHistoryItem[]`
- `GET /v1/students/me/bookmarks` | `POST /v1/students/me/bookmarks` | `DELETE /v1/students/me/bookmarks/:id`
- `GET /v1/students/me/mistakes` -> `ApiStudentMistakeItem[]`
- `POST /v1/students/me/mistakes/:mcqId/resolve` -> marks mistake mastered
- `GET /v1/students/me/flashcards?chapterId=...` -> `ApiFlashcardReviewState[]`
- `POST /v1/students/me/flashcards/review` (`{ flashcardId, quality: 0..5 }`) -> SM-2 spaced repetition next review state
- `GET /v1/students/me/insights` -> `ApiStudentInsightsResponse` *(returns `readinessPercentage: null` and `insufficientDataReason` when fewer than 2 graded attempts exist)*
- `GET /v1/students/me/planner` | `PUT /v1/students/me/planner` (goal minutes & exam countdown date)
- `POST /v1/students/me/planner/tasks` | `PUT /v1/students/me/planner/tasks/:taskId` | `DELETE /v1/students/me/planner/tasks/:taskId`
- `GET /v1/students/me/motivation` -> XP, level, streak, challenges, achievements & anonymized opted-in leaderboard entries
- `POST /v1/students/me/leaderboard-opt-in` (`{ optIn: boolean, aliasHandle?: string }`)
- `POST /v1/feedback` (`ApiFeedbackRequest`) -> student issue/mistake report linked to chapter/MCQ/question

## 5. Server-Side AI Tutor
- `POST /v1/tutor/ask` (`ApiAiTutorRequest`: `{ boardCode, classLevel, subjectId?, chapterId?, question, mode: 'explain'|'step_by_step'|'simplify'|'mcq_hint' }`) -> `ApiAiTutorResponse` *(fetches D1 curriculum context for grounding, calls Gemini strictly server-side using Worker secret `GEMINI_API_KEY`; returns HTTP `503 TUTOR_UNAVAILABLE` honestly if unconfigured)*

## 6. Restricted AI Agent Ingestion
- `POST /v1/agent/submissions` *(requires `X-Agent-Secret` header)* -> `AgentIngestSubmissionResponse` *(always queued as `'queued'`, detects duplicate SHA-256, cannot self-publish)*

## 7. Admin Authentication & Management (`Authorization: Bearer <admin-jwt>`)
- `GET /v1/admin/bootstrap-status` -> `{ hasAdmin, bootstrapConfigured }`
- `POST /v1/admin/bootstrap` (`AdminBootstrapRequest`) -> `AdminAuthResponse` *(locked with `409` after first `super_admin` exists)*
- `POST /v1/admin/login` (`AdminLoginRequest`) -> `AdminAuthResponse` *(PBKDF2-SHA256 verification)*
- `POST /v1/admin/logout` -> `{ loggedOut: true }`
- `GET /v1/admin/overview` -> Full Admin dashboard snapshot (`boards`, `classes`, `subjects`, `chapters`, `contents`, `mcqs`, `aiSubmissions`, `pastPapers`, `pdfs`, `students`, `feedbackReports`, `auditLog`, `analytics`)
- `POST /v1/admin/subjects` | `PUT /v1/admin/subjects/:id` | `DELETE /v1/admin/subjects/:id`
- `POST /v1/admin/chapters` | `PUT /v1/admin/chapters/:id` | `DELETE /v1/admin/chapters/:id`
- `GET /v1/admin/mcqs` | `POST /v1/admin/mcqs` | `PUT /v1/admin/mcqs/:id` | `DELETE /v1/admin/mcqs/:id`
- `POST /v1/admin/content/drafts` -> Saves schema-validated draft
- `POST /v1/admin/content/:contentId/status` -> 4-stage workflow transition (`draft` -> `in_review` -> `approved` -> `published`)
- `POST /v1/admin/content/:contentId/publish` -> Bumps semantic version & publishes live to students
- `GET /v1/admin/ai-submissions` | `POST /v1/admin/ai-submissions/:id/decision` | `POST /v1/admin/ai-submissions/:id/promote`
- `POST /v1/admin/pdfs` *(multipart/form-data PDF upload -> `%PDF-` check -> Backblaze B2 PUT + D1 metadata; supports `category`: `past_paper | textbook | notes_pack | syllabus`)*
- `DELETE /v1/admin/pdfs/:id` *(deletes from Backblaze B2 + D1)*
- `GET /v1/admin/feedback` | `PUT /v1/admin/feedback/:id` *(triage `open | reviewing | resolved | dismissed` + admin notes)*
- `GET /v1/admin/students` | `GET /v1/admin/analytics` | `GET /v1/admin/infrastructure` | `GET /v1/admin/settings` | `PUT /v1/admin/settings`
