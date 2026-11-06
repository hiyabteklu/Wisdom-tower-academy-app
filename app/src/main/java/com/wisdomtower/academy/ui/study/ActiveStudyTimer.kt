package com.wisdomtower.academy.ui.study

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * U3b + U3c (docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): study time that counts ONLY while the reader is really in front,
 * and focus time that counts only while the student is really reading.
 *
 * U3b: the notes reader and the PDF reader used to add wall-clock time, so a phone left open (screen off, app in the
 * background, phone asleep) added hours to "Total hours". This timer runs only between the screen's ON_RESUME and
 * ON_PAUSE (which also happen when the screen turns off or the app goes to the background).
 *
 * U3c, FOCUS IN PLAIN WORDS: a second counts as FOCUS when the student has touched, scrolled or turned a page within the
 * last [FOCUS_IDLE_MS] (3 minutes). So continuous reading with no pause longer than 3 minutes is all focus; after 3 minutes
 * with no touch the time still counts as study time but not as focus, until the next touch. Coming back to the screen
 * counts as a touch. Focus is never more than study time.
 *
 * Plain words: [takeSlice] returns the study seconds and focus seconds counted since the last call and starts again from
 * zero, at most `maxSeconds` per call (the readers use 600 = 10 minutes per save). [take] gives the study seconds only.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
const val FOCUS_IDLE_MS = 180_000L

/** Study seconds and the part of them that was focus (focusSeconds <= seconds). */
class StudySlice(val seconds: Int, val focusSeconds: Int)

class ActiveStudyTimer(private val idleLimitMs: Long = FOCUS_IDLE_MS) {
    private var running = false
    private var lastAccrue = 0L // elapsedRealtime of the last accounting while running
    private var lastTouch = 0L // elapsedRealtime of the last touch / scroll / page turn / resume
    private var activeMs = 0L // counted, not handed out yet
    private var focusMs = 0L // the part of activeMs that was focus

    /** Adds the time since the last accounting. Call only while running, inside a synchronized method. */
    private fun accrue(now: Long) {
        if (now <= lastAccrue) return
        activeMs += now - lastAccrue
        val focusEnd = minOf(now, lastTouch + idleLimitMs)
        if (focusEnd > lastAccrue) focusMs += focusEnd - lastAccrue
        lastAccrue = now
    }

    @Synchronized
    fun resume() {
        if (!running) {
            val now = SystemClock.elapsedRealtime()
            running = true
            lastAccrue = now
            lastTouch = now // coming back to the screen counts as a touch
        }
    }

    @Synchronized
    fun pause() {
        if (running) {
            accrue(SystemClock.elapsedRealtime())
            running = false
        }
    }

    /** The student touched, scrolled or turned a page. Ignored while the screen is not in front. */
    @Synchronized
    fun touch() {
        if (running) {
            val now = SystemClock.elapsedRealtime()
            accrue(now)
            lastTouch = now
        }
    }

    /** Study and focus seconds counted since the last call (study 0 to [maxSeconds], focus never above study). */
    @Synchronized
    fun takeSlice(maxSeconds: Int): StudySlice {
        if (running) accrue(SystemClock.elapsedRealtime())
        val seconds = (activeMs / 1000L).toInt().coerceIn(0, maxSeconds)
        val focus = (focusMs / 1000L).toInt().coerceIn(0, seconds)
        activeMs = 0L
        focusMs = 0L
        return StudySlice(seconds, focus)
    }

    /** Study seconds counted since the last call (0 to [maxSeconds]); the focus part is dropped. Kept from U3b. */
    fun take(maxSeconds: Int): Int = takeSlice(maxSeconds).seconds
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
