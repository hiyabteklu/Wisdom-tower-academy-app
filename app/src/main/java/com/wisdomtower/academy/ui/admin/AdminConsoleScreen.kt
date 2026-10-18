package com.wisdomtower.academy.ui.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.delay

// ADMIN-native N1 (2026-10-18, docs/ADMIN_NATIVE_PLAN.md): the function in this file was renamed from
// AdminConsoleScreen to AdminWebSection and made internal. The public AdminConsoleScreen (same signature, the one
// MainActivity.kt calls) is now in AdminConsoleHost.kt: native home grid first, this WebView only for modules that
// have no native screen yet. Nothing else in this file changed.
//
// Admin slice A3 (docs/ADMIN_MOBILE_PLAN.md section 5, findings F1 to F7 in docs/SESSION_2026-10-15_ADMIN_A0.md).
// The Admin Console page hosted in its OWN small WebView, meant to sit between the app top bar and the bottom
// navigation (same idea as CompetitionFullScreen), instead of the shared tool overlay.
// What this WebView deliberately does NOT do: it never runs EARLY_HIDE_CHROME_JS, NATIVE_CHROME_JS,
// AI_TUTOR_CHROME_JS, PRECACHE_AND_UNBLOCK_JS or AUTH_BRIDGE_JS. So no header, footer or nav element of the admin page
// is hidden (F2), no close button is hijacked (F3), no observer and no polling timer runs (F4), and text selection
// and copy are left alone (F5).
// What it does: injects the signed-in session (AuthStateManager.injectSessionIntoWebView, like the main WebView),
// keeps pinch zoom OFF (F6), turns the over-scroll glow and the sideways scroll bar off, lets only admin pages
// of the website load inside it, hands every other website link to the app (onLeaveToApp) and every outside link to
// the system browser. Back goes back inside the page (sections are opened with pushState by slice A1), and when
// there is no page history left it calls onClose.
// Not here: the native top bar and the close control (the call site owns them), the call site itself (a patch
// script for MainActivity.kt, next slice), a file chooser (no WebView in this app has one today, so uploads in
// admin panels are not possible yet).
//
// 2026-10-16 (owner report: "the scroll does not start unless the finger stays down a moment; a quick swipe is
// lost", the same symptom as the notes reader in docs/SESSION_2026-10-12_AR.md, fixed there by ShellPause.kt).
// The same two measures are now applied here, in this file only (no MainActivity.kt change):
//  - F1c: a touch listener on the admin WebView that, on the first finger-down, asks every parent not to
//    intercept the gesture (requestDisallowInterceptTouchEvent(true)); it never consumes a touch.
//  - F1b: while the admin is shown, every OTHER visible WebView that already has a page (the big website WebView
//    under this screen) is set INVISIBLE and paused, and shown and resumed again when this screen leaves. The
//    admin page is opaque, so nothing visible changes. pauseTimers() is NOT used (it is global and would stop this
//    WebView too). A 1.5 second check pauses them again, because MainActivity resumes its WebView when the app
//    comes back from the background.
// Status: written, compiles only after CI, not seen on a device. Cause of the lost quick swipe is a hypothesis.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
private const val ADMIN_WEBVIEW_TAG = "wta-admin-webview"

private fun allWebViews(root: View): List<WebView> {
    val found = ArrayList<WebView>()
    fun walk(v: View) {
        if (v is WebView) {
            found.add(v)
            return
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
    }
    walk(root)
    return found
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun AdminWebSection(
    url: String,
    onClose: () -> Unit,
    onLeaveToApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val webRef = remember { arrayOfNulls<WebView>(1) }
    val hidden = remember { ArrayList<WebView>() }
    val closeNow = rememberUpdatedState(onClose)
    val leaveNow = rememberUpdatedState(onLeaveToApp)

    BackHandler {
        val web = webRef[0]
        if (web != null && web.canGoBack()) web.goBack() else closeNow.value()
    }

    // F1b: quiet the website WebView under this screen while the admin is open.
    DisposableEffect(view) {
        for (wv in allWebViews(view.rootView)) {
            try {
                if (wv.tag == ADMIN_WEBVIEW_TAG || wv.url == null) continue
                if (wv.visibility == View.VISIBLE) {
                    wv.visibility = View.INVISIBLE
                    hidden.add(wv)
                }
                wv.onPause()
            } catch (_: Exception) {
            }
        }
        onDispose {
            for (wv in hidden) {
                try {
                    wv.visibility = View.VISIBLE
                    wv.onResume()
                } catch (_: Exception) {
                }
            }
            hidden.clear()
        }
    }

    // MainActivity calls onResume() on its WebView when the app returns from the background: pause again.
    LaunchedEffect(view) {
        while (true) {
            delay(1500L)
            for (wv in hidden) {
                try {
                    wv.onPause()
                } catch (_: Exception) {
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                tag = ADMIN_WEBVIEW_TAG
                setBackgroundColor(0xFF0A101D.toInt())
                overScrollMode = View.OVER_SCROLL_NEVER
                isHorizontalScrollBarEnabled = false
                // F1c: never let a parent take over the first touch of a scroll.
                setOnTouchListener { v, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    false
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = false
                settings.userAgentString = settings.userAgentString + " WisdomTowerApp wta-native"
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, pageUrl: String?, favicon: Bitmap?) {
                        if (pageUrl != null && !pageUrl.startsWith("file://")) {
                            AuthStateManager.injectSessionIntoWebView(view)
                        }
                    }

                    override fun onPageFinished(view: WebView, pageUrl: String?) {
                        if (pageUrl != null && !pageUrl.startsWith("file://")) {
                            AuthStateManager.injectSessionIntoWebView(view)
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val target = request.url
                        val host = target.host ?: ""
                        val path = target.path ?: ""
                        val ours = host == "wisdom-tower-academy.live" || host.endsWith(".wisdom-tower-academy.live")
                        if (ours && path.startsWith("/admin")) return false
                        if (ours) {
                            leaveNow.value(target.toString())
                            return true
                        }
                        try {
                            val open = Intent(Intent.ACTION_VIEW, Uri.parse(target.toString()))
                            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            view.context.startActivity(open)
                        } catch (e: Exception) {
                            // no app can open the link: ignore it
                        }
                        return true
                    }
                }
                webRef[0] = this
                loadUrl(url)
            }
        },
        onRelease = {
            if (webRef[0] === it) webRef[0] = null
            it.destroy()
        }
    )
}
