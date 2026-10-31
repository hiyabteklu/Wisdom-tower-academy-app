package com.wisdomtower.academy.data.admin

import org.json.JSONArray
import org.json.JSONObject

data class ExamIntegrityEvent(
    val id: String,
    val kind: String,
    val createdAt: String,
    val awayMs: Long?,
    val detail: String
)

data class ExamIntegrityAttempt(
    val entryId: String,
    val userId: String,
    val studentName: String,
    val email: String,
    val studentId: String,
    val examTitle: String,
    val mode: String,
    val status: String,
    val startedAt: String,
    val submittedAt: String,
    val score: Int?,
    val totalCount: Int?,
    val flagCount: Int,
    val leaveCount: Int,
    val totalAwayMs: Long,
    val longestAwayMs: Long,
    val lastFlagAt: String,
    val reviewedAt: String,
    val reviewNote: String,
    val events: List<ExamIntegrityEvent>
)

data class RegisteredAccountDevice(
    val userId: String,
    val email: String,
    val fullName: String,
    val deviceLabel: String,
    val registeredAt: String,
    val lastSeenAt: String
)

data class AccountDeviceEvent(
    val eventId: Long,
    val userId: String,
    val email: String,
    val fullName: String,
    val deviceLabel: String,
    val eventType: String,
    val createdAt: String,
    val reviewedAt: String,
    val reviewNote: String
)

data class AdminSecurityList<T>(
    val rows: List<T> = emptyList(),
    val error: String? = null
) {
    val ok: Boolean get() = error == null
}

/**
 * Native Admin Console security data. All RPCs use the signed-in admin JWT; the database functions enforce admin access.
 * Never use the service-role key in this client.
 */
private fun JSONObject.safeString(key: String, fallback: String = ""): String =
    if (isNull(key)) fallback else optString(key, fallback)

object AdminSecurityRepository {
    suspend fun examIntegrity(): AdminSecurityList<ExamIntegrityAttempt> {
        val reply = AdminSupabase.rpc("admin_exam_integrity_report")
        if (!reply.ok) return AdminSecurityList(error = reply.errorText())
        return try {
            val json = JSONArray(reply.body)
            val rows = (0 until json.length()).mapNotNull { i ->
                json.optJSONObject(i)?.let(::parseExamAttempt)
            }
            AdminSecurityList(rows)
        } catch (_: Exception) {
            AdminSecurityList(error = "The exam integrity report could not be read.")
        }
    }

    suspend fun reviewExam(entryId: String, note: String): AdminHttpResult =
        AdminSupabase.rpc(
            "admin_review_exam_integrity",
            JSONObject()
                .put("p_entry_id", entryId)
                .put("p_review_note", note.trim().ifBlank { JSONObject.NULL })
                .toString()
        )

    suspend fun registeredDevices(): AdminSecurityList<RegisteredAccountDevice> {
        val reply = AdminSupabase.rpc("admin_list_account_devices")
        if (!reply.ok) return AdminSecurityList(error = reply.errorText())
        return try {
            val json = JSONArray(reply.body)
            AdminSecurityList((0 until json.length()).mapNotNull { i ->
                json.optJSONObject(i)?.let {
                    RegisteredAccountDevice(
                        userId = it.safeString("user_id"),
                        email = it.safeString("email"),
                        fullName = it.safeString("full_name"),
                        deviceLabel = it.safeString("device_label"),
                        registeredAt = it.safeString("registered_at"),
                        lastSeenAt = it.safeString("last_seen_at")
                    )
                }
            })
        } catch (_: Exception) {
            AdminSecurityList(error = "The registered-device list could not be read.")
        }
    }

    suspend fun deviceEvents(): AdminSecurityList<AccountDeviceEvent> {
        val reply = AdminSupabase.rpc("admin_list_account_device_events")
        if (!reply.ok) return AdminSecurityList(error = reply.errorText())
        return try {
            val json = JSONArray(reply.body)
            AdminSecurityList((0 until json.length()).mapNotNull { i ->
                json.optJSONObject(i)?.let {
                    AccountDeviceEvent(
                        eventId = it.optLong("event_id"),
                        userId = it.optString("user_id"),
                        email = it.optString("email"),
                        fullName = it.optString("full_name"),
                        deviceLabel = it.optString("device_label"),
                        eventType = it.safeString("event_type"),
                        createdAt = it.safeString("created_at"),
                        reviewedAt = it.safeString("reviewed_at"),
                        reviewNote = it.safeString("review_note")
                    )
                }
            })
        } catch (_: Exception) {
            AdminSecurityList(error = "The device activity history could not be read.")
        }
    }

    suspend fun resetDevice(userId: String): AdminHttpResult =
        AdminSupabase.rpc(
            "admin_reset_account_device",
            JSONObject().put("p_user_id", userId).toString()
        )

    suspend fun reviewDeviceEvent(eventId: Long, note: String): AdminHttpResult =
        AdminSupabase.rpc(
            "admin_review_account_device_event",
            JSONObject()
                .put("p_event_id", eventId)
                .put("p_review_note", note.trim().ifBlank { JSONObject.NULL })
                .toString()
        )

    suspend fun pendingExamStudentCount(): Int? {
        val result = examIntegrity()
        if (!result.ok) return null
        return result.rows.filter { it.reviewedAt.isBlank() }.map { it.userId }.distinct().size
    }

    suspend fun pendingDeviceStudentCount(): Int? {
        val result = deviceEvents()
        if (!result.ok) return null
        return result.rows
            .filter { it.eventType == "blocked_new_device" && it.reviewedAt.isBlank() }
            .map { it.userId }
            .distinct()
            .size
    }

    private fun parseExamAttempt(row: JSONObject): ExamIntegrityAttempt {
        val eventsJson = row.optJSONArray("events") ?: JSONArray()
        val events = (0 until eventsJson.length()).mapNotNull { i ->
            eventsJson.optJSONObject(i)?.let { event ->
                val detail = event.optJSONObject("detail")
                ExamIntegrityEvent(
                    id = event.safeString("id"),
                    kind = event.safeString("kind"),
                    createdAt = event.safeString("created_at"),
                    awayMs = if (event.isNull("away_ms")) null else event.optLong("away_ms"),
                    detail = detail?.toString().orEmpty()
                )
            }
        }
        return ExamIntegrityAttempt(
            entryId = row.safeString("entry_id"),
            userId = row.safeString("user_id"),
            studentName = row.safeString("student_name", "Student"),
            email = row.safeString("email"),
            studentId = row.safeString("student_id"),
            examTitle = row.safeString("competition_title"),
            mode = row.safeString("mode"),
            status = row.safeString("attempt_status"),
            startedAt = row.safeString("started_at"),
            submittedAt = row.safeString("submitted_at"),
            score = if (row.isNull("score")) null else row.optInt("score"),
            totalCount = if (row.isNull("total_count")) null else row.optInt("total_count"),
            flagCount = row.optInt("flag_count"),
            leaveCount = row.optInt("leave_count"),
            totalAwayMs = row.optLong("total_away_ms"),
            longestAwayMs = row.optLong("longest_away_ms"),
            lastFlagAt = row.safeString("last_flag_at"),
            reviewedAt = row.safeString("reviewed_at"),
            reviewNote = row.safeString("review_note"),
            events = events
        )
    }
}
