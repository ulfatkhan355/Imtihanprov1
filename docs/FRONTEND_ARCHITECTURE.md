# Imtihan Pro — Android Architecture & Screen Inventory (v2.0)

## 1. Navigation Map (5 Primary Bottom Tabs)
1. **Home** (`home`) — Greeting, Class 11 & BISE sub-board switcher, 4-metric overview (Study Time, Qs, Accuracy, Streak), Global Search, Continue Learning, 6 Quick Actions (`MCQs`, `Mock Exams`, `Past Papers`, `Textbooks`, `Notes`, `Flashcards`), Daily Challenge, Focus Studio, Subjects, Syllabus & Exam Countdown.
2. **Learn** (`subjects`) — Subject Catalog -> Subject Details -> Chapter List -> Chapter Workspace -> Detailed Notes / Revision Notes / Solved Questions / Formula & Diagram filters / Textbooks & Past Papers PDF Reader.
3. **Practice** (`practice`) — Diagnostic Test, Chapter Quiz, KPK BISE Timed Mock Exams, Subject Difficulty Drills, Quiz Session (Standard + OMR Bubble Grid, Persistent Timer, Mark-for-Review, Crash/Double-Submit Guard), Quiz Results & Review.
4. **Progress** (`smart_insights`) — Segmented `Overview` & `Subjects` Progress view with Exam Readiness Ring, Recorded Subject Mastery Bar Chart, Weak Chapters Radar, Mistake Master, and SM-2 3D Flashcards.
5. **Profile** (`profile`) — Guest / Google account sync, Light/Dark theme, Urdu/English medium, Sound/Haptic toggles, Worker URL config, Focus Pomodoro Studio, Study Planner, Feedback, and Offline Downloads.

## 2. Offline Reader & Download Architecture
- **Math & Scientific Notation (`OfflineLatexMathView`)**: Zero-network HTML+CSS renderer inside an isolated JS-disabled `WebView` supporting stacked fractions, square roots, matrices/determinants, subscripts/superscripts, and Greek symbols.
- **PDF Reader (`ImtihanPdfReader` + `PdfRenderCoordinator` + `PdfTextSearchEngine`)**:
  - Hybrid engine using native Android `PdfRenderer` for hardware-sharp display & vector zoom plus `pdfbox-android` (`2.0.27.0`) for offline streaming full-text search, match snippets, and Table of Contents outline extraction on Android 7–15.
  - Full-bleed unobstructed `HorizontalPager` with single-page left/right realistic slide transitions, windowed neighbor pre-warming (`page ± 1` only), and auto-hide floating HUD (`Show/Hide` on center tap, edge tap turn, scrubber slider, Paper/Warm/Dim surface tints, bookmarks, notes, and `FileProvider` sharing).
  - Byte-budget `LruCache` safe for 4GB–8GB RAM devices, progressive fit-width bitmaps + high-DPI Matrix viewport tile crops up to `4.0x` zoom, aspect-ratio placeholders, per-page error recovery, and non-truncated Room reading progress restoration.
  - Covered by programmatic synthetic PDF fixtures (math formulas, landscape diagrams, tables, scanned raster page, 120-page stress textbook) with Robolectric JVM, Compose UI, and Android native `PdfRenderer` instrumented tests.
- **Download System (`ImtihanApiClient.downloadPdfToFile` + `PdfDownloadWorker`)**: WorkManager background downloads with HTTP `Range` resume support on `.tmp` partial files, real byte-transfer progress callbacks, `%PDF-` signature validation, optional SHA-256 verification, and atomic rename to final offline storage.

## 3. Android Build Verification Note
- Kotlin/Android compilation (`./gradlew testDebugUnitTest connectedDebugAndroidTest assembleDebug`) requires JDK 17 + Android SDK 35 on your local Ubuntu / Android Studio machine.
