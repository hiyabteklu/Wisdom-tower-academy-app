package com.wisdomtower.academy.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.GameSetData
import com.wisdomtower.academy.data.games.GameSetsRepository
import com.wisdomtower.academy.data.games.GameSetsResult
import com.wisdomtower.academy.ui.games.match.MatchScreen
import com.wisdomtower.academy.ui.packages.NativePackage
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.launch

/**
 * Game tab root (docs/GAMES_HUB_PLAN.md switchover, section 9).
 * Shows [GameHubScreen]; on Match chapter play loads the set and opens [MatchScreen].
 * [onSelectPackage] is kept so MainActivity's existing call site still compiles; the new hub does not use it.
 */
@Composable
fun GameTabScreen(
    packageList: List<NativePackage>,
    onSelectPackage: (NativePackage) -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedSelect = onSelectPackage

    var activeSet by remember { mutableStateOf<GameSetData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { GameLaunch.kind = null }

    BackHandler(enabled = activeSet != null || error != null) {
        activeSet = null
        error = null
        loading = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            activeSet != null -> {
                MatchScreen(
                    set = activeSet!!,
                    onExit = { activeSet = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
            loading -> {
                AtmosphereBackground(modifier = Modifier.fillMaxSize())
                CircularProgressIndicator(
                    color = WisdomCyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                AtmosphereBackground(modifier = Modifier.fillMaxSize())
                Text(
                    text = error ?: "",
                    color = WisdomMuted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
            else -> {
                GameHubScreen(
                    packageList = packageList,
                    modifier = Modifier.fillMaxSize(),
                    onPlay = { game, scopePath, chapter ->
                        if (game != GameSetsRepository.GAME_MATCH) {
                            error = "Wisdom Defense is coming soon."
                            return@GameHubScreen
                        }
                        loading = true
                        error = null
                        scope.launch {
                            when (val result = GameSetsRepository.loadSet(
                                context, scopePath, chapter, GameSetsRepository.GAME_MATCH
                            )) {
                                is GameSetsResult.Success -> {
                                    val set = result.value
                                    if (set == null || set.matchPairs.isEmpty()) {
                                        error = "No Match set for this chapter yet."
                                        loading = false
                                    } else {
                                        activeSet = set
                                        loading = false
                                    }
                                }
                                is GameSetsResult.Failure -> {
                                    error = result.message
                                    loading = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

/** Kept for any code that still writes GameLaunch.kind from the old Climb/Defense tab flow. */
object GameLaunch {
    var kind by mutableStateOf<String?>(null)
}
