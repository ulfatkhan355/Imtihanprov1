-- Imtihan Pro Stage 2 — Cloudflare D1 Initial Schema (imtihan-pro-db)
-- SQLite-compatible DDL with explicit indexes and foreign keys.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS boards (
  id TEXT PRIMARY KEY,
  code TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  region TEXT NOT NULL,
  is_primary_initial_focus INTEGER NOT NULL DEFAULT 0,
  is_active INTEGER NOT NULL DEFAULT 1,
  supported_classes_json TEXT NOT NULL DEFAULT '[9,10,11,12]'
);

CREATE TABLE IF NOT EXISTS academic_classes (
  id TEXT PRIMARY KEY,
  level INTEGER NOT NULL CHECK (level IN (9, 10, 11, 12)),
  label TEXT NOT NULL,
  board_id TEXT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
  board_code TEXT NOT NULL,
  academic_year TEXT NOT NULL,
  coverage_status TEXT NOT NULL DEFAULT 'planned',
  notes TEXT NOT NULL DEFAULT '',
  UNIQUE(board_code, level)
);

CREATE TABLE IF NOT EXISTS subjects (
  id TEXT PRIMARY KEY,
  class_id TEXT NOT NULL REFERENCES academic_classes(id) ON DELETE CASCADE,
  class_level INTEGER NOT NULL CHECK (class_level IN (9, 10, 11, 12)),
  board_code TEXT NOT NULL,
  code TEXT NOT NULL,
  name TEXT NOT NULL,
  icon_key TEXT NOT NULL,
  accent_hex TEXT NOT NULL DEFAULT '#2563EB',
  total_chapters INTEGER NOT NULL DEFAULT 0,
  published_chapters INTEGER NOT NULL DEFAULT 0,
  is_offline_pack_available INTEGER NOT NULL DEFAULT 0,
  coverage_status TEXT NOT NULL DEFAULT 'structure_ready',
  updated_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_subjects_board_class ON subjects(board_code, class_level);

CREATE TABLE IF NOT EXISTS chapters (
  id TEXT PRIMARY KEY,
  subject_id TEXT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  chapter_number INTEGER NOT NULL,
  title TEXT NOT NULL,
  subtitle TEXT NOT NULL DEFAULT '',
  topics_json TEXT NOT NULL DEFAULT '[]',
  current_version TEXT NOT NULL DEFAULT 'v0.1-draft',
  publish_status TEXT NOT NULL DEFAULT 'draft',
  has_detailed_notes INTEGER NOT NULL DEFAULT 0,
  has_revision_notes INTEGER NOT NULL DEFAULT 0,
  mcq_count INTEGER NOT NULL DEFAULT 0,
  solved_questions_count INTEGER NOT NULL DEFAULT 0,
  is_locked_by_default INTEGER NOT NULL DEFAULT 0,
  estimated_size_kb INTEGER NOT NULL DEFAULT 0,
  updated_at_iso TEXT NOT NULL,
  UNIQUE(subject_id, chapter_number)
);

CREATE INDEX IF NOT EXISTS idx_chapters_subject ON chapters(subject_id, chapter_number);

CREATE TABLE IF NOT EXISTS chapter_content_versions (
  id TEXT PRIMARY KEY,
  chapter_id TEXT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
  mode TEXT NOT NULL CHECK (mode IN ('detailed_notes', 'revision_notes', 'solved_questions')),
  version TEXT NOT NULL,
  version_num INTEGER NOT NULL DEFAULT 1,
  publish_status TEXT NOT NULL DEFAULT 'draft',
  language TEXT NOT NULL DEFAULT 'en',
  blocks_json TEXT NOT NULL,
  author_or_source TEXT NOT NULL,
  content_sha256 TEXT NOT NULL,
  created_by_admin_id TEXT,
  reviewed_by_admin_id TEXT,
  published_at_iso TEXT,
  updated_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_content_chapter_mode ON chapter_content_versions(chapter_id, mode, publish_status, version_num DESC);

CREATE TABLE IF NOT EXISTS mcqs (
  id TEXT PRIMARY KEY,
  chapter_id TEXT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
  subject_id TEXT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  class_level INTEGER NOT NULL CHECK (class_level IN (9, 10, 11, 12)),
  question TEXT NOT NULL,
  options_json TEXT NOT NULL,
  correct_option_id TEXT NOT NULL CHECK (correct_option_id IN ('A', 'B', 'C', 'D')),
  explanation TEXT NOT NULL,
  difficulty TEXT NOT NULL DEFAULT 'medium',
  topic_title TEXT NOT NULL DEFAULT '',
  past_paper_year INTEGER,
  created_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mcqs_chapter ON mcqs(chapter_id);

CREATE TABLE IF NOT EXISTS students (
  id TEXT PRIMARY KEY,
  guest_device_id TEXT NOT NULL UNIQUE,
  google_uid TEXT UNIQUE,
  email TEXT,
  display_name TEXT NOT NULL DEFAULT 'Guest Learner',
  avatar_url TEXT,
  selected_class INTEGER NOT NULL DEFAULT 10,
  selected_board TEXT NOT NULL DEFAULT 'KPK',
  study_hours_total REAL NOT NULL DEFAULT 0,
  daily_goal_percent INTEGER NOT NULL DEFAULT 0,
  last_active_iso TEXT NOT NULL,
  created_at_iso TEXT NOT NULL,
  updated_at_iso TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS student_chapter_progress (
  student_id TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  chapter_id TEXT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
  subject_id TEXT NOT NULL DEFAULT '',
  completion_percent INTEGER NOT NULL DEFAULT 0,
  last_block_index INTEGER NOT NULL DEFAULT 0,
  is_completed INTEGER NOT NULL DEFAULT 0,
  last_accessed_at_iso TEXT NOT NULL,
  updated_at_iso TEXT NOT NULL,
  PRIMARY KEY (student_id, chapter_id)
);

CREATE TABLE IF NOT EXISTS quiz_submissions (
  id TEXT PRIMARY KEY,
  session_id TEXT NOT NULL,
  student_id TEXT,
  guest_device_id TEXT NOT NULL,
  subject_id TEXT NOT NULL,
  chapter_id TEXT,
  total_questions INTEGER NOT NULL,
  correct_count INTEGER NOT NULL,
  incorrect_count INTEGER NOT NULL,
  unanswered_count INTEGER NOT NULL,
  percentage INTEGER NOT NULL,
  elapsed_seconds INTEGER NOT NULL,
  answers_json TEXT NOT NULL,
  submitted_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quiz_submitted_at ON quiz_submissions(submitted_at_iso DESC);

CREATE TABLE IF NOT EXISTS pdf_assets (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  category TEXT NOT NULL CHECK (category IN ('past_paper', 'notes_pack', 'syllabus')),
  class_level INTEGER NOT NULL CHECK (class_level IN (9, 10, 11, 12)),
  board_code TEXT NOT NULL,
  subject_id TEXT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  object_key TEXT NOT NULL UNIQUE,
  mime_type TEXT NOT NULL DEFAULT 'application/pdf',
  file_size_bytes INTEGER NOT NULL,
  page_count INTEGER NOT NULL DEFAULT 1,
  checksum_sha256 TEXT NOT NULL,
  offline_cacheable INTEGER NOT NULL DEFAULT 1,
  version TEXT NOT NULL DEFAULT '1.0',
  created_at_iso TEXT NOT NULL,
  updated_at_iso TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS past_papers (
  id TEXT PRIMARY KEY,
  board_code TEXT NOT NULL,
  class_level INTEGER NOT NULL CHECK (class_level IN (9, 10, 11, 12)),
  subject_id TEXT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  subject_name TEXT NOT NULL,
  year INTEGER NOT NULL,
  session TEXT NOT NULL DEFAULT 'Annual',
  is_solved INTEGER NOT NULL DEFAULT 0,
  pdf_metadata_id TEXT NOT NULL REFERENCES pdf_assets(id) ON DELETE CASCADE,
  file_size_mb REAL NOT NULL,
  page_count INTEGER NOT NULL,
  download_count INTEGER NOT NULL DEFAULT 0,
  created_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_past_papers_class ON past_papers(board_code, class_level, year DESC);

CREATE TABLE IF NOT EXISTS admin_users (
  id TEXT PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  display_name TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('reviewer', 'admin', 'super_admin')),
  password_hash TEXT NOT NULL,
  password_salt TEXT NOT NULL,
  is_active INTEGER NOT NULL DEFAULT 1,
  created_at_iso TEXT NOT NULL,
  last_login_at_iso TEXT
);

CREATE TABLE IF NOT EXISTS ai_agent_submissions (
  id TEXT PRIMARY KEY,
  agent_name TEXT NOT NULL,
  model_identifier TEXT NOT NULL,
  target_class INTEGER NOT NULL CHECK (target_class IN (9, 10, 11, 12)),
  board_code TEXT NOT NULL,
  subject_id TEXT NOT NULL,
  subject_name TEXT NOT NULL,
  chapter_id TEXT,
  chapter_title TEXT NOT NULL,
  submission_type TEXT NOT NULL CHECK (submission_type IN ('detailed_notes', 'revision_notes', 'mcq_batch', 'solved_questions')),
  payload_json TEXT NOT NULL,
  payload_sha256 TEXT NOT NULL,
  blocks_count INTEGER NOT NULL DEFAULT 0,
  schema_validation_passed INTEGER NOT NULL DEFAULT 1,
  validation_warnings_json TEXT NOT NULL DEFAULT '[]',
  status TEXT NOT NULL DEFAULT 'queued' CHECK (status IN ('queued', 'in_review', 'approved', 'revision_requested', 'rejected')),
  reviewer_notes TEXT NOT NULL DEFAULT '',
  reviewed_by_admin_id TEXT,
  promoted_content_id TEXT,
  submitted_at_iso TEXT NOT NULL,
  updated_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_submissions_status ON ai_agent_submissions(status, submitted_at_iso DESC);

CREATE TABLE IF NOT EXISTS admin_audit_log (
  id TEXT PRIMARY KEY,
  actor_admin_id TEXT,
  actor_name TEXT NOT NULL,
  action TEXT NOT NULL,
  target_entity TEXT NOT NULL,
  environment_mode TEXT NOT NULL DEFAULT 'cloudflare_stage_2',
  details_json TEXT NOT NULL DEFAULT '{}',
  timestamp_iso TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS rate_limits (
  rate_key TEXT PRIMARY KEY,
  count INTEGER NOT NULL,
  window_start_ms INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS app_settings (
  setting_key TEXT PRIMARY KEY,
  setting_json TEXT NOT NULL,
  updated_at_iso TEXT NOT NULL
);
