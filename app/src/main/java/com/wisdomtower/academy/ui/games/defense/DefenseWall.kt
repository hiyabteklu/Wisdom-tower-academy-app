package com.wisdomtower.academy.ui.games.defense

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.wisdomtower.academy.ui.theme.WisdomCyan
import kotlin.math.sin

/**
 * Wisdom Defense wall and floor grid (2026-10-24).
 * The old empire drawing had a tower with a crystal on the wall that looked like a gun but did nothing. The only gun is
 * now the cannon in the cockpit (DefenseCockpit.kt), so the wall is just a wall: shield arch, body, battlements and a
 * row of small lamps. The floor gets a perspective grid that scrolls toward the wall.
 * Pure drawing helpers, no state. [clock] is seconds since the game started (animation only).
 */

private fun Color.fa(f: Float): Color = copy(alpha = (alpha * f).coerceIn(0f, 1f))

/** Moving energy grid on the lane floor: crossing lines that speed up toward the wall, plus two guide lines. */
internal fun DrawScope.drawDefenseGrid(w: Float, h: Float, clock: Float) {
    val cx = w * 0.5f
    val topHalf = w * 0.11f
    val botHalf = w * 0.40f
    val n = 10
    val phase = (clock * 0.22f) % 1f
    for (i in 0 until n) {
        val t = (i + phase) / n
        val y = h * t * t
        val half = topHalf + (botHalf - topHalf) * (y / h)
        drawLine(
            WisdomCyan.fa(0.05f + 0.16f * t),
            Offset(cx - half, y),
            Offset(cx + half, y),
            strokeWidth = 1f + 2f * t
        )
    }
    for (k in floatArrayOf(-0.5f, 0.5f)) {
        drawLine(
            WisdomCyan.fa(0.10f),
            Offset(cx + k * topHalf * 2f, 0f),
            Offset(cx + k * botHalf * 2f, h),
            strokeWidth = 2f
        )
    }
}

/** Wall: shield arch, body, battlements and lamps. No tower, no crystal. */
internal fun DrawScope.drawDefenseWall(w: Float, h: Float, clock: Float) {
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
    drawArc(WisdomCyan.fa(0.05f + 0.05f * pulse), 180f, 180f, true, archTop, archSize)
    drawArc(WisdomCyan.fa(0.30f + 0.25f * pulse), 180f, 180f, false, archTop, archSize, style = Stroke(width = 3f))

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
    drawLine(WisdomCyan.fa(0.7f), Offset(left, wallTop), Offset(left + wallW, wallTop), strokeWidth = 3f)
    drawLine(
        Color.White.fa(0.08f),
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

    // lamps along the wall face
    for (i in 0 until 8) {
        val x = left + wallW * (i + 0.5f) / 8f
        val glow = 0.30f + 0.30f * (0.5f + 0.5f * sin(clock * 2f + i * 0.8f))
        drawCircle(WisdomCyan.fa(glow), 2.5f, Offset(x, wallTop + wallH * 0.72f))
    }
}
