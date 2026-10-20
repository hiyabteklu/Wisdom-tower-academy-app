package com.wisdomtower.academy.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.BroadcastsRepository
import com.wisdomtower.academy.data.admin.CompetitionItem
import com.wisdomtower.academy.data.admin.CompetitionsRepository
import com.wisdomtower.academy.data.admin.CompetitionsResult
import com.wisdomtower.academy.data.admin.EntriesResult
import com.wisdomtower.academy.data.admin.EntryItem
import com.wisdomtower.academy.data.admin.ExamOption
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomNavy
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ADMIN-native N9 (docs/ADMIN_NATIVE_PLAN.md). Native version of the website Competitions panel
// (CompetitionsAdminPanel.tsx and EntryFlagsInfo.tsx): create a contest from an exam of the exam hub, then show or
// hide, pause or resume the countdown, start now, reschedule, cancel, list the ranked entries with their integrity
// flags, and disqualify an entry. One LazyColumn, no sideways scrolling. Every typed time is Ethiopia time.
// Reuses the admin kit (AdminToneBadge, AdminActionButton, AdminConfirmDialog, AdminMessageCard, AdminTextField,
// AdminTextArea, AdminSearchBox, AdminToast). Sentences are copied from the website panel.
// B3a (2026-10-20, docs/ADMIN_DASHBOARD_NEXT.md): the create form and the exam picker are AdminDetailPanels.
// B3b and B3c (2026-10-20): every contest is a compact row (badges, title, start, arrow). A tap opens the contest panel
// (details plus all the actions). Reschedule is a small panel over it with Save fixed at the bottom. Entries is a panel
// over it with a count and a search box; the flag details stay inside the entry. The list stays composed underneath.
// Logic, texts, confirm dialogs and every CompetitionsRepository call are unchanged. Layout change only.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val CompOrange: Color = Color(0xFFFB923C)

private fun phaseTone(key: String): Color = when (key) {
    "live" -> WisdomAccentEmerald
    "upcoming" -> WisdomCyan
    "paused" -> WisdomAccentAmber
    "cancelled" -> AdminRose
    else -> WisdomMuted
}

private class CompAction(val label: String, val tone: Color, val onClick: () -> Unit)

private fun examLabel(x: ExamOption): String {
    var text = x.title
    val pkg = x.packageId
    val minutes = x.durationMin
    if (pkg != null) text += " (" + pkg + ")"
    if (minutes != null) text += " - " + minutes + " min"
    return text
}

private fun compStateKey(c: CompetitionItem, nowMs: Long): String {
    if (c.countdownState == "cancelled") return "cancelled"
    if (c.countdownState == "paused") return "paused"
    return CompetitionsRepository.phaseOf(c, nowMs)
}

@Composable
internal fun CompetitionsScreen(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<CompetitionsResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var busyKey by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf("") }

    // Create form (the panel New competition)
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var resourceId by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var startTime by rememberSaveable { mutableStateOf("") }
    var durationText by rememberSaveable { mutableStateOf("") }
    var audience by rememberSaveable { mutableStateOf("") }
    var pointsText by rememberSaveable { mutableStateOf("0") }
    var prizeTitle by rememberSaveable { mutableStateOf("") }
    var prizeDetails by rememberSaveable { mutableStateOf("") }
    var requiresPackage by rememberSaveable { mutableStateOf(false) }
    var publishNow by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }
    var confirmCreate by remember { mutableStateOf(false) }

    // The contest panel (one contest at a time)
    var detailId by remember { mutableStateOf<String?>(null) }

    // Reschedule panel (over the contest panel)
    var rescheduleId by remember { mutableStateOf<String?>(null) }
    var editDate by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editDuration by remember { mutableStateOf("") }

    // Entries panel (over the contest panel)
    var entriesFor by remember { mutableStateOf<String?>(null) }
    var entriesResult by remember { mutableStateOf<EntriesResult?>(null) }
    var entriesLoading by remember { mutableStateOf(false) }
    var entriesKey by remember { mutableIntStateOf(0) }
    var entriesQuery by remember { mutableStateOf("") }
    var flagsOpenId by remember { mutableStateOf<String?>(null) }

    // Confirmations
    var startTarget by remember { mutableStateOf<CompetitionItem?>(null) }
    var cancelTarget by remember { mutableStateOf<CompetitionItem?>(null) }
    var dqTarget by remember { mutableStateOf<EntryItem?>(null) }
    var dqReason by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        result = CompetitionsRepository.load()
        nowMs = System.currentTimeMillis()
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(4500L)
            toast = ""
        }
    }
    LaunchedEffect(entriesFor, entriesKey) {
        val wanted = entriesFor
        if (wanted == null) {
            entriesResult = null
        } else {
            entriesLoading = true
            val loaded = CompetitionsRepository.loadEntries(wanted)
            if (entriesFor == wanted) entriesResult = loaded
            entriesLoading = false
        }
    }

    val current = result
    val anyBusy = busyKey != null

    fun runAction(key: String, okMessage: String, call: suspend () -> String?) {
        scope.launch {
            busyKey = key
            val error = call()
            busyKey = null
            toast = if (error == null) okMessage else error
            reloadKey += 1
            entriesKey += 1
        }
    }

    fun createError(): String? {
        if (resourceId.isEmpty()) return "Please choose an exam from the exam hub."
        val iso = CompetitionsRepository.eatToIso(startDate, startTime)
        val startMs = if (iso == null) null else CompetitionsRepository.instantMs(iso)
        if (startMs == null || startMs <= System.currentTimeMillis() + 60_000L) {
            return "Pick a start time at least one minute from now (Ethiopia time)."
        }
        val d = durationText.trim()
        if (d.isNotEmpty() && (!Regex("^\\d{1,3}$").matches(d) || d.toInt() < 1)) {
            return "Duration must be a whole number of minutes."
        }
        val p = pointsText.trim().ifEmpty { "0" }
        if (!Regex("^\\d{1,6}$").matches(p)) return "Points must be a whole number (0 or more)."
        return null
    }

    fun doCreate() {
        val iso = CompetitionsRepository.eatToIso(startDate, startTime)
        if (iso == null) return
        val d = durationText.trim()
        val duration: Int? = if (d.isEmpty()) null else d.toInt()
        val points = pointsText.trim().ifEmpty { "0" }.toInt()
        val publish = publishNow
        val exam = resourceId
        val titleValue = title
        val audienceValue = audience
        val prizeTitleValue = prizeTitle
        val prizeDetailsValue = prizeDetails
        val packageOnly = requiresPackage
        scope.launch {
            busyKey = "create"
            val error = CompetitionsRepository.create(
                resourceId = exam,
                startsIso = iso,
                title = titleValue,
                audience = audienceValue,
                durationMin = duration,
                points = points,
                prizeTitle = prizeTitleValue,
                prizeDetails = prizeDetailsValue,
                requiresPackage = packageOnly,
                publish = publish
            )
            busyKey = null
            if (error == null) {
                toast = if (publish) {
                    "Competition created and visible to students."
                } else {
                    "Competition created as hidden. Use Show to publish it when ready."
                }
                resourceId = ""
                title = ""
                startDate = ""
                startTime = ""
                durationText = ""
                audience = ""
                pointsText = "0"
                prizeTitle = ""
                prizeDetails = ""
                requiresPackage = false
                publishNow = false
                pickerOpen = false
                createOpen = false
            } else {
                toast = error
            }
            reloadKey += 1
        }
    }

    fun submitCreate() {
        val problem = createError()
        if (problem != null) {
            toast = problem
        } else if (publishNow) {
            confirmCreate = true
        } else {
            doCreate()
        }
    }

    fun submitReschedule(c: CompetitionItem) {
        val iso = CompetitionsRepository.eatToIso(editDate, editTime)
        val startMs = if (iso == null) null else CompetitionsRepository.instantMs(iso)
        if (iso == null || startMs == null || startMs <= System.currentTimeMillis() + 60_000L) {
            toast = "Pick a new start at least one minute from now (Ethiopia time)."
            return
        }
        val d = editDuration.trim()
        if (d.isNotEmpty() && (!Regex("^\\d{1,3}$").matches(d) || d.toInt() < 1)) {
            toast = "Duration must be a whole number of minutes."
            return
        }
        val duration: Int? = if (d.isEmpty()) null else d.toInt()
        rescheduleId = null
        runAction("resched-" + c.id, "Rescheduled.") {
            CompetitionsRepository.reschedule(c.id, iso, duration)
        }
    }

    val startItem = startTarget
    if (startItem != null) {
        AdminConfirmDialog(
            title = "Start now",
            text = "Start this contest right now? Students can enter immediately.",
            confirmLabel = "Start now",
            confirmTone = WisdomAccentEmerald,
            onConfirm = {
                startTarget = null
                runAction("start-" + startItem.id, "Started now.") {
                    CompetitionsRepository.startNow(startItem.id)
                }
            },
            onDismiss = { startTarget = null }
        )
    }
    val cancelItem = cancelTarget
    if (cancelItem != null) {
        AdminConfirmDialog(
            title = "Cancel contest",
            text = "Cancel this contest? Students will see it as cancelled.",
            confirmLabel = "Yes, cancel it",
            confirmTone = AdminRose,
            onConfirm = {
                cancelTarget = null
                runAction("cancel-" + cancelItem.id, "Cancelled.") {
                    CompetitionsRepository.setCountdown(cancelItem.id, "cancelled")
                }
            },
            onDismiss = { cancelTarget = null }
        )
    }
    if (confirmCreate) {
        AdminConfirmDialog(
            title = "Show to students now",
            text = "This contest will be visible to students as soon as it is created. Continue?",
            confirmLabel = "Create and show",
            confirmTone = WisdomAccentAmber,
            onConfirm = {
                confirmCreate = false
                doCreate()
            },
            onDismiss = { confirmCreate = false }
        )
    }
    val dqItem = dqTarget
    if (dqItem != null) {
        AlertDialog(
            onDismissRequest = { dqTarget = null },
            containerColor = WisdomNavy,
            title = {
                Text(text = "Disqualify entry", color = WisdomTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Reason for disqualifying this entry (the student sees it, and the entry leaves the leaderboard):",
                        color = WisdomMuted,
                        fontSize = 14.sp
                    )
                    AdminTextField(
                        value = dqReason,
                        onValueChange = { dqReason = it },
                        placeholder = "Reason"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val reason = dqReason.trim()
                        if (reason.isEmpty()) {
                            toast = "Please type the reason."
                        } else {
                            dqTarget = null
                            dqReason = ""
                            runAction("dq-" + dqItem.id, "Entry disqualified.") {
                                CompetitionsRepository.disqualify(dqItem.id, reason)
                            }
                        }
                    }
                ) {
                    Text(text = "Disqualify", color = AdminRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dqTarget = null; dqReason = "" }) {
                    Text(text = "Cancel", color = WisdomMuted)
                }
            }
        )
    }

    val exams: List<ExamOption> = if (current is CompetitionsResult.Success) current.exams else emptyList()
    val examsFailed = current is CompetitionsResult.Success && current.examsFailed
    val chosenExam = exams.firstOrNull { it.id == resourceId }
    val pickerMatches = exams.filter {
        pickerQuery.isBlank() || examLabel(it).contains(pickerQuery.trim(), ignoreCase = true)
    }
    val allItems: List<CompetitionItem> = if (current is CompetitionsResult.Success) current.items else emptyList()
    val detailItem = allItems.firstOrNull { it.id == detailId }
    val rescheduleItem = allItems.firstOrNull { it.id == rescheduleId }
    val entriesItem = allItems.firstOrNull { it.id == entriesFor }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Global Timed Exams",
                            color = WisdomTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { reloadKey += 1 }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = WisdomMuted,
                                modifier = Modifier.alpha(if (loading) 0.4f else 1f)
                            )
                        }
                    }
                    Text(
                        text = "Create a contest from an exam of the exam hub, then show, pause, start, reschedule or cancel it. All times are Ethiopia time.",
                        color = WisdomMuted,
                        fontSize = 13.sp
                    )
                }
            }

            item(key = "create") {
                val shape = RoundedCornerShape(16.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(WisdomCard)
                        .border(1.dp, WisdomBorderWhite, shape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Create a Competition",
                        color = WisdomTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Questions, correct answers and solutions are copied from the chosen exam. Every question must have a solution. The contest is created hidden unless you choose to show it.",
                        color = WisdomMuted,
                        fontSize = 13.sp
                    )
                    AdminActionButton(
                        label = "New competition",
                        tone = WisdomAccentAmber,
                        filled = false,
                        enabled = true,
                        onClick = { createOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (current == null) {
                item(key = "loading") {
                    Text(
                        text = "Loading...",
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    )
                }
            } else if (current is CompetitionsResult.Failure) {
                item(key = "error") {
                    AdminMessageCard(
                        title = "Could not load the competitions",
                        body = current.message,
                        actionLabel = "Try again",
                        onAction = { reloadKey += 1 }
                    )
                }
            } else if (current is CompetitionsResult.Success) {
                item(key = "list-title") {
                    Text(
                        text = "All Competitions (" + current.items.size + ")",
                        color = WisdomTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (current.items.isEmpty()) {
                    item(key = "empty") {
                        AdminMessageCard(
                            title = "No competitions yet.",
                            body = "Contests you create appear here.",
                            actionLabel = null,
                            onAction = {}
                        )
                    }
                } else {
                    items(current.items, key = { it.id }) { c ->
                        CompetitionRow(
                            c = c,
                            nowMs = nowMs,
                            onClick = { detailId = c.id }
                        )
                    }
                }
            }
        }

        // B3a: the New competition panel. Drawn after the list, so it covers it; the list stays composed underneath.
        if (createOpen) {
            AdminDetailPanel(
                title = "New competition",
                subtitle = "Competition",
                onBack = { createOpen = false; pickerOpen = false },
                actions = {
                    AdminActionButton(
                        label = if (busyKey == "create") "Creating..." else "Create competition",
                        tone = WisdomAccentAmber,
                        filled = true,
                        enabled = !anyBusy,
                        onClick = { submitCreate() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            ) {
                Text(
                    text = "Questions, correct answers and solutions are copied from the chosen exam. Every question must have a solution. The contest is created hidden unless you tick Show to students now.",
                    color = WisdomMuted,
                    fontSize = 13.sp
                )

                CompFieldBlock("Exam from the exam hub (required)") {
                    CompPickerButton(
                        text = if (chosenExam == null) "Choose an exam..." else examLabel(chosenExam),
                        chosen = chosenExam != null,
                        onClick = { pickerOpen = true }
                    )
                }

                CompFieldBlock("Title (optional, default: the exam title)") {
                    AdminTextField(value = title, onValueChange = { title = it }, placeholder = "Title")
                }

                CompFieldBlock("Starts at, Ethiopia time (required)") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            placeholder = "Date 2026-10-25",
                            modifier = Modifier.weight(1f)
                        )
                        AdminTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            placeholder = "Time 14:30",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "Type the date as year-month-day and the time in 24-hour format.",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }

                CompFieldBlock("Duration in minutes (optional, default: the exam's own)") {
                    AdminTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        placeholder = "Minutes",
                        keyboardType = KeyboardType.Number
                    )
                }

                CompFieldBlock("Audience (optional, default: the exam's package)") {
                    AdminTextField(value = audience, onValueChange = { audience = it }, placeholder = "Audience")
                }

                CompFieldBlock("Points for the account total") {
                    AdminTextField(
                        value = pointsText,
                        onValueChange = { pointsText = it },
                        placeholder = "0",
                        keyboardType = KeyboardType.Number
                    )
                }

                CompFieldBlock("Prize title (shown only after the contest ends)") {
                    AdminTextField(value = prizeTitle, onValueChange = { prizeTitle = it }, placeholder = "Prize title")
                }

                CompFieldBlock("Prize details (shown only after the contest ends)") {
                    AdminTextArea(
                        value = prizeDetails,
                        onValueChange = { prizeDetails = it },
                        placeholder = "Prize details",
                        minHeight = 80.dp
                    )
                }

                CompToggleRow(
                    label = "Only students who own the exam's package may enter",
                    checked = requiresPackage,
                    onChange = { requiresPackage = it }
                )
                CompToggleRow(
                    label = "Show to students now",
                    checked = publishNow,
                    onChange = { publishNow = it }
                )
            }
        }

        // B3a: the exam picker, a second panel over the New competition panel. Drawn after it, so its Back closes it first.
        if (createOpen && pickerOpen) {
            AdminDetailPanel(
                title = "Choose an exam",
                subtitle = "New competition",
                onBack = { pickerOpen = false; pickerQuery = "" }
            ) {
                AdminSearchBox(
                    query = pickerQuery,
                    onQueryChange = { pickerQuery = it },
                    placeholder = "Search exams"
                )
                if (examsFailed) {
                    Text(
                        text = "The exam list could not be loaded. Tap Refresh and try again.",
                        color = AdminRose,
                        fontSize = 13.sp
                    )
                } else if (exams.isEmpty()) {
                    Text(
                        text = "No exams in the exam hub yet.",
                        color = WisdomMuted,
                        fontSize = 13.sp
                    )
                } else if (pickerMatches.isEmpty()) {
                    Text(
                        text = "No exam matches your search.",
                        color = WisdomMuted,
                        fontSize = 13.sp
                    )
                }
                for (exam in pickerMatches.take(30)) {
                    Text(
                        text = examLabel(exam),
                        color = if (exam.id == resourceId) WisdomAccentAmber else WisdomTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (exam.id == resourceId) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                resourceId = exam.id
                                pickerOpen = false
                                pickerQuery = ""
                            }
                            .padding(vertical = 10.dp)
                    )
                }
                if (pickerMatches.size > 30) {
                    Text(
                        text = "Showing the first 30. Type in the search box to narrow the list.",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // B3b: the contest panel. Details and all the actions of one contest.
        if (detailItem != null) {
            AdminDetailPanel(
                title = detailItem.title,
                subtitle = "Competition",
                onBack = { detailId = null }
            ) {
                CompetitionDetailBody(
                    c = detailItem,
                    nowMs = nowMs,
                    busy = anyBusy,
                    onTogglePublished = {
                        runAction(
                            "pub-" + detailItem.id,
                            if (detailItem.published) "Hidden from students." else "Now visible to students."
                        ) { CompetitionsRepository.setPublished(detailItem.id, !detailItem.published) }
                    },
                    onPause = {
                        runAction("pause-" + detailItem.id, "Countdown paused (students see it as postponed).") {
                            CompetitionsRepository.setCountdown(detailItem.id, "paused")
                        }
                    },
                    onResume = {
                        runAction("resume-" + detailItem.id, "Countdown running again.") {
                            CompetitionsRepository.setCountdown(detailItem.id, "running")
                        }
                    },
                    onStartNow = { startTarget = detailItem },
                    onCancel = { cancelTarget = detailItem },
                    onReschedule = {
                        editDate = ""
                        editTime = ""
                        editDuration = detailItem.durationMin.toString()
                        rescheduleId = detailItem.id
                    },
                    onEntries = {
                        entriesResult = null
                        flagsOpenId = null
                        entriesQuery = ""
                        entriesFor = detailItem.id
                    }
                )
            }
        }

        // B3c: reschedule, a small panel over the contest panel with Save fixed at the bottom.
        if (rescheduleItem != null) {
            AdminDetailPanel(
                title = rescheduleItem.title,
                subtitle = "Reschedule",
                onBack = { rescheduleId = null },
                actions = {
                    AdminActionButton(
                        label = "Save new time",
                        tone = WisdomAccentAmber,
                        filled = true,
                        enabled = !anyBusy,
                        onClick = { submitReschedule(rescheduleItem) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            ) {
                CompFieldBlock("New start, Ethiopia time") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminTextField(
                            value = editDate,
                            onValueChange = { editDate = it },
                            placeholder = "Date 2026-10-25",
                            modifier = Modifier.weight(1f)
                        )
                        AdminTextField(
                            value = editTime,
                            onValueChange = { editTime = it },
                            placeholder = "Time 14:30",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "Type the date as year-month-day and the time in 24-hour format.",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
                CompFieldBlock("Duration (minutes)") {
                    AdminTextField(
                        value = editDuration,
                        onValueChange = { editDuration = it },
                        placeholder = "Minutes",
                        keyboardType = KeyboardType.Number
                    )
                }
            }
        }

        // B3c: entries, a panel over the contest panel with a count and a search box. Flag details stay inside the entry.
        if (entriesFor != null) {
            AdminDetailPanel(
                title = "Entries",
                subtitle = entriesItem?.title ?: "Competition",
                onBack = { entriesFor = null; entriesQuery = "" }
            ) {
                EntriesBody(
                    result = entriesResult,
                    loading = entriesLoading,
                    query = entriesQuery,
                    onQuery = { entriesQuery = it },
                    flagsOpenId = flagsOpenId,
                    busy = anyBusy,
                    onToggleFlags = { id ->
                        flagsOpenId = if (flagsOpenId == id) null else id
                    },
                    onDisqualify = { entry ->
                        dqReason = ""
                        dqTarget = entry
                    }
                )
            }
        }

        if (toast.isNotEmpty()) {
            val barOpen = (createOpen && !pickerOpen) || rescheduleItem != null
            AdminToast(
                text = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (barOpen) 88.dp else 16.dp
                    )
            )
        }
    }
}

@Composable
private fun CompFieldLabel(text: String) {
    Text(
        text = text,
        color = WisdomMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

/** A label with its field(s) right under it, so the 12 dp gap of the panel falls between fields, not inside one. */
@Composable
private fun CompFieldBlock(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CompFieldLabel(label)
        content()
    }
}

@Composable
private fun CompPickerButton(text: String, chosen: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (chosen) WisdomTextPrimary else WisdomMuted,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** B3b: the compact list row of one contest: state and visibility badges, title, start and length, arrow. */
@Composable
private fun CompetitionRow(
    c: CompetitionItem,
    nowMs: Long,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val stateKey = compStateKey(c, nowMs)
    val startText = BroadcastsRepository.formatEat(c.startsAt) + " - " + c.durationMin + " min"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdminToneBadge(text = stateKey, tone = phaseTone(stateKey))
                AdminToneBadge(
                    text = if (c.published) "visible" else "hidden",
                    tone = if (c.published) WisdomAccentEmerald else WisdomMuted
                )
            }
            Text(
                text = c.title,
                color = WisdomTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = startText,
                color = WisdomMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = WisdomMuted
        )
    }
}

/** B3b: the content of the contest panel: badges, the details, and the actions two to a row. */
@Composable
private fun CompetitionDetailBody(
    c: CompetitionItem,
    nowMs: Long,
    busy: Boolean,
    onTogglePublished: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStartNow: () -> Unit,
    onCancel: () -> Unit,
    onReschedule: () -> Unit,
    onEntries: () -> Unit
) {
    val cancelled = c.countdownState == "cancelled"
    val paused = c.countdownState == "paused"
    val phase = CompetitionsRepository.phaseOf(c, nowMs)
    val upcoming = phase == "upcoming" && !cancelled
    val stateKey = compStateKey(c, nowMs)
    val endsAt = c.endsAt
    val prize = c.prizeTitle

    var whenText = BroadcastsRepository.formatEat(c.startsAt) + " - " + c.durationMin + " min"
    if (endsAt != null) whenText += " - ends " + BroadcastsRepository.formatEat(endsAt)
    whenText += " - " + c.points + (if (c.points == 1) " point" else " points")

    var audienceText = c.audience ?: "no audience set"
    if (c.requiresPackage) audienceText += " / package required"

    val actions = ArrayList<CompAction>()
    actions.add(CompAction(if (c.published) "Hide" else "Show", WisdomCyan, onTogglePublished))
    if (upcoming && !paused) actions.add(CompAction("Pause", WisdomAccentAmber, onPause))
    if (paused && !cancelled) actions.add(CompAction("Resume", WisdomAccentEmerald, onResume))
    if (upcoming) actions.add(CompAction("Start now", WisdomAccentEmerald, onStartNow))
    if (upcoming) actions.add(CompAction("Reschedule", WisdomCyan, onReschedule))
    if (!cancelled && phase != "ended") actions.add(CompAction("Cancel contest", AdminRose, onCancel))
    actions.add(CompAction("Entries", CompOrange, onEntries))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AdminToneBadge(text = stateKey, tone = phaseTone(stateKey))
        AdminToneBadge(
            text = if (c.published) "visible" else "hidden",
            tone = if (c.published) WisdomAccentEmerald else WisdomMuted
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = whenText, color = WisdomMuted, fontSize = 13.sp)
        Text(text = audienceText, color = WisdomMuted, fontSize = 13.sp)
        if (prize != null) {
            Text(text = "Prize: " + prize, color = WisdomMuted, fontSize = 13.sp)
        }
    }

    for (pair in actions.chunked(2)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (action in pair) {
                AdminActionButton(
                    label = action.label,
                    tone = action.tone,
                    filled = false,
                    enabled = !busy,
                    onClick = action.onClick,
                    modifier = Modifier.weight(1f)
                )
            }
            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** B3c: the content of the entries panel: a count, a search box, then the ranked entries. */
@Composable
private fun EntriesBody(
    result: EntriesResult?,
    loading: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    flagsOpenId: String?,
    busy: Boolean,
    onToggleFlags: (String) -> Unit,
    onDisqualify: (EntryItem) -> Unit
) {
    if (loading || result == null) {
        Text(text = "Loading entries...", color = WisdomMuted, fontSize = 14.sp)
    } else if (result is EntriesResult.Failure) {
        Text(text = result.message, color = AdminRose, fontSize = 14.sp)
    } else if (result is EntriesResult.Success) {
        val flagsError = result.flagsError
        if (result.entries.isEmpty()) {
            Text(text = "No ranked entries yet.", color = WisdomMuted, fontSize = 14.sp)
        } else {
            val matches = result.entries.filter {
                query.isBlank() || it.name.contains(query.trim(), ignoreCase = true)
            }
            Text(
                text = "Entries (" + result.entries.size + ")",
                color = WisdomTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            AdminSearchBox(
                query = query,
                onQueryChange = onQuery,
                placeholder = "Search entries by name"
            )
            if (flagsError != null) {
                Text(text = "Flags could not be loaded: " + flagsError, color = AdminRose, fontSize = 13.sp)
            }
            if (matches.isEmpty()) {
                Text(text = "No entry matches your search.", color = WisdomMuted, fontSize = 13.sp)
            }
            for (entry in matches) {
                EntryRow(
                    entry = entry,
                    flagsOpen = flagsOpenId == entry.id,
                    busy = busy,
                    onToggleFlags = { onToggleFlags(entry.id) },
                    onDisqualify = { onDisqualify(entry) }
                )
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: EntryItem,
    flagsOpen: Boolean,
    busy: Boolean,
    onToggleFlags: () -> Unit,
    onDisqualify: () -> Unit
) {
    val score = entry.score
    val total = entry.totalCount
    val taken = entry.timeTakenMs
    var line = entry.status
    if (score != null && total != null) line += " - " + score + " / " + total
    if (taken != null) line += " - " + CompetitionsRepository.formatTime(taken)
    val reason = entry.disqualifiedReason
    val summary = CompetitionsRepository.flagSummary(entry.flags)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WisdomBorderWhite)
        )
        Text(
            text = entry.name,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = line, color = WisdomMuted, fontSize = 12.sp)
        if (entry.disqualified) {
            Text(text = "Disqualified: " + (reason ?: ""), color = AdminRose, fontSize = 12.sp)
        }
        if (summary != null) {
            Text(
                text = summary + (if (flagsOpen) " (hide)" else " (details)"),
                color = WisdomAccentAmber,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(onClick = onToggleFlags)
                    .padding(vertical = 6.dp)
            )
            if (flagsOpen) {
                for (flag in entry.flags) {
                    Text(text = CompetitionsRepository.flagLine(flag), color = WisdomTextPrimary, fontSize = 12.sp)
                }
                Text(
                    text = "A flag is a signal to look at, not proof (a phone call can also send the app to the background). Nothing is disqualified automatically.",
                    color = WisdomMuted,
                    fontSize = 12.sp
                )
            }
        }
        if (!entry.disqualified) {
            AdminActionButton(
                label = "Disqualify",
                tone = AdminRose,
                filled = false,
                enabled = !busy,
                onClick = onDisqualify,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
