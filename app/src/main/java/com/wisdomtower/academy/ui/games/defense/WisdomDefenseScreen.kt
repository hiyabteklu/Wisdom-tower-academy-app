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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.wisdomtower.academy.data.games.GameLevelPrefs
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
 * Cockpit (slice W3): the four answers and the cannon (DefenseCockpit.kt); the 2x2 cards below stay as the
 * fallback for very narrow screens (under COCKPIT_MIN_WIDTH).
 * Juice (slice W4): combo, banners and score numbers live in DefenseEffects.kt; the only hook here is the frame loop,
 * where the engine step is multiplied by fx.timeScale (slow-motion on the last kill).
 * Result v2 (slice W6): after a win on Easy or Medium the result offers "Next level". The screen keeps the level in
 * [currentLevel] (starts as [level]), so the next level starts here without the hub; results are saved with the level
 * that was played.
 * Slice W7b: a dying boss is drawn at boss size (the engine's dying enemy carries a boss flag).
 * Slice W7d: "Next level" also saves the new level as the last level used for this game ([GameLevelPrefs]), the same
 * value the level picker pre-selects, so the hub's "last level used" follows.
 * Slice W5 (power-ups, owner yes 2026-10-22): on Medium and Hard, 3 correct answers in a row earn a Freeze (enemies stand
 * still 3 s) and 5 in a row earn a Shield (absorbs one wall hit). The chip strip is [DefensePowerUps], laid over the
 * bottom-left of the battlefield (no layout change). A frozen field gets an icy tint and frame, an active Shield a
 * glowing line along the wall, and a blocked hit a short white flash. Rules live in [WisdomDefenseEngine].
 *
 * 2026-10-24 (one gun): the tower with the crystal on the wall is gone (DefenseWall.kt draws just the wall and a floor
 * grid). The only gun is the cannon in the cockpit: its barrel reaches up over the wall, turns toward the nearest
 * enemy (`aimRef` holds the angle, written here while the battlefield is drawn and read by the cockpit), kicks back and
 * flashes on every shot, and every shot starts at its muzzle ([defenseCannonTip]). The answers are two mirrored
 * columns of two on both sides of the cannon.
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
    // Cannon barrel angle in degrees (0 = straight up). Written while the battlefield is drawn, read by the cockpit.
    val aimRef = remember(set.id, attempt) { floatArrayOf(0f) }
    var frame by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }
    val questionSlide = remember { Animatable(1f) }
    // W5: short white flash on the wall when the Shield blocks a hit.
    val shieldFlash = remember(set.id, attempt, currentLevel) { Animatable(0f) }
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
                        // W7d: remember it as the last level used (the picker pre-selects this one next time).
                        GameLevelPrefs.save(context, GameSetsRepository.GAME_DEFENSE, nextLevel)
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
                    val questionShape = RoundedCornerShape(16.dp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .graphicsLayer {
                                alpha = questionSlide.value
                                translationY = (1f - questionSlide.value) * -24.dp.toPx()
                            }
                            .clip(questionShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xCC0B1A2C), Color(0xCC133350), Color(0xCC0B1A2C))
                                ),
                                shape = questionShape
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        WisdomCyan.copy(alpha = 0.15f),
                                        WisdomCyan.copy(alpha = 0.70f),
                                        WisdomCyan.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = questionShape
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = q?.question ?: "\u2026",
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

                    // Battlefield. The answer panels sit below it, so nothing covers the wall any more.
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
                                if (!lowEnd) drawDefenseGrid(w, h, clk)
                                drawDefenseWall(w, h, clk)

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

                                // Cannon aim: the barrel (drawn by the cockpit) turns toward the nearest enemy.
                                val cannonPivot = defenseCannonPivot(w, h)
                                val wantDeg = if (nearest != null) {
                                    val ex = defenseLaneX(w, nearest.id, nearest.progress)
                                    val ey = defenseLaneY(h, nearest.progress) - defenseUnit(w, h, nearest.progress)
                                    defenseAimDegrees(cannonPivot, ex, ey)
                                } else {
                                    0f
                                }
                                aimRef[0] += (wantDeg - aimRef[0]) * 0.22f

                                // Shot with a fading trail. Neutral colour: right or wrong shows on impact.
                                // It leaves the cannon muzzle, which is below the battlefield canvas (this draws a
                                // little outside the canvas, which Compose does not clip).
                                val sp = engine.shotProgress
                                if (sp != null && nearest != null) {
                                    val tx = defenseLaneX(w, nearest.id, nearest.progress)
                                    val tu = defenseUnit(w, h, nearest.progress)
                                    val ty = defenseLaneY(h, nearest.progress) - tu * 1.0f
                                    val tip = defenseCannonTip(w, h, aimRef[0])
                                    val fromX = tip.x
                                    val fromY = tip.y
                                    val core = 3.5.dp.toPx()
                                    // Layered, tapered energy trail: segmented beams read as motion better than
                                    // floating dots, while the white-hot head stays easy to track on the field.
                                    for (k in 8 downTo 1) {
                                        val tailT = (sp - k * 0.035f).coerceAtLeast(0f)
                                        val headT = (sp - (k - 1) * 0.035f).coerceAtLeast(0f)
                                        if (sp > 0f && headT > tailT) {
                                            val f = 1f - k / 9f
                                            val tail = Offset(
                                                fromX + (tx - fromX) * tailT,
                                                fromY + (ty - fromY) * tailT
                                            )
                                            val head = Offset(
                                                fromX + (tx - fromX) * headT,
                                                fromY + (ty - fromY) * headT
                                            )
                                            drawLine(
                                                color = WisdomCyan.copy(alpha = 0.18f + 0.62f * f),
                                                start = tail,
                                                end = head,
                                                strokeWidth = core * (0.35f + 0.65f * f)
                                            )
                                        }
                                    }
                                    val sx = fromX + (tx - fromX) * sp
                                    val sy = fromY + (ty - fromY) * sp
                                    val shot = Offset(sx, sy)
                                    drawCircle(WisdomCyan.copy(alpha = 0.16f), core * 3.2f, shot)
                                    drawCircle(WisdomCyan.copy(alpha = 0.48f), core * 1.8f, shot)
                                    drawCircle(Color.White.copy(alpha = 0.95f), core * 0.72f, shot)
                                    drawCircle(Color.White, core * 0.42f, shot)
                                }

                                fx.drawWorld(this)
                            }
                            fx.drawOverlay(this)

                            // W5: power-up overlays (they do not shake with the wall).
                            if (engine.freezeLeft > 0f) {
                                val pulse = if (reduceMotion) 0.10f else 0.10f + 0.04f * kotlin.math.sin(clk * 6f)
                                drawRect(Color(0xFF7DD3FC).copy(alpha = pulse.coerceIn(0f, 1f)))
                                drawRect(Color(0xFFBAE6FD).copy(alpha = 0.55f), style = Stroke(width = 3.dp.toPx()))
                            }
                            val wallY = defenseLaneY(h, 1f)
                            if (engine.shieldActive) {
                                val a = if (reduceMotion) 0.9f else 0.7f + 0.2f * kotlin.math.sin(clk * 5f)
                                drawLine(
                                    WisdomCyan.copy(alpha = 0.25f),
                                    Offset(w * 0.04f, wallY), Offset(w * 0.96f, wallY),
                                    strokeWidth = 14.dp.toPx()
                                )
                                drawLine(
                                    WisdomCyan.copy(alpha = a.coerceIn(0f, 1f)),
                                    Offset(w * 0.04f, wallY), Offset(w * 0.96f, wallY),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            val sf = shieldFlash.value
                            if (sf > 0f) {
                                drawRect(
                                    Color.White.copy(alpha = (0.30f * sf).coerceIn(0f, 1f)),
                                    topLeft = Offset(0f, (wallY - h * 0.12f).coerceAtLeast(0f)),
                                    size = Size(w, h * 0.24f)
                                )
                            }
                        }

                        // W5: power-up chips over the bottom-left of the battlefield (Medium and Hard only). They sit
                        // at the side so the cannon barrel does not cover them.
                        if (engine.powerUpsOn) {
                            DefensePowerUps(
                                streak = engine.streak,
                                freezeReady = engine.freezeReady,
                                freezeLeft = engine.freezeLeft,
                                shieldReady = engine.shieldReady,
                                shieldActive = engine.shieldActive,
                                onFreeze = {
                                    if (engine.useFreeze()) {
                                        GameAudio.play(GameAudio.Sfx.Correct)
                                        GameHaptics.tap(view, true)
                                        frame++
                                    }
                                },
                                onShield = {
                                    if (engine.useShield()) {
                                        GameAudio.play(GameAudio.Sfx.Correct)
                                        GameHaptics.tap(view, true)
                                        frame++
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 8.dp)
                            )
                        }
                    }

                    // Answers: the cockpit (cannon in the middle, two answers on each side) on normal widths, the old
                    // 2x2 cards on very narrow screens.
                    val choices = q?.choices.orEmpty()
                    if (choices.size == 4) {
                        val firing = engine.shotProgress != null
                        val fireChoice: (Int) -> Unit = { index ->
                            if (engine.fire(index)) {
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
                                    modifier = Modifier.padding(
                                        start = 12.dp,
                                        end = 12.dp,
                                        top = COCKPIT_TOP_PADDING,
                                        bottom = 12.dp
                                    ),
                                    aim = aimRef
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
    // W5: power-up earned = Combo chirp | power-up used = Correct chirp + tap | Shield blocks a hit = Hit + tap + flash
    val killed = engine.killed
    val hearts = engine.hearts
    val lockedCount = engine.lockedChoices.size
    val didWin = engine.phase is WisdomDefensePhase.Won
    val didLose = engine.phase is WisdomDefensePhase.Lost
    val freezeReady = engine.freezeReady
    val shieldReady = engine.shieldReady
    val shieldBlocks = engine.shieldBlocks
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
    LaunchedEffect(freezeReady) {
        if (freezeReady) GameAudio.play(GameAudio.Sfx.Combo)
    }
    LaunchedEffect(shieldReady) {
        if (shieldReady) GameAudio.play(GameAudio.Sfx.Combo)
    }
    LaunchedEffect(shieldBlocks) {
        if (shieldBlocks > 0) {
            GameAudio.play(GameAudio.Sfx.Hit)
            GameHaptics.tap(view, true)
            if (reduceMotion) {
                shieldFlash.snapTo(0f)
            } else {
                shieldFlash.snapTo(1f)
                shieldFlash.animateTo(0f, tween(450))
            }
        }
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
                    text = if (locked) "\u2715" else letter,
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
