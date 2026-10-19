package com.wisdomtower.academy.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.BroadcastItem
import com.wisdomtower.academy.data.admin.BroadcastsRepository
import com.wisdomtower.academy.data.admin.HistoryResult
import com.wisdomtower.academy.data.admin.ScheduledItem
import com.wisdomtower.academy.data.admin.ScheduledResult
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ADMIN-native N8a (docs/ADMIN_NATIVE_PLAN.md). Native version of the READ and CANCEL half of the website Push
// Broadcasts panel (NotificationsPanel.tsx and ScheduledNotificationsPanel.tsx): the Scheduled list (cancel a waiting
// one) and the Broadcast History (delete one). Two chips switch between the two lists, one LazyColumn, no sideways
// scrolling. Writing and sending a new notification (composer v2) is NOT native yet (slice N8b): the card at the top
// opens the website composer in the host (AdminConsoleHost.kt) and Back returns here.
// Reuses the admin kit (AdminToneBadge, AdminChoiceChip, AdminActionButton, AdminConfirmDialog, AdminMessageCard,
// AdminToast). Texts for empty states and the cancel question are copied from the website panels.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val BroadcastOrange: Color = Color(0xFFFB923C)

private fun scheduledStatusTone(status: String): Color = when (status) {
    "pending" -> WisdomCyan
    "sending" -> WisdomAccentAmber
    "sent" -> WisdomAccentEmerald
    "failed" -> AdminRose
    else -> WisdomMuted
}

private fun historyTypeTone(type: String): Color = when (type) {
    "material" -> WisdomCyan
    "admin" -> WisdomAccentAmber
    "contest" -> BroadcastOrange
    "payment" -> WisdomAccentEmerald
    else -> WisdomMuted
}

@Composable
internal fun BroadcastsScreen(
    origin: String,
    onWriteOnWebsite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scheduled by remember { mutableStateOf<ScheduledResult?>(null) }
    var history by remember { mutableStateOf<HistoryResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf("scheduled") }
    var cancelTarget by remember { mutableStateOf<ScheduledItem?>(null) }
    var deleteTarget by remember { mutableStateOf<BroadcastItem?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        val scheduledJob = async { BroadcastsRepository.loadScheduled() }
        val historyJob = async { BroadcastsRepository.loadHistory(origin) }
        scheduled = scheduledJob.await()
        history = historyJob.await()
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(2500L)
            toast = ""
        }
    }

    val sched = scheduled
    val hist = history
    val scheduledCount: Int? = if (sched is ScheduledResult.Success) sched.items.size else null
    val historyCount: Int? = if (hist is HistoryResult.Success) hist.items.size else null

    fun cancelScheduled(item: ScheduledItem) {
        scope.launch {
            busyId = item.id
            val error = BroadcastsRepository.cancelScheduled(item.id)
            busyId = null
            toast = if (error == null) "Cancelled." else error
            reloadKey += 1
        }
    }

    fun deleteBroadcast(item: BroadcastItem) {
        scope.launch {
            busyId = item.id
            val error = BroadcastsRepository.deleteHistory(origin, item.id)
            busyId = null
            toast = if (error == null) "Deleted." else error
            reloadKey += 1
        }
    }

    val cancelItem = cancelTarget
    if (cancelItem != null) {
        AdminConfirmDialog(
            title = "Cancel scheduled notification",
            text = "Cancel this scheduled notification? It will not be sent.",
            confirmLabel = "Yes, cancel it",
            confirmTone = AdminRose,
            onConfirm = {
                cancelTarget = null
                cancelScheduled(cancelItem)
            },
            onDismiss = { cancelTarget = null }
        )
    }
    val deleteItem = deleteTarget
    if (deleteItem != null) {
        AdminConfirmDialog(
            title = "Delete notification",
            text = "Delete this notification from the history? This cannot be undone.",
            confirmLabel = "Delete",
            confirmTone = AdminRose,
            onConfirm = {
                deleteTarget = null
                deleteBroadcast(deleteItem)
            },
            onDismiss = { deleteTarget = null }
        )
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
                    Text(
                        text = "Push notifications",
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
            }

            item(key = "compose") {
                val shape = RoundedCornerShape(16.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(WisdomCard)
                        .border(1.dp, WisdomBorderWhite, shape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "New notification",
                        color = WisdomTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Writing, sending and scheduling still use the website composer. It opens inside the app; Back returns here.",
                        color = WisdomMuted,
                        fontSize = 13.sp
                    )
                    AdminActionButton(
                        label = "Open the composer",
                        tone = WisdomAccentAmber,
                        filled = false,
                        enabled = true,
                        onClick = onWriteOnWebsite,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item(key = "tabs") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminChoiceChip(
                        label = if (scheduledCount == null) "Scheduled" else "Scheduled ($scheduledCount)",
                        selected = tab == "scheduled",
                        onClick = { tab = "scheduled" },
                        modifier = Modifier.weight(1f)
                    )
                    AdminChoiceChip(
                        label = if (historyCount == null) "History" else "History ($historyCount)",
                        selected = tab == "history",
                        onClick = { tab = "history" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (tab == "scheduled") {
                if (sched == null) {
                    item(key = "sched-loading") { BroadcastsLoadingText("Loading...") }
                } else if (sched is ScheduledResult.Failure) {
                    item(key = "sched-error") {
                        AdminMessageCard(
                            title = "Could not load scheduled notifications",
                            body = sched.message,
                            actionLabel = "Try again",
                            onAction = { reloadKey += 1 }
                        )
                    }
                } else if (sched is ScheduledResult.Success) {
                    if (sched.items.isEmpty()) {
                        item(key = "sched-empty") {
                            AdminMessageCard(
                                title = "Nothing scheduled yet.",
                                body = "Notifications you schedule for later appear here.",
                                actionLabel = null,
                                onAction = {}
                            )
                        }
                    } else {
                        items(sched.items, key = { "s-" + it.id }) { row ->
                            ScheduledCard(
                                item = row,
                                busy = busyId == row.id,
                                onCancel = { cancelTarget = row }
                            )
                        }
                    }
                }
            } else {
                if (hist == null) {
                    item(key = "hist-loading") { BroadcastsLoadingText("Loading broadcasts...") }
                } else if (hist is HistoryResult.Failure) {
                    item(key = "hist-error") {
                        AdminMessageCard(
                            title = "Could not load the history",
                            body = hist.message,
                            actionLabel = "Try again",
                            onAction = { reloadKey += 1 }
                        )
                    }
                } else if (hist is HistoryResult.Success) {
                    if (hist.items.isEmpty()) {
                        item(key = "hist-empty") {
                            AdminMessageCard(
                                title = "No notifications broadcasted yet.",
                                body = "Sent notifications appear here.",
                                actionLabel = null,
                                onAction = {}
                            )
                        }
                    } else {
                        items(hist.items, key = { "h-" + it.id }) { row ->
                            HistoryCard(
                                item = row,
                                busy = busyId == row.id,
                                onDelete = { deleteTarget = row }
                            )
                        }
                    }
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

@Composable
private fun BroadcastsLoadingText(text: String) {
    Text(
        text = text,
        color = WisdomMuted,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
    )
}

@Composable
private fun ScheduledCard(item: ScheduledItem, busy: Boolean, onCancel: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val category = item.category
    val link = item.url
    val error = item.lastError
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdminToneBadge(text = item.status, tone = scheduledStatusTone(item.status))
            if (category != null) {
                AdminToneBadge(text = category, tone = WisdomAccentAmber)
            }
        }
        Text(
            text = item.title,
            color = WisdomTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = item.body,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = BroadcastsRepository.formatEat(item.sendAt),
            color = WisdomMuted,
            fontSize = 12.sp
        )
        val packageNote = if (item.packageId != null) " / package ${item.packageId}" else ""
        Text(
            text = BroadcastsRepository.audienceText(item) + packageNote,
            color = WisdomMuted,
            fontSize = 12.sp
        )
        if (link != null) {
            Text(text = "Link: $link", color = WisdomCyan, fontSize = 12.sp)
        }
        if (error != null) {
            Text(text = error, color = AdminRose, fontSize = 12.sp)
        }
        if (item.status == "pending") {
            AdminActionButton(
                label = "Cancel notification",
                tone = AdminRose,
                filled = false,
                enabled = !busy,
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HistoryCard(item: BroadcastItem, busy: Boolean, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val link = item.url
    val targetText = if (item.target == "all") "All users" else item.target
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminToneBadge(text = item.type, tone = historyTypeTone(item.type))
        Text(
            text = item.title,
            color = WisdomTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = item.body,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Target: $targetText",
            color = WisdomMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = adminFormatWhen(item.createdAt),
            color = WisdomMuted,
            fontSize = 12.sp
        )
        if (link != null) {
            Text(text = "Link: $link", color = WisdomCyan, fontSize = 12.sp)
        }
        AdminActionButton(
            label = "Delete",
            tone = AdminRose,
            filled = false,
            enabled = !busy,
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
