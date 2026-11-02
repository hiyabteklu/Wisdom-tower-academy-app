# PLAN (2026-11-02): reader, flashcards, exams, speed, match game, sign-in fixes

> Repo is PUBLIC: no secrets, no student data.
> Written by the owner's request. Follows `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`:
> one slice = one commit pushed straight to `main`, tick the slice HERE in the same commit,
> record every live Supabase change (migration name + one line) in the "Supabase changes" list below.
> Owner is NOT a coder, free tier, new agent every 3-4 prompts. Say "Continue" = do the next unticked slice.
> Talk to the owner in plain words: what changed, what to test on the phone.

## How to start (new agent)
1. Read `OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`, this file, then the newest `docs/SESSION_*` file.
2. Do the FIRST unticked slice only. Find the real code first (search the repo), do not guess file names.
3. Push to `main`, tick the slice, write a short `docs/SESSION_<date>_<slice>.md`.

## Where the code is (found in E1)
- Practice and exams (question banks, exams, their review): `app/src/main/assets/notes/quiz.html` (one HTML page with its own JS, `draw()` rebuilds the whole page). Kotlin host: `app/src/main/java/com/wisdomtower/academy/ui/study/QuizExamScreen.kt`.
- Competition exams: `app/src/main/assets/notes/competition_exam.html` (same `draw()` code, confirmed in E1b; it has no review page, the server grades). Kotlin host: `app/src/main/java/com/wisdomtower/academy/ui/competition/CompetitionExamScreen.kt` (global deadline, autosave, integrity flags).
- Website copy (not a priority): `website-reference/src/components/learning/QuizExamViewer.tsx`.
- Repo is now named `hiyabteklu/Wisdom-tower-academy-app` (the old name `...-packed` redirects, code search only works with the new name).

## The owner's 10 problems (his words, shortened)
1. Book/PDF reader remembers the last page. Notes reader does not. He wants the same for notes.
2. Flashcards: flip and swipe animations are wrong. Swipe/next/prev should have ONE animation, flip/tap a DIFFERENT one. Cards lack a 3D card feel. Flashcards feel awkward overall, rethink them.
3. Exams / question banks: the question-number board (top right, 1-40) shakes once you go past about question 6-7. Example: on question 23, press Next, the board scrolls back to 1 then forward to 24. Bug.
4. Exams must have a Start Exam button. Today the exam starts instantly. The countdown must only start when he presses Start.
5. Review mode (exam/question bank): pressing "Solution" or "Explain with AI" sends him back to question 1. Example: on question 20, press Solution, he lands on question 1. Must return to the same question.
6. AI tutor at the end of every note, next to the Summary button, with the context of that chapter (like the summary has). Only if it has no bad side effects.
7. Question viewer is one question per screen, but Next / Prev / Submit make their own footer block. The buttons move up and down with short and long questions and the screen looks cluttered. Wanted: clean floating buttons in a fixed place.
8. App is slow when online or on a slow connection, but better offline. It acts "online only": spinners everywhere, hubs open slowly. Wanted: show what is already saved FIRST, refresh in the background. Fast for users who already downloaded their content.
9. Match game: after a correct pair the cards disappear but the new cards come too slowly and leave blank slots. Also a question and its answer must NOT always arrive as a pair (too predictable): add some randomness.
10. Sign in / sign up screen: fine on his phone (his phone is in a mini/desktop-like mode) but too big and confusing on other phones. The Sign in / Sign up switch is a square with round corners, it must be a PILL. Needs a cool, advanced look (not a flat form) and the brand/logo above it.

## Likely shared causes (check first, do not assume)
- Problems 3 and 5 were the SAME kind of bug (confirmed in E1): `draw()` rebuilds the page with `innerHTML`, which reset the number strip and the page scroll. Fixed in `quiz.html` (E1) and `competition_exam.html` (E1b).
- Problems 4 and 7 both live in the same exam screen shell. Do them one after the other in the same files.
- Problem 8 overlaps `docs/OFFLINE_STATUS_PLAN.md` (O1-O5). Reuse its saved-sync idea, do not build a second system.

## Slices (do in this order, one per chat)
Order = most painful bugs first, biggest/riskiest last.

- [x] **E1 Question navigator jump (problem 3) + review returns to same question (problem 5)** DONE for `quiz.html` on 2026-11-02, see `docs/SESSION_2026-11-02_E1_NAVIGATOR_REVIEW_SCROLL.md`. Owner still has to test on the phone with a new build.
  Keep the current question when Next/Prev/Solution/Explain with AI is pressed. Navigator scrolls ONLY to bring the current number into view (smooth, no jump to 1), and only when the number is off screen. Opening Solution/AI and coming back shows the same question and the same answer state.
  Owner test: exam with 40 questions, go to 23, press Next (board must not flicker, goes to 24). In review on question 20, press Solution and Explain with AI, close, still on 20.
- [x] **E1b Same fix in `competition_exam.html`** (competition exams) DONE 2026-11-02, see `docs/SESSION_2026-11-02_E1B_COMPETITION_EXAM_SCROLL.md`. Same three changes as `quiz.html`. The competition exam has no review page (server grades), so nothing to fix there. Owner still has to test on the phone with a new build.
- [x] **E2 Start Exam button (problem 4)** DONE for `quiz.html` (hub exams) on 2026-11-02, see `docs/SESSION_2026-11-02_E2_START_EXAM.md`. Exams open on a start screen (title, questions, time, rules); the countdown and the time-used clock start only on Start Exam; Retake goes back to the start screen; question banks open directly. Owner still has to test on the phone with a new build.
  **Competition exams were left WITHOUT a Start screen on purpose** (their clock is fixed to the end of the competition and the entry already exists when the window opens). If the owner wants a "Start competition" step it must be added BEFORE the entry is created, in the competition hub. Owner decision needed, see "Plan changes".
  Owner test: open an exam, nothing counts down until Start is pressed.
- [x] **E3 Clean floating Prev / Next / Submit (problem 7)** DONE in `quiz.html` and `competition_exam.html` on 2026-11-02, see `docs/SESSION_2026-11-02_E3_FLOATING_NAV.md`. Prev / Next / Submit are one floating pill bar fixed to the bottom, same place for short and long questions; a spacer under the question keeps text visible; on the last question Next is disabled and Submit lights up in its place. Day look not verified on a device. Owner still has to test on the phone with a new build.
  Remove the footer block (in `quiz.html` it is `.nav` with `position: sticky`, built by `navBar()`; same in `competition_exam.html`). Buttons float over the screen in a fixed place (same spot for short and long questions), pill/rounded style, with bottom padding on the question area so text is never hidden. Submit appears in the same place on the last question. Works in day and night looks (see `DAY_NIGHT_PLAN.md`) and with the system gesture bar.
  Owner test: go through a short and a long question, buttons never move.
- [x] **N1 Notes reader remembers position (problem 1)** DONE 2026-11-02, see `docs/SESSION_2026-11-02_N1_NOTES_POSITION.md`. The notes page reports its scroll position as a share of the page height; `NotesReaderScreen.kt` keeps it on the phone (SharedPreferences `notes_reader_state`, key `pos_<resourceId>`, like the PDF reader's `last_page_<id>`) and hands it back to the page when the note opens. Phone copy only, not synced (no table added). A note read to the end opens at the top. Not compiled, not tested on a device: owner still has to test with a new build.
  Save last scroll position (or last section) per note, restore on reopen, exactly like the PDF reader. Reuse whatever the PDF reader uses to save the page. Store locally (works offline). Optional second step, only if the app already syncs reading progress: sync it, otherwise do not add a table. If a note is edited and the saved position no longer fits, fall back to the top without errors.
  Owner test: scroll deep into a note, close, reopen, it opens where he stopped.
- [ ] **F1 Flashcards rethink (problem 2)**
  Two separate motions: (a) FLIP on tap = 3D rotation around the vertical axis with perspective, front/back faces hidden when turned away, about 350-450 ms; (b) SWIPE / Next / Prev = card slides and fades out, next card slides in with a slight tilt, same animation for the swipe gesture and for the buttons. Add depth: soft shadow that changes while flipping, a stack of 1-2 cards visible behind the top card, spring-back when a swipe is released too early. No flip while a swipe is in progress. Respect "reduce motion" accessibility setting (simple fade). Also review the screen as a whole (progress, known / not known actions, shuffle) and note in the session file what felt awkward and what was changed. Check `SESSION_2026-10-24_REVIEW_FULLSCREEN_FLASHCARDS.md` and `SESSION_2026-10-23_NOTES_FLASH_2.md` first so nothing done before is undone.
  Owner test: tap = flips in 3D, swipe and buttons = same slide animation, never mixed up.
- [ ] **M1 Match game blanks and predictability (problem 9)**
  Replace matched cards immediately (new card appears in the same slot with a short fade/scale in, no waiting for a slow load). Keep a small pool ahead so slots are never empty. Do NOT always bring a question together with its own answer: refill from a shuffled pool so the visible cards are a mix, but make sure every card on screen can still be matched (its partner is always on screen or guaranteed to appear). Read `GAMES_FEEL_PLAN.md` (M1-M3) first, this slice may already overlap it, then tick both.
  Owner test: match 10 pairs in a row, no empty slots, partners are not always neighbours.
- [ ] **T1 AI tutor at the end of each note (problem 6)**
  Next to the Summary button at the end of each note add "Ask AI tutor". It opens the existing tutor with the chapter text as context (same text the Summary uses), tutor name/subject prefilled, user does not have to explain anything. Checks before building: how the summary gets its text, the token/size limit (cut long notes sensibly), the AI usage limit / cache rules (see `SESSION_2026-10-31_AI_CACHE.md`), and that free vs paid access rules still apply. Do not send student personal data. If the context is too large, send the current section plus the summary and say so in the session file.
  Owner test: open the end of a note, tap the tutor, ask "explain the second point", it answers about this chapter.
- [ ] **A1 Sign in / Sign up screen (problem 10)**
  One layout that scales: use screen-size based sizing (not fixed big sizes), test on small, normal and large phones and with the system "display size" / font size set large. Sign in / Sign up switch = pill with a sliding selected pill. Add the brand (logo + app name) above the form, a richer look (gradient/glass card, subtle motion), still readable in day and night looks. Keep the one-sign-in-look decision (`SESSION_2026-10-17_S5_ONE_SIGNIN_LOOK.md`) and the width cap work (`SESSION_2026-10-17_S8_WIDTH_CAP.md`). Do not change the sign in logic.
  Owner test: on his phone and a friend's phone it looks the same size, switch is a pill.
- [ ] **P1 Fast when online (problem 8)** (biggest, split into sub-slices in the first commit of P1)
  Goal: saved content first, refresh in the background. Steps for the agent: (1) measure first, list which screens wait for the network before showing anything (hubs, course page, status, notes list, question banks) and write the list in the session file; (2) for each, show the saved copy immediately and refresh silently, replace only when new data differs; (3) short timeouts on slow connections, no full-screen spinner when saved data exists, small non-blocking "updating" hint instead; (4) only show a spinner or skeleton when there is truly nothing saved; (5) do not block opening on "checking access" if the last known access is saved (keep the security rules in `SECURITY_PLAN_CONTENT_ACCESS.md`, never show locked content); (6) look at the main thread work already reduced in `SESSION_2026-10-23_O4_MAIN_THREAD_RELIEF.md` and `SESSION_2026-10-23_O3_STARTUP_COVER.md`. Write sub-slices P1a, P1b... here before coding.
  Owner test: with data on and with weak network, open hubs and course pages, they open at once with the old content, then update.

## Rules for every slice
- Day and night looks both must work (see `DAY_NIGHT_PLAN.md`, `DAY_MODE_PLAN.md`).
- No secrets in the repo. No student data in docs.
- Do not break exam integrity rules (`EXAM_INTEGRITY_ADMIN.md`, `EXAM_INTEGRITY_REVIEW.md`).
- If the plan is wrong, write why under "Plan changes" and keep going unless the owner decides.
- Commit message ends with `not compiled` if CI was not seen.

## Supabase changes (live)
- None yet. (Slices above are expected to need no database change. If one does, list migration name + one line here.)

## Plan changes
- 2026-11-02: added slice E1b (same fix in `competition_exam.html`), kept E1 as `quiz.html` only so each commit stays small.
- 2026-11-02 (E2): no Start screen inside the competition exam window. Its countdown runs to the end of the competition and the entry already exists, so a Start screen would hide a running clock without moving the deadline. Open question for the owner: add a "Start competition" step in the competition hub before the entry is created? Not done unless he says yes.
- 2026-11-02 (N1): notes position is kept on the phone only (no server sync, no new table). Open question for the owner: also sync it so a note opens at the same place on a second phone? Not done unless he says yes.

## Next slice
F1
