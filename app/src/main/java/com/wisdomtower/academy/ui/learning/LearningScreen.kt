package com.wisdomtower.academy.ui.learning

import com.wisdomtower.academy.ui.theme.WisdomInk
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wisdomtower.academy.data.competition.ExamLockdown
import com.wisdomtower.academy.data.model.UserProfile
import com.wisdomtower.academy.ui.auth.SignInPill
import com.wisdomtower.academy.ui.packages.CATALOG_PACKAGES
import com.wisdomtower.academy.ui.packages.NativePackage
import com.wisdomtower.academy.ui.packages.OpenCard
import com.wisdomtower.academy.ui.packages.openGrid
import com.wisdomtower.academy.ui.study.NativeCalculatorScreen
import com.wisdomtower.academy.ui.study.NativeCoverPageScreen
import com.wisdomtower.academy.ui.study.NativeNotebookScreen
import com.wisdomtower.academy.ui.study.NativeStatusScreen
import com.wisdomtower.academy.ui.study.NativeTargetsScreen
import com.wisdomtower.academy.ui.study.NativeTimerScreen
import com.wisdomtower.academy.ui.study.NativePlannerScreen
import com.wisdomtower.academy.ui.study.NativeToolRequests
import com.wisdomtower.academy.ui.study.NativeTutorScreen
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDark
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomPress
import com.wisdomtower.academy.ui.theme.wisdomPressClickable
import com.wisdomtower.academy.ui.theme.wisdomReveal

/**
 * Native Learning screen mirroring website reference:
 * - src/app/learning/page.tsx
 * - src/app/learning/LearningContent.tsx
 *
 * GLASS (2026-10-13, owner request, docs/GLASS_PLAN.md area 2): the guest banner, the Study Tools
 * card (and its tiles) and the empty-curriculum card use the see-through glass look of the Targets screen.
 *
 * M11 slice 6 (docs/SESSION_2026-10-16_UI_AF.md): the guest banner, Study Tools card, Curriculum header and
 * empty-curriculum card rise in once per app session (wisdomReveal); the curriculum cards already do through openGrid.
 *
 * COVER PAGE (owner order 2026-10-16, docs/SESSION_2026-10-16_COVER_A.md): the name / student ID / email card above
 * the Study Tools dock was REMOVED (the account details live in the Account tab), and the "Courses" tile of the dock
 * was replaced by "Cover Page", which opens the native Cover Page Generator ([NativeCoverPageScreen]) in place of
 * this screen; Back returns here. "All Courses" in the Curriculum header still opens the packages tab.
 * Status: written, not compiled, not seen on a device.
 *
 * C10 (session C10-C, owner decision 2026-10-15: "Ai tutor is prohibited, and calculator is allowed always"):
 * while a RANKED competition exam is open (ExamLockdown.active) the native AI Tutor does not open from the tile or
 * from a request made elsewhere (drawer row, website links go through NativeToolRequests.requestTutor), a short
 * notice is shown instead, and a tutor that was already open is closed. The calculator is never blocked.
 * Status: written, not compiled, not seen on a device. The website pop-up path (onOpenTool with tool=tutor) is a
 * fallback that no tile uses any more; MainActivity.kt routes every tutor link here through requestTutor.
 *
 * S3 part 2 (2026-10-17, docs/SESSION_2026-10-17_S3_PART2_LEARNING.md): every plain clickable here (guest Sign In /
 * Create Account, "All Courses", "Browse Courses", the 8 tool tiles) now has role Button for TalkBack, and the small
 * ones have a 48 dp touch target. Looks unchanged. Icons next to a text label stay decorative.
 *
 * V7 (docs/SESSION_2026-11-03_V14_LEARNING_PRESS.md): Create Account, All Courses, Browse Courses and the 8 tool tiles
 * now press in (shared wisdomPress / wisdomPressClickable) with the light tick. Actions and looks unchanged.
 *
 * Guest Scholar Banner if signed out.
 *
 * Study Tools Dock (8 tiles):
 * - Timer, Planner, Targets, Notebook, Calculator, AI Tutor, Your status, Cover Page
 * - All tiles open NATIVE screens inside this tab: Timer ([NativeTimerScreen]), Planner ([NativePlannerScreen]),
 *   Targets ([NativeTargetsScreen], P11 step), Notebook ([NativeNotebookScreen], P11 step), Calculator, AI Tutor
 *   ([NativeTutorScreen]), Your status ([NativeStatusScreen], F7 part 5, real data only) and Cover Page
 *   ([NativeCoverPageScreen]).
 *   The website pop-up path (`onOpenTool`) is now only a fallback for tiles without a native screen (none at the moment).
 *   NOT yet done for "Your status": the entry points outside this screen (the side-menu row and website links with
 *   `tool=analytics`) still open the website pop-up; they need a patch of `MainActivity.kt` like P11.
 *
 * Curriculum Section:
 * - Header: Curriculum with "All Courses (X)", X = number of courses shown
 * - Courses chosen by the website's `getDefaultPackagesForAcademicLevel(level)` rule from `src/lib/academic-levels.ts`
 *   (the website also lets a student change this list; that choice lives in WebView localStorage and is not read here)
 * - Cards = shared OpenCard (picture, title, Open) in two columns; a tap opens the package page (subject list)
 */

data class StudyToolTile(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val url: String
)

val STUDY_TOOLS_TILES = listOf(
    StudyToolTile("timer", "Timer", Icons.Default.Timer, "/learning?tool=timer"),
    StudyToolTile("planner", "Planner", Icons.Default.CalendarMonth, "/learning?tool=planner"),
    StudyToolTile("goals", "Targets", Icons.Default.CheckCircle, "/learning?tool=goals"),
    StudyToolTile("notes", "Notebook", Icons.Default.Folder, "/learning?tool=notes"),
    StudyToolTile("calculator", "Calculator", Icons.Default.Calculate, "/learning?tool=calculator"),
    StudyToolTile("tutor", "AI Tutor", Icons.Default.AutoAwesome, "/learning?tool=tutor"),
    StudyToolTile("analytics", "Your status", Icons.AutoMirrored.Filled.TrendingUp, "/learning?tool=analytics"),
    StudyToolTile("cover", "Cover Page", Icons.Default.Description, "/learning?tool=cover")
)

/**
 * Same rule as `getDefaultPackagesForAcademicLevel` in website-reference/src/lib/academic-levels.ts.
 * The website id "ece" is the single 3rd Year (ECE) card; natively it is the first package whose id starts with "ece".
 */
private fun defaultPackageIdsForLevel(level: String?): List<String> {
    if (level.isNullOrBlank()) return listOf("freshman", "coc")
    val t = level.trim().lowercase()
    return when {
        t == "grade 9" || t == "grade-9" || t == "g9" -> listOf("grade-9")
        t == "grade 10" || t == "grade-10" || t == "g10" -> listOf("grade-10")
        t == "grade 11" || t == "grade-11" || t == "g11" -> listOf("grade-11")
        t == "grade 12" || t == "grade-12" || t == "g12" -> listOf("grade-12")
        t.contains("remedial") -> listOf("remedial")
        t.contains("freshman") -> listOf("freshman", "coc")
        t == "3rd year (ece)" || t == "3rd year ece" || t.contains("ece") || t.contains("electrical") -> listOf("ece")
        else -> emptyList()
    }
}

@Composable
fun LearningScreen(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean = false,
    userName: String? = null,
    userEmail: String? = null,
    userProfile: UserProfile? = null,
    packageList: List<NativePackage> = CATALOG_PACKAGES,
    onOpenTool: (String) -> Unit = {},
    onOpenHub: (String) -> Unit = {},
    onSelectCourse: (NativePackage) -> Unit = {},
    onNavigateToUrl: (String) -> Unit = {}
) {
    val toolContext = LocalContext.current
    // Native AI tutor (N10): the "AI Tutor" tile shows it in place of this screen; Back returns here.
    var tutorOpen by remember { mutableStateOf(false) }
    // Native calculator (P8): the "Calculator" tile and requests from outside show it in place of this screen
    var calculatorOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = calculatorOpen) { calculatorOpen = false }
    LaunchedEffect(NativeToolRequests.pendingCalculator) {
        if (NativeToolRequests.pendingCalculator) {
            NativeToolRequests.pendingCalculator = false
            calculatorOpen = true
        }
    }
    if (calculatorOpen) {
        NativeCalculatorScreen(onBack = { calculatorOpen = false }, modifier = modifier)
        return
    }
    // Native focus timer (P9): the "Timer" tile and requests from outside show it in place of this screen
    var timerOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = timerOpen) { timerOpen = false }
    LaunchedEffect(NativeToolRequests.pendingTimer) {
        if (NativeToolRequests.pendingTimer) {
            NativeToolRequests.pendingTimer = false
            timerOpen = true
        }
    }
    if (timerOpen) {
        NativeTimerScreen(onBack = { timerOpen = false }, modifier = modifier)
        return
    }
    // Native study planner (P10): the "Planner" tile and requests from outside show it in place of this screen
    var plannerOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = plannerOpen) { plannerOpen = false }
    LaunchedEffect(NativeToolRequests.pendingPlanner) {
        if (NativeToolRequests.pendingPlanner) {
            NativeToolRequests.pendingPlanner = false
            plannerOpen = true
        }
    }
    if (plannerOpen) {
        NativePlannerScreen(onBack = { plannerOpen = false }, modifier = modifier)
        return
    }
    // Native notebook (F7 part 4): the "Notebook" tile and requests from outside (patch P11) show it in place of this screen
    var notebookOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = notebookOpen) { notebookOpen = false }
    LaunchedEffect(NativeToolRequests.pendingNotebook) {
        if (NativeToolRequests.pendingNotebook) {
            NativeToolRequests.pendingNotebook = false
            notebookOpen = true
        }
    }
    if (notebookOpen) {
        NativeNotebookScreen(onBack = { notebookOpen = false }, modifier = modifier)
        return
    }
    // Native targets (F7 part 4): the "Targets" tile and requests from outside (patch P11) show it in place of this screen
    var targetsOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = targetsOpen) { targetsOpen = false }
    LaunchedEffect(NativeToolRequests.pendingTargets) {
        if (NativeToolRequests.pendingTargets) {
            NativeToolRequests.pendingTargets = false
            targetsOpen = true
        }
    }
    if (targetsOpen) {
        NativeTargetsScreen(onBack = { targetsOpen = false }, modifier = modifier)
        return
    }
    // Native "Your status" (F7 part 5): the "Your status" tile shows it in place of this screen; Back returns here.
    // Requests from outside (side menu, website links) are not wired yet.
    var statusOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = statusOpen) { statusOpen = false }
    LaunchedEffect(NativeToolRequests.pendingStatus) {
        if (NativeToolRequests.pendingStatus) {
            NativeToolRequests.pendingStatus = false
            statusOpen = true
        }
    }
    if (statusOpen) {
        NativeStatusScreen(
            onBack = { statusOpen = false },
            isLoggedIn = isLoggedIn,
            studentName = userProfile?.fullName ?: userName,
            educationLevel = userProfile?.educationLevel,
            stream = userProfile?.stream,
            dailyGoalMinutes = userProfile?.dailyStudyGoalMinutes?.takeIf { it > 0 } ?: 45,
            modifier = modifier
        )
        return
    }
    // Cover Page Generator (owner order 2026-10-16): the "Cover Page" tile shows it in place of this screen; Back returns here.
    var coverOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = coverOpen) { coverOpen = false }
    if (coverOpen) {
        NativeCoverPageScreen(onBack = { coverOpen = false }, modifier = modifier)
        return
    }
    BackHandler(enabled = tutorOpen) { tutorOpen = false }
    // Requests from outside this screen (drawer, website links): open the native tutor as well (P7).
    // C10: not while a RANKED competition exam is open; a notice is shown instead.
    LaunchedEffect(NativeToolRequests.pendingTutor) {
        if (NativeToolRequests.pendingTutor) {
            NativeToolRequests.pendingTutor = false
            if (ExamLockdown.active) {
                showTutorBlockedNotice(toolContext)
            } else {
                tutorOpen = true
            }
        }
    }
    // C10: a ranked exam started while the tutor was open underneath: close it
    LaunchedEffect(ExamLockdown.active) {
        if (ExamLockdown.active) tutorOpen = false
    }
    if (tutorOpen) {
        NativeTutorScreen(onBack = { tutorOpen = false }, modifier = modifier)
        return
    }

    // Courses for the Curriculum section: the website's default list for the student's academic level
    val enrolledCourses = remember(packageList, userProfile?.educationLevel) {
        defaultPackageIdsForLevel(userProfile?.educationLevel).mapNotNull { id ->
            if (id == "ece") {
                packageList.firstOrNull { it.id.startsWith("ece") }
            } else {
                packageList.firstOrNull { it.id == id }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // GUEST SCHOLAR BANNER (if signed out)
            if (!isLoggedIn) {
                item(key = "guest_banner") {
                    WisdomGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wisdomReveal("learning_guest_banner", 1),
                        cornerRadius = 18.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Scholar Access",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WisdomInk
                            )
                            Text(
                                text = "Sign in or create an account to sync your study notes, targets, and course progress.",
                                fontSize = 11.sp,
                                color = WisdomMuted,
                                lineHeight = 15.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SignInPill(label = "Sign In", onClick = { onNavigateToUrl("/login") }) // S5-signin

                                Box(
                                    modifier = Modifier
                                        .wisdomPressClickable(role = Role.Button) { onNavigateToUrl("/signup") }
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(50))
                                        .background(WisdomGlassFillStrong)
                                        .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.2f)), RoundedCornerShape(50))
                                        .padding(horizontal = 20.dp, vertical = 9.dp), // S5-signin: same size as the Sign In pill
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Create Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
                                }
                            }
                        }
                    }
                }
            }

            // STUDY TOOLS DOCK (8 tiles matching website)
            item(key = "study_tools_dock") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal("learning_tools_dock", 2),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "STUDY TOOLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WisdomMuted,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )

                        // 4x2 Grid of phone-like toolbar tiles
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1 (Timer, Planner, Targets, Notebook)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                STUDY_TOOLS_TILES.take(4).forEach { tile ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ToolTileButton(
                                            tile = tile,
                                            highlight = false,
                                            onClick = {
                                                if (tile.key == "timer") {
                                                    timerOpen = true
                                                } else if (tile.key == "planner") {
                                                    plannerOpen = true
                                                } else if (tile.key == "goals") {
                                                    targetsOpen = true
                                                } else if (tile.key == "notes") {
                                                    notebookOpen = true
                                                } else {
                                                    onOpenTool(tile.url)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Row 2 (Calculator, AI Tutor, Your status, Cover Page)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                STUDY_TOOLS_TILES.drop(4).forEach { tile ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ToolTileButton(
                                            tile = tile,
                                            highlight = tile.key == "cover",
                                            onClick = {
                                                if (tile.key == "cover") {
                                                    coverOpen = true
                                                } else if (tile.key == "calculator") {
                                                    calculatorOpen = true
                                                } else if (tile.key == "tutor") {
                                                    if (ExamLockdown.active) {
                                                        showTutorBlockedNotice(toolContext)
                                                    } else {
                                                        tutorOpen = true
                                                    }
                                                } else if (tile.key == "analytics") {
                                                    statusOpen = true
                                                } else {
                                                    onOpenTool(tile.url)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CURRICULUM SECTION
            item(key = "curriculum_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal("learning_curriculum_header", 3)
                        .padding(top = 10.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = WisdomCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Curriculum",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WisdomInk
                        )
                    }

                    Box(
                        modifier = Modifier
                            .wisdomPressClickable(role = Role.Button) { onNavigateToUrl("/packages") }
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(WisdomGlassFill)
                            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "All Courses (${enrolledCourses.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WisdomCyan
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = WisdomCyan,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            // Empty state (same texts as the website when no package is selected)
            if (enrolledCourses.isEmpty()) {
                item(key = "curriculum_empty") {
                    WisdomGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wisdomReveal("learning_curriculum_empty", 4),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No curriculum packages selected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WisdomInk,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "You can browse all courses, choose packages in the course manager, or update your academic level in Settings.",
                                fontSize = 11.sp,
                                color = WisdomMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                            Box(
                                modifier = Modifier
                                    .wisdomPressClickable(role = Role.Button) { onNavigateToUrl("/packages") }
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WisdomInk.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Browse Courses",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WisdomInk
                                )
                            }
                        }
                    }
                }
            }

            // Curriculum cards: the shared OpenCard (picture, title, Open) in two columns.
            // Tapping a card opens the package page with its subject list (never straight to the six hubs,
            // which do not exist at package level for Freshman, Grades, Remedial and ECE and gave a 404).
            openGrid("curriculum", enrolledCourses) { pkg ->
                // UX-CONSISTENCY-2026-10-17: the ECE card stands for the whole year, so it uses the Home picture and title
                val isEceTrack = pkg.id.startsWith("ece")
                OpenCard(
                    if (isEceTrack) com.wisdomtower.academy.ui.packages.ECE_TRACK_TITLE else pkg.name,
                    if (isEceTrack) com.wisdomtower.academy.ui.packages.ECE_TRACK_COVER_ASSET else pkg.assetImage
                ) { onSelectCourse(pkg) }
            }

            item(key = "learning_bottom_spacer") {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// C10: plain notice shown when the AI Tutor is asked for while a RANKED competition exam is open.
// The wording is the agent's (the owner only decided that the tutor is prohibited); change it if the owner wants other words.
private fun showTutorBlockedNotice(context: android.content.Context) {
    Toast.makeText(context, "AI Tutor is switched off while a ranked competition exam is open.", Toast.LENGTH_SHORT).show()
}

@Composable
private fun ToolTileButton(
    tile: StudyToolTile,
    highlight: Boolean,
    onClick: () -> Unit
) {
    // The Cover Page tile (new, owner order 2026-10-16) has a cyan edge and a cyan-tinted icon plate so it stands out.
    val edge = if (highlight) WisdomCyan.copy(alpha = 0.65f) else WisdomGlassBorder
    val plate = if (highlight) WisdomCyan.copy(alpha = 0.18f) else WisdomGlassFillStrong
    // V7: one interaction source feeds both the press-in scale (outermost modifier, so the whole tile squeezes) and the click
    val tileSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wisdomPress(tileSource)
            .clip(RoundedCornerShape(16.dp))
            .background(WisdomGlassFill)
            .border(BorderStroke(1.dp, edge), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = tileSource,
                indication = ripple(color = WisdomCyan.copy(alpha = 0.2f)),
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(plate)
                    .border(BorderStroke(1.dp, edge), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = WisdomCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tile.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = WisdomInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
