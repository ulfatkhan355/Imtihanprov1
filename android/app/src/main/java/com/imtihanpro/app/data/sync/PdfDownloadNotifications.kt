package com.imtihanpro.app.data.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.imtihanpro.app.MainActivity
import com.imtihanpro.app.R
import com.imtihanpro.app.domain.engine.DownloadPausedReason
import com.imtihanpro.app.domain.engine.PdfDownloadEngine

object PdfDownloadNotifications {

    const val CHANNEL_ID = "downloads"
    const val GROUP_KEY = "com.imtihanpro.app.DOWNLOADS_GROUP"
    const val ONGOING_NOTIFICATION_ID = 7001
    private const val SUMMARY_NOTIFICATION_ID = 7000

    const val ACTION_PAUSE = "com.imtihanpro.app.DOWNLOAD_PAUSE"
    const val ACTION_RESUME = "com.imtihanpro.app.DOWNLOAD_RESUME"
    const val ACTION_CANCEL = "com.imtihanpro.app.DOWNLOAD_CANCEL"
    const val ACTION_RETRY = "com.imtihanpro.app.DOWNLOAD_RETRY"
    const val ACTION_ALLOW_MOBILE_DATA = "com.imtihanpro.app.DOWNLOAD_ALLOW_MOBILE_DATA"

    const val EXTRA_PDF_ID = "extra_pdf_id"
    const val EXTRA_OPEN_PDF_ID = "extra_open_pdf_id"
    const val EXTRA_OPEN_PAPER_ID = "extra_open_paper_id"
    const val EXTRA_OPEN_PDF_TITLE = "extra_open_pdf_title"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.dl_notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.dl_notif_channel_desc)
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun itemNotificationId(pdfId: String): Int = 7100 + (pdfId.hashCode() and 0x0FFF)

    fun buildOngoingNotification(
        context: Context,
        pdfId: String,
        title: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        isVerifying: Boolean
    ): Notification {
        ensureChannel(context)
        val pct = PdfDownloadEngine.computeProgressPercent(bytesDownloaded, totalBytes)
        val indeterminate = isVerifying || pct < 0
        val progressText = if (isVerifying) {
            context.getString(R.string.dl_state_verifying)
        } else {
            val sizePart = PdfDownloadEngine.formatBytesProgress(bytesDownloaded, totalBytes)
            val speedPart = PdfDownloadEngine.formatSpeedBps(speedBytesPerSec)
            val pctPart = if (pct >= 0) "$pct%" else ""
            listOf(pctPart, sizePart, speedPart).filter { it.isNotBlank() }.joinToString(" · ")
        }

        val pausePi = actionPendingIntent(context, ACTION_PAUSE, pdfId, 1)
        val cancelPi = actionPendingIntent(context, ACTION_CANCEL, pdfId, 2)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(progressText)
            .setSubText(context.getString(R.string.dl_state_downloading))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setGroup(GROUP_KEY)
            .setProgress(100, pct.coerceIn(0, 100), indeterminate)
            .setContentIntent(openAppDownloadsPendingIntent(context, pdfId))
            .addAction(0, context.getString(R.string.dl_action_pause), pausePi)
            .addAction(0, context.getString(R.string.dl_action_cancel), cancelPi)
            .build()
    }

    fun buildForegroundInfo(
        context: Context,
        pdfId: String,
        title: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        isVerifying: Boolean
    ): ForegroundInfo {
        val notif = buildOngoingNotification(
            context = context,
            pdfId = pdfId,
            title = title,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            isVerifying = isVerifying
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                ONGOING_NOTIFICATION_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(ONGOING_NOTIFICATION_ID, notif)
        }
    }

    fun updateOngoingProgressNotification(
        context: Context,
        pdfId: String,
        title: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        isVerifying: Boolean
    ) {
        if (!canPostNotifications(context)) return
        val notif = buildOngoingNotification(
            context = context,
            pdfId = pdfId,
            title = title,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            isVerifying = isVerifying
        )
        runCatching {
            NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, notif)
        }
    }

    fun showPausedNotification(
        context: Context,
        pdfId: String,
        title: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        reason: DownloadPausedReason
    ) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val statusText = when (reason) {
            DownloadPausedReason.WAITING_WIFI -> context.getString(R.string.dl_state_waiting_wifi)
            DownloadPausedReason.WAITING_NETWORK -> context.getString(R.string.dl_state_waiting_network)
            DownloadPausedReason.RETRY_BACKOFF -> context.getString(R.string.dl_state_retry_backoff)
            else -> context.getString(R.string.dl_state_paused)
        }
        val sizePart = PdfDownloadEngine.formatBytesProgress(bytesDownloaded, totalBytes)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("$statusText · $sizePart")
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(false)
            .setGroup(GROUP_KEY)
            .setContentIntent(openAppDownloadsPendingIntent(context, pdfId))

        if (reason == DownloadPausedReason.WAITING_WIFI) {
            builder.addAction(
                0,
                context.getString(R.string.dl_action_mobile_data),
                actionPendingIntent(context, ACTION_ALLOW_MOBILE_DATA, pdfId, 3)
            )
        } else {
            builder.addAction(
                0,
                context.getString(R.string.dl_action_resume),
                actionPendingIntent(context, ACTION_RESUME, pdfId, 4)
            )
        }
        builder.addAction(
            0,
            context.getString(R.string.dl_action_cancel),
            actionPendingIntent(context, ACTION_CANCEL, pdfId, 5)
        )

        runCatching {
            val nm = NotificationManagerCompat.from(context)
            nm.notify(itemNotificationId(pdfId), builder.build())
            postSummaryIfNeeded(context, nm)
        }
    }

    fun showCompletedNotification(
        context: Context,
        pdfId: String,
        paperId: String,
        title: String,
        totalBytes: Long
    ) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val sizePart = PdfDownloadEngine.formatMb(totalBytes)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("${context.getString(R.string.dl_state_completed)} ($sizePart) · ${context.getString(R.string.dl_completed_notif_text)}")
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setContentIntent(openPdfReaderPendingIntent(context, pdfId, paperId, title))
            .build()

        runCatching {
            val nm = NotificationManagerCompat.from(context)
            nm.notify(itemNotificationId(pdfId), notif)
            postSummaryIfNeeded(context, nm)
        }
    }

    fun showFailedNotification(
        context: Context,
        pdfId: String,
        title: String,
        errorMessage: String
    ) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("${context.getString(R.string.dl_state_failed)}: $title")
            .setContentText(errorMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setContentIntent(openAppDownloadsPendingIntent(context, pdfId))
            .addAction(
                0,
                context.getString(R.string.dl_action_retry),
                actionPendingIntent(context, ACTION_RETRY, pdfId, 6)
            )
            .addAction(
                0,
                context.getString(R.string.dl_action_cancel),
                actionPendingIntent(context, ACTION_CANCEL, pdfId, 7)
            )
            .build()

        runCatching {
            val nm = NotificationManagerCompat.from(context)
            nm.notify(itemNotificationId(pdfId), notif)
            postSummaryIfNeeded(context, nm)
        }
    }

    fun cancelNotification(context: Context, pdfId: String) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(itemNotificationId(pdfId))
        }
    }

    private fun postSummaryIfNeeded(context: Context, nm: NotificationManagerCompat) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.dl_notif_group_summary))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setSilent(true)
            .setAutoCancel(true)
            .build()
        nm.notify(SUMMARY_NOTIFICATION_ID, summary)
    }

    private fun actionPendingIntent(
        context: Context,
        action: String,
        pdfId: String,
        salt: Int
    ): PendingIntent {
        val intent = Intent(context, PdfDownloadActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_PDF_ID, pdfId)
        }
        val reqCode = (pdfId.hashCode() * 31 + salt) and 0x7FFFFFFF
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openPdfReaderPendingIntent(
        context: Context,
        pdfId: String,
        paperId: String,
        title: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PDF_ID, pdfId)
            putExtra(EXTRA_OPEN_PAPER_ID, paperId)
            putExtra(EXTRA_OPEN_PDF_TITLE, title)
        }
        val reqCode = ("open_$pdfId".hashCode()) and 0x7FFFFFFF
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppDownloadsPendingIntent(context: Context, pdfId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PDF_ID, pdfId)
        }
        val reqCode = ("dl_$pdfId".hashCode()) and 0x7FFFFFFF
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
