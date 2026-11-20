package com.imtihanpro.app.ui.reader

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class PdfSearchProgress(
    val query: String,
    val scannedPages: Int,
    val totalPages: Int,
    val matches: List<PdfSearchMatch>,
    val isFinished: Boolean
)

/**
 * Offline PDF full-text search and outline (TOC) engine backed by PdfBox-Android.
 * Uses MemoryUsageSetting.setupTempFileOnly() and page-by-page streaming to remain responsive
 * and memory-safe on 4GB RAM devices even for 400+ page scanned/vector textbooks.
 */
object PdfTextSearchEngine {

    private val initialized = AtomicBoolean(false)

    fun ensureInitialized(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            runCatching {
                PDFBoxResourceLoader.init(context.applicationContext)
            }.onFailure {
                initialized.set(false)
            }
        }
    }

    suspend fun extractOutline(context: Context, file: File): List<PdfOutlineNode> = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                val outline = doc.documentCatalog?.documentOutline ?: return@use emptyList()
                val nodes = mutableListOf<PdfOutlineNode>()
                collectOutlineItems(doc, outline.firstChild, level = 0, out = nodes)
                nodes
            }
        }.getOrElse { emptyList() }
    }

    private fun collectOutlineItems(
        doc: PDDocument,
        startItem: PDOutlineItem?,
        level: Int,
        out: MutableList<PdfOutlineNode>
    ) {
        if (level > 3 || out.size >= 150) return
        var item = startItem
        while (item != null && out.size < 150) {
            val destPage = runCatching { item.findDestinationPage(doc) }.getOrNull()
            val pageIdx = if (destPage != null) doc.pages.indexOf(destPage) else -1
            if (pageIdx >= 0 && !item.title.isNullOrBlank()) {
                out.add(
                    PdfOutlineNode(
                        title = item.title.trim(),
                        pageNumber = pageIdx + 1,
                        level = level
                    )
                )
            }
            if (item.hasChildren()) {
                collectOutlineItems(doc, item.firstChild, level + 1, out)
            }
            item = item.nextSibling
        }
    }

    fun searchStreaming(
        context: Context,
        file: File,
        query: String
    ): Flow<PdfSearchProgress> = flow {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2 || !file.exists()) {
            emit(PdfSearchProgress(cleanQuery, 0, 0, emptyList(), isFinished = true))
            return@flow
        }

        ensureInitialized(context)
        val found = mutableListOf<PdfSearchMatch>()

        PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
            val total = doc.numberOfPages
            val stripper = PDFTextStripper().apply {
                sortByPosition = true
            }

            for (pageNum in 1..total) {
                currentCoroutineContext().ensureActive()
                stripper.startPage = pageNum
                stripper.endPage = pageNum
                val pageText = runCatching { stripper.getText(doc) }.getOrDefault("")
                val match = findMatchesInPageText(pageNum, pageText, cleanQuery)
                if (match != null) {
                    found.add(match)
                    emit(
                        PdfSearchProgress(
                            query = cleanQuery,
                            scannedPages = pageNum,
                            totalPages = total,
                            matches = found.toList(),
                            isFinished = pageNum == total
                        )
                    )
                } else if (pageNum % 10 == 0 || pageNum == total) {
                    emit(
                        PdfSearchProgress(
                            query = cleanQuery,
                            scannedPages = pageNum,
                            totalPages = total,
                            matches = found.toList(),
                            isFinished = pageNum == total
                        )
                    )
                }
            }

            if (total == 0) {
                emit(PdfSearchProgress(cleanQuery, 0, 0, emptyList(), isFinished = true))
            }
        }
    }.flowOn(Dispatchers.IO)

    internal fun findMatchesInPageText(
        pageNumber: Int,
        pageText: String,
        query: String
    ): PdfSearchMatch? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty() || pageText.isBlank()) return null

        val normalizedText = pageText.replace(Regex("\\s+"), " ").trim()
        val normalizedQuery = trimmedQuery.replace(Regex("\\s+"), " ")
        val lowerText = normalizedText.lowercase()
        val lowerQuery = normalizedQuery.lowercase()

        var idx = lowerText.indexOf(lowerQuery)
        if (idx < 0) return null

        val firstHit = idx
        var count = 0
        while (idx >= 0) {
            count++
            idx = lowerText.indexOf(lowerQuery, startIndex = idx + lowerQuery.length)
        }

        val start = (firstHit - 36).coerceAtLeast(0)
        val end = (firstHit + normalizedQuery.length + 56).coerceAtMost(normalizedText.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < normalizedText.length) "…" else ""
        val snippet = "$prefix${normalizedText.substring(start, end)}$suffix"

        return PdfSearchMatch(
            pageNumber = pageNumber,
            snippet = snippet,
            matchCountOnPage = count
        )
    }
}
