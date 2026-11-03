package com.wisdomtower.academy.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import com.wisdomtower.academy.ui.theme.wisdomDialogEnter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.content.BookFileStore
import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.data.content.ContentItemResult
import com.wisdomtower.academy.data.content.ContentListResult
import com.wisdomtower.academy.data.content.CourseLocks
import com.wisdomtower.academy.data.content.HubGate
import com.wisdomtower.academy.data.content.LearningContentRepository
import com.wisdomtower.academy.data.content.LearningResource
import com.wisdomtower.academy.data.content.SavedContent
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomMessageCard
import com.wisdomtower.academy.ui.theme.wisdomReveal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Progress value meaning "downloading, size not known" (notes, flashcards, questions have no byte count). */
private const val SIZE_UNKNOWN = -1f

/** P1a: how long the refresh must run before the small muted "Updating…" word shows (a fast refresh shows nothing). */
private const val UPDATING_HINT_DELAY_MS = 1500L

/** Soft red used for "remove" and failure texts. D6b: a getter (night FDA4AF as before, day deep rose) so it follows the switch. */
private val RemoveRed: Color get() = dayNight(Color(0xFFFDA4AF), WisdomAccentRose)

/** One row of the hub list: a real material, or (Telegram build) an empty chapter the admin set to Coming soon. */
private sealed interface HubListEntry {
    data class Real(val item: LearningResource) : HubListEntry
    data class SoonChapter(val chapter: Int) : HubListEntry
}

/**
 * The materials in their order, plus a placeholder row for each Coming soon chapter in [soonChapters] that has no
 * material. A placeholder goes before the first material of a later chapter, else at the end.
 */
private fun buildHubEntries(items: List<LearningResource>, soonChapters: List<Int>): List<HubListEntry> {
    val have = items.mapNotNull { it.chapter }.toSet()
    val missing = soonChapters.filter { it !in have }
    if (missing.isEmpty()) return items.map { HubListEntry.Real(it) }
    val out = ArrayList<HubListEntry>()
    var next = 0
    for (item in items) {
        val c = item.chapter
        if (c != null) {
            while (next < missing.size && missing[next] < c) {
                out.add(HubListEntry.SoonChapter(missing[next]))
                next++
            }
        }
        out.add(HubListEntry.Real(item))
    }
    while (next < missing.size) {
        out.add(HubListEntry.SoonChapter(missing[next]))
        next++
    }
    return out
}

/**
 * Native list of the items inside one learning hub of one course (books, notes, flashcards, question banks, exams).
 * Rows, per-item Download button, "Download all (n)" / "Remove saved (n)", offline marks, granular admin locks
 * (lock icon + "Coming soon" / "Package required" pop-up) and the S2 light list (counts from `item_count`) are unchanged;
 * see the git history of this file for the full F4 / F4b / GLASS / D6b / GRANULAR LOCKS / SECURITY S2 notes.
 *
 * COMPACT HEADER (owner request 2026-10-22): no round back button and no course-title + hub-title block. ONE small muted
 * line names the hub (the course was already shown on the previous screen). Back = phone back key / gesture, handled by
 * the [BackHandler] in StudyWorkspaceScreen. [courseTitle] is kept so the caller does not change.
 *
 * TELEGRAM PAID BUILD (S1, `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md`): the list is never closed; only rows get the lock icon
 * (the gate of each row comes from [CourseLocks.itemGate]). A chapter the admin made empty and set to Coming soon shows as
 * a row "Chapter N: Coming soon" even with no materials ([CourseLocks.comingSoonChapters]). A tap on an Owned-only row still
 * shows the old "Package required" pop-up until S2 replaces it with the native purchase flow.
 *
 * P1a (2026-11-03, `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md`, owner problem 8): SAVED LIST FIRST. When this hub was opened
 * before on this phone, its saved list ([LearningContentRepository.listSaved], no network) is drawn at once, with no
 * spinner; the server is asked in the background ([LearningContentRepository.list], short timeout because a saved list
 * exists) and the screen swaps the list ONLY when the new rows really differ (no flicker). If the refresh takes longer
 * than 1.5 s the hub line gets a muted " · Updating…" after it; a refresh that fails keeps the saved list. The full-screen
 * spinner only shows when nothing was ever saved for this hub. Locks are unchanged: the lock rows still mark rows and the
 * checked door still decides when an item is opened.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun HubItemListScreen(
    scopePath: String,
    hubId: String,
    hubName: String,
    courseTitle: String,
    onBack: () -> Unit,
    onOpenItem: (LearningResource) -> Unit,
    modifier: Modifier = Modifier,
    locks: CourseLocks? = null
) {
    val context = LocalContext.current
    val loggedIn by AuthStateManager.isLoggedIn.collectAsState()
    var result by remember(scopePath, hubId) { mutableStateOf<ContentListResult?>(null) }
    var reload by remember { mutableStateOf(0) }
    // P1a: true while the background refresh has run longer than UPDATING_HINT_DELAY_MS
    var updating by remember(scopePath, hubId) { mutableStateOf(false) }
    val seenPrefs = remember { context.getSharedPreferences("wt_seen_resources", Context.MODE_PRIVATE) }
    var seen by remember { mutableStateOf<Set<String>>(seenPrefs.getStringSet("ids", null)?.toSet() ?: emptySet()) }

    // F4: downloads (ids saved on this phone, ids busy right now with their progress, ids that failed)
    val downloadScope = rememberCoroutineScope()
    val downloadPrefs = remember { context.getSharedPreferences("wt_downloaded_resources", Context.MODE_PRIVATE) }
    var downloaded by remember { mutableStateOf<Set<String>>(downloadPrefs.getStringSet("ids", null)?.toSet() ?: emptySet()) }
    var busy by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var failed by remember { mutableStateOf<Set<String>>(emptySet()) }

    // T4 (2026-10-12): Real book file sizes (memory map keyed by resource id)
    var fileSizeMap by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    // F4b: "Download all" progress (items done, items in total) and the items waiting for a "Remove" answer
    var bulk by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var confirmRemove by remember { mutableStateOf<List<LearningResource>?>(null) }

    // GRANULAR LOCKS: the pop-up for a tapped locked row (gate + the row's title)
    var lockedPopup by remember { mutableStateOf<Pair<HubGate, String>?>(null) }

    // S1 (Telegram build): empty chapters set to Coming soon (empty list in the Play build)
    val soonChapters = locks?.comingSoonChapters(hubId).orEmpty()

    /** The extra lock (chapter or item row) on one material, or null when it is open. */
    fun lockOf(item: LearningResource): HubGate? =
        locks?.itemGate(hubId, item.chapter, item.id)?.takeIf { it != HubGate.Open }

    /** Saves one item. The caller has already put the item into [busy]; this removes it again at the end. */
    suspend fun runDownload(item: LearningResource) {
        failed = failed - item.id
        try {
            downloadItem(context, item) { fraction ->
                val newPct = (fraction.coerceIn(0f, 1f) * 100).toInt()
                val oldPct = ((busy[item.id] ?: -1f).coerceIn(0f, 1f) * 100).toInt()
                if (newPct != oldPct || busy[item.id] == null) {
                    busy = busy + (item.id to fraction)
                }
            }
            downloaded = downloaded + item.id
            downloadPrefs.edit().putStringSet("ids", downloaded.toSet()).apply()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = failed + item.id
        } finally {
            busy = busy - item.id
        }
    }

    fun startDownload(item: LearningResource) {
        if (item.id in busy) return
        if (lockOf(item) != null) return
        busy = busy + (item.id to SIZE_UNKNOWN)
        downloadScope.launch { runDownload(item) }
    }

    fun downloadAll(all: List<LearningResource>) {
        if (bulk != null) return
        val todo = all.filter { it.id !in downloaded && it.id !in busy && lockOf(it) == null }
        if (todo.isEmpty()) return
        bulk = 0 to todo.size
        downloadScope.launch {
            try {
                for ((index, item) in todo.withIndex()) {
                    busy = busy + (item.id to SIZE_UNKNOWN)
                    runDownload(item)
                    bulk = (index + 1) to todo.size
                }
            } finally {
                bulk = null
            }
        }
    }

    fun removeSaved(list: List<LearningResource>) {
        downloadScope.launch {
            withContext(Dispatchers.IO) {
                for (item in list) SavedContent.delete(context, item)
            }
            val ids = list.map { it.id }.toSet()
            downloaded = downloaded - ids
            downloadPrefs.edit().putStringSet("ids", downloaded.toSet()).apply()
            failed = failed - ids
        }
    }

    // P1a: saved list first (no network, drawn at once), then the server in the background; swap only when it differs.
    LaunchedEffect(scopePath, hubId, loggedIn, reload) {
        if (loggedIn) {
            // S2: always a light list; counts come from item_count, heavy content from getResource.
            val saved = LearningContentRepository.listSaved(context, scopePath, hubId)
            val shown = result
            if (saved != null) {
                // Draw the saved list at once (keep what is already shown when it is a list: a Retry or a re-run must not flicker).
                if (shown !is ContentListResult.Success) result = ContentListResult.Success(saved, true)
            } else if (shown !is ContentListResult.Success) {
                // Nothing saved: the full-screen spinner (also after a Retry on a failure card).
                result = null
            }
            val hint = launch {
                delay(UPDATING_HINT_DELAY_MS)
                updating = true
            }
            try {
                val fresh = LearningContentRepository.list(context, scopePath, hubId)
                val now = result
                if (fresh is ContentListResult.Success) {
                    // Replace only when the rows really differ (data class equality of every field).
                    if (now !is ContentListResult.Success || now.items != fresh.items) result = fresh
                } else if (now !is ContentListResult.Success) {
                    // A failed refresh never throws away a list that is already on the screen.
                    result = fresh
                }
            } finally {
                hint.cancel()
                updating = false
            }
        }
    }

    // T4: If this is the books hub, load cached file sizes instantly and probe missing ones quietly
    LaunchedEffect(result, hubId) {
        val current = result
        if (hubId == "books" && current is ContentListResult.Success) {
            // 1) Quick read of local cache / saved files (no network)
            val quickSizes = mutableMapOf<String, Long>()
            for (item in current.items) {
                BookFileStore.getCachedFileSize(context, item.id)?.let {
                    quickSizes[item.id] = it
                }
            }
            if (quickSizes.isNotEmpty()) {
                fileSizeMap = fileSizeMap + quickSizes
            }

            // 2) Probe remaining un-downloaded books with lightweight HEAD / Range request
            for (item in current.items) {
                if (item.id !in fileSizeMap && item.id !in quickSizes) {
                    val url = LearningContentRepository.appwriteViewUrl(item.storagePath)
                    if (url != null) {
                        val probed = BookFileStore.probeFileSize(context, item.id, url)
                        if (probed != null && probed > 0L) {
                            fileSizeMap = fileSizeMap + (item.id to probed)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hub_list_header") {
                Text(
                    // P1a: a saved list is on screen and the refresh is slow: one small muted word, never blocking.
                    text = if (updating && result is ContentListResult.Success) "$hubName \u00B7 Updating\u2026" else hubName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WisdomMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val current = result
            when {
                !loggedIn -> item(key = "hub_list_signin") {
                    WisdomMessageCard( // S6-msgcard: card and Sign in pill in one
                        title = "Sign in to open",
                        message = "Create a free account to unlock books, notes, questions, and exams. " +
                            "No payment required for registered students.",
                        showSignIn = true
                    )
                }

                current == null -> item(key = "hub_list_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = WisdomCyan)
                    }
                }

                current is ContentListResult.Failure -> item(key = "hub_list_failure") {
                    MessageCard(title = null, message = current.message, onRetry = { reload += 1 })
                }

                current is ContentListResult.Success && current.items.isEmpty() && soonChapters.isEmpty() ->
                    item(key = "hub_list_empty") {
                        MessageCard(
                            title = null,
                            message = "No published materials in this hub yet. Check back soon.",
                            onRetry = null
                        )
                    }

                current is ContentListResult.Success -> {
                    val entries = buildHubEntries(current.items, soonChapters)
                    item(key = "hub_list_tools") {
                        DownloadTools(
                            todo = current.items.count { it.id !in downloaded && lockOf(it) == null },
                            saved = current.items.count { it.id in downloaded },
                            bulk = bulk,
                            onDownloadAll = { downloadAll(current.items) },
                            onRemoveAll = { confirmRemove = current.items.filter { it.id in downloaded } }
                        )
                    }
                    items(
                        entries,
                        key = { entry ->
                            when (entry) {
                                is HubListEntry.Real -> entry.item.id
                                is HubListEntry.SoonChapter -> "soon_chapter_${entry.chapter}"
                            }
                        }
                    ) { entry ->
                        val rowIndex = entries.indexOf(entry)
                        when (entry) {
                            is HubListEntry.SoonChapter -> {
                                Box(modifier = Modifier.wisdomReveal("hub_${hubId}_soon_${entry.chapter}", rowIndex)) {
                                    SoonChapterRow(chapter = entry.chapter) {
                                        lockedPopup = HubGate.ComingSoon to "Chapter ${entry.chapter}"
                                    }
                                }
                            }

                            is HubListEntry.Real -> {
                                val item = entry.item
                                val isNew = item.id !in seen
                                val rowLock = lockOf(item)
                                Box(modifier = Modifier.wisdomReveal("hub_${hubId}_${item.id}", rowIndex)) {
                                    ItemRow(
                                        item = item,
                                        hubId = hubId,
                                        isNew = isNew && rowLock == null,
                                        isDownloaded = item.id in downloaded,
                                        knownSize = if (hubId == "books") fileSizeMap[item.id] else null,
                                        progress = busy[item.id],
                                        hasFailed = item.id in failed,
                                        lockedGate = rowLock,
                                        onDownload = { startDownload(item) },
                                        onRemove = { confirmRemove = listOf(item) }
                                    ) {
                                        if (rowLock != null) {
                                            lockedPopup = rowLock to item.title
                                        } else {
                                            seen = seen + item.id
                                            seenPrefs.edit().putStringSet("ids", seen.toSet()).apply()
                                            onOpenItem(item)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "hub_list_bottom") { Spacer(modifier = Modifier.height(32.dp)) }
        }

        val toRemove = confirmRemove
        if (toRemove != null && toRemove.isNotEmpty()) {
            AlertDialog(
                modifier = Modifier.wisdomDialogEnter(),
                onDismissRequest = { confirmRemove = null },
                containerColor = dayNight(Color(0xF2111B2E), Color(0xF2F8F5EE)),
                title = {
                    Text(
                        text = if (toRemove.size == 1) "Remove from this phone?" else "Remove ${toRemove.size} saved items?",
                        color = WisdomInk,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (toRemove.size == 1) {
                            "\"${toRemove[0].title}\" will no longer open offline. You can download it again at any time."
                        } else {
                            "These items will no longer open offline. You can download them again at any time."
                        },
                        color = WisdomMuted
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        removeSaved(toRemove)
                        confirmRemove = null
                    }) {
                        Text(text = "Remove", color = RemoveRed, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmRemove = null }) {
                        Text(text = "Cancel", color = WisdomCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // GRANULAR LOCKS: same pop-up as a closed hub card, titled with the tapped row.
        val popup = lockedPopup
        if (popup != null) {
            if (popup.first == HubGate.ComingSoon) {
                HubGateDialog(
                    badge = ContentAccess.COMING_SOON_TITLE,
                    title = ContentAccess.COMING_SOON_TITLE,
                    hubName = popup.second,
                    message = ContentAccess.COMING_SOON_BODY,
                    buttonText = ContentAccess.COMING_SOON_BUTTON,
                    onClose = { lockedPopup = null }
                )
            } else {
                HubGateDialog(
                    badge = null,
                    title = ContentAccess.PURCHASE_TITLE,
                    hubName = popup.second,
                    message = ContentAccess.PURCHASE_BODY,
                    buttonText = ContentAccess.PURCHASE_BUTTON,
                    onClose = { lockedPopup = null }
                )
            }
        }
    }
}

/**
 * Saves one item on the phone. Books: the PDF file (progress 0..1 through [onProgress]). Everything else: the
 * full row from the server, saved by the repository (throws when the server could not be reached, so a stale
 * copy is never reported as "downloaded").
 */
private suspend fun downloadItem(context: Context, item: LearningResource, onProgress: (Float) -> Unit) {
    if (item.contentType == "pdf") {
        val url = LearningContentRepository.appwriteViewUrl(item.storagePath)
            ?: throw IllegalStateException("No file address")
        BookFileStore.ensureFile(context, item.id, url, onProgress)
    } else {
        val result = LearningContentRepository.getResource(context, item.id, refresh = true)
        val found = result as? ContentItemResult.Found ?: throw IllegalStateException("Not available")
        if (found.fromCache) throw java.io.IOException("Offline")
    }
}

/**
 * The two buttons above the list: "Download all (n)" (n = items not saved yet) or, while it runs,
 * "Downloading 2 of 10..."; and "Remove saved (n)" when something of this hub is saved.
 */
@Composable
private fun DownloadTools(
    todo: Int,
    saved: Int,
    bulk: Pair<Int, Int>?,
    onDownloadAll: () -> Unit,
    onRemoveAll: () -> Unit
) {
    if (bulk == null && todo == 0 && saved == 0) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val currentBulk = bulk
        if (currentBulk != null) {
            val done = currentBulk.first
            val total = currentBulk.second
            ToolPill(
                text = "Downloading ${minOf(done + 1, total)} of $total\u2026",
                color = WisdomCyan,
                enabled = false,
                onClick = {},
                leadingIcon = {
                    DownloadGauge(
                        state = DownloadState.DOWNLOADING,
                        progress = if (total > 0) done.toFloat() / total.toFloat() else null,
                        size = 14.dp,
                        strokeWidth = 2.dp
                    )
                }
            )
        } else {
            if (todo > 0) {
                ToolPill(text = "Download all ($todo)", color = WisdomCyan, enabled = true, onClick = onDownloadAll)
            }
            if (saved > 0) {
                ToolPill(text = "Remove saved ($saved)", color = RemoveRed, enabled = true, onClick = onRemoveAll)
            }
        }
    }
}

/** Small rounded text button (same look as the "Retry" button of the message card). */
@Composable
private fun ToolPill(
    text: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.45f)), RoundedCornerShape(10.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = color.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingIcon?.invoke()
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

/** S1 (Telegram build): the row of an empty chapter that the admin set to Coming soon. Lock icon, tap = Coming soon message. */
@Composable
private fun SoonChapterRow(chapter: Int, onClick: () -> Unit) {
    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chapter $chapter: Coming soon",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WisdomInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = WisdomMuted,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: LearningResource,
    hubId: String,
    isNew: Boolean,
    isDownloaded: Boolean,
    knownSize: Long?,
    progress: Float?,
    hasFailed: Boolean,
    lockedGate: HubGate?,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val count = remember(item.id, item.itemCount, item.metaJson) { itemCount(item) }
    val countText = when {
        count == null -> null
        hubId == "flashcards" -> "$count ${if (count == 1) "card" else "cards"}"
        else -> "$count ${if (count == 1) "question" else "questions"}"
    }
    val sizeText = when {
        hubId != "books" -> null
        knownSize != null && knownSize > 0L -> BookFileStore.formatBytes(knownSize)
        else -> null
    }
    val lockText = when (lockedGate) {
        HubGate.ComingSoon -> "Coming soon"
        HubGate.PackageRequired -> "Package required"
        else -> null
    }
    val subline = listOfNotNull(
        item.chapter?.let { "Chapter $it" },
        sizeText,
        countText,
        lockText,
        if (isDownloaded && lockedGate == null) "Saved offline" else null
    ).joinToString(" \u00B7 ")

    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WisdomInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isNew) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = dayNight(Color(0xFFFECDD3), WisdomAccentRose)
                        )
                    }
                }
                if (subline.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subline, fontSize = 12.sp, color = WisdomMuted)
                }
                if (progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (progress >= 0f) {
                        val barProgress by animateFloatAsState(
                            targetValue = progress.coerceIn(0f, 1f),
                            animationSpec = tween(250),
                            label = "hubRowProgress"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(WisdomInk.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barProgress)
                                    .height(4.dp)
                                    .background(WisdomCyan)
                            )
                        }
                    } else {
                        Text(text = "Downloading\u2026", fontSize = 11.sp, color = WisdomCyan)
                    }
                } else if (hasFailed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download failed. Check your connection and tap the button to try again.",
                        fontSize = 11.sp,
                        color = RemoveRed
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (lockedGate != null) {
                // Locked by the admin: no download button, a lock instead of the arrow.
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = WisdomMuted,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(20.dp)
                )
            } else {
                DownloadButton(
                    isDownloaded = isDownloaded,
                    progress = progress,
                    failed = hasFailed,
                    onClick = if (isDownloaded) onRemove else onDownload
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = WisdomMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Small round button on the item: download arrow, circular gauge while busy, green check when saved,
 * red retry icon when failed.
 * F4b: the green check can be tapped (it asks before removing the saved copy); [onClick] is the download
 * for a not saved item and the remove question for a saved one.
 */
@Composable
private fun DownloadButton(
    isDownloaded: Boolean,
    progress: Float?,
    failed: Boolean = false,
    onClick: () -> Unit
) {
    val enabled = progress == null
    val border = when {
        isDownloaded -> BorderStroke(1.dp, dayNight(Color(0xFF34D399), WisdomAccentEmerald).copy(alpha = 0.6f))
        failed -> BorderStroke(1.dp, dayNight(Color(0xFFF87171), WisdomAccentRose).copy(alpha = 0.6f))
        else -> BorderStroke(1.dp, WisdomBorderWhite)
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(WisdomGlassFillStrong)
            .border(border, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = WisdomCyan.copy(alpha = 0.25f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        DownloadGauge(
            isDownloaded = isDownloaded,
            progress = progress,
            failed = failed,
            size = 22.dp,
            strokeWidth = 2.dp
        )
    }
}

/**
 * Number of flashcards or questions of a row: the database column `item_count` (security slice S2), or, for an old
 * saved row that still carries its `meta`, the length of its `cards` / `questions`; null when it has neither.
 */
private fun itemCount(item: LearningResource): Int? = item.itemCount ?: runCatching {
    val meta = JSONObject(item.metaJson)
    (meta.optJSONArray("cards") ?: meta.optJSONArray("questions"))?.length()
}.getOrNull()

@Composable
private fun MessageCard(title: String?, message: String, onRetry: (() -> Unit)?) {
    WisdomMessageCard(message = message, title = title, onRetry = onRetry) // S6-msgcard
}

/**
 * The round back button used at the top of the study screens (glass look, same as the Targets screen).
 * D6b: the arrow is [WisdomInk] (white at night, deep navy by day); the glass fill and edge were already tokens.
 */
@Composable
internal fun ScreenBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(WisdomGlassFillStrong)
            .border(BorderStroke(1.dp, WisdomGlassBorder), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = WisdomCyan.copy(alpha = 0.2f)),
                role = androidx.compose.ui.semantics.Role.Button, // S3-a11y
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = WisdomInk,
            modifier = Modifier.size(20.dp)
        )
    }
}

// D6b: night value first (exactly the old literal), day value second. A function, so it is read while composing and follows the switch.
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day
