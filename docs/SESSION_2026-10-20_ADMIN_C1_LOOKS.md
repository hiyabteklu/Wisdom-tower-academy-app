# SESSION 2026-10-20 — Admin C1 (Day / Night / Transparent looks)

## Owner report
Day mode is correct. Night mode is not similar to the rest of the app. Transparent mode is not available in the admin dashboard.

## Root cause (verified in code)
1. `AdminConsoleHost.kt` wrapped the whole console in solid `.background(WisdomBackground)`. That paint sits on top of the shared app backdrop from Theme / `appBackdrop()`, so the Transparent photo never showed and Night looked like a flat solid page instead of glass over the same backdrop as Home / Learning / etc.
2. `AdminDetailPanel` in `AdminKit.kt` did the same solid fill for the full-screen detail panel and its bottom action bar.
3. Settings already offers Night | Day | Transparent (`AppBackground.options` includes `Photo2`). The gap was admin covering the backdrop, not the picker.

Day still worked because Day tokens + solid ivory are close enough to the Day backdrop.

## Change
- `AdminConsoleHost.kt`: `.background(WisdomBackground)` -> `.appBackdrop()`.
- `AdminKit.kt` `AdminDetailPanel`: panel root and action bar use `.appBackdrop()` instead of solid `WisdomBackground`.
- Behaviour, permissions, and module logic unchanged.

## Left for later (not this slice)
- Fixed accent literals in some admin screens (e.g. Competitions CompOrange) noted in `docs/ADMIN_DASHBOARD_NEXT.md` C1 notes.
- `AdminWebSection` WebView still uses a fixed dark `setBackgroundColor` for residual website modules; only matters for non-native panels.
- `AdminConfirmDialog` still uses solid `WisdomNavy` container (dialogs are short-lived; can follow if the owner wants).

## Status
`[~]` Written and pushed. Not compiled by the agent. Not seen on a device.

## Phone test (owner)
1. Settings > Background: pick **Night**. Open Account > Admin Console. Page should feel like the rest of the app (glass cards over dark), not a flat solid slab.
2. Pick **Transparent**. Re-open Admin. The photo background should show through (same as Home).
3. Pick **Day**. Admin should stay ivory / correct as before.
4. Open any module, open a detail panel (e.g. Content locks > a package), Back: panel and list should follow the same look.
5. Night regression: Home, Learning, bottom nav still look as before.
