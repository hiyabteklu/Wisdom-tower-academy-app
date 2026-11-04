package com.wisdomtower.academy.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomCardShadow
import com.wisdomtower.academy.ui.theme.wisdomDialogEnter

/**
 * Small message pop-up used when a course hub is closed (plan step N9). Layout follows the website's
 * `ComingSoonModal.tsx`: optional amber badge, title, hub name in cyan, sentence, one cyan button.
 * The texts come from `ContentAccess` (copied from the website). In the Play build there is never a price or a purchase button.
 *
 * M11 slice 7: the card scales in and fades (wisdomDialogEnter, 200 ms); closing stays the platform's instant dismiss.
 *
 * D6a (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. By day the card is frosted ivory at 92 percent (overlay rule of the
 * plan) with the soft navy shadow, the title is deep navy ink, the badge is the premium gold (fill F1DFB4, text 5C3F0A) and
 * the button text is white on the deep teal. Night keeps every old literal, so night looks exactly as before.
 *
 * BG-POPUP-3 (owner request 2026-11-04, "make all popups more solid, text mixes with the screen behind"): the card fill is
 * now near-solid, night 96 percent navy 0F1829 (was 30 percent 111B2E), day 97 percent ivory (was 92). To go back: night
 * Color(0x4D111B2E), day Color(0xEBF8F5EE).
 *
 * TELEGRAM BUILD (S2, `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md`): when the title is the "Package required" title and
 * [rememberPaidOffer] has an offer (paid build, course with a package, price in the admin dashboard), this pop-up is NOT
 * shown: the native [PaidPurchaseScreen] (Offer, Pay, Pending) opens instead. The old "Buy now" / "My orders" website
 * opener is removed. With no offer (always in the Play build) the card is exactly as before. Callers are unchanged.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
internal fun HubGateDialog(
    badge: String?,
    title: String,
    hubName: String?,
    message: String,
    buttonText: String,
    onClose: () -> Unit
) {
    val offer = rememberPaidOffer().takeIf { title == ContentAccess.PURCHASE_TITLE }
    if (offer != null) {
        PaidPurchaseScreen(packageId = offer.packageId, price = offer.price, onClose = onClose)
        return
    }
    val cardShape = RoundedCornerShape(24.dp)
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .wisdomDialogEnter()
                .fillMaxWidth()
                .wisdomCardShadow(cardShape)
                .clip(cardShape)
                .background(dayNight(Color(0xF50F1829), Color(0xF7F8F5EE)))
                .border(BorderStroke(1.dp, WisdomBorderWhite), cardShape)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badge != null) {
                Text(
                    text = badge.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = dayNight(Color(0xFFFCD34D), Color(0xFF5C3F0A)),
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(dayNight(Color(0xFFF59E0B).copy(alpha = 0.10f), Color(0xFFF1DFB4)))
                        .border(
                            BorderStroke(1.dp, dayNight(Color(0xFFFBBF24).copy(alpha = 0.25f), Color(0xFF5C3F0A).copy(alpha = 0.25f))),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WisdomInk,
                textAlign = TextAlign.Center
            )

            if (hubName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hubName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WisdomCyan,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = WisdomMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WisdomCyan)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                        onClick = onClose
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = dayNight(Color(0xFF020617), Color.White)
                )
            }
        }
    }
}

// D6a: night value first (exactly the old literal), day value second. A function, so it is read while composing and follows the switch.
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day
