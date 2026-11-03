package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * M3 (docs/UI_MOTION_PLAN.md): fade-through transition for the five bottom tabs (iOS tab bars dissolve, they do not slide).
 *
 * V2 cross-dissolve: incoming content starts fading immediately and rises 6 dp. Outgoing content remains opaque until
 * the incoming layer passes 60 percent, then fades quickly. This prevents the dark gap caused by two sequential fades.
 *
 * - An opaque WisdomNavy backdrop stays behind the fading content, so the WebView underneath can never show through.
 * - [active] = false (the Learning tab while a hub page is open in the WebView) removes the layer at once, no fade.
 * - Everything animates through `graphicsLayer` (alpha, translationY) read in the draw phase: no recomposition per frame.
 * - The incoming layer is drawn on top (zIndex 11 vs 10), so it takes the touches from the first frame.
 * - Tab selection stays owned by `selectedIndex` in MainActivity; this file never changes it.
 */
@Composable
fun WisdomTabLayer(
    tab: Int,
    selectedIndex: Int,
    active: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSelectedTab = selectedIndex == tab
    val wanted = isSelectedTab && active
    val fade = remember { Animatable(if (wanted) 1f else 0f) }
    // true from the moment this tab was shown until its fade-out has finished (covers the first frame after a switch)
    var shown by remember { mutableStateOf(wanted) }

    LaunchedEffect(wanted, isSelectedTab) {
        if (wanted) {
            shown = true
            fade.animateTo(1f, tween(durationMillis = 220, easing = LinearOutSlowInEasing))
        } else if (isSelectedTab) {
            // the tab is still selected but its native screen is hidden on purpose (hub page open): no fade
            fade.snapTo(0f)
            shown = false
        } else {
            fade.animateTo(0f, tween(durationMillis = 88, delayMillis = 132, easing = FastOutLinearInEasing))
            shown = false
        }
    }

    if (wanted || (shown && !isSelectedTab)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (wanted) 11f else 10f)
                .appBackdrop()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = fade.value
                        translationY = if (wanted) (1f - fade.value) * 6.dp.toPx() else 0f
                    }
            ) {
                content()
            }
        }
    }
}
