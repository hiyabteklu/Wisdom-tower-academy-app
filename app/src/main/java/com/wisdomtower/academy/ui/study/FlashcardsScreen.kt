package com.wisdomtower.academy.ui.study

import android.annotation.SuppressLint
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import com.wisdomtower.academy.ui.theme.wisdomScreenEntrance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.wisdomtower.academy.data.content.ContentItemResult
import com.wisdomtower.academy.data.content.LearningContentRepository
import com.wisdomtower.academy.data.content.LearningProgressRepository
import com.wisdomtower.academy.ui.theme.AppBackdrop
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Native flashcard deck (plan step N3). Loads the full row (`meta.cards`) with
 * [LearningContentRepository.getResource] (offline copy kept) and shows it in the LOCAL page
 * `assets/notes/flashcards.html` (port of the website's `FlashcardViewer.tsx`, shared KaTeX renderer).
 * Every Know / Learning / Again tap reports the counters back through a small JS bridge and they are saved
 * to `learning_progress` like the website (`progress_pct` = cards seen / cards, `meta.flashcards`).
 *
 * Clean reading (owner request 2026-10-13): NO top row (back arrow + deck title) any more, the deck uses the whole
 * screen under the app bar. Leaving is done with the phone Back button / gesture (handled by `StudyWorkspaceScreen`);
 * [onBack] and [title] are kept in the signature but not drawn.
 *
 * Default background picture (owner request 2026-10-17): the WebView is see-through and the page's own dark page colour
 * is cleared ([AppBackdrop]), so the app picture drawn by [AtmosphereBackground] shows behind the cards.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun FlashcardsScreen(
    title: String,
    resourceId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var attempt by remember { mutableStateOf(0) }
    var result by remember(resourceId) { mutableStateOf<ContentItemResult?>(null) }

    LaunchedEffect(resourceId, attempt) {
        result = null
        result = LearningContentRepository.getResource(context, resourceId)
    }

    val onProgress: (String) -> Unit = remember(resourceId) {
        { json ->
            scope.launch {
                try {
                    val stats = JSONObject(json)
                    val total = stats.optInt("total", 0)
                    val seen = stats.optInt("seen", 0)
                    val pct = (seen * 100.0 / maxOf(1, total)).roundToInt()
                    LearningProgressRepository.saveProgress(
                        resourceId = resourceId,
                        progressPct = pct,
                        meta = JSONObject().put("flashcards", stats)
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // A bad progress message must never break the deck.
                }
            }
            Unit
        }
    }

    Box(modifier = modifier.fillMaxSize().wisdomScreenEntrance()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            when (val r = result) {
                null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WisdomCyan)
                }

                is ContentItemResult.Found -> DeckWebView(
                    metaJson = r.item.metaJson,
                    onProgress = onProgress,
                    modifier = Modifier.fillMaxSize()
                )

                is ContentItemResult.NotFound -> DeckMessage(
                    message = "Unable to load materials right now. Please try again.",
                    onRetry = { attempt += 1 }
                )

                is ContentItemResult.Failure -> DeckMessage(message = r.message, onRetry = { attempt += 1 })
            }
        }
    }
}

/** Receives the counters from `flashcards.html` (`window.WTBridge.onProgress(json)`), on a WebView thread. */
private class DeckBridge(private val callback: (String) -> Unit) {
    @JavascriptInterface
    fun onProgress(json: String) {
        callback(json)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun DeckWebView(metaJson: String, onProgress: (String) -> Unit, modifier: Modifier) {
    // JSONObject.quote gives a safe JavaScript string literal (quotes, backslashes, newlines escaped)
    val literal = remember(metaJson) { JSONObject.quote(metaJson) }
    key(literal) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AppBackdrop.webViewColor(ctx))
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.blockNetworkLoads = true
                    settings.domStorageEnabled = false
                    addJavascriptInterface(DeckBridge(onProgress), "WTBridge")
                    webViewClient = object : WebViewClient() {
                        // BG-LOADING: hide the page until the glass script has run, the native backdrop shows meanwhile
                        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                            AppBackdrop.hidePageUntilReady(view)
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            AppBackdrop.clearPageBackground(view)
                            view.evaluateJavascript("window.setDeck($literal);", null)
                        }

                        // The deck page never navigates away; ignore every link tap.
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                    }
                    loadUrl("file:///android_asset/notes/flashcards.html")
                }
            },
            onRelease = {
                it.removeJavascriptInterface("WTBridge")
                it.destroy()
            }
        )
    }
}

@Composable
private fun DeckMessage(message: String, onRetry: () -> Unit) {
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
