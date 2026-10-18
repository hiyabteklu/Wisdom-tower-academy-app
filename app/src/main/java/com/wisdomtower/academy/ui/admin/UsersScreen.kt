package com.wisdomtower.academy.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
// Management): list with search and two filters, scholar cards, a History and Scores view and a Grant Package view.
// The two extra views replace the list inside this screen (no pop-up windows), each with its own back arrow and
// Back-button handling; the list scroll position is kept. Filters share the width, no horizontal scrolling.
// Differences on purpose: Grant all packages asks for confirmation first; the placeholder student number and the
// placeholder name of the website are NOT copied (a missing value shows Not set); enrollments and manual grants are
// listed under separate labels; no CSV export yet (needs a file save); no photo (initials only).
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

    // These two handlers are registered after the one in AdminConsoleHost, so they run first.
    BackHandler(enabled = grantUser != null) { if (!grantBusy) grantUser = null }
    BackHandler(enabled = inspectUser != null && grantUser == null) { inspectUser = null }

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
            if (outcome.ok) grantUser = null
        }
    }

    val grantTarget = grantUser
    val inspectTarget = inspectUser

    Box(modifier = modifier.fillMaxSize()) {
        if (grantTarget != null) {
            GrantView(
                scholar = grantTarget,
                busy = grantBusy,
                onBack = { if (!grantBusy) grantUser = null },
                onConfirm = { packageId, note -> doGrant(grantTarget, packageId, note) }
            )
        } else if (inspectTarget != null) {
            InspectView(
                scholar = inspectTarget,
                onBack = { inspectUser = null },
                onGrant = {
                    inspectUser = null
                    grantUser = inspectTarget
                }
            )
        } else {
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
                    ScholarCard(
                        scholar = scholar,
                        onInspect = { inspectUser = scholar },
                        onGrant = { grantUser = scholar },
                        onCopyId = { value -> copyText("Student ID", value) }
                    )
                }
            }
        }

        if (toast.isNotEmpty()) {
            AdminToast(
                text = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
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

@Composable
private fun ScholarCard(
    scholar: ScholarItem,
    onInspect: () -> Unit,
    onGrant: () -> Unit,
    onCopyId: (String) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val avatarShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(avatarShape)
                    .background(WisdomCyan.copy(alpha = 0.12f))
                    .border(1.dp, WisdomCyan.copy(alpha = 0.3f), avatarShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialsOf(scholar),
                    color = WisdomCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nameOf(scholar),
                    color = WisdomTextPrimary,
                    fontSize = 16.sp,
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
            }
        }
        val studentId = scholar.studentId
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
        }
        InfoPairRow(
            leftLabel = "School",
            leftValue = scholar.schoolName ?: "Not set",
            rightLabel = "Region",
            rightValue = scholar.townRegion ?: "Not set"
        )
        InfoPairRow(
            leftLabel = "Stream",
            leftValue = scholar.stream ?: "Not set",
            rightLabel = "Level",
            rightValue = levelOf(scholar)
        )
        Text(
            text = "Joined ${adminFormatDay(scholar.createdAt)}",
            color = WisdomMuted,
            fontSize = 12.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminActionButton(
                label = "History & Scores",
                tone = WisdomCyan,
                filled = false,
                enabled = true,
                onClick = onInspect,
                modifier = Modifier.weight(1.3f)
            )
            AdminActionButton(
                label = "Grant Package",
                tone = WisdomAccentAmber,
                filled = false,
                enabled = true,
                onClick = onGrant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ViewHeader(kicker: String, title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WisdomMuted
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kicker.uppercase(),
                color = WisdomAccentAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                color = WisdomTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
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

@Composable
private fun InspectView(
    scholar: ScholarItem,
    onBack: () -> Unit,
    onGrant: () -> Unit
) {
    var detail by remember(scholar.id) { mutableStateOf<StudentDetailResult?>(null) }
    var reloadKey by remember(scholar.id) { mutableIntStateOf(0) }

    LaunchedEffect(scholar.id, reloadKey) {
        detail = null
        detail = UsersRepository.detail(scholar.id, scholar.email)
    }

    val current = detail
    val summaryShape = RoundedCornerShape(16.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") {
            ViewHeader(kicker = "Student Scholar Profile", title = nameOf(scholar), onBack = onBack)
        }

        item(key = "summary") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(summaryShape)
                    .background(WisdomCard)
                    .border(1.dp, WisdomBorderWhite, summaryShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoPairRow(
                    leftLabel = "Student ID",
                    leftValue = scholar.studentId ?: "Not assigned",
                    rightLabel = "Level",
                    rightValue = levelOf(scholar)
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
                InfoPairRow(
                    leftLabel = "Target exam",
                    leftValue = scholar.targetExam ?: "-",
                    rightLabel = "Joined",
                    rightValue = adminFormatDay(scholar.createdAt)
                )
            }
        }

        if (current == null) {
            item(key = "loading") {
                Text(
                    text = "Fetching academic history & enrollments...",
                    color = WisdomMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        } else if (current is StudentDetailResult.Failure) {
            item(key = "error") {
                AdminMessageCard(
                    title = "Could not load history",
                    body = current.message,
                    actionLabel = "Try again",
                    onAction = { reloadKey += 1 }
                )
            }
        } else if (current is StudentDetailResult.Success) {
            val d = current.detail
            item(key = "unlockedHead") {
                SectionTitle(
                    text = "Unlocked Packages & Access Grants (${d.enrollments.size} enrolled, ${d.grants.size} manual)"
                )
            }
            if (d.enrollments.isEmpty() && d.grants.isEmpty()) {
                item(key = "unlockedNone") { EmptyLine(text = "No active packages unlocked yet.") }
            }
            items(d.enrollments) { label ->
                PackageLine(label = label, tag = "Enrolled", tone = WisdomAccentEmerald)
            }
            items(d.grants) { label ->
                PackageLine(label = label, tag = "Manual grant", tone = WisdomCyan)
            }
            item(key = "scoresHead") {
                SectionTitle(text = "Recent Test & Examination Scores (${d.scores.size})")
            }
            if (d.scores.isEmpty()) {
                item(key = "scoresNone") { EmptyLine(text = "No recorded quiz or test submissions yet.") }
            }
            items(d.scores) { score -> ScoreLine(score = score) }
        }

        item(key = "actions") {
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

@Composable
private fun GrantView(
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") {
            ViewHeader(
                kicker = "Direct Scholar Access Grant",
                title = "Unlock Materials for ${scholar.fullName ?: scholar.email ?: "this scholar"}",
                onBack = onBack
            )
        }
        item(key = "recipientLabel") { SectionTitle(text = "Recipient Scholar") }
        item(key = "recipient") {
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
        }
        item(key = "noteLabel") { SectionTitle(text = "Administrative Note") }
        item(key = "note") {
            AdminTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "e.g. Scholarship award, beta tester, payment verified"
            )
        }
        item(key = "packageLabel") { SectionTitle(text = "Package to Unlock") }
        item(key = "packageAll") {
            PackageChoice(
                label = "All Packages (Full Academy Access)",
                selected = selected == ALL_PACKAGES_ID,
                tone = WisdomAccentAmber,
                onClick = { selected = ALL_PACKAGES_ID }
            )
        }
        items(ADMIN_PACKAGES, key = { it.id }) { pkg ->
            PackageChoice(
                label = pkg.name,
                selected = selected == pkg.id,
                tone = WisdomCyan,
                onClick = { selected = pkg.id }
            )
        }
        item(key = "actions") {
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
