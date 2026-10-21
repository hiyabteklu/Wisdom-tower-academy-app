package com.wisdomtower.academy.ui.games.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted

@Composable
fun GameHubBanner(isClimb: Boolean, onClick: () -> Unit) {
    val title = if (isClimb) "Tower Climb" else "Tower Defense"
    val subtitle = if (isClimb) "Climb the tower with question banks" else "Defend the citadel with exams"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0E3A4A), Color(0xFF1A1A3A))))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            if (isClimb) {
                drawRect(
                    Color(0xFF22E0FF),
                    topLeft = Offset(size.width * 0.3f, size.height * 0.2f),
                    size = Size(size.width * 0.4f, size.height * 0.7f)
                )
                drawCircle(Color(0xFFF1DFB4), radius = 6f, center = Offset(size.width * 0.5f, size.height * 0.25f))
            } else {
                drawCircle(Color(0xFF22E0FF), radius = size.minDimension * 0.35f, center = center)
                drawCircle(Color(0xFFF43F5E), radius = 5f, center = Offset(size.width * 0.8f, size.height * 0.3f))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = WisdomCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = WisdomMuted, fontSize = 12.sp)
        }
    }
}
