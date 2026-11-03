package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Wisdom Motion: the ONE place for the app's motion values (docs/UI_MOTION_PLAN.md, section 4).
 * Calm, iOS-like. The open motion is a copy of the approved sign-in motion (expand + fade 280 ms, quicker close).
 * Starting values: tune on the phone, change them here and every screen follows.
 */
object WisdomMotion {
    const val RevealMs = 280
    const val ConcealMs = 220
    const val ConcealFadeMs = 160
    const val ColorMs = 240

    val RevealIn: EnterTransition =
        expandVertically(expandFrom = Alignment.Top, animationSpec = tween(RevealMs)) +
            fadeIn(animationSpec = tween(RevealMs))

    val ConcealOut: ExitTransition =
        shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(ConcealMs)) +
            fadeOut(animationSpec = tween(ConcealFadeMs))
}

/** A section that opens and closes smoothly (accordions, forms). Replaces a plain `if (open) { ... }`. */
@Composable
fun WisdomExpandable(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = WisdomMotion.RevealIn,
        exit = WisdomMotion.ConcealOut
    ) {
        content()
    }
}

/** Colour that glides between selected and not selected (chips, pills, borders, text). */
@Composable
fun wisdomAnimatedColor(selected: Boolean, selectedColor: Color, idleColor: Color): Color {
    val color by animateColorAsState(
        targetValue = if (selected) selectedColor else idleColor,
        animationSpec = tween(WisdomMotion.ColorMs),
        label = "wisdomAnimatedColor"
    )
    return color
}


/** Centralized V2 motion tokens for restrained, tactile interactions. */
object WisdomMotionV2 {
    const val PressInMs = 90
    const val PressScale = 0.97f
    const val ReturnEntranceMs = 180
    const val PushStiffness = 260f
    const val PopStiffness = 340f
}

/**
 * Adds a subtle press scale and light release haptic to an existing clickable surface.
 * The same interactionSource must be passed to the clickable/toggleable modifier.
 */
@Composable
fun Modifier.wisdomPress(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) WisdomMotionV2.PressScale else 1f,
        animationSpec = if (pressed) tween(WisdomMotionV2.PressInMs)
        else spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "wisdomPressScale"
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction: Interaction ->
            if (interaction is PressInteraction.Release) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
