# Session 2026-11-03 V3 CLOSE-OUT: code check after the owner's manifest approval (Claude, chat)

Append-only. Follows `docs/SESSION_2026-11-03_V3.md`, `docs/SESSION_2026-11-03_V3_MANIFEST_NOTE.md` and `docs/SESSION_2026-11-03_V19_MANIFEST_APPROVED.md`.

## Owner order
2026-11-03: the owner again approved the manifest permission for predictive back ("yes and I approve for the manifest permission").

## What I found (read from `main`, nothing changed in code)
- `app/src/main/AndroidManifest.xml` ALREADY has `android:enableOnBackInvokedCallback="true"` on `<application>` (added in V19). No second edit was made. No permission, component or export flag was added, so nothing changes for Play Store review (hypothesis: lint may show a warning because minSdk is 24 and the attribute needs API 33; not seen on CI).
- `ui/theme/WisdomPushLayer.kt`: `PredictiveBackHandler(enabled = wanted && onBack != null)` follows the finger through the layer's own `progress`; commit springs out, then calls `onBack`; cancel springs back. The V16 build fix is in (no `ExperimentalActivityApi`). Read as correct, not compiled by me.
- `MainActivity.kt` passes `onBack` to all five push layers: study workspace (z 18) clears `activeWorkspace`, guides (z 15) clears `activeGuideSlug`, competitions (z 17) clears `showCompetitions`, notifications clears `showNotifications` and `notificationOpenId`, drawer screens (z 25) clear `activeDrawerScreen`. Same state changes the ordinary Back already makes.
- Notification reader: `NotificationReaderScreen.kt` has its own `BackHandler(onBack = onBack)`. It sits inside the push layer, so it is registered after the layer's handler and should win: Back closes the reader first, the gesture then closes the list. In the reader itself Back is the normal (not finger-tracking) back. Hypothesis from reading, not run.
- The shell uses the androidx `BackHandler` composable (code search found it in `MainActivity.kt`), which is compatible with the opt-in. This supports the V19 hypothesis that the old `onBackPressed` path is not needed. `MainActivity.kt` was NOT read in full.
- WebView reader pages (notes, flashcards) are still excluded on purpose.

## State of V3
Code and manifest are both in `main`. NOT verified by me: CI result after V19, and any phone behaviour. V3 stays `[~]` until the owner's phone test passes.

## Owner phone test (Android 13 to 15)
1. Open Competitions, Notifications, a drawer page (About), Other resources, a Learning hub. From the left edge swipe slowly: the screen follows the finger. Let go early: it springs back. Let go late: it closes.
2. Notifications: open one notice, press Back: the reader closes and the list stays. Swipe again: the list closes.
3. Study workspace: from the item list press Back twice (item list, course page); the old order must be unchanged.
4. Notes reader and flashcards: horizontal scroll must still work.
5. Home screen with nothing open: Back should leave the app normally.
If any screen stops answering Back, look first at `MainActivity.kt` (V19 side effect note).

## Left in the plan (not V3)
V5 shrink-back and push-grow (patches #89 to #91 merged, phone test pending), V6 list coverage, V7 remainder, V8 drag-to-dismiss test, V10 optional, V12 sweep.
