package com.wisdomtower.academy.ui.packages

/**
 * The picture and title of the ECE track as a whole (Home's "ECE Engineering" card). The two semester
 * packages ("ece-y3-sem-1" / "ece-y3-sem-2") keep their own semester pictures in the Packages tab and on the
 * semester chooser; any card that stands for the WHOLE ECE year (Learning > Curriculum) uses these so it
 * matches Home instead of repeating the Semester 1 picture.
 *
 * ECE-FIX-C2b: the picture now goes through the helper: `ece-y3.jpg` if the owner added it, else the old `ece.jpg`.
 * It is a getter (not a `const`) because the helper reads the app assets; callers use it the same way.
 * UNSURE: that this whole-track card should show the Year 3 picture (the Learning card is the Year 3 package).
 */
val ECE_TRACK_COVER_ASSET: String
    get() = eceYearCover(3)
const val ECE_TRACK_TITLE = "ECE Engineering"
