package com.wisdomtower.academy.ui.admin

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.OrderItem
import com.wisdomtower.academy.data.admin.OrdersRepository
import com.wisdomtower.academy.data.admin.OrdersResult
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ADMIN-native N3 (docs/ADMIN_NATIVE_PLAN.md). Native version of the website PaymentsPanel: totals, search, status and
// payment-method filters, order list, receipt, approve and reject. Texts copied from the website where it has them.
// Differences on purpose: approve asks for confirmation (one mistaken tap on a phone would unlock content and send an
// SMS), the receipt opens in the phone's viewer (image or PDF), no CSV export yet (needs a file save).
// One LazyColumn, filters share the width, no horizontal scrolling.
// B3e (2026-10-20, docs/ADMIN_DASHBOARD_NEXT.md): each order is a compact row (package, student, amount, date, status,
// arrow). A tap opens the order panel (AdminDetailPanel, subtitle Order) with all the facts and View Receipt; for a
// pending order Approve and Reject are the bar fixed at the bottom. Money related: LAYOUT CHANGE ONLY. doApprove,
// doReject, both confirm dialogs and every OrdersRepository call are unchanged. The panel finds the order by id in the
// reloaded list. After a successful approve or reject the panel closes (the order leaves the Pending list) and the toast
// shows the result; after an error the panel stays open. The list stays composed underneath, so the scroll position, the
// search text and the chips survive.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val STATUS_FILTERS: List<Pair<String, String>> = listOf(
    "pending_verification" to "Pending",
    "verified" to "Verified",
    "rejected" to "Rejected",
    "all" to "All"
)

private val METHOD_FILTERS: List<Pair<String, String>> = listOf(
    "all" to "Any method",
    "telebirr" to "Telebirr",
    "cbe" to "CBE",
    "abyssinia" to "Abyssinia"
)

private fun etb(amount: Double): String {
    val f = NumberFormat.getNumberInstance(Locale.US)
    f.maximumFractionDigits = 3
    return f.format(amount) + " ETB"
}

/** Same wording as statusLabel in lib/orders.ts. */
private fun statusText(status: String): String = when (status) {
    "pending_payment" -> "Awaiting payment"
    "pending_verification" -> "Pending verification"
    "verified" -> "Verified \u00B7 enrolled"
    "rejected" -> "Needs attention"
    else -> status
}

private fun statusTone(status: String): Color = when (status) {
    "verified" -> WisdomAccentEmerald
    "pending_verification" -> WisdomAccentAmber
    "rejected" -> AdminRose
    else -> WisdomMuted
}

@Composable
internal fun PaymentsScreen(origin: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<OrdersResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("pending_verification") }
    var methodFilter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var approveTarget by remember { mutableStateOf<OrderItem?>(null) }
    var rejectTarget by remember { mutableStateOf<OrderItem?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        result = OrdersRepository.list()
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(4500L)
            toast = ""
        }
    }

    val current = result
    val all: List<OrderItem> = if (current is OrdersResult.Success) current.items else emptyList()
    val pending = all.filter { it.status == "pending_verification" }
    val verified = all.filter { it.status == "verified" }
    val pendingAmount = pending.sumOf { it.amountEtb }
    val verifiedAmount = verified.sumOf { it.amountEtb }
    val detailOrder = all.firstOrNull { it.id == detailId }

    val filtered = remember(all, statusFilter, methodFilter, query) {
        val q = query.trim().lowercase()
        all.filter { o ->
            if (statusFilter != "all" && o.status != statusFilter) return@filter false
            if (methodFilter != "all") {
                val method = (o.paymentMethod ?: "").lowercase()
                if (!method.contains(methodFilter)) return@filter false
            }
            if (q.isNotEmpty()) {
                val hit = o.id.lowercase().contains(q) ||
                    o.studentName.lowercase().contains(q) ||
                    o.phone.lowercase().contains(q) ||
                    (o.email ?: "").lowercase().contains(q) ||
                    o.transactionRef.lowercase().contains(q) ||
                    o.packageName.lowercase().contains(q)
                if (!hit) return@filter false
            }
            true
        }
    }

    fun doApprove(order: OrderItem) {
        scope.launch {
            busyId = order.id
            val outcome = OrdersRepository.verify(order)
            val error = outcome.error
            if (error != null) {
                busyId = null
                toast = error
            } else {
                val notice = OrdersRepository.notify(origin, order.id, true)
                busyId = null
                val warning = outcome.enrollWarning
                toast = if (warning != null) "$notice \u00B7 $warning" else notice
                detailId = null
                reloadKey += 1
            }
        }
    }

    fun doReject(order: OrderItem) {
        scope.launch {
            busyId = order.id
            val error = OrdersRepository.reject(order)
            if (error != null) {
                busyId = null
                toast = error
            } else {
                val notice = OrdersRepository.notify(origin, order.id, false)
                busyId = null
                toast = notice
                detailId = null
                reloadKey += 1
            }
        }
    }

    fun openReceipt(url: String) {
        try {
            val open = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(open)
        } catch (e: Exception) {
            toast = "No app can open this receipt"
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
                            text = "Payment Operations",
                            color = WisdomTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manual bank slip verification and student enrollment",
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

            if (current is OrdersResult.Success) {
                item(key = "totals") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TotalTile(
                                label = "Pending verification",
                                amount = etb(pendingAmount),
                                caption = "${pending.size} awaiting approval",
                                tone = WisdomAccentAmber,
                                modifier = Modifier.weight(1f)
                            )
                            TotalTile(
                                label = "Verified revenue",
                                amount = etb(verifiedAmount),
                                caption = "${verified.size} confirmed paid",
                                tone = WisdomAccentEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "${all.size} orders in total",
                            color = WisdomMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item(key = "search") {
                AdminSearchBox(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search order ID, student, phone, email, or transaction ref..."
                )
            }

            item(key = "statusChips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (entry in STATUS_FILTERS) {
                        val label = if (entry.first == "pending_verification" && current is OrdersResult.Success) {
                            "Pending (${pending.size})"
                        } else {
                            entry.second
                        }
                        AdminChoiceChip(
                            label = label,
                            selected = statusFilter == entry.first,
                            onClick = { statusFilter = entry.first },
                            modifier = Modifier.weight(if (entry.first == "pending_verification") 1.4f else 1f)
                        )
                    }
                }
            }

            item(key = "methodChips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (entry in METHOD_FILTERS) {
                        AdminChoiceChip(
                            label = entry.second,
                            selected = methodFilter == entry.first,
                            onClick = { methodFilter = entry.first },
                            modifier = Modifier.weight(if (entry.first == "all") 1.3f else 1f),
                            tone = WisdomCyan
                        )
                    }
                }
            }

            if (current == null) {
                item(key = "loading") {
                    Text(
                        text = "Loading orders...",
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    )
                }
            } else if (current is OrdersResult.Failure) {
                item(key = "error") {
                    PaymentsMessage(
                        title = "Could not load orders",
                        body = current.message,
                        actionLabel = "Try again",
                        onAction = { reloadKey += 1 }
                    )
                }
            } else if (filtered.isEmpty()) {
                item(key = "empty") {
                    PaymentsMessage(
                        title = "No orders found",
                        body = "No orders matching the selected status or search filter.",
                        actionLabel = null,
                        onAction = {}
                    )
                }
            }

            items(filtered, key = { it.id }) { order ->
                OrderRow(
                    order = order,
                    onClick = { detailId = order.id }
                )
            }
        }

        // B3e: the order panel. Drawn after the list, so it covers it; the list stays composed underneath.
        // A pending order gets Approve and Reject fixed at the bottom; any other order has no bottom bar.
        if (detailOrder != null) {
            if (detailOrder.status == "pending_verification") {
                AdminDetailPanel(
                    title = detailOrder.studentName,
                    subtitle = "Order",
                    onBack = { detailId = null },
                    actions = {
                        OrderActionButtons(
                            busy = busyId == detailOrder.id,
                            onApprove = { approveTarget = detailOrder },
                            onReject = { rejectTarget = detailOrder }
                        )
                    }
                ) {
                    OrderDetailBody(
                        order = detailOrder,
                        onReceipt = { url -> openReceipt(url) }
                    )
                }
            } else {
                AdminDetailPanel(
                    title = detailOrder.studentName,
                    subtitle = "Order",
                    onBack = { detailId = null }
                ) {
                    OrderDetailBody(
                        order = detailOrder,
                        onReceipt = { url -> openReceipt(url) }
                    )
                }
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
                        bottom = if (detailOrder != null && detailOrder.status == "pending_verification") 88.dp else 16.dp
                    )
            )
        }
    }

    val toApprove = approveTarget
    if (toApprove != null) {
        AdminConfirmDialog(
            title = "Approve order ${toApprove.id}?",
            text = "${toApprove.studentName} \u00B7 ${etb(toApprove.amountEtb)} \u00B7 ${toApprove.packageName}. " +
                "This verifies the payment, enrolls the student, and sends the approval e-mail and SMS if they are set up.",
            confirmLabel = "Approve & Unlock",
            confirmTone = WisdomAccentEmerald,
            onConfirm = {
                approveTarget = null
                doApprove(toApprove)
            },
            onDismiss = { approveTarget = null }
        )
    }

    val toReject = rejectTarget
    if (toReject != null) {
        AdminConfirmDialog(
            title = "Reject order ${toReject.id}?",
            text = "Student: ${toReject.studentName} (${etb(toReject.amountEtb)}). " +
                "Are you sure you want to reject this payment? The order will be flagged as rejected and the student will be notified if email or SMS is active.",
            confirmLabel = "Confirm Rejection",
            confirmTone = AdminRose,
            onConfirm = {
                rejectTarget = null
                doReject(toReject)
            },
            onDismiss = { rejectTarget = null }
        )
    }
}

@Composable
private fun TotalTile(
    label: String,
    amount: String,
    caption: String,
    tone: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(tone.copy(alpha = 0.10f))
            .border(1.dp, tone.copy(alpha = 0.25f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = tone,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = amount,
            color = WisdomTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = caption,
            color = WisdomMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** B3e: the compact list row of one order: package, student, amount, date, status badge, arrow. */
@Composable
private fun OrderRow(
    order: OrderItem,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val isPending = order.status == "pending_verification"
    val borderColor = if (isPending) WisdomAccentAmber.copy(alpha = 0.4f) else WisdomBorderWhite
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (order.status == "rejected") 0.85f else 1f)
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = order.packageName,
                color = WisdomTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = order.studentName,
                color = WisdomMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = etb(order.amountEtb),
                    color = WisdomAccentAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = adminFormatWhen(order.createdAt),
                    color = WisdomMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AdminToneBadge(text = statusText(order.status), tone = statusTone(order.status))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = WisdomMuted
        )
    }
}

/** B3e: the content of the order panel. Same facts and wording as the old order card, in the same order. */
@Composable
private fun OrderDetailBody(
    order: OrderItem,
    onReceipt: (String) -> Unit
) {
    val email = order.email
    val note = order.note
    val receiptUrl = order.receiptUrl
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdminToneBadge(text = statusText(order.status), tone = statusTone(order.status))
        Text(
            text = "Placed ${adminFormatWhen(order.createdAt)}",
            color = WisdomMuted,
            fontSize = 12.sp
        )
        Text(
            text = order.id,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
    Column {
        Text(
            text = order.packageName,
            color = WisdomTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = etb(order.amountEtb),
                color = WisdomAccentAmber,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "  via ${(order.paymentMethod ?: "Bank").uppercase()}",
                color = WisdomMuted,
                fontSize = 12.sp
            )
        }
    }
    Column {
        Text(
            text = order.studentName,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = order.phone, color = WisdomMuted, fontSize = 13.sp)
        if (email != null) {
            Text(
                text = email,
                color = WisdomMuted,
                fontSize = 13.sp
            )
        }
    }
    Column {
        Row {
            Text(text = "Ref: ", color = WisdomMuted, fontSize = 13.sp)
            Text(
                text = order.transactionRef,
                color = WisdomCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        if (note != null) {
            Text(
                text = "($note)",
                color = WisdomMuted,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
    if (receiptUrl != null) {
        AdminActionButton(
            label = "View Receipt",
            tone = WisdomCyan,
            filled = false,
            enabled = true,
            onClick = { onReceipt(receiptUrl) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** B3e: Approve and Reject, the bar fixed at the bottom of the order panel (pending orders only). */
@Composable
private fun OrderActionButtons(
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminActionButton(
            label = if (busy) "Working..." else "Approve & Unlock",
            tone = WisdomAccentEmerald,
            filled = true,
            enabled = !busy,
            onClick = onApprove,
            modifier = Modifier.weight(1.6f)
        )
        AdminActionButton(
            label = "Reject",
            tone = AdminRose,
            filled = false,
            enabled = !busy,
            onClick = onReject,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaymentsMessage(
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
            AdminActionButton(
                label = actionLabel,
                tone = WisdomCyan,
                filled = false,
                enabled = true,
                onClick = onAction
            )
        }
    }
}
