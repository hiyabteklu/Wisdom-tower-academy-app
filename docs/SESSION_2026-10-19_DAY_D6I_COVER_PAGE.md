# SESSION 2026-10-19 — D6i: Cover Page Generator (day mode)

> Repo is public: no secrets. Owner order: "Continue" (after D6h). One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## What was found (read from `main`, 2026-10-19)
`ui/study/NativeCoverPageScreen.kt` (30 KB) already used the theme tokens for its cards (`WisdomGlassCard`), muted text, the cyan accent, borders and pills, so most of it follows the Day / Night switch since D1 and D2. What was still fixed to night: `Color.White` for the "Group members" label, the template name labels and the field text; a white ring around the selected colour swatch; the logo box fill (white 8 percent); the text on a filled pill (`04121F`, dark, meant for the night cyan); the gold label of premium templates (`E8C766`). `CoverPageRenderer.kt` (cover art, PDF) was not opened and was not touched.

## What was written (file resent whole, commit `41977cc`; layout, texts, form state, preview and PDF code copied unchanged)
- Private `dayNight(night, day)` plus four getters (`CvRing`, `CvLogoBoxFill`, `CvPremiumInk`, `CvPillOnInk`). Night = the exact old literals.
- Day: white texts and field text = `WisdomInk` (labels, template names, the typed text, the field's focused / unfocused text colour); swatch ring = ink; logo box = quiet glass; filled pill text = white (the day cyan is the deep teal); premium "★ name" label = dark gold `5C3F0A`; outline pills = ink text.
- `WisdomInk` is white at night, so night is the same as before.
- NOT changed on purpose: the white sheet behind the live preview and behind each template thumbnail (a cover page is white paper in both modes); the cover art and the PDF.
- New imports: `AppThemeMode`, `WisdomGlassFillQuiet`, `WisdomInk` (exist in `ui/theme`).

## Owner: what to test (both modes: Settings > Display & Theme)
1. Learning > Cover Page: the style card (template names, the premium ones with a star, the selected template ring), the colour dots (selected one has a clear ring), the Border pills (selected = solid teal with readable text).
2. The preview stays a white page; change template, colour and border: it redraws.
3. Logo: empty box with "LOGO", "Choose logo" button; pick a picture.
4. Fields: type in University, Department, Title, Group name, a member, Submitted to: the typed text and the labels are readable; the date field and its calendar icon.
5. Group / Individual pills, "+ Add member", "Clear" (outline) and "Download PDF" (solid).
6. Download a PDF: it must look the same as before in both modes.
7. Night looks exactly as before. If CI is red, send the error text.

## Not verified (hypotheses)
- `DatePickerDialog` is a system dialog and follows the phone theme, not the app switch (same as before).
- White on the day teal pill was not measured (D9 will).
- The D3 part 3, D4 and D5 patch scripts may still not have been run (see the D6a, D6f logs).

## Still open in D6 (D6j onwards)
`NativeTutorScreen.kt` (38 KB); `DownloadGauge.kt` unchecked.

## Board note
`docs/DAY_MODE_PLAN.md` still ends at D6a; D6b to D6i live in their session files until a later slice folds them in.

## Next
D6j: Tutor. The owner says "Continue".
