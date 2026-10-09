package com.wisdomtower.academy

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

internal const val TAB_HOME = 0
internal const val TAB_LEARNING = 1
internal const val TAB_PACKAGES = 2
internal const val TAB_ACCOUNT = 3
internal const val TAB_SETTINGS = 4

internal val TAB_DEFAULT_URLS = listOf(
    "https://www.wisdom-tower-academy.live/",
    "https://www.wisdom-tower-academy.live/learning",
    "https://www.wisdom-tower-academy.live/packages",
    "https://www.wisdom-tower-academy.live/account",
    "https://www.wisdom-tower-academy.live/settings"
)

private val ALLOWED_ORIGIN_RULES = setOf(
    "https://wisdom-tower-academy.live",
    "https://www.wisdom-tower-academy.live"
)

internal const val DOCUMENT_START_JS = """
(function() {
    try {
        if (window.location.protocol === 'file:') return;
        document.documentElement.classList.add('wta-native-app');
        var css = 'html,body{background-color:#060B15!important;color-scheme:dark!important;}' +
            'header:not([data-ai-tutor-header]),header:not([data-ai-tutor-header]).fixed.top-0,header:not([data-ai-tutor-header])[data-site-header],.site-header,[data-site-header],[role="banner"]:not([data-ai-tutor-header]),' +
            'nav[aria-label="Main"],nav.hidden.md\\:flex,.site-nav,.site-navigation,[data-site-nav],' +
            'body > footer,footer.site-footer,[data-site-footer],[role="contentinfo"],footer:not([data-ai-tutor-root] footer),' +
            '.hide-on-app,.app-hidden,[data-hide-on-app],[data-hide-app],.web-only,[data-web-only],' +
            '#nprogress,.nprogress,#nprogress .bar,[data-nprogress],#nextjs-toploader,.nextjs-toploader,' +
            'nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast],' +
            'header:not([data-ai-tutor-header]) button[aria-label*="menu" i],button[aria-label*="menu" i],.mobile-menu,[data-mobile-menu],' +
            'nav[aria-label*="mobile" i],[data-bottom-nav],.bottom-nav,nav.fixed.bottom-0,' +
            'img.wta-img-broken,img:not([src]),img[src=""]' +
            '{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;opacity:0!important;pointer-events:none!important;margin:0!important;padding:0!important;}' +
            'div:has(#wt-ai-tutor-input){bottom:0!important;}' +
            '[data-ai-tutor-root] footer,#wt-ai-tutor-input,[role="dialog"] footer' +
            '{display:block!important;visibility:visible!important;height:auto!important;max-height:none!important;opacity:1!important;pointer-events:auto!important;}';

        var id = 'wta-app-chrome';
        var s = document.getElementById(id);
        if (!s) {
            s = document.createElement('style');
            s.id = id;
            var target = document.head || document.documentElement;
            if (target) {
                target.insertBefore(s, target.firstChild);
            }
        }
        s.textContent = css;

        if (document.body) {
            document.body.classList.add('wta-native-app');
        } else {
            document.addEventListener('DOMContentLoaded', function() {
                if (document.body) document.body.classList.add('wta-native-app');
            });
        }

        // Native smooth in-tab transition hook for Next.js soft navigation and link taps
        if (!window.__wta_nav_hooked) {
            window.__wta_nav_hooked = true;
            function triggerInTabTransition() {
                if (window.AndroidOfflineVault && typeof window.AndroidOfflineVault.notifyLoadingStarted === 'function') {
                    window.AndroidOfflineVault.notifyLoadingStarted("in-tab");
                }
                requestAnimationFrame(function() {
                    requestAnimationFrame(function() {
                        if (window.AndroidOfflineVault && typeof window.AndroidOfflineVault.notifyVisualCommitReady === 'function') {
                            window.AndroidOfflineVault.notifyVisualCommitReady();
                        }
                    });
                });
            }

            var origPush = history.pushState;
            history.pushState = function() {
                var res = origPush.apply(this, arguments);
                triggerInTabTransition();
                return res;
            };

            var origReplace = history.replaceState;
            history.replaceState = function() {
                var res = origReplace.apply(this, arguments);
                triggerInTabTransition();
                return res;
            };

            window.addEventListener('popstate', function() {
                triggerInTabTransition();
            });

            document.addEventListener('click', function(e) {
                var a = e.target && e.target.closest ? e.target.closest('a[href]') : null;
                if (a && a.href && !a.getAttribute('target') && !a.href.startsWith('javascript:')) {
                    try {
                        var u = new URL(a.href, window.location.origin);
                        if (u.origin === window.location.origin && !u.pathname.toLowerCase().endsWith('.pdf')) {
                            triggerInTabTransition();
                        }
                    } catch(_) {}
                }
            }, true);
        }
    } catch(e) {}
})();
"""

internal fun isAllowedDomain(host: String?): Boolean {
    if (host == null) return false
    val h = host.lowercase()
    return h == "wisdom-tower-academy.live" ||
           h.endsWith(".wisdom-tower-academy.live") ||
           h == "wisdomtower.tech" ||
           h.endsWith(".wisdomtower.tech")
}

internal fun isPdfUri(uri: Uri): Boolean {
    val path = uri.path.orEmpty().removeSuffix("/").lowercase()
    return path.endsWith(".pdf") ||
           path == "/pdf" || path.startsWith("/pdf/") ||
           path == "/api/content/pdf" || path.startsWith("/api/content/pdf/") ||
           path.contains("/pdf/")
}

internal fun isLearningToolUri(uri: Uri): Boolean {
    if (!uri.isHierarchical) return false
    val path = uri.path.orEmpty().removeSuffix("/").lowercase()
    val isLearning = path == "/learning" || path.startsWith("/learning/")
    val hasToolParam = try { uri.getQueryParameter("tool") != null } catch (_: Exception) { false } || uri.toString().contains("tool=")
    return isLearning && hasToolParam
}

internal fun isOfflineUri(uri: Uri): Boolean {
    val path = uri.path.orEmpty().removeSuffix("/").lowercase()
    return path == "/offline" || path.startsWith("/offline/") || path.endsWith("/offline")
}

/**
 * Per-tab state holder.
 */
class SingleTabState(val index: Int, val defaultUrl: String) {
    var webView: WebView? by mutableStateOf(null)
    var isRevealed by mutableStateOf(false)
    var isFirstLoad by mutableStateOf(true)
    var showSpinner by mutableStateOf(false)
    var inTabTransitioning by mutableStateOf(false)
    var lastTargetUrl: String = defaultUrl
    var lastOnlineUrl: String = defaultUrl
    internal var spinnerTimeoutRunnable: Runnable? = null
}

/**
 * Controller and state for managing 5 persistent per-tab WebViews.
 */
class TabWebViewHostState(
    private val context: Context,
    val onRouteChangedCallback: (String) -> Unit,
    val onUserLoginCallback: (String?) -> Unit,
    val syncStudyTimerCallback: (Boolean, Int, Int, String?) -> Unit,
    val notifyPlannerTaskCallback: (String?, String?, String?) -> Unit,
    val notifyDailyStudyGoalCallback: () -> Unit,
    val openNotificationSettingsCallback: () -> Unit,
    val openToolOverlayCallback: (String, String) -> Unit,
    val openOrDownloadPdfCallback: (WebView, Context, String) -> Unit,
    val showOfflineCallback: (WebView, Boolean) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    val tabStates = List(5) { i -> SingleTabState(i, TAB_DEFAULT_URLS[i]) }
    private val tabAccessLru = mutableListOf<Int>()
    internal val startedDownloads = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    init {
        // Initially access Tab 0
        recordTabAccess(TAB_HOME)
    }

    fun recordTabAccess(index: Int) {
        synchronized(tabAccessLru) {
            tabAccessLru.remove(index)
            tabAccessLru.add(index)
        }
    }

    fun getActiveWebView(selectedIndex: Int): WebView? {
        return tabStates.getOrNull(selectedIndex)?.webView
    }

    fun canActiveTabGoBack(selectedIndex: Int): Boolean {
        val wv = getActiveWebView(selectedIndex) ?: return false
        val cur = wv.url.orEmpty()
        if (cur.isBlank() || cur.startsWith("file://") || cur.contains("offline.html")) return false
        val list = wv.copyBackForwardList()
        val currIdx = list.currentIndex
        if (currIdx > 0 && wv.canGoBack()) {
            val prevItem = list.getItemAtIndex(currIdx - 1)
            val prevUrl = prevItem.url.orEmpty()
            return prevUrl.isNotBlank() && !prevUrl.startsWith("file://") && !prevUrl.contains("offline.html")
        }
        return false
    }

    fun activeTabGoBack(selectedIndex: Int): Boolean {
        val wv = getActiveWebView(selectedIndex) ?: return false
        if (canActiveTabGoBack(selectedIndex)) {
            wv.goBack()
            return true
        }
        return false
    }

    fun prewarmTab(index: Int) {
        if (index !in tabStates.indices) return
        val tab = tabStates[index]
        if (tab.webView == null) {
            mainHandler.post {
                createWebViewForTab(index, context)
            }
        }
    }

    fun onTrimMemory(level: Int, currentSelectedIndex: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            synchronized(tabAccessLru) {
                val toEvict = tabAccessLru.filter { it != currentSelectedIndex }
                // Keep 1 background tab on moderate trim; evict all non-active tabs on running low
                val keepCount = if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) 0 else 1
                val evictList = if (toEvict.size > keepCount) toEvict.take(toEvict.size - keepCount) else emptyList()
                for (tabIdx in evictList) {
                    evictTab(tabIdx)
                }
            }
        }
    }

    private fun evictTab(tabIndex: Int) {
        val tab = tabStates.getOrNull(tabIndex) ?: return
        val wv = tab.webView ?: return
        val currentUrl = wv.url.orEmpty()
        if (currentUrl.isNotBlank() && !currentUrl.startsWith("file://") && !currentUrl.contains("offline.html")) {
            tab.lastTargetUrl = currentUrl
            tab.lastOnlineUrl = currentUrl
        }
        try {
            wv.stopLoading()
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
        } catch (_: Exception) {}
        tab.webView = null
        tab.isRevealed = false
        tab.isFirstLoad = true
        tab.showSpinner = false
        tab.inTabTransitioning = false
        tab.spinnerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        tab.spinnerTimeoutRunnable = null
        synchronized(tabAccessLru) {
            tabAccessLru.remove(tabIndex)
        }
    }

    fun reloadActiveTab(selectedIndex: Int, isOnline: Boolean) {
        val tab = tabStates.getOrNull(selectedIndex) ?: return
        val wv = tab.webView ?: return
        val currentUrl = wv.url.orEmpty()
        val isOfflineAsset = currentUrl.contains("offline.html") || currentUrl.startsWith("file://")
        val target = if (isOfflineAsset) tab.lastTargetUrl.ifBlank { tab.defaultUrl } else currentUrl.ifBlank { tab.lastTargetUrl }

        startRevealGate(tab)
        if (isOnline) {
            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
            if (isOfflineAsset) {
                wv.loadUrl(target)
            } else {
                wv.evaluateJavascript(
                    "(function(){try{window.location.reload(true);}catch(e){window.location.reload();}})();"
                ) {
                    mainHandler.post { wv.reload() }
                }
            }
        } else {
            if (WebCacheVault.has(context, target)) {
                wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                wv.loadUrl(target)
            } else {
                showOfflineCallback(wv, true)
            }
        }
    }

    fun navigateTo(
        url: String,
        targetTabIndex: Int,
        resetHistory: Boolean = false,
        isOnline: Boolean = true
    ) {
        recordTabAccess(targetTabIndex)
        val tab = tabStates.getOrNull(targetTabIndex) ?: return
        var wv = tab.webView
        if (wv == null) {
            wv = createWebViewForTab(targetTabIndex, context)
        }

        tab.lastTargetUrl = url
        if (isOnline) {
            tab.lastOnlineUrl = url
        }

        // Start reveal gate transition
        startRevealGate(tab)

        if (resetHistory) {
            wv.clearHistory()
        }

        if (isOnline) {
            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
            val currentUrl = wv.url.orEmpty()
            if (!resetHistory && tab.isRevealed && currentUrl.isNotBlank() && !currentUrl.startsWith("file://") && !currentUrl.contains("offline.html")) {
                val escapedUrl = url.replace("\\", "\\\\").replace("'", "\\'")
                val js = "(function(targetUrl){try{" +
                    "if(!targetUrl||window.location.protocol==='file:')return 'fallback';" +
                    "var cur=window.location.href;" +
                    "if(cur===targetUrl)return 'noop';" +
                    "var path=targetUrl;" +
                    "try{var u=new URL(targetUrl,window.location.origin);path=u.pathname+u.search+u.hash;}catch(_){}" +
                    "try{window.dispatchEvent(new CustomEvent('wta-navigate',{detail:{path:path,url:targetUrl}}));}catch(_){}" +
                    "if(typeof window.__wtaNavigate==='function'){try{var r=window.__wtaNavigate(path);if(r===true||r==='ok')return 'ok';}catch(_){}}" +
                    "if(window.next&&window.next.router&&typeof window.next.router.push==='function'){try{window.next.router.push(path);return 'ok';}catch(_){}}" +
                    "try{var existing=document.querySelector('a[href=\"'+path+'\"],a[href=\"'+targetUrl+'\"]');if(existing){existing.click();return 'ok';}}catch(_){}" +
                    "return 'fallback';" +
                    "}catch(e){return 'fallback';}})('$escapedUrl');"
                wv.evaluateJavascript(js) { res ->
                    val r = res?.replace("\"", "")?.trim().orEmpty()
                    if (r != "ok" && r != "noop") {
                        mainHandler.post { wv.loadUrl(url) }
                    }
                }
            } else {
                wv.loadUrl(url)
            }
        } else {
            if (WebCacheVault.has(context, url)) {
                wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                wv.loadUrl(url)
            } else {
                showOfflineCallback(wv, true)
            }
        }
    }

    fun startRevealGate(tab: SingleTabState) {
        tab.spinnerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        if (tab.isRevealed) {
            // In-tab navigation: use short native transition layer
            tab.inTabTransitioning = true
        } else {
            tab.isRevealed = false
        }
        tab.showSpinner = false

        // Show native spinner ONLY if reveal takes >400ms
        val showSpinnerRunnable = Runnable {
            if (!tab.isRevealed || tab.inTabTransitioning) {
                tab.showSpinner = true
            }
        }
        tab.spinnerTimeoutRunnable = showSpinnerRunnable
        mainHandler.postDelayed(showSpinnerRunnable, 400L)
    }

    fun markVisualReady(tabIndex: Int) {
        val tab = tabStates.getOrNull(tabIndex) ?: return
        tab.spinnerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        tab.spinnerTimeoutRunnable = null
        tab.showSpinner = false
        tab.inTabTransitioning = false
        tab.isRevealed = true
        tab.isFirstLoad = false
    }

    fun createWebViewForTab(tabIndex: Int, ctx: Context): WebView {
        val tab = tabStates[tabIndex]
        tab.webView?.let { return it }

        val newWv = WebView(ctx).apply {
            setBackgroundColor(AndroidColor.parseColor("#060B15"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isLongClickable = true
            setOnLongClickListener {
                val hit = hitTestResult
                val isEditText = hit?.type == WebView.HitTestResult.EDIT_TEXT_TYPE
                val currentUrl = url?.lowercase().orEmpty()
                val isAllowedPage = currentUrl.contains("/login") || currentUrl.contains("/auth") ||
                                    currentUrl.contains("/signin") || currentUrl.contains("/signup") ||
                                    currentUrl.contains("/register") || currentUrl.contains("/learning") ||
                                    currentUrl.contains("/notes") || currentUrl.contains("/study") ||
                                    currentUrl.contains("/account")
                if (isEditText || isAllowedPage) false else true
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                loadsImagesAutomatically = true
                blockNetworkImage = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                // Add WisdomTowerApp token so the Next.js server recognizes native app
                userAgentString = "${settings.userAgentString} WisdomTowerApp wta-native"
            }

            val cookieMgr = CookieManager.getInstance()
            cookieMgr.setAcceptCookie(true)
            cookieMgr.setAcceptThirdPartyCookies(this, true)

            // Document-start CSS injection
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                try {
                    WebViewCompat.addDocumentStartJavaScript(
                        this,
                        DOCUMENT_START_JS,
                        ALLOWED_ORIGIN_RULES
                    )
                } catch (_: Exception) {}
            }

            addJavascriptInterface(object {
                @JavascriptInterface
                fun isPdfCached(url: String?): Boolean {
                    if (url.isNullOrBlank()) return false
                    return OfflineVault.has(ctx, url)
                }

                @JavascriptInterface
                fun getPdfSize(url: String?): Long {
                    if (url.isNullOrBlank()) return 0L
                    val clean = OfflineVault.normalizeUrl(url)
                    val known = OfflineVault.getPdfSize(ctx, clean)
                    if (known > 0L) return known
                    val knownOrig = OfflineVault.getPdfSize(ctx, url)
                    if (knownOrig > 0L) return knownOrig
                    val probed = OfflineVault.probeSizeOnline(ctx, clean)
                    if (probed > 0L) return probed
                    return 0L
                }

                @JavascriptInterface
                fun getPdfSizeByTitle(title: String?): Long {
                    if (title.isNullOrBlank()) return 0L
                    return OfflineVault.getPdfSizeByTitle(title)
                }

                @JavascriptInterface
                fun markDownloadStarted(url: String?) {
                    if (!url.isNullOrBlank()) {
                        startedDownloads.add(url.trim())
                        startedDownloads.add(OfflineVault.normalizeUrl(url))
                    }
                }

                @JavascriptInterface
                fun isDownloadStarted(url: String?): Boolean {
                    if (url.isNullOrBlank()) return false
                    return startedDownloads.contains(url.trim()) ||
                           startedDownloads.contains(OfflineVault.normalizeUrl(url))
                }

                @JavascriptInterface
                fun getDownloadProgress(url: String?): String {
                    val clean = url?.trim().orEmpty()
                    val p = if (clean.isNotBlank()) {
                        OfflineVault.getDownloadProgress(clean)
                            ?: OfflineVault.getDownloadProgress(OfflineVault.normalizeUrl(clean))
                            ?: OfflineVault.getActiveProgressAny()
                    } else {
                        OfflineVault.getActiveProgressAny()
                    } ?: return "{\"loaded\":0,\"total\":0}"
                    return "{\"loaded\":${p.loaded},\"total\":${p.total}}"
                }

                @JavascriptInterface
                fun onRouteChanged(path: String?) {
                    if (!path.isNullOrBlank()) {
                        mainHandler.post {
                            onRouteChangedCallback(path)
                        }
                    }
                }

                @JavascriptInterface
                fun onUserLogin(userId: String? = null) {
                    mainHandler.post {
                        onUserLoginCallback(userId)
                    }
                }

                @JavascriptInterface
                fun getOfflinePdfUrl(url: String?): String {
                    if (url.isNullOrBlank()) return ""
                    val f = OfflineVault.localFileFor(ctx, url) ?: return ""
                    return OfflineVault.fileUrl(f)
                }

                @JavascriptInterface
                fun openCachedPdf(url: String?) {
                    if (url.isNullOrBlank()) return
                    mainHandler.post {
                        openOrDownloadPdfCallback(this@apply, ctx, url)
                    }
                }

                @JavascriptInterface
                fun showToast(message: String?) {
                    if (message.isNullOrBlank()) return
                    mainHandler.post {
                        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                    }
                }

                @JavascriptInterface
                fun notifyLoadingStarted(msg: String?) {
                    mainHandler.post {
                        startRevealGate(tab)
                    }
                }

                @JavascriptInterface
                fun notifyLoadingFinished() {
                    mainHandler.post {
                        markVisualReady(tabIndex)
                    }
                }

                @JavascriptInterface
                fun notifyVisualCommitReady() {
                    mainHandler.post {
                        markVisualReady(tabIndex)
                    }
                }

                @JavascriptInterface
                fun reloadLastOnlinePage() {
                    mainHandler.post {
                        reloadActiveTab(tabIndex, true)
                    }
                }

                @JavascriptInterface
                fun showOfflinePage() {
                    mainHandler.post {
                        val cur = url.orEmpty()
                        if (cur.contains("offline.html") || cur.startsWith("file://")) return@post
                        showOfflineCallback(this@apply, false)
                    }
                }

                @JavascriptInterface
                fun syncStudyTimer(isRunning: Boolean, secondsLeft: Int, totalSeconds: Int, title: String?) {
                    mainHandler.post {
                        syncStudyTimerCallback(isRunning, secondsLeft, totalSeconds, title)
                    }
                }

                @JavascriptInterface
                fun notifyPlannerTask(taskName: String?, dueTime: String?, studentName: String?) {
                    if (!taskName.isNullOrBlank()) {
                        mainHandler.post {
                            notifyPlannerTaskCallback(taskName, dueTime, studentName)
                        }
                    }
                }

                @JavascriptInterface
                fun notifyDailyStudyGoal() {
                    mainHandler.post {
                        notifyDailyStudyGoalCallback()
                    }
                }

                @JavascriptInterface
                fun openNotificationSettings() {
                    mainHandler.post {
                        openNotificationSettingsCallback()
                    }
                }

                @JavascriptInterface
                fun returnToStudyPage() {
                    mainHandler.post {
                        navigateTo(TAB_DEFAULT_URLS[TAB_LEARNING], TAB_LEARNING)
                    }
                }

                @JavascriptInterface
                fun isNativeApp(): Boolean = true
            }, "AndroidOfflineVault")

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    val msg = consoleMessage?.message().orEmpty()
                    val line = consoleMessage?.lineNumber() ?: 0
                    val src = consoleMessage?.sourceId().orEmpty()
                    android.util.Log.d("WTA_TAB_$tabIndex", "[$src:$line] $msg")
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    val v = view ?: return
                    startRevealGate(tab)
                    // Fallback CSS injection if DOCUMENT_START_SCRIPT is not supported
                    if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        v.evaluateJavascript(DOCUMENT_START_JS, null)
                    }
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    val v = view ?: return
                    val u = url.orEmpty()
                    if (u.isNotBlank()) {
                        try {
                            val uri = Uri.parse(u)
                            if (isOfflineUri(uri)) {
                                showOfflineCallback(v, true)
                                return
                            }
                        } catch (_: Exception) {}
                    }

                    // 2 animation frames reveal gate callback
                    v.post {
                        v.evaluateJavascript(
                            "(function(){requestAnimationFrame(function(){requestAnimationFrame(function(){" +
                            "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.notifyVisualCommitReady==='function'){" +
                            "window.AndroidOfflineVault.notifyVisualCommitReady();" +
                            "}" +
                            "});});})();",
                            null
                        )
                    }

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
                        try {
                            WebViewCompat.postVisualStateCallback(v, System.currentTimeMillis()) { _ ->
                                mainHandler.post { markVisualReady(tabIndex) }
                            }
                        } catch (_: Exception) {}
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val v = view ?: return
                    val u = url ?: v.url.orEmpty()
                    if (u.isNotBlank() && !u.startsWith("file://") && !u.contains("offline.html")) {
                        tab.lastOnlineUrl = u
                        tab.lastTargetUrl = u
                    }
                    markVisualReady(tabIndex)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame != true) return
                    val v = view ?: return
                    val failedUrl = request.url?.toString().orEmpty()
                    if (failedUrl.startsWith("file:///android_asset/")) return
                    if (!WebCacheVault.has(ctx, failedUrl)) {
                        showOfflineCallback(v, true)
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame != true) return
                    val v = view ?: return
                    val statusCode = errorResponse?.statusCode ?: 0
                    val failedUrl = request.url?.toString().orEmpty()
                    if (failedUrl.startsWith("file:///android_asset/")) return
                    if (statusCode in listOf(404, 500, 502, 503, 504)) {
                        if (!WebCacheVault.has(ctx, failedUrl)) {
                            showOfflineCallback(v, true)
                        }
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    handler?.cancel()
                    val v = view ?: return
                    showOfflineCallback(v, true)
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    val v = view ?: return true
                    try {
                        (v.parent as? ViewGroup)?.removeView(v)
                        v.destroy()
                    } catch (_: Exception) {}
                    mainHandler.post {
                        tab.webView = null
                        val recreated = createWebViewForTab(tabIndex, ctx)
                        val reloadUrl = tab.lastOnlineUrl.ifBlank { tab.defaultUrl }
                        recreated.loadUrl(reloadUrl)
                    }
                    return true
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val u = request?.url?.toString() ?: return false
                    val v = view ?: return false
                    val uri = request.url ?: return false

                    if (isOfflineUri(uri)) {
                        showOfflineCallback(v, true)
                        return true
                    }

                    if (isLearningToolUri(uri)) {
                        val toolTitle = when {
                            u.contains("tool=tutor") -> "AI Tutor"
                            u.contains("tool=calc") -> "Scientific Calculator"
                            u.contains("tool=note") -> "Study Notebook"
                            u.contains("tool=time") -> "Study Timer"
                            u.contains("tool=plan") -> "Study Planner"
                            else -> "Study Tool"
                        }
                        openToolOverlayCallback(u, toolTitle)
                        return true
                    }

                    if (isPdfUri(uri)) {
                        openOrDownloadPdfCallback(v, ctx, u)
                        return true
                    }

                    val host = uri.host?.lowercase().orEmpty()
                    if (!isAllowedDomain(host) && !u.startsWith("file://") &&
                        (u.startsWith("http://") || u.startsWith("https://") ||
                         u.startsWith("tg:") || u.startsWith("tel:") || u.startsWith("mailto:"))
                    ) {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (_: Exception) {}
                        return true
                    }

                    return false
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val req = request ?: return null
                    val u = req.url?.toString() ?: return null
                    val uri = req.url ?: return null
                    val method = req.method?.uppercase() ?: "GET"
                    val isGet = method == "GET"
                    val isHead = method == "HEAD"
                    val cleanLower = u.lowercase()

                    // Brand assets local interception (0ms)
                    if (isGet && (cleanLower.contains("animation.gif") || cleanLower.contains("brand/animation.gif"))) {
                        try {
                            val stream = ctx.assets.open("brand/animation.gif")
                            val headers = mapOf(
                                "Access-Control-Allow-Origin" to "*",
                                "Cache-Control" to "public, max-age=31536000"
                            )
                            return WebResourceResponse("image/gif", null, 200, "OK", headers, stream)
                        } catch (_: Exception) {
                            val bytes = BrandBytes.gif(ctx)
                            if (bytes.isNotEmpty()) {
                                return WebResourceResponse("image/gif", null, 200, "OK", mapOf("Access-Control-Allow-Origin" to "*"), ByteArrayInputStream(bytes))
                            }
                        }
                    }

                    if (isGet && (cleanLower.contains("logo.png") || cleanLower.contains("brand/logo.png"))) {
                        try {
                            val stream = ctx.assets.open("brand/logo.png")
                            val headers = mapOf(
                                "Access-Control-Allow-Origin" to "*",
                                "Cache-Control" to "public, max-age=31536000"
                            )
                            return WebResourceResponse("image/png", null, 200, "OK", headers, stream)
                        } catch (_: Exception) {
                            val bytes = BrandBytes.logo(ctx)
                            if (bytes.isNotEmpty()) {
                                return WebResourceResponse("image/png", null, 200, "OK", mapOf("Access-Control-Allow-Origin" to "*"), ByteArrayInputStream(bytes))
                            }
                        }
                    }

                    // PDF probe and download interception
                    if ((isGet || isHead) && isPdfUri(uri)) {
                        val rangeHeader = req.requestHeaders?.entries?.firstOrNull { it.key.equals("Range", ignoreCase = true) }?.value
                        val isRangeProbe = rangeHeader != null && rangeHeader.matches(Regex("""bytes=\s*0-\s*[01]"""))
                        val isExplicitDownload = startedDownloads.contains(u.trim())
                        val isProbe = isHead || isRangeProbe

                        if (isProbe) {
                            val local = OfflineVault.localFileFor(ctx, u)
                            var size = if (local != null && local.exists() && local.length() > 0L) {
                                local.length()
                            } else {
                                OfflineVault.getPdfSize(ctx, u)
                            }
                            if (size <= 0L) size = OfflineVault.getPdfSize(ctx, OfflineVault.normalizeUrl(u))
                            if (size <= 0L) size = OfflineVault.probeSizeOnline(ctx, u)

                            val headers = HashMap<String, String>().apply {
                                put("Access-Control-Allow-Origin", "*")
                                put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                                put("Access-Control-Allow-Headers", "*")
                                put("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges, Content-Type")
                                put("Content-Type", "application/pdf")
                                put("Accept-Ranges", "bytes")
                                if (size > 0L) {
                                    put("Content-Length", if (isHead) size.toString() else "1")
                                    put("Content-Range", "bytes 0-0/$size")
                                }
                                put("Cache-Control", "public, max-age=300")
                            }
                            val status = if (size > 0L) (if (isHead) 200 else 206) else 200
                            val statusText = if (size > 0L && !isHead) "Partial Content" else "OK"
                            return WebResourceResponse(
                                "application/pdf",
                                null,
                                status,
                                statusText,
                                headers,
                                ByteArrayInputStream(if (size > 0L && !isHead) ByteArray(1) else ByteArray(0))
                            )
                        }

                        val local = OfflineVault.localFileFor(ctx, u)
                        if (local != null && local.exists() && local.length() > 0L) {
                            return try {
                                val headers = mapOf(
                                    "Access-Control-Allow-Origin" to "*",
                                    "Access-Control-Allow-Methods" to "GET, HEAD, OPTIONS",
                                    "Content-Type" to "application/pdf",
                                    "Content-Length" to local.length().toString(),
                                    "Cache-Control" to "public, max-age=31536000, immutable"
                                )
                                WebResourceResponse("application/pdf", null, 200, "OK", headers, FileInputStream(local))
                            } catch (_: Exception) { null }
                        }
                    }

                    // Intercept /offline path
                    if (isOfflineUri(uri)) {
                        try {
                            val stream = ctx.assets.open("offline.html")
                            val headers = mapOf(
                                "Content-Type" to "text/html; charset=utf-8",
                                "Cache-Control" to "no-cache, no-store, must-revalidate"
                            )
                            return WebResourceResponse("text/html", "utf-8", 200, "OK", headers, stream)
                        } catch (_: Exception) {}
                    }

                    // WebCacheVault cached response
                    if (WebCacheVault.has(ctx, u)) {
                        val cached = WebCacheVault.getCachedResponse(ctx, u)
                        if (cached != null) return cached
                    }

                    return super.shouldInterceptRequest(view, request)
                }
            }

            setDownloadListener { url, _, _, _, _ ->
                openOrDownloadPdfCallback(this, ctx, url)
            }

            loadUrl(tab.lastTargetUrl)
        }

        tab.webView = newWv
        return newWv
    }
}

/**
 * Native Skeleton UI shown behind the solid navy layer on the first uncached load.
 */
@Composable
internal fun NativeTabSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonPulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060B15))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C1424).copy(alpha = pulseAlpha))
                .border(1.dp, Color(0x1F22E0FF), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x3322E0FF))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x1F22E0FF))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x1F22E0FF))
                )
            }
        }

        // Section Title Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x2E22E0FF).copy(alpha = pulseAlpha))
        )

        // 3 Structured Item Cards
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0C1424).copy(alpha = pulseAlpha))
                    .border(1.dp, Color(0x1F22E0FF), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x2222E0FF))
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3322E0FF))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x1F22E0FF))
                    )
                }
            }
        }
    }
}

/**
 * Tab Reveal Gate:
 * Holds the page behind solid navy (#060B15) until onPageCommitVisible + 2 frames,
 * showing native skeleton on first load, native spinner only if load takes >400ms,
 * and crossfading in over ~150ms.
 */
@Composable
private fun TabRevealGate(
    isRevealed: Boolean,
    inTabTransitioning: Boolean,
    isFirstLoad: Boolean,
    showSpinner: Boolean,
    modifier: Modifier = Modifier
) {
    val shouldShowCover = !isRevealed || inTabTransitioning

    val gateAlpha by animateFloatAsState(
        targetValue = if (shouldShowCover) 1f else 0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "gateAlpha"
    )

    if (gateAlpha > 0.01f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .alpha(gateAlpha)
                .background(Color(0xFF060B15))
                .zIndex(50f),
            contentAlignment = Alignment.Center
        ) {
            if (isFirstLoad && !showSpinner) {
                NativeTabSkeleton()
            } else if (showSpinner) {
                CustomCenteredLoader()
            }
        }
    }
}

/**
 * TabWebViewHost composable:
 * Holds all 5 tab WebViews persistent in memory,
 * switching between them with an alpha crossfade over ~150ms,
 * keeping DOM/scroll/form state alive,
 * and gated by TabRevealGate.
 */
@Composable
fun TabWebViewHost(
    state: TabWebViewHostState,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Register ComponentCallbacks2 for memory trims and LRU eviction
    DisposableEffect(context, selectedIndex) {
        val callbacks = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                state.onTrimMemory(level, selectedIndex)
            }
            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
            override fun onLowMemory() {
                state.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, selectedIndex)
            }
        }
        context.applicationContext.registerComponentCallbacks(callbacks)
        onDispose {
            context.applicationContext.unregisterComponentCallbacks(callbacks)
        }
    }

    // Lazily instantiate selected tab
    LaunchedEffect(selectedIndex) {
        state.recordTabAccess(selectedIndex)
        if (state.tabStates[selectedIndex].webView == null) {
            state.createWebViewForTab(selectedIndex, context)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        state.tabStates.forEachIndexed { index, tabState ->
            val isActive = selectedIndex == index
            val tabAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec = tween(durationMillis = 150),
                label = "tabCrossfadeAlpha_$index"
            )

            // Compose tab if it has been instantiated
            val wv = tabState.webView
            if (wv != null && (tabAlpha > 0.005f || isActive)) {
                key(index) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(tabAlpha)
                            .zIndex(if (isActive) 10f else 1f)
                    ) {
                        AndroidView(
                            factory = {
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                wv
                            },
                            update = { view ->
                                view.visibility = if (tabAlpha > 0.005f) View.VISIBLE else View.INVISIBLE
                                view.isEnabled = isActive
                                view.isClickable = isActive
                                view.isFocusable = isActive
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Reveal Gate overlay for this tab
                        TabRevealGate(
                            isRevealed = tabState.isRevealed,
                            inTabTransitioning = tabState.inTabTransitioning,
                            isFirstLoad = tabState.isFirstLoad,
                            showSpinner = tabState.showSpinner,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
