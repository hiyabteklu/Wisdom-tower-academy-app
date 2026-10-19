package com.wisdomtower.academy.ui.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wisdomtower.academy.data.admin.AdminStorage
import com.wisdomtower.academy.data.admin.FreeResourceDraft
import com.wisdomtower.academy.data.admin.FreeResourceItem
import com.wisdomtower.academy.data.admin.FreeResourcePage
import com.wisdomtower.academy.data.admin.FreeResourcesRepository
import com.wisdomtower.academy.data.admin.FreeResourcesResult
import com.wisdomtower.academy.ui.guides.GuidesRepository
import com.wisdomtower.academy.ui.guides.UniversityDetail
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ADMIN-native N10 (docs/ADMIN_NATIVE_PLAN.md). Native version of the website FreeResourcesPanel: six tabs, the page
// settings card (title, subtitle, Markdown text, published) for Success Stories and Scholarships, an info card for the
// four note-only tabs, the item list (Edit, Delete, Add) and the item editor with every field of the website form,
// including the photo. The editor replaces the list inside this screen (no pop-up window), with its own back arrow and
// Back handling, like Pricing and Catalog. The photo is chosen with the phone's own file chooser (GetContent) and
// uploaded to the public bucket when the item is saved. Texts copied from the website where it has them.
// Differences from the website: the deadline is typed as 2026-11-30 (no date picker in the app); the university list
// comes from the app's own university guide (assets data universities.json), shown as a search list; Delete asks in
// the app's own dialog; an Open live page button leaves the admin to the student app page.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

/** Everything the item editor collects, handed to the screen when Save item is tapped. */
private data class ItemForm(
    val title: String,
    val subtitle: String,
    val body: String,
    val metaEdits: Map<String, String>,
    val published: Boolean,
    val featured: Boolean,
    val deadline: String,
    val externalUrl: String,
    val pickedUri: Uri?
)

@Composable
internal fun FreeResourcesScreen(
    origin: String,
    onLeaveToApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<FreeResourcesResult?>(null) }
    var lastLoadedSlug by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var activeSlug by rememberSaveable { mutableStateOf(FreeResourcesRepository.SLUGS[0]) }
    var editing by remember { mutableStateOf<FreeResourceItem?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var savingPage by remember { mutableStateOf(false) }
    var busyId by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<FreeResourceItem?>(null) }
    var universities by remember { mutableStateOf<List<UniversityDetail>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeSlug, reloadKey) {
        if (lastLoadedSlug != activeSlug) result = null
        loading = true
        val loaded = FreeResourcesRepository.load(activeSlug)
        lastLoadedSlug = activeSlug
        result = loaded
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(4500L)
            toast = ""
        }
    }
    LaunchedEffect(activeSlug) {
        if (activeSlug == "universities" && universities.isEmpty()) {
            universities = withContext(Dispatchers.IO) { GuidesRepository.loadUniversities(context).second }
        }
    }

    // Registered after the one in AdminConsoleHost, so it runs first.
    BackHandler(enabled = editing != null) { if (!saving) editing = null }

    val current = result
    val editTarget = editing
    val pages: List<FreeResourcePage> = if (current is FreeResourcesResult.Success) current.pages else emptyList()
    val rows: List<FreeResourceItem> = if (current is FreeResourcesResult.Success) current.items else emptyList()
    val itemKind = FreeResourcesRepository.kindFor(activeSlug)
    val noteOnly = FreeResourcesRepository.isNoteOnly(activeSlug)

    fun openNew(count: Int) {
        val kind = itemKind ?: return
        isNew = true
        editing = FreeResourceItem(
            id = "",
            pageSlug = activeSlug,
            kind = kind,
            title = "",
            subtitle = "",
            bodyMd = "",
            imagePath = "",
            externalUrl = "",
            deadline = "",
            metaJson = FreeResourcesRepository.buildMeta(kind, activeSlug, "{}", emptyMap()).toString(),
            sortOrder = count * 10,
            featured = false,
            published = false
        )
    }

    fun doSavePage(pageId: String?, title: String, subtitle: String, body: String, published: Boolean) {
        val slug = activeSlug
        scope.launch {
            savingPage = true
            val outcome = FreeResourcesRepository.savePage(pageId, slug, title, subtitle, body, published)
            savingPage = false
            toast = outcome.message
            if (outcome.ok) reloadKey += 1
        }
    }

    fun doSaveItem(base: FreeResourceItem, form: ItemForm) {
        val slug = activeSlug
        val creating = isNew
        scope.launch {
            saving = true
            var imagePath: String? = if (base.imagePath.isEmpty()) null else base.imagePath
            val uri = form.pickedUri
            if (uri != null) {
                val upload = AdminStorage.uploadFreeResourcePhoto(context, uri, slug)
                val uploadError = upload.error
                if (uploadError != null) {
                    saving = false
                    toast = uploadError
                    return@launch
                }
                imagePath = upload.path
            }
            val draft = FreeResourceDraft(
                id = if (creating) null else base.id,
                pageSlug = slug,
                kind = base.kind,
                title = form.title,
                subtitle = form.subtitle,
                bodyMd = form.body,
                imagePath = imagePath,
                externalUrl = form.externalUrl,
                deadline = form.deadline,
                meta = FreeResourcesRepository.buildMeta(base.kind, slug, base.metaJson, form.metaEdits),
                sortOrder = base.sortOrder,
                featured = form.featured,
                published = form.published
            )
            val outcome = FreeResourcesRepository.saveItem(draft)
            saving = false
            toast = outcome.message
            if (outcome.ok) {
                editing = null
                isNew = false
                reloadKey += 1
            }
        }
    }

    fun doDelete(item: FreeResourceItem) {
        scope.launch {
            busyId = item.id
            val outcome = FreeResourcesRepository.deleteItem(item.id)
            busyId = ""
            toast = outcome.message
            if (outcome.ok) reloadKey += 1
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (editTarget != null) {
            FreeItemEditor(
                initial = editTarget,
                isNew = isNew,
                noteOnly = noteOnly,
                slug = activeSlug,
                universities = universities,
                saving = saving,
                onBack = { if (!saving) editing = null },
                onSave = { form -> doSaveItem(editTarget, form) }
            )
        } else {
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
                                text = "Free resources",
                                color = WisdomTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Stories and scholarships are full cards. University, department, campus life, and study " +
                                    "technique notes only append at the end of the hard-coded guides.",
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

                item(key = "tabs") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (pair in FreeResourcesRepository.SLUGS.chunked(2)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (slug in pair) {
                                    val live = pages.firstOrNull { it.slug == slug }?.published == true
                                    val mark = if (live && !FreeResourcesRepository.isNoteOnly(slug)) "\u2713 " else ""
                                    AdminChoiceChip(
                                        label = mark + FreeResourcesRepository.labelFor(slug),
                                        selected = activeSlug == slug,
                                        onClick = { activeSlug = slug },
                                        modifier = Modifier.weight(1f),
                                        tone = WisdomCyan
                                    )
                                }
                            }
                        }
                    }
                }

                if (current == null) {
                    item(key = "loading") {
                        Text(
                            text = "Loading...",
                            color = WisdomMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                } else if (current is FreeResourcesResult.Failure) {
                    item(key = "error") {
                        AdminMessageCard(
                            title = "Could not load free resources",
                            body = current.message,
                            actionLabel = "Try again",
                            onAction = { reloadKey += 1 }
                        )
                    }
                } else if (current is FreeResourcesResult.Success) {
                    if (!noteOnly) {
                        item(key = "page") {
                            PageSettingsCard(
                                slug = activeSlug,
                                page = pages.firstOrNull { it.slug == activeSlug },
                                saving = savingPage,
                                onOpenLive = { onLeaveToApp("$origin/academy/$activeSlug") },
                                onSave = { title, subtitle, body, published ->
                                    doSavePage(
                                        pages.firstOrNull { it.slug == activeSlug }?.id,
                                        title,
                                        subtitle,
                                        body,
                                        published
                                    )
                                }
                            )
                        }
                    } else {
                        item(key = "noteinfo") {
                            NoteOnlyCard(onOpenLive = { onLeaveToApp("$origin/academy/$activeSlug") })
                        }
                    }

                    if (itemKind != null) {
                        item(key = "itemsHeader") {
                            val title = when (itemKind) {
                                "success_story" -> "Student stories"
                                "scholarship" -> "Opportunity listings"
                                else -> "Published notes"
                            }
                            Column {
                                Text(
                                    text = title,
                                    color = WisdomTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${rows.size} \u00B7 ${rows.count { it.published }} live",
                                    color = WisdomMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        item(key = "add") {
                            AdminActionButton(
                                label = FreeResourcesRepository.addLabel(itemKind),
                                tone = WisdomAccentEmerald,
                                filled = true,
                                enabled = true,
                                onClick = { openNew(rows.size) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (rows.isEmpty()) {
                            item(key = "empty") {
                                AdminMessageCard(
                                    title = "No items yet",
                                    body = "Tap the Add button to create the first one.",
                                    actionLabel = null,
                                    onAction = {}
                                )
                            }
                        }
                        items(rows, key = { it.id }) { row ->
                            val uni = if (activeSlug == "universities") {
                                val uniId = FreeResourcesRepository.metaText(row.metaJson, "universityId")
                                universities.firstOrNull { it.id == uniId }
                            } else {
                                null
                            }
                            val line = if (uni != null) {
                                "${uni.abbr} \u00B7 ${uni.name}"
                            } else if (row.subtitle.isNotEmpty()) {
                                row.subtitle
                            } else {
                                row.kind
                            }
                            FreeItemCard(
                                item = row,
                                line = line,
                                busy = busyId == row.id,
                                onEdit = {
                                    isNew = false
                                    editing = row
                                },
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
            title = "Delete item?",
            text = "Delete this item permanently?",
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
private fun FreeFieldLabel(text: String) {
    Text(text = text, color = WisdomMuted, fontSize = 12.sp)
}

/** Photo box: the picture when there is one (a web link or a chosen file), otherwise the words No photo. */
@Composable
private fun FreeThumb(model: Any?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "No photo",
                color = WisdomMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FreeChoiceRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = text,
        color = if (selected) WisdomAccentEmerald else WisdomTextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) WisdomAccentEmerald.copy(alpha = 0.12f) else WisdomCard)
            .border(
                1.dp,
                if (selected) WisdomAccentEmerald.copy(alpha = 0.4f) else WisdomBorderWhite,
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

@Composable
private fun PublishedChips(published: Boolean, onChange: (Boolean) -> Unit, publishedLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AdminChoiceChip(
            label = publishedLabel,
            selected = published,
            onClick = { onChange(true) },
            modifier = Modifier.weight(1f),
            tone = WisdomAccentEmerald
        )
        AdminChoiceChip(
            label = "Draft",
            selected = !published,
            onClick = { onChange(false) },
            modifier = Modifier.weight(1f),
            tone = WisdomMuted
        )
    }
}

@Composable
private fun NoteOnlyCard(onOpenLive: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomAccentAmber.copy(alpha = 0.08f))
            .border(1.dp, WisdomAccentAmber.copy(alpha = 0.25f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Additive notes only",
            color = WisdomAccentAmber,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Hard-coded page content stays in code. Notes you publish here appear at the end of the matching " +
                "card or section on the live page.",
            color = WisdomMuted,
            fontSize = 13.sp
        )
        AdminActionButton(
            label = "Open live page",
            tone = WisdomCyan,
            filled = false,
            enabled = true,
            onClick = onOpenLive,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PageSettingsCard(
    slug: String,
    page: FreeResourcePage?,
    saving: Boolean,
    onOpenLive: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var title by rememberSaveable(slug, page) { mutableStateOf(page?.title ?: FreeResourcesRepository.labelFor(slug)) }
    var subtitle by rememberSaveable(slug, page) { mutableStateOf(page?.subtitle ?: "") }
    var body by remember(slug, page) { mutableStateOf(page?.bodyMd ?: "") }
    var published by rememberSaveable(slug, page) { mutableStateOf(page?.published ?: false) }
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
            text = "Page settings \u00B7 $slug",
            color = WisdomMuted,
            fontSize = 12.sp
        )
        AdminActionButton(
            label = "Open live page",
            tone = WisdomCyan,
            filled = false,
            enabled = true,
            onClick = onOpenLive,
            modifier = Modifier.fillMaxWidth()
        )
        FreeFieldLabel("Page title")
        AdminTextField(value = title, onValueChange = { title = it }, placeholder = "Page title")
        FreeFieldLabel("Subtitle")
        AdminTextField(value = subtitle, onValueChange = { subtitle = it }, placeholder = "One line under the title")
        FreeFieldLabel(
            if (slug == "scholarships") "Tips and guidance (Markdown)" else "Intro blurb (Markdown, optional)"
        )
        AdminTextArea(
            value = body,
            onValueChange = { body = it },
            placeholder = "## Heading\n\nYour content...",
            minHeight = if (slug == "scholarships") 200.dp else 100.dp
        )
        PublishedChips(published = published, onChange = { published = it }, publishedLabel = "Page published")
        AdminActionButton(
            label = if (saving) "Saving..." else "Save page",
            tone = WisdomAccentEmerald,
            filled = true,
            enabled = !saving,
            onClick = { onSave(title, subtitle, body, published) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FreeItemCard(
    item: FreeResourceItem,
    line: String,
    busy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val photoUrl = AdminStorage.publicUrl(item.imagePath)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FreeThumb(
                model = if (photoUrl.isEmpty()) null else photoUrl,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = WisdomTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = line,
                    color = WisdomMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AdminToneBadge(
                text = if (item.published) "Published" else "Draft",
                tone = if (item.published) WisdomAccentEmerald else WisdomMuted
            )
            if (item.featured) {
                AdminToneBadge(text = "Featured", tone = WisdomAccentAmber)
            }
        }
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
private fun FreeItemEditor(
    initial: FreeResourceItem,
    isNew: Boolean,
    noteOnly: Boolean,
    slug: String,
    universities: List<UniversityDetail>,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (ItemForm) -> Unit
) {
    val kind = initial.kind
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var subtitle by rememberSaveable(initial.id) { mutableStateOf(initial.subtitle) }
    var body by remember(initial.id) { mutableStateOf(initial.bodyMd) }
    var studentName by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "studentName"))
    }
    var resultText by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "result"))
    }
    var program by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "program"))
    }
    var year by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "year"))
    }
    var quote by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "quote"))
    }
    var organization by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "organization"))
    }
    var amount by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "amount"))
    }
    var eligibility by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "eligibility"))
    }
    var universityId by rememberSaveable(initial.id) {
        mutableStateOf(FreeResourcesRepository.metaText(initial.metaJson, "universityId"))
    }
    var deadline by rememberSaveable(initial.id) { mutableStateOf(initial.deadline) }
    var externalUrl by rememberSaveable(initial.id) { mutableStateOf(initial.externalUrl) }
    var published by rememberSaveable(initial.id) { mutableStateOf(initial.published) }
    var featured by rememberSaveable(initial.id) { mutableStateOf(initial.featured) }
    var pickedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var uniQuery by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> if (uri != null) pickedUri = uri }
    )

    val needle = uniQuery.trim().lowercase()
    val matches = universities
        .filter { needle.isEmpty() || it.name.lowercase().contains(needle) || it.abbr.lowercase().contains(needle) }
        .take(8)
    val chosen = universities.firstOrNull { it.id == universityId }

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
        item(key = "titleLabel") { FreeFieldLabel("Title *") }
        item(key = "title") {
            AdminTextField(value = title, onValueChange = { title = it }, placeholder = "Title")
        }
        item(key = "subtitleLabel") { FreeFieldLabel("Subtitle") }
        item(key = "subtitle") {
            AdminTextField(value = subtitle, onValueChange = { subtitle = it }, placeholder = "Subtitle")
        }

        if (slug == "universities") {
            item(key = "uniLabel") { FreeFieldLabel("University *") }
            item(key = "uniChosen") {
                val chosenText = if (chosen != null) {
                    "Selected: ${chosen.abbr} \u00B7 ${chosen.name}"
                } else if (universityId.isNotEmpty()) {
                    "Selected: $universityId"
                } else {
                    "None selected"
                }
                Text(
                    text = chosenText,
                    color = if (universityId.isEmpty()) WisdomMuted else WisdomAccentEmerald,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item(key = "uniSearch") {
                AdminSearchBox(
                    query = uniQuery,
                    onQueryChange = { uniQuery = it },
                    placeholder = "Search universities"
                )
            }
            item(key = "uniList") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (matches.isEmpty()) {
                        Text(text = "No university found", color = WisdomMuted, fontSize = 13.sp)
                    }
                    for (u in matches) {
                        FreeChoiceRow(
                            text = "${u.abbr} \u00B7 ${u.name}",
                            selected = u.id == universityId,
                            onClick = { universityId = u.id }
                        )
                    }
                }
            }
        }

        if (kind == "success_story") {
            item(key = "nameLabel") { FreeFieldLabel("Student name") }
            item(key = "name") {
                AdminTextField(value = studentName, onValueChange = { studentName = it }, placeholder = "Student name")
            }
            item(key = "resultLabel") { FreeFieldLabel("Result / score") }
            item(key = "result") {
                AdminTextField(value = resultText, onValueChange = { resultText = it }, placeholder = "e.g. 600/600")
            }
            item(key = "programLabel") { FreeFieldLabel("Program") }
            item(key = "program") {
                AdminTextField(value = program, onValueChange = { program = it }, placeholder = "Program")
            }
            item(key = "yearLabel") { FreeFieldLabel("Year") }
            item(key = "year") {
                AdminTextField(value = year, onValueChange = { year = it }, placeholder = "Year")
            }
            item(key = "quoteLabel") { FreeFieldLabel("Quote") }
            item(key = "quote") {
                AdminTextField(value = quote, onValueChange = { quote = it }, placeholder = "Quote")
            }
        }

        if (kind == "scholarship") {
            item(key = "orgLabel") { FreeFieldLabel("Organization") }
            item(key = "org") {
                AdminTextField(value = organization, onValueChange = { organization = it }, placeholder = "Organization")
            }
            item(key = "amountLabel") { FreeFieldLabel("Amount") }
            item(key = "amount") {
                AdminTextField(value = amount, onValueChange = { amount = it }, placeholder = "Amount")
            }
            item(key = "deadlineLabel") { FreeFieldLabel("Deadline (year-month-day, for example 2026-11-30)") }
            item(key = "deadline") {
                AdminTextField(value = deadline, onValueChange = { deadline = it }, placeholder = "2026-11-30")
            }
            item(key = "urlLabel") { FreeFieldLabel("External URL") }
            item(key = "url") {
                AdminTextField(
                    value = externalUrl,
                    onValueChange = { externalUrl = it },
                    placeholder = "https://...",
                    keyboardType = KeyboardType.Uri
                )
            }
            item(key = "eligLabel") { FreeFieldLabel("Eligibility") }
            item(key = "elig") {
                AdminTextArea(
                    value = eligibility,
                    onValueChange = { eligibility = it },
                    placeholder = "Who can apply",
                    minHeight = 80.dp
                )
            }
        }

        item(key = "bodyLabel") {
            FreeFieldLabel(if (noteOnly) "Note body (Markdown)" else "Full text / story (Markdown)")
        }
        item(key = "body") {
            AdminTextArea(
                value = body,
                onValueChange = { body = it },
                placeholder = "Write in Markdown",
                minHeight = 160.dp
            )
        }

        if (!noteOnly) {
            item(key = "photoLabel") { FreeFieldLabel("Photo") }
            item(key = "photo") {
                val picked = pickedUri
                val existing = AdminStorage.publicUrl(initial.imagePath)
                val previewModel: Any? = picked ?: (if (existing.isEmpty()) null else existing)
                val hint = if (picked != null) {
                    "New photo chosen. It uploads when you save (max 8 MB)."
                } else if (existing.isNotEmpty()) {
                    "The current photo stays unless you choose a new one."
                } else {
                    "Optional. Max 8 MB."
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FreeThumb(
                        model = previewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    Text(text = hint, color = WisdomMuted, fontSize = 12.sp)
                    AdminActionButton(
                        label = if (previewModel == null) "Choose photo" else "Change photo",
                        tone = WisdomCyan,
                        filled = false,
                        enabled = !saving,
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item(key = "publishedLabel") { FreeFieldLabel("Visibility") }
        item(key = "published") {
            PublishedChips(published = published, onChange = { published = it }, publishedLabel = "Published")
        }
        if (!noteOnly) {
            item(key = "featuredLabel") { FreeFieldLabel("Featured") }
            item(key = "featured") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdminChoiceChip(
                        label = "Featured",
                        selected = featured,
                        onClick = { featured = true },
                        modifier = Modifier.weight(1f),
                        tone = WisdomAccentAmber
                    )
                    AdminChoiceChip(
                        label = "Not featured",
                        selected = !featured,
                        onClick = { featured = false },
                        modifier = Modifier.weight(1f),
                        tone = WisdomMuted
                    )
                }
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
                    label = if (saving) "Saving..." else "Save item",
                    tone = WisdomAccentEmerald,
                    filled = true,
                    enabled = !saving,
                    onClick = {
                        val edits = LinkedHashMap<String, String>()
                        if (kind == "success_story") {
                            edits["studentName"] = studentName
                            edits["result"] = resultText
                            edits["program"] = program
                            edits["year"] = year
                            edits["quote"] = quote
                        }
                        if (kind == "scholarship") {
                            edits["organization"] = organization
                            edits["amount"] = amount
                            edits["eligibility"] = eligibility
                        }
                        if (slug == "universities") {
                            edits["universityId"] = universityId
                        }
                        onSave(
                            ItemForm(
                                title = title,
                                subtitle = subtitle,
                                body = body,
                                metaEdits = edits,
                                published = published,
                                featured = featured,
                                deadline = deadline,
                                externalUrl = externalUrl,
                                pickedUri = pickedUri
                            )
                        )
                    },
                    modifier = Modifier.weight(1.6f)
                )
            }
        }
    }
}
