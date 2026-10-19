# SESSION 2026-10-19 — BG-SEAM: one picture across header, page and bottom bar

> Repo is public: no secrets. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## Owner report (with a phone screenshot, BG 2 selected)
The picture background looks cut into three slices (header, main screen, bottom nav): they show different crops of the picture, the joints do not match, and the pieces paint late at start.

## Cause (code read, not run)
`MainActivity.kt` draws `.appBackdrop()` plus `AtmosphereBackground` over the whole window, but EVERY screen (Home, Learning, Packages, Settings, ...) draws its own `AtmosphereBackground(Modifier.fillMaxSize())` inside the Scaffold content, i.e. only the area between the top bar and the bottom bar. `appBackdrop` scaled the picture to "cover" the size of the box it was drawn in, so the page copy showed a different scale and crop than the window copy visible behind the bars. Hypothesis for the late painting: the page copy appears a frame after the window copy (screens mount after the shell) and the picture decodes on a thread the first time.

## Change (one file, `ui/theme/AppBackground.kt`, no screen file touched)
- `appBackdrop()` with no option now lays the colour, the picture, the shade and the glows out for the WHOLE window (size of the root layout) and shifts them by the box's position (`onGloballyPositioned`, `positionInRoot`, `findRootCoordinates`), clipped to the box. Every copy therefore shows the matching part of one picture, and the joints line up. Glows (Blue, Day) are aligned the same way.
- `appBackdrop(option)` with an option (used for small previews) keeps the old behaviour.
- The drawing code moved into `private fun DrawScope.drawBackdrop(opt, w, h)`; the contents are the old code with `w`/`h` instead of `size`.
- Before the first layout pass the box draws the old way for one frame (it does not know its place yet).

## NOT done / hypotheses
- Not compiled. Risk to check first if the build is red: imports `androidx.compose.ui.composed`, `androidx.compose.ui.layout.findRootCoordinates` / `onGloballyPositioned` / `positionInRoot`, `drawscope.clipRect` / `translate`.
- Late painting is not fixed at the source: the picture is still decoded the first time it is drawn. If the start still shows a bare colour for a moment, next step is to preload the picture in `WisdomTowerApplication`.
- Pop-ups, dialogs and sheets that are separate windows have their own root, so a backdrop drawn inside them is aligned to that window, not the main one.
- Local WebView pages use `cssBackground` (a CSS copy of the look); not touched.

## Owner: what to test
Settings > Background > BG 2: Home, Learning, Packages, Settings. The picture should run unbroken behind the top bar, the page and the bottom bar, with no step at the joints. Scroll a page: the picture stays still. Open the keyboard in a text field: no jump. Also check Dark and Day look as before.
