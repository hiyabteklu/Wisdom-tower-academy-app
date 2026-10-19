package com.wisdomtower.academy.data.admin

import android.util.Log
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * ADMIN-native N8a (docs/ADMIN_NATIVE_PLAN.md). Calls to the WEBSITE API routes that need the admin's own login
 * (Authorization: Bearer access token), exactly like the website panel does with supabase.auth.getSession().
 * Used for the broadcast history list and delete (api/notifications with admin=true). Same token handling as
 * AdminSupabase.kt: refresh first, one retry on 401. No secret key, ever. Replies use AdminHttpResult
 * (code -1 = could not reach the server, -2 = not signed in).
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 * Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
 */
object AdminWebApi {
    private const val TAG = "AdminWebApi"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun execute(method: String, absoluteUrl: String, token: String): AdminHttpResult {
        val builder = Request.Builder()
            .url(absoluteUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
        if (method == "DELETE") builder.delete() else builder.get()
        http.newCall(builder.build()).execute().use { resp ->
            return AdminHttpResult(resp.code, resp.body?.string().orEmpty())
        }
    }

    private suspend fun request(method: String, absoluteUrl: String): AdminHttpResult =
        withContext(Dispatchers.IO) {
            try {
                AuthStateManager.refreshIfNeeded()
                val token = AuthStateManager.currentUser.value?.accessToken
                if (token.isNullOrBlank()) return@withContext AdminHttpResult(-2, "")
                var reply = execute(method, absoluteUrl, token)
                if (reply.code == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
                    val token2 = AuthStateManager.currentUser.value?.accessToken
                    if (!token2.isNullOrBlank()) reply = execute(method, absoluteUrl, token2)
                }
                reply
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "$method failed: ${e.javaClass.simpleName}")
                AdminHttpResult(-1, "")
            }
        }

    /** absoluteUrl is the full website url, for example the origin plus /api/notifications?admin=true */
    suspend fun get(absoluteUrl: String): AdminHttpResult = request("GET", absoluteUrl)

    suspend fun delete(absoluteUrl: String): AdminHttpResult = request("DELETE", absoluteUrl)
}
