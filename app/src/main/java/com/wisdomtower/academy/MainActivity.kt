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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import com.wisdomtower.academy.fcm.AcademyFirebaseMessagingService
import com.wisdomtower.academy.fcm.FcmTokenRegistrar
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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

// Unified native tokens mirroring the live website (wisdom.* palette)
private val BarBg = Color(0xFF060B15) // Deep navy, exactly matching website page background
private val Accent = Color(0xFF22E0FF) // Website source of truth cyan
private val AccentDark = Color(0xFF00C4E6)
private val CardSurface = Color(0xFF0C1424) // Glass-like card surface
private val CardBorder = Color(0x3322E0FF) // Soft 1px cyan border
private val CardBorderSubtle = Color(0x1F22E0FF) // Hairline quiet divider
private val SurfaceColor = CardSurface
private val Muted = Color(0xFF94A3B8)
private val TextPrimary = Color(0xFFF8FAFC)
private val DarkOnCyan = Color(0xFF070D17) // Dark text on solid cyan pills

private const val OFFLINE_ASSET = "file:///android_asset/offline.html"
// Direct 200 URL (eliminates 308 redirect round-trip delay)
private const val SITE = "https://www.wisdom-tower-academy.live/"
private const val MIN_SPLASH_DISPLAY_MS = 1200L

private const val SOFT_NAV_JS =
    "(function(targetUrl){try{" +
        "if(!targetUrl||window.location.protocol==='file:')return 'fallback';" +
        "var cur=window.location.href;" +
        "if(cur===targetUrl)return 'noop';" +
        "var path=targetUrl;" +
        "try{var u=new URL(targetUrl,window.location.origin);path=u.pathname+u.search+u.hash;}catch(_){}" +
        "try{" +
            "window.dispatchEvent(new CustomEvent('wta-navigate',{detail:{path:path,url:targetUrl}}));" +
        "}catch(_){}" +
        "if(typeof window.__wtaNavigate==='function'){" +
            "try{var r=window.__wtaNavigate(path);if(r===true||r==='ok')return 'ok';}catch(_){}" +
        "}" +
        "if(window.next&&window.next.router&&typeof window.next.router.push==='function'){" +
            "try{window.next.router.push(path);return 'ok';}catch(_){}" +
        "}" +
        "try{" +
            "var existing=document.querySelector('a[href=\"'+path+'\"],a[href=\"'+targetUrl+'\"]');" +
            "if(existing){existing.click();return 'ok';}" +
        "}catch(_){}" +
        "return 'fallback';" +
    "}catch(e){return 'fallback';}})"

private const val CRITICAL_CHROME_STYLE =
    "<style id=\"wta-critical-hide\">" +
    "html,body{background-color:#060B15!important;color-scheme:dark!important;}" +
    "header,header.fixed.top-0,header[data-site-header],.site-header,[data-site-header],[role=\"banner\"]," +
    "nav[aria-label=\"Main\"],nav.hidden.md\\:flex,.site-nav,.site-navigation,[data-site-nav]," +
    "body > footer,footer.site-footer,[data-site-footer],[role=\"contentinfo\"]," +
    ".hide-on-app,.app-hidden,[data-hide-on-app],[data-hide-app],.web-only,[data-web-only]," +
    "#nprogress,.nprogress,#nprogress .bar,[data-nprogress],#nextjs-toploader,.nextjs-toploader," +
    "nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast]," +
    "header button[aria-label*=\"menu\" i],button[aria-label*=\"menu\" i],.mobile-menu,[data-mobile-menu]," +
    "nav[aria-label*=\"mobile\" i],[data-bottom-nav],.bottom-nav,nav.fixed.bottom-0," +
    "img.wta-img-broken,img:not([src]),img[src=\"\"]" +
    "{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;opacity:0!important;pointer-events:none!important;margin:0!important;padding:0!important;}" +
    "div:has(#wt-ai-tutor-input){bottom:0!important;}" +
    "footer:has(input),footer:has(form),footer:has(#wt-ai-tutor-input),#wt-ai-tutor-input,[role=\"dialog\"] footer" +
    "{display:block!important;visibility:visible!important;height:auto!important;max-height:none!important;opacity:1!important;pointer-events:auto!important;}" +
    "</style>"

private const val EARLY_HIDE_CHROME_JS =
    "(function(){try{" +
        "if(window.location.protocol==='file:')return;" +
        "document.documentElement.classList.add('wta-native-app');" +
        "if(document.body){document.body.classList.add('wta-native-app');}" +
        "var id='wta-app-chrome';var s=document.getElementById(id);" +
        "if(!s){s=document.createElement('style');s.id=id;var head=document.head||document.documentElement;if(head){head.insertBefore(s,head.firstChild);}}" +
        "s.textContent='html,body{background-color:#060B15!important;color-scheme:dark!important;}header,header.fixed.top-0,header[data-site-header],body > footer,footer.site-footer,[data-site-footer],.site-header,.site-footer," +
        "nav[aria-label=\"Main\"],nav.hidden.md\\\\:flex,.site-nav,.site-navigation,[data-site-nav],[role=\"banner\"],[role=\"contentinfo\"]," +
        ".hide-on-app,.app-hidden,[data-hide-on-app],[data-hide-app],.web-only,[data-web-only]," +
        "#nprogress,.nprogress,#nprogress .bar,[data-nprogress],#nextjs-toploader,.nextjs-toploader," +
        "nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast],header button[aria-label*=\"menu\" i],button[aria-label*=\"menu\" i],.mobile-menu,[data-mobile-menu],nav[aria-label*=\"mobile\" i],[data-bottom-nav],.bottom-nav,nav.fixed.bottom-0,img.wta-img-broken,img:not([src]),img[src=\"\"]" +
        "{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;opacity:0!important;pointer-events:none!important;margin:0!important;padding:0!important;}" +
        "div:has(#wt-ai-tutor-input){bottom:0!important;}" +
        "footer:has(input),footer:has(form),footer:has(#wt-ai-tutor-input),#wt-ai-tutor-input,[role=\"dialog\"] footer" +
        "{display:block!important;visibility:visible!important;height:auto!important;max-height:none!important;opacity:1!important;pointer-events:auto!important;}';" +
    "}catch(e){}})"

private const val NATIVE_CHROME_JS =
    "(function(){try{" +
        "if(window.location.protocol==='file:')return;" +
        "document.documentElement.classList.add('wta-native-app');" +
        "if(document.body){document.body.classList.add('wta-native-app');}" +
        "var id='wta-app-chrome';var s=document.getElementById(id);" +
        "if(!s){s=document.createElement('style');s.id=id;var head=document.head||document.documentElement;if(head){head.insertBefore(s,head.firstChild);}}" +
        "s.textContent='html,body{background-color:#060B15!important;color-scheme:dark!important;}' +" +
        "'header,header.fixed.top-0,header[data-site-header],body > footer,footer.site-footer,[data-site-footer],.site-header,.site-footer,' +" +
        "'nav[aria-label=\"Main\"],nav.hidden.md\\\\:flex,.site-nav,.site-navigation,[data-site-nav],[role=\"banner\"],[role=\"contentinfo\"],' +" +
        "'.hide-on-app,.app-hidden,[data-hide-on-app],[data-hide-app],.web-only,[data-web-only],' +" +
        "'#nprogress,.nprogress,#nprogress .bar,[data-nprogress],#nextjs-toploader,.nextjs-toploader,' +" +
        "'nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast],' +" +
        "'header button[aria-label*=\"menu\" i],button[aria-label*=\"menu\" i],.mobile-menu,[data-mobile-menu],nav[aria-label*=\"mobile\" i],[data-bottom-nav],.bottom-nav,nav.fixed.bottom-0,' +" +
        "'img.wta-img-broken,img:not([src]),img[src=\"\"]' +" +
        "'{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;opacity:0!important;pointer-events:none!important;margin:0!important;padding:0!important;}' +" +
        "'div:has(#wt-ai-tutor-input){bottom:0!important;}' +" +
        "'footer:has(input),footer:has(form),footer:has(#wt-ai-tutor-input),#wt-ai-tutor-input,[role=\"dialog\"] footer' +" +
        "'{display:block!important;visibility:visible!important;height:auto!important;max-height:none!important;opacity:1!important;pointer-events:auto!important;}';" +
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
        "function cleanSettingsUI(){" +
            "try{" +
                "var curPath=(window.location.pathname||'').toLowerCase();" +
                "if(curPath.indexOf('/settings')===-1)return;" +
                "var nodes=document.querySelectorAll('h1,h2,h3,h4,p,span,button');" +
                "for(var k=0;k<nodes.length;k++){" +
                    "var el=nodes[k];" +
                    "var t=(el.textContent||'').trim().toLowerCase();" +
                    "var isGameSound=t==='game sound'||t==='game sound effects'||t==='game audio volume'||t.indexOf('game sound')!==-1||t.indexOf('educational games and study challenges')!==-1;" +
                    "var isCloudSync=t==='offline data & cloud sync'||t.indexOf('offline data & cloud sync')!==-1||t.indexOf('total cached offline data')!==-1||t==='sync to cloud'||t.indexOf('sync to cloud')!==-1||t.indexOf('cloud synchronization')!==-1||t.indexOf('cached by the app')!==-1;" +
                    "if(isGameSound||isCloudSync){" +
                        "var card=el.closest?el.closest('.rounded-3xl, .rounded-2xl, section, article, div.border'):null;" +
                        "if(card&&card!==document.body&&card!==document.documentElement){" +
                            "card.style.setProperty('display','none','important');" +
                        "}" +
                    "}" +
                "}" +
            "}catch(_){}" +
        "}" +
        "cleanSettingsUI();" +
        "if(!window.__wta_settings_mo&&window.MutationObserver){" +
            "window.__wta_settings_mo=true;" +
            "var mo=new MutationObserver(function(){cleanSettingsUI();});" +
            "mo.observe(document.documentElement,{childList:true,subtree:true});" +
        "}" +
        "if(!window.__wta_settings_cleaner){" +
            "window.__wta_settings_cleaner=true;" +
            "setInterval(cleanSettingsUI,300);" +
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
            "window.addEventListener('error',function(e){" +
                "var m=(e&&e.message)?e.message:'';" +
                "if(m.indexOf('Loading chunk')!==-1||m.indexOf('ChunkLoadError')!==-1){" +
                    "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.showOfflinePage==='function'){" +
                        "window.AndroidOfflineVault.showOfflinePage();" +
                    "}" +
                "}" +
            "});" +
            "window.addEventListener('unhandledrejection',function(e){" +
                "var r=e?(String(e.reason||'')):'';" +
                "if(r.indexOf('Loading chunk')!==-1||r.indexOf('ChunkLoadError')!==-1){" +
                    "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.showOfflinePage==='function'){" +
                        "window.AndroidOfflineVault.showOfflinePage();" +
                    "}" +
                "}" +
            "});" +
            "function checkRawErrors(){" +
                "try{" +
                    "var p=(window.location.pathname||'').toLowerCase();" +
                    "var h=(window.location.href||'').toLowerCase();" +
                    "if(p.indexOf('/offline')!==-1||h.indexOf('/offline')!==-1){" +
                        "if(navigator.onLine===false){" +
                            "if(document.documentElement)document.documentElement.style.display='none';" +
                            "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.showOfflinePage==='function'){" +
                                "window.AndroidOfflineVault.showOfflinePage();return;" +
                            "}else{location.replace('file:///android_asset/offline.html');return;}" +
                        "}" +
                    "}" +
                    "var text=(document.body&&document.body.innerText)?document.body.innerText:'';" +
                    "if(!text)text=(document.body&&document.body.textContent)?document.body.textContent:'';" +
                    "var isFatalError=text.indexOf('Loading chunk')!==-1||" +
                        "text.indexOf('ChunkLoadError')!==-1||" +
                        "text.indexOf('TEMPORARY DISPLAY ISSUE')!==-1||" +
                        "text.indexOf('Temporary Display Issue')!==-1||" +
                        "text.indexOf('Application error: a client-side exception')!==-1||" +
                        "text.indexOf('ERR_INTERNET_DISCONNECTED')!==-1||" +
                        "text.indexOf('ERR_CONNECTION_REFUSED')!==-1||" +
                        "text.indexOf('ERR_NAME_NOT_RESOLVED')!==-1;" +
                    "if(isFatalError&&navigator.onLine===false){" +
                        "if(document.documentElement)document.documentElement.style.display='none';" +
                        "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.showOfflinePage==='function'){" +
                            "window.AndroidOfflineVault.showOfflinePage();" +
                        "}else{location.replace('file:///android_asset/offline.html');}" +
                    "}" +
                "}catch(_){}" +
            "}" +
            "checkRawErrors();" +
            "if(!window.__wta_err_obs&&window.MutationObserver){" +
                "window.__wta_err_obs=new MutationObserver(checkRawErrors);" +
                "window.__wta_err_obs.observe(document.documentElement||document.body,{childList:true,subtree:true,characterData:true});" +
            "}" +
        "}" +
        "if(!window.__wta_route_monitor){" +
            "window.__wta_route_monitor=true;" +
            "window.addEventListener('popstate',function(){" +
                "cleanSettingsUI();" +
                "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.notifyLoadingFinished==='function'){" +
                    "window.AndroidOfflineVault.notifyLoadingFinished();" +
                "}" +
            "});" +
        "}" +
        "}catch(e){}})();"

private const val AI_TUTOR_CHROME_JS =
    "(function(){try{" +
        "if(window.location.protocol==='file:')return;" +
        "var styleId='wta-ai-tutor-style';" +
        "var st=document.getElementById(styleId);" +
        "if(!st){" +
            "st=document.createElement('style');" +
            "st.id=styleId;" +
            "st.textContent=" +
                "'@keyframes wtaSpinFast{0%{transform:rotate(0deg);}100%{transform:rotate(360deg);}}' +" +
                "'@keyframes wtaSpinGear{0%{transform:rotate(0deg);}100%{transform:rotate(360deg);}}' +" +
                "'@keyframes wtaSpinReverse{0%{transform:rotate(360deg);}100%{transform:rotate(0deg);}}' +" +
                "'div:has(#wt-ai-tutor-input){bottom:0!important;}' +" +
                "'footer:has(#wt-ai-tutor-input){display:block!important;visibility:visible!important;position:sticky!important;bottom:0!important;z-index:50!important;background-color:#070d1d!important;border-top:1px solid rgba(255,255,255,0.12)!important;padding-bottom:max(0.5rem,env(safe-area-inset-bottom,0px))!important;}' +" +
                "'#wt-ai-tutor-input{font-size:16px!important;color:#FFFFFF!important;caret-color:#00E5FF!important;background-color:#060B17!important;visibility:visible!important;opacity:1!important;}' +" +
                "'.wta-tutor-avatar,img.wta-tutor-avatar-img{animation:none!important;transform:none!important;}';" +
            "(document.head||document.documentElement).appendChild(st);" +
        "}" +
        "function syncAiTutorChrome(){" +
            "try{" +
                "var p=(window.location.pathname||'').toLowerCase();" +
                "var q=(window.location.search||'').toLowerCase();" +
                "var isTutor=p.indexOf('/learning')!==-1&&q.indexOf('tool=tutor')!==-1;" +
                "var hasTutorInput=document.getElementById('wt-ai-tutor-input')!==null;" +
                "if(!isTutor&&!hasTutorInput)return;" +
                "var botBoxes=document.querySelectorAll('div.w-8.h-8.rounded-xl, [data-bot-avatar]');" +
                "for(var i=0;i<botBoxes.length;i++){" +
                    "var box=botBoxes[i];" +
                    "if(box.getAttribute('data-wta-avatar')==='ready')continue;" +
                    "var svg=box.querySelector('svg');" +
                    "if(svg){" +
                        "var cls=box.className||'';" +
                        "var isBot=cls.indexOf('cyan')!==-1||cls.indexOf('indigo')!==-1||cls.indexOf('shadow')!==-1;" +
                        "var parentRow=box.closest('.flex');" +
                        "if(isBot||(parentRow&&(parentRow.textContent||'').indexOf('Thinking')!==-1)){" +
                            "box.setAttribute('data-wta-avatar','ready');" +
                            "box.style.padding='0';" +
                            "box.style.overflow='hidden';" +
                            "box.style.display='flex';" +
                            "box.style.alignItems='center';" +
                            "box.style.justifyContent='center';" +
                            "box.style.animation='none';" +
                            "box.style.transform='none';" +
                            "svg.style.display='none';" +
                            "if(!box.querySelector('img.wta-tutor-avatar-img')){" +
                                "var img=document.createElement('img');" +
                                "img.src='/animation.gif';" +
                                "img.alt='Wisdom Tower AI Tutor';" +
                                "img.className='wta-tutor-avatar-img';" +
                                "img.style.cssText='width:100%;height:100%;object-fit:contain;border-radius:12px;pointer-events:none;animation:none!important;transform:none!important;';" +
                                "box.appendChild(img);" +
                            "}" +
                        "}" +
                    "}" +
                "}" +
                "var spans=document.querySelectorAll('span,div');" +
                "for(var j=0;j<spans.length;j++){" +
                    "var spNode=spans[j];" +
                    "if((spNode.textContent||'').trim()==='Thinking'&&spNode.parentElement){" +
                        "var row=spNode.closest('.flex');" +
                        "if(row){" +
                            "var spinners=row.querySelectorAll('.animate-spin,div.w-4.h-4');" +
                            "for(var k=0;k<spinners.length;k++){" +
                                "var sEl=spinners[k];" +
                                "var sParent=sEl.parentElement;" +
                                "if(sParent&&sParent.getAttribute('data-wta-loader')!=='done'){" +
                                    "sParent.setAttribute('data-wta-loader','done');" +
                                    "sEl.style.display='none';" +
                                    "var extra=sParent.querySelectorAll('.animate-pulse,.absolute');" +
                                    "for(var e=0;e<extra.length;e++){extra[e].style.display='none';}" +
                                    "var loader=document.createElement('div');" +
                                    "loader.className='wta-compact-brand-loader';" +
                                    "loader.style.cssText='position:relative;width:28px;height:28px;display:flex;align-items:center;justify-content:center;flex-shrink:0;';" +
                                    "loader.innerHTML=" +
                                        "'<div style=\"position:absolute;inset:0;border-radius:50%;border:2px solid transparent;border-top-color:#00E5FF;border-right-color:rgba(0,229,255,0.4);animation:wtaSpinFast 0.75s linear infinite;\"></div>' +" +
                                        "'<svg viewBox=\"0 0 32 32\" style=\"position:absolute;inset:2px;width:24px;height:24px;animation:wtaSpinGear 0.85s linear infinite;\">' +" +
                                            "'<line x1=\"16\" y1=\"2\" x2=\"16\" y2=\"5\" stroke=\"#00E5FF\" stroke-width=\"1.8\" stroke-linecap=\"round\"/>' +" +
                                            "'<line x1=\"16\" y1=\"27\" x2=\"16\" y2=\"30\" stroke=\"#00E5FF\" stroke-width=\"1.8\" stroke-linecap=\"round\"/>' +" +
                                            "'<line x1=\"2\" y1=\"16\" x2=\"5\" y2=\"16\" stroke=\"#38BDF8\" stroke-width=\"1.8\" stroke-linecap=\"round\"/>' +" +
                                            "'<line x1=\"27\" y1=\"16\" x2=\"30\" y2=\"16\" stroke=\"#38BDF8\" stroke-width=\"1.8\" stroke-linecap=\"round\"/>' +" +
                                            "'<line x1=\"6.1\" y1=\"6.1\" x2=\"8.2\" y2=\"8.2\" stroke=\"#00E5FF\" stroke-width=\"1.8\" stroke-linecap=\"round\"/>' +" +
                                            "'<line x1=\"23.8\" y1=\"23.8\" x2=\"25.9\" y2=\"25.9\" stroke=\"#00E5FF\" stroke-width=\"1.8\" stroke-linecap=\"round\"/>' +" +
                                            "'<line x1=\"6.1\" y1=\"25.9\" x2=\"8.2\" y2=\"23.8\" stroke=\"#38BDF8\" stroke-width=\"1.8\" stroke-linecap=\"round\"/>' +" +
                                            "'<line x1=\"23.8\" y1=\"8.2\" x2=\"25.9\" y2=\"6.1\" stroke=\"#38BDF8\" stroke-width=\"1.8\" stroke-linecap=\"round\"/>' +" +
                                        "'</svg>' +" +
                                        "'<div style=\"position:absolute;inset:4px;border-radius:50%;border:1.8px solid transparent;border-bottom-color:#818CF8;border-left-color:rgba(129,140,248,0.4);animation:wtaSpinReverse 0.95s linear infinite;\"></div>' +" +
                                        "'<img src=\"/animation.gif\" alt=\"\" style=\"width:14px;height:14px;object-fit:contain;border-radius:3px;position:relative;z-index:2;pointer-events:none;transform:none!important;animation:none!important;\" />';" +
                                    "sParent.appendChild(loader);" +
                                "}" +
                            "}" +
                        "}" +
                    "}" +
                "}" +
                "var inputEl=document.getElementById('wt-ai-tutor-input');" +
                "if(inputEl&&!inputEl.__wta_kb_bound){" +
                    "inputEl.__wta_kb_bound=true;" +
                    "function keepVisible(){" +
                        "requestAnimationFrame(function(){" +
                            "try{inputEl.scrollIntoView({block:'nearest',behavior:'smooth'});}catch(_){}" +
                        "});" +
                    "}" +
                    "inputEl.addEventListener('focus',function(){keepVisible();setTimeout(keepVisible,150);setTimeout(keepVisible,350);});" +
                    "inputEl.addEventListener('input',keepVisible);" +
                    "if(window.visualViewport){window.visualViewport.addEventListener('resize',keepVisible);}" +
                "}" +
                "var closeBtns=document.querySelectorAll('button[aria-label*=\"close\" i],button:has(svg.lucide-x)');" +
                "for(var c=0;c<closeBtns.length;c++){" +
                    "var cb=closeBtns[c];" +
                    "if(!cb.__wta_close_bound){" +
                        "cb.__wta_close_bound=true;" +
                        "cb.addEventListener('click',function(e){" +
                            "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.returnToStudyPage==='function'){" +
                                "e.preventDefault();e.stopPropagation();" +
                                "window.AndroidOfflineVault.returnToStudyPage();" +
                            "}" +
                        "},true);" +
                    "}" +
                "}" +
            "}catch(_){}" +
        "}" +
        "syncAiTutorChrome();" +
        "if(!window.__wta_tutor_mo&&window.MutationObserver){" +
            "window.__wta_tutor_mo=true;" +
            "var tmo=new MutationObserver(syncAiTutorChrome);" +
            "tmo.observe(document.documentElement||document.body,{childList:true,subtree:true});" +
        "}" +
        "if(!window.__wta_tutor_timer){" +
            "window.__wta_tutor_timer=setInterval(syncAiTutorChrome,400);" +
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
        "function repairImage(img){" +
            "try{" +
                "if(!img||img.__wta_repaired)return;" +
                "var src=img.getAttribute('src')||'';" +
                "if(!src||src.indexOf('data:')===0||src.indexOf('blob:')===0)return;" +
                "img.__wta_repaired=true;" +
                "img.classList.add('wta-img-broken');" +
                "img.style.setProperty('visibility','hidden','important');" +
                "img.style.setProperty('opacity','0','important');" +
                "if(img.parentElement){img.parentElement.classList.add('wta-img-fallback');}" +
            "}catch(_){}" +
        "}" +
        "if(!window.__wta_err_bound){" +
            "window.__wta_err_bound=true;" +
            "window.addEventListener('error',function(e){" +
                "var t=e.target;" +
                "if(t&&t.tagName==='IMG'){" +
                    "repairImage(t);" +
                "}" +
            "},true);" +
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

    private val pendingNotificationUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        extractNotificationUrl(intent)
        val splash = installSplashScreen()
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        // TODO: Re-enable FLAG_SECURE before final production release to prevent unauthorized screen captures
        // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val navy = AndroidColor.parseColor("#060B15")
        @Suppress("DEPRECATION")
        window.statusBarColor = navy
        @Suppress("DEPRECATION")
        window.navigationBarColor = AndroidColor.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = AndroidColor.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false
        setContent {
            MyApplicationTheme {
                MainScreen(
                    onReady = { keepSplash = false },
                    initialNotificationUrl = pendingNotificationUrl.value,
                    onNotificationUrlConsumed = { pendingNotificationUrl.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationUrl(intent)
    }

    private fun extractNotificationUrl(intent: Intent?) {
        val target = intent?.getStringExtra(AcademyFirebaseMessagingService.EXTRA_TARGET_URL)
            ?: intent?.getStringExtra("url")
            ?: intent?.getStringExtra("link")
            ?: intent?.getStringExtra("path")
            ?: intent?.dataString
        if (!target.isNullOrBlank()) {
            pendingNotificationUrl.value = FcmTokenRegistrar.resolveTargetUrl(target)
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
    val isDestructive: Boolean = false,
)

private val overflowMenuLinks = listOf(
    MenuLink("About Academy", "https://www.wisdom-tower-academy.live/about", Icons.Filled.Info),
    MenuLink("Contact & Support", "https://www.wisdom-tower-academy.live/contact", Icons.Outlined.Email),
    MenuLink("FAQ", "https://www.wisdom-tower-academy.live/academy/faq", Icons.AutoMirrored.Outlined.HelpOutline),
    MenuLink("Privacy Policy", "https://www.wisdom-tower-academy.live/privacy", Icons.Outlined.PrivacyTip),
    MenuLink("Terms of Service", "https://www.wisdom-tower-academy.live/terms", Icons.Outlined.Policy)
)

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        @Suppress("DEPRECATION")
        val info = cm.activeNetworkInfo
        return info != null && info.isConnected
    } catch (_: Exception) {}
    return false
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
fun MainScreen(
    onReady: () -> Unit = {},
    initialNotificationUrl: String? = null,
    onNotificationUrlConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Learning,
        BottomNavItem.Packages,
        BottomNavItem.Account,
        BottomNavItem.Settings
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            FcmTokenRegistrar.checkAndRegisterToken(context)
        }
    }

    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Instant cold-start and process-death network validation
    val isInitiallyOnline = isOnline(context)
    var lastTargetUrl by remember { mutableStateOf(initialNotificationUrl?.takeIf { it.isNotBlank() } ?: SITE) }

    // Loading states for zero blank screen and lively navigation feedback
    var isInitialLoading by remember { mutableStateOf(true) }
    var minSplashElapsed by remember { mutableStateOf(false) }
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
        if (delayMs > 0L) {
            val showRunnable = Runnable { isNavigating = true }
            navShowRunnable = showRunnable
            mainHandler.postDelayed(showRunnable, delayMs)
        } else {
            isNavigating = true
        }
        val timeout = Runnable { isNavigating = false }
        navTimeoutRunnable = timeout
        mainHandler.postDelayed(timeout, 4000L)
    }

    fun stopNavigationLoading(forceImmediate: Boolean = false) {
        navShowRunnable?.let { mainHandler.removeCallbacks(it) }
        navShowRunnable = null
        navTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        navTimeoutRunnable = null
        if (forceImmediate) {
            isNavigating = false
        } else {
            val elapsed = System.currentTimeMillis() - navigationStartTime
            val remainingMin = (260L - elapsed).coerceAtLeast(80L)
            mainHandler.postDelayed({
                isNavigating = false
            }, remainingMin)
        }
    }

    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showTimerControlDialog by remember { mutableStateOf(false) }
    var studyPageReturnUrl by remember { mutableStateOf("") }
    var previousTimerRunning by remember { mutableStateOf(false) }
    var lastOnlineUrl by remember { mutableStateOf(SITE) }
    var pendingClearHistory by remember { mutableStateOf(false) }

    var pendingNavRunnable by remember { mutableStateOf<Runnable?>(null) }

    fun showOffline(wv: WebView, force: Boolean = false) {
        if (!force && isOnline(context)) {
            // Absolute safety check: Never show offline page during normal online browsing
            return
        }
        val cur = wv.url.orEmpty()
        // If already showing offline asset, DO NOT reload it in an infinite loop!
        if (cur.contains("offline.html") || cur.startsWith("file:///android_asset/")) {
            if (minSplashElapsed) {
                isInitialLoading = false
            }
            stopNavigationLoading()
            return
        }

        if (cur.isNotBlank() && !cur.startsWith("file://") && !cur.contains("offline.html")) {
            lastOnlineUrl = cur
        }
        mainHandler.post {
            if (!force && isOnline(context)) return@post
            try {
                wv.stopLoading()
            } catch (_: Exception) {}
            wv.loadUrl(OFFLINE_ASSET)
        }
        mainHandler.postDelayed({
            if (minSplashElapsed) {
                isInitialLoading = false
            }
            stopNavigationLoading()
        }, 550L)
    }

    fun navigateTo(url: String, tabIndex: Int? = null, resetHistory: Boolean = false) {
        pendingNavRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingNavRunnable = null

        val targetUrl = url.trim()
        val wv = webView

        if (tabIndex != null) {
            selectedIndex = tabIndex
        } else {
            selectedIndex = tabIndexForUrl(targetUrl, selectedIndex)
        }
        if (resetHistory) pendingClearHistory = true

        if (!targetUrl.startsWith("file://") && !targetUrl.contains("offline.html")) {
            lastTargetUrl = targetUrl
        }

        if (!targetUrl.contains("tool=")) {
            studyPageReturnUrl = ""
        }

        val targetTitle = when {
            tabIndex != null && tabIndex in items.indices -> items[tabIndex].title
            else -> {
                val idx = tabIndexForUrl(targetUrl, selectedIndex)
                if (idx in items.indices) items[idx].title else "Wisdom Tower Academy"
            }
        }
        navigationStatusText = "Opening $targetTitle…"

        if (wv == null) {
            val r = Runnable { stopNavigationLoading(forceImmediate = true) }
            pendingNavRunnable = r
            mainHandler.postDelayed(r, 600L)
            return
        }

        val online = isOnline(context)
        startNavigationLoading(delayMs = 0L)
        wv.evaluateJavascript(EARLY_HIDE_CHROME_JS, null)

        // For tool URLs: remember previous study return URL if not already on a tool
        if (targetUrl.contains("tool=")) {
            val currentUrl = wv.url.orEmpty().trim()
            if (!currentUrl.contains("tool=") && currentUrl.isNotBlank() && !currentUrl.startsWith("file://") && !currentUrl.contains("offline.html")) {
                studyPageReturnUrl = currentUrl
            }
        }

        if (online) {
            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
            wv.loadUrl(targetUrl)
        } else {
            if (WebCacheVault.has(context, targetUrl)) {
                wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                wv.loadUrl(targetUrl)
            } else {
                showOffline(wv, force = true)
            }
        }
    }

    // MP3-player style Study Timer state
    var isTimerRunning by remember { mutableStateOf(false) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var timerRemainingSeconds by remember { mutableIntStateOf(0) }
    var timerTotalSeconds by remember { mutableIntStateOf(0) }
    var timerTitle by remember { mutableStateOf("Study Timer") }
    var isTimerDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning && !previousTimerRunning) {
            Toast.makeText(context, "Focus session started — we'll notify you when it wraps up.", Toast.LENGTH_SHORT).show()
        }
        previousTimerRunning = isTimerRunning
    }

    LaunchedEffect(isTimerRunning, timerRemainingSeconds) {
        if (isTimerRunning && timerRemainingSeconds > 0) {
            delay(1000L)
            timerRemainingSeconds = (timerRemainingSeconds - 1).coerceAtLeast(0)
            if (timerRemainingSeconds == 0) {
                isTimerRunning = false
                isTimerPaused = false
                AcademyNotificationManager.notifyStudyTimerCompleted(context)
                Toast.makeText(context, AcademyNotificationManager.TIMER_REWARDING_MESSAGES.random(), Toast.LENGTH_LONG).show()
            }
        }
    }

    var lastResumeRefreshAt by remember { mutableLongStateOf(0L) }
    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding(context)) }
    var showExitDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    val stopStudyTimerWithEndFlow: () -> Unit = {
        val wasActive = isTimerRunning || isTimerPaused || timerRemainingSeconds > 0
        isTimerRunning = false
        isTimerPaused = false
        timerRemainingSeconds = 0
        isTimerDismissed = true
        showTimerControlDialog = false

        val js = "(function(){try{" +
            "window.dispatchEvent(new CustomEvent('wta-study-timer-control',{detail:{action:'stop'}}));" +
            "var raw=localStorage.getItem('wt_focus_timer_v1');" +
            "if(raw){var s=JSON.parse(raw);s.running=false;s.endAt=null;s.leftWhenPaused=s.totalSec||1500;localStorage.setItem('wt_focus_timer_v1',JSON.stringify(s));window.dispatchEvent(new CustomEvent('wt-focus-timer'));}" +
            "}catch(e){}})();"
        webView?.evaluateJavascript(js, null)

        if (wasActive) {
            val rewardingMsg = AcademyNotificationManager.TIMER_REWARDING_MESSAGES.random()
            Toast.makeText(context, rewardingMsg, Toast.LENGTH_LONG).show()
            AcademyNotificationManager.notifyStudyTimerCompleted(context)
        }
    }

    LaunchedEffect(initialNotificationUrl) {
        if (!initialNotificationUrl.isNullOrBlank()) {
            navigateTo(initialNotificationUrl, null)
            onNotificationUrlConsumed()
        }
    }

    // Immediate cold-start / process-death check for offline
    LaunchedEffect(Unit) {
        FcmTokenRegistrar.checkAndRegisterToken(context)
        onReady()
        delay(MIN_SPLASH_DISPLAY_MS)
        minSplashElapsed = true
        if (!isOnline(context)) {
            if (!WebCacheVault.hasAnyPage(context)) {
                webView?.let { showOffline(it, force = true) }
            }
            delay(200L)
            isInitialLoading = false
        } else {
            val startWait = System.currentTimeMillis()
            while (!pageRendered && (System.currentTimeMillis() - startWait) < 14_000L) {
                delay(120L)
            }
            isInitialLoading = false
        }
    }

    // Active network monitoring for instant auto-recovery and offline enforcement
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                mainHandler.postDelayed({
                    webView?.let { wv ->
                        if (isOnline(context)) {
                            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
                            val curUrl = wv.url.orEmpty()
                            if (curUrl.contains("offline.html") || curUrl.startsWith("file://")) {
                                val target = lastTargetUrl.ifBlank { SITE }
                                navigationStatusText = "Connecting…"
                                startNavigationLoading()
                                wv.loadUrl(target)
                            }
                        }
                    }
                }, 300L)
            }
            override fun onLost(network: android.net.Network) {
                mainHandler.post {
                    webView?.settings?.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
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
                    wv.settings.cacheMode = if (online) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                    if (online) {
                        val current = wv.url.orEmpty()
                        if (current.contains("offline.html") || current.startsWith("file://")) {
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
        if (showTimerControlDialog) {
            showTimerControlDialog = false
            return@BackHandler
        }
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
        if (wv == null || currentUrl.isBlank() || currentUrl.startsWith("file://") || currentUrl.contains("offline.html")) {
            if (selectedIndex != 0) {
                navigateTo(StructuralNav.SITE_HOME, tabIndex = 0)
                return@BackHandler
            }
            triggerDoubleTapExit()
            return@BackHandler
        }

        // Structural back JS bridge. If "ok" -> handled by site; else app structural back
        wv.evaluateJavascript(StructuralNav.STRUCTURAL_BACK_JS) { rawResult ->
            val res = rawResult?.trim('"')?.trim() ?: ""
            if (res == "ok") {
                return@evaluateJavascript
            }
            mainHandler.post {
                val activeUrl = wv.url ?: currentUrl
                val activeTab = tabIndexForUrl(activeUrl, selectedIndex)
                val isToolUrl = activeUrl.contains("tool=")

                // 1) Prefer normal WebView history so device Back returns to the previous page
                // (e.g. question bank -> tool -> Back -> question bank). Do not clear the whole stack to Home.
                val list = wv.copyBackForwardList()
                val currIdx = list.currentIndex
                if (currIdx > 0 && wv.canGoBack()) {
                    val prevItem = list.getItemAtIndex(currIdx - 1)
                    val prevUrl = prevItem.url.orEmpty()
                    if (prevUrl.isNotBlank() && !prevUrl.startsWith("file://") && !prevUrl.contains("offline.html")) {
                        wv.goBack()
                        return@post
                    }
                }

                // 2) If on a tool URL without web history, restore the remembered study return URL
                if (isToolUrl && studyPageReturnUrl.isNotBlank() && studyPageReturnUrl != activeUrl) {
                    val returnUrl = studyPageReturnUrl
                    studyPageReturnUrl = ""
                    navigateTo(returnUrl, tabIndex = tabIndexForUrl(returnUrl, 1))
                    return@post
                }

                // 3) If no previous history step, use structural parent hierarchy
                val parentUrl = StructuralNav.getParentStructuralUrl(activeUrl)
                if (parentUrl != null) {
                    navigateTo(parentUrl, tabIndex = activeTab)
                } else {
                    if (isToolUrl) {
                        // Never clear stack to Home from a tool page; fall back to learning hub
                        navigateTo(StructuralNav.SITE_LEARNING, tabIndex = 1)
                    } else if (activeTab != 0 || selectedIndex != 0) {
                        navigateTo(StructuralNav.SITE_HOME, tabIndex = 0)
                    } else {
                        triggerDoubleTapExit()
                    }
                }
            }
        }
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

    if (showOnboarding) {
        OnboardingScreen(
            onFinished = {
                setOnboardingCompleted(context)
                showOnboarding = false
                requestNotificationPermission()
            }
        )
        return
    }

    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

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
                                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
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

                        // Right: Small top-right timer indicator + Refresh + Notification Actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Small top-right indicator while study timer is running (zero floating countdown box)
                            val isTimerActive = (isTimerRunning || (isTimerPaused && timerRemainingSeconds > 0)) && !isTimerDismissed
                            AnimatedVisibility(
                                visible = isTimerActive,
                                enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.85f),
                                exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.85f)
                            ) {
                                val mins = timerRemainingSeconds / 60
                                val secs = timerRemainingSeconds % 60
                                val formatted = String.format("%02d:%02d", mins, secs)

                                Surface(
                                    color = if (isTimerRunning) Color(0x2422E0FF) else Color(0x24F59E0B),
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, if (isTimerRunning) Accent.copy(alpha = 0.6f) else Color(0x66F59E0B)),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            showTimerControlDialog = true
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Timer,
                                            contentDescription = "Study Timer",
                                            tint = if (isTimerRunning) Accent else Color(0xFFF59E0B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = formatted,
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    val wv = webView
                                    val currentUrl = wv?.url.orEmpty()
                                    if (wv != null) {
                                        navigationStatusText = "Refreshing…"
                                        startNavigationLoading()
                                        webProgress = 15
                                        val online = isOnline(context)
                                        val isOfflineAsset = currentUrl.contains("offline.html") || currentUrl.startsWith("file://")
                                        val target = if (isOfflineAsset) lastTargetUrl.ifBlank { SITE } else currentUrl.ifBlank { lastTargetUrl.ifBlank { SITE } }

                                        if (online) {
                                            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
                                            if (isOfflineAsset) {
                                                wv.loadUrl(target)
                                            } else {
                                                wv.evaluateJavascript(StructuralNav.HARD_REFRESH_JS) {
                                                    Handler(Looper.getMainLooper()).post {
                                                        wv.reload()
                                                    }
                                                }
                                            }
                                        } else {
                                            if (WebCacheVault.has(context, target)) {
                                                wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                                wv.loadUrl(target)
                                            } else {
                                                showOffline(wv, force = true)
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
                                    requestNotificationPermission()
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardBorderSubtle)
                    )
                }
            },
            bottomBar = {
                if (!isKeyboardVisible) {
                    AliveBottomNav(
                        items = items,
                        selectedIndex = selectedIndex,
                        onItemSelected = { index, item ->
                            selectedIndex = index
                            navigateTo(item.url, index)
                        }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                // Main Web View
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
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
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                try {
                                    val swController = ServiceWorkerController.getInstance()
                                    val swSettings = swController.serviceWorkerWebSettings
                                    swSettings.cacheMode = WebSettings.LOAD_DEFAULT
                                    swSettings.allowContentAccess = true
                                    swSettings.allowFileAccess = true
                                    swSettings.blockNetworkLoads = false
                                } catch (_: Exception) {}
                            }
                            val prefs = ctx.getSharedPreferences("wta_web_cache_prefs", Context.MODE_PRIVATE)
                            if (!prefs.getBoolean("wta_vault_v3_sanitized", false)) {
                                try {
                                    clearCache(false)
                                    prefs.edit().putBoolean("wta_vault_v3_sanitized", true).apply()
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
                                            mainHandler.postDelayed({
                                                stopNavigationLoading()
                                            }, 100L)
                                        }
                                        val lower = path.lowercase()
                                        if (lower.contains("login") || lower.contains("account") || lower.contains("dashboard") || lower.contains("learning")) {
                                            FcmTokenRegistrar.checkAndRegisterToken(ctx)
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun onUserLogin(userId: String? = null) {
                                    FcmTokenRegistrar.checkAndRegisterToken(ctx)
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
                                        navigationStatusText = "Connecting…"
                                        startNavigationLoading(0L)
                                        webProgress = 25
                                        val wv = webView ?: return@post
                                        val online = isOnline(context)
                                        val target = lastTargetUrl.ifBlank { SITE }
                                        if (online) {
                                            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
                                            try {
                                                wv.stopLoading()
                                            } catch (_: Exception) {}
                                            wv.loadUrl(target)
                                        } else {
                                            if (WebCacheVault.has(context, target)) {
                                                wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                                try {
                                                    wv.stopLoading()
                                                } catch (_: Exception) {}
                                                wv.loadUrl(target)
                                            } else {
                                                mainHandler.postDelayed({
                                                    stopNavigationLoading()
                                                    wv.evaluateJavascript("if(typeof onRetryFailed==='function')onRetryFailed();", null)
                                                }, 800L)
                                            }
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun openOfflineUrl(url: String?) {
                                    if (url.isNullOrBlank()) return
                                    mainHandler.post {
                                        webView?.let { wv ->
                                            lastTargetUrl = url
                                            val online = isOnline(context)
                                            wv.settings.cacheMode = if (online) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                                            try {
                                                wv.stopLoading()
                                            } catch (_: Exception) {}
                                            wv.loadUrl(url)
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun showOfflinePage() {
                                    mainHandler.post {
                                        val wv = webView ?: return@post
                                        val cur = wv.url.orEmpty()
                                        if (cur.contains("offline.html") || cur.startsWith("file://")) return@post
                                        if (isOnline(context)) return@post
                                        showOffline(wv, force = false)
                                    }
                                }

                                @JavascriptInterface
                                fun syncStudyTimer(isRunning: Boolean, secondsLeft: Int, totalSeconds: Int, title: String?) {
                                    mainHandler.post {
                                        val wasRunning = isTimerRunning
                                        isTimerRunning = isRunning
                                        if (isRunning) {
                                            if (!wasRunning) {
                                                Toast.makeText(context, "Focus session started — we'll notify you when it wraps up.", Toast.LENGTH_SHORT).show()
                                            }
                                            isTimerPaused = false
                                            timerRemainingSeconds = secondsLeft.coerceAtLeast(0)
                                            isTimerDismissed = false
                                        } else {
                                            if (wasRunning && secondsLeft == 0 && totalSeconds > 0) {
                                                AcademyNotificationManager.notifyStudyTimerCompleted(ctx)
                                            }
                                            if (secondsLeft > 0 && !isTimerDismissed) {
                                                isTimerPaused = true
                                                timerRemainingSeconds = secondsLeft
                                            } else {
                                                isTimerPaused = false
                                                timerRemainingSeconds = 0
                                            }
                                        }
                                        timerTotalSeconds = totalSeconds.coerceAtLeast(0)
                                        if (!title.isNullOrBlank()) {
                                            timerTitle = title
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun notifyPlannerTask(taskName: String?, dueTime: String?, studentName: String?) {
                                    if (!taskName.isNullOrBlank()) {
                                        mainHandler.post {
                                            AcademyNotificationManager.notifyPlannerTaskDue(ctx, taskName, dueTime, studentName)
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun notifyDailyStudyGoal() {
                                    mainHandler.post {
                                        AcademyNotificationManager.notifyDailyGoalNudge(ctx)
                                    }
                                }

                                @JavascriptInterface
                                fun openNotificationSettings() {
                                    mainHandler.post {
                                        showNotificationSettingsDialog = true
                                    }
                                }

                                @JavascriptInterface
                                fun isStudyTimerActive(): Boolean {
                                    return (isTimerRunning || (isTimerPaused && timerRemainingSeconds > 0)) && !isTimerDismissed
                                }

                                @JavascriptInterface
                                fun returnToStudyPage() {
                                    mainHandler.post {
                                        if (studyPageReturnUrl.isNotBlank()) {
                                            val returnUrl = studyPageReturnUrl
                                            studyPageReturnUrl = ""
                                            navigateTo(returnUrl, tabIndex = tabIndexForUrl(returnUrl, 1))
                                        } else {
                                            navigateTo(StructuralNav.SITE_LEARNING, tabIndex = 1)
                                        }
                                    }
                                }
                            }, "AndroidOfflineVault")

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    webProgress = newProgress
                                    if (newProgress >= 85) {
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
                                    if (url != null && (url.startsWith("chrome-error://") || url.startsWith("about:neterror") || url.contains("chromewebdata"))) {
                                        try {
                                            wv.stopLoading()
                                        } catch (_: Exception) {}
                                        showOffline(wv)
                                        return
                                    }
                                    wv.settings.cacheMode = if (isOnline(ctx)) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                                    if (url != null && !url.startsWith("file://")) {
                                        // Early hide-chrome CSS injection at onPageStarted (before first paint)
                                        wv.evaluateJavascript(EARLY_HIDE_CHROME_JS, null)

                                        val newIdx = tabIndexForUrl(url, selectedIndex)
                                        val targetIdx = tabIndexForUrl(lastTargetUrl, selectedIndex)
                                        if (!isNavigating || newIdx == targetIdx) {
                                            selectedIndex = newIdx
                                        }
                                        if (!isInitialLoading) {
                                            val title = if (newIdx in items.indices) items[newIdx].title else "Wisdom Tower Academy"
                                            navigationStatusText = "Opening $title…"
                                            startNavigationLoading()
                                        }
                                    }
                                    if (url != null && url.startsWith("file:///android_asset/")) {
                                        if (minSplashElapsed) {
                                            isInitialLoading = false
                                        }
                                    }
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    val u = url.orEmpty()
                                    if (!u.startsWith("file://") && (u.contains("wisdom-tower-academy.live/offline") || u.endsWith("/offline"))) {
                                        view?.let { showOffline(it, force = true) }
                                        return
                                    }
                                    if (u.isNotBlank() && !u.startsWith("file://")) {
                                        val newIdx = tabIndexForUrl(u, selectedIndex)
                                        val targetIdx = tabIndexForUrl(lastTargetUrl, selectedIndex)
                                        if (!isNavigating || newIdx == targetIdx) {
                                            selectedIndex = newIdx
                                        }
                                    }
                                    pageRendered = true
                                    if (minSplashElapsed) {
                                        isInitialLoading = false
                                    }
                                    stopNavigationLoading()
                                    if (!u.startsWith("file://")) {
                                        view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                        view?.evaluateJavascript(AI_TUTOR_CHROME_JS, null)
                                        view?.evaluateJavascript(PRECACHE_AND_UNBLOCK_JS, null)
                                        view?.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                        view?.evaluateJavascript(STUDY_TIMER_BRIDGE_JS, null)
                                        view?.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    val wv = view ?: return
                                    val curUrl = url ?: wv.url
                                    val u = curUrl.orEmpty()
                                    if (!u.startsWith("file://") && (u.contains("wisdom-tower-academy.live/offline") || u.endsWith("/offline"))) {
                                        showOffline(wv, force = true)
                                        return
                                    }
                                    if (u.isNotBlank() && !u.startsWith("file://") && !u.contains("offline.html")) {
                                        val newIdx = tabIndexForUrl(u, selectedIndex)
                                        val targetIdx = tabIndexForUrl(lastTargetUrl, selectedIndex)
                                        if (!isNavigating || newIdx == targetIdx) {
                                            selectedIndex = newIdx
                                        }
                                        lastOnlineUrl = u
                                        lastTargetUrl = u

                                        // Persist rendered HTML into WebCacheVault for 100% offline availability
                                        if (isOnline(ctx)) {
                                            wv.evaluateJavascript("(function(){return document.documentElement ? document.documentElement.outerHTML : '';})();") { htmlJson ->
                                                if (!htmlJson.isNullOrBlank() && htmlJson != "\"\"" && htmlJson != "null") {
                                                    try {
                                                        val rawHtml = org.json.JSONTokener(htmlJson).nextValue() as? String ?: ""
                                                        val saveUrl = curUrl ?: u
                                                        if (rawHtml.length > 500 &&
                                                            saveUrl.isNotBlank() &&
                                                            !WebCacheVault.isDynamicListOrDataApi(saveUrl) &&
                                                            !WebCacheVault.isDegradedOrEmptyHtml(rawHtml) &&
                                                            (rawHtml.contains("<html") || rawHtml.contains("<body") || rawHtml.contains("<!DOCTYPE"))
                                                        ) {
                                                            WebCacheVault.saveHtmlPage(ctx, saveUrl, rawHtml)
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        }
                                    }
                                    pageRendered = true
                                    if (minSplashElapsed) {
                                        isInitialLoading = false
                                    }
                                    stopNavigationLoading()
                                    if (!u.startsWith("file://")) {
                                        wv.evaluateJavascript(NATIVE_CHROME_JS, null)
                                        wv.evaluateJavascript(AI_TUTOR_CHROME_JS, null)
                                        wv.evaluateJavascript(PRECACHE_AND_UNBLOCK_JS, null)
                                        wv.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                        wv.evaluateJavascript(STUDY_TIMER_BRIDGE_JS, null)
                                        wv.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                        mainHandler.postDelayed({
                                            val cur = wv.url ?: ""
                                            if (!cur.startsWith("file://")) {
                                                wv.evaluateJavascript(AI_TUTOR_CHROME_JS, null)
                                                wv.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                                wv.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
                                                wv.evaluateJavascript(STUDY_TIMER_BRIDGE_JS, null)
                                            }
                                        }, 800L)
                                    }
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

                                    if (statusCode in listOf(404, 500, 502, 503, 504)) {
                                        if (!isOnline(context) && !WebCacheVault.has(context, failedUrl)) {
                                            try {
                                                wv.stopLoading()
                                            } catch (_: Exception) {}
                                            showOffline(wv, force = true)
                                        }
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
                                    if (!isOnline(context) && !WebCacheVault.has(context, failedUrl)) {
                                        try {
                                            wv.stopLoading()
                                        } catch (_: Exception) {}
                                        showOffline(wv, force = true)
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
                                    val failedUrl = failingUrl.orEmpty()
                                    if (failedUrl.startsWith("file:///android_asset/")) return
                                    if (!isOnline(context) && !WebCacheVault.has(context, failedUrl)) {
                                        try {
                                            wv.stopLoading()
                                        } catch (_: Exception) {}
                                        showOffline(wv, force = true)
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.cancel()
                                    val wv = view ?: return
                                    if (!isOnline(context)) {
                                        showOffline(wv, force = true)
                                    }
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

                                    val cleanLower = u.lowercase()

                                    // Intercept any attempt to load /offline or site's offline fallback
                                    if (cleanLower.endsWith("/offline") || cleanLower.contains("/offline")) {
                                        try {
                                            val stream = ctx.assets.open("offline.html")
                                            val headers = mapOf(
                                                "Content-Type" to "text/html; charset=utf-8",
                                                "Cache-Control" to "no-cache, no-store, must-revalidate"
                                            )
                                            return WebResourceResponse("text/html", "utf-8", 200, "OK", headers, stream)
                                        } catch (_: Exception) {}
                                    }

                                    // Local brand asset interception (instant 0ms, works 100% offline and online)
                                    if (cleanLower.endsWith("/animation.gif") || cleanLower.contains("animation.gif") || cleanLower.contains("/brand/animation.gif")) {
                                        try {
                                            val stream = ctx.assets.open("brand/animation.gif")
                                            val headers = mapOf(
                                                "Content-Type" to "image/gif",
                                                "Cache-Control" to "public, max-age=31536000, immutable",
                                                "Access-Control-Allow-Origin" to "*"
                                            )
                                            return WebResourceResponse("image/gif", null, 200, "OK", headers, stream)
                                        } catch (_: Exception) {}
                                    }

                                    if (cleanLower.endsWith("/logo.png") || cleanLower.contains("logo.png") || cleanLower.contains("/brand/logo.png")) {
                                        try {
                                            val stream = ctx.assets.open("brand/logo.png")
                                            val headers = mapOf(
                                                "Content-Type" to "image/png",
                                                "Cache-Control" to "public, max-age=31536000, immutable",
                                                "Access-Control-Allow-Origin" to "*"
                                            )
                                            return WebResourceResponse("image/png", null, 200, "OK", headers, stream)
                                        } catch (_: Exception) {}
                                    }

                                    // 2. WebCacheVault handling
                                    val online = isOnline(ctx)

                                    if (!online) {
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

                                        // Return from WebCacheVault if cached offline
                                        if (WebCacheVault.has(ctx, u)) {
                                            val cached = WebCacheVault.getCachedResponse(ctx, u)
                                            if (cached != null) return cached
                                        }

                                        // Fallback for non-cached images offline: return transparent 1x1 PNG to avoid broken document icons
                                        if (cleanLower.endsWith(".png") || cleanLower.endsWith(".jpg") ||
                                            cleanLower.endsWith(".jpeg") || cleanLower.endsWith(".webp") ||
                                            cleanLower.endsWith(".svg") || cleanLower.endsWith(".gif") ||
                                            cleanLower.contains("/_next/image")
                                        ) {
                                            return WebResourceResponse("image/png", null, 200, "OK", emptyMap(), ByteArrayInputStream(WebCacheVault.EMPTY_PNG))
                                        }

                                        // If main frame requested while offline and NOT in WebCacheVault:
                                        // Return offline.html asset stream directly so Chromium never shows chrome error
                                        if (req.isForMainFrame) {
                                            try {
                                                val stream = ctx.assets.open("offline.html")
                                                val headers = mapOf(
                                                    "Content-Type" to "text/html; charset=utf-8",
                                                    "Cache-Control" to "no-cache, no-store, must-revalidate"
                                                )
                                                return WebResourceResponse("text/html", "utf-8", 200, "OK", headers, stream)
                                            } catch (_: Exception) {}
                                        }

                                        return null
                                    }

                                    // Online: serve cached subresources if available for maximum speed (never for dynamic list/data APIs)
                                    if (isGet && !req.isForMainFrame && !WebCacheVault.isDynamicListOrDataApi(u) && WebCacheVault.has(ctx, u)) {
                                        val cached = WebCacheVault.getCachedResponse(ctx, u)
                                        if (cached != null) return cached
                                    }

                                    // Online: precache assets in background
                                    if (isGet && WebCacheVault.isCacheable(u) && !WebCacheVault.has(ctx, u)) {
                                        val isImg = cleanLower.endsWith(".png") || cleanLower.endsWith(".jpg") ||
                                            cleanLower.endsWith(".jpeg") || cleanLower.endsWith(".webp") ||
                                            cleanLower.endsWith(".svg") || cleanLower.endsWith(".gif") ||
                                            cleanLower.contains("/_next/image")
                                        if (isImg) {
                                            try {
                                                val fetched = WebCacheVault.fetchAndCache(ctx, req)
                                                if (fetched != null) return fetched
                                            } catch (_: Exception) {}
                                        } else {
                                            WebCacheVault.cacheUrlAsync(ctx, u)
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
                                    if (u.startsWith("chrome-error://") || u.startsWith("about:neterror") || u.contains("chromewebdata") || u.contains("/offline")) {
                                        showOffline(wv, force = true)
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

                            val target = lastTargetUrl.ifBlank { SITE }
                            if (isOnline(ctx)) {
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                loadUrl(target)
                            } else {
                                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                if (WebCacheVault.has(ctx, target) || WebCacheVault.hasAnyPage(ctx)) {
                                    loadUrl(target)
                                } else {
                                    showOffline(this, force = true)
                                }
                            }
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Restored & Fixed Custom Animated Loader Overlay:
                // - Transparent background only (solid BarBg only on initial cold splash)
                // - NO text at all (no titles, no captions)
                // - NO percent (no 20%, 80%, etc.)
                // - NO card, NO panel, NO dim box
                // - ONE continuous stable animation (same 110.dp scale, same center, zero size jumps)
                val isLoaderVisible = isInitialLoading || isNavigating
                AnimatedVisibility(
                    visible = isLoaderVisible,
                    enter = fadeIn(tween(140, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(200, easing = FastOutSlowInEasing)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(95f)
                ) {
                    val loaderBg = BarBg
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(loaderBg),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomCenteredLoader()
                    }
                }
            }
        }

        // Native Slide-In Side Menu (solid, anchored to left screen edge, brand only at top)
        AnimatedVisibility(
            visible = menuExpanded,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.zIndex(150f)
        ) {
            BackHandler(enabled = menuExpanded) { menuExpanded = false }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuExpanded = false }
            ) {
                // Docked directly to the left screen border
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(270.dp)
                        .background(Color(0xFF070D18))
                        .border(
                            BorderStroke(1.dp, CardBorder),
                            RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        )
                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* prevent close when clicking drawer content */ }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp)
                    ) {
                        // Top of menu: Brand ONLY (No subtitles, no extra tags)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
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
                                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                ) {
                                    BrandLogo(size = 30.dp)
                                }
                                Text(
                                    text = "Wisdom Tower",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
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
                                    contentDescription = "Close menu",
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
                                .background(CardBorderSubtle)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Compact, tight item rows with less wasted width & height
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Section: TOOLS (Navigates WebView to website Learning tools)
                            Text(
                                text = "TOOLS",
                                color = Accent.copy(alpha = 0.8f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.AutoAwesome,
                                label = "AI Tutor",
                                onClick = {
                                    menuExpanded = false
                                    navigateTo("https://www.wisdom-tower-academy.live/learning?tool=tutor", tabIndex = 1)
                                }
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.Calculate,
                                label = "Calculator",
                                onClick = {
                                    menuExpanded = false
                                    navigateTo("https://www.wisdom-tower-academy.live/learning?tool=calculator", tabIndex = 1)
                                }
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.EditNote,
                                label = "Notebook",
                                onClick = {
                                    menuExpanded = false
                                    navigateTo("https://www.wisdom-tower-academy.live/learning?tool=notes", tabIndex = 1)
                                }
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.Timer,
                                label = "Timer",
                                onClick = {
                                    menuExpanded = false
                                    navigateTo("https://www.wisdom-tower-academy.live/learning?tool=timer", tabIndex = 1)
                                }
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.CalendarMonth,
                                label = "Planner",
                                onClick = {
                                    menuExpanded = false
                                    navigateTo("https://www.wisdom-tower-academy.live/learning?tool=planner", tabIndex = 1)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(CardBorderSubtle)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "NOTIFICATIONS & PREFERENCES",
                                color = Accent.copy(alpha = 0.8f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.Notifications,
                                label = "Notification Settings",
                                onClick = {
                                    menuExpanded = false
                                    showNotificationSettingsDialog = true
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(CardBorderSubtle)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "SUPPORT & ABOUT",
                                color = Accent.copy(alpha = 0.8f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )

                            overflowMenuLinks.forEach { link ->
                                DrawerCompactRow(
                                    icon = link.icon,
                                    label = link.label,
                                    onClick = {
                                        menuExpanded = false
                                        navigateTo(link.url, tabIndex = null)
                                    }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CardBorderSubtle)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Wisdom Tower Academy • Native Shell",
                            color = Muted.copy(alpha = 0.45f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (showNotificationSettingsDialog) {
            NotificationSettingsDialog(
                onDismiss = { showNotificationSettingsDialog = false }
            )
        }

        if (showTimerControlDialog) {
            TimerControlDialog(
                remainingSeconds = timerRemainingSeconds,
                isRunning = isTimerRunning,
                isPaused = isTimerPaused,
                onTogglePlayPause = {
                    if (isTimerRunning) {
                        isTimerRunning = false
                        isTimerPaused = true
                        val js = "(function(){try{" +
                            "window.dispatchEvent(new CustomEvent('wta-study-timer-control',{detail:{action:'pause'}}));" +
                            "var raw=localStorage.getItem('wt_focus_timer_v1');" +
                            "if(raw){var s=JSON.parse(raw);s.running=false;s.endAt=null;s.leftWhenPaused=$timerRemainingSeconds;localStorage.setItem('wt_focus_timer_v1',JSON.stringify(s));window.dispatchEvent(new CustomEvent('wt-focus-timer'));}" +
                            "}catch(e){}})();"
                        webView?.evaluateJavascript(js, null)
                    } else {
                        isTimerRunning = true
                        isTimerPaused = false
                        val js = "(function(){try{" +
                            "window.dispatchEvent(new CustomEvent('wta-study-timer-control',{detail:{action:'resume'}}));" +
                            "var raw=localStorage.getItem('wt_focus_timer_v1');" +
                            "if(raw){var s=JSON.parse(raw);s.running=true;s.endAt=Date.now()+($timerRemainingSeconds*1000);localStorage.setItem('wt_focus_timer_v1',JSON.stringify(s));window.dispatchEvent(new CustomEvent('wt-focus-timer'));}" +
                            "}catch(e){}})();"
                        webView?.evaluateJavascript(js, null)
                    }
                },
                onStop = stopStudyTimerWithEndFlow,
                onOpenLearning = {
                    showTimerControlDialog = false
                    navigateTo("https://www.wisdom-tower-academy.live/learning?tool=timer", tabIndex = 1)
                },
                onDismiss = { showTimerControlDialog = false }
            )
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
 * Advanced, high-tech bottom navigation bar.
 * Clean, precise icon + typography tinting, ZERO clutter dots.
 * Icons are strictly contained within their capsule bounds with non-bouncy micro-press damping.
 * Background blends 100% seamlessly into the mobile screen bottom edge with NO gap and NO color difference.
 */
/**
 * Segmented Pill Bottom Navigation matching the Account page toggle language:
 * [ Academic Analytics & Progress ]  [ Inquiries & Support ]
 * - Shared dark capsule track: rounded-full ends, dark surface, quiet 1px border.
 * - Selected active tab: solid cyan/teal pill (stadium / 999.dp capsule),
 *   crisp #22E0FF cyan border, deep rich teal background, cyan icon & bold label.
 * - Unselected tabs: completely unboxed, quiet muted slate icon & label.
 * - Selection animation: smooth physics-based spring slide across the shared track.
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
                .background(BarBg)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Subtle hairline gradient divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x2622E0FF),
                                Color(0x4D22E0FF),
                                Color(0x2622E0FF),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Shared Dark Capsule Track matching the Account page segmented pill toggle
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF09111D))
                    .border(
                        BorderStroke(1.dp, Color(0x3322E0FF)),
                        RoundedCornerShape(999.dp)
                    )
            ) {
                val totalWidth = maxWidth
                val itemCount = items.size.coerceAtLeast(1)
                val tabWidth = totalWidth / itemCount

                // Smooth sliding active pill indicator across the shared track
                val animatedLeftOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "bottomNavSegmentSlide"
                )

                // The active capsule pill (identical to the Account active pill segment)
                Box(
                    modifier = Modifier
                        .offset(x = animatedLeftOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp, vertical = 2.5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F3D52),
                                    Color(0xFF0A2B3A)
                                )
                            )
                        )
                        .border(
                            BorderStroke(1.5.dp, Accent),
                            RoundedCornerShape(999.dp)
                        )
                )

                // 5 Tab items row layered on the track
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = selectedIndex == index
                        val interactionSource = remember(index) { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        // Micro-scale on press for tactile feel
                        val contentScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.93f else (if (selected) 1.04f else 1.0f),
                            animationSpec = spring(
                                dampingRatio = 0.75f,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "tabItemScale"
                        )

                        val iconColor by animateColorAsState(
                            targetValue = if (selected) Accent else Muted,
                            animationSpec = tween(180),
                            label = "tabIconColor"
                        )

                        val textColor by animateColorAsState(
                            targetValue = if (selected) Accent else Muted,
                            animationSpec = tween(180),
                            label = "tabTextColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
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
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.title,
                                    color = textColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    letterSpacing = (-0.1).sp
                                )
                            }
                        }
                    }
                }
            }
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
/**
 * Restored and Fixed Custom Animated Loader:
 * - Brand animated logo in center (BrandLoader, showCard = false)
 * - Opposite-direction high-speed neon rings (cyan & violet/indigo)
 * - Radial outward thick gear ticks / cog teeth rotating continuously
 * - Pure transparent background: NO card, NO panel, NO dim box
 * - NO text at all (no "Loading…", no titles, no captions)
 * - NO percent (no 20%, 80%, etc.)
 * - Constant single scale (110.dp) and constant center (no size jumps, no glitches)
 */
@Composable
private fun CustomCenteredLoader(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        FuturisticGearRings(
            size = 110.dp,
            brandSize = 64.dp,
            numTicks = 16,
            tickInnerRatio = 0.68f,
            tickOuterRatio = 0.88f,
            tickWidth = 3.dp
        )
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
private fun DrawerCompactRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) Color(0x2222E0FF) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Accent.copy(alpha = 0.2f))
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x1F22E0FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                color = Color(0xFFF1F5F9),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Muted.copy(alpha = 0.35f),
            modifier = Modifier.size(11.dp)
        )
    }
}

@Composable
private fun NotificationSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var timerEnabled by remember { mutableStateOf(AcademyNotificationManager.isTimerEnabled(context)) }
    var plannerEnabled by remember { mutableStateOf(AcademyNotificationManager.isPlannerEnabled(context)) }
    var goalsEnabled by remember { mutableStateOf(AcademyNotificationManager.isGoalsEnabled(context)) }
    var updatesEnabled by remember { mutableStateOf(AcademyNotificationManager.isUpdatesEnabled(context)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = CardSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
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
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2622E0FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = "Notification Settings",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Muted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorderSubtle))
                Spacer(modifier = Modifier.height(12.dp))

                NotificationToggleItem(
                    title = "Study Timer Rewards",
                    description = "Warm completion alerts with rewarding messages when sessions end",
                    checked = timerEnabled,
                    onCheckedChange = {
                        timerEnabled = it
                        AcademyNotificationManager.setSetting(context, AcademyNotificationManager.KEY_TIMER_ENABLED, it)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                NotificationToggleItem(
                    title = "Planner & Deadlines",
                    description = "Personalized due-time reminders for scheduled study tasks",
                    checked = plannerEnabled,
                    onCheckedChange = {
                        plannerEnabled = it
                        AcademyNotificationManager.setSetting(context, AcademyNotificationManager.KEY_PLANNER_ENABLED, it)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                NotificationToggleItem(
                    title = "Daily Goal Nudges",
                    description = "Caring, encouraging check-ins when under your daily study target",
                    checked = goalsEnabled,
                    onCheckedChange = {
                        goalsEnabled = it
                        AcademyNotificationManager.setSetting(context, AcademyNotificationManager.KEY_GOALS_ENABLED, it)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                NotificationToggleItem(
                    title = "Academy Announcements",
                    description = "New materials, exam schedules, and curriculum updates",
                    checked = updatesEnabled,
                    onCheckedChange = {
                        updatesEnabled = it
                        AcademyNotificationManager.setSetting(context, AcademyNotificationManager.KEY_UPDATES_ENABLED, it)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text(
                        text = "Save Preferences",
                        color = Color(0xFF04101A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Muted,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF04101A),
                checkedTrackColor = Accent,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

@Composable
private fun TimerControlDialog(
    remainingSeconds: Int,
    isRunning: Boolean,
    isPaused: Boolean,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onOpenLearning: () -> Unit,
    onDismiss: () -> Unit,
) {
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formatted = String.format("%02d:%02d", mins, secs)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = CardSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Focus Session",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Muted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = formatted,
                    color = Accent,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = if (isRunning) "Focus mode active — we'll notify you on completion" else "Session paused",
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTogglePlayPause,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) Color(0x3322E0FF) else Accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (isRunning) Accent else Color(0xFF04101A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "Pause" else "Resume",
                            color = if (isRunning) Accent else Color(0xFF04101A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x26EF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(
                            text = "Stop",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onOpenLearning) {
                    Text(
                        text = "Open in Study Hub",
                        color = Accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
