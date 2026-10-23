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
import org.json.JSONObject
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
    /**
     * OFFLINE STATUS (O1): [fromCache] is true when the server could not be reached and these rows are the saved copy of
     * the latest sync ([savedAtMs] = when it was saved). Both have defaults, so every old `Success(rows)` still compiles.
     */
    data class Success(
        val rows: List<ProgressRow>,
        val fromCache: Boolean = false,
        val savedAtMs: Long? = null
    ) : ProgressFetchResult()
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
 * OFFLINE STATUS (2026-10-23, docs/OFFLINE_STATUS_PLAN.md slice O1): every good read is saved on the phone
 * ([StatusSnapshotStore], kind `progress`, per student). When a read fails (offline, server error) and a saved copy of the
 * SAME student exists, it is returned as `Success(fromCache = true, savedAtMs = ...)`; otherwise `Failed` as before.
 * `NotSignedIn` never uses a copy. The screens do not show the "from cache" notice yet (slice O2).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object StudentProgressReader {
    private const val TAG = "StudentProgress"
    private const val KIND = "progress"
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
        var userId: String? = null
        try {
            AuthStateManager.refreshIfNeeded()
            val first = AuthStateManager.currentUser.value
            val token1 = first?.accessToken
            if (first == null || token1.isNullOrBlank() || !UUID_REGEX.matches(first.id)) {
                return@withContext ProgressFetchResult.NotSignedIn
            }
            userId = first.id
            var outcome = read(first.id, token1)
            if (outcome.status == STATUS_UNAUTHORIZED && AuthStateManager.refreshIfNeeded(force = true)) {
                val second = AuthStateManager.currentUser.value
                val token2 = second?.accessToken
                if (second != null && !token2.isNullOrBlank() && UUID_REGEX.matches(second.id)) {
                    outcome = read(second.id, token2)
                    userId = second.id
                }
            }
            if (outcome.status == STATUS_OK) {
                saveSnapshot(userId, outcome.rows)
                ProgressFetchResult.Success(outcome.rows)
            } else {
                cachedOrFailed(userId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchAll failed: ${e.javaClass.simpleName}")
            cachedOrFailed(userId ?: AuthStateManager.currentUser.value?.id)
        }
    }

    /** Saves the latest good read for this student. Never throws. */
    private fun saveSnapshot(userId: String, rows: List<ProgressRow>) {
        runCatching {
            val arr = JSONArray()
            for (r in rows) {
                val o = JSONObject()
                    .put("rid", r.resourceId)
                    .put("pct", r.progressPct)
                    .put("tot", r.totalSeconds)
                    .put("foc", r.focusSeconds)
                    .put("qa", r.quizAttempted)
                    .put("qc", r.quizCorrect)
                    .put("fk", r.flashcardsKnow)
                    .put("fs", r.flashcardsSeen)
                if (r.lastOpenedAt != null) o.put("opened", r.lastOpenedAt)
                if (r.lastPage != null) o.put("page", r.lastPage)
                arr.put(o)
            }
            StatusSnapshotStore.save(userId, KIND, arr)
        }
    }

    /** The saved copy of this student as a result, or `Failed` when there is none. */
    private fun cachedOrFailed(userId: String?): ProgressFetchResult {
        if (userId == null || !UUID_REGEX.matches(userId)) return ProgressFetchResult.Failed
        val saved = runCatching { StatusSnapshotStore.load(userId, KIND) }.getOrNull()
            ?: return ProgressFetchResult.Failed
        val rows = runCatching {
            val list = ArrayList<ProgressRow>(saved.rows.length())
            for (i in 0 until saved.rows.length()) {
                val o = saved.rows.getJSONObject(i)
                list.add(
                    ProgressRow(
                        resourceId = o.optString("rid", ""),
                        progressPct = o.optDouble("pct", 0.0).let { if (it.isNaN()) 0.0 else it },
                        totalSeconds = o.optLong("tot", 0L),
                        focusSeconds = o.optLong("foc", 0L),
                        lastOpenedAt = if (o.isNull("opened")) null else o.optString("opened", "").ifBlank { null },
                        quizAttempted = o.optInt("qa", 0),
                        quizCorrect = o.optInt("qc", 0),
                        lastPage = if (!o.has("page") || o.isNull("page")) null else o.optInt("page"),
                        flashcardsKnow = o.optInt("fk", 0),
                        flashcardsSeen = o.optInt("fs", 0)
                    )
                )
            }
            list
        }.getOrNull() ?: return ProgressFetchResult.Failed
        return ProgressFetchResult.Success(rows, fromCache = true, savedAtMs = saved.savedAtMs)
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
