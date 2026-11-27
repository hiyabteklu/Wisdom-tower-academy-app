# Code covers (owner request 2026-11-27)

Goal: draw the 16:9 Canva covers with code so the app carries no picture file for them (smaller app, new subjects need no new image).

## How it works (central: no screen was edited)
- `OpenCardLayout` (`ui/packages/OpenCard.kt`, THE card used everywhere) calls `isCodeCover(image)` / `codeCoverLabel(image)`. If true it draws `CodeCover(label)` in the 16:9 slot, else it loads the picture through Coil as before.
- `codeCoverText(model)` in `ui/packages/CodeCover.kt` decides. It returns a label for:
  - `code-cover://ece-y<1..5>` and `code-cover://ece-y<1..5>-sem-<1..2>` (`eceYearCover`, `eceSemesterCover` in `EceCoverAsset.kt`);
  - bundled `file:///android_asset/images/...` pictures in these folders ONLY: `packages`, `hubs`, `freshman`, `academy`, `special-packages` (incl. `courses`), `web/grade-N`, `web/remedial`, `web/entrance`, `web/uat-hubs`. The title comes from the file name (hash suffix and `gN-` prefix removed, capitals); hand-written titles are in `PACKAGE_LABELS`, `FRESHMAN_LABELS`, `COURSE_LABELS`.
  - NEVER matched (stay pictures): the Home welcome card (`images/home/academy.jpg`, cached banner file, remote banner), remote https pictures, any other folder (`banks`, `brand`, `home`, guides, ...).
- Label format `TITLE` or `TITLE\nCAPTION`: the title wraps and shrinks to fit, the caption is one small line (grade, "SEMESTER 1", course code, ...).
- Only fonts already in `res/font`. No new dependency, no new file in assets. All old picture files are still in the app (not deleted).

## Add a new subject cover
Nothing to draw: give the card an image path in a covered folder (the title comes from the file name) or add a line to the label tables in `CodeCover.kt`. Better: pass `code-cover://<id>` and add the id to `codeCoverText`.

## State
- Slice 1 (Year 1): owner saw it on a phone and approved (2026-11-27).
- Slice 2 (Years 2 to 5, merge to `main`): pushed 2026-11-27.
- Slice 3 (everything that goes through `OpenCard`): pushed 2026-11-27, NOT compiled, not seen on a phone.
- NOT done / UNSURE (next agent: check on the phone, then fix):
  - Screens that show a cover WITHOUT `OpenCard` / `OpenCardLayout` still show the old picture: candidates are `ui/learning/LearningScreen.kt`, `ui/study/StudyWorkspaceScreen.kt` and `HubItemListScreen.kt` (hub cards), `ui/guides/GuidesScreen.kt`. Find them by looking for `AsyncImage` of a 16:9 cover, then route them through `CodeCover` the same way.
  - `ECE_TRACK_COVER_ASSET` (`EceTrackCover.kt`) is `eceYearCover(3)`: a `code-cover://` string. If a screen passes it straight to Coil it shows blank.
  - Labels are derived from file names and were never seen: some may read badly (list is in the tables above, fix by editing them). Entrance Questions / `ENTRANCE_IMAGE_ASSET` file name not read.
  - All covers use cyan/magenta. Per-group accent colours (idea) are not done.
  - The title letters are Outfit Black squeezed sideways, not the exact Canva font (a condensed font such as Anton needs a .ttf in `res/font`).
- LAST slice (only after the owner has checked every cover on the phone): delete the unused picture files from `website-reference/public/images/...` and the app assets, and drop the `copy*Images` tasks that become empty. Never delete in the same commit that switches a screen.
- To undo everything: make `isCodeCover` return false.
- Note: the Canva file for year 3 says "3ND"; the code label is "3RD".
