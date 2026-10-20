package com.wisdomtower.academy.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.ADMIN_PACKAGES
import com.wisdomtower.academy.data.admin.ALL_PACKAGES_ID
import com.wisdomtower.academy.data.admin.ScholarItem
import com.wisdomtower.academy.data.admin.ScholarsResult
import com.wisdomtower.academy.data.admin.StudentDetailResult
import com.wisdomtower.academy.data.admin.StudentScore
import com.wisdomtower.academy.data.admin.UsersRepository
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ADMIN-native N4 (docs/ADMIN_NATIVE_PLAN.md). Native version of the website UsersPanel (Scholar Directory and User
// Management): list with search and two filters, a History and Scores view and a Grant Package view.
// B3g (2026-10-20, docs/ADMIN_DASHBOARD_NEXT.md): LAYOUT ONLY. Each scholar is a compact row. A tap opens the profile as an
// AdminDetailPanel (details, unlocked packages, recent scores; Close and Grant New Package fixed at the bottom). Grant New
// Package opens a SECOND panel over the profile (recipient, note, package choices; Cancel and Confirm fixed at the bottom).
// The list stays composed under the panels, so search, filters and scroll position survive. The panels register their own
// Back handling, so this screen has none. Grant all packages still asks for confirmation first.
// Differences on purpose (from N4): the placeholder student number and the placeholder name of the website are NOT copied
// (a missing value shows Not set); enrollments and manual grants are listed under separate labels; no CSV export yet
// (needs a file save); no photo (initials only).
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val STREAM_FILTERS: List<Pair<String, String>> = listOf(
    "all" to "All streams",
    "natural" to "Natural",
    "social" to "Social"
)

private val LEVEL_FILTERS: List<Pair<String, String>> = listOf(
    "all" to "All levels",
    "grade-12" to "Grade 12",
    "grade-11" to "Grade 11",
    "freshman" to "Freshman",
    "remedial" to "Remedial"
)

@Composable
internal fun UsersScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<ScholarsResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var streamFilter by rememberSaveable { mutableStateOf("all") }
    var levelFilter by rememberSaveable { mutableStateOf("all") }
    var inspectUser by remember { mutableStateOf<ScholarItem?>(null) }
    var grantUser by remember { mutableStateOf<ScholarItem?>(null) }
    var grantBusy by remember { mutableStateOf(false) }
    var profileReload by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        result = UsersRepository.list()
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(4500L)
            toast = ""
        }
    }

    val current = result
    val all: List<ScholarItem> = if (current is ScholarsResult.Success) current.items else emptyList()
    val truncated = current is ScholarsResult.Success && current.truncated

    val filtered = remember(all, query, streamFilter, levelFilter) {
        val q = query.trim().lowercase()
        all.filter { u ->
            if (q.isNotEmpty()) {
                val hit = (u.fullName ?: "").lowercase().contains(q) ||
                    (u.email ?: "").lowercase().contains(q) ||
                    (u.studentId ?: "").lowercase().contains(q) ||
                    (u.schoolName ?: "").lowercase().contains(q) ||
                    (u.townRegion ?: "").lowercase().contains(q) ||
                    (u.phone ?: "").lowercase().contains(q)
                if (!hit) return@filter false
            }
            val streamValue = (u.stream ?: "").lowercase()
            if (streamFilter == "natural" && !streamValue.contains("nat")) return@filter false
            if (streamFilter == "social" && !streamValue.contains("soc")) return@filter false
            if (levelFilter != "all" && (u.educationLevel ?: "").lowercase() != levelFilter) return@filter false
            true
        }
    }

    fun copyText(label: String, value: String) {
        try {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText(label, value))
            toast = "Copied $value"
        } catch (e: Exception) {
            toast = "Could not copy"
        }
    }

    fun doGrant(user: ScholarItem, packageId: String, note: String) {
        scope.launch {
            grantBusy = true
            val outcome = UsersRepository.grant(user, packageId, note)
            grantBusy = false
            toast = outcome.message
            if (outcome.ok) {
                grantUser = null
                profileReload += 1
            }
        }
    }

    val grantTarget = grantUser
    val inspectTarget = inspectUser

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scholar Directory & User Management",
                            color = WisdomTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Registered students, school demographics, and direct access controls",
                            color = WisdomMuted,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { reloadKey += 1 }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = WisdomMuted,
                            modifier = Modifier.alpha(if (loading) 0.4f else 1f)
                        )
                    }
                }
            }

            item(key = "search") {
                AdminSearchBox(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search name, email, ID, school, town or phone..."
                )
            }

            item(key = "streamChips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (entry in STREAM_FILTERS) {
                        AdminChoiceChip(
                            label = entry.second,
                            selected = streamFilter == entry.first,
                            onClick = { streamFilter = entry.first },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "levelChipsA") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (entry in LEVEL_FILTERS.take(3)) {
                        AdminChoiceChip(
                            label = entry.second,
                            selected = levelFilter == entry.first,
                            onClick = { levelFilter = entry.first },
                            modifier = Modifier.weight(1f),
                            tone = WisdomCyan
                        )
                    }
                }
            }

            item(key = "levelChipsB") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (entry in LEVEL_FILTERS.drop(3)) {
                        AdminChoiceChip(
                            label = entry.second,
                            selected = levelFilter == entry.first,
                            onClick = { levelFilter = entry.first },
                            modifier = Modifier.weight(1f),
                            tone = WisdomCyan
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (current is ScholarsResult.Success) {
                item(key = "count") {
                    val note = if (truncated) " (newest ${UsersRepository.LIST_LIMIT} loaded)" else ""
                    Text(
                        text = "Showing ${filtered.size} of ${all.size}$note",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
            }

            if (current == null) {
                item(key = "loading") {
                    Text(
                        text = "Loading scholars directory...",
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    )
                }
            } else if (current is ScholarsResult.Failure) {
                item(key = "error") {
                    AdminMessageCard(
                        title = "Could not load scholars",
                        body = current.message,
                        actionLabel = "Try again",
                        onAction = { reloadKey += 1 }
                    )
                }
            } else if (filtered.isEmpty()) {
                item(key = "empty") {
                    AdminMessageCard(
                        title = "No matching scholars found",
                        body = "Try adjusting your search criteria or stream filters.",
                        actionLabel = null,
                        onAction = {}
                    )
                }
            }

            items(filtered, key = { it.id }) { scholar ->
                ScholarRow(scholar = scholar, onOpen = { inspectUser = scholar })
            }
        }

        // The profile panel is drawn after the list; the grant panel after the profile, so its Back handler runs first.
        if (inspectTarget != null) {
            ProfilePanel(
                scholar = inspectTarget,
                reloadKey = profileReload,
                onBack = { inspectUser = null },
                onGrant = { grantUser = inspectTarget },
                onCopyId = { value -> copyText("Student ID", value) }
            )
        }
        if (grantTarget != null) {
            GrantPanel(
                scholar = grantTarget,
                busy = grantBusy,
                onBack = { if (!grantBusy) grantUser = null },
                onConfirm = { packageId, note -> doGrant(grantTarget, packageId, note) }
            )
        }

        if (toast.isNotEmpty()) {
            AdminToast(
                text = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (inspectTarget != null || grantTarget != null) 88.dp else 16.dp
                    )
            )
        }
    }
}

private fun nameOf(scholar: ScholarItem): String = scholar.fullName ?: "Scholar"

private fun initialsOf(scholar: ScholarItem): String =
    (scholar.fullName ?: scholar.email ?: "S").take(2).uppercase()

private fun levelOf(scholar: ScholarItem): String =
    (scholar.educationLevel ?: "Not set").replace('_', ' ')

@Composable
private fun InfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    Column(modifier = modifier) {
        Text(text = label, color = WisdomMuted, fontSize = 12.sp, maxLines = 1)
        Text(
            text = value,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoPairRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCell(label = leftLabel, value = leftValue, modifier = Modifier.weight(1f), maxLines = maxLines)
        InfoCell(label = rightLabel, value = rightValue, modifier = Modifier.weight(1f), maxLines = maxLines)
    }
}

/** B3g: one short row per scholar (initials, name, e-mail, student id and level, arrow). A tap opens the profile panel. */
@Composable
private fun ScholarRow(scholar: ScholarItem, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val avatarShape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(avatarShape)
                .background(WisdomCyan.copy(alpha = 0.12f))
                .border(1.dp, WisdomCyan.copy(alpha = 0.3f), avatarShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsOf(scholar),
                color = WisdomCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nameOf(scholar),
                color = WisdomTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = scholar.email ?: "No email",
                color = WisdomMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${scholar.studentId ?: "No ID"} - ${levelOf(scholar)}",
                color = WisdomMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = WisdomMuted
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = WisdomMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmptyLine(text: String) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = text,
        color = WisdomMuted,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(12.dp)
    )
}

@Composable
private fun PackageLine(label: String, tag: String, tone: Color) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        AdminToneBadge(text = tag, tone = tone)
    }
}

@Composable
private fun ScoreLine(score: StudentScore) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = score.title,
                color = WisdomTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (score.createdAt.isNotEmpty()) {
                Text(text = adminFormatDay(score.createdAt), color = WisdomMuted, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${score.correct}/${score.total} (${score.percent}%)",
            color = WisdomCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** B3g: the History and Scores view as a panel. Close and Grant New Package are the bar fixed at the bottom. */
@Composable
private fun ProfilePanel(
    scholar: ScholarItem,
    reloadKey: Int,
    onBack: () -> Unit,
    onGrant: () -> Unit,
    onCopyId: (String) -> Unit
) {
    AdminDetailPanel(
        title = nameOf(scholar),
        onBack = onBack,
        subtitle = "Student Scholar Profile",
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminActionButton(
                    label = "Close",
                    tone = WisdomMuted,
                    filled = false,
                    enabled = true,
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
                AdminActionButton(
                    label = "Grant New Package",
                    tone = WisdomAccentEmerald,
                    filled = true,
                    enabled = true,
                    onClick = onGrant,
                    modifier = Modifier.weight(1.6f)
                )
            }
        }
    ) {
        ProfileBody(scholar = scholar, reloadKey = reloadKey, onCopyId = onCopyId)
    }
}

@Composable
private fun ProfileBody(
    scholar: ScholarItem,
    reloadKey: Int,
    onCopyId: (String) -> Unit
) {
    var detail by remember(scholar.id) { mutableStateOf<StudentDetailResult?>(null) }
    var retryKey by remember(scholar.id) { mutableIntStateOf(0) }

    LaunchedEffect(scholar.id, reloadKey, retryKey) {
        detail = null
        detail = UsersRepository.detail(scholar.id, scholar.email)
    }

    val current = detail
    val summaryShape = RoundedCornerShape(16.dp)
    val studentId = scholar.studentId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(summaryShape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, summaryShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (studentId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WisdomCyan.copy(alpha = 0.08f))
                    .clickable { onCopyId(studentId) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = studentId,
                    color = WisdomCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "Tap to copy", color = WisdomMuted, fontSize = 12.sp)
            }
        } else {
            InfoCell(label = "Student ID", value = "Not assigned")
        }
        InfoPairRow(
            leftLabel = "Level",
            leftValue = levelOf(scholar),
            rightLabel = "Joined",
            rightValue = adminFormatDay(scholar.createdAt)
        )
        InfoCell(label = "Email", value = scholar.email ?: "-", maxLines = 2)
        InfoPairRow(
            leftLabel = "Phone",
            leftValue = scholar.phone ?: "-",
            rightLabel = "School",
            rightValue = scholar.schoolName ?: "-",
            maxLines = 2
        )
        InfoPairRow(
            leftLabel = "Stream",
            leftValue = scholar.stream ?: "-",
            rightLabel = "Town / Region",
            rightValue = scholar.townRegion ?: "-",
            maxLines = 2
        )
        InfoCell(label = "Target exam", value = scholar.targetExam ?: "-", maxLines = 2)
    }

    if (current == null) {
        Text(
            text = "Fetching academic history & enrollments...",
            color = WisdomMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        )
    } else if (current is StudentDetailResult.Failure) {
        AdminMessageCard(
            title = "Could not load history",
            body = current.message,
            actionLabel = "Try again",
            onAction = { retryKey += 1 }
        )
    } else if (current is StudentDetailResult.Success) {
        val d = current.detail
        SectionTitle(
            text = "Unlocked Packages & Access Grants (${d.enrollments.size} enrolled, ${d.grants.size} manual)"
        )
        if (d.enrollments.isEmpty() && d.grants.isEmpty()) {
            EmptyLine(text = "No active packages unlocked yet.")
        }
        for (label in d.enrollments) {
            PackageLine(label = label, tag = "Enrolled", tone = WisdomAccentEmerald)
        }
        for (label in d.grants) {
            PackageLine(label = label, tag = "Manual grant", tone = WisdomCyan)
        }
        SectionTitle(text = "Recent Test & Examination Scores (${d.scores.size})")
        if (d.scores.isEmpty()) {
            EmptyLine(text = "No recorded quiz or test submissions yet.")
        }
        for (score in d.scores) {
            ScoreLine(score = score)
        }
    }
}

@Composable
private fun PackageChoice(label: String, selected: Boolean, tone: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(if (selected) tone.copy(alpha = 0.15f) else WisdomCard)
            .border(1.dp, if (selected) tone.copy(alpha = 0.5f) else WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) tone else WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = tone,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** B3g: the Grant Package view as a second panel over the profile. Cancel and Confirm are the bar fixed at the bottom. */
@Composable
private fun GrantPanel(
    scholar: ScholarItem,
    busy: Boolean,
    onBack: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(ALL_PACKAGES_ID) }
    var note by rememberSaveable { mutableStateOf("Direct scholar grant") }
    var confirmAll by remember { mutableStateOf(false) }
    val recipientShape = RoundedCornerShape(14.dp)
    val recipient = "${nameOf(scholar)} (${scholar.email ?: "No email"})"

    AdminDetailPanel(
        title = "Unlock Materials for ${scholar.fullName ?: scholar.email ?: "this scholar"}",
        onBack = onBack,
        subtitle = "Direct Scholar Access Grant",
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminActionButton(
                    label = "Cancel",
                    tone = WisdomMuted,
                    filled = false,
                    enabled = !busy,
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
                AdminActionButton(
                    label = if (busy) "Granting..." else "Confirm & Unlock Package",
                    tone = WisdomAccentEmerald,
                    filled = true,
                    enabled = !busy,
                    onClick = {
                        if (selected == ALL_PACKAGES_ID) confirmAll = true else onConfirm(selected, note)
                    },
                    modifier = Modifier.weight(1.8f)
                )
            }
        }
    ) {
        SectionTitle(text = "Recipient Scholar")
        Text(
            text = recipient,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(recipientShape)
                .background(WisdomCard)
                .border(1.dp, WisdomBorderWhite, recipientShape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
        SectionTitle(text = "Administrative Note")
        AdminTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = "e.g. Scholarship award, beta tester, payment verified"
        )
        SectionTitle(text = "Package to Unlock")
        PackageChoice(
            label = "All Packages (Full Academy Access)",
            selected = selected == ALL_PACKAGES_ID,
            tone = WisdomAccentAmber,
            onClick = { selected = ALL_PACKAGES_ID }
        )
        for (pkg in ADMIN_PACKAGES) {
            PackageChoice(
                label = pkg.name,
                selected = selected == pkg.id,
                tone = WisdomCyan,
                onClick = { selected = pkg.id }
            )
        }
    }

    if (confirmAll) {
        AdminConfirmDialog(
            title = "Grant full access?",
            text = "This unlocks all ${ADMIN_PACKAGES.size} packages for $recipient and records the grant under your admin account.",
            confirmLabel = "Grant all packages",
            confirmTone = WisdomAccentEmerald,
            onConfirm = {
                confirmAll = false
                onConfirm(ALL_PACKAGES_ID, note)
            },
            onDismiss = { confirmAll = false }
        )
    }
}
