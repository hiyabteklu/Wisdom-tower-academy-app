package com.wisdomtower.academy.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.BrandLogo
import com.wisdomtower.academy.data.auth.AuthFailureKind
import com.wisdomtower.academy.data.auth.SupabaseAuthClient
import com.wisdomtower.academy.data.auth.SupabaseAuthResult
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.wisdomSelectionPop
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomCardSurfaceGradient
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch

/**
 * Native copy of the website login page (website-reference/src/app/login/page.tsx).
 * Texts, field order and behaviour follow that page. This screen only collects input and calls
 * [SupabaseAuthClient]; storing the session and telling the WebView is done by the caller
 * through [onSuccess] (S2c / S2d in STATUS.md).
 *
 * "Forgot password?" now opens the native [ForgotPasswordScreen] inside this screen (N13, session
 * log BB), so the caller needs no change. [onForgotPassword] is kept only so existing callers still
 * compile; it is no longer called.
 *
 * Glass look (session log 2026-10-13 J, docs/GLASS_PLAN.md): see-through card with a bright top edge,
 * sliding segmented tabs, glass fields, glossy cyan button with a press effect, soft entrance and
 * smooth open/close of the sign-up fields. Only the look and motion changed; texts and logic did not.
 *
 * TalkBack (S3 part 2b, 2026-10-17): roles and spoken states only (tabs, dropdown, level options,
 * buttons, terms checkbox name, tappable links as TalkBack actions, error notice announced).
 * No look, text or logic change.
 *
 * A1 (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md, owner problem 10: fine on his phone, too big and confusing on
 * other phones, the Sign in / Create Account switch must be a PILL, richer look, brand above the form):
 *  - SIZE: the layout measures the room it gets ([BoxWithConstraints]) and derives ONE factor `fit` (1.0 on a roomy screen,
 *    down to 0.80 on a narrow or short one: width / 400 dp and height / 780 dp, the smaller wins). The logo, the title, the
 *    gaps, the card paddings and the button height are multiplied by it. Text fields keep Material's own height (they need
 *    their touch size) and text stays in sp, so the system font size still works; the page scrolls, so a large font size or
 *    display size never hides a field. The card width cap (448 dp, S8) is unchanged.
 *  - SWITCH: the track and the sliding selected part are fully round (pill), the selected part is cyan tinted; each tab
 *    is at least 44 dp high.
 *  - LOOK: a soft cyan glow at the top of the screen and a faint cyan tint at the top of the card (night) on top of the
 *    existing glass; the logo and the name stay above the card.
 *  Texts, fields, validation and the sign in / sign up logic are NOT changed. One sign-in look (S5) is kept.
 *
 * STATUS: written, NOT yet compiled or tested on a device.
 */
enum class AuthMode { SIGN_IN, SIGN_UP }

// Same list as ACADEMIC_LEVEL_OPTIONS in website-reference/src/lib/academic-levels.ts
private val ACADEMIC_LEVEL_OPTIONS = listOf(
    "Grade 9",
    "Grade 10",
    "Grade 11",
    "Grade 12",
    "Remedial",
    "Freshman",
    "3rd Year (ECE)",
    "Other"
)

// D5: day mode. Night keeps the old value, day uses the Ivory & Navy tokens (docs/DAY_MODE_PLAN.md).
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day
private val Slate200: Color get() = dayNight(Color(0xFFE2E8F0), WisdomTextPrimary)
private val Slate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomTextPrimary)
private val Slate400: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)
private val CyanLight: Color get() = dayNight(Color(0xFF67E8F9), WisdomCyan)

// Glass tokens for this screen (same family as ui/theme/WisdomGlass.kt, a little stronger so
// text fields stay easy to see on the card).
private val FieldBackground: Color get() = WisdomInk.copy(alpha = 0.06f)
private val FieldBorder: Color get() = WisdomInk.copy(alpha = 0.12f)
private val GlassRowFill: Color get() = WisdomInk.copy(alpha = 0.04f)
private val GlassRowBorder: Color get() = WisdomInk.copy(alpha = 0.10f)

private class AuthNotice(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

private class LinkSpec(val label: String, val action: () -> Unit)

@Composable
fun NativeSignInScreen(
    onSuccess: (SupabaseAuthResult.Success) -> Unit,
    modifier: Modifier = Modifier,
    initialMode: AuthMode = AuthMode.SIGN_IN,
    initialIdentifier: String = "",
    onForgotPassword: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onClose: (() -> Unit)? = null
) {
    var mode by remember { mutableStateOf(initialMode) }
    var identifier by remember { mutableStateOf(initialIdentifier) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("Freshman") }
    var customLevel by remember { mutableStateOf("") }
    var levelPickerOpen by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<AuthNotice?>(null) }
    var showReset by remember { mutableStateOf(false) }

    // Soft entrance of the card, plays once when the screen opens.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enter by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 560),
        label = "authEnter"
    )
    val chevron by animateFloatAsState(
        targetValue = if (levelPickerOpen) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "levelChevron"
    )
    val reveal = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(280))
    val conceal = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(160))

    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val appContext = LocalContext.current.applicationContext

    fun switchMode(target: AuthMode) {
        mode = target
        notice = null
        levelPickerOpen = false
    }

    fun submit() {
        if (loading) return
        notice = null
        val id = identifier.trim()
        if (id.isEmpty()) {
            notice = AuthNotice("Please enter your email or phone number.")
            return
        }

        if (mode == AuthMode.SIGN_IN) {
            if (password.isEmpty()) {
                notice = AuthNotice("Please enter your password.")
                return
            }
            focus.clearFocus()
            loading = true
            scope.launch {
                when (val result = SupabaseAuthClient.signIn(id, password, appContext)) {
                    is SupabaseAuthResult.Success -> onSuccess(result)
                    is SupabaseAuthResult.Failure -> {
                        loading = false
                        notice = AuthNotice(result.message)
                    }
                }
            }
            return
        }

        // Create Account
        if (fullName.trim().isEmpty()) {
            notice = AuthNotice("Please enter your full legal name.")
            return
        }
        if (password.length < 6) {
            notice = AuthNotice("Password must be at least 6 characters.")
            return
        }
        if (password != confirmPassword) {
            notice = AuthNotice("Passwords do not match.")
            return
        }
        if (!agreedToTerms) {
            notice = AuthNotice("Please accept the Terms of Service to continue.")
            return
        }
        val finalLevel =
            if (educationLevel == "Other") customLevel.trim().ifEmpty { "Other" } else educationLevel
        focus.clearFocus()
        loading = true
        scope.launch {
            when (val result = SupabaseAuthClient.signUp(fullName.trim(), id, password, finalLevel, appContext)) {
                is SupabaseAuthResult.Success -> onSuccess(result)
                is SupabaseAuthResult.Failure -> {
                    loading = false
                    when (result.kind) {
                        AuthFailureKind.USER_EXISTS -> {
                            mode = AuthMode.SIGN_IN
                            notice = AuthNotice(
                                "An account with this email or phone already exists. Please enter your password to sign in."
                            )
                        }
                        AuthFailureKind.CONFIRM_EMAIL_SENT -> {
                            mode = AuthMode.SIGN_IN
                            notice = AuthNotice(result.message)
                        }
                        else -> notice = AuthNotice(result.message)
                    }
                }
            }
        }
    }

    // Native password reset (N13): shown instead of the form until the student goes back.
    if (showReset) {
        ForgotPasswordScreen(
            initialEmail = identifier.trim().takeIf { SupabaseAuthClient.looksLikeEmail(it) }.orEmpty(),
            onBackToSignIn = { showReset = false },
            onPasswordChanged = { changedEmail ->
                if (changedEmail.isNotEmpty()) identifier = changedEmail
                password = ""
                mode = AuthMode.SIGN_IN
                notice = AuthNotice("Your password has been successfully changed.")
                showReset = false
            },
            modifier = modifier,
            onClose = onClose
        )
        return
    }

    // A1: BoxWithConstraints so the layout can size itself to the room it really gets (see the header comment).
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 1.0 on a roomy screen, down to 0.80 on a narrow or short one. Only dp sizes use it; text stays in sp.
        val fit = minOf(maxWidth.value / 400f, maxHeight.value / 780f).coerceIn(0.80f, 1f)
        val gap = (16 * fit).dp

        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        // A1: soft cyan glow at the top of the screen (on top of the chosen background, under everything else).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val center = Offset(size.width / 2f, 0f)
                    val radius = size.width * 0.95f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(WisdomCyan.copy(alpha = 0.14f), Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = (16 * fit).dp, vertical = (24 * fit).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header branding: logo with a soft cyan glow behind it
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = enter }
                    .drawBehind {
                        val r = size.minDimension * 1.4f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(WisdomCyan.copy(alpha = 0.26f), Color.Transparent),
                                radius = r
                            ),
                            radius = r
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                BrandLogo(size = (64 * fit).dp)
            }
            Spacer(modifier = Modifier.height((14 * fit).dp))
            Text(
                text = "Wisdom Tower Academy",
                color = WisdomInk,
                fontSize = (26 * fit).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = enter }
            )
            Spacer(modifier = Modifier.height((22 * fit).dp))

            // Glass auth card
            val cardShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .widthIn(max = 448.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = enter
                        translationY = (1f - enter) * 48f
                    }
                    .clip(cardShape)
                    .background(
                        if (AppThemeMode.isDark) {
                            Brush.verticalGradient(
                                listOf(
                                    WisdomCyan.copy(alpha = 0.10f),
                                    WisdomInk.copy(alpha = 0.03f)
                                )
                            )
                        } else {
                            WisdomCardSurfaceGradient
                        }
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    WisdomInk.copy(alpha = if (AppThemeMode.isDark) 0.28f else 0.14f),
                                    WisdomInk.copy(alpha = if (AppThemeMode.isDark) 0.06f else 0.05f)
                                )
                            )
                        ),
                        cardShape
                    )
            ) {
                // Bright top edge (thin cyan sheen, like light catching the glass)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.7f)
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    WisdomCyan.copy(alpha = 0.75f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.padding(
                        start = (22 * fit).dp,
                        end = (22 * fit).dp,
                        top = (24 * fit).dp,
                        bottom = (18 * fit).dp
                    )
                ) {
                    // Mode tabs: pill shaped segmented control with a sliding pill
                    GlassModeTabs(mode = mode, onSelect = { switchMode(it) })
                    Spacer(modifier = Modifier.height((20 * fit).dp))

                    // Full legal name (sign up only)
                    AnimatedVisibility(
                        visible = mode == AuthMode.SIGN_UP,
                        enter = reveal,
                        exit = conceal
                    ) {
                        Column {
                            AuthField(
                                label = "Full Legal Name",
                                value = fullName,
                                onValueChange = { fullName = it },
                                placeholder = "e.g. Abebe Bikila",
                                leadingIcon = Icons.Filled.School
                            )
                            Spacer(modifier = Modifier.height(gap))
                        }
                    }

                    // Email or phone
                    AuthField(
                        label = "Email or Phone Number",
                        value = identifier,
                        onValueChange = { identifier = it },
                        placeholder = "name@email.com or 09xxxxxxxx",
                        leadingIcon = if (identifier.contains("@")) Icons.Filled.Email else Icons.Filled.Phone,
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(modifier = Modifier.height(gap))

                    // Academic level (sign up only)
                    AnimatedVisibility(
                        visible = mode == AuthMode.SIGN_UP,
                        enter = reveal,
                        exit = conceal
                    ) {
                        Column {
                            Text(
                                text = "ACADEMIC LEVEL",
                                color = Slate200,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val pickerShape = RoundedCornerShape(14.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(pickerShape)
                                    .background(FieldBackground)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (levelPickerOpen) WisdomCyan.copy(alpha = 0.85f) else FieldBorder
                                        ),
                                        pickerShape
                                    )
                                    .semantics {
                                        stateDescription = if (levelPickerOpen) "Expanded" else "Collapsed"
                                    }
                                    .clickable(role = Role.DropdownList) { levelPickerOpen = !levelPickerOpen }
                                    .padding(horizontal = 14.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = educationLevel,
                                    color = WisdomInk,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (levelPickerOpen) CyanLight else Slate400,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(chevron)
                                )
                            }
                            AnimatedVisibility(
                                visible = levelPickerOpen,
                                enter = reveal,
                                exit = conceal
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val listShape = RoundedCornerShape(16.dp)
                                    // Stays almost solid (like dropdown menus elsewhere) so it is easy to read.
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(listShape)
                                            .background(dayNight(Color(0x66101A2E), Color(0xEBF8F5EE)))
                                            .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.16f)), listShape)
                                            .padding(vertical = 6.dp)
                                    ) {
                                        ACADEMIC_LEVEL_OPTIONS.forEach { option ->
                                            val isSelected = option == educationLevel
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (isSelected) dayNight(Color(0x3306B6D4), WisdomCyan.copy(alpha = 0.14f)) else Color.Transparent
                                                    )
                                                    .selectable(
                                                        selected = isSelected,
                                                        role = Role.RadioButton,
                                                        onClick = {
                                                            educationLevel = option
                                                            levelPickerOpen = false
                                                        }
                                                    )
                                                    .heightIn(min = 48.dp)
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option,
                                                    color = if (isSelected) CyanLight else Slate200,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = CyanLight,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(gap))

                            AnimatedVisibility(
                                visible = educationLevel == "Other",
                                enter = reveal,
                                exit = conceal
                            ) {
                                Column {
                                    AuthField(
                                        label = "Specify your academic level",
                                        value = customLevel,
                                        onValueChange = { customLevel = it },
                                        placeholder = "e.g. Master's, College Diploma, Self-learner...",
                                        leadingIcon = Icons.Filled.School
                                    )
                                    Spacer(modifier = Modifier.height(gap))
                                }
                            }
                        }
                    }

                    // Password
                    AuthField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        leadingIcon = Icons.Filled.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = if (mode == AuthMode.SIGN_IN) ImeAction.Done else ImeAction.Next,
                        onImeAction = { submit() },
                        isPassword = true,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        labelTrailing = if (mode == AuthMode.SIGN_IN) {
                            {
                                Text(
                                    text = "Forgot password?",
                                    color = Slate400,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable(role = Role.Button) { showReset = true }
                                )
                            }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(gap))

                    // Confirm password + terms (sign up only)
                    AnimatedVisibility(
                        visible = mode == AuthMode.SIGN_UP,
                        enter = reveal,
                        exit = conceal
                    ) {
                        Column {
                            AuthField(
                                label = "Confirm Password",
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = "••••••••",
                                leadingIcon = Icons.Filled.Lock,
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                                onImeAction = { submit() },
                                isPassword = true,
                                showPassword = showPassword
                            )
                            Spacer(modifier = Modifier.height(gap))

                            val termsShape = RoundedCornerShape(14.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(termsShape)
                                    .background(GlassRowFill)
                                    .border(BorderStroke(1.dp, GlassRowBorder), termsShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = agreedToTerms,
                                    onCheckedChange = { agreedToTerms = it },
                                    modifier = Modifier.wisdomSelectionPop(agreedToTerms).semantics { // V7 controls pop
                                        contentDescription = "Accept Terms of Service and Privacy Policy"
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = WisdomCyan,
                                        uncheckedColor = Slate400,
                                        checkmarkColor = WisdomDarkOnCyan
                                    )
                                )
                                TappableText(
                                    text = "I agree to the Terms of Service and Privacy Policy.",
                                    links = listOf(
                                        LinkSpec("Terms of Service") { onOpenTerms() },
                                        LinkSpec("Privacy Policy") { onOpenPrivacy() }
                                    ),
                                    baseColor = Slate300,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(gap))
                        }
                    }

                    // Notice box (glass). Announced by TalkBack when it appears (errors, success).
                    AnimatedVisibility(
                        visible = notice != null,
                        enter = reveal,
                        exit = conceal
                    ) {
                        notice?.let { n ->
                            Column {
                                val noticeShape = RoundedCornerShape(14.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(noticeShape)
                                        .background(WisdomInk.copy(alpha = 0.06f))
                                        .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.14f)), noticeShape)
                                        .semantics { liveRegion = LiveRegionMode.Polite }
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = n.text,
                                        color = Slate300,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    if (n.actionLabel != null && n.onAction != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = n.actionLabel + " →",
                                            color = WisdomCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = TextDecoration.Underline,
                                            modifier = Modifier.clickable(role = Role.Button) { n.onAction.invoke() }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(gap))
                            }
                        }
                    }

                    // Submit button
                    GlassPrimaryButton(
                        label = if (mode == AuthMode.SIGN_IN) "Sign In" else "Create Account",
                        busyLabel = if (mode == AuthMode.SIGN_IN) "Signing In..." else "Creating Account...",
                        loading = loading,
                        enabled = !loading && !(mode == AuthMode.SIGN_UP && !agreedToTerms),
                        verticalPadding = (15 * fit).dp,
                        onClick = { submit() }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TappableText(
                        text = "Account security: this app registers a private installation ID and phone model to protect your account. Privacy Policy.",
                        links = listOf(LinkSpec("Privacy Policy") { onOpenPrivacy() }),
                        baseColor = Slate400,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )

                    // Footer: switch mode
                    Spacer(modifier = Modifier.height((18 * fit).dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(WisdomInk.copy(alpha = 0.08f))
                    )
                    Spacer(modifier = Modifier.height(gap))
                    if (mode == AuthMode.SIGN_IN) {
                        TappableText(
                            text = "Don't have an account? Create a free account",
                            links = listOf(LinkSpec("Create a free account") { switchMode(AuthMode.SIGN_UP) }),
                            baseColor = Slate400,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TappableText(
                            text = "Already have an account? Sign in",
                            links = listOf(LinkSpec("Sign in") { switchMode(AuthMode.SIGN_IN) }),
                            baseColor = Slate400,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (onClose != null) {
            // Round glass close button, same family as the back buttons of the other screens.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(WisdomInk.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.08f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onClose, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Slate300,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * A1: pill shaped segmented control. The track and the sliding selected part are fully round (CircleShape = half of the
 * height as the corner radius); the selected part has a cyan tint. It was a rounded rectangle (16 dp / 13 dp corners).
 */
@Composable
private fun GlassModeTabs(
    mode: AuthMode,
    onSelect: (AuthMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val bias by animateFloatAsState(
        targetValue = if (mode == AuthMode.SIGN_IN) -1f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
        label = "authTabBias"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(dayNight(Color.Black.copy(alpha = 0.22f), WisdomInk.copy(alpha = 0.08f)))
            .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.08f)), CircleShape)
    ) {
        // Sliding pill (sized by the labels row below, so it always matches its height)
        Box(modifier = Modifier.matchParentSize().padding(3.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .align(BiasAlignment(horizontalBias = bias, verticalBias = 0f))
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(WisdomCyan.copy(alpha = 0.34f), WisdomCyan.copy(alpha = 0.18f))
                        )
                    )
                    .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.55f)), CircleShape)
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(3.dp)) {
            ModeTab(
                label = "Sign In",
                selected = mode == AuthMode.SIGN_IN,
                onClick = { onSelect(AuthMode.SIGN_IN) },
                modifier = Modifier.weight(1f)
            )
            ModeTab(
                label = "Create Account",
                selected = mode == AuthMode.SIGN_UP,
                onClick = { onSelect(AuthMode.SIGN_UP) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (selected) WisdomInk else Slate400,
        animationSpec = tween(durationMillis = 220),
        label = "authTabText"
    )
    Box(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/** Glossy cyan button with a soft glow and a small "press in" effect, like an iOS button. */
@Composable
private fun GlassPrimaryButton(
    label: String,
    busyLabel: String,
    loading: Boolean,
    enabled: Boolean,
    verticalPadding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "authButtonScale"
    )
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = WisdomCyan,
                spotColor = WisdomCyan
            )
            .clip(shape)
            .background(if (enabled) WisdomCyan else WisdomCyan.copy(alpha = 0.45f))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.Transparent)
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 24.dp, vertical = verticalPadding),
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

/** `internal` (was private) so [ForgotPasswordScreen] in the same package reuses the same field look. */
@Composable
internal fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    labelTrailing: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                color = Slate200,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            labelTrailing?.invoke()
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text(text = placeholder, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onImeAction() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WisdomInk,
                unfocusedTextColor = WisdomInk,
                focusedContainerColor = FieldBackground,
                unfocusedContainerColor = FieldBackground,
                focusedBorderColor = WisdomCyan.copy(alpha = 0.85f),
                unfocusedBorderColor = FieldBorder,
                cursorColor = WisdomCyan,
                focusedLeadingIconColor = CyanLight,
                unfocusedLeadingIconColor = Slate400,
                focusedTrailingIconColor = Slate400,
                unfocusedTrailingIconColor = Slate400,
                focusedPlaceholderColor = Slate400,
                unfocusedPlaceholderColor = Slate400
            )
        )
    }
}

/**
 * Plain text where the given labels are cyan, underlined and tappable. Uses only stable APIs
 * (tap position -> text offset) so it does not depend on the Compose version.
 * TalkBack cannot "see" tap-position links, so each link is also offered as a TalkBack action
 * (actions menu) with the link's own label.
 */
@Composable
private fun TappableText(
    text: String,
    links: List<LinkSpec>,
    baseColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    textAlign: TextAlign = TextAlign.Start
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentLinks by rememberUpdatedState(links)
    val annotated = buildAnnotatedString {
        append(text)
        links.forEach { link ->
            val start = text.indexOf(link.label)
            if (start >= 0) {
                addStyle(
                    SpanStyle(
                        color = WisdomCyan,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    start + link.label.length
                )
            }
        }
    }
    Text(
        text = annotated,
        color = baseColor,
        fontSize = fontSize,
        textAlign = textAlign,
        onTextLayout = { layout = it },
        modifier = modifier
            .semantics {
                customActions = currentLinks.map { link ->
                    CustomAccessibilityAction(link.label) {
                        link.action()
                        true
                    }
                }
            }
            .pointerInput(text) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    val offset = result.getOffsetForPosition(position)
                    currentLinks.firstOrNull { link ->
                        val start = text.indexOf(link.label)
                        start >= 0 && offset >= start && offset < start + link.label.length
                    }?.action?.invoke()
                }
            }
    )
}
