# SESSION 2026-10-19 — D7e: competition exam and review screens in day mode

> Repo is public: no secrets. Owner order: "Continue". One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## Files (all in `app/src/main/java/com/wisdomtower/academy/ui/competition/`)
- `CompetitionExamScreen.kt` (17 KB), CHANGED in one place. The exam itself is the local page `competition_exam.html` (covered by the D8 script). The only native colours are in the "You left the exam window" dialog: card was fixed dark 0F1829 at 95 percent with a white title. Night keeps exactly that. Day: ivory FBF9F4 at 95 percent and the title in `WisdomInk`. Body text (`WisdomMuted`) and the rose button were already mode aware. New imports: `AppThemeMode`, `WisdomInk`. No logic touched (clock, autosave, lockdown flag, submit, flags all copied unchanged).
- `CompetitionReviewScreen.kt` (10 KB), CHANGED. Three top-level private vals (cyan, muted 94A3B8, red F87171) became getters (a top-level val is read once at class load and would not follow the switch). Night values identical. Day: `WisdomCyan` (deep teal), `WisdomMuted`, red B91C1C. Two `Color.White` text lines (screen title, "Question n of m") became `WisdomInk`. New imports: `AppThemeMode`, `WisdomCyan`, `WisdomInk`, `WisdomMuted`. Logic unchanged.

## Not verified
- Not compiled. Both files were resent whole; only the lines above differ from `main` before this slice.
- Contrast of B91C1C on ivory and of the dialog text on the 95 percent ivory card: hand estimates, D9 measures.
- The exam and review PAGES look in day was only read in D8, never seen on a phone.

## Owner: what to test (Day, then Night)
1. Open a contest review: title and "Question n of m" in navy, score line in teal, Back / Previous / Next pills readable, the question card from the page in ivory glass.
2. Ranked exam only: leave the app and come back: the warning dialog is an ivory card with a navy title, grey-blue text and a red "I understand".
3. Night: both exactly as before.

## Next
D7f: `CompetitionLeaderboardScreen.kt` (19 KB) and `CompetitionsHubScreen.kt` (26 KB; gold for the three trophies could use the plan's gold values). The DAY_MODE_PLAN board row still says D7e is open: the next chat should update it (D7d and D7e).
