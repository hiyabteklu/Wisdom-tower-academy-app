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
 * A4 (docs/PLAN_2026-11-28_SIGNUP_AND_GAMES_NAV.md): the text a student writes when the academic level is "Other".
 *
 * Table `public.profiles`, column `custom_education_level` (text, at most 80 characters, CHECK added by the A2 migration).
 * Sign-up (SignUpProfileWriter) already saves the text there. The Profile screen reads and writes it through this file,
 * the same way My Subjects uses ProfileSubjectsSync, so the main profile client (SupabaseAccountClient) stays untouched.
 * The student's own token is used; the existing row rules (own row only, admins see all) cover it.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object ProfileLevelSync {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private const val SESSION_EXPIRED = "Your session expired. Please sign in again."
    private const val NO_INTERNET = "Check your internet connection and try again."
    private const val SAVE_FAILED = "Could not save your academic level text to the server."

    /** Longest allowed text (same limit as the database CHECK). */
    const val MAX_LENGTH = 80

    /** The student's saved "Other" text, or null when there is none (or the call failed). */
    suspend fun loadCustom(): String? = withContext(Dispatchers.IO) {
        try {
            AuthStateManager.refreshIfNeeded()
            val user = AuthStateManager.currentUser.value
            val token = user?.accessToken
            if (user == null || token.isNullOrBlank()) return@withContext null
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.${user.id}&select=custom_education_level")
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
                if (!row.has("custom_education_level") || row.isNull("custom_education_level")) return@use null
                row.optString("custom_education_level").trim().ifBlank { null }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** Writes the text (blank clears it). Call AFTER the profile row is saved (the row must exist). */
    suspend fun saveCustom(text: String): AccountCallResult =
        withContext(Dispatchers.IO) {
            AuthStateManager.refreshIfNeeded()
            var result = attempt(text)
            if (result is AccountCallResult.Failure && result.sessionExpired &&
                AuthStateManager.refreshIfNeeded(force = true)
            ) {
                result = attempt(text)
            }
            result
        }

    private fun attempt(text: String): AccountCallResult {
        val user = AuthStateManager.currentUser.value
        val token = user?.accessToken
        if (user == null || token.isNullOrBlank()) return AccountCallResult.Failure(SESSION_EXPIRED, true)
        return try {
            val clean = text.trim().take(MAX_LENGTH)
            val body = JSONObject()
                .put("custom_education_level", if (clean.isBlank()) JSONObject.NULL else clean)
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
                val respText = resp.body?.string().orEmpty()
                val rows = try {
                    JSONArray(respText).length()
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
