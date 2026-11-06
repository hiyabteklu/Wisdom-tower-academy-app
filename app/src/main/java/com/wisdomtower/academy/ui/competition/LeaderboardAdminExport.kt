package com.wisdomtower.academy.ui.competition

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.AdminAccess
import com.wisdomtower.academy.data.admin.LeaderboardExportRepository
import com.wisdomtower.academy.data.admin.LeaderboardExportResult
import com.wisdomtower.academy.ui.admin.LeaderboardJpgFormat
import com.wisdomtower.academy.ui.admin.LeaderboardJpgRenderer
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// U5 (2026-11-06, owner): the Top 3 JPG download lives on the contest leaderboard (the trophy section), for ADMINS ONLY.
// Nothing is drawn for a student: AdminAccess.isAdmin() asks the database (am_i_admin) and any failure answers false.
// The export itself is checked again on the server (admin_competition_export raises admin_only for non-admins), so
// hiding the buttons is only a courtesy, never the lock. No minimum number of participants: 0 to 3 winners all export.
// Two sizes: 4:3 (1600x1200) and 9:16 (1080x1920). The app asks where to save the file.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
@Composable
fun LeaderboardAdminExport(competitionId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAdmin by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(Unit) { isAdmin = AdminAccess.isAdmin() }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        val bytes = pendingBytes
        pendingBytes = null
        if (uri != null && bytes != null) {
            scope.launch {
                message = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("Could not open the selected save location.")
                    }
                    "Leaderboard JPG saved."
                } catch (_: Exception) {
                    "Could not save the JPG. Please choose another location."
                }
            }
        }
    }

    fun export(format: LeaderboardJpgFormat) {
        if (busy) return
        scope.launch {
            busy = true
            message = ""
            when (val loaded = LeaderboardExportRepository.load(context, competitionId)) {
                is LeaderboardExportResult.Success -> {
                    try {
                        val bytes = withContext(Dispatchers.Default) {
                            LeaderboardJpgRenderer.render(loaded.data, format)
                        }
                        pendingBytes = bytes
                        val safeTitle = loaded.data.examTitle
                            .lowercase()
                            .replace(Regex("[^a-z0-9]+"), "-")
                            .trim('-')
                            .take(32)
                            .ifBlank { "competition" }
                        saveLauncher.launch("wta-" + safeTitle + "-top3-" + format.suffix + ".jpg")
                    } catch (_: Exception) {
                        message = "The leaderboard image could not be prepared."
                    }
                }
                is LeaderboardExportResult.Failure -> message = loaded.message
            }
            busy = false
        }
    }

    if (!isAdmin) return

    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Admin: download Top 3 as JPG",
                color = WisdomInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompetitionPillButton(
                    label = if (busy) "Preparing..." else "4:3 landscape",
                    onClick = { export(LeaderboardJpgFormat.LANDSCAPE_4_3) },
                    modifier = Modifier.weight(1f),
                    emphasized = true,
                    enabled = !busy
                )
                CompetitionPillButton(
                    label = if (busy) "Preparing..." else "9:16 portrait",
                    onClick = { export(LeaderboardJpgFormat.PORTRAIT_9_16) },
                    modifier = Modifier.weight(1f),
                    emphasized = true,
                    enabled = !busy
                )
            }
            if (message.isNotEmpty()) {
                Text(text = message, color = WisdomMuted, fontSize = 12.sp)
            }
        }
    }
}
