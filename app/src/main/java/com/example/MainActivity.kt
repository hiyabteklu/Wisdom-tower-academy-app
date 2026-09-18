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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

private const val OFFLINE_ASSET = "file:///android_asset/offline.html"
private const val SITE = "https://wisdom-tower-academy.live/"
private const val MIN_SPLASH_MS = 4500L

private const val NATIVE_CHROME_JS =
    "(function(){try{" +
        "document.documentElement.classList.add('wta-native-app');" +
        "if(document.body){document.body.classList.add('wta-native-app');document.body.style.pointerEvents='auto';" +
        "document.body.style.backgroundColor='#0F172A';document.documentElement.style.backgroundColor='#0F172A';}" +
        "var id='wta-app-chrome';var s=document.getElementById(id);" +
        "if(!s){s=document.createElement('style');s.id=id;document.documentElement.appendChild(s);}" +
        "s.textContent=" +
        "'html,body{background:#0F172A!important;}" +
        "header,[data-site-header],footer,[data-site-footer],.site-header,.site-footer{display:none!important;}" +
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
        "function softenErrors(root){try{var nodes=root.querySelectorAll('p,div,span,li,h1,h2,h3');" +
        "for(var i=0;i<nodes.length;i++){var t=nodes[i].childNodes;" +
        "for(var j=0;j<t.length;j++){if(t[j].nodeType===3){var v=t[j].nodeValue;if(!v)continue;" +
        "if(/TypeError|Failed to fetch|NetworkError|ERR_|net::/i.test(v)){" +
        "t[j].nodeValue=v.replace(/Could not load[^.]*\\.\\s*/i,'')" +
        ".replace(/TypeError:\\s*Failed to fetch/gi,'Something went wrong. Check your connection and try again.')" +
        ".replace(/Failed to fetch/gi,'Something went wrong. Check your connection and try again.')" +
        ".replace(/TypeError:[^.]*/gi,'Something went wrong.');" +
        "}" +
        "if(/offline|no internet|not available offline|ERR_INTERNET|Webpage not available/i.test(v)){" +
        "t[j].nodeValue='Connection required. Please turn on your mobile data or connect to WiFi to continue.';" +
        "}" +
        "}}}}catch(e){}}" +
        "softenErrors(document);" +
        "if(!window.__wtaErrObs){window.__wtaErrObs=new MutationObserver(function(muts){" +
        "muts.forEach(function(m){m.addedNodes&&m.addedNodes.forEach(function(n){if(n.nodeType===1)softenErrors(n);});});" +
        "});window.__wtaErrObs.observe(document.documentElement,{childList:true,subtree:true});}" +
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
