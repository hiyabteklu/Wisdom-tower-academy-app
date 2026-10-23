package com.wisdomtower.academy.data.content

/**
 * TELEGRAM BUILD ONLY (S2, `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md`): the accounts a student can pay to, copied from the
 * website `data/packages.ts` (`paymentMethods`, the entry "other" is left out: it has no account). Same texts, same
 * numbers as the website. Logos are packed in `assets/images/banks/` (see `scripts/patch_bank_logos.py`); without
 * the files the screen shows initials instead.
 *
 * STATUS: written, NOT compiled, NOT seen on a phone.
 */
object PaymentMethods {
    data class Method(
        val id: String,
        val name: String,
        val shortLabel: String,
        val accountLabel: String,
        val accountValue: String,
        val accountName: String,
        val logoAsset: String
    )

    private const val ACCOUNT_NAME = "Hiyab Teklu"

    val all: List<Method> = listOf(
        Method("telebirr", "Telebirr", "Telebirr", "Telebirr number", "0900763030", ACCOUNT_NAME, "images/banks/telebirr.png"),
        Method("cbe", "Commercial Bank of Ethiopia (CBE)", "CBE", "CBE account", "1000665070654", ACCOUNT_NAME, "images/banks/cbe.png"),
        Method("abyssinia", "Bank of Abyssinia", "Abyssinia", "Abyssinia account", "211958545", ACCOUNT_NAME, "images/banks/abyssinia.png")
    )

    /** Inquiries line (owner, 2026-10-23). */
    const val INQUIRIES = "Inquiries: 0900763030 or Telegram @hiyab_t"

    /** Package names of the website `academyPackages` (the order row needs a name). Unknown id: the id itself. */
    fun packageName(packageId: String): String = when (packageId) {
        "grade-9" -> "Grade 9 Package"
        "grade-10" -> "Grade 10 Package"
        "grade-11" -> "Grade 11 Package"
        "grade-12" -> "Grade 12 Package"
        "freshman" -> "Freshman Package"
        "uat" -> "UAT Package"
        "gat" -> "GAT Package"
        "coc" -> "COC Package"
        "exit-exam" -> "Exit Exam Package"
        "remedial" -> "Remedial Package"
        "ece-y3-sem-1" -> "ECE Year 3: Semester 1"
        "ece-y3-sem-2" -> "ECE Year 3: Semester 2"
        else -> packageId
    }
}
