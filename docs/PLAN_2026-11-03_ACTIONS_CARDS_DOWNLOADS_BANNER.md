# PLAN 2026-11-03: scholar quick actions, taller material cards, instant sizes, background downloads, dynamic home banner

> Owner order (2026-11-03): plan first, then push each slice straight to `main` (see `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`). Repo is PUBLIC: no secrets, no student data in this file or in commits.
> This file is PLAN ONLY. No code was changed in the commit that added it.

## Order of work (one slice = one commit, tick here in the same commit)

- [x] **A1** — implemented and pushed; owner phone test pending. Scholar list: quick actions (Push, Call, Email) on every row and in the profile panel
- [x] **A2** — implemented and pushed; owner phone test pending. Inquiries list: same actions, with Reply first
- [x] **C1** — database column, app caching, save-time HEAD capture, and old-book backfill implemented; phone/CI verification pending. Book size shown immediately (like the title)
- [ ] **D1** Downloads keep going when the student leaves the page (stay in the app)
- [ ] **E1** Downloads start fast, also on a slow connection
- [ ] **B1** Taller material cards (books, notes, question banks, all lists): title, type, description, size
- [ ] **F1** Home welcome picture becomes dynamic (app side: cached remote picture, bundled one is the fallback)
- [ ] **F2** Admin "Home Banner" control center (change picture, back to default)

Why this order: A and C are small and visible, D/E fix the biggest daily annoyance, B is layout, F is the only slice that needs new Supabase pieces.

---

## What the code does today (read on 2026-11-03)

Read: `OfflineVault.kt`, `ui/home/HomeScreen.kt`, `ui/admin/UsersScreen.kt`. To READ before coding each slice (not read yet): `ui/study/HubItemListScreen.kt`, `ui/study/DownloadGauge.kt`, `ui/admin/InquiriesScreen.kt`, `ui/admin/BroadcastComposerScreen.kt`, `ui/learning/LearningScreen.kt`, `docs/CONTENT_SHAPES.md`, and the place in `MainActivity.kt` that serves PDF requests through `CachingInputStream`.

- **Scholar list** (`UsersScreen.kt`): each scholar is a short row (initials, name, email, id and level, arrow). Tap opens a profile panel with only Close and Grant New Package. Phone and email are on the profile as plain text only. No push, call or email action anywhere.
- **Sizes** (`OfflineVault.kt`): a book size is known only if (1) the file is already downloaded, (2) its file id is in the hard-coded `PRESEEDED_BOOK_SIZES` list (27 old books), (3) it was cached before, or (4) a HEAD request worked (6 second limit). New books (for example the Signals and Systems ones) are in none of these, so the size shows only after the download starts and the stream reports it.
- **Downloads**: the PDF is saved WHILE the reader/web page streams it (`CachingInputStream`). When the student leaves the page, `close()` runs, deletes the `.tmp` file and clears the progress. That is why leaving the page kills the download. There is no resume.
- **Slow start**: all background work (saving files, clearing progress, even an 800 ms sleep) shares ONE thread (`OfflineVault.io`). Connect limit is 30 s, read limit 60 s, no retry. Progress is first reported only after 32 KB or 60 ms of data, and while the total is unknown it is guessed (`loaded + 1 MB`), so the bar looks stuck at the start.
- **Welcome picture** (`HomeScreen.kt`, item `welcome_card`): hard-coded `file:///android_asset/images/home/academy.jpg` for both signed in (`OpenCardLayout` with `WelcomeTitle`) and signed out (`OpenCard`).

---

## A1 + A2: quick actions on scholars and inquiries

Owner wish: from the scholar list or the inquiry list, push a notification, call, e-mail (and reply on inquiries) WITHOUT searching a user id in the push screen.

- Add a small action strip to `ScholarRow` and to `ProfilePanel`: **Push**, **Call**, **Email** (round icon buttons, 48 dp touch targets, labels for TalkBack). A tap on an action must NOT open the profile (profile opens from the rest of the row).
- **Call**: `Intent.ACTION_DIAL` with the scholar phone (opens the phone app, never calls by itself). Hidden or greyed when the phone is empty.
- **Email**: `Intent.ACTION_SENDTO` `mailto:` with the scholar email. Hidden or greyed when empty.
- **Push**: a small sheet "Send push to <name>": title, message, Send. It uses the SAME sending path as `BroadcastComposerScreen` / the Broadcasts screen, with that one scholar as the only target (no id typing). Must confirm by reading the composer how a single user is targeted; if it only supports ids typed by hand, reuse the same call with the id taken from `ScholarItem.id`. If the scholar has no registered device, show "No device registered yet" instead of failing silently.
- Tap-to-copy stays for the student id.
- **Inquiries** (`InquiriesScreen.kt`): same strip. **Reply** comes first (if a reply flow already exists, expose it on the card; if not, Reply = e-mail to the inquiry sender with the subject prefilled, and push when the sender is a signed-in student). Check what the inquiry record holds (email, phone, user id) before wiring.
- No database change expected for A.
- Phone test: open Scholars, tap Push on a row, send, see it arrive on a test phone; tap Call and Email and see the phone and mail apps open with the right value.

## C1: size shown immediately

Goal: the size is visible on the card together with the title, before any download, also offline.

- Source of truth: store the file size WITH the material record in the database (a `size_bytes` value next to title/type). Admin tools that upload or link a book fill it automatically (HEAD request once). A one-time backfill fills old books.
- App: the list already loads title; it loads size in the same request and caches it (same cache as the title, so it also shows offline). `OfflineVault.getPdfSize` reads this first, then the old hard-coded list (kept as extra fallback), then cache.
- If a material has no size yet: a quiet background HEAD probe (online only, one at a time, never downloads) fills the cache; the card shows a small shimmer, not an empty gap.
- Check `docs/CONTENT_SHAPES.md` for the material shape and whether a size column already exists before adding anything. Any Supabase change is written under "Live changes" below.

## D1: downloads continue when leaving the page

Goal: once started, a download finishes while the student is anywhere in the app. (Not required: surviving the app being closed. That can come later with WorkManager.)

- Move the download out of the reader/web stream into an app-level download manager (lives in the Application scope, not the screen). The tap only asks the manager to start; the manager writes `<key>.pdf.part` and renames when complete.
- Progress is published from the manager (`OfflineVault.updateProgress` keeps working) so `DownloadGauge` and the list cards show progress on any page.
- `CachingInputStream.close()` must stop deleting a download that the manager owns.
- Resume with HTTP `Range` from the `.part` file after a drop or a re-tap.
- One tap on an item already downloading does not start a second copy.

## E1: faster start, also on slow connection

Honest limit: the network speed itself cannot be changed. What can be fixed is the waiting before the first byte and the stuck look.

- Dedicated download thread pool (2 at a time). Downloads never wait behind `OfflineVault.io` jobs.
- Start the connection the moment the student taps (no size probe in front of it). Optional: warm the connection (DNS + TLS) when the item sheet opens.
- Show "Connecting..." immediately, then real progress. Report the first progress right away, not after 32 KB. Use the known size (C1) as the total so the bar is real from the first second.
- Shorter connect limit with automatic retry (for example 15 s, 3 tries with growing wait) instead of one 30 s wait; keep the longer read limit for slow links.
- Resume (from D1) means a retry never starts from zero.
- Phone test on a slow connection: tap download, see "Connecting..." at once, bytes start flowing, leave the page and come back, it is still going.

## B1: taller material cards

Owner wish: cards about twice as tall as now, with title, type, description; looks good, no empty space.

- Applies to the lists in `HubItemListScreen.kt` (books, notes, question banks and the other hub lists) and to the Learning list cards if they use the same card.
- New card: type chip (Book, Notes, Question bank, Flashcards, Exam), title (up to 2 lines), description (up to 3 lines), a footer row with size or question count and the saved / download state. Same glass look and press motion as today; the status circle stays on the right.
- NO EMPTY CARD rule: every field shows only when it has a value. If there is no description, fill the space with a useful line built from what we know (chapter, question count, size) so a card is never half blank.
- Descriptions come from the material record. If there is no description field yet, add one (admin edits it in the existing materials screens `CourseContentScreen.kt` / `FreeResourcesScreen.kt` / `CatalogScreen.kt`) and write the change under "Live changes".
- Day and night looks must both work (use the existing tokens, not fixed colours).

## F1 + F2: dynamic home banner (welcome picture)

Owner wish: the welcome picture is no longer fixed in the app; the admin can post news through it from a dedicated control center, change it, or go back to default. 16:9, light, cached on the phone, changes when the admin changes it.

**App side (F1, `HomeScreen.kt`):**
- The bundled `academy.jpg` stays in the app ONLY as the fallback (nothing deleted).
- A small `HomeBanner` loader: show the cached picture at once; on opening Home do one tiny check of a version number; download the new picture only when the version changed; keep it on disk (private cache). If there is no banner, no network, or any error: show the bundled picture. No flashing, no layout jump (card stays 16:9).
- Same loader used for the signed-in card (with `WelcomeTitle`) and the signed-out `OpenCard`.

**Admin side (F2, new screen in `ui/admin/`, registered in `AdminModules.kt`):**
- "Home Banner" control center: shows the live picture, the date it was changed, the buttons **Change picture** and **Back to default**.
- Change picture: pick from the phone, force 16:9 (crop frame), shrink to about 1280x720, compress so it stays light (target under about 200 KB), upload, then bump the version. A confirmation before publishing; Back to default asks first too.
- Only admins can change it (database rule, not only hidden buttons).

**Supabase pieces (written under "Live changes" when made):**
- a public-read storage bucket for banner pictures (admin-only write);
- one small settings row: current picture address, version (updated time), and an "is default" flag, readable by everyone, writable only by admins.

Not in this plan (can come later if wanted): tap-through link or headline on the banner, scheduled banners, several banners rotating.

---

## Rules for every slice

- Read the file(s) first, change as little as needed, keep the existing look and the motion style.
- Push to `main` right after each slice, tick the box above in the same commit, add a `docs/SESSION_2026-11-xx_<slice>.md`.
- Commit message ends with `not compiled` when the CI result was not seen.
- Plain words to the owner: what changed, what to test on the phone.

## Live changes (Supabase)

- `learning_resources_size_bytes` — added nullable `size_bytes` to `learning_resources`, exposed it to student light-list reads, preserved the admin-only view, and granted authenticated admin writes. Add one line per migration here: name + what it does.

## Plan changes

- 2026-11-03 (C1): Added `size_bytes` to material rows. The app reads it with the title and stores it in the existing offline list cache. Saving a book with a new Appwrite ID measures the file with HEAD; opening Admin > Course Content > Books backfills older rows with missing sizes.
- 2026-11-03 (A2): Inquiry rows and details now offer Reply, Push, and Email. Reply opens the mail app with a prefilled subject; Push resolves the inquiry email to an Academy profile before using the existing targeted notification sender. Inquiry records contain no phone or user ID, so Call is not shown.
- 2026-11-03 (A1): Added scholar Push / Call / Email actions in the directory and profile. Push reuses `ComposerRepository.sendNow` with the scholar user ID; Call opens the dialer and Email opens a mail composer. Zero-device responses are explained.
