package com.wisdomtower.academy.ui.competition

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.wisdomtower.academy.ui.theme.AppBackdrop
import org.json.JSONArray
import org.json.JSONObject

// C7 slice 3: shows ONE reviewed competition question (prompt, choices with the correct one marked, the student's
// own pick, the solution), LaTeX and KaTeX included, in the local page assets/notes/competition_review.html.
// One WebView is kept for the whole review: the screen sends the current question with window.setReview(...).
// The page has no network (blockNetworkLoads) and ignores every link.
// The correct answer and the solution only reach this view after the server closed the window (the server
// refuses competition_review before that).
// C7 slice 4: the optional explanation parameter is the text of an AI explanation (empty when there is none);
// the page shows it in its own block, math rendered.
// Session AJ (owner, 2026-10-15): the page now holds the hub's two buttons (Official solution and Explain with
// AI) in one row and tells this view two things through a small bridge named WTReview: swipe (left = next,
// right = previous) and explain. canExplain says whether the Explain button is drawn at all (false while a ranked
// exam is running), explaining says the AI answer is on its way. The bridge calls come on a background thread,
// so they are handed to the main thread before the screen's lambdas run.
// Default background picture (owner, 2026-10-17): the WebView is see-through and the page's own dark page colour is
// cleared (AppBackdrop), so the app picture drawn behind the review screen shows through.
// Owner request 2026-10-24: bottomPaddingDp adds empty space at the end of the page (one style rule added after the page
// loaded), so the last lines can scroll above the floating Previous / Next pills that the screen draws over the page.
// Status: written, not compiled, not seen on a device.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CompetitionReviewView(
    questionId: String,
    prompt: String,
    choices: List<String>,
    correct: Int,
    chosen: Int?,
    solution: String,
    modifier: Modifier = Modifier,
    bottomPaddingDp: Int = 0,
    explanation: String = "",
    canExplain: Boolean = false,
    explaining: Boolean = false,
    onSwipe: (Boolean) -> Unit = {},
    onExplain: () -> Unit = {}
) {
    val webRef = remember { arrayOfNulls<WebView>(1) }
    var pageReady by remember { mutableStateOf(false) }
    val swipeNow = rememberUpdatedState(onSwipe)
    val explainNow = rememberUpdatedState(onExplain)

    val payload = remember(questionId, prompt, choices, correct, chosen, solution, explanation, canExplain, explaining) {
        val list = JSONArray()
        for (choice in choices) list.put(choice)
        JSONObject()
            .put("id", questionId)
            .put("prompt", prompt)
            .put("choices", list)
            .put("correct", correct)
            .put("chosen", chosen ?: -1)
            .put("solution", solution)
            .put("explanation", explanation)
            .put("canExplain", canExplain)
            .put("explaining", explaining)
            .toString()
    }

    // JSONObject.quote gives a safe JavaScript string literal; the page parses it with JSON.parse
    LaunchedEffect(payload, pageReady) {
        val web = webRef[0]
        if (pageReady && web != null) {
            web.evaluateJavascript("window.setReview(" + JSONObject.quote(payload) + ");", null)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AppBackdrop.webViewColor(ctx))
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.blockNetworkLoads = true
                settings.domStorageEnabled = false
                addJavascriptInterface(
                    ReviewBridge(
                        onSwipe = { next -> swipeNow.value(next) },
                        onExplain = { explainNow.value() }
                    ),
                    "WTReview"
                )
                webViewClient = object : WebViewClient() {
                    // BG-LOADING: hide the page until the glass script has run, the native backdrop shows meanwhile
                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                        AppBackdrop.hidePageUntilReady(view)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        AppBackdrop.clearPageBackground(view)
                        if (bottomPaddingDp > 0) {
                            val css = "#root{padding-bottom:" + bottomPaddingDp.toString() + "px !important}"
                            view.evaluateJavascript(
                                "(function(){var s=document.createElement('style');s.textContent=" +
                                    JSONObject.quote(css) + ";document.head.appendChild(s);})();",
                                null
                            )
                        }
                        pageReady = true
                    }

                    // the page never navigates away; ignore every link tap
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                }
                webRef[0] = this
                loadUrl("file:///android_asset/notes/competition_review.html")
            }
        },
        onRelease = {
            if (webRef[0] === it) webRef[0] = null
            it.destroy()
        }
    )
}

// What the review page can tell the app. JavaScript calls arrive on a background thread.
private class ReviewBridge(
    private val onSwipe: (Boolean) -> Unit,
    private val onExplain: () -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun swipe(direction: String) {
        main.post { onSwipe(direction == "next") }
    }

    @JavascriptInterface
    fun explain(unused: String) {
        main.post { onExplain() }
    }
}
