package com.wisdomtower.academy.data.admin

import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OverviewDay(val label: String, val orders: Int, val verified: Int, val revenue: Double)

/** amount is verified revenue for orders based lists, unused (0) for stream counts. */
class NamedCount(val name: String, val count: Int, val amount: Double)

class OrderStats(
    val total: Int,
    val truncated: Boolean,
    val pending: Int,
    val verified: Int,
    val revenueVerified: Double,
    val pipeline: Double,
    val avgOrder: Double,
    val conversionPct: Double,
    val todayCount: Int,
    val todayRevenue: Double,
    val urgentPending: Int,
    val last7: List<OverviewDay>,
    val byMethod: List<NamedCount>,
    val byPackage: List<NamedCount>
)

class ProfileStats(val loaded: Int, val truncated: Boolean, val byStream: List<NamedCount>)

/** kind is one of order, enroll, inquiry, user. */
class FeedItem(val id: String, val kind: String, val whenIso: String, val text: String, val detail: String?)

/** A null block or count means that read failed; the screen shows a dash or a short message, never a made-up number. */
class OverviewStats(
    val orders: OrderStats?,
    val profiles: ProfileStats?,
    val scholars: Int?,
    val enrollments: Int?,
    val explanations: Int?,
    val quizzes: Int?,
    val inquiriesTotal: Int?,
    val inquiriesNew: Int?,
    val feed: List<FeedItem>
)

sealed class OverviewResult {
    class Success(val stats: OverviewStats) : OverviewResult()
    class Failure(val message: String) : OverviewResult()
}

private class OverviewOrder(
    val id: String,
    val packageName: String,
    val amountEtb: Double,
    val status: String,
    val paymentMethod: String?,
    val studentName: String,
    val createdAt: String
)

/**
 * ADMIN-native N5. Same numbers as the website AnalyticsPanel (lib/admin-data.ts fetchAdminStats), read with the
 * admin's own login. Plain Supabase REST, no API route, no secret.
 * Differences, on purpose: totals that the website took from a capped list (scholars, enrollments, inquiries) are
 * exact counts here; orders and profiles are read up to 1000 rows (the website read 500) and the screen says so when
 * the cap is reached; the 7-day chart and Today use the PHONE's calendar days (the website mixed UTC days with local
 * midnight, which shifts the bars by a day in Ethiopia); a failed read shows a dash.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object OverviewRepository {
    private const val ROW_LIMIT = 1000

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    fun etb(amount: Double): String {
        val f = NumberFormat.getNumberInstance(Locale.US)
        f.maximumFractionDigits = 3
        return f.format(amount) + " ETB"
    }

    /** 40.0 prints as 40, 42.5 as 42.5. */
    fun plainNumber(v: Double): String =
        if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()

    private fun millisOf(iso: String): Long? {
        if (iso.length < 19) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            parser.parse(iso.substring(0, 19))?.time
        } catch (e: Exception) {
            null
        }
    }

    /** Same wording as timeAgo in AnalyticsPanel.tsx. */
    fun timeAgo(iso: String): String {
        val ms = millisOf(iso) ?: return iso
        val s = (System.currentTimeMillis() - ms) / 1000
        if (s < 0) return "now"
        if (s < 60) return "${s}s ago"
        if (s < 3600) return "${s / 60}m ago"
        if (s < 86400) return "${s / 3600}h ago"
        return "${s / 86400}d ago"
    }

    /** Same wording as statusLabel in lib/orders.ts. */
    private fun statusLabel(status: String): String = when (status) {
        "pending_payment" -> "Awaiting payment"
        "pending_verification" -> "Pending verification"
        "verified" -> "Verified \u00B7 enrolled"
        "rejected" -> "Needs attention"
        else -> status
    }

    private fun parseOrders(body: String): List<OverviewOrder>? = try {
        val arr = JSONArray(body)
        val out = ArrayList<OverviewOrder>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val amount = o.optDouble("amount_etb", 0.0)
            out.add(
                OverviewOrder(
                    id = o.text("id") ?: continue,
                    packageName = o.text("package_name") ?: "Unknown",
                    amountEtb = if (amount.isNaN()) 0.0 else amount,
                    status = o.text("status") ?: "",
                    paymentMethod = o.text("payment_method"),
                    studentName = o.text("student_name") ?: "",
                    createdAt = o.text("created_at") ?: ""
                )
            )
        }
        out
    } catch (e: Exception) {
        null
    }

    private fun streamLabel(raw: String?): String {
        if (raw == null) return "Unspecified"
        val low = raw.lowercase()
        return if (low.contains("nat")) "Natural Science" else if (low.contains("soc")) "Social Science" else raw
    }

    private fun computeOrderStats(orders: List<OverviewOrder>): OrderStats {
        val pending = orders.filter { it.status == "pending_verification" }
        val verified = orders.filter { it.status == "verified" }
        val revenue = verified.sumOf { it.amountEtb }
        val pipeline = pending.sumOf { it.amountEtb }
        val conversion =
            if (orders.isNotEmpty()) Math.round(verified.size.toDouble() / orders.size * 1000) / 10.0 else 0.0
        val average = if (verified.isNotEmpty()) Math.round(revenue / verified.size).toDouble() else 0.0

        val nowMs = System.currentTimeMillis()
        val twoHoursAgo = nowMs - 2L * 60L * 60L * 1000L
        val urgent = pending.count { o ->
            val ms = millisOf(o.createdAt)
            ms != null && ms < twoHoursAgo
        }

        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val todayKey = dayFormat.format(Date(nowMs))
        val dayKeys = ArrayList<String>()
        val dayLabels = ArrayList<String>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0 until 7) {
            dayKeys.add(dayFormat.format(cal.time))
            dayLabels.add(labelFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val dayOrders = IntArray(7)
        val dayVerified = IntArray(7)
        val dayRevenue = DoubleArray(7)
        var todayCount = 0
        var todayRevenue = 0.0
        for (o in orders) {
            val ms = millisOf(o.createdAt) ?: continue
            val key = dayFormat.format(Date(ms))
            val isVerified = o.status == "verified"
            if (key == todayKey) {
                todayCount += 1
                if (isVerified) todayRevenue += o.amountEtb
            }
            val idx = dayKeys.indexOf(key)
            if (idx >= 0) {
                dayOrders[idx] += 1
                if (isVerified) {
                    dayVerified[idx] += 1
                    dayRevenue[idx] += o.amountEtb
                }
            }
        }
        val last7 = ArrayList<OverviewDay>()
        for (i in 0 until 7) last7.add(OverviewDay(dayLabels[i], dayOrders[i], dayVerified[i], dayRevenue[i]))

        val methodCounts = LinkedHashMap<String, Int>()
        val methodAmounts = HashMap<String, Double>()
        val packageCounts = LinkedHashMap<String, Int>()
        val packageAmounts = HashMap<String, Double>()
        for (o in orders) {
            val method = o.paymentMethod ?: "unknown"
            methodCounts[method] = (methodCounts[method] ?: 0) + 1
            packageCounts[o.packageName] = (packageCounts[o.packageName] ?: 0) + 1
            if (o.status == "verified") {
                methodAmounts[method] = (methodAmounts[method] ?: 0.0) + o.amountEtb
                packageAmounts[o.packageName] = (packageAmounts[o.packageName] ?: 0.0) + o.amountEtb
            }
        }
        val byMethod = methodCounts.entries.sortedByDescending { it.value }
            .map { NamedCount(it.key, it.value, methodAmounts[it.key] ?: 0.0) }
        val byPackage = packageCounts.entries.sortedByDescending { it.value }.take(8)
            .map { NamedCount(it.key, it.value, packageAmounts[it.key] ?: 0.0) }

        return OrderStats(
            total = orders.size,
            truncated = orders.size >= ROW_LIMIT,
            pending = pending.size,
            verified = verified.size,
            revenueVerified = revenue,
            pipeline = pipeline,
            avgOrder = average,
            conversionPct = conversion,
            todayCount = todayCount,
            todayRevenue = todayRevenue,
            urgentPending = urgent,
            last7 = last7,
            byMethod = byMethod,
            byPackage = byPackage
        )
    }

    suspend fun load(): OverviewResult {
        try {
            return coroutineScope {
                // One refresh up front so the parallel calls below do not each try to refresh the login.
                AuthStateManager.refreshIfNeeded()
                val ordersJob = async {
                    AdminSupabase.get(
                        "orders?select=id,package_name,amount_etb,status,payment_method,student_name,created_at" +
                            "&order=created_at.desc&limit=$ROW_LIMIT"
                    )
                }
                val profilesJob = async {
                    AdminSupabase.get(
                        "profiles?select=id,email,full_name,school_name,stream,created_at&order=created_at.desc&limit=$ROW_LIMIT"
                    )
                }
                val enrollJob = async {
                    AdminSupabase.get("enrollments?select=id,package_name,email,created_at&order=created_at.desc&limit=8")
                }
                val inquiryJob = async {
                    AdminSupabase.get("inquiries?select=id,name,service,message,created_at&order=created_at.desc&limit=8")
                }
                val scholarsJob = async { AdminSupabase.count("profiles?select=id") }
                val enrollCountJob = async { AdminSupabase.count("enrollments?select=id") }
                val explainJob = async { AdminSupabase.count("question_explanations?select=question_id") }
                val quizJob = async { AdminSupabase.count("academic_results?select=id") }
                val inquiryTotalJob = async { AdminSupabase.count("inquiries?select=id") }
                val inquiryNewJob = async { AdminSupabase.count("inquiries?select=id&status=eq.new") }

                val ordersReply = ordersJob.await()
                val profilesReply = profilesJob.await()
                val enrollReply = enrollJob.await()
                val inquiryReply = inquiryJob.await()
                val scholars = scholarsJob.await()
                val enrollCount = enrollCountJob.await()
                val explanations = explainJob.await()
                val quizzes = quizJob.await()
                val inquiryTotal = inquiryTotalJob.await()
                val inquiryNew = inquiryNewJob.await()

                val nothingRead = !ordersReply.ok && !profilesReply.ok && scholars == null && enrollCount == null &&
                    explanations == null && quizzes == null && inquiryTotal == null && inquiryNew == null
                if (nothingRead) {
                    return@coroutineScope OverviewResult.Failure(ordersReply.errorText())
                }

                val feed = ArrayList<FeedItem>()

                var orderStats: OrderStats? = null
                if (ordersReply.ok) {
                    val orders = parseOrders(ordersReply.body)
                    if (orders != null) {
                        orderStats = computeOrderStats(orders)
                        for (o in orders.take(10)) {
                            feed.add(
                                FeedItem(
                                    id = "o-${o.id}",
                                    kind = "order",
                                    whenIso = o.createdAt,
                                    text = "Order ${o.id} \u00B7 ${o.packageName} \u00B7 ${o.studentName}",
                                    detail = "${statusLabel(o.status)} (${etb(o.amountEtb)}) via ${o.paymentMethod ?: "bank"}"
                                )
                            )
                        }
                    }
                }

                var profileStats: ProfileStats? = null
                if (profilesReply.ok) {
                    try {
                        val arr = JSONArray(profilesReply.body)
                        val streamCounts = LinkedHashMap<String, Int>()
                        for (i in 0 until arr.length()) {
                            val p = arr.getJSONObject(i)
                            val label = streamLabel(p.text("stream"))
                            streamCounts[label] = (streamCounts[label] ?: 0) + 1
                            if (i < 10) {
                                val name = p.text("full_name")
                                val mail = p.text("email")
                                val place = listOfNotNull(p.text("school_name"), p.text("stream")).joinToString(" \u00B7 ")
                                feed.add(
                                    FeedItem(
                                        id = "u-${p.text("id") ?: i}",
                                        kind = "user",
                                        whenIso = p.text("created_at") ?: "",
                                        text = "New student registration: ${name ?: mail ?: "Scholar"}",
                                        detail = if (place.isNotEmpty()) place else mail ?: "Registered profile"
                                    )
                                )
                            }
                        }
                        profileStats = ProfileStats(
                            loaded = arr.length(),
                            truncated = arr.length() >= ROW_LIMIT,
                            byStream = streamCounts.entries.sortedByDescending { it.value }
                                .map { NamedCount(it.key, it.value, 0.0) }
                        )
                    } catch (e: Exception) {
                        profileStats = null
                    }
                }

                if (enrollReply.ok) {
                    try {
                        val arr = JSONArray(enrollReply.body)
                        for (i in 0 until arr.length()) {
                            val e = arr.getJSONObject(i)
                            feed.add(
                                FeedItem(
                                    id = "e-${e.text("id") ?: i}",
                                    kind = "enroll",
                                    whenIso = e.text("created_at") ?: "",
                                    text = "Unlocked \u00B7 ${e.text("package_name") ?: "Package"}",
                                    detail = e.text("email") ?: "Active Enrollment"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // feed simply has no grants
                    }
                }

                if (inquiryReply.ok) {
                    try {
                        val arr = JSONArray(inquiryReply.body)
                        for (i in 0 until arr.length()) {
                            val q = arr.getJSONObject(i)
                            val message = q.text("message") ?: ""
                            val shortMessage = if (message.length > 90) message.take(90) + "\u2026" else message
                            feed.add(
                                FeedItem(
                                    id = "i-${q.text("id") ?: i}",
                                    kind = "inquiry",
                                    whenIso = q.text("created_at") ?: "",
                                    text = "Inquiry from ${q.text("name") ?: "Visitor"} (${q.text("service") ?: "General"})",
                                    detail = shortMessage.ifEmpty { null }
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // feed simply has no inquiries
                    }
                }

                feed.sortByDescending { it.whenIso }

                OverviewResult.Success(
                    OverviewStats(
                        orders = orderStats,
                        profiles = profileStats,
                        scholars = scholars,
                        enrollments = enrollCount,
                        explanations = explanations,
                        quizzes = quizzes,
                        inquiriesTotal = inquiryTotal,
                        inquiriesNew = inquiryNew,
                        feed = feed
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return OverviewResult.Failure("Could not load the overview")
        }
    }
}
