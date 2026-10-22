package com.wisdomtower.academy.ui.games.defense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.wisdomtower.academy.data.games.WisdomDefenseEngine
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Slice W5 (power-ups, owner yes 2026-10-22): the small strip above the cockpit.
 * Earned by streak, never bought: 3 correct answers in a row earn a Freeze (enemies stand still for
 * [WisdomDefenseEngine.FREEZE_TIME] seconds), 5 in a row earn a Shield (absorbs one wall hit). A wrong answer sets the
 * streak back to 0. Off on Easy. Tap a ready chip to use it.
 * The strip shows only what matters now: a ready power-up, a running one, or a hint about the next one.
 * It sits over the bottom of the battlefield, so the layout does not move when chips appear.
 */
@Composable
internal fun DefensePowerUps(
    streak: Int,
    freezeReady: Boolean,
    freezeLeft: Float,
    shieldReady: Boolean,
    shieldActive: Boolean,
    onFreeze: () -> Unit,
    onShield: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frozen = freezeLeft > 0f
    val hint: String? = when {
        freezeReady || frozen -> null
        streak < WisdomDefenseEngine.FREEZE_AT -> "Streak $streak/${WisdomDefenseEngine.FREEZE_AT}: Freeze"
        !shieldReady && !shieldActive && streak < WisdomDefenseEngine.SHIELD_AT ->
            "Streak $streak/${WisdomDefenseEngine.SHIELD_AT}: Shield"
        else -> null
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (freezeReady || frozen || shieldReady || shieldActive) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (freezeReady) {
                    PowerChip("\u2744 Freeze", ready = true, onClick = onFreeze)
                } else if (frozen) {
                    PowerChip("\u2744 ${(freezeLeft + 0.5f).toInt()}s", ready = false, onClick = {})
                }
                if (shieldReady) {
                    PowerChip("\uD83D\uDEE1 Shield", ready = true, onClick = onShield)
                } else if (shieldActive) {
                    PowerChip("\uD83D\uDEE1 On", ready = false, onClick = {})
                }
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                color = WisdomMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PowerChip(label: String, ready: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val edge = if (ready) WisdomCyan else WisdomCyan.copy(alpha = 0.45f)
    val bg = if (ready) Color(0xCC14566B) else Color(0x880E1A2B)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(2.dp, edge, shape)
            .clickable(enabled = ready, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (ready) Color.White else WisdomCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
