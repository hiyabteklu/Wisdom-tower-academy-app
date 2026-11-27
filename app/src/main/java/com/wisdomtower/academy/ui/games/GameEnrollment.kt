package com.wisdomtower.academy.ui.games

import com.wisdomtower.academy.data.auth.AcademicLevels
import com.wisdomtower.academy.ui.packages.ECE_SEM1_COURSES
import com.wisdomtower.academy.ui.packages.ECE_SEM2_COURSES
import com.wisdomtower.academy.ui.packages.FRESHMAN_SUBJECTS
import com.wisdomtower.academy.ui.packages.GRADE_10_SUBJECTS
import com.wisdomtower.academy.ui.packages.GRADE_11_SUBJECTS
import com.wisdomtower.academy.ui.packages.GRADE_12_SUBJECTS
import com.wisdomtower.academy.ui.packages.GRADE_9_SUBJECTS
import com.wisdomtower.academy.ui.packages.NativePackage
import com.wisdomtower.academy.ui.packages.REMEDIAL_SUBJECTS

/**
 * Game tab = same courses as the My Learning tab (owner order 2026-10-22).
 *
 * 2026-11-28 (A1, docs/PLAN_2026-11-28_SIGNUP_AND_GAMES_NAV.md): the rule now lives in ONE place,
 * [AcademicLevels.resolvePackages] (data/auth), used by this tab and by ui/learning/LearningScreen.kt. A student only sees
 * the packages that match the academic level chosen at registration / in Settings, never the whole catalog.
 * This also fixes the ECE card: it used to open the FIRST ece package (Year 1) for a Year 3 student.
 */

/** The packages the student is enrolled in, exactly the same list as My Learning. */
fun enrolledGamePackages(level: String?, packageList: List<NativePackage>): List<NativePackage> =
    AcademicLevels.resolvePackages(level, packageList)

/**
 * Thumbnail of a subject row in the game hub: the same picture the package page uses for that subject.
 * Falls back to the package picture, so there is always the same artwork from course to chapter list.
 */
fun gameSubjectImage(pkg: NativePackage, scopePath: String): String {
    val last = scopePath.substringAfterLast('/')
    val found: String? = when {
        pkg.id == "grade-9" -> GRADE_9_SUBJECTS.firstOrNull { it.id == last }?.image
        pkg.id == "grade-10" -> GRADE_10_SUBJECTS.firstOrNull { it.id == last }?.image
        pkg.id == "grade-11" -> GRADE_11_SUBJECTS.firstOrNull { it.id == last }?.image
        pkg.id == "grade-12" -> GRADE_12_SUBJECTS.firstOrNull { it.id == last }?.image
        pkg.id == "freshman" -> FRESHMAN_SUBJECTS.firstOrNull { it.id == last }?.assetImage
        pkg.id == "remedial" ->
            REMEDIAL_SUBJECTS.firstOrNull { it.id == last || (last == "mathematics" && it.id == "maths") }?.assetImage
        pkg.id.startsWith("ece") ->
            (ECE_SEM1_COURSES + ECE_SEM2_COURSES).firstOrNull { it.slug == last }?.assetImage
        else -> null
    }
    return found ?: pkg.assetImage
}
