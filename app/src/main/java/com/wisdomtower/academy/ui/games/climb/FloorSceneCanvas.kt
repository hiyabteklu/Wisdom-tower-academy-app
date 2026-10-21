package com.wisdomtower.academy.ui.games.climb

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.wisdomtower.academy.ui.games.common.GameTheme
import kotlin.math.sin

@Composable
fun FloorSceneCanvas(
    totalRungs: Int,
    currentRung: Int,
    phase: Float,
    correctFlash: Boolean,
    modifier: Modifier = Modifier
) {
    val rungs = totalRungs.coerceAtLeast(1)
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFF0E1624))
        drawRect(Color(0xFF1A2438), Offset(w * 0.1f, h * 0.05f), Size(w * 0.8f, h * 0.9f))
        val left = w * 0.35f
        val right = w * 0.65f
        val top = h * 0.08f
        val bottom = h * 0.92f
        drawLine(Color(0xFF8B6914), Offset(left, top), Offset(left, bottom), strokeWidth = 6f)
        drawLine(Color(0xFF8B6914), Offset(right, top), Offset(right, bottom), strokeWidth = 6f)
        val step = (bottom - top) / rungs
        for (i in 0 until rungs) {
            val y = bottom - step * (i + 0.5f)
            val filled = i < currentRung
            drawLine(
                if (filled) GameTheme.gold else Color(0xFF6B5A3A),
                Offset(left, y),
                Offset(right, y),
                strokeWidth = 5f
            )
        }
        val flicker = 0.7f + 0.3f * sin(phase * 6f)
        drawCircle(Color(0x88FF9800).copy(alpha = flicker), 12f, Offset(w * 0.18f, h * 0.3f))
        drawCircle(Color(0x88FF9800).copy(alpha = flicker), 12f, Offset(w * 0.82f, h * 0.3f))
        val owlRung = currentRung.coerceIn(0, rungs)
        val owlY = if (owlRung <= 0) bottom - 20f else bottom - step * (owlRung - 0.15f)
        val owlX = (left + right) / 2f
        OwlPainter.draw(
            this,
            center = Offset(owlX, owlY),
            sizePx = 48f,
            phase = phase,
            flap = if (correctFlash) 0.8f else 0.1f * sin(phase * 2f)
        )
        if (currentRung >= rungs) {
            drawRoundRect(
                GameTheme.gold.copy(alpha = 0.9f),
                Offset(w * 0.4f, top - 10f),
                Size(w * 0.2f, 28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
    }
}
