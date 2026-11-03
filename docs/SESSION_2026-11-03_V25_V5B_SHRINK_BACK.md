# Session 2026-11-03 V25 — V5b shrink back into the card on Back: patch script written (Claude, chat)

Append-only. Updates the V5 row of `docs/IOS_MOTION_V2_PLAN.md` (section 6) and `docs/SESSION_2026-11-03_V24_V5_GROW_FROM_CARD.md`.

## Start of this session
- Owner reported green builds after the V7 controls patch (#88) and the V5 grow patch (#89); both merged on `main`.

## What was read (code read of `main`, 533aa6b)
- `leavePackage()` sets `selectedPackage = null` and `selectedIndex = packageOriginTab`. The package screen was removed in the same frame, so there was nothing to animate on Back.
- `PackageDetailScreen` only has its own `BackHandler` for its inner levels (stream or semester); the main Back chain in `MainActivity.kt` closes the package.
- `WisdomGrowFromCard.kt` (V5) holds `WisdomCardOrigin` (box of the last tapped card, 1.5 s, used once).

## Written
Patch script `scripts/patch_v5b_shrink_back.py` (marker `V5b shrink`):
1. Adds `WisdomGrowLayer(item, zIndexValue) { ... }` to `ui/theme/WisdomGrowFromCard.kt`. It keeps the last package composed while it closes, takes the card box once when the package opens, grows out of it, and on close shrinks and moves back onto the same card while fading out (spring damping 1.0: push stiffness in, pop stiffness out). No known card: a short fade with scale 0.94 to 1. `graphicsLayer` only; it never changes `selectedPackage`.
2. `MainActivity.kt`: `if (currentPkg != null) { PackageDetailScreen(...) }` becomes `WisdomGrowLayer(item = currentPkg, zIndexValue = 10f) { shownPkg -> PackageDetailScreen(pkg = shownPkg, ...) }`; the one extra `currentPkg.name` inside becomes `shownPkg.name`; the V5 modifier `.wisdomGrowFromCard(...)` on the screen is removed (one animation clock only); `backEnabled` also requires a selected package so Back is not claimed twice while the screen is closing. Import swapped. Runs `scripts/pack_mainactivity.py` afterwards.
3. Not changed: `leavePackage()`, the Back chain, tab logic, hub cards, Home program card.

## Tested
Script run twice on a fresh clone of `main`: first run "patched (4 edits in MainActivity.kt, plus the layer added to WisdomGrowFromCard.kt)" and "pack_mainactivity.py: ok", second run "already patched". Braces balanced; `MainActivity.kt` parentheses keep the same difference as before (3). The script also checks that no `currentPkg.` is left inside the layer block. Not compiled, not seen on a phone.

## OWNER STEPS
GitHub > Actions > Apply pending patches > Run workflow > script `patch_v5b_shrink_back.py`, branch `fix/v5b-shrink-back`. Wait for a green build, then merge.
Test on the phone:
1. Packages tab: open a package, press Back (phone Back and the screen's own back arrow): the package screen shrinks back onto its card and fades out; the list is where you left it.
2. Open a package from Home, press Back: you land on Home; a short shrink is fine.
3. Open a package, tap Back twice quickly: no crash, you end on the list, not two screens back.
4. Open a subject inside the package, then Back: still the full-width slide.

## Risks (hypotheses, not seen)
- If the package was opened from Home, the tab switches back while the screen shrinks, so the shrink plays inside the tab fade and may be hard to see.
- If you scroll the list while the package is open (not possible by touch, the package screen covers it), the end point would be off; not a normal case.

## Still open for V5
Hub card to its list (needs `WisdomPushLayer.kt`), Home program card, tool cards.
