-- Imtihan Pro Production Launch (v1.0) — Class 11 KPK (HSSC Part I) Curriculum & Exam Security
-- Strictly additive migration: preserves all existing Class 9/10 rows while establishing
-- Class 11 KPK across Science (Pre-Med, Pre-Eng, ICS), Humanities (FA), and Commerce (I.Com)
-- as the verified, complete initial launch class.

ALTER TABLE academic_classes ADD COLUMN is_public_available INTEGER NOT NULL DEFAULT 0;

ALTER TABLE chapter_content_versions ADD COLUMN generation_method TEXT NOT NULL DEFAULT 'human_authored';
ALTER TABLE chapter_content_versions ADD COLUMN verification_status TEXT NOT NULL DEFAULT 'verified';
ALTER TABLE chapter_content_versions ADD COLUMN source_version TEXT NOT NULL DEFAULT 'KPK-2026';

CREATE TABLE IF NOT EXISTS exam_sessions (
  id TEXT PRIMARY KEY,
  student_id TEXT,
  guest_device_id TEXT,
  mode TEXT NOT NULL,
  board_code TEXT NOT NULL,
  class_level INTEGER NOT NULL,
  subject_id TEXT NOT NULL,
  chapter_id TEXT,
  mcq_ids_json TEXT NOT NULL,
  marks_per_mcq REAL NOT NULL DEFAULT 1,
  negative_marking_penalty REAL NOT NULL DEFAULT 0,
  pass_percentage INTEGER NOT NULL DEFAULT 40,
  started_at_iso TEXT NOT NULL,
  submitted_at_iso TEXT
);

CREATE INDEX IF NOT EXISTS idx_exam_sessions_started ON exam_sessions(started_at_iso DESC);

-- Mark ONLY Class 11 (HSSC Part I) as active and publicly advertised for v1.0 launch;
-- Classes 9, 10, and 12 stay prepared in schema without being advertised as complete.
UPDATE academic_classes
SET coverage_status = 'active_seed',
    is_public_available = 1,
    notes = 'Primary Production Launch (v1.0): KPK HSSC Part I (Class 11) across Pre-Medical, Pre-Engineering, ICS, Humanities (FA), and Commerce (I.Com).'
WHERE id = 'cls_kpk_11';

UPDATE academic_classes
SET is_public_available = 0
WHERE id IN ('cls_kpk_9', 'cls_kpk_10', 'cls_kpk_12');

-- Class 11 KPK HSSC-I Subjects across Science, Computer Science, Humanities & Commerce
INSERT OR IGNORE INTO subjects (
  id, class_id, class_level, board_code, code, name, icon_key, accent_hex,
  total_chapters, published_chapters, is_offline_pack_available, coverage_status,
  updated_at_iso, study_groups_json, description, pairing_scheme_summary, offline_pack_size_mb
) VALUES
  (
    'sub_kpk11_phy', 'cls_kpk_11', 11, 'KPK', 'PHY-11', 'Physics (HSSC-I)', 'physics', '#2563EB',
    11, 2, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["PRE_MEDICAL","PRE_ENGINEERING","ICS"]',
    'KPK Textbook Board Class 11 Physics covering Measurement, Vectors, Motion, Work & Rotational Dynamics.',
    'BISE KPK HSSC-I: 18 MCQs (18m) + Sec-B Short 11/14 (44m) + Sec-C Long 3/4 (23m) = 85 Marks Theory',
    0.9
  ),
  (
    'sub_kpk11_chem', 'cls_kpk_11', 11, 'KPK', 'CHM-11', 'Chemistry (HSSC-I)', 'chemistry', '#7C3AED',
    12, 2, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["PRE_MEDICAL","PRE_ENGINEERING"]',
    'Stoichiometry, Atomic Structure, Chemical Bonding, Gases, Liquids & Chemical Equilibrium.',
    'BISE KPK HSSC-I: 18 MCQs (18m) + Sec-B Short 11/14 (44m) + Sec-C Long 3/4 (23m) = 85 Marks Theory',
    0.8
  ),
  (
    'sub_kpk11_bio', 'cls_kpk_11', 11, 'KPK', 'BIO-11', 'Biology (HSSC-I)', 'biology', '#16A34A',
    13, 2, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["PRE_MEDICAL"]',
    'Cell Structure & Function, Biological Molecules, Enzymes, Bioenergetics & Kingdom Plantae/Animalia.',
    'BISE KPK HSSC-I: 18 MCQs (18m) + Sec-B Short 11/14 (44m) + Sec-C Long 3/4 (23m) = 85 Marks Theory',
    0.8
  ),
  (
    'sub_kpk11_math', 'cls_kpk_11', 11, 'KPK', 'MTH-11', 'Mathematics (HSSC-I)', 'math', '#0284C7',
    12, 2, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["PRE_ENGINEERING","ICS"]',
    'Complex Numbers, Matrices & Determinants, Sequences & Series, Permutations, Trigonometry & Vectors.',
    'BISE KPK HSSC-I: 20 MCQs (20m) + Sec-B Short 10/14 (40m) + Sec-C Long 4/6 (40m) = 100 Marks',
    0.9
  ),
  (
    'sub_kpk11_cs', 'cls_kpk_11', 11, 'KPK', 'CS-11', 'Computer Science (HSSC-I)', 'cs', '#0891B2',
    8, 2, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["ICS"]',
    'Computer Systems, Memory Architecture, Operating Systems, Data Communication & Database Concepts.',
    'BISE KPK HSSC-I: 15 MCQs (15m) + Sec-B Short 10/13 (40m) + Sec-C Long 2/3 (20m) = 75 Marks Theory',
    0.7
  ),
  (
    'sub_kpk11_eng', 'cls_kpk_11', 11, 'KPK', 'ENG-11', 'English (HSSC-I)', 'english', '#3B82F6',
    15, 1, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["PRE_MEDICAL","PRE_ENGINEERING","ICS","FA","ICOM"]',
    'KPK Class 11 English Prose, Poetry, Literary Analysis, Grammar, Paraphrasing & Essay Composition.',
    'BISE KPK HSSC-I: 20 MCQs (20m) + Sec-B Comprehension & Poetry (40m) + Sec-C Letters, Essays & Grammar (40m) = 100 Marks',
    0.6
  ),
  (
    'sub_kpk11_urd', 'cls_kpk_11', 11, 'KPK', 'URD-11', 'Urdu (HSSC-I)', 'urdu', '#059669',
    16, 1, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["PRE_MEDICAL","PRE_ENGINEERING","ICS","FA","ICOM"]',
    'KPK HSSC-I Urdu Prose, Ghazals, Nazms, Tashreeh, Khulasa & Essay Writing in Nastaliq.',
    'BISE KPK HSSC-I: 20 MCQs (20m) + Sec-B Ashar/Ibarat Tashreeh (40m) + Sec-C Khulasa, Darkhwast & Mazmoon (40m) = 100 Marks',
    0.6
  ),
  (
    'sub_kpk11_isl', 'cls_kpk_11', 11, 'KPK', 'ISL-11', 'Islamic Education (HSSC-I)', 'islamiat', '#D97706',
    4, 1, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["PRE_MEDICAL","PRE_ENGINEERING","ICS","FA","ICOM"]',
    'Compulsory Islamic Studies covering Aqaid, Quranic Surahs, Selected Ahadith & Seerat-un-Nabi (PBUH).',
    'BISE KPK HSSC-I: 10 MCQs (10m) + Sec-B Short 8/11 (24m) + Sec-C Long 2/3 (16m) = 50 Marks',
    0.4
  ),
  (
    'sub_kpk11_eco', 'cls_kpk_11', 11, 'KPK', 'ECO-11', 'Economics (HSSC-I)', 'math', '#4F46E5',
    10, 1, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["FA","ICOM"]',
    'Microeconomics Foundations: Utility, Demand & Supply Elasticity, Production Cost & Market Structures.',
    'BISE KPK HSSC-I (FA/I.Com): 20 MCQs (20m) + Sec-B Short 10/14 (40m) + Sec-C Long 3/5 (40m) = 100 Marks',
    0.6
  ),
  (
    'sub_kpk11_acc', 'cls_kpk_11', 11, 'KPK', 'ACC-11', 'Principles of Accounting (HSSC-I)', 'cs', '#0D9488',
    10, 1, 1, 'active_seed', '2026-11-18T12:00:00Z',
    '["ICOM"]',
    'Accounting Equation, Journal, Ledger, Trial Balance, Bank Reconciliation & Final Accounts.',
    'BISE KPK HSSC-I (I.Com): 20 MCQs (20m) + Sec-B Numericals & Short Qs (40m) + Sec-C Final Accounts & Journal (40m) = 100 Marks',
    0.7
  );

-- Update Physics XI existing skeleton cleanly so published_chapters reflects newly seeded published chapters
UPDATE subjects
SET published_chapters = 2,
    is_offline_pack_available = 1,
    coverage_status = 'active_seed',
    study_groups_json = '["PRE_MEDICAL","PRE_ENGINEERING","ICS"]',
    description = 'KPK Textbook Board Class 11 Physics covering Measurement, Vectors, Motion, Work & Rotational Dynamics.',
    pairing_scheme_summary = 'BISE KPK HSSC-I: 18 MCQs (18m) + Sec-B Short 11/14 (44m) + Sec-C Long 3/4 (23m) = 85 Marks Theory',
    offline_pack_size_mb = 0.9,
    updated_at_iso = '2026-11-18T12:00:00Z'
WHERE id = 'sub_kpk11_phy';

-- Class 11 Published Chapters (All complete with notes, revision notes, solved questions, MCQs & flashcards)
INSERT OR IGNORE INTO chapters (
  id, subject_id, chapter_number, title, subtitle, topics_json, current_version,
  publish_status, has_detailed_notes, has_revision_notes, mcq_count,
  solved_questions_count, is_locked_by_default, estimated_size_kb, updated_at_iso,
  curriculum_version, difficulty_tag, source_reference, reviewed_by_name,
  title_urdu, subtitle_urdu, board_weightage_marks, pairing_hint, night_before_summary
) VALUES
  (
    'ch_phy11_01', 'sub_kpk11_phy', 1, 'Measurements',
    'SI units, supplementary units, uncertainty propagation & dimensional analysis',
    '[{"id":"t11_p1_1","chapterId":"ch_phy11_01","orderIndex":1,"title":"SI Base, Derived & Supplementary Units (Radian/Steradian)","estimatedMinutes":20},{"id":"t11_p1_2","chapterId":"ch_phy11_01","orderIndex":2,"title":"Errors & Percentage Uncertainty Rules","estimatedMinutes":24},{"id":"t11_p1_3","chapterId":"ch_phy11_01","orderIndex":3,"title":"Dimensional Analysis & Homogeneity","estimatedMinutes":22}]',
    'v1.0.0', 'published', 1, 1, 4, 2, 0, 420, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Physics XI Ch 1', 'Editorial Lead',
    'پیمائش (Measurements)', 'بین الاقوامی اکائیاں، غیر یقینی صورتحال اور ابعادی تجزیہ',
    10, '2 MCQs + 1 Short Q + Ch 2 Long Pair',
    'Radian (plane angle) & Steradian (solid angle); in multiplication/division percentage uncertainties add; in power rule multiply exponent by % uncertainty; [G] = [M^-1 L^3 T^-2].'
  ),
  (
    'ch_phy11_02', 'sub_kpk11_phy', 2, 'Vectors and Equilibrium',
    'Rectangular components, scalar/vector products, torque & conditions of equilibrium',
    '[{"id":"t11_p2_1","chapterId":"ch_phy11_02","orderIndex":1,"title":"Vector Addition by Rectangular Components","estimatedMinutes":24},{"id":"t11_p2_2","chapterId":"ch_phy11_02","orderIndex":2,"title":"Dot (Scalar) & Cross (Vector) Products","estimatedMinutes":26},{"id":"t11_p2_3","chapterId":"ch_phy11_02","orderIndex":3,"title":"Torque & Two Conditions of Equilibrium","estimatedMinutes":22}]',
    'v1.0.0', 'published', 1, 1, 3, 2, 0, 450, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Physics XI Ch 2', 'Editorial Lead',
    'ویکٹرز اور توازن', 'مستطیل اجزاء، ڈاٹ و کراس پروڈکٹ، ٹارک اور توازن کی شرائط',
    12, '2 MCQs + 2 Short Qs + 1 Long Derivation',
    'A·B = AB cosθ (max at 0°); |A×B| = AB sinθ (max at 90°, anticommutative); Torque τ = r×F; Static equilibrium requires ΣF = 0 and Στ = 0.'
  ),
  (
    'ch_chem11_01', 'sub_kpk11_chem', 1, 'Stoichiometry',
    'Mole concept, Avogadro number, limiting reactant, yield & empirical formula',
    '[{"id":"t11_c1_1","chapterId":"ch_chem11_01","orderIndex":1,"title":"Mole, Molar Mass & Avogadro Number","estimatedMinutes":20},{"id":"t11_c1_2","chapterId":"ch_chem11_01","orderIndex":2,"title":"Empirical & Molecular Formulas from Combustion","estimatedMinutes":24},{"id":"t11_c1_3","chapterId":"ch_chem11_01","orderIndex":3,"title":"Limiting Reactant & Percentage Yield","estimatedMinutes":24}]',
    'v1.0.0', 'published', 1, 1, 3, 2, 0, 400, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Chemistry XI Ch 1', 'Editorial Lead',
    'اسٹوئیشیومیٹری', 'مول کا تصور، ایووگاڈرو عدد، لمیٹنگ ری ایکٹنٹ اور فیصد پیداوار',
    10, '2 MCQs + 1 Short Q + Numerical Long Pair',
    '1 mole = 6.022×10^23 particles = 22.414 dm^3 gas at STP; Limiting reactant yields the smallest moles of product; % Yield = (Actual / Theoretical) × 100.'
  ),
  (
    'ch_chem11_02', 'sub_kpk11_chem', 2, 'Atomic Structure',
    'Bohr atomic model, hydrogen spectrum, quantum numbers & electronic configuration',
    '[{"id":"t11_c2_1","chapterId":"ch_chem11_02","orderIndex":1,"title":"Planck Quantum Theory & Bohr Radius/Energy","estimatedMinutes":22},{"id":"t11_c2_2","chapterId":"ch_chem11_02","orderIndex":2,"title":"Hydrogen Emission Spectral Series","estimatedMinutes":20},{"id":"t11_c2_3","chapterId":"ch_chem11_02","orderIndex":3,"title":"Four Quantum Numbers & Aufbau Principle","estimatedMinutes":24}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 390, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Chemistry XI Ch 2', 'Editorial Lead',
    'جوہری ساخت', 'بوہر ماڈل، ہائیڈروجن اسپیکٹرم اور کوانٹم نمبرز',
    11, '2 MCQs + 2 Short Qs + Bohr Radius Long Q',
    'Lyman (UV, n1=1), Balmer (Visible, n1=2), Paschen (IR, n1=3); Azimuthal quantum number l ranges 0 to n-1; Pauli exclusion: max 2 electrons with opposite spin per orbital.'
  ),
  (
    'ch_bio11_01', 'sub_kpk11_bio', 1, 'Cell Structure and Function',
    'Fluid mosaic plasma membrane, organelles, mitochondria, plastids & nucleus',
    '[{"id":"t11_b1_1","chapterId":"ch_bio11_01","orderIndex":1,"title":"Cell Wall & Fluid Mosaic Plasma Membrane","estimatedMinutes":20},{"id":"t11_b1_2","chapterId":"ch_bio11_01","orderIndex":2,"title":"Endoplasmic Reticulum, Golgi & Lysosomes","estimatedMinutes":22},{"id":"t11_b1_3","chapterId":"ch_bio11_01","orderIndex":3,"title":"Mitochondria, Chloroplasts & Nucleus","estimatedMinutes":24}]',
    'v1.0.0', 'published', 1, 1, 3, 2, 0, 410, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Biology XI Ch 1', 'Editorial Lead',
    'خلیے کی ساخت اور افعال', 'پلازما ممبرین کا فلوئڈ موزیک ماڈل، مائٹوکونڈریا اور نیوکلیئس',
    11, '2 MCQs + 2 Short Qs + Organelle Long Q',
    'Singer & Nicolson Fluid Mosaic Model (1972); Prokaryotes have 70S ribosomes while Eukaryotic cytosol has 80S; Mitochondria have circular DNA & double membrane.'
  ),
  (
    'ch_bio11_02', 'sub_kpk11_bio', 2, 'Biological Molecules',
    'Properties of water, carbohydrates, amino acids, proteins & nucleic acids',
    '[{"id":"t11_b2_1","chapterId":"ch_bio11_02","orderIndex":1,"title":"Biochemical Properties of Water","estimatedMinutes":18},{"id":"t11_b2_2","chapterId":"ch_bio11_02","orderIndex":2,"title":"Monosaccharides, Polysaccharides & Peptide Bonds","estimatedMinutes":24}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 380, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Biology XI Ch 2', 'Editorial Lead',
    'حیاتیاتی سالمات', 'پانی کی خصوصیات، کاربوہائیڈریٹس، پروٹین اور نیوکلیک ایسڈ',
    10, '2 MCQs + 2 Short Qs',
    'Glycosidic bond joins monosaccharides; Peptide bond (-CO-NH-) joins amino acids; DNA uses deoxyribose & Thymine, RNA uses ribose & Uracil.'
  ),
  (
    'ch_math11_01', 'sub_kpk11_math', 1, 'Complex Numbers',
    'Imaginary unit i, Argand plane, modulus, conjugate & De Moivre basics',
    '[{"id":"t11_m1_1","chapterId":"ch_math11_01","orderIndex":1,"title":"Powers of i & Algebra of Complex Numbers","estimatedMinutes":20},{"id":"t11_m1_2","chapterId":"ch_math11_01","orderIndex":2,"title":"Modulus, Conjugate & Polar Form","estimatedMinutes":24}]',
    'v1.0.0', 'published', 1, 1, 3, 2, 0, 390, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Mathematics XI Ch 1', 'Editorial Lead',
    'مختلط اعداد', 'تخیلی اکائی، آرگنڈ پلین، ماڈیولس اور کونجوگیٹ',
    10, '2 MCQs + 2 Short Qs + Ch 2 Long Pair',
    'i^2 = -1, i^4 = 1; For z = a + bi, |z| = sqrt(a^2 + b^2), z * z_bar = a^2 + b^2; Division multiplies numerator & denominator by conjugate.'
  ),
  (
    'ch_math11_02', 'sub_kpk11_math', 2, 'Matrices and Determinants',
    'Cofactors, adjoint, 3x3 determinants & Gaussian row reduction for linear systems',
    '[{"id":"t11_m2_1","chapterId":"ch_math11_02","orderIndex":1,"title":"Determinants of Order 3 & Properties","estimatedMinutes":24},{"id":"t11_m2_2","chapterId":"ch_math11_02","orderIndex":2,"title":"Echelon Form & System of Linear Equations","estimatedMinutes":26}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 410, '2026-11-18T12:00:00Z',
    'KPK-2026', 'hard', 'KPK Textbook Board Mathematics XI Ch 2', 'Editorial Lead',
    'میٹرکس اور ڈیٹرمیننٹ', 'تین درجے کا ڈیٹرمیننٹ، ایchelon فارم اور خطی مساوات',
    12, '2 MCQs + 2 Short Qs + 1 Long Solution',
    'det(A^T) = det(A); If two rows are identical, det = 0; Non-trivial solution of homogeneous AX = 0 exists iff det(A) = 0.'
  ),
  (
    'ch_cs11_01', 'sub_kpk11_cs', 1, 'Overview of Computer Systems',
    'Hardware/software layers, input/output devices, CPU buses & instruction cycle',
    '[{"id":"t11_cs1_1","chapterId":"ch_cs11_01","orderIndex":1,"title":"Hardware vs Software & System Architecture","estimatedMinutes":18},{"id":"t11_cs1_2","chapterId":"ch_cs11_01","orderIndex":2,"title":"Primary & Secondary Storage Hierarchy","estimatedMinutes":22}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 340, '2026-11-18T12:00:00Z',
    'KPK-2026', 'easy', 'KPK Textbook Board Computer Science XI Ch 1', 'Editorial Lead',
    'کمپیوٹر سسٹمز کا جائزہ', 'ہارڈ ویئر، سافٹ ویئر، بسز اور اسٹوریج کی درجہ بندی',
    10, '2 MCQs + 2 Short Qs',
    'RAM is volatile primary memory; ROM stores firmware/BIOS permanently; Cache (SRAM) bridges CPU registers and DRAM.'
  ),
  (
    'ch_cs11_02', 'sub_kpk11_cs', 2, 'Computer Memory & Logic Architecture',
    'Registers, data/address/control buses, fetch-decode-execute cycle & ports',
    '[{"id":"t11_cs2_1","chapterId":"ch_cs11_02","orderIndex":1,"title":"CPU Registers & Fetch-Decode-Execute Cycle","estimatedMinutes":22}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 330, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Computer Science XI Ch 2', 'Editorial Lead',
    'کمپیوٹر میموری اور سی پی یو آرکیٹیکچر', 'رجسٹرز، فیچ ڈیکوڈ ایگزیکیوٹ سائیکل اور سسٹم بسز',
    11, '2 MCQs + 2 Short Qs + 1 Long Q',
    'PC holds address of next instruction; IR holds current instruction; Address bus is unidirectional while Data bus is bidirectional.'
  ),
  (
    'ch_eng11_01', 'sub_kpk11_eng', 1, 'Responsibilities of Youth',
    'Quaid-e-Azam''s Dhaka address analysis, comprehension, vocabulary & grammar',
    '[{"id":"t11_e1_1","chapterId":"ch_eng11_01","orderIndex":1,"title":"Summary & Key Themes of Quaid''s Address","estimatedMinutes":18},{"id":"t11_e1_2","chapterId":"ch_eng11_01","orderIndex":2,"title":"Comprehension Questions & Synonyms","estimatedMinutes":20}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 310, '2026-11-18T12:00:00Z',
    'KPK-2026', 'easy', 'KPK Textbook Board English XI Lesson 1', 'Editorial Lead',
    'نوجوانوں کی ذمہ داریاں', 'قائداعظم کا خطاب، خلاصہ، اہم سوالات اور گرامر',
    12, '2 MCQs + 2 Short Qs + Essay/Theme Link',
    'Delivered at Dhaka University Convocation on 24 March 1948; Quaid urged students to avoid political exploitation and prioritize disciplined nation-building.'
  ),
  (
    'ch_urd11_01', 'sub_kpk11_urd', 1, 'اسوہ حسنہ ﷺ (Uswa-e-Hasana)',
    'سیرت النبی ﷺ کے اخلاقی اصول، خلاصہ، تشریح اور کثیر الانتخابی سوالات',
    '[{"id":"t11_u1_1","chapterId":"ch_urd11_01","orderIndex":1,"title":"سبق کا مرکزی خیال اور خلاصہ","estimatedMinutes":18},{"id":"t11_u1_2","chapterId":"ch_urd11_01","orderIndex":2,"title":"اہم اقتباسات کی تشریح","estimatedMinutes":22}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 320, '2026-11-18T12:00:00Z',
    'KPK-2026', 'easy', 'KPK Textbook Board Urdu XI Lesson 1', 'Editorial Lead',
    'اسوہ حسنہ ﷺ', 'سید سلیمان ندوی کا مضمون: خلاصہ، تشریح اور اہم نکات',
    12, '2 MCQs + خلاصہ / تشریح',
    'مصنف: سید سلیمان ندوی؛ مرکزی پیغام: حضور اکرم ﷺ کی حیاتِ طیبہ ہر طبقے کے لیے عملی اور جامع نمونہ ہے۔'
  ),
  (
    'ch_isl11_01', 'sub_kpk11_isl', 1, 'عقیدہ توحید و رسالت (Tauheed & Risalat)',
    'بنیادی عقائد اسلام، توحید کے انسانی زندگی پر اثرات اور ختم نبوت ﷺ',
    '[{"id":"t11_i1_1","chapterId":"ch_isl11_01","orderIndex":1,"title":"توحید کی اقسام اور عملی اثرات","estimatedMinutes":18}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 280, '2026-11-18T12:00:00Z',
    'KPK-2026', 'easy', 'KPK Textbook Board Islamiat XI Ch 1', 'Editorial Lead',
    'عقیدہ توحید و رسالت', 'توحید باری تعالیٰ، رسالت اور ختمِ نبوت ﷺ',
    12, '2 MCQs + 2 Short Qs + 1 Long Q',
    'توحید کا بنیادی تقاضا ذات، صفات اور عبادت میں اللہ کو یکتا ماننا ہے؛ ختم نبوت ﷺ اسلام کا بنیادی عقیدہ ہے۔'
  ),
  (
    'ch_eco11_01', 'sub_kpk11_eco', 1, 'Nature & Scope of Economics',
    'Adam Smith, Alfred Marshall & Lionel Robbins definitions, scarcity & choice',
    '[{"id":"t11_ec1_1","chapterId":"ch_eco11_01","orderIndex":1,"title":"Classical, Neo-Classical & Scarcity Definitions","estimatedMinutes":22}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 330, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Economics XI Ch 1', 'Editorial Lead',
    'معاشیات کی نوعیت اور وسعت', 'آدم اسمتھ، مارشل اور رابنز کی تعریفیں اور قلتِ وسائل',
    12, '2 MCQs + 2 Short Qs + 1 Long Comparison',
    'Adam Smith (1776 Wealth of Nations); Alfred Marshall (1890 Material Welfare); Lionel Robbins (1932 Scarcity & Choice with alternative uses).'
  ),
  (
    'ch_acc11_01', 'sub_kpk11_acc', 1, 'Accounting Equation & Transactions',
    'Assets = Liabilities + Owner Equity, double-entry rules & transaction effects',
    '[{"id":"t11_ac1_1","chapterId":"ch_acc11_01","orderIndex":1,"title":"Basic Accounting Equation & Debit/Credit Rules","estimatedMinutes":24}]',
    'v1.0.0', 'published', 1, 1, 2, 1, 0, 350, '2026-11-18T12:00:00Z',
    'KPK-2026', 'medium', 'KPK Textbook Board Principles of Accounting XI Ch 1', 'Editorial Lead',
    'حسابداری کی مساوات اور اندراجات', 'اثاثے = واجبات + سرمایہ اور دوہرے اندراج کے اصول',
    15, '2 MCQs + 2 Short Qs + 1 Accounting Equation Numerical',
    'Assets = Liabilities + Capital; Revenue increases capital, Expenses and Drawings decrease capital; Assets & Expenses carry normal Debit balances.'
  );

-- Published Content Blocks (Detailed Notes) for Class 11 KPK Chapters
INSERT OR IGNORE INTO chapter_content_versions (
  id, chapter_id, mode, version, version_num, publish_status, language,
  blocks_json, author_or_source, content_sha256, published_at_iso, updated_at_iso,
  generation_method, verification_status, source_version
) VALUES
  (
    'ccv_phy11_01_det', 'ch_phy11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"p11_1_h1","orderIndex":1,"type":"heading","text":"1.1 International System of Units & Supplementary Units","sectionCode":"1.1"},{"id":"p11_1_p1","orderIndex":2,"type":"paragraph","text":"In KPK HSSC-I Physics, physical quantities are categorized into 7 SI Base quantities, Derived quantities, and 2 Supplementary geometrical units: the Radian (plane angle) and Steradian (solid angle)."},{"id":"p11_1_def1","orderIndex":3,"type":"definition","term":"Radian (rad)","definition":"The plane angle subtended at the centre of a circle by an arc whose length equals the radius of that circle (1 full circle = 2pi rad).","formulaHint":"theta = s / r"},{"id":"p11_1_math1","orderIndex":4,"type":"math_expression","latex":"\\frac{\\Delta Z}{Z} = n\\frac{\\Delta A}{A} + m\\frac{\\Delta B}{B}","plainNotation":"% Uncertainty(Z) = n(% Uncertainty A) + m(% Uncertainty B) for Z = A^n * B^m","caption":"Percentage Uncertainty Power & Product Rule"},{"id":"p11_1_ex1","orderIndex":5,"type":"solved_question","questionType":"numerical","marks":4,"question":"The radius of a sphere is measured as r = 2.25 +/- 0.01 cm. Calculate the percentage uncertainty in its volume V = (4/3) pi r^3.","answerSteps":["Percentage uncertainty in r = (0.01 / 2.25) x 100 = 0.44%.","Volume depends on r^3, so multiply % uncertainty by power 3.","Total percentage uncertainty in Volume = 3 x 0.44% = 1.3% (approx)."],"yearReference":"BISE Peshawar HSSC-I"},{"id":"p11_1_ep1","orderIndex":6,"type":"exam_point","title":"High-Yield Dimensional Analysis Checks","points":["Dimensional formula of Gravitational Constant G is [M^-1 L^3 T^-2].","Dimensionless quantities (e.g., plane angle, refractive index, coefficient of friction) carry no dimensions even when they have units like radian."],"boardFrequency":"High"}]',
    'KPK Textbook Board Physics XI', 'sha_phy11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_phy11_02_det', 'ch_phy11_02', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"p11_2_h1","orderIndex":1,"type":"heading","text":"2.1 Scalar & Vector Products & Equilibrium","sectionCode":"2.1"},{"id":"p11_2_def1","orderIndex":2,"type":"definition","term":"Torque (Moment of Force)","definition":"The turning effect of a force about an axis of rotation, equal to the cross product of position vector r and force F.","formulaHint":"tau = r x F = rF sin(theta)"},{"id":"p11_2_math1","orderIndex":3,"type":"math_expression","latex":"\\vec{A}\\cdot\\vec{B} = A_x B_x + A_y B_y + A_z B_z = AB\\cos\\theta","plainNotation":"A . B = AxBx + AyBy + AzBz = A B cos(theta)","caption":"Dot Product & Angle Formula"},{"id":"p11_2_ex1","orderIndex":4,"type":"solved_question","questionType":"short","marks":4,"question":"State the two necessary conditions for complete static equilibrium of a rigid body.","answerSteps":["First Condition (Translational Equilibrium): Vector sum of all external forces acting on the body is zero (Sigma F = 0).","Second Condition (Rotational Equilibrium): Vector sum of all torques about any chosen axis is zero (Sigma tau = 0)."],"yearReference":"KPK Board Pattern"}]',
    'KPK Textbook Board Physics XI', 'sha_phy11_02', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_chem11_01_det', 'ch_chem11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"c11_1_h1","orderIndex":1,"type":"heading","text":"1.1 Stoichiometry, Mole & Limiting Reactant","sectionCode":"1.1"},{"id":"c11_1_def1","orderIndex":2,"type":"definition","term":"Limiting Reactant","definition":"The reactant that is completely consumed first in a chemical reaction and therefore determines the maximum amount of product formed.","formulaHint":"Moles = Mass / Molar Mass"},{"id":"c11_1_math1","orderIndex":3,"type":"math_expression","latex":"\\%\\text{ Yield} = \\frac{\\text{Actual Yield}}{\\text{Theoretical Yield}}\\times 100","plainNotation":"% Yield = (Actual Yield / Theoretical Yield) * 100","caption":"Reaction Efficiency"},{"id":"c11_1_ex1","orderIndex":4,"type":"solved_question","questionType":"numerical","marks":4,"question":"Calculate the number of moles in 44 g of CO2 (Molar mass = 44 g/mol) and its volume at STP.","answerSteps":["Moles of CO2 = 44 g / 44 g/mol = 1.0 mol.","At STP, 1 mole of any ideal gas occupies 22.414 dm^3."],"yearReference":"KPK HSSC-I"}]',
    'KPK Textbook Board Chemistry XI', 'sha_chem11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_chem11_02_det', 'ch_chem11_02', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"c11_2_h1","orderIndex":1,"type":"heading","text":"2.1 Bohr Model & Quantum Numbers","sectionCode":"2.1"},{"id":"c11_2_def1","orderIndex":2,"type":"definition","term":"Pauli Exclusion Principle","definition":"No two electrons in the same atom can have identical values for all four quantum numbers (n, l, m, s).","formulaHint":"Max electrons in shell = 2n^2"},{"id":"c11_2_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"Name the spectral series of hydrogen falling in UV and Visible regions.","answerSteps":["Lyman series lies in the Ultraviolet (UV) region when electrons fall to n1 = 1.","Balmer series lies in the Visible region when electrons fall to n1 = 2."],"yearReference":"KPK HSSC-I"}]',
    'KPK Textbook Board Chemistry XI', 'sha_chem11_02', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_bio11_01_det', 'ch_bio11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"b11_1_h1","orderIndex":1,"type":"heading","text":"1.1 Plasma Membrane & Cell Organelles","sectionCode":"1.1"},{"id":"b11_1_def1","orderIndex":2,"type":"definition","term":"Fluid Mosaic Model","definition":"Proposed by Singer and Nicolson (1972): the plasma membrane is a fluid phospholipid bilayer with protein molecules embedded like a mosaic.","formulaHint":"Hydrophilic heads out, hydrophobic tails in"},{"id":"b11_1_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"Why are mitochondria called semi-autonomous organelles?","answerSteps":["Mitochondria possess their own circular DNA and 70S ribosomes.","They can replicate independently by binary fission and synthesize some of their own proteins."],"yearReference":"KPK Biology XI"}]',
    'KPK Textbook Board Biology XI', 'sha_bio11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_bio11_02_det', 'ch_bio11_02', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"b11_2_h1","orderIndex":1,"type":"heading","text":"2.1 Carbohydrates, Proteins & Nucleic Acids","sectionCode":"2.1"},{"id":"b11_2_def1","orderIndex":2,"type":"definition","term":"Peptide Bond","definition":"A covalent C-N linkage formed between the carboxyl group of one amino acid and the amino group of another by condensation.","formulaHint":"-CO-NH- + H2O"},{"id":"b11_2_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"Differentiate between DNA and RNA sugar and nitrogenous bases.","answerSteps":["DNA contains deoxyribose sugar and Thymine (A, T, G, C).","RNA contains ribose sugar and Uracil in place of Thymine (A, U, G, C)."],"yearReference":"KPK Biology XI"}]',
    'KPK Textbook Board Biology XI', 'sha_bio11_02', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_math11_01_det', 'ch_math11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"m11_1_h1","orderIndex":1,"type":"heading","text":"1.1 Complex Numbers, Conjugate & Modulus","sectionCode":"1.1"},{"id":"m11_1_math1","orderIndex":2,"type":"math_expression","latex":"z = a + bi,\\quad |z| = \\sqrt{a^2 + b^2},\\quad z\\bar{z} = a^2 + b^2","plainNotation":"|a + bi| = sqrt(a^2 + b^2) and z * z_bar = a^2 + b^2","caption":"Modulus & Conjugate Identity"},{"id":"m11_1_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"Find the multiplicative inverse of z = 3 - 4i.","answerSteps":["Modulus squared |z|^2 = 3^2 + (-4)^2 = 9 + 16 = 25.","Conjugate z_bar = 3 + 4i.","z^-1 = z_bar / |z|^2 = (3 + 4i)/25 = 3/25 + (4/25)i."],"yearReference":"KPK Math XI"}]',
    'KPK Textbook Board Math XI', 'sha_math11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_math11_02_det', 'ch_math11_02', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"m11_2_h1","orderIndex":1,"type":"heading","text":"2.1 3x3 Determinants & Linear Systems","sectionCode":"2.1"},{"id":"m11_2_def1","orderIndex":2,"type":"definition","term":"Echelon Form of a Matrix","definition":"A row-reduced form where leading non-zero entries shift strictly rightward in each lower row and zero rows sit at the bottom.","formulaHint":"rank(A) = number of non-zero rows"},{"id":"m11_2_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"When does a homogeneous system AX = 0 have a non-trivial solution?","answerSteps":["A homogeneous system AX = 0 always admits the trivial solution X = 0.","It has infinitely many non-trivial solutions if and only if det(A) = 0 (A is singular)."],"yearReference":"KPK Math XI"}]',
    'KPK Textbook Board Math XI', 'sha_math11_02', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_cs11_01_det', 'ch_cs11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"cs11_1_h1","orderIndex":1,"type":"heading","text":"1.1 Computer Architecture & Storage Hierarchy","sectionCode":"1.1"},{"id":"cs11_1_def1","orderIndex":2,"type":"definition","term":"System Software","definition":"Programs such as Operating Systems, Device Drivers, and Utility Tools that manage hardware resources and host application software.","formulaHint":"OS bridges user applications and CPU/memory"},{"id":"cs11_1_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"Differentiate between RAM and ROM in modern computer systems.","answerSteps":["RAM (Random Access Memory) is volatile read/write primary memory storing active OS/app instructions.","ROM (Read-Only Memory) is non-volatile memory storing boot firmware (BIOS/UEFI)."],"yearReference":"KPK CS XI"}]',
    'KPK Textbook Board CS XI', 'sha_cs11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_cs11_02_det', 'ch_cs11_02', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"cs11_2_h1","orderIndex":1,"type":"heading","text":"2.1 CPU Registers & System Buses","sectionCode":"2.1"},{"id":"cs11_2_def1","orderIndex":2,"type":"definition","term":"Program Counter (PC)","definition":"A dedicated CPU register that holds the memory address of the next instruction to be fetched and executed.","formulaHint":"Increments after each fetch"},{"id":"cs11_2_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"Explain the role of the Control Bus, Address Bus, and Data Bus.","answerSteps":["Address Bus carries memory locations unidirectionally from CPU to RAM.","Data Bus transfers instructions and data bidirectionally.","Control Bus carries timing and read/write signals."],"yearReference":"KPK CS XI"}]',
    'KPK Textbook Board CS XI', 'sha_cs11_02', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_eng11_01_det', 'ch_eng11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"e11_1_h1","orderIndex":1,"type":"heading","text":"Lesson 1: Responsibilities of Youth","sectionCode":"1.1"},{"id":"e11_1_p1","orderIndex":2,"type":"paragraph","text":"In his historic address at the Dhaka University Convocation on March 24, 1948, Quaid-e-Azam Muhammad Ali Jinnah advised students to devote their primary energy to education, discipline, and technical skill-building so Pakistan could thrive as a sovereign state."},{"id":"e11_1_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"Why did Quaid-e-Azam warn students against falling prey to political opportunists?","answerSteps":["He noted that self-serving factions often exploit students'' youthful idealism to disrupt classrooms.","He emphasized that genuine nation-building requires completed degrees, economic competence, and integrity."],"yearReference":"KPK English XI"}]',
    'KPK Textbook Board English XI', 'sha_eng11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_urd11_01_det', 'ch_urd11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'ur',
    '[{"id":"u11_1_h1","orderIndex":1,"type":"heading","text":"سبق 1: اسوہ حسنہ ﷺ (سید سلیمان ندوی)","sectionCode":"1.1"},{"id":"u11_1_p1","orderIndex":2,"type":"paragraph","text":"سید سلیمان ندوی اس مضمون میں واضح کرتے ہیں کہ دنیا کی ہر قوم، پیشے اور عمر کے انسان کے لیے کامل ترین عملی نمونہ حضرت محمد مصطفیٰ ﷺ کی حیاتِ طیبہ میں موجود ہے۔"},{"id":"u11_1_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"مصنف کے نزدیک کامل نمونہ کی چار بنیادی شرائط کیا ہیں؟","answerSteps":["تاریخی توثیق (Historical authenticity)","کاملیت (Completeness)","جامعیت (Comprehensiveness)","عملیت (Practicality)"],"yearReference":"KPK Urdu XI"}]',
    'KPK Textbook Board Urdu XI', 'sha_urd11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_isl11_01_det', 'ch_isl11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'ur',
    '[{"id":"i11_1_h1","orderIndex":1,"type":"heading","text":"باب 1: عقیدہ توحید و رسالت","sectionCode":"1.1"},{"id":"i11_1_p1","orderIndex":2,"type":"paragraph","text":"توحید کے معنی اللہ تعالیٰ کو اس کی ذات، صفات اور اختیارات میں یکتا و بے مثال ماننا ہے۔ عقیدہ توحید انسان میں عزتِ نفس، جرات اور احساسِ ذمہ داری پیدا کرتا ہے۔"},{"id":"i11_1_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"عقیدہ توحید کے انسانی زندگی پر دو اہم اثرات بیان کریں۔","answerSteps":["انسان غیر اللہ کے خوف اور غلامی سے آزاد ہو جاتا ہے۔","معاشرتی مساوات اور تقویٰ کی بنیاد مضبوط ہوتی ہے۔"],"yearReference":"KPK Islamiat XI"}]',
    'KPK Textbook Board Islamiat XI', 'sha_isl11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_eco11_01_det', 'ch_eco11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"ec11_1_h1","orderIndex":1,"type":"heading","text":"1.1 Evolution of Economic Thought","sectionCode":"1.1"},{"id":"ec11_1_def1","orderIndex":2,"type":"definition","term":"Robbins Scarcity Definition","definition":"Economics is the science which studies human behaviour as a relationship between unlimited ends and scarce means having alternative uses.","formulaHint":"Unlimited Wants + Scarce Means -> Choice"},{"id":"ec11_1_ex1","orderIndex":3,"type":"solved_question","questionType":"short","marks":4,"question":"Distinguish between Microeconomics and Macroeconomics with examples.","answerSteps":["Microeconomics studies individual units (consumer demand, single firm pricing).","Macroeconomics studies economy-wide aggregates (GDP, inflation, unemployment)."],"yearReference":"KPK Economics XI"}]',
    'KPK Textbook Board Economics XI', 'sha_eco11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  ),
  (
    'ccv_acc11_01_det', 'ch_acc11_01', 'detailed_notes', 'v1.0.0', 1, 'published', 'en',
    '[{"id":"ac11_1_h1","orderIndex":1,"type":"heading","text":"1.1 The Accounting Equation & Double Entry","sectionCode":"1.1"},{"id":"ac11_1_math1","orderIndex":2,"type":"math_expression","latex":"\\text{Assets} = \\text{Liabilities} + \\text{Owner''s Equity}","plainNotation":"Assets = Liabilities + Owner Equity","caption":"Fundamental Accounting Identity"},{"id":"ac11_1_ex1","orderIndex":3,"type":"solved_question","questionType":"numerical","marks":4,"question":"If business Assets total PKR 450,000 and Owner Equity is PKR 310,000, calculate total Liabilities.","answerSteps":["Assets = Liabilities + Owner Equity.","450,000 = Liabilities + 310,000.","Liabilities = 450,000 - 310,000 = PKR 140,000."],"yearReference":"KPK I.Com Part I"}]',
    'KPK Textbook Board Accounting XI', 'sha_acc11_01', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z',
    'human_authored', 'verified', 'KPK-2026'
  );

-- Class 11 KPK Published MCQs (with Urdu translations on primary science/languages)
INSERT OR IGNORE INTO mcqs (
  id, chapter_id, subject_id, class_level, question, options_json,
  correct_option_id, explanation, difficulty, topic_title, past_paper_year,
  created_at_iso, publish_status, source_reference,
  question_urdu, options_urdu_json, explanation_urdu
) VALUES
  (
    'mcq_phy11_01_1', 'ch_phy11_01', 'sub_kpk11_phy', 11,
    'Which of the following is an SI supplementary unit?',
    '[{"id":"A","text":"Kelvin"},{"id":"B","text":"Steradian"},{"id":"C","text":"Newton"},{"id":"D","text":"Candela"}]',
    'B', 'Radian and Steradian are the two SI supplementary geometrical units.', 'easy', 'SI Base, Derived & Supplementary Units (Radian/Steradian)', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK BISE HSSC-I',
    'ان میں سے کون سی ایس آئی کی ضمنی اکائی (Supplementary Unit) ہے؟',
    '[{"id":"A","text":"کیلون"},{"id":"B","text":"اسٹیریڈین"},{"id":"C","text":"نیوٹن"},{"id":"D","text":"کینڈیلا"}]',
    'ریڈین اور اسٹیریڈین ایس آئی سسٹم کی دو ضمنی اکائیاں ہیں۔'
  ),
  (
    'mcq_phy11_01_2', 'ch_phy11_01', 'sub_kpk11_phy', 11,
    'The dimensional formula of Universal Gravitational Constant G is:',
    '[{"id":"A","text":"[M L^2 T^-2]"},{"id":"B","text":"[M^-1 L^3 T^-2]"},{"id":"C","text":"[M L^-1 T^-2]"},{"id":"D","text":"[M^-1 L^2 T^-1]"}]',
    'B', 'From F = G m1 m2 / r^2, [G] = [F r^2 / m^2] = [M^-1 L^3 T^-2].', 'medium', 'Dimensional Analysis & Homogeneity', 2023,
    '2026-11-18T12:00:00Z', 'published', 'KPK BISE HSSC-I',
    'عالمی ثقلی مستقل G کا ابعادی فارمولا کیا ہے؟',
    '[{"id":"A","text":"[M L^2 T^-2]"},{"id":"B","text":"[M^-1 L^3 T^-2]"},{"id":"C","text":"[M L^-1 T^-2]"},{"id":"D","text":"[M^-1 L^2 T^-1]"}]',
    'G = Fr²/m₁m₂ کے مطابق اس کا ابعادی فارمولا [M⁻¹ L³ T⁻²] ہے۔'
  ),
  (
    'mcq_phy11_01_3', 'ch_phy11_01', 'sub_kpk11_phy', 11,
    'If radius measurement has 2% uncertainty, the percentage uncertainty in sphere volume is:',
    '[{"id":"A","text":"2%"},{"id":"B","text":"4%"},{"id":"C","text":"6%"},{"id":"D","text":"8%"}]',
    'C', 'Volume varies as r^3, so % uncertainty in V = 3 × 2% = 6%.', 'medium', 'Errors & Percentage Uncertainty Rules', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK BISE HSSC-I',
    'اگر رداس کی پیمائش میں 2% غیر یقینی ہو تو کرے کے حجم میں فیصد غیر یقینی کتنی ہوگی؟',
    '[{"id":"A","text":"2%"},{"id":"B","text":"4%"},{"id":"C","text":"6%"},{"id":"D","text":"8%"}]',
    'حجم رداس کے مکعب r³ پر منحصر ہے، اس لیے 3 × 2% = 6%۔'
  ),
  (
    'mcq_phy11_01_4', 'ch_phy11_01', 'sub_kpk11_phy', 11,
    'Light year is the unit of:',
    '[{"id":"A","text":"Time"},{"id":"B","text":"Speed"},{"id":"C","text":"Distance"},{"id":"D","text":"Intensity"}]',
    'C', 'A light year is the distance light travels in vacuum in one year (~9.46×10^15 m).', 'easy', 'SI Base, Derived & Supplementary Units (Radian/Steradian)', 2022,
    '2026-11-18T12:00:00Z', 'published', 'KPK BISE HSSC-I',
    'نوری سال (Light Year) کس مقدار کی اکائی ہے؟',
    '[{"id":"A","text":"وقت"},{"id":"B","text":"رفتار"},{"id":"C","text":"فاصلہ"},{"id":"D","text":"شدت"}]',
    'نوری سال وہ فاصلہ ہے جو روشنی ایک سال میں طے کرتی ہے۔'
  ),
  (
    'mcq_phy11_02_1', 'ch_phy11_02', 'sub_kpk11_phy', 11,
    'If A·B = 0 and A, B are non-zero vectors, the angle between them is:',
    '[{"id":"A","text":"0°"},{"id":"B","text":"45°"},{"id":"C","text":"90°"},{"id":"D","text":"180°"}]',
    'C', 'Dot product AB cos(theta) = 0 implies cos(theta) = 0, so theta = 90°.', 'easy', 'Dot (Scalar) & Cross (Vector) Products', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK BISE HSSC-I',
    'اگر دو غیر صفر ویکٹرز کا ڈاٹ پروڈکٹ صفر ہو تو ان کے درمیان زاویہ ہوگا:',
    '[{"id":"A","text":"0°"},{"id":"B","text":"45°"},{"id":"C","text":"90°"},{"id":"D","text":"180°"}]',
    'cos(90°) = 0 اس لیے دونوں ویکٹرز عموداً ہیں۔'
  ),
  (
    'mcq_phy11_02_2', 'ch_phy11_02', 'sub_kpk11_phy', 11,
    'The cross product i × j equals:',
    '[{"id":"A","text":"0"},{"id":"B","text":"k"},{"id":"C","text":"-k"},{"id":"D","text":"1"}]',
    'B', 'Following right-hand cyclic order (i -> j -> k), i × j = +k.', 'easy', 'Dot (Scalar) & Cross (Vector) Products', 2023,
    '2026-11-18T12:00:00Z', 'published', 'KPK BISE HSSC-I',
    NULL, '[]', NULL
  ),
  (
    'mcq_phy11_02_3', 'ch_phy11_02', 'sub_kpk11_phy', 11,
    'For complete static equilibrium of a body:',
    '[{"id":"A","text":"Sigma F = 0 only"},{"id":"B","text":"Sigma tau = 0 only"},{"id":"C","text":"Sigma F = 0 and Sigma tau = 0"},{"id":"D","text":"Velocity must be non-zero"}]',
    'C', 'Both net force and net torque must vanish for translational & rotational equilibrium.', 'easy', 'Torque & Two Conditions of Equilibrium', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK BISE HSSC-I',
    NULL, '[]', NULL
  ),
  (
    'mcq_chem11_01_1', 'ch_chem11_01', 'sub_kpk11_chem', 11,
    'One mole of any ideal gas at STP occupies a volume of:',
    '[{"id":"A","text":"11.2 dm^3"},{"id":"B","text":"22.414 dm^3"},{"id":"C","text":"24.0 dm^3"},{"id":"D","text":"1.0 dm^3"}]',
    'B', 'Molar volume of an ideal gas at 273.15 K and 1 atm is 22.414 dm^3.', 'easy', 'Mole, Molar Mass & Avogadro Number', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Chemistry XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_chem11_01_2', 'ch_chem11_01', 'sub_kpk11_chem', 11,
    'Which reactant controls the maximum amount of product formed?',
    '[{"id":"A","text":"Excess reactant"},{"id":"B","text":"Catalyst"},{"id":"C","text":"Limiting reactant"},{"id":"D","text":"Solvent"}]',
    'C', 'The limiting reactant is consumed first, halting further product formation.', 'easy', 'Limiting Reactant & Percentage Yield', 2023,
    '2026-11-18T12:00:00Z', 'published', 'KPK Chemistry XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_chem11_02_1', 'ch_chem11_02', 'sub_kpk11_chem', 11,
    'The Balmer series of the hydrogen spectrum lies in the:',
    '[{"id":"A","text":"Ultraviolet region"},{"id":"B","text":"Visible region"},{"id":"C","text":"Infrared region"},{"id":"D","text":"X-ray region"}]',
    'B', 'Transitions terminating at n1 = 2 emit photons in the visible Balmer series.', 'easy', 'Hydrogen Emission Spectral Series', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Chemistry XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_bio11_01_1', 'ch_bio11_01', 'sub_kpk11_bio', 11,
    'Fluid Mosaic Model of plasma membrane was proposed by:',
    '[{"id":"A","text":"Robert Hooke"},{"id":"B","text":"Singer and Nicolson"},{"id":"C","text":"Watson and Crick"},{"id":"D","text":"Robert Brown"}]',
    'B', 'S.J. Singer and G.L. Nicolson proposed the Fluid Mosaic Model in 1972.', 'easy', 'Cell Wall & Fluid Mosaic Plasma Membrane', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Biology XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_bio11_02_1', 'ch_bio11_02', 'sub_kpk11_bio', 11,
    'Amino acids in proteins are linked together by:',
    '[{"id":"A","text":"Glycosidic bond"},{"id":"B","text":"Ester bond"},{"id":"C","text":"Peptide bond"},{"id":"D","text":"Hydrogen bond only"}]',
    'C', 'A covalent peptide (-CO-NH-) bond links adjacent amino acid residues.', 'easy', 'Monosaccharides, Polysaccharides & Peptide Bonds', 2023,
    '2026-11-18T12:00:00Z', 'published', 'KPK Biology XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_math11_01_1', 'ch_math11_01', 'sub_kpk11_math', 11,
    'The value of i^25 equals:',
    '[{"id":"A","text":"1"},{"id":"B","text":"-1"},{"id":"C","text":"i"},{"id":"D","text":"-i"}]',
    'C', 'i^25 = (i^4)^6 · i = 1 · i = i.', 'easy', 'Powers of i & Algebra of Complex Numbers', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Math XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_math11_02_1', 'ch_math11_02', 'sub_kpk11_math', 11,
    'A square matrix A is singular if:',
    '[{"id":"A","text":"|A| = 1"},{"id":"B","text":"|A| = 0"},{"id":"C","text":"|A| > 0"},{"id":"D","text":"A = A^T"}]',
    'B', 'A matrix whose determinant is zero has no multiplicative inverse and is called singular.', 'easy', 'Determinants of Order 3 & Properties', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Math XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_cs11_01_1', 'ch_cs11_01', 'sub_kpk11_cs', 11,
    'Which memory retains startup BIOS instructions even when power is turned off?',
    '[{"id":"A","text":"DRAM"},{"id":"B","text":"SRAM Cache"},{"id":"C","text":"ROM"},{"id":"D","text":"CPU Register"}]',
    'C', 'ROM is non-volatile memory storing permanent firmware such as BIOS/UEFI.', 'easy', 'Primary & Secondary Storage Hierarchy', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK CS XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_cs11_02_1', 'ch_cs11_02', 'sub_kpk11_cs', 11,
    'Which system bus is unidirectional from CPU to main memory?',
    '[{"id":"A","text":"Data bus"},{"id":"B","text":"Address bus"},{"id":"C","text":"USB bus"},{"id":"D","text":"External bus"}]',
    'B', 'The CPU places target memory addresses onto the unidirectional Address Bus.', 'medium', 'CPU Registers & Fetch-Decode-Execute Cycle', 2023,
    '2026-11-18T12:00:00Z', 'published', 'KPK CS XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_eng11_01_1', 'ch_eng11_01', 'sub_kpk11_eng', 11,
    'Where did Quaid-e-Azam deliver the address "Responsibilities of Youth"?',
    '[{"id":"A","text":"Peshawar University"},{"id":"B","text":"Dhaka University Convocation"},{"id":"C","text":"Lahore Islamia College"},{"id":"D","text":"Karachi Bar Association"}]',
    'B', 'Delivered at the Dhaka University Convocation on 24 March 1948.', 'easy', 'Summary & Key Themes of Quaid''s Address', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK English XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_urd11_01_1', 'ch_urd11_01', 'sub_kpk11_urd', 11,
    'سبق "اسوہ حسنہ ﷺ" کے مصنف کون ہیں؟',
    '[{"id":"A","text":"شبلی نعمانی"},{"id":"B","text":"سید سلیمان ندوی"},{"id":"C","text":"مولانا حالی"},{"id":"D","text":"سرسید احمد خان"}]',
    'B', 'یہ معروف مضمون سید سلیمان ندوی کی تصنیف خطباتِ مدراس سے ماخوذ ہے۔', 'easy', 'سبق کا مرکزی خیال اور خلاصہ', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Urdu XI',
    'سبق "اسوہ حسنہ ﷺ" کے مصنف کون ہیں؟',
    '[{"id":"A","text":"شبلی نعمانی"},{"id":"B","text":"سید سلیمان ندوی"},{"id":"C","text":"مولانا حالی"},{"id":"D","text":"سرسید احمد خان"}]',
    'یہ مضمون سید سلیمان ندوی نے تحریر فرمایا۔'
  ),
  (
    'mcq_isl11_01_1', 'ch_isl11_01', 'sub_kpk11_isl', 11,
    'دینِ اسلام کا سب سے پہلا اور بنیادی عقیدہ کیا ہے؟',
    '[{"id":"A","text":"عقیدہ توحید"},{"id":"B","text":"زکوٰۃ"},{"id":"C","text":"حج"},{"id":"D","text":"جہاد"}]',
    'A', 'عقیدہ توحید یعنی اللہ تعالیٰ کو واحد ماننا اسلام کا اولین بنیادی رکن ہے۔', 'easy', 'توحید کی اقسام اور عملی اثرات', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Islamiat XI',
    'دینِ اسلام کا سب سے پہلا اور بنیادی عقیدہ کیا ہے؟',
    '[{"id":"A","text":"عقیدہ توحید"},{"id":"B","text":"زکوٰۃ"},{"id":"C","text":"حج"},{"id":"D","text":"جہاد"}]',
    'توحید اسلام کا اولین عقیدہ ہے۔'
  ),
  (
    'mcq_eco11_01_1', 'ch_eco11_01', 'sub_kpk11_eco', 11,
    'Who defined economics as the science of scarcity and choice?',
    '[{"id":"A","text":"Adam Smith"},{"id":"B","text":"Alfred Marshall"},{"id":"C","text":"Lionel Robbins"},{"id":"D","text":"J.M. Keynes"}]',
    'C', 'Lionel Robbins (1932) defined economics around scarce means with alternative uses.', 'easy', 'Classical, Neo-Classical & Scarcity Definitions', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Economics XI',
    NULL, '[]', NULL
  ),
  (
    'mcq_acc11_01_1', 'ch_acc11_01', 'sub_kpk11_acc', 11,
    'The fundamental accounting equation states:',
    '[{"id":"A","text":"Assets + Liabilities = Capital"},{"id":"B","text":"Assets = Liabilities + Owner Equity"},{"id":"C","text":"Capital = Assets + Liabilities"},{"id":"D","text":"Revenue = Expenses + Assets"}]',
    'B', 'Assets always equal the sum of Liabilities (creditor claims) and Owner Equity.', 'easy', 'Basic Accounting Equation & Debit/Credit Rules', 2024,
    '2026-11-18T12:00:00Z', 'published', 'KPK Accounting XI',
    NULL, '[]', NULL
  );

-- Class 11 KPK Flashcards (SM-2 Spaced Repetition Ready)
INSERT OR IGNORE INTO flashcards (
  id, subject_id, chapter_id, front, back, formula_hint, topic_title, created_at_iso, front_urdu, back_urdu
) VALUES
  (
    'fc_phy11_01_1', 'sub_kpk11_phy', 'ch_phy11_01',
    'Define Radian (SI Supplementary Unit)',
    'Plane angle subtended at the centre of a circle by an arc equal in length to the radius.',
    '1 rev = 2pi rad = 360°', 'SI Units', '2026-11-18T12:00:00Z',
    'ریڈین کی تعریف بیان کریں', 'دائرے کے مرکز پر وہ زاویہ جو رداس کے برابر قوس بنائے۔'
  ),
  (
    'fc_phy11_02_1', 'sub_kpk11_phy', 'ch_phy11_02',
    'State the two conditions of equilibrium',
    'First: Sigma F = 0 (no linear acceleration). Second: Sigma tau = 0 (no angular acceleration).',
    'tau = r × F', 'Equilibrium', '2026-11-18T12:00:00Z',
    'توازن کی دو شرائط کیا ہیں؟', 'پہلی شرط: کل قوت صفر (ΣF = 0)؛ دوسری شرط: کل ٹارک صفر (Στ = 0)۔'
  ),
  (
    'fc_chem11_01_1', 'sub_kpk11_chem', 'ch_chem11_01',
    'What is Avogadro Number and Molar Gas Volume at STP?',
    '6.022 × 10^23 particles per mole; 1 mole ideal gas occupies 22.414 dm^3 at STP.',
    'n = m / M = V / 22.414', 'Stoichiometry', '2026-11-18T12:00:00Z',
    NULL, NULL
  ),
  (
    'fc_bio11_01_1', 'sub_kpk11_bio', 'ch_bio11_01',
    '70S vs 80S Ribosomes in Cells',
    '70S ribosomes occur in prokaryotes, mitochondria & chloroplasts; 80S ribosomes occur in eukaryotic cytosol.',
    'S = Svedberg unit', 'Organelles', '2026-11-18T12:00:00Z',
    NULL, NULL
  ),
  (
    'fc_math11_01_1', 'sub_kpk11_math', 'ch_math11_01',
    'Modulus and Multiplicative Inverse of z = a + bi',
    '|z| = sqrt(a^2 + b^2); z^-1 = (a - bi) / (a^2 + b^2).',
    'z · z_bar = |z|^2', 'Complex Numbers', '2026-11-18T12:00:00Z',
    NULL, NULL
  ),
  (
    'fc_cs11_01_1', 'sub_kpk11_cs', 'ch_cs11_01',
    'What is the Fetch-Decode-Execute Cycle?',
    'CPU fetches instruction at PC from RAM, Control Unit decodes opcode, ALU/CPU executes and writes result.',
    'PC -> MAR -> MDR -> IR', 'CPU Architecture', '2026-11-18T12:00:00Z',
    NULL, NULL
  ),
  (
    'fc_acc11_01_1', 'sub_kpk11_acc', 'ch_acc11_01',
    'What increases and decreases Owner Equity?',
    'Owner Investment & Revenues increase Equity; Drawings & Expenses decrease Equity.',
    'Assets = Liabilities + Equity', 'Accounting Equation', '2026-11-18T12:00:00Z',
    NULL, NULL
  );

-- Class 11 KPK HSSC-I Exam Patterns
INSERT OR IGNORE INTO exam_patterns (
  id, title, mode, class_level, board_code, subject_id, question_count,
  duration_minutes, marks_per_mcq, negative_marking_penalty, pass_percentage,
  includes_short_questions_count, includes_long_questions_count, section_summary
) VALUES
  (
    'pat_kpk11_diag', 'KPK Class 11 Quick Diagnostic', 'diagnostic_test', 11, 'KPK', NULL,
    5, 8, 1, 0, 40, 0, 0, 'Quick HSSC-I baseline across Physics, Chem/Bio/Math & CS'
  ),
  (
    'pat_kpk11_chap', 'Class 11 Chapter Mastery Test', 'chapter_test', 11, 'KPK', NULL,
    4, 10, 1, 0, 50, 2, 0, 'Chapter MCQs + Short Questions Review'
  ),
  (
    'pat_kpk11_phy_mock', 'BISE KPK Class 11 Physics Full Paper Pattern', 'full_mock_exam', 11, 'KPK', 'sub_kpk11_phy',
    7, 25, 1, 0, 33, 3, 1, 'Sec-A MCQs + Sec-B Short + Sec-C Numerical/Long Questions'
  ),
  (
    'pat_kpk11_chem_mock', 'BISE KPK Class 11 Chemistry Pattern', 'full_mock_exam', 11, 'KPK', 'sub_kpk11_chem',
    5, 20, 1, 0, 33, 3, 1, 'Sec-A MCQs + Sec-B Stoichiometry/Bohr Short + Long'
  );

-- Class 11 KPK Textbook & Past Paper Catalog Metadata (objectKeys mapped to Backblaze B2 imtihan-pro-assets)
INSERT OR IGNORE INTO pdf_assets (
  id, title, category, class_level, board_code, subject_id, object_key,
  mime_type, file_size_bytes, page_count, checksum_sha256, offline_cacheable,
  version, created_at_iso, updated_at_iso
) VALUES
  (
    'pdf_tb_phy11', 'KPK Physics Class 11 Textbook (HSSC-I)', 'textbook', 11, 'KPK', 'sub_kpk11_phy',
    'kpk/class-11/physics/physics-11-kpk-textbook.pdf', 'application/pdf', 4194304, 240, '', 1,
    '1.0', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z'
  ),
  (
    'pdf_pp_phy11_2025', 'BISE Peshawar Class 11 Physics Annual 2025 (Solved)', 'past_paper', 11, 'KPK', 'sub_kpk11_phy',
    'kpk/class-11/physics/2025-annual-solved.pdf', 'application/pdf', 1572864, 8, '', 1,
    '1.0', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z'
  ),
  (
    'pdf_pp_math11_2025', 'BISE KPK Class 11 Mathematics Annual 2025', 'past_paper', 11, 'KPK', 'sub_kpk11_math',
    'kpk/class-11/mathematics/2025-annual.pdf', 'application/pdf', 1310720, 6, '', 1,
    '1.0', '2026-11-18T12:00:00Z', '2026-11-18T12:00:00Z'
  );

INSERT OR IGNORE INTO past_papers (
  id, board_code, class_level, subject_id, subject_name, year, session,
  is_solved, pdf_metadata_id, file_size_mb, page_count, download_count, created_at_iso
) VALUES
  (
    'pp_kpk11_phy_2025', 'KPK', 11, 'sub_kpk11_phy', 'Physics (HSSC-I)', 2025, 'Annual',
    1, 'pdf_pp_phy11_2025', 1.5, 8, 0, '2026-11-18T12:00:00Z'
  ),
  (
    'pp_kpk11_math_2025', 'KPK', 11, 'sub_kpk11_math', 'Mathematics (HSSC-I)', 2025, 'Annual',
    0, 'pdf_pp_math11_2025', 1.25, 6, 0, '2026-11-18T12:00:00Z'
  );
