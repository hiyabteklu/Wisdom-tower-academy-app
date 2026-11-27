package com.wisdomtower.academy.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AcademicLevels
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

/** The three gender answers saved in `profiles.gender` (CHECK in Supabase, migration profiles_add_gender). */
internal val GENDER_OPTIONS = listOf("Male", "Female", "Prefer not to say")

/** Longest "Other" text; the same limit is a CHECK on `profiles.custom_education_level`. */
internal const val OTHER_LEVEL_MAX = 80

private val LEVEL_GROUPS = listOf(
    AcademicLevels.GROUP_SCHOOL to "School",
    AcademicLevels.GROUP_UNIVERSITY to "University",
    AcademicLevels.GROUP_EXAMS to "Exams",
    AcademicLevels.GROUP_OTHER to "Other"
)

/**
 * A3 (docs/PLAN_2026-11-28_SIGNUP_AND_GAMES_NAV.md): step 2 of the sign-up form ("about you"): gender chips, then the
 * academic level as grouped cards built from [AcademicLevels.ALL]. Choosing Other shows one text box.
 * Only collects the answers; [NativeSignInScreen] validates them and saves them.
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
internal fun SignUpAboutStep(
    gender: String?,
    onGender: (String) -> Unit,
    level: String?,
    onLevel: (String) -> Unit,
    customLevel: String,
    onCustomLevel: (String) -> Unit,
    gap: Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StepLabel("GENDER")
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GENDER_OPTIONS.forEach { option ->
                ChoiceCard(
                    label = option,
                    selected = option == gender,
                    onClick = { onGender(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(gap))

        StepLabel("ACADEMIC LEVEL")
        LEVEL_GROUPS.forEach { (group, title) ->
            val items = AcademicLevels.ALL.filter { it.group == group }
            if (items.isEmpty()) return@forEach
            Spacer(modifier = Modifier.height(10.dp))
            if (group != AcademicLevels.GROUP_OTHER) {
                Text(
                    text = title.uppercase(),
                    color = WisdomMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
            items.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { item ->
                        ChoiceCard(
                            label = item.label,
                            selected = item.value == level,
                            onClick = { onLevel(item.value) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (level == "Other") {
            Spacer(modifier = Modifier.height(gap))
            AuthField(
                label = "Tell us what you are studying",
                value = customLevel,
                onValueChange = { onCustomLevel(it.take(OTHER_LEVEL_MAX)) },
                placeholder = "e.g. Master's, Diploma, Self-learner...",
                leadingIcon = Icons.Filled.School
            )
        }
        Spacer(modifier = Modifier.height(gap))
    }
}

@Composable
private fun StepLabel(text: String) {
    Text(
        text = text,
        color = WisdomInk.copy(alpha = 0.85f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
    )
}

/** One tappable choice (gender chip or level card): cyan border and a check mark when selected. */
@Composable
private fun ChoiceCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) WisdomCyan.copy(alpha = 0.16f) else WisdomInk.copy(alpha = 0.05f))
            .border(
                BorderStroke(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) WisdomCyan.copy(alpha = 0.85f) else WisdomInk.copy(alpha = 0.12f)
                ),
                shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) WisdomCyan else WisdomInk,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        if (selected) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = WisdomCyan,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
