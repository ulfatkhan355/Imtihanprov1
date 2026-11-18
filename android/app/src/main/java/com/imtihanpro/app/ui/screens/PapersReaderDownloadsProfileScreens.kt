package com.imtihanpro.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.domain.engine.Stage3To8Engines
import com.imtihanpro.app.domain.model.AdPlacementPolicyUi
import com.imtihanpro.app.domain.model.DownloadedFileUi
import com.imtihanpro.app.domain.model.PastPaperUi
import com.imtihanpro.app.domain.model.PdfAnnotationUi
import com.imtihanpro.app.domain.model.PdfBookmarkUi
import com.imtihanpro.app.domain.model.PdfResourceUi
import com.imtihanpro.app.domain.model.UiResourceState
import com.imtihanpro.app.ui.components.ImtihanEmptyView
import com.imtihanpro.app.ui.components.ImtihanErrorView
import com.imtihanpro.app.ui.components.ImtihanLoadingView
import com.imtihanpro.app.ui.components.ImtihanProgressBar
import com.imtihanpro.app.ui.components.SilverBlueCanvas
import com.imtihanpro.app.ui.components.SilverBlueCard
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.viewmodel.AppShellState
import com.imtihanpro.app.ui.viewmodel.PdfViewerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Screen 13: Papers, Textbooks & PDF Study Library
 */
@Composable
fun PastPapersScreen(
    papersState: UiResourceState<List<PastPaperUi>>,
    pdfLibrary: List<PdfResourceUi> = emptyList(),
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
    onBannerDisplayed: () -> Unit = {}
) {
    val colors = LocalImtihanColors.current
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Papers & Study PDFs",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipButton("All", solvedFilter == null) { onSetFilter(null) }
                    FilterChipButton("Solved", solvedFilter == true) { onSetFilter(true) }
                    FilterChipButton("Unsolved", solvedFilter == false) { onSetFilter(false) }
                }
                Spacer(modifier = Modifier.height(12.dp))

                when (papersState) {
                    is UiResourceState.Loading -> ImtihanLoadingView()
                    is UiResourceState.Error -> ImtihanErrorView(papersState.message, onRetry = {})
                    is UiResourceState.Empty -> ImtihanEmptyView(papersState.title, papersState.description)
                    is UiResourceState.Success -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(papersState.data, key = { it.id }) { paper ->
                                SilverBlueCard(
                                    onClick = { onOpenPaper(paper) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = colors.danger.copy(alpha = 0.16f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PictureAsPdf,
                                                    contentDescription = null,
                                                    tint = colors.danger
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = paper.subjectName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "${paper.year} · ${if (paper.isSolved) "Solved" else "Original"} · ${paper.fileSizeLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                        IconButton(onClick = { onToggleDownloadPaper(paper) }) {
                                            Icon(
                                                imageVector = if (paper.isDownloaded) Icons.Outlined.CheckCircle else Icons.Outlined.CloudDownload,
                                                contentDescription = if (paper.isDownloaded) "Downloaded" else "Download paper",
                                                tint = if (paper.isDownloaded) colors.success else colors.accentBlue
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
                        title = "No ${selectedCategory.replace('_', ' ')} PDFs Published",
                        description = "Upload books, syllabi, or notes packs in Admin Console to make them available offline."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredDocs, key = { it.id }) { pdf ->
                            SilverBlueCard(onClick = { onOpenPdfResource(pdf) }, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.accentBlue.copy(alpha = 0.16f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = colors.accentBlue)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pdf.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                        Text(
                                            text = "${pdf.pageCount} pages · ${"%.1f".format(pdf.sizeMb)} MB · ${pdf.version}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    IconButton(onClick = { onToggleDownloadPdf(pdf) }) {
                                        Icon(
                                            imageVector = if (pdf.isDownloaded) Icons.Outlined.CheckCircle else Icons.Outlined.CloudDownload,
                                            contentDescription = "Download PDF",
                                            tint = if (pdf.isDownloaded) colors.success else colors.accentBlue
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

@Composable
private fun FilterChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalImtihanColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.accentBlue else colors.cardSurface,
        border = BorderStroke(1.dp, if (selected) colors.accentBlue else colors.cardBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else colors.textSecondary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Screen 14: Real Android PdfRenderer PDF Reader (Zero Ads Strictly Enforced During Study)
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
    onBack: () -> Unit
) {
    val colors = LocalImtihanColors.current
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var actualPageCount by remember(fallbackTotalPages) { mutableStateOf(maxOf(1, fallbackTotalPages)) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var noteDraft by remember { mutableStateOf("") }

    val rendererHolder = remember(viewerState.localFilePath) {
        val path = viewerState.localFilePath
        if (path != null && File(path).exists()) {
            runCatching {
                val pfd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pfd to renderer
            }.getOrNull()
        } else {
            null
        }
    }

    DisposableEffect(rendererHolder) {
        if (rendererHolder != null) {
            actualPageCount = maxOf(1, rendererHolder.second.pageCount)
        }
        onDispose {
            runCatching { rendererHolder?.second?.close() }
            runCatching { rendererHolder?.first?.close() }
        }
    }

    val currentPage = viewerState.currentPage.coerceIn(1, actualPageCount)
    val zoomPercent = viewerState.zoomPercent.coerceIn(75, 250)
    val isCurrentPageBookmarked = bookmarks.any { it.pageNumber == currentPage }
    val currentPageNotes = annotations.filter { it.pageNumber == currentPage }

    LaunchedEffect(rendererHolder, currentPage, zoomPercent) {
        val pair = rendererHolder ?: return@LaunchedEffect
        val renderer = pair.second
        renderError = null
        val bmp = withContext(Dispatchers.Default) {
            runCatching {
                val pageIndex = (currentPage - 1).coerceIn(0, renderer.pageCount - 1)
                renderer.openPage(pageIndex).use { page ->
                    val scaleFactor = (zoomPercent / 100f) * 2.0f
                    val width = (page.width * scaleFactor).toInt().coerceIn(320, 2600)
                    val height = (page.height * scaleFactor).toInt().coerceIn(400, 3600)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    Canvas(bitmap).drawColor(AndroidColor.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }.getOrElse { err ->
                renderError = err.message ?: "Failed to render PDF page."
                null
            }
        }
        renderedBitmap = bmp
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
                        text = paperTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (viewerState.localFilePath != null) {
                            "Offline PDF · ${bookmarks.size} bookmarks · ${annotations.size} notes"
                        } else {
                            "Cloud PDF Stream (Backblaze B2)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { showNotesDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.NoteAdd,
                        contentDescription = "Page notes and bookmarks",
                        tint = if (currentPageNotes.isNotEmpty()) colors.accentBlue else colors.textPrimary
                    )
                }
                IconButton(onClick = { onToggleBookmark(currentPage, "Page $currentPage") }) {
                    Icon(
                        imageVector = if (isCurrentPageBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark page",
                        tint = if (isCurrentPageBookmarked) colors.warningGold else colors.textPrimary
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC)
            ) {
                when {
                    viewerState.isDownloading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = colors.accentBlue)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Downloading PDF from Cloudflare Worker / Backblaze B2...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }

                    renderedBitmap != null -> {
                        val vScroll = rememberScrollState()
                        val hScroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(vScroll)
                                .horizontalScroll(hScroll),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                bitmap = renderedBitmap!!.asImageBitmap(),
                                contentDescription = "$paperTitle Page $currentPage",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = renderError ?: viewerState.errorMessage ?: "PDF not cached locally yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onRetryDownload,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download PDF Offline")
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (zoomPercent > 75) onUpdatePosition(currentPage, actualPageCount, zoomPercent - 25)
                    }) {
                        Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom out", tint = colors.textPrimary)
                    }
                    Text(
                        text = "$zoomPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    IconButton(onClick = {
                        if (zoomPercent < 250) onUpdatePosition(currentPage, actualPageCount, zoomPercent + 25)
                    }) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom in", tint = colors.textPrimary)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentPage > 1) onUpdatePosition(currentPage - 1, actualPageCount, zoomPercent)
                        },
                        enabled = currentPage > 1,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Prev", maxLines = 1)
                    }
                    Text(
                        text = "$currentPage / $actualPageCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Button(
                        onClick = {
                            if (currentPage < actualPageCount) onUpdatePosition(currentPage + 1, actualPageCount, zoomPercent)
                        },
                        enabled = currentPage < actualPageCount,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                    ) {
                        Text("Next", maxLines = 1)
                    }
                }
            }
        }
    }

    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text("Bookmarks & Notes (Page $currentPage)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        label = { Text("Add study note for page $currentPage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (noteDraft.isNotBlank()) {
                                onAddAnnotation(currentPage, noteDraft)
                                noteDraft = ""
                            }
                        },
                        enabled = noteDraft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Page Note")
                    }
                    if (bookmarks.isNotEmpty()) {
                        Text("Saved Bookmarks:", style = MaterialTheme.typography.labelMedium)
                        bookmarks.forEach { bm ->
                            TextButton(
                                onClick = {
                                    onUpdatePosition(bm.pageNumber, actualPageCount, zoomPercent)
                                    showNotesDialog = false
                                }
                            ) {
                                Text("Jump to Page ${bm.pageNumber} (${bm.label})")
                            }
                        }
                    }
                    if (annotations.isNotEmpty()) {
                        Text("Study Notes:", style = MaterialTheme.typography.labelMedium)
                        annotations.take(5).forEach { note ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        onUpdatePosition(note.pageNumber, actualPageCount, zoomPercent)
                                        showNotesDialog = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("P.${note.pageNumber}: ${note.noteText}", maxLines = 2)
                                }
                                IconButton(onClick = { onDeleteAnnotation(note.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete note")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Screen 15: Downloads
 */
@Composable
fun DownloadsScreen(
    downloads: List<DownloadedFileUi>,
    onBack: () -> Unit,
    onClearAll: () -> Unit
) {
    val colors = LocalImtihanColors.current
    val totalMb = downloads.sumOf { it.sizeMb }
    val capacityMb = 200.0

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
                    text = "Downloads (${downloads.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (downloads.isEmpty()) {
                    item {
                        ImtihanEmptyView(
                            title = "No Offline Files Yet",
                            description = "Download chapter notes or past paper PDFs to study without internet access."
                        )
                    }
                } else {
                    items(downloads, key = { it.id }) { item ->
                        SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(
                                        text = "${item.categoryLabel} · ${"%.1f".format(item.sizeMb)} MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                                Text(item.statusLabel, style = MaterialTheme.typography.labelMedium, color = colors.success)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Storage Usage", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(
                                text = "${"%.1f".format(totalMb)} MB / ${capacityMb.toInt()} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        ImtihanProgressBar(progress = (totalMb / capacityMb).toFloat().coerceIn(0f, 1f))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onClearAll,
                            enabled = downloads.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Clear All Downloads")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 16: Profile & Settings
 */
@Composable
fun ProfileSettingsScreen(
    shellState: AppShellState,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleOfflineSimulated: (Boolean) -> Unit,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
            }

            item {
                SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.accentBlue.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, colors.accentBlue),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = shellState.profile.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colors.accentBlue
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = shellState.profile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Class ${shellState.selectedClassLevel} • ${shellState.selectedBoardCode} (${shellState.selectedSubBoardCode}) • $groupLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (shellState.profile.isGuest) "Guest Local Mode" else shellState.userEmail ?: "Google Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accentBlue
                        )
                        Spacer(modifier = Modifier.height(14.dp))
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

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileNavRow("Focus Pomodoro Studio", "15m / 25m / 50m study sprints & ambient audio", onOpenFocusTimer)
                    ProfileNavRow("Smart Learning Insights", "Honest exam readiness & weak chapters", onOpenSmartInsights)
                    ProfileNavRow("Study Planner & Goals", "Target ${shellState.dailyTargetMinutes}m/day · tasks & countdown", onOpenStudyPlanner)
                    ProfileNavRow("Achievements & Leaderboard", "Badges, weekly challenges & opt-in rank", onOpenMotivation)
                    ProfileNavRow("Flashcards & Bookmarks", "3D SM-2 review deck & saved chapters", onOpenBookmarksFlashcards)
                    ProfileNavRow("Quiz & Mock History", "Inspect past exam percentages & marks", onOpenQuizHistory)
                    ProfileNavRow("Report Content Error / Feedback", "Submit MCQ fixes or suggestions", onOpenFeedback)
                }
            }

            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cloud Sync (Cloudflare Worker)", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(
                                text = shellState.configuredApiBaseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.accentBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Queued actions: ${shellState.pendingSyncCount} • Last sync: ${shellState.lastSyncedAtIso ?: "Not yet"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            if (!shellState.syncStatusMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = shellState.syncStatusMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSyncNow,
                            enabled = !shellState.isSyncing && !shellState.isOfflineSimulated,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                        ) {
                            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (shellState.isSyncing) "Syncing..." else "Sync Now")
                        }
                        OutlinedButton(
                            onClick = { showUrlDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Worker URL")
                        }
                    }
                }
            }

            item {
                SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                    SettingToggleRow(
                        title = "Theme",
                        subtitle = if (shellState.isDarkMode) "2050 OLED Obsidian Dark (Primary)" else "Ice-Glass Light Mode",
                        checked = shellState.isDarkMode,
                        onCheckedChange = onToggleDarkMode
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        title = "Quiz Sound Effects",
                        subtitle = "Crisp procedural chimes & card-flip SFX",
                        checked = shellState.soundEffectsEnabled,
                        onCheckedChange = onToggleSoundEffects
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        title = "Haptic Feedback",
                        subtitle = "Tactile vibration on MCQ bubbles & 3D flips",
                        checked = shellState.hapticsEnabled,
                        onCheckedChange = onToggleHaptics
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        title = "High-Contrast Notes Reader",
                        subtitle = if (shellState.readerHighContrastMode == "PAPER") "Paper Contrast Active" else "OLED Contrast Active",
                        checked = shellState.readerHighContrastMode == "PAPER",
                        onCheckedChange = { paper -> onSetReaderHighContrastMode(if (paper) "PAPER" else "OLED") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        title = "Offline Mode",
                        subtitle = "Use Room offline cache without network calls",
                        checked = shellState.isOfflineSimulated,
                        onCheckedChange = onToggleOfflineSimulated
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        title = "Study Reminders",
                        subtitle = "Daily exam goal notifications",
                        checked = shellState.notificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                }
            }

            item {
                SilverBlueCard(
                    onClick = {
                        if (shellState.profile.isGuest) showGoogleDialog = true else onUnlinkGoogle()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (shellState.profile.isGuest) "Link Google Account (Optional)" else "Sign Out Google (Keep Local Progress)",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (shellState.profile.isGuest) {
                            "Exchange a Google Sign-In ID token with /v1/auth/google/exchange to sync across devices."
                        } else {
                            "Signed in as ${shellState.userEmail ?: shellState.profile.displayName}. Tap to return to Guest mode."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            item {
                ProfileNavRow("Offline Downloads", "Manage cached chapters, books & past paper PDFs", onOpenDownloads)
            }

            item {
                SilverBlueCard(onClick = onCycleLanguage, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Language", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(shellState.languageCode, style = MaterialTheme.typography.bodySmall, color = colors.accentBlue)
                    }
                }
            }

            item {
                ProfileNavRow("Change Class / BISE / Study Group", "Switch Classes 9-12, KPK division & academic group", onChangeClassOrBoard)
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
private fun ProfileNavRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalImtihanColors.current
    SilverBlueCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    val colors = LocalImtihanColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
