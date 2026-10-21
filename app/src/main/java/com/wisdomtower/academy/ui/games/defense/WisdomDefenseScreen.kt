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
import com.wisdomtower.academy.data.games.GameSetData
import com.wisdomtower.academy.data.games.WisdomDefenseEngine
import com.wisdomtower.academy.data.games.WisdomDefensePhase
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
 */
@Composable
fun WisdomDefenseScreen(
    set: GameSetData,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameFullScreenEffect()
    val context = LocalContext.current
    val view = LocalView.current
    // Retry starts a fresh engine and fresh effects for the same set.
    var attempt by remember { mutableIntStateOf(0) }
    val engine = remember(set.id, attempt) { WisdomDefenseEngine(set.defenseItems) }
    // Questions at the start = number of enemies to defeat (the engine has just been created).
    val total = remember(set.id, attempt) { engine.questionsLeft }
    val lowEnd = remember { GameTheme.isLowEnd(context) }
    val reduceMotion = remember { GameTheme.reduceMotion(context) }
    val fx = remember(set.id, attempt) { DefenseEffects(GameParticleSystem.create(context), lowEnd, reduceMotion) }
    fx.dens = LocalDensity.current.density
    var frame by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }
    val questionSlide = remember { Animatable(1f) }

    LaunchedEffect(Unit) { GameAudio.init(context) }
    DisposableEffect(Unit) { onDispose { GameAudio.release() } }

    BackHandler(onBack = onExit)

    val playing = engine.phase is WisdomDefensePhase.Playing
    GameFrameLoop(running = playing) { dt ->
        engine.tick(dt)
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
                DefenseResult(
                    won = won,
                    killed = killedCount,
                    total = total,
                    heartsLeft = heartsLeft,
                    maxHearts = engine.maxHearts,
                    reduceMotion = reduceMotion,
                    onRetry = { attempt++ },
                    onDone = onExit
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
                        val animFrac by animateFloatAsState(targetValue = frac, animationSpec = tween(300))
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

                            // The world shakes when the wall is hit; the overlay does not.
                            val shake = fx.shakeOffset(clk)
                            translate(left = shake.x, top = shake.y) {
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
                                    drawDefenseEnemy(e.kind, x, foot, u, clk, e.id)
                                    if (e === nearest && fx.wrongFlashT > 0f) {
                                        drawCircle(
                                            GameTheme.wrong.copy(alpha = 0.4f * fx.wrongFlashT / DefenseEffects.WRONG_FLASH_TIME),
                                            u * 1.5f,
                                            Offset(x, foot - u * 1.2f)
                                        )
                                    }
                                }

                                // Death: white flash, shrink and fade (burst comes from the effects).
                                for (d in engine.dying) {
                                    val t = d.t
                                    val x = defenseLaneX(w, d.id, d.progress)
                                    val foot = defenseLaneY(h, d.progress)
                                    val u = defenseUnit(w, h, d.progress) * (1f - 0.35f * t)
                                    drawDefenseEnemy(
                                        d.kind, x, foot, u, clk, d.id,
                                        alpha = 1f - t,
                                        flash = (1f - t * 2.5f).coerceIn(0f, 1f)
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

                    // 2x2 answer cards.
                    val choices = q?.choices.orEmpty()
                    if (choices.size == 4) {
                        val firing = engine.shotProgress != null
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
                                            onClick = {
                                                if (engine.fire(index)) {
                                                    fx.fireMuzzle()
                                                    GameAudio.play(GameAudio.Sfx.Shot)
                                                    GameHaptics.tap(view, true)
                                                    frame++
                                                }
                                            },
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
    @Suppress("UNUSED_EXPRESSION")
    frame

    val killed = engine.killed
    val hearts = engine.hearts
    LaunchedEffect(killed) {
        if (killed > 0) {
            GameAudio.play(GameAudio.Sfx.Hit)
            GameHaptics.correct(view, true)
        }
    }
    LaunchedEffect(hearts) {
        if (hearts < engine.maxHearts) {
            GameAudio.play(GameAudio.Sfx.HeartLost)
            GameHaptics.heartLost(view, true)
        }
    }
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
