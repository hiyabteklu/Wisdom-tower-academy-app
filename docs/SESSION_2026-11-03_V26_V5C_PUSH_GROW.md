# Session 2026-11-03 V26 — V5c pushed screens grow out of the tapped card: patch script written (Claude, chat)

Append-only. Updates the V5 row of `docs/IOS_MOTION_V2_PLAN.md` (section 6).

## What was read (code read of `main`, 06c320f)
- The subject and course cards inside the package screen are `OpenCard`s; they call `onOpenSubjectWorkspace`, which sets `activeWorkspace`, which `WisdomPushLayer` shows (full-width slide). Since V5, every `OpenCard` already records its box when tapped (`WisdomCardOrigin`), so the push layer can use it.
- The same layer shows guides, competitions, notifications and drawer pages. Those are not opened from an `OpenCard`, so they keep the plain slide unless a card was tapped in the last 1.5 s.
- `wisdomPushRecede` slides the tab below 30 percent to the left under every push. With a grow that would carry the tapped card away from under the growing screen, so it gets one switch (`WisdomGrowFlag`).
- Hub tiles inside the study workspace (the six hubs) open WebView pages or native lists through `onOpenHub`; they are not `OpenCard`s and are NOT part of this slice.

## Written
Patch script `scripts/patch_v5c_push_grow.py` (marker `V5c grow`), edits `ui/theme/WisdomPushLayer.kt` and makes `applyGrow` in `WisdomGrowFromCard.kt` `internal`:
1. On open the layer takes the tapped card box once. With a box: grow out of the card, shrink back into it on close, predictive back drags the same shrink with the finger. Without a box: the old slide, unchanged.
2. `WisdomGrowFlag.active` is true while such a screen is open: the tab below stays in place (still dims). Released when the close finishes.
3. Not changed: Back chain, `onBack` callbacks, `MainActivity.kt`, the state that decides visibility.

## Tested
Script run on a fresh clone of `main`: before V5b it refuses with a clear message and changes nothing; after V5b it reports "patched (6 edits in WisdomPushLayer.kt, 1 in WisdomGrowFromCard.kt)"; a second run says "already patched". Braces and parentheses balanced in both files. Not compiled, not seen on a phone.

## OWNER STEPS
Run AFTER `patch_v5b_shrink_back.py` is merged and green (each patch starts from `main`).
GitHub > Actions > Apply pending patches > Run workflow > script `patch_v5c_push_grow.py`, branch `fix/v5c-push-grow`. Wait for a green build, then merge.
Test on the phone:
1. Open a package, tap a subject card: the subject workspace grows out of the card; the package screen behind it does NOT slide left.
2. Press Back (button and the edge swipe): it shrinks back into the same card.
3. Open a drawer page (About, FAQ) from the menu: still the full-width slide.
4. Open the trophy (Competitions) and the bell: still the full-width slide.

## Risks (hypotheses, not seen)
- If a guide or drawer page is opened within 1.5 s after tapping a card, it will grow from that card instead of sliding. Rare, harmless.
- The grow starts a little off the card if the card sits under the status bar; it always ends exactly full screen.

## Still open for V5
Home program card with its own origin (it goes through the Packages tab, V5 already covers it), the six hub tiles inside the workspace.
