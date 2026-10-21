package com.wisdomtower.academy.ui.games.match

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameSetData
import com.wisdomtower.academy.data.games.MatchEngine
import com.wisdomtower.academy.data.games.MatchPhase
import com.wisdomtower.academy.data.games.MatchSelectResult
import com.wisdomtower.academy.ui.games.common.GameAudio
import com.wisdomtower.academy.ui.games.common.GameFullScreenEffect
import com.wisdomtower.academy.ui.games.common.GameHaptics
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Match game screen (docs/GAMES_HUB_PLAN.md step 4).
 * Takes a loaded [GameSetData] with match pairs and [onExit].
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

    LaunchedEffect(Unit) { GameAudio.init(context) }
    DisposableEffect(Unit) { onDispose { GameAudio.release() } }

    var tick by remember { mutableIntStateOf(0) }
    var wrongFlashId by remember { mutableStateOf<Int?>(null) }

    BackHandler(onBack = onExit)

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        when (val phase = engine.phase) {
            is MatchPhase.Complete -> {
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
            }
            is MatchPhase.Playing -> {
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
                        items(engine.board, key = { it.id }) { card ->
                            val selected = engine.selectedId == card.id
                            val wrong = wrongFlashId == card.id
                            MatchCardView(
                                text = card.text,
                                selected = selected,
                                wrong = wrong,
                                onClick = {
                                    wrongFlashId = null
                                    when (engine.select(card.id)) {
                                        MatchSelectResult.Highlighted -> {
                                            GameAudio.play(GameAudio.Sfx.Tap)
                                            GameHaptics.tap(view, true)
                                        }
                                        MatchSelectResult.Matched,
                                        MatchSelectResult.MatchedAndFinished -> {
                                            GameAudio.play(GameAudio.Sfx.Correct)
                                            GameHaptics.correct(view, true)
                                        }
                                        MatchSelectResult.Wrong -> {
                                            GameAudio.play(GameAudio.Sfx.Wrong)
                                            GameHaptics.wrong(view, true)
                                            wrongFlashId = card.id
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
    @Suppress("UNUSED_EXPRESSION")
    tick
}

@Composable
private fun MatchCardView(
    text: String,
    selected: Boolean,
    wrong: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        wrong -> Color(0xFFF43F5E)
        selected -> WisdomCyan
        else -> Color(0x33FFFFFF)
    }
    val bg = when {
        selected -> Color(0x3322E0FF)
        wrong -> Color(0x33F43F5E)
        else -> Color(0x22121A2A)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
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
