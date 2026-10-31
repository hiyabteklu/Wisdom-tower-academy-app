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
 * Real admin dashboard counts. A failed read is null, never a fabricated zero.
 */
data class AdminCounts(
    val scholars: Int?,
    val pendingPayments: Int?,
    val newInquiries: Int?,
    val pendingExamReviews: Int? = null,
    val pendingDeviceAttempts: Int? = null
)

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
            val total = resp.header("Content-Range")?.substringAfter('/', "")?.trim()?.toIntOrNull()
            return CountReply(resp.code, total)
        }
    }

    private fun safeCount(path: String, token: String): CountReply = try {
        headCount(path, token)
    } catch (e: Exception) {
        Log.w(TAG, "count failed: ${e.javaClass.simpleName}")
        CountReply(-1, null)
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
            counts = AdminCounts(okTotal(u), okTotal(p), okTotal(i)),
            unauthorized = p.code == 401 || i.code == 401 || u.code == 401
        )
    }

    suspend fun load(): AdminCounts = withContext(Dispatchers.IO) {
        try {
            AuthStateManager.refreshIfNeeded()
            var token = AuthStateManager.currentUser.value?.accessToken
            if (token.isNullOrBlank()) return@withContext EMPTY
            var result = fetchAll(token)
            if (result.unauthorized && AuthStateManager.refreshIfNeeded(force = true)) {
                token = AuthStateManager.currentUser.value?.accessToken
                if (!token.isNullOrBlank()) result = fetchAll(token)
            }
            result.counts.copy(
                pendingExamReviews = AdminSecurityRepository.pendingExamStudentCount(),
                pendingDeviceAttempts = AdminSecurityRepository.pendingDeviceStudentCount()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "load failed: ${e.javaClass.simpleName}")
            EMPTY
        }
    }
}
