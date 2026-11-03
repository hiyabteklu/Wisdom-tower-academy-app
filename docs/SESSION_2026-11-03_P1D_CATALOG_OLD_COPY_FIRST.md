# SESSION 2026-11-03 P1d: the catalog answers from the saved copy at once, even when it is old

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1d, owner problem 8).
File changed (1 code file): `app/src/main/java/com/wisdomtower/academy/data/content/ResourceCatalogIndex.kt`. NOT compiled, NOT tested on a device.

## What changed
- Before: `ResourceCatalogIndex.get` returned the saved copy only when it was younger than 24 h. An older copy made the caller WAIT for the server (up to 15 s + 20 s per page, several pages), and used the old copy only after that failed.
- Now: when any non-empty saved copy exists, `get` returns it at once, whatever its age. A copy older than 24 h also starts ONE silent refresh in the background (only one at a time, short client 4 s + 8 s); a good answer replaces the saved copy for the next call, a failed or empty answer leaves it alone. Nothing on screen waits for it.
- `forceRefresh = true` (used by the "My Subjects" filter when it meets a resource the saved map does not know) still reads the server, but with the short client when a saved copy exists, and falls back to the copy on failure. With no saved copy the long client (15 s + 20 s) is used, as before.
- An EMPTY saved catalog counts as "nothing saved" (same rule as P1a to P1c): the screen waits for the live read instead of showing an empty course list.
- `getSaved` (no network, any age) is unchanged.

## Effect on screens
The course board, "By subject" and the hub lists no longer hold on a slow connection when the catalog copy is older than a day. Because the old copy is returned, new content may show up one open later (the refresh finishes in the background and is used the next time).

## Owner test (phone, new build)
1. With data on, open a course page and "Your status" the day after you last used the app: the rings and rows show at once, no long wait.
2. Add or publish a new item on the website, open the app twice: it appears by the second open at the latest.
3. Airplane mode: nothing changes, the saved copy is used as before.

## Next
P1e (measure the rest: `ContentAccess.resolveLocks`, Learning tab, Home, package screens, Notifications, Games hubs; write what waits in the session file, split only if something does).
