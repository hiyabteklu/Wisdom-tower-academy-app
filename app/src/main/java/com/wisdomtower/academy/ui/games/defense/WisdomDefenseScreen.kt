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
import androidx.compose.ui.platform.LocalContext
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
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Wisdom Defense cockpit screen (docs/GAMES_HUB_PLAN.md step 5).
 * Art (monsters, lane, empire) is drawn by DefenseArt.kt (docs/GAMES_FEEL_PLAN.md slice D2).
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
    var frame by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { GameAudio.init(context) }
    DisposableEffect(Unit) { onDispose { GameAudio.release() } }

    BackHandler(onBack = onExit)

    val playing = engine.phase is WisdomDefensePhase.Playing
    GameFrameLoop(running = playing) { dt ->
        engine.tick(dt)
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
                                Box(
                                    Modifier
                                        .padding(2.dp)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (on) Color(0xFFF43F5E) else Color(0x33FFFFFF))
                                )
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
                        Canvas(Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val clk = clock

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
                            }

                            // Death: white flash, shrink and fade (particles come in D3).
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

                            val sp = engine.shotProgress
                            if (sp != null && nearest != null) {
                                val tx = defenseLaneX(w, nearest.id, nearest.progress)
                                val tu = defenseUnit(w, h, nearest.progress)
                                val ty = defenseLaneY(h, nearest.progress) - tu * 1.0f
                                val fromX = w * 0.5f
                                val fromY = defenseMuzzleY(h)
                                val sx = fromX + (tx - fromX) * sp
                                val sy = fromY + (ty - fromY) * sp
                                val col = if (engine.lastShotCorrect == true) Color(0xFF10B981) else Color(0xFFF43F5E)
                                drawCircle(col.copy(alpha = 0.3f), 18f, Offset(sx, sy))
                                drawCircle(col, 8f, Offset(sx, sy))
                            }
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
