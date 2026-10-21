package com.wisdomtower.academy.ui.games.defense

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.wisdomtower.academy.data.games.DefenseEnemyKind
import kotlin.math.sin

object DefenseSprites {

    private val scoutPath: Path = Path().apply {
        moveTo(0f, -12f)
        lineTo(8f, 0f)
        lineTo(4f, 12f)
        lineTo(-4f, 12f)
        lineTo(-8f, 0f)
        close()
    }

    private val standardBody: Path = Path().apply {
        addOval(Rect(-10f, -14f, 10f, 12f))
    }

    private val armoredShield: Path = Path().apply {
        moveTo(-12f, -8f)
        lineTo(12f, -8f)
        lineTo(10f, 14f)
        lineTo(-10f, 14f)
        close()
    }

    fun drawEnemy(
        scope: DrawScope,
        kind: DefenseEnemyKind,
        center: Offset,
        sizePx: Float,
        phase: Float,
        damaged: Boolean = false
    ) {
        scope.translate(center.x, center.y) {
            scale(sizePx / 28f, sizePx / 28f, pivot = Offset.Zero) {
                when (kind) {
                    DefenseEnemyKind.Scout -> {
                        val bob = 2f * sin(phase * 6f)
                        translate(0f, bob) {
                            drawPath(scoutPath, Color(0xFFE8A838))
                            drawCircle(Color(0xFFFFF3C4), 2f, Offset(-3f, -4f))
                            drawCircle(Color(0xFFFFF3C4), 2f, Offset(3f, -4f))
                        }
                    }
                    DefenseEnemyKind.Standard -> {
                        drawPath(standardBody, Color(0xFF4A5568))
                        drawCircle(Color(0xFF22E0FF), 3f, Offset(-4f, -4f))
                        drawCircle(Color(0xFF22E0FF), 3f, Offset(4f, -4f))
                        drawCircle(Color(0xFF0A101C), 1.5f, Offset(-4f, -4f))
                        drawCircle(Color(0xFF0A101C), 1.5f, Offset(4f, -4f))
                    }
                    DefenseEnemyKind.Armored -> {
                        drawPath(armoredShield, if (damaged) Color(0xFF7A6A5A) else Color(0xFF6B7280))
                        drawPath(armoredShield, Color(0xFF9CA3AF), style = Stroke(2f))
                        if (damaged) {
                            drawLine(Color(0xFFF43F5E), Offset(-6f, -4f), Offset(6f, 8f), 2f)
                        }
                        drawCircle(Color(0xFFF1DFB4), 3f, Offset(0f, -2f))
                    }
                }
            }
        }
    }

    fun drawCitadel(
        scope: DrawScope,
        left: Float,
        groundY: Float,
        health: Int,
        maxHealth: Int,
        phase: Float
    ) {
        val baseW = 70f
        val baseH = 110f
        val x = left
        val y = groundY - baseH
        scope.drawRect(Color(0xFF2A3348), Offset(x, y), Size(baseW, baseH))
        scope.drawRect(Color(0xFF1A2233), Offset(x + 8f, y + 12f), Size(baseW - 16f, baseH - 24f))
        for (i in 0 until 4) {
            scope.drawRect(
                Color(0xFF3A4560),
                Offset(x + i * (baseW / 4f) + 2f, y - 12f),
                Size(baseW / 4f - 4f, 14f)
            )
        }
        val coreAlpha = 0.5f + 0.3f * sin(phase * 2f)
        val coreColor = when {
            health <= 1 -> Color(0xFFF43F5E)
            health <= maxHealth / 2 -> Color(0xFFF59E0B)
            else -> Color(0xFF22E0FF)
        }
        scope.drawCircle(coreColor.copy(alpha = coreAlpha), 16f, Offset(x + baseW / 2f, y + baseH * 0.45f))
        scope.drawCircle(Color.White.copy(alpha = 0.5f), 6f, Offset(x + baseW / 2f, y + baseH * 0.45f))
        for (i in 0 until maxHealth) {
            val sx = x + 10f + i * 12f
            val sy = y + baseH + 8f
            val filled = i < health
            scope.drawCircle(if (filled) coreColor else Color(0xFF334155), 5f, Offset(sx, sy))
        }
    }

    fun drawSilhouette(scope: DrawScope, kind: DefenseEnemyKind, center: Offset, sizePx: Float) {
        scope.translate(center.x, center.y) {
            scale(sizePx / 28f * 0.7f, sizePx / 28f * 0.7f, pivot = Offset.Zero) {
                val c = Color(0x44222A3A)
                when (kind) {
                    DefenseEnemyKind.Scout -> drawPath(scoutPath, c)
                    DefenseEnemyKind.Standard -> drawPath(standardBody, c)
                    DefenseEnemyKind.Armored -> drawPath(armoredShield, c)
                }
            }
        }
    }
}
