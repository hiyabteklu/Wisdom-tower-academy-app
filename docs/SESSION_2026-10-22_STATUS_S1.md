# SESSION 2026-10-22 STATUS S1: My Status V2 data layer (no UI)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_PLAN.md`. Rule: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`.

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `data/content/ResourceCatalogIndex.kt` (NEW): published `learning_resources` with `id, package_id, scope_path, hub, title, chapter, content_type, item_count`, subject key via the existing `ResourceScopeIndex.subjectKey` (that file is untouched), 24 h disk cache `status_catalog_index.json`, offline fallback.
- `data/content/ExamAttemptsReader.kt` (NEW): own `exam_attempts` (`resource_id, started_at, submitted_at, score, meta.total`), same auth/refresh/401-retry pattern as `StudentProgressReader`.
- `data/content/StudentProgressReader.kt`: `ProgressRow` gets `lastPage: Int? = null`, `flashcardsKnow: Int = 0`, `flashcardsSeen: Int = 0` (defaults, so no other call site breaks); the select adds `last_page`. File resent whole, nothing else changed.
- `ui/study/StatusBreakdown.kt` (NEW, pure functions): `totals`, `chapterStatus`, `subjectStatus`, `examStatus`, `subjectKeys`, `progressMap`. Unknown value = null (dash in the UI), never 0 or an estimate (G4).

DEFINITIONS USED: opened = progress row with progress > 0, or time > 0, or an opened date; completion = mean `progress_pct` over ALL published items of the group (unopened = 0); focus % = focus / study seconds; reading = hubs books + short-notes; watch = hub videos; accuracy = `meta.quiz` correct / attempted; flashcards known % = know / seen; exam % = score / `meta.total` (only when 0 <= score <= total); exam time = submitted - started.

LEFT: S2 next (`StatusRing` reusing the `DownloadGauge.kt` drawing, then `StatusDetailCard`). Nothing in the app calls the new code yet.

TEST: nothing to see on the phone. Owner: check that the GitHub Action build is green after this commit; if red, copy the error to the next agent and log it in `docs/BUILD_PITFALLS.md`.

UNSURE (hypotheses, not verified):
1. `exam_attempts.score` is the number of correct answers (the plan says `score` + `meta.total`). If it is already a percent, exam percentages will be wrong or null: check one real row first when S4 is built.
2. `learning_progress.progress_pct` is on a 0..100 scale (clamped to that).
3. Timestamps are parsed to the second and read as UTC; both ends of an exam have the same offset, so the duration is right, only the absolute time is not used.
4. `learning_progress.last_page` exists as a column (listed in the plan table); if the select returns HTTP 400 the status screen would show its error state: look at logcat tag `StudentProgress` first.
