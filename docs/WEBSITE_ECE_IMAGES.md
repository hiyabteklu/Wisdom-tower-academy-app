# ECE thumbnails: file names (Phase C)

The owner uploads these 15 files to the WEBSITE repo. The app build copies them (task `copySpecialPackageImages`, do not change it).

## Folder
- Website repo: `public/images/special-packages/`
- In this repo the synced copy is `website-reference/public/images/special-packages/`
- In the app they appear as `file:///android_asset/images/special-packages/<name>`

## The 15 file names
Year cards (Home and Learning > Curriculum):
`ece-y1.jpg`, `ece-y2.jpg`, `ece-y3.jpg`, `ece-y4.jpg`, `ece-y5.jpg`

Semester / package covers:
`ece-y1-sem-1.jpg`, `ece-y1-sem-2.jpg`, `ece-y2-sem-1.jpg`, `ece-y2-sem-2.jpg`, `ece-y3-sem-1.jpg`, `ece-y3-sem-2.jpg`, `ece-y4-sem-1.jpg`, `ece-y4-sem-2.jpg`, `ece-y5-sem-1.jpg`, `ece-y5-sem-2.jpg`

## Size
Same pixel size and same width:height ratio as the existing `ece-sem-1.jpg` (open its properties and copy them). UNSURE: the exact pixel size was not read.

## What happens when a file is missing
The app uses the helper `eceCoverAsset` (`ui/packages/EceCoverAsset.kt`): new name if it exists, else the old one (`ece.jpg`, `ece-sem-1.jpg`, `ece-sem-2.jpg`). The old files are never deleted. A missing new picture never shows a broken image.

## Needs owner (not edited by the agent)
The website's own references still point to the OLD names:
- `website-reference/src/data/special-packages.ts`
- `website-reference/src/data/packages.ts`
- `catalog_items.image` in the database

## Website repo changes
(Phase 5 will list the website admin mirror edits here.)
