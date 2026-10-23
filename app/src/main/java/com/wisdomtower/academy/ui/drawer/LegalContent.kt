package com.wisdomtower.academy.ui.drawer

/**
 * Privacy Policy and Terms of Service shown in the app drawer.
 * Version 3.3, last revised 24 October 2026 (owner request: plainer professional wording, no payment topics).
 * The website pages (website-reference/src/app/privacy/page.tsx and terms/page.tsx) still carry version 3.2:
 * copy this text to the website so both say the same thing.
 */
sealed class LegalBlock {
    /** A paragraph; [lead] is printed in bold in front of [text]. */
    data class Para(val text: String, val lead: String = "", val italic: Boolean = false) : LegalBlock()

    /** Bullet list; each item is (bold lead, text), the lead may be empty. */
    data class Bullets(val items: List<Pair<String, String>>) : LegalBlock()

    /** A small boxed note with a title and a text. */
    data class Card(val title: String, val text: String) : LegalBlock()

    /** Contact box; each link is (prefix, label, uri). */
    data class Contact(
        val title: String,
        val lines: List<String>,
        val links: List<Triple<String, String, String>>
    ) : LegalBlock()
}

data class LegalSection(val number: String, val title: String, val blocks: List<LegalBlock>)

data class LegalDocument(
    val badge: String,
    val effective: String,
    val title: String,
    val intro: String,
    val meta: String,
    val summaryTitle: String,
    val summaryItems: List<Pair<String, String>>,
    val sections: List<LegalSection>
)

val PRIVACY_DOCUMENT = LegalDocument(
    badge = "Your Privacy",
    effective = "Effective: Academic Year 2026/2027",
    title = "Privacy Policy",
    intro = "Wisdom Tower Academy respects the privacy of every student who uses it. This policy explains what information we collect, why we collect it, how we protect it, and the choices you have.",
    meta = "Policy version: 3.3 • Last revised: 24 October 2026",
    summaryTitle = "The Short Version",
    summaryItems = listOf(
        "We do not sell your data:" to "We never sell or rent your personal or academic information to advertisers or other third parties.",
        "Protected in transit and storage:" to "Connections are encrypted, and each student can only reach their own records.",
        "Your progress is yours:" to "You can view, correct or delete your information at any time.",
        "Extra care for younger students:" to "Learners in Grades 9–12 are covered by additional safeguards."
    ),
    sections = listOf(
        LegalSection(
            "01", "Who We Are", listOf(
                LegalBlock.Para("Wisdom Tower Academy (“we”, “our” or “us”) provides the Wisdom Tower Academy app and website at wisdom-tower-academy.live. We are responsible for the personal information described in this policy."),
                LegalBlock.Para("If you have a question about this policy or want to use any of your rights, write to us at support@wisdomtower.tech.")
            )
        ),
        LegalSection(
            "02", "Information We Collect", listOf(
                LegalBlock.Para("We collect only what we need to run your account and your learning experience:"),
                LegalBlock.Card("Account information", "Your name, email address, a unique profile identifier and your password in protected (hashed) form. If you sign in with Google, we receive your name, email address and profile picture. We never see your Google password."),
                LegalBlock.Card("Learning activity", "Chapters you complete, answers and scores in quizzes and exams, study streaks, study planner entries, flashcard ratings, and your results on leaderboards if you choose to take part."),
                LegalBlock.Card("Device and usage information", "Your device type, operating system, app version and an approximate region based on your IP address. Study content you download is stored on your device so you can use it offline.")
            )
        ),
        LegalSection(
            "03", "How We Use Your Information", listOf(
                LegalBlock.Para("We use your information to:"),
                LegalBlock.Bullets(
                    listOf(
                        "Provide the service:" to "sign you in, keep your progress, calculate scores and show your results.",
                        "Improve the platform:" to "understand which content helps students most, and find and fix errors.",
                        "Keep the platform safe:" to "prevent misuse, protect accounts and stop unauthorised copying of our content.",
                        "Stay in touch:" to "send service notices, and optional updates if you have agreed to receive them."
                    )
                ),
                LegalBlock.Para("Features such as leaderboards and optional surveys are used only if you choose to take part.")
            )
        ),
        LegalSection(
            "04", "How We Protect Your Information", listOf(
                LegalBlock.Para("We use reasonable technical and organisational measures to protect your information:"),
                LegalBlock.Bullets(
                    listOf(
                        "Encryption in transit:" to "all traffic between the app, the website and our servers uses HTTPS.",
                        "Separated records:" to "our database applies row-level security, so each student can only access their own records.",
                        "Limited staff access:" to "only authorised staff can reach administrative tools, based on their role, and their actions are logged."
                    )
                ),
                LegalBlock.Para("No online service can be guaranteed to be completely secure, but we take protecting your information seriously.")
            )
        ),
        LegalSection(
            "05", "Service Providers", listOf(
                LegalBlock.Para("We work with a small number of providers who process information on our behalf and are bound to keep it confidential:"),
                LegalBlock.Bullets(
                    listOf(
                        "Supabase:" to "database, sign-in and secure file storage.",
                        "Vercel and Google Cloud:" to "hosting and delivery of the website and app content.",
                        "Google (Gemini API):" to "processes the question text when you use the optional explanation feature. Only the question content is sent, never your name or contact details."
                    )
                )
            )
        ),
        LegalSection(
            "06", "Offline Content", listOf(
                LegalBlock.Para("To help when your connection is slow or expensive, the app and website can keep downloaded notes, flashcards and reading progress on your device. This information stays on your device and you can remove it at any time by clearing the app data or your browser data.")
            )
        ),
        LegalSection(
            "07", "Your Rights", listOf(
                LegalBlock.Para("You are in control of your information. You can:"),
                LegalBlock.Bullets(
                    listOf(
                        "See it:" to "review your profile, enrolled courses and results in your account.",
                        "Correct it:" to "update your name and study preferences at any time.",
                        "Delete it:" to "ask us to permanently delete your account and learning history.",
                        "Opt out of leaderboards:" to "turn off leaderboard visibility so your scores stay private."
                    )
                )
            )
        ),
        LegalSection(
            "08", "Students Under 18", listOf(
                LegalBlock.Para("Many of our learners are in Grades 9 to 12 and may be under 18. For them we:"),
                LegalBlock.Bullets(
                    listOf(
                        "" to "do not show contact details or social media handles on leaderboards.",
                        "" to "allow parents and guardians to ask to see or change their child’s records by contacting our support team.",
                        "" to "keep the platform free of advertising and do not build advertising profiles."
                    )
                )
            )
        ),
        LegalSection(
            "09", "How Long We Keep Information", listOf(
                LegalBlock.Para("We keep your account and learning history for as long as your account is active, so your progress carries over from one semester to the next. Accounts that have been inactive for two years may be deleted after we notify you by email. You can ask us to delete your account at any time.")
            )
        ),
        LegalSection(
            "10", "Changes to This Policy", listOf(
                LegalBlock.Para("We may update this policy from time to time. The version number and date at the top show when it was last changed. If we make an important change, we will tell you in the app or by email.")
            )
        ),
        LegalSection(
            "11", "Contact Us", listOf(
                LegalBlock.Para("For any question or request about your information, contact us:"),
                LegalBlock.Contact(
                    title = "Wisdom Tower Academy · Privacy",
                    lines = listOf("Addis Ababa, Ethiopia"),
                    links = listOf(
                        Triple("Email: ", "support@wisdomtower.tech", "mailto:support@wisdomtower.tech"),
                        Triple("Telegram: ", "@wisdom_tower2", "https://t.me/wisdom_tower2")
                    )
                )
            )
        )
    )
)

val TERMS_DOCUMENT = LegalDocument(
    badge = "Terms",
    effective = "Effective: Academic Year 2026/2027",
    title = "Terms of Service",
    intro = "These terms explain the rules for using Wisdom Tower Academy, including its courses, study materials and practice exams. By creating an account or using the app or website, you agree to them.",
    meta = "Document version: 3.3 • Last revised: 24 October 2026",
    summaryTitle = "The Short Version",
    summaryItems = listOf(
        "Personal use:" to "The materials are for your own study. Please do not copy, share or resell them.",
        "Honest practice:" to "Take timed exams and join leaderboards fairly, so results mean something.",
        "Keep your account safe:" to "Your account is yours alone. Do not share your sign-in details."
    ),
    sections = listOf(
        LegalSection(
            "01", "About These Terms", listOf(
                LegalBlock.Para("Wisdom Tower Academy (“the Platform”, “we”, “our” or “us”) provides study materials and practice tools for secondary school students (Grades 9–12), remedial and freshman students, university students, and candidates preparing for national exams such as UAT, GAT, COC and the University Exit Exam."),
                LegalBlock.Para("By creating an account, signing in (including with Google) or using the Platform, you confirm that you are at least 18 years old or have the permission of a parent or guardian, and that you agree to these terms. If you do not agree, please do not use the Platform.")
            )
        ),
        LegalSection(
            "02", "Your Account", listOf(
                LegalBlock.Para("Each account belongs to one student. Keeping it personal keeps leaderboards, streaks and progress records fair and accurate."),
                LegalBlock.Bullets(
                    listOf(
                        "Accurate details:" to "Please give us correct information when you register, including your real name and a working email address.",
                        "No sharing:" to "Do not share your sign-in details, sell access to your account or let others use it. We may suspend or close accounts that are shared.",
                        "Your devices:" to "You may use your account on your own phone, tablet and computer. Activity that suggests several people are using one account may lead to a temporary security hold."
                    )
                )
            )
        ),
        LegalSection(
            "03", "Content and Licence", listOf(
                LegalBlock.Para("The courses, notes, worked solutions, question banks, flashcards, software, artwork and branding on the Platform belong to Wisdom Tower Academy and its contributors."),
                LegalBlock.Card("What you may do", "You may view, read and practise with the materials available to you for your own private study and exam revision. This licence is personal, non-exclusive, non-transferable and can be withdrawn if these terms are broken."),
                LegalBlock.Para("", lead = "What you may not do:"),
                LegalBlock.Bullets(
                    listOf(
                        "" to "Use bots, scrapers or other automated tools to copy or download our content.",
                        "" to "Republish, share or resell our notes, question banks or solutions on Telegram channels, social media or other websites.",
                        "" to "Copy, modify, reverse-engineer or build on our software or content tools."
                    )
                )
            )
        ),
        LegalSection(
            "04", "Fair Use and Academic Integrity", listOf(
                LegalBlock.Para("We want every student to learn for real. When you use timed exams, competitions and leaderboards, please:"),
                LegalBlock.Bullets(
                    listOf(
                        "" to "Complete exams honestly, without tampering with timers or submitting false scores.",
                        "" to "Treat the explanations in the app as a study aid that supports your own problem solving, not a replacement for it.",
                        "" to "Do not post exam questions or answers while a live competition or assessment is still running."
                    )
                )
            )
        ),
        LegalSection(
            "05", "Availability of the Service", listOf(
                LegalBlock.Para("We know how important reliable access is during exam season, so the Platform lets you keep downloaded content on your device for offline study."),
                LegalBlock.Para("We work to keep the Platform running smoothly, but we cannot promise that it will always be available or free of interruptions, for example because of network outages or maintenance. We will announce planned maintenance in advance where we can.")
            )
        ),
        LegalSection(
            "06", "Results and Disclaimer", listOf(
                LegalBlock.Para("We create our materials carefully and align them with the curriculum, but your results in national exams, competitions or admissions depend on your own effort, preparation and performance on the day, and we cannot guarantee any particular outcome."),
                LegalBlock.Para("The Platform is provided “as is” and “as available”. To the extent the law allows, Wisdom Tower Academy is not liable for indirect or consequential losses arising from the use of, or inability to use, the Platform.", italic = true)
            )
        ),
        LegalSection(
            "07", "Suspension and Closing an Account", listOf(
                LegalBlock.Para("You may stop using the Platform and ask us to delete your account at any time. If an account breaks these terms, we may suspend or close it. Where possible we will tell you why.")
            )
        ),
        LegalSection(
            "08", "Changes to These Terms", listOf(
                LegalBlock.Para("We may update these terms from time to time. The version number and date at the top show when they were last changed. If you keep using the Platform after an update, you accept the new terms.")
            )
        ),
        LegalSection(
            "09", "Governing Law", listOf(
                LegalBlock.Para("These terms are governed by the laws of the Federal Democratic Republic of Ethiopia. If a disagreement arises, we ask that you contact us first so we can try to resolve it informally. If that does not work, the competent courts of Addis Ababa, Ethiopia will have jurisdiction.")
            )
        ),
        LegalSection(
            "10", "Contact Us", listOf(
                LegalBlock.Para("For questions about these terms, contact us:"),
                LegalBlock.Contact(
                    title = "Wisdom Tower Academy · Support",
                    lines = listOf("Addis Ababa, Ethiopia"),
                    links = listOf(
                        Triple("Email: ", "support@wisdomtower.tech", "mailto:support@wisdomtower.tech"),
                        Triple("Telegram: ", "@wisdom_tower2", "https://t.me/wisdom_tower2")
                    )
                )
            )
        )
    )
)
