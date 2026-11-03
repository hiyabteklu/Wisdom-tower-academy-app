package com.wisdomtower.academy.ui.study

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import com.wisdomtower.academy.ui.theme.wisdomDialogEnter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.wisdomtower.academy.data.competition.ExamLockdown
import com.wisdomtower.academy.data.content.AiTutorRepository
import com.wisdomtower.academy.data.content.TutorEvent
import com.wisdomtower.academy.data.content.TutorMessage
import com.wisdomtower.academy.data.content.TutorSession
import com.wisdomtower.academy.data.content.TutorSessionStore
import com.wisdomtower.academy.ui.theme.AppBackdrop
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// Texts copied from website-reference/src/components/learning/AiTutor.tsx
private const val CALM_BUSY =
    "The Wisdom Tower AI Tutor is currently experiencing high demand. Please wait a moment and try asking your question again."
private const val STOPPED = "Response stopped. What would you like to explore instead?"
private const val CUT_NOTICE = "\n\n*(Response stopped due to high server demand. You can ask to continue.)*"

// ---- D6j day / night colours. Night value = the exact old literal; day value = Ivory & Navy token. Getters: read while composing. ----
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

/** Glass tints of the floating controls. Night: translucent so the chat shows through. Day: ivory at about 92 percent (overlay rule). */
private val FloatFill: Color get() = dayNight(Color(0x66050A14), Color(0xEBF8F5EE))
private val FloatEdge: Color get() = dayNight(Color(0x26FFFFFF), WisdomGlassBorder)
private val TuCyanText: Color get() = dayNight(Color(0xFFA5F3FC), WisdomCyan)
private val TuSlate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomMuted)
private val TuSlate200: Color get() = dayNight(Color(0xFFE2E8F0), WisdomInk)
private val TuStopInk: Color get() = dayNight(Color(0xFFFDE68A), WisdomInk)
private val TuRose: Color get() = dayNight(Color(0xFFFDA4AF), WisdomAccentRose)
private val TuRoseEdge: Color get() = dayNight(Color(0xFFF43F5E).copy(alpha = 0.25f), WisdomAccentRose.copy(alpha = 0.40f))
private val TuOnCyanInk: Color get() = dayNight(Color(0xFF020617), Color.White)
private val TuDialogFill: Color get() = dayNight(Color(0xF20E172A), Color(0xF2F8F5EE))

/** Marks a failed stream (thrown inside the collector, caught by the sender). */
private class TutorStreamFailure : Exception()

/** State of the running request that must not trigger recomposition. [token] changes on every send and abort. */
private class TutorStreamHolder {
    var job: Job? = null
    var token = 0
    var received = false
    var sessionId: String? = null
    var lastPush = 0L
}

/**
 * Native AI tutor (plan step N10, port of website-reference/src/components/learning/AiTutor.tsx), opened from the
 * "AI Tutor" tile of the Learning tab instead of the website pop-up.
 *  - Chats: several saved chats on this phone ([TutorSessionStore]), "Chats" list, "New", clear, delete, "Clear All
 *    Past Chats", chat titled from the first question (36 characters), like the website.
 *  - Answers: streamed from the website route through [AiTutorRepository]; the message list is drawn by the local
 *    page `assets/notes/tutor.html` with the shared KaTeX renderer (math, tables, ==highlights== as in the notes).
 *  - Texts (welcome, prompts, placeholders, error and stop notices, follow-up chips) copied from `AiTutor.tsx`.
 *  - Chats list (U2 change 5): opens as a side panel from the right over the chat (dimmed area closes it); the chat
 *    page stays alive underneath, so nothing reloads when the list is opened or closed.
 * Differences from the website: no avatar picture on the welcome screen and answers.
 *
 * T1 (2026-11-02, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md, owner: an AI tutor at the end of every note with the context of
 * that chapter): when the tutor is opened by the "Ask AI tutor" button of the notes reader, [NativeToolRequests] holds the
 * chapter text (and a short instruction). This screen takes it ONCE when it opens ([NativeToolRequests.takeTutorContext]),
 * starts a NEW empty chat for it (the older chats stay in the list) and sends it to the website route as `courseContext`
 * with every question of this visit, instead of the course title the shell passes ([courseContext]). The route appends it
 * to the AI's system instruction, so the student does not have to explain what they are reading. Opened from the drawer
 * or the Learning tab, there is no chapter text and nothing changes. If the student opens an OLDER chat from the list
 * during the same visit, the chapter text is still sent with its questions.
 *
 * Scroll freeze fix (owner report 2026-10-13): this screen calls [PauseShellWhileShown], which gives the local
 * WebView page the touch fix and quiets the hidden website WebView.
 *
 * C10 (owner decision 2026-10-15): while a RANKED competition exam is open (ExamLockdown.active) this screen draws
 * nothing, shows a short notice and calls onBack. The calculator is a different screen and is never blocked.
 *
 * Background (plan B1, owner request 2026-10-17): the picture that was added on 2026-10-16 is gone. The screen draws the
 * shared solid app colour ([AtmosphereBackground]) and the chat WebView uses the same colour ([AppBackdrop]).
 *
 * FULL-SCREEN LAYOUT (owner request 2026-10-17: "double header and footers make the usable screen small"): there is no
 * toolbar row and no composer row any more. The chat page fills the whole area; the controls float on top of it:
 *  - top left: ONE small translucent menu bar holding Back, Chats (with the count), New and Clear. No title text.
 *  - bottom: a slim translucent message pill (44 dp high, grows to 4 lines) with a round Send / Stop button.
 * The page gets matching top and bottom padding through JavaScript (see [applyInsets]) so the first and the last
 * message are never hidden under the controls, but everything scrolls underneath them. `tutor.html` is unchanged.
 * The app's own header and bottom bar belong to the shell (MainActivity) and are not touched here.
 *
 * D6j (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode for the NATIVE parts of this screen only. Night keeps the exact old
 * literals as `dayNight` pairs. By day: the floating menu bar and message pill are ivory at about 92 percent (they float
 * over scrolling text, so they must be nearly opaque); icons, the typed text and titles are ink; the "cyan" light texts
 * (A5F3FC) are the deep teal; the Send button's arrow is white on the teal (teal outline when the box is empty); Stop
 * is the amber tint with ink text; the chats side panel and the "Clear this chat?" dialog are ivory (95 percent); delete
 * and clear-all use the day rose. NOT done here: the chat page itself (`assets/notes/tutor.html`: bubbles, text, math,
 * code) is a local WebView page and belongs to D8. Until D8 the page keeps its night colours by day, so the messages may
 * be hard to read by day; test the controls in this slice, the messages after D8.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun NativeTutorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    courseContext: String? = null
) {
    // T1: the chapter text of the note the student came from (taken once, so it is forgotten afterwards). Read BEFORE the
    // C10 gate below so a blocked open cannot leave an old chapter behind for the next open.
    val chapterContext = remember { NativeToolRequests.takeTutorContext() }
    val effectiveContext = chapterContext ?: courseContext

    // C10 gate (owner decision 2026-10-15): no tutor while a ranked competition exam is open.
    if (ExamLockdown.active) {
        val gateContext = LocalContext.current
        val gateBack by rememberUpdatedState(onBack)
        LaunchedEffect(Unit) {
            // The wording is the agent's (the owner only decided that the tutor is prohibited); change it if wanted.
            Toast.makeText(gateContext, "AI Tutor is switched off while a ranked competition exam is open.", Toast.LENGTH_SHORT).show()
            gateBack()
        }
        return
    }

    // Same touch fix + quiet background as the notes, flashcards and quiz screens (see ShellPause.kt).
    PauseShellWhileShown()

    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val initial = remember {
        val loaded = TutorSessionStore.load(context)
        if (chapterContext != null) {
            // T1: a fresh chat for this chapter on top; the older chats stay in the list.
            val fresh = TutorSessionStore.newSession()
            (listOf(fresh) + (loaded?.first ?: emptyList())) to fresh.id
        } else {
            loaded ?: TutorSessionStore.newSession().let { listOf(it) to it.id }
        }
    }
    var sessions by remember { mutableStateOf(initial.first) }
    var activeId by remember { mutableStateOf(initial.second) }
    var showHistory by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isStreaming by remember { mutableStateOf(false) }
    var pageReady by remember { mutableStateOf(false) }
    var composerPx by remember { mutableStateOf(0) }
    val holder = remember { TutorStreamHolder() }
    val webRef = remember { arrayOfNulls<WebView>(1) }
    val latestSessions by rememberUpdatedState(sessions)
    val latestActive by rememberUpdatedState(activeId)

    val activeSession = sessions.firstOrNull { it.id == activeId } ?: sessions.first()
    val busy = isThinking || isStreaming

    // ---- chat helpers (ported from AiTutor.tsx) ----

    fun updateSession(id: String, transform: (List<TutorMessage>) -> List<TutorMessage>) {
        sessions = sessions.map { s ->
            if (s.id != id) {
                s
            } else {
                val next = transform(s.messages)
                var title = s.title
                if (title == "New chat" || title == "New Study Session") {
                    val firstUser = next.firstOrNull { it.role == "user" }
                    if (firstUser != null && firstUser.content.isNotBlank()) {
                        val cleaned = firstUser.content.replace(Regex("\\s+"), " ").trim()
                        title = if (cleaned.length > 36) cleaned.take(36).trim() + "…" else cleaned
                    }
                }
                s.copy(title = title, messages = next, updatedAt = System.currentTimeMillis())
            }
        }
    }

    fun abortStream() {
        holder.token += 1
        holder.job?.cancel()
        holder.job = null
        isThinking = false
        isStreaming = false
    }

    fun send(textOverride: String?) {
        val q = (textOverride ?: input).trim()
        if (q.isEmpty() || isThinking || isStreaming) return

        val sid = activeId
        val userMsg = TutorMessage("user-" + System.currentTimeMillis(), "user", q, timeLabel())
        val before = sessions.firstOrNull { it.id == sid }?.messages ?: emptyList()
        val history = (before + userMsg).map { it.role to it.content }
        updateSession(sid) { it + userMsg }
        input = ""
        isThinking = true
        isStreaming = false

        holder.token += 1
        val token = holder.token
        holder.received = false
        holder.sessionId = sid
        val botId = "bot-" + System.currentTimeMillis()
        val botTime = timeLabel()

        holder.job = scope.launch {
            var acc = ""
            try {
                AiTutorRepository.stream(history, effectiveContext).collect { ev ->
                    if (holder.token == token) {
                        when (ev) {
                            is TutorEvent.Chunk -> {
                                if (!holder.received) {
                                    holder.received = true
                                    isThinking = false
                                    isStreaming = true
                                    acc = ev.text
                                    updateSession(sid) { it + TutorMessage(botId, "assistant", acc, botTime) }
                                } else {
                                    acc += ev.text
                                    updateSession(sid) { list ->
                                        list.map { if (it.id == botId) it.copy(content = acc) else it }
                                    }
                                }
                            }

                            is TutorEvent.Suggestions -> {
                                updateSession(sid) { list ->
                                    list.map { if (it.id == botId) it.copy(suggestions = ev.items) else it }
                                }
                            }

                            TutorEvent.Done -> {
                                // nothing to do: the answer is complete
                            }

                            TutorEvent.Failed -> throw TutorStreamFailure()
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (holder.token == token) {
                    if (!holder.received) {
                        updateSession(sid) {
                            it + TutorMessage(
                                "err-" + System.currentTimeMillis(),
                                "assistant",
                                CALM_BUSY,
                                botTime,
                                listOf("Try asking again", "Explain in simpler terms", "Give me an example")
                            )
                        }
                    } else {
                        updateSession(sid) { list ->
                            list.map {
                                if (it.id == botId) {
                                    it.copy(
                                        content = it.content + CUT_NOTICE,
                                        suggestions = listOf("Please continue where you left off", "Summarize this topic")
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                    }
                }
            } finally {
                if (holder.token == token) {
                    isThinking = false
                    isStreaming = false
                    holder.job = null
                }
            }
        }
    }

    fun stop() {
        val sid = holder.sessionId
        val hadText = holder.received
        abortStream()
        if (!hadText && sid != null) {
            updateSession(sid) {
                it + TutorMessage(
                    "bot-" + System.currentTimeMillis(),
                    "assistant",
                    STOPPED,
                    timeLabel(),
                    listOf("Give me a practice problem", "Explain the formula step-by-step", "What are common exam pitfalls?")
                )
            }
        }
    }

    fun newChat() {
        abortStream()
        val fresh = TutorSessionStore.newSession()
        sessions = listOf(fresh) + sessions
        activeId = fresh.id
        input = ""
        showHistory = false
    }

    fun selectSession(id: String) {
        if (id == activeId) {
            showHistory = false
            return
        }
        abortStream()
        activeId = id
        input = ""
        showHistory = false
    }

    fun deleteSession(id: String) {
        if (id == activeId) abortStream()
        val filtered = sessions.filter { it.id != id }
        if (filtered.isEmpty()) {
            val fresh = TutorSessionStore.newSession()
            sessions = listOf(fresh)
            activeId = fresh.id
        } else {
            sessions = filtered
            if (id == activeId) activeId = filtered[0].id
        }
    }

    fun clearAll() {
        abortStream()
        val fresh = TutorSessionStore.newSession()
        sessions = listOf(fresh)
        activeId = fresh.id
        showHistory = false
        input = ""
    }

    fun clearChat() {
        if (isThinking || isStreaming) abortStream()
        updateSession(activeId) { emptyList() }
    }

    // ---- bridge from the message page ----

    val bridge = remember {
        TutorBridge(
            onSend = { text -> scope.launch { send(text) }; Unit },
            onCopy = { text ->
                scope.launch {
                    try {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("AI Tutor", text))
                    } catch (e: Exception) {
                        // copying is best effort
                    }
                }
                Unit
            }
        )
    }

    // ---- save the chats (debounced) and when the screen is left ----

    LaunchedEffect(sessions, activeId) {
        delay(250)
        TutorSessionStore.save(context, sessions, activeId)
    }
    DisposableEffect(Unit) {
        onDispose {
            holder.token += 1
            holder.job?.cancel()
            TutorSessionStore.save(context, latestSessions, latestActive)
        }
    }

    // ---- push the messages into the page (at most about every 80 ms while streaming) ----

    LaunchedEffect(pageReady, activeSession.id, activeSession.messages, isThinking, isStreaming) {
        if (pageReady) {
            val wait = holder.lastPush + 80 - SystemClock.elapsedRealtime()
            if (wait > 0) delay(wait)
            holder.lastPush = SystemClock.elapsedRealtime()
            val json = pageJson(activeSession.id, activeSession.messages, isThinking, isStreaming)
            webRef[0]?.evaluateJavascript("window.wtTutorSet(${JSONObject.quote(json)});", null)
        }
    }

    // ---- keep the first and last message clear of the floating controls (the page itself fills the screen) ----

    LaunchedEffect(pageReady, composerPx) {
        if (pageReady) {
            val bottomDp = with(density) { composerPx.toDp().value }
            applyInsets(webRef[0], topDp = TOP_INSET_DP, bottomDp = bottomDp)
        }
    }

    BackHandler(enabled = showHistory) { showHistory = false }
    BackHandler(enabled = !showHistory) { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        // Chat page fills everything; the controls float on top of it.
        Box(modifier = Modifier.fillMaxSize().imePadding()) {
            TutorWebView(
                bridge = bridge,
                webRef = webRef,
                onReady = { pageReady = true },
                onGone = { pageReady = false },
                modifier = Modifier.fillMaxSize()
            )

            // Top left: one small translucent menu bar with every control inside, nothing else.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(FloatFill)
                    .border(BorderStroke(1.dp, FloatEdge), RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MenuIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back") { onBack() }
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(if (showHistory) WisdomCyan.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable { showHistory = !showHistory }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Chats",
                        tint = if (showHistory) TuCyanText else WisdomInk,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${sessions.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showHistory) TuCyanText else WisdomInk
                    )
                }
                MenuIcon(Icons.Default.Add, "New chat", TuCyanText) { newChat() }
                MenuIcon(Icons.Default.Delete, "Clear chat", TuSlate300) { showClearConfirm = true }
            }

            // Bottom: slim translucent message pill + round send / stop button, floating over the chat.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { composerPx = it.height }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(FloatFill)
                        .border(BorderStroke(1.dp, FloatEdge), RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (input.isEmpty()) {
                        Text(
                            text = if (busy) "AI Tutor is responding…" else "Ask a concept, formula, or problem…",
                            fontSize = 14.sp,
                            color = WisdomMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = WisdomInk, fontSize = 14.sp, lineHeight = 20.sp),
                        cursorBrush = SolidColor(WisdomCyan),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send(null) })
                    )
                }
                if (busy) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(WisdomAccentAmber.copy(alpha = 0.3f))
                            .border(BorderStroke(1.dp, WisdomAccentAmber.copy(alpha = 0.45f)), RoundedCornerShape(22.dp))
                            .clickable { stop() }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Stop", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TuStopInk)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WisdomCyan.copy(alpha = if (input.isBlank()) 0.22f else 0.65f))
                            .border(BorderStroke(1.dp, FloatEdge), CircleShape)
                            .clickable { send(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send question",
                            tint = if (AppThemeMode.isDark || input.isNotBlank()) Color.White else WisdomCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Chats side panel (U2 change 5): dimmed area closes it, the panel sits on the right over the chat
        if (showHistory) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66030812))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showHistory = false }
            )
            val panelBrush = if (AppThemeMode.isDark) {
                Brush.verticalGradient(listOf(Color(0x66152238), Color(0x660D1829)))
            } else {
                Brush.verticalGradient(listOf(Color(0xF5F8F5EE), Color(0xF5EFEADF)))
            }
            HistoryPanel(
                sessions = sessions,
                activeId = activeId,
                onClose = { showHistory = false },
                onNew = { newChat() },
                onSelect = { selectSession(it) },
                onDelete = { deleteSession(it) },
                onClearAll = { clearAll() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .background(panelBrush)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .padding(top = 8.dp)
            )
        }

        if (showClearConfirm) {
            AlertDialog(
                modifier = Modifier.wisdomDialogEnter(),
                onDismissRequest = { showClearConfirm = false },
                containerColor = TuDialogFill,
                titleContentColor = WisdomInk,
                textContentColor = TuSlate300,
                title = { Text("Clear this chat?", fontWeight = FontWeight.Bold) },
                text = { Text("All messages in this chat will be removed.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearConfirm = false
                        clearChat()
                    }) {
                        Text("Clear", color = TuRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel", color = TuCyanText)
                    }
                }
            )
        }
    }
}

/** Height reserved at the top of the chat page for the floating menu bar (38 dp buttons + paddings + a little air). */
private const val TOP_INSET_DP = 60f

/**
 * Sets the padding of the page's `#root` (top = floating menu bar, bottom = floating message pill) and lifts the round
 * "jump to latest" arrow above the pill. CSS pixels in the WebView equal dp. `tutor.html` itself is not edited.
 */
private fun applyInsets(web: WebView?, topDp: Float, bottomDp: Float) {
    if (web == null) return
    val bottom = bottomDp + 6f
    web.evaluateJavascript(
        "(function(){var r=document.getElementById('root');if(r){r.style.paddingTop='" + topDp.toInt() +
            "px';r.style.paddingBottom='" + bottom.toInt() + "px';}" +
            "var j=document.getElementById('jump');if(j){j.style.bottom='" + (bottom + 8f).toInt() + "px';}})();",
        null
    )
}

/** Small round icon button of the floating menu bar. */
@Composable
private fun MenuIcon(icon: ImageVector, description: String, tint: Color = WisdomInk, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Receives the messages of `notes/tutor.html` (`window.WTBridge.*`), on a WebView thread. */
private class TutorBridge(
    private val onSend: (String) -> Unit,
    private val onCopy: (String) -> Unit
) {
    @JavascriptInterface
    fun onSend(text: String) {
        onSend.invoke(text)
    }

    @JavascriptInterface
    fun onCopy(text: String) {
        onCopy.invoke(text)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun TutorWebView(
    bridge: Any,
    webRef: Array<WebView?>,
    onReady: () -> Unit,
    onGone: () -> Unit,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AppBackdrop.webViewColor(ctx))
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.blockNetworkLoads = true
                settings.domStorageEnabled = false
                addJavascriptInterface(bridge, "WTBridge")
                webViewClient = object : WebViewClient() {
                    // BG-LOADING: hide the page until the glass script has run, the native backdrop shows meanwhile
                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                        AppBackdrop.hidePageUntilReady(view)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        AppBackdrop.clearPageBackground(view)
                        onReady()
                    }

                    // The tutor page never navigates away; ignore every link tap.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                }
                webRef[0] = this
                loadUrl("file:///android_asset/notes/tutor.html")
            }
        },
        onRelease = {
            it.removeJavascriptInterface("WTBridge")
            if (webRef[0] === it) webRef[0] = null
            it.destroy()
            onGone()
        }
    )
}

/** The list of saved chats ("Study Chats" of the website's side sheet). */
@Composable
private fun HistoryPanel(
    sessions: List<TutorSession>,
    activeId: String,
    onClose: () -> Unit,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Study Chats", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(WisdomCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${sessions.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TuCyanText)
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close history",
                    tint = WisdomMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(WisdomCyan)
                .clickable { onNew() }
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("+ Start New Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TuOnCyanInk)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sessions, key = { it.id }) { s ->
                val isActive = s.id == activeId
                val questions = s.messages.count { it.role == "user" }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) WisdomCyan.copy(alpha = 0.15f) else WisdomGlassFill)
                        .border(
                            BorderStroke(1.dp, if (isActive) WisdomCyan.copy(alpha = 0.4f) else WisdomGlassBorder),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelect(s.id) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) TuCyanText else TuSlate200,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = formatSessionTime(s.updatedAt) + " • " + questions + (if (questions == 1) " question" else " questions") +
                                (if (isActive) " • Active" else ""),
                            fontSize = 12.sp,
                            color = WisdomMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDelete(s.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete chat session",
                            tint = WisdomMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (sessions.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, TuRoseEdge), RoundedCornerShape(12.dp))
                    .clickable { onClearAll() }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Clear All Past Chats", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TuRose)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private fun timeLabel(): String =
    java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date())

/** Same wording as the website's `formatSessionTime`. */
private fun formatSessionTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    if (minutes < 1) return "Just now"
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    if (days == 1L) return "Yesterday"
    if (days < 7) return "${days}d ago"
    return java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}

/** The JSON that `window.wtTutorSet` in `notes/tutor.html` reads. */
private fun pageJson(sessionId: String, messages: List<TutorMessage>, thinking: Boolean, streaming: Boolean): String {
    val arr = JSONArray()
    messages.forEach { m ->
        arr.put(
            JSONObject()
                .put("id", m.id)
                .put("role", m.role)
                .put("content", m.content)
                .put("timestamp", m.timestamp)
                .put("suggestions", JSONArray(m.suggestions))
        )
    }
    return JSONObject()
        .put("sid", sessionId)
        .put("busy", thinking || streaming)
        .put("thinking", thinking)
        .put("messages", arr)
        .toString()
}
