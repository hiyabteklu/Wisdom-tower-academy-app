#!/usr/bin/env python3
"""Fix U2 resume update-check coroutine scope: lifecycleScope is an imported LifecycleOwner extension."""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "WTA_U2_LIFECYCLE_SCOPE_FIX"

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do")
    sys.exit(0)

edits = [
    (
        "import androidx.lifecycle.LifecycleEventObserver\n",
        "import androidx.lifecycle.LifecycleEventObserver\n"
        "import androidx.lifecycle.lifecycleScope\n"
    ),
    (
        "import kotlinx.coroutines.delay\n",
        "import kotlinx.coroutines.delay\n"
        "import kotlinx.coroutines.launch\n"
    ),
    (
        "androidx.lifecycle.lifecycleScope.launch { checkUpdate() }",
        "updateLifecycleOwner.lifecycleScope.launch { checkUpdate() } // " + MARK
    ),
]

for anchor, replacement in edits:
    count = src.count(anchor)
    if count != 1:
        print(f"ERROR: anchor found {count} times (need exactly 1); file not changed", file=sys.stderr)
        sys.exit(1)

for anchor, replacement in edits:
    src = src.replace(anchor, replacement, 1)

FILE.write_text(src, encoding="utf-8")
print("U2 lifecycleScope fix applied")
