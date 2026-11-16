-- Imtihan Pro Stages 3–8 — Schema Extensions & Class 9 KPK English-Medium Seed Dataset
-- Preserves all Stage 2 Class 10 tables and rows while expanding KPK BISE academic breadth.

PRAGMA foreign_keys = OFF;

-- Recreate pdf_assets to allow 'textbook' category alongside past_paper, notes_pack, syllabus
CREATE TABLE IF NOT EXISTS pdf_assets_v3 (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  category TEXT NOT NULL CHECK (category IN ('past_paper', 'notes_pack', 'syllabus', 'textbook')),
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

INSERT INTO pdf_assets_v3 (
  id, title, category, class_level, board_code, subject_id, object_key,
  mime_type, file_size_bytes, page_count, checksum_sha256, offline_cacheable,
  version, created_at_iso, updated_at_iso
)
SELECT
  id, title, category, class_level, board_code, subject_id, object_key,
  mime_type, file_size_bytes, page_count, checksum_sha256, offline_cacheable,
  version, created_at_iso, updated_at_iso
FROM pdf_assets;

DROP TABLE pdf_assets;
ALTER TABLE pdf_assets_v3 RENAME TO pdf_assets;

PRAGMA foreign_keys = ON;

-- Extend existing tables with Stage 3-8 columns
ALTER TABLE subjects ADD COLUMN study_groups_json TEXT NOT NULL DEFAULT '[]';
ALTER TABLE subjects ADD COLUMN description TEXT NOT NULL DEFAULT '';
ALTER TABLE subjects ADD COLUMN textbook_pdf_id TEXT;

ALTER TABLE chapters ADD COLUMN curriculum_version TEXT NOT NULL DEFAULT 'KPK-2026';
ALTER TABLE chapters ADD COLUMN difficulty_tag TEXT NOT NULL DEFAULT 'medium';
ALTER TABLE chapters ADD COLUMN source_reference TEXT NOT NULL DEFAULT 'KPK Textbook Board Peshawar';
ALTER TABLE chapters ADD COLUMN reviewed_by_name TEXT;

ALTER TABLE mcqs ADD COLUMN publish_status TEXT NOT NULL DEFAULT 'published';
ALTER TABLE mcqs ADD COLUMN source_reference TEXT NOT NULL DEFAULT 'KPK Textbook & Board Papers';

ALTER TABLE students ADD COLUMN selected_sub_board TEXT DEFAULT 'PESHAWAR';
ALTER TABLE students ADD COLUMN study_group TEXT DEFAULT 'SCIENCE_BIOLOGY';
ALTER TABLE students ADD COLUMN language TEXT NOT NULL DEFAULT 'en';
ALTER TABLE students ADD COLUMN daily_target_minutes INTEGER NOT NULL DEFAULT 60;
ALTER TABLE students ADD COLUMN exam_countdown_date_iso TEXT;
ALTER TABLE students ADD COLUMN diagnostic_completed INTEGER NOT NULL DEFAULT 0;
ALTER TABLE students ADD COLUMN xp_total INTEGER NOT NULL DEFAULT 0;
ALTER TABLE students ADD COLUMN current_streak_days INTEGER NOT NULL DEFAULT 0;
ALTER TABLE students ADD COLUMN longest_streak_days INTEGER NOT NULL DEFAULT 0;
ALTER TABLE students ADD COLUMN leaderboard_opt_in INTEGER NOT NULL DEFAULT 0;
ALTER TABLE students ADD COLUMN alias_handle TEXT;

ALTER TABLE quiz_submissions ADD COLUMN exam_mode TEXT NOT NULL DEFAULT 'chapter_test';
ALTER TABLE quiz_submissions ADD COLUMN marks_obtained REAL NOT NULL DEFAULT 0;
ALTER TABLE quiz_submissions ADD COLUMN total_marks REAL NOT NULL DEFAULT 0;
ALTER TABLE quiz_submissions ADD COLUMN passed INTEGER NOT NULL DEFAULT 0;

-- Stage 4: Examination Engine Patterns
CREATE TABLE IF NOT EXISTS exam_patterns (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  mode TEXT NOT NULL CHECK (mode IN ('chapter_test', 'topic_test', 'subject_test', 'full_mock_exam', 'diagnostic_test')),
  class_level INTEGER NOT NULL CHECK (class_level IN (9, 10, 11, 12)),
  board_code TEXT NOT NULL DEFAULT 'KPK',
  subject_id TEXT,
  question_count INTEGER NOT NULL,
  duration_minutes INTEGER NOT NULL,
  marks_per_mcq REAL NOT NULL DEFAULT 1,
  negative_marking_penalty REAL NOT NULL DEFAULT 0,
  pass_percentage INTEGER NOT NULL DEFAULT 40,
  includes_short_questions_count INTEGER NOT NULL DEFAULT 0,
  includes_long_questions_count INTEGER NOT NULL DEFAULT 0,
  section_summary TEXT NOT NULL DEFAULT ''
);

-- Stage 3 & 5: Student Bookmarks, Mistakes, Flashcards & SRS Reviews
CREATE TABLE IF NOT EXISTS student_bookmarks (
  id TEXT PRIMARY KEY,
  student_id TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  item_type TEXT NOT NULL CHECK (item_type IN ('chapter', 'note_block', 'mcq', 'solved_question', 'pdf')),
  item_id TEXT NOT NULL,
  chapter_id TEXT,
  subject_id TEXT,
  title TEXT NOT NULL,
  subtitle TEXT NOT NULL DEFAULT '',
  created_at_iso TEXT NOT NULL,
  UNIQUE(student_id, item_type, item_id)
);

CREATE INDEX IF NOT EXISTS idx_bookmarks_student ON student_bookmarks(student_id, created_at_iso DESC);

CREATE TABLE IF NOT EXISTS student_mistakes (
  id TEXT PRIMARY KEY,
  student_id TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  mcq_id TEXT NOT NULL REFERENCES mcqs(id) ON DELETE CASCADE,
  subject_id TEXT NOT NULL,
  chapter_id TEXT NOT NULL,
  topic_title TEXT NOT NULL DEFAULT '',
  question TEXT NOT NULL,
  last_wrong_option_id TEXT,
  correct_option_id TEXT NOT NULL,
  wrong_count INTEGER NOT NULL DEFAULT 1,
  mastered INTEGER NOT NULL DEFAULT 0,
  last_attempted_at_iso TEXT NOT NULL,
  UNIQUE(student_id, mcq_id)
);

CREATE INDEX IF NOT EXISTS idx_mistakes_student ON student_mistakes(student_id, mastered, last_attempted_at_iso DESC);

CREATE TABLE IF NOT EXISTS flashcards (
  id TEXT PRIMARY KEY,
  subject_id TEXT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  chapter_id TEXT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
  front TEXT NOT NULL,
  back TEXT NOT NULL,
  formula_hint TEXT,
  topic_title TEXT NOT NULL DEFAULT '',
  created_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_flashcards_chapter ON flashcards(chapter_id);

CREATE TABLE IF NOT EXISTS student_flashcard_reviews (
  student_id TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  flashcard_id TEXT NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
  interval_days INTEGER NOT NULL DEFAULT 1,
  ease_factor REAL NOT NULL DEFAULT 2.5,
  repetitions INTEGER NOT NULL DEFAULT 0,
  next_review_at_iso TEXT NOT NULL,
  last_reviewed_at_iso TEXT NOT NULL,
  PRIMARY KEY (student_id, flashcard_id)
);

-- Stage 6 & 7: Study Planner, Activity Log, Achievements
CREATE TABLE IF NOT EXISTS student_study_tasks (
  id TEXT PRIMARY KEY,
  student_id TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  subject_id TEXT,
  chapter_id TEXT,
  planned_date_iso TEXT NOT NULL,
  estimated_minutes INTEGER NOT NULL DEFAULT 30,
  task_type TEXT NOT NULL CHECK (task_type IN ('study', 'revision', 'quiz', 'past_paper', 'flashcards')),
  completed INTEGER NOT NULL DEFAULT 0,
  completed_at_iso TEXT,
  created_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tasks_student ON student_study_tasks(student_id, planned_date_iso ASC);

CREATE TABLE IF NOT EXISTS student_activity_days (
  student_id TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  day_ymd TEXT NOT NULL,
  minutes_logged INTEGER NOT NULL DEFAULT 0,
  quizzes_completed INTEGER NOT NULL DEFAULT 0,
  chapters_studied INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (student_id, day_ymd)
);

CREATE TABLE IF NOT EXISTS student_achievements (
  student_id TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  achievement_code TEXT NOT NULL,
  unlocked_at_iso TEXT NOT NULL,
  PRIMARY KEY (student_id, achievement_code)
);

-- Stage 8: Student Feedback & Content Error Reporting
CREATE TABLE IF NOT EXISTS feedback_reports (
  id TEXT PRIMARY KEY,
  student_id TEXT,
  guest_device_id TEXT,
  category TEXT NOT NULL CHECK (category IN ('content_error', 'mcq_answer_dispute', 'app_bug', 'feature_suggestion')),
  subject_id TEXT,
  chapter_id TEXT,
  mcq_id TEXT,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'triaged', 'resolved', 'dismissed')),
  admin_resolution_note TEXT NOT NULL DEFAULT '',
  created_at_iso TEXT NOT NULL,
  updated_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback_reports(status, created_at_iso DESC);

-- Tag Study Groups on Existing Class 10 Subjects
UPDATE subjects SET study_groups_json = '["SCIENCE_BIOLOGY","SCIENCE_CS"]', description = 'KPK SSC Part II Physics covering waves, optics, electrostatics & electronics.' WHERE id = 'sub_kpk10_phy';
UPDATE subjects SET study_groups_json = '["SCIENCE_BIOLOGY","SCIENCE_CS"]', description = 'KPK SSC Part II Chemistry covering chemical equilibrium, acids/bases & organic chemistry.' WHERE id = 'sub_kpk10_chem';
UPDATE subjects SET study_groups_json = '["SCIENCE_BIOLOGY","SCIENCE_CS"]', description = 'KPK SSC Part II Mathematics covering quadratic equations, variation & trigonometry.' WHERE id = 'sub_kpk10_math';
UPDATE subjects SET study_groups_json = '["SCIENCE_BIOLOGY"]', description = 'KPK SSC Part II Biology covering gaseous exchange, homeostasis & genetics.' WHERE id = 'sub_kpk10_bio';
UPDATE subjects SET study_groups_json = '["SCIENCE_BIOLOGY","SCIENCE_CS","ARTS_HUMANITIES"]', description = 'KPK SSC Part II English prose, poetry, grammar and translation.' WHERE id = 'sub_kpk10_eng';
UPDATE subjects SET study_groups_json = '["SCIENCE_BIOLOGY","SCIENCE_CS","ARTS_HUMANITIES"]', description = 'KPK SSC Part II Urdu literature, grammar and essay composition.' WHERE id = 'sub_kpk10_urd';

-- Class 9 KPK English-Medium Primary Seed Expansion
UPDATE academic_classes
SET notes = 'Primary KPK Class 9 (SSC Part I) English-medium starter curriculum covering Physics, Chemistry, Biology, Mathematics, Computer Science, English & Urdu.'
WHERE id = 'cls_kpk_9';

INSERT OR IGNORE INTO subjects (
  id, class_id, class_level, board_code, code, name, icon_key, accent_hex,
  total_chapters, published_chapters, is_offline_pack_available, coverage_status,
  updated_at_iso, study_groups_json, description
) VALUES
  ('sub_kpk9_chem', 'cls_kpk_9', 9, 'KPK', 'CHM-09', 'Chemistry', 'chemistry', '#8B5CF6', 8, 1, 1, 'active_seed', '2026-11-15T10:00:00Z', '["SCIENCE_BIOLOGY","SCIENCE_CS"]', 'Fundamentals of chemistry, atomic structure, periodic table & bonding (KPK Class 9).'),
  ('sub_kpk9_bio', 'cls_kpk_9', 9, 'KPK', 'BIO-09', 'Biology', 'biology', '#22C55E', 9, 1, 1, 'active_seed', '2026-11-15T10:00:00Z', '["SCIENCE_BIOLOGY"]', 'Introduction to biology, biological method, biodiversity & cells (KPK Class 9).'),
  ('sub_kpk9_cs', 'cls_kpk_9', 9, 'KPK', 'CS-09', 'Computer Science', 'cs', '#06B6D4', 6, 1, 1, 'active_seed', '2026-11-15T10:00:00Z', '["SCIENCE_CS"]', 'Problem solving, flowcharts, binary number systems & digital logic (KPK Class 9).'),
  ('sub_kpk9_eng', 'cls_kpk_9', 9, 'KPK', 'ENG-09', 'English', 'english', '#60A5FA', 12, 1, 1, 'active_seed', '2026-11-15T10:00:00Z', '["SCIENCE_BIOLOGY","SCIENCE_CS","ARTS_HUMANITIES"]', 'KPK Class 9 English textbook lessons, vocabulary, grammar and comprehension.'),
  ('sub_kpk9_urd', 'cls_kpk_9', 9, 'KPK', 'URD-09', 'Urdu', 'urdu', '#10B981', 10, 0, 0, 'structure_ready', '2026-11-15T10:00:00Z', '["SCIENCE_BIOLOGY","SCIENCE_CS","ARTS_HUMANITIES"]', 'KPK Class 9 Urdu prose, nazm, ghazal and grammar rules.');

UPDATE subjects
SET published_chapters = 2,
    is_offline_pack_available = 1,
    coverage_status = 'active_seed',
    study_groups_json = '["SCIENCE_BIOLOGY","SCIENCE_CS"]',
    description = 'Physical quantities, kinematics, dynamics & forces (KPK Class 9).'
WHERE id = 'sub_kpk9_phy';

UPDATE subjects
SET total_chapters = 17,
    published_chapters = 2,
    is_offline_pack_available = 1,
    coverage_status = 'active_seed',
    study_groups_json = '["SCIENCE_BIOLOGY","SCIENCE_CS"]',
    description = 'Matrices, real/complex numbers, logarithms & algebraic expressions (KPK Class 9).'
WHERE id = 'sub_kpk9_math';

-- Class 9 Seeded Chapters
INSERT OR IGNORE INTO chapters (
  id, subject_id, chapter_number, title, subtitle, topics_json, current_version,
  publish_status, has_detailed_notes, has_revision_notes, mcq_count,
  solved_questions_count, is_locked_by_default, estimated_size_kb, updated_at_iso,
  curriculum_version, difficulty_tag, source_reference, reviewed_by_name
) VALUES
  (
    'ch_phy9_01', 'sub_kpk9_phy', 1, 'Physical Quantities & Measurement',
    'SI base/derived units, scientific notation, vernier callipers & significant figures',
    '[{"id":"t9_p1_1","chapterId":"ch_phy9_01","orderIndex":1,"title":"Base & Derived SI Units","estimatedMinutes":15},{"id":"t9_p1_2","chapterId":"ch_phy9_01","orderIndex":2,"title":"Prefixes & Scientific Notation","estimatedMinutes":18},{"id":"t9_p1_3","chapterId":"ch_phy9_01","orderIndex":3,"title":"Measuring Instruments & Significant Figures","estimatedMinutes":22}]',
    'v1.1.0', 'published', 1, 1, 4, 3, 0, 360, '2026-11-15T10:00:00Z',
    'KPK-2026', 'easy', 'KPK Physics Grade 9 Ch 1', 'Editorial Lead'
  ),
  (
    'ch_math9_01', 'sub_kpk9_math', 1, 'Matrices and Determinants',
    'Order of matrix, types, addition, multiplication, determinant & Cramer''s rule',
    '[{"id":"t9_m1_1","chapterId":"ch_math9_01","orderIndex":1,"title":"Introduction & Types of Matrices","estimatedMinutes":18},{"id":"t9_m1_2","chapterId":"ch_math9_01","orderIndex":2,"title":"Determinant & Multiplicative Inverse","estimatedMinutes":24},{"id":"t9_m1_3","chapterId":"ch_math9_01","orderIndex":3,"title":"Cramer''s Rule & Matrix Inversion","estimatedMinutes":26}]',
    'v1.2.0', 'published', 1, 1, 4, 3, 0, 410, '2026-11-15T10:00:00Z',
    'KPK-2026', 'medium', 'KPK Mathematics Grade 9 Ch 1', 'Editorial Lead'
  ),
  (
    'ch_math9_02', 'sub_kpk9_math', 2, 'Real and Complex Numbers',
    'Properties of real numbers, radicals, laws of exponents & complex conjugate',
    '[{"id":"t9_m2_1","chapterId":"ch_math9_02","orderIndex":1,"title":"Real Number Line & Radicals","estimatedMinutes":18},{"id":"t9_m2_2","chapterId":"ch_math9_02","orderIndex":2,"title":"Complex Numbers z = a + bi","estimatedMinutes":20}]',
    'v1.0.0', 'published', 1, 1, 3, 2, 0, 330, '2026-11-15T10:00:00Z',
    'KPK-2026', 'medium', 'KPK Mathematics Grade 9 Ch 2', 'Editorial Lead'
  ),
  (
    'ch_chem9_01', 'sub_kpk9_chem', 1, 'Fundamentals of Chemistry',
    'Branches of chemistry, empirical/molecular formula, mole & Avogadro''s number',
    '[{"id":"t9_c1_1","chapterId":"ch_chem9_01","orderIndex":1,"title":"Branches of Chemistry & Matter","estimatedMinutes":15},{"id":"t9_c1_2","chapterId":"ch_chem9_01","orderIndex":2,"title":"Mole & Avogadro''s Number","estimatedMinutes":22}]',
    'v1.0.0', 'published', 1, 1, 4, 2, 0, 350, '2026-11-15T10:00:00Z',
    'KPK-2026', 'medium', 'KPK Chemistry Grade 9 Ch 1', 'Editorial Lead'
  ),
  (
    'ch_bio9_01', 'sub_kpk9_bio', 1, 'Introduction to Biology',
    'Divisions of biology, branches, cellular organization & Muslim scientists',
    '[{"id":"t9_b1_1","chapterId":"ch_bio9_01","orderIndex":1,"title":"Divisions & Branches of Biology","estimatedMinutes":16},{"id":"t9_b1_2","chapterId":"ch_bio9_01","orderIndex":2,"title":"Levels of Biological Organization","estimatedMinutes":20}]',
    'v1.0.0', 'published', 1, 1, 3, 2, 0, 320, '2026-11-15T10:00:00Z',
    'KPK-2026', 'easy', 'KPK Biology Grade 9 Ch 1', 'Editorial Lead'
  ),
  (
    'ch_cs9_01', 'sub_kpk9_cs', 1, 'Problem Solving and Algorithms',
    'Problem analysis, algorithm properties, flowchart symbols & trace tables',
    '[{"id":"t9_cs1_1","chapterId":"ch_cs9_01","orderIndex":1,"title":"Steps in Problem Solving","estimatedMinutes":15},{"id":"t9_cs1_2","chapterId":"ch_cs9_01","orderIndex":2,"title":"Flowcharts & Algorithms","estimatedMinutes":20}]',
    'v1.0.0', 'published', 1, 1, 3, 2, 0, 300, '2026-11-15T10:00:00Z',
    'KPK-2026', 'easy', 'KPK Computer Science Grade 9 Ch 1', 'Editorial Lead'
  ),
  (
    'ch_eng9_01', 'sub_kpk9_eng', 1, 'Hazrat Muhammad (SAW): The Model of Tolerance',
    'Reading comprehension, vocabulary synonyms, active/passive & paragraph writing',
    '[{"id":"t9_e1_1","chapterId":"ch_eng9_01","orderIndex":1,"title":"Lesson Summary & Vocabulary","estimatedMinutes":15},{"id":"t9_e1_2","chapterId":"ch_eng9_01","orderIndex":2,"title":"Parts of Speech & Grammar Practice","estimatedMinutes":18}]',
    'v1.0.0', 'published', 1, 1, 3, 2, 0, 260, '2026-11-15T10:00:00Z',
    'KPK-2026', 'easy', 'KPK English Grade 9 Unit 1', 'Editorial Lead'
  );

-- Update Physics 9 Ch 2 MCQ count to reflect new Class 9 MCQs
UPDATE chapters SET mcq_count = 3, solved_questions_count = 2 WHERE id = 'ch_phy9_02';

-- Class 9 Chapter Content Blocks (detailed_notes + solved_questions)
INSERT OR IGNORE INTO chapter_content_versions (
  id, chapter_id, mode, version, version_num, publish_status, language,
  blocks_json, author_or_source, content_sha256, published_at_iso, updated_at_iso
) VALUES
  (
    'cnt_phy9_01_detailed_v110',
    'ch_phy9_01',
    'detailed_notes',
    'v1.1.0',
    1,
    'published',
    'en',
    '[{"id":"p91_1","orderIndex":1,"type":"heading","sectionCode":"1.1","text":"Physical Quantities & International System (SI)"},{"id":"p91_2","orderIndex":2,"type":"paragraph","text":"All measurable quantities are called physical quantities. A physical quantity consists of a numerical magnitude and a standard unit (for example, length = 5 m)."},{"id":"p91_3","orderIndex":3,"type":"definition","term":"Base Quantities","definition":"Seven fundamental physical quantities (length, mass, time, electric current, temperature, intensity of light, and amount of substance) used to derive all other physical quantities.","formulaHint":"7 SI Base Units: m, kg, s, A, K, cd, mol"},{"id":"p91_4","orderIndex":4,"type":"table","title":"Seven SI Base Quantities & Units","headers":["Quantity","SI Unit","Symbol"],"rows":[["Length","Metre","m"],["Mass","Kilogram","kg"],["Time","Second","s"],["Electric Current","Ampere","A"],["Thermodynamic Temperature","Kelvin","K"],["Luminous Intensity","Candela","cd"],["Amount of Substance","Mole","mol"]]},{"id":"p91_5","orderIndex":5,"type":"math_expression","latex":"\\text{Least Count of Vernier Callipers} = \\frac{\\text{Smallest Main Scale Division}}{\\text{Total Vernier Scale Divisions}} = 0.1\\text{ mm} = 0.01\\text{ cm}","plainNotation":"Least Count = 1 mm / 10 = 0.1 mm = 0.01 cm","caption":"Standard Vernier Callipers least count in KPK Physics"},{"id":"p91_6","orderIndex":6,"type":"example","title":"Worked Numerical: Scientific Notation","problemStatement":"Express the average radius of the Earth (6,400,000 m) in standard scientific notation.","steps":["Move the decimal point 6 places to the left to leave one non-zero digit before the decimal.","6,400,000 m = 6.4 × 10^6 m."],"finalAnswer":"6.4 × 10^6 m"},{"id":"p91_7","orderIndex":7,"type":"exam_point","title":"KPK Board High-Yield Tips","boardFrequency":"High","points":["Zero error of Vernier Callipers is positive when the zero of Vernier scale lies right of main scale zero.","All non-zero digits and zeros between significant digits are significant.","Prefix micro (μ) equals 10^-6 while nano (n) equals 10^-9."]},{"id":"p91_8","orderIndex":8,"type":"solved_question","questionType":"short","marks":4,"yearReference":"BISE Peshawar 2023","question":"Differentiate between base quantities and derived quantities with two examples each.","answerSteps":["Base quantities do not depend on other quantities (e.g., length, mass, time).","Derived quantities are expressed in terms of base quantities through multiplication or division (e.g., speed = distance/time, force = mass × acceleration)."]},{"id":"p91_9","orderIndex":9,"type":"solved_question","questionType":"numerical","marks":4,"yearReference":"BISE Mardan 2022","question":"Find the number of significant figures in 0.00450 kg and 2.70 × 10^4 m.","answerSteps":["In 0.00450 kg, leading zeros are non-significant; 4, 5, and trailing zero after decimal give 3 significant figures.","In 2.70 × 10^4 m, all digits in the mantissa (2, 7, 0) are significant -> 3 significant figures."]}]',
    'KPK Class 9 Curriculum Seed',
    'seed_phy9_01_v110',
    '2026-11-15T10:00:00Z',
    '2026-11-15T10:00:00Z'
  ),
  (
    'cnt_phy9_02_detailed_v120',
    'ch_phy9_02',
    'detailed_notes',
    'v1.2.0',
    1,
    'published',
    'en',
    '[{"id":"p92_1","orderIndex":1,"type":"heading","sectionCode":"2.1","text":"Kinematics & Equations of Motion"},{"id":"p92_2","orderIndex":2,"type":"paragraph","text":"Kinematics is the study of motion of an object without discussing the cause of motion (force). Motion is divided into translatory, rotatory, and vibratory motion."},{"id":"p92_3","orderIndex":3,"type":"definition","term":"Acceleration","definition":"The rate of change of velocity of a body with respect to time. It is a vector quantity with SI unit m/s².","formulaHint":"a = (vf - vi) / t"},{"id":"p92_4","orderIndex":4,"type":"math_expression","latex":"v_f = v_i + at \\quad | \\quad S = v_i t + \\frac{1}{2}at^2 \\quad | \\quad 2aS = v_f^2 - v_i^2","plainNotation":"vf = vi + at   |   S = vi*t + 0.5*a*t²   |   2aS = vf² - vi²","caption":"Three Equations of Uniformly Accelerated Motion"},{"id":"p92_5","orderIndex":5,"type":"example","title":"Worked Numerical: Train Deceleration","problemStatement":"A train starting from rest accelerates uniformly and attains a velocity of 20 m/s in 10 seconds. Find its acceleration and distance covered.","steps":["Given: vi = 0 m/s, vf = 20 m/s, t = 10 s","a = (vf - vi) / t = (20 - 0) / 10 = 2 m/s²","S = vi*t + 0.5*a*t² = 0 + 0.5 * 2 * 100 = 100 m"],"finalAnswer":"a = 2 m/s², S = 100 m"},{"id":"p92_6","orderIndex":6,"type":"solved_question","questionType":"long","marks":6,"yearReference":"BISE Swat 2023","question":"Derive the second equation of motion S = vi*t + 1/2 at² using a velocity-time graph.","answerSteps":["Consider a body moving with initial velocity vi and uniform acceleration a for time t until final velocity vf.","Total distance S equals the area under the velocity-time graph (rectangle OABD + triangle ABC).","Area of rectangle = vi × t; Area of triangle = 1/2 × base × height = 1/2 × t × (at).","Adding both gives S = vi*t + 1/2 at²."]}]',
    'KPK Class 9 Curriculum Seed',
    'seed_phy9_02_v120',
    '2026-11-15T10:00:00Z',
    '2026-11-15T10:00:00Z'
  ),
  (
    'cnt_math9_01_detailed_v120',
    'ch_math9_01',
    'detailed_notes',
    'v1.2.0',
    1,
    'published',
    'en',
    '[{"id":"m91_1","orderIndex":1,"type":"heading","sectionCode":"1.1","text":"Matrices, Determinants & Cramer''s Rule"},{"id":"m91_2","orderIndex":2,"type":"definition","term":"Singular Matrix","definition":"A square matrix A is called singular if its determinant |A| equals zero. A singular matrix does not have a multiplicative inverse.","formulaHint":"|A| = ad - bc = 0"},{"id":"m91_3","orderIndex":3,"type":"math_expression","latex":"A^{-1} = \\frac{1}{|A|} \\operatorname{Adj}(A) \\quad (|A| \\neq 0)","plainNotation":"A^-1 = (1 / |A|) * Adj(A)","caption":"Multiplicative Inverse of a 2x2 Matrix"},{"id":"m91_4","orderIndex":4,"type":"example","title":"Worked Example: Solve via Cramer''s Rule","problemStatement":"Solve 2x - 2y = 4 and 3x + 2y = 6 using Cramer''s rule.","steps":["Coefficient matrix A = [[2, -2], [3, 2]], |A| = (2)(2) - (-2)(3) = 4 + 6 = 10","|Ax| = |[4, -2], [6, 2]| = 8 + 12 = 20  => x = 20 / 10 = 2","|Ay| = |[2, 4], [3, 6]| = 12 - 12 = 0  => y = 0 / 10 = 0"],"finalAnswer":"x = 2, y = 0"},{"id":"m91_5","orderIndex":5,"type":"solved_question","questionType":"short","marks":4,"yearReference":"BISE Abbottabad 2022","question":"Find the determinant and adjoint of matrix B = [[3, -1], [2, 4]].","answerSteps":["|B| = (3)(4) - (-1)(2) = 12 + 2 = 14.","Swap diagonal entries and negate off-diagonal entries: Adj(B) = [[4, 1], [-2, 3]]."]}]',
    'KPK Class 9 Curriculum Seed',
    'seed_math9_01_v120',
    '2026-11-15T10:00:00Z',
    '2026-11-15T10:00:00Z'
  ),
  (
    'cnt_math9_02_detailed_v100',
    'ch_math9_02',
    'detailed_notes',
    'v1.0.0',
    1,
    'published',
    'en',
    '[{"id":"m92_1","orderIndex":1,"type":"heading","sectionCode":"2.1","text":"Real & Complex Numbers"},{"id":"m92_2","orderIndex":2,"type":"definition","term":"Complex Number","definition":"A number of the form z = a + bi where a and b are real numbers and i = √(-1) such that i² = -1.","formulaHint":"z̄ = a - bi (conjugate)"},{"id":"m92_3","orderIndex":3,"type":"math_expression","latex":"i^2 = -1, \\quad i^3 = -i, \\quad i^4 = 1","plainNotation":"i² = -1, i³ = -i, i⁴ = 1","caption":"Powers of imaginary unit i"},{"id":"m92_4","orderIndex":4,"type":"solved_question","questionType":"short","marks":4,"yearReference":"BISE Kohat 2023","question":"Simplify i^27 into standard a + bi form.","answerSteps":["Write 27 = 4(6) + 3.","i^27 = (i^4)^6 × i^3 = (1)^6 × (-i) = 0 - i."]}]',
    'KPK Class 9 Curriculum Seed',
    'seed_math9_02_v100',
    '2026-11-15T10:00:00Z',
    '2026-11-15T10:00:00Z'
  ),
  (
    'cnt_chem9_01_detailed_v100',
    'ch_chem9_01',
    'detailed_notes',
    'v1.0.0',
    1,
    'published',
    'en',
    '[{"id":"c91_1","orderIndex":1,"type":"heading","sectionCode":"1.1","text":"Fundamentals of Chemistry & The Mole"},{"id":"c91_2","orderIndex":2,"type":"definition","term":"Mole & Avogadro''s Number","definition":"One mole is the amount of substance that contains exactly 6.022 × 10^23 elementary entities (atoms, molecules, or ions).","formulaHint":"n = Given Mass / Molar Mass"},{"id":"c91_3","orderIndex":3,"type":"math_expression","latex":"n = \\frac{m}{M} \\quad \\text{and} \\quad N = n \\times N_A","plainNotation":"n = m / M   and   Particles N = n × 6.022 × 10²³","caption":"Mole-mass and Avogadro particle conversion"},{"id":"c91_4","orderIndex":4,"type":"solved_question","questionType":"numerical","marks":4,"yearReference":"BISE Peshawar 2023","question":"Calculate the number of moles in 36 g of water (H2O).","answerSteps":["Molar mass of H2O = 2(1) + 16 = 18 g/mol.","n = m / M = 36 / 18 = 2.0 moles."]}]',
    'KPK Class 9 Curriculum Seed',
    'seed_chem9_01_v100',
    '2026-11-15T10:00:00Z',
    '2026-11-15T10:00:00Z'
  ),
  (
    'cnt_bio9_01_detailed_v100',
    'ch_bio9_01',
    'detailed_notes',
    'v1.0.0',
    1,
    'published',
    'en',
    '[{"id":"b91_1","orderIndex":1,"type":"heading","sectionCode":"1.1","text":"Introduction to Biology"},{"id":"b91_2","orderIndex":2,"type":"paragraph","text":"Biology is the scientific study of life. Its three major divisions are Botany (plants), Zoology (animals), and Microbiology (microorganisms)."},{"id":"b91_3","orderIndex":3,"type":"list","ordered":true,"title":"Hierarchy of Biological Organization","items":["Subatomic & Atomic level","Molecular level (Bioelements)","Organelle & Cellular level","Tissue, Organ & Organ System level","Organism, Population, Community & Biosphere"]},{"id":"b91_4","orderIndex":4,"type":"solved_question","questionType":"short","marks":4,"yearReference":"BISE Malakand 2022","question":"Name two contributions of Jabir Bin Hayan and Bu Ali Sina to biology/medicine.","answerSteps":["Jabir Bin Hayan introduced experimental chemistry and authored Al-Nabatat and Al-Hayawan.","Bu Ali Sina (Avicenna) is known as the founder of medicine and wrote Al-Qanun fi al-Tib."]}]',
    'KPK Class 9 Curriculum Seed',
    'seed_bio9_01_v100',
    '2026-11-15T10:00:00Z',
    '2026-11-15T10:00:00Z'
  ),
  (
    'cnt_cs9_01_detailed_v100',
    'ch_cs9_01',
    'detailed_notes',
    'v1.0.0',
    1,
    'published',
    'en',
    '[{"id":"cs91_1","orderIndex":1,"type":"heading","sectionCode":"1.1","text":"Problem Solving & Flowcharts"},{"id":"cs91_2","orderIndex":2,"type":"definition","term":"Algorithm","definition":"A finite, step-by-step set of unambiguous instructions designed to solve a well-defined computational problem.","formulaHint":"Input -> Processing -> Output"},{"id":"cs91_3","orderIndex":3,"type":"table","title":"Standard Flowchart Symbols","headers":["Symbol","Purpose"],"rows":[["Oval / Terminal","Start or End of flowchart"],["Parallelogram","Input / Output operation"],["Rectangle","Processing or calculation"],["Diamond","Decision / Conditional branch"]]},{"id":"cs91_4","orderIndex":4,"type":"solved_question","questionType":"short","marks":4,"yearReference":"BISE Bannu 2023","question":"Write an algorithm to find the largest among three given numbers A, B, and C.","answerSteps":["Step 1: Start and Read A, B, C.","Step 2: If A > B and A > C, Print A is largest.","Step 3: Else if B > C, Print B is largest.","Step 4: Else Print C is largest. Stop."]}]',
    'KPK Class 9 Curriculum Seed',
    'seed_cs9_01_v100',
    '2026-11-15T10:00:00Z',
    '2026-11-15T10:00:00Z'
  ),
  (
    'cnt_eng9_01_detailed_v100',
    'ch_eng9_01',
    'detailed_notes',
    'v1.0.0',
    1,
    'published',
    'en',
    '[{"id":"e91_1","orderIndex":1,"type":"heading","sectionCode":"1.1","text":"Unit 1: Model of Tolerance — Key Comprehension & Grammar"},{"id":"e91_2","orderIndex":2,"type":"paragraph","text":"Tolerance (Burdbaari) means to bear pain or hardships with patience and to respect others even when their opinions or customs differ from yours."},{"id":"e91_3","orderIndex":3,"type":"exam_point","title":"Board Exam Vocabulary & Grammar Focus","boardFrequency":"High","points":["Tolerance = patience and forbearance; Persecute = oppress unfairly.","Identify collective nouns, abstract nouns, and transitional devices in board MCQs."]},{"id":"e91_4","orderIndex":4,"type":"solved_question","questionType":"short","marks":3,"yearReference":"BISE Peshawar 2022","question":"What did the Holy Prophet (SAW) say about forgiveness at the conquest of Makkah?","answerSteps":["He declared a general amnesty saying, ''No reproof against you this day; go your way, for you are free.''"]}]',
    'KPK Class 9 Curriculum Seed',
    'seed_eng9_01_v100',
    '2026-11-15T10:00:00Z',
    '2026-11-15T10:00:00Z'
  );

-- Class 9 MCQs (Physics, Maths, Chemistry, Biology, CS, English)
INSERT OR IGNORE INTO mcqs (
  id, chapter_id, subject_id, class_level, question, options_json,
  correct_option_id, explanation, difficulty, topic_title, past_paper_year, created_at_iso
) VALUES
  ('mcq_p91_1', 'ch_phy9_01', 'sub_kpk9_phy', 9, 'Which of the following is NOT a base SI quantity?', '[{"id":"A","text":"Length"},{"id":"B","text":"Mass"},{"id":"C","text":"Force"},{"id":"D","text":"Time"}]', 'C', 'Force (Newton = kg·m/s²) is a derived quantity, not one of the 7 SI base quantities.', 'easy', 'Base & Derived SI Units', 2022, '2026-11-15T10:00:00Z'),
  ('mcq_p91_2', 'ch_phy9_01', 'sub_kpk9_phy', 9, 'The least count of a standard Vernier Callipers is:', '[{"id":"A","text":"0.1 cm"},{"id":"B","text":"0.01 cm"},{"id":"C","text":"0.001 cm"},{"id":"D","text":"1.0 mm"}]', 'B', 'Standard Vernier Callipers has a least count of 0.1 mm = 0.01 cm.', 'easy', 'Measuring Instruments & Significant Figures', 2023, '2026-11-15T10:00:00Z'),
  ('mcq_p91_3', 'ch_phy9_01', 'sub_kpk9_phy', 9, 'The SI prefix "micro" (μ) represents a factor of:', '[{"id":"A","text":"10^-3"},{"id":"B","text":"10^-6"},{"id":"C","text":"10^-9"},{"id":"D","text":"10^6"}]', 'B', 'Micro (μ) equals 10^-6 (one millionth).', 'easy', 'Prefixes & Scientific Notation', 2021, '2026-11-15T10:00:00Z'),
  ('mcq_p91_4', 'ch_phy9_01', 'sub_kpk9_phy', 9, 'How many significant figures are in 0.02050 kg?', '[{"id":"A","text":"2"},{"id":"B","text":"3"},{"id":"C","text":"4"},{"id":"D","text":"6"}]', 'C', 'Leading zeros are non-significant; 2, 0, 5, and the trailing decimal zero give 4 significant figures.', 'medium', 'Measuring Instruments & Significant Figures', 2023, '2026-11-15T10:00:00Z'),

  ('mcq_p92_1', 'ch_phy9_02', 'sub_kpk9_phy', 9, 'A body has translatory motion if it moves along a:', '[{"id":"A","text":"Straight line only"},{"id":"B","text":"Line without rotation"},{"id":"C","text":"Circle about an axis"},{"id":"D","text":"Fixed point back and forth"}]', 'B', 'In translatory motion, all particles of a body move parallel along a straight or curved line without rotation.', 'easy', 'Translatory, Rotatory & Vibratory Motion', 2021, '2026-11-15T10:00:00Z'),
  ('mcq_p92_2', 'ch_phy9_02', 'sub_kpk9_phy', 9, 'The slope of a velocity-time graph represents:', '[{"id":"A","text":"Displacement"},{"id":"B","text":"Speed"},{"id":"C","text":"Acceleration"},{"id":"D","text":"Momentum"}]', 'C', 'Change in velocity divided by change in time is acceleration.', 'easy', 'Equations of Motion', 2022, '2026-11-15T10:00:00Z'),
  ('mcq_p92_3', 'ch_phy9_02', 'sub_kpk9_phy', 9, 'A car starts from rest and reaches 20 m/s in 5 s with uniform acceleration. Distance covered is:', '[{"id":"A","text":"25 m"},{"id":"B","text":"50 m"},{"id":"C","text":"100 m"},{"id":"D","text":"200 m"}]', 'B', 'a = 20/5 = 4 m/s²; S = 0 + 0.5(4)(5²) = 50 m.', 'hard', 'Equations of Motion', 2023, '2026-11-15T10:00:00Z'),

  ('mcq_m91_1', 'ch_math9_01', 'sub_kpk9_math', 9, 'A matrix having the same number of rows and columns is called a:', '[{"id":"A","text":"Rectangular matrix"},{"id":"B","text":"Square matrix"},{"id":"C","text":"Scalar matrix"},{"id":"D","text":"Null matrix"}]', 'B', 'Order m × n with m = n defines a square matrix.', 'easy', 'Introduction & Types of Matrices', 2022, '2026-11-15T10:00:00Z'),
  ('mcq_m91_2', 'ch_math9_01', 'sub_kpk9_math', 9, 'If |A| = 0 for a square matrix A, then A is called:', '[{"id":"A","text":"Identity matrix"},{"id":"B","text":"Non-singular matrix"},{"id":"C","text":"Singular matrix"},{"id":"D","text":"Transpose matrix"}]', 'C', 'A square matrix with zero determinant is singular and has no inverse.', 'easy', 'Determinant & Multiplicative Inverse', 2021, '2026-11-15T10:00:00Z'),
  ('mcq_m91_3', 'ch_math9_01', 'sub_kpk9_math', 9, 'The determinant of [[4, 2], [3, 5]] is:', '[{"id":"A","text":"14"},{"id":"B","text":"26"},{"id":"C","text":"-14"},{"id":"D","text":"8"}]', 'A', '4×5 - 2×3 = 20 - 6 = 14.', 'medium', 'Determinant & Multiplicative Inverse', 2023, '2026-11-15T10:00:00Z'),
  ('mcq_m91_4', 'ch_math9_01', 'sub_kpk9_math', 9, 'Product AB of matrices A (2×3) and B (3×4) has order:', '[{"id":"A","text":"3 × 3"},{"id":"B","text":"2 × 4"},{"id":"C","text":"4 × 2"},{"id":"D","text":"Undefined"}]', 'B', 'Multiplying (2×3) by (3×4) yields order (2×4).', 'medium', 'Cramer''s Rule & Matrix Inversion', 2022, '2026-11-15T10:00:00Z'),

  ('mcq_m92_1', 'ch_math9_02', 'sub_kpk9_math', 9, 'The value of i^2 is equal to:', '[{"id":"A","text":"1"},{"id":"B","text":"-1"},{"id":"C","text":"i"},{"id":"D","text":"-i"}]', 'B', 'By definition of imaginary unit i = √(-1), i² = -1.', 'easy', 'Complex Numbers z = a + bi', 2021, '2026-11-15T10:00:00Z'),
  ('mcq_m92_2', 'ch_math9_02', 'sub_kpk9_math', 9, 'The complex conjugate of -3 + 5i is:', '[{"id":"A","text":"3 - 5i"},{"id":"B","text":"-3 - 5i"},{"id":"C","text":"3 + 5i"},{"id":"D","text":"5 - 3i"}]', 'B', 'Changing the sign of the imaginary part keeps real part -3 unchanged and yields -3 - 5i.', 'easy', 'Complex Numbers z = a + bi', 2022, '2026-11-15T10:00:00Z'),
  ('mcq_m92_3', 'ch_math9_02', 'sub_kpk9_math', 9, 'Every rational number together with every irrational number forms the set of:', '[{"id":"A","text":"Whole numbers"},{"id":"B","text":"Integers"},{"id":"C","text":"Real numbers"},{"id":"D","text":"Natural numbers"}]', 'C', 'Q ∪ Q'' = R (the set of real numbers).', 'easy', 'Real Number Line & Radicals', 2020, '2026-11-15T10:00:00Z'),

  ('mcq_c91_1', 'ch_chem9_01', 'sub_kpk9_chem', 9, 'Avogadro''s number NA is equal to:', '[{"id":"A","text":"6.022 × 10^22"},{"id":"B","text":"6.022 × 10^23"},{"id":"C","text":"6.022 × 10^24"},{"id":"D","text":"3.01 × 10^23"}]', 'B', 'One mole of any substance contains 6.022 × 10^23 particles.', 'easy', 'Mole & Avogadro''s Number', 2022, '2026-11-15T10:00:00Z'),
  ('mcq_c91_2', 'ch_chem9_01', 'sub_kpk9_chem', 9, 'The branch of chemistry dealing with carbon compounds (except carbonates/oxides) is:', '[{"id":"A","text":"Inorganic chemistry"},{"id":"B","text":"Physical chemistry"},{"id":"C","text":"Organic chemistry"},{"id":"D","text":"Nuclear chemistry"}]', 'C', 'Organic chemistry studies hydrocarbons and their derivatives.', 'easy', 'Branches of Chemistry & Matter', 2021, '2026-11-15T10:00:00Z'),
  ('mcq_c91_3', 'ch_chem9_01', 'sub_kpk9_chem', 9, 'What is the molar mass of CO2 (C=12, O=16)?', '[{"id":"A","text":"28 g/mol"},{"id":"B","text":"32 g/mol"},{"id":"C","text":"44 g/mol"},{"id":"D","text":"48 g/mol"}]', 'C', '12 + 2(16) = 44 g/mol.', 'medium', 'Mole & Avogadro''s Number', 2023, '2026-11-15T10:00:00Z'),
  ('mcq_c91_4', 'ch_chem9_01', 'sub_kpk9_chem', 9, 'Which of the following is a compound?', '[{"id":"A","text":"Air"},{"id":"B","text":"Brass"},{"id":"C","text":"Water"},{"id":"D","text":"Soil"}]', 'C', 'Water (H2O) has fixed chemical composition; air, brass, and soil are mixtures.', 'easy', 'Branches of Chemistry & Matter', 2020, '2026-11-15T10:00:00Z'),

  ('mcq_b91_1', 'ch_bio9_01', 'sub_kpk9_bio', 9, 'The study of tissues under a microscope is called:', '[{"id":"A","text":"Morphology"},{"id":"B","text":"Histology"},{"id":"C","text":"Cytology"},{"id":"D","text":"Taxonomy"}]', 'B', 'Histology is the microscopic study of biological tissues.', 'easy', 'Divisions & Branches of Biology', 2022, '2026-11-15T10:00:00Z'),
  ('mcq_b91_2', 'ch_bio9_01', 'sub_kpk9_bio', 9, 'Which bioelement makes up about 65% of human body mass?', '[{"id":"A","text":"Carbon"},{"id":"B","text":"Oxygen"},{"id":"C","text":"Hydrogen"},{"id":"D","text":"Nitrogen"}]', 'B', 'Oxygen accounts for ~65% of human body mass largely due to water and biomolecules.', 'medium', 'Levels of Biological Organization', 2021, '2026-11-15T10:00:00Z'),
  ('mcq_b91_3', 'ch_bio9_01', 'sub_kpk9_bio', 9, 'Who wrote the famous medical book Al-Qanun fi al-Tib?', '[{"id":"A","text":"Jabir Bin Hayan"},{"id":"B","text":"Abdul Malik Asmai"},{"id":"C","text":"Bu Ali Sina"},{"id":"D","text":"Al-Razi"}]', 'C', 'Bu Ali Sina (Avicenna) authored the Canon of Medicine (Al-Qanun fi al-Tib).', 'easy', 'Divisions & Branches of Biology', 2023, '2026-11-15T10:00:00Z'),

  ('mcq_cs91_1', 'ch_cs9_01', 'sub_kpk9_cs', 9, 'In a flowchart, which symbol is used for a conditional decision?', '[{"id":"A","text":"Rectangle"},{"id":"B","text":"Oval"},{"id":"C","text":"Diamond"},{"id":"D","text":"Parallelogram"}]', 'C', 'The diamond symbol represents branching decisions (Yes/No or True/False).', 'easy', 'Flowcharts & Algorithms', 2022, '2026-11-15T10:00:00Z'),
  ('mcq_cs91_2', 'ch_cs9_01', 'sub_kpk9_cs', 9, 'A rectangle in a flowchart represents:', '[{"id":"A","text":"Input/Output"},{"id":"B","text":"Processing"},{"id":"C","text":"Termination"},{"id":"D","text":"Flow direction"}]', 'B', 'Rectangles represent arithmetic or data processing steps.', 'easy', 'Flowcharts & Algorithms', 2021, '2026-11-15T10:00:00Z'),
  ('mcq_cs91_3', 'ch_cs9_01', 'sub_kpk9_cs', 9, 'Verifying an algorithm manually with test values in tabular form is called a:', '[{"id":"A","text":"Flowline"},{"id":"B","text":"Trace table"},{"id":"C","text":"Compiler"},{"id":"D","text":"Syntax tree"}]', 'B', 'A trace table tracks variable states step by step to check logical correctness.', 'medium', 'Steps in Problem Solving', 2023, '2026-11-15T10:00:00Z'),

  ('mcq_e91_1', 'ch_eng9_01', 'sub_kpk9_eng', 9, 'What is the closest synonym of "tolerance"?', '[{"id":"A","text":"Anger"},{"id":"B","text":"Forbearance"},{"id":"C","text":"Haste"},{"id":"D","text":"Pride"}]', 'B', 'Tolerance means patience and forbearance toward others.', 'easy', 'Lesson Summary & Vocabulary', 2022, '2026-11-15T10:00:00Z'),
  ('mcq_e91_2', 'ch_eng9_01', 'sub_kpk9_eng', 9, 'In "Tolerance is a noble virtue", the word "tolerance" is an:', '[{"id":"A","text":"Concrete noun"},{"id":"B","text":"Proper noun"},{"id":"C","text":"Abstract noun"},{"id":"D","text":"Collective noun"}]', 'C', 'Tolerance names a quality/concept that cannot be physically touched, making it an abstract noun.', 'easy', 'Parts of Speech & Grammar Practice', 2023, '2026-11-15T10:00:00Z'),
  ('mcq_e91_3', 'ch_eng9_01', 'sub_kpk9_eng', 9, 'Choose the correct passive form: "Patience conquers anger."', '[{"id":"A","text":"Anger was conquered by patience."},{"id":"B","text":"Anger is conquered by patience."},{"id":"C","text":"Anger has been conquered by patience."},{"id":"D","text":"Anger is conquering patience."}]', 'B', 'Present simple active converts to "is/are + past participle".', 'medium', 'Parts of Speech & Grammar Practice', 2021, '2026-11-15T10:00:00Z');

-- Stage 4 Exam Patterns (Class 9 & Class 10 KPK)
INSERT OR IGNORE INTO exam_patterns (
  id, title, mode, class_level, board_code, subject_id, question_count,
  duration_minutes, marks_per_mcq, negative_marking_penalty, pass_percentage,
  includes_short_questions_count, includes_long_questions_count, section_summary
) VALUES
  ('pat_kpk9_diag', 'Class 9 Quick Diagnostic Check', 'diagnostic_test', 9, 'KPK', NULL, 6, 8, 1, 0, 50, 0, 0, 'Cross-subject diagnostic sampling Physics, Math, Chemistry & Biology.'),
  ('pat_kpk9_ch_std', 'Chapter Concept Test (Standard KPK)', 'chapter_test', 9, 'KPK', NULL, 10, 12, 1, 0, 40, 2, 0, 'Section A: Objective MCQs + Self-Check Short Questions.'),
  ('pat_kpk9_phy_mock', 'KPK Class 9 Physics Full Board Mock', 'full_mock_exam', 9, 'KPK', 'sub_kpk9_phy', 12, 20, 1, 0, 33, 3, 1, 'BISE KPK SSC-I Physics format: Objective (Sec A) + Short & Long Rubric (Sec B/C).'),
  ('pat_kpk9_math_mock', 'KPK Class 9 Mathematics Full Board Mock', 'full_mock_exam', 9, 'KPK', 'sub_kpk9_math', 15, 25, 1, 0, 33, 3, 1, 'BISE KPK SSC-I Mathematics format covering Matrices & Complex Numbers.'),
  ('pat_kpk10_diag', 'Class 10 Quick Diagnostic Check', 'diagnostic_test', 10, 'KPK', NULL, 6, 8, 1, 0, 50, 0, 0, 'Cross-subject Class 10 diagnostic across Physics, Math & Chemistry.'),
  ('pat_kpk10_phy_mock', 'KPK Class 10 Physics Full Board Mock', 'full_mock_exam', 10, 'KPK', 'sub_kpk10_phy', 12, 20, 1, 0, 33, 3, 1, 'BISE KPK SSC-II Physics objective + solved numerical rubric.');

-- Stage 5 Seed Flashcards (KPK Class 9 & Class 10)
INSERT OR IGNORE INTO flashcards (id, subject_id, chapter_id, front, back, formula_hint, topic_title, created_at_iso) VALUES
  ('fc_p91_1', 'sub_kpk9_phy', 'ch_phy9_01', 'Least count of Vernier Callipers', '0.1 mm or 0.01 cm (1 MSD / 10 VSD)', 'LC = 0.01 cm', 'Measuring Instruments', '2026-11-15T10:00:00Z'),
  ('fc_p91_2', 'sub_kpk9_phy', 'ch_phy9_01', 'SI Base Unit of Amount of Substance', 'Mole (symbol: mol)', '1 mol = 6.022 × 10^23 entities', 'SI Base Units', '2026-11-15T10:00:00Z'),
  ('fc_p92_1', 'sub_kpk9_phy', 'ch_phy9_02', 'Second Equation of Motion', 'Relates displacement S with initial velocity vi, time t, and uniform acceleration a.', 'S = vi*t + 1/2 a t²', 'Equations of Motion', '2026-11-15T10:00:00Z'),
  ('fc_p92_2', 'sub_kpk9_phy', 'ch_phy9_02', 'Third Equation of Motion', 'Relates velocities and displacement without time.', '2aS = vf² - vi²', 'Equations of Motion', '2026-11-15T10:00:00Z'),
  ('fc_m91_1', 'sub_kpk9_math', 'ch_math9_01', 'Singular vs Non-Singular Matrix', 'Singular if |A| = 0 (no inverse). Non-singular if |A| ≠ 0 (invertible).', 'A^-1 = Adj(A)/|A|', 'Determinant', '2026-11-15T10:00:00Z'),
  ('fc_m92_1', 'sub_kpk9_math', 'ch_math9_02', 'Powers of i', 'i² = -1, i³ = -i, i⁴ = 1', 'z = a + bi', 'Complex Numbers', '2026-11-15T10:00:00Z'),
  ('fc_c91_1', 'sub_kpk9_chem', 'ch_chem9_01', 'Avogadro''s Number (NA)', '6.022 × 10^23 atoms, molecules, or formula units per mole.', 'n = mass / molar mass', 'The Mole', '2026-11-15T10:00:00Z'),
  ('fc_b91_1', 'sub_kpk9_bio', 'ch_bio9_01', 'Histology vs Cytology', 'Histology is microscopic study of tissues; Cytology (Cell Biology) studies cell structure and function.', NULL, 'Branches of Biology', '2026-11-15T10:00:00Z'),
  ('fc_cs91_1', 'sub_kpk9_cs', 'ch_cs9_01', 'Flowchart Decision Symbol', 'Diamond shape represents branching conditions (True/False).', NULL, 'Flowcharts', '2026-11-15T10:00:00Z'),
  ('fc_p101_1', 'sub_kpk10_phy', 'ch_phy10_01', 'Simple Pendulum Time Period', 'Depends on length l and gravity g; independent of bob mass.', 'T = 2π √(l / g)', 'Simple Pendulum', '2026-11-15T10:00:00Z'),
  ('fc_m101_1', 'sub_kpk10_math', 'ch_math10_01', 'Quadratic Formula & Discriminant', 'Roots of ax² + bx + c = 0; discriminant Δ = b² - 4ac.', 'x = (-b ± √(b² - 4ac)) / 2a', 'Quadratic Formula', '2026-11-15T10:00:00Z'),
  ('fc_c101_1', 'sub_kpk10_chem', 'ch_chem10_01', 'Law of Mass Action (Kc)', 'Ratio of product concentrations to reactant concentrations raised to stoichiometric coefficients at equilibrium.', 'Kc = [C]^c[D]^d / [A]^a[B]^b', 'Chemical Equilibrium', '2026-11-15T10:00:00Z');

-- Class 9 Textbooks & Past Papers Catalog Entries (B2-backed metadata)
INSERT OR IGNORE INTO pdf_assets (
  id, title, category, class_level, board_code, subject_id, object_key,
  mime_type, file_size_bytes, page_count, checksum_sha256, offline_cacheable,
  version, created_at_iso, updated_at_iso
) VALUES
  ('pdf_9_phy_tb', 'KPK Class 9 Physics Official Textbook (Catalog Seed)', 'textbook', 9, 'KPK', 'sub_kpk9_phy', 'kpk/class-9/physics/textbook-kpk-2026.pdf', 'application/pdf', 4823450, 198, 'seed_pending_b2_upload_9_phy_tb', 1, '1.0', '2026-11-15T10:00:00Z', '2026-11-15T10:00:00Z'),
  ('pdf_9_math_tb', 'KPK Class 9 Mathematics Official Textbook (Catalog Seed)', 'textbook', 9, 'KPK', 'sub_kpk9_math', 'kpk/class-9/math/textbook-kpk-2026.pdf', 'application/pdf', 5242880, 264, 'seed_pending_b2_upload_9_math_tb', 1, '1.0', '2026-11-15T10:00:00Z', '2026-11-15T10:00:00Z'),
  ('pdf_9_phy_pp23', 'BISE Peshawar Class 9 Physics — 2023 Solved Annual Paper (Catalog Seed)', 'past_paper', 9, 'KPK', 'sub_kpk9_phy', 'kpk/class-9/physics/2023-annual-solved.pdf', 'application/pdf', 2150400, 10, 'seed_pending_b2_upload_9_phy_pp23', 1, '1.0', '2026-11-15T10:00:00Z', '2026-11-15T10:00:00Z');

UPDATE subjects SET textbook_pdf_id = 'pdf_9_phy_tb' WHERE id = 'sub_kpk9_phy';
UPDATE subjects SET textbook_pdf_id = 'pdf_9_math_tb' WHERE id = 'sub_kpk9_math';

INSERT OR IGNORE INTO past_papers (
  id, board_code, class_level, subject_id, subject_name, year, session,
  is_solved, pdf_metadata_id, file_size_mb, page_count, download_count, created_at_iso
) VALUES
  ('pp_phy9_2023', 'KPK', 9, 'sub_kpk9_phy', 'Physics', 2023, 'Annual', 1, 'pdf_9_phy_pp23', 2.05, 10, 0, '2026-11-15T10:00:00Z');

-- Default non-intrusive ad placement policy (disabled until admin enables)
INSERT OR IGNORE INTO app_settings (setting_key, setting_json, updated_at_iso) VALUES
  ('ad_placement', '{"enabled":false,"bannerOnHome":true,"bannerOnPapers":true,"suppressDuringQuizAndExam":true,"suppressDuringNotesReading":true,"dailyBannerCap":4,"providerNote":"Banner slots disabled by default; strictly suppressed during quizzes, mock exams, and active reading."}', '2026-11-15T10:00:00Z');
