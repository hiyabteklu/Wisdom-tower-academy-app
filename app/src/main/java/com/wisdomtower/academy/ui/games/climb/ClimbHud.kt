package com.wisdomtower.academy.ui.games.climb

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.games.common.GamePowerUpChip
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

@Composable
fun ClimbHud(
    hearts: Int,
    maxHearts: Int,
    timeLeft: Float,
    maxTime: Float,
    streak: Int,
    questionIndex: Int,
    totalQuestions: Int,
    fiftyFifty: Int,
    extraTime: Int,
    onFiftyFifty: () -> Unit,
    onExtraTime: () -> Unit,
    boss: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until maxHearts) {
                    HeartIcon(filled = i < hearts)
                }
            }
            Text(
                text = "${questionIndex + 1}/$totalQuestions",
                color = WisdomTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (streak >= 3) {
                Text("🔥$streak", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("", modifier = Modifier.size(1.dp))
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp)) {
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                val frac = (timeLeft / maxTime).coerceIn(0f, 1f)
                drawRoundRect(Color(0xFF1E293B), size = size)
                val color = when {
                    frac < 0.25f -> GameTheme.wrong
                    frac < 0.5f -> GameTheme.amber
                    else -> GameTheme.correct
                }
                drawRoundRect(color, size = Size(size.width * frac, size.height))
            }
        }
        Text(
            text = "${timeLeft.toInt()}s",
            color = if (timeLeft <= 5f) GameTheme.wrong else WisdomTextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
        )
        if (!boss) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GamePowerUpChip(label = "50/50", charges = fiftyFifty, onClick = onFiftyFifty)
                GamePowerUpChip(label = "+10s", charges = extraTime, onClick = onExtraTime)
            }
        }
    }
}

@Composable
private fun HeartIcon(filled: Boolean) {
    Canvas(Modifier.size(22.dp)) {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            moveTo(w / 2f, h * 0.85f)
            cubicTo(w * 0.1f, h * 0.55f, w * 0.05f, h * 0.15f, w / 2f, h * 0.3f)
            cubicTo(w * 0.95f, h * 0.15f, w * 0.9f, h * 0.55f, w / 2f, h * 0.85f)
            close()
        }
        drawPath(path, if (filled) GameTheme.heart else Color(0xFF334155))
        if (!filled) drawPath(path, Color(0xFF64748B), style = Stroke(1.5f))
    }
}
