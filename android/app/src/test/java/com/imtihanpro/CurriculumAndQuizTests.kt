package com.imtihanpro.app

import com.imtihanpro.app.data.mapper.CurriculumMappers
import com.imtihanpro.app.data.remote.ApiContentBlockDto
import com.imtihanpro.app.data.remote.ApiPastPaperDto
import com.imtihanpro.app.data.remote.ApiSubjectDto
import com.imtihanpro.app.data.repository.MockSeedDataSource
import com.imtihanpro.app.data.repository.Stage1MockCurriculumRepository
import com.imtihanpro.app.domain.model.ContentBlockUi
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurriculumAndQuizTests {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `subject mapper calculates accurate percentage`() {
        val dto = ApiSubjectDto(
            id = "sub_test",
            classLevel = 10,
            boardCode = "KPK",
            code = "PHY-10",
            name = "Physics",
            iconKey = "physics",
            totalChapters = 10,
            publishedChapters = 3,
            coverageStatus = "active_seed"
        )
        val ui = CurriculumMappers.toSubjectUi(dto, completedChapters = 7)
        assertEquals(70, ui.progressPercent)
        assertTrue(ui.hasSeedContent)
    }

    @Test
    fun `past paper mapper preserves numeric sizeMb and formatted label`() {
        val dto = ApiPastPaperDto(
            id = "pp_test",
            boardCode = "KPK",
            classLevel = 10,
            subjectId = "sub_kpk10_phy",
            subjectName = "Physics",
            year = 2023,
            isSolved = true,
            pdfMetadataId = "pdf_test",
            fileSizeMb = 2.4,
            pageCount = 11
        )
        val ui = CurriculumMappers.toPastPaperUi(dto, downloadedIds = setOf("pp_test"))
        assertEquals(2.4, ui.fileSizeMb, 0.001)
        assertEquals("2.4 MB", ui.fileSizeLabel)
        assertTrue(ui.isDownloaded)
    }

    @Test
    fun `quiz grading computes exact score and percentage`() {
        val repo = Stage1MockCurriculumRepository()
        val questions = MockSeedDataSource.mcqs
            .filter { it.chapterId == "ch_phy10_01" }
            .map { CurriculumMappers.toMcqUi(it) }
        val answers = mapOf(
            "mcq_1" to "A", // correct
            "mcq_2" to "B", // correct
            "mcq_3" to "A", // wrong (C is correct)
            "mcq_4" to "B"  // correct
            // mcq_5 unanswered
        )
        val result = repo.gradeQuiz(questions, answers, elapsedSeconds = 125)
        assertEquals(5, result.totalQuestions)
        assertEquals(3, result.correctCount)
        assertEquals(2, result.incorrectCount)
        assertEquals(60, result.percentage)
        assertEquals("02:05", result.elapsedTimeFormatted)
    }

    @Test
    fun `content block mapper converts math expression cleanly`() {
        val dto = ApiContentBlockDto.MathExpressionDto(
            id = "m1",
            orderIndex = 1,
            latex = "F = ma",
            plainNotation = "F = m × a",
            caption = "Newton's second law"
        )
        val mapped = CurriculumMappers.toContentBlockUi(dto)
        assertTrue(mapped is ContentBlockUi.MathExpression)
        assertEquals("F = m × a", (mapped as ContentBlockUi.MathExpression).plainNotation)
    }

    @Test
    fun `changing class level reactively updates pastPapersState`() = runTest {
        val viewModel = ImtihanViewModel(
            repository = Stage1MockCurriculumRepository(),
            preferencesDataStore = null
        )
        val collectorJob = backgroundScope.launch {
            viewModel.pastPapersState.collect {}
        }

        val class10State = viewModel.pastPapersState.first { it !is UiResourceState.Loading }
        assertTrue(class10State is UiResourceState.Success)

        viewModel.selectClassAndBoard(classLevel = 11, boardCode = "KPK")
        val class11State = viewModel.pastPapersState.first { it is UiResourceState.Empty }
        assertTrue(class11State is UiResourceState.Empty)

        collectorJob.cancel()
    }
}
