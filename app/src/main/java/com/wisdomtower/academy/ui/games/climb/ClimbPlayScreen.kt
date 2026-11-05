package com.wisdomtower.academy.ui.games.climb

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.wisdomtower.academy.data.content.CourseLocks
import com.wisdomtower.academy.data.games.ClimbFloor
import com.wisdomtower.academy.data.games.ClimbPowerUp
import com.wisdomtower.academy.data.games.ClimbRewards
import com.wisdomtower.academy.data.games.ClimbSession
import com.wisdomtower.academy.data.games.ClimbSessionState
import com.wisdomtower.academy.data.games.ClimbTowerBuilder
import com.wisdomtower.academy.data.games.GameQuestion
import com.wisdomtower.academy.data.games.GameQuestionLoader
import com.wisdomtower.academy.data.games.GameStore
import com.wisdomtower.academy.ui.games.common.GameAudio
import com.wisdomtower.academy.ui.games.common.GameCardState
import com.wisdomtower.academy.ui.games.common.GameFrameLoop
import com.wisdomtower.academy.ui.games.common.GameHaptics
import com.wisdomtower.academy.ui.games.common.GamePauseOverlay
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameQuestionCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch

@Composable
fun ClimbPlayScreen(
    scopePath: String,
    floor: ClimbFloor,
    locks: CourseLocks?,
    allFloorsForBoss: List<ClimbFloor>,
    onExitToTower: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<ClimbSession?>(null) }
    var tick by remember { mutableStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(GameStore.getSettings(context)) }
    var selectedChoice by remember { mutableStateOf(-1) }
    var correctFlash by remember { mutableStateOf(false) }
    var resultRecorded by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "play")
    val phase by infinite.animateFloat(
        0f, (Math.PI * 2).toFloat(),
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    fun rebuildSession(pool: List<GameQuestion>) {
        val climb = GameStore.getClimb(context, scopePath)
        val atticIds = climb.attic.map { it.questionId }.toSet()
        val already = (climb.floors[floor.key]?.stars ?: 0) > 0
        session = ClimbSession(floor, pool, atticIds, already)
        session?.start()
        selectedChoice = -1
        resultRecorded = false
        tick++
    }

    fun poolFromCache(): List<GameQuestion> {
        val banks = if (floor.isBoss) {
            allFloorsForBoss.filter { !it.sealed }.flatMap { ClimbTowerBuilder.openBanks(it, locks) }
        } else {
            ClimbTowerBuilder.openBanks(floor, locks)
        }
        val pool = ArrayList<GameQuestion>()
        for (b in banks) {
            GameQuestionLoader.cached(b.id)?.let { pool.addAll(it.first) }
        }
        return pool
    }

    LaunchedEffect(floor.key) {
        loading = true
        loadError = null
        GameAudio.init(context)
        GameAudio.setMuted(!settings.soundOn)
        val banks = if (floor.isBoss) {
            allFloorsForBoss.filter { !it.sealed }.flatMap { ClimbTowerBuilder.openBanks(it, locks) }
        } else {
            ClimbTowerBuilder.openBanks(floor, locks)
        }
        val pool = ArrayList<GameQuestion>()
        for (bank in banks) {
            when (val r = GameQuestionLoader.load(context, bank)) {
                is GameQuestionLoader.LoadResult.Loaded -> pool.addAll(r.questions)
                is GameQuestionLoader.LoadResult.Locked -> {
                    loadError = r.message; loading = false; return@LaunchedEffect
                }
                is GameQuestionLoader.LoadResult.NeedsInternet -> {
                    loadError = "Needs internet once to open this bank."; loading = false; return@LaunchedEffect
                }
                is GameQuestionLoader.LoadResult.Failed -> {
                    loadError = r.message; loading = false; return@LaunchedEffect
                }
            }
        }
        if (pool.isEmpty()) {
            loadError = "No questions available for this floor."; loading = false; return@LaunchedEffect
        }
        rebuildSession(pool)
        loading = false
    }

    LaunchedEffect(session?.state) {
        val st = session?.state
        if (st is ClimbSessionState.FloorComplete && !resultRecorded) {
            resultRecorded = true
            GameAudio.play(GameAudio.Sfx.Victory)
            GameStore.recordFloorClear(
                context, scopePath, floor.key,
                st.stars, st.accuracy, st.xpGained, session?.bestStreakInRun() ?: 0
            )
        } else if (st is ClimbSessionState.Failed && !resultRecorded) {
            resultRecorded = true
            GameAudio.play(GameAudio.Sfx.Defeat)
            GameStore.recordFloorFail(context, scopePath, floor.key)
        }
    }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            GameAudio.release()
        }
    }

    BackHandler {
        if (paused) onExitToTower() else paused = true
    }

    val s = session
    val state = s?.state

    GameFrameLoop(running = !paused && state is ClimbSessionState.Question) { dt ->
        if (s?.tick(dt) == true) {
            GameAudio.play(GameAudio.Sfx.Wrong)
            GameHaptics.wrong(view, settings.vibrationOn)
        }
        tick++
    }

    Box(modifier.fillMaxSize().background(Color(0xFF070C16))) {
        when {
            loading -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = WisdomCyan)
                    Text("Loading questions…", color = WisdomMuted, modifier = Modifier.padding(12.dp))
                }
            }
            loadError != null -> {
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError ?: "", color = WisdomTextPrimary)
                    Spacer(Modifier.height(16.dp))
                    GamePrimaryButton(label = "Back", onClick = onExitToTower)
                }
            }
            s != null && state != null -> {
                when (state) {
                    is ClimbSessionState.Loading -> {}
                    is ClimbSessionState.Question, is ClimbSessionState.Reveal -> {
                        val q = s.currentQuestion()
                        val qIndex = when (state) {
                            is ClimbSessionState.Question -> state.index
                            is ClimbSessionState.Reveal -> state.index
                            else -> 0
                        }
                        val timeLeft = (state as? ClimbSessionState.Question)?.timeLeft
                            ?: ClimbRewards.SECONDS_PER_QUESTION.toFloat()
                        Column(Modifier.fillMaxSize()) {
                            FloorSceneCanvas(
                                totalRungs = s.totalQuestions,
                                currentRung = qIndex + if (state is ClimbSessionState.Reveal && state.wasCorrect) 1 else 0,
                                phase = phase,
                                correctFlash = correctFlash,
                                modifier = Modifier.fillMaxWidth().height(180.dp)
                            )
                            ClimbHud(
                                hearts = s.heartsRemaining(),
                                maxHearts = s.maxHearts,
                                timeLeft = timeLeft,
                                maxTime = ClimbRewards.SECONDS_PER_QUESTION.toFloat(),
                                streak = s.currentStreak(),
                                questionIndex = qIndex,
                                totalQuestions = s.totalQuestions,
                                fiftyFifty = s.fiftyFiftyCharges(),
                                extraTime = s.extraTimeCharges(),
                                onFiftyFifty = {
                                    if (s.usePowerUp(ClimbPowerUp.FiftyFifty)) {
                                        GameAudio.play(GameAudio.Sfx.Tap); tick++
                                    }
                                },
                                onExtraTime = {
                                    if (s.usePowerUp(ClimbPowerUp.ExtraTime)) {
                                        GameAudio.play(GameAudio.Sfx.Freeze); tick++
                                    }
                                },
                                boss = s.isBoss
                            )
                            if (q != null) {
                                val reveal = state as? ClimbSessionState.Reveal
                                GameQuestionCard(
                                    card = GameCardState(
                                        id = q.id,
                                        prompt = q.prompt,
                                        choices = q.choices,
                                        selected = selectedChoice,
                                        enabled = reveal == null,
                                        hidden = s.hiddenChoices(),
                                        revealCorrect = if (reveal != null) q.correctIndex else null,
                                        revealSolution = if (reveal != null) q.solution else null
                                    ),
                                    onPick = { idx ->
                                        selectedChoice = idx
                                        s.pick(idx)
                                        val ok = idx == q.correctIndex
                                        correctFlash = ok
                                        if (ok) {
                                            GameAudio.play(GameAudio.Sfx.Correct)
                                            GameHaptics.correct(view, settings.vibrationOn)
                                        } else {
                                            GameAudio.play(GameAudio.Sfx.Wrong)
                                            GameHaptics.wrong(view, settings.vibrationOn)
                                            if (s.heartsRemaining() <= 0) {
                                                GameHaptics.heartLost(view, settings.vibrationOn)
                                            }
                                            val climb = GameStore.getClimb(context, scopePath)
                                            val existing = climb.attic.toMutableList()
                                            if (existing.none { it.questionId == q.id }) {
                                                existing.add(GameStore.AtticEntry(q.id, q.bankId, 0))
                                                GameStore.setAttic(context, scopePath, existing)
                                            }
                                        }
                                        tick++
                                    },
                                    modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)
                                )
                            }
                            if (state is ClimbSessionState.Reveal) {
                                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                                    GamePrimaryButton(
                                        label = "Got it",
                                        onClick = {
                                            s.acknowledge()
                                            selectedChoice = -1
                                            correctFlash = false
                                            tick++
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    is ClimbSessionState.FloorComplete -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            FloorResultSheet(
                                complete = state,
                                onNext = onExitToTower,
                                onReplay = {
                                    val pool = poolFromCache()
                                    if (pool.isNotEmpty()) rebuildSession(pool) else onExitToTower()
                                },
                                onBackToTower = onExitToTower,
                                hasNext = true
                            )
                        }
                    }
                    is ClimbSessionState.Failed -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            FloorFailSheet(
                                onRetry = {
                                    val pool = poolFromCache()
                                    if (pool.isNotEmpty()) rebuildSession(pool) else onExitToTower()
                                },
                                onBackToTower = onExitToTower
                            )
                        }
                    }
                }
            }
        }

        if (paused) {
            GamePauseOverlay(
                soundOn = settings.soundOn,
                vibrationOn = settings.vibrationOn,
                onResume = { paused = false },
                onToggleSound = {
                    val on = !settings.soundOn
                    GameStore.setSoundOn(context, on)
                    settings = settings.copy(soundOn = on)
                    GameAudio.setMuted(!on)
                },
                onToggleVibration = {
                    val on = !settings.vibrationOn
                    GameStore.setVibrationOn(context, on)
                    settings = settings.copy(vibrationOn = on)
                },
                onQuit = onExitToTower
            )
        }
    }
    @Suppress("UNUSED_EXPRESSION")
    tick
}
