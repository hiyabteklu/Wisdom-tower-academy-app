package com.wisdomtower.academy.ui.games.defense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameSaveOutcome
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameSaveLine
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Wisdom Defense result screen (docs/GAMES_FEEL_PLAN.md slice D5).
 * Stars (only when won): 3 hearts left = 3 stars, 2 = 2 stars, 1 = 1 star. Lost = 0 stars.
 * Slice R2: [levelLabel] (empty = hidden) and, when [showSave], the save line with [saveOutcome] (null = still saving).
 * Slice W6: stars pop with a little overshoot, a confetti burst after the third star, the level as a chip, a score line
 * with the level multiplier ([score] below 0 = hidden), and a "Next level" button when [nextLevelLabel] and
 * [onNextLevel] are given (the screen passes them only after a win on Easy or Medium).
 * Slice W7a: on a low-end phone the confetti has fewer pieces ([CONFETTI_PIECES_LOW] instead of [CONFETTI_PIECES]).
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
    modifier: Modifier = Modifier,
    levelLabel: String = "",
    showSave: Boolean = false,
    saveOutcome: GameSaveOutcome? = null,
    score: Int = -1,
    scoreMultiplier: Float = 1f,
    nextLevelLabel: String? = null,
    onNextLevel: (() -> Unit)? = null
) {
    val stars = if (won) heartsLeft.coerceIn(1, 3) else 0
    val context = LocalContext.current
    val lowEnd = remember { GameTheme.isLowEnd(context) }
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
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
            if (levelLabel.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LevelChip(levelLabel)
            }
            Spacer(Modifier.height(12.dp))
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
            if (score >= 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Score ", color = WisdomMuted, fontSize = 14.sp)
                    Text("$score", color = GameTheme.amber, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    if (scoreMultiplier != 1f) {
                        Text("  ${multiplierText(scoreMultiplier)} level bonus", color = WisdomMuted, fontSize = 12.sp)
                    }
                }
            }
            if (showSave) {
                Spacer(Modifier.height(8.dp))
                GameSaveLine(saveOutcome)
            }
            Spacer(Modifier.height(24.dp))
            if (nextLevelLabel != null && onNextLevel != null) {
                GamePrimaryButton("Next level: $nextLevelLabel", onClick = onNextLevel, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                GameSecondaryButton("Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            } else {
                GamePrimaryButton("Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            GameSecondaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
        // Confetti after the third star has popped in (300 + 2 x 250 ms delay + the pop itself).
        if (stars == 3 && !reduceMotion) {
            ConfettiBurst(
                Modifier.fillMaxSize(),
                startDelayMs = 1000L,
                pieces = if (lowEnd) CONFETTI_PIECES_LOW else CONFETTI_PIECES
            )
        }
    }
}

/** Confetti pieces on a normal phone, and on a low-end phone (slice W7a). */
private const val CONFETTI_PIECES = 36
private const val CONFETTI_PIECES_LOW = 14

/** "x2" for a whole multiplier, "x1.5" otherwise. */
private fun multiplierText(m: Float): String {
    val whole = m.toInt()
    return if (m == whole.toFloat()) "x$whole" else "x$m"
}

@Composable
private fun LevelChip(label: String) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(WisdomCyan.copy(alpha = 0.15f))
            .border(1.dp, WisdomCyan.copy(alpha = 0.6f), shape)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text("$label level", color = WisdomCyan, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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

/** A star that pops in with a small overshoot (a spring), or just shows when [animate] is false (reduce-motion). */
@Composable
private fun ResultStar(filled: Boolean, delayMs: Int, animate: Boolean) {
    val s = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
            delay(delayMs.toLong())
            s.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
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
            alpha = s.value.coerceIn(0f, 1f)
        }
    )
}

/**
 * Confetti falling over the whole screen for about 2 seconds, started after [startDelayMs]. [pieces] small coloured
 * pieces (36, or 14 on a low-end phone); positions come from the piece number (no randomness), so it is cheap and
 * always looks the same. Draws nothing before and after, and does not take any touches.
 */
@Composable
private fun ConfettiBurst(modifier: Modifier, startDelayMs: Long, pieces: Int) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(startDelayMs)
        t.animateTo(1f, tween(2200, easing = LinearEasing))
    }
    val colors = remember {
        listOf(Color(0xFFFBBF24), Color(0xFF22D3EE), Color(0xFF4ADE80), Color(0xFFF43F5E), Color(0xFFA78BFA))
    }
    Canvas(modifier) {
        val p0 = t.value
        if (p0 <= 0f || p0 >= 1f) return@Canvas
        val w = size.width
        val h = size.height
        val fade = (1f - p0 * p0 * p0).coerceIn(0f, 1f)
        for (i in 0 until pieces) {
            val r1 = ((i * 37) % 100) / 100f
            val r2 = ((i * 53 + 11) % 100) / 100f
            val p = (p0 * (0.8f + 0.5f * r2)).coerceIn(0f, 1f)
            val x = w * r1 + sin(p * 7f + i) * 14.dp.toPx()
            val y = -0.05f * h + p * 1.15f * h
            val s = (6f + 6f * r2).dp.toPx()
            rotate(degrees = p * 360f * (1f + r1), pivot = Offset(x, y)) {
                drawRect(
                    colors[i % colors.size].copy(alpha = fade),
                    Offset(x - s / 2f, y - s / 2f),
                    Size(s, s * 0.6f)
                )
            }
        }
    }
}
