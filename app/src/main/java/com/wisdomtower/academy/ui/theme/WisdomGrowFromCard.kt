package com.wisdomtower.academy.ui.theme

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * V5 (docs/IOS_MOTION_V2_PLAN.md): the box of the card that was just tapped, so the next screen can grow out of it.
 * Only a tap in the last 1.5 seconds counts, and it is used once.
 */
object WisdomCardOrigin {
    private var bounds: Rect? = null
    private var at = 0L

    fun record(box: Rect?) {
        bounds = box
        at = SystemClock.uptimeMillis()
    }

    fun take(): Rect? {
        val box = bounds
        val fresh = box != null && SystemClock.uptimeMillis() - at < 1500L
        bounds = null
        return if (fresh) box else null
    }
}

/** Per-card holder of the card's current box (plain field, no recomposition). */
class WisdomCardSource {
    var bounds: Rect? = null
}

@Composable
fun rememberWisdomCardSource(): WisdomCardSource = remember { WisdomCardSource() }

fun Modifier.wisdomCardSource(source: WisdomCardSource): Modifier =
    onGloballyPositioned { coordinates -> source.bounds = coordinates.boundsInRoot() }

fun WisdomCardSource.markOrigin() {
    WisdomCardOrigin.record(bounds)
}

private fun lerpFloat(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * The screen starts as a small copy of itself on top of the tapped card and springs to full screen (push spring, no bounce).
 * Scale, move, fade and corner radius only, read in the draw phase through `graphicsLayer`; touch works from the first frame.
 * Without a fresh tapped card ([WisdomCardOrigin]) nothing is animated.
 */
fun Modifier.wisdomGrowFromCard(key: Any?): Modifier = composed {
    val origin = remember(key) { WisdomCardOrigin.take() }
    val progress = remember(key) { Animatable(if (origin == null) 1f else 0f) }
    val layerPos = remember { FloatArray(2) }
    LaunchedEffect(key) {
        if (origin != null) {
            progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PushStiffness))
        }
    }
    this
        .onGloballyPositioned { coordinates ->
            val p = coordinates.positionInRoot()
            layerPos[0] = p.x
            layerPos[1] = p.y
        }
        .graphicsLayer {
            val p = progress.value
            val box = origin
            if (box != null && p < 1f && size.width > 0f) {
                val s = lerpFloat(box.width / size.width, 1f, p)
                scaleX = s
                scaleY = s
                translationX = lerpFloat(box.center.x - (layerPos[0] + size.width / 2f), 0f, p)
                translationY = lerpFloat(box.center.y - (layerPos[1] + size.height / 2f), 0f, p)
                alpha = (p * 3f).coerceIn(0f, 1f)
                shape = RoundedCornerShape(lerpFloat(16.dp.toPx(), 0f, p))
                clip = true
            } else {
                scaleX = 1f
                scaleY = 1f
                translationX = 0f
                translationY = 0f
                alpha = 1f
                clip = false
            }
        }
}

private class GrowHolder<T> {
    var last: T? = null
    var origin: Rect? = null
    var active = false
}

private fun GraphicsLayerScope.applyGrow(p: Float, box: Rect?, layerX: Float, layerY: Float) {
    if (p >= 1f || size.width <= 0f) {
        scaleX = 1f
        scaleY = 1f
        translationX = 0f
        translationY = 0f
        alpha = 1f
        clip = false
    } else if (box != null) {
        val s = lerpFloat(box.width / size.width, 1f, p)
        scaleX = s
        scaleY = s
        translationX = lerpFloat(box.center.x - (layerX + size.width / 2f), 0f, p)
        translationY = lerpFloat(box.center.y - (layerY + size.height / 2f), 0f, p)
        alpha = (p * 3f).coerceIn(0f, 1f)
        shape = RoundedCornerShape(lerpFloat(16.dp.toPx(), 0f, p))
        clip = true
    } else {
        val s = lerpFloat(0.94f, 1f, p)
        scaleX = s
        scaleY = s
        translationX = 0f
        translationY = 0f
        alpha = p.coerceIn(0f, 1f)
        clip = false
    }
}

/**
 * V5b shrink (docs/IOS_MOTION_V2_PLAN.md): a full-screen layer that grows out of the tapped card when [item] appears and
 * shrinks back into the same card when [item] becomes null. The last item stays composed until the close finishes.
 * `graphicsLayer` only; the layer never changes the state that decides visibility, so back handling stays where it is.
 */
@Composable
fun <T : Any> WisdomGrowLayer(
    item: T?,
    zIndexValue: Float,
    content: @Composable (T) -> Unit
) {
    val holder = remember { GrowHolder<T>() }
    val wanted = item != null
    if (item != null) {
        holder.last = item
        if (!holder.active) {
            holder.active = true
            holder.origin = WisdomCardOrigin.take()
        }
    } else {
        holder.active = false
    }
    val progress = remember { Animatable(if (wanted) 1f else 0f) }
    var shown by remember { mutableStateOf(wanted) }
    val layerPos = remember { FloatArray(2) }

    LaunchedEffect(wanted) {
        if (wanted) {
            shown = true
            progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PushStiffness))
        } else {
            progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PopStiffness))
            shown = false
            holder.last = null
        }
    }

    val current = holder.last
    if (current != null && (wanted || shown)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(zIndexValue)
                .onGloballyPositioned { coordinates ->
                    val p = coordinates.positionInRoot()
                    layerPos[0] = p.x
                    layerPos[1] = p.y
                }
                .graphicsLayer {
                    applyGrow(progress.value, holder.origin, layerPos[0], layerPos[1])
                }
        ) {
            content(current)
        }
    }
}
