package com.wisdomtower.academy.ui.packages

/**
 * ECE Years 1, 2, 4, 5 course and stream covers (owner request 2026-11-27): these cards must look exactly like the
 * Year 3 cards (16:9 code cover + title + Open button), not like narrow rows.
 *
 * No new drawing code: the cover is a normal `code-cover://<id>` address. [codeCoverText] turns the id into the
 * title (hyphens become spaces, capitals), so the label "LAB I" is passed as `code-cover://lab-i`. Same rule as
 * Year 3 (`COURSE_LABELS` in CodeCover.kt): one simple word (or two) per subject, NO course code on the cover.
 * The full course name stays under the card, as on Year 3. A course missing from the table falls back to its own title.
 *
 * Key = lowercase course code (the same id as `EceTreeCourse.id`).
 */
private val ECE_COURSE_COVER_LABELS: Map<String, String> = mapOf(
    // Year 1, Semester 1
    "flen1011" to "English I",
    "loct1011" to "Critical Thinking",
    "math1011" to "Natural Math",
    "phys1011" to "Physics",
    "gees1011" to "Geography",
    "psch1011" to "Psychology",
    "spsc1011" to "Fitness",
    // Year 1, Semester 2
    "flen1012" to "English II",
    "anth1012" to "Anthropology",
    "math1042" to "Applied Math I",
    "mgmt1012" to "Entrepreneurship",
    "emte1012" to "Emerging Tech",
    "mcie1012" to "Civics",
    "eceg1052" to "Programming",
    "hist1012" to "History",
    // Year 2, Semester 1
    "eceg2041" to "Electrical Fundamentals",
    "eceg2043" to "Lab I",
    "meng2061" to "Drawing",
    "ceng2051" to "Mechanics I",
    "math2061" to "Applied Math II",
    "eceg2061" to "Probability",
    "snie1012" to "Inclusiveness",
    // Year 2, Semester 2
    "math2062" to "Applied Math III",
    "meng2052" to "Mechanics II",
    "eceg2070" to "Electronics I",
    "eceg2052" to "Modern Physics",
    "gltr1012" to "Global Trend",
    "eceg2072" to "Lab II",
    "eceg2042" to "Workshop I",
    // Year 4, Semester 1
    "eceg4101" to "Computer Architecture",
    "eceg4111" to "Comm Systems",
    "eceg4113" to "DSP",
    "eceg4115" to "Control Systems",
    "eceg4117" to "Power Intro",
    "eceg4045" to "Lab V",
    "eceg4119" to "Instrumentation",
    // Year 4, Semester 2 (streams)
    "eceg4270" to "Research Methods",
    "eceg4120" to "Digital Communication",
    "eceg4180" to "Computer Networks",
    "eceg4154" to "Microcomputers",
    "eceg4140" to "EM Waves",
    "eceg4250" to "Microelectronics",
    "eceg4142" to "Antennas",
    "eceg4160" to "Data Structures",
    "eceg4170" to "Software Engineering",
    "eceg4182" to "Network Security",
    "eceg4150" to "Embedded Systems",
    "eceg4190" to "Modern Control",
    "eceg4220" to "Machines",
    "eceg4222" to "Installation",
    "eceg4260" to "Power Electronics",
    "eceg4210" to "Power Systems",
    "eceg4212" to "Power Operation",
    "eceg4240" to "Electronic Design",
    "eceg4262" to "IC Technology",
    "eceg4122" to "Optoelectronics",
    // Year 5, Semester 1 (streams)
    "eceg5271" to "Internship",
    "meng5402" to "Industrial Management",
    "eceg5141" to "Telecom Switching",
    "eceg5162" to "Databases",
    "eceg5242" to "VLSI",
    "eceg5171" to "Compilers",
    "eceg5191" to "Neural Networks",
    "eceg5231" to "Power Planning",
    "eceg5241" to "Analog Design",
    // Year 5, Semester 2 (streams)
    "eceg5272" to "Project",
    "eceg5132" to "Microwaves",
    "eceg5130" to "Wireless",
    "eceg5122" to "Optics",
    "eceg5170" to "Operating Systems",
    "eceg5172" to "Algorithms",
    "eceg5202" to "Robotics",
    "eceg5192" to "Instrumentation",
    "eceg5194" to "Digital Control",
    "eceg5204" to "Process Control",
    "eceg5220" to "Rural Energy",
    "eceg5214" to "Protection",
    "eceg5232" to "Power Automation",
    "eceg5252" to "High Frequency",
    "eceg5244" to "Digital Design",
    "eceg5264" to "Power Tech",
    "eceg5262" to "PCB Design"
)

private fun coverIdOf(label: String): String =
    label.trim().lowercase().replace(Regex("\\s+"), "-")

/** Code cover address for the course with this id (lowercase code) and full title. */
fun eceCourseCover(courseId: String, title: String): String =
    codeCoverUri(coverIdOf(ECE_COURSE_COVER_LABELS[courseId.lowercase()] ?: title))

/** Code cover address for a stream card ("Communication", "Computer", ...). */
fun eceStreamCover(streamLabel: String): String = codeCoverUri(coverIdOf(streamLabel))
