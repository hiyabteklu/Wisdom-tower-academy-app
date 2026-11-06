package com.wisdomtower.academy.ui.competition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.competition.CompetitionRepository
import com.wisdomtower.academy.data.competition.CompetitionResult
import com.wisdomtower.academy.data.competition.LeaderboardResult
import com.wisdomtower.academy.data.competition.LeaderboardRow
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomExpandable
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomReveal

// D7f (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. The private colours were top-level vals (read once at class load, so
// they would not follow the switch); they are getters now. Night keeps the old values exactly (cyan 22E0FF, muted 94A3B8,
// red F87171, gold FBBF24, silver CBD5E1, bronze D97706). Day: cyan and muted come from the tokens, red B91C1C, and the
// medal colours are darker (gold B7791F, silver 7B8794, bronze B45309) because the night gold and silver are almost
// invisible on ivory. Big numbers and scores use rankTextColor (day: gold 7A4F0A, silver 475569, bronze 9A3412) so they
// stay readable as text; the trophy ring, icon and card edge use the medal colour. The dark text on the rank chip stays
// dark in both modes (the chip is a medal-coloured pill). The white text lines became WisdomInk.
private val LbCyan: Color
    get() = WisdomCyan
private val LbMuted: Color
    get() = if (AppThemeMode.isDark) Color(0xFF94A3B8) else WisdomMuted
private val LbRed: Color
    get() = if (AppThemeMode.isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
private val LbDarkText = Color(0xFF0A101D)
private val LbGold: Color
    get() = if (AppThemeMode.isDark) Color(0xFFFBBF24) else Color(0xFFB7791F)
private val LbSilver: Color
    get() = if (AppThemeMode.isDark) Color(0xFFCBD5E1) else Color(0xFF7B8794)
private val LbBronze: Color
    get() = if (AppThemeMode.isDark) Color(0xFFD97706) else Color(0xFFB45309)

// C7 slice 1: the leaderboard of one finished contest (owner decisions D, E, F, G).
// Everything shown is a server value from competition_leaderboard: names, scores, times, prize, points.
// Session X (owner, 2026-10-15): ranks 1 to 3 are three trophy cards (a big gold card for the winner, silver and
// bronze side by side), each with a glowing trophy, the rank, the full name, the score as score of total and the
// time used. Ranks 4 to 10 sit behind one expandable button (rank, name, time, score). A student outside the top 10
// sees their own row (rank, name, time, score) under the list as the extra row (E). A disqualified student sees a
// message with the reason (F). Before the window closes the server says "not available" and this screen shows a
// short notice. The screen is drawn inside the hub (top bar and bottom tabs of the app stay visible).
// Session AK (owner, 2026-10-15): glass look like the other competition screens. No solid navy any more: the
// trophy cards, the rows and the prize panel are see-through glass (WisdomGlassCard and the same white tints), the
// buttons are CompetitionPillButton. The medal colours stay on the trophies, the rank chips and a soft edge.
// Logic, texts and data are unchanged.
// Session M9 (docs/UI_MOTION_PLAN.md, motion only): the three trophy cards rise in the order 2nd, 1st, 3rd
// (wisdomReveal, once per contest per app session), the own row and the prize panel rise after them, and ranks 4 to
// 10 open and close with WisdomExpandable instead of appearing at once. No text, data or logic changed.
// U5 (2026-11-06, owner): admins only see a small card with the Top 3 JPG download (4:3 and 9:16) right under the
// participant count (LeaderboardAdminExport). Students never see it. No minimum number of participants.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
@Composable
fun CompetitionLeaderboardScreen(
    competitionId: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loading by remember(competitionId) { mutableStateOf(true) }
    var result by remember(competitionId) { mutableStateOf<LeaderboardResult?>(null) }
    var errorText by remember(competitionId) { mutableStateOf<String?>(null) }
    var reloadKey by remember(competitionId) { mutableIntStateOf(0) }

    LaunchedEffect(competitionId, reloadKey) {
        loading = true
        errorText = null
        val r = CompetitionRepository.leaderboard(competitionId)
        if (r is CompetitionResult.Ok) {
            result = r.value
        } else if (r is CompetitionResult.Failed) {
            errorText = r.message
        }
        loading = false
    }

    BackHandler(enabled = true) { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompetitionPillButton(
                    label = "Back",
                    onClick = onBack,
                    modifier = Modifier.widthIn(min = 76.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = title,
                    color = WisdomInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            val res = result
            val err = errorText
            if (loading && res == null) {
                Text(text = "Loading...", color = LbMuted, fontSize = 14.sp)
            } else if (res == null) {
                Text(text = err ?: "The leaderboard could not be loaded.", color = LbRed, fontSize = 13.sp)
                CompetitionPillButton(
                    label = "Try again",
                    onClick = { reloadKey += 1 },
                    modifier = Modifier.widthIn(min = 110.dp),
                    emphasized = true
                )
            } else if (res is LeaderboardResult.NotAvailable) {
                Text(
                    text = "The leaderboard opens when the competition window closes.",
                    color = LbMuted,
                    fontSize = 14.sp
                )
            } else if (res is LeaderboardResult.Available) {
                LeaderboardBody(competitionId = competitionId, res = res)
            }
        }
    }
}

@Composable
private fun LeaderboardBody(competitionId: String, res: LeaderboardResult.Available) {
    var expanded by remember { mutableStateOf(false) }
    val first = res.top.firstOrNull { it.rank == 1 }
    val second = res.top.firstOrNull { it.rank == 2 }
    val third = res.top.firstOrNull { it.rank == 3 }
    val others = res.top.filter { it.rank > 3 }
    val me = res.me
    val myRank = me?.rank
    // M9: one stable key prefix per contest, so each card rises once per app session
    val revealKey = "lb_" + competitionId

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (res.disqualified) {
            val reason = res.disqualifiedReason
            val message = if (reason.isNullOrBlank()) {
                "You were disqualified from this competition."
            } else {
                "You were disqualified from this competition. Reason: " + reason
            }
            Text(text = message, color = LbRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = res.participants.toString() + (if (res.participants == 1) " participant" else " participants"),
            color = LbMuted,
            fontSize = 12.sp
        )

        // U5: admins only (the card draws nothing for a student)
        LeaderboardAdminExport(competitionId = competitionId)

        if (res.top.isEmpty()) {
            Text(text = "No ranked results yet.", color = LbMuted, fontSize = 14.sp)
        }

        // the three trophies: a big gold card, then silver and bronze side by side
        // M9 order of appearance: 2nd first, then 1st, then 3rd
        if (first != null) {
            HeroTrophyCard(
                row = first,
                highlight = first.rank == myRank,
                modifier = Modifier.wisdomReveal(key = revealKey + "_1", index = 1)
            )
        }
        if (second != null || third != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (second != null) {
                    PodiumTrophyCard(
                        row = second,
                        highlight = second.rank == myRank,
                        modifier = Modifier
                            .weight(1f)
                            .wisdomReveal(key = revealKey + "_2", index = 0)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (third != null) {
                    PodiumTrophyCard(
                        row = third,
                        highlight = third.rank == myRank,
                        modifier = Modifier
                            .weight(1f)
                            .wisdomReveal(key = revealKey + "_3", index = 2)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // ranks 4 to 10, expandable (rank, name, score and time)
        if (others.isNotEmpty()) {
            val lastRank = others.last().rank
            val arrow = if (expanded) "\u25B4" else "\u25BE"
            // one inner column with no spacing, so the closed state leaves no extra gap under the button
            Column {
                CompetitionPillButton(
                    label = "Ranks 4 to " + lastRank.toString() + "  " + arrow,
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                )
                WisdomExpandable(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (row in others) {
                            LbRow(row = row, highlight = row.rank == myRank)
                        }
                    }
                }
            }
        }

        // the student's own row when ranked outside the top 10 (decision E)
        if (me != null && res.top.none { it.rank == me.rank }) {
            Text(text = "Your rank", color = LbMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.wisdomReveal(key = revealKey + "_me", index = 3)) {
                LbRow(row = me, highlight = true)
            }
        }

        val prizeTitle = res.prizeTitle
        val prizeDetails = res.prizeDetails
        val points = res.points
        if (prizeTitle != null || prizeDetails != null || points != null) {
            WisdomGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wisdomReveal(key = revealKey + "_prize", index = 4),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (prizeTitle != null) {
                        Text(text = prizeTitle, color = WisdomInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (prizeDetails != null) {
                        Text(text = prizeDetails, color = LbMuted, fontSize = 13.sp)
                    }
                    if (points != null) {
                        Text(text = "Points: " + points.toString(), color = LbCyan, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun rankColor(rank: Int): Color = when (rank) {
    1 -> LbGold
    2 -> LbSilver
    3 -> LbBronze
    else -> LbCyan
}

// The medal colour used as TEXT (scores). Night: the medal colour itself. Day: a darker shade so it reads on ivory.
private fun rankTextColor(rank: Int): Color {
    if (AppThemeMode.isDark) return rankColor(rank)
    return when (rank) {
        1 -> Color(0xFF7A4F0A)
        2 -> Color(0xFF475569)
        3 -> Color(0xFF9A3412)
        else -> LbCyan
    }
}

private fun ordinal(rank: Int): String = when (rank) {
    1 -> "1st"
    2 -> "2nd"
    3 -> "3rd"
    else -> rank.toString() + "th"
}

// 125000 ms becomes 2:05
private fun formatTaken(ms: Long): String {
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return minutes.toString() + ":" + (if (seconds < 10L) "0" else "") + seconds.toString()
}

// A glowing trophy: a soft halo, a ring and the trophy icon in the medal colour.
@Composable
private fun TrophyBadge(color: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0f))),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .background(color.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}

@Composable
private fun RankChip(rank: Int) {
    val color = rankColor(rank)
    Text(
        text = ordinal(rank),
        color = LbDarkText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun HeroTrophyCard(row: LeaderboardRow, highlight: Boolean, modifier: Modifier = Modifier) {
    val accent = rankColor(row.rank)
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), WisdomGlassFill)))
            .border(
                if (highlight) 2.dp else 1.dp,
                if (highlight) LbCyan else accent.copy(alpha = 0.5f),
                shape
            )
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrophyBadge(color = accent, size = 92.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RankChip(rank = row.rank)
            Text(
                text = row.name,
                color = WisdomInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = "Time " + formatTaken(row.timeTakenMs), color = LbMuted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = row.score.toString(), color = rankTextColor(row.rank), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(text = "of " + row.total.toString(), color = LbMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PodiumTrophyCard(row: LeaderboardRow, highlight: Boolean, modifier: Modifier = Modifier) {
    val accent = rankColor(row.rank)
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.14f), WisdomGlassFill)))
            .border(
                if (highlight) 2.dp else 1.dp,
                if (highlight) LbCyan else accent.copy(alpha = 0.5f),
                shape
            )
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TrophyBadge(color = accent, size = 70.dp)
        RankChip(rank = row.rank)
        Text(
            text = row.name,
            color = WisdomInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = row.score.toString() + " of " + row.total.toString(),
            color = rankTextColor(row.rank),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Time " + formatTaken(row.timeTakenMs), color = LbMuted, fontSize = 12.sp)
    }
}

@Composable
private fun LbRow(row: LeaderboardRow, highlight: Boolean) {
    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        fill = if (highlight) LbCyan.copy(alpha = 0.06f) else WisdomGlassFill,
        borderColor = if (highlight) LbCyan.copy(alpha = 0.7f) else WisdomGlassBorder,
        borderWidth = if (highlight) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = row.rank.toString(),
                color = LbCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.size(width = 34.dp, height = 22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    color = WisdomInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = "Time " + formatTaken(row.timeTakenMs), color = LbMuted, fontSize = 12.sp)
            }
            Text(
                text = row.score.toString() + " of " + row.total.toString(),
                color = LbCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
