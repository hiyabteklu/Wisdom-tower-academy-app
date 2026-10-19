package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable UI Components reproducing website CSS classes (globals.css & ui-polish.css).
 */

/**
 * Soft press-in used by the tappable controls (iOS feel, same spring as the login button: scale down a little while the
 * finger is down, springy return). It must be the FIRST modifier of the element so the whole element (shape included)
 * scales. Added 2026-10-14 (docs/SESSION_2026-10-14_resources_ios.md).
 */
@Composable
fun Modifier.wisdomPressScale(source: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "wisdomPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * GLASS (2026-10-13, owner request, docs/GLASS_PLAN.md area 5): the default card is now the see-through glass of the
 * Targets screen (white 4% fill, white 8% edge, see WisdomGlass.kt), so every screen that still calls this card
 * follows the glass look. An explicit borderColor passed by a caller (accent edges) is kept.
 * D2 (day mode): by day it is the frosted ivory card with a soft navy shadow ([wisdomCardShadow]); night is unchanged.
 *
 * 2026-10-14: a card that is tappable ([onClick] given) now presses in softly (scale 0.975). A card without [onClick]
 * is exactly as before (no extra state, so long lists stay cheap).
 *
 * S3 (TalkBack): the four tappable components in this file (card, primary, secondary and open buttons) are announced
 * as buttons (Role.Button). Looks are unchanged.
 */
@Composable
fun WisdomModernCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = WisdomGlassBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (onClick == null) {
        Box(
            modifier = modifier
                .wisdomCardShadow(shape)
                .clip(shape)
                .background(WisdomGlassFill)
                .border(BorderStroke(borderWidth, borderColor), shape),
            content = content
        )
    } else {
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = modifier
                .wisdomPressScale(interaction, 0.975f)
                .wisdomCardShadow(shape)
                .clip(shape)
                .background(WisdomGlassFill)
                .border(BorderStroke(borderWidth, borderColor), shape)
                .clickable(
                    interactionSource = interaction,
                    role = Role.Button,
                    indication = ripple(color = WisdomCyan.copy(alpha = 0.2f)),
                    onClick = onClick
                ),
            content = content
        )
    }
}

/** Pill shaped (2026-10-14, iOS feel) with a soft press-in. Size and colours unchanged. */
@Composable
fun WisdomPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val shape = CircleShape
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .wisdomPressScale(interaction, 0.97f)
            .clip(shape)
            .background(WisdomPrimaryBtnGradient)
            .clickable(
                interactionSource = interaction,
                role = Role.Button,
                indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = WisdomDarkOnCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        if (icon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            icon()
        }
    }
}

/** Pill shaped (2026-10-14, iOS feel) with a soft press-in. Size and colours unchanged. */
@Composable
fun WisdomSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = WisdomBorderWhite,
    textColor: Color = WisdomTextPrimary,
    icon: (@Composable () -> Unit)? = null
) {
    val shape = CircleShape
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .wisdomPressScale(interaction, 0.97f)
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .background(WisdomGlassFillStrong)
            .clickable(
                interactionSource = interaction,
                role = Role.Button,
                indication = ripple(color = WisdomCyan.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

/**
 * Compact pill button (.btn-open) used across all pathway cards. Soft press-in added 2026-10-14.
 */
@Composable
fun WisdomOpenButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Open →"
) {
    val shape = CircleShape
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .wisdomPressScale(interaction, 0.94f)
            .clip(shape)
            .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.5f)), shape)
            .background(WisdomCyan.copy(alpha = 0.08f))
            .clickable(
                interactionSource = interaction,
                role = Role.Button,
                indication = ripple(color = WisdomCyan.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = WisdomCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

/**
 * The app background that every screen and the app shell draw: the background the student chose in Settings
 * (ui/theme/AppBackground.kt): slightly blue, black, or a smooth space look made of soft glows. Midnight (blue) is the
 * default.
 *
 * 2026-10-17 rework: the five options were all nearly the same flat navy, so the owner asked for real differences. There
 * is still no picture file and no dots or stars; the look is drawn from soft, low-opacity glows ([appBackdrop]), and it
 * redraws at once when the choice changes.
 */
@Composable
fun AtmosphereBackground(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .appBackdrop()
            .clipToBounds()
    )
}
