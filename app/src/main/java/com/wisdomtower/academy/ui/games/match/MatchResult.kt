package com.wisdomtower.academy.ui.games.match

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
import com.wisdomtower.academy.data.games.MatchTuning
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameSaveLine
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.sin

/**
 * Stars of a finished Match game from the level's [MatchTuning]: 3 when moves <= ceil(pairs x star3Factor) AND (no par time
 * or finished under par), 2 when moves <= ceil(pairs x star2Factor), otherwise 1. MEDIUM = 1.25 and 2, exactly as before.
 * Used by the result screen and by the result saver (slice R2), so both always agree.
 */
internal fun matchStars(pairsTotal: Int, moves: Int, tuning: MatchTuning, elapsedSeconds: Int): Int {
    val parSeconds: Int? = tuning.parSecondsPerPair?.let { it * pairsTotal }
    val underPar = parSeconds == null || elapsedSeconds <= parSeconds
    return when {
        moves <= ceil(pairsTotal * tuning.star3Factor.toDouble()).toInt() && underPar -> 3
        moves <= ceil(pairsTotal * tuning.star2Factor.toDouble()).toInt() -> 2
        else -> 1
    }
}

/** Match score for the saved best: 100 per pair, minus 20 for every move beyond one per pair, never below 0. */
internal fun matchScore(pairsTotal: Int, moves: Int): Int {
    val extra = (moves - pairsTotal).coerceAtLeast(0)
    return (pairsTotal * 100 - extra * 20).coerceAtLeast(0)
}

/**
 * Match result screen (docs/GAMES_FEEL_PLAN.md slice M3, levels by docs/GAMES_COOL_PLAN.md slice G3).
 * Stars: see [matchStars]. A finished set always earns at least 1 star.
 * Slice R2: when [showSave], the save line with [saveOutcome] (null = still saving).
 * Slice MW (part 1): stars pop in with a little overshoot, and 3 stars bring a confetti burst (36 pieces, 14 on a
 * low-end phone, none with reduce-motion), the same look as the Defense result.
 */
@Composable
internal fun MatchResult(
    pairsTotal: Int,
    moves: Int,
    bestStreak: Int,
    reduceMotion: Boolean,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    tuning: MatchTuning = MatchTuning.MEDIUM,
    elapsedSeconds: Int = 0,
    showSave: Boolean = false,
    saveOutcome: GameSaveOutcome? = null
) {
    val context = LocalContext.current
    val lowEnd = remember { GameTheme.isLowEnd(context) }
    val threeStars = pairsTotal > 0 && matchStars(pairsTotal, moves, tuning, elapsedSeconds) == 3
    Box(modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pairsTotal == 0) {
            Text(
                "No pairs in this set",
                color = WisdomInk,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            GamePrimaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
            return@Column
        }

        val parSeconds: Int? = tuning.parSecondsPerPair?.let { it * pairsTotal }
        val underPar = parSeconds == null || elapsedSeconds <= parSeconds
        val stars = matchStars(pairsTotal, moves, tuning, elapsedSeconds)
        val accuracy = if (moves <= 0) 100 else (pairsTotal * 100 / moves).coerceIn(0, 100)

        Text(
            "Set complete!",
            color = WisdomInk,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..2) {
                MatchResultStar(filled = i < stars, delayMs = 300 + i * 250, animate = !reduceMotion)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MatchStatTile("Moves", "$moves", Modifier.weight(1f))
            MatchStatTile("Best streak", "x$bestStreak", Modifier.weight(1f))
            MatchStatTile("Accuracy", "$accuracy%", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${tuning.level.label} \u00B7 $pairsTotal pairs matched",
            color = WisdomMuted,
            fontSize = 13.sp
        )
        if (parSeconds != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Time ${formatMatchTime(elapsedSeconds)} (par ${formatMatchTime(parSeconds)})",
                color = if (underPar) WisdomMuted else Color(0xFFF43F5E),
                fontSize = 13.sp
            )
        }
        if (showSave) {
            Spacer(Modifier.height(6.dp))
            GameSaveLine(saveOutcome)
        }
        Spacer(Modifier.height(28.dp))
        GamePrimaryButton("Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        GameSecondaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
    // Confetti after the third star has popped in (300 + 2 x 250 ms delay + the pop itself).
    if (threeStars && !reduceMotion) {
        MatchConfetti(Modifier.fillMaxSize(), startDelayMs = 1000L, pieces = if (lowEnd) 14 else 36)
    }
    }
}

/** m:ss */
internal fun formatMatchTime(totalSeconds: Int): String {
    val s = if (totalSeconds < 0) 0 else totalSeconds
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
private fun MatchStatTile(label: String, value: String, modifier: Modifier = Modifier) {
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
private fun MatchResultStar(filled: Boolean, delayMs: Int, animate: Boolean) {
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
 * Confetti falling over the whole screen for about 2 seconds, started after [startDelayMs]. Same drawing as the Defense
 * result (a private copy, so neither file depends on the other): [pieces] coloured pieces, positions from the piece
 * number (no randomness), no touches taken.
 */
@Composable
private fun MatchConfetti(modifier: Modifier, startDelayMs: Long, pieces: Int) {
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
