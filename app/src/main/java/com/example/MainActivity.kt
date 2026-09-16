package com.example

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
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
import androidx.compose.foundation.layout.windowInsetsPadding
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
        // Prevent screenshots & screen recording (FLAG_SECURE)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        // Enable edge to edge for proper safe area handling
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
    var currentUrl by remember { mutableStateOf(BottomNavItem.Home.url) }
    var webView: WebView? by remember { mutableStateOf(null) }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Learning,
        BottomNavItem.Packages,
        BottomNavItem.Account
    )

    // Handle system back button to navigate within WebView
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing, // Automatically handle status bar & gesture bar
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A), // Dark navy slate color
                contentColor = Color.White
            ) {
                items.forEach { item ->
                    // Strip query params and trailing slashes for robust matching
                    val normalizedCurrentUrl = currentUrl.substringBefore("?").removeSuffix("/")
                    val normalizedItemUrl = item.url.substringBefore("?").removeSuffix("/")
                    
                    val isSelected = if (item == BottomNavItem.Home) {
                        normalizedCurrentUrl == "https://wisdom-tower-academy.live"
                    } else {
                        normalizedCurrentUrl.startsWith(normalizedItemUrl)
                    }
                        
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = isSelected,
                        onClick = {
                            currentUrl = item.url
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
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK // Aggressive caching
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            
                            // Custom User-Agent so the website can know it's the app
                            userAgentString = userAgentString + " WisdomTowerApp/1.0 Capacitor/Equivalent"
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false // Let the WebView load the URL
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Inject advanced JS to hide header, footer, bottom nav, social links, terms, etc.
                                val jsInjection = """
                                    (function() {
                                        var style = document.createElement('style');
                                        style.innerHTML = `
                                            header, footer, nav,
                                            [class*="header"], [class*="footer"], [class*="nav"],
                                            a[href*="terms"], a[href*="privacy"],
                                            a[href*="facebook"], a[href*="twitter"], a[href*="instagram"], a[href*="linkedin"] {
                                                display: none !important;
                                            }
                                            body {
                                                padding-top: 0 !important;
                                                padding-bottom: 0 !important;
                                                min-height: 100vh !important;
                                            }
                                            main {
                                                min-height: 100vh !important;
                                            }
                                        `;
                                        document.head.appendChild(style);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(jsInjection, null)
                            }
                            
                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                url?.let { currentUrl = it }
                            }
                        }
                        loadUrl(currentUrl)
                        webView = this
                    }
                },
                update = { view ->
                    // Component updates if needed
                }
            )
        }
    }
}
