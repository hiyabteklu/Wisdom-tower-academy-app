package com.wisdomtower.academy.ui.study

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wisdomtower.academy.data.content.OrderRepository
import com.wisdomtower.academy.data.content.OrderResult
import com.wisdomtower.academy.data.content.PackagePrices
import com.wisdomtower.academy.data.content.PaymentMethods
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.launch

private enum class PurchaseStep { Offer, Pay, Pending }

/**
 * TELEGRAM BUILD ONLY (S2, `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md`): the native purchase flow, all inside the app, no web
 * page at any point. Shown (by [HubGateDialog]) when a student taps an Owned-only row and the package has a price in the
 * admin dashboard.
 *  1. Offer: package name, price, button "Pay".
 *  2. Pay: the account cards (logo, name, number with Copy), the instructions, the inquiries line, a transaction id field,
 *     "Attach screenshot" and "Submit". Needs a transaction id or a screenshot.
 *  3. Pending: "Pending. We will approve your request in a few minutes." (also shown at once when this student already
 *     has a pending request for the package).
 * The request goes to [OrderRepository] (table `orders`, bucket `payment-receipts`). The admin approves it; the lock
 * screen re-reads ownership when the student comes back to the app (P4).
 *
 * STATUS: written, NOT compiled, NOT seen on a phone.
 */
@Composable
internal fun PaidPurchaseScreen(packageId: String, price: Double, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val packageName = PaymentMethods.packageName(packageId)
    val priceText = PackagePrices.format(price)

    var step by remember { mutableStateOf(PurchaseStep.Offer) }
    var methodId by remember { mutableStateOf(PaymentMethods.all.first().id) }
    var transactionRef by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var copiedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(packageId) {
        if (OrderRepository.hasPending(packageId)) step = PurchaseStep.Pending
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) picked = uri
    }

    fun copy(methodIdToCopy: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("account", text))
        copiedId = methodIdToCopy
    }

    fun submit() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            val result = OrderRepository.submit(context, packageId, price, methodId, transactionRef.trim(), picked)
            busy = false
            when (result) {
                is OrderResult.Success -> step = PurchaseStep.Pending
                is OrderResult.Failure -> error = result.message
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AtmosphereBackground(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (step) {
                    PurchaseStep.Offer -> {
                        Spacer(modifier = Modifier.height(40.dp))
                        Text(
                            text = packageName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WisdomInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = priceText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WisdomCyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PurchaseButton(text = "Pay", enabled = true) { step = PurchaseStep.Pay }
                        PurchaseLink(text = "Close", onClick = onClose)
                    }

                    PurchaseStep.Pay -> {
                        Text(text = packageName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WisdomInk)
                        Text(text = priceText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WisdomCyan)
                        Text(
                            text = "Pay the exact amount with the account you choose, then come back to upload the " +
                                "screenshot or enter the transaction ID.",
                            fontSize = 14.sp,
                            color = WisdomMuted
                        )
                        for (method in PaymentMethods.all) {
                            val selected = method.id == methodId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(WisdomGlassFill)
                                    .border(
                                        BorderStroke(if (selected) 2.dp else 1.dp, if (selected) WisdomCyan else WisdomBorderWhite),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = WisdomCyan.copy(alpha = 0.2f))
                                    ) { methodId = method.id }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BankLogo(asset = method.logoAsset, label = method.shortLabel)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = method.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                                    Text(text = method.accountValue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
                                    Text(text = method.accountName, fontSize = 12.sp, color = WisdomMuted)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                SmallPill(text = if (copiedId == method.id) "Copied" else "Copy") {
                                    copy(method.id, method.accountValue)
                                }
                            }
                        }
                        Text(text = PaymentMethods.INQUIRIES, fontSize = 13.sp, color = WisdomMuted)

                        OutlinedTextField(
                            value = transactionRef,
                            onValueChange = { transactionRef = it.take(60) },
                            label = { Text("Transaction ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = WisdomInk,
                                unfocusedTextColor = WisdomInk,
                                focusedBorderColor = WisdomCyan,
                                unfocusedBorderColor = WisdomBorderWhite,
                                cursorColor = WisdomCyan,
                                focusedLabelColor = WisdomCyan,
                                unfocusedLabelColor = WisdomMuted
                            )
                        )
                        SmallPill(
                            text = if (picked == null) "Attach screenshot" else "Screenshot attached (tap to change)",
                            wide = true
                        ) { picker.launch("image/*") }

                        val message = error
                        if (message != null) {
                            Text(text = message, fontSize = 13.sp, color = WisdomAccentRose)
                        }
                        PurchaseButton(
                            text = if (busy) "Submitting\u2026" else "Submit",
                            enabled = !busy && (transactionRef.isNotBlank() || picked != null)
                        ) { submit() }
                        PurchaseLink(text = "Close", onClick = onClose)
                    }

                    PurchaseStep.Pending -> {
                        Spacer(modifier = Modifier.height(60.dp))
                        Text(
                            text = "Pending",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WisdomCyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "We will approve your request in a few minutes.",
                            fontSize = 15.sp,
                            color = WisdomInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PurchaseButton(text = "Close", enabled = true, onClick = onClose)
                    }
                }
            }
        }
    }
}

/** The bank logo from `assets/images/banks/`; initials on white when the file is not packed yet. */
@Composable
private fun BankLogo(asset: String, label: String) {
    val context = LocalContext.current
    val bitmap = remember(asset) {
        runCatching {
            context.assets.open(asset).use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        }.getOrNull()
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        } else {
            Text(text = label.take(2).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF020617))
        }
    }
}

@Composable
private fun PurchaseButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) WisdomCyan else WisdomCyan.copy(alpha = 0.35f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (AppThemeMode.isDark) Color(0xFF020617) else Color.White
        )
    }
}

@Composable
private fun PurchaseLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = WisdomMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = WisdomMuted.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(vertical = 10.dp)
    )
}

@Composable
private fun SmallPill(text: String, wide: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = (if (wide) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(10.dp))
            .background(WisdomCyan.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.45f)), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = WisdomCyan.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WisdomCyan)
    }
}
