package com.wisdomtower.academy.data.content

/**
 * TELEGRAM BUILD ONLY (`docs/TELEGRAM_PAID_BUILD_PLAN.md`, slice P3): what the "Package required" card needs to send a
 * student to the website checkout. Nothing here is used in the Play build ([com.wisdomtower.academy.BuildConfig.PAID_UI]
 * false): the card never asks for it.
 *
 * [scope] and [opener] are set by `StudyWorkspaceScreen` while a course page is on screen (the card is shown from that
 * screen and from the hub list it hosts) and cleared when it leaves. [opener] opens a website page the same way the
 * course page opens a hub on the website, so the student stays signed in.
 *
 * STATUS: written, NOT compiled, NOT seen on a phone.
 */
object PaidCheckout {
    const val SITE_ORIGIN = "https://www.wisdom-tower-academy.live"
    const val ORDERS_URL = "$SITE_ORIGIN/orders"

    /** Lock scope path of the course on screen (for example `freshman/physics`), or null. */
    @Volatile
    var scope: String? = null

    /** Opens a website URL inside the app. Null when no course page is on screen. */
    @Volatile
    var opener: ((String) -> Unit)? = null

    fun checkoutUrl(packageId: String): String = "$SITE_ORIGIN/checkout/$packageId"

    /**
     * Catalog package id (`catalog_items.id`) that unlocks the course at lock [scope], or null when the scope has no
     * package. Same mapping as `ContentAccess.inferPackageAndScope`, except that an ECE semester maps to ITS OWN
     * semester package (the course page of semester 2 must sell semester 2).
     * 2026-11-05: "exit-exam" added (docs/PLAN_2026-11-05_PACKAGES_WIRING.md).
     */
    fun packageIdForScope(scope: String): String? {
        val parts = scope.split('/')
        return when (parts.firstOrNull()) {
            "freshman" -> "freshman"
            "grade" -> parts.getOrNull(1)?.takeIf { it in setOf("9", "10", "11", "12") }?.let { "grade-$it" }
            "ece" -> when (parts.getOrNull(1)) {
                "sem-1" -> "ece-y3-sem-1"
                "sem-2" -> "ece-y3-sem-2"
                else -> null
            }
            "coc" -> "coc"
            "uat" -> "uat"
            "gat" -> "gat"
            "exit-exam" -> "exit-exam"
            "remedial" -> "remedial"
            "entrance" -> "entrance-questions"
            else -> null
        }
    }
}
