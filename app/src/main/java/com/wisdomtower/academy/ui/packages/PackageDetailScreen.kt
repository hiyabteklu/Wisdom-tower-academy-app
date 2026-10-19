package com.wisdomtower.academy.ui.packages

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted

// Electrical & Computer Engineering (website-reference/src/data/special-packages.ts: name "Electrical & Computer
// Engineering", yearLabel "Year 3", semesters "Semester 1" / "Semester 2" with their own cover pictures).
private const val ECE_PACKAGE_NAME = "Electrical & Computer Engineering"
private const val ECE_YEAR_LABEL = "Year 3"

private data class EceSemesterEntry(
    val id: String,
    val label: String,
    val image: String,
    val courses: List<NativeSpecialCourse>
)

private val ECE_SEMESTER_ENTRIES = listOf(
    EceSemesterEntry("sem-1", "Semester 1", "file:///android_asset/images/special-packages/ece-sem-1.jpg", ECE_SEM1_COURSES),
    EceSemesterEntry("sem-2", "Semester 2", "file:///android_asset/images/special-packages/ece-sem-2.jpg", ECE_SEM2_COURSES)
)

/**
 * Package page. Rewritten 2026-10-11 (owner rules): every card is the shared [OpenCard]
 * (picture, title, Open button) in a TWO-column grid. No hero picture with gradient or tag, no
 * descriptions, no "What is included" list, no filter chips, no invented headings, nothing locked.
 *
 * Headings copied from the website pages (website-reference/src/app/academy/):
 *  - freshman/page.tsx: "Freshman courses" + "including preengineering & others", label "Subjects"
 *  - grades/[grade]/page.tsx: "Secondary Curriculum" + grade label ("Grade 9" ...), label "Subjects"
 *  - remedial/page.tsx: "Remedial", label "Subjects"
 *  - special-packages/[slug]/page.tsx (ECE, owner request 2026-10-11): first level = the two semester cover cards
 *    ("Semester 1", "Semester 2"), eyebrow "Department track · Year 3", title "Electrical & Computer Engineering";
 *    special-packages/[slug]/[semester]/page.tsx: second level = the 7 courses of that semester, eyebrow
 *    "Electrical & Computer Engineering · Year 3", title "Semester 1" / "Semester 2". Back goes from the courses to the
 *    two semester cards first, then leaves the package.
 *  - entrance-questions (2026-10-11 website update): root "Past Exams" / "Entrance Questions" with the two stream
 *    cards; stream page "Entrance Questions" / stream label with subject cards; subject page
 *    "Entrance Questions · {stream}" / subject name, label "Exam year", year cards. A year opens the website
 *    exams page (see StudyWorkspaceScreen). The three Entrance levels are inside this one screen and the
 *    Back button / phone back key go up one level first.
 * The Freshman GPA calculator is kept (the website shows it on the same page).
 * The unused parameters are kept so the caller in MainActivity.kt does not change.
 *
 * GLASS (2026-10-13, docs/GLASS_PLAN.md area 3): the round back button uses the glass look of the Targets screen.
 * S3 part 2 (2026-10-17): that back button is a Role.Button with a 48 dp touch target (it still looks 36 dp).
 * D3 (day mode): the back arrow and the page title use [WisdomInk] (white at night, deep navy by day) instead of Color.White.
 *
 * Clean cards (owner request 2026-10-13): the ECE course cards (and the workspace header they open) show the course
 * TITLE only, without the course code ("ECEg3051 · Electromagnetic Fields" -> "Electromagnetic Fields"). The code is
 * still in the data and in the cover picture.
 */
@Composable
fun PackageDetailScreen(
    pkg: NativePackage,
    onBack: () -> Unit,
    onOpenSubjectWorkspace: (title: String, subtitle: String, path: String) -> Unit,
    onStartLearning: (String) -> Unit,
    onUnlock: (String) -> Unit,
    isLoggedIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isFreshman = pkg.id == "freshman"
    val isEce = pkg.id.startsWith("ece")
    val isGrade = pkg.group == "grades"
    val isRemedial = pkg.id == "remedial"
    val isEntrance = pkg.id == ENTRANCE_PACKAGE_ID
    val hasSubjectList = isFreshman || isEce || isGrade || isRemedial || isEntrance

    // Entrance Questions drill-down state (stream -> subject)
    var streamId by rememberSaveable { mutableStateOf<String?>(null) }
    var subjectId by rememberSaveable { mutableStateOf<String?>(null) }
    val stream = if (isEntrance) ENTRANCE_STREAMS.firstOrNull { it.id == streamId } else null
    val subject = stream?.subjects?.firstOrNull { it.id == subjectId }

    // ECE drill-down state (semester cards -> that semester's courses)
    var eceSemesterId by rememberSaveable { mutableStateOf<String?>(null) }
    val eceSemester = if (isEce) ECE_SEMESTER_ENTRIES.firstOrNull { it.id == eceSemesterId } else null

    val goBack: () -> Unit = {
        if (subject != null) {
            subjectId = null
        } else if (stream != null) {
            streamId = null
        } else if (eceSemester != null) {
            eceSemesterId = null
        } else {
            onBack()
        }
    }
    BackHandler(enabled = stream != null || eceSemester != null) { goBack() }

    val eyebrow: String? = when {
        isGrade -> "Secondary Curriculum"
        isEce && eceSemester != null -> "$ECE_PACKAGE_NAME · $ECE_YEAR_LABEL"
        isEce -> "Department track · $ECE_YEAR_LABEL"
        isEntrance && stream != null && subject != null -> "Entrance Questions · ${stream.label}"
        isEntrance && stream != null -> "Entrance Questions"
        isEntrance -> "Past Exams"
        else -> null
    }
    val pageTitle: String = when {
        isFreshman -> "Freshman courses"
        isGrade -> "Grade ${pkg.id.removePrefix("grade-")}"
        isEce && eceSemester != null -> eceSemester.label
        isEce -> ECE_PACKAGE_NAME
        isEntrance && subject != null -> subject.name
        isEntrance && stream != null -> stream.label
        else -> pkg.name
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "top_nav") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WisdomGlassFillStrong)
                            .border(BorderStroke(1.dp, WisdomGlassBorder), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = WisdomCyan.copy(alpha = 0.2f)),
                                role = Role.Button,
                                onClick = goBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = WisdomInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            // Page heading (website wording)
            item(key = "page_heading") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (eyebrow != null) {
                        Text(
                            text = eyebrow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WisdomMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = pageTitle,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WisdomInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isFreshman) {
                        Text(
                            text = "including preengineering & others",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WisdomMuted
                        )
                    }
                }
            }

            if (isFreshman) {
                item(key = "gpa_calculator") {
                    GpaCalculatorCard()
                }
            }

            // ── Freshman: all subjects (the website has no stream/semester filters) ──
            if (isFreshman) {
                item(key = "subjects_label") { SubjectsLabel() }
                openGrid("freshman", FRESHMAN_SUBJECTS) { subject ->
                    OpenCard(subject.name, subject.assetImage) {
                        onOpenSubjectWorkspace(subject.name, "Freshman Program · Subject", subject.path)
                    }
                }
            }

            // ── ECE: first the two semester cover cards, then the 7 courses of the chosen semester ──
            if (isEce) {
                if (eceSemester == null) {
                    openGrid("ece_semesters", ECE_SEMESTER_ENTRIES) { sem ->
                        OpenCard(sem.label, sem.image) { eceSemesterId = sem.id }
                    }
                } else {
                    openGrid("ece_courses_${eceSemester.id}", eceSemester.courses) { course ->
                        OpenCard(course.title, course.assetImage) {
                            onOpenSubjectWorkspace(
                                course.title,
                                "ECE Year 3 · ${eceSemester.label}",
                                course.path
                            )
                        }
                    }
                }
            }

            // ── Grades 9–12 (subject photos from the website, packed by `copyWebsiteImages`) ──
            if (isGrade) {
                val gradeSubjects = when (pkg.id) {
                    "grade-9" -> GRADE_9_SUBJECTS
                    "grade-10" -> GRADE_10_SUBJECTS
                    "grade-11" -> GRADE_11_SUBJECTS
                    "grade-12" -> GRADE_12_SUBJECTS
                    else -> emptyList()
                }
                item(key = "subjects_label") { SubjectsLabel() }
                openGrid("grade", gradeSubjects) { gSubject ->
                    OpenCard(gSubject.name, gSubject.image) {
                        onOpenSubjectWorkspace(
                            "${gSubject.name} (Grade ${gSubject.grade})",
                            "Grade ${gSubject.grade} Curriculum",
                            gSubject.path
                        )
                    }
                }
            }

            // ── Entrance Questions: streams -> subjects -> years ──
            if (isEntrance) {
                if (stream == null) {
                    openGrid("entrance_streams", ENTRANCE_STREAMS) { s ->
                        OpenCard(s.label, s.image) { streamId = s.id }
                    }
                } else if (subject == null) {
                    openGrid("entrance_subjects", stream.subjects) { sub ->
                        OpenCard(sub.name, null) { subjectId = sub.id }
                    }
                } else {
                    item(key = "entrance_years_label") { SubjectsLabel("Exam year") }
                    openGrid("entrance_years", ENTRANCE_YEARS) { year ->
                        OpenCard(year, null) {
                            onOpenSubjectWorkspace(
                                "${subject.name} $year",
                                "Entrance Questions · ${stream.label}",
                                entranceYearPath(stream.id, subject.id, year)
                            )
                        }
                    }
                }
            }

            // ── Remedial ──
            if (isRemedial) {
                item(key = "subjects_label") { SubjectsLabel() }
                openGrid("remedial", REMEDIAL_SUBJECTS) { subject ->
                    OpenCard(subject.name, subject.assetImage) {
                        onOpenSubjectWorkspace(subject.name, "Remedial Program", subject.path)
                    }
                }
            }

            // Any other package (COC, UAT, GAT, Exit Exam ...): one card that opens its study hubs
            if (!hasSubjectList) {
                openGrid("package", listOf(pkg)) { p ->
                    OpenCard(p.name, p.assetImage) {
                        onOpenSubjectWorkspace(p.name, "Academy Package", p.path)
                    }
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun SubjectsLabel(text: String = "Subjects") {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = WisdomMuted,
        letterSpacing = 1.5.sp
    )
}
