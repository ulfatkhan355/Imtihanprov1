# Imtihan Pro — Platform Architecture

## 1. Repository Structure
- `android/` — Native Android student application written in Kotlin, Jetpack Compose, Material 3, Navigation Compose, MVVM, Coroutines/Flow, Room v3 (`ImtihanDatabase`, 18 entities + `MIGRATION_2_3`), DataStore (`UserPreferencesDataStore`), OkHttp REST/PDF client (`ImtihanApiClient`), native `PdfRenderer`, and pure JVM engines (`Stage3To8Engines.kt`). *(Updated through Stage 8; requires JDK 17 + Android SDK 35 + Gradle 8.9 on Ubuntu to compile APK & run JVM unit tests.)*
- `admin/` — Responsive web Admin Console built with React 19, TypeScript, Vite, and Tailwind CSS v4 (mounted by `/src/main.tsx`), backed by `CloudflareAdminApiClient` (with explicit Offline UI Preview toggle preserving mock preview data).
- `backend/` — Cloudflare Worker API (`imtihan-pro-api`) with D1 SQL migrations/seed (`0001_initial_schema.sql`, `0002_seed_kpk_sample.sql`, `0003_stage3_to_stage8.sql`), Web Crypto PBKDF2 + HMAC-SHA256 sessions, D1 rate limiting, AWS SigV4 Backblaze B2 S3 integration, server-side Gemini AI Tutor handler, and Google Sign-In ID token verification.
- `shared-contracts/` — Canonical Stage 2–8 API contracts and deterministic rules (`types.ts`, `endpoints.ts`, `rules.ts`) shared across `backend/`, `admin/`, and mirrored in Kotlin (`Stage3To8Engines.kt`).
- `scripts/` — Ubuntu setup (`setup-local.sh`), Worker deploy (`deploy-backend.sh`), Admin build (`build-admin.sh`), and Android Debug APK build (`build-android-debug.sh`).
- `tests/` — Automated Node.js TypeScript unit & integration tests (`node:test` + `node:sqlite`) verifying domain rules, SQLite/D1 migrations (`0001`–`0003`), Worker routes, Backblaze B2 SigV4 signing/upload/download/delete, Class 9 KPK catalog & search, SM-2/readiness/streak rules, AI Tutor gating, and feedback triage.
- `docs/` & `README_DEPLOYMENT.md` — Architecture, API contracts, and Ubuntu 26.04 deployment instructions.

## 2. Cloudflare + Backblaze B2 Infrastructure Architecture
- **API Compute**: Cloudflare Worker — `imtihan-pro-api` (`backend/wrangler.toml`, awaiting user deployment from Ubuntu).
- **Relational Database**: Cloudflare D1 — `imtihan-pro-db` (`079128c1-203b-477b-8c4b-d6b34daf8bbe`). Stores boards, KPK BISE sub-boards, classes (9–12, Class 9 primary starter), study groups, subjects, chapters, versioned content blocks, MCQs, exam patterns, past papers, PDF assets (`past_paper`, `textbook`, `notes_pack`, `syllabus`), student profiles, quiz submissions, bookmarks, mistakes, flashcards/SM-2 reviews, study tasks/activity days, achievements, feedback reports, admin accounts, AI ingestion queue, audit log, rate limits, and app/ad settings.
- **Object Storage**: Backblaze B2 — bucket `imtihan-pro-assets` (`1bdf9ec6cd50dbdfad180110`, endpoint: `https://s3.eu-central-003.backblazeb2.com`, region: `eu-central-003`). Uses provider-neutral `objectKey` in API contracts. Zero Cloudflare R2 usage.
- **Zero Client Secrets**: Android and Admin Console never embed B2 keys, signing secrets, or `GEMINI_API_KEY`. PDF uploads stream multipart form data to `/v1/admin/pdfs` where the Worker verifies `%PDF-` magic bytes and executes AWS SigV4 PUT requests to Backblaze B2; AI Tutor queries route exclusively through `POST /v1/tutor/ask`.

## 3. Product Scope & Visual Identity — KPK Board & Silver Blue
- **Starter Focus**: First-time setup defaults to **KPK Board** (`PESHAWAR` + all 8 KPK BISE sub-boards), **Class 9** (primary focus, while preserving Class 10–12 catalog structure), **Study Group** (`SCIENCE_BIOLOGY`, `SCIENCE_COMPUTER`, etc.), and **English / Urdu** medium preference, plus an optional 5-minute diagnostic test.
- **Visual System**: Dark mode primary canvas (`#060E21`, `#0B1733`), layered navy surfaces (`#112247`, `#172B56`), silver-blue hairline borders (`#223B70`, `#28447E`), silver-ice text (`#F0F6FF`), electric azure primary accents (`#3B82F6`, `#2563EB`), with a matching Light Mode counterpart (`#F4F7FC`, `#FFFFFF`, `#CBD8EE`, `#1D4ED8`).
- **5 Primary Android Navigation Tabs**: `Home`, `Subjects`, `Practice`, `Papers`, `Profile`, opening secondary routes (`QuizSession`, `SmartInsights`, `MistakeMaster`, `BookmarksFlashcards`, `QuizHistory`, `AiTutor`, `StudyPlanner`, `Motivation`, `Feedback`, `PdfReader`, `Downloads`).

## 4. Deterministic Rules, Integrity & Guardrails
- **Honest Readiness & Weak-Area Analytics**: `computeHonestExamReadiness` returns `null` when a student has fewer than `2` quiz attempts rather than fabricating readiness scores; `detectWeakChapters` flags low-accuracy chapters (`< 70%`) or unresolved mistakes.
- **Negative Marking & Grading**: `gradeQuizWithRules` calculates raw correct/incorrect/unanswered, optional negative marking (e.g., `-0.25`), pass threshold (`40%`), XP earned, and topic-wise accuracy breakdown.
- **SM-2 Spaced Repetition**: `calculateSm2NextReview` schedules flashcard intervals (`1` -> `6` -> `interval * easiness`) across Again/Hard/Good/Easy recalls.
- **Ethical Non-Intrusive Ads**: `shouldDisplayAd` strictly suppresses ads (`false`) during **Notes Reader**, **Solved Questions**, **MCQ Practice / Mock Exams**, and **PDF Reader**, as well as when daily frequency caps (`<= 3/day`) or runtime flags disable ads.
- **Privacy-Safe Leaderboard**: Opt-in only (`leaderboardOptIn`), displaying anonymized handles (`anonymizeLeaderboardHandle`) and never exposing emails or device IDs.
- **Honest AI Tutor**: Grounded strictly against chapter/subject D1 blocks; returns HTTP `503 TUTOR_UNAVAILABLE` cleanly when `GEMINI_API_KEY` is unconfigured or offline.
