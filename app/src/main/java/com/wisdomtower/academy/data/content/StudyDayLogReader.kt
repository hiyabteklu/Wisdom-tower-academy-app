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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** One row of `public.study_day_log` (the student's own): [day] is `yyyy-MM-dd` (the phone's local date when it was written). */
data class StudyDayRow(
    val day: String,
    val seconds: Int,
    val focusSeconds: Int
)

sealed class StudyDayLogFetchResult {
    /**
     * OFFLINE STATUS (O1): [fromCache] is true when the server could not be reached and these rows are the saved copy of
     * the latest sync ([savedAtMs] = when it was saved). Both have defaults, so every old `Success(rows)` still compiles.
     */
    data class Success(
        val rows: List<StudyDayRow>,
        val fromCache: Boolean = false,
        val savedAtMs: Long? = null
    ) : StudyDayLogFetchResult()
    object NotSignedIn : StudyDayLogFetchResult()
    object Failed : StudyDayLogFetchResult()
}

/**
 * My Status V2 (S6c): reads the signed-in student's own recent `study_day_log` rows (policy `auth.uid() = user_id`), same
 * pattern as [ExamAttemptsReader] (own token, refresh when needed, one retry after a 401, only HTTP codes logged, never
 * throws). Days without a row simply are not in the list: the UI shows them as empty, never as a made-up value (G4).
 *
 * OFFLINE STATUS (2026-10-23, docs/OFFLINE_STATUS_PLAN.md slice O1): every good read is saved on the phone
 * ([StatusSnapshotStore], kind `daylog`, per student). When a read fails and a saved copy of the SAME student exists it is
 * returned as `Success(fromCache = true, savedAtMs = ...)`, keeping only the days inside the asked window; otherwise
 * `Failed` as before. `NotSignedIn` never uses a copy.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object StudyDayLogReader {
    private const val TAG = "StudyDayLog"
    private const val KIND = "daylog"
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

    private class ReadOutcome(val status: Int, val rows: List<StudyDayRow>)

    /** The last [days] days including today (default 14), oldest first. */
    suspend fun fetchRecent(days: Int = 14): StudyDayLogFetchResult = withContext(Dispatchers.IO) {
        var userId: String? = null
        try {
            AuthStateManager.refreshIfNeeded()
            val first = AuthStateManager.currentUser.value
            val token1 = first?.accessToken
            if (first == null || token1.isNullOrBlank() || !UUID_REGEX.matches(first.id)) {
                return@withContext StudyDayLogFetchResult.NotSignedIn
            }
            userId = first.id
            val since = sinceDay(days)
            var outcome = read(first.id, token1, since)
            if (outcome.status == STATUS_UNAUTHORIZED && AuthStateManager.refreshIfNeeded(force = true)) {
                val second = AuthStateManager.currentUser.value
                val token2 = second?.accessToken
                if (second != null && !token2.isNullOrBlank() && UUID_REGEX.matches(second.id)) {
                    outcome = read(second.id, token2, since)
                    userId = second.id
                }
            }
            if (outcome.status == STATUS_OK) {
                saveSnapshot(userId, outcome.rows)
                StudyDayLogFetchResult.Success(outcome.rows)
            } else {
                cachedOrFailed(userId, since)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchRecent failed: ${e.javaClass.simpleName}")
            cachedOrFailed(userId ?: AuthStateManager.currentUser.value?.id, sinceDay(days))
        }
    }

    /** Local date `days - 1` days ago (so [days] = 7 gives today and the 6 days before). */
    private fun sinceDay(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(maxOf(days, 1) - 1))
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    /** Saves the latest good read for this student. Never throws. */
    private fun saveSnapshot(userId: String, rows: List<StudyDayRow>) {
        runCatching {
            val arr = JSONArray()
            for (r in rows) {
                arr.put(JSONObject().put("day", r.day).put("s", r.seconds).put("f", r.focusSeconds))
            }
            StatusSnapshotStore.save(userId, KIND, arr)
        }
    }

    /** The saved copy of this student (only days from [since] on) as a result, or `Failed` when there is none. */
    private fun cachedOrFailed(userId: String?, since: String): StudyDayLogFetchResult {
        if (userId == null || !UUID_REGEX.matches(userId)) return StudyDayLogFetchResult.Failed
        val saved = runCatching { StatusSnapshotStore.load(userId, KIND) }.getOrNull()
            ?: return StudyDayLogFetchResult.Failed
        val rows = runCatching {
            val list = ArrayList<StudyDayRow>(saved.rows.length())
            for (i in 0 until saved.rows.length()) {
                val o = saved.rows.getJSONObject(i)
                val day = o.optString("day", "")
                if (day.length < 10 || day < since) continue
                list.add(StudyDayRow(day.substring(0, 10), o.optInt("s", 0), o.optInt("f", 0)))
            }
            list
        }.getOrNull() ?: return StudyDayLogFetchResult.Failed
        return StudyDayLogFetchResult.Success(rows, fromCache = true, savedAtMs = saved.savedAtMs)
    }

    private fun read(userId: String, token: String, sinceDay: String): ReadOutcome {
        val url = "$supabaseUrl/rest/v1/study_day_log".toHttpUrl().newBuilder()
            .addQueryParameter("select", "day,seconds,focus_seconds")
            .addQueryParameter("user_id", "eq.$userId")
            .addQueryParameter("day", "gte.$sinceDay")
            .addQueryParameter("order", "day.asc")
            .addQueryParameter("limit", "60")
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
            val rows = ArrayList<StudyDayRow>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val day = o.optString("day", "")
                if (day.length < 10) continue
                rows.add(StudyDayRow(day.substring(0, 10), o.optInt("seconds", 0), o.optInt("focus_seconds", 0)))
            }
            return ReadOutcome(STATUS_OK, rows)
        }
    }
}
