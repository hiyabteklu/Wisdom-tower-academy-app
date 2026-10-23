# Session 2026-10-23: drawer cleanup (patch written, NOT applied)

Owner order: remove the "Refresh" button from the drawer and the "Wisdom Tower Academy • Native Shell" text at the bottom.

## Written
- `scripts/patch_drawer_remove_refresh_and_shell.py` (marker `DRAWER-CLEAN`, 3 exact anchors in `MainActivity.kt`, safe to run twice). Tested twice on a copy of the real `MainActivity.kt` from `main` (first run patched, second run "already patched"); diff reviewed: Refresh row and its upper divider block gone (one divider stays before SUPPORT & ABOUT), footer divider + Native Shell text gone, support rows' animation index 6 -> 5.
- Left alone on purpose: `performHardRefresh()` (now has no caller in the file; a harmless unused-function warning) and the Refresh icon import (unused import warning). No `allWarningsAsErrors` in the repo (code search found none).

## Evidence
- `[~]` Code only. NOT applied, NOT compiled, NOT seen on a phone.

## Test
1. GitHub > Actions > Apply pending patches > Run workflow: script `patch_drawer_remove_refresh_and_shell.py` (WITHOUT `scripts/`), any new branch name (for example `fix/drawer-clean`).
2. Wait for the build on that branch to go green; if red, paste the error to the next agent.
3. Phone: open the menu. Tools ends with Planner, one divider, then SUPPORT & ABOUT; no Refresh row; nothing under Terms of Service.
