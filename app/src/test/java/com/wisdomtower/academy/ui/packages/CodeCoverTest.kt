package com.wisdomtower.academy.ui.packages

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeCoverTest {

    @Test
    fun groupedCodeCoverIdsResolveToLabels() {
        assertEquals("APPLIED MATH", codeCoverText("code-cover://freshman/applied-math-1"))
        assertEquals("MATERIALS", codeCoverText("code-cover://course/eceg3052"))
        assertEquals("ALGEBRA\nGRADE 9", codeCoverText("code-cover://grade-9/g9-algebra"))
    }

    @Test
    fun legacyThumbnailPathsRemainRecognizedWithoutBundledImages() {
        assertEquals(
            "GRADE 9-12",
            codeCoverText("file:///android_asset/images/packages/grade-9-12_9842aa.jpeg")
        )
        assertEquals(null, codeCoverText("file:///android_asset/images/home/academy.jpg"))
    }
}
