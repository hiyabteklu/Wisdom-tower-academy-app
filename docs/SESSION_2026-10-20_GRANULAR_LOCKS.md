# SESSION 2026-10-20: granular content locks (hub, chapter, item)

> Repo is PUBLIC: no secrets, no student data. Owner order: "lock and unlock materials at chapter level and material list level, without code change or release; today the admin lock makes the entire hub coming soon".

## Found (read, not guessed)
- `public.content_locks` (lock_key UNIQUE, mode in open / locked / coming_soon / require_purchase, public SELECT, admin ALL via `private.is_admin()`). No DB change needed: any new key format is just a new row.
- The app (`ContentAccess.resolve`) and the website grid only read `package:` and `scope:` rows. A course lock therefore closed ALL hub cards of the course. The two `hub:` rows already in the table were never read. No chapter or item level existed.

## Done (app, branch `feat/granular-content-locks`; NOT compiled, NOT tested on a device)
- `data/content/ContentAccess.kt`: new `CourseLocks`, `resolveLocks`, `staticLocks`. One request per course (cached 30 s) reads the package row, the scope row, the `hub:` and `chapter:` rows of that scope and all `item:` rows. Most specific wins: item, chapter, hub, scope, package, static rule. `resolve`, `staticAccess`, `isUnlocked` unchanged.
- `ui/study/StudyWorkspaceScreen.kt`: a hub card tap uses `locks.hubGate(hubId)` (that hub only). Passes `locks` to the list.
- `ui/study/HubItemListScreen.kt`: new optional parameter `locks`. A locked row stays visible with a lock icon and "Coming soon" / "Package required"; a tap shows the same pop-up as a closed hub card; its download button is hidden and "Download all" skips it.

## Lock keys (the admin or SQL writes these rows; no app update needed to add, change or remove one)
- `package:<package id>`, `scope:<scope path>` (existing)
- `hub:<scope path>:<hub id>`, example `hub:freshman/physics:books`
- `chapter:<scope path>:<hub id>:<chapter number>`, example `chapter:freshman/physics:books:3`
- `item:<learning_resources.id>`
- Unlock one hub inside a closed course: `scope:` row = coming_soon plus `hub:` row = open.
- Clear a lock: delete its row.

## LEFT
- Admin UI to create hub / chapter / item rows: the website `LocksPanel` (live website repo) and the native admin panel (`data/admin/LocksRepository.kt`) only offer package, course and hub today. Until then rows are written with SQL.
- Website `ResourceHubGrid` (live website repo) still reads package and scope rows only, so website users still see a course lock on all hubs.
- Locks are an interface gate only: published `learning_resources` rows and the PDF proxy stay readable with the public key. Real enforcement would need RLS on `learning_resources` tied to `content_locks`.
- Hypothesis, not checked: PostgREST `or=(... like."hub:<scope>:*" ...)` quoting; if the course lock read fails, the app falls back to the last answer or the static rule (never a lock-out).

## TEST (owner)
1. GitHub Action build on the branch is green (if red, copy the error to the next agent and log it in `docs/BUILD_PITFALLS.md`).
2. Insert `hub:<course scope>:books` = coming_soon: only the Books card shows the pop-up; the other cards open.
3. Insert `chapter:<scope>:books:2` = coming_soon: Books list opens, chapter 2 rows show a lock and the pop-up, the rest open.
4. Insert `item:<id>` = coming_soon for one material: only that row is locked.
5. Delete the rows: everything opens again within about 30 seconds of reopening the course.
