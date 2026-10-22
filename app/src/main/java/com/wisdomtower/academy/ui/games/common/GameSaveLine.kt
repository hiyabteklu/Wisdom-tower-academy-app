package com.wisdomtower.academy.ui.games.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameSaveOutcome
import com.wisdomtower.academy.data.games.GameSaveStatus
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * "New best!" and the save state under the result stars (docs/GAMES_COOL_PLAN.md slice R2).
 * [outcome] null = still saving.
 */
@Composable
fun GameSaveLine(outcome: GameSaveOutcome?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (outcome != null && outcome.newBest) {
            Text("New best!", color = GameTheme.amber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        val text: String
        val color = when (outcome?.status) {
            null -> {
                text = "Saving\u2026"
                WisdomMuted
            }
            GameSaveStatus.Saved -> {
                text = "\u2713 Saved"
                GameTheme.correct
            }
            GameSaveStatus.Pending -> {
                text = "Will save when online"
                GameTheme.amber
            }
            GameSaveStatus.NotSignedIn -> {
                text = "Sign in to save your results"
                WisdomMuted
            }
            GameSaveStatus.Failed -> {
                text = "Could not save this result"
                GameTheme.wrong
            }
        }
        Text(text, color = color, fontSize = 13.sp)
    }
}
