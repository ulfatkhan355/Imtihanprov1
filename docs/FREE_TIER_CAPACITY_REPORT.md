# Imtihan Pro — Free-Tier Capacity, Scalability & Cost Protection Report

## 1. Verified Official Free-Tier Allowances Table
Imtihan Pro is architected to operate at **$0/month** during initial launch using existing approved infrastructure only (`imtihan-pro-api`, `imtihan-pro-db`, `imtihan-pro-assets`, optional Gemini API Free Tier).

| Service & Resource | Published Free-Tier Allowance | Enforcement / Design Notes |
| :--- | :--- | :--- |
| **Cloudflare Workers Free** | **100,000** requests/day<br>**10 ms** CPU time/invocation<br>**128 MB** memory<br>**50** subrequests/invocation | Public curriculum GETs use deterministic `ETag` (`304`) + Cloudflare Edge `caches.default` + OkHttp 12 MB disk cache to reduce origin Worker invocations. CPU logic is O(1)/O(N) lightweight JSON shaping. |
| **Cloudflare D1 Free** | **5,000,000** rows read/day<br>**100,000** rows written/day<br>**5 GB** total account storage<br>**500 MB** max per DB<br>**50** queries/invocation | Migration `0004_capacity_and_indexes.sql` indexes hot joins/filters; public GETs use in-memory rate limiting (0 D1 writes); telemetry buffers in-isolate (1 batch upsert per 20 reqs or 30s); progress sync skips unchanged chapters. |
| **Backblaze B2 Free** | **10 GB** free storage<br>**1 GB/day** conservative download plan<br>(Modern PAYG allows **3× monthly avg storage** free egress; legacy free tier 1 GB/day)<br>**2,500/day** Class D cap | Never bundle textbooks/PDFs inside the Android APK. All PDFs remain lazily fetched from B2, cached locally on Android (`OkHttp 12 MB` + `filesDir`), and guarded by configurable `b2DailyDownloadSoftCapMb` (`950 MB` default). |
| **Google Gemini API Free** *(Optional)* | **$0** token cost<br>Dynamic project **RPM / TPM / RPD** in AI Studio<br>(Conservative **1,500 RPD / ~15 RPM** ceiling) | Kept strictly optional on the Worker (`GEMINI_API_KEY`). Guarded by `ai_tutor_cache` (7-day TTL D1 cache), `maxOutputTokens: 700`, `6 req/min` burst limit, per-student daily cap (`10`), and global daily cap (`400`). |

---

## 2. Student Capacity Model (300 to 5,000 DAU — Theoretical vs. Measured)

> **IMPORTANT DISTINCTION**: The DAU matrix below is a **theoretical capacity calculation** computed by `shared-contracts/capacity.ts` (`estimateCapacityForDau`). It has **not** been proven against live multi-region Cloudflare Workers/D1 production infrastructure. Measured isolated local SQLite + Worker router benchmarks appear separately in Section 3.

### Default Model Assumptions (`DEFAULT_CAPACITY_ASSUMPTIONS`)
- `rawApiRequestsPerStudent = 28` requests/day (app boot, subjects/chapters/notes/MCQs, exam session, quiz submit, insights, delta sync)
- `publicCacheHitRatio = 0.65` (65% of cacheable catalog/note GETs satisfied by Cloudflare Edge Cache / OkHttp `304 Not Modified`)
- `avgD1ReadsPerWorkerHit = 42` rows read per origin Worker invocation (after `0004` composite indexes & SQL `LIMIT` pagination)
- `avgD1WritesPerStudent = 14` rows written/day (after removing public-GET D1 rate-limit writes, batching telemetry, and delta-skipping unchanged syncs)
- `pdfDownloadsPerStudentPerDay = 0.25` uncached downloads/student/day (`avgPdfSizeMb = 1.4 MB`)
- `aiTutorUsageRatePerStudent = 0.25`, `aiCacheHitRatio = 0.45`

### Theoretical Daily Consumption & Free-Tier Utilization
| DAU Tier | Worker Req/Day | D1 Rows Read/Day | D1 Rows Written/Day | Est. D1 Growth/Mo | B2 Origin Egress/Day | AI Calls/Day | Peak Conc. (Est.) | Verdict |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | :--- |
| **300 DAU** | 4,141 (4.1%) | 173,922 (3.5%) | 4,200 (4.2%) | 1.23 MB | 0.103 GB (10.3%) | 41 (2.7%) | ~7 (0.13 req/s) | **Within Free Tier** |
| **500 DAU** | 6,902 (6.9%) | 289,884 (5.8%) | 7,000 (7.0%) | 1.72 MB | 0.171 GB (17.1%) | 69 (4.6%) | ~12 (0.22 req/s) | **Within Free Tier** |
| **1,000 DAU** | 13,804 (13.8%) | 579,768 (11.6%) | 14,000 (14.0%) | 2.94 MB | 0.342 GB (34.2%) | 138 (9.2%) | ~23 (0.45 req/s) | **Within Free Tier** |
| **2,000 DAU** | 27,608 (27.6%) | 1,159,536 (23.2%) | 28,000 (28.0%) | 5.38 MB | 0.684 GB (68.4%) | 275 (18.3%) | ~47 (0.89 req/s) | **Within Free Tier** |
| **5,000 DAU** | 69,020 (69.0%) | 2,898,840 (58.0%) | 70,000 (70.0%) | 12.71 MB | 1.709 GB (170.9%)* | 688 (45.9%) | ~117 (2.24 req/s) | **Exceeds Conservative B2 1 GB/Day** |

*\*At 5,000 DAU, the conservative `1 GB/day` B2 origin egress cap triggers first (`170.9%`), whereas Cloudflare Worker requests (`69%`), D1 reads (`58%`), and D1 writes (`70%`) remain inside free-tier limits. Enabling Cloudflare CDN proxy / Bandwidth Alliance for public B2 PDFs cuts repeat origin egress substantially.*

---

## 3. Isolated Local Progressive Load-Test Benchmarks (Measured)

Executed locally via `./scripts/run-load-test.sh` against the real `handleWorkerRequest` router backed by an in-memory `node:sqlite` emulator with migrations `0001`–`0004` applied (`tests/loadTest.ts`). Each simulated student runs `POST /v1/auth/guest` -> conditional `ETag` `GET /v1/subjects` & `GET /v1/chapters/:id/content` -> `POST /v1/exams/start` -> `POST /v1/quizzes/submissions` -> `GET /v1/students/me/insights`.

| Concurrency Stage | Requests Sent | Throughput | `304` Cache Hit Rate | SQL Queries / Request | p50 Latency | p95 Latency | p99 Latency | Errors |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| **10 parallel students** | 62 | 734.5 req/s | 32.3% | 5.98 | 14.12 ms | 23.47 ms | 84.60 ms | 0 |
| **25 parallel students** | 152 | 1,091.8 req/s | 32.9% | 6.07 | 17.20 ms | 40.32 ms | 42.66 ms | 0 |
| **50 parallel students** | 302 | 1,378.2 req/s | 33.1% | 6.08 | 30.46 ms | 64.40 ms | 68.84 ms | 0 |
| **100 parallel students** | 602 | 1,347.2 req/s | 33.2% | 6.09 | 57.04 ms | 136.04 ms | 144.29 ms | 0 |

**Measurement Scope & Honesty Note**: These timings were captured on **2026-11-16** inside an isolated local Node.js + SQLite harness (not live Cloudflare edge POPs or remote D1). They confirm zero HTTP 4xx/5xx failures up to 100 concurrent student flows, ~6.0 SQL queries per full workflow average, and zero extra D1 `rate_limits` writes on public `GET` routes.

---

## 4. Database Optimization Report (`backend/migrations/0004_capacity_and_indexes.sql`)

1. **15 Composite Covering/Sorting Indexes Added**:
   - `idx_quiz_student_submitted` (`quiz_submissions(student_id, submitted_at_iso DESC)`)
   - `idx_student_progress_student_completed` (`student_chapter_progress(student_id, is_completed, completion_percent)`)
   - `idx_mcqs_chapter_status_created` (`mcqs(chapter_id, publish_status, created_at_iso ASC)`)
   - `idx_mcqs_subject_class_status`, `idx_pdf_assets_filter`, `idx_past_papers_filter`, `idx_chapters_subject_status_num`, `idx_content_chapter_mode_status`, `idx_flashcards_subject_chapter`, `idx_flashcard_reviews_due`, `idx_bookmarks_student_created`, `idx_feedback_status_created`, `idx_activity_student_day`, `idx_students_board_class_xp`, `idx_exam_patterns_board_class`
   - Verified via SQLite `EXPLAIN QUERY PLAN` in `tests/capacityAndCostControl.test.ts` (uses named composite indexes instead of full-table scans).
2. **Eliminated Full-Table Scans in Repository Queries (`backend/src/db/repository.ts`)**:
   - `listLatestContents()` rewritten using SQL window function `ROW_NUMBER() OVER (PARTITION BY chapter_id, mode ORDER BY datetime(updated_at_iso) DESC)` + `LIMIT`.
   - `searchCatalog()` rewritten from scanning every chapter/MCQ row in JS into indexed SQL `LIKE ... ESCAPE '\' LIMIT` queries (`15`–`25` rows max).
   - Added `limit`/`offset` pagination to `listChapterMcqs`, `listMcqsFiltered`, `listPastPapers`, `listPdfMetadata`, `listFeedbackReports`, and student quiz/mistake endpoints.
3. **Zero-D1-Write Public Rate Limiting & Delta Progress Sync**:
   - Public `GET` requests use per-isolate in-memory token buckets (`enforceInMemoryRateLimit`), eliminating thousands of unnecessary D1 `rate_limits` row writes per day.
   - Mutating/auth routes keep D1-backed atomic `INSERT ... ON CONFLICT DO UPDATE ... RETURNING count` rate limiting (`1` SQL statement per request instead of `SELECT + INSERT/UPDATE`).
   - `POST /v1/students/me/progress/sync` diffs incoming `chapterProgress` and student preference fields against existing D1 rows, skipping unchanged writes completely and batching modified rows via `env.DB.batch()`.
4. **Private Caching Discipline (`backend/src/middleware/cache.ts` + `backend/src/index.ts`)**:
   - Public curriculum routes emit deterministic weak `ETag` headers computed solely over `data` (ignoring `meta.timestampIso`) + `Cache-Control: public, max-age=..., s-maxage=...`.
   - All `/v1/students/me*`, `/v1/auth/*`, `/v1/tutor/*`, `/v1/admin/*`, `/v1/agent/*`, and pre-signed URLs strictly enforce `Cache-Control: private, no-store, max-age=0`.

---

## 5. Content Storage Allocation Plan (Backblaze B2 `10 GB` Free Tier)

Target total initial budget: **`10.00 GB`** (`10,240 MB`) inside `imtihan-pro-assets`:

| Bucket Path Prefix | Asset Category | Target Quota | File Size Guidelines (Compression Rules) |
| :--- | :--- | ---: | :--- |
| `past-papers/{board}/class-{9..12}/` | KPK Past Papers (2015–2026) | **4.50 GB** (45%) | Target `<= 2.5 MB` per paper (150 DPI grayscale PDF, text layer preferred). Covers ~1,800–2,200 papers. |
| `textbooks/{board}/class-{9..12}/` | Official KPK Textbooks | **2.50 GB** (25%) | Chapter-split or compressed textbook PDFs (`<= 12–18 MB` per subject book). Stored strictly in B2, **never in APK**. |
| `notes-packs/{board}/class-{9..12}/` | Printable Revision & Formula Packs | **1.50 GB** (15%) | Vector PDFs (`300–900 KB` per subject pack). |
| `diagrams/{board}/class-{9..12}/` | Physics / Biology / Chemistry Diagrams | **0.50 GB** (5%) | WebP/SVG compressed (`<= 80 KB` per diagram). |
| `reserve/` | Operational Headroom (10%) | **1.00 GB** (10%) | Retained free so B2 bucket never hits hard 10 GB rejection during exams. |

**Android Client Bandwidth Savings (`ImtihanApiClient.kt` + `ImtihanRepositories.kt`)**:
- `ImtihanApiClient.createWithCache(cacheDir)` configures a **12 MB OkHttp HTTP disk cache** honoring Worker `ETag` (`304`) and `Cache-Control` headers.
- Downloaded PDFs are stored locally on device for offline repeat opens with **0** repeat B2 egress.
- Failed offline-sync queue retries are bounded at `attemptCount < 5` to avoid background Worker request storms on poor cellular connections.

---

## 6. AI Cost & Quota Controls (`POST /v1/tutor/ask`)
1. **D1 Response Cache (`ai_tutor_cache`)**: Normalized syllabus prompt + `(board, class, subject, chapter, action, language)` SHA-256 key. Cache hits return HTTP `200` with `X-Imtihan-Ai-Cache: HIT`, **0 upstream Gemini calls**, and do **not** consume per-student or global daily Gemini quota.
2. **Per-Student Daily Cap**: Default `10` uncached questions/student/day (`ai_student_daily_usage`).
3. **Global Daily Cap & Critical Safeguard**: Default `400` uncached Gemini calls/day (auto-paused at `criticalThresholdPct = 95%` -> `380` uncached calls/day when `autoDegradeAiAtCritical = true`).
4. **Prompt & Output Guardrails**: Prompts clamped to `1,200` chars; `maxOutputTokens: 700`; grounded in up to 8 chapter blocks; burst limited to `6 requests/minute` per student/IP.
5. **Graceful Degradation**: Returns honest `503 AI_PROVIDER_UNCONFIGURED` or `429 AI_GLOBAL_QUOTA_REACHED` / `429 AI_STUDENT_QUOTA_REACHED` with zero impact on Notes, MCQs, Exam Simulator, Flashcards, Mistake Master, or Offline PDFs.

---

## 7. Threshold Alerts (`70% / 85% / 95%`), Security & Backup Runbook
- **Admin Console (`Capacity & Quotas` tab)**: Monitors Worker requests, D1 reads/writes/DB MB, B2 storage/daily download MB, and AI Tutor calls in real time via `GET /v1/admin/capacity` and updates thresholds via `PUT /v1/admin/capacity-policy`:
  - **70% (Warning)**: Yellow alert + mitigation guidance.
  - **85% (High)**: Escalated alert recommending shorter search/sync churn & B2 CDN check.
  - **95% (Critical)**: Automatic safe fallback pauses uncached AI Tutor (`autoDegradeAiAtCritical`) and/or blocks uncached B2 PDF streams (`autoBlockUncachedPdfAtCritical`) while preserving cached/offline features.
- **Database Backup & Recovery Policy**:
  - Export weekly D1 snapshot on Ubuntu (`npx --prefix backend wrangler d1 export imtihan-pro-db --remote --output=backups/imtihan-pro-$(date +%F).sql`).
  - Backblaze B2 assets maintain deterministic `objectKey` + `checksumSha256` metadata inside D1 `pdf_assets` so object integrity is verifiable after restores.

---

## 8. Concrete Paid Upgrade Triggers
Stay on $0/month free tier until ANY of these objective operational triggers occurs for **7 consecutive days**:
1. **Cloudflare Workers Paid ($5/mo)**: Daily Worker origin requests exceed **80,000/day** (80% of free quota) after `304`/Edge caching, OR `>1%` CPU time-limit errors (>10 ms) on complex routes.
2. **Cloudflare D1 (Included with Workers Paid $5/mo -> 25B reads/50M writes)**: Daily D1 writes exceed **80,000 rows/day** OR database file size crosses **400 MB** (`80%` of 500 MB free DB cap).
3. **Backblaze B2 Egress / Storage**: Storage approaches **8.5 GB** (`85%` of 10 GB) OR daily origin downloads after CDN caching exceed **850 MB/day** sustained under free allowance.
4. **Google Gemini API Pay-as-you-go**: Valid uncached AI Tutor demand exceeds project AI Studio RPD/RPM limits with `>10%` of active AI queries receiving `429` despite `ai_tutor_cache` warming.
