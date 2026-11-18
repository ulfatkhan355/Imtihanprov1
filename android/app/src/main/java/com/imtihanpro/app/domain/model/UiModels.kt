package com.imtihanpro.app.domain.model

enum class BoardOption(val code: String, val displayName: String, val isActiveInStage1: Boolean) {
    KPK("KPK", "KPK Board (BISE)", true),
    FEDERAL("FEDERAL", "Federal Board (FBISE - Planned)", false),
    PUNJAB("PUNJAB", "Punjab Board (Planned)", false)
}

enum class ClassLevelOption(val level: Int, val displayLabel: String, val hasSeedContent: Boolean) {
    CLASS_9(9, "Class 9", true),
    CLASS_10(10, "Class 10", true),
    CLASS_11(11, "Class 11", false),
    CLASS_12(12, "Class 12", false)
}

enum class ChapterProgressStatus {
    COMPLETED,
    IN_PROGRESS,
    AVAILABLE,
    LOCKED
}

data class KpkSubBoardUi(
    val code: String,
    val name: String,
    val headquarters: String
)

data class StudyGroupOptionUi(
    val code: String,
    val label: String,
    val applicableClasses: List<Int>,
    val description: String
)

data class TopicUiModel(
    val id: String,
    val chapterId: String,
    val orderIndex: Int,
    val title: String,
    val estimatedMinutes: Int
)

data class SubjectUiModel(
    val id: String,
    val classLevel: Int,
    val boardCode: String,
    val name: String,
    val iconKey: String,
    val totalChapters: Int,
    val completedChapters: Int,
    val progressPercent: Int,
    val hasSeedContent: Boolean,
    val studyGroups: List<String> = emptyList(),
    val description: String = "",
    val textbookPdfId: String? = null,
    val pairingSchemeSummary: String? = null,
    val offlinePackSizeMb: Double = 0.6
)

data class ChapterUiModel(
    val id: String,
    val subjectId: String,
    val chapterNumber: Int,
    val title: String,
    val subtitle: String,
    val versionLabel: String,
    val status: ChapterProgressStatus,
    val mcqCount: Int,
    val solvedCount: Int,
    val estimatedSizeKb: Int,
    val isDownloadedOffline: Boolean,
    val topics: List<TopicUiModel> = emptyList(),
    val difficultyTag: String? = null,
    val sourceReference: String? = null,
    val titleUrdu: String? = null,
    val subtitleUrdu: String? = null,
    val boardWeightageMarks: Int = 0,
    val pairingHint: String? = null,
    val nightBeforeSummary: String? = null
)

sealed interface ContentBlockUi {
    val id: String

    data class Heading(override val id: String, val sectionCode: String?, val text: String, val isSubheading: Boolean) : ContentBlockUi
    data class Paragraph(override val id: String, val text: String) : ContentBlockUi
    data class MathExpression(override val id: String, val latex: String, val plainNotation: String, val caption: String?) : ContentBlockUi
    data class Table(override val id: String, val title: String?, val headers: List<String>, val rows: List<List<String>>) : ContentBlockUi
    data class BulletOrNumberedList(override val id: String, val ordered: Boolean, val title: String?, val items: List<String>) : ContentBlockUi
    data class ImportantDefinition(override val id: String, val term: String, val definition: String, val formulaHint: String?) : ContentBlockUi
    data class WorkedExample(override val id: String, val title: String, val problemStatement: String, val steps: List<String>, val finalAnswer: String) : ContentBlockUi
    data class Diagram(override val id: String, val title: String, val caption: String, val assetKey: String, val altText: String) : ContentBlockUi
    data class HighlightedExamPoint(override val id: String, val title: String, val points: List<String>, val boardFrequency: String?) : ContentBlockUi
    data class InlineMcq(
        override val id: String,
        val question: String,
        val options: List<McqOptionUi>,
        val correctOptionId: String,
        val explanation: String
    ) : ContentBlockUi
    data class SolvedQuestion(
        override val id: String,
        val questionType: String,
        val marks: Int,
        val question: String,
        val answerSteps: List<String>,
        val yearReference: String?
    ) : ContentBlockUi
}

data class McqOptionUi(
    val id: String,
    val text: String
)

data class McqQuestionUi(
    val id: String,
    val chapterId: String,
    val question: String,
    val options: List<McqOptionUi>,
    val correctOptionId: String,
    val explanation: String,
    val subjectId: String = "",
    val difficulty: String = "medium",
    val topicTitle: String = "Core Concepts",
    val pastPaperYear: Int? = null,
    val questionUrdu: String? = null,
    val optionsUrdu: List<String> = emptyList(),
    val explanationUrdu: String? = null
)

data class TopicScoreBreakdownUi(
    val topicTitle: String,
    val correct: Int,
    val total: Int,
    val accuracyPercent: Int
)

data class QuizResultUi(
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val percentage: Int,
    val elapsedTimeFormatted: String,
    val reviewItems: List<QuizReviewItemUi>,
    val unansweredCount: Int = 0,
    val marksObtained: Double = correctCount.toDouble(),
    val totalMarks: Double = totalQuestions.toDouble(),
    val passed: Boolean = true,
    val passPercentage: Int = 40,
    val xpEarned: Int = correctCount * 10,
    val topicBreakdown: List<TopicScoreBreakdownUi> = emptyList(),
    val examMode: String = "chapter_test"
)

data class QuizReviewItemUi(
    val question: String,
    val selectedOptionId: String?,
    val correctOptionId: String,
    val explanation: String,
    val isCorrect: Boolean,
    val mcqId: String = "",
    val topicTitle: String = ""
)

data class ExamPatternUi(
    val id: String,
    val title: String,
    val mode: String,
    val classLevel: Int,
    val boardCode: String,
    val subjectId: String?,
    val questionCount: Int,
    val durationMinutes: Int,
    val marksPerMcq: Double,
    val negativeMarkingPenalty: Double,
    val passPercentage: Int,
    val includesShortQuestionsCount: Int,
    val includesLongQuestionsCount: Int,
    val sectionSummary: String
)

data class SubjectivePromptUi(
    val id: String,
    val questionType: String,
    val marks: Int,
    val question: String,
    val answerSteps: List<String>,
    val yearReference: String?
)

data class QuizHistoryItemUi(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val chapterId: String?,
    val chapterTitle: String?,
    val examMode: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val percentage: Int,
    val marksObtained: Double,
    val totalMarks: Double,
    val passed: Boolean,
    val elapsedSeconds: Int,
    val submittedAtIso: String
)

data class PastPaperUi(
    val id: String,
    val subjectName: String,
    val year: Int,
    val isSolved: Boolean,
    val fileSizeMb: Double,
    val fileSizeLabel: String,
    val pageCount: Int,
    val isDownloaded: Boolean,
    val pdfMetadataId: String = "",
    val category: String = "past_paper"
)

data class PdfResourceUi(
    val id: String,
    val title: String,
    val category: String,
    val classLevel: Int,
    val boardCode: String,
    val subjectId: String,
    val sizeMb: Double,
    val pageCount: Int,
    val version: String,
    val isDownloaded: Boolean
)

data class PdfBookmarkUi(
    val id: String,
    val pdfId: String,
    val pageNumber: Int,
    val label: String
)

data class PdfAnnotationUi(
    val id: String,
    val pdfId: String,
    val pageNumber: Int,
    val noteText: String,
    val createdAtEpochMs: Long
)

data class PdfReadingStateUi(
    val pdfId: String,
    val lastPage: Int = 1,
    val totalPages: Int = 1,
    val zoomPercent: Int = 100
)

data class DownloadedFileUi(
    val id: String,
    val title: String,
    val categoryLabel: String,
    val sizeMb: Double,
    val statusLabel: String
)

data class StudentProfileUi(
    val displayName: String,
    val isGuest: Boolean,
    val selectedClass: Int,
    val selectedBoard: String,
    val studyHours: Int,
    val chaptersCompleted: Int,
    val overallProgressPercent: Int,
    val dailyGoalPercent: Int,
    val selectedSubBoard: String = "PESHAWAR",
    val studyGroup: String = "SCIENCE_BIOLOGY",
    val xpTotal: Int = 0,
    val currentStreakDays: Int = 0,
    val dailyTargetMinutes: Int = 45,
    val examCountdownDateIso: String? = null
)

data class SearchResultsUi(
    val query: String = "",
    val subjects: List<SubjectUiModel> = emptyList(),
    val chapters: List<ChapterUiModel> = emptyList(),
    val topics: List<Pair<String, TopicUiModel>> = emptyList(),
    val mcqs: List<McqQuestionUi> = emptyList(),
    val pdfs: List<PdfResourceUi> = emptyList()
)

data class StudentBookmarkUi(
    val id: String,
    val itemType: String,
    val itemId: String,
    val chapterId: String?,
    val subjectId: String?,
    val title: String,
    val subtitle: String,
    val createdAtEpochMs: Long
)

data class MistakeRecordUi(
    val mcqId: String,
    val subjectId: String,
    val chapterId: String,
    val topicTitle: String,
    val question: String,
    val options: List<McqOptionUi>,
    val lastWrongOptionId: String?,
    val correctOptionId: String,
    val explanation: String,
    val wrongCount: Int,
    val mastered: Boolean
)

data class FlashcardUi(
    val id: String,
    val subjectId: String,
    val chapterId: String,
    val front: String,
    val back: String,
    val formulaHint: String?,
    val topicTitle: String,
    val intervalDays: Int,
    val easeFactor: Double,
    val repetitions: Int,
    val isDue: Boolean,
    val frontUrdu: String? = null,
    val backUrdu: String? = null
)

data class HonestReadinessUi(
    val readinessPercent: Int?,
    val status: String,
    val message: String
)

data class WeakChapterUi(
    val chapterId: String,
    val chapterTitle: String,
    val subjectId: String,
    val subjectName: String,
    val accuracyPercent: Int?,
    val attemptsCount: Int,
    val completionPercent: Int,
    val reason: String,
    val priority: String
)

data class SubjectAnalyticsUi(
    val subjectId: String,
    val subjectName: String,
    val masteryPercent: Int?,
    val quizAveragePercent: Int?,
    val chaptersCompleted: Int,
    val totalChapters: Int,
    val attemptsCount: Int
)

data class RevisionRecommendationUi(
    val id: String,
    val chapterId: String,
    val subjectId: String,
    val title: String,
    val reason: String,
    val actionType: String
)

data class SmartInsightsUi(
    val readiness: HonestReadinessUi,
    val diagnosticCompleted: Boolean,
    val overallAccuracyPercent: Int?,
    val totalQuizAttempts: Int,
    val masteredChaptersCount: Int,
    val weakChapters: List<WeakChapterUi>,
    val subjectAnalytics: List<SubjectAnalyticsUi>,
    val recommendations: List<RevisionRecommendationUi>,
    val dueFlashcardsCount: Int,
    val activeMistakesCount: Int
)

data class TutorGroundedSourceUi(
    val type: String,
    val title: String,
    val referenceId: String
)

data class TutorResponseUi(
    val available: Boolean,
    val isLoading: Boolean = false,
    val answer: String? = null,
    val model: String? = null,
    val groundedSources: List<TutorGroundedSourceUi> = emptyList(),
    val unavailableReason: String? = null
)

data class StudyPlanTaskUi(
    val id: String,
    val title: String,
    val subjectId: String?,
    val chapterId: String?,
    val plannedDateIso: String,
    val estimatedMinutes: Int,
    val taskType: String,
    val completed: Boolean
)

data class StudyPlannerUi(
    val dailyTargetMinutes: Int = 45,
    val examCountdownDateIso: String? = null,
    val remindersEnabled: Boolean = true,
    val reminderHourLocal: Int = 19,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val minutesLoggedToday: Int = 0,
    val tasks: List<StudyPlanTaskUi> = emptyList(),
    val recentDailyMinutes: List<Pair<String, Int>> = emptyList()
)

data class AchievementUi(
    val code: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val unlocked: Boolean
)

data class ChallengeUi(
    val id: String,
    val title: String,
    val description: String,
    val cadence: String,
    val targetCount: Int,
    val currentCount: Int,
    val xpReward: Int,
    val completed: Boolean
)

data class LeaderboardEntryUi(
    val rank: Int,
    val aliasHandle: String,
    val boardCode: String,
    val classLevel: Int,
    val xpTotal: Int,
    val streakDays: Int,
    val isCurrentUser: Boolean
)

data class MotivationSummaryUi(
    val xpTotal: Int = 0,
    val level: Int = 1,
    val nextLevelXp: Int = 150,
    val currentStreakDays: Int = 0,
    val leaderboardOptIn: Boolean = false,
    val aliasHandle: String? = null,
    val achievements: List<AchievementUi> = emptyList(),
    val challenges: List<ChallengeUi> = emptyList(),
    val leaderboard: List<LeaderboardEntryUi> = emptyList()
)

data class AdPlacementPolicyUi(
    val enabled: Boolean = false,
    val bannerOnHome: Boolean = false,
    val bannerOnPapers: Boolean = false,
    val suppressDuringQuizAndExam: Boolean = true,
    val suppressDuringNotesReading: Boolean = true,
    val dailyBannerCap: Int = 2,
    val providerNote: String = ""
)

sealed interface UiResourceState<out T> {
    data object Loading : UiResourceState<Nothing>
    data class Success<T>(val data: T, val isOfflineFallback: Boolean = false) : UiResourceState<T>
    data class Empty(val title: String, val description: String) : UiResourceState<Nothing>
    data class Error(val message: String) : UiResourceState<Nothing>
}
