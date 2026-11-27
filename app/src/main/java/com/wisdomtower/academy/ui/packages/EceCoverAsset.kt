package com.wisdomtower.academy.ui.packages

/** ECE covers are rendered from their identity; no cover image is loaded from assets or the network. */
fun eceYearCover(year: Int): String = codeCoverUri("ece-y$year")

/** Semester covers retain year-specific labels without shipping image files. */
fun eceSemesterCover(year: Int, sem: Int): String = codeCoverUri("ece-y$year-sem-$sem")
