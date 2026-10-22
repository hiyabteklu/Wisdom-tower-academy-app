package com.wisdomtower.academy.data.content

import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Saves the student's study progress to `public.learning_progress` (own rows only, policy `auth.uid() = user_id`),
 * the same table and the same rules as `saveProgress` in `website-reference/src/lib/content.ts`:
 * read the existing row, add the seconds, keep the HIGHER progress percent, merge `meta` over the old `meta`,
 * then upsert on `user_id,resource_id`. One difference on purpose: `last_page` is only sent when given, so a
 * flashcard save never erases a page number saved by the PDF reader (plan step N4 part 2 added the `lastPage`
 * argument and [getLastPage]).
 *
 * Uses the student's own access token (refreshed when needed, one retry after a 401). Saves are queued one
 * after the other so a quick series of cards cannot overwrite a newer save with an older one.
 * Never logs tokens or bodies, only HTTP status codes.
 *
 * STATUS: written, NOT compiled, NOT tested on a device (plan steps N3 and N4).
 */
object LearningProgressRepository {
    private const val TAG = "LearningProgress"
    private const val STATUS_OK = 0
    private const val STATUS_UNAUTHORIZED = 1
    private const val STATUS_FAILED = 2

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val mutex = Mutex()

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    /**
     * Returns true when the server accepted the progress. [meta] is merged over the stored meta (for
     * flashcards `{ "flashcards": { know, learning, again, seen, total, accuracy } }`). [lastPage] (1-based page
     * number of a PDF) is written to `last_page` only when it is not null.
     */
    suspend fun saveProgress(
        resourceId: String,
        progressPct: Int,
        meta: JSONObject? = null,
        addSeconds: Int = 0,
        addFocusSeconds: Int = 0,
        lastPage: Int? = null
    ): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!UUID_REGEX.matches(resourceId)) return@withContext false
            try {
                AuthStateManager.refreshIfNeeded()
                val first = AuthStateManager.currentUser.value
                val token1 = first?.accessToken
                if (first == null || token1.isNullOrBlank() || !UUID_REGEX.matches(first.id)) {
                    return@withContext false
                }
                var status = write(first.id, token1, resourceId, progressPct, meta, addSeconds, addFocusSeconds, lastPage)
                if (status == STATUS_UNAUTHORIZED && AuthStateManager.refreshIfNeeded(force = true)) {
                    val second = AuthStateManager.currentUser.value
                    val token2 = second?.accessToken
                    if (second != null && !token2.isNullOrBlank() && UUID_REGEX.matches(second.id)) {
                        status = write(second.id, token2, resourceId, progressPct, meta, addSeconds, addFocusSeconds, lastPage)
                    }
                }
                if (status == STATUS_OK && addSeconds > 0) { // MYSTATUS-S6B
                    val dayToken = AuthStateManager.currentUser.value?.accessToken
                    if (!dayToken.isNullOrBlank()) logDay(dayToken, addSeconds, addFocusSeconds)
                }
                status == STATUS_OK
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "saveProgress failed: ${e.javaClass.simpleName}")
                false
            }
        }
    }

    /**
     * The 1-based page number the student last reached in this PDF (`learning_progress.last_page`), or null when
     * nothing is saved, the student is signed out, or the server cannot be reached. Never throws.
     */
    suspend fun getLastPage(resourceId: String): Int? = withContext(Dispatchers.IO) {
        if (!UUID_REGEX.matches(resourceId)) return@withContext null
        try {
            AuthStateManager.refreshIfNeeded()
            val user = AuthStateManager.currentUser.value
            val token = user?.accessToken
            if (user == null || token.isNullOrBlank() || !UUID_REGEX.matches(user.id)) {
                return@withContext null
            }
            val url = "$supabaseUrl/rest/v1/learning_progress".toHttpUrl().newBuilder()
                .addQueryParameter("select", "last_page")
                .addQueryParameter("user_id", "eq.${user.id}")
                .addQueryParameter("resource_id", "eq.$resourceId")
                .addQueryParameter("limit", "1")
                .build()
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .get()
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "getLastPage failed: HTTP ${resp.code}")
                    return@withContext null
                }
                val rows = JSONArray(resp.body?.string().orEmpty())
                if (rows.length() == 0) return@withContext null
                val row = rows.getJSONObject(0)
                if (row.isNull("last_page")) return@withContext null
                val page = row.optInt("last_page", 0)
                if (page >= 1) page else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "getLastPage failed: ${e.javaClass.simpleName}")
            null
        }
    }

    /** One read + one upsert. Returns [STATUS_OK], [STATUS_UNAUTHORIZED] (HTTP 401) or [STATUS_FAILED]. */
    private fun write(
        userId: String,
        token: String,
        resourceId: String,
        progressPct: Int,
        meta: JSONObject?,
        addSeconds: Int,
        addFocusSeconds: Int,
        lastPage: Int?
    ): Int {
        val table = "$supabaseUrl/rest/v1/learning_progress"

        var totalSeconds = 0L
        var focusSeconds = 0L
        var previousPct = 0.0
        var mergedMeta = JSONObject()

        val readUrl = table.toHttpUrl().newBuilder()
            .addQueryParameter("select", "total_seconds,focus_seconds,meta,progress_pct")
            .addQueryParameter("user_id", "eq.$userId")
            .addQueryParameter("resource_id", "eq.$resourceId")
            .addQueryParameter("limit", "1")
            .build()
        val readRequest = Request.Builder()
            .url(readUrl)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .get()
            .build()
        http.newCall(readRequest).execute().use { resp ->
            if (resp.code == 401) return STATUS_UNAUTHORIZED
            if (!resp.isSuccessful) {
                Log.w(TAG, "read failed: HTTP ${resp.code}")
                return STATUS_FAILED
            }
            val rows = JSONArray(resp.body?.string().orEmpty())
            if (rows.length() > 0) {
                val row = rows.getJSONObject(0)
                totalSeconds = row.optLong("total_seconds", 0L)
                focusSeconds = row.optLong("focus_seconds", 0L)
                previousPct = row.optDouble("progress_pct", 0.0).let { if (it.isNaN()) 0.0 else it }
                row.optJSONObject("meta")?.let { mergedMeta = it }
            }
        }

        if (meta != null) {
            val keys = meta.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                mergedMeta.put(key, meta.get(key))
            }
        }

        val nextPct = maxOf(previousPct, progressPct.coerceIn(0, 100).toDouble())
        val body = JSONObject()
            .put("user_id", userId)
            .put("resource_id", resourceId)
            .put("progress_pct", if (nextPct % 1.0 == 0.0) nextPct.toLong() else nextPct)
            .put("total_seconds", totalSeconds + maxOf(0, addSeconds))
            .put("focus_seconds", focusSeconds + maxOf(0, addFocusSeconds))
            .put("last_opened_at", nowIso())
            .put("meta", mergedMeta)
        if (lastPage != null && lastPage >= 1) {
            body.put("last_page", lastPage)
        }

        val writeUrl = table.toHttpUrl().newBuilder()
            .addQueryParameter("on_conflict", "user_id,resource_id")
            .build()
        val writeRequest = Request.Builder()
            .url(writeUrl)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        http.newCall(writeRequest).execute().use { resp ->
            return when {
                resp.code == 401 -> STATUS_UNAUTHORIZED
                resp.isSuccessful -> STATUS_OK
                else -> {
                    Log.w(TAG, "write failed: HTTP ${resp.code}")
                    STATUS_FAILED
                }
            }
        }
    }

    /** MYSTATUS-S6B: adds the seconds to today's row of study_day_log (day = the phone's local date). Never throws, never changes saveProgress. */
    private fun logDay(token: String, addSeconds: Int, addFocusSeconds: Int) {
        try {
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val body = JSONObject()
                .put("p_day", day)
                .put("p_seconds", addSeconds)
                .put("p_focus", maxOf(0, addFocusSeconds))
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/rpc/add_study_seconds")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) Log.w(TAG, "day log failed: HTTP ${resp.code}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "day log failed: ${e.javaClass.simpleName}")
        }
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
}
