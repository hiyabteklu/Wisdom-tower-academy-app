# Session 2026-11-03 V3 follow-up — Predictive-back opt-in boundary

The Compose `PredictiveBackHandler` and native overlay callbacks are in main. The Android manifest was deliberately not changed.

Android's predictive system gesture animations may require `android:enableOnBackInvokedCallback="true"` in the manifest on Android 13/14 (and opt-in for system animations on Android 15). The repository motion plan marks the manifest as a forbidden file and requires the owner's explicit approval if that flag is needed.

Therefore V3 remains **partial**, not complete, until the owner authorizes the manifest opt-in and the gesture is tested on the phone. The ordinary Back callback remains connected to the same existing close actions. WebView pages remain excluded to protect horizontal page gestures.
