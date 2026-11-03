# SESSION 2026-11-03 T1: "Ask AI tutor" at the end of every note

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice T1, owner problem 6). Read first: `NotesReaderScreen.kt`, `assets/notes/index.html`, `NativeTutorScreen.kt`, `AiTutorRepository.kt`, the website route `website-reference/src/app/api/ai/tutor/route.ts`, `SESSION_2026-10-31_AI_CACHE.md`.

## Checks done before building (as the plan asked)
- How the Summary gets its text: the page sends the first 8000 characters of the note to the app (`WTBridge.onSummarize`), the app calls `AiExplainRepository.summarize`.
- How the tutor takes context: `NativeTutorScreen(courseContext)` already sends `courseContext` to the website route `/api/ai/tutor`, which appends it to the AI's system instruction (`Current Student Course Context: ...`). The shell passes only the course TITLE today. The route has no size limit of its own, no sign-in and does not use the AI cache.
- Cache / usage rules (`SESSION_2026-10-31_AI_CACHE.md`): they belong to the `/api/ai/explain` route (Explain with AI and Summary). The tutor route is not cached and has no per-user limit, so this slice adds no new AI call path, no cache entry and no rate-limit risk beyond ordinary tutor questions.
- Free vs paid: the button only exists inside a note the student has already opened, so the access rules already applied. The tutor itself is open to everyone (as from the drawer). The tutor is switched off during a RANKED competition exam (C10 gate, unchanged); a note cannot be open then.
- Personal data: the context holds only the note title and the note text. Nothing about the student.

## What changed (4 code files)
- `assets/notes/index.html`: next to Summary a second pill button "Ask AI tutor" (cyan tint). It sends `WTBridge.onTutor({ text })` with the first 14000 characters of the note (Summary uses 8000). Nothing comes back to the page.
- `ui/study/NotesReaderScreen.kt`: new bridge method `onTutor`. It builds the context (`buildTutorContext`: names the note, tells the tutor to stay on THIS chapter first and to say so when a question goes beyond it, then the note text) and calls `NativeToolRequests.requestTutorWithContext(...)` on the main thread.
- `ui/study/NativeToolRequests.kt`: new `requestTutorWithContext(context)` (sets the context and the existing `pendingTutor` flag) and `takeTutorContext()` (returns it once, then forgets it). The context is a plain field, not Compose state.
- `ui/study/NativeTutorScreen.kt`: when it opens it takes the context ONCE (read before the C10 gate so a blocked open cannot leave an old chapter behind). If there is a context it starts a NEW empty chat on top of the saved ones (older chats stay in the list) and sends the context as `courseContext` with every question of this visit. No context (drawer, Learning tab) = nothing changes: same chat as before, `courseContext` from the shell.

## How it flows
Student taps "Ask AI tutor" at the end of a note, the page sends the text, the app sets the request, `MainActivity` (unchanged, it already watches `pendingTutor` and shows the global tutor overlay above everything), the tutor opens on a fresh chat that already knows the chapter. Back closes the tutor and the note is still there, at the same place.

## Not changed on purpose
- `MainActivity.kt` was NOT edited (about 266 KB, too big to resend safely). That is why the context travels through `NativeToolRequests`, which MainActivity already uses.
- No new welcome text or suggestion chips on the tutor screen (the route's suggestions are chosen from keywords in the context, as for any course; they may be generic). If the owner wants a line like "Ask me about this chapter", it is a small addition in `tutor.html` or the screen.

## Limits / unsure (not run, read from the code)
- A note longer than 14000 characters is cut at 14000, the rest is not seen by the tutor (the tutor is told nothing about the cut). Tell me if your notes are longer and I will send the part around the reading position instead.
- If the student opens an OLDER chat from the list during the same visit, the chapter text is still sent with its questions (one context per visit).
- Very long context makes the first answer a little slower (the route tries Gemini first with a 4.5 s start timeout per model, then Groq).
- The live website route is the one in `hiyabteklu/Wisdom-tower-academy`; the copy here already supports `courseContext`, so no website change is needed. I did not read the live repo.
- Not compiled, not tested on a device. Hypothesis: `TutorSessionStore.load` returns a Pair of the chat list and the active id (the old code used `.first` and `.second` on it), so the new `if` expression type-checks.

## Owner test on the phone (new build)
1. Open a short note, scroll to the end: two buttons, "Summary" and "Ask AI tutor".
2. Tap "Ask AI tutor": the tutor opens on an EMPTY new chat. Ask "explain the second point": the answer is about this chapter.
3. Press Back: you are in the same note at the same place. The new chat is in the chats list.
4. Open the tutor from the drawer or the Learning tab afterwards: it opens the last chat as before, no chapter text is sent.
5. Open a different note, tap the tutor: it talks about THAT note.

## Next slice
A1 (Sign in / Sign up screen).
