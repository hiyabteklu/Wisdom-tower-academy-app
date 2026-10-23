# SESSION 2026-10-23 TAB HIGHLIGHT (owner bug report, both builds)

Bug (owner): opening a package from Home or My Learning highlighted the Game tab, and Back landed on the games list, not where the student came from.

Cause (read in `MainActivity.kt`): the package page (`PackageDetailScreen`) is drawn inside tab layer 2, which is the Game tab (`BottomNavItem.Packages`, title "Game"). Opening a package set `selectedIndex = 2` (in `handleGlobalNav` and in `onSelectCourse` of the Learning tab). Back set `selectedPackage = null` and stayed on tab 2, so the games list showed.

Fix (patch script, NOT yet applied): `scripts/patch_package_tab_highlight.py`, tested on a copy (applies cleanly, second run does nothing). It remembers the tab the student came from (`packageOriginTab`), highlights that tab in the bottom bar while a package page is open, and returns there on page Back and phone Back. Tapping the highlighted tab while a package is open leaves the package. A package opened from the Game tab itself behaves as before.

NOT compiled, NOT run on a phone. To apply: GitHub > Actions > Apply pending patches > Run workflow > script `patch_package_tab_highlight.py` + a new branch name (see `docs/APPLY_PATCHES_WORKFLOW.md`), wait for green, test: Home > package > Learning (highlight stays Home) > Back > Home; Learning > package > Back > Learning; Game > package > Back > Game list. Then merge with a Pull request.

Known limit: the package page is still drawn on the Game tab layer (only the highlight and the return are fixed). A package opened from Learning that the student leaves by tapping the Game tab shows the games list, as expected.
