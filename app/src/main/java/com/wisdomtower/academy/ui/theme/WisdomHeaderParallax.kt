package com.wisdomtower.academy.ui.theme

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * V10 header (docs/IOS_MOTION_V2_PLAN.md): the lazy item with [key] (a title or hero card at the top of a list) lags behind the scroll,
 * shrinks a little and fades while it leaves the top of the screen. At rest, and while the item is fully in place, nothing is applied.
 * Read in the draw phase through `graphicsLayer`, so scrolling does not recompose anything. [strength] scales all three effects.
 */
fun Modifier.wisdomHeaderParallax(listState: LazyListState, key: Any, strength: Float = 1f): Modifier =
    graphicsLayer {
        val layout = listState.layoutInfo
        val info = layout.visibleItemsInfo.firstOrNull { it.key == key }
        if (info != null && info.size > 0) {
            val scrolled = (layout.beforeContentPadding - info.offset).coerceAtLeast(0)
            if (scrolled > 0) {
                val gone = (scrolled.toFloat() / info.size.toFloat()).coerceIn(0f, 1f)
                translationY = scrolled * 0.35f * strength
                val s = 1f - 0.06f * gone * strength
                scaleX = s
                scaleY = s
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = (1f - 0.55f * gone * strength).coerceIn(0f, 1f)
            }
        }
    }
