package com.imtihanpro.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.imtihanpro.app.R
import com.imtihanpro.app.domain.engine.DownloadPausedReason
import com.imtihanpro.app.domain.engine.DownloadState
import com.imtihanpro.app.domain.engine.PdfDownloadEngine
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.AdPlacementPolicyUi
import com.imtihanpro.app.domain.model.DownloadTaskUi
import com.imtihanpro.app.domain.model.DownloadedFileUi
import com.imtihanpro.app.domain.model.PastPaperUi
import com.imtihanpro.app.domain.model.PdfAnnotationUi
import com.imtihanpro.app.domain.model.PdfBookmarkUi
import com.imtihanpro.app.domain.model.PdfResourceUi
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanErrorView
import com.imtihanpro.app.ui.components.ImtihanHeroCard
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanNavigationRow
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.ImtihanSegmentedControl
import com.imtihanpro.app.ui.components.ImtihanStatusBadge
import com.imtihanpro.app.ui.components.ImtihanTopBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.reader.ImtihanPdfReader
import com.imtihanpro.app.ui.theme.ImtihanDimensions
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.ImtihanSpacing
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.theme.tabular
import com.imtihanpro.app.ui.viewmodel.AppShellState
import com.imtihanpro.app.ui.viewmodel.PdfViewerUiState

/**
 * Screen 13: Papers, Textbooks & PDF Study Library — matches reference screen #11
 */
@Composable
fun PastPapersScreen(
    papersState: UiResourceState<List<PastPaperUi>>,
    pdfLibrary: List<PdfResourceUi> = emptyList(),
    downloadTasks: List<DownloadTaskUi> = emptyList(),
    solvedFilter: Boolean?,
    selectedCategory: String? = null,
    adPolicy: AdPlacementPolicyUi = AdPlacementPolicyUi(),
    bannersShownToday: Int = 0,
    onSetFilter: (Boolean?) -> Unit,
    onSelectCategory: (String?) -> Unit = {},
    onOpenPaper: (PastPaperUi) -> Unit,
    onOpenPdfResource: (PdfResourceUi) -> Unit = {},
    onToggleDownloadPaper: (PastPaperUi) -> Unit,
    onToggleDownloadPdf: (PdfResourceUi) -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onRetryDownload: (String) -> Unit = {},
    onDownloadOnMobileData: (String) -> Unit = {},
    onBannerDisplayed: () -> Unit = {},
    onRetryPapers: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val colors = LocalImtihanColors.current
    val context = LocalContext.current
    var pendingDownloadAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var askedNotificationThisSession by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingDownloadAction?.invoke()
        pendingDownloadAction = null
    }

    fun startDownloadWithNotificationRationale(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotificationThisSession) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                pendingDownloadAction = action
                return
            }
        }
        action()
    }

    val showPapersAd = Stage3To8Engines.shouldDisplayAd(
        policy = adPolicy,
        surface = "papers",
        isInActiveQuizOrExam = false,
        isReadingNotesOrPdf = false,
        bannersShownToday = bannersShownToday
    )

    LaunchedEffect(showPapersAd) {
        if (showPapersAd) onBannerDisplayed()
    }

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Papers & Study Library",
                subtitle = "KPK BISE Past Papers, Textbooks & Guides",
                onBack = onBack,
                showBottomDivider = true
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ImtihanSpacing.md)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipButton("Past Papers", selectedCategory == null || selectedCategory == "past_paper") {
                        onSelectCategory(null)
                    }
                    FilterChipButton("Textbooks", selectedCategory == "textbook") {
                        onSelectCategory("textbook")
                    }
                    FilterChipButton("Notes Packs", selectedCategory == "notes_pack") {
                        onSelectCategory("notes_pack")
                    }
                    FilterChipButton("Syllabi", selectedCategory == "syllabus") {
                        onSelectCategory("syllabus")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (selectedCategory == null || selectedCategory == "past_paper") {
                    val segmentIndex = when (solvedFilter) {
                        null -> 0
                        false -> 1
                        true -> 2
                    }
                    ImtihanSegmentedControl(
                        options = listOf("All", "Original", "Solved"),
                        selectedIndex = segmentIndex,
                        onSelect = { idx ->
                            when (idx) {
                                0 -> onSetFilter(null)
                                1 -> onSetFilter(false)
                                else -> onSetFilter(true)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    when (papersState) {
                        is UiResourceState.Loading -> ImtihanLoadingView(showSkeletons = true)
                        is UiResourceState.Error -> ImtihanErrorView(papersState.message, onRetry = onRetryPapers)
                        is UiResourceState.Empty -> ImtihanEmptyView(papersState.title, papersState.description)
                        is UiResourceState.Success -> {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(papersState.data, key = { it.id }) { paper ->
                                    val targetPdfId = paper.pdfMetadataId.ifBlank { paper.id }
                                    val task = downloadTasks.firstOrNull { it.pdfId == targetPdfId }
                                    SilverBlueCard(
                                        onClick = { onOpenPaper(paper) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(ImtihanRadii.chip),
                                                    color = colors.danger.copy(alpha = 0.14f),
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.PictureAsPdf,
                                                            contentDescription = null,
                                                            tint = colors.danger,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = paper.subjectName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "PDF · ${paper.year} · ${if (paper.isSolved) "Solved" else "Original"} · ${paper.fileSizeLabel}",
                                                        style = MaterialTheme.typography.bodySmall.tabular(),
                                                        color = colors.textSecondary
                                                    )
                                                }
                                                PaperOrPdfDownloadButton(
                                                    isDownloaded = paper.isDownloaded || task?.state == DownloadState.COMPLETED,
                                                    task = task,
                                                    onStartDownload = {
                                                        startDownloadWithNotificationRationale {
                                                            onToggleDownloadPaper(paper)
                                                        }
                                                    },
                                                    onPause = { onPauseDownload(targetPdfId) },
                                                    onResume = { onResumeDownload(targetPdfId) },
                                                    onRetry = { onRetryDownload(targetPdfId) }
                                                )
                                            }
                                            if (task != null && task.state != DownloadState.COMPLETED && task.state != DownloadState.CANCELLED) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                InlineDownloadTaskRow(
                                                    task = task,
                                                    onPause = { onPauseDownload(targetPdfId) },
                                                    onResume = { onResumeDownload(targetPdfId) },
                                                    onCancel = { onCancelDownload(targetPdfId) },
                                                    onRetry = { onRetryDownload(targetPdfId) },
                                                    onDownloadOnMobileData = { onDownloadOnMobileData(targetPdfId) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val filteredDocs = pdfLibrary.filter { it.category == selectedCategory }
                    if (filteredDocs.isEmpty()) {
                        ImtihanEmptyView(
                            title = "No ${selectedCategory.replace('_', ' ')} PDFs Published Yet",
                            description = "Published Class 11 KPK textbooks, syllabi, and notes packs appear here for online reading and offline downloads."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredDocs, key = { it.id }) { pdf ->
                                val task = downloadTasks.firstOrNull { it.pdfId == pdf.id }
                                SilverBlueCard(onClick = { onOpenPdfResource(pdf) }, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                                color = colors.accentBlue.copy(alpha = 0.14f),
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.MenuBook,
                                                        contentDescription = null,
                                                        tint = colors.accentBlue,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = pdf.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${pdf.pageCount} pages · ${"%.1f".format(pdf.sizeMb)} MB · ${pdf.version}",
                                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                                    color = colors.textSecondary
                                                )
                                            }
                                            PaperOrPdfDownloadButton(
                                                isDownloaded = pdf.isDownloaded || task?.state == DownloadState.COMPLETED,
                                                task = task,
                                                onStartDownload = {
                                                    startDownloadWithNotificationRationale {
                                                        onToggleDownloadPdf(pdf)
                                                    }
                                                },
                                                onPause = { onPauseDownload(pdf.id) },
                                                onResume = { onResumeDownload(pdf.id) },
                                                onRetry = { onRetryDownload(pdf.id) }
                                            )
                                        }
                                        if (task != null && task.state != DownloadState.COMPLETED && task.state != DownloadState.CANCELLED) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            InlineDownloadTaskRow(
                                                task = task,
                                                onPause = { onPauseDownload(pdf.id) },
                                                onResume = { onResumeDownload(pdf.id) },
                                                onCancel = { onCancelDownload(pdf.id) },
                                                onRetry = { onRetryDownload(pdf.id) },
                                                onDownloadOnMobileData = { onDownloadOnMobileData(pdf.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showPapersAd) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = adPolicy.providerNote.ifBlank { "Academic Partner Banner (Disabled inside PDF Reader & Exams)" },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    if (pendingDownloadAction != null) {
        AlertDialog(
            onDismissRequest = {
                askedNotificationThisSession = true
                pendingDownloadAction?.invoke()
                pendingDownloadAction = null
            },
            title = { Text(stringResource(R.string.dl_notif_permission_title)) },
            text = { Text(stringResource(R.string.dl_notif_permission_body)) },
            confirmButton = {
                Button(onClick = {
                    askedNotificationThisSession = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        pendingDownloadAction?.invoke()
                        pendingDownloadAction = null
                    }
                }) {
                    Text(stringResource(R.string.dl_notif_permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    askedNotificationThisSession = true
                    pendingDownloadAction?.invoke()
                    pendingDownloadAction = null
                }) {
                    Text(stringResource(R.string.dl_notif_permission_not_now))
                }
            }
        )
    }
}

@Composable
private fun PaperOrPdfDownloadButton(
    isDownloaded: Boolean,
    task: DownloadTaskUi?,
    onStartDownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = LocalImtihanColors.current
    val (icon, tint, desc, onClick) = when {
        isDownloaded -> QuadAction(Icons.Outlined.CheckCircle, colors.success, "Downloaded", onStartDownload)
        task?.state == DownloadState.RUNNING || task?.state == DownloadState.QUEUED || task?.state == DownloadState.VERIFYING ->
            QuadAction(Icons.Outlined.Pause, colors.accentBlue, stringResource(R.string.dl_action_pause), onPause)
        task?.state == DownloadState.PAUSED ->
            QuadAction(Icons.Outlined.PlayArrow, colors.warningGold, stringResource(R.string.dl_action_resume), onResume)
        task?.state == DownloadState.FAILED ->
            QuadAction(Icons.Outlined.Refresh, colors.danger, stringResource(R.string.dl_action_retry), onRetry)
        else ->
            QuadAction(Icons.Outlined.CloudDownload, colors.accentBlue, "Download PDF", onStartDownload)
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(
            minWidth = ImtihanDimensions.minTouchTarget,
            minHeight = ImtihanDimensions.minTouchTarget
        )
    ) {
        Icon(imageVector = icon, contentDescription = desc, tint = tint)
    }
}

private data class QuadAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val contentDescription: String,
    val onClick: () -> Unit
)

@Composable
private fun InlineDownloadTaskRow(
    task: DownloadTaskUi,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDownloadOnMobileData: () -> Unit
) {
    val colors = LocalImtihanColors.current
    val statusText = localizedDownloadStateLabel(task)
    val badgeTint = when (task.state) {
        DownloadState.RUNNING, DownloadState.VERIFYING -> colors.accentBlue
        DownloadState.QUEUED -> colors.cyanPulse
        DownloadState.PAUSED -> colors.warningGold
        DownloadState.FAILED -> colors.danger
        DownloadState.COMPLETED -> colors.success
        DownloadState.CANCELLED -> colors.textSecondary
    }
    val bytesText = PdfDownloadEngine.formatBytesProgress(task.bytesDownloaded, task.totalBytes)
    val pctText = if (task.progressPercent >= 0) "${task.progressPercent}%" else ""
    val speedText = if (task.state == DownloadState.RUNNING) PdfDownloadEngine.formatSpeedBps(task.speedBytesPerSec) else ""
    val metaLine = listOf(pctText, bytesText, speedText).filter { it.isNotBlank() }.joinToString(" · ")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ImtihanProgressBar(
            progress = if (task.progressPercent >= 0) (task.progressPercent / 100f).coerceIn(0f, 1f) else 0.15f
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = badgeTint,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = metaLine,
                style = MaterialTheme.typography.labelSmall.tabular(),
                color = colors.textSecondary
            )
        }
        if (!task.errorMessage.isNullOrBlank()) {
            Text(
                text = task.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (task.state == DownloadState.FAILED) colors.danger else colors.textSecondary
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.state == DownloadState.PAUSED && task.pausedReason == DownloadPausedReason.WAITING_WIFI) {
                TextButton(onClick = onDownloadOnMobileData) {
                    Text(stringResource(R.string.dl_action_mobile_data))
                }
            } else if (task.state == DownloadState.PAUSED) {
                TextButton(onClick = onResume) {
                    Text(stringResource(R.string.dl_action_resume))
                }
            } else if (task.state == DownloadState.FAILED) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.dl_action_retry))
                }
            } else if (task.state == DownloadState.RUNNING || task.state == DownloadState.QUEUED) {
                TextButton(onClick = onPause) {
                    Text(stringResource(R.string.dl_action_pause))
                }
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.dl_action_cancel), color = colors.danger)
            }
        }
    }
}

@Composable
private fun localizedDownloadStateLabel(task: DownloadTaskUi): String {
    return when (task.state) {
        DownloadState.QUEUED -> stringResource(R.string.dl_state_queued)
        DownloadState.RUNNING -> stringResource(R.string.dl_state_downloading)
        DownloadState.VERIFYING -> stringResource(R.string.dl_state_verifying)
        DownloadState.PAUSED -> when (task.pausedReason) {
            DownloadPausedReason.WAITING_WIFI -> stringResource(R.string.dl_state_waiting_wifi)
            DownloadPausedReason.WAITING_NETWORK -> stringResource(R.string.dl_state_waiting_network)
            DownloadPausedReason.RETRY_BACKOFF -> stringResource(R.string.dl_state_retry_backoff)
            else -> stringResource(R.string.dl_state_paused)
        }
        DownloadState.COMPLETED -> stringResource(R.string.dl_state_completed)
        DownloadState.FAILED -> if (task.errorCode in setOf("CHECKSUM_MISMATCH", "INVALID_PDF_STREAM", "SIZE_MISMATCH")) {
            stringResource(R.string.dl_state_corrupt)
        } else {
            stringResource(R.string.dl_state_failed)
        }
        DownloadState.CANCELLED -> stringResource(R.string.dl_state_cancelled)
    }
}

@Composable
private fun FilterChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalImtihanColors.current
    Surface(
        onClick = onClick,
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
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Screen 14: Textbook-Grade Native Horizontal PDF Reader
 */
@Composable
fun PdfReaderScreen(
    paperTitle: String,
    fallbackTotalPages: Int,
    viewerState: PdfViewerUiState,
    bookmarks: List<PdfBookmarkUi>,
    annotations: List<PdfAnnotationUi>,
    onRetryDownload: () -> Unit,
    onUpdatePosition: (page: Int, totalPages: Int, zoomPercent: Int) -> Unit,
    onToggleBookmark: (page: Int, label: String) -> Unit,
    onAddAnnotation: (page: Int, noteText: String) -> Unit,
    onDeleteAnnotation: (annotationId: String) -> Unit,
    onBack: () -> Unit,
    onPauseDownload: () -> Unit = {},
    onResumeDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onDownloadOnMobileData: () -> Unit = {}
) {
    ImtihanPdfReader(
        paperTitle = paperTitle,
        fallbackTotalPages = fallbackTotalPages,
        viewerState = viewerState,
        bookmarks = bookmarks,
        annotations = annotations,
        onRetryDownload = onRetryDownload,
        onUpdatePosition = onUpdatePosition,
        onToggleBookmark = onToggleBookmark,
        onAddAnnotation = onAddAnnotation,
        onDeleteAnnotation = onDeleteAnnotation,
        onBack = onBack,
        onPauseDownload = onPauseDownload,
        onResumeDownload = onResumeDownload,
        onCancelDownload = onCancelDownload,
        onDownloadOnMobileData = onDownloadOnMobileData
    )
}

/**
 * Screen 15: Downloads — Active Queue (Pause/Resume/Retry/Cancel/Mobile-Data) + Offline Ready Files
 */
@Composable
fun DownloadsScreen(
    downloads: List<DownloadedFileUi>,
    downloadTasks: List<DownloadTaskUi> = emptyList(),
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onBrowseLibrary: (() -> Unit)? = null,
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onRetryDownload: (String) -> Unit = {},
    onRetryAllFailed: () -> Unit = {},
    onDownloadOnMobileData: (String) -> Unit = {},
    onOpenDownloadedPdf: (pdfId: String, title: String) -> Unit = { _, _ -> }
) {
    val colors = LocalImtihanColors.current
    val totalMb = downloads.sumOf { it.sizeMb }
    val capacityMb = 200.0
    var showClearConfirm by remember { mutableStateOf(false) }

    val queueTasks = downloadTasks.filter {
        it.state != DownloadState.COMPLETED && it.state != DownloadState.CANCELLED
    }
    val hasFailedTasks = queueTasks.any { it.state == DownloadState.FAILED }

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Downloads (${downloads.size})",
                subtitle = if (queueTasks.isNotEmpty()) "${queueTasks.size} active/queued · Saved for offline study" else "Saved for offline study",
                onBack = onBack,
                showBottomDivider = true
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (queueTasks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Download Queue (${queueTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (hasFailedTasks) {
                                TextButton(onClick = onRetryAllFailed) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.dl_action_retry_all))
                                }
                            }
                        }
                    }

                    items(queueTasks, key = { "q_${it.pdfId}" }) { task ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${task.categoryLabel} · ${PdfDownloadEngine.formatBytesProgress(task.bytesDownloaded, task.totalBytes)}",
                                            style = MaterialTheme.typography.bodySmall.tabular(),
                                            color = colors.textSecondary
                                        )
                                    }
                                    val badgeTint = when (task.state) {
                                        DownloadState.RUNNING, DownloadState.VERIFYING -> colors.accentBlue
                                        DownloadState.QUEUED -> colors.cyanPulse
                                        DownloadState.PAUSED -> colors.warningGold
                                        DownloadState.FAILED -> colors.danger
                                        else -> colors.success
                                    }
                                    ImtihanStatusBadge(text = localizedDownloadStateLabel(task), tint = badgeTint)
                                }

                                val progressFraction = if (task.progressPercent >= 0) {
                                    (task.progressPercent / 100f).coerceIn(0f, 1f)
                                } else 0f
                                ImtihanProgressBar(progress = progressFraction)

                                val pctStr = if (task.progressPercent >= 0) "${task.progressPercent}%" else ""
                                val speedStr = if (task.state == DownloadState.RUNNING) PdfDownloadEngine.formatSpeedBps(task.speedBytesPerSec) else ""
                                val meta = listOf(pctStr, speedStr).filter { it.isNotBlank() }.joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.labelSmall.tabular(),
                                        color = colors.textSecondary
                                    )
                                }

                                if (!task.errorMessage.isNullOrBlank()) {
                                    Text(
                                        text = task.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (task.state == DownloadState.FAILED) colors.danger else colors.textSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when {
                                        task.state == DownloadState.PAUSED && task.pausedReason == DownloadPausedReason.WAITING_WIFI -> {
                                            Button(
                                                onClick = { onDownloadOnMobileData(task.pdfId) },
                                                shape = RoundedCornerShape(ImtihanRadii.button),
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                            ) {
                                                Text(stringResource(R.string.dl_action_mobile_data))
                                            }
                                        }
                                        task.state == DownloadState.PAUSED -> {
                                            Button(
                                                onClick = { onResumeDownload(task.pdfId) },
                                                shape = RoundedCornerShape(ImtihanRadii.button),
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                            ) {
                                                Text(stringResource(R.string.dl_action_resume))
                                            }
                                        }
                                        task.state == DownloadState.FAILED -> {
                                            Button(
                                                onClick = { onRetryDownload(task.pdfId) },
                                                shape = RoundedCornerShape(ImtihanRadii.button),
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                            ) {
                                                Text(stringResource(R.string.dl_action_retry))
                                            }
                                        }
                                        task.state == DownloadState.RUNNING || task.state == DownloadState.QUEUED -> {
                                            OutlinedButton(
                                                onClick = { onPauseDownload(task.pdfId) },
                                                shape = RoundedCornerShape(ImtihanRadii.button)
                                            ) {
                                                Text(stringResource(R.string.dl_action_pause))
                                            }
                                        }
                                        else -> Unit
                                    }
                                    OutlinedButton(
                                        onClick = { onCancelDownload(task.pdfId) },
                                        shape = RoundedCornerShape(ImtihanRadii.button)
                                    ) {
                                        Text(stringResource(R.string.dl_action_cancel), color = colors.danger)
                                    }
                                }
                            }
                        }
                    }
                }

                if (downloads.isEmpty() && queueTasks.isEmpty()) {
                    item {
                        ImtihanEmptyView(
                            title = "No Offline Files Yet",
                            description = "Download chapter notes or past paper PDFs to study without internet access.",
                            actionLabel = if (onBrowseLibrary != null) "Browse Papers & Books" else null,
                            onAction = onBrowseLibrary
                        )
                    }
                } else if (downloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Offline Ready (${downloads.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(downloads, key = { it.id }) { item ->
                        SilverBlueCard(
                            onClick = { onOpenDownloadedPdf(item.id, item.title) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(
                                        text = "${item.categoryLabel} · ${"%.1f".format(item.sizeMb)} MB",
                                        style = MaterialTheme.typography.bodySmall.tabular(),
                                        color = colors.textSecondary
                                    )
                                }
                                ImtihanStatusBadge(text = item.statusLabel, tint = colors.success)
                                IconButton(onClick = { onCancelDownload(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove offline file",
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Storage Usage", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(
                                text = "${"%.1f".format(totalMb)} MB / ${capacityMb.toInt()} MB",
                                style = MaterialTheme.typography.bodySmall.tabular(),
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        ImtihanProgressBar(progress = (totalMb / capacityMb).toFloat().coerceIn(0f, 1f))
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            enabled = downloads.isNotEmpty() || queueTasks.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                            shape = RoundedCornerShape(ImtihanRadii.button)
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Downloads")
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all offline downloads?") },
            text = { Text("You can re-download any chapter or past paper anytime when connected.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Screen 16: Profile & Settings — Grouped hairline sections replacing flat button clutter
 */
@Composable
fun ProfileSettingsScreen(
    shellState: AppShellState,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleOfflineSimulated: (Boolean) -> Unit,
    onToggleAllowMobileData: (Boolean) -> Unit = {},
    onToggleNotifications: (Boolean) -> Unit,
    onToggleSoundEffects: (Boolean) -> Unit = {},
    onToggleHaptics: (Boolean) -> Unit = {},
    onSetReaderHighContrastMode: (String) -> Unit = {},
    onCycleLanguage: () -> Unit,
    onOpenDownloads: () -> Unit,
    onChangeClassOrBoard: () -> Unit,
    onOpenFocusTimer: () -> Unit = {},
    onOpenStudyPlanner: () -> Unit = {},
    onOpenMotivation: () -> Unit = {},
    onOpenSmartInsights: () -> Unit = {},
    onOpenBookmarksFlashcards: () -> Unit = {},
    onOpenQuizHistory: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onUpdateApiBaseUrl: (String) -> String? = { null },
    onLinkGoogleToken: (String) -> Unit = {},
    onUnlinkGoogle: () -> Unit = {}
) {
    val colors = LocalImtihanColors.current
    var showUrlDialog by remember { mutableStateOf(false) }
    var apiUrlInput by remember(shellState.configuredApiBaseUrl) { mutableStateOf(shellState.configuredApiBaseUrl) }
    var urlError by remember { mutableStateOf<String?>(null) }

    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleIdTokenInput by remember { mutableStateOf("") }

    val groupLabel = Stage3To8Engines.STUDY_GROUPS.firstOrNull { it.code == shellState.studyGroupCode }?.label ?: shellState.studyGroupCode

    SilverBlueCanvas {
        Column(modifier = Modifier.fillMaxSize()) {
            ImtihanTopBar(
                title = "Profile & Settings",
                subtitle = "Class ${shellState.selectedClassLevel} · ${shellState.selectedBoardCode}",
                showBottomDivider = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(ImtihanSpacing.md),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Profile Card
                item {
                    ImtihanHeroCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = shellState.profile.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = shellState.profile.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Class ${shellState.selectedClassLevel} · ${shellState.selectedSubBoardCode} · $groupLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (shellState.profile.isGuest) "Guest Local Mode" else shellState.userEmail ?: "Google Synced",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProfileStat("${shellState.profile.studyHours}", "Hours")
                                ProfileStat("${shellState.profile.chaptersCompleted}", "Chapters")
                                ProfileStat("${shellState.realHomeMetrics.xpTotal}", "XP")
                                ProfileStat("${shellState.realHomeMetrics.currentStreakDays}d", "Streak")
                            }
                        }
                    }
                }

                // Study Tools Group
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Study Tools",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textSecondary
                        )
                        SilverBlueCard(
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ImtihanNavigationRow(
                                title = "Focus Pomodoro Studio",
                                subtitle = "15m / 25m / 50m study sprints & ambient audio",
                                icon = Icons.Outlined.Timer,
                                onClick = onOpenFocusTimer
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Smart Learning Insights",
                                subtitle = "Honest exam readiness & weak chapters",
                                icon = Icons.Outlined.Insights,
                                iconTint = colors.success,
                                onClick = onOpenSmartInsights
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Study Planner & Goals",
                                subtitle = "Target ${shellState.dailyTargetMinutes}m/day · tasks & countdown",
                                icon = Icons.Outlined.Today,
                                iconTint = colors.cyanPulse,
                                onClick = onOpenStudyPlanner
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Achievements & Leaderboard",
                                subtitle = "Badges, weekly challenges & opt-in rank",
                                icon = Icons.Outlined.EmojiEvents,
                                iconTint = colors.warningGold,
                                onClick = onOpenMotivation
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Flashcards & Bookmarks",
                                subtitle = "SM-2 review deck & saved chapters",
                                icon = Icons.Outlined.Style,
                                iconTint = colors.accentBlue,
                                onClick = onOpenBookmarksFlashcards
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Quiz & Mock History",
                                subtitle = "Inspect past exam percentages & marks",
                                icon = Icons.Outlined.History,
                                onClick = onOpenQuizHistory
                            )
                        }
                    }
                }

                // Curriculum & Offline Library Group
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Curriculum & Offline Library",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textSecondary
                        )
                        SilverBlueCard(
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ImtihanNavigationRow(
                                title = "Offline Downloads",
                                subtitle = "Manage cached chapters, books & past paper PDFs",
                                icon = Icons.Outlined.CloudDownload,
                                onClick = onOpenDownloads
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Language & Medium",
                                subtitle = "Switch English or اردو Nastaliq curriculum view",
                                icon = Icons.Outlined.Translate,
                                iconTint = colors.cyanPulse,
                                trailingText = shellState.languageCode,
                                onClick = onCycleLanguage
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "BISE Region & Study Group",
                                subtitle = "Change KPK division or Class 11 stream",
                                icon = Icons.Outlined.School,
                                trailingText = shellState.selectedSubBoardCode,
                                onClick = onChangeClassOrBoard
                            )
                        }
                    }
                }

                // Preferences & Accessibility Toggles
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Preferences & Comfort",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textSecondary
                        )
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            SettingToggleRow(
                                title = "Dark Theme",
                                subtitle = if (shellState.isDarkMode) "Clean Obsidian Dark" else "Crisp Alabaster Light",
                                checked = shellState.isDarkMode,
                                onCheckedChange = onToggleDarkMode
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.cardBorder.copy(alpha = 0.4f))
                            SettingToggleRow(
                                title = "Quiz Sound Effects",
                                subtitle = "Crisp procedural chimes & card-flip SFX",
                                checked = shellState.soundEffectsEnabled,
                                onCheckedChange = onToggleSoundEffects
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.cardBorder.copy(alpha = 0.4f))
                            SettingToggleRow(
                                title = "Haptic Feedback",
                                subtitle = "Tactile vibration on MCQ bubbles & 3D flips",
                                checked = shellState.hapticsEnabled,
                                onCheckedChange = onToggleHaptics
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.cardBorder.copy(alpha = 0.4f))
                            SettingToggleRow(
                                title = "High-Contrast Notes Reader",
                                subtitle = if (shellState.readerHighContrastMode == "PAPER") "Paper Contrast Active" else "Clean Dark Contrast Active",
                                checked = shellState.readerHighContrastMode == "PAPER",
                                onCheckedChange = { paper -> onSetReaderHighContrastMode(if (paper) "PAPER" else "OLED") }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.cardBorder.copy(alpha = 0.4f))
                            SettingToggleRow(
                                title = "Offline Mode",
                                subtitle = "Use Room offline cache without network calls",
                                checked = shellState.isOfflineSimulated,
                                onCheckedChange = onToggleOfflineSimulated
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.cardBorder.copy(alpha = 0.4f))
                            SettingToggleRow(
                                title = "Download over Mobile Data",
                                subtitle = if (shellState.allowDownloadOverMobileData) {
                                    "Allow large PDFs (> 5 MB) on 2G/3G/4G"
                                } else {
                                    "Wi-Fi only by default for PDFs over 5 MB"
                                },
                                checked = shellState.allowDownloadOverMobileData,
                                onCheckedChange = onToggleAllowMobileData
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.cardBorder.copy(alpha = 0.4f))
                            SettingToggleRow(
                                title = "Study & Download Notifications",
                                subtitle = "Progress notifications and daily exam reminders",
                                checked = shellState.notificationsEnabled,
                                onCheckedChange = onToggleNotifications
                            )
                        }
                    }
                }

                // Cloud Sync, Account & Support
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Cloud Sync & Account",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textSecondary
                        )
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Cloud Sync (Cloudflare Worker)", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(
                                text = shellState.configuredApiBaseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.accentBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Queued actions: ${shellState.pendingSyncCount} · Last sync: ${shellState.lastSyncedAtIso ?: "Not yet"}",
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = colors.textSecondary
                            )
                            if (!shellState.syncStatusMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = shellState.syncStatusMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onSyncNow,
                                    enabled = !shellState.isSyncing && !shellState.isOfflineSimulated,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                    shape = RoundedCornerShape(ImtihanRadii.button),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                ) {
                                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (shellState.isSyncing) "Syncing..." else "Sync Now")
                                }
                                OutlinedButton(
                                    onClick = { showUrlDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget),
                                    shape = RoundedCornerShape(ImtihanRadii.button)
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Worker URL")
                                }
                            }
                        }

                        SilverBlueCard(
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ImtihanNavigationRow(
                                title = if (shellState.profile.isGuest) "Link Google Account (Optional)" else "Sign Out Google",
                                subtitle = if (shellState.profile.isGuest) {
                                    "Sync guest progress across devices via Google ID token"
                                } else {
                                    "Signed in as ${shellState.userEmail ?: shellState.profile.displayName}"
                                },
                                icon = Icons.Outlined.Sync,
                                onClick = {
                                    if (shellState.profile.isGuest) showGoogleDialog = true else onUnlinkGoogle()
                                }
                            )
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                            ImtihanNavigationRow(
                                title = "Report Content Error / Feedback",
                                subtitle = "Submit MCQ fixes or feature suggestions",
                                icon = Icons.Outlined.Flag,
                                iconTint = colors.danger,
                                onClick = onOpenFeedback
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Configure Worker API URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Use http://10.0.2.2:8787/v1 for Android Emulator, http://<ubuntu-lan-ip>:8787/v1 for USB/Wi-Fi phone testing, or your https://*.workers.dev/v1 URL.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = apiUrlInput,
                        onValueChange = {
                            apiUrlInput = it
                            urlError = null
                        },
                        singleLine = true,
                        label = { Text("Worker /v1 Base URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (urlError != null) {
                        Text(urlError!!, color = colors.danger, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val err = onUpdateApiBaseUrl(apiUrlInput)
                    if (err == null) showUrlDialog = false else urlError = err
                }) {
                    Text("Save URL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = { Text("Link Google Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Guest progress is preserved and merged automatically via POST /v1/auth/google/exchange. Paste a Google OAuth ID token:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = googleIdTokenInput,
                        onValueChange = { googleIdTokenInput = it },
                        label = { Text("Google ID Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googleIdTokenInput.isNotBlank()) {
                            onLinkGoogleToken(googleIdTokenInput)
                            googleIdTokenInput = ""
                            showGoogleDialog = false
                        }
                    },
                    enabled = googleIdTokenInput.isNotBlank()
                ) {
                    Text("Verify & Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.tabular(),
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalImtihanColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .defaultMinSize(minHeight = ImtihanDimensions.minTouchTarget)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
