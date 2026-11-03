# Session 2026-11-03 V21 — V8 tool sheets: spring arrival and drag-down to close (Claude, chat)

Append-only. Updates the V8 row of `docs/SESSION_2026-11-03_V18_STATUS.md`.

## What was read (code read of the live files)
- `MainActivity.kt`: the seven global native tools (AI Tutor, Calculator, Timer, Planner, Notebook, Targets, Status) are drawn by one `when (currentTool)` block at zIndex 120 with no arrival motion and no drag. Back is a plain `BackHandler` that sets `activeStudyTool = null`.
- `WisdomOverlayMotion.kt` already has `wisdomSheetEnter` (spring 0.85 / 300).
- Compose BOM 2024.09.00 and activity-compose 1.10.1 (`gradle/libs.versions.toml`), so `NestedScrollSource.UserInput` exists.

## Written
1. New file `ui/theme/WisdomToolSheet.kt` (on main now): spring arrival; drag down to close through nested scrolling (content at its top and the finger still pulls down moves the whole sheet; release past 30 percent of the height or a fast down-fling closes it, otherwise it springs back). `graphicsLayer` only. A screen without a scrolling list simply does not drag, so keypads, sliders and text fields are not disturbed.
2. Patch script `scripts/patch_v8_tool_sheet.py` (marker `V8 tool sheet`): wraps the `when (currentTool)` block in `WisdomToolSheet(sheetKey = currentTool, onDismiss = { activeStudyTool = null })`. The BackHandler, the screens and their parameters are not changed.

## Tested
Script run twice on a copy of the real `MainActivity.kt`: first run "patched (2 edits)", second "already patched". Braces 1096/1096; parentheses keep the same difference as before (3). Not compiled, not seen on a phone.

## OWNER STEPS
GitHub > Actions > Apply pending patches > Run workflow > script `patch_v8_tool_sheet.py`, branch `fix/v8-tool-sheet` > wait for the build > test > merge. If you also run `patch_v9_cover_until_paint.py`, run the two one after the other (each from `main`; merge the first before running the second, so the second starts from the merged file).
Test: open AI Tutor, Calculator, Timer, Planner, Notebook, Targets and Status from the menu: each springs up softly. In Planner, Notebook, Targets, Status and the Tutor chat list, scroll to the top and keep pulling down: the screen follows the finger; let go early and it springs back, pull far or flick and it closes. Calculator and Timer have no scrolling list, so use Back or their close button there.

## Risk to know (hypotheses, from reading)
- A screen whose list uses a custom scroll (not the standard Compose scroll) will not drag. Harmless: it just behaves as before.
- Chat lists that start from the bottom (Tutor) may only drag when scrolled to the oldest message. Tell me if you want a handle strip at the top instead.
- The tools overlay for website pages (checkout, orders) is a separate layer and is not changed here.

## Not done
The website tools overlay drag, other menus, and the Tools sheets' dim backdrop. `docs/PENDING_PATCHES.md` not edited (this log holds the entry).
