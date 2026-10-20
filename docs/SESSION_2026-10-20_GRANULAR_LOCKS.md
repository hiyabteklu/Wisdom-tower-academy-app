# SESSION 2026-10-20: granular content locks (hub, chapter, item)

> Repo is PUBLIC: no secrets, no student data. Owner order: "lock and unlock materials at chapter level and material list level, without code change or release; today the admin lock makes the entire hub coming soon".

## OWNER RULE (stated several times, do not ask again)
**The website is NOT a priority. Work on the app only.** Do not propose website changes, do not end a reply with "want me to do the website?", do not list website work as a next step. The website repo stays untouched unless the owner opens that topic himself. Anything that exists only on the website side is simply noted under "Known gaps" below, nothing more.

## Found (read, not guessed)
- `public.content_locks` (lock_key UNIQUE, mode in open / locked / coming_soon / require_purchase, public SELECT, admin ALL via `private.is_admin()`). No DB change needed: any new key format is just a new row.
- The app (`ContentAccess.resolve`) only read `package:` and `scope:` rows. A course lock therefore closed ALL hub cards of the course. The two `hub:` rows already in the table were never read. No chapter or item level existed.

## Done (app, branch `feat/granular-content-locks`; NOT compiled, NOT tested on a device)
- `data/content/ContentAccess.kt`: new `CourseLocks`, `resolveLocks`, `staticLocks`. One request per course (cached 30 s) reads the package row, the scope row, the `hub:` and `chapter:` rows of that scope and all `item:` rows. Most specific wins: item, chapter, hub, scope, package, static rule. `resolve`, `staticAccess`, `isUnlocked` unchanged.
- `ui/study/StudyWorkspaceScreen.kt`: a hub card tap uses `locks.hubGate(hubId)` (that hub only). Passes `locks` to the list.
- `ui/study/HubItemListScreen.kt`: new optional parameter `locks`. A locked row stays visible with a lock icon and "Coming soon" / "Package required"; a tap shows the same pop-up as a closed hub card; its download button is hidden and "Download all" skips it.
- `data/admin/LocksRepository.kt`: `chapterLockKey`, `itemLockKey`, `listHubItems` (every material of one hub, published or not, same scope rules as the student app).
- `ui/admin/LocksScreen.kt` (native admin, Content locks): opening a hub now shows "CHAPTERS AND MATERIALS". Each chapter ("Chapter 3 - all 5 materials") and each material has the same four modes and "Reset to code default" as the other levels. Unpublished materials are marked. Saving a lock does not reload the list, so the open block stays open.

## Lock keys (written by the admin screen or SQL; no app update needed to add, change or remove one)
- `package:<package id>`, `scope:<scope path>` (existing)
- `hub:<scope path>:<hub id>`, example `hub:freshman/physics:books`
- `chapter:<scope path>:<hub id>:<chapter number>`, example `chapter:freshman/physics:books:3`
- `item:<learning_resources.id>`
- Unlock one hub inside a closed course: `scope:` row = coming_soon plus `hub:` row = open.
- A chapter or item row only matters while its hub is open (the hub card is checked first).
- Clear a lock: delete its row ("Reset to code default").

## Known gaps
- Locks are an interface gate only: published `learning_resources` rows and the PDF proxy stay readable with the public key. Real enforcement would need RLS on `learning_resources` tied to `content_locks`.
- Website side (`ResourceHubGrid`, website LocksPanel) reads and writes only package and scope rows. Owner: not a priority, see rule above.
- Hypothesis, not checked: PostgREST `or=(... like."hub:<scope>:*" ...)` quoting; if the course lock read fails, the app falls back to the last answer or the static rule (never a lock-out).
- Hypothesis, not checked: `listHubItems` ordering `chapter.asc.nullslast` and the admin read of unpublished rows depend on the admin RLS policy of `learning_resources`.

## TEST (owner)
1. GitHub Action build on the branch is green (if red, copy the error to the next agent and log it in `docs/BUILD_PITFALLS.md`).
2. Admin > Content locks > a subject > Books: set the hub to Coming soon. Student app: only the Books card shows the pop-up, the other cards open.
3. Same screen: set Chapter 2 of Books to Coming soon (hub back to default). Books list opens, chapter 2 rows show a lock and the pop-up, the rest open.
4. Lock one material only: just that row is locked.
5. Reset each lock: everything opens again within about 30 seconds of reopening the course.
