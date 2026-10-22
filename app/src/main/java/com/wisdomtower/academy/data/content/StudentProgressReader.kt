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

/** One row of `public.learning_progress` (the student's own), reduced to what the "Your status" screens need. */
data class ProgressRow(
    val resourceId: String,
    val progressPct: Double,
    val totalSeconds: Long,
    val focusSeconds: Long,
    val lastOpenedAt: String?,
    /** `meta.quiz.attempted` (0 when the row has no quiz data) */
    val quizAttempted: Int,
    /** `meta.quiz.correct` (0 when the row has no quiz data) */
    val quizCorrect: Int,
    /** My Status V2: `last_page` (PDF), null when the row has none */
    val lastPage: Int? = null,
    /** My Status V2: `meta.flashcards.know` (0 when the row has no flashcard data) */
    val flashcardsKnow: Int = 0,
    /** My Status V2: `meta.flashcards.seen` (0 when the row has no flashcard data) */
    val flashcardsSeen: Int = 0
)

sealed class ProgressFetchResult {
    data class Success(val rows: List<ProgressRow>) : ProgressFetchResult()
    object NotSignedIn : ProgressFetchResult()
    object Failed : ProgressFetchResult()
}

/**
 * Reads ALL of the signed-in student's rows of `public.learning_progress` (own rows only, policy `auth.uid() = user_id`),
 * the same select as `StudentAnalyticsDashboard.tsx` (`resource_id, progress_pct, total_seconds, focus_seconds,
 * last_opened_at, meta`) plus `last_page`. Used by the native "Your status" screen (F7 part 5). Uses the student's own
 * access token (refreshed when needed, one retry after a 401). Never logs tokens or bodies, only HTTP status codes.
 * Never throws.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object StudentProgressReader {
    private const val TAG = "StudentProgress"
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

    private class ReadOutcome(val status: Int, val rows: List<ProgressRow>)

    suspend fun fetchAll(): ProgressFetchResult = withContext(Dispatchers.IO) {
        try {
            AuthStateManager.refreshIfNeeded()
            val first = AuthStateManager.currentUser.value
            val token1 = first?.accessToken
            if (first == null || token1.isNullOrBlank() || !UUID_REGEX.matches(first.id)) {
                return@withContext ProgressFetchResult.NotSignedIn
            }
            var outcome = read(first.id, token1)
            if (outcome.status == STATUS_UNAUTHORIZED && AuthStateManager.refreshIfNeeded(force = true)) {
                val second = AuthStateManager.currentUser.value
                val token2 = second?.accessToken
                if (second != null && !token2.isNullOrBlank() && UUID_REGEX.matches(second.id)) {
                    outcome = read(second.id, token2)
                }
            }
            if (outcome.status == STATUS_OK) ProgressFetchResult.Success(outcome.rows) else ProgressFetchResult.Failed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchAll failed: ${e.javaClass.simpleName}")
            ProgressFetchResult.Failed
        }
    }

    private fun read(userId: String, token: String): ReadOutcome {
        val url = "$supabaseUrl/rest/v1/learning_progress".toHttpUrl().newBuilder()
            .addQueryParameter("select", "resource_id,progress_pct,total_seconds,focus_seconds,last_opened_at,last_page,meta")
            .addQueryParameter("user_id", "eq.$userId")
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
            val rows = ArrayList<ProgressRow>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val meta = o.optJSONObject("meta")
                val quiz = meta?.optJSONObject("quiz")
                val cards = meta?.optJSONObject("flashcards")
                val pct = o.optDouble("progress_pct", 0.0)
                val opened = if (o.isNull("last_opened_at")) null else o.optString("last_opened_at", "").ifBlank { null }
                rows.add(
                    ProgressRow(
                        resourceId = o.optString("resource_id", ""),
                        progressPct = if (pct.isNaN()) 0.0 else pct,
                        totalSeconds = o.optLong("total_seconds", 0L),
                        focusSeconds = o.optLong("focus_seconds", 0L),
                        lastOpenedAt = opened,
                        quizAttempted = quiz?.optInt("attempted", 0) ?: 0,
                        quizCorrect = quiz?.optInt("correct", 0) ?: 0,
                        lastPage = if (o.isNull("last_page")) null else o.optInt("last_page"),
                        flashcardsKnow = cards?.optInt("know", 0) ?: 0,
                        flashcardsSeen = cards?.optInt("seen", 0) ?: 0
                    )
                )
            }
            return ReadOutcome(STATUS_OK, rows)
        }
    }
}
