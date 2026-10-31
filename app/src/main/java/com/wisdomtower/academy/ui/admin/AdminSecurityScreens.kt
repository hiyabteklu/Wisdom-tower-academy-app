package com.wisdomtower.academy.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.AccountDeviceEvent
import com.wisdomtower.academy.data.admin.AdminSecurityRepository
import com.wisdomtower.academy.data.admin.ExamIntegrityAttempt
import com.wisdomtower.academy.data.admin.ExamIntegrityEvent
import com.wisdomtower.academy.data.admin.RegisteredAccountDevice
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch

private fun securityDuration(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val rest = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${rest}s"
        else -> "${rest}s"
    }
}

private fun securityDate(raw: String): String =
    raw.takeIf { it.isNotBlank() }?.replace("T", " ")?.take(16) ?: "Not recorded"

private fun securityEventLabel(kind: String): String = when (kind) {
    "app_background" -> "Left the exam"
    "app_foreground" -> "Returned to the exam"
    "copy_attempt" -> "Copy or cut attempt"
    "overlay_open" -> "Obscured overlay touch"
    "blocked_new_device" -> "Sign-in blocked on another device"
    "registered" -> "Device registered"
    "same_device_login" -> "Same-device sign-in"
    "admin_reset" -> "Admin reset"
    else -> kind.replace("_", " ").replaceFirstChar { it.uppercase() }
}

@Composable
internal fun ExamIntegrityAdminScreen(modifier: Modifier = Modifier) {
    var rows by remember { mutableStateOf<List<ExamIntegrityAttempt>?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewTarget by remember { mutableStateOf<ExamIntegrityAttempt?>(null) }
    var reviewNote by rememberSaveable { mutableStateOf("") }
    var busyId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        loading = true
        val result = AdminSecurityRepository.examIntegrity()
        rows = if (result.ok) result.rows else null
        error = result.error.orEmpty()
        loading = false
    }

    val all = rows.orEmpty()
    val pending = all.filter { it.reviewedAt.isBlank() }
    val totalLeaves = all.sumOf { it.leaveCount }
    val totalAway = all.sumOf { it.totalAwayMs }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Exam integrity", color = WisdomTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Ranked exam interruptions and review signals. Flags are not proof of cheating.", color = WisdomMuted, fontSize = 12.sp)
                }
                IconButton(onClick = { reload++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = WisdomMuted)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityMetric("Needs review", pending.size, WisdomAccentAmber, Modifier.weight(1f))
                SecurityMetric("Flagged students", pending.map { it.userId }.distinct().size, AdminRose, Modifier.weight(1f))
                SecurityMetric("Leaves", totalLeaves, WisdomCyan, Modifier.weight(1f))
            }
        }
        item {
            SecurityMetric("Total time away", null, WisdomCyan, Modifier.fillMaxWidth(), securityDuration(totalAway))
        }
        if (error.isNotBlank()) item { SecurityMessage(error, isError = true) }
        if (loading && rows == null) item { SecurityMessage("Loading exam activity…") }
        if (!loading && rows != null && all.isEmpty()) item {
            SecurityMessage("No ranked exam activity has been flagged yet.")
        }
        items(all, key = { it.entryId }) { row ->
            ExamAttemptCard(
                row = row,
                expanded = expandedId == row.entryId,
                busy = busyId == row.entryId,
                onExpand = { expandedId = if (expandedId == row.entryId) null else row.entryId },
                onReview = {
                    reviewTarget = row
                    reviewNote = ""
                }
            )
        }
    }

    val target = reviewTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { if (busyId == null) reviewTarget = null },
            title = { Text("Mark attempt reviewed?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This records that an administrator checked the activity. It does not label the student as guilty or change their score.")
                    OutlinedTextField(
                        value = reviewNote,
                        onValueChange = { reviewNote = it.take(1000) },
                        label = { Text("Review note (optional)") },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = busyId == null,
                    onClick = {
                        scope.launch {
                            busyId = target.entryId
                            val result = AdminSecurityRepository.reviewExam(target.entryId, reviewNote)
                            busyId = null
                            if (result.ok && result.body.trim() == "true") {
                                reviewTarget = null
                                reload++
                            } else {
                                error = if (result.ok) "The review was not saved." else result.errorText()
                            }
                        }
                    }
                ) { Text(if (busyId == target.entryId) "Saving…" else "Mark reviewed") }
            },
            dismissButton = { TextButton(onClick = { reviewTarget = null }, enabled = busyId == null) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ExamAttemptCard(
    row: ExamIntegrityAttempt,
    expanded: Boolean,
    busy: Boolean,
    onExpand: () -> Unit,
    onReview: () -> Unit
) {
    SecurityCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(row.studentName.ifBlank { "Student" }, color = WisdomTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(row.email.ifBlank { row.studentId }, color = WisdomMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row.examTitle, color = WisdomTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Started ${securityDate(row.startedAt)} · ${row.status}", color = WisdomMuted, fontSize = 11.sp)
            }
            AdminToneBadge(
                text = if (row.reviewedAt.isBlank()) "Needs review" else "Reviewed",
                tone = if (row.reviewedAt.isBlank()) WisdomAccentAmber else WisdomAccentEmerald
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecurityMiniStat("Leaves", row.leaveCount.toString(), Modifier.weight(1f))
            SecurityMiniStat("Time away", securityDuration(row.totalAwayMs), Modifier.weight(1f))
            SecurityMiniStat("Longest", securityDuration(row.longestAwayMs), Modifier.weight(1f))
            SecurityMiniStat("Flags", row.flagCount.toString(), Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExpand) { Text(if (expanded) "Hide timeline" else "Student & leave details") }
            Spacer(Modifier.weight(1f))
            if (row.reviewedAt.isBlank()) {
                Button(onClick = onReview, enabled = !busy) { Text(if (busy) "Saving…" else "Review") }
            }
        }
        if (expanded) {
            Text("LEAVE / RETURN TIMELINE", color = WisdomMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (row.events.isEmpty()) {
                Text("No event details were returned.", color = WisdomMuted, fontSize = 13.sp)
            } else {
                row.events.forEachIndexed { index, event ->
                    ExamTimelineEvent(event = event, nextReturn = row.events.drop(index + 1).firstOrNull { it.kind == "app_foreground" && event.kind == "app_background" })
                }
            }
            if (row.score != null) {
                Text("Score: ${row.score}/${row.totalCount ?: "?"}", color = WisdomTextPrimary, fontSize = 13.sp)
            }
            if (row.reviewedAt.isNotBlank()) {
                Text("Reviewed ${securityDate(row.reviewedAt)}", color = WisdomAccentEmerald, fontSize = 12.sp)
                if (row.reviewNote.isNotBlank()) Text(row.reviewNote, color = WisdomMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ExamTimelineEvent(event: ExamIntegrityEvent, nextReturn: ExamIntegrityEvent?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp).border(
            width = 0.dp, color = Color.Transparent, shape = RoundedCornerShape(1.dp)
        ).padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(securityEventLabel(event.kind), color = WisdomTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (event.kind == "app_background") {
                Text(
                    text = event.awayMs?.let { "Away ${securityDuration(it)}" } ?: "No return recorded",
                    color = WisdomAccentAmber,
                    fontSize = 11.sp
                )
            }
        }
        Text(securityDate(event.createdAt), color = WisdomMuted, fontSize = 11.sp)
        if (nextReturn != null) {
            Text("Returned ${securityDate(nextReturn.createdAt)}", color = WisdomCyan, fontSize = 11.sp)
        }
        if (event.detail.isNotBlank() && event.kind != "app_background" && event.kind != "app_foreground") {
            Text(event.detail.take(180), color = WisdomMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun DeviceSecurityAdminScreen(modifier: Modifier = Modifier) {
    var devices by remember { mutableStateOf<List<RegisteredAccountDevice>?>(null) }
    var events by remember { mutableStateOf<List<AccountDeviceEvent>?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var resetTarget by remember { mutableStateOf<RegisteredAccountDevice?>(null) }
    var reviewTarget by remember { mutableStateOf<AccountDeviceEvent?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        loading = true
        val d = AdminSecurityRepository.registeredDevices()
        val e = AdminSecurityRepository.deviceEvents()
        devices = if (d.ok) d.rows else null
        events = if (e.ok) e.rows else null
        error = listOfNotNull(d.error, e.error).joinToString(" · ")
        loading = false
    }

    val pending = events.orEmpty().filter { it.eventType == "blocked_new_device" && it.reviewedAt.isBlank() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Device security", color = WisdomTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("One registered installation per account. Reset only after verifying the student's request.", color = WisdomMuted, fontSize = 12.sp)
                }
                IconButton(onClick = { reload++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = WisdomMuted)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityMetric("Registered phones", devices?.size, WisdomCyan, Modifier.weight(1f))
                SecurityMetric("Blocked attempts", pending.size, AdminRose, Modifier.weight(1f))
                SecurityMetric("Students flagged", pending.map { it.userId }.distinct().size, WisdomAccentAmber, Modifier.weight(1f))
            }
        }
        if (error.isNotBlank()) item { SecurityMessage(error, isError = true) }
        if (loading && devices == null) item { SecurityMessage("Loading device records…") }
        item { Text("REGISTERED DEVICES", color = WisdomMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        if (devices != null && devices!!.isEmpty()) item { SecurityMessage("No student devices have registered through the updated Android app yet.") }
        items(devices.orEmpty(), key = { it.userId }) { device ->
            SecurityCard {
                Text(device.fullName.ifBlank { "Student" }, color = WisdomTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(device.email, color = WisdomMuted, fontSize = 12.sp)
                Text(device.deviceLabel.ifBlank { "Android device" }, color = WisdomCyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Registered ${securityDate(device.registeredAt)}", color = WisdomMuted, fontSize = 11.sp)
                Text("Last seen ${securityDate(device.lastSeenAt)}", color = WisdomMuted, fontSize = 11.sp)
                Button(onClick = { resetTarget = device }) { Text("Reset registered phone") }
            }
        }
        item { Text("BLOCKED SIGN-IN ATTEMPTS", color = WisdomMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        if (events != null && events!!.isEmpty()) item { SecurityMessage("No device activity has been recorded.") }
        items(events.orEmpty(), key = { it.eventId }) { event ->
            SecurityCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.fullName.ifBlank { "Student" }, color = WisdomTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    AdminToneBadge(
                        text = if (event.eventType == "blocked_new_device" && event.reviewedAt.isBlank()) "Review" else "Logged",
                        tone = if (event.eventType == "blocked_new_device" && event.reviewedAt.isBlank()) AdminRose else WisdomMuted
                    )
                }
                Text(event.email, color = WisdomMuted, fontSize = 12.sp)
                Text("${securityEventLabel(event.eventType)} · ${event.deviceLabel}", color = WisdomTextPrimary, fontSize = 13.sp)
                Text(securityDate(event.createdAt), color = WisdomMuted, fontSize = 11.sp)
                if (event.reviewedAt.isBlank() && event.eventType == "blocked_new_device") {
                    TextButton(onClick = { reviewTarget = event; note = "" }) { Text("Mark attempt reviewed") }
                } else if (event.reviewNote.isNotBlank()) {
                    Text("Review note: ${event.reviewNote}", color = WisdomMuted, fontSize = 12.sp)
                }
            }
        }
    }

    val reset = resetTarget
    if (reset != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) resetTarget = null },
            title = { Text("Reset this student's device?") },
            text = { Text("This removes the registered phone and revokes its session. The student can register a replacement phone on the next sign-in. Verify the student's identity before continuing.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        val result = AdminSecurityRepository.resetDevice(reset.userId)
                        busy = false
                        if (result.ok && result.body.trim() == "true") {
                            resetTarget = null
                            reload++
                        } else error = if (result.ok) "No active registered device was reset." else result.errorText()
                    }
                }) { Text(if (busy) "Resetting…" else "Reset device") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { resetTarget = null }) { Text("Cancel") } }
        )
    }

    val review = reviewTarget
    if (review != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) reviewTarget = null },
            title = { Text("Review blocked sign-in?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This records an administrative review of the attempt. It does not automatically prove account sharing.")
                    OutlinedTextField(value = note, onValueChange = { note = it.take(1000) }, label = { Text("Review note (optional)") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        val result = AdminSecurityRepository.reviewDeviceEvent(review.eventId, note)
                        busy = false
                        if (result.ok && result.body.trim() == "true") {
                            reviewTarget = null
                            reload++
                        } else error = if (result.ok) "The review was not saved." else result.errorText()
                    }
                }) { Text(if (busy) "Saving…" else "Mark reviewed") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { reviewTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SecurityMetric(label: String, value: Int?, tone: Color, modifier: Modifier = Modifier, detail: String? = null) {
    SecurityCard(modifier = modifier) {
        Text(value?.toString() ?: detail ?: "—", color = tone, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = WisdomMuted, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
private fun SecurityMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(WisdomCard).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, color = WisdomTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = WisdomMuted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun SecurityCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun SecurityMessage(message: String, isError: Boolean = false) {
    SecurityCard {
        Text(message, color = if (isError) AdminRose else WisdomMuted, fontSize = 13.sp)
    }
}
