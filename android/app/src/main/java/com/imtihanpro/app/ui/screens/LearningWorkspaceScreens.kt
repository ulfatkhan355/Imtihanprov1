package com.imtihanpro.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.model.ChapterProgressStatus
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.ContentBlockUi
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.audio.ImtihanFeedbackController
import com.imtihanpro.app.ui.components.ContentBlockRenderer
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanErrorView
import com.imtihanpro.app.ui.components.ImtihanHeroCard
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanNavigationRow
import com.imtihanpro.app.ui.components.ImtihanPrimaryButton
import com.imtihanpro.app.ui.components.ImtihanProgressBar
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
import kotlinx.coroutines.launch

/**
 * Screen 7: Chapter Learning Workspace — matches visual reference with cobalt hero,
 * "What You'll Learn" learning outcomes, "Start Learning" primary action, Topic Drills,
 * and grouped Related Resources list.
 */
@Composable
fun ChapterLearningWorkspaceScreen(
    chapter: ChapterUiModel?,
    isBookmarked: Boolean = false,
    onBack: () -> Unit,
    onOpenDetailedNotes: () -> Unit,
    onOpenRevisionNotes: () -> Unit,
    onOpenMcqPractice: () -> Unit,
    onOpenSolvedQuestions: () -> Unit,
    onStartTopicPractice: (topicTitle: String) -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onAskAiTutor: () -> Unit = {},
    onReportIssue: () -> Unit = {},
    onToggleOfflineDownload: () -> Unit
) {
    val colors = LocalImtihanColors.current
    val progressValue = when (chapter?.status) {
        ChapterProgressStatus.COMPLETED -> 1f
        ChapterProgressStatus.IN_PROGRESS -> 0.65f
        else -> 0.15f
    }

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Chapter ${chapter?.chapterNumber ?: 1}",
                subtitle = chapter?.title ?: "Learning Workspace",
                onBack = onBack,
                showBottomDivider = true,
                actions = {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.defaultMinSize(
                            minWidth = ImtihanDimensions.minTouchTarget,
                            minHeight = ImtihanDimensions.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark chapter",
                            tint = if (isBookmarked) colors.warningGold else colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onReportIssue,
                        modifier = Modifier.defaultMinSize(
                            minWidth = ImtihanDimensions.minTouchTarget,
                            minHeight = ImtihanDimensions.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "Report content issue",
                            tint = colors.textSecondary
                        )
                    }
                }
            )

            LazyColumn(
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ImtihanHeroCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chapter ${chapter?.chapterNumber ?: 1} · ${chapter?.versionLabel ?: "v1.0"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFBFDBFE)
                            )
                            if (chapter != null && chapter.boardWeightageMarks > 0) {
                                Text(
                                    text = "${chapter.boardWeightageMarks} BISE Marks",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = chapter?.title ?: "Physical Quantities & Measurement",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!chapter?.subtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chapter!!.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                        if (!chapter?.sourceReference.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Source: ${chapter!!.sourceReference}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ImtihanProgressBar(
                            progress = progressValue,
                            trackColor = Color.White.copy(alpha = 0.24f),
                            fillColor = Color(0xFF7DD3FC)
                        )
                    }
                }

                // "What You'll Learn" learning outcome checklist + Start Learning primary CTA
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "What You'll Learn",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val outcomes = remember(chapter) {
                            if (chapter != null && chapter.topics.isNotEmpty()) {
                                chapter.topics.take(4).map { it.title }
                            } else {
                                listOf(
                                    "Core concepts & KPK board definitions",
                                    "Key formulas & mathematical derivations",
                                    "Exam-focused solved examples & MCQs"
                                )
                            }
                        }
                        outcomes.forEach { itemText ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = itemText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        ImtihanPrimaryButton(
                            text = "Start Learning",
                            leadingIcon = Icons.Outlined.PlayArrow,
                            onClick = onOpenDetailedNotes
                        )
                    }
                }

                // Interactive Topic Drills row
                if (chapter != null && chapter.topics.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Topic Drills (${chapter.topics.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chapter.topics.forEach { topic ->
                                    Surface(
                                        onClick = { onStartTopicPractice(topic.title) },
                                        shape = RoundedCornerShape(ImtihanRadii.chip),
                                        color = colors.cardSurface,
                                        border = BorderStroke(ImtihanDimensions.hairlineBorder, colors.cardBorder),
                                        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${topic.title} (${topic.estimatedMinutes}m)",
                                                style = MaterialTheme.typography.labelMedium.tabular(),
                                                color = colors.accentBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Grouped Related Resources Card
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Related Resources",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        SilverBlueCard(
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ImtihanNavigationRow(
                                title = "Detailed Notes",
                                subtitle = "Concepts, equations, tables & worked numericals",
                                icon = Icons.Outlined.Description,
                                onClick = onOpenDetailedNotes
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Revision Notes",
                                subtitle = "High-yield definitions, formulas & exam highlights",
                                icon = Icons.Outlined.Bolt,
                                iconTint = colors.warningGold,
                                onClick = onOpenRevisionNotes
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "MCQ Practice",
                                subtitle = "Timed practice questions with explanations",
                                icon = Icons.Outlined.Quiz,
                                trailingText = "${chapter?.mcqCount ?: 5} Qs",
                                onClick = onOpenMcqPractice
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Solved Questions",
                                subtitle = "Short, long & numerical board solutions",
                                icon = Icons.Outlined.QuestionAnswer,
                                iconTint = colors.cyanPulse,
                                trailingText = "${chapter?.solvedCount ?: 3} Solved",
                                onClick = onOpenSolvedQuestions
                            )
                        }
                    }
                }

                // Offline & AI Tutor Utility Actions
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onToggleOfflineDownload,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                            shape = RoundedCornerShape(ImtihanRadii.button),
                            border = BorderStroke(
                                1.dp,
                                if (chapter?.isDownloadedOffline == true) colors.success.copy(alpha = 0.55f) else colors.cardBorder
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                tint = if (chapter?.isDownloadedOffline == true) colors.success else colors.accentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (chapter?.isDownloadedOffline == true) "Offline Ready" else "Save Offline",
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = onAskAiTutor,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                            shape = RoundedCornerShape(ImtihanRadii.button),
                            border = BorderStroke(1.dp, colors.cardBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = colors.warningGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Tutor",
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 8: Detailed Notes
 */
@Composable
fun DetailedNotesScreen(
    chapterTitle: String,
    blocksState: UiResourceState<List<ContentBlockUi>>,
    isBookmarked: Boolean = false,
    chapterTitleUrdu: String? = null,
    nightBeforeSummary: String? = null,
    languageCode: String = "English",
    readerHighContrastMode: String = "OLED",
    feedback: ImtihanFeedbackController? = null,
    onToggleMedium: () -> Unit = {},
    onToggleContrastMode: () -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onAskAiTutor: () -> Unit = {},
    onReportIssue: () -> Unit = {},
    onRetryNotes: () -> Unit = {},
    onBack: () -> Unit
) {
    ReusableNotesScaffold(
        title = chapterTitle,
        subtitle = "Detailed Notes",
        blocksState = blocksState,
        isBookmarked = isBookmarked,
        chapterTitleUrdu = chapterTitleUrdu,
        nightBeforeSummary = nightBeforeSummary,
        languageCode = languageCode,
        readerHighContrastMode = readerHighContrastMode,
        feedback = feedback,
        onToggleMedium = onToggleMedium,
        onToggleContrastMode = onToggleContrastMode,
        onToggleBookmark = onToggleBookmark,
        onAskAiTutor = onAskAiTutor,
        onReportIssue = onReportIssue,
        onRetryNotes = onRetryNotes,
        onBack = onBack
    )
}

/**
 * Screen 9: Revision Notes
 */
@Composable
fun RevisionNotesScreen(
    chapterTitle: String,
    blocksState: UiResourceState<List<ContentBlockUi>>,
    isBookmarked: Boolean = false,
    chapterTitleUrdu: String? = null,
    nightBeforeSummary: String? = null,
    languageCode: String = "English",
    readerHighContrastMode: String = "OLED",
    feedback: ImtihanFeedbackController? = null,
    onToggleMedium: () -> Unit = {},
    onToggleContrastMode: () -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onAskAiTutor: () -> Unit = {},
    onReportIssue: () -> Unit = {},
    onRetryNotes: () -> Unit = {},
    onBack: () -> Unit
) {
    ReusableNotesScaffold(
        title = chapterTitle,
        subtitle = "Quick Revision Sheet",
        blocksState = blocksState,
        isBookmarked = isBookmarked,
        chapterTitleUrdu = chapterTitleUrdu,
        nightBeforeSummary = nightBeforeSummary,
        languageCode = languageCode,
        readerHighContrastMode = readerHighContrastMode,
        feedback = feedback,
        onToggleMedium = onToggleMedium,
        onToggleContrastMode = onToggleContrastMode,
        onToggleBookmark = onToggleBookmark,
        onAskAiTutor = onAskAiTutor,
        onReportIssue = onReportIssue,
        onRetryNotes = onRetryNotes,
        onBack = onBack
    )
}

/**
 * Screen 12: Solved Questions
 */
@Composable
fun SolvedQuestionsScreen(
    chapterTitle: String,
    blocksState: UiResourceState<List<ContentBlockUi>>,
    isBookmarked: Boolean = false,
    chapterTitleUrdu: String? = null,
    nightBeforeSummary: String? = null,
    languageCode: String = "English",
    readerHighContrastMode: String = "OLED",
    feedback: ImtihanFeedbackController? = null,
    onToggleMedium: () -> Unit = {},
    onToggleContrastMode: () -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onAskAiTutor: () -> Unit = {},
    onReportIssue: () -> Unit = {},
    onRetryNotes: () -> Unit = {},
    onBack: () -> Unit
) {
    val resolvedState = when (blocksState) {
        is UiResourceState.Success -> {
            val solved = blocksState.data.filter {
                it is ContentBlockUi.SolvedQuestion || it is ContentBlockUi.WorkedExample
            }
            if (solved.isEmpty()) {
                UiResourceState.Empty(
                    "No Solved Questions Published",
                    "Solved board questions and worked numericals for this chapter will sync when published."
                )
            } else {
                UiResourceState.Success(solved, blocksState.isOfflineFallback)
            }
        }
        else -> blocksState
    }

    ReusableNotesScaffold(
        title = chapterTitle,
        subtitle = "Solved Board Questions & Numericals",
        blocksState = resolvedState,
        isBookmarked = isBookmarked,
        chapterTitleUrdu = chapterTitleUrdu,
        nightBeforeSummary = nightBeforeSummary,
        languageCode = languageCode,
        readerHighContrastMode = readerHighContrastMode,
        feedback = feedback,
        onToggleMedium = onToggleMedium,
        onToggleContrastMode = onToggleContrastMode,
        onToggleBookmark = onToggleBookmark,
        onAskAiTutor = onAskAiTutor,
        onReportIssue = onReportIssue,
        onRetryNotes = onRetryNotes,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReusableNotesScaffold(
    title: String,
    subtitle: String,
    blocksState: UiResourceState<List<ContentBlockUi>>,
    isBookmarked: Boolean,
    chapterTitleUrdu: String? = null,
    nightBeforeSummary: String? = null,
    languageCode: String = "English",
    readerHighContrastMode: String = "OLED",
    feedback: ImtihanFeedbackController? = null,
    onToggleMedium: () -> Unit = {},
    onToggleContrastMode: () -> Unit = {},
    onToggleBookmark: () -> Unit,
    onAskAiTutor: () -> Unit,
    onReportIssue: () -> Unit,
    onRetryNotes: () -> Unit = {},
    onBack: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var fontScale by remember { mutableFloatStateOf(1f) }
    var nightBeforeOnly by remember { mutableStateOf(false) }
    var focusIndex by remember { mutableIntStateOf(0) } // 0=All Notes, 1=Formulas, 2=Diagrams
    var showReaderSettings by remember { mutableStateOf(false) }

    val isUrdu = languageCode.equals("Urdu", ignoreCase = true)
    val isPaperContrast = readerHighContrastMode == "PAPER"
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val blockFocusMode = when (focusIndex) {
        1 -> "FORMULAS"
        2 -> "DIAGRAMS"
        else -> "ALL"
    }

    val blocks = (blocksState as? UiResourceState.Success)?.data.orEmpty()
    val sectionAnchorIndices = remember(blocks) {
        val anchors = blocks.mapIndexedNotNull { idx, b ->
            if (b is ContentBlockUi.Heading && !b.isSubheading) idx else null
        }
        if (anchors.isNotEmpty()) anchors else if (blocks.isNotEmpty()) listOf(0) else emptyList()
    }
    var currentSectionIdx by remember(sectionAnchorIndices) { mutableIntStateOf(0) }
    val totalSections = maxOf(1, sectionAnchorIndices.size)

    fun jumpToSection(nextIdx: Int) {
        if (sectionAnchorIndices.isEmpty()) return
        val clamped = nextIdx.coerceIn(0, sectionAnchorIndices.lastIndex)
        currentSectionIdx = clamped
        val blockPos = sectionAnchorIndices[clamped]
        scope.launch {
            listState.animateScrollToItem(blockPos)
        }
    }

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            // Streamlined TopBar: keeps title completely readable on 360dp phones
            ImtihanTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                showBottomDivider = true,
                actions = {
                    IconButton(
                        onClick = onAskAiTutor,
                        modifier = Modifier.defaultMinSize(
                            minWidth = ImtihanDimensions.minTouchTarget,
                            minHeight = ImtihanDimensions.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Ask AI Tutor",
                            tint = colors.warningGold
                        )
                    }
                    IconButton(
                        onClick = {
                            feedback?.tap()
                            onToggleBookmark()
                        },
                        modifier = Modifier.defaultMinSize(
                            minWidth = ImtihanDimensions.minTouchTarget,
                            minHeight = ImtihanDimensions.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark notes",
                            tint = if (isBookmarked) colors.warningGold else colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            feedback?.tap()
                            showReaderSettings = true
                        },
                        modifier = Modifier.defaultMinSize(
                            minWidth = ImtihanDimensions.minTouchTarget,
                            minHeight = ImtihanDimensions.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Reader comfort settings",
                            tint = colors.textPrimary
                        )
                    }
                }
            )

            // Segmented Focus Filter + Night-Before indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ImtihanSpacing.md, vertical = ImtihanSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImtihanSegmentedControl(
                    options = listOf("All Notes", "Formulas", "Diagrams"),
                    selectedIndex = focusIndex,
                    onSelect = {
                        feedback?.tap()
                        focusIndex = it
                    }
                )
                AnimatedVisibility(visible = nightBeforeOnly) {
                    Surface(
                        onClick = {
                            feedback?.tap()
                            nightBeforeOnly = false
                        },
                        shape = RoundedCornerShape(ImtihanRadii.chip),
                        color = colors.warningGold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, colors.warningGold.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NightsStay,
                                    contentDescription = null,
                                    tint = colors.warningGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Night-Before High-Yield Filter Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary
                                )
                            }
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.warningGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isPaperContrast) {
                            Modifier.background(colors.cardElevated.copy(alpha = 0.65f))
                        } else {
                            Modifier
                        }
                    ),
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isUrdu && !chapterTitleUrdu.isNullOrBlank()) {
                    item {
                        SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                            ImtihanStatusBadge(text = "Urdu Nastaliq Title", tint = colors.cyanPulse)
                            Spacer(modifier = Modifier.height(6.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = chapterTitleUrdu,
                                    style = urduTextStyle(sizeSp = 19f, bold = true),
                                    color = colors.textPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (nightBeforeOnly && !nightBeforeSummary.isNullOrBlank()) {
                    item {
                        SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                            ImtihanStatusBadge(text = "Night-Before Exam Briefing", tint = colors.warningGold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = nightBeforeSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                when (blocksState) {
                    is UiResourceState.Loading -> item { ImtihanLoadingView(showSkeletons = true) }
                    is UiResourceState.Error -> item { ImtihanErrorView(blocksState.message, onRetry = onRetryNotes) }
                    is UiResourceState.Empty -> item { ImtihanEmptyView(blocksState.title, blocksState.description) }
                    is UiResourceState.Success -> {
                        val filteredBlocks = when (blockFocusMode) {
                            "FORMULAS" -> blocksState.data.filter {
                                it is ContentBlockUi.MathExpression || it is ContentBlockUi.ImportantDefinition
                            }
                            "DIAGRAMS" -> blocksState.data.filter { it is ContentBlockUi.Diagram }
                            else -> blocksState.data
                        }
                        if (filteredBlocks.isEmpty()) {
                            item {
                                ImtihanEmptyView(
                                    title = "No ${blockFocusMode.lowercase()} in this chapter",
                                    description = "Switch back to All Notes to inspect the full chapter content.",
                                    actionLabel = "View All Notes",
                                    onAction = { focusIndex = 0 }
                                )
                            }
                        } else {
                            itemsIndexed(filteredBlocks, key = { _, b -> b.id }) { _, block ->
                                ContentBlockRenderer(
                                    blocks = listOf(block),
                                    fontScaleMultiplier = fontScale,
                                    nightBeforeOnly = nightBeforeOnly && blockFocusMode == "ALL",
                                    onFormulaCopied = { feedback?.tap() }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Section Pager Bar
            Surface(
                color = colors.cardSurface,
                border = BorderStroke(ImtihanDimensions.hairlineBorder, colors.cardBorder.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ImtihanSpacing.md, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { jumpToSection(currentSectionIdx - 1) },
                        enabled = currentSectionIdx > 0,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(ImtihanRadii.button)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Prev")
                    }
                    Text(
                        text = if (sectionAnchorIndices.isEmpty()) "0 / 0" else "Section ${currentSectionIdx + 1} / $totalSections",
                        style = MaterialTheme.typography.labelMedium.tabular(),
                        color = colors.textSecondary
                    )
                    Button(
                        onClick = { jumpToSection(currentSectionIdx + 1) },
                        enabled = currentSectionIdx + 1 < totalSections,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(ImtihanRadii.button),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showReaderSettings) {
        ModalBottomSheet(
            onDismissRequest = { showReaderSettings = false },
            containerColor = colors.cardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ImtihanSpacing.md)
                    .padding(bottom = ImtihanSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Reading Comfort & Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                // Text Size Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Text Scale",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${(fontScale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.tabular(),
                            color = colors.accentBlue
                        )
                    }
                    Slider(
                        value = fontScale,
                        onValueChange = { fontScale = it },
                        valueRange = 0.85f..1.35f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accentBlue,
                            activeTrackColor = colors.accentBlue
                        )
                    )
                }

                // Night-Before Focus Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Switch) {
                            feedback?.tap()
                            nightBeforeOnly = !nightBeforeOnly
                        }
                        .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.NightsStay, contentDescription = null, tint = colors.warningGold)
                        Column {
                            Text("Night-Before Exam Focus", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text("Highlight only definitions, formulas & key points", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                    Switch(checked = nightBeforeOnly, onCheckedChange = null)
                }

                HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))

                ImtihanNavigationRow(
                    title = if (isUrdu) "Language: اردو Nastaliq" else "Language: English Medium",
                    subtitle = "Switch bilingual curriculum view",
                    icon = Icons.Outlined.Translate,
                    iconTint = colors.cyanPulse,
                    trailingText = if (isUrdu) "UR" else "EN",
                    onClick = {
                        feedback?.tap()
                        onToggleMedium()
                    }
                )
                HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))

                ImtihanNavigationRow(
                    title = if (isPaperContrast) "Contrast: High-Contrast Paper" else "Contrast: Silver Blue OLED",
                    subtitle = "Optimize surface glare for day or night reading",
                    icon = Icons.Outlined.Contrast,
                    onClick = {
                        feedback?.tap()
                        onToggleContrastMode()
                    }
                )
                HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))

                ImtihanNavigationRow(
                    title = "Report Content Issue",
                    subtitle = "Flag a typo, formula error, or broken diagram",
                    icon = Icons.Outlined.Flag,
                    iconTint = colors.danger,
                    onClick = {
                        showReaderSettings = false
                        onReportIssue()
                    }
                )
            }
        }
    }
}
