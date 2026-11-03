# Session 2026-11-03 V4 follow-up — Preserve tab saveable state

**State:** Saveable state holder added to each persistent tab layer and pushed to main.

## Changes
- Each `WisdomTabLayer` now owns a `rememberSaveableStateHolder()`.
- The tab content is wrapped in `SaveableStateProvider(tab)`.
- Compose saveable state, including `rememberLazyListState()` scroll positions, is retained while a tab is not selected and its content layer is removed.
- The overlap cross-dissolve and selectedIndex ownership are unchanged.

## Limitation
Ordinary `remember` state that is not saveable is not guaranteed to persist. This does not alter tab navigation or WebView lifecycle.

## Phone test
Scroll Home, Learning, Packages, Account and Settings. Switch between tabs and return. Confirm lazy-list scroll positions remain where you left them and the dark dip stays gone.

## Verification
No CI/build run and no phone test performed.
