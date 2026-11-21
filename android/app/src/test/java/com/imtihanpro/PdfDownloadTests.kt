package com.imtihanpro.app

import android.content.Context
import android.content.res.Configuration
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.imtihanpro.app.data.local.room.DownloadTaskEntity
import com.imtihanpro.app.data.local.room.ImtihanDatabase
import com.imtihanpro.app.data.remote.ApiResult
import com.imtihanpro.app.data.remote.ImtihanApiClient
import com.imtihanpro.app.data.remote.PdfDownloadOutcome
import com.imtihanpro.app.data.repository.MockSeedDataSource
import com.imtihanpro.app.domain.engine.DownloadFailureDecision
import com.imtihanpro.app.domain.engine.DownloadPausedReason
import com.imtihanpro.app.domain.engine.DownloadState
import com.imtihanpro.app.domain.engine.PdfDownloadEngine
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale

class PdfDownloadEngineAndClientTests {

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

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `wifi-only gate enforces unmetered Wi-Fi above 5 MB unless overridden`() {
        val over5Mb = 6L * 1024L * 1024L
        val under5Mb = 2L * 1024L * 1024L

        assertTrue(PdfDownloadEngine.requiresUnmeteredWifi(over5Mb, allowMobileDataForItem = false, globalAllowMobileData = false))
        assertFalse(PdfDownloadEngine.requiresUnmeteredWifi(under5Mb, allowMobileDataForItem = false, globalAllowMobileData = false))
        assertFalse(PdfDownloadEngine.requiresUnmeteredWifi(over5Mb, allowMobileDataForItem = true, globalAllowMobileData = false))
        assertFalse(PdfDownloadEngine.requiresUnmeteredWifi(over5Mb, allowMobileDataForItem = false, globalAllowMobileData = true))
    }

    @Test
    fun `storage guard checks remaining bytes plus 10 MB safety margin`() {
        val total = 4L * 1024L * 1024L
        val already = 1L * 1024L * 1024L
        val exactRequired = (total - already) + PdfDownloadEngine.FREE_STORAGE_MARGIN_BYTES

        assertTrue(PdfDownloadEngine.hasSufficientStorage(exactRequired, total, already))
        assertFalse(PdfDownloadEngine.hasSufficientStorage(exactRequired - 1L, total, already))
    }

    @Test
    fun `classifyHttpFailure returns RestartFromZero on 416`() {
        val decision = PdfDownloadEngine.classifyHttpFailure(
            statusCode = 416,
            errorCode = "RANGE_NOT_SATISFIABLE",
            message = "Out of bounds",
            retryAfterHeader = null,
            attempt = 0
        )
        assertTrue(decision is DownloadFailureDecision.RestartFromZero)
    }

    @Test
    fun `classifyHttpFailure treats 403 and 404 as terminal failures without retrying`() {
        val forbidden = PdfDownloadEngine.classifyHttpFailure(403, "FORBIDDEN", "Denied", null, 0)
        val notFound = PdfDownloadEngine.classifyHttpFailure(404, "NOT_FOUND", "Missing", null, 0)

        assertTrue(forbidden is DownloadFailureDecision.TerminalFailure)
        assertTrue(notFound is DownloadFailureDecision.TerminalFailure)
    }

    @Test
    fun `classifyHttpFailure retries 408 429 5xx and network errors honoring Retry-After`() {
        val d429 = PdfDownloadEngine.classifyHttpFailure(429, "B2_DAILY_BANDWIDTH_CAP", "Cap", "45", 0, jitterRatio = 0.0)
        assertTrue(d429 is DownloadFailureDecision.Retryable)
        assertEquals(45_000L, (d429 as DownloadFailureDecision.Retryable).delayMs)

        val d503 = PdfDownloadEngine.classifyHttpFailure(503, "HTTP_503", "Busy", null, 1, jitterRatio = 0.0)
        assertTrue(d503 is DownloadFailureDecision.Retryable)
        assertEquals(30_000L, (d503 as DownloadFailureDecision.Retryable).delayMs)

        val d408 = PdfDownloadEngine.classifyHttpFailure(408, "HTTP_408", "Timeout", null, 0, jitterRatio = 0.5)
        assertTrue(d408 is DownloadFailureDecision.Retryable)
        assertTrue((d408 as DownloadFailureDecision.Retryable).delayMs > 15_000L)

        val dNet = PdfDownloadEngine.classifyHttpFailure(null, "NETWORK_ERROR", "Socket reset", null, 0, jitterRatio = 0.0)
        assertTrue(dNet is DownloadFailureDecision.Retryable)

        val dExhausted = PdfDownloadEngine.classifyHttpFailure(503, "HTTP_503", "Busy", null, PdfDownloadEngine.MAX_RETRY_ATTEMPTS)
        assertTrue(dExhausted is DownloadFailureDecision.TerminalFailure)
    }

    @Test
    fun `parseContentRange and strong ETag helpers handle valid and malformed headers`() {
        val valid = PdfDownloadEngine.parseContentRange("bytes 1024-2047/4096")
        assertNotNull(valid)
        assertEquals(1024L, valid!!.start)
        assertEquals(2047L, valid.end)
        assertEquals(4096L, valid.totalBytes)

        val star416 = PdfDownloadEngine.parseContentRange("bytes */4096")
        assertNotNull(star416)
        assertEquals(-1L, star416!!.start)
        assertEquals(4096L, star416.totalBytes)

        assertNull(PdfDownloadEngine.parseContentRange("items 0-5/10"))
        assertNull(PdfDownloadEngine.normalizeStrongEtag("W/\"weak-tag\""))

        val hex = "a".repeat(64)
        assertEquals("\"$hex\"", PdfDownloadEngine.normalizeStrongEtag("\"$hex\""))
        assertEquals(hex, PdfDownloadEngine.extractSha256FromEtagIfHex("\"$hex\""))
    }

    @Test
    fun `state machine enforces valid download transitions`() {
        assertTrue(PdfDownloadEngine.canTransition(DownloadState.QUEUED, DownloadState.RUNNING))
        assertTrue(PdfDownloadEngine.canTransition(DownloadState.RUNNING, DownloadState.PAUSED))
        assertTrue(PdfDownloadEngine.canTransition(DownloadState.RUNNING, DownloadState.VERIFYING))
        assertTrue(PdfDownloadEngine.canTransition(DownloadState.VERIFYING, DownloadState.COMPLETED))
        assertTrue(PdfDownloadEngine.canTransition(DownloadState.FAILED, DownloadState.QUEUED))
        assertFalse(PdfDownloadEngine.canTransition(DownloadState.COMPLETED, DownloadState.RUNNING))
        assertFalse(PdfDownloadEngine.canTransition(DownloadState.QUEUED, DownloadState.COMPLETED))
    }

    @Test
    fun `probePdfHead sends HEAD request and extracts Content-Length ETag Accept-Ranges`() = runTest {
        val sha = "b".repeat(64)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "1048576")
                .setHeader("ETag", "\"$sha\"")
                .setHeader("Accept-Ranges", "bytes")
        )

        val res = client.probePdfHead(server.url("/v1").toString(), "pdf_head_1")
        assertTrue(res is ApiResult.Success)
        val probe = (res as ApiResult.Success).data
        assertEquals(1_048_576L, probe.contentLength)
        assertEquals("\"$sha\"", probe.etag)
        assertTrue(probe.acceptRangesBytes)
        assertEquals(sha, probe.sha256Hint)

        val recorded = server.takeRequest()
        assertEquals("HEAD", recorded.method)
    }

    @Test
    fun `downloadPdfResumable performs Range + If-Range 206 resume and atomic rename`() = runTest {
        val tempDir = Files.createTempDirectory("imtihan_dl_206").toFile()
        try {
            val part1 = "%PDF-1.5\n1 0 obj\n<< /Length 4 >>\n".toByteArray(Charsets.US_ASCII)
            val part2 = "stream\nabcd\nendstream\n%%EOF".toByteArray(Charsets.US_ASCII)
            val combined = part1 + part2
            val sha = sha256Hex(combined)
            val etag = "\"$sha\""

            val target = File(tempDir, "notes.pdf")
            val tmp = File(tempDir, "notes.pdf.tmp")
            tmp.writeBytes(part1)

            server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setHeader("Content-Type", "application/pdf")
                    .setHeader("Content-Range", "bytes ${part1.size}-${combined.size - 1}/${combined.size}")
                    .setHeader("ETag", etag)
                    .setBody(Buffer().write(part2))
            )

            var verifyingCalled = false
            val outcome = client.downloadPdfResumable(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_206",
                targetFile = target,
                expectedSha256 = sha,
                expectedTotalBytes = combined.size.toLong(),
                knownEtag = etag,
                onVerifying = { verifyingCalled = true }
            )

            assertTrue(outcome is PdfDownloadOutcome.Success)
            assertTrue(verifyingCalled)
            assertTrue(target.exists())
            assertFalse(tmp.exists())
            assertEquals(combined.size.toLong(), target.length())
            assertEquals(sha, (outcome as PdfDownloadOutcome.Success).verifiedSha256)

            val req = server.takeRequest()
            assertEquals("bytes=${part1.size}-", req.getHeader("Range"))
            assertEquals(etag, req.getHeader("If-Range"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `If-Range mismatch returning 200 overwrites stale tmp cleanly without corrupting file`() = runTest {
        val tempDir = Files.createTempDirectory("imtihan_dl_ifrange").toFile()
        try {
            val freshPdf = "%PDF-1.7\n1 0 obj\n<< /Updated true >>\nendobj\n%%EOF".toByteArray(Charsets.US_ASCII)
            val freshSha = sha256Hex(freshPdf)

            val target = File(tempDir, "updated.pdf")
            val tmp = File(tempDir, "updated.pdf.tmp")
            tmp.writeBytes("%PDF-1.4\nSTALE_OLD_PART".toByteArray(Charsets.US_ASCII))

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/pdf")
                    .setHeader("ETag", "\"$freshSha\"")
                    .setBody(Buffer().write(freshPdf))
            )

            val outcome = client.downloadPdfResumable(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_ifrange",
                targetFile = target,
                expectedSha256 = freshSha,
                knownEtag = "\"old_etag_value\""
            )

            assertTrue(outcome is PdfDownloadOutcome.Success)
            assertEquals(freshPdf.toList(), target.readBytes().toList())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `416 response cleans stale tmp and downloadPdfToFile recovers cleanly from byte 0`() = runTest {
        val tempDir = Files.createTempDirectory("imtihan_dl_416").toFile()
        try {
            val fullPdf = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".toByteArray(Charsets.US_ASCII)
            val fullSha = sha256Hex(fullPdf)

            val target = File(tempDir, "recover.pdf")
            val tmp = File(tempDir, "recover.pdf.tmp")
            tmp.writeBytes(ByteArray(9999) { 0x41 })

            server.enqueue(
                MockResponse()
                    .setResponseCode(416)
                    .setHeader("Content-Range", "bytes */${fullPdf.size}")
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/pdf")
                    .setHeader("ETag", "\"$fullSha\"")
                    .setBody(Buffer().write(fullPdf))
            )

            val res = client.downloadPdfToFile(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_416",
                targetFile = target,
                expectedSha256 = fullSha
            )

            assertTrue(res is ApiResult.Success)
            assertTrue(target.exists())
            assertFalse(tmp.exists())
            assertEquals(2, server.requestCount)
            val req1 = server.takeRequest()
            val req2 = server.takeRequest()
            assertNotNull(req1.getHeader("Range"))
            assertNull(req2.getHeader("Range"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `corrupt SHA-256 or missing PDF header or truncated payload deletes tmp and returns CorruptFile`() = runTest {
        val tempDir = Files.createTempDirectory("imtihan_dl_corrupt").toFile()
        try {
            val validPdf = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".toByteArray(Charsets.US_ASCII)

            // 1. Checksum mismatch
            server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(validPdf)))
            val target1 = File(tempDir, "bad_sha.pdf")
            val out1 = client.downloadPdfResumable(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_bad_sha",
                targetFile = target1,
                expectedSha256 = "f".repeat(64)
            )
            assertTrue(out1 is PdfDownloadOutcome.CorruptFile)
            assertEquals("CHECKSUM_MISMATCH", (out1 as PdfDownloadOutcome.CorruptFile).code)
            assertFalse(File(tempDir, "bad_sha.pdf.tmp").exists())
            assertFalse(target1.exists())

            // 2. Missing %PDF- header
            server.enqueue(MockResponse().setResponseCode(200).setBody("<html>Not a PDF</html>"))
            val target2 = File(tempDir, "bad_magic.pdf")
            val out2 = client.downloadPdfResumable(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_bad_magic",
                targetFile = target2
            )
            assertTrue(out2 is PdfDownloadOutcome.CorruptFile)
            assertEquals("INVALID_PDF_STREAM", (out2 as PdfDownloadOutcome.CorruptFile).code)
            assertFalse(File(tempDir, "bad_magic.pdf.tmp").exists())

            // 3. Size mismatch against declared expectedTotalBytes
            server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(validPdf)))
            val target3 = File(tempDir, "bad_size.pdf")
            val out3 = client.downloadPdfResumable(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_bad_size",
                targetFile = target3,
                expectedTotalBytes = validPdf.size.toLong() + 500L
            )
            assertTrue(out3 is PdfDownloadOutcome.CorruptFile)
            assertEquals("SIZE_MISMATCH", (out3 as PdfDownloadOutcome.CorruptFile).code)
            assertFalse(File(tempDir, "bad_size.pdf.tmp").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `429 Retry-After header and 404 errors are surfaced cleanly in HttpFailure`() = runTest {
        val tempDir = Files.createTempDirectory("imtihan_dl_http").toFile()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(429)
                    .setHeader("Retry-After", "120")
                    .setBody("""{"success":false,"error":{"code":"B2_DAILY_BANDWIDTH_CAP","message":"Daily limit reached"}}""")
            )
            val out429 = client.downloadPdfResumable(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_cap",
                targetFile = File(tempDir, "cap.pdf")
            )
            assertTrue(out429 is PdfDownloadOutcome.HttpFailure)
            val f429 = out429 as PdfDownloadOutcome.HttpFailure
            assertEquals(429, f429.statusCode)
            assertEquals("120", f429.retryAfterHeader)
            assertEquals("B2_DAILY_BANDWIDTH_CAP", f429.code)

            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"success":false,"error":{"code":"NOT_FOUND","message":"Missing"}}"""))
            val out404 = client.downloadPdfResumable(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_404",
                targetFile = File(tempDir, "missing.pdf")
            )
            assertTrue(out404 is PdfDownloadOutcome.HttpFailure)
            assertEquals(404, (out404 as PdfDownloadOutcome.HttpFailure).statusCode)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `mid-stream cancellation preserves partial tmp bytes for later resume`() = runTest {
        val tempDir = Files.createTempDirectory("imtihan_dl_pause").toFile()
        try {
            val chunk = ("%PDF-1.4\n" + "A".repeat(40_000)).toByteArray(Charsets.US_ASCII)
            server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(chunk)))

            var cancelled = false
            val target = File(tempDir, "paused.pdf")
            var checks = 0
            val outcome = client.downloadPdfResumable(
                baseUrl = server.url("/v1").toString(),
                pdfId = "pdf_pause",
                targetFile = target,
                isCancelled = {
                    checks += 1
                    if (checks >= 3) cancelled = true
                    cancelled
                }
            )
            assertTrue(outcome is PdfDownloadOutcome.PausedOrCancelled)
            val tmp = File(tempDir, "paused.pdf.tmp")
            assertTrue(tmp.exists())
            assertTrue(tmp.length() > 0L)
            assertFalse(target.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `progress percentage MB and speed formatters produce accurate KPK-friendly readouts`() {
        assertEquals(50, PdfDownloadEngine.computeProgressPercent(512L, 1024L))
        assertEquals(-1, PdfDownloadEngine.computeProgressPercent(512L, 0L))
        assertEquals("2.5 MB", PdfDownloadEngine.formatMb((2.5 * 1024 * 1024).toLong()))
        assertEquals("1.0 MB / 4.0 MB", PdfDownloadEngine.formatBytesProgress(1024L * 1024L, 4L * 1024L * 1024L))
        assertEquals("64 KB/s", PdfDownloadEngine.formatSpeedBps(64L * 1024L))
        assertEquals("1.5 MB/s", PdfDownloadEngine.formatSpeedBps((1.5 * 1024 * 1024).toLong()))

        val corruptDecision = PdfDownloadEngine.classifyHttpFailure(
            statusCode = 200,
            errorCode = "CHECKSUM_MISMATCH",
            message = "Hash mismatch",
            retryAfterHeader = null,
            attempt = 0
        )
        assertTrue(corruptDecision is DownloadFailureDecision.TerminalFailure)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PdfDownloadRoomAndLocalizationTests {

    private lateinit var context: Context
    private lateinit var db: ImtihanDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ImtihanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `DownloadTaskDao enforces single-active priority order and persists pause-resume fields`() = runTest {
        val dao = db.downloadTaskDao()
        dao.upsert(
            DownloadTaskEntity(
                pdfId = "pdf_q2",
                paperId = "p2",
                title = "Second Queued",
                categoryLabel = "Past Paper",
                remoteUrl = "https://api.example/v1/pdfs/pdf_q2/download",
                state = DownloadState.QUEUED.name,
                createdAtEpochMs = 200L,
                updatedAtEpochMs = 200L
            )
        )
        dao.upsert(
            DownloadTaskEntity(
                pdfId = "pdf_q1",
                paperId = "p1",
                title = "First Queued",
                categoryLabel = "Textbook",
                remoteUrl = "https://api.example/v1/pdfs/pdf_q1/download",
                state = DownloadState.QUEUED.name,
                createdAtEpochMs = 100L,
                updatedAtEpochMs = 100L
            )
        )
        dao.upsert(
            DownloadTaskEntity(
                pdfId = "pdf_paused_wifi",
                paperId = "pw",
                title = "Waiting Wifi",
                categoryLabel = "Textbook",
                remoteUrl = "https://api.example/v1/pdfs/pdf_paused_wifi/download",
                state = DownloadState.PAUSED.name,
                pausedReason = DownloadPausedReason.WAITING_WIFI.name,
                totalBytes = 8_000_000L,
                createdAtEpochMs = 50L,
                updatedAtEpochMs = 50L
            )
        )

        val next = dao.getNextRunnableOnce()
        assertNotNull(next)
        assertEquals("pdf_q1", next!!.pdfId)

        val autoPaused = dao.getAutoResumablePausedOnce()
        assertEquals(1, autoPaused.size)
        assertEquals("pdf_paused_wifi", autoPaused[0].pdfId)

        dao.allowMobileDataAndQueue("pdf_paused_wifi", allowMobileData = true, updatedAt = 300L)
        val afterOverride = dao.getByIdOnce("pdf_paused_wifi")!!
        assertEquals(DownloadState.QUEUED.name, afterOverride.state)
        assertTrue(afterOverride.allowMobileData)
    }

    @Test
    fun `DownloadTaskDao finds orphaned RUNNING and VERIFYING tasks after process death`() = runTest {
        val dao = db.downloadTaskDao()
        dao.upsert(
            DownloadTaskEntity(
                pdfId = "pdf_orphan_run",
                paperId = "p_r",
                title = "Running Interrupted",
                categoryLabel = "Textbook",
                remoteUrl = "https://api.example/v1/pdfs/pdf_orphan_run/download",
                state = DownloadState.RUNNING.name,
                bytesDownloaded = 1_048_576L,
                totalBytes = 4_194_304L,
                createdAtEpochMs = 10L,
                updatedAtEpochMs = 20L
            )
        )
        dao.upsert(
            DownloadTaskEntity(
                pdfId = "pdf_orphan_ver",
                paperId = "p_v",
                title = "Verifying Interrupted",
                categoryLabel = "Past Paper",
                remoteUrl = "https://api.example/v1/pdfs/pdf_orphan_ver/download",
                state = DownloadState.VERIFYING.name,
                bytesDownloaded = 1_572_864L,
                totalBytes = 1_572_864L,
                createdAtEpochMs = 15L,
                updatedAtEpochMs = 25L
            )
        )

        val orphans = dao.getOrphanedRunningOnce()
        assertEquals(2, orphans.size)
        for (orphan in orphans) {
            dao.updateState(orphan.pdfId, DownloadState.QUEUED.name, DownloadPausedReason.NONE.name, null, null, 100L)
        }
        assertTrue(dao.getOrphanedRunningOnce().isEmpty())
        assertEquals(2, dao.getQueuedOnce().size)
    }

    @Test
    fun `MockSeedDataSource pdfLibrary has valid 64-char SHA-256 hex checksums on every PDF`() {
        assertTrue(MockSeedDataSource.pdfLibrary.isNotEmpty())
        for (pdf in MockSeedDataSource.pdfLibrary) {
            assertTrue(
                "PDF ${pdf.id} has invalid SHA-256: '${pdf.checksumSha256}'",
                PdfDownloadEngine.isValidSha256Hex(pdf.checksumSha256)
            )
        }
    }

    @Test
    fun `English and Urdu download state resources resolve cleanly`() {
        val enQueued = context.getString(R.string.dl_state_queued)
        assertEquals("Queued", enQueued)
        assertTrue(context.getString(R.string.dl_action_mobile_data).isNotBlank())

        val urConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale("ur"))
        }
        val urContext = context.createConfigurationContext(urConfig)
        val urQueued = urContext.getString(R.string.dl_state_queued)
        val urMobileData = urContext.getString(R.string.dl_action_mobile_data)
        assertTrue(urQueued.isNotBlank() && urQueued != enQueued)
        assertTrue(urMobileData.isNotBlank())
    }
}
