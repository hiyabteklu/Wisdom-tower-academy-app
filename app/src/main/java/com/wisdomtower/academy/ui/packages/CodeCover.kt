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
 * HOW IT IS USED: a cover is a plain String in the form `code-cover://<id>`. [eceYearCover] returns it for the ECE years and
 * [OpenCardLayout] draws [CodeCover] instead of loading a picture when [isCodeCover] is true. Every other card is unchanged.
 * Only the fonts already packed in res/font are used (Outfit Black for the title, Outfit Bold for the logo text).
 */
const val CODE_COVER_SCHEME = "code-cover://"

private val CODE_COVER_LABELS = mapOf(
    "ece-y1" to "ECE 1ST YEAR",
    "ece-y2" to "ECE 2ND YEAR",
    "ece-y3" to "ECE 3RD YEAR",
    "ece-y4" to "ECE 4TH YEAR",
    "ece-y5" to "ECE 5TH YEAR"
)

/** The title width is calibrated on this label, so every year shares the same letter shape (like the Canva set). */
private const val CODE_COVER_REFERENCE_LABEL = "ECE 1ST YEAR"

fun codeCoverUri(id: String): String = CODE_COVER_SCHEME + id

fun isCodeCover(model: Any?): Boolean = model is String && model.startsWith(CODE_COVER_SCHEME)

fun codeCoverLabel(model: String): String = CODE_COVER_LABELS[model.removePrefix(CODE_COVER_SCHEME)] ?: ""

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
 * White title, centred at 48.5 percent of the width and 42.5 percent of the height, cap height 20.5 percent of the
 * height. Outfit Black is wider than the Canva font, so it is squeezed sideways until the reference label fills
 * 62 percent of the width; every label then uses the same squeeze. Magenta copy 0.65 percent of the width down-right,
 * cyan copy the same up-left, white on top.
 */
private fun DrawScope.drawGlitchTitle(label: String, paint: Paint, w: Float, h: Float) {
    val capTarget = h * 0.205f
    val bounds = Rect()
    paint.textScaleX = 1f
    paint.textSize = 100f
    paint.getTextBounds("E", 0, 1, bounds)
    val capRatio = bounds.height() / 100f
    paint.textSize = capTarget / capRatio
    val referenceWidth = paint.measureText(CODE_COVER_REFERENCE_LABEL)
    paint.textScaleX = ((w * 0.62f) / referenceWidth).coerceIn(0.4f, 1f)

    val cx = w * 0.485f
    val baseline = h * 0.425f + capTarget / 2f
    val d = w * 0.0065f
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        paint.color = CoverMagenta.toArgb()
        native.drawText(label, cx + d, baseline + d, paint)
        paint.color = CoverCyan.toArgb()
        native.drawText(label, cx - d, baseline - d, paint)
        paint.color = Color.White.toArgb()
        native.drawText(label, cx, baseline, paint)
    }
}
