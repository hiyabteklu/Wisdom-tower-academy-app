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
internal val BarBg = Color(0xFF060B15) // Deep navy, exactly matching website page background
internal val Accent = Color(0xFF22E0FF) // Website source of truth cyan
internal val AccentDark = Color(0xFF00C4E6)
internal val CardSurface = Color(0xFF0C1424) // Glass-like card surface
internal val CardBorder = Color(0x3322E0FF) // Soft 1px cyan border
internal val CardBorderSubtle = Color(0x1F22E0FF) // Hairline quiet divider
internal val SurfaceColor = CardSurface
internal val Muted = Color(0xFF94A3B8)
internal val TextPrimary = Color(0xFFF8FAFC)
internal val DarkOnCyan = Color(0xFF070D17) // Dark text on solid cyan pills

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
    "header:not([data-ai-tutor-header]),header:not([data-ai-tutor-header]).fixed.top-0,header:not([data-ai-tutor-header])[data-site-header],.site-header,[data-site-header],[role=\"banner\"]:not([data-ai-tutor-header])," +
    "nav[aria-label=\"Main\"],nav.hidden.md\\:flex,.site-nav,.site-navigation,[data-site-nav]," +
    "body > footer,footer.site-footer,[data-site-footer],[role=\"contentinfo\"],footer:not([data-ai-tutor-root] footer)," +
    ".hide-on-app,.app-hidden,[data-hide-on-app],[data-hide-app],.web-only,[data-web-only]," +
    "#nprogress,.nprogress,#nprogress .bar,[data-nprogress],#nextjs-toploader,.nextjs-toploader," +
    "nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast]," +
    "header:not([data-ai-tutor-header]) button[aria-label*=\"menu\" i],button[aria-label*=\"menu\" i],.mobile-menu,[data-mobile-menu]," +
    "nav[aria-label*=\"mobile\" i],[data-bottom-nav],.bottom-nav,nav.fixed.bottom-0," +
    "img.wta-img-broken,img:not([src]),img[src=\"\"]" +
    "{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;opacity:0!important;pointer-events:none!important;margin:0!important;padding:0!important;}" +
    "div:has(#wt-ai-tutor-input){bottom:0!important;}" +
    "[data-ai-tutor-root] footer,#wt-ai-tutor-input,[role=\"dialog\"] footer" +
    "{display:block!important;visibility:visible!important;height:auto!important;max-height:none!important;opacity:1!important;pointer-events:auto!important;}" +
    "</style>"

private const val EARLY_HIDE_CHROME_JS =
    "(function(){try{" +
        "if(window.location.protocol==='file:')return;" +
        "document.documentElement.classList.add('wta-native-app');" +
        "if(document.body){document.body.classList.add('wta-native-app');}" +
        "var id='wta-app-chrome';var s=document.getElementById(id);" +
        "if(!s){s=document.createElement('style');s.id=id;var head=document.head||document.documentElement;if(head){head.insertBefore(s,head.firstChild);}}" +
        "s.textContent='html,body{background-color:#060B15!important;color-scheme:dark!important;}header:not([data-ai-tutor-header]),header:not([data-ai-tutor-header]).fixed.top-0,header:not([data-ai-tutor-header])[data-site-header],body > footer,footer.site-footer,[data-site-footer],.site-header,.site-footer,footer:not([data-ai-tutor-root] footer)," +
        "nav[aria-label=\"Main\"],nav.hidden.md\\\\:flex,.site-nav,.site-navigation,[data-site-nav],[role=\"banner\"]:not([data-ai-tutor-header]),[role=\"contentinfo\"]," +
        ".hide-on-app,.app-hidden,[data-hide-on-app],[data-hide-app],.web-only,[data-web-only]," +
        "#nprogress,.nprogress,#nprogress .bar,[data-nprogress],#nextjs-toploader,.nextjs-toploader," +
        "nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast],header:not([data-ai-tutor-header]) button[aria-label*=\"menu\" i],button[aria-label*=\"menu\" i],.mobile-menu,[data-mobile-menu],nav[aria-label*=\"mobile\" i],[data-bottom-nav],.bottom-nav,nav.fixed.bottom-0,img.wta-img-broken,img:not([src]),img[src=\"\"]," +
        "template[data-dgst]+div,[data-dgst=\"BAILOUT_TO_CLIENT_SIDE_RENDERING\"]+div,body.wta-tool-overlay header:not([data-ai-tutor-header]),body.wta-tool-overlay footer:not([data-ai-tutor-root] footer),html.wta-tool-overlay header:not([data-ai-tutor-header]),html.wta-tool-overlay footer:not([data-ai-tutor-root] footer)" +
        "{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;opacity:0!important;pointer-events:none!important;margin:0!important;padding:0!important;}" +
        "div:has(#wt-ai-tutor-input){bottom:0!important;}" +
        "[data-ai-tutor-root] footer,#wt-ai-tutor-input,[role=\"dialog\"] footer" +
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
        "'header:not([data-ai-tutor-header]),header:not([data-ai-tutor-header]).fixed.top-0,header:not([data-ai-tutor-header])[data-site-header],body > footer,footer.site-footer,[data-site-footer],.site-header,.site-footer,footer:not([data-ai-tutor-root] footer),' +" +
        "'nav[aria-label=\"Main\"],nav.hidden.md\\\\:flex,.site-nav,.site-navigation,[data-site-nav],[role=\"banner\"]:not([data-ai-tutor-header]),[role=\"contentinfo\"],' +" +
        "'.hide-on-app,.app-hidden,[data-hide-on-app],[data-hide-app],.web-only,[data-web-only],' +" +
        "'#nprogress,.nprogress,#nprogress .bar,[data-nprogress],#nextjs-toploader,.nextjs-toploader,' +" +
        "'nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast],' +" +
        "'header:not([data-ai-tutor-header]) button[aria-label*=\"menu\" i],button[aria-label*=\"menu\" i],.mobile-menu,[data-mobile-menu],nav[aria-label*=\"mobile\" i],[data-bottom-nav],.bottom-nav,nav.fixed.bottom-0,' +" +
        "'img.wta-img-broken,img:not([src]),img[src=\"\"]' +" +
        "'{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;opacity:0!important;pointer-events:none!important;margin:0!important;padding:0!important;}' +" +
        "'div:has(#wt-ai-tutor-input){bottom:0!important;}' +" +
        "'[data-ai-tutor-root] footer,#wt-ai-tutor-input,[role=\"dialog\"] footer' +" +
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
        "window.__wtaOverlay=true;" +
        "document.documentElement.classList.add('wta-native-app');" +
        "document.documentElement.classList.add('wta-tool-overlay');" +
        "document.documentElement.classList.add('wta-standalone-tool');" +
        "if(document.body){" +
            "document.body.classList.add('wta-native-app');" +
            "document.body.classList.add('wta-tool-overlay');" +
            "document.body.classList.add('wta-standalone-tool');" +
        "}" +
        "try{sessionStorage.setItem('wta-native-app','1');localStorage.setItem('wta-native-app','1');}catch(_){}" +
        "var styleId='wta-ai-tutor-style';" +
        "var st=document.getElementById(styleId);" +
        "if(!st){" +
            "st=document.createElement('style');" +
            "st.id=styleId;" +
            "(document.head||document.documentElement).appendChild(st);" +
        "}" +
        "st.textContent=" +
            "'[data-ai-tutor-header],header[data-ai-tutor-header],.ai-tutor-header,[data-tool-header],header:not([data-ai-tutor-header]),header.fixed.top-0,header[data-site-header],.site-header,[data-site-header],[role=\"banner\"],header button[aria-label*=\"menu\" i]{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;margin:0!important;padding:0!important;pointer-events:none!important;opacity:0!important;}' +" +
            "'body > footer,footer.site-footer,[data-site-footer],[role=\"contentinfo\"],.site-footer,footer:not([data-ai-tutor-root] footer){display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;margin:0!important;padding:0!important;pointer-events:none!important;opacity:0!important;}' +" +
            "'template[data-dgst]+div,[data-dgst=\"BAILOUT_TO_CLIENT_SIDE_RENDERING\"]+div,.wta-route-loader,[data-wta-spinner],[data-nextjs-loading]{display:none!important;visibility:hidden!important;height:0!important;}' +" +
            "'body.wta-tool-overlay header,html.wta-tool-overlay header,body.wta-tool-overlay [data-ai-tutor-header],html.wta-tool-overlay [data-ai-tutor-header]{display:none!important;visibility:hidden!important;height:0!important;overflow:hidden!important;}' +" +
            "'body.wta-tool-overlay footer:not([data-ai-tutor-root] footer),html.wta-tool-overlay footer:not([data-ai-tutor-root] footer){display:none!important;}' +" +
            "'div[data-ai-tutor-root]{display:flex!important;visibility:visible!important;}' +" +
            "'[data-ai-tutor-root] footer{display:block!important;visibility:visible!important;height:auto!important;max-height:none!important;opacity:1!important;pointer-events:auto!important;}' +" +
            "'#wt-ai-tutor-input{font-size:16px!important;color:#FFFFFF!important;caret-color:#00E5FF!important;background-color:#060B17!important;visibility:visible!important;opacity:1!important;}';" +
        "function syncAiTutorChrome(){" +
            "try{" +
                "var nodes=document.querySelectorAll('template[data-dgst]+div,h2,p,.wta-route-loader');" +
                "for(var k=0;k<nodes.length;k++){" +
                    "var el=nodes[k];" +
                    "var txt=(el.textContent||'').trim();" +
                    "if(txt.indexOf('Loading Learning Suite')!==-1||txt.indexOf('Opening your Wisdom Tower study')!==-1){" +
                        "var c=el.closest?el.closest('.relative,div'):null;" +
                        "if(c&&c!==document.body&&c!==document.documentElement){c.style.setProperty('display','none','important');}" +
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
                            "}else if(window.AndroidBridge&&typeof window.AndroidBridge.closeOverlay==='function'){" +
                                "e.preventDefault();e.stopPropagation();" +
                                "window.AndroidBridge.closeOverlay();" +
                            "}else if(window.Android&&typeof window.Android.closeOverlay==='function'){" +
                                "e.preventDefault();e.stopPropagation();" +
                                "window.Android.closeOverlay();" +
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
            "window.__wta_tutor_timer=setInterval(syncAiTutorChrome,300);" +
        "}" +
        "if(!window.__wta_close_ev_listener){" +
            "window.__wta_close_ev_listener=true;" +
            "window.addEventListener('wta-close-tool',function(){" +
                "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.returnToStudyPage==='function'){" +
                    "window.AndroidOfflineVault.returnToStudyPage();" +
                "}else if(window.AndroidBridge&&typeof window.AndroidBridge.closeOverlay==='function'){" +
                    "window.AndroidBridge.closeOverlay();" +
                "}else if(window.Android&&typeof window.Android.closeOverlay==='function'){" +
                    "window.Android.closeOverlay();" +
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
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
    var menuExpanded by remember { mutableStateOf(false) }

    // Instant cold-start and process-death network validation
    val isInitiallyOnline = isOnline(context)
    var lastTargetUrl by remember { mutableStateOf(initialNotificationUrl?.takeIf { it.isNotBlank() } ?: SITE) }

    var isInitialLoading by remember { mutableStateOf(true) }
    var minSplashElapsed by remember { mutableStateOf(false) }

    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showTimerControlDialog by remember { mutableStateOf(false) }
    var previousTimerRunning by remember { mutableStateOf(false) }

    var activeToolOverlayUrl by remember { mutableStateOf<String?>(null) }
    var activeToolOverlayTitle by remember { mutableStateOf("Study Tool") }
    var activeToolOverlayLoading by remember { mutableStateOf(false) }
    var toolOverlayWebView by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedOverlayUrl by remember { mutableStateOf<String?>(null) }

    val openToolOverlay: (String, String) -> Unit = { rawUrl, title ->
        menuExpanded = false
        activeToolOverlayTitle = title
        activeToolOverlayLoading = true
        var finalUrl = rawUrl.trim()
        if (!finalUrl.contains("overlay=")) {
            finalUrl = if (finalUrl.contains("?")) "$finalUrl&overlay=1" else "$finalUrl?overlay=1"
        }
        if (!finalUrl.contains("app=")) {
            finalUrl = if (finalUrl.contains("?")) "$finalUrl&app=1" else "$finalUrl?app=1"
        }
        activeToolOverlayUrl = finalUrl
    }

    val closeToolOverlay: () -> Unit = {
        activeToolOverlayUrl = null
        lastLoadedOverlayUrl = null
        activeToolOverlayLoading = false
        toolOverlayWebView?.stopLoading()
    }

    fun showOffline(wv: WebView, force: Boolean = false) {
        if (!force && isOnline(context)) return
        val cur = wv.url.orEmpty()
        if (cur.contains("offline.html") || cur.startsWith("file:///android_asset/")) {
            return
        }
        mainHandler.post {
            if (!force && isOnline(context)) return@post
            try { wv.stopLoading() } catch (_: Exception) {}
            wv.loadUrl(OFFLINE_ASSET)
        }
    }

    // MP3-player style Study Timer state
    var isTimerRunning by remember { mutableStateOf(false) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var timerRemainingSeconds by remember { mutableIntStateOf(0) }
    var timerTotalSeconds by remember { mutableIntStateOf(0) }
    var timerTitle by remember { mutableStateOf("Study Timer") }
    var isTimerDismissed by remember { mutableStateOf(false) }

    val tabHostState = remember {
        TabWebViewHostState(
            context = context,
            onRouteChangedCallback = { path ->
                selectedIndex = tabIndexForUrl(path, selectedIndex)
                val lower = path.lowercase()
                if (lower.contains("login") || lower.contains("account") || lower.contains("dashboard") || lower.contains("learning")) {
                    FcmTokenRegistrar.checkAndRegisterToken(context)
                }
            },
            onUserLoginCallback = { _ ->
                FcmTokenRegistrar.checkAndRegisterToken(context)
            },
            syncStudyTimerCallback = { isRunning, secondsLeft, totalSeconds, title ->
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
                        AcademyNotificationManager.notifyStudyTimerCompleted(context)
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
            },
            notifyPlannerTaskCallback = { taskName, dueTime, studentName ->
                AcademyNotificationManager.notifyPlannerTaskDue(context, taskName, dueTime, studentName)
            },
            notifyDailyStudyGoalCallback = {
                AcademyNotificationManager.notifyDailyGoalNudge(context)
            },
            openNotificationSettingsCallback = {
                showNotificationSettingsDialog = true
            },
            openToolOverlayCallback = { u, t ->
                openToolOverlay(u, t)
            },
            openOrDownloadPdfCallback = { wv, ctx, u ->
                openOrDownloadPdf(wv, ctx, u)
            },
            showOfflineCallback = { wv, force ->
                showOffline(wv, force)
            }
        )
    }

    fun navigateTo(url: String, tabIndex: Int? = null, resetHistory: Boolean = false) {
        val targetUrl = url.trim()
        val uri = try { Uri.parse(targetUrl) } catch (_: Exception) { null }
        if (uri != null && isLearningToolUri(uri)) {
            val toolTitle = when {
                targetUrl.contains("tool=tutor") -> "AI Tutor"
                targetUrl.contains("tool=calc") -> "Scientific Calculator"
                targetUrl.contains("tool=note") -> "Study Notebook"
                targetUrl.contains("tool=time") -> "Study Timer"
                targetUrl.contains("tool=plan") -> "Study Planner"
                else -> "Study Tool"
            }
            openToolOverlay(targetUrl, toolTitle)
            return
        }

        val path = uri?.path?.removeSuffix("/")?.lowercase().orEmpty()
        val isNotif = path == "/notifications" || path.startsWith("/notifications/")
        val targetTab = if (isNotif) {
            selectedIndex // Notifications do NOT alter selected bottom-nav tab
        } else if (tabIndex != null) {
            tabIndex
        } else {
            tabIndexForUrl(targetUrl, selectedIndex)
        }

        if (!isNotif) {
            selectedIndex = targetTab
        }

        tabHostState.navigateTo(
            url = targetUrl,
            targetTabIndex = targetTab,
            resetHistory = resetHistory,
            isOnline = isOnline(context)
        )
    }

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
        tabHostState.getActiveWebView(selectedIndex)?.evaluateJavascript(js, null)

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

    // Initial launch splash check
    LaunchedEffect(Unit) {
        FcmTokenRegistrar.checkAndRegisterToken(context)
        onReady()
        delay(MIN_SPLASH_DISPLAY_MS)
        minSplashElapsed = true
        isInitialLoading = false
    }

    // Prewarm Learning tab in background (Requirement 5)
    LaunchedEffect(Unit) {
        delay(600L)
        tabHostState.prewarmTab(TAB_LEARNING)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, selectedIndex) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val wv = tabHostState.getActiveWebView(selectedIndex) ?: return@LifecycleEventObserver
                    wv.onResume()
                    wv.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    val wv = tabHostState.getActiveWebView(selectedIndex) ?: return@LifecycleEventObserver
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
        // 1) Tool overlay has highest priority
        if (activeToolOverlayUrl != null) {
            val twv = toolOverlayWebView
            if (twv != null && twv.canGoBack()) {
                val list = twv.copyBackForwardList()
                val currIdx = list.currentIndex
                if (currIdx > 0) {
                    twv.goBack()
                    return@BackHandler
                }
            }
            closeToolOverlay()
            return@BackHandler
        }

        // 2) Active tab history
        if (tabHostState.canActiveTabGoBack(selectedIndex)) {
            tabHostState.activeTabGoBack(selectedIndex)
            return@BackHandler
        }

        // 3) Onboarding rules
        if (showOnboarding) return@BackHandler
        if (showExitDialog) {
            showExitDialog = false
            return@BackHandler
        }

        // 4) Existing exit behavior
        if (selectedIndex != TAB_HOME) {
            navigateTo(StructuralNav.SITE_HOME, tabIndex = TAB_HOME)
            return@BackHandler
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            activity?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
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
                                    tabHostState.reloadActiveTab(selectedIndex, isOnline(context))
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
                TabWebViewHost(
                    state = tabHostState,
                    selectedIndex = selectedIndex,
                    modifier = Modifier.fillMaxSize()
                )

                // Initial launch splash only
                AnimatedVisibility(
                    visible = isInitialLoading,
                    enter = fadeIn(tween(140, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(200, easing = FastOutSlowInEasing)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(95f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomCenteredLoader()
                    }
                }
            }
        }

        // Study Tool Overlay Layer (Independent second WebView preserving underlying study page)
        AnimatedVisibility(
            visible = activeToolOverlayUrl != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(160)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(130f)
        ) {
            BackHandler(enabled = activeToolOverlayUrl != null) {
                val twv = toolOverlayWebView
                if (twv != null && twv.canGoBack()) {
                    val list = twv.copyBackForwardList()
                    val currIdx = list.currentIndex
                    if (currIdx > 0) {
                        twv.goBack()
                        return@BackHandler
                    }
                }
                closeToolOverlay()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF060B15))
                    .imePadding()
            ) {
                // Dedicated Thin Native Header Bar with Back / Title / Done button
                Surface(
                    color = Color(0xFF070D18),
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    closeToolOverlay()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close tool overlay",
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = activeToolOverlayTitle,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        // Explicit Red "Close" Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x26EF4444))
                                .border(BorderStroke(1.dp, Color(0x66EF4444)), RoundedCornerShape(8.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    closeToolOverlay()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Close",
                                color = Color(0xFFF87171),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
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

                // Tool Overlay WebView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF060B15))
                ) {
                    activeToolOverlayUrl?.let { toolUrl ->
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    setBackgroundColor(AndroidColor.parseColor("#060B15"))
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
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        mediaPlaybackRequiresUserGesture = false
                                        allowFileAccess = false
                                        allowContentAccess = false
                                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                        cacheMode = if (isOnline(ctx)) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                                        userAgentString = "${settings.userAgentString} WisdomTowerApp wta-native"
                                    }
                                    try {
                                        CookieManager.getInstance().setAcceptCookie(true)
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    } catch (_: Exception) {}
                                    var overlayShowingOffline = false

                                    fun loadToolTarget(wv: WebView, urlToLoad: String) {
                                        val online = isOnline(ctx)
                                        wv.settings.cacheMode = if (online) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                                        if (online) {
                                            overlayShowingOffline = false
                                            activeToolOverlayLoading = true
                                            lastLoadedOverlayUrl = urlToLoad
                                            wv.loadUrl(urlToLoad)
                                        } else {
                                            try {
                                                wv.stopLoading()
                                            } catch (_: Exception) {}
                                            overlayShowingOffline = true
                                            activeToolOverlayLoading = false
                                            wv.loadUrl(OFFLINE_ASSET)
                                        }
                                    }

                                    val overlayBridge = object {
                                        @JavascriptInterface
                                        fun reloadLastOnlinePage() {
                                            mainHandler.post {
                                                val wv = toolOverlayWebView ?: return@post
                                                val online = isOnline(ctx)
                                                if (online) {
                                                    overlayShowingOffline = false
                                                    activeToolOverlayLoading = true
                                                    val urlToLoad = activeToolOverlayUrl ?: toolUrl
                                                    wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
                                                    try {
                                                        wv.stopLoading()
                                                    } catch (_: Exception) {}
                                                    lastLoadedOverlayUrl = urlToLoad
                                                    wv.loadUrl(urlToLoad)
                                                } else {
                                                    mainHandler.postDelayed({
                                                        wv.evaluateJavascript("if(typeof onRetryFailed==='function')onRetryFailed();", null)
                                                    }, 700L)
                                                }
                                            }
                                        }
                                        @JavascriptInterface
                                        fun returnToStudyPage() {
                                            mainHandler.post { closeToolOverlay() }
                                        }
                                        @JavascriptInterface
                                        fun closeOverlay() {
                                            mainHandler.post { closeToolOverlay() }
                                        }
                                        @JavascriptInterface
                                        fun closeTool() {
                                            mainHandler.post { closeToolOverlay() }
                                        }
                                        @JavascriptInterface
                                        fun dismissOverlay() {
                                            mainHandler.post { closeToolOverlay() }
                                        }
                                        @JavascriptInterface
                                        fun onClose() {
                                            mainHandler.post { closeToolOverlay() }
                                        }
                                        @JavascriptInterface
                                        fun isOverlay(): Boolean = true
                                        @JavascriptInterface
                                        fun isNativeApp(): Boolean = true
                                    }
                                    addJavascriptInterface(overlayBridge, "AndroidOfflineVault")
                                    addJavascriptInterface(overlayBridge, "AndroidBridge")
                                    addJavascriptInterface(overlayBridge, "Android")
                                    addJavascriptInterface(overlayBridge, "WisdomTower")
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            val currentUrl = view?.url.orEmpty()
                                            val isOfflinePage = currentUrl.contains("offline.html") || currentUrl.startsWith("file://")
                                            if (newProgress >= 70 && !isOfflinePage) {
                                                view?.evaluateJavascript("document.documentElement.classList.add('wta-native-app');document.documentElement.classList.add('wta-tool-overlay');", null)
                                                view?.evaluateJavascript(AI_TUTOR_CHROME_JS, null)
                                            }
                                            if (newProgress >= 85) {
                                                activeToolOverlayLoading = false
                                            }
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            val cur = url.orEmpty()
                                            val isOfflinePage = cur.contains("offline.html") || cur.startsWith("file://")
                                            if (!isOfflinePage) {
                                                overlayShowingOffline = false
                                                activeToolOverlayLoading = true
                                                view?.evaluateJavascript("document.documentElement.classList.add('wta-native-app');document.documentElement.classList.add('wta-tool-overlay');", null)
                                                view?.evaluateJavascript(EARLY_HIDE_CHROME_JS, null)
                                                view?.evaluateJavascript(AI_TUTOR_CHROME_JS, null)
                                            } else {
                                                overlayShowingOffline = true
                                                activeToolOverlayLoading = false
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            val cur = url.orEmpty()
                                            val isOfflinePage = cur.contains("offline.html") || cur.startsWith("file://")
                                            activeToolOverlayLoading = false
                                            if (!isOfflinePage) {
                                                overlayShowingOffline = false
                                                view?.evaluateJavascript("document.documentElement.classList.add('wta-native-app');document.documentElement.classList.add('wta-tool-overlay');", null)
                                                view?.evaluateJavascript(EARLY_HIDE_CHROME_JS, null)
                                                view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                                view?.evaluateJavascript(AI_TUTOR_CHROME_JS, null)
                                                view?.evaluateJavascript(PRECACHE_AND_UNBLOCK_JS, null)
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
                                            mainHandler.post {
                                                try {
                                                    wv.stopLoading()
                                                } catch (_: Exception) {}
                                                overlayShowingOffline = true
                                                activeToolOverlayLoading = false
                                                wv.loadUrl(OFFLINE_ASSET)
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
                                            mainHandler.post {
                                                try {
                                                    wv.stopLoading()
                                                } catch (_: Exception) {}
                                                overlayShowingOffline = true
                                                activeToolOverlayLoading = false
                                                wv.loadUrl(OFFLINE_ASSET)
                                            }
                                        }

                                        override fun shouldInterceptRequest(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): WebResourceResponse? {
                                            val req = request ?: return null
                                            val u = req.url?.toString() ?: return null
                                            val isGet = (req.method?.uppercase() ?: "GET") == "GET"
                                            val cleanLower = u.lowercase()
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
                                            return super.shouldInterceptRequest(view, request)
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
                                                toolOverlayWebView = null
                                                lastLoadedOverlayUrl = null
                                            }
                                            return true
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val u = request?.url?.toString() ?: return false
                                            val uri = request.url ?: return false
                                            val host = uri.host?.lowercase().orEmpty()
                                            val isInternal = isAllowedDomain(host)
                                            if (isInternal && !isLearningToolUri(uri)) {
                                                closeToolOverlay()
                                                navigateTo(u)
                                                return true
                                            }
                                            if (!isInternal && !u.startsWith("file://")) {
                                                try {
                                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                } catch (_: Exception) {}
                                                return true
                                            }
                                            return false
                                        }
                                    }
                                    toolOverlayWebView = this
                                    loadToolTarget(this, toolUrl)
                                }
                            },
                            update = { wv ->
                                if (lastLoadedOverlayUrl != toolUrl) {
                                    val online = isOnline(wv.context)
                                    wv.settings.cacheMode = if (online) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                                    if (online) {
                                        activeToolOverlayLoading = true
                                        lastLoadedOverlayUrl = toolUrl
                                        wv.loadUrl(toolUrl)
                                    } else {
                                        try {
                                            wv.stopLoading()
                                        } catch (_: Exception) {}
                                        activeToolOverlayLoading = false
                                        wv.loadUrl(OFFLINE_ASSET)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (activeToolOverlayLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF060B15)),
                            contentAlignment = Alignment.Center
                        ) {
                            CustomCenteredLoader()
                        }
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
                                    openToolOverlay("https://www.wisdom-tower-academy.live/learning?tool=tutor&overlay=1", "AI Tutor")
                                }
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.Calculate,
                                label = "Calculator",
                                onClick = {
                                    menuExpanded = false
                                    openToolOverlay("https://www.wisdom-tower-academy.live/learning?tool=calculator&overlay=1", "Scientific Calculator")
                                }
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.EditNote,
                                label = "Notebook",
                                onClick = {
                                    menuExpanded = false
                                    openToolOverlay("https://www.wisdom-tower-academy.live/learning?tool=notes&overlay=1", "Study Notebook")
                                }
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.Timer,
                                label = "Timer",
                                onClick = {
                                    menuExpanded = false
                                    openToolOverlay("https://www.wisdom-tower-academy.live/learning?tool=timer&overlay=1", "Study Timer")
                                }
                            )

                            DrawerCompactRow(
                                icon = Icons.Filled.CalendarMonth,
                                label = "Planner",
                                onClick = {
                                    menuExpanded = false
                                    openToolOverlay("https://www.wisdom-tower-academy.live/learning?tool=planner&overlay=1", "Study Planner")
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
                    openToolOverlay("https://www.wisdom-tower-academy.live/learning?tool=timer", "Study Timer")
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
