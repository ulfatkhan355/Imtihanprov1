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
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.AdPlacementPolicyUi
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.ContentBlockUi
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

data class AppShellState(
    val hasCompletedSetup: Boolean = false,
    val isDarkMode: Boolean = true,
    val isOfflineSimulated: Boolean = false,
    val selectedClassLevel: Int = 9,
    val selectedBoardCode: String = "KPK",
    val selectedSubBoardCode: String = "PESHAWAR",
    val studyGroupCode: String = "SCIENCE_BIOLOGY",
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
    val profile: StudentProfileUi = StudentProfileUi(
        displayName = "Guest Learner",
        isGuest = true,
        selectedClass = 9,
        selectedBoard = "KPK",
        studyHours = 0,
        chaptersCompleted = 0,
        overallProgressPercent = 0,
        dailyGoalPercent = 0,
        selectedSubBoard = "PESHAWAR",
        studyGroup = "SCIENCE_BIOLOGY"
    )
)

data class QuizSessionState(
    val chapterId: String = "ch_phy9_01",
    val subjectId: String = "sub_kpk9_phy",
    val sessionId: String = "chapter_session",
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
    val zoomPercent: Int = 100
)

class ImtihanViewModel(
    private val repository: CurriculumRepository = Stage1MockCurriculumRepository(),
    private val preferencesDataStore: UserPreferencesDataStore? = null
) : ViewModel() {

    private val _shellState = MutableStateFlow(AppShellState())
    val shellState: StateFlow<AppShellState> = _shellState.asStateFlow()

    private val _selectedSubjectId = MutableStateFlow("sub_kpk9_phy")
    val selectedSubjectId: StateFlow<String> = _selectedSubjectId.asStateFlow()

    private val _selectedChapterId = MutableStateFlow("ch_phy9_01")
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

    private val _activePdfId = MutableStateFlow("")

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
    val subjectsState: StateFlow<UiResourceState<List<SubjectUiModel>>> = _shellState
        .map { Triple(it.selectedClassLevel, it.selectedBoardCode, it.isOfflineSimulated to it.studyGroupCode) }
        .distinctUntilChanged()
        .flatMapLatest { (classLevel, boardCode, _) ->
            repository.observeSubjects(classLevel, boardCode)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val chaptersState: StateFlow<UiResourceState<List<ChapterUiModel>>> = combine(
        _selectedSubjectId,
        _shellState.map { it.isOfflineSimulated }.distinctUntilChanged()
    ) { subjectId, _ -> subjectId }
        .flatMapLatest { subjectId -> repository.observeChapters(subjectId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val detailedNotesState: StateFlow<UiResourceState<List<ContentBlockUi>>> = _selectedChapterId
        .flatMapLatest { chId -> repository.observeChapterBlocks(chId, revisionOnly = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val revisionNotesState: StateFlow<UiResourceState<List<ContentBlockUi>>> = _selectedChapterId
        .flatMapLatest { chId -> repository.observeChapterBlocks(chId, revisionOnly = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val solvedQuestionsState: StateFlow<UiResourceState<List<ContentBlockUi>>> = _selectedChapterId
        .flatMapLatest { chId -> repository.observeSolvedQuestions(chId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val mcqsState: StateFlow<UiResourceState<List<McqQuestionUi>>> = _selectedChapterId
        .flatMapLatest { chId -> repository.observeMcqsForChapter(chId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResourceState.Loading)

    @Suppress("OPT_IN_USAGE")
    val pastPapersState: StateFlow<UiResourceState<List<PastPaperUi>>> = combine(
        _shellState.map { it.selectedClassLevel to it.isOfflineSimulated }.distinctUntilChanged(),
        _paperSolvedFilter
    ) { (classLevel, _), filter ->
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
        if (classLevel == 9) {
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
        if (classLevel == 9) {
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

    fun setNotificationsEnabled(enabled: Boolean) {
        _shellState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch {
            preferencesDataStore?.setNotificationsEnabled(enabled)
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
            sessionId = "mistake_retry",
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
        val immediateResult = Stage3To8Engines.gradeQuizWithRules(
            questions = questions,
            selectedAnswers = current.selectedAnswers,
            elapsedSeconds = current.elapsedSeconds,
            marksPerMcq = current.marksPerMcq,
            negativeMarkingPenalty = current.negativeMarkingPenalty,
            passPercentage = current.passPercentage
        ).copy(examMode = current.mode)
        _quizState.update { it.copy(latestResult = immediateResult) }

        viewModelScope.launch {
            val subjectName = (subjectsState.value as? UiResourceState.Success)
                ?.data?.firstOrNull { it.id == current.subjectId }?.name ?: "KPK Subject"
            val chapterTitle = (chaptersState.value as? UiResourceState.Success)
                ?.data?.firstOrNull { it.id == current.chapterId }?.title
            repository.submitExamResult(
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
                passPercentage = current.passPercentage
            )
        }
        onFinished()
    }

    fun resetQuiz() {
        _quizState.value = QuizSessionState(
            chapterId = _selectedChapterId.value,
            subjectId = _selectedSubjectId.value,
            elapsedSeconds = 0
        )
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

    fun clearDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
        }
    }

    fun openPdfForReading(paperId: String, pdfId: String, title: String, totalPages: Int) {
        val targetId = pdfId.ifBlank { paperId }
        _activePdfId.value = targetId
        _pdfViewerState.value = PdfViewerUiState(
            paperId = paperId,
            pdfId = targetId,
            localFilePath = null,
            isDownloading = true,
            errorMessage = null,
            currentPage = 1,
            zoomPercent = 100
        )
        viewModelScope.launch {
            val savedState = repository.getPdfReadingState(targetId)
            val local = repository.findLocalPdfPath(paperId, targetId)
            if (local != null) {
                _pdfViewerState.update {
                    it.copy(
                        localFilePath = local,
                        isDownloading = false,
                        currentPage = savedState.lastPage.coerceIn(1, maxOf(1, totalPages)),
                        zoomPercent = savedState.zoomPercent
                    )
                }
            } else {
                val downloaded = repository.getOrDownloadPdfFile(paperId, targetId, title, 0.0)
                _pdfViewerState.update {
                    downloaded.fold(
                        onSuccess = { path ->
                            it.copy(
                                localFilePath = path,
                                isDownloading = false,
                                errorMessage = null,
                                currentPage = savedState.lastPage.coerceIn(1, maxOf(1, totalPages)),
                                zoomPercent = savedState.zoomPercent
                            )
                        },
                        onFailure = { err ->
                            it.copy(
                                localFilePath = null,
                                isDownloading = false,
                                errorMessage = err.message ?: "Unable to fetch PDF from Worker/B2.",
                                currentPage = savedState.lastPage.coerceIn(1, maxOf(1, totalPages)),
                                zoomPercent = savedState.zoomPercent
                            )
                        }
                    )
                }
            }
        }
    }

    fun updatePdfPosition(page: Int, totalPages: Int, zoomPercent: Int) {
        val pdfId = _activePdfId.value
        if (pdfId.isBlank()) return
        _pdfViewerState.update { it.copy(currentPage = page, zoomPercent = zoomPercent) }
        viewModelScope.launch {
            repository.savePdfReadingState(pdfId, page, totalPages, zoomPercent)
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
