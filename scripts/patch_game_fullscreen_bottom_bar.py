#!/usr/bin/env python3
"""One-line MainActivity change: hide bottom nav while GameFullScreen.active (plan section 9)."""
from pathlib import Path

path = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
text = path.read_text(encoding="utf-8")
old = """            bottomBar = {
                if (!isKeyboardVisible) {
                    AliveBottomNav("""
new = """            bottomBar = {
                if (!isKeyboardVisible && !com.wisdomtower.academy.ui.games.common.GameFullScreen.active) {
                    AliveBottomNav("""
if old not in text:
    raise SystemExit("pattern not found — MainActivity may already be patched or layout changed")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("patched MainActivity bottomBar for GameFullScreen.active")
