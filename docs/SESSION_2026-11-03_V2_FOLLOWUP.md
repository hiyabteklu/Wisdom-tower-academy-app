# Session 2026-11-03 V2 follow-up — Underlying screen recede

**State:** Full-width push and old-screen depth now connected and pushed to main.

## Changes
- Added `Modifier.wisdomPushRecede(active)`: a single spring moves the selected tab left by 30% of its width and dims it to 85% brightness.
- Added a `recede` input to `WisdomTabLayer`.
- MainActivity derives one `hasNativePush` flag from active workspace, guide, competitions, drawer, notifications, tool overlay and admin overlay state, then passes it to all five tab layers.
- Existing push/pop spring and tab selection/back ownership are unchanged.

## Phone test
Open a study workspace, guide, competition hub, notification screen, drawer page, tool overlay and admin console. The underlying selected tab should shift left and dim as the new screen enters; Back/close should restore it with a spring.

## Verification
No CI/build run and no phone test performed, per owner instruction.
