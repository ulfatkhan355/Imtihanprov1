package com.imtihanpro.app.data.repository

import android.content.Context
import com.imtihanpro.app.data.local.datastore.UserPreferencesDataStore
import com.imtihanpro.app.data.local.room.BookmarkEntity
import com.imtihanpro.app.data.local.room.CachedChapterEntity
import com.imtihanpro.app.data.local.room.CachedChapterMetaEntity
import com.imtihanpro.app.data.local.room.CachedMcqEntity
import com.imtihanpro.app.data.local.room.CachedPastPaperEntity
import com.imtihanpro.app.data.local.room.CachedSubjectEntity
import com.imtihanpro.app.data.local.room.DownloadTaskEntity
import com.imtihanpro.app.data.local.room.DownloadedPdfEntity
import com.imtihanpro.app.data.local.room.FlashcardReviewEntity
import com.imtihanpro.app.data.local.room.GuestProgressEntity
import com.imtihanpro.app.data.local.room.ImtihanDatabase
import com.imtihanpro.app.data.local.room.MistakeQuestionEntity
import com.imtihanpro.app.data.local.room.PdfAnnotationEntity
import com.imtihanpro.app.data.local.room.PdfBookmarkEntity
import com.imtihanpro.app.data.local.room.PdfReadingStateEntity
import com.imtihanpro.app.data.local.room.PendingFeedbackEntity
import com.imtihanpro.app.data.local.room.PendingSyncQueueEntity
import com.imtihanpro.app.data.local.room.QuizAttemptEntity
import com.imtihanpro.app.data.local.room.StudyActivityDayEntity
import com.imtihanpro.app.data.local.room.StudyPlanTaskEntity
import com.imtihanpro.app.data.mapper.CurriculumMappers
import com.imtihanpro.app.data.remote.ApiChapterProgressSyncDto
import com.imtihanpro.app.data.remote.ApiConfig
import com.imtihanpro.app.data.remote.ApiContentJsonParser
import com.imtihanpro.app.data.remote.ApiResult
import com.imtihanpro.app.data.remote.ImtihanApiClient
import com.imtihanpro.app.data.remote.ImtihanSyncRules
import com.imtihanpro.app.data.sync.PdfDownloadNotifications
import com.imtihanpro.app.data.sync.SyncScheduler
import com.imtihanpro.app.domain.engine.DownloadPausedReason
import com.imtihanpro.app.domain.engine.DownloadState
import com.imtihanpro.app.domain.engine.PdfDownloadEngine
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.AchievementUi
import com.imtihanpro.app.domain.model.AdPlacementPolicyUi
import com.imtihanpro.app.domain.model.ChallengeUi
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.ContentBlockUi
import com.imtihanpro.app.domain.model.DownloadTaskUi
import com.imtihanpro.app.domain.model.DownloadedFileUi
import com.imtihanpro.app.domain.model.ExamPatternUi
import com.imtihanpro.app.domain.model.FlashcardUi
import com.imtihanpro.app.domain.model.LeaderboardEntryUi
import com.imtihanpro.app.domain.model.McqQuestionUi
import com.imtihanpro.app.domain.model.MistakeRecordUi
import com.imtihanpro.app.domain.model.MotivationSummaryUi
import com.imtihanpro.app.domain.model.PastPaperUi
import com.imtihanpro.app.domain.model.PdfAnnotationUi
import com.imtihanpro.app.domain.model.PdfBookmarkUi
import com.imtihanpro.app.domain.model.PdfReadingStateUi
import com.imtihanpro.app.domain.model.PdfResourceUi
import com.imtihanpro.app.domain.model.QuizHistoryItemUi
import com.imtihanpro.app.domain.model.QuizResultUi
import com.imtihanpro.app.domain.model.QuizReviewItemUi
import com.imtihanpro.app.domain.model.RevisionRecommendationUi
import com.imtihanpro.app.domain.model.SearchResultsUi
import com.imtihanpro.app.domain.model.SmartInsightsUi
import com.imtihanpro.app.domain.model.StudentBookmarkUi
import com.imtihanpro.app.domain.model.StudyPlanTaskUi
import com.imtihanpro.app.domain.model.StudyPlannerUi
import com.imtihanpro.app.domain.model.SubjectAnalyticsUi
import com.imtihanpro.app.domain.model.SubjectUiModel
import com.imtihanpro.app.domain.model.SubjectivePromptUi
import com.imtihanpro.app.domain.model.TopicScoreBreakdownUi
import com.imtihanpro.app.domain.model.TutorGroundedSourceUi
import com.imtihanpro.app.domain.model.TutorResponseUi
import com.imtihanpro.app.domain.model.UiResourceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.roundToInt

data class LoadedExamSession(
    val sessionId: String,
    val title: String,
    val mode: String,
    val subjectId: String,
    val chapterId: String?,
    val durationMinutes: Int,
    val marksPerMcq: Double,
    val negativeMarkingPenalty: Double,
    val passPercentage: Int,
    val mcqs: List<McqQuestionUi>,
    val subjectivePrompts: List<SubjectivePromptUi>
)

data class RealHomeSummaryMetrics(
    val studyHours: Int = 0,
    val chaptersCompleted: Int = 0,
    val overallProgressPercent: Int = 0,
    val dailyGoalPercent: Int = 0,
    val currentStreakDays: Int = 0,
    val xpTotal: Int = 0,
    val recentDailyMinutes: List<Pair<String, Int>> = emptyList(),
    val continueChapterId: String? = null,
    val continueChapterTitle: String? = null,
    val continueChapterPercent: Int = 0
)

interface CurriculumRepository {
    fun observeSubjects(classLevel: Int, boardCode: String): Flow<UiResourceState<List<SubjectUiModel>>>
    fun observeChapters(subjectId: String): Flow<UiResourceState<List<ChapterUiModel>>>
    fun observeChapterBlocks(chapterId: String, revisionOnly: Boolean): Flow<UiResourceState<List<ContentBlockUi>>>
    fun observeSolvedQuestions(chapterId: String): Flow<UiResourceState<List<ContentBlockUi>>> =
        observeChapterBlocks(chapterId, revisionOnly = false).map { state ->
            when (state) {
                is UiResourceState.Success -> {
                    val solved = state.data.filter {
                        it is ContentBlockUi.SolvedQuestion || it is ContentBlockUi.WorkedExample
                    }
                    if (solved.isEmpty()) {
                        UiResourceState.Empty(
                            "No Solved Questions Published",
                            "Worked numericals and board questions will appear here once published."
                        )
                    } else {
                        UiResourceState.Success(solved, state.isOfflineFallback)
                    }
                }
                else -> state
            }
        }

    fun observeMcqsForChapter(chapterId: String): Flow<UiResourceState<List<McqQuestionUi>>>
    fun observePastPapers(classLevel: Int, solvedFilter: Boolean?): Flow<UiResourceState<List<PastPaperUi>>>
    fun observeDownloads(): Flow<List<DownloadedFileUi>>
    suspend fun toggleOfflineChapter(chapterId: String, title: String, sizeMb: Double)
    suspend fun toggleOfflinePaper(paperId: String, title: String, sizeMb: Double)
    suspend fun clearAllDownloads()
    fun gradeQuiz(
        questions: List<McqQuestionUi>,
        selectedAnswers: Map<String, String>,
        elapsedSeconds: Int
    ): QuizResultUi

    // Stage 2 sync, auth, and PDF reader
    suspend fun syncWithCloud(): Result<String> = Result.success("Local mode")
    suspend fun initGuestOrGoogleSession(googleIdToken: String? = null): Result<String> = Result.success("Guest local")
    suspend fun submitQuizResult(
        subjectId: String,
        chapterId: String,
        questions: List<McqQuestionUi>,
        selectedAnswers: Map<String, String>,
        elapsedSeconds: Int
    ): QuizResultUi = gradeQuiz(questions, selectedAnswers, elapsedSeconds)

    suspend fun submitExamResult(
        subjectId: String,
        subjectName: String,
        chapterId: String?,
        chapterTitle: String?,
        examMode: String,
        questions: List<McqQuestionUi>,
        selectedAnswers: Map<String, String>,
        elapsedSeconds: Int,
        marksPerMcq: Double = 1.0,
        negativeMarkingPenalty: Double = 0.0,
        passPercentage: Int = 40,
        sessionId: String? = null
    ): QuizResultUi = Stage3To8Engines.gradeQuizWithRules(
        questions = questions,
        selectedAnswers = selectedAnswers,
        elapsedSeconds = elapsedSeconds,
        marksPerMcq = marksPerMcq,
        negativeMarkingPenalty = negativeMarkingPenalty,
        passPercentage = passPercentage
    ).copy(examMode = examMode)

    suspend fun recordChapterProgress(
        chapterId: String,
        subjectId: String,
        completionPercent: Int,
        lastBlockIndex: Int
    ) {}

    suspend fun getOrDownloadPdfFile(
        paperId: String,
        pdfMetadataId: String,
        title: String,
        sizeMb: Double
    ): Result<String> = Result.failure(IllegalStateException("PDF streaming requires runtime context."))

    suspend fun findLocalPdfPath(paperId: String, pdfMetadataId: String): String? = null
    fun observeDownloadTasks(): Flow<List<DownloadTaskUi>> = flowOf(emptyList())
    suspend fun enqueuePdfDownload(
        paperId: String,
        pdfId: String,
        title: String,
        categoryLabel: String = "PDF Document",
        sizeMb: Double = 0.0,
        expectedSha256: String? = null,
        allowMobileData: Boolean = false
    ) {}
    suspend fun pausePdfDownload(pdfId: String) {}
    suspend fun resumePdfDownload(pdfId: String) {}
    suspend fun cancelPdfDownload(pdfId: String) {}
    suspend fun retryPdfDownload(pdfId: String) {}
    suspend fun retryAllFailedDownloads() {}
    suspend fun downloadOnMobileData(pdfId: String) {}
    suspend fun recoverDownloadQueueOnStartup() {}
    fun observePdfBookmarks(pdfId: String): Flow<List<PdfBookmarkUi>> = flowOf(emptyList())
    suspend fun togglePdfBookmark(pdfId: String, pageNumber: Int, label: String) {}
    fun observePdfAnnotations(pdfId: String): Flow<List<PdfAnnotationUi>> = flowOf(emptyList())
    suspend fun addPdfAnnotation(pdfId: String, pageNumber: Int, noteText: String) {}
    suspend fun deletePdfAnnotation(annotationId: String) {}
    suspend fun getPdfReadingState(pdfId: String): PdfReadingStateUi = PdfReadingStateUi(pdfId)
    suspend fun savePdfReadingState(pdfId: String, lastPage: Int, totalPages: Int, zoomPercent: Int) {}
    fun observePendingSyncCount(): Flow<Int> = flowOf(0)

    // Stage 3-8 capabilities
    fun observeHomeMetrics(classLevel: Int, boardCode: String): Flow<RealHomeSummaryMetrics> =
        flowOf(RealHomeSummaryMetrics())

    suspend fun searchCurriculum(query: String, classLevel: Int, boardCode: String): SearchResultsUi = SearchResultsUi(query)
    suspend fun loadExamPatterns(classLevel: Int, boardCode: String, subjectId: String? = null): List<ExamPatternUi> =
        MockSeedDataSource.examPatterns.filter { it.classLevel == classLevel && it.boardCode == boardCode }
            .map { CurriculumMappers.toExamPatternUi(it) }

    suspend fun startExamSession(
        mode: String,
        boardCode: String,
        classLevel: Int,
        patternId: String? = null,
        subjectId: String? = null,
        chapterId: String? = null,
        topicTitle: String? = null,
        difficulty: String = "mixed",
        questionCount: Int = 10
    ): Result<LoadedExamSession> = Result.failure(IllegalStateException("Offline test bank used."))

    fun observeQuizHistory(): Flow<List<QuizHistoryItemUi>> = flowOf(emptyList())
    suspend fun refreshQuizHistory() {}

    fun observeStudentBookmarks(): Flow<List<StudentBookmarkUi>> = flowOf(emptyList())
    suspend fun toggleStudentBookmark(
        itemType: String,
        itemId: String,
        chapterId: String?,
        subjectId: String?,
        title: String,
        subtitle: String
    ) {}

    fun observeMistakes(): Flow<List<MistakeRecordUi>> = flowOf(emptyList())
    suspend fun resolveMistake(mcqId: String) {}

    fun observeFlashcards(): Flow<List<FlashcardUi>> = flowOf(emptyList())
    suspend fun reviewFlashcard(flashcardId: String, quality: Int) {}

    fun observeSmartInsights(classLevel: Int, boardCode: String): Flow<SmartInsightsUi> =
        flowOf(
            SmartInsightsUi(
                readiness = Stage3To8Engines.computeHonestExamReadiness(0, 0, null, 0, 0, 0),
                diagnosticCompleted = false,
                overallAccuracyPercent = null,
                totalQuizAttempts = 0,
                masteredChaptersCount = 0,
                weakChapters = emptyList(),
                subjectAnalytics = emptyList(),
                recommendations = emptyList(),
                dueFlashcardsCount = 0,
                activeMistakesCount = 0
            )
        )

    suspend fun askAiTutor(
        chapterId: String?,
        subjectId: String?,
        classLevel: Int,
        boardCode: String,
        languageCode: String,
        action: String,
        prompt: String
    ): TutorResponseUi = TutorResponseUi(
        available = false,
        unavailableReason = "AI Tutor requires connection to Cloudflare Worker with GEMINI_API_KEY."
    )

    fun observeStudyPlanner(): Flow<StudyPlannerUi> = flowOf(StudyPlannerUi())
    suspend fun addStudyPlanTask(
        title: String,
        subjectId: String?,
        chapterId: String?,
        plannedDateIso: String,
        estimatedMinutes: Int,
        taskType: String
    ) {}

    suspend fun toggleStudyPlanTask(taskId: String, completed: Boolean) {}
    suspend fun deleteStudyPlanTask(taskId: String) {}
    suspend fun updateStudyGoalPreferences(dailyTargetMinutes: Int, examCountdownDateIso: String?, reminderHourLocal: Int) {}

    fun observeMotivationSummary(classLevel: Int, boardCode: String): Flow<MotivationSummaryUi> =
        flowOf(MotivationSummaryUi())

    suspend fun setLeaderboardOptIn(optIn: Boolean, aliasHandle: String?) {}
    suspend fun submitFeedback(
        category: String,
        subjectId: String?,
        chapterId: String?,
        mcqId: String?,
        title: String,
        description: String
    ): Result<String> = Result.success("Saved locally.")

    fun observePdfLibrary(classLevel: Int, category: String?): Flow<List<PdfResourceUi>> = flowOf(emptyList())
    suspend fun fetchPublicAdPolicy(): Pair<AdPlacementPolicyUi, Boolean> = AdPlacementPolicyUi() to false

    // Flagship 2050 additions: Full-Subject Offline Pack & Focus Pomodoro Sprint XP
    suspend fun downloadSubjectOfflinePack(subjectId: String): Result<Int> = Result.success(0)
    suspend fun logFocusPomodoroSession(minutes: Int, completed: Boolean): Int =
        Stage3To8Engines.computeFocusSessionXp(minutes, completed)
}

/**
 * Stage 1 offline-first mock repository preserved for deterministic unit tests and seed fallback.
 */
class Stage1MockCurriculumRepository : CurriculumRepository {

    private val completedChapterIds = MutableStateFlow(setOf("ch_phy10_01"))
    private val inProgressChapterIds = MutableStateFlow(setOf("ch_phy10_02"))
    private val downloadedIds = MutableStateFlow(setOf("ch_phy10_01", "pp_math_2019", "ch_chem10_01"))

    private val downloadItems = MutableStateFlow(
        listOf(
            DownloadedFileUi("ch_phy10_01", "Physics Notes · Ch 1", "Chapter Pack", 2.4, "Ready"),
            DownloadedFileUi("pp_math_2019", "Maths Past Paper 2019", "Past Paper PDF", 1.8, "Ready"),
            DownloadedFileUi("ch_chem10_01", "Chemistry Notes · Ch 1", "Chapter Pack", 2.1, "Ready")
        )
    )

    override fun observeSubjects(classLevel: Int, boardCode: String): Flow<UiResourceState<List<SubjectUiModel>>> {
        return completedChapterIds.map { completed ->
            val matching = MockSeedDataSource.subjects.filter {
                it.classLevel == classLevel && it.boardCode == boardCode
            }
            if (matching.isEmpty()) {
                UiResourceState.Empty(
                    title = "Curriculum Structure Ready",
                    description = "Subjects for $boardCode Class $classLevel will sync once backend is connected."
                )
            } else {
                val mapped = matching.map { sub ->
                    val doneCount = MockSeedDataSource.chapters.count {
                        it.subjectId == sub.id && it.id in completed
                    }
                    CurriculumMappers.toSubjectUi(sub, doneCount)
                }
                UiResourceState.Success(mapped)
            }
        }
    }

    override fun observeChapters(subjectId: String): Flow<UiResourceState<List<ChapterUiModel>>> {
        return downloadedIds.map { downloaded ->
            val matching = MockSeedDataSource.chapters.filter { it.subjectId == subjectId }
            if (matching.isEmpty()) {
                UiResourceState.Empty(
                    title = "No Chapters Seeded Yet",
                    description = "This subject structure is prepared for Cloudflare content publishing."
                )
            } else {
                UiResourceState.Success(
                    matching.map {
                        CurriculumMappers.toChapterUi(
                            dto = it,
                            completedChapterIds = completedChapterIds.value,
                            inProgressChapterIds = inProgressChapterIds.value,
                            downloadedChapterIds = downloaded
                        )
                    }
                )
            }
        }
    }

    override fun observeChapterBlocks(
        chapterId: String,
        revisionOnly: Boolean
    ): Flow<UiResourceState<List<ContentBlockUi>>> {
        val raw = MockSeedDataSource.detailedNotesBlocks[chapterId]
            ?: MockSeedDataSource.detailedNotesBlocks["ch_phy9_01"].orEmpty()
        val mapped = raw.map { CurriculumMappers.toContentBlockUi(it) }
        val filtered = if (revisionOnly) {
            mapped.filter {
                it is ContentBlockUi.Heading ||
                    it is ContentBlockUi.ImportantDefinition ||
                    it is ContentBlockUi.MathExpression ||
                    it is ContentBlockUi.HighlightedExamPoint
            }
        } else {
            mapped
        }
        return MutableStateFlow(UiResourceState.Success(filtered))
    }

    override fun observeMcqsForChapter(chapterId: String): Flow<UiResourceState<List<McqQuestionUi>>> {
        val list = MockSeedDataSource.mcqs
            .filter { it.chapterId == chapterId }
            .map { CurriculumMappers.toMcqUi(it) }
        return if (list.isEmpty()) {
            MutableStateFlow(
                UiResourceState.Empty(
                    title = "No Practice MCQs Yet",
                    description = "MCQs for this chapter will be delivered by the API."
                )
            )
        } else {
            MutableStateFlow(UiResourceState.Success(list))
        }
    }

    override fun observePastPapers(
        classLevel: Int,
        solvedFilter: Boolean?
    ): Flow<UiResourceState<List<PastPaperUi>>> {
        return downloadedIds.map { downloaded ->
            val filtered = MockSeedDataSource.pastPapers.filter {
                it.classLevel == classLevel && (solvedFilter == null || it.isSolved == solvedFilter)
            }
            if (filtered.isEmpty()) {
                UiResourceState.Empty(
                    title = "No Past Papers Available",
                    description = "Past papers for Class $classLevel will appear once uploaded."
                )
            } else {
                UiResourceState.Success(filtered.map { CurriculumMappers.toPastPaperUi(it, downloaded) })
            }
        }
    }

    override fun observeDownloads(): Flow<List<DownloadedFileUi>> = downloadItems

    override suspend fun toggleOfflineChapter(chapterId: String, title: String, sizeMb: Double) {
        if (chapterId in downloadedIds.value) {
            downloadedIds.value -= chapterId
            downloadItems.value = downloadItems.value.filterNot { it.id == chapterId }
        } else {
            downloadedIds.value += chapterId
            downloadItems.value = listOf(
                DownloadedFileUi(chapterId, title, "Chapter Pack", sizeMb, "Ready")
            ) + downloadItems.value
        }
    }

    override suspend fun toggleOfflinePaper(paperId: String, title: String, sizeMb: Double) {
        if (paperId in downloadedIds.value) {
            downloadedIds.value -= paperId
            downloadItems.value = downloadItems.value.filterNot { it.id == paperId }
        } else {
            downloadedIds.value += paperId
            downloadItems.value = listOf(
                DownloadedFileUi(paperId, title, "Past Paper PDF", sizeMb, "Ready")
            ) + downloadItems.value
        }
    }

    override suspend fun clearAllDownloads() {
        downloadedIds.value = emptySet()
        downloadItems.value = emptyList()
    }

    override fun gradeQuiz(
        questions: List<McqQuestionUi>,
        selectedAnswers: Map<String, String>,
        elapsedSeconds: Int
    ): QuizResultUi = computeQuizResult(questions, selectedAnswers, elapsedSeconds)
}

/**
 * Offline-first repository backed by Room + DataStore + ImtihanApiClient (Cloudflare Worker + B2).
 */
class OfflineFirstCurriculumRepository(
    private val context: Context,
    private val database: ImtihanDatabase,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val apiClient: ImtihanApiClient = ImtihanApiClient()
) : CurriculumRepository {

    private val downloadTaskDao = database.downloadTaskDao()

    private val cacheDao = database.curriculumCacheDao()
    private val offlineDao = database.offlineContentDao()
    private val progressDao = database.guestProgressDao()
    private val pdfReaderDao = database.pdfReaderDao()
    private val syncQueueDao = database.syncQueueDao()
    private val quizAttemptDao = database.quizAttemptDao()
    private val mistakeDao = database.mistakeQuestionDao()
    private val bookmarkDao = database.bookmarkDao()
    private val flashcardDao = database.flashcardReviewDao()
    private val plannerDao = database.studyPlannerDao()
    private val activityDao = database.studyActivityDao()
    private val feedbackDao = database.feedbackDao()

    private fun nowIsoUtc(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private fun todayYmdUtc(): String = nowIsoUtc().take(10)

    override fun observeSubjects(classLevel: Int, boardCode: String): Flow<UiResourceState<List<SubjectUiModel>>> = flow {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchSubjects(prefs.configuredApiBaseUrl, classLevel, boardCode, prefs.studyGroupCode)) {
                is ApiResult.Success -> {
                    val now = System.currentTimeMillis()
                    cacheDao.upsertSubjects(
                        res.data.map {
                            CachedSubjectEntity(
                                id = it.id,
                                classLevel = it.classLevel,
                                boardCode = it.boardCode,
                                code = it.code,
                                name = it.name,
                                iconKey = it.iconKey,
                                totalChapters = it.totalChapters,
                                publishedChapters = it.publishedChapters,
                                coverageStatus = it.coverageStatus,
                                updatedAtEpochMs = now,
                                studyGroupsCsv = it.studyGroups.joinToString(","),
                                pairingSchemeSummary = it.pairingSchemeSummary,
                                offlinePackSizeMb = it.offlinePackSizeMb
                            )
                        }
                    )
                }
                is ApiResult.Failure -> Unit
            }
        }

        combine(
            cacheDao.observeSubjects(classLevel, boardCode),
            progressDao.observeAllProgress()
        ) { cachedSubjects, progressList ->
            val completedIds = progressList.filter { it.isCompleted }.map { it.chapterId }.toSet()
            if (cachedSubjects.isNotEmpty()) {
                val filtered = cachedSubjects.filter { s ->
                    val groups = s.studyGroupsCsv.split(",").filter { it.isNotBlank() }
                    groups.isEmpty() || prefs.studyGroupCode in groups
                }.ifEmpty { cachedSubjects }

                val mapped = filtered.map { s ->
                    val doneCount = progressList.count { it.subjectId == s.id && it.isCompleted }
                    val seedFallback = MockSeedDataSource.subjects.firstOrNull { it.id == s.id }
                    SubjectUiModel(
                        id = s.id,
                        classLevel = s.classLevel,
                        boardCode = s.boardCode,
                        name = s.name,
                        iconKey = s.iconKey,
                        totalChapters = s.totalChapters,
                        completedChapters = doneCount,
                        progressPercent = if (s.totalChapters <= 0) 0 else ((doneCount.toFloat() / s.totalChapters.toFloat()) * 100f).toInt().coerceIn(0, 100),
                        hasSeedContent = s.coverageStatus == "active_seed",
                        studyGroups = s.studyGroupsCsv.split(",").filter { it.isNotBlank() },
                        description = seedFallback?.description ?: "",
                        textbookPdfId = seedFallback?.textbookPdfId,
                        pairingSchemeSummary = s.pairingSchemeSummary ?: seedFallback?.pairingSchemeSummary,
                        offlinePackSizeMb = s.offlinePackSizeMb
                    )
                }
                UiResourceState.Success(mapped, isOfflineFallback = prefs.offlineOnlyMode)
            } else {
                val seed = MockSeedDataSource.subjects.filter {
                    it.classLevel == classLevel && it.boardCode == boardCode &&
                        (it.studyGroups.isEmpty() || prefs.studyGroupCode in it.studyGroups)
                }.ifEmpty {
                    MockSeedDataSource.subjects.filter { it.classLevel == classLevel && it.boardCode == boardCode }
                }
                if (seed.isEmpty()) {
                    UiResourceState.Empty(
                        title = "Curriculum Structure Ready",
                        description = "Subjects for $boardCode Class $classLevel will appear once synced from Worker API."
                    )
                } else {
                    val mapped = seed.map { s ->
                        val done = MockSeedDataSource.chapters.count { it.subjectId == s.id && it.id in completedIds }
                        CurriculumMappers.toSubjectUi(s, done)
                    }
                    UiResourceState.Success(mapped, isOfflineFallback = true)
                }
            }
        }.collect { emit(it) }
    }

    override fun observeChapters(subjectId: String): Flow<UiResourceState<List<ChapterUiModel>>> = flow {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchChapters(prefs.configuredApiBaseUrl, subjectId)) {
                is ApiResult.Success -> {
                    cacheDao.upsertChapterMeta(
                        res.data.map {
                            CachedChapterMetaEntity(
                                id = it.id,
                                subjectId = it.subjectId,
                                chapterNumber = it.chapterNumber,
                                title = it.title,
                                subtitle = it.subtitle,
                                currentVersion = it.currentVersion,
                                publishStatus = it.publishStatus,
                                mcqCount = it.mcqCount,
                                solvedQuestionsCount = it.solvedQuestionsCount,
                                isLockedByDefault = it.isLockedByDefault,
                                estimatedSizeKb = it.estimatedSizeKb,
                                topicsJson = ApiContentJsonParser.serializeTopics(it.topics),
                                titleUrdu = it.titleUrdu,
                                subtitleUrdu = it.subtitleUrdu,
                                boardWeightageMarks = it.boardWeightageMarks,
                                pairingHint = it.pairingHint,
                                nightBeforeSummary = it.nightBeforeSummary
                            )
                        }
                    )
                }
                is ApiResult.Failure -> Unit
            }
        }

        combine(
            cacheDao.observeChapterMeta(subjectId),
            progressDao.observeAllProgress(),
            offlineDao.observeDownloadedChapterIds()
        ) { cachedMeta, progressList, downloadedList ->
            val completedIds = progressList.filter { it.isCompleted }.map { it.chapterId }.toSet()
            val inProgressIds = progressList.filter { !it.isCompleted && it.completionPercent > 0 }.map { it.chapterId }.toSet()
            val downloadedSet = downloadedList.toSet()

            val dtos = if (cachedMeta.isNotEmpty()) {
                cachedMeta.map {
                    val seedFallback = MockSeedDataSource.chapters.firstOrNull { s -> s.id == it.id }
                    com.imtihanpro.app.data.remote.ApiChapterDto(
                        id = it.id,
                        subjectId = it.subjectId,
                        chapterNumber = it.chapterNumber,
                        title = it.title,
                        subtitle = it.subtitle,
                        currentVersion = it.currentVersion,
                        publishStatus = it.publishStatus,
                        mcqCount = it.mcqCount,
                        solvedQuestionsCount = it.solvedQuestionsCount,
                        isLockedByDefault = it.isLockedByDefault,
                        estimatedSizeKb = it.estimatedSizeKb,
                        topics = runCatching {
                            ApiContentJsonParser.parseTopics(JSONArray(it.topicsJson))
                        }.getOrElse { emptyList() },
                        titleUrdu = it.titleUrdu ?: seedFallback?.titleUrdu,
                        subtitleUrdu = it.subtitleUrdu ?: seedFallback?.subtitleUrdu,
                        boardWeightageMarks = if (it.boardWeightageMarks > 0) it.boardWeightageMarks else (seedFallback?.boardWeightageMarks ?: 0),
                        pairingHint = it.pairingHint ?: seedFallback?.pairingHint,
                        nightBeforeSummary = it.nightBeforeSummary ?: seedFallback?.nightBeforeSummary
                    )
                }
            } else {
                MockSeedDataSource.chapters.filter { it.subjectId == subjectId }
            }

            if (dtos.isEmpty()) {
                UiResourceState.Empty(
                    title = "No Chapters Published Yet",
                    description = "Chapters for this subject will appear once published in Admin Content Studio."
                )
            } else {
                UiResourceState.Success(
                    data = dtos.map {
                        CurriculumMappers.toChapterUi(
                            dto = it,
                            completedChapterIds = completedIds,
                            inProgressChapterIds = inProgressIds,
                            downloadedChapterIds = downloadedSet
                        )
                    },
                    isOfflineFallback = cachedMeta.isEmpty() || prefs.offlineOnlyMode
                )
            }
        }.collect { emit(it) }
    }

    override fun observeChapterBlocks(
        chapterId: String,
        revisionOnly: Boolean
    ): Flow<UiResourceState<List<ContentBlockUi>>> = flow {
        val prefs = preferencesDataStore.preferencesFlow.first()
        var offlineFallbackUsed = false

        if (!prefs.offlineOnlyMode) {
            val mode = if (revisionOnly) "revision_notes" else "detailed_notes"
            when (val res = apiClient.fetchChapterContent(prefs.configuredApiBaseUrl, chapterId, mode)) {
                is ApiResult.Success -> {
                    val serialized = ApiContentJsonParser.serializeContentBlocks(res.data.blocks)
                    val cacheKey = if (revisionOnly) "${chapterId}_rev" else chapterId
                    offlineDao.upsertCachedChapter(
                        CachedChapterEntity(
                            chapterId = cacheKey,
                            subjectId = "",
                            chapterNumber = 1,
                            title = chapterId,
                            contentVersion = res.data.version,
                            serializedBlocksJson = serialized,
                            downloadedAtEpochMs = System.currentTimeMillis(),
                            sizeKb = (serialized.length / 1024).coerceAtLeast(1)
                        )
                    )
                }
                is ApiResult.Failure -> {
                    offlineFallbackUsed = true
                }
            }
        } else {
            offlineFallbackUsed = true
        }

        val cacheKey = if (revisionOnly) "${chapterId}_rev" else chapterId
        combine(
            offlineDao.observeCachedChapter(cacheKey),
            offlineDao.observeCachedChapter(chapterId)
        ) { modeCached, baseCached ->
            val cached = modeCached ?: baseCached
            val rawDtos = if (cached != null) {
                runCatching {
                    ApiContentJsonParser.parseContentBlocks(JSONArray(cached.serializedBlocksJson))
                }.getOrElse {
                    MockSeedDataSource.detailedNotesBlocks[chapterId].orEmpty()
                }
            } else {
                MockSeedDataSource.detailedNotesBlocks[chapterId].orEmpty()
            }

            val mapped = rawDtos.map { CurriculumMappers.toContentBlockUi(it) }
            val filtered = if (revisionOnly) {
                mapped.filter {
                    it is ContentBlockUi.Heading ||
                        it is ContentBlockUi.ImportantDefinition ||
                        it is ContentBlockUi.MathExpression ||
                        it is ContentBlockUi.HighlightedExamPoint
                }
            } else {
                mapped
            }

            if (filtered.isEmpty()) {
                UiResourceState.Empty(
                    title = "No Notes Published Yet",
                    description = "Publish notes for this chapter in the Admin Studio to read them here."
                )
            } else {
                UiResourceState.Success(filtered, isOfflineFallback = offlineFallbackUsed || cached == null)
            }
        }.collect { emit(it) }
    }

    override fun observeSolvedQuestions(chapterId: String): Flow<UiResourceState<List<ContentBlockUi>>> = flow {
        val prefs = preferencesDataStore.preferencesFlow.first()
        var offlineFallbackUsed = false

        if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchChapterContent(prefs.configuredApiBaseUrl, chapterId, "solved_questions")) {
                is ApiResult.Success -> {
                    val serialized = ApiContentJsonParser.serializeContentBlocks(res.data.blocks)
                    offlineDao.upsertCachedChapter(
                        CachedChapterEntity(
                            chapterId = "${chapterId}_solved",
                            subjectId = "",
                            chapterNumber = 1,
                            title = chapterId,
                            contentVersion = res.data.version,
                            serializedBlocksJson = serialized,
                            downloadedAtEpochMs = System.currentTimeMillis(),
                            sizeKb = (serialized.length / 1024).coerceAtLeast(1)
                        )
                    )
                }
                is ApiResult.Failure -> offlineFallbackUsed = true
            }
        } else {
            offlineFallbackUsed = true
        }

        combine(
            offlineDao.observeCachedChapter("${chapterId}_solved"),
            offlineDao.observeCachedChapter(chapterId)
        ) { solvedCached, baseCached ->
            val cached = solvedCached ?: baseCached
            val rawDtos = if (cached != null) {
                runCatching {
                    ApiContentJsonParser.parseContentBlocks(JSONArray(cached.serializedBlocksJson))
                }.getOrElse {
                    MockSeedDataSource.detailedNotesBlocks[chapterId].orEmpty()
                }
            } else {
                MockSeedDataSource.detailedNotesBlocks[chapterId].orEmpty()
            }

            val solved = rawDtos.map { CurriculumMappers.toContentBlockUi(it) }.filter {
                it is ContentBlockUi.SolvedQuestion || it is ContentBlockUi.WorkedExample
            }

            if (solved.isEmpty()) {
                UiResourceState.Empty(
                    title = "No Solved Questions Yet",
                    description = "Board-style solved short, long & numerical questions for this chapter will appear once published."
                )
            } else {
                UiResourceState.Success(solved, isOfflineFallback = offlineFallbackUsed || cached == null)
            }
        }.collect { emit(it) }
    }

    override fun observeMcqsForChapter(chapterId: String): Flow<UiResourceState<List<McqQuestionUi>>> = flow {
        val prefs = preferencesDataStore.preferencesFlow.first()
        var fallback = false
        if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchChapterMcqs(prefs.configuredApiBaseUrl, chapterId)) {
                is ApiResult.Success -> {
                    cacheDao.upsertMcqs(
                        res.data.map { m ->
                            CachedMcqEntity(
                                id = m.id,
                                chapterId = m.chapterId,
                                subjectId = m.subjectId,
                                question = m.question,
                                optionsJson = ApiContentJsonParser.serializeOptions(m.options),
                                correctOptionId = m.correctOptionId ?: "",
                                explanation = m.explanation ?: "",
                                difficulty = m.difficulty,
                                topicTitle = m.topicTitle,
                                questionUrdu = m.questionUrdu,
                                optionsUrduJson = JSONArray(m.optionsUrdu).toString(),
                                explanationUrdu = m.explanationUrdu
                            )
                        }
                    )
                }
                is ApiResult.Failure -> fallback = true
            }
        } else {
            fallback = true
        }

        cacheDao.observeMcqs(chapterId).collect { cachedMcqs ->
            val dtos = if (cachedMcqs.isNotEmpty()) {
                cachedMcqs.map { m ->
                    val seedFallback = MockSeedDataSource.mcqs.firstOrNull { it.id == m.id }
                    val urduOpts = ApiContentJsonParser.parseStringList(m.optionsUrduJson)
                    com.imtihanpro.app.data.remote.ApiMcqQuestionDto(
                        id = m.id,
                        chapterId = m.chapterId,
                        subjectId = m.subjectId,
                        question = m.question,
                        options = ApiContentJsonParser.parseOptions(JSONArray(m.optionsJson)),
                        correctOptionId = m.correctOptionId.ifBlank { seedFallback?.correctOptionId },
                        explanation = m.explanation.ifBlank { seedFallback?.explanation },
                        difficulty = m.difficulty,
                        topicTitle = m.topicTitle,
                        questionUrdu = m.questionUrdu ?: seedFallback?.questionUrdu,
                        optionsUrdu = urduOpts.ifEmpty { seedFallback?.optionsUrdu ?: emptyList() },
                        explanationUrdu = m.explanationUrdu ?: seedFallback?.explanationUrdu
                    )
                }
            } else {
                MockSeedDataSource.mcqs.filter { it.chapterId == chapterId }
            }

            if (dtos.isEmpty()) {
                emit(
                    UiResourceState.Empty(
                        title = "No Practice MCQs Yet",
                        description = "Add MCQs for this chapter in the Admin Studio."
                    )
                )
            } else {
                emit(
                    UiResourceState.Success(
                        data = dtos.map { CurriculumMappers.toMcqUi(it) },
                        isOfflineFallback = fallback || cachedMcqs.isEmpty()
                    )
                )
            }
        }
    }

    override fun observePastPapers(
        classLevel: Int,
        solvedFilter: Boolean?
    ): Flow<UiResourceState<List<PastPaperUi>>> = flow {
        val prefs = preferencesDataStore.preferencesFlow.first()
        var fallback = false
        if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchPastPapers(prefs.configuredApiBaseUrl, classLevel, prefs.selectedBoardCode, solvedFilter)) {
                is ApiResult.Success -> {
                    cacheDao.upsertPastPapers(
                        res.data.map { p ->
                            CachedPastPaperEntity(
                                id = p.id,
                                boardCode = p.boardCode,
                                classLevel = p.classLevel,
                                subjectId = p.subjectId,
                                subjectName = p.subjectName,
                                year = p.year,
                                isSolved = p.isSolved,
                                pdfMetadataId = p.pdfMetadataId,
                                fileSizeMb = p.fileSizeMb,
                                pageCount = p.pageCount
                            )
                        }
                    )
                }
                is ApiResult.Failure -> fallback = true
            }
        } else {
            fallback = true
        }

        combine(
            cacheDao.observePastPapers(classLevel),
            offlineDao.observeDownloadedPdfs()
        ) { cachedPapers, downloadedPdfs ->
            val downloadedIds = downloadedPdfs.map { it.pdfId }.toSet()
            val dtos = if (cachedPapers.isNotEmpty()) {
                cachedPapers.map { p ->
                    com.imtihanpro.app.data.remote.ApiPastPaperDto(
                        id = p.id,
                        boardCode = p.boardCode,
                        classLevel = p.classLevel,
                        subjectId = p.subjectId,
                        subjectName = p.subjectName,
                        year = p.year,
                        isSolved = p.isSolved,
                        pdfMetadataId = p.pdfMetadataId,
                        fileSizeMb = p.fileSizeMb,
                        pageCount = p.pageCount
                    )
                }
            } else {
                MockSeedDataSource.pastPapers.filter { it.classLevel == classLevel }
            }.filter { solvedFilter == null || it.isSolved == solvedFilter }

            if (dtos.isEmpty()) {
                UiResourceState.Empty(
                    title = "No Past Papers Available",
                    description = "Upload past paper PDFs in the Admin Console to make them downloadable here."
                )
            } else {
                UiResourceState.Success(
                    data = dtos.map { CurriculumMappers.toPastPaperUi(it, downloadedIds) },
                    isOfflineFallback = fallback || cachedPapers.isEmpty()
                )
            }
        }.collect { emit(it) }
    }

    override fun observePdfLibrary(classLevel: Int, category: String?): Flow<List<PdfResourceUi>> = flow {
        val prefs = preferencesDataStore.preferencesFlow.first()
        val remote = if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchPdfs(prefs.configuredApiBaseUrl, category, classLevel, prefs.selectedBoardCode)) {
                is ApiResult.Success -> res.data
                is ApiResult.Failure -> null
            }
        } else null

        offlineDao.observeDownloadedPdfs().collect { downloaded ->
            val downloadedIds = downloaded.map { it.pdfId }.toSet()
            val source = remote ?: MockSeedDataSource.pdfLibrary.filter {
                it.classLevel == classLevel && (category == null || it.category == category)
            }
            emit(source.map { CurriculumMappers.toPdfResourceUi(it, downloadedIds) })
        }
    }

    override fun observeDownloads(): Flow<List<DownloadedFileUi>> {
        return offlineDao.observeDownloadedPdfs().map { rows ->
            rows.map {
                DownloadedFileUi(
                    id = it.pdfId,
                    title = it.title,
                    categoryLabel = it.categoryLabel,
                    sizeMb = it.sizeMb,
                    statusLabel = if (File(it.localFilePath).exists()) "Ready Offline" else "Metadata Cached"
                )
            }
        }
    }

    override suspend fun toggleOfflineChapter(chapterId: String, title: String, sizeMb: Double) {
        val existing = offlineDao.getCachedChapterOnce(chapterId)
        if (existing != null) {
            offlineDao.removeCachedChapter(chapterId)
            return
        }
        val prefs = preferencesDataStore.preferencesFlow.first()
        val blocks = if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchChapterContent(prefs.configuredApiBaseUrl, chapterId, "detailed_notes")) {
                is ApiResult.Success -> res.data.blocks
                is ApiResult.Failure -> MockSeedDataSource.detailedNotesBlocks[chapterId].orEmpty()
            }
        } else {
            MockSeedDataSource.detailedNotesBlocks[chapterId].orEmpty()
        }
        val json = ApiContentJsonParser.serializeContentBlocks(blocks)
        offlineDao.upsertCachedChapter(
            CachedChapterEntity(
                chapterId = chapterId,
                subjectId = "",
                chapterNumber = 1,
                title = title,
                contentVersion = "v1.0.0",
                serializedBlocksJson = json,
                downloadedAtEpochMs = System.currentTimeMillis(),
                sizeKb = (sizeMb * 1024).toInt().coerceAtLeast(1)
            )
        )
    }

    override fun observeDownloadTasks(): Flow<List<DownloadTaskUi>> {
        return downloadTaskDao.observeAll().map { rows ->
            rows.map { row ->
                val state = DownloadState.fromRaw(row.state)
                val reason = DownloadPausedReason.fromRaw(row.pausedReason)
                DownloadTaskUi(
                    pdfId = row.pdfId,
                    paperId = row.paperId,
                    title = row.title,
                    categoryLabel = row.categoryLabel,
                    state = state,
                    pausedReason = reason,
                    bytesDownloaded = row.bytesDownloaded,
                    totalBytes = row.totalBytes,
                    progressPercent = PdfDownloadEngine.computeProgressPercent(row.bytesDownloaded, row.totalBytes),
                    speedBytesPerSec = row.speedBytesPerSec,
                    errorCode = row.errorCode,
                    errorMessage = row.errorMessage,
                    localPath = row.localPath,
                    allowMobileData = row.allowMobileData,
                    updatedAtEpochMs = row.updatedAtEpochMs
                )
            }
        }
    }

    override suspend fun toggleOfflinePaper(paperId: String, title: String, sizeMb: Double) {
        val existing = offlineDao.getDownloadedPdfOnce(paperId)
        val existingTask = downloadTaskDao.getByIdOnce(paperId)
        if (existing != null || existingTask?.state == DownloadState.COMPLETED.name) {
            existing?.localFilePath?.let { File(it).delete() }
            existingTask?.localPath?.let { File(it).delete() }
            existingTask?.tmpPath?.takeIf { it.isNotBlank() }?.let { File(it).delete() }
            offlineDao.removeDownloadedPdf(paperId)
            downloadTaskDao.deleteById(paperId)
            PdfDownloadNotifications.cancelNotification(context, paperId)
            return
        }
        if (existingTask != null && (existingTask.state == DownloadState.RUNNING.name || existingTask.state == DownloadState.QUEUED.name)) {
            pausePdfDownload(paperId)
            return
        }
        enqueuePdfDownload(
            paperId = paperId,
            pdfId = paperId,
            title = title,
            categoryLabel = "Past Paper / PDF",
            sizeMb = sizeMb
        )
    }

    override suspend fun enqueuePdfDownload(
        paperId: String,
        pdfId: String,
        title: String,
        categoryLabel: String,
        sizeMb: Double,
        expectedSha256: String?,
        allowMobileData: Boolean
    ) {
        val targetPdfId = pdfId.ifBlank { paperId }
        val alreadyLocal = findLocalPdfPath(paperId, targetPdfId)
        val now = System.currentTimeMillis()
        if (alreadyLocal != null) {
            val fileLen = File(alreadyLocal).length()
            val resolvedMb = if (sizeMb > 0.0) sizeMb else maxOf(0.1, fileLen / (1024.0 * 1024.0))
            offlineDao.upsertDownloadedPdf(
                DownloadedPdfEntity(
                    pdfId = targetPdfId,
                    title = title,
                    categoryLabel = categoryLabel,
                    localFilePath = alreadyLocal,
                    sizeMb = resolvedMb,
                    downloadedAtEpochMs = now
                )
            )
            val existingTask = downloadTaskDao.getByIdOnce(targetPdfId)
            if (existingTask == null || existingTask.state != DownloadState.COMPLETED.name) {
                downloadTaskDao.upsert(
                    DownloadTaskEntity(
                        pdfId = targetPdfId,
                        paperId = paperId,
                        title = title,
                        categoryLabel = categoryLabel,
                        remoteUrl = "",
                        state = DownloadState.COMPLETED.name,
                        pausedReason = DownloadPausedReason.NONE.name,
                        bytesDownloaded = fileLen,
                        totalBytes = fileLen,
                        localPath = alreadyLocal,
                        createdAtEpochMs = existingTask?.createdAtEpochMs ?: now,
                        updatedAtEpochMs = now
                    )
                )
            }
            return
        }

        val prefs = preferencesDataStore.preferencesFlow.first()
        val seedMeta = MockSeedDataSource.pdfLibrary.firstOrNull { it.id == targetPdfId }
        val resolvedSha = expectedSha256?.takeIf { PdfDownloadEngine.isValidSha256Hex(it) }
            ?: seedMeta?.checksumSha256?.takeIf { PdfDownloadEngine.isValidSha256Hex(it) }
        val totalBytesHint = when {
            sizeMb > 0.0 -> (sizeMb * 1024.0 * 1024.0).toLong()
            seedMeta != null && seedMeta.fileSizeBytes > 0L -> seedMeta.fileSizeBytes
            else -> 0L
        }

        val pdfDir = File(context.filesDir, "imtihan_pdfs").apply { mkdirs() }
        val safeName = targetPdfId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetFile = File(pdfDir, "$safeName.pdf")
        val tmpFile = File(pdfDir, "${targetFile.name}.tmp")
        val partialBytes = if (tmpFile.exists() && tmpFile.length() > 0L) tmpFile.length() else 0L
        val remoteUrl = "${ApiConfig.normalizeBaseUrl(prefs.configuredApiBaseUrl)}/pdfs/$targetPdfId/download?mode=stream"

        val existing = downloadTaskDao.getByIdOnce(targetPdfId)
        val allowMobile = allowMobileData || (existing?.allowMobileData == true) || prefs.allowDownloadOverMobileData

        SyncScheduler.clearInterrupted(targetPdfId)
        PdfDownloadNotifications.cancelNotification(context, targetPdfId)

        val initialState: String
        val initialReason: String
        val initialErrorCode: String?
        val initialErrorMsg: String?
        if (prefs.offlineOnlyMode) {
            initialState = DownloadState.PAUSED.name
            initialReason = DownloadPausedReason.WAITING_NETWORK.name
            initialErrorCode = "OFFLINE_MODE"
            initialErrorMsg = "Offline Mode is enabled. Disable it in Profile to download."
        } else {
            initialState = DownloadState.QUEUED.name
            initialReason = DownloadPausedReason.NONE.name
            initialErrorCode = null
            initialErrorMsg = null
        }

        downloadTaskDao.upsert(
            DownloadTaskEntity(
                pdfId = targetPdfId,
                paperId = paperId,
                title = title,
                categoryLabel = categoryLabel,
                remoteUrl = remoteUrl,
                state = initialState,
                pausedReason = initialReason,
                bytesDownloaded = partialBytes,
                totalBytes = maxOf(totalBytesHint, existing?.totalBytes ?: 0L),
                speedBytesPerSec = 0L,
                etag = existing?.etag,
                expectedSha256 = resolvedSha ?: existing?.expectedSha256,
                retryCount = 0,
                nextRetryAtMs = 0L,
                errorCode = initialErrorCode,
                errorMessage = initialErrorMsg,
                localPath = null,
                tmpPath = tmpFile.absolutePath,
                allowMobileData = allowMobile,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )

        if (!prefs.offlineOnlyMode) {
            SyncScheduler.enqueueSerialDownloadQueue(context, replaceIfIdle = false)
        }
    }

    override suspend fun pausePdfDownload(pdfId: String) {
        SyncScheduler.markInterrupted(pdfId)
        val existing = downloadTaskDao.getByIdOnce(pdfId) ?: return
        downloadTaskDao.updateState(
            pdfId = pdfId,
            state = DownloadState.PAUSED.name,
            pausedReason = DownloadPausedReason.USER.name,
            errorCode = null,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        PdfDownloadNotifications.showPausedNotification(
            context = context,
            pdfId = pdfId,
            title = existing.title,
            bytesDownloaded = existing.bytesDownloaded,
            totalBytes = existing.totalBytes,
            reason = DownloadPausedReason.USER
        )
    }

    override suspend fun resumePdfDownload(pdfId: String) {
        SyncScheduler.clearInterrupted(pdfId)
        PdfDownloadNotifications.cancelNotification(context, pdfId)
        val existing = downloadTaskDao.getByIdOnce(pdfId) ?: return
        downloadTaskDao.upsert(
            existing.copy(
                state = DownloadState.QUEUED.name,
                pausedReason = DownloadPausedReason.NONE.name,
                retryCount = 0,
                nextRetryAtMs = 0L,
                errorCode = null,
                errorMessage = null,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
        SyncScheduler.enqueueSerialDownloadQueue(context, replaceIfIdle = false)
    }

    override suspend fun retryPdfDownload(pdfId: String) {
        resumePdfDownload(pdfId)
    }

    override suspend fun retryAllFailedDownloads() {
        val failed = downloadTaskDao.getFailedOnce()
        if (failed.isEmpty()) return
        val now = System.currentTimeMillis()
        for (task in failed) {
            SyncScheduler.clearInterrupted(task.pdfId)
            PdfDownloadNotifications.cancelNotification(context, task.pdfId)
            downloadTaskDao.upsert(
                task.copy(
                    state = DownloadState.QUEUED.name,
                    pausedReason = DownloadPausedReason.NONE.name,
                    retryCount = 0,
                    nextRetryAtMs = 0L,
                    errorCode = null,
                    errorMessage = null,
                    updatedAtEpochMs = now
                )
            )
        }
        SyncScheduler.enqueueSerialDownloadQueue(context, replaceIfIdle = false)
    }

    override suspend fun downloadOnMobileData(pdfId: String) {
        SyncScheduler.clearInterrupted(pdfId)
        PdfDownloadNotifications.cancelNotification(context, pdfId)
        downloadTaskDao.allowMobileDataAndQueue(pdfId, true, System.currentTimeMillis())
        SyncScheduler.enqueueSerialDownloadQueue(context, replaceIfIdle = false)
    }

    override suspend fun cancelPdfDownload(pdfId: String) {
        SyncScheduler.markInterrupted(pdfId)
        PdfDownloadNotifications.cancelNotification(context, pdfId)
        val existing = downloadTaskDao.getByIdOnce(pdfId)
        if (existing != null) {
            downloadTaskDao.updateState(
                pdfId = pdfId,
                state = DownloadState.CANCELLED.name,
                pausedReason = DownloadPausedReason.NONE.name,
                errorCode = null,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
            if (existing.tmpPath.isNotBlank()) File(existing.tmpPath).delete()
            existing.localPath?.let { File(it).delete() }
            downloadTaskDao.deleteById(pdfId)
        }
        offlineDao.removeDownloadedPdf(pdfId)
    }

    override suspend fun recoverDownloadQueueOnStartup() {
        SyncScheduler.registerConnectivityMonitor(context)
        SyncScheduler.recoverAndResumeQueueOnStartup(context)
    }

    override suspend fun clearAllDownloads() {
        val allTasks = downloadTaskDao.observeAll().first()
        allTasks.forEach { t ->
            SyncScheduler.markInterrupted(t.pdfId)
            PdfDownloadNotifications.cancelNotification(context, t.pdfId)
            if (t.tmpPath.isNotBlank()) File(t.tmpPath).delete()
            t.localPath?.let { File(it).delete() }
        }
        downloadTaskDao.deleteAll()
        val all = offlineDao.observeDownloadedPdfs().first()
        all.forEach { File(it.localFilePath).delete() }
        offlineDao.clearAllDownloads()
        File(context.filesDir, "imtihan_pdfs").deleteRecursively()
        File(context.filesDir, "pdfs").deleteRecursively()
    }

    override suspend fun findLocalPdfPath(paperId: String, pdfMetadataId: String): String? {
        val candidates = listOfNotNull(
            pdfMetadataId.takeIf { it.isNotBlank() },
            paperId.takeIf { it.isNotBlank() }
        ).distinct()
        for (id in candidates) {
            val task = downloadTaskDao.getByIdOnce(id)
            if (task != null && task.state == DownloadState.COMPLETED.name && !task.localPath.isNullOrBlank()) {
                val f = File(task.localPath)
                if (f.exists() && f.length() > 8L) return f.absolutePath
            }
            val row = offlineDao.getDownloadedPdfOnce(id)
            if (row != null && File(row.localFilePath).exists() && File(row.localFilePath).length() > 8L) {
                return row.localFilePath
            }
            val safeName = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val unifiedFile = File(File(context.filesDir, "imtihan_pdfs"), "$safeName.pdf")
            if (unifiedFile.exists() && unifiedFile.length() > 8L) {
                return unifiedFile.absolutePath
            }
            val legacyFile = File(File(context.filesDir, "pdfs"), "$id.pdf")
            if (legacyFile.exists() && legacyFile.length() > 8L) {
                return legacyFile.absolutePath
            }
        }
        return null
    }

    override suspend fun getOrDownloadPdfFile(
        paperId: String,
        pdfMetadataId: String,
        title: String,
        sizeMb: Double
    ): Result<String> {
        val existing = findLocalPdfPath(paperId, pdfMetadataId)
        if (existing != null) return Result.success(existing)
        enqueuePdfDownload(
            paperId = paperId,
            pdfId = pdfMetadataId.ifBlank { paperId },
            title = title,
            categoryLabel = "PDF Document",
            sizeMb = sizeMb
        )
        return Result.failure(IllegalStateException("Queued PDF download in background worker."))
    }

    override suspend fun initGuestOrGoogleSession(googleIdToken: String?): Result<String> {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (prefs.offlineOnlyMode) {
            return Result.failure(IllegalStateException("Disable Offline Mode before connecting to Worker API."))
        }
        val result = if (!googleIdToken.isNullOrBlank()) {
            apiClient.exchangeGoogleIdToken(
                baseUrl = prefs.configuredApiBaseUrl,
                idToken = googleIdToken,
                guestDeviceId = prefs.guestDeviceId,
                selectedClass = prefs.selectedClassLevel,
                selectedBoard = prefs.selectedBoardCode,
                selectedSubBoard = prefs.selectedSubBoardCode,
                studyGroup = prefs.studyGroupCode,
                language = prefs.apiLanguageCode()
            )
        } else {
            apiClient.initGuestSession(
                baseUrl = prefs.configuredApiBaseUrl,
                guestDeviceId = prefs.guestDeviceId,
                displayName = prefs.displayName,
                selectedClass = prefs.selectedClassLevel,
                selectedBoard = prefs.selectedBoardCode,
                selectedSubBoard = prefs.selectedSubBoardCode,
                studyGroup = prefs.studyGroupCode,
                language = prefs.apiLanguageCode()
            )
        }
        return when (result) {
            is ApiResult.Success -> {
                preferencesDataStore.saveAuthSession(
                    token = result.data.token,
                    displayName = result.data.displayName,
                    email = result.data.email,
                    isGoogleLinked = result.data.mode == "google_authenticated"
                )
                syncWithCloud()
                Result.success(
                    if (result.data.mode == "google_authenticated") {
                        "Linked Google account (${result.data.email ?: result.data.displayName ?: "Verified"})."
                    } else {
                        "Guest session synced with Cloudflare D1."
                    }
                )
            }
            is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
        }
    }

    override suspend fun syncWithCloud(): Result<String> {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (prefs.offlineOnlyMode) {
            return Result.failure(IllegalStateException("Offline Mode enabled."))
        }

        // 1. Flush pending quiz submissions (cap retries at 5 to protect Worker free-tier quotas)
        val pending = syncQueueDao.getAllPending()
        for (item in pending) {
            if (item.attemptCount >= 5) continue
            if (item.actionType == "quiz_submit") {
                val obj = runCatching { JSONObject(item.payloadJson) }.getOrNull()
                if (obj != null) {
                    val ansArr = obj.optJSONArray("answers") ?: JSONArray()
                    val pairs = mutableListOf<Pair<String, String?>>()
                    for (i in 0 until ansArr.length()) {
                        val a = ansArr.getJSONObject(i)
                        val opt = if (a.isNull("selectedOptionId")) null else a.optString("selectedOptionId")
                        pairs.add(a.getString("mcqId") to opt)
                    }
                    val res = apiClient.submitQuiz(
                        baseUrl = prefs.configuredApiBaseUrl,
                        sessionId = obj.getString("sessionId"),
                        guestDeviceId = prefs.guestDeviceId,
                        bearerToken = prefs.authToken,
                        subjectId = obj.getString("subjectId"),
                        chapterId = obj.optString("chapterId").takeIf { it.isNotBlank() && it != "null" },
                        answers = pairs,
                        elapsedSeconds = obj.optInt("elapsedSeconds", 0),
                        submittedAtIso = obj.optString("submittedAtIso", nowIsoUtc()),
                        examMode = obj.optString("examMode", "chapter_test"),
                        marksPerMcq = obj.optDouble("marksPerMcq", 1.0),
                        negativeMarkingPenalty = obj.optDouble("negativeMarkingPenalty", 0.0),
                        passPercentage = obj.optInt("passPercentage", 40)
                    )
                    if (res is ApiResult.Success) {
                        syncQueueDao.deleteById(item.id)
                    } else {
                        syncQueueDao.incrementAttempt(item.id)
                    }
                } else {
                    syncQueueDao.deleteById(item.id)
                }
            }
        }

        // 2. Compute honest local study hours and daily goal completion
        val activity = activityDao.getAllActivityOnce()
        val totalMinutes = activity.sumOf { it.minutesLogged }
        val studyHours = ((totalMinutes / 60.0) * 10.0).roundToInt() / 10.0
        val todayMins = activity.firstOrNull { it.dayYmd == todayYmdUtc() }?.minutesLogged ?: 0
        val dailyGoalPct = ((todayMins.toFloat() / maxOf(1, prefs.dailyTargetMinutes).toFloat()) * 100f)
            .roundToInt().coerceIn(0, 100)

        val localProgress = progressDao.getAllProgressOnce().map {
            ApiChapterProgressSyncDto(
                chapterId = it.chapterId,
                subjectId = it.subjectId,
                completionPercent = it.completionPercent,
                lastBlockIndex = it.lastReadBlockIndex,
                isCompleted = it.isCompleted,
                lastAccessedAtIso = nowIsoUtc()
            )
        }
        return when (
            val res = apiClient.syncProgress(
                baseUrl = prefs.configuredApiBaseUrl,
                guestDeviceId = prefs.guestDeviceId,
                bearerToken = prefs.authToken,
                selectedClass = prefs.selectedClassLevel,
                selectedBoard = prefs.selectedBoardCode,
                studyHoursTotal = studyHours,
                dailyGoalPercent = dailyGoalPct,
                chapters = localProgress,
                selectedSubBoard = prefs.selectedSubBoardCode,
                studyGroup = prefs.studyGroupCode,
                language = prefs.apiLanguageCode(),
                dailyTargetMinutes = prefs.dailyTargetMinutes,
                examCountdownDateIso = prefs.examCountdownDateIso,
                diagnosticCompleted = prefs.diagnosticCompleted
            )
        ) {
            is ApiResult.Success -> {
                val merged = ImtihanSyncRules.mergeProgressMonotonic(localProgress, res.data.chapterProgress)
                val now = System.currentTimeMillis()
                progressDao.upsertAllProgress(
                    merged.map {
                        GuestProgressEntity(
                            chapterId = it.chapterId,
                            subjectId = it.subjectId,
                            completionPercent = it.completionPercent,
                            lastReadBlockIndex = it.lastBlockIndex,
                            isCompleted = it.isCompleted,
                            updatedAtEpochMs = now
                        )
                    }
                )
                val stamp = nowIsoUtc()
                preferencesDataStore.setLastSyncedAtIso(stamp)
                Result.success("Synced ${merged.size} chapter progress records at $stamp")
            }
            is ApiResult.Failure -> Result.failure(IllegalStateException(res.message))
        }
    }

    override suspend fun submitQuizResult(
        subjectId: String,
        chapterId: String,
        questions: List<McqQuestionUi>,
        selectedAnswers: Map<String, String>,
        elapsedSeconds: Int
    ): QuizResultUi = submitExamResult(
        subjectId = subjectId,
        subjectName = MockSeedDataSource.subjects.firstOrNull { it.id == subjectId }?.name ?: "Subject",
        chapterId = chapterId,
        chapterTitle = MockSeedDataSource.chapters.firstOrNull { it.id == chapterId }?.title,
        examMode = "chapter_test",
        questions = questions,
        selectedAnswers = selectedAnswers,
        elapsedSeconds = elapsedSeconds
    )

    override suspend fun submitExamResult(
        subjectId: String,
        subjectName: String,
        chapterId: String?,
        chapterTitle: String?,
        examMode: String,
        questions: List<McqQuestionUi>,
        selectedAnswers: Map<String, String>,
        elapsedSeconds: Int,
        marksPerMcq: Double,
        negativeMarkingPenalty: Double,
        passPercentage: Int,
        sessionId: String?
    ): QuizResultUi {
        val prefs = preferencesDataStore.preferencesFlow.first()
        val effectiveSessionId = sessionId?.takeIf { it.isNotBlank() }
            ?: "quiz_${UUID.randomUUID().toString().take(10)}"
        val submittedAt = nowIsoUtc()
        val nowMs = System.currentTimeMillis()
        val answerPairs = questions.map { it.id to selectedAnswers[it.id] }

        var sentOnline = false
        var remoteDto: com.imtihanpro.app.data.remote.ApiQuizSubmissionResultDto? = null
        if (!prefs.offlineOnlyMode) {
            val remote = apiClient.submitQuiz(
                baseUrl = prefs.configuredApiBaseUrl,
                sessionId = effectiveSessionId,
                guestDeviceId = prefs.guestDeviceId,
                bearerToken = prefs.authToken,
                subjectId = subjectId,
                chapterId = chapterId,
                answers = answerPairs,
                elapsedSeconds = elapsedSeconds,
                submittedAtIso = submittedAt,
                examMode = examMode,
                marksPerMcq = marksPerMcq,
                negativeMarkingPenalty = negativeMarkingPenalty,
                passPercentage = passPercentage
            )
            if (remote is ApiResult.Success) {
                sentOnline = true
                remoteDto = remote.data
            }
        }

        val result = if (remoteDto != null && remoteDto.perQuestionFeedback.isNotEmpty()) {
            val feedbackByMcqId = remoteDto.perQuestionFeedback.associateBy { it.mcqId }
            val topicAgg = linkedMapOf<String, Pair<Int, Int>>()
            val review = questions.map { q ->
                val fb = feedbackByMcqId[q.id]
                val chosen = selectedAnswers[q.id]
                val resolvedCorrect = fb?.correctOptionId ?: q.correctOptionId
                val resolvedExp = fb?.explanation?.ifBlank { null } ?: q.explanation ?: ""
                val isCorrect = fb?.isCorrect ?: (chosen != null && resolvedCorrect != null && chosen == resolvedCorrect)
                val topic = q.topicTitle.ifBlank { "Core Concepts" }
                val prev = topicAgg[topic] ?: (0 to 0)
                topicAgg[topic] = (prev.first + if (isCorrect) 1 else 0) to (prev.second + 1)
                QuizReviewItemUi(
                    mcqId = q.id,
                    question = q.question,
                    selectedOptionId = chosen,
                    correctOptionId = resolvedCorrect,
                    explanation = resolvedExp,
                    isCorrect = isCorrect,
                    topicTitle = topic
                )
            }
            val mins = remoteDto.elapsedSeconds / 60
            val secs = remoteDto.elapsedSeconds % 60
            val formattedTime = "%02d:%02d".format(Locale.US, mins, secs)
            val topicBreakdown = topicAgg.map { (topic, counts) ->
                val acc = if (counts.second == 0) 0 else ((counts.first.toFloat() / counts.second.toFloat()) * 100f).roundToInt()
                TopicScoreBreakdownUi(topicTitle = topic, correct = counts.first, total = counts.second, accuracyPercent = acc)
            }
            QuizResultUi(
                totalQuestions = remoteDto.totalQuestions,
                correctCount = remoteDto.correctCount,
                incorrectCount = remoteDto.incorrectCount,
                unansweredCount = remoteDto.unansweredCount,
                percentage = remoteDto.percentage,
                elapsedTimeFormatted = formattedTime,
                reviewItems = review,
                marksObtained = remoteDto.marksObtained,
                totalMarks = remoteDto.totalMarks,
                passed = remoteDto.passed,
                passPercentage = remoteDto.passPercentage,
                xpEarned = remoteDto.xpEarned,
                topicBreakdown = topicBreakdown,
                examMode = examMode
            )
        } else {
            Stage3To8Engines.gradeQuizWithRules(
                questions = questions,
                selectedAnswers = selectedAnswers,
                elapsedSeconds = elapsedSeconds,
                marksPerMcq = marksPerMcq,
                negativeMarkingPenalty = negativeMarkingPenalty,
                passPercentage = passPercentage
            ).copy(examMode = examMode)
        }

        // Save quiz attempt locally in Room
        quizAttemptDao.insertAttempt(
            QuizAttemptEntity(
                id = "qa_${UUID.randomUUID().toString().take(10)}",
                sessionId = effectiveSessionId,
                subjectId = subjectId,
                subjectName = subjectName,
                chapterId = chapterId,
                chapterTitle = chapterTitle,
                examMode = examMode,
                totalQuestions = result.totalQuestions,
                correctCount = result.correctCount,
                incorrectCount = result.incorrectCount,
                unansweredCount = result.unansweredCount,
                percentage = result.percentage,
                marksObtained = result.marksObtained,
                totalMarks = result.totalMarks,
                passed = result.passed,
                elapsedSeconds = elapsedSeconds,
                submittedAtIso = submittedAt,
                submittedAtEpochMs = nowMs
            )
        )

        // Record mistakes and resolve mastered questions locally
        val reviewByMcqId = result.reviewItems.associateBy { it.mcqId }
        for (q in questions) {
            val reviewItem = reviewByMcqId[q.id]
            val chosen = selectedAnswers[q.id]
            val isCorrect = reviewItem?.isCorrect ?: (chosen != null && q.correctOptionId != null && chosen == q.correctOptionId)
            val resolvedCorrect = reviewItem?.correctOptionId ?: q.correctOptionId
            val resolvedExplanation = reviewItem?.explanation?.ifBlank { null } ?: q.explanation ?: ""
            if (isCorrect) {
                mistakeDao.markMastered(q.id, nowMs)
            } else if (!resolvedCorrect.isNullOrBlank()) {
                val existing = mistakeDao.getByIdOnce(q.id)
                mistakeDao.upsertMistake(
                    MistakeQuestionEntity(
                        mcqId = q.id,
                        subjectId = q.subjectId.ifBlank { subjectId },
                        chapterId = q.chapterId.ifBlank { chapterId ?: "" },
                        topicTitle = q.topicTitle,
                        question = q.question,
                        optionsJson = ApiContentJsonParser.serializeOptions(q.options.map { it.id to it.text }),
                        lastWrongOptionId = chosen,
                        correctOptionId = resolvedCorrect,
                        explanation = resolvedExplanation,
                        wrongCount = (existing?.wrongCount ?: 0) + 1,
                        mastered = false,
                        lastAttemptedAtEpochMs = nowMs
                    )
                )
            }
        }

        // Record today's study activity for honest streak & daily goal calculation
        val loggedMinutes = maxOf(1, (elapsedSeconds / 60.0).roundToInt())
        recordActivityDelta(minutesDelta = loggedMinutes, quizzesDelta = 1, chaptersDelta = 0)

        if (examMode == "diagnostic_test") {
            preferencesDataStore.setDiagnosticCompleted(true)
        }

        if (!sentOnline) {
            val arr = JSONArray()
            answerPairs.forEach { (id, opt) ->
                arr.put(JSONObject().put("mcqId", id).put("selectedOptionId", opt ?: JSONObject.NULL))
            }
            val queued = JSONObject()
                .put("sessionId", effectiveSessionId)
                .put("subjectId", subjectId)
                .put("chapterId", chapterId ?: JSONObject.NULL)
                .put("elapsedSeconds", elapsedSeconds)
                .put("submittedAtIso", submittedAt)
                .put("examMode", examMode)
                .put("marksPerMcq", marksPerMcq)
                .put("negativeMarkingPenalty", negativeMarkingPenalty)
                .put("passPercentage", passPercentage)
                .put("answers", arr)
            syncQueueDao.enqueue(
                PendingSyncQueueEntity(
                    actionType = "quiz_submit",
                    payloadJson = queued.toString(),
                    createdAtEpochMs = nowMs
                )
            )
            SyncScheduler.enqueueImmediateSync(context)
        }

        if (!chapterId.isNullOrBlank() && result.percentage >= 60) {
            recordChapterProgress(chapterId, subjectId, 100, 0)
        }
        return result
    }

    private suspend fun recordActivityDelta(minutesDelta: Int, quizzesDelta: Int, chaptersDelta: Int) {
        val ymd = todayYmdUtc()
        val prev = activityDao.getDayOnce(ymd)
        activityDao.upsertDay(
            StudyActivityDayEntity(
                dayYmd = ymd,
                minutesLogged = (prev?.minutesLogged ?: 0) + minutesDelta,
                quizzesCompleted = (prev?.quizzesCompleted ?: 0) + quizzesDelta,
                chaptersStudied = (prev?.chaptersStudied ?: 0) + chaptersDelta
            )
        )
    }

    override suspend fun recordChapterProgress(
        chapterId: String,
        subjectId: String,
        completionPercent: Int,
        lastBlockIndex: Int
    ) {
        val current = progressDao.getAllProgressOnce().firstOrNull { it.chapterId == chapterId }
        val nextPct = maxOf(current?.completionPercent ?: 0, completionPercent.coerceIn(0, 100))
        val nextBlock = maxOf(current?.lastReadBlockIndex ?: 0, lastBlockIndex)
        progressDao.upsertProgress(
            GuestProgressEntity(
                chapterId = chapterId,
                subjectId = subjectId.ifBlank { current?.subjectId ?: "" },
                completionPercent = nextPct,
                lastReadBlockIndex = nextBlock,
                isCompleted = nextPct >= 100 || (current?.isCompleted == true),
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
        if (current == null) {
            recordActivityDelta(minutesDelta = 5, quizzesDelta = 0, chaptersDelta = 1)
        }
    }

    override fun observeHomeMetrics(classLevel: Int, boardCode: String): Flow<RealHomeSummaryMetrics> {
        return combine(
            progressDao.observeAllProgress(),
            activityDao.observeRecentActivity(),
            quizAttemptDao.observeQuizHistory(),
            preferencesDataStore.preferencesFlow
        ) { progress, activity, quizzes, prefs ->
            val subjects = MockSeedDataSource.subjects.filter { it.classLevel == classLevel && it.boardCode == boardCode }
            val chapters = MockSeedDataSource.chapters.filter { ch -> subjects.any { it.id == ch.subjectId } }
            val totalCh = chapters.size.coerceAtLeast(1)
            val completedCh = progress.count { p -> p.isCompleted && chapters.any { it.id == p.chapterId } }
            val avgProgress = if (progress.isEmpty()) 0 else {
                val relevant = progress.filter { p -> chapters.any { it.id == p.chapterId } }
                if (relevant.isEmpty()) 0 else (relevant.sumOf { it.completionPercent } / totalCh).coerceIn(0, 100)
            }

            val totalMinutes = activity.sumOf { it.minutesLogged }
            val studyHours = totalMinutes / 60
            val todayMins = activity.firstOrNull { it.dayYmd == todayYmdUtc() }?.minutesLogged ?: 0
            val dailyGoalPct = ((todayMins.toFloat() / maxOf(1, prefs.dailyTargetMinutes).toFloat()) * 100f)
                .roundToInt().coerceIn(0, 100)
            val (currentStreak, _) = Stage3To8Engines.computeStudyStreak(activity.map { it.dayYmd })
            val xp = quizzes.sumOf { maxOf(5, it.correctCount * 10) } + completedCh * 25
            val latestProg = progress.maxByOrNull { it.updatedAtEpochMs }
            val continueChapter = latestProg?.let { p -> chapters.firstOrNull { it.id == p.chapterId } }
                ?: chapters.firstOrNull()

            RealHomeSummaryMetrics(
                studyHours = studyHours,
                chaptersCompleted = completedCh,
                overallProgressPercent = avgProgress,
                dailyGoalPercent = dailyGoalPct,
                currentStreakDays = currentStreak,
                xpTotal = xp,
                recentDailyMinutes = activity.sortedBy { it.dayYmd }.takeLast(7).map { it.dayYmd.takeLast(5) to it.minutesLogged },
                continueChapterId = continueChapter?.id,
                continueChapterTitle = continueChapter?.let { "Chapter ${it.chapterNumber}: ${it.title}" },
                continueChapterPercent = latestProg?.completionPercent ?: 0
            )
        }
    }

    override suspend fun searchCurriculum(query: String, classLevel: Int, boardCode: String): SearchResultsUi {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return SearchResultsUi()
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.searchCatalog(prefs.configuredApiBaseUrl, trimmed, boardCode, classLevel)) {
                is ApiResult.Success -> {
                    val d = res.data
                    return SearchResultsUi(
                        query = trimmed,
                        subjects = d.subjects.map { CurriculumMappers.toSubjectUi(it, 0) },
                        chapters = d.chapters.map { CurriculumMappers.toChapterUi(it, emptySet(), emptySet(), emptySet()) },
                        topics = d.topics.map { t ->
                            t.chapterTitle to com.imtihanpro.app.domain.model.TopicUiModel(
                                t.id, t.chapterId, 0, t.title, 15
                            )
                        },
                        mcqs = d.mcqs.map { CurriculumMappers.toMcqUi(it) },
                        pdfs = d.pdfs.map { CurriculumMappers.toPdfResourceUi(it, emptySet()) }
                    )
                }
                is ApiResult.Failure -> Unit
            }
        }

        // Local Room + seed search
        val subjects = MockSeedDataSource.subjects.filter {
            it.classLevel == classLevel && it.boardCode == boardCode && it.name.contains(trimmed, ignoreCase = true)
        }.map { CurriculumMappers.toSubjectUi(it, 0) }
        val chapters = MockSeedDataSource.chapters.filter {
            it.title.contains(trimmed, ignoreCase = true) || it.subtitle.contains(trimmed, ignoreCase = true)
        }.map { CurriculumMappers.toChapterUi(it, emptySet(), emptySet(), emptySet()) }
        val topics = MockSeedDataSource.chapters.flatMap { ch ->
            ch.topics.filter { it.title.contains(trimmed, ignoreCase = true) }
                .map { ch.title to com.imtihanpro.app.domain.model.TopicUiModel(it.id, ch.id, it.orderIndex, it.title, it.estimatedMinutes) }
        }
        val mcqs = MockSeedDataSource.mcqs.filter {
            it.question.contains(trimmed, ignoreCase = true) || it.topicTitle.contains(trimmed, ignoreCase = true)
        }.map { CurriculumMappers.toMcqUi(it) }
        val pdfs = MockSeedDataSource.pdfLibrary.filter {
            it.classLevel == classLevel && it.title.contains(trimmed, ignoreCase = true)
        }.map { CurriculumMappers.toPdfResourceUi(it, emptySet()) }

        return SearchResultsUi(trimmed, subjects, chapters, topics, mcqs, pdfs)
    }

    override suspend fun loadExamPatterns(classLevel: Int, boardCode: String, subjectId: String?): List<ExamPatternUi> {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchExamPatterns(prefs.configuredApiBaseUrl, boardCode, classLevel, subjectId)) {
                is ApiResult.Success -> if (res.data.isNotEmpty()) return res.data.map { CurriculumMappers.toExamPatternUi(it) }
                is ApiResult.Failure -> Unit
            }
        }
        return MockSeedDataSource.examPatterns
            .filter { it.classLevel == classLevel && it.boardCode == boardCode && (subjectId == null || it.subjectId == null || it.subjectId == subjectId) }
            .map { CurriculumMappers.toExamPatternUi(it) }
    }

    override suspend fun startExamSession(
        mode: String,
        boardCode: String,
        classLevel: Int,
        patternId: String?,
        subjectId: String?,
        chapterId: String?,
        topicTitle: String?,
        difficulty: String,
        questionCount: Int
    ): Result<LoadedExamSession> {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            when (
                val res = apiClient.startExamSession(
                    baseUrl = prefs.configuredApiBaseUrl,
                    mode = mode,
                    boardCode = boardCode,
                    classLevel = classLevel,
                    patternId = patternId,
                    subjectId = subjectId,
                    chapterId = chapterId,
                    topicTitle = topicTitle,
                    difficulty = difficulty,
                    questionCount = questionCount
                )
            ) {
                is ApiResult.Success -> {
                    val d = res.data
                    return Result.success(
                        LoadedExamSession(
                            sessionId = d.sessionId,
                            title = d.title,
                            mode = d.mode,
                            subjectId = d.subjectId,
                            chapterId = d.chapterId,
                            durationMinutes = d.durationMinutes,
                            marksPerMcq = d.marksPerMcq,
                            negativeMarkingPenalty = d.negativeMarkingPenalty,
                            passPercentage = d.passPercentage,
                            mcqs = d.mcqs.map { CurriculumMappers.toMcqUi(it) },
                            subjectivePrompts = d.subjectivePrompts.map { CurriculumMappers.toSubjectivePromptUi(it) }
                        )
                    )
                }
                is ApiResult.Failure -> Unit
            }
        }

        // Offline local exam generator from Room + seed MCQs
        val pattern = patternId?.let { id -> MockSeedDataSource.examPatterns.firstOrNull { it.id == id } }
        val subjectsForClass = MockSeedDataSource.subjects.filter { it.classLevel == classLevel && it.boardCode == boardCode }
        val subjectIds = subjectsForClass.map { it.id }.toSet()

        val pool = MockSeedDataSource.mcqs.filter { m ->
            val classOk = m.subjectId in subjectIds || subjectsForClass.isEmpty()
            val subOk = subjectId == null || m.subjectId == subjectId
            val chOk = chapterId == null || m.chapterId == chapterId
            val topicOk = topicTitle.isNullOrBlank() || m.topicTitle.equals(topicTitle, ignoreCase = true)
            val diffOk = difficulty == "mixed" || m.difficulty.equals(difficulty, ignoreCase = true)
            classOk && subOk && chOk && topicOk && diffOk
        }.ifEmpty {
            MockSeedDataSource.mcqs.filter { it.subjectId in subjectIds }
        }

        if (pool.isEmpty()) {
            return Result.failure(IllegalStateException("No MCQs available offline for Class $classLevel."))
        }

        val selected = pool.take(pattern?.questionCount ?: questionCount).map { CurriculumMappers.toMcqUi(it) }
        val subjectivePrompts = if (mode == "full_mock_exam") {
            val blockSource = chapterId?.let { MockSeedDataSource.detailedNotesBlocks[it] }
                ?: MockSeedDataSource.detailedNotesBlocks.values.flatten()
            blockSource.filterIsInstance<com.imtihanpro.app.data.remote.ApiContentBlockDto.SolvedQuestionDto>()
                .take(4)
                .map {
                    SubjectivePromptUi(it.id, it.questionType, it.marks, it.question, it.answerSteps, it.yearReference)
                }
        } else {
            emptyList()
        }

        return Result.success(
            LoadedExamSession(
                sessionId = "local_exam_${UUID.randomUUID().toString().take(8)}",
                title = pattern?.title ?: "KPK Class $classLevel ${mode.replace('_', ' ')}",
                mode = mode,
                subjectId = subjectId ?: selected.first().subjectId,
                chapterId = chapterId,
                durationMinutes = pattern?.durationMinutes ?: maxOf(5, selected.size),
                marksPerMcq = pattern?.marksPerMcq ?: 1.0,
                negativeMarkingPenalty = pattern?.negativeMarkingPenalty ?: 0.0,
                passPercentage = pattern?.passPercentage ?: 40,
                mcqs = selected,
                subjectivePrompts = subjectivePrompts
            )
        )
    }

    override fun observeQuizHistory(): Flow<List<QuizHistoryItemUi>> =
        quizAttemptDao.observeQuizHistory().map { list ->
            list.map {
                QuizHistoryItemUi(
                    id = it.id,
                    subjectId = it.subjectId,
                    subjectName = it.subjectName,
                    chapterId = it.chapterId,
                    chapterTitle = it.chapterTitle,
                    examMode = it.examMode,
                    totalQuestions = it.totalQuestions,
                    correctCount = it.correctCount,
                    percentage = it.percentage,
                    marksObtained = it.marksObtained,
                    totalMarks = it.totalMarks,
                    passed = it.passed,
                    elapsedSeconds = it.elapsedSeconds,
                    submittedAtIso = it.submittedAtIso
                )
            }
        }

    override suspend fun refreshQuizHistory() {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (prefs.offlineOnlyMode) return
        when (val res = apiClient.fetchQuizHistory(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken)) {
            is ApiResult.Success -> {
                res.data.forEach { item ->
                    quizAttemptDao.insertAttempt(
                        QuizAttemptEntity(
                            id = item.id,
                            sessionId = item.sessionId,
                            subjectId = item.subjectId,
                            subjectName = item.subjectName,
                            chapterId = item.chapterId,
                            chapterTitle = item.chapterTitle,
                            examMode = item.examMode,
                            totalQuestions = item.totalQuestions,
                            correctCount = item.correctCount,
                            incorrectCount = item.incorrectCount,
                            unansweredCount = item.unansweredCount,
                            percentage = item.percentage,
                            marksObtained = item.marksObtained,
                            totalMarks = item.totalMarks,
                            passed = item.passed,
                            elapsedSeconds = item.elapsedSeconds,
                            submittedAtIso = item.submittedAtIso,
                            submittedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                }
            }
            is ApiResult.Failure -> Unit
        }
    }

    override fun observeStudentBookmarks(): Flow<List<StudentBookmarkUi>> =
        bookmarkDao.observeBookmarks().map { rows ->
            rows.map {
                StudentBookmarkUi(it.id, it.itemType, it.itemId, it.chapterId, it.subjectId, it.title, it.subtitle, it.createdAtEpochMs)
            }
        }

    override suspend fun toggleStudentBookmark(
        itemType: String,
        itemId: String,
        chapterId: String?,
        subjectId: String?,
        title: String,
        subtitle: String
    ) {
        val existing = bookmarkDao.findByTypeAndItemId(itemType, itemId)
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (existing != null) {
            bookmarkDao.deleteByIdOrItemId(existing.id)
            if (!prefs.offlineOnlyMode) {
                apiClient.deleteBookmark(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken, itemId)
            }
        } else {
            val id = "bmk_${UUID.randomUUID().toString().take(10)}"
            bookmarkDao.upsertBookmark(
                BookmarkEntity(id, itemType, itemId, chapterId, subjectId, title, subtitle, System.currentTimeMillis())
            )
            if (!prefs.offlineOnlyMode) {
                apiClient.upsertBookmark(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken, itemType, itemId, chapterId, subjectId, title, subtitle)
            }
        }
    }

    override fun observeMistakes(): Flow<List<MistakeRecordUi>> =
        mistakeDao.observeActiveMistakes().map { rows ->
            rows.map { r ->
                val opts = ApiContentJsonParser.parseOptions(runCatching { JSONArray(r.optionsJson) }.getOrNull())
                    .map { com.imtihanpro.app.domain.model.McqOptionUi(it.first, it.second) }
                MistakeRecordUi(
                    mcqId = r.mcqId,
                    subjectId = r.subjectId,
                    chapterId = r.chapterId,
                    topicTitle = r.topicTitle,
                    question = r.question,
                    options = opts,
                    lastWrongOptionId = r.lastWrongOptionId,
                    correctOptionId = r.correctOptionId,
                    explanation = r.explanation,
                    wrongCount = r.wrongCount,
                    mastered = r.mastered
                )
            }
        }

    override suspend fun resolveMistake(mcqId: String) {
        mistakeDao.markMastered(mcqId, System.currentTimeMillis())
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            apiClient.resolveMistake(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken, mcqId)
        }
    }

    override fun observeFlashcards(): Flow<List<FlashcardUi>> = flow {
        val existing = flashcardDao.observeAllFlashcards().first()
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            flashcardDao.upsertAll(
                MockSeedDataSource.flashcards.map { f ->
                    FlashcardReviewEntity(
                        flashcardId = f.id,
                        subjectId = f.subjectId,
                        chapterId = f.chapterId,
                        front = f.front,
                        back = f.back,
                        formulaHint = f.formulaHint,
                        topicTitle = f.topicTitle,
                        intervalDays = 1,
                        easeFactor = 2.5,
                        repetitions = 0,
                        nextReviewEpochMs = now,
                        lastReviewedEpochMs = null,
                        frontUrdu = f.frontUrdu,
                        backUrdu = f.backUrdu
                    )
                }
            )
        }
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchFlashcards(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken, prefs.selectedClassLevel, dueOnly = false)) {
                is ApiResult.Success -> {
                    val now = System.currentTimeMillis()
                    flashcardDao.upsertAll(
                        res.data.map { f ->
                            val local = flashcardDao.getByIdOnce(f.id)
                            FlashcardReviewEntity(
                                flashcardId = f.id,
                                subjectId = f.subjectId,
                                chapterId = f.chapterId,
                                front = f.front,
                                back = f.back,
                                formulaHint = f.formulaHint,
                                topicTitle = f.topicTitle,
                                intervalDays = local?.intervalDays ?: f.intervalDays,
                                easeFactor = local?.easeFactor ?: f.easeFactor,
                                repetitions = local?.repetitions ?: f.repetitions,
                                nextReviewEpochMs = local?.nextReviewEpochMs ?: now,
                                lastReviewedEpochMs = local?.lastReviewedEpochMs,
                                frontUrdu = f.frontUrdu ?: local?.frontUrdu,
                                backUrdu = f.backUrdu ?: local?.backUrdu
                            )
                        }
                    )
                }
                is ApiResult.Failure -> Unit
            }
        }
        flashcardDao.observeAllFlashcards().collect { rows ->
            val now = System.currentTimeMillis()
            emit(
                rows.map { r ->
                    val seedFallback = MockSeedDataSource.flashcards.firstOrNull { it.id == r.flashcardId }
                    FlashcardUi(
                        id = r.flashcardId,
                        subjectId = r.subjectId,
                        chapterId = r.chapterId,
                        front = r.front,
                        back = r.back,
                        formulaHint = r.formulaHint,
                        topicTitle = r.topicTitle,
                        intervalDays = r.intervalDays,
                        easeFactor = r.easeFactor,
                        repetitions = r.repetitions,
                        isDue = r.nextReviewEpochMs <= now,
                        frontUrdu = r.frontUrdu ?: seedFallback?.frontUrdu,
                        backUrdu = r.backUrdu ?: seedFallback?.backUrdu
                    )
                }
            )
        }
    }

    override suspend fun reviewFlashcard(flashcardId: String, quality: Int) {
        val current = flashcardDao.getByIdOnce(flashcardId) ?: return
        val now = System.currentTimeMillis()
        val next = Stage3To8Engines.calculateSm2NextReview(
            previousIntervalDays = current.intervalDays,
            previousEaseFactor = current.easeFactor,
            previousRepetitions = current.repetitions,
            quality = quality,
            nowEpochMs = now
        )
        flashcardDao.upsertOne(
            current.copy(
                intervalDays = next.intervalDays,
                easeFactor = next.easeFactor,
                repetitions = next.repetitions,
                nextReviewEpochMs = next.nextReviewEpochMs,
                lastReviewedEpochMs = now
            )
        )
        recordActivityDelta(minutesDelta = 2, quizzesDelta = 0, chaptersDelta = 0)
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            apiClient.reviewFlashcard(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken, flashcardId, quality)
        }
    }

    override fun observeSmartInsights(classLevel: Int, boardCode: String): Flow<SmartInsightsUi> {
        return combine(
            progressDao.observeAllProgress(),
            quizAttemptDao.observeQuizHistory(),
            mistakeDao.observeActiveMistakes(),
            flashcardDao.observeDueFlashcards(System.currentTimeMillis()),
            preferencesDataStore.preferencesFlow
        ) { progressList, quizzes, mistakes, dueCards, prefs ->
            val subjects = MockSeedDataSource.subjects.filter { it.classLevel == classLevel && it.boardCode == boardCode }
            val chapters = MockSeedDataSource.chapters
                .filter { ch -> subjects.any { it.id == ch.subjectId } }
                .map { CurriculumMappers.toChapterUi(it, emptySet(), emptySet(), emptySet()) }

            val totalAttempts = quizzes.size
            val totalQuestionsAnswered = quizzes.sumOf { it.totalQuestions }
            val overallAccuracy = if (totalAttempts == 0) null else (quizzes.sumOf { it.percentage } / totalAttempts)
            val completedCount = progressList.count { p -> p.isCompleted && chapters.any { it.id == p.chapterId } }
            val publishedCount = MockSeedDataSource.chapters.count { ch ->
                subjects.any { it.id == ch.subjectId } && ch.publishStatus == "published"
            }

            val readiness = Stage3To8Engines.computeHonestExamReadiness(
                quizAttemptsCount = totalAttempts,
                totalQuestionsAnswered = totalQuestionsAnswered,
                overallAccuracyPercent = overallAccuracy,
                completedChaptersCount = completedCount,
                totalPublishedChaptersCount = publishedCount,
                activeMistakesCount = mistakes.size
            )

            val quizAggByCh = quizzes.filter { !it.chapterId.isNullOrBlank() }
                .groupBy { it.chapterId!! }
                .mapValues { (_, list) -> list.size to (list.sumOf { it.percentage } / list.size) }
            val mistakesByCh = mistakes.groupBy { it.chapterId }.mapValues { it.value.size }
            val completionByCh = progressList.associate { it.chapterId to it.completionPercent }
            val subjectNames = subjects.associate { it.id to it.name }

            val weakChapters = Stage3To8Engines.detectWeakChapters(
                chapters = chapters,
                subjectNamesById = subjectNames,
                quizAccuracyByChapterId = quizAggByCh,
                activeMistakesByChapterId = mistakesByCh,
                completionByChapterId = completionByCh
            )

            val subjectAnalytics = subjects.map { s ->
                val subQuizzes = quizzes.filter { it.subjectId == s.id }
                val subProg = progressList.filter { it.subjectId == s.id }
                val quizAvg = if (subQuizzes.isEmpty()) null else (subQuizzes.sumOf { it.percentage } / subQuizzes.size)
                val noteAvg = if (subProg.isEmpty()) 0 else (subProg.sumOf { it.completionPercent } / subProg.size)
                val mastery = when {
                    quizAvg != null -> (quizAvg * 0.65 + noteAvg * 0.35).roundToInt()
                    subProg.isNotEmpty() -> noteAvg
                    else -> null
                }
                SubjectAnalyticsUi(
                    subjectId = s.id,
                    subjectName = s.name,
                    masteryPercent = mastery,
                    quizAveragePercent = quizAvg,
                    chaptersCompleted = subProg.count { it.isCompleted },
                    totalChapters = s.totalChapters,
                    attemptsCount = subQuizzes.size
                )
            }

            val recommendations = buildList {
                if (mistakes.isNotEmpty()) {
                    val m = mistakes.first()
                    add(
                        RevisionRecommendationUi(
                            id = "rec_msk_${m.mcqId}",
                            chapterId = m.chapterId,
                            subjectId = m.subjectId,
                            title = "Clear ${mistakes.size} question(s) in Mistake Master",
                            reason = "Resolving past mistakes improves KPK Board accuracy.",
                            actionType = "review_mistakes"
                        )
                    )
                }
                weakChapters.take(3).forEach { w ->
                    add(
                        RevisionRecommendationUi(
                            id = "rec_wk_${w.chapterId}",
                            chapterId = w.chapterId,
                            subjectId = w.subjectId,
                            title = if (w.attemptsCount > 0) "Revise ${w.chapterTitle}" else "Quiz ${w.chapterTitle}",
                            reason = w.reason,
                            actionType = if (w.attemptsCount > 0) "revise_notes" else "take_test"
                        )
                    )
                }
            }

            SmartInsightsUi(
                readiness = readiness,
                diagnosticCompleted = prefs.diagnosticCompleted,
                overallAccuracyPercent = overallAccuracy,
                totalQuizAttempts = totalAttempts,
                masteredChaptersCount = completedCount,
                weakChapters = weakChapters,
                subjectAnalytics = subjectAnalytics,
                recommendations = recommendations,
                dueFlashcardsCount = dueCards.size,
                activeMistakesCount = mistakes.size
            )
        }
    }

    override suspend fun askAiTutor(
        chapterId: String?,
        subjectId: String?,
        classLevel: Int,
        boardCode: String,
        languageCode: String,
        action: String,
        prompt: String
    ): TutorResponseUi {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (prefs.offlineOnlyMode) {
            return TutorResponseUi(
                available = false,
                unavailableReason = "Offline Mode is active. Turn off Offline Mode in Profile to ask the AI Tutor."
            )
        }
        return when (
            val res = apiClient.askTutor(
                baseUrl = prefs.configuredApiBaseUrl,
                guestDeviceId = prefs.guestDeviceId,
                bearerToken = prefs.authToken,
                chapterId = chapterId,
                subjectId = subjectId,
                classLevel = classLevel,
                boardCode = boardCode,
                language = if (languageCode.equals("Urdu", true) || languageCode == "ur") "ur" else "en",
                action = action,
                prompt = prompt
            )
        ) {
            is ApiResult.Success -> TutorResponseUi(
                available = res.data.available,
                answer = res.data.answer,
                model = res.data.model,
                groundedSources = res.data.groundedSources.map {
                    TutorGroundedSourceUi(it.type, it.title, it.referenceId)
                },
                unavailableReason = res.data.unavailableReason
            )
            is ApiResult.Failure -> TutorResponseUi(
                available = false,
                unavailableReason = res.message
            )
        }
    }

    override fun observeStudyPlanner(): Flow<StudyPlannerUi> =
        combine(
            plannerDao.observeTasks(),
            activityDao.observeRecentActivity(),
            preferencesDataStore.preferencesFlow
        ) { tasks, activity, prefs ->
            val (currentStreak, longestStreak) = Stage3To8Engines.computeStudyStreak(activity.map { it.dayYmd })
            val todayMins = activity.firstOrNull { it.dayYmd == todayYmdUtc() }?.minutesLogged ?: 0
            StudyPlannerUi(
                dailyTargetMinutes = prefs.dailyTargetMinutes,
                examCountdownDateIso = prefs.examCountdownDateIso,
                remindersEnabled = prefs.notificationsEnabled,
                reminderHourLocal = prefs.reminderHourLocal,
                currentStreakDays = currentStreak,
                longestStreakDays = longestStreak,
                minutesLoggedToday = todayMins,
                tasks = tasks.map {
                    StudyPlanTaskUi(
                        id = it.id,
                        title = it.title,
                        subjectId = it.subjectId,
                        chapterId = it.chapterId,
                        plannedDateIso = it.plannedDateIso,
                        estimatedMinutes = it.estimatedMinutes,
                        taskType = it.taskType,
                        completed = it.completed
                    )
                },
                recentDailyMinutes = activity.sortedBy { it.dayYmd }.takeLast(7).map { it.dayYmd.takeLast(5) to it.minutesLogged }
            )
        }

    override suspend fun addStudyPlanTask(
        title: String,
        subjectId: String?,
        chapterId: String?,
        plannedDateIso: String,
        estimatedMinutes: Int,
        taskType: String
    ) {
        if (title.isBlank()) return
        val id = "tsk_${UUID.randomUUID().toString().take(10)}"
        val dateStr = plannedDateIso.ifBlank { todayYmdUtc() }
        plannerDao.upsertTask(
            StudyPlanTaskEntity(
                id = id,
                title = title.trim(),
                subjectId = subjectId,
                chapterId = chapterId,
                plannedDateIso = dateStr,
                estimatedMinutes = estimatedMinutes.coerceIn(5, 240),
                taskType = taskType,
                completed = false,
                completedAtIso = null,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            apiClient.createStudyPlanTask(
                prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken,
                title.trim(), subjectId, chapterId, dateStr, estimatedMinutes, taskType,
                id = id
            )
        }
    }

    override suspend fun toggleStudyPlanTask(taskId: String, completed: Boolean) {
        val current = plannerDao.getTaskOnce(taskId) ?: return
        plannerDao.upsertTask(
            current.copy(
                completed = completed,
                completedAtIso = if (completed) nowIsoUtc() else null
            )
        )
        if (completed && !current.completed) {
            recordActivityDelta(minutesDelta = current.estimatedMinutes, quizzesDelta = 0, chaptersDelta = 0)
        }
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            apiClient.toggleStudyPlanTask(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken, taskId, completed)
        }
    }

    override suspend fun deleteStudyPlanTask(taskId: String) {
        plannerDao.deleteTask(taskId)
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            apiClient.deleteStudyPlanTask(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken, taskId)
        }
    }

    override suspend fun updateStudyGoalPreferences(
        dailyTargetMinutes: Int,
        examCountdownDateIso: String?,
        reminderHourLocal: Int
    ) {
        preferencesDataStore.setStudyGoalPreferences(dailyTargetMinutes, examCountdownDateIso, reminderHourLocal)
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.offlineOnlyMode) {
            apiClient.updatePlannerGoals(
                prefs.configuredApiBaseUrl,
                prefs.guestDeviceId,
                prefs.authToken,
                dailyTargetMinutes,
                examCountdownDateIso
            )
        }
    }

    override fun observeMotivationSummary(classLevel: Int, boardCode: String): Flow<MotivationSummaryUi> = flow {
        val prefs = preferencesDataStore.preferencesFlow.first()
        val remoteSummary = if (!prefs.offlineOnlyMode) {
            when (val res = apiClient.fetchMotivationSummary(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken)) {
                is ApiResult.Success -> res.data
                is ApiResult.Failure -> null
            }
        } else null
        var baselineLocalXp: Int? = null

        combine(
            quizAttemptDao.observeQuizHistory(),
            mistakeDao.observeAllMistakes(),
            flashcardDao.observeAllFlashcards(),
            activityDao.observeRecentActivity(),
            preferencesDataStore.preferencesFlow
        ) { quizzes, allMistakes, flashcards, activity, curPrefs ->
            val (localStreak, _) = Stage3To8Engines.computeStudyStreak(activity.map { it.dayYmd })
            val reviewedCards = flashcards.count { it.lastReviewedEpochMs != null }
            val resolvedMistakes = allMistakes.count { it.mastered }
            val localXp = quizzes.sumOf { maxOf(5, it.correctCount * 10) } + reviewedCards * 5 + resolvedMistakes * 15
            if (baselineLocalXp == null) {
                baselineLocalXp = localXp
            }
            val localDeltaSinceFetch = maxOf(0, localXp - (baselineLocalXp ?: localXp))
            val effectiveXp = if (remoteSummary != null) {
                maxOf(localXp, remoteSummary.xpTotal + localDeltaSinceFetch)
            } else {
                localXp
            }
            val effectiveLevel = (effectiveXp / 150) + 1
            val effectiveStreak = maxOf(localStreak, remoteSummary?.currentStreakDays ?: 0)
            val todayQuizzes = activity.firstOrNull { it.dayYmd == todayYmdUtc() }?.quizzesCompleted ?: 0
            val weeklyQuizzes = activity.take(7).sumOf { it.quizzesCompleted }
            val alias = curPrefs.leaderboardAlias
                ?: remoteSummary?.aliasHandle
                ?: Stage3To8Engines.anonymizeLeaderboardHandle(curPrefs.displayName, curPrefs.guestDeviceId)

            val localAchievements = listOf(
                AchievementUi("first_quiz", "First Practice Attempt", "Submitted your first practice quiz.", 25, quizzes.isNotEmpty()),
                AchievementUi("perfect_score", "100% Concept Mastery", "Scored 100% on a quiz with 3+ questions.", 50, quizzes.any { it.totalQuestions >= 3 && it.percentage == 100 }),
                AchievementUi("flashcard_reviewer", "Spaced Repetition Starter", "Reviewed flashcards using SM-2 memory scheduling.", 20, reviewedCards > 0),
                AchievementUi("mistake_conqueror", "Mistake Master", "Resolved a missed question in Mistake Master.", 30, resolvedMistakes > 0),
                AchievementUi("streak_3", "3-Day Study Streak", "Studied on 3 consecutive days.", 40, effectiveStreak >= 3)
            )
            val mergedAchievements = if (remoteSummary != null && remoteSummary.achievements.isNotEmpty()) {
                val localUnlocked = localAchievements.filter { it.unlocked }.map { it.code }.toSet()
                remoteSummary.achievements.map {
                    AchievementUi(it.code, it.title, it.description, it.xpReward, it.unlocked || it.code in localUnlocked)
                }
            } else {
                localAchievements
            }

            val localChallenges = listOf(
                ChallengeUi("chg_daily_quiz", "Daily Practice Sprint", "Complete at least 1 quiz today.", "daily", 1, minOf(1, todayQuizzes), 25, todayQuizzes >= 1),
                ChallengeUi("chg_weekly_5", "Weekly KPK Board Marathon", "Complete 5 quizzes this week.", "weekly", 5, minOf(5, weeklyQuizzes), 100, weeklyQuizzes >= 5)
            )
            val mergedChallenges = if (remoteSummary != null && remoteSummary.challenges.isNotEmpty()) {
                val localById = localChallenges.associateBy { it.id }
                remoteSummary.challenges.map { rc ->
                    val lc = localById[rc.id]
                    val current = maxOf(rc.currentCount, lc?.currentCount ?: 0).coerceAtMost(rc.targetCount)
                    ChallengeUi(rc.id, rc.title, rc.description, rc.cadence, rc.targetCount, current, rc.xpReward, rc.completed || current >= rc.targetCount)
                }
            } else {
                localChallenges
            }

            val leaderboard = if (curPrefs.leaderboardOptIn) {
                if (remoteSummary != null && remoteSummary.leaderboard.isNotEmpty()) {
                    remoteSummary.leaderboard.map {
                        LeaderboardEntryUi(it.rank, it.aliasHandle, it.boardCode, it.classLevel, if (it.isCurrentUser) maxOf(it.xpTotal, effectiveXp) else it.xpTotal, it.streakDays, it.isCurrentUser)
                    }
                } else {
                    listOf(LeaderboardEntryUi(1, alias, boardCode, classLevel, effectiveXp, effectiveStreak, isCurrentUser = true))
                }
            } else emptyList()

            MotivationSummaryUi(
                xpTotal = effectiveXp,
                level = effectiveLevel,
                nextLevelXp = effectiveLevel * 150,
                currentStreakDays = effectiveStreak,
                leaderboardOptIn = curPrefs.leaderboardOptIn,
                aliasHandle = alias,
                achievements = mergedAchievements,
                challenges = mergedChallenges,
                leaderboard = leaderboard
            )
        }.collect { emit(it) }
    }

    override suspend fun setLeaderboardOptIn(optIn: Boolean, aliasHandle: String?) {
        val prefs = preferencesDataStore.preferencesFlow.first()
        val alias = aliasHandle?.trim()?.ifBlank { null }
            ?: Stage3To8Engines.anonymizeLeaderboardHandle(prefs.displayName, prefs.guestDeviceId)
        preferencesDataStore.setLeaderboardOptIn(optIn, alias)
        if (!prefs.offlineOnlyMode) {
            apiClient.setLeaderboardOptIn(prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken, optIn, alias)
        }
    }

    override suspend fun submitFeedback(
        category: String,
        subjectId: String?,
        chapterId: String?,
        mcqId: String?,
        title: String,
        description: String
    ): Result<String> {
        if (title.isBlank() || description.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter both a short title and details."))
        }
        val prefs = preferencesDataStore.preferencesFlow.first()
        val id = "fb_${UUID.randomUUID().toString().take(10)}"
        val synced = if (!prefs.offlineOnlyMode) {
            apiClient.submitFeedback(
                prefs.configuredApiBaseUrl, prefs.guestDeviceId, prefs.authToken,
                category, subjectId, chapterId, mcqId, title.trim(), description.trim()
            ) is ApiResult.Success
        } else false

        feedbackDao.upsertFeedback(
            PendingFeedbackEntity(
                id = id,
                category = category,
                subjectId = subjectId,
                chapterId = chapterId,
                mcqId = mcqId,
                title = title.trim(),
                description = description.trim(),
                synced = synced,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
        return Result.success(
            if (synced) "Report submitted to Imtihan Pro reviewers."
            else "Report saved locally and will sync when connected."
        )
    }

    override suspend fun fetchPublicAdPolicy(): Pair<AdPlacementPolicyUi, Boolean> {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (prefs.offlineOnlyMode) return AdPlacementPolicyUi() to false
        return when (val res = apiClient.fetchPublicConfig(prefs.configuredApiBaseUrl)) {
            is ApiResult.Success -> CurriculumMappers.toAdPolicyUi(res.data.adPlacement) to res.data.aiTutorConfigured
            is ApiResult.Failure -> AdPlacementPolicyUi() to false
        }
    }

    override fun observePdfBookmarks(pdfId: String): Flow<List<PdfBookmarkUi>> =
        pdfReaderDao.observeBookmarks(pdfId).map { list ->
            list.map { PdfBookmarkUi(it.id, it.pdfId, it.pageNumber, it.label) }
        }

    override suspend fun togglePdfBookmark(pdfId: String, pageNumber: Int, label: String) {
        val existing = pdfReaderDao.observeBookmarks(pdfId).first().firstOrNull { it.pageNumber == pageNumber }
        if (existing != null) {
            pdfReaderDao.removeBookmarkForPage(pdfId, pageNumber)
        } else {
            pdfReaderDao.upsertBookmark(
                PdfBookmarkEntity(
                    id = "${pdfId}_p$pageNumber",
                    pdfId = pdfId,
                    pageNumber = pageNumber,
                    label = label.ifBlank { "Page $pageNumber" },
                    createdAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    override fun observePdfAnnotations(pdfId: String): Flow<List<PdfAnnotationUi>> =
        pdfReaderDao.observeAnnotations(pdfId).map { list ->
            list.map { PdfAnnotationUi(it.id, it.pdfId, it.pageNumber, it.noteText, it.createdAtEpochMs) }
        }

    override suspend fun addPdfAnnotation(pdfId: String, pageNumber: Int, noteText: String) {
        if (noteText.isBlank()) return
        pdfReaderDao.upsertAnnotation(
            PdfAnnotationEntity(
                id = "note_${UUID.randomUUID().toString().take(10)}",
                pdfId = pdfId,
                pageNumber = pageNumber,
                noteText = noteText.trim(),
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deletePdfAnnotation(annotationId: String) {
        pdfReaderDao.deleteAnnotation(annotationId)
    }

    override suspend fun getPdfReadingState(pdfId: String): PdfReadingStateUi {
        val row = pdfReaderDao.getReadingStateOnce(pdfId)
        return if (row != null) {
            PdfReadingStateUi(row.pdfId, row.lastPage, row.totalPages, row.zoomPercent)
        } else {
            PdfReadingStateUi(pdfId)
        }
    }

    override suspend fun savePdfReadingState(
        pdfId: String,
        lastPage: Int,
        totalPages: Int,
        zoomPercent: Int
    ) {
        pdfReaderDao.upsertReadingState(
            PdfReadingStateEntity(
                pdfId = pdfId,
                lastPage = lastPage.coerceAtLeast(1),
                totalPages = totalPages.coerceAtLeast(1),
                zoomPercent = zoomPercent.coerceIn(75, 400),
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    override fun observePendingSyncCount(): Flow<Int> = syncQueueDao.observePendingCount()

    override fun gradeQuiz(
        questions: List<McqQuestionUi>,
        selectedAnswers: Map<String, String>,
        elapsedSeconds: Int
    ): QuizResultUi = computeQuizResult(questions, selectedAnswers, elapsedSeconds)

    override suspend fun downloadSubjectOfflinePack(subjectId: String): Result<Int> {
        val prefs = preferencesDataStore.preferencesFlow.first()
        val chapters = MockSeedDataSource.chapters.filter {
            it.subjectId == subjectId && it.publishStatus == "published"
        }.ifEmpty {
            cacheDao.observeChapterMeta(subjectId).first()
                .filter { it.publishStatus == "published" }
                .map {
                    com.imtihanpro.app.data.remote.ApiChapterDto(
                        id = it.id,
                        subjectId = it.subjectId,
                        chapterNumber = it.chapterNumber,
                        title = it.title,
                        subtitle = it.subtitle,
                        currentVersion = it.currentVersion,
                        publishStatus = it.publishStatus,
                        mcqCount = it.mcqCount,
                        solvedQuestionsCount = it.solvedQuestionsCount,
                        isLockedByDefault = it.isLockedByDefault,
                        estimatedSizeKb = it.estimatedSizeKb
                    )
                }
        }
        var cachedCount = 0
        for (ch in chapters) {
            val blocks = if (!prefs.offlineOnlyMode) {
                when (val res = apiClient.fetchChapterContent(prefs.configuredApiBaseUrl, ch.id, "detailed_notes")) {
                    is ApiResult.Success -> res.data.blocks
                    is ApiResult.Failure -> MockSeedDataSource.detailedNotesBlocks[ch.id].orEmpty()
                }
            } else {
                MockSeedDataSource.detailedNotesBlocks[ch.id].orEmpty()
            }
            val serialized = ApiContentJsonParser.serializeContentBlocks(blocks)
            offlineDao.upsertCachedChapter(
                CachedChapterEntity(
                    chapterId = ch.id,
                    subjectId = ch.subjectId,
                    chapterNumber = ch.chapterNumber,
                    title = ch.title,
                    contentVersion = ch.currentVersion,
                    serializedBlocksJson = serialized,
                    downloadedAtEpochMs = System.currentTimeMillis(),
                    sizeKb = maxOf(1, serialized.length / 1024)
                )
            )
            val mcqs = if (!prefs.offlineOnlyMode) {
                when (val res = apiClient.fetchChapterMcqs(prefs.configuredApiBaseUrl, ch.id)) {
                    is ApiResult.Success -> res.data
                    is ApiResult.Failure -> MockSeedDataSource.mcqs.filter { it.chapterId == ch.id }
                }
            } else {
                MockSeedDataSource.mcqs.filter { it.chapterId == ch.id }
            }
            if (mcqs.isNotEmpty()) {
                cacheDao.upsertMcqs(
                    mcqs.map { m ->
                        CachedMcqEntity(
                            id = m.id,
                            chapterId = m.chapterId,
                            subjectId = m.subjectId,
                            question = m.question,
                            optionsJson = ApiContentJsonParser.serializeOptions(m.options),
                            correctOptionId = m.correctOptionId ?: "",
                            explanation = m.explanation ?: "",
                            difficulty = m.difficulty,
                            topicTitle = m.topicTitle,
                            questionUrdu = m.questionUrdu,
                            optionsUrduJson = JSONArray(m.optionsUrdu).toString(),
                            explanationUrdu = m.explanationUrdu
                        )
                    }
                )
            }
            cachedCount += 1
        }
        return Result.success(cachedCount)
    }

    override suspend fun logFocusPomodoroSession(minutes: Int, completed: Boolean): Int {
        val safeMinutes = minutes.coerceIn(1, 180)
        recordActivityDelta(minutesDelta = safeMinutes, quizzesDelta = 0, chaptersDelta = 0)
        return Stage3To8Engines.computeFocusSessionXp(safeMinutes, completed)
    }
}

internal fun computeQuizResult(
    questions: List<McqQuestionUi>,
    selectedAnswers: Map<String, String>,
    elapsedSeconds: Int
): QuizResultUi = Stage3To8Engines.gradeQuizWithRules(
    questions = questions,
    selectedAnswers = selectedAnswers,
    elapsedSeconds = elapsedSeconds
)
