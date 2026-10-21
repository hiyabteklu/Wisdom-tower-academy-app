package com.wisdomtower.academy.ui.games.defense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.delay

/**
 * Wisdom Defense result screen (docs/GAMES_FEEL_PLAN.md slice D5).
 * Stars (only when won): 3 hearts left = 3 stars, 2 = 2 stars, 1 = 1 star. Lost = 0 stars.
 */
@Composable
internal fun DefenseResult(
    won: Boolean,
    killed: Int,
    total: Int,
    heartsLeft: Int,
    maxHearts: Int,
    reduceMotion: Boolean,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stars = if (won) heartsLeft.coerceIn(1, 3) else 0
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (won) "Empire held!" else "Empire breached",
            color = if (won) WisdomInk else GameTheme.wrong,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..2) {
                ResultStar(filled = i < stars, delayMs = 300 + i * 250, animate = !reduceMotion)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile("Defeated", "$killed / $total", Modifier.weight(1f))
            StatTile("Hearts left", "$heartsLeft / $maxHearts", Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        GamePrimaryButton("Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        GameSecondaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0x990E1A2B))
            .border(1.dp, Color(0x33FFFFFF), shape)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = WisdomInk, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = WisdomMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ResultStar(filled: Boolean, delayMs: Int, animate: Boolean) {
    val s = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
            delay(delayMs.toLong())
            s.animateTo(1f, tween(320))
        }
    }
    Text(
        text = "\u2605",
        color = if (filled) GameTheme.amber else Color(0x33FFFFFF),
        fontSize = 52.sp,
        modifier = Modifier.graphicsLayer {
            val k = 0.4f + 0.6f * s.value
            scaleX = k
            scaleY = k
            alpha = s.value
        }
    )
}
