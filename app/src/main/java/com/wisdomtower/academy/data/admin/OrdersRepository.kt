package com.wisdomtower.academy.data.admin

import android.net.Uri
import com.wisdomtower.academy.data.auth.AuthStateManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row of the `orders` table (columns as in website-reference/src/lib/orders.ts rowToOrder). */
data class OrderItem(
    val id: String,
    val packageId: String,
    val packageName: String,
    val amountEtb: Double,
    val status: String,
    val paymentMethod: String?,
    val studentName: String,
    val phone: String,
    val email: String?,
    val transactionRef: String,
    val note: String?,
    val receiptUrl: String?,
    val createdAt: String,
    val userId: String?
)

sealed class OrdersResult {
    class Success(val items: List<OrderItem>) : OrdersResult()
    class Failure(val message: String) : OrdersResult()
}

/** error == null means the order is now verified. enrollWarning is set when the order was approved but an enrollment failed. */
class VerifyOutcome(val error: String?, val enrollWarning: String?)

/**
 * ADMIN-native N3. Same steps as the website PaymentsPanel (lib/orders.ts verifyOrder, rejectOrder, listOrdersFromDb,
 * lib/notify.ts):
 *  - list: newest 200 orders
 *  - approve: set status verified (+ verified_at, verified_by = the admin's e-mail), then create one `enrollments` row
 *    per package id (ids from the order note `packages:id1,id2`, or the order's package id), ignoring duplicates,
 *    then ask the website route /api/notify-approval to send the e-mail and SMS
 *  - reject: set status rejected (+ verified_at, verified_by), then the same notify route
 * Differences, on purpose: the status change only applies while the order is still pending_verification (protects
 * against a double tap or two admins), zero changed rows is reported as a failure, and a failed enrollment is
 * reported in the toast (the website only logs it).
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object OrdersRepository {
    private val NOTE_PACKAGES = Regex("packages:([a-z0-9_,-]+)", RegexOption.IGNORE_CASE)

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    suspend fun list(): OrdersResult {
        val reply = AdminSupabase.get("orders?select=*&order=created_at.desc&limit=200")
        if (!reply.ok) return OrdersResult.Failure(reply.errorText())
        return try {
            val arr = JSONArray(reply.body)
            val out = ArrayList<OrderItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val amount = o.optDouble("amount_etb", 0.0)
                out.add(
                    OrderItem(
                        id = o.text("id") ?: continue,
                        packageId = o.text("package_id") ?: "",
                        packageName = o.text("package_name") ?: "",
                        amountEtb = if (amount.isNaN()) 0.0 else amount,
                        status = o.text("status") ?: "",
                        paymentMethod = o.text("payment_method"),
                        studentName = o.text("student_name") ?: "",
                        phone = o.text("phone") ?: "",
                        email = o.text("email"),
                        transactionRef = o.text("transaction_ref") ?: "",
                        note = o.text("note"),
                        receiptUrl = o.text("receipt_url"),
                        createdAt = o.text("created_at") ?: "",
                        userId = o.text("user_id")
                    )
                )
            }
            OrdersResult.Success(out)
        } catch (e: Exception) {
            OrdersResult.Failure("Unexpected reply from the server")
        }
    }

    /** Copy of packageIdsFromOrderNote in lib/orders.ts. */
    fun packageIdsFor(note: String?, packageId: String): List<String> {
        val ids = ArrayList<String>()
        if (note != null) {
            val match = NOTE_PACKAGES.find(note)
            if (match != null) {
                for (part in match.groupValues[1].split(",")) {
                    val t = part.trim()
                    if (t.isNotEmpty() && !ids.contains(t)) ids.add(t)
                }
            }
        }
        if (packageId.isNotEmpty() && packageId != "multi" && !ids.contains(packageId)) ids.add(0, packageId)
        return ids
    }

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    /** Returns null on success, otherwise a short message. Only a still-pending order is changed. */
    private suspend fun setStatus(orderId: String, status: String): String? {
        val adminEmail = AuthStateManager.currentUser.value?.email
        val body = JSONObject()
            .put("status", status)
            .put("verified_at", nowIso())
            .put("verified_by", adminEmail ?: JSONObject.NULL)
            .toString()
        val reply = AdminSupabase.patch(
            "orders?id=eq.${Uri.encode(orderId)}&status=eq.pending_verification",
            body
        )
        if (!reply.ok) return reply.errorText()
        val changed = try {
            JSONArray(reply.body).length()
        } catch (e: Exception) {
            0
        }
        return if (changed > 0) null else "Not changed: the order is no longer pending, or no permission"
    }

    suspend fun verify(order: OrderItem): VerifyOutcome {
        val statusError = setStatus(order.id, "verified")
        if (statusError != null) return VerifyOutcome(statusError, null)
        val failures = ArrayList<String>()
        for (pid in packageIdsFor(order.note, order.packageId)) {
            val body = JSONObject()
                .put("order_id", order.id)
                .put("package_id", pid)
                .put("package_name", order.packageName)
                .put("email", order.email ?: JSONObject.NULL)
                .put("user_id", order.userId ?: JSONObject.NULL)
                .toString()
            val path = if (order.userId != null) "enrollments?on_conflict=user_id,package_id" else "enrollments"
            val reply = AdminSupabase.post(path, body, "resolution=ignore-duplicates,return=minimal")
            if (!reply.ok) failures.add("$pid: ${reply.errorText()}")
        }
        val warning = if (failures.isEmpty()) null else "Enrollment problem (" + failures.joinToString("; ") + ")"
        return VerifyOutcome(null, warning)
    }

    suspend fun reject(order: OrderItem): String? = setStatus(order.id, "rejected")

    private fun channelText(json: JSONObject, key: String, label: String, skipHint: String): String? {
        val c = json.optJSONObject(key) ?: return null
        if (c.optBoolean("sent", false)) return "$label sent"
        if (c.optBoolean("skipped", false)) return "$label skipped ($skipHint)"
        val err = c.optString("error", "")
        return if (err.isNotEmpty()) "$label: $err" else null
    }

    /** Calls the website route and builds the same toast text as formatNotifyToast in lib/notify.ts. Never throws. */
    suspend fun notify(origin: String, orderId: String, approved: Boolean): String {
        val verb = if (approved) "Approved" else "Rejected"
        val body = JSONObject()
            .put("orderId", orderId)
            .put("type", if (approved) "approved" else "rejected")
            .toString()
        val reply = AdminSupabase.postPublic("$origin/api/notify-approval", body)
        if (reply.code < 0) return "$verb, but notify failed: ${reply.errorText()}"
        val json = try {
            JSONObject(reply.body)
        } catch (e: Exception) {
            null
        }
        if (!reply.ok || json == null || !json.optBoolean("ok", false)) {
            val err = json?.optString("error", "").orEmpty()
            return "$verb, but notify failed: ${if (err.isNotEmpty()) err else "HTTP ${reply.code}"}"
        }
        val parts = ArrayList<String>()
        parts.add(verb)
        channelText(json, "email", "email", "no key/address")?.let { parts.add(it) }
        channelText(json, "sms", "SMS", "no key/phone")?.let { parts.add(it) }
        return parts.joinToString(" \u00B7 ")
    }
}
