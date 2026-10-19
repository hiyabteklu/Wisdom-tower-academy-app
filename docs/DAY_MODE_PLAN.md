# DAY_MODE_PLAN.md — premium day mode "Ivory & Navy" (owner order 2026-10-19)

> Repo is PUBLIC: no secrets, no student data. Planning only: NO code was changed in this session. Free-plan rule: ONE slice per chat (`docs/AGENT_QUOTA_PROTOCOL.md`).

## 1. Owner words (2026-10-19)
The custom background BG 2 is awkward. Keep the dark mode as it is and design a compatible plan for day mode: not white, something premium that elevates the app. The app is being prepared for publishing.

## 2. Decision and what it replaces
- This brings day mode BACK. It overrides the cancel of 2026-10-17 (`CLAUDE.md`, `docs/DAY_NIGHT_PLAN.md`) for day mode only. Night stays exactly as it is.
- The cancel reason was real: the glass cards are see-through dark panels and do not work on a light page. So day cards are NOT see-through. They are frosted ivory (about 80 to 88 percent opaque) with a hairline border and a soft navy shadow. This is the key rule of the whole plan.
- The old pale day palette in `DAY_NIGHT_PLAN.md` (page F4F7FB, white cards) is NOT used. Ignore its slices L1 to L9. The slices below are D1 to D9.
- ASSUMPTION (owner did not answer yet; hypothesis, change on the owner's word): Day REPLACES BG 2 in the two-icon pill (moon = Dark, unchanged; sun = Day). `bg_2.jpg` stays in the repo, unused (nothing is deleted).

## 3. Verified facts (code read 2026-10-19)
- `AppThemeMode.isDark` is forced true. Every colour token in `ui/theme/Color.kt` is a mode-aware getter (`pick(dark, light)`), so the switch plumbing still exists. The light values in `WisdomLightPalette` are the old pale ones and get replaced in D1.
- `ui/theme/AppBackground.kt`: options Dark (`Black`, pure black), BG 1/2/3 (pictures), Blue; offered in the picker: Dark and BG 2 only. BG 2 = `assets/backgrounds/bg_2.jpg` (270 KB) with a 35 percent black shade (`PHOTO_SCRIM_ALPHA`).
- The glass for local WebView pages is a script in `ui/theme/AppBackdrop.kt` (reads page colours, paints glass). It assumes light text on a dark page, so D8 needs a day variant.
- NOT verified: what BG 2 looks like (the chat tool cannot view pictures). Hypothesis for "awkward": the photo competes with text, like the first galaxy photo did (`docs/SESSION_2026-10-17_B6_GALAXY_PHOTO.md`).

## 4. Design: Ivory & Navy (values are the starting point; the owner sees them on a phone in D1)
| Token role | Day value |
|---|---|
| Page | ivory-stone E9E5DC, plus two very soft glows (pale sky at the top, champagne at the bottom), low alpha, drawn by `appBackdrop` like the Blue option |
| Card | frosted ivory F8F5EE at about 82 percent, border navy 12 percent, soft navy shadow |
| Overlays (nav, top bar, drawer, dialogs, sheets) | the same ivory at about 92 percent (owner feedback 2026-10-17: overlays need more opacity) |
| Text | deep navy ink 0B1B33 (never pure black); muted 4A5A70 |
| Accent | deep teal-cyan 0B7285 (white text on it); night keeps 22E0FF |
| Gold | premium moments only (badges, leaderboard, Pro): fill F1DFB4 with dark gold text 5C3F0A; gold is never body text |
- Contrast figures are my hand estimates (ink on card about 15:1, muted about 6:1, accent about 5:1): hypothesis, to be measured in D9. Normal text 4.5:1, large text and icons 3:1, in BOTH modes.

## 5. Rules for every slice
1. Night must stay identical. Every slice ends with a night check of the screens touched.
2. Reuse the sweep rules in `docs/DAY_NIGHT_PLAN.md` section 3 (no `Color.White` / `Color.Black` for text; use `WisdomInk`, `WisdomOnInk`, `WisdomTextPrimary`, `WisdomMuted`; never read a token once at class load; literal dark surfaces become tokens and the text on them flips in the same slice).
3. Big files (`MainActivity.kt`, `SettingsScreen.kt`, `LearningScreen.kt`, over about 30 KB): patch script plus the Action (`docs/EDITING_LARGE_FILES.md`). Real Material icons only. No new dependency or host. Forbidden files stay forbidden (`HANDOFF.md` rule 12).
4. Written code is marked `[~]` until CI is green and the owner has tested it.

## 6. Slices
| Slice | What | Status |
|---|---|---|
| D1 | Foundation: replace the day palette in `Color.kt`; switch driven by the saved choice (default night); picker pill moon / sun (an old saved BG choice falls back to Dark); real light Material scheme and status bars in `Theme.kt`; day backdrop in `AppBackground.kt` | open |
| D2 | Glass and surface tokens for day (`WisdomGlass.kt`, card gradient, field surface, message card), shared cards, overlays and pop-ups | open |
| D3 | Home, Learning, Packages, Package detail, Settings, My Subjects, GPA card, Study workspace | open |
| D4 | Top bar, bottom nav, drawer (`MainActivity.kt` / `StructuralNav.kt`, patch script) | open |
| D5 | Account, native sign-in, forgot password | open |
| D6 | Study tools, readers, hub lists, tutor, PDF reader, dialogs | open |
| D7 | Competitions, native admin console, onboarding, splash | open |
| D8 | Local WebView pages (notes, quiz, flashcards, tutor, math): day variant of the glass script and page CSS; math, highlights and code blocks need day colours | open |
| D9 | Full contrast audit with owner screenshots in both modes (all tabs, TalkBack, font Large) and a night regression pass | open |

## 7. Publishing gate (recommended; owner to confirm)
Ship the first Play Store build with Dark only (BG 2 removed from the picker, no sun icon). Turn the sun on in an update after D9 is signed off. A half-finished day mode must not reach students.

## 8. Open question for the owner
Day REPLACES BG 2 in the pill (assumed above), or sits next to it as a third icon?

## 9. Session log
- 2026-10-19 (Claude chat): plan written from the owner's request after reading `Color.kt`, `AppBackground.kt`, `CLAUDE.md`, `docs/DAY_NIGHT_PLAN.md`, session logs B5, B6 and BG_FEEDBACK_1. No code changed. Next: the owner says "do D1".
