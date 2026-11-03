package com.wisdomtower.academy.ui.account

import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wisdomtower.academy.ui.theme.wisdomPressClickable
import com.wisdomtower.academy.ui.theme.wisdomScreenEntrance
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
import com.wisdomtower.academy.data.auth.AccountCallResult
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.auth.ProfileSubjectsSync
import com.wisdomtower.academy.data.content.MySubjectsStore
import com.wisdomtower.academy.data.model.UserProfile
import com.wisdomtower.academy.ui.auth.NativeSignInScreen
import com.wisdomtower.academy.ui.settings.MySubjectsEditor
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomPrimaryBtnGradient
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch
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

/** Daily study target choices. The database accepts 5 to 600 minutes (profiles_daily_goal_valid). */
private val DAILY_GOAL_OPTIONS = listOf(15, 30, 45, 60, 90, 120)

/** Preferred study window ids must stay exactly these four (profiles_preferred_study_time_valid). */
private val STUDY_WINDOWS = listOf(
    Triple("morning", "Early Morning", "5:00 AM to 8:00 AM"),
    Triple("afternoon", "Afternoon", "1:00 PM to 4:00 PM"),
    Triple("evening", "Evening", "6:00 PM to 9:00 PM"),
    Triple("night", "Late Night", "10:00 PM to 1:00 AM")
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
 * Native PROFILE screen (the bottom tab was called "Account").
 * - Signed-out: native sign-in / create-account screen (ui/auth/NativeSignInScreen.kt).
 * - Signed-in: completion gauge, then FIVE cards, then ONE Save button, Log Out last:
 *     1. Personal Details            (first name, last name, phone)
 *     2. Academic Level & Institution (level, university or school, region)
 *     3. My Subjects                 (what "Your status" counts)
 *     4. Study Goals & Targets       (daily minutes only)
 *     5. Study Schedule              (preferred study window)
 *
 * 2026-10-24 (owner order): the Competition Points card that used to sit at the top of this screen was removed. It loaded
 * late and pushed the page down. Competition points now live in "Your status" (ui/study/StatusCompetitionPanel.kt).
 *
 * 2026-10-19 (owner order): Study Goals and My Subjects were moved here from Settings, and the duplicate
 * "Target Exam" field (it wrote the same `target_exam` column as the Settings field) is now ONE field.
 * Save uses [AuthStateManager.saveProfile], so "Saved" is shown only when the server really returned the row
 * (before, this screen showed "Saved" even when the server write failed). Subjects are written with
 * [ProfileSubjectsSync] (`my_subject_keys` + `study_subjects`).
 *
 * 2026-10-23 (owner order): the "Target Exam / Milestone" and "Target Score / GPA" boxes were REMOVED from
 * this screen. Save no longer sends them, so whatever is already stored in `target_exam` / `target_score`
 * stays untouched. The 10 completion points that "Target Exam" used to give now come from My Subjects
 * (at least one subject chosen), so the gauge can still reach 100%.
 *
 * Saving never touches `stream` or `avatarPreset`: whatever is already stored stays as it is.
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
    val currentProfile = remember(userProfile) {
        userProfile ?: UserProfile(
            id = "student",
            email = userEmail,
            fullName = userName,
            firstName = userName?.substringBefore(" "),
            lastName = userName?.substringAfter(" ", "")
        )
    }

    Box(modifier = modifier.fillMaxSize().wisdomScreenEntrance()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ADMIN-entry: shown only for admins (database check); above the profile panel so Log Out stays last
            item(key = "admin_entry") {
                AdminEntryButton(userKey = userEmail, onOpen = { onNavigateToUrl("/admin") })
            }

            item(key = "my_profile_panel") {
                MyProfilePanel(profile = currentProfile, onSignOut = onSignOut)
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
// My Profile: panel (gauge + five cards + save + log out)
// ---------------------------------------------------------------------------

@Composable
private fun MyProfilePanel(
    profile: UserProfile,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authUser by AuthStateManager.currentUser.collectAsState()
    val userId = authUser?.id ?: profile.id

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
    var phone by remember(profile.phone) { mutableStateOf(profile.phone ?: "") }
    var educationLevel by remember(profile.educationLevel) {
        mutableStateOf(profile.educationLevel?.takeIf { it.isNotBlank() } ?: "Freshman")
    }
    var schoolName by remember(profile.schoolName) { mutableStateOf(profile.schoolName ?: "") }
    var townRegion by remember(profile.townRegion) { mutableStateOf(profile.townRegion ?: "") }
    // Website stores the free-text level under "Other" in bio as "Custom Level: ..."
    var customLevel by remember(profile.bio) {
        mutableStateOf(
            profile.bio?.takeIf { it.startsWith("Custom Level: ") }?.removePrefix("Custom Level: ") ?: ""
        )
    }

    // Study goals and schedule (were in Settings). Target exam / target score boxes were removed 2026-10-23.
    var dailyGoal by remember(profile.dailyStudyGoalMinutes) { mutableStateOf(profile.dailyStudyGoalMinutes) }
    var studyWindow by remember(profile.preferredStudyTime) {
        mutableStateOf(profile.preferredStudyTime?.takeIf { it.isNotBlank() } ?: "evening")
    }

    // My Subjects (was in Settings). Saved on this phone at once; written to the server with the Save button.
    val initialPicks = remember(userId, profile.educationLevel) { MySubjectsStore.load(context, userId, profile.educationLevel) }
    var picks by remember(userId, profile.educationLevel) { mutableStateOf(initialPicks.keys) }
    var picksFromLevel by remember(userId, profile.educationLevel) { mutableStateOf(initialPicks.fromAcademicLevel) }
    var subjectsTouched by remember(userId) { mutableStateOf(false) }

    // A new phone: take the picks the student saved on the server (unless they already changed them here).
    LaunchedEffect(userId) {
        val remote = ProfileSubjectsSync.load()
        if (remote != null && !subjectsTouched) {
            MySubjectsStore.save(context, userId, remote)
            picks = remote
            picksFromLevel = false
        }
    }

    var saving by remember { mutableStateOf(false) }
    var openCard by remember { mutableStateOf(0) } // 0 = all closed, otherwise the card number that is open

    val hasName = firstName.isNotBlank() || !profile.fullName.isNullOrBlank()
    val isUniversityStudent = educationLevel in UNIVERSITY_LEVELS
    val completionScore = run {
        var score = 0
        if (hasName) score += 20
        if (educationLevel.isNotBlank()) score += 20
        if (schoolName.isNotBlank()) score += 30
        if (townRegion.isNotBlank()) score += 10
        if (picks.isNotEmpty()) score += 10
        if (phone.isNotBlank()) score += 10
        minOf(100, score)
    }

    val composedName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileCompletionGauge(score = completionScore)

        // CARD 1: Personal details
        StepAccordionCard(
            stepNumber = 1,
            title = "Personal Details",
            subtitle = "Your name and phone number",
            isFilled = hasName,
            unfilledLabel = "Required",
            isExpanded = openCard == 1,
            onToggle = { openCard = if (openCard == 1) 0 else 1 }
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
            }
        }

        // CARD 2: Academic level and institution
        StepAccordionCard(
            stepNumber = 2,
            title = "Academic Level & Institution",
            subtitle = "Academic level, university or school, and region",
            isFilled = educationLevel.isNotBlank() && schoolName.isNotBlank() && townRegion.isNotBlank(),
            unfilledLabel = "Incomplete",
            isExpanded = openCard == 2,
            onToggle = { openCard = if (openCard == 2) 0 else 2 }
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

        // CARD 3: My Subjects
        StepAccordionCard(
            stepNumber = 3,
            title = "My Subjects",
            subtitle = "Choose what you study. Your status counts only these.",
            isFilled = picks.isNotEmpty(),
            unfilledLabel = "Not chosen",
            isExpanded = openCard == 3,
            onToggle = { openCard = if (openCard == 3) 0 else 3 }
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                MySubjectsEditor(
                    picks = picks,
                    fromAcademicLevel = picksFromLevel,
                    onPicksChange = { newPicks ->
                        picks = newPicks
                        picksFromLevel = false
                        subjectsTouched = true
                        MySubjectsStore.save(context, userId, newPicks)
                    }
                )
            }
        }

        // CARD 4: Study goals (daily minutes only; target exam and target score boxes removed 2026-10-23)
        StepAccordionCard(
            stepNumber = 4,
            title = "Study Goals & Targets",
            subtitle = "Daily target ($dailyGoal mins)",
            isFilled = dailyGoal > 0,
            unfilledLabel = "Not set",
            isExpanded = openCard == 4,
            onToggle = { openCard = if (openCard == 4) 0 else 4 }
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FieldLabel("DAILY STUDY TARGET (MINUTES PER DAY)")
                Text(
                    text = "Select your target study commitment. This powers your streak calculation and pacing HUD.",
                    fontSize = 11.sp,
                    color = WisdomMuted,
                    lineHeight = 15.sp
                )
                PillSelector(
                    options = DAILY_GOAL_OPTIONS.map { "$it min/day" },
                    selected = "$dailyGoal min/day",
                    accent = WisdomCyan,
                    onSelect = { label -> dailyGoal = label.substringBefore(" ").toIntOrNull() ?: dailyGoal }
                )
            }
        }

        // CARD 5: Study schedule
        StepAccordionCard(
            stepNumber = 5,
            title = "Study Schedule",
            subtitle = "Your preferred daily study window",
            isFilled = studyWindow.isNotBlank(),
            unfilledLabel = "Not set",
            isExpanded = openCard == 5,
            onToggle = { openCard = if (openCard == 5) 0 else 5 }
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel("PREFERRED DAILY STUDY WINDOW")
                STUDY_WINDOWS.chunked(2).forEach { pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { (id, label, hours) ->
                            StudyWindowOption(
                                label = label,
                                hours = hours,
                                selected = studyWindow == id,
                                modifier = Modifier.weight(1f),
                                onClick = { studyWindow = id }
                            )
                        }
                    }
                }
            }
        }

        SaveProfileButton(
            saving = saving,
            onClick = {
                if (saving) return@SaveProfileButton
                saving = true
                // Website: bio is only rewritten while the level is "Other"
                val newBio = if (educationLevel == "Other") {
                    if (customLevel.isNotBlank()) "Custom Level: ${customLevel.trim()}" else ""
                } else {
                    profile.bio
                }
                // stream, avatarPreset, targetExam and targetScore are NOT set here: whatever is stored stays untouched
                val updated = profile.copy(
                    fullName = if (composedName.isNotBlank()) composedName else profile.fullName,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    phone = phone.trim(),
                    educationLevel = educationLevel,
                    schoolName = schoolName.trim(),
                    townRegion = townRegion,
                    bio = newBio,
                    dailyStudyGoalMinutes = dailyGoal,
                    preferredStudyTime = studyWindow
                )
                // Same keys the old Settings card wrote, so anything that still reads them keeps working.
                context.getSharedPreferences("wt_settings_prefs", Context.MODE_PRIVATE).edit()
                    .putInt("daily_study_goal_minutes", dailyGoal)
                    .putString("preferred_study_time", studyWindow)
                    .apply()

                val sendSubjects = subjectsTouched || !picksFromLevel
                val keysToSend = picks
                scope.launch {
                    var result: AccountCallResult = AuthStateManager.saveProfile(updated)
                    if (result is AccountCallResult.Success && sendSubjects) {
                        val labels = MySubjectsStore.packages
                            .filter { pkg -> pkg.subjects.any { it.key in keysToSend } }
                            .map { it.label }
                        val subjectsResult = ProfileSubjectsSync.save(keysToSend, labels)
                        if (subjectsResult is AccountCallResult.Failure) result = subjectsResult
                    }
                    saving = false
                    val text = when (val done = result) {
                        is AccountCallResult.Success -> "Saved"
                        is AccountCallResult.Failure -> done.message
                    }
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                }
            }
        )

        // Log Out is the LAST item of the screen (owner request 2026-10-13)
        Spacer(modifier = Modifier.height(6.dp))
        LogOutButton(onClick = onSignOut)
    }
}

// ---------------------------------------------------------------------------
// Completion gauge (no emoji, no big card, no clutter)
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
                .wisdomPressClickable(role = Role.DropdownList, onClick = { expanded = !expanded })
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
            .wisdomPressClickable(role = Role.RadioButton, onClick = onClick)
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
            .minimumInteractiveComponentSize() // 48 dp touch target, the pill looks the same
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

/** One preferred-study-window choice: name on top, hours below (same look the Settings card had). */
@Composable
private fun StudyWindowOption(
    label: String,
    hours: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) WisdomCyan.copy(alpha = 0.16f) else WisdomInk.copy(alpha = 0.04f))
            .border(BorderStroke(1.dp, if (selected) WisdomCyan else WisdomBorderWhite), shape)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) WisdomCyan else WisdomInk
        )
        Text(text = hours, fontSize = 12.sp, color = WisdomMuted)
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
private fun SaveProfileButton(saving: Boolean, onClick: () -> Unit) {
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
        Text(
            text = if (saving) "Saving..." else "Save Profile Changes",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = WisdomDarkOnCyan
        )
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
