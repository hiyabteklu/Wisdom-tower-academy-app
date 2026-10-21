package com.wisdomtower.academy.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameHubIndex
import com.wisdomtower.academy.data.games.GameHubSubject
import com.wisdomtower.academy.data.games.GameSetChapter
import com.wisdomtower.academy.data.games.GameSetsRepository
import com.wisdomtower.academy.data.games.GameSetsResult
import com.wisdomtower.academy.ui.packages.NativePackage
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * NEW game hub (docs/GAMES_HUB_PLAN.md, step 2b). Flow: pick a game -> pick a course -> pick a subject -> chapter list.
 * Chapters come from published `game_sets` rows. A chapter that has no set for the chosen game shows
 * "Coming soon" for that game only.
 *
 * [onPlay] is called when the student taps a chapter that has a set; the games themselves (Match, Wisdom Defense)
 * are plan steps 4 and 5, so by default it does nothing.
 *
 * NOT wired into the bottom nav yet: `GameTabScreen` (old Climb / Defense hub) is still what the Game tab shows.
 * It is switched over together with the first playable game, so students never meet a dead end.
 *
 * STATUS: written, NOT compiled, NOT tested.
 */
@Composable
fun GameHubScreen(
    packageList: List<NativePackage>,
    modifier: Modifier = Modifier,
    onPlay: (game: String, scopePath: String, chapter: Int) -> Unit = { _, _, _ -> }
) {
    var game by remember { mutableStateOf<String?>(null) }
    var pkg by remember { mutableStateOf<NativePackage?>(null) }
    var subject by remember { mutableStateOf<GameHubSubject?>(null) }

    BackHandler(enabled = game != null) {
        when {
            subject != null -> subject = null
            pkg != null -> pkg = null
            else -> game = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())
        val g = game
        val p = pkg
        val s = subject
        when {
            g == null -> GamePickStep(onPick = { game = it })
            p == null -> CourseStep(g, packageList, onPick = { pkg = it })
            s == null -> SubjectStep(g, p, onPick = { subject = it })
            else -> ChapterStep(g, s, onPlay)
        }
    }
}

private fun gameTitle(game: String): String =
    if (game == GameSetsRepository.GAME_DEFENSE) "Wisdom Defense" else "Match"

@Composable
private fun GamePickStep(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Games", color = WisdomInk, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        HubGameCard(
            emoji = "\uD83D\uDEE1\uFE0F",
            title = "Wisdom Defense",
            body = "Answer to shoot the enemies driving at your empire.",
            onClick = { onPick(GameSetsRepository.GAME_DEFENSE) }
        )
        HubGameCard(
            emoji = "\uD83C\uDCCF",
            title = "Match",
            body = "Match each term with its meaning.",
            onClick = { onPick(GameSetsRepository.GAME_MATCH) }
        )
    }
}

@Composable
private fun CourseStep(game: String, packageList: List<NativePackage>, onPick: (NativePackage) -> Unit) {
    StepColumn(kicker = gameTitle(game), title = "Choose a course") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(packageList, key = { it.id }) { pkg ->
                HubRow(title = pkg.name, trailing = null, trailingColor = WisdomMuted, onClick = { onPick(pkg) })
            }
            item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SubjectStep(game: String, pkg: NativePackage, onPick: (GameHubSubject) -> Unit) {
    val context = LocalContext.current
    var subjects by remember(pkg.id) { mutableStateOf<List<GameHubSubject>?>(null) }
    var failure by remember(pkg.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(pkg.id) {
        val known = GameHubIndex.knownSubjects(pkg.id)
        if (known.isNotEmpty()) {
            subjects = known
        } else {
            when (val result = GameHubIndex.packageSubjects(context, pkg.id)) {
                is GameSetsResult.Success -> subjects = result.value
                is GameSetsResult.Failure -> failure = result.message
            }
        }
    }
    val list = subjects
    val error = failure
    StepColumn(kicker = "${gameTitle(game)} \u00B7 ${pkg.name}", title = "Choose a subject") {
        when {
            error != null -> StatusText(error)
            list == null -> StatusText("Loading\u2026")
            list.isEmpty() -> StatusText("Coming soon")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.scopePath }) { subject ->
                    HubRow(title = subject.name, trailing = null, trailingColor = WisdomMuted, onClick = { onPick(subject) })
                }
                item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ChapterStep(game: String, subject: GameHubSubject, onPlay: (String, String, Int) -> Unit) {
    val context = LocalContext.current
    var chapters by remember(subject.scopePath) { mutableStateOf<List<GameSetChapter>?>(null) }
    var failure by remember(subject.scopePath) { mutableStateOf<String?>(null) }
    LaunchedEffect(subject.scopePath) {
        when (val result = GameSetsRepository.chapters(context, subject.scopePath)) {
            is GameSetsResult.Success -> chapters = result.value
            is GameSetsResult.Failure -> failure = result.message
        }
    }
    val list = chapters
    val error = failure
    StepColumn(kicker = "${gameTitle(game)} \u00B7 ${subject.name}", title = "Choose a chapter") {
        when {
            error != null -> StatusText(error)
            list == null -> StatusText("Loading\u2026")
            list.isEmpty() -> StatusText("Coming soon")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.chapter }) { chapter ->
                    val available =
                        if (game == GameSetsRepository.GAME_DEFENSE) chapter.hasDefense else chapter.hasMatch
                    val label = if (chapter.title.isEmpty()) "Chapter ${chapter.chapter}"
                    else "Chapter ${chapter.chapter}: ${chapter.title}"
                    HubRow(
                        title = label,
                        trailing = if (available) "Play" else "Coming soon",
                        trailingColor = if (available) WisdomCyan else WisdomMuted,
                        onClick = { if (available) onPlay(game, subject.scopePath, chapter.chapter) }
                    )
                }
                item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun StepColumn(kicker: String, title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
        Text(kicker, color = WisdomCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(title, color = WisdomInk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text, color = WisdomMuted, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun HubRow(title: String, trailing: String?, trailingColor: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x1FFFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = WisdomInk,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(trailing, color = trailingColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HubGameCard(emoji: String, title: String, body: String, onClick: () -> Unit) {
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
