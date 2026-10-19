package com.wisdomtower.academy.ui.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.ADMIN_PACKAGES
import com.wisdomtower.academy.data.admin.CatalogItem
import com.wisdomtower.academy.data.admin.CatalogRepository
import com.wisdomtower.academy.data.admin.CatalogResult
import com.wisdomtower.academy.data.admin.OverviewRepository
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ADMIN-native N6c (docs/ADMIN_NATIVE_PLAN.md). Native version of the website CatalogPanel (Package catalog): list of
// catalog rows with price, Edit and Delete, Add package, and the editor with every field of the website form.
// The editor replaces the list inside this screen (no pop-up window), with its own back arrow and Back handling.
// Not carried over: Seed from static (needs the website's full static texts). Delete asks for confirmation in the
// app's own dialog (the website used a browser confirm). Texts copied from the website where it has them.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val GROUPS: List<String> = listOf("grades", "branch", "special", "custom")

private val RoseTone = Color(0xFFFB7185)

/** Ids of the packages that are defined in the website code (static list). For these the website shows its own price, description and includes. */
private fun isBuiltIn(id: String): Boolean =
    id == "grade-9-12" || ADMIN_PACKAGES.any { it.id == id }

private fun newItem(): CatalogItem = CatalogItem(
    id = "custom-" + java.lang.Long.toString(System.currentTimeMillis(), 36),
    name = "",
    shortName = "",
    description = "",
    priceEtb = 250.0,
    href = "/packages",
    image = "",
    includes = emptyList(),
    enrolledLabel = "",
    group = "custom",
    active = true,
    sortOrder = 200
)

@Composable
internal fun CatalogScreen(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<CatalogResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<CatalogItem?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var busyId by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CatalogItem?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loading = true
        result = CatalogRepository.list()
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(4500L)
            toast = ""
        }
    }

    // Registered after the one in AdminConsoleHost, so it runs first.
    BackHandler(enabled = editing != null) { if (!saving) editing = null }

    fun doSave(input: CatalogItem) {
        scope.launch {
            saving = true
            val outcome = CatalogRepository.save(input, isNew)
            saving = false
            toast = outcome.message
            if (outcome.ok) {
                editing = null
                reloadKey += 1
            }
        }
    }

    fun doDelete(item: CatalogItem) {
        scope.launch {
            busyId = item.id
            val outcome = CatalogRepository.delete(item.id)
            busyId = ""
            toast = outcome.message
            if (outcome.ok) reloadKey += 1
        }
    }

    val current = result
    val editTarget = editing

    Box(modifier = modifier.fillMaxSize()) {
        if (editTarget != null) {
            CatalogEditor(
                initial = editTarget,
                isNew = isNew,
                saving = saving,
                onBack = { if (!saving) editing = null },
                onSave = { doSave(it) }
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
                                text = "Package catalog",
                                color = WisdomTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Prices and About text shown on /packages and offer banners. Default new price: 250 ETB.",
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

                item(key = "add") {
                    AdminActionButton(
                        label = "Add package",
                        tone = WisdomAccentEmerald,
                        filled = true,
                        enabled = true,
                        onClick = {
                            isNew = true
                            editing = newItem()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "note") {
                    val noteShape = RoundedCornerShape(14.dp)
                    Text(
                        text = "Note: for the packages that exist in the website code (grade-9 to ece-y3-sem-2), students see " +
                            "the price, description and includes from the website code, so changing those here does not " +
                            "change what they see. Rows you add here use their own values.",
                        color = WisdomAccentAmber,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(noteShape)
                            .background(WisdomAccentAmber.copy(alpha = 0.08f))
                            .border(1.dp, WisdomAccentAmber.copy(alpha = 0.25f), noteShape)
                            .padding(12.dp)
                    )
                }

                if (current == null) {
                    item(key = "loading") {
                        Text(
                            text = "Loading catalog...",
                            color = WisdomMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                } else if (current is CatalogResult.Failure) {
                    item(key = "error") {
                        AdminMessageCard(
                            title = "Could not load the catalog",
                            body = current.message,
                            actionLabel = "Try again",
                            onAction = { reloadKey += 1 }
                        )
                    }
                } else if (current is CatalogResult.Success) {
                    if (current.items.isEmpty()) {
                        item(key = "empty") {
                            AdminMessageCard(
                                title = "No catalog rows",
                                body = "No catalog rows yet. Seed from static packages (on the website) or add one.",
                                actionLabel = null,
                                onAction = {}
                            )
                        }
                    }
                    items(current.items, key = { it.id }) { row ->
                        CatalogCard(
                            row = row,
                            busy = busyId == row.id,
                            onEdit = {
                                isNew = false
                                editing = row
                            },
                            onDelete = { deleteTarget = row }
                        )
                    }
                    item(key = "footnote") {
                        Text(
                            text = "Static fallback list has ${ADMIN_PACKAGES.size} packages in this app's copy.",
                            color = WisdomMuted,
                            fontSize = 12.sp
                        )
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
            title = "Delete package?",
            text = "Delete package ${toDelete.id}?",
            confirmLabel = "Delete",
            confirmTone = RoseTone,
            onConfirm = {
                deleteTarget = null
                doDelete(toDelete)
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun CatalogCard(row: CatalogItem, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.name.ifEmpty { row.id },
                color = WisdomTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!row.active) {
                AdminToneBadge(text = "Inactive", tone = WisdomMuted)
            }
        }
        Text(text = "(${row.id})", color = WisdomMuted, fontSize = 12.sp)
        Text(
            text = OverviewRepository.etb(row.priceEtb),
            color = WisdomAccentAmber,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        if (row.description.isNotEmpty()) {
            Text(
                text = row.description,
                color = WisdomMuted,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
                tone = RoseTone,
                filled = false,
                enabled = !busy,
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, color = WisdomMuted, fontSize = 12.sp)
}

@Composable
private fun CatalogEditor(
    initial: CatalogItem,
    isNew: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (CatalogItem) -> Unit
) {
    var id by rememberSaveable(initial.id) { mutableStateOf(initial.id) }
    var priceText by rememberSaveable(initial.id) { mutableStateOf(OverviewRepository.plainNumber(initial.priceEtb)) }
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var shortName by rememberSaveable(initial.id) { mutableStateOf(initial.shortName) }
    var group by rememberSaveable(initial.id) { mutableStateOf(initial.group) }
    var description by rememberSaveable(initial.id) { mutableStateOf(initial.description) }
    var href by rememberSaveable(initial.id) { mutableStateOf(initial.href) }
    var image by rememberSaveable(initial.id) { mutableStateOf(initial.image) }
    var includesText by rememberSaveable(initial.id) { mutableStateOf(initial.includes.joinToString("\n")) }
    var enrolledLabel by rememberSaveable(initial.id) { mutableStateOf(initial.enrolledLabel) }
    var sortText by rememberSaveable(initial.id) { mutableStateOf(initial.sortOrder.toString()) }
    var active by rememberSaveable(initial.id) { mutableStateOf(initial.active) }

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
                    text = if (isNew) "New package" else "Edit package",
                    color = WisdomTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (!isNew && isBuiltIn(id)) {
            item(key = "builtIn") {
                Text(
                    text = "This id is a built-in package: the price, description and includes students see come from the " +
                        "website code, not from this row.",
                    color = WisdomAccentAmber,
                    fontSize = 12.sp
                )
            }
        }
        item(key = "idLabel") { FieldLabel("ID") }
        item(key = "id") {
            Box(modifier = Modifier.alpha(if (isNew) 1f else 0.6f)) {
                if (isNew) {
                    AdminTextField(value = id, onValueChange = { id = it }, placeholder = "custom-id")
                } else {
                    AdminTextField(value = id, onValueChange = {}, placeholder = "")
                }
            }
        }
        item(key = "priceLabel") { FieldLabel("Price (ETB)") }
        item(key = "price") {
            AdminTextField(
                value = priceText,
                onValueChange = { priceText = it },
                placeholder = "250",
                keyboardType = KeyboardType.Decimal
            )
        }
        item(key = "nameLabel") { FieldLabel("Name") }
        item(key = "name") {
            AdminTextField(value = name, onValueChange = { name = it }, placeholder = "Package name")
        }
        item(key = "shortLabel") { FieldLabel("Short name") }
        item(key = "short") {
            AdminTextField(value = shortName, onValueChange = { shortName = it }, placeholder = "Short name")
        }
        item(key = "groupLabel") { FieldLabel("Group") }
        item(key = "group") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (g in GROUPS) {
                    AdminChoiceChip(
                        label = g,
                        selected = group == g,
                        onClick = { group = g },
                        modifier = Modifier.weight(1f),
                        tone = WisdomCyan
                    )
                }
            }
        }
        item(key = "descLabel") { FieldLabel("About this package (shown to buyers)") }
        item(key = "desc") {
            AdminTextArea(
                value = description,
                onValueChange = { description = it },
                placeholder = "What students get, who it is for, and what is included...",
                minHeight = 140.dp
            )
        }
        item(key = "hrefLabel") { FieldLabel("Preview href") }
        item(key = "href") {
            AdminTextField(value = href, onValueChange = { href = it }, placeholder = "/academy/freshman")
        }
        item(key = "imageLabel") { FieldLabel("Image URL") }
        item(key = "image") {
            AdminTextField(
                value = image,
                onValueChange = { image = it },
                placeholder = "https://...",
                keyboardType = KeyboardType.Uri
            )
        }
        item(key = "includesLabel") { FieldLabel("Includes (one per line)") }
        item(key = "includes") {
            AdminTextArea(
                value = includesText,
                onValueChange = { includesText = it },
                placeholder = "One item per line",
                minHeight = 100.dp
            )
        }
        item(key = "enrolledLabelLabel") { FieldLabel("Enrolled label") }
        item(key = "enrolled") {
            AdminTextField(
                value = enrolledLabel,
                onValueChange = { enrolledLabel = it },
                placeholder = "e.g. 320+ students"
            )
        }
        item(key = "sortLabel") { FieldLabel("Sort order") }
        item(key = "sort") {
            AdminTextField(
                value = sortText,
                onValueChange = { sortText = it },
                placeholder = "200",
                keyboardType = KeyboardType.Number
            )
        }
        item(key = "activeLabel") { FieldLabel("Active (sellable)") }
        item(key = "active") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdminChoiceChip(
                    label = "Active",
                    selected = active,
                    onClick = { active = true },
                    modifier = Modifier.weight(1f),
                    tone = WisdomAccentEmerald
                )
                AdminChoiceChip(
                    label = "Inactive",
                    selected = !active,
                    onClick = { active = false },
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
                    label = if (saving) "Saving..." else "Save package",
                    tone = WisdomAccentEmerald,
                    filled = true,
                    enabled = !saving,
                    onClick = {
                        onSave(
                            CatalogItem(
                                id = id,
                                name = name,
                                shortName = shortName,
                                description = description,
                                priceEtb = priceText.trim().toDoubleOrNull() ?: 0.0,
                                href = href,
                                image = image,
                                includes = includesText.split("\n"),
                                enrolledLabel = enrolledLabel,
                                group = group,
                                active = active,
                                sortOrder = sortText.trim().toIntOrNull() ?: 0
                            )
                        )
                    },
                    modifier = Modifier.weight(1.6f)
                )
            }
        }
    }
}
