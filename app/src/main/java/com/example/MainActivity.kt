package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FLAG_SECURE temporarily disabled for debugging screenshots
        // window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainScreen() {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Learning,
        BottomNavItem.Packages,
        BottomNavItem.Account
    )

    var selectedIndex by remember { mutableIntStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
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
                            webView?.loadUrl(item.url)
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
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Do NOT use LAYER_TYPE_SOFTWARE — it causes extreme scroll lag on real devices.

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            userAgentString =
                                userAgentString + " WisdomTowerApp/1.0 Capacitor/Equivalent"
                        }

                        // Cookies for Google / site login
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)

                                url?.let { u ->
                                    val path = u.substringBefore("?").removeSuffix("/")
                                    selectedIndex = when {
                                        path.contains("/learning") || path.contains("/my-learning") -> 1
                                        path.contains("/packages") -> 2
                                        path.contains("/account") || path.contains("/login") ||
                                            path.contains("/auth") || path.contains("accounts.google") -> 3
                                        else -> 0
                                    }
                                }

                                val js = """
                                    (function() {
                                        if (document.getElementById('wta-app-chrome')) return;
                                        var s = document.createElement('style');
                                        s.id = 'wta-app-chrome';
                                        s.textContent = `
                                          header, footer,
                                          #header, #footer,
                                          [data-site-header], [data-site-footer],
                                          a[href*="terms"], a[href*="privacy"] {
                                            display: none !important;
                                          }
                                          body { padding: 0 !important; margin: 0 !important; }
                                        `;
                                        document.head.appendChild(s);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(js, null)
                            }
                        }

                        loadUrl(items[0].url)
                        webView = this
                    }
                },
                update = { }
            )
        }
    }
}
