# SESSION 2026-10-23 PAID P5 + P6 (website repo `hiyabteklu/Wisdom-tower-academy`)

Owner said "all agreed, all approved" after the previous agent hit its limit. This covers slices P5 and P6 of `docs/TELEGRAM_PAID_BUILD_PLAN.md` (that plan's checklist still shows them as `[ ]`; read this note as the newer status).

## P5 DONE (written, not compiled): paid mode from Vercel
- Website commit 005dfd2, `src/lib/ownership.ts`: `IS_FREE_MODE = process.env.NEXT_PUBLIC_PAID_MODE !== "true"`.
- Default stays FREE (variable missing or anything but "true"), so the Play review behaviour is unchanged.
- To switch: Vercel > Project > Settings > Environment Variables > `NEXT_PUBLIC_PAID_MODE` = `true`, then REDEPLOY (the value is fixed at build time).
- WARNING: this is the whole website, not only the app. While it is "true", the public website also shows prices, cart and checkout and stops giving everything free. Do not leave it "true" while the Play Store review is running if the review loads the website.
- `FREE_FOR_REGISTERED_PACKAGE_IDS` was NOT changed (grade-9 to 12, freshman, ece-y3-sem-1/2, entrance-questions stay free for any signed-in user in paid mode too). Owner decision still open: this can conflict with the dashboard prices of 500 (grades, freshman) and 300 (ece sems) in the app lock design.

## P6 DONE (written, not compiled): server-side price
- Website commit 5974f38, `src/app/api/verify-payment/route.ts`.
- The server now computes the expected amount from `catalog_items` (service-role read, active rows; dashboard price wins when above 0, else the static `packages.ts` price, same rule as the checkout page). The `amountEtb` sent by the browser is ignored, also for the saved order row and the Verify.ET amount check.
- Several packages (`packageIds`): the server sums them. Unknown package, inactive package, no price, or catalog unreadable: no auto-verify (400, or 503 `PRICE_UNAVAILABLE`, which `CheckoutForm` already treats as "save pending for admin").
- A student who pays the displayed price after the owner changed the dashboard price in the last seconds just ends up pending for admin review.

## STILL OPEN (found while reading, not changed)
1. `CheckoutForm.tsx` also calls `saveOrder(...)` from the browser with `status: "verified"` after `data.verified`. If the `orders` table policy lets a student insert a row with status verified, that is a second way to self-enrol without paying. Needs a read of the `orders` / `enrollments` RLS policies (SQL), not done.
2. `src/lib/catalog.ts` still falls back to the static price when the dashboard price is 0 or missing (owner said "no fallback"). The server uses the same fallback so the two agree; removing it needs both places changed together.
3. Nothing from P1 to P6 is proven to compile. Check the Actions runs after #1056 for the artifact `Wisdom-tower-academy-TELEGRAM-APK` (if the run has `build-log-telegram` instead, read that log).
4. Not done in the app: price on the package detail screen and ECE semester cards, game pop-ups still show the old "Package required" text.
5. Owner steps: set `system:enforce` to `locked` and the `require_purchase` locks in the dashboard, set `NEXT_PUBLIC_PAID_MODE` on Vercel, test on a phone (section 5 of the plan).
