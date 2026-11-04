package com.wisdomtower.academy.data.admin

// ADMIN-native N6b. Hand copy of ADMIN_CONTENT_TREE in website-reference/src/data/admin-nav.ts, built from the same
// source lists (grades and hubs: data/academy.ts; subjects per grade: data/grade-subjects.ts; freshman courses:
// data/freshman.ts; remedial subjects: data/remedial.ts; ECE courses: data/special-packages.ts; entrance streams,
// subjects and years: data/entrance-questions.ts). Only ids and names are kept (no pictures, no descriptions, no prices).
// HYPOTHESIS: when the website adds or renames a subject, course, year or hub, this file must be updated by hand.
// The Course Content slice (N7) will reuse this tree.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

/** One step of the admin navigation tree. packageId and scopePath decide which lock key the node uses. */
class AdminNavNode(
    val id: String,
    val label: String,
    val packageId: String?,
    val scopePath: String?,
    val children: List<AdminNavNode>
)

fun packageLockKey(packageId: String): String = "package:$packageId"

fun scopeLockKey(scopePath: String): String = "scope:$scopePath"

fun hubLockKey(scopePath: String, hub: String): String = "hub:$scopePath:$hub"

/** The learning hubs shown on every subject (resourceHubs in academy.ts; videos is not shown there). */
private val HUBS: List<Pair<String, String>> = listOf(
    "books" to "Books",
    "short-notes" to "Short Notes",
    "flashcards" to "Flashcards",
    "question-banks" to "Question Banks",
    "exams" to "Exams",
    "life-savers" to "Life Savers"
)

/** HUB_IDS of LocksPanel.tsx. A node with one of these ids and a scopePath is a hub. */
val ADMIN_HUB_IDS: Set<String> = setOf(
    "books", "short-notes", "videos", "flashcards", "question-banks", "exams", "life-savers"
)

private fun hubNodes(packageId: String?, scopePath: String, onlyId: String? = null): List<AdminNavNode> =
    HUBS.filter { onlyId == null || it.first == onlyId }
        .map { AdminNavNode(it.first, it.second, packageId, scopePath, emptyList()) }

private fun subjectNode(packageId: String?, scopePath: String, id: String, name: String): AdminNavNode =
    AdminNavNode(id, name, packageId, scopePath, hubNodes(packageId, scopePath))

private val GRADE_SUBJECTS: Map<String, List<Pair<String, String>>> = mapOf(
    "9" to listOf(
        "chemistry" to "Chemistry", "mathematics" to "Math", "physics" to "Physics", "biology" to "Biology",
        "geography" to "Geography", "history" to "History", "economics" to "Economics"
    ),
    "10" to listOf(
        "mathematics" to "Math", "biology" to "Biology", "chemistry" to "Chemistry", "physics" to "Physics",
        "geography" to "Geography", "history" to "History", "economics" to "Economics"
    ),
    "11" to listOf(
        "mathematics" to "Math", "biology" to "Biology", "physics" to "Physics", "chemistry" to "Chemistry",
        "economics" to "Economics", "geography" to "Geography", "history" to "History"
    ),
    "12" to listOf(
        "english" to "English", "chemistry" to "Chemistry", "biology" to "Biology", "mathematics" to "Math",
        "physics" to "Physics", "geography" to "Geography", "economics" to "Economics", "history" to "History",
        "sat" to "SAT"
    )
)

private fun gradeNodes(): List<AdminNavNode> = listOf("9", "10", "11", "12").map { g ->
    val packageId = "grade-$g"
    AdminNavNode(
        id = "grade-$g",
        label = "Grade $g",
        packageId = packageId,
        scopePath = null,
        children = (GRADE_SUBJECTS[g] ?: emptyList()).map { (subjectId, name) ->
            subjectNode(packageId, "grade/$g/$subjectId", subjectId, name)
        }
    )
}

private val REMEDIAL_SUBJECTS: List<Pair<String, String>> = listOf(
    "english" to "English",
    "maths" to "Maths",
    "physics" to "Physics",
    "chemistry" to "Chemistry",
    "biology" to "Biology",
    "history" to "History",
    "geography" to "Geography"
)

private fun remedialNode(): AdminNavNode = AdminNavNode(
    id = "remedial",
    label = "Remedial",
    packageId = "remedial",
    scopePath = null,
    children = REMEDIAL_SUBJECTS.map { (id, name) ->
        subjectNode("remedial", "remedial/$id", id, name)
    }
)

private const val ENTRANCE_PACKAGE_ID = "entrance-questions"
private val ENTRANCE_YEARS: List<String> = listOf("2014", "2015", "2016", "2017", "2018", "2019")

private val ENTRANCE_COMMON: List<Pair<String, String>> = listOf(
    "english" to "English",
    "mathematics" to "Mathematics",
    "sat" to "Scholastic Aptitude Test (SAT)"
)

private fun entranceNode(): AdminNavNode {
    val streams: List<Triple<String, String, List<Pair<String, String>>>> = listOf(
        Triple(
            "natural", "Natural Science",
            ENTRANCE_COMMON + listOf("physics" to "Physics", "chemistry" to "Chemistry", "biology" to "Biology")
        ),
        Triple(
            "social", "Social Science",
            ENTRANCE_COMMON + listOf("geography" to "Geography", "history" to "History", "economics" to "Economics")
        )
    )
    return AdminNavNode(
        id = "entrance-questions",
        label = "Entrance Questions",
        packageId = ENTRANCE_PACKAGE_ID,
        scopePath = null,
        children = streams.map { stream ->
            AdminNavNode(
                id = "entrance-${stream.first}",
                label = stream.second,
                packageId = ENTRANCE_PACKAGE_ID,
                scopePath = null,
                children = stream.third.map { (subjectId, subjectName) ->
                    AdminNavNode(
                        id = "${stream.first}-$subjectId",
                        label = subjectName,
                        packageId = ENTRANCE_PACKAGE_ID,
                        scopePath = null,
                        children = ENTRANCE_YEARS.map { year ->
                            val scope = "entrance/${stream.first}/$subjectId/$year"
                            AdminNavNode(
                                id = "${stream.first}-$subjectId-$year",
                                label = year,
                                packageId = ENTRANCE_PACKAGE_ID,
                                scopePath = scope,
                                children = hubNodes(ENTRANCE_PACKAGE_ID, scope, "exams")
                            )
                        }
                    )
                }
            )
        }
    )
}

private val FRESHMAN_SUBJECTS: List<Pair<String, String>> = listOf(
    "math-natural" to "Math Natural",
    "math-social" to "Math Social",
    "physical-fitness" to "Physical Fitness",
    "english-1" to "English 1",
    "physics" to "Physics",
    "psychology" to "Psychology",
    "logic" to "Logic",
    "geography" to "Geography",
    "anthropology" to "Anthropology",
    "civics" to "Civics",
    "economics" to "Economics",
    "emerging-technology" to "Emerging Technology",
    "entrepreneurship" to "Entrepreneurship",
    "global-trends" to "Global Trends",
    "history" to "History",
    "inclusiveness" to "Inclusiveness",
    "chemistry" to "Chemistry",
    "biology" to "Biology",
    "cpp-programming" to "C++ Programming",
    "applied-math-1" to "Applied Math 1",
    "english-2" to "English 2"
)

private fun freshmanNode(): AdminNavNode = AdminNavNode(
    id = "freshman",
    label = "Freshman",
    packageId = "freshman",
    scopePath = null,
    children = FRESHMAN_SUBJECTS.map { (id, name) -> subjectNode("freshman", "freshman/$id", id, name) }
)

private class EceSemester(
    val id: String,
    val shortLabel: String,
    val packageId: String,
    val courses: List<Pair<String, String>>
)

private val ECE_SEMESTERS: List<EceSemester> = listOf(
    EceSemester(
        "sem-1", "S1", "ece-y3-sem-1",
        listOf(
            "ECEg3071" to "Applied Electronics II",
            "Econ1011" to "Economics",
            "ECEg3051" to "Electromagnetic Fields",
            "ECEg3081" to "Signals and Systems Analysis",
            "ECEg3073" to "Electrical Engineering Laboratory III",
            "ECEg3101" to "Object Oriented Programming",
            "ECEg3061" to "Computational Methods"
        )
    ),
    EceSemester(
        "sem-2", "S2", "ece-y3-sem-2",
        listOf(
            "MEng3052" to "Engineering Thermodynamics",
            "ECEg3082" to "Network Analysis and Synthesis",
            "ECEg3092" to "Introduction to Electrical Machines",
            "ECEg3094" to "Electrical Engineering Lab IV",
            "ECEg3102" to "Digital Logic Design",
            "ECEg3052" to "Electrical Materials and Technology",
            "ECEg3096" to "Electrical Workshop Practice II"
        )
    )
)

private fun specialNode(): AdminNavNode = AdminNavNode(
    id = "special",
    label = "Special packages",
    packageId = null,
    scopePath = null,
    children = ECE_SEMESTERS.map { sem ->
        AdminNavNode(
            id = "electrical-computer-engineering-${sem.id}",
            label = "Electrical & Computer Engineering · ${sem.shortLabel}",
            packageId = sem.packageId,
            scopePath = null,
            children = sem.courses.map { (code, title) ->
                val slug = code.lowercase()
                subjectNode(sem.packageId, "ece/${sem.id}/$slug", slug, "$code · $title")
            }
        )
    }
)

val ADMIN_CONTENT_TREE: List<AdminNavNode> = listOf(
    AdminNavNode("grades", "Grades 9–12", null, null, gradeNodes()),
    entranceNode(),
    freshmanNode(),
    remedialNode(),
    specialNode()
)
