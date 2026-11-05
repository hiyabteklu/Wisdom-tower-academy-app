package com.wisdomtower.academy.ui.competition

import com.wisdomtower.academy.ui.study.SecureContentCaptureProtection
import com.wisdomtower.academy.ui.theme.WisdomPopupFill

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wisdomtower.academy.data.competition.CompetitionError
import com.wisdomtower.academy.data.competition.CompetitionRepository
import com.wisdomtower.academy.data.competition.CompetitionResult
import com.wisdomtower.academy.data.competition.CompetitionRunnerState
import com.wisdomtower.academy.data.competition.CompetitionSession
import com.wisdomtower.academy.data.competition.ExamLockdown
import com.wisdomtower.academy.data.competition.SubmitResult
import com.wisdomtower.academy.ui.theme.AppBackdrop
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// A last autosave when the student leaves the screen must not be cancelled with the screen.
private val examSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

// C6 UI slice 1 (owner, 2026-10-15): the competition exam is now the SAME exam window as the hub.
// The whole window (top bar with answered counter and countdown, question number pills, Flag, cross-out on every
// choice, Prev / Next / Submit, the confirm dialog and the score card with the gauge) is the local page
// assets/notes/competition_exam.html, a copy of the hub exam window (quiz.html) made for server grading.
// This file is now only the host: it keeps the clock, the autosave, the lockdown flag and the server submit.
// The page holds NO answer key. The page reports picks with WTBridge.onAnswers, asks for the grade with
// WTBridge.onSubmit and gets the server score back through window.wtSubmitResult(...). The Done button of the page
// calls WTBridge.onDone, which closes this screen through onFinished.
// What stayed from before: the global deadline (server-corrected clock), autosave after each change and every
// 10 seconds, forced submit when the server clock says time is up, ALREADY_SUBMITTED handled as onFinished(null),
// Back keeps the clock running (the student can resume until the deadline).
// Clipped buttons: the page has its own sticky bottom bar with safe-area padding and this screen adds no padding.
// C10 slice 2 (session C10-B): while a RANKED exam is open, leaving the app (activity stop) is recorded for the
// admin with CompetitionRepository.reportFlag (app_background when the app leaves, app_foreground with the time
// away when it comes back). Failures are ignored.
// C10 slice 5 (owner decision Q9, strict warning): when the student comes back after leaving the app during a
// RANKED exam, a warning dialog must be acknowledged. It does not stop the clock and does not disqualify; the
// admin reviews the recorded flags. The warning wording is the owner's (2026-10-16): detected, will be reviewed
// by admins, may result in disqualification. The owner decided NOT to pin the phone.
// Default background picture (owner, 2026-10-17): the screen now draws the app background (AtmosphereBackground)
// behind the page, the WebView is see-through and the page's own dark page colour is cleared (AppBackdrop).
// D7e (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. The only native colours here are in the away-warning dialog: its
// card was fixed dark 0F1829 at 95 percent with a white title. Night keeps exactly that; day uses ivory FBF9F4 at 95
// percent with the navy ink title (WisdomInk). The body text (WisdomMuted) and the rose button (WisdomAccentRose) were
// already mode aware. The exam page itself is covered by the D8 script.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

// Receives the calls from the page on a WebView thread and hands them to the main thread.
private class ExamBridge(
    private val onAnswersJson: (String) -> Unit,
    private val onSubmitJson: (String) -> Unit,
    private val onDoneCall: () -> Unit,
    private val onIntegrityFlagJson: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onAnswers(json: String) {
        main.post { onAnswersJson(json) }
    }

    @JavascriptInterface
    fun onSubmit(json: String) {
        main.post { onSubmitJson(json) }
    }

    @JavascriptInterface
    fun onDone(json: String) {
        main.post { onDoneCall() }
    }

    @JavascriptInterface
    fun onIntegrityFlag(json: String) {
        main.post { onIntegrityFlagJson(json) }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun CompetitionExamScreen(
    session: CompetitionSession,
    onFinished: (SubmitResult?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = ""
) {
    SecureContentCaptureProtection()
    val state = remember(session.entryId) { CompetitionRunnerState(session) }
    var version by remember(session.entryId) { mutableIntStateOf(0) }
    var nowMs by remember { mutableLongStateOf(CompetitionRepository.serverNowMs()) }
    var submitting by remember { mutableStateOf(false) }
    var submittedOk by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<SubmitResult?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var showAwayWarning by remember { mutableStateOf(false) }
    val webRef = remember { arrayOfNulls<WebView>(1) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // keep the screen on while the exam is open
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // ranked exam open: raise the app-wide lockdown flag, lower it when the screen goes away
    DisposableEffect(session.entryId) {
        if (state.isRanked) ExamLockdown.enter()
        onDispose {
            if (state.isRanked) ExamLockdown.leave()
        }
    }

    // C10: record leaving the app during a RANKED exam, and warn the student when they come back
    DisposableEffect(lifecycleOwner, session.entryId) {
        var leftAtMs = 0L
        val observer = LifecycleEventObserver { _, event ->
            if (state.isRanked && !submittedOk) {
                if (event == Lifecycle.Event.ON_STOP) {
                    leftAtMs = CompetitionRepository.serverNowMs()
                    val detail = JSONObject().put("at_ms", leftAtMs)
                    examSaveScope.launch {
                        CompetitionRepository.reportFlag(session.entryId, "app_background", detail)
                    }
                } else if (event == Lifecycle.Event.ON_START && leftAtMs > 0L) {
                    val awayMs = CompetitionRepository.serverNowMs() - leftAtMs
                    leftAtMs = 0L
                    val detail = JSONObject().put("away_ms", awayMs)
                    examSaveScope.launch {
                        CompetitionRepository.reportFlag(session.entryId, "app_foreground", detail)
                    }
                    showAwayWarning = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // clock, once per second, server-corrected
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = CompetitionRepository.serverNowMs()
            delay(1000L)
        }
    }

    suspend fun saveNow() {
        if (!state.hasUnsavedChanges()) return
        val snapshot = state.answersSnapshot()
        val saved = CompetitionRepository.saveAnswers(session.entryId, snapshot)
        if (saved is CompetitionResult.Ok) state.markSaved(snapshot)
    }

    // autosave a moment after each change (the effect restarts on every change, so it waits for a pause)
    LaunchedEffect(version) {
        if (state.hasUnsavedChanges() && !submitting && !submittedOk) {
            delay(1200L)
            if (!submitting && !submittedOk) saveNow()
        }
    }

    // and retry every 10 seconds, so a failed save is not forgotten
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            if (!submitting && !submittedOk) saveNow()
        }
    }

    // the page sends its answers as { answers: { "<question index>": choice } }
    fun applyPageAnswers(json: String) {
        try {
            val map = JSONObject(json).optJSONObject("answers") ?: return
            val keys = map.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val index = key.toIntOrNull() ?: continue
                val choice = map.optInt(key, -1)
                val question = state.questions.getOrNull(index) ?: continue
                if (choice >= 0) state.select(question.id, choice)
            }
            version += 1
        } catch (e: Exception) {
            // a malformed message from the page is ignored; the next pick sends the full state again
        }
    }

    fun sendResultToPage(ok: Boolean, result: SubmitResult?, message: String?) {
        val web = webRef[0] ?: return
        val body = JSONObject()
        body.put("ok", ok)
        body.put("score", result?.score ?: JSONObject.NULL)
        body.put("total", result?.total ?: JSONObject.NULL)
        body.put("timeTakenMs", result?.timeTakenMs ?: JSONObject.NULL)
        if (message != null) body.put("message", message)
        web.evaluateJavascript("window.wtSubmitResult(" + JSONObject.quote(body.toString()) + ");", null)
    }

    fun handleSubmit(json: String) {
        if (submitting || submittedOk) return
        applyPageAnswers(json)
        submitting = true
        scope.launch {
            val r = CompetitionRepository.submit(session.entryId, state.answersSnapshot())
            if (r is CompetitionResult.Ok) {
                submitResult = r.value
                submittedOk = true
                sendResultToPage(true, r.value, null)
            } else if (r is CompetitionResult.Failed) {
                if (r.error == CompetitionError.ALREADY_SUBMITTED) {
                    submitResult = null
                    submittedOk = true
                    sendResultToPage(true, null, null)
                } else {
                    sendResultToPage(false, null, r.message)
                }
            }
            submitting = false
        }
    }

    val latestAnswers by rememberUpdatedState<(String) -> Unit> { json -> applyPageAnswers(json) }
    val latestSubmit by rememberUpdatedState<(String) -> Unit> { json -> handleSubmit(json) }
    val latestDone by rememberUpdatedState<() -> Unit> { onFinished(submitResult) }
    val latestIntegrityFlag by rememberUpdatedState<(String) -> Unit> { json ->
        try {
            val payload = JSONObject(json)
            val kind = payload.optString("kind")
            if (kind == "copy_attempt" && state.isRanked && !submittedOk) {
                val detail = payload.optJSONObject("detail") ?: JSONObject()
                examSaveScope.launch {
                    CompetitionRepository.reportFlag(session.entryId, kind, detail)
                }
            }
        } catch (_: Exception) {
            // Ignore malformed integrity telemetry; it must never interrupt the exam.
        }
    }
    val bridge = remember {
        ExamBridge(
            onAnswersJson = { json -> latestAnswers(json) },
            onSubmitJson = { json -> latestSubmit(json) },
            onDoneCall = { latestDone() },
            onIntegrityFlagJson = { json -> latestIntegrityFlag(json) }
        )
    }

    // send the questions to the page once it is loaded (no answer keys, only prompt and choices)
    LaunchedEffect(pageReady) {
        val web = webRef[0]
        if (pageReady && web != null) {
            val questions = JSONArray()
            for (q in state.questions) {
                val choices = JSONArray()
                for (c in q.choices) choices.put(c)
                questions.put(JSONObject().put("prompt", q.prompt).put("choices", choices))
            }
            val meta = JSONObject().put("questions", questions)
            val initial = JSONObject()
            for (i in state.questions.indices) {
                val chosen = state.answerFor(state.questions[i].id)
                if (chosen != null) initial.put(i.toString(), chosen)
            }
            val options = JSONObject().put("title", title).put("ranked", state.isRanked).put("initial", initial)
            val left = state.remainingMs(CompetitionRepository.serverNowMs())
            if (left != null) options.put("remainingMs", left)
            web.evaluateJavascript(
                "window.setQuiz(" + JSONObject.quote(meta.toString()) + ", " + JSONObject.quote(options.toString()) + ");",
                null
            )
        }
    }

    // time is up on the SERVER clock: ask the page to submit (it also submits by its own countdown)
    val timeUp = state.isRanked && state.isTimeUp(nowMs)
    LaunchedEffect(timeUp, pageReady) {
        val web = webRef[0]
        if (timeUp && pageReady && web != null) {
            web.evaluateJavascript("window.wtForceSubmit();", null)
        }
    }

    // Back: the page closes its own dialog first; otherwise save and leave (the clock keeps running)
    BackHandler(enabled = true) {
        val web = webRef[0]
        val leave = {
            if (submittedOk) {
                onFinished(submitResult)
            } else {
                val snapshot = state.answersSnapshot()
                if (state.hasUnsavedChanges()) {
                    examSaveScope.launch { CompetitionRepository.saveAnswers(session.entryId, snapshot) }
                }
                onExit()
            }
        }
        if (web == null) {
            leave()
        } else {
            web.evaluateJavascript("window.wtBack();") { answer ->
                if (answer != "true") leave()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                var obscuredGestureReported = false
                WebView(ctx).apply {
                    setOnTouchListener { _, event ->
                        val fullyObscured = (event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0
                        val partiallyObscured = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            (event.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0
                        if ((fullyObscured || partiallyObscured) && !obscuredGestureReported &&
                            state.isRanked && !submittedOk
                        ) {
                            obscuredGestureReported = true
                            examSaveScope.launch {
                                CompetitionRepository.reportFlag(
                                    session.entryId,
                                    "overlay_open",
                                    JSONObject().put("source", "obscured_touch")
                                )
                            }
                        }
                        if (event.actionMasked == MotionEvent.ACTION_UP ||
                            event.actionMasked == MotionEvent.ACTION_CANCEL
                        ) {
                            obscuredGestureReported = false
                        }
                        false
                    }
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
                            pageReady = true
                        }

                        // the page never navigates away; ignore every link tap
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                    }
                    webRef[0] = this
                    loadUrl("file:///android_asset/notes/competition_exam.html")
                }
            },
            onRelease = {
                it.removeJavascriptInterface("WTBridge")
                if (webRef[0] === it) webRef[0] = null
                it.destroy()
            }
        )
    }

    // C10 slice 5: warning after the student came back from another app during a RANKED exam (owner's wording)
    if (showAwayWarning && !submittedOk) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = WisdomPopupFill,
            title = {
                Text(
                    text = "You left the exam window",
                    color = WisdomInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "We detected that you left the exam window during the exam. " +
                        "This will be reviewed by admins and may result in disqualification.",
                    fontSize = 12.5.sp,
                    color = WisdomMuted,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAwayWarning = false }) {
                    Text(text = "I understand", color = WisdomAccentRose, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
