# TELEGRAM PAID BUILD PLAN (owner order 2026-10-23)

> Repo is PUBLIC: no secrets, no student data. Rules in force: `HANDOFF.md` section 0, G1 to G9 of `docs/UI_POLISH_TASKS.md`. One slice per commit, tick it below in the same commit.

## 1. Owner words (2026-10-23)
"The app is to be submitted to Play Store, but I also wanted to distribute it on Telegram groups. How can I keep this version for the Play Store and make another one with purchase icons and prices etc visible?" Then: "Without changing package name, two separate apps, because notifications should work, all signing keys GitHub secrets, Vercel, Supabase all the same." "Instead of the 'Package required' pop-up without clear steps, I will enable paid mode in a controlled way (lock where enough learning material is stored, unlock the unfinished sections free as a promotion). The in-app purchase flow, upload payment screenshot etc. all in place. I need that." "I hate big rewrites, copying/cloning whole repos."

**This OVERRIDES the older rule "no purchase buttons or prices in the app, ever" (STATUS.md S6, HANDOFF.md money model) for the TELEGRAM BUILD ONLY.** The Play Store build keeps that rule exactly.

## 2. Verified facts (code read 2026-10-23, nothing run)
- The website already has the whole payment flow: `website-reference/src/app/checkout/[packageId]/page.tsx` + `components/CheckoutForm.tsx` (payment methods and account details from `data/packages.ts`, "Enter TX ID" with auto-verify through `/api/verify-payment`, "Upload receipt" screenshot or PDF, order reference), `/orders`, `/cart`.
- BUT `website-reference/src/lib/ownership.ts` has `export const IS_FREE_MODE = true;` (hard-coded, comment: Play Store review). While true, checkout shows "is Unlocked" and all price / cart UI is hidden. The website repo is `hiyabteklu/Wisdom-tower-academy` (not edited from here without the owner's approval).
- The native app decides ownership only from the DATABASE (`public.my_package_access()` + `public.content_locks`, see `ContentAccess.kt`). A locked course shows the native "Package required" card with no price and no button (`HubGate.PackageRequired`). The app has no price or checkout screen today (code search found none).
- CI (`.github/workflows/build-apk.yml`) builds a signed AAB and APK from the GitHub secrets and fails without them. Package name stays `com.wisdomtower.academy` for both builds, so Firebase push keeps working. Hypothesis: a Telegram APK and the Play-installed app are signed with different keys (Google re-signs Play builds), so one cannot update the other; a student moving between them must uninstall first.

## 3. Design
1. ONE code base. A build switch `BuildConfig.PAID_UI` (false by default, true only when Gradle runs with `-PpaidUi=true`). Every paid screen checks it, so the Play build is byte-for-byte the current behaviour.
2. CI makes two APKs per run: `Wisdom-tower-academy-APK` (Play, as now) and `Wisdom-tower-academy-TELEGRAM-APK` (paid UI on). The AAB stays Play-only. The Telegram step is `continue-on-error` so it can never break the Play build.
3. Paid build only: package cards show the price (copied from the website `data/packages.ts`, never invented); the "Package required" card shows package name, price and a Buy button that opens the website checkout inside the app (signed in) and an Orders link; after the student comes back the app re-reads ownership.
4. Website (separate repo, owner approval needed): `IS_FREE_MODE` reads a Vercel env var instead of being fixed, so the owner switches paid mode with no code push. Also decide what to do with `FREE_FOR_REGISTERED_PACKAGE_IDS` (freshman, grades 9 to 12, ECE are listed free there).
5. Database (owner, no code): finished courses locked as `require_purchase`, unfinished ones left `open` as the promotion; the enforcement lock `system:enforce` must be ON or locks do nothing. Payments verified by `/api/verify-payment` or by the admin dashboard grant access.

## 4. Slices (one per commit)
- [~] **P1 Build switch + second APK** (2026-10-23): `app/build.gradle.kts` (`PAID_UI` field) and `.github/workflows/build-apk.yml` (step "Build Telegram APK", artifact `Wisdom-tower-academy-TELEGRAM-APK`). Written, NOT compiled. Owner approved touching these two forbidden files by saying "Go" to this plan (2026-10-23).
- [ ] **P2** price on package cards (paid build only).
- [ ] **P3** "Package required" card with price + Buy + Orders (paid build only).
- [ ] **P4** re-read ownership after checkout.
- [ ] **P5 (owner approval, website repo)** `IS_FREE_MODE` from a Vercel env var.

## 5. Phone test
1. Run Actions: two APK artifacts appear. Play APK: nothing changes anywhere. Telegram APK: installs as the same app (uninstall the other first).
2. After P2 to P4: in the Telegram APK a locked course shows price and Buy; checkout opens signed in; send a test payment; the admin unlocks; the course opens.

## 6. Status log (append only)
- 2026-10-23 P1 written. Not compiled. Next: P2.
