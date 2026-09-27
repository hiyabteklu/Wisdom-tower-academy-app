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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.material3.ripple
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
        "nav[aria-label=\"Main\"],.hide-on-app,#nprogress,.nprogress,#nprogress .bar," +
        "[data-nprogress],#nextjs-toploader,.nextjs-toploader" +
        "{display:none!important;visibility:hidden!important;height:0!important;overflow:hidden!important;opacity:0!important;}';" +
        "var noCopyId='wta-disable-copy';var cs=document.getElementById(noCopyId);" +
        "if(!cs){cs=document.createElement('style');cs.id=noCopyId;document.documentElement.appendChild(cs);}" +
        "cs.textContent=" +
        "'*,html,body,div,p,span,h1,h2,h3,h4,h5,h6,a,li,table,td,th,article,section,main,pre,code{" +
        "-webkit-user-select:none!important;-moz-user-select:none!important;-ms-user-select:none!important;user-select:none!important;-webkit-touch-callout:none!important;}" +
        "input,textarea,[contenteditable=\"true\"]{" +
        "-webkit-user-select:auto!important;-moz-user-select:auto!important;user-select:auto!important;-webkit-touch-callout:default!important;}';" +
        "if(!window.__wta_copy_blocked){" +
        "window.__wta_copy_blocked=true;" +
        "document.addEventListener('copy',function(e){var t=e.target;if(t&&(t.tagName==='INPUT'||t.tagName==='TEXTAREA'||t.isContentEditable))return;e.preventDefault();if(e.clipboardData)e.clipboardData.setData('text/plain','');return false;},true);" +
        "document.addEventListener('cut',function(e){var t=e.target;if(t&&(t.tagName==='INPUT'||t.tagName==='TEXTAREA'||t.isContentEditable))return;e.preventDefault();return false;},true);" +
        "document.addEventListener('contextmenu',function(e){var t=e.target;if(t&&(t.tagName==='INPUT'||t.tagName==='TEXTAREA'||t.isContentEditable))return;e.preventDefault();return false;},true);" +
        "document.addEventListener('selectstart',function(e){var t=e.target;if(t&&(t.tagName==='INPUT'||t.tagName==='TEXTAREA'||t.isContentEditable))return;e.preventDefault();return false;},true);" +
        "document.addEventListener('dragstart',function(e){var t=e.target;if(t&&(t.tagName==='INPUT'||t.tagName==='TEXTAREA'||t.isContentEditable))return;e.preventDefault();return false;},true);" +
        "document.addEventListener('selectionchange',function(){try{var s=window.getSelection();if(!s||s.isCollapsed)return;var a=s.anchorNode;var p=a?(a.nodeType===1?a:a.parentElement):null;if(p&&p.closest&&p.closest('input,textarea,[contenteditable=\"true\"]'))return;s.removeAllRanges();}catch(_){}});" +
        "}" +
        "if(!window.__wta_interactive_loader_bound){" +
            "window.__wta_interactive_loader_bound=true;" +
            "var _navT=null;" +
            "function _startNav(){" +
                "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.notifyLoadingStarted==='function'){" +
                    "window.AndroidOfflineVault.notifyLoadingStarted('Loading…');" +
                "}" +
                "clearTimeout(_navT);" +
                "_navT=setTimeout(_stopNav,3500);" +
            "}" +
            "function _stopNav(){" +
                "clearTimeout(_navT);" +
                "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.notifyLoadingFinished==='function'){" +
                    "window.AndroidOfflineVault.notifyLoadingFinished();" +
                "}" +
            "}" +
            "var _origPush=history.pushState;" +
            "history.pushState=function(){" +
                "_startNav();" +
                "return _origPush.apply(this,arguments);" +
            "};" +
            "var _origReplace=history.replaceState;" +
            "history.replaceState=function(){" +
                "_startNav();" +
                "return _origReplace.apply(this,arguments);" +
            "};" +
            "document.addEventListener('click',function(e){" +
                "var el=e.target;" +
                "while(el&&el!==document.body){" +
                    "var tag=(el.tagName||'').toLowerCase();" +
                    "var href=el.getAttribute?(el.getAttribute('href')||''):'';" +
                    "var cls=(el.className||'').toString().toLowerCase();" +
                    "var isInteractive=cls.indexOf('card')!==-1||cls.indexOf('subject')!==-1||cls.indexOf('hub')!==-1||cls.indexOf('module')!==-1||cls.indexOf('item')!==-1||cls.indexOf('cursor-pointer')!==-1||(el.getAttribute&&(el.getAttribute('role')==='button'||el.hasAttribute('data-subject')||el.hasAttribute('data-slug')||el.hasAttribute('data-hub')));" +
                    "if((tag==='a'&&href&&href.charAt(0)!=='#'&&href.indexOf('javascript:')===-1)||isInteractive){" +
                        "if(!href||href.charAt(0)==='/'||href.indexOf('wisdom-tower-academy.live')!==-1){" +
                            "_startNav();" +
                        "}" +
                        "break;" +
                    "}" +
                    "el=el.parentElement;" +
                "}" +
            "},true);" +
            "var _lastUrl=window.location.href;" +
            "var _obs=new MutationObserver(function(){" +
                "if(window.location.href!==_lastUrl){" +
                    "_lastUrl=window.location.href;" +
                    "setTimeout(_stopNav,200);" +
                "}" +
            "});" +
            "_obs.observe(document.documentElement,{subtree:true,childList:true});" +
            "window.addEventListener('popstate',function(){setTimeout(_stopNav,200);});" +
        "}" +
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
        "var existingHud=document.getElementById('wta-floating-download-hud');" +
        "if(existingHud&&existingHud.parentNode){existingHud.parentNode.removeChild(existingHud);}" +
        "if(!window.__wta_fetch_probe_hook&&window.fetch){" +
            "window.__wta_fetch_probe_hook=true;" +
            "var _origFetch=window.fetch;" +
            "window.fetch=function(input,init){" +
                "var urlStr=(typeof input==='string')?input:(input&&input.url?input.url:'');" +
                "if(urlStr&&(urlStr.indexOf('/api/content/pdf')!==-1||urlStr.indexOf('.pdf')!==-1)){" +
                    "window.__wta_current_pdf_url=urlStr;" +
                    "var m=(init&&init.method)?init.method.toUpperCase():(input&&input.method?input.method.toUpperCase():'GET');" +
                    "var h=(init&&init.headers)?init.headers:(input&&input.headers?input.headers:null);" +
                    "var isRangeProbe=false;" +
                    "if(h){" +
                        "var r='';" +
                        "if(typeof h.get==='function'){r=h.get('Range')||h.get('range')||'';}" +
                        "else if(Array.isArray(h)){" +
                            "for(var hi=0;hi<h.length;hi++){" +
                                "if(h[hi]&&h[hi][0]&&h[hi][0].toLowerCase()==='range'){r=h[hi][1];break;}" +
                            "}" +
                        "}else if(typeof h==='object'){r=h.Range||h.range||'';}" +
                        "if(r&&(r.indexOf('bytes=0-0')!==-1||r.indexOf('bytes=0-1')!==-1))isRangeProbe=true;" +
                    "}" +
                    "if(m==='HEAD'||isRangeProbe){" +
                        "var fullUrl=(typeof URL==='function')?(new URL(urlStr,window.location.href)).href:urlStr;" +
                        "var sz=0;" +
                        "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.getPdfSize==='function'){" +
                            "sz=window.AndroidOfflineVault.getPdfSize(fullUrl);" +
                            "if(!sz||sz<=0)sz=window.AndroidOfflineVault.getPdfSize(urlStr);" +
                        "}" +
                        "if(!sz||sz<=0){" +
                            "var titleEl=document.querySelector('h1,h2,h3,.font-display,[data-book-title],[data-title]');" +
                            "var titleText=(titleEl?titleEl.textContent:'')||'';" +
                            "if(titleText&&window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.getPdfSizeByTitle==='function'){" +
                                "sz=window.AndroidOfflineVault.getPdfSizeByTitle(titleText);" +
                            "}" +
                        "}" +
                        "var respH=new Headers();" +
                        "respH.set('Content-Type','application/pdf');" +
                        "respH.set('Accept-Ranges','bytes');" +
                        "respH.set('Access-Control-Expose-Headers','Content-Length, Content-Range, Accept-Ranges, Content-Type');" +
                        "if(sz&&sz>0){" +
                            "respH.set('Content-Length',String(sz));" +
                            "respH.set('Content-Range','bytes 0-0/'+sz);" +
                            "var status=(m==='HEAD')?200:206;" +
                            "var statusText=(m==='HEAD')?'OK':'Partial Content';" +
                            "return Promise.resolve(new Response(new Uint8Array(1),{status:status,statusText:statusText,headers:respH}));" +
                        "}else{" +
                            "return Promise.resolve(new Response(new Uint8Array(0),{status:503,statusText:'Size Unknown',headers:respH}));" +
                        "}" +
                    "}" +
                "}" +
                "return _origFetch.apply(this,arguments);" +
            "};" +
        "}" +
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
                        "var orig=btn.innerHTML;" +
                        "btn.innerHTML='<span style=\"display:inline-flex;align-items:center;gap:6px;\">Already downloaded — opening…</span>';" +
                        "btn.disabled=true;" +
                        "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.openCachedPdf==='function'){" +
                            "window.AndroidOfflineVault.openCachedPdf(u);" +
                        "}else if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.markDownloadStarted==='function'){" +
                            "window.AndroidOfflineVault.markDownloadStarted(u);" +
                        "}" +
                        "setTimeout(function(){btn.disabled=false;btn.innerHTML=orig;},2500);" +
                        "return;" +
                    "}" +
                    "if(_wta_download_in_flight){" +
                        "e.preventDefault();" +
                        "e.stopPropagation();" +
                        "return;" +
                    "}" +
                    "_wta_download_in_flight=true;" +
                    "setTimeout(function(){_wta_download_in_flight=false;},8000);" +
                    "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.markDownloadStarted==='function'){" +
                        "window.AndroidOfflineVault.markDownloadStarted(u);" +
                    "}" +
                    "if(window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.getDownloadProgress==='function'){" +
                        "var fullU=(typeof URL==='function'&&u)?(new URL(u,window.location.href)).href:u;" +
                        "var _pTimer=setInterval(function(){" +
                            "try{" +
                                "var isBookOpen=Boolean(document.querySelector('[data-page],canvas,.react-pdf__Page,[data-pdf-page],button[title=\"Top\"],input[type=\"number\"]'));" +
                                "if(isBookOpen){" +
                                    "clearInterval(_pTimer);" +
                                    "return;" +
                                "}" +
                                "var raw=window.AndroidOfflineVault.getDownloadProgress(u)||" +
                                        "window.AndroidOfflineVault.getDownloadProgress(fullU)||" +
                                        "window.AndroidOfflineVault.getDownloadProgress('');" +
                                "if(raw){" +
                                    "var p=JSON.parse(raw);" +
                                    "if(p&&p.loaded>0){" +
                                        "var tot=(p.total>0)?p.total:(p.loaded);" +
                                        "var pct=(tot>0)?Math.min(100,Math.round((p.loaded/tot)*100)):0;" +
                                        "var loadedStr=formatBytes(p.loaded);" +
                                        "var totStr=formatBytes(tot);" +
                                        "window.dispatchEvent(new CustomEvent('wta:download-progress',{detail:{url:u,loaded:p.loaded,total:tot,percent:pct}}));" +
                                        "var pBars=document.querySelectorAll('[role=\"progressbar\"],.progress-bar,[data-progress],div[class*=\"max-w-xs\"] div[class*=\"bg-cyan\"]');" +
                                        "for(var pi=0;pi<pBars.length;pi++){" +
                                            "pBars[pi].style.width=pct+'%';" +
                                            "pBars[pi].setAttribute('aria-valuenow',String(pct));" +
                                        "}" +
                                        "var textEls=document.querySelectorAll('p[class*=\"tabular-nums\"],.tabular-nums,[role=\"dialog\"] p,[role=\"dialog\"] span,.modal-content p');" +
                                        "for(var ti=0;ti<textEls.length;ti++){" +
                                            "var tEl=textEls[ti];" +
                                            "if(tEl.children.length>0)continue;" +
                                            "var tt=(tEl.textContent||'').trim();" +
                                            "if(tt.indexOf('/')!==-1&&(tt.indexOf('MB')!==-1||tt.indexOf('KB')!==-1)){" +
                                                "tEl.textContent=pct+'% • '+loadedStr+' / '+totStr;" +
                                            "}" +
                                        "}" +
                                        "var btns=document.querySelectorAll('button');" +
                                        "for(var bi=0;bi<btns.length;bi++){" +
                                            "var bEl=btns[bi];" +
                                            "var bText=(bEl.textContent||'').trim();" +
                                            "if(bText.indexOf('Downloading')!==-1||bText.indexOf('Save')!==-1||bText.indexOf('Download')!==-1){" +
                                                "if(bText.indexOf('(')!==-1||bText.indexOf('%')!==-1||bText.indexOf('•')!==-1){" +
                                                    "bEl.innerHTML='<span style=\"display:inline-flex;align-items:center;gap:6px;\">Downloading ' + pct + '% (' + loadedStr + ' / ' + totStr + ')</span>';" +
                                                "}" +
                                            "}" +
                                        "}" +
                                        "if(p.loaded>=tot&&tot>0){" +
                                            "clearInterval(_pTimer);" +
                                        "}" +
                                    "}" +
                                "}" +
                            "}catch(_){}" +
                        "},120);" +
                        "setTimeout(function(){clearInterval(_pTimer);},45000);" +
                    "}" +
                "}catch(err){}" +
            "},true);" +
        "}" +
        "function updatePdfPreOpenLabels(){" +
            "try{" +
                "var path=window.location.pathname||'';" +
                "var hasPdfModal=document.querySelector('[data-book-download],button[data-url*=\".pdf\"],button[data-url*=\"/api/content/pdf\"],a[href*=\"/api/content/pdf\"],a[href*=\".pdf\"]');" +
                "var isBookContext=(path.indexOf('/read')!==-1||path.indexOf('/book')!==-1||path.indexOf('/material')!==-1||path.indexOf('/content')!==-1||Boolean(hasPdfModal));" +
                "if(!isBookContext)return;" +
                "var targetUrl=window.__wta_current_pdf_url||'';" +
                "if(!targetUrl&&hasPdfModal){" +
                    "targetUrl=(typeof hasPdfModal.getAttribute==='function')?(hasPdfModal.getAttribute('href')||hasPdfModal.getAttribute('data-url')||''):'';" +
                "}" +
                "if(!targetUrl)return;" +
                "var fullUrl=targetUrl?((typeof URL==='function')?(new URL(targetUrl,window.location.href)).href:targetUrl):'';" +
                "var sz=0;" +
                "if(fullUrl&&window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.getPdfSize==='function'){" +
                    "sz=window.AndroidOfflineVault.getPdfSize(fullUrl);" +
                "}" +
                "if((!sz||sz<=0)&&targetUrl&&window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.getPdfSize==='function'){" +
                    "sz=window.AndroidOfflineVault.getPdfSize(targetUrl);" +
                "}" +
                "if(!sz||sz<=0){" +
                    "var titleEl=document.querySelector('h1,h2,h3,.font-display,[data-book-title],[data-title]');" +
                    "var titleText=(titleEl?titleEl.textContent:'')||'';" +
                    "if(titleText&&window.AndroidOfflineVault&&typeof window.AndroidOfflineVault.getPdfSizeByTitle==='function'){" +
                        "sz=window.AndroidOfflineVault.getPdfSizeByTitle(titleText);" +
                    "}" +
                "}" +
                "if(sz&&sz>0){" +
                    "var labelText=formatBytes(sz);" +
                    "var btns=document.querySelectorAll('button,a[data-download],a[download]');" +
                    "for(var b=0;b<btns.length;b++){" +
                        "var btn=btns[b];" +
                        "var bt=(btn.textContent||'').trim();" +
                        "if((bt.indexOf('Download & open')!==-1||bt==='Download'||bt.indexOf('Download (')!==-1)&&bt.indexOf('Already')===-1&&bt.indexOf('%')===-1){" +
                            "var badge=btn.querySelector('.wta-size-badge');" +
                            "if(!badge&&bt.indexOf('(')===-1){" +
                                "var span=document.createElement('span');" +
                                "span.className='wta-size-badge opacity-90 font-semibold tabular-nums ml-1.5 px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-300 text-xs';" +
                                "span.textContent='('+labelText+')';" +
                                "btn.appendChild(span);" +
                            "}else if(badge){" +
                                "badge.textContent='('+labelText+')';" +
                            "}" +
                        "}" +
                    "}" +
                    "var modals=document.querySelectorAll('[role=\"dialog\"],[data-book-modal],.modal-content,[data-download-modal],[data-book-download]');" +
                    "for(var mi=0;mi<modals.length;mi++){" +
                        "var mEl=modals[mi];" +
                        "var mText=(mEl.textContent||'');" +
                        "if(mText.indexOf('Download')===-1&&mText.indexOf('PDF')===-1&&mText.indexOf('Book')===-1)continue;" +
                        "if(mText.indexOf('STREAK')!==-1||mText.indexOf('Streak')!==-1||mText.indexOf('EXAM')!==-1||mText.indexOf('Exam')!==-1)continue;" +
                        "var leafEls=mEl.querySelectorAll('span,p,div');" +
                        "for(var li=0;li<leafEls.length;li++){" +
                            "var leaf=leafEls[li];" +
                            "if(leaf.children.length>0)continue;" +
                            "var lt=(leaf.textContent||'').trim();" +
                            "if(lt.indexOf('Checking size')!==-1){" +
                                "leaf.textContent=lt.replace(/Checking size[….]*/g,labelText);" +
                            "}else if(lt==='· Size unknown'||lt==='Size unknown'){" +
                                "leaf.textContent='· '+labelText;" +
                            "}else if(lt==='Ready to download'){" +
                                "leaf.textContent=labelText;" +
                            "}" +
                        "}" +
                    "}" +
                "}" +
            "}catch(e){}" +
        "}" +
        "updatePdfPreOpenLabels();" +
        "if(!window.__wta_size_interval){" +
            "window.__wta_size_interval=setInterval(updatePdfPreOpenLabels,400);" +
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
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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

    // Loading states for zero blank screen and lively navigation feedback
    var isInitialLoading by remember { mutableStateOf(true) }
    var minSplashElapsed by remember { mutableStateOf(false) }
    var pageRendered by remember { mutableStateOf(false) }
    var webProgress by remember { mutableIntStateOf(0) }
    var isNavigating by remember { mutableStateOf(false) }
    var navTimeoutRunnable by remember { mutableStateOf<Runnable?>(null) }

    val startNavigationLoading: () -> Unit = {
        isNavigating = true
        navTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        val r = Runnable { isNavigating = false }
        navTimeoutRunnable = r
        mainHandler.postDelayed(r, 3800L)
    }
    val stopNavigationLoading: () -> Unit = {
        isNavigating = false
        navTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
    }

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
            wv.evaluateJavascript("(function(){return window.location.pathname||'';})();") { rawPath ->
                val p = rawPath?.trim('"')?.trim() ?: ""
                if (p.isNotBlank() && p != "null") {
                    selectedIndex = tabIndexForUrl(p, selectedIndex)
                }
            }
            if (!isOnline(context)) return@LifecycleEventObserver
            val currentUrl = wv.url
            if (currentUrl == null || currentUrl.startsWith("file://")) return@LifecycleEventObserver
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

    fun showOffline(wv: WebView) {
        isInitialLoading = false
        stopNavigationLoading()
        wv.loadUrl(OFFLINE_ASSET)
    }

    fun navigateTo(url: String, tabIndex: Int? = null, resetHistory: Boolean = false) {
        val wv = webView ?: return
        if (tabIndex != null) selectedIndex = tabIndex
        if (resetHistory) pendingClearHistory = true
        startNavigationLoading()
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
                            isLongClickable = false
                            setOnLongClickListener {
                                val hit = hitTestResult
                                hit?.type == WebView.HitTestResult.EDIT_TEXT_TYPE
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
                                        startNavigationLoading()
                                    }
                                }

                                @JavascriptInterface
                                fun notifyLoadingFinished() {
                                    mainHandler.post {
                                        stopNavigationLoading()
                                    }
                                }
                            }, "AndroidOfflineVault")

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    webProgress = newProgress
                                    if (newProgress >= 100) {
                                        stopNavigationLoading()
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
                                    if (url != null && !url.startsWith("file://")) {
                                        selectedIndex = tabIndexForUrl(url, selectedIndex)
                                    }
                                    if (url != null && url.startsWith("file:///android_asset/")) {
                                        isInitialLoading = false
                                        stopNavigationLoading()
                                    }
                                    view?.evaluateJavascript(NATIVE_CHROME_JS, null)
                                    view?.evaluateJavascript(BOOK_PAGE_HELPERS_JS, null)
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
                                    view?.evaluateJavascript(DETECT_AND_RECOVER_JS, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    val curUrl = url ?: view?.url
                                    if (curUrl != null && !curUrl.startsWith("file://")) {
                                        selectedIndex = tabIndexForUrl(curUrl, selectedIndex)
                                    }
                                    pageRendered = true
                                    if (minSplashElapsed) {
                                        isInitialLoading = false
                                    }
                                    stopNavigationLoading()
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

                // Centered Big Circular Loader for all page transitions and loading waits
                AnimatedVisibility(
                    visible = isNavigating || (webProgress in 1..95 && !isInitialLoading),
                    enter = fadeIn(tween(160)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                ) {
                    CenteredBigCircularLoader(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66030712)),
                        statusText = "Wisdom Tower Academy",
                        subText = "Loading…",
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
            // Compact usual loader on a subtle frosted card with ambient backdrop
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xF20B1528))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x4400E5FF),
                                Color(0x2238BDF8),
                                Color(0x18818CF8)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 26.dp, vertical = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FuturisticGearRings(
                        size = 86.dp,
                        brandSize = 46.dp,
                        numTicks = 14,
                        tickInnerRatio = 0.68f,
                        tickOuterRatio = 0.88f,
                        tickWidth = 3.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = subText,
                        color = Muted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal
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
 * Advanced, high-tech bottom navigation bar.
 * Clean, precise icon + typography tinting, ZERO clutter dots.
 * Icons are strictly contained within their capsule bounds with non-bouncy micro-press damping
 * to guarantee icons NEVER go out of their box on hover, click, or tap.
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
        color = Color(0xF2070E1B),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(66.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
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
                                if (selectedIndex != index) {
                                    onItemSelected(index, item)
                                }
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
