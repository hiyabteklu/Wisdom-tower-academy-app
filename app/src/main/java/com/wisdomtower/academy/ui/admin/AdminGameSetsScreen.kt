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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.ADMIN_CONTENT_TREE
import com.wisdomtower.academy.data.admin.AdminGameSetRow
import com.wisdomtower.academy.data.admin.AdminGameSetsListResult
import com.wisdomtower.academy.data.admin.AdminGameSetsRepository
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch

/**
 * Admin bulk paste for game_sets (docs/GAMES_HUB_PLAN.md step 7).
 */
@Composable
fun AdminGameSetsScreen(modifier: Modifier = Modifier) {
    var stack by remember { mutableStateOf(listOf(ADMIN_CONTENT_TREE)) }
    val currentNodes = stack.last()
    var selectedScope by remember { mutableStateOf<String?>(null) }
    var selectedLabel by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<AdminGameSetRow>>(emptyList()) }
    var listError by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<AdminGameSetRow?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload(scopePath: String) {
        scope.launch {
            when (val r = AdminGameSetsRepository.list(scopePath)) {
                is AdminGameSetsListResult.Success -> {
                    rows = r.rows
                    listError = null
                }
                is AdminGameSetsListResult.Failure -> {
                    rows = emptyList()
                    listError = r.message
                }
            }
        }
    }

    BackHandler(enabled = stack.size > 1 || selectedScope != null || editing != null || creating) {
        when {
            editing != null || creating -> {
                editing = null
                creating = false
            }
            selectedScope != null -> {
                selectedScope = null
                selectedLabel = ""
                rows = emptyList()
            }
            stack.size > 1 -> stack = stack.dropLast(1)
        }
    }

    when {
        editing != null || creating -> {
            GameSetEditor(
                existing = editing,
                scopePath = selectedScope ?: editing?.scopePath.orEmpty(),
                onCancel = { editing = null; creating = false },
                onSaved = {
                    editing = null
                    creating = false
                    selectedScope?.let { reload(it) }
                },
                modifier = modifier
            )
        }
        selectedScope != null -> {
            Column(modifier.fillMaxSize().padding(12.dp)) {
                Text(selectedLabel, color = WisdomTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(selectedScope!!, color = WisdomMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminActionButton("Add set", tone = WisdomCyan, filled = true, enabled = true, onClick = { creating = true })
                    AdminActionButton("Refresh", tone = WisdomCyan, filled = false, enabled = true, onClick = { reload(selectedScope!!) })
                }
                Spacer(Modifier.height(8.dp))
                listError?.let { Text(it, color = WisdomMuted, fontSize = 13.sp) }
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(rows, key = { it.id }) { row ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WisdomCard)
                                .border(1.dp, WisdomBorderWhite, RoundedCornerShape(12.dp))
                                .clickable { editing = row }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "${row.game} · ch ${row.chapter} · ${row.itemCount} items",
                                    color = WisdomTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    row.chapterTitle.ifBlank { row.title }.ifBlank { "—" } +
                                        if (row.published) " · published" else " · draft",
                                    color = WisdomMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            LaunchedEffect(selectedScope) { selectedScope?.let { reload(it) } }
        }
        else -> {
            Column(modifier.fillMaxSize().padding(12.dp)) {
                Text("Pick a subject for game sets", color = WisdomTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Match and Defense use dedicated game_sets rows, not Question Banks.",
                    color = WisdomMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentNodes, key = { it.id }) { node ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WisdomCard)
                                .border(1.dp, WisdomBorderWhite, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (node.children.isNotEmpty() && node.scopePath == null) {
                                        stack = stack + listOf(node.children)
                                    } else if (node.scopePath != null && node.children.any {
                                            it.id in setOf("books", "exams", "question-banks")
                                        }) {
                                        selectedScope = node.scopePath
                                        selectedLabel = node.label
                                    } else if (node.scopePath != null && node.children.isEmpty()) {
                                        selectedScope = node.scopePath
                                        selectedLabel = node.label
                                    } else if (node.children.isNotEmpty()) {
                                        stack = stack + listOf(node.children)
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            Text(node.label, color = WisdomTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameSetEditor(
    existing: AdminGameSetRow?,
    scopePath: String,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var game by remember { mutableStateOf(existing?.game ?: "match") }
    var chapter by remember { mutableStateOf(existing?.chapter?.toString() ?: "1") }
    var chapterTitle by remember { mutableStateOf(existing?.chapterTitle ?: "") }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var itemsText by remember {
        mutableStateOf(
            if (existing == null) {
                "[{\"a\":\"term\",\"b\":\"meaning\"}]"
            } else {
                "[]"
            }
        )
    }
    var published by remember { mutableStateOf(existing?.published ?: false) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize().padding(12.dp)) {
        Text(
            if (existing == null) "New game set" else "Edit game set",
            color = WisdomTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(scopePath, color = WisdomMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminChoiceChip("Match", selected = game == "match", onClick = { game = "match" })
            AdminChoiceChip("Defense", selected = game == "defense", onClick = { game = "defense" })
        }
        Spacer(Modifier.height(8.dp))
        AdminTextField(chapter, { chapter = it.filter { ch -> ch.isDigit() } }, "Chapter number", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(6.dp))
        AdminTextField(chapterTitle, { chapterTitle = it }, "Chapter title")
        Spacer(Modifier.height(6.dp))
        AdminTextField(title, { title = it }, "Set title (optional)")
        Spacer(Modifier.height(6.dp))
        Text("Items JSON (paste bulk array)", color = WisdomMuted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        AdminTextArea(
            value = itemsText,
            onValueChange = { itemsText = it },
            placeholder = "Paste JSON array…",
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.height(6.dp))
        AdminChoiceChip(
            if (published) "Published" else "Draft",
            selected = published,
            onClick = { published = !published }
        )
        status?.let {
            Text(it, color = WisdomCyan, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminActionButton("Cancel", tone = WisdomCyan, filled = false, enabled = true, onClick = onCancel)
            AdminActionButton(
                label = if (busy) "Saving…" else "Save",
                tone = WisdomCyan,
                filled = true,
                enabled = !busy,
                onClick = {
                    if (busy) return@AdminActionButton
                    val ch = chapter.toIntOrNull() ?: 0
                    busy = true
                    scope.launch {
                        val (ok, msg) = AdminGameSetsRepository.save(
                            existingId = existing?.id,
                            scopePath = scopePath,
                            game = game,
                            chapter = ch,
                            chapterTitle = chapterTitle,
                            title = title,
                            itemsText = itemsText,
                            published = published
                        )
                        status = msg
                        busy = false
                        if (ok) onSaved()
                    }
                }
            )
            if (existing != null) {
                AdminActionButton(
                    "Delete",
                    tone = Color(0xFFF43F5E),
                    filled = true,
                    enabled = true,
                    onClick = {
                        scope.launch {
                            val (ok, msg) = AdminGameSetsRepository.delete(existing.id)
                            status = msg
                            if (ok) onSaved()
                        }
                    }
                )
            }
        }
    }
}
