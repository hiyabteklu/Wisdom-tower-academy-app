# Session 2026-11-03 V13 — Audit of V1 to V12 (Claude, chat)

**State:** Read-only audit plus one tiny compile-safety fix. No slice finished, so no tick in `docs/IOS_MOTION_V2_PLAN.md`. No build, CI or phone test seen.

## What was checked (evidence: code read)
- `WisdomMotion.kt` (press kit, `wisdomPressClickable`): reads correct for Compose BOM 2024.09 and activity-compose 1.10.1.
- `WisdomPushLayer.kt`: predictive back wired through `PredictiveBackHandler`; manifest was NOT touched (still forbidden, still needs owner approval).
- `WisdomTabLayer.kt`: per-tab `SaveableStateProvider` is there.
- The plan's status table matches the commits of 2026-11-03 (V1, V2, V4, V11 done; V3, V6, V7, V8, V9, V12 partial; V5 and V10 not started).

## Fix pushed
- Removed `import kotlinx.coroutines.flow.collect` from `WisdomPushLayer.kt`. The Flow member `collect` is used, so the import was not needed and is a known "unresolved/hidden" risk with coroutines 1.10. Nothing else in the file changed.

## Hypotheses found (NOT fixed, need a phone or CI to confirm)
1. **Tab dissolve may still dip.** In `WisdomTabLayer.kt` the incoming layer (zIndex 11) carries its own opaque `appBackdrop()` on the OUTER box, so it hides the outgoing tab at once and only its content fades in. That is a dip to the background, not a true cross-dissolve. A fix would move the backdrop inside the fading box for the incoming layer only, but it needs a look at how `MainActivity.kt` hides the hub page (`active = false`) first.
2. **Push scrim is invisible.** In `WisdomPushLayer.kt` the black scrim box sits inside the sliding layer and under the screen content, so it is covered. The 15 percent dim of the screen below is done by `wisdomPushRecede`, so the effect exists, but the scrim code is dead weight.
3. **Predictive back on Android 13 to 15** needs `android:enableOnBackInvokedCallback="true"` in the manifest to follow the finger. On Android 16 (target 36) it should be on by default. Owner approval needed before touching the manifest.
4. **Nothing since V1 has been compiled.** The V12 follow-up says no CI was run. First action for the next agent or the owner: run the Actions build and read the errors.

## Still open (unchanged from the plan)
V3 manifest decision, V5 grow-from-card, V6 tutor sessions list and other dynamic lists, V7 top bar, bottom tabs, switches, checkboxes, V8 tool overlay spring and drag-to-dismiss, V9 native cover until first paint, V10 optional, V12 final sweep.
