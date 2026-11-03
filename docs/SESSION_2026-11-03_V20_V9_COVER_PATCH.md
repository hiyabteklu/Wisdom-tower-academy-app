# Session 2026-11-03 V20 — V9 native cover until first paint: patch script written (Claude, chat)

Append-only. Updates the V9 row of `docs/SESSION_2026-11-03_V18_STATUS.md`.

## Cause (code read of the live `MainActivity.kt`, hypothesis for the visible flash, facts for the code)
- The main WebView and the tool-overlay WebView have a transparent background. Their loader was removed at 85 percent load progress (`onProgressChanged`), which can come before the first drawn frame, so the bare app background shows for a moment (blank / dark flash).
- The main WebView already releases the loader at `onPageCommitVisible` (first drawn frame), `onPageFinished`, `onPageReady`, `onRouteChanged`, and has hard timeouts (3.5 s and 4 s). The tool overlay had no `onPageCommitVisible` at all.
- The notes reader (`NotesReaderScreen.kt`) already hides its page until it reports ready (BG-NOFLASH); no change needed there.

## Patch (3 edits in `MainActivity.kt`, script `scripts/patch_v9_cover_until_paint.py`, marker `V9 cover until paint`)
1. Main WebView: the 85 percent rule now releases only the offline asset page; web pages wait for the first drawn frame.
2. Tool overlay: same, offline page only.
3. Tool overlay: new `onPageCommitVisible` releases its loader.
Start-up cover, fail-safe timeouts, page logic: untouched.

## Tested
Run twice on a copy of the real `MainActivity.kt` from `main`: first run "patched (3 edits)", second "already patched". Brace count balanced (1095/1095), parentheses keep the same difference as before (3). Not compiled, not seen on a phone.

## OWNER STEPS
GitHub > Actions > Apply pending patches > Run workflow > script `patch_v9_cover_until_paint.py`, branch `fix/v9-cover-until-paint` > wait for the build on that branch > test > merge the pull request.
Test: open a book or a study page from a hub, and a tool from the overlay (checkout or order pages): the round loader stays until the page is drawn, then goes; no empty or dark frame in between. With the phone offline the offline page still shows.

## Risk to know
If a page never fires the first-frame signal, the loader now stays until `onPageFinished` or the hard timeout (main WebView 3.5 s / 4 s). The tool overlay has no timeout of its own but `onPageFinished` and the error callbacks already clear it.

## Not done
Competition exam and cover-page pages were not changed in this slice; `docs/PENDING_PATCHES.md` was not edited (the entry is in this log).
