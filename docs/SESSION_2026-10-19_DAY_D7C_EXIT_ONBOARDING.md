# SESSION 2026-10-19 — D7c: Exit dialog in day mode, Onboarding checked

> Repo is public: no secrets. Owner order: "Continue" (after D8d). One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## D7 cut into parts (files listed from `main`, 2026-10-19)
- **D7c (this one):** `OnboardingScreen.kt` (12.7 KB), `ExitGuiltDialog.kt` (5.4 KB), both in the root package.
- **D7d:** the small competition files in `ui/competition/`: `CompetitionGlass.kt`, `CompetitionPointsCard.kt`, `CompetitionQuestionView.kt`, `CompetitionReviewView.kt`, `CompetitionRulesScreen.kt`, `CompetitionTrophyIcon.kt` (3 to 7 KB each).
- **D7e:** `CompetitionExamScreen.kt` (17 KB), `CompetitionReviewScreen.kt` (10 KB).
- **D7f:** `CompetitionLeaderboardScreen.kt` (19 KB), `CompetitionsHubScreen.kt` (26 KB).
- **D7g and later (owner-only):** the native admin console in `ui/admin/`: 18 files, about 450 KB (`BroadcastComposerScreen.kt` 49 KB, `CompetitionsScreen.kt` 44 KB, `FreeResourcesScreen.kt` 42 KB, `UsersScreen.kt` 35 KB, and the rest). Students never see it, so it goes last and is cut into several parts. Also still open: the Android system splash (theme in `res/`, a protected area, not touched without the owner's word).

## What was found
- `ExitGuiltDialog.kt` (shown when the student presses Back on the home screen, so reachable by day): its card, border, buttons and text were fixed night literals (dark navy 70 percent card, white title, grey text, cyan button). By day it would have been a dark panel with white text among ivory pop-ups.
- `OnboardingScreen.kt`: draws its own fixed dark gradient, white titles and a cyan button. It runs once, on a first launch, and `onboarding_done` is saved in the app's preferences, as is the background choice. A fresh install (or cleared data) therefore always starts in Night, and the screen is never seen by day. I did NOT change it (no visible effect, only risk).

## What was written (one commit, small file resent whole)
`ExitGuiltDialog.kt`: the five private colour vals became getters with a day / night pair (helper `exitDayNight`). Night keeps exactly the old values: card `0C1424` at 70 percent, cyan hairline, `22E0FF` button (`WisdomCyan` equals it), `070D17` text on it (`WisdomDarkOnCyan` equals it), white title (`WisdomInk` equals it), grey `94A3B8` text. Day: frosted ivory 92 percent card, navy hairline, deep teal pill with white text, navy ink title, muted `4A5A70` text, outline pill with a navy tint and ink text. Layout, texts, sizes and behaviour unchanged. Only new imports: `AppThemeMode`, `WisdomCyan`, `WisdomDarkOnCyan`, `WisdomInk`, `WisdomMuted`, `WisdomTextPrimary` (all exist in `ui/theme`).

## Observation for the owner (NOT changed)
Onboarding page 4 says "Built-in AI support for every purchased course". The rule since 2026-10-11 is no purchase buttons or prices in the app; this is only a sentence, not a button, and the website text may be the source. Tell me if you want it reworded or removed.

## Not verified
Contrast of the outline pill text and the outline border on ivory (hand estimate; D9 measures).

## Owner: what to test (Settings > Display & Theme, Day)
Go to the Home tab and press the phone Back button: the "Exit Wisdom Tower Academy?" dialog should be a light ivory card with dark text, a dark teal Exit button (white text) and a light Stay in App button. Then Night: exactly as before.

## Next
D7d (small competition files).
