package com.imtihanpro.app.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.imtihanpro.app.R
import com.imtihanpro.app.data.local.datastore.UserPreferencesDataStore
import com.imtihanpro.app.data.local.room.DownloadTaskEntity
import com.imtihanpro.app.data.local.room.DownloadedPdfEntity
import com.imtihanpro.app.data.local.room.ImtihanDatabase
import com.imtihanpro.app.data.local.room.ImtihanDatabaseProvider
import com.imtihanpro.app.data.remote.ApiConfig
import com.imtihanpro.app.data.remote.ApiResult
import com.imtihanpro.app.data.remote.ImtihanApiClient
import com.imtihanpro.app.data.repository.OfflineFirstCurriculumRepository
import com.imtihanpro.app.domain.engine.DownloadFailureDecision
import com.imtihanpro.app.domain.engine.DownloadPausedReason
import com.imtihanpro.app.domain.engine.DownloadState
import com.imtihanpro.app.domain.engine.PdfDownloadEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Reliable background synchronization worker backed by AndroidX WorkManager.
 * Flushes pending offline progress, quiz submissions, flashcards, and planner tasks
 * once network connectivity is restored, with exponential backoff on transient failures.
 */
class ImtihanSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = ImtihanDatabaseProvider.getInstance(applicationContext)
        val preferences = UserPreferencesDataStore(applicationContext)
        val repository = OfflineFirstCurriculumRepository(
            context = applicationContext,
            database = database,
            preferencesDataStore = preferences,
            apiClient = ImtihanApiClient.createWithCache(applicationContext.cacheDir)
        )

        val synced = runCatching { repository.syncWithCloud().isSuccess }.getOrDefault(false)
        return if (synced) {
            Result.success()
        } else if (runAttemptCount < 4) {
            Result.retry()
        } else {
            Result.failure()
        }
    }
}

/**
 * Serialized Foreground Service PDF Download Worker backed by WorkManager + Room `download_tasks`.
 * Guarantees:
 * - Only 1 active PDF download runs at a time; queued tasks drain in FIFO order.
 * - Survives app kills, process death, and device reboots with `.tmp` Range + If-Range resume.
 * - Honors > 5 MB Wi-Fi-only gating unless overridden globally or per item.
 * - Checks free storage (`StatFs`), `%PDF-` magic header, exact bytes, and SHA-256 before atomic move.
 */
class PdfDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = workerMutex.withLock {
        val db = ImtihanDatabaseProvider.getInstance(applicationContext)
        val taskDao = db.downloadTaskDao()
        val offlineDao = db.offlineContentDao()
        val preferencesStore = UserPreferencesDataStore(applicationContext)
        val apiClient = ImtihanApiClient.createWithCache(applicationContext.cacheDir)

        PdfDownloadNotifications.ensureChannel(applicationContext)

        // Recover any tasks left in RUNNING / VERIFYING across a prior process kill
        val orphaned = taskDao.getOrphanedRunningOnce()
        val nowStart = System.currentTimeMillis()
        for (orphan in orphaned) {
            taskDao.updateState(
                pdfId = orphan.pdfId,
                state = DownloadState.QUEUED.name,
                pausedReason = DownloadPausedReason.NONE.name,
                errorCode = null,
                errorMessage = null,
                updatedAt = nowStart
            )
        }

        while (!isStopped) {
            val task = taskDao.getNextRunnableOnce() ?: break
            val prefs = preferencesStore.preferencesFlow.first()

            if (prefs.offlineOnlyMode) {
                taskDao.updateState(
                    pdfId = task.pdfId,
                    state = DownloadState.PAUSED.name,
                    pausedReason = DownloadPausedReason.WAITING_NETWORK.name,
                    errorCode = "OFFLINE_MODE",
                    errorMessage = applicationContext.getString(R.string.dl_state_waiting_network),
                    updatedAt = System.currentTimeMillis()
                )
                break
            }

            SyncScheduler.clearInterrupted(task.pdfId)
            val targetDir = File(applicationContext.filesDir, "imtihan_pdfs").apply { mkdirs() }
            val safeName = task.pdfId.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val targetFile = File(targetDir, "$safeName.pdf")
            val tmpFile = File(targetDir, "${targetFile.name}.tmp")

            val existingBytes = if (tmpFile.exists() && tmpFile.length() > 0L) tmpFile.length() else 0L
            var knownTotalBytes = task.totalBytes
            var knownEtag = task.etag
            var knownSha256 = task.expectedSha256

            taskDao.upsert(
                task.copy(
                    state = DownloadState.RUNNING.name,
                    pausedReason = DownloadPausedReason.NONE.name,
                    bytesDownloaded = existingBytes,
                    errorCode = null,
                    errorMessage = null,
                    tmpPath = tmpFile.absolutePath,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )

            setForeground(
                PdfDownloadNotifications.buildForegroundInfo(
                    context = applicationContext,
                    pdfId = task.pdfId,
                    title = task.title,
                    bytesDownloaded = existingBytes,
                    totalBytes = knownTotalBytes,
                    speedBytesPerSec = 0L,
                    isVerifying = false
                )
            )

            // Probe HEAD metadata if total size or ETag is missing so > 5 MB Wi-Fi check & If-Range are accurate
            if (knownTotalBytes <= 0L || knownEtag.isNullOrBlank()) {
                when (val probe = apiClient.probePdfHead(prefs.configuredApiBaseUrl, task.pdfId)) {
                    is ApiResult.Success -> {
                        if (probe.data.contentLength > 0L) knownTotalBytes = probe.data.contentLength
                        if (!probe.data.etag.isNullOrBlank()) knownEtag = probe.data.etag
                        if (!probe.data.sha256Hint.isNullOrBlank()) knownSha256 = probe.data.sha256Hint
                        taskDao.updateMetadata(
                            pdfId = task.pdfId,
                            totalBytes = knownTotalBytes,
                            etag = knownEtag,
                            expectedSha256 = knownSha256,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    is ApiResult.Failure -> {
                        // Proceed gracefully if HEAD fails due to proxy/mock; GET handles error classification
                    }
                }
            }

            // Enforce > 5 MB Wi-Fi-only policy unless explicitly overridden
            if (PdfDownloadEngine.requiresUnmeteredWifi(
                    totalBytes = knownTotalBytes,
                    allowMobileDataForItem = task.allowMobileData,
                    globalAllowMobileData = prefs.allowDownloadOverMobileData
                ) && SyncScheduler.isActiveNetworkMetered(applicationContext)
            ) {
                taskDao.updateState(
                    pdfId = task.pdfId,
                    state = DownloadState.PAUSED.name,
                    pausedReason = DownloadPausedReason.WAITING_WIFI.name,
                    errorCode = "WIFI_REQUIRED",
                    errorMessage = applicationContext.getString(R.string.dl_error_wifi_required),
                    updatedAt = System.currentTimeMillis()
                )
                PdfDownloadNotifications.showPausedNotification(
                    context = applicationContext,
                    pdfId = task.pdfId,
                    title = task.title,
                    bytesDownloaded = existingBytes,
                    totalBytes = knownTotalBytes,
                    reason = DownloadPausedReason.WAITING_WIFI
                )
                continue
            }

            var lastNotifMs = 0L
            var didResetRangeOnce = false

            while (true) {
                val outcome = apiClient.downloadPdfResumable(
                    baseUrl = prefs.configuredApiBaseUrl,
                    pdfId = task.pdfId,
                    targetFile = targetFile,
                    expectedSha256 = knownSha256,
                    expectedTotalBytes = knownTotalBytes,
                    knownEtag = knownEtag,
                    isCancelled = { isStopped || SyncScheduler.isInterrupted(task.pdfId) },
                    onVerifying = {
                        kotlinx.coroutines.runBlocking {
                            taskDao.updateState(
                                pdfId = task.pdfId,
                                state = DownloadState.VERIFYING.name,
                                pausedReason = DownloadPausedReason.NONE.name,
                                errorCode = null,
                                errorMessage = null,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        PdfDownloadNotifications.updateOngoingProgressNotification(
                            context = applicationContext,
                            pdfId = task.pdfId,
                            title = task.title,
                            bytesDownloaded = knownTotalBytes.coerceAtLeast(existingBytes),
                            totalBytes = knownTotalBytes,
                            speedBytesPerSec = 0L,
                            isVerifying = true
                        )
                    },
                    onProgress = { bytesRead, totalBytes, speedBps, etag ->
                        if (totalBytes > 0L) knownTotalBytes = totalBytes
                        if (!etag.isNullOrBlank()) knownEtag = etag
                        val now = System.currentTimeMillis()
                        if (now - lastNotifMs >= 450L || (totalBytes > 0L && bytesRead >= totalBytes)) {
                            lastNotifMs = now
                            kotlinx.coroutines.runBlocking {
                                taskDao.updateProgress(
                                    pdfId = task.pdfId,
                                    bytesDownloaded = bytesRead,
                                    totalBytes = totalBytes,
                                    speedBps = speedBps,
                                    etag = etag,
                                    updatedAt = now
                                )
                            }
                            PdfDownloadNotifications.updateOngoingProgressNotification(
                                context = applicationContext,
                                pdfId = task.pdfId,
                                title = task.title,
                                bytesDownloaded = bytesRead,
                                totalBytes = totalBytes,
                                speedBytesPerSec = speedBps,
                                isVerifying = false
                            )
                            val pct = PdfDownloadEngine.computeProgressPercent(bytesRead, totalBytes)
                            setProgressAsync(
                                workDataOf(
                                    KEY_PDF_ID to task.pdfId,
                                    KEY_BYTES_READ to bytesRead,
                                    KEY_TOTAL_BYTES to totalBytes,
                                    KEY_PROGRESS_PERCENT to pct
                                )
                            )
                        }
                    }
                )

                when (outcome) {
                    is com.imtihanpro.app.data.remote.PdfDownloadOutcome.Success -> {
                        val completedAt = System.currentTimeMillis()
                        val latest = taskDao.getByIdOnce(task.pdfId) ?: task
                        taskDao.upsert(
                            latest.copy(
                                state = DownloadState.COMPLETED.name,
                                pausedReason = DownloadPausedReason.NONE.name,
                                bytesDownloaded = outcome.totalBytes,
                                totalBytes = outcome.totalBytes,
                                speedBytesPerSec = 0L,
                                etag = outcome.etag ?: knownEtag,
                                expectedSha256 = outcome.verifiedSha256,
                                errorCode = null,
                                errorMessage = null,
                                localPath = outcome.file.absolutePath,
                                retryCount = 0,
                                nextRetryAtMs = 0L,
                                updatedAtEpochMs = completedAt
                            )
                        )
                        val sizeMb = maxOf(0.1, outcome.totalBytes / (1024.0 * 1024.0))
                        offlineDao.upsertDownloadedPdf(
                            DownloadedPdfEntity(
                                pdfId = task.pdfId,
                                title = task.title,
                                categoryLabel = task.categoryLabel,
                                localFilePath = outcome.file.absolutePath,
                                sizeMb = sizeMb,
                                downloadedAtEpochMs = completedAt
                            )
                        )
                        PdfDownloadNotifications.showCompletedNotification(
                            context = applicationContext,
                            pdfId = task.pdfId,
                            paperId = task.paperId,
                            title = task.title,
                            totalBytes = outcome.totalBytes
                        )
                        break
                    }

                    is com.imtihanpro.app.data.remote.PdfDownloadOutcome.RangeResetRequired -> {
                        if (tmpFile.exists()) tmpFile.delete()
                        knownEtag = null
                        val latest = taskDao.getByIdOnce(task.pdfId) ?: task
                        taskDao.upsert(
                            latest.copy(
                                bytesDownloaded = 0L,
                                etag = null,
                                updatedAtEpochMs = System.currentTimeMillis()
                            )
                        )
                        if (!didResetRangeOnce) {
                            didResetRangeOnce = true
                            continue
                        } else {
                            taskDao.updateState(
                                pdfId = task.pdfId,
                                state = DownloadState.FAILED.name,
                                pausedReason = DownloadPausedReason.NONE.name,
                                errorCode = "RANGE_RESET_LOOP",
                                errorMessage = "Server rejected byte offset twice. Tap Retry to start cleanly.",
                                updatedAt = System.currentTimeMillis()
                            )
                            break
                        }
                    }

                    is com.imtihanpro.app.data.remote.PdfDownloadOutcome.PausedOrCancelled -> {
                        val dbTask = taskDao.getByIdOnce(task.pdfId)
                        if (dbTask == null || dbTask.state == DownloadState.CANCELLED.name) {
                            if (tmpFile.exists()) tmpFile.delete()
                            taskDao.deleteById(task.pdfId)
                            PdfDownloadNotifications.cancelNotification(applicationContext, task.pdfId)
                        } else {
                            val currentBytes = if (tmpFile.exists()) tmpFile.length() else dbTask.bytesDownloaded
                            val reason = DownloadPausedReason.fromRaw(dbTask.pausedReason)
                                .takeIf { dbTask.state == DownloadState.PAUSED.name && it != DownloadPausedReason.NONE }
                                ?: if (isStopped) DownloadPausedReason.WAITING_NETWORK else DownloadPausedReason.USER
                            taskDao.upsert(
                                dbTask.copy(
                                    state = DownloadState.PAUSED.name,
                                    pausedReason = reason.name,
                                    bytesDownloaded = currentBytes,
                                    speedBytesPerSec = 0L,
                                    updatedAtEpochMs = System.currentTimeMillis()
                                )
                            )
                            PdfDownloadNotifications.showPausedNotification(
                                context = applicationContext,
                                pdfId = task.pdfId,
                                title = task.title,
                                bytesDownloaded = currentBytes,
                                totalBytes = knownTotalBytes,
                                reason = reason
                            )
                        }
                        SyncScheduler.clearInterrupted(task.pdfId)
                        break
                    }

                    is com.imtihanpro.app.data.remote.PdfDownloadOutcome.InsufficientStorage -> {
                        val msg = applicationContext.getString(R.string.dl_error_low_storage) +
                            " (${PdfDownloadEngine.formatMb(outcome.availableBytes)} free, need ${PdfDownloadEngine.formatMb(outcome.requiredBytes)})"
                        taskDao.updateState(
                            pdfId = task.pdfId,
                            state = DownloadState.FAILED.name,
                            pausedReason = DownloadPausedReason.NONE.name,
                            errorCode = "INSUFFICIENT_STORAGE",
                            errorMessage = msg,
                            updatedAt = System.currentTimeMillis()
                        )
                        PdfDownloadNotifications.showFailedNotification(applicationContext, task.pdfId, task.title, msg)
                        break
                    }

                    is com.imtihanpro.app.data.remote.PdfDownloadOutcome.CorruptFile -> {
                        val msg = if (outcome.code == "CHECKSUM_MISMATCH" || outcome.code == "INVALID_PDF_STREAM") {
                            applicationContext.getString(R.string.dl_error_corrupt_deleted)
                        } else {
                            outcome.message
                        }
                        val latest = taskDao.getByIdOnce(task.pdfId) ?: task
                        taskDao.upsert(
                            latest.copy(
                                state = DownloadState.FAILED.name,
                                pausedReason = DownloadPausedReason.NONE.name,
                                bytesDownloaded = 0L,
                                speedBytesPerSec = 0L,
                                errorCode = outcome.code,
                                errorMessage = msg,
                                updatedAtEpochMs = System.currentTimeMillis()
                            )
                        )
                        PdfDownloadNotifications.showFailedNotification(applicationContext, task.pdfId, task.title, msg)
                        break
                    }

                    is com.imtihanpro.app.data.remote.PdfDownloadOutcome.HttpFailure,
                    is com.imtihanpro.app.data.remote.PdfDownloadOutcome.NetworkFailure -> {
                        val statusCode = (outcome as? com.imtihanpro.app.data.remote.PdfDownloadOutcome.HttpFailure)?.statusCode
                        val errCode = (outcome as? com.imtihanpro.app.data.remote.PdfDownloadOutcome.HttpFailure)?.code
                            ?: "NETWORK_ERROR"
                        val rawMsg = when (outcome) {
                            is com.imtihanpro.app.data.remote.PdfDownloadOutcome.HttpFailure -> outcome.message
                            is com.imtihanpro.app.data.remote.PdfDownloadOutcome.NetworkFailure -> outcome.message
                            else -> "Network error"
                        }
                        val retryAfter = (outcome as? com.imtihanpro.app.data.remote.PdfDownloadOutcome.HttpFailure)?.retryAfterHeader
                        val currentTask = taskDao.getByIdOnce(task.pdfId) ?: task
                        val decision = PdfDownloadEngine.classifyHttpFailure(
                            statusCode = statusCode,
                            errorCode = errCode,
                            message = rawMsg,
                            retryAfterHeader = retryAfter,
                            attempt = currentTask.retryCount,
                            jitterRatio = Random.nextDouble(0.0, 1.0)
                        )
                        when (decision) {
                            is DownloadFailureDecision.RestartFromZero -> {
                                if (tmpFile.exists()) tmpFile.delete()
                                knownEtag = null
                                if (!didResetRangeOnce) {
                                    didResetRangeOnce = true
                                    continue
                                }
                            }
                            is DownloadFailureDecision.Retryable -> {
                                val nextRetryAt = System.currentTimeMillis() + decision.delayMs
                                val currentBytes = if (tmpFile.exists()) tmpFile.length() else currentTask.bytesDownloaded
                                taskDao.upsert(
                                    currentTask.copy(
                                        state = DownloadState.PAUSED.name,
                                        pausedReason = DownloadPausedReason.RETRY_BACKOFF.name,
                                        bytesDownloaded = currentBytes,
                                        speedBytesPerSec = 0L,
                                        retryCount = currentTask.retryCount + 1,
                                        nextRetryAtMs = nextRetryAt,
                                        errorCode = errCode,
                                        errorMessage = decision.reason,
                                        updatedAtEpochMs = System.currentTimeMillis()
                                    )
                                PdfDownloadNotifications.showPausedNotification(
                                    context = applicationContext,
                                    pdfId = task.pdfId,
                                    title = task.title,
                                    bytesDownloaded = currentBytes,
                                    totalBytes = knownTotalBytes,
                                    reason = DownloadPausedReason.RETRY_BACKOFF
                                )
                                SyncScheduler.scheduleDelayedQueueResume(applicationContext, decision.delayMs)
                                break
                            }
                            is DownloadFailureDecision.TerminalFailure -> {
                                taskDao.updateState(
                                    pdfId = task.pdfId,
                                    state = DownloadState.FAILED.name,
                                    pausedReason = DownloadPausedReason.NONE.name,
                                    errorCode = decision.errorCode,
                                    errorMessage = decision.message,
                                    updatedAt = System.currentTimeMillis()
                                )
                                PdfDownloadNotifications.showFailedNotification(
                                    applicationContext,
                                    task.pdfId,
                                    task.title,
                                    decision.message
                                )
                            }
                        }
                        break
                    }
                }
            }
        }

        return Result.success()
    }

    companion object {
        private val workerMutex = Mutex()
        const val KEY_PDF_ID = "pdf_id"
        const val KEY_EXPECTED_SHA256 = "expected_sha256"
        const val KEY_BYTES_READ = "bytes_read"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_LOCAL_PATH = "local_path"
        const val KEY_ERROR = "error"
    }
}

class PdfDownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pdfId = intent.getStringExtra(PdfDownloadNotifications.EXTRA_PDF_ID) ?: return
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = ImtihanDatabaseProvider.getInstance(appContext).downloadTaskDao()
                val existing = dao.getByIdOnce(pdfId)
                when (action) {
                    PdfDownloadNotifications.ACTION_PAUSE -> {
                        SyncScheduler.markInterrupted(pdfId)
                        if (existing != null) {
                            dao.updateState(
                                pdfId = pdfId,
                                state = DownloadState.PAUSED.name,
                                pausedReason = DownloadPausedReason.USER.name,
                                errorCode = null,
                                errorMessage = null,
                                updatedAt = System.currentTimeMillis()
                            )
                            PdfDownloadNotifications.showPausedNotification(
                                context = appContext,
                                pdfId = pdfId,
                                title = existing.title,
                                bytesDownloaded = existing.bytesDownloaded,
                                totalBytes = existing.totalBytes,
                                reason = DownloadPausedReason.USER
                            )
                        }
                    }

                    PdfDownloadNotifications.ACTION_RESUME,
                    PdfDownloadNotifications.ACTION_RETRY -> {
                        SyncScheduler.clearInterrupted(pdfId)
                        PdfDownloadNotifications.cancelNotification(appContext, pdfId)
                        if (existing != null) {
                            dao.upsert(
                                existing.copy(
                                    state = DownloadState.QUEUED.name,
                                    pausedReason = DownloadPausedReason.NONE.name,
                                    retryCount = 0,
                                    nextRetryAtMs = 0L,
                                    errorCode = null,
                                    errorMessage = null,
                                    updatedAtEpochMs = System.currentTimeMillis()
                                )
                            )
                            SyncScheduler.enqueueSerialDownloadQueue(appContext, replaceIfIdle = false)
                        }
                    }

                    PdfDownloadNotifications.ACTION_ALLOW_MOBILE_DATA -> {
                        SyncScheduler.clearInterrupted(pdfId)
                        PdfDownloadNotifications.cancelNotification(appContext, pdfId)
                        dao.allowMobileDataAndQueue(pdfId, true, System.currentTimeMillis())
                        SyncScheduler.enqueueSerialDownloadQueue(appContext, replaceIfIdle = false)
                    }

                    PdfDownloadNotifications.ACTION_CANCEL -> {
                        SyncScheduler.markInterrupted(pdfId)
                        PdfDownloadNotifications.cancelNotification(appContext, pdfId)
                        if (existing != null) {
                            dao.updateState(
                                pdfId = pdfId,
                                state = DownloadState.CANCELLED.name,
                                pausedReason = DownloadPausedReason.NONE.name,
                                errorCode = null,
                                errorMessage = null,
                                updatedAt = System.currentTimeMillis()
                            )
                            if (existing.tmpPath.isNotBlank()) {
                                File(existing.tmpPath).delete()
                            }
                            dao.deleteById(pdfId)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class PdfDownloadBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                SyncScheduler.recoverAndResumeQueueOnStartup(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object SyncScheduler {
    private const val PERIODIC_WORK_NAME = "imtihan_periodic_cloud_sync"
    private const val IMMEDIATE_WORK_NAME = "imtihan_immediate_cloud_sync"
    private const val DOWNLOAD_QUEUE_WORK_NAME = "imtihan_pdf_serial_queue"
    private const val DOWNLOAD_RETRY_WORK_NAME = "imtihan_pdf_serial_retry"

    private val interruptedPdfIds = ConcurrentHashMap<String, Boolean>()
    private val connectivityRegistered = AtomicBoolean(false)

    private val connectedConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun markInterrupted(pdfId: String) {
        interruptedPdfIds[pdfId] = true
    }

    fun clearInterrupted(pdfId: String) {
        interruptedPdfIds.remove(pdfId)
    }

    fun isInterrupted(pdfId: String): Boolean = interruptedPdfIds[pdfId] == true

    fun isActiveNetworkMetered(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    fun registerConnectivityMonitor(context: Context) {
        if (!connectivityRegistered.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = cm.getNetworkCapabilities(network)
                val isUnmetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
                handleNetworkTransition(appContext, isUnmetered)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val isUnmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                handleNetworkTransition(appContext, isUnmetered)
            }
        }
        runCatching {
            cm.registerDefaultNetworkCallback(callback)
        }.onFailure {
            connectivityRegistered.set(false)
        }
    }

    private fun handleNetworkTransition(appContext: Context, isUnmeteredWifi: Boolean) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val db = ImtihanDatabaseProvider.getInstance(appContext)
            val dao = db.downloadTaskDao()
            val prefs = UserPreferencesDataStore(appContext).preferencesFlow.first()
            if (prefs.offlineOnlyMode) return@launch

            if (!isUnmeteredWifi && !prefs.allowDownloadOverMobileData) {
                // If an active > 5 MB download without mobile-data override is running and switched to metered, pause cleanly
                val running = dao.getNextRunnableOnce()
                if (running != null &&
                    running.state == DownloadState.RUNNING.name &&
                    PdfDownloadEngine.requiresUnmeteredWifi(running.totalBytes, running.allowMobileData, false)
                ) {
                    markInterrupted(running.pdfId)
                    dao.updateState(
                        pdfId = running.pdfId,
                        state = DownloadState.PAUSED.name,
                        pausedReason = DownloadPausedReason.WAITING_WIFI.name,
                        errorCode = "WIFI_REQUIRED",
                        errorMessage = appContext.getString(R.string.dl_error_wifi_required),
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }

            val autoPaused = dao.getAutoResumablePausedOnce()
            var enqueuedAny = false
            val now = System.currentTimeMillis()
            for (task in autoPaused) {
                val reason = DownloadPausedReason.fromRaw(task.pausedReason)
                val canResumeNow = when (reason) {
                    DownloadPausedReason.WAITING_WIFI -> {
                        isUnmeteredWifi || task.allowMobileData || prefs.allowDownloadOverMobileData
                    }
                    DownloadPausedReason.WAITING_NETWORK,
                    DownloadPausedReason.RETRY_BACKOFF -> {
                        val wifiOk = !PdfDownloadEngine.requiresUnmeteredWifi(
                            task.totalBytes,
                            task.allowMobileData,
                            prefs.allowDownloadOverMobileData
                        ) || isUnmeteredWifi
                        wifiOk && (task.nextRetryAtMs <= now || isUnmeteredWifi)
                    }
                    else -> false
                }
                if (canResumeNow) {
                    clearInterrupted(task.pdfId)
                    PdfDownloadNotifications.cancelNotification(appContext, task.pdfId)
                    dao.updateState(
                        pdfId = task.pdfId,
                        state = DownloadState.QUEUED.name,
                        pausedReason = DownloadPausedReason.NONE.name,
                        errorCode = null,
                        errorMessage = null,
                        updatedAt = now
                    )
                    enqueuedAny = true
                }
            }

            if (enqueuedAny || dao.getQueuedOnce().isNotEmpty()) {
                enqueueSerialDownloadQueue(appContext, replaceIfIdle = false)
            }
        }
    }

    suspend fun recoverAndResumeQueueOnStartup(context: Context) {
        val db = ImtihanDatabaseProvider.getInstance(context.applicationContext)
        val dao = db.downloadTaskDao()
        val now = System.currentTimeMillis()
        for (orphan in dao.getOrphanedRunningOnce()) {
            dao.updateState(
                pdfId = orphan.pdfId,
                state = DownloadState.QUEUED.name,
                pausedReason = DownloadPausedReason.NONE.name,
                errorCode = null,
                errorMessage = null,
                updatedAt = now
            )
        }
        val prefs = UserPreferencesDataStore(context.applicationContext).preferencesFlow.first()
        if (!prefs.offlineOnlyMode && (dao.getNextRunnableOnce() != null || dao.getAutoResumablePausedOnce().isNotEmpty())) {
            handleNetworkTransition(context.applicationContext, !isActiveNetworkMetered(context.applicationContext))
        }
    }

    fun schedulePeriodicSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<ImtihanSyncWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        runCatching {
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    fun enqueueImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<ImtihanSyncWorker>()
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        runCatching {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    fun enqueueSerialDownloadQueue(context: Context, replaceIfIdle: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<PdfDownloadWorker>()
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        runCatching {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                DOWNLOAD_QUEUE_WORK_NAME,
                if (replaceIfIdle) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    fun scheduleDelayedQueueResume(context: Context, delayMs: Long) {
        val request = OneTimeWorkRequestBuilder<PdfDownloadWorker>()
            .setConstraints(connectedConstraints)
            .setInitialDelay(delayMs.coerceAtLeast(1_000L), TimeUnit.MILLISECONDS)
            .build()

        runCatching {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                DOWNLOAD_RETRY_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun enqueuePdfDownload(context: Context, pdfId: String, expectedSha256: String? = null) {
        enqueueSerialDownloadQueue(context, replaceIfIdle = false)
    }

    fun cancelPdfDownload(context: Context, pdfId: String) {
        markInterrupted(pdfId)
    }
}
