package com.wisdomtower.academy.ui.packages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.ui.study.HubGateDialog
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape

// Electrical & Computer Engineering (website-reference/src/data/special-packages.ts: name "Electrical & Computer
// Engineering", yearLabel "Year 3", semesters "Semester 1" / "Semester 2" with their own cover pictures).
private const val ECE_PACKAGE_NAME = "Electrical & Computer Engineering"
// Year 3 label (LOCKED). Years 1 and 2 take their label from EceTreeData.kt (E2, docs/ECE_STATUS.md).
private const val ECE_YEAR_LABEL = "Year 3"

// One course card on the ECE course list. Year 3 fills it from NativeSpecialCourse (picture + path unchanged);
// Years 1, 2, 4, 5 fill it from EceTreeCourse (no course pictures exist for them, so image is null and [code] is set).
private data class EceCourseCard(
    val title: String,
    val image: String?,
    val path: String,
    val code: String? = null
)

private data class EceSemesterEntry(
    val id: String,
    val label: String,
    val image: String,
    val courses: List<EceCourseCard>,
    val streams: List<EceTreeStream> = emptyList()
)

private fun List<NativeSpecialCourse>.toEceCards(): List<EceCourseCard> =
    map { EceCourseCard(it.title, it.assetImage, it.path) }

// ECE-FIX-C2: Year 3 semester covers go through the helper (ece-y3-sem-N.jpg first, old ece-sem-N.jpg fallback).
// Computed on demand (not a top-level val) so the asset listing is read when the screen is used, not at class load.
private fun eceYear3SemesterEntries(): List<EceSemesterEntry> = listOf(
    EceSemesterEntry("sem-1", "Semester 1", eceSemesterCover(3, 1), ECE_SEM1_COURSES.toEceCards()),
    EceSemesterEntry("sem-2", "Semester 2", eceSemesterCover(3, 2), ECE_SEM2_COURSES.toEceCards())
)

/** The Year 1 / 2 / 4 / 5 data for a package id like "ece-y1-sem-1"; null for Year 3 ("ece-y3-sem-N") and anything else. */
private fun eceTreeYearFor(pkgId: String): EceTreeYear? =
    ECE_TREE_YEARS.firstOrNull { pkgId.startsWith("ece-${it.id}-") }

private fun eceYearLabelFor(pkgId: String): String =
    eceTreeYearFor(pkgId)?.label ?: ECE_YEAR_LABEL

/** Semester cards for the year of this package. Year 3 (and any unknown ece id) returns the original Year 3 list. */
private fun eceSemesterEntriesFor(pkgId: String): List<EceSemesterEntry> {
    val treeYear = eceTreeYearFor(pkgId) ?: return eceYear3SemesterEntries()
    // Tree year ids look like "y1".."y5". If one does not parse, the old Year 3 pictures are used as before.
    val yearNo = treeYear.id.removePrefix("y").toIntOrNull()
    return treeYear.semesters.map { sem ->
        val semNo = if (sem.id == "sem-1") 1 else 2
        EceSemesterEntry(
            id = sem.id,
            label = sem.label,
            // ECE-FIX-C2: own cover ece-y<year>-sem-<n>.jpg if the owner added it, else the old shared ece-sem-<n>.jpg.
            image = if (yearNo != null) {
                eceSemesterCover(yearNo, semNo)
            } else {
                eceCoverAsset("ece-sem-$semNo.jpg", "ece-sem-$semNo.jpg")
            },
            courses = sem.courses.map { EceCourseCard(it.title, null, it.path, it.code) },
            streams = sem.streams
        )
    }
}

/**
 * Package page (shared [OpenCard] cards in a TWO-column grid; headings from the website pages, see git history of
 * this file for the full wording notes: Freshman, Secondary Curriculum grades, Remedial, ECE semesters -> courses,
 * Entrance Questions streams -> subjects -> years).
 *
 * COMPACT HEADER (owner request 2026-10-22): no round back button and no big title block (it looked like a website
 * and wasted vertical space). The top of the page is ONE small muted line that names where the student is (the page
 * title of the current level only, no eyebrow, no duplicate texts). Going back = the phone back key / gesture:
 * [BackHandler] goes up one drill-down level first (ECE semester, Entrance subject / stream), then the app's own
 * back handling leaves the package. The "Subjects" label is gone too (the cards speak for themselves).
 * The unused parameters are kept so the caller in MainActivity.kt does not change.
 *
 * NO PRICES HERE (owner order 2026-10-23): the app stays clean. A price appears only in the "Package required"
 * pop-up when a locked chapter is tapped (Telegram build, `docs/TELEGRAM_PAID_BUILD_PLAN.md`).
 *
 * ECE-FIX-B (2026-11-23): for Years 1, 2, 4, 5 the courses and the stream chooser are narrow [EceCourseRow] rows (no
 * pictures exist). Year 3 keeps its two-column image card grid exactly as before. The semester chooser stays cards.
 */
@Composable
fun PackageDetailScreen(
    pkg: NativePackage,
    onBack: () -> Unit,
    onOpenSubjectWorkspace: (title: String, subtitle: String, path: String) -> Unit,
    onStartLearning: (String) -> Unit,
    onUnlock: (String) -> Unit,
    isLoggedIn: Boolean = false,
    backEnabled: Boolean = true, // BACKFIX-S7
    modifier: Modifier = Modifier
) {
    val isFreshman = pkg.id == "freshman"
    val isEce = pkg.id.startsWith("ece")
    val isGrade = pkg.group == "grades"
    val isRemedial = pkg.id == "remedial"
    val isEntrance = pkg.id == ENTRANCE_PACKAGE_ID
    val isUat = pkg.id == "uat"
    val isCoc = pkg.id == "coc"
    val hasSubjectList = isFreshman || isEce || isGrade || isRemedial || isEntrance

    // Entrance Questions drill-down state (stream -> subject)
    var streamId by rememberSaveable { mutableStateOf<String?>(null) }
    var subjectId by rememberSaveable { mutableStateOf<String?>(null) }
    val stream = if (isEntrance) ENTRANCE_STREAMS.firstOrNull { it.id == streamId } else null
    val subject = stream?.subjects?.firstOrNull { it.id == subjectId }

    // ECE drill-down state (semester cards -> that semester's courses). The year (1, 2, 3, 4 or 5) comes from the package id.
    var eceSemesterId by rememberSaveable { mutableStateOf<String?>(null) }
    var eceStreamId by rememberSaveable { mutableStateOf<String?>(null) }
    var cocStreamLabel by rememberSaveable { mutableStateOf<String?>(null) }
    val eceYearLabel = if (isEce) eceYearLabelFor(pkg.id) else ECE_YEAR_LABEL
    val eceSemesters = if (isEce) eceSemesterEntriesFor(pkg.id) else emptyList()
    val eceSemester = if (isEce) eceSemesters.firstOrNull { it.id == eceSemesterId } else null
    val eceStream = eceSemester?.streams?.firstOrNull { it.id == eceStreamId }
    // ECE-FIX-B: true for Years 1, 2, 4, 5 (rows); false for Year 3 (image card grid).
    val eceUsesRows = isEce && eceTreeYearFor(pkg.id) != null

    val goBack: () -> Unit = {
        if (subject != null) {
            subjectId = null
        } else if (stream != null) {
            streamId = null
        } else if (eceStream != null) {
            eceStreamId = null
        } else if (eceSemester != null) {
            eceSemesterId = null
        } else {
            onBack()
        }
    }
    BackHandler(enabled = backEnabled && (stream != null || eceStream != null || eceSemester != null)) { goBack() } // BACKFIX-S7

    // ONE small line: where am I (current level only)
    val pageTitle: String = when {
        isFreshman -> "Freshman courses"
        isGrade -> "Grade ${pkg.id.removePrefix("grade-")}"
        isEce && eceStream != null -> "${eceStream.label} · ${eceSemester?.label.orEmpty()} · $eceYearLabel"
        isEce && eceSemester != null -> "${eceSemester.label} \u00B7 $eceYearLabel"
        isEce -> "$ECE_PACKAGE_NAME \u00B7 $eceYearLabel"
        isEntrance && subject != null -> subject.name
        isUat -> "University Admission Test"
        isCoc -> "Certificate of Competency"
        isEntrance && stream != null -> stream.label
        isEntrance -> "Entrance Questions"
        else -> pkg.name
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "page_heading") {
                Text(
                    text = pageTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WisdomMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
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

            // ── ECE: first the semester cover cards, then the courses of the chosen semester (Year 1 to 5) ──
            if (isEce) {
                if (eceSemester == null) {
                    openGrid("ece_semesters", eceSemesters) { sem ->
                        OpenCard(sem.label, sem.image) { eceSemesterId = sem.id }
                    }
                } else if (eceSemester.streams.isNotEmpty() && eceStream == null) {
                    // ECE-FIX-B: stream chooser = narrow rows ("Communication", "7 courses")
                    items(eceSemester.streams, key = { "ece_stream_${eceSemester.id}_${it.id}" }) { stream ->
                        val n = stream.courses.size
                        EceCourseRow(
                            code = "$n ${if (n == 1) "course" else "courses"}",
                            title = stream.label,
                            onClick = { eceStreamId = stream.id }
                        )
                    }
                } else if (eceStream != null) {
                    items(eceStream.courses, key = { "ece_sc_${eceStream.id}_${it.path}" }) { course ->
                        EceCourseRow(
                            code = course.code,
                            title = course.title,
                            onClick = {
                                onOpenSubjectWorkspace(
                                    course.title,
                                    "ECE $eceYearLabel · ${eceSemester.label} · ${eceStream.label}",
                                    course.path
                                )
                            }
                        )
                    }
                } else if (eceUsesRows) {
                    // ECE-FIX-B: Years 1, 2, 4 (S1) = narrow rows
                    items(eceSemester.courses, key = { "ece_row_${it.path}" }) { course ->
                        EceCourseRow(
                            code = course.code,
                            title = course.title,
                            onClick = {
                                onOpenSubjectWorkspace(
                                    course.title,
                                    "ECE $eceYearLabel · ${eceSemester.label}",
                                    course.path
                                )
                            }
                        )
                    }
                } else {
                    // Year 3 (LOCKED): the original image card grid, unchanged.
                    openGrid("ece_courses_${eceSemester.id}", eceSemester.courses) { course ->
                        OpenCard(course.title, course.image) {
                            onOpenSubjectWorkspace(
                                course.title,
                                "ECE $eceYearLabel · ${eceSemester.label}",
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

            // UAT has four purpose-built hubs, matching website-reference/docs/UAT-HUBS.md.
            // Card pictures: the uploaded 16:9 JPGs (website-reference/public/images/uat-hubs/*.jpg, packed into
            // assets/images/web/uat-hubs/ by `copyWebsiteImages`). The .svg files are only the old placeholders.
            if (isUat) {
                item(key = "uat_facts") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("100 questions", "150 minutes", "Computer-based").forEach { fact ->
                            Text(
                                text = fact,
                                color = WisdomInk,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).background(WisdomAccentEmerald.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                item(key = "uat_hubs_label") { SubjectsLabel("Learning Hubs") }
                val uatHubs = listOf(
                    Triple("Tips", "file:///android_asset/images/web/uat-hubs/tips.jpg", "/academy/uat/tips"),
                    Triple("Revision Notes", "file:///android_asset/images/web/uat-hubs/revision-notes.jpg", "/academy/uat/revision-notes"),
                    Triple("Previous Exams", "file:///android_asset/images/web/uat-hubs/previous-exams.jpg", "/academy/uat/previous-exams"),
                    Triple("Resources", "file:///android_asset/images/web/uat-hubs/resources.jpg", "/academy/uat/resources")
                )
                openGrid("uat_hubs", uatHubs) { hub ->
                    OpenCard(hub.first, hub.second) {
                        onOpenSubjectWorkspace(hub.first, "UAT Learning Hub", hub.third)
                    }
                }
            }

            // COC currently offers Natural and Social stream cards, both explicitly under development.
            if (isCoc) {
                item(key = "coc_stream_label") { SubjectsLabel("Choose your stream") }
                openGrid("coc_streams", ENTRANCE_STREAMS) { stream ->
                    OpenCard(stream.label, stream.image) { cocStreamLabel = stream.label }
                }
            }

            // Other packages that still use the generic one-card landing.
            if (!hasSubjectList && !isUat && !isCoc) {
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

    if (cocStreamLabel != null) {
        HubGateDialog(
            badge = ContentAccess.COMING_SOON_TITLE,
            title = ContentAccess.COMING_SOON_TITLE,
            hubName = cocStreamLabel,
            message = "We are building the COC material for this stream. It will appear here when it is ready.",
            buttonText = ContentAccess.COMING_SOON_BUTTON,
            onClose = { cocStreamLabel = null }
        )
    }
}

/** Section label. The default "Subjects" label is no longer drawn (compact header, 2026-10-22). */
@Composable
private fun SubjectsLabel(text: String = "Subjects") {
    if (text == "Subjects") return
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = WisdomMuted,
        letterSpacing = 1.5.sp
    )
}
