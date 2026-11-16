package com.imtihanpro.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.components.BottomNavTab
import com.imtihanpro.app.ui.components.ImtihanBottomNavBar
import com.imtihanpro.app.ui.screens.AiTutorScreen
import com.imtihanpro.app.ui.screens.BookmarksAndFlashcardsScreen
import com.imtihanpro.app.ui.screens.ChapterLearningWorkspaceScreen
import com.imtihanpro.app.ui.screens.ChapterListingScreen
import com.imtihanpro.app.ui.screens.DetailedNotesScreen
import com.imtihanpro.app.ui.screens.DownloadsScreen
import com.imtihanpro.app.ui.screens.FeedbackReportScreen
import com.imtihanpro.app.ui.screens.FirstTimeSetupScreen
import com.imtihanpro.app.ui.screens.HomeDashboardScreen
import com.imtihanpro.app.ui.screens.McqPracticeScreen
import com.imtihanpro.app.ui.screens.MistakeMasterScreen
import com.imtihanpro.app.ui.screens.MotivationAndLeaderboardScreen
import com.imtihanpro.app.ui.screens.PastPapersScreen
import com.imtihanpro.app.ui.screens.PdfReaderScreen
import com.imtihanpro.app.ui.screens.PracticeHubScreen
import com.imtihanpro.app.ui.screens.ProfileSettingsScreen
import com.imtihanpro.app.ui.screens.QuizHistoryScreen
import com.imtihanpro.app.ui.screens.QuizResultsScreen
import com.imtihanpro.app.ui.screens.RevisionNotesScreen
import com.imtihanpro.app.ui.screens.SmartInsightsScreen
import com.imtihanpro.app.ui.screens.SolvedQuestionsScreen
import com.imtihanpro.app.ui.screens.SplashScreen
import com.imtihanpro.app.ui.screens.StudyPlannerScreen
import com.imtihanpro.app.ui.screens.SubjectDetailsScreen
import com.imtihanpro.app.ui.screens.SubjectListingScreen
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.viewmodel.ImtihanViewModel

object Routes {
    const val SPLASH = "splash"
    const val SETUP = "setup"
    const val HOME = "home"
    const val SUBJECTS = "subjects"
    const val SUBJECT_DETAILS = "subject_details"
    const val CHAPTER_LIST = "chapter_list"
    const val WORKSPACE = "workspace"
    const val DETAILED_NOTES = "detailed_notes"
    const val REVISION_NOTES = "revision_notes"
    const val PRACTICE = "practice"
    const val QUIZ_SESSION = "quiz_session"
    const val QUIZ_RESULTS = "quiz_results"
    const val SOLVED_QUESTIONS = "solved_questions"
    const val PAPERS = "papers"
    const val PDF_READER = "pdf_reader/{paperId}/{pdfId}/{title}/{pages}"
    const val DOWNLOADS = "downloads"
    const val PROFILE = "profile"
    const val SMART_INSIGHTS = "smart_insights"
    const val MISTAKE_MASTER = "mistake_master"
    const val BOOKMARKS_FLASHCARDS = "bookmarks_flashcards"
    const val QUIZ_HISTORY = "quiz_history"
    const val AI_TUTOR = "ai_tutor"
    const val STUDY_PLANNER = "study_planner"
    const val MOTIVATION = "motivation"
    const val FEEDBACK = "feedback"

    fun pdfReader(paperId: String, pdfId: String, title: String, pages: Int): String =
        "pdf_reader/${Uri.encode(paperId)}/${Uri.encode(pdfId.ifBlank { paperId })}/${Uri.encode(title)}/$pages"
}

@Composable
fun ImtihanNavGraph(viewModel: ImtihanViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val shellState by viewModel.shellState.collectAsState()
    val subjectsState by viewModel.subjectsState.collectAsState()
    val chaptersState by viewModel.chaptersState.collectAsState()
    val detailedNotesState by viewModel.detailedNotesState.collectAsState()
    val revisionNotesState by viewModel.revisionNotesState.collectAsState()
    val solvedQuestionsState by viewModel.solvedQuestionsState.collectAsState()
    val mcqsState by viewModel.mcqsState.collectAsState()
    val pastPapersState by viewModel.pastPapersState.collectAsState()
    val pdfLibraryState by viewModel.pdfLibraryState.collectAsState()
    val pdfCategoryFilter by viewModel.pdfCategoryFilter.collectAsState()
    val downloadsState by viewModel.downloadsState.collectAsState()
    val pdfViewerState by viewModel.pdfViewerState.collectAsState()
    val pdfBookmarks by viewModel.pdfBookmarksState.collectAsState()
    val pdfAnnotations by viewModel.pdfAnnotationsState.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val selectedChapterId by viewModel.selectedChapterId.collectAsState()
    val paperSolvedFilter by viewModel.paperSolvedFilter.collectAsState()
    val quizState by viewModel.quizState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResultsState.collectAsState()
    val examPatterns by viewModel.examPatternsState.collectAsState()
    val quizHistory by viewModel.quizHistoryState.collectAsState()
    val bookmarks by viewModel.bookmarksState.collectAsState()
    val mistakes by viewModel.mistakesState.collectAsState()
    val flashcards by viewModel.flashcardsState.collectAsState()
    val smartInsights by viewModel.smartInsightsState.collectAsState()
    val studyPlanner by viewModel.studyPlannerState.collectAsState()
    val motivation by viewModel.motivationState.collectAsState()
    val tutorState by viewModel.tutorState.collectAsState()
    val feedbackStatus by viewModel.feedbackStatusMessage.collectAsState()

    val colors = LocalImtihanColors.current

    val bottomBarRoutes = setOf(
        Routes.HOME,
        Routes.SUBJECTS,
        Routes.PRACTICE,
        Routes.PAPERS,
        Routes.PROFILE
    )

    val selectedSubject = (subjectsState as? UiResourceState.Success)
        ?.data
        ?.firstOrNull { it.id == selectedSubjectId }
    val selectedChapter = (chaptersState as? UiResourceState.Success)
        ?.data
        ?.firstOrNull { it.id == selectedChapterId }

    val isChapterBookmarked = bookmarks.any {
        it.itemType == "chapter" && it.itemId == selectedChapterId
    }

    Scaffold(
        containerColor = colors.canvasDeep,
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                ImtihanBottomNavBar(
                    currentRoute = currentRoute,
                    onSelectTab = { tab: BottomNavTab ->
                        navController.navigate(tab.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        val dest = if (shellState.hasCompletedSetup) Routes.HOME else Routes.SETUP
                        navController.navigate(dest) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SETUP) {
                FirstTimeSetupScreen(
                    initialClass = shellState.selectedClassLevel,
                    initialBoard = shellState.selectedBoardCode,
                    initialSubBoard = shellState.selectedSubBoardCode,
                    initialStudyGroup = shellState.studyGroupCode,
                    initialLanguage = shellState.languageCode,
                    initialName = if (shellState.hasCompletedSetup) shellState.profile.displayName else "",
                    canNavigateBack = shellState.hasCompletedSetup,
                    onBack = { navController.popBackStack() },
                    onStartAsGuest = { cls, board, subBoard, group, lang, name, launchDiagnostic ->
                        viewModel.completeFirstTimeSetup(
                            classLevel = cls,
                            boardCode = board,
                            studentName = name,
                            asGoogleUser = false,
                            subBoardCode = subBoard,
                            studyGroupCode = group,
                            languageCode = lang
                        )
                        if (launchDiagnostic) {
                            viewModel.startDiagnosticAssessment {
                                navController.navigate(Routes.QUIZ_SESSION) {
                                    popUpTo(Routes.SETUP) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.SETUP) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onContinueWithGoogle = { cls, board, subBoard, group, lang, name ->
                        viewModel.completeFirstTimeSetup(
                            classLevel = cls,
                            boardCode = board,
                            studentName = name,
                            asGoogleUser = false,
                            subBoardCode = subBoard,
                            studyGroupCode = group,
                            languageCode = lang
                        )
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(Routes.SETUP) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeDashboardScreen(
                    shellState = shellState,
                    subjectsState = subjectsState,
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    continueChapterTitle = selectedChapter?.let {
                        "Chapter ${it.chapterNumber}: ${it.title}"
                    } ?: "Chapter 1: Physical Quantities & Measurement",
                    onSearchChange = viewModel::updateSearchQuery,
                    onContinueLearning = { navController.navigate(Routes.WORKSPACE) },
                    onViewAllSubjects = { navController.navigate(Routes.SUBJECTS) },
                    onOpenSubject = { subject ->
                        viewModel.selectSubject(subject.id)
                        navController.navigate(Routes.SUBJECT_DETAILS)
                    },
                    onOpenSearchedChapter = { chapter ->
                        viewModel.selectSubject(chapter.subjectId)
                        viewModel.selectChapter(chapter.id)
                        navController.navigate(Routes.WORKSPACE)
                    },
                    onOpenSearchedPdf = { pdf ->
                        viewModel.openPdfForReading(pdf.id, pdf.id, pdf.title, pdf.pageCount)
                        navController.navigate(Routes.pdfReader(pdf.id, pdf.id, pdf.title, pdf.pageCount))
                    },
                    onOpenMcqPractice = { navController.navigate(Routes.PRACTICE) },
                    onOpenAiTutor = { navController.navigate(Routes.AI_TUTOR) },
                    onOpenSmartInsights = { navController.navigate(Routes.SMART_INSIGHTS) },
                    onOpenStudyPlanner = { navController.navigate(Routes.STUDY_PLANNER) },
                    onOpenMistakeMaster = { navController.navigate(Routes.MISTAKE_MASTER) },
                    onOpenBookmarksFlashcards = { navController.navigate(Routes.BOOKMARKS_FLASHCARDS) },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    onBannerDisplayed = viewModel::recordBannerImpression
                )
            }

            composable(Routes.SUBJECTS) {
                SubjectListingScreen(
                    classLevel = shellState.selectedClassLevel,
                    studyGroupCode = shellState.studyGroupCode,
                    subjectsState = subjectsState,
                    onSelectSubject = { subject ->
                        viewModel.selectSubject(subject.id)
                        navController.navigate(Routes.SUBJECT_DETAILS)
                    },
                    onOpenMotivation = { navController.navigate(Routes.MOTIVATION) }
                )
            }

            composable(Routes.SUBJECT_DETAILS) {
                val subPdfs = pdfLibraryState.filter { it.subjectId == selectedSubjectId }
                SubjectDetailsScreen(
                    subject = selectedSubject,
                    chaptersState = chaptersState,
                    subjectPdfs = subPdfs,
                    onBack = { navController.popBackStack() },
                    onOpenChapter = { chapter ->
                        viewModel.selectChapter(chapter.id)
                        navController.navigate(Routes.WORKSPACE)
                    },
                    onOpenAllChapters = { navController.navigate(Routes.CHAPTER_LIST) },
                    onOpenPastPapers = { navController.navigate(Routes.PAPERS) },
                    onOpenPdf = { pdf ->
                        viewModel.openPdfForReading(pdf.id, pdf.id, pdf.title, pdf.pageCount)
                        navController.navigate(Routes.pdfReader(pdf.id, pdf.id, pdf.title, pdf.pageCount))
                    },
                    onStartSubjectMock = {
                        viewModel.startGeneratedExam(
                            mode = "subject_test",
                            subjectId = selectedSubjectId,
                            questionCount = 10
                        ) {
                            navController.navigate(Routes.QUIZ_SESSION)
                        }
                    }
                )
            }

            composable(Routes.CHAPTER_LIST) {
                ChapterListingScreen(
                    subjectTitle = selectedSubject?.name ?: "Subject",
                    chaptersState = chaptersState,
                    onBack = { navController.popBackStack() },
                    onSelectChapter = { chapter ->
                        viewModel.selectChapter(chapter.id)
                        navController.navigate(Routes.WORKSPACE)
                    }
                )
            }

            composable(Routes.WORKSPACE) {
                ChapterLearningWorkspaceScreen(
                    chapter = selectedChapter,
                    isBookmarked = isChapterBookmarked,
                    onBack = { navController.popBackStack() },
                    onOpenDetailedNotes = { navController.navigate(Routes.DETAILED_NOTES) },
                    onOpenRevisionNotes = { navController.navigate(Routes.REVISION_NOTES) },
                    onOpenMcqPractice = {
                        viewModel.startChapterQuiz(
                            chapterId = selectedChapterId,
                            subjectId = selectedSubjectId,
                            title = selectedChapter?.title ?: "Chapter Practice Quiz"
                        )
                        navController.navigate(Routes.QUIZ_SESSION)
                    },
                    onOpenSolvedQuestions = { navController.navigate(Routes.SOLVED_QUESTIONS) },
                    onStartTopicPractice = { topicTitle ->
                        viewModel.startGeneratedExam(
                            mode = "topic_test",
                            subjectId = selectedSubjectId,
                            chapterId = selectedChapterId,
                            topicTitle = topicTitle,
                            questionCount = 5
                        ) {
                            navController.navigate(Routes.QUIZ_SESSION)
                        }
                    },
                    onToggleBookmark = {
                        val ch = selectedChapter
                        viewModel.toggleBookmark(
                            itemType = "chapter",
                            itemId = selectedChapterId,
                            chapterId = selectedChapterId,
                            subjectId = selectedSubjectId,
                            title = ch?.title ?: "Bookmarked Chapter",
                            subtitle = "${selectedSubject?.name ?: "KPK"} · Ch ${ch?.chapterNumber ?: 1}"
                        )
                    },
                    onAskAiTutor = { navController.navigate(Routes.AI_TUTOR) },
                    onReportIssue = { navController.navigate(Routes.FEEDBACK) },
                    onToggleOfflineDownload = {
                        val ch = selectedChapter
                        if (ch != null) {
                            viewModel.toggleDownloadChapter(
                                ch.id,
                                ch.title,
                                ch.estimatedSizeKb / 1024.0
                            )
                        }
                    }
                )
            }

            composable(Routes.DETAILED_NOTES) {
                DetailedNotesScreen(
                    chapterTitle = selectedChapter?.title ?: "Detailed Notes",
                    blocksState = detailedNotesState,
                    isBookmarked = isChapterBookmarked,
                    onToggleBookmark = {
                        val ch = selectedChapter
                        viewModel.toggleBookmark(
                            itemType = "chapter",
                            itemId = selectedChapterId,
                            chapterId = selectedChapterId,
                            subjectId = selectedSubjectId,
                            title = ch?.title ?: "Bookmarked Notes",
                            subtitle = "Detailed Notes · ${selectedSubject?.name ?: "KPK"}"
                        )
                    },
                    onAskAiTutor = { navController.navigate(Routes.AI_TUTOR) },
                    onReportIssue = { navController.navigate(Routes.FEEDBACK) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.REVISION_NOTES) {
                RevisionNotesScreen(
                    chapterTitle = selectedChapter?.title ?: "Revision Notes",
                    blocksState = revisionNotesState,
                    isBookmarked = isChapterBookmarked,
                    onToggleBookmark = {
                        val ch = selectedChapter
                        viewModel.toggleBookmark(
                            itemType = "chapter",
                            itemId = selectedChapterId,
                            chapterId = selectedChapterId,
                            subjectId = selectedSubjectId,
                            title = ch?.title ?: "Revision Notes",
                            subtitle = "Revision Sheet · ${selectedSubject?.name ?: "KPK"}"
                        )
                    },
                    onAskAiTutor = { navController.navigate(Routes.AI_TUTOR) },
                    onReportIssue = { navController.navigate(Routes.FEEDBACK) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PRACTICE) {
                PracticeHubScreen(
                    classLevel = shellState.selectedClassLevel,
                    diagnosticCompleted = shellState.diagnosticCompleted,
                    activeMistakesCount = mistakes.size,
                    dueFlashcardsCount = flashcards.count { it.isDue },
                    subjectsState = subjectsState,
                    examPatterns = examPatterns,
                    sessionError = quizState.sessionError,
                    onStartChapterQuiz = {
                        viewModel.startChapterQuiz(
                            chapterId = selectedChapterId,
                            subjectId = selectedSubjectId,
                            title = selectedChapter?.title ?: "Chapter Practice Quiz"
                        )
                        navController.navigate(Routes.QUIZ_SESSION)
                    },
                    onStartPatternExam = { pattern ->
                        viewModel.startGeneratedExam(
                            mode = pattern.mode,
                            patternId = pattern.id,
                            subjectId = pattern.subjectId,
                            questionCount = pattern.questionCount
                        ) {
                            navController.navigate(Routes.QUIZ_SESSION)
                        }
                    },
                    onStartSubjectDrill = { subId, diff ->
                        viewModel.startGeneratedExam(
                            mode = "subject_test",
                            subjectId = subId,
                            difficulty = diff,
                            questionCount = 10
                        ) {
                            navController.navigate(Routes.QUIZ_SESSION)
                        }
                    },
                    onStartDiagnostic = {
                        viewModel.startDiagnosticAssessment {
                            navController.navigate(Routes.QUIZ_SESSION)
                        }
                    },
                    onOpenMistakeMaster = { navController.navigate(Routes.MISTAKE_MASTER) },
                    onOpenFlashcards = { navController.navigate(Routes.BOOKMARKS_FLASHCARDS) },
                    onOpenHistory = { navController.navigate(Routes.QUIZ_HISTORY) },
                    onOpenInsights = { navController.navigate(Routes.SMART_INSIGHTS) }
                )
            }

            composable(Routes.QUIZ_SESSION) {
                McqPracticeScreen(
                    mcqsState = mcqsState,
                    sessionState = quizState,
                    onBack = { navController.popBackStack() },
                    onSelectOption = viewModel::selectMcqOption,
                    onToggleMarkForReview = viewModel::toggleMarkForReview,
                    onJumpToQuestion = viewModel::jumpToQuestion,
                    onTickTimer = viewModel::incrementQuizTimer,
                    onNextOrSubmit = { list ->
                        viewModel.nextQuestionOrSubmit(list) {
                            navController.navigate(Routes.QUIZ_RESULTS)
                        }
                    },
                    onForceSubmit = { list ->
                        viewModel.submitExamSession(list) {
                            navController.navigate(Routes.QUIZ_RESULTS)
                        }
                    },
                    onReportQuestion = { _ -> navController.navigate(Routes.FEEDBACK) }
                )
            }

            composable(Routes.QUIZ_RESULTS) {
                QuizResultsScreen(
                    result = quizState.latestResult,
                    onBackToHome = {
                        viewModel.resetQuiz()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onRetakeQuiz = {
                        viewModel.resetQuiz()
                        navController.popBackStack()
                    },
                    onOpenMistakeMaster = { navController.navigate(Routes.MISTAKE_MASTER) }
                )
            }

            composable(Routes.SOLVED_QUESTIONS) {
                SolvedQuestionsScreen(
                    chapterTitle = selectedChapter?.title ?: "Solved Questions",
                    blocksState = solvedQuestionsState,
                    isBookmarked = isChapterBookmarked,
                    onToggleBookmark = {
                        val ch = selectedChapter
                        viewModel.toggleBookmark(
                            itemType = "chapter",
                            itemId = selectedChapterId,
                            chapterId = selectedChapterId,
                            subjectId = selectedSubjectId,
                            title = ch?.title ?: "Solved Questions",
                            subtitle = "Board Solutions · ${selectedSubject?.name ?: "KPK"}"
                        )
                    },
                    onAskAiTutor = { navController.navigate(Routes.AI_TUTOR) },
                    onReportIssue = { navController.navigate(Routes.FEEDBACK) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PAPERS) {
                PastPapersScreen(
                    papersState = pastPapersState,
                    pdfLibrary = pdfLibraryState,
                    solvedFilter = paperSolvedFilter,
                    selectedCategory = pdfCategoryFilter,
                    adPolicy = shellState.adPolicy,
                    bannersShownToday = shellState.bannersShownToday,
                    onSetFilter = viewModel::setPaperFilter,
                    onSelectCategory = viewModel::setPdfCategoryFilter,
                    onOpenPaper = { paper ->
                        val title = "${paper.subjectName} — ${paper.year}"
                        val pdfId = paper.pdfMetadataId.ifBlank { paper.id }
                        viewModel.openPdfForReading(paper.id, pdfId, title, paper.pageCount)
                        navController.navigate(Routes.pdfReader(paper.id, pdfId, title, paper.pageCount))
                    },
                    onOpenPdfResource = { pdf ->
                        viewModel.openPdfForReading(pdf.id, pdf.id, pdf.title, pdf.pageCount)
                        navController.navigate(Routes.pdfReader(pdf.id, pdf.id, pdf.title, pdf.pageCount))
                    },
                    onToggleDownloadPaper = { paper ->
                        viewModel.toggleDownloadPaper(
                            paper.pdfMetadataId.ifBlank { paper.id },
                            "${paper.subjectName} Past Paper ${paper.year}",
                            paper.fileSizeMb
                        )
                    },
                    onToggleDownloadPdf = { pdf ->
                        viewModel.toggleDownloadPaper(pdf.id, pdf.title, pdf.sizeMb)
                    },
                    onBannerDisplayed = viewModel::recordBannerImpression
                )
            }

            composable(
                route = Routes.PDF_READER,
                arguments = listOf(
                    navArgument("paperId") { type = NavType.StringType },
                    navArgument("pdfId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("pages") { type = NavType.IntType }
                )
            ) { entry ->
                val paperId = entry.arguments?.getString("paperId")?.let { Uri.decode(it) } ?: ""
                val pdfId = entry.arguments?.getString("pdfId")?.let { Uri.decode(it) } ?: paperId
                val title = entry.arguments?.getString("title")?.let { Uri.decode(it) } ?: "Past Paper PDF"
                val pages = entry.arguments?.getInt("pages") ?: 10
                PdfReaderScreen(
                    paperTitle = title,
                    fallbackTotalPages = pages,
                    viewerState = pdfViewerState,
                    bookmarks = pdfBookmarks,
                    annotations = pdfAnnotations,
                    onRetryDownload = { viewModel.openPdfForReading(paperId, pdfId, title, pages) },
                    onUpdatePosition = viewModel::updatePdfPosition,
                    onToggleBookmark = viewModel::toggleCurrentPdfBookmark,
                    onAddAnnotation = viewModel::addPdfNote,
                    onDeleteAnnotation = viewModel::deletePdfNote,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.DOWNLOADS) {
                DownloadsScreen(
                    downloads = downloadsState,
                    onBack = { navController.popBackStack() },
                    onClearAll = viewModel::clearDownloads
                )
            }

            composable(Routes.PROFILE) {
                ProfileSettingsScreen(
                    shellState = shellState,
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    onToggleOfflineSimulated = viewModel::toggleOfflineSimulated,
                    onToggleNotifications = viewModel::setNotificationsEnabled,
                    onCycleLanguage = {
                        val next = if (shellState.languageCode == "English") "Urdu" else "English"
                        viewModel.setLanguage(next)
                    },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onChangeClassOrBoard = { navController.navigate(Routes.SETUP) },
                    onOpenStudyPlanner = { navController.navigate(Routes.STUDY_PLANNER) },
                    onOpenMotivation = { navController.navigate(Routes.MOTIVATION) },
                    onOpenSmartInsights = { navController.navigate(Routes.SMART_INSIGHTS) },
                    onOpenBookmarksFlashcards = { navController.navigate(Routes.BOOKMARKS_FLASHCARDS) },
                    onOpenQuizHistory = { navController.navigate(Routes.QUIZ_HISTORY) },
                    onOpenFeedback = { navController.navigate(Routes.FEEDBACK) },
                    onSyncNow = viewModel::syncNow,
                    onUpdateApiBaseUrl = viewModel::updateApiBaseUrl,
                    onLinkGoogleToken = viewModel::linkGoogleIdToken,
                    onUnlinkGoogle = viewModel::unlinkGoogleToGuest
                )
            }

            composable(Routes.SMART_INSIGHTS) {
                SmartInsightsScreen(
                    insights = smartInsights,
                    onBack = { navController.popBackStack() },
                    onStartDiagnostic = {
                        viewModel.startDiagnosticAssessment {
                            navController.navigate(Routes.QUIZ_SESSION)
                        }
                    },
                    onOpenChapterNotes = { chId, subId ->
                        viewModel.selectSubject(subId)
                        viewModel.selectChapter(chId)
                        navController.navigate(Routes.WORKSPACE)
                    },
                    onStartChapterQuiz = { chId, subId ->
                        viewModel.startChapterQuiz(chId, subId, "Revision Quiz")
                        navController.navigate(Routes.QUIZ_SESSION)
                    },
                    onOpenMistakeMaster = { navController.navigate(Routes.MISTAKE_MASTER) },
                    onOpenFlashcards = { navController.navigate(Routes.BOOKMARKS_FLASHCARDS) }
                )
            }

            composable(Routes.MISTAKE_MASTER) {
                MistakeMasterScreen(
                    mistakes = mistakes,
                    onBack = { navController.popBackStack() },
                    onResolveMistake = viewModel::resolveMistake,
                    onPracticeMistakes = { list ->
                        viewModel.startMistakeRetryQuiz(list) {
                            navController.navigate(Routes.QUIZ_SESSION)
                        }
                    }
                )
            }

            composable(Routes.BOOKMARKS_FLASHCARDS) {
                BookmarksAndFlashcardsScreen(
                    bookmarks = bookmarks,
                    flashcards = flashcards,
                    onBack = { navController.popBackStack() },
                    onOpenBookmark = { bm ->
                        if (!bm.subjectId.isNullOrBlank()) viewModel.selectSubject(bm.subjectId)
                        if (!bm.chapterId.isNullOrBlank()) {
                            viewModel.selectChapter(bm.chapterId)
                            navController.navigate(Routes.WORKSPACE)
                        }
                    },
                    onRemoveBookmark = { bm ->
                        viewModel.toggleBookmark(bm.itemType, bm.itemId, bm.chapterId, bm.subjectId, bm.title, bm.subtitle)
                    },
                    onReviewFlashcard = viewModel::reviewFlashcard
                )
            }

            composable(Routes.QUIZ_HISTORY) {
                QuizHistoryScreen(
                    history = quizHistory,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AI_TUTOR) {
                AiTutorScreen(
                    chapterTitle = selectedChapter?.title,
                    classLevel = shellState.selectedClassLevel,
                    boardCode = shellState.selectedBoardCode,
                    languageCode = shellState.languageCode,
                    tutorState = tutorState,
                    onAskTutor = { action, prompt ->
                        viewModel.askAiTutor(action, prompt, selectedChapterId, selectedSubjectId)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.STUDY_PLANNER) {
                StudyPlannerScreen(
                    planner = studyPlanner,
                    onAddTask = { title, dateIso, mins, type ->
                        viewModel.addStudyTask(title, dateIso, mins, type, selectedSubjectId, selectedChapterId)
                    },
                    onToggleTask = viewModel::toggleStudyTask,
                    onDeleteTask = viewModel::deleteStudyTask,
                    onUpdateGoals = viewModel::updateStudyGoals,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MOTIVATION) {
                MotivationAndLeaderboardScreen(
                    motivation = motivation,
                    onToggleLeaderboardOptIn = viewModel::setLeaderboardOptIn,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.FEEDBACK) {
                FeedbackReportScreen(
                    subjectId = selectedSubjectId,
                    chapterId = selectedChapterId,
                    statusMessage = feedbackStatus,
                    onSubmitFeedback = { cat, title, desc ->
                        viewModel.submitFeedback(cat, title, desc, selectedChapterId, selectedSubjectId, null)
                    },
                    onClearStatus = viewModel::clearFeedbackStatus,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
