package com.wisdomtower.academy.ui.games.defense

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.wisdomtower.academy.data.games.GameLevel
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Wisdom Defense atmosphere (docs/GAMES_COOL_PLAN.md slice W1). Pure drawing helpers, no state, no new dependencies.
 * Drawn BEFORE the lane and the enemies, so everything else stays on top. Same [clock] (seconds) as DefenseArt.
 *
 * - [defenseAtmosphereTint]: Easy teal, Medium violet, Hard ember red.
 * - [drawDefenseAtmosphere]: two slow nebula glows and layered stars drifting toward the wall (parallax: far stars slow
 *   and small, near stars faster and bigger). [lowEnd] uses fewer stars; [reduceMotion] freezes the drift and twinkle.
 * - Low-contrast fortress silhouettes sit at the horizon edges, with restrained window lights and a slow parallax drift.
 * - [drawDefenseLaneGlow]: soft glowing edges of the lane in the level colour (same lane shape as drawDefenseField:
 *   top half-width 0.11 w, bottom half-width 0.40 w, centred).
 *
 * STATUS: [~] written, NOT compiled, NOT seen on a phone. All numbers are proposals; tune after a phone test.
 */

private fun Color.al(f: Float): Color = copy(alpha = (alpha * f).coerceIn(0f, 1f))

internal fun defenseAtmosphereTint(level: GameLevel): Color = when (level) {
    GameLevel.EASY -> Color(0xFF14B8A6)
    GameLevel.MEDIUM -> Color(0xFF8B5CF6)
    GameLevel.HARD -> Color(0xFFEF4444)
}

/** Stable pseudo-random number 0..1 from an index (no Random state, so stars never jump between frames). */
private fun starRand(i: Int, salt: Int): Float {
    val x = sin(i * 12.9898f + salt * 78.233f) * 43758.547f
    return x - floor(x)
}

internal fun DrawScope.drawDefenseAtmosphere(
    w: Float,
    h: Float,
    clock: Float,
    tint: Color,
    lowEnd: Boolean,
    reduceMotion: Boolean
) {
    val drift = if (reduceMotion) 0f else clock

    // Nebula glows
    val c1 = Offset(w * (0.25f + 0.05f * sin(drift * 0.15f)), h * 0.30f)
    val r1 = w * 0.70f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(tint.al(0.22f), Color.Transparent),
            center = c1,
            radius = r1
        ),
        radius = r1,
        center = c1
    )
    val c2 = Offset(w * (0.78f + 0.05f * cos(drift * 0.12f)), h * 0.62f)
    val r2 = w * 0.60f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(tint.al(0.16f), Color.Transparent),
            center = c2,
            radius = r2
        ),
        radius = r2,
        center = c2
    )

    // Distant fortress skyline: low-contrast silhouettes give the lane a world to sit inside.
    // Keep them on the outer edges and below the glow so the question and enemy silhouettes stay dominant.
    val skylineShift = if (reduceMotion) 0f else drift * 0.006f
    for (side in 0..1) {
        val sign = if (side == 0) 1f else -1f
        for (i in 0..4) {
            val seed = side * 11 + i
            val xBase = if (side == 0) w * (0.015f + i * 0.052f) else w * (0.985f - i * 0.052f)
            val x = xBase + sign * skylineShift * w * (i + 1)
            val bw = w * (0.035f + starRand(seed, 7) * 0.025f)
            val bh = h * (0.07f + starRand(seed, 8) * 0.12f)
            val baseY = h * (0.39f + starRand(seed, 9) * 0.07f)
            val body = Color(0xFF101C31).copy(alpha = 0.50f + starRand(seed, 10) * 0.16f)
            drawRect(body, Offset(x - bw / 2f, baseY - bh), Size(bw, bh))
            // Small stepped roof makes the silhouettes read as fortress structures, not random blocks.
            drawRect(body, Offset(x - bw * 0.34f, baseY - bh - bw * 0.12f), Size(bw * 0.68f, bw * 0.12f))
            val windowColor = tint.copy(alpha = if (reduceMotion) 0.20f else 0.14f + 0.08f * (0.5f + 0.5f * sin(clock * 0.7f + seed)))
            if (bw > w * 0.04f) {
                drawRect(windowColor, Offset(x - bw * 0.18f, baseY - bh * 0.65f), Size(bw * 0.12f, bh * 0.10f))
                drawRect(windowColor, Offset(x + bw * 0.06f, baseY - bh * 0.65f), Size(bw * 0.12f, bh * 0.10f))
            }
        }
    }

    // Stars in 3 layers
    val count = if (lowEnd) 18 else 40
    val speeds = floatArrayOf(0.012f, 0.025f, 0.045f)
    for (i in 0 until count) {
        val layer = i % 3
        val x = starRand(i, 1) * w
        val y = ((starRand(i, 2) + drift * speeds[layer]) % 1f) * h
        val r = w * 0.0022f * (1f + layer * 0.7f)
        val twinkle = if (reduceMotion) 0f else 0.15f * sin(clock * 2f + i)
        val base = 0.25f + 0.2f * layer + twinkle
        val col = if (i % 5 == 0) tint else Color.White
        drawCircle(col.al(base), r, Offset(x, y))
    }
}

internal fun DrawScope.drawDefenseLaneGlow(w: Float, h: Float, clock: Float, tint: Color, reduceMotion: Boolean) {
    val cx = w * 0.5f
    val topHalf = w * 0.11f
    val botHalf = w * 0.40f
    val pulse = if (reduceMotion) 0.5f else 0.5f + 0.5f * sin(clock * 1.6f)
    val a = 0.10f + 0.10f * pulse
    drawLine(tint.al(a), Offset(cx - topHalf, 0f), Offset(cx - botHalf, h), strokeWidth = 14f)
    drawLine(tint.al(a), Offset(cx + topHalf, 0f), Offset(cx + botHalf, h), strokeWidth = 14f)
    drawLine(tint.al(a * 0.6f), Offset(cx - topHalf, 0f), Offset(cx - botHalf, h), strokeWidth = 7f)
    drawLine(tint.al(a * 0.6f), Offset(cx + topHalf, 0f), Offset(cx + botHalf, h), strokeWidth = 7f)
}
