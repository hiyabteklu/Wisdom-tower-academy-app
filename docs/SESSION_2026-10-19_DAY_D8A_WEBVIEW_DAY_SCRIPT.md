# SESSION 2026-10-19 — D8a: day script for the local WebView pages (notes, flashcards, quiz, tutor, competition)

> Repo is public: no secrets. Owner order: "Continue" (after D6l; I had suggested D8 before D7). One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## Why
The local pages (`assets/notes/*.html`) are written for a dark page: light text, navy panels. `AppBackdrop.clearPageBackground` (called by every one of those screens) installed only the NIGHT glass script, so by day they would show light text on the ivory page. One shared place, so one fix covers every page (same idea as the old BG-WEB slice).

## What was written (commit `69c997b`, `ui/theme/AppBackdrop.kt` resent whole; night script copied unchanged)
- New `GLASS_JS_DAY` next to `GLASS_JS`. `clearPageBackground` picks the day script when `AppThemeMode.isDark` is false. Night = the old script, byte for byte.
- Panels: navy fills become ivory glass (84 percent), pop-up panels ivory 97 percent, the pop-up dim is navy 50 percent, full-screen wrappers transparent, inline dark chips a navy 8 percent tint.
- Text: on the page or on a dark panel, near-white text becomes deep navy ink (alpha kept, at least 0.75), grey-blue becomes muted 4A5A70, light colours (cyan, green, rose, amber) keep their hue at 27 percent lightness. Text on a coloured non-navy fill (green button, cyan chip) is left alone.
- Borders: whitish hairlines on those elements become navy 16 percent.
- Same restore-before-every-pass design as the night script (selected / correct / wrong states keep working); on streaming pages (tutor) it runs at most every 120 ms.
- Syntax of the day script was checked with Node (no dollar sign, no triple quote, parses).

## NOT done / hypotheses (D8b onwards)
- Not run in a browser or on a phone. Logic of the colour mapping is my reading of the code, not a measurement.
- SVG `fill` / `stroke` attributes and icons drawn with fixed light colours are not remapped (only CSS `color`, so `currentColor` icons follow).
- Heading text that uses a gradient clip (transparent text colour) is skipped: it may stay light by day.
- Colours that `richtext.css` sets on code blocks, highlights (`==text==`), tables and math are only handled if they are navy fills or light text. A light-coloured code theme or a yellow highlight with light text may still need `richtext.css` edits.
- Whether the day glass strength (84 percent) looks right over the day backdrop (the old 16 percent figure was for night).
- `competition*.html` pages: only covered through this script; not opened.
- Pages are classified when they finish loading; switching Day / Night while a page is open does not repaint it.

## Owner: what to test (Settings > Display & Theme, set Day)
1. Open a short note, a flashcard deck, a question bank and an exam, the AI Tutor with a few messages (also while it types), then a competition exam if one is open. Is the text readable? Are panels visible against the page? Send screenshots of anything that looks wrong (light text on light, or a dark box left over).
2. Open pop-ups inside the pages (Explain with AI, results card, submit confirmation).
3. Night: look exactly as before (the night script is untouched).
4. If CI is red, send the error text.

## Next
D8b: fix what the owner's screenshots show (and `richtext.css` for code, highlights, tables, math). Or D7 (competitions, native admin console, onboarding, splash, `BrandSplashLoader`).
