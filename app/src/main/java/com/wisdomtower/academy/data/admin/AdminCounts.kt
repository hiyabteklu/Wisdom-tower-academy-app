package com.wisdomtower.academy.data.admin

import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * The three REAL counts the website admin home reads (website-reference/src/app/admin/page.tsx):
 * pending payments (orders.status = pending_verification), new inquiries (inquiries.status = new), scholars (profiles).
 * A count that could not be read is null (the screen shows a dash), never a made-up number.
 */
data class AdminCounts(
    val scholars: Int?,
    val pendingPayments: Int?,
    val newInquiries: Int?
)

/**
 * ADMIN-native N1. Reads the counts with the signed-in admin's own token through PostgREST (HEAD plus Prefer count=exact,
 * the same request supabase-js sends for `head: true, count: "exact"`). Row-level security decides what is visible,
 * so a student gets nothing. No secret key is used. Same token handling as AdminAccess.kt (refresh, retry once on 401).
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object AdminCountsRepository {
    private const val TAG = "AdminCounts"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private val EMPTY = AdminCounts(null, null, null)

    private class CountReply(val code: Int, val total: Int?)
    private class Fetched(val counts: AdminCounts, val unauthorized: Boolean)

    private fun headCount(path: String, token: String): CountReply {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/$path")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .addHeader("Prefer", "count=exact")
            .head()
            .build()
        http.newCall(request).execute().use { resp ->
            // Content-Range looks like 0-4/5 or */0: the number after the slash is the total.
            val total = resp.header("Content-Range")?.substringAfter('/', "")?.trim()?.toIntOrNull()
            return CountReply(resp.code, total)
        }
    }

    private fun safeCount(path: String, token: String): CountReply {
        return try {
            headCount(path, token)
        } catch (e: Exception) {
            Log.w(TAG, "count failed: ${e.javaClass.simpleName}")
            CountReply(-1, null)
        }
    }

    private fun okTotal(reply: CountReply): Int? = if (reply.code in 200..299) reply.total else null

    private suspend fun fetchAll(token: String): Fetched = coroutineScope {
        val pay = async { safeCount("orders?select=id&status=eq.pending_verification", token) }
        val inq = async { safeCount("inquiries?select=id&status=eq.new", token) }
        val usr = async { safeCount("profiles?select=id", token) }
        val p = pay.await()
        val i = inq.await()
        val u = usr.await()
        Fetched(
            counts = AdminCounts(
                scholars = okTotal(u),
                pendingPayments = okTotal(p),
                newInquiries = okTotal(i)
            ),
            unauthorized = p.code == 401 || i.code == 401 || u.code == 401
        )
    }

    suspend fun load(): AdminCounts = withContext(Dispatchers.IO) {
        try {
            AuthStateManager.refreshIfNeeded()
            val token = AuthStateManager.currentUser.value?.accessToken
            if (token.isNullOrBlank()) return@withContext EMPTY
            val first = fetchAll(token)
            if (first.unauthorized && AuthStateManager.refreshIfNeeded(force = true)) {
                val token2 = AuthStateManager.currentUser.value?.accessToken
                if (!token2.isNullOrBlank()) {\n                    val refreshed = fetchAll(token2).counts\n                    return@withContext refreshed.copy(\n                        pendingExamReviews = AdminSecurityRepository.pendingExamStudentCount(),\n                        pendingDeviceAttempts = AdminSecurityRepository.pendingDeviceStudentCount()\n                    )\n                }
            }
            first.counts
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "load failed: ${e.javaClass.simpleName}")
            EMPTY
        }
    }
}
