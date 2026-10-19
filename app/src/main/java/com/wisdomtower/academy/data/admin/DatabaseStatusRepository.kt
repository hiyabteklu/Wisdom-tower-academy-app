package com.wisdomtower.academy.data.admin

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** How one tracked table answered the row count. */
enum class DbTableState { OK, MISSING, DENIED, ERROR, UNREACHABLE }

/** rowCount is null when the count could not be read (the screen shows a dash, never a made-up number). */
class DbTableStatus(
    val table: String,
    val label: String,
    val description: String,
    val state: DbTableState,
    val rowCount: Int?,
    val detail: String
)

/**
 * ADMIN-native N10b. READ-ONLY database status for the admin console. It replaces the website Database and SQL Hub
 * in the app. The only thing it does is ask Supabase for the exact row count of each tracked table with a HEAD
 * request (no rows are read, nothing is written, no SQL is run). The table names, labels and descriptions are the
 * ones of TRACKED_TABLES in website-reference/src/components/admin/DatabaseHubPanel.tsx.
 * NOT carried over on purpose: the master SQL script and its Copy button, the Open SQL Editor link, the setup
 * steps, and the two fixed text cards about row level security and student ids (they were not read from the
 * database).
 * What a row count can show is limited by the signed-in account's own row level security, like on the website.
 * Follow-up 2026-10-19: tables in NO_ADMIN_READ have no read policy for admins (checked live), so their count would
 * always be 0 and look like an empty table; they show no count instead.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object DatabaseStatusRepository {
    private class Tracked(val table: String, val label: String, val description: String)

    private val TRACKED: List<Tracked> = listOf(
        Tracked("profiles", "Student Profiles", "Matriculation data, student IDs, streams, school names, settings"),
        Tracked("orders", "Payment Orders", "Manual payment orders, transaction refs, Telebirr/CBE reconciliation"),
        Tracked("enrollments", "Course Enrollments", "Active access records matching verified payment orders"),
        Tracked("access_grants", "Manual Access Grants", "Direct admin-granted access, scholarship passes, beta testing overrides"),
        Tracked("content_locks", "Content Access Locks", "Fine-grained chapter/hub locks, coming soon toggles, purchase gates"),
        Tracked("academic_results", "Quiz & Exam Scores", "Student test submissions, scores, percentages, and progress history"),
        Tracked("question_explanations", "AI Explanations Cache", "Cached Groq/OpenAI tutor explanations for zero-latency queries"),
        Tracked("inquiries", "Contact & Inquiries", "Student help inquiries, contact form messages, and support records"),
        Tracked("free_resource_items", "Free Resource Hub", "Scholarships, campus guides, success stories, and university articles"),
        Tracked("api_rate_limits", "API Rate Limiting", "Protection against brute force on AI endpoints and checkouts")
    )

    /** Tables that admins cannot read (no read policy). A HEAD count on them is always 0, which would mislead. */
    private val NO_ADMIN_READ: Set<String> = setOf("api_rate_limits")

    /** All tracked tables, asked at the same time. Never throws. */
    suspend fun check(): List<DbTableStatus> = coroutineScope {
        val calls = TRACKED.map { t -> async { checkOne(t) } }
        calls.map { it.await() }
    }

    private suspend fun checkOne(t: Tracked): DbTableStatus {
        // select=* because some tables have no id column (for example api_rate_limits)
        val reply = AdminSupabase.headCount("${t.table}?select=*")
        if (reply.ok) {
            if (NO_ADMIN_READ.contains(t.table)) {
                return DbTableStatus(
                    t.table, t.label, t.description, DbTableState.DENIED, null,
                    "Admins have no read access to this table, so no count is shown"
                )
            }
            val n = reply.contentRange?.substringAfter('/', "")?.trim()?.toIntOrNull()
            return DbTableStatus(t.table, t.label, t.description, DbTableState.OK, n, "")
        }
        return when (reply.code) {
            -1 -> DbTableStatus(t.table, t.label, t.description, DbTableState.UNREACHABLE, null, "Could not reach the server")
            -2 -> DbTableStatus(t.table, t.label, t.description, DbTableState.ERROR, null, "Not signed in")
            404 -> DbTableStatus(t.table, t.label, t.description, DbTableState.MISSING, null, "Table not created yet")
            401, 403 -> DbTableStatus(t.table, t.label, t.description, DbTableState.DENIED, null, "This account may not read it")
            else -> DbTableStatus(t.table, t.label, t.description, DbTableState.ERROR, null, "HTTP ${reply.code}")
        }
    }
}
