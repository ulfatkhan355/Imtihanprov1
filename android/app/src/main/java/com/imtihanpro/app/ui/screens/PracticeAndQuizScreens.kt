package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.model.ExamPatternUi
import com.imtihanpro.app.domain.model.McqQuestionUi
import com.imtihanpro.app.domain.model.QuizResultUi
import com.imtihanpro.app.domain.model.SubjectUiModel
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.audio.ImtihanFeedbackController
import com.imtihanpro.app.ui.components.AnimatedProgressRing
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanErrorView
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.components.ZeroPillMetaLine
import com.imtihanpro.app.ui.share.ShareCardGenerator
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.urduTextStyle
import com.imtihanpro.app.ui.viewmodel.QuizSessionState
import kotlinx.coroutines.delay

/**
 * Practice Hub (Tab 3: Chapter quizzes, KPK BISE Mock Exams, Diagnostic Test, Negative-Marking Drills, Mistake Master, Flashcards)
 */
@Composable
fun PracticeHubScreen(
    classLevel: Int,
    diagnosticCompleted: Boolean,
    activeMistakesCount: Int,
    dueFlashcardsCount: Int,
    subjectsState: UiResourceState<List<SubjectUiModel>>,
    examPatterns: List<ExamPatternUi>,
    sessionError: String?,
    onStartChapterQuiz: () -> Unit,
    onStartPatternExam: (ExamPatternUi) -> Unit,
    onStartSubjectDrill: (subjectId: String, difficulty: String) -> Unit,
    onStartDiagnostic: () -> Unit,
    onOpenMistakeMaster: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenInsights: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var selectedDifficulty by remember { mutableStateOf("mixed") }
    val subjects = (subjectsState as? UiResourceState.Success)?.data.orEmpty()

    SilverBlueCanvas {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Exam & Practice Arena",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = "KPK Class $classLevel MCQs, Mock Board Papers & Spaced Drills",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            if (!sessionError.isNullOrBlank()) {
                item {
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Text(sessionError, style = MaterialTheme.typography.bodySmall, color = colors.danger)
                    }
                }
            }

            item {
                SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (diagnosticCompleted) "Baseline Diagnostic Completed" else "5-Min Diagnostic Assessment",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Calibrate Smart Insights with real accuracy data across core subjects.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = colors.warningGold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onStartDiagnostic,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                    ) {
                        Text(if (diagnosticCompleted) "Retake Diagnostic Test" else "Start Diagnostic Test")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SilverBlueCard(onClick = onStartChapterQuiz, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Quiz, contentDescription = null, tint = colors.accentBlue)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Active Chapter Quiz", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("Immediate feedback", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    SilverBlueCard(onClick = onOpenMistakeMaster, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.ReportProblem, contentDescription = null, tint = colors.danger)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Mistake Master ($activeMistakesCount)", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("Retry weak MCQs", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SilverBlueCard(onClick = onOpenFlashcards, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Style, contentDescription = null, tint = colors.warningGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Flashcards ($dueFlashcardsCount due)", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("SM-2 memory deck", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    SilverBlueCard(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = colors.success)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Quiz History", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("Past attempts & marks", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            item {
                SilverBlueCard(onClick = onOpenInsights, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Insights, contentDescription = null, tint = colors.success)
                        Column {
                            Text("Smart Insights & Readiness", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text("See honest exam readiness and topic weaknesses", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }

            if (examPatterns.isNotEmpty()) {
                item {
                    Text(
                        text = "KPK BISE Board Patterns & Timed Drills",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }
                items(examPatterns, key = { it.id }) { pattern ->
                    SilverBlueCard(onClick = { onStartPatternExam(pattern) }, modifier = Modifier.fillMaxWidth()) {
                        Text(pattern.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(pattern.sectionSummary, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        val negText = if (pattern.negativeMarkingPenalty > 0.0) " · -${pattern.negativeMarkingPenalty} negative marking" else ""
                        Text(
                            text = "${pattern.questionCount} MCQs · ${pattern.durationMinutes} min · Pass ${pattern.passPercentage}%$negText",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (pattern.negativeMarkingPenalty > 0.0) colors.warningGold else colors.accentBlue
                        )
                    }
                }
            }

            if (subjects.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Custom Subject Drill",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("mixed", "easy", "medium", "hard").forEach { diff ->
                                val selected = selectedDifficulty == diff
                                Surface(
                                    onClick = { selectedDifficulty = diff },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) colors.accentBlue else colors.cardSurface,
                                    border = BorderStroke(1.dp, if (selected) colors.accentBlue else colors.cardBorder)
                                ) {
                                    Text(
                                        text = diff.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subjects.forEach { sub ->
                                OutlinedButton(onClick = { onStartSubjectDrill(sub.id, selectedDifficulty) }) {
                                    Text("${sub.name} Quiz")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 10: Active MCQ / Mock Exam Runner (Zero Ads Strictly Enforced, Question Palette, Countdown/Elapsed Timer, Negative Marking Banner, Subjective Rubric Modal)
 */
@Composable
fun McqPracticeScreen(
    mcqsState: UiResourceState<List<McqQuestionUi>>,
    sessionState: QuizSessionState,
    languageCode: String = "English",
    feedback: ImtihanFeedbackController? = null,
    onBack: () -> Unit,
    onSelectOption: (questionId: String, optionId: String) -> Unit,
    onToggleMarkForReview: (questionId: String) -> Unit,
    onJumpToQuestion: (Int) -> Unit = {},
    onTickTimer: () -> Unit = {},
    onNextOrSubmit: (List<McqQuestionUi>) -> Unit,
    onForceSubmit: (List<McqQuestionUi>) -> Unit = onNextOrSubmit,
    onReportQuestion: (mcqId: String) -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    val latestOnTick by rememberUpdatedState(onTickTimer)
    val latestOnForceSubmit by rememberUpdatedState(onForceSubmit)

    var showPaletteDialog by remember { mutableStateOf(false) }
    var showSubjectiveDialog by remember { mutableStateOf(false) }
    var omrMode by remember { mutableStateOf(false) }
    val isUrdu = languageCode.equals("Urdu", ignoreCase = true)

    val resolvedMcqsState: UiResourceState<List<McqQuestionUi>> = remember(sessionState.customQuestions, mcqsState) {
        val custom = sessionState.customQuestions
        when {
            sessionState.isLoadingSession -> UiResourceState.Loading
            !sessionState.sessionError.isNullOrBlank() -> UiResourceState.Error(sessionState.sessionError)
            custom != null -> {
                if (custom.isEmpty()) UiResourceState.Empty("No Exam Questions", "Try another subject or difficulty.")
                else UiResourceState.Success(custom)
            }
            else -> mcqsState
        }
    }

    val questions = (resolvedMcqsState as? UiResourceState.Success)?.data.orEmpty()
    val hasQuestions = questions.isNotEmpty()
    val totalAllowedSeconds = sessionState.durationMinutes * 60
    val isTimedCountdown = sessionState.durationMinutes > 0

    LaunchedEffect(hasQuestions, sessionState.sessionId) {
        if (hasQuestions) {
            while (true) {
                delay(1_000L)
                latestOnTick()
            }
        }
    }

    LaunchedEffect(sessionState.elapsedSeconds, isTimedCountdown, hasQuestions) {
        if (isTimedCountdown && hasQuestions && sessionState.elapsedSeconds >= totalAllowedSeconds) {
            latestOnForceSubmit(questions)
        }
    }

    val displaySeconds = if (isTimedCountdown) {
        (totalAllowedSeconds - sessionState.elapsedSeconds).coerceAtLeast(0)
    } else {
        sessionState.elapsedSeconds
    }
    val formattedTimer = "%02d:%02d".format(displaySeconds / 60, displaySeconds % 60)

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                    Text(
                        text = sessionState.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasQuestions) {
                        Surface(
                            onClick = {
                                feedback?.tap()
                                omrMode = !omrMode
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (omrMode) colors.accentBlue else colors.cardElevated,
                            border = BorderStroke(1.dp, if (omrMode) colors.cyanPulse else colors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FactCheck,
                                    contentDescription = "Toggle OMR Board Sheet",
                                    tint = if (omrMode) MaterialTheme.colorScheme.onPrimary else colors.cyanPulse,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (omrMode) "OMR" else "Drill",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (omrMode) MaterialTheme.colorScheme.onPrimary else colors.textPrimary
                                )
                            }
                        }
                    }
                    if (sessionState.subjectivePrompts.isNotEmpty()) {
                        IconButton(onClick = { showSubjectiveDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Section B/C Subjective Rubric",
                                tint = colors.warningGold
                            )
                        }
                    }
                    if (hasQuestions) {
                        IconButton(onClick = { showPaletteDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = "Question Palette",
                                tint = colors.accentBlue
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = if (isTimedCountdown && displaySeconds <= 60) colors.danger else colors.accentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formattedTimer,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isTimedCountdown && displaySeconds <= 60) colors.danger else colors.textPrimary
                        )
                    }
                }
            }

            when (resolvedMcqsState) {
                is UiResourceState.Loading -> ImtihanLoadingView()
                is UiResourceState.Error -> ImtihanErrorView(resolvedMcqsState.message, onRetry = onBack)
                is UiResourceState.Empty -> Box(modifier = Modifier.padding(16.dp)) {
                    ImtihanEmptyView(resolvedMcqsState.title, resolvedMcqsState.description)
                }
                is UiResourceState.Success -> {
                    val index = sessionState.currentIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
                    val currentQuestion = questions.getOrNull(index) ?: return@SilverBlueCanvas
                    val selectedId = sessionState.selectedAnswers[currentQuestion.id]
                    val marked = currentQuestion.id in sessionState.markedForReviewIds
                    val progressFraction = (index + 1).toFloat() / questions.size.toFloat()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (sessionState.negativeMarkingPenalty > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.warningGold.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, colors.warningGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Negative Marking Active: -${sessionState.negativeMarkingPenalty} mark per wrong selection (+${sessionState.marksPerMcq} correct)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.warningGold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Q: ${index + 1} / ${questions.size} · ${currentQuestion.topicTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.success
                                )
                            }
                            ImtihanProgressBar(progress = progressFraction)

                            Text(
                                text = currentQuestion.question,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.textPrimary
                            )

                            if (isUrdu && !currentQuestion.questionUrdu.isNullOrBlank()) {
                                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = currentQuestion.questionUrdu,
                                            style = urduTextStyle(sizeSp = 17f, bold = true),
                                            color = colors.cyanPulse,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                currentQuestion.options.forEachIndexed { optIdx, option ->
                                    val isSelected = selectedId == option.id
                                    val urduOptionText = currentQuestion.optionsUrdu.getOrNull(optIdx)
                                    Surface(
                                        onClick = {
                                            feedback?.tap()
                                            onSelectOption(currentQuestion.id, option.id)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) colors.accentBlue.copy(alpha = 0.25f) else colors.cardSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) colors.accentBlue else colors.cardBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) colors.accentBlue else colors.cardElevated,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = option.id,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else colors.textPrimary
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = option.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.textPrimary
                                                )
                                                if (isUrdu && !urduOptionText.isNullOrBlank()) {
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                        Text(
                                                            text = urduOptionText,
                                                            style = urduTextStyle(sizeSp = 15f),
                                                            color = colors.cyanPulse,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (omrMode) {
                                SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                                    ZeroPillMetaLine("KPK BISE OMR BUBBLE SHEET (${sessionState.selectedAnswers.size}/${questions.size} BUBBLED)")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        questions.forEachIndexed { qIdx, qItem ->
                                            val chosen = sessionState.selectedAnswers[qItem.id]
                                            val isCurrent = qIdx == index
                                            Surface(
                                                onClick = {
                                                    feedback?.tap()
                                                    onJumpToQuestion(qIdx)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isCurrent) colors.accentBlue.copy(alpha = 0.16f) else colors.canvasDeep.copy(alpha = 0.45f),
                                                border = BorderStroke(1.dp, if (isCurrent) colors.cyanPulse else colors.cardBorder.copy(alpha = 0.45f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Q%02d".format(qIdx + 1),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isCurrent) colors.cyanPulse else colors.textSecondary
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        listOf("A", "B", "C", "D").forEach { letter ->
                                                            val filled = chosen == letter
                                                            Surface(
                                                                onClick = {
                                                                    feedback?.tap()
                                                                    onJumpToQuestion(qIdx)
                                                                    onSelectOption(qItem.id, letter)
                                                                },
                                                                shape = CircleShape,
                                                                color = if (filled) colors.accentBlue else colors.cardElevated,
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (filled) colors.cyanPulse else colors.cardBorder
                                                                ),
                                                                modifier = Modifier.size(30.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = letter,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = if (filled) MaterialTheme.colorScheme.onPrimary else colors.textPrimary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = marked,
                                        onCheckedChange = {
                                            feedback?.tap()
                                            onToggleMarkForReview(currentQuestion.id)
                                        }
                                    )
                                    Text(
                                        text = "Mark for Review",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                                TextButton(onClick = { onReportQuestion(currentQuestion.id) }) {
                                    Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Report")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (index > 0) {
                                OutlinedButton(
                                    onClick = {
                                        feedback?.tap()
                                        onJumpToQuestion(index - 1)
                                    },
                                    modifier = Modifier.height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Prev")
                                }
                            }
                            Button(
                                onClick = {
                                    feedback?.tap()
                                    onNextOrSubmit(questions)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                            ) {
                                Text(
                                    text = if (index + 1 < questions.size) "Next >" else "Submit Quiz",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (showPaletteDialog) {
                        AlertDialog(
                            onDismissRequest = { showPaletteDialog = false },
                            title = { Text("Question Palette (${sessionState.selectedAnswers.size}/${questions.size} answered)") },
                            text = {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(220.dp)
                                ) {
                                    itemsIndexed(questions) { idx, q ->
                                        val isAnswered = q.id in sessionState.selectedAnswers
                                        val isMarked = q.id in sessionState.markedForReviewIds
                                        val bg = when {
                                            isMarked -> colors.warningGold
                                            isAnswered -> colors.success
                                            else -> colors.cardElevated
                                        }
                                        Surface(
                                            onClick = {
                                                onJumpToQuestion(idx)
                                                showPaletteDialog = false
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = bg.copy(alpha = if (isAnswered || isMarked) 0.25f else 1f),
                                            border = BorderStroke(1.dp, if (idx == index) colors.accentBlue else colors.cardBorder)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("${idx + 1}", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showPaletteDialog = false
                                    onForceSubmit(questions)
                                }) {
                                    Text("Finish & Grade Now")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPaletteDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    if (showSubjectiveDialog) {
                        AlertDialog(
                            onDismissRequest = { showSubjectiveDialog = false },
                            title = { Text("Section B & C Subjective Prompts") },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    sessionState.subjectivePrompts.forEach { sp ->
                                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                                            Text("${sp.questionType.uppercase()} · ${sp.marks} Marks", style = MaterialTheme.typography.labelSmall, color = colors.accentBlue)
                                            Text(sp.question, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                            if (sp.answerSteps.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Marking Rubric: ${sp.answerSteps.joinToString(" | ")}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showSubjectiveDialog = false }) { Text("Done") }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 11: Quiz Results (Score, Marks with negative marking, Pass/Fail, XP, Topic Breakdown, Mistake Master note, Answer Review)
 */
@Composable
fun QuizResultsScreen(
    result: QuizResultUi?,
    isSubmitting: Boolean = false,
    studentName: String = "Guest Learner",
    boardLabel: String = "KPK",
    classLevel: Int = 11,
    subjectName: String = "KPK Subject",
    streakDays: Int = 0,
    xpTotal: Int = 0,
    feedback: ImtihanFeedbackController? = null,
    onBackToHome: () -> Unit,
    onRetakeQuiz: () -> Unit,
    onOpenMistakeMaster: () -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    val context = LocalContext.current
    var showReviewList by remember { mutableStateOf(false) }

    LaunchedEffect(result) {
        if (result != null) {
            if (result.passed) feedback?.fanfare() else feedback?.wrong()
        }
    }

    SilverBlueCanvas {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (result == null && isSubmitting) {
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                    ImtihanLoadingView(message = "Grading answers with official KPK board key...")
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    AnimatedProgressRing(
                        progress = (result?.percentage ?: 0) / 100f,
                        size = 112.dp,
                        strokeWidth = 9.dp,
                        accentColor = if (result?.passed == true) colors.success else colors.warningGold,
                        centerLabel = "${result?.percentage ?: 0}%",
                        centerCaption = if (result?.passed == true) "PASSED" else "REVIEW"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (result?.passed == true) "Quiz Completed · Passed!" else "Quiz Completed",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary
                    )
                }
            }

            if (result != null) {
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Your Score", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${result.correctCount} / ${result.totalQuestions}",
                                style = MaterialTheme.typography.displaySmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${result.percentage}% · Marks ${result.marksObtained}/${result.totalMarks} · +${result.xpEarned} XP",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.warningGold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ImtihanProgressBar(
                                progress = result.percentage / 100f,
                                fillColor = if (result.passed) colors.success else colors.warningGold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ResultStatRow("Correct", "${result.correctCount}", colors.success, Icons.Outlined.CheckCircle)
                            Spacer(modifier = Modifier.height(8.dp))
                            ResultStatRow("Incorrect", "${result.incorrectCount}", colors.danger, Icons.Outlined.Cancel)
                            Spacer(modifier = Modifier.height(8.dp))
                            ResultStatRow("Time", result.elapsedTimeFormatted, colors.accentBlue, Icons.Outlined.Timer)
                        }
                    }
                }

                if (result.topicBreakdown.isNotEmpty()) {
                    item {
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Topic Accuracy Breakdown", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            result.topicBreakdown.forEach { tb ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(tb.topicTitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    Text("${tb.correct}/${tb.total} (${tb.accuracyPercent}%)", style = MaterialTheme.typography.labelMedium, color = colors.accentBlue)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                ImtihanProgressBar(progress = tb.accuracyPercent / 100f)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                if (result.incorrectCount > 0 || result.unansweredCount > 0) {
                    item {
                        SilverBlueCard(onClick = onOpenMistakeMaster, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Missed questions saved to Mistake Master",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.danger
                            )
                            Text(
                                text = "Tap to review explanations and re-practice your mistakes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            feedback?.tap()
                            ShareCardGenerator.shareQuizScorecard(
                                context = context,
                                studentName = studentName,
                                boardLabel = boardLabel,
                                classLevel = classLevel,
                                subjectName = subjectName,
                                scorePercent = result.percentage,
                                correctCount = result.correctCount,
                                totalQuestions = result.totalQuestions,
                                streakDays = streakDays,
                                xpTotal = xpTotal
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.cyanPulse)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Scorecard to WhatsApp / Story", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showReviewList = !showReviewList },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                    ) {
                        Text(if (showReviewList) "Hide Answer Review" else "Review Answers")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRetakeQuiz,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Practice Again", color = colors.textPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onBackToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Back to Home", color = colors.textPrimary)
                    }
                }

                if (showReviewList) {
                    items(result.reviewItems) { item ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Text(item.question, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Selected: ${item.selectedOptionId ?: "Skipped"} · Correct: ${item.correctOptionId ?: "Graded"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (item.isCorrect) colors.success else colors.danger
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.explanation, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatRow(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val colors = LocalImtihanColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        Text(value, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
    }
}
