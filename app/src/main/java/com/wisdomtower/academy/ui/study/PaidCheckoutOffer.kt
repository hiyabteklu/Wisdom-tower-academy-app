package com.wisdomtower.academy.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.content.PackagePrices
import com.wisdomtower.academy.data.content.PaidCheckout

/** What the native purchase flow needs (S2): the package to sell and its dashboard price. No web address any more. */
internal class PaidOffer(
    val packageId: String,
    val price: Double
)

/**
 * TELEGRAM BUILD ONLY. The offer for the course on screen, or null. Null (so the "Package required" card stays exactly as
 * in the Play build) when: this is the Play build, no course page is on screen, the course has no package, or the admin
 * dashboard has no price for the package (owner order: no price in the dashboard = nothing shown, no built-in price).
 *
 * S2 (2026-10-23): the old "Buy now" / "My orders" website opener is gone; [HubGateDialog] shows [PaidPurchaseScreen] instead.
 */
@Composable
internal fun rememberPaidOffer(): PaidOffer? {
    if (!BuildConfig.PAID_UI) return null
    val prices by PackagePrices.prices.collectAsState()
    val scope = PaidCheckout.scope ?: return null
    val packageId = PaidCheckout.packageIdForScope(scope) ?: return null
    val price = prices[packageId] ?: return null
    return PaidOffer(packageId = packageId, price = price)
}
