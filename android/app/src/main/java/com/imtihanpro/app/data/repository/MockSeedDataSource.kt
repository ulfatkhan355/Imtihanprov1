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
 * Primary starter release focus: KPK Class 9 English-medium (Science Biology & CS),
 * while preserving Class 10 KPK sample data and structural Class 11/12 skeletons.
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
            textbookPdfId = "pdf_tb_phy9"
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
            textbookPdfId = "pdf_tb_chem9"
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
            textbookPdfId = "pdf_tb_math9"
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
            description = "Introduction to Biology, Cells, Enzymes & Bioenergetics"
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
            description = "Problem Solving, Number Systems, HTML & Networks (KPK Class 9)"
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

        // Class 11 & 12 structural entries
        ApiSubjectDto("sub_kpk11_phy", 11, "KPK", "PHY-11", "Physics (HSSC-I)", "physics", 11, 0, "structure_ready", listOf("PRE_MEDICAL", "PRE_ENGINEERING", "ICS")),
        ApiSubjectDto("sub_kpk12_math", 12, "KPK", "MTH-12", "Mathematics (HSSC-II)", "math", 7, 0, "structure_ready", listOf("PRE_ENGINEERING", "ICS"))
    )

    val chapters: List<ApiChapterDto> = listOf(
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
            sourceReference = "KPK Textbook Board Peshawar — Physics 9 Ch 1"
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
            sourceReference = "KPK Textbook Board Peshawar — Physics 9 Ch 2"
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
            difficultyTag = "medium"
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
            difficultyTag = "medium"
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
            difficultyTag = "easy"
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
            difficultyTag = "easy"
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
            estimatedSizeKb = 420
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
            estimatedSizeKb = 380
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
            pastPaperYear = 2023
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
            pastPaperYear = 2022
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
            topicTitle = "Prefixes & Scientific Notation"
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
            lastReviewedAtIso = null
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
            lastReviewedAtIso = null
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
            lastReviewedAtIso = null
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
            lastReviewedAtIso = null
        )
    )

    val examPatterns: List<ApiExamPatternDto> = listOf(
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
        ApiPastPaperDto("pp_phy9_2023", "KPK", 9, "sub_kpk9_phy", "Physics", 2023, true, "pdf_pp_phy9_2023", 2.1, 10),
        ApiPastPaperDto("pp_math9_2022", "KPK", 9, "sub_kpk9_math", "Mathematics", 2022, false, "pdf_pp_math9_2022", 2.4, 12),
        ApiPastPaperDto("pp_chem9_2023", "KPK", 9, "sub_kpk9_chem", "Chemistry", 2023, true, "pdf_pp_chem9_2023", 1.9, 9),
        ApiPastPaperDto("pp_math_2019", "KPK", 10, "sub_kpk10_math", "Mathematics", 2019, true, "pdf_001", 2.5, 12),
        ApiPastPaperDto("pp_phy_2020", "KPK", 10, "sub_kpk10_phy", "Physics", 2020, false, "pdf_002", 1.8, 8),
        ApiPastPaperDto("pp_chem_2021", "KPK", 10, "sub_kpk10_chem", "Chemistry", 2021, true, "pdf_003", 2.3, 10),
        ApiPastPaperDto("pp_bio_2022", "KPK", 10, "sub_kpk10_bio", "Biology", 2022, false, "pdf_004", 1.9, 9)
    )

    val pdfLibrary: List<ApiPdfMetadataDto> = listOf(
        ApiPdfMetadataDto("pdf_tb_phy9", "KPK Physics Class 9 Textbook (English)", "textbook", 9, "KPK", "sub_kpk9_phy", "textbooks/kpk/class9/physics-9-en.pdf", 8_400_000L, 196, "sha_phy9", true, "v2025"),
        ApiPdfMetadataDto("pdf_tb_chem9", "KPK Chemistry Class 9 Textbook (English)", "textbook", 9, "KPK", "sub_kpk9_chem", "textbooks/kpk/class9/chemistry-9-en.pdf", 7_100_000L, 168, "sha_chem9", true, "v2025"),
        ApiPdfMetadataDto("pdf_tb_math9", "KPK Mathematics Class 9 Textbook", "textbook", 9, "KPK", "sub_kpk9_math", "textbooks/kpk/class9/math-9-en.pdf", 9_200_000L, 280, "sha_math9", true, "v2025"),
        ApiPdfMetadataDto("pdf_syl_kpk9", "KPK BISE Class 9 Official Pairing Scheme & Syllabus", "syllabus", 9, "KPK", "sub_kpk9_phy", "syllabi/kpk/class9-scheme.pdf", 1_100_000L, 16, "sha_syl9", true, "v2025"),
        ApiPdfMetadataDto("pdf_notes_phy9_ch1", "Physics Class 9 Ch 1 Formula & Revision Pack", "notes_pack", 9, "KPK", "sub_kpk9_phy", "notes/kpk/class9/phy-ch1-pack.pdf", 1_400_000L, 14, "sha_np9", true, "v1.2")
    )
}
