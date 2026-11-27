package com.wisdomtower.academy.data.auth

import android.util.Log
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
import java.util.concurrent.TimeUnit

/**
 * A3 (docs/PLAN_2026-11-28_SIGNUP_AND_GAMES_NAV.md): right after a successful sign-up, writes the three sign-up answers to
 * the student's own `public.profiles` row with the student's own access token (RLS: profiles_update_own / profiles_insert_own):
 * `education_level`, `gender` and, for Other, `custom_education_level` (at most 80 characters, same CHECK as the database).
 * Nothing else is sent. Best effort: a failure never blocks the sign-up (the level is also in the sign-up metadata and the
 * student can change it in Settings). Returns true only when the server returned the row.
 * No secrets: public URL + anon key from BuildConfig, like [SupabaseAccountClient].
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object SignUpProfileWriter {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private const val TAG = "SignUpProfileWriter"

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    suspend fun write(
        session: SupabaseAuthResult.Success,
        educationLevel: String,
        gender: String?,
        customLevel: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = JSONObject(session.sessionJson).optString("access_token")
            if (token.isBlank() || session.userId.isBlank()) return@withContext false

            val body = JSONObject().put("education_level", educationLevel)
            if (!gender.isNullOrBlank()) body.put("gender", gender)
            if (educationLevel == "Other" && !customLevel.isNullOrBlank()) {
                body.put("custom_education_level", customLevel.trim().take(80))
            }

            // The row normally exists already (created by the database when the account is created): update it.
            val (code, text) = send("PATCH", "$supabaseUrl/rest/v1/profiles?id=eq.${session.userId}", token, body, "return=representation")
            if (code in 200..299 && rowCount(text) > 0) return@withContext true

            // No row yet: create it.
            body.put("id", session.userId)
            val (code2, text2) = send(
                "POST",
                "$supabaseUrl/rest/v1/profiles?on_conflict=id",
                token,
                body,
                "resolution=merge-duplicates,return=representation"
            )
            if (code2 in 200..299 && rowCount(text2) > 0) return@withContext true
            Log.w(TAG, "write failed: PATCH $code, POST $code2")
            false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    private fun send(method: String, url: String, token: String, body: JSONObject, prefer: String): Pair<Int, String> {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .addHeader("Prefer", prefer)
            .method(method, body.toString().toRequestBody(JSON_MEDIA))
            .build()
        return http.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
    }

    private fun rowCount(text: String): Int = try {
        JSONArray(text).length()
    } catch (_: Exception) {
        0
    }
}
