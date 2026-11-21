package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
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
import com.imtihanpro.app.ui.components.ImtihanHeroCard
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanNavigationRow
import com.imtihanpro.app.ui.components.ImtihanPrimaryButton
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.ImtihanSecondaryButton
import com.imtihanpro.app.ui.components.ImtihanStatusBadge
import com.imtihanpro.app.ui.components.ImtihanTopBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.share.ShareCardGenerator
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.tabular
import com.imtihanpro.app.ui.theme.urduTextStyle
import com.imtihanpro.app.ui.viewmodel.QuizSessionState
import kotlinx.coroutines.delay

/**
 * Practice Hub (Tab 3: Chapter quizzes, KPK BISE Mock Exams, Diagnostic Test, Mistake Master, Flashcards)
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
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Exam & Practice Arena",
                subtitle = "KPK Class $classLevel MCQs & Timed Mock Exams",
                showBottomDivider = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!sessionError.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(ImtihanRadii.button),
                            color = colors.danger.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = sessionError,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.danger,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Diagnostic Hero Card
                item {
                    ImtihanHeroCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (diagnosticCompleted) "Baseline Diagnostic Completed" else "5-Min Diagnostic Assessment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Calibrate Smart Insights with real accuracy data across core subjects.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE68A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onStartDiagnostic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                            shape = RoundedCornerShape(ImtihanRadii.button),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1D4ED8)
                            )
                        ) {
                            Text(
                                text = if (diagnosticCompleted) "Retake Diagnostic Test" else "Start Diagnostic Test",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2x2 Practice Tools Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PracticeToolGridCard(
                                title = "Active Chapter Quiz",
                                subtitle = "Immediate feedback",
                                icon = Icons.Outlined.Quiz,
                                iconTint = colors.accentBlue,
                                onClick = onStartChapterQuiz,
                                modifier = Modifier.weight(1f)
                            )
                            PracticeToolGridCard(
                                title = "Mistake Master ($activeMistakesCount)",
                                subtitle = "Retry weak MCQs",
                                icon = Icons.Outlined.ReportProblem,
                                iconTint = colors.danger,
                                onClick = onOpenMistakeMaster,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PracticeToolGridCard(
                                title = "Flashcards ($dueFlashcardsCount due)",
                                subtitle = "SM-2 memory deck",
                                icon = Icons.Outlined.Style,
                                iconTint = colors.warningGold,
                                onClick = onOpenFlashcards,
                                modifier = Modifier.weight(1f)
                            )
                            PracticeToolGridCard(
                                title = "Quiz History",
                                subtitle = "Past attempts & marks",
                                icon = Icons.Outlined.History,
                                iconTint = colors.success,
                                onClick = onOpenHistory,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Smart Insights Row
                item {
                    SilverBlueCard(
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ImtihanNavigationRow(
                            title = "Smart Insights & Readiness",
                            subtitle = "See honest exam readiness and topic weaknesses",
                            icon = Icons.Outlined.Insights,
                            iconTint = colors.success,
                            onClick = onOpenInsights
                        )
                    }
                }

                // KPK BISE Board Patterns
                if (examPatterns.isNotEmpty()) {
                    item {
                        Text(
                            text = "KPK BISE Board Patterns & Timed Drills",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    items(examPatterns, key = { it.id }) { pattern ->
                        SilverBlueCard(
                            onClick = { onStartPatternExam(pattern) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pattern.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = colors.textMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pattern.sectionSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ImtihanStatusBadge(
                                    text = "${pattern.questionCount} MCQs · ${pattern.durationMinutes} min",
                                    tint = colors.accentBlue
                                )
                                ImtihanStatusBadge(
                                    text = if (pattern.negativeMarkingPenalty > 0.0) {
                                        "-${pattern.negativeMarkingPenalty} neg"
                                    } else {
                                        "Pass ${pattern.passPercentage}%"
                                    },
                                    tint = if (pattern.negativeMarkingPenalty > 0.0) colors.warningGold else colors.success
                                )
                            }
                        }
                    }
                }

                // Custom Subject Drill
                if (subjects.isNotEmpty()) {
                    item {
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Custom Subject Drill",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("mixed", "easy", "medium", "hard").forEach { diff ->
                                    val selected = selectedDifficulty == diff
                                    Surface(
                                        onClick = { selectedDifficulty = diff },
                                        shape = RoundedCornerShape(ImtihanRadii.chip),
                                        color = if (selected) colors.accentBlue else colors.canvasDeep,
                                        border = BorderStroke(
                                            ImtihanDimensions.hairlineBorder,
                                            if (selected) colors.accentBlue else colors.cardBorder
                                        ),
                                        modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = diff.replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) Color.White else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                subjects.forEach { sub ->
                                    OutlinedButton(
                                        onClick = { onStartSubjectDrill(sub.id, selectedDifficulty) },
                                        modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                                        shape = RoundedCornerShape(ImtihanRadii.button)
                                    ) {
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
}

@Composable
private fun PracticeToolGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    SilverBlueCard(onClick = onClick, modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(ImtihanRadii.chip),
            color = iconTint.copy(alpha = 0.14f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.tabular(),
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Screen 10: Active MCQ / Mock Exam Runner (Clean exam chrome, OMR bubble sheet, Palette, Tabular Timer)
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

    var autoSubmitted by remember(sessionState.sessionId) { mutableStateOf(false) }

    LaunchedEffect(hasQuestions, sessionState.sessionId, sessionState.isSubmitting, sessionState.latestResult) {
        if (hasQuestions && !sessionState.isSubmitting && sessionState.latestResult == null) {
            while (true) {
                delay(1_000L)
                latestOnTick()
            }
        }
    }

    LaunchedEffect(
        sessionState.elapsedSeconds,
        isTimedCountdown,
        hasQuestions,
        sessionState.isSubmitting,
        sessionState.latestResult
    ) {
        if (
            isTimedCountdown &&
            hasQuestions &&
            !autoSubmitted &&
            !sessionState.isSubmitting &&
            sessionState.latestResult == null &&
            sessionState.elapsedSeconds >= totalAllowedSeconds
        ) {
            autoSubmitted = true
            latestOnForceSubmit(questions)
        }
    }

    val displaySeconds = if (isTimedCountdown) {
        (totalAllowedSeconds - sessionState.elapsedSeconds).coerceAtLeast(0)
    } else {
        sessionState.elapsedSeconds
    }
    val formattedTimer = "%02d:%02d".format(displaySeconds / 60, displaySeconds % 60)
    val timerUrgent = isTimedCountdown && displaySeconds <= 60

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            // Streamlined exam chrome that fits comfortably on 360dp widths
            Surface(
                color = colors.canvasDeep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = ImtihanDimensions.topBarHeight)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.defaultMinSize(
                                minWidth = ImtihanDimensions.minTouchTarget,
                                minHeight = ImtihanDimensions.minTouchTarget
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                        Text(
                            text = sessionState.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (hasQuestions) {
                            Surface(
                                onClick = {
                                    feedback?.tap()
                                    omrMode = !omrMode
                                },
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                color = if (omrMode) colors.accentBlue else colors.cardSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (omrMode) colors.accentBlue else colors.cardBorder
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FactCheck,
                                        contentDescription = "Toggle OMR Sheet",
                                        tint = if (omrMode) Color.White else colors.cyanPulse,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (omrMode) "OMR" else "Drill",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (omrMode) Color.White else colors.textPrimary
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
                        Surface(
                            shape = RoundedCornerShape(ImtihanRadii.chip),
                            color = if (timerUrgent) colors.danger.copy(alpha = 0.16f) else colors.cardSurface,
                            border = BorderStroke(
                                1.dp,
                                if (timerUrgent) colors.danger else colors.cardBorder
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = if (timerUrgent) colors.danger else colors.accentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formattedTimer,
                                    style = MaterialTheme.typography.labelMedium.tabular(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (timerUrgent) colors.danger else colors.textPrimary
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        thickness = ImtihanDimensions.hairlineBorder,
                        color = colors.cardBorder.copy(alpha = 0.55f)
                    )
                }
            }

            when (resolvedMcqsState) {
                is UiResourceState.Loading -> ImtihanLoadingView(showSkeletons = true)
                is UiResourceState.Error -> Box(modifier = Modifier.padding(ImtihanSpacing.md)) {
                    ImtihanErrorView(resolvedMcqsState.message, onRetry = onBack)
                }
                is UiResourceState.Empty -> Box(modifier = Modifier.padding(ImtihanSpacing.md)) {
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
                            .padding(horizontal = ImtihanSpacing.md, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (sessionState.negativeMarkingPenalty > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(ImtihanRadii.chip),
                                    color = colors.warningGold.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, colors.warningGold.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Negative Marking: -${sessionState.negativeMarkingPenalty} per wrong option (+${sessionState.marksPerMcq} correct)",
                                        style = MaterialTheme.typography.labelSmall.tabular(),
                                        color = colors.warningGold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Q ${index + 1} / ${questions.size} · ${currentQuestion.topicTitle}",
                                    style = MaterialTheme.typography.labelMedium.tabular(),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.tabular(),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.success
                                )
                            }
                            ImtihanProgressBar(progress = progressFraction)

                            SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = currentQuestion.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }

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
                                        shape = RoundedCornerShape(ImtihanRadii.button),
                                        color = if (isSelected) colors.accentBlue.copy(alpha = 0.22f) else colors.cardSurface,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) colors.accentBlue else colors.cardBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 56.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) colors.accentBlue else colors.cardElevated,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = option.id,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else colors.textPrimary
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
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = colors.accentBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (omrMode) {
                                SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "KPK BISE OMR Bubble Sheet (${sessionState.selectedAnswers.size}/${questions.size} bubbled)",
                                        style = MaterialTheme.typography.labelMedium.tabular(),
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.cyanPulse
                                    )
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
                                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                                color = if (isCurrent) colors.accentBlue.copy(alpha = 0.16f) else colors.canvasDeep.copy(alpha = 0.55f),
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
                                                        style = MaterialTheme.typography.labelMedium.tabular(),
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
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = letter,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (filled) Color.White else colors.textPrimary
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
                                Row(
                                    modifier = Modifier
                                        .clickable(role = Role.Checkbox) {
                                            feedback?.tap()
                                            onToggleMarkForReview(currentQuestion.id)
                                        }
                                        .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget)
                                        .padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = marked,
                                        onCheckedChange = null
                                    )
                                    Text(
                                        text = "Mark for Review",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                                TextButton(
                                    onClick = { onReportQuestion(currentQuestion.id) },
                                    modifier = Modifier.defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget)
                                ) {
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
                                    modifier = Modifier.defaultMinSize(minHeight = 50.dp),
                                    shape = RoundedCornerShape(ImtihanRadii.button)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Previous")
                                }
                            }
                            Button(
                                onClick = {
                                    feedback?.tap()
                                    onNextOrSubmit(questions)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 50.dp),
                                shape = RoundedCornerShape(ImtihanRadii.button),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                            ) {
                                Text(
                                    text = if (index + 1 < questions.size) "Next Question" else "Submit Quiz",
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (index + 1 < questions.size) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (showPaletteDialog) {
                        AlertDialog(
                            onDismissRequest = { showPaletteDialog = false },
                            title = { Text("Question Palette (${sessionState.selectedAnswers.size}/${questions.size} answered)") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        ImtihanStatusBadge(text = "Answered", tint = colors.success)
                                        ImtihanStatusBadge(text = "Marked", tint = colors.warningGold)
                                    }
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
                                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                                color = bg.copy(alpha = if (isAnswered || isMarked) 0.25f else 1f),
                                                border = BorderStroke(
                                                    width = if (idx == index) 1.5.dp else 1.dp,
                                                    color = if (idx == index) colors.accentBlue else colors.cardBorder
                                                ),
                                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${idx + 1}",
                                                        style = MaterialTheme.typography.labelMedium.tabular(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textPrimary
                                                    )
                                                }
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
                                            ImtihanStatusBadge(
                                                text = "${sp.questionType} · ${sp.marks} Marks",
                                                tint = colors.accentBlue
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(sp.question, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                            if (sp.answerSteps.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Marking Rubric: ${sp.answerSteps.joinToString(" | ")}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textSecondary
                                                )
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
 * Screen 11: Quiz Results — matches commercial reference screen with celebration hero,
 * tabular score & marks breakdown, topic breakdown, Mistake Master trigger, and clear button hierarchy.
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
            contentPadding = PaddingValues(ImtihanSpacing.md),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (result == null && isSubmitting) {
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                    ImtihanLoadingView(message = "Grading answers with official KPK board key...")
                }
            } else if (result != null) {
                item {
                    ImtihanHeroCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when {
                                    result.percentage >= 85 -> "Excellent!"
                                    result.passed -> "Great Effort!"
                                    else -> "Keep Practicing"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "You scored",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${result.percentage}%",
                                style = MaterialTheme.typography.displayMedium.tabular(),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047)
                            )
                            Text(
                                text = "${result.correctCount}/${result.totalQuestions} Correct · Marks ${result.marksObtained}/${result.totalMarks} · +${result.xpEarned} XP",
                                style = MaterialTheme.typography.labelLarge.tabular(),
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }

                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        ResultStatRow("Correct", "${result.correctCount}", colors.success, Icons.Outlined.CheckCircle)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = colors.cardBorder.copy(alpha = 0.45f))
                        ResultStatRow("Incorrect", "${result.incorrectCount}", colors.danger, Icons.Outlined.Cancel)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = colors.cardBorder.copy(alpha = 0.45f))
                        ResultStatRow("Unanswered", "${result.unansweredCount}", colors.textMuted, Icons.Outlined.RemoveCircleOutline)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = colors.cardBorder.copy(alpha = 0.45f))
                        ResultStatRow("Time Spent", result.elapsedTimeFormatted, colors.accentBlue, Icons.Outlined.Timer)
                    }
                }

                if (result.topicBreakdown.isNotEmpty()) {
                    item {
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Topic Accuracy Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            result.topicBreakdown.forEach { tb ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(tb.topicTitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    Text(
                                        text = "${tb.correct}/${tb.total} (${tb.accuracyPercent}%)",
                                        style = MaterialTheme.typography.labelMedium.tabular(),
                                        color = colors.accentBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                ImtihanProgressBar(progress = tb.accuracyPercent / 100f)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                if (result.incorrectCount > 0 || result.unansweredCount > 0) {
                    item {
                        SilverBlueCard(
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ImtihanNavigationRow(
                                title = "Missed questions saved to Mistake Master",
                                subtitle = "Tap to review explanations and re-practice weak topics",
                                icon = Icons.Outlined.ReportProblem,
                                iconTint = colors.danger,
                                onClick = onOpenMistakeMaster
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ImtihanPrimaryButton(
                            text = if (showReviewList) "Hide Answer Review" else "Review Answers",
                            onClick = { showReviewList = !showReviewList }
                        )
                        ImtihanSecondaryButton(
                            text = "Share Scorecard",
                            leadingIcon = Icons.Outlined.Share,
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
                            }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onRetakeQuiz,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                shape = RoundedCornerShape(ImtihanRadii.button)
                            ) {
                                Text("Try Again", color = colors.textPrimary)
                            }
                            OutlinedButton(
                                onClick = onBackToHome,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                shape = RoundedCornerShape(ImtihanRadii.button)
                            ) {
                                Text("Back to Home", color = colors.textPrimary)
                            }
                        }
                    }
                }

                if (showReviewList) {
                    items(result.reviewItems) { item ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isCorrect) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                                    contentDescription = if (item.isCorrect) "Correct" else "Incorrect",
                                    tint = if (item.isCorrect) colors.success else colors.danger,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Selected: ${item.selectedOptionId ?: "Skipped"} · Correct: ${item.correctOptionId ?: "Graded"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.isCorrect) colors.success else colors.danger
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(item.question, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
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
    tint: Color,
    icon: ImageVector
) {
    val colors = LocalImtihanColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.tabular(),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}
