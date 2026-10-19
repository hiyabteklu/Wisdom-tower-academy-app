package com.wisdomtower.academy.data.auth

import com.wisdomtower.academy.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Server copy of "My Subjects" (Profile screen), so the choice follows the student to a new phone.
 *
 * Table `public.profiles` (same project as the website):
 *  - `my_subject_keys text[]`  : the exact subject keys picked in the app (NULL = never chosen).
 *  - `study_subjects text[]`   : package labels (Freshman, Grade 9 ...), the website's own column, kept in step.
 *
 * Kept in its own file so the main profile client stays untouched.
 */
object ProfileSubjectsSync {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private const val SESSION_EXPIRED = "Your session expired. Please sign in again."
    private const val NO_INTERNET = "Check your internet connection and try again."
    private const val SAVE_FAILED = "Could not save your subjects to the server."

    /** The student's saved keys, or null when none were saved yet (or the call failed). */
    suspend fun load(): Set<String>? = withContext(Dispatchers.IO) {
        try {
            AuthStateManager.refreshIfNeeded()
            val user = AuthStateManager.currentUser.value
            val token = user?.accessToken
            if (user == null || token.isNullOrBlank()) return@withContext null
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.${user.id}&select=my_subject_keys")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .get()
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val rows = JSONArray(resp.body?.string().orEmpty())
                if (rows.length() == 0) return@use null
                val row = rows.getJSONObject(0)
                if (!row.has("my_subject_keys") || row.isNull("my_subject_keys")) return@use null
                val arr = row.getJSONArray("my_subject_keys")
                val out = HashSet<String>()
                for (i in 0 until arr.length()) out.add(arr.getString(i))
                out as Set<String>
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** Writes the picks and the matching package labels. Call AFTER the profile row exists (saveProfile creates it). */
    suspend fun save(keys: Set<String>, packageLabels: List<String>): AccountCallResult =
        withContext(Dispatchers.IO) {
            AuthStateManager.refreshIfNeeded()
            var result = attempt(keys, packageLabels)
            if (result is AccountCallResult.Failure && result.sessionExpired &&
                AuthStateManager.refreshIfNeeded(force = true)
            ) {
                result = attempt(keys, packageLabels)
            }
            result
        }

    private fun attempt(keys: Set<String>, packageLabels: List<String>): AccountCallResult {
        val user = AuthStateManager.currentUser.value
        val token = user?.accessToken
        if (user == null || token.isNullOrBlank()) return AccountCallResult.Failure(SESSION_EXPIRED, true)
        return try {
            val body = JSONObject()
                .put("my_subject_keys", JSONArray(keys.toList()))
                .put("study_subjects", JSONArray(packageLabels))
                .put("updated_at", nowIso())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.${user.id}")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val rows = try {
                    JSONArray(text).length()
                } catch (_: Exception) {
                    0
                }
                when {
                    resp.code == 401 -> AccountCallResult.Failure(SESSION_EXPIRED, true)
                    resp.isSuccessful && rows > 0 -> AccountCallResult.Success
                    else -> AccountCallResult.Failure(SAVE_FAILED)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AccountCallResult.Failure(NO_INTERNET)
        } catch (e: Exception) {
            AccountCallResult.Failure(SAVE_FAILED)
        }
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
}
