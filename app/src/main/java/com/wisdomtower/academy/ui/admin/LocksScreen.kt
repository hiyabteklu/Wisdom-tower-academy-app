package com.wisdomtower.academy.ui.admin

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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.ADMIN_CONTENT_TREE
import com.wisdomtower.academy.data.admin.ADMIN_HUB_IDS
import com.wisdomtower.academy.data.admin.AdminNavNode
import com.wisdomtower.academy.data.admin.LockItem
import com.wisdomtower.academy.data.admin.LocksRepository
import com.wisdomtower.academy.data.admin.LocksResult
import com.wisdomtower.academy.data.admin.hubLockKey
import com.wisdomtower.academy.data.admin.packageLockKey
import com.wisdomtower.academy.data.admin.scopeLockKey
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ADMIN-native N6b (docs/ADMIN_NATIVE_PLAN.md). Native version of the website LocksPanel (Content locks): three
// package shortcuts, then a step-by-step tree (Academy > package > subject > hub) with the four lock modes for the
// selected step, and the list of all active overrides at the top level. Each mode is a full-width row with its hint
// (no tiny buttons); shortcuts open on tap. System Back and the arrow go up one step. Texts copied from the website.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private class LockMode(val id: String, val label: String, val hint: String)

private val MODES: List<LockMode> = listOf(
    LockMode("open", "Open", "Anyone can access (no purchase gate)"),
    LockMode(
        "require_purchase",
        "Require sign-in / purchase",
        "Locked until user owns the package (or free-for-registered)"
    ),
    LockMode("coming_soon", "Coming soon", "Shown as uploading / not ready"),
    LockMode("locked", "Hard locked", "Fully closed (same UI as coming soon)")
)

private val SHORTCUTS: List<Pair<String, String>> = listOf(
    "freshman" to "Freshman package",
    "ece-y3-sem-1" to "ECE Year 3 \u00B7 Semester 1",
    "ece-y3-sem-2" to "ECE Year 3 \u00B7 Semester 2"
)

private val RoseTone = Color(0xFFFB7185)

private fun modeTone(mode: String?): Color = when (mode) {
    "open" -> WisdomAccentEmerald
    "require_purchase" -> WisdomAccentAmber
    "coming_soon" -> WisdomCyan
    "locked" -> RoseTone
    else -> WisdomMuted
}

private fun modeText(mode: String?): String = if (mode == null) "default" else mode.replace('_', ' ')

private fun keyFor(node: AdminNavNode): String? {
    val scope = node.scopePath
    val pkg = node.packageId
    return if (scope != null && ADMIN_HUB_IDS.contains(node.id)) {
        hubLockKey(scope, node.id)
    } else if (scope != null) {
        scopeLockKey(scope)
    } else if (pkg != null) {
        packageLockKey(pkg)
    } else {
        null
    }
}

@Composable
internal fun LocksScreen(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<LocksResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var busyKey by remember { mutableStateOf("") }
    var crumbs by remember { mutableStateOf<List<AdminNavNode>>(emptyList()) }
    var openShortcut by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        result = LocksRepository.list()
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(4000L)
            toast = ""
        }
    }
    LaunchedEffect(crumbs.size) { listState.scrollToItem(0) }

    // Registered after the one in AdminConsoleHost, so it runs first: up one step, and only at the top level the host takes over.
    BackHandler(enabled = crumbs.isNotEmpty()) { crumbs = crumbs.dropLast(1) }

    val current = result
    val locks: List<LockItem> = if (current is LocksResult.Success) current.items else emptyList()
    val lockMap: Map<String, LockItem> = remember(locks) { locks.associateBy { it.lockKey } }

    fun applyMode(lockKey: String, mode: String, label: String?) {
        scope.launch {
            busyKey = lockKey
            val outcome = LocksRepository.set(lockKey, mode, label)
            busyKey = ""
            toast = outcome.message
            if (outcome.ok) reloadKey += 1
        }
    }

    fun resetToDefault(lockKey: String) {
        scope.launch {
            busyKey = lockKey
            val outcome = LocksRepository.clear(lockKey)
            busyKey = ""
            toast = outcome.message
            if (outcome.ok) reloadKey += 1
        }
    }

    val node: AdminNavNode? = crumbs.lastOrNull()
    val listNodes: List<AdminNavNode> = if (node == null) ADMIN_CONTENT_TREE else node.children
    val isHub = node != null && node.scopePath != null && ADMIN_HUB_IDS.contains(node.id)
    val activeKey: String? = if (node == null) null else keyFor(node)
    val activeLock: LockItem? = if (activeKey == null) null else lockMap[activeKey]
    val pathText = (listOf("Academy") + crumbs.map { it.label }).joinToString(" \u203A ")
    val cardShape = RoundedCornerShape(16.dp)

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
                            text = "Content locks",
                            color = WisdomTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lock or unlock packages, subjects, and hubs (books, exams, etc.). " +
                                "Specific exams stay controlled by the Published checkbox in Content.",
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

            if (current is LocksResult.Failure) {
                item(key = "error") {
                    AdminMessageCard(
                        title = "Could not load locks",
                        body = current.message,
                        actionLabel = "Try again",
                        onAction = { reloadKey += 1 }
                    )
                }
            }

            if (current == null) {
                item(key = "loading") {
                    Text(
                        text = "Loading locks...",
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            }

            if (current is LocksResult.Success) {
                item(key = "shortcuts") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(WisdomCard)
                            .border(1.dp, WisdomBorderWhite, cardShape)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PACKAGE SHORTCUTS",
                            color = WisdomMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        for (entry in SHORTCUTS) {
                            val key = packageLockKey(entry.first)
                            val lock = lockMap[key]
                            ShortcutBlock(
                                title = entry.second,
                                lock = lock,
                                expanded = openShortcut == entry.first,
                                busy = busyKey == key,
                                onToggle = { openShortcut = if (openShortcut == entry.first) "" else entry.first },
                                onMode = { mode -> applyMode(key, mode, entry.second) },
                                onReset = { resetToDefault(key) }
                            )
                        }
                    }
                }

                item(key = "crumbs") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (crumbs.isNotEmpty()) {
                            IconButton(onClick = { crumbs = crumbs.dropLast(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Up one level",
                                    tint = WisdomMuted
                                )
                            }
                        }
                        Text(
                            text = pathText,
                            color = WisdomAccentAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (node != null && activeKey != null) {
                    item(key = "active") {
                        val activeShape = RoundedCornerShape(16.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(activeShape)
                                .background(WisdomCard)
                                .border(1.dp, WisdomAccentAmber.copy(alpha = 0.3f), activeShape)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (activeLock == null || activeLock.mode == "open") Icons.Filled.LockOpen else Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = if (activeLock == null || activeLock.mode == "open") WisdomAccentEmerald else RoseTone,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = node.label,
                                    color = WisdomTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AdminToneBadge(text = modeText(activeLock?.mode), tone = modeTone(activeLock?.mode))
                            }
                            Text(
                                text = activeKey,
                                color = WisdomMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            LockControls(
                                lock = activeLock,
                                busy = busyKey == activeKey,
                                onMode = { mode -> applyMode(activeKey, mode, node.label) },
                                onReset = { resetToDefault(activeKey) }
                            )
                            Text(
                                text = "Hub-level locks override subject locks; subject locks override package locks. " +
                                    "Individual exams: use Published in the Content tab.",
                                color = WisdomMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (isHub) {
                    item(key = "hubNote") {
                        AdminMessageCard(
                            title = "This is a hub",
                            body = "Use the buttons above to lock/unlock the whole hub. Open the Content tab to " +
                                "publish or unpublish individual exams and items.",
                            actionLabel = null,
                            onAction = {}
                        )
                    }
                } else {
                    items(listNodes, key = { it.id }) { child ->
                        val childKey = keyFor(child)
                        val childLock = if (childKey == null) null else lockMap[childKey]
                        NodeRow(node = child, lock = childLock, onClick = { crumbs = crumbs + child })
                    }
                }

                if (locks.isNotEmpty() && crumbs.isEmpty()) {
                    item(key = "overridesHead") {
                        Text(
                            text = "ALL ACTIVE OVERRIDES (${locks.size})",
                            color = WisdomMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(locks, key = { "ov-" + it.lockKey }) { lock ->
                        OverrideLine(lock = lock)
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
private fun ModeRow(mode: LockMode, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(if (selected) WisdomAccentAmber.copy(alpha = 0.15f) else WisdomCard)
            .border(
                1.dp,
                if (selected) WisdomAccentAmber.copy(alpha = 0.5f) else WisdomBorderWhite,
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = mode.label,
            color = if (selected) WisdomAccentAmber else WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = mode.hint, color = WisdomMuted, fontSize = 12.sp)
    }
}

@Composable
private fun LockControls(lock: LockItem?, busy: Boolean, onMode: (String) -> Unit, onReset: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (mode in MODES) {
            ModeRow(
                mode = mode,
                selected = lock?.mode == mode.id,
                enabled = !busy,
                onClick = { onMode(mode.id) }
            )
        }
        AdminActionButton(
            label = if (busy) "Saving..." else "Reset to code default",
            tone = WisdomMuted,
            filled = false,
            enabled = !busy && lock != null,
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ShortcutBlock(
    title: String,
    lock: LockItem?,
    expanded: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onMode: (String) -> Unit,
    onReset: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, WisdomBorderWhite, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = WisdomTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AdminToneBadge(text = modeText(lock?.mode), tone = modeTone(lock?.mode))
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Close" else "Open",
                tint = WisdomMuted
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                LockControls(lock = lock, busy = busy, onMode = onMode, onReset = onReset)
            }
        }
    }
}

@Composable
private fun NodeRow(node: AdminNavNode, lock: LockItem?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = WisdomAccentAmber,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = node.label,
            color = WisdomTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (lock != null) {
            Spacer(modifier = Modifier.width(8.dp))
            AdminToneBadge(text = modeText(lock.mode), tone = modeTone(lock.mode))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = WisdomMuted
        )
    }
}

@Composable
private fun OverrideLine(lock: LockItem) {
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
            text = lock.lockKey,
            color = WisdomCyan,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        AdminToneBadge(text = modeText(lock.mode), tone = modeTone(lock.mode))
    }
}
