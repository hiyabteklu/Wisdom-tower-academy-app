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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import kotlin.math.sin

/**
 * Wisdom Defense cockpit screen (docs/GAMES_HUB_PLAN.md step 5).
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

    LaunchedEffect(Unit) { GameAudio.init(context) }
    DisposableEffect(Unit) { onDispose { GameAudio.release() } }

    BackHandler(onBack = onExit)

    val playing = engine.phase is WisdomDefensePhase.Playing
    GameFrameLoop(running = playing) { dt ->
        engine.tick(dt)
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
                            val empireY = h * 0.88f
                            val empireX = w * 0.5f

                            drawRect(
                                Color(0x2200E5FF),
                                topLeft = Offset(w * 0.35f, 0f),
                                size = Size(w * 0.3f, h)
                            )
                            drawCircle(WisdomCyan, radius = 18f, center = Offset(empireX, empireY))

                            for (e in engine.enemies) {
                                val y = h * (0.12f + e.progress * 0.7f)
                                val x = w * 0.5f + sin(e.id * 1.7f) * w * 0.08f
                                drawCircle(Color(0xFFF43F5E), radius = 14f, center = Offset(x, y))
                            }

                            val sp = engine.shotProgress
                            if (sp != null && engine.enemies.isNotEmpty()) {
                                val nearest = engine.enemies.maxByOrNull { it.progress }!!
                                val ty = h * (0.12f + nearest.progress * 0.7f)
                                val tx = w * 0.5f
                                val fromY = empireY - 40f
                                val y = fromY + (ty - fromY) * sp
                                val col = if (engine.lastShotCorrect == true) Color(0xFF10B981) else Color(0xFFF43F5E)
                                drawCircle(col, radius = 8f, center = Offset(tx, y))
                            }

                            val path = Path()
                            path.moveTo(0f, h)
                            path.quadraticBezierTo(w * 0.5f, h * 0.72f, w, h)
                            path.lineTo(w, h)
                            path.lineTo(0f, h)
                            path.close()
                            drawPath(path, Color(0x44070D17))
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
