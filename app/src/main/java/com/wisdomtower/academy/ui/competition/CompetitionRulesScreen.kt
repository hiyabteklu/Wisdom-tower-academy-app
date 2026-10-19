package com.wisdomtower.academy.ui.competition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import com.wisdomtower.academy.ui.theme.wisdomReveal

// D7d (docs/DAY_MODE_PLAN.md, 2026-10-19): the four private colours below are getters with a night / day pair, because a
// top-level val is read once at class load and would not follow the day / night switch. Night keeps the old values
// exactly (muted 94A3B8, red F87171, cover 0A101D at 40 percent). Day: muted 4A5A70 (WisdomMuted), a deeper red B91C1C so the
// warning stays readable on ivory, and the cover is the ivory page colour E9E5DC at 95 percent so the hub list below
// cannot show through.
private val RulesMuted: Color
    get() = if (AppThemeMode.isDark) Color(0xFF94A3B8) else WisdomMuted
private val RulesRed: Color
    get() = if (AppThemeMode.isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
private val RulesNavy: Color
    get() = if (AppThemeMode.isDark) Color(0x660A101D) else Color(0xF2E9E5DC)
private val RulesMeta: Color
    get() = if (AppThemeMode.isDark) Color(0xFFE2E8F0) else WisdomTextPrimary

// Session U (owner, 2026-10-15): the student must first read a short red warning before a ranked attempt.
// Two parts live in this file:
// 1. CompetitionFullScreen: the cover the hub puts over its list for the rules page, the exam, the
//    leaderboard and the review. Back is handled by the content (BackHandler).
// 2. CompetitionRulesScreen: the reading page before a RANKED attempt. Short red lines with an exclamation
//    mark, in the owner's words.
// Session X (owner, 2026-10-15): "keep the app's bottom navigation and top navigation, do not hide them".
// CompetitionFullScreen is no longer a Dialog (a dialog is its own window and covers the whole phone, bars
// included). It is now a plain box drawn inside the hub's own place in MainActivity: that place is the area
// between the top bar and the bottom navigation, so both bars stay visible and nothing is clipped by them.
// The box has a background and the same zIndex (17) that MainActivity gives the hub, and it takes
// all taps so the list below cannot be touched. Hypothesis (not seen on a device): the overlay layering that
// failed in session W is fixed by this background and zIndex.
// Session AI (owner, 2026-10-15): the warning is a glass card with a soft red tint instead of a solid box,
// and Cancel and Start are CompetitionPillButton (round, no bright solid colour). Text is unchanged.
// Session UI_AA (M11 slice 2, 2026-10-16): the title, the questions-and-minutes line, the warning card and
// the note rise in once with wisdomReveal (UI_MOTION_PLAN section 4). The Cancel and Start buttons are NOT
// animated: they are the controls of the attempt and stay instant. Texts and logic are unchanged.
// NOTE for the owner: the warning says the built-in system will flag the student. The flagging itself is
// stage C10 and is NOT built yet, so today the text is a warning only.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
@Composable
fun CompetitionFullScreen(
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    // the content handles Back itself; this is only the backup
    BackHandler(enabled = true) { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RulesNavy)
            .zIndex(17f)
            .pointerInput(Unit) { }
    ) {
        content()
    }
}

@Composable
fun CompetitionRulesScreen(
    title: String,
    questionCount: Int,
    durationMin: Int,
    starting: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        if (!starting) onCancel()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    color = WisdomInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.wisdomReveal(key = "comp_rules_title_" + title, index = 0)
                )
                Text(
                    text = questionCount.toString() + " questions  \u2022  " + durationMin.toString() + " min",
                    color = RulesMeta,
                    fontSize = 14.sp,
                    modifier = Modifier.wisdomReveal(key = "comp_rules_meta_" + title, index = 1)
                )

                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "comp_rules_card_" + title, index = 2),
                    cornerRadius = 24.dp,
                    fill = RulesRed.copy(alpha = if (AppThemeMode.isDark) 0.06f else 0.08f),
                    borderColor = RulesRed.copy(alpha = 0.30f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RulesLine("! Any attempt to cheat will result in disqualification.")
                        RulesLine("! No notes, books or other references.")
                        RulesLine("! No AI tools.")
                        RulesLine("! Do not leave this window.")
                        RulesLine("! Our built-in system will flag you.")
                    }
                }

                Text(
                    text = "Your ranked attempt cannot be restarted. The clock keeps running until the contest ends.",
                    color = RulesMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.wisdomReveal(key = "comp_rules_note_" + title, index = 3)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompetitionPillButton(
                    label = "Cancel",
                    emphasized = false,
                    enabled = !starting,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
                CompetitionPillButton(
                    label = if (starting) "Starting..." else "I understand, start",
                    emphasized = true,
                    enabled = !starting,
                    onClick = onStart,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}

@Composable
private fun RulesLine(text: String) {
    Text(
        text = text,
        color = RulesRed,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
}
