package com.wisdomtower.academy.ui.games.match

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.games.common.GameTheme

/**
 * Match HUD (docs/GAMES_FEEL_PLAN.md slice M3): progress bar on top with a streak chip at its end.
 * The chip shows from a streak of 2 and pops each time the streak grows. Its space is always
 * reserved so the bar does not jump.
 */
@Composable
internal fun MatchProgressBar(
    matched: Int,
    total: Int,
    streak: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val target = if (total <= 0) 0f else (matched.toFloat() / total).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(if (reduceMotion) 0 else 350),
        label = "matchProgress"
    )
    val barShape = RoundedCornerShape(5.dp)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(barShape)
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(barShape)
                    .background(GameTheme.correct)
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(52.dp), contentAlignment = Alignment.CenterEnd) {
            StreakChip(streak = streak, reduceMotion = reduceMotion)
        }
    }
}

@Composable
private fun StreakChip(streak: Int, reduceMotion: Boolean) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(streak) {
        if (streak >= 2 && !reduceMotion) {
            pop.snapTo(1.35f)
            pop.animateTo(1f, tween(220))
        } else {
            pop.snapTo(1f)
        }
    }
    if (streak < 2) return
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = "x$streak",
        color = GameTheme.amber,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .clip(shape)
            .background(Color(0x33FFB020))
            .border(1.dp, GameTheme.amber, shape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}
