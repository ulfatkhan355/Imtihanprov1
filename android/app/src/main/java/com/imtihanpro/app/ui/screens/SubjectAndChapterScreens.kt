package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.ChapterProgressStatus
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.PdfResourceUi
import com.imtihanpro.app.domain.model.SubjectUiModel
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanErrorView
import com.imtihanpro.app.ui.components.ImtihanHeroCard
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanNavigationRow
import com.imtihanpro.app.ui.components.ImtihanPrimaryButton
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.ImtihanSecondaryButton
import com.imtihanpro.app.ui.components.ImtihanSegmentedControl
import com.imtihanpro.app.ui.components.ImtihanStatusBadge
import com.imtihanpro.app.ui.components.ImtihanTopBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.components.SubjectVectorIcon
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.tabular
import com.imtihanpro.app.ui.theme.urduTextStyle

/**
 * Screen 4: Subject Listing (Curated 2-column grid with category filter pills & full-scroll grid)
 */
@Composable
fun SubjectListingScreen(
    classLevel: Int = 11,
    studyGroupCode: String = "PRE_MEDICAL",
    subjectsState: UiResourceState<List<SubjectUiModel>>,
    onSelectSubject: (SubjectUiModel) -> Unit,
    onOpenMotivation: () -> Unit = {},
    onRetrySubjects: () -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    val groupLabel = Stage3To8Engines.STUDY_GROUPS.firstOrNull { it.code == studyGroupCode }?.label ?: studyGroupCode
    var selectedFilter by remember { mutableStateOf("All") }

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Class $classLevel Subjects",
                subtitle = "$groupLabel · KPK BISE",
                showBottomDivider = true
            )

            when (subjectsState) {
                is UiResourceState.Loading -> ImtihanLoadingView(showSkeletons = true)
                is UiResourceState.Error -> Box(modifier = Modifier.padding(ImtihanSpacing.md)) {
                    ImtihanErrorView(subjectsState.message, onRetry = onRetrySubjects)
                }
                is UiResourceState.Empty -> Box(modifier = Modifier.padding(ImtihanSpacing.md)) {
                    ImtihanEmptyView(subjectsState.title, subjectsState.description)
                }
                is UiResourceState.Success -> {
                    val allSubjects = subjectsState.data
                    val filterOptions = listOf("All", "Science", "Languages", "Active")
                    val filteredSubjects = remember(allSubjects, selectedFilter) {
                        when (selectedFilter) {
                            "Science" -> allSubjects.filter {
                                it.iconKey.lowercase() in setOf("physics", "chemistry", "math", "biology", "cs")
                            }
                            "Languages" -> allSubjects.filter {
                                it.iconKey.lowercase() in setOf("english", "urdu", "islamiat")
                            }
                            "Active" -> allSubjects.filter { it.hasSeedContent }
                            else -> allSubjects
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(ImtihanSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filterOptions.forEach { filter ->
                                    val selected = selectedFilter == filter
                                    Surface(
                                        onClick = { selectedFilter = filter },
                                        shape = RoundedCornerShape(ImtihanRadii.chip),
                                        color = if (selected) colors.accentBlue else colors.cardSurface,
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
                                                text = filter,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) Color.White else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredSubjects.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                ImtihanEmptyView(
                                    title = "No matching subjects",
                                    description = "Switch back to All to inspect every subject in your stream.",
                                    actionLabel = "Show All",
                                    onAction = { selectedFilter = "All" }
                                )
                            }
                        } else {
                            items(filteredSubjects, key = { it.id }) { subject ->
                                SilverBlueCard(
                                    onClick = { onSelectSubject(subject) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(ImtihanRadii.chip),
                                            color = colors.accentBlue.copy(alpha = 0.14f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                SubjectVectorIcon(
                                                    iconKey = subject.iconKey,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${subject.progressPercent}%",
                                            style = MaterialTheme.typography.labelMedium.tabular(),
                                            fontWeight = FontWeight.Bold,
                                            color = colors.cyanPulse
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${subject.totalChapters} Chapters",
                                        style = MaterialTheme.typography.bodySmall.tabular(),
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ImtihanProgressBar(progress = subject.progressPercent / 100f)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ImtihanStatusBadge(
                                            text = if (subject.hasSeedContent) "Study Ready" else "Syllabus",
                                            tint = if (subject.hasSeedContent) colors.success else colors.textMuted
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SilverBlueCard(
                                elevated = true,
                                onClick = onOpenMotivation,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(ImtihanRadii.chip),
                                        color = colors.warningGold.copy(alpha = 0.16f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.EmojiEvents,
                                                contentDescription = null,
                                                tint = colors.warningGold,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "XP, Badges & Weekly Challenges",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Track your board mastery milestones honestly.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colors.textMuted
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

/**
 * Screen 5: Subject Details (Hero + Chapters / Books & Papers Segmented Control + Subject Mock CTA)
 */
@Composable
fun SubjectDetailsScreen(
    subject: SubjectUiModel?,
    chaptersState: UiResourceState<List<ChapterUiModel>>,
    subjectPdfs: List<PdfResourceUi> = emptyList(),
    languageCode: String = "English",
    offlinePackStatusMessage: String? = null,
    onBack: () -> Unit,
    onOpenChapter: (ChapterUiModel) -> Unit,
    onOpenAllChapters: () -> Unit,
    onOpenPastPapers: () -> Unit,
    onOpenPdf: (PdfResourceUi) -> Unit = {},
    onStartSubjectMock: () -> Unit = {},
    onDownloadOfflinePack: () -> Unit = {},
    onClearOfflinePackStatus: () -> Unit = {},
    onRetryChapters: () -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val chaptersList = (chaptersState as? UiResourceState.Success)?.data.orEmpty()
    val biseSummary = remember(chaptersList) {
        Stage3To8Engines.calculateBiseWeightageSummary(chaptersList)
    }

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = subject?.name ?: "Subject Details",
                subtitle = "${subject?.boardCode ?: "KPK"} Class ${subject?.classLevel ?: 11}",
                onBack = onBack,
                showBottomDivider = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ImtihanHeroCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    SubjectVectorIcon(
                                        iconKey = subject?.iconKey ?: "physics",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subject?.name ?: "Physics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${subject?.totalChapters ?: 9} Chapters · ${subject?.progressPercent ?: 0}% Mastered",
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        if (!subject?.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = subject!!.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        ImtihanProgressBar(
                            progress = (subject?.progressPercent ?: 0) / 100f,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            fillColor = Color(0xFF7DD3FC)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onStartSubjectMock,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                shape = RoundedCornerShape(ImtihanRadii.button),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Outlined.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subject Mock", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = onDownloadOfflinePack,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                shape = RoundedCornerShape(ImtihanRadii.button),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1D4ED8)
                                )
                            ) {
                                Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save (${"%.1f".format(subject?.offlinePackSizeMb ?: 0.6)} MB)",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (!offlinePackStatusMessage.isNullOrBlank()) {
                    item {
                        Surface(
                            onClick = onClearOfflinePackStatus,
                            shape = RoundedCornerShape(ImtihanRadii.button),
                            color = colors.success.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, colors.success.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$offlinePackStatusMessage · Tap to dismiss",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.success,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (!subject?.pairingSchemeSummary.isNullOrBlank() || biseSummary.totalMappedMarks > 0) {
                    item {
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "KPK BISE Pairing Scheme",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                                if (biseSummary.totalMappedMarks > 0) {
                                    ImtihanStatusBadge(
                                        text = "${biseSummary.totalMappedMarks} Marks · ${biseSummary.highYieldChaptersCount} High-Yield",
                                        tint = colors.cyanPulse
                                    )
                                }
                            }
                            if (!subject?.pairingSchemeSummary.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = subject!!.pairingSchemeSummary!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    ImtihanSegmentedControl(
                        options = listOf("Chapters", "Books & Papers"),
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it }
                    )
                }

                if (selectedTab == 0) {
                    when (chaptersState) {
                        is UiResourceState.Loading -> item { ImtihanLoadingView(showSkeletons = true) }
                        is UiResourceState.Error -> item { ImtihanErrorView(chaptersState.message, onRetry = onRetryChapters) }
                        is UiResourceState.Empty -> item { ImtihanEmptyView(chaptersState.title, chaptersState.description) }
                        is UiResourceState.Success -> {
                            items(chaptersState.data, key = { it.id }) { chapter ->
                                ChapterRowCard(
                                    chapter = chapter,
                                    preferUrdu = languageCode.equals("Urdu", ignoreCase = true),
                                    onClick = { onOpenChapter(chapter) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                ImtihanPrimaryButton(
                                    text = "Browse All Chapters",
                                    onClick = onOpenAllChapters
                                )
                            }
                        }
                    }
                } else {
                    if (subjectPdfs.isNotEmpty()) {
                        items(subjectPdfs, key = { it.id }) { pdf ->
                            SilverBlueCard(onClick = { onOpenPdf(pdf) }, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(ImtihanRadii.chip),
                                        color = colors.accentBlue.copy(alpha = 0.14f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.MenuBook,
                                                contentDescription = null,
                                                tint = colors.accentBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pdf.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pdf.category.replace('_', ' ')} · ${pdf.pageCount} pages · ${"%.1f".format(pdf.sizeMb)} MB",
                                            style = MaterialTheme.typography.bodySmall.tabular(),
                                            color = colors.textSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SilverBlueCard(contentPadding = PaddingValues(0.dp), modifier = Modifier.fillMaxWidth()) {
                            ImtihanNavigationRow(
                                title = "Board Past Papers & PDF Library",
                                subtitle = "Solved & original KPK BISE papers, textbooks & pairing schemes",
                                icon = Icons.Outlined.MenuBook,
                                onClick = onOpenPastPapers
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 6: Chapter Listing
 */
@Composable
fun ChapterListingScreen(
    subjectTitle: String,
    chaptersState: UiResourceState<List<ChapterUiModel>>,
    onBack: () -> Unit,
    onSelectChapter: (ChapterUiModel) -> Unit,
    onRetryChapters: () -> Unit = {}
) {
    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "$subjectTitle Chapters",
                subtitle = "Select a chapter to start",
                onBack = onBack,
                showBottomDivider = true
            )

            when (chaptersState) {
                is UiResourceState.Loading -> ImtihanLoadingView(showSkeletons = true)
                is UiResourceState.Error -> Box(modifier = Modifier.padding(ImtihanSpacing.md)) {
                    ImtihanErrorView(chaptersState.message, onRetry = onRetryChapters)
                }
                is UiResourceState.Empty -> Box(modifier = Modifier.padding(ImtihanSpacing.md)) {
                    ImtihanEmptyView(chaptersState.title, chaptersState.description)
                }
                is UiResourceState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(ImtihanSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chaptersState.data, key = { it.id }) { chapter ->
                            ChapterRowCard(chapter = chapter, onClick = { onSelectChapter(chapter) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRowCard(
    chapter: ChapterUiModel,
    preferUrdu: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalImtihanColors.current
    val statusIcon = when (chapter.status) {
        ChapterProgressStatus.COMPLETED -> Icons.Outlined.CheckCircle
        ChapterProgressStatus.IN_PROGRESS -> Icons.Outlined.PlayCircle
        ChapterProgressStatus.LOCKED -> Icons.Outlined.Lock
        ChapterProgressStatus.AVAILABLE -> Icons.Outlined.PlayCircle
    }
    val statusTint = when (chapter.status) {
        ChapterProgressStatus.COMPLETED -> colors.success
        ChapterProgressStatus.IN_PROGRESS -> colors.accentBlue
        ChapterProgressStatus.LOCKED -> colors.textMuted
        ChapterProgressStatus.AVAILABLE -> colors.accentBlue
    }
    val statusText = when (chapter.status) {
        ChapterProgressStatus.COMPLETED -> "Completed"
        ChapterProgressStatus.IN_PROGRESS -> "In Progress"
        ChapterProgressStatus.LOCKED -> "Preview Draft"
        ChapterProgressStatus.AVAILABLE -> "Ready"
    }

    SilverBlueCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(ImtihanRadii.chip),
                color = statusTint.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusText,
                        tint = statusTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${chapter.chapterNumber}. ${chapter.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (preferUrdu && !chapter.titleUrdu.isNullOrBlank()) {
                    Text(
                        text = chapter.titleUrdu,
                        style = urduTextStyle(sizeSp = 15f),
                        color = colors.cyanPulse
                    )
                }
                val marksPart = if (chapter.boardWeightageMarks > 0) " · ${chapter.boardWeightageMarks} Marks" else ""
                val topicsPart = if (chapter.topics.isNotEmpty()) " · ${chapter.topics.size} topics" else ""
                Text(
                    text = "$statusText · ${chapter.mcqCount} MCQs$topicsPart$marksPart",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.textSecondary
                )
                if (!chapter.pairingHint.isNullOrBlank()) {
                    Text(
                        text = chapter.pairingHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
