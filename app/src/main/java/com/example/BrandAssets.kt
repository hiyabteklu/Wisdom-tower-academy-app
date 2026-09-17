package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MarkBg = Color(0xFF0A0A0A)
private val MarkWhite = Color(0xFFFFFFFF)
private val MarkCyan = Color(0xFF00E5FF)

/**
 * Official-style brand mark drawn in Compose.
 * Rounded square, works fully offline, never depends on network.
 */
@Composable
fun BrandMark(
    size: Dp = 34.dp,
    corner: Dp = 8.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(MarkBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            // Two vertical bars (left taller) — matches Wisdom Tower icon
            val barW = w * 0.12f
            val gap = w * 0.07f
            val leftH = h * 0.55f
            val rightH = h * 0.40f
            val barsBottom = h * 0.78f
            val leftX = w * 0.28f
            val rightX = leftX + barW + gap

            drawRoundRect(
                color = MarkWhite,
                topLeft = Offset(leftX, barsBottom - leftH),
                size = Size(barW, leftH),
                cornerRadius = CornerRadius(barW * 0.25f, barW * 0.25f),
            )
            drawRoundRect(
                color = MarkWhite,
                topLeft = Offset(rightX, barsBottom - rightH),
                size = Size(barW, rightH),
                cornerRadius = CornerRadius(barW * 0.25f, barW * 0.25f),
            )
            // Cyan dot on top of the right bar
            val dotR = barW * 0.55f
            drawCircle(
                color = MarkCyan,
                radius = dotR,
                center = Offset(rightX + barW / 2f, barsBottom - rightH - dotR * 0.15f),
            )
        }
    }
}
