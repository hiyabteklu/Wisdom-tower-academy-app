package com.wisdomtower.academy.ui.packages

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
 * A label may end with `\u0000<hub key>` (only the 8 hub covers in images/hubs): then the cover also gets the small blue
 * verified badge and two faint icons of that hub ([HUB_ICONS]).
 *
 * LOOK FIX 2026-11-27 (owner, after seeing it on the phone): the title letters had no spacing and the glitch edge made
 * them stick together, so the letters now have a little spacing and the cyan/magenta edge is much smaller; course codes
 * were removed (one simple word per subject, see [COURSE_LABELS]); hub covers got the badge and icons. Not compiled.
 */
const val CODE_COVER_SCHEME = "code-cover://"
private const val ASSET_IMAGES = "file:///android_asset/images/"
private const val HUB_MARK = '\u0000'

/** The title width is calibrated on this label, so every cover shares the same letter shape (like the Canva set). */
private const val CODE_COVER_REFERENCE_LABEL = "ECE 1ST YEAR"

/** Space between the title letters, in em (0.07 is about 7 percent of the letter size). */
private const val TITLE_LETTER_SPACING = 0.07f

/** Size of the cyan / magenta edge as a share of the letter height (was 0.0565, which made the letters merge). */
private const val GLITCH_OFFSET = 0.022f

private val ECE_ORDINALS = mapOf(1 to "1ST", 2 to "2ND", 3 to "3RD", 4 to "4TH", 5 to "5TH")
private val ECE_ID = Regex("""ece-y([1-5])(?:-sem-([12]))?""")
private val ECE_OLD_SEMESTER = Regex("""ece-sem-([12])""")
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "svg")

/** Hand-written titles where the file name alone would read badly. Everything else is the file name in capitals. */
private val PACKAGE_LABELS = mapOf(
    "grade-9-12" to "GRADE 9-12"
)

/** One simple word (or two) per subject: a cover helps students find the card, it is not a description. */
private val FRESHMAN_LABELS = mapOf(
    "applied-math-1" to "APPLIED MATH",
    "cpp-programming" to "C++",
    "english-1" to "ENGLISH I",
    "english-2" to "ENGLISH II",
    "math-natural" to "MATHEMATICS\nNATURAL",
    "math-social" to "MATHEMATICS\nSOCIAL",
    "emerging-technology" to "EMERGING TECH",
    "physical-fitness" to "FITNESS"
)

/** ECE Year 3 course covers: one simple word, NO course code. */
private val COURSE_LABELS = mapOf(
    "eceg3071" to "ELECTRONICS II",
    "econ1011" to "ECONOMICS",
    "eceg3051" to "ELECTROMAGNETICS",
    "eceg3081" to "SIGNALS",
    "eceg3073" to "LAB III",
    "eceg3101" to "OOP",
    "eceg3061" to "METHODS",
    "meng3052" to "THERMODYNAMICS",
    "eceg3082" to "NETWORKS",
    "eceg3092" to "MACHINES",
    "eceg3094" to "LAB IV",
    "eceg3102" to "DIGITAL LOGIC",
    "eceg3052" to "MATERIALS",
    "eceg3096" to "WORKSHOP II"
)

/** Two faint icons per hub, chosen to say what is inside the card (native Material icons, no emoji). */
private val HUB_ICONS: Map<String, Pair<ImageVector, ImageVector>> = mapOf(
    "books" to (Icons.AutoMirrored.Filled.MenuBook to Icons.Default.Description),
    "short-notes" to (Icons.Default.Edit to Icons.Default.Folder),
    "flashcards" to (Icons.Default.Style to Icons.Default.Psychology),
    "question-banks" to (Icons.Default.Quiz to Icons.Default.CheckCircle),
    "exams" to (Icons.Default.Timer to Icons.Default.EmojiEvents),
    "life-savers" to (Icons.Default.Favorite to Icons.Default.LocalFireDepartment),
    "references" to (Icons.Default.Bookmarks to Icons.Default.Link),
    "videos" to (Icons.Default.Videocam to Icons.Default.PlayArrow)
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
        "hubs" -> words(stem) + HUB_MARK + stem
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
        "ece" -> "ECE"
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
private val VerifiedBlue = Color(0xFF1D9BF0)

@Composable
fun CodeCover(label: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val context = LocalContext.current
    val text = label.substringBefore(HUB_MARK)
    val hub = label.substringAfter(HUB_MARK, "").ifEmpty { null }
    val titlePaint = remember(context) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.outfit_black) }.getOrNull()
                ?: Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            letterSpacing = TITLE_LETTER_SPACING
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
    val icons = HUB_ICONS[hub] ?: (Icons.Default.Folder to Icons.Default.Folder)
    val iconA = rememberVectorPainter(icons.first)
    val iconB = rememberVectorPainter(icons.second)
    val verified = rememberVectorPainter(Icons.Default.Verified)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        clipRect {
            drawBrushedMetal(w, h)
            if (hub != null && hub in HUB_ICONS) drawHubIcons(w, h, hub, iconA, iconB)
            drawGoldBand(w, h)
            drawLogo(w, h, logoPaint)
            drawGlitchTitle(text, titlePaint, w, h)
            if (hub != null) drawVerifiedBadge(w, h, verified)
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

/**
 * Two faint white icons of the hub, one on each side of the title, slightly different size, height and tilt for every hub
 * (seeded by the hub name, so a card always looks the same). They stay at about 13 percent opacity behind the title.
 */
private fun DrawScope.drawHubIcons(w: Float, h: Float, hub: String, a: Painter, b: Painter) {
    val random = Random(hub.hashCode().toLong())
    val firstOnLeft = random.nextBoolean()
    fun place(painter: Painter, onLeft: Boolean) {
        val s = h * (0.26f + random.nextFloat() * 0.08f)
        val cx = w * if (onLeft) 0.12f + random.nextFloat() * 0.10f else 0.78f + random.nextFloat() * 0.10f
        val cy = h * (0.34f + random.nextFloat() * 0.28f)
        val degrees = random.nextFloat() * 50f - 25f
        rotate(degrees, pivot = Offset(cx, cy)) {
            translate(cx - s / 2f, cy - s / 2f) {
                with(painter) {
                    draw(size = Size(s, s), colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.13f)))
                }
            }
        }
    }
    place(a, firstOnLeft)
    place(b, !firstOnLeft)
}

/** Small blue verified seal in the top-right corner of a hub cover. */
private fun DrawScope.drawVerifiedBadge(w: Float, h: Float, painter: Painter) {
    val s = h * 0.14f
    translate(w - s - w * 0.04f, h * 0.06f) {
        with(painter) {
            draw(size = Size(s, s), colorFilter = ColorFilter.tint(VerifiedBlue))
        }
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
 * White title with a small magenta copy down-right and cyan copy up-left (offset [GLITCH_OFFSET] of the letter height),
 * letters spaced by [TITLE_LETTER_SPACING]. A one-line title such as "ECE 1ST YEAR" is centred at 48.5 percent of the width
 * and 42.5 percent of the height, cap height 20.5 percent of the height, squeezed sideways until that label fills 62 percent
 * of the width (Outfit Black is wider than the Canva font). Longer titles wrap into lines and shrink until they fit 80
 * percent of the width and 70 percent of the height. An optional caption (after a newline) is one smaller line below.
 */
private fun DrawScope.drawGlitchTitle(label: String, paint: Paint, w: Float, h: Float) {
    val bounds = Rect()
    paint.letterSpacing = TITLE_LETTER_SPACING
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
            val d = letterCap * GLITCH_OFFSET
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
