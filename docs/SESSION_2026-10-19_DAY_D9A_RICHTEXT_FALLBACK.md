# SESSION 2026-10-19 DAY D9a: day safety net in richtext.css

Owner (2026-10-19, with 3 phone screenshots): day mode "looks inverse and awkward". In the Packages note page ("The general feedback structure") headings are yellow, bold terms and table text are white, highlighted terms are white on pale teal. In a Home card ("Keeping friends without losing the semester" / "What everyone is doing really means") the text is white or very faint on pale pink. Owner idea: do not edit day mode file by file; change the white page into something that gives good contrast.

## What I checked (code read, not run)
- Page colour: `WisdomLightPalette.background` E9E5DC and `AppBackgroundOption.Day` (ivory plus two soft glows). Ink text 0B1B33.
- The note page is a local WebView page (`assets/notes/index.html` plus `richtext.css`). `richtext.css` hard-codes light colours (`.study-h1` fff, `.study-h2` 67e8f9, `.study-h3` fcd34d, `strong` fff, `em`, `mark`, `code`, `.katex` fff, table borders white).
- `AppBackdrop.kt` GLASS_JS_DAY should recolour those at run time. Why it missed them on the owner's phone: NOT found (hypothesis: timing or an element kind the script skips; needs a device check).

## Why a darker page alone does not fix it (arithmetic, hypothesis for look, exact for numbers)
A single mid-tone page that works for both white text and navy ink text has luminance about 0.20, giving about 4.2:1 for each. The bright yellow and cyan headings then fall to about 3:1 or lower, and the already converted native screens (ink text) lose contrast. Only a dark page fixes white text, and that is the night look.

## Change (one file, no page file touched)
`app/src/main/assets/notes/richtext.css`: appended block "D9a" under `html.wt-day` with `!important` dark day values for headings, strong, em, marks, code, pre, table borders and heads, KaTeX, Summary box. Night never gets `wt-day`, so night is unchanged. Covers notes, flashcards, quiz (shared file). Marked `[~]`: not built, not seen on a phone.
To go back: delete the D9a block.

## Not done
- The Home card with white text on pale pink looks like a native Compose card with `Color.White` text (or a faded animation frame). Screen name needed to find the file.
- Other local pages with their own inline `<style>` colours (tutor, competition pages) are not covered by this block.

## Owner test
Build, set Settings > Background > Day, open a note in a package: headings dark teal / amber, bold terms dark ink, table readable. Night: same as before.
