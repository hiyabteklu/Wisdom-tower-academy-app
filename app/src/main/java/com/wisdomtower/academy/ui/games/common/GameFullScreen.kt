package com.wisdomtower.academy.ui.games.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Full-screen switch for the games (docs/GAMES_HUB_PLAN.md, step 3).
 *
 * [active] is true while a game screen that calls [GameFullScreenEffect] is on screen. The bottom navigation bar of
 * MainActivity must be hidden while it is true (that one line is added at the switchover, see the plan, section 9).
 * Nothing reads it yet.
 *
 * STATUS: written, NOT compiled, NOT tested, used by no screen yet.
 */
object GameFullScreen {
    var active by mutableStateOf(false)
        private set

    internal fun set(value: Boolean) {
        active = value
    }
}

/**
 * Call this once at the top of a game screen. While it is composed the status bar and the system navigation bar are
 * hidden (a swipe from the edge shows them for a moment); when the screen leaves they come back.
 */
@Composable
fun GameFullScreenEffect() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findGameHostActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        GameFullScreen.set(true)
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            GameFullScreen.set(false)
        }
    }
}

private fun Context.findGameHostActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
