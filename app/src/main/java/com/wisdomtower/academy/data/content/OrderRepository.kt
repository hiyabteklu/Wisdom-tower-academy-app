package com.wisdomtower.academy.data.content

import android.content.Context
import android.net.Uri
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/** Result of sending a payment request. [Failure.message] is plain language, safe to show. */
sealed class OrderResult {
    object Success : OrderResult()
    data class Failure(val message: String) : OrderResult()
}

/**
 * TELEGRAM BUILD ONLY (S2, `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md`): the native payment request. Writes ONE row to
 * `public.orders` (status `pending_verification`, the student's own `user_id`) and, when the student attached a
 * screenshot, uploads it first to the bucket `payment-receipts`, both with the STUDENT'S OWN session. Read from the live
 * database (2026-10-23): policy `ord_create_pending` lets a signed-in student insert a pending row for himself,
 * `receipts_upload` allows the upload, the bucket is public with an 8 MB limit, `ord_own_select` lets him read his own
 * orders. No SQL change is needed. The row has the same columns the website checkout writes; the admin approves it
 * (never automatic).
 *
 * STATUS: written, NOT compiled, NOT seen on a phone.
 */
object OrderRepository {
    private const val TAG = "OrderRepository"
    private const val MAX_BYTES = 8 * 1024 * 1024
    private const val REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    /** Same look as the website `generateOrderRef`: WT-A7K2M9. */
    private fun newOrderRef(): String {
        val random = SecureRandom()
        val sb = StringBuilder("WT-")
        repeat(6) { sb.append(REF_CHARS[random.nextInt(REF_CHARS.length)]) }
        return sb.toString()
    }

    private suspend fun token(force: Boolean = false): String? {
        AuthStateManager.refreshIfNeeded(force = force)
        return AuthStateManager.currentUser.value?.accessToken?.takeIf { it.isNotBlank() }
    }

    /** True when this student already has a pending request for [packageId] (so the screen can show Pending at once). */
    suspend fun hasPending(packageId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = AuthStateManager.currentUser.value?.id ?: return@withContext false
            val token = token() ?: return@withContext false
            val url = "$supabaseUrl/rest/v1/orders".toHttpUrl().newBuilder()
                .addQueryParameter("select", "id")
                .addQueryParameter("user_id", "eq.$userId")
                .addQueryParameter("package_id", "eq.$packageId")
                .addQueryParameter("status", "eq.pending_verification")
                .addQueryParameter("limit", "1")
                .build()
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .get()
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                JSONArray(resp.body?.string().orEmpty()).length() > 0
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "pending check failed: ${e.javaClass.simpleName}")
            false
        }
    }

    /**
     * Sends the request: uploads [receipt] (if any), then inserts the pending order. Needs a transaction id or a
     * screenshot (the screen checks that too). Nothing is saved when the upload fails.
     */
    suspend fun submit(
        context: Context,
        packageId: String,
        amountEtb: Double,
        methodId: String,
        transactionRef: String,
        receipt: Uri?
    ): OrderResult = withContext(Dispatchers.IO) {
        try {
            val user = AuthStateManager.currentUser.value
                ?: return@withContext OrderResult.Failure("Please sign in first.")
            var accessToken = token()
                ?: return@withContext OrderResult.Failure("Your session expired. Please sign in again.")
            val orderId = newOrderRef()

            var receiptUrl: String? = null
            if (receipt != null) {
                val upload = readReceipt(context, receipt)
                    ?: return@withContext OrderResult.Failure("Use a photo (JPG or PNG) up to 8 MB.")
                val path = "$orderId/${System.currentTimeMillis()}.${upload.second}"
                var code = putReceipt(accessToken, path, upload.first, upload.third)
                if (code == 401) {
                    accessToken = token(force = true)
                        ?: return@withContext OrderResult.Failure("Your session expired. Please sign in again.")
                    code = putReceipt(accessToken, path, upload.first, upload.third)
                }
                if (code !in 200..299) {
                    return@withContext OrderResult.Failure("The screenshot could not be sent. Check your connection and try again.")
                }
                receiptUrl = "$supabaseUrl/storage/v1/object/public/payment-receipts/$path"
            }

            val profile = AuthStateManager.currentProfile.value
            val body = JSONObject().apply {
                put("id", orderId)
                put("user_id", user.id)
                put("package_id", packageId)
                put("package_name", PaymentMethods.packageName(packageId))
                put("amount_etb", amountEtb)
                put("status", "pending_verification")
                put("payment_method", methodId)
                put("student_name", profile?.fullName ?: user.fullName ?: JSONObject.NULL)
                put("email", user.email ?: JSONObject.NULL)
                put("transaction_ref", transactionRef.ifBlank { orderId })
                put("receipt_url", receiptUrl ?: JSONObject.NULL)
            }
            var reply = postOrder(accessToken, body)
            if (reply.first == 401) {
                accessToken = token(force = true)
                    ?: return@withContext OrderResult.Failure("Your session expired. Please sign in again.")
                reply = postOrder(accessToken, body)
            }
            if (reply.first in 200..299) {
                OrderResult.Success
            } else {
                Log.w(TAG, "order insert failed: HTTP ${reply.first}")
                OrderResult.Failure("The request could not be sent. Check your connection and try again.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "submit failed: ${e.javaClass.simpleName}")
            OrderResult.Failure("The request could not be sent. Check your connection and try again.")
        }
    }

    /** (bytes, file extension, mime type), or null when it is not a photo or is bigger than 8 MB. */
    private fun readReceipt(context: Context, uri: Uri): Triple<ByteArray, String, String>? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val ext = when (mime) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> return null
        }
        val out = ByteArrayOutputStream()
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_BYTES) return null
            }
        } ?: return null
        if (out.size() == 0) return null
        return Triple(out.toByteArray(), ext, mime)
    }

    /** Uploads the screenshot with the student's token; returns the HTTP code. */
    private fun putReceipt(token: String, path: String, bytes: ByteArray, mime: String): Int {
        val request = Request.Builder()
            .url("$supabaseUrl/storage/v1/object/payment-receipts/$path")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .post(bytes.toRequestBody(mime.toMediaType()))
            .build()
        http.newCall(request).execute().use { resp -> return resp.code }
    }

    private fun postOrder(token: String, body: JSONObject): Pair<Int, String> {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/orders")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Prefer", "return=minimal")
            .addHeader("Accept", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        http.newCall(request).execute().use { resp ->
            return Pair(resp.code, resp.body?.string().orEmpty())
        }
    }
}
