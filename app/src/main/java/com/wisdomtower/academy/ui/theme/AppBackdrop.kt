package com.wisdomtower.academy.ui.theme

import android.content.Context
import android.webkit.WebView
import androidx.compose.ui.graphics.Color

/**
 * The app background as the rest of the code sees it (plan B2 of docs/PLAN_SOLID_BACKGROUND_OPTIONS.md, reworked
 * 2026-10-17: the 5 options are now blue, black and three soft space looks, see [AppBackgroundOption]).
 *
 * Native screens draw it through [AtmosphereBackground]. Every local-page WebView (notes, flashcards, quiz, question
 * banks, competition exam and review, AI Tutor) calls [webViewColor] and [clearPageBackground].
 *
 * Slice BG1 (2026-10-17): the WebView and its page are TRANSPARENT, the chosen picture is drawn natively behind it.
 * Slice BG-WEB (2026-10-17, owner: "make the background literally everywhere", ONE place for all web pages):
 * [clearPageBackground] also installs a small script that turns every dark navy panel, card, bar, button and strip
 * of the page into see-through glass (see [GLASS_JS]), so no page file needs its own edit and new pages are covered
 * automatically. Accent colours (green, orange, cyan, red) and inline highlights are left alone.
 *
 * Slice BG-BLINK (2026-10-17, owner: "it blinks the solid color on every click"): hypothesis for the cause, two parts.
 * (a) The script re-ran 120 ms AFTER a class change, so a new or changed panel was painted solid for that time.
 * (b) A page with a CSS background transition: after the old glass was lifted to measure the real colour, the
 * measured value was the start of a transition (so nothing was recognised) and, after glass was set again, the
 * panel faded from solid to glass. Fix: the script now runs INSIDE the mutation callback (before the next paint) and
 * finishes background-colour / background-image transitions straight away, before measuring and after applying.
 * Glass strength raised from 5 to 8 percent (owner: glass is too faint on the textured background).
 *
 * Slice BG-LOADING (2026-10-17, owner: "in loading it's black, especially on notes, exams, flashcards"): the page's own
 * dark colour showed from the start of loading until the glass script ran. [hidePageUntilReady] (called from
 * onPageStarted by scripts/patch_backdrop_loading.py) makes the WebView invisible, so the native backdrop shows, and
 * [clearPageBackground] fades it in once the glass script has run (safety reveal after 1.5 s).
 *
 * Slice BG-POPUP (2026-10-17, owner: "pop ups overlap the text behind, dim the background, raise glass"): cause found.
 * The full-screen dark backdrop of a pop-up (for example `.ovl` in competition_exam.html, rgba(2,6,23,.82)) was treated
 * like a page wrapper and made transparent, so nothing dimmed the page, and the pop-up box itself became 8 percent
 * glass, so the text behind showed through it. Now a full-screen FIXED layer that is partly see-through is a scrim: it
 * becomes a dim, and every dark panel inside it becomes a navy panel. Normal glass strength raised from 8 to 16 percent.
 * BG-POPUP-2 (owner: still not readable): scrim 55 to 65 percent black, panel 82 to 96 percent navy (readable first).
 * To go back: glass 0.08, remove the scrim / panel branches in the script.
 *
 * D8a (docs/DAY_MODE_PLAN.md, 2026-10-19): DAY variant, [GLASS_JS_DAY]. The local pages are written for a dark page
 * (light text, navy panels). By day [clearPageBackground] installs the day script instead of [GLASS_JS]; NIGHT RUNS THE
 * OLD SCRIPT UNCHANGED. The day script does the same navy-to-glass job with the Ivory & Navy values (panel = ivory 84
 * percent, pop-up panel = ivory 97 percent, scrim = navy 50 percent, inline dark chips = a 8 percent navy tint) and in
 * addition:
 *  - TEXT: every element sitting on the page or on a dark panel gets a day text colour (almost white = deep navy ink,
 *    light grey-blue = muted 4A5A70, light colours such as cyan or green = the same hue at 27 percent lightness).
 *    Text on a coloured, non-navy fill (a green button, a cyan chip) is left alone, so white on green stays white.
 *  - BORDERS: whitish hairlines on those elements become a navy 16 percent line.
 * It also keeps the original inline values and restores them before every pass, like the night script.
 * D8b (2026-10-19), after reading quiz.html, tutor.html and richtext.css against the script:
 *  - a dark fill counts from 35 percent opacity (the night script needs 60): the exam timer (black 50 percent) and the
 *    crossed-out choice (black 40 percent) were treated as coloured fills, so their light text would have stayed light;
 *  - the script runs on every change, like the night script (a first version waited up to 150 ms, which would have shown
 *    new tutor text in its light night colour for a moment while an answer streams);
 *  - it adds the class `wt-day` to the page's html element, so `richtext.css` can give day backgrounds (code chips, table
 *    heads, highlights, the result gauge track);
 *  - the insides of KaTeX formulas are skipped (the formula root gets the colour and the parts inherit it), and the size
 *    limit is 15000 elements, because a maths page can have thousands of nodes.
 * NOT done (hypotheses, D8c onwards): SVG fill and stroke attributes other than the gauge track, gradient-clip text
 * (transparent text colour), `flashcards.html`, `index.html` and the `competition*.html` pages were not read, and the
 * glass strength by day. Not compiled, not seen on a phone. Perf risk: on a long chat the script walks every element
 * on every streamed update, like the night script does.
 */
object AppBackdrop {
    /** The plain page colour of the chosen option. Reading it in a composable follows the choice live. */
    val color: Color
        get() = AppBackground.selected.color

    // One style element per page; calling it again updates the same element, and the important flag beats the page's own rule.
    private fun clearPageJs(css: String): String =
        "(function(){var d=document;var s=d.getElementById('wt-appbg');" +
            "if(!s){s=d.createElement('style');s.id='wt-appbg';(d.head||d.documentElement).appendChild(s);}" +
            "s.textContent='" + css + "';})();"

    /**
     * Turns every OPAQUE DARK-BLUE background (computed colour, blue is the strongest channel and nothing above 110,
     * so black, navy and ink-blue; not green, red, orange or cyan) of a block element into white 16 percent glass, and a
     * navy-only gradient into the same glass. A full-screen wrapper becomes transparent instead, EXCEPT a full-screen
     * fixed layer that is partly see-through (a pop-up backdrop): that becomes a dim, and the dark panels inside it
     * become near-solid navy so their text is readable. Inline elements
     * (highlights, code chips) are skipped. It re-runs (inside the mutation callback, so before the next paint)
     * whenever a class changes or nodes are added, so selected / correct / wrong states keep working: the old inline
     * value is restored before every pass. Background transitions are finished at once so no solid colour fades in.
     * Light (day) pages have no dark-blue fills, so nothing changes there.
     * No dollar sign and no triple quote may appear in this script (Kotlin raw string).
     */
    private const val GLASS_JS = """
(function(){
 if(window.__wtGlass){window.__wtGlass.run();return;}
 var KEY='data-wtg',GLASS='rgba(255,255,255,0.16)',SCRIM='rgba(0,0,0,0.65)',PANEL='rgba(12,20,36,0.96)',timer=null,busy=false;
 function parse(s){var m=s&&s.match(/rgba?\(([^)]+)\)/);if(!m)return null;
  var p=m[1].split(/[ ,\/]+/).filter(Boolean).map(parseFloat);
  return {r:p[0],g:p[1],b:p[2],a:(p.length>3&&!isNaN(p[3]))?p[3]:1};}
 function navy(c){return !!c&&c.a>=0.6&&c.b>=c.g&&c.b>=c.r&&Math.max(c.r,c.g,c.b)<=110;}
 function settle(){try{var a=document.getAnimations(),i,t;
  for(i=0;i<a.length;i++){t=a[i].transitionProperty;if(t&&t.indexOf('background')===0)a[i].finish();}}catch(err){}}
 function restore(e){var o=e.__wtOrig;
  if(o){e.style.removeProperty('background-color');e.style.removeProperty('background-image');
   if(o.c)e.style.setProperty('background-color',o.c,o.cp);
   if(o.i)e.style.setProperty('background-image',o.i,o.ip);}
  e.removeAttribute(KEY);e.__wtOrig=null;}
 function run(){
  if(busy||!document.body)return;busy=true;
  try{
   var els=document.body.querySelectorAll('*');
   if(els.length>4000){busy=false;return;}
   var i,j,e,cs,bc,bi,r,jobs=[],scrims=[];
   for(i=0;i<els.length;i++){if(els[i].hasAttribute(KEY))restore(els[i]);}
   settle();
   var W=window.innerWidth,H=window.innerHeight;
   for(i=0;i<els.length;i++){
    e=els[i];var tg=e.tagName;
    if(tg==='SCRIPT'||tg==='STYLE'||tg==='IMG'||tg==='CANVAS'||tg==='VIDEO')continue;
    cs=getComputedStyle(e);
    if(cs.display==='inline'||cs.display==='none')continue;
    bc=parse(cs.backgroundColor);bi=cs.backgroundImage;var hit=0;
    if(navy(bc))hit=1;
    else if(bi&&bi.indexOf('gradient(')>-1){
     var cols=bi.match(/rgba?\([^)]+\)/g)||[],ok=cols.length>0,k;
     for(k=0;k<cols.length;k++){if(!navy(parse(cols[k]))){ok=false;break;}}
     if(ok)hit=2;}
    if(!hit)continue;
    r=e.getBoundingClientRect();
    var full=(r.width*r.height>0.6*W*H);
    var scrim=full&&hit===1&&cs.position==='fixed'&&!!bc&&bc.a<1;
    if(scrim)scrims.push(e);
    jobs.push([e,hit,full,scrim]);
   }
   for(i=0;i<jobs.length;i++){
    e=jobs[i][0];
    var inScrim=false;
    if(!jobs[i][3]){for(j=0;j<scrims.length;j++){if(scrims[j]!==e&&scrims[j].contains(e)){inScrim=true;break;}}}
    e.__wtOrig={c:e.style.getPropertyValue('background-color'),cp:e.style.getPropertyPriority('background-color'),
                i:e.style.getPropertyValue('background-image'),ip:e.style.getPropertyPriority('background-image')};
    var col=jobs[i][3]?SCRIM:(jobs[i][2]?'transparent':(inScrim?PANEL:GLASS));
    e.style.setProperty('background-color',col,'important');
    if(jobs[i][1]===2||jobs[i][2])e.style.setProperty('background-image','none','important');
    e.setAttribute(KEY,'1');
   }
   settle();
  }catch(err){}
  busy=false;}
 function sched(){run();clearTimeout(timer);timer=setTimeout(run,150);}
 try{new MutationObserver(sched).observe(document.documentElement,
  {childList:true,subtree:true,attributes:true,attributeFilter:['class']});}catch(err){}
 window.__wtGlass={run:run};
 run();
})();
"""

    /**
     * D8a / D8b: the DAY script (see the class comment). Same structure as [GLASS_JS], with ivory glass and a text / border
     * remap for pages that were written for a dark page. No dollar sign and no triple quote may appear (Kotlin raw string).
     */
    private const val GLASS_JS_DAY = """
(function(){
 if(window.__wtGlassDay){window.__wtGlassDay.run();return;}
 try{document.documentElement.classList.add('wt-day');}catch(err){}
 var KEY='data-wtg',GLASS='rgba(248,245,238,0.84)',SCRIM='rgba(11,27,51,0.5)',PANEL='rgba(248,245,238,0.97)',
  CHIP='rgba(11,27,51,0.08)',INK='11,27,51',MUTED='rgb(74,90,112)',LINE='rgba(11,27,51,0.16)',
  PROPS=['background-color','background-image','color','border-color'],timer=null,busy=false;
 function parse(s){var m=s&&s.match(/rgba?\(([^)]+)\)/);if(!m)return null;
  var p=m[1].split(/[ ,\/]+/).filter(Boolean).map(parseFloat);
  return {r:p[0],g:p[1],b:p[2],a:(p.length>3&&!isNaN(p[3]))?p[3]:1};}
 function navy(c){return !!c&&c.a>=0.35&&c.b>=c.g&&c.b>=c.r&&Math.max(c.r,c.g,c.b)<=110;}
 function hsl(c){var r=c.r/255,g=c.g/255,b=c.b/255,mx=Math.max(r,g,b),mn=Math.min(r,g,b),l=(mx+mn)/2,h=0,s=0,d=mx-mn;
  if(d>0){s=l>0.5?d/(2-mx-mn):d/(mx+mn);
   if(mx===r)h=(g-b)/d+(g<b?6:0);else if(mx===g)h=(b-r)/d+2;else h=(r-g)/d+4;h*=60;}
  return {h:h,s:s,l:l};}
 function rgbOf(h,s,l){var c=(1-Math.abs(2*l-1))*s,x=c*(1-Math.abs((h/60)%2-1)),m=l-c/2,r=0,g=0,b=0;
  if(h<60){r=c;g=x;}else if(h<120){r=x;g=c;}else if(h<180){g=c;b=x;}else if(h<240){g=x;b=c;}else if(h<300){r=x;b=c;}else{r=c;b=x;}
  return 'rgb('+Math.round((r+m)*255)+','+Math.round((g+m)*255)+','+Math.round((b+m)*255)+')';}
 function dayText(c){
  if(!c||c.a<0.05)return null;var v=hsl(c);
  if(v.s<0.3){if(v.l>0.85)return 'rgba('+INK+','+Math.max(c.a,0.75)+')';if(v.l>0.5)return MUTED;return null;}
  if(v.l>0.35)return rgbOf(v.h,Math.min(v.s,0.85),0.27);
  return null;}
 function settle(){try{var a=document.getAnimations(),i,t;
  for(i=0;i<a.length;i++){t=a[i].transitionProperty;if(t&&t.indexOf('background')===0)a[i].finish();}}catch(err){}}
 function save(e){if(e.__wtOrig)return;var o={};
  PROPS.forEach(function(p){o[p]=[e.style.getPropertyValue(p),e.style.getPropertyPriority(p)];});e.__wtOrig=o;}
 function restore(e){var o=e.__wtOrig;
  if(o){PROPS.forEach(function(p){e.style.removeProperty(p);var v=o[p];if(v&&v[0])e.style.setProperty(p,v[0],v[1]);});}
  e.removeAttribute(KEY);e.__wtOrig=null;}
 function run(){
  if(busy||!document.body)return;busy=true;
  try{
   var els=document.body.querySelectorAll('*');
   if(els.length>15000){busy=false;return;}
   var i,j,e,a,cs,bc,bi,r,recs=[],scrims=[],kind=new Map();
   for(i=0;i<els.length;i++){if(els[i].hasAttribute(KEY))restore(els[i]);}
   settle();
   var W=window.innerWidth,H=window.innerHeight;
   for(i=0;i<els.length;i++){
    e=els[i];var tg=e.tagName;
    if(tg==='SCRIPT'||tg==='STYLE'||tg==='IMG'||tg==='CANVAS'||tg==='VIDEO')continue;
    if(e.parentElement&&e.parentElement.closest&&e.parentElement.closest('.katex'))continue;
    cs=getComputedStyle(e);
    if(cs.display==='none')continue;
    var inl=cs.display==='inline';
    bc=parse(cs.backgroundColor);bi=cs.backgroundImage;
    var hit=0,own='none',full=false,scrim=false;
    if(navy(bc)){hit=inl?3:1;own='dark';}
    else{
     var gr=!!bi&&bi.indexOf('gradient(')>-1;
     if(gr){var cols=bi.match(/rgba?\([^)]+\)/g)||[],ok=cols.length>0,k;
      for(k=0;k<cols.length;k++){if(!navy(parse(cols[k]))){ok=false;break;}}
      if(ok){hit=inl?3:2;own='dark';}else if(cols.length>0){own='accent';}}
     else if(bc&&bc.a>=0.5){own='accent';}
    }
    if(hit&&hit!==3){
     r=e.getBoundingClientRect();
     full=(r.width*r.height>0.6*W*H);
     scrim=full&&hit===1&&cs.position==='fixed'&&!!bc&&bc.a<1;
     if(scrim)scrims.push(e);
    }
    kind.set(e,own);
    var eff='dark';
    for(a=e;a&&a!==document.documentElement;a=a.parentElement){
     var kk=kind.get(a);
     if(kk==='accent'){eff='accent';break;}
     if(kk==='dark'){break;}
    }
    var col=null,bd=null;
    if(eff==='dark'){
     col=dayText(parse(cs.color));
     if((parseFloat(cs.borderTopWidth)||0)>0){var bk=parse(cs.borderTopColor);
      if(bk&&bk.a>0){var bh=hsl(bk);if(bh.l>0.7&&bh.s<0.3)bd=LINE;}}
    }
    if(hit||col||bd)recs.push({e:e,hit:hit,full:full,scrim:scrim,col:col,bd:bd});
   }
   for(i=0;i<recs.length;i++){
    var q=recs[i];e=q.e;
    save(e);
    if(q.hit){
     var inScrim=false;
     if(!q.scrim){for(j=0;j<scrims.length;j++){if(scrims[j]!==e&&scrims[j].contains(e)){inScrim=true;break;}}}
     var bgc=q.scrim?SCRIM:(q.hit===3?CHIP:(q.full?'transparent':(inScrim?PANEL:GLASS)));
     e.style.setProperty('background-color',bgc,'important');
     if(q.hit===2||q.full)e.style.setProperty('background-image','none','important');
    }
    if(q.col)e.style.setProperty('color',q.col,'important');
    if(q.bd)e.style.setProperty('border-color',q.bd,'important');
    e.setAttribute(KEY,'1');
   }
   settle();
  }catch(err){}
  busy=false;}
 function sched(){run();clearTimeout(timer);timer=setTimeout(run,150);}
 try{new MutationObserver(sched).observe(document.documentElement,
  {childList:true,subtree:true,attributes:true,attributeFilter:['class']});}catch(err){}
 window.__wtGlassDay={run:run};
 run();
})();
"""

    /** Background colour for a WebView that shows a local page: transparent, the native backdrop shows through. */
    @Suppress("UNUSED_PARAMETER")
    fun webViewColor(context: Context): Int = android.graphics.Color.TRANSPARENT

    /**
     * BG-LOADING: call from onPageStarted of a local-page WebView. The page is invisible (the native backdrop shows)
     * until [clearPageBackground] has run the glass script; a safety reveal after 1.5 s covers a page that never finishes.
     */
    fun hidePageUntilReady(view: WebView) {
        view.animate().cancel()
        view.alpha = 0f
        view.postDelayed({ view.animate().alpha(1f).setDuration(140).start() }, 1500)
    }

    /**
     * Call from onPageFinished of a local-page WebView: makes the page itself transparent and turns its dark-blue
     * panels into glass, then fades the page in (see [hidePageUntilReady]). Every caller must have
     * [AtmosphereBackground] behind its WebView (checked 2026-10-17: Notes, Flashcards, Quiz, Competition exam and
     * review, AI Tutor all have it).
     * D8a: night runs [GLASS_JS] exactly as before; day runs [GLASS_JS_DAY]. The mode is read when the page finishes
     * loading (a page opened in one mode keeps its look until it is opened again).
     */
    fun clearPageBackground(view: WebView) {
        val css = "html{background:transparent !important}body{background:transparent !important}"
        view.evaluateJavascript(clearPageJs(css), null)
        val script = if (AppThemeMode.isDark) GLASS_JS else GLASS_JS_DAY
        view.evaluateJavascript(script) {
            view.animate().cancel()
            view.animate().alpha(1f).setDuration(140).start()
        }
    }
}
