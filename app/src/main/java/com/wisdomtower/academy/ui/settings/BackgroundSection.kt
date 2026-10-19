package com.wisdomtower.academy.ui.settings

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AppBackground
import com.wisdomtower.academy.ui.theme.AppBackgroundOption
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomExpandable
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomAnimatedColor
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * "Background" card for the Settings screen. Choices (owner order 2026-10-19): Dark (moon icon) and Transparent (drop
 * icon), shown in one iOS-style pill that copies the bottom navigation: a glass track, a gradient highlight pill that
 * springs from one choice to the other, the chosen choice opens its name next to its icon, a small press-down squeeze and
 * a tap haptic. Tapping applies it to the whole app at once and saves it ([AppBackground.select]). Same glass card,
 * header and smooth expand as the other Settings sections. The subtitle shows the chosen name.
 *
 * Text and tints use the ink tokens (white at night) instead of Color.White.
 *
 * It is a separate file because SettingsScreen.kt is too big to resend; one line in SettingsScreen.kt calls it
 * (added by scripts/patch_b3_background_section.py).
 *
 * STATUS: written, NOT compiled by the agent (CI decides), NOT tested on a device.
 */
@Composable
fun BackgroundSection() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val chosen = AppBackground.selected

    val edge by animateColorAsState(
        targetValue = if (expanded) WisdomCyan.copy(alpha = 0.4f) else WisdomBorderWhite,
        animationSpec = tween(240),
        label = "backgroundEdge"
    )
    val chevronTint by animateColorAsState(
        targetValue = if (expanded) WisdomCyan else WisdomMuted,
        animationSpec = tween(240),
        label = "backgroundChevronTint"
    )
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "backgroundChevron"
    )

    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = edge
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WisdomInk.copy(alpha = 0.05f))
                            .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = WisdomCyan, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(text = "Background", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                        Text(text = chosen.label, fontSize = 11.sp, color = WisdomMuted)
                    }
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = chevronTint,
                    modifier = Modifier.size(20.dp).rotate(chevronAngle)
                )
            }

            WisdomExpandable(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WisdomInk.copy(alpha = 0.02f))
                        .border(BorderStroke(1.dp, WisdomBorderWhite.copy(alpha = 0.4f)))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BackgroundPill(
                        chosen = chosen,
                        onPick = { AppBackground.select(context, it) }
                    )
                }
            }
        }
    }
}

/** Moon for Dark, a drop (see-through) for Transparent. */
private fun iconFor(option: AppBackgroundOption): ImageVector =
    if (option == AppBackgroundOption.Black) Icons.Filled.DarkMode else Icons.Filled.Opacity

/** Extra width (in tab units) the chosen tab gets so its name fits. Same value as the bottom navigation. */
private const val PILL_EXTRA_WEIGHT = 1.35f

/** Width weight of tab [i] while the highlight is at position [pos] (a fraction while it moves). */
private fun pillTabWeight(i: Int, pos: Float): Float =
    1f + PILL_EXTRA_WEIGHT * (1f - abs(pos - i)).coerceAtLeast(0f)

/**
 * iOS-style pill, built like the bottom navigation: ONE spring clock (`pos`) drives the highlight, the tab widths and
 * the name fade, and it is only read while measuring and drawing, so nothing recomposes on every frame.
 */
@Composable
private fun BackgroundPill(chosen: AppBackgroundOption, onPick: (AppBackgroundOption) -> Unit) {
    val options = AppBackground.options
    val view = LocalView.current
    val lastIndex = (options.size - 1).coerceAtLeast(0)
    val selectedIndex = options.indexOf(chosen).coerceAtLeast(0)
    val pos = remember { Animatable(selectedIndex.coerceIn(0, lastIndex).toFloat()) }
    LaunchedEffect(selectedIndex) {
        pos.animateTo(
            selectedIndex.coerceIn(0, lastIndex).toFloat(),
            spring(dampingRatio = 0.85f, stiffness = 420f)
        )
    }

    val trackShape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(trackShape)
            .background(WisdomInk.copy(alpha = 0.09f))
    ) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                // child 0: the highlight pill
                Box(
                    modifier = Modifier
                        .clip(trackShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(WisdomCyan.copy(alpha = 0.26f), WisdomCyan.copy(alpha = 0.12f))
                            )
                        )
                        .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.6f)), trackShape)
                )
                // children 1..n: the choices
                options.forEachIndexed { index, option ->
                    val isSelected = option == chosen
                    val interactionSource = remember(index) { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1.0f,
                        animationSpec = tween(120),
                        label = "backgroundTabScale"
                    )
                    val tint = wisdomAnimatedColor(isSelected, WisdomCyan, WisdomMuted)
                    Box(
                        modifier = Modifier
                            .clip(trackShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.RadioButton
                            ) {
                                if (!isSelected) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onPick(option)
                                }
                            }
                            .semantics { this.selected = isSelected },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                        ) {
                            Icon(
                                imageVector = iconFor(option),
                                contentDescription = option.label,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            // The name opens with the SAME clock: its width and fade follow `pos`.
                            Box(
                                modifier = Modifier
                                    .clipToBounds()
                                    .layout { measurable, _ ->
                                        val placeable = measurable.measure(Constraints())
                                        val near = (1f - abs(pos.value.coerceIn(0f, lastIndex.toFloat()) - index))
                                            .coerceAtLeast(0f)
                                        val w = (placeable.width * near).roundToInt()
                                        layout(w, placeable.height) { placeable.placeRelative(0, 0) }
                                    }
                                    .graphicsLayer {
                                        val near = (1f - abs(pos.value.coerceIn(0f, lastIndex.toFloat()) - index))
                                            .coerceAtLeast(0f)
                                        alpha = ((near - 0.35f) / 0.65f).coerceIn(0f, 1f)
                                    }
                            ) {
                                Text(
                                    text = option.label,
                                    color = tint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.clearAndSetSemantics { }.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val n = measurables.size - 1
            val insetPx = 4.dp.roundToPx()
            val pillHeightPx = 40.dp.roundToPx()
            val trackW = constraints.maxWidth
            val trackH = constraints.maxHeight
            if (n <= 0) return@Layout layout(trackW, trackH) {}
            val innerPx = (trackW - insetPx * 2).coerceAtLeast(0)

            val p = pos.value.coerceIn(0f, (n - 1).coerceAtLeast(0).toFloat())
            val weights = FloatArray(n) { pillTabWeight(it, p) }
            val total = weights.sum().coerceAtLeast(0.001f)
            val unit = innerPx / total

            // choice edges (rounded cumulative, so there are no 1 px gaps)
            val edges = IntArray(n + 1)
            var cum = 0f
            for (i in 0 until n) {
                edges[i] = insetPx + (unit * cum).roundToInt()
                cum += weights[i]
            }
            edges[n] = insetPx + innerPx

            // highlight: slides from the slot of choice a to the slot of choice a + 1 on the same clock
            val a = floor(p).toInt().coerceIn(0, (n - 2).coerceAtLeast(0))
            val t = p - a
            val nextW = if (a + 1 < n) weights[a + 1] else weights[a]
            var before = 0f
            for (j in 0 until a) before += weights[j]
            val pillLeft = insetPx + (unit * (before + t * weights[a])).roundToInt()
            val pillWidth = (unit * (weights[a] * (1f - t) + nextW * t)).roundToInt().coerceAtLeast(0)

            val pill = measurables[0].measure(Constraints.fixed(pillWidth, pillHeightPx))
            val tabs = (0 until n).map { i ->
                measurables[i + 1].measure(Constraints.fixed((edges[i + 1] - edges[i]).coerceAtLeast(0), trackH))
            }
            layout(trackW, trackH) {
                pill.placeRelative(pillLeft, (trackH - pillHeightPx) / 2)
                tabs.forEachIndexed { i, tab -> tab.placeRelative(edges[i], 0) }
            }
        }
    }
}
