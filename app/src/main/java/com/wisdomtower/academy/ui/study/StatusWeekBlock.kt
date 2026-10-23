package com.wisdomtower.academy.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.ProgressRow
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.CancellationException

/**
 * STATUS CARD (docs/STATUS_CARD_PLAN.md, slice S2): the short "Your week" block on Your status. Plain sentences only, read
 * from [StatusCardData] (slice S1) so this block and the future picture (S3) can never disagree. REAL DATA ONLY: a figure
 * that cannot be known is left out or said plainly, never shown as 0 and never invented. If the data cannot be built at
 * all the block is simply not shown (the rest of Your status is unchanged).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusWeekBlock(
    rows: List<ProgressRow>,
    summary: StatusSummary,
    track: StatusTrack,
    studentName: String?,
    educationLevel: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var data by remember { mutableStateOf<StatusCardData?>(null) }

    LaunchedEffect(rows, summary, track.id) {
        data = try {
            buildStatusCardData(
                context = context,
                rows = rows,
                summary = summary,
                track = track,
                studentName = studentName,
                studentIdNumber = null, // the ID is not shown in this block
                educationLevel = educationLevel
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    val d = data
    if (d != null) {
        val sentences = weekSentences(d)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(WisdomGlassFill)
                .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.20f)), RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "YOUR WEEK  \u00B7  ${d.weekRangeLabel}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WisdomCyan,
                letterSpacing = 0.8.sp
            )
            sentences.forEach { line ->
                Text(line, fontSize = 13.sp, color = WisdomInk, lineHeight = 19.sp)
            }
            val accent = if (d.weak != null) WisdomAccentRose else WisdomCyan
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("NEXT STEP:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 0.6.sp)
                Text(d.nextStep, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WisdomInk, lineHeight = 17.sp)
            }
            val asOf = d.asOfLabel
            if (asOf != null) {
                Text(asOf, fontSize = 11.sp, color = WisdomMuted)
            }
        }
    }
}

/**
 * The 3 to 4 plain sentences above the next-step box (the next step itself is [StatusCardData.nextStep]).
 * Pure function: same data in, same sentences out. Grade 9 wording, no jargon.
 */
internal fun weekSentences(d: StatusCardData): List<String> {
    val out = ArrayList<String>(4)

    // 1. hours this week against the target
    val minutes = d.weekMinutes
    if (minutes == null) {
        out.add("We could not read your study days right now.")
    } else {
        val partial = if (d.weekComplete) "" else " Some days are missing from the saved copy."
        out.add("This week you studied ${scDuration(minutes)}. Your goal is ${formatStatusNumber(d.weekTargetHours)} hours.$partial")
    }

    // 2. best day
    val bestName = d.bestDayName
    val bestMinutes = d.bestDayMinutes
    if (bestName != null && bestMinutes != null) {
        out.add("Your best day was $bestName, with ${scDuration(bestMinutes)}.")
    }

    // 3. streak
    val streak = d.streakDays
    if (streak != null) {
        out.add(
            when (streak) {
                0 -> "Your streak is 0 days. Study today to start it."
                1 -> "Your streak is 1 day."
                else -> "Your streak is $streak days."
            }
        )
    }

    // 4. subject that needs the most work, or the note when nothing is opened yet
    val weak = d.weak
    if (weak != null) {
        out.add("${weak.subject.name} needs the most work right now.")
    } else {
        val note = d.weakNote
        if (note != null) out.add(note)
    }
    return out
}
