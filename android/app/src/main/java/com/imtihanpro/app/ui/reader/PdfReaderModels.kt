package com.imtihanpro.app.ui.reader

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

enum class PdfReadingTint(val label: String, val backdropColor: Color, val overlayTint: Color?) {
    PAPER(
        label = "Paper",
        backdropColor = Color(0xFFF6F8FC),
        overlayTint = null
    ),
    WARM(
        label = "Warm",
        backdropColor = Color(0xFFF3EAD3),
        overlayTint = Color(0x1FD99A36)
    ),
    DIM(
        label = "Dim",
        backdropColor = Color(0xFF141C2B),
        overlayTint = Color(0x2E0D1422)
    )
}

data class PdfPageRatio(
    val pageIndex: Int,
    val widthPts: Int,
    val heightPts: Int
) {
    val aspectRatio: Float
        get() = if (heightPts > 0 && widthPts > 0) {
            (widthPts.toFloat() / heightPts.toFloat()).coerceIn(0.35f, 2.5f)
        } else {
            0.707f // Standard A4 ratio fallback
        }
}

data class PdfHighDpiTileSpec(
    val pageIndex: Int,
    val zoomScale: Float,
    val panX: Float,
    val panY: Float,
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    val matrixScale: Float,
    val matrixTranslateX: Float,
    val matrixTranslateY: Float
)

data class PdfPanBounds(
    val maxPanX: Float,
    val maxPanY: Float,
    val clampedX: Float,
    val clampedY: Float
) {
    val isAtLeftEdge: Boolean get() = maxPanX <= 1f || clampedX >= maxPanX - 2f
    val isAtRightEdge: Boolean get() = maxPanX <= 1f || clampedX <= -maxPanX + 2f
}

data class PdfSearchMatch(
    val pageNumber: Int,
    val snippet: String,
    val matchCountOnPage: Int
)

data class PdfOutlineNode(
    val title: String,
    val pageNumber: Int,
    val level: Int = 0
)

/**
 * Pure Kotlin viewport, DPI, caching budget, and matrix transform math for PdfRenderer.
 * Tested directly on JVM without Android graphics dependencies.
 */
object PdfViewportMath {

    private const val DEFAULT_MAX_BASE_PIXELS = 2_400_000L // ~9.6 MB ARGB_8888
    private const val DEFAULT_MAX_TILE_PIXELS = 3_600_000L // ~14.4 MB ARGB_8888

    fun computeCacheBudgetBytes(maxHeapBytes: Long): Int {
        val raw = (maxHeapBytes / 8L)
        return raw.coerceIn(16L * 1024L * 1024L, 48L * 1024L * 1024L).toInt()
    }

    /**
     * Preserves exact PDF page aspect ratio while sizing fit-width bitmaps cleanly for crisp
     * typography and diagrams without over-allocating memory on jumbo pages.
     */
    fun computeBaseFitSize(
        pageWidthPts: Int,
        pageHeightPts: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        qualityBoost: Float = 1.35f,
        maxPixels: Long = DEFAULT_MAX_BASE_PIXELS
    ): Pair<Int, Int> {
        val safePageW = pageWidthPts.coerceAtLeast(1)
        val safePageH = pageHeightPts.coerceAtLeast(1)
        val safeViewW = viewportWidthPx.coerceAtLeast(360)
        val safeViewH = viewportHeightPx.coerceAtLeast(480)

        val fitScale = minOf(
            safeViewW.toFloat() / safePageW.toFloat(),
            safeViewH.toFloat() / safePageH.toFloat()
        )
        val targetScale = fitScale * qualityBoost.coerceIn(1.0f, 2.0f)

        var outW = (safePageW * targetScale).roundToInt().coerceAtLeast(360)
        var outH = (safePageH * targetScale).roundToInt().coerceAtLeast(360)

        val totalPixels = outW.toLong() * outH.toLong()
        if (totalPixels > maxPixels) {
            val downscale = kotlin.math.sqrt(maxPixels.toDouble() / totalPixels.toDouble())
            outW = (outW * downscale).roundToInt().coerceAtLeast(320)
            outH = (outH * downscale).roundToInt().coerceAtLeast(320)
        }
        return outW to outH
    }

    fun computePanBounds(
        pageWidthPts: Int,
        pageHeightPts: Int,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        zoomScale: Float,
        desiredPanX: Float,
        desiredPanY: Float
    ): PdfPanBounds {
        if (viewportWidthPx <= 0f || viewportHeightPx <= 0f || pageWidthPts <= 0 || pageHeightPts <= 0) {
            return PdfPanBounds(0f, 0f, 0f, 0f)
        }
        val fitScale = minOf(viewportWidthPx / pageWidthPts, viewportHeightPx / pageHeightPts)
        val scaledW = pageWidthPts * fitScale * zoomScale
        val scaledH = pageHeightPts * fitScale * zoomScale
        val maxPanX = maxOf(0f, (scaledW - viewportWidthPx) / 2f)
        val maxPanY = maxOf(0f, (scaledH - viewportHeightPx) / 2f)
        return PdfPanBounds(
            maxPanX = maxPanX,
            maxPanY = maxPanY,
            clampedX = desiredPanX.coerceIn(-maxPanX, maxPanX),
            clampedY = desiredPanY.coerceIn(-maxPanY, maxPanY)
        )
    }

    /**
     * Computes viewport-aligned Matrix parameters so PdfRenderer renders ONLY the visible zoomed
     * window directly from vector outlines & embedded diagrams at high DPI.
     */
    fun computeHighDpiTileSpec(
        pageIndex: Int,
        pageWidthPts: Int,
        pageHeightPts: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        zoomScale: Float,
        panX: Float,
        panY: Float,
        desiredSuperSample: Float = 1.25f,
        maxTilePixels: Long = DEFAULT_MAX_TILE_PIXELS
    ): PdfHighDpiTileSpec {
        val safeW = viewportWidthPx.coerceAtLeast(320)
        val safeH = viewportHeightPx.coerceAtLeast(320)
        val safePageW = pageWidthPts.coerceAtLeast(1)
        val safePageH = pageHeightPts.coerceAtLeast(1)

        var superSample = desiredSuperSample.coerceIn(1.0f, 1.5f)
        val rawPixels = (safeW * superSample) * (safeH * superSample)
        if (rawPixels > maxTilePixels) {
            superSample *= kotlin.math.sqrt(maxTilePixels.toFloat() / rawPixels.toFloat())
        }

        val bitmapW = (safeW * superSample).roundToInt().coerceAtLeast(256)
        val bitmapH = (safeH * superSample).roundToInt().coerceAtLeast(256)
        val actualSampleX = bitmapW.toFloat() / safeW.toFloat()

        val fitScale = minOf(safeW.toFloat() / safePageW, safeH.toFloat() / safePageH)
        val dispW = safePageW * fitScale
        val dispH = safePageH * fitScale

        val bounds = computePanBounds(
            pageWidthPts = safePageW,
            pageHeightPts = safePageH,
            viewportWidthPx = safeW.toFloat(),
            viewportHeightPx = safeH.toFloat(),
            zoomScale = zoomScale,
            desiredPanX = panX,
            desiredPanY = panY
        )

        val pageLeftScreen = safeW / 2f - (dispW * zoomScale) / 2f + bounds.clampedX
        val pageTopScreen = safeH / 2f - (dispH * zoomScale) / 2f + bounds.clampedY

        return PdfHighDpiTileSpec(
            pageIndex = pageIndex,
            zoomScale = zoomScale,
            panX = bounds.clampedX,
            panY = bounds.clampedY,
            bitmapWidth = bitmapW,
            bitmapHeight = bitmapH,
            matrixScale = fitScale * zoomScale * actualSampleX,
            matrixTranslateX = pageLeftScreen * actualSampleX,
            matrixTranslateY = pageTopScreen * actualSampleX
        )
    }
}
