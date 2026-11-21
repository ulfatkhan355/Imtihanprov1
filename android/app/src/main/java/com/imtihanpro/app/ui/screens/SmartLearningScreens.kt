package com.imtihanpro.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.model.FlashcardUi
import com.imtihanpro.app.domain.model.MistakeRecordUi
import com.imtihanpro.app.domain.model.QuizHistoryItemUi
import com.imtihanpro.app.domain.model.SmartInsightsUi
import com.imtihanpro.app.domain.model.StudentBookmarkUi
import com.imtihanpro.app.ui.audio.ImtihanFeedbackController
import com.imtihanpro.app.ui.components.AnimatedProgressRing
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanHeroCard
import com.imtihanpro.app.ui.components.ImtihanNavigationRow
import com.imtihanpro.app.ui.components.ImtihanPrimaryButton
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.ImtihanSecondaryButton
import com.imtihanpro.app.ui.components.ImtihanSegmentedControl
import com.imtihanpro.app.ui.components.ImtihanStatusBadge
import com.imtihanpro.app.ui.components.ImtihanTopBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.tabular
import com.imtihanpro.app.ui.theme.urduTextStyle

/**
 * Smart Insights Screen (Progress Tab): Reference-aligned Overview / Subjects segmented views,
 * AnimatedProgressRing, Tabular Accuracy & Mistakes stats, Subject Mastery bar chart with gridlines,
 * and Weak Chapters Radar.
 */
@Composable
fun SmartInsightsScreen(
    insights: SmartInsightsUi,
    showBackButton: Boolean = true,
    onBack: () -> Unit,
    onStartDiagnostic: () -> Unit,
    onOpenChapterNotes: (chapterId: String, subjectId: String) -> Unit,
    onStartChapterQuiz: (chapterId: String, subjectId: String) -> Unit,
    onOpenMistakeMaster: () -> Unit,
    onOpenFlashcards: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var selectedSegment by remember { mutableIntStateOf(0) }

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "My Progress",
                subtitle = "Honest readiness & mastery analytics",
                onBack = if (showBackButton) onBack else null,
                showBottomDivider = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ImtihanSpacing.md, vertical = ImtihanSpacing.sm)
            ) {
                ImtihanSegmentedControl(
                    options = listOf("Overview", "Subjects"),
                    selectedIndex = selectedSegment,
                    onSelect = { selectedSegment = it }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedSegment == 0) {
                    item {
                        SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedProgressRing(
                                    progress = (insights.readiness.readinessPercent ?: insights.overallAccuracyPercent ?: 0) / 100f,
                                    size = 96.dp,
                                    strokeWidth = 9.dp,
                                    accentColor = if ((insights.readiness.readinessPercent ?: 0) >= 60) colors.success else colors.accentBlue,
                                    centerLabel = insights.readiness.readinessPercent?.let { "$it%" }
                                        ?: insights.overallAccuracyPercent?.let { "$it%" }
                                        ?: "—",
                                    centerCaption = "Overall"
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Exam Readiness Index",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    if (insights.readiness.readinessPercent != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ImtihanStatusBadge(
                                            text = insights.readiness.status.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                            tint = if (insights.readiness.readinessPercent >= 60) colors.success else colors.warningGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = insights.readiness.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                    if (insights.readiness.readinessPercent == null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        ImtihanPrimaryButton(
                                            text = "Take Baseline Diagnostic",
                                            onClick = onStartDiagnostic
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SilverBlueCard(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(ImtihanRadii.chip),
                                        color = colors.accentBlue.copy(alpha = 0.14f),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Insights,
                                                contentDescription = null,
                                                tint = colors.accentBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text("Accuracy", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = insights.overallAccuracyPercent?.let { "$it%" } ?: "—",
                                    style = MaterialTheme.typography.headlineSmall.tabular(),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${insights.totalQuizAttempts} attempt(s)",
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = colors.textMuted
                                )
                            }
                            SilverBlueCard(onClick = onOpenMistakeMaster, modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(ImtihanRadii.chip),
                                        color = (if (insights.activeMistakesCount > 0) colors.danger else colors.success).copy(alpha = 0.14f),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.ReportProblem,
                                                contentDescription = null,
                                                tint = if (insights.activeMistakesCount > 0) colors.danger else colors.success,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text("Mistakes", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${insights.activeMistakesCount}",
                                    style = MaterialTheme.typography.headlineSmall.tabular(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (insights.activeMistakesCount > 0) colors.danger else colors.success
                                )
                                Text(
                                    text = "${insights.dueFlashcardsCount} cards due",
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = colors.accentBlue
                                )
                            }
                        }
                    }

                    if (insights.subjectAnalytics.isNotEmpty()) {
                        item {
                            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Subject Mastery Trend",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "0–100%",
                                        style = MaterialTheme.typography.labelSmall.tabular(),
                                        color = colors.textMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                val bars = insights.subjectAnalytics.take(6)
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                ) {
                                    val guideColor = colors.cardBorder.copy(alpha = 0.5f)
                                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                    listOf(0.25f, 0.5f, 0.75f).forEach { level ->
                                        val yPos = size.height * (1f - level)
                                        drawLine(
                                            color = guideColor,
                                            start = Offset(0f, yPos),
                                            end = Offset(size.width, yPos),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = dash
                                        )
                                    }
                                    drawLine(
                                        color = colors.cardBorder,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )

                                    val count = bars.size.coerceAtLeast(1)
                                    val slotWidth = size.width / count
                                    val barWidth = (slotWidth * 0.48f).coerceAtMost(32.dp.toPx())
                                    bars.forEachIndexed { idx, sub ->
                                        val pct = (sub.masteryPercent ?: 0).coerceIn(0, 100) / 100f
                                        val barHeight = (size.height - 6.dp.toPx()) * pct.coerceAtLeast(0.06f)
                                        val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                                        val y = size.height - barHeight
                                        drawRoundRect(
                                            color = if (sub.masteryPercent == null) colors.cardBorder else colors.accentBlue,
                                            topLeft = Offset(x, y),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    bars.forEach { sub ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = sub.subjectName.take(4),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary
                                            )
                                            Text(
                                                text = sub.masteryPercent?.let { "$it%" } ?: "—",
                                                style = MaterialTheme.typography.labelSmall.tabular(),
                                                color = colors.accentBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (insights.recommendations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recommended Next Steps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        items(insights.recommendations, key = { it.id }) { rec ->
                            SilverBlueCard(
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ImtihanNavigationRow(
                                    title = rec.title,
                                    subtitle = rec.reason,
                                    icon = Icons.Outlined.Insights,
                                    onClick = {
                                        when (rec.actionType) {
                                            "review_mistakes" -> onOpenMistakeMaster()
                                            "take_test" -> onStartChapterQuiz(rec.chapterId, rec.subjectId)
                                            else -> onOpenChapterNotes(rec.chapterId, rec.subjectId)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Weak Chapters Radar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    if (insights.weakChapters.isEmpty()) {
                        item {
                            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                                Text("No weak chapters flagged yet", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Weak chapters automatically appear when quiz accuracy < 70% or mistakes accumulate.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    } else {
                        items(insights.weakChapters, key = { it.chapterId }) { weak ->
                            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = weak.chapterTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ImtihanStatusBadge(
                                        text = weak.priority.replaceFirstChar { it.uppercase() },
                                        tint = if (weak.priority == "high") colors.danger else colors.warningGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${weak.subjectName} · ${weak.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onOpenChapterNotes(weak.chapterId, weak.subjectId) },
                                        shape = RoundedCornerShape(ImtihanRadii.button),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget)
                                    ) {
                                        Text("Revise Notes")
                                    }
                                    Button(
                                        onClick = { onStartChapterQuiz(weak.chapterId, weak.subjectId) },
                                        shape = RoundedCornerShape(ImtihanRadii.button),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                    ) {
                                        Text("Practice Quiz")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (insights.subjectAnalytics.isEmpty()) {
                        item {
                            ImtihanEmptyView(
                                title = "No Subject Attempts Yet",
                                description = "Complete chapter quizzes or mock exams to unlock per-subject mastery analytics."
                            )
                        }
                    } else {
                        items(insights.subjectAnalytics, key = { it.subjectId }) { sub ->
                            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(sub.subjectName, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(
                                        text = sub.masteryPercent?.let { "$it%" } ?: "No quizzes",
                                        style = MaterialTheme.typography.labelMedium.tabular(),
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accentBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                ImtihanProgressBar(progress = (sub.masteryPercent ?: 0) / 100f)
                            }
                        }
                    }
                }

                item {
                    ImtihanSecondaryButton(
                        text = "Open SM-2 Flashcard Deck (${insights.dueFlashcardsCount} due)",
                        leadingIcon = Icons.Outlined.Style,
                        onClick = onOpenFlashcards
                    )
                }
            }
        }
    }
}

/**
 * Mistake Master Screen — matches reference screen #12 with focal target hero card
 */
@Composable
fun MistakeMasterScreen(
    mistakes: List<MistakeRecordUi>,
    onBack: () -> Unit,
    onResolveMistake: (String) -> Unit,
    onPracticeMistakes: (List<MistakeRecordUi>) -> Unit
) {
    val colors = LocalImtihanColors.current

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Mistake Master",
                subtitle = "${mistakes.size} unresolved question(s)",
                onBack = onBack,
                showBottomDivider = true
            )

            LazyColumn(
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mistakes.isEmpty()) {
                    item {
                        ImtihanEmptyView(
                            title = "Zero Unresolved Mistakes",
                            description = "Any incorrect MCQ from chapter quizzes or mock exams will appear here automatically.",
                            icon = Icons.Outlined.CheckCircle,
                            tint = colors.success
                        )
                    }
                } else {
                    item {
                        ImtihanHeroCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.TrackChanges,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE68A),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Your Mistakes · ${mistakes.size} Questions",
                                    style = MaterialTheme.typography.titleLarge.tabular(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Focus on your weak topics and turn errors into exam marks.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onPracticeMistakes(mistakes) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                    shape = RoundedCornerShape(ImtihanRadii.button),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1D4ED8)
                                    )
                                ) {
                                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Practice", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(mistakes, key = { it.mcqId }) { item ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.topicTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.accentBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                ImtihanStatusBadge(
                                    text = "Missed ${item.wrongCount}x",
                                    tint = colors.danger
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(item.question, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Last picked: ${item.lastWrongOptionId ?: "Skipped"} · Correct: ${item.correctOptionId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.success
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.explanation, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { onResolveMistake(item.mcqId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                shape = RoundedCornerShape(ImtihanRadii.button)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark Mastered", color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bookmarks & SM-2 Flashcards Screen
 */
@Composable
fun BookmarksAndFlashcardsScreen(
    initialTab: Int = 0,
    bookmarks: List<StudentBookmarkUi>,
    flashcards: List<FlashcardUi>,
    languageCode: String = "English",
    feedback: ImtihanFeedbackController? = null,
    onBack: () -> Unit,
    onOpenBookmark: (StudentBookmarkUi) -> Unit,
    onRemoveBookmark: (StudentBookmarkUi) -> Unit,
    onReviewFlashcard: (flashcardId: String, quality: Int) -> Unit
) {
    val colors = LocalImtihanColors.current
    val density = LocalDensity.current.density
    val isUrdu = languageCode.equals("Urdu", ignoreCase = true)

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var activeIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val clampedIdx = if (flashcards.isEmpty()) 0 else activeIndex.coerceIn(0, flashcards.lastIndex)
    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "flashcard_flip_y"
    )
    val animatedDragX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "flashcard_drag_x"
    )

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Flashcards & Bookmarks",
                onBack = onBack,
                showBottomDivider = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ImtihanSpacing.md, vertical = ImtihanSpacing.sm)
            ) {
                ImtihanSegmentedControl(
                    options = listOf("SM-2 Deck (${flashcards.size})", "Saved (${bookmarks.size})"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    if (flashcards.isEmpty()) {
                        item {
                            ImtihanEmptyView(
                                title = "No Flashcards Loaded",
                                description = "Connect or sync Class 11 KPK subjects to study key flashcards."
                            )
                        }
                    } else {
                        val card = flashcards[clampedIdx]
                        item(key = "deck_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap card to flip · Swipe left/right",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                                ImtihanStatusBadge(
                                    text = "${clampedIdx + 1} / ${flashcards.size}",
                                    tint = colors.accentBlue
                                )
                            }
                        }

                        item(key = "active_3d_card_${card.id}") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(card.id, flashcards.size) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (dragOffsetX < -95f && clampedIdx < flashcards.lastIndex) {
                                                    feedback?.tap()
                                                    activeIndex = clampedIdx + 1
                                                    isFlipped = false
                                                } else if (dragOffsetX > 95f && clampedIdx > 0) {
                                                    feedback?.tap()
                                                    activeIndex = clampedIdx - 1
                                                    isFlipped = false
                                                }
                                                dragOffsetX = 0f
                                            },
                                            onDragCancel = { dragOffsetX = 0f },
                                            onHorizontalDrag = { _, dragAmount ->
                                                dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-220f, 220f)
                                            }
                                        )
                                    }
                                    .graphicsLayer {
                                        rotationY = flipRotation
                                        cameraDistance = 12f * density
                                        translationX = animatedDragX
                                        rotationZ = animatedDragX * 0.025f
                                    }
                            ) {
                                val showingBack = flipRotation > 90f
                                SilverBlueCard(
                                    elevated = true,
                                    onClick = {
                                        feedback?.flipCard()
                                        isFlipped = !isFlipped
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 248.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                rotationY = if (showingBack) 180f else 0f
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(card.topicTitle, style = MaterialTheme.typography.labelMedium, color = colors.cyanPulse)
                                                ImtihanStatusBadge(
                                                    text = if (card.isDue) "Due Now" else "Next ${card.intervalDays}d",
                                                    tint = if (card.isDue) colors.warningGold else colors.success
                                                )
                                            }

                                            if (!showingBack) {
                                                Text(
                                                    text = card.front,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                                if (isUrdu && !card.frontUrdu.isNullOrBlank()) {
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                        Text(
                                                            text = card.frontUrdu,
                                                            style = urduTextStyle(sizeSp = 17f),
                                                            color = colors.cyanPulse,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "Tap card to reveal answer",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textMuted
                                                )
                                            } else {
                                                ImtihanStatusBadge(text = "Answer & Recall", tint = colors.success)
                                                Text(
                                                    text = card.back,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.success
                                                )
                                                if (isUrdu && !card.backUrdu.isNullOrBlank()) {
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                        Text(
                                                            text = card.backUrdu,
                                                            style = urduTextStyle(sizeSp = 16f),
                                                            color = colors.textPrimary,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                                if (!card.formulaHint.isNullOrBlank()) {
                                                    Text(
                                                        text = card.formulaHint,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = colors.cyanPulse
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    val ratingTints = mapOf(
                                                        1 to colors.danger,
                                                        3 to colors.warningGold,
                                                        4 to colors.accentBlue,
                                                        5 to colors.success
                                                    )
                                                    listOf(1 to "Again", 3 to "Hard", 4 to "Good", 5 to "Easy").forEach { (q, lbl) ->
                                                        val tint = ratingTints[q] ?: colors.accentBlue
                                                        Surface(
                                                            onClick = {
                                                                if (q >= 4) feedback?.correct() else feedback?.tap()
                                                                onReviewFlashcard(card.id, q)
                                                                isFlipped = false
                                                                if (clampedIdx < flashcards.lastIndex) {
                                                                    activeIndex = clampedIdx + 1
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .defaultMinSize(minHeight = 44.dp),
                                                            shape = RoundedCornerShape(ImtihanRadii.chip),
                                                            color = tint.copy(alpha = 0.14f),
                                                            border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.padding(vertical = 10.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = lbl,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = tint,
                                                                    textAlign = TextAlign.Center
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

                        item(key = "deck_nav") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        feedback?.tap()
                                        if (clampedIdx > 0) {
                                            activeIndex = clampedIdx - 1
                                            isFlipped = false
                                        }
                                    },
                                    enabled = clampedIdx > 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                    shape = RoundedCornerShape(ImtihanRadii.button)
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Previous Card")
                                }
                                Button(
                                    onClick = {
                                        feedback?.tap()
                                        if (clampedIdx < flashcards.lastIndex) {
                                            activeIndex = clampedIdx + 1
                                            isFlipped = false
                                        }
                                    },
                                    enabled = clampedIdx < flashcards.lastIndex,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                    shape = RoundedCornerShape(ImtihanRadii.button),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                ) {
                                    Text("Next Card")
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                } else {
                    if (bookmarks.isEmpty()) {
                        item {
                            ImtihanEmptyView(
                                title = "No Bookmarks Yet",
                                description = "Tap the Bookmark icon inside any Chapter Notes or Workspace to save quick revision links."
                            )
                        }
                    } else {
                        items(bookmarks, key = { it.id }) { bm ->
                            SilverBlueCard(onClick = { onOpenBookmark(bm) }, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        ImtihanStatusBadge(
                                            text = bm.itemType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                            tint = colors.accentBlue
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(bm.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                        Text(bm.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    }
                                    IconButton(
                                        onClick = { onRemoveBookmark(bm) },
                                        modifier = Modifier.defaultMinSize(
                                            minWidth = ImtihanDimensions.minTouchTarget,
                                            minHeight = ImtihanDimensions.minTouchTarget
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remove bookmark",
                                            tint = colors.danger
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

/**
 * Quiz History Screen
 */
@Composable
fun QuizHistoryScreen(
    history: List<QuizHistoryItemUi>,
    onBack: () -> Unit
) {
    val colors = LocalImtihanColors.current

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Quiz & Mock History",
                subtitle = "${history.size} recorded attempts",
                onBack = onBack,
                showBottomDivider = true
            )

            LazyColumn(
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (history.isEmpty()) {
                    item {
                        ImtihanEmptyView(
                            title = "No Quiz Attempts Yet",
                            description = "Submit any chapter quiz, mock exam, or diagnostic test to view scores over time."
                        )
                    }
                } else {
                    items(history, key = { it.id }) { item ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (item.passed) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                                        contentDescription = if (item.passed) "Passed" else "Needs review",
                                        tint = if (item.passed) colors.success else colors.danger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = item.subjectName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${item.percentage}% (${item.marksObtained}/${item.totalMarks})",
                                    style = MaterialTheme.typography.labelMedium.tabular(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.passed) colors.success else colors.danger
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.chapterTitle ?: item.examMode.replace('_', ' ')} · ${item.correctCount}/${item.totalQuestions} correct · ${item.elapsedSeconds}s",
                                style = MaterialTheme.typography.bodySmall.tabular(),
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
