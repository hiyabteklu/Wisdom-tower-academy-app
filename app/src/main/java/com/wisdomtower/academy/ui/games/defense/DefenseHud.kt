package com.wisdomtower.academy.ui.games.defense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.DefensePowerUp
import com.wisdomtower.academy.ui.games.common.GamePowerUpChip
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

@Composable
fun DefenseHud(
    score: Int,
    wave: Int,
    streak: Int,
    citadelHealth: Int,
    chronos: Int,
    logic: Int,
    fortify: Int,
    deflect: Int,
    freezeLeft: Float,
    onPowerUp: (DefensePowerUp) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wave $wave", color = WisdomTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Score $score", color = GameTheme.gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("❤ $citadelHealth", color = GameTheme.heart, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (streak >= 3) {
                Text(
                    "Combo x${when {
                        streak >= 8 -> 4
                        streak >= 5 -> 3
                        else -> 2
                    }} 🔥$streak",
                    color = GameTheme.amber,
                    fontSize = 13.sp
                )
            } else {
                Text("", fontSize = 13.sp)
            }
            if (freezeLeft > 0f) {
                Text("Frozen ${freezeLeft.toInt()}s", color = GameTheme.cyan, fontSize = 13.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GamePowerUpChip("Chronos", chronos, onClick = { onPowerUp(DefensePowerUp.ChronosStasis) })
            GamePowerUpChip("Logic", logic, onClick = { onPowerUp(DefensePowerUp.LogicFilter) })
            GamePowerUpChip("Fortify", fortify, onClick = { onPowerUp(DefensePowerUp.FortifyCore) })
            GamePowerUpChip("Deflect", deflect, onClick = { onPowerUp(DefensePowerUp.TacticalDeflection) })
        }
    }
}
