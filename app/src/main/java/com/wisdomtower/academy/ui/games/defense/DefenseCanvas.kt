package com.wisdomtower.academy.ui.games.defense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.wisdomtower.academy.data.games.DefenseEnemy
import com.wisdomtower.academy.data.games.DefenseEnemyKind
import com.wisdomtower.academy.data.games.DefenseTuning
import kotlin.math.sin

@Composable
fun DefenseCanvas(
    citadelHealth: Int,
    lead: DefenseEnemy?,
    silhouettes: List<DefenseEnemy>,
    wave: Int,
    phase: Float,
    freezeActive: Boolean,
    boltProgress: Float = -1f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.78f
        val citadelLeft = w * 0.04f
        val laneLeft = citadelLeft + 80f
        val laneRight = w * 0.95f
        val laneLen = laneRight - laneLeft

        val night = (wave * 0.08f).coerceIn(0f, 0.7f)
        val top = Color(
            red = (0.05f + (1f - night) * 0.15f),
            green = (0.08f + (1f - night) * 0.1f),
            blue = (0.18f + night * 0.05f)
        )
        val bottom = Color(0.04f, 0.06f, 0.12f)
        drawRect(Brush.verticalGradient(listOf(top, bottom)))

        drawRect(Color(0xFF152033), Offset(0f, groundY - 40f), Size(w, 50f))
        drawRect(Color(0xFF0E1828), Offset(0f, groundY - 20f), Size(w, 30f))
        drawRect(Color(0xFF1A2438), Offset(0f, groundY), Size(w, h - groundY))
        drawLine(
            if (freezeActive) Color(0xFF67E8F9) else Color(0xFF334155),
            Offset(laneLeft, groundY - 2f),
            Offset(laneRight, groundY - 2f),
            strokeWidth = 4f
        )

        val flicker = 0.6f + 0.4f * sin(phase * 5f)
        drawCircle(Color(0x88FF9800).copy(alpha = flicker), 10f, Offset(w * 0.22f, groundY - 50f))
        drawCircle(Color(0x88FF9800).copy(alpha = flicker), 10f, Offset(w * 0.55f, groundY - 45f))

        for (i in 0 until 8) {
            val ex = (w * 0.1f + i * 47f + phase * 30f * (i % 3 + 1)) % w
            val ey = groundY - 80f - (i * 13f) % 60f - sin(phase + i) * 8f
            drawCircle(Color(0x66FFB74D), 2f, Offset(ex, ey))
        }

        DefenseSprites.drawCitadel(
            this,
            left = citadelLeft,
            groundY = groundY,
            health = citadelHealth,
            maxHealth = DefenseTuning.CITADEL_HEALTH + DefenseTuning.CITADEL_HEALTH_CAP_EXTRA,
            phase = phase
        )

        for (s in silhouettes) {
            val x = laneLeft + s.position.coerceIn(0f, 1.4f) * laneLen
            DefenseSprites.drawSilhouette(this, s.kind, Offset(x, groundY - 20f), 36f)
        }

        if (lead != null && lead.alive) {
            val x = laneLeft + lead.position.coerceIn(0f, 1f) * laneLen
            DefenseSprites.drawEnemy(
                this,
                lead.kind,
                Offset(x, groundY - 24f),
                48f,
                phase,
                damaged = lead.hitsRemaining < 2 && lead.kind == DefenseEnemyKind.Armored
            )
        }

        if (boltProgress in 0f..1f && lead != null) {
            val start = Offset(citadelLeft + 35f, groundY - 55f)
            val endX = laneLeft + lead.position.coerceIn(0f, 1f) * laneLen
            val end = Offset(endX, groundY - 24f)
            val mid = Offset(
                (start.x + end.x) / 2f,
                minOf(start.y, end.y) - 40f * (1f - boltProgress)
            )
            val t = boltProgress
            val bx = (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * mid.x + t * t * end.x
            val by = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * mid.y + t * t * end.y
            drawCircle(Color(0xFF22E0FF), 6f, Offset(bx, by))
            drawCircle(Color.White, 3f, Offset(bx, by))
        }
    }
}
