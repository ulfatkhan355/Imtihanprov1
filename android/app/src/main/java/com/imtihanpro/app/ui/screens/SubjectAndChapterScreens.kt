package com.imtihanpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Quiz
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.CloudDownload
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.ChapterProgressStatus
import com.imtihanpro.app.domain.model.ChapterUiModel
import com.imtihanpro.app.domain.model.PdfResourceUi
import com.imtihanpro.app.domain.model.SubjectUiModel
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanErrorView
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.components.SubjectVectorIcon
import com.imtihanpro.app.ui.components.ZeroPillMetaLine
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.urduTextStyle

/**
 * Screen 4: Subject Listing (2-column grid matching Silver Blue reference + Study Group label)
 */
@Composable
fun SubjectListingScreen(
    classLevel: Int = 9,
    studyGroupCode: String = "SCIENCE_BIOLOGY",
    subjectsState: UiResourceState<List<SubjectUiModel>>,
    onSelectSubject: (SubjectUiModel) -> Unit,
    onOpenMotivation: () -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    val groupLabel = Stage3To8Engines.STUDY_GROUPS.firstOrNull { it.code == studyGroupCode }?.label ?: studyGroupCode

    SilverBlueCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Class $classLevel Subjects",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                text = "Track: $groupLabel",
                style = MaterialTheme.typography.bodySmall,
                color = colors.accentBlue
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (subjectsState) {
                is UiResourceState.Loading -> ImtihanLoadingView()
                is UiResourceState.Error -> ImtihanErrorView(subjectsState.message, onRetry = {})
                is UiResourceState.Empty -> ImtihanEmptyView(subjectsState.title, subjectsState.description)
                is UiResourceState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(subjectsState.data, key = { it.id }) { subject ->
                            SilverBlueCard(
                                onClick = { onSelectSubject(subject) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SubjectVectorIcon(
                                        iconKey = subject.iconKey,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "${subject.progressPercent}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.cyanPulse
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${subject.totalChapters} Chapters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ImtihanProgressBar(progress = subject.progressPercent / 100f)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${subject.progressPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = if (subject.hasSeedContent) "Active" else "Syllabus",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (subject.hasSeedContent) colors.success else colors.textMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SilverBlueCard(
                        elevated = true,
                        onClick = onOpenMotivation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = colors.warningGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "View XP, Badges & Challenges",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Track your board mastery milestones honestly.",
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
}

/**
 * Screen 5: Subject Details (Header + Chapters / Resources Tabs + Subject Mock Exam CTA)
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
    onClearOfflinePackStatus: () -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val chaptersList = (chaptersState as? UiResourceState.Success)?.data.orEmpty()
    val biseSummary = remember(chaptersList) {
        Stage3To8Engines.calculateBiseWeightageSummary(chaptersList)
    }

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
                    text = subject?.name ?: "Subject Details",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SubjectVectorIcon(
                                iconKey = subject?.iconKey ?: "physics",
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subject?.name ?: "Physics",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${subject?.totalChapters ?: 9} Chapters · ${subject?.boardCode ?: "KPK"} Class ${subject?.classLevel ?: 9}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        if (!subject?.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = subject!!.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        if (!subject?.pairingSchemeSummary.isNullOrBlank() || biseSummary.totalMappedMarks > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.canvasDeep.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, colors.cyanPulse.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    ZeroPillMetaLine(
                                        text = "KPK BISE PAIRING · ${biseSummary.totalMappedMarks} MAPPED MARKS (${biseSummary.highYieldChaptersCount} HIGH-YIELD CH)"
                                    )
                                    if (!subject?.pairingSchemeSummary.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subject!!.pairingSchemeSummary!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        ImtihanProgressBar(progress = (subject?.progressPercent ?: 0) / 100f)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onStartSubjectMock,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Subject Mock", maxLines = 1)
                            }
                            Button(
                                onClick = onDownloadOfflinePack,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                            ) {
                                Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Pack (${"%.1f".format(subject?.offlinePackSizeMb ?: 0.6)}M)", maxLines = 1)
                            }
                        }
                        if (!offlinePackStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                onClick = onClearOfflinePackStatus,
                                shape = RoundedCornerShape(10.dp),
                                color = colors.success.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, colors.success.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$offlinePackStatusMessage (Tap to dismiss)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.success,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = colors.cardSurface,
                        contentColor = colors.accentBlue
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Chapters") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Books & Papers") }
                        )
                    }
                }

                if (selectedTab == 0) {
                    when (chaptersState) {
                        is UiResourceState.Loading -> item { ImtihanLoadingView() }
                        is UiResourceState.Error -> item { ImtihanErrorView(chaptersState.message, onRetry = {}) }
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
                                Button(
                                    onClick = onOpenAllChapters,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                ) {
                                    Text("Browse All Chapters")
                                }
                            }
                        }
                    }
                } else {
                    if (subjectPdfs.isNotEmpty()) {
                        items(subjectPdfs, key = { it.id }) { pdf ->
                            SilverBlueCard(onClick = { onOpenPdf(pdf) }, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = colors.accentBlue)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pdf.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                        Text(
                                            "${pdf.category.replace('_', ' ')} · ${pdf.pageCount} pages · ${"%.1f".format(pdf.sizeMb)} MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SilverBlueCard(onClick = onOpenPastPapers, modifier = Modifier.fillMaxWidth()) {
                            Text("Board Past Papers & PDF Library", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text("Solved and original KPK BISE papers, textbooks & pairing schemes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
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
    onSelectChapter: (ChapterUiModel) -> Unit
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
                    text = "$subjectTitle Chapters",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
            }

            when (chaptersState) {
                is UiResourceState.Loading -> ImtihanLoadingView()
                is UiResourceState.Error -> ImtihanErrorView(chaptersState.message, onRetry = {})
                is UiResourceState.Empty -> ImtihanEmptyView(chaptersState.title, chaptersState.description)
                is UiResourceState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
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
                shape = RoundedCornerShape(10.dp),
                color = colors.cardElevated,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusText,
                    tint = statusTint,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${chapter.chapterNumber}. ${chapter.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                if (preferUrdu && !chapter.titleUrdu.isNullOrBlank()) {
                    Text(
                        text = chapter.titleUrdu,
                        style = urduTextStyle(sizeSp = 15f),
                        color = colors.cyanPulse
                    )
                }
                val marksPart = if (chapter.boardWeightageMarks > 0) " · ${chapter.boardWeightageMarks} BISE Marks" else ""
                val topicsPart = if (chapter.topics.isNotEmpty()) " · ${chapter.topics.size} topics" else ""
                Text(
                    text = "$statusText · ${chapter.mcqCount} MCQs$topicsPart$marksPart",
                    style = MaterialTheme.typography.bodySmall,
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
                tint = colors.textSecondary
            )
        }
    }
}
