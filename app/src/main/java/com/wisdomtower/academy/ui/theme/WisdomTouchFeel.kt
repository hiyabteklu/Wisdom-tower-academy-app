package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration

/**
 * V7 sweep (docs/IOS_MOTION_V2_PLAN.md): press feel for a control that already has its own plain `.clickable`.
 *
 * Put it at the START of the modifier chain (`Modifier.wisdomTouchFeel().clip(...).background(...).clickable { }`) so the
 * background and border scale together with the content. It only WATCHES the touch (Initial pass, nothing is consumed), so the
 * clickable, scrolling and every other gesture work exactly as before.
 * - finger down: scale to 0.97 (same tokens as `wisdomPress`);
 * - finger up without leaving the touch slop: spring back and one light tick (same haptic as `wisdomPress`);
 * - finger moved away (scroll or drag): spring back, no tick.
 * Scale only, read in the draw phase through `graphicsLayer`.
 */
fun Modifier.wisdomTouchFeel(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) WisdomMotionV2.PressScale else 1f,
        animationSpec = if (pressed) tween(WisdomMotionV2.PressInMs)
        else spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "wisdomTouchFeelScale"
    )
    val haptic = LocalHapticFeedback.current
    val slop = LocalViewConfiguration.current.touchSlop
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                var released = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        released = true
                        break
                    }
                    if ((change.position - down.position).getDistance() > slop) break
                }
                pressed = false
                if (released) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
}
