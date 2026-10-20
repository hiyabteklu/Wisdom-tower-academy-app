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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.ADMIN_PACKAGES
import com.wisdomtower.academy.data.admin.ALL_PACKAGES_ID
import com.wisdomtower.academy.data.admin.AccessGrantItem
import com.wisdomtower.academy.data.admin.GrantsRepository
import com.wisdomtower.academy.data.admin.GrantsResult
import com.wisdomtower.academy.data.admin.LocksRepository
import com.wisdomtower.academy.data.admin.LocksResult
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ADMIN-native N6 (docs/ADMIN_NATIVE_PLAN.md). Native version of the website AccessGrantsPanel: grant form (e-mail,
// package, note), filter, and the list of grants with Revoke and Revoke all. Revoke, Revoke all and Grant all packages
// ask for confirmation first; the website used a browser confirm for the two revokes and none for grant-all.
// A3 (2026-10-20, docs/ADMIN_DASHBOARD_NEXT.md): after a grant the message says who now has access to which package and
// when the student sees it; a yellow note appears while Paid mode is OFF (then a grant changes nothing, everybody
// signed-in already opens everything); RoseTone is the kit's AdminRose (the fixed pink was too light by day).
// B1b (2026-10-20, docs/ADMIN_DASHBOARD_NEXT.md): nothing opens inline any more. The package picker is now a panel on top
// (AdminDetailPanel, AdminKit.kt) with the package list, and a grant row is a compact tappable row that opens a detail
// panel with the full facts and Revoke / Revoke all fixed at the bottom. The form and the list underneath stay composed
// (typed text and scroll position are kept); System Back closes the panel first. Grant, revoke and the confirm dialogs
// are unchanged. Texts copied from the website where it has them.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val RoseTone: Color get() = AdminRose

private const val PAID_MODE_KEY = "system:enforce"

@Composable
internal fun AccessGrantsScreen(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<GrantsResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var packageId by rememberSaveable { mutableStateOf(ALL_PACKAGES_ID) }
    var note by rememberSaveable { mutableStateOf("Beta access") }
    var filter by rememberSaveable { mutableStateOf("") }
    // B1b: the package picker panel and the grant detail panel (each drawn on top of the list; null / false = closed)
    var pickerOpen by remember { mutableStateOf(false) }
    var grantDetail by remember { mutableStateOf<AccessGrantItem?>(null) }
    var confirmAll by remember { mutableStateOf(false) }
    var revokeTarget by remember { mutableStateOf<AccessGrantItem?>(null) }
    var revokeAllEmail by remember { mutableStateOf<String?>(null) }
    // null = not known (could not be read); false = Paid mode OFF; true = ON
    var paidMode by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        result = GrantsRepository.list()
        val locksNow = LocksRepository.list()
        paidMode = if (locksNow is LocksResult.Success) {
            locksNow.items.firstOrNull { it.lockKey == PAID_MODE_KEY }?.mode == "locked"
        } else {
            null
        }
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(5500L)
            toast = ""
        }
    }

    val current = result
    val all: List<AccessGrantItem> = if (current is GrantsResult.Success) current.items else emptyList()
    val truncated = current is GrantsResult.Success && current.truncated

    val filtered = remember(all, filter) {
        val q = filter.trim().lowercase()
        if (q.isEmpty()) {
            all
        } else {
            all.filter { g ->
                g.email.lowercase().contains(q) ||
                    g.packageId.lowercase().contains(q) ||
                    (g.packageName ?: "").lowercase().contains(q) ||
                    (g.note ?: "").lowercase().contains(q)
            }
        }
    }

    val selectedLabel =
        if (packageId == ALL_PACKAGES_ID) {
            "All packages (full access)"
        } else {
            ADMIN_PACKAGES.firstOrNull { it.id == packageId }?.name ?: packageId
        }

    fun doGrant() {
        val mail = email.trim().lowercase()
        val label = selectedLabel
        scope.launch {
            busy = true
            val outcome = GrantsRepository.grant(email, packageId, note)
            busy = false
            toast = if (outcome.ok) {
                "$mail now has access to $label. The student sees it within about 30 seconds of opening the app."
            } else {
                outcome.message
            }
            if (outcome.ok) {
                email = ""
            }
            reloadKey += 1
        }
    }

    fun onGrantClick() {
        if (email.isBlank()) {
            toast = "Email is required"
            return
        }
        if (packageId == ALL_PACKAGES_ID) confirmAll = true else doGrant()
    }

    fun doRevoke(item: AccessGrantItem) {
        scope.launch {
            busy = true
            val outcome = GrantsRepository.revoke(item)
            busy = false
            toast = outcome.message
            reloadKey += 1
        }
    }

    fun doRevokeAll(mail: String) {
        scope.launch {
            busy = true
            val outcome = GrantsRepository.revokeAllForEmail(mail)
            busy = false
            toast = outcome.message
            reloadKey += 1
        }
    }

    val formShape = RoundedCornerShape(16.dp)

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
                            text = "Access grants",
                            color = WisdomTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Whitelist emails for beta users: unlock specific packages or the whole catalog " +
                                "without payment. Works even before they sign up (matched by email).",
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

            if (paidMode == false) {
                item(key = "paidOff") {
                    AdminMessageCard(
                        title = "Paid mode is OFF",
                        body = "While Paid mode is OFF, every signed-in student already opens everything, so a grant " +
                            "changes nothing yet. Turn Paid mode ON in the Locks screen when the locks are set.",
                        actionLabel = null,
                        onAction = {}
                    )
                }
            }

            item(key = "form") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(formShape)
                        .background(WisdomCard)
                        .border(1.dp, WisdomAccentAmber.copy(alpha = 0.3f), formShape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "GRANT ACCESS",
                        color = WisdomAccentAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Email", color = WisdomMuted, fontSize = 12.sp)
                    AdminTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "beta.user@example.com",
                        keyboardType = KeyboardType.Email
                    )
                    Text(text = "Package", color = WisdomMuted, fontSize = 12.sp)
                    PickerHeader(
                        label = selectedLabel,
                        onClick = { pickerOpen = true }
                    )
                    Text(text = "Note (optional)", color = WisdomMuted, fontSize = 12.sp)
                    AdminTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = "Beta tester \u00B7 March cohort"
                    )
                    AdminActionButton(
                        label = if (busy) "Saving..." else "Grant access",
                        tone = WisdomAccentEmerald,
                        filled = true,
                        enabled = !busy,
                        onClick = { onGrantClick() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "User does not need an account yet. When they sign in with this email, packages " +
                            "unlock automatically: no cart or payment.",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
            }

            item(key = "filter") {
                AdminSearchBox(
                    query = filter,
                    onQueryChange = { filter = it },
                    placeholder = "Filter by email or package..."
                )
            }

            if (current is GrantsResult.Success) {
                item(key = "count") {
                    val note2 = if (truncated) " (newest ${GrantsRepository.LIST_LIMIT} loaded)" else ""
                    Text(text = "${filtered.size} grant(s)$note2", color = WisdomMuted, fontSize = 12.sp)
                }
            }

            if (current == null) {
                item(key = "loading") {
                    Text(
                        text = "Loading grants...",
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else if (current is GrantsResult.Failure) {
                item(key = "error") {
                    AdminMessageCard(
                        title = "Could not load grants",
                        body = current.message,
                        actionLabel = "Try again",
                        onAction = { reloadKey += 1 }
                    )
                }
            } else if (filtered.isEmpty()) {
                item(key = "empty") {
                    AdminMessageCard(
                        title = "No access grants",
                        body = "No access grants yet. Add a beta email above.",
                        actionLabel = null,
                        onAction = {}
                    )
                }
            }

            items(filtered, key = { it.id }) { grant ->
                GrantRow(grant = grant, onClick = { grantDetail = grant })
            }
        }

        // B1b: the package picker panel, drawn over the list and the form (which stay composed, so typed text is kept).
        if (pickerOpen) {
            AdminDetailPanel(
                title = "Choose a package",
                subtitle = "Package",
                onBack = { pickerOpen = false }
            ) {
                PickRow(
                    label = "All packages (full access)",
                    selected = packageId == ALL_PACKAGES_ID,
                    tone = WisdomAccentAmber,
                    onClick = {
                        packageId = ALL_PACKAGES_ID
                        pickerOpen = false
                    }
                )
                for (pkg in ADMIN_PACKAGES) {
                    PickRow(
                        label = pkg.name,
                        selected = packageId == pkg.id,
                        tone = WisdomCyan,
                        onClick = {
                            packageId = pkg.id
                            pickerOpen = false
                        }
                    )
                }
            }
        }

        // B1b: the detail panel of one grant, with Revoke and Revoke all fixed at the bottom (both still ask first).
        val detailGrant = grantDetail
        if (detailGrant != null) {
            val isAllPackages = detailGrant.packageId == ALL_PACKAGES_ID
            AdminDetailPanel(
                title = detailGrant.email,
                subtitle = "Access grant",
                onBack = { grantDetail = null },
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminActionButton(
                            label = "Revoke",
                            tone = RoseTone,
                            filled = true,
                            enabled = !busy,
                            onClick = { revokeTarget = detailGrant },
                            modifier = Modifier.weight(1f)
                        )
                        AdminActionButton(
                            label = "Revoke all",
                            tone = WisdomMuted,
                            filled = false,
                            enabled = !busy,
                            onClick = { revokeAllEmail = detailGrant.email },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            ) {
                DetailLine(
                    label = "PACKAGE",
                    value = detailGrant.packageName ?: detailGrant.packageId,
                    valueTone = if (isAllPackages) WisdomAccentAmber else WisdomCyan
                )
                DetailLine(label = "PACKAGE ID", value = detailGrant.packageId, mono = true)
                if (!detailGrant.note.isNullOrEmpty()) {
                    DetailLine(label = "NOTE", value = detailGrant.note)
                }
                if (!detailGrant.grantedBy.isNullOrEmpty()) {
                    DetailLine(label = "GRANTED BY", value = detailGrant.grantedBy)
                }
                DetailLine(
                    label = "ACCOUNT",
                    value = if (detailGrant.userId != null) "Linked account" else "Email only (before sign-up)"
                )
                Text(
                    text = "Revoke removes this one package. Revoke all removes every grant for this email. " +
                        "Both ask you to confirm first.",
                    color = WisdomMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (toast.isNotEmpty()) {
            AdminToast(
                text = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = if (grantDetail != null) 88.dp else 16.dp)
            )
        }
    }

    if (confirmAll) {
        AdminConfirmDialog(
            title = "Grant full access?",
            text = "This unlocks all ${ADMIN_PACKAGES.size} packages for ${email.trim().lowercase()} and records the grant under your admin account.",
            confirmLabel = "Grant all packages",
            confirmTone = WisdomAccentEmerald,
            onConfirm = {
                confirmAll = false
                doGrant()
            },
            onDismiss = { confirmAll = false }
        )
    }

    val oneRevoke = revokeTarget
    if (oneRevoke != null) {
        AdminConfirmDialog(
            title = "Revoke access?",
            text = "Revoke access for ${oneRevoke.email} / ${oneRevoke.packageId}?",
            confirmLabel = "Revoke",
            confirmTone = RoseTone,
            onConfirm = {
                revokeTarget = null
                grantDetail = null
                doRevoke(oneRevoke)
            },
            onDismiss = { revokeTarget = null }
        )
    }

    val allRevoke = revokeAllEmail
    if (allRevoke != null) {
        AdminConfirmDialog(
            title = "Revoke all grants?",
            text = "Revoke ALL grants for $allRevoke?",
            confirmLabel = "Revoke all",
            confirmTone = RoseTone,
            onConfirm = {
                revokeAllEmail = null
                grantDetail = null
                doRevokeAll(allRevoke)
            },
            onDismiss = { revokeAllEmail = null }
        )
    }
}

/** The package field of the form. A tap opens the package panel (B1b); nothing expands inline. */
@Composable
private fun PickerHeader(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Choose a package",
            tint = WisdomMuted
        )
    }
}

@Composable
private fun PickRow(label: String, selected: Boolean, tone: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(if (selected) tone.copy(alpha = 0.15f) else WisdomCard)
            .border(1.dp, if (selected) tone.copy(alpha = 0.5f) else WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
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

/** B1b: one grant as a compact tappable row (email, package, arrow). A tap opens the detail panel. */
@Composable
private fun GrantRow(grant: AccessGrantItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val isAll = grant.packageId == ALL_PACKAGES_ID
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = grant.email,
                color = WisdomTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = grant.packageName ?: grant.packageId,
                color = if (isAll) WisdomAccentAmber else WisdomCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = WisdomMuted
        )
    }
}

/** A small label above a value, used in the grant detail panel. */
@Composable
private fun DetailLine(label: String, value: String, valueTone: Color = WisdomTextPrimary, mono: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, color = WisdomMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = valueTone,
            fontSize = 14.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        )
    }
}
