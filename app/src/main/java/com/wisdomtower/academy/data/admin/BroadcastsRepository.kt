package com.wisdomtower.academy.data.admin

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One sent notification (website NotificationItem, from GET /api/notifications?admin=true). */
data class BroadcastItem(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val target: String,
    val url: String?,
    val createdAt: String
)

/** One row of admin_list_scheduled_notifications_v2 (website ScheduledRowV2, the columns the list shows). */
data class ScheduledItem(
    val id: String,
    val title: String,
    val body: String,
    val url: String?,
    val sendAt: String,
    val status: String,
    val category: String?,
    val audiences: List<String>,
    val audienceScope: String?,
    val packageId: String?,
    val lastError: String?
)

sealed class HistoryResult {
    class Success(val items: List<BroadcastItem>) : HistoryResult()
    class Failure(val message: String) : HistoryResult()
}

sealed class ScheduledResult {
    class Success(val items: List<ScheduledItem>) : ScheduledResult()
    class Failure(val message: String) : ScheduledResult()
}

/**
 * ADMIN-native N8a. The read and cancel half of the website Push Broadcasts panel, same calls as the website:
 *  - history: GET api/notifications?admin=true with the admin token (website route, newest 100)
 *  - delete one: DELETE api/notifications?id=... with the admin token
 *  - scheduled list: database function admin_list_scheduled_notifications_v2 (p_limit 50), admin's own login
 *  - cancel a waiting one: database function admin_cancel_scheduled_notification (p_id)
 * Writing and sending a new notification (composer) is NOT here yet (slice N8b).
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 * Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
 */
object BroadcastsRepository {
    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    /** Same wording as friendlyScheduleError on the website. */
    fun friendlyScheduleError(message: String): String {
        if (message.contains("admin_only")) return "Only an admin account can do this. Please sign in again as admin."
        if (message.contains("send_at_must_be_in_future")) return "The send time must be in the future."
        if (message.contains("title_required")) return "Please enter a title."
        if (message.contains("body_required")) return "Please enter a message."
        if (message.contains("url_required_for_material")) return "Pick where this material notice should open."
        if (message.contains("bad_content") || message.contains("content_too_large")) {
            return "The message content is not valid or is too long."
        }
        if (message.contains("not_pending_or_not_found")) return "This notification is no longer waiting to be sent."
        return if (message.isBlank()) "Something went wrong." else message
    }

    private fun apiError(reply: AdminHttpResult, fallback: String): String {
        if (reply.code == -2) return "Not signed in"
        if (reply.code == -1) return "Could not reach the server"
        val fromServer = try {
            JSONObject(reply.body).optString("error", "")
        } catch (e: Exception) {
            ""
        }
        if (fromServer.isNotBlank()) return fromServer
        if (reply.code == 401 || reply.code == 403) return "Only an admin account can do this. Please sign in again as admin."
        return "$fallback (HTTP ${reply.code})"
    }

    suspend fun loadHistory(origin: String): HistoryResult {
        val reply = AdminWebApi.get("$origin/api/notifications?admin=true")
        if (!reply.ok) return HistoryResult.Failure(apiError(reply, "Could not load the broadcast history"))
        return try {
            val root = JSONObject(reply.body)
            val arr = root.optJSONArray("notifications") ?: JSONArray()
            val out = ArrayList<BroadcastItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    BroadcastItem(
                        id = o.text("id") ?: continue,
                        title = o.text("title") ?: "",
                        body = o.text("body") ?: "",
                        type = o.text("type") ?: "general",
                        target = o.text("target") ?: "all",
                        url = o.text("url"),
                        createdAt = o.text("createdAt") ?: ""
                    )
                )
            }
            HistoryResult.Success(out)
        } catch (e: Exception) {
            HistoryResult.Failure("Unexpected reply from the server")
        }
    }

    /** Returns null on success, otherwise a short message. */
    suspend fun deleteHistory(origin: String, id: String): String? {
        val reply = AdminWebApi.delete("$origin/api/notifications?id=${Uri.encode(id)}")
        if (!reply.ok) return apiError(reply, "Could not delete the notification")
        return null
    }

    suspend fun loadScheduled(): ScheduledResult {
        val body = JSONObject().put("p_limit", 50).toString()
        val reply = AdminSupabase.post("rpc/admin_list_scheduled_notifications_v2", body, "count=none")
        if (!reply.ok) return ScheduledResult.Failure(friendlyScheduleError(reply.errorText()))
        return try {
            val arr = JSONArray(reply.body)
            val out = ArrayList<ScheduledItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val audiences = ArrayList<String>()
                val audArr = o.optJSONArray("audiences")
                if (audArr != null) {
                    for (j in 0 until audArr.length()) {
                        val entry = audArr.optString(j, "")
                        if (entry.isNotEmpty()) audiences.add(entry)
                    }
                }
                out.add(
                    ScheduledItem(
                        id = o.text("id") ?: continue,
                        title = o.text("title") ?: "",
                        body = o.text("body") ?: "",
                        url = o.text("url"),
                        sendAt = o.text("send_at") ?: "",
                        status = o.text("status") ?: "pending",
                        category = o.text("category"),
                        audiences = audiences,
                        audienceScope = o.text("audience_scope"),
                        packageId = o.text("package_id"),
                        lastError = o.text("last_error")
                    )
                )
            }
            ScheduledResult.Success(out)
        } catch (e: Exception) {
            ScheduledResult.Failure("Unexpected reply from the server")
        }
    }

    /** Returns null on success, otherwise a short message (same wording as the website). */
    suspend fun cancelScheduled(id: String): String? {
        val body = JSONObject().put("p_id", id).toString()
        val reply = AdminSupabase.post("rpc/admin_cancel_scheduled_notification", body, "count=none")
        if (!reply.ok) return friendlyScheduleError(reply.errorText())
        return null
    }

    private fun parseInstant(iso: String): Date? {
        return try {
            var s = iso.trim().replace(Regex("\\.\\d+"), "")
            if (s.endsWith("Z")) s = s.dropLast(1) + "+00:00"
            if (s.length == 19) s += "+00:00"
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(s)
        } catch (e: Exception) {
            null
        }
    }

    /** Ethiopia time (EAT, UTC+3) like the website's formatEat. Falls back to the raw text. */
    fun formatEat(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val date = parseInstant(iso) ?: return iso
        val out = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        out.timeZone = TimeZone.getTimeZone("Africa/Addis_Ababa")
        return out.format(date) + " EAT"
    }

    /** Same rule as audienceText on the website. */
    fun audienceText(item: ScheduledItem): String {
        if (item.audiences.isNotEmpty()) {
            return item.audiences.joinToString(", ") { if (it.startsWith("custom:")) "custom audience" else it }
        }
        val scope = item.audienceScope
        return if (scope.isNullOrBlank()) "everyone" else scope
    }
}
