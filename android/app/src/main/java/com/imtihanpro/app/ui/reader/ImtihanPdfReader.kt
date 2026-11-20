package com.imtihanpro.app.ui.reader

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Toc
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.imtihanpro.app.domain.model.PdfAnnotationUi
import com.imtihanpro.app.domain.model.PdfBookmarkUi
import com.imtihanpro.app.ui.theme.ImtihanRadii
import com.imtihanpro.app.ui.theme.LocalImtihanColors
import com.imtihanpro.app.ui.viewmodel.PdfViewerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Textbook-Grade Native Android PDF Reader
 *
 * Highlights:
 * - Unobstructed edge-to-edge single-page HorizontalPager (swipe left -> next page, swipe right -> prev)
 * - Windowed page loading (beyondViewportPageCount = 1 + adjacent neighbor pre-warming)
 * - Byte-budget LruCache safe for 4GB-8GB RAM devices
 * - Progressive fit-width render + high-DPI Matrix viewport tile render for 1.2x..4.0x zoom clarity
 * - Offline full-text search & document outline (TOC) powered by PdfBox-Android
 * - Room reading progress restoration, bookmarks, page annotations, Paper/Warm/Dim tints, & Share
 */
@Composable
fun ImtihanPdfReader(
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
    coordinatorFactory: (File) -> PdfRenderCoordinator = { PdfRenderCoordinator(it) }
) {
    val colors = LocalImtihanColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val coordinator = remember(viewerState.localFilePath) {
        val path = viewerState.localFilePath
        if (!path.isNullOrBlank() && File(path).exists()) {
            coordinatorFactory(File(path))
        } else {
            null
        }
    }

    DisposableEffect(coordinator) {
        onDispose {
            coordinator?.close()
        }
    }

    val actualPageCount = remember(coordinator, fallbackTotalPages) {
        maxOf(1, coordinator?.pageCount ?: fallbackTotalPages)
    }

    val initialPageIdx = remember(viewerState.localFilePath, actualPageCount) {
        (viewerState.currentPage - 1).coerceIn(0, actualPageCount - 1)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIdx,
        pageCount = { actualPageCount }
    )

    // Restore exact saved page once native PdfRenderer resolves real page count
    var didApplySavedPageRestore by remember(viewerState.localFilePath) { mutableStateOf(false) }
    LaunchedEffect(coordinator, viewerState.currentPage, actualPageCount) {
        if (coordinator != null && coordinator.isOpen && !didApplySavedPageRestore) {
            val targetIdx = (viewerState.currentPage - 1).coerceIn(0, actualPageCount - 1)
            if (pagerState.currentPage != targetIdx) {
                pagerState.scrollToPage(targetIdx)
            }
            didApplySavedPageRestore = true
        }
    }

    var showChrome by remember { mutableStateOf(false) }
    var readingTint by remember { mutableStateOf(PdfReadingTint.PAPER) }
    var zoomScale by remember(viewerState.localFilePath) {
        mutableFloatStateOf((viewerState.zoomPercent / 100f).coerceIn(1.0f, 4.0f))
    }
    var panOffset by remember(pagerState.currentPage) { mutableStateOf(Offset.Zero) }

    // Search & Study Drawer States
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<PdfSearchMatch>>(emptyList()) }
    var searchScannedPages by remember { mutableIntStateOf(0) }
    var searchTotalPages by remember { mutableIntStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }
    var activeMatchIndex by remember { mutableIntStateOf(0) }

    var outlineNodes by remember(viewerState.localFilePath) { mutableStateOf<List<PdfOutlineNode>>(emptyList()) }
    var showStudyDrawer by remember { mutableStateOf(false) }
    var studyDrawerTab by remember { mutableIntStateOf(0) }
    var noteDraft by remember { mutableStateOf("") }

    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var scrubberPreviewPage by remember { mutableStateOf<Int?>(null) }

    val currentPageNumber = (pagerState.currentPage + 1).coerceIn(1, actualPageCount)
    val zoomPercent = (zoomScale * 100f).roundToInt().coerceIn(100, 400)
    val isCurrentPageBookmarked = bookmarks.any { it.pageNumber == currentPageNumber }
    val currentPageNotes = annotations.filter { it.pageNumber == currentPageNumber }

    val latestOnUpdatePosition by rememberUpdatedState(onUpdatePosition)

    // Extract PDF TOC outline lazily once per opened file
    LaunchedEffect(viewerState.localFilePath) {
        val path = viewerState.localFilePath
        outlineNodes = if (!path.isNullOrBlank() && File(path).exists()) {
            PdfTextSearchEngine.extractOutline(context, File(path))
        } else {
            emptyList()
        }
    }

    // Stream offline full-text search results when searchQuery changes
    LaunchedEffect(searchQuery, viewerState.localFilePath, showSearchBar) {
        val path = viewerState.localFilePath
        if (!showSearchBar || searchQuery.trim().length < 2 || path.isNullOrBlank()) {
            searchMatches = emptyList()
            isSearching = false
            searchScannedPages = 0
            return@LaunchedEffect
        }
        delay(220)
        isSearching = true
        activeMatchIndex = 0
        PdfTextSearchEngine.searchStreaming(context, File(path), searchQuery)
            .collectLatest { progress ->
                searchMatches = progress.matches
                searchScannedPages = progress.scannedPages
                searchTotalPages = progress.totalPages
                isSearching = !progress.isFinished
            }
    }

    // Persist settled reading position only once initial saved-page restoration completes
    LaunchedEffect(pagerState.settledPage, actualPageCount, coordinator, didApplySavedPageRestore) {
        if (coordinator != null && coordinator.isOpen && didApplySavedPageRestore) {
            val settledNum = (pagerState.settledPage + 1).coerceIn(1, actualPageCount)
            latestOnUpdatePosition(settledNum, actualPageCount, zoomPercent)
        }
    }

    fun jumpToPage(targetPageOneBased: Int) {
        val targetIdx = (targetPageOneBased - 1).coerceIn(0, actualPageCount - 1)
        zoomScale = 1.0f
        panOffset = Offset.Zero
        coroutineScope.launch {
            if (abs(pagerState.currentPage - targetIdx) > 3) {
                pagerState.scrollToPage(targetIdx)
            } else {
                pagerState.animateScrollToPage(targetIdx)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readingTint.backdropColor)
    ) {
        when {
            viewerState.isDownloading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pdf_downloading_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = colors.accentBlue)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Preparing textbook PDF offline…",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (readingTint == PdfReadingTint.DIM) Color.White else colors.textPrimary
                        )
                    }
                }
            }

            coordinator == null || !coordinator.isOpen -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("pdf_document_error_state"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        tint = colors.accentBlue,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = coordinator?.openError
                            ?: viewerState.errorMessage
                            ?: "This textbook PDF is not cached offline yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (readingTint == PdfReadingTint.DIM) Color.White else colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(ImtihanRadii.button)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = onRetryDownload,
                            shape = RoundedCornerShape(ImtihanRadii.button),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download PDF")
                        }
                    }
                }
            }

            else -> {
                // Edge-to-edge single-page horizontal reader with realistic depth-slide transition
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = zoomScale <= 1.05f,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pdf_pager")
                ) { pageIndex ->
                    val isCurrent = pagerState.currentPage == pageIndex
                    PdfTextbookPageSlot(
                        pageIndex = pageIndex,
                        totalPages = actualPageCount,
                        coordinator = coordinator,
                        readingTint = readingTint,
                        isCurrentPage = isCurrent,
                        activeZoomScale = if (isCurrent) zoomScale else 1.0f,
                        activePanOffset = if (isCurrent) panOffset else Offset.Zero,
                        onUpdateZoomAndPan = { nextZoom, nextPan ->
                            zoomScale = nextZoom
                            panOffset = nextPan
                            if (nextZoom <= 1.02f) {
                                latestOnUpdatePosition(currentPageNumber, actualPageCount, 100)
                            }
                        },
                        onCenterTap = { showChrome = !showChrome },
                        onLeftEdgeTap = {
                            if (pageIndex > 0) jumpToPage(pageIndex)
                        },
                        onRightEdgeTap = {
                            if (pageIndex + 1 < actualPageCount) jumpToPage(pageIndex + 2)
                        },
                        onRetryWholePdf = onRetryDownload,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                                    .coerceIn(-1f, 1f)
                                val dist = abs(pageOffset)
                                val easeScale = 1f - dist * 0.045f
                                scaleX = easeScale
                                scaleY = easeScale
                                alpha = 1f - dist * 0.16f
                                shadowElevation = (1f - dist) * 14f
                            }
                    )
                }

                // Pre-warm only adjacent neighbor pages (page - 1 and page + 1)
                LaunchedEffect(pagerState.currentPage, coordinator) {
                    val dm = context.resources.displayMetrics
                    coordinator.prewarmAdjacentPages(
                        currentPageIndex = pagerState.currentPage,
                        viewportWidthPx = dm.widthPixels.coerceAtLeast(720),
                        viewportHeightPx = dm.heightPixels.coerceAtLeast(1080)
                    )
                }
            }
        }

        // Subtle floating zoom badge ONLY while zoomed in & HUD hidden so user can return to 1x instantly
        if (!showChrome && zoomScale > 1.05f) {
            Surface(
                onClick = {
                    zoomScale = 1.0f
                    panOffset = Offset.Zero
                    latestOnUpdatePosition(currentPageNumber, actualPageCount, 100)
                },
                shape = RoundedCornerShape(ImtihanRadii.chip),
                color = colors.cardSurface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .testTag("pdf_zoom_reset")
            ) {
                Text(
                    text = "${zoomPercent}% · Reset",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentBlue,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        // Scrubber preview badge while dragging the bottom page slider
        scrubberPreviewPage?.let { previewPage ->
            Surface(
                shape = RoundedCornerShape(ImtihanRadii.button),
                color = colors.accentBlue,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 112.dp)
            ) {
                Text(
                    text = "Page $previewPage / $actualPageCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Top Floating Auto-Hide HUD
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = colors.cardSurface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .testTag("pdf_hud_top")
            ) {
                Column {
                    if (!showSearchBar) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
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
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Page $currentPageNumber of $actualPageCount · Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { showSearchBar = true },
                                modifier = Modifier.testTag("pdf_search_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search inside PDF",
                                    tint = colors.textPrimary
                                )
                            }
                            IconButton(
                                onClick = { showStudyDrawer = true },
                                modifier = Modifier.testTag("pdf_drawer_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Toc,
                                    contentDescription = "Contents, Bookmarks and Notes",
                                    tint = if (currentPageNotes.isNotEmpty()) colors.accentBlue else colors.textPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    readingTint = when (readingTint) {
                                        PdfReadingTint.PAPER -> PdfReadingTint.WARM
                                        PdfReadingTint.WARM -> PdfReadingTint.DIM
                                        PdfReadingTint.DIM -> PdfReadingTint.PAPER
                                    }
                                },
                                modifier = Modifier.testTag("pdf_tint_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MenuBook,
                                    contentDescription = "Cycle reading surface tint",
                                    tint = colors.accentBlue
                                )
                            }
                            IconButton(
                                onClick = { onToggleBookmark(currentPageNumber, "Page $currentPageNumber") },
                                modifier = Modifier.testTag("pdf_bookmark_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPageBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmark page",
                                    tint = if (isCurrentPageBookmarked) colors.warningGold else colors.textPrimary
                                )
                            }
                            if (!viewerState.localFilePath.isNullOrBlank()) {
                                IconButton(onClick = {
                                    runCatching {
                                        val pdfFile = File(viewerState.localFilePath)
                                        if (pdfFile.exists()) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                pdfFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, paperTitle)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            val chooser = Intent.createChooser(shareIntent, "Share Study PDF").apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(chooser)
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = "Share PDF",
                                        tint = colors.textPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        // Inline Streaming Search Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search formula, theorem, or keyword…") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pdf_search_input")
                                )
                                if (searchMatches.isNotEmpty()) {
                                    val safeIdx = activeMatchIndex.coerceIn(0, searchMatches.lastIndex)
                                    Text(
                                        text = "${safeIdx + 1}/${searchMatches.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.accentBlue
                                    )
                                    IconButton(onClick = {
                                        val prev = (safeIdx - 1).let { if (it < 0) searchMatches.lastIndex else it }
                                        activeMatchIndex = prev
                                        jumpToPage(searchMatches[prev].pageNumber)
                                    }) {
                                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Previous match")
                                    }
                                    IconButton(onClick = {
                                        val next = (safeIdx + 1) % searchMatches.size
                                        activeMatchIndex = next
                                        jumpToPage(searchMatches[next].pageNumber)
                                    }) {
                                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Next match")
                                    }
                                }
                                IconButton(onClick = {
                                    showSearchBar = false
                                    searchQuery = ""
                                    searchMatches = emptyList()
                                }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Close search")
                                }
                            }

                            if (isSearching && searchTotalPages > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (searchScannedPages.toFloat() / searchTotalPages.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (searchMatches.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(searchMatches, key = { it.pageNumber }) { match ->
                                        Surface(
                                            onClick = {
                                                activeMatchIndex = searchMatches.indexOf(match).coerceAtLeast(0)
                                                jumpToPage(match.pageNumber)
                                                showChrome = false
                                            },
                                            shape = RoundedCornerShape(ImtihanRadii.chip),
                                            color = colors.cardElevated,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                                Text(
                                                    text = "Page ${match.pageNumber} (${match.matchCountOnPage} hit${if (match.matchCountOnPage > 1) "s" else ""})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.accentBlue,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = match.snippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textPrimary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (!isSearching && searchQuery.trim().length >= 2) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No text matches found for \"${searchQuery.trim()}\". (Scanned image pages without OCR layer cannot be text-searched.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Floating Auto-Hide HUD
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = colors.cardSurface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("pdf_hud_bottom")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (actualPageCount > 1) {
                        var sliderPos by remember(currentPageNumber) {
                            mutableFloatStateOf(currentPageNumber.toFloat())
                        }
                        Slider(
                            value = sliderPos,
                            onValueChange = { value ->
                                sliderPos = value
                                scrubberPreviewPage = value.roundToInt().coerceIn(1, actualPageCount)
                            },
                            onValueChangeFinished = {
                                val target = sliderPos.roundToInt().coerceIn(1, actualPageCount)
                                scrubberPreviewPage = null
                                jumpToPage(target)
                            },
                            valueRange = 1f..actualPageCount.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accentBlue,
                                activeTrackColor = colors.accentBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val nextZoom = (zoomScale - 0.4f).coerceIn(1.0f, 4.0f)
                                zoomScale = nextZoom
                                if (nextZoom <= 1.02f) panOffset = Offset.Zero
                                latestOnUpdatePosition(currentPageNumber, actualPageCount, (nextZoom * 100f).roundToInt())
                            }) {
                                Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom out", tint = colors.textPrimary)
                            }
                            TextButton(
                                onClick = {
                                    zoomScale = 1.0f
                                    panOffset = Offset.Zero
                                    latestOnUpdatePosition(currentPageNumber, actualPageCount, 100)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "$zoomPercent%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.accentBlue
                                )
                            }
                            IconButton(onClick = {
                                val nextZoom = (zoomScale + 0.5f).coerceIn(1.0f, 4.0f)
                                zoomScale = nextZoom
                                latestOnUpdatePosition(currentPageNumber, actualPageCount, (nextZoom * 100f).roundToInt())
                            }) {
                                Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom in", tint = colors.textPrimary)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { if (currentPageNumber > 1) jumpToPage(currentPageNumber - 1) },
                                enabled = currentPageNumber > 1,
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Prev", maxLines = 1)
                            }
                            TextButton(
                                onClick = {
                                    jumpPageInput = currentPageNumber.toString()
                                    showJumpDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$currentPageNumber / $actualPageCount",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.accentBlue,
                                    maxLines = 1
                                )
                            }
                            Button(
                                onClick = { if (currentPageNumber < actualPageCount) jumpToPage(currentPageNumber + 1) },
                                enabled = currentPageNumber < actualPageCount,
                                shape = RoundedCornerShape(ImtihanRadii.chip),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                            ) {
                                Text("Next", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page (1 – $actualPageCount)") },
            text = {
                OutlinedTextField(
                    value = jumpPageInput,
                    onValueChange = { jumpPageInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Page number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = jumpPageInput.toIntOrNull()?.coerceIn(1, actualPageCount) ?: currentPageNumber
                    jumpToPage(target)
                    showJumpDialog = false
                }) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showStudyDrawer) {
        AlertDialog(
            onDismissRequest = { showStudyDrawer = false },
            title = { Text("Contents, Bookmarks & Notes") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(selectedTabIndex = studyDrawerTab) {
                        Tab(selected = studyDrawerTab == 0, onClick = { studyDrawerTab = 0 }, text = { Text("Contents") })
                        Tab(selected = studyDrawerTab == 1, onClick = { studyDrawerTab = 1 }, text = { Text("Saved (${bookmarks.size})") })
                        Tab(selected = studyDrawerTab == 2, onClick = { studyDrawerTab = 2 }, text = { Text("Notes (${annotations.size})") })
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    when (studyDrawerTab) {
                        0 -> {
                            if (outlineNodes.isEmpty()) {
                                Text(
                                    text = "No embedded Table of Contents in this PDF. Use Search or Page Scrubber to navigate chapters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 260.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(outlineNodes) { node ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    jumpToPage(node.pageNumber)
                                                    showStudyDrawer = false
                                                }
                                                .padding(
                                                    start = (node.level * 12).dp,
                                                    top = 8.dp,
                                                    bottom = 8.dp,
                                                    end = 4.dp
                                                ),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = node.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textPrimary,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "p.${node.pageNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.accentBlue
                                            )
                                        }
                                        HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        1 -> {
                            if (bookmarks.isEmpty()) {
                                Text(
                                    text = "Tap the Bookmark icon on any textbook page to save quick revision markers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 260.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(bookmarks, key = { it.id }) { bm ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    jumpToPage(bm.pageNumber)
                                                    showStudyDrawer = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("${bm.label} (Page ${bm.pageNumber})", modifier = Modifier.fillMaxWidth())
                                            }
                                            IconButton(onClick = { onToggleBookmark(bm.pageNumber, bm.label) }) {
                                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove bookmark")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = noteDraft,
                                    onValueChange = { noteDraft = it },
                                    label = { Text("Add note for Page $currentPageNumber") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (noteDraft.isNotBlank()) {
                                            onAddAnnotation(currentPageNumber, noteDraft)
                                            noteDraft = ""
                                        }
                                    },
                                    enabled = noteDraft.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Study Note")
                                }
                                if (annotations.isNotEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier.heightIn(max = 180.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(annotations, key = { it.id }) { note ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        jumpToPage(note.pageNumber)
                                                        showStudyDrawer = false
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
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStudyDrawer = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PdfTextbookPageSlot(
    pageIndex: Int,
    totalPages: Int,
    coordinator: PdfRenderCoordinator,
    readingTint: PdfReadingTint,
    isCurrentPage: Boolean,
    activeZoomScale: Float,
    activePanOffset: Offset,
    onUpdateZoomAndPan: (nextZoom: Float, nextPan: Offset) -> Unit,
    onCenterTap: () -> Unit,
    onLeftEdgeTap: () -> Unit,
    onRightEdgeTap: () -> Unit,
    onRetryWholePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    var retryNonce by remember { mutableIntStateOf(0) }
    var pageRatio by remember(pageIndex, coordinator) { mutableStateOf(PdfPageRatio(pageIndex, 595, 842)) }
    var fitBitmap by remember(pageIndex, coordinator) { mutableStateOf<Bitmap?>(null) }
    var highDpiTile by remember(pageIndex, coordinator) { mutableStateOf<Bitmap?>(null) }
    var renderError by remember(pageIndex, coordinator) { mutableStateOf<String?>(null) }

    val latestZoom by rememberUpdatedState(activeZoomScale)
    val latestPan by rememberUpdatedState(activePanOffset)
    val latestRatio by rememberUpdatedState(pageRatio)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val viewportWidthPx = constraints.maxWidth.coerceAtLeast(320)
        val viewportHeightPx = constraints.maxHeight.coerceAtLeast(480)

        // Step 1: Progressive fit-width bitmap render (instant from pre-warmed cache when turning pages)
        LaunchedEffect(pageIndex, viewportWidthPx, viewportHeightPx, coordinator, retryNonce) {
            renderError = null
            pageRatio = coordinator.getPageRatio(pageIndex)
            val result = coordinator.renderFitPage(
                pageIndex = pageIndex,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                forceRefresh = retryNonce > 0
            )
            result.fold(
                onSuccess = { (bmp, actualRatio) ->
                    fitBitmap = bmp
                    pageRatio = actualRatio
                },
                onFailure = { err ->
                    renderError = err.message ?: "Failed to render page ${pageIndex + 1}."
                }
            )
        }

        // Step 2: High-DPI Matrix viewport tile render whenever zoomed past 1.15x on the active page
        LaunchedEffect(isCurrentPage, activeZoomScale, activePanOffset, viewportWidthPx, viewportHeightPx, coordinator) {
            if (!isCurrentPage || activeZoomScale <= 1.15f) {
                highDpiTile = null
                return@LaunchedEffect
            }
            delay(85)
            coordinator.renderHighDpiViewportTile(
                pageIndex = pageIndex,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                zoomScale = activeZoomScale,
                panX = activePanOffset.x,
                panY = activePanOffset.y
            ).onSuccess { (tile, _) ->
                highDpiTile = tile
            }
        }

        val gestureModifier = Modifier
            .fillMaxSize()
            .pointerInput(pageIndex, isCurrentPage, viewportWidthPx, viewportHeightPx) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (!isCurrentPage) return@detectTapGestures
                        if (latestZoom > 1.1f) {
                            onUpdateZoomAndPan(1.0f, Offset.Zero)
                        } else {
                            val targetZoom = 2.25f
                            val desiredPanX = (viewportWidthPx / 2f - tapOffset.x) * (targetZoom - 1f)
                            val desiredPanY = (viewportHeightPx / 2f - tapOffset.y) * (targetZoom - 1f)
                            val clamped = PdfViewportMath.computePanBounds(
                                pageWidthPts = latestRatio.widthPts,
                                pageHeightPts = latestRatio.heightPts,
                                viewportWidthPx = viewportWidthPx.toFloat(),
                                viewportHeightPx = viewportHeightPx.toFloat(),
                                zoomScale = targetZoom,
                                desiredPanX = desiredPanX,
                                desiredPanY = desiredPanY
                            )
                            onUpdateZoomAndPan(targetZoom, Offset(clamped.clampedX, clamped.clampedY))
                        }
                    },
                    onTap = { tapOffset ->
                        if (!isCurrentPage) return@detectTapGestures
                        if (latestZoom <= 1.05f) {
                            val fracX = tapOffset.x / viewportWidthPx.toFloat()
                            when {
                                fracX < 0.16f && pageIndex > 0 -> onLeftEdgeTap()
                                fracX > 0.84f && pageIndex + 1 < totalPages -> onRightEdgeTap()
                                else -> onCenterTap()
                            }
                        } else {
                            onCenterTap()
                        }
                    }
                )
            }
            .pointerInput(pageIndex, isCurrentPage, viewportWidthPx, viewportHeightPx) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (!isCurrentPage) continue
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            val zoomFactor = event.calculateZoom()
                            val panDelta = event.calculatePan()
                            val nextZoom = (latestZoom * zoomFactor).coerceIn(1.0f, 4.0f)
                            val clamped = PdfViewportMath.computePanBounds(
                                pageWidthPts = latestRatio.widthPts,
                                pageHeightPts = latestRatio.heightPts,
                                viewportWidthPx = viewportWidthPx.toFloat(),
                                viewportHeightPx = viewportHeightPx.toFloat(),
                                zoomScale = nextZoom,
                                desiredPanX = latestPan.x + panDelta.x,
                                desiredPanY = latestPan.y + panDelta.y
                            )
                            onUpdateZoomAndPan(nextZoom, Offset(clamped.clampedX, clamped.clampedY))
                            event.changes.forEach { it.consume() }
                        } else if (pressedCount == 1 && latestZoom > 1.05f) {
                            val panDelta = event.calculatePan()
                            if (panDelta != Offset.Zero) {
                                val clamped = PdfViewportMath.computePanBounds(
                                    pageWidthPts = latestRatio.widthPts,
                                    pageHeightPts = latestRatio.heightPts,
                                    viewportWidthPx = viewportWidthPx.toFloat(),
                                    viewportHeightPx = viewportHeightPx.toFloat(),
                                    zoomScale = latestZoom,
                                    desiredPanX = latestPan.x + panDelta.x,
                                    desiredPanY = latestPan.y + panDelta.y
                                )
                                onUpdateZoomAndPan(latestZoom, Offset(clamped.clampedX, clamped.clampedY))
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }

        Box(
            modifier = gestureModifier,
            contentAlignment = Alignment.Center
        ) {
            when {
                renderError != null -> {
                    Surface(
                        shape = RoundedCornerShape(ImtihanRadii.card),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier
                            .padding(24.dp)
                            .testTag("pdf_page_error_${pageIndex + 1}")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Page ${pageIndex + 1} could not be rendered cleanly.",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = renderError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onRetryWholePdf) {
                                    Text("Re-download")
                                }
                                Button(
                                    onClick = { retryNonce++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
                                ) {
                                    Text("Retry Page")
                                }
                            }
                        }
                    }
                }

                fitBitmap == null -> {
                    // Aspect-ratio matched placeholder prevents stretched flash or layout jump
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, colors.cardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .aspectRatio(pageRatio.aspectRatio)
                            .testTag("pdf_page_placeholder_${pageIndex + 1}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = colors.accentBlue,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Rendering Page ${pageIndex + 1}…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Layer 1: Full-page aspect-preserved bitmap scaled smoothly during gestures
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = activeZoomScale
                                scaleY = activeZoomScale
                                translationX = activePanOffset.x
                                translationY = activePanOffset.y
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = fitBitmap!!.asImageBitmap(),
                            contentDescription = "Textbook Page ${pageIndex + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pdf_page_image_${pageIndex + 1}")
                        )
                    }

                    // Layer 2: High-DPI native Matrix-rendered viewport overlay at >1.15x zoom
                    val sharpTile = highDpiTile
                    if (isCurrentPage && activeZoomScale > 1.15f && sharpTile != null && !sharpTile.isRecycled) {
                        Image(
                            bitmap = sharpTile.asImageBitmap(),
                            contentDescription = "High-DPI zoomed Page ${pageIndex + 1}",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pdf_page_hidpi_tile_${pageIndex + 1}")
                        )
                    }

                    // Reading comfort tint overlay without inverting diagrams/equations
                    readingTint.overlayTint?.let { tintColor ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(tintColor)
                        )
                    }
                }
            }
        }
    }
}
