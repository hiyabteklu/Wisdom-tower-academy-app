package com.wisdomtower.academy.data.content

import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/** One row of `public.exam_attempts` (the student's own). `score` is read as the number of correct answers (hypothesis, see docs). */
data class ExamAttemptRow(
    val resourceId: String,
    val startedAt: String?,
    val submittedAt: String?,
    val score: Double?,
    /** `meta.total`, null when the row has none */
    val total: Int?
)

sealed class ExamAttemptsFetchResult {
    data class Success(val rows: List<ExamAttemptRow>) : ExamAttemptsFetchResult()
    object NotSignedIn : ExamAttemptsFetchResult()
    object Failed : ExamAttemptsFetchResult()
}

/**
 * My Status V2 (S1): reads the signed-in student's own `exam_attempts` (policy `auth.uid() = user_id`), same pattern as
 * `StudentProgressReader` (own token, refresh when needed, one retry after a 401, only HTTP codes logged, never throws).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object ExamAttemptsReader {
    private const val TAG = "ExamAttempts"
    private const val STATUS_OK = 0
    private const val STATUS_UNAUTHORIZED = 1
    private const val STATUS_FAILED = 2

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private class ReadOutcome(val status: Int, val rows: List<ExamAttemptRow>)

    suspend fun fetchAll(): ExamAttemptsFetchResult = withContext(Dispatchers.IO) {
        try {
            AuthStateManager.refreshIfNeeded()
            val first = AuthStateManager.currentUser.value
            val token1 = first?.accessToken
            if (first == null || token1.isNullOrBlank() || !UUID_REGEX.matches(first.id)) {
                return@withContext ExamAttemptsFetchResult.NotSignedIn
            }
            var outcome = read(first.id, token1)
            if (outcome.status == STATUS_UNAUTHORIZED && AuthStateManager.refreshIfNeeded(force = true)) {
                val second = AuthStateManager.currentUser.value
                val token2 = second?.accessToken
                if (second != null && !token2.isNullOrBlank() && UUID_REGEX.matches(second.id)) {
                    outcome = read(second.id, token2)
                }
            }
            if (outcome.status == STATUS_OK) ExamAttemptsFetchResult.Success(outcome.rows) else ExamAttemptsFetchResult.Failed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchAll failed: ${e.javaClass.simpleName}")
            ExamAttemptsFetchResult.Failed
        }
    }

    private fun read(userId: String, token: String): ReadOutcome {
        val url = "$supabaseUrl/rest/v1/exam_attempts".toHttpUrl().newBuilder()
            .addQueryParameter("select", "resource_id,started_at,submitted_at,score,meta")
            .addQueryParameter("user_id", "eq.$userId")
            .addQueryParameter("order", "started_at.desc")
            .addQueryParameter("limit", "1000")
            .build()
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .get()
            .build()
        http.newCall(request).execute().use { resp ->
            if (resp.code == 401) return ReadOutcome(STATUS_UNAUTHORIZED, emptyList())
            if (!resp.isSuccessful) {
                Log.w(TAG, "read failed: HTTP ${resp.code}")
                return ReadOutcome(STATUS_FAILED, emptyList())
            }
            val arr = JSONArray(resp.body?.string().orEmpty())
            val rows = ArrayList<ExamAttemptRow>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val score = if (o.isNull("score")) null else o.optDouble("score").takeUnless { it.isNaN() }
                val meta = o.optJSONObject("meta")
                val total = if (meta == null || meta.isNull("total")) null else meta.optInt("total")
                rows.add(
                    ExamAttemptRow(
                        resourceId = o.optString("resource_id", ""),
                        startedAt = if (o.isNull("started_at")) null else o.optString("started_at", "").ifBlank { null },
                        submittedAt = if (o.isNull("submitted_at")) null else o.optString("submitted_at", "").ifBlank { null },
                        score = score,
                        total = total
                    )
                )
            }
            return ReadOutcome(STATUS_OK, rows)
        }
    }
}
