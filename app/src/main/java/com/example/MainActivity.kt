package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

private val BarBg = Color(0xFF0F172A)
private val Accent = Color(0xFF00E5FF)
private val Surface = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)
private val LoaderCard = Color(0xE6111827)

private const val OFFLINE_ASSET = "file:///android_asset/offline.html"
private const val SITE = "https://wisdom-tower-academy.live/"
private const val MIN_SPLASH_MS = 4500L

/**
 * Hide site chrome + site spinners, force lazy images so navigation
 * is not blocked while thumbnails download (same feel as mobile web).
 */
private const val NATIVE_CHROME_JS =
    "(function(){try{" +
        "document.documentElement.classList.add('wta-native-app');" +
        "if(document.body){document.body.classList.add('wta-native-app');document.body.style.pointerEvents='auto';}" +
        "var id='wta-app-chrome';var s=document.getElementById(id);" +
        "if(!s){s=document.createElement('style');s.id=id;document.documentElement.appendChild(s);}" +
        "s.textContent=" +
        "'header,[data-site-header],footer,[data-site-footer],.site-header,.site-footer{display:none!important;}" +
        "html.wta-native-app,body.wta-native-app{overscroll-behavior:none;}" +
        ".loading,.spinner,.loader,[class*=\"spinner\"],[class*=\"loading\"]," +
        "[aria-busy=true],.animate-spin,.nprogress,.bar-loader," +
        ".MuiCircularProgress-root,[data-loading],.progress-circle{display:none!important;visibility:hidden!important;}" +
        "header,[data-site-header],footer,[data-site-footer],.site-header,.site-footer," +
        "nav[role=navigation],.bottom-nav,.app-chrome{-webkit-user-select:none!important;-webkit-touch-callout:none!important;user-select:none!important;}'" +
        ";" +
        "document.querySelectorAll('img').forEach(function(img){" +
        "if(!img.getAttribute('loading'))img.setAttribute('loading','lazy');" +
        "if(!img.getAttribute('decoding'))img.setAttribute('decoding','async');" +
        "});" +
        "}catch(e){}})();"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        enableEdgeToEdge()

        val navy = AndroidColor.parseColor("#0F172A")
        window.statusBarColor = navy
        window.navigationBarColor = navy
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false

        setContent {
            MyApplicationTheme {
                MainScreen(onReady = { keepSplash = false })
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

private data class MenuLink(
    val label: String,
    val url: String,
    val icon: ImageVector,
    val external: Boolean = false,
)

private val overflowMenuLinks = listOf(
    MenuLink("Settings", "https://wisdom-tower-academy.live/settings", Icons.Filled.Settings),
    MenuLink("My account", "https://wisdom-tower-academy.live/account", Icons.Filled.Person),
    MenuLink("Sign out", "https://wisdom-tower-academy.live/logout", Icons.AutoMirrored.Filled.Logout),
    MenuLink("About", "https://wisdom-tower-academy.live/about", Icons.Filled.Info),
    MenuLink("Contact us", "https://wisdom-tower-academy.live/contact", Icons.Outlined.Email),
    MenuLink("FAQ", "https://wisdom-tower-academy.live/academy/faq", Icons.Outlined.HelpOutline),
    MenuLink("Wisdom Digital", "https://wisdomtower.tech", Icons.AutoMirrored.Filled.OpenInNew, external = true),
    MenuLink("Telegram group", "https://t.me/wisdom_tower1", Icons.AutoMirrored.Filled.Send, external = true),
    MenuLink("Telegram channel", "https://t.me/wisdom_tower2", Icons.Filled.Campaign, external = true),
    MenuLink("LinkedIn", "https://www.linkedin.com/company/wisdom-tower/", Icons.Filled.Business, external = true),
    MenuLink("Privacy", "https://wisdom-tower-academy.live/privacy", Icons.Outlined.PrivacyTip),
    MenuLink("Terms", "https://wisdom-tower-academy.live/terms", Icons.Outlined.Policy),
)

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
fun MainScreen(onReady: () -> Unit = {}) {
    val context = LocalContext.current
    val view = LocalView.current
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Learning,
        BottomNavItem.Packages,
        BottomNavItem.Account
    )

    var selectedIndex by remember { mutableIntStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pageLoading by remember { mutableStateOf(true) }
    var largeLoader by remember { mutableStateOf(true) }
    var lastResumeRefreshAt by remember { mutableLongStateOf(0L) }
    var splashHoldDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onReady()
        delay(MIN_SPLASH_MS)
        splashHoldDone = true
        if (largeLoader) {
            pageLoading = false
            largeLoader = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val wv = webView ?: return@LifecycleEventObserver
            if (!isOnline(context)) return@LifecycleEventObserver
            val url = wv.url ?: return@LifecycleEventObserver
            if (url.startsWith("file://")) return@LifecycleEventObserver
            val now = System.currentTimeMillis()
            if (now - lastResumeRefreshAt < 4000L) return@LifecycleEventObserver
            lastResumeRefreshAt = now
            wv.evaluateJavascript(
                "(function(){try{window.dispatchEvent(new CustomEvent('wta-refresh',{detail:{source:'app-resume'}}));}catch(e){}})();",
                null
            )
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activity = context as? ComponentActivity
    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            activity?.finish()
        }
    }

    fun finishLoadingIfAllowed() {
        if (largeLoader) {
            if (splashHoldDone) {
                pageLoading = false
                largeLoader = false
            }
        } else {
            pageLoading = false
        }
    }

    fun showOffline(wv: WebView) {
        pageLoading = false
        largeLoader = false
        wv.loadUrl(OFFLINE_ASSET)
    }

    fun navigateTo(url: String, tabIndex: Int? = null) {
        val wv = webView ?: return
        if (tabIndex != null) selectedIndex = tabIndex
        largeLoader = false
        pageLoading = true
        if (!isOnline(context)) {
            wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            wv.loadUrl(url)
            return
        }
        wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
        wv.loadUrl(url)
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
            Toast.makeText(ctx, "Saving book for offline\u2026", Toast.LENGTH_SHORT).show()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BarBg)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            containerColor = BarBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BarBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    menuExpanded = true
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu",
                                    tint = Accent
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                containerColor = BarBg,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.widthIn(min = 260.dp)
                            ) {
                                overflowMenuLinks.forEach { link ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = link.label,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = link.icon,
                                                contentDescription = null,
                                                tint = if (link.label == "Sign out") Color(0xFFF87171) else Accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            if (link.external) {
                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                                    )
                                                } catch (_: Exception) {
                                                    navigateTo(link.url)
                                                }
                                            } else {
                                                val tab = when {
                                                    link.url.contains("/account") -> 3
                                                    link.url.contains("/settings") -> 3
                                                    else -> null
                                                }
                                                navigateTo(link.url, tab)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            BrandLogo(size = 34.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wisdom Tower Academy",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                navigateTo("https://wisdom-tower-academy.live/notifications", null)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Accent
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = BarBg,
                    contentColor = Color.White,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, maxLines = 1) },
                            selected = selectedIndex == index,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                if (selectedIndex == index) return@NavigationBarItem
                                selectedIndex = index
                                navigateTo(item.url, index)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = Surface
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.blockNetworkImage = false
                            settings.cacheMode = if (isOnline(ctx)) {
                                WebSettings.LOAD_DEFAULT
                            } else {
                                WebSettings.LOAD_CACHE_ELSE_NETWORK
                            }
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    if (!largeLoader && newProgress >= 55 && pageLoading) {
                                        pageLoading = false
                                    }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    if (url != null && url.startsWith("file:///android_asset/")) {
                                        pageLoading = false
                                        largeLoader = false
                                    } else if (!largeLoader) {
                                        pageLoading = true
                                    }
                                    view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    if (!largeLoader) {
                                        pageLoading = false
                                    }
                                    view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                    finishLoadingIfAllowed()
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        view?.let { showOffline(it) }
                                    }
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    view?.let { showOffline(it) }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val u = request?.url?.toString() ?: return false
                                    if (u.endsWith(".pdf", ignoreCase = true) || u.contains("/pdf")) {
                                        view?.let { openOrDownloadPdf(it, ctx, u) }
                                        return true
                                    }
                                    if (!u.contains("wisdom-tower-academy.live") &&
                                        !u.contains("wisdomtower.tech") &&
                                        !u.startsWith("file://") &&
                                        (u.startsWith("http://") || u.startsWith("https://"))
                                    ) {
                                        try {
                                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                                        } catch (_: Exception) { }
                                        return true
                                    }
                                    return false
                                }
                            }

                            setDownloadListener(DownloadListener { url, _, _, _, _ ->
                                openOrDownloadPdf(this, ctx, url)
                            })

                            if (isOnline(ctx)) {
                                loadUrl(SITE)
                            } else {
                                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                loadUrl(SITE)
                            }
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (pageLoading) {
                    if (largeLoader) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(LoaderCard)
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BrandLoader(size = 180.dp)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(LoaderCard)
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BrandLoader(size = 56.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
