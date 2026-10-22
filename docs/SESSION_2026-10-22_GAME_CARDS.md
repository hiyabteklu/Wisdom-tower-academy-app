# SESSION 2026-10-22: Game tab cards, pictures and download icon

Owner order (2026-10-22, with two screenshots of the Game tab): the Game tab course and subject lists must use the same picture cards as the Learning tab, and game chapters must get the same download icon as question banks and notes. Push straight to `main` (free plan).

## What changed (native app, all in `main`)
- `ui/games/GameHubScreen.kt`: course step and subject step now use `OpenCard` + `openGrid` (two columns, 16:9 picture, title, Open), the same composables as `PackageDetailScreen`. Course picture = `NativePackage.assetImage`. Subject picture = looked up from the packed lists (`FRESHMAN_SUBJECTS`, `GRADE_*_SUBJECTS`, `REMEDIAL_SUBJECTS`, `ECE_SEM*_COURSES`) by `scope_path`. ECE titles show without the course code (owner rule 2026-10-13). Subjects with no packed picture (UAT, GAT, COC, Exit Exam) show the card without a picture, like the Learning tab. Chapter rows are glass cards with `Play` / `Coming soon` and the learning `DownloadGauge` (tap: save offline, tap when saved: remove, red retry icon on failure).
- `data/games/GameSetsRepository.kt`: new `isDownloaded`, `download`, `removeDownload`. A saved chapter is the same cache file `loadSet` already uses, so playing online also marks the chapter as saved, and offline play uses it.

## Not verified
- Not compiled, not run: `[~]`. Check CI. Hypotheses to check on the phone: `WisdomGlassCard` accepts a Row as content; `DownloadGauge(state=, progress=null)` shows a spinner while saving (no real percent, because one request has no byte progress).
- The "only list courses that have content" rule of `docs/GAME_COURSE_NAV_PLAN.md` is NOT done here: all enrolled courses still show.

## Owner test
Game tab > Match or Wisdom Defense > courses show pictures in two columns > subjects show pictures > chapter shows the download arrow > tap it: spinner then green check > airplane mode: the chapter still plays > tap the check: back to the arrow.
