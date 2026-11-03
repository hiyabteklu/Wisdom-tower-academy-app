package com.wisdomtower.academy.ui.games

import androidx.compose.foundation.background
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameLevel
import com.wisdomtower.academy.data.games.GameLocalBest
import com.wisdomtower.academy.data.games.MatchTuning
import com.wisdomtower.academy.data.games.WisdomDefenseTuning
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Full-screen "Choose level" step (docs/GAMES_COOL_PLAN.md slices G2 and G3).
 * Three cards (Easy green, Medium amber, Hard rose). Tap a card to select it, then Play.
 * [initial] is the level used last time. Back is handled by the parent (GameTabScreen BackHandler).
 * [describe] gives the card text of one level; use [defenseLevelLine] or [matchLevelLine] so the text
 * is always built from the real tuning numbers and can never disagree with the game.
 * Slice R3 (first part): [bestFor] gives the best result kept on this phone for a level (null = never played); a card
 * with a result shows "Best" stars and how many times it was played.
 */
@Composable
fun LevelPickerScreen(
    gameTitle: String,
    initial: GameLevel,
    describe: (GameLevel) -> String,
    onPick: (GameLevel) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bestFor: (GameLevel) -> GameLocalBest? = { null }
) {
    var selected by remember { mutableStateOf(initial) }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                "Back",
                color = WisdomCyan,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.wisdomTouchFeel().clickable(onClick = onBack).padding(8.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose level",
                color = WisdomInk,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                gameTitle,
                color = WisdomMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (level in GameLevel.values()) {
                    val best = remember(level) { bestFor(level) }
                    LevelCard(
                        level = level,
                        text = describe(level),
                        best = best,
                        selected = level == selected,
                        onClick = { selected = level }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            val playShape = RoundedCornerShape(28.dp)
            Box(
                Modifier.wisdomTouchFeel()
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(playShape)
                    .background(levelColor(selected))
                    .clickable { onPick(selected) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Play " + selected.label,
                    color = Color(0xFF0B1220),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun levelColor(level: GameLevel): Color = when (level) {
    GameLevel.EASY -> Color(0xFF34D399)
    GameLevel.MEDIUM -> Color(0xFFF59E0B)
    GameLevel.HARD -> Color(0xFFF43F5E)
}

/** Card text for Wisdom Defense, built from [WisdomDefenseTuning]. */
fun defenseLevelLine(level: GameLevel): String {
    val t = WisdomDefenseTuning.forLevel(level)
    val mult = if (t.scoreMultiplier == 1f) "x1" else "x" + t.scoreMultiplier.toString()
    val extra = when (level) {
        GameLevel.EASY -> "Slower enemies. A hint after a wrong answer."
        GameLevel.MEDIUM -> "The normal game."
        GameLevel.HARD -> "Fast enemies. Armored golems and a boss."
    }
    return extra + "\n" + t.hearts + " hearts, up to " + t.maxEnemies + " enemies, score " + mult
}

/** Card text for Match, built from [MatchTuning]. */
fun matchLevelLine(level: GameLevel): String {
    val t = MatchTuning.forLevel(level)
    val extra = when (level) {
        GameLevel.EASY -> "No timer. A wrong pair shows the right card."
        GameLevel.MEDIUM -> "The normal game."
        GameLevel.HARD -> "A timer runs. 3 stars need few moves and a time under par."
    }
    return extra + "\n" + t.pairsOnBoard + " pairs on the board (" + (t.pairsOnBoard * 2) + " cards)"
}

@Composable
private fun LevelCard(
    level: GameLevel,
    text: String,
    best: GameLocalBest?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = levelColor(level)
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.wisdomTouchFeel()
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.18f) else Color(0x990E1A2B))
            .border(if (selected) 3.dp else 1.dp, if (selected) color else color.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(
            level.label,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            color = WisdomInk,
            fontSize = 13.sp,
            textAlign = TextAlign.Start
        )
        if (best != null && best.plays > 0) {
            Spacer(Modifier.height(6.dp))
            val stars = best.stars.coerceIn(0, 3)
            val timesWord = if (best.plays == 1) " time" else " times"
            Text(
                "Best " + "\u2605".repeat(stars) + "\u2606".repeat(3 - stars) + "   Played " + best.plays + timesWord,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
