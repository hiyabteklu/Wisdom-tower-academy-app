package com.wisdomtower.academy.ui.study

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.wisdomtower.academy.data.content.AiExplainRepository
import com.wisdomtower.academy.data.content.ContentItemResult
import com.wisdomtower.academy.data.content.ExamAttemptRepository
import com.wisdomtower.academy.data.content.LearningContentRepository
import com.wisdomtower.academy.data.content.LearningProgressRepository
import com.wisdomtower.academy.ui.theme.AppBackdrop
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Saves started from the quiz page must survive the student leaving the screen right after "Submit Exam",
 * so they run in this app-wide scope, not in the screen's own scope (that one is cancelled on leaving).
 */
private val quizSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private const val QUIZ_PREFS = "wt_quiz_prefs"
private const val QUIZ_PREF_MODE = "feedback_mode"

/**
 * Native question bank / exam runner (plan step N5). Loads the full row (`meta.questions`, `meta.durationMin`)
 * with [LearningContentRepository.getResource] (offline copy kept) and shows it in the LOCAL page
 * `assets/notes/quiz.html` (port of the website's `QuizExamViewer.tsx` + `QuizResultModal.tsx`, shared KaTeX
 * renderer). The page reports back through a small JS bridge:
 *  - `onProgress`: saved to `learning_progress` like the website (`progress_pct`, `meta.quiz`); question banks
 *    on every answer, exams only once submitted;
 *  - `onSubmit` (exams only): saved to `exam_attempts` and, with [trackerScopeId], to `academic_results`
 *    ([ExamAttemptRepository]), the same rows as the website;
 *  - `onMode`: the "Right Away" / "After Finish" choice, remembered on this phone like the website's
 *    `wta_qb_feedback_mode`;
 *  - `onExplain` (plan step N10): "Explain with AI". The page sends `{ id, payload }`; this screen adds
 *    `resourceId`, asks the website route through [AiExplainRepository] (the page itself cannot reach the
 *    network) and answers with `window.wtExplainResult(id, text)`. The answer is dropped if the screen was left.
 * Exams keep the screen on (the website asks for a screen wake lock). System Back first closes an open dialog
 * inside the page, otherwise leaves.
 *
 * Clean reading (owner request 2026-10-13): NO top row (back arrow + title) any more, the quiz page uses the whole
 * screen under the app bar (its own timer, submit and question controls are inside the page and unchanged).
 * Leaving is done with the phone Back button / gesture (the BackHandler below); [title] is still passed to the page.
 *
 * Default background picture (owner request 2026-10-17): the WebView is see-through and the page's own dark page colour
 * is cleared ([AppBackdrop]), so the app picture drawn by [AtmosphereBackground] shows behind the questions.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun QuizExamScreen(
    title: String,
    resourceId: String,
    isExam: Boolean,
    trackerScopeId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureContentCaptureProtection()
    val context = LocalContext.current
    val screenScope = rememberCoroutineScope()
    var attempt by remember { mutableStateOf(0) }
    var result by remember(resourceId) { mutableStateOf<ContentItemResult?>(null) }
    val webRef = remember { arrayOfNulls<WebView>(1) }

    LaunchedEffect(resourceId, attempt) {
        result = null
        result = LearningContentRepository.getResource(context, resourceId)
    }

    BackHandler(enabled = true) {
        val web = webRef[0]
        if (web == null) {
            onBack()
        } else {
            web.evaluateJavascript("(window.wtBack ? window.wtBack() : false)") { reply ->
                if (reply != "true") onBack()
            }
        }
    }

    val bridge = remember(resourceId, isExam, trackerScopeId, title) {
        QuizBridge(
            onProgress = { json ->
                quizSaveScope.launch {
                    try {
                        val data = JSONObject(json)
                        val quiz = data.optJSONObject("quiz") ?: return@launch
                        LearningProgressRepository.saveProgress(
                            resourceId = resourceId,
                            progressPct = data.optInt("pct", 0),
                            meta = JSONObject().put("quiz", quiz)
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // A bad progress message must never break the quiz.
                    }
                }
                Unit
            },
            onSubmit = { json ->
                quizSaveScope.launch {
                    try {
                        val data = JSONObject(json)
                        ExamAttemptRepository.saveAttempt(
                            resourceId = resourceId,
                            score = data.optInt("score", 0),
                            total = data.optInt("total", 0),
                            answers = data.optJSONObject("answers") ?: JSONObject(),
                            title = title,
                            scopeId = trackerScopeId
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // The result card is already shown; a failed save is only logged inside the repository.
                    }
                }
                Unit
            },
            onMode = { json ->
                try {
                    val mode = JSONObject(json).optString("mode", "immediate")
                    context.getSharedPreferences(QUIZ_PREFS, Context.MODE_PRIVATE)
                        .edit().putString(QUIZ_PREF_MODE, if (mode == "completion") "completion" else "immediate").apply()
                } catch (e: Exception) {
                    // ignore
                }
                Unit
            },
            onExplain = { json ->
                // Runs in the screen's own scope: leaving the screen cancels a pending request, which is fine.
                screenScope.launch {
                    var id = ""
                    var text = "AI unavailable"
                    try {
                        val data = JSONObject(json)
                        id = data.optString("id", "")
                        val payload = data.optJSONObject("payload")
                        if (payload != null) {
                            payload.put("resourceId", resourceId)
                            text = AiExplainRepository.explain(payload.toString())
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // keep "AI unavailable"
                    }
                    if (id.isNotEmpty()) {
                        webRef[0]?.evaluateJavascript(
                            "window.wtExplainResult(${JSONObject.quote(id)}, ${JSONObject.quote(text)});",
                            null
                        )
                    }
                }
                Unit
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            when (val r = result) {
                null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WisdomCyan)
                }

                is ContentItemResult.Found -> QuizWebView(
                    metaJson = r.item.metaJson,
                    isExam = isExam,
                    title = title,
                    bridge = bridge,
                    webRef = webRef,
                    modifier = Modifier.fillMaxSize()
                )

                is ContentItemResult.NotFound -> QuizMessage(
                    message = "Unable to load materials right now. Please try again.",
                    onRetry = { attempt += 1 }
                )

                is ContentItemResult.Failure -> QuizMessage(message = r.message, onRetry = { attempt += 1 })
            }
        }
    }
}

/** Receives the messages of `quiz.html` (`window.WTBridge.*(json)`), on a WebView thread. */
private class QuizBridge(
    private val onProgress: (String) -> Unit,
    private val onSubmit: (String) -> Unit,
    private val onMode: (String) -> Unit,
    private val onExplain: (String) -> Unit
) {
    @JavascriptInterface
    fun onProgress(json: String) {
        onProgress.invoke(json)
    }

    @JavascriptInterface
    fun onSubmit(json: String) {
        onSubmit.invoke(json)
    }

    @JavascriptInterface
    fun onMode(json: String) {
        onMode.invoke(json)
    }

    @JavascriptInterface
    fun onExplain(json: String) {
        onExplain.invoke(json)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun QuizWebView(
    metaJson: String,
    isExam: Boolean,
    title: String,
    bridge: Any,
    webRef: Array<WebView?>,
    modifier: Modifier
) {
    val context = LocalContext.current
    // Saved "Right Away" / "After Finish" choice, read once when the quiz is opened
    val options = remember(isExam, title) {
        val mode = context.getSharedPreferences(QUIZ_PREFS, Context.MODE_PRIVATE)
            .getString(QUIZ_PREF_MODE, "immediate") ?: "immediate"
        JSONObject().put("isExam", isExam).put("title", title).put("feedbackMode", mode).toString()
    }
    // JSONObject.quote gives a safe JavaScript string literal (quotes, backslashes, newlines escaped)
    val metaLiteral = remember(metaJson) { JSONObject.quote(metaJson) }
    val optionsLiteral = remember(options) { JSONObject.quote(options) }
    key(metaLiteral, optionsLiteral) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AppBackdrop.webViewColor(ctx))
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.blockNetworkLoads = true
                    settings.domStorageEnabled = false
                    keepScreenOn = isExam
                    addJavascriptInterface(bridge, "WTBridge")
                    webViewClient = object : WebViewClient() {
                        // BG-LOADING: hide the page until the glass script has run, the native backdrop shows meanwhile
                        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                            AppBackdrop.hidePageUntilReady(view)
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            AppBackdrop.clearPageBackground(view)
                            view.evaluateJavascript("window.setQuiz($metaLiteral, $optionsLiteral);", null)
                        }

                        // The quiz page never navigates away; ignore every link tap.
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                    }
                    webRef[0] = this
                    loadUrl("file:///android_asset/notes/quiz.html")
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
private fun QuizMessage(message: String, onRetry: () -> Unit) {
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
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(WisdomCyan.copy(alpha = 0.12f))
                .clickable { onRetry() }
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Text(text = "Retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WisdomCyan)
        }
    }
}
