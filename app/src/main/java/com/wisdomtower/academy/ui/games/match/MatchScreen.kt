package com.wisdomtower.academy.ui.games.match

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameSetData
import com.wisdomtower.academy.data.games.MatchCard
import com.wisdomtower.academy.data.games.MatchEngine
import com.wisdomtower.academy.data.games.MatchPhase
import com.wisdomtower.academy.data.games.MatchSelectResult
import com.wisdomtower.academy.ui.games.common.GameAudio
import com.wisdomtower.academy.ui.games.common.GameFullScreenEffect
import com.wisdomtower.academy.ui.games.common.GameHaptics
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Match game screen (docs/GAMES_HUB_PLAN.md step 4, animated by docs/GAMES_FEEL_PLAN.md slice M2).
 * Takes a loaded [GameSetData] with match pairs and [onExit].
 *
 * The engine swaps matched cards for new ones at once (M1). The screen keeps its own `shown`
 * list so the matched pair can pulse green and fade out first, and the new cards then take the
 * same two slots and scale in. Every other card stays where it is.
 */
@Composable
fun MatchScreen(
    set: GameSetData,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameFullScreenEffect()
    val context = LocalContext.current
    val view = LocalView.current
    val engine = remember(set.id) { MatchEngine(set.matchPairs) }
    val reduceMotion = remember { GameTheme.reduceMotion(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { GameAudio.init(context) }
    DisposableEffect(Unit) { onDispose { GameAudio.release() } }

    var tick by remember { mutableIntStateOf(0) }
    var shown by remember(set.id) { mutableStateOf(engine.board) }
    var leaving by remember(set.id) { mutableStateOf<Set<Int>>(emptySet()) }
    var wrongIds by remember(set.id) { mutableStateOf<List<Int>>(emptyList()) }
    var busy by remember(set.id) { mutableIntStateOf(0) }

    // A wrong pair shakes and glows red, then resets by itself.
    LaunchedEffect(wrongIds) {
        if (wrongIds.isNotEmpty()) {
            delay(450)
            wrongIds = emptyList()
        }
    }

    BackHandler(onBack = onExit)

    @Suppress("UNUSED_VARIABLE")
    val revision = tick
    val phase = engine.phase
    val finished = phase is MatchPhase.Complete && busy == 0

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        if (finished && phase is MatchPhase.Complete) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (phase.pairsTotal == 0) "No pairs in this set" else "Set complete!",
                    color = WisdomInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                if (phase.pairsTotal > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${phase.pairsTotal} pairs · ${phase.moves} moves",
                        color = WisdomMuted,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(24.dp))
                GamePrimaryButton("Done", onClick = onExit, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Exit",
                        color = WisdomCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onExit).padding(8.dp)
                    )
                    Text(
                        set.chapterTitle.ifBlank { set.title },
                        color = WisdomInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${engine.matchedCount}/${engine.pairsTotal}",
                        color = WisdomMuted,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap a card, then its match",
                    color = WisdomMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(shown, key = { it.id }) { card ->
                        val isLeaving = card.id in leaving
                        MatchCardView(
                            text = card.text,
                            selected = engine.selectedId == card.id,
                            wrong = card.id in wrongIds,
                            leaving = isLeaving,
                            reduceMotion = reduceMotion,
                            onClick = {
                                if (isLeaving) return@MatchCardView
                                wrongIds = emptyList()
                                when (engine.select(card.id)) {
                                    MatchSelectResult.Highlighted -> {
                                        GameAudio.play(GameAudio.Sfx.Tap)
                                        GameHaptics.tap(view, true)
                                    }
                                    MatchSelectResult.Matched,
                                    MatchSelectResult.MatchedAndFinished -> {
                                        GameAudio.play(GameAudio.Sfx.Correct)
                                        GameHaptics.correct(view, true)
                                        val oldIds = engine.lastMatchedCards.map { it.id }
                                        val newCards: List<MatchCard> = engine.lastNewCardIds
                                            .mapNotNull { id -> engine.board.find { it.id == id } }
                                        leaving = leaving + oldIds
                                        busy++
                                        scope.launch {
                                            delay(if (reduceMotion) 0L else 520L)
                                            shown = shown.mapNotNull { c ->
                                                val i = oldIds.indexOf(c.id)
                                                if (i < 0) c else newCards.getOrNull(i)
                                            }
                                            leaving = leaving - oldIds.toSet()
                                            busy--
                                        }
                                    }
                                    MatchSelectResult.Wrong -> {
                                        GameAudio.play(GameAudio.Sfx.Wrong)
                                        GameHaptics.wrong(view, true)
                                        wrongIds = engine.lastWrongIds
                                    }
                                    else -> {}
                                }
                                tick++
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                GameSecondaryButton("Exit", onClick = onExit, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MatchCardView(
    text: String,
    selected: Boolean,
    wrong: Boolean,
    leaving: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current.density
    val shape = RoundedCornerShape(14.dp)

    // New cards scale in.
    val appear = remember { Animatable(if (reduceMotion) 1f else 0.6f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) appear.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }

    // Selected card lifts.
    val lift by animateFloatAsState(if (selected) 1.06f else 1f, tween(120), label = "matchLift")

    // Matched card: green pulse, then shrink and fade.
    val pulse = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }
    LaunchedEffect(leaving) {
        if (leaving) {
            if (reduceMotion) {
                exit.snapTo(0f)
            } else {
                pulse.animateTo(1f, tween(250))
                exit.animateTo(0f, tween(250))
            }
        }
    }

    // Wrong card: shake.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(wrong) {
        if (wrong && !reduceMotion) {
            repeat(3) {
                shake.animateTo(8f, tween(50))
                shake.animateTo(-8f, tween(50))
            }
            shake.animateTo(0f, tween(50))
        } else {
            shake.snapTo(0f)
        }
    }

    val green = GameTheme.correct
    val borderColor = when {
        leaving -> green
        wrong -> Color(0xFFF43F5E)
        selected -> WisdomCyan
        else -> Color(0x33FFFFFF)
    }
    val baseBg = when {
        selected -> Color(0x3322E0FF)
        wrong -> Color(0x33F43F5E)
        else -> Color(0x22121A2A)
    }
    val bg = lerp(baseBg, green.copy(alpha = 0.45f), pulse.value)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .graphicsLayer {
                val s = appear.value * lift * exit.value
                scaleX = s
                scaleY = s
                alpha = exit.value
                translationX = shake.value * density
            }
            .shadow(
                elevation = if (selected) 10.dp else 0.dp,
                shape = shape,
                ambientColor = WisdomCyan,
                spotColor = WisdomCyan
            )
            .clip(shape)
            .background(bg)
            .border(if (selected || leaving) 3.dp else 2.dp, borderColor, shape)
            .clickable(enabled = !leaving, onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = WisdomInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
    }
}
