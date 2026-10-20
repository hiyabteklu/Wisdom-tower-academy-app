# SESSION 2026-10-20: granular content locks (hub, chapter, item) + STRONG database locks

> Repo is PUBLIC: no secrets, no student data. Owner order: "lock and unlock materials at chapter level and material list level, without code change or release; today the admin lock makes the entire hub coming soon". Later the same day: "I want reliable access lock; the app is free until Play Store approval, after that I want STRONG lock for parts of the hubs".

## OWNER RULE (stated several times, do not ask again)
**The website is NOT a priority. Work on the app and the database only.** Do not propose website changes, do not end a reply with "want me to do the website?". Do not explain things the owner cannot act on: give the result, then the one thing to do.

## STRONG LOCKS IN THE DATABASE (live on Supabase project amieczmpqrvavzjikbdi since 2026-10-20, applied with migrations `content_guard_function_and_switch` and `content_guard_policy_swap`)
Why: the old policy `lr_public_select_published` let ANYONE (even signed out) read every published row including `body_md`, `meta` and `storage_path`, so every lock was only a picture in the app. Now the database decides.
- Function `content_guard.resource_visible(package_id, scope_path, hub, chapter, id)` (SECURITY DEFINER, schema `content_guard`, executable by anon and authenticated).
- Policy `lr_public_select_unlocked` on `learning_resources` replaces `lr_public_select_published`: a student reads a published row only when the function says yes. `lr_admin_all` is unchanged: admins always see everything. The competition function `admin_publish_competition_from_exam` is SECURITY DEFINER and is not affected.
- **MASTER SWITCH:** content_locks row `system:enforce`. mode `open` = OFF (free period, everything readable exactly as before). mode `locked` = ON (strong locks enforced). **It is OFF now.** To turn ON after Play Store approval: `update public.content_locks set mode='locked' where lock_key='system:enforce';` (and back to `open` to switch off again).
- When ON, the most specific lock row wins: `item:<id>`, `chapter:<scope>:<hub>:<n>`, `hub:<scope>:<hub>`, `scope:<scope>`, `package:<package id>`. No row = open. `open` = readable, `coming_soon` and `locked` = nobody except admins, `require_purchase` = only a signed-in student with an `enrollments` or `access_grants` row for that package (or package `__all__`; any ECE package counts for any ECE row). A locked row is not returned at all: the student app shows an empty list for it ("No published materials in this hub yet").
- Tested 2026-10-20 inside rolled-back transactions: 13 function checks (switch off/on, hub, scope, chapter, item, precedence, anonymous, stranger, enrolled student) all as expected; real anonymous role: 399 of 399 published rows visible with the switch off, 399 with it on and no locks, 398 after locking one hub (exactly that hub's single book). Everything was rolled back; final state checked: switch `open`, no lock rows other than the old `open` ones.
- Works with the app that is ALREADY released, no update needed: lock a hub or a single material with a row, the released app simply shows nothing there.

## What "chapter" means in your data
No published row has the `chapter` column filled (0 of 399). In practice each chapter IS its own material row (for example short-notes "Chapter #1 notes", "#2 ..."). So the lock for a chapter is the lock of that material: `item:<id>`. The `chapter:` key exists and works, but only matters if someone fills the `chapter` column.

## Found earlier (read, not guessed)
- `public.content_locks` (lock_key UNIQUE, mode in open / locked / coming_soon / require_purchase, public SELECT, admin ALL via `private.is_admin()`).
- The released app only read `package:` and `scope:` rows, so a course lock closed all hub cards; the `hub:` rows already in the table were never read. All lock rows that existed on 2026-10-20 are mode `open`.

## Done in the app (branch `feat/granular-content-locks`; NOT merged, NOT compiled, NOT tested on a device)
- `data/content/ContentAccess.kt`: `CourseLocks`, `resolveLocks`, `staticLocks` (hub, chapter, item levels; most specific wins). `resolve`, `staticAccess`, `isUnlocked` unchanged.
- `ui/study/StudyWorkspaceScreen.kt`: a hub card tap uses `locks.hubGate(hubId)` (that hub only).
- `ui/study/HubItemListScreen.kt`: locked rows show a lock icon and the pop-up, no download. NOTE: with the database switch ON a locked material is not returned at all, so this only shows for locks the student is still allowed to SEE (it protects nothing by itself; the database does).
- `data/admin/LocksRepository.kt` + `ui/admin/LocksScreen.kt`: Admin > Content locks > subject > hub now lists the materials of the hub, each with the four modes and "Reset to code default" (writes `item:` rows; chapter rows for rows that have a chapter number).

## Known gaps
- The PDFs themselves live in Appwrite. Hiding the row hides the file id from students, but anyone who already has a file id or link can still download it until the Appwrite bucket permissions are tightened (not possible from here, owner action in Appwrite).
- No in-app switch for `system:enforce` yet; until then it is flipped with the SQL above (ask the agent).
- Locked materials vanish instead of showing "Coming soon" for item level (RLS cannot hide only some columns). A teaser list function could be added later.
- Website: not a priority (see rule above).

## TEST (owner)
1. Today nothing should look different to students (switch OFF). That is the correct result.
2. Ask the agent to run a live lock test, or: set `system:enforce` to `locked`, add a `hub:<scope>:<hub>` row with `coming_soon`, open that hub in the released app: empty list. Delete the row: content is back.
3. GitHub Action build of the branch is green before merging (if red, copy the error to the next agent and log it in `docs/BUILD_PITFALLS.md`).
