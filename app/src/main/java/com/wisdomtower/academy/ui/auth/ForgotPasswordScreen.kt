package com.wisdomtower.academy.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.BrandLogo
import com.wisdomtower.academy.data.auth.AccountCallResult
import com.wisdomtower.academy.data.auth.RecoveryVerifyResult
import com.wisdomtower.academy.data.auth.SupabaseAccountClient
import com.wisdomtower.academy.data.auth.SupabasePasswordResetClient
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCardSurfaceGradient
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Native password reset (replaces opening the website /forgot-password page).
 * Texts follow website-reference/src/app/forgot-password/page.tsx and reset-password/page.tsx.
 * Three steps: ask for the e-mail, enter the e-mail code + new password, done.
 * Network code is in [SupabasePasswordResetClient]; the new password is set with
 * [SupabaseAccountClient.updatePassword].
 *
 * TalkBack (S3 part 2d, 2026-10-17): buttons say "button", error box and "Code accepted." and
 * "Password updated!" are announced when they appear. Look, texts and logic unchanged.
 *
 * STATUS: written, NOT yet compiled or tested on a device (see session log BB).
 */
private enum class ResetStep { REQUEST, CODE, DONE }

// D5: day mode. Night keeps the old value, day uses the Ivory & Navy tokens (docs/DAY_MODE_PLAN.md).
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day
private val ResetSlate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomTextPrimary)
private val ResetSlate400: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)
private val ResetCyanLight: Color get() = dayNight(Color(0xFF67E8F9), WisdomCyan)

@Composable
fun ForgotPasswordScreen(
    initialEmail: String,
    onBackToSignIn: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    var step by remember { mutableStateOf(ResetStep.REQUEST) }
    var email by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    // Short recovery session after the code was accepted: lets the student fix a weak password
    // without asking for a new code. Never shown, never logged.
    var recoveryToken by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    fun sendLink() {
        if (loading) return
        error = ""
        if (email.trim().isEmpty()) {
            error = "Please enter your email address."
            return
        }
        focus.clearFocus()
        loading = true
        scope.launch {
            when (val r = SupabasePasswordResetClient.requestReset(email)) {
                is AccountCallResult.Success -> {
                    loading = false
                    code = ""
                    password = ""
                    confirmPassword = ""
                    recoveryToken = null
                    step = ResetStep.CODE
                }
                is AccountCallResult.Failure -> {
                    loading = false
                    error = r.message
                }
            }
        }
    }

    fun updatePassword() {
        if (loading) return
        error = ""
        if (recoveryToken == null && code.trim().isEmpty()) {
            error = "Enter the code from the email."
            return
        }
        if (password.length < 6) {
            error = "Password must be at least 6 characters." // UX-CONSISTENCY-2026-10-17: same wording as Settings
            return
        }
        if (password != confirmPassword) {
            error = "Passwords do not match. Please re-enter."
            return
        }
        focus.clearFocus()
        loading = true
        scope.launch {
            val access: String = recoveryToken ?: when (val v = SupabasePasswordResetClient.verifyCode(email, code)) {
                is RecoveryVerifyResult.Verified -> v.accessToken.also { recoveryToken = it }
                is RecoveryVerifyResult.Failure -> {
                    loading = false
                    error = v.message
                    return@launch
                }
            }
            when (val r = SupabaseAccountClient.updatePassword(access, password)) {
                is AccountCallResult.Success -> {
                    SupabasePasswordResetClient.endRecoverySession(access)
                    recoveryToken = null
                    loading = false
                    step = ResetStep.DONE
                }
                is AccountCallResult.Failure -> {
                    loading = false
                    if (r.sessionExpired) recoveryToken = null
                    error = r.message
                }
            }
        }
    }

    LaunchedEffect(step) {
        if (step == ResetStep.DONE) {
            delay(2500)
            onPasswordChanged(email.trim())
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                ResetStep.REQUEST -> {
                    ResetHeader(
                        title = "Reset password",
                        subtitle = "Enter your email and we'll send you a reset link"
                    )
                    ResetCard {
                        AuthField(
                            label = "Email",
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "you@example.com",
                            leadingIcon = Icons.Filled.Email,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                            onImeAction = { sendLink() }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        if (error.isNotEmpty()) {
                            ResetErrorBox(error)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        ResetPrimaryButton(
                            label = "Send Reset Link",
                            busyLabel = "Sending...",
                            loading = loading,
                            enabled = true,
                            onClick = { sendLink() }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        ResetSecondaryButton(
                            label = "Back to Sign In",
                            showBackIcon = true,
                            onClick = onBackToSignIn,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                ResetStep.CODE -> {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x1A22D3EE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = WisdomCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Check your email",
                        color = WisdomInk,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("We sent a password reset email to ")
                            withStyle(SpanStyle(color = WisdomInk, fontWeight = FontWeight.Bold)) {
                                append(email.trim())
                            }
                            append(". Enter the code from the email below and choose a new password. ")
                            append("If the email has only a link, open it in Chrome or Safari, set the new password on the website, then sign in again in the app.")
                        },
                        color = ResetSlate400,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 448.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    ResetCard {
                        if (recoveryToken == null) {
                            AuthField(
                                label = "Reset code",
                                value = code,
                                onValueChange = { code = it },
                                placeholder = "Code from the email",
                                leadingIcon = Icons.Filled.Lock,
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        } else {
                            Text(
                                text = "Code accepted.",
                                color = ResetCyanLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AuthField(
                            label = "New Password",
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "At least 6 characters",
                            leadingIcon = Icons.Filled.Lock,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                            isPassword = true,
                            showPassword = showPassword,
                            onTogglePassword = { showPassword = !showPassword }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AuthField(
                            label = "Confirm Password",
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "Repeat your password",
                            leadingIcon = Icons.Filled.Lock,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            onImeAction = { updatePassword() },
                            isPassword = true,
                            showPassword = showPassword
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        if (error.isNotEmpty()) {
                            ResetErrorBox(error)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        ResetPrimaryButton(
                            label = "Update Password",
                            busyLabel = "Updating...",
                            loading = loading,
                            enabled = true,
                            onClick = { updatePassword() }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ResetSecondaryButton(
                                label = "Back to Sign In",
                                showBackIcon = false,
                                onClick = onBackToSignIn
                            )
                            ResetSecondaryButton(
                                label = "Request new link",
                                showBackIcon = false,
                                onClick = {
                                    error = ""
                                    recoveryToken = null
                                    step = ResetStep.REQUEST
                                }
                            )
                        }
                    }
                }

                ResetStep.DONE -> {
                    Spacer(modifier = Modifier.height(48.dp))
                    ResetCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1A22C55E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = dayNight(Color(0xFF4ADE80), WisdomAccentEmerald),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Password updated!",
                                color = WisdomInk,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your password has been successfully changed. Redirecting to login...",
                                color = ResetSlate400,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            ResetSecondaryButton(
                                label = "Back to Sign In",
                                showBackIcon = true,
                                onClick = { onPasswordChanged(email.trim()) }
                            )
                        }
                    }
                }
            }
        }

        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = ResetSlate300
                )
            }
        }
    }
}

@Composable
private fun ResetHeader(title: String, subtitle: String?) {
    BrandLogo(size = 60.dp)
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = title,
        color = WisdomInk,
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
    )
    if (subtitle != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = ResetSlate400,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ResetCard(content: @Composable ColumnScope.() -> Unit) {
    val cardShape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .widthIn(max = 448.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .border(BorderStroke(1.dp, dayNight(Color(0x33FFFFFF), WisdomBorderWhite)), cardShape)
            .background(
                if (AppThemeMode.isDark) {
                    Brush.verticalGradient(
                        listOf(WisdomInk.copy(alpha = 0.07f), WisdomInk.copy(alpha = 0.04f), WisdomInk.copy(alpha = 0.03f))
                    )
                } else {
                    WisdomCardSurfaceGradient
                }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(WisdomCyan, Color(0xFF7DD3FC), WisdomCyan)
                    )
                )
        )
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp)) {
            content()
        }
    }
}

@Composable
private fun ResetErrorBox(text: String) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(BorderStroke(1.dp, Color(0x33EF4444)), shape)
            .background(Color(0x1AEF4444))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(12.dp)
    ) {
        Text(text = text, color = dayNight(Color(0xFFF87171), WisdomAccentRose), fontSize = 13.sp)
    }
}

@Composable
private fun ResetPrimaryButton(
    label: String,
    busyLabel: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val active = enabled && !loading
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) WisdomCyan else WisdomCyan.copy(alpha = 0.5f))
            .clickable(enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = WisdomDarkOnCyan
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = busyLabel,
                color = WisdomDarkOnCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = label,
                color = WisdomDarkOnCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = WisdomDarkOnCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ResetSecondaryButton(
    label: String,
    showBackIcon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, dayNight(Color(0x1AFFFFFF), WisdomBorderWhite)), shape)
            .background(dayNight(Color(0x0DFFFFFF), WisdomInk.copy(alpha = 0.05f)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackIcon) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = ResetCyanLight,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = ResetSlate300,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
