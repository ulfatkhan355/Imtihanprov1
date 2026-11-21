package com.imtihanpro.app

import com.imtihanpro.app.data.repository.Stage1MockCurriculumRepository
import com.imtihanpro.app.domain.model.McqOptionUi
import com.imtihanpro.app.domain.model.McqQuestionUi
import com.imtihanpro.app.domain.model.MistakeRecordUi
import com.imtihanpro.app.ui.components.latexToHtmlFragment
import com.imtihanpro.app.ui.viewmodel.ImtihanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QaAuditRegressionTests {

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
    fun `latexToHtmlFragment renders matrices without splitting &amp; and escapes HTML safely`() {
        val html = latexToHtmlFragment("\\begin{bmatrix} 1 & 2 \\\\ 3 & <4> \\end{bmatrix} + \\frac{1}{2}")
        assertTrue(html.contains("matrix-grid"))
        assertTrue(html.contains("grid-template-columns:repeat(2, auto)"))
        assertTrue(html.contains("<span>1</span><span>2</span>"))
        assertTrue(html.contains("<span>3</span><span>&lt;4&gt;</span>"))
        assertFalse(html.contains("amp;"))
        assertTrue(html.contains("<span class=\"frac\">"))
    }

    @Test
    fun `quiz starts generate unique sessionIds, guard double submit, and resetQuiz preserves retake mode`() = runTest {
        val vm = ImtihanViewModel(Stage1MockCurriculumRepository(), null)

        vm.startChapterQuiz("ch_phy11_01", "sub_kpk11_phy", "Ch 1 Quiz")
        val id1 = vm.quizState.value.sessionId
        vm.startChapterQuiz("ch_phy11_01", "sub_kpk11_phy", "Ch 1 Quiz")
        val id2 = vm.quizState.value.sessionId
        assertNotEquals(id1, id2)

        vm.startFiveMinRapidFire()
        val rapidId = vm.quizState.value.sessionId
        assertEquals("rapid_fire", vm.quizState.value.mode)
        assertEquals(5, vm.quizState.value.durationMinutes)
        assertNotNull(vm.quizState.value.customQuestions)

        val sampleQuestions = listOf(
            McqQuestionUi("q1", "ch_1", "Q1", listOf(McqOptionUi("A", "Yes"), McqOptionUi("B", "No")), "A", "Exp")
        )
        vm.selectMcqOption("q1", "A")
        var finishedCalls = 0
        vm.submitExamSession(sampleQuestions) { finishedCalls += 1 }
        // Second call while result already exists must be ignored
        vm.submitExamSession(sampleQuestions) { finishedCalls += 1 }
        assertEquals(1, finishedCalls)
        assertNotNull(vm.quizState.value.latestResult)
        assertFalse(vm.quizState.value.isSubmitting)

        // Retake preserves rapid_fire config and generates a fresh unique sessionId
        vm.resetQuiz()
        assertNull(vm.quizState.value.latestResult)
        assertEquals("rapid_fire", vm.quizState.value.mode)
        assertEquals(5, vm.quizState.value.durationMinutes)
        assertNotNull(vm.quizState.value.customQuestions)
        assertTrue(vm.quizState.value.selectedAnswers.isEmpty())
        assertNotEquals(rapidId, vm.quizState.value.sessionId)

        val mistakes = listOf(
            MistakeRecordUi("m1", "sub_kpk11_phy", "ch_phy11_01", "Units", "Q?", listOf(McqOptionUi("A", "1")), "B", "A", "Exp", 1, false)
        )
        vm.startMistakeRetryQuiz(mistakes)
        assertEquals("mistake_retry", vm.quizState.value.mode)
        assertNotEquals("mistake_retry", vm.quizState.value.sessionId)
    }
}
