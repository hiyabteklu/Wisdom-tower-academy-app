# Session 2026-11-03 V29 — V5 hub tiles (V5d) and V10 header: two patch scripts written (Claude, chat)

Append-only. Owner order 2026-11-03: "Motion plan is priority, build is green, continue and write V5 and V10". Builds on `docs/SESSION_2026-11-03_V28_PATCHES_MERGED.md`. The owner reported the build of the V7 and V6 merges as GREEN (owner report, not seen by me).

## What was read (code read of `main`, dd3694a)
- `StudyWorkspaceScreen.kt`: the six hub tiles ARE `OpenCard`s (V27 said they were not: that was wrong, so they already record the tapped box). The problem is different: tapping a tile sets `view = HubList(...)`, and the code then draws the sub-screen and `return`s, so the tile grid is REMOVED at once. There is nothing to grow from and nothing to shrink back into.
- `WisdomGrowFromCard.kt`: `WisdomGrowLayer(item, zIndexValue) { ... }` (V5b) already does grow and shrink for one item and is reusable.
- Home, Learning and Packages screens read in full for V10. None of them has a collapsing title to hook into: only Packages has a real large title ("Academy packages"), Home has a welcome card, Learning has a guest banner and a Study Tools card.

## Written
1. `scripts/patch_v5d_hub_tiles_grow.py` (marker `V5d hub grow`), one file: `ui/study/StudyWorkspaceScreen.kt`, 5 edits. The grid now stays composed. The hub list, and the book / note / cards / quiz opened from it, are drawn inside `WisdomGrowLayer`: grows out of the tapped tile, shrinks back into it when Back returns to the grid. The grid is hidden from TalkBack while the layer is open and the layer swallows touches. The code of every sub-screen is the same text as before (moved into a lambda `subView`; the parameters keep their old names on purpose, so the compiler may print "name shadowed" warnings, which are harmless). Opened straight into a hub from a website link (entrance questions): no grid, no grow, as before. Not changed: `view`, the BackHandler, locks, counts, dialogs, `WisdomGrowFromCard.kt`, `MainActivity.kt`.
2. `scripts/patch_v10_header_parallax.py` (marker `V10 header`): new `ui/theme/WisdomHeaderParallax.kt` with `Modifier.wisdomHeaderParallax(listState, key)` plus 16 small edits in `PackagesScreen.kt`, `HomeScreen.kt`, `LearningScreen.kt`. The top title or card lags behind the scroll (35 percent), shrinks up to 6 percent and fades up to 55 percent while it leaves the top; nothing applies at rest. `graphicsLayer` only (draw phase), alpha with `ModulateAlpha` (no extra offscreen buffer), no layout, text or colour change. Used on: Packages title, Home welcome card, Learning guest banner and Study Tools card. The "cards at the bottom edge settle" part is NOT written, on purpose (costs frames on long lists). V10 therefore stays a header-only version, as V27 offered.

## Tested
Both scripts run twice on anchor copies of the real code (copied from `main`): first run patched, second run "already patched"; the V5d output was read through and its braces are balanced. NOT compiled, NOT seen on a phone.

## OWNER STEPS (GitHub > Actions > Apply pending patches; one at a time, merge before the next, each starts from `main`)
1. `patch_v5d_hub_tiles_grow.py`, branch `fix/v5d-hub-tiles-grow`. Build green, then merge.
2. `patch_v10_header_parallax.py`, branch `fix/v10-header-parallax`. Build green, then merge.
If a build is red, send the error text to the next agent.

Phone test for V5d: open a package, a subject, then tap a hub tile (Books, Short Notes, Flashcards, Question Banks, Exams): the list grows out of the tile; open a note or book, press Back twice: the list shrinks back into the same tile and the tile grid is where you left it; tap an empty spot of the hub list: nothing underneath opens; TalkBack on: only the list is read; open Entrance Questions: it opens straight into the list as before.
Phone test for V10: scroll Home, Learning and Packages: the top card or title slows, shrinks a little and fades as it leaves; back at the top everything is normal; the cards still tap normally.

## Risks (hypotheses, not seen)
- A WebView screen (note, quiz, cards) now sits inside the grow layer. The layer applies no transform once it is fully open, so I expect no change, but this is the first thing to check on the phone (open a note and a quiz).
- Hub list rows have their own entrance (`wisdomReveal`) and now also play inside the grow: two soft motions on the first 300 ms. If it feels busy, say so and the row entrance is removed for that screen.
- Pressing Back during the 300 ms shrink closes the whole course page (the Back handler is already off by then). Rare.
- The tile grid behind the hub list stays composed, so the course page builds the status board once and keeps it; small cost.

## Board of IOS_MOTION_V2_PLAN after this session (replaces the V27 board)
| Slice | State |
|---|---|
| V1 to V4, V6 to V9, V11 | written (V6, V7 merged, build green per owner) |
| V5 | package cards, subject and course cards merged; hub tiles written in `patch_v5d_hub_tiles_grow.py`, waiting for the owner to run it |
| V10 | header-only version written in `patch_v10_header_parallax.py`, waiting for the owner to run it |
| V12 | final phone pass after V5d and V10 are merged: the owner's report decides what is left |
