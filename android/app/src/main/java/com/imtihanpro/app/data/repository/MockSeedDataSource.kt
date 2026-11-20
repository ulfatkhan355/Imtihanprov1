package com.imtihanpro.app.data.repository

import com.imtihanpro.app.data.remote.ApiChapterDto
import com.imtihanpro.app.data.remote.ApiContentBlockDto
import com.imtihanpro.app.data.remote.ApiExamPatternDto
import com.imtihanpro.app.data.remote.ApiFlashcardDto
import com.imtihanpro.app.data.remote.ApiMcqQuestionDto
import com.imtihanpro.app.data.remote.ApiPastPaperDto
import com.imtihanpro.app.data.remote.ApiPdfMetadataDto
import com.imtihanpro.app.data.remote.ApiSubjectDto
import com.imtihanpro.app.data.remote.ApiTopicDto

/**
 * Deterministic KPK Board offline-first seed catalog.
 * Primary v1.0 launch focus: KPK Class 11 (HSSC-I) across Pre-Medical, Pre-Engineering,
 * ICS, Humanities (FA), and Commerce (I.Com), while preserving Class 9/10 fixtures for unit tests.
 */
object MockSeedDataSource {

    val subjects: List<ApiSubjectDto> = listOf(
        // Primary Starter Class 9 KPK English-medium subjects
        ApiSubjectDto(
            id = "sub_kpk9_phy",
            classLevel = 9,
            boardCode = "KPK",
            code = "PHY-09",
            name = "Physics",
            iconKey = "physics",
            totalChapters = 9,
            publishedChapters = 2,
            coverageStatus = "active_seed",
            studyGroups = listOf("SCIENCE_BIOLOGY", "SCIENCE_CS"),
            description = "KPK Textbook Board Class 9 Physics (Mechanics, Matter & Thermal Physics)",
            textbookPdfId = "pdf_tb_phy9",
            pairingSchemeSummary = "BISE KPK: 12 MCQs (12m) + Sec-B Short 8/11 (32m) + Sec-C Long 3/4 (21m) = 65 Marks",
            offlinePackSizeMb = 0.8
        ),
        ApiSubjectDto(
            id = "sub_kpk9_chem",
            classLevel = 9,
            boardCode = "KPK",
            code = "CHM-09",
            name = "Chemistry",
            iconKey = "chemistry",
            totalChapters = 8,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("SCIENCE_BIOLOGY", "SCIENCE_CS"),
            description = "Fundamentals of Chemistry, Atomic Structure & Bonding (Class 9 KPK)",
            textbookPdfId = "pdf_tb_chem9",
            pairingSchemeSummary = "BISE KPK: 12 MCQs + 8 Short Qs + 3 Long Pairs = 65 Marks Theory",
            offlinePackSizeMb = 0.7
        ),
        ApiSubjectDto(
            id = "sub_kpk9_math",
            classLevel = 9,
            boardCode = "KPK",
            code = "MTH-09",
            name = "Mathematics",
            iconKey = "math",
            totalChapters = 17,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("SCIENCE_BIOLOGY", "SCIENCE_CS", "ARTS_HUMANITIES"),
            description = "Matrices, Logarithms, Algebra & Geometry for KPK Class 9",
            textbookPdfId = "pdf_tb_math9",
            pairingSchemeSummary = "BISE KPK: 15 MCQs (15m) + Sec-B 9/12 Short (36m) + Sec-C Theorems & Long (24m) = 75 Marks",
            offlinePackSizeMb = 0.9
        ),
        ApiSubjectDto(
            id = "sub_kpk9_bio",
            classLevel = 9,
            boardCode = "KPK",
            code = "BIO-09",
            name = "Biology",
            iconKey = "biology",
            totalChapters = 9,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("SCIENCE_BIOLOGY"),
            description = "Introduction to Biology, Cells, Enzymes & Bioenergetics",
            pairingSchemeSummary = "BISE KPK: 12 MCQs + Sec-B Short 8/11 + Sec-C 3/4 Long = 65 Marks",
            offlinePackSizeMb = 0.7
        ),
        ApiSubjectDto(
            id = "sub_kpk9_cs",
            classLevel = 9,
            boardCode = "KPK",
            code = "CS-09",
            name = "Computer Science",
            iconKey = "cs",
            totalChapters = 6,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("SCIENCE_CS"),
            description = "Problem Solving, Number Systems, HTML & Networks (KPK Class 9)",
            pairingSchemeSummary = "BISE KPK: 10 MCQs + Sec-B Short (24m) + Sec-C Long (16m) = 50 Marks",
            offlinePackSizeMb = 0.6
        ),
        ApiSubjectDto(
            id = "sub_kpk9_eng",
            classLevel = 9,
            boardCode = "KPK",
            code = "ENG-09",
            name = "English",
            iconKey = "english",
            totalChapters = 14,
            publishedChapters = 0,
            coverageStatus = "structure_ready",
            studyGroups = listOf("SCIENCE_BIOLOGY", "SCIENCE_CS", "ARTS_HUMANITIES"),
            description = "KPK Class 9 English Comprehension, Grammar & Composition"
        ),

        // Class 10 KPK preserved sample subjects
        ApiSubjectDto("sub_kpk10_phy", 10, "KPK", "PHY-10", "Physics", "physics", 12, 3, "active_seed", listOf("SCIENCE_BIOLOGY", "SCIENCE_CS")),
        ApiSubjectDto("sub_kpk10_chem", 10, "KPK", "CHM-10", "Chemistry", "chemistry", 12, 1, "active_seed", listOf("SCIENCE_BIOLOGY", "SCIENCE_CS")),
        ApiSubjectDto("sub_kpk10_math", 10, "KPK", "MTH-10", "Mathematics", "math", 14, 2, "active_seed", listOf("SCIENCE_BIOLOGY", "SCIENCE_CS")),
        ApiSubjectDto("sub_kpk10_bio", 10, "KPK", "BIO-10", "Biology", "biology", 12, 1, "active_seed", listOf("SCIENCE_BIOLOGY")),
        ApiSubjectDto("sub_kpk10_eng", 10, "KPK", "ENG-10", "English", "english", 10, 1, "active_seed", listOf("SCIENCE_BIOLOGY", "SCIENCE_CS", "ARTS_HUMANITIES")),
        ApiSubjectDto("sub_kpk10_urd", 10, "KPK", "URD-10", "Urdu", "urdu", 8, 0, "structure_ready", listOf("SCIENCE_BIOLOGY", "SCIENCE_CS", "ARTS_HUMANITIES")),

        // Primary v1.0 Production Launch: Class 11 KPK HSSC-I across all study streams
        ApiSubjectDto(
            id = "sub_kpk11_phy",
            classLevel = 11,
            boardCode = "KPK",
            code = "PHY-11",
            name = "Physics (HSSC-I)",
            iconKey = "physics",
            totalChapters = 11,
            publishedChapters = 2,
            coverageStatus = "active_seed",
            studyGroups = listOf("PRE_MEDICAL", "PRE_ENGINEERING", "ICS"),
            description = "KPK Textbook Board Class 11 Physics covering Measurement, Vectors, Motion, Work & Rotational Dynamics.",
            textbookPdfId = "pdf_tb_phy11",
            pairingSchemeSummary = "BISE KPK HSSC-I: 18 MCQs (18m) + Sec-B Short 11/14 (44m) + Sec-C Long 3/4 (23m) = 85 Marks Theory",
            offlinePackSizeMb = 0.9
        ),
        ApiSubjectDto(
            id = "sub_kpk11_chem",
            classLevel = 11,
            boardCode = "KPK",
            code = "CHM-11",
            name = "Chemistry (HSSC-I)",
            iconKey = "chemistry",
            totalChapters = 12,
            publishedChapters = 2,
            coverageStatus = "active_seed",
            studyGroups = listOf("PRE_MEDICAL", "PRE_ENGINEERING"),
            description = "Stoichiometry, Atomic Structure, Chemical Bonding, Gases, Liquids & Chemical Equilibrium.",
            pairingSchemeSummary = "BISE KPK HSSC-I: 18 MCQs (18m) + Sec-B Short 11/14 (44m) + Sec-C Long 3/4 (23m) = 85 Marks Theory",
            offlinePackSizeMb = 0.8
        ),
        ApiSubjectDto(
            id = "sub_kpk11_bio",
            classLevel = 11,
            boardCode = "KPK",
            code = "BIO-11",
            name = "Biology (HSSC-I)",
            iconKey = "biology",
            totalChapters = 13,
            publishedChapters = 2,
            coverageStatus = "active_seed",
            studyGroups = listOf("PRE_MEDICAL"),
            description = "Cell Structure & Function, Biological Molecules, Enzymes, Bioenergetics & Kingdom Plantae/Animalia.",
            pairingSchemeSummary = "BISE KPK HSSC-I: 18 MCQs (18m) + Sec-B Short 11/14 (44m) + Sec-C Long 3/4 (23m) = 85 Marks Theory",
            offlinePackSizeMb = 0.8
        ),
        ApiSubjectDto(
            id = "sub_kpk11_math",
            classLevel = 11,
            boardCode = "KPK",
            code = "MTH-11",
            name = "Mathematics (HSSC-I)",
            iconKey = "math",
            totalChapters = 12,
            publishedChapters = 2,
            coverageStatus = "active_seed",
            studyGroups = listOf("PRE_ENGINEERING", "ICS"),
            description = "Complex Numbers, Matrices & Determinants, Sequences & Series, Permutations, Trigonometry & Vectors.",
            pairingSchemeSummary = "BISE KPK HSSC-I: 20 MCQs (20m) + Sec-B Short 10/14 (40m) + Sec-C Long 4/6 (40m) = 100 Marks",
            offlinePackSizeMb = 0.9
        ),
        ApiSubjectDto(
            id = "sub_kpk11_cs",
            classLevel = 11,
            boardCode = "KPK",
            code = "CS-11",
            name = "Computer Science (HSSC-I)",
            iconKey = "cs",
            totalChapters = 8,
            publishedChapters = 2,
            coverageStatus = "active_seed",
            studyGroups = listOf("ICS"),
            description = "Computer Systems, Memory Architecture, Operating Systems, Data Communication & Database Concepts.",
            pairingSchemeSummary = "BISE KPK HSSC-I: 15 MCQs (15m) + Sec-B Short 10/13 (40m) + Sec-C Long 2/3 (20m) = 75 Marks Theory",
            offlinePackSizeMb = 0.7
        ),
        ApiSubjectDto(
            id = "sub_kpk11_eng",
            classLevel = 11,
            boardCode = "KPK",
            code = "ENG-11",
            name = "English (HSSC-I)",
            iconKey = "english",
            totalChapters = 15,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("PRE_MEDICAL", "PRE_ENGINEERING", "ICS", "FA", "ICOM"),
            description = "KPK Class 11 English Prose, Poetry, Literary Analysis, Grammar, Paraphrasing & Essay Composition.",
            pairingSchemeSummary = "BISE KPK HSSC-I: 20 MCQs + Sec-B Comprehension & Poetry + Sec-C Letters & Essays = 100 Marks",
            offlinePackSizeMb = 0.6
        ),
        ApiSubjectDto(
            id = "sub_kpk11_urd",
            classLevel = 11,
            boardCode = "KPK",
            code = "URD-11",
            name = "Urdu (HSSC-I)",
            iconKey = "urdu",
            totalChapters = 16,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("PRE_MEDICAL", "PRE_ENGINEERING", "ICS", "FA", "ICOM"),
            description = "KPK HSSC-I Urdu Prose, Ghazals, Nazms, Tashreeh, Khulasa & Essay Writing in Nastaliq.",
            pairingSchemeSummary = "BISE KPK HSSC-I: 20 MCQs + Sec-B Ashar/Ibarat Tashreeh + Sec-C Khulasa & Mazmoon = 100 Marks",
            offlinePackSizeMb = 0.6
        ),
        ApiSubjectDto(
            id = "sub_kpk11_isl",
            classLevel = 11,
            boardCode = "KPK",
            code = "ISL-11",
            name = "Islamic Education (HSSC-I)",
            iconKey = "islamiat",
            totalChapters = 4,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("PRE_MEDICAL", "PRE_ENGINEERING", "ICS", "FA", "ICOM"),
            description = "Compulsory Islamic Studies covering Aqaid, Quranic Surahs, Selected Ahadith & Seerat-un-Nabi (PBUH).",
            pairingSchemeSummary = "BISE KPK HSSC-I: 10 MCQs + Sec-B Short 8/11 + Sec-C Long 2/3 = 50 Marks",
            offlinePackSizeMb = 0.4
        ),
        ApiSubjectDto(
            id = "sub_kpk11_eco",
            classLevel = 11,
            boardCode = "KPK",
            code = "ECO-11",
            name = "Economics (HSSC-I)",
            iconKey = "math",
            totalChapters = 10,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("FA", "ICOM"),
            description = "Microeconomics Foundations: Utility, Demand & Supply Elasticity, Production Cost & Market Structures.",
            pairingSchemeSummary = "BISE KPK HSSC-I (FA/I.Com): 20 MCQs + Sec-B Short 10/14 + Sec-C Long 3/5 = 100 Marks",
            offlinePackSizeMb = 0.6
        ),
        ApiSubjectDto(
            id = "sub_kpk11_acc",
            classLevel = 11,
            boardCode = "KPK",
            code = "ACC-11",
            name = "Principles of Accounting (HSSC-I)",
            iconKey = "cs",
            totalChapters = 10,
            publishedChapters = 1,
            coverageStatus = "active_seed",
            studyGroups = listOf("ICOM"),
            description = "Accounting Equation, Journal, Ledger, Trial Balance, Bank Reconciliation & Final Accounts.",
            pairingSchemeSummary = "BISE KPK HSSC-I (I.Com): 20 MCQs + Sec-B Numericals + Sec-C Final Accounts = 100 Marks",
            offlinePackSizeMb = 0.7
        ),

        // Class 12 structural entry (prepared in schema, not advertised in v1.0 launch UI)
        ApiSubjectDto("sub_kpk12_math", 12, "KPK", "MTH-12", "Mathematics (HSSC-II)", "math", 7, 0, "structure_ready", listOf("PRE_ENGINEERING", "ICS"))
    )

    val chapters: List<ApiChapterDto> = listOf(
        // Primary v1.0 Launch: Class 11 KPK HSSC-I Chapters
        ApiChapterDto(
            id = "ch_phy11_01",
            subjectId = "sub_kpk11_phy",
            chapterNumber = 1,
            title = "Measurements",
            subtitle = "SI units, supplementary units, uncertainty propagation & dimensional analysis",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 4,
            solvedQuestionsCount = 2,
            isLockedByDefault = false,
            estimatedSizeKb = 420,
            topics = listOf(
                ApiTopicDto("t11_p1_1", "ch_phy11_01", 1, "SI Base, Derived & Supplementary Units (Radian/Steradian)", 20),
                ApiTopicDto("t11_p1_2", "ch_phy11_01", 2, "Errors & Percentage Uncertainty Rules", 24),
                ApiTopicDto("t11_p1_3", "ch_phy11_01", 3, "Dimensional Analysis & Homogeneity", 22)
            ),
            difficultyTag = "medium",
            sourceReference = "KPK Textbook Board Physics XI Ch 1",
            titleUrdu = "پیمائش (Measurements)",
            subtitleUrdu = "بین الاقوامی اکائیاں، غیر یقینی صورتحال اور ابعادی تجزیہ",
            boardWeightageMarks = 10,
            pairingHint = "2 MCQs + 1 Short Q + Ch 2 Long Pair",
            nightBeforeSummary = "Radian (plane angle) & Steradian (solid angle); in multiplication/division % uncertainties add; in power rule multiply exponent by % uncertainty; [G] = [M^-1 L^3 T^-2]."
        ),
        ApiChapterDto(
            id = "ch_phy11_02",
            subjectId = "sub_kpk11_phy",
            chapterNumber = 2,
            title = "Vectors and Equilibrium",
            subtitle = "Rectangular components, scalar/vector products, torque & conditions of equilibrium",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 3,
            solvedQuestionsCount = 2,
            isLockedByDefault = false,
            estimatedSizeKb = 450,
            topics = listOf(
                ApiTopicDto("t11_p2_1", "ch_phy11_02", 1, "Vector Addition by Rectangular Components", 24),
                ApiTopicDto("t11_p2_2", "ch_phy11_02", 2, "Dot (Scalar) & Cross (Vector) Products", 26),
                ApiTopicDto("t11_p2_3", "ch_phy11_02", 3, "Torque & Two Conditions of Equilibrium", 22)
            ),
            difficultyTag = "medium",
            sourceReference = "KPK Textbook Board Physics XI Ch 2",
            titleUrdu = "ویکٹرز اور توازن",
            subtitleUrdu = "مستطیل اجزاء، ڈاٹ و کراس پروڈکٹ، ٹارک اور توازن کی شرائط",
            boardWeightageMarks = 12,
            pairingHint = "2 MCQs + 2 Short Qs + 1 Long Derivation",
            nightBeforeSummary = "A·B = AB cosθ (max at 0°); |A×B| = AB sinθ (max at 90°); Torque τ = r×F; Static equilibrium requires ΣF = 0 and Στ = 0."
        ),
        ApiChapterDto(
            id = "ch_chem11_01",
            subjectId = "sub_kpk11_chem",
            chapterNumber = 1,
            title = "Stoichiometry",
            subtitle = "Mole concept, Avogadro number, limiting reactant, yield & empirical formula",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 2,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 400,
            topics = listOf(
                ApiTopicDto("t11_c1_1", "ch_chem11_01", 1, "Mole, Molar Mass & Avogadro Number", 20),
                ApiTopicDto("t11_c1_2", "ch_chem11_01", 2, "Limiting Reactant & Percentage Yield", 24)
            ),
            difficultyTag = "medium",
            titleUrdu = "اسٹوئیشیومیٹری",
            subtitleUrdu = "مول کا تصور، ایووگاڈرو عدد، لمیٹنگ ری ایکٹنٹ اور فیصد پیداوار",
            boardWeightageMarks = 10,
            pairingHint = "2 MCQs + 1 Short Q + Numerical Long Pair",
            nightBeforeSummary = "1 mole = 6.022×10^23 particles = 22.414 dm^3 gas at STP; Limiting reactant yields smallest moles of product; % Yield = (Actual/Theoretical)×100."
        ),
        ApiChapterDto(
            id = "ch_chem11_02",
            subjectId = "sub_kpk11_chem",
            chapterNumber = 2,
            title = "Atomic Structure",
            subtitle = "Bohr atomic model, hydrogen spectrum, quantum numbers & electronic configuration",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 390,
            titleUrdu = "جوہری ساخت",
            subtitleUrdu = "بوہر ماڈل، ہائیڈروجن اسپیکٹرم اور کوانٹم نمبرز",
            boardWeightageMarks = 11,
            pairingHint = "2 MCQs + 2 Short Qs + Bohr Radius Long Q",
            nightBeforeSummary = "Lyman (UV, n1=1), Balmer (Visible, n1=2), Paschen (IR, n1=3); Azimuthal quantum number l ranges 0 to n-1."
        ),
        ApiChapterDto(
            id = "ch_bio11_01",
            subjectId = "sub_kpk11_bio",
            chapterNumber = 1,
            title = "Cell Structure and Function",
            subtitle = "Fluid mosaic plasma membrane, organelles, mitochondria, plastids & nucleus",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 410,
            titleUrdu = "خلیے کی ساخت اور افعال",
            subtitleUrdu = "پلازما ممبرین کا فلوئڈ موزیک ماڈل، مائٹوکونڈریا اور نیوکلیئس",
            boardWeightageMarks = 11,
            pairingHint = "2 MCQs + 2 Short Qs + Organelle Long Q",
            nightBeforeSummary = "Singer & Nicolson Fluid Mosaic Model (1972); Prokaryotes have 70S ribosomes vs 80S eukaryotic cytosol."
        ),
        ApiChapterDto(
            id = "ch_bio11_02",
            subjectId = "sub_kpk11_bio",
            chapterNumber = 2,
            title = "Biological Molecules",
            subtitle = "Properties of water, carbohydrates, amino acids, proteins & nucleic acids",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 380,
            titleUrdu = "حیاتیاتی سالمات",
            subtitleUrdu = "پانی کی خصوصیات، کاربوہائیڈریٹس، پروٹین اور نیوکلیک ایسڈ",
            boardWeightageMarks = 10,
            pairingHint = "2 MCQs + 2 Short Qs",
            nightBeforeSummary = "Glycosidic bond joins monosaccharides; Peptide bond joins amino acids; DNA uses Thymine, RNA uses Uracil."
        ),
        ApiChapterDto(
            id = "ch_math11_01",
            subjectId = "sub_kpk11_math",
            chapterNumber = 1,
            title = "Complex Numbers",
            subtitle = "Imaginary unit i, Argand plane, modulus, conjugate & De Moivre basics",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 390,
            titleUrdu = "مختلط اعداد",
            subtitleUrdu = "تخیلی اکائی، آرگنڈ پلین، ماڈیولس اور کونجوگیٹ",
            boardWeightageMarks = 10,
            pairingHint = "2 MCQs + 2 Short Qs + Ch 2 Long Pair",
            nightBeforeSummary = "i^2 = -1, i^4 = 1; For z = a + bi, |z| = sqrt(a^2 + b^2), z^-1 = z_bar / |z|^2."
        ),
        ApiChapterDto(
            id = "ch_math11_02",
            subjectId = "sub_kpk11_math",
            chapterNumber = 2,
            title = "Matrices and Determinants",
            subtitle = "Cofactors, adjoint, 3x3 determinants & Gaussian row reduction for linear systems",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 410,
            titleUrdu = "میٹرکس اور ڈیٹرمیننٹ",
            subtitleUrdu = "تین درجے کا ڈیٹرمیننٹ، ایچیلون فارم اور خطی مساوات",
            boardWeightageMarks = 12,
            pairingHint = "2 MCQs + 2 Short Qs + 1 Long Solution",
            nightBeforeSummary = "det(A^T) = det(A); If two rows are identical, det = 0; Non-trivial solution exists iff det(A) = 0."
        ),
        ApiChapterDto(
            id = "ch_cs11_01",
            subjectId = "sub_kpk11_cs",
            chapterNumber = 1,
            title = "Overview of Computer Systems",
            subtitle = "Hardware/software layers, input/output devices, CPU buses & instruction cycle",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 340,
            titleUrdu = "کمپیوٹر سسٹمز کا جائزہ",
            subtitleUrdu = "ہارڈ ویئر، سافٹ ویئر، بسز اور اسٹوریج کی درجہ بندی",
            boardWeightageMarks = 10,
            pairingHint = "2 MCQs + 2 Short Qs",
            nightBeforeSummary = "RAM is volatile primary memory; ROM stores BIOS/UEFI firmware permanently; Cache bridges CPU and DRAM."
        ),
        ApiChapterDto(
            id = "ch_cs11_02",
            subjectId = "sub_kpk11_cs",
            chapterNumber = 2,
            title = "Computer Memory & Logic Architecture",
            subtitle = "Registers, data/address/control buses, fetch-decode-execute cycle & ports",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 330,
            titleUrdu = "کمپیوٹر میموری اور سی پی یو آرکیٹیکچر",
            subtitleUrdu = "رجسٹرز، فیچ ڈیکوڈ ایگزیکیوٹ سائیکل اور سسٹم بسز",
            boardWeightageMarks = 11,
            pairingHint = "2 MCQs + 2 Short Qs + 1 Long Q",
            nightBeforeSummary = "PC holds address of next instruction; Address bus is unidirectional while Data bus is bidirectional."
        ),
        ApiChapterDto(
            id = "ch_eng11_01",
            subjectId = "sub_kpk11_eng",
            chapterNumber = 1,
            title = "Responsibilities of Youth",
            subtitle = "Quaid-e-Azam's Dhaka address analysis, comprehension, vocabulary & grammar",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 310,
            titleUrdu = "نوجوانوں کی ذمہ داریاں",
            subtitleUrdu = "قائداعظم کا خطاب، خلاصہ، اہم سوالات اور گرامر",
            boardWeightageMarks = 12,
            pairingHint = "2 MCQs + 2 Short Qs + Essay/Theme Link",
            nightBeforeSummary = "Delivered at Dhaka University Convocation on 24 March 1948; focused on education and disciplined nation-building."
        ),
        ApiChapterDto(
            id = "ch_urd11_01",
            subjectId = "sub_kpk11_urd",
            chapterNumber = 1,
            title = "اسوہ حسنہ ﷺ (Uswa-e-Hasana)",
            subtitle = "سیرت النبی ﷺ کے اخلاقی اصول، خلاصہ، تشریح اور کثیر الانتخابی سوالات",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 320,
            titleUrdu = "اسوہ حسنہ ﷺ",
            subtitleUrdu = "سید سلیمان ندوی کا مضمون: خلاصہ، تشریح اور اہم نکات",
            boardWeightageMarks = 12,
            pairingHint = "2 MCQs + خلاصہ / تشریح",
            nightBeforeSummary = "مصنف: سید سلیمان ندوی؛ حضور اکرم ﷺ کی حیاتِ طیبہ ہر طبقے کے لیے عملی نمونہ ہے۔"
        ),
        ApiChapterDto(
            id = "ch_isl11_01",
            subjectId = "sub_kpk11_isl",
            chapterNumber = 1,
            title = "عقیدہ توحید و رسالت (Tauheed & Risalat)",
            subtitle = "بنیادی عقائد اسلام، توحید کے انسانی زندگی پر اثرات اور ختم نبوت ﷺ",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 280,
            titleUrdu = "عقیدہ توحید و رسالت",
            subtitleUrdu = "توحید باری تعالیٰ، رسالت اور ختمِ نبوت ﷺ",
            boardWeightageMarks = 12,
            pairingHint = "2 MCQs + 2 Short Qs + 1 Long Q",
            nightBeforeSummary = "توحید کا بنیادی تقاضا اللہ کو یکتا ماننا ہے؛ ختم نبوت ﷺ اسلام کا بنیادی عقیدہ ہے۔"
        ),
        ApiChapterDto(
            id = "ch_eco11_01",
            subjectId = "sub_kpk11_eco",
            chapterNumber = 1,
            title = "Nature & Scope of Economics",
            subtitle = "Adam Smith, Alfred Marshall & Lionel Robbins definitions, scarcity & choice",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 330,
            titleUrdu = "معاشیات کی نوعیت اور وسعت",
            subtitleUrdu = "آدم اسمتھ، مارشل اور رابنز کی تعریفیں اور قلتِ وسائل",
            boardWeightageMarks = 12,
            pairingHint = "2 MCQs + 2 Short Qs + 1 Long Comparison",
            nightBeforeSummary = "Adam Smith (1776 Wealth); Alfred Marshall (1890 Welfare); Lionel Robbins (1932 Scarcity & Choice)."
        ),
        ApiChapterDto(
            id = "ch_acc11_01",
            subjectId = "sub_kpk11_acc",
            chapterNumber = 1,
            title = "Accounting Equation & Transactions",
            subtitle = "Assets = Liabilities + Owner Equity, double-entry rules & transaction effects",
            currentVersion = "v1.0.0",
            publishStatus = "published",
            mcqCount = 1,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 350,
            titleUrdu = "حسابداری کی مساوات اور اندراجات",
            subtitleUrdu = "اثاثے = واجبات + سرمایہ اور دوہرے اندراج کے اصول",
            boardWeightageMarks = 15,
            pairingHint = "2 MCQs + 2 Short Qs + 1 Accounting Equation Numerical",
            nightBeforeSummary = "Assets = Liabilities + Capital; Assets & Expenses carry normal Debit balances."
        ),

        // Class 9 KPK Physics
        ApiChapterDto(
            id = "ch_phy9_01",
            subjectId = "sub_kpk9_phy",
            chapterNumber = 1,
            title = "Physical Quantities & Measurement",
            subtitle = "SI Base & Derived Units, Prefixes, Scientific Notation, Vernier Callipers & Screw Gauge",
            currentVersion = "v1.2",
            publishStatus = "published",
            mcqCount = 5,
            solvedQuestionsCount = 3,
            isLockedByDefault = false,
            estimatedSizeKb = 410,
            topics = listOf(
                ApiTopicDto("top_phy9_01_1", "ch_phy9_01", 1, "Base & Derived Units", 15),
                ApiTopicDto("top_phy9_01_2", "ch_phy9_01", 2, "Prefixes & Scientific Notation", 15),
                ApiTopicDto("top_phy9_01_3", "ch_phy9_01", 3, "Measuring Instruments & Significant Figures", 20)
            ),
            difficultyTag = "medium",
            sourceReference = "KPK Textbook Board Peshawar — Physics 9 Ch 1",
            titleUrdu = "طبیعی مقداریں اور پیمائش",
            subtitleUrdu = "بنیادی و ماخوذ اکائیاں، سائنسی ترقیم، ورنیئر کیلیپرز اور سکرو گیج",
            boardWeightageMarks = 10,
            pairingHint = "2 MCQs + 1 Short Q + Numerical Ch-2 Pair",
            nightBeforeSummary = "7 SI Base Units (m, kg, s, A, K, mol, cd); Vernier LC = 0.01 cm; Screw Gauge LC = 0.01 mm; Leading zeros are never significant."
        ),
        ApiChapterDto(
            id = "ch_phy9_02",
            subjectId = "sub_kpk9_phy",
            chapterNumber = 2,
            title = "Kinematics",
            subtitle = "Scalars vs Vectors, Speed, Velocity, Acceleration & Equations of Motion",
            currentVersion = "v1.0",
            publishStatus = "published",
            mcqCount = 4,
            solvedQuestionsCount = 2,
            isLockedByDefault = false,
            estimatedSizeKb = 390,
            topics = listOf(
                ApiTopicDto("top_phy9_02_1", "ch_phy9_02", 1, "Scalars & Vectors", 15),
                ApiTopicDto("top_phy9_02_2", "ch_phy9_02", 2, "Equations of Motion", 25)
            ),
            difficultyTag = "medium",
            sourceReference = "KPK Textbook Board Peshawar — Physics 9 Ch 2",
            titleUrdu = "حرکیات (کائنی میٹکس)",
            subtitleUrdu = "اسکیلر اور ویکٹر مقداریں، رفتار، اسراع اور حرکت کی مساواتیں",
            boardWeightageMarks = 12,
            pairingHint = "2 MCQs + 2 Short Qs + 1 Long (Equations Derivation)",
            nightBeforeSummary = "3 Equations of Motion: vf=vi+at, S=vit+½at², 2aS=vf²-vi². Slope of d-t graph = speed; area under v-t graph = distance."
        ),
        ApiChapterDto(
            id = "ch_chem9_01",
            subjectId = "sub_kpk9_chem",
            chapterNumber = 1,
            title = "Fundamentals of Chemistry",
            subtitle = "Branches of Chemistry, Atomic Number, Mass Number, Mole & Avogadro's Number",
            currentVersion = "v1.1",
            publishStatus = "published",
            mcqCount = 3,
            solvedQuestionsCount = 2,
            isLockedByDefault = false,
            estimatedSizeKb = 360,
            topics = listOf(
                ApiTopicDto("top_chem9_01_1", "ch_chem9_01", 1, "Elements, Compounds & Mixtures", 15),
                ApiTopicDto("top_chem9_01_2", "ch_chem9_01", 2, "Mole & Avogadro Calculations", 20)
            ),
            difficultyTag = "medium",
            titleUrdu = "کیمیاء کے بنیادی اصول",
            subtitleUrdu = "کیمیاء کی شاخیں، ایٹمی نمبر، مول اور ایوگیڈرو نمبر",
            boardWeightageMarks = 9,
            pairingHint = "2 MCQs + 1 Short Q + Mole Numerical",
            nightBeforeSummary = "Mole = Mass / Molar Mass; NA = 6.02×10²³ particles/mol; Compound has fixed ratio by mass, mixture has variable ratio."
        ),
        ApiChapterDto(
            id = "ch_math9_01",
            subjectId = "sub_kpk9_math",
            chapterNumber = 1,
            title = "Matrices & Determinants",
            subtitle = "Order of a Matrix, Addition, Multiplication, Determinant & Cramer's Rule",
            currentVersion = "v1.0",
            publishStatus = "published",
            mcqCount = 3,
            solvedQuestionsCount = 2,
            isLockedByDefault = false,
            estimatedSizeKb = 330,
            topics = listOf(
                ApiTopicDto("top_math9_01_1", "ch_math9_01", 1, "Types & Operations on Matrices", 20),
                ApiTopicDto("top_math9_01_2", "ch_math9_01", 2, "Determinants & Inverse", 25)
            ),
            difficultyTag = "medium",
            titleUrdu = "قالب اور مقطعات",
            subtitleUrdu = "قالب کا مرتبہ، ضرب، مقطع اور کریمر کا اصول",
            boardWeightageMarks = 10,
            pairingHint = "2 MCQs + 1 Short Q + 1 Long (Cramer / Matrix Inversion)",
            nightBeforeSummary = "AB ≠ BA in general; Singular if |A|=0; Inverse A⁻¹ = Adj(A)/|A|; Cramer's Rule guaranteed in Section B/C."
        ),
        ApiChapterDto(
            id = "ch_bio9_01",
            subjectId = "sub_kpk9_bio",
            chapterNumber = 1,
            title = "Introduction to Biology",
            subtitle = "Divisions of Biology, Careers, Muslim Scientists & Cellular Organization",
            currentVersion = "v1.0",
            publishStatus = "published",
            mcqCount = 2,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 300,
            difficultyTag = "easy",
            titleUrdu = "حیاتیات کا تعارف",
            subtitleUrdu = "حیاتیات کے شعبے، مسلم سائنسدان اور خلوی تنظیم",
            boardWeightageMarks = 8,
            pairingHint = "1 MCQ + 2 Short Qs + Organ Levels",
            nightBeforeSummary = "Divisions: Botany, Zoology, Microbiology; Jabir Bin Hayan (Al-Nabatat), Bu Ali Sina (Al-Qanun fi al-Tib)."
        ),
        ApiChapterDto(
            id = "ch_cs9_01",
            subjectId = "sub_kpk9_cs",
            chapterNumber = 1,
            title = "Fundamentals of Computer",
            subtitle = "Evolution of Computers, Generations, Hardware Components & Software",
            currentVersion = "v1.0",
            publishStatus = "published",
            mcqCount = 2,
            solvedQuestionsCount = 1,
            isLockedByDefault = false,
            estimatedSizeKb = 290,
            difficultyTag = "easy",
            titleUrdu = "کمپیوٹر کے بنیادی اصول",
            subtitleUrdu = "کمپیوٹر کی نسلیں، ہارڈویئر اور سافٹ ویئر",
            boardWeightageMarks = 9,
            pairingHint = "2 MCQs + 1 Short Q + CPU Architecture",
            nightBeforeSummary = "5 Generations: Vacuum Tubes → Transistors → ICs → Microprocessors → AI. CPU = ALU + CU."
        ),

        // Class 10 KPK preserved chapters
        ApiChapterDto(
            id = "ch_phy10_01",
            subjectId = "sub_kpk10_phy",
            chapterNumber = 1,
            title = "Physical Quantities & SHM",
            subtitle = "Oscillatory motion, pendulum period, and restoring force",
            currentVersion = "v1.4",
            publishStatus = "published",
            mcqCount = 5,
            solvedQuestionsCount = 3,
            isLockedByDefault = false,
            estimatedSizeKb = 420,
            titleUrdu = "سادہ ہارمونک حرکت اور امواج",
            subtitleUrdu = "ارتعاشی حرکت، سادہ پینڈولم کا دورانیہ اور بحالی قوت",
            boardWeightageMarks = 11,
            pairingHint = "2 MCQs + 1 Short Q + Pendulum Numerical",
            nightBeforeSummary = "F = -kx; T = 2π√(l/g) independent of bob mass; Velocity max at mean position, PE max at extreme."
        ),
        ApiChapterDto(
            id = "ch_phy10_02",
            subjectId = "sub_kpk10_phy",
            chapterNumber = 2,
            title = "Motion & Waves",
            subtitle = "Wave equation, frequency, wavelength & ripple tank",
            currentVersion = "v1.2",
            publishStatus = "published",
            mcqCount = 5,
            solvedQuestionsCount = 2,
            isLockedByDefault = false,
            estimatedSizeKb = 380,
            titleUrdu = "حرکت اور امواج",
            subtitleUrdu = "موج کی مساوات، تعدد، طولِ موج اور رپل ٹینک",
            boardWeightageMarks = 10,
            pairingHint = "1 MCQ + 2 Short Qs + Wave Numerical",
            nightBeforeSummary = "v = fλ; f = 1/T; Diffraction bends waves around obstacles matching wavelength size."
        ),
        ApiChapterDto(
            id = "ch_phy10_03",
            subjectId = "sub_kpk10_phy",
            chapterNumber = 3,
            title = "Forces and Laws",
            subtitle = "Geometrical optics, reflection and spherical mirrors",
            currentVersion = "v0.9",
            publishStatus = "in_review",
            mcqCount = 4,
            solvedQuestionsCount = 2,
            isLockedByDefault = true,
            estimatedSizeKb = 460
        ),
        ApiChapterDto(
            id = "ch_phy10_04",
            subjectId = "sub_kpk10_phy",
            chapterNumber = 4,
            title = "Work and Energy",
            subtitle = "Electrostatics & Coulomb force basics",
            currentVersion = "v0.5",
            publishStatus = "draft",
            mcqCount = 0,
            solvedQuestionsCount = 0,
            isLockedByDefault = true,
            estimatedSizeKb = 250
        ),
        ApiChapterDto(
            id = "ch_math10_01",
            subjectId = "sub_kpk10_math",
            chapterNumber = 1,
            title = "Quadratic Equations",
            subtitle = "Standard form, factorization & quadratic formula",
            currentVersion = "v1.3",
            publishStatus = "published",
            mcqCount = 4,
            solvedQuestionsCount = 3,
            isLockedByDefault = false,
            estimatedSizeKb = 340
        ),
        ApiChapterDto(
            id = "ch_chem10_01",
            subjectId = "sub_kpk10_chem",
            chapterNumber = 1,
            title = "Chemical Equilibrium",
            subtitle = "Law of mass action and equilibrium constant Kc",
            currentVersion = "v1.0",
            publishStatus = "published",
            mcqCount = 4,
            solvedQuestionsCount = 2,
            isLockedByDefault = false,
            estimatedSizeKb = 310
        )
    )

    val detailedNotesBlocks: Map<String, List<ApiContentBlockDto>> = mapOf(
        "ch_phy11_01" to listOf(
            ApiContentBlockDto.HeadingDto("p11_1_h1", 1, "1.1", "International System of Units & Supplementary Units"),
            ApiContentBlockDto.ParagraphDto(
                "p11_1_p1",
                2,
                "In KPK HSSC-I Physics, physical quantities are categorized into 7 SI Base quantities, Derived quantities, and 2 Supplementary geometrical units: the Radian (plane angle) and Steradian (solid angle)."
            ),
            ApiContentBlockDto.DefinitionDto(
                "p11_1_def1",
                3,
                "Radian (rad)",
                "The plane angle subtended at the centre of a circle by an arc whose length equals the radius of that circle (1 full circle = 2π rad).",
                "θ = s / r"
            ),
            ApiContentBlockDto.MathExpressionDto(
                "p11_1_math1",
                4,
                "\\frac{\\Delta Z}{Z} = n\\frac{\\Delta A}{A} + m\\frac{\\Delta B}{B}",
                "% Uncertainty(Z) = n(% Uncertainty A) + m(% Uncertainty B) for Z = Aⁿ · Bᵐ",
                "Percentage Uncertainty Power & Product Rule"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "p11_1_ex1",
                5,
                "numerical",
                4,
                "The radius of a sphere is measured as r = 2.25 ± 0.01 cm. Calculate the percentage uncertainty in its volume V = (4/3)πr³.",
                listOf(
                    "Percentage uncertainty in r = (0.01 / 2.25) × 100 = 0.44%.",
                    "Volume depends on r³, so multiply % uncertainty by power 3.",
                    "Total percentage uncertainty in Volume = 3 × 0.44% ≈ 1.3%."
                ),
                "BISE Peshawar HSSC-I"
            ),
            ApiContentBlockDto.ExamPointDto(
                "p11_1_ep1",
                6,
                "High-Yield Dimensional Analysis Checks",
                listOf(
                    "Dimensional formula of Gravitational Constant G is [M⁻¹ L³ T⁻²].",
                    "Dimensionless quantities (plane angle, refractive index, coefficient of friction) carry no dimensions even when they have units like radian."
                ),
                "High"
            )
        ),
        "ch_phy11_02" to listOf(
            ApiContentBlockDto.HeadingDto("p11_2_h1", 1, "2.1", "Scalar & Vector Products & Equilibrium"),
            ApiContentBlockDto.DefinitionDto(
                "p11_2_def1",
                2,
                "Torque (Moment of Force)",
                "The turning effect of a force about an axis of rotation, equal to the cross product of position vector r and force F.",
                "τ = r × F = rF sinθ"
            ),
            ApiContentBlockDto.MathExpressionDto(
                "p11_2_math1",
                3,
                "\\vec{A}\\cdot\\vec{B} = A_x B_x + A_y B_y + A_z B_z = AB\\cos\\theta",
                "A · B = AxBx + AyBy + AzBz = AB cosθ",
                "Dot Product & Angle Formula"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "p11_2_ex1",
                4,
                "short",
                4,
                "State the two necessary conditions for complete static equilibrium of a rigid body.",
                listOf(
                    "First Condition (Translational Equilibrium): Vector sum of all external forces acting on the body is zero (ΣF = 0).",
                    "Second Condition (Rotational Equilibrium): Vector sum of all torques about any chosen axis is zero (Στ = 0)."
                ),
                "KPK Board Pattern"
            )
        ),
        "ch_chem11_01" to listOf(
            ApiContentBlockDto.HeadingDto("c11_1_h1", 1, "1.1", "Stoichiometry, Mole & Limiting Reactant"),
            ApiContentBlockDto.DefinitionDto(
                "c11_1_def1",
                2,
                "Limiting Reactant",
                "The reactant that is completely consumed first in a chemical reaction and therefore determines the maximum amount of product formed.",
                "n = Mass / Molar Mass"
            ),
            ApiContentBlockDto.MathExpressionDto(
                "c11_1_math1",
                3,
                "\\%\\text{ Yield} = \\frac{\\text{Actual Yield}}{\\text{Theoretical Yield}}\\times 100",
                "% Yield = (Actual Yield / Theoretical Yield) × 100",
                "Reaction Efficiency"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "c11_1_ex1",
                4,
                "numerical",
                4,
                "Calculate the number of moles in 44 g of CO₂ (Molar mass = 44 g/mol) and its volume at STP.",
                listOf(
                    "Moles of CO₂ = 44 g / 44 g/mol = 1.0 mol.",
                    "At STP, 1 mole of any ideal gas occupies 22.414 dm³."
                ),
                "KPK HSSC-I"
            )
        ),
        "ch_chem11_02" to listOf(
            ApiContentBlockDto.HeadingDto("c11_2_h1", 1, "2.1", "Bohr Model & Quantum Numbers"),
            ApiContentBlockDto.DefinitionDto(
                "c11_2_def1",
                2,
                "Pauli Exclusion Principle",
                "No two electrons in the same atom can have identical values for all four quantum numbers (n, l, m, s).",
                "Max electrons in shell = 2n²"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "c11_2_ex1",
                3,
                "short",
                4,
                "Name the spectral series of hydrogen falling in UV and Visible regions.",
                listOf(
                    "Lyman series lies in the Ultraviolet (UV) region when electrons transition down to n₁ = 1.",
                    "Balmer series lies in the Visible region when electrons transition down to n₁ = 2."
                ),
                "KPK HSSC-I"
            )
        ),
        "ch_bio11_01" to listOf(
            ApiContentBlockDto.HeadingDto("b11_1_h1", 1, "1.1", "Plasma Membrane & Cell Organelles"),
            ApiContentBlockDto.DefinitionDto(
                "b11_1_def1",
                2,
                "Fluid Mosaic Model",
                "Proposed by Singer and Nicolson (1972): the plasma membrane is a fluid phospholipid bilayer with protein molecules embedded like a mosaic.",
                "Hydrophilic heads face out; hydrophobic tails face in"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "b11_1_ex1",
                3,
                "short",
                4,
                "Why are mitochondria called semi-autonomous organelles?",
                listOf(
                    "Mitochondria possess their own circular DNA and 70S ribosomes.",
                    "They replicate independently by binary fission and synthesize some of their own proteins."
                ),
                "KPK Biology XI"
            )
        ),
        "ch_bio11_02" to listOf(
            ApiContentBlockDto.HeadingDto("b11_2_h1", 1, "2.1", "Carbohydrates, Proteins & Nucleic Acids"),
            ApiContentBlockDto.DefinitionDto(
                "b11_2_def1",
                2,
                "Peptide Bond",
                "A covalent C-N linkage formed between the carboxyl group of one amino acid and the amino group of another by condensation.",
                "-CO-NH- + H₂O"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "b11_2_ex1",
                3,
                "short",
                4,
                "Differentiate between DNA and RNA sugar and nitrogenous bases.",
                listOf(
                    "DNA contains deoxyribose sugar and Thymine (A, T, G, C).",
                    "RNA contains ribose sugar and Uracil in place of Thymine (A, U, G, C)."
                ),
                "KPK Biology XI"
            )
        ),
        "ch_math11_01" to listOf(
            ApiContentBlockDto.HeadingDto("m11_1_h1", 1, "1.1", "Complex Numbers, Conjugate & Modulus"),
            ApiContentBlockDto.MathExpressionDto(
                "m11_1_math1",
                2,
                "z = a + bi,\\quad |z| = \\sqrt{a^2 + b^2},\\quad z\\bar{z} = a^2 + b^2",
                "|a + bi| = √(a² + b²) and z·z̄ = a² + b²",
                "Modulus & Conjugate Identity"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "m11_1_ex1",
                3,
                "short",
                4,
                "Find the multiplicative inverse of z = 3 - 4i.",
                listOf(
                    "Modulus squared |z|² = 3² + (-4)² = 9 + 16 = 25.",
                    "Conjugate z̄ = 3 + 4i.",
                    "z⁻¹ = z̄ / |z|² = 3/25 + (4/25)i."
                ),
                "KPK Math XI"
            )
        ),
        "ch_math11_02" to listOf(
            ApiContentBlockDto.HeadingDto("m11_2_h1", 1, "2.1", "3×3 Determinants & Linear Systems"),
            ApiContentBlockDto.DefinitionDto(
                "m11_2_def1",
                2,
                "Echelon Form of a Matrix",
                "A row-reduced form where leading non-zero entries shift strictly rightward in each lower row and zero rows sit at the bottom.",
                "rank(A) = non-zero rows"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "m11_2_ex1",
                3,
                "short",
                4,
                "When does a homogeneous system AX = 0 have a non-trivial solution?",
                listOf(
                    "A homogeneous system AX = 0 always admits the trivial solution X = 0.",
                    "It has infinitely many non-trivial solutions if and only if det(A) = 0."
                ),
                "KPK Math XI"
            )
        ),
        "ch_cs11_01" to listOf(
            ApiContentBlockDto.HeadingDto("cs11_1_h1", 1, "1.1", "Computer Architecture & Storage Hierarchy"),
            ApiContentBlockDto.DefinitionDto(
                "cs11_1_def1",
                2,
                "System Software",
                "Programs such as Operating Systems, Device Drivers, and Utilities that manage hardware resources and host application software.",
                "OS bridges hardware and applications"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "cs11_1_ex1",
                3,
                "short",
                4,
                "Differentiate between RAM and ROM in modern computer systems.",
                listOf(
                    "RAM is volatile read/write primary memory storing active OS and app instructions.",
                    "ROM is non-volatile memory storing permanent boot firmware (BIOS/UEFI)."
                ),
                "KPK CS XI"
            )
        ),
        "ch_cs11_02" to listOf(
            ApiContentBlockDto.HeadingDto("cs11_2_h1", 1, "2.1", "CPU Registers & System Buses"),
            ApiContentBlockDto.DefinitionDto(
                "cs11_2_def1",
                2,
                "Program Counter (PC)",
                "A dedicated CPU register that holds the memory address of the next instruction to be fetched and executed.",
                "Increments after each fetch"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "cs11_2_ex1",
                3,
                "short",
                4,
                "Explain the roles of Control Bus, Address Bus, and Data Bus.",
                listOf(
                    "Address Bus carries memory addresses unidirectionally from CPU to RAM.",
                    "Data Bus transfers instructions and data bidirectionally.",
                    "Control Bus carries timing and read/write signals."
                ),
                "KPK CS XI"
            )
        ),
        "ch_eng11_01" to listOf(
            ApiContentBlockDto.HeadingDto("e11_1_h1", 1, "1.1", "Lesson 1: Responsibilities of Youth"),
            ApiContentBlockDto.ParagraphDto(
                "e11_1_p1",
                2,
                "In his historic address at the Dhaka University Convocation on March 24, 1948, Quaid-e-Azam Muhammad Ali Jinnah advised students to devote their primary energy to education, discipline, and technical skill-building."
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "e11_1_ex1",
                3,
                "short",
                4,
                "Why did Quaid-e-Azam warn students against falling prey to political opportunists?",
                listOf(
                    "He noted that self-serving factions exploit students' youthful idealism to disrupt classrooms.",
                    "He emphasized that nation-building demands completed degrees, competence, and integrity."
                ),
                "KPK English XI"
            )
        ),
        "ch_urd11_01" to listOf(
            ApiContentBlockDto.HeadingDto("u11_1_h1", 1, "1.1", "سبق 1: اسوہ حسنہ ﷺ (سید سلیمان ندوی)"),
            ApiContentBlockDto.ParagraphDto(
                "u11_1_p1",
                2,
                "سید سلیمان ندوی اس مضمون میں واضح کرتے ہیں کہ دنیا کی ہر قوم، پیشے اور عمر کے انسان کے لیے کامل ترین عملی نمونہ حضرت محمد مصطفیٰ ﷺ کی حیاتِ طیبہ میں موجود ہے۔"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "u11_1_ex1",
                3,
                "short",
                4,
                "مصنف کے نزدیک کامل نمونہ کی چار بنیادی شرائط کیا ہیں؟",
                listOf("تاریخی توثیق", "کاملیت", "جامعیت", "عملیت"),
                "KPK Urdu XI"
            )
        ),
        "ch_isl11_01" to listOf(
            ApiContentBlockDto.HeadingDto("i11_1_h1", 1, "1.1", "باب 1: عقیدہ توحید و رسالت"),
            ApiContentBlockDto.ParagraphDto(
                "i11_1_p1",
                2,
                "توحید کے معنی اللہ تعالیٰ کو اس کی ذات، صفات اور اختیارات میں یکتا و بے مثال ماننا ہے۔ عقیدہ توحید انسان میں عزتِ نفس اور احساسِ ذمہ داری پیدا کرتا ہے۔"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "i11_1_ex1",
                3,
                "short",
                4,
                "عقیدہ توحید کے انسانی زندگی پر دو اہم اثرات بیان کریں۔",
                listOf("انسان غیر اللہ کے خوف سے آزاد ہو جاتا ہے۔", "معاشرتی مساوات اور تقویٰ کی بنیاد مضبوط ہوتی ہے۔"),
                "KPK Islamiat XI"
            )
        ),
        "ch_eco11_01" to listOf(
            ApiContentBlockDto.HeadingDto("ec11_1_h1", 1, "1.1", "Evolution of Economic Thought"),
            ApiContentBlockDto.DefinitionDto(
                "ec11_1_def1",
                2,
                "Robbins Scarcity Definition",
                "Economics is the science which studies human behaviour as a relationship between unlimited ends and scarce means having alternative uses.",
                "Unlimited Wants + Scarce Means → Choice"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "ec11_1_ex1",
                3,
                "short",
                4,
                "Distinguish between Microeconomics and Macroeconomics with examples.",
                listOf(
                    "Microeconomics studies individual units (consumer demand, firm pricing).",
                    "Macroeconomics studies economy-wide aggregates (GDP, inflation, unemployment)."
                ),
                "KPK Economics XI"
            )
        ),
        "ch_acc11_01" to listOf(
            ApiContentBlockDto.HeadingDto("ac11_1_h1", 1, "1.1", "The Accounting Equation & Double Entry"),
            ApiContentBlockDto.MathExpressionDto(
                "ac11_1_math1",
                2,
                "\\text{Assets} = \\text{Liabilities} + \\text{Owner's Equity}",
                "Assets = Liabilities + Owner Equity",
                "Fundamental Accounting Identity"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "ac11_1_ex1",
                3,
                "numerical",
                4,
                "If business Assets total PKR 450,000 and Owner Equity is PKR 310,000, calculate total Liabilities.",
                listOf(
                    "Assets = Liabilities + Owner Equity",
                    "450,000 = Liabilities + 310,000",
                    "Liabilities = 450,000 - 310,000 = PKR 140,000"
                ),
                "KPK I.Com Part I"
            )
        ),
        "ch_phy9_01" to listOf(
            ApiContentBlockDto.HeadingDto("p9_b1", 1, "1.1", "Physical Quantities & International System of Units"),
            ApiContentBlockDto.ParagraphDto(
                "p9_b2",
                2,
                "All measurable quantities are called physical quantities. Every physical quantity has two essential characteristics: its numerical magnitude and a unit in which it is measured."
            ),
            ApiContentBlockDto.DefinitionDto(
                "p9_b3",
                3,
                "Base Quantities",
                "Seven fundamental physical quantities selected by SI upon which other quantities can be expressed: Length, Mass, Time, Electric Current, Temperature, Amount of Substance, and Luminous Intensity.",
                "7 SI Base Units: m, kg, s, A, K, mol, cd"
            ),
            ApiContentBlockDto.TableDto(
                "p9_b4",
                4,
                "SI Base Units & Standard Symbols (KPK Class 9)",
                listOf("Quantity", "Symbol", "SI Unit", "Unit Symbol"),
                listOf(
                    listOf("Length", "l", "metre", "m"),
                    listOf("Mass", "m", "kilogram", "kg"),
                    listOf("Time", "t", "second", "s"),
                    listOf("Electric Current", "I", "ampere", "A"),
                    listOf("Temperature", "T", "kelvin", "K")
                )
            ),
            ApiContentBlockDto.HeadingDto("p9_b5", 5, "1.2", "Prefixes & Scientific Notation"),
            ApiContentBlockDto.MathExpressionDto(
                "p9_b6",
                6,
                "N = M \\times 10^n \\quad (1 \\le M < 10)",
                "N = M × 10ⁿ   (where 1 ≤ M < 10)",
                "Standard scientific notation rule tested in BISE Peshawar numericals"
            ),
            ApiContentBlockDto.HeadingDto("p9_b7", 7, "1.3", "Measuring Instruments & Least Count"),
            ApiContentBlockDto.ExamPointDto(
                "p9_b8",
                8,
                "High-Yield KPK Board Exam Points",
                listOf(
                    "Least count of Metre Rule = 1 mm (0.1 cm).",
                    "Least count of Vernier Callipers = 0.1 mm (0.01 cm).",
                    "Least count of Screw Gauge (Micrometre) = 0.01 mm (0.001 cm).",
                    "Zero error is subtracted algebraically from observed reading."
                ),
                "High"
            ),
            ApiContentBlockDto.ExampleDto(
                "p9_b9",
                9,
                "Worked Numerical 1.2 (Screw Gauge Reading)",
                "The main scale reading on a screw gauge is 2 mm and the 35th circular scale division coincides with the datum line. Find the diameter if LC = 0.01 mm and zero error = 0.",
                listOf(
                    "Main Scale Reading (MSR) = 2 mm",
                    "Circular Scale Reading (CSR) = 35 × 0.01 mm = 0.35 mm",
                    "Total Reading = MSR + CSR = 2 + 0.35 = 2.35 mm"
                ),
                "2.35 mm"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "p9_b10",
                10,
                "short",
                4,
                "Differentiate between base quantities and derived quantities with two examples each.",
                listOf(
                    "Base quantities are independent fundamental quantities (e.g., mass, length, time).",
                    "Derived quantities are obtained by multiplying or dividing base quantities (e.g., speed = m/s, force = kg·m/s²).",
                    "SI defines exactly 7 base units and unlimited derived units."
                ),
                "BISE Peshawar 2023"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "p9_b11",
                11,
                "numerical",
                4,
                "Express the speed of light (300,000,000 m/s) in scientific notation and with the prefix Giga (G).",
                listOf(
                    "Scientific notation: 300,000,000 m/s = 3.0 × 10⁸ m/s",
                    "Using 1 G = 10⁹: 3.0 × 10⁸ = 0.3 × 10⁹ m/s = 0.3 Gm/s"
                ),
                "KPK Model Paper"
            )
        ),
        "ch_phy10_01" to listOf(
            ApiContentBlockDto.HeadingDto("b1", 1, "3.1", "Introduction to Motion & SHM"),
            ApiContentBlockDto.ParagraphDto(
                "b2",
                2,
                "Motion is the change in position of an object with respect to time. Simple Harmonic Motion (SHM) occurs when the restoring force is directly proportional to displacement from the mean position."
            ),
            ApiContentBlockDto.ExamPointDto(
                "b3",
                3,
                "Key Points for KPK Board",
                listOf(
                    "Motion is relative and depends on the chosen reference frame.",
                    "At mean position, kinetic energy and velocity are maximum.",
                    "Time period of a simple pendulum depends on length l and gravity g, not mass."
                ),
                "High"
            ),
            ApiContentBlockDto.DefinitionDto(
                "b4",
                4,
                "Restoring Force",
                "A force that always pushes or pulls an oscillating body back toward its equilibrium position.",
                "F = -kx"
            ),
            ApiContentBlockDto.MathExpressionDto(
                "b5",
                5,
                "T = 2\\pi \\sqrt{\\frac{l}{g}}",
                "T = 2π √(l / g)",
                "Time period formula for a simple pendulum"
            ),
            ApiContentBlockDto.TableDto(
                "b6",
                6,
                "Transverse vs Longitudinal Waves",
                listOf("Feature", "Transverse", "Longitudinal"),
                listOf(
                    listOf("Vibration", "Perpendicular", "Parallel"),
                    listOf("Regions", "Crests & Troughs", "Compressions & Rarefactions")
                )
            ),
            ApiContentBlockDto.ListDto(
                "b7",
                7,
                ordered = false,
                title = "Conditions for SHM",
                items = listOf(
                    "System must possess inertia.",
                    "Restoring force must act toward mean position.",
                    "Damping friction is ideally negligible."
                )
            ),
            ApiContentBlockDto.ExampleDto(
                "b8",
                8,
                "Worked Numerical 3.1",
                "Calculate the time period of a simple pendulum of length 1.0 m where g = 9.8 m/s².",
                listOf(
                    "Given: l = 1.0 m, g = 9.8 m/s²",
                    "Use T = 2π √(l / g)",
                    "T = 2 × 3.1416 × √(1.0 / 9.8) = 2.01 s"
                ),
                "T ≈ 2.01 s"
            ),
            ApiContentBlockDto.DiagramDto(
                "b9",
                9,
                "Types of Motion & Pendulum Forces",
                "Illustration of linear displacement and restoring force vectors.",
                "diagram_motion_pendulum",
                "Diagram showing force vectors acting on a pendulum bob"
            ),
            ApiContentBlockDto.McqDto(
                "b10",
                10,
                "What is the value of acceleration due to gravity on Earth?",
                listOf("A" to "9.8 m/s²", "B" to "9.8 km/s²", "C" to "10 m/s", "D" to "9.6 m/s²"),
                "A",
                "Standard Earth surface gravitational acceleration is g = 9.8 m/s².",
                "easy"
            ),
            ApiContentBlockDto.SolvedQuestionDto(
                "b11",
                11,
                "short",
                4,
                "Why is the time period of a simple pendulum independent of bob mass?",
                listOf(
                    "Restoring force F = -mg sinθ increases with mass m.",
                    "Acceleration a = F/m = -g sinθ cancels mass m out.",
                    "Therefore T = 2π √(l/g) depends only on l and g."
                ),
                "KPK Board 2022"
            )
        )
    )

    val mcqs: List<ApiMcqQuestionDto> = listOf(
        // Primary v1.0 Launch: Class 11 KPK HSSC-I MCQs
        ApiMcqQuestionDto(
            id = "mcq_phy11_01_1",
            chapterId = "ch_phy11_01",
            subjectId = "sub_kpk11_phy",
            question = "Which of the following is an SI supplementary unit?",
            options = listOf("A" to "Kelvin", "B" to "Steradian", "C" to "Newton", "D" to "Candela"),
            correctOptionId = "B",
            explanation = "Radian and Steradian are the two SI supplementary geometrical units.",
            difficulty = "easy",
            topicTitle = "SI Base, Derived & Supplementary Units (Radian/Steradian)",
            pastPaperYear = 2024,
            questionUrdu = "ان میں سے کون سی ایس آئی کی ضمنی اکائی (Supplementary Unit) ہے؟",
            optionsUrdu = listOf("کیلون", "اسٹیریڈین", "نیوٹن", "کینڈیلا"),
            explanationUrdu = "ریڈین اور اسٹیریڈین ایس آئی سسٹم کی دو ضمنی اکائیاں ہیں۔"
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy11_01_2",
            chapterId = "ch_phy11_01",
            subjectId = "sub_kpk11_phy",
            question = "The dimensional formula of Universal Gravitational Constant G is:",
            options = listOf("A" to "[M L² T⁻²]", "B" to "[M⁻¹ L³ T⁻²]", "C" to "[M L⁻¹ T⁻²]", "D" to "[M⁻¹ L² T⁻¹]"),
            correctOptionId = "B",
            explanation = "From F = G m₁m₂ / r², [G] = [Fr² / m²] = [M⁻¹ L³ T⁻²].",
            difficulty = "medium",
            topicTitle = "Dimensional Analysis & Homogeneity",
            pastPaperYear = 2023,
            questionUrdu = "عالمی ثقلی مستقل G کا ابعادی فارمولا کیا ہے؟",
            optionsUrdu = listOf("[M L² T⁻²]", "[M⁻¹ L³ T⁻²]", "[M L⁻¹ T⁻²]", "[M⁻¹ L² T⁻¹]"),
            explanationUrdu = "G = Fr²/m₁m₂ کے مطابق اس کا ابعادی فارمولا [M⁻¹ L³ T⁻²] ہے۔"
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy11_01_3",
            chapterId = "ch_phy11_01",
            subjectId = "sub_kpk11_phy",
            question = "If radius measurement has 2% uncertainty, the percentage uncertainty in sphere volume is:",
            options = listOf("A" to "2%", "B" to "4%", "C" to "6%", "D" to "8%"),
            correctOptionId = "C",
            explanation = "Volume varies as r³, so % uncertainty in V = 3 × 2% = 6%.",
            difficulty = "medium",
            topicTitle = "Errors & Percentage Uncertainty Rules",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy11_01_4",
            chapterId = "ch_phy11_01",
            subjectId = "sub_kpk11_phy",
            question = "Light year is the unit of:",
            options = listOf("A" to "Time", "B" to "Speed", "C" to "Distance", "D" to "Intensity"),
            correctOptionId = "C",
            explanation = "A light year is the distance light travels in vacuum in one year (~9.46×10¹⁵ m).",
            difficulty = "easy",
            topicTitle = "SI Base, Derived & Supplementary Units (Radian/Steradian)",
            pastPaperYear = 2022
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy11_02_1",
            chapterId = "ch_phy11_02",
            subjectId = "sub_kpk11_phy",
            question = "If A·B = 0 and A, B are non-zero vectors, the angle between them is:",
            options = listOf("A" to "0°", "B" to "45°", "C" to "90°", "D" to "180°"),
            correctOptionId = "C",
            explanation = "Dot product AB cosθ = 0 implies cosθ = 0, so θ = 90°.",
            difficulty = "easy",
            topicTitle = "Dot (Scalar) & Cross (Vector) Products",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy11_02_2",
            chapterId = "ch_phy11_02",
            subjectId = "sub_kpk11_phy",
            question = "The cross product i × j equals:",
            options = listOf("A" to "0", "B" to "k", "C" to "-k", "D" to "1"),
            correctOptionId = "B",
            explanation = "Following right-hand cyclic order (i → j → k), i × j = +k.",
            difficulty = "easy",
            topicTitle = "Dot (Scalar) & Cross (Vector) Products",
            pastPaperYear = 2023
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy11_02_3",
            chapterId = "ch_phy11_02",
            subjectId = "sub_kpk11_phy",
            question = "For complete static equilibrium of a body:",
            options = listOf("A" to "ΣF = 0 only", "B" to "Στ = 0 only", "C" to "ΣF = 0 and Στ = 0", "D" to "Velocity must be non-zero"),
            correctOptionId = "C",
            explanation = "Both net force and net torque must vanish for translational & rotational equilibrium.",
            difficulty = "easy",
            topicTitle = "Torque & Two Conditions of Equilibrium",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_chem11_01_1",
            chapterId = "ch_chem11_01",
            subjectId = "sub_kpk11_chem",
            question = "One mole of any ideal gas at STP occupies a volume of:",
            options = listOf("A" to "11.2 dm³", "B" to "22.414 dm³", "C" to "24.0 dm³", "D" to "1.0 dm³"),
            correctOptionId = "B",
            explanation = "Molar volume of an ideal gas at 273.15 K and 1 atm is 22.414 dm³.",
            difficulty = "easy",
            topicTitle = "Mole, Molar Mass & Avogadro Number",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_chem11_01_2",
            chapterId = "ch_chem11_01",
            subjectId = "sub_kpk11_chem",
            question = "Which reactant controls the maximum amount of product formed?",
            options = listOf("A" to "Excess reactant", "B" to "Catalyst", "C" to "Limiting reactant", "D" to "Solvent"),
            correctOptionId = "C",
            explanation = "The limiting reactant is consumed first, halting further product formation.",
            difficulty = "easy",
            topicTitle = "Limiting Reactant & Percentage Yield",
            pastPaperYear = 2023
        ),
        ApiMcqQuestionDto(
            id = "mcq_chem11_02_1",
            chapterId = "ch_chem11_02",
            subjectId = "sub_kpk11_chem",
            question = "The Balmer series of the hydrogen spectrum lies in the:",
            options = listOf("A" to "Ultraviolet region", "B" to "Visible region", "C" to "Infrared region", "D" to "X-ray region"),
            correctOptionId = "B",
            explanation = "Transitions terminating at n₁ = 2 emit photons in the visible Balmer series.",
            difficulty = "easy",
            topicTitle = "Hydrogen Emission Spectral Series",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_bio11_01_1",
            chapterId = "ch_bio11_01",
            subjectId = "sub_kpk11_bio",
            question = "Fluid Mosaic Model of plasma membrane was proposed by:",
            options = listOf("A" to "Robert Hooke", "B" to "Singer and Nicolson", "C" to "Watson and Crick", "D" to "Robert Brown"),
            correctOptionId = "B",
            explanation = "S.J. Singer and G.L. Nicolson proposed the Fluid Mosaic Model in 1972.",
            difficulty = "easy",
            topicTitle = "Plasma Membrane",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_bio11_02_1",
            chapterId = "ch_bio11_02",
            subjectId = "sub_kpk11_bio",
            question = "Amino acids in proteins are linked together by:",
            options = listOf("A" to "Glycosidic bond", "B" to "Ester bond", "C" to "Peptide bond", "D" to "Hydrogen bond only"),
            correctOptionId = "C",
            explanation = "A covalent peptide (-CO-NH-) bond links adjacent amino acid residues.",
            difficulty = "easy",
            topicTitle = "Proteins",
            pastPaperYear = 2023
        ),
        ApiMcqQuestionDto(
            id = "mcq_math11_01_1",
            chapterId = "ch_math11_01",
            subjectId = "sub_kpk11_math",
            question = "The value of i²⁵ equals:",
            options = listOf("A" to "1", "B" to "-1", "C" to "i", "D" to "-i"),
            correctOptionId = "C",
            explanation = "i²⁵ = (i⁴)⁶ · i = 1 · i = i.",
            difficulty = "easy",
            topicTitle = "Powers of i",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_math11_02_1",
            chapterId = "ch_math11_02",
            subjectId = "sub_kpk11_math",
            question = "A square matrix A is singular if:",
            options = listOf("A" to "|A| = 1", "B" to "|A| = 0", "C" to "|A| > 0", "D" to "A = Aᵀ"),
            correctOptionId = "B",
            explanation = "A matrix whose determinant is zero has no multiplicative inverse and is called singular.",
            difficulty = "easy",
            topicTitle = "Determinants",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_cs11_01_1",
            chapterId = "ch_cs11_01",
            subjectId = "sub_kpk11_cs",
            question = "Which memory retains startup BIOS instructions even when power is turned off?",
            options = listOf("A" to "DRAM", "B" to "SRAM Cache", "C" to "ROM", "D" to "CPU Register"),
            correctOptionId = "C",
            explanation = "ROM is non-volatile memory storing permanent firmware such as BIOS/UEFI.",
            difficulty = "easy",
            topicTitle = "Memory Hierarchy",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_cs11_02_1",
            chapterId = "ch_cs11_02",
            subjectId = "sub_kpk11_cs",
            question = "Which system bus is unidirectional from CPU to main memory?",
            options = listOf("A" to "Data bus", "B" to "Address bus", "C" to "USB bus", "D" to "External bus"),
            correctOptionId = "B",
            explanation = "The CPU places target memory addresses onto the unidirectional Address Bus.",
            difficulty = "medium",
            topicTitle = "System Buses",
            pastPaperYear = 2023
        ),
        ApiMcqQuestionDto(
            id = "mcq_eng11_01_1",
            chapterId = "ch_eng11_01",
            subjectId = "sub_kpk11_eng",
            question = "Where did Quaid-e-Azam deliver the address \"Responsibilities of Youth\"?",
            options = listOf("A" to "Peshawar University", "B" to "Dhaka University Convocation", "C" to "Lahore Islamia College", "D" to "Karachi Bar Association"),
            correctOptionId = "B",
            explanation = "Delivered at the Dhaka University Convocation on 24 March 1948.",
            difficulty = "easy",
            topicTitle = "Prose Comprehension",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_urd11_01_1",
            chapterId = "ch_urd11_01",
            subjectId = "sub_kpk11_urd",
            question = "سبق \"اسوہ حسنہ ﷺ\" کے مصنف کون ہیں؟",
            options = listOf("A" to "شبلی نعمانی", "B" to "سید سلیمان ندوی", "C" to "مولانا حالی", "D" to "سرسید احمد خان"),
            correctOptionId = "B",
            explanation = "یہ معروف مضمون سید سلیمان ندوی کی تصنیف خطباتِ مدراس سے ماخوذ ہے۔",
            difficulty = "easy",
            topicTitle = "نثری اسباق",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_isl11_01_1",
            chapterId = "ch_isl11_01",
            subjectId = "sub_kpk11_isl",
            question = "دینِ اسلام کا سب سے پہلا اور بنیادی عقیدہ کیا ہے؟",
            options = listOf("A" to "عقیدہ توحید", "B" to "زکوٰۃ", "C" to "حج", "D" to "جہاد"),
            correctOptionId = "A",
            explanation = "عقیدہ توحید یعنی اللہ تعالیٰ کو واحد ماننا اسلام کا اولین بنیادی رکن ہے۔",
            difficulty = "easy",
            topicTitle = "عقائد اسلام",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_eco11_01_1",
            chapterId = "ch_eco11_01",
            subjectId = "sub_kpk11_eco",
            question = "Who defined economics as the science of scarcity and choice?",
            options = listOf("A" to "Adam Smith", "B" to "Alfred Marshall", "C" to "Lionel Robbins", "D" to "J.M. Keynes"),
            correctOptionId = "C",
            explanation = "Lionel Robbins (1932) defined economics around scarce means with alternative uses.",
            difficulty = "easy",
            topicTitle = "Economic Definitions",
            pastPaperYear = 2024
        ),
        ApiMcqQuestionDto(
            id = "mcq_acc11_01_1",
            chapterId = "ch_acc11_01",
            subjectId = "sub_kpk11_acc",
            question = "The fundamental accounting equation states:",
            options = listOf("A" to "Assets + Liabilities = Capital", "B" to "Assets = Liabilities + Owner Equity", "C" to "Capital = Assets + Liabilities", "D" to "Revenue = Expenses + Assets"),
            correctOptionId = "B",
            explanation = "Assets always equal the sum of Liabilities and Owner Equity.",
            difficulty = "easy",
            topicTitle = "Accounting Equation",
            pastPaperYear = 2024
        ),

        // Class 9 Physics Ch 1
        ApiMcqQuestionDto(
            id = "mcq_phy9_1",
            chapterId = "ch_phy9_01",
            subjectId = "sub_kpk9_phy",
            question = "Which of the following is a derived physical quantity?",
            options = listOf("A" to "Length", "B" to "Time", "C" to "Velocity", "D" to "Temperature"),
            correctOptionId = "C",
            explanation = "Velocity is derived by dividing displacement (length) by time.",
            difficulty = "easy",
            topicTitle = "Base & Derived Units",
            pastPaperYear = 2023,
            questionUrdu = "درج ذیل میں سے کون سی ماخوذ طبیعی مقدار ہے؟",
            optionsUrdu = listOf("لمبائی", "وقت", "ولوسٹی (رفتار)", "درجہ حرارت"),
            explanationUrdu = "ولوسٹی (رفتار) ہٹاؤ کو وقت پر تقسیم کرنے سے حاصل ہوتی ہے۔"
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy9_2",
            chapterId = "ch_phy9_01",
            subjectId = "sub_kpk9_phy",
            question = "The least count of a standard Vernier Callipers is:",
            options = listOf("A" to "0.1 cm", "B" to "0.01 cm", "C" to "0.001 cm", "D" to "1.0 mm"),
            correctOptionId = "B",
            explanation = "Vernier Callipers measures accurately up to 0.01 cm (0.1 mm).",
            difficulty = "easy",
            topicTitle = "Measuring Instruments & Significant Figures",
            pastPaperYear = 2022,
            questionUrdu = "معیاری ورنیئر کیلیپرز کا لیسٹ کاؤنٹ کیا ہے؟",
            optionsUrdu = listOf("0.1 سینٹی میٹر", "0.01 سینٹی میٹر", "0.001 سینٹی میٹر", "1.0 ملی میٹر"),
            explanationUrdu = "ورنیئر کیلیپرز کم از کم 0.01 سینٹی میٹر (0.1 ملی میٹر) تک درست پیمائش کرتا ہے۔"
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy9_3",
            chapterId = "ch_phy9_01",
            subjectId = "sub_kpk9_phy",
            question = "One mega (M) prefix equals:",
            options = listOf("A" to "10³", "B" to "10⁶", "C" to "10⁹", "D" to "10⁻⁶"),
            correctOptionId = "B",
            explanation = "Mega (M) represents 10⁶ = 1,000,000.",
            difficulty = "easy",
            topicTitle = "Prefixes & Scientific Notation",
            questionUrdu = "ایک میگا (M) پریفکس برابر ہوتا ہے:",
            optionsUrdu = listOf("10³", "10⁶", "10⁹", "10⁻⁶"),
            explanationUrdu = "میگا (M) دس لاکھ یعنی 10⁶ کے برابر ہے۔"
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy9_4",
            chapterId = "ch_phy9_01",
            subjectId = "sub_kpk9_phy",
            question = "How many significant figures are in 0.00450 kg?",
            options = listOf("A" to "2", "B" to "3", "C" to "5", "D" to "6"),
            correctOptionId = "B",
            explanation = "Leading zeros are not significant; 4, 5, and trailing zero after decimal are significant (3 figures).",
            difficulty = "medium",
            topicTitle = "Measuring Instruments & Significant Figures"
        ),
        ApiMcqQuestionDto(
            id = "mcq_phy9_5",
            chapterId = "ch_phy9_01",
            subjectId = "sub_kpk9_phy",
            question = "The SI base unit of luminous intensity is:",
            options = listOf("A" to "mole", "B" to "kelvin", "C" to "candela", "D" to "ampere"),
            correctOptionId = "C",
            explanation = "Candela (cd) is the SI unit of luminous intensity.",
            difficulty = "medium",
            topicTitle = "Base & Derived Units"
        ),
        ApiMcqQuestionDto(
            id = "mcq_chem9_1",
            chapterId = "ch_chem9_01",
            subjectId = "sub_kpk9_chem",
            question = "Avogadro's number equals how many particles per mole?",
            options = listOf("A" to "6.02 × 10²³", "B" to "6.02 × 10²²", "C" to "3.01 × 10²³", "D" to "1.60 × 10⁻¹⁹"),
            correctOptionId = "A",
            explanation = "One mole of any substance contains 6.02 × 10²³ particles.",
            difficulty = "easy",
            topicTitle = "Mole & Avogadro Calculations"
        ),
        ApiMcqQuestionDto(
            id = "mcq_math9_1",
            chapterId = "ch_math9_01",
            subjectId = "sub_kpk9_math",
            question = "A matrix with only one row is called a:",
            options = listOf("A" to "Column matrix", "B" to "Row matrix", "C" to "Null matrix", "D" to "Diagonal matrix"),
            correctOptionId = "B",
            explanation = "A matrix of order 1 × n is a row matrix.",
            difficulty = "easy",
            topicTitle = "Types & Operations on Matrices"
        ),

        // Preserved Class 10 MCQs (needed by existing CurriculumAndQuizTests)
        ApiMcqQuestionDto(
            id = "mcq_1",
            chapterId = "ch_phy10_01",
            subjectId = "sub_kpk10_phy",
            question = "What is the value of acceleration due to gravity on Earth?",
            options = listOf("A" to "9.8 m/s²", "B" to "9.8 km/s²", "C" to "10 m/s", "D" to "9.6 m/s²"),
            correctOptionId = "A",
            explanation = "Standard acceleration due to gravity near Earth's surface is 9.8 m/s².",
            topicTitle = "SHM & Gravity"
        ),
        ApiMcqQuestionDto(
            id = "mcq_2",
            chapterId = "ch_phy10_01",
            subjectId = "sub_kpk10_phy",
            question = "In Simple Harmonic Motion, the acceleration of a body is always directed:",
            options = listOf(
                "A" to "Away from mean position",
                "B" to "Toward the mean position",
                "C" to "Perpendicular to displacement",
                "D" to "Along the tangent only"
            ),
            correctOptionId = "B",
            explanation = "By Hooke's law, a ∝ -x, meaning acceleration always points toward equilibrium.",
            topicTitle = "SHM & Gravity"
        ),
        ApiMcqQuestionDto(
            id = "mcq_3",
            chapterId = "ch_phy10_01",
            subjectId = "sub_kpk10_phy",
            question = "Which formula relates wave speed v, frequency f, and wavelength λ?",
            options = listOf("A" to "v = f / λ", "B" to "v = λ / f", "C" to "v = fλ", "D" to "v = f²λ"),
            correctOptionId = "C",
            explanation = "Wave speed equals frequency multiplied by wavelength: v = fλ.",
            topicTitle = "Wave Equation"
        ),
        ApiMcqQuestionDto(
            id = "mcq_4",
            chapterId = "ch_phy10_01",
            subjectId = "sub_kpk10_phy",
            question = "If the length of a simple pendulum is quadrupled, its time period becomes:",
            options = listOf("A" to "Half", "B" to "Double", "C" to "Four times", "D" to "Unchanged"),
            correctOptionId = "B",
            explanation = "Since T ∝ √l, multiplying length by 4 multiplies T by √4 = 2.",
            topicTitle = "Simple Pendulum"
        ),
        ApiMcqQuestionDto(
            id = "mcq_5",
            chapterId = "ch_phy10_01",
            subjectId = "sub_kpk10_phy",
            question = "At the extreme position of an oscillating pendulum, which quantity is zero?",
            options = listOf("A" to "Acceleration", "B" to "Potential Energy", "C" to "Velocity", "D" to "Restoring Force"),
            correctOptionId = "C",
            explanation = "The bob momentarily stops at each extreme point, so instantaneous velocity is zero.",
            topicTitle = "Simple Pendulum"
        )
    )

    val flashcards: List<ApiFlashcardDto> = listOf(
        ApiFlashcardDto(
            id = "fc_phy11_01_1",
            subjectId = "sub_kpk11_phy",
            chapterId = "ch_phy11_01",
            front = "Define Radian (SI Supplementary Unit)",
            back = "Plane angle subtended at the centre of a circle by an arc equal in length to the radius.",
            formulaHint = "1 rev = 2π rad = 360°",
            topicTitle = "SI Units",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null,
            frontUrdu = "ریڈین کی تعریف بیان کریں",
            backUrdu = "دائرے کے مرکز پر وہ زاویہ جو رداس کے برابر قوس بنائے۔"
        ),
        ApiFlashcardDto(
            id = "fc_phy11_02_1",
            subjectId = "sub_kpk11_phy",
            chapterId = "ch_phy11_02",
            front = "State the two conditions of static equilibrium",
            back = "First: ΣF = 0 (no linear acceleration). Second: Στ = 0 (no angular acceleration).",
            formulaHint = "τ = r × F",
            topicTitle = "Equilibrium",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null,
            frontUrdu = "توازن کی دو شرائط کیا ہیں؟",
            backUrdu = "پہلی شرط: کل قوت صفر (ΣF = 0)؛ دوسری شرط: کل ٹارک صفر (Στ = 0)۔"
        ),
        ApiFlashcardDto(
            id = "fc_chem11_01_1",
            subjectId = "sub_kpk11_chem",
            chapterId = "ch_chem11_01",
            front = "What is Avogadro Number and Molar Gas Volume at STP?",
            back = "6.022 × 10²³ particles per mole; 1 mole ideal gas occupies 22.414 dm³ at STP.",
            formulaHint = "n = m / M = V / 22.414",
            topicTitle = "Stoichiometry",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null
        ),
        ApiFlashcardDto(
            id = "fc_bio11_01_1",
            subjectId = "sub_kpk11_bio",
            chapterId = "ch_bio11_01",
            front = "70S vs 80S Ribosomes in Cells",
            back = "70S ribosomes occur in prokaryotes, mitochondria & chloroplasts; 80S ribosomes occur in eukaryotic cytosol.",
            formulaHint = "S = Svedberg sedimentation unit",
            topicTitle = "Organelles",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null
        ),
        ApiFlashcardDto(
            id = "fc_math11_01_1",
            subjectId = "sub_kpk11_math",
            chapterId = "ch_math11_01",
            front = "Modulus and Multiplicative Inverse of z = a + bi",
            back = "|z| = √(a² + b²); z⁻¹ = (a - bi) / (a² + b²).",
            formulaHint = "z · z̄ = |z|²",
            topicTitle = "Complex Numbers",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null
        ),
        ApiFlashcardDto(
            id = "fc_cs11_01_1",
            subjectId = "sub_kpk11_cs",
            chapterId = "ch_cs11_01",
            front = "What is the Fetch-Decode-Execute Cycle?",
            back = "CPU fetches instruction at PC from RAM, Control Unit decodes opcode, ALU/CPU executes and writes result.",
            formulaHint = "PC → MAR → MDR → IR",
            topicTitle = "CPU Architecture",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null
        ),
        ApiFlashcardDto(
            id = "fc_acc11_01_1",
            subjectId = "sub_kpk11_acc",
            chapterId = "ch_acc11_01",
            front = "What increases and decreases Owner Equity?",
            back = "Owner Investment & Revenues increase Equity; Drawings & Expenses decrease Equity.",
            formulaHint = "Assets = Liabilities + Equity",
            topicTitle = "Accounting Equation",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null
        ),
        ApiFlashcardDto(
            id = "fc_phy9_01",
            subjectId = "sub_kpk9_phy",
            chapterId = "ch_phy9_01",
            front = "What is the Least Count of a Vernier Callipers?",
            back = "0.1 mm or 0.01 cm (difference between 1 MSD and 1 VSD).",
            formulaHint = "LC = 1 mm / 10 divisions",
            topicTitle = "Measuring Instruments",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null,
            frontUrdu = "ورنیئر کیلیپرز کا لیسٹ کاؤنٹ کیا ہے؟",
            backUrdu = "0.1 ملی میٹر یا 0.01 سینٹی میٹر۔"
        ),
        ApiFlashcardDto(
            id = "fc_phy9_02",
            subjectId = "sub_kpk9_phy",
            chapterId = "ch_phy9_01",
            front = "List the 7 SI Base Units.",
            back = "metre (m), kilogram (kg), second (s), ampere (A), kelvin (K), mole (mol), candela (cd).",
            formulaHint = null,
            topicTitle = "Base & Derived Units",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null,
            frontUrdu = "سات بنیادی SI اکائیاں کون سی ہیں؟",
            backUrdu = "میٹر، کلوگرام، سیکنڈ، ایمپیئر، کیلون، مول، کینڈیلا۔"
        ),
        ApiFlashcardDto(
            id = "fc_phy9_03",
            subjectId = "sub_kpk9_phy",
            chapterId = "ch_phy9_02",
            front = "State Newton's First Equation of Motion.",
            back = "Relates final velocity, initial velocity, uniform acceleration, and time.",
            formulaHint = "vf = vi + at",
            topicTitle = "Equations of Motion",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null,
            frontUrdu = "حرکت کی پہلی مساوات بیان کریں۔",
            backUrdu = "vf = vi + at (آخری رفتار، ابتدائی رفتار، اسراع اور وقت کا تعلق)۔"
        ),
        ApiFlashcardDto(
            id = "fc_chem9_01",
            subjectId = "sub_kpk9_chem",
            chapterId = "ch_chem9_01",
            front = "What is Avogadro's Number (NA)?",
            back = "6.02 × 10²³ particles present in exactly 1 mole of any substance.",
            formulaHint = "n = Given Mass / Molar Mass",
            topicTitle = "Mole Concept",
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            nextReviewAtIso = "",
            lastReviewedAtIso = null,
            frontUrdu = "ایوگیڈرو نمبر (NA) کیا ہے؟",
            backUrdu = "کسی بھی شے کے ایک مول میں موجود 6.02×10²³ ذرات کی تعداد۔"
        )
    )

    val examPatterns: List<ApiExamPatternDto> = listOf(
        ApiExamPatternDto(
            id = "pat_kpk11_diag",
            title = "KPK Class 11 Quick Diagnostic",
            mode = "diagnostic_test",
            classLevel = 11,
            boardCode = "KPK",
            subjectId = null,
            questionCount = 5,
            durationMinutes = 8,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.0,
            passPercentage = 40,
            includesShortQuestionsCount = 0,
            includesLongQuestionsCount = 0,
            sectionSummary = "Quick HSSC-I baseline across Physics, Chem/Bio/Math & CS"
        ),
        ApiExamPatternDto(
            id = "pat_kpk11_chap",
            title = "Class 11 Chapter Mastery Test",
            mode = "chapter_test",
            classLevel = 11,
            boardCode = "KPK",
            subjectId = null,
            questionCount = 4,
            durationMinutes = 10,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.0,
            passPercentage = 50,
            includesShortQuestionsCount = 2,
            includesLongQuestionsCount = 0,
            sectionSummary = "Chapter MCQs + Short Questions Review"
        ),
        ApiExamPatternDto(
            id = "pat_kpk11_phy_mock",
            title = "BISE KPK Class 11 Physics Full Paper Pattern",
            mode = "full_mock_exam",
            classLevel = 11,
            boardCode = "KPK",
            subjectId = "sub_kpk11_phy",
            questionCount = 7,
            durationMinutes = 25,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.0,
            passPercentage = 33,
            includesShortQuestionsCount = 3,
            includesLongQuestionsCount = 1,
            sectionSummary = "Sec-A MCQs + Sec-B Short + Sec-C Numerical/Long Questions"
        ),
        ApiExamPatternDto(
            id = "pat_kpk11_chem_mock",
            title = "BISE KPK Class 11 Chemistry Pattern",
            mode = "full_mock_exam",
            classLevel = 11,
            boardCode = "KPK",
            subjectId = "sub_kpk11_chem",
            questionCount = 5,
            durationMinutes = 20,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.0,
            passPercentage = 33,
            includesShortQuestionsCount = 3,
            includesLongQuestionsCount = 1,
            sectionSummary = "Sec-A MCQs + Sec-B Stoichiometry/Bohr Short + Long"
        ),
        ApiExamPatternDto(
            id = "pat_kpk9_phy_mock",
            title = "KPK BISE Class 9 Physics Model Paper",
            mode = "full_mock_exam",
            classLevel = 9,
            boardCode = "KPK",
            subjectId = "sub_kpk9_phy",
            questionCount = 12,
            durationMinutes = 20,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.0,
            passPercentage = 40,
            includesShortQuestionsCount = 3,
            includesLongQuestionsCount = 1,
            sectionSummary = "Section A: 12 Objective MCQs · Section B/C: Short & Long Board Rubrics"
        ),
        ApiExamPatternDto(
            id = "pat_kpk9_entry_neg",
            title = "KPK Scholarship / Entry Timed Drill (-0.25 Marking)",
            mode = "subject_test",
            classLevel = 9,
            boardCode = "KPK",
            subjectId = null,
            questionCount = 10,
            durationMinutes = 12,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.25,
            passPercentage = 50,
            includesShortQuestionsCount = 0,
            includesLongQuestionsCount = 0,
            sectionSummary = "High-discipline timed MCQs with -0.25 negative marking per wrong choice"
        ),
        ApiExamPatternDto(
            id = "pat_kpk10_phy_mock",
            title = "KPK BISE Class 10 Physics Model Paper",
            mode = "full_mock_exam",
            classLevel = 10,
            boardCode = "KPK",
            subjectId = "sub_kpk10_phy",
            questionCount = 12,
            durationMinutes = 20,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.0,
            passPercentage = 40,
            includesShortQuestionsCount = 3,
            includesLongQuestionsCount = 1,
            sectionSummary = "Section A Objective + Section B/C Subjective Checklist"
        )
    )

    val pastPapers: List<ApiPastPaperDto> = listOf(
        ApiPastPaperDto("pp_kpk11_phy_2025", "KPK", 11, "sub_kpk11_phy", "Physics (HSSC-I)", 2025, true, "pdf_pp_phy11_2025", 1.5, 8),
        ApiPastPaperDto("pp_kpk11_math_2025", "KPK", 11, "sub_kpk11_math", "Mathematics (HSSC-I)", 2025, false, "pdf_pp_math11_2025", 1.25, 6),
        ApiPastPaperDto("pp_phy9_2023", "KPK", 9, "sub_kpk9_phy", "Physics", 2023, true, "pdf_pp_phy9_2023", 2.1, 10),
        ApiPastPaperDto("pp_math9_2022", "KPK", 9, "sub_kpk9_math", "Mathematics", 2022, false, "pdf_pp_math9_2022", 2.4, 12),
        ApiPastPaperDto("pp_chem9_2023", "KPK", 9, "sub_kpk9_chem", "Chemistry", 2023, true, "pdf_pp_chem9_2023", 1.9, 9),
        ApiPastPaperDto("pp_math_2019", "KPK", 10, "sub_kpk10_math", "Mathematics", 2019, true, "pdf_001", 2.5, 12),
        ApiPastPaperDto("pp_phy_2020", "KPK", 10, "sub_kpk10_phy", "Physics", 2020, false, "pdf_002", 1.8, 8),
        ApiPastPaperDto("pp_chem_2021", "KPK", 10, "sub_kpk10_chem", "Chemistry", 2021, true, "pdf_003", 2.3, 10),
        ApiPastPaperDto("pp_bio_2022", "KPK", 10, "sub_kpk10_bio", "Biology", 2022, false, "pdf_004", 1.9, 9)
    )

    val pdfLibrary: List<ApiPdfMetadataDto> = listOf(
        ApiPdfMetadataDto("pdf_tb_phy11", "KPK Physics Class 11 Textbook (HSSC-I)", "textbook", 11, "KPK", "sub_kpk11_phy", "kpk/class-11/physics/physics-11-kpk-textbook.pdf", 4_194_304L, 240, "", true, "1.0"),
        ApiPdfMetadataDto("pdf_pp_phy11_2025", "BISE Peshawar Class 11 Physics Annual 2025 (Solved)", "past_paper", 11, "KPK", "sub_kpk11_phy", "kpk/class-11/physics/2025-annual-solved.pdf", 1_572_864L, 8, "", true, "1.0"),
        ApiPdfMetadataDto("pdf_pp_math11_2025", "BISE KPK Class 11 Mathematics Annual 2025", "past_paper", 11, "KPK", "sub_kpk11_math", "kpk/class-11/mathematics/2025-annual.pdf", 1_310_720L, 6, "", true, "1.0"),
        ApiPdfMetadataDto("pdf_tb_phy9", "KPK Physics Class 9 Textbook (English)", "textbook", 9, "KPK", "sub_kpk9_phy", "textbooks/kpk/class9/physics-9-en.pdf", 8_400_000L, 196, "sha_phy9", true, "v2025"),
        ApiPdfMetadataDto("pdf_tb_chem9", "KPK Chemistry Class 9 Textbook (English)", "textbook", 9, "KPK", "sub_kpk9_chem", "textbooks/kpk/class9/chemistry-9-en.pdf", 7_100_000L, 168, "sha_chem9", true, "v2025"),
        ApiPdfMetadataDto("pdf_tb_math9", "KPK Mathematics Class 9 Textbook", "textbook", 9, "KPK", "sub_kpk9_math", "textbooks/kpk/class9/math-9-en.pdf", 9_200_000L, 280, "sha_math9", true, "v2025"),
        ApiPdfMetadataDto("pdf_syl_kpk9", "KPK BISE Class 9 Official Pairing Scheme & Syllabus", "syllabus", 9, "KPK", "sub_kpk9_phy", "syllabi/kpk/class9-scheme.pdf", 1_100_000L, 16, "sha_syl9", true, "v2025"),
        ApiPdfMetadataDto("pdf_notes_phy9_ch1", "Physics Class 9 Ch 1 Formula & Revision Pack", "notes_pack", 9, "KPK", "sub_kpk9_phy", "notes/kpk/class9/phy-ch1-pack.pdf", 1_400_000L, 14, "sha_np9", true, "v1.2")
    )
}
