package com.wisdomtower.academy.ui.study

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * U3b (docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): study time that counts ONLY while the reader is really in front.
 *
 * The notes reader and the PDF reader used to add wall-clock time, so a phone left open (screen off, app in the
 * background, phone asleep) added hours to "Total hours". This timer runs only between the screen's ON_RESUME and
 * ON_PAUSE (which also happen when the screen turns off or the app goes to the background), and [take] hands out the
 * counted seconds at most [take]'s `maxSeconds` per call (the readers use 600 = 10 minutes per save).
 *
 * Plain words: [take] returns the seconds counted since the last [take] and starts again from zero.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
class ActiveStudyTimer {
    private var runningSince = 0L // elapsedRealtime when counting started, 0 = not counting
    private var bankedMs = 0L // counted before the last pause, not handed out yet

    @Synchronized
    fun resume() {
        if (runningSince == 0L) runningSince = SystemClock.elapsedRealtime()
    }

    @Synchronized
    fun pause() {
        if (runningSince != 0L) {
            bankedMs += SystemClock.elapsedRealtime() - runningSince
            runningSince = 0L
        }
    }

    /** Seconds counted since the last call (0 to [maxSeconds]); counting continues from zero if it was running. */
    @Synchronized
    fun take(maxSeconds: Int): Int {
        val now = SystemClock.elapsedRealtime()
        var total = bankedMs
        if (runningSince != 0L) {
            total += now - runningSince
            runningSince = now
        }
        bankedMs = 0L
        return (total / 1000L).toInt().coerceIn(0, maxSeconds)
    }
}

/**
 * One [ActiveStudyTimer] for a reader screen. It starts counting when the screen is resumed (or already is) and stops
 * when the screen is paused or leaves the composition. [key] makes a new timer per note / book.
 */
@Composable
fun rememberActiveStudyTimer(key: Any?): ActiveStudyTimer {
    val timer = remember(key) { ActiveStudyTimer() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, timer) {
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) timer.resume()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> timer.resume()
                Lifecycle.Event.ON_PAUSE -> timer.pause()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            timer.pause()
        }
    }
    return timer
}
