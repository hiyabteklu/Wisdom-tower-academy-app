# SESSION 2026-10-22 STATUS S2: My Status V2 ring + subject card (not wired)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_PLAN.md`. Rule: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`.

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `ui/study/StatusRing.kt` (NEW): `StatusRing(value 0..1 or null, color, diameter, strokeWidth, textSize, centerText)` = the `DownloadGauge.kt` arc drawing (track + round-cap arc from -90 degrees, 250 ms ease), null = muted empty ring with a dash. `statusRingColor(pct)`: >= 75 emerald, >= 50 amber, below rose, null muted (website HUD thresholds, day/night like `NativeStatusScreen.kt`).
- `ui/study/StatusDetailCard.kt` (NEW): `StatusDetailCard(title, breakdown: SubjectBreakdown, onDismiss)`: Dialog with dim scrim + bottom card. Hero ring = completion, items opened X of Y; small rings accuracy / focus / flashcards known; rows study / reading / watch time, exams taken, best / latest / average score, last active, last PDF page. 0 seconds or unknown = dash. Colours are theme tokens (`WisdomInk`, `WisdomMuted`, `WisdomGlass*`) + `MaterialTheme.colorScheme.surface` for the card body.
- No existing file touched. Nothing calls the new composables yet.

LEFT: S3 next (new `StatusSubjectsPanel.kt` + one inserted call in `NativeStatusScreen.kt`): load `ResourceCatalogIndex`, `ExamAttemptsReader`, progress rows, build `StatusBreakdown.subjectStatus(...)` per picked subject, one row per subject with a mini ring and a small gauge icon, tap -> `StatusDetailCard`. Subject display name from `PackagesData.kt`.

TEST: nothing to see on the phone yet. Owner: check that the GitHub Action build is green after this commit; if red, copy the error to the next agent and log it in `docs/BUILD_PITFALLS.md`.

UNSURE (hypotheses, not verified):
1. `MaterialTheme.colorScheme.surface` follows day / night (D1); if the card looks wrong in one mode, swap for a glass token.
2. `Dialog` with `usePlatformDefaultWidth = false` may leave a thin gap at the screen edge on some phones; harmless.
3. S1 build (#960) was still running when S2 was written; if it fails, fix S1 first.
