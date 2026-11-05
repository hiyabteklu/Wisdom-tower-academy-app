package com.wisdomtower.academy.ui.study

import android.annotation.SuppressLint
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import android.content.Context
import android.os.SystemClock
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.wisdomtower.academy.BrandSplashLoader
import com.wisdomtower.academy.data.content.AiTutorRepository
import com.wisdomtower.academy.data.content.ContentItemResult
import com.wisdomtower.academy.data.content.LearningContentRepository
import com.wisdomtower.academy.data.content.LearningProgressRepository
import com.wisdomtower.academy.data.content.TutorEvent
import com.wisdomtower.academy.ui.theme.AppBackdrop
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Phone-only copy of where the student stopped reading each note (N1), one float per note: 0 to 1 of the scroll height. */
private const val NOTES_STATE_PREFS = "notes_reader_state"

// Texts copied from NativeTutorScreen / website-reference AiTutor.tsx (the inline tutor on the note page uses the same route)
private const val NOTE_TUTOR_BUSY =
    "The Wisdom Tower AI Tutor is currently experiencing high demand. Please wait a moment and try asking your question again."
private const val NOTE_TUTOR_CUT = "\n\n*(Response stopped due to high server demand. You can ask to continue.)*"

/** Marks a failed tutor stream (thrown inside the collector, caught by the sender). */
private class NoteTutorFailure : Exception()

/**
 * Saves started from the notes page must survive the student leaving the screen, so they run in this app-wide
 * scope, not in the screen's own scope (that one is cancelled on leaving).
 */
private val notesSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** What the reader has reached so far, shared by the page bridge and the reading-time ticker. */
private class NoteProgress {
    @Volatile var pct: Int = 0
    @Volatile var active: Boolean = false
    @Volatile var lastFlush: Long = 0L
}

/** Adds the reading seconds since the last flush (and the current percent) to `learning_progress`. */
private fun flushNoteProgress(resourceId: String, progress: NoteProgress) {
    if (!progress.active) return
    val now = SystemClock.elapsedRealtime()
    val seconds = ((now - progress.lastFlush) / 1000L).toInt()
    if (seconds < 1) return
    progress.lastFlush = now
    val pct = progress.pct
    notesSaveScope.launch {
        LearningProgressRepository.saveProgress(
            resourceId = resourceId,
            progressPct = pct,
            addSeconds = seconds
        )
    }
}

/**
 * T1: the text the AI tutor gets as its chapter context (sent to the website route as `courseContext`, which appends it to
 * the tutor's instructions). It names the note, tells the tutor to stay on this chapter and to say so when a question goes
 * beyond it, then gives the note text. Only the note title and the note text are in it: nothing about the student.
 */
private fun buildTutorContext(title: String, noteText: String): String =
    "The student is studying the short note \"" + title.trim() + "\" right now. Answer their questions about THIS chapter first, " +
        "using the note text below as your source. If a question goes beyond the note, say so briefly and then answer from " +
        "your general knowledge.\n\nNOTE TEXT:\n" + noteText.trim()

/**
 * Native reader for one short note (plan step N6): loads the full row from Supabase
 * ([LearningContentRepository.getResource], offline copy kept) and shows its `body_md` in the LOCAL page
 * `assets/notes/index.html` (the website's RichContent + KaTeX dialect, no website page).
 * Reading progress (plan step N10, like the website's `NotesViewer` + `HubContentView`): the page reports the
 * scrolled percent with `WTBridge.onProgress({ pct })` (100 from 95 percent on), saved to
 * `learning_progress` (the higher percent is kept by the repository); the reading time is added every 30 seconds
 * and once more when the screen is left. Nothing is saved for a signed-out student (the repository returns false).
 *
 * AI tutor, INLINE and AUTO-OPEN (owner request 2026-11-04): a full-width tutor panel appears automatically after the note.
 * There is no Summary button and no tutor launch/toggle button. The page keeps the conversation and calls
 * `WTBridge.onTutorAsk({ id, messages })` for each question; this screen sends the history to [AiTutorRepository.stream]
 * with the chapter context ([buildTutorContext]: note title and first 14000 characters, no student personal data).
 * Answers stream back through `window.wtTutorSet` and follow-up prompts through `window.wtTutorSuggest`.
 * The page always keeps a fourth "Continue learning with me" action after its three recommended prompts.
 * Clear chat cancels a running answer and resets the current conversation after confirmation.
 * The screen reuses the existing tutor API; it adds no new endpoint, API key, cache or sign-in path.
 *
 * CHAT KEPT ON THE PHONE (owner report 2026-11-05: the tutor under a note was empty after closing and reopening the app):
 * the page reports the whole conversation with `WTBridge.onTutorState(json)` (a JSON array of role, content, suggestions
 * and a partial flag) and it is kept per note in [NOTES_STATE_PREFS], key `tutor_<resourceId>`, next to the reading
 * position. It is handed back as the third argument of `window.setNote(text, frac, chat)` when the note opens, so the
 * conversation is there exactly as it was left. An empty conversation removes the key. It never leaves the phone and is
 * separate from the chats list of the drawer tutor ([NativeTutorScreen]), which stays as it was.
 *
 * Reading position (N1, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md, 2026-11-02, owner: the book reader remembers the last
 * page, the notes reader must too): the page reports where the student is with `WTBridge.onPosition({ frac })`
 * (0 to 1 of the scroll height, a share so it still fits another screen width or text size). It is kept in the phone's
 * SharedPreferences [NOTES_STATE_PREFS], key `pos_<resourceId>`, like the PDF reader keeps `last_page_<id>`, so it also
 * works offline and for a signed-out student. On opening, the saved share is handed to the page with
 * `window.setNote(text, frac)`, which scrolls there once the page is laid out. Not synced to the server on purpose:
 * `learning_progress` has a `last_page` for books but nothing for a scroll share, and the plan says not to add a table.
 * A note edited since then simply opens at the same share (clamped); a saved 0 opens at the top.
 * FIX 2026-11-05 (owner: a note left at the end opened at the top the next day): the page no longer treats a share of 98
 * percent or more as "read to the end, open at the top". The tutor panel is part of the page now, so its bottom IS the end
 * of the page and that rule sent everyone who stopped there back to the top.
 *
 * Loading look (owner request 2026-10-13): while the note is fetched and while the page lays itself out (the maths
 * is typeset and the lines change length, so the page would visibly jump), the app-start loader ([BrandSplashLoader])
 * is shown in the middle; the page is revealed when it reports ready.
 * BG-NOTE-LOADER (2026-10-17, owner: the loader had a solid dark background, it must be transparent so the chosen
 * background shows): the cover used to be a dark fill that hid the page. Now the cover has NO fill. The page itself
 * is hidden instead, so the jumping is still hidden, and the loader floats on the chosen app background.
 * To go back: put `.background(Color(0x4D0A101D))` on the cover Box again.
 * BG-NOFADE (2026-10-23, owner: the background goes black the moment before the text is painted): the page's alpha
 * used to be animated (1 minus the cover alpha, 220 ms). The page is now either fully hidden or fully shown. Only the
 * loader (a plain Compose node) still fades out. To go back: `.alpha(1f - coverAlpha)` on the NoteWebView modifier.
 * BG-NOFLASH (2026-10-23, owner screenshots: loader, then a dark navy screen, then the text; BG-NOFADE did not fix it):
 * CAUSE (read from this file and `assets/notes/index.html`): the 1.2 s safety timer below sets `pageReady` even when the
 * page has not finished loading (the first WebView of a run can need more than a second to start). The page was then
 * shown with its OWN colour, `html, body { background: #0a101d }` in index.html, which is exactly the navy in the
 * screenshot; the transparent override of [AppBackdrop.clearPageBackground] only runs at onPageFinished, and the
 * WebView was only hidden from onPageStarted on. FIX, two parts: (1) the WebView is hidden from the moment it is
 * created (`alpha = 0f` in the factory, shown again by [AppBackdrop.clearPageBackground] or by a 4 s safety), so the
 * early reveal shows the app background; (2) index.html no longer paints a background of its own.
 * Build fix 2026-10-13: the cover used `AnimatedVisibility` inside a `Box` inside a `Column`, which the compiler
 * refuses; it is now a plain `animateFloatAsState` alpha.
 *
 * Clean reading (owner request 2026-10-13): NO top row (back arrow + note title) any more, the note uses the whole
 * screen under the app bar. Leaving is done with the phone Back button / gesture (handled by `StudyWorkspaceScreen`);
 * [onBack] and [title] are kept in the signature but not drawn.
 *
 * Default background picture (owner request 2026-10-17): the WebView is see-through and the page's own dark page colour
 * is cleared ([AppBackdrop]), so the app picture drawn by [AtmosphereBackground] shows behind the text.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun NotesReaderScreen(
    title: String,
    resourceId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureContentCaptureProtection()
    val context = LocalContext.current
    val screenScope = rememberCoroutineScope()
    var attempt by remember { mutableStateOf(0) }
    var result by remember(resourceId) { mutableStateOf<ContentItemResult?>(null) }
    val webRef = remember { arrayOfNulls<WebView>(1) }
    val progress = remember(resourceId) { NoteProgress() }
    var pageReady by remember(resourceId) { mutableStateOf(false) }
    // N1: where the student stopped last time (read once when the note opens)
    val statePrefs = remember { context.getSharedPreferences(NOTES_STATE_PREFS, Context.MODE_PRIVATE) }
    val startFrac = remember(resourceId) { statePrefs.getFloat("pos_$resourceId", 0f).coerceIn(0f, 1f) }
    // The tutor conversation of this note, kept on the phone (read once when the note opens; empty text = none)
    val savedChat = remember(resourceId) { statePrefs.getString("tutor_$resourceId", "") ?: "" }

    LaunchedEffect(resourceId, attempt) {
        result = null
        result = LearningContentRepository.getResource(context, resourceId)
    }

    LaunchedEffect(resourceId, result) {
        if (result is ContentItemResult.Found) {
            // Safety timeout: ensure note is revealed even if WebView JS bridge drops callback
            delay(1200L)
            pageReady = true
        }
    }

    // Reading time: starts when the note is shown, added every 30 seconds (the website does the same)
    val noteShown = result is ContentItemResult.Found
    LaunchedEffect(resourceId, noteShown) {
        if (noteShown) {
            progress.lastFlush = SystemClock.elapsedRealtime()
            progress.active = true
            while (true) {
                delay(30_000L)
                flushNoteProgress(resourceId, progress)
            }
        }
    }
    DisposableEffect(resourceId) {
        onDispose { flushNoteProgress(resourceId, progress) }
    }

    // ---- Inline AI tutor (2026-11-03): one running answer at a time, pushed into the note page ----
    val tutorJob = remember(resourceId) { arrayOfNulls<Job>(1) }

    /** Sends the answer so far to the page. [state] is `stream`, `done` or `failed`. Main thread only. */
    fun pushTutor(id: String, text: String, state: String) {
        webRef[0]?.evaluateJavascript(
            "window.wtTutorSet(${JSONObject.quote(id)}, ${JSONObject.quote(text)}, ${JSONObject.quote(state)});",
            null
        )
    }

    fun pushTutorSuggestions(id: String, items: List<String>) {
        webRef[0]?.evaluateJavascript(
            "window.wtTutorSuggest(${JSONObject.quote(id)}, ${JSONObject.quote(JSONArray(items).toString())});",
            null
        )
    }

    suspend fun streamTutor(id: String, history: List<Pair<String, String>>, chapterContext: String) {
        var acc = ""
        var suggestions: List<String> = emptyList()
        var lastPush = 0L
        var failed = false
        try {
            AiTutorRepository.stream(history, chapterContext).collect { ev ->
                when (ev) {
                    is TutorEvent.Chunk -> {
                        acc += ev.text
                        val now = SystemClock.elapsedRealtime()
                        // The page redraws the whole answer each time, so at most about every 80 ms.
                        if (now - lastPush >= 80L) {
                            lastPush = now
                            pushTutor(id, acc, "stream")
                        }
                    }

                    is TutorEvent.Suggestions -> {
                        suggestions = ev.items
                    }

                    TutorEvent.Done -> {
                        // nothing to do: the answer is complete
                    }

                    TutorEvent.Failed -> throw NoteTutorFailure()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
        if (failed) {
            pushTutor(id, if (acc.isEmpty()) NOTE_TUTOR_BUSY else acc + NOTE_TUTOR_CUT, "failed")
        } else {
            pushTutor(id, acc, "done")
            if (suggestions.isNotEmpty()) pushTutorSuggestions(id, suggestions)
        }
    }

    fun startTutor(json: String) {
        var id = ""
        try {
            val data = JSONObject(json)
            id = data.optString("id", "")
            val arr = data.optJSONArray("messages")
            val history = ArrayList<Pair<String, String>>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val role = o.optString("role", "")
                    val content = o.optString("content", "")
                    if ((role == "user" || role == "assistant") && content.isNotBlank()) {
                        history.add(role to content)
                    }
                }
            }
            if (id.isEmpty() || history.isEmpty()) return
            val noteText = (result as? ContentItemResult.Found)?.item?.bodyMd.orEmpty().take(14000)
            tutorJob[0]?.cancel()
            tutorJob[0] = screenScope.launch { streamTutor(id, history, buildTutorContext(title, noteText)) }
        } catch (e: Exception) {
            // A bad tutor message must never break the reader.
            if (id.isNotEmpty()) pushTutor(id, NOTE_TUTOR_BUSY, "failed")
        }
    }

    val bridge = remember(resourceId) {
        NotesBridge(
            onProgress = { json ->
                try {
                    val pct = JSONObject(json).optInt("pct", 0).coerceIn(0, 100)
                    progress.pct = pct
                    notesSaveScope.launch {
                        LearningProgressRepository.saveProgress(resourceId = resourceId, progressPct = pct)
                    }
                } catch (e: Exception) {
                    // A bad progress message must never break the reader.
                }
                Unit
            },
            onPosition = { json ->
                try {
                    val frac = JSONObject(json).optDouble("frac", 0.0).toFloat().coerceIn(0f, 1f)
                    statePrefs.edit().putFloat("pos_$resourceId", frac).apply()
                } catch (e: Exception) {
                    // A bad position message must never break the reader.
                }
                Unit
            },
            onTutorState = { json ->
                try {
                    // An empty conversation removes the saved one; anything absurdly large is not kept.
                    if (json.length <= 2) {
                        statePrefs.edit().remove("tutor_$resourceId").apply()
                    } else if (json.length < 600_000) {
                        statePrefs.edit().putString("tutor_$resourceId", json).apply()
                    }
                } catch (e: Exception) {
                    // Saving the chat is best effort and must never break the reader.
                }
                Unit
            },
            onTutorAsk = { json ->
                // Runs on the main thread: the answer is pushed back with evaluateJavascript.
                screenScope.launch { startTutor(json) }
                Unit
            },
            onTutorStop = {
                screenScope.launch {
                    tutorJob[0]?.cancel()
                    tutorJob[0] = null
                }
                Unit
            },
            onReady = {
                screenScope.launch(Dispatchers.Main) {
                    pageReady = true
                }
            }
        )
    }

    // imePadding: the tutor composer is at the end of the note page; the keyboard must not cover it.
    Box(modifier = modifier.fillMaxSize().imePadding()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            when (val r = result) {
                null -> {
                    var showSpinner by remember { mutableStateOf(false) }
                    LaunchedEffect(resourceId) {
                        delay(150L)
                        showSpinner = true
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (showSpinner) {
                            BrandSplashLoader()
                        }
                    }
                }

                is ContentItemResult.Found -> {
                    val coverAlpha by animateFloatAsState(
                        targetValue = if (pageReady) 0f else 1f,
                        animationSpec = tween(220),
                        label = "noteCoverAlpha"
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        // The page is hidden while it lays itself out: fully hidden, then fully shown (BG-NOFADE).
                        NoteWebView(
                            body = r.item.bodyMd.orEmpty(),
                            startFrac = startFrac,
                            chatJson = savedChat,
                            bridge = bridge,
                            webRef = webRef,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (pageReady) 1f else 0f)
                        )
                        if (coverAlpha > 0f) {
                            var showCoverLoader by remember { mutableStateOf(false) }
                            LaunchedEffect(resourceId) {
                                delay(150L)
                                showCoverLoader = true
                            }
                            // No fill: only the loader, drawn on the chosen app background.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(coverAlpha),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showCoverLoader) {
                                    BrandSplashLoader()
                                }
                            }
                        }
                    }
                }

                is ContentItemResult.NotFound -> NoteMessage(
                    message = "Unable to load materials right now. Please try again.",
                    onRetry = { attempt += 1 }
                )

                is ContentItemResult.Failure -> NoteMessage(message = r.message, onRetry = { attempt += 1 })
            }
        }
    }
}

/** Receives the messages of `notes/index.html` (`window.WTBridge.*(json)`), on a WebView thread. */
private class NotesBridge(
    private val onProgress: (String) -> Unit,
    private val onPosition: (String) -> Unit,
    private val onTutorState: (String) -> Unit,
    private val onTutorAsk: (String) -> Unit,
    private val onTutorStop: () -> Unit,
    private val onReady: () -> Unit
) {
    @JavascriptInterface
    fun onProgress(json: String) {
        onProgress.invoke(json)
    }

    @JavascriptInterface
    fun onPosition(json: String) {
        onPosition.invoke(json)
    }

    @JavascriptInterface
    fun onTutorState(json: String) {
        onTutorState.invoke(json)
    }

    @JavascriptInterface
    fun onTutorAsk(json: String) {
        onTutorAsk.invoke(json)
    }

    @JavascriptInterface
    fun onTutorStop() {
        onTutorStop.invoke()
    }

    @JavascriptInterface
    fun onReady() {
        onReady.invoke()
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun NoteWebView(body: String, startFrac: Float, chatJson: String, bridge: Any, webRef: Array<WebView?>, modifier: Modifier) {
    // JSONObject.quote gives a safe JavaScript string literal (quotes, backslashes, newlines escaped)
    val literal = remember(body) { JSONObject.quote(body) }
    val chatLiteral = remember(chatJson) { JSONObject.quote(chatJson) }
    key(literal) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AppBackdrop.webViewColor(ctx))
                    // BG-NOFLASH: hidden from the moment it exists. AppBackdrop.clearPageBackground shows it once the
                    // page is transparent; the 4 s safety shows it anyway if that never happens.
                    alpha = 0f
                    postDelayed({ alpha = 1f }, 4000)
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
                            // N1: the second argument is where the student stopped last time (0 = the top);
                            // the third is the tutor conversation kept for this note (empty text = none)
                            view.evaluateJavascript("window.setNote($literal, $startFrac, $chatLiteral);", null)
                        }

                        // The note page never navigates away; ignore every link tap.
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                    }
                    webRef[0] = this
                    loadUrl("file:///android_asset/notes/index.html")
                }
            },
            onRelease = {
                it.removeJavascriptInterface("WTBridge")
                if (webRef[0] === it) webRef[0] = null
                it.destroy()
            }
        )
    }
}

@Composable
private fun NoteMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, fontSize = 13.sp, color = WisdomMuted, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier.wisdomTouchFeel()
                .clip(RoundedCornerShape(10.dp))
                .background(WisdomCyan.copy(alpha = 0.12f))
                .clickable { onRetry() }
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Text(text = "Retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WisdomCyan)
        }
    }
}
