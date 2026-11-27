package com.wisdomtower.academy.ui.packages

import android.content.Context
import com.wisdomtower.academy.WisdomTowerApplication

/**
 * ECE-FIX-C1 (docs/ADMIN_ECE_Y1_Y5_PLAN.md, Phase C): picks the cover picture of an ECE year card or semester cover.
 * The NEW file name is used when it exists in the app assets (`images/special-packages/`, copied there by the Gradle
 * task `copySpecialPackageImages`); otherwise the OLD file is used (`ece.jpg`, `ece-sem-1.jpg`, `ece-sem-2.jpg`, which
 * stay in place and are never deleted). So a missing new picture never shows a broken image.
 *
 * The asset folder is listed ONCE and kept in memory. If the listing fails, the old file is used.
 * File names: see docs/WEBSITE_ECE_IMAGES.md.
 */
private const val ECE_COVER_DIR = "images/special-packages"
private const val ECE_COVER_URL_BASE = "file:///android_asset/images/special-packages/"

private val eceCoverNames: Set<String> by lazy {
    runCatching {
        WisdomTowerApplication.instance.assets.list(ECE_COVER_DIR)?.toSet()
    }.getOrNull() ?: emptySet()
}

/** `file:///android_asset/...` address of [newName] if it exists in the app assets, else of [oldName]. */
@Suppress("UNUSED_PARAMETER")
fun eceCoverAsset(context: Context, newName: String, oldName: String): String =
    ECE_COVER_URL_BASE + if (newName in eceCoverNames) newName else oldName

/** Same without a Context (uses the application), for static lists such as `CATALOG_PACKAGES`. */
fun eceCoverAsset(newName: String, oldName: String): String =
    ECE_COVER_URL_BASE + if (newName in eceCoverNames) newName else oldName

/**
 * Year card cover. CODE COVERS (2026-11-27, docs/CODE_COVERS.md): Years 1 to 5 are drawn by code ([CodeCover],
 * `code-cover://ece-y<year>`), no picture file at all. [OpenCardLayout] recognises that address. Any other year number
 * keeps the old picture. To go back to the pictures, return the `eceCoverAsset(...)` line for every year again.
 */
fun eceYearCover(year: Int): String =
    if (year in 1..5) codeCoverUri("ece-y$year") else eceCoverAsset("ece-y$year.jpg", "ece.jpg")

/** Semester / package cover: `ece-y<year>-sem-<sem>.jpg`, falling back to the old `ece-sem-<sem>.jpg`. */
fun eceSemesterCover(year: Int, sem: Int): String =
    eceCoverAsset("ece-y$year-sem-$sem.jpg", "ece-sem-$sem.jpg")
