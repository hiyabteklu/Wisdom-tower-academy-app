package com.wisdomtower.academy.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * "Glass" look of the Targets screen (owner reference, 2026-10-13): a see-through white tint with a thin
 * white edge, so the glow of [AtmosphereBackground] shows through. Night values:
 *  - panel / row fill   : white 12%  ([WisdomGlassFill])
 *  - pill / button fill : white 16%  ([WisdomGlassFillStrong])
 *  - quiet (done) fill  : white 7%   ([WisdomGlassFillQuiet])
 *  - edge               : white 18%  ([WisdomGlassBorder])
 *  - corners            : 16dp for rows, 24dp for big panels, fully round for pills
 * BG-POPUP (2026-10-17, owner: "increase the glass opacity, I am not seeing much difference"): the night values were
 * 4 / 6 / 2 / 8 percent. To go back to them, use those four numbers.
 * Plan and order of the areas: docs/GLASS_PLAN.md.
 *
 * D2 (docs/DAY_MODE_PLAN.md, owner order 2026-10-19): by day the glass is NOT see-through (that was the reason day mode was
 * cancelled on 2026-10-17). It is frosted: panel / row 82 percent, pill / button 90 percent, quiet (done) 65 percent, with a
 * hairline deep-navy edge (12 percent) and a soft navy shadow ([wisdomCardShadow]). The text on it is [WisdomInk] (deep
 * navy). Night is unchanged. These are getters: read them while composing or drawing, not once at class load.
 *
 * DAY-SAGE (owner order 2026-10-19) made the day frost a pale sage (DCEAE7 / E4F0ED / D3E3DF). DAY-IVORY2 (owner order
 * 2026-10-19, the sage page looked muddy and inverse) put it BACK to frosted ivory (F8F5EE / FBF9F4 / F3EFE6), to match the
 * ivory page in AppBackground.kt (Day) and the card tokens in Color.kt. Change them together.
 */
/**
 * Shared popup and overlay surfaces. Denser than normal cards so text stays readable above the scrim,
 * while the Transparent background remains perceptible around each panel. Getter follows background changes.
 */
val WisdomPopupFill: Color
    get() = when (AppBackground.selected) {
        AppBackgroundOption.Day -> Color(0xEEF8F5EE)       // 93% frosted ivory
        AppBackgroundOption.Photo2 -> Color(0xD90F1829)    // 85% navy glass over the photo background
        else -> Color(0xE60F1829)                          // 90% navy glass at night
    }
val WisdomPopupFillLow: Color
    get() = when (AppBackground.selected) {
        AppBackgroundOption.Day -> Color(0xE6F3EFE6)       // 90% secondary ivory
        AppBackgroundOption.Photo2 -> Color(0xCC0F1829)    // 80% secondary navy glass
        else -> Color(0xD90F1829)                          // 85% secondary navy glass
    }
val WisdomPopupSurfaceElevated: Color
    get() = if (AppThemeMode.isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFE4DFD3)

val WisdomGlassFill: Color
    get() = if (AppThemeMode.isDark) Color.White.copy(alpha = 0.12f) else Color(0xD1F8F5EE)
val WisdomGlassFillStrong: Color
    get() = if (AppThemeMode.isDark) Color.White.copy(alpha = 0.16f) else Color(0xE6FBF9F4)
val WisdomGlassFillQuiet: Color
    get() = if (AppThemeMode.isDark) Color.White.copy(alpha = 0.07f) else Color(0xA6F3EFE6)
val WisdomGlassBorder: Color
    get() = if (AppThemeMode.isDark) Color.White.copy(alpha = 0.18f) else WisdomLightPalette.ink.copy(alpha = 0.12f)

/**
 * Soft navy shadow under a card, DAY ONLY (plan section 4: frosted card, hairline border, soft navy shadow). At night it
 * returns the modifier unchanged, so night cards are exactly as before. Put it BEFORE `.clip(shape)` so the shadow is drawn
 * outside the card. Elevation is small (3 dp) so long lists stay cheap and smooth.
 */
fun Modifier.wisdomCardShadow(shape: Shape): Modifier =
    if (AppThemeMode.isDark) {
        this
    } else {
        this.shadow(
            elevation = 3.dp,
            shape = shape,
            clip = false,
            ambientColor = WisdomLightPalette.ink.copy(alpha = 0.10f),
            spotColor = WisdomLightPalette.ink.copy(alpha = 0.14f)
        )
    }

/**
 * Same shape of call as [WisdomModernCard], but see-through glass instead of the almost solid navy.
 * S3 (TalkBack): a tappable card is announced as a button (Role.Button).
 */
@Composable
fun WisdomGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    fill: Color = WisdomGlassFill,
    borderColor: Color = WisdomGlassBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            role = Role.Button,
            indication = ripple(color = WisdomCyan.copy(alpha = 0.2f)),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .wisdomCardShadow(shape)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .then(clickModifier),
        content = content
    )
}
