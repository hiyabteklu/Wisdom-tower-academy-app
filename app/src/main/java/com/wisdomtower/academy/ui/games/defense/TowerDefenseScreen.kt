package com.wisdomtower.academy.ui.games.defense

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.CourseLocks
import com.wisdomtower.academy.data.content.LearningResource
import com.wisdomtower.academy.data.games.DefenseEngine
import com.wisdomtower.academy.data.games.DefenseEvent
import com.wisdomtower.academy.data.games.DefenseMode
import com.wisdomtower.academy.data.games.DefensePhase
import com.wisdomtower.academy.data.games.DefensePowerUp
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

@Composable
fun TowerDefenseScreen(
    scopePath: String,
    courseTitle: String,
    locks: CourseLocks?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stage by remember { mutableStateOf<DefStage>(DefStage.Picker) }
    when (val s = stage) {
        is DefStage.Picker -> ExamPickerScreen(
            scopePath = scopePath, courseTitle = courseTitle, locks = locks,
            onBack = onBack,
            onStart = { mode, exams -> stage = DefStage.Loading(mode, exams) },
            modifier = modifier
        )
        is DefStage.Loading -> DefenseLoading(
            exams = s.exams, mode = s.mode,
            onReady = { pool -> stage = DefStage.Play(s.mode, s.exams, pool) },
            onFail = { msg -> stage = DefStage.Error(msg) },
            onBack = { stage = DefStage.Picker },
            modifier = modifier
        )
        is DefStage.Error -> Box(modifier.fillMaxSize().background(Color(0xFF070C16)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(s.message, color = WisdomTextPrimary)
                Spacer(Modifier.height(12.dp))
                GamePrimaryButton("Back", onClick = { stage = DefStage.Picker })
            }
        }
        is DefStage.Play -> DefensePlayHost(
            scopePath = scopePath, mode = s.mode, exams = s.exams, pool = s.pool,
            onExit = { stage = DefStage.Picker },
            onFinished = { result -> stage = DefStage.Result(s.mode, s.exams, s.pool, result) },
            modifier = modifier
        )
        is DefStage.Result -> DefenseResultScreen(
            result = s.result,
            onPlayAgain = { stage = DefStage.Play(s.mode, s.exams, s.pool) },
            onBack = { stage = DefStage.Picker },
            modifier = modifier
        )
    }
}

private sealed class DefStage {
    object Picker : DefStage()
    data class Loading(val mode: DefenseMode, val exams: List<LearningResource>) : DefStage()
    data class Error(val message: String) : DefStage()
    data class Play(val mode: DefenseMode, val exams: List<LearningResource>, val pool: List<GameQuestion>) : DefStage()
    data class Result(
        val mode: DefenseMode, val exams: List<LearningResource>,
        val pool: List<GameQuestion>, val result: DefenseRunResult
    ) : DefStage()
}

@Composable
private fun DefenseLoading(
    exams: List<LearningResource>, mode: DefenseMode,
    onReady: (List<GameQuestion>) -> Unit, onFail: (String) -> Unit, onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(exams, mode) {
        val pool = ArrayList<GameQuestion>()
        for (exam in exams) {
            when (val r = GameQuestionLoader.load(context, exam)) {
                is GameQuestionLoader.LoadResult.Loaded -> pool.addAll(r.questions)
                is GameQuestionLoader.LoadResult.Locked -> { onFail(r.message); return@LaunchedEffect }
                is GameQuestionLoader.LoadResult.NeedsInternet -> {
                    onFail("Needs internet once to open this exam."); return@LaunchedEffect
                }
                is GameQuestionLoader.LoadResult.Failed -> { onFail(r.message); return@LaunchedEffect }
            }
        }
        if (pool.isEmpty()) onFail("No questions in selected exams.") else onReady(pool)
    }
    Box(modifier.fillMaxSize().background(Color(0xFF070C16)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = WisdomCyan)
            Text("Loading exams…", color = WisdomMuted, modifier = Modifier.padding(12.dp))
            Spacer(Modifier.height(8.dp))
            GamePrimaryButton("Cancel", onClick = onBack)
        }
    }
}

@Composable
private fun DefensePlayHost(
    scopePath: String, mode: DefenseMode, exams: List<LearningResource>, pool: List<GameQuestion>,
    onExit: () -> Unit, onFinished: (DefenseRunResult) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val engine = remember(pool, mode) { DefenseEngine(pool, mode) }
    var tick by remember { mutableStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(GameStore.getSettings(context)) }
    var selected by remember { mutableStateOf(-1) }
    var bolt by remember { mutableStateOf(-1f) }
    var finished by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "def")
    val phase by infinite.animateFloat(
        0f, (Math.PI * 2).toFloat(),
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    LaunchedEffect(Unit) {
        GameAudio.init(context)
        GameAudio.muted = !settings.soundOn
        engine.startCountdown()
    }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; GameAudio.release() }
    }

    BackHandler { if (paused) onExit() else paused = true }

    val finishRun: (Boolean) -> Unit = finish@ { victory ->
        if (finished) return@finish
        finished = true
        val examId = if (mode == DefenseMode.ExamRun && exams.isNotEmpty()) exams.first().id else "endless"
        val prev = GameStore.getDefense(context, scopePath, examId)
        val newBest = engine.score > prev.bestScore
        GameStore.recordDefenseRun(context, scopePath, examId, engine.score, engine.wavesCleared, engine.bestCombo)
        val missedQs = engine.missedIndices().mapNotNull { i -> pool.getOrNull(i) }
        onFinished(
            DefenseRunResult(
                score = engine.score, accuracy = engine.accuracy(), bestCombo = engine.bestCombo,
                wavesCleared = engine.wavesCleared, victory = victory, newBest = newBest, missed = missedQs
            )
        )
    }

    GameFrameLoop(running = !paused && !finished) { dt ->
        if (bolt >= 0f) {
            bolt += dt * 3f
            if (bolt > 1f) bolt = -1f
        }
        engine.tick(dt)
        for (ev in engine.consumeEvents()) {
            when (ev) {
                is DefenseEvent.Killed -> {
                    GameAudio.play(GameAudio.Sfx.Correct)
                    GameHaptics.correct(view, settings.vibrationOn)
                    bolt = 0f
                }
                is DefenseEvent.Hit -> if (ev.cracked) GameAudio.play(GameAudio.Sfx.Tap)
                is DefenseEvent.CitadelDamaged -> {
                    GameAudio.play(GameAudio.Sfx.Wrong)
                    GameHaptics.wrong(view, settings.vibrationOn)
                }
                is DefenseEvent.Victory -> { GameAudio.play(GameAudio.Sfx.Victory); finishRun(true) }
                is DefenseEvent.Defeat -> { GameAudio.play(GameAudio.Sfx.Defeat); finishRun(false) }
                is DefenseEvent.PowerUpUsed -> GameAudio.play(GameAudio.Sfx.Freeze)
                else -> {}
            }
        }
        tick++
    }

    Box(modifier.fillMaxSize().background(Color(0xFF070C16))) {
        when (val p = engine.phase) {
            is DefensePhase.Countdown -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = engine.countdownSeconds.toInt().coerceAtLeast(1).toString(),
                        color = WisdomCyan, fontSize = 72.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            is DefensePhase.Marching, is DefensePhase.Reveal -> {
                Column(Modifier.fillMaxSize()) {
                    DefenseCanvas(
                        citadelHealth = engine.citadelHealth,
                        lead = engine.leadEnemy(),
                        silhouettes = engine.silhouettes(),
                        wave = engine.wave,
                        phase = phase,
                        freezeActive = engine.freezeLeft > 0f,
                        boltProgress = bolt,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                    DefenseHud(
                        score = engine.score, wave = engine.wave, streak = engine.streak,
                        citadelHealth = engine.citadelHealth,
                        chronos = engine.chronosCharges, logic = engine.logicCharges,
                        fortify = engine.fortifyCharges, deflect = engine.deflectCharges,
                        freezeLeft = engine.freezeLeft,
                        onPowerUp = { type -> if (engine.usePowerUp(type)) tick++ }
                    )
                    val q = engine.currentQuestion()
                    if (q != null) {
                        val reveal = p as? DefensePhase.Reveal
                        GameQuestionCard(
                            card = GameCardState(
                                id = q.id, prompt = q.prompt, choices = q.choices,
                                selected = selected, enabled = reveal == null,
                                hidden = engine.hiddenChoiceIndices(),
                                revealCorrect = if (reveal != null) q.correctIndex else null,
                                revealSolution = if (reveal != null) q.solution else null
                            ),
                            onPick = { idx -> selected = idx; engine.pick(idx); tick++ },
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)
                        )
                    }
                    if (p is DefensePhase.Reveal) {
                        Box(Modifier.fillMaxWidth().padding(16.dp)) {
                            GamePrimaryButton(
                                "Next",
                                onClick = { engine.acknowledgeReveal(); selected = -1; tick++ },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            else -> {}
        }

        if (paused) {
            GamePauseOverlay(
                soundOn = settings.soundOn, vibrationOn = settings.vibrationOn,
                onResume = { paused = false },
                onToggleSound = {
                    val on = !settings.soundOn
                    GameStore.setSoundOn(context, on)
                    settings = settings.copy(soundOn = on)
                    GameAudio.muted = !on
                },
                onToggleVibration = {
                    val on = !settings.vibrationOn
                    GameStore.setVibrationOn(context, on)
                    settings = settings.copy(vibrationOn = on)
                },
                onQuit = onExit
            )
        }
    }
    @Suppress("UNUSED_EXPRESSION")
    tick
}
