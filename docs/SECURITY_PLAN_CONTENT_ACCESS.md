# ACTIVE PLAN: real content security (locks enforced by the server, not just hidden in the app)

> Repo is PUBLIC: no secrets, no student data. Owner rules: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`.
> Owner goal (plain): he controls every material from the admin dashboard (lock / unlock a package, hub, chapter or single material). Locked content must NEVER reach a student who is not allowed. App may be shared as an APK on Telegram before Play Store approval, so it must survive someone taking the APK apart. Honest limit: no app is 100% reverse-engineering-proof; the real protection is that locked content stays on the server.

## STATE FOUND 2026-10-20 (read from live Supabase + repo, not guessed)
- Supabase project "Wisdom tower academy". Table `public.content_locks` (keys `item:`, `chapter:`, `hub:`, `scope:`, `package:`, `system:enforce`; most specific wins). See `docs/SESSION_2026-10-20_ACCESS_CONTROL_DB.md` for the rules and the PAID MODE switch (`system:enforce`, mode `locked` = ON).
- Admin screen Content locks (`ui/admin/LocksScreen.kt`) is done (hub, chapter, item, Paid mode card). Student UI shows locks (`ContentAccess.kt`, `HubItemListScreen.kt`).
- **THE GAP:** migration `learning_resources_always_listable` (2026-10-20 05:19) put policy `lr_public_select_published` back, so EVERY published row (`body_md`, `meta`, `storage_path`) is readable with the public anon key. Locks are an app-side gate only. The doc `SESSION_2026-10-20_ACCESS_CONTROL_DB.md` (written 04:58) is STALE on this point: it describes RLS enforcement that was later reverted.
- `public.open_resource(p_id uuid)` exists (migration `open_resource_checked_door_and_item_count`): signed-in only, applies locks + Paid mode + purchases/grants through `content_guard.resource_visible`, returns `{status: ok|locked|not_found|not_signed_in, row?, gate?}`. anon cannot run it.
- `learning_resources.item_count` column added (card/question count without heavy `meta`).
- Student token pattern: `AuthStateManager.refreshIfNeeded()` then `AuthStateManager.currentUser.value?.accessToken` (see `LearningProgressRepository.kt`).
- Book PDFs live in Appwrite; the website proxy `/api/content/pdf` serves them with no lock check (website, not priority). A known Appwrite file id downloads without any check. The app builds the Appwrite address from `storage_path` (`appwriteViewUrl`).
- Flashcards / quiz / exam screens load their content through `getResource` (checked door), NOT from the list row.
- Advisors: admin_* functions check admin inside (OK). Open: view `public.leaderboard_by_scope` is SECURITY DEFINER (owner decision on full names pending); leaked-password protection OFF (owner dashboard toggle).

## SLICES (tick when pushed to main)
- [x] **S1 (app, additive) — pushed 2026-10-20, NOT compiled, NOT tested on a device.** `LearningContentRepository.getResource` / `fetchItem` ask `rpc/open_resource` with the student's token. `locked` -> `ContentItemResult.Failure("This material is locked...")` and the saved copy of that item is deleted; `not_found` -> NotFound; `ok` -> row. Constant `LEGACY_DIRECT_READ = true` keeps the old direct read as a fallback (no token, HTTP error, odd answer). **S4 must set it to false.** Known weakness: cache-first still shows a saved copy once before the background check deletes it. If CI is red, paste the line starting with `e: file:///` to the next agent and log it in `docs/BUILD_PITFALLS.md`.
- [x] **S2 (app) — pushed 2026-10-20 (3 commits), NOT compiled, NOT tested on a device.** Lists are ALWAYS light now: `LearningContentRepository.list` ignores `full` and never selects `body_md` / `meta`; `LIGHT_COLUMNS` now includes `item_count`; new field `LearningResource.itemCount` (default null); `HubItemListScreen` shows "N cards / N questions" from `itemCount` and no longer asks for a full list. Heavy content only through `getResource` (door). Still in the light list: `storage_path` (book file address) -> slice S5. Old saved lists on phones have no `item_count` until the next online refresh (counts may be blank once).
- [ ] **S3 (app hardening for the Telegram APK):** release build with R8 minify + resource shrink on (check `proguard-rules.pro` first: HANDOVER notes it was never read, shrinking can break JSON parsing), no secrets in the APK besides the public anon key, tamper/debug check (simple), `android:allowBackup=false`, network security config (no cleartext). Read `app/build.gradle.kts`, `AndroidManifest.xml`, `proguard-rules.pro` first. Document what was done and the honest limit.
- [ ] **S4 (DATABASE, only AFTER S1+S2 are released and the owner tested on a phone):** set `LEGACY_DIRECT_READ = false` in `LearningContentRepository.kt`, then column-level revoke of `body_md` and `meta` for `anon` and `authenticated` on `learning_resources` (rows stay listable, content only through `open_resource`; `storage_path` only after S5). THIS BREAKS OLD APP VERSIONS: say so to the owner before doing it. Before the revoke: check with a read-only query that no other app code or website page needs those columns (website is not a priority but would break: tell the owner). Record the migration name here.
- [ ] **S5 (PDF books):** put book file access behind a server check (signed URL from a Supabase function that calls the same lock check) so a known Appwrite id no longer downloads, then hide `storage_path` from the public list. Needs design; write it here before coding.
- [ ] **S6 (owner items):** leaked-password protection toggle, leaderboard view decision, `ece-y3-full` mapping, remove hardcoded `IS_FREE_MODE` in `ContentAccess.kt`.

## OWNER PHONE TEST (give the owner these plain steps after S1+S2 are built green)
1. Admin > Content locks: set one subject's package to Owners only, Chapter 1 of Short notes to Free, Paid mode ON.
2. Sign in on a phone as a student with NO purchase: Chapter 1 opens, the other chapters show a lock and open nothing.
3. Sign in as a student WITH a purchase/grant: everything opens.
4. Paid mode OFF: everything opens for every signed-in student.
5. Flashcards / Question banks / Exams lists still show "N cards" / "N questions" and open normally.

## LIVE SUPABASE CHANGE LOG (append every change made through the connector)
- 2026-10-20 (earlier agent): `content_locks_chapters_audit_signedin_open`, `content_guard_*`, `restore_open_visible_to_app_public_key`, `learning_resources_always_listable`, `open_resource_checked_door_and_item_count`.
- 2026-10-20 (this agent): none (S1 and S2 are app-only).

## PLAN CHANGES
(none yet)
