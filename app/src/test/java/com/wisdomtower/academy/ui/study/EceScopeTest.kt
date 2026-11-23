package com.wisdomtower.academy.ui.study

import com.wisdomtower.academy.ui.packages.ECE_TREE_YEARS
import com.wisdomtower.academy.ui.packages.EceTreeCourse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ECE-FIX-A4 (docs/ADMIN_ECE_Y1_Y5_PLAN.md, Phase A step 4): every Year 1, 2, 4, 5 course path must resolve to a
 * native content scope, so a hub tap never falls through to the website page (the old 404). Year 3 must not change.
 * Plain JUnit: contentScopePath is internal and has no Android dependency.
 */
class EceScopeTest {

    private val hubs = listOf("books", "short-notes", "flashcards", "question-banks", "exams", "life-savers")

    private fun allCourses(): List<Pair<String, EceTreeCourse>> {
        val out = mutableListOf<Pair<String, EceTreeCourse>>()
        for (year in ECE_TREE_YEARS) {
            for (sem in year.semesters) {
                val semKey = "${year.id}-${sem.id}"
                sem.courses.forEach { out.add(semKey to it) }
                sem.streams.forEach { stream -> stream.courses.forEach { out.add(semKey to it) } }
            }
        }
        return out
    }

    @Test
    fun treeHasCourses() {
        assertTrue(allCourses().size > 50)
    }

    @Test
    fun everyCoursePathGivesItsNativeScope() {
        for ((semKey, course) in allCourses()) {
            val expected = "ece/$semKey/${course.id}"
            assertEquals("course ${course.code}", expected, contentScopePath(course.path))
        }
    }

    @Test
    fun everyHubOfEveryCourseGivesTheSameScope() {
        for ((semKey, course) in allCourses()) {
            val expected = "ece/$semKey/${course.id}"
            for (hub in hubs) {
                assertEquals(
                    "course ${course.code} hub $hub",
                    expected,
                    contentScopePath(course.path + "/" + hub)
                )
            }
        }
    }

    @Test
    fun year3PathsAreUnchanged() {
        val base = "/academy/special-packages/electrical-computer-engineering"
        assertEquals("ece/sem-1/eceg3041", contentScopePath("$base/sem-1/eceg3041"))
        assertEquals("ece/sem-2/eceg3042", contentScopePath("$base/sem-2/eceg3042"))
        assertEquals("ece/sem-1/eceg3041", contentScopePath("$base/sem-1/eceg3041/books"))
    }
}
