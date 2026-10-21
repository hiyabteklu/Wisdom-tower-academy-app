package com.wisdomtower.academy.ui.games.defense

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.wisdomtower.academy.ui.theme.WisdomCyan
import kotlin.math.abs
import kotlin.math.sin

/**
 * Wisdom Defense art (docs/GAMES_FEEL_PLAN.md slice D2): Canvas monsters, perspective, lane, empire wall.
 * Pure drawing helpers, no state. [clock] is seconds since the game started (animation only).
 * Effects (particles, trail, shake, vignette) come in D3.
 */

private fun Color.a(f: Float): Color = copy(alpha = (alpha * f).coerceIn(0f, 1f))

/** Y of an enemy's feet. progress 0 = far edge, 1 = empire wall. */
internal fun defenseLaneY(h: Float, progress: Float): Float = h * (0.12f + progress * 0.7f)

/** X of an enemy: stays near the lane centre, spreads a little as it comes closer. */
internal fun defenseLaneX(w: Float, id: Int, progress: Float): Float =
    w * 0.5f + sin(id * 1.7f) * w * (0.03f + 0.07f * progress.coerceIn(0f, 1f))

/** Perspective: small far away (0.5), big at the wall (1.3). */
internal fun defenseScale(progress: Float): Float = 0.5f + 0.8f * progress.coerceIn(0f, 1f)

/** Base size unit of an enemy at this distance. */
internal fun defenseUnit(w: Float, h: Float, progress: Float): Float =
    minOf(w, h) * 0.065f * defenseScale(progress)

/** Where shots start (the empire crystal). */
internal fun defenseMuzzleY(h: Float): Float = h * 0.70f

/** Horizon glow, lane in perspective with moving centre dashes. */
internal fun DrawScope.drawDefenseField(w: Float, h: Float, clock: Float) {
    val cx = w * 0.5f
    val topHalf = w * 0.11f
    val botHalf = w * 0.40f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x3322D3EE), Color.Transparent),
            startY = 0f,
            endY = h * 0.45f
        ),
        size = Size(w, h * 0.45f)
    )
    val lane = Path().apply {
        moveTo(cx - topHalf, 0f)
        lineTo(cx + topHalf, 0f)
        lineTo(cx + botHalf, h)
        lineTo(cx - botHalf, h)
        close()
    }
    drawPath(
        lane,
        Brush.verticalGradient(
            colors = listOf(Color(0x1A22D3EE), Color(0x550E7490)),
            startY = 0f,
            endY = h
        )
    )
    drawLine(WisdomCyan.a(0.45f), Offset(cx - topHalf, 0f), Offset(cx - botHalf, h), strokeWidth = 3f)
    drawLine(WisdomCyan.a(0.45f), Offset(cx + topHalf, 0f), Offset(cx + botHalf, h), strokeWidth = 3f)
    val n = 8
    val phase = (clock * 0.35f) % 1f
    for (i in 0 until n) {
        val t0 = (i + phase) / n
        val t1 = t0 + (0.30f / n) * (0.5f + t0)
        drawLine(
            Color.White.a(0.10f + 0.18f * t0),
            Offset(cx, h * t0),
            Offset(cx, h * t1),
            strokeWidth = 2f + 4f * t0
        )
    }
}

/** Empire: battlement wall, tower with a glowing crystal, pulsing shield arch. */
internal fun DrawScope.drawDefenseEmpire(w: Float, h: Float, clock: Float) {
    val cx = w * 0.5f
    val wallTop = h * 0.84f
    val wallH = h * 0.10f
    val wallW = w * 0.64f
    val left = cx - wallW / 2f
    val pulse = 0.5f + 0.5f * sin(clock * 2.2f)

    // shield arch
    val archH = wallW * 0.5f
    val archTop = Offset(left - w * 0.02f, wallTop - archH / 2f)
    val archSize = Size(wallW + w * 0.04f, archH)
    drawArc(WisdomCyan.a(0.05f + 0.05f * pulse), 180f, 180f, true, archTop, archSize)
    drawArc(WisdomCyan.a(0.30f + 0.25f * pulse), 180f, 180f, false, archTop, archSize, style = Stroke(width = 3f))

    // wall body and top edge
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E3A5F), Color(0xFF0F1F36)),
            startY = wallTop,
            endY = wallTop + wallH
        ),
        topLeft = Offset(left, wallTop),
        size = Size(wallW, wallH),
        cornerRadius = CornerRadius(6f)
    )
    drawLine(WisdomCyan.a(0.7f), Offset(left, wallTop), Offset(left + wallW, wallTop), strokeWidth = 3f)
    drawLine(
        Color.White.a(0.08f),
        Offset(left, wallTop + wallH * 0.5f),
        Offset(left + wallW, wallTop + wallH * 0.5f),
        strokeWidth = 2f
    )

    // battlements
    val mw = wallW / 17f
    for (i in 0 until 9) {
        drawRoundRect(
            Color(0xFF1E3A5F),
            Offset(left + i * 2f * mw, wallTop - mw * 0.9f),
            Size(mw, mw * 0.9f),
            CornerRadius(3f)
        )
    }

    // tower
    val tw = wallW * 0.12f
    val th = wallH * 1.3f
    drawRoundRect(
        Color(0xFF16304F),
        Offset(cx - tw / 2f, wallTop - th),
        Size(tw, th),
        CornerRadius(4f)
    )
    drawRoundRect(
        Color(0xFF22507A),
        Offset(cx - tw * 0.65f, wallTop - th - tw * 0.15f),
        Size(tw * 1.3f, tw * 0.3f),
        CornerRadius(3f)
    )

    // crystal
    val cr = wallW * 0.035f
    val cy = wallTop - th - cr * 1.4f - 3f * sin(clock * 2.5f)
    val glowR = cr * 3.2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WisdomCyan.a(0.45f + 0.2f * pulse), Color.Transparent),
            center = Offset(cx, cy),
            radius = glowR
        ),
        radius = glowR,
        center = Offset(cx, cy)
    )
    val crystal = Path().apply {
        moveTo(cx, cy - cr * 1.3f)
        lineTo(cx + cr * 0.8f, cy)
        lineTo(cx, cy + cr * 1.3f)
        lineTo(cx - cr * 0.8f, cy)
        close()
    }
    drawPath(
        crystal,
        Brush.verticalGradient(
            colors = listOf(Color(0xFFA5F3FC), Color(0xFF0891B2)),
            startY = cy - cr * 1.3f,
            endY = cy + cr * 1.3f
        )
    )
}

/** Ring on the ground under the enemy the next shot will hit. */
internal fun DrawScope.drawDefenseTargetRing(x: Float, footY: Float, unit: Float, clock: Float) {
    val pulse = 0.5f + 0.5f * sin(clock * 6f)
    drawOval(
        WisdomCyan.a(0.5f + 0.35f * pulse),
        Offset(x - unit * 1.5f, footY - unit * 0.3f),
        Size(unit * 3f, unit * 0.6f),
        style = Stroke(width = 3f)
    )
}

/**
 * One enemy. [kind] 0 slime, 1 bat, 2 golem. [footY] is where it touches the ground.
 * [alpha] fades it (death), [flash] 0..1 paints a white hit flash.
 */
internal fun DrawScope.drawDefenseEnemy(
    kind: Int,
    x: Float,
    footY: Float,
    unit: Float,
    clock: Float,
    seed: Int,
    alpha: Float = 1f,
    flash: Float = 0f
) {
    val shadowHalf = if (kind == 1) 1.0f else 1.6f
    drawOval(
        Color.Black.a(0.35f * alpha),
        Offset(x - unit * shadowHalf, footY - unit * 0.12f),
        Size(unit * shadowHalf * 2f, unit * 0.34f)
    )
    when (kind) {
        1 -> drawBat(x, footY, unit, clock, seed, alpha)
        2 -> drawGolem(x, footY, unit, clock, seed, alpha)
        else -> drawSlime(x, footY, unit, clock, seed, alpha)
    }
    if (flash > 0f) {
        drawCircle(Color.White.a(0.55f * flash), unit * 1.6f, Offset(x, footY - unit * 1.2f))
    }
}

private fun DrawScope.drawSlime(x: Float, foot: Float, u: Float, clk: Float, seed: Int, al: Float) {
    val bob = sin(clk * 5f + seed)
    val bw = u * 1.15f
    val bh = u * 1.5f * (1f + 0.07f * bob)
    val top = foot - bh
    val body = Path().apply {
        moveTo(x - bw, foot)
        cubicTo(x - bw * 1.05f, top + bh * 0.15f, x - bw * 0.5f, top, x, top)
        cubicTo(x + bw * 0.5f, top, x + bw * 1.05f, top + bh * 0.15f, x + bw, foot)
        close()
    }
    drawPath(
        body,
        Brush.verticalGradient(
            colors = listOf(Color(0xFF4ADE80).a(al), Color(0xFF15803D).a(al)),
            startY = top,
            endY = foot
        )
    )
    drawOval(
        Color.White.a(0.35f * al),
        Offset(x - bw * 0.6f, top + bh * 0.12f),
        Size(bw * 0.55f, bh * 0.2f)
    )
    val ink = Color(0xFF052E16).a(al)
    for (s in floatArrayOf(-1f, 1f)) {
        val ex = x + s * bw * 0.38f
        val ey = top + bh * 0.48f
        drawCircle(Color.White.a(al), u * 0.24f, Offset(ex, ey))
        drawCircle(ink, u * 0.11f, Offset(ex, ey + u * 0.05f))
        drawLine(
            ink,
            Offset(ex + s * u * 0.32f, ey - u * 0.34f),
            Offset(ex - s * u * 0.2f, ey - u * 0.18f),
            strokeWidth = u * 0.09f
        )
    }
}

private fun DrawScope.drawBat(x: Float, foot: Float, u: Float, clk: Float, seed: Int, al: Float) {
    val cy = foot - u * 1.7f + u * 0.3f * sin(clk * 7f + seed)
    val flap = sin(clk * 14f + seed)
    val wingCol = Color(0xFF6D28D9).a(al)
    for (s in floatArrayOf(-1f, 1f)) {
        val wing = Path().apply {
            moveTo(x + s * u * 0.4f, cy - u * 0.1f)
            quadraticBezierTo(
                x + s * u * 1.4f, cy - u * 0.9f + flap * u * 0.5f,
                x + s * u * 2.1f, cy - u * 0.2f + flap * u * 0.8f
            )
            quadraticBezierTo(x + s * u * 1.5f, cy + u * 0.1f, x + s * u * 1.2f, cy + u * 0.6f)
            quadraticBezierTo(x + s * u * 0.85f, cy + u * 0.2f, x + s * u * 0.4f, cy + u * 0.45f)
            close()
        }
        drawPath(wing, wingCol)
    }
    val bodyTop = Color(0xFFA78BFA).a(al)
    val bodyBottom = Color(0xFF5B21B6).a(al)
    for (s in floatArrayOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(x + s * u * 0.15f, cy - u * 0.5f)
            lineTo(x + s * u * 0.55f, cy - u * 0.5f)
            lineTo(x + s * u * 0.45f, cy - u * 0.95f)
            close()
        }
        drawPath(ear, bodyBottom)
    }
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(bodyTop, bodyBottom),
            startY = cy - u * 0.6f,
            endY = cy + u * 0.6f
        ),
        radius = u * 0.6f,
        center = Offset(x, cy)
    )
    for (s in floatArrayOf(-1f, 1f)) {
        drawCircle(Color(0xFFF43F5E).a(0.35f * al), u * 0.24f, Offset(x + s * u * 0.24f, cy - u * 0.08f))
        drawCircle(Color(0xFFF43F5E).a(al), u * 0.13f, Offset(x + s * u * 0.24f, cy - u * 0.08f))
        val fang = Path().apply {
            moveTo(x + s * u * 0.2f, cy + u * 0.2f)
            lineTo(x + s * u * 0.08f, cy + u * 0.2f)
            lineTo(x + s * u * 0.14f, cy + u * 0.42f)
            close()
        }
        drawPath(fang, Color.White.a(al))
    }
}

private fun DrawScope.drawGolem(x0: Float, foot: Float, u: Float, clk: Float, seed: Int, al: Float) {
    val step = sin(clk * 3f + seed)
    val x = x0 + step * u * 0.05f
    val f = foot - abs(step) * u * 0.06f
    val stone = Color(0xFF64748B).a(al)
    val dark = Color(0xFF334155).a(al)
    val light = Color(0xFF94A3B8).a(al)
    val crack = Color(0xFF1E293B).a(0.8f * al)
    val bodyTop = f - u * 1.9f

    // legs
    drawRoundRect(dark, Offset(x - u * 0.75f, f - u * 0.5f), Size(u * 0.6f, u * 0.5f), CornerRadius(u * 0.1f))
    drawRoundRect(dark, Offset(x + u * 0.15f, f - u * 0.5f), Size(u * 0.6f, u * 0.5f), CornerRadius(u * 0.1f))
    // arms
    drawRoundRect(dark, Offset(x - u * 1.3f, bodyTop + u * 0.55f), Size(u * 0.4f, u * 0.85f), CornerRadius(u * 0.1f))
    drawRoundRect(dark, Offset(x + u * 0.9f, bodyTop + u * 0.55f), Size(u * 0.4f, u * 0.85f), CornerRadius(u * 0.1f))
    // body
    drawRoundRect(
        brush = Brush.verticalGradient(colors = listOf(light, stone), startY = bodyTop, endY = bodyTop + u * 1.45f),
        topLeft = Offset(x - u * 0.95f, bodyTop),
        size = Size(u * 1.9f, u * 1.45f),
        cornerRadius = CornerRadius(u * 0.2f)
    )
    // shoulders
    drawRoundRect(light, Offset(x - u * 1.35f, bodyTop - u * 0.05f), Size(u * 0.6f, u * 0.6f), CornerRadius(u * 0.15f))
    drawRoundRect(light, Offset(x + u * 0.75f, bodyTop - u * 0.05f), Size(u * 0.6f, u * 0.6f), CornerRadius(u * 0.15f))
    // head
    drawRoundRect(light, Offset(x - u * 0.5f, bodyTop - u * 0.65f), Size(u * 1.0f, u * 0.75f), CornerRadius(u * 0.15f))
    for (s in floatArrayOf(-1f, 1f)) {
        val c = Offset(x + s * u * 0.2f, bodyTop - u * 0.28f)
        drawCircle(Color(0xFFFB923C).a(0.35f * al), u * 0.2f, c)
        drawCircle(Color(0xFFFDBA74).a(al), u * 0.09f, c)
    }
    // cracks and core
    drawLine(crack, Offset(x - u * 0.2f, bodyTop + u * 0.2f), Offset(x + u * 0.05f, bodyTop + u * 0.7f), strokeWidth = u * 0.05f)
    drawLine(crack, Offset(x + u * 0.5f, bodyTop + u * 0.3f), Offset(x + u * 0.35f, bodyTop + u * 0.8f), strokeWidth = u * 0.05f)
    drawCircle(Color(0xFFFB923C).a(0.45f * al), u * 0.28f, Offset(x, bodyTop + u * 0.95f))
    drawCircle(Color(0xFFFDBA74).a(al), u * 0.12f, Offset(x, bodyTop + u * 0.95f))
}
