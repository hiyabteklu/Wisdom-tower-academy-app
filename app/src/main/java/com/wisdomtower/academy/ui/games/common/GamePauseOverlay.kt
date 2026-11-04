package com.wisdomtower.academy.ui.games.common

import com.wisdomtower.academy.ui.theme.WisdomPopupScrim
import com.wisdomtower.academy.ui.theme.WisdomPopupFill
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

/**
 * Pause / quit / sound / vibration overlay. Shown when Back is pressed during play
 * or when the player opens pause. Does not leave the game until Quit is chosen.
 */
@Composable
fun GamePauseOverlay(
    soundOn: Boolean,
    vibrationOn: Boolean,
    onResume: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WisdomPopupScrim),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(WisdomPopupFill)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Paused",
                color = WisdomTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(Modifier.height(4.dp))
            GamePrimaryButton(label = "Resume", onClick = onResume)
            GameSecondaryButton(
                label = if (soundOn) "Sound: On" else "Sound: Off",
                onClick = onToggleSound
            )
            GameSecondaryButton(
                label = if (vibrationOn) "Vibration: On" else "Vibration: Off",
                onClick = onToggleVibration
            )
            GameSecondaryButton(label = "Quit", onClick = onQuit)
        }
    }
}
