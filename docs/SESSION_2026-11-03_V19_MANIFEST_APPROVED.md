# Session 2026-11-03 V19 — Owner approved the manifest opt-in for V3 (Claude, chat)

Append-only correction to `docs/SESSION_2026-11-03_V3_MANIFEST_NOTE.md` and to the V3 row of `docs/SESSION_2026-11-03_V18_STATUS.md`.

## Owner order
2026-11-03: the owner approved changing the manifest for the predictive-back opt-in, on one condition: it must not affect Play Store acceptance.

## Change (one line, `app/src/main/AndroidManifest.xml`)
Added `android:enableOnBackInvokedCallback="true"` to the `<application>` element. Nothing else in the file changed (no permission, no component, no export flag).

## Why this does not affect Play Store acceptance (code read plus platform facts)
- It is a standard opt-in attribute documented by Android for predictive back. It adds no permission, no sensitive API and no data access, so nothing in the Play Console Data safety form, permission declarations or review policy changes.
- The app already targets SDK 36 (`app/build.gradle.kts`), where predictive back is on by default; this line makes Android 13 to 15 devices behave the same way. minSdk is 24: older Android versions ignore the attribute.
- `tools:targetApi="31"` stays; the attribute needs API 33, and Android lint reports an attribute newer than minSdk only as a warning, not an error (hypothesis: not checked on CI yet).

## Side effect to know (hypothesis from reading, not from running)
With this opt-in, Android no longer calls the old `onBackPressed()` or Back key events on Android 13 and above; only back callbacks registered with the back dispatcher run (Compose `BackHandler` and `PredictiveBackHandler` do). A code search for `onBackPressed`, `KEYCODE_BACK`, `OnBackPressedCallback` and `onKeyDown` in this repo found nothing, so nothing is expected to break. `MainActivity.kt` (245 KB) was not read in full: if any screen stops reacting to Back, look there first.

## State of V3
Code written and pushed, manifest opt-in now in `main`. Left for the owner: a phone test on Android 13 to 15 (a left-edge swipe on a pushed screen should follow the finger; releasing early springs back; completing it closes the screen; in Notifications the reader closes before the list). WebView reader pages stay excluded on purpose.
