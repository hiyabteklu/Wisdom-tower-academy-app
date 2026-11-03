# Session 2026-11-03 V24 — V5 grow-from-card for the package screen: patch script written (Claude, chat)

Append-only. Updates the V5 row of `docs/IOS_MOTION_V2_PLAN.md` (section 6) and `docs/SESSION_2026-11-03_V18_STATUS.md`.

## What was read (code read of `main`, a42d6f4)
- The package screen is NOT a push overlay. In `MainActivity.kt` the Packages tab (`WisdomTabLayer(tab = 2)`) shows `PackageDetailScreen` when `selectedPackage != null` and the package list (`GameTabScreen`) otherwise, so the list disappeared the moment a card was tapped. A grow animation over an empty background would not read as "the card becomes the screen".
- Package cards are `OpenCard` (`ui/packages/OpenCard.kt`, card body and the Open button both call the same `onClick`). The same card is also used for subjects and semesters inside the package screen.
- Hub cards open `StudyWorkspaceScreen` through `WisdomPushLayer` (a different path); the Home program card goes through `onSelectCourse` (switches to the Packages tab).
- `WisdomPushLayer.kt`, `WisdomMotion.kt` (`wisdomPress`, `WisdomMotionV2.PushStiffness`) read. No experimental shared-element API used.

## Written
1. Patch script `scripts/patch_v5_grow_from_card.py` (marker `V5 grow`). It creates `ui/theme/WisdomGrowFromCard.kt` and makes 7 edits in 2 files:
   - `WisdomCardOrigin`: the box (on screen) of the last tapped card, valid for 1.5 s, used once. `Modifier.wisdomGrowFromCard(key)`: the screen starts as a small copy of itself centred on that card (uniform scale, rounded corners, fading in over the first third) and springs to full screen with the push spring (damping 1.0, no bounce). `graphicsLayer` only. No fresh card = no effect.
   - `OpenCard.kt`: the card records its box when tapped (card body or Open button). Click behaviour unchanged.
   - `MainActivity.kt`: `GameTabScreen` is always composed on the Packages tab and hidden from TalkBack while a package is open; `PackageDetailScreen` is drawn on top of it with `.wisdomGrowFromCard(currentPkg.name)`. Side benefit: the list keeps its scroll position when you come back.
   - Runs `scripts/pack_mainactivity.py` afterwards (HANDOFF rule 10), as the other `MainActivity.kt` patches do.
2. Not changed: Back (the package screen still closes at once, no shrink back into the card), hub cards, Home program card, `WisdomPushLayer.kt`.

## Tested
Script run twice on a fresh clone of `main`: first run "patched (7 edits in 2 files, plus WisdomGrowFromCard.kt)" and "pack_mainactivity.py: ok", second run "already patched". Braces balanced in all three files; `MainActivity.kt` parentheses keep the same difference as before (3). Not compiled, not seen on a phone.

## OWNER STEPS
Run AFTER the V7 controls patch is merged (each patch starts from `main`).
GitHub > Actions > Apply pending patches > Run workflow > script `patch_v5_grow_from_card.py`, branch `fix/v5-grow-from-card`. Wait for a green build, then merge.
Test on the phone:
1. Packages tab: tap a package card. The package screen grows out of the card and fades in; the list is still visible behind it for a moment.
2. Press Back: the package screen closes and the list is where you left it (same scroll position).
3. Open a package from Home (Learning): it should still open correctly; it grows from the Home card or just appears.
4. Open a subject inside a package: same as before (the full-width slide).
5. TalkBack on, package open: it should read only the package screen.

## Risks (hypotheses, not seen)
- The list is composed under the package screen, so the Packages tab builds slightly more at once. If the grow stutters on your phone, tell me and I will lower it.
- If the grow starts a few pixels off the card, the start position is wrong by the status bar height; it ends exactly full screen either way.

## Still open for V5
Shrink back into the card on Back, hub card to its list, Home program card (own origin), tool cards.
