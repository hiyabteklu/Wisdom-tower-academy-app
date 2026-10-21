package com.wisdomtower.academy.ui.games.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Frame loop for games. [onFrame] receives dt in seconds, clamped to at most 0.05s.
 * Pauses when [running] is false or the app leaves the foreground.
 */
@Composable
fun GameFrameLoop(
    running: Boolean,
    onFrame: (dt: Float) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> foreground = true
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_PAUSE -> foreground = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val active = running && foreground
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                var dt = (now - last) / 1_000_000_000f
                if (dt > 0.05f) dt = 0.05f
                if (dt > 0f) onFrame(dt)
            }
            last = now
        }
    }
}
