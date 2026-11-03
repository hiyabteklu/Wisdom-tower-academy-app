package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * V7 (docs/IOS_MOTION_V2_PLAN.md): press feel for a control that already has its own click and its own haptic
 * (the top bar icons). Scale only: 0.97 in 90 ms while the finger is down, a soft spring back on release.
 * The finger is only WATCHED (Initial pass, never consumed), so the control's own click, ripple and haptic are unchanged
 * and no second vibration is added. Read in the draw phase through `graphicsLayer`.
 */
fun Modifier.wisdomPressScale(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) WisdomMotionV2.PressScale else 1f,
        animationSpec = if (pressed) tween(WisdomMotionV2.PressInMs)
        else spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "wisdomPressScaleOnly"
    )
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                pressed = false
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
