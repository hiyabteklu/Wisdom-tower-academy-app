package com.wisdomtower.academy.ui.games.defense

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
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
    val engine = remember(set.id) { WisdomDefenseEngine(set.defenseItems) }
    val lowEnd = remember { GameTheme.isLowEnd(context) }
    val reduceMotion = remember { GameTheme.reduceMotion(context) }
    val fx = remember(set.id) { DefenseEffects(GameParticleSystem.create(context), lowEnd, reduceMotion) }
    fx.dens = LocalDensity.current.density
    var frame by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }

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

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        when (val phase = engine.phase) {
            is WisdomDefensePhase.Won, is WisdomDefensePhase.Lost -> {
                val title = if (phase is WisdomDefensePhase.Won) "Empire held!" else "Empire breached"
                val detail = when (phase) {
                    is WisdomDefensePhase.Won -> "${phase.killed} defeated · ${phase.heartsLeft} hearts left"
                    is WisdomDefensePhase.Lost -> "${phase.killed} defeated"
                    else -> ""
                }
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, color = WisdomInk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(detail, color = WisdomMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                    GamePrimaryButton("Done", onClick = onExit, modifier = Modifier.fillMaxWidth())
                }
            }
            is WisdomDefensePhase.Playing -> {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Exit",
                            color = WisdomCyan,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onExit).padding(8.dp)
                        )
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

                    val q = engine.current
                    Text(
                        text = q?.question ?: "…",
                        color = WisdomInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

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

                        val choices = engine.current?.choices.orEmpty()
                        if (choices.size == 4) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 28.dp, start = 8.dp, end = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    choices.forEachIndexed { index, label ->
                                        ChoiceBullet(
                                            label = label,
                                            onClick = {
                                                if (engine.fire(index)) {
                                                    fx.fireMuzzle()
                                                    GameAudio.play(GameAudio.Sfx.Shot)
                                                    GameHaptics.tap(view, true)
                                                    frame++
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "Killed ${engine.killed}",
                        color = WisdomMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
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

@Composable
private fun ChoiceBullet(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 78.dp, height = 78.dp)
            .clip(CircleShape)
            .background(Color(0xCC0E3A4A))
            .border(2.dp, WisdomCyan, CircleShape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = WisdomInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 4
        )
    }
}
