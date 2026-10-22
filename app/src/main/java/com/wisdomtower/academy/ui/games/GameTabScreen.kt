package com.wisdomtower.academy.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.games.GameSetData
import com.wisdomtower.academy.data.games.GameSetsRepository
import com.wisdomtower.academy.data.games.GameSetsResult
import com.wisdomtower.academy.ui.games.defense.WisdomDefenseScreen
import com.wisdomtower.academy.ui.games.match.MatchScreen
import com.wisdomtower.academy.ui.packages.NativePackage
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.launch

/**
 * Game tab root (docs/GAMES_HUB_PLAN.md switchover).
 * Hosts [GameHubScreen]; Match and Wisdom Defense load sets and open their screens.
 *
 * 2026-10-22: the course list is now only the student's own packages (same rule as the My Learning tab,
 * see [enrolledGamePackages]), read from the signed-in profile, instead of the whole catalog.
 */
@Composable
fun GameTabScreen(
    packageList: List<NativePackage>,
    onSelectPackage: (NativePackage) -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedSelect = onSelectPackage

    val profile by AuthStateManager.currentProfile.collectAsState()
    val enrolledPackages = remember(packageList, profile?.educationLevel) {
        enrolledGamePackages(profile?.educationLevel, packageList)
    }

    var activeMatch by remember { mutableStateOf<GameSetData?>(null) }
    var activeDefense by remember { mutableStateOf<GameSetData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { GameLaunch.kind = null }

    BackHandler(enabled = activeMatch != null || activeDefense != null || error != null) {
        activeMatch = null
        activeDefense = null
        error = null
        loading = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            activeMatch != null -> {
                MatchScreen(
                    set = activeMatch!!,
                    onExit = { activeMatch = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
            activeDefense != null -> {
                WisdomDefenseScreen(
                    set = activeDefense!!,
                    onExit = { activeDefense = null },
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
                    packageList = enrolledPackages,
                    modifier = Modifier.fillMaxSize(),
                    onPlay = { game, scopePath, chapter ->
                        if (game != GameSetsRepository.GAME_MATCH && game != GameSetsRepository.GAME_DEFENSE) {
                            error = "Unknown game."
                            return@GameHubScreen
                        }
                        loading = true
                        error = null
                        scope.launch {
                            when (val result = GameSetsRepository.loadSet(
                                context, scopePath, chapter, game
                            )) {
                                is GameSetsResult.Success -> {
                                    val set = result.value
                                    when (game) {
                                        GameSetsRepository.GAME_MATCH -> {
                                            if (set == null || set.matchPairs.isEmpty()) {
                                                error = "No Match set for this chapter yet."
                                            } else {
                                                activeMatch = set
                                            }
                                        }
                                        GameSetsRepository.GAME_DEFENSE -> {
                                            if (set == null || set.defenseItems.isEmpty()) {
                                                error = "No Defense set for this chapter yet."
                                            } else {
                                                activeDefense = set
                                            }
                                        }
                                    }
                                    loading = false
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

object GameLaunch {
    var kind by mutableStateOf<String?>(null)
}
