# ACTIVE PLAN: app updates outside Play Store, forced update, FLAG_SECURE, paid sections (owner order 2026-11-03)

> Repo is PUBLIC: no secrets, no student data. Owner rules: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md` (plan first, one slice = one commit pushed to `main`, update this file in the same commit, record every live Supabase change below, plain words to the owner).
> Written by Claude from reading `app/build.gradle.kts`, `AndroidManifest.xml`, `IntegrityGuard.kt`, `docs/SECURITY_PLAN_CONTENT_ACCESS.md`, `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md`. Nothing here is built yet. Nothing was compiled or tested.

## 1. Situation (owner, 2026-11-03)
- The app was shared on Telegram about a week ago, BEFORE it was security hardened (no `FLAG_SECURE`).
- Play Store release is delayed until the owner can pay for the developer account. Meanwhile the APK keeps being shared on Telegram.
- The owner wants: (a) a way to update installed apps without Play Store, (b) to force the already released old version to update, (c) a forced update with a "what's new" popup in the new release, (d) to know what happens when paid content is added to sections that were not ready at the first release (Remedial, EUEE, others).

## 2. Facts found in the code (read, not guessed)
- `app/build.gradle.kts`: `applicationId = com.wisdomtower.academy`, `versionCode = 3`, `versionName = "1.0.0"`. The Telegram APK is built by CI with `-PpaidUi=true` (`BuildConfig.PAID_UI`), same package name as the Play build.
- Release signing: CI refuses to build a release without the upload keystore secrets (`KEYSTORE_BASE64` etc.). Local builds without the keystore fall back to the DEBUG key ("NOT for Play Store or public Telegram production"). An update installs only over an app with the SAME signing key.
- `AndroidManifest.xml`: permissions are INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED only. NO `REQUEST_INSTALL_PACKAGES`, NO FileProvider, no updater. So the released app CANNOT update itself and cannot be forced from inside. It does have Firebase push notifications and in-app admin broadcasts.
- `FLAG_SECURE` is two commented lines in `MainActivity.kt` (around lines 845 and 892 per the security plan). Standing decision 4 (2026-10-20): stays off until the owner says the app is finished. Android only applies it to an installed app through an update.
- Server protection already live (security plan S1 to S4, 2026-10-20): `learning_resources.body_md` and `meta` are hidden from the public key; notes, flashcards, quizzes and exams open only through `open_resource` (locks, Paid mode, purchases). Apps older than S1 cannot open those at all. The checks enforce locks only when `content_locks` row `system:enforce` = `locked` (Paid mode). The state of that flag was NOT checked for this plan.
- NOT protected: PDFs (Life Savers and any paid PDF). They sit in one public Appwrite bucket (security plan S5 is not built). A known file id downloads with no check, on any app version.
- Locks are a screen gate in the app (`ContentAccess.kt`); the old release has no purchase screen.

## 3. Answers to the owner's questions (plain)
1. **Update without Play Store:** possible. The new APK installs over the old one only if package name and signing key are the same and `versionCode` is higher. Back up the keystore (lose it = nobody can ever update). When Play Store comes, choose "upload your own key" in Play App Signing, otherwise Telegram users cannot update to the Play version without uninstalling (they would lose local data).
2. **Force the old release (v3) to update:** NOT from inside (no updater in it). Only from outside: Telegram announcement, push notification / admin broadcast, and a server side block (section 5, slice U5). The block is blunt: the old app shows a generic "locked / unable to load" message.
3. **Forced update with popup in the new release:** yes, slices U1 to U4.
4. **Paid sections added later:** the old app shows no purchase screen. Notes, flashcards, quizzes and exams of locked rows stay closed to it IF the server enforces (Paid mode on; the Play app is not out, so turning it on only affects Telegram users). Paid PDFs are open to everybody until S5. So publish paid sections only after the minimum version is raised, and keep paid PDF sections unpublished until S5.

## 4. Slices (one commit each, tick when pushed to `main`)
- [x] **U0 Owner checks (no code):** (a) Confirm release keystore is backed up somewhere safe outside GitHub Secrets. Current repo `versionCode=3` and Telegram workflow builds with `-PpaidUi=true`, but the shipped artifact facts are not independently verified. (b) Supabase Paid mode checked live: ON (`system:enforce=locked`). (c) Owner decision: keep `FLAG_SECURE` disabled until finishing app/screenshots. (d) Owner instruction: do not block old v3 until announcement and new APK release; server-side block is deferred.
- [x] **U1 Version table (Supabase, additive).** Live table `public.app_versions` created via Supabase migration `app_versions_update_delivery`: `version_code int primary key`, `version_name text`, `changelog text`, `apk_url text`, `min_version_code int`, `published boolean default false`, `created_at`. RLS enabled; anon/authenticated can read published versions, admins can read all and insert/update/delete. Explicit Data API grants added. Verified policies through `pg_policies`. No release row published yet.
- [x] **U2 App: version check + popup.** Added `data/update/UpdateRepository.kt` (reads latest published `app_versions` row using public anon key, caches for offline fallback) and `ui/update/UpdatePrompt.kt` (version/changelog dialog). MainActivity check wired at launch and resume, hourly throttling, optional update flow; `scripts/patch_app_update_u2.py` applied on owner branch. Lifecycle compile fix script `patch_app_update_u2_lifecycle_scope.py` applied and merged via PR #97; owner reports build green. Current interim Update now opens APK URL externally; U4 replaces with in-app download.
- [x] **U3 App: forced mode (implementation committed; CI compile/device test pending).** `UpdatePrompt.kt` now omits Later and dismissal in forced mode; Back exits the Activity rather than dismissing the dialog. Dialog remains modal so app content cannot be interacted with underneath. Forced status derives from the cached/published release `min_version_code`; repository offline fallback uses its cached release row.
- [ ] **U4 App: download and install (implementation committed; release build/device test pending).** Added `REQUEST_INSTALL_PACKAGES`, a non-exported AndroidX `FileProvider`, and cache-only `res/xml/update_file_paths.xml`. `UpdateRepository.downloadApk()` streams HTTPS APK bytes to app-private cache, reports determinate/indeterminate progress, checks ZIP/APK magic bytes, and never offers a partial file. `UpdatePrompt` handles the per-app Install unknown apps setting and resumes download after permission is granted; `ApkInstaller` hands the content URI to Android's package installer (student must confirm installation). Old cached APK/partial files are cleaned before a fresh download. CI workflow now publishes the Telegram APK as a versioned GitHub Release asset (`telegram-v{versionName}-{versionCode}`), supports reruns with clobber, and retains the normal Actions artifact. Remaining: compile/device test, then publish a release row in `app_versions` only after the owner announces the new APK. Android owns install completion; app does not silently install or delete the current installer file prematurely.
- [ ] **U5 Old release (v3): push from outside.** (1) Telegram post text with the new APK and a short "what's new". (2) Admin broadcast / push notification saying "Update required, get the new version from Telegram" (existing admin broadcasts, no code). (3) OPTIONAL server block, only when the owner decides: the new app calls a new content door (for example `open_resource_v2`) and the old `open_resource` answers old builds with a refusal. Old app shows its generic locked/unable-to-load message, so do (1) and (2) first and give people a few days. Record the migration. Not decided, not started.
- [ ] **U6 FLAG_SECURE (owner decision needed: standing decision 4 says off until the app is finished).** If the owner says yes: enable the two commented lines in `MainActivity.kt` only when `!BuildConfig.DEBUG`, so debug builds (`assembleDebug`) still allow screenshots for mock-ups. Remember `FLAG_SECURE` also blocks screen recording and shows a black preview in the recent apps list. Patch script, not a direct edit.
- [ ] **U7 Paid sections go live (Remedial, EUEE, others).** Order: new build out (U2 to U4, plus U6 if wanted) -> `min_version_code` raised -> Paid mode confirmed on -> lock rows set in Admin > Content locks -> THEN publish the paid rows. Keep sections unpublished or Coming soon until then. Test with the OLD APK and a test account without purchase: a locked note must NOT open. Paid PDFs: do not publish before security plan S5 (private bucket + file tokens), otherwise they can be downloaded free by anyone.

## 5. Release routine for every new Telegram build (checklist)
1. Raise `versionCode` (and `versionName`) in `app/build.gradle.kts`. Never reuse a number.
2. CI builds with the real upload keystore (never the debug key).
3. Smoke test on a phone: sign in, open a note, flashcards, a quiz, a notification (R8 is on).
4. Upload the APK as a GitHub release asset; insert the `app_versions` row (`published = true`) with changelog and link.
5. Post on Telegram. Raise `min_version_code` only when the old version must stop (for example a security fix).

## 6. Honest limits
- Android always asks the student to confirm an install; there is no silent update outside Play Store.
- An old version that is never opened again cannot be reached except by Telegram, push notification and the server block.
- `FLAG_SECURE` and the other hardening only stop casual copying; the real protection is the server (see the security plan).
- A student who legitimately downloaded a PDF keeps it on the phone.

## 7. LIVE SUPABASE CHANGE LOG (append every change)
- 2026-11-03: `app_versions_update_delivery` — created `public.app_versions`, enabled RLS, configured published-row read + admin write policies, and explicit Data API grants. Verified policy definitions. No release row inserted.

## 8. Plan changes / status log (append only)
- 2026-11-03: Owner confirmed old v3 must NOT be blocked until after announcement and new APK release; keep `FLAG_SECURE` disabled until owner finishes app and screenshots. Live Supabase check: `system:enforce` = `locked` (Paid mode ON). U1 applied and verified. Owner confirmed release keystore backup outside GitHub Secrets. Repo currently has `versionCode=3`; Telegram workflow uses `-PpaidUi=true` (shipped artifact itself not verified). Next: run release build and device test for U2-U4; do not publish app_versions until owner approves announcement/release; U5 block remains explicitly deferred until announcement/release and owner approval; U6 stays disabled.

- 2026-11-03: U4 implementation committed to `main`: secure FileProvider, APK download/progress/validation, unknown-source permission handoff, Android installer intent, and versioned GitHub Release asset publishing in the manual Telegram workflow. Build/device test pending. No release row published; old v3 remains unblocked; FLAG_SECURE remains disabled.

- 2026-11-03: Telegram workflow publish step failed after APK compilation because the sed replacement emitted literal `\\1`, producing an invalid release tag. Fixed on `main` in commit `27e388e` by parsing versionCode/versionName with awk and validating both before creating the tag. Re-run manual workflow; APK build itself had succeeded in run #1152, but release publishing/device update tests remain pending.


---

# RELEASE HARDENING + ADMIN EXPORT ROADMAP (owner approval 2026-11-05)

> **This section is the newest owner-approved direction and overrides conflicting older notes above and in older plans.** It is a plan only: no code or live database change is claimed by this addendum. Keep pushing each implementation slice directly to `main`, one commit per slice, and update this plan in that same commit. Do not put secrets, signing keys, APKs, student data, or private URLs containing tokens in this public repository.

## 9. Owner decisions — authoritative from 2026-11-05

1. **Mandatory updates only.** The app must not offer Later, Not now, close, swipe-away, Back dismissal, or any route into normal app content while the installed version is below the published minimum. Android's own installer confirmation remains required. If the user installs a newer APK externally, the next app launch must detect the installed `versionCode` and continue normally when it meets the minimum.
2. **Both update routes.** In-app APK download/install is the primary route. A Telegram channel post is the external fallback. The in-app update screen must offer the Telegram post link as an alternative download route. The app must not assume that opening Telegram means the update succeeded; it checks the installed version again on the next launch/resume.
3. **Older versions must lose protected access.** A new app's popup alone cannot force an already-installed old app to update. Old builds must be blocked at the server/content-door layer after the new release has been published and users have been announced and given the owner-approved migration window. Do not block the old release before the replacement APK is available and verified.
4. **Student content protection with verified admin exceptions.** Enable screenshot/screen-recording protection for ordinary student use in production. Admin exceptions must be narrowly scoped to a dedicated admin export/capture surface and granted only after fresh server-verified admin authorization. Never use a hardcoded email, client-side role flag, cached preference, or a hidden UI switch as authorization.
5. **Admin leaderboard JPG export.** Add admin-only export of the actual top-three competition results in both 4:3 landscape and 9:16 portrait. Generate the artwork from result data; do not take a screenshot of the leaderboard UI.
6. **Offline material revocation.** Add revocation checks and cache deletion for updated clients. Be honest that a device which stays offline cannot receive a revocation, and a file already legitimately copied outside app storage cannot be remotely erased.
7. **This approval supersedes the 2026-10-20 standing decision to leave FLAG_SECURE off until finishing the app, and the earlier instruction not to revisit leaderboard export.** The new scoped admin exception and export work are now explicitly requested. Other standing content-security decisions remain in force: Books are free public MoE/MoSHE material; Life Savers and other paid PDFs require the private-bucket/token work in S5; do not ask about leaked-password protection.

## 10. Mandatory update experience — required behaviour

### 10.1 Release metadata and admin control
- Continue using `public.app_versions` as the source of truth. Existing fields are `version_code`, `version_name`, `changelog`, `apk_url`, `min_version_code`, `published`, `created_at`.
- Add a separate Telegram post URL field (proposed `telegram_url`) so the app can distinguish a direct APK URL used by the in-app downloader from the Telegram fallback post. Confirm the exact field name and migration before applying it.
- Add an Admin Console release-management screen. A verified admin can draft/edit release name, version code, user-facing changelog, direct HTTPS APK URL, Telegram post URL, minimum supported version, and publish/unpublish status.
- Validate before publishing: positive and never-reused version code; version name present; changelog present; direct APK URL is HTTPS and returns an APK; Telegram URL points to the intended channel post; minimum version is not greater than the published release version; no accidental rollback. Only admins may write release metadata. Public clients may read published rows only.
- Never publish a release row until its APK has been built with the real production upload key, installed over the previous release on a test device, and passed the release smoke test.

### 10.2 Student update screen
- On launch and resume, compare installed `BuildConfig.VERSION_CODE` with the latest published release and `min_version_code`. Preserve reasonable network throttling for ordinary checks, but a known mandatory update must remain locked.
- If installed version is below minimum: show a blocking update screen with release name, version, admin-written changelog, and two actions: **Update in app** (primary) and **Get APK from Telegram** (fallback). No dismiss control, Later action, outside-tap dismissal, Back dismissal, or access to tabs/content.
- In-app route: download only over HTTPS into app-private cache; show progress and recoverable errors; validate APK bytes/package metadata/version before offering the Android installer; use the existing non-exported FileProvider; hand off to Android's package installer. Never silently install, bypass Android prompts, or delete the only valid downloaded installer before Android has consumed it.
- Telegram route: open the exact published Telegram post. On return, re-check installed version. If still below minimum, show the blocking screen again. If the user installed a newer compatible APK, allow entry only after the new installed version is verified.
- Android may still display its “install unknown apps” permission request and Play Protect warning. Do not attempt to bypass or suppress Android security UI. A direct in-app download does not guarantee that Play Protect will stop warning.
- A successful update requires the same application ID (`com.wisdomtower.academy`), the same signing certificate lineage, and a strictly greater `versionCode`. A greater version number alone is not sufficient. Keep the production signing key backed up securely outside GitHub; never commit it.
- If the update service is unavailable, do not silently treat an unknown version as current. Use the last successfully cached release/minimum data where available. If no trustworthy release metadata has ever been cached, show a blocking retry/help state rather than an unverified bypass. Preserve offline access only when the app can establish that no mandatory update is already known.

### 10.3 Release and migration order
1. Increment `versionCode`; never reuse it. Keep package ID and signing identity stable.
2. Build the Telegram production APK with the real upload key. Confirm CI build and release publication succeeded.
3. Install the new APK over the currently shipped APK on a physical phone; verify no uninstall/data wipe is required.
4. Test the in-app download/install route and Telegram fallback route separately. Verify that a newer APK installed externally is recognized after relaunch.
5. Publish the release row with changelog and both URLs. Confirm the blocking update appears on an older supported test build.
6. Announce the exact Telegram post and new version. Send the existing admin broadcast/push notification.
7. Allow the migration window chosen by the owner. Only then raise `min_version_code` and activate server-side rejection for old app builds.
8. Verify old builds cannot fetch protected notes, flashcards, quizzes, exams, or paid materials; verify the new build and website continue to work as intended.
9. Keep a rollback/recovery release ready. Never lower the minimum or republish an old APK as a silent workaround without recording the security impact.

## 11. Server-side old-version enforcement

- A forced popup is not sufficient: an old installed APK has no new updater code and can ignore client-side UI. Enforcement must happen at every protected server content door.
- Inventory every app-facing RPC, Edge Function, API route, and storage-token route used to fetch protected content. Identify which are also used by the website before changing them.
- Add an explicit app-version-aware access path for the new release. The app must send its installed version code through a request field/header that the server validates; never trust a version value supplied only by an untrusted UI.
- After the replacement APK and migration announcement are live, make legacy protected-content doors reject unsupported app versions/build paths. If a legacy door cannot distinguish app from website callers, create a separate app-only door and migrate the new app before disabling the legacy app door. Do not break the website or student progress accidentally.
- Apply the version check consistently to notes, flashcards, quizzes, exams, paid PDFs, offline reauthorization, and any new protected content endpoint. A hidden/disabled button is not enforcement.
- Return a clear machine-readable `update_required` status where the caller supports it. Older releases that cannot understand that status must still be denied access; their generic failure screen is an accepted limitation, so Telegram/push announcement is essential.
- Do not activate the old-version block until the owner confirms the replacement APK is publicly available and the migration window has ended. Record the exact minimum version and activation date in this file.

## 12. Screenshot, recording, and copy protection

### 12.1 Student protection
- Enable `WindowManager.LayoutParams.FLAG_SECURE` for production student sessions. Keep debug builds usable for development/mock-up screenshots.
- Apply protection consistently to the native Activity and every WebView/Compose content surface that can display protected materials. Verify screen capture, screen recording, and recent-apps preview behaviour on a physical phone.
- Review existing WebView anti-copy handling and add scoped restrictions against selecting/copying protected student material where appropriate. Do not break text inputs, accessibility, normal navigation, or legitimate learning interactions.
- Treat client-side anti-copy as a deterrent only. The server remains the actual access-control boundary.

### 12.2 Verified admin exception
- Keep the normal app window protected. Do not clear `FLAG_SECURE` for the whole app merely because a user is signed in or a local role flag says “admin”.
- Provide a dedicated, non-exported admin export/capture Activity or equally isolated surface. Before opening it, perform a fresh server-side admin-role check using the current authenticated session. Deny access on network/auth failure, expired session, role mismatch, or sign-out.
- Remove screenshot protection only inside that isolated export surface and only for the duration of the authorized export. Re-enable protection on exit, backgrounding, session loss, and every error path. Do not grant broad capture access to protected student content.
- Test with a normal student, a verified admin, an expired admin session, and a user whose local UI state is stale. Only the verified admin export surface may be capturable.

## 13. Offline material revocation and paid PDF security

- Preserve the existing OfflineVault private app-storage model. First inventory all cached file types, index entries, and all code paths that open cached content.
- Add a server-controlled revocation/authorization version for each protected material (or a central revocation manifest). On every online refresh and before opening a protected offline item when online, check current access and revocation state.
- If a material is locked, deleted, expired, refunded, or the user's entitlement is revoked, remove its cached file and index entry and prevent reopening. Also purge stale/partial downloads and handle missing/corrupt cache entries safely.
- For protected paid PDFs, complete the existing S5 design before publishing: private Appwrite bucket with file security enabled; server-issued short-lived file tokens only after the Supabase access check; move Life Savers/paid PDFs; delete the old public copies; keep free Books in the existing public bucket. Never expose private file IDs or long-lived storage credentials in public list responses.
- Evaluate encryption-at-rest using Android Keystore for protected offline files, with a clear migration path for existing cached files. Do not claim encryption makes offline revocation absolute.
- Honest limitation: a phone that remains offline cannot receive a new revocation. A student may also retain a copy made outside the app. Revocation can reliably remove app-managed copies and deny future server access after the device reconnects; it cannot remotely erase arbitrary copies.

## 14. Admin competition leaderboard JPG export

- Add export actions only in the admin competition/results workflow. Every export request must perform fresh server-side admin authorization; normal students must not see or invoke the export endpoint/action.
- Use the actual competition result records, not manually typed or screenshot-derived values. Validate that the selected competition has at least three eligible ranked results; handle ties and missing values according to the competition's existing ranking rules.
- Include: Wisdom Tower Academy branding; competition/exam title; subject title; date; top three ranks and winner names; score and total; time taken in readable format. Use the actual saved values and existing rank order. Do not invent missing metadata.
- Produce two polished JPG layouts from one shared result model:
  - 4:3 landscape for posts/cards.
  - 9:16 portrait for stories/status.
- Use a dedicated export renderer (for example Compose/Android drawing to a bitmap) so the output is a clean designed graphic, not a screenshot of the protected app screen. Keep typography legible and allow long names/titles to wrap safely.
- Save/share the JPG through Android's supported media/file APIs with clear success/error feedback. Do not request broad storage access if MediaStore or the system document picker is sufficient.
- Verify both aspect ratios, image dimensions, text clipping, special characters, ties, long names, missing time values, and that only a verified admin can export. Student leaderboard viewing remains unchanged.

## 15. Implementation slices — execution order

One slice = one commit pushed directly to `main`; update this plan in the same commit. Append exact CI run/result and phone-test result. If CI was not seen, the commit message must end in `not compiled`.

- [x] **R1 — Baseline and release audit (read-only inspection completed 2026-11-05; no build/test claimed).** Re-read current updater, CI workflow, signing config, version metadata, update prompt, manifest/FileProvider, and recent commits. Verify U2-U4 against actual `main` code; do not assume the earlier plan status means a green current build. No signing secret is to be read or printed. Confirm actual currently distributed APK version/signing identity with the owner when needed.
- [x] **R2 — Mandatory update UX (code pushed; CI/device test pending).** Every published newer release is mandatory; no Later, outside-tap dismissal, Back dismissal, or normal app content before the release check. If metadata is unavailable and no cached release exists, a retry-only blocking state is shown. The app reads the new nullable `telegram_url`, validates Telegram hosts, opens the fallback post, and rechecks installed version on return. External installation of a newer compatible APK is recognized on the next check. R4 still must validate the APK package/version/signing identity before installer handoff.
- [x] **R3a — Supabase release metadata field.** Live migration `app_versions_telegram_fallback_url` added nullable `public.app_versions.telegram_url`; existing RLS policies and table grants remain in place. Verified the column exists and is readable through the table's existing Data API grant. No release row was published. [ ] **R3b — Admin release console.** Still pending: verified-admin editor, URL/version validation, publish/unpublish controls, and end-to-end tests.
- [ ] **R4 — In-app APK delivery verification.** Compile, run CI, install over the previous APK, test download progress, permission handoff, installer launch, failed/corrupt download recovery, external Telegram install, and post-install version detection. Fix issues before publishing.
- [ ] **R5 — Version-aware server access.** Inventory all callers and build app-only version-aware protected doors; migrate the new app; test website compatibility; stage legacy-door denial. Activate only after owner approves announcement and migration window.
- [ ] **R6 — Student screenshot/copy protection + isolated admin exception.** Enable production FLAG_SECURE; add the dedicated server-authorized admin export surface; restore protection on every exit/background/auth-loss path; device-test student/admin separation.
- [ ] **R7 — Offline revocation.** Add revocation manifest/versions, purge files and index entries, reauthorize protected offline opens, and test online revocation plus offline/reconnect behaviour.
- [ ] **R8 — Paid PDF S5.** Complete private Appwrite bucket and short-lived token flow; migrate Life Savers/paid PDFs; delete public duplicates; keep Books free/public. Requires the owner's Appwrite setup steps documented in the security plan.
- [ ] **R9 — Leaderboard export renderer.** Add verified-admin-only 4:3 and 9:16 JPG exports for the top three, using actual competition data and all required metadata.
- [ ] **R10 — Full release gate.** Run a full production build and phone regression checklist: sign-in/session refresh; all five tabs; learning hubs; GAT/Exit Exam/COC/EUEE/Remedial paths; AI Tutor and math rendering; notes/flashcards/question banks/exams; notifications; offline vault; paid locks; mandatory updater; Telegram fallback; screenshot/admin exception; leaderboard exports; website compatibility; and old-build denial after activation.

## 16. Acceptance criteria — do not call the release ready until all applicable checks pass

- [ ] Production APK is signed with the same valid signing identity as the currently distributed APK and has a greater version code.
- [ ] A student on a below-minimum build sees a blocking update screen with no dismissal route.
- [ ] In-app download/install works, and Android's installer confirmation is handled correctly.
- [ ] Telegram fallback opens the exact published post; after an external install, the app recognizes the new version.
- [ ] No below-minimum app can fetch protected server content after the enforcement date; current website flows remain intact.
- [ ] Student screenshot/screen-recording protection works on protected screens.
- [ ] Only a freshly server-verified admin can enter the isolated export surface; protection is restored on exit/background/session loss.
- [ ] Revoked protected content is removed from app-managed offline storage after the device reconnects; limitations are communicated honestly.
- [ ] Paid PDFs are inaccessible through public bucket URLs before they are released as products.
- [ ] Verified admins can export accurate top-three JPGs in both formats; students cannot.
- [ ] CI and physical-device regression checks are green. A code commit or successful compile alone is not a release sign-off.

## 17. Live Supabase / Appwrite change log for this roadmap

- Existing Supabase migration `app_versions_update_delivery` (2026-11-03): creates `public.app_versions` with published release read and admin write policies.\n- 2026-11-05: `app_versions_telegram_fallback_url` — added nullable `telegram_url text` to `public.app_versions` for the Telegram fallback post. Verified the column exists; RLS remains enabled; existing published-row SELECT and admin-only write policies remain unchanged; anon/authenticated table SELECT grants remain present. No release row was published.
- Future migrations/functions must be recorded here with exact names, purpose, RLS/policy verification, and whether deployed live. Do not write “done” until the connector or owner has verified the live state.
- Appwrite S5 setup is owner-assisted and must record the private bucket ID only if it is safe to be public; never record API keys or tokens.

## 18. Plan changes / status log

- 2026-11-05 (owner approval): mandatory update only; no Later/dismiss/Back escape. In-app APK update is primary and Telegram post is fallback. A newer compatible APK installed externally must be recognized by installed version code. Old versions will be server-blocked only after the replacement release is available, announced, and the migration window ends.
- 2026-11-05 (owner approval): supersedes prior FLAG_SECURE-off and “do not revisit leaderboard” decisions. Student screenshot/recording protection is required, with a narrowly scoped exception only for a dedicated export surface after fresh server-side admin verification. Add admin-only top-three leaderboard JPG export in 4:3 and 9:16.
- 2026-11-05 (planning only): no app code, database migration, Appwrite configuration, release row, or minimum-version change was applied. Existing U2-U4 implementation claims remain unverified until R1/R4. Existing old-version block remains inactive until the staged release/migration process is completed.


- 2026-11-05 (R1 baseline audit, read-only): `UpdateRepository.kt` currently reads only `version_code,version_name,changelog,apk_url,min_version_code`; it has no Telegram post URL. `UpdatePrompt.kt` has a forced mode with no Later button and ignores outside dismissal, but Back finishes the Activity; its non-forced mode still exposes Later. `MainActivity.kt` only assigns the release when `latest.isUpdateAvailable` (latest version code greater than installed), so it can miss a mandatory minimum-version condition if metadata is inconsistent; it also allows app startup when no release metadata/cache is available. MainActivity passes an external APK action callback, while the prompt currently performs its own in-app download, leaving the callback unused. `downloadApk()` validates HTTPS and APK ZIP magic bytes only; it does not verify package name, version code, signing certificate, or APK signature before installer handoff. The manifest already has `REQUEST_INSTALL_PACKAGES` and a non-exported FileProvider restricted to `cache/updates/`. Telegram workflow `.github/workflows/build-apk.yml` is manual-dispatch only, builds the release APK with the production keystore requirement, publishes a versioned GitHub Release asset, and uploads an Actions artifact; it does not publish a Telegram post. Workflow step `restore_mainactivity.py` preserves the checked-in MainActivity when it is present and larger than 50 KB. GitHub combined status and workflow-run queries for source commit `52690bbebe22ff5bc040f933960011bf55ce5754` returned no status checks or associated runs, so current build health is unverified. No code, database, Appwrite, release-row, or minimum-version changes were made in R1.
- 2026-11-05 (R1 next): implement the mandatory gate and Telegram fallback only after the release metadata field is added and verified; then validate downloaded APK identity/version/signature before installation. Do not announce a release or raise the minimum until a production-signed APK passes the physical-device update test.


- 2026-11-05 (R3a complete): Supabase project “Wisdom tower academy” received migration `app_versions_telegram_fallback_url`. Verified `telegram_url` is nullable text and the existing app_versions RLS/policies/table grants remain enabled. This was additive; no existing release data or minimum version was changed. R3b admin UI remains pending. Security Advisor also returned pre-existing project-wide findings unrelated to this column; no unrelated policies/views/functions were changed in this slice.


- 2026-11-05 (R2 implementation pushed): `UpdateRepository.kt` now reads the published `telegram_url` and distinguishes a published release, no published release, and an unavailable check; all published newer versions are mandatory. `MainActivity.kt` gates normal app content until the version check completes and shows a blocking retry state if it cannot verify release status. `UpdatePrompt.kt` has no dismiss/Later action and consumes Back; it offers in-app installation plus the Telegram fallback and rechecks version when returning. Added blocking/checking gate screens. CI compile and physical-device tests have not yet been run. APK package/version/signature validation remains R4.


- 2026-11-05 (R2 safety correction): a successful empty published-release query now retains any previously cached release instead of silently clearing a known mandatory update. A first install with no cached release and no published rows may proceed; an API failure with no cache remains blocked. This is a narrow safety correction to the R2 gate.


- 2026-11-05 (R2 follow-up hardening): periodic metadata refreshes no longer unmount the student's current screen while a check is running. If an already-verified session's hourly refresh fails, it retains the previously verified app state; a first launch with no cached/verified release status remains blocked until retry succeeds. Also corrected nullable Telegram-host validation to use explicit host comparisons. CI/device verification is still pending.


---

# ALREADY-PUBLISHED APK BASELINE — DO NOT CONFUSE WITH THE NEXT RELEASE

**Owner-confirmed public release baseline (2026-11-05):**
- Released source commit: `51e92c84f9ec338c02411640b7d2940f30ba7321` — [commit](https://github.com/hiyabteklu/Wisdom-tower-academy-app/commit/51e92c84f9ec338c02411640b7d2940f30ba7321).
- The owner confirms this is the APK/source version already distributed publicly to students. Treat it as the currently deployed legacy release until the owner confirms a replacement is live.
- At that commit, Android package ID is `com.wisdomtower.academy`, `versionCode = 3`, `versionName = 1.0.0`.
- `FLAG_SECURE` is disabled: the two `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` calls in `MainActivity.kt` are commented out and marked TODO.
- The release therefore does not provide the intended screenshot/screen-recording protection. Other gaps are tracked in this plan; do not assume the newer `main` commits or roadmap changes are present in the already-installed public APK.
- This commit is a baseline reference only. Do not rewrite history, delete it, or treat pushing a newer commit to `main` as updating phones that already have this APK.

## Controlled public-release migration (owner-directed)

The currently released APK remains publicly available while the owner prepares the replacement and communicates the change. Do **not** abruptly revoke the APK, raise the minimum version, or deny student content access before the warning and replacement sequence is complete.

1. **Prepare and verify replacement first.** Build a higher `versionCode` APK with the same package ID and compatible production signing identity. Complete CI and physical-device install-over-existing-app testing, including login/session retention and the mandatory updater.
2. **Publish the replacement route.** Configure the published release row with the direct HTTPS APK URL and exact Telegram channel post URL. Verify both links and the actual APK before announcing.
3. **Warn students publicly.** Announce through the owner's social-media channels and Telegram channel that an update is required, explain the improvements and the deadline/grace period, and point to the exact trusted update post. Use more than one reminder before enforcement.
4. **Migration/grace period.** Keep the current release and protected content doors working during the owner-approved warning window. Do not infer or invent the deadline; record the date/time the owner chooses.
5. **Enforce after warning.** First publish/confirm the mandatory release metadata and verify new clients receive it. Then raise `min_version_code` and activate server-side rejection for unsupported legacy app requests at protected content doors. Old builds may display only a generic access failure if they cannot understand the new response; the public warning is therefore required.
6. **Monitor and support.** Check update uptake, student reports, login issues, and failed downloads during the migration. Keep the replacement APK and a recovery path available.
7. **Only then retire legacy access.** The owner may remove/replace the old public APK link or mark the old release unsupported after the grace period. Removing the download link alone does not uninstall copies already on phones; server-side content enforcement is what prevents an old installed app from continuing to fetch protected material.

### Important distinction
- **APK availability/revocation:** removing an old APK from Telegram or another download page prevents future downloads from that link, but cannot remove copies already downloaded or installed.
- **Installed-app blocking:** the old app cannot be remotely uninstalled. It can be denied future access to protected server content once the server checks its version/build path.
- **Offline copies:** server denial cannot erase files a student already copied outside app-managed storage, and a device that stays offline cannot receive a new revocation. Updated clients can remove app-managed cached materials when they reconnect and receive revocation state.
- **Do not block the website accidentally.** Version enforcement must distinguish app callers from website callers or use a dedicated app-only protected-content route.

## Baseline-specific execution notes
- Treat `versionCode 3 / versionName 1.0.0` as the known public baseline for migration planning. Confirm the actual installed APK/signing lineage with a device/build artifact before publishing the next release; do not assume the source commit alone proves the signing certificate of the distributed binary.
- The next release must have a strictly greater version code and retain `com.wisdomtower.academy` plus the production signing identity.
- The existing disabled `FLAG_SECURE` in this baseline is a known gap, not a reason to break current users immediately. Enable student protection in the replacement build only after the scoped admin export exception is implemented and tested.
- 2026-11-05 owner instruction: the public legacy APK may remain available during the social-media warning/migration period; after users have been warned and the replacement is available, the owner intends to retire the old release and/or deny protected content to old versions. The exact warning deadline and enforcement date remain **not yet chosen**.


- 2026-11-05 (R4 code hardening): `ApkInstaller.kt` now inspects the downloaded APK before opening Android's installer. It rejects unreadable archives, a different application ID, a version that is not newer than the installed app, and signing certificates not accepted by the installed app's signing identity/lineage (with an exact signer match on Android 7/8). Android's package installer still performs its own final verification and requires user confirmation. This is code-only; no APK was built or device-tested.
- 2026-11-05 (R3b implementation): added native Admin > App Releases editor, backed by `AppReleasesRepository.kt` and the existing signed-in admin PostgREST helper/RLS. It lists recent releases, creates/edits version metadata, validates HTTPS APK URLs and Telegram post hosts, and saves drafts or published rows. The UI warns that publishing a newer version immediately makes it mandatory. Compile, live-admin-permission and end-to-end checks remain pending; no release row was created or published.
- Workflow decision (owner): batch source work and update the plan in grouped commits. Do not run a production APK build for each commit. Build once at a meaningful milestone (R3b + R4 + R6 integration, then final release gate) unless a targeted compile check is needed to resolve a concrete source issue. No build or phone test is claimed for the commits above.


- 2026-11-05 (R5 groundwork, live Supabase migration): migration `app_content_version_gate_v1` replaced the legacy `open_resource(uuid)` and `open_book(uuid)` bodies with compatibility gates that treat the currently distributed legacy client as versionCode 3. Added `open_resource_v2(uuid, integer)` and `open_book_v2(uuid, integer)` for replacement clients; each checks the supplied client version against the latest published `app_versions.min_version_code` before returning protected content. Execute privileges are granted to `authenticated` and `service_role`, revoked from `public` and `anon`. Verified live: authenticated can execute all four functions; anon cannot. A rollback-only SQL test inserted a temporary version row with minimum 4 and confirmed legacy doors return `update_required` while v2 calls reporting version 4 proceed to the normal resource lookup (`not_found` for the deliberately random IDs). The transaction rolled back. The real table still has zero published releases and no active minimum; no student is blocked today.
- 2026-11-05 (R5 app/Edge Function compatibility): replacement app source now calls `open_resource_v2` with `BuildConfig.VERSION_CODE`; the paid-PDF request also includes `client_version`. Deployed `book-url` Edge Function v3 accepts both formats: existing clients without a version continue through the legacy `open_book` gate, while replacement clients use `open_book_v2`. JWT verification remains enabled, and the Appwrite token key remains server-side. The same source is committed at `supabase/functions/book-url/index.ts`. No paid PDF content or token was exposed in this work.
- 2026-11-05 (release build baseline): app Gradle metadata is now prepared as `versionCode 4 / versionName 1.1.0`, above the owner-confirmed public v3 baseline. This is source metadata only; no APK has been built, signed, published, or announced. Keep the live `app_versions` table unpublished until the v4 production APK passes install-over-v3 and device checks.
- 2026-11-05 (R5 verification): a read-only live SQL check confirmed `published_release_count = 0`, no active minimum, and the expected function execute grants. The rollback-only gate test passed as described above. This does not yet prove every app/web protected-data path is version-gated or that the Android client compiles; R5 remains in progress pending endpoint inventory and later phone validation.


- 2026-11-05 (R6 scoped screen capture protection): added `SecureContentCaptureProtection.kt` and attached it only to the native PDF reader, notes reader, flashcards, question banks/exams and ranked competition exam. It applies `FLAG_SECURE` while a protected screen is composed and reasserts it on resume; a reference counter prevents one reader from clearing protection while another protected screen remains active. No app-wide flag is set; home, sign-in, admin console and admin export remain capturable. Debug builds are unaffected. This implements the owner's later scoped request and supersedes the older blanket "keep FLAG_SECURE commented out" note; no app-wide capture lock was enabled.
- 2026-11-05 (R7 admin leaderboard export groundwork): live migration `admin_competition_jpg_export` adds `public.admin_competition_export(uuid)`. It checks `auth.uid()` and `private.is_admin()` on every call, requires a closed competition, runs the same overdue-entry sweep and ranking order as `competition_leaderboard`, excludes disqualified/non-ranked/in-progress entries, returns only the top three, and refuses incomplete result rows. Execute is revoked from public/anon and granted to authenticated/service_role; the authenticated role can call the guarded RPC, anon cannot. A rollback-only non-admin test confirmed the function raises `admin_only`.
- 2026-11-05 (R7 native JPG export UI): added an admin-only top-three export action to ended competitions, with separate 4:3 landscape and 9:16 portrait formats. Data is fetched through the guarded RPC, subject names are resolved from the bundled course catalog, and a branded JPG is drawn in native Android graphics. The Save As system picker writes the JPG to a location chosen by the admin; no broad storage permission and no screenshot capture are used. Source changes are committed; compilation and on-device save/share behavior remain unverified.
