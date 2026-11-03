package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Keys that already played their entrance in this app session (main thread only). */
private val revealSeen = HashSet<String>()

/**
 * M4 (docs/UI_MOTION_PLAN.md section 4, "Entrance" + "Stagger"): a card rises in once.
 * fade 0 to 1, rise 24 dp to 0, scale 0.96 to 1, 320 ms ease-out; the first 8 items start 45 ms apart (max 315 ms of stagger).
 *
 * - Plays ONCE per [key] per app session: scrolling back, returning to a tab or recomposing never replays it.
 * - Only `graphicsLayer` (alpha, translationY, scale) read in the draw phase: no recomposition per frame.
 * - Never blocks touch: a layer at alpha 0 still receives taps, so content is usable from the first frame.
 * - [key] must be stable and unique for the item on its screen (e.g. "programs_row_3").
 */
@Composable
fun Modifier.wisdomReveal(key: String, index: Int = 0): Modifier {
    val alreadySeen = remember(key) { key in revealSeen }
    val progress = remember(key) { Animatable(if (alreadySeen) 1f else 0f) }
    LaunchedEffect(key) {
        if (!alreadySeen) {
            // marked at the start: if the item scrolls away mid-way and comes back it simply shows, it does not replay
            revealSeen.add(key)
            val stagger = if (index in 0..7) index * 45 else 0
            progress.animateTo(
                1f,
                tween(durationMillis = 320, delayMillis = stagger, easing = FastOutSlowInEasing)
            )
        }
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 24.dp.toPx()
        val s = 0.96f + 0.04f * p
        scaleX = s
        scaleY = s
    }
}


/**
 * Light return-visit entrance for a whole screen. Unlike item-level wisdomReveal,
 * this is attached to a screen root, so it replays when the tab/overlay is opened
 * again without replaying as individual lazy-list rows are recycled while scrolling.
 */
@Composable
fun Modifier.wisdomScreenEntrance(): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(durationMillis = WisdomMotionV2.ReturnEntranceMs, easing = FastOutSlowInEasing)
        )
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 10.dp.toPx()
    }
}
