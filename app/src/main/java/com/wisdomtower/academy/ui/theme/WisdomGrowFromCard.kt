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
import androidx.compose.ui.unit.dp

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
