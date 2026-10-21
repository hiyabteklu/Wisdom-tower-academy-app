package com.wisdomtower.academy.ui.games.climb

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin

/**
 * Wise owl with a golden lantern — drawn from ovals/paths (no bitmaps).
 * [phase] drives idle breath/blink; [flap] 0..1 for wing motion.
 */
object OwlPainter {

    private val bodyColor = Color(0xFF6B5A4A)
    private val bellyColor = Color(0xFFC4A882)
    private val beakColor = Color(0xFFE8A838)
    private val lanternGold = Color(0xFFF1DFB4)
    private val lanternGlow = Color(0x88F59E0B)
    private val eyeWhite = Color(0xFFF5F0E8)
    private val pupil = Color(0xFF1A1A1A)

    fun draw(
        scope: DrawScope,
        center: Offset,
        sizePx: Float,
        phase: Float,
        flap: Float = 0f,
        blink: Boolean = false
    ) {
        val breath = 1f + 0.03f * sin(phase * 2.2f)
        scope.translate(center.x, center.y) {
            scale(sizePx / 48f * breath, sizePx / 48f * breath, pivot = Offset.Zero) {
                drawOval(bodyColor, topLeft = Offset(-14f, -8f), size = Size(28f, 32f))
                drawOval(bellyColor, topLeft = Offset(-9f, -2f), size = Size(18f, 22f))
                drawCircle(bodyColor, radius = 12f, center = Offset(0f, -16f))
                if (!blink) {
                    drawCircle(eyeWhite, radius = 4.5f, center = Offset(-5f, -17f))
                    drawCircle(eyeWhite, radius = 4.5f, center = Offset(5f, -17f))
                    drawCircle(pupil, radius = 2f, center = Offset(-5f, -17f))
                    drawCircle(pupil, radius = 2f, center = Offset(5f, -17f))
                } else {
                    drawLine(pupil, Offset(-8f, -17f), Offset(-2f, -17f), strokeWidth = 1.5f)
                    drawLine(pupil, Offset(2f, -17f), Offset(8f, -17f), strokeWidth = 1.5f)
                }
                val beak = Path().apply {
                    moveTo(0f, -12f)
                    lineTo(-3f, -8f)
                    lineTo(3f, -8f)
                    close()
                }
                drawPath(beak, beakColor)
                rotate(-12f - flap * 25f, pivot = Offset(-12f, 0f)) {
                    drawOval(bodyColor, topLeft = Offset(-22f, -4f), size = Size(12f, 20f))
                }
                rotate(12f + flap * 25f, pivot = Offset(12f, 0f)) {
                    drawOval(bodyColor, topLeft = Offset(10f, -4f), size = Size(12f, 20f))
                }
                val lx = 16f
                val ly = 10f
                val flicker = 0.7f + 0.3f * sin(phase * 7f) * sin(phase * 3.1f)
                drawCircle(lanternGlow.copy(alpha = 0.35f * flicker), radius = 10f, center = Offset(lx, ly))
                drawRoundRect(
                    color = lanternGold,
                    topLeft = Offset(lx - 5f, ly - 4f),
                    size = Size(10f, 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
                drawLine(Color(0xFF8B6914), Offset(lx, ly - 4f), Offset(lx, ly - 10f), strokeWidth = 1.5f)
                drawCircle(Color(0xFFFFE08A).copy(alpha = flicker), radius = 3f, center = Offset(lx, ly + 1f))
            }
        }
    }
}
