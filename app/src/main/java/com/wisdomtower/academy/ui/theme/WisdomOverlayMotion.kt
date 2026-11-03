package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * M7 (docs/UI_MOTION_PLAN.md): motion helpers for the drawer and dialogs.
 * `graphicsLayer` only, read in the draw phase (no recomposition per frame). Content is tappable from the first frame.
 */

/**
 * A drawer row rises in from the left: fade 0 to 1 and a 14 dp slide, 220 ms, [stepMs] apart per row (first 10 rows),
 * starting [baseDelayMs] after the drawer opens so the panel is already sliding. Unlike `wisdomReveal` this plays EVERY time
 * the row is composed, i.e. every time the drawer opens (its content is removed when it closes).
 */
@Composable
fun Modifier.wisdomStaggerIn(index: Int, stepMs: Int = 30, baseDelayMs: Int = 90): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val slot = if (index in 0..9) index else 9
        progress.animateTo(
            1f,
            tween(durationMillis = 220, delayMillis = baseDelayMs + slot * stepMs, easing = FastOutSlowInEasing)
        )
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationX = (p - 1f) * 14.dp.toPx()
    }
}

/**
 * A dialog card appears: scale 0.94 to 1 and fade, 200 ms. Apply to the root card inside `Dialog { }`.
 * Closing is still the platform's instant dismiss (a Dialog window cannot be animated out from inside).
 */
@Composable
fun Modifier.wisdomDialogEnter(): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 300f))
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        val s = 0.94f + 0.06f * p
        scaleX = s
        scaleY = s
    }
}


/** Spring arrival for a bottom sheet surface. Apply to the sheet's outer card/container. */
@Composable
fun Modifier.wisdomSheetEnter(): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 300f))
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * size.height * 0.12f
    }
}
