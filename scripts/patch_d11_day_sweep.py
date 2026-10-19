#!/usr/bin/env python3
"""D11 (docs/DAY_MODE_PLAN.md): day-mode sweep of five files the D3 to D7 slices never covered.

  ui/guides/FreeResourcesStateCard.kt        title white, message CBD5E1  -> WisdomInk / WisdomMuted by day
  ui/settings/AccountDeletionSection.kt      dialog was dark with white text -> ivory dialog, ink text by day
  ui/drawer/DrawerScreens.kt                 About, Contact, FAQ, Privacy, Terms: Color.White text and tints -> WisdomInk,
                                             Color.Black on the cyan button -> WisdomOnInk, dropdown menu fill and red error
                                             text get day values
  ui/notifications/NotificationsScreen.kt    skeleton bar tint white 10 percent -> WisdomInk
  ui/notifications/NotificationReaderScreen.kt  bold run in a notification body was white -> WisdomInk

Night values stay exactly as they were (WisdomInk is white and WisdomOnInk black at night; literals are kept in
isDark branches). Left on purpose: the unread-count badge text in NotificationsScreen.kt (background not verified).
Safe to run twice. If any anchor is missing nothing is written. Standard library only.
"""
import re
import sys
from pathlib import Path

ROOT = Path("app/src/main/java/com/wisdomtower/academy/ui")
THEME = "com.wisdomtower.academy.ui.theme."


def ensure_import(src, name):
    line = f"import {THEME}{name}\n"
    if line in src:
        return src
    i = src.index("\nimport ") + 1
    return src[:i] + line + src[i:]


def rep(src, old, new, minimum=1, exact=None, label=""):
    n = src.count(old)
    if (exact is not None and n != exact) or (exact is None and n < minimum):
        print(f"ERROR {label}: {old!r} found {n} times; nothing written")
        sys.exit(1)
    return src.replace(old, new)


def sweep_white_black(src, label):
    src = rep(src, "unfocusedTextColor = Color.White", "unfocusedTextColor = WisdomInk", 0, label=label)  # before focused
    src = rep(src, "focusedTextColor = Color.White", "focusedTextColor = WisdomInk", 0, label=label)
    src = rep(src, "color = Color.White", "color = WisdomInk", 0, label=label)
    src = rep(src, "tint = Color.White", "tint = WisdomInk", 0, label=label)
    src = rep(src, "Color.White.copy(", "WisdomInk.copy(", 0, label=label)
    src = rep(src, "color = Color.Black", "color = WisdomOnInk", 0, label=label)
    src = rep(src, "tint = Color.Black", "tint = WisdomOnInk", 0, label=label)
    return src


def patch_state_card(src):
    src = rep(src, "color = Color.White\n", "color = WisdomInk\n", exact=1, label="state card title")
    src = rep(src, "color = Color(0xFFCBD5E1)\n",
              "color = if (AppThemeMode.isDark) Color(0xFFCBD5E1) else WisdomMuted\n", exact=1, label="state card msg")
    for n in ("AppThemeMode", "WisdomInk", "WisdomMuted"):
        src = ensure_import(src, n)
    return src


def patch_deletion(src):
    src = rep(src, "containerColor = Color(0xF20F1829),",
              "containerColor = if (AppThemeMode.isDark) Color(0xF20F1829) else Color(0xF2FBF9F4),", exact=1, label="dialog fill")
    src = rep(src, "unfocusedTextColor = Color.White", "unfocusedTextColor = WisdomInk", exact=1, label="deletion unfocused")
    src = rep(src, "focusedTextColor = Color.White", "focusedTextColor = WisdomInk", exact=1, label="deletion focused")
    src = rep(src, "color = Color.White", "color = WisdomInk", exact=3, label="deletion text")
    src = ensure_import(src, "AppThemeMode")
    src = ensure_import(src, "WisdomInk")
    return src


def patch_drawer(src):
    src = rep(src, "modifier = Modifier.background(Color(0x4D0F172A))",
              "modifier = Modifier.background(if (AppThemeMode.isDark) Color(0x4D0F172A) else Color(0xF2FBF9F4))",
              exact=1, label="dropdown fill")
    src = rep(src, "color = Color(0xFFF87171),",
              "color = if (AppThemeMode.isDark) Color(0xFFF87171) else WisdomAccentRose,", exact=1, label="error text")
    src = sweep_white_black(src, "drawer")
    for n in ("AppThemeMode", "WisdomInk", "WisdomOnInk", "WisdomAccentRose"):
        src = ensure_import(src, n)
    return src


def patch_notifications(src):
    src = rep(src, "Color.White.copy(", "WisdomInk.copy(", 1, label="skeleton tint")
    return ensure_import(src, "WisdomInk")


def patch_reader(src):
    src = rep(src, "SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)",
              "SpanStyle(color = WisdomInk, fontWeight = FontWeight.ExtraBold)", exact=1, label="bold run")
    return ensure_import(src, "WisdomInk")


JOBS = [
    ("guides/FreeResourcesStateCard.kt", patch_state_card, "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"),
    ("settings/AccountDeletionSection.kt", patch_deletion, "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"),
    ("drawer/DrawerScreens.kt", patch_drawer, "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"),
    ("notifications/NotificationsScreen.kt", patch_notifications, "WisdomInk.copy("),
    ("notifications/NotificationReaderScreen.kt", patch_reader, "SpanStyle(color = WisdomInk"),
]

results = []
for rel, fn, marker in JOBS:
    path = ROOT / rel
    src = path.read_text(encoding="utf-8")
    if marker in src:
        print(f"{rel}: already patched")
        continue
    results.append((path, fn(src)))
# post-check: no stray Color.White / Color.Black left where the sweep should have removed them
for path, new in results:
    if path.name == "DrawerScreens.kt" and ("Color.White" in new or "Color.Black" in new):
        print("ERROR: DrawerScreens.kt still has Color.White / Color.Black after the sweep; nothing written")
        sys.exit(1)
for path, new in results:
    path.write_text(new, encoding="utf-8")
    print(f"patched {path.name}")
