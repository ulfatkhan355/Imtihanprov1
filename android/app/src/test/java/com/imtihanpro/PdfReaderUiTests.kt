package com.imtihanpro.app.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import com.imtihanpro.app.domain.model.PdfAnnotationUi
import com.imtihanpro.app.domain.model.PdfBookmarkUi
import com.imtihanpro.app.ui.reader.ImtihanPdfReader
import com.imtihanpro.app.ui.reader.PdfRenderCoordinator
import com.imtihanpro.app.ui.reader.PdfTextSearchEngine
import com.imtihanpro.app.ui.theme.ImtihanProTheme
import com.imtihanpro.app.ui.viewmodel.PdfViewerUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PdfReaderUiTests {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var tempDir: File
    private lateinit var samplePdf: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PdfTextSearchEngine.ensureInitialized(context)
        tempDir = Files.createTempDirectory("imtihan_pdf_ui_tests").toFile()
        samplePdf = SyntheticPdfFixtures.createTextbookSamplePdf(File(tempDir, "kpk_physics_11.pdf"))
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `horizontal swipe turns page realistically, HUD auto-hides, search jumps, and bookmarks trigger`() {
        var latestPage = 1
        val bookmarkedPages = mutableListOf<Int>()
        val bookmarksState = mutableStateOf<List<PdfBookmarkUi>>(emptyList())

        composeRule.setContent {
            ImtihanProTheme(darkTheme = false) {
                ImtihanPdfReader(
                    paperTitle = "Class 11 Physics Notes",
                    fallbackTotalPages = 5,
                    viewerState = PdfViewerUiState(
                        paperId = "phy_11",
                        pdfId = "phy_11",
                        localFilePath = samplePdf.absolutePath,
                        isDownloading = false,
                        currentPage = 1,
                        zoomPercent = 100
                    ),
                    bookmarks = bookmarksState.value,
                    annotations = emptyList<PdfAnnotationUi>(),
                    onRetryDownload = {},
                    onUpdatePosition = { page, _, _ -> latestPage = page },
                    onToggleBookmark = { page, label ->
                        bookmarkedPages.add(page)
                        bookmarksState.value = listOf(PdfBookmarkUi("bm_$page", "phy_11", page, label))
                    },
                    onAddAnnotation = { _, _ -> },
                    onDeleteAnnotation = {},
                    onBack = {},
                    coordinatorFactory = { f -> PdfRenderCoordinator(f, backend = PdfBoxJvmTestBackend(f)) }
                )
            }
        }

        // Unobstructed initial view: PDF page 1 rendered, floating HUD hidden
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pdf_page_image_1").assertIsDisplayed()
        composeRule.onNodeWithTag("pdf_hud_top").assertDoesNotExist()

        // Swipe left cleanly transitions horizontally to Page 2
        composeRule.onNodeWithTag("pdf_pager").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 3_000L) { latestPage == 2 }
        composeRule.onNodeWithTag("pdf_page_image_2").assertIsDisplayed()
        assertEquals(2, latestPage)

        // Center tap reveals floating HUD bars (advance past double-tap detection timeout)
        composeRule.onNodeWithTag("pdf_pager").performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodesWithTag("pdf_hud_top").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("pdf_hud_top").assertIsDisplayed()
        composeRule.onNodeWithTag("pdf_hud_bottom").assertIsDisplayed()

        composeRule.onNodeWithTag("pdf_bookmark_toggle").performClick()
        composeRule.waitForIdle()
        assertTrue(2 in bookmarkedPages)

        // Open search, query "Newton", wait for streaming hit on Page 3, tap result to jump to Page 3
        composeRule.onNodeWithTag("pdf_search_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pdf_search_input").performTextInput("Newton")

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Page 3 (1 hit)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Page 3 (1 hit)").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodesWithTag("pdf_page_image_3").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("pdf_page_image_3").assertIsDisplayed()
    }

    @Test
    fun `saved reading page restores cleanly after download without being overwritten by page 1`() {
        var viewerState by mutableStateOf(
            PdfViewerUiState(
                paperId = "large_book",
                pdfId = "large_book",
                localFilePath = null,
                isDownloading = true,
                currentPage = 1
            )
        )
        val reportedPages = mutableListOf<Int>()
        val largePdf = SyntheticPdfFixtures.createLargeStressPdf(File(tempDir, "large_restore.pdf"), 120)

        composeRule.setContent {
            ImtihanProTheme {
                ImtihanPdfReader(
                    paperTitle = "Class 11 Textbook",
                    fallbackTotalPages = 10,
                    viewerState = viewerState,
                    bookmarks = emptyList(),
                    annotations = emptyList(),
                    onRetryDownload = {},
                    onUpdatePosition = { page, _, _ -> reportedPages.add(page) },
                    onToggleBookmark = { _, _ -> },
                    onAddAnnotation = { _, _ -> },
                    onDeleteAnnotation = {},
                    onBack = {},
                    coordinatorFactory = { f -> PdfRenderCoordinator(f, backend = PdfBoxJvmTestBackend(f)) }
                )
            }
        }

        composeRule.onNodeWithTag("pdf_downloading_state").assertIsDisplayed()

        // Complete download and restore saved page 95 simultaneously
        viewerState = viewerState.copy(
            localFilePath = largePdf.absolutePath,
            isDownloading = false,
            currentPage = 95
        )
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            95 in reportedPages &&
                composeRule.onAllNodesWithTag("pdf_page_image_95").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("pdf_page_image_95").assertIsDisplayed()
        assertTrue(95 in reportedPages)
        assertTrue(1 !in reportedPages)
    }

    @Test
    fun `downloading state and corrupt PDF error recovery state display actionable controls`() {
        var viewerState by mutableStateOf(
            PdfViewerUiState(
                paperId = "p1",
                pdfId = "p1",
                localFilePath = null,
                isDownloading = true
            )
        )
        var retryTapped = false
        val corruptPdf = SyntheticPdfFixtures.createCorruptPdf(File(tempDir, "broken.pdf"))

        composeRule.setContent {
            ImtihanProTheme {
                ImtihanPdfReader(
                    paperTitle = "Broken Paper",
                    fallbackTotalPages = 5,
                    viewerState = viewerState,
                    bookmarks = emptyList(),
                    annotations = emptyList(),
                    onRetryDownload = { retryTapped = true },
                    onUpdatePosition = { _, _, _ -> },
                    onToggleBookmark = { _, _ -> },
                    onAddAnnotation = { _, _ -> },
                    onDeleteAnnotation = {},
                    onBack = {},
                    coordinatorFactory = { f -> PdfRenderCoordinator(f, backend = PdfBoxJvmTestBackend(f)) }
                )
            }
        }

        composeRule.onNodeWithTag("pdf_downloading_state").assertIsDisplayed()

        viewerState = viewerState.copy(
            localFilePath = corruptPdf.absolutePath,
            isDownloading = false
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pdf_document_error_state").assertIsDisplayed()
        composeRule.onNodeWithText("Download PDF").performClick()
        assertTrue(retryTapped)
    }
}
