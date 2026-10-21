package com.wisdomtower.academy.ui.games.defense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameQuestion
import com.wisdomtower.academy.ui.games.common.GameCardState
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameQuestionCard
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

data class DefenseRunResult(
    val score: Int,
    val accuracy: Float,
    val bestCombo: Int,
    val wavesCleared: Int,
    val victory: Boolean,
    val newBest: Boolean,
    val missed: List<GameQuestion>
)

@Composable
fun DefenseResultScreen(
    result: DefenseRunResult,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var reviewIndex by remember { mutableStateOf(0) }
    val missed = result.missed

    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFF070C16))
            .padding(20.dp)
    ) {
        Text(
            if (result.victory) "Citadel stands!" else "Citadel fell",
            color = if (result.victory) GameTheme.gold else GameTheme.wrong,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(12.dp))
        Text("Score ${result.score}", color = WisdomTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Accuracy ${(result.accuracy * 100).toInt()}% · Best combo ${result.bestCombo} · Waves ${result.wavesCleared}",
            color = WisdomMuted,
            fontSize = 14.sp
        )
        if (result.newBest) {
            Text("New best!", color = GameTheme.amber, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        if (missed.isNotEmpty()) {
            Text(
                "Missed review (${reviewIndex + 1}/${missed.size})",
                color = WisdomTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            val q = missed[reviewIndex.coerceIn(0, missed.lastIndex)]
            GameQuestionCard(
                card = GameCardState(
                    id = q.id,
                    prompt = q.prompt,
                    choices = q.choices,
                    selected = -1,
                    enabled = false,
                    hidden = emptyList(),
                    revealCorrect = q.correctIndex,
                    revealSolution = q.solution
                ),
                onPick = {},
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (reviewIndex > 0) {
                    GameSecondaryButton("Previous", onClick = { reviewIndex-- })
                } else {
                    Spacer(Modifier.padding(1.dp))
                }
                if (reviewIndex < missed.lastIndex) {
                    GameSecondaryButton("Next", onClick = { reviewIndex++ })
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
            Text("No missed questions.", color = WisdomMuted)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        GamePrimaryButton("Play again", onClick = onPlayAgain, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        GameSecondaryButton("Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
