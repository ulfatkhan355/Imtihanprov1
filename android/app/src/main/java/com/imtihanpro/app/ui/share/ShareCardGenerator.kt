package com.imtihanpro.app.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Generates crisp 1080x1350 Neo-Silver Glass PNG scorecards & streak posters natively on-device
 * and shares them via Android FileProvider (WhatsApp / IG Story / Telegram compatible).
 * Zero network calls, zero extra permissions, zero APK image bloat.
 */
object ShareCardGenerator {

    fun shareQuizScorecard(
        context: Context,
        studentName: String,
        boardLabel: String,
        classLevel: Int,
        subjectName: String,
        scorePercent: Int,
        correctCount: Int,
        totalQuestions: Int,
        streakDays: Int,
        xpTotal: Int,
    ) {
        val bitmap = renderCardBitmap(
            headline = "$scorePercent%",
            eyebrow = "IMTIHAN PRO · BISE $boardLabel · CLASS $classLevel",
            title = if (scorePercent >= 80) "Board-Ready Mastery" else if (scorePercent >= 60) "Strong Exam Sprint" else "Rising KPK Scholar",
            subtitle = "$subjectName · $correctCount / $totalQuestions Correct",
            statLeftLabel = "STREAK",
            statLeftValue = "$streakDays Days",
            statRightLabel = "SCHOLAR XP",
            statRightValue = "$xpTotal XP",
            footerStudent = studentName.ifBlank { "KPK Topper" },
            accentStart = if (scorePercent >= 70) 0xFF38BDF8.toInt() else 0xFF3B82F6.toInt(),
            accentEnd = if (scorePercent >= 70) 0xFF10B981.toInt() else 0xFF6366F1.toInt(),
        )
        launchShareIntent(
            context = context,
            bitmap = bitmap,
            captionText = "Just scored $scorePercent% ($correctCount/$totalQuestions) in $subjectName on Imtihan Pro! Preparing for BISE $boardLabel Class $classLevel."
        )
    }

    fun shareStreakPoster(
        context: Context,
        studentName: String,
        boardLabel: String,
        classLevel: Int,
        streakDays: Int,
        xpTotal: Int,
        chaptersCompleted: Int,
        readinessPercent: Int?,
    ) {
        val headlineText = "${streakDays.coerceAtLeast(1)}d"
        val readinessLine = readinessPercent?.let { "Readiness $it% · " } ?: ""
        val bitmap = renderCardBitmap(
            headline = headlineText,
            eyebrow = "IMTIHAN PRO · BISE $boardLabel · CLASS $classLevel",
            title = "Active Study Streak",
            subtitle = "${readinessLine}$chaptersCompleted Chapters Completed",
            statLeftLabel = "SCHOLAR XP",
            statLeftValue = "$xpTotal XP",
            statRightLabel = "BOARD TARGET",
            statRightValue = "Class $classLevel",
            footerStudent = studentName.ifBlank { "KPK Scholar" },
            accentStart = 0xFF38BDF8.toInt(),
            accentEnd = 0xFF3B82F6.toInt(),
        )
        launchShareIntent(
            context = context,
            bitmap = bitmap,
            captionText = "$streakDays-day study streak and $xpTotal XP earned on Imtihan Pro for BISE $boardLabel Class $classLevel!"
        )
    }

    private fun renderCardBitmap(
        headline: String,
        eyebrow: String,
        title: String,
        subtitle: String,
        statLeftLabel: String,
        statLeftValue: String,
        statRightLabel: String,
        statRightValue: String,
        footerStudent: String,
        accentStart: Int,
        accentEnd: Int,
    ): Bitmap {
        val w = 1080
        val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 1. Deep OLED Obsidian backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(0xFF020612.toInt(), 0xFF091B42.toInt(), 0xFF020612.toInt()),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Subtle radial glow behind hero metric
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentStart
            alpha = 38
        }
        canvas.drawCircle(w * 0.5f, h * 0.42f, 340f, glowPaint)

        // 2. Outer Neo-Silver Glass card frame
        val cardRect = RectF(72f, 84f, w - 72f, h - 84f)
        val glassFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCC0B152C.toInt()
        }
        canvas.drawRoundRect(cardRect, 54f, 54f, glassFill)

        val glassBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            shader = LinearGradient(
                cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                intArrayOf(0x88E2ECFF.toInt(), 0x2638BDF8.toInt(), 0x6694A3B8.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, 54f, 54f, glassBorder)

        // 3. Eyebrow label
        val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF38BDF8.toInt()
            textSize = 30f
            letterSpacing = 0.14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(eyebrow, w * 0.5f, 210f, eyebrowPaint)

        // Crescent-Spark Emblem motif
        val emblemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2ECFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawCircle(w * 0.5f, 315f, 44f, emblemPaint)

        // 4. Giant Hero Number
        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 210f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            shader = LinearGradient(
                w * 0.25f, 400f, w * 0.75f, 650f,
                intArrayOf(0xFFFFFFFF.toInt(), accentStart, accentEnd),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawText(headline, w * 0.5f, 595f, heroPaint)

        // 5. Title & Subtitle
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 54f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, w * 0.5f, 700f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFAEC2E8.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtitle, w * 0.5f, 765f, subtitlePaint)

        // 6. Dual KPI Glass Tiles
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF102145.toInt()
        }
        val tileBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0x44CBD5E1
        }
        val leftRect = RectF(132f, 860f, w * 0.5f - 20f, 1060f)
        val rightRect = RectF(w * 0.5f + 20f, 860f, w - 132f, 1060f)
        canvas.drawRoundRect(leftRect, 32f, 32f, tilePaint)
        canvas.drawRoundRect(leftRect, 32f, 32f, tileBorder)
        canvas.drawRoundRect(rightRect, 32f, 32f, tilePaint)
        canvas.drawRoundRect(rightRect, 32f, 32f, tileBorder)

        val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF7FA3E6.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val statValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 46f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(statLeftLabel, leftRect.centerX(), 930f, statLabelPaint)
        canvas.drawText(statLeftValue, leftRect.centerX(), 1005f, statValPaint)
        canvas.drawText(statRightLabel, rightRect.centerX(), 930f, statLabelPaint)
        canvas.drawText(statRightValue, rightRect.centerX(), 1005f, statValPaint)

        // 7. Footer Signature
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCBD5E1.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$footerStudent · KPK Board Topper Track", w * 0.5f, 1170f, footerPaint)

        val brandSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("IMTIHAN PRO · 100% OFFLINE & FREE", w * 0.5f, 1220f, brandSubPaint)

        return bmp
    }

    private fun launchShareIntent(
        context: Context,
        bitmap: Bitmap,
        captionText: String,
    ) {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, "imtihan_share_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 96, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, captionText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share to WhatsApp, Status or Story").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
