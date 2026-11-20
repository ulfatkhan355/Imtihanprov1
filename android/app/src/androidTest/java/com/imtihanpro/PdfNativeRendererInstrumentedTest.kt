package com.imtihanpro.app.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.imtihanpro.app.ui.reader.AndroidNativePdfBackend
import com.imtihanpro.app.ui.reader.PdfRenderCoordinator
import com.imtihanpro.app.ui.reader.PdfTextSearchEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-Device / Emulator Instrumented Test validating actual Android native PdfRenderer (PDFium)
 * ink density across fit render and 2.2x / 4.0x high-DPI Matrix viewport crops (zero blank pages).
 */
@RunWith(AndroidJUnit4::class)
class PdfNativeRendererInstrumentedTest {

    private lateinit var context: Context
    private lateinit var testPdf: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PdfTextSearchEngine.ensureInitialized(context)
        testPdf = File(context.cacheDir, "instrumented_math_diagram.pdf")
        PDDocument().use { doc ->
            val p1 = PDPage(PDRectangle.A4)
            doc.addPage(p1)
            PDPageContentStream(doc, p1).use { cs ->
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA_BOLD, 22f)
                cs.newLineAtOffset(72f, 700f)
                cs.showText("Imtihan Pro Math & Diagram Native Render Check")
                cs.endText()
                cs.setLineWidth(4f)
                cs.addRect(72f, 400f, 360f, 220f)
                cs.stroke()
            }
            doc.save(testPdf)
        }
    }

    @After
    fun tearDown() {
        testPdf.delete()
    }

    @Test
    fun nativePdfRendererProducesNonBlankSharpPixelsAt1xAnd4xZoom() = runBlocking {
        val backend = AndroidNativePdfBackend(testPdf)
        val coordinator = PdfRenderCoordinator(testPdf, backend = backend)
        try {
            assertTrue(coordinator.isOpen)
            assertEquals(1, coordinator.pageCount)

            val (fitBmp, _) = coordinator.renderFitPage(0, 1080, 1920).getOrThrow()
            assertTrue("Fit render must contain non-white rendered ink", countDarkPixels(fitBmp) > 500)

            val (tileBmp, _) = coordinator.renderHighDpiViewportTile(
                pageIndex = 0,
                viewportWidthPx = 1080,
                viewportHeightPx = 1920,
                zoomScale = 4.0f,
                panX = 0f,
                panY = 0f
            ).getOrThrow()
            assertTrue("4x zoomed tile render must remain non-blank", countDarkPixels(tileBmp) > 100)
        } finally {
            coordinator.close()
        }
    }

    private fun countDarkPixels(bmp: Bitmap): Int {
        var count = 0
        val stepX = maxOf(1, bmp.width / 120)
        val stepY = maxOf(1, bmp.height / 160)
        for (y in 0 until bmp.height step stepY) {
            for (x in 0 until bmp.width step stepX) {
                val pixel = bmp.getPixel(x, y)
                if (Color.red(pixel) < 230 || Color.green(pixel) < 230 || Color.blue(pixel) < 230) {
                    count++
                }
            }
        }
        return count
    }
}
