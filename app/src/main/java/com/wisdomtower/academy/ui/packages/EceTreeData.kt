package com.wisdomtower.academy.ui.packages

/**
 * ECE hierarchy model for the expansion (docs/ECE_STATUS.md, slice E1): year > semester > optional stream > course.
 * Year 3 is LOCKED and is NOT in this file; it keeps
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

val ECE_TREE_YEAR_4 = EceTreeYear(
    id = "y4",
    label = "Year 4",
    semesters = listOf(
        EceTreeSemester(
            packageId = "ece-y4-sem-1", id = "sem-1", label = "Semester 1",
            courses = listOf(
                eceTreeCourse(4, 1, "ECEg4101", "Computer Architecture and Organization"),
                eceTreeCourse(4, 1, "ECEg4111", "Introduction to Communication Systems"),
                eceTreeCourse(4, 1, "ECEg4113", "Digital Signal Processing"),
                eceTreeCourse(4, 1, "ECEg4115", "Introduction to Control Systems"),
                eceTreeCourse(4, 1, "ECEg4117", "Introduction to Power Systems"),
                eceTreeCourse(4, 1, "ECEg4045", "Electrical Engineering Lab V"),
                eceTreeCourse(4, 1, "ECEg4119", "Introduction to Instrumentation")
            )
        ),
        EceTreeSemester(
            packageId = "ece-y4-sem-2", id = "sem-2", label = "Semester 2",
            courses = emptyList(),
            streams = listOf(
                EceTreeStream("communication", "Communication", listOf(
                    eceTreeCourse(4, 2, "ECEg4270", "Research Methods and Presentation"),
                    eceTreeCourse(4, 2, "ECEg4120", "Communication Systems (Digital)"),
                    eceTreeCourse(4, 2, "ECEg4180", "Data Communication and Computer Networks"),
                    eceTreeCourse(4, 2, "ECEg4154", "Microcomputers and Interfacing"),
                    eceTreeCourse(4, 2, "ECEg4140", "EM Waves and Guide Structures"),
                    eceTreeCourse(4, 2, "ECEg4250", "Microelectronic Devices and Circuits"),
                    eceTreeCourse(4, 2, "ECEg4142", "Antennas and Radio Wave Propagation")
                )),
                EceTreeStream("computer", "Computer", listOf(
                    eceTreeCourse(4, 2, "ECEg4154", "Microcomputers and Interfacing"),
                    eceTreeCourse(4, 2, "ECEg4160", "Data Structures"),
                    eceTreeCourse(4, 2, "ECEg4170", "Software Engineering"),
                    eceTreeCourse(4, 2, "ECEg4182", "Computer and Network Security"),
                    eceTreeCourse(4, 2, "ECEg4270", "Research Methods and Presentation"),
                    eceTreeCourse(4, 2, "ECEg4180", "Data Communication and Computer Networks"),
                    eceTreeCourse(4, 2, "ECEg4150", "Embedded Systems")
                )),
                EceTreeStream("control", "Control", listOf(
                    eceTreeCourse(4, 2, "ECEg4190", "Modern Control Systems"),
                    eceTreeCourse(4, 2, "ECEg4154", "Microcomputers and Interfacing"),
                    eceTreeCourse(4, 2, "ECEg4150", "Embedded Systems"),
                    eceTreeCourse(4, 2, "ECEg4220", "Electrical Machines"),
                    eceTreeCourse(4, 2, "ECEg4222", "Electrical Installation"),
                    eceTreeCourse(4, 2, "ECEg4270", "Research Methods and Presentation"),
                    eceTreeCourse(4, 2, "ECEg4260", "Power Electronics and Electrical Drives")
                )),
                EceTreeStream("power", "Power", listOf(
                    eceTreeCourse(4, 2, "ECEg4220", "Electrical Machines"),
                    eceTreeCourse(4, 2, "ECEg4260", "Power Electronics and Electrical Drives"),
                    eceTreeCourse(4, 2, "ECEg4222", "Electrical Installation"),
                    eceTreeCourse(4, 2, "ECEg4210", "Power Systems"),
                    eceTreeCourse(4, 2, "ECEg4270", "Research Methods and Presentation"),
                    eceTreeCourse(4, 2, "ECEg4154", "Microcomputers and Interfacing"),
                    eceTreeCourse(4, 2, "ECEg4212", "Power System Operation and Control")
                )),
                EceTreeStream("electronics", "Electronics", listOf(
                    eceTreeCourse(4, 2, "ECEg4240", "Principles of Electronic Design"),
                    eceTreeCourse(4, 2, "ECEg4250", "Microelectronic Devices and Circuits"),
                    eceTreeCourse(4, 2, "ECEg4262", "Integrated Circuit Technology"),
                    eceTreeCourse(4, 2, "ECEg4154", "Microcomputers and Interfacing"),
                    eceTreeCourse(4, 2, "ECEg4270", "Research Methods and Presentation"),
                    eceTreeCourse(4, 2, "ECEg4140", "EM Waves and Guide Structures"),
                    eceTreeCourse(4, 2, "ECEg4122", "Optoelectronics")
                ))
            )
        )
    )
)

val ECE_TREE_YEAR_5 = EceTreeYear(
    id = "y5",
    label = "Year 5",
    semesters = listOf(
        EceTreeSemester(
            packageId = "ece-y5-sem-1", id = "sem-1", label = "Semester 1",
            courses = emptyList(),
            streams = listOf(
                EceTreeStream("communication", "Communication", listOf(
                    eceTreeCourse(5, 1, "ECEg5271", "Industry Internship"),
                    eceTreeCourse(5, 1, "MEng5402", "Industrial Management and Engineering Economy"),
                    eceTreeCourse(5, 1, "ECEg5141", "Telecommunication and Switching Networks")
                )),
                EceTreeStream("computer", "Computer", listOf(
                    eceTreeCourse(5, 1, "ECEg5271", "Industry Internship"),
                    eceTreeCourse(5, 1, "MEng5402", "Industrial Management and Engineering Economy"),
                    eceTreeCourse(5, 1, "ECEg5162", "Database Systems"),
                    eceTreeCourse(5, 1, "ECEg5242", "VLSI Design"),
                    eceTreeCourse(5, 1, "ECEg5171", "Introduction to Compiler")
                )),
                EceTreeStream("control", "Control", listOf(
                    eceTreeCourse(5, 1, "ECEg5271", "Industry Internship"),
                    eceTreeCourse(5, 1, "MEng5402", "Industrial Management and Engineering Economy"),
                    eceTreeCourse(5, 1, "ECEg5191", "Neural Network and Fuzzy Logic Control")
                )),
                EceTreeStream("power", "Power", listOf(
                    eceTreeCourse(5, 1, "ECEg5271", "Industry Internship"),
                    eceTreeCourse(5, 1, "MEng5402", "Industrial Management and Engineering Economy"),
                    eceTreeCourse(5, 1, "ECEg5231", "Power System Planning and Management")
                )),
                EceTreeStream("electronics", "Electronics", listOf(
                    eceTreeCourse(5, 1, "ECEg5271", "Industry Internship"),
                    eceTreeCourse(5, 1, "MEng5402", "Industrial Management and Engineering Economy"),
                    eceTreeCourse(5, 1, "ECEg5241", "Analog System Design")
                ))
            )
        ),
        EceTreeSemester(
            packageId = "ece-y5-sem-2", id = "sem-2", label = "Semester 2",
            courses = emptyList(),
            streams = listOf(
                EceTreeStream("communication", "Communication", listOf(
                    eceTreeCourse(5, 2, "ECEg5272", "B.Sc. Project"),
                    eceTreeCourse(5, 2, "ECEg5132", "Microwave Devices and Systems"),
                    eceTreeCourse(5, 2, "ECEg5130", "Wireless and Mobile Communication"),
                    eceTreeCourse(5, 2, "ECEg5122", "Optics and Optical Communication")
                )),
                EceTreeStream("computer", "Computer", listOf(
                    eceTreeCourse(5, 2, "ECEg5272", "B.Sc. Project"),
                    eceTreeCourse(5, 2, "ECEg5170", "Operating Systems"),
                    eceTreeCourse(5, 2, "ECEg5172", "Algorithm Analysis and Design"),
                    eceTreeCourse(5, 2, "ECEg5202", "Introduction to Robotics"),
                    eceTreeCourse(5, 2, "ECEg5162", "Database Systems"),
                    eceTreeCourse(5, 2, "ECEg5242", "VLSI Design"),
                    eceTreeCourse(5, 2, "ECEg5171", "Introduction to Compiler")
                )),
                EceTreeStream("control", "Control", listOf(
                    eceTreeCourse(5, 2, "ECEg5272", "B.Sc. Project"),
                    eceTreeCourse(5, 2, "ECEg5192", "Instrumentation Engineering"),
                    eceTreeCourse(5, 2, "ECEg5194", "Digital Control Systems"),
                    eceTreeCourse(5, 2, "ECEg5204", "Industrial Process Control and Automation"),
                    eceTreeCourse(5, 2, "ECEg5202", "Introduction to Robotics")
                )),
                EceTreeStream("power", "Power", listOf(
                    eceTreeCourse(5, 2, "ECEg5272", "B.Sc. Project"),
                    eceTreeCourse(5, 2, "ECEg5220", "Energy Conversion and Rural Electrification"),
                    eceTreeCourse(5, 2, "ECEg5214", "Power System Protection"),
                    eceTreeCourse(5, 2, "ECEg4190", "Modern Control Systems"),
                    eceTreeCourse(5, 2, "ECEg5232", "Introduction to Power System Automation")
                )),
                EceTreeStream("electronics", "Electronics", listOf(
                    eceTreeCourse(5, 2, "ECEg5272", "B.Sc. Project"),
                    eceTreeCourse(5, 2, "ECEg5242", "VLSI Design"),
                    eceTreeCourse(5, 2, "ECEg5252", "High Frequency Electronic Devices and Systems"),
                    eceTreeCourse(5, 2, "ECEg5244", "Digital System Design"),
                    eceTreeCourse(5, 2, "ECEg5264", "Power Electronics Technology and Applications"),
                    eceTreeCourse(5, 2, "ECEg5262", "PCB Design")
                ))
            )
        )
    )
)

val ECE_TREE_YEARS = listOf(ECE_TREE_YEAR_1, ECE_TREE_YEAR_2, ECE_TREE_YEAR_4, ECE_TREE_YEAR_5)
