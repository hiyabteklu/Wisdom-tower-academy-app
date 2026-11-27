# ECE website thumbnails: file names (Phase C)

These 15 files are website-only. The Android app now draws ECE covers with CodeCover and does not copy or load these files.

## Folder
- Website repo: `public/images/special-packages/`
- In this repo the synced copy is `website-reference/public/images/special-packages/`
- They are not bundled in `app/src/main/assets/`.

## The 15 file names
Year cards (Home and Learning > Curriculum):
`ece-y1.jpg`, `ece-y2.jpg`, `ece-y3.jpg`, `ece-y4.jpg`, `ece-y5.jpg`

Semester / package covers:
`ece-y1-sem-1.jpg`, `ece-y1-sem-2.jpg`, `ece-y2-sem-1.jpg`, `ece-y2-sem-2.jpg`, `ece-y3-sem-1.jpg`, `ece-y3-sem-2.jpg`, `ece-y4-sem-1.jpg`, `ece-y4-sem-2.jpg`, `ece-y5-sem-1.jpg`, `ece-y5-sem-2.jpg`

## Size
Same pixel size and same width:height ratio as the existing `ece-sem-1.jpg` (open its properties and copy them). UNSURE: the exact pixel size was not read.

## What happens when a file is missing
The app uses the helper `eceCoverAsset` (`ui/packages/EceCoverAsset.kt`): new name if it exists, else the old one (`ece.jpg`, `ece-sem-1.jpg`, `ece-sem-2.jpg`). The old files are never deleted. A missing new picture never shows a broken image.

The Gradle task `warnMissingEceImages` (`app/build.gradle.kts`) prints ONE warning line in the build log that lists which of the 15 files are missing from `website-reference/public/images/special-packages/`. It only warns and never fails the build, so every build prints it until all 15 files are added.

## Where the app uses the helper (Phase C step 2, pushed 2026-11-23)
- `PackagesData.kt`: the 10 ECE package covers, `eceSemesterCover(year, sem)`.
- `HomeScreen.kt`: the 5 ECE year cards, `eceYearCover(1..5)`.
- `PackageDetailScreen.kt`: the semester cards of every year, `eceSemesterCover(year, sem)`.
- `EceTrackCover.kt`: `ECE_TRACK_COVER_ASSET` is now `eceYearCover(3)` (UNSURE: that the whole-track card should show the Year 3 picture).
- NOT done: Learning > Curriculum still shows that one picture for every ECE package (owner decision: use each package's own year picture?).

## Needs owner (not edited by the agent)
The website's own references still point to the OLD names:
- `website-reference/src/data/special-packages.ts`
- `website-reference/src/data/packages.ts`
- `catalog_items.image` in the database

## Website repo changes
`website-reference` is only a snapshot of the website repo, so edits made here would be overwritten by the next "Sync website reference". Phase 5 of `docs/ADMIN_ECE_Y1_Y5_PLAN.md` was therefore NOT done in this repo. The owner (or an agent working in the website repo) mirrors the app's admin tree there. Same data as the app: `ECE_TREE_YEARS` in `app/src/main/java/com/wisdomtower/academy/ui/packages/EceTreeData.kt`, and the generated tree in `app/src/main/java/com/wisdomtower/academy/data/admin/AdminContentTree.kt` (commit `cebf194`).
Files to change in the website repo (UNSURE: none of them was opened, the list is taken from the plan):
- `src/data/admin-nav.ts`: add the semesters of Years 1, 2, 4, 5 under Special packages. Scope `ece/<yN-sem-M>/<slug>`, package id `ece-yN-sem-M`, each course once per semester (union of streams), six hubs each. Year 3 unchanged.
- `src/components/admin/LocksPanel.tsx`: `PACKAGE_SHORTCUTS` gets the 8 new package ids.
- `src/data/packages.ts` and `src/data/special-packages.ts`: the 8 packages, and the new thumbnail names (see above).
- `src/components/admin/NotificationAudiencePicker.tsx`: the 8 new package ids.
