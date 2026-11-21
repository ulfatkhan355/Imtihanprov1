package com.imtihanpro.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imtihanpro.app.data.local.datastore.UserPreferencesDataStore
import com.imtihanpro.app.data.local.room.ImtihanDatabaseProvider
import com.imtihanpro.app.data.remote.ApiConfig
import com.imtihanpro.app.data.remote.ImtihanApiClient
import com.imtihanpro.app.data.remote.ImtihanSyncRules
import com.imtihanpro.app.data.repository.CurriculumRepository
import com.imtihanpro.app.data.repository.OfflineFirstCurriculumRepository
import com.imtihanpro.app.data.repository.RealHomeSummaryMetrics
import com.imtihanpro.app.data.repository.Stage1MockCurriculumRepository
import com.imtihanpro.app.domain.engine.DownloadPausedReason
import com.imtihanpro.app.domain.engine.DownloadState
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.AdPlacementPolicyUi
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.ContentBlockUi
import com.imtihanpro.app.domain.model.DownloadTaskUi
import com.imtihanpro.app.domain.model.DownloadedFileUi
import com.imtihanpro.app.domain.model.ExamPatternUi
import com.imtihanpro.app.domain.model.FlashcardUi
import com.imtihanpro.app.domain.model.McqOptionUi
import com.imtihanpro.app.domain.model.McqQuestionUi
import com.imtihanpro.app.domain.model.MistakeRecordUi
import com.imtihanpro.app.domain.model.MotivationSummaryUi
import com.imtihanpro.app.domain.model.PastPaperUi
import com.imtihanpro.app.domain.model.PdfAnnotationUi
import com.imtihanpro.app.domain.model.PdfBookmarkUi
import com.imtihanpro.app.domain.model.PdfResourceUi
import com.imtihanpro.app.domain.model.QuizHistoryItemUi
import com.imtihanpro.app.domain.model.QuizResultUi
import com.imtihanpro.app.domain.model.SearchResultsUi
import com.imtihanpro.app.domain.model.SmartInsightsUi
import com.imtihanpro.app.domain.model.StudentBookmarkUi
import com.imtihanpro.app.domain.model.StudentProfileUi
import com.imtihanpro.app.domain.model.StudyPlannerUi
import com.imtihanpro.app.domain.model.SubjectUiModel
import com.imtihanpro.app.domain.model.SubjectivePromptUi
import com.imtihanpro.app.domain.model.TutorResponseUi
import com.imtihanpro.app.domain.model.UiResourceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AppShellState(
    val hasCompletedSetup: Boolean = false,
    val isDarkMode: Boolean = true,
    val isOfflineSimulated: Boolean = false,
    val selectedClassLevel: Int = 11,
    val selectedBoardCode: String = "KPK",
    val selectedSubBoardCode: String = "PESHAWAR",
    val studyGroupCode: String = "PRE_MEDICAL",
    val languageCode: String = "English",
    val notificationsEnabled: Boolean = true,
    val dailyTargetMinutes: Int = 45,
    val examCountdownDateIso: String? = null,
    val reminderHourLocal: Int = 19,
    val diagnosticCompleted: Boolean = false,
    val leaderboardOptIn: Boolean = false,
    val leaderboardAlias: String? = null,
    val configuredApiBaseUrl: String = ApiConfig.defaultBaseUrl,
    val userEmail: String? = null,
    val lastSyncedAtIso: String? = null,
    val pendingSyncCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null,
    val realHomeMetrics: RealHomeSummaryMetrics = RealHomeSummaryMetrics(),
    val adPolicy: AdPlacementPolicyUi = AdPlacementPolicyUi(),
    val aiTutorConfigured: Boolean = false,
    val bannersShownToday: Int = 0,
    val soundEffectsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val readerHighContrastMode: String = "OLED",
    val allowDownloadOverMobileData: Boolean = false,
    val offlinePackStatusMessage: String? = null,
    val profile: StudentProfileUi = StudentProfileUi(
        displayName = "Guest Learner",
        isGuest = true,
        selectedClass = 11,
        selectedBoard = "KPK",
        studyHours = 0,
        chaptersCompleted = 0,
        overallProgressPercent = 0,
        dailyGoalPercent = 0,
        selectedSubBoard = "PESHAWAR",
        studyGroup = "PRE_MEDICAL"
    )
)

data class QuizSessionState(
    val chapterId: String = "ch_phy11_01",
    val subjectId: String = "sub_kpk11_phy",
    val sessionId: String = "chapter_${UUID.randomUUID().toString().take(8)}",
    val title: String = "Chapter Practice Quiz",
    val mode: String = "chapter_test",
    val currentIndex: Int = 0,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val markedForReviewIds: Set<String> = emptySet(),
    val elapsedSeconds: Int = 0,
    val durationMinutes: Int = 0,
    val marksPerMcq: Double = 1.0,
    val negativeMarkingPenalty: Double = 0.0,
    val passPercentage: Int = 40,
    val customQuestions: List<McqQuestionUi>? = null,
    val subjectivePrompts: List<SubjectivePromptUi> = emptyList(),
    val isLoadingSession: Boolean = false,
    val isSubmitting: Boolean = false,
    val sessionError: String? = null,
    val latestResult: QuizResultUi? = null
)

data class PdfViewerUiState(
    val paperId: String = "",
    val pdfId: String = "",
    val localFilePath: String? = null,
    val isDownloading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val zoomPercent: Int = 100,
    val downloadState: DownloadState? = null,
    val pausedReason: DownloadPausedReason = DownloadPausedReason.NONE,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Int = -1,
    val speedBytesPerSec: Long = 0L
)

data class PdfDeepLinkRequest(
    val paperId: String,
    val pdfId: String,
    val title: String
)

class ImtihanViewModel(
    private val repository: CurriculumRepository = Stage1MockCurriculumRepository(),
    private val preferencesDataStore: UserPreferencesDataStore? = null
) : ViewModel() {

    private val _shellState = MutableStateFlow(AppShellState())
    val shellState: StateFlow<AppShellState> = _shellState.asStateFlow()

    private fun newSessionId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().take(8)}"

    private val _selectedSubjectId = MutableStateFlow("sub_kpk11_phy")
    val selectedSubjectId: StateFlow<String> = _selectedSubjectId.asStateFlow()

    private val _selectedChapterId = MutableStateFlow("ch_phy11_01")
    val selectedChapterId: StateFlow<String> = _selectedChapterId.asStateFlow()

    private val _paperSolvedFilter = MutableStateFlow<Boolean?>(null)
    val paperSolvedFilter: StateFlow<Boolean?> = _paperSolvedFilter.asStateFlow()

    private val _pdfCategoryFilter = MutableStateFlow<String?>(null)
    val pdfCategoryFilter: StateFlow<String?> = _pdfCategoryFilter.asStateFlow()

    private val _quizState = MutableStateFlow(QuizSessionState())
    val quizState: StateFlow<QuizSessionState> = _quizState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _examPatternsState = MutableStateFlow<List<ExamPatternUi>>(emptyList())
    val examPatternsState: StateFlow<List<ExamPatternUi>> = _examPatternsState.asStateFlow()

    private val _tutorState = MutableStateFlow(TutorResponseUi(available = true, isLoading = false))
    val tutorState: StateFlow<TutorResponseUi> = _tutorState.asStateFlow()

    private val _feedbackStatusMessage = MutableStateFlow<String?>(null)
    val feedbackStatusMessage: StateFlow<String?> = _feedbackStatusMessage.asStateFlow()

    private val _pdfViewerState = MutableStateFlow(PdfViewerUiState())
    val pdfViewerState: StateFlow<PdfViewerUiState> = _pdfViewerState.asStateFlow()

    private val _pendingPdfDeepLink = MutableStateFlow<PdfDeepLinkRequest?>(null)
    val pendingPdfDeepLink: StateFlow<PdfDeepLinkRequest?> = _pendingPdfDeepLink.asStateFlow()

    private val _activePdfId = MutableStateFlow("")
    private val _activePdfFallbackPages = MutableStateFlow(1)
    private val _refreshTick = MutableStateFlow(0)

    fun refreshCatalog() {
        _refreshTick.update { it + 1 }
        loadExamPatterns()
    }

    init {
        preferencesDataStore?.let { store ->
            viewModelScope.launch {
                store.preferencesFlow.collect { prefs ->
                    _shellState.update { current ->
                        current.copy(
                            hasCompletedSetup = prefs.hasCompletedSetup,
                            isDarkMode = prefs.isDarkMode,
                            isOfflineSimulated = prefs.offlineOnlyMode,
                            selectedClassLevel = prefs.selectedClassLevel,
                            selectedBoardCode = prefs.selectedBoardCode,
                            selectedSubBoardCode = prefs.selectedSubBoardCode,
                            studyGroupCode = prefs.studyGroupCode,
                            languageCode = prefs.languageCode,
                            notificationsEnabled = prefs.notificationsEnabled,
                            dailyTargetMinutes = prefs.dailyTargetMinutes,
                            examCountdownDateIso = prefs.examCountdownDateIso,
                            reminderHourLocal = prefs.reminderHourLocal,
                            diagnosticCompleted = prefs.diagnosticCompleted,
                            leaderboardOptIn = prefs.leaderboardOptIn,
                            leaderboardAlias = prefs.leaderboardAlias,
                            configuredApiBaseUrl = prefs.configuredApiBaseUrl,
                            userEmail = prefs.userEmail,
                            lastSyncedAtIso = prefs.lastSyncedAtIso,
                            soundEffectsEnabled = prefs.soundEffectsEnabled,
                            hapticsEnabled = prefs.hapticsEnabled,
                            readerHighContrastMode = prefs.readerHighContrastMode,
                            allowDownloadOverMobileData = prefs.allowDownloadOverMobileData,
                            profile = current.profile.copy(
                                displayName = prefs.displayName,
                                isGuest = !prefs.isGoogleLinked,
                                selectedClass = prefs.selectedClassLevel,
                                selectedBoard = prefs.selectedBoardCode,
                                selectedSubBoard = prefs.selectedSubBoardCode,
                                studyGroup = prefs.studyGroupCode,
                                dailyTargetMinutes = prefs.dailyTargetMinutes,
                                examCountdownDateIso = prefs.examCountdownDateIso
                            )
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.recoverDownloadQueueOnStartup()
        }

        @Suppress("OPT_IN_USAGE")
        viewModelScope.launch {
            combine(
                _activePdfId,
                repository.observeDownloadTasks()
            ) { activeId, tasks ->
                activeId to tasks.firstOrNull { it.pdfId == activeId }
            }.collect { (activeId, task) ->
                if (activeId.isBlank() || task == null) return@collect
                val currentViewer = _pdfViewerState.value
                if (currentViewer.pdfId != activeId) return@collect
                when (task.state) {
                    DownloadState.COMPLETED -> {
                        val path = task.localPath
                        if (!path.isNullOrBlank() && java.io.File(path).exists()) {
                            val savedState = repository.getPdfReadingState(activeId)
                            val upper = maxOf(1, _activePdfFallbackPages.value, savedState.totalPages, savedState.lastPage)
                            val restoredPage = savedState.lastPage.coerceIn(1, upper)
                            val restoredZoom = savedState.zoomPercent.coerceIn(100, 400)
                            _pdfViewerState.update {
                                it.copy(
                                    localFilePath = path,
                                    isDownloading = false,
                                    errorMessage = null,
                                    currentPage = restoredPage,
                                    zoomPercent = restoredZoom,
                                    downloadState = DownloadState.COMPLETED,
                                    pausedReason = DownloadPausedReason.NONE,
                                    bytesDownloaded = task.bytesDownloaded,
                                    totalBytes = task.totalBytes,
                                    progressPercent = 100,
                                    speedBytesPerSec = 0L
                                )
                            }
                        }
                    }
                    DownloadState.FAILED -> {
                        _pdfViewerState.update {
                            it.copy(
                                localFilePath = null,
                                isDownloading = false,
                                errorMessage = task.errorMessage ?: "Unable to download PDF.",
                                downloadState = DownloadState.FAILED,
                                pausedReason = task.pausedReason,
                                bytesDownloaded = task.bytesDownloaded,
                                totalBytes = task.totalBytes,
                                progressPercent = task.progressPercent,
                                speedBytesPerSec = 0L
                            )
                        }
                    }
                    DownloadState.CANCELLED -> {
                        _pdfViewerState.update {
                            it.copy(
                                localFilePath = null,
                                isDownloading = false,
                                errorMessage = "Download cancelled.",
                                downloadState = DownloadState.CANCELLED,
                                pausedReason = DownloadPausedReason.NONE,
                                bytesDownloaded = 0L,
                                totalBytes = task.totalBytes,
                                progressPercent = 0,
                                speedBytesPerSec = 0L
                            )
                        }
                    }
                    DownloadState.QUEUED,
                    DownloadState.RUNNING,
                    DownloadState.VERIFYING,
                    DownloadState.PAUSED -> {
                        if (currentViewer.localFilePath == null) {
                            _pdfViewerState.update {
                                it.copy(
                                    isDownloading = true,
                                    errorMessage = task.errorMessage,
                                    downloadState = task.state,
                                    pausedReason = task.pausedReason,
                                    bytesDownloaded = task.bytesDownloaded,
                                    totalBytes = task.totalBytes,
                                    progressPercent = task.progressPercent,
                                    speedBytesPerSec = task.speedBytesPerSec
                                )
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.observePendingSyncCount().collect { count ->
                _shellState.update { it.copy(pendingSyncCount = count) }
            }
        }

        @Suppress("OPT_IN_USAGE")
        viewModelScope.launch {
            _shellState
                .map { it.selectedClassLevel to it.selectedBoardCode }
                .distinctUntilChanged()
                .flatMapLatest { (cls, board) -> repository.observeHomeMetrics(cls, board) }
                .collect { metrics ->
                    _shellState.update { current ->
                        current.copy(
                            realHomeMetrics = metrics,
                            profile = current.profile.copy(
                                studyHours = metrics.studyHours,
                                chaptersCompleted = metrics.chaptersCompleted,
                                overallProgressPercent = metrics.overallProgressPercent,
                                dailyGoalPercent = metrics.dailyGoalPercent,
                                xpTotal = metrics.xpTotal,
                                currentStreakDays = metrics.currentStreakDays
                            )
                        )
                    }
                }
        }

        viewModelScope.launch {
            val (policy, tutorReady) = repository.fetchPublicAdPolicy()
            _shellState.update { it.copy(adPolicy = policy, aiTutorConfigured = tutorReady) }
        }

        loadExamPatterns()
    }

    @Suppress("OPT_IN_USAGE")
    val subjectsState: StateFlow<UiResourceState<List<SubjectUiModel>>> = combine(
        _shellState.map { Triple(it.selectedClassLevel, it.selectedBoardCode, it.isOfflineSimulated to it.studyGroupCode) }.distinctUntilChanged(),
        _refreshTick
    ) { key, _ -> key }
        .flatMapLatest { (classLevel, boardCode, _) ->
            repository.observeSubjects(classLevel, boardCode)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val chaptersState: StateFlow<UiResourceState<List<ChapterUiModel>>> = combine(
        _selectedSubjectId,
        _shellState.map { it.isOfflineSimulated }.distinctUntilChanged(),
        _refreshTick
    ) { subjectId, _, _ -> subjectId }
        .flatMapLatest { subjectId -> repository.observeChapters(subjectId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val detailedNotesState: StateFlow<UiResourceState<List<ContentBlockUi>>> = combine(
        _selectedChapterId,
        _refreshTick
    ) { chId, _ -> chId }
        .flatMapLatest { chId -> repository.observeChapterBlocks(chId, revisionOnly = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val revisionNotesState: StateFlow<UiResourceState<List<ContentBlockUi>>> = combine(
        _selectedChapterId,
        _refreshTick
    ) { chId, _ -> chId }
        .flatMapLatest { chId -> repository.observeChapterBlocks(chId, revisionOnly = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val solvedQuestionsState: StateFlow<UiResourceState<List<ContentBlockUi>>> = combine(
        _selectedChapterId,
        _refreshTick
    ) { chId, _ -> chId }
        .flatMapLatest { chId -> repository.observeSolvedQuestions(chId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val mcqsState: StateFlow<UiResourceState<List<McqQuestionUi>>> = combine(
        _selectedChapterId,
        _refreshTick
    ) { chId, _ -> chId }
        .flatMapLatest { chId -> repository.observeMcqsForChapter(chId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val pastPapersState: StateFlow<UiResourceState<List<PastPaperUi>>> = combine(
        _shellState.map { it.selectedClassLevel to it.isOfflineSimulated }.distinctUntilChanged(),
        _paperSolvedFilter,
        _refreshTick
    ) { (classLevel, _), filter, _ ->
        classLevel to filter
    }
        .flatMapLatest { (classLevel, filter) ->
            repository.observePastPapers(classLevel, filter)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val pdfLibraryState: StateFlow<List<PdfResourceUi>> = combine(
        _shellState.map { it.selectedClassLevel to it.isOfflineSimulated }.distinctUntilChanged(),
        _pdfCategoryFilter
    ) { (cls, _), category -> cls to category }
        .flatMapLatest { (cls, category) ->
            repository.observePdfLibrary(cls, category)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @Suppress("OPT_IN_USAGE")
    val searchResultsState: StateFlow<SearchResultsUi> = combine(
        _searchQuery,
        _shellState.map { it.selectedClassLevel to it.selectedBoardCode }.distinctUntilChanged()
    ) { q, (cls, board) -> Triple(q, cls, board) }
        .flatMapLatest { (q, cls, board) ->
            if (q.trim().length < 2) {
                flowOf(SearchResultsUi(q))
            } else {
                kotlinx.coroutines.flow.flow {
                    emit(repository.searchCurriculum(q, cls, board))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResultsUi())

    val downloadsState: StateFlow<List<DownloadedFileUi>> = repository.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloadTasksState: StateFlow<List<DownloadTaskUi>> = repository.observeDownloadTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val quizHistoryState: StateFlow<List<QuizHistoryItemUi>> = repository.observeQuizHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarksState: StateFlow<List<StudentBookmarkUi>> = repository.observeStudentBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mistakesState: StateFlow<List<MistakeRecordUi>> = repository.observeMistakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val flashcardsState: StateFlow<List<FlashcardUi>> = repository.observeFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @Suppress("OPT_IN_USAGE")
    val smartInsightsState: StateFlow<SmartInsightsUi> = _shellState
        .map { it.selectedClassLevel to it.selectedBoardCode }
        .distinctUntilChanged()
        .flatMapLatest { (cls, board) -> repository.observeSmartInsights(cls, board) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
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

    val studyPlannerState: StateFlow<StudyPlannerUi> = repository.observeStudyPlanner()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyPlannerUi())

    @Suppress("OPT_IN_USAGE")
    val motivationState: StateFlow<MotivationSummaryUi> = _shellState
        .map { it.selectedClassLevel to it.selectedBoardCode }
        .distinctUntilChanged()
        .flatMapLatest { (cls, board) -> repository.observeMotivationSummary(cls, board) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MotivationSummaryUi())

    @Suppress("OPT_IN_USAGE")
    val pdfBookmarksState: StateFlow<List<PdfBookmarkUi>> = _activePdfId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList()) else repository.observePdfBookmarks(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @Suppress("OPT_IN_USAGE")
    val pdfAnnotationsState: StateFlow<List<PdfAnnotationUi>> = _activePdfId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList()) else repository.observePdfAnnotations(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun completeFirstTimeSetup(
        classLevel: Int,
        boardCode: String,
        studentName: String,
        asGoogleUser: Boolean,
        subBoardCode: String = "PESHAWAR",
        studyGroupCode: String = Stage3To8Engines.defaultStudyGroupForClass(classLevel),
        languageCode: String = "English"
    ) {
        val resolvedName = if (studentName.isBlank()) "Guest Learner" else studentName.trim()
        _shellState.update { current ->
            current.copy(
                hasCompletedSetup = true,
                selectedClassLevel = classLevel,
                selectedBoardCode = boardCode,
                selectedSubBoardCode = subBoardCode,
                studyGroupCode = studyGroupCode,
                languageCode = languageCode,
                profile = current.profile.copy(
                    displayName = resolvedName,
                    isGuest = !asGoogleUser,
                    selectedClass = classLevel,
                    selectedBoard = boardCode,
                    selectedSubBoard = subBoardCode,
                    studyGroup = studyGroupCode
                )
            )
        }
        if (classLevel == 11) {
            _selectedSubjectId.value = "sub_kpk11_phy"
            _selectedChapterId.value = "ch_phy11_01"
        } else if (classLevel == 9) {
            _selectedSubjectId.value = "sub_kpk9_phy"
            _selectedChapterId.value = "ch_phy9_01"
        } else if (classLevel == 10) {
            _selectedSubjectId.value = "sub_kpk10_phy"
            _selectedChapterId.value = "ch_phy10_01"
        }

        viewModelScope.launch {
            preferencesDataStore?.completeFirstTimeSetup(
                classLevel = classLevel,
                boardCode = boardCode,
                displayName = resolvedName,
                isGoogleLinked = asGoogleUser,
                subBoardCode = subBoardCode,
                studyGroupCode = studyGroupCode,
                languageCode = languageCode
            )
            repository.initGuestOrGoogleSession(null)
            loadExamPatterns()
        }
    }

    fun selectClassAndBoard(
        classLevel: Int,
        boardCode: String,
        subBoardCode: String = _shellState.value.selectedSubBoardCode,
        studyGroupCode: String = Stage3To8Engines.defaultStudyGroupForClass(classLevel)
    ) {
        _shellState.update { current ->
            current.copy(
                selectedClassLevel = classLevel,
                selectedBoardCode = boardCode,
                selectedSubBoardCode = subBoardCode,
                studyGroupCode = studyGroupCode,
                profile = current.profile.copy(
                    selectedClass = classLevel,
                    selectedBoard = boardCode,
                    selectedSubBoard = subBoardCode,
                    studyGroup = studyGroupCode
                )
            )
        }
        if (classLevel == 11) {
            _selectedSubjectId.value = "sub_kpk11_phy"
            _selectedChapterId.value = "ch_phy11_01"
        } else if (classLevel == 9) {
            _selectedSubjectId.value = "sub_kpk9_phy"
            _selectedChapterId.value = "ch_phy9_01"
        } else if (classLevel == 10) {
            _selectedSubjectId.value = "sub_kpk10_phy"
            _selectedChapterId.value = "ch_phy10_01"
        }
        viewModelScope.launch {
            preferencesDataStore?.setClassAndBoard(classLevel, boardCode, subBoardCode, studyGroupCode)
            loadExamPatterns()
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _shellState.update { it.copy(isDarkMode = enabled) }
        viewModelScope.launch {
            preferencesDataStore?.setDarkMode(enabled)
        }
    }

    fun toggleOfflineSimulated(enabled: Boolean) {
        _shellState.update { it.copy(isOfflineSimulated = enabled) }
        viewModelScope.launch {
            preferencesDataStore?.setOfflineOnlyMode(enabled)
        }
    }

    fun setLanguage(language: String) {
        _shellState.update { it.copy(languageCode = language) }
        viewModelScope.launch {
            preferencesDataStore?.setLanguage(language)
        }
    }

    fun toggleStudyMedium() {
        val next = if (_shellState.value.languageCode.equals("Urdu", ignoreCase = true)) "English" else "Urdu"
        setLanguage(next)
    }

    fun toggleSoundEffects(enabled: Boolean = !_shellState.value.soundEffectsEnabled) {
        _shellState.update { it.copy(soundEffectsEnabled = enabled) }
        viewModelScope.launch {
            preferencesDataStore?.setSoundEffectsEnabled(enabled)
        }
    }

    fun toggleHaptics(enabled: Boolean = !_shellState.value.hapticsEnabled) {
        _shellState.update { it.copy(hapticsEnabled = enabled) }
        viewModelScope.launch {
            preferencesDataStore?.setHapticsEnabled(enabled)
        }
    }

    fun setReaderHighContrastMode(mode: String) {
        val normalized = if (mode == "PAPER") "PAPER" else "OLED"
        _shellState.update { it.copy(readerHighContrastMode = normalized) }
        viewModelScope.launch {
            preferencesDataStore?.setReaderHighContrastMode(normalized)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _shellState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch {
            preferencesDataStore?.setNotificationsEnabled(enabled)
        }
    }

    fun setAllowDownloadOverMobileData(enabled: Boolean) {
        _shellState.update { it.copy(allowDownloadOverMobileData = enabled) }
        viewModelScope.launch {
            preferencesDataStore?.setAllowDownloadOverMobileData(enabled)
        }
    }

    fun updateApiBaseUrl(rawUrl: String): String? {
        val (valid, err) = ImtihanSyncRules.validateWorkerBaseUrl(rawUrl)
        if (!valid) return err
        val normalized = ApiConfig.normalizeBaseUrl(rawUrl)
        _shellState.update {
            it.copy(
                configuredApiBaseUrl = normalized,
                syncStatusMessage = "Worker URL updated. Tap Sync Now to test connection."
            )
        }
        viewModelScope.launch {
            preferencesDataStore?.setApiBaseUrl(normalized)
        }
        return null
    }

    fun syncNow() {
        viewModelScope.launch {
            _shellState.update { it.copy(isSyncing = true, syncStatusMessage = null) }
            val result = repository.syncWithCloud()
            repository.refreshQuizHistory()
            val (policy, tutorReady) = repository.fetchPublicAdPolicy()
            _shellState.update {
                it.copy(
                    isSyncing = false,
                    adPolicy = policy,
                    aiTutorConfigured = tutorReady,
                    syncStatusMessage = result.fold(
                        onSuccess = { msg -> msg },
                        onFailure = { err -> err.message ?: "Sync failed." }
                    )
                )
            }
        }
    }

    fun linkGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _shellState.update { it.copy(isSyncing = true, syncStatusMessage = null) }
            val res = repository.initGuestOrGoogleSession(idToken.trim())
            _shellState.update {
                it.copy(
                    isSyncing = false,
                    syncStatusMessage = res.fold(
                        onSuccess = { msg -> msg },
                        onFailure = { err -> err.message ?: "Google account linking failed." }
                    )
                )
            }
        }
    }

    fun unlinkGoogleToGuest() {
        viewModelScope.launch {
            preferencesDataStore?.clearGoogleSession()
            _shellState.update {
                it.copy(
                    userEmail = null,
                    syncStatusMessage = "Switched back to Guest mode. Local study data preserved.",
                    profile = it.profile.copy(isGuest = true)
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSubject(subjectId: String) {
        _selectedSubjectId.value = subjectId
    }

    fun selectChapter(chapterId: String) {
        _selectedChapterId.value = chapterId
        viewModelScope.launch {
            repository.recordChapterProgress(
                chapterId = chapterId,
                subjectId = _selectedSubjectId.value,
                completionPercent = 25,
                lastBlockIndex = 0
            )
        }
    }

    fun setPaperFilter(solved: Boolean?) {
        _paperSolvedFilter.value = solved
    }

    fun setPdfCategoryFilter(category: String?) {
        _pdfCategoryFilter.value = category
    }

    fun loadExamPatterns(subjectId: String? = null) {
        val state = _shellState.value
        viewModelScope.launch {
            _examPatternsState.value = repository.loadExamPatterns(state.selectedClassLevel, state.selectedBoardCode, subjectId)
        }
    }

    fun selectMcqOption(questionId: String, optionId: String) {
        _quizState.update { state ->
            state.copy(selectedAnswers = state.selectedAnswers + (questionId to optionId))
        }
    }

    fun toggleMarkForReview(questionId: String) {
        _quizState.update { state ->
            val next = if (questionId in state.markedForReviewIds) {
                state.markedForReviewIds - questionId
            } else {
                state.markedForReviewIds + questionId
            }
            state.copy(markedForReviewIds = next)
        }
    }

    fun jumpToQuestion(index: Int) {
        _quizState.update { it.copy(currentIndex = index.coerceAtLeast(0)) }
    }

    fun incrementQuizTimer() {
        _quizState.update { state ->
            state.copy(elapsedSeconds = state.elapsedSeconds + 1)
        }
    }

    fun startChapterQuiz(chapterId: String = _selectedChapterId.value, subjectId: String = _selectedSubjectId.value, title: String = "Chapter Practice Quiz") {
        _selectedSubjectId.value = subjectId
        _selectedChapterId.value = chapterId
        _quizState.value = QuizSessionState(
            chapterId = chapterId,
            subjectId = subjectId,
            sessionId = newSessionId("chapter"),
            title = title,
            mode = "chapter_test",
            customQuestions = null
        )
    }

    fun startGeneratedExam(
        mode: String,
        patternId: String? = null,
        subjectId: String? = null,
        chapterId: String? = null,
        topicTitle: String? = null,
        difficulty: String = "mixed",
        questionCount: Int = 10,
        onStarted: () -> Unit = {}
    ) {
        val shell = _shellState.value
        _quizState.update { it.copy(isLoadingSession = true, sessionError = null, latestResult = null) }
        viewModelScope.launch {
            val res = repository.startExamSession(
                mode = mode,
                boardCode = shell.selectedBoardCode,
                classLevel = shell.selectedClassLevel,
                patternId = patternId,
                subjectId = subjectId,
                chapterId = chapterId,
                topicTitle = topicTitle,
                difficulty = difficulty,
                questionCount = questionCount
            )
            res.fold(
                onSuccess = { loaded ->
                    _quizState.value = QuizSessionState(
                        chapterId = loaded.chapterId ?: "",
                        subjectId = loaded.subjectId,
                        sessionId = loaded.sessionId,
                        title = loaded.title,
                        mode = loaded.mode,
                        currentIndex = 0,
                        selectedAnswers = emptyMap(),
                        markedForReviewIds = emptySet(),
                        elapsedSeconds = 0,
                        durationMinutes = loaded.durationMinutes,
                        marksPerMcq = loaded.marksPerMcq,
                        negativeMarkingPenalty = loaded.negativeMarkingPenalty,
                        passPercentage = loaded.passPercentage,
                        customQuestions = loaded.mcqs,
                        subjectivePrompts = loaded.subjectivePrompts,
                        isLoadingSession = false,
                        sessionError = null
                    )
                    onStarted()
                },
                onFailure = { err ->
                    _quizState.update {
                        it.copy(isLoadingSession = false, sessionError = err.message ?: "Unable to generate exam session.")
                    }
                }
            )
        }
    }

    fun startDiagnosticAssessment(onStarted: () -> Unit = {}) {
        startGeneratedExam(
            mode = "diagnostic_test",
            questionCount = 8,
            onStarted = onStarted
        )
    }

    fun startFiveMinRapidFire(onStarted: () -> Unit = {}) {
        val shell = _shellState.value
        val classSubIds = com.imtihanpro.app.data.repository.MockSeedDataSource.subjects
            .filter { it.classLevel == shell.selectedClassLevel }
            .map { it.id }
            .toSet()
        val pool = com.imtihanpro.app.data.repository.MockSeedDataSource.mcqs
            .filter { it.subjectId in classSubIds || classSubIds.isEmpty() }
            .map { com.imtihanpro.app.data.mapper.CurriculumMappers.toMcqUi(it) }
        val picked = Stage3To8Engines.pickRapidFireQuestions(pool, count = 5)
        if (picked.isEmpty()) return
        _quizState.value = QuizSessionState(
            chapterId = picked.first().chapterId,
            subjectId = picked.first().subjectId,
            sessionId = newSessionId("rapid_fire"),
            title = "5-Min KPK Rapid Fire",
            mode = "rapid_fire",
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            markedForReviewIds = emptySet(),
            elapsedSeconds = 0,
            durationMinutes = 5,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.0,
            passPercentage = 60,
            customQuestions = picked
        )
        onStarted()
    }

    fun startMistakeRetryQuiz(mistakes: List<MistakeRecordUi>, onStarted: () -> Unit = {}) {
        if (mistakes.isEmpty()) return
        val questions = mistakes.map { m ->
            McqQuestionUi(
                id = m.mcqId,
                chapterId = m.chapterId,
                question = m.question,
                options = m.options.ifEmpty {
                    listOf(
                        McqOptionUi("A", "Option A"),
                        McqOptionUi("B", "Option B"),
                        McqOptionUi("C", "Option C"),
                        McqOptionUi("D", "Option D")
                    )
                },
                correctOptionId = m.correctOptionId,
                explanation = m.explanation,
                subjectId = m.subjectId,
                topicTitle = m.topicTitle
            )
        }
        _quizState.value = QuizSessionState(
            chapterId = questions.first().chapterId,
            subjectId = questions.first().subjectId,
            sessionId = newSessionId("mistake_retry"),
            title = "Mistake Master Drill",
            mode = "mistake_retry",
            customQuestions = questions,
            durationMinutes = 0,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.0,
            passPercentage = 70
        )
        onStarted()
    }

    fun nextQuestionOrSubmit(questions: List<McqQuestionUi>, onFinished: () -> Unit) {
        val current = _quizState.value
        if (current.currentIndex + 1 < questions.size) {
            _quizState.update { it.copy(currentIndex = it.currentIndex + 1) }
        } else {
            submitExamSession(questions, onFinished)
        }
    }

    fun submitExamSession(questions: List<McqQuestionUi>, onFinished: () -> Unit) {
        val current = _quizState.value
        if (current.isSubmitting || current.latestResult != null || questions.isEmpty()) return

        val requiresServerGrading = questions.any { it.correctOptionId == null }
        val immediateResult = if (requiresServerGrading) {
            null
        } else {
            Stage3To8Engines.gradeQuizWithRules(
                questions = questions,
                selectedAnswers = current.selectedAnswers,
                elapsedSeconds = current.elapsedSeconds,
                marksPerMcq = current.marksPerMcq,
                negativeMarkingPenalty = current.negativeMarkingPenalty,
                passPercentage = current.passPercentage
            ).copy(examMode = current.mode)
        }
        _quizState.update {
            it.copy(
                isSubmitting = true,
                sessionError = null,
                latestResult = immediateResult
            )
        }

        viewModelScope.launch {
            try {
                val subjectName = (subjectsState.value as? UiResourceState.Success)
                    ?.data?.firstOrNull { it.id == current.subjectId }?.name ?: "KPK Subject"
                val chapterTitle = (chaptersState.value as? UiResourceState.Success)
                    ?.data?.firstOrNull { it.id == current.chapterId }?.title
                val finalResult = repository.submitExamResult(
                    subjectId = current.subjectId.ifBlank { _selectedSubjectId.value },
                    subjectName = subjectName,
                    chapterId = current.chapterId.takeIf { it.isNotBlank() },
                    chapterTitle = chapterTitle,
                    examMode = current.mode,
                    questions = questions,
                    selectedAnswers = current.selectedAnswers,
                    elapsedSeconds = current.elapsedSeconds,
                    marksPerMcq = current.marksPerMcq,
                    negativeMarkingPenalty = current.negativeMarkingPenalty,
                    passPercentage = current.passPercentage,
                    sessionId = current.sessionId
                )
                _quizState.update { it.copy(latestResult = finalResult, isSubmitting = false, sessionError = null) }
                onFinished()
            } catch (err: Throwable) {
                _quizState.update {
                    it.copy(
                        isSubmitting = false,
                        sessionError = err.message ?: "Unable to submit exam result."
                    )
                }
            }
        }
    }

    fun resetQuiz() {
        _quizState.update { current ->
            current.copy(
                sessionId = newSessionId(current.mode.ifBlank { "chapter" }),
                currentIndex = 0,
                selectedAnswers = emptyMap(),
                markedForReviewIds = emptySet(),
                elapsedSeconds = 0,
                isLoadingSession = false,
                isSubmitting = false,
                sessionError = null,
                latestResult = null
            )
        }
    }

    fun toggleBookmark(
        itemType: String,
        itemId: String,
        chapterId: String?,
        subjectId: String?,
        title: String,
        subtitle: String
    ) {
        viewModelScope.launch {
            repository.toggleStudentBookmark(itemType, itemId, chapterId, subjectId, title, subtitle)
        }
    }

    fun resolveMistake(mcqId: String) {
        viewModelScope.launch {
            repository.resolveMistake(mcqId)
        }
    }

    fun reviewFlashcard(flashcardId: String, quality: Int) {
        viewModelScope.launch {
            repository.reviewFlashcard(flashcardId, quality)
        }
    }

    fun askAiTutor(
        action: String,
        prompt: String,
        chapterId: String? = _selectedChapterId.value,
        subjectId: String? = _selectedSubjectId.value
    ) {
        val shell = _shellState.value
        _tutorState.value = TutorResponseUi(available = true, isLoading = true)
        viewModelScope.launch {
            val res = repository.askAiTutor(
                chapterId = chapterId,
                subjectId = subjectId,
                classLevel = shell.selectedClassLevel,
                boardCode = shell.selectedBoardCode,
                languageCode = shell.languageCode,
                action = action,
                prompt = prompt
            )
            _tutorState.value = res
        }
    }

    fun clearTutorResponse() {
        _tutorState.value = TutorResponseUi(available = true, isLoading = false)
    }

    fun addStudyTask(
        title: String,
        plannedDateIso: String,
        estimatedMinutes: Int,
        taskType: String,
        subjectId: String? = _selectedSubjectId.value,
        chapterId: String? = null
    ) {
        viewModelScope.launch {
            repository.addStudyPlanTask(title, subjectId, chapterId, plannedDateIso, estimatedMinutes, taskType)
        }
    }

    fun toggleStudyTask(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleStudyPlanTask(taskId, completed)
        }
    }

    fun deleteStudyTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteStudyPlanTask(taskId)
        }
    }

    fun updateStudyGoals(dailyTargetMinutes: Int, examCountdownDateIso: String?, reminderHourLocal: Int) {
        viewModelScope.launch {
            repository.updateStudyGoalPreferences(dailyTargetMinutes, examCountdownDateIso, reminderHourLocal)
        }
    }

    fun setLeaderboardOptIn(optIn: Boolean, aliasHandle: String? = null) {
        viewModelScope.launch {
            repository.setLeaderboardOptIn(optIn, aliasHandle)
        }
    }

    fun submitFeedback(
        category: String,
        title: String,
        description: String,
        chapterId: String? = _selectedChapterId.value,
        subjectId: String? = _selectedSubjectId.value,
        mcqId: String? = null
    ) {
        viewModelScope.launch {
            val res = repository.submitFeedback(category, subjectId, chapterId, mcqId, title, description)
            _feedbackStatusMessage.value = res.fold(
                onSuccess = { it },
                onFailure = { it.message ?: "Unable to save feedback." }
            )
        }
    }

    fun clearFeedbackStatus() {
        _feedbackStatusMessage.value = null
    }

    fun recordBannerImpression() {
        _shellState.update { it.copy(bannersShownToday = it.bannersShownToday + 1) }
    }

    fun toggleDownloadChapter(chapterId: String, title: String, sizeMb: Double) {
        viewModelScope.launch {
            repository.toggleOfflineChapter(chapterId, title, sizeMb)
        }
    }

    fun toggleDownloadPaper(paperId: String, title: String, sizeMb: Double) {
        viewModelScope.launch {
            repository.toggleOfflinePaper(paperId, title, sizeMb)
        }
    }

    fun pausePdfDownload(pdfId: String) {
        viewModelScope.launch {
            repository.pausePdfDownload(pdfId)
        }
    }

    fun resumePdfDownload(pdfId: String) {
        viewModelScope.launch {
            repository.resumePdfDownload(pdfId)
        }
    }

    fun cancelPdfDownload(pdfId: String) {
        viewModelScope.launch {
            repository.cancelPdfDownload(pdfId)
        }
    }

    fun retryPdfDownload(pdfId: String) {
        viewModelScope.launch {
            repository.retryPdfDownload(pdfId)
        }
    }

    fun retryAllFailedDownloads() {
        viewModelScope.launch {
            repository.retryAllFailedDownloads()
        }
    }

    fun downloadOnMobileData(pdfId: String) {
        viewModelScope.launch {
            repository.downloadOnMobileData(pdfId)
        }
    }

    fun handleNotificationDeepLink(paperId: String, pdfId: String, title: String) {
        if (pdfId.isBlank() && paperId.isBlank()) return
        val resolvedPdf = pdfId.ifBlank { paperId }
        val resolvedPaper = paperId.ifBlank { resolvedPdf }
        openPdfForReading(resolvedPaper, resolvedPdf, title.ifBlank { "Offline PDF" }, 10)
        _pendingPdfDeepLink.value = PdfDeepLinkRequest(resolvedPaper, resolvedPdf, title.ifBlank { "Offline PDF" })
    }

    fun consumePendingPdfDeepLink() {
        _pendingPdfDeepLink.value = null
    }

    fun clearDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
        }
    }

    fun openPdfForReading(paperId: String, pdfId: String, title: String, totalPages: Int, sizeMb: Double = 0.0) {
        val targetId = pdfId.ifBlank { paperId }
        _activePdfId.value = targetId
        _activePdfFallbackPages.value = totalPages.coerceAtLeast(1)
        _pdfViewerState.value = PdfViewerUiState(
            paperId = paperId,
            pdfId = targetId,
            localFilePath = null,
            isDownloading = true,
            errorMessage = null,
            currentPage = 1,
            zoomPercent = 100,
            downloadState = DownloadState.QUEUED
        )
        viewModelScope.launch {
            val savedState = repository.getPdfReadingState(targetId)
            val effectiveUpperBound = maxOf(1, totalPages, savedState.totalPages, savedState.lastPage)
            val restoredPage = savedState.lastPage.coerceIn(1, effectiveUpperBound)
            val restoredZoom = savedState.zoomPercent.coerceIn(100, 400)
            val local = repository.findLocalPdfPath(paperId, targetId)
            if (local != null) {
                _pdfViewerState.update {
                    it.copy(
                        localFilePath = local,
                        isDownloading = false,
                        currentPage = restoredPage,
                        zoomPercent = restoredZoom,
                        downloadState = DownloadState.COMPLETED,
                        progressPercent = 100
                    )
                }
            } else {
                repository.enqueuePdfDownload(
                    paperId = paperId,
                    pdfId = targetId,
                    title = title,
                    categoryLabel = "PDF Document",
                    sizeMb = sizeMb
                )
            }
        }
    }

    fun updatePdfPosition(page: Int, totalPages: Int, zoomPercent: Int) {
        val pdfId = _activePdfId.value
        if (pdfId.isBlank()) return
        val clampedZoom = zoomPercent.coerceIn(100, 400)
        _pdfViewerState.update { it.copy(currentPage = page, zoomPercent = clampedZoom) }
        viewModelScope.launch {
            repository.savePdfReadingState(pdfId, page, totalPages, clampedZoom)
        }
    }

    fun toggleCurrentPdfBookmark(page: Int, label: String) {
        val pdfId = _activePdfId.value
        if (pdfId.isBlank()) return
        viewModelScope.launch {
            repository.togglePdfBookmark(pdfId, page, label)
        }
    }

    fun addPdfNote(page: Int, noteText: String) {
        val pdfId = _activePdfId.value
        if (pdfId.isBlank() || noteText.isBlank()) return
        viewModelScope.launch {
            repository.addPdfAnnotation(pdfId, page, noteText)
        }
    }

    fun deletePdfNote(annotationId: String) {
        viewModelScope.launch {
            repository.deletePdfAnnotation(annotationId)
        }
    }

    fun downloadSubjectOfflinePack(subjectId: String, subjectName: String) {
        viewModelScope.launch {
            _shellState.update { it.copy(offlinePackStatusMessage = "Downloading $subjectName offline pack…") }
            val res = repository.downloadSubjectOfflinePack(subjectId)
            val msg = res.fold(
                onSuccess = { count -> "$subjectName: $count chapter(s) + MCQs saved offline." },
                onFailure = { err -> err.message ?: "Unable to cache offline pack." }
            )
            _shellState.update { it.copy(offlinePackStatusMessage = msg) }
        }
    }

    fun clearOfflinePackStatus() {
        _shellState.update { it.copy(offlinePackStatusMessage = null) }
    }

    fun logFocusSession(minutes: Int, completed: Boolean, onXpEarned: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val xp = repository.logFocusPomodoroSession(minutes, completed)
            onXpEarned(xp)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val prefs = UserPreferencesDataStore(appContext)
                    val db = ImtihanDatabaseProvider.getInstance(appContext)
                    val repo = OfflineFirstCurriculumRepository(
                        context = appContext,
                        database = db,
                        preferencesDataStore = prefs,
                        apiClient = ImtihanApiClient.createWithCache(appContext.cacheDir)
                    )
                    return ImtihanViewModel(
                        repository = repo,
                        preferencesDataStore = prefs
                    ) as T
                }
            }
    }
}
