package com.wisdomtower.academy.data.admin

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** One row of the `inquiries` table (columns as in website-reference/src/lib/admin-data.ts InquiryRow). */
data class InquiryItem(
    val id: String,
    val createdAt: String,
    val name: String,
    val email: String,
    val service: String?,
    val message: String,
    val status: String
)

sealed class InquiriesResult {
    class Success(val items: List<InquiryItem>) : InquiriesResult()
    class Failure(val message: String) : InquiriesResult()
}

/**
 * ADMIN-native N2. Same two calls as the website's InquiriesPanel (listInquiries, updateInquiryStatus):
 * newest 200 rows, and a status change. Allowed statuses on the website: new, read, replied, archived.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object InquiriesRepository {
    val STATUSES: List<String> = listOf("new", "read", "replied", "archived")

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    suspend fun list(): InquiriesResult {
        val reply = AdminSupabase.get(
            "inquiries?select=id,created_at,name,email,service,message,status&order=created_at.desc&limit=200"
        )
        if (!reply.ok) return InquiriesResult.Failure(reply.errorText())
        return try {
            val arr = JSONArray(reply.body)
            val out = ArrayList<InquiryItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    InquiryItem(
                        id = o.text("id") ?: continue,
                        createdAt = o.text("created_at") ?: "",
                        name = o.text("name") ?: "",
                        email = o.text("email") ?: "",
                        service = o.text("service"),
                        message = o.text("message") ?: "",
                        status = o.text("status") ?: "new"
                    )
                )
            }
            InquiriesResult.Success(out)
        } catch (e: Exception) {
            InquiriesResult.Failure("Unexpected reply from the server")
        }
    }

    /** Resolve an inquiry sender to a registered Academy account, if one exists. */
    suspend fun findStudentIdByEmail(email: String): String? {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank()) return null
        val reply = AdminSupabase.get("profiles?select=id&email=ilike." + Uri.encode(normalized) + "&limit=1")
        if (!reply.ok) return null
        return try {
            val rows = JSONArray(reply.body)
            if (rows.length() == 0) null else rows.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
    }

    /** Returns null on success, otherwise a short message. Zero changed rows counts as a failure (RLS refusal). */
    suspend fun setStatus(id: String, status: String): String? {
        val body = JSONObject().put("status", status).toString()
        val reply = AdminSupabase.patch("inquiries?id=eq.${Uri.encode(id)}", body)
        if (!reply.ok) return reply.errorText()
        val changed = try {
            JSONArray(reply.body).length()
        } catch (e: Exception) {
            0
        }
        return if (changed > 0) null else "Not changed: no permission or the message no longer exists"
    }
}
