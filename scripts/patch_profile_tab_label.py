#!/usr/bin/env python3
"""Renames the bottom tab "Account" to "Profile" (MainActivity.kt, one line).

Run through Actions > Apply pending patches AFTER the fix/profile-merge pull request is merged.
Idempotent: running it twice changes nothing the second time. Prints ERROR and changes nothing if the line is not found.
"""
import sys

PATH = "app/src/main/java/com/wisdomtower/academy/MainActivity.kt"
OLD = 'object Account : BottomNavItem("Account", Icons.Filled.Person,'
NEW = 'object Account : BottomNavItem("Profile", Icons.Filled.Person,'

with open(PATH, encoding="utf-8") as f:
    text = f.read()

if NEW in text:
    print("already patched: tab is already called Profile")
    sys.exit(0)
if text.count(OLD) != 1:
    print("ERROR: expected exactly one Account tab line, found", text.count(OLD))
    sys.exit(1)

with open(PATH, "w", encoding="utf-8") as f:
    f.write(text.replace(OLD, NEW))
print("patched: tab renamed to Profile (1 edit)")
