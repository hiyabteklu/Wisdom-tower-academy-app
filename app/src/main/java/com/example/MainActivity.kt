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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
private val SurfaceColor = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)

private const val OFFLINE_ASSET = "file:///android_asset/offline.html"
// Direct 200 URL (eliminates 308 redirect round-trip delay)
private const val SITE = "https://www.wisdom-tower-academy.live/"
private const val MIN_SPLASH_DISPLAY_MS = 1000L

private const val NATIVE_CHROME_JS =
    "(function(){try{" +
        "document.documentElement.classList.add('wta-native-app');" +
        "if(document.body){document.body.classList.add('wta-native-app');document.body.style.pointerEvents='auto';}" +
        "var id='wta-app-chrome';var s=document.getElementById(id);" +
        "if(!s){s=document.createElement('style');s.id=id;document.documentElement.appendChild(s);}" +
        "s.textContent=" +
        "'header,[data-site-header],footer,[data-site-footer],.site-header,.site-footer," +
        "nav[aria-label=\"Main\"],.hide-on-app{display:none!important;visibility:hidden!important;height:0!important;overflow:hidden!important;}'" +
        ";" +
        "}catch(e){}})();"

private const val PRECACHE_AND_UNBLOCK_JS =
    "(function(){try{" +
        "var imgs=document.querySelectorAll('img[loading=\"lazy\"]');" +
        "for(var i=0;i<imgs.length;i++){imgs[i].removeAttribute('loading');imgs[i].setAttribute('decoding','async');}" +
        "if('serviceWorker' in navigator&&navigator.serviceWorker.controller){" +
            "var links=document.querySelectorAll('a[href^=\"/\"],a[href*=\"wisdom-tower-academy.live\"]');" +
            "var urls=[];" +
            "for(var j=0;j<Math.min(links.length,25);j++){" +
                "var h=links[j].href;" +
                "if(h&&!h.includes('#')&&!h.includes('logout')&&urls.indexOf(h)===-1)urls.push(h);" +
            "}" +
            "if(urls.length>0){" +
                "navigator.serviceWorker.controller.postMessage({type:'PRECACHE_URLS',urls:urls});" +
            "}" +
        "}" +
    "}catch(e){}})();"

private const val BOOK_PAGE_HELPERS_JS =
    "(function(){try{" +
        "if(window.AndroidOfflineVault&&!window.__wta_vault_synced){" +
            "window.__wta_vault_synced=true;" +
        "}" +
        "function formatBytes(bytes){" +
            "if(!bytes||bytes<=0)return '';" +
            "if(bytes<1024*1024)return (bytes/1024).toFixed(1)+' KB';" +
            "return (bytes/(1024*1024)).toFixed(1)+' MB';" +
        "}" +
        "function updatePdfPreOpenLabels(){" +
            "try{" +
                "var textNodes=[];" +
                "var walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT,null,false);" +
                "var n;" +
                "while(n=walker.nextNode()){" +
                    "var val=n.nodeValue||'';" +
                    "if(val.indexOf('Checking size')!==-1||val.indexOf('Checking size…')!==-1||val.trim()==='—'||val.indexOf('· —')!==-1){" +
                        "textNodes.push(n);" +
                    "}" +
                "}" +
                "if(textNodes.length===0)return;" +
                "var cachedSizeFormatted='';" +
                "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.getPdfSize==='function'){" +
                    "var links=document.querySelectorAll('a[href*=\".pdf\"],a[href*=\"/api/content/pdf\"],button[data-url],a[href*=\"/pdf\"]');" +
                    "for(var i=0;i<links.length;i++){" +
                        "var href=links[i].getAttribute('href')||links[i].getAttribute('data-url')||'';" +
                        "if(href){" +
                            "var sz=window.AndroidOfflineVault.getPdfSize(href);" +
                            "if(sz&&sz>0){" +
                                "cachedSizeFormatted=formatBytes(sz);" +
                                "break;" +
                            "}" +
                        "}" +
                    "}" +
                "}" +
                "for(var j=0;j<textNodes.length;j++){" +
                    "var node=textNodes[j];" +
                    "var t=node.nodeValue||'';" +
                    "if(cachedSizeFormatted){" +
                        "if(t.indexOf('· —')!==-1){" +
                            "node.nodeValue=t.replace('· —','· '+cachedSizeFormatted);" +
                        "}else if(t.trim()==='—'){" +
                            "node.nodeValue=cachedSizeFormatted;" +
                        "}else if(t.indexOf('Checking size')!==-1){" +
                            "node.nodeValue=t.replace(/Checking size[….]*/g,cachedSizeFormatted);" +
                        "}" +
                    "}else{" +
                        "var now=Date.now();" +
                        "if(!window.__wta_size_check_start){window.__wta_size_check_start=now;}" +
                        "if(now-window.__wta_size_check_start>3000){" +
                            "if(t.indexOf('Checking size')!==-1){" +
                                "node.nodeValue=t.replace(/Checking size[….]*/g,'Ready to open');" +
                            "}else if(t.trim()==='—'){" +
                                "node.nodeValue='Ready';" +
                            "}else if(t.indexOf('· —')!==-1){" +
                                "node.nodeValue=t.replace('· —','· Ready');" +
                            "}" +
                        "}" +
                    "}" +
                "}" +
            "}catch(e){}" +
        "}" +
        "updatePdfPreOpenLabels();" +
        "if(!window.__wta_size_interval){" +
            "window.__wta_size_interval=setInterval(updatePdfPreOpenLabels,1000);" +
        "}" +
    "}catch(e){}})();"

private const val DETECT_AND_RECOVER_JS =
    "(function(){try{" +
        "if(window.location.protocol==='file:')return;" +
        "function goOffline(){window.location.replace('" + OFFLINE_ASSET + "');}" +
        "var body=document.body;if(!body)return;" +
        "var text=(body.innerText||body.textContent||'').toLowerCase();" +
        "var isErrorShell=false;" +
        "if(text.indexOf('application error')!==-1&&" +
           "(text.indexOf('client-side exception')!==-1||text.indexOf('browser console')!==-1)){" +
            "isErrorShell=true;" +
        "}else if(document.querySelector('div[id=\"__next-build-watcher\"],nextjs-portal')){" +
            "if(text.indexOf('application error')!==-1)isErrorShell=true;" +
        "}else if(navigator.onLine===false&&" +
                 "(text.indexOf('this page could not be found')!==-1||" +
                  "text.indexOf('internal server error')!==-1||" +
                  "(text.length<120&&text.indexOf('404')!==-1))){" +
            "isErrorShell=true;" +
        "}else if(navigator.onLine===false&&text.trim().length<30&&!document.querySelector('img,video,canvas,iframe')){" +
            "isErrorShell=true;" +
        "}" +
        "if(isErrorShell){" +
            "goOffline();" +
            "return;" +
        "}" +
        "if(!window.__wta_err_bound){" +
            "window.__wta_err_bound=true;" +
            "window.addEventListener('error',function(){if(!navigator.onLine)goOffline();});" +
            "window.addEventListener('unhandledrejection',function(){if(!navigator.onLine)goOffline();});" +
        "}" +
    "}catch(e){}})();"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val navy = AndroidColor.parseColor("#0F172A")
        @Suppress("DEPRECATION")
        window.statusBarColor = navy
        @Suppress("DEPRECATION")
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
    object Home : BottomNavItem("Home", Icons.Filled.Home, "https://www.wisdom-tower-academy.live/")
    object Learning : BottomNavItem("Learning", Icons.AutoMirrored.Filled.MenuBook, "https://www.wisdom-tower-academy.live/learning")
    object Packages : BottomNavItem("Packages", Icons.AutoMirrored.Filled.ViewList, "https://www.wisdom-tower-academy.live/packages")
    object Account : BottomNavItem("Account", Icons.Filled.Person, "https://www.wisdom-tower-academy.live/account")
}

private data class MenuLink(
    val label: String,
    val url: String,
    val icon: ImageVector,
    val external: Boolean = false,
)

private val overflowMenuLinks = listOf(
    MenuLink("Settings", "https://www.wisdom-tower-academy.live/settings", Icons.Filled.Settings),
    MenuLink("My account", "https://www.wisdom-tower-academy.live/account", Icons.Filled.Person),
    MenuLink("Sign out", "https://www.wisdom-tower-academy.live/logout", Icons.AutoMirrored.Filled.Logout),
    MenuLink("About", "https://www.wisdom-tower-academy.live/about", Icons.Filled.Info),
    MenuLink("Contact us", "https://www.wisdom-tower-academy.live/contact", Icons.Outlined.Email),
    MenuLink("FAQ", "https://www.wisdom-tower-academy.live/academy/faq", Icons.AutoMirrored.Outlined.HelpOutline),
    MenuLink("Wisdom Digital", "https://wisdomtower.tech", Icons.AutoMirrored.Filled.OpenInNew, external = true),
    MenuLink("Telegram group", "https://t.me/wisdom_tower1", Icons.AutoMirrored.Filled.Send, external = true),
    MenuLink("Telegram channel", "https://t.me/wisdom_tower2", Icons.Filled.Campaign, external = true),
    MenuLink("LinkedIn", "https://www.linkedin.com/company/wisdom-tower/", Icons.Filled.Business, external = true),
    MenuLink("Privacy", "https://www.wisdom-tower-academy.live/privacy", Icons.Outlined.PrivacyTip),
    MenuLink("Terms", "https://www.wisdom-tower-academy.live/terms", Icons.Outlined.Policy),
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

    // Loading states for zero blank screen and lively navigation feedback
    var isInitialLoading by remember { mutableStateOf(true) }
    var minSplashElapsed by remember { mutableStateOf(false) }
    var pageRendered by remember { mutableStateOf(false) }
    var webProgress by remember { mutableIntStateOf(0) }
    var isNavigating by remember { mutableStateOf(false) }

    var lastResumeRefreshAt by remember { mutableLongStateOf(0L) }
    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding(context)) }
    var showExitDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var pendingClearHistory by remember { mutableStateOf(false) }

    // Hand-off from system splash to brand animated loader with smooth minimum cinematic timing
    LaunchedEffect(Unit) {
        onReady()
        delay(MIN_SPLASH_DISPLAY_MS)
        minSplashElapsed = true
        if (pageRendered) {
            isInitialLoading = false
        }
        // Safety timeout: ensure loader never hangs if network is slow
        delay(4000L)
        isInitialLoading = false
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
        if (showOnboarding) return@BackHandler
        if (showExitDialog) {
            showExitDialog = false
            return@BackHandler
        }
        val triggerDoubleTapExit: () -> Unit = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                activity?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }

        val wv = webView
        val currentUrl = wv?.url ?: ""
        if (wv == null || currentUrl.isBlank() || currentUrl.startsWith("file://")) {
            triggerDoubleTapExit()
        } else {
            wv.evaluateJavascript(StructuralNav.STRUCTURAL_BACK_JS) { rawResult ->
                val res = rawResult?.trim('"')?.trim() ?: ""
                if (res != "ok") {
                    triggerDoubleTapExit()
                }
            }
        }
    }

    fun showOffline(wv: WebView) {
        isInitialLoading = false
        isNavigating = false
        wv.loadUrl(OFFLINE_ASSET)
    }

    fun navigateTo(url: String, tabIndex: Int? = null, resetHistory: Boolean = false) {
        val wv = webView ?: return
        if (tabIndex != null) selectedIndex = tabIndex
        if (resetHistory) pendingClearHistory = true
        isNavigating = true
        webProgress = 20

        val online = isOnline(context)
        wv.settings.cacheMode = if (online) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }

        if (!online) {
            wv.loadUrl(url)
            mainHandler.postDelayed({
                val current = wv.url ?: ""
                if (!isOnline(context) && !current.startsWith("file://") &&
                    (wv.progress < 100 || current.isEmpty() || current == "about:blank")) {
                    showOffline(wv)
                }
            }, 600L)
            return
        }

        val currentUrl = wv.url ?: ""
        if (currentUrl.contains("wisdom-tower-academy.live") && url.contains("wisdom-tower-academy.live")) {
            val js = "(function(){" +
                "try{" +
                    "var a = document.createElement('a');" +
                    "a.href = '$url';" +
                    "document.body.appendChild(a);" +
                    "a.click();" +
                    "a.remove();" +
                "}catch(e){" +
                    "window.location.href = '$url';" +
                "}" +
            "})();"
            wv.evaluateJavascript(js, null)
        } else {
            wv.loadUrl(url)
        }
    }

    fun openOrDownloadPdf(wv: WebView, ctx: Context, url: String) {
        val cleanUrl = url.trim()
        val local = OfflineVault.localFileFor(ctx, cleanUrl)
        if (local != null && local.exists() && local.length() > 0) {
            wv.loadUrl(OfflineVault.fileUrl(local))
            return
        }
        if (!isOnline(ctx)) {
            mainHandler.post {
                Toast.makeText(ctx, "Book not available offline yet. Open it once while online.", Toast.LENGTH_LONG).show()
            }
            return
        }
        mainHandler.post {
            Toast.makeText(ctx, "Saving for offline use", Toast.LENGTH_SHORT).show()
        }
        OfflineVault.downloadAsync(ctx, cleanUrl) { file ->
            mainHandler.post {
                if (file != null && file.exists() && file.length() > 0) {
                    Toast.makeText(ctx, "Saved offline", Toast.LENGTH_SHORT).show()
                    wv.loadUrl(OfflineVault.fileUrl(file))
                } else {
                    wv.loadUrl(cleanUrl)
                }
            }
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            onFinished = {
                setOnboardingCompleted(context)
                showOnboarding = false
            }
        )
        return
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
                    // Top App Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Hamburger menu
                        Box {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    menuExpanded = true
                                },
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu",
                                    tint = Accent,
                                    modifier = Modifier.size(24.dp)
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
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
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
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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

                        // Center: Brand Logo and Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                            ) {
                                BrandLogo(size = 34.dp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Wisdom Tower Academy",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right: Pinned Refresh and Notification Actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    val wv = webView
                                    val currentUrl = wv?.url ?: ""
                                    if (wv != null) {
                                        isNavigating = true
                                        webProgress = 15
                                        if (currentUrl.isBlank() || currentUrl.startsWith("file://")) {
                                            if (isOnline(context)) {
                                                navigateTo("https://www.wisdom-tower-academy.live/", null)
                                            } else {
                                                wv.reload()
                                            }
                                        } else {
                                            wv.evaluateJavascript(StructuralNav.HARD_REFRESH_JS) {
                                                Handler(Looper.getMainLooper()).post {
                                                    wv.reload()
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    navigateTo("https://www.wisdom-tower-academy.live/notifications", null)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Sleek animated linear progress bar right beneath top bar
                    AnimatedVisibility(
                        visible = isNavigating || (webProgress in 1..95 && !isInitialLoading),
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(250))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(Color(0x1A00E5FF))
                        ) {
                            val progressFraction = (webProgress.coerceIn(0, 100)) / 100f
                            val animatedProgress by animateFloatAsState(
                                targetValue = if (isNavigating && progressFraction < 0.2f) 0.35f else progressFraction,
                                animationSpec = tween(220),
                                label = "navProgressBar"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF00E5FF),
                                                Color(0xFF38BDF8),
                                                Color(0xFF00E5FF)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // Hairline subtle divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x14FFFFFF))
                    )
                }
            },
            bottomBar = {
                AliveBottomNav(
                    items = items,
                    selectedIndex = selectedIndex,
                    onItemSelected = { index, item ->
                        selectedIndex = index
                        navigateTo(item.url, index)
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Main Web View
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(AndroidColor.parseColor("#0F172A"))
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                @Suppress("DEPRECATION")
                                databaseEnabled = true
                                loadsImagesAutomatically = true
                                blockNetworkImage = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                allowContentAccess = true

                                // Ultra-smooth rendering and fast hydration
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                offscreenPreRaster = true
                                cacheMode = if (isOnline(ctx)) {
                                    WebSettings.LOAD_DEFAULT
                                } else {
                                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                                }
                            }
                            val cookieMgr = CookieManager.getInstance()
                            cookieMgr.setAcceptCookie(true)
                            cookieMgr.setAcceptThirdPartyCookies(this, true)

                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun isPdfCached(url: String?): Boolean {
                                    if (url.isNullOrBlank()) return false
                                    return OfflineVault.has(ctx, url)
                                }

                                @JavascriptInterface
                                fun getPdfSize(url: String?): Long {
                                    if (url.isNullOrBlank()) return 0L
                                    val f = OfflineVault.localFileFor(ctx, url) ?: return 0L
                                    return f.length()
                                }

                                @JavascriptInterface
                                fun getOfflinePdfUrl(url: String?): String {
                                    if (url.isNullOrBlank()) return ""
                                    val f = OfflineVault.localFileFor(ctx, url) ?: return ""
                                    return OfflineVault.fileUrl(f)
                                }
                            }, "AndroidOfflineVault")

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    webProgress = newProgress
                                    if (newProgress >= 100) {
                                        isNavigating = false
                                    }
                                    if (newProgress >= 70) {
                                        pageRendered = true
                                        if (minSplashElapsed) {
                                            isInitialLoading = false
                                        }
                                    }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    if (url != null && url.startsWith("file:///android_asset/")) {
                                        isInitialLoading = false
                                        isNavigating = false
                                    }
                                    view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                    view?.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    pageRendered = true
                                    if (minSplashElapsed) {
                                        isInitialLoading = false
                                    }
                                    isNavigating = false
                                    view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                    view?.evaluateJavascript(PRECACHE_AND_UNBLOCK_JS, null)
                                    view?.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                    view?.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    pageRendered = true
                                    if (minSplashElapsed) {
                                        isInitialLoading = false
                                    }
                                    isNavigating = false
                                    view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                    view?.evaluateJavascript(PRECACHE_AND_UNBLOCK_JS, null)
                                    view?.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                    view?.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                    mainHandler.postDelayed({
                                        val cur = view?.url ?: ""
                                        if (!cur.startsWith("file://")) {
                                            view?.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                            view?.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                        }
                                    }, 800L)
                                    if (pendingClearHistory) {
                                        pendingClearHistory = false
                                        view?.clearHistory()
                                    }
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    if (url != null && !url.startsWith("file://")) {
                                        val path = url.substringBefore("?").removeSuffix("/")
                                        selectedIndex = when {
                                            path.endsWith("/learning") || path.contains("/learning/") || path.contains("/my-learning") -> 1
                                            path.endsWith("/packages") || path.contains("/packages/") -> 2
                                            path.endsWith("/account") || path.endsWith("/settings") ||
                                                path.endsWith("/login") || path.endsWith("/auth") ||
                                                path.endsWith("/logout") -> 3
                                            path.contains("/notifications") -> selectedIndex
                                            path == "https://www.wisdom-tower-academy.live" ||
                                                path == "https://www.wisdom-tower-academy.live/" ||
                                                path == "https://wisdom-tower-academy.live" ||
                                                path == "https://wisdom-tower-academy.live/" -> 0
                                            else -> selectedIndex
                                        }
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    if (request?.isForMainFrame != true) return
                                    val wv = view ?: return
                                    val statusCode = errorResponse?.statusCode ?: 0
                                    val failedUrl = request.url?.toString() ?: ""
                                    if (failedUrl.startsWith("file://")) return
                                    if (statusCode >= 400 && !isOnline(ctx)) {
                                        showOffline(wv)
                                    } else if (statusCode >= 500) {
                                        showOffline(wv)
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame != true) return
                                    val wv = view ?: return
                                    val failedUrl = request.url?.toString()
                                    if (failedUrl != null && !failedUrl.startsWith("file://") &&
                                        wv.settings.cacheMode != WebSettings.LOAD_CACHE_ELSE_NETWORK) {
                                        wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                        wv.loadUrl(failedUrl)
                                        return
                                    }
                                    if (failedUrl == null || !failedUrl.contains("offline.html")) {
                                        showOffline(wv)
                                    }
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    val wv = view ?: return
                                    if (failingUrl != null && failingUrl == wv.url &&
                                        !failingUrl.contains("offline.html")) {
                                        if (wv.settings.cacheMode != WebSettings.LOAD_CACHE_ELSE_NETWORK) {
                                            wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                            wv.loadUrl(failingUrl)
                                            return
                                        }
                                        showOffline(wv)
                                    }
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val req = request ?: return null
                                    val u = req.url?.toString() ?: return null
                                    val isGet = req.method.equals("GET", ignoreCase = true)
                                    val isHead = req.method.equals("HEAD", ignoreCase = true)

                                    if ((isGet || isHead) && (u.contains("/api/content/pdf") || OfflineVault.isPdfUrl(u))) {
                                        val local = OfflineVault.localFileFor(ctx, u)
                                        if (local != null && local.exists() && local.length() > 0) {
                                            return try {
                                                val headers = HashMap<String, String>().apply {
                                                    put("Access-Control-Allow-Origin", "*")
                                                    put("Content-Type", "application/pdf")
                                                    put("Content-Length", local.length().toString())
                                                    put("Accept-Ranges", "bytes")
                                                    put("Cache-Control", "public, max-age=31536000, immutable")
                                                }
                                                val stream = if (isHead) ByteArrayInputStream(ByteArray(0)) else FileInputStream(local)
                                                WebResourceResponse("application/pdf", "binary", 200, "OK", headers, stream)
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                        if (isOnline(ctx)) {
                                            return try {
                                                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                                                    requestMethod = if (isHead) "HEAD" else "GET"
                                                    connectTimeout = 20_000
                                                    readTimeout = 45_000
                                                    instanceFollowRedirects = true
                                                    setRequestProperty("User-Agent", "WisdomTowerApp/1.0")
                                                    val cookies = CookieManager.getInstance().getCookie(u)
                                                    if (!cookies.isNullOrBlank()) {
                                                        setRequestProperty("Cookie", cookies)
                                                    }
                                                    req.requestHeaders?.forEach { (k, v) ->
                                                        if (!k.equals("Cookie", ignoreCase = true) &&
                                                            !k.equals("User-Agent", ignoreCase = true)) {
                                                            setRequestProperty(k, v)
                                                        }
                                                    }
                                                }
                                                conn.connect()
                                                val code = conn.responseCode
                                                if (code in 200..299) {
                                                    val mime = conn.contentType ?: "application/pdf"
                                                    val contentLength = conn.contentLengthLong
                                                    val headers = HashMap<String, String>().apply {
                                                        put("Access-Control-Allow-Origin", "*")
                                                        put("Content-Type", mime)
                                                        if (contentLength > 0) {
                                                            put("Content-Length", contentLength.toString())
                                                        }
                                                        put("Cache-Control", "public, max-age=31536000, immutable")
                                                    }
                                                    if (isHead) {
                                                        conn.disconnect()
                                                        WebResourceResponse(mime, "binary", 200, "OK", headers, ByteArrayInputStream(ByteArray(0)))
                                                    } else {
                                                        val bytes = conn.inputStream.use { it.readBytes() }
                                                        conn.disconnect()
                                                        OfflineVault.saveBytesAsync(ctx, u, bytes)
                                                        WebResourceResponse(mime, "binary", 200, "OK", headers, ByteArrayInputStream(bytes))
                                                    }
                                                } else {
                                                    conn.disconnect()
                                                    null
                                                }
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val u = request?.url?.toString() ?: return false
                                    if (u.contains("/my-learning")) {
                                        navigateTo("https://www.wisdom-tower-academy.live/learning", 1)
                                        return true
                                    }
                                    if (OfflineVault.isPdfUrl(u) || u.endsWith(".pdf", ignoreCase = true) || u.contains("/pdf")) {
                                        view?.let { openOrDownloadPdf(it, ctx, u) }
                                        return true
                                    }
                                    val host = request.url?.host?.lowercase() ?: ""
                                    val isInternal = host.contains("wisdom-tower-academy.live") ||
                                        host.contains("wisdomtower.tech")
                                    if (!isInternal && !u.startsWith("file://") &&
                                        (u.startsWith("http://") || u.startsWith("https://") ||
                                         u.startsWith("tg:") || u.startsWith("tel:") || u.startsWith("mailto:"))
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

                            loadUrl(SITE)
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Perfect Timing Splash / Loading Overlay: Eliminates blank screens
                AnimatedVisibility(
                    visible = isInitialLoading,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(380, easing = FastOutSlowInEasing))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient radial glowing aura behind card
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0x2E00E5FF),
                                            Color(0x0A00E5FF),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            // High-quality animated brand logo GIF
                            BrandLoader(size = 140.dp, showCard = true)

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Wisdom Tower Academy",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Preparing your learning space…",
                                color = Muted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Sleek rounded micro progress track
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x2600E5FF))
                            ) {
                                val progressFraction = (webProgress.coerceIn(0, 100)) / 100f
                                val animProgress by animateFloatAsState(
                                    targetValue = if (progressFraction < 0.2f) 0.35f else progressFraction,
                                    animationSpec = tween(250),
                                    label = "splashTrack"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animProgress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Accent)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showExitDialog) {
            ExitGuiltDialog(
                onStay = { showExitDialog = false },
                onExit = {
                    showExitDialog = false
                    activity?.finish()
                }
            )
        }
    }
}

/**
 * High-tactile alive bottom navigation bar with fluid spring physics,
 * glowing active capsule, bouncing icons, and haptic feedback.
 */
@Composable
private fun AliveBottomNav(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int, BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Surface(
        color = BarBg,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(60.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Hairline top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1A334155))
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEachIndexed { index, item ->
                    val selected = selectedIndex == index

                    // Bouncy spring scale on tap
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.16f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.45f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "iconScale"
                    )

                    // Vertical lift on active
                    val yOffset by animateDpAsState(
                        targetValue = if (selected) (-2.5).dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = 0.55f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "yOffset"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (selected) Accent else Muted,
                        animationSpec = tween(180),
                        label = "iconColor"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (selected) Accent else Muted,
                        animationSpec = tween(180),
                        label = "textColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (selectedIndex != index) {
                                    onItemSelected(index, item)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Illuminated active capsule glow
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .size(width = 66.dp, height = 46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x2600E5FF),
                                                Color(0x0A00E5FF)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Color(0x3300E5FF),
                                        RoundedCornerShape(12.dp)
                                    )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.offset(y = yOffset)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                color = textColor,
                                fontSize = 10.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Micro glowing active dot
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(tween(120)) + scaleIn(spring(dampingRatio = 0.5f)),
                                exit = fadeOut(tween(80)) + scaleOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(3.5.dp)
                                        .clip(CircleShape)
                                        .background(Accent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
