package com.wisdomtower.academy.ui.games.climb

import com.wisdomtower.academy.ui.theme.WisdomPopupFill
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.ClimbSessionState
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

@Composable
fun FloorResultSheet(
    complete: ClimbSessionState.FloorComplete,
    onNext: () -> Unit,
    onReplay: () -> Unit,
    onBackToTower: () -> Unit,
    hasNext: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(WisdomPopupFill)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Floor clear!", color = GameTheme.gold, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "★".repeat(complete.stars) + "☆".repeat(3 - complete.stars),
            color = GameTheme.gold,
            fontSize = 28.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${(complete.accuracy * 100).toInt()}% · ${complete.correct}/${complete.total}",
            color = WisdomTextPrimary,
            fontSize = 16.sp
        )
        Text("+${complete.xpGained} XP", color = GameTheme.cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (complete.firstClear) {
            Text("First clear!", color = GameTheme.amber, fontSize = 14.sp)
        }
        Spacer(Modifier.height(20.dp))
        if (hasNext) {
            GamePrimaryButton(label = "Next floor", onClick = onNext, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        GameSecondaryButton(label = "Replay", onClick = onReplay, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        GameSecondaryButton(label = "Back to tower", onClick = onBackToTower, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun FloorFailSheet(
    onRetry: () -> Unit,
    onBackToTower: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xFF121A2A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("The lantern goes out", color = GameTheme.wrong, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("You ran out of hearts. Try again?", color = WisdomMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        GamePrimaryButton(label = "Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        GameSecondaryButton(label = "Back to tower", onClick = onBackToTower, modifier = Modifier.fillMaxWidth())
    }
}
