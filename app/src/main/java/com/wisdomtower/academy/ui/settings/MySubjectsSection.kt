package com.wisdomtower.academy.ui.settings

import androidx.compose.foundation.BorderStroke
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.MySubjectsPackage
import com.wisdomtower.academy.data.content.MySubjectsStore
import com.wisdomtower.academy.ui.packages.FRESHMAN_NATURAL_IDS
import com.wisdomtower.academy.ui.packages.FRESHMAN_SEM1_IDS
import com.wisdomtower.academy.ui.packages.FRESHMAN_SEM2_IDS
import com.wisdomtower.academy.ui.packages.FRESHMAN_SOCIAL_IDS
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomOnInk

/**
 * "My Subjects" editor. It used to be its own card on the Settings screen; it now lives inside the Profile screen
 * (card 3). It only draws the controls: the Profile screen owns the picks, saves them on this phone at once
 * ([MySubjectsStore]) and writes them to the server together with "Save Profile Changes" ([ProfileSubjectsSync]).
 * Nothing is hidden from the learning hubs and nothing is deleted: the picks only decide what "Your status" counts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MySubjectsEditor(
    picks: Set<String>,
    fromAcademicLevel: Boolean,
    onPicksChange: (Set<String>) -> Unit
) {
    // Packages the student opened by hand. Packages that already hold picks are always shown too.
    var opened by remember { mutableStateOf(picks.map { MySubjectsStore.packageOf(it) }.toSet()) }
    val shown = opened + picks.map { MySubjectsStore.packageOf(it) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (fromAcademicLevel && picks.isNotEmpty()) {
            Text("Pre-selected from your Academic Level. Change it any time.", fontSize = 11.sp, color = WisdomMuted, lineHeight = 15.sp)
        }

        Text("WHAT ARE YOU STUDYING?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, letterSpacing = 0.5.sp)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MySubjectsStore.packages.forEach { pkg ->
                SubjectPill(pkg.label, selected = pkg.id in shown) {
                    if (pkg.id in shown) {
                        opened = opened - pkg.id
                        onPicksChange(picks.filterNot { MySubjectsStore.packageOf(it) == pkg.id }.toSet())
                    } else {
                        opened = opened + pkg.id
                    }
                }
            }
        }

        MySubjectsStore.packages.filter { it.id in shown }.forEach { pkg ->
            PackagePicker(pkg, picks, onPicksChange)
        }

        Text(
            "Your other study time and attempts are kept. They count again if you pick those subjects again.",
            fontSize = 12.sp,
            color = WisdomMuted,
            lineHeight = 15.sp
        )
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
                modifier = Modifier.wisdomTouchFeel()
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
        Modifier.wisdomTouchFeel().clickable(role = Role.Button, onClick = onClick)
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
