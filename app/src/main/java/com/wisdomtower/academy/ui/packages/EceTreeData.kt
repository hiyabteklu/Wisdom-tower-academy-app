package com.wisdomtower.academy.ui.packages

/**
 * ECE hierarchy model for the expansion (docs/ECE_STATUS.md, slice E1): year > semester > optional stream > course.
 * DATA ONLY. No screen uses it yet (E2 wires Year 1, E3 Year 2). Year 3 is LOCKED and is NOT in this file; it keeps
 * ECE_SEM1_COURSES / ECE_SEM2_COURSES and the ids ece-y3-sem-1 / ece-y3-sem-2.
 *
 * Names are the short titles (same style as Year 3), no credit hours, no prerequisites.
 * Course id = lowercase code, stable, never renamed. Path keeps the Year 3 depth:
 * /academy/special-packages/electrical-computer-engineering/<yN-sem-M>/<slug>
 * Package ids follow the Year 3 pattern: ece-y1-sem-1 ... (access rules for them are NOT decided yet).
 *
 * Blueprint typo: it prints ECEg2052 for BOTH Computer Programming and Applied Modern Physics. Computer Programming
 * keeps the id eceg1052 (the id already used by its published content); Applied Modern Physics keeps eceg2052.
 */
data class EceTreeCourse(val id: String, val code: String, val title: String, val path: String)

data class EceTreeStream(val id: String, val label: String, val courses: List<EceTreeCourse>)

data class EceTreeSemester(
    val packageId: String,
    val id: String,
    val label: String,
    val courses: List<EceTreeCourse>,
    val streams: List<EceTreeStream> = emptyList()
)

data class EceTreeYear(val id: String, val label: String, val semesters: List<EceTreeSemester>)

private const val ECE_TREE_BASE = "/academy/special-packages/electrical-computer-engineering"

private fun eceTreeCourse(year: Int, sem: Int, code: String, title: String): EceTreeCourse {
    val slug = code.lowercase()
    return EceTreeCourse(slug, code, title, "$ECE_TREE_BASE/y$year-sem-$sem/$slug")
}

val ECE_TREE_YEAR_1 = EceTreeYear(
    id = "y1",
    label = "Year 1",
    semesters = listOf(
        EceTreeSemester(
            packageId = "ece-y1-sem-1", id = "sem-1", label = "Semester 1",
            courses = listOf(
                eceTreeCourse(1, 1, "FLEn1011", "Communicative English I"),
                eceTreeCourse(1, 1, "LoCT1011", "Critical Thinking"),
                eceTreeCourse(1, 1, "Math1011", "Mathematics for Natural Sciences"),
                eceTreeCourse(1, 1, "Phys1011", "General Physics"),
                eceTreeCourse(1, 1, "GeES1011", "Geography of Ethiopia and the Horn"),
                eceTreeCourse(1, 1, "Psch1011", "General Psychology"),
                eceTreeCourse(1, 1, "SpSc1011", "Physical Fitness")
            )
        ),
        EceTreeSemester(
            packageId = "ece-y1-sem-2", id = "sem-2", label = "Semester 2",
            courses = listOf(
                eceTreeCourse(1, 2, "FLEn1012", "Communicative English II"),
                eceTreeCourse(1, 2, "Anth1012", "Social Anthropology"),
                eceTreeCourse(1, 2, "Math1042", "Applied Mathematics I"),
                eceTreeCourse(1, 2, "MGMT1012", "Entrepreneurship"),
                eceTreeCourse(1, 2, "EmTe1012", "Emerging Technologies"),
                eceTreeCourse(1, 2, "MCiE1012", "Moral and Civic Education"),
                eceTreeCourse(1, 2, "ECEg1052", "Computer Programming"),
                eceTreeCourse(1, 2, "Hist1012", "History of Ethiopia and the Horn")
            )
        )
    )
)

val ECE_TREE_YEAR_2 = EceTreeYear(
    id = "y2",
    label = "Year 2",
    semesters = listOf(
        EceTreeSemester(
            packageId = "ece-y2-sem-1", id = "sem-1", label = "Semester 1",
            courses = listOf(
                eceTreeCourse(2, 1, "ECEg2041", "Fundamentals of Electrical Engineering"),
                eceTreeCourse(2, 1, "ECEg2043", "Electrical Engineering Lab I"),
                eceTreeCourse(2, 1, "MEng2061", "Engineering Drawing"),
                eceTreeCourse(2, 1, "CEng2051", "Engineering Mechanics I"),
                eceTreeCourse(2, 1, "Math2061", "Applied Mathematics II"),
                eceTreeCourse(2, 1, "ECEg2061", "Probability and Random Process"),
                eceTreeCourse(2, 1, "SNIE1012", "Inclusiveness")
            )
        ),
        EceTreeSemester(
            packageId = "ece-y2-sem-2", id = "sem-2", label = "Semester 2",
            courses = listOf(
                eceTreeCourse(2, 2, "Math2062", "Applied Mathematics III"),
                eceTreeCourse(2, 2, "MEng2052", "Engineering Mechanics II"),
                eceTreeCourse(2, 2, "ECEg2070", "Applied Electronics I"),
                eceTreeCourse(2, 2, "ECEg2052", "Applied Modern Physics"),
                eceTreeCourse(2, 2, "GlTr1012", "Global Trend"),
                eceTreeCourse(2, 2, "ECEg2072", "Electrical Engineering Lab II"),
                eceTreeCourse(2, 2, "ECEg2042", "Electrical Workshop Practice I")
            )
        )
    )
)

/** Years that have data so far. E4 adds Year 4, E6 adds Year 5 (both use [EceTreeSemester.streams]). */
val ECE_TREE_YEARS = listOf(ECE_TREE_YEAR_1, ECE_TREE_YEAR_2)
