package com.wisdomtower.academy.ui.packages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDark
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.wisdomPressClickable
import com.wisdomtower.academy.ui.theme.wisdomReveal

/**
 * THE one card used for packages, subjects and courses everywhere in the app (owner rule 2026-10-11):
 * optional picture (16:9, no overlay, no tag, no gradient), the title, and one "Open" button.
 * Nothing else: no description, no price, no lock, no label. Always free.
 * Always used inside [openGrid] so every list is two cards per row.
 *
 * GLASS (2026-10-13, owner request, docs/GLASS_PLAN.md area 3): the card body is the see-through glass of the
 * Targets screen ([WisdomGlassCard]) instead of the almost solid navy. Picture, texts and the Open button are unchanged.
 *
 * S3 (TalkBack): the whole card is ONE button (Role.Button from [WisdomGlassCard]) named by its title; the picture is
 * decorative and the inner "Open" label is hidden from TalkBack so nothing is read twice. Looks are unchanged.
 *
 * L2 (day / night): the title is [WisdomInk] (white at night, deep navy by day); everything else already used tokens.
 *
 * DAY-IVORY2 (owner order 2026-10-19): by day the Open button was teal on a teal tint on a card and washed out. By day it is
 * now a SOLID deep teal with white text. At night it is exactly as before (cyan 12 percent fill, cyan edge, cyan text).
 *
 * 2026-10-22: the card layout now lives in [OpenCardLayout], which takes the title as a composable slot. The normal
 * OpenCard overloads pass the usual 13 sp bold title (looks unchanged). Home passes an animated "Welcome back, name"
 * title for the signed-in student.
 */
@Composable
fun OpenCard(
    title: String,
    image: Any?,
    onClick: () -> Unit
) {
    OpenCard(
        title = title,
        image = image,
        subtitle = null,
        modifier = Modifier,
        onClick = onClick
    )
}

@Composable
fun OpenCard(
    title: String,
    image: Any?,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OpenCardLayout(
        image = image,
        subtitle = subtitle,
        modifier = modifier,
        onClick = onClick,
        titleSlot = {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WisdomInk,
                minLines = if (subtitle.isNullOrBlank()) 2 else 1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

/** The shared card body: picture, [titleSlot], optional subtitle, and the "Open" button. */
@Composable
fun OpenCardLayout(
    image: Any?,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    titleSlot: @Composable () -> Unit
) {
    val context = LocalContext.current
    val night = AppThemeMode.isDark
    val openFill = if (night) WisdomCyan.copy(alpha = 0.12f) else WisdomCyan
    val openEdge = if (night) WisdomCyan.copy(alpha = 0.45f) else WisdomCyan
    val openText = if (night) WisdomCyan else WisdomDarkOnCyan

    WisdomGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (image != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(WisdomDark)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(image)
                            .crossfade(true)
                            .build(),
                        contentDescription = null, // S3: the title below names the card; avoids reading it twice
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                titleSlot()

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WisdomCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(openFill)
                        .border(
                            BorderStroke(1.dp, openEdge),
                            RoundedCornerShape(10.dp)
                        )
                        .wisdomPressClickable(onClick = onClick)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Open",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = openText,
                        modifier = Modifier.clearAndSetSemantics { } // S3: the whole card is ONE button named by its title
                    )
                }
            }
        }
    }
}

/**
 * Lays a list out as a two-column grid inside a LazyColumn (two cards per row, like the website
 * `grid-cols-2`). [prefix] must be unique inside one LazyColumn.
 * M4: each row rises in once per app session (`wisdomReveal`, docs/UI_MOTION_PLAN.md).
 */
fun <T> LazyListScope.openGrid(
    prefix: String,
    list: List<T>,
    card: @Composable (T) -> Unit
) {
    items(
        count = (list.size + 1) / 2,
        key = { rowIndex -> "${prefix}_row_$rowIndex" }
    ) { rowIndex ->
        val first = list[rowIndex * 2]
        val second = list.getOrNull(rowIndex * 2 + 1)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wisdomReveal("${prefix}_row_$rowIndex", rowIndex),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) { card(first) }
            if (second != null) {
                Box(modifier = Modifier.weight(1f)) { card(second) }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
