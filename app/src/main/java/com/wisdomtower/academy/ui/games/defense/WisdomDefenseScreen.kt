package com.wisdomtower.academy.ui.games.defense

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameLevel
import com.wisdomtower.academy.data.games.GameResultInput
import com.wisdomtower.academy.data.games.GameResultsRepository
import com.wisdomtower.academy.data.games.GameSaveOutcome
import com.wisdomtower.academy.data.games.GameSetData
import com.wisdomtower.academy.data.games.GameSetsRepository
import com.wisdomtower.academy.data.games.WisdomDefenseEngine
import com.wisdomtower.academy.data.games.WisdomDefensePhase
import com.wisdomtower.academy.data.games.WisdomDefenseTuning
import com.wisdomtower.academy.ui.games.common.GameAudio
import com.wisdomtower.academy.ui.games.common.GameFrameLoop
import com.wisdomtower.academy.ui.games.common.GameFullScreenEffect
import com.wisdomtower.academy.ui.games.common.GameHaptics
import com.wisdomtower.academy.ui.games.common.GameParticleSystem
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Wisdom Defense cockpit screen (docs/GAMES_HUB_PLAN.md step 5).
 * Art (monsters, lane, empire) is drawn by DefenseArt.kt (docs/GAMES_FEEL_PLAN.md slice D2).
 * Effects (shot trail, particles, shield shatter, wall shake, vignette, floating text)
 * come from DefenseEffects.kt (slice D3).
 * UI (HUD with progress bar, question panel with slide, 2x2 answer cards) is slice D4.
 * The result screen (stars, stats, Retry and Done) is DefenseResult.kt (slice D5).
 * Sound and haptics mapping, reduce-motion on the HUD bar: slice P1 (see the block at the end).
 * Level (slice G2): [level] picks the numbers (hearts, enemies at once, speed, hint) from [WisdomDefenseTuning].
 * Result saver (slice R2): when [scopePath] is not blank, the finished game (won or lost) is saved once through
 * [GameResultsRepository.save] and the result screen shows the save line. A blank [scopePath] saves nothing.
 * Atmosphere (slice W1): nebula glow, drifting stars and lane glow in the level colour (DefenseAtmosphere.kt).
 * Enemies (slice W2): level skin, portal flash at the far edge, armor plates, Hard boss with a health bar (DefenseArt.kt).
 * Cockpit (slice W3): turret and four answer bullets in an arch (DefenseCockpit.kt); the 2x2 cards below stay as the
 * fallback for very narrow screens (under COCKPIT_MIN_WIDTH).
 * Juice (slice W4): combo, banners and score numbers live in DefenseEffects.kt; the only hook here is the frame loop,
 * where the engine step is multiplied by fx.timeScale (slow-motion on the last kill).
 * Result v2 (slice W6): after a win on Easy or Medium the result offers "Next level". The screen keeps the level in
 * [currentLevel] (starts as [level]), so the next level starts here without the hub; results are saved with the level
 * that was played. The hub's "last level used" is not updated by this.
 * Slice W7b: a dying boss is drawn at boss size (the engine's dying enemy carries a boss flag).
 */
@Composable
fun WisdomDefenseScreen(
    set: GameSetData,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    level: GameLevel = GameLevel.MEDIUM,
    scopePath: String = ""
) {
    GameFullScreenEffect()
    val context = LocalContext.current
    val view = LocalView.current
    // Retry starts a fresh engine and fresh effects for the same set (and the same level).
    var attempt by remember { mutableIntStateOf(0) }
    // W6: the level being played; "Next level" on the result changes it.
    var currentLevel by remember(level) { mutableStateOf(level) }
    val tuning = remember(currentLevel) { WisdomDefenseTuning.forLevel(currentLevel) }
    val engine = remember(set.id, attempt, currentLevel) {
        WisdomDefenseEngine(
            set.defenseItems,
            maxHearts = tuning.hearts,
            maxEnemies = tuning.maxEnemies,
            tuning = tuning
        )
    }
    // Questions at the start = number of enemies to defeat (the engine has just been created).
    val total = remember(set.id, attempt, currentLevel) { engine.questionsLeft }
    val lowEnd = remember { GameTheme.isLowEnd(context) }
    val reduceMotion = remember { GameTheme.reduceMotion(context) }
    val fx = remember(set.id, attempt) { DefenseEffects(GameParticleSystem.create(context), lowEnd, reduceMotion) }
    fx.dens = LocalDensity.current.density
    var frame by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }
    val questionSlide = remember { Animatable(1f) }
    // R2: result of saving this finished game (null = still saving or not saved).
    var saveOutcome by remember(set.id, attempt, currentLevel) { mutableStateOf<GameSaveOutcome?>(null) }
    val canSave = scopePath.isNotBlank()
    // W1: colour of the atmosphere for this level.
    val atmosphereTint = remember(currentLevel) { defenseAtmosphereTint(currentLevel) }
    // W2: enemy colours for this level.
    val skin = remember(currentLevel) { defenseSkin(currentLevel) }

    LaunchedEffect(Unit) { GameAudio.init(context) }
    DisposableEffect(Unit) { onDispose { GameAudio.release() } }

    BackHandler(onBack = onExit)

    val playing = engine.phase is WisdomDefensePhase.Playing
    GameFrameLoop(running = playing) { dt ->
        // W4: slow-motion on the last kill slows the game (engine) but not the effects or the clock.
        engine.tick(dt * fx.timeScale)
        fx.update(dt, engine)
        clock += dt
        frame++
    }

    // The question panel slides in whenever the question changes.
    val questionText = engine.current?.question
    LaunchedEffect(questionText) {
        if (reduceMotion) {
            questionSlide.snapTo(1f)
        } else {
            questionSlide.snapTo(0f)
            questionSlide.animateTo(1f, tween(280))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        when (val phase = engine.phase) {
            is WisdomDefensePhase.Won, is WisdomDefensePhase.Lost -> {
                val won = phase is WisdomDefensePhase.Won
                val killedCount = when (phase) {
                    is WisdomDefensePhase.Won -> phase.killed
                    is WisdomDefensePhase.Lost -> phase.killed
                    else -> 0
                }
                val heartsLeft = if (phase is WisdomDefensePhase.Won) phase.heartsLeft else 0
                // W6: "Next level" only after a win, and only while there is a harder level.
                val nextLevel = if (won) GameLevel.values().getOrNull(currentLevel.ordinal + 1) else null
                val goNext: (() -> Unit)? = if (nextLevel != null) {
                    {
                        currentLevel = nextLevel
                        attempt++
                    }
                } else null
                DefenseResult(
                    won = won,
                    killed = killedCount,
                    total = total,
                    heartsLeft = heartsLeft,
                    maxHearts = engine.maxHearts,
                    reduceMotion = reduceMotion,
                    onRetry = { attempt++ },
                    onDone = onExit,
                    levelLabel = currentLevel.label,
                    showSave = canSave,
                    saveOutcome = saveOutcome,
                    score = defenseScore(killedCount, heartsLeft, won, tuning.scoreMultiplier),
                    scoreMultiplier = tuning.scoreMultiplier,
                    nextLevelLabel = nextLevel?.label,
                    onNextLevel = goNext
                )
            }
            is WisdomDefensePhase.Playing -> {
                Column(Modifier.fillMaxSize()) {
                    // HUD: Exit, progress bar, hearts.
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Exit",
                            color = WisdomCyan,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onExit).padding(8.dp)
                        )
                        val frac = if (total > 0) (engine.killed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                        val animFrac by animateFloatAsState(
                            targetValue = frac,
                            animationSpec = tween(if (reduceMotion) 0 else 300),
                            label = "defenseProgress"
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                "${engine.killed} / $total",
                                color = WisdomMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(animFrac)
                                        .height(8.dp)
                                        .background(GameTheme.cyan)
                                )
                            }
                        }
                        Row {
                            repeat(engine.maxHearts) { i ->
                                val on = i < engine.hearts
                                val breaking = !on && i == engine.hearts && fx.heartBreakT > 0f
                                val bp = if (breaking) fx.heartBreakT / DefenseEffects.HEART_BREAK_TIME else 0f
                                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    if (breaking) {
                                        Box(
                                            Modifier
                                                .size((14f + 10f * (1f - bp)).dp)
                                                .clip(CircleShape)
                                                .background(GameTheme.heart.copy(alpha = bp))
                                        )
                                    }
                                    Box(
                                        Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(if (on) GameTheme.heart else Color(0x33FFFFFF))
                                    )
                                }
                            }
                        }
                    }

                    // Question panel (slides in for every new question).
                    val q = engine.current
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .graphicsLayer {
                                alpha = questionSlide.value
                                translationY = (1f - questionSlide.value) * -24.dp.toPx()
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x990E1A2B))
                            .border(1.dp, WisdomCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = q?.question ?: "…",
                            color = WisdomInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Easy level: the item's hint shows after the first wrong answer (engine.hint is null otherwise).
                    val hintText = engine.hint
                    if (!hintText.isNullOrBlank()) {
                        Text(
                            text = "Hint: $hintText",
                            color = WisdomCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)
                        )
                    }

                    // Battlefield. The answer cards sit below it, so nothing covers the wall any more.
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
                        Canvas(
                            Modifier
                                .fillMaxSize()
                                .onSizeChanged { fx.setSize(it.width.toFloat(), it.height.toFloat()) }
                        ) {
                            val w = size.width
                            val h = size.height
                            val clk = clock

                            // W1: sky behind everything (does not shake with the wall).
                            drawDefenseAtmosphere(w, h, clk, atmosphereTint, lowEnd, reduceMotion)

                            // The world shakes when the wall is hit; the overlay does not.
                            val shake = fx.shakeOffset(clk)
                            translate(left = shake.x, top = shake.y) {
                                drawDefenseLaneGlow(w, h, clk, atmosphereTint, reduceMotion)
                                drawDefenseField(w, h, clk)
                                drawDefenseEmpire(w, h, clk)

                                // Far enemies first, so near ones are drawn on top.
                                val ordered = engine.enemies.sortedBy { it.progress }
                                val nearest = ordered.lastOrNull()
                                for (e in ordered) {
                                    val x = defenseLaneX(w, e.id, e.progress)
                                    val foot = defenseLaneY(h, e.progress)
                                    val u = defenseUnit(w, h, e.progress)
                                    if (e === nearest) drawDefenseTargetRing(x, foot, u, clk)
                                    // W2: portal flash while the enemy is still at the far edge.
                                    val spawnT = if (e.progress < 0.10f) ((0.10f - e.progress) / 0.10f).coerceIn(0f, 1f) else 0f
                                    drawDefenseEnemy(
                                        e.kind, x, foot, u, clk, e.id,
                                        skin = skin,
                                        armor = e.armor,
                                        boss = e.boss,
                                        bossFrac = (e.armor + 1) / WisdomDefenseEngine.BOSS_HITS.toFloat(),
                                        spawnT = spawnT
                                    )
                                    if (e === nearest && fx.wrongFlashT > 0f) {
                                        drawCircle(
                                            GameTheme.wrong.copy(alpha = 0.4f * fx.wrongFlashT / DefenseEffects.WRONG_FLASH_TIME),
                                            u * 1.5f,
                                            Offset(x, foot - u * 1.2f)
                                        )
                                    }
                                }

                                // Death: white flash, shrink and fade (burst comes from the effects).
                                // W7b: a dying boss keeps its boss size (bar empty).
                                for (d in engine.dying) {
                                    val t = d.t
                                    val x = defenseLaneX(w, d.id, d.progress)
                                    val foot = defenseLaneY(h, d.progress)
                                    val u = defenseUnit(w, h, d.progress) * (1f - 0.35f * t)
                                    drawDefenseEnemy(
                                        d.kind, x, foot, u, clk, d.id,
                                        alpha = 1f - t,
                                        flash = (1f - t * 2.5f).coerceIn(0f, 1f),
                                        skin = skin,
                                        boss = d.boss,
                                        bossFrac = 0f
                                    )
                                }

                                // Shot with a fading trail. Neutral colour: right or wrong shows on impact.
                                val sp = engine.shotProgress
                                if (sp != null && nearest != null) {
                                    val tx = defenseLaneX(w, nearest.id, nearest.progress)
                                    val tu = defenseUnit(w, h, nearest.progress)
                                    val ty = defenseLaneY(h, nearest.progress) - tu * 1.0f
                                    val fromX = w * 0.5f
                                    val fromY = defenseMuzzleY(h)
                                    val core = 3.5.dp.toPx()
                                    for (k in 8 downTo 1) {
                                        val tp = sp - k * 0.03f
                                        if (tp > 0f) {
                                            val f = 1f - k / 9f
                                            drawCircle(
                                                WisdomCyan.copy(alpha = 0.55f * f),
                                                core * (0.3f + 0.7f * f),
                                                Offset(fromX + (tx - fromX) * tp, fromY + (ty - fromY) * tp)
                                            )
                                        }
                                    }
                                    val sx = fromX + (tx - fromX) * sp
                                    val sy = fromY + (ty - fromY) * sp
                                    drawCircle(WisdomCyan.copy(alpha = 0.35f), core * 2.4f, Offset(sx, sy))
                                    drawCircle(Color.White, core, Offset(sx, sy))
                                }

                                fx.drawWorld(this)
                            }
                            fx.drawOverlay(this)
                        }
                    }

                    // Answers: W3 cockpit (turret + four bullets in an arch) on normal widths, the old 2x2 cards on
                    // very narrow screens.
                    val choices = q?.choices.orEmpty()
                    if (choices.size == 4) {
                        val firing = engine.shotProgress != null
                        val fireChoice: (Int) -> Unit = { index ->
                            if (engine.fire(index)) {
                                fx.fireMuzzle()
                                GameAudio.play(GameAudio.Sfx.Shot)
                                GameHaptics.tap(view, true)
                                frame++
                            }
                        }
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            if (maxWidth >= COCKPIT_MIN_WIDTH) {
                                DefenseCockpit(
                                    choices = choices,
                                    locked = engine.lockedChoices,
                                    firingIndex = if (firing) engine.lastShotChoice else null,
                                    shotProgress = engine.shotProgress,
                                    clock = clock,
                                    reduceMotion = reduceMotion,
                                    onFire = fireChoice,
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
                                )
                            } else {
                                // 2x2 answer cards (fallback).
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (row in 0..1) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            for (col in 0..1) {
                                                val index = row * 2 + col
                                                AnswerCard(
                                                    letter = ('A' + index).toString(),
                                                    label = choices[index],
                                                    locked = index in engine.lockedChoices,
                                                    firing = firing && engine.lastShotChoice == index,
                                                    onClick = { fireChoice(index) },
                                                    modifier = Modifier.weight(1f).height(72.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Suppress("UNUSED_EXPRESSION")
    frame

    // P1 sound and haptics map:
    //   fire = Shot + light tap | kill = Hit + correct buzz | wrong answer (new locked card) = Wrong + wrong buzz
    //   heart lost = HeartLost + heart buzz (the sound is skipped on the last heart, Defeat plays instead)
    //   won = Victory | lost = Defeat
    val killed = engine.killed
    val hearts = engine.hearts
    val lockedCount = engine.lockedChoices.size
    val didWin = engine.phase is WisdomDefensePhase.Won
    val didLose = engine.phase is WisdomDefensePhase.Lost
    LaunchedEffect(killed) {
        if (killed > 0) {
            GameAudio.play(GameAudio.Sfx.Hit)
            GameHaptics.correct(view, true)
        }
    }
    LaunchedEffect(hearts) {
        if (hearts < engine.maxHearts) {
            if (hearts > 0) GameAudio.play(GameAudio.Sfx.HeartLost)
            GameHaptics.heartLost(view, true)
        }
    }
    LaunchedEffect(lockedCount) {
        if (lockedCount > 0) {
            GameAudio.play(GameAudio.Sfx.Wrong)
            GameHaptics.wrong(view, true)
        }
    }
    LaunchedEffect(didWin, didLose) {
        if (didWin) GameAudio.play(GameAudio.Sfx.Victory)
        else if (didLose) GameAudio.play(GameAudio.Sfx.Defeat)
    }

    // R2: save the finished game once (Retry or Next level starts a new attempt, which saves its own result).
    LaunchedEffect(didWin, didLose, attempt, currentLevel) {
        if ((didWin || didLose) && canSave) {
            val ph = engine.phase
            val won = ph is WisdomDefensePhase.Won
            val killedCount = when (ph) {
                is WisdomDefensePhase.Won -> ph.killed
                is WisdomDefensePhase.Lost -> ph.killed
                else -> 0
            }
            val heartsLeft = if (ph is WisdomDefensePhase.Won) ph.heartsLeft else 0
            // Same stars rule as DefenseResult: 3 hearts left = 3 stars, 2 = 2, 1 = 1, lost = 0.
            val stars = if (won) heartsLeft.coerceIn(1, 3) else 0
            saveOutcome = GameResultsRepository.save(
                context,
                GameResultInput(
                    game = GameSetsRepository.GAME_DEFENSE,
                    scopePath = scopePath,
                    chapter = set.chapter,
                    level = currentLevel,
                    stars = stars,
                    score = defenseScore(killedCount, heartsLeft, won, tuning.scoreMultiplier),
                    won = won
                )
            )
        }
    }
}

/**
 * Defense score for the saved best (a proposal, change after a phone test): 100 per enemy defeated, plus 50 per heart
 * left when won, times the level multiplier of [WisdomDefenseTuning], kept between 0 and 100000.
 */
private fun defenseScore(killed: Int, heartsLeft: Int, won: Boolean, multiplier: Float): Int {
    val base = killed * 100 + (if (won) heartsLeft * 50 else 0)
    return (base * multiplier).toInt().coerceIn(0, 100000)
}

/**
 * One answer card. States: normal, pressed (slightly smaller), firing (the shot for this answer
 * is in the air), locked (tried wrongly: greyed out with a red edge, cannot be tapped again).
 */
@Composable
private fun AnswerCard(
    letter: String,
    label: String,
    locked: Boolean,
    firing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    val bg = when {
        locked -> Color(0x33222A36)
        firing -> Color(0xCC14566B)
        else -> Color(0xCC0E3A4A)
    }
    val edge = when {
        locked -> GameTheme.wrong.copy(alpha = 0.55f)
        firing -> Color.White
        else -> WisdomCyan
    }
    val scale = if (pressed && !locked) 0.96f else 1f
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (locked) 0.55f else 1f
            }
            .clip(shape)
            .background(bg)
            .border(2.dp, edge, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = !locked,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (locked) Color(0x33FFFFFF) else WisdomCyan.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (locked) "✕" else letter,
                    color = if (locked) GameTheme.wrong else WisdomCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = label,
                color = if (locked) WisdomMuted else WisdomInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
