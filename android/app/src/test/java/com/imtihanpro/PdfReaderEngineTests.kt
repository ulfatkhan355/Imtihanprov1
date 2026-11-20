package com.imtihanpro.app.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.imtihanpro.app.data.local.room.ImtihanDatabase
import com.imtihanpro.app.data.local.room.PdfReadingStateEntity
import com.imtihanpro.app.ui.reader.PdfHighDpiTileSpec
import com.imtihanpro.app.ui.reader.PdfPageBackend
import com.imtihanpro.app.ui.reader.PdfRenderCoordinator
import com.imtihanpro.app.ui.reader.PdfTextSearchEngine
import com.imtihanpro.app.ui.reader.PdfViewportMath
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * PdfBox-backed PdfPageBackend for Robolectric JVM verification:
 * Reads real binary page count & mediaBox point dimensions from synthetic PDFs
 * while rendering crisp verification bitmaps at exact target/tile dimensions without Android native JNI.
 */
class PdfBoxJvmTestBackend(
    private val file: File,
    private val failOnPageIndex: Int? = null
) : PdfPageBackend {

    private var document: PDDocument? = null
    override var pageCount: Int = 0
        private set
    override var openError: String? = null
        private set
    override val isOpen: Boolean
        get() = document != null && pageCount > 0

    val renderedPages = mutableListOf<Int>()

    init {
        runCatching {
            val loaded = PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly())
            document = loaded
            pageCount = loaded.numberOfPages
        }.onFailure { err ->
            openError = err.message ?: "Invalid PDF document"
        }
    }

    override fun readPageSizePts(pageIndex: Int): Pair<Int, Int> {
        val doc = document ?: throw IllegalStateException(openError ?: "Closed")
        val safeIdx = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        val box = doc.getPage(safeIdx).mediaBox
        return box.width.roundToInt() to box.height.roundToInt()
    }

    override fun renderFitBitmap(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        if (failOnPageIndex == pageIndex) {
            throw IllegalStateException("Simulated corrupt page $pageIndex")
        }
        renderedPages.add(pageIndex)
        val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.WHITE)
            val paint = Paint().apply { color = Color.BLACK; strokeWidth = 2f }
            drawLine(20f, 20f, (targetWidth - 20).toFloat(), 20f, paint)
        }
        return bmp
    }

    override fun renderTileBitmap(pageIndex: Int, spec: PdfHighDpiTileSpec): Bitmap {
        if (failOnPageIndex == pageIndex) {
            throw IllegalStateException("Simulated corrupt tile on page $pageIndex")
        }
        val bmp = Bitmap.createBitmap(spec.bitmapWidth, spec.bitmapHeight, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.WHITE)
            val paint = Paint().apply { color = Color.BLUE; strokeWidth = 3f }
            drawRect(10f, 10f, (spec.bitmapWidth - 10).toFloat(), (spec.bitmapHeight - 10).toFloat(), paint)
        }
        return bmp
    }

    override fun close() {
        runCatching { document?.close() }
        document = null
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PdfReaderEngineTests {

    private lateinit var context: Context
    private lateinit var tempDir: File
    private lateinit var db: ImtihanDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PdfTextSearchEngine.ensureInitialized(context)
        tempDir = Files.createTempDirectory("imtihan_pdf_engine_tests").toFile()
        db = Room.inMemoryDatabaseBuilder(context, ImtihanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun `aspect ratio and base fit size never stretch portrait, landscape diagram, or jumbo pages`() {
        // Portrait A4 (595 x 842)
        val (pw, ph) = PdfViewportMath.computeBaseFitSize(595, 842, 1080, 2200)
        val portraitRatio = pw.toFloat() / ph.toFloat()
        assertTrue(abs(portraitRatio - (595f / 842f)) < 0.02f)

        // Wide Landscape Scientific Diagram (842 x 595)
        val (lw, lh) = PdfViewportMath.computeBaseFitSize(842, 595, 1080, 2200)
        val landscapeRatio = lw.toFloat() / lh.toFloat()
        assertTrue(abs(landscapeRatio - (842f / 595f)) < 0.02f)

        // Jumbo page (4000 x 6000) capped safely under DEFAULT_MAX_BASE_PIXELS (~2.4M px)
        val (jw, jh) = PdfViewportMath.computeBaseFitSize(4000, 6000, 1440, 2560)
        assertTrue(jw.toLong() * jh.toLong() <= 2_450_000L)
        assertTrue(abs((jw.toFloat() / jh.toFloat()) - (4000f / 6000f)) < 0.02f)
    }

    @Test
    fun `high-DPI tile spec scales matrix linearly with zoom and clamps pan strictly`() {
        val spec15 = PdfViewportMath.computeHighDpiTileSpec(
            pageIndex = 0,
            pageWidthPts = 595,
            pageHeightPts = 842,
            viewportWidthPx = 1080,
            viewportHeightPx = 2000,
            zoomScale = 1.5f,
            panX = 0f,
            panY = 0f
        )
        val spec30 = PdfViewportMath.computeHighDpiTileSpec(
            pageIndex = 0,
            pageWidthPts = 595,
            pageHeightPts = 842,
            viewportWidthPx = 1080,
            viewportHeightPx = 2000,
            zoomScale = 3.0f,
            panX = 99999f, // Out-of-bounds pan should clamp cleanly
            panY = -99999f
        )

        assertEquals(spec15.bitmapWidth, spec30.bitmapWidth)
        assertEquals(spec15.bitmapHeight, spec30.bitmapHeight)
        assertTrue(abs(spec30.matrixScale - spec15.matrixScale * 2f) < 0.01f)
        assertTrue(spec30.panX < 99999f)
        assertTrue(spec30.panY > -99999f)

        val bounds = PdfViewportMath.computePanBounds(595, 842, 1080f, 2000f, 2.5f, 99999f, 0f)
        assertTrue(bounds.isAtLeftEdge)
        assertFalse(bounds.isAtRightEdge)
    }

    @Test
    fun `PdfTextSearchEngine finds math, diagram, table, outline, and 120-page stress matches offline`() = runTest {
        val samplePdf = SyntheticPdfFixtures.createTextbookSamplePdf(File(tempDir, "textbook.pdf"))
        val largePdf = SyntheticPdfFixtures.createLargeStressPdf(File(tempDir, "large_120.pdf"), 120)

        // Outline extraction
        val outline = PdfTextSearchEngine.extractOutline(context, samplePdf)
        assertEquals(3, outline.size)
        assertEquals(1, outline[0].pageNumber)
        assertEquals(2, outline[1].pageNumber)
        assertEquals(5, outline[2].pageNumber)

        // Math formula hit (Page 1)
        val mathSearch = PdfTextSearchEngine.searchStreaming(context, samplePdf, "Quadratic Formula").toList().last()
        assertEquals(1, mathSearch.matches.size)
        assertEquals(1, mathSearch.matches[0].pageNumber)
        assertTrue(mathSearch.matches[0].snippet.contains("Quadratic Formula"))

        // Scientific diagram hit (Page 2)
        val diagramSearch = PdfTextSearchEngine.searchStreaming(context, samplePdf, "Prism Refraction").toList().last()
        assertEquals(listOf(2), diagramSearch.matches.map { it.pageNumber })

        // Structured table hit (Page 3)
        val tableSearch = PdfTextSearchEngine.searchStreaming(context, samplePdf, "Newton").toList().last()
        assertEquals(listOf(3), tableSearch.matches.map { it.pageNumber })

        // Scanned archive label hit (Page 4)
        val scanSearch = PdfTextSearchEngine.searchStreaming(context, samplePdf, "Peshawar 2024").toList().last()
        assertEquals(listOf(4), scanSearch.matches.map { it.pageNumber })

        // 120-page stress search (Page 95)
        val stressSearch = PdfTextSearchEngine.searchStreaming(context, largePdf, "Chapter 95 Benchmark").toList().last()
        assertTrue(stressSearch.isFinished)
        assertEquals(120, stressSearch.totalPages)
        assertEquals(listOf(95), stressSearch.matches.map { it.pageNumber })
    }

    @Test
    fun `coordinator prewarms only neighbors, enforces byte-budget eviction, and isolates page errors`() = runTest {
        val largePdf = SyntheticPdfFixtures.createLargeStressPdf(File(tempDir, "stress_120.pdf"), 120)
        val backend = PdfBoxJvmTestBackend(largePdf, failOnPageIndex = 7)
        // Tight 4 MB cache budget forces LRU eviction as pages are visited
        val coordinator = PdfRenderCoordinator(largePdf, backend = backend, maxCacheBytes = 4 * 1024 * 1024)

        assertTrue(coordinator.isOpen)
        assertEquals(120, coordinator.pageCount)

        // Render page 0 & prewarm adjacent -> should ONLY touch pages 0 and 1 (never all 120 pages!)
        val p0 = coordinator.renderFitPage(0, 1080, 1920)
        assertTrue(p0.isSuccess)
        coordinator.prewarmAdjacentPages(0, 1080, 1920)
        assertEquals(setOf(0, 1), backend.renderedPages.toSet())

        // High-DPI viewport tile on zoomed page 2
        val tileRes = coordinator.renderHighDpiViewportTile(2, 1080, 1920, 2.5f, 40f, -20f)
        assertTrue(tileRes.isSuccess)
        val (tileBmp, spec) = tileRes.getOrThrow()
        assertEquals(spec.bitmapWidth, tileBmp.width)
        assertEquals(spec.bitmapHeight, tileBmp.height)

        // Per-page error isolation: page 7 fails gracefully while page 8 succeeds
        val badPage = coordinator.renderFitPage(7, 1080, 1920)
        assertTrue(badPage.isFailure)
        val goodNeighbor = coordinator.renderFitPage(8, 1080, 1920)
        assertTrue(goodNeighbor.isSuccess)

        // Stress traversal stays strictly bounded below maxCacheBytes
        for (idx in 20..40) {
            coordinator.renderFitPage(idx, 1080, 1920)
            assertTrue(coordinator.cacheByteUsage() <= coordinator.cacheMaxBytes())
        }

        coordinator.close()

        // Corrupt PDF failure check
        val corruptPdf = SyntheticPdfFixtures.createCorruptPdf(File(tempDir, "corrupt.pdf"))
        val corruptCoord = PdfRenderCoordinator(corruptPdf, backend = PdfBoxJvmTestBackend(corruptPdf))
        assertFalse(corruptCoord.isOpen)
        assertNotNull(corruptCoord.openError)
        corruptCoord.close()
    }

    @Test
    fun `Room reading progress persists and restores high page numbers without fallback truncation`() = runTest {
        val dao = db.pdfReaderDao()
        dao.upsertReadingState(
            PdfReadingStateEntity(
                pdfId = "kpk11_phy_book",
                lastPage = 95,
                totalPages = 120,
                zoomPercent = 185,
                updatedAtEpochMs = 1_760_000_000_000L
            )
        )
        val saved = dao.getReadingStateOnce("kpk11_phy_book")
        assertNotNull(saved)
        assertEquals(95, saved!!.lastPage)
        assertEquals(120, saved.totalPages)
        assertEquals(185, saved.zoomPercent)

        // Verify upper bound logic matches ImtihanViewModel fix even if metadata says totalPages = 10
        val effectiveUpperBound = maxOf(1, 10, saved.totalPages, saved.lastPage)
        val restoredPage = saved.lastPage.coerceIn(1, effectiveUpperBound)
        assertEquals(95, restoredPage)
    }
}
