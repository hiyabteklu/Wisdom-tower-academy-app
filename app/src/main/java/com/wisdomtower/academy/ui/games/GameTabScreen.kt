package com.wisdomtower.academy.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.packages.NativePackage
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Hand-over between the Game tab and StudyWorkspaceScreen.
 * kind = "climb" or "defense". StudyWorkspaceScreen reads it once when a course workspace opens, starts that game
 * straight away and sets it back to null. GameTabScreen also clears it every time it appears.
 */
object GameLaunch {
    var kind by mutableStateOf<String?>(null)
}

/**
 * Bottom-navigation "Game" tab (replaces the old Packages tab).
 * Step 1: choose Tower Climb or Tower Defense. Step 2: choose a course. Then MainActivity shows the course page
 * (subject list, the same screen as before); choosing a subject there starts the game.
 */
@Composable
fun GameTabScreen(
    packageList: List<NativePackage>,
    onSelectPackage: (NativePackage) -> Unit,
    modifier: Modifier = Modifier
) {
    var picking by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { GameLaunch.kind = null }
    BackHandler(enabled = picking != null) { picking = null }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())
        val kind = picking
        if (kind == null) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Games", color = WisdomInk, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text("Practise your question banks and exams as a game.", color = WisdomMuted, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                GameCard(
                    emoji = "\uD83E\uDDD7",
                    title = "Tower Climb",
                    body = "Climb floor by floor. Each floor is a question bank.",
                    onClick = { picking = "climb" }
                )
                GameCard(
                    emoji = "\uD83D\uDEE1\uFE0F",
                    title = "Tower Defense",
                    body = "Defend the tower with your exam answers.",
                    onClick = { picking = "defense" }
                )
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
                Text(
                    if (kind == "climb") "Tower Climb" else "Tower Defense",
                    color = WisdomCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text("Choose a course", color = WisdomInk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(packageList, key = { it.id }) { pkg ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x1FFFFFFF))
                                .clickable {
                                    GameLaunch.kind = kind
                                    onSelectPackage(pkg)
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Text(pkg.name, color = WisdomInk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                    item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun GameCard(emoji: String, title: String, body: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x1FFFFFFF))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Text(emoji, fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, color = WisdomInk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(body, color = WisdomMuted, fontSize = 14.sp)
    }
}
