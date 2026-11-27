# PROMPT FOR THE NEXT AGENT: replace every card picture in the app with a code-drawn cover

> Owner order 2026-11-27. Owner is on a free plan and reaches limits fast: work in small slices, push each slice, log it at the bottom of this file. Public repo: no secrets.
> This file is the plan. Read it first, then `docs/CODE_COVERS.md`. Do not restart or invent another approach.

## Goal
Every card picture in the app (16:9 thumbnails) is drawn by code, so the app carries no cover image files and new subjects cost no app size. **EXCEPTION: the Home welcome card picture (`bannerImage` / `HomeBannerRepository`) stays exactly as it is.**

## What already exists (owner saw ECE Year 1 on a phone: "flawless")
- Branch `code-cover-ece-y1` holds the working slice 1 (not yet in `main`):
  - `ui/packages/CodeCover.kt`: `CodeCover(label)` Canvas composable, `codeCoverUri(id)`, `isCodeCover(model)`, `codeCoverLabel(model)`, `CODE_COVER_SCHEME = "code-cover://"`.
  - `ui/packages/OpenCard.kt`: `OpenCardLayout` draws `CodeCover` when `image` is a `code-cover://<id>` string, otherwise Coil `AsyncImage` as before.
  - `ui/packages/EceCoverAsset.kt`: `eceYearCover(1)` returns the code cover.
- **Step 0: merge `code-cover-ece-y1` into `main`** (owner approved the look). Then continue on `main`, one commit per slice.

## The look (do not change; measured on the Canva 1280 x 720 frame)
Dark brushed-metal background with fine diagonal streaks, white heavy title with a cyan copy up-left and a magenta copy down-right (0.65 percent of width), gold band at the bottom (y 595 to 720, bronze left to dark right) with the Wisdom Tower mark and the two-line "WISDOM TOWER". Title centred at 48.5 percent width, 42.5 percent height, cap height 20.5 percent of height. Fonts: only those already in `res/font` (Outfit Black title, Outfit Bold logo). All constants are in `CodeCover.kt`.

## What to build
1. **Generalise** `CodeCover` so a cover is data, not a hard-coded map: `title` (one to three lines; long titles like "Electrical and Computer Engineering" or "Applied Mathematics I" must shrink or wrap to fit, never overflow) plus optional second line / small caption. Keep the `code-cover://<id>` string address so every existing `image: Any?` call keeps working; ids resolve to a title through ONE table (`CodeCoverCatalog`) so adding a subject = one line.
2. **Replace every card picture**, slice by slice (check each folder; list ONE narrow folder at a time, `search_code` does not work on this repo):
   - Home program cards (`HOME_PROGRAMS` in `ui/home/HomeScreen.kt`, 27 KB: patch script, do not resend): ECE Year 1 to 5, Freshman, Grade 9, 10, 11, 12, Entrance Questions, Remedial, UAT, COC, GAT, Exit Exam.
   - ECE semester / package covers (`PackagesData.kt` 35 KB: patch script; `eceSemesterCover`), ECE course covers.
   - Freshman course thumbnails (about 22 subjects, `images/freshman/`), Grade 9 to 12 subject covers (`images/web/grade-N/`), Remedial subject covers (`images/web/remedial/`), Entrance (natural / social), UAT hubs (`images/web/uat-hubs/`, SVG placeholders).
   - Hub cards (Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers, References, Videos), Academy partnership picture, any other `OpenCard(..., image = ...)` caller.
   - Find the rest by looking at every caller of `OpenCard` / `OpenCardLayout` and every `AsyncImage` that shows a 16:9 card picture.
3. **Do NOT touch:** the Home welcome card picture, the app logo / launcher icon / `logo.png` / animation, bank and payment logos, user-uploaded or real photographs that are not covers (decide case by case; if unsure write `UNSURE:` in the log and leave it), Year 3 data, access rules, Supabase, payments.
4. **Subject covers need a design variety rule:** the owner wants them recognisable per subject. Use the same layout, but a per-subject accent edge colour instead of always cyan/magenta where it helps (keep ECE years cyan/magenta). One accent pair per group, stored in the catalog table.
5. **Removing the old files is the LAST slice, and only after the owner has checked all covers on the phone.** Until then keep the image files and the fallback: if an id has no catalog entry, fall back to the old picture so nothing shows blank. Never delete files in the same commit that switches a screen.
6. Website repo is NOT part of this (app only).

## Rules
- Never break the build. CI is the real test; commit messages end with `not compiled` when CI was not seen.
- Files over 10 KB: never resend, use a patch script (`docs/EDITING_LARGE_FILES.md`).
- Update this file's log in the same commit as each slice. Tell the owner in plain short words what to test on the phone.
- Known detail: the Canva Year 3 image says "3ND"; the code label is "3RD".
- Known limit: the title is Outfit Black squeezed sideways, not the exact Canva font. A condensed font (e.g. Anton, needs a `.ttf` in `res/font`) would be closer; only add it if the owner asks.

## Log (append one line per slice)
| Date | Slice | What was pushed | Left / UNSURE |
|---|---|---|---|
| 2026-11-27 | 1 | ECE Year 1 on branch `code-cover-ece-y1`, owner saw it on phone and approved | Merge to `main`, then Years 2 to 5 and everything above |
