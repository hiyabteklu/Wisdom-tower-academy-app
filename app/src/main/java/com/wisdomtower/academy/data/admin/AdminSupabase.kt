package com.wisdomtower.academy.data.admin

import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Reply of one admin REST call. code -1 = could not reach the server, -2 = not signed in. */
class AdminHttpResult(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299

    /** A short, honest message for a toast or an error card. */
    fun errorText(): String {
        if (code == -2) return "Not signed in"
        if (code == -1) return "Could not reach the server"
        val fromServer = try {
            JSONObject(body).optString("message", "")
        } catch (e: Exception) {
            ""
        }
        return if (fromServer.isNotBlank()) fromServer else "Request failed (HTTP $code)"
    }
}

/**
 * ADMIN-native N2. ONE shared helper for every native admin module: PostgREST calls with the signed-in admin's own
 * token (refresh first, one retry on 401, same handling as AdminAccess.kt). Row-level security decides what the
 * account may read or change. No secret key, ever. PATCH asks for the changed rows back so a silent RLS refusal
 * (HTTP 200 with zero rows) can be detected by the caller.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object AdminSupabase {
    private const val TAG = "AdminSupabase"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private fun execute(method: String, path: String, jsonBody: String?, token: String): AdminHttpResult {
        val builder = Request.Builder()
            .url("$supabaseUrl/rest/v1/$path")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
        when (method) {
            "PATCH" -> builder
                .addHeader("Prefer", "return=representation")
                .patch((jsonBody ?: "{}").toRequestBody(JSON_MEDIA))
            else -> builder.get()
        }
        http.newCall(builder.build()).execute().use { resp ->
            return AdminHttpResult(resp.code, resp.body?.string().orEmpty())
        }
    }

    private suspend fun request(method: String, path: String, jsonBody: String?): AdminHttpResult =
        withContext(Dispatchers.IO) {
            try {
                AuthStateManager.refreshIfNeeded()
                val token = AuthStateManager.currentUser.value?.accessToken
                if (token.isNullOrBlank()) return@withContext AdminHttpResult(-2, "")
                var reply = execute(method, path, jsonBody, token)
                if (reply.code == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
                    val token2 = AuthStateManager.currentUser.value?.accessToken
                    if (!token2.isNullOrBlank()) reply = execute(method, path, jsonBody, token2)
                }
                reply
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "$method failed: ${e.javaClass.simpleName}")
                AdminHttpResult(-1, "")
            }
        }

    /** path is everything after /rest/v1/, e.g. inquiries?select=id&limit=5 */
    suspend fun get(path: String): AdminHttpResult = request("GET", path, null)

    /** path includes the row filter, e.g. inquiries?id=eq.abc ; the body is a JSON object of the columns to change. */
    suspend fun patch(path: String, jsonBody: String): AdminHttpResult = request("PATCH", path, jsonBody)
}
