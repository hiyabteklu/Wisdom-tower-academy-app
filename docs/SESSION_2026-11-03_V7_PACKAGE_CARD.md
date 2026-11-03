# 2026-11-03 — V7 package-card press follow-up

- Updated `ui/packages/OpenCard.kt`: the visible Open action now uses the shared `wisdomPressClickable` modifier, giving it the V2 0.97 press scale and light release haptic.
- Removed the old local clickable/interaction-source/ripple imports made obsolete by the shared helper.
- Updated `IOS_MOTION_V2_PLAN.md` in the same work slice to record this additional V7 coverage.
- Commits: `cc2bd130` (implementation/import cleanup), `d78a8221` (plan status).
- Verification: source and plan writes were accepted by GitHub on `main`. No Gradle build, CI run, emulator test, or phone test was run. This is a narrow V7 improvement, not completion of V7 or the V1–V12 plan.
