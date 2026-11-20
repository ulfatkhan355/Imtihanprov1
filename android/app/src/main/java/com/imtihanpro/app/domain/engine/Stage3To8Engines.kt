package com.imtihanpro.app.domain.engine

import com.imtihanpro.app.domain.model.AdPlacementPolicyUi
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.HonestReadinessUi
import com.imtihanpro.app.domain.model.KpkSubBoardUi
import com.imtihanpro.app.domain.model.McqQuestionUi
import com.imtihanpro.app.domain.model.QuizResultUi
import com.imtihanpro.app.domain.model.QuizReviewItemUi
import com.imtihanpro.app.domain.model.StudyGroupOptionUi
import com.imtihanpro.app.domain.model.TopicScoreBreakdownUi
import com.imtihanpro.app.domain.model.WeakChapterUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure deterministic domain engines for Stages 3-8 mirroring /shared-contracts/rules.ts.
 * Zero Android framework dependencies so every rule is unit-tested on JVM.
 */
object Stage3To8Engines {

    val KPK_SUB_BOARDS: List<KpkSubBoardUi> = listOf(
        KpkSubBoardUi("PESHAWAR", "BISE Peshawar", "Peshawar"),
        KpkSubBoardUi("MARDAN", "BISE Mardan", "Mardan"),
        KpkSubBoardUi("SWAT", "BISE Swat", "Saidu Sharif, Swat"),
        KpkSubBoardUi("ABBOTTABAD", "BISE Abbottabad", "Abbottabad"),
        KpkSubBoardUi("KOHAT", "BISE Kohat", "Kohat"),
        KpkSubBoardUi("BANNU", "BISE Bannu", "Bannu"),
        KpkSubBoardUi("DI_KHAN", "BISE D.I. Khan", "Dera Ismail Khan"),
        KpkSubBoardUi("MALAKAND", "BISE Malakand", "Chakdara, Malakand")
    )

    val STUDY_GROUPS: List<StudyGroupOptionUi> = listOf(
        StudyGroupOptionUi(
            code = "SCIENCE_BIOLOGY",
            label = "Science (Biology)",
            applicableClasses = listOf(9, 10),
            description = "Physics, Chemistry, Mathematics, Biology, English & Urdu"
        ),
        StudyGroupOptionUi(
            code = "SCIENCE_CS",
            label = "Science (Computer Science)",
            applicableClasses = listOf(9, 10),
            description = "Physics, Chemistry, Mathematics, Computer Science, English & Urdu"
        ),
        StudyGroupOptionUi(
            code = "ARTS_HUMANITIES",
            label = "Arts / Humanities",
            applicableClasses = listOf(9, 10),
            description = "General Science, General Math, English, Urdu & Electives"
        ),
        StudyGroupOptionUi(
            code = "PRE_MEDICAL",
            label = "FSc Pre-Medical",
            applicableClasses = listOf(11, 12),
            description = "Physics, Chemistry, Biology, English & Urdu"
        ),
        StudyGroupOptionUi(
            code = "PRE_ENGINEERING",
            label = "FSc Pre-Engineering",
            applicableClasses = listOf(11, 12),
            description = "Physics, Chemistry, Mathematics, English & Urdu"
        ),
        StudyGroupOptionUi(
            code = "ICS",
            label = "ICS (Computer Science)",
            applicableClasses = listOf(11, 12),
            description = "Physics/Stats, Mathematics, Computer Science, English"
        ),
        StudyGroupOptionUi(
            code = "FA",
            label = "F.A. (Humanities)",
            applicableClasses = listOf(11, 12),
            description = "Humanities & Languages track"
        ),
        StudyGroupOptionUi(
            code = "ICOM",
            label = "I.Com (Commerce)",
            applicableClasses = listOf(11, 12),
            description = "Accounting, Commerce & Economics track"
        )
    )

    fun studyGroupsForClass(classLevel: Int): List<StudyGroupOptionUi> {
        val matches = STUDY_GROUPS.filter { classLevel in it.applicableClasses }
        return matches.ifEmpty { STUDY_GROUPS }
    }

    fun defaultStudyGroupForClass(classLevel: Int): String =
        if (classLevel >= 11) "PRE_MEDICAL" else "SCIENCE_BIOLOGY"

    /**
     * Grades an MCQ practice or mock exam session with optional negative marking, pass percentage,
     * topic-level breakdown, and XP calculation.
     */
    fun gradeQuizWithRules(
        questions: List<McqQuestionUi>,
        selectedAnswers: Map<String, String>,
        elapsedSeconds: Int,
        marksPerMcq: Double = 1.0,
        negativeMarkingPenalty: Double = 0.0,
        passPercentage: Int = 40
    ): QuizResultUi {
        var correct = 0
        var incorrect = 0
        var unanswered = 0
        val topicAgg = linkedMapOf<String, Pair<Int, Int>>() // correct to total

        val review = questions.map { q ->
            val chosen = selectedAnswers[q.id]
            val isCorrect = chosen != null && q.correctOptionId != null && chosen == q.correctOptionId
            when {
                chosen == null -> unanswered += 1
                isCorrect -> correct += 1
                else -> incorrect += 1
            }
            val topic = q.topicTitle.ifBlank { "Core Concepts" }
            val prev = topicAgg[topic] ?: (0 to 0)
            topicAgg[topic] = (prev.first + if (isCorrect) 1 else 0) to (prev.second + 1)

            QuizReviewItemUi(
                mcqId = q.id,
                question = q.question,
                selectedOptionId = chosen,
                correctOptionId = q.correctOptionId,
                explanation = q.explanation ?: "",
                isCorrect = isCorrect,
                topicTitle = topic
            )
        }

        val total = questions.size
        val rawMarks = max(0.0, (correct * marksPerMcq) - (incorrect * negativeMarkingPenalty))
        val marksObtained = (rawMarks * 100.0).roundToInt() / 100.0
        val totalMarks = (total * marksPerMcq * 100.0).roundToInt() / 100.0
        val pct = if (totalMarks <= 0.0) 0 else ((marksObtained / totalMarks) * 100.0).roundToInt().coerceIn(0, 100)
        val passed = total > 0 && pct >= passPercentage
        val xpEarned = max(5, correct * 10 + if (pct >= 80 && total >= 3) 15 else 0)

        val mins = elapsedSeconds / 60
        val secs = elapsedSeconds % 60
        val formattedTime = "%02d:%02d".format(Locale.US, mins, secs)

        val topicBreakdown = topicAgg.map { (topic, counts) ->
            val acc = if (counts.second == 0) 0 else ((counts.first.toFloat() / counts.second.toFloat()) * 100f).roundToInt()
            TopicScoreBreakdownUi(
                topicTitle = topic,
                correct = counts.first,
                total = counts.second,
                accuracyPercent = acc
            )
        }

        return QuizResultUi(
            totalQuestions = total,
            correctCount = correct,
            incorrectCount = incorrect,
            unansweredCount = unanswered,
            percentage = pct,
            marksObtained = marksObtained,
            totalMarks = totalMarks,
            passed = passed,
            passPercentage = passPercentage,
            xpEarned = xpEarned,
            elapsedTimeFormatted = formattedTime,
            reviewItems = review,
            topicBreakdown = topicBreakdown
        )
    }

    /**
     * SM-2 Spaced Repetition calculation (mirrors shared-contracts/rules.ts).
     */
    data class Sm2Result(
        val intervalDays: Int,
        val easeFactor: Double,
        val repetitions: Int,
        val nextReviewEpochMs: Long
    )

    fun calculateSm2NextReview(
        previousIntervalDays: Int,
        previousEaseFactor: Double,
        previousRepetitions: Int,
        quality: Int,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Sm2Result {
        val q = quality.coerceIn(0, 5)
        var ef = previousEaseFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        if (ef < 1.3) ef = 1.3
        val roundedEf = (ef * 100.0).roundToInt() / 100.0

        val reps: Int
        val interval: Int
        if (q < 3) {
            reps = 0
            interval = 1
        } else {
            reps = previousRepetitions + 1
            interval = when (reps) {
                1 -> 1
                2 -> 6
                else -> max(1, (previousIntervalDays * roundedEf).roundToInt())
            }
        }

        val nextEpochMs = nowEpochMs + interval.toLong() * 86_400_000L
        return Sm2Result(
            intervalDays = interval,
            easeFactor = roundedEf,
            repetitions = reps,
            nextReviewEpochMs = nextEpochMs
        )
    }

    /**
     * Honest Exam Readiness guardrail:
     * Never fabricates an artificial readiness score when < 2 quiz attempts exist.
     */
    fun computeHonestExamReadiness(
        quizAttemptsCount: Int,
        totalQuestionsAnswered: Int,
        overallAccuracyPercent: Int?,
        completedChaptersCount: Int,
        totalPublishedChaptersCount: Int,
        activeMistakesCount: Int
    ): HonestReadinessUi {
        if (quizAttemptsCount < 2 || totalQuestionsAnswered < 3 || overallAccuracyPercent == null) {
            return HonestReadinessUi(
                readinessPercent = null,
                status = "insufficient_data",
                message = "Complete at least 2 practice quizzes or a diagnostic test to calculate honest exam readiness."
            )
        }

        val coveragePct = if (totalPublishedChaptersCount > 0) {
            ((completedChaptersCount.toFloat() / totalPublishedChaptersCount.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        val mistakePenalty = minOf(15, activeMistakesCount * 2)
        val rawScore = (overallAccuracyPercent * 0.72 + coveragePct * 0.28 - mistakePenalty).roundToInt().coerceIn(0, 100)

        val (status, msg) = when {
            rawScore >= 80 -> "exam_ready" to "Strong command across practiced KPK topics. Focus on timed full mock papers."
            rawScore >= 55 -> "on_track" to "Solid foundation. Clear your active Mistake Master queue to cross 80%."
            else -> "building" to "Early preparation phase. Revise weak chapters and practice topic MCQs daily."
        }

        return HonestReadinessUi(
            readinessPercent = rawScore,
            status = status,
            message = msg
        )
    }

    /**
     * Identifies weak chapters from actual quiz accuracy, unresolved mistakes, and incomplete chapters.
     */
    fun detectWeakChapters(
        chapters: List<ChapterUiModel>,
        subjectNamesById: Map<String, String>,
        quizAccuracyByChapterId: Map<String, Pair<Int, Int>>, // attemptsCount to avgPercent
        activeMistakesByChapterId: Map<String, Int>,
        completionByChapterId: Map<String, Int>
    ): List<WeakChapterUi> {
        val candidates = mutableListOf<WeakChapterUi>()
        for (ch in chapters) {
            val stats = quizAccuracyByChapterId[ch.id]
            val mistakes = activeMistakesByChapterId[ch.id] ?: 0
            val completion = completionByChapterId[ch.id] ?: 0
            val avg = stats?.second
            val attempts = stats?.first ?: 0

            val isWeak = (attempts > 0 && (avg ?: 100) < 70) || mistakes >= 2 || (completion in 1..49)
            if (!isWeak) continue

            val priority = when {
                (attempts > 0 && (avg ?: 100) < 50) || mistakes >= 3 -> "high"
                attempts > 0 && (avg ?: 100) < 70 -> "medium"
                else -> "watch"
            }

            val reason = when {
                mistakes >= 2 && attempts > 0 -> "$avg% quiz accuracy and $mistakes unresolved mistake(s)."
                attempts > 0 && avg != null -> "Average quiz accuracy is $avg% across $attempts attempt(s)."
                mistakes > 0 -> "$mistakes unresolved mistake(s) waiting in Mistake Master."
                else -> "Started chapter ($completion% read) with no quiz verification yet."
            }

            candidates.add(
                WeakChapterUi(
                    chapterId = ch.id,
                    chapterTitle = ch.title,
                    subjectId = ch.subjectId,
                    subjectName = subjectNamesById[ch.subjectId] ?: "Subject",
                    accuracyPercent = avg,
                    attemptsCount = attempts,
                    completionPercent = completion,
                    reason = reason,
                    priority = priority
                )
            )
        }
        val order = mapOf("high" to 0, "medium" to 1, "watch" to 2)
        return candidates.sortedWith(
            compareBy<WeakChapterUi> { order[it.priority] ?: 3 }
                .thenBy { it.accuracyPercent ?: 999 }
        )
    }

    /**
     * Computes consecutive study streak from YYYY-MM-DD activity dates.
     */
    fun computeStudyStreak(activeDayYmds: Collection<String>, referenceEpochMs: Long = System.currentTimeMillis()): Pair<Int, Int> {
        if (activeDayYmds.isEmpty()) return 0 to 0
        val sortedDays = activeDayYmds.map { it.take(10) }.distinct().sorted()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val dayMillis = sortedDays.mapNotNull { runCatching { fmt.parse(it)?.time }.getOrNull() }
        if (dayMillis.isEmpty()) return 0 to 0

        var longest = 1
        var run = 1
        for (i in 1 until dayMillis.size) {
            val diffDays = ((dayMillis[i] - dayMillis[i - 1]) / 86_400_000L).toInt()
            if (diffDays == 1) {
                run += 1
                if (run > longest) longest = run
            } else if (diffDays > 1) {
                run = 1
            }
        }

        val todayYmd = fmt.format(referenceEpochMs)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = referenceEpochMs }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayYmd = fmt.format(cal.time)

        val latestDay = sortedDays.last()
        val current = if (latestDay == todayYmd || latestDay == yesterdayYmd) run else 0
        return current to longest
    }

    /**
     * Ethical Ad Guardrail: strictly suppresses ads during active quizzes, mock exams, notes reading, and PDF study.
     */
    fun shouldDisplayAd(
        policy: AdPlacementPolicyUi,
        surface: String,
        isInActiveQuizOrExam: Boolean = false,
        isReadingNotesOrPdf: Boolean = false,
        bannersShownToday: Int = 0
    ): Boolean {
        if (!policy.enabled) return false
        if (policy.suppressDuringQuizAndExam && isInActiveQuizOrExam) return false
        if (policy.suppressDuringNotesReading && isReadingNotesOrPdf) return false
        if (bannersShownToday >= policy.dailyBannerCap) return false
        return when (surface) {
            "home" -> policy.bannerOnHome
            "papers" -> policy.bannerOnPapers
            else -> false
        }
    }

    fun anonymizeLeaderboardHandle(displayName: String?, guestDeviceId: String): String {
        val base = (displayName ?: "Scholar")
            .trim()
            .replace(Regex("@.*"), "")
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.take(10)
            ?.ifBlank { "Scholar" } ?: "Scholar"
        val suffix = abs(guestDeviceId.hashCode()).toString().take(4).padStart(4, '0')
        return "${base}_$suffix"
    }

    /**
     * Computes days remaining until a YYYY-MM-DD KPK BISE board exam date.
     * Defaults to null if no date is valid/set.
     */
    fun computeDaysUntilExam(examDateIso: String?, nowEpochMs: Long = System.currentTimeMillis()): Int? {
        val raw = examDateIso?.trim()?.take(10) ?: return null
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(raw)) return null
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val targetMs = runCatching { fmt.parse(raw)?.time }.getOrNull() ?: return null
        val todayStr = fmt.format(nowEpochMs)
        val todayMs = fmt.parse(todayStr)?.time ?: nowEpochMs
        val diff = ((targetMs - todayMs) / 86_400_000L).toInt()
        return max(0, diff)
    }

    /**
     * Calculates XP earned for a Focus Pomodoro session.
     * Completed 15m/25m/50m sprints earn full milestone bonus; early pauses earn partial credit.
     */
    fun computeFocusSessionXp(minutesFocused: Int, completedFullSprint: Boolean): Int {
        val clamped = minutesFocused.coerceIn(0, 180)
        if (clamped < 1) return 0
        val base = clamped * 2
        val bonus = if (completedFullSprint && clamped >= 15) 20 else 0
        return base + bonus
    }

    data class BiseWeightageSummary(
        val totalMappedMarks: Int,
        val publishedMappedMarks: Int,
        val highYieldChaptersCount: Int
    )

    fun calculateBiseWeightageSummary(chapters: List<ChapterUiModel>): BiseWeightageSummary {
        var total = 0
        var published = 0
        var highYield = 0
        for (ch in chapters) {
            val marks = max(0, ch.boardWeightageMarks)
            total += marks
            if (ch.status != com.imtihanpro.app.domain.model.ChapterProgressStatus.LOCKED) {
                published += marks
            }
            if (marks >= 10) highYield += 1
        }
        return BiseWeightageSummary(total, published, highYield)
    }

    /**
     * Deterministically selects up to [count] questions for the 5-Minute KPK Rapid Fire sprint.
     */
    fun pickRapidFireQuestions(mcqs: List<McqQuestionUi>, count: Int = 5): List<McqQuestionUi> {
        if (mcqs.isEmpty()) return emptyList()
        val pastPaperFirst = mcqs.sortedWith(
            compareByDescending<McqQuestionUi> { it.pastPaperYear != null }
                .thenBy { it.id }
        )
        return pastPaperFirst.take(count.coerceAtLeast(1))
    }
}
