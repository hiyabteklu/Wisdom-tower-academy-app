package com.wisdomtower.academy.data.admin

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/** Result of one photo upload: path is the storage path on success, error is a short message on failure. */
class AdminUploadResult(val path: String?, val error: String?)

/**
 * ADMIN-native N10. Photo upload to the PUBLIC Supabase storage bucket `free-resources`, with the signed-in admin's
 * own token (the same call the website makes in uploadFreeResourceFile: storage.from("free-resources").upload with
 * upsert true). No secret key, no website API route. The storage policies of website-reference/docs/free-resources-setup.sql
 * let any signed-in account write to this bucket and everybody read it.
 * The file is chosen with the phone's own file chooser (the screen launches it), read from the content address and
 * sent as one raw request body with its real content type. Limit 8 MB, same as the website.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object AdminStorage {
    private const val TAG = "AdminStorage"
    private const val BUCKET = "free-resources"
    const val MAX_BYTES = 8 * 1024 * 1024

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private fun encodePath(path: String): String =
        path.trimStart('/').split('/').joinToString("/") { Uri.encode(it) }

    /**
     * Same rule as freeResourcePublicUrl on the website: a full http(s) link is used as it is, anything else is a path
     * inside the public bucket. Returns an empty text when there is no path.
     */
    fun publicUrl(storagePath: String): String {
        val path = storagePath.trim()
        if (path.isEmpty()) return ""
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return "$supabaseUrl/storage/v1/object/public/$BUCKET/${encodePath(path)}"
    }

    private fun displayName(context: Context, uri: Uri): String {
        try {
            val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null) {
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            val name = c.getString(index)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "name lookup failed: ${e.javaClass.simpleName}")
        }
        return "photo"
    }

    /** Reads at most MAX_BYTES. Null when the file is larger. */
    private fun readLimited(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > MAX_BYTES) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun send(path: String, bytes: ByteArray, mime: String, token: String): AdminHttpResult {
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$supabaseUrl/storage/v1/object/$BUCKET/${encodePath(path)}")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("x-upsert", "true")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()
        val reply = http.newCall(request).execute().use { resp ->
            AdminHttpResult(resp.code, resp.body?.string().orEmpty())
        }
        return reply
    }

    /**
     * Uploads the chosen file as folder/<time>-<safe name> (same path rule as the website panel).
     * Never throws: a failure comes back in error.
     */
    suspend fun uploadFreeResourcePhoto(context: Context, uri: Uri, folder: String): AdminUploadResult =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val stream = resolver.openInputStream(uri)
                    ?: return@withContext AdminUploadResult(null, "Could not read the chosen file")
                val bytes = stream.use { readLimited(it) }
                    ?: return@withContext AdminUploadResult(null, "Max file size 8 MB")
                val safeName = displayName(context, uri).replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val path = "$folder/${System.currentTimeMillis()}-$safeName"

                AuthStateManager.refreshIfNeeded()
                val token = AuthStateManager.currentUser.value?.accessToken
                if (token.isNullOrBlank()) return@withContext AdminUploadResult(null, "Not signed in")
                var reply = send(path, bytes, mime, token)
                if (reply.code == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
                    val token2 = AuthStateManager.currentUser.value?.accessToken
                    if (!token2.isNullOrBlank()) reply = send(path, bytes, mime, token2)
                }
                if (reply.ok) AdminUploadResult(path, null) else AdminUploadResult(null, reply.errorText())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "upload failed: ${e.javaClass.simpleName}")
                AdminUploadResult(null, "Could not reach the server")
            }
        }
}
