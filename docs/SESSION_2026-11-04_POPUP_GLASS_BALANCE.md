# Session 2026-11-04: balanced popup glass across background modes

## Owner request
Balance popup and overlay opacity so text behind panels no longer competes with popup content, avoid both overly transparent and overly solid surfaces, and make popup colors follow Transparent, Night, and Day background selections.

## Changes pushed directly to `main`
- Added shared mode-aware popup tokens in `ui/theme/WisdomGlass.kt`: Transparent popup fill 85% and secondary fill 80%; Night 90% and 85%; Day frosted ivory 93% and 90%. Added a mode-aware scrim.
- Changed Material 3 popup surface roles in `ui/theme/Theme.kt` to use the shared tokens. The color schemes are getters, so popup colors are recomputed when the selected background changes.
- Balanced local WebView popup panels in `ui/theme/AppBackdrop.kt` (night 88%, day 93%). Registered live WebViews weakly and refresh their glass script when `AppBackground.select` changes the mode. Updated script restoration so day/night switching restores original inline text, border, and background styles before applying the new palette.
- Applied shared popup fills to native confirmation dialogs and sheets in Admin, Study, Settings, Competition, the app timer, and game overlays. Updated game/status scrims to use the shared mode-aware scrim.

## Scope preserved
No navigation, exam logic, learning data, or application workflows were changed. Normal cards retain their existing lighter glass tokens; popup surfaces are intentionally denser than ordinary cards.

## Verification
- Read back the edited files from `main` after committing.
- GitHub reported no check runs for the latest code commit; no Android build or on-device visual test was run in this session.
- Owner should visually check one popup in each background mode, including a local WebView popup while switching modes without closing that screen.
