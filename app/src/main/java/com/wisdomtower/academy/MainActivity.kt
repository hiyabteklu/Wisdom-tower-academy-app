package com.wisdomtower.academy

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.ServiceWorkerController
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.PaddingValues
import android.webkit.SslErrorHandler
import android.net.http.SslError
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wisdomtower.academy.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

private val BarBg = Color(0xFF0F172A)
private val Accent = Color(0xFF00E5FF)
private val SurfaceColor = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)

private const val OFFLINE_ASSET = "file:///android_asset/offline.html"
// Direct 200 URL (eliminates 308 redirect round-trip delay)
private const val SITE = "https://www.wisdom-tower-academy.live/"
private const val MIN_SPLASH_DISPLAY_MS = 2200L

private const val NATIVE_CHROME_JS =
    "(function(){try{" +
        "document.documentElement.classList.add('wta-native-app');" +
        "if(document.body){document.body.classList.add('wta-native-app');}" +
        "var id='wta-app-chrome';var s=document.getElementById(id);" +
        "if(!s){s=document.createElement('style');s.id=id;document.head?document.head.appendChild(s):document.documentElement.appendChild(s);}" +
        "s.textContent=" +
        "'header.fixed.top-0,header[data-site-header],footer,[data-site-footer],.site-header,.site-footer," +
        "nav[aria-label=\"Main\"],.hide-on-app,#nprogress,.nprogress,#nprogress .bar," +
        "[data-nprogress],#nextjs-toploader,.nextjs-toploader," +
        "nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast]" +
        "{display:none!important;visibility:hidden!important;height:0!important;overflow:hidden!important;opacity:0!important;}';" +
        "var noCopyId='wta-disable-copy';var cs=document.getElementById(noCopyId);" +
        "if(!cs){cs=document.createElement('style');cs.id=noCopyId;document.head?document.head.appendChild(cs):document.documentElement.appendChild(cs);}" +
        "var p=(window.location.pathname||'').toLowerCase();" +
        "var isAuthOrLearning=p.indexOf('/login')!==-1||p.indexOf('/auth')!==-1||p.indexOf('/signin')!==-1||p.indexOf('/sign-in')!==-1||p.indexOf('/signup')!==-1||p.indexOf('/register')!==-1||p.indexOf('password')!==-1||p.indexOf('/account')!==-1||p.indexOf('/learning')!==-1||p.indexOf('/notes')!==-1||p.indexOf('/study')!==-1;" +
        "if(isAuthOrLearning){" +
            "cs.textContent='* { -webkit-user-select:text!important; user-select:text!important; -webkit-touch-callout:default!important; } input,textarea,[contenteditable=\"true\"],.note,.notes { -webkit-user-select:auto!important; user-select:auto!important; }';" +
        "}else{" +
            "cs.textContent=" +
            "'html,body,table,td,th,article,section,main{" +
            "-webkit-user-select:none;user-select:none;}" +
            "a,button,[role=\"button\"],.cursor-pointer,input,textarea,select,p,span,h1,h2,h3,h4,h5,h6{" +
            "-webkit-user-select:auto!important;user-select:auto!important;-webkit-touch-callout:default!important;" +
            "pointer-events:auto!important;touch-action:manipulation!important;-webkit-tap-highlight-color:rgba(0,229,255,0.2)!important;}" +
            "input,textarea,[contenteditable=\"true\"],.note,.notes,[data-notes]{-webkit-user-select:text!important;user-select:text!important;}';" +
        "}" +
        "if(!window.__wta_copy_handler){" +
            "window.__wta_copy_handler=true;" +
            "function isAllowedCopy(el){" +
                "var curP=(window.location.pathname||'').toLowerCase();" +
                "if(curP.indexOf('/login')!==-1||curP.indexOf('/auth')!==-1||curP.indexOf('/signin')!==-1||curP.indexOf('/signup')!==-1||curP.indexOf('/register')!==-1||curP.indexOf('password')!==-1||curP.indexOf('/account')!==-1||curP.indexOf('/learning')!==-1||curP.indexOf('/notes')!==-1||curP.indexOf('/study')!==-1) return true;" +
                "if(!el) return false;" +
                "if(el.tagName==='INPUT'||el.tagName==='TEXTAREA'||el.isContentEditable) return true;" +
                "if(el.closest && el.closest('input,textarea,[contenteditable=\"true\"],.note,.notes,[data-notes],.ql-editor,.DraftEditor-root,form')) return true;" +
                "return false;" +
            "}" +
            "document.addEventListener('copy',function(e){if(isAllowedCopy(e.target))return;e.preventDefault();if(e.clipboardData)e.clipboardData.setData('text/plain','');return false;},true);" +
            "document.addEventListener('cut',function(e){if(isAllowedCopy(e.target))return;e.preventDefault();return false;},true);" +
            "window.addEventListener('error',function(e){if(e&&e.message&&(e.message.indexOf('fetch')!==-1||e.message.indexOf('Network')!==-1)){e.preventDefault();}});" +
            "window.addEventListener('unhandledrejection',function(e){if(e&&e.reason&&(String(e.reason).indexOf('fetch')!==-1||String(e.reason).indexOf('Network')!==-1)){e.preventDefault();}});" +
        "}" +
        "if(!window.__wta_route_monitor){" +
            "window.__wta_route_monitor=true;" +
            "window.addEventListener('popstate',function(){" +
                "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.notifyLoadingFinished==='function'){" +
                    "window.AndroidOfflineVault.notifyLoadingFinished();" +
                "}" +
            "});" +
        "}" +
        "}catch(e){}})();"

private const val PRECACHE_AND_UNBLOCK_JS =
    "(function(){try{" +
        "function unblockImgs(){" +
            "var imgs=document.querySelectorAll('img');" +
            "for(var i=0;i<imgs.length;i++){" +
                "var img=imgs[i];" +
                "if(img.getAttribute('loading')==='lazy'){img.removeAttribute('loading');}" +
                "if(img.getAttribute('fetchpriority')==='low'){img.removeAttribute('fetchpriority');}" +
                "img.setAttribute('decoding','async');" +
            "}" +
        "}" +
        "unblockImgs();" +
        "if(!window.__wta_img_observer&&window.MutationObserver){" +
            "window.__wta_img_observer=new MutationObserver(function(){unblockImgs();});" +
            "window.__wta_img_observer.observe(document.body||document.documentElement,{childList:true,subtree:true});" +
        "}" +
        "if('serviceWorker' in navigator&&navigator.serviceWorker.controller){" +
            "var links=document.querySelectorAll('a[href^=\"/\"],a[href*=\"wisdom-tower-academy.live\"]');" +
            "var urls=[];" +
            "for(var j=0;j<Math.min(links.length,15);j++){" +
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
        "if(!window.__wta_route_hook){" +
            "window.__wta_route_hook=true;" +
            "function syncRoute(){" +
                "try{" +
                    "var p=window.location.pathname||'';" +
                    "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.onRouteChanged==='function'){" +
                        "window.AndroidOfflineVault.onRouteChanged(p);" +
                    "}" +
                "}catch(e){}" +
            "}" +
            "var _ps=history.pushState;" +
            "history.pushState=function(){" +
                "var ret=_ps.apply(this,arguments);" +
                "syncRoute();" +
                "return ret;" +
            "};" +
            "var _rs=history.replaceState;" +
            "history.replaceState=function(){" +
                "var ret=_rs.apply(this,arguments);" +
                "syncRoute();" +
                "return ret;" +
            "};" +
            "window.addEventListener('popstate',syncRoute);" +
            "syncRoute();" +
        "}" +
        "if(!window.__wta_download_click_hook){" +
            "window.__wta_download_click_hook=true;" +
            "var _wta_download_in_flight=false;" +
            "document.addEventListener('click',function(e){" +
                "try{" +
                    "var t=e.target;" +
                    "var btn=t.closest?t.closest('button'):null;" +
                    "if(!btn)return;" +
                    "var bt=(btn.textContent||'').trim();" +
                    "if(bt.indexOf('Download')===-1)return;" +
                    "var u=window.__wta_current_pdf_url||'';" +
                    "if(!u){" +
                        "var links=document.querySelectorAll('a[href*=\".pdf\"],a[href*=\"/api/content/pdf\"],button[data-url],a[href*=\"/pdf\"]');" +
                        "for(var i=0;i<links.length;i++){" +
                            "var h=links[i].getAttribute('href')||links[i].getAttribute('data-url')||'';" +
                            "if(h){u=h;break;}" +
                        "}" +
                    "}" +
                    "var isCached=false;" +
                    "if(u&&window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.isPdfCached==='function'){" +
                        "isCached=window.AndroidOfflineVault.isPdfCached(u);" +
                    "}" +
                    "if(isCached){" +
                        "e.preventDefault();" +
                        "e.stopPropagation();" +
                        "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.openCachedPdf==='function'){" +
                            "window.AndroidOfflineVault.openCachedPdf(u);" +
                        "}else if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.markDownloadStarted==='function'){" +
                            "window.AndroidOfflineVault.markDownloadStarted(u);" +
                        "}" +
                        "return;" +
                    "}" +
                    "if(_wta_download_in_flight)return;" +
                    "_wta_download_in_flight=true;" +
                    "setTimeout(function(){_wta_download_in_flight=false;},8000);" +
                    "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.markDownloadStarted==='function'){" +
                        "window.AndroidOfflineVault.markDownloadStarted(u);" +
                    "}" +
                "}catch(err){}" +
            "},true);" +
        "}" +
    "}catch(e){}})();"

private const val DETECT_AND_RECOVER_JS =
    "(function(){try{" +
        "if(window.location.protocol==='file:')return;" +
        "if(!window.__wta_err_bound){" +
            "window.__wta_err_bound=true;" +
            "window.addEventListener('error',function(e){" +
                "var msg=(e&&e.message)?e.message:'';" +
                "if(msg.indexOf('client-side exception')!==-1||(document.body&&document.body.textContent&&document.body.textContent.indexOf('client-side exception')!==-1)){" +
                    "var k='__wta_recov_'+window.location.pathname;" +
                    "if(!sessionStorage.getItem(k)){" +
                        "sessionStorage.setItem(k,'1');" +
                        "window.location.reload();" +
                    "}" +
                "}" +
            "});" +
        "}" +
    "}catch(e){}})();"

private const val STUDY_TIMER_BRIDGE_JS =
    "(function(){try{" +
        "if(window.__wta_timer_bridge_hooked)return;" +
        "window.__wta_timer_bridge_hooked=true;" +
        "function checkFocusTimer(){" +
            "try{" +
                "var raw=localStorage.getItem('wt_focus_timer_v1');" +
                "if(!raw){" +
                    "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.syncStudyTimer==='function'){" +
                        "window.AndroidOfflineVault.syncStudyTimer(false,0,0,'');" +
                    "}" +
                    "return;" +
                "}" +
                "var s=JSON.parse(raw);" +
                "var now=Date.now();" +
                "var isRunning=Boolean(s.running&&s.endAt&&s.endAt>now);" +
                "var rem=isRunning?Math.max(0,Math.ceil((s.endAt-now)/1000)):0;" +
                "var tot=s.totalSec||1500;" +
                "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.syncStudyTimer==='function'){" +
                    "window.AndroidOfflineVault.syncStudyTimer(isRunning,rem,tot,'Study Timer');" +
                "}" +
            "}catch(_){}" +
        "}" +
        "window.addEventListener('wt-focus-timer',checkFocusTimer);" +
        "window.addEventListener('storage',function(e){if(e.key==='wt_focus_timer_v1')checkFocusTimer();});" +
        "window.addEventListener('wta-study-timer-control',function(e){" +
            "try{" +
                "var act=(e.detail&&e.detail.action)?e.detail.action:'';" +
                "var raw=localStorage.getItem('wt_focus_timer_v1');" +
                "var s=raw?JSON.parse(raw):null;" +
                "if(!s)return;" +
                "var now=Date.now();" +
                "if(act==='pause'){" +
                    "var left=(s.running&&s.endAt)?Math.max(0,Math.ceil((s.endAt-now)/1000)):(s.leftWhenPaused||0);" +
                    "s.running=false;s.endAt=null;s.leftWhenPaused=left;" +
                    "localStorage.setItem('wt_focus_timer_v1',JSON.stringify(s));" +
                    "window.dispatchEvent(new CustomEvent('wt-focus-timer'));" +
                "}else if(act==='resume'){" +
                    "var left=(s.leftWhenPaused&&s.leftWhenPaused>0)?s.leftWhenPaused:(s.totalSec||1500);" +
                    "s.running=true;s.endAt=now+(left*1000);" +
                    "localStorage.setItem('wt_focus_timer_v1',JSON.stringify(s));" +
                    "window.dispatchEvent(new CustomEvent('wt-focus-timer'));" +
                "}else if(act==='stop'||act==='close'){" +
                    "s.running=false;s.endAt=null;s.leftWhenPaused=s.totalSec||1500;" +
                    "localStorage.setItem('wt_focus_timer_v1',JSON.stringify(s));" +
                    "window.dispatchEvent(new CustomEvent('wt-focus-timer'));" +
                "}" +
            "}catch(_){}" +
        "});" +
        "setInterval(checkFocusTimer,1000);" +
        "checkFocusTimer();" +
    "}catch(e){}})();"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        // TODO: Re-enable FLAG_SECURE before final production release to prevent unauthorized screen captures
        // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val navy = AndroidColor.parseColor("#0F172A")
        @Suppress("DEPRECATION")
        window.statusBarColor = navy
        @Suppress("DEPRECATION")
        window.navigationBarColor = navy
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false
        setContent {
            MyApplicationTheme {
                MainScreen(onReady = { keepSplash = false })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO: Re-enable FLAG_SECURE before final production release to prevent unauthorized screen captures
        // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

sealed class BottomNavItem(val title: String, val icon: ImageVector, val url: String) {
    object Home : BottomNavItem("Home", Icons.Filled.Home, "https://www.wisdom-tower-academy.live/")
    object Learning : BottomNavItem("Learning", Icons.AutoMirrored.Filled.MenuBook, "https://www.wisdom-tower-academy.live/learning")
    object Packages : BottomNavItem("Packages", Icons.AutoMirrored.Filled.ViewList, "https://www.wisdom-tower-academy.live/packages")
    object Account : BottomNavItem("Account", Icons.Filled.Person, "https://www.wisdom-tower-academy.live/account")
    object Settings : BottomNavItem("Settings", Icons.Filled.Settings, "https://www.wisdom-tower-academy.live/settings")
}

private data class MenuLink(
    val label: String,
    val url: String,
    val icon: ImageVector,
)

private val overflowMenuLinks = listOf(
    MenuLink("About", "https://www.wisdom-tower-academy.live/about", Icons.Filled.Info),
    MenuLink("Contact us", "https://www.wisdom-tower-academy.live/contact", Icons.Outlined.Email),
    MenuLink("FAQ", "https://www.wisdom-tower-academy.live/academy/faq", Icons.AutoMirrored.Outlined.HelpOutline),
    MenuLink("Privacy", "https://www.wisdom-tower-academy.live/privacy", Icons.Outlined.PrivacyTip),
    MenuLink("Terms", "https://www.wisdom-tower-academy.live/terms", Icons.Outlined.Policy),
)

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Maps a site URL / path to the corresponding bottom nav tab index with 100% precision.
 * Tab 0: Home
 * Tab 1: Learning (/learning, /academy, courses, subjects, books)
 * Tab 2: Packages (/packages, /cart, /checkout, /orders)
 * Tab 3: Account (/account, /login, /signup, /auth, etc.)
 * Tab 4: Settings (/settings)
 * /notifications: preserves currentTab (Architecture rule #6)
 */
private fun tabIndexForUrl(url: String?, currentTab: Int): Int {
    if (url.isNullOrBlank() || url.startsWith("file://")) return currentTab
    val clean = url.trim()
    val path = try {
        val uri = Uri.parse(clean)
        uri.path?.removeSuffix("/")?.lowercase() ?: ""
    } catch (_: Exception) {
        clean.substringBefore("?").substringBefore("#").removeSuffix("/").lowercase()
    }

    if (path.contains("notifications") || clean.contains("/notifications")) {
        return currentTab
    }

    // Tab 4: Settings
    if (path == "/settings" || path.startsWith("/settings/")) {
        return 4
    }

    // Tab 1: Learning
    if (path == "/learning" || path.startsWith("/learning/") ||
        path == "/academy" || path.startsWith("/academy/") ||
        path.contains("learning") || path.contains("academy")
    ) {
        return 1
    }

    // Tab 2: Packages
    if (path == "/packages" || path.startsWith("/packages/") ||
        path == "/cart" || path.startsWith("/cart/") ||
        path == "/orders" || path.startsWith("/orders/") ||
        path.startsWith("/checkout") || path.contains("/checkout/")
    ) {
        return 2
    }

    // Tab 3: Account
    if (path == "/account" || path.startsWith("/account/") ||
        path == "/login" || path.startsWith("/login/") ||
        path == "/signup" || path.startsWith("/signup/") ||
        path == "/register" || path.startsWith("/register/") ||
        path == "/logout" || path.startsWith("/logout/") ||
        path.startsWith("/auth") || path.contains("forgot-password") || path.contains("reset-password")
    ) {
        return 3
    }

    // Tab 0: Home
    if (path.isEmpty() || path == "/" || path == "/home" ||
        path == "/about" || path == "/contact" || path == "/faq" ||
        path == "/privacy" || path == "/terms" ||
        clean == "https://www.wisdom-tower-academy.live" ||
        clean == "https://www.wisdom-tower-academy.live/" ||
        clean == "https://wisdom-tower-academy.live" ||
        clean == "https://wisdom-tower-academy.live/"
    ) {
        return 0
    }

    return currentTab
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
        BottomNavItem.Account,
        BottomNavItem.Settings
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Instant cold-start and process-death network validation
    val isInitiallyOnline = isOnline(context)
    var isOfflineState by remember { mutableStateOf(!isInitiallyOnline) }
    var lastTargetUrl by remember { mutableStateOf(SITE) }

    // Loading states for zero blank screen and lively navigation feedback
    var isInitialLoading by remember { mutableStateOf(isInitiallyOnline) }
    var minSplashElapsed by remember { mutableStateOf(!isInitiallyOnline) }
    var pageRendered by remember { mutableStateOf(false) }
    var webProgress by remember { mutableIntStateOf(0) }
    var isNavigating by remember { mutableStateOf(false) }
    var navigationStatusText by remember { mutableStateOf("Loading…") }
    var navigationStartTime by remember { mutableLongStateOf(0L) }
    var navShowRunnable by remember { mutableStateOf<Runnable?>(null) }
    var navTimeoutRunnable by remember { mutableStateOf<Runnable?>(null) }

    fun startNavigationLoading(delayMs: Long = 0L) {
        navShowRunnable?.let { mainHandler.removeCallbacks(it) }
        navTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        navigationStartTime = System.currentTimeMillis()
        isNavigating = true
        val timeout = Runnable { isNavigating = false }
        navTimeoutRunnable = timeout
        mainHandler.postDelayed(timeout, 2500L)
    }

    fun stopNavigationLoading() {
        navShowRunnable?.let { mainHandler.removeCallbacks(it) }
        navShowRunnable = null
        navTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        navTimeoutRunnable = null
        isNavigating = false
    }

    var lastResumeRefreshAt by remember { mutableLongStateOf(0L) }
    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding(context)) }
    var showExitDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var pendingClearHistory by remember { mutableStateOf(false) }

    // Immediate cold-start / process-death check for offline
    LaunchedEffect(Unit) {
        onReady()
        if (!isOnline(context)) {
            minSplashElapsed = true
            isInitialLoading = false
            isOfflineState = true
            webView?.let { showOffline(it) }
        } else {
            delay(MIN_SPLASH_DISPLAY_MS)
            minSplashElapsed = true
            if (pageRendered) {
                isInitialLoading = false
            }
            delay(2000L)
            isInitialLoading = false
        }
    }

    // Active network monitoring for instant auto-recovery and offline enforcement
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                mainHandler.post {
                    if (isOfflineState) {
                        isOfflineState = false
                        webView?.let { wv ->
                            val target = lastTargetUrl.ifBlank { SITE }
                            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
                            wv.loadUrl(target)
                        }
                    }
                }
            }
            override fun onLost(network: android.net.Network) {
                mainHandler.post {
                    if (!isOnline(context)) {
                        webView?.let { showOffline(it) }
                    }
                }
            }
        }
        val request = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm?.registerNetworkCallback(request, callback)
        } catch (_: Exception) {}
        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val wv = webView ?: return@LifecycleEventObserver
                    wv.onResume()
                    wv.resumeTimers()
                    val online = isOnline(context)
                    if (!online) {
                        showOffline(wv)
                    } else {
                        if (isOfflineState) {
                            isOfflineState = false
                            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
                            wv.loadUrl(lastTargetUrl.ifBlank { SITE })
                        } else {
                            wv.evaluateJavascript("(function(){return window.location.pathname||'';})();") { rawPath ->
                                val p = rawPath?.trim('"')?.trim() ?: ""
                                if (p.isNotBlank() && p != "null") {
                                    selectedIndex = tabIndexForUrl(p, selectedIndex)
                                }
                            }
                            val currentUrl = wv.url
                            if (currentUrl != null && !currentUrl.startsWith("file://")) {
                                val now = System.currentTimeMillis()
                                if (now - lastResumeRefreshAt >= 5000L) {
                                    lastResumeRefreshAt = now
                                    wv.evaluateJavascript(
                                        "(function(){try{window.dispatchEvent(new CustomEvent('wta-refresh',{detail:{source:'app-resume'}}));}catch(e){}})();",
                                        null
                                    )
                                }
                            }
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    val wv = webView ?: return@LifecycleEventObserver
                    wv.onPause()
                    wv.pauseTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activity = context as? ComponentActivity
    BackHandler {
        if (menuExpanded) {
            menuExpanded = false
            return@BackHandler
        }
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

    var isTimerRunning by remember { mutableStateOf(false) }
    var timerRemainingSeconds by remember { mutableIntStateOf(0) }
    var timerTotalSeconds by remember { mutableIntStateOf(0) }
    var timerTitle by remember { mutableStateOf("Study Timer") }
    var isTimerDismissed by remember { mutableStateOf(false) }
    var lastOnlineUrl by remember { mutableStateOf(SITE) }

    LaunchedEffect(isTimerRunning, timerRemainingSeconds) {
        if (isTimerRunning && timerRemainingSeconds > 0) {
            delay(1000L)
            timerRemainingSeconds = (timerRemainingSeconds - 1).coerceAtLeast(0)
            if (timerRemainingSeconds == 0) {
                isTimerRunning = false
            }
        }
    }

    fun showOffline(wv: WebView) {
        isInitialLoading = false
        stopNavigationLoading()
        isOfflineState = true
        mainHandler.post {
            val current = wv.url.orEmpty()
            if (current.isNotBlank() && !current.startsWith("file://") && !current.contains("offline.html")) {
                lastOnlineUrl = current
                lastTargetUrl = current
            }
            try {
                wv.stopLoading()
            } catch (_: Exception) {}
            if (!current.contains("offline.html")) {
                wv.loadUrl(OFFLINE_ASSET)
            }
        }
    }

    fun navigateTo(url: String, tabIndex: Int? = null, resetHistory: Boolean = false) {
        val wv = webView ?: return
        if (tabIndex != null) selectedIndex = tabIndex
        lastTargetUrl = url
        if (resetHistory) pendingClearHistory = true

        val online = isOnline(context)
        if (!online) {
            showOffline(wv)
            return
        }

        val targetTitle = when {
            tabIndex != null && tabIndex in items.indices -> items[tabIndex].title
            else -> {
                val idx = tabIndexForUrl(url, selectedIndex)
                if (idx in items.indices) items[idx].title else "Wisdom Tower Academy"
            }
        }
        navigationStatusText = "Opening $targetTitle…"
        startNavigationLoading()
        webProgress = 20

        isOfflineState = false
        lastOnlineUrl = url
        wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
        try {
            wv.stopLoading()
        } catch (_: Exception) {}
        wv.loadUrl(url)
    }

    fun openOrDownloadPdf(wv: WebView, ctx: Context, url: String) {
        val cleanUrl = url.trim()
        val local = OfflineVault.localFileFor(ctx, cleanUrl)
        if (local != null && local.exists() && local.length() > 0) {
            mainHandler.post {
                Toast.makeText(ctx, "Already downloaded — opening", Toast.LENGTH_SHORT).show()
            }
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
        OfflineVault.downloadUserInitiated(ctx, cleanUrl) { file ->
            mainHandler.post {
                if (file != null && file.exists() && file.length() > 0) {
                    OfflineVault.cachePdfSize(ctx, cleanUrl, file.length())
                    Toast.makeText(ctx, "Saved offline", Toast.LENGTH_SHORT).show()
                    wv.loadUrl(OfflineVault.fileUrl(file))
                } else {
                    wv.loadUrl(cleanUrl)
                }
            }
        }
    }

    DisposableEffect(context, webView) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                mainHandler.post {
                    webView?.settings?.cacheMode = WebSettings.LOAD_DEFAULT
                    if (webView?.url?.contains("offline.html") == true) {
                        val target = if (lastOnlineUrl.isNotBlank() && !lastOnlineUrl.startsWith("file://")) lastOnlineUrl else SITE
                        navigateTo(target, null)
                    }
                }
                WebCacheVault.precacheHubsAsync(context)
            }

            override fun onLost(network: android.net.Network) {
                mainHandler.post {
                    webView?.settings?.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                }
            }
        }
        try {
            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {}
        onDispose {
            try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {}
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
                        // Left: Hamburger menu button
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
                                        startNavigationLoading()
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

                    // Compact Study Timer Indicator placed cleanly under the notification bell
                    AnimatedVisibility(
                        visible = isTimerRunning && timerRemainingSeconds > 0 && !isTimerDismissed,
                        enter = expandVertically(tween(180)) + fadeIn(tween(160)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(160))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TopBarStudyTimerIndicator(
                                remainingSeconds = timerRemainingSeconds,
                                onOpenWebTimer = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    val js = "(function(){try{" +
                                        "var el=document.querySelector('#focus-timer,[data-focus-timer],.focus-timer,[id*=\"pomodoro\" i],[class*=\"pomodoro\" i],[id*=\"timer\" i]');" +
                                        "if(el){el.scrollIntoView({behavior:'smooth',block:'center'});}" +
                                        "}catch(e){}})();"
                                    webView?.evaluateJavascript(js, null)
                                },
                                onStopTimer = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    isTimerRunning = false
                                    timerRemainingSeconds = 0
                                    isTimerDismissed = true
                                    val js = "(function(){try{" +
                                        "window.dispatchEvent(new CustomEvent('wta-study-timer-control',{detail:{action:'stop'}}));" +
                                        "var raw=localStorage.getItem('wt_focus_timer_v1');" +
                                        "if(raw){var s=JSON.parse(raw);s.running=false;s.endAt=null;localStorage.setItem('wt_focus_timer_v1',JSON.stringify(s));window.dispatchEvent(new CustomEvent('wt-focus-timer'));}" +
                                        "var btn=document.querySelector('[data-timer-stop],[data-study-timer-stop],button[aria-label*=\"stop\" i],button[aria-label*=\"reset\" i]');" +
                                        "if(btn)btn.click();" +
                                        "}catch(e){}})();"
                                    webView?.evaluateJavascript(js, null)
                                }
                            )
                        }
                    }

                    // Hairline subtle divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(Color(0x1AFFFFFF))
                    )
                }
            },
            bottomBar = {
                AliveBottomNav(
                    items = items,
                    selectedIndex = selectedIndex,
                    onItemSelected = { index, item ->
                        selectedIndex = index
                        lastTargetUrl = item.url
                        val online = isOnline(context)
                        if (!online) {
                            isOfflineState = true
                            stopNavigationLoading()
                            webView?.let { showOffline(it) }
                        } else {
                            isOfflineState = false
                            navigateTo(item.url, index)
                        }
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
                                if (isEditText || isAllowedPage) {
                                    false
                                } else {
                                    true
                                }
                            }
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
                                offscreenPreRaster = false
                                cacheMode = if (isOnline(ctx)) {
                                    WebSettings.LOAD_DEFAULT
                                } else {
                                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                                }
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                try {
                                    val swController = ServiceWorkerController.getInstance()
                                    val swSettings = swController.serviceWorkerWebSettings
                                    swSettings.cacheMode = if (isOnline(ctx)) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                                    swSettings.allowContentAccess = true
                                    swSettings.allowFileAccess = true
                                    swSettings.blockNetworkLoads = false
                                } catch (_: Exception) {}
                            }
                            if (isOnline(ctx)) {
                                WebCacheVault.precacheHubsAsync(ctx)
                            }
                            val cookieMgr = CookieManager.getInstance()
                            cookieMgr.setAcceptCookie(true)
                            cookieMgr.setAcceptThirdPartyCookies(this, true)

                            val startedDownloads = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

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
                                            selectedIndex = tabIndexForUrl(path, selectedIndex)
                                            stopNavigationLoading()
                                        }
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
                                        openOrDownloadPdf(this@apply, ctx, url)
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
                                        if (!msg.isNullOrBlank()) {
                                            navigationStatusText = msg
                                        }
                                        startNavigationLoading(0L)
                                    }
                                }

                                @JavascriptInterface
                                fun notifyLoadingFinished() {
                                    mainHandler.post {
                                        stopNavigationLoading()
                                    }
                                }

                                @JavascriptInterface
                                fun reloadLastOnlinePage() {
                                    mainHandler.post {
                                        webView?.let { wv ->
                                            val online = isOnline(context)
                                            if (online) {
                                                isOfflineState = false
                                                val target = lastTargetUrl.ifBlank { SITE }
                                                navigateTo(target, selectedIndex)
                                            } else {
                                                showOffline(wv)
                                                Toast.makeText(context, "You're offline. Please connect to Wi-Fi or mobile data and try again.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun openOfflineUrl(url: String?) {
                                    if (url.isNullOrBlank()) return
                                    mainHandler.post {
                                        webView?.let { wv ->
                                            val online = isOnline(context)
                                            lastTargetUrl = url
                                            if (online) {
                                                isOfflineState = false
                                                navigateTo(url, null)
                                            } else {
                                                showOffline(wv)
                                                Toast.makeText(context, "You're offline. Please connect to Wi-Fi or mobile data and try again.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun showOfflinePage() {
                                    mainHandler.post {
                                        webView?.let { showOffline(it) }
                                    }
                                }

                                @JavascriptInterface
                                fun syncStudyTimer(isRunning: Boolean, secondsLeft: Int, totalSeconds: Int, title: String?) {
                                    mainHandler.post {
                                        isTimerRunning = isRunning
                                        timerRemainingSeconds = if (isRunning) secondsLeft.coerceAtLeast(0) else 0
                                        timerTotalSeconds = totalSeconds.coerceAtLeast(0)
                                        if (!title.isNullOrBlank()) {
                                            timerTitle = title
                                        }
                                        if (!isRunning) {
                                            isTimerDismissed = false
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun isStudyTimerActive(): Boolean {
                                    return isTimerRunning && timerRemainingSeconds > 0 && !isTimerDismissed
                                }
                            }, "AndroidOfflineVault")

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    webProgress = newProgress
                                    if (newProgress >= 70) {
                                        stopNavigationLoading()
                                    }
                                    if (newProgress >= 60) {
                                        pageRendered = true
                                        if (minSplashElapsed) {
                                            isInitialLoading = false
                                        }
                                    }
                                }

                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    val msg = consoleMessage?.message().orEmpty()
                                    val line = consoleMessage?.lineNumber() ?: 0
                                    val src = consoleMessage?.sourceId().orEmpty()
                                    android.util.Log.d("WTA_JS", "[$src:$line] $msg")
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    val wv = view ?: return
                                    if (!isOnline(ctx) && url != null && !url.startsWith("file:///android_asset/")) {
                                        try {
                                            wv.stopLoading()
                                        } catch (_: Exception) {}
                                        showOffline(wv)
                                        return
                                    }
                                    if (url != null && !url.startsWith("file://")) {
                                        val newIdx = tabIndexForUrl(url, selectedIndex)
                                        selectedIndex = newIdx
                                        if (!isInitialLoading) {
                                            val title = if (newIdx in items.indices) items[newIdx].title else "Wisdom Tower Academy"
                                            navigationStatusText = "Opening $title…"
                                            startNavigationLoading()
                                        }
                                    }
                                    if (url != null && url.startsWith("file:///android_asset/")) {
                                        isInitialLoading = false
                                        stopNavigationLoading()
                                    }
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    if (url != null && !url.startsWith("file://")) {
                                        selectedIndex = tabIndexForUrl(url, selectedIndex)
                                    }
                                    pageRendered = true
                                    if (minSplashElapsed) {
                                        isInitialLoading = false
                                    }
                                    stopNavigationLoading()
                                    view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                    view?.evaluateJavascript(PRECACHE_AND_UNBLOCK_JS, null)
                                    view?.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                    view?.evaluateJavascript(STUDY_TIMER_BRIDGE_JS, null)
                                    view?.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    val wv = view ?: return
                                    val curUrl = url ?: wv.url
                                    if (curUrl != null && !curUrl.startsWith("file://") && !curUrl.contains("offline.html")) {
                                        selectedIndex = tabIndexForUrl(curUrl, selectedIndex)
                                        if (isOnline(ctx)) {
                                            isOfflineState = false
                                        } else {
                                            showOffline(wv)
                                            return
                                        }
                                    }
                                    pageRendered = true
                                    if (minSplashElapsed) {
                                        isInitialLoading = false
                                    }
                                    stopNavigationLoading()
                                    wv.evaluateJavascript(NATIVE_CHROME_JS, null)
                                    wv.evaluateJavascript(PRECACHE_AND_UNBLOCK_JS, null)
                                    wv.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                    wv.evaluateJavascript(STUDY_TIMER_BRIDGE_JS, null)
                                    wv.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                    mainHandler.postDelayed({
                                        val cur = wv.url ?: ""
                                        if (!cur.startsWith("file://")) {
                                            wv.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                            wv.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                            wv.evaluateJavascript(STUDY_TIMER_BRIDGE_JS, null)
                                        }
                                    }, 800L)
                                    if (pendingClearHistory) {
                                        pendingClearHistory = false
                                        wv.clearHistory()
                                    }
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    if (url != null && !url.startsWith("file://")) {
                                        selectedIndex = tabIndexForUrl(url, selectedIndex)
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
                                    val failedUrl = request.url?.toString().orEmpty()
                                    if (failedUrl.startsWith("file:///android_asset/")) return

                                    if (statusCode in listOf(404, 500, 502, 503, 504) || !isOnline(ctx)) {
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
                                    val failedUrl = request.url?.toString().orEmpty()
                                    if (failedUrl.startsWith("file:///android_asset/")) return
                                    showOffline(wv)
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    val wv = view ?: return
                                    val failedUrl = failingUrl.orEmpty()
                                    if (failedUrl.startsWith("file:///android_asset/")) return
                                    showOffline(wv)
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.cancel()
                                    val wv = view ?: return
                                    showOffline(wv)
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    val wv = view ?: return true
                                    try {
                                        (wv.parent as? ViewGroup)?.removeView(wv)
                                        wv.destroy()
                                    } catch (_: Exception) {}
                                    mainHandler.post {
                                        val targetUrl = lastOnlineUrl.ifBlank { SITE }
                                        webView?.loadUrl(targetUrl)
                                    }
                                    return true
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val req = request ?: return null
                                    val u = req.url?.toString() ?: return null
                                    val method = req.method?.uppercase() ?: "GET"
                                    val isGet = method == "GET"
                                    val isHead = method == "HEAD"

                                    if ((isGet || isHead) && (u.contains("/api/content/pdf") || OfflineVault.isPdfUrl(u))) {
                                        val rangeHeader = req.requestHeaders?.entries?.firstOrNull { it.key.equals("Range", ignoreCase = true) }?.value
                                        val isRangeProbe = rangeHeader != null && rangeHeader.matches(Regex("""bytes=\s*0-\s*[01]"""))
                                        val isExplicitDownload = startedDownloads.contains(u.trim())
                                        val isProbe = isHead || isRangeProbe

                                        // 1. Lightweight Size Probes: Answer ONLY from local/catalog/memory, NEVER download body or save to vault
                                        if (isProbe) {
                                            val local = OfflineVault.localFileFor(ctx, u)
                                            var size = if (local != null && local.exists() && local.length() > 0L) {
                                                local.length()
                                            } else {
                                                OfflineVault.getPdfSize(ctx, u)
                                            }
                                            if (size <= 0L) {
                                                size = OfflineVault.getPdfSize(ctx, OfflineVault.normalizeUrl(u))
                                            }
                                            if (size <= 0L) {
                                                size = OfflineVault.probeSizeOnline(ctx, u)
                                            }
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
                                                "binary",
                                                status,
                                                statusText,
                                                headers,
                                                ByteArrayInputStream(if (size > 0L && !isHead) ByteArray(1) else ByteArray(0))
                                            )
                                        }

                                        // 2. Full Download/View (User explicitly clicked Download & open):
                                        val local = OfflineVault.localFileFor(ctx, u)
                                        if (local != null && local.exists() && local.length() > 0L) {
                                            if (isExplicitDownload) {
                                                mainHandler.post {
                                                    Toast.makeText(ctx, "Already downloaded — opening", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            return try {
                                                val headers = HashMap<String, String>().apply {
                                                    put("Access-Control-Allow-Origin", "*")
                                                    put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                                                    put("Access-Control-Allow-Headers", "*")
                                                    put("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges, Content-Type")
                                                    put("Content-Type", "application/pdf")
                                                    put("Content-Length", local.length().toString())
                                                    put("Accept-Ranges", "bytes")
                                                    put("Cache-Control", "public, max-age=31536000, immutable")
                                                }
                                                WebResourceResponse("application/pdf", "binary", 200, "OK", headers, FileInputStream(local))
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                        if (isOnline(ctx)) {
                                            return try {
                                                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                                                    requestMethod = "GET"
                                                    connectTimeout = 30_000
                                                    readTimeout = 60_000
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
                                                    val totalLen = conn.contentLengthLong
                                                    val mime = conn.contentType ?: "application/pdf"
                                                    var totalToUse = if (totalLen > 0L) totalLen else OfflineVault.getPdfSize(ctx, u)
                                                    if (totalToUse <= 0L) {
                                                        totalToUse = OfflineVault.getPdfSize(ctx, OfflineVault.normalizeUrl(u))
                                                    }
                                                    if (totalToUse <= 0L) {
                                                        totalToUse = OfflineVault.probeSizeOnline(ctx, u)
                                                    }
                                                    val headers = HashMap<String, String>().apply {
                                                        put("Access-Control-Allow-Origin", "*")
                                                        put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                                                        put("Access-Control-Allow-Headers", "*")
                                                        put("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges, Content-Type")
                                                        put("Content-Type", mime)
                                                        if (totalToUse > 0L) {
                                                            put("Content-Length", totalToUse.toString())
                                                        } else if (totalLen > 0L) {
                                                            put("Content-Length", totalLen.toString())
                                                        }
                                                        put("Accept-Ranges", "bytes")
                                                        put("Cache-Control", "public, max-age=31536000, immutable")
                                                    }
                                                    val stream = if (isExplicitDownload || u.contains("/api/content/pdf") || OfflineVault.isPdfUrl(u)) {
                                                        val targetFile = OfflineVault.targetFileFor(ctx, u)
                                                        CachingInputStream(conn.inputStream, targetFile, u, totalToUse) { savedFile ->
                                                            OfflineVault.remember(ctx, u, savedFile.name)
                                                            OfflineVault.cachePdfSize(ctx, u, savedFile.length())
                                                        }
                                                    } else {
                                                        conn.inputStream
                                                    }
                                                    WebResourceResponse(mime, "binary", 200, "OK", headers, stream)
                                                } else {
                                                    conn.disconnect()
                                                    null
                                                }
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                    }

                                    // 2. WebCacheVault handling for HTML pages, thumbnails, Next.js static assets, notes, and hubs
                                    if (!isOnline(ctx)) {
                                        if (req.isForMainFrame) {
                                            mainHandler.post {
                                                view?.let { showOffline(it) }
                                            }
                                            return WebResourceResponse("text/html", "utf-8", 200, "OK", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                                        }
                                        if (OfflineVault.isPdfUrl(u)) {
                                            val local = OfflineVault.localFileFor(ctx, u)
                                            if (local != null && local.exists() && local.length() > 0L) {
                                                val headers = mapOf(
                                                    "Access-Control-Allow-Origin" to "*",
                                                    "Content-Type" to "application/pdf"
                                                )
                                                return WebResourceResponse("application/pdf", "binary", 200, "OK", headers, FileInputStream(local))
                                            }
                                        }
                                        return null
                                    } else {
                                        // ONLINE:
                                        // If resource is already cached in WebCacheVault (HTML, CSS, JS, thumbnails, images, fonts),
                                        // serve immediately from disk for 0ms load speed.
                                        // Otherwise let Chromium download natively via its high-performance HTTP/2 pipeline into its disk cache.
                                        if (WebCacheVault.has(ctx, u)) {
                                            val cached = WebCacheVault.getCachedResponse(ctx, u)
                                            if (cached != null) return cached
                                        }
                                    }

                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val u = request?.url?.toString() ?: return false
                                    val wv = view ?: return false
                                    if (!isOnline(ctx)) {
                                        showOffline(wv)
                                        return true
                                    }
                                    if (u.startsWith("chrome-error://") || u.startsWith("about:neterror")) {
                                        showOffline(wv)
                                        return true
                                    }
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

                            if (isOnline(ctx)) {
                                loadUrl(SITE)
                            } else {
                                showOffline(this)
                            }
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Sleek high-tech neon cyan progress line at top of WebView: non-blocking, lightning fast!
                AnimatedVisibility(
                    visible = isNavigating && !isInitialLoading,
                    enter = fadeIn(tween(80)),
                    exit = fadeOut(tween(250)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .zIndex(100f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x3300E5FF),
                                        Color(0xFF00E5FF),
                                        Color(0xFF38BDF8),
                                        Color(0xFF00E5FF),
                                        Color(0x3300E5FF)
                                    )
                                )
                            )
                    )
                }

                // Custom loading animation in transitions (learning to home.. To setting etc..)
                AnimatedVisibility(
                    visible = isNavigating && !isInitialLoading,
                    enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.92f, animationSpec = tween(180)),
                    exit = fadeOut(tween(260)) + scaleOut(targetScale = 0.96f, animationSpec = tween(220)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(95f)
                ) {
                    CenteredBigCircularLoader(
                        modifier = Modifier.background(Color(0x8A070E1A)),
                        statusText = navigationStatusText,
                        subText = "Switching section…",
                        progress = webProgress,
                        isSplash = false
                    )
                }

                // Perfect Timing Splash / Loading Overlay: Eliminates blank screens
                AnimatedVisibility(
                    visible = isInitialLoading,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(380, easing = FastOutSlowInEasing))
                ) {
                    CenteredBigCircularLoader(
                        modifier = Modifier.background(BarBg),
                        statusText = "Wisdom Tower Academy",
                        subText = "Preparing your learning space…",
                        progress = webProgress,
                        isSplash = true
                    )
                }

                // Native Compose Offline / Error Notice Overlay
                AnimatedVisibility(
                    visible = isOfflineState,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.95f),
                    exit = fadeOut(tween(180)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(140f)
                ) {
                    NativeOfflineNotice(
                        onRetry = {
                            val online = isOnline(context)
                            if (online) {
                                isOfflineState = false
                                webView?.let { wv ->
                                    val target = lastTargetUrl.ifBlank { SITE }
                                    wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    wv.loadUrl(target)
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "You're offline. Please connect to Wi-Fi or mobile data and try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }

        if (menuExpanded) {
            Dialog(
                onDismissRequest = { menuExpanded = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { menuExpanded = false },
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        color = Color(0xF4080F1E),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color(0x3300E5FF)),
                        shadowElevation = 20.dp,
                        modifier = Modifier
                            .padding(start = 14.dp, top = 54.dp, end = 14.dp)
                            .widthIn(min = 260.dp, max = 300.dp)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* prevent close on card click */ }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x2600E5FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = null,
                                            tint = Accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Menu",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        menuExpanded = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close",
                                        tint = Muted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0x2200E5FF))
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // 5 clean links
                            overflowMenuLinks.forEach { link ->
                                val linkInteraction = remember { MutableInteractionSource() }
                                val linkPressed by linkInteraction.collectIsPressedAsState()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (linkPressed) Color(0x2600E5FF) else Color.Transparent)
                                        .clickable(
                                            interactionSource = linkInteraction,
                                            indication = ripple(color = Accent.copy(alpha = 0.2f))
                                        ) {
                                            menuExpanded = false
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            navigateTo(link.url)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x1A00E5FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = link.icon,
                                                contentDescription = null,
                                                tint = Accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = link.label,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = Muted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0x14FFFFFF))
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Wisdom Tower Academy",
                                color = Muted.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
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
 * Universal high-speed centered circular animated loader.
 * Futuristic cybernetic aesthetic:
 * 1. Fast dual opposite-direction spinning neon rings (cyan & violet/indigo).
 * 2. Radial outward thick gear ticks / cog teeth rotating fast between the two circles.
 * 3. Subtle frosted card background with ambient glow (compact for page loads/navigation, full-screen for splash).
 */
@Composable
private fun CenteredBigCircularLoader(
    modifier: Modifier = Modifier,
    statusText: String = "Wisdom Tower Academy",
    subText: String = "Loading…",
    progress: Int = 0,
    isSplash: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isSplash) {
            // Full-screen splash layout
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
                FuturisticGearRings(
                    size = 160.dp,
                    brandSize = 100.dp,
                    numTicks = 18,
                    tickInnerRatio = 0.70f,
                    tickOuterRatio = 0.88f,
                    tickWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subText,
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )

                if (progress in 1..99) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LoaderProgressPill(progress = progress, compact = false)
                }
            }
        } else {
            // Elegant frosted transition card with custom animated loader, glowing neon border, and section title
            Surface(
                color = Color(0xF20B132B),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color(0x8000E5FF), Color(0x33818CF8)))),
                shadowElevation = 16.dp,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 20.dp)
                ) {
                    FuturisticGearRings(
                        size = 88.dp,
                        brandSize = 52.dp,
                        numTicks = 14,
                        tickInnerRatio = 0.68f,
                        tickOuterRatio = 0.88f,
                        tickWidth = 2.8.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )

                    if (progress in 1..99) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LoaderProgressPill(progress = progress, compact = true)
                    }
                }
            }
        }
    }
}

/**
 * High-speed dual orbital neon rings with outward thick gear ticks / teeth rotating between them.
 */
@Composable
private fun FuturisticGearRings(
    size: Dp,
    brandSize: Dp,
    numTicks: Int,
    tickInnerRatio: Float,
    tickOuterRatio: Float,
    tickWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val transition = rememberInfiniteTransition(label = "futuristicGear")
        val fastSpin by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fastSpin"
        )
        val gearSpin by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gearSpin"
        )
        val counterSpin by transition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(950, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "counterSpin"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f - 3.dp.toPx()
            val innerRadius = outerRadius * 0.62f
            val rGearInner = outerRadius * tickInnerRatio
            val rGearOuter = outerRadius * tickOuterRatio
            val tickPx = tickWidth.toPx()

            // 1. Futuristic Gear base track circle
            drawCircle(
                color = Color(0x3300E5FF),
                radius = rGearInner,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Outward thick gear teeth / ticks spinning fast
            for (i in 0 until numTicks) {
                val tickAngle = gearSpin + (i * 360f / numTicks)
                val rad = Math.toRadians(tickAngle.toDouble())
                val cos = Math.cos(rad).toFloat()
                val sin = Math.sin(rad).toFloat()
                val p1 = Offset(centerOffset.x + rGearInner * cos, centerOffset.y + rGearInner * sin)
                val p2 = Offset(centerOffset.x + rGearOuter * cos, centerOffset.y + rGearOuter * sin)
                val tickColor = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFF38BDF8)
                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = tickPx,
                    cap = StrokeCap.Round
                )
            }

            // 3. Outer Neon Cyan Arc spinning fast (clockwise)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0x0000E5FF),
                        Color(0x5500E5FF),
                        Color(0xFF00E5FF),
                        Color(0xFF38BDF8)
                    )
                ),
                startAngle = fastSpin,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(centerOffset.x - outerRadius, centerOffset.y - outerRadius),
                size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Inner Neon Violet Arc spinning fast in opposite direction (counter-clockwise)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0x00818CF8),
                        Color(0x55818CF8),
                        Color(0xFF818CF8),
                        Color(0xFF00E5FF)
                    )
                ),
                startAngle = counterSpin,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(centerOffset.x - innerRadius, centerOffset.y - innerRadius),
                size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Brand animated GIF in center
        BrandLoader(size = brandSize, showCard = false)
    }
}

@Composable
private fun LoaderProgressPill(
    progress: Int,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x2600E5FF))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
            .padding(
                horizontal = if (compact) 9.dp else 12.dp,
                vertical = if (compact) 3.5.dp else 5.dp
            )
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loaderPulse")
        val dotAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "loaderDotAlpha"
        )
        Box(
            modifier = Modifier
                .size(if (compact) 5.dp else 6.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = dotAlpha))
        )
        Text(
            text = "Loading $progress%",
            color = Color(0xFF38BDF8),
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * Compact native Study Timer status indicator in the top bar directly under the notification bell.
 * Displays only while the web Pomodoro/Focus timer is actively running.
 * Tapping the pill scrolls/opens the web timer; tapping the inline close (X) reliably stops the timer.
 */
@Composable
private fun TopBarStudyTimerIndicator(
    remainingSeconds: Int,
    onOpenWebTimer: () -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "topBarTimerPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "topBarTimerDotAlpha"
    )

    Surface(
        color = Color(0xF20B132B),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0x6600E5FF)),
        shadowElevation = 4.dp,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenWebTimer)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
        ) {
            // Pulsing cyan indicator dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = dotAlpha))
            )

            // Live countdown text
            Text(
                text = timeFormatted,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )

            // Clear, always-visible Stop / Close (X) button
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable(
                        onClick = onStopTimer,
                        role = androidx.compose.ui.semantics.Role.Button
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Stop timer",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Advanced, high-tech bottom navigation bar.
 * Clean, precise icon + typography tinting, ZERO clutter dots.
 * Icons are strictly contained within their capsule bounds with non-bouncy micro-press damping.
 * Background blends 100% seamlessly into the mobile screen bottom edge with NO gap and NO color difference.
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
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Elegant cyber glow top border line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x3300E5FF),
                                Color(0x6638BDF8),
                                Color(0x3300E5FF),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEachIndexed { index, item ->
                    val selected = selectedIndex == index
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    // Non-bouncy smooth micro-press: inward to 0.95f, zero overshoot on release so icon NEVER pops out!
                    val contentScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tabContentScale"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (selected) Accent else if (isHovered) Color.White else Muted,
                        animationSpec = tween(160),
                        label = "tabIconColor"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (selected) Accent else if (isHovered) Color.White else Muted,
                        animationSpec = tween(160),
                        label = "tabTextColor"
                    )

                    val capsuleShape = RoundedCornerShape(14.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .padding(horizontal = 1.5.dp)
                            .clip(capsuleShape)
                            .then(
                                if (selected) {
                                    Modifier
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0x3300E5FF),
                                                    Color(0x1400E5FF)
                                                )
                                            )
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0x8000E5FF),
                                                        Color(0x2400E5FF)
                                                    )
                                                )
                                            ),
                                            capsuleShape
                                        )
                                } else if (isHovered) {
                                    Modifier.background(Color(0x12FFFFFF))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(
                                    bounded = true,
                                    color = Accent.copy(alpha = 0.2f)
                                )
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onItemSelected(index, item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(contentScale)
                                .padding(vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    // Subtle cybernetic neon glow behind active icon
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color(0x4000E5FF),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                color = textColor,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean native Jetpack Compose Offline Notice Screen
 * Replaces emoji-heavy version with native Android vector icon (satellite / cloud-off)
 * and clear, actionable retry mechanism.
 */
@Composable
private fun NativeOfflineNotice(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BarBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .border(BorderStroke(1.dp, Color(0x3300E5FF)), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Native satellite / cloud-off style vector icon (NO emojis)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A00E5FF))
                    .border(BorderStroke(1.5.dp, Color(0x4D00E5FF)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Offline indicator",
                    tint = Accent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "You're offline",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "You're offline. Please connect to Wi-Fi or mobile data and try again.",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = BarBg
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Try again",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
