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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.wisdomtower.academy.data.games.GameLevel
import com.wisdomtower.academy.data.games.GameResultInput
import com.wisdomtower.academy.data.games.GameResultsRepository
import com.wisdomtower.academy.data.games.GameSaveOutcome
import com.wisdomtower.academy.data.games.GameSetData
import com.wisdomtower.academy.data.games.GameSetsRepository
import com.wisdomtower.academy.data.games.MatchCard
import com.wisdomtower.academy.data.games.MatchEngine
import com.wisdomtower.academy.data.games.MatchPhase
import com.wisdomtower.academy.data.games.MatchSelectResult
import com.wisdomtower.academy.data.games.MatchTuning
import com.wisdomtower.academy.ui.games.common.GameAudio
import com.wisdomtower.academy.ui.games.common.GameFullScreenEffect
import com.wisdomtower.academy.ui.games.common.GameHaptics
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Match game screen (docs/GAMES_HUB_PLAN.md step 4, animated by docs/GAMES_FEEL_PLAN.md slice M2,
 * HUD and result by slice M3, sound mapping by slice P1).
 * Takes a loaded [GameSetData] with match pairs and [onExit].
 *
 * The engine swaps matched cards for new ones at once (M1). The screen keeps its own `shown`
 * list so the matched pair can pulse green and fade out first, and the new cards then take the
 * same two slots and scale in. Every other card stays where it is.
 * `attempt` restarts the game (new engine, fresh screen state) when the player taps Retry.
 *
 * P1 sound map: tap = Tap + light tap | match = Correct (Combo from a streak of 2) + correct buzz |
 * wrong pair = Wrong + wrong buzz | result screen shown = Victory.
 *
 * 2026-10-22 fix: the first-tapped card now stays clearly highlighted until its pair is tapped
 * (tapping the same card again clears it). The selection is mirrored into Compose state
 * (`selectedId`) because the engine's plain field did not trigger recomposition of the grid cards,
 * and the highlight colours were made stronger.
 *
 * Slice G3 (levels): [level] picks the [MatchTuning]. Pairs on the board come from the engine; Easy shows which card was
 * the right partner after a wrong pair; Hard shows a timer against par (pairs x seconds) and uses it for the 3rd star
 * (see MatchResult). 10 or more cards use 3 columns so the board does not scroll.
 *
 * Slice R2 (result saver): when [scopePath] is not blank, the finished game is saved once through
 * [GameResultsRepository.save] (stars from [matchStars], score from [matchScore]) and the result screen shows the
 * save line. A blank [scopePath] saves nothing.
 */
@Composable
fun MatchScreen(
    set: GameSetData,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    level: GameLevel = GameLevel.MEDIUM,
    scopePath: String = ""
) {
    GameFullScreenEffect()
    val context = LocalContext.current
    val view = LocalView.current
    var attempt by remember(set.id) { mutableIntStateOf(0) }
    val tuning = remember(level) { MatchTuning.forLevel(level) }
    val engine = remember(set.id, attempt, level) { MatchEngine(set.matchPairs, tuning = tuning) }
    val reduceMotion = remember { GameTheme.reduceMotion(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { GameAudio.init(context) }
    DisposableEffect(Unit) { onDispose { GameAudio.release() } }

    var tick by remember { mutableIntStateOf(0) }
    var shown by remember(set.id, attempt, level) { mutableStateOf(engine.board) }
    var leaving by remember(set.id, attempt, level) { mutableStateOf<Set<Int>>(emptySet()) }
    var wrongIds by remember(set.id, attempt, level) { mutableStateOf<List<Int>>(emptyList()) }
    // Easy: the card that was the right partner of the first tapped card of a wrong pair (shown briefly).
    var hintId by remember(set.id, attempt, level) { mutableStateOf<Int?>(null) }
    var busy by remember(set.id, attempt, level) { mutableIntStateOf(0) }
    // Compose-state copy of engine.selectedId so the highlighted card recomposes right away.
    var selectedId by remember(set.id, attempt, level) { mutableStateOf<Int?>(null) }
    // Hard: seconds since the game started (stops when the set is complete).
    var elapsed by remember(set.id, attempt, level) { mutableFloatStateOf(0f) }
    // R2: result of saving this finished game (null = still saving or not saved).
    var saveOutcome by remember(set.id, attempt, level) { mutableStateOf<GameSaveOutcome?>(null) }
    val canSave = scopePath.isNotBlank()

    // A wrong pair shakes and glows red, then resets by itself.
    LaunchedEffect(wrongIds) {
        if (wrongIds.isNotEmpty()) {
            delay(450)
            wrongIds = emptyList()
        }
    }
    // The Easy hint stays a little longer than the shake.
    LaunchedEffect(hintId) {
        if (hintId != null) {
            delay(900)
            hintId = null
        }
    }

    BackHandler(onBack = onExit)

    @Suppress("UNUSED_VARIABLE")
    val revision = tick
    val phase = engine.phase
    val finished = phase is MatchPhase.Complete && busy == 0
    val gameDone = phase is MatchPhase.Complete
    val timed = tuning.parSecondsPerPair != null

    LaunchedEffect(set.id, attempt, level, timed, gameDone) {
        if (timed) {
            while (!gameDone) {
                delay(100)
                elapsed += 0.1f
            }
        }
    }

    // P1: the result screen plays the fanfare once per finished game (Retry resets `finished`).
    LaunchedEffect(finished, attempt) {
        if (finished) GameAudio.play(GameAudio.Sfx.Victory)
    }

    // R2: save the finished game once (Retry starts a new attempt, which saves its own result).
    LaunchedEffect(finished, attempt, level) {
        if (finished && canSave && phase is MatchPhase.Complete) {
            val stars = matchStars(phase.pairsTotal, phase.moves, tuning, elapsed.toInt())
            val score = matchScore(phase.pairsTotal, phase.moves)
            saveOutcome = GameResultsRepository.save(
                context,
                GameResultInput(
                    game = GameSetsRepository.GAME_MATCH,
                    scopePath = scopePath,
                    chapter = set.chapter,
                    level = level,
                    stars = stars,
                    score = score,
                    won = true
                )
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        if (finished && phase is MatchPhase.Complete) {
            MatchResult(
                pairsTotal = phase.pairsTotal,
                moves = phase.moves,
                bestStreak = engine.bestStreak,
                reduceMotion = reduceMotion,
                onRetry = { attempt++ },
                onDone = onExit,
                tuning = tuning,
                elapsedSeconds = elapsed.toInt(),
                showSave = canSave,
                saveOutcome = saveOutcome
            )
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
                MatchProgressBar(
                    matched = engine.matchedCount,
                    total = engine.pairsTotal,
                    streak = engine.streak,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                val parSeconds: Int? = tuning.parSecondsPerPair?.let { it * engine.pairsTotal }
                if (parSeconds != null) {
                    val secs = elapsed.toInt()
                    Text(
                        "Time ${formatMatchTime(secs)}  /  par ${formatMatchTime(parSeconds)}",
                        color = if (secs > parSeconds) Color(0xFFF43F5E) else WisdomMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))

                // 10 or more cards on the board (Hard): 3 columns so everything fits without scrolling.
                val compact = tuning.pairsOnBoard * 2 >= 10
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (compact) 3 else 2),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(shown, key = { it.id }) { card ->
                        val isLeaving = card.id in leaving
                        MatchCardView(
                            text = card.text,
                            selected = selectedId == card.id,
                            wrong = card.id in wrongIds,
                            hint = hintId == card.id,
                            leaving = isLeaving,
                            compact = compact,
                            reduceMotion = reduceMotion,
                            onClick = {
                                if (isLeaving) return@MatchCardView
                                wrongIds = emptyList()
                                hintId = null
                                when (engine.select(card.id)) {
                                    MatchSelectResult.Highlighted -> {
                                        GameAudio.play(GameAudio.Sfx.Tap)
                                        GameHaptics.tap(view, true)
                                    }
                                    MatchSelectResult.Matched,
                                    MatchSelectResult.MatchedAndFinished -> {
                                        // Combo chirp from a streak of 2; the last pair keeps the plain
                                        // Correct sound because the Victory fanfare follows on the result screen.
                                        val lastPair = engine.phase is MatchPhase.Complete
                                        if (engine.streak >= 2 && !lastPair) {
                                            GameAudio.play(GameAudio.Sfx.Combo)
                                        } else {
                                            GameAudio.play(GameAudio.Sfx.Correct)
                                        }
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
                                        if (tuning.wrongPairHint) {
                                            val firstId = engine.lastWrongIds.firstOrNull()
                                            val first = engine.board.find { it.id == firstId }
                                            hintId = if (first == null) {
                                                null
                                            } else {
                                                engine.board.find { it.pairId == first.pairId && it.id != first.id }?.id
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                                // Mirror the engine's selection (set on first tap, null after the same
                                // card is tapped again, a match or a wrong pair).
                                selectedId = engine.selectedId
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
    hint: Boolean,
    leaving: Boolean,
    compact: Boolean,
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
    val lift by animateFloatAsState(if (selected) 1.06f else 1f, tween(if (reduceMotion) 0 else 120), label = "matchLift")

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
        hint -> green
        wrong -> Color(0xFFF43F5E)
        selected -> WisdomCyan
        else -> Color(0x33FFFFFF)
    }
    val baseBg = when {
        hint -> green.copy(alpha = 0.30f)
        selected -> Color(0x6622E0FF)
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
            .border(if (selected || leaving || hint) 4.dp else 2.dp, borderColor, shape)
            .clickable(enabled = !leaving, onClick = onClick)
            .padding(if (compact) 6.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = WisdomInk,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = if (compact) 5 else 6,
            overflow = TextOverflow.Ellipsis
        )
    }
}
