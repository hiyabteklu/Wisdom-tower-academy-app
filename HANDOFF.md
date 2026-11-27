# Handoff (current status only)
Done: Slices 1-5 + AI_STUDIO_NEXT Tasks A & B (verified web leftovers against Gradle/CI; checked Tinos assets/fonts usage — deleted nothing per decision rule).
Next: Owner decides whether copyWebFonts/richtext.css should load Tinos from res/font before deleting assets/fonts. Then docs/ECE_STATUS.md.
Blocked: none.

## Verification (A)
Checked `settings.gradle.kts`, `app/build.gradle.kts`, `.github/workflows/*.yml`:
- `server.js`: web-only (not named in Gradle or CI workflows)
- `src/`: web-only (not named in Gradle or CI workflows; Android uses `app/src/`)
- `index.html`: web-only (root file not named in Gradle or CI; Android uses `app/src/main/assets/notes/index.html`)
- `vite.config.ts`: web-only (not named in Gradle or CI workflows)
- `package.json`: web-only (not named in Gradle or CI workflows)
- `tsconfig.json`: web-only (not named in Gradle or CI workflows)

## Tinos (B)
Deleted NOTHING (`assets/notes/richtext.css` loads `../fonts/tinos_*.ttf` -> `app/src/main/assets/fonts/tinos_*.ttf`, and `copyWebFonts` in `app/build.gradle.kts` copies only `plus_jakarta_sans_*` and `outfit_*`):
- `app/src/main/assets/notes/richtext.css:17-20`: `@font-face { font-family: "WT Tinos"; ... src: url("../fonts/tinos_regular.ttf")` (and `tinos_italic.ttf`, `tinos_bold.ttf`, `tinos_bold_italic.ttf`)
- `app/src/main/java/com/wisdomtower/academy/ui/theme/Type.kt:30-34`: `val TinosFontFamily = FontFamily(Font(R.font.tinos_regular...), ...)`
