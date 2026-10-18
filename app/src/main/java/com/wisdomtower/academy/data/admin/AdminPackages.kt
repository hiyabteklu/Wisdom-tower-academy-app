package com.wisdomtower.academy.data.admin

/** Package id the website uses for full catalog access (website-reference/src/lib/access-grants.ts, ALL_PACKAGES_ID). */
const val ALL_PACKAGES_ID = "__all__"

/** One sellable package: id and display name only. No price is kept here, so no number can go stale. */
class AdminPackage(val id: String, val name: String)

/**
 * ADMIN-native N4. Copy of academyPackages (ids and names) in website-reference/src/data/packages.ts, in the same
 * order. Used by the Grant Package screen and by grant-all. HYPOTHESIS: when a package is added or renamed on the
 * website this list must be updated by hand (a later slice, N6 Pricing and Catalog, may read the catalog instead).
 */
val ADMIN_PACKAGES: List<AdminPackage> = listOf(
    AdminPackage("grade-9", "Grade 9 Package"),
    AdminPackage("grade-10", "Grade 10 Package"),
    AdminPackage("grade-11", "Grade 11 Package"),
    AdminPackage("grade-12", "Grade 12 Package"),
    AdminPackage("freshman", "Freshman Package"),
    AdminPackage("uat", "UAT Package"),
    AdminPackage("gat", "GAT Package"),
    AdminPackage("coc", "COC Package"),
    AdminPackage("exit-exam", "Exit Exam Package"),
    AdminPackage("remedial", "Remedial Package"),
    AdminPackage("ece-y3-sem-1", "ECE Year 3: Semester 1"),
    AdminPackage("ece-y3-sem-2", "ECE Year 3: Semester 2")
)
