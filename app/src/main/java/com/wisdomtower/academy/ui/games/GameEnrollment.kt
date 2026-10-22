package com.wisdomtower.academy.ui.games

import com.wisdomtower.academy.ui.packages.CATALOG_PACKAGES
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
 * This is the SAME rule as `defaultPackageIdsForLevel` + `enrolledCourses` in ui/learning/LearningScreen.kt
 * (website `getDefaultPackagesForAcademicLevel`). Keep the two in sync. A student only sees the packages that match
 * the academic level chosen at registration / in Settings, never the whole catalog.
 */
private fun gamePackageIdsForLevel(level: String?): List<String> {
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

/** The packages the student is enrolled in, in the same order and with the same ECE rule as My Learning. */
fun enrolledGamePackages(level: String?, packageList: List<NativePackage>): List<NativePackage> =
    gamePackageIdsForLevel(level).mapNotNull { id ->
        if (id == "ece") packageList.firstOrNull { it.id.startsWith("ece") }
        else packageList.firstOrNull { it.id == id }
    }

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
