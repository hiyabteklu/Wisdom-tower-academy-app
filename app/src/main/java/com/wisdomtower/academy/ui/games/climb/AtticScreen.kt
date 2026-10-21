package com.wisdomtower.academy.ui.games.climb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameStore
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

@Composable
fun AtticScreen(
    attic: List<GameStore.AtticEntry>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFF070C16))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WisdomTextPrimary,
                modifier = Modifier.clickable(onClick = onBack).padding(8.dp)
            )
            Text("Attic", color = WisdomCyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        if (attic.isEmpty()) {
            Text(
                "No missed questions yet. Climb a floor to collect reviews here.",
                color = WisdomMuted,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(attic, key = { it.questionId }) { entry ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color(0xFF121A2A))
                            .padding(12.dp)
                    ) {
                        Text(entry.questionId, color = WisdomTextPrimary, fontSize = 13.sp)
                        Text(
                            "Bank ${entry.bankId.take(8)}… · correct in a row: ${entry.correctInRow}",
                            color = WisdomMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
