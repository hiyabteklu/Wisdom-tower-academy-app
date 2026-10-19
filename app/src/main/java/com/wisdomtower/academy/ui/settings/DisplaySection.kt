package com.wisdomtower.academy.ui.settings

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AppBackground
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomAnimatedColor
import kotlin.math.roundToInt

/**
 * The top of the Settings list (owner order 2026-10-19): ONLY a pill, "Dark" and "Transparent", always visible. No card,
 * no title, nothing to open. It replaces the old "Display & Theme" card, so the FONT SIZE, TYPOGRAPHY STYLE and LIVE
 * SAMPLE PREVIEW parts are gone from Settings (the saved setting `font_size` is no longer changed from here; its value
 * stays at whatever it was, normally "normal").
 *
 * Looks like the Appearance pill the owner showed: a rounded track with a rounded highlight on the chosen name. The
 * highlight springs across (same spring as the bottom navigation), the press squeezes slightly, and a tap gives a small
 * buzz. Tapping applies the choice to the whole app at once and saves it ([AppBackground.select]).
 *
 * The function keeps its old name so the call in SettingsScreen.kt stays the same.
 *
 * STATUS: written, NOT compiled by the agent (CI decides), NOT tested on a device.
 */
@Composable
fun DisplayThemeSection() {
    val context = LocalContext.current
    val view = LocalView.current
    val options = AppBackground.options
    val chosen = AppBackground.selected
    val selectedIndex = options.indexOf(chosen).coerceAtLeast(0)
    val pos = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        pos.animateTo(selectedIndex.toFloat(), spring(dampingRatio = 0.85f, stiffness = 420f))
    }

    val trackShape = RoundedCornerShape(999.dp)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(trackShape)
            .background(WisdomInk.copy(alpha = 0.09f))
            .border(BorderStroke(1.dp, WisdomBorderWhite), trackShape)
            .padding(4.dp)
    ) {
        val slotWidth = maxWidth / options.size

        // the highlight: one slot wide, slides on the spring clock
        Box(
            modifier = Modifier
                .offset { IntOffset((slotWidth.toPx() * pos.value).roundToInt(), 0) }
                .width(slotWidth)
                .fillMaxHeight()
                .clip(trackShape)
                .background(
                    Brush.verticalGradient(
                        listOf(WisdomCyan.copy(alpha = 0.26f), WisdomCyan.copy(alpha = 0.12f))
                    )
                )
                .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.6f)), trackShape)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val isSelected = option == chosen
                val interactionSource = remember(index) { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1.0f,
                    animationSpec = tween(120),
                    label = "appearanceTabScale"
                )
                val tint = wisdomAnimatedColor(isSelected, WisdomCyan, WisdomMuted)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(trackShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.RadioButton
                        ) {
                            if (!isSelected) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                AppBackground.select(context, option)
                            }
                        }
                        .semantics { this.selected = isSelected },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        color = tint,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                    )
                }
            }
        }
    }
}
