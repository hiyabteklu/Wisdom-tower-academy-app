package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex

/**
 * V8 (docs/IOS_MOTION_V2_PLAN.md): host for the global native study tools (AI Tutor, Calculator, Timer, Planner,
 * Notebook, Targets, Status).
 *
 * - Arrival: the sheet springs up a little and fades in (the same spring as `wisdomSheetEnter`: damping 0.85, stiffness 300).
 * - Drag to close: when the content is scrolled to its top and the finger keeps pulling DOWN, the whole sheet follows the
 *   finger. Let go past 30 percent of the height, or with a fast downward fling, and it slides away and calls [onDismiss];
 *   otherwise it springs back. It works through nested scrolling, so a screen with no scrolling list simply does not drag
 *   (nothing fights taps, sliders, text fields or keypads). The pull is only taken from finger input, never from a fling.
 * - `graphicsLayer` only, read in the draw phase. Taps work from the first frame. The Back button chain is NOT changed:
 *   MainActivity keeps its own `BackHandler` for the tools.
 * - [sheetKey] restarts the arrival when the open tool changes.
 */
@Composable
fun WisdomToolSheet(sheetKey: Any, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    key(sheetKey) {
        WisdomToolSheetBody(onDismiss = onDismiss, content = content)
    }
}

@Composable
private fun WisdomToolSheetBody(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var dragY by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(1f) }
    val enter = remember { Animatable(0f) }
    val latestDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 300f))
    }

    val connection = remember {
        object : NestedScrollConnection {
            // Pulling back up while the sheet is dragged down: shrink the drag first, before the content scrolls.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && dragY > 0f && available.y < 0f) {
                    val used = maxOf(available.y, -dragY)
                    dragY += used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            // The content is at its top and the finger still pulls down: the sheet takes the rest.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    dragY += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            // Finger lifted: close or spring back.
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragY <= 0f) return Velocity.Zero
                val dismiss = dragY > heightPx * 0.30f || available.y > 1800f
                val target = if (dismiss) heightPx else 0f
                val settle = Animatable(dragY)
                settle.animateTo(target, spring(dampingRatio = 1f, stiffness = 380f)) {
                    dragY = value
                }
                if (dismiss) latestDismiss()
                return available
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(120f)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .nestedScroll(connection)
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * size.height * 0.12f + dragY
            }
    ) {
        content()
    }
}
