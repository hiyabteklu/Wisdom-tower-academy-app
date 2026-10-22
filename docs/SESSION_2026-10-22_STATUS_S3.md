# SESSION 2026-10-22 STATUS S3: "By subject" panel in Your status

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_PLAN.md`. Rule: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`.

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `ui/study/StatusSubjectsPanel.kt` (NEW): `StatusSubjectsPanel(rows, educationLevel)`. Loads `ResourceCatalogIndex` + `ExamAttemptsReader`, builds `StatusBreakdown.subjectStatus` per subject (T8 picks; no picks = every subject with activity), one row per subject: mini ring (completion), name (from `MySubjectsStore.packages`, fallback = slug), "X of Y items opened", small gauge icon. Tap row or icon -> `StatusDetailCard`. Loading spinner; failed read -> `WisdomMessageCard` with Retry; empty -> "Nothing to show yet."
- `ui/study/NativeStatusScreen.kt`: ONE inserted line (the call) after the Active Streak card. Nothing else changed.

LEFT: S4 next (chapter list inside the subject card, chapter card, exam attempts list + exam card).

TEST (phone, after a green build): Learning > Your status > scroll below the four cards: "By subject" shows your subjects with rings; tap one: the card slides up over a dim background; tap outside or the X to close. A subject you never opened shows an empty ring with "—" or 0%.

UNSURE (hypotheses, not verified):
1. `MySubjectsStore` subject keys equal `ResourceScopeIndex.subjectKey` output (both lowercase `package/slug`); if a picked subject never shows, compare the two.
2. `WisdomMessageCard` named params `message`, `isError`, `onRetry` are copied from the existing calls in `NativeStatusScreen.kt`.
3. Exam read failing hides the whole panel behind Retry (so "Exams taken" is never a false 0).
