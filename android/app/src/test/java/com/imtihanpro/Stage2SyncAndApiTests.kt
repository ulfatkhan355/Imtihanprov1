package com.imtihanpro.app

import com.imtihanpro.app.data.remote.ApiChapterProgressSyncDto
import com.imtihanpro.app.data.remote.ApiContentBlockDto
import com.imtihanpro.app.data.remote.ApiResult
import com.imtihanpro.app.data.remote.ImtihanApiClient
import com.imtihanpro.app.data.remote.ImtihanSyncRules
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class Stage2SyncAndApiTests {

    private lateinit var server: MockWebServer
    private lateinit var client: ImtihanApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ImtihanApiClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchSubjects parses Cloudflare Worker envelope accurately`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "success": true,
                      "data": [
                        {
                          "id": "sub_kpk10_phy",
                          "classLevel": 10,
                          "boardCode": "KPK",
                          "code": "PHY-10",
                          "name": "Physics",
                          "iconKey": "physics",
                          "totalChapters": 12,
                          "publishedChapters": 2,
                          "coverageStatus": "active_seed"
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val baseUrl = server.url("/v1").toString()
        val result = client.fetchSubjects(baseUrl, 10, "KPK")
        assertTrue(result is ApiResult.Success)
        val subjects = (result as ApiResult.Success).data
        assertEquals(1, subjects.size)
        assertEquals("Physics", subjects[0].name)
        assertEquals(12, subjects[0].totalChapters)
    }

    @Test
    fun `fetchChapterContent parses heading, math, and mcq blocks cleanly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "success": true,
                      "data": {
                        "id": "cv_1",
                        "chapterId": "ch_phy10_01",
                        "mode": "detailed_notes",
                        "version": "v1.4.0",
                        "publishStatus": "published",
                        "updatedAtIso": "2026-11-14T10:00:00Z",
                        "blocks": [
                          { "id": "b1", "orderIndex": 1, "type": "heading", "sectionCode": "1.1", "text": "SHM", "isSubheading": false },
                          { "id": "b2", "orderIndex": 2, "type": "math_expression", "latex": "F = -kx", "plainNotation": "F = -kx", "caption": "Hooke Law" }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val baseUrl = server.url("/v1").toString()
        val res = client.fetchChapterContent(baseUrl, "ch_phy10_01")
        assertTrue(res is ApiResult.Success)
        val content = (res as ApiResult.Success).data
        assertEquals("v1.4.0", content.version)
        assertEquals(2, content.blocks.size)
        assertTrue(content.blocks[0] is ApiContentBlockDto.HeadingDto)
        assertTrue(content.blocks[1] is ApiContentBlockDto.MathExpressionDto)
    }

    @Test
    fun `downloadPdfToFile saves valid PDF stream and rejects non-PDF payload`() = runTest {
        val tempDir = Files.createTempDirectory("imtihan_pdf_test").toFile()
        try {
            val validPdfBytes = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".toByteArray(Charsets.US_ASCII)
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/pdf")
                    .setBody(Buffer().write(validPdfBytes))
            )

            val outFile = File(tempDir, "paper_1.pdf")
            val validSha256 = java.security.MessageDigest.getInstance("SHA-256")
                .digest(validPdfBytes)
                .joinToString("") { "%02x".format(it) }
            val ok = client.downloadPdfToFile(
                server.url("/v1").toString(),
                "pdf_001",
                outFile,
                expectedSha256 = validSha256
            )
            assertTrue(ok is ApiResult.Success)
            assertTrue(outFile.exists())
            assertEquals(validPdfBytes.size.toLong(), outFile.length())

            // Reject mismatched SHA-256 checksum
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/pdf")
                    .setBody(Buffer().write(validPdfBytes))
            )
            val hashFailFile = File(tempDir, "paper_bad_sha.pdf")
            val hashFail = client.downloadPdfToFile(
                server.url("/v1").toString(),
                "pdf_001",
                hashFailFile,
                expectedSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
            )
            assertTrue(hashFail is ApiResult.Failure)
            assertFalse(hashFailFile.exists())

            // Reject invalid non-PDF payload
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("NOT A REAL PDF FILE")
            )
            val badFile = File(tempDir, "paper_bad.pdf")
            val bad = client.downloadPdfToFile(server.url("/v1").toString(), "pdf_bad", badFile)
            assertTrue(bad is ApiResult.Failure)
            assertFalse(badFile.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `mergeProgressMonotonic preserves higher progress and completion flags`() {
        val local = listOf(
            ApiChapterProgressSyncDto("ch_1", "sub_1", 80, 7, false, "2026-11-14T10:00:00Z")
        )
        val remote = listOf(
            ApiChapterProgressSyncDto("ch_1", "sub_1", 50, 9, true, "2026-11-14T09:00:00Z"),
            ApiChapterProgressSyncDto("ch_2", "sub_1", 40, 2, false, "2026-11-14T11:00:00Z")
        )
        val merged = ImtihanSyncRules.mergeProgressMonotonic(local, remote)
        assertEquals(2, merged.size)
        val ch1 = merged.first { it.chapterId == "ch_1" }
        assertEquals(80, ch1.completionPercent)
        assertEquals(9, ch1.lastBlockIndex)
        assertTrue(ch1.isCompleted)
    }

    @Test
    fun `validateWorkerBaseUrl supports local Ubuntu LAN and rejects insecure remote HTTP`() {
        assertTrue(ImtihanSyncRules.validateWorkerBaseUrl("http://10.0.2.2:8787/v1").first)
        assertTrue(ImtihanSyncRules.validateWorkerBaseUrl("http://192.168.1.42:8787/v1").first)
        assertTrue(ImtihanSyncRules.validateWorkerBaseUrl("https://imtihan-pro-api.workers.dev/v1").first)
        assertFalse(ImtihanSyncRules.validateWorkerBaseUrl("http://public.example.com/v1").first)
    }

    @Test
    fun `downloadPdfToFile resumes partial tmp file using Range header on 206 response`() = runTest {
        val tempDir = Files.createTempDirectory("imtihan_resume_test").toFile()
        try {
            val part1 = "%PDF-1.4\n1 0 ".toByteArray(Charsets.US_ASCII)
            val part2 = "obj\n<<>>\nendobj\n%%EOF".toByteArray(Charsets.US_ASCII)
            val combined = part1 + part2

            val outFile = File(tempDir, "resumed.pdf")
            val partialTmp = File(tempDir, "resumed.pdf.tmp")
            partialTmp.writeBytes(part1)

            server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setHeader("Content-Type", "application/pdf")
                    .setBody(Buffer().write(part2))
            )

            val expectedSha = java.security.MessageDigest.getInstance("SHA-256")
                .digest(combined)
                .joinToString("") { "%02x".format(it) }

            val res = client.downloadPdfToFile(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_resume",
                targetFile = outFile,
                expectedSha256 = expectedSha
            )
            assertTrue(res is ApiResult.Success)
            assertTrue(outFile.exists())
            assertFalse(partialTmp.exists())
            assertEquals(combined.size.toLong(), outFile.length())

            val recordedRequest = server.takeRequest()
            assertEquals("bytes=${part1.size}-", recordedRequest.getHeader("Range"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `latexToHtmlFragment renders fractions, roots, matrices and escapes HTML`() {
        val fracHtml = com.imtihanpro.app.ui.components.latexToHtmlFragment("\\frac{a+b}{c}")
        assertTrue(fracHtml.contains("class=\"frac\""))
        assertTrue(fracHtml.contains("<span class=\"num\">a+b</span>"))
        assertTrue(fracHtml.contains("<span class=\"den\">c</span>"))

        val matrixHtml = com.imtihanpro.app.ui.components.latexToHtmlFragment("\\begin{bmatrix}1 & 0\\\\0 & 1\\end{bmatrix}")
        assertTrue(matrixHtml.contains("matrix-wrap"))
        assertTrue(matrixHtml.contains("repeat(2, auto)"))

        val escaped = com.imtihanpro.app.ui.components.latexToHtmlFragment("x < y & \\sqrt{9}")
        assertTrue(escaped.contains("&lt;"))
        assertTrue(escaped.contains("&amp;"))
        assertTrue(escaped.contains("class=\"sqrt\""))
    }
}
