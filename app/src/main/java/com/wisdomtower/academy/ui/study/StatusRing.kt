package com.wisdomtower.academy.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlin.math.roundToInt

// My Status V2 (S2): the ring is the arc drawing of DownloadGauge.kt (track circle + round-cap arc from -90 degrees, 250 ms
// ease), parameterised. Colours = the same day / night values as NativeStatusScreen.kt (night = old literals, day = tokens).
private fun ringDayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val RingEmerald: Color get() = ringDayNight(Color(0xFF34D399), WisdomAccentEmerald)
private val RingAmber: Color get() = ringDayNight(Color(0xFFFBBF24), WisdomAccentAmber)
private val RingRose: Color get() = ringDayNight(Color(0xFFFB7185), WisdomAccentRose)
private val RingMuted: Color get() = ringDayNight(Color(0xFF94A3B8), WisdomMuted)

/** Website HUD thresholds: >= 75 emerald, >= 50 amber, below rose, no data = muted. */
internal fun statusRingColor(pct: Int?): Color = when {
    pct == null -> RingMuted
    pct >= 75 -> RingEmerald
    pct >= 50 -> RingAmber
    else -> RingRose
}

/**
 * Circular gauge. [value] 0..1, or null = no data (muted empty ring with a dash, never a fake number, G4).
 * [centerText] overrides the default centre text (the rounded percent).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusRing(
    value: Float?,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 56.dp,
    strokeWidth: Dp = 5.dp,
    textSize: TextUnit = 14.sp,
    centerText: String? = null
) {
    val shown by animateFloatAsState(
        targetValue = (value ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(250),
        label = "statusRing"
    )
    val hasData = value != null
    val text = centerText ?: if (value == null) "—" else "${(value.coerceIn(0f, 1f) * 100f).roundToInt()}%"

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val half = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(half, half)
            drawArc(
                color = if (hasData) color.copy(alpha = 0.2f) else color.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )
            if (hasData && shown > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * shown,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Text(text = text, fontSize = textSize, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}
