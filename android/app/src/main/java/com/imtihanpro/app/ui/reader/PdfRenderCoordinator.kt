package com.imtihanpro.app.ui.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstraction over Android native PdfRenderer so production uses hardware/PDFium rendering
 * while JVM/Robolectric tests can supply real PDFBox-backed dimensions or deterministic backends.
 */
interface PdfPageBackend : Closeable {
    val pageCount: Int
    val isOpen: Boolean
    val openError: String?
    fun readPageSizePts(pageIndex: Int): Pair<Int, Int>
    fun renderFitBitmap(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap
    fun renderTileBitmap(pageIndex: Int, spec: PdfHighDpiTileSpec): Bitmap
}

class AndroidNativePdfBackend(private val file: File) : PdfPageBackend {
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    override var pageCount: Int = 0
        private set
    override var openError: String? = null
        private set

    override val isOpen: Boolean
        get() = renderer != null && pageCount > 0

    init {
        runCatching {
            if (!file.exists() || !file.canRead()) {
                throw IllegalStateException("Offline PDF file missing or unreadable.")
            }
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(pfd)
            descriptor = pfd
            renderer = pdfRenderer
            pageCount = pdfRenderer.pageCount
        }.onFailure { err ->
            openError = err.message ?: "Unable to open PDF document."
            close()
        }
    }

    override fun readPageSizePts(pageIndex: Int): Pair<Int, Int> {
        val activeRenderer = renderer ?: throw IllegalStateException(openError ?: "PDF renderer closed.")
        val safeIdx = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        val page = activeRenderer.openPage(safeIdx)
        return try {
            page.width to page.height
        } finally {
            page.close()
        }
    }

    override fun renderFitBitmap(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        val activeRenderer = renderer ?: throw IllegalStateException(openError ?: "PDF renderer closed.")
        val safeIdx = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        val page = activeRenderer.openPage(safeIdx)
        return try {
            val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            Canvas(bmp).drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        } finally {
            page.close()
        }
    }

    override fun renderTileBitmap(pageIndex: Int, spec: PdfHighDpiTileSpec): Bitmap {
        val activeRenderer = renderer ?: throw IllegalStateException(openError ?: "PDF renderer closed.")
        val safeIdx = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        val page = activeRenderer.openPage(safeIdx)
        return try {
            val matrix = Matrix().apply {
                setScale(spec.matrixScale, spec.matrixScale)
                postTranslate(spec.matrixTranslateX, spec.matrixTranslateY)
            }
            val tileBitmap = Bitmap.createBitmap(spec.bitmapWidth, spec.bitmapHeight, Bitmap.Config.ARGB_8888)
            Canvas(tileBitmap).drawColor(Color.WHITE)
            page.render(tileBitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            tileBitmap
        } finally {
            page.close()
        }
    }

    override fun close() {
        runCatching { renderer?.close() }
        runCatching { descriptor?.close() }
        renderer = null
        descriptor = null
    }
}

/**
 * Thread-safe PDF rendering coordinator with:
 * - Byte-budget LruCache safe for 4GB-8GB RAM devices
 * - Fast page aspect-ratio inspection (zero bitmap cost)
 * - Adjacent neighbor pre-warming (page - 1, page + 1)
 * - High-DPI Matrix viewport tile rendering for crisp math/diagrams/tables at 1.2x..4.0x zoom
 * - Per-page error isolation and clean recovery
 */
class PdfRenderCoordinator(
    private val file: File,
    private val backend: PdfPageBackend = AndroidNativePdfBackend(file),
    maxCacheBytes: Int = PdfViewportMath.computeCacheBudgetBytes(Runtime.getRuntime().maxMemory())
) : Closeable {

    private val mutex = Mutex()
    private val ratios = ConcurrentHashMap<Int, PdfPageRatio>()

    val pageCount: Int
        get() = backend.pageCount

    val openError: String?
        get() = backend.openError

    val isOpen: Boolean
        get() = backend.isOpen

    private val fitCache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun getCachedFitBitmap(pageIndex: Int, targetWidth: Int): Bitmap? =
        fitCache.get(fitKey(pageIndex, targetWidth))

    fun cacheByteUsage(): Int = fitCache.size()
    fun cacheMaxBytes(): Int = fitCache.maxSize()

    suspend fun getPageRatio(pageIndex: Int): PdfPageRatio {
        ratios[pageIndex]?.let { return it }
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                ratios[pageIndex]?.let { return@withLock it }
                if (!backend.isOpen) return@withLock PdfPageRatio(pageIndex, 595, 842)
                val safeIdx = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                runCatching {
                    val (w, h) = backend.readPageSizePts(safeIdx)
                    PdfPageRatio(safeIdx, w, h).also { ratios[safeIdx] = it }
                }.getOrElse {
                    PdfPageRatio(safeIdx, 595, 842)
                }
            }
        }
    }

    suspend fun renderFitPage(
        pageIndex: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        forceRefresh: Boolean = false
    ): Result<Pair<Bitmap, PdfPageRatio>> = withContext(Dispatchers.Default) {
        val ratio = getPageRatio(pageIndex)
        val (targetW, targetH) = PdfViewportMath.computeBaseFitSize(
            pageWidthPts = ratio.widthPts,
            pageHeightPts = ratio.heightPts,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx
        )
        val key = fitKey(pageIndex, targetW)
        if (!forceRefresh) {
            fitCache.get(key)?.let { cached ->
                if (!cached.isRecycled) {
                    return@withContext Result.success(cached to ratio)
                }
            }
        }

        mutex.withLock {
            if (!forceRefresh) {
                fitCache.get(key)?.let { cached ->
                    if (!cached.isRecycled) {
                        return@withLock Result.success(cached to ratio)
                    }
                }
            }
            if (!backend.isOpen) {
                return@withLock Result.failure(IllegalStateException(backend.openError ?: "PDF renderer closed."))
            }
            val safeIdx = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

            runCatching {
                val (wPts, hPts) = backend.readPageSizePts(safeIdx)
                val actualRatio = PdfPageRatio(safeIdx, wPts, hPts).also { ratios[safeIdx] = it }
                val (w, h) = PdfViewportMath.computeBaseFitSize(
                    pageWidthPts = actualRatio.widthPts,
                    pageHeightPts = actualRatio.heightPts,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx
                )
                val bmp = backend.renderFitBitmap(safeIdx, w, h)
                fitCache.put(fitKey(safeIdx, w), bmp)
                bmp to actualRatio
            }
        }
    }

    /**
     * Renders ONLY the currently visible zoomed viewport crop natively using PdfRenderer's Matrix
     * transform, producing razor-sharp vector text, math formulas, and diagrams at up to 4.0x zoom.
     */
    suspend fun renderHighDpiViewportTile(
        pageIndex: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        zoomScale: Float,
        panX: Float,
        panY: Float
    ): Result<Pair<Bitmap, PdfHighDpiTileSpec>> = withContext(Dispatchers.Default) {
        val ratio = getPageRatio(pageIndex)
        val spec = PdfViewportMath.computeHighDpiTileSpec(
            pageIndex = pageIndex,
            pageWidthPts = ratio.widthPts,
            pageHeightPts = ratio.heightPts,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            zoomScale = zoomScale,
            panX = panX,
            panY = panY
        )

        mutex.withLock {
            if (!backend.isOpen) {
                return@withLock Result.failure(IllegalStateException(backend.openError ?: "PDF renderer closed."))
            }
            val safeIdx = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            runCatching {
                val tileBitmap = backend.renderTileBitmap(safeIdx, spec)
                tileBitmap to spec
            }
        }
    }

    /**
     * Windowed neighbor pre-warming: renders only active page - 1 and + 1 in the background
     * so realistic horizontal page flips appear instantaneously without loading the entire book.
     */
    suspend fun prewarmAdjacentPages(
        currentPageIndex: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int
    ) {
        if (!isOpen || viewportWidthPx <= 0 || viewportHeightPx <= 0) return
        val targets = listOf(currentPageIndex + 1, currentPageIndex - 1)
            .filter { it in 0 until pageCount }
        for (idx in targets) {
            renderFitPage(idx, viewportWidthPx, viewportHeightPx)
        }
    }

    fun clearCache() {
        fitCache.evictAll()
    }

    override fun close() {
        fitCache.evictAll()
        runCatching { backend.close() }
    }

    private fun fitKey(pageIndex: Int, widthPx: Int): String {
        val bucket = (widthPx / 32) * 32
        return "fit_${pageIndex}_$bucket"
    }
}
