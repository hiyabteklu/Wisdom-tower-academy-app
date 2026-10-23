# Session 2026-10-24: review screen full height, flashcards without sideways move

Owner request (plain words):

1. Competition review screen (`CompetitionReviewScreen.kt`): no Back row, title, score line or "Question N of M" line once the review is loaded. The page fills the screen; leave with the phone Back button. Previous and Next are two separate floating pills (with a small N / M pill between them), no strip, no second footer. `CompetitionReviewView.kt` got `bottomPaddingDp` (72) so the last lines scroll above the pills. The Back row stays while loading, on an error and when there are no questions.
2. Normal flashcards (`assets/notes/flashcards.html`): the card no longer moves sideways or tilts with the finger, no slide-in. A swipe of 60 px or more still changes the card. Every card change (Previous, Next, swipe, Again / Learning / Known) now plays the same turn animation as the flip (old card turns edge-on 0.14 s, new card turns in 0.2 s), so the buttons are animated too.

Not touched: the competition EXAM runner page (`competition_exam.html`) has its own sticky bottom bar. If the owner wants the same floating look there, that is the next slice.

Status: written, CI not seen yet.
