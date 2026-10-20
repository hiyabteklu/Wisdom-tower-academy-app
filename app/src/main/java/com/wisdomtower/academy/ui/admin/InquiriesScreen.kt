package com.wisdomtower.academy.ui.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.InquiriesRepository
import com.wisdomtower.academy.data.admin.InquiriesResult
import com.wisdomtower.academy.data.admin.InquiryItem
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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ADMIN-native N2 (docs/ADMIN_NATIVE_PLAN.md). Native version of the website InquiriesPanel (Contact inquiries).
// Same data and actions: list the newest 200, change status (new, read, replied, archived). Texts for the empty state
// are copied from the website. Layout follows docs/ADMIN_UI_V2_PLAN.md section 3: dark cards, filter chips that all fit
// the width (no horizontal scrolling), one LazyColumn.
// B3d (2026-10-20, docs/ADMIN_DASHBOARD_NEXT.md): each inquiry is a compact row (name, e-mail, status, two lines of the
// message, date, arrow). A tap opens the inquiry panel (AdminDetailPanel, subtitle Inquiry): status and date, e-mail,
// interest, the whole message, and the four status buttons fixed at the bottom. Nothing expands inside the list any more.
// A search box under the filter chips narrows the list by name, e-mail, interest or message. The list stays composed
// underneath the panel, so its scroll position and the typed search survive. Repository calls, texts and the status
// logic are unchanged. Layout change only.
// This file also defines the small pieces the next admin slices reuse (AdminToast, AdminStatusBadge). Never delete them.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val FILTERS: List<Pair<String, String>> = listOf(
    "all" to "All",
    "new" to "New",
    "read" to "Read",
    "replied" to "Replied",
    "archived" to "Archived"
)

@Composable
internal fun InquiriesScreen(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<InquiriesResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        result = InquiriesRepository.list()
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(2500L)
            toast = ""
        }
    }

    val current = result
    val all: List<InquiryItem> = if (current is InquiriesResult.Success) current.items else emptyList()
    val newCount = all.count { it.status == "new" }
    val byStatus = if (filter == "all") all else all.filter { it.status == filter }
    val q = query.trim()
    val shown = if (q.isEmpty()) {
        byStatus
    } else {
        byStatus.filter {
            val service = it.service
            it.name.contains(q, ignoreCase = true) ||
                it.email.contains(q, ignoreCase = true) ||
                it.message.contains(q, ignoreCase = true) ||
                (service != null && service.contains(q, ignoreCase = true))
        }
    }
    val detailItem = all.firstOrNull { it.id == detailId }

    fun changeStatus(item: InquiryItem, status: String) {
        scope.launch {
            busyId = item.id
            val error = InquiriesRepository.setStatus(item.id, status)
            busyId = null
            if (error == null) {
                toast = "Marked $status"
                reloadKey += 1
            } else {
                toast = error
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
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
                            text = "Contact inquiries",
                            color = WisdomTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (current is InquiriesResult.Success) {
                            Text(
                                text = "$newCount new \u00B7 ${all.size} total",
                                color = WisdomMuted,
                                fontSize = 13.sp
                            )
                        }
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

            item(key = "filters") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (entry in FILTERS) {
                        FilterChip(
                            label = entry.second,
                            selected = filter == entry.first,
                            onClick = { filter = entry.first },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "search") {
                AdminSearchBox(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search by name, e-mail or message"
                )
            }

            if (current == null) {
                item(key = "loading") {
                    Text(
                        text = "Loading inquiries...",
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    )
                }
            } else if (current is InquiriesResult.Failure) {
                item(key = "error") {
                    MessageCard(
                        title = "Could not load inquiries",
                        body = current.message,
                        actionLabel = "Try again",
                        onAction = { reloadKey += 1 }
                    )
                }
            } else if (all.isEmpty()) {
                item(key = "empty") {
                    MessageCard(
                        title = "Inbox empty",
                        body = "Messages from the Contact page land in the inquiries table.",
                        actionLabel = null,
                        onAction = {}
                    )
                }
            } else if (shown.isEmpty()) {
                item(key = "nomatch") {
                    Text(
                        text = if (q.isEmpty()) "No messages with this status" else "No message matches your search",
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            }

            items(shown, key = { it.id }) { item ->
                InquiryRow(
                    item = item,
                    onClick = { detailId = item.id }
                )
            }
        }

        // B3d: the inquiry panel. Drawn after the list, so it covers it; the list stays composed underneath.
        if (detailItem != null) {
            AdminDetailPanel(
                title = detailItem.name,
                subtitle = "Inquiry",
                onBack = { detailId = null },
                actions = {
                    StatusButtons(
                        currentStatus = detailItem.status,
                        busy = busyId == detailItem.id,
                        onStatus = { status -> changeStatus(detailItem, status) }
                    )
                }
            ) {
                InquiryDetailBody(item = detailItem)
            }
        }

        if (toast.isNotEmpty()) {
            AdminToast(
                text = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (detailItem != null) 128.dp else 16.dp
                    )
            )
        }
    }
}

/** B3d: the compact list row of one inquiry: name, e-mail, status, two lines of the message, date, arrow. */
@Composable
private fun InquiryRow(
    item: InquiryItem,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = WisdomTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.email,
                        color = WisdomMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AdminStatusBadge(status = item.status)
            }
            Text(
                text = item.message,
                color = WisdomTextPrimary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatWhen(item.createdAt),
                color = WisdomMuted,
                fontSize = 12.sp
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

/** B3d: the content of the inquiry panel: status and date, e-mail, interest, then the whole message. */
@Composable
private fun InquiryDetailBody(item: InquiryItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminStatusBadge(status = item.status)
        Text(
            text = formatWhen(item.createdAt),
            color = WisdomMuted,
            fontSize = 12.sp
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = item.email,
            color = WisdomMuted,
            fontSize = 14.sp
        )
        if (item.service != null) {
            Text(
                text = "Interest: ${item.service}",
                color = WisdomCyan,
                fontSize = 13.sp
            )
        }
    }
    Text(
        text = item.message,
        color = WisdomTextPrimary,
        fontSize = 15.sp
    )
}

/** B3d: the four status buttons, two to a row. Used as the bar fixed at the bottom of the inquiry panel. */
@Composable
private fun StatusButtons(
    currentStatus: String,
    busy: Boolean,
    onStatus: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (pair in InquiriesRepository.STATUSES.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (status in pair) {
                    StatusButton(
                        label = status,
                        selected = currentStatus == status,
                        enabled = !busy && currentStatus != status,
                        onClick = { onStatus(status) },
                        modifier = Modifier.weight(1f)
                    )
                }
                for (i in pair.size until 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatusButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    val tone = if (selected) WisdomCyan else WisdomMuted
    Row(
        modifier = modifier
            .height(44.dp)
            .alpha(if (enabled || selected) 1f else 0.5f)
            .clip(shape)
            .background(if (selected) WisdomCyan.copy(alpha = 0.15f) else WisdomCard)
            .border(1.dp, if (selected) WisdomCyan.copy(alpha = 0.4f) else WisdomBorderWhite, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = tone,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) WisdomAccentAmber.copy(alpha = 0.15f) else WisdomCard)
            .border(1.dp, if (selected) WisdomAccentAmber.copy(alpha = 0.4f) else WisdomBorderWhite, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) WisdomAccentAmber else WisdomMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Inbox,
            contentDescription = null,
            tint = WisdomMuted,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = title,
            color = WisdomTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            color = WisdomMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            val pill = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(pill)
                    .background(WisdomCyan.copy(alpha = 0.15f))
                    .border(1.dp, WisdomCyan.copy(alpha = 0.4f), pill)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    color = WisdomCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Small status pill. Reused by the next admin slices. */
@Composable
internal fun AdminStatusBadge(status: String, modifier: Modifier = Modifier) {
    val tone = when (status) {
        "new" -> WisdomCyan
        "replied" -> WisdomAccentEmerald
        else -> WisdomMuted
    }
    val shape = RoundedCornerShape(50)
    Text(
        text = status.uppercase(),
        color = tone,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.4f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** One slim bar at the bottom for short results. Solid fill so it stays readable over a list. Reused later. */
@Composable
internal fun AdminToast(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        text = text,
        color = WisdomCyan,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomNavy)
            .border(1.dp, WisdomCyan.copy(alpha = 0.4f), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/** created_at from Supabase is UTC (+00:00). Shown in the phone's own time zone. Falls back to the raw text. */
private fun formatWhen(iso: String): String {
    if (iso.length < 19) return iso
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso.substring(0, 19))
        if (date == null) iso else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
    } catch (e: Exception) {
        iso.take(16).replace('T', ' ')
    }
}
