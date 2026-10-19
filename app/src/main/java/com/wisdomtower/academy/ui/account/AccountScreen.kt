package com.wisdomtower.academy.ui.account

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.model.UserProfile
import com.wisdomtower.academy.ui.auth.NativeSignInScreen
import com.wisdomtower.academy.ui.competition.CompetitionPointsCard
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomPrimaryBtnGradient
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Option lists. Never invent options here: the chosen value is written to the same
// Supabase `profiles` row the website uses.
//  - levels, regions: website-reference/src/lib/academic-levels.ts and profile.ts
//  - universities: app/src/main/assets/data/universities.json (the 45 names of the app's own
//    university guide, same order)
// ---------------------------------------------------------------------------
val EDUCATION_LEVEL_OPTIONS = listOf(
    "Grade 9", "Grade 10", "Grade 11", "Grade 12",
    "Remedial", "Freshman", "3rd Year (ECE)", "Other"
)

/** Levels whose students are at a university: they pick the university from the list. Everyone else types a school name. */
val UNIVERSITY_LEVELS = setOf("Remedial", "Freshman", "3rd Year (ECE)")

val UNIVERSITY_OPTIONS = listOf(
    "Addis Ababa University",
    "Addis Ababa Science and Technology University",
    "Adama Science and Technology University",
    "Jimma University",
    "Mekelle University",
    "Haramaya University",
    "Bahir Dar University",
    "University of Gondar",
    "Hawassa University",
    "Arba Minch University",
    "Aksum University",
    "Ambo University",
    "Debre Berhan University",
    "Debre Markos University",
    "Dilla University",
    "Dire Dawa University",
    "Wollo University",
    "Wolaita Sodo University",
    "Jigjiga University",
    "Madda Walabu University",
    "Mizan-Tepi University",
    "Samara University",
    "Kotebe University of Education",
    "Adigrat University",
    "Assosa University",
    "Bule Hora University",
    "Debre Tabor University",
    "Mattu University",
    "Wachemo University",
    "Wolkite University",
    "Woldia University",
    "Arsi University",
    "Gambella University",
    "Oda Bultum University",
    "Salale University",
    "Dembi Dolo University",
    "Debark University",
    "Bonga University",
    "Werabe University",
    "Jinka University",
    "Kebri Dehar University",
    "Injibara University",
    "Raya University",
    "Mekdela Amba University",
    "Wollega University"
)

val ETHIOPIAN_REGION_OPTIONS = listOf(
    "Addis Ababa", "Oromia", "Amhara", "Tigray", "Sidama",
    "Central Ethiopia", "South Ethiopia", "South West Ethiopia",
    "Dire Dawa", "Harari", "Somali", "Afar", "Benishangul-Gumuz", "Gambela"
)

// Gold tones for the completed gauge (metal look: light, mid, deep)
private val GoldLight = Color(0xFFFFF1B8)
private val GoldMid = Color(0xFFF2C14E)
private val GoldDeep = Color(0xFFC48A14)

// Text colour on a filled (selected) pill
// D5: day mode. Night keeps the old value, day uses the Ivory & Navy tokens (docs/DAY_MODE_PLAN.md).
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day
private val PillOnAccent: Color get() = dayNight(Color(0xFF06101F), WisdomDarkOnCyan)

/**
 * Native Account screen.
 * - Signed-out: native sign-in / create-account screen (ui/auth/NativeSignInScreen.kt), a copy of
 *   the website login page that talks to Supabase directly (S2d). On success the session is stored
 *   by AuthStateManager.applyNativeSession and this screen switches to the signed-in view.
 * - Signed-in (T7 2026-10-13; simplified 2026-10-16): ONLY the profile completion gauge at the top,
 *   then three sections: name; academic level + university (university students pick from a list)
 *   or school name (typed) + region; phone + target exam. Then Save, and Log Out as the LAST item.
 *   The avatar picker and the stream / academic track picker were removed (owner, 2026-10-16).
 * - Competition points (owner decision B, session O): a small card above the profile panel with the
 *   cumulative competition points total from the server (ui/competition/CompetitionPointsCard.kt).
 *
 * Saving never touches `stream` or `avatarPreset`: whatever is already stored stays as it is.
 * Not ported: the website's Admin button (shown only for admin e-mails).
 *
 * S3 part 2 (2026-10-17, docs/SESSION_2026-10-17_S3_PART2_ACCOUNT.md): TalkBack. The three step headers and the
 * university field say "Expanded" / "Collapsed"; the pills and university rows are radio buttons that say
 * "selected"; Save and Log Out are buttons; pills and rows have a 48 dp touch target. Looks unchanged.
 */
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean = false,
    userName: String? = null,
    userEmail: String? = null,
    userProfile: UserProfile? = null,
    onSignOut: () -> Unit = {},
    onNavigateToUrl: (String) -> Unit
) {
    val context = LocalContext.current

    if (!isLoggedIn) {
        NativeSignInScreen(
            modifier = modifier,
            onSuccess = { result -> AuthStateManager.applyNativeSession(result) },
            onForgotPassword = { onNavigateToUrl("/forgot-password") },
            onOpenTerms = { onNavigateToUrl("/terms") },
            onOpenPrivacy = { onNavigateToUrl("/privacy") }
        )
        return
    }

    // Signed-in state. When no saved profile exists yet, start empty (the website does the same).
    var currentProfile by remember(userProfile) {
        mutableStateOf(
            userProfile ?: UserProfile(
                id = "student",
                email = userEmail,
                fullName = userName,
                firstName = userName?.substringBefore(" "),
                lastName = userName?.substringAfter(" ", "")
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "competition_points") {
                CompetitionPointsCard()
            }

            // ADMIN-entry: shown only for admins (database check); above the profile panel so Log Out stays last
            item(key = "admin_entry") {
                AdminEntryButton(userKey = userEmail, onOpen = { onNavigateToUrl("/admin") })
            }

            item(key = "my_profile_panel") {
                MyProfilePanel(
                    profile = currentProfile,
                    onProfileUpdated = { updated ->
                        currentProfile = updated
                        AuthStateManager.updateProfile(updated)
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    onSignOut = onSignOut
                )
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------------

/** iOS-like press feedback: a quick, slightly bouncy shrink while the finger is down. */
@Composable
private fun pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.96f): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press_scale"
    )
    return scale
}

// ---------------------------------------------------------------------------
// My Profile: panel (gauge + sections + save + log out)
// ---------------------------------------------------------------------------

/**
 * Same fields, defaults and option lists as the website ProfileCompletionPanel.tsx, minus avatar and stream.
 * The completion score is computed live from the form state. Without the stream (it was worth 15) the
 * institution is worth 30, so the total is still 100.
 */
@Composable
private fun MyProfilePanel(
    profile: UserProfile,
    onProfileUpdated: (UserProfile) -> Unit,
    onSignOut: () -> Unit
) {
    // Website defaults: first/last name from full name; level "Freshman"; region empty
    var firstName by remember(profile.firstName, profile.fullName) {
        mutableStateOf(
            profile.firstName?.takeIf { it.isNotBlank() }
                ?: profile.fullName?.trim()?.substringBefore(" ").orEmpty()
        )
    }
    var lastName by remember(profile.lastName, profile.fullName) {
        mutableStateOf(
            profile.lastName?.takeIf { it.isNotBlank() }
                ?: profile.fullName?.trim()?.substringAfter(" ", "").orEmpty()
        )
    }
    var educationLevel by remember(profile.educationLevel) {
        mutableStateOf(profile.educationLevel?.takeIf { it.isNotBlank() } ?: "Freshman")
    }
    var schoolName by remember(profile.schoolName) { mutableStateOf(profile.schoolName ?: "") }
    var townRegion by remember(profile.townRegion) { mutableStateOf(profile.townRegion ?: "") }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone ?: "") }
    var targetExam by remember(profile.targetExam) { mutableStateOf(profile.targetExam ?: "") }
    // Website stores the free-text level under "Other" in bio as "Custom Level: ..."
    var customLevel by remember(profile.bio) {
        mutableStateOf(
            profile.bio?.takeIf { it.startsWith("Custom Level: ") }?.removePrefix("Custom Level: ") ?: ""
        )
    }

    var step1Expanded by remember { mutableStateOf(false) }
    var step2Expanded by remember { mutableStateOf(false) }
    var step3Expanded by remember { mutableStateOf(false) }

    val hasName = firstName.isNotBlank() || !profile.fullName.isNullOrBlank()
    val isUniversityStudent = educationLevel in UNIVERSITY_LEVELS
    val completionScore = run {
        var score = 0
        if (hasName) score += 20
        if (educationLevel.isNotBlank()) score += 20
        if (schoolName.isNotBlank()) score += 30
        if (townRegion.isNotBlank()) score += 10
        if (targetExam.isNotBlank()) score += 10
        if (phone.isNotBlank()) score += 10
        minOf(100, score)
    }

    val composedName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileCompletionGauge(score = completionScore)

        // STEP 1: Legal Identity
        StepAccordionCard(
            stepNumber = 1,
            title = "Legal Identity",
            subtitle = "Your first and last name",
            isFilled = hasName,
            unfilledLabel = "Required",
            isExpanded = step1Expanded,
            onToggle = { step1Expanded = !step1Expanded }
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Abebe", fontSize = 11.sp, color = WisdomMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Bikila", fontSize = 11.sp, color = WisdomMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )
                }
            }
        }

        // STEP 2: Academic level and institution
        StepAccordionCard(
            stepNumber = 2,
            title = "Academic Level & Institution",
            subtitle = "Academic level, university or school, and region",
            isFilled = educationLevel.isNotBlank() && schoolName.isNotBlank() && townRegion.isNotBlank(),
            unfilledLabel = "Incomplete",
            isExpanded = step2Expanded,
            onToggle = { step2Expanded = !step2Expanded }
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FieldLabel("EDUCATION LEVEL")
                PillSelector(
                    options = EDUCATION_LEVEL_OPTIONS,
                    selected = educationLevel,
                    accent = WisdomCyan,
                    onSelect = { newLevel ->
                        // Switching between university and school clears the institution: a university
                        // name makes no sense for a Grade 10 student and the other way round.
                        if ((educationLevel in UNIVERSITY_LEVELS) != (newLevel in UNIVERSITY_LEVELS)) {
                            schoolName = ""
                        }
                        educationLevel = newLevel
                    }
                )

                if (educationLevel == "Other") {
                    OutlinedTextField(
                        value = customLevel,
                        onValueChange = { customLevel = it },
                        label = { Text("Specify Your Academic Level", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Master's Degree, College Diploma, Self-learner...", fontSize = 11.sp, color = WisdomMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )
                }

                if (isUniversityStudent) {
                    FieldLabel("UNIVERSITY")
                    UniversityPicker(
                        selected = schoolName,
                        onSelect = { schoolName = it }
                    )
                } else {
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("School Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Millenium High School", fontSize = 11.sp, color = WisdomMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )
                }

                FieldLabel("TOWN / REGION IN ETHIOPIA")
                PillSelector(
                    options = ETHIOPIAN_REGION_OPTIONS,
                    selected = townRegion,
                    accent = WisdomAccentEmerald,
                    onSelect = { townRegion = it }
                )
            }
        }

        // STEP 3: Contact Phone & Target Exam
        StepAccordionCard(
            stepNumber = 3,
            title = "Contact Phone & Target Exam",
            subtitle = "Phone number, target national exam, and score goal",
            isFilled = phone.isNotBlank() && targetExam.isNotBlank(),
            unfilledLabel = "Incomplete",
            isExpanded = step3Expanded,
            onToggle = { step3Expanded = !step3Expanded }
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. 0911223344", fontSize = 11.sp, color = WisdomMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )

                OutlinedTextField(
                    value = targetExam,
                    onValueChange = { targetExam = it },
                    label = { Text("Target Exam / Goal", fontSize = 11.sp) },
                    placeholder = { Text("e.g. National Matric, Exit Exam, GPA 3.8+", fontSize = 11.sp, color = WisdomMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
            }
        }

        SaveProfileButton(
            onClick = {
                // Website: bio is only rewritten while the level is "Other"
                val newBio = if (educationLevel == "Other") {
                    if (customLevel.isNotBlank()) "Custom Level: ${customLevel.trim()}" else ""
                } else {
                    profile.bio
                }
                // stream and avatarPreset are NOT set here: whatever is stored stays untouched
                val updated = profile.copy(
                    fullName = if (composedName.isNotBlank()) composedName else profile.fullName,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    educationLevel = educationLevel,
                    schoolName = schoolName.trim(),
                    townRegion = townRegion,
                    phone = phone.trim(),
                    targetExam = targetExam.trim(),
                    bio = newBio
                )
                onProfileUpdated(updated)
            }
        )

        // Log Out is the LAST item of the screen (owner request 2026-10-13)
        Spacer(modifier = Modifier.height(6.dp))
        LogOutButton(onClick = onSignOut)
    }
}

// ---------------------------------------------------------------------------
// Completion gauge: the only thing at the top (owner request 2026-10-16:
// no emoji, no big card, no clutter)
// ---------------------------------------------------------------------------

/**
 * Circular completion gauge drawn with Canvas (same method as the timer ring, per BUILD_PITFALLS).
 * The real percent sits in the middle. Cyan while incomplete; at 100% the ring turns gold with a soft glow.
 */
@Composable
private fun ProfileCompletionGauge(score: Int) {
    val complete = score >= 100
    val progress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "my_profile_gauge"
    )
    val shownScore = (progress * 100f).roundToInt()
    val accent = if (complete) dayNight(GoldMid, Color(0xFF5C3F0A)) else WisdomCyan

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)

                if (complete) {
                    // soft golden glow behind the ring
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GoldMid.copy(alpha = 0.30f), Color.Transparent),
                            center = center,
                            radius = size.minDimension / 2f
                        ),
                        radius = size.minDimension / 2f,
                        center = center
                    )
                }

                // track
                drawArc(
                    color = WisdomInk.copy(alpha = 0.08f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )

                if (progress > 0f) {
                    if (complete) {
                        rotate(-90f) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(GoldLight, GoldMid, GoldDeep, GoldMid, GoldLight)),
                                startAngle = 0f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    } else {
                        drawArc(
                            color = WisdomCyan,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$shownScore%",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = accent,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "COMPLETE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WisdomMuted,
                    letterSpacing = 1.6.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// University picker: a field that opens a searchable, scrollable list of the 45 universities
// ---------------------------------------------------------------------------

@Composable
private fun UniversityPicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "university_chevron"
    )
    val shape = RoundedCornerShape(14.dp)
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) UNIVERSITY_OPTIONS else UNIVERSITY_OPTIONS.filter { it.contains(q, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(WisdomInk.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, if (expanded) WisdomCyan else WisdomBorderWhite), shape)
                .semantics(mergeDescendants = true) {
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                }
                .clickable(role = Role.DropdownList, onClick = { expanded = !expanded })
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.ifBlank { "Select your university" },
                fontSize = 13.sp,
                fontWeight = if (selected.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (selected.isBlank()) WisdomMuted else WisdomInk,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = WisdomMuted,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevron }
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search universities", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = shape,
                    colors = fieldColors()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(shape)
                        .background(WisdomInk.copy(alpha = 0.04f))
                        .border(BorderStroke(1.dp, WisdomBorderWhite), shape)
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        filtered.forEach { name ->
                            UniversityRow(
                                name = name,
                                selected = name == selected,
                                onClick = {
                                    onSelect(name)
                                    query = ""
                                    expanded = false
                                }
                            )
                        }
                        if (filtered.isEmpty()) {
                            Text(
                                text = "No university matches your search",
                                fontSize = 12.sp,
                                color = WisdomMuted,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversityRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) WisdomCyan else dayNight(Color(0xFFE2E8F0), WisdomTextPrimary),
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = WisdomCyan, modifier = Modifier.size(14.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// iOS-like pill selectors (wrap onto several lines: nothing hidden to the right)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PillSelector(
    options: List<String>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            PillOption(
                label = option,
                selected = selected == option,
                accent = accent,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun PillOption(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction, 0.93f)
    val background by animateColorAsState(
        targetValue = if (selected) accent else WisdomInk.copy(alpha = 0.06f),
        animationSpec = tween(220),
        label = "pill_background"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) PillOnAccent else dayNight(Color(0xFFE2E8F0), WisdomTextPrimary),
        animationSpec = tween(220),
        label = "pill_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else WisdomBorderWhite,
        animationSpec = tween(220),
        label = "pill_border"
    )
    val glow by animateDpAsState(
        targetValue = if (selected) 10.dp else 0.dp,
        animationSpec = tween(220),
        label = "pill_glow"
    )
    val shape = RoundedCornerShape(50)

    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize() // S3: 48 dp touch target, the pill looks the same
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(glow, shape, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, borderColor), shape)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .heightIn(min = 38.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = selected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = WisdomMuted,
        letterSpacing = 0.8.sp
    )
}

// ---------------------------------------------------------------------------
// Sections, buttons, fields
// ---------------------------------------------------------------------------

@Composable
private fun StepAccordionCard(
    stepNumber: Int,
    title: String,
    subtitle: String,
    isFilled: Boolean,
    unfilledLabel: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val chevron by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(250),
        label = "step_chevron"
    )

    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        borderColor = WisdomBorderWhite
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                    }
                    .clickable(role = Role.Button, onClick = onToggle)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(WisdomCyan.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.4f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "$stepNumber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WisdomCyan)
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                    Text(text = subtitle, fontSize = 12.sp, color = WisdomMuted)
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) WisdomAccentEmerald.copy(alpha = 0.15f) else WisdomAccentAmber.copy(alpha = 0.15f))
                            .border(
                                BorderStroke(1.dp, if (isFilled) WisdomAccentEmerald.copy(alpha = 0.4f) else WisdomAccentAmber.copy(alpha = 0.4f)),
                                CircleShape
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isFilled) "Filled" else unfilledLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFilled) WisdomAccentEmerald else WisdomAccentAmber
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = WisdomMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevron }
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WisdomInk.copy(alpha = 0.02f))
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SaveProfileButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction, 0.97f)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (AppThemeMode.isDark) Brush.horizontalGradient(listOf(WisdomCyan, Color(0xFF38BDF8))) else WisdomPrimaryBtnGradient)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = WisdomDarkOnCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Save Profile Changes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WisdomDarkOnCyan)
    }
}

@Composable
private fun LogOutButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction, 0.97f)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(WisdomAccentRose.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, WisdomAccentRose.copy(alpha = 0.45f)), shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = WisdomAccentRose,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Log Out", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WisdomAccentRose)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WisdomCyan,
    unfocusedBorderColor = WisdomBorderWhite,
    focusedContainerColor = WisdomInk.copy(alpha = 0.06f),
    unfocusedContainerColor = WisdomInk.copy(alpha = 0.06f),
    focusedTextColor = WisdomInk,
    unfocusedTextColor = WisdomInk,
    focusedLabelColor = WisdomCyan,
    unfocusedLabelColor = WisdomMuted
)
