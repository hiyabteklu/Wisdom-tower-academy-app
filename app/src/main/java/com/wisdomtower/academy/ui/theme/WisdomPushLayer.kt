package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.zIndex

/** Holds the last non-null item so the screen keeps its content while it slides out. Not Compose state on purpose. */
private class LastItem<T>(var value: T?)

/**
 * M5 (docs/UI_MOTION_PLAN.md): push / pop motion for a full-screen overlay (study workspace, guides, competitions).
 *
 * V2: the incoming screen makes a full-width spring push. A restrained black scrim adds depth to the screen underneath.
 * Closing reverses the same spring, keeping the LAST item composed until the transition finishes.
 *
 * - `graphicsLayer` only (alpha, translationX), read in the draw phase: no recomposition per frame.
 * - Touch works from the first frame (a layer at alpha 0 still receives taps).
 * - This file never changes the state that decides visibility; back handling stays where it is.
 */
@Composable
fun <T : Any> WisdomPushLayer(
    item: T?,
    zIndexValue: Float,
    content: @Composable (T) -> Unit
) {
    val last = remember { LastItem<T>(item) }
    if (item != null) last.value = item
    val wanted = item != null
    val progress = remember { Animatable(if (wanted) 1f else 0f) }
    var shown by remember { mutableStateOf(wanted) }

    LaunchedEffect(wanted) {
        if (wanted) {
            shown = true
            progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PushStiffness))
        } else {
            progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PopStiffness))
            shown = false
            last.value = null
        }
    }

    val current = last.value
    if (current != null && (wanted || shown)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(zIndexValue)
                .graphicsLayer {
                    translationX = (1f - progress.value) * size.width
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - progress.value) * 0.15f))
            )
            content(current)
        }
    }
}


/**
 * Recedes the currently selected tab underneath a full-screen push: 30% left shift
 * and 15% dim, driven by one spring and drawn through graphicsLayer.
 */
@Composable
fun Modifier.wisdomPushRecede(active: Boolean): Modifier {
    val progress = remember { Animatable(if (active) 1f else 0f) }
    LaunchedEffect(active) {
        progress.animateTo(
            if (active) 1f else 0f,
            spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PushStiffness)
        )
    }
    return graphicsLayer {
        val p = progress.value
        translationX = -size.width * 0.30f * p
        alpha = 1f - 0.15f * p
    }
}
