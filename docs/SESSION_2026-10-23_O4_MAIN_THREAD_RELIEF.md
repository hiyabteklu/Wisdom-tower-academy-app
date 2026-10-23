# SESSION 2026-10-23 O4: main-thread relief (onboarding and start-up plan, slice O4)

Plan: `docs/ONBOARDING_STARTUP_PLAN.md`. Follows `docs/SESSION_2026-10-23_O3_STARTUP_COVER.md`.

Owner bug: right after opening, quick scrolling or quick tab switching freezes the app once.

Cause (read in `MainActivity.kt`, hypothesis H1 of the plan, not proven): in the main WebView's `onPageFinished`, the whole page text is read with `outerHTML`; the result callback runs on the UI thread, parses it with `JSONTokener` and calls `WebCacheVault.saveHtmlPage`, which writes the page to disk under up to four keys. Seven scripts are then injected and four of them again 800 ms later. Quick tab switching repeats this for every page.

Fix (patch script, NOT yet applied): `scripts/patch_o4_main_thread_relief.py`, tested on a copy of the real `MainActivity.kt` (2 edits, applies cleanly, second run does nothing).
- The parse, the checks and `saveHtmlPage` now run on `WebCacheVault.io` (the vault's own pool). Same conditions and same result on disk, only the thread changes. `WebCacheVault.index` is a ConcurrentHashMap and `save` already uses the same pool for `persistIndex`.
- The second 800 ms injection round is skipped when the page changed in the meantime (compared without the `#` part).

Still on the main thread (unavoidable with `evaluateJavascript`): the page text arriving from the WebView. If the freeze remains after O3 and O4, the next suspects are the first-start picture decode and the first injection round; then a logcat or an "App not responding" screenshot with the time is needed.

NOT compiled, NOT run on a phone. To apply: GitHub > Actions > Apply pending patches > Run workflow > script `patch_o4_main_thread_relief.py` + a new branch name. O3 must be applied first or in the same branch order (both edit `MainActivity.kt`, different places; each anchor is checked, so either order works; if the second run says an anchor was not found, tell the next agent).

Owner phone test: open the app, right away tap tabs fast and scroll hard 10 times; no freeze. Open a page, then go airplane mode: the page opened online still opens offline (cache still saved).

NEXT: O5, the owner's phone report. Then fix from it.
