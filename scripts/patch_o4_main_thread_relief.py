#!/usr/bin/env python3
"""O4 (docs/ONBOARDING_STARTUP_PLAN.md): main-thread relief after every page load in MainActivity.kt.

Owner bug (2026-10-23): right after opening, scrolling or switching tabs quickly freezes the app once.

Cause (verified in code, hypothesis H1 in the plan): the main WebView's `onPageFinished` reads the whole page HTML
(`document.documentElement.outerHTML`) and the result callback runs on the UI thread: it parses the full text with
`JSONTokener` and calls `WebCacheVault.saveHtmlPage`, which writes the page to disk under up to four keys. Then 7 scripts
are injected and 4 of them again after 800 ms. Quick tab switching repeats all of it for every page.

What this patch does (2 edits, MainActivity.kt only):
1. The parse, the checks and `saveHtmlPage` now run on `WebCacheVault.io` (the vault's own 2-thread pool), not on the UI
   thread. Same conditions, same call, same result on disk; only the thread changes. (`save` and `index` are already
   safe for this: the index is a ConcurrentHashMap and `save` already hands `persistIndex` to the same pool.)
2. The second 800 ms injection round is skipped when the page changed in the meantime (path differs from the page that
   just finished). Before it re-ran its four scripts on whatever page was showing.

Not touched: the first injection round, the loader, anything else. Safe to run twice (marker check). Every anchor must
match exactly once, otherwise nothing is written.
"""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "O4: main-thread relief"

EDITS = []


def edit(anchor, replacement):
    EDITS.append((anchor, replacement))


I = " " * 52  # indentation of the `try {` line

# 1. parse + save off the main thread ----------------------------------------------------------------------------------
OLD1 = (
    I + "try {\n"
    + I + "    val rawHtml = org.json.JSONTokener(htmlJson).nextValue() as? String ?: \"\"\n"
    + I + "    val saveUrl = curUrl ?: u\n"
    + I + "    if (rawHtml.length > 500 &&\n"
    + I + "        saveUrl.isNotBlank() &&\n"
    + I + "        !WebCacheVault.isDynamicListOrDataApi(saveUrl) &&\n"
    + I + "        !WebCacheVault.isDegradedOrEmptyHtml(rawHtml) &&\n"
    + I + "        (rawHtml.contains(\"<html\") || rawHtml.contains(\"<body\") || rawHtml.contains(\"<!DOCTYPE\"))\n"
    + I + "    ) {\n"
    + I + "        WebCacheVault.saveHtmlPage(ctx, saveUrl, rawHtml)\n"
    + I + "    }\n"
    + I + "} catch (_: Exception) {}\n"
)
NEW1 = (
    I + "// O4: main-thread relief. Parsing the whole page text and writing it to disk now run on the vault's own threads.\n"
    + I + "WebCacheVault.io.execute {\n"
    + I + "try {\n"
    + I + "    val rawHtml = org.json.JSONTokener(htmlJson).nextValue() as? String ?: \"\"\n"
    + I + "    val saveUrl = curUrl ?: u\n"
    + I + "    if (rawHtml.length > 500 &&\n"
    + I + "        saveUrl.isNotBlank() &&\n"
    + I + "        !WebCacheVault.isDynamicListOrDataApi(saveUrl) &&\n"
    + I + "        !WebCacheVault.isDegradedOrEmptyHtml(rawHtml) &&\n"
    + I + "        (rawHtml.contains(\"<html\") || rawHtml.contains(\"<body\") || rawHtml.contains(\"<!DOCTYPE\"))\n"
    + I + "    ) {\n"
    + I + "        WebCacheVault.saveHtmlPage(ctx, saveUrl, rawHtml)\n"
    + I + "    }\n"
    + I + "} catch (_: Exception) {}\n"
    + I + "}\n"
)
edit(OLD1, NEW1)

# 2. skip the second injection round when the page changed ------------------------------------------------------------
J = " " * 40
edit(
    J + "mainHandler.postDelayed({\n"
    + J + "    val cur = wv.url ?: \"\"\n"
    + J + "    if (!cur.startsWith(\"file://\")) {\n"
    + J + "        wv.evaluateJavascript(AI_TUTOR_CHROME_JS, null)\n"
    + J + "        wv.evaluateJavascript(DETECT_AND_RECOVER_JS, null)\n",
    J + "mainHandler.postDelayed({\n"
    + J + "    val cur = wv.url ?: \"\"\n"
    + J + "    // O4: main-thread relief. Skip this second round when the page changed in the meantime.\n"
    + J + "    if (!cur.startsWith(\"file://\") && cur.substringBefore('#') == u.substringBefore('#')) {\n"
    + J + "        wv.evaluateJavascript(AI_TUTOR_CHROME_JS, null)\n"
    + J + "        wv.evaluateJavascript(DETECT_AND_RECOVER_JS, null)\n",
)

# ---------------------------------------------------------------------------------------------------------------------
src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do")
    sys.exit(0)

out = src
for i, (anchor, replacement) in enumerate(EDITS):
    n = out.count(anchor)
    if n != 1:
        print(f"ERROR: edit #{i} anchor found {n} times (need exactly 1); file not changed")
        print(anchor[:200])
        sys.exit(1)
    out = out.replace(anchor, replacement)

FILE.write_text(out, encoding="utf-8")
print(f"patched ({len(EDITS)} edits)")
