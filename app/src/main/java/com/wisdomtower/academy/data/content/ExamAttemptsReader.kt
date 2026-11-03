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
    /**
     * OFFLINE STATUS (O1): [fromCache] is true when the server could not be reached and these rows are the saved copy of
     * the latest sync ([savedAtMs] = when it was saved). Both have defaults, so every old `Success(rows)` still compiles.
     */
    data class Success(
        val rows: List<ExamAttemptRow>,
        val fromCache: Boolean = false,
        val savedAtMs: Long? = null
    ) : ExamAttemptsFetchResult()
    object NotSignedIn : ExamAttemptsFetchResult()
    object Failed : ExamAttemptsFetchResult()
}

/**
 * My Status V2 (S1): reads the signed-in student's own `exam_attempts` (policy `auth.uid() = user_id`), same pattern as
 * `StudentProgressReader` (own token, refresh when needed, one retry after a 401, only HTTP codes logged, never throws).
 *
 * OFFLINE STATUS (2026-10-23, docs/OFFLINE_STATUS_PLAN.md slice O1): every good read is saved on the phone
 * ([StatusSnapshotStore], kind `exams`, per student). When a read fails and a saved copy of the SAME student exists it is
 * returned as `Success(fromCache = true, savedAtMs = ...)`; otherwise `Failed` as before. `NotSignedIn` never uses a copy.
 *
 * SAVED COPY FIRST (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md slice P1c, part c1): [fetchAllSaved] returns the saved
 * copy with NO network so a screen can draw it at once, and [fetchAll] uses a SHORT client (4 s connect + 8 s read) when a
 * saved copy of this student exists (15 s + 20 s when nothing is saved, as before).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object ExamAttemptsReader {
    private const val TAG = "ExamAttempts"
    private const val KIND = "exams"
    private const val STATUS_OK = 0
    private const val STATUS_UNAUTHORIZED = 1
    private const val STATUS_FAILED = 2

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** P1c: used instead of [http] when a saved copy of this student exists (the copy is the fallback, so waiting long gains nothing). */
    private val fastHttp = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private class ReadOutcome(val status: Int, val rows: List<ExamAttemptRow>)

    /**
     * P1c: the saved copy of the signed-in student, read from the phone only (no network, no sign-in refresh). Null when
     * nobody is signed in, nothing was saved, the file cannot be read, or the saved list is EMPTY (an empty copy counts as
     * "nothing saved", so a screen keeps its loading state until the server answers instead of showing an old empty answer).
     * The result has `fromCache = true` and `savedAtMs` set. Never throws.
     */
    suspend fun fetchAllSaved(): ExamAttemptsFetchResult.Success? = withContext(Dispatchers.IO) {
        val id = AuthStateManager.currentUser.value?.id ?: return@withContext null
        val saved = cachedOrFailed(id) as? ExamAttemptsFetchResult.Success ?: return@withContext null
        if (saved.rows.isEmpty()) null else saved
    }

    suspend fun fetchAll(): ExamAttemptsFetchResult = withContext(Dispatchers.IO) {
        var userId: String? = null
        try {
            AuthStateManager.refreshIfNeeded()
            val first = AuthStateManager.currentUser.value
            val token1 = first?.accessToken
            if (first == null || token1.isNullOrBlank() || !UUID_REGEX.matches(first.id)) {
                return@withContext ExamAttemptsFetchResult.NotSignedIn
            }
            userId = first.id
            // P1c: short timeouts when a saved copy of this student can answer instead.
            val client = if (runCatching { StatusSnapshotStore.load(first.id, KIND) }.getOrNull() != null) fastHttp else http
            var outcome = read(first.id, token1, client)
            if (outcome.status == STATUS_UNAUTHORIZED && AuthStateManager.refreshIfNeeded(force = true)) {
                val second = AuthStateManager.currentUser.value
                val token2 = second?.accessToken
                if (second != null && !token2.isNullOrBlank() && UUID_REGEX.matches(second.id)) {
                    outcome = read(second.id, token2, client)
                    userId = second.id
                }
            }
            if (outcome.status == STATUS_OK) {
                saveSnapshot(userId, outcome.rows)
                ExamAttemptsFetchResult.Success(outcome.rows)
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
    private fun saveSnapshot(userId: String, rows: List<ExamAttemptRow>) {
        runCatching {
            val arr = JSONArray()
            for (r in rows) {
                val o = JSONObject().put("rid", r.resourceId)
                if (r.startedAt != null) o.put("st", r.startedAt)
                if (r.submittedAt != null) o.put("sub", r.submittedAt)
                if (r.score != null) o.put("score", r.score)
                if (r.total != null) o.put("total", r.total)
                arr.put(o)
            }
            StatusSnapshotStore.save(userId, KIND, arr)
        }
    }

    /** The saved copy of this student as a result, or `Failed` when there is none. */
    private fun cachedOrFailed(userId: String?): ExamAttemptsFetchResult {
        if (userId == null || !UUID_REGEX.matches(userId)) return ExamAttemptsFetchResult.Failed
        val saved = runCatching { StatusSnapshotStore.load(userId, KIND) }.getOrNull()
            ?: return ExamAttemptsFetchResult.Failed
        val rows = runCatching {
            val list = ArrayList<ExamAttemptRow>(saved.rows.length())
            for (i in 0 until saved.rows.length()) {
                val o = saved.rows.getJSONObject(i)
                list.add(
                    ExamAttemptRow(
                        resourceId = o.optString("rid", ""),
                        startedAt = if (o.isNull("st")) null else o.optString("st", "").ifBlank { null },
                        submittedAt = if (o.isNull("sub")) null else o.optString("sub", "").ifBlank { null },
                        score = if (!o.has("score") || o.isNull("score")) null else o.optDouble("score").takeUnless { it.isNaN() },
                        total = if (!o.has("total") || o.isNull("total")) null else o.optInt("total")
                    )
                )
            }
            list
        }.getOrNull() ?: return ExamAttemptsFetchResult.Failed
        return ExamAttemptsFetchResult.Success(rows, fromCache = true, savedAtMs = saved.savedAtMs)
    }

    private fun read(userId: String, token: String, client: OkHttpClient): ReadOutcome {
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
        client.newCall(request).execute().use { resp ->
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
