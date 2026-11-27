package com.wisdomtower.academy.ui.packages

import androidx.compose.ui.graphics.Color
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentFuchsia
import com.wisdomtower.academy.ui.theme.WisdomAccentIndigo
import com.wisdomtower.academy.ui.theme.WisdomAccentPurple
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomAccentViolet
import com.wisdomtower.academy.ui.theme.WisdomCyan

/**
 * Seed data copied from the website reference:
 * - src/data/packages.ts        (package cards; descriptions here are shortened, see STATUS.md S4 item d)
 * - src/data/freshman.ts        (subject names and descriptions, exact)
 * - src/data/special-packages.ts (ECE courses, exact)
 * - src/data/grade-subjects.ts  (grade subjects, exact; 2026-10-11 website update: no "Exam tips" in Grade 12;
 *                                covers are generated from grade and subject ids)
 * - src/data/entrance-questions.ts (see EntranceData.kt; the package entry below is the "Entrance Questions" card)
 * - src/data/remedial.ts        (remedial subject ids, names and descriptions exact; covers are generated from ids)
 */

data class NativePackage(
    val id: String,
    val name: String,
    val shortName: String,
    val description: String,
    val path: String,
    val assetImage: String,
    val enrolledLabel: String,
    val group: String, // "grades" | "branch" | "special" | "entrance" (not listed in the Packages tab, like the website catalog)
    val accentColor: Color,
    val includes: List<String>
)

data class NativeSubject(
    val id: String,
    val name: String,
    val description: String,
    val assetImage: String,
    val path: String
)

// Exact copy of CORE_PACKAGE_INCLUDES in website-reference/src/data/packages.ts
val CORE_INCLUDES = listOf(
    "Official Textbooks & Comprehensive Reference Books",
    "Chapter-by-Chapter Short Notes & Key Summaries",
    "Extensive Chapter Question Banks",
    "Mock & Model Practice Exams",
    "All worked with official step-by-step solutions + Explain with AI",
    "All official universities midterms & finals worked with detailed solutions",
    "Flashcards for rapid active recall",
    "And more!"
)

val CATALOG_PACKAGES = listOf(
    // Grades 9–12
    NativePackage(
        id = "grade-9",
        name = "Grade 9 Package",
        shortName = "G9",
        description = "Start secondary with material built for Grade 9, not recycled general notes. Clear explanations, chapter practice, and exam-style questions so you build real confidence from the first term.",
        path = "/academy/grades/9",
        assetImage = codeCoverUri("grade-9"),
        enrolledLabel = "320+ students",
        group = "grades",
        accentColor = WisdomAccentSky,
        includes = listOf("Complete Grade 9 syllabus & curriculum coverage") + CORE_INCLUDES
    ),
    NativePackage(
        id = "grade-10",
        name = "Grade 10 Package",
        shortName = "G10",
        description = "Grade 10 is where depth matters. Structured notes, chapter question banks, and solved practice exams help you master concepts before they pile up.",
        path = "/academy/grades/10",
        assetImage = codeCoverUri("grade-10"),
        enrolledLabel = "410+ students",
        group = "grades",
        accentColor = WisdomAccentSky,
        includes = listOf("Complete Grade 10 curriculum & stream preparation") + CORE_INCLUDES
    ),
    NativePackage(
        id = "grade-11",
        name = "Grade 11 Package",
        shortName = "G11",
        description = "Grade 11 raises the standard. Get subject notes written for this level, dense practice by chapter, and exams with full solutions so you know exactly how answers are built.",
        path = "/academy/grades/11",
        assetImage = codeCoverUri("grade-11"),
        enrolledLabel = "480+ students",
        group = "grades",
        accentColor = WisdomAccentSky,
        includes = listOf("Complete Grade 11 Natural & Social science streams") + CORE_INCLUDES
    ),
    NativePackage(
        id = "grade-12",
        name = "Grade 12 Package",
        shortName = "G12",
        description = "The year that counts most. Grade 12 material is organized for finals pace: precise notes, chapter drills, flashcards for fast review, and practice exams with solutions.",
        path = "/academy/grades/12",
        assetImage = codeCoverUri("grade-12"),
        enrolledLabel = "560+ students",
        group = "grades",
        accentColor = WisdomAccentSky,
        includes = listOf("Complete Grade 12 syllabus & national matriculation prep") + CORE_INCLUDES
    ),

    // Entrance Questions: independent top-level card on the website Home / Academy page.
    // Not part of the website package catalog (never sellable), so group "entrance" keeps it out of the
    // Packages tab. Texts copied from LandingPathways.tsx (description, includes, category as label).
    NativePackage(
        id = ENTRANCE_PACKAGE_ID,
        name = "Entrance Questions",
        shortName = "Entrance",
        description = "Past entrance exams for Natural and Social Science students, organised by subject and year (2014-2019).",
        path = ENTRANCE_BASE_PATH,
        assetImage = codeCoverUri("entrance-questions"),
        enrolledLabel = "University Entrance",
        group = "entrance",
        accentColor = WisdomCyan,
        includes = listOf(
            "Natural Science and Social Science entrance exams",
            "English, Mathematics, SAT and stream subjects",
            "Exams for 2014 to 2019"
        )
    ),

    // Other branches
    NativePackage(
        id = "freshman",
        name = "Freshman Package",
        shortName = "Freshman",
        description = "Every first-year course in one place, natural and social streams included. Notes, chapter questions, flashcards, and solved practice exams for 20+ courses.",
        path = "/academy/freshman",
        assetImage = codeCoverUri("freshman"),
        enrolledLabel = "890+ students",
        group = "branch",
        accentColor = WisdomAccentPurple,
        includes = listOf(
            "All 20+ freshman courses (natural and social streams)",
            "Ethiopian university GPA calculator & field leaderboard"
        ) + CORE_INCLUDES
    ),
    NativePackage(
        id = "uat",
        name = "UAT Package",
        shortName = "UAT",
        description = "University Admission Test prep that respects how the exam is actually written. Focused notes, chapter question banks, flashcards for rapid recall, and practice exams with solutions.",
        path = "/academy/uat",
        assetImage = codeCoverUri("uat"),
        enrolledLabel = "610+ students",
        group = "branch",
        accentColor = WisdomAccentEmerald,
        includes = listOf("Comprehensive UAT quantitative & verbal entrance tracks") + CORE_INCLUDES
    ),
    NativePackage(
        id = "gat",
        name = "GAT Package",
        shortName = "GAT",
        description = "Graduate Admission Test resources organized the way the exam expects you to think. Notes on core GAT material, chapter questions, flashcards, and practice exams with solutions.",
        path = "/academy/gat",
        assetImage = codeCoverUri("gat"),
        enrolledLabel = "Coming soon",
        group = "branch",
        accentColor = WisdomAccentRose,
        includes = listOf("Postgraduate GAT analytical & quantitative problem tracks") + CORE_INCLUDES
    ),
    NativePackage(
        id = "coc",
        name = "COC Package",
        shortName = "COC",
        description = "Certificate of Competency prep with clear notes, chapter practice, flashcards, and solved exams. Material aimed at the skills and judgment the assessment rewards.",
        path = "/academy/coc",
        assetImage = codeCoverUri("coc"),
        enrolledLabel = "380+ students",
        group = "branch",
        accentColor = WisdomAccentIndigo,
        includes = listOf("Occupational standard competencies & evaluation prep") + CORE_INCLUDES
    ),
    NativePackage(
        id = "exit-exam",
        name = "Exit Exam Package",
        shortName = "Exit Exam",
        description = "University exit exam review by department, with structured notes and practice when materials open. Designed for final-year students who need focused revision.",
        path = "/academy/exit-exam",
        assetImage = codeCoverUri("exit-exam"),
        enrolledLabel = "Coming soon",
        group = "branch",
        accentColor = WisdomAccentFuchsia,
        includes = listOf("Department graduation exit exam comprehensive tracks") + CORE_INCLUDES
    ),
    NativePackage(
        id = "remedial",
        name = "Remedial Package",
        shortName = "Remedial",
        description = "Catch-up pathway for core subjects. Strengthen foundations in English, Maths, Physics, Chemistry, Biology, History and Geography with the same learning hubs used across the Academy.",
        path = "/academy/remedial",
        assetImage = codeCoverUri("remedial"),
        enrolledLabel = "New pathway",
        group = "branch",
        accentColor = WisdomAccentAmber,
        includes = listOf("All seven core remedial prerequisite subjects") + CORE_INCLUDES
    ),

    // Special packages
    // ECE covers use the code-drawn image identity; no per-year artwork files are bundled.
    NativePackage(
        id = "ece-y1-sem-1",
        name = "ECE Year 1: Semester 1",
        shortName = "ECE Y1 S1",
        description = "Electrical and Computer Engineering, Year 1, Semester 1. Course material written for your department, with the same six learning hubs in every course.",
        path = "/academy/special-packages/electrical-computer-engineering/y1-sem-1",
        assetImage = eceSemesterCover(1, 1),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("All Year 1 Semester 1 engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y1-sem-2",
        name = "ECE Year 1: Semester 2",
        shortName = "ECE Y1 S2",
        description = "Electrical and Computer Engineering, Year 1, Semester 2. Course material written for your department, with the same six learning hubs in every course.",
        path = "/academy/special-packages/electrical-computer-engineering/y1-sem-2",
        assetImage = eceSemesterCover(1, 2),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("All Year 1 Semester 2 engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y2-sem-1",
        name = "ECE Year 2: Semester 1",
        shortName = "ECE Y2 S1",
        description = "Electrical and Computer Engineering, Year 2, Semester 1. Course material written for your department, with the same six learning hubs in every course.",
        path = "/academy/special-packages/electrical-computer-engineering/y2-sem-1",
        assetImage = eceSemesterCover(2, 1),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("All Year 2 Semester 1 engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y2-sem-2",
        name = "ECE Year 2: Semester 2",
        shortName = "ECE Y2 S2",
        description = "Electrical and Computer Engineering, Year 2, Semester 2. Course material written for your department, with the same six learning hubs in every course.",
        path = "/academy/special-packages/electrical-computer-engineering/y2-sem-2",
        assetImage = eceSemesterCover(2, 2),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("All Year 2 Semester 2 engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y3-sem-1",
        name = "ECE Year 3: Semester 1",
        shortName = "ECE S1",
        description = "Senior Electrical and Computer Engineering, Semester 1. Course material written for your department, not generic engineering notes.",
        path = "/academy/special-packages/electrical-computer-engineering/sem-1",
        assetImage = eceSemesterCover(3, 1),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("All 7 Year 3 Semester 1 engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y3-sem-2",
        name = "ECE Year 3: Semester 2",
        shortName = "ECE S2",
        description = "Senior Electrical and Computer Engineering, Semester 2. Same department standard as Semester 1: course-level notes, chapter questions, flashcards, and solved practice exams.",
        path = "/academy/special-packages/electrical-computer-engineering/sem-2",
        assetImage = eceSemesterCover(3, 2),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("All 7 Year 3 Semester 2 engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y4-sem-1",
        name = "ECE Year 4: Semester 1",
        shortName = "ECE Y4 S1",
        description = "Electrical and Computer Engineering, Year 4, Semester 1.",
        path = "/academy/special-packages/electrical-computer-engineering/y4-sem-1",
        assetImage = eceSemesterCover(4, 1),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("All Year 4 Semester 1 engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y4-sem-2",
        name = "ECE Year 4: Semester 2",
        shortName = "ECE Y4 S2",
        description = "Electrical and Computer Engineering, Year 4, Semester 2.",
        path = "/academy/special-packages/electrical-computer-engineering/y4-sem-2",
        assetImage = eceSemesterCover(4, 2),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("Year 4 Semester 2 streams and engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y5-sem-1",
        name = "ECE Year 5: Semester 1",
        shortName = "ECE Y5 S1",
        description = "Electrical and Computer Engineering, Year 5, Semester 1.",
        path = "/academy/special-packages/electrical-computer-engineering/y5-sem-1",
        assetImage = eceSemesterCover(5, 1),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("Year 5 Semester 1 streams and engineering courses") + CORE_INCLUDES
    ),
    NativePackage(
        id = "ece-y5-sem-2",
        name = "ECE Year 5: Semester 2",
        shortName = "ECE Y5 S2",
        description = "Electrical and Computer Engineering, Year 5, Semester 2.",
        path = "/academy/special-packages/electrical-computer-engineering/y5-sem-2",
        assetImage = eceSemesterCover(5, 2),
        enrolledLabel = "Special track",
        group = "special",
        accentColor = WisdomAccentViolet,
        includes = listOf("Year 5 Semester 2 streams and engineering courses") + CORE_INCLUDES
    )
)

// Names, descriptions and order copied exactly from website-reference/src/data/freshman.ts; covers are code-drawn.
val FRESHMAN_SUBJECTS = listOf(
    NativeSubject("math-natural", "Math Natural", "Calculus (differentiation and integration), analytic geometry, vectors, matrices, and algebraic systems for STEM disciplines.", codeCoverUri("freshman/math-natural"), "/academy/freshman/math-natural"),
    NativeSubject("math-social", "Math Social", "Algebraic foundations, linear programming, financial mathematics, descriptive and inferential statistics for social sciences.", codeCoverUri("freshman/math-social"), "/academy/freshman/math-social"),
    NativeSubject("physical-fitness", "Physical Fitness", "Health-related fitness, cardiovascular endurance, strength development, and lifetime personal wellness principles.", codeCoverUri("freshman/physical-fitness"), "/academy/freshman/physical-fitness"),
    NativeSubject("english-1", "English 1", "Academic reading strategies, complex grammatical structures, formal paragraph mechanics, and expository writing.", codeCoverUri("freshman/english-1"), "/academy/freshman/english-1"),
    NativeSubject("physics", "Physics", "Classical mechanics, vectors, motion in one and two dimensions, work-energy theorem, fluid dynamics, and thermodynamics.", codeCoverUri("freshman/physics"), "/academy/freshman/physics"),
    NativeSubject("psychology", "Psychology", "Biological bases of behavior, cognitive psychology, learning theories, motivation, personality development, and mental health.", codeCoverUri("freshman/psychology"), "/academy/freshman/psychology"),
    NativeSubject("logic", "Logic", "Formal propositions, truth tables, categorical logic, syllogistic deductions, and fallacies in argumentation.", codeCoverUri("freshman/logic"), "/academy/freshman/logic"),
    NativeSubject("geography", "Geography", "Physical landscape processes, climate dynamics, population distribution, and resource management across Ethiopia and the Horn.", codeCoverUri("freshman/geography"), "/academy/freshman/geography"),
    NativeSubject("anthropology", "Anthropology", "Human biological and cultural evolution, ethnographic methods, indigenous traditions, and societal institutions in Ethiopia.", codeCoverUri("freshman/anthropology"), "/academy/freshman/anthropology"),
    NativeSubject("civics", "Civics", "Constitutional principles, federal governance structure, human rights jurisprudence, rule of law, and civic participation.", codeCoverUri("freshman/civics"), "/academy/freshman/civics"),
    NativeSubject("economics", "Economics", "Microeconomic behavior (supply, demand, consumer theory, production costs) and macroeconomic aggregates (GDP, fiscal policy, monetary systems).", codeCoverUri("freshman/economics"), "/academy/freshman/economics"),
    NativeSubject("emerging-technology", "Emerging Technology", "Architectures and applications of AI, data science, IoT networks, cloud computing, cybersecurity, and additive manufacturing.", codeCoverUri("freshman/emerging-technology"), "/academy/freshman/emerging-technology"),
    NativeSubject("entrepreneurship", "Entrepreneurship", "Venture ideation, business model design, market validation, Ethiopian regulatory frameworks, and financial planning.", codeCoverUri("freshman/entrepreneurship"), "/academy/freshman/entrepreneurship"),
    NativeSubject("global-trends", "Global Trends", "Post-Cold War international order, regional cooperation in East Africa, global trade policies, multilateral institutions, and environmental diplomacy.", codeCoverUri("freshman/global-trends"), "/academy/freshman/global-trends"),
    NativeSubject("history", "History", "Historical trajectory of Ethiopia and the Horn of Africa: statecraft, trade networks, modernization initiatives, and historical historiography.", codeCoverUri("freshman/history"), "/academy/freshman/history"),
    NativeSubject("inclusiveness", "Inclusiveness", "Principles of universal design, disability rights, inclusive education paradigms, vulnerability assessments, and supportive public accommodations.", codeCoverUri("freshman/inclusiveness"), "/academy/freshman/inclusiveness"),
    NativeSubject("chemistry", "Chemistry", "Atomic orbital theory, molecular geometry, stoichiometry, thermodynamics, chemical kinetics, chemical equilibria, and solution chemistry.", codeCoverUri("freshman/chemistry"), "/academy/freshman/chemistry"),
    NativeSubject("biology", "Biology", "Cell architecture, bioenergetics, DNA replication, gene expression, plant and animal physiology, and ecological ecosystems.", codeCoverUri("freshman/biology"), "/academy/freshman/biology"),
    NativeSubject("cpp-programming", "C++ Programming", "Structured problem solving, procedural programming in C++, pointer arithmetic, memory allocation, arrays, and object-oriented abstractions.", codeCoverUri("freshman/cpp-programming"), "/academy/freshman/cpp-programming"),
    NativeSubject("applied-math-1", "Applied Math 1", "Applied differential equations, matrix methods, numerical approximations, and mathematical modeling for physical sciences.", codeCoverUri("freshman/applied-math-1"), "/academy/freshman/applied-math-1"),
    NativeSubject("english-2", "English 2", "Advanced research methodology, source synthesis, APA/IEEE citation standards, literature reviews, argumentative essays, and oral academic presentations.", codeCoverUri("freshman/english-2"), "/academy/freshman/english-2")
)

val FRESHMAN_NATURAL_IDS = setOf(
    "math-natural", "physics", "chemistry", "biology", "emerging-technology",
    "cpp-programming", "applied-math-1", "physical-fitness", "english-1", "english-2", "logic"
)

val FRESHMAN_SOCIAL_IDS = setOf(
    "math-social", "economics", "geography", "history", "civics",
    "psychology", "anthropology", "inclusiveness", "global-trends", "entrepreneurship",
    "english-1", "english-2", "logic"
)

val FRESHMAN_SEM1_IDS = setOf(
    "math-natural", "math-social", "physics", "chemistry", "biology",
    "english-1", "psychology", "logic", "geography", "physical-fitness"
)

val FRESHMAN_SEM2_IDS = setOf(
    "anthropology", "civics", "economics", "emerging-technology", "cpp-programming",
    "applied-math-1", "english-2", "inclusiveness", "global-trends", "entrepreneurship"
)

data class NativeLearningHub(
    val id: String,
    val name: String,
    val description: String,
    val accentColor: Color,
    val assetImage: String
)

val NATIVE_LEARNING_HUBS = listOf(
    NativeLearningHub(
        id = "books",
        name = "Books",
        description = "Core textbooks and curated reading lists",
        accentColor = WisdomAccentSky,
        assetImage = codeCoverUri("hubs/books")
    ),
    NativeLearningHub(
        id = "short-notes",
        name = "Short Notes",
        description = "Concise notes, summaries, and study guides",
        accentColor = WisdomAccentViolet,
        assetImage = codeCoverUri("hubs/short-notes")
    ),
    NativeLearningHub(
        id = "flashcards",
        name = "Flashcards",
        description = "Quick recall decks for key concepts",
        accentColor = WisdomAccentAmber,
        assetImage = codeCoverUri("hubs/flashcards")
    ),
    NativeLearningHub(
        id = "question-banks",
        name = "Question Banks",
        description = "Practice sets by topic and difficulty",
        accentColor = WisdomCyan,
        assetImage = codeCoverUri("hubs/question-banks")
    ),
    NativeLearningHub(
        id = "exams",
        name = "Exams",
        description = "Past papers, mocks, and timed drills",
        accentColor = WisdomAccentEmerald,
        assetImage = codeCoverUri("hubs/exams")
    ),
    NativeLearningHub(
        id = "life-savers",
        name = "Life Savers",
        description = "High-yield summaries, cheat sheets, and exam lifesavers",
        accentColor = WisdomAccentRose,
        assetImage = codeCoverUri("hubs/life-savers")
    )
)

data class NativeSpecialCourse(
    val code: String,
    val title: String,
    val slug: String,
    val semester: String,
    val assetImage: String,
    val path: String
)

val ECE_SEM1_COURSES = listOf(
    NativeSpecialCourse("ECEg3071", "Applied Electronics II", "eceg3071", "sem-1", codeCoverUri("course/eceg3071"), "/academy/special-packages/electrical-computer-engineering/sem-1/eceg3071"),
    NativeSpecialCourse("Econ1011", "Economics", "econ1011", "sem-1", codeCoverUri("course/econ1011"), "/academy/special-packages/electrical-computer-engineering/sem-1/econ1011"),
    NativeSpecialCourse("ECEg3051", "Electromagnetic Fields", "eceg3051", "sem-1", codeCoverUri("course/eceg3051"), "/academy/special-packages/electrical-computer-engineering/sem-1/eceg3051"),
    NativeSpecialCourse("ECEg3081", "Signals and Systems Analysis", "eceg3081", "sem-1", codeCoverUri("course/eceg3081"), "/academy/special-packages/electrical-computer-engineering/sem-1/eceg3081"),
    NativeSpecialCourse("ECEg3073", "Electrical Engineering Laboratory III", "eceg3073", "sem-1", codeCoverUri("course/eceg3073"), "/academy/special-packages/electrical-computer-engineering/sem-1/eceg3073"),
    NativeSpecialCourse("ECEg3101", "Object Oriented Programming", "eceg3101", "sem-1", codeCoverUri("course/eceg3101"), "/academy/special-packages/electrical-computer-engineering/sem-1/eceg3101"),
    NativeSpecialCourse("ECEg3061", "Computational Methods", "eceg3061", "sem-1", codeCoverUri("course/eceg3061"), "/academy/special-packages/electrical-computer-engineering/sem-1/eceg3061")
)

val ECE_SEM2_COURSES = listOf(
    NativeSpecialCourse("MEng3052", "Engineering Thermodynamics", "meng3052", "sem-2", codeCoverUri("course/meng3052"), "/academy/special-packages/electrical-computer-engineering/sem-2/meng3052"),
    NativeSpecialCourse("ECEg3082", "Network Analysis and Synthesis", "eceg3082", "sem-2", codeCoverUri("course/eceg3082"), "/academy/special-packages/electrical-computer-engineering/sem-2/eceg3082"),
    NativeSpecialCourse("ECEg3092", "Introduction to Electrical Machines", "eceg3092", "sem-2", codeCoverUri("course/eceg3092"), "/academy/special-packages/electrical-computer-engineering/sem-2/eceg3092"),
    NativeSpecialCourse("ECEg3094", "Electrical Engineering Lab IV", "eceg3094", "sem-2", codeCoverUri("course/eceg3094"), "/academy/special-packages/electrical-computer-engineering/sem-2/eceg3094"),
    NativeSpecialCourse("ECEg3102", "Digital Logic Design", "eceg3102", "sem-2", codeCoverUri("course/eceg3102"), "/academy/special-packages/electrical-computer-engineering/sem-2/eceg3102"),
    NativeSpecialCourse("ECEg3052", "Electrical Materials and Technology", "eceg3052", "sem-2", codeCoverUri("course/eceg3052"), "/academy/special-packages/electrical-computer-engineering/sem-2/eceg3052"),
    NativeSpecialCourse("ECEg3096", "Electrical Workshop Practice II", "eceg3096", "sem-2", codeCoverUri("course/eceg3096"), "/academy/special-packages/electrical-computer-engineering/sem-2/eceg3096")
)

data class NativeGradeSubject(
    val id: String,
    val name: String,
    val hint: String,
    val grade: String,
    val path: String,
    val image: String? = null
)

// Code-drawn grade subject cover identity; the filename stem preserves the grade-specific label.
private fun gradeImg(grade: String, file: String): String =
    codeCoverUri("grade-$grade/${file.substringBeforeLast('.')}")

val GRADE_9_SUBJECTS = listOf(
    NativeGradeSubject("chemistry", "Chemistry", "Matter, atoms, reactions", "9", "/academy/grades/9/chemistry", gradeImg("9", "g9-chemistry.jpg")),
    NativeGradeSubject("mathematics", "Math", "Algebra, geometry, number work", "9", "/academy/grades/9/mathematics", gradeImg("9", "g9-math.jpg")),
    NativeGradeSubject("physics", "Physics", "Motion, forces, energy", "9", "/academy/grades/9/physics", gradeImg("9", "g9-physics.jpg")),
    NativeGradeSubject("biology", "Biology", "Cells, organisms, life processes", "9", "/academy/grades/9/biology", gradeImg("9", "g9-biology.jpg")),
    NativeGradeSubject("geography", "Geography", "Earth systems and places", "9", "/academy/grades/9/geography", gradeImg("9", "g9-geography.jpg")),
    NativeGradeSubject("history", "History", "Past societies and change", "9", "/academy/grades/9/history", gradeImg("9", "g9-history.jpg")),
    NativeGradeSubject("economics", "Economics", "Resources, markets, choices", "9", "/academy/grades/9/economics", gradeImg("9", "g9-economics.jpg"))
)

val GRADE_10_SUBJECTS = listOf(
    NativeGradeSubject("mathematics", "Math", "Functions, geometry, statistics", "10", "/academy/grades/10/mathematics", gradeImg("10", "g10-math.jpg")),
    NativeGradeSubject("biology", "Biology", "Systems, ecology, health", "10", "/academy/grades/10/biology", gradeImg("10", "g10-biology.jpg")),
    NativeGradeSubject("chemistry", "Chemistry", "Bonding, stoichiometry", "10", "/academy/grades/10/chemistry", gradeImg("10", "g10-chemistry.jpg")),
    NativeGradeSubject("physics", "Physics", "Waves, electricity foundations", "10", "/academy/grades/10/physics", gradeImg("10", "g10-physics.jpg")),
    NativeGradeSubject("geography", "Geography", "Human and physical geography", "10", "/academy/grades/10/geography", gradeImg("10", "g10-geography.jpg")),
    NativeGradeSubject("history", "History", "Regional and world contexts", "10", "/academy/grades/10/history", gradeImg("10", "g10-history.jpg")),
    NativeGradeSubject("economics", "Economics", "Production and national economy", "10", "/academy/grades/10/economics", gradeImg("10", "g10-economics.jpg"))
)

val GRADE_11_SUBJECTS = listOf(
    NativeGradeSubject("mathematics", "Math", "Advanced algebra and exam path", "11", "/academy/grades/11/mathematics", gradeImg("11", "g11-math.jpeg")),
    NativeGradeSubject("biology", "Biology", "Genetics, physiology, ecology", "11", "/academy/grades/11/biology", gradeImg("11", "g11-biology.jpeg")),
    NativeGradeSubject("physics", "Physics", "Mechanics, energy, fields", "11", "/academy/grades/11/physics", gradeImg("11", "g11-physics.jpeg")),
    NativeGradeSubject("chemistry", "Chemistry", "Structure, reactions, equilibrium", "11", "/academy/grades/11/chemistry", gradeImg("11", "g11-chemistry.jpg")),
    NativeGradeSubject("economics", "Economics", "Markets, macro, development", "11", "/academy/grades/11/economics", gradeImg("11", "g11-economics.jpeg")),
    NativeGradeSubject("geography", "Geography", "Population, development, Ethiopia", "11", "/academy/grades/11/geography", gradeImg("11", "g11-geography.jpeg")),
    NativeGradeSubject("history", "History", "Ethiopia and global history", "11", "/academy/grades/11/history", gradeImg("11", "g11-history.jpeg"))
)

// Grade 12: "Exam tips" was removed from the website (commit ce89c43), so it is removed here too.
val GRADE_12_SUBJECTS = listOf(
    NativeGradeSubject("english", "English", "Leaving-exam English", "12", "/academy/grades/12/english", gradeImg("12", "g12-english.jpeg")),
    NativeGradeSubject("chemistry", "Chemistry", "Organic and physical chemistry", "12", "/academy/grades/12/chemistry", gradeImg("12", "g12-chemistry.jpeg")),
    NativeGradeSubject("biology", "Biology", "Exam depth across life sciences", "12", "/academy/grades/12/biology", gradeImg("12", "g12-biology.jpeg")),
    NativeGradeSubject("mathematics", "Math", "Exam-ready pure and applied math", "12", "/academy/grades/12/mathematics", gradeImg("12", "g12-math.jpeg")),
    NativeGradeSubject("physics", "Physics", "Electromagnetism, modern physics", "12", "/academy/grades/12/physics", gradeImg("12", "g12-physics.jpeg")),
    NativeGradeSubject("geography", "Geography", "Exam depth in geo systems", "12", "/academy/grades/12/geography", gradeImg("12", "g12-geography.jpeg")),
    NativeGradeSubject("economics", "Economics", "Exam depth in economic theory", "12", "/academy/grades/12/economics", gradeImg("12", "g12-economics.jpeg")),
    NativeGradeSubject("history", "History", "Exam depth in historical analysis", "12", "/academy/grades/12/history", gradeImg("12", "g12-history.jpeg")),
    NativeGradeSubject("sat", "SAT", "SAT-style practice and strategies", "12", "/academy/grades/12/sat", gradeImg("12", "sat.jpeg"))
)

// Ids, names and descriptions copied exactly from website-reference/src/data/remedial.ts.
private fun remedialImg(id: String): String =
    codeCoverUri("remedial/$id")

val REMEDIAL_SUBJECTS = listOf(
    NativeSubject("english", "English", "Grammar fundamentals, reading comprehension, vocabulary expansion, sentence mechanics, and introductory essay writing.", remedialImg("english"), "/academy/remedial/english"),
    NativeSubject("maths", "Maths", "Algebraic simplification, polynomials, quadratic equations, coordinate geometry, trigonometry, and logarithms.", remedialImg("maths"), "/academy/remedial/maths"),
    NativeSubject("physics", "Physics", "Kinematics, Newton's laws of motion, gravitation, work and power, introductory electrostatics, and wave properties.", remedialImg("physics"), "/academy/remedial/physics"),
    NativeSubject("chemistry", "Chemistry", "Classification of matter, periodic trends, chemical equations, stoichiometric calculations, and introductory acid-base chemistry.", remedialImg("chemistry"), "/academy/remedial/chemistry"),
    NativeSubject("biology", "Biology", "Cell structure and function, basic taxonomy, human organ systems, plant anatomy, and basic genetics.", remedialImg("biology"), "/academy/remedial/biology"),
    NativeSubject("history", "History", "Modern Ethiopian history, major political transitions, regional interactions, and key global historical milestones.", remedialImg("history"), "/academy/remedial/history"),
    NativeSubject("geography", "Geography", "Map reading skills, Ethiopian physical geography, climate zones, population demographics, and economic activities.", remedialImg("geography"), "/academy/remedial/geography")
)
