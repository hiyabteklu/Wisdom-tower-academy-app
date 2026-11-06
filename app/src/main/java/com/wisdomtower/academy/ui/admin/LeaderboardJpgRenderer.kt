package com.wisdomtower.academy.ui.admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.wisdomtower.academy.data.admin.BroadcastsRepository
import com.wisdomtower.academy.data.admin.LeaderboardExportData
import com.wisdomtower.academy.data.admin.LeaderboardExportWinner
import java.io.ByteArrayOutputStream
import kotlin.math.max

enum class LeaderboardJpgFormat(val width: Int, val height: Int, val suffix: String) {
    LANDSCAPE_4_3(1600, 1200, "4x3"),
    PORTRAIT_9_16(1080, 1920, "9x16")
}

// U5 (2026-11-06, owner): no minimum number of participants. The image is drawn with 0, 1, 2 or 3 winners: a missing
// rank is simply not drawn, and with nobody ranked a short "no participants" card is shown. Design otherwise unchanged.
object LeaderboardJpgRenderer {
    private val navy = Color.rgb(5, 13, 27)
    private val ink = Color.rgb(244, 248, 255)
    private val muted = Color.rgb(161, 178, 199)
    private val gold = Color.rgb(246, 195, 74)
    private val silver = Color.rgb(195, 211, 229)
    private val bronze = Color.rgb(202, 129, 75)
    private val cyan = Color.rgb(55, 211, 232)

    fun render(data: LeaderboardExportData, format: LeaderboardJpgFormat): ByteArray {
        val bitmap = Bitmap.createBitmap(format.width, format.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(navy)
        drawBackground(canvas, format.width, format.height)

        val portrait = format == LeaderboardJpgFormat.PORTRAIT_9_16
        val pad = if (portrait) 72f else 90f
        val contentWidth = format.width - pad * 2f

        val brandPaint = textPaint(30f, gold, true)
        canvas.drawText("WISDOM TOWER ACADEMY", pad, if (portrait) 100f else 94f, brandPaint)

        val smallPaint = textPaint(if (portrait) 25f else 22f, muted, false)
        canvas.drawText("GLOBAL TIMED EXAM  ·  TOP THREE", pad, if (portrait) 145f else 135f, smallPaint)

        val titleSize = if (portrait) 55f else 54f
        val titleBottom = drawWrapped(
            canvas,
            data.examTitle.ifBlank { data.competitionTitle },
            pad,
            if (portrait) 235f else 220f,
            contentWidth,
            titleSize,
            ink,
            true,
            if (portrait) 2 else 2
        )
        val subjectY = titleBottom + if (portrait) 12f else 8f
        val subjectPaint = textPaint(if (portrait) 31f else 28f, cyan, true)
        drawFit(canvas, "SUBJECT  ·  ${data.subjectTitle}", pad, subjectY, contentWidth, subjectPaint)

        val eventDate = BroadcastsRepository.formatEat(data.eventDate)
        val datePaint = textPaint(if (portrait) 24f else 22f, muted, false)
        canvas.drawText("Competition date: $eventDate", pad, subjectY + if (portrait) 54f else 46f, datePaint)

        val ordered = data.winners.sortedBy { it.rank }
        if (ordered.isEmpty()) {
            val top = if (portrait) max(subjectY + 100f, 455f) else 430f
            drawNoParticipants(canvas, pad, top, contentWidth, portrait)
        } else if (portrait) {
            val cardTop = max(subjectY + 100f, 455f)
            val cardGap = 26f
            val cardHeight = 350f
            ordered.forEach { winner ->
                drawWinnerCard(
                    canvas, winner, pad, cardTop + (winner.rank - 1) * (cardHeight + cardGap),
                    contentWidth, cardHeight, portrait = true
                )
            }
        } else {
            val winner = ordered.firstOrNull { it.rank == 1 } ?: ordered.first()
            val podium = ordered.filter { it.rank == 2 || it.rank == 3 }
            val firstTop = 430f
            drawWinnerCard(canvas, winner, pad, firstTop, contentWidth, 290f, portrait = false)
            val gap = 34f
            val cardWidth = (contentWidth - gap) / 2f
            podium.getOrNull(0)?.let {
                drawWinnerCard(canvas, it, pad, 760f, cardWidth, 300f, portrait = false)
            }
            podium.getOrNull(1)?.let {
                drawWinnerCard(canvas, it, pad + cardWidth + gap, 760f, cardWidth, 300f, portrait = false)
            }
        }
        drawFooter(canvas, format.width, format.height, pad, portrait)

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)
        bitmap.recycle()
        return output.toByteArray()
    }

    private fun drawNoParticipants(canvas: Canvas, x: Float, y: Float, width: Float, portrait: Boolean) {
        val height = if (portrait) 300f else 260f
        val rect = RectF(x, y, x + width, y + height)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(30, 28, 47, 69) }
        canvas.drawRoundRect(rect, 34f, 34f, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 55, 211, 232)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, 34f, 34f, border)
        val inset = if (portrait) 38f else 42f
        val inner = width - inset * 2f
        drawFit(canvas, "NO PARTICIPANTS", x + inset, y + height / 2f - 6f, inner, textPaint(if (portrait) 48f else 44f, gold, true))
        drawFit(canvas, "No ranked results for this competition.", x + inset, y + height / 2f + 48f, inner, textPaint(26f, muted, false))
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                width * 0.82f, height * 0.12f, width * 0.62f,
                intArrayOf(Color.argb(48, 25, 151, 186), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(28, 125, 174, 205)
            strokeWidth = 2f
        }
        val step = if (width > height) 100f else 88f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), line)
            x += step
        }
    }

    private fun drawWinnerCard(
        canvas: Canvas,
        winner: LeaderboardExportWinner,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        portrait: Boolean
    ) {
        val accent = when (winner.rank) {
            1 -> gold
            2 -> silver
            else -> bronze
        }
        val rect = RectF(x, y, x + width, y + height)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                x, y, x + width, y + height,
                intArrayOf(Color.argb(42, Color.red(accent), Color.green(accent), Color.blue(accent)), Color.argb(18, 28, 47, 69)),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, 34f, 34f, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, Color.red(accent), Color.green(accent), Color.blue(accent))
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, 34f, 34f, border)

        val inset = if (portrait) 38f else 42f
        val label = when (winner.rank) { 1 -> "1ST PLACE"; 2 -> "2ND PLACE"; else -> "3RD PLACE" }
        val labelPaint = textPaint(if (portrait) 27f else 25f, accent, true)
        canvas.drawText(label, x + inset, y + if (portrait) 66f else 62f, labelPaint)

        val nameSize = if (portrait) 48f else if (winner.rank == 1) 48f else 39f
        val nameBottom = drawWrapped(
            canvas,
            winner.name.ifBlank { "Student" },
            x + inset,
            y + if (portrait) 140f else 132f,
            width - inset * 2f,
            nameSize,
            ink,
            true,
            2
        )

        val scoreSize = if (portrait) 58f else if (winner.rank == 1) 62f else 50f
        val scorePaint = textPaint(scoreSize, accent, true)
        val scoreText = "${winner.score} / ${winner.total}"
        val scoreY = max(nameBottom + if (portrait) 70f else 54f, y + height - if (portrait) 120f else 108f)
        drawFit(canvas, scoreText, x + inset, scoreY, width - inset * 2f, scorePaint)

        val timeText = "TIME TAKEN  ·  ${formatTime(winner.timeTakenMs)}"
        val timePaint = textPaint(if (portrait) 25f else 22f, muted, true)
        canvas.drawText(timeText, x + inset, y + height - 42f, timePaint)
    }

    private fun drawFooter(canvas: Canvas, width: Int, height: Int, pad: Float, portrait: Boolean) {
        val paint = textPaint(if (portrait) 21f else 20f, muted, false)
        val y = if (portrait) height - 64f else height - 48f
        canvas.drawText("WISDOM TOWER ACADEMY  ·  LEARN WITH PURPOSE", pad, y, paint)
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 55, 211, 232)
            strokeWidth = 2f
        }
        canvas.drawLine(pad, y + 22f, width - pad, y + 22f, rule)
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun drawFit(canvas: Canvas, text: String, x: Float, baseline: Float, maxWidth: Float, paint: Paint) {
        val original = paint.textSize
        while (paint.measureText(text) > maxWidth && paint.textSize > 16f) paint.textSize -= 1f
        canvas.drawText(text, x, baseline, paint)
        paint.textSize = original
    }

    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        maxWidth: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        maxLines: Int
    ): Float {
        val paint = textPaint(size, color, bold)
        val words = text.replace(Regex("\\s+"), " ").trim().split(' ').filter { it.isNotBlank() }
        val lines = ArrayList<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        val visible = lines.take(maxLines).toMutableList()
        if (lines.size > maxLines && visible.isNotEmpty()) {
            val last = visible.last()
            visible[visible.lastIndex] = last.take(max(1, last.length - 3)) + "..."
        }
        val lineHeight = size * 1.18f
        visible.forEachIndexed { index, line -> drawFit(canvas, line, x, baseline + index * lineHeight, maxWidth, paint) }
        return baseline + max(0, visible.size - 1) * lineHeight
    }

    private fun formatTime(ms: Long?): String {
        if (ms == null || ms < 0L) return "Not recorded"
        val totalSeconds = ms / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "${hours}h ${minutes}m ${seconds}s"
        } else {
            "${minutes}m ${seconds}s"
        }
    }
}
