# DAY_MODE_PLAN.md — premium day mode "Ivory & Navy" (owner order 2026-10-19)

> Repo is PUBLIC: no secrets, no student data. Free-plan rule: ONE slice per chat (`docs/AGENT_QUOTA_PROTOCOL.md`).

## 1. Owner words (2026-10-19)
The custom background BG 2 is awkward. Keep the dark mode as it is and design a compatible plan for day mode: not white, something premium that elevates the app. The app is being prepared for publishing.

## 2. Decision and what it replaces
- This brings day mode BACK. It overrides the cancel of 2026-10-17 (`CLAUDE.md`, `docs/DAY_NIGHT_PLAN.md`) for day mode only. Night stays exactly as it is.
- The cancel reason was real: the glass cards are see-through dark panels and do not work on a light page. So day cards are NOT see-through. They are frosted ivory (about 80 to 88 percent opaque) with a hairline border and a soft navy shadow. This is the key rule of the whole plan.
- The old pale day palette in `DAY_NIGHT_PLAN.md` (page F4F7FB, white cards) is NOT used. Ignore its slices L1 to L9. The slices below are D1 to D9.
- ASSUMPTION (owner did not answer yet; hypothesis, change on the owner's word): Day REPLACES BG 2 in the picker (Dark and Day). `bg_2.jpg` stays in the repo, unused (nothing is deleted).

## 3. Verified facts (code read 2026-10-19)
- Before D1: `AppThemeMode.isDark` was forced true. Every colour token in `ui/theme/Color.kt` is a mode-aware getter (`pick(dark, light)`), so the switch plumbing still existed.
- `ui/theme/AppBackground.kt`: options Dark (`Black`, pure black), BG 1/2/3 (pictures), Blue; offered in the picker before D1: Dark and BG 2. BG 2 = `assets/backgrounds/bg_2.jpg` (270 KB) with a 35 percent black shade (`PHOTO_SCRIM_ALPHA`).
- The Settings picker that is actually used is `DisplayThemeSection` (`ui/settings/DisplaySection.kt`): it shows one round swatch per entry of `AppBackground.options`, drawn with `appBackdrop(option)`. So adding Day to `options` shows a Day swatch with no Settings edit. (`BackgroundSection.kt` is an older pill that is not the active one; its `iconFor` falls back to the Image icon for Day.)
- The glass for local WebView pages is a script in `ui/theme/AppBackdrop.kt` (reads page colours, paints glass). It assumes light text on a dark page, so D8 needs a day variant.
- NOT verified: what BG 2 looks like (the chat tool cannot view pictures). Hypothesis for "awkward": the photo competes with text, like the first galaxy photo did (`docs/SESSION_2026-10-17_B6_GALAXY_PHOTO.md`).

## 4. Design: Ivory & Navy (values are the starting point; the owner sees them on a phone in D1)
| Token role | Day value |
|---|---|
| Page | ivory-stone E9E5DC, plus two very soft glows (pale sky at the top, champagne at the bottom), low alpha, drawn by `appBackdrop` like the Blue option |
| Card | frosted ivory F8F5EE at about 82 percent, border navy 12 percent, soft navy shadow (the shadow comes in D2) |
| Overlays (nav, top bar, drawer, dialogs, sheets) | the same ivory at about 92 percent (owner feedback 2026-10-17: overlays need more opacity) |
| Text | deep navy ink 0B1B33 (never pure black); muted 4A5A70 |
| Accent | deep teal-cyan 0B7285 (white text on it); night keeps 22E0FF |
| Gold | premium moments only (badges, leaderboard, Pro): fill F1DFB4 with dark gold text 5C3F0A; gold is never body text. Comes with the screen slices, not D1 |
- Contrast figures are my hand estimates (ink on card about 15:1, muted about 6:1, accent about 5:1): hypothesis, to be measured in D9. Normal text 4.5:1, large text and icons 3:1, in BOTH modes.

## 5. Rules for every slice
1. Night must stay identical. Every slice ends with a night check of the screens touched.
2. Reuse the sweep rules in `docs/DAY_NIGHT_PLAN.md` section 3 (no `Color.White` / `Color.Black` for text; use `WisdomInk`, `WisdomOnInk`, `WisdomTextPrimary`, `WisdomMuted`; never read a token once at class load; literal dark surfaces become tokens and the text on them flips in the same slice).
3. Big files (`MainActivity.kt`, `SettingsScreen.kt`, `LearningScreen.kt`, over about 30 KB): patch script plus the Action (`docs/EDITING_LARGE_FILES.md`). Real Material icons only. No new dependency or host. Forbidden files stay forbidden (`HANDOFF.md` rule 12).
3b. While the slices are unfinished, Day must not reach students: `AppBackground.DAY_MODE_ENABLED` (constant in `AppBackground.kt`) is the switch; see section 7.
4. Written code is marked `[~]` until CI is green and the owner has tested it.

## 6. Slices
| Slice | What | Status |
|---|---|---|
| D1 | Foundation: replace the day palette in `Color.kt`; day / night follows the chosen background (`AppThemeMode`); Day option in `AppBackground.kt` replaces BG 2 in the picker; light Material scheme with ivory pop-up roles and status bars in `Theme.kt` | WRITTEN 2026-10-19 on branch `feature/day-mode-d1` (3 commits), `[~]` not compiled, CI not seen, NOT on main |
| D2 | Glass and surface tokens for day (`WisdomGlass.kt`, field surface, message card), shared cards, overlays and pop-ups, soft shadows | open |
| D3 | Home, Learning, Packages, Package detail, Settings, My Subjects, GPA card, Study workspace | open |
| D4 | Top bar, bottom nav, drawer (`MainActivity.kt` / `StructuralNav.kt`, patch script) | open |
| D5 | Account, native sign-in, forgot password | open |
| D6 | Study tools, readers, hub lists, tutor, PDF reader, dialogs | open |
| D7 | Competitions, native admin console, onboarding, splash | open |
| D8 | Local WebView pages (notes, quiz, flashcards, tutor, math): day variant of the glass script and page CSS; math, highlights and code blocks need day colours. Until then `AppBackgroundOption.Day` hands the web pages the Dark look on purpose | open |
| D9 | Full contrast audit with owner screenshots in both modes (all tabs, TalkBack, font Large) and a night regression pass | open |

### D1 details (what the owner sees on the branch build)
- Settings > Display & Theme > BACKGROUND now shows two swatches: Dark and Day (BG 1, 2, 3 and Blue are not offered; an old saved choice falls back to Dark).
- Tapping Day switches the app's colour tokens, the Material scheme, the status and navigation bars (dark icons) and the page backdrop at once.
- EXPECTED and NOT a bug until D2 to D8: many screens still use literal white text or literal dark cards, so parts of the app look wrong in Day. Local web pages (notes, quiz, tutor ...) stay dark. Do not judge the whole look yet; judge the page colour, the cards drawn with the tokens, and the text colours.
- Night: must look exactly as before. Check Home, Learning, Packages, Settings in Dark after installing.

## 7. Publishing gate (recommended; owner to confirm)
Ship the first Play Store build with Dark only. Two safe ways: (a) publish from `main` (D1 is NOT merged there), where BG 2 is still offered; or (b) merge D1 with `AppBackground.DAY_MODE_ENABLED = false` (one word to change), which gives Dark only and removes BG 2 from the picker. Turn the sun on in an update after D9 is signed off. A half-finished day mode must not reach students.

## 8. Open question for the owner
Day REPLACES BG 2 (assumed above), or sits next to it?

## 9. Session log
- 2026-10-19 (Claude chat, plan): plan written from the owner's request after reading `Color.kt`, `AppBackground.kt`, `CLAUDE.md`, `docs/DAY_NIGHT_PLAN.md`, session logs B5, B6 and BG_FEEDBACK_1. No code changed.
- 2026-10-19 (Claude chat, D1): read `AppThemeMode.kt`, `Theme.kt`, `Color.kt`, `AppBackground.kt`, `DisplaySection.kt`, `BackgroundSection.kt`, `docs/BUILD_PITFALLS.md`, and searched `AppBackground.selected` users (only `AppBackdrop.kt`, `DisplaySection.kt`, `BackgroundSection.kt`). Wrote on branch `feature/day-mode-d1`: `AppThemeMode.kt` (isDark = chosen background is not Day), `AppBackground.kt` (Day option, `DAY_MODE_ENABLED`, options Dark + Day, Day hands web pages the Dark look), `Color.kt` (Ivory & Navy palette and day gradients), `Theme.kt` (light scheme with ivory pop-up roles). NOT done: D2 to D9. Not compiled; the tool cannot build. If the build is red, paste the line that starts with `e: file:///`. Hypotheses: Compose state read in `AppThemeMode.isDark` makes tokens follow the choice (same mechanism the BG already uses); the Day glow strengths (0.55 / 0.50) may need tuning on a phone.
