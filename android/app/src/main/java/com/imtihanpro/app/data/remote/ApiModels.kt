package com.imtihanpro.app.data.remote

/**
 * Stage 2-8 Cloudflare REST API DTOs.
 * Strictly separated from Jetpack Compose UI models (domain/model/UiModels.kt).
 */
data class ApiBoardDto(
    val id: String,
    val code: String,
    val name: String,
    val isPrimaryInitialFocus: Boolean,
    val isActive: Boolean
)

data class ApiKpkSubBoardDto(
    val code: String,
    val name: String,
    val headquarters: String
)

data class ApiStudyGroupDto(
    val code: String,
    val label: String,
    val applicableClasses: List<Int>,
    val description: String
)

data class ApiTopicDto(
    val id: String,
    val chapterId: String,
    val orderIndex: Int,
    val title: String,
    val estimatedMinutes: Int
)

data class ApiSubjectDto(
    val id: String,
    val classLevel: Int,
    val boardCode: String,
    val code: String,
    val name: String,
    val iconKey: String,
    val totalChapters: Int,
    val publishedChapters: Int,
    val coverageStatus: String,
    val studyGroups: List<String> = emptyList(),
    val description: String = "",
    val textbookPdfId: String? = null,
    val pairingSchemeSummary: String? = null,
    val offlinePackSizeMb: Double = 0.6
)

data class ApiChapterDto(
    val id: String,
    val subjectId: String,
    val chapterNumber: Int,
    val title: String,
    val subtitle: String,
    val currentVersion: String,
    val publishStatus: String,
    val mcqCount: Int,
    val solvedQuestionsCount: Int,
    val isLockedByDefault: Boolean,
    val estimatedSizeKb: Int,
    val topics: List<ApiTopicDto> = emptyList(),
    val difficultyTag: String? = null,
    val sourceReference: String? = null,
    val titleUrdu: String? = null,
    val subtitleUrdu: String? = null,
    val boardWeightageMarks: Int = 0,
    val pairingHint: String? = null,
    val nightBeforeSummary: String? = null
)

sealed interface ApiContentBlockDto {
    val id: String
    val orderIndex: Int

    data class HeadingDto(
        override val id: String,
        override val orderIndex: Int,
        val sectionCode: String?,
        val text: String,
        val isSubheading: Boolean = false
    ) : ApiContentBlockDto

    data class ParagraphDto(
        override val id: String,
        override val orderIndex: Int,
        val text: String
    ) : ApiContentBlockDto

    data class MathExpressionDto(
        override val id: String,
        override val orderIndex: Int,
        val latex: String,
        val plainNotation: String,
        val caption: String?
    ) : ApiContentBlockDto

    data class TableDto(
        override val id: String,
        override val orderIndex: Int,
        val title: String?,
        val headers: List<String>,
        val rows: List<List<String>>
    ) : ApiContentBlockDto

    data class ListDto(
        override val id: String,
        override val orderIndex: Int,
        val ordered: Boolean,
        val title: String?,
        val items: List<String>
    ) : ApiContentBlockDto

    data class DefinitionDto(
        override val id: String,
        override val orderIndex: Int,
        val term: String,
        val definition: String,
        val formulaHint: String?
    ) : ApiContentBlockDto

    data class ExampleDto(
        override val id: String,
        override val orderIndex: Int,
        val title: String,
        val problemStatement: String,
        val steps: List<String>,
        val finalAnswer: String
    ) : ApiContentBlockDto

    data class DiagramDto(
        override val id: String,
        override val orderIndex: Int,
        val title: String,
        val caption: String,
        val assetKey: String,
        val altText: String
    ) : ApiContentBlockDto

    data class ExamPointDto(
        override val id: String,
        override val orderIndex: Int,
        val title: String,
        val points: List<String>,
        val boardFrequency: String?
    ) : ApiContentBlockDto

    data class McqDto(
        override val id: String,
        override val orderIndex: Int,
        val question: String,
        val options: List<Pair<String, String>>,
        val correctOptionId: String,
        val explanation: String,
        val difficulty: String
    ) : ApiContentBlockDto

    data class SolvedQuestionDto(
        override val id: String,
        override val orderIndex: Int,
        val questionType: String,
        val marks: Int,
        val question: String,
        val answerSteps: List<String>,
        val yearReference: String?
    ) : ApiContentBlockDto
}

data class ApiChapterContentResponseDto(
    val id: String,
    val chapterId: String,
    val mode: String,
    val version: String,
    val publishStatus: String,
    val blocks: List<ApiContentBlockDto>,
    val updatedAtIso: String
)

data class ApiPastPaperDto(
    val id: String,
    val boardCode: String,
    val classLevel: Int,
    val subjectId: String,
    val subjectName: String,
    val year: Int,
    val isSolved: Boolean,
    val pdfMetadataId: String,
    val fileSizeMb: Double,
    val pageCount: Int
)

data class ApiPdfMetadataDto(
    val id: String,
    val title: String,
    val category: String,
    val classLevel: Int,
    val boardCode: String,
    val subjectId: String,
    val objectKey: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val checksumSha256: String,
    val offlineCacheable: Boolean,
    val version: String
)

data class ApiMcqQuestionDto(
    val id: String,
    val chapterId: String,
    val subjectId: String,
    val question: String,
    val options: List<Pair<String, String>>,
    val correctOptionId: String? = null,
    val explanation: String? = null,
    val difficulty: String = "medium",
    val topicTitle: String = "Core Concepts",
    val pastPaperYear: Int? = null,
    val questionUrdu: String? = null,
    val optionsUrdu: List<String> = emptyList(),
    val explanationUrdu: String? = null
)

data class ApiAuthSessionDto(
    val mode: String,
    val guestDeviceId: String,
    val token: String?,
    val userId: String?,
    val displayName: String?,
    val email: String?
)

data class ApiChapterProgressSyncDto(
    val chapterId: String,
    val subjectId: String,
    val completionPercent: Int,
    val lastBlockIndex: Int,
    val isCompleted: Boolean,
    val lastAccessedAtIso: String
)

data class ApiStudentProgressDto(
    val studentId: String,
    val selectedClass: Int,
    val selectedBoard: String,
    val studyHoursTotal: Double,
    val dailyGoalPercent: Int,
    val chapterProgress: List<ApiChapterProgressSyncDto>,
    val selectedSubBoard: String? = null,
    val studyGroup: String? = null,
    val language: String = "en",
    val dailyTargetMinutes: Int = 45,
    val examCountdownDateIso: String? = null,
    val diagnosticCompleted: Boolean = false,
    val xpTotal: Int = 0,
    val currentStreakDays: Int = 0
) {
    val localProfileId: String get() = studentId
}

data class ApiQuizQuestionFeedbackDto(
    val mcqId: String,
    val selectedOptionId: String? = null,
    val isCorrect: Boolean,
    val correctOptionId: String? = null,
    val explanation: String = ""
)

data class ApiQuizSubmissionResultDto(
    val submissionId: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val percentage: Int,
    val elapsedSeconds: Int,
    val marksObtained: Double = correctCount.toDouble(),
    val totalMarks: Double = totalQuestions.toDouble(),
    val passed: Boolean = true,
    val passPercentage: Int = 40,
    val xpEarned: Int = correctCount * 10,
    val perQuestionFeedback: List<ApiQuizQuestionFeedbackDto> = emptyList()
)

data class ApiExamPatternDto(
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

data class ApiSubjectivePromptDto(
    val id: String,
    val questionType: String,
    val marks: Int,
    val question: String,
    val answerSteps: List<String>,
    val yearReference: String?
)

data class ApiExamSessionDto(
    val sessionId: String,
    val mode: String,
    val title: String,
    val subjectId: String,
    val chapterId: String?,
    val durationMinutes: Int,
    val marksPerMcq: Double,
    val negativeMarkingPenalty: Double,
    val passPercentage: Int,
    val mcqs: List<ApiMcqQuestionDto>,
    val subjectivePrompts: List<ApiSubjectivePromptDto>
)

data class ApiQuizHistoryItemDto(
    val id: String,
    val sessionId: String,
    val subjectId: String,
    val subjectName: String,
    val chapterId: String?,
    val chapterTitle: String?,
    val examMode: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val percentage: Int,
    val marksObtained: Double,
    val totalMarks: Double,
    val passed: Boolean,
    val elapsedSeconds: Int,
    val submittedAtIso: String
)

data class ApiSearchTopicMatchDto(
    val id: String,
    val chapterId: String,
    val title: String,
    val subjectId: String,
    val chapterTitle: String
)

data class ApiSearchResultsDto(
    val query: String,
    val subjects: List<ApiSubjectDto>,
    val chapters: List<ApiChapterDto>,
    val topics: List<ApiSearchTopicMatchDto>,
    val mcqs: List<ApiMcqQuestionDto>,
    val pdfs: List<ApiPdfMetadataDto>
)

data class ApiStudentBookmarkDto(
    val id: String,
    val itemType: String,
    val itemId: String,
    val chapterId: String?,
    val subjectId: String?,
    val title: String,
    val subtitle: String,
    val createdAtIso: String
)

data class ApiMistakeRecordDto(
    val id: String,
    val mcqId: String,
    val subjectId: String,
    val chapterId: String,
    val topicTitle: String,
    val question: String,
    val lastWrongOptionId: String?,
    val correctOptionId: String,
    val wrongCount: Int,
    val mastered: Boolean,
    val lastAttemptedAtIso: String,
    val mcq: ApiMcqQuestionDto?
)

data class ApiFlashcardDto(
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
    val nextReviewAtIso: String,
    val lastReviewedAtIso: String?,
    val frontUrdu: String? = null,
    val backUrdu: String? = null
)

data class ApiTutorGroundedSourceDto(
    val type: String,
    val title: String,
    val referenceId: String
)

data class ApiTutorResponseDto(
    val available: Boolean,
    val provider: String,
    val model: String?,
    val answer: String?,
    val groundedSources: List<ApiTutorGroundedSourceDto>,
    val unavailableReason: String?,
    val generatedAtIso: String
)

data class ApiStudyPlanTaskDto(
    val id: String,
    val title: String,
    val subjectId: String?,
    val chapterId: String?,
    val plannedDateIso: String,
    val estimatedMinutes: Int,
    val taskType: String,
    val completed: Boolean,
    val completedAtIso: String?
)

data class ApiStudyPlannerStateDto(
    val dailyTargetMinutes: Int,
    val examCountdownDateIso: String?,
    val remindersEnabled: Boolean,
    val reminderHourLocal: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val minutesLoggedToday: Int,
    val tasks: List<ApiStudyPlanTaskDto>
)

data class ApiAchievementDto(
    val id: String,
    val code: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val unlocked: Boolean,
    val unlockedAtIso: String?
)

data class ApiChallengeDto(
    val id: String,
    val title: String,
    val description: String,
    val cadence: String,
    val targetCount: Int,
    val currentCount: Int,
    val xpReward: Int,
    val completed: Boolean,
    val expiresAtIso: String
)

data class ApiLeaderboardEntryDto(
    val rank: Int,
    val aliasHandle: String,
    val boardCode: String,
    val classLevel: Int,
    val xpTotal: Int,
    val streakDays: Int,
    val isCurrentUser: Boolean
)

data class ApiMotivationSummaryDto(
    val xpTotal: Int,
    val level: Int,
    val nextLevelXp: Int,
    val currentStreakDays: Int,
    val leaderboardOptIn: Boolean,
    val aliasHandle: String?,
    val achievements: List<ApiAchievementDto>,
    val challenges: List<ApiChallengeDto>,
    val leaderboard: List<ApiLeaderboardEntryDto>
)

data class ApiAdPlacementConfigDto(
    val enabled: Boolean = false,
    val bannerOnHome: Boolean = false,
    val bannerOnPapers: Boolean = false,
    val suppressDuringQuizAndExam: Boolean = true,
    val suppressDuringNotesReading: Boolean = true,
    val dailyBannerCap: Int = 2,
    val providerNote: String = ""
)

data class ApiPublicRuntimeConfigDto(
    val defaultBoardCode: String = "KPK",
    val defaultClassLevel: Int = 11,
    val availableClassLevels: List<Int> = listOf(11),
    val defaultStudyGroup: String = "PRE_MEDICAL",
    val kpkSubBoards: List<ApiKpkSubBoardDto> = emptyList(),
    val studyGroups: List<ApiStudyGroupDto> = emptyList(),
    val adPlacement: ApiAdPlacementConfigDto = ApiAdPlacementConfigDto(),
    val aiTutorConfigured: Boolean = false
)
