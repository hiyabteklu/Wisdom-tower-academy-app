package com.wisdomtower.academy.data.auth

import com.wisdomtower.academy.ui.packages.NativePackage

/** One academic level the student can pick (sign-up, Profile). [value] is the exact string saved in `profiles.education_level`. */
data class AcademicLevel(val value: String, val label: String, val group: String)

/**
 * A1 (docs/PLAN_2026-11-28_SIGNUP_AND_GAMES_NAV.md, 2026-11-28): the ONE place that knows the academic levels and which
 * packages a level opens. Before this there were two copies of the rule (My Learning in ui/learning/LearningScreen.kt and
 * the Games tab in ui/games/GameEnrollment.kt) and they had drifted: Games still opened the FIRST ece package, which is
 * Year 1 since ECE-FIX-C2e, for a Year 3 student. Both tabs now call [resolvePackages].
 *
 * Rules (same as before, plus the new ECE years):
 *  - Grade 9..12 -> that grade package. Remedial -> remedial. Freshman (and no level at all) -> freshman + coc.
 *  - `ECE Year N` (N 1..5) -> the package of that year (id `ece-yN-...`). The old `3rd Year (ECE)` and any other loose
 *    ece / electrical text mean Year 3 (Year 3 package first, any ece package as the old fallback).
 *  - Exit Exam, COC, GAT, EUEE, UAT, Other -> no package yet (nothing in My Learning, Games says Coming soon).
 *
 * The values must match the `education_level` CHECK in Supabase. The ECE Year 1..5, COC, EUEE and UAT values were added by
 * the A2 migration (LIVE 2026-11-28).
 * A4 (2026-11-28) added [normalize] and [isUniversityLevel] for the Profile screen.
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object AcademicLevels {
    const val GROUP_SCHOOL = "School"
    const val GROUP_UNIVERSITY = "University"
    const val GROUP_EXAMS = "Exams"
    const val GROUP_OTHER = "Other"

    /** The levels offered to a student, in display order. */
    val ALL: List<AcademicLevel> = listOf(
        AcademicLevel("Grade 9", "Grade 9", GROUP_SCHOOL),
        AcademicLevel("Grade 10", "Grade 10", GROUP_SCHOOL),
        AcademicLevel("Grade 11", "Grade 11", GROUP_SCHOOL),
        AcademicLevel("Grade 12", "Grade 12", GROUP_SCHOOL),
        AcademicLevel("Freshman", "Freshman", GROUP_UNIVERSITY),
        AcademicLevel("ECE Year 1", "ECE Year 1", GROUP_UNIVERSITY),
        AcademicLevel("ECE Year 2", "ECE Year 2", GROUP_UNIVERSITY),
        AcademicLevel("ECE Year 3", "ECE Year 3", GROUP_UNIVERSITY),
        AcademicLevel("ECE Year 4", "ECE Year 4", GROUP_UNIVERSITY),
        AcademicLevel("ECE Year 5", "ECE Year 5", GROUP_UNIVERSITY),
        AcademicLevel("Remedial", "Remedial", GROUP_EXAMS),
        AcademicLevel("Exit Exam", "Exit Exam", GROUP_EXAMS),
        AcademicLevel("COC", "COC", GROUP_EXAMS),
        AcademicLevel("GAT", "GAT", GROUP_EXAMS),
        AcademicLevel("EUEE", "EUEE", GROUP_EXAMS),
        AcademicLevel("UAT", "UAT", GROUP_EXAMS),
        AcademicLevel("Other", "Other", GROUP_OTHER)
    )

    private val ECE_YEAR = Regex("^ece\\s*year\\s*([1-5])$")

    /** 1..5 for an ECE level, null for every other level. Loose ece / electrical text (old accounts) counts as Year 3. */
    fun eceYearOf(level: String?): Int? {
        if (level.isNullOrBlank()) return null
        val t = level.trim().lowercase()
        ECE_YEAR.find(t)?.let { return it.groupValues[1].toInt() }
        if (t.contains("remedial") || t.contains("freshman")) return null
        val old = t == "3rd year (ece)" || t == "3rd year ece" || t.contains("ece") || t.contains("electrical")
        return if (old) 3 else null
    }

    /**
     * The value to show as selected on the Profile screen. The old `3rd Year (ECE)` becomes `ECE Year 3` (same package, and
     * the database accepts it since A2). A level that matches the list in another letter case gets the list spelling.
     * Anything else is returned trimmed; null stays null.
     */
    fun normalize(level: String?): String? {
        if (level == null) return null
        val t = level.trim()
        if (t.isEmpty()) return null
        ALL.firstOrNull { it.value.equals(t, ignoreCase = true) }?.let { return it.value }
        val lower = t.lowercase()
        if (lower == "3rd year (ece)" || lower == "3rd year ece") return "ECE Year 3"
        return t
    }

    /**
     * True when the student picks a university from the list (Profile screen) instead of typing a school name:
     * Freshman, every ECE year, Remedial, Exit Exam, COC and GAT. School grades, EUEE, UAT and Other type a school name.
     */
    fun isUniversityLevel(level: String?): Boolean {
        if (level.isNullOrBlank()) return false
        if (eceYearOf(level) != null) return true
        val t = level.trim().lowercase()
        return t == "freshman" || t == "remedial" || t == "exit exam" || t == "coc" || t == "gat"
    }

    /** Package ids of the non-ECE levels. No level = the website default (freshman + coc). */
    private fun plainPackageIds(level: String?): List<String> {
        if (level.isNullOrBlank()) return listOf("freshman", "coc")
        val t = level.trim().lowercase()
        return when {
            t == "grade 9" || t == "grade-9" || t == "g9" -> listOf("grade-9")
            t == "grade 10" || t == "grade-10" || t == "g10" -> listOf("grade-10")
            t == "grade 11" || t == "grade-11" || t == "g11" -> listOf("grade-11")
            t == "grade 12" || t == "grade-12" || t == "g12" -> listOf("grade-12")
            t.contains("remedial") -> listOf("remedial")
            t.contains("freshman") -> listOf("freshman", "coc")
            else -> emptyList()
        }
    }

    /** The packages this level opens, taken from [packageList] (My Learning and Games use exactly this list). */
    fun resolvePackages(level: String?, packageList: List<NativePackage>): List<NativePackage> {
        val year = eceYearOf(level)
        if (year != null) {
            val exact = packageList.firstOrNull { it.id == "ece-y$year" || it.id.startsWith("ece-y$year-") }
            // Old rule kept for Year 3 only: any ece package. Other years never fall back to a different year.
            val pkg = exact ?: if (year == 3) packageList.firstOrNull { it.id.startsWith("ece") } else null
            return listOfNotNull(pkg)
        }
        return plainPackageIds(level).mapNotNull { id -> packageList.firstOrNull { it.id == id } }
    }

    /** False for Exit Exam, COC, GAT, EUEE, UAT and Other: the Games tab shows Coming soon for them. */
    fun hasGames(level: String?): Boolean {
        if (level.isNullOrBlank()) return true
        if (eceYearOf(level) != null) return true
        return plainPackageIds(level).isNotEmpty()
    }
}
