package com.wisdomtower.academy.data.admin

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
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

/** Outcome of one phone upload. On success [fileId] is the Appwrite File ID inside the private bucket. */
class PaidUploadResult(val ok: Boolean, val fileId: String, val message: String)

/**
 * SECURITY S5d (2026-10-20): uploads a PDF chosen on the phone into the PRIVATE Appwrite bucket `paid-pdfs`, through
 * the Supabase Edge Function `paid-pdf-upload` (docs/S5d-phone-upload.md). The app holds NO Appwrite key: the function
 * checks that the caller is an admin (public.am_i_admin with the caller's own token) and forwards each chunk.
 *
 * The file is sent in chunks of at most [CHUNK_BYTES] (the function refuses more). The first chunk has no upload id;
 * the function answers with `file_id`, which every following chunk sends back as `x-upload-id`.
 * Limits (same as the function): PDF only, at most [MAX_BYTES].
 *
 * Nothing here touches the database. The caller puts the returned File ID into the editor field; the editor's save()
 * already stores it as `appwrite-paid:<id>` for the hub `life-savers` (S5c).
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object PaidPdfUploader {
    private const val TAG = "PaidPdfUploader"
    const val CHUNK_BYTES = 5 * 1024 * 1024
    const val MAX_BYTES = 50L * 1024L * 1024L

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val OCTET = "application/octet-stream".toMediaType()

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private fun fail(message: String) = PaidUploadResult(false, "", message)

    /** Display name and size of the chosen file, or null when the phone does not tell. */
    private fun describe(resolver: ContentResolver, uri: Uri): Pair<String, Long>? {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIdx >= 0) c.getString(nameIdx).orEmpty() else ""
                val size = if (sizeIdx >= 0 && !c.isNull(sizeIdx)) c.getLong(sizeIdx) else -1L
                if (size > 0) return Pair(name.ifBlank { "file.pdf" }, size)
            }
        }
        return null
    }

    /** HTTP header values must be plain ASCII; the server cleans the name again. */
    private fun headerSafe(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifEmpty { "file.pdf" }

    private fun sendChunk(
        token: String,
        name: String,
        total: Long,
        start: Long,
        uploadId: String,
        bytes: ByteArray,
        length: Int
    ): Pair<Int, String> {
        val builder = Request.Builder()
            .url("$supabaseUrl/functions/v1/paid-pdf-upload")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("x-file-name", headerSafe(name))
            .addHeader("x-total-size", total.toString())
            .addHeader("x-chunk-start", start.toString())
            .post(bytes.toRequestBody(OCTET, 0, length))
        if (uploadId.isNotEmpty()) builder.addHeader("x-upload-id", uploadId)
        http.newCall(builder.build()).execute().use { resp ->
            return Pair(resp.code, resp.body?.string().orEmpty())
        }
    }

    private fun messageFor(code: Int, body: String): String {
        if (code == 403) return "Only admins can upload"
        if (code == 401) return "Not signed in"
        val fromServer = try {
            JSONObject(body).optString("message", "")
        } catch (e: Exception) {
            ""
        }
        return if (fromServer.isNotBlank()) fromServer else "Upload failed (HTTP $code)"
    }

    /**
     * Upload the file behind [uri]. [onProgress] gets 0..100 (called from a background thread, so only write it into
     * Compose state or post it to the main thread). Returns the File ID on success.
     */
    suspend fun upload(resolver: ContentResolver, uri: Uri, onProgress: (Int) -> Unit): PaidUploadResult =
        withContext(Dispatchers.IO) {
            try {
                val info = describe(resolver, uri) ?: return@withContext fail("Could not read the file size")
                val name = info.first
                val total = info.second
                if (total > MAX_BYTES) return@withContext fail("The PDF is larger than 50 MB")

                AuthStateManager.refreshIfNeeded()
                var token = AuthStateManager.currentUser.value?.accessToken
                if (token.isNullOrBlank()) return@withContext fail("Not signed in")

                val input = resolver.openInputStream(uri) ?: return@withContext fail("Could not open the file")
                input.use { stream ->
                    val buffer = ByteArray(CHUNK_BYTES)
                    var sent = 0L
                    var uploadId = ""
                    onProgress(0)
                    while (sent < total) {
                        val want = minOf(CHUNK_BYTES.toLong(), total - sent).toInt()
                        var read = 0
                        while (read < want) {
                            val n = stream.read(buffer, read, want - read)
                            if (n < 0) break
                            read += n
                        }
                        if (read != want) return@withContext fail("The file ended early. Pick it again.")

                        var reply = sendChunk(token!!, name, total, sent, uploadId, buffer, read)
                        if (reply.first == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
                            val fresh = AuthStateManager.currentUser.value?.accessToken
                            if (!fresh.isNullOrBlank()) {
                                token = fresh
                                reply = sendChunk(token, name, total, sent, uploadId, buffer, read)
                            }
                        }
                        if (reply.first !in 200..299) return@withContext fail(messageFor(reply.first, reply.second))

                        val json = try {
                            JSONObject(reply.second)
                        } catch (e: Exception) {
                            return@withContext fail("Unexpected reply from the server")
                        }
                        if (json.optString("status") != "ok") {
                            return@withContext fail(messageFor(reply.first, reply.second))
                        }
                        val id = json.optString("file_id", "")
                        if (id.isBlank()) return@withContext fail("The server did not return a File ID")
                        uploadId = id
                        sent += read
                        onProgress((sent * 100L / total).toInt())
                    }
                    return@withContext PaidUploadResult(true, uploadId, "Uploaded")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "upload failed: ${e.javaClass.simpleName}")
                fail("Could not reach the server")
            }
        }
}
