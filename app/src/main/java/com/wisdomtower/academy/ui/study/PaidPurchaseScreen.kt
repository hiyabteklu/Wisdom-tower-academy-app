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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
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
 * UI REDESIGN (2026-10-23, owner: "the screen is flat, improve the design"): same three steps and same logic, new look.
 * Top bar with Back (Pay step) and Close, a 3-step progress bar, a glowing lock badge, a price card, a "how it works" card
 * with the three real steps of the flow (no invented text), bank cards with a selected check and a copy icon, a big check
 * badge on Pending. Only existing theme colours; icons are Material filled icons (Lock, Close, Check, ContentCopy, AttachFile).
 *
 * STATUS: written, NOT compiled (redesign), logic compiled and seen working on the owner's phone (Offer screen).
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
            ) {
                // Top bar: Back (Pay step only), the 3-step progress bar, Close.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step == PurchaseStep.Pay) {
                        IconButton(onClick = { step = PurchaseStep.Offer }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = WisdomInk
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    StepBar(
                        active = when (step) {
                            PurchaseStep.Offer -> 0
                            PurchaseStep.Pay -> 1
                            PurchaseStep.Pending -> 2
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = WisdomInk
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        PurchaseStep.Offer -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            GlowBadge { Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = WisdomCyan, modifier = Modifier.size(32.dp)) }
                            Text(
                                text = "PACKAGE REQUIRED",
                                fontSize = 12.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold,
                                color = WisdomMuted,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = packageName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WisdomInk,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Price card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(WisdomCyan.copy(alpha = 0.20f), WisdomCyan.copy(alpha = 0.04f))
                                        )
                                    )
                                    .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.45f)), RoundedCornerShape(24.dp))
                                    .padding(vertical = 20.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "PRICE", fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = WisdomMuted)
                                Text(text = priceText, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = WisdomCyan)
                                Text(
                                    text = "The whole package, every chapter and item",
                                    fontSize = 13.sp,
                                    color = WisdomMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                            // How it works (the three real steps of this flow)
                            GlassCard {
                                Text(text = "How it works", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                                HowRow(1, "Pay the exact amount to the account you choose")
                                HowRow(2, "Upload the screenshot or enter the transaction ID")
                                HowRow(3, "We approve your request in a few minutes")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            PurchaseButton(text = "Pay", enabled = true) { step = PurchaseStep.Pay }
                            PurchaseLink(text = "Close", onClick = onClose)
                        }

                        PurchaseStep.Pay -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "PAY", fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = WisdomMuted)
                                    Text(text = packageName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WisdomInk)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = priceText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WisdomCyan,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(WisdomCyan.copy(alpha = 0.12f))
                                        .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.45f)), RoundedCornerShape(50))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            GlassCard {
                                Row(verticalAlignment = Alignment.Top) {
                                    NumberBadge(1)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Pay the exact amount with the account you choose, then come back to upload the " +
                                            "screenshot or enter the transaction ID.",
                                        fontSize = 14.sp,
                                        color = WisdomMuted
                                    )
                                }
                            }
                            Text(
                                text = "CHOOSE AN ACCOUNT",
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold,
                                color = WisdomMuted,
                                modifier = Modifier.fillMaxWidth()
                            )
                            for (method in PaymentMethods.all) {
                                val selected = method.id == methodId
                                val cardShape = RoundedCornerShape(20.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(cardShape)
                                        .background(
                                            if (selected) {
                                                Brush.horizontalGradient(
                                                    listOf(WisdomCyan.copy(alpha = 0.16f), WisdomGlassFill)
                                                )
                                            } else {
                                                Brush.horizontalGradient(listOf(WisdomGlassFill, WisdomGlassFill))
                                            }
                                        )
                                        .border(
                                            BorderStroke(
                                                if (selected) 2.dp else 1.dp,
                                                if (selected) WisdomCyan else WisdomBorderWhite
                                            ),
                                            cardShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(color = WisdomCyan.copy(alpha = 0.2f))
                                        ) { methodId = method.id }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BankLogo(asset = method.logoAsset, label = method.shortLabel)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = method.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                                        Text(text = method.accountValue, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = WisdomInk)
                                        Text(text = method.accountName, fontSize = 12.sp, color = WisdomMuted)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (selected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(WisdomCyan),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Selected",
                                                    tint = if (AppThemeMode.isDark) Color(0xFF020617) else Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        SmallPill(text = if (copiedId == method.id) "Copied" else "Copy", copyIcon = copiedId != method.id) {
                                            copy(method.id, method.accountValue)
                                        }
                                    }
                                }
                            }
                            Text(text = PaymentMethods.INQUIRIES, fontSize = 13.sp, color = WisdomMuted, modifier = Modifier.fillMaxWidth())

                            GlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NumberBadge(2)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Send your proof",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WisdomInk
                                    )
                                }
                                OutlinedTextField(
                                    value = transactionRef,
                                    onValueChange = { transactionRef = it.take(60) },
                                    label = { Text("Transaction ID") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
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
                                    wide = true,
                                    attachIcon = true
                                ) { picker.launch("image/*") }
                            }

                            val message = error
                            if (message != null) {
                                Text(text = message, fontSize = 13.sp, color = WisdomAccentRose, modifier = Modifier.fillMaxWidth())
                            }
                            PurchaseButton(
                                text = if (busy) "Submitting\u2026" else "Submit",
                                enabled = !busy && (transactionRef.isNotBlank() || picked != null)
                            ) { submit() }
                            PurchaseLink(text = "Close", onClick = onClose)
                        }

                        PurchaseStep.Pending -> {
                            Spacer(modifier = Modifier.height(40.dp))
                            GlowBadge {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = WisdomCyan,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Text(
                                text = "Pending",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WisdomCyan,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "We will approve your request in a few minutes.",
                                fontSize = 16.sp,
                                color = WisdomInk,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            GlassCard {
                                Text(text = packageName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                                Text(text = priceText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomCyan)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            PurchaseButton(text = "Close", enabled = true, onClick = onClose)
                        }
                    }
                }
            }
        }
    }
}

/** Three capsules: the current step is a wide cyan bar, finished steps cyan, the rest faint. */
@Composable
private fun StepBar(active: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..2) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (i <= active) WisdomCyan else WisdomBorderWhite)
            )
        }
    }
}

/** A glowing round badge (soft cyan halo, glass disc, thin cyan ring) that holds one icon. */
@Composable
private fun GlowBadge(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                Brush.radialGradient(listOf(WisdomCyan.copy(alpha = 0.35f), Color.Transparent)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(WisdomGlassFill)
                .border(BorderStroke(1.5.dp, WisdomCyan.copy(alpha = 0.7f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** Rounded glass card with the standard padding and spacing. */
@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WisdomGlassFill)
            .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
private fun NumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(WisdomCyan.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.6f)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = number.toString(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = WisdomCyan)
    }
}

@Composable
private fun HowRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberBadge(number)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp, color = WisdomInk, modifier = Modifier.weight(1f))
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
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
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
                    .padding(5.dp)
            )
        } else {
            Text(text = label.take(2).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF020617))
        }
    }
}

@Composable
private fun PurchaseButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(WisdomCyan, WisdomCyan.copy(alpha = 0.78f)))
                } else {
                    Brush.horizontalGradient(listOf(WisdomCyan.copy(alpha = 0.35f), WisdomCyan.copy(alpha = 0.35f)))
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
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
private fun SmallPill(
    text: String,
    wide: Boolean = false,
    copyIcon: Boolean = false,
    attachIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = (if (wide) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(WisdomCyan.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.45f)), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = WisdomCyan.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = if (wide) 14.dp else 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (copyIcon || attachIcon) {
            Icon(
                imageVector = if (attachIcon) Icons.Filled.AttachFile else Icons.Filled.ContentCopy,
                contentDescription = null,
                tint = WisdomCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WisdomCyan)
    }
}
