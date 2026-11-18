package com.imtihanpro.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.model.FlashcardUi
import com.imtihanpro.app.domain.model.MistakeRecordUi
import com.imtihanpro.app.domain.model.QuizHistoryItemUi
import com.imtihanpro.app.domain.model.SmartInsightsUi
import com.imtihanpro.app.domain.model.StudentBookmarkUi
import com.imtihanpro.app.ui.audio.ImtihanFeedbackController
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.components.ZeroPillMetaLine
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.urduTextStyle

/**
 * Smart Insights Screen: Honest Exam Readiness, Weak Chapters Radar, Subject Mastery, Revision Recommendations
 */
@Composable
fun SmartInsightsScreen(
    insights: SmartInsightsUi,
    onBack: () -> Unit,
    onStartDiagnostic: () -> Unit,
    onOpenChapterNotes: (chapterId: String, subjectId: String) -> Unit,
    onStartChapterQuiz: (chapterId: String, subjectId: String) -> Unit,
    onOpenMistakeMaster: () -> Unit,
    onOpenFlashcards: () -> Unit
) {
    val colors = LocalImtihanColors.current

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text("Smart Learning Insights", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Exam Readiness Index", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Icon(Icons.Outlined.Insights, contentDescription = null, tint = colors.accentBlue)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (insights.readiness.readinessPercent == null) {
                            Text(
                                text = "Not enough data yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.warningGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = insights.readiness.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onStartDiagnostic,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                            ) {
                                Text("Take Baseline Diagnostic")
                            }
                        } else {
                            Text(
                                text = "${insights.readiness.readinessPercent}% · ${insights.readiness.status.replace('_', ' ').uppercase()}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.success
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ImtihanProgressBar(
                                progress = insights.readiness.readinessPercent / 100f,
                                fillColor = colors.success
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(insights.readiness.message, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SilverBlueCard(modifier = Modifier.weight(1f)) {
                            Text("Overall Accuracy", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            Text(
                                text = insights.overallAccuracyPercent?.let { "$it%" } ?: "—",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.textPrimary
                            )
                            Text("${insights.totalQuizAttempts} attempt(s)", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                        SilverBlueCard(onClick = onOpenMistakeMaster, modifier = Modifier.weight(1f)) {
                            Text("Active Mistakes", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            Text(
                                text = "${insights.activeMistakesCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (insights.activeMistakesCount > 0) colors.danger else colors.success
                            )
                            Text("${insights.dueFlashcardsCount} cards due", style = MaterialTheme.typography.bodySmall, color = colors.accentBlue)
                        }
                    }
                }

                if (insights.recommendations.isNotEmpty()) {
                    item {
                        Text("Recommended Next Steps", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    items(insights.recommendations, key = { it.id }) { rec ->
                        SilverBlueCard(
                            onClick = {
                                when (rec.actionType) {
                                    "review_mistakes" -> onOpenMistakeMaster()
                                    "take_test" -> onStartChapterQuiz(rec.chapterId, rec.subjectId)
                                    else -> onOpenChapterNotes(rec.chapterId, rec.subjectId)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(rec.title, style = MaterialTheme.typography.titleSmall, color = colors.accentBlue)
                            Text(rec.reason, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }

                item {
                    Text("Weak Chapters Radar", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }

                if (insights.weakChapters.isEmpty()) {
                    item {
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Text("No weak chapters flagged yet", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text("Weak chapters automatically appear when quiz accuracy < 70% or mistakes accumulate.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                } else {
                    items(insights.weakChapters, key = { it.chapterId }) { weak ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(weak.chapterTitle, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                Text(
                                    text = weak.priority.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (weak.priority == "high") colors.danger else colors.warningGold
                                )
                            }
                            Text("${weak.subjectName} · ${weak.reason}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenChapterNotes(weak.chapterId, weak.subjectId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Revise Notes")
                                }
                                Button(
                                    onClick = { onStartChapterQuiz(weak.chapterId, weak.subjectId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                ) {
                                    Text("Practice Quiz")
                                }
                            }
                        }
                    }
                }

                if (insights.subjectAnalytics.isNotEmpty()) {
                    item {
                        Text("Subject Mastery", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    items(insights.subjectAnalytics, key = { it.subjectId }) { sub ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sub.subjectName, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text(
                                    text = sub.masteryPercent?.let { "$it%" } ?: "No quizzes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.accentBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            ImtihanProgressBar(progress = (sub.masteryPercent ?: 0) / 100f)
                        }
                    }
                }

                item {
                    OutlinedButton(onClick = onOpenFlashcards, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Style, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Open SM-2 Flashcard Deck (${insights.dueFlashcardsCount} due)")
                    }
                }
            }
        }
    }
}

/**
 * Mistake Master Screen
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Text("Mistake Master (${mistakes.size})", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                }
                if (mistakes.isNotEmpty()) {
                    TextButton(onClick = { onPracticeMistakes(mistakes) }) {
                        Text("Retry All")
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mistakes.isEmpty()) {
                    item {
                        ImtihanEmptyView(
                            title = "Zero Unresolved Mistakes",
                            description = "Any incorrect MCQ from chapter quizzes or mock exams will appear here automatically."
                        )
                    }
                } else {
                    items(mistakes, key = { it.mcqId }) { item ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.topicTitle, style = MaterialTheme.typography.labelMedium, color = colors.accentBlue)
                                Text("Missed ${item.wrongCount}x", style = MaterialTheme.typography.labelSmall, color = colors.danger)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.question, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Last picked: ${item.lastWrongOptionId ?: "Skipped"} · Correct: ${item.correctOptionId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.success
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.explanation, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onResolveMistake(item.mcqId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.success)
                                Spacer(modifier = Modifier.padding(4.dp))
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text("Flashcards & Bookmarks", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.cardSurface,
                contentColor = colors.accentBlue
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("SM-2 Deck (${flashcards.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Saved (${bookmarks.size})") })
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    if (flashcards.isEmpty()) {
                        item {
                            ImtihanEmptyView("No Flashcards Loaded", "Connect or switch to Class 9/10 to study key KPK cards.")
                        }
                    } else {
                        val card = flashcards[clampedIdx]
                        item(key = "deck_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ZeroPillMetaLine("3D INTERACTIVE SM-2 DECK · TAP TO FLIP • SWIPE TO ADVANCE")
                                Text(
                                    text = "${clampedIdx + 1} / ${flashcards.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.accentBlue
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
                                        .heightIn(min = 240.dp)
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
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(card.topicTitle, style = MaterialTheme.typography.labelMedium, color = colors.cyanPulse)
                                                Text(
                                                    text = if (card.isDue) "DUE NOW" else "NEXT ${card.intervalDays}d",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (card.isDue) colors.warningGold else colors.success
                                                )
                                            }

                                            if (!showingBack) {
                                                Text(
                                                    text = card.front,
                                                    style = MaterialTheme.typography.headlineSmall,
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
                                                    text = "Tap card to flip in 3D · Swipe left/right for next card",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textMuted
                                                )
                                            } else {
                                                Text("ANSWER & RECALL", style = MaterialTheme.typography.labelSmall, color = colors.success)
                                                Text(
                                                    text = card.back,
                                                    style = MaterialTheme.typography.titleMedium,
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
                                                    listOf(1 to "Again", 3 to "Hard", 4 to "Good", 5 to "Easy").forEach { (q, lbl) ->
                                                        Surface(
                                                            onClick = {
                                                                if (q >= 4) feedback?.correct() else feedback?.tap()
                                                                onReviewFlashcard(card.id, q)
                                                                isFlipped = false
                                                                if (clampedIdx < flashcards.lastIndex) {
                                                                    activeIndex = clampedIdx + 1
                                                                }
                                                            },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = colors.cardElevated,
                                                            border = BorderStroke(1.dp, if (q >= 4) colors.success.copy(alpha = 0.5f) else colors.cardBorder)
                                                        ) {
                                                            Text(
                                                                text = lbl,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = colors.textPrimary,
                                                                modifier = Modifier.padding(vertical = 10.dp),
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

                        item(key = "deck_nav") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        feedback?.tap()
                                        if (clampedIdx > 0) {
                                            activeIndex = clampedIdx - 1
                                            isFlipped = false
                                        }
                                    },
                                    enabled = clampedIdx > 0
                                ) {
                                    Text("< Previous Card")
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
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                ) {
                                    Text("Next Card >")
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
                                        Text(bm.itemType.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.accentBlue)
                                        Text(bm.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                        Text(bm.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    }
                                    TextButton(onClick = { onRemoveBookmark(bm) }) {
                                        Text("Remove", color = colors.danger)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text("Quiz & Mock History", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.subjectName, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text(
                                    text = "${item.percentage}% (${item.marksObtained}/${item.totalMarks})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.passed) colors.success else colors.danger
                                )
                            }
                            Text(
                                text = "${item.chapterTitle ?: item.examMode.replace('_', ' ')} · ${item.correctCount}/${item.totalQuestions} correct · ${item.elapsedSeconds}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
