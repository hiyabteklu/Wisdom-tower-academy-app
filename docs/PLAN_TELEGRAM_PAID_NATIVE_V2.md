# PLAN: TELEGRAM PAID BUILD, ALL NATIVE (owner order 2026-10-23, replaces `TELEGRAM_PAID_BUILD_PLAN.md`)

> Repo is PUBLIC: no secrets, no student data. Telegram build only (`BuildConfig.PAID_UI`). The Play build never changes: no prices, no locks, everything open. The website is NOT touched.
> OWNER RULE: KEEP IT SIMPLE. Few steps, plain words on screen, no extra modes, no extra warnings. The owner has limited quota: one step = one commit, say what is done in 3 lines.

## 1. What the owner wants (his words, 2026-10-23)
- "Every screen native, no web view at all ever on payment or purchase, till the payment screenshot upload / transaction ID form, all native."
- "The locking and unlocking mechanism should be clean. That is where admin unlocks access for verified payments."
- Admin lock screen: "clean tree like the user content navigation (ece > semester > course > hub > chapter)", not one long list of details. "We don't unlock subjects separately." Actions only at package level and chapter level.
- "If the package is free for everyone, all chapter locks are cleared. If the package is in paid mode, all chapters are locked (all items in all 6 hubs: books, life savers, flashcards ...). While paid, admin can still unlock specific chapters / items for trial (example: chapter 1 of notes, one book, one exam)."
- Tapping a locked chapter or item opens a NATIVE screen: package price, then the account options with their logos (CBE, Telebirr, Abyssinia): copy the account number, the payment instructions ("pay with what you chose and come back to upload the screenshot"), inquiries: 0900763030 or Telegram @hiyab_t. The student uploads the screenshot or enters the transaction ID. Then it says PENDING: "we'll approve your request in a few minutes".
- Admin approves in the admin dashboard (admin only, never automatic). The whole package is then owned by that student.
- Bug seen on phone: the locked chapter opened the WEBSITE checkout, which said "is Unlocked" by itself (website free mode). That path must be gone.

## 2. The model (two words: FREE and PAID)
- PACKAGE: **Free** or **Paid**. Stored as the existing `content_locks` row `package:<package id>`: no row or `open` = Free, `require_purchase` = Paid.
- Free package: everything open, and every chapter / item lock of that package is cleared (admin screen deletes those rows when switching to Free).
- Paid package, student does NOT own it: the 6 hub cards of every course OPEN normally (student navigates freely). Inside a hub, EVERY chapter and item shows locked (lock icon). EXCEPT the ones the admin set to **Open for trial**.
- Trial: existing rows `chapter:<scope>:<hub>:<n>` or `item:<learning_resources.id>` with mode `open`. Only these two kinds of row are used. `scope:` and `hub:` rows are NOT used any more by the Telegram build (no subject-level or hub-level locks).
- Student owns the package (admin approved the payment, or admin granted it): everything open.
- Play build: ignores all of this, always open.
- Open question for step 1 (read first, do not guess): `public.my_package_access()` only enforces when `content_locks` has `system:enforce` = `locked`. Both apps share one database, so the Telegram build must decide by the package row itself (paid + not owned = locked) and NOT by this global flag, otherwise the Play app would be locked too. Read what `my_package_access()` returns and use only its list of owned packages.

## 3. Steps (do in this order, one commit each, tick when done)
- [ ] **S1 Student lock logic** (`data/content/ContentAccess.kt`, `ui/study/HubItemListScreen.kt`, `StudyWorkspaceScreen.kt`): implement section 2. Hubs always open in a paid package; locked rows (lock icon) inside; trial rows open. Telegram build only (`PAID_UI`); Play build open.
- [ ] **S2 Native purchase flow** (new screens, all Compose): tap a locked chapter/item > (1) Offer: package name, price from the admin dashboard (`PackagePrices`), button "Pay"; (2) Pay: account cards (logo, bank name, copy account number, account name), instructions text, inquiries line, field for transaction ID, button to attach the screenshot, button "Submit"; (3) Pending: "Pending. We will approve your request in a few minutes." The order is written to the existing `orders` table (status `pending_verification`), the screenshot to the existing receipts storage bucket, with the student's own session. Account details and logos come from the website `data/packages.ts` `paymentMethods`, copied into the app (logos packed like the other images). Remove the old "Buy now / My orders" web opener from `HubGateDialog.kt` and `PaidCheckoutOffer.kt`; the old pop-up is replaced by step (1). Read first: RLS of `orders` and the receipts bucket (can a student insert his own pending row and upload?). If not, one small SQL for the owner.
- [ ] **S3 Admin Locks screen as a tree** (`ui/admin/LocksScreen.kt`): same drill-down as the student content: Packages (each with a Free / Paid badge) > package (ECE: semester > course) > the 6 hubs > chapters > items. Actions ONLY: on a package: **Free** or **Paid**; on a chapter or an item: **Open for trial** or **Locked again** (deletes the row). Nothing else on screen (no "default", "hard lock", "coming soon" words). Status shown as a small badge on each row, details only after tapping down the tree. I could not tell which "4 actions" the owner meant; these are 2 + 2. Owner may correct.
- [ ] **S4 Admin approval** (existing native admin Payments / Grants screens): approving a pending order must give the student the whole package (access grant or enrollment, whichever `my_package_access()` reads). Read first what the Payments screen does on approve; add the grant only if missing. The student app already re-reads ownership when it comes back to the front (P4).
- [ ] **S5 Cleanup**: delete the unused `PackagePriceTag.kt` and any code that opens the website checkout. Do NOT set `NEXT_PUBLIC_PAID_MODE` on Vercel (no longer needed; the website stays as it is).

## 4. Already done and kept
- `BuildConfig.PAID_UI` and the second APK in CI (`Wisdom-tower-academy-TELEGRAM-APK`, same package name, notifications work). Seen green, artifact present.
- `PackagePrices.kt`: prices from `catalog_items.price_etb` (admin dashboard), saved copy for offline. Used ONLY in the purchase Offer screen. No price anywhere else (`docs/OWNER_RULE_NO_PRICES_OUTSIDE_POPUP.md`).
- P4: ownership re-read when the app returns to the front.
- Admin approval only: website `verify-payment` disabled and the website checkout saves only pending orders (website commits 465ed73, fd752b6). Website `ownership.ts` reads `NEXT_PUBLIC_PAID_MODE` (default free): leave it unset.
- Pending patch (owner action, not applied yet): `scripts/patch_package_tab_highlight.py` (tab highlight bug, see `docs/SESSION_2026-10-23_TAB_HIGHLIGHT.md`).

## 5. Facts to rely on (read earlier, not guessed)
- `content_locks`: `lock_key` unique, `mode` in open / locked / coming_soon / require_purchase, public read, admin write. Keys: `package:<id>`, `chapter:<scope>:<hub>:<n>`, `item:<id>`. Most specific wins: item, chapter, then package. No DB change needed.
- `catalog_items` (12 rows): public read of active rows, admin write. Prices: grades 9-12, freshman, uat, gat, coc, exit-exam 500; ece-y3-full 600; ece sem 1 and 2: 300.
- Locks are an interface gate only (the files stay readable with the public key). Real enforcement would need RLS on `learning_resources`. Not part of these steps.
- The chat GitHub tool cannot write `.github/`. `MainActivity.kt` (265 KB) is edited only with patch scripts (`docs/EDITING_LARGE_FILES.md`).

## 6. Phone test (owner, after S1 to S4 are green)
1. Admin: set a package to Paid; set one chapter to Open for trial.
2. Student (test account): open the package, the 6 hubs open; inside, the trial chapter opens, every other row shows a lock.
3. Tap a locked row: native Offer > Pay (logos, copy account) > submit a transaction ID > Pending. No web page at any point.
4. Admin approves the order. Student comes back to the app: everything in that package opens.
5. Admin sets the package to Free: all locks gone, no lock icon anywhere.
6. Play APK: nothing locked, no price, no purchase screen.

## 7. Status log (append only; if you cannot append, add a new `docs/SESSION_*.md`)
- 2026-10-23 plan written. Nothing from S1 to S5 started.
