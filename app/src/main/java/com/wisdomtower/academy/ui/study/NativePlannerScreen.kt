package com.wisdomtower.academy.ui.study

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentOrange
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomAccentViolet
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Native port of `website-reference/src/components/learning/StudyPlanner.tsx` (owner problem 1, 2026-10-12, F7 part 3):
 * the "24-Hour Weekly Timetable" with the "Minute-Accurate" tag, Morning / Afternoon / Evening / Now jump buttons,
 * "Add Block", day chips ("Full Week", Mon to Sun with a dot for today and a count), the 24-hour grid (12 AM to 11 PM,
 * 38 dp per hour, "GMT+3" corner, a red line for the current time), blocks that show only their title, and the editor
 * ("Schedule Study Block" / "Edit Study Session": title, day, start and end time, +30m/+45m/+60m/+90m/+120m helpers,
 * 6 colour tags, Delete / Cancel / Create or Save). Same rules: an end time not after the start becomes start + 60 min
 * (at most 23:59), a block is drawn at least 15 minutes long, new blocks cycle through the colours.
 *
 * Differences: the time fields open the system time picker (the website uses the browser's time input); the website's
 * 7 invented sample blocks are NOT created (the planner starts empty, owner rule "no fabricated content"); the website's
 * hover texts and the "Today" tag (hidden on phones there) are not drawn; a Close button is added (this is a full screen).
 *
 * D6g (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. Night keeps the exact old literals as `dayNight` pairs (the `Pl...`
 * getters at the bottom of the file). By day: the page card, grid, rail and chips are the ivory glass; titles, hour labels
 * and block titles are ink or muted slate; the selected day chip, "Add Block", "Create / Save", the selected day in the
 * editor and the today dot use the deep teal (white text on the solid ones); the +Nm helper in use is the day gold
 * (F1DFB4 with 5C3F0A); the red "now" line and Delete use the day rose; the sun / twilight / moon jump icons use the day
 * amber / orange / violet; the editor is the 95 percent ivory overlay. The six block colours (fill / line) are kept in
 * both modes (they are tints on the grid; block titles are ink by day, white at night). Layout and behaviour unchanged.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
private val HOUR_HEIGHT = 38.dp

private val PLANNER_DAYS = listOf(
    "Mon" to "Monday", "Tue" to "Tuesday", "Wed" to "Wednesday", "Thu" to "Thursday",
    "Fri" to "Friday", "Sat" to "Saturday", "Sun" to "Sunday"
)

private data class PlannerPalette(
    val id: String,
    val name: String,
    val classes: String,
    val fill: Color,
    val line: Color
)

// `classes` are the website's own strings (stored in each block), `fill` / `line` are the matching Tailwind 500 / 400 colours.
private val PLANNER_PALETTES = listOf(
    PlannerPalette("cyan", "Cyan", "bg-cyan-500/20 border-cyan-400/80 text-cyan-100 hover:bg-cyan-500/30", Color(0xFF06B6D4), Color(0xFF22D3EE)),
    PlannerPalette("amber", "Gold", "bg-amber-500/20 border-amber-400/80 text-amber-100 hover:bg-amber-500/30", Color(0xFFF59E0B), Color(0xFFFBBF24)),
    PlannerPalette("emerald", "Emerald", "bg-emerald-500/20 border-emerald-400/80 text-emerald-100 hover:bg-emerald-500/30", Color(0xFF10B981), Color(0xFF34D399)),
    PlannerPalette("violet", "Purple", "bg-violet-500/20 border-violet-400/80 text-violet-100 hover:bg-violet-500/30", Color(0xFF8B5CF6), Color(0xFFA78BFA)),
    PlannerPalette("rose", "Rose", "bg-rose-500/20 border-rose-400/80 text-rose-100 hover:bg-rose-500/30", Color(0xFFF43F5E), Color(0xFFFB7185)),
    PlannerPalette("sky", "Sky", "bg-sky-500/20 border-sky-400/80 text-sky-100 hover:bg-sky-500/30", Color(0xFF0EA5E9), Color(0xFF38BDF8))
)

private fun paletteFor(classes: String): PlannerPalette =
    PLANNER_PALETTES.firstOrNull { it.classes == classes }
        ?: PLANNER_PALETTES.firstOrNull { classes.contains("-${it.id}-") }
        ?: PLANNER_PALETTES[0]

private fun plannerHm(h: Int, m: Int): String = String.format(Locale.US, "%02d:%02d", h, m)

private fun plannerTimeToMinutes(t: String): Int {
    if (t.isEmpty()) return 0
    val parts = t.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return h * 60 + m
}

private fun plannerMinutesToTime(total: Int): String {
    val c = total.coerceIn(0, 24 * 60 - 1)
    return plannerHm(c / 60, c % 60)
}

private fun plannerFormat12(t: String): String {
    if (t.isEmpty()) return ""
    val mins = plannerTimeToMinutes(t)
    val h = mins / 60
    val m = mins % 60
    val ampm = if (h >= 12) "PM" else "AM"
    val displayH = if (h % 12 == 0) 12 else h % 12
    return String.format(Locale.US, "%d:%02d %s", displayH, m, ampm)
}

private fun plannerHourLabel(h: Int): String {
    val ampm = if (h >= 12) "PM" else "AM"
    val displayH = if (h % 12 == 0) 12 else h % 12
    return "$displayH $ampm"
}

private fun plannerNowMinutes(): Int {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
}

/** Monday = 0 ... Sunday = 6 (website `todayIndex`). */
private fun plannerTodayIndex(): Int = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

@Composable
fun NativePlannerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { NativePlannerState.init(context) }
    val blocks = NativePlannerState.blocks

    var dayFilter by remember { mutableStateOf<Int?>(null) } // null = Full Week
    var nowMinutes by remember { mutableIntStateOf(plannerNowMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowMinutes = plannerNowMinutes()
        }
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scrollToHour: (Int) -> Unit = { hour ->
        val y = with(density) { (HOUR_HEIGHT * hour - 30.dp).toPx() }.toInt().coerceAtLeast(0)
        scope.launch { scrollState.animateScrollTo(y) }
    }
    // Like the website: on first open, scroll to the current hour (between 6 AM and 8 PM).
    LaunchedEffect(Unit) {
        delay(150L)
        val currentH = (nowMinutes / 60 - 1).coerceIn(6, 20)
        scrollToHour(currentH)
    }

    // Editor state
    var dialogOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftDay by remember { mutableIntStateOf(0) }
    var draftStart by remember { mutableStateOf("09:00") }
    var draftEnd by remember { mutableStateOf("10:30") }
    var draftColor by remember { mutableStateOf(PLANNER_PALETTES[0].classes) }

    val today = plannerTodayIndex()

    fun openAdd(day: Int, hour: Int) {
        editingId = null
        draftTitle = ""
        draftDay = day
        draftStart = plannerHm(hour, 0)
        draftEnd = plannerHm((hour + 1) % 24, 30)
        draftColor = PLANNER_PALETTES[blocks.size % PLANNER_PALETTES.size].classes
        dialogOpen = true
    }

    fun openEdit(b: StudyBlock) {
        editingId = b.id
        draftTitle = b.title
        draftDay = b.day
        draftStart = b.startTime
        draftEnd = b.endTime
        draftColor = b.color
        dialogOpen = true
    }

    fun saveDraft() {
        val title = draftTitle.trim()
        if (title.isEmpty()) return // the website's title field is "required"
        val startM = plannerTimeToMinutes(draftStart)
        var endM = plannerTimeToMinutes(draftEnd)
        if (endM <= startM) endM = minOf(24 * 60 - 1, startM + 60)
        val cleanStart = plannerMinutesToTime(startM)
        val cleanEnd = plannerMinutesToTime(endM)
        val id = editingId
        if (id != null) {
            NativePlannerState.save(
                blocks.map {
                    if (it.id == id) it.copy(title = title, day = draftDay, startTime = cleanStart, endTime = cleanEnd, color = draftColor) else it
                }
            )
        } else {
            NativePlannerState.save(
                blocks + StudyBlock(NativePlannerState.newId(), draftDay, cleanStart, cleanEnd, title, draftColor)
            )
        }
        dialogOpen = false
        editingId = null
    }

    fun deleteDraft() {
        val id = editingId ?: return
        NativePlannerState.save(blocks.filter { it.id != id })
        dialogOpen = false
        editingId = null
    }

    BackHandler(enabled = true) { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PlCardFill)
                .border(BorderStroke(1.dp, PlPageBorder), RoundedCornerShape(24.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header with ScreenBackButton (48dp touch target, top-left)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScreenBackButton(onBack)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PlAccent, modifier = Modifier.size(20.dp))
                        Text("24-Hour Weekly Timetable", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WisdomInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PlTagFill)
                                .border(BorderStroke(1.dp, PlTagBorder), RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Minute-Accurate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlAccentText, maxLines = 1)
                        }
                    }
                    Text(
                        "Vertical 24 hours (12 AM – 11 PM) · Tap any time slot to set or adjust a block",
                        fontSize = 11.sp,
                        color = PlSlate300,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Quick time jumpers + Add Block
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PlQuietFill)
                        .border(BorderStroke(1.dp, PlChipBorder), RoundedCornerShape(50))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlannerJumpButton(onClick = { scrollToHour(7) }) {
                        Icon(Icons.Filled.WbSunny, contentDescription = "Jump to Morning (7 AM)", tint = PlSun, modifier = Modifier.size(14.dp))
                    }
                    PlannerJumpButton(onClick = { scrollToHour(13) }) {
                        Icon(Icons.Filled.WbTwilight, contentDescription = "Jump to Afternoon (1 PM)", tint = PlTwilight, modifier = Modifier.size(14.dp))
                    }
                    PlannerJumpButton(onClick = { scrollToHour(19) }) {
                        Icon(Icons.Filled.NightsStay, contentDescription = "Jump to Evening (7 PM)", tint = PlMoon, modifier = Modifier.size(14.dp))
                    }
                    PlannerJumpButton(onClick = { scrollToHour(nowMinutes / 60) }) {
                        Text("Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PlPrimaryBg)
                        .clickable { openAdd(dayFilter ?: today, 9) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = PlPrimaryInk, modifier = Modifier.size(14.dp))
                    Text("Add Block", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PlPrimaryInk)
                }
            }

            // 2. Day chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlannerDayChip(selected = dayFilter == null, onClick = { dayFilter = null }) {
                    Text("Full Week", fontSize = 12.sp, fontWeight = if (dayFilter == null) FontWeight.SemiBold else FontWeight.Medium, color = if (dayFilter == null) PlPrimaryInk else PlSlate300)
                }
                PLANNER_DAYS.forEachIndexed { i, d ->
                    val selected = dayFilter == i
                    val count = blocks.count { it.day == i }
                    PlannerDayChip(selected = selected, onClick = { dayFilter = i }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(d.first, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) PlPrimaryInk else PlSlate300)
                            if (i == today) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (selected) PlPrimaryInk else PlAccent))
                            }
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (selected) PlChipCountOn else PlChipCountOff)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text("$count", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = if (selected) PlPrimaryInk else PlSlate300)
                                }
                            }
                        }
                    }
                }
            }

            // 3. The 24-hour grid
            val visibleDays: List<Int> = dayFilter?.let { listOf(it) } ?: (0..6).toList()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PlCardFill)
                    .border(BorderStroke(1.dp, PlGridBorder), RoundedCornerShape(16.dp))
            ) {
                // Header row: corner + day headers (stays in place while the hours scroll)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(40.dp)
                            .background(PlHeadFill)
                            .border(BorderStroke(0.5.dp, PlGridBorder)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GMT+3", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PlSlate400)
                    }
                    visibleDays.forEach { d ->
                        val isToday = d == today
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(if (isToday) PlHeadTodayFill else PlHeadFill)
                                .border(BorderStroke(0.5.dp, PlGridBorder)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(PLANNER_DAYS[d].first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isToday) PlAccentText else WisdomInk)
                            Text("${blocks.count { it.day == d }}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = PlSlate400)
                        }
                    }
                }

                // Scrolling part: hour rail + one column per day
                Row(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
                    Column(modifier = Modifier.width(38.dp).background(PlRailFill)) {
                        for (h in 0 until 24) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(HOUR_HEIGHT).padding(end = 3.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(plannerHourLabel(h), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PlSlate400, maxLines = 1)
                                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(PlLine))
                            }
                        }
                    }
                    visibleDays.forEach { d ->
                        val isToday = d == today
                        val dayBlocks = blocks.filter { it.day == d }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(HOUR_HEIGHT * 24)
                                .background(if (isToday) PlTodayTint else Color.Transparent)
                        ) {
                            // Hour cells: tap to add a block at that hour
                            Column(modifier = Modifier.fillMaxWidth()) {
                                for (h in 0 until 24) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(HOUR_HEIGHT)
                                            .clickable { openAdd(d, h) }
                                    ) {
                                        Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(1.dp).background(PlLineHalf))
                                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(PlLine))
                                    }
                                }
                            }

                            // Current time line (today's column only)
                            if (isToday) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = HOUR_HEIGHT * (nowMinutes / 60f)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PlNow))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(PlNow.copy(alpha = 0.80f)))
                                }
                            }

                            // Blocks: only the title is shown
                            dayBlocks.forEach { b ->
                                val startM = plannerTimeToMinutes(b.startTime)
                                val endM = plannerTimeToMinutes(b.endTime)
                                val durationM = maxOf(15, endM - startM)
                                val pal = paletteFor(b.color)
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .fillMaxWidth()
                                        .offset(y = HOUR_HEIGHT * (startM / 60f))
                                        .height(maxOf(18.dp, HOUR_HEIGHT * (durationM / 60f)))
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(pal.fill.copy(alpha = 0.20f))
                                        .border(BorderStroke(1.dp, pal.line.copy(alpha = 0.80f)), RoundedCornerShape(6.dp))
                                        .clickable { openEdit(b) }
                                        .padding(horizontal = 3.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(b.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WisdomInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Editor
    if (dialogOpen) {
        Dialog(
            onDismissRequest = { dialogOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding()
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PlDialogFill)
                    .border(BorderStroke(1.dp, PlDialogBorder), RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PlTagFill)
                                .border(BorderStroke(1.dp, PlTagBorder), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = PlAccentText, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(if (editingId != null) "Edit Study Session" else "Schedule Study Block", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                            Text("Set subject & exact start/finish clock times", fontSize = 11.sp, color = PlSlate400)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, PlChipBorder), RoundedCornerShape(12.dp))
                            .clickable { dialogOpen = false }
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = PlSlate400, modifier = Modifier.size(16.dp))
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PlDivider))

                // Title
                Column {
                    Text("Subject or Session Title", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlSlate200, modifier = Modifier.padding(bottom = 6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PlFieldFill)
                            .border(BorderStroke(1.dp, PlDialogBorder), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        BasicTextField(
                            value = draftTitle,
                            onValueChange = { draftTitle = it },
                            singleLine = true,
                            textStyle = TextStyle(color = WisdomInk, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(PlAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (draftTitle.isEmpty()) {
                            Text("e.g. Mathematics - Calculus Integration", fontSize = 14.sp, color = PlSlate500)
                        }
                    }
                }

                // Day of the week
                Column {
                    Text("Day of the Week", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlSlate200, modifier = Modifier.padding(bottom = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PLANNER_DAYS.forEachIndexed { i, d ->
                            val sel = draftDay == i
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) PlAccent else PlOptFill)
                                    .border(BorderStroke(1.dp, if (sel) PlAccent else PlChipBorder), RoundedCornerShape(12.dp))
                                    .clickable { draftDay = i }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(d.first, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Bold, color = if (sel) PlPrimaryInk else PlSlate300, maxLines = 1)
                            }
                        }
                    }
                }

                // Start / End time (system time picker, 24-hour)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlannerTimeField(
                        label = "Start Time (24h)",
                        value = draftStart,
                        modifier = Modifier.weight(1f),
                        onPick = { h, m -> draftStart = plannerHm(h, m) }
                    )
                    PlannerTimeField(
                        label = "End Time (24h)",
                        value = draftEnd,
                        modifier = Modifier.weight(1f),
                        onPick = { h, m -> draftEnd = plannerHm(h, m) }
                    )
                }

                // Quick durations
                Column {
                    Text("Quick Duration Helpers (From Start Time)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PlSlate400, modifier = Modifier.padding(bottom = 6.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val currentDur = plannerTimeToMinutes(draftEnd) - plannerTimeToMinutes(draftStart)
                        listOf(30, 45, 60, 90, 120).forEach { mins ->
                            val active = currentDur == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) PlGoldFill else PlOptSoft)
                                    .border(BorderStroke(1.dp, if (active) PlGoldFill else PlChipBorder), RoundedCornerShape(8.dp))
                                    .clickable { draftEnd = plannerMinutesToTime(plannerTimeToMinutes(draftStart) + mins) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("+${mins}m", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (active) PlGoldInk else PlSlate300)
                            }
                        }
                    }
                }

                // Colour tag
                Column {
                    Text("Color Tag", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlSlate200, modifier = Modifier.padding(bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PLANNER_PALETTES.forEach { p ->
                            val sel = draftColor == p.classes
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .scale(if (sel) 1.1f else 1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(p.line.copy(alpha = if (sel) 1f else 0.75f))
                                    .border(BorderStroke(2.dp, if (sel) PlSwatchRing else Color.Transparent), RoundedCornerShape(12.dp))
                                    .clickable { draftColor = p.classes },
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel) Icon(Icons.Filled.Check, contentDescription = p.name, tint = Color(0xFF020617), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PlDivider))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (editingId != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(BorderStroke(1.dp, PlDeleteBorder), RoundedCornerShape(50))
                                .clickable { deleteDraft() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = PlRose, modifier = Modifier.size(14.dp))
                            Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PlRose)
                        }
                    } else {
                        Box(modifier = Modifier.size(1.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(BorderStroke(1.dp, PlCancelBorder), RoundedCornerShape(50))
                                .clickable { dialogOpen = false }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PlSlate300)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PlPrimaryBg)
                                .clickable { saveDraft() }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(if (editingId != null) "Save" else "Create", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PlPrimaryInk, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// ---- D6g day / night colours. Night value = the exact old literal; day value = Ivory & Navy token. Getters: read while composing. ----
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val PlSlate500: Color get() = dayNight(Color(0xFF64748B), WisdomMuted)
private val PlSlate400: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)
private val PlSlate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomMuted)
private val PlSlate200: Color get() = dayNight(Color(0xFFE2E8F0), WisdomInk)
private val PlAccent: Color get() = dayNight(Color(0xFF22D3EE), WisdomCyan)
private val PlAccentText: Color get() = dayNight(Color(0xFF67E8F9), WisdomCyan)
private val PlSun: Color get() = dayNight(Color(0xFFFCD34D), WisdomAccentAmber)
private val PlTwilight: Color get() = dayNight(Color(0xFFFDBA74), WisdomAccentOrange)
private val PlMoon: Color get() = dayNight(Color(0xFFC4B5FD), WisdomAccentViolet)
private val PlNow: Color get() = dayNight(Color(0xFFFB7185), WisdomAccentRose)
private val PlRose: Color get() = dayNight(Color(0xFFFDA4AF), WisdomAccentRose)
private val PlDeleteBorder: Color get() = dayNight(Color(0xFFF43F5E).copy(alpha = 0.40f), WisdomAccentRose.copy(alpha = 0.50f))

private val PlCardFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFill)
private val PlPageBorder: Color get() = dayNight(Color(0xFF22D3EE).copy(alpha = 0.30f), WisdomCyan.copy(alpha = 0.35f))
private val PlGridBorder: Color get() = dayNight(Color.White.copy(alpha = 0.15f), WisdomGlassBorder)
private val PlQuietFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFillQuiet)
private val PlChipBorder: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val PlTagFill: Color get() = dayNight(Color(0xFF22D3EE).copy(alpha = 0.20f), WisdomCyan.copy(alpha = 0.14f))
private val PlTagBorder: Color get() = dayNight(Color(0xFF22D3EE).copy(alpha = 0.30f), WisdomCyan.copy(alpha = 0.40f))
private val PlChipCountOn: Color get() = dayNight(Color(0xFF0F172A).copy(alpha = 0.30f), Color.White.copy(alpha = 0.25f))
private val PlChipCountOff: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomInk.copy(alpha = 0.08f))

private val PlHeadFill: Color get() = dayNight(Color(0xFF0B1528), WisdomGlassFillStrong)
private val PlHeadTodayFill: Color get() = dayNight(Color(0xFF0E213D), WisdomCyan.copy(alpha = 0.14f))
private val PlRailFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFillQuiet)
private val PlTodayTint: Color get() = dayNight(Color(0xFF06B6D4).copy(alpha = 0.02f), WisdomCyan.copy(alpha = 0.05f))
private val PlLine: Color get() = dayNight(Color.White.copy(alpha = 0.08f), WisdomInk.copy(alpha = 0.10f))
private val PlLineHalf: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomInk.copy(alpha = 0.05f))

private val PlPrimaryBg: Color get() = dayNight(Color.White, WisdomCyan)
private val PlPrimaryInk: Color get() = dayNight(Color(0xFF020617), Color.White)

private val PlDialogFill: Color get() = dayNight(Color(0x4D0B1528), Color(0xF2F8F5EE))
private val PlDialogBorder: Color get() = dayNight(Color.White.copy(alpha = 0.20f), WisdomGlassBorder)
private val PlDivider: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val PlFieldFill: Color get() = dayNight(Color(0x4D020617).copy(alpha = 0.80f), WisdomGlassFillStrong)
private val PlOptFill: Color get() = dayNight(Color(0xFF020617).copy(alpha = 0.60f), WisdomGlassFillQuiet)
private val PlOptSoft: Color get() = dayNight(Color.White.copy(alpha = 0.05f), WisdomGlassFillQuiet)
private val PlGoldFill: Color get() = dayNight(Color(0xFFFBBF24), Color(0xFFF1DFB4))
private val PlGoldInk: Color get() = dayNight(Color(0xFF020617), Color(0xFF5C3F0A))
private val PlSwatchRing: Color get() = dayNight(Color.White, WisdomInk)
private val PlCancelBorder: Color get() = dayNight(Color.White.copy(alpha = 0.15f), WisdomGlassBorder)

@Composable
private fun PlannerJumpButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun PlannerDayChip(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) PlPrimaryBg else PlQuietFill)
            .border(BorderStroke(1.dp, if (selected) PlPrimaryBg else PlChipBorder), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun PlannerTimeField(label: String, value: String, modifier: Modifier = Modifier, onPick: (Int, Int) -> Unit) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlSlate200, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PlFieldFill)
                .border(BorderStroke(1.dp, PlDialogBorder), RoundedCornerShape(12.dp))
                .clickable {
                    val mins = plannerTimeToMinutes(value)
                    TimePickerDialog(context, { _, h, m -> onPick(h, m) }, mins / 60, mins % 60, true).show()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(value, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = WisdomInk)
        }
        Text(plannerFormat12(value), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = PlAccentText, modifier = Modifier.padding(top = 4.dp))
    }
}
