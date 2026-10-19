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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
// package, note), filter, and the list of grants with Revoke and Revoke all. The package list opens inline (no
// pop-up menu). Revoke, Revoke all and Grant all packages ask for confirmation first; the website used a browser
// confirm for the two revokes and none for grant-all.
// Texts copied from the website where it has them.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val RoseTone = Color(0xFFFB7185)

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
    var pickerOpen by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    var revokeTarget by remember { mutableStateOf<AccessGrantItem?>(null) }
    var revokeAllEmail by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        result = GrantsRepository.list()
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(4500L)
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
        scope.launch {
            busy = true
            val outcome = GrantsRepository.grant(email, packageId, note)
            busy = false
            toast = outcome.message
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
                        open = pickerOpen,
                        onClick = { pickerOpen = !pickerOpen }
                    )
                    if (pickerOpen) {
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
                GrantCard(
                    grant = grant,
                    busy = busy,
                    onRevoke = { revokeTarget = grant },
                    onRevokeAll = { revokeAllEmail = grant.email }
                )
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
                doRevokeAll(allRevoke)
            },
            onDismiss = { revokeAllEmail = null }
        )
    }
}

@Composable
private fun PickerHeader(label: String, open: Boolean, onClick: () -> Unit) {
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
            imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (open) "Close package list" else "Open package list",
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
            .height(44.dp)
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

@Composable
private fun GrantCard(
    grant: AccessGrantItem,
    busy: Boolean,
    onRevoke: () -> Unit,
    onRevokeAll: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val isAll = grant.packageId == ALL_PACKAGES_ID
    val details = buildString {
        if (!grant.note.isNullOrEmpty()) append(grant.note)
        if (!grant.grantedBy.isNullOrEmpty()) {
            if (isNotEmpty()) append(" \u00B7 ")
            append("by ").append(grant.grantedBy)
        }
        if (isNotEmpty()) append(" \u00B7 ")
        append(if (grant.userId != null) "linked account" else "email only (pre-signup)")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = grant.email,
            color = WisdomTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = grant.packageName ?: grant.packageId,
            color = if (isAll) WisdomAccentAmber else WisdomCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = details, color = WisdomMuted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminActionButton(
                label = "Revoke",
                tone = RoseTone,
                filled = false,
                enabled = !busy,
                onClick = onRevoke,
                modifier = Modifier.weight(1f)
            )
            AdminActionButton(
                label = "Revoke all",
                tone = WisdomMuted,
                filled = false,
                enabled = !busy,
                onClick = onRevokeAll,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
