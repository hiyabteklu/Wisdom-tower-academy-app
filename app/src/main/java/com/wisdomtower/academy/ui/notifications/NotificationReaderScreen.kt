package com.wisdomtower.academy.ui.notifications

import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.model.NativeNotificationItem
import com.wisdomtower.academy.data.model.NotificationBlock
import com.wisdomtower.academy.data.model.NotificationCategory
import com.wisdomtower.academy.data.model.NotificationRun
import com.wisdomtower.academy.ui.theme.WisdomAccentOrange
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Notifications v2 (slice R5): the full-screen reading view for announcements.
 * It opens INSIDE the notifications section (no navigation anywhere); back returns to the list.
 *
 * Gold look: the gold is the amber-300 value the Announcement icon already used in NotificationsScreen
 * (0xFFFCD34D); nothing new is invented. Title = strong gold, subtitle = softer gold, highlight = gold
 * background with dark text, bold = bright white, callout = glass card with a gold left bar,
 * divider = thin gold gradient line. No markdown is parsed: bold and highlight are flags on text runs.
 */
internal val NoticeGold = Color(0xFFFCD34D)
private val NoticeGoldSoft = Color(0xFFFCD34D).copy(alpha = 0.75f)
private val NoticeBodyText = Color(0xFFE2E8F0)

internal fun categoryLabel(category: NotificationCategory): String = when (category) {
    NotificationCategory.ADMIN -> "Announcement"
    NotificationCategory.MATERIAL -> "Material Release"
    NotificationCategory.CONTEST -> "Contest"
}

internal fun categoryAccent(category: NotificationCategory): Color = when (category) {
    NotificationCategory.ADMIN -> NoticeGold
    NotificationCategory.MATERIAL -> Color(0xFF7DD3FC)
    NotificationCategory.CONTEST -> WisdomAccentOrange
}

private fun runsToText(runs: List<NotificationRun>): AnnotatedString = buildAnnotatedString {
    runs.forEach { r ->
        val style = when {
            r.highlight -> SpanStyle(
                background = NoticeGold.copy(alpha = 0.90f),
                color = WisdomDarkOnCyan,
                fontWeight = if (r.bold) FontWeight.ExtraBold else FontWeight.SemiBold
            )
            r.bold -> SpanStyle(color = WisdomInk, fontWeight = FontWeight.ExtraBold)
            else -> SpanStyle()
        }
        withStyle(style) { append(r.text) }
    }
}

/** Rows made before the composer existed have no blocks: show their title and body in the same style. */
private fun fallbackBlocks(item: NativeNotificationItem): List<NotificationBlock> {
    val out = mutableListOf<NotificationBlock>()
    if (item.title.isNotBlank()) out.add(NotificationBlock.Title(item.title))
    if (item.body.isNotBlank()) out.add(NotificationBlock.Paragraph(listOf(NotificationRun(item.body))))
    return out
}

@Composable
fun NotificationReaderScreen(
    item: NativeNotificationItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val blocks = remember(item) { if (item.content.isNotEmpty()) item.content else fallbackBlocks(item) }
    val category = item.category
    val accent = categoryAccent(category)
    val dateText = item.createdAt.take(10)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.wisdomTouchFeel().clickable { onBack() }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WisdomMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Notifications", color = WisdomMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, accent.copy(alpha = 0.35f)), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        categoryLabel(category).uppercase(),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (dateText.isNotBlank()) {
                    Text("  •  $dateText", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            blocks.forEach { block -> BlockView(block) }
        }
    }
}

@Composable
private fun BlockView(block: NotificationBlock) {
    when (block) {
        is NotificationBlock.Title -> Text(
            text = block.text,
            color = NoticeGold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        )
        is NotificationBlock.Subtitle -> Text(
            text = block.text,
            color = NoticeGoldSoft,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        is NotificationBlock.Paragraph -> Text(
            text = runsToText(block.runs),
            color = NoticeBodyText,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        is NotificationBlock.Callout -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(14.dp))
                .background(WisdomGlassFill)
                .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(14.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(NoticeGold)
            )
            Text(
                text = runsToText(block.runs),
                color = NoticeBodyText,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(14.dp)
            )
        }
        is NotificationBlock.Bullets -> Column(modifier = Modifier.padding(vertical = 6.dp)) {
            block.items.forEach { runs ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("•", color = NoticeGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = runsToText(runs),
                        color = NoticeBodyText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        NotificationBlock.Divider -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, NoticeGold.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )
    }
}
