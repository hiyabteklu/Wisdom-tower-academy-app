package com.wisdomtower.academy.ui.packages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.content.PackagePrices

/**
 * TELEGRAM BUILD ONLY (`docs/TELEGRAM_PAID_BUILD_PLAN.md`, slice P2): the dashboard price of a package under its card.
 * Draws NOTHING in the Play build, and nothing for a package that has no price in the admin dashboard.
 * The caller must run `PackagePrices.refresh()` once (PackagesScreen does).
 */
@Composable
fun PackagePriceTag(packageId: String, modifier: Modifier = Modifier) {
    if (!BuildConfig.PAID_UI) return
    val prices by PackagePrices.prices.collectAsState()
    val price = prices[packageId] ?: return
    Text(
        text = PackagePrices.format(price),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFCD34D),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    )
}
