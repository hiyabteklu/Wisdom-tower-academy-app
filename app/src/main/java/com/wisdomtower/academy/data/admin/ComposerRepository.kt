package com.wisdomtower.academy.data.admin

import org.json.JSONArray
import org.json.JSONObject

/** A value or a short error message. */
class ApiResult<T>(val value: T?, val error: String?)

class LevelCount(val level: String, val n: Int)

class LevelCounts(val total: Int, val noLevel: Int, val levels: List<LevelCount>)

class CustomAudience(val id: String, val label: String, val students: Int)

/**
 * ADMIN-native N8b. The write half of the website Push Broadcasts panel (NotificationComposerV2 and
 * NotificationAudiencePicker), same calls as the website:
 *  - send now: POST api/notifications/send with the admin token (the server pushes through Firebase)
 *  - schedule: database function admin_schedule_notification_v2; edit a waiting row: admin_update_scheduled_notification_v2
 *  - audience chips: admin_audience_level_counts, admin_count_notification_audience, admin_list_notification_audiences,
 *    admin_add_notification_audience, admin_delete_notification_audience (admin's own login)
 * Parameter names are the ones the website passes (p_title, p_body, p_send_at, p_category, p_content, p_url,
 * p_audiences, p_id, p_label, p_match_levels, p_match_package_ids).
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 * HYPOTHESIS: admin_audience_level_counts returns one JSON object with total, no_level and levels (level, n) and
 * admin_count_notification_audience returns a bare number, exactly as the website code reads them.
 * Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
 */
object ComposerRepository {
    /** Same list as ACADEMIC_LEVEL_OPTIONS in website-reference/src/lib/academic-levels.ts. */
    val ACADEMIC_LEVELS: List<String> = listOf(
        "Grade 9", "Grade 10", "Grade 11", "Grade 12", "Remedial", "Freshman", "3rd Year (ECE)", "Other"
    )

    private suspend fun rpc(name: String, body: JSONObject): AdminHttpResult =
        AdminSupabase.post("rpc/$name", body.toString(), "count=none")

    suspend fun levelCounts(): ApiResult<LevelCounts> {
        val reply = rpc("admin_audience_level_counts", JSONObject())
        if (!reply.ok) return ApiResult(null, reply.errorText())
        return try {
            val o = JSONObject(reply.body)
            val arr = o.optJSONArray("levels") ?: JSONArray()
            val levels = ArrayList<LevelCount>()
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                levels.add(LevelCount(row.optString("level", ""), row.optInt("n", 0)))
            }
            ApiResult(LevelCounts(o.optInt("total", 0), o.optInt("no_level", 0), levels), null)
        } catch (e: Exception) {
            ApiResult(null, "Unexpected reply from the server")
        }
    }

    suspend fun customAudiences(): ApiResult<List<CustomAudience>> {
        val reply = rpc("admin_list_notification_audiences", JSONObject())
        if (!reply.ok) return ApiResult(null, reply.errorText())
        return try {
            val arr = JSONArray(reply.body)
            val out = ArrayList<CustomAudience>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                if (id.isEmpty()) continue
                out.add(CustomAudience(id, o.optString("label", ""), o.optInt("students", 0)))
            }
            ApiResult(out, null)
        } catch (e: Exception) {
            ApiResult(null, "Unexpected reply from the server")
        }
    }

    /** Live count of the students a selection reaches. Null when it could not be read (never a made-up number). */
    suspend fun countAudience(audiences: List<String>): Int? {
        val body = JSONObject().put("p_audiences", JSONArray(audiences))
        val reply = rpc("admin_count_notification_audience", body)
        if (!reply.ok) return null
        return reply.body.trim().toIntOrNull()
    }

    /** Returns null on success, otherwise a short message. */
    suspend fun addAudience(label: String, levels: List<String>, packages: List<String>): String? {
        val body = JSONObject()
            .put("p_label", label)
            .put("p_match_levels", JSONArray(levels))
            .put("p_match_package_ids", JSONArray(packages))
        val reply = rpc("admin_add_notification_audience", body)
        return if (reply.ok) null else reply.errorText()
    }

    /** Returns null on success, otherwise a short message. */
    suspend fun deleteAudience(id: String): String? {
        val reply = rpc("admin_delete_notification_audience", JSONObject().put("p_id", id))
        return if (reply.ok) null else reply.errorText()
    }

    /**
     * Schedule (editId null) or save changes to a waiting row. sendAtIso is a UTC instant. url is the picked path for a
     * material notice, otherwise null. An empty audiences list means everyone. Returns null on success.
     */
    suspend fun schedule(
        editId: String?,
        title: String,
        preview: String,
        sendAtIso: String,
        category: String,
        content: JSONObject,
        url: String?,
        audiences: List<String>
    ): String? {
        val body = JSONObject()
            .put("p_title", title)
            .put("p_body", preview)
            .put("p_send_at", sendAtIso)
            .put("p_category", category)
            .put("p_content", content)
            .put("p_url", url ?: JSONObject.NULL)
            .put("p_audiences", if (audiences.isEmpty()) JSONObject.NULL else JSONArray(audiences))
        val name = if (editId != null) "admin_update_scheduled_notification_v2" else "admin_schedule_notification_v2"
        if (editId != null) body.put("p_id", editId)
        val reply = rpc(name, body)
        return if (reply.ok) null else BroadcastsRepository.friendlyScheduleError(reply.errorText())
    }

    /**
     * Send now. target is one student's user id (then audiences is ignored); otherwise audiences (empty = everyone).
     * Returns the success sentence (same wording as the website) or an error message.
     */
    suspend fun sendNow(
        origin: String,
        title: String,
        category: String,
        content: JSONObject,
        url: String?,
        audiences: List<String>,
        target: String?
    ): ApiResult<String> {
        val payload = JSONObject()
            .put("title", title)
            .put("category", category)
            .put("content", content)
        if (url != null) payload.put("url", url)
        if (target != null) payload.put("target", target) else payload.put("audiences", JSONArray(audiences))
        val reply = AdminWebApi.post("$origin/api/notifications/send", payload.toString())
        if (!reply.ok) return ApiResult(null, BroadcastsRepository.apiError(reply, "Failed to send the notification"))
        return try {
            val root = JSONObject(reply.body)
            if (!root.optBoolean("ok", false)) {
                val message = root.optString("error", "")
                ApiResult(null, if (message.isBlank()) "Failed to send the notification." else message)
            } else {
                val recipients = root.optInt("recipients", 0)
                val pushed: Int? = if (root.has("pushed") && !root.isNull("pushed")) root.optInt("pushed", 0) else null
                val sb = StringBuilder("Sent. ")
                if (root.optBoolean("broadcast", false)) sb.append("Everyone can see it. ")
                else if (recipients > 0) sb.append("Students reached: $recipients. ")
                if (pushed != null) sb.append("Push sent to $pushed device(s).")
                ApiResult(sb.toString().trim(), null)
            }
        } catch (e: Exception) {
            ApiResult(null, "Unexpected reply from the server")
        }
    }
}
