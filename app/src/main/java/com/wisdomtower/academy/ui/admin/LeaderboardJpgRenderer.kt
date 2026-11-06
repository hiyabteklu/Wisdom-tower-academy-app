package com.wisdomtower.academy.ui.admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.wisdomtower.academy.data.admin.BroadcastsRepository
import com.wisdomtower.academy.data.admin.LeaderboardExportData
import com.wisdomtower.academy.data.admin.LeaderboardExportWinner
import java.io.ByteArrayOutputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class LeaderboardJpgFormat(val width: Int, val height: Int, val suffix: String) {
    LANDSCAPE_4_3(1600, 1200, "4x3"),
    PORTRAIT_9_16(1080, 1920, "9x16")
}

// U5 (2026-11-06, owner): golden celebration poster for the Top 3, in 4:3 and 9:16.
// Night-navy background with a warm gold glow and light rays, confetti and sparkles, a gold frame, a golden trophy,
// the word CHAMPIONS, the exam title, subject and date, then a real podium (2nd left, 1st centre and tallest, 3rd right)
// with gold, silver and bronze blocks. Above each block: the name, the score and the time taken.
// No minimum number of participants: a missing rank leaves its podium slot empty; nobody ranked = a short card.
// No phone, e-mail or other personal data is drawn (names, scores and times only). Confetti is seeded, so the same
// competition always produces the same picture.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
object LeaderboardJpgRenderer {
    private val navyTop = Color.rgb(3, 8, 20)
    private val navyMid = Color.rgb(9, 22, 46)
    private val navyDark = Color.rgb(5, 13, 27)
    private val ink = Color.rgb(247, 250, 255)
    private val muted = Color.rgb(170, 185, 205)
    private val goldLight = Color.rgb(255, 238, 168)
    private val gold = Color.rgb(246, 195, 74)
    private val goldDeep = Color.rgb(176, 122, 30)
    private val silverLight = Color.rgb(236, 243, 252)
    private val silver = Color.rgb(190, 205, 224)
    private val silverDeep = Color.rgb(110, 128, 152)
    private val bronzeLight = Color.rgb(236, 170, 112)
    private val bronze = Color.rgb(202, 129, 75)
    private val bronzeDeep = Color.rgb(120, 66, 30)
    private val cyan = Color.rgb(55, 211, 232)
    private val darkText = Color.argb(215, 5, 13, 27)

    fun render(data: LeaderboardExportData, format: LeaderboardJpgFormat): ByteArray {
        val w = format.width
        val h = format.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val portrait = format == LeaderboardJpgFormat.PORTRAIT_9_16
        val cx = w / 2f
        val contentWidth = w - 220f

        drawBackground(canvas, w, h, if (portrait) 330f else 245f)
        drawRays(canvas, cx, if (portrait) 330f else 245f, max(w, h).toFloat())
        drawConfetti(canvas, w, h, portrait)
        drawFrame(canvas, w, h)

        // header
        val brandY = if (portrait) 118f else 86f
        drawCentered(canvas, "WISDOM TOWER ACADEMY", cx, brandY, contentWidth, paintOf(30f, gold, true, false, 0.3f))
        val subY = brandY + if (portrait) 46f else 38f
        drawCentered(
            canvas, "GLOBAL TIMED EXAM  \u00b7  LEADERBOARD", cx, subY, contentWidth,
            paintOf(if (portrait) 24f else 22f, muted, false, false, 0.2f)
        )

        val trophyTop = if (portrait) 205f else 150f
        val trophySize = if (portrait) 250f else 190f
        drawTrophy(canvas, cx, trophyTop, trophySize)

        val titleSize = if (portrait) 112f else 92f
        val titleBase = trophyTop + trophySize + if (portrait) 135f else 100f
        val champions = paintOf(titleSize, gold, true, true, 0.06f)
        champions.shader = LinearGradient(0f, titleBase - titleSize, 0f, titleBase, goldLight, goldDeep, Shader.TileMode.CLAMP)
        drawCentered(canvas, "CHAMPIONS", cx, titleBase, contentWidth, champions)

        val examPaint = paintOf(if (portrait) 46f else 38f, ink, true, false, 0f)
        val examLines = wrapLines(data.examTitle.ifBlank { data.competitionTitle }, examPaint, contentWidth, if (portrait) 2 else 1)
        val examLineH = examPaint.textSize * 1.18f
        val examFirst = titleBase + if (portrait) 80f else 56f
        examLines.forEachIndexed { index, line ->
            drawCentered(canvas, line, cx, examFirst + index * examLineH, contentWidth, examPaint)
        }
        val examBottom = examFirst + max(0, examLines.size - 1) * examLineH

        val subjectY = examBottom + if (portrait) 58f else 46f
        drawCentered(
            canvas, "SUBJECT  \u00b7  ${data.subjectTitle}", cx, subjectY, contentWidth,
            paintOf(if (portrait) 30f else 26f, cyan, true, false, 0.08f)
        )
        val eventDate = BroadcastsRepository.formatEat(data.eventDate)
        drawCentered(
            canvas, "Competition date: $eventDate", cx, subjectY + if (portrait) 48f else 38f, contentWidth,
            paintOf(if (portrait) 24f else 22f, muted, false, false, 0f)
        )

        val ordered = data.winners.sortedBy { it.rank }
        if (ordered.isEmpty()) {
            val top = if (portrait) 1000f else 700f
            drawNoParticipants(canvas, 110f, top, contentWidth, if (portrait) 300f else 260f, portrait)
        } else if (portrait) {
            drawPodium(canvas, ordered, w, 1700f, 300f, 16f, 560f, 430f, 330f, true)
        } else {
            drawPodium(canvas, ordered, w, 1110f, 440f, 24f, 300f, 230f, 180f, false)
        }

        drawCentered(
            canvas, "WISDOM TOWER ACADEMY  \u00b7  LEARN WITH PURPOSE", cx,
            if (portrait) h - 72f else h - 56f, w - 160f,
            paintOf(if (portrait) 22f else 20f, muted, false, false, 0.12f)
        )

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)
        bitmap.recycle()
        return output.toByteArray()
    }

    private fun drawBackground(canvas: Canvas, w: Int, h: Int, glowY: Float) {
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(navyTop, navyMid, navyDark),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), base)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, glowY, w * 0.95f,
                Color.argb(95, 255, 196, 80), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow)
    }

    private fun drawRays(canvas: Canvas, ox: Float, oy: Float, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                ox, oy, radius,
                Color.argb(70, 255, 214, 110), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        val count = 16
        val step = 360.0 / count
        for (i in 0 until count) {
            val a1 = Math.toRadians(i * step)
            val a2 = Math.toRadians(i * step + step * 0.42)
            val path = Path()
            path.moveTo(ox, oy)
            path.lineTo(ox + (radius * cos(a1)).toFloat(), oy + (radius * sin(a1)).toFloat())
            path.lineTo(ox + (radius * cos(a2)).toFloat(), oy + (radius * sin(a2)).toFloat())
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private fun drawConfetti(canvas: Canvas, w: Int, h: Int, portrait: Boolean) {
        val rnd = java.util.Random(20261106L)
        val colors = intArrayOf(goldLight, gold, silverLight, cyan, bronzeLight, Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = if (portrait) 130 else 115
        repeat(count) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val size = 8f + rnd.nextFloat() * 14f
            paint.color = colors[rnd.nextInt(colors.size)]
            paint.alpha = 90 + rnd.nextInt(130)
            val kind = rnd.nextInt(3)
            canvas.save()
            canvas.rotate(rnd.nextFloat() * 360f, x, y)
            when (kind) {
                0 -> canvas.drawRect(x, y, x + size, y + size * 0.5f, paint)
                1 -> canvas.drawCircle(x, y, size * 0.35f, paint)
                else -> canvas.drawRoundRect(RectF(x, y, x + size * 1.8f, y + size * 0.28f), 4f, 4f, paint)
            }
            canvas.restore()
        }
        val spots = arrayOf(
            floatArrayOf(0.12f, 0.10f), floatArrayOf(0.88f, 0.12f), floatArrayOf(0.07f, 0.30f),
            floatArrayOf(0.93f, 0.33f), floatArrayOf(0.20f, 0.50f), floatArrayOf(0.80f, 0.48f),
            floatArrayOf(0.06f, 0.68f), floatArrayOf(0.95f, 0.70f), floatArrayOf(0.30f, 0.07f),
            floatArrayOf(0.70f, 0.06f)
        )
        for (spot in spots) {
            paint.color = if (rnd.nextBoolean()) Color.WHITE else goldLight
            paint.alpha = 235
            drawSparkle(canvas, spot[0] * w, spot[1] * h, 18f + rnd.nextFloat() * 18f, paint)
        }
    }

    private fun drawSparkle(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val path = Path()
        path.moveTo(cx, cy - r)
        path.quadTo(cx, cy, cx + r, cy)
        path.quadTo(cx, cy, cx, cy + r)
        path.quadTo(cx, cy, cx - r, cy)
        path.quadTo(cx, cy, cx, cy - r)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawFrame(canvas: Canvas, w: Int, h: Int) {
        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), goldLight, goldDeep, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(28f, 28f, w - 28f, h - 28f), 36f, 36f, outer)
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb(90, 246, 195, 74)
        }
        canvas.drawRoundRect(RectF(44f, 44f, w - 44f, h - 44f), 26f, 26f, inner)
    }

    private fun drawTrophy(canvas: Canvas, cx: Float, top: Float, s: Float) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, top + s * 0.5f, s * 1.1f,
                Color.argb(120, 255, 210, 90), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, top + s * 0.5f, s * 1.1f, halo)

        val goldShader = LinearGradient(
            cx - s * 0.4f, top, cx + s * 0.4f, top + s,
            intArrayOf(goldLight, gold, goldDeep), null, Shader.TileMode.CLAMP
        )
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = goldShader }
        val handle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = goldShader
            style = Paint.Style.STROKE
            strokeWidth = s * 0.05f
        }
        val w = s * 0.62f

        // handles first, so the cup covers their inner ends
        canvas.drawArc(RectF(cx - w / 2f - s * 0.20f, top + s * 0.06f, cx - w / 2f + s * 0.06f, top + s * 0.40f), 90f, 180f, false, handle)
        canvas.drawArc(RectF(cx + w / 2f - s * 0.06f, top + s * 0.06f, cx + w / 2f + s * 0.20f, top + s * 0.40f), 270f, 180f, false, handle)

        val cup = Path()
        cup.moveTo(cx - w / 2f, top)
        cup.lineTo(cx + w / 2f, top)
        cup.cubicTo(cx + w / 2f, top + s * 0.42f, cx + w * 0.18f, top + s * 0.52f, cx + w * 0.10f, top + s * 0.60f)
        cup.lineTo(cx - w * 0.10f, top + s * 0.60f)
        cup.cubicTo(cx - w * 0.18f, top + s * 0.52f, cx - w / 2f, top + s * 0.42f, cx - w / 2f, top)
        cup.close()
        canvas.drawPath(cup, fill)

        canvas.drawRect(cx - s * 0.05f, top + s * 0.60f, cx + s * 0.05f, top + s * 0.78f, fill)
        canvas.drawRoundRect(RectF(cx - s * 0.22f, top + s * 0.78f, cx + s * 0.22f, top + s * 0.90f), s * 0.03f, s * 0.03f, fill)
        canvas.drawRoundRect(RectF(cx - s * 0.30f, top + s * 0.90f, cx + s * 0.30f, top + s), s * 0.03f, s * 0.03f, fill)

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 120, 70, 10) }
        drawStar(canvas, cx, top + s * 0.27f, s * 0.12f, starPaint)
        val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(110, 255, 255, 255) }
        canvas.drawRoundRect(RectF(cx - w * 0.40f, top + s * 0.05f, cx - w * 0.30f, top + s * 0.34f), s * 0.03f, s * 0.03f, shine)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, outer: Float, paint: Paint) {
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else outer * 0.45f
            val ang = -Math.PI / 2 + i * Math.PI / 5
            val px = cx + (r * cos(ang)).toFloat()
            val py = cy + (r * sin(ang)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun palette(rank: Int): IntArray = when (rank) {
        1 -> intArrayOf(goldLight, gold, goldDeep)
        2 -> intArrayOf(silverLight, silver, silverDeep)
        else -> intArrayOf(bronzeLight, bronze, bronzeDeep)
    }

    private fun drawPodium(
        canvas: Canvas,
        winners: List<LeaderboardExportWinner>,
        w: Int,
        baseY: Float,
        blockW: Float,
        gap: Float,
        h1: Float,
        h2: Float,
        h3: Float,
        portrait: Boolean
    ) {
        val total = blockW * 3f + gap * 2f
        val startX = (w - total) / 2f
        val floor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, baseY, total * 0.6f,
                Color.argb(110, 246, 195, 74), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.save()
        canvas.scale(1f, 0.18f, w / 2f, baseY)
        canvas.drawCircle(w / 2f, baseY, total * 0.6f, floor)
        canvas.restore()

        val slots = intArrayOf(2, 1, 3)
        slots.forEachIndexed { index, rank ->
            val winner = winners.firstOrNull { it.rank == rank }
            if (winner != null) {
                val blockH = when (rank) { 1 -> h1; 2 -> h2; else -> h3 }
                val x = startX + index * (blockW + gap)
                drawPodiumBlock(canvas, winner, x, baseY - blockH, blockW, blockH, portrait)
            }
        }

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 3f
            color = Color.argb(200, 246, 195, 74)
        }
        canvas.drawLine(startX - 30f, baseY, startX + total + 30f, baseY, line)
    }

    private fun drawPodiumBlock(
        canvas: Canvas,
        winner: LeaderboardExportWinner,
        x: Float,
        top: Float,
        bw: Float,
        bh: Float,
        portrait: Boolean
    ) {
        val colors = palette(winner.rank)
        val cx = x + bw / 2f

        if (winner.rank == 1) {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, top, bw * 1.3f,
                    Color.argb(120, 255, 200, 80), Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, top, bw * 1.3f, glow)
        }

        val shape = Path()
        shape.addRoundRect(
            RectF(x, top, x + bw, top + bh),
            floatArrayOf(28f, 28f, 28f, 28f, 0f, 0f, 0f, 0f),
            Path.Direction.CW
        )
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                x, top, x, top + bh,
                intArrayOf(colors[0], colors[1], colors[2]),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(shape, fill)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(170, 255, 255, 255)
        }
        canvas.drawPath(shape, edge)

        // rank numeral and label on the block face
        val numSize = min(bw * 0.42f, bh * 0.55f)
        val numBase = top + 22f + numSize * 0.92f
        canvas.drawText(winner.rank.toString(), cx, numBase, paintOf(numSize, darkText, true, true, 0f))
        val labelText = when (winner.rank) { 1 -> "CHAMPION"; 2 -> "RUNNER-UP"; else -> "THIRD PLACE" }
        drawCentered(
            canvas, labelText, cx, numBase + if (portrait) 46f else 40f, bw - 24f,
            paintOf(if (portrait) 26f else 24f, darkText, true, false, 0.12f)
        )

        // name, score and time above the block
        val nameSize = if (winner.rank == 1) (if (portrait) 44f else 40f) else (if (portrait) 36f else 32f)
        val scoreSize = if (winner.rank == 1) (if (portrait) 52f else 46f) else (if (portrait) 46f else 40f)
        val timeSize = if (portrait) 24f else 22f

        val timeY = top - 22f
        drawCentered(
            canvas, "TIME  \u00b7  ${formatTime(winner.timeTakenMs)}", cx, timeY, bw,
            paintOf(timeSize, muted, true, false, 0.05f)
        )
        val scoreY = timeY - timeSize - 14f
        val scoreText = if (winner.total > 0) "${winner.score} / ${winner.total}" else "${winner.score} pts"
        drawCentered(canvas, scoreText, cx, scoreY, bw, paintOf(scoreSize, colors[0], true, true, 0f))

        val namePaint = paintOf(nameSize, ink, true, false, 0f)
        val lines = wrapLines(winner.name.ifBlank { "Student" }, namePaint, bw, 2)
        val lineH = nameSize * 1.18f
        val lastY = scoreY - scoreSize - 8f
        lines.forEachIndexed { k, line ->
            drawCentered(canvas, line, cx, lastY - (lines.size - 1 - k) * lineH, bw, namePaint)
        }
    }

    private fun drawNoParticipants(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, portrait: Boolean) {
        val rect = RectF(x, y, x + width, y + height)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(40, 28, 47, 69) }
        canvas.drawRoundRect(rect, 34f, 34f, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(170, 246, 195, 74)
        }
        canvas.drawRoundRect(rect, 34f, 34f, border)
        val cx = x + width / 2f
        val gt = paintOf(if (portrait) 56f else 48f, gold, true, true, 0.05f)
        drawCentered(canvas, "NO PARTICIPANTS", cx, y + height / 2f - 4f, width - 80f, gt)
        drawCentered(
            canvas, "No ranked results for this competition.", cx, y + height / 2f + 52f, width - 80f,
            paintOf(28f, muted, false, false, 0f)
        )
    }

    private fun paintOf(size: Float, color: Int, bold: Boolean, serif: Boolean, spacing: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = Paint.Align.CENTER
            letterSpacing = spacing
            typeface = Typeface.create(if (serif) "serif" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun drawCentered(canvas: Canvas, text: String, cx: Float, baseline: Float, maxWidth: Float, paint: Paint) {
        val original = paint.textSize
        while (paint.measureText(text) > maxWidth && paint.textSize > 14f) paint.textSize -= 1f
        canvas.drawText(text, cx, baseline, paint)
        paint.textSize = original
    }

    private fun wrapLines(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
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
        return visible
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
