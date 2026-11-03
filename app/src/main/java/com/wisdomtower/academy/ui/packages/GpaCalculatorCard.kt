package com.wisdomtower.academy.ui.packages

import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentPurple
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDark
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomModernCard
import com.wisdomtower.academy.ui.theme.WisdomMuted
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Native port of the website GPA calculator:
 * - website-reference/src/components/CollapsibleGpa.tsx (title "GPA calculator", subtitle "Estimate your average", closed by default)
 * - website-reference/src/components/freshman/GpaCalculator.tsx (rows: course, credits, grade; letter / percent / points input; grade-scale table; total credits + semester GPA)
 * - website-reference/src/data/gpa-scale.ts (the 12 grade bands below are copied from it)
 *
 * The course list is the Freshman subject list without "physical-fitness" (pass/fail), as on the website.
 * Nothing here is invented: every text and number comes from those three files.
 *
 * Glass look (docs/GLASS_PLAN.md area 8): the three inner panels (course rows, grade-scale table, result box) use the
 * shared glass tokens instead of dark fills, so they match the glass card around them.
 *
 * S3 part 2 (2026-10-17, docs/SESSION_2026-10-17_S3_PART2_PACKAGES.md): TalkBack. The header is a button that says
 * "Expanded" / "Collapsed"; Grade scale, Add course, the grade-type switch, the remove icon and the dropdowns have roles
 * and 48 dp targets; the two number fields and the grade-type switch have spoken labels (not shown on screen).
 */
private data class GpaGradeBand(
    val min: Double,
    val maxExclusive: Double,
    val includeMax: Boolean,
    val letter: String,
    val points: Double,
    val status: String
)

private val GPA_GRADE_BANDS = listOf(
    GpaGradeBand(90.0, 100.0, true, "A+", 4.0, "Excellent"),
    GpaGradeBand(85.0, 90.0, false, "A", 4.0, "Excellent"),
    GpaGradeBand(80.0, 85.0, false, "A-", 3.75, "Excellent"),
    GpaGradeBand(75.0, 80.0, false, "B+", 3.5, "Very Good"),
    GpaGradeBand(70.0, 75.0, false, "B", 3.0, "Very Good"),
    GpaGradeBand(65.0, 70.0, false, "B-", 2.75, "Good"),
    GpaGradeBand(60.0, 65.0, false, "C+", 2.5, "Good"),
    GpaGradeBand(50.0, 60.0, false, "C", 2.0, "Satisfactory"),
    GpaGradeBand(45.0, 50.0, false, "C-", 1.75, "Unsatisfactory"),
    GpaGradeBand(40.0, 45.0, false, "D", 1.0, "Very Poor"),
    GpaGradeBand(30.0, 40.0, false, "Fx", 0.0, "Fail (Re-exam)"),
    GpaGradeBand(0.0, 30.0, false, "F", 0.0, "Fail (Repeat course)")
)

private val GPA_POINT_OPTIONS = listOf(4.0, 3.75, 3.5, 3.0, 2.75, 2.5, 2.0, 1.75, 1.0, 0.0)

private fun gpaTwoDecimals(value: Double): String = String.format(Locale.US, "%.2f", value)

private class GpaRow(val id: Int) {
    var subjectId by mutableStateOf("")
    var credits by mutableStateOf("3")
    /** 0 = letter, 1 = percent, 2 = points (same toggle order as the website: letter, percent, points) */
    var mode by mutableIntStateOf(0)
    var percent by mutableStateOf("")
    var letter by mutableStateOf("A")
    var points by mutableStateOf("4.00")
}

private fun gpaBandFromPercent(percent: Double): GpaGradeBand {
    val p = percent.coerceIn(0.0, 100.0)
    for (b in GPA_GRADE_BANDS) {
        if (b.includeMax) {
            if (p >= b.min && p <= b.maxExclusive) return b
        } else if (p >= b.min && p < b.maxExclusive) {
            return b
        }
    }
    return GPA_GRADE_BANDS.last()
}

private fun gpaResolveBand(row: GpaRow): GpaGradeBand? {
    return when (row.mode) {
        1 -> {
            val n = row.percent.toDoubleOrNull()
            if (n == null) null else gpaBandFromPercent(n)
        }
        0 -> GPA_GRADE_BANDS.firstOrNull { it.letter.equals(row.letter, ignoreCase = true) }
        else -> {
            val n = row.points.toDoubleOrNull()
            if (n == null) {
                null
            } else {
                GPA_GRADE_BANDS.firstOrNull { abs(it.points - n) < 0.001 } ?: GPA_GRADE_BANDS.last()
            }
        }
    }
}

@Composable
fun GpaCalculatorCard() {
    var isOpen by remember { mutableStateOf(false) }
    var showScale by remember { mutableStateOf(false) }
    val subjects = remember { FRESHMAN_SUBJECTS.filter { it.id != "physical-fitness" } }
    val rows = remember { mutableStateListOf(GpaRow(0)) }
    var nextId by remember { mutableIntStateOf(1) }

    WisdomModernCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.wisdomTouchFeel()
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        stateDescription = if (isOpen) "Expanded" else "Collapsed"
                    }
                    .clickable(role = Role.Button) { isOpen = !isOpen },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = WisdomAccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "GPA calculator",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WisdomInk
                        )
                        Text(
                            text = "Estimate your average",
                            fontSize = 11.sp,
                            color = WisdomMuted
                        )
                    }
                }

                Icon(
                    imageVector = if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = WisdomMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add courses and grades; calculated with the official national scale.",
                            fontSize = 11.sp,
                            color = WisdomMuted,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.wisdomTouchFeel()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(BorderStroke(1.dp, WisdomAccentPurple.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button) { showScale = !showScale }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (showScale) "Hide scale" else "Grade scale",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WisdomAccentPurple
                            )
                        }
                    }

                    if (showScale) {
                        GpaScaleTable()
                    }

                    val usedIds = rows.map { it.subjectId }.filter { it.isNotBlank() }.toSet()
                    rows.forEach { row ->
                        val available = subjects.filter { it.id == row.subjectId || it.id !in usedIds }
                        GpaRowEditor(
                            row = row,
                            available = available,
                            canRemove = rows.size > 1,
                            onRemove = { rows.remove(row) }
                        )
                    }

                    val canAdd = rows.size < subjects.size
                    Box(
                        modifier = Modifier.wisdomTouchFeel()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, WisdomAccentPurple.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                            .clickable(enabled = canAdd, role = Role.Button) {
                                rows.add(GpaRow(nextId))
                                nextId += 1
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (canAdd) WisdomAccentPurple else WisdomMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add course",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canAdd) WisdomAccentPurple else WisdomMuted
                            )
                        }
                    }

                    // Results: same maths as the website (credit-weighted points, credits at least 0.5)
                    var creditSum = 0.0
                    var pointCreditSum = 0.0
                    var counted = 0
                    for (row in rows) {
                        if (row.subjectId.isBlank()) continue
                        val band = gpaResolveBand(row) ?: continue
                        val cr = max(0.5, row.credits.toDoubleOrNull() ?: 0.0)
                        creditSum += cr
                        pointCreditSum += band.points * cr
                        counted += 1
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(WisdomGlassFillStrong)
                            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (counted == 0 || creditSum <= 0.0) {
                            Text(
                                text = "Select courses and enter grades to calculate your semester GPA.",
                                fontSize = 12.sp,
                                color = WisdomMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            val gpa = pointCreditSum / creditSum
                            val gpaColor = when {
                                gpa >= 3.5 -> WisdomAccentEmerald
                                gpa >= 2.5 -> WisdomAccentSky
                                gpa >= 2.0 -> WisdomAccentAmber
                                else -> WisdomAccentRose
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TOTAL CREDITS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WisdomMuted,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f", creditSum),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WisdomInk
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "SEMESTER GPA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WisdomMuted,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = gpaTwoDecimals(gpa),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = gpaColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GpaScaleTable() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WisdomGlassFill)
            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Interval %", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, modifier = Modifier.weight(1.2f))
            Text("Letter", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, modifier = Modifier.weight(0.6f))
            Text("Points", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, modifier = Modifier.weight(0.7f))
            Text("Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, modifier = Modifier.weight(1.5f))
        }
        GPA_GRADE_BANDS.forEach { b ->
            val interval = "[${b.min.toInt()}, ${b.maxExclusive.toInt()}${if (b.includeMax) "]" else ")"}"
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(interval, fontSize = 11.sp, color = WisdomMuted, modifier = Modifier.weight(1.2f))
                Text(b.letter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomAccentPurple, modifier = Modifier.weight(0.6f))
                Text(gpaTwoDecimals(b.points), fontSize = 11.sp, color = WisdomInk, modifier = Modifier.weight(0.7f))
                Text(b.status, fontSize = 11.sp, color = WisdomMuted, modifier = Modifier.weight(1.5f))
            }
        }
    }
}

@Composable
private fun GpaRowEditor(
    row: GpaRow,
    available: List<NativeSubject>,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    val band = gpaResolveBand(row)
    val selectedName = available.firstOrNull { it.id == row.subjectId }?.name
    val letters = GPA_GRADE_BANDS.map { it.letter }
    val pointLabels = GPA_POINT_OPTIONS.map { gpaTwoDecimals(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WisdomGlassFill)
            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GpaDropdown(
                label = selectedName ?: "Select course\u2026",
                options = available.map { it.name },
                onPick = { index -> row.subjectId = available[index].id },
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove course",
                tint = if (canRemove) WisdomMuted else WisdomMuted.copy(alpha = 0.25f),
                modifier = Modifier.wisdomTouchFeel()
                    .minimumInteractiveComponentSize()
                    .size(20.dp)
                    .clickable(enabled = canRemove, role = Role.Button, onClick = onRemove)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GpaNumberField(
                value = row.credits,
                onChange = { row.credits = it },
                placeholder = "Cr",
                description = "Credits",
                modifier = Modifier.weight(0.7f)
            )

            when (row.mode) {
                0 -> GpaDropdown(
                    label = row.letter,
                    options = letters,
                    onPick = { index -> row.letter = letters[index] },
                    modifier = Modifier.weight(1.3f)
                )
                1 -> GpaNumberField(
                    value = row.percent,
                    onChange = { row.percent = it },
                    placeholder = "%",
                    description = "Grade in percent",
                    modifier = Modifier.weight(1.3f)
                )
                else -> GpaDropdown(
                    label = row.points,
                    options = pointLabels,
                    onPick = { index -> row.points = pointLabels[index] },
                    modifier = Modifier.weight(1.3f)
                )
            }

            Box(
                modifier = Modifier.wisdomTouchFeel()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(WisdomInk.copy(alpha = 0.04f))
                    .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { row.mode = (row.mode + 1) % 3 }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Spoken label (not shown): the letters L / % / Pt alone mean nothing to TalkBack.
                val modeSpoken = when (row.mode) {
                    0 -> "Grade input: letter. Tap to change."
                    1 -> "Grade input: percent. Tap to change."
                    else -> "Grade input: points. Tap to change."
                }
                Text(
                    text = when (row.mode) {
                        0 -> "L"
                        1 -> "%"
                        else -> "Pt"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WisdomMuted,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = modeSpoken }
                )
            }

            Text(
                text = if (band != null) String.format(Locale.US, "%.1f", band.points) + " pts" else "-",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WisdomAccentPurple
            )
        }
    }
}

@Composable
private fun GpaDropdown(
    label: String,
    options: List<String>,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.wisdomTouchFeel()
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(WisdomInk.copy(alpha = 0.04f))
                .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
                .clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = WisdomInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = WisdomMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 12.sp) },
                    onClick = {
                        onPick(index)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GpaNumberField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(WisdomInk.copy(alpha = 0.04f))
            .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = 12.sp,
                color = WisdomMuted,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { text ->
                if (text.length <= 5 && text.all { c -> c.isDigit() || c == '.' }) onChange(text)
            },
            singleLine = true,
            textStyle = TextStyle(
                color = WisdomInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(WisdomCyan),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description }
        )
    }
}
