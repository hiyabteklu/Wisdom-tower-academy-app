package com.wisdomtower.academy.ui.packages

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.wisdomtower.academy.R
import java.util.Random

/**
 * CODE COVERS (owner request 2026-11-27, docs/CODE_COVERS.md): the 16:9 Canva cover is drawn by code, so the app
 * needs no picture file for it (no APK size, new subjects cost nothing). Same layout as the Canva design, measured on
 * a 1280 x 720 frame: dark brushed-metal background with fine diagonal streaks, the white title with a cyan edge up-left
 * and a magenta edge down-right, and the gold band at the bottom with the Wisdom Tower mark.
 *
 * HOW IT IS USED (central, no screen needs editing): [OpenCardLayout] asks [isCodeCover] about its `image`. That is true for
 * (1) a `code-cover://<id>` address (ECE years and semesters, see [eceCoverLabel]) and (2) any bundled cover picture
 * `file:///android_asset/images/...` in the known cover folders, whose title is derived from the file name by
 * [codeCoverText]. Anything else (the Home welcome card, remote pictures, files, other folders) is loaded exactly as before.
 * Only the fonts already packed in res/font are used (Outfit Black for the title, Outfit Bold for the logo text).
 *
 * LABEL FORMAT: `TITLE` or `TITLE\nCAPTION`. The title wraps and shrinks to fit; the caption is one smaller line.
 */
const val CODE_COVER_SCHEME = "code-cover://"
private const val ASSET_IMAGES = "file:///android_asset/images/"

/** The title width is calibrated on this label, so every cover shares the same letter shape (like the Canva set). */
private const val CODE_COVER_REFERENCE_LABEL = "ECE 1ST YEAR"

private val ECE_ORDINALS = mapOf(1 to "1ST", 2 to "2ND", 3 to "3RD", 4 to "4TH", 5 to "5TH")
private val ECE_ID = Regex("""ece-y([1-5])(?:-sem-([12]))?""")
private val ECE_OLD_SEMESTER = Regex("""ece-sem-([12])""")
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "svg")

/** Hand-written titles where the file name alone would read badly. Everything else is the file name in capitals. */
private val PACKAGE_LABELS = mapOf(
    "grade-9-12" to "GRADE 9-12"
)

private val FRESHMAN_LABELS = mapOf(
    "applied-math-1" to "APPLIED MATHEMATICS I",
    "cpp-programming" to "C++ PROGRAMMING",
    "english-1" to "ENGLISH I",
    "english-2" to "ENGLISH II",
    "math-natural" to "MATHEMATICS (NATURAL)",
    "math-social" to "MATHEMATICS (SOCIAL)"
)

/** ECE Year 3 course covers: title, then the course code as the caption (names from the special-packages README). */
private val COURSE_LABELS = mapOf(
    "eceg3071" to "APPLIED ELECTRONICS II\nECEG3071",
    "econ1011" to "ECONOMICS\nECON1011",
    "eceg3051" to "ELECTROMAGNETIC FIELDS\nECEG3051",
    "eceg3081" to "SIGNALS AND SYSTEMS ANALYSIS\nECEG3081",
    "eceg3073" to "ELECTRICAL ENGINEERING LABORATORY III\nECEG3073",
    "eceg3101" to "OBJECT ORIENTED PROGRAMMING\nECEG3101",
    "eceg3061" to "COMPUTATIONAL METHODS\nECEG3061",
    "meng3052" to "ENGINEERING THERMODYNAMICS\nMENG3052",
    "eceg3082" to "NETWORK ANALYSIS AND SYNTHESIS\nECEG3082",
    "eceg3092" to "INTRODUCTION TO ELECTRICAL MACHINES\nECEG3092",
    "eceg3094" to "ELECTRICAL ENGINEERING LAB IV\nECEG3094",
    "eceg3102" to "DIGITAL LOGIC DESIGN\nECEG3102",
    "eceg3052" to "ELECTRICAL MATERIALS AND TECHNOLOGY\nECEG3052",
    "eceg3096" to "ELECTRICAL WORKSHOP PRACTICE II\nECEG3096"
)

fun codeCoverUri(id: String): String = CODE_COVER_SCHEME + id

/** Label of an ECE id: `ece-y1` is "ECE 1ST YEAR", `ece-y2-sem-1` is "ECE 2ND YEAR" with the caption "SEMESTER 1". */
private fun eceCoverLabel(id: String): String? {
    val match = ECE_ID.matchEntire(id) ?: return null
    val year = ECE_ORDINALS[match.groupValues[1].toInt()] ?: return null
    val semester = match.groupValues[2]
    return "ECE $year YEAR" + if (semester.isNotEmpty()) "\nSEMESTER $semester" else ""
}

private fun words(stem: String): String = stem.replace('-', ' ').replace('_', ' ').trim().uppercase()

/**
 * The label to draw for [model], or null when [model] is not a cover that is drawn by code (then it is loaded as a
 * picture exactly as before). The Home welcome card picture (`images/home/`, cached files, remote addresses) is never
 * matched: only the folders below are.
 */
fun codeCoverText(model: Any?): String? {
    if (model !is String) return null
    if (model.startsWith(CODE_COVER_SCHEME)) {
        val id = model.removePrefix(CODE_COVER_SCHEME)
        return eceCoverLabel(id) ?: words(id)
    }
    if (!model.startsWith(ASSET_IMAGES)) return null
    val parts = model.removePrefix(ASSET_IMAGES).substringBefore('?').split('/')
    val file = parts.last()
    if (file.substringAfterLast('.', "").lowercase() !in IMAGE_EXTENSIONS) return null
    val stem = file.substringBeforeLast('.').replace(Regex("_[0-9a-f]{6}$"), "")
    return when (parts.first()) {
        "packages" -> PACKAGE_LABELS[stem] ?: words(stem)
        "hubs" -> words(stem)
        "freshman" -> FRESHMAN_LABELS[stem] ?: words(stem)
        "academy" -> words(stem)
        "special-packages" -> specialPackageLabel(parts, stem)
        "web" -> if (parts.size >= 3) webLabel(parts[1], stem) else null
        else -> null
    }
}

private fun specialPackageLabel(parts: List<String>, stem: String): String {
    if (parts.size >= 3 && parts[1] == "courses") return COURSE_LABELS[stem] ?: words(stem)
    eceCoverLabel(stem)?.let { return it }
    ECE_OLD_SEMESTER.matchEntire(stem)?.let { return "ECE\nSEMESTER ${it.groupValues[1]}" }
    return when (stem) {
        "ece" -> "ELECTRICAL AND COMPUTER ENGINEERING"
        else -> words(stem)
    }
}

private fun webLabel(group: String, stem: String): String? = when {
    group.startsWith("grade-") ->
        words(stem.replace(Regex("^g[0-9]+-"), "")) + "\nGRADE " + group.removePrefix("grade-")
    group == "remedial" -> words(stem) + "\nREMEDIAL"
    group == "uat-hubs" -> words(stem) + "\nUAT"
    group == "entrance" -> when (stem) {
        "natural" -> "NATURAL SCIENCE\nENTRANCE"
        "social" -> "SOCIAL SCIENCE\nENTRANCE"
        else -> words(stem)
    }
    else -> null
}

fun isCodeCover(model: Any?): Boolean = codeCoverText(model) != null

/** The label of a model for which [isCodeCover] is true. */
fun codeCoverLabel(model: String): String = codeCoverText(model) ?: ""

private val CoverCyan = Color(0xFF00F0FF)
private val CoverMagenta = Color(0xFFFF1FD2)

@Composable
fun CodeCover(label: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val context = LocalContext.current
    val titlePaint = remember(context) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.outfit_black) }.getOrNull()
                ?: Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
    }
    val logoPaint = remember(context) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.outfit_bold) }.getOrNull()
                ?: Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
            color = Color.White.toArgb()
        }
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        clipRect {
            drawBrushedMetal(w, h)
            drawGoldBand(w, h)
            drawLogo(w, h, logoPaint)
            drawGlitchTitle(label, titlePaint, w, h)
        }
    }
}

/** Near-black base, a soft light from the top, and about 90 fine diagonal streaks (same every time, seeded). */
private fun DrawScope.drawBrushedMetal(w: Float, h: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF242424),
            0.45f to Color(0xFF0D0D0D),
            1f to Color(0xFF050505)
        ),
        size = Size(w, h)
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x26FFFFFF), Color.Transparent),
            center = Offset(w * 0.62f, 0f),
            radius = w * 0.75f
        ),
        size = Size(w, h)
    )
    val random = Random(11L)
    repeat(90) {
        val x0 = random.nextFloat() * (w + h) - h
        val alpha = 0.03f + random.nextFloat() * 0.07f
        val stroke = w * (0.0008f + random.nextFloat() * 0.0016f)
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(x0, 0f),
            end = Offset(x0 + h, h),
            strokeWidth = stroke
        )
    }
}

/** Gold band: bottom 17.4 percent (y 595 to 720 on the 1280 x 720 frame), light bronze on the left to dark on the right. */
private fun DrawScope.drawGoldBand(w: Float, h: Float) {
    val top = h * (595f / 720f)
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFF7D6546), Color(0xFF3D3022))),
        topLeft = Offset(0f, top),
        size = Size(w, h - top)
    )
}

/** Wisdom Tower mark (two bars, cyan dot) and the two-line wordmark, positions measured on the 1280 x 720 frame. */
private fun DrawScope.drawLogo(w: Float, h: Float, paint: Paint) {
    val s = w / 1280f
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(77f * s, 620f * s),
        size = Size(12f * s, 86f * s),
        cornerRadius = CornerRadius(2f * s)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(99f * s, 642f * s),
        size = Size(10f * s, 64f * s),
        cornerRadius = CornerRadius(2f * s)
    )
    drawCircle(color = CoverCyan, radius = 6.5f * s, center = Offset(104f * s, 627f * s))

    val bounds = Rect()
    paint.textSize = 100f
    paint.getTextBounds("E", 0, 1, bounds)
    val capRatio = bounds.height() / 100f
    paint.textSize = (36f * s) / capRatio
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("WISDOM", 124f * s, 655f * s, paint)
        canvas.nativeCanvas.drawText("TOWER", 124f * s, 705f * s, paint)
    }
}

/**
 * White title with a magenta copy down-right and a cyan copy up-left (offset 5.65 percent of the letter height).
 * A one-line title such as "ECE 1ST YEAR" is exactly the Canva geometry: centred at 48.5 percent of the width and
 * 42.5 percent of the height, cap height 20.5 percent of the height, squeezed sideways until that label fills 62 percent
 * of the width (Outfit Black is wider than the Canva font). Longer titles wrap into lines and shrink until they fit 80
 * percent of the width and 70 percent of the height. An optional caption (after a newline) is one smaller line below.
 */
private fun DrawScope.drawGlitchTitle(label: String, paint: Paint, w: Float, h: Float) {
    val bounds = Rect()
    paint.textScaleX = 1f
    paint.textSize = 100f
    paint.getTextBounds("E", 0, 1, bounds)
    val capRatio = bounds.height() / 100f
    val referenceCap = h * 0.205f
    paint.textSize = referenceCap / capRatio
    val squeeze = ((w * 0.62f) / paint.measureText(CODE_COVER_REFERENCE_LABEL)).coerceIn(0.4f, 1f)
    paint.textScaleX = squeeze

    fun widthAt(text: String, cap: Float): Float {
        paint.textSize = cap / capRatio
        return paint.measureText(text)
    }

    val maxWidth = w * 0.80f
    fun wrap(title: String, cap: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        for (word in title.split(' ').filter { it.isNotEmpty() }) {
            val trial = if (current.isEmpty()) word else "$current $word"
            if (current.isEmpty() || widthAt(trial, cap) <= maxWidth) {
                current = trial
            } else {
                lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    val parts = label.split('\n')
    val title = parts[0]
    val caption = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
    var captionCap = h * 0.085f
    if (caption != null) {
        val captionWidth = widthAt(caption, captionCap)
        if (captionWidth > maxWidth) captionCap *= maxWidth / captionWidth
    }
    val captionGap = h * 0.05f
    fun blockHeight(lineCount: Int, cap: Float): Float =
        lineCount * cap + (lineCount - 1) * cap * 0.35f + if (caption != null) captionGap + captionCap else 0f

    var cap = referenceCap
    var lines = wrap(title, cap)
    while (cap > h * 0.07f &&
        (blockHeight(lines.size, cap) > h * 0.70f || lines.any { widthAt(it, cap) > maxWidth })
    ) {
        cap *= 0.9f
        lines = wrap(title, cap)
    }

    val cx = w * 0.485f
    val top = h * 0.425f - blockHeight(lines.size, cap) / 2f
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        fun glitchLine(text: String, letterCap: Float, baseline: Float) {
            paint.textSize = letterCap / capRatio
            val d = letterCap * 0.0565f
            paint.color = CoverMagenta.toArgb()
            native.drawText(text, cx + d, baseline + d, paint)
            paint.color = CoverCyan.toArgb()
            native.drawText(text, cx - d, baseline - d, paint)
            paint.color = Color.White.toArgb()
            native.drawText(text, cx, baseline, paint)
        }
        var baseline = top + cap
        var lastBaseline = baseline
        for (line in lines) {
            glitchLine(line, cap, baseline)
            lastBaseline = baseline
            baseline += cap * 1.35f
        }
        if (caption != null) {
            glitchLine(caption, captionCap, lastBaseline + captionGap + captionCap)
        }
    }
}
