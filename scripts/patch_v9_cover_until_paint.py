#!/usr/bin/env python3
"""V9 (docs/IOS_MOTION_V2_PLAN.md): keep the native loader over a web page until the page has really painted.

Cause (code read of MainActivity.kt, 2026-11-03):
- The main WebView and the tool-overlay WebView are see-through (transparent background). Their loader was removed when the
  load progress reached 85 percent (`onProgressChanged`), which can happen BEFORE the first frame of the page is drawn. For
  that moment the student sees the bare app background: the blank / dark flash.
- The real "first frame is visible" signal is `WebViewClient.onPageCommitVisible`. The main WebView already releases the
  loader there (and in onPageFinished, onPageReady, onRouteChanged, plus the hard 3.5 s and 4 s timeouts). The tool overlay
  had NO onPageCommitVisible at all.

Change (3 edits, nothing else):
1. Main WebView: the 85 percent rule now only releases the loader for the offline asset page (file://); web pages wait for
   onPageCommitVisible / onPageFinished as before.
2. Tool overlay: same rule, offline page only.
3. Tool overlay: new onPageCommitVisible that releases its loader for web pages.
The start-up cover (pageRendered at 60 percent), the fail-safe timeouts and all page logic are untouched.
"""
import subprocess
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "V9 cover until paint"

EDITS = [
    (
        # 1. main WebView
        "                                    webProgress = newProgress\n"
        "                                    if (newProgress >= 85) {\n"
        "                                        stopNavigationLoading()\n"
        "                                    }\n",
        "                                    webProgress = newProgress\n"
        "                                    // V9 cover until paint: a web page keeps the loader until onPageCommitVisible (first drawn frame);\n"
        "                                    // only the offline asset page is released by progress.\n"
        "                                    if (newProgress >= 85 && view?.url.orEmpty().startsWith(\"file://\")) {\n"
        "                                        stopNavigationLoading()\n"
        "                                    }\n",
    ),
    (
        # 2. tool overlay progress rule
        "                                            if (newProgress >= 85) {\n"
        "                                                activeToolOverlayLoading = false\n"
        "                                            }\n",
        "                                            // V9 cover until paint: the overlay loader waits for onPageCommitVisible; progress releases only the offline page\n"
        "                                            if (newProgress >= 85 && isOfflinePage) {\n"
        "                                                activeToolOverlayLoading = false\n"
        "                                            }\n",
    ),
    (
        # 3. tool overlay: first drawn frame releases the loader (new override, placed before the overlay's onPageFinished)
        "                                        override fun onPageFinished(view: WebView?, url: String?) {\n"
        "                                            val cur = url.orEmpty()\n",
        "                                        override fun onPageCommitVisible(view: WebView?, url: String?) {\n"
        "                                            // V9 cover until paint: the first frame of the page is on screen, remove the loader\n"
        "                                            activeToolOverlayLoading = false\n"
        "                                        }\n"
        "\n"
        "                                        override fun onPageFinished(view: WebView?, url: String?) {\n"
        "                                            val cur = url.orEmpty()\n",
    ),
]

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do")
    sys.exit(0)

out = src
for i, (anchor, replacement) in enumerate(EDITS):
    n = out.count(anchor)
    if n != 1:
        print(f"ERROR: edit #{i + 1} anchor found {n} times (need exactly 1); file not changed")
        print(anchor[:200])
        sys.exit(1)
    out = out.replace(anchor, replacement)

FILE.write_text(out, encoding="utf-8")

# keep the packed copy of MainActivity.kt in step (HANDOFF rule 10); a failure here does not undo the patch
pack = Path("scripts/pack_mainactivity.py")
if pack.exists():
    r = subprocess.run([sys.executable, str(pack)], capture_output=True, text=True)
    print("pack_mainactivity.py:", "ok" if r.returncode == 0 else "FAILED (patch itself is applied): " + r.stderr[-200:])
print(f"patched ({len(EDITS)} edits)")
