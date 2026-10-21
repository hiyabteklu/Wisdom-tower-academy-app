package com.wisdomtower.academy.ui.games.common

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
import org.json.JSONArray
import org.json.JSONObject

/**
 * State for one game question card shown in the WebView.
 * [hidden] indexes are removed by the 50/50 power-up.
 * [reveal] null until the app judges the answer; then correct index + solution text.
 */
data class GameCardState(
    val id: String,
    val prompt: String,
    val choices: List<String>,
    val selected: Int = -1,
    val enabled: Boolean = true,
    val hidden: List<Int> = emptyList(),
    val revealCorrect: Int? = null,
    val revealSolution: String? = null
)

private class GameQuestionBridge(
    private val onPickIndex: (Int) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPick(index: Int) {
        main.post { onPickIndex(index) }
    }
}

/**
 * Transparent WebView question card for Climb and Defense.
 * One WebView per session: update via [GameCardState], do not recreate per question.
 * Pattern copied from CompetitionQuestionView (setCard, blockNetworkLoads, WTBridge).
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun GameQuestionCard(
    card: GameCardState,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnPick by rememberUpdatedState(onPick)
    val bridge = remember { GameQuestionBridge { index -> latestOnPick(index) } }
    val webRef = remember { arrayOfNulls<WebView>(1) }
    var pageReady by remember { mutableStateOf(false) }

    val payload = remember(card) {
        val list = JSONArray()
        for (choice in card.choices) list.put(choice)
        val hidden = JSONArray()
        for (h in card.hidden) hidden.put(h)
        val obj = JSONObject()
            .put("id", card.id)
            .put("prompt", card.prompt)
            .put("choices", list)
            .put("selected", card.selected)
            .put("enabled", card.enabled)
            .put("hidden", hidden)
        if (card.revealCorrect != null) {
            val reveal = JSONObject()
                .put("correct", card.revealCorrect)
                .put("solution", card.revealSolution ?: "")
            obj.put("reveal", reveal)
        } else {
            obj.put("reveal", JSONObject.NULL)
        }
        obj.toString()
    }

    LaunchedEffect(payload, pageReady) {
        val web = webRef[0]
        if (pageReady && web != null) {
            web.evaluateJavascript("window.setCard(" + JSONObject.quote(payload) + ");", null)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.blockNetworkLoads = true
                settings.domStorageEnabled = false
                addJavascriptInterface(bridge, "WTBridge")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        pageReady = true
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                }
                webRef[0] = this
                loadUrl("file:///android_asset/notes/game_question.html")
            }
        },
        onRelease = {
            it.removeJavascriptInterface("WTBridge")
            if (webRef[0] === it) webRef[0] = null
            it.destroy()
        }
    )
}
