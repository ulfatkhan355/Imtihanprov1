package com.imtihanpro.app

import com.imtihanpro.app.data.mapper.CurriculumMappers
import com.imtihanpro.app.data.remote.ApiResult
import com.imtihanpro.app.data.remote.ImtihanApiClient
import com.imtihanpro.app.data.repository.MockSeedDataSource
import com.imtihanpro.app.data.repository.Stage1MockCurriculumRepository
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.AdPlacementPolicyUi
import com.imtihanpro.app.domain.model.ChapterProgressStatus
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.McqOptionUi
import com.imtihanpro.app.domain.model.McqQuestionUi
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.viewmodel.ImtihanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Stage3To8EngineTests {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var client: ImtihanApiClient

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        server = MockWebServer()
        server.start()
        client = ImtihanApiClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun `api client rejects ok=false envelope cleanly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"ok":false,"error":{"code":"BAD_INPUT","message":"Rejected envelope"}}""")
        )
        val res = client.fetchSubjects(server.url("/v1").toString(), 9, "KPK")
        assertTrue(res is ApiResult.Failure)
        assertTrue((res as ApiResult.Failure).message.contains("Rejected envelope"))
    }

    @Test
    fun `api client parses student progress with localProfileId and asks tutor honestly on 503`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "ok": true,
                      "data": {
                        "localProfileId": "loc_99",
                        "guestDeviceId": "dev_99",
                        "selectedClass": 9,
                        "selectedBoard": "KPK",
                        "selectedSubBoard": "PESHAWAR",
                        "studyGroup": "SCIENCE_BIOLOGY",
                        "language": "en",
                        "studyHoursTotal": 1.5,
                        "dailyGoalPercent": 60,
                        "chapterProgress": [],
                        "recentQuizzes": []
                      }
                    }
                    """.trimIndent()
                )
        )
        val progressRes = client.fetchStudentProgress(server.url("/v1").toString(), "dev_99", null)
        assertTrue(progressRes is ApiResult.Success)
        assertEquals("loc_99", (progressRes as ApiResult.Success).data.localProfileId)
        assertEquals("PESHAWAR", progressRes.data.selectedSubBoard)

        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"ok":false,"error":{"code":"TUTOR_UNCONFIGURED","message":"GEMINI_API_KEY not configured"}}""")
        )
        val tutorRes = client.askTutor(
            baseUrl = server.url("/v1").toString(),
            guestDeviceId = "dev_99",
            bearerToken = null,
            chapterId = "ch_phy9_01",
            subjectId = "sub_kpk9_phy",
            classLevel = 9,
            boardCode = "KPK",
            language = "en",
            action = "explain_simple",
            prompt = "Explain Vernier Callipers"
        )
        assertTrue(tutorRes is ApiResult.Failure)
        assertEquals(503, (tutorRes as ApiResult.Failure).httpStatus)
    }

    @Test
    fun `gradeQuizWithRules computes negative marking, pass fail, topic breakdown and XP`() {
        val questions = listOf(
            McqQuestionUi("q1", "ch_1", "Q1", listOf(McqOptionUi("A", "1"), McqOptionUi("B", "2")), "A", "Exp", topicTitle = "Units"),
            McqQuestionUi("q2", "ch_1", "Q2", listOf(McqOptionUi("A", "1"), McqOptionUi("B", "2")), "B", "Exp", topicTitle = "Units"),
            McqQuestionUi("q3", "ch_1", "Q3", listOf(McqOptionUi("A", "1"), McqOptionUi("B", "2")), "A", "Exp", topicTitle = "Errors"),
            McqQuestionUi("q4", "ch_1", "Q4", listOf(McqOptionUi("A", "1"), McqOptionUi("B", "2")), "A", "Exp", topicTitle = "Errors")
        )
        val answers = mapOf(
            "q1" to "A", // +1.0
            "q2" to "A", // -0.25 (wrong)
            "q3" to "A"  // +1.0
            // q4 skipped -> 0.0
        )
        val result = Stage3To8Engines.gradeQuizWithRules(
            questions = questions,
            selectedAnswers = answers,
            elapsedSeconds = 95,
            marksPerMcq = 1.0,
            negativeMarkingPenalty = 0.25,
            passPercentage = 40
        )
        assertEquals(2, result.correctCount)
        assertEquals(1, result.incorrectCount)
        assertEquals(1, result.unansweredCount)
        assertEquals(1.75, result.marksObtained, 0.001)
        assertEquals(4.0, result.totalMarks, 0.001)
        assertEquals(44, result.percentage)
        assertTrue(result.passed)
        assertEquals(2, result.topicBreakdown.size)
        assertEquals(20, result.xpEarned)
    }

    @Test
    fun `calculateSm2NextReview scales intervals and resets on poor recall`() {
        val rep1 = Stage3To8Engines.calculateSm2NextReview(1, 2.5, 0, quality = 4, nowEpochMs = 1_000L)
        assertEquals(1, rep1.repetitions)
        assertEquals(1, rep1.intervalDays)

        val rep2 = Stage3To8Engines.calculateSm2NextReview(rep1.intervalDays, rep1.easeFactor, rep1.repetitions, quality = 5, nowEpochMs = 1_000L)
        assertEquals(2, rep2.repetitions)
        assertEquals(6, rep2.intervalDays)

        val rep3 = Stage3To8Engines.calculateSm2NextReview(rep2.intervalDays, rep2.easeFactor, rep2.repetitions, quality = 4, nowEpochMs = 1_000L)
        assertEquals(3, rep3.repetitions)
        assertTrue(rep3.intervalDays >= 12)

        val reset = Stage3To8Engines.calculateSm2NextReview(rep3.intervalDays, rep3.easeFactor, rep3.repetitions, quality = 1, nowEpochMs = 1_000L)
        assertEquals(0, reset.repetitions)
        assertEquals(1, reset.intervalDays)
    }

    @Test
    fun `computeHonestExamReadiness returns null when insufficient data and calculates when ready`() {
        val empty = Stage3To8Engines.computeHonestExamReadiness(0, 0, null, 0, 5, 0)
        assertNull(empty.readinessPercent)
        assertEquals("insufficient_data", empty.status)

        val oneAttempt = Stage3To8Engines.computeHonestExamReadiness(1, 5, 80, 1, 5, 0)
        assertNull(oneAttempt.readinessPercent)

        val ready = Stage3To8Engines.computeHonestExamReadiness(3, 15, 85, 3, 5, 1)
        assertNotNull(ready.readinessPercent)
        assertTrue(ready.readinessPercent!! >= 55)
    }

    @Test
    fun `detectWeakChapters prioritizes low accuracy and high mistakes`() {
        val chapters = listOf(
            ChapterUiModel("ch_1", "sub_1", 1, "Measurement", "", "v1", ChapterProgressStatus.IN_PROGRESS, 5, 2, 300, false),
            ChapterUiModel("ch_2", "sub_1", 2, "Kinematics", "", "v1", ChapterProgressStatus.AVAILABLE, 5, 2, 300, false)
        )
        val weak = Stage3To8Engines.detectWeakChapters(
            chapters = chapters,
            subjectNamesById = mapOf("sub_1" to "Physics"),
            quizAccuracyByChapterId = mapOf("ch_1" to (2 to 42), "ch_2" to (2 to 65)),
            activeMistakesByChapterId = mapOf("ch_1" to 3, "ch_2" to 1),
            completionByChapterId = mapOf("ch_1" to 40, "ch_2" to 80)
        )
        assertEquals(2, weak.size)
        assertEquals("ch_1", weak[0].chapterId)
        assertEquals("high", weak[0].priority)
        assertEquals("medium", weak[1].priority)
    }

    @Test
    fun `shouldDisplayAd strictly blocks ads during exams, notes reading, and daily caps`() {
        val enabledPolicy = AdPlacementPolicyUi(
            enabled = true,
            bannerOnHome = true,
            bannerOnPapers = true,
            suppressDuringQuizAndExam = true,
            suppressDuringNotesReading = true,
            dailyBannerCap = 2
        )
        assertTrue(Stage3To8Engines.shouldDisplayAd(enabledPolicy, "home", isInActiveQuizOrExam = false, isReadingNotesOrPdf = false, bannersShownToday = 0))
        assertFalse(Stage3To8Engines.shouldDisplayAd(enabledPolicy, "home", isInActiveQuizOrExam = true, isReadingNotesOrPdf = false, bannersShownToday = 0))
        assertFalse(Stage3To8Engines.shouldDisplayAd(enabledPolicy, "papers", isInActiveQuizOrExam = false, isReadingNotesOrPdf = true, bannersShownToday = 0))
        assertFalse(Stage3To8Engines.shouldDisplayAd(enabledPolicy, "home", isInActiveQuizOrExam = false, isReadingNotesOrPdf = false, bannersShownToday = 2))
    }

    @Test
    fun `class 9 KPK primary seed subjects and dynamic study groups are present`() = runTest {
        assertEquals(8, Stage3To8Engines.KPK_SUB_BOARDS.size)
        assertTrue(Stage3To8Engines.studyGroupsForClass(9).any { it.code == "SCIENCE_BIOLOGY" })
        assertTrue(Stage3To8Engines.studyGroupsForClass(11).any { it.code == "PRE_ENGINEERING" })
        assertTrue(MockSeedDataSource.subjects.any { it.id == "sub_kpk9_phy" && it.classLevel == 9 })
        assertTrue(MockSeedDataSource.detailedNotesBlocks.containsKey("ch_phy9_01"))

        val vm = ImtihanViewModel(Stage1MockCurriculumRepository(), null)
        assertEquals(9, vm.shellState.value.selectedClassLevel)
        assertEquals(0, vm.shellState.value.profile.overallProgressPercent)
        val job = backgroundScope.launch { vm.subjectsState.collect {} }
        val state = vm.subjectsState.first { it !is UiResourceState.Loading }
        assertTrue(state is UiResourceState.Success)
        assertTrue((state as UiResourceState.Success).data.any { it.id == "sub_kpk9_phy" })
        job.cancel()
    }
}
