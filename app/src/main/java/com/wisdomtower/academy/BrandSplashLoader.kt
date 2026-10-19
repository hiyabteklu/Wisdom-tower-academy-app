package com.wisdomtower.academy

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wisdomtower.academy.ui.theme.AppThemeMode

/**
 * The splash loader (neon rings, spinning gear ticks, animated brand logo in the middle) as a public composable, so
 * any screen can use the same loading animation as the app start. It is an exact copy of `CustomCenteredLoader` /
 * `FuturisticGearRings` in `MainActivity.kt` (those stay private there and are untouched). No text, no percent, no
 * card: draw it on top of whatever background the screen has.
 *
 * D7a (docs/DAY_MODE_PLAN.md, owner order 2026-10-19): by day the bright neon cyan (00E5FF) is almost invisible on the
 * ivory page, so the rings, ticks and arcs use the deep day colours of the Ivory & Navy palette (teal 0B7285, sky 0369A1,
 * indigo 4338CA, the same values as `WisdomLightPalette`). At night every colour below is the old literal, so night is
 * unchanged. The logo in the middle ([BrandLoader], the packed GIF) is not changed by this slice.
 */
@Composable
fun BrandSplashLoader(modifier: Modifier = Modifier, size: Dp = 110.dp) {
    val brandSize = size * (64f / 110f)
    // Read while composing, so the loader follows the Day / Night switch.
    val isDark = AppThemeMode.isDark
    val ringHairline = if (isDark) Color(0x3300E5FF) else Color(0x330B7285)
    val tickEven = if (isDark) Color(0xFF00E5FF) else Color(0xFF0B7285)
    val tickOdd = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
    val outerArcColors = if (isDark) {
        listOf(Color(0x0000E5FF), Color(0x5500E5FF), Color(0xFF00E5FF), Color(0xFF38BDF8))
    } else {
        listOf(Color(0x000B7285), Color(0x550B7285), Color(0xFF0B7285), Color(0xFF0369A1))
    }
    val innerArcColors = if (isDark) {
        listOf(Color(0x00818CF8), Color(0x55818CF8), Color(0xFF818CF8), Color(0xFF00E5FF))
    } else {
        listOf(Color(0x004338CA), Color(0x554338CA), Color(0xFF4338CA), Color(0xFF0B7285))
    }
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val transition = rememberInfiniteTransition(label = "brandSplashLoader")
        val fastSpin by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fastSpin"
        )
        val gearSpin by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gearSpin"
        )
        val counterSpin by transition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(950, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "counterSpin"
        )

        val numTicks = 16
        val tickInnerRatio = 0.68f
        val tickOuterRatio = 0.88f
        val tickWidth = 3.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f - 3.dp.toPx()
            val innerRadius = outerRadius * 0.62f
            val rGearInner = outerRadius * tickInnerRatio
            val rGearOuter = outerRadius * tickOuterRatio
            val tickPx = tickWidth.toPx()

            drawCircle(
                color = ringHairline,
                radius = rGearInner,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            for (i in 0 until numTicks) {
                val tickAngle = gearSpin + (i * 360f / numTicks)
                val rad = Math.toRadians(tickAngle.toDouble())
                val cos = Math.cos(rad).toFloat()
                val sin = Math.sin(rad).toFloat()
                val p1 = Offset(centerOffset.x + rGearInner * cos, centerOffset.y + rGearInner * sin)
                val p2 = Offset(centerOffset.x + rGearOuter * cos, centerOffset.y + rGearOuter * sin)
                val tickColor = if (i % 2 == 0) tickEven else tickOdd
                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = tickPx,
                    cap = StrokeCap.Round
                )
            }

            drawArc(
                brush = Brush.sweepGradient(outerArcColors),
                startAngle = fastSpin,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(centerOffset.x - outerRadius, centerOffset.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(innerArcColors),
                startAngle = counterSpin,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(centerOffset.x - innerRadius, centerOffset.y - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        BrandLoader(size = brandSize, showCard = false)
    }
}
