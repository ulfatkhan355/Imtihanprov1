-- Imtihan Pro Stage 2 — Idempotent Initial Sample Curriculum Seed (KPK Focus)
-- Contains NO admin credentials or fake user activity telemetry.
-- Admin users are initialized securely via POST /v1/admin/bootstrap with ADMIN_BOOTSTRAP_SECRET.

INSERT OR IGNORE INTO boards (id, code, name, region, is_primary_initial_focus, is_active, supported_classes_json) VALUES
  ('board_kpk', 'KPK', 'Khyber Pakhtunkhwa (KPK) Boards', 'Peshawar, Mardan, Abbottabad, Swat, Kohat, Bannu, DI Khan, Malakand', 1, 1, '[9,10,11,12]'),
  ('board_federal', 'FEDERAL', 'Federal Board (FBISE)', 'Islamabad & Federal Territories', 0, 0, '[9,10,11,12]'),
  ('board_punjab', 'PUNJAB', 'Punjab Curriculum & Textbook Boards', 'Lahore, Rawalpindi, Multan, Faisalabad', 0, 0, '[9,10,11,12]'),
  ('board_sindh', 'SINDH', 'Sindh Boards (BSEK / BISE)', 'Karachi, Hyderabad, Sukkur', 0, 0, '[9,10,11,12]'),
  ('board_balochistan', 'BALOCHISTAN', 'Balochistan Board (BBISE)', 'Quetta & Balochistan Region', 0, 0, '[9,10,11,12]');

INSERT OR IGNORE INTO academic_classes (id, level, label, board_id, board_code, academic_year, coverage_status, notes) VALUES
  ('cls_kpk_9', 9, 'Class 9 (SSC Part I)', 'board_kpk', 'KPK', '2026-2027', 'active_seed', 'Sample KPK Class 9 Physics & Mathematics structure seeded.'),
  ('cls_kpk_10', 10, 'Class 10 (SSC Part II)', 'board_kpk', 'KPK', '2026-2027', 'active_seed', 'Primary initial sample dataset covering Class 10 KPK subjects, Physics SHM, Chemistry Equilibrium & Maths.'),
  ('cls_kpk_11', 11, 'Class 11 (HSSC Part I)', 'board_kpk', 'KPK', '2026-2027', 'structure_ready', 'Academic structure configured; ready for editorial publishing or AI draft ingestion.'),
  ('cls_kpk_12', 12, 'Class 12 (HSSC Part II)', 'board_kpk', 'KPK', '2026-2027', 'structure_ready', 'Academic structure configured; ready for editorial publishing or AI draft ingestion.');

INSERT OR IGNORE INTO subjects (id, class_id, class_level, board_code, code, name, icon_key, accent_hex, total_chapters, published_chapters, is_offline_pack_available, coverage_status, updated_at_iso) VALUES
  ('sub_kpk10_phy', 'cls_kpk_10', 10, 'KPK', 'PHY-10', 'Physics', 'physics', '#3B82F6', 9, 2, 1, 'active_seed', '2026-11-12T10:30:00Z'),
  ('sub_kpk10_chem', 'cls_kpk_10', 10, 'KPK', 'CHM-10', 'Chemistry', 'chemistry', '#8B5CF6', 8, 1, 1, 'active_seed', '2026-11-11T15:20:00Z'),
  ('sub_kpk10_math', 'cls_kpk_10', 10, 'KPK', 'MTH-10', 'Mathematics', 'math', '#0EA5E9', 13, 1, 1, 'active_seed', '2026-11-10T09:15:00Z'),
  ('sub_kpk10_bio', 'cls_kpk_10', 10, 'KPK', 'BIO-10', 'Biology', 'biology', '#22C55E', 9, 0, 0, 'active_seed', '2026-11-09T14:00:00Z'),
  ('sub_kpk10_eng', 'cls_kpk_10', 10, 'KPK', 'ENG-10', 'English', 'english', '#60A5FA', 12, 0, 0, 'structure_ready', '2026-11-08T11:45:00Z'),
  ('sub_kpk10_urd', 'cls_kpk_10', 10, 'KPK', 'URD-10', 'Urdu', 'urdu', '#10B981', 10, 0, 0, 'structure_ready', '2026-11-07T16:10:00Z'),
  ('sub_kpk9_phy', 'cls_kpk_9', 9, 'KPK', 'PHY-09', 'Physics', 'physics', '#3B82F6', 9, 1, 1, 'active_seed', '2026-11-06T09:00:00Z'),
  ('sub_kpk9_math', 'cls_kpk_9', 9, 'KPK', 'MTH-09', 'Mathematics', 'math', '#0EA5E9', 17, 0, 0, 'structure_ready', '2026-11-06T09:00:00Z'),
  ('sub_kpk11_phy', 'cls_kpk_11', 11, 'KPK', 'PHY-11', 'Physics (HSSC-I)', 'physics', '#3B82F6', 11, 0, 0, 'structure_ready', '2026-11-01T08:00:00Z'),
  ('sub_kpk12_math', 'cls_kpk_12', 12, 'KPK', 'MTH-12', 'Mathematics (HSSC-II)', 'math', '#0EA5E9', 7, 0, 0, 'structure_ready', '2026-11-01T08:00:00Z');

INSERT OR IGNORE INTO chapters (id, subject_id, chapter_number, title, subtitle, topics_json, current_version, publish_status, has_detailed_notes, has_revision_notes, mcq_count, solved_questions_count, is_locked_by_default, estimated_size_kb, updated_at_iso) VALUES
  ('ch_phy10_01', 'sub_kpk10_phy', 10, 'Simple Harmonic Motion & Waves', 'Oscillatory motion, Hooke''s law, pendulum period & wave equation', '[{"id":"t1","chapterId":"ch_phy10_01","orderIndex":1,"title":"Restoring Force & SHM","estimatedMinutes":18},{"id":"t2","chapterId":"ch_phy10_01","orderIndex":2,"title":"Simple Pendulum Time Period","estimatedMinutes":22},{"id":"t3","chapterId":"ch_phy10_01","orderIndex":3,"title":"Ripple Tank & Wave Equation","estimatedMinutes":20}]', 'v1.4.0', 'published', 1, 1, 5, 3, 0, 420, '2026-11-12T10:30:00Z'),
  ('ch_phy10_02', 'sub_kpk10_phy', 11, 'Sound', 'Longitudinal acoustic waves, intensity, decibels & echoes', '[{"id":"t4","chapterId":"ch_phy10_02","orderIndex":1,"title":"Characteristics of Sound","estimatedMinutes":16},{"id":"t5","chapterId":"ch_phy10_02","orderIndex":2,"title":"Speed of Sound & Echo","estimatedMinutes":19}]', 'v1.1.0', 'published', 1, 1, 2, 1, 0, 360, '2026-11-11T14:15:00Z'),
  ('ch_phy10_03', 'sub_kpk10_phy', 12, 'Geometrical Optics', 'Reflection, spherical mirrors, refraction & lenses', '[{"id":"t6","chapterId":"ch_phy10_03","orderIndex":1,"title":"Spherical Mirrors Formula","estimatedMinutes":24},{"id":"t7","chapterId":"ch_phy10_03","orderIndex":2,"title":"Total Internal Reflection","estimatedMinutes":18}]', 'v0.9.0', 'in_review', 1, 0, 0, 0, 1, 480, '2026-11-12T08:40:00Z'),
  ('ch_phy10_04', 'sub_kpk10_phy', 13, 'Electrostatics', 'Coulomb law, electric field & capacitors', '[]', 'v0.5.0', 'draft', 0, 0, 0, 0, 1, 250, '2026-11-10T09:00:00Z'),
  ('ch_phy9_02', 'sub_kpk9_phy', 2, 'Kinematics', 'Types of motion, scalar/vector quantities & equations of motion', '[{"id":"t8","chapterId":"ch_phy9_02","orderIndex":1,"title":"Translatory, Rotatory & Vibratory Motion","estimatedMinutes":15},{"id":"t9","chapterId":"ch_phy9_02","orderIndex":2,"title":"Equations of Motion","estimatedMinutes":25}]', 'v1.2.0', 'published', 1, 1, 1, 1, 0, 390, '2026-11-10T12:00:00Z'),
  ('ch_math10_01', 'sub_kpk10_math', 1, 'Quadratic Equations', 'Factorization, completing square, and quadratic formula', '[{"id":"t10","chapterId":"ch_math10_01","orderIndex":1,"title":"Standard Form & Factorization","estimatedMinutes":20},{"id":"t11","chapterId":"ch_math10_01","orderIndex":2,"title":"Quadratic Formula Derivation","estimatedMinutes":25}]', 'v1.3.0', 'published', 1, 1, 2, 2, 0, 340, '2026-11-09T17:10:00Z'),
  ('ch_chem10_01', 'sub_kpk10_chem', 9, 'Chemical Equilibrium', 'Reversible reactions, Law of Mass Action & Kc', '[{"id":"t12","chapterId":"ch_chem10_01","orderIndex":1,"title":"Dynamic Equilibrium","estimatedMinutes":17}]', 'v1.0.0', 'published', 1, 1, 2, 1, 0, 310, '2026-11-08T13:20:00Z');

INSERT OR IGNORE INTO chapter_content_versions (id, chapter_id, mode, version, version_num, publish_status, language, blocks_json, author_or_source, content_sha256, published_at_iso, updated_at_iso) VALUES
  (
    'cnt_phy10_01_detailed_v140',
    'ch_phy10_01',
    'detailed_notes',
    'v1.4.0',
    1,
    'published',
    'en',
    '[{"id":"blk_1","orderIndex":1,"type":"heading","sectionCode":"10.1","text":"Simple Harmonic Motion (SHM)"},{"id":"blk_2","orderIndex":2,"type":"paragraph","text":"Simple Harmonic Motion is a special type of oscillatory motion in which the net restoring force is directly proportional to the displacement from the mean position and is always directed toward the mean position."},{"id":"blk_3","orderIndex":3,"type":"definition","term":"Restoring Force","definition":"A force that always pushes or pulls an oscillating object back toward its equilibrium (mean) position.","formulaHint":"F = -kx"},{"id":"blk_4","orderIndex":4,"type":"math_expression","latex":"a \\propto -x \\quad \\text{and} \\quad T = 2\\pi \\sqrt{\\frac{l}{g}}","plainNotation":"a ∝ -x   and   T = 2π √(l / g)","caption":"Fundamental acceleration condition and time period of a simple pendulum"},{"id":"blk_5","orderIndex":5,"type":"exam_point","title":"High-Yield KPK Board Exam Points","boardFrequency":"High","points":["At the mean position, velocity and kinetic energy are maximum while acceleration and potential energy are zero.","At extreme positions, acceleration and potential energy are maximum while velocity is zero.","The time period T of a simple pendulum is independent of the mass of the bob."]},{"id":"blk_6","orderIndex":6,"type":"table","title":"Comparison of Mechanical Waves","headers":["Property","Transverse Waves","Longitudinal Waves"],"rows":[["Particle Vibration","Perpendicular to wave propagation","Parallel to wave propagation"],["Wave Regions","Crests and Troughs","Compressions and Rarefactions"],["Example","Light / Water surface ripples","Sound waves in air"]]},{"id":"blk_7","orderIndex":7,"type":"example","title":"Worked Numerical 10.1","problemStatement":"Find the time period of a simple pendulum of length 1.0 m at a location where g = 9.8 m/s².","steps":["Given: l = 1.0 m, g = 9.8 m/s²","Formula: T = 2π √(l / g)","Substitute: T = 2 × 3.1416 × √(1.0 / 9.8) = 6.2832 × 0.3194"],"finalAnswer":"T ≈ 2.01 seconds"},{"id":"blk_8","orderIndex":8,"type":"list","ordered":false,"title":"Necessary Conditions for SHM","items":["The system must possess inertia.","A restoring force must act toward the equilibrium point.","Friction and damping forces are ideally negligible."]},{"id":"blk_9","orderIndex":9,"type":"solved_question","questionType":"short","marks":4,"yearReference":"BISE Peshawar 2022","question":"Why does the time period of a simple pendulum not change if we double the mass of the metallic bob?","answerSteps":["The restoring force acting on the bob is F = -mg sinθ, which is proportional to mass m.","By Newton''s second law, acceleration a = F/m = -g sinθ, so mass m cancels out.","Hence T = 2π √(l/g) depends only on length l and gravitational acceleration g."]}]',
    'KPK Sample Curriculum Seed',
    'seed_phy10_01_v140',
    '2026-11-12T10:30:00Z',
    '2026-11-12T10:30:00Z'
  ),
  (
    'cnt_phy10_02_detailed_v110',
    'ch_phy10_02',
    'detailed_notes',
    'v1.1.0',
    1,
    'published',
    'en',
    '[{"id":"blk_s1","orderIndex":1,"type":"heading","sectionCode":"11.1","text":"Production & Propagation of Sound"},{"id":"blk_s2","orderIndex":2,"type":"paragraph","text":"Sound is a longitudinal mechanical wave produced by vibrating bodies and requires a material medium (solid, liquid, or gas) for propagation."},{"id":"blk_s3","orderIndex":3,"type":"math_expression","latex":"\\beta = 10 \\log_{10} \\left(\\frac{I}{I_0}\\right) \\text{ dB}","plainNotation":"β = 10 log10(I / I0) dB","caption":"Sound intensity level in decibels (I0 = 10^-12 W/m²)"},{"id":"blk_s4","orderIndex":4,"type":"solved_question","questionType":"numerical","marks":4,"yearReference":"BISE Abbottabad 2021","question":"What is the minimum distance required to hear a distinct echo at 20°C if sound speed is 344 m/s?","answerSteps":["Minimum persistence of hearing in human ear is 0.1 s.","Total distance = v × t = 344 × 0.1 = 34.4 m.","One-way distance to reflecting obstacle = 34.4 / 2 = 17.2 m."]}]',
    'KPK Sample Curriculum Seed',
    'seed_phy10_02_v110',
    '2026-11-11T14:15:00Z',
    '2026-11-11T14:15:00Z'
  ),
  (
    'cnt_math10_01_detailed_v130',
    'ch_math10_01',
    'detailed_notes',
    'v1.3.0',
    1,
    'published',
    'en',
    '[{"id":"blk_m1","orderIndex":1,"type":"heading","sectionCode":"1.1","text":"Standard Quadratic Equation"},{"id":"blk_m2","orderIndex":2,"type":"definition","term":"Quadratic Equation","definition":"A second-degree polynomial equation in one variable x of the form ax² + bx + c = 0 where a ≠ 0.","formulaHint":"ax² + bx + c = 0"},{"id":"blk_m3","orderIndex":3,"type":"math_expression","latex":"x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}","plainNotation":"x = (-b ± √(b² - 4ac)) / (2a)","caption":"The Quadratic Formula"}]',
    'KPK Sample Curriculum Seed',
    'seed_math10_01_v130',
    '2026-11-09T17:10:00Z',
    '2026-11-09T17:10:00Z'
  ),
  (
    'cnt_chem10_01_detailed_v100',
    'ch_chem10_01',
    'detailed_notes',
    'v1.0.0',
    1,
    'published',
    'en',
    '[{"id":"blk_c1","orderIndex":1,"type":"heading","sectionCode":"9.1","text":"Dynamic Chemical Equilibrium"},{"id":"blk_c2","orderIndex":2,"type":"paragraph","text":"In a reversible chemical reaction inside a closed container, dynamic equilibrium is reached when the rate of the forward reaction equals the rate of the reverse reaction."},{"id":"blk_c3","orderIndex":3,"type":"math_expression","latex":"K_c = \\frac{[C]^c[D]^d}{[A]^a[B]^b}","plainNotation":"Kc = ([C]^c [D]^d) / ([A]^a [B]^b)","caption":"Equilibrium constant expression for aA + bB ⇌ cC + dD"}]',
    'KPK Sample Curriculum Seed',
    'seed_chem10_01_v100',
    '2026-11-08T13:20:00Z',
    '2026-11-08T13:20:00Z'
  );

INSERT OR IGNORE INTO mcqs (id, chapter_id, subject_id, class_level, question, options_json, correct_option_id, explanation, difficulty, topic_title, past_paper_year, created_at_iso) VALUES
  ('mcq_1', 'ch_phy10_01', 'sub_kpk10_phy', 10, 'What is the value of acceleration due to gravity on Earth?', '[{"id":"A","text":"9.8 m/s²"},{"id":"B","text":"9.8 km/s²"},{"id":"C","text":"10 m/s"},{"id":"D","text":"9.6 m/s²"}]', 'A', 'Standard acceleration due to gravity near Earth''s surface is 9.8 m/s².', 'easy', 'Simple Pendulum', 2022, '2026-11-12T10:30:00Z'),
  ('mcq_2', 'ch_phy10_01', 'sub_kpk10_phy', 10, 'In Simple Harmonic Motion, the acceleration of a body is always directed:', '[{"id":"A","text":"Away from mean position"},{"id":"B","text":"Toward the mean position"},{"id":"C","text":"Perpendicular to displacement"},{"id":"D","text":"Along the tangent only"}]', 'B', 'By Hooke''s law, a ∝ -x, meaning acceleration always points toward equilibrium.', 'easy', 'SHM', 2021, '2026-11-12T10:30:00Z'),
  ('mcq_3', 'ch_phy10_01', 'sub_kpk10_phy', 10, 'Which formula relates wave speed v, frequency f, and wavelength λ?', '[{"id":"A","text":"v = f / λ"},{"id":"B","text":"v = λ / f"},{"id":"C","text":"v = fλ"},{"id":"D","text":"v = f²λ"}]', 'C', 'Wave speed equals frequency multiplied by wavelength: v = fλ.', 'easy', 'Waves', 2020, '2026-11-12T10:30:00Z'),
  ('mcq_4', 'ch_phy10_01', 'sub_kpk10_phy', 10, 'If the length of a simple pendulum is quadrupled, its time period becomes:', '[{"id":"A","text":"Half"},{"id":"B","text":"Double"},{"id":"C","text":"Four times"},{"id":"D","text":"Unchanged"}]', 'B', 'Since T ∝ √l, multiplying length by 4 multiplies T by √4 = 2.', 'medium', 'Simple Pendulum', 2023, '2026-11-12T10:30:00Z'),
  ('mcq_5', 'ch_phy10_01', 'sub_kpk10_phy', 10, 'At the extreme position of an oscillating pendulum, which quantity is zero?', '[{"id":"A","text":"Acceleration"},{"id":"B","text":"Potential Energy"},{"id":"C","text":"Velocity"},{"id":"D","text":"Restoring Force"}]', 'C', 'The bob momentarily stops at each extreme point, so instantaneous velocity is zero.', 'medium', 'SHM', 2022, '2026-11-12T10:30:00Z'),
  ('mcq_s1', 'ch_phy10_02', 'sub_kpk10_phy', 10, 'The human audible frequency range is approximately:', '[{"id":"A","text":"2 Hz to 200 Hz"},{"id":"B","text":"20 Hz to 20,000 Hz"},{"id":"C","text":"200 Hz to 200,000 Hz"},{"id":"D","text":"Above 20 kHz only"}]', 'B', 'Human ears typically perceive frequencies between 20 Hz and 20 kHz.', 'easy', 'Sound', 2021, '2026-11-11T14:15:00Z'),
  ('mcq_m1', 'ch_math10_01', 'sub_kpk10_math', 10, 'The discriminant of ax² + bx + c = 0 is:', '[{"id":"A","text":"b² - 4ac"},{"id":"B","text":"b² + 4ac"},{"id":"C","text":"-b / a"},{"id":"D","text":"c / a"}]', 'A', 'The expression b² - 4ac under the radical determines the nature of roots.', 'easy', 'Quadratic Formula', 2022, '2026-11-09T17:10:00Z'),
  ('mcq_c1', 'ch_chem10_01', 'sub_kpk10_chem', 10, 'At dynamic chemical equilibrium, the forward and reverse reaction rates are:', '[{"id":"A","text":"Both zero"},{"id":"B","text":"Equal"},{"id":"C","text":"Forward greater than reverse"},{"id":"D","text":"Reverse greater than forward"}]', 'B', 'Dynamic equilibrium means forward rate equals reverse rate with constant macroscopic concentrations.', 'easy', 'Dynamic Equilibrium', 2021, '2026-11-08T13:20:00Z');

INSERT OR IGNORE INTO pdf_assets (id, title, category, class_level, board_code, subject_id, object_key, mime_type, file_size_bytes, page_count, checksum_sha256, offline_cacheable, version, created_at_iso, updated_at_iso) VALUES
  ('pdf_001', 'KPK Class 10 Mathematics — 2019 Solved Annual Paper (Catalog Seed)', 'past_paper', 10, 'KPK', 'sub_kpk10_math', 'kpk/class-10/math/2019-annual-solved.pdf', 'application/pdf', 2621440, 12, 'seed_pending_b2_upload_001', 1, '1.0', '2026-11-10T09:00:00Z', '2026-11-10T09:00:00Z'),
  ('pdf_002', 'KPK Class 10 Physics — 2020 Original Board Paper (Catalog Seed)', 'past_paper', 10, 'KPK', 'sub_kpk10_phy', 'kpk/class-10/physics/2020-annual-original.pdf', 'application/pdf', 1887436, 8, 'seed_pending_b2_upload_002', 1, '1.0', '2026-11-10T09:00:00Z', '2026-11-10T09:00:00Z');

INSERT OR IGNORE INTO past_papers (id, board_code, class_level, subject_id, subject_name, year, session, is_solved, pdf_metadata_id, file_size_mb, page_count, download_count, created_at_iso) VALUES
  ('pp_math_2019', 'KPK', 10, 'sub_kpk10_math', 'Mathematics', 2019, 'Annual', 1, 'pdf_001', 2.5, 12, 0, '2026-11-10T09:00:00Z'),
  ('pp_phy_2020', 'KPK', 10, 'sub_kpk10_phy', 'Physics', 2020, 'Annual', 0, 'pdf_002', 1.8, 8, 0, '2026-11-10T09:00:00Z');

INSERT OR IGNORE INTO app_settings (setting_key, setting_json, updated_at_iso) VALUES
  ('platform_policy', '{"guestFirstMandatory":true,"allowOptionalGoogleSync":true,"defaultBoardCode":"KPK","offlineMaxCacheMb":200}', '2026-11-14T00:00:00Z');
