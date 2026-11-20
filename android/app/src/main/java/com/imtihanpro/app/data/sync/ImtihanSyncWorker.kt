package com.imtihanpro.app.data.sync

import android.content.Context
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
import com.imtihanpro.app.data.local.datastore.UserPreferencesDataStore
import com.imtihanpro.app.data.local.room.ImtihanDatabase
import com.imtihanpro.app.data.remote.ApiResult
import com.imtihanpro.app.data.remote.ImtihanApiClient
import com.imtihanpro.app.data.repository.OfflineFirstCurriculumRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

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
        val database = ImtihanDatabase.getInstance(applicationContext)
        val preferences = UserPreferencesDataStore(applicationContext)
        val repository = OfflineFirstCurriculumRepository(
            context = applicationContext,
            database = database,
            preferences = preferences,
            apiClient = ImtihanApiClient()
        )

        val synced = runCatching { repository.syncWithCloud() }.getOrDefault(false)
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
 * Resumable background PDF Download Worker backed by WorkManager + HTTP Range + SHA-256 verification.
 * Emits real byte-transfer progress (`bytesRead`, `totalBytes`, `progressPercent`) without fake percentages.
 */
class PdfDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pdfId = inputData.getString(KEY_PDF_ID) ?: return Result.failure()
        val expectedSha = inputData.getString(KEY_EXPECTED_SHA256)
        val prefs = UserPreferencesDataStore(applicationContext).preferencesFlow.first()
        val apiClient = ImtihanApiClient.createWithCache(applicationContext.cacheDir)
        val targetDir = File(applicationContext.filesDir, "imtihan_pdfs")
        val safeName = pdfId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetFile = File(targetDir, "$safeName.pdf")

        return when (
            val result = apiClient.downloadPdfToFile(
                baseUrl = prefs.configuredApiBaseUrl,
                pdfId = pdfId,
                targetFile = targetFile,
                expectedSha256 = expectedSha,
                onProgress = { bytesRead, totalBytes ->
                    val pct = if (totalBytes > 0L) ((bytesRead * 100L) / totalBytes).toInt().coerceIn(0, 100) else -1
                    setProgressAsync(
                        workDataOf(
                            KEY_PDF_ID to pdfId,
                            KEY_BYTES_READ to bytesRead,
                            KEY_TOTAL_BYTES to totalBytes,
                            KEY_PROGRESS_PERCENT to pct
                        )
                    )
                }
            )
        ) {
            is ApiResult.Success -> Result.success(
                workDataOf(
                    KEY_PDF_ID to pdfId,
                    KEY_LOCAL_PATH to result.data.absolutePath,
                    KEY_PROGRESS_PERCENT to 100
                )
            )
            is ApiResult.Failure -> {
                if (result.statusCode == null && runAttemptCount < 3) Result.retry()
                else Result.failure(workDataOf(KEY_ERROR to result.message))
            }
        }
    }

    companion object {
        const val KEY_PDF_ID = "pdf_id"
        const val KEY_EXPECTED_SHA256 = "expected_sha256"
        const val KEY_BYTES_READ = "bytes_read"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_LOCAL_PATH = "local_path"
        const val KEY_ERROR = "error"
    }
}

object SyncScheduler {
    private const val PERIODIC_WORK_NAME = "imtihan_periodic_cloud_sync"
    private const val IMMEDIATE_WORK_NAME = "imtihan_immediate_cloud_sync"

    private val connectedConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

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

    fun enqueuePdfDownload(context: Context, pdfId: String, expectedSha256: String? = null) {
        val request = OneTimeWorkRequestBuilder<PdfDownloadWorker>()
            .setConstraints(connectedConstraints)
            .setInputData(
                workDataOf(
                    PdfDownloadWorker.KEY_PDF_ID to pdfId,
                    PdfDownloadWorker.KEY_EXPECTED_SHA256 to expectedSha256
                )
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        runCatching {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "imtihan_pdf_dl_$pdfId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    fun cancelPdfDownload(context: Context, pdfId: String) {
        runCatching {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork("imtihan_pdf_dl_$pdfId")
        }
    }
}
