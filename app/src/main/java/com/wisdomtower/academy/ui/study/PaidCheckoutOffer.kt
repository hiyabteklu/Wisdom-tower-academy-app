package com.wisdomtower.academy.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.content.PackagePrices
import com.wisdomtower.academy.data.content.PaidCheckout

/** What the "Package required" card shows and does in the Telegram build. */
internal class PaidOffer(
    val priceText: String,
    val onBuy: () -> Unit,
    val onOrders: () -> Unit
)

/**
 * TELEGRAM BUILD ONLY (slice P3). The offer for the course on screen, or null. Null (so the card stays exactly as in the
 * Play build) when: this is the Play build, no course page is on screen, the course has no package, or the admin
 * dashboard has no price for the package (owner order: no price in the dashboard = nothing shown, no built-in price).
 */
@Composable
internal fun rememberPaidOffer(): PaidOffer? {
    if (!BuildConfig.PAID_UI) return null
    val prices by PackagePrices.prices.collectAsState()
    val scope = PaidCheckout.scope ?: return null
    val opener = PaidCheckout.opener ?: return null
    val packageId = PaidCheckout.packageIdForScope(scope) ?: return null
    val price = prices[packageId] ?: return null
    return PaidOffer(
        priceText = PackagePrices.format(price),
        onBuy = { opener(PaidCheckout.checkoutUrl(packageId)) },
        onOrders = { opener(PaidCheckout.ORDERS_URL) }
    )
}
