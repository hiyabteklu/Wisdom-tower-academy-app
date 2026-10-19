package com.wisdomtower.academy.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.BroadcastsRepository
import com.wisdomtower.academy.data.admin.ComposerRepository
import com.wisdomtower.academy.data.admin.ContentBlock
import com.wisdomtower.academy.data.admin.ContentRun
import com.wisdomtower.academy.data.admin.CustomAudience
import com.wisdomtower.academy.data.admin.EditorBlock
import com.wisdomtower.academy.data.admin.LevelCounts
import com.wisdomtower.academy.data.admin.NotificationAuthoring
import com.wisdomtower.academy.data.admin.NotificationPaths
import com.wisdomtower.academy.data.admin.ScheduledItem
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ADMIN-native N8b (docs/ADMIN_NATIVE_PLAN.md). Native version of the website NotificationComposerV2 (with the block
// editor and the audience picker). Same five steps: 1 Category, 2 Write, 3 Destination, 4 Audience, 5 When.
// One scrolling form, no sideways scrolling. Differences from the website are listed in the session log.
// Pieces: data/admin/NotificationAuthoring.kt (blocks to content), NotificationPaths.kt (destination tree),
// ComposerRepository.kt (send, schedule, audience calls). Reuses the admin kit.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val Gold = Color(0xFFFCD34D)
private val GoldSoft = Color(0xBFFCD34D)
private val BodyText = Color(0xFFE2E8F0)
private val DarkOnGold = Color(0xFF0B1220)
private val PreviewBg = Color(0xFF0B1220)

private class UiBlock(val id: String, val t: String)

private class SubmitPlan(
    val later: Boolean,
    val sendAtIso: String,
    val content: JSONObject,
    val preview: String
)

private class Category(val id: String, val label: String, val hint: String, val icon: ImageVector)

private val CATEGORIES: List<Category> = listOf(
    Category(
        "admin", "Admin announcement",
        "Students read it inside Notifications. It does not take them anywhere.",
        Icons.Filled.Notifications
    ),
    Category(
        "material", "New material release",
        "Opens the subject or hub you pick below.",
        Icons.AutoMirrored.Filled.MenuBook
    ),
    Category(
        "contest", "Contest",
        "Opens the Contest (trophy) screen. Nothing to choose.",
        Icons.Filled.EmojiEvents
    )
)

private val BLOCK_LABEL: Map<String, String> = mapOf(
    "title" to "Title",
    "subtitle" to "Subtitle",
    "p" to "Paragraph",
    "callout" to "Callout",
    "list" to "Bullets (one per line)",
    "divider" to "Divider"
)

private fun sameText(a: String, b: String): Boolean = a.trim().equals(b.trim(), ignoreCase = true)

private fun parseEatInstant(date: String, time: String): Date? {
    val d = date.trim()
    val t = time.trim()
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(d)) return null
    if (!Regex("^\\d{2}:\\d{2}$").matches(t)) return null
    return try {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        f.isLenient = false
        f.parse(d + "T" + t + ":00+03:00")
    } catch (e: Exception) {
        null
    }
}

private fun utcIso(date: Date): String {
    val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(date)
}

/** The starting values of the form: empty for a new notification, filled from a waiting row when editing. */
private class ComposerInitial(
    val category: String,
    val title: String,
    val blocks: List<EditorBlock>,
    val trail: List<String>,
    val groups: List<String>,
    val date: String,
    val time: String
) {
    companion object {
        fun from(edit: ScheduledItem?): ComposerInitial {
            if (edit == null) {
                return ComposerInitial(
                    category = "admin",
                    title = "",
                    blocks = listOf(
                        EditorBlock(NotificationAuthoring.newId(), "title", ""),
                        EditorBlock(NotificationAuthoring.newId(), "p", "")
                    ),
                    trail = emptyList(),
                    groups = emptyList(),
                    date = "",
                    time = ""
                )
            }
            val known = edit.category == "admin" || edit.category == "material" || edit.category == "contest"
            val fromContent = edit.content?.let { NotificationAuthoring.fromJson(it) }
            val blocks = if (fromContent != null && fromContent.isNotEmpty()) {
                fromContent
            } else {
                listOf(EditorBlock(NotificationAuthoring.newId(), "p", edit.body))
            }
            val url = edit.url
            val trail = if (url != null) (NotificationPaths.trailForPath(url) ?: emptyList()) else emptyList()
            val parts = BroadcastsRepository.eatDateTime(edit.sendAt)
            return ComposerInitial(
                category = if (known) (edit.category ?: "admin") else "admin",
                title = edit.title,
                blocks = blocks,
                trail = trail,
                groups = edit.audiences,
                date = parts?.first ?: "",
                time = parts?.second ?: ""
            )
        }
    }
}

@Composable
internal fun BroadcastComposerScreen(
    origin: String,
    edit: ScheduledItem?,
    onFinished: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember(edit) { ComposerInitial.from(edit) }
    val editing = edit != null
    val scope = rememberCoroutineScope()

    var category by remember { mutableStateOf(initial.category) }
    var title by remember { mutableStateOf(initial.title) }
    val blocks = remember {
        mutableStateListOf<UiBlock>().apply { for (b in initial.blocks) add(UiBlock(b.id, b.t)) }
    }
    val values = remember {
        mutableStateMapOf<String, TextFieldValue>().apply {
            for (b in initial.blocks) put(b.id, TextFieldValue(b.text))
        }
    }
    var activeId by remember { mutableStateOf<String?>(null) }
    val trail = remember { mutableStateListOf<String>().apply { addAll(initial.trail) } }
    var mode by remember { mutableStateOf("groups") }
    val groups = remember { mutableStateListOf<String>().apply { addAll(initial.groups) } }
    var groupCount by remember { mutableStateOf<Int?>(null) }
    var targetUser by remember { mutableStateOf("") }
    var whenMode by remember { mutableStateOf(if (editing) "later" else "now") }
    var dateText by remember { mutableStateOf(initial.date) }
    var timeText by remember { mutableStateOf(initial.time) }
    var sending by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var pendingPlan by remember { mutableStateOf<SubmitPlan?>(null) }

    // Audience picker state
    var counts by remember { mutableStateOf<LevelCounts?>(null) }
    var customs by remember { mutableStateOf<List<CustomAudience>>(emptyList()) }
    var audError by remember { mutableStateOf<String?>(null) }
    var audReload by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var newLabel by remember { mutableStateOf("") }
    val newLevels = remember { mutableStateListOf<String>() }
    var newPackages by remember { mutableStateOf("") }
    var audBusy by remember { mutableStateOf(false) }
    var deleteAudience by remember { mutableStateOf<CustomAudience?>(null) }

    BackHandler(enabled = true) { onClose() }

    LaunchedEffect(audReload) {
        audError = null
        val levelReply = ComposerRepository.levelCounts()
        val customReply = ComposerRepository.customAudiences()
        val levelValue = levelReply.value
        val customValue = customReply.value
        if (levelValue != null) counts = levelValue else audError = levelReply.error
        if (customValue != null) customs = customValue else audError = customReply.error ?: audError
    }

    val groupsKey = groups.toList()
    LaunchedEffect(groupsKey, counts) {
        if (groupsKey.isEmpty()) {
            groupCount = counts?.total
        } else {
            groupCount = ComposerRepository.countAudience(groupsKey)
        }
    }

    val later = whenMode == "later" || editing
    val trailList = trail.toList()
    val pickedPath = if (category == "material") NotificationPaths.pathFromTrail(trailList) else null
    val crumbs = if (category == "material") NotificationPaths.crumbsFor(trailList) else emptyList()

    fun editorBlocks(): List<EditorBlock> = blocks.map { EditorBlock(it.id, it.t, values[it.id]?.text ?: "") }

    fun addBlock(type: String) {
        val b = UiBlock(NotificationAuthoring.newId(), type)
        blocks.add(b)
        values[b.id] = TextFieldValue("")
    }

    fun wrapSelection(marker: String) {
        val id = activeId
        val current = if (id == null) null else values[id]
        if (id == null || current == null || current.selection.collapsed) {
            feedback = Pair(false, "Select some text in a box first, then press Bold or Highlight.")
            return
        }
        val start = current.selection.min
        val end = current.selection.max
        val text = current.text
        values[id] = TextFieldValue(
            text = text.substring(0, start) + marker + text.substring(start, end) + marker + text.substring(end),
            selection = TextRange(start, end + marker.length * 2)
        )
        feedback = null
    }

    fun validate(): SubmitPlan? {
        feedback = null
        val contentBlocks = NotificationAuthoring.toBlocks(editorBlocks())
        val content = NotificationAuthoring.toJson(contentBlocks)
        if (title.isBlank()) {
            feedback = Pair(false, "Write the notification title first.")
            return null
        }
        if (content == null) {
            feedback = Pair(false, "Write the message (add a paragraph with some text).")
            return null
        }
        if (category == "material" && pickedPath == null) {
            feedback = Pair(false, "Pick where this material notice should open.")
            return null
        }
        if (later && mode == "user") {
            feedback = Pair(false, "Scheduling works for groups of students. Use Send now for one student.")
            return null
        }
        if (mode == "user" && targetUser.isBlank()) {
            feedback = Pair(false, "Enter the student's user id.")
            return null
        }
        var sendAtIso = ""
        if (later) {
            val sendAt = parseEatInstant(dateText, timeText)
            if (sendAt == null) {
                feedback = Pair(false, "Type the date as YYYY-MM-DD and the time as HH:mm (Ethiopia time, 24-hour).")
                return null
            }
            if (sendAt.time <= System.currentTimeMillis() + 60_000L) {
                feedback = Pair(false, "Pick a time at least one minute from now (Ethiopia time).")
                return null
            }
            sendAtIso = utcIso(sendAt)
        }
        return SubmitPlan(later, sendAtIso, content, NotificationAuthoring.preview(contentBlocks))
    }

    fun run(plan: SubmitPlan) {
        scope.launch {
            sending = true
            if (plan.later) {
                val error = ComposerRepository.schedule(
                    editId = edit?.id,
                    title = title.trim(),
                    preview = plan.preview.ifBlank { title.trim() },
                    sendAtIso = plan.sendAtIso,
                    category = category,
                    content = plan.content,
                    url = pickedPath,
                    audiences = groups.toList()
                )
                sending = false
                if (error != null) {
                    feedback = Pair(false, error)
                } else {
                    val head = if (editing) "Changes saved. " else "Scheduled. "
                    onFinished(head + "It goes out at " + BroadcastsRepository.formatEat(plan.sendAtIso) + " (within a minute after that).")
                }
            } else {
                val reply = ComposerRepository.sendNow(
                    origin = origin,
                    title = title.trim(),
                    category = category,
                    content = plan.content,
                    url = pickedPath,
                    audiences = if (mode == "user") emptyList() else groups.toList(),
                    target = if (mode == "user") targetUser.trim() else null
                )
                sending = false
                val error = reply.error
                if (error != null) feedback = Pair(false, error) else onFinished(reply.value ?: "Sent.")
            }
        }
    }

    fun submit() {
        val plan = validate() ?: return
        if (!plan.later && mode == "groups") {
            pendingPlan = plan
        } else {
            run(plan)
        }
    }

    val plan0 = pendingPlan
    if (plan0 != null) {
        val who = if (groups.isEmpty()) {
            "EVERY student"
        } else if (groupCount == null) {
            "the selected groups"
        } else {
            "$groupCount student(s)"
        }
        AdminConfirmDialog(
            title = "Send now",
            text = "Send this to $who now? This cannot be undone.",
            confirmLabel = "Send",
            confirmTone = WisdomAccentAmber,
            onConfirm = {
                pendingPlan = null
                run(plan0)
            },
            onDismiss = { pendingPlan = null }
        )
    }
    val deleteTarget = deleteAudience
    if (deleteTarget != null) {
        AdminConfirmDialog(
            title = "Delete audience",
            text = "Delete the audience \"${deleteTarget.label}\"?",
            confirmLabel = "Delete",
            confirmTone = AdminRose,
            onConfirm = {
                deleteAudience = null
                scope.launch {
                    val error = ComposerRepository.deleteAudience(deleteTarget.id)
                    if (error != null) {
                        audError = error
                    } else {
                        groups.remove("custom:" + deleteTarget.id)
                        audReload += 1
                    }
                }
            },
            onDismiss = { deleteAudience = null }
        )
    }

    val previewBlocks = NotificationAuthoring.toBlocks(editorBlocks())
    val levelEntries: List<String> = run {
        val extra = ArrayList<String>()
        val c = counts
        if (c != null) {
            for (l in c.levels) {
                if (ComposerRepository.ACADEMIC_LEVELS.none { sameText(it, l.level) }) extra.add(l.level)
            }
        }
        ComposerRepository.ACADEMIC_LEVELS + extra
    }

    fun countFor(level: String): Int? {
        val c = counts ?: return null
        return c.levels.firstOrNull { sameText(it.level, level) }?.n ?: 0
    }

    fun toggleGroup(entry: String) {
        val existing = groups.firstOrNull { sameText(it, entry) }
        if (existing != null) groups.remove(existing) else groups.add(entry)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (editing) "Edit scheduled notification" else "New notification",
                    color = WisdomTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1 Category, 2 Write, 3 Destination, 4 Audience, 5 When, then send.",
                    color = WisdomMuted,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = WisdomMuted)
            }
        }

        val fb = feedback
        if (fb != null) {
            FeedbackBanner(ok = fb.first, message = fb.second)
        }

        // 1 Category
        SectionLabel("1. Category")
        for (cat in CATEGORIES) {
            CategoryCard(
                label = cat.label,
                hint = cat.hint,
                icon = cat.icon,
                selected = category == cat.id,
                onClick = { category = cat.id }
            )
        }

        // 2 Write
        SectionLabel("2. Write")
        Text(
            text = "Notification title (shown on the phone and in the list)",
            color = WisdomMuted,
            fontSize = 12.sp
        )
        AdminTextField(
            value = title,
            onValueChange = { title = it.take(140) },
            placeholder = "e.g. Exam schedule update"
        )
        Text(text = "Add a block:", color = WisdomMuted, fontSize = 12.sp)
        val types = NotificationAuthoring.BLOCK_TYPES
        for (rowStart in types.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminChoiceChip(
                    label = "+ " + types[rowStart].second,
                    selected = false,
                    onClick = { addBlock(types[rowStart].first) },
                    modifier = Modifier.weight(1f)
                )
                if (rowStart + 1 < types.size) {
                    AdminChoiceChip(
                        label = "+ " + types[rowStart + 1].second,
                        selected = false,
                        onClick = { addBlock(types[rowStart + 1].first) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            text = "Select text in a box, then press a button (or type **bold** and ==highlight== yourself):",
            color = WisdomMuted,
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminActionButton(
                label = "Bold",
                tone = WisdomTextPrimary,
                filled = false,
                enabled = true,
                onClick = { wrapSelection("**") },
                modifier = Modifier.weight(1f)
            )
            AdminActionButton(
                label = "Highlight",
                tone = Gold,
                filled = false,
                enabled = true,
                onClick = { wrapSelection("==") },
                modifier = Modifier.weight(1f)
            )
        }

        if (blocks.isEmpty()) {
            Text(
                text = "Nothing yet. Add a Title, then a Paragraph.",
                color = WisdomMuted,
                fontSize = 14.sp
            )
        }
        for (index in blocks.indices) {
            val block = blocks[index]
            key(block.id) {
                val shape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(WisdomCard)
                        .border(1.dp, WisdomBorderWhite, shape)
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = (BLOCK_LABEL[block.t] ?: block.t).uppercase(),
                            color = GoldSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val moved = blocks.removeAt(index)
                                    blocks.add(index - 1, moved)
                                }
                            },
                            enabled = index > 0
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = "Move up",
                                tint = WisdomMuted,
                                modifier = Modifier.alpha(if (index > 0) 1f else 0.3f)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index < blocks.size - 1) {
                                    val moved = blocks.removeAt(index)
                                    blocks.add(index + 1, moved)
                                }
                            },
                            enabled = index < blocks.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = "Move down",
                                tint = WisdomMuted,
                                modifier = Modifier.alpha(if (index < blocks.size - 1) 1f else 0.3f)
                            )
                        }
                        IconButton(
                            onClick = {
                                blocks.removeAt(index)
                                values.remove(block.id)
                                if (activeId == block.id) activeId = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete block",
                                tint = AdminRose
                            )
                        }
                    }
                    if (block.t == "divider") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp, top = 8.dp, bottom = 4.dp)
                                .height(1.dp)
                                .background(Gold.copy(alpha = 0.6f))
                        )
                    } else {
                        ComposerTextField(
                            value = values[block.id] ?: TextFieldValue(""),
                            onValueChange = { values[block.id] = it },
                            placeholder = if (block.t == "list") "One bullet per line" else "Write here",
                            minHeight = if (block.t == "title" || block.t == "subtitle") 44.dp else 88.dp,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .onFocusChanged { state ->
                                    if (state.isFocused) activeId = block.id
                                }
                        )
                    }
                }
            }
        }

        Text(text = "Live preview (reading view)", color = WisdomMuted, fontSize = 12.sp)
        ComposerPreview(blocks = previewBlocks)

        // 3 Destination
        SectionLabel("3. Destination")
        if (category == "admin") {
            Text(
                text = "None. A tap opens the full-screen reading view inside Notifications.",
                color = WisdomMuted,
                fontSize = 14.sp
            )
        } else if (category == "contest") {
            Text(
                text = "Automatic: a tap opens the Contest (trophy) screen.",
                color = WisdomMuted,
                fontSize = 14.sp
            )
        } else {
            val opens = if (pickedPath != null) {
                crumbs.joinToString(" \u203A ") + " (" + pickedPath + ")"
            } else {
                "nothing picked yet (choose at least a subject or course)"
            }
            Text(
                text = "This opens: $opens",
                color = if (pickedPath != null) WisdomAccentAmber else WisdomMuted,
                fontSize = 13.sp
            )
            if (trail.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminActionButton(
                        label = "Up one level",
                        tone = WisdomMuted,
                        filled = false,
                        enabled = true,
                        onClick = { trail.removeAt(trail.size - 1) },
                        modifier = Modifier.weight(1f)
                    )
                    AdminActionButton(
                        label = "Clear",
                        tone = WisdomMuted,
                        filled = false,
                        enabled = true,
                        onClick = { trail.clear() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            val options = NotificationPaths.nodesAfter(trailList)
            if (options.isEmpty() && trail.isNotEmpty()) {
                Text(text = "Nothing deeper to pick.", color = WisdomMuted, fontSize = 13.sp)
            }
            for (node in options) {
                PathRow(
                    label = node.label,
                    deeper = node.children.isNotEmpty(),
                    onClick = { trail.add(node.id) }
                )
            }
        }

        // 4 Audience
        SectionLabel("4. Audience")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminChoiceChip(
                label = "Groups of students",
                selected = mode == "groups",
                onClick = { mode = "groups" },
                modifier = Modifier.weight(1f)
            )
            if (!later) {
                AdminChoiceChip(
                    label = "One student (user id)",
                    selected = mode == "user",
                    onClick = { mode = "user" },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (mode == "groups" || later) {
            val labels = ArrayList<String>()
            val flags = ArrayList<Boolean>()
            val values2 = ArrayList<String?>()
            labels.add(if (counts != null) "Everyone (${counts?.total ?: 0})" else "Everyone")
            flags.add(groups.isEmpty())
            values2.add(null)
            for (level in levelEntries) {
                val n = countFor(level)
                labels.add(if (n != null) "$level ($n)" else level)
                flags.add(groups.any { sameText(it, level) })
                values2.add(level)
            }
            for (c in customs) {
                labels.add(c.label + " (" + c.students + ")")
                flags.add(groups.contains("custom:" + c.id))
                values2.add("custom:" + c.id)
            }
            for (rowStart in labels.indices step 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminChoiceChip(
                        label = labels[rowStart],
                        selected = flags[rowStart],
                        onClick = {
                            val v = values2[rowStart]
                            if (v == null) groups.clear() else toggleGroup(v)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (rowStart + 1 < labels.size) {
                        AdminChoiceChip(
                            label = labels[rowStart + 1],
                            selected = flags[rowStart + 1],
                            onClick = {
                                val v = values2[rowStart + 1]
                                if (v == null) groups.clear() else toggleGroup(v)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            val reach = if (groupCount == null) "..." else "$groupCount student" + (if (groupCount == 1) "" else "s")
            Text(
                text = (if (groups.isEmpty()) "Reaches everyone: " else "Reaches: ") + reach,
                color = WisdomTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            val c = counts
            if (c != null && c.noLevel > 0) {
                Text(
                    text = "${c.noLevel} have no level saved: only Everyone reaches them.",
                    color = WisdomMuted,
                    fontSize = 12.sp
                )
            }
            for (custom in customs) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Custom audience: " + custom.label,
                        color = WisdomMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { deleteAudience = custom }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete audience " + custom.label,
                            tint = AdminRose
                        )
                    }
                }
            }
            AdminActionButton(
                label = if (adding) "Close custom audience form" else "+ Custom audience",
                tone = WisdomMuted,
                filled = false,
                enabled = true,
                onClick = { adding = !adding },
                modifier = Modifier.fillMaxWidth()
            )
            if (adding) {
                val shape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(WisdomCard)
                        .border(1.dp, WisdomBorderWhite, shape)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it.take(60) },
                        placeholder = "Audience name, e.g. Exit exam group"
                    )
                    Text(text = "Matches these levels:", color = WisdomMuted, fontSize = 12.sp)
                    for (rowStart in levelEntries.indices step 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminChoiceChip(
                                label = levelEntries[rowStart],
                                selected = newLevels.any { sameText(it, levelEntries[rowStart]) },
                                onClick = {
                                    val level = levelEntries[rowStart]
                                    val existing = newLevels.firstOrNull { sameText(it, level) }
                                    if (existing != null) newLevels.remove(existing) else newLevels.add(level)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (rowStart + 1 < levelEntries.size) {
                                AdminChoiceChip(
                                    label = levelEntries[rowStart + 1],
                                    selected = newLevels.any { sameText(it, levelEntries[rowStart + 1]) },
                                    onClick = {
                                        val level = levelEntries[rowStart + 1]
                                        val existing = newLevels.firstOrNull { sameText(it, level) }
                                        if (existing != null) newLevels.remove(existing) else newLevels.add(level)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    AdminTextField(
                        value = newPackages,
                        onValueChange = { newPackages = it },
                        placeholder = "Optional: package ids separated by commas"
                    )
                    val canSave = !audBusy && newLabel.isNotBlank() && (newLevels.isNotEmpty() || newPackages.isNotBlank())
                    AdminActionButton(
                        label = if (audBusy) "Saving..." else "Save audience",
                        tone = WisdomAccentAmber,
                        filled = false,
                        enabled = canSave,
                        onClick = {
                            scope.launch {
                                audBusy = true
                                val packages = newPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val error = ComposerRepository.addAudience(newLabel, newLevels.toList(), packages)
                                audBusy = false
                                if (error != null) {
                                    audError = error
                                } else {
                                    adding = false
                                    newLabel = ""
                                    newLevels.clear()
                                    newPackages = ""
                                    audReload += 1
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            AdminTextField(
                value = targetUser,
                onValueChange = { targetUser = it },
                placeholder = "Student user id (UUID)"
            )
        }
        val audMessage = audError
        if (audMessage != null) {
            Text(text = audMessage, color = AdminRose, fontSize = 12.sp)
        }

        // 5 When
        SectionLabel("5. When")
        if (!editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminChoiceChip(
                    label = "Send now",
                    selected = whenMode == "now",
                    onClick = { whenMode = "now" },
                    modifier = Modifier.weight(1f)
                )
                AdminChoiceChip(
                    label = "Schedule for later",
                    selected = whenMode == "later",
                    onClick = {
                        whenMode = "later"
                        mode = "groups"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (later) {
            Text(text = "Send at (Ethiopia time, 24-hour)", color = WisdomMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(1f)
                )
                AdminTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    placeholder = "HH:mm",
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Checked every minute. Whatever the time zone of this phone, the time typed here is Ethiopia time.",
                color = WisdomMuted,
                fontSize = 12.sp
            )
        }

        ComposerPrimaryButton(
            label = if (sending) "Working..." else if (editing) "Save changes" else if (later) "Schedule" else "Send now",
            enabled = !sending,
            onClick = { submit() }
        )
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = WisdomTextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun FeedbackBanner(ok: Boolean, message: String) {
    val tone = if (ok) WisdomAccentEmerald else AdminRose
    val shape = RoundedCornerShape(14.dp)
    Text(
        text = message,
        color = tone,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.4f), shape)
            .padding(12.dp)
    )
}

@Composable
private fun CategoryCard(
    label: String,
    hint: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val tone = if (selected) WisdomAccentAmber else WisdomMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) WisdomAccentAmber.copy(alpha = 0.12f) else WisdomCard)
            .border(1.dp, if (selected) WisdomAccentAmber.copy(alpha = 0.6f) else WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tone)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = WisdomTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = hint, color = WisdomMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PathRow(label: String, deeper: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinHeight44()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (deeper) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "\u203A", color = WisdomMuted, fontSize = 18.sp)
        }
    }
}

private fun Modifier.defaultMinHeight44(): Modifier = this.then(Modifier.height(IntrinsicSize.Min))

@Composable
private fun ComposerTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    minHeight: Dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = WisdomTextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(WisdomCyan),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(shape)
                    .background(WisdomCard)
                    .border(1.dp, WisdomBorderWhite, shape)
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Spacer(modifier = Modifier.height(minHeight - 24.dp))
                if (value.text.isEmpty()) {
                    Text(text = placeholder, color = WisdomMuted, fontSize = 14.sp, maxLines = 2)
                }
                inner()
            }
        }
    )
}

@Composable
private fun ComposerPrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(WisdomAccentAmber)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = DarkOnGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private fun runsAnnotated(runs: List<ContentRun>): AnnotatedString = buildAnnotatedString {
    for (r in runs) {
        val style = if (r.hl) {
            SpanStyle(
                background = Color(0xE6FCD34D),
                color = DarkOnGold,
                fontWeight = if (r.b) FontWeight.ExtraBold else FontWeight.SemiBold
            )
        } else if (r.b) {
            SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)
        } else {
            SpanStyle()
        }
        withStyle(style) { append(r.text) }
    }
}

/** The reading view as the student will see it: gold title, soft subtitle, light body, callout with a gold bar. */
@Composable
private fun ComposerPreview(blocks: List<ContentBlock>) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PreviewBg)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (blocks.isEmpty()) {
            Text(text = "The preview appears as you type.", color = Color(0xFF94A3B8), fontSize = 14.sp)
        }
        for (block in blocks) {
            when (block) {
                is ContentBlock.Title -> Text(
                    text = block.text,
                    color = Gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                is ContentBlock.Subtitle -> Text(
                    text = block.text,
                    color = GoldSoft,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                is ContentBlock.Para -> Text(
                    text = runsAnnotated(block.runs),
                    color = BodyText,
                    fontSize = 14.sp
                )
                is ContentBlock.Callout -> {
                    val calloutShape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clip(calloutShape)
                            .background(Color(0x0FFFFFFF))
                            .border(1.dp, Color(0x1FFFFFFF), calloutShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(Gold)
                        )
                        Text(
                            text = runsAnnotated(block.runs),
                            color = BodyText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is ContentBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (item in block.items) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "\u2022", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = runsAnnotated(item),
                                color = BodyText,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                is ContentBlock.Divider -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .height(1.dp)
                        .background(Gold.copy(alpha = 0.6f))
                )
            }
        }
    }
}
