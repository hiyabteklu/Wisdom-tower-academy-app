package com.wisdomtower.academy.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.ADMIN_CONTENT_TREE
import com.wisdomtower.academy.data.admin.ADMIN_HUB_IDS
import com.wisdomtower.academy.data.admin.AdminContentInput
import com.wisdomtower.academy.data.admin.AdminContentItem
import com.wisdomtower.academy.data.admin.AdminContentRepository
import com.wisdomtower.academy.data.admin.AdminContentResult
import com.wisdomtower.academy.data.admin.AdminNavNode
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ADMIN-native N7 (docs/ADMIN_NATIVE_PLAN.md). Native version of the website ContentPanel (Content library):
// Academy > package > subject > hub as a 2-column grid of folder cards (not tall thin rows), then the items of
// the chosen hub with Add item, Edit and Delete, and the editor with the website's fields (title, chapter, notes or
// video link, Content JSON for flashcards, question banks and exams, Appwrite File ID, Published).
// The tree is the hand copy in data/admin/AdminContentTree.kt (also used by Content Locks).
// Choosing a file on the phone: ONLY for the hub life-savers (S5d, PaidPdfPicker.kt, private bucket through the
// Edge Function paid-pdf-upload). Books still use the pasted File ID. The editor
// replaces the list inside this screen (no pop-up window); Back and the arrow go up one step. Large text boxes
// (notes and JSON) use plain remember, not rememberSaveable, so a big exam never overflows the saved-state limit.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private fun resolvePath(pathKey: String): List<AdminNavNode> {
    if (pathKey.isEmpty()) return emptyList()
    val out = ArrayList<AdminNavNode>()
    var level: List<AdminNavNode> = ADMIN_CONTENT_TREE
    for (id in pathKey.split("|")) {
        val next = level.firstOrNull { it.id == id } ?: break
        out.add(next)
        level = next.children
    }
    return out
}

private fun blankItem(packageId: String, scopePath: String, hub: String, sortOrder: Int): AdminContentItem =
    AdminContentItem(
        id = "",
        packageId = packageId,
        scopePath = scopePath,
        hub = hub,
        title = "",
        chapter = null,
        sortOrder = sortOrder,
        contentType = AdminContentRepository.contentTypeFor(hub, "markdown"),
        storagePath = null,
        bodyMd = null,
        metaRaw = AdminContentRepository.defaultMeta(hub),
        published = false
    )

@Composable
internal fun CourseContentScreen(modifier: Modifier = Modifier) {
    var pathKey by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<AdminContentResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<AdminContentItem?>(null) }
    var saving by remember { mutableStateOf(false) }
    var busyId by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<AdminContentItem?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val crumbs: List<AdminNavNode> = resolvePath(pathKey)
    val node: AdminNavNode? = crumbs.lastOrNull()
    val nodeScope: String? = node?.scopePath
    val isHub = node != null && nodeScope != null && ADMIN_HUB_IDS.contains(node.id)
    val hubId: String = if (isHub && node != null) node.id else ""
    val hubScope: String = if (isHub && nodeScope != null) nodeScope else ""
    val hubPackage: String = node?.packageId ?: "freshman"

    LaunchedEffect(hubScope, hubId, reloadKey) {
        if (hubId.isEmpty()) {
            result = null
            loading = false
        } else {
            loading = true
            result = null
            result = AdminContentRepository.list(hubScope, hubId)
            loading = false
        }
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(4500L)
            toast = ""
        }
    }
    LaunchedEffect(pathKey) { listState.scrollToItem(0) }

    // Registered after the one in AdminConsoleHost, so these run first. They never overlap.
    BackHandler(enabled = editing != null) { if (!saving) editing = null }
    BackHandler(enabled = editing == null && crumbs.isNotEmpty()) {
        pathKey = crumbs.dropLast(1).joinToString("|") { it.id }
    }

    fun doSave(itemId: String, input: AdminContentInput) {
        scope.launch {
            saving = true
            val outcome = AdminContentRepository.save(itemId, input)
            saving = false
            toast = if (outcome.ok) {
                if (input.published) "Saved & published" else "Saved (draft)"
            } else {
                outcome.message
            }
            if (outcome.ok) {
                editing = null
                reloadKey += 1
            }
        }
    }

    fun doDelete(item: AdminContentItem) {
        scope.launch {
            busyId = item.id
            val outcome = AdminContentRepository.delete(item.id)
            busyId = ""
            toast = outcome.message
            if (outcome.ok) reloadKey += 1
        }
    }

    val current = result
    val editTarget = editing
    val pathText = (listOf("Academy") + crumbs.map { it.label }).joinToString(" \u203A ")
    val folderRows: List<List<AdminNavNode>> =
        (if (node == null) ADMIN_CONTENT_TREE else node.children).chunked(2)

    Box(modifier = modifier.fillMaxSize()) {
        if (editTarget != null) {
            ContentEditor(
                initial = editTarget,
                saving = saving,
                onBack = { if (!saving) editing = null },
                onSave = { input -> doSave(editTarget.id, input) }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Content library",
                                color = WisdomTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "All package PDFs/files \u2192 Appwrite \u00B7 catalog & auth stay on Supabase",
                                color = WisdomMuted,
                                fontSize = 12.sp
                            )
                        }
                        if (isHub) {
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
                }

                item(key = "crumbs") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (crumbs.isNotEmpty()) {
                            IconButton(
                                onClick = { pathKey = crumbs.dropLast(1).joinToString("|") { it.id } }
                            ) {
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

                if (!isHub) {
                    items(folderRows, key = { row -> "row-" + row[0].id }) { row ->
                        FolderRow(row = row, onOpen = { picked ->
                            pathKey = (crumbs + picked).joinToString("|") { it.id }
                        })
                    }
                } else {
                    item(key = "hubInfo") {
                        // S5d: Life Savers can now be chosen on the phone, so the old "not in the app yet" sentence goes.
                        val hint = AdminContentRepository.hubHint(hubId)
                            .replace(" Choosing a file on the phone is not in the app yet.", "")
                        Text(
                            text = "Scope $hubScope \u00B7 hub $hubId \u00B7 " + hint,
                            color = WisdomMuted,
                            fontSize = 12.sp
                        )
                    }
                    item(key = "add") {
                        AdminActionButton(
                            label = "Add item",
                            tone = WisdomAccentEmerald,
                            filled = true,
                            enabled = true,
                            onClick = {
                                val count = if (current is AdminContentResult.Success) current.items.size else 0
                                editing = blankItem(hubPackage, hubScope, hubId, count * 10)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (current == null) {
                        item(key = "loading") {
                            Text(
                                text = "Loading\u2026",
                                color = WisdomMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            )
                        }
                    } else if (current is AdminContentResult.Failure) {
                        item(key = "error") {
                            AdminMessageCard(
                                title = "Could not load the items",
                                body = current.message,
                                actionLabel = "Try again",
                                onAction = { reloadKey += 1 }
                            )
                        }
                    } else if (current is AdminContentResult.Success) {
                        if (current.items.isEmpty()) {
                            item(key = "empty") {
                                AdminMessageCard(
                                    title = "No items yet",
                                    body = "No items yet. Tap Add item.",
                                    actionLabel = null,
                                    onAction = {}
                                )
                            }
                        }
                        items(current.items, key = { "item-" + it.id }) { row ->
                            ContentItemCard(
                                item = row,
                                busy = busyId == row.id,
                                onEdit = { editing = row },
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

    val toDelete = deleteTarget
    if (toDelete != null) {
        AdminConfirmDialog(
            title = "Delete this content item?",
            text = toDelete.title.ifEmpty { "Untitled" },
            confirmLabel = "Delete",
            confirmTone = AdminRose,
            onConfirm = {
                deleteTarget = null
                doDelete(toDelete)
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun FolderRow(row: List<AdminNavNode>, onOpen: (AdminNavNode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FolderCard(node = row[0], onClick = { onOpen(row[0]) }, modifier = Modifier.weight(1f))
        if (row.size > 1) {
            FolderCard(node = row[1], onClick = { onOpen(row[1]) }, modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FolderCard(node: AdminNavNode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minHeight = 80.dp)
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = WisdomAccentAmber,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = node.label,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContentItemCard(item: AdminContentItem, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val status = buildString {
        append(item.contentType)
        if (item.chapter != null) append(" \u00B7 ch ").append(item.chapter)
        append(if (item.published) " \u00B7 published" else " \u00B7 draft")
        if (AdminContentRepository.isAppwritePath(item.storagePath)) append(" \u00B7 Appwrite")
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
            text = item.title.ifEmpty { "Untitled" },
            color = WisdomTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = status, color = WisdomMuted, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminActionButton(
                label = "Edit",
                tone = WisdomCyan,
                filled = false,
                enabled = !busy,
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            )
            AdminActionButton(
                label = "Delete",
                tone = AdminRose,
                filled = false,
                enabled = !busy,
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(text = text, color = WisdomMuted, fontSize = 12.sp)
}

@Composable
private fun ContentEditor(
    initial: AdminContentItem,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (AdminContentInput) -> Unit
) {
    val isNew = initial.id.isEmpty()
    val hub = initial.hub
    val hasFile = hub == "books" || hub == "life-savers" || hub == "videos"
    val hasBody = hub == "short-notes" || hub == "videos"
    val hasMeta = hub == "flashcards" || hub == "question-banks" || hub == "exams"

    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var chapterText by rememberSaveable(initial.id) { mutableStateOf(initial.chapter?.toString().orEmpty()) }
    var fileId by rememberSaveable(initial.id) {
        mutableStateOf(AdminContentRepository.appwriteIdOf(initial.storagePath))
    }
    var published by rememberSaveable(initial.id) { mutableStateOf(initial.published) }
    // Large text: plain remember on purpose (see the note at the top of this file).
    var bodyMd by remember(initial.id) { mutableStateOf(initial.bodyMd.orEmpty()) }
    var metaText by remember(initial.id) { mutableStateOf(AdminContentRepository.prettyMeta(initial.metaRaw)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header") {
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
                Text(
                    text = if (isNew) "New item" else "Edit item",
                    color = WisdomTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item(key = "where") {
            Text(
                text = "Scope ${initial.scopePath} \u00B7 hub ${initial.hub}",
                color = WisdomMuted,
                fontSize = 12.sp
            )
        }
        item(key = "titleLabel") { EditorLabel("Title") }
        item(key = "title") {
            AdminTextField(value = title, onValueChange = { title = it }, placeholder = "Title")
        }
        item(key = "chapterLabel") { EditorLabel("Chapter # (optional)") }
        item(key = "chapter") {
            AdminTextField(
                value = chapterText,
                onValueChange = { chapterText = it },
                placeholder = "e.g. 3",
                keyboardType = KeyboardType.Number
            )
        }
        if (hasFile) {
            item(key = "fileLabel") {
                EditorLabel(
                    "Appwrite File ID " +
                        if (hub == "life-savers") "(for Life Saver PDFs)" else "(for large textbooks)"
                )
            }
            item(key = "file") {
                AdminTextField(
                    value = fileId,
                    onValueChange = { fileId = it },
                    placeholder = "Paste file ID from Appwrite Console"
                )
            }
            if (hub == "life-savers") {
                // S5d: pick a PDF on the phone; it goes to the PRIVATE bucket through the admin-only server function.
                item(key = "pdfPick") {
                    PaidPdfPicker(onUploaded = { id -> fileId = id })
                }
            }
            item(key = "fileHelp") {
                Text(
                    text = if (hub == "life-savers") {
                        "Tap Choose PDF from phone (private bucket, up to 50 MB), or paste a File ID from " +
                            "Appwrite \u2192 Storage \u2192 paid-pdfs."
                    } else {
                        "Upload the PDF in Appwrite \u2192 Storage \u2192 your bucket \u2192 Create file, then copy the " +
                            "File ID here. Choosing a file on the phone is not in the app yet."
                    },
                    color = WisdomMuted,
                    fontSize = 12.sp
                )
            }
            val existing = initial.storagePath
            if (existing != null && !AdminContentRepository.isAppwritePath(existing)) {
                item(key = "fileKept") {
                    Text(
                        text = "Current file: $existing (kept unless you paste a File ID)",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
        if (hasBody) {
            item(key = "bodyLabel") {
                EditorLabel(if (hub == "videos") "Video URL (or notes)" else "Short notes (Markdown)")
            }
            item(key = "body") {
                AdminTextArea(
                    value = bodyMd,
                    onValueChange = { bodyMd = it },
                    placeholder = if (hub == "videos") "https://youtube.com/..." else "## Topic\n\nYour short notes\u2026",
                    minHeight = 180.dp
                )
            }
        }
        if (hasMeta) {
            item(key = "metaLabel") { EditorLabel("Content JSON (meta)") }
            item(key = "meta") {
                AdminTextArea(
                    value = metaText,
                    onValueChange = { metaText = it },
                    placeholder = "{}",
                    minHeight = 220.dp
                )
            }
        }
        item(key = "publishedLabel") { EditorLabel("Published (visible to students who own the package)") }
        item(key = "published") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdminChoiceChip(
                    label = "Published",
                    selected = published,
                    onClick = { published = true },
                    modifier = Modifier.weight(1f),
                    tone = WisdomAccentEmerald
                )
                AdminChoiceChip(
                    label = "Draft",
                    selected = !published,
                    onClick = { published = false },
                    modifier = Modifier.weight(1f),
                    tone = WisdomMuted
                )
            }
        }
        item(key = "actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminActionButton(
                    label = "Cancel",
                    tone = WisdomMuted,
                    filled = false,
                    enabled = !saving,
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
                AdminActionButton(
                    label = if (saving) "Saving..." else "Save",
                    tone = WisdomAccentEmerald,
                    filled = true,
                    enabled = !saving,
                    onClick = {
                        val pasted = fileId.trim()
                        onSave(
                            AdminContentInput(
                                packageId = initial.packageId,
                                scopePath = initial.scopePath,
                                hub = initial.hub,
                                title = title,
                                chapter = chapterText.trim().toIntOrNull(),
                                sortOrder = initial.sortOrder,
                                contentType = AdminContentRepository.contentTypeFor(initial.hub, initial.contentType),
                                storagePath = if (pasted.isNotEmpty()) AdminContentRepository.appwritePath(pasted) else initial.storagePath,
                                bodyMd = bodyMd,
                                metaText = metaText,
                                published = published
                            )
                        )
                    },
                    modifier = Modifier.weight(1.6f)
                )
            }
        }
    }
}
