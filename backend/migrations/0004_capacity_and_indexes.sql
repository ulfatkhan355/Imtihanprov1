-- Imtihan Pro — Free-Tier Capacity, Query Indexes, AI Response Cache & Daily Telemetry (0004)
-- Strictly preserves all existing 0001-0003 schema tables and seeded KPK data.

PRAGMA foreign_keys = ON;

-- 1. Query-Optimization Composite Indexes (Eliminate Full Table Scans)
CREATE INDEX IF NOT EXISTS idx_quiz_student_submitted
  ON quiz_submissions(student_id, submitted_at_iso DESC);

CREATE INDEX IF NOT EXISTS idx_quiz_guest_submitted
  ON quiz_submissions(guest_device_id, submitted_at_iso DESC);

CREATE INDEX IF NOT EXISTS idx_quiz_subject_submitted
  ON quiz_submissions(subject_id, submitted_at_iso DESC);

CREATE INDEX IF NOT EXISTS idx_student_progress_student_completed
  ON student_chapter_progress(student_id, is_completed, subject_id);

CREATE INDEX IF NOT EXISTS idx_mcqs_chapter_status_created
  ON mcqs(chapter_id, publish_status, created_at_iso ASC);

CREATE INDEX IF NOT EXISTS idx_mcqs_status_class_subject
  ON mcqs(publish_status, class_level, subject_id, difficulty, created_at_iso ASC);

CREATE INDEX IF NOT EXISTS idx_pdf_assets_filter_created
  ON pdf_assets(board_code, class_level, category, created_at_iso DESC);

CREATE INDEX IF NOT EXISTS idx_flashcards_subject_chapter
  ON flashcards(subject_id, chapter_id);

CREATE INDEX IF NOT EXISTS idx_flashcard_reviews_due
  ON student_flashcard_reviews(student_id, next_review_at_iso ASC);

CREATE INDEX IF NOT EXISTS idx_feedback_status_created
  ON feedback_reports(status, created_at_iso DESC);

CREATE INDEX IF NOT EXISTS idx_activity_days_student_day
  ON student_activity_days(student_id, day_ymd DESC);

CREATE INDEX IF NOT EXISTS idx_students_leaderboard
  ON students(leaderboard_opt_in, selected_class, selected_board, xp_total DESC);

CREATE INDEX IF NOT EXISTS idx_students_last_active
  ON students(last_active_iso DESC);

CREATE INDEX IF NOT EXISTS idx_exam_patterns_board_class
  ON exam_patterns(board_code, class_level, mode);

CREATE INDEX IF NOT EXISTS idx_rate_limits_window
  ON rate_limits(window_start_ms);

-- 2. Lightweight Daily Quota Telemetry (1 row per UTC day, buffered flushes)
CREATE TABLE IF NOT EXISTS daily_usage_metrics (
  day_ymd TEXT PRIMARY KEY,
  worker_requests INTEGER NOT NULL DEFAULT 0,
  edge_cache_hits INTEGER NOT NULL DEFAULT 0,
  d1_reads_est INTEGER NOT NULL DEFAULT 0,
  d1_writes_est INTEGER NOT NULL DEFAULT 0,
  b2_download_bytes INTEGER NOT NULL DEFAULT 0,
  b2_download_count INTEGER NOT NULL DEFAULT 0,
  b2_upload_bytes INTEGER NOT NULL DEFAULT 0,
  ai_requests INTEGER NOT NULL DEFAULT 0,
  ai_cache_hits INTEGER NOT NULL DEFAULT 0,
  ai_blocked_quota INTEGER NOT NULL DEFAULT 0,
  error_count INTEGER NOT NULL DEFAULT 0,
  latency_sum_ms INTEGER NOT NULL DEFAULT 0,
  latency_samples INTEGER NOT NULL DEFAULT 0,
  updated_at_iso TEXT NOT NULL
);

-- 3. AI Tutor Reusable Response Cache (Zero-Cost Semantic/Prompt Cache)
CREATE TABLE IF NOT EXISTS ai_tutor_cache (
  cache_key TEXT PRIMARY KEY,
  board_code TEXT NOT NULL,
  class_level INTEGER NOT NULL,
  subject_id TEXT,
  chapter_id TEXT,
  action TEXT NOT NULL,
  language TEXT NOT NULL,
  normalized_prompt TEXT NOT NULL,
  answer TEXT NOT NULL,
  model TEXT NOT NULL,
  grounded_sources_json TEXT NOT NULL DEFAULT '[]',
  hit_count INTEGER NOT NULL DEFAULT 0,
  created_at_iso TEXT NOT NULL,
  expires_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_cache_expires ON ai_tutor_cache(expires_at_iso);

-- 4. AI Per-Student Daily Cap Tracker (1 row per student/UTC day when AI is used)
CREATE TABLE IF NOT EXISTS ai_student_daily_usage (
  student_key TEXT NOT NULL,
  day_ymd TEXT NOT NULL,
  request_count INTEGER NOT NULL DEFAULT 0,
  updated_at_iso TEXT NOT NULL,
  PRIMARY KEY (student_key, day_ymd)
);

-- 5. Default Capacity & Cost-Protection Policy (idempotent)
INSERT OR IGNORE INTO app_settings (setting_key, setting_json, updated_at_iso) VALUES (
  'capacity_policy',
  '{"warningThresholdPct":70,"highThresholdPct":85,"criticalThresholdPct":95,"aiTutorDailyGlobalCap":400,"aiTutorDailyPerStudentCap":10,"aiCacheTtlHours":168,"b2DailyDownloadSoftCapMb":950,"d1DailyReadSoftCap":4500000,"d1DailyWriteSoftCap":90000,"workerDailyRequestSoftCap":90000,"autoDegradeAiAtCritical":true,"autoBlockUncachedPdfAtCritical":true}',
  '2026-11-15T00:00:00Z'
);
