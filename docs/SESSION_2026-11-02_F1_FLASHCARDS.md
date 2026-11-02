# SESSION 2026-11-02 F1: flashcards, two separate motions

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice F1, owner problem 2).

## Read first (so nothing earlier is undone)
- `SESSION_2026-10-24_REVIEW_FULLSCREEN_FLASHCARDS.md`: the owner then asked that the card NOT follow the finger, no tilt, no slide-in, and that every card change use the same turn as the flip.
- `SESSION_2026-10-23_NOTES_FLASH_2.md`: about the notes page background flash only. It says the flashcards page was left alone (hidden from `onPageStarted`). Nothing there is touched.
- Kotlin host `FlashcardsScreen.kt`: not changed (bridge `onProgress` and `setDeck` are the same).

## CONFLICT, decided in favour of the newer plan (owner, please confirm)
The 2026-10-24 request and today's plan disagree. Today's plan (written later) asks for: flip = 3D turn, swipe / Next / Prev = slide with tilt, spring-back when released too early. A spring-back needs the card to follow the finger. So I followed the plan and replaced the 10-24 behaviour. How to go back to the 10-24 look: in `flashcards.html` make `swapCard` add the old `.slot.leave` turn again and remove the finger-follow code in `touchmove`; ask an agent, the old version is in git history (commit before this one).

## What was awkward (read from the old page)
1. Tap and Next used the SAME turn, so you could not tell a flip from a card change (the owner's complaint).
2. The flip was a plain 2D-looking turn with no depth: no shadow change, nothing behind the card.
3. Swiping gave no feedback under the finger (nothing moved until release).
4. The 'flip' could start while a card change was running (guarded only by `busy`, not during a swipe).

## What changed (only `app/src/main/assets/notes/flashcards.html`)
- FLIP (tap): the card turns around its vertical axis with perspective (`perspective(900px) rotateY()` inside the keyframes, 0.4 s, ease-in to edge-on then ease-out), the faces are swapped at the edge-on moment, and the card shadow shrinks and comes back. Still NO `preserve-3d` / `backface-visibility` (the F1d lesson: on the owner's phone a missing 3D transform once hid the answer), so only one face exists at a time.
- CHANGE CARD (swipe, Previous, Next, Again / Learning / Known) = ONE animation: the card slides out sideways with a 14 degree tilt and fades (0.22 s); the next card slides in from the other side with a 4 degree tilt (0.3 s). Next / Again / Learning / Known send the card to the left, Previous to the right. Same code for the buttons and the swipe.
- Swipe: the card follows the finger with a slight tilt. Release at 60 px or more: it slides out. Release earlier: springs back (0.32 s, small overshoot). At the first or last card the drag has resistance (a quarter of the distance) and always springs back.
- Deck look: up to two cards are drawn behind the top card (scaled down, lower, dimmer); they disappear toward the end of the deck.
- No flip while a swipe or a card change is running (`swiping` or `busy`).
- Remove animations (phone setting): no turn, no slide, no drag; the faces swap at once and a changed card only fades in (0.18 s).
- A card sliding off screen can no longer make the page scroll sideways (`overflow-x` clip on `#root`).
- Not changed: card texts, the Known / Learning / Again stats sent to the app, the 'Deck complete' card, Previous / Next / rating buttons, hint line, text sizes, day / night recolouring (the page script works from computed colours, the stack cards use the same dark gradient family as the faces; not verified on a device).

## Not done on purpose
- No shuffle button and no new 'known' logic: the plan says only to review them and note it. The screen already has progress chips, Again / Learning / Known and Practice again. A shuffle would be a new feature, so I did not add it. Say so if you want it.

Not compiled, not tested on a device (HTML/CSS/JS asset, no Gradle change). Hypothesis, unproven: very long cards that scroll inside might fight the horizontal drag a little; the page only starts a drag when the move is mostly horizontal.

## Owner test on the phone (new build)
1. Tap a card: it turns in 3D and shows the answer; shadow softens in the middle. Tap again: turns back.
2. Press Next and Previous: the card slides out and the next one slides in. Same slide for Again / Learning / Known.
3. Drag a card slowly sideways and let go early: it springs back. Drag past a short distance and let go: it slides out and the next card comes in.
4. On the first card drag right, on the last card drag left: heavy drag, then it springs back.
5. You see 1 to 2 cards behind the top card, fewer near the end.
6. Tapping during a slide does nothing odd (no half flip).
7. Phone setting Remove animations on: cards just change with a quick fade, flip is instant.
8. Day look: the cards and the ones behind should recolour like other panels (tell me if the stack stays dark).

## Next slice
M1 (match game blanks and predictability). Read `GAMES_FEEL_PLAN.md` (M1-M3) first, it may already overlap.
