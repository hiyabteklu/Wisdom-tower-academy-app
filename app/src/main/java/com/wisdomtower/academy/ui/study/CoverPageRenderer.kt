package com.wisdomtower.academy.ui.study

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cover Page Generator, drawing code.
 *
 * ONE drawing function ([drawCoverPage]) paints a page on an android.graphics.Canvas. The same function feeds the
 * on-screen preview (a Bitmap) and the PDF (android.graphics.pdf.PdfDocument, built into Android: no new dependency,
 * no permission), so the PDF is exactly what the preview shows. The page is A4 at 72 dpi: 595 x 842 points.
 *
 * 2026-10-23 redesign (owner order): the 11 weak templates (Classic, Banner, Elegant, Bold, Split, Minimal, Dark, Royal,
 * Executive, Heritage, Prestige) were deleted and replaced by 5 new premium ones (Gallery, Crest, Noir, Prism, Orbit).
 * The 11 plain borders (Default, Thin, Thick, Double, Thick-Thin, Triple, Dashed, Dotted, Rounded, Corners, Shadow) were
 * deleted and replaced by 7 decorative ones (Leaves, Laurel, Hearts, Flowers, Vine, Clovers, Lace).
 *
 * VARIATIONS = template (15) x page border (15) x colour (18). Border index 0 = None. A border paints a 24 pt paper
 * margin and draws the border inside it, over the template.
 *
 * No text is invented: in the PREVIEW an empty field shows a faint grey hint, in the PDF it is left out.
 * Status: written, NOT compiled by the author, not seen on a device.
 */
const val COVER_PAGE_W = 595
const val COVER_PAGE_H = 842

data class CoverMember(val name: String = "", val id: String = "")

data class CoverPageData(
    val universityName: String = "",
    val department: String = "",
    val title: String = "",
    val isGroup: Boolean = true,
    val groupName: String = "",
    val members: List<CoverMember> = emptyList(),
    val submittedTo: String = "",
    val submissionDate: String = ""
)

data class CoverTemplate(val name: String, val premium: Boolean = false)

val COVER_TEMPLATES = listOf(
    CoverTemplate("Gallery", true),
    CoverTemplate("Crest", true),
    CoverTemplate("Noir", true),
    CoverTemplate("Prism", true),
    CoverTemplate("Orbit", true),
    CoverTemplate("Aurora", true),
    CoverTemplate("Luxe", true),
    CoverTemplate("Sidebar"),
    CoverTemplate("Circles"),
    CoverTemplate("Corporate"),
    CoverTemplate("Watermark"),
    CoverTemplate("Geometric"),
    CoverTemplate("Ribbon"),
    CoverTemplate("Wave"),
    CoverTemplate("Stripe")
)

/** Border choices. Index 0 = no border. */
val COVER_BORDERS = listOf(
    "None", "Greek key", "Stars", "Zigzag", "Scallop", "Diamonds", "Checker", "Pearls",
    "Leaves", "Laurel", "Hearts", "Flowers", "Vine", "Clovers", "Lace"
)

data class CoverAccent(val name: String, val color: Int)

val COVER_ACCENTS = listOf(
    CoverAccent("Navy", Color.parseColor("#1E3A8A")),
    CoverAccent("Emerald", Color.parseColor("#047857")),
    CoverAccent("Crimson", Color.parseColor("#B91C1C")),
    CoverAccent("Purple", Color.parseColor("#6D28D9")),
    CoverAccent("Teal", Color.parseColor("#0E7490")),
    CoverAccent("Charcoal", Color.parseColor("#1F2937")),
    CoverAccent("Gold", Color.parseColor("#B45309")),
    CoverAccent("Rose", Color.parseColor("#BE185D")),
    CoverAccent("Forest", Color.parseColor("#166534")),
    CoverAccent("Sky", Color.parseColor("#0369A1")),
    CoverAccent("Midnight", Color.parseColor("#0B1B3F")),
    CoverAccent("Burgundy", Color.parseColor("#6B0F2A")),
    CoverAccent("Royal Blue", Color.parseColor("#1D4ED8")),
    CoverAccent("Bronze", Color.parseColor("#8B5E34")),
    CoverAccent("Plum", Color.parseColor("#581C87")),
    CoverAccent("Slate", Color.parseColor("#334155")),
    CoverAccent("Olive", Color.parseColor("#4D5D1F")),
    CoverAccent("Onyx", Color.parseColor("#0A0A0A"))
)

private val INK = Color.parseColor("#111827")
private val MUTED = Color.parseColor("#4B5563")
private val CREAM = Color.parseColor("#FFFBF2")
private val SOFT_GRAY = Color.parseColor("#F3F4F6")
private val GOLD = Color.parseColor("#D4AF37")
private val GOLD_LIGHT = Color.parseColor("#F3DE8A")
private val GOLD_DEEP = Color.parseColor("#8F6B1C")

private class Pal(val ink: Int, val muted: Int, val line: Int)

private val LIGHT_PAL = Pal(INK, MUTED, Color.argb(150, 120, 126, 140))
private val DARK_PAL = Pal(Color.WHITE, Color.argb(210, 255, 255, 255), Color.argb(140, 255, 255, 255))

// ---------------------------------------------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------------------------------------------

private fun prismFrom(accent: Int): Int = mix(accent, Color.BLACK, 0.62f)
private fun prismTo(accent: Int): Int = mix(accent, Color.BLACK, 0.12f)

/** Paper colour under the border margin, per template. */
private fun paperOf(t: Int, accent: Int): Int {
    return when (t) {
        0 -> accent
        1 -> CREAM
        2 -> prestigeBg(accent)
        else -> Color.WHITE
    }
}

/** Paper paint under the border margin (Prism is a gradient, the rest are flat colours). */
private fun paperPaintOf(t: Int, accent: Int): Paint {
    if (t == 3) {
        return gradientPaint(0f, 0f, COVER_PAGE_W.toFloat(), COVER_PAGE_H.toFloat(), prismFrom(accent), prismTo(accent))
    }
    return fillPaint(paperOf(t, accent))
}

/** Border colour, per template (gold on the dark and ornate ones). */
private fun borderColorOf(t: Int, accent: Int): Int {
    return when (t) {
        0, 1, 2, 6 -> GOLD
        3 -> GOLD_LIGHT
        else -> accent
    }
}

/** Paints one A4 page (595 x 842 points) on [c]. [preview] = true draws grey hints for empty fields. */
fun drawCoverPage(
    c: Canvas,
    data: CoverPageData,
    logo: Bitmap?,
    template: Int,
    accent: Int,
    preview: Boolean,
    border: Int
) {
    val d = resolve(data, preview)
    val t = if (template in COVER_TEMPLATES.indices) template else 0
    when (t) {
        1 -> tplCrest(c, d, logo, accent, preview)
        2 -> tplNoir(c, d, logo, accent, preview)
        3 -> tplPrism(c, d, logo, accent, preview)
        4 -> tplOrbit(c, d, logo, accent, preview)
        5 -> tplAurora(c, d, logo, accent, preview)
        6 -> tplLuxe(c, d, logo, accent, preview)
        7 -> tplSidebar(c, d, logo, accent, preview)
        8 -> tplCircles(c, d, logo, accent, preview)
        9 -> tplCorporate(c, d, logo, accent, preview)
        10 -> tplWatermark(c, d, logo, accent, preview)
        11 -> tplGeometric(c, d, logo, accent, preview)
        12 -> tplRibbon(c, d, logo, accent, preview)
        13 -> tplWave(c, d, logo, accent, preview)
        14 -> tplStripe(c, d, logo, accent, preview)
        else -> tplGallery(c, d, logo, accent, preview)
    }
    applyBorder(c, border, borderColorOf(t, accent), paperPaintOf(t, accent))
}

/** Renders the page to a Bitmap (scale 1.0 = 595 x 842 px). Used for the preview and the template thumbnails. */
fun renderCoverBitmap(
    data: CoverPageData,
    logo: Bitmap?,
    template: Int,
    accent: Int,
    scale: Float,
    border: Int
): Bitmap {
    val bmp = Bitmap.createBitmap(
        max(1, (COVER_PAGE_W * scale).toInt()),
        max(1, (COVER_PAGE_H * scale).toInt()),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    canvas.scale(scale, scale)
    drawCoverPage(canvas, data, logo, template, accent, true, border)
    return bmp
}

/** Writes a one-page A4 PDF to [out]. Empty fields are left out (no hints). The caller closes [out]. */
fun writeCoverPdf(out: OutputStream, data: CoverPageData, logo: Bitmap?, template: Int, accent: Int, border: Int) {
    val doc = PdfDocument()
    try {
        val info = PdfDocument.PageInfo.Builder(COVER_PAGE_W, COVER_PAGE_H, 1).create()
        val page = doc.startPage(info)
        drawCoverPage(page.canvas, data, logo, template, accent, false, border)
        doc.finishPage(page)
        doc.writeTo(out)
    } finally {
        doc.close()
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Resolved fields (value or grey hint)
// ---------------------------------------------------------------------------------------------------------------

private class Fld(val text: String, val hint: Boolean) {
    val blank: Boolean get() = text.isBlank()
    fun up(): Fld = Fld(text.uppercase(), hint)
}

private class Mem(val name: String, val id: String, val hint: Boolean)

private class Doc(
    val uni: Fld,
    val dept: Fld,
    val title: Fld,
    val group: Fld,
    val to: Fld,
    val date: Fld,
    val isGroup: Boolean,
    val members: List<Mem>
)

private fun fld(value: String, hint: String, preview: Boolean): Fld {
    val v = value.trim()
    return if (v.isNotEmpty()) Fld(v, false) else if (preview) Fld(hint, true) else Fld("", false)
}

private fun resolve(d: CoverPageData, preview: Boolean): Doc {
    val filled = d.members.filter { it.name.isNotBlank() || it.id.isNotBlank() }
    val used = if (d.isGroup) filled else filled.take(1)
    val mem: List<Mem> = if (used.isNotEmpty()) {
        used.map { Mem(it.name.trim(), it.id.trim(), false) }
    } else if (preview) {
        if (d.isGroup) {
            listOf(
                Mem("Member full name", "ID number", true),
                Mem("Member full name", "ID number", true),
                Mem("Member full name", "ID number", true)
            )
        } else {
            listOf(Mem("Student full name", "ID number", true))
        }
    } else {
        emptyList()
    }
    return Doc(
        uni = fld(d.universityName, "University name", preview),
        dept = fld(d.department, "Department", preview),
        title = fld(d.title, "Assignment or course title", preview),
        group = fld(d.groupName, "Group name or number", preview),
        to = fld(d.submittedTo, "Instructor name", preview),
        date = fld(d.submissionDate, "Submission date", preview),
        isGroup = d.isGroup,
        members = mem
    )
}

// ---------------------------------------------------------------------------------------------------------------
// Drawing helpers
// ---------------------------------------------------------------------------------------------------------------

private fun tp(
    size: Float,
    color: Int,
    bold: Boolean = false,
    serif: Boolean = false,
    italic: Boolean = false,
    spacing: Float = 0f,
    black: Boolean = false,
    light: Boolean = false
): TextPaint {
    val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    p.textSize = size
    p.color = color
    val family = if (black) "sans-serif-black" else if (light) "sans-serif-light" else if (serif) "serif" else "sans-serif"
    val style = if (bold && italic) Typeface.BOLD_ITALIC else if (bold) Typeface.BOLD else if (italic) Typeface.ITALIC else Typeface.NORMAL
    p.typeface = Typeface.create(family, style)
    p.letterSpacing = spacing
    return p
}

private fun fillPaint(color: Int): Paint {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.FILL
    p.color = color
    return p
}

private fun strokePaint(color: Int, width: Float): Paint {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.STROKE
    p.strokeWidth = width
    p.color = color
    return p
}

/** Gold foil: a fill paint with a shiny gold gradient running from (x0, y0) to (x1, y1). */
private fun goldPaint(x0: Float, y0: Float, x1: Float, y1: Float): Paint {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.FILL
    p.shader = LinearGradient(
        x0, y0, x1, y1,
        intArrayOf(GOLD_DEEP, GOLD_LIGHT, GOLD, GOLD_LIGHT, GOLD_DEEP),
        floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        Shader.TileMode.CLAMP
    )
    return p
}

/** A fill paint with a two colour gradient from (x0, y0) to (x1, y1). */
private fun gradientPaint(x0: Float, y0: Float, x1: Float, y1: Float, from: Int, to: Int): Paint {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.FILL
    p.shader = LinearGradient(x0, y0, x1, y1, from, to, Shader.TileMode.CLAMP)
    return p
}

private fun mix(a: Int, b: Int, t: Float): Int {
    val k = t.coerceIn(0f, 1f)
    return Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * k).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * k).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).toInt()
    )
}

private fun strokeRect(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, width: Float) {
    c.drawRect(l, t, r, b, strokePaint(color, width))
}

private fun hRule(c: Canvas, x1: Float, x2: Float, y: Float, color: Int, width: Float) {
    c.drawLine(x1, y, x2, y, strokePaint(color, width))
}

private fun withAlpha(color: Int, alpha: Int): Int {
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}

private fun darkTint(color: Int): Int {
    return Color.rgb(
        (Color.red(color) * 0.38f).toInt(),
        (Color.green(color) * 0.38f).toInt(),
        (Color.blue(color) * 0.38f).toInt()
    )
}

private fun prestigeBg(accent: Int): Int = mix(darkTint(accent), Color.BLACK, 0.4f)

private fun diamond(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
    val path = Path()
    path.moveTo(cx, cy - r)
    path.lineTo(cx + r, cy)
    path.lineTo(cx, cy + r)
    path.lineTo(cx - r, cy)
    path.close()
    c.drawPath(path, fillPaint(color))
}

private fun diamondRule(c: Canvas, cx: Float, y: Float, half: Float, color: Int) {
    hRule(c, cx - half, cx - 10f, y, color, 0.9f)
    hRule(c, cx + 10f, cx + half, y, color, 0.9f)
    diamond(c, cx, y, 4.5f, color)
}

private fun triangle(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
    val path = Path()
    path.moveTo(x1, y1)
    path.lineTo(x2, y2)
    path.lineTo(x3, y3)
    path.close()
    c.drawPath(path, fillPaint(color))
}

/** Rectangle with the four corners cut off at 45 degrees (k = size of the cut). */
private fun chamfer(l: Float, t: Float, r: Float, b: Float, k: Float): Path {
    val p = Path()
    p.moveTo(l + k, t)
    p.lineTo(r - k, t)
    p.lineTo(r, t + k)
    p.lineTo(r, b - k)
    p.lineTo(r - k, b)
    p.lineTo(l + k, b)
    p.lineTo(l, b - k)
    p.lineTo(l, t + k)
    p.close()
    return p
}

/** A tall window with a round top. */
private fun archPath(cx: Float, top: Float, half: Float, bottom: Float): Path {
    val p = Path()
    p.moveTo(cx - half, bottom)
    p.lineTo(cx - half, top + half)
    p.arcTo(RectF(cx - half, top, cx + half, top + 2f * half), 180f, 180f, false)
    p.lineTo(cx + half, bottom)
    p.close()
    return p
}

private fun makeLayout(text: String, p: TextPaint, w: Float, align: Layout.Alignment): StaticLayout {
    return StaticLayout.Builder.obtain(text, 0, text.length, p, max(w.toInt(), 1))
        .setAlignment(align)
        .setLineSpacing(0f, 1.12f)
        .setIncludePad(false)
        .build()
}

/** Height the text would take (0 for a blank field). */
private fun measureBlock(f: Fld, w: Float, p: TextPaint, align: Layout.Alignment): Float {
    if (f.blank) return 0f
    return makeLayout(f.text, p, w, align).height.toFloat()
}

/** Draws wrapped text at (x, y) inside width w; returns the y below the text. Blank fields draw nothing. */
private fun drawBlock(c: Canvas, f: Fld, x: Float, y: Float, w: Float, p: TextPaint, align: Layout.Alignment): Float {
    if (f.blank) return y
    val saved = p.alpha
    if (f.hint) p.alpha = 110
    val layout = makeLayout(f.text, p, w, align)
    c.save()
    c.translate(x, y)
    layout.draw(c)
    c.restore()
    p.alpha = saved
    return y + layout.height
}

/** One line of text inside a table cell (cut with an ellipsis when too long). */
private fun cell(c: Canvas, text: String, hint: Boolean, x: Float, baseline: Float, maxW: Float, p: TextPaint) {
    if (text.isEmpty()) return
    val saved = p.alpha
    if (hint) p.alpha = 110
    val cs = TextUtils.ellipsize(text, p, max(maxW, 1f), TextUtils.TruncateAt.END)
    c.drawText(cs, 0, cs.length, x, baseline, p)
    p.alpha = saved
}

/** Draws the logo scaled to fit the box. With no logo, the preview shows a dashed LOGO placeholder; the PDF shows nothing. */
private fun drawLogo(
    c: Canvas,
    logo: Bitmap?,
    left: Float,
    top: Float,
    boxW: Float,
    boxH: Float,
    preview: Boolean,
    hAlign: Float,
    alpha: Int = 255
): Boolean {
    if (logo == null) {
        if (!preview) return false
        val s = min(boxW, boxH)
        val l = left + (boxW - s) * hAlign
        val t = top + (boxH - s) / 2f
        val tone = Color.argb(120, 110, 118, 135)
        val edge = strokePaint(tone, 1.2f)
        edge.pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
        c.drawRoundRect(RectF(l, t, l + s, t + s), 12f, 12f, edge)
        val label = tp(s * 0.2f, tone, bold = true)
        c.drawText("LOGO", l + s / 2f - label.measureText("LOGO") / 2f, t + s / 2f + s * 0.07f, label)
        return true
    }
    val scale = min(boxW / logo.width, boxH / logo.height)
    val w = logo.width * scale
    val h = logo.height * scale
    val l = left + (boxW - w) * hAlign
    val t = top + (boxH - h) / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    paint.alpha = alpha
    c.drawBitmap(logo, null, RectF(l, t, l + w, t + h), paint)
    return true
}

/** White round badge with the logo inside (for logos on a colour background). Returns true when something was drawn. */
private fun drawBadgeLogo(c: Canvas, logo: Bitmap?, cx: Float, cy: Float, r: Float, preview: Boolean): Boolean {
    if (logo == null && !preview) return false
    c.drawCircle(cx, cy, r, fillPaint(Color.WHITE))
    drawLogo(c, logo, cx - r + r * 0.24f, cy - r + r * 0.24f, r * 1.52f, r * 1.52f, preview, 0.5f)
    return true
}

/** Members table. Returns the y below it. Rows shrink so the table never goes below [maxBottom]. */
private fun drawMembers(
    c: Canvas,
    mem: List<Mem>,
    left: Float,
    top: Float,
    width: Float,
    maxBottom: Float,
    accent: Int,
    filledHeader: Boolean,
    serif: Boolean,
    pal: Pal
): Float {
    if (mem.isEmpty()) return top
    val headerH = 22f
    val avail = maxBottom - top - headerH
    val rowH = min(26f, avail / mem.size).coerceAtLeast(13f)
    val fs = (rowH * 0.46f).coerceIn(7.5f, 11.5f)
    val noW = 34f
    val idW = 130f
    val nameW = width - noW - idW
    val line = strokePaint(pal.line, 0.7f)
    val head = tp(9.5f, if (filledHeader) Color.WHITE else accent, bold = true, serif = serif, spacing = 0.06f)
    val body = tp(fs, pal.ink, serif = serif)
    var y = top
    if (filledHeader) {
        c.drawRect(left, y, left + width, y + headerH, fillPaint(accent))
    }
    val hb = y + headerH / 2f + 3.4f
    cell(c, "No.", false, left + 8f, hb, noW - 10f, head)
    cell(c, "FULL NAME", false, left + noW + 8f, hb, nameW - 12f, head)
    cell(c, "ID NUMBER", false, left + noW + nameW + 8f, hb, idW - 12f, head)
    y += headerH
    if (!filledHeader) {
        hRule(c, left, left + width, y, accent, 1.2f)
    }
    mem.forEachIndexed { i, m ->
        val bottom = y + rowH
        val base = y + rowH / 2f + fs * 0.35f
        cell(c, (i + 1).toString(), false, left + 8f, base, noW - 10f, body)
        cell(c, m.name, m.hint, left + noW + 8f, base, nameW - 12f, body)
        cell(c, m.id, m.hint, left + noW + nameW + 8f, base, idW - 12f, body)
        c.drawLine(left, bottom, left + width, bottom, line)
        y = bottom
    }
    c.drawLine(left, top, left, y, line)
    c.drawLine(left + width, top, left + width, y, line)
    c.drawLine(left + noW, top + headerH, left + noW, y, line)
    c.drawLine(left + noW + nameW, top + headerH, left + noW + nameW, y, line)
    return y
}

/** Group block (group name, GROUP MEMBERS label, table) or individual block (STUDENT, name, ID). Returns the y below. */
private fun drawPeople(
    c: Canvas,
    d: Doc,
    x: Float,
    top: Float,
    w: Float,
    maxBottom: Float,
    accent: Int,
    serif: Boolean,
    filledHeader: Boolean,
    centered: Boolean,
    pal: Pal = LIGHT_PAL
): Float {
    val align = if (centered) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
    val label = tp(9.5f, pal.muted, bold = true, serif = serif, spacing = 0.14f)
    var y = top
    if (d.isGroup) {
        y = drawBlock(c, d.group, x, y, w, tp(16f, accent, bold = true, serif = serif), align)
        if (!d.group.blank) y += 10f
        if (d.members.isNotEmpty()) {
            y = drawBlock(c, Fld("GROUP MEMBERS", false), x, y, w, label, align)
            y += 7f
            y = drawMembers(c, d.members, x, y, w, maxBottom, accent, filledHeader, serif, pal)
        }
    } else {
        val m = d.members.firstOrNull()
        if (m != null) {
            y = drawBlock(c, Fld("STUDENT", false), x, y, w, label, align)
            y += 5f
            y = drawBlock(c, Fld(m.name, m.hint), x, y, w, tp(18f, pal.ink, bold = true, serif = serif), align)
            if (m.id.isNotEmpty()) {
                y += 4f
                val idText = if (m.hint) m.id else "ID: " + m.id
                y = drawBlock(c, Fld(idText, m.hint), x, y, w, tp(13f, pal.muted, serif = serif), align)
            }
        }
    }
    return y
}

/** SUBMITTED TO on the left, SUBMISSION DATE on the right (each only when it has a value). */
private fun drawTwoCol(
    c: Canvas,
    d: Doc,
    x: Float,
    top: Float,
    w: Float,
    labelColor: Int,
    valueColor: Int,
    serif: Boolean,
    valueSize: Float
) {
    val gap = 24f
    val cw = (w - gap) / 2f
    val normal = Layout.Alignment.ALIGN_NORMAL
    if (!d.to.blank) {
        var y = drawBlock(c, Fld("SUBMITTED TO", false), x, top, cw, tp(9f, labelColor, bold = true, serif = serif, spacing = 0.14f), normal)
        y += 5f
        drawBlock(c, d.to, x, y, cw, tp(valueSize, valueColor, bold = true, serif = serif), normal)
    }
    if (!d.date.blank) {
        val rx = x + cw + gap
        var y = drawBlock(c, Fld("SUBMISSION DATE", false), rx, top, cw, tp(9f, labelColor, bold = true, serif = serif, spacing = 0.14f), normal)
        y += 5f
        drawBlock(c, d.date, rx, y, cw, tp(valueSize, valueColor, bold = true, serif = serif), normal)
    }
}

/** A thin rule and the two-column footer below it (only when SUBMITTED TO or the date has a value). */
private fun footer(c: Canvas, d: Doc, x: Float, w: Float, ruleY: Float, ruleColor: Int, labelColor: Int, valueColor: Int, serif: Boolean) {
    if (d.to.blank && d.date.blank) return
    hRule(c, x, x + w, ruleY, ruleColor, 1f)
    drawTwoCol(c, d, x, ruleY + 16f, w, labelColor, valueColor, serif, 14f)
}

/** Centred serif footer under a diamond rule: SUBMITTED TO, then SUBMISSION DATE (each only when it has a value). */
private fun centeredFooter(c: Canvas, d: Doc, x: Float, w: Float, top: Float, rule: Int, labelColor: Int, valueColor: Int) {
    if (d.to.blank && d.date.blank) return
    val center = Layout.Alignment.ALIGN_CENTER
    var fy = top
    diamondRule(c, x + w / 2f, fy, 90f, rule)
    fy += 18f
    if (!d.to.blank) {
        fy = drawBlock(c, Fld("SUBMITTED TO", false), x, fy, w, tp(9f, labelColor, bold = true, serif = true, spacing = 0.14f), center)
        fy += 4f
        fy = drawBlock(c, d.to, x, fy, w, tp(14.5f, valueColor, serif = true, bold = true), center)
        fy += 14f
    }
    if (!d.date.blank) {
        fy = drawBlock(c, Fld("SUBMISSION DATE", false), x, fy, w, tp(9f, labelColor, bold = true, serif = true, spacing = 0.14f), center)
        fy += 4f
        drawBlock(c, d.date, x, fy, w, tp(14.5f, valueColor, serif = true, bold = true), center)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Page borders. The border sits in a 24 pt paper margin painted over the template.
// ---------------------------------------------------------------------------------------------------------------

private const val MARGIN = 24f

/** Runs [block] once for each of the four edges, in a rotated frame: x runs along the edge, y runs inward. */
private fun edges(c: Canvas, inset: Float, block: (Float) -> Unit) {
    val w = COVER_PAGE_W.toFloat()
    val h = COVER_PAGE_H.toFloat()
    val lh = w - 2f * inset
    val lv = h - 2f * inset
    c.save()
    c.translate(inset, inset)
    block(lh)
    c.restore()
    c.save()
    c.translate(w - inset, inset)
    c.rotate(90f)
    block(lv)
    c.restore()
    c.save()
    c.translate(w - inset, h - inset)
    c.rotate(180f)
    block(lh)
    c.restore()
    c.save()
    c.translate(inset, h - inset)
    c.rotate(270f)
    block(lv)
    c.restore()
}

private fun starPath(cx: Float, cy: Float, r: Float): Path {
    val path = Path()
    for (k in 0 until 10) {
        val rad = if (k % 2 == 0) r else r * 0.42f
        val ang = Math.toRadians((-90 + k * 36).toDouble())
        val px = cx + (rad * cos(ang)).toFloat()
        val py = cy + (rad * sin(ang)).toFloat()
        if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

/** A leaf growing from (x, y), pointing along [angle] degrees (0 = along the edge), [len] long and [wid] wide. */
private fun leaf(c: Canvas, x: Float, y: Float, angle: Float, len: Float, wid: Float, paint: Paint) {
    val p = Path()
    p.moveTo(0f, 0f)
    p.quadTo(len * 0.5f, -wid, len, 0f)
    p.quadTo(len * 0.5f, wid, 0f, 0f)
    p.close()
    c.save()
    c.translate(x, y)
    c.rotate(angle)
    c.drawPath(p, paint)
    c.restore()
}

/** A heart centred on (cx, cy); [s] is about a third of its width. */
private fun heart(c: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
    val p = Path()
    p.moveTo(cx, cy + s * 0.95f)
    p.cubicTo(cx - s * 1.8f, cy - s * 0.1f, cx - s * 0.7f, cy - s * 1.3f, cx, cy - s * 0.35f)
    p.cubicTo(cx + s * 0.7f, cy - s * 1.3f, cx + s * 1.8f, cy - s * 0.1f, cx, cy + s * 0.95f)
    p.close()
    c.drawPath(p, paint)
}

/** Five round petals around a centre dot. */
private fun flower(c: Canvas, cx: Float, cy: Float, r: Float, petal: Paint, center: Paint) {
    for (k in 0 until 5) {
        val ang = Math.toRadians((-90 + k * 72).toDouble())
        c.drawCircle(cx + (r * cos(ang)).toFloat(), cy + (r * sin(ang)).toFloat(), r * 0.62f, petal)
    }
    c.drawCircle(cx, cy, r * 0.45f, center)
}

/** Four round lobes (a quatrefoil). */
private fun clover(c: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    val o = r * 0.55f
    c.drawCircle(cx - o, cy, o, paint)
    c.drawCircle(cx + o, cy, o, paint)
    c.drawCircle(cx, cy - o, o, paint)
    c.drawCircle(cx, cy + o, o, paint)
}

private fun applyBorder(c: Canvas, style: Int, color: Int, paper: Paint) {
    if (style <= 0 || style >= COVER_BORDERS.size) return
    val w = COVER_PAGE_W.toFloat()
    val h = COVER_PAGE_H.toFloat()
    val m = MARGIN
    c.drawRect(0f, 0f, w, m, paper)
    c.drawRect(0f, h - m, w, h, paper)
    c.drawRect(0f, m, m, h - m, paper)
    c.drawRect(w - m, m, w, h - m, paper)
    val fill = fillPaint(color)
    val soft = fillPaint(mix(color, Color.WHITE, 0.72f))
    when (style) {
        1 -> {
            // Greek key (meander): a line along the edge with stepped hooks pointing inward
            edges(c, 6f) { len ->
                val u = 3.6f
                val cell = 4f * u
                val n = max(1, (len / cell).toInt())
                val s = len / n
                val line = strokePaint(color, 1.3f)
                c.drawLine(0f, 0f, len, 0f, line)
                for (i in 0 until n) {
                    val ox = i * s + (s - cell) / 2f
                    val path = Path()
                    path.moveTo(ox, 0f)
                    path.lineTo(ox, 3f * u)
                    path.lineTo(ox + 3f * u, 3f * u)
                    path.lineTo(ox + 3f * u, u)
                    path.lineTo(ox + u, u)
                    path.lineTo(ox + u, 2f * u)
                    path.lineTo(ox + 2f * u, 2f * u)
                    c.drawPath(path, line)
                }
            }
        }
        2 -> {
            edges(c, 4f) { len ->
                val n = max(1, (len / 16f).toInt())
                val s = len / n
                c.drawLine(0f, 9f, len, 9f, strokePaint(color, 0.7f))
                for (i in 0 until n) {
                    c.drawPath(starPath(i * s + s / 2f, 9f, 6f), fill)
                }
            }
        }
        3 -> {
            edges(c, 5f) { len ->
                val n = max(1, (len / 14f).toInt())
                val s = len / n
                c.drawLine(0f, 0f, len, 0f, strokePaint(color, 1.2f))
                for (i in 0 until n) {
                    val path = Path()
                    path.moveTo(i * s, 0f)
                    path.lineTo(i * s + s / 2f, 11f)
                    path.lineTo((i + 1) * s, 0f)
                    path.close()
                    c.drawPath(path, fill)
                }
            }
        }
        4 -> {
            edges(c, 6f) { len ->
                val n = max(1, (len / 16f).toInt())
                val s = len / n
                val arc = strokePaint(color, 1.8f)
                for (i in 0 until n) {
                    c.drawArc(RectF(i * s, -s / 2f, (i + 1) * s, s / 2f), 0f, 180f, false, arc)
                }
            }
        }
        5 -> {
            edges(c, 8f) { len ->
                val n = max(1, (len / 14f).toInt())
                val s = len / n
                c.drawLine(0f, 7f, len, 7f, strokePaint(color, 0.7f))
                for (i in 0 until n) {
                    diamond(c, i * s + s / 2f, 7f, 5f, color)
                }
            }
        }
        6 -> {
            edges(c, 8f) { len ->
                val sq = 6f
                val n = max(1, (len / sq).toInt())
                val s = len / n
                for (i in 0 until n) {
                    for (row in 0 until 2) {
                        if ((i + row) % 2 == 0) {
                            c.drawRect(i * s, row * sq, (i + 1) * s, (row + 1) * sq, fill)
                        }
                    }
                }
                c.drawLine(0f, 0f, len, 0f, strokePaint(color, 0.8f))
                c.drawLine(0f, 2f * sq, len, 2f * sq, strokePaint(color, 0.8f))
            }
        }
        7 -> {
            edges(c, 9f) { len ->
                val n = max(1, (len / 11f).toInt())
                val s = len / n
                c.drawLine(0f, 6f, len, 6f, strokePaint(color, 0.7f))
                for (i in 0 until n) {
                    c.drawCircle(i * s + s / 2f, 6f, 3.4f, fill)
                }
            }
        }
        8 -> {
            // Leaves: a fine stem with leaves sprouting on alternate sides
            edges(c, 5f) { len ->
                val n = max(1, (len / 15f).toInt())
                val s = len / n
                c.drawLine(0f, 9f, len, 9f, strokePaint(color, 0.8f))
                for (i in 0 until n) {
                    val a = if (i % 2 == 0) 38f else -38f
                    leaf(c, i * s + s / 2f - 6f, 9f, a, 12f, 3.8f, fill)
                }
            }
        }
        9 -> {
            // Laurel: pairs of leaves along the stem, like a wreath
            edges(c, 6f) { len ->
                val n = max(1, (len / 12f).toInt())
                val s = len / n
                c.drawLine(0f, 9f, len, 9f, strokePaint(color, 0.9f))
                for (i in 0 until n) {
                    val x = i * s + s / 2f - 4f
                    leaf(c, x, 9f, 36f, 11f, 3.4f, fill)
                    leaf(c, x, 9f, -36f, 11f, 3.4f, fill)
                }
            }
        }
        10 -> {
            // Hearts with small dots between them
            edges(c, 6f) { len ->
                val n = max(1, (len / 17f).toInt())
                val s = len / n
                for (i in 0 until n) {
                    heart(c, i * s + s / 2f, 10f, 4.3f, fill)
                    c.drawCircle(i * s, 10f, 1.3f, fill)
                }
                c.drawCircle(len, 10f, 1.3f, fill)
            }
        }
        11 -> {
            // Flowers on a fine line, with small dots between them
            edges(c, 6f) { len ->
                val n = max(1, (len / 22f).toInt())
                val s = len / n
                c.drawLine(0f, 10f, len, 10f, strokePaint(color, 0.6f))
                for (i in 0 until n) {
                    flower(c, i * s + s / 2f, 10f, 4.6f, fill, soft)
                    c.drawCircle(i * s, 10f, 1.4f, fill)
                }
                c.drawCircle(len, 10f, 1.4f, fill)
            }
        }
        12 -> {
            // Vine: a wavy stem with leaves on both sides
            edges(c, 5f) { len ->
                val n = max(1, (len / 26f).toInt())
                val s = len / n
                val stem = Path()
                var t = 0f
                stem.moveTo(0f, 9f)
                while (t <= len) {
                    stem.lineTo(t, 9f + 3.2f * sin(t / s * 2f * Math.PI.toFloat()))
                    t += 2f
                }
                c.drawPath(stem, strokePaint(color, 1.1f))
                for (i in 0 until n) {
                    leaf(c, (i + 0.25f) * s, 12.2f, 62f, 9f, 3f, fill)
                    leaf(c, (i + 0.75f) * s, 5.8f, -62f, 9f, 3f, fill)
                }
            }
        }
        13 -> {
            // Clovers on a fine line
            edges(c, 6f) { len ->
                val n = max(1, (len / 17f).toInt())
                val s = len / n
                c.drawLine(0f, 9f, len, 9f, strokePaint(color, 0.6f))
                for (i in 0 until n) {
                    clover(c, i * s + s / 2f, 9f, 3.8f, fill)
                }
            }
        }
        14 -> {
            // Lace: a fine line, hanging arcs, and a dot under each arc
            edges(c, 6f) { len ->
                val n = max(1, (len / 14f).toInt())
                val s = len / n
                val arc = strokePaint(color, 1.3f)
                c.drawLine(0f, 0f, len, 0f, strokePaint(color, 0.8f))
                for (i in 0 until n) {
                    c.drawArc(RectF(i * s, -s / 2f, (i + 1) * s, s / 2f), 0f, 180f, false, arc)
                    c.drawCircle(i * s + s / 2f, s / 2f + 4.5f, 1.7f, fill)
                }
            }
        }
    }
}

// ===============================================================================================================
// NEW premium templates (2026-10-23): Gallery, Crest, Noir, Prism, Orbit
// ===============================================================================================================

// ---------------------------------------------------------------------------------------------------------------
// Gallery: colour mat with a white panel, cut corners and a fine gold inner line, centred
// ---------------------------------------------------------------------------------------------------------------

private fun tplGallery(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val center = Layout.Alignment.ALIGN_CENTER
    c.drawColor(accent)
    c.drawPath(chamfer(30f, 30f, W - 30f, H - 30f, 28f), fillPaint(Color.WHITE))
    c.drawPath(chamfer(40f, 40f, W - 40f, H - 40f, 22f), strokePaint(GOLD, 1.2f))
    val x = 80f
    val w = W - 160f
    var y = 78f
    if (drawLogo(c, logo, x, y, w, 84f, preview, 0.5f)) y += 84f + 18f
    y = drawBlock(c, d.uni.up(), x, y, w, tp(18f, accent, bold = true, spacing = 0.14f), center)
    y += 10f
    c.drawRect(W / 2f - 36f, y, W / 2f + 36f, y + 2.4f, goldPaint(W / 2f - 36f, y, W / 2f + 36f, y))
    y += 16f
    y = drawBlock(c, d.dept, x, y, w, tp(13.5f, MUTED), center)
    y += 46f
    y = drawBlock(c, d.title, x, y, w, tp(29f, INK, bold = true), center)
    y += 28f
    drawPeople(c, d, x, y, w, H - 205f, accent, false, true, true)
    footer(c, d, x, w, H - 168f, GOLD_DEEP, MUTED, INK, false)
}

// ---------------------------------------------------------------------------------------------------------------
// Crest: cream paper, a colour arch window with the logo and a gold outline, serif title
// ---------------------------------------------------------------------------------------------------------------

private fun tplCrest(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val center = Layout.Alignment.ALIGN_CENTER
    val cx = W / 2f
    c.drawColor(CREAM)
    c.drawPath(archPath(cx, 46f, 100f, 262f), fillPaint(accent))
    c.drawPath(archPath(cx, 38f, 108f, 270f), strokePaint(GOLD, 1.6f))
    c.drawPath(archPath(cx, 54f, 92f, 262f), strokePaint(withAlpha(GOLD, 170), 0.7f))
    if (drawBadgeLogo(c, logo, cx, 150f, 46f, preview)) {
        c.drawCircle(cx, 150f, 51f, strokePaint(GOLD, 1.6f))
    }
    val x = 80f
    val w = W - 160f
    var y = 296f
    y = drawBlock(c, d.uni.up(), x, y, w, tp(18f, accent, bold = true, serif = true, spacing = 0.16f), center)
    y += 8f
    diamondRule(c, cx, y + 6f, 90f, GOLD)
    y += 24f
    y = drawBlock(c, d.dept, x, y, w, tp(13.5f, INK, serif = true, italic = true), center)
    y += 34f
    y = drawBlock(c, d.title, x, y, w, tp(27f, INK, bold = true, serif = true), center)
    y += 24f
    drawPeople(c, d, x, y, w, H - 215f, accent, true, false, true)
    centeredFooter(c, d, x, w, H - 178f, GOLD, MUTED, INK)
}

// ---------------------------------------------------------------------------------------------------------------
// Noir: near-black page, double gold frame with diamond corners, gold foil bar, white serif title
// ---------------------------------------------------------------------------------------------------------------

private fun tplNoir(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val center = Layout.Alignment.ALIGN_CENTER
    c.drawColor(prestigeBg(accent))
    strokeRect(c, 30f, 30f, W - 30f, H - 30f, GOLD, 1.2f)
    strokeRect(c, 36f, 36f, W - 36f, H - 36f, GOLD, 0.5f)
    diamond(c, 30f, 30f, 6f, GOLD)
    diamond(c, W - 30f, 30f, 6f, GOLD)
    diamond(c, 30f, H - 30f, 6f, GOLD)
    diamond(c, W - 30f, H - 30f, 6f, GOLD)
    c.drawRect(W / 2f - 60f, 28.5f, W / 2f + 60f, 31.5f, goldPaint(W / 2f - 60f, 0f, W / 2f + 60f, 0f))
    c.drawRect(W / 2f - 60f, H - 31.5f, W / 2f + 60f, H - 28.5f, goldPaint(W / 2f - 60f, 0f, W / 2f + 60f, 0f))
    val x = 80f
    val w = W - 160f
    var y = 78f
    if (drawBadgeLogo(c, logo, W / 2f, y + 46f, 44f, preview)) {
        c.drawCircle(W / 2f, y + 46f, 50f, strokePaint(GOLD, 1.6f))
        y += 112f
    }
    y = drawBlock(c, d.uni.up(), x, y, w, tp(18f, GOLD, bold = true, serif = true, spacing = 0.2f), center)
    y += 8f
    diamondRule(c, W / 2f, y + 6f, 90f, GOLD)
    y += 24f
    y = drawBlock(c, d.dept, x, y, w, tp(13.5f, Color.argb(220, 255, 255, 255), serif = true, italic = true), center)
    y += 42f
    y = drawBlock(c, d.title, x, y, w, tp(29f, Color.WHITE, bold = true, serif = true), center)
    y += 28f
    drawPeople(c, d, x, y, w, H - 215f, GOLD, true, false, true, DARK_PAL)
    centeredFooter(c, d, x, w, H - 178f, GOLD, GOLD, Color.WHITE)
}

// ---------------------------------------------------------------------------------------------------------------
// Prism: deep colour gradient page, soft glass circles and glass cards, white heavy title
// ---------------------------------------------------------------------------------------------------------------

private fun tplPrism(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val normal = Layout.Alignment.ALIGN_NORMAL
    c.drawRect(0f, 0f, W, H, gradientPaint(0f, 0f, W, H, prismFrom(accent), prismTo(accent)))
    c.drawCircle(W - 50f, 130f, 210f, fillPaint(Color.argb(22, 255, 255, 255)))
    c.drawCircle(W - 50f, 130f, 150f, strokePaint(Color.argb(40, 255, 255, 255), 1.2f))
    c.drawCircle(70f, H - 130f, 180f, fillPaint(Color.argb(16, 255, 255, 255)))
    c.drawRect(0f, 0f, W, 6f, goldPaint(0f, 0f, W, 0f))
    val x = 52f
    val w = W - 104f
    val hasBadge = drawBadgeLogo(c, logo, x + 34f, 92f, 34f, preview)
    val tx = if (hasBadge) x + 88f else x
    var ty = 72f
    ty = drawBlock(c, d.uni.up(), tx, ty, W - 52f - tx, tp(15f, Color.WHITE, bold = true, spacing = 0.08f), normal)
    ty += 5f
    drawBlock(c, d.dept, tx, ty, W - 52f - tx, tp(11.5f, Color.argb(225, 255, 255, 255)), normal)
    var y = 190f
    y = drawBlock(c, d.title, x, y, w, tp(34f, Color.WHITE, black = true), normal)
    y += 14f
    c.drawRect(x, y, x + 70f, y + 4f, goldPaint(x, y, x + 70f, y))
    y += 30f
    val bottom = H - 168f
    if (bottom - y > 80f) {
        val card = RectF(x, y, W - 52f, bottom)
        c.drawRoundRect(card, 20f, 20f, fillPaint(Color.argb(30, 255, 255, 255)))
        c.drawRoundRect(card, 20f, 20f, strokePaint(Color.argb(70, 255, 255, 255), 0.8f))
        drawPeople(c, d, x + 20f, y + 18f, w - 40f, bottom - 14f, GOLD_LIGHT, false, false, false, DARK_PAL)
    }
    if (!d.to.blank || !d.date.blank) {
        val card = RectF(x, H - 142f, W - 52f, H - 60f)
        c.drawRoundRect(card, 18f, 18f, fillPaint(Color.argb(30, 255, 255, 255)))
        c.drawRoundRect(card, 18f, 18f, strokePaint(Color.argb(70, 255, 255, 255), 0.8f))
        drawTwoCol(c, d, x + 22f, H - 122f, w - 44f, Color.argb(200, 255, 255, 255), Color.WHITE, false, 14f)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Orbit: white page, concentric rings and a solid disc in two corners, modern left-aligned type
// ---------------------------------------------------------------------------------------------------------------

private fun tplOrbit(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val normal = Layout.Alignment.ALIGN_NORMAL
    c.drawColor(Color.WHITE)
    var r = 70f
    while (r <= 300f) {
        c.drawCircle(W - 30f, 40f, r, strokePaint(withAlpha(accent, 70), 1.2f))
        r += 38f
    }
    c.drawCircle(W - 30f, 40f, 48f, fillPaint(withAlpha(accent, 235)))
    r = 60f
    while (r <= 230f) {
        c.drawCircle(30f, H - 30f, r, strokePaint(withAlpha(accent, 38), 1.2f))
        r += 34f
    }
    c.drawCircle(30f, H - 30f, 30f, fillPaint(withAlpha(GOLD, 220)))
    val x = 56f
    val w = W - 112f
    var y = 72f
    if (drawLogo(c, logo, x, y, 64f, 64f, preview, 0f)) y += 64f + 20f
    y = drawBlock(c, d.uni.up(), x, y, w - 110f, tp(17f, accent, bold = true, spacing = 0.1f), normal)
    y += 6f
    y = drawBlock(c, d.dept, x, y, w - 110f, tp(12.5f, MUTED), normal)
    y = max(y + 70f, 290f)
    y = drawBlock(c, d.title, x, y, w, tp(36f, INK, black = true), normal)
    y += 14f
    c.drawRect(x, y, x + 60f, y + 4f, fillPaint(accent))
    y += 32f
    drawPeople(c, d, x, y, w, H - 175f, accent, false, true, false)
    footer(c, d, x, w, H - 150f, accent, MUTED, INK, false)
}

// ---------------------------------------------------------------------------------------------------------------
// Kept templates (unchanged): Aurora, Luxe, Sidebar, Circles, Corporate, Watermark, Geometric, Ribbon, Wave, Stripe
// ---------------------------------------------------------------------------------------------------------------

private fun tplAurora(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val normal = Layout.Alignment.ALIGN_NORMAL
    c.drawColor(Color.WHITE)
    val hdr = Path()
    hdr.moveTo(0f, 0f)
    hdr.lineTo(W, 0f)
    hdr.lineTo(W, 300f)
    hdr.quadTo(W * 0.7f, 360f, W * 0.4f, 316f)
    hdr.quadTo(W * 0.18f, 284f, 0f, 332f)
    hdr.close()
    c.drawPath(hdr, gradientPaint(0f, 0f, W, 340f, mix(accent, Color.BLACK, 0.45f), mix(accent, Color.WHITE, 0.2f)))
    c.drawCircle(W - 40f, 60f, 130f, fillPaint(Color.argb(28, 255, 255, 255)))
    c.drawCircle(W - 120f, 190f, 80f, fillPaint(Color.argb(22, 255, 255, 255)))
    c.drawCircle(60f, 250f, 60f, fillPaint(Color.argb(18, 255, 255, 255)))
    c.drawRect(0f, 0f, W, 6f, goldPaint(0f, 0f, W, 0f))
    val x = 52f
    val w = W - 104f
    val hasBadge = drawBadgeLogo(c, logo, x + 34f, 86f, 34f, preview)
    val tx = if (hasBadge) x + 86f else x
    var ty = 62f
    ty = drawBlock(c, d.uni.up(), tx, ty, W - 52f - tx, tp(15f, Color.WHITE, bold = true, spacing = 0.08f), normal)
    ty += 5f
    drawBlock(c, d.dept, tx, ty, W - 52f - tx, tp(11.5f, Color.argb(225, 255, 255, 255)), normal)
    drawBlock(c, d.title, x, 170f, w, tp(32f, Color.WHITE, bold = true), normal)
    drawPeople(c, d, x, 376f, w, H - 190f, accent, false, true, false)
    if (!d.to.blank || !d.date.blank) {
        c.drawRoundRect(RectF(x + 2f, H - 144f + 5f, W - 52f + 2f, H - 56f + 5f), 16f, 16f, fillPaint(Color.argb(34, 0, 0, 0)))
        c.drawRoundRect(RectF(x, H - 144f, W - 52f, H - 56f), 16f, 16f, fillPaint(Color.WHITE))
        c.drawRoundRect(RectF(x, H - 144f, W - 52f, H - 56f), 16f, 16f, strokePaint(Color.argb(60, 120, 126, 140), 0.8f))
        c.drawRect(x + 14f, H - 144f, x + 70f, H - 141f, goldPaint(x + 14f, 0f, x + 70f, 0f))
        drawTwoCol(c, d, x + 24f, H - 124f, w - 48f, MUTED, INK, false, 14f)
    }
}

private fun tplLuxe(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val center = Layout.Alignment.ALIGN_CENTER
    c.drawColor(Color.WHITE)
    c.drawRect(0f, 0f, W, 12f, goldPaint(0f, 0f, W, 0f))
    c.drawRect(0f, H - 12f, W, H, goldPaint(0f, 0f, W, 0f))
    c.drawRect(0f, 12f, W, 15f, fillPaint(accent))
    c.drawRect(0f, H - 15f, W, H - 12f, fillPaint(accent))
    strokeRect(c, 40f, 40f, W - 40f, H - 40f, GOLD, 1.2f)
    strokeRect(c, 45f, 45f, W - 45f, H - 45f, accent, 0.5f)
    val x = 78f
    val w = W - 156f
    var y = 76f
    if (drawLogo(c, logo, x, y, w, 88f, preview, 0.5f)) y += 88f + 18f
    y = drawBlock(c, d.uni.up(), x, y, w, tp(18f, accent, bold = true, serif = true, spacing = 0.2f), center)
    y += 8f
    diamondRule(c, W / 2f, y + 6f, 80f, GOLD)
    y += 24f
    y = drawBlock(c, d.dept, x, y, w, tp(13f, MUTED, serif = true, italic = true), center)
    y += 44f
    y = drawBlock(c, d.title, x, y, w, tp(29f, INK, bold = true, serif = true), center)
    y += 28f
    drawPeople(c, d, x, y, w, H - 215f, accent, true, false, true)
    centeredFooter(c, d, x, w, H - 178f, GOLD, MUTED, INK)
}

private fun tplSidebar(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val normal = Layout.Alignment.ALIGN_NORMAL
    val center = Layout.Alignment.ALIGN_CENTER
    c.drawColor(Color.WHITE)
    val bw = 176f
    c.drawRect(0f, 0f, bw, H, fillPaint(accent))
    c.drawRect(bw - 6f, 0f, bw, H, fillPaint(Color.argb(45, 0, 0, 0)))

    var y = 60f
    if (drawBadgeLogo(c, logo, bw / 2f + 6f, y + 50f, 50f, preview)) {
        y += 100f + 24f
    }
    y = drawBlock(c, d.uni.up(), 30f, y, bw - 56f, tp(12.5f, Color.WHITE, bold = true, spacing = 0.06f), center)
    y += 10f
    hRule(c, bw / 2f - 16f, bw / 2f + 28f, y, Color.argb(190, 255, 255, 255), 1.2f)
    y += 12f
    drawBlock(c, d.dept, 30f, y, bw - 56f, tp(10.5f, Color.argb(230, 255, 255, 255)), center)
    if (!d.date.blank) {
        var dy = H - 96f
        dy = drawBlock(c, Fld("SUBMISSION DATE", false), 30f, dy, bw - 56f, tp(8.5f, Color.argb(210, 255, 255, 255), bold = true, spacing = 0.14f), center)
        dy += 4f
        drawBlock(c, d.date, 30f, dy, bw - 56f, tp(12.5f, Color.WHITE, bold = true), center)
    }

    val mx = bw + 34f
    val mw = W - mx - 40f
    var my = 90f
    c.drawRect(mx, my, mx + 50f, my + 4f, fillPaint(accent))
    my += 18f
    my = drawBlock(c, d.title, mx, my, mw, tp(27f, INK, bold = true), normal)
    my += 26f
    drawPeople(c, d, mx, my, mw, H - 160f, accent, false, false, false)
    if (!d.to.blank) {
        hRule(c, mx, mx + mw, H - 132f, accent, 1f)
        var fy = H - 116f
        fy = drawBlock(c, Fld("SUBMITTED TO", false), mx, fy, mw, tp(9f, MUTED, bold = true, spacing = 0.14f), normal)
        fy += 5f
        drawBlock(c, d.to, mx, fy, mw, tp(15f, INK, bold = true), normal)
    }
}

private fun tplCircles(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val normal = Layout.Alignment.ALIGN_NORMAL
    c.drawColor(Color.WHITE)
    c.drawCircle(W - 30f, 50f, 190f, fillPaint(withAlpha(accent, 38)))
    c.drawCircle(W - 110f, 10f, 115f, fillPaint(withAlpha(accent, 70)))
    c.drawCircle(50f, H - 30f, 150f, fillPaint(withAlpha(accent, 34)))
    c.drawCircle(150f, H + 10f, 90f, fillPaint(withAlpha(accent, 60)))
    val x = 56f
    val w = W - 112f
    var y = 64f
    if (drawLogo(c, logo, x, y, 72f, 72f, preview, 0f)) y += 72f + 20f
    y = drawBlock(c, d.uni.up(), x, y, w - 120f, tp(20f, accent, bold = true, spacing = 0.04f), normal)
    y += 6f
    y = drawBlock(c, d.dept, x, y, w - 120f, tp(13f, MUTED), normal)
    y = max(y + 60f, 290f)
    y = drawBlock(c, d.title, x, y, w, tp(32f, INK, bold = true), normal)
    y += 26f
    drawPeople(c, d, x, y, w, H - 175f, accent, false, true, false)
    if (!d.to.blank || !d.date.blank) {
        c.drawRoundRect(RectF(x, H - 150f, W - 56f, H - 64f), 12f, 12f, fillPaint(Color.argb(210, 255, 255, 255)))
        c.drawRect(x, H - 150f, x + 5f, H - 64f, fillPaint(accent))
        drawTwoCol(c, d, x + 24f, H - 132f, w - 44f, MUTED, INK, false, 14f)
    }
}

private fun tplCorporate(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val normal = Layout.Alignment.ALIGN_NORMAL
    c.drawColor(Color.WHITE)
    c.drawRect(0f, 0f, W, 10f, fillPaint(accent))
    val x = 50f
    val w = W - 100f
    var y = 40f
    val drew = drawLogo(c, logo, x, y, 60f, 60f, preview, 0f)
    val tx = if (drew) x + 76f else x
    var ty = y + 4f
    ty = drawBlock(c, d.uni.up(), tx, ty, W - 50f - tx, tp(14f, INK, bold = true, spacing = 0.05f), normal)
    ty += 4f
    ty = drawBlock(c, d.dept, tx, ty, W - 50f - tx, tp(11f, MUTED), normal)
    y = max(y + (if (drew) 60f else 0f), ty) + 14f
    hRule(c, x, x + w, y, accent, 2f)
    y += 70f
    val tpt = tp(26f, INK, bold = true)
    val th = measureBlock(d.title, w - 56f, tpt, normal)
    if (th > 0f) {
        strokeRect(c, x, y, x + w, y + th + 36f, accent, 1.4f)
        c.drawRect(x, y, x + 8f, y + th + 36f, fillPaint(accent))
        drawBlock(c, d.title, x + 28f, y + 18f, w - 56f, tpt, normal)
        y += th + 36f + 28f
    }
    drawPeople(c, d, x, y, w, H - 170f, accent, false, true, false)
    if (!d.to.blank || !d.date.blank) {
        c.drawRect(x, H - 132f, x + w, H - 54f, fillPaint(SOFT_GRAY))
        c.drawRect(x, H - 132f, x + 6f, H - 54f, fillPaint(accent))
        drawTwoCol(c, d, x + 26f, H - 114f, w - 44f, MUTED, INK, false, 14f)
    }
}

private fun tplWatermark(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val center = Layout.Alignment.ALIGN_CENTER
    c.drawColor(Color.WHITE)
    if (logo != null) {
        drawLogo(c, logo, 110f, 250f, 375f, 375f, false, 0.5f, 34)
    }
    c.drawRect(0f, 0f, W, 12f, fillPaint(accent))
    c.drawRect(0f, H - 12f, W, H, fillPaint(accent))
    val x = 70f
    val w = W - 140f
    var y = 64f
    y = drawBlock(c, d.uni.up(), x, y, w, tp(22f, accent, bold = true, spacing = 0.05f), center)
    y += 10f
    hRule(c, W / 2f - 20f, W / 2f + 20f, y, accent, 1.6f)
    y += 14f
    y = drawBlock(c, d.dept, x, y, w, tp(14f, INK), center)
    y += 70f
    y = drawBlock(c, d.title, x, y, w, tp(30f, INK, bold = true), center)
    y += 30f
    drawPeople(c, d, x, y, w, H - 185f, accent, false, false, true)
    footer(c, d, x, w, H - 166f, accent, MUTED, INK, false)
}

private fun tplGeometric(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val center = Layout.Alignment.ALIGN_CENTER
    c.drawColor(Color.WHITE)
    triangle(c, 0f, 0f, 300f, 0f, 0f, 300f, withAlpha(accent, 60))
    triangle(c, 0f, 0f, 210f, 0f, 0f, 210f, accent)
    triangle(c, W, H, W - 300f, H, W, H - 300f, withAlpha(accent, 60))
    triangle(c, W, H, W - 210f, H, W, H - 210f, accent)
    val x = 80f
    val w = W - 160f
    var y = 84f
    if (drawLogo(c, logo, x, y, w, 90f, preview, 0.5f)) y += 90f + 16f
    y = drawBlock(c, d.uni.up(), x, y, w, tp(19f, accent, bold = true, spacing = 0.06f), center)
    y += 8f
    y = drawBlock(c, d.dept, x, y, w, tp(13f, MUTED), center)
    y += 46f
    y = drawBlock(c, d.title, x, y, w, tp(29f, INK, bold = true), center)
    y += 28f
    drawPeople(c, d, x, y, w, H - 215f, accent, false, true, true)
    if (!d.to.blank || !d.date.blank) {
        drawTwoCol(c, d, 120f, H - 190f, W - 240f, MUTED, INK, false, 14f)
    }
}

private fun tplRibbon(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val normal = Layout.Alignment.ALIGN_NORMAL
    c.drawColor(Color.WHITE)
    val ribbon = Path()
    ribbon.moveTo(56f, 0f)
    ribbon.lineTo(134f, 0f)
    ribbon.lineTo(134f, 160f)
    ribbon.lineTo(95f, 132f)
    ribbon.lineTo(56f, 160f)
    ribbon.close()
    c.drawPath(ribbon, fillPaint(accent))
    drawBadgeLogo(c, logo, 95f, 66f, 30f, preview)
    val x = 56f
    val w = W - 112f
    val tx = 166f
    var ty = 58f
    ty = drawBlock(c, d.uni.up(), tx, ty, W - 50f - tx, tp(16f, accent, bold = true, spacing = 0.06f), normal)
    ty += 6f
    drawBlock(c, d.dept, tx, ty, W - 50f - tx, tp(12f, MUTED), normal)
    var y = 250f
    y = drawBlock(c, d.title, x, y, w, tp(32f, INK, bold = true), normal)
    y += 12f
    c.drawRect(x, y, x + 70f, y + 4f, fillPaint(accent))
    y += 30f
    drawPeople(c, d, x, y, w, H - 165f, accent, false, true, false)
    footer(c, d, x, w, H - 130f, accent, MUTED, INK, false)
}

private fun tplWave(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val normal = Layout.Alignment.ALIGN_NORMAL
    c.drawColor(Color.WHITE)
    val back = Path()
    back.moveTo(0f, 0f)
    back.lineTo(W, 0f)
    back.lineTo(W, 172f)
    back.quadTo(W * 0.75f, 238f, W * 0.5f, 184f)
    back.quadTo(W * 0.25f, 128f, 0f, 194f)
    back.close()
    c.drawPath(back, fillPaint(withAlpha(accent, 80)))
    val front = Path()
    front.moveTo(0f, 0f)
    front.lineTo(W, 0f)
    front.lineTo(W, 150f)
    front.quadTo(W * 0.75f, 216f, W * 0.5f, 162f)
    front.quadTo(W * 0.25f, 106f, 0f, 172f)
    front.close()
    c.drawPath(front, fillPaint(accent))
    val bottom = Path()
    bottom.moveTo(0f, H)
    bottom.lineTo(W, H)
    bottom.lineTo(W, H - 56f)
    bottom.quadTo(W * 0.75f, H - 112f, W * 0.5f, H - 62f)
    bottom.quadTo(W * 0.25f, H - 14f, 0f, H - 68f)
    bottom.close()
    c.drawPath(bottom, fillPaint(accent))
    val hasBadge = drawBadgeLogo(c, logo, 84f, 70f, 36f, preview)
    val tx = if (hasBadge) 136f else 50f
    var ty = 46f
    ty = drawBlock(c, d.uni.up(), tx, ty, W - 44f - tx, tp(16f, Color.WHITE, bold = true, spacing = 0.05f), normal)
    ty += 5f
    drawBlock(c, d.dept, tx, ty, W - 44f - tx, tp(11.5f, Color.argb(230, 255, 255, 255)), normal)
    val x = 56f
    val w = W - 112f
    var y = 262f
    y = drawBlock(c, d.title, x, y, w, tp(30f, INK, bold = true), normal)
    y += 12f
    c.drawRect(x, y, x + 60f, y + 4f, fillPaint(accent))
    y += 28f
    drawPeople(c, d, x, y, w, H - 205f, accent, false, true, false)
    if (!d.to.blank || !d.date.blank) {
        drawTwoCol(c, d, x, H - 188f, w, MUTED, INK, false, 14f)
    }
}

private fun tplStripe(c: Canvas, d: Doc, logo: Bitmap?, accent: Int, preview: Boolean) {
    val W = COVER_PAGE_W.toFloat()
    val H = COVER_PAGE_H.toFloat()
    val center = Layout.Alignment.ALIGN_CENTER
    c.drawColor(Color.WHITE)
    c.drawRect(0f, 0f, W, 14f, fillPaint(accent))
    c.drawRect(0f, 22f, W, 25f, fillPaint(accent))
    val x = 56f
    val w = W - 112f
    var y = 62f
    if (drawLogo(c, logo, x, y, w, 80f, preview, 0.5f)) y += 96f
    y = drawBlock(c, d.uni.up(), x, y, w, tp(17f, INK, bold = true, spacing = 0.12f), center)
    y += 6f
    y = drawBlock(c, d.dept, x, y, w, tp(12.5f, MUTED), center)
    y += 34f
    val tpt = tp(30f, Color.WHITE, bold = true)
    val th = measureBlock(d.title, w - 20f, tpt, center)
    if (th > 0f) {
        val bh = th + 56f
        c.drawRect(0f, y, W, y + bh, fillPaint(accent))
        drawBlock(c, d.title, x + 10f, y + 28f, w - 20f, tpt, center)
        y += bh + 28f
    }
    drawPeople(c, d, x, y, w, H - 170f, accent, false, true, true)
    footer(c, d, x, w, H - 130f, accent, MUTED, INK, false)
}
