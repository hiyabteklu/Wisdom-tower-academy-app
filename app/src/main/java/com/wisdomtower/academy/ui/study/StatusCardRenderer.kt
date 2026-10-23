package com.wisdomtower.academy.ui.study

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.wisdomtower.academy.BrandBytes
import com.wisdomtower.academy.R
import kotlin.math.max
import kotlin.math.min

/**
 * STATUS CARD (docs/STATUS_CARD_PLAN.md, slice S3): draws the 1080 x 1920 (9:16) weekly picture with android.graphics.Canvas,
 * same approach as CoverPageRenderer. NO UI here (the Save button is slice S4). Reads only [StatusCardData], so the picture
 * and the "Your week" block can never disagree.
 *
 * REAL DATA ONLY: an unknown figure is a dash and a grey ring, never 0. The logo is the sign-in logo (assets/brand/logo.png
 * through BrandBytes). Titles use the bundled Tinos serif, numbers use the system sans bold. No new font files.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */

private const val SCR_W = 1080
private const val SCR_H = 1920

private val SCR_BG_TOP = Color.parseColor("#0B1426")
private val SCR_BG_BOTTOM = Color.parseColor("#060B15")
private val SCR_CARD = Color.parseColor("#111C33")
private val SCR_CARD_EDGE = Color.parseColor("#22304D")
private val SCR_TEXT = Color.parseColor("#F1F5F9")
private val SCR_MUTED = Color.parseColor("#94A3B8")
private val SCR_TRACK = Color.parseColor("#2A3650")
private val SCR_CYAN = Color.parseColor("#22E0FF")
private val SCR_AMBER = Color.parseColor("#FBBF24")
private val SCR_EMERALD = Color.parseColor("#34D399")
private val SCR_ROSE = Color.parseColor("#FB7185")

/** The sign-in logo as a Bitmap, or null when the asset is missing. Call off the main thread. */
fun loadStatusCardLogo(context: Context): Bitmap? {
    val bytes = BrandBytes.logo(context)
    if (bytes.isEmpty()) return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

/** The bundled Tinos bold, or the phone's serif bold when it cannot be loaded. */
fun loadStatusCardSerif(context: Context): Typeface {
    val f = try {
        ResourcesCompat.getFont(context, R.font.tinos_bold)
    } catch (_: Exception) {
        null
    }
    return f ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)
}

private fun scrPaint(color: Int, size: Float, face: Typeface, align: Paint.Align = Paint.Align.LEFT): Paint {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = color
    p.textSize = size
    p.typeface = face
    p.textAlign = align
    return p
}

/** Shrinks the text until it fits [maxW], then cuts it with "...". Leaves the paint at the size it used. */
private fun scrFit(p: Paint, text: String, maxW: Float, startSize: Float, minSize: Float): String {
    var s = startSize
    p.textSize = s
    while (p.measureText(text) > maxW && s > minSize) {
        s -= 2f
        p.textSize = s
    }
    if (p.measureText(text) <= maxW) return text
    var t = text
    while (t.length > 1 && p.measureText("$t...") > maxW) t = t.dropLast(1)
    return "$t..."
}

/** Greedy word wrap into at most [maxLines] lines; the last line is cut with "..." when the text is longer. */
private fun scrWrap(p: Paint, text: String, maxW: Float, maxLines: Int): List<String> {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val lines = ArrayList<String>()
    var cur = ""
    var i = 0
    while (i < words.size) {
        val w = words[i]
        val tryLine = if (cur.isEmpty()) w else "$cur $w"
        if (p.measureText(tryLine) <= maxW || cur.isEmpty()) {
            cur = tryLine
            i++
        } else {
            lines.add(cur)
            cur = ""
            if (lines.size == maxLines) break
        }
    }
    if (lines.size < maxLines && cur.isNotEmpty()) {
        lines.add(cur)
        cur = ""
    }
    val usedAll = i >= words.size && cur.isEmpty()
    if (!usedAll && lines.isNotEmpty()) {
        var last = lines.removeAt(lines.size - 1)
        while (last.length > 1 && p.measureText("$last...") > maxW) last = last.dropLast(1)
        lines.add("$last...")
    }
    return lines
}

private fun scrCard(c: Canvas, l: Float, t: Float, r: Float, b: Float, edge: Int = SCR_CARD_EDGE, fill: Int = SCR_CARD) {
    val rect = RectF(l, t, r, b)
    val f = Paint(Paint.ANTI_ALIAS_FLAG)
    f.color = fill
    c.drawRoundRect(rect, 28f, 28f, f)
    val s = Paint(Paint.ANTI_ALIAS_FLAG)
    s.style = Paint.Style.STROKE
    s.strokeWidth = 2f
    s.color = edge
    c.drawRoundRect(rect, 28f, 28f, s)
}

/** One gauge: 300 degree ring, number in the middle, small line under it, label under the ring. Unknown = grey ring + dash. */
private fun scrGauge(
    c: Canvas, left: Float, top: Float, w: Float, h: Float,
    label: String, number: String?, sub: String?, fill: Float?, color: Int, sans: Typeface
) {
    scrCard(c, left, top, left + w, top + h)
    val cx = left + w / 2f
    val cy = top + 128f
    val radius = 92f
    val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    ring.style = Paint.Style.STROKE
    ring.strokeWidth = 22f
    ring.strokeCap = Paint.Cap.ROUND
    val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
    ring.color = SCR_TRACK
    c.drawArc(oval, 120f, 300f, false, ring)
    if (fill != null && fill > 0f) {
        ring.color = color
        c.drawArc(oval, 120f, 300f * fill.coerceIn(0f, 1f), false, ring)
    }
    val numP = scrPaint(if (number == null) SCR_MUTED else SCR_TEXT, 54f, sans, Paint.Align.CENTER)
    val numText = scrFit(numP, number ?: "–", 140f, 54f, 26f)
    c.drawText(numText, cx, cy + 14f, numP)
    if (sub != null) {
        val subP = scrPaint(SCR_MUTED, 22f, sans, Paint.Align.CENTER)
        c.drawText(scrFit(subP, sub, 140f, 22f, 14f), cx, cy + 46f, subP)
    }
    val labP = scrPaint(SCR_TEXT, 30f, sans, Paint.Align.CENTER)
    c.drawText(scrFit(labP, label, w - 40f, 30f, 18f), cx, top + h - 28f, labP)
}

/**
 * Draws the picture. [logo] may be null (the header then has no logo). [serif] for titles and the name, [sans] for numbers.
 * Call off the main thread. Recycle the returned bitmap after saving.
 */
fun renderStatusCardBitmap(data: StatusCardData, logo: Bitmap?, serif: Typeface, sans: Typeface): Bitmap {
    val bmp = Bitmap.createBitmap(SCR_W, SCR_H, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val sansBold = Typeface.create(sans, Typeface.BOLD)

    // background + borders
    val bg = Paint()
    bg.shader = LinearGradient(0f, 0f, 0f, SCR_H.toFloat(), SCR_BG_TOP, SCR_BG_BOTTOM, Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, SCR_W.toFloat(), SCR_H.toFloat(), bg)
    val border = Paint(Paint.ANTI_ALIAS_FLAG)
    border.style = Paint.Style.STROKE
    border.strokeWidth = 6f
    border.color = SCR_CYAN
    c.drawRoundRect(RectF(24f, 24f, SCR_W - 24f, SCR_H - 24f), 36f, 36f, border)
    border.strokeWidth = 2f
    border.color = SCR_CARD_EDGE
    c.drawRoundRect(RectF(40f, 40f, SCR_W - 40f, SCR_H - 40f), 28f, 28f, border)

    val left = 70f
    val right = SCR_W - 70f
    val inner = right - left

    // header 0-230
    if (logo != null && logo.width > 0 && logo.height > 0) {
        val side = min(logo.width, logo.height)
        val src = android.graphics.Rect((logo.width - side) / 2, (logo.height - side) / 2, (logo.width + side) / 2, (logo.height + side) / 2)
        val dst = RectF(left, 70f, left + 120f, 190f)
        val clip = Path()
        clip.addRoundRect(dst, 26f, 26f, Path.Direction.CW)
        c.save()
        c.clipPath(clip)
        c.drawBitmap(logo, src, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        c.restore()
    }
    val titleX = left + 150f
    val titleP = scrPaint(SCR_TEXT, 46f, serif)
    c.drawText(scrFit(titleP, "WISDOM TOWER ACADEMY", right - titleX, 46f, 26f), titleX, 128f, titleP)
    val subP = scrPaint(SCR_CYAN, 32f, sans)
    c.drawText(scrFit(subP, "Weekly status, ${data.weekRangeLabel}", right - titleX, 32f, 20f), titleX, 176f, subP)
    if (data.asOfLabel != null) {
        val asP = scrPaint(SCR_AMBER, 24f, sans)
        c.drawText(scrFit(asP, data.asOfLabel, inner, 24f, 16f), left, 224f, asP)
    }

    // identity 250-470
    val nameP = scrPaint(SCR_TEXT, 56f, serif)
    val nameLines = scrWrap(nameP, data.studentName, inner, 2)
    var y = 300f
    for ((i, line) in nameLines.withIndex()) {
        c.drawText(line, left, y, nameP)
        if (i < nameLines.size - 1) y += 62f
    }
    y += 46f
    if (data.studentId != null) {
        val idP = scrPaint(SCR_MUTED, 30f, sans)
        c.drawText("ID  ${data.studentId}", left, y, idP)
        y += 44f
    }
    val chipText = data.standing
    var chipLeft = right
    if (chipText != null) {
        val chipColor = when (chipText) {
            "Top performer", "Strong" -> SCR_EMERALD
            "On track" -> SCR_CYAN
            else -> SCR_ROSE
        }
        val chipP = scrPaint(chipColor, 28f, sansBold, Paint.Align.CENTER)
        val cw = chipP.measureText(chipText) + 44f
        chipLeft = right - cw
        val chipRect = RectF(chipLeft, y - 36f, right, y + 12f)
        val chipStroke = Paint(Paint.ANTI_ALIAS_FLAG)
        chipStroke.style = Paint.Style.STROKE
        chipStroke.strokeWidth = 3f
        chipStroke.color = chipColor
        c.drawRoundRect(chipRect, 24f, 24f, chipStroke)
        c.drawText(chipText, chipLeft + cw / 2f, y, chipP)
    }
    val trackP = scrPaint(SCR_TEXT, 30f, sans)
    c.drawText(scrFit(trackP, data.trackName, max(chipLeft - left - 20f, 100f), 30f, 18f), left, y, trackP)

    // gauges 500-1130 (2 x 2)
    val gap = 30f
    val gw = (inner - gap) / 2f
    val gh = 300f
    val g1Number = data.weekMinutes?.let { scDuration(it) }
    val g1Sub = if (data.weekMinutes != null) "of ${formatStatusNumber(data.weekTargetHours)} h goal" else null
    scrGauge(c, left, 500f, gw, gh, "Study this week", g1Number, g1Sub, data.weekFill, SCR_CYAN, sansBold)
    val g2Sub = if (data.accuracyPct != null && data.questionsAttempted > 0) "${data.questionsCorrect} of ${data.questionsAttempted} right" else null
    scrGauge(
        c, left + gw + gap, 500f, gw, gh, "Quiz accuracy",
        data.accuracyPct?.let { "$it%" }, g2Sub, data.accuracyPct?.let { it / 100f }, SCR_EMERALD, sansBold
    )
    scrGauge(
        c, left, 500f + gh + gap, gw, gh, "Day streak",
        data.streakDays?.toString(), if (data.streakDays != null) "days in a row" else null, data.streakFill, SCR_AMBER, sansBold
    )
    scrGauge(
        c, left + gw + gap, 500f + gh + gap, gw, gh, "Time you stayed focused",
        data.focusPct?.let { "$it%" }, null, data.focusPct?.let { it / 100f }, SCR_CYAN, sansBold
    )

    // week bars 1150-1360
    scrCard(c, left, 1150f, right, 1360f)
    val wkTitle = scrPaint(SCR_TEXT, 32f, serif)
    c.drawText("Your week", left + 30f, 1200f, wkTitle)
    val wkTotal = scrPaint(SCR_MUTED, 28f, sans, Paint.Align.RIGHT)
    val totalText = data.weekMinutes?.let { "${scDuration(it)} this week" } ?: "–"
    c.drawText(scrFit(wkTotal, totalText, 520f, 28f, 18f), right - 30f, 1200f, wkTotal)
    val maxSec = max(data.days.maxOfOrNull { it.seconds ?: 0 } ?: 0, 1)
    val slot = (inner - 60f) / 7f
    val barW = 64f
    val baseY = 1316f
    val topY = 1226f
    val letterP = scrPaint(SCR_MUTED, 26f, sans, Paint.Align.CENTER)
    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    for ((i, d) in data.days.withIndex()) {
        val bx = left + 30f + slot * i + slot / 2f
        val sec = d.seconds
        val hgt = if (sec == null) 6f else max(6f, (baseY - topY) * (sec.toFloat() / maxSec.toFloat()))
        barPaint.color = when {
            sec == null -> SCR_TRACK
            d.isToday -> SCR_CYAN
            else -> Color.parseColor("#3B6FB6")
        }
        c.drawRoundRect(RectF(bx - barW / 2f, baseY - hgt, bx + barW / 2f, baseY), 10f, 10f, barPaint)
        letterP.color = if (d.isToday) SCR_CYAN else SCR_MUTED
        c.drawText(d.letter, bx, 1346f, letterP)
    }

    // subjects 1380-1650
    c.drawText("Subjects", left, 1420f, scrPaint(SCR_TEXT, 32f, serif))
    if (data.subjects.isEmpty()) {
        val nP = scrPaint(SCR_MUTED, 28f, sans)
        val note = data.weakNote ?: "No subject activity yet."
        for ((i, line) in scrWrap(nP, note, inner, 2).withIndex()) c.drawText(line, left, 1470f + i * 38f, nP)
    } else {
        val namePaint = scrPaint(SCR_TEXT, 28f, sans)
        val pctPaint = scrPaint(SCR_MUTED, 26f, sansBold, Paint.Align.RIGHT)
        val barBg = Paint(Paint.ANTI_ALIAS_FLAG)
        val barFg = Paint(Paint.ANTI_ALIAS_FLAG)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        dot.color = SCR_ROSE
        for ((i, s) in data.subjects.withIndex()) {
            val ry = 1466f + i * 42f
            val nx = if (s.isWeak) left + 26f else left
            if (s.isWeak) c.drawCircle(left + 9f, ry - 9f, 9f, dot)
            c.drawText(scrFit(namePaint, s.name, 400f - (nx - left), 28f, 16f), nx, ry, namePaint)
            val bl = left + 430f
            val br = right - 110f
            barBg.color = SCR_TRACK
            c.drawRoundRect(RectF(bl, ry - 18f, br, ry - 6f), 6f, 6f, barBg)
            val pct = s.completionPct
            if (pct != null && pct > 0) {
                barFg.color = if (s.isWeak) SCR_ROSE else SCR_CYAN
                c.drawRoundRect(RectF(bl, ry - 18f, bl + (br - bl) * (pct.coerceIn(0, 100) / 100f), ry - 6f), 6f, 6f, barFg)
            }
            c.drawText(if (pct != null) "$pct%" else "–", right, ry, pctPaint)
        }
    }

    // weak box (or good-week box) 1655-1830
    val weak = data.weak
    val boxColor = if (weak != null) SCR_ROSE else SCR_EMERALD
    scrCard(c, left, 1655f, right, 1830f, edge = boxColor)
    val headP = scrPaint(boxColor, 32f, serif)
    val head = if (weak != null) "Needs work: ${weak.subject.name}" else "Keep going"
    c.drawText(scrFit(headP, head, inner - 60f, 32f, 20f), left + 30f, 1706f, headP)
    val stepP = scrPaint(SCR_TEXT, 29f, sans)
    for ((i, line) in scrWrap(stepP, data.nextStep, inner - 60f, 3).withIndex()) {
        c.drawText(line, left + 30f, 1752f + i * 36f, stepP)
    }

    // footer
    val footP = scrPaint(SCR_MUTED, 24f, sans, Paint.Align.CENTER)
    c.drawText(
        scrFit(footP, "wisdom-tower-academy.live   |   ${data.savedLabel}", inner, 24f, 14f),
        SCR_W / 2f, 1872f, footP
    )
    return bmp
}
