package com.wisdomtower.academy.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
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

/**
 * "Background" card for the Settings screen. D1 (owner order 2026-10-19): the choices are Dark (moon icon) and Day (sun
 * icon), shown as icon buttons inside one smooth pill; the highlight slides from one icon to the other. Tapping applies it
 * to the whole app at once and saves it ([AppBackground.select]); Day also switches the whole app to the day colours (see
 * AppThemeMode). Same glass card, header and smooth expand as the other Settings sections. The subtitle shows the chosen
 * name; the icon buttons carry the names for screen readers.
 *
 * Text and tints use the ink tokens (white at night, navy by day) instead of Color.White, so night looks exactly as before.
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
                        .padding(vertical = 14.dp),
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

/** Moon for Dark, sun for any light look (Day), a picture icon for the picture looks the code still knows but does not offer. */
private fun iconFor(option: AppBackgroundOption): ImageVector = when {
    option == AppBackgroundOption.Black -> Icons.Filled.DarkMode
    option.isLight -> Icons.Filled.LightMode
    else -> Icons.Filled.Image
}

/** One rounded pill holding the icon buttons; a highlight pill slides under the chosen one. */
@Composable
private fun BackgroundPill(chosen: AppBackgroundOption, onPick: (AppBackgroundOption) -> Unit) {
    val options = AppBackground.options
    val slotWidth = 68.dp
    val slotHeight = 42.dp
    val pillShape = RoundedCornerShape(50)
    val index = options.indexOf(chosen).coerceAtLeast(0)
    val offset by animateDpAsState(
        targetValue = slotWidth * index,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "backgroundPillOffset"
    )

    Box(
        modifier = Modifier
            .clip(pillShape)
            .background(WisdomInk.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, WisdomBorderWhite), pillShape)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(width = slotWidth, height = slotHeight)
                .clip(pillShape)
                .background(WisdomCyan.copy(alpha = 0.22f))
                .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.6f)), pillShape)
        )
        Row {
            options.forEach { option ->
                val isSelected = option == chosen
                val tint = wisdomAnimatedColor(isSelected, WisdomCyan, WisdomMuted)
                Box(
                    modifier = Modifier
                        .size(width = slotWidth, height = slotHeight)
                        .clip(pillShape)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onPick(option) }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconFor(option),
                        contentDescription = option.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
