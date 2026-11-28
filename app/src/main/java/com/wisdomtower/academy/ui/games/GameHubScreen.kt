package com.wisdomtower.academy.ui.games

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wisdomtower.academy.ui.theme.wisdomPressClickable
import com.wisdomtower.academy.ui.theme.wisdomScreenEntrance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.HubGate
import com.wisdomtower.academy.data.games.GameAccess
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
import com.wisdomtower.academy.ui.packages.eceCourseCover
import com.wisdomtower.academy.ui.packages.eceSemesterCover
import com.wisdomtower.academy.ui.packages.eceYearCover
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
 * Game hub. B3 (2026-11-28, docs/PLAN_2026-11-28_SIGNUP_AND_GAMES_NAV.md): the flow is now the SAME as My Learning:
 *   my course cards -> (ECE only: Semester 1 / Semester 2 cards) -> subject cards -> chapter list.
 * The GAME is chosen on the chapter row: each chapter shows one line for Wisdom Defense and one for Match, each with its
 * own "Play" / "Package required" / "Coming soon" label and its own download gauge. (Before, the first screen was a game
 * picker and the course list came second.) Chapters come from published `game_sets` rows; a chapter that has no set for
 * a game shows "Coming soon" on that game's line only.
 *
 * The position (course, semester, subject) is kept with rememberSaveable, so leaving the tab and coming back (or
 * rotating the phone) does not send the student back to the first screen. If the saved course is no longer one of the
 * student's courses (the level was changed on the Profile screen) the hub starts again from the first screen.
 *
 * ECE: a student's level maps to ONE ECE course card (see AcademicLevels.resolvePackages), shown as "ECE Year N". Its two
 * semesters are separate packages in the app (`ece-yN-sem-1`, `ece-yN-sem-2`) and the subjects come from the admin
 * content tree per semester ([GameHubIndex.knownSubjects]). BUG FIXED on the way: before, only the subjects of Semester 1
 * could be reached in Games (the card was the Semester 1 package), so no Semester 2 chapter could be played.
 * Subject pictures for ECE Years 1, 2, 4, 5 now come from [eceCourseCover] (same covers as My Learning); Year 3 keeps the
 * `SUBJECT_IMAGES` entries. Nothing from My Learning's screens was changed or moved (they stay as they are).
 *
 * Earlier notes (still true): course and subject steps use the same [OpenCard] two-column grid as the Learning tab; every
 * chapter game line that has a set shows the same [DownloadGauge] as the learning lists (tap: save offline, tap again when
 * saved: remove); a chapter that was played shows a small line "Medium ★★☆" (best stars kept on this phone for the level
 * used last in that game, [GameLevelPrefs.last]); a chapter the student may not open shows "Package required" /
 * "Coming soon" instead of "Play" and a tap calls [onLocked] (gate from [GameAccess.gates]; display only, the
 * authoritative check is done again in GameTabScreen before a set is loaded; docs/PLAN_2026-11-09_GAMES_OWNERSHIP.md).
 *
 * [onPlay] is called when the student taps a game line that has a set and may be opened.
 *
 * STATUS: [~] written, NOT compiled, NOT tested on a phone.
 */
@Composable
fun GameHubScreen(
    packageList: List<NativePackage>,
    modifier: Modifier = Modifier,
    onLocked: (gate: HubGate) -> Unit = {},
    onPlay: (game: String, scopePath: String, chapter: Int) -> Unit = { _, _, _ -> }
) {
    // Position of the student. Plain strings so that rememberSaveable can keep them.
    var pkgId by rememberSaveable { mutableStateOf<String?>(null) }
    var semId by rememberSaveable { mutableStateOf<String?>(null) }
    var subjectScope by rememberSaveable { mutableStateOf<String?>(null) }
    var subjectName by rememberSaveable { mutableStateOf("") }

    val pkg = pkgId?.let { id -> packageList.firstOrNull { it.id == id } }
    val year: Int? = pkg?.let { eceYearOf(it) }
    val semesters: List<GameSemester> = if (year != null) eceSemestersOf(year) else emptyList()
    val semester = if (year != null) semesters.firstOrNull { it.id == semId } else null
    val subject: GameHubSubject? = subjectScope?.let { GameHubSubject(subjectName, it) }

    // The saved course is not one of the student's courses any more (level changed): start again.
    LaunchedEffect(pkgId, packageList) {
        if (pkgId != null && packageList.none { it.id == pkgId }) {
            pkgId = null
            semId = null
            subjectScope = null
        }
    }

    BackHandler(enabled = pkg != null) {
        when {
            subject != null -> subjectScope = null
            semester != null -> semId = null
            else -> {
                pkgId = null
                semId = null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().wisdomScreenEntrance()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())
        when {
            pkg == null -> CourseStep(packageList, onPick = {
                pkgId = it.id
                semId = null
                subjectScope = null
            })
            year != null && semester == null -> SemesterStep(year, semesters, onPick = { semId = it.id })
            subject == null -> SubjectStep(
                kicker = if (year != null && semester != null) "Games \u00B7 ECE Year $year \u00B7 ${semester.label}" else "Games \u00B7 ${pkg.name}",
                packageId = semester?.id ?: pkg.id,
                onPick = {
                    subjectName = it.name
                    subjectScope = it.scopePath
                }
            )
            else -> ChapterStep(subject, onPlay, onLocked)
        }
    }
}

private fun gameTitle(game: String): String =
    if (game == GameSetsRepository.GAME_DEFENSE) "Wisdom Defense" else "Match"

/** The two games, in the order their lines are shown on a chapter. */
private val GAME_ORDER = listOf(GameSetsRepository.GAME_DEFENSE, GameSetsRepository.GAME_MATCH)

/** One semester card of an ECE year. [id] is the semester's package id (`ece-y3-sem-2`), the one the admin tree uses. */
private class GameSemester(val id: String, val label: String, val image: String)

private val ECE_PACKAGE_ID = Regex("^ece-y([1-5])-sem-[12]$")

/** 1..5 for an ECE package, null for every other package. */
private fun eceYearOf(pkg: NativePackage): Int? = ECE_PACKAGE_ID.find(pkg.id)?.groupValues?.get(1)?.toIntOrNull()

private fun eceSemestersOf(year: Int): List<GameSemester> =
    listOf(1, 2).map { GameSemester("ece-y$year-sem-$it", "Semester $it", eceSemesterCover(year, it)) }

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

/** Cover of a subject card: the packed lists first (Year 3 ECE too), then the ECE code covers (Years 1, 2, 4, 5). */
private fun subjectImage(scopePath: String, name: String): String? =
    SUBJECT_IMAGES[scopePath]
        ?: if (scopePath.startsWith("ece/")) eceCourseCover(scopePath.substringAfterLast('/'), cleanSubjectTitle(name)) else null

@Composable
private fun CourseStep(packageList: List<NativePackage>, onPick: (NativePackage) -> Unit) {
    StepColumn(kicker = "Games", title = "Choose a course") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            openGrid("game_courses", packageList) { pkg ->
                val year = eceYearOf(pkg)
                if (year != null) {
                    // One card per ECE year; the semester is chosen on the next step.
                    OpenCard("ECE Year $year", eceYearCover(year)) { onPick(pkg) }
                } else {
                    OpenCard(pkg.name, pkg.assetImage) { onPick(pkg) }
                }
            }
            item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SemesterStep(year: Int, semesters: List<GameSemester>, onPick: (GameSemester) -> Unit) {
    StepColumn(kicker = "Games \u00B7 ECE Year $year", title = "Choose a semester") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            openGrid("game_semesters_$year", semesters) { sem ->
                OpenCard(sem.label, sem.image) { onPick(sem) }
            }
            item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SubjectStep(kicker: String, packageId: String, onPick: (GameHubSubject) -> Unit) {
    val context = LocalContext.current
    var subjects by remember(packageId) { mutableStateOf<List<GameHubSubject>?>(null) }
    var failure by remember(packageId) { mutableStateOf<String?>(null) }
    LaunchedEffect(packageId) {
        val known = GameHubIndex.knownSubjects(packageId)
        if (known.isNotEmpty()) {
            subjects = known
        } else {
            when (val result = GameHubIndex.packageSubjects(context, packageId)) {
                is GameSetsResult.Success -> subjects = result.value
                is GameSetsResult.Failure -> failure = result.message
            }
        }
    }
    val list = subjects
    val error = failure
    StepColumn(kicker = kicker, title = "Choose a subject") {
        when {
            error != null -> StatusText(error)
            list == null -> StatusText("Loading\u2026")
            list.isEmpty() -> StatusText("Coming soon")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                openGrid("game_subjects_$packageId", list) { subject ->
                    OpenCard(cleanSubjectTitle(subject.name), subjectImage(subject.scopePath, subject.name)) { onPick(subject) }
                }
                item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ChapterStep(
    subject: GameHubSubject,
    onPlay: (String, String, Int) -> Unit,
    onLocked: (HubGate) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chapters by remember(subject.scopePath) { mutableStateOf<List<GameSetChapter>?>(null) }
    var failure by remember(subject.scopePath) { mutableStateOf<String?>(null) }
    // "<game>#<chapter>" -> download state; only game lines that have a set get an entry
    val downloads = remember(subject.scopePath) { mutableStateMapOf<String, DownloadState>() }
    // R3 part 2: the level used last in each game; the best line of a chapter is shown for this level.
    val lastLevels = remember { GAME_ORDER.associateWith { GameLevelPrefs.last(context, it) } }
    // Package ownership (plan 2026-11-09): chapter number -> gate. A chapter without an entry is treated as open for
    // display only; the tap is checked again before the set loads.
    var gates by remember(subject.scopePath) { mutableStateOf<Map<Int, HubGate>>(emptyMap()) }

    LaunchedEffect(subject.scopePath) {
        when (val result = GameSetsRepository.chapters(context, subject.scopePath)) {
            is GameSetsResult.Success -> chapters = result.value
            is GameSetsResult.Failure -> failure = result.message
        }
    }
    val list = chapters
    val error = failure

    LaunchedEffect(list) {
        if (list != null) {
            gates = GameAccess.gates(subject.scopePath, list.map { it.chapter })
        }
    }

    // Which game lines are already saved on the phone (checked once when the list appears)
    LaunchedEffect(list) {
        list?.forEach { chapter ->
            GAME_ORDER.forEach { game ->
                val available = if (game == GameSetsRepository.GAME_DEFENSE) chapter.hasDefense else chapter.hasMatch
                val key = "$game#${chapter.chapter}"
                if (available && downloads[key] == null) {
                    val saved = GameSetsRepository.isDownloaded(context, subject.scopePath, chapter.chapter, game)
                    downloads[key] = if (saved) DownloadState.DOWNLOADED else DownloadState.IDLE
                }
            }
        }
    }

    StepColumn(kicker = "Games \u00B7 ${cleanSubjectTitle(subject.name)}", title = "Choose a chapter") {
        when {
            error != null -> StatusText(error)
            list == null -> StatusText("Loading\u2026")
            list.isEmpty() -> StatusText("Coming soon")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.chapter }) { chapter ->
                    val gate = gates[chapter.chapter] ?: HubGate.Open
                    val label = if (chapter.title.isEmpty()) "Chapter ${chapter.chapter}"
                    else "Chapter ${chapter.chapter}: ${chapter.title}"
                    ChapterCard(title = label) {
                        GAME_ORDER.forEach { game ->
                            val available =
                                if (game == GameSetsRepository.GAME_DEFENSE) chapter.hasDefense else chapter.hasMatch
                            val lockedLabel: String? = if (available) GameAccess.label(gate) else null
                            val key = "$game#${chapter.chapter}"
                            val level = lastLevels.getValue(game)
                            val best = remember(chapter.chapter, game, level) {
                                GameResultsRepository.localBest(context, game, subject.scopePath, chapter.chapter, level)
                            }
                            val bestLine: String? = if (available && best != null && best.plays > 0) {
                                val stars = best.stars.coerceIn(0, 3)
                                level.label + " " + "\u2605".repeat(stars) + "\u2606".repeat(3 - stars)
                            } else {
                                null
                            }
                            GameLine(
                                name = gameTitle(game),
                                available = available,
                                lockedLabel = lockedLabel,
                                downloadState = if (available && lockedLabel == null) {
                                    downloads[key] ?: DownloadState.IDLE
                                } else {
                                    null
                                },
                                bestLine = bestLine,
                                onClick = {
                                    if (available) {
                                        if (lockedLabel != null) onLocked(gate)
                                        else onPlay(game, subject.scopePath, chapter.chapter)
                                    }
                                },
                                onDownloadClick = {
                                    when (downloads[key]) {
                                        DownloadState.DOWNLOADING -> Unit
                                        DownloadState.DOWNLOADED -> scope.launch {
                                            GameSetsRepository.removeDownload(context, subject.scopePath, chapter.chapter, game)
                                            downloads[key] = DownloadState.IDLE
                                        }
                                        else -> scope.launch {
                                            downloads[key] = DownloadState.DOWNLOADING
                                            val ok = GameSetsRepository.download(context, subject.scopePath, chapter.chapter, game)
                                            downloads[key] = if (ok) DownloadState.DOWNLOADED else DownloadState.FAILED
                                        }
                                    }
                                }
                            )
                        }
                    }
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

/** Chapter card: glass card like the other lists, the chapter title, then one [GameLine] per game. */
@Composable
private fun ChapterCard(title: String, games: @Composable () -> Unit) {
    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                title,
                color = WisdomInk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            games()
        }
    }
}

/**
 * One game of a chapter: its name, the best line ([bestLine], null = hidden), "Play" / "Package required" /
 * "Coming soon" ([lockedLabel] null = open), and the learning download gauge on the right (null = hidden).
 */
@Composable
private fun GameLine(
    name: String,
    available: Boolean,
    lockedLabel: String?,
    downloadState: DownloadState?,
    bestLine: String?,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = available, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                name,
                color = WisdomInk,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
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
            lockedLabel ?: if (available) "Play" else "Coming soon",
            color = if (available && lockedLabel == null) WisdomCyan else WisdomMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        if (downloadState != null) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .wisdomPressClickable(onClick = onDownloadClick),
                contentAlignment = Alignment.Center
            ) {
                DownloadGauge(state = downloadState, progress = null)
            }
        } else {
            // keeps the "Play" labels of all lines in one column
            Spacer(Modifier.padding(end = 48.dp))
        }
    }
}
