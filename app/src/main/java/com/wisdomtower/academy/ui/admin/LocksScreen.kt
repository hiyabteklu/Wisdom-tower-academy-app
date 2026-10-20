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
import com.wisdomtower.academy.data.admin.HubItemsResult
import com.wisdomtower.academy.data.admin.LockItem
import com.wisdomtower.academy.data.admin.LocksRepository
import com.wisdomtower.academy.data.admin.LocksResult
import com.wisdomtower.academy.data.admin.chapterLockKey
import com.wisdomtower.academy.data.admin.hubLockKey
import com.wisdomtower.academy.data.admin.itemLockKey
import com.wisdomtower.academy.data.admin.packageLockKey
import com.wisdomtower.academy.data.admin.scopeLockKey
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ADMIN-native N6b (docs/ADMIN_NATIVE_PLAN.md). Native version of the website LocksPanel (Content locks): package
// shortcuts, then a step-by-step tree (Academy > package > subject > hub) with the four lock modes for the
// selected step, and the list of all active overrides at the top level. Each mode is a full-width row with its hint
// (no tiny buttons). System Back and the arrow go up one step. Texts copied from the website.
// D7j (docs/DAY_MODE_PLAN.md, 2026-10-19): RoseTone is now the kit's AdminRose (night FB7185 as before, day BE123C; the
// literal pink was too light as the lock icon on ivory), and the shortcut block tint is WisdomInk at 4 percent (night:
// white 4 percent as before; day: a faint navy tint, white 4 percent was invisible on ivory). Night is identical.
// GRANULAR LOCKS (2026-10-20, owner order): inside a hub the screen now lists its chapters and its materials (read from
// learning_resources, published or not) and every chapter / material gets the same four lock modes. Keys:
// chapter:<scope>:<hub>:<n> and item:<resource id> (see LocksRepository). The student app reads them live, no release.
// ACCESS CONTROL (2026-10-20, owner order, docs/SESSION_2026-10-20_ACCESS_CONTROL_DB.md): the database now enforces the
// locks itself. Mode names are shown in plain words: open = Free for signed-in, require_purchase = Owners only,
// coming_soon / locked = closed for everybody. New PAID MODE card at the top (row system:enforce, locked = ON,
// open = OFF); all packages have a shortcut. Locked material stays visible in the student lists, with a lock.
// A3 (2026-10-20, docs/ADMIN_DASHBOARD_NEXT.md): plain wording. Every mode hint says WHO is affected; the open step shows a
// "what happens now" line (and warns when Owners only is set but Paid mode is OFF, because then nothing is enforced);
// "Reset to code default" is now "Remove this lock". Layout and behaviour unchanged.
// B1a (2026-10-20, docs/ADMIN_DASHBOARD_NEXT.md): package shortcuts, chapters and materials no longer expand below the
// tapped row. A tap opens AdminDetailPanel (AdminKit.kt) on top of the list: Back arrow, the four modes starting at the
// top, "Remove this lock" fixed at the bottom. The list stays underneath and keeps its scroll position; System Back closes
// the panel first. The top step card (active) is unchanged. Lock rules and saving are unchanged.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private class LockMode(val id: String, val label: String, val hint: String)

/** What the open detail panel edits: the lock key, the name shown in the panel header, and a small kind label above it. */
private class LockTarget(val key: String, val label: String, val kind: String)

private val MODES: List<LockMode> = listOf(
    LockMode(
        "open",
        "Free for signed-in",
        "Every signed-in student can open it. Signed-out visitors are asked to sign in."
    ),
    LockMode(
        "require_purchase",
        "Owners only",
        "Students who bought the package or were granted it can open it. Everyone else sees Package required. Needs Paid mode ON."
    ),
    LockMode(
        "coming_soon",
        "Coming soon",
        "Everybody sees it with a lock and Coming soon, owners too. Use it for material that is not ready."
    ),
    LockMode(
        "locked",
        "Hard locked",
        "Closed for everybody, owners too (same look as Coming soon). Use it to switch something off at once."
    )
)

private const val PAID_KEY = "system:enforce"
private const val PAID_LABEL =
    "Paid mode switch: LOCKED = ON (Require-purchase content needs an enrollment or grant) / OPEN = OFF (free for every signed-in student)"

private val PAID_ON = LockMode("locked", "Paid mode ON", "Owners-only content is locked for students who do not own the package")
private val PAID_OFF = LockMode("open", "Paid mode OFF", "Everything is free for every signed-in student (nothing is locked)")

private val SHORTCUTS: List<Pair<String, String>> = listOf(
    "freshman" to "Freshman package",
    "ece-y3-sem-1" to "ECE Year 3 \u00B7 Semester 1",
    "ece-y3-sem-2" to "ECE Year 3 \u00B7 Semester 2",
    "grade-9" to "Grade 9 package",
    "grade-10" to "Grade 10 package",
    "grade-11" to "Grade 11 package",
    "grade-12" to "Grade 12 package",
    "remedial" to "Remedial package",
    "coc" to "COC package",
    "uat" to "UAT package",
    "gat" to "GAT package",
    "exit-exam" to "Exit Exam package"
)

private val RoseTone: Color get() = AdminRose

private fun modeTone(mode: String?): Color = when (mode) {
    "open" -> WisdomAccentEmerald
    "require_purchase" -> WisdomAccentAmber
    "coming_soon" -> WisdomCyan
    "locked" -> RoseTone
    else -> WisdomMuted
}

private fun modeText(mode: String?): String = when (mode) {
    null -> "default"
    "open" -> "free (signed-in)"
    "require_purchase" -> "owners only"
    "coming_soon" -> "coming soon"
    "locked" -> "hard locked"
    else -> mode.replace('_', ' ')
}

/** One plain sentence: what a student sees right now for this lock mode (A3). */
private fun effectText(mode: String?, paidOn: Boolean): String = when (mode) {
    null -> "No lock is set here, so the level above decides."
    "open" -> "Every signed-in student can open this. Signed-out visitors are asked to sign in."
    "require_purchase" ->
        if (paidOn) {
            "Only students who bought or were granted the package can open this. Everyone else sees Package required."
        } else {
            "Paid mode is OFF, so every signed-in student opens this for now. Turn Paid mode ON to enforce it."
        }
    "coming_soon" -> "Everybody, owners too, sees this with a lock and Coming soon."
    "locked" -> "Closed for everybody, owners too. Shown with the same lock as Coming soon."
    else -> ""
}

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
    // B1a: the package shortcut, chapter or material whose detail panel is open on top of the list (null = none)
    var detail by remember { mutableStateOf<LockTarget?>(null) }
    // GRANULAR LOCKS: the materials of the open hub and a retry counter
    var hubItems by remember { mutableStateOf<HubItemsResult?>(null) }
    var itemsReload by remember { mutableIntStateOf(0) }
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
    // The detail panel (AdminDetailPanel) registers its own handler later still, so System Back closes the panel before this runs.
    BackHandler(enabled = crumbs.isNotEmpty()) { crumbs = crumbs.dropLast(1) }

    val current = result
    val locks: List<LockItem> = if (current is LocksResult.Success) current.items else emptyList()
    val lockMap: Map<String, LockItem> = remember(locks) { locks.associateBy { it.lockKey } }
    val paidOnNow = lockMap[PAID_KEY]?.mode == "locked"

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

    // Load the chapters and materials of the hub that is open (only when the hub or the retry counter changes;
    // saving a lock does not reload them, so an open detail panel stays open).
    val hubScopeToLoad: String? = if (isHub) node?.scopePath else null
    val hubIdToLoad: String? = if (isHub) node?.id else null
    LaunchedEffect(hubScopeToLoad, hubIdToLoad, itemsReload) {
        hubItems = null
        detail = null
        if (hubScopeToLoad != null && hubIdToLoad != null) {
            hubItems = LocksRepository.listHubItems(hubScopeToLoad, hubIdToLoad)
        }
    }

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
                            text = "Lock or unlock packages, subjects, hubs, chapters, and single materials. " +
                                "The most specific lock wins. Locked material stays visible with a lock. " +
                                "Changes reach students within about 30 seconds, no app update needed.",
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
                item(key = "paidMode") {
                    val paidOn = lockMap[PAID_KEY]?.mode == "locked"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(WisdomCard)
                            .border(1.dp, WisdomBorderWhite, cardShape)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PAID MODE",
                                color = WisdomMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            AdminToneBadge(
                                text = if (paidOn) "ON" else "OFF",
                                tone = if (paidOn) WisdomAccentAmber else WisdomAccentEmerald
                            )
                        }
                        Text(
                            text = "Master switch. While it is OFF, every signed-in student opens everything, even locked packages. " +
                                "Turn it ON only after the locks below are set.",
                            color = WisdomMuted,
                            fontSize = 12.sp
                        )
                        ModeRow(
                            mode = PAID_ON,
                            selected = paidOn,
                            enabled = busyKey != PAID_KEY,
                            onClick = { applyMode(PAID_KEY, PAID_ON.id, PAID_LABEL) }
                        )
                        ModeRow(
                            mode = PAID_OFF,
                            selected = !paidOn,
                            enabled = busyKey != PAID_KEY,
                            onClick = { applyMode(PAID_KEY, PAID_OFF.id, PAID_LABEL) }
                        )
                    }
                }

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
                            LockRow(
                                title = entry.second,
                                lock = lockMap[key],
                                onClick = { detail = LockTarget(key, entry.second, "Package") }
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
                            Text(
                                text = effectText(activeLock?.mode, paidOnNow),
                                color = WisdomTextPrimary,
                                fontSize = 13.sp
                            )
                            LockControls(
                                lock = activeLock,
                                busy = busyKey == activeKey,
                                showReset = true,
                                onMode = { mode -> applyMode(activeKey, mode, node.label) },
                                onReset = { resetToDefault(activeKey) }
                            )
                            Text(
                                text = "Material lock beats chapter lock, chapter beats hub, hub beats subject, " +
                                    "subject beats package. Example: package = Owners only, Chapter 1 = Free for signed-in.",
                                color = WisdomMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (isHub) {
                    val hubScope = node?.scopePath
                    val hubId = node?.id
                    val loaded = hubItems
                    item(key = "itemsHead") {
                        Text(
                            text = "CHAPTERS AND MATERIALS",
                            color = WisdomMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    when {
                        hubScope == null || hubId == null -> Unit

                        loaded == null -> item(key = "itemsLoading") {
                            Text(
                                text = "Loading materials...",
                                color = WisdomMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            )
                        }

                        loaded is HubItemsResult.Failure -> item(key = "itemsError") {
                            AdminMessageCard(
                                title = "Could not load materials",
                                body = loaded.message,
                                actionLabel = "Try again",
                                onAction = { itemsReload += 1 }
                            )
                        }

                        loaded is HubItemsResult.Success && loaded.items.isEmpty() -> item(key = "itemsEmpty") {
                            AdminMessageCard(
                                title = "No materials in this hub yet",
                                body = "Add materials in the Content tab. They appear here and can be locked one by one.",
                                actionLabel = null,
                                onAction = {}
                            )
                        }

                        loaded is HubItemsResult.Success -> {
                            val groups = loaded.items.groupBy { it.chapter }
                            for ((chapter, rows) in groups) {
                                if (chapter != null) {
                                    val chapterKey = chapterLockKey(hubScope, hubId, chapter)
                                    item(key = "ch-$chapter") {
                                        LockRow(
                                            title = "Chapter $chapter \u00B7 all ${rows.size} " +
                                                if (rows.size == 1) "material" else "materials",
                                            lock = lockMap[chapterKey],
                                            onClick = { detail = LockTarget(chapterKey, "Chapter $chapter", "Chapter") }
                                        )
                                    }
                                } else {
                                    item(key = "ch-none") {
                                        Text(
                                            text = "NO CHAPTER NUMBER",
                                            color = WisdomMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                items(rows, key = { "it-" + it.id }) { row ->
                                    val rowKey = itemLockKey(row.id)
                                    Box(modifier = Modifier.padding(start = if (chapter != null) 14.dp else 0.dp)) {
                                        LockRow(
                                            title = row.title + if (row.published) "" else " \u00B7 unpublished",
                                            lock = lockMap[rowKey],
                                            onClick = { detail = LockTarget(rowKey, row.title, "Material") }
                                        )
                                    }
                                }
                            }
                        }
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

        // B1a: the detail panel is drawn over the list. The list above stays composed, so its scroll position is kept.
        val target = detail
        if (target != null) {
            val targetLock: LockItem? = lockMap[target.key]
            val targetBusy = busyKey == target.key
            AdminDetailPanel(
                title = target.label,
                subtitle = target.kind,
                onBack = { detail = null },
                actions = {
                    AdminActionButton(
                        label = if (targetBusy) "Saving..." else "Remove this lock (the level above decides)",
                        tone = WisdomMuted,
                        filled = false,
                        enabled = !targetBusy && targetLock != null,
                        onClick = { resetToDefault(target.key) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = target.key,
                        color = WisdomMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AdminToneBadge(text = modeText(targetLock?.mode), tone = modeTone(targetLock?.mode))
                }
                Text(
                    text = effectText(targetLock?.mode, paidOnNow),
                    color = WisdomTextPrimary,
                    fontSize = 13.sp
                )
                LockControls(
                    lock = targetLock,
                    busy = targetBusy,
                    showReset = false,
                    onMode = { mode -> applyMode(target.key, mode, target.label) },
                    onReset = { resetToDefault(target.key) }
                )
                Text(
                    text = "Material lock beats chapter lock, chapter beats hub, hub beats subject, " +
                        "subject beats package. Example: package = Owners only, Chapter 1 = Free for signed-in.",
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
                    .padding(start = 16.dp, end = 16.dp, bottom = if (detail != null) 88.dp else 16.dp)
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

/** The four mode rows. showReset = false when the caller puts the Remove button somewhere else (the detail panel does). */
@Composable
private fun LockControls(
    lock: LockItem?,
    busy: Boolean,
    showReset: Boolean,
    onMode: (String) -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (mode in MODES) {
            ModeRow(
                mode = mode,
                selected = lock?.mode == mode.id,
                enabled = !busy,
                onClick = { onMode(mode.id) }
            )
        }
        if (showReset) {
            AdminActionButton(
                label = if (busy) "Saving..." else "Remove this lock (the level above decides)",
                tone = WisdomMuted,
                filled = false,
                enabled = !busy && lock != null,
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** B1a: one tappable row (name, current mode badge, arrow). A tap opens the detail panel; nothing expands below it. */
@Composable
private fun LockRow(title: String, lock: LockItem?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(WisdomInk.copy(alpha = 0.04f))
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
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
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = WisdomMuted
        )
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
    val isPaid = lock.lockKey == PAID_KEY
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
        AdminToneBadge(
            text = if (isPaid) (if (lock.mode == "locked") "paid mode ON" else "paid mode OFF") else modeText(lock.mode),
            tone = modeTone(lock.mode)
        )
    }
}
