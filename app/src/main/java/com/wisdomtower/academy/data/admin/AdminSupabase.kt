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

/**
 * Reply of one admin REST call. code -1 = could not reach the server, -2 = not signed in.
 * contentRange is the Content-Range header (only filled by count(), it looks like 0-4/5 or star/0).
 */
class AdminHttpResult(val code: Int, val body: String, val contentRange: String? = null) {
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
 * ADMIN-native N2/N3. ONE shared helper for every native admin module: PostgREST calls with the signed-in admin's own
 * token (refresh first, one retry on 401, same handling as AdminAccess.kt). Row-level security decides what the
 * account may read or change. No secret key, ever. PATCH asks for the changed rows back so a silent RLS refusal
 * (HTTP 200 with zero rows) can be detected by the caller.
 * N3 added: post() for inserts and upserts, and postPublic() for the website API route that sends the payment
 * e-mail and SMS (that route is called exactly as the website panel calls it: JSON body, no key from the app).
 * N5 added: count() for exact row counts (HEAD request with Prefer count=exact, the request supabase-js sends for
 * head: true, count: exact). A count that could not be read is null, never a made-up number.
 * N6 added: delete(). It asks for the deleted rows back so a silent RLS refusal (HTTP 200 with zero rows) can be
 * detected by the caller, exactly like patch().
 * N10b added: headCount(). Same HEAD request as count(), but it returns the whole reply so the read-only Database
 * status screen can tell a missing table (404) from a refusal (401 or 403) from an unreachable server.
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

    private fun execute(
        method: String,
        path: String,
        jsonBody: String?,
        token: String,
        prefer: String?
    ): AdminHttpResult {
        val builder = Request.Builder()
            .url("$supabaseUrl/rest/v1/$path")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
        when (method) {
            "PATCH" -> {
                builder.addHeader("Prefer", prefer ?: "return=representation")
                builder.patch((jsonBody ?: "{}").toRequestBody(JSON_MEDIA))
            }
            "POST" -> {
                if (prefer != null) builder.addHeader("Prefer", prefer)
                builder.post((jsonBody ?: "{}").toRequestBody(JSON_MEDIA))
            }
            "DELETE" -> {
                builder.addHeader("Prefer", prefer ?: "return=representation")
                builder.delete()
            }
            "HEAD" -> {
                builder.addHeader("Prefer", "count=exact")
                builder.head()
            }
            else -> builder.get()
        }
        http.newCall(builder.build()).execute().use { resp ->
            return AdminHttpResult(resp.code, resp.body?.string().orEmpty(), resp.header("Content-Range"))
        }
    }

    private suspend fun request(
        method: String,
        path: String,
        jsonBody: String?,
        prefer: String?
    ): AdminHttpResult =
        withContext(Dispatchers.IO) {
            try {
                AuthStateManager.refreshIfNeeded()
                val token = AuthStateManager.currentUser.value?.accessToken
                if (token.isNullOrBlank()) return@withContext AdminHttpResult(-2, "")
                var reply = execute(method, path, jsonBody, token, prefer)
                if (reply.code == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
                    val token2 = AuthStateManager.currentUser.value?.accessToken
                    if (!token2.isNullOrBlank()) reply = execute(method, path, jsonBody, token2, prefer)
                }
                reply
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "$method failed: ${e.javaClass.simpleName}")
                AdminHttpResult(-1, "")
            }
        }

    /** Call an admin-only Postgres RPC with the current user's JWT. The database remains the authorization boundary. */
    suspend fun rpc(functionName: String, jsonBody: String = "{}"): AdminHttpResult {
        require(functionName.matches(Regex("^[a-z][a-z0-9_]*$"))) { "Invalid RPC name" }
        return request("POST", "rpc/$functionName", jsonBody, null)
    }

    /** path is everything after /rest/v1/, e.g. inquiries?select=id&limit=5 */
    suspend fun get(path: String): AdminHttpResult = request("GET", path, null, null)

    /** Exact number of rows that match path (include select=id and the filters). Null when it could not be read. */
    suspend fun count(path: String): Int? {
        val reply = request("HEAD", path, null, null)
        if (!reply.ok) return null
        return reply.contentRange?.substringAfter('/', "")?.trim()?.toIntOrNull()
    }

    /**
     * Same request as count(), but the whole reply comes back (code and Content-Range). A HEAD request reads no rows
     * and changes nothing. Use select=* when the table may have no id column.
     */
    suspend fun headCount(path: String): AdminHttpResult = request("HEAD", path, null, null)

    /** path includes the row filter, e.g. inquiries?id=eq.abc ; the body is a JSON object of the columns to change. */
    suspend fun patch(path: String, jsonBody: String): AdminHttpResult = request("PATCH", path, jsonBody, null)

    /**
     * path MUST include a row filter, e.g. access_grants?id=eq.abc or access_grants?id=in.(a,b). The reply body is the
     * array of the rows that were deleted: an empty array with HTTP 200 means nothing was removed.
     */
    suspend fun delete(path: String): AdminHttpResult = request("DELETE", path, null, null)

    /** Insert or upsert. prefer example: resolution=ignore-duplicates,return=minimal */
    suspend fun post(path: String, jsonBody: String, prefer: String): AdminHttpResult =
        request("POST", path, jsonBody, prefer)

    suspend fun uploadStorageObject(
        bucket: String, objectPath: String, bytes: ByteArray, contentType: String
    ): AdminHttpResult = withContext(Dispatchers.IO) {
        require(bucket.matches(Regex("^[a-z0-9-]+$")))
        require(objectPath.matches(Regex("^[A-Za-z0-9._-]+$")))
        try {
            AuthStateManager.refreshIfNeeded()
            var token = AuthStateManager.currentUser.value?.accessToken
            if (token.isNullOrBlank()) return@withContext AdminHttpResult(-2, "")
            fun send(accessToken: String): AdminHttpResult {
                val request = Request.Builder()
                    .url("$supabaseUrl/storage/v1/object/$bucket/$objectPath")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("x-upsert", "true")
                    .post(bytes.toRequestBody(contentType.toMediaType()))
                    .build()
                return http.newCall(request).execute().use { response ->
                    AdminHttpResult(response.code, response.body?.string().orEmpty())
                }
            }
            var reply = send(token!!)
            if (reply.code == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
                token = AuthStateManager.currentUser.value?.accessToken
                if (!token.isNullOrBlank()) reply = send(token!!)
            }
            reply
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Storage upload failed: ${e.javaClass.simpleName}")
            AdminHttpResult(-1, "")
        }
    }

    fun publicStorageUrl(bucket: String, objectPath: String): String =
        "$supabaseUrl/storage/v1/object/public/$bucket/$objectPath"

    /** POST to a public website API route (no login header, same as the website panel). absoluteUrl is the full url. */
    suspend fun postPublic(absoluteUrl: String, jsonBody: String): AdminHttpResult =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(absoluteUrl)
                    .addHeader("Accept", "application/json")
                    .post(jsonBody.toRequestBody(JSON_MEDIA))
                    .build()
                http.newCall(req).execute().use { resp ->
                    AdminHttpResult(resp.code, resp.body?.string().orEmpty())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "public POST failed: ${e.javaClass.simpleName}")
                AdminHttpResult(-1, "")
            }
        }
}
