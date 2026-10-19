package com.wisdomtower.academy.data.study

import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.ui.study.StudyBlock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Outcome of reading the student's `study_plans` row. */
sealed class PlanFetchResult {
    data class Found(val blocks: List<StudyBlock>, val version: Int) : PlanFetchResult()

    /** The call worked but this student has no row yet. */
    object NotFound : PlanFetchResult()

    object SessionExpired : PlanFetchResult()

    /** Network or server problem: nothing is known about the row. */
    object Failed : PlanFetchResult()
}

/** Outcome of writing the student's blocks. */
sealed class PlanSaveResult {
    object Saved : PlanSaveResult()

    /** The row changed on the server since we read it (admin push, other phone): read again and merge. */
    object Conflict : PlanSaveResult()

    object SessionExpired : PlanSaveResult()

    object Failed : PlanSaveResult()
}

/**
 * Study Planner on the server: table `public.study_plans` (one row per student: user_id, blocks jsonb, version,
 * updated_at, last_pushed_*). Same Supabase project as the website, public URL + anon key + the student's own
 * access token (row-level security lets a student read and write only their own row; admins push plans through
 * the RPC `admin_push_study_plan`, which bumps `version`). No secrets in the app.
 *
 * Block JSON shape = [StudyBlock] = the website's StudyBlock = what `admin_push_study_plan` writes:
 * { id, day 0..6 (0 = Monday), startTime "HH:MM", endTime "HH:MM", title, color }.
 *
 * A student's own save NEVER sends `version` (only admin pushes change it). Updates are guarded with
 * `version=eq.<version we read>` so a push that arrived in between is never overwritten.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object SupabasePlanClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val HHMM = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")

    private const val TAG = "SupabasePlan"
    private const val MAX_BLOCKS = 300 // same limit as the table check

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    suspend fun fetch(userId: String, accessToken: String): PlanFetchResult = withContext(Dispatchers.IO) {
        if (!UUID_REGEX.matches(userId) || accessToken.isBlank()) return@withContext PlanFetchResult.Failed
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/study_plans?user_id=eq.$userId&select=blocks,version")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .get()
                .build()
            http.newCall(request).execute().use { resp ->
                when {
                    resp.code == 401 -> PlanFetchResult.SessionExpired
                    !resp.isSuccessful -> {
                        Log.w(TAG, "fetch failed: HTTP ${resp.code}")
                        PlanFetchResult.Failed
                    }
                    else -> {
                        val arr = JSONArray(resp.body?.string().orEmpty())
                        if (arr.length() == 0) {
                            PlanFetchResult.NotFound
                        } else {
                            val row = arr.getJSONObject(0)
                            PlanFetchResult.Found(parseBlocks(row.optJSONArray("blocks")), row.optInt("version", 1))
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            PlanFetchResult.Failed
        }
    }

    /** First save of a student who has no row yet. 409 means a row appeared meanwhile (admin push): [PlanSaveResult.Conflict]. */
    suspend fun insert(userId: String, accessToken: String, blocks: List<StudyBlock>): PlanSaveResult =
        withContext(Dispatchers.IO) {
            if (!UUID_REGEX.matches(userId) || accessToken.isBlank()) return@withContext PlanSaveResult.Failed
            try {
                val body = JSONObject().put("user_id", userId).put("blocks", toJson(blocks))
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/study_plans")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body.toString().toRequestBody(JSON_MEDIA))
                    .build()
                http.newCall(request).execute().use { resp ->
                    when {
                        resp.code == 401 -> PlanSaveResult.SessionExpired
                        resp.code == 409 -> PlanSaveResult.Conflict
                        resp.isSuccessful -> PlanSaveResult.Saved
                        else -> {
                            Log.w(TAG, "insert failed: HTTP ${resp.code}")
                            PlanSaveResult.Failed
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                PlanSaveResult.Failed
            }
        }

    /**
     * Writes the blocks only if the row is still at [expectedVersion]. Success only when the server really returned
     * the row (a refused or non-matching update returns no row and is reported as [PlanSaveResult.Conflict]).
     */
    suspend fun update(
        userId: String,
        accessToken: String,
        blocks: List<StudyBlock>,
        expectedVersion: Int
    ): PlanSaveResult = withContext(Dispatchers.IO) {
        if (!UUID_REGEX.matches(userId) || accessToken.isBlank()) return@withContext PlanSaveResult.Failed
        try {
            val body = JSONObject().put("blocks", toJson(blocks)).put("updated_at", nowIso())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/study_plans?user_id=eq.$userId&version=eq.$expectedVersion")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                when {
                    resp.code == 401 -> PlanSaveResult.SessionExpired
                    resp.isSuccessful -> if (rowCount(text) > 0) PlanSaveResult.Saved else PlanSaveResult.Conflict
                    else -> {
                        Log.w(TAG, "update failed: HTTP ${resp.code}")
                        PlanSaveResult.Failed
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            PlanSaveResult.Failed
        }
    }

    // ---------- internals ----------

    /** Blocks that cannot be shown or scheduled (missing id, bad HH:MM) are skipped, never crash the planner. */
    private fun parseBlocks(arr: JSONArray?): List<StudyBlock> {
        if (arr == null) return emptyList()
        val out = ArrayList<StudyBlock>()
        for (i in 0 until minOf(arr.length(), MAX_BLOCKS)) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id", "")
            val start = o.optString("startTime", "")
            val end = o.optString("endTime", "")
            if (id.isEmpty() || !HHMM.matches(start) || !HHMM.matches(end)) continue
            out.add(
                StudyBlock(
                    id = id,
                    day = o.optInt("day", 0).coerceIn(0, 6),
                    startTime = start,
                    endTime = end,
                    title = o.optString("title", "Study Session").ifBlank { "Study Session" },
                    color = o.optString("color", "")
                )
            )
        }
        return out
    }

    private fun toJson(blocks: List<StudyBlock>): JSONArray {
        val arr = JSONArray()
        blocks.take(MAX_BLOCKS).forEach { b ->
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("day", b.day)
                    .put("startTime", b.startTime)
                    .put("endTime", b.endTime)
                    .put("title", b.title)
                    .put("color", b.color)
            )
        }
        return arr
    }

    private fun rowCount(text: String): Int = try {
        JSONArray(text).length()
    } catch (_: Exception) {
        0
    }

    private fun nowIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())
}
