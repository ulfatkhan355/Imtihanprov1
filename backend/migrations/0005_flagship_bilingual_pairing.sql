-- Imtihan Pro Stage 9 Flagship — Bilingual Urdu Nastaliq, BISE Pairing Blueprints & Night-Before Summaries
-- Zero-budget, strictly additive D1 migration (safe for live production databases).

ALTER TABLE subjects ADD COLUMN pairing_scheme_summary TEXT NOT NULL DEFAULT '';
ALTER TABLE subjects ADD COLUMN offline_pack_size_mb REAL NOT NULL DEFAULT 0.6;

ALTER TABLE chapters ADD COLUMN title_urdu TEXT NOT NULL DEFAULT '';
ALTER TABLE chapters ADD COLUMN subtitle_urdu TEXT NOT NULL DEFAULT '';
ALTER TABLE chapters ADD COLUMN board_weightage_marks INTEGER NOT NULL DEFAULT 0;
ALTER TABLE chapters ADD COLUMN pairing_hint TEXT NOT NULL DEFAULT '';
ALTER TABLE chapters ADD COLUMN night_before_summary TEXT NOT NULL DEFAULT '';

ALTER TABLE mcqs ADD COLUMN question_urdu TEXT NOT NULL DEFAULT '';
ALTER TABLE mcqs ADD COLUMN options_urdu_json TEXT NOT NULL DEFAULT '[]';
ALTER TABLE mcqs ADD COLUMN explanation_urdu TEXT NOT NULL DEFAULT '';

ALTER TABLE flashcards ADD COLUMN front_urdu TEXT NOT NULL DEFAULT '';
ALTER TABLE flashcards ADD COLUMN back_urdu TEXT NOT NULL DEFAULT '';

-- Enrich subjects with official BISE KPK 2026 pairing scheme & lightweight offline bundle sizes
UPDATE subjects
SET pairing_scheme_summary = 'BISE KPK SSC-I Physics: 12 MCQs (12 Marks) · Sec-B Short 8/11 (32 Marks) · Sec-C Long 2/3 (21 Marks). High yield: Ch 1, 2, 3 & 6.',
    offline_pack_size_mb = 0.8
WHERE id = 'sub_kpk9_phy';

UPDATE subjects
SET pairing_scheme_summary = 'BISE KPK SSC-I Maths: 15 MCQs (15 Marks) · Sec-B Short 9/12 (36 Marks) · Sec-C Theorems & Long 3/5 (24 Marks).',
    offline_pack_size_mb = 0.9
WHERE id = 'sub_kpk9_math';

UPDATE subjects
SET pairing_scheme_summary = 'BISE KPK SSC-I Chemistry: 12 MCQs (12 Marks) · Sec-B Short 8/11 (32 Marks) · Sec-C Long 2/3 (21 Marks).',
    offline_pack_size_mb = 0.7
WHERE id = 'sub_kpk9_chem';

UPDATE subjects
SET pairing_scheme_summary = 'BISE KPK SSC-I Biology: 12 MCQs (12 Marks) · Sec-B Short 8/11 (32 Marks) · Sec-C Long 2/3 (21 Marks).',
    offline_pack_size_mb = 0.7
WHERE id = 'sub_kpk9_bio';

UPDATE subjects
SET pairing_scheme_summary = 'BISE KPK SSC-I CS: 10 MCQs (10 Marks) · Sec-B Short 6/9 (24 Marks) · Sec-C Long 2/3 (16 Marks).',
    offline_pack_size_mb = 0.5
WHERE id = 'sub_kpk9_cs';

UPDATE subjects
SET pairing_scheme_summary = 'BISE KPK SSC-II Physics: 12 MCQs (12 Marks) · Sec-B Short 8/11 (32 Marks) · Sec-C Long 2/3 (21 Marks).',
    offline_pack_size_mb = 0.9
WHERE id = 'sub_kpk10_phy';

UPDATE subjects
SET pairing_scheme_summary = 'BISE KPK SSC-II Maths: 15 MCQs (15 Marks) · Sec-B Short 9/12 (36 Marks) · Sec-C Long 3/5 (24 Marks).',
    offline_pack_size_mb = 0.9
WHERE id = 'sub_kpk10_math';

UPDATE subjects
SET pairing_scheme_summary = 'BISE KPK SSC-II Chemistry: 12 MCQs (12 Marks) · Sec-B Short 8/11 (32 Marks) · Sec-C Long 2/3 (21 Marks).',
    offline_pack_size_mb = 0.7
WHERE id = 'sub_kpk10_chem';

-- Enrich seeded chapters with Urdu Nastaliq titles, BISE marks weightage, pairing hints, and Night-Before Exam summaries
UPDATE chapters SET
  title_urdu = 'طبیعی مقداریں اور پیمائش',
  subtitle_urdu = 'ایس آئی بنیادی اکائیاں، سائنسی ترقیم، ورنیئر کیلیپرز اور اہم ہندسے',
  board_weightage_marks = 8,
  pairing_hint = '1–2 MCQs + 1 Short Question (Vernier LC / Significant Figures)',
  night_before_summary = 'Memorize all 7 SI Base Units (m, kg, s, A, K, cd, mol), prefixes micro (10⁻⁶) & nano (10⁻⁹), Vernier LC = 0.01 cm, Screw Gauge LC = 0.001 cm, and 4 significant-figure rules.'
WHERE id = 'ch_phy9_01';

UPDATE chapters SET
  title_urdu = 'کائینی میٹکس (حرکت کا بیان)',
  subtitle_urdu = 'حرکت کی اقسام، سکیلر اور ویکٹر مقداریں، مساواتِ حرکت',
  board_weightage_marks = 10,
  pairing_hint = '2 MCQs + 1 Short + Paired Long Question (2nd or 3rd Equation Derivation)',
  night_before_summary = 'Slope of distance-time = speed; slope of velocity-time = acceleration; area under v-t graph = distance. Practice derivations of S = vit + ½at² and 2aS = vf² - vi².'
WHERE id = 'ch_phy9_02';

UPDATE chapters SET
  title_urdu = 'میٹرکس اور ڈٹرمیننٹ',
  subtitle_urdu = 'میٹرکس کا مرتبہ، ضرب، مقطع اور کریمر کا اصول',
  board_weightage_marks = 9,
  pairing_hint = '2 MCQs + 1 Short + 1 Long Numerical (Cramer''s Rule / Matrix Inversion)',
  night_before_summary = 'Singular matrix iff |A| = ad - bc = 0. A⁻¹ = Adj(A)/|A|. Always verify (AB)⁻¹ = B⁻¹A⁻¹ and Cramer''s rule determinants Ax, Ay.'
WHERE id = 'ch_math9_01';

UPDATE chapters SET
  title_urdu = 'حقیقی اور کمپلیکس اعداد',
  subtitle_urdu = 'حقیقی اعداد کے خواص، جذور اور کمپلیکس کا زوج (Conjugate)',
  board_weightage_marks = 7,
  pairing_hint = '2 MCQs + 1 Short Question (Simplify iⁿ or Conjugate)',
  night_before_summary = 'Remember i² = -1, i³ = -i, i⁴ = 1. Break high powers iⁿ using n = 4q + r. Conjugate of a + bi is a - bi.'
WHERE id = 'ch_math9_02';

UPDATE chapters SET
  title_urdu = 'کیمیاء کے بنیادی اصول',
  subtitle_urdu = 'کیمیاء کی شاخیں، تجرباتی و سالماتی فارمولا، مول اور ایووگیڈرو نمبر',
  board_weightage_marks = 9,
  pairing_hint = '2 MCQs + 1 Short + Mole Numerical',
  night_before_summary = 'Master n = mass / molar mass and particles N = n × 6.022 × 10²³. Empirical formula is simplest whole-number ratio; Molecular formula = n × Empirical.'
WHERE id = 'ch_chem9_01';

UPDATE chapters SET
  title_urdu = 'حیاتیات کا تعارف',
  subtitle_urdu = 'حیاتیات کی شاخیں، خلیاتی تنظیم اور مسلم سائنسدان',
  board_weightage_marks = 7,
  pairing_hint = '2 MCQs + 1 Short Question (Muslim Scientists / Tissue vs Cell)',
  night_before_summary = 'Revise Botany, Zoology, Microbiology, Histology (tissues), and contributions of Jabir Bin Hayan, Abdul Malik Asmai, and Bu Ali Sina.'
WHERE id = 'ch_bio9_01';

UPDATE chapters SET
  title_urdu = 'مسائل کا حل اور الگورتھم',
  subtitle_urdu = 'الگورتھم کی خصوصیات، فلو چارٹ کی علامات اور ٹریس ٹیبل',
  board_weightage_marks = 9,
  pairing_hint = '2 MCQs + 1 Short + 1 Flowchart / Algorithm Question',
  night_before_summary = 'Oval = Start/Stop, Parallelogram = Input/Output, Rectangle = Process, Diamond = Decision. Practice trace tables for loops and max-of-3 problems.'
WHERE id = 'ch_cs9_01';

UPDATE chapters SET
  title_urdu = 'حضرت محمد ﷺ: رواداری کا مثالی نمونہ',
  subtitle_urdu = 'تفہیم، مترادفات اور گرامر کی مشق',
  board_weightage_marks = 8,
  pairing_hint = '2 MCQs + Comprehension Short Questions + Abstract/Collective Noun',
  night_before_summary = 'Focus on tolerance (Burdbaari), Conquest of Makkah amnesty message, vocabulary synonyms (tolerance/forbearance), and active-to-passive conversion.'
WHERE id = 'ch_eng9_01';

UPDATE chapters SET
  title_urdu = 'سادہ ہارمونک موشن اور لہریں',
  subtitle_urdu = 'ارتعاشی حرکت، ہک کا قانون، سادہ پینڈولم اور لہر کی مساوات',
  board_weightage_marks = 11,
  pairing_hint = '2 MCQs + 1 Short + 1 Long (Simple Pendulum T = 2π√(l/g) or Ripple Tank)',
  night_before_summary = 'F = -kx, a ∝ -x. At mean position: v and KE max, a = 0. At extreme: a and PE max, v = 0. T = 2π√(l/g) is independent of bob mass; v = fλ.'
WHERE id = 'ch_phy10_01';

UPDATE chapters SET
  title_urdu = 'آواز (ساؤنڈ)',
  subtitle_urdu = 'طولی لہریں، شدت، ڈیسی بل اور گونج (Echo)',
  board_weightage_marks = 9,
  pairing_hint = '1–2 MCQs + 1 Short Numerical (Echo minimum distance = 17.2 m)',
  night_before_summary = 'Sound is a longitudinal mechanical wave requiring a medium. Audible range: 20 Hz to 20,000 Hz. β = 10 log₁₀(I/I₀) dB. Echo distance = vt/2 ≈ 17.2 m.'
WHERE id = 'ch_phy10_02';

UPDATE chapters SET
  title_urdu = 'دو درجی مساواتیں',
  subtitle_urdu = 'تجزی، تکمیلِ مربع اور دو درجی فارمولا',
  board_weightage_marks = 10,
  pairing_hint = '2 MCQs + 1 Short + 1 Long (Quadratic Formula / Equations Reducible)',
  night_before_summary = 'Standard form: ax² + bx + c = 0 (a ≠ 0). Discriminant Δ = b² - 4ac. Quadratic formula: x = (-b ± √(b² - 4ac)) / (2a).'
WHERE id = 'ch_math10_01';

UPDATE chapters SET
  title_urdu = 'کیمیائی توازن',
  subtitle_urdu = 'رجعت پذیر تعاملات، قانونِ کمیتِ عمل اور Kc',
  board_weightage_marks = 9,
  pairing_hint = '2 MCQs + 1 Short + 1 Long (Law of Mass Action & Kc Units)',
  night_before_summary = 'Dynamic equilibrium occurs when forward rate equals reverse rate inside a closed vessel. Kc = [Products]/[Reactants] with coefficients as exponents.'
WHERE id = 'ch_chem10_01';

-- Bilingual Urdu MCQ translations for priority Grade 9 & 10 MCQs
UPDATE mcqs SET
  question_urdu = 'مندرجہ ذیل میں سے کون سی ایس آئی بنیادی مقدار نہیں ہے؟',
  options_urdu_json = '[{"id":"A","text":"لمبائی"},{"id":"B","text":"کمیت"},{"id":"C","text":"قوت (فورس)"},{"id":"D","text":"وقت"}]',
  explanation_urdu = 'قوت (نیوٹن) ایک ماخوذ مقدار ہے، سات بنیادی ایس آئی مقداروں میں شامل نہیں ہے۔'
WHERE id = 'mcq_p91_1';

UPDATE mcqs SET
  question_urdu = 'معیاری ورنیئر کیلیپرز کا لیسٹ کاؤنٹ (کم سے کم پیمائش) کیا ہوتا ہے؟',
  options_urdu_json = '[{"id":"A","text":"0.1 سینٹی میٹر"},{"id":"B","text":"0.01 سینٹی میٹر"},{"id":"C","text":"0.001 سینٹی میٹر"},{"id":"D","text":"1.0 ملی میٹر"}]',
  explanation_urdu = 'معیاری ورنیئر کیلیپرز کا لیسٹ کاؤنٹ 0.1 ملی میٹر یا 0.01 سینٹی میٹر ہوتا ہے۔'
WHERE id = 'mcq_p91_2';

UPDATE mcqs SET
  question_urdu = 'ولاسٹی ٹائم گراف کا ڈھلوان (Slope) کس مقدار کو ظاہر کرتا ہے؟',
  options_urdu_json = '[{"id":"A","text":"ہٹاؤ"},{"id":"B","text":"سپیڈ"},{"id":"C","text":"اسراع (Acceleration)"},{"id":"D","text":"مومنٹم"}]',
  explanation_urdu = 'وقت کے لحاظ سے ولاسٹی میں تبدیلی کی شرح اسراع (Acceleration) کہلاتی ہے۔'
WHERE id = 'mcq_p92_2';

UPDATE mcqs SET
  question_urdu = 'وہ مربع میٹرکس جس کا مقطع (Determinant) صفر ہو کیا کہلاتا ہے؟',
  options_urdu_json = '[{"id":"A","text":"وحدانی میٹرکس"},{"id":"B","text":"غیر نادر میٹرکس"},{"id":"C","text":"نادر (Singular) میٹرکس"},{"id":"D","text":"ٹرانسپوز میٹرکس"}]',
  explanation_urdu = 'اگر |A| = 0 ہو تو میٹرکس نادر (Singular) کہلاتا ہے اور اس کا ضربی معکوس ممکن نہیں۔'
WHERE id = 'mcq_m91_2';

UPDATE mcqs SET
  question_urdu = 'زمین پر کششِ ثقل کے اسراع (g) کی معیاری قیمت کیا ہے؟',
  options_urdu_json = '[{"id":"A","text":"9.8 m/s²"},{"id":"B","text":"9.8 km/s²"},{"id":"C","text":"10 m/s"},{"id":"D","text":"9.6 m/s²"}]',
  explanation_urdu = 'سطحِ زمین کے قریب کششِ ثقل کا معیاری اسراع 9.8 میٹر فی سیکنڈ مربع ہے۔'
WHERE id = 'mcq_1';

UPDATE mcqs SET
  question_urdu = 'سادہ ہارمونک موشن میں جسم کا اسراع ہمیشہ کس سمت میں ہوتا ہے؟',
  options_urdu_json = '[{"id":"A","text":"وسطی مقام سے دور"},{"id":"B","text":"وسطی مقام کی طرف"},{"id":"C","text":"ہٹاؤ کے عموداً"},{"id":"D","text":"صرف مماس کے ساتھ"}]',
  explanation_urdu = 'ہک کے قانون a ∝ -x کے مطابق اسراع ہمیشہ توازن کے وسطی مقام کی طرف ہوتا ہے۔'
WHERE id = 'mcq_2';

UPDATE mcqs SET
  question_urdu = 'اگر سادہ پینڈولم کی لمبائی چار گنا کر دی جائے تو اس کا ٹائم پیریڈ ہو جائے گا:',
  options_urdu_json = '[{"id":"A","text":"آدھا"},{"id":"B","text":"دو گنا"},{"id":"C","text":"چار گنا"},{"id":"D","text":"تبدیل نہیں ہوگا"}]',
  explanation_urdu = 'چونکہ T ∝ √l، لمبائی 4 گنا کرنے سے ٹائم پیریڈ √4 = 2 گنا ہو جاتا ہے۔'
WHERE id = 'mcq_4';

-- Bilingual Urdu translations for flagship flashcards
UPDATE flashcards SET
  front_urdu = 'ورنیئر کیلیپرز کا لیسٹ کاؤنٹ',
  back_urdu = '0.1 ملی میٹر یا 0.01 سینٹی میٹر'
WHERE id = 'fc_p91_1';

UPDATE flashcards SET
  front_urdu = 'حرکت کی دوسری مساوات',
  back_urdu = 'ہٹاؤ S کو ابتدائی ولاسٹی vi، وقت t اور اسراع a سے جوڑتی ہے۔'
WHERE id = 'fc_p92_1';

UPDATE flashcards SET
  front_urdu = 'نادر اور غیر نادر میٹرکس',
  back_urdu = 'اگر |A| = 0 تو نادر (معکوس ناممکن)، اگر |A| ≠ 0 تو غیر نادر۔'
WHERE id = 'fc_m91_1';

UPDATE flashcards SET
  front_urdu = 'ایووگیڈرو نمبر (NA)',
  back_urdu = 'کسی بھی شے کے ایک مول میں 6.022 × 10²³ ذرات ہوتے ہیں۔'
WHERE id = 'fc_c91_1';

UPDATE flashcards SET
  front_urdu = 'سادہ پینڈولم کا ٹائم پیریڈ',
  back_urdu = 'لمبائی l اور کشش ثقل g پر منحصر ہے؛ گولے کی کمیت پر منحصر نہیں۔'
WHERE id = 'fc_p101_1';
