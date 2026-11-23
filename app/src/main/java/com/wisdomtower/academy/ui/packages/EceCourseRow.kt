package com.wisdomtower.academy.ui.packages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * ECE-FIX-B (docs/ADMIN_ECE_Y1_Y5_PLAN.md, Phase B): a narrow full-width row for the courses of Years 1, 2, 4, 5
 * (they have no course pictures, so no image card grid). Same glass card, corner radius and text sizes as the item rows
 * in `HubItemListScreen.kt`. Book icon at the start, title (one or two lines), small muted [code] under it, chevron at
 * the end, ripple from the glass card, at least 56 dp high.
 *
 * Also used for the stream chooser (Year 4 Semester 2, Year 5): title = stream name, [code] = "N courses".
 * Year 3 does NOT use this (it keeps its image card grid).
 */
@Composable
fun EceCourseRow(
    code: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WisdomGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = WisdomCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WisdomInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!code.isNullOrBlank()) {
                    Text(
                        text = code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = WisdomMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = WisdomMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
