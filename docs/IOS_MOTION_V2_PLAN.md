# IOS MOTION V2 PLAN (2026-11-03): premium, iOS-like motion in the whole app

> Repo is PUBLIC: no secrets, no student data.
> Follows `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`: one slice = one commit pushed straight to `main`, tick the slice HERE in the same commit, write a short `docs/SESSION_<date>_<slice>.md`.
> Owner is NOT a coder, free tier, new agent every 3-4 prompts. "Continue" = do the first unticked slice. Talk to the owner in plain words: what changed, what to look at on the phone.
> This plan is ONLY about motion and touch feel. No colors, layout, text, cards, navigation structure, exam logic or competition logic change.

## 0. How this fits the older plan
`docs/UI_MOTION_PLAN.md` (M1 to M11, 2026-10-16) is APPLIED in code: motion kit, bottom pill, tab fade, screen push, row entrances, overlays, flashcards, competition, quiz. Its own status table says "CI not seen". This V2 does NOT undo it. It is a deeper second pass, because the owner still feels "flat instant opens, dry clicks". V2 keeps the files and names of M1-M11 and upgrades them.
Constraints stay: no new dependency (no Lottie, no Accompanist), no forbidden files (G1-G9 in `docs/UI_POLISH_TASKS.md`), `MainActivity.kt` only through a patch script in `scripts/` (`docs/EDITING_LARGE_FILES.md`), read `docs/BUILD_PITFALLS.md` before writing Compose code.

## 1. Owner words (2026-11-03, as written)
"flat instant opens, just dry clicks nonsense, can we plan cool overall premium feel, iOS like animation in the entire app"

## 2. What was read (evidence) and what was NOT
Read on 2026-11-03: `docs/UI_MOTION_PLAN.md`, `ui/theme/WisdomMotion.kt`, `WisdomPushLayer.kt`, `WisdomReveal.kt`, `WisdomTabLayer.kt`; list of files that call `wisdomReveal`; list of `ui/` folders; code search for `AnimatedContent`, `Crossfade`, `NavHost`, `enableOnBackInvokedCallback`.
NOT read: `MainActivity.kt` (the live tab and push sites), `AliveBottomNav`, every screen file, the HTML pages in `assets/notes/`, the Gradle files (Compose version). Everything about those is a HYPOTHESIS until the slice's step 1 (inspect, write the real cause in the session log, then fix).

## 3. Why it still feels flat (V = verified in code, H = hypothesis)
1. **V: the screen push is too small to feel.** `WisdomPushLayer` slides the new screen only 56 dp from the right while fading, 280 ms. The screen below does not move, does not dim, and nothing covers it. iOS slides the whole width, moves the old screen about 30 percent to the left and dims it, so the eye sees depth. Today it reads as "a fade".
2. **V: the tab change dips to dark.** `WisdomTabLayer` fades the old tab out in 90 ms while the new one waits 60 ms and then fades in 210 ms, over an opaque navy backdrop. For a moment both are partly see-through, so the screen dips. That is the "flash". Also "no state is kept": a tab is thrown away when you leave it and built again when you come back, so the build work lands inside the animation.
3. **V: entrances play only once per app session.** `wisdomReveal` remembers every key it has played. The second time you open Home, a hub or a list, everything is instant. After the first visit the whole app is flat again.
4. **V: coverage is partial.** `wisdomReveal` is used only in 11 files (home, learning, packages cards, hub list, competition, drawer pages). H: notifications, settings rows, account, games hubs, study workspace, status, tutor, cover pages have none.
5. **V: every motion is a timed tween** (fixed milliseconds). iOS feels alive because motion is a spring that carries the finger's speed. Springs exist only in the bottom pill and Other resources.
6. **V/H: no gesture back.** No edge swipe that drags the screen with the finger (no `PredictiveBackHandler`; the manifest flag `enableOnBackInvokedCallback` was not found by code search, to be confirmed in V3).
7. **H: no "grow from the card".** A tapped card does not become the next screen; the next screen just slides in from the side.
8. **H: pages inside the WebView (notes, quiz, flashcards, tutor) paint instantly.** Native pushes into them, but the page itself has no entrance, and a blank or dark flash can show while the page loads.
9. **H: touch feedback is uneven.** Only `WisdomModernCard` and the Wisdom buttons press in. Plain `.clickable` rows, chips, list rows, switches and tabs have no press feel and no small haptic tick.
10. **H: sheets and dialogs** (tools overlay, hub gate, confirmations) enter with a fade and a small slide, not a spring, and cannot be dragged away.

## 4. Wisdom Motion V2: the iOS feel in numbers
Values are STARTING values, tune on the phone in ONE place (`WisdomMotion.kt`, new tokens added next to the old ones, nothing removed).

| Token | Starting value | Where |
|---|---|---|
| Push spring | spring, damping 1.0 (no bounce), stiffness about 260, about 380 ms | screen goes deeper |
| Pop spring | same spring, a little faster (stiffness about 340) | back |
| Back screen recede | moves 30 percent of width to the left, dims to 85 percent brightness (black scrim 15 percent) | under a pushed screen |
| Tab change | cross-dissolve with overlap: incoming fades 0 to 1 over 220 ms from the FIRST frame, outgoing stays fully visible until the incoming is above 60 percent, then fades out; no dark dip; 6 dp rise on the incoming only | five bottom tabs |
| Press | scale to 0.97 in about 90 ms, spring back with damping 0.6; light haptic tick on tap-up | every tappable |
| Sheet | spring up from the bottom (damping 0.85, stiffness 300), scrim fades, drag down to dismiss | tools overlay, bottom sheets |
| Dialog | scale 0.92 to 1 with a soft spring + fade, scrim fades | all dialogs (not exam-screen dialogs, rule 6) |
| Entrance, first visit | fade + rise 24 dp + scale 0.96 to 1, 320 ms, 45 ms stagger, first 8 items | as today |
| Entrance, return visit | a light version: fade + rise 10 dp, 180 ms, 25 ms stagger, first 6 items. Replays when the screen is OPENED again, never while scrolling | NEW (fixes cause 3) |
| Selection | colour glides, a check or dot pops with a spring (scale 0.6 to 1.0, damping 0.55) | chips, pills, checkboxes, options |
| Number or bar | eases to the REAL value only, a number never counts up (G4) | gauges, progress |

Safety rules for EVERY slice (same as M-plan, repeated because they matter):
1. Animate with `graphicsLayer` (alpha, translation, scale) read in the draw phase. No recomposition per frame. Aim for 60 fps on the owner's phone (the slow case).
2. One clock per moving thing. Never two animations on the same width, height or position.
3. Never delay access: everything is tappable from the first frame; no entrance longer than 400 ms; total stagger under 600 ms.
4. Heavy work (page composition, network) starts BEFORE the animation or after the first 120 ms, never in the middle.
5. Follow the phone's "Remove animations" setting: with it on, every motion becomes instant or a 100 ms fade (check once in V1 how `Animatable` and tween behave, then write the result in the session file).
6. Exam, timer, submit and close controls are never delayed or animated away. Exam screens are the LAST slice and motion there stays minimal.
7. No new text, no invented numbers. Back handling, `selectedIndex` ownership and the WebView lifecycle are not changed (BUILD_PITFALLS, Phase D).
8. If a slice makes a screen drop frames, lower the effect on that screen and write it in the session file. Smooth beats fancy.

## 5. Slices (ONE per chat, in this order = most felt first, riskiest later)

- [x] **V1 Spring kit and press feel (foundation).** Add to `WisdomMotion.kt`: the spring tokens above, `Modifier.wisdomPress()` (scale 0.97 spring + light haptic) and the selection pop helper. Do NOT change screens yet except one test target: `SettingsScreen.kt` rows and chips get the press feel. Step 1: confirm how "Remove animations" affects `Animatable` (write the answer). Owner test: tap Settings rows and chips, they press in and spring back.
- [ ] **V2 Push v2 (depth).** Rewrite `WisdomPushLayer.kt`: full-width slide from the right with the push spring, a black scrim, and the screen below shifts 30 percent left and dims. The shifting of the screen below needs a small change where the tab screens are drawn in `MainActivity.kt` (patch script `scripts/patch_v2_push_depth.py`, marker `PushDepth v1`, step 1 = read the live site). Same layer is used for all current pushes (study workspace, guides, competitions, drawer screens), so one change lifts all of them. Pop is the reverse. Owner test: open a package, a hub, a note, a book, a drawer page: it slides the full width, the old screen moves back and darkens; Back reverses.
- [ ] **V3 Edge-swipe back that follows the finger.** Swipe from the left edge drags the top screen with the finger; release past 40 percent or with speed = go back, otherwise spring back. Use Compose `PredictiveBackHandler` if the Compose version allows it (step 1: read the Gradle files for the version; do not edit them) and the manifest flag only if it is allowed (manifest is a forbidden file: if the flag is needed, STOP and ask the owner). Must call the SAME back chain that exists today, never reorder it. WebView screens: the edge gesture must not fight the page's own horizontal scroll (test the notes reader and flashcards). Owner decision needed first (section 7).
- [ ] **V4 Tab change v2 (no dark dip, state kept).** Rewrite `WisdomTabLayer.kt` with the overlap cross-dissolve above. Keep each tab's state and scroll position instead of rebuilding (`rememberSaveableStateHolder` hypothesis; step 1 = read how the tab layers are called in `MainActivity.kt`; patch script `scripts/patch_v2_tabs.py`, marker `TabLayer v2`). Coordinate with the bottom pill glide (the page switch starts when the glide is under way). Owner test: tap the five tabs fast and slow: no flash, no dark dip, the scroll position of a tab is where you left it.
- [ ] **V5 Grow-from-card opening.** The main card to screen openings (package card to package detail, hub card to its list, Home program card) start the new screen from the tapped card's position and size and grow to full screen (scale and move of ONE layer with the push spring, the card fades as it becomes the screen). Do NOT use the experimental shared-element API unless step 1 shows the Compose version supports it as stable; otherwise store the tapped card's bounds (`onGloballyPositioned`) and animate a plain layer from them. Falls back to V2 push if bounds are unknown. Owner test: tap a package card, it grows into the package screen; Back shrinks it back to the card.
- [ ] **V6 Entrance v2 and coverage.** `wisdomReveal` gets the return-visit light version (section 4) and a replay when a screen is opened again. Add it to every list and card screen that lacks it: notifications, settings (sections appear in order), account, games hubs (Tower Defense, Tower Climb, Match, Exam picker), study workspace, status screens, tutor sessions list, cover page generator, other drawer pages. Add `animateItem` (insert, remove and reorder motion) to lists that change live (notifications, tutor sessions, hub lists after refresh). Split into two chats if the diff is big. Owner test: open the same screen twice, it comes in softly both times; delete a notification, the row slides away and the others close the gap.
- [ ] **V7 Touch feel everywhere.** Apply `wisdomPress` to every plain `.clickable` (list rows, chips, drawer rows, top bar icons, bottom tabs, game buttons). Switches and checkboxes: spring thumb, selection pop. Light haptic tick on tab change, chip select and toggle (reuse the haptic the bottom pill already uses, no new permission). Owner test: nothing in the app taps "dead".
- [ ] **V8 Sheets, dialogs, menus.** Tools overlay (AI Tutor, Calculator, Timer, Planner, Notebook, Targets, Status) and other sheets enter with the sheet spring and can be dragged down to close; dialogs and drop-down menus scale in with the dialog spring. Reuse `WisdomOverlayMotion.kt`, do not duplicate. Exam-screen dialogs stay instant (rule 6). Owner test: open the tools, drag one down to close it; open a dialog, it pops softly.
- [ ] **V9 WebView pages.** Quiz, notes, flashcards, tutor, competition exam, cover pages: (a) keep the native cover until the page has painted, so there is no blank or dark flash when a page opens; (b) a short CSS page entrance inside the page (fade + 10 px rise, 200 ms) and softer button press in the HTML (`:active` scale 0.97); (c) one shared small CSS block reused by the pages if the pages already share one (step 1 = look in `app/src/main/assets/notes/`). Nothing changes the page logic. Quiz and exam pages: entrance only on open and results, never on question change (that is already done in M10 and E-slices). One chat per two pages. Owner test: open a note, a quiz, flashcards, the tutor: no flash, a soft arrival.
- [ ] **V10 Scroll and header polish (optional).** Large title that collapses while scrolling on Home, Learning, Packages; slight parallax of the hero; cards just entering at the bottom edge a little smaller and settle (the old optional `wisdomScrollDepth`). Skip any effect that costs frames on the owner's phone.
- [ ] **V11 Exam screens, last and careful.** Question change, option select pulse, results arrival, timer low-time pulse (visual only). Timer, submit, close stay instant and reachable. No change to timing, grading or integrity hooks (`EXAM_INTEGRITY_ADMIN.md`).
- [ ] **V12 Consistency and speed pass.** Sweep every screen: anything without the kit, different feel between screens, dropped frames. Update the phone checklist. Check with "Remove animations" on and on a low battery or slow phone. No new features.

## 6. Status table (update in the same commit as each slice)
| Slice | State |
|---|---|
| Plan | WRITTEN and pushed 2026-11-03 |
| V1 | CODE PUSHED (2026-11-03; phone test pending) |\n| V2 to V12 | NOT STARTED |

## 7. Owner decisions (defaults chosen, change any of them)
1. **Style:** calm and premium like iOS: smooth springs, no bounce on screens, a small bounce only on small things (selection pop, press). Not playful.
2. **Return visits:** screens come in with the LIGHT entrance every time they are opened (default), instead of being instant after the first visit. Say "first visit only" if it gets annoying.
3. **Edge-swipe back (V3):** wanted? It changes how Back feels on the whole app and is the riskiest slice. Default: do it after V2 and V4 are tested on the phone.
4. **Manifest flag** for predictive back: the manifest is a forbidden file. If V3 needs it, the owner must say yes first.
5. **Haptics:** a light tick on tap, tab change and toggles (default yes). Say no if you do not want any vibration.
6. **Admin console** is the website in a WebView (repo `Wisdom-tower-academy`): out of scope here.

## 8. Phone checklist after every slice (on top of `HANDOFF.md` section 9)
Tap the five tabs quickly and slowly: no flash, no dark dip. Open and close a package, a hub, a note, a book, a drawer page: the full-width slide with the old screen receding, Back reverses. Open the same screen twice. Tap rows, chips, switches, tabs: they press in. Open a sheet and a dialog. Switch the phone to "Remove animations": the app still works instantly. Exam timer, submit, close still reachable and instant.
