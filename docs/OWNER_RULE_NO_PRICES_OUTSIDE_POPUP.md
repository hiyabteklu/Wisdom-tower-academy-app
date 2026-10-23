# OWNER RULE 2026-10-23: NO PRICES OUTSIDE THE "PACKAGE REQUIRED" POP-UP

This OVERRIDES slice P2 and P2b in `docs/TELEGRAM_PAID_BUILD_PLAN.md` (price under package cards, price on the package page, price under the ECE semester cards). Those were REMOVED (PackagesScreen.kt, PackageDetailScreen.kt). Do not put them back.

Owner words: "clean app, user navigate freely, some chapters will be locked and some free, so when they click on the locked chapter, the purchase of the package pops up and they continue, otherwise they stay free. So if admin unlocked all individual chapters, user can't see any price at all."

Rules:
1. Packages tab, package pages, course lists, cards: NO price, NO Buy button, in both builds.
2. The price shows ONLY in the pop-up that opens when the student taps a LOCKED chapter or item (slice P3: `HubGateDialog.kt` with `PaidCheckoutOffer.kt`), Telegram build only (`BuildConfig.PAID_UI`).
3. If nothing is locked for the student (admin opened everything, or the student owns the package), no price is visible anywhere in the app.
4. Admin approves payments only (no automatic approval).

`ui/packages/PackagePriceTag.kt` and `PackagePrices.refresh()` on those two screens are no longer used by the cards. `PackagePrices` is still used by the pop-up (P3).
