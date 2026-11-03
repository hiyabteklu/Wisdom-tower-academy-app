package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * V7 (docs/IOS_MOTION_V2_PLAN.md, "Selection"): a small spring pop when a switch or checkbox changes.
 * Nothing happens on the first draw. When [selected] becomes true the control starts at [from] and springs to full size
 * (damping 0.55, a small bounce on a small thing); when it becomes false it just rests at full size.
 * Scale only, read in the draw phase through `graphicsLayer`; the control's own click and state are not touched.
 */
fun Modifier.wisdomSelectionPop(selected: Boolean, from: Float = 0.8f): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val firstDraw = remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (firstDraw.value) {
            firstDraw.value = false
        } else if (selected) {
            scale.snapTo(from)
            scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 500f))
        } else {
            scale.snapTo(1f)
        }
    }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
