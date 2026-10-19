# SESSION 2026-10-19: D10, day mode for the Other resources (guides) screens

Owner screenshots (phone, day mode) showed the Other resources pages with white titles and pale body text on ivory cards: nearly invisible.

## Cause (read from the code)
`ui/guides/GuidesScreen.kt` (about 100 KB) was never in a D slice (D3 to D7 list Home, Learning, Packages, Settings, shell, account, study tools, competitions; D8 the WebView pages). It still has `Color.White` titles and icons, light slate body text (`E2E8F0`, `CBD5E1`), pale yellow quotes (`FDE68A`) and `Color.White.copy(...)` tints. The text styles were top-level vals (read once at class load) and `GUIDE_TABS` in `GuidesData.kt` also captured accent colours once.

## What was written
`scripts/patch_d10_guides_day.py` (11 edits in `GuidesScreen.kt`, 1 in `GuidesData.kt`). Night values are unchanged. Day uses `WisdomInk`, `WisdomTextPrimary`, `WisdomMuted`, `WisdomAccentAmber`. Left on purpose: white text on the solid accent "Apply / Official Page" button, the dark placeholder behind story photos.

## Tested
On a FIXTURE with the exact anchor lines (not on the real 100 KB file: the chat tool could not save it to disk). Run twice: patched, then "already patched". The test found and fixed one bug (`focusedTextColor` is a substring of `unfocusedTextColor`; order fixed). If an anchor count differs on the real file the script stops and writes nothing. NOT compiled.

## Owner steps
GitHub > Actions > Apply pending patches > Run workflow > script `patch_d10_guides_day.py`, new branch name, wait for green build, test all six tabs in Day and in Dark, merge the pull request.

## Test list
- Day: Study Techniques, Success Stories, Campus Life, Universities, Departments, Scholarships: every title, body, chip, search box, accordion readable; tab pill colours dark teal / amber etc, not neon.
- Night: same six tabs look exactly as before.
- Switch Dark to Day while the app is open: the tab accents change at once.

## Still open (same bug, found by search, not yet fixed)
`FreeResourcesStateCard.kt`, `ui/drawer/DrawerScreens.kt`, `ui/notifications/NotificationsScreen.kt`, `NotificationReaderScreen.kt`, `ui/settings/AccountDeletionSection.kt`, `ui/theme/WisdomComponents.kt` still have `color = Color.White`. The search matched one pattern only, so the list may be longer. Next slice: D11, sweep these, then D9 audit.
