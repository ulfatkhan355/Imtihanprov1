package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.ContentBlockUi
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.components.ContentBlockRenderer
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanErrorView
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import kotlinx.coroutines.launch

/**
 * Screen 7: Chapter Learning Workspace (Topics, Notes, Revision, MCQs, Solved, AI Tutor, Bookmark, Offline Pack, Feedback)
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

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = chapter?.title ?: "Learning Workspace",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark chapter",
                        tint = if (isBookmarked) colors.warningGold else colors.textPrimary
                    )
                }
                IconButton(onClick = onReportIssue) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Report content issue",
                        tint = colors.textSecondary
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Chapter ${chapter?.chapterNumber ?: 1} · ${chapter?.versionLabel ?: "v1.0"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accentBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chapter?.title ?: "Physical Quantities & Measurement",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chapter?.subtitle ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        if (!chapter?.sourceReference.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Source: ${chapter!!.sourceReference}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onToggleOfflineDownload,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    tint = if (chapter?.isDownloadedOffline == true) colors.success else colors.accentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.padding(3.dp))
                                Text(
                                    text = if (chapter?.isDownloadedOffline == true) "Offline Ready" else "Save Offline",
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = onAskAiTutor,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.warningGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.padding(3.dp))
                                Text("AI Tutor", color = colors.textPrimary, maxLines = 1)
                            }
                        }
                    }
                }

                if (chapter != null && chapter.topics.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.cardSurface,
                                        border = BorderStroke(1.dp, colors.cardBorder)
                                    ) {
                                        Text(
                                            text = "${topic.title} (${topic.estimatedMinutes}m)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.accentBlue,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    WorkspaceOptionCard(
                        title = "Detailed Notes",
                        subtitle = "Complete concepts, equations, tables, diagrams & worked numericals",
                        icon = Icons.Outlined.Description,
                        onClick = onOpenDetailedNotes
                    )
                }
                item {
                    WorkspaceOptionCard(
                        title = "Revision Notes",
                        subtitle = "High-yield definitions, formulas & KPK exam highlights",
                        icon = Icons.Outlined.Bolt,
                        onClick = onOpenRevisionNotes
                    )
                }
                item {
                    WorkspaceOptionCard(
                        title = "MCQ Practice",
                        subtitle = "${chapter?.mcqCount ?: 5} interactive timed questions with explanations",
                        icon = Icons.Outlined.Quiz,
                        onClick = onOpenMcqPractice
                    )
                }
                item {
                    WorkspaceOptionCard(
                        title = "Solved Questions",
                        subtitle = "${chapter?.solvedCount ?: 3} short, long & numerical board questions",
                        icon = Icons.Outlined.QuestionAnswer,
                        onClick = onOpenSolvedQuestions
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalImtihanColors.current
    SilverBlueCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.accentBlue)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
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
    onToggleBookmark: () -> Unit = {},
    onAskAiTutor: () -> Unit = {},
    onReportIssue: () -> Unit = {},
    onBack: () -> Unit
) {
    ReusableNotesScaffold(
        title = chapterTitle,
        subtitle = "Detailed Notes",
        blocksState = blocksState,
        isBookmarked = isBookmarked,
        onToggleBookmark = onToggleBookmark,
        onAskAiTutor = onAskAiTutor,
        onReportIssue = onReportIssue,
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
    onToggleBookmark: () -> Unit = {},
    onAskAiTutor: () -> Unit = {},
    onReportIssue: () -> Unit = {},
    onBack: () -> Unit
) {
    ReusableNotesScaffold(
        title = chapterTitle,
        subtitle = "Quick Revision Sheet",
        blocksState = blocksState,
        isBookmarked = isBookmarked,
        onToggleBookmark = onToggleBookmark,
        onAskAiTutor = onAskAiTutor,
        onReportIssue = onReportIssue,
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
    onToggleBookmark: () -> Unit = {},
    onAskAiTutor: () -> Unit = {},
    onReportIssue: () -> Unit = {},
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
        onToggleBookmark = onToggleBookmark,
        onAskAiTutor = onAskAiTutor,
        onReportIssue = onReportIssue,
        onBack = onBack
    )
}

@Composable
private fun ReusableNotesScaffold(
    title: String,
    subtitle: String,
    blocksState: UiResourceState<List<ContentBlockUi>>,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onAskAiTutor: () -> Unit,
    onReportIssue: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var fontScale by remember { mutableFloatStateOf(1f) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accentBlue,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onAskAiTutor) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Ask AI Tutor",
                        tint = colors.warningGold
                    )
                }
                IconButton(onClick = { fontScale = if (fontScale > 1.15f) 1f else fontScale + 0.1f }) {
                    Icon(
                        imageVector = Icons.Outlined.FormatSize,
                        contentDescription = "Adjust text size",
                        tint = colors.textPrimary
                    )
                }
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark notes",
                        tint = if (isBookmarked) colors.warningGold else colors.textPrimary
                    )
                }
                IconButton(onClick = onReportIssue) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Report error",
                        tint = colors.textSecondary
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (blocksState) {
                    is UiResourceState.Loading -> item { ImtihanLoadingView() }
                    is UiResourceState.Error -> item { ImtihanErrorView(blocksState.message, onRetry = {}) }
                    is UiResourceState.Empty -> item { ImtihanEmptyView(blocksState.title, blocksState.description) }
                    is UiResourceState.Success -> {
                        itemsIndexed(blocksState.data, key = { _, b -> b.id }) { _, block ->
                            ContentBlockRenderer(
                                blocks = listOf(block),
                                fontScaleMultiplier = fontScale
                            )
                        }
                    }
                }
            }

            Surface(
                color = colors.canvasDeep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { jumpToSection(currentSectionIdx - 1) },
                        enabled = currentSectionIdx > 0
                    ) {
                        Text("< Prev")
                    }
                    Text(
                        text = if (sectionAnchorIndices.isEmpty()) "0 / 0" else "Section ${currentSectionIdx + 1} / $totalSections",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Button(
                        onClick = { jumpToSection(currentSectionIdx + 1) },
                        enabled = currentSectionIdx + 1 < totalSections,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                    ) {
                        Text("Next >")
                    }
                }
            }
        }
    }
}
