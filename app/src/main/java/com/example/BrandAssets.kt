package com.example

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MarkBg = Color(0xFF000000)
private val MarkWhite = Color(0xFFFFFFFF)
private val MarkCyan = Color(0xFF00E5C8)

/**
 * Official Wisdom Tower mark drawn in Compose.
 * Rounded square, works fully offline.
 * [blinkDot] = cyan dot opacity loops 100% -> 0% -> 100% over 1s.
 */
@Composable
fun BrandMark(
    size: Dp = 34.dp,
    corner: Dp = 8.dp,
    blinkDot: Boolean = false,
) {
    val dotAlpha = if (blinkDot) {
        val infinite = rememberInfiniteTransition(label = "cyan-blink")
        val a by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dotAlpha",
        )
        a
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(MarkBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val barW = w * 0.14f
            val gap = w * 0.08f
            val leftH = h * 0.58f
            val rightH = h * 0.42f
            val barsBottom = h * 0.80f
            val leftX = w * 0.30f
            val rightX = leftX + barW + gap

            drawRoundRect(
                color = MarkWhite,
                topLeft = Offset(leftX, barsBottom - leftH),
                size = Size(barW, leftH),
                cornerRadius = CornerRadius(barW * 0.22f, barW * 0.22f),
            )
            drawRoundRect(
                color = MarkWhite,
                topLeft = Offset(rightX, barsBottom - rightH),
                size = Size(barW, rightH),
                cornerRadius = CornerRadius(barW * 0.22f, barW * 0.22f),
            )
            val dotR = barW * 0.52f
            drawCircle(
                color = MarkCyan.copy(alpha = dotAlpha.coerceIn(0f, 1f)),
                radius = dotR,
                center = Offset(leftX + barW / 2f, barsBottom - leftH - dotR * 0.05f),
            )
        }
    }
}
