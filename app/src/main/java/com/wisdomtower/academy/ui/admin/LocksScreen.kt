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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.ADMIN_CONTENT_TREE
import com.wisdomtower.academy.data.admin.ADMIN_HUB_IDS
import com.wisdomtower.academy.data.admin.AdminNavNode
import com.wisdomtower.academy.data.admin.GrantOutcome
import com.wisdomtower.academy.data.admin.HubItemsResult
import com.wisdomtower.academy.data.admin.LockItem
import com.wisdomtower.academy.data.admin.LockableItem
import com.wisdomtower.academy.data.admin.LocksBulk
import com.wisdomtower.academy.data.admin.LocksRepository
import com.wisdomtower.academy.data.admin.LocksResult
import com.wisdomtower.academy.data.admin.chapterLockKey
import com.wisdomtower.academy.data.admin.itemLockKey
import com.wisdomtower.academy.data.admin.packageLockKey
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

// S3 (2026-10-23, docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md, owner order): the admin Locks screen is now a clean TREE like the
// student navigation (Academy > package > subject / course > hub > chapters > items) with only THREE actions:
//   Owned only (require_purchase) / Free for signed-in (open) / Coming soon (coming_soon).
// The action of a PACKAGE is set on the package step; it removes every chapter and material lock of that package first
// ("apply to all", the admin confirms when something would be removed), then the admin goes down to a hub and sets single
// chapters or materials differently (trial unlocks). A chapter or material set to the same action as its package has its
// row removed (no useless rows). No subject-level or hub-level actions any more (scope: and hub: rows are not used by the
// Telegram build). "Add chapter" makes an empty chapter set to Coming soon (the student list shows it as a row).
// Removed from this screen on purpose: the PAID MODE switch (system:enforce, the Telegram build decides by the rows), the
// package shortcut block and the "all active overrides" list; the old Hard locked action (an old row with mode locked is
// shown as Coming soon). Saving and the keys are unchanged (LocksRepository): package:<id>, chapter:<scope>:<hub>:<n>,
// item:<resource id>. The student app reads them live, no release.
// STATUS: [~] written, NOT compiled, NOT tested on a device.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private class LockMode(val id: String, val label: String, val hint: String)

/** What the open detail panel edits: the lock key, the name shown in the panel header, and a small kind label above it. */
private class LockTarget(val key: String, val label: String, val kind: String)

/** A package action waiting for the admin's OK because it removes chapter / material settings. */
private class PendingApply(val pkgKey: String, val label: String, val mode: String, val keys: List<String>)

private val MODES: List<LockMode> = listOf(
    LockMode(
        "require_purchase",
        "Owned only",
        "Students who own the package can open it. Other students see the lock and the purchase screen."
    ),
    LockMode(
        "open",
        "Free for signed-in",
        "Every signed-in student can open it."
    ),
    LockMode(
        "coming_soon",
        "Coming soon",
        "Shown with a lock. A tap only says Coming soon, there is no purchase."
    )
)

private fun normMode(mode: String?): String? = if (mode == "locked") "coming_soon" else mode

private fun modeTone(mode: String?): Color = when (normMode(mode)) {
    "open" -> WisdomAccentEmerald
    "require_purchase" -> WisdomAccentAmber
    "coming_soon" -> WisdomCyan
    else -> WisdomMuted
}

private fun modeText(mode: String?): String = when (normMode(mode)) {
    null -> "Not set"
    "open" -> "Free"
    "require_purchase" -> "Owned only"
    "coming_soon" -> "Coming soon"
    else -> mode?.replace('_', ' ') ?: "Not set"
}

/** Coloured tag = set on this row. Grey tag = same as the package (no row of its own). */
private fun rowTag(own: LockItem?, pkg: LockItem?): Pair<String, Color> = when {
    own != null -> modeText(own.mode) to modeTone(own.mode)
    pkg != null -> modeText(pkg.mode) to WisdomMuted
    else -> "Not set" to WisdomMuted
}

private fun collectScopes(n: AdminNavNode, out: MutableSet<String>) {
    val s = n.scopePath
    if (s != null) out.add(s)
    for (c in n.children) collectScopes(c, out)
}

/** Scope prefix used to find the materials of a package: grade/9/, ece/sem-1/, freshman/, entrance/. */
private fun prefixOf(scope: String): String {
    val p = scope.split("/")
    return if ((p[0] == "grade" || p[0] == "ece") && p.size >= 2) p[0] + "/" + p[1] + "/" else p[0] + "/"
}

/**
 * The lock keys below one package: its chapter rows and its material rows. Null when the server could not be asked
 * (the caller stops, nothing is changed).
 */
private suspend fun exceptionKeysFor(root: AdminNavNode, locks: List<LockItem>): List<String>? {
    val scopes = HashSet<String>()
    collectScopes(root, scopes)
    val out = ArrayList<String>()
    for (l in locks) {
        if (l.lockKey.startsWith("chapter:")) {
            val parts = l.lockKey.split(":")
            if (parts.size >= 4 && scopes.contains(parts[1])) out.add(l.lockKey)
        }
    }
    val itemIds = locks.filter { it.lockKey.startsWith("item:") }.map { it.lockKey.removePrefix("item:") }
    if (itemIds.isNotEmpty()) {
        val prefixes = scopes.map { prefixOf(it) }.toSet()
        val found = HashSet<String>()
        for (prefix in prefixes) {
            val ids = LocksBulk.itemIdsInPrefix(itemIds, prefix) ?: return null
            found.addAll(ids)
        }
        for (id in found) out.add(itemLockKey(id))
    }
    return out
}

@Composable
internal fun LocksScreen(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<LocksResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var busyKey by remember { mutableStateOf("") }
    var crumbs by remember { mutableStateOf<List<AdminNavNode>>(emptyList()) }
    var detail by remember { mutableStateOf<LockTarget?>(null) }
    var pending by remember { mutableStateOf<PendingApply?>(null) }
    var hubItems by remember { mutableStateOf<HubItemsResult?>(null) }
    var itemsReload by remember { mutableIntStateOf(0) }
    var addChapter by remember { mutableStateOf(false) }
    var newNumber by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
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

    // Registered after the one in AdminConsoleHost, so it runs first: up one step. The detail panels register their own
    // handler later still, so System Back closes a panel before this runs.
    BackHandler(enabled = crumbs.isNotEmpty()) { crumbs = crumbs.dropLast(1) }

    val current = result
    val locks: List<LockItem> = if (current is LocksResult.Success) current.items else emptyList()
    val lockMap: Map<String, LockItem> = remember(locks) { locks.associateBy { it.lockKey } }

    val node: AdminNavNode? = crumbs.lastOrNull()
    val listNodes: List<AdminNavNode> = if (node == null) ADMIN_CONTENT_TREE else node.children
    val isHub = node != null && node.scopePath != null && ADMIN_HUB_IDS.contains(node.id)
    val pkgId: String? = node?.packageId
    val pkgRoot: AdminNavNode? = if (pkgId == null) null else crumbs.firstOrNull { it.packageId == pkgId }
    val pkgKey: String? = if (pkgId == null) null else packageLockKey(pkgId)
    val pkgLock: LockItem? = if (pkgKey == null) null else lockMap[pkgKey]
    val isPackageStep = node != null && pkgRoot === node
    val pathText = (listOf("Academy") + crumbs.map { it.label }).joinToString(" \u203A ")
    val cardShape = RoundedCornerShape(16.dp)

    val hubScopeToLoad: String? = if (isHub) node?.scopePath else null
    val hubIdToLoad: String? = if (isHub) node?.id else null
    LaunchedEffect(hubScopeToLoad, hubIdToLoad, itemsReload) {
        hubItems = null
        detail = null
        addChapter = false
        if (hubScopeToLoad != null && hubIdToLoad != null) {
            hubItems = LocksRepository.listHubItems(hubScopeToLoad, hubIdToLoad)
        }
    }

    fun finishRow(outcome: GrantOutcome) {
        busyKey = ""
        toast = outcome.message
        if (outcome.ok) reloadKey += 1
    }

    /** Chapter / material action. Same as the package = remove the row (no useless rows). */
    fun applyRow(key: String, label: String, mode: String) {
        scope.launch {
            busyKey = key
            val same = pkgLock != null && normMode(pkgLock.mode) == mode
            val outcome = if (same) {
                if (lockMap[key] != null) LocksRepository.clear(key) else GrantOutcome(true, "Same as the package")
            } else {
                LocksRepository.set(key, mode, label)
            }
            finishRow(outcome)
        }
    }

    fun removeRow(key: String) {
        scope.launch {
            busyKey = key
            finishRow(LocksRepository.clear(key))
        }
    }

    /** Package action: remove the chapter / material rows below it, then write the package row. */
    fun runPackageApply(p: PendingApply) {
        scope.launch {
            busyKey = p.pkgKey
            val cleared = LocksBulk.clearMany(p.keys)
            if (!cleared.ok) {
                busyKey = ""
                toast = cleared.message
                return@launch
            }
            val outcome = LocksRepository.set(p.pkgKey, p.mode, p.label)
            busyKey = ""
            toast = if (outcome.ok) {
                if (p.keys.isEmpty()) "Saved" else "Saved. ${p.keys.size} chapter and material settings removed."
            } else {
                outcome.message
            }
            reloadKey += 1
        }
    }

    fun requestPackageMode(root: AdminNavNode, mode: String) {
        val id = root.packageId ?: return
        val key = packageLockKey(id)
        scope.launch {
            busyKey = key
            val keys = exceptionKeysFor(root, locks)
            busyKey = ""
            if (keys == null) {
                toast = "Could not check the chapters and materials of this package. Nothing was changed. Try again."
                return@launch
            }
            val p = PendingApply(key, root.label, mode, keys)
            if (keys.isEmpty()) runPackageApply(p) else pending = p
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
                            text = "Open a package and choose what happens when a student taps it. Then go down to a hub and " +
                                "set single chapters or materials differently. Students always see everything; a lock only " +
                                "decides what a tap does. Coloured tag = set here. Grey tag = same as the package. " +
                                "Changes reach students within about 30 seconds.",
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

                if (node != null && isPackageStep && pkgKey != null) {
                    item(key = "package") {
                        val busy = busyKey == pkgKey
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(WisdomCard)
                                .border(1.dp, WisdomAccentAmber.copy(alpha = 0.3f), cardShape)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "THIS PACKAGE",
                                    color = WisdomMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                AdminToneBadge(text = modeText(pkgLock?.mode), tone = modeTone(pkgLock?.mode))
                            }
                            Text(
                                text = "Applies to every chapter and material of ${node.label}. Chapters and materials you set " +
                                    "separately below are replaced, you are asked first.",
                                color = WisdomMuted,
                                fontSize = 12.sp
                            )
                            for (mode in MODES) {
                                ModeRow(
                                    mode = mode,
                                    selected = normMode(pkgLock?.mode) == mode.id,
                                    enabled = !busy,
                                    onClick = { requestPackageMode(node, mode.id) }
                                )
                            }
                            if (busy) {
                                Text(text = "Saving...", color = WisdomMuted, fontSize = 12.sp)
                            }
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

                        loaded is HubItemsResult.Success -> {
                            val chPrefix = "chapter:$hubScope:$hubId:"
                            val byChapter: Map<Int?, List<LockableItem>> = loaded.items.groupBy { it.chapter }
                            val emptyChapters = lockMap.keys
                                .filter { it.startsWith(chPrefix) }
                                .mapNotNull { it.removePrefix(chPrefix).toIntOrNull() }
                                .filter { !byChapter.containsKey(it) }
                            val numbers = (byChapter.keys.filterNotNull() + emptyChapters).distinct().sorted()
                            item(key = "addChapter") {
                                AdminActionButton(
                                    label = "Add chapter (Coming soon)",
                                    tone = WisdomCyan,
                                    filled = false,
                                    enabled = true,
                                    onClick = {
                                        newNumber = ""
                                        newTitle = ""
                                        addChapter = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (numbers.isEmpty() && byChapter.isEmpty()) {
                                item(key = "itemsEmpty") {
                                    AdminMessageCard(
                                        title = "No materials in this hub yet",
                                        body = "Add materials in the Content tab, or add an empty chapter above.",
                                        actionLabel = null,
                                        onAction = {}
                                    )
                                }
                            }
                            for (n in numbers) {
                                val chapterKey = chapterLockKey(hubScope, hubId, n)
                                val rows = byChapter[n] ?: emptyList()
                                val tag = rowTag(lockMap[chapterKey], pkgLock)
                                item(key = "ch-$n") {
                                    LockRow(
                                        title = "Chapter $n \u00B7 " + when (rows.size) {
                                            0 -> "empty"
                                            1 -> "1 material"
                                            else -> "${rows.size} materials"
                                        },
                                        tagText = tag.first,
                                        tagTone = tag.second,
                                        onClick = { detail = LockTarget(chapterKey, "Chapter $n", "Chapter") }
                                    )
                                }
                                for (row in rows) {
                                    val rowKey = itemLockKey(row.id)
                                    val rowTagPair = rowTag(lockMap[rowKey], pkgLock)
                                    item(key = "it-" + row.id) {
                                        Box(modifier = Modifier.padding(start = 14.dp)) {
                                            LockRow(
                                                title = row.title + if (row.published) "" else " \u00B7 unpublished",
                                                tagText = rowTagPair.first,
                                                tagTone = rowTagPair.second,
                                                onClick = { detail = LockTarget(rowKey, row.title, "Material") }
                                            )
                                        }
                                    }
                                }
                            }
                            val loose = byChapter[null] ?: emptyList()
                            if (loose.isNotEmpty()) {
                                item(key = "ch-none") {
                                    Text(
                                        text = "NO CHAPTER NUMBER",
                                        color = WisdomMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                for (row in loose) {
                                    val rowKey = itemLockKey(row.id)
                                    val rowTagPair = rowTag(lockMap[rowKey], pkgLock)
                                    item(key = "it-" + row.id) {
                                        LockRow(
                                            title = row.title + if (row.published) "" else " \u00B7 unpublished",
                                            tagText = rowTagPair.first,
                                            tagTone = rowTagPair.second,
                                            onClick = { detail = LockTarget(rowKey, row.title, "Material") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    for (child in listNodes) {
                        val isRoot = child.packageId != null && child.packageId != pkgId
                        val childLock = if (isRoot) lockMap[packageLockKey(child.packageId ?: "")] else null
                        item(key = child.id) {
                            NodeRow(
                                node = child,
                                tag = if (isRoot) modeText(childLock?.mode) else null,
                                tagTone = modeTone(childLock?.mode),
                                onClick = { crumbs = crumbs + child }
                            )
                        }
                    }
                }
            }
        }

        // The chapter / material panel is drawn over the list. The list stays composed, so its scroll position is kept.
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
                        label = if (targetBusy) "Saving..." else "Same as the package",
                        tone = WisdomMuted,
                        filled = false,
                        enabled = !targetBusy && targetLock != null,
                        onClick = { removeRow(target.key) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (targetLock != null) "Set here" else "Same as the package",
                        color = WisdomMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    val tag = rowTag(targetLock, pkgLock)
                    AdminToneBadge(text = tag.first, tone = tag.second)
                }
                for (mode in MODES) {
                    ModeRow(
                        mode = mode,
                        selected = normMode(targetLock?.mode) == mode.id,
                        enabled = !targetBusy,
                        onClick = { applyRow(target.key, target.label, mode.id) }
                    )
                }
                Text(
                    text = "A chapter or material set the same as its package needs no setting of its own, so it goes back " +
                        "to Same as the package. Example: package = Owned only, Chapter 1 = Free for signed-in.",
                    color = WisdomMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Add an empty chapter (Coming soon). Only inside a hub.
        if (addChapter && isHub) {
            val hubScope = node?.scopePath
            val hubId = node?.id
            val number = newNumber.trim().toIntOrNull()
            val exists = hubItems.let { h ->
                h is HubItemsResult.Success && number != null && h.items.any { it.chapter == number }
            }
            val valid = number != null && number in 1..999 && hubScope != null && hubId != null
            val addKey = if (valid && hubScope != null && hubId != null && number != null) chapterLockKey(hubScope, hubId, number) else ""
            val busy = busyKey == addKey && addKey.isNotEmpty()
            AdminDetailPanel(
                title = "Add chapter",
                subtitle = "Coming soon",
                onBack = { addChapter = false },
                actions = {
                    AdminActionButton(
                        label = if (busy) "Saving..." else "Save as Coming soon",
                        tone = WisdomCyan,
                        filled = true,
                        enabled = valid && !busy,
                        onClick = {
                            if (exists) {
                                toast = "Chapter $number already has materials. Open it in the list instead."
                            } else if (valid && number != null && hubScope != null && hubId != null) {
                                val key = chapterLockKey(hubScope, hubId, number)
                                val label = newTitle.trim().ifEmpty { "Chapter $number" }
                                scope.launch {
                                    busyKey = key
                                    val outcome = LocksRepository.set(key, "coming_soon", label)
                                    busyKey = ""
                                    toast = outcome.message
                                    if (outcome.ok) {
                                        addChapter = false
                                        reloadKey += 1
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            ) {
                Text(
                    text = "Makes an empty chapter that students see as a row marked Coming soon. You can put materials " +
                        "in it later and change its action here.",
                    color = WisdomMuted,
                    fontSize = 12.sp
                )
                LockField(value = newNumber, onChange = { newNumber = it }, placeholder = "Chapter number, for example 4", numeric = true)
                LockField(value = newTitle, onChange = { newTitle = it }, placeholder = "Title (optional)", numeric = false)
            }
        }

        val ask = pending
        if (ask != null) {
            AdminConfirmDialog(
                title = "Apply to the whole package?",
                text = "${ask.label} will be set to ${modeText(ask.mode)}. This removes ${ask.keys.size} chapter and " +
                    "material setting" + (if (ask.keys.size == 1) "" else "s") + " inside it. You can set single chapters " +
                    "and materials again afterwards.",
                confirmLabel = "Apply",
                confirmTone = WisdomAccentAmber,
                onConfirm = {
                    pending = null
                    runPackageApply(ask)
                },
                onDismiss = { pending = null }
            )
        }

        if (toast.isNotEmpty()) {
            AdminToast(
                text = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = if (detail != null || addChapter) 88.dp else 16.dp)
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

/** One tappable row: name, action tag, arrow. A tap opens the panel for that chapter or material. */
@Composable
private fun LockRow(title: String, tagText: String, tagTone: Color, onClick: () -> Unit) {
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
        AdminToneBadge(text = tagText, tone = tagTone)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = WisdomMuted
        )
    }
}

/** A folder row of the tree. The tag is shown only on package steps (the first step that has its own package). */
@Composable
private fun NodeRow(node: AdminNavNode, tag: String?, tagTone: Color, onClick: () -> Unit) {
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
        if (tag != null) {
            Spacer(modifier = Modifier.width(8.dp))
            AdminToneBadge(text = tag, tone = tagTone)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = WisdomMuted
        )
    }
}

/** Small one-line text box in the look of the admin search box (no icon). */
@Composable
private fun LockField(value: String, onChange: (String) -> Unit, placeholder: String, numeric: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = WisdomTextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(WisdomCyan),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .background(WisdomCard)
                    .border(1.dp, WisdomBorderWhite, shape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        }
    )
}
