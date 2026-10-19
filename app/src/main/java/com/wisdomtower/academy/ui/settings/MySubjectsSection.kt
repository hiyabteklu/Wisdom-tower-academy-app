package com.wisdomtower.academy.ui.settings

import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomOnInk
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.content.MySubjectsPackage
import com.wisdomtower.academy.data.content.MySubjectsStore
import com.wisdomtower.academy.ui.packages.FRESHMAN_NATURAL_IDS
import com.wisdomtower.academy.ui.packages.FRESHMAN_SEM1_IDS
import com.wisdomtower.academy.ui.packages.FRESHMAN_SEM2_IDS
import com.wisdomtower.academy.ui.packages.FRESHMAN_SOCIAL_IDS
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * T8 "My Subjects" card for the Settings screen (docs/T8_MY_SUBJECTS_PLAN.md). The student picks which packages they
 * study and, inside each, which subjects. "Your status" counts only these. Every change is saved at once, on this phone
 * only, per signed-in student ([MySubjectsStore]). Nothing is hidden from the learning hubs and nothing is deleted.
 *
 * Quick-fill pills for Freshman only PRE-SELECT subjects from the website's own lists; the student can always edit.
 * No number of subjects is claimed.
 *
 * TalkBack (S3 part 3b, 2026-10-17): header is a button with spoken "Expanded" / "Collapsed"; package and subject
 * pills are checkboxes (on / off); quick-fill pills and "Clear" are buttons. Look and logic unchanged.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MySubjectsSection() {
    val context = LocalContext.current
    val user by AuthStateManager.currentUser.collectAsState()
    val profile by AuthStateManager.currentProfile.collectAsState()
    val userId = user?.id
    var expanded by remember { mutableStateOf(false) }

    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = WisdomBorderWhite
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
                        Icon(Icons.Filled.School, contentDescription = null, tint = WisdomCyan, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("My Subjects", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                        Text("Choose what you study. Your status counts only these.", fontSize = 11.sp, color = WisdomMuted)
                    }
                }
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = WisdomMuted, modifier = Modifier.size(20.dp))
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WisdomInk.copy(alpha = 0.02f))
                        .border(BorderStroke(1.dp, WisdomBorderWhite.copy(alpha = 0.4f)))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (userId.isNullOrBlank()) {
                        Text("Sign in to choose your subjects.", fontSize = 12.sp, color = WisdomMuted)
                        com.wisdomtower.academy.ui.auth.SignInPill() // UX-CONSISTENCY-2026-10-17
                    } else {
                        val level = profile?.educationLevel
                        val initial = remember(userId, level) { MySubjectsStore.load(context, userId, level) }
                        var picks by remember(userId, level) { mutableStateOf(initial.keys) }
                        var fromLevel by remember(userId, level) { mutableStateOf(initial.fromAcademicLevel) }
                        var open by remember(userId, level) {
                            mutableStateOf(MySubjectsStore.packages.filter { p -> p.subjects.any { it.key in initial.keys } }.map { it.id }.toSet())
                        }

                        fun update(newPicks: Set<String>) {
                            picks = newPicks
                            fromLevel = false
                            MySubjectsStore.save(context, userId, newPicks)
                        }

                        if (fromLevel && picks.isNotEmpty()) {
                            Text("Pre-selected from your Academic Level. Change it any time.", fontSize = 11.sp, color = WisdomMuted, lineHeight = 15.sp)
                        }

                        Text("WHAT ARE YOU STUDYING?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, letterSpacing = 0.5.sp)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MySubjectsStore.packages.forEach { pkg ->
                                SubjectPill(pkg.label, selected = pkg.id in open) {
                                    if (pkg.id in open) {
                                        open = open - pkg.id
                                        update(picks.filterNot { MySubjectsStore.packageOf(it) == pkg.id }.toSet())
                                    } else {
                                        open = open + pkg.id
                                    }
                                }
                            }
                        }

                        MySubjectsStore.packages.filter { it.id in open }.forEach { pkg ->
                            PackagePicker(pkg, picks) { update(it) }
                        }

                        Text(
                            "Your other study time and attempts are kept. They count again if you pick those subjects again.",
                            fontSize = 12.sp,
                            color = WisdomMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackagePicker(pkg: MySubjectsPackage, picks: Set<String>, onChange: (Set<String>) -> Unit) {
    val packageKeys = pkg.subjects.map { it.key }.toSet()
    val selectedCount = picks.count { it in packageKeys }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WisdomInk.copy(alpha = 0.03f))
            .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pkg.label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                Text("$selectedCount selected", fontSize = 11.sp, color = WisdomMuted)
            }
            Text(
                "Clear",
                fontSize = 11.5.sp,
                color = WisdomCyan,
                modifier = Modifier
                    .semantics { contentDescription = "Clear " + pkg.label }
                    .clickable(role = Role.Button) { onChange(picks - packageKeys) }
                    .padding(8.dp)
            )
        }

        if (pkg.id == MySubjectsStore.FRESHMAN) {
            // Quick fill: only pre-selects from the website's own lists; the student edits freely afterwards.
            val quick = listOf(
                "Natural stream" to FRESHMAN_NATURAL_IDS,
                "Social stream" to FRESHMAN_SOCIAL_IDS,
                "Semester 1" to FRESHMAN_SEM1_IDS,
                "Semester 2" to FRESHMAN_SEM2_IDS
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quick.forEach { (label, ids) ->
                    val keys = ids.map { "${MySubjectsStore.FRESHMAN}/$it" }.toSet()
                    SubjectPill(label, selected = false, outlined = true) {
                        onChange((picks - packageKeys) + keys)
                    }
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pkg.subjects.forEach { s ->
                SubjectPill(s.name, selected = s.key in picks) {
                    onChange(if (s.key in picks) picks - s.key else picks + s.key)
                }
            }
        }
    }
}

@Composable
private fun SubjectPill(text: String, selected: Boolean, outlined: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    // Quick-fill pills (outlined) do an action, so they are buttons. The others switch on and off, so they are checkboxes.
    val tapModifier = if (outlined) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                when {
                    selected -> WisdomInk
                    outlined -> WisdomCyan.copy(alpha = 0.10f)
                    else -> WisdomInk.copy(alpha = 0.04f)
                }
            )
            .border(
                BorderStroke(1.dp, if (selected) WisdomInk else if (outlined) WisdomCyan.copy(alpha = 0.4f) else WisdomBorderWhite),
                shape
            )
            .then(tapModifier)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) WisdomOnInk else if (outlined) WisdomCyan else WisdomInk
        )
    }
}
