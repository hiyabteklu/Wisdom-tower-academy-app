package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

sealed class BottomNavItem(val title: String, val icon: ImageVector, val url: String) {
    object Home : BottomNavItem("Home", Icons.Filled.Home, "https://wisdom-tower-academy.live/")
    object Learning : BottomNavItem("Learning", Icons.Filled.MenuBook, "https://wisdom-tower-academy.live/learning")
    object Packages : BottomNavItem("Packages", Icons.Filled.ViewList, "https://wisdom-tower-academy.live/packages")
    object Account : BottomNavItem("Account", Icons.Filled.Person, "https://wisdom-tower-academy.live/account")
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private val mainHandler = Handler(Looper.getMainLooper())

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Learning,
        BottomNavItem.Packages,
        BottomNavItem.Account
    )

    var selectedIndex by remember { mutableIntStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }

    val activity = context as? ComponentActivity
    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            activity?.finish()
        }
    }

    fun openOrDownloadPdf(wv: WebView, ctx: Context, url: String) {
        val local = OfflineVault.localFileFor(ctx, url)
            ?: OfflineVault.localFileFor(ctx, url.substringBefore("?"))
        if (local != null) {
            wv.loadUrl(OfflineVault.fileUrl(local))
            return
        }
        if (!isOnline(ctx)) {
            mainHandler.post {
                Toast.makeText(ctx, "Book not saved offline yet. Open it once online.", Toast.LENGTH_LONG).show()
            }
            return
        }
        mainHandler.post {
            Toast.makeText(ctx, "Saving book for offline…", Toast.LENGTH_SHORT).show()
        }
        OfflineVault.downloadAsync(ctx, url) { file ->
            mainHandler.post {
                if (file != null) {
                    Toast.makeText(ctx, "Saved offline", Toast.LENGTH_SHORT).show()
                    wv.loadUrl(OfflineVault.fileUrl(file))
                } else {
                    wv.loadUrl(url)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            val wv = webView ?: return@NavigationBarItem
                            wv.settings.cacheMode = if (isOnline(context)) {
                                WebSettings.LOAD_DEFAULT
                            } else {
                                WebSettings.LOAD_CACHE_ELSE_NETWORK
                            }
                            wv.loadUrl(item.url)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF38BDF8),
                            selectedTextColor = Color(0xFF38BDF8),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF1E293B)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = if (isOnline(ctx)) {
                                WebSettings.LOAD_DEFAULT
                            } else {
                                WebSettings.LOAD_CACHE_ELSE_NETWORK
                            }
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            allowContentAccess = true
                            offscreenPreRaster = true
                            userAgentString =
                                userAgentString + " WisdomTowerApp/1.0 Capacitor/Equivalent"
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        setDownloadListener(DownloadListener { url, _, contentDisposition, mimeType, _ ->
                            val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
                            mainHandler.post {
                                Toast.makeText(ctx, "Saving for offline: $name", Toast.LENGTH_SHORT).show()
                            }
                            OfflineVault.downloadAsync(ctx, url, name) { file ->
                                mainHandler.post {
                                    if (file != null) {
                                        Toast.makeText(ctx, "Saved offline", Toast.LENGTH_SHORT).show()
                                        if (name.endsWith(".pdf", true) || mimeType?.contains("pdf") == true) {
                                            loadUrl(OfflineVault.fileUrl(file))
                                        }
                                    } else {
                                        Toast.makeText(ctx, "Could not save file", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (OfflineVault.isPdfUrl(url)) {
                                    view?.let { openOrDownloadPdf(it, ctx, url) }
                                    return true
                                }
                                if (!isOnline(ctx)) {
                                    val local = OfflineVault.localFileFor(ctx, url)
                                    if (local != null) {
                                        view?.loadUrl(OfflineVault.fileUrl(local))
                                        return true
                                    }
                                }
                                return false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    val failUrl = request.url?.toString()
                                    if (failUrl != null) {
                                        val local = OfflineVault.localFileFor(ctx, failUrl)
                                        if (local != null) {
                                            view?.loadUrl(OfflineVault.fileUrl(local))
                                            return
                                        }
                                    }
                                    view?.loadUrl("file:///android_asset/offline.html")
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                if (failingUrl != null) {
                                    val local = OfflineVault.localFileFor(ctx, failingUrl)
                                    if (local != null) {
                                        view?.loadUrl(OfflineVault.fileUrl(local))
                                        return
                                    }
                                }
                                view?.loadUrl("file:///android_asset/offline.html")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)

                                if (url != null && !url.startsWith("file://")) {
                                    val path = url.substringBefore("?").removeSuffix("/")
                                    selectedIndex = when {
                                        path.contains("/learning") || path.contains("/my-learning") -> 1
                                        path.contains("/packages") -> 2
                                        path.contains("/account") || path.contains("/login") ||
                                            path.contains("/auth") || path.contains("accounts.google") -> 3
                                        else -> 0
                                    }
                                }

                                if (url != null && !url.startsWith("file://")) {
                                    val js =
                                        "(function(){" +
                                        "if(document.getElementById('wta-app-chrome'))return;" +
                                        "var s=document.createElement('style');" +
                                        "s.id='wta-app-chrome';" +
                                        "s.textContent='body>header,body>footer,[data-site-header],[data-site-footer],nav[aria-label=\"Main\"],a[href*=\"/privacy\"],a[href*=\"/terms\"]{display:none!important}';" +
                                        "document.head.appendChild(s);" +
                                        "})();"
                                    view?.evaluateJavascript(js, null)
                                }
                            }
                        }

                        if (isOnline(ctx)) {
                            loadUrl(items[0].url)
                        } else {
                            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                            loadUrl(items[0].url)
                        }
                        webView = this
                    }
                },
                update = { }
            )
        }
    }
}
