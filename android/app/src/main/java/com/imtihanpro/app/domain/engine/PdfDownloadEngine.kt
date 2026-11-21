package com.imtihanpro.app.domain.engine

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min
import kotlin.math.pow

enum class DownloadState {
    QUEUED,
    RUNNING,
    PAUSED,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED;

    companion object {
        fun fromRaw(raw: String?): DownloadState =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: QUEUED
    }
}

enum class DownloadPausedReason {
    NONE,
    USER,
    WAITING_WIFI,
    WAITING_NETWORK,
    RETRY_BACKOFF;

    companion object {
        fun fromRaw(raw: String?): DownloadPausedReason =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NONE
    }
}

data class ByteRangeSpec(
    val start: Long,
    val end: Long,
    val totalBytes: Long
)

sealed interface DownloadFailureDecision {
    data class Retryable(val delayMs: Long, val reason: String) : DownloadFailureDecision
    data object RestartFromZero : DownloadFailureDecision
    data class TerminalFailure(val errorCode: String, val message: String) : DownloadFailureDecision
}

/**
 * Pure JVM-testable business rules for KPK 2G/3G resilient PDF downloads:
 * - State machine validation
 * - Wi-Fi-only > 5 MB gating
 * - StatFs free storage guard
 * - Retry-After + exponential backoff with jitter
 * - HTTP status & Content-Range / ETag / SHA-256 helpers
 */
object PdfDownloadEngine {

    const val WIFI_ONLY_THRESHOLD_BYTES: Long = 5L * 1024L * 1024L // 5 MB
    const val CHUNK_CHECKPOINT_BYTES: Long = 1024L * 1024L // 1 MB
    const val FREE_STORAGE_MARGIN_BYTES: Long = 10L * 1024L * 1024L // 10 MB safety margin
    const val BASE_BACKOFF_MS: Long = 15_000L
    const val MAX_BACKOFF_MS: Long = 300_000L
    const val MAX_RETRY_ATTEMPTS: Int = 5

    private val SHA256_REGEX = Regex("^[0-9a-f]{64}$")

    fun isValidSha256Hex(candidate: String?): Boolean {
        val normalized = candidate?.trim()?.lowercase(Locale.US) ?: return false
        return SHA256_REGEX.matches(normalized)
    }

    fun normalizeStrongEtag(rawHeader: String?): String? {
        val trimmed = rawHeader?.trim() ?: return null
        if (trimmed.isEmpty() || trimmed.startsWith("W/", ignoreCase = true)) return null
        return trimmed
    }

    fun extractSha256FromEtagIfHex(etagHeader: String?): String? {
        val strong = normalizeStrongEtag(etagHeader) ?: return null
        val unquoted = strong.removePrefix("\"").removeSuffix("\"").trim().lowercase(Locale.US)
        return unquoted.takeIf { SHA256_REGEX.matches(it) }
    }

    fun parseContentRange(header: String?): ByteRangeSpec? {
        val raw = header?.trim() ?: return null
        if (!raw.startsWith("bytes ", ignoreCase = true)) return null
        val spec = raw.substring(6).trim()
        val slashIdx = spec.indexOf('/')
        if (slashIdx <= 0) return null
        val rangePart = spec.substring(0, slashIdx).trim()
        val totalStr = spec.substring(slashIdx + 1).trim()
        val total = if (totalStr == "*") -1L else totalStr.toLongOrNull() ?: return null
        if (rangePart == "*") {
            return ByteRangeSpec(start = -1L, end = -1L, totalBytes = total)
        }
        val dashIdx = rangePart.indexOf('-')
        if (dashIdx <= 0) return null
        val start = rangePart.substring(0, dashIdx).toLongOrNull() ?: return null
        val end = rangePart.substring(dashIdx + 1).toLongOrNull() ?: return null
        if (start < 0L || end < start) return null
        return ByteRangeSpec(start = start, end = end, totalBytes = total)
    }

    fun requiresUnmeteredWifi(
        totalBytes: Long,
        allowMobileDataForItem: Boolean,
        globalAllowMobileData: Boolean
    ): Boolean {
        if (allowMobileDataForItem || globalAllowMobileData) return false
        return totalBytes > WIFI_ONLY_THRESHOLD_BYTES
    }

    fun hasSufficientStorage(
        availableBytes: Long,
        expectedTotalBytes: Long,
        alreadyDownloadedBytes: Long = 0L
    ): Boolean {
        val remaining = if (expectedTotalBytes > 0L) {
            (expectedTotalBytes - alreadyDownloadedBytes).coerceAtLeast(0L)
        } else {
            0L
        }
        val required = remaining + FREE_STORAGE_MARGIN_BYTES
        return availableBytes >= required
    }

    fun parseRetryAfterMs(headerValue: String?, nowEpochMs: Long = System.currentTimeMillis()): Long? {
        val raw = headerValue?.trim() ?: return null
        if (raw.isEmpty()) return null
        raw.toLongOrNull()?.let { seconds ->
            return (seconds * 1000L).coerceIn(1_000L, 3_600_000L)
        }
        return runCatching {
            val zdt = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
            val deltaMs = zdt.toInstant().toEpochMilli() - nowEpochMs
            deltaMs.coerceIn(1_000L, 3_600_000L)
        }.getOrNull()
    }

    fun computeBackoffDelayMs(
        attempt: Int,
        retryAfterHeader: String? = null,
        jitterRatio: Double = 0.25,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Long {
        val retryAfterMs = parseRetryAfterMs(retryAfterHeader, nowEpochMs)
        val clampedAttempt = attempt.coerceIn(0, 10)
        val exp = min(
            MAX_BACKOFF_MS.toDouble(),
            BASE_BACKOFF_MS.toDouble() * 2.0.pow(clampedAttempt.toDouble())
        )
        val clampedJitter = jitterRatio.coerceIn(0.0, 1.0)
        val jittered = (exp * (1.0 + clampedJitter * 0.30)).toLong().coerceAtLeast(1_000L)
        return if (retryAfterMs != null) maxOf(retryAfterMs, BASE_BACKOFF_MS.coerceAtMost(retryAfterMs)) else jittered
    }

    fun classifyHttpFailure(
        statusCode: Int?,
        errorCode: String?,
        message: String?,
        retryAfterHeader: String?,
        attempt: Int,
        jitterRatio: Double = 0.25,
        nowEpochMs: Long = System.currentTimeMillis()
    ): DownloadFailureDecision {
        if (statusCode == 416) {
            return DownloadFailureDecision.RestartFromZero
        }
        if (statusCode == 403 || statusCode == 404) {
            val defaultMsg = if (statusCode == 404) {
                "PDF not found on server (404)."
            } else {
                "Access denied for this PDF (403)."
            }
            return DownloadFailureDecision.TerminalFailure(
                errorCode = errorCode ?: "HTTP_$statusCode",
                message = message?.ifBlank { null } ?: defaultMsg
            )
        }
        if (errorCode in setOf("INSUFFICIENT_STORAGE", "CHECKSUM_MISMATCH", "INVALID_PDF_STREAM", "SIZE_MISMATCH", "EMPTY_PDF")) {
            return DownloadFailureDecision.TerminalFailure(
                errorCode = errorCode ?: "INTEGRITY_ERROR",
                message = message?.ifBlank { null } ?: "Downloaded file failed integrity verification."
            )
        }
        val isRetryableStatus = statusCode == null || statusCode == 408 || statusCode == 429 || statusCode in 500..599
        if (!isRetryableStatus) {
            return DownloadFailureDecision.TerminalFailure(
                errorCode = errorCode ?: "HTTP_$statusCode",
                message = message?.ifBlank { null } ?: "Download failed with HTTP $statusCode."
            )
        }
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            return DownloadFailureDecision.TerminalFailure(
                errorCode = errorCode ?: "MAX_RETRIES_EXCEEDED",
                message = message?.ifBlank { null } ?: "Network remained unstable after $MAX_RETRY_ATTEMPTS attempts. Tap Retry when signal improves."
            )
        }
        val delayMs = computeBackoffDelayMs(
            attempt = attempt,
            retryAfterHeader = retryAfterHeader,
            jitterRatio = jitterRatio,
            nowEpochMs = nowEpochMs
        )
        val reason = when (statusCode) {
            429 -> "Server daily bandwidth limit reached; retrying automatically."
            408 -> "Connection timed out; retrying automatically."
            in 500..599 -> "Server temporarily busy ($statusCode); retrying automatically."
            else -> "Network interrupted; resuming from last saved byte."
        }
        return DownloadFailureDecision.Retryable(delayMs = delayMs, reason = reason)
    }

    fun canTransition(from: DownloadState, to: DownloadState): Boolean {
        if (from == to) return true
        return when (from) {
            DownloadState.QUEUED -> to in setOf(DownloadState.RUNNING, DownloadState.PAUSED, DownloadState.FAILED, DownloadState.CANCELLED)
            DownloadState.RUNNING -> to in setOf(DownloadState.QUEUED, DownloadState.PAUSED, DownloadState.VERIFYING, DownloadState.FAILED, DownloadState.CANCELLED)
            DownloadState.PAUSED -> to in setOf(DownloadState.QUEUED, DownloadState.RUNNING, DownloadState.FAILED, DownloadState.CANCELLED)
            DownloadState.VERIFYING -> to in setOf(DownloadState.COMPLETED, DownloadState.FAILED, DownloadState.CANCELLED)
            DownloadState.FAILED -> to in setOf(DownloadState.QUEUED, DownloadState.CANCELLED)
            DownloadState.COMPLETED -> to in setOf(DownloadState.QUEUED, DownloadState.CANCELLED)
            DownloadState.CANCELLED -> to == DownloadState.QUEUED
        }
    }

    fun computeProgressPercent(bytesDownloaded: Long, totalBytes: Long): Int {
        if (totalBytes <= 0L) return -1
        return ((bytesDownloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
    }

    fun formatMb(bytes: Long): String {
        val mb = bytes.coerceAtLeast(0L) / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun formatBytesProgress(bytesDownloaded: Long, totalBytes: Long): String {
        val dlStr = formatMb(bytesDownloaded)
        return if (totalBytes > 0L) "$dlStr / ${formatMb(totalBytes)}" else dlStr
    }

    fun formatSpeedBps(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return ""
        return if (bytesPerSec >= 1024L * 1024L) {
            String.format(Locale.US, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
        } else {
            "${(bytesPerSec / 1024L).coerceAtLeast(1L)} KB/s"
        }
    }
}
