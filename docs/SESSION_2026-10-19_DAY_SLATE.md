# SESSION 2026-10-19 — DAY-SLATE: Day is a slate-blue page with the night colours

> Repo is public: no secrets. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## Owner order
The ivory Day look showed white text on a light page and black thumbnails on ivory (owner: "inverse and awkward"). Owner: "what it takes to change the white background in day mode without going through every page's code", then "continue with #52667F".

## What I found (code read)
`AppThemeMode.isDark = !AppBackground.selected.isLight`. Every colour token, the Material scheme, the status bars and the local-page scripts follow that one flag. So the cheapest fix is not a new palette: keep the flag false for Day and change only the page colour.

## Change (one file, `ui/theme/AppBackground.kt`; only the `Day` entry and doc comments differ from the BG-SEAM version)
- `Day`: base `52667F` (was ivory `E9E5DC`), glows: a faint lighter one at the top (`8FAAC9`, alpha 0.22) and a deeper one at the bottom (`2B3C57`, alpha 0.45), `isLight = false` (was true).
- Result: Day now uses the NIGHT tokens (white text, see-through dark glass cards, night WebView script, dark status bars) over a slate page. Nothing else was touched.
- Hand arithmetic (hypothesis, not measured): white on 52667F about 5.9:1; white on a night glass card over it (about 2E3B50) about 11:1; the muted text colour AAB6C8 directly on the page is weak (about 2.9:1), inside cards it is fine; cyan 22E0FF text directly on the page about 3.7:1.
- The ivory work D1 to D8 (Color.kt, WisdomGlass.kt, MainActivity.kt, AppBackdrop.kt day script, richtext.css D9a block) is untouched and unused while `isLight` is false. To go back: see the comment on the `Day` entry.

## Not done / notes
- The status and navigation bars are the night navy (solid dark) because `WisdomNavy` follows the night palette; if they look heavy against the slate page, a small follow-up can tint them.
- To try another colour, change the first colour of `Day` only.

## Owner: what to test
Settings > Background > sun (Day): slate-blue page, white text, dark glass cards, thumbnails look right. Check Home, Learning, Packages, a note page, a quiz, the AI Tutor. Send screenshots of anything hard to read.
