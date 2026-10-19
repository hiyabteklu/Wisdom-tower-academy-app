package com.wisdomtower.academy.ui.competition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.wisdomPressScale

// Session AH (owner, 2026-10-15): the competition screens follow the glass look of Settings, Account and the
// sign-in forms. This file holds the ONE pill button every competition screen should use: round ends, a
// see-through white fill (or a soft cyan tint when emphasized), a thin edge, a soft press-in, and no bright
// solid colour. The text never wraps (one line), so a label such as Review cannot break into two lines.
// D7d (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. The private night-only cyan val is gone: the pill reads the
// mode-aware tokens WisdomCyan (night 22E0FF as before, day deep teal 0B7285), WisdomInk (night white, day navy ink)
// and WisdomGlassFillStrong (night white 16 percent, day frosted ivory). Night values are exactly the old ones.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
@Composable
fun CompetitionPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val fade = if (enabled) 1f else 0.5f
    val accent = WisdomCyan
    val fill = if (emphasized) accent.copy(alpha = 0.14f) else WisdomGlassFillStrong
    val edge = if (emphasized) accent.copy(alpha = 0.45f) else WisdomInk.copy(alpha = 0.14f)
    val textColor = if (emphasized) accent else WisdomInk
    Box(
        modifier = modifier
            .wisdomPressScale(interaction, 0.96f)
            .clip(CircleShape)
            .background(fill)
            .border(BorderStroke(1.dp, edge.copy(alpha = edge.alpha * fade)), CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = ripple(color = accent.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor.copy(alpha = fade),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}
