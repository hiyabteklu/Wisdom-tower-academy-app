package com.wisdomtower.academy.ui.games

import androidx.compose.foundation.background
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
import com.wisdomtower.academy.data.games.WisdomDefenseTuning
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Full-screen "Choose level" step (docs/GAMES_COOL_PLAN.md slice G2).
 * Three cards (Easy green, Medium amber, Hard rose). Tap a card to select it, then Play.
 * [initial] is the level used last time. Back is handled by the parent (GameTabScreen BackHandler).
 * Numbers shown come from [WisdomDefenseTuning], so the text can never disagree with the game.
 */
@Composable
fun LevelPickerScreen(
    gameTitle: String,
    initial: GameLevel,
    onPick: (GameLevel) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
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
                modifier = Modifier.clickable(onClick = onBack).padding(8.dp)
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
                    LevelCard(
                        level = level,
                        selected = level == selected,
                        onClick = { selected = level }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            val playShape = RoundedCornerShape(28.dp)
            Box(
                Modifier
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

private fun levelLine(level: GameLevel): String {
    val t = WisdomDefenseTuning.forLevel(level)
    val mult = if (t.scoreMultiplier == 1f) "x1" else "x" + t.scoreMultiplier.toString()
    val extra = when (level) {
        GameLevel.EASY -> "Slower enemies. A hint after a wrong answer."
        GameLevel.MEDIUM -> "The normal game."
        GameLevel.HARD -> "Fast enemies. Armored golems and a boss."
    }
    return extra + "\n" + t.hearts + " hearts, up to " + t.maxEnemies + " enemies, score " + mult
}

@Composable
private fun LevelCard(
    level: GameLevel,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = levelColor(level)
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
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
            levelLine(level),
            color = WisdomInk,
            fontSize = 13.sp,
            textAlign = TextAlign.Start
        )
    }
}
