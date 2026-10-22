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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameHubIndex
import com.wisdomtower.academy.data.games.GameHubSubject
import com.wisdomtower.academy.data.games.GameLevelPrefs
import com.wisdomtower.academy.data.games.GameResultsRepository
import com.wisdomtower.academy.data.games.GameSetChapter
import com.wisdomtower.academy.data.games.GameSetsRepository
import com.wisdomtower.academy.data.games.GameSetsResult
import com.wisdomtower.academy.ui.packages.ECE_SEM1_COURSES
import com.wisdomtower.academy.ui.packages.ECE_SEM2_COURSES
import com.wisdomtower.academy.ui.packages.FRESHMAN_SUBJECTS
import com.wisdomtower.academy.ui.packages.GRADE_10_SUBJECTS
import com.wisdomtower.academy.ui.packages.GRADE_11_SUBJECTS
import com.wisdomtower.academy.ui.packages.GRADE_12_SUBJECTS
import com.wisdomtower.academy.ui.packages.GRADE_9_SUBJECTS
import com.wisdomtower.academy.ui.packages.NativePackage
import com.wisdomtower.academy.ui.packages.OpenCard
import com.wisdomtower.academy.ui.packages.REMEDIAL_SUBJECTS
import com.wisdomtower.academy.ui.packages.openGrid
import com.wisdomtower.academy.ui.study.DownloadGauge
import com.wisdomtower.academy.ui.study.DownloadState
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.launch

/**
 * Game hub (docs/GAMES_HUB_PLAN.md, step 2b). Flow: pick a game -> pick a course -> pick a subject -> chapter list.
 * Chapters come from published `game_sets` rows. A chapter that has no set for the chosen game shows
 * "Coming soon" for that game only.
 *
 * 2026-10-22 (owner order): the course and subject steps use the SAME [OpenCard] two-column grid with pictures as the
 * Learning tab (course picture from the package, subject picture from the packed website images), and every chapter
 * that has a set for the chosen game shows the same [DownloadGauge] as the learning lists (tap: save offline, tap again
 * when saved: remove). Texts are unchanged; nothing new is invented. Subjects with no packed picture (UAT, GAT, COC,
 * Exit Exam) simply show the card without a picture, like the Learning tab does.
 *
 * 2026-10-22 (slice R3 part 2): a chapter that was played shows a small line "Medium ★★☆" under its title: the best
 * stars kept on this phone for the level used last in this game ([GameLevelPrefs.last]). Nothing is shown for a chapter
 * that was never played (no invented value).
 *
 * [onPlay] is called when the student taps a chapter that has a set.
 *
 * STATUS: [~] written, NOT compiled, NOT tested on a phone.
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

/** Picture of a subject, found from its `scope_path` in the same packed lists the Learning tab uses. */
private val SUBJECT_IMAGES: Map<String, String> by lazy {
    val map = HashMap<String, String>()
    FRESHMAN_SUBJECTS.forEach { map["freshman/${it.id}"] = it.assetImage }
    map["freshman/mathematics"] = FRESHMAN_SUBJECTS.first { it.id == "math-natural" }.assetImage
    (GRADE_9_SUBJECTS + GRADE_10_SUBJECTS + GRADE_11_SUBJECTS + GRADE_12_SUBJECTS).forEach { s ->
        s.image?.let { map["grade/${s.grade}/${s.id}"] = it }
    }
    REMEDIAL_SUBJECTS.forEach { map["remedial/${it.id}"] = it.assetImage }
    (ECE_SEM1_COURSES + ECE_SEM2_COURSES).forEach { map["ece/${it.semester}/${it.slug}"] = it.assetImage }
    map
}

/** "ECEg3071 \u00B7 Applied Electronics II" becomes "Applied Electronics II" (clean cards, owner rule 2026-10-13). */
private fun cleanSubjectTitle(name: String): String =
    if (name.contains(" \u00B7 ")) name.substringAfter(" \u00B7 ").trim().ifEmpty { name } else name

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
            openGrid("game_courses", packageList) { pkg ->
                OpenCard(pkg.name, pkg.assetImage) { onPick(pkg) }
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
                openGrid("game_subjects_${pkg.id}", list) { subject ->
                    OpenCard(cleanSubjectTitle(subject.name), SUBJECT_IMAGES[subject.scopePath]) { onPick(subject) }
                }
                item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ChapterStep(game: String, subject: GameHubSubject, onPlay: (String, String, Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chapters by remember(subject.scopePath) { mutableStateOf<List<GameSetChapter>?>(null) }
    var failure by remember(subject.scopePath) { mutableStateOf<String?>(null) }
    // chapter number -> download state; only chapters that have a set for this game get an entry
    val downloads = remember(subject.scopePath, game) { mutableStateMapOf<Int, DownloadState>() }
    // R3 part 2: the level used last in this game; the best line of a chapter is shown for this level.
    val lastLevel = remember(game) { GameLevelPrefs.last(context, game) }

    LaunchedEffect(subject.scopePath) {
        when (val result = GameSetsRepository.chapters(context, subject.scopePath)) {
            is GameSetsResult.Success -> chapters = result.value
            is GameSetsResult.Failure -> failure = result.message
        }
    }
    val list = chapters
    val error = failure

    // Which chapters are already saved on the phone (checked once when the list appears)
    LaunchedEffect(list, game) {
        list?.forEach { chapter ->
            val available =
                if (game == GameSetsRepository.GAME_DEFENSE) chapter.hasDefense else chapter.hasMatch
            if (available && downloads[chapter.chapter] == null) {
                val saved = GameSetsRepository.isDownloaded(context, subject.scopePath, chapter.chapter, game)
                downloads[chapter.chapter] = if (saved) DownloadState.DOWNLOADED else DownloadState.IDLE
            }
        }
    }

    StepColumn(kicker = "${gameTitle(game)} \u00B7 ${cleanSubjectTitle(subject.name)}", title = "Choose a chapter") {
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
                    val best = remember(chapter.chapter, game, lastLevel) {
                        GameResultsRepository.localBest(context, game, subject.scopePath, chapter.chapter, lastLevel)
                    }
                    val bestLine: String? = if (available && best != null && best.plays > 0) {
                        val stars = best.stars.coerceIn(0, 3)
                        lastLevel.label + " " + "\u2605".repeat(stars) + "\u2606".repeat(3 - stars)
                    } else {
                        null
                    }
                    ChapterCard(
                        title = label,
                        available = available,
                        downloadState = if (available) downloads[chapter.chapter] ?: DownloadState.IDLE else null,
                        bestLine = bestLine,
                        onClick = { if (available) onPlay(game, subject.scopePath, chapter.chapter) },
                        onDownloadClick = {
                            when (downloads[chapter.chapter]) {
                                DownloadState.DOWNLOADING -> Unit
                                DownloadState.DOWNLOADED -> scope.launch {
                                    GameSetsRepository.removeDownload(context, subject.scopePath, chapter.chapter, game)
                                    downloads[chapter.chapter] = DownloadState.IDLE
                                }
                                else -> scope.launch {
                                    downloads[chapter.chapter] = DownloadState.DOWNLOADING
                                    val ok = GameSetsRepository.download(context, subject.scopePath, chapter.chapter, game)
                                    downloads[chapter.chapter] = if (ok) DownloadState.DOWNLOADED else DownloadState.FAILED
                                }
                            }
                        }
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

/**
 * Chapter card: glass card like the other lists, "Play" / "Coming soon", and the learning download gauge on the right.
 * [bestLine] (null = hidden) is a small line under the title with the best stars of the last used level.
 */
@Composable
private fun ChapterCard(
    title: String,
    available: Boolean,
    downloadState: DownloadState?,
    bestLine: String?,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    title,
                    color = WisdomInk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (bestLine != null) {
                    Text(
                        bestLine,
                        color = WisdomCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Text(
                if (available) "Play" else "Coming soon",
                color = if (available) WisdomCyan else WisdomMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            if (downloadState != null) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onDownloadClick),
                    contentAlignment = Alignment.Center
                ) {
                    DownloadGauge(state = downloadState, progress = null)
                }
            }
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
