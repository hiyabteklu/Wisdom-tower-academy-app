# SESSION 2026-10-19 — D8c: notes, flashcards and competition pages checked against the day script

> Repo is public: no secrets. Owner order: "Continue" (after D8b). One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone. No owner screenshots yet, so this is a code review by hand.

## What I read
`assets/notes/index.html` (notes), `flashcards.html`, `competition.html` (one question) and `competition_exam.html`, each against the D8b day script. `competition_review.html` I did NOT open.

## Found and fixed (commit `4dc7ba4`, `AppBackdrop.kt`, day script only; night untouched)
The text of a note is the soft light grey `CBD5E1` (lightness about 0.83). The D8a rule made only lightness above 0.85 deep navy ink; everything between 0.5 and 0.85 became the muted grey `4A5A70`. So the whole body of every note (and the grey `Explain` and summary text) would have been muted grey by day, the same grade as captions. Now lightness 0.78 and above becomes ink; `94A3B8` (0.65) and below stays muted. Also affects eyebrow labels and gauge captions in the quiz (they become ink, fine).

## Hand-check result (hypothesis, not run)
- Notes page: text ink, headings teal / amber (from `richtext.css`), Summary button and box on glass with navy hairlines.
- Flashcards: card (both faces) ivory glass, question label violet, answer label teal, chips, the three rating buttons (Again rose, Learning amber, Known stays bright green with dark text), "Practice again" stays violet with white text, "Deck complete" card glass. The swipe and flip effects only touch transform and opacity, so the script does not interfere.
- Competition question page: same card and choices as the quiz.
- Competition exam: same as the quiz plus the red "Cheating means disqualification" line (red text goes to a deep red on its own pale-red tint).
- Rating buttons and choices have only a navy hairline by day (their white 3 to 4 percent fill is invisible on ivory). I did not add a day fill because a rule written for `.btn` would also override the coloured `.btn.know` and `.btn.violet` backgrounds.

## Still not done (D8d onwards)
- `competition_review.html`: not read.
- SVG fills and strokes other than the gauge track; gradient-clip text; bright accent fills with light text (D9 measures).
- Day glass strength; a page keeps the mode it was opened in.
- Nothing has been run. The three pages that have been read in D8a to D8c (quiz, tutor, richtext.css) are the same pattern as these.

## Owner: what to test (Settings > Display & Theme, Day)
Notes (is the body ink, easy to read?), a flashcard deck (flip, swipe, rate), a question bank, an exam, a competition exam if one is open. Screenshots of anything wrong, please. Night must look exactly as before.

## Next
Wait for the owner's screenshots of the pages, or do D8d (`competition_review.html` plus any fix), or D7 (native competitions, admin console, onboarding, splash, `BrandSplashLoader`).
