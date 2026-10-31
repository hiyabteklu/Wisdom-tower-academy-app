package com.wisdomtower.academy.data.auth

import android.content.Context
import android.os.Build
import java.security.MessageDigest
import java.io.File
import java.util.UUID

import com.wisdomtower.academy.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Email or phone mapped exactly like the website's `authEmailFromIdentifier`
 * (website-reference/src/lib/authIdentity.ts). Phone users are stored in Supabase
 * as p<digits>@phone.wisdomtower.app.
 */
data class AuthIdentity(val email: String, val phone: String?)

class InvalidIdentifierException(message: String) : Exception(message)

enum class AuthFailureKind {
    WRONG_CREDENTIALS,
    EMAIL_NOT_CONFIRMED,
    CONFIRM_EMAIL_SENT,
    USER_EXISTS,
    WEAK_PASSWORD,
    INVALID_INPUT,
    NETWORK,
    DEVICE_ALREADY_REGISTERED,
    OTHER
}

sealed class SupabaseAuthResult {
    /**
     * [sessionJson] is Supabase's own token response (access_token, token_type, expires_in,
     * expires_at, refresh_token, user{...}). It already has the exact shape the website keeps in
     * localStorage['wt-academy-auth-v1'], so it can be injected into the WebView as-is.
     * It contains tokens: NEVER log it.
     */
    data class Success(
        val sessionJson: String,
        val userId: String,
        val email: String?,
        val fullName: String?
    ) : SupabaseAuthResult()

    /** [message] is plain text that is safe to show to the student. */
    data class Failure(val kind: AuthFailureKind, val message: String) : SupabaseAuthResult()
}

/**
 * Talks directly to the same Supabase project as the website (public URL + anon key from
 * .env.example via BuildConfig). No secrets here. Written to replace the fragile
 * "open the website /login page in a pop-up and copy the session" path.
 *
 * STATUS: written, NOT yet compiled or tested on a device (see STATUS.md).
 */
object SupabaseAuthClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val installationIdLock = Any()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()
    private val siteUrl: String get() = BuildConfig.NEXT_PUBLIC_SITE_URL.trim().trimEnd('/')

    // ---------- identity (mirror of website authIdentity.ts) ----------

    fun looksLikeEmail(value: String): Boolean {
        val v = value.trim()
        return v.contains("@") && EMAIL_REGEX.matches(v)
    }

    fun normalizePhone(raw: String): String {
        var d = raw.filter { it in '0'..'9' }
        if (d.startsWith("0") && d.length == 10) d = "251" + d.substring(1)
        if ((d.startsWith("9") || d.startsWith("7")) && d.length == 9) d = "251" + d
        return d
    }

    /** Throws [InvalidIdentifierException] with the website's own wording. */
    fun identityFrom(identifier: String): AuthIdentity {
        val trimmed = identifier.trim()
        if (looksLikeEmail(trimmed)) return AuthIdentity(trimmed.lowercase(), null)
        val phone = normalizePhone(trimmed)
        if (phone.length < 9) {
            throw InvalidIdentifierException("Enter a valid email or Ethiopian phone (e.g. 09xxxxxxxx).")
        }
        return AuthIdentity("p$phone@phone.wisdomtower.app", phone)
    }

    // ---------- public API ----------

    suspend fun signIn(identifier: String, password: String, context: Context): SupabaseAuthResult = safely {
        val id = try {
            identityFrom(identifier)
        } catch (e: InvalidIdentifierException) {
            return@safely SupabaseAuthResult.Failure(
                AuthFailureKind.INVALID_INPUT,
                e.message ?: "Enter a valid email or phone number."
            )
        }
        if (password.isEmpty()) {
            return@safely SupabaseAuthResult.Failure(AuthFailureKind.INVALID_INPUT, "Please enter your password.")
        }
        val result = tokenCall(
            "/auth/v1/token?grant_type=password",
            JSONObject().put("email", id.email).put("password", password)
        )
        if (result is SupabaseAuthResult.Success) registerDevice(context, result) else result
    }

    /**
     * Mirrors the website's registerScholar: ask the website's register route (best effort),
     * call Supabase sign-up (best effort), then sign in to get real tokens.
     */
    suspend fun signUp(
        fullName: String,
        identifier: String,
        password: String,
        educationLevel: String,
        context: Context
    ): SupabaseAuthResult = safely {
        val name = fullName.trim()
        if (name.isEmpty()) {
            return@safely SupabaseAuthResult.Failure(AuthFailureKind.INVALID_INPUT, "Please enter your full legal name.")
        }
        val id = try {
            identityFrom(identifier)
        } catch (e: InvalidIdentifierException) {
            return@safely SupabaseAuthResult.Failure(
                AuthFailureKind.INVALID_INPUT,
                e.message ?: "Enter a valid email or phone number."
            )
        }
        if (password.length < 6) {
            return@safely SupabaseAuthResult.Failure(AuthFailureKind.WEAK_PASSWORD, "Password must be at least 6 characters.")
        }

        // 1) website server route (same call the website makes); result ignored on purpose
        try {
            val body = JSONObject()
                .put("email", id.email)
                .put("password", password)
                .put("fullName", name)
                .put("educationLevel", educationLevel)
                .put("phone", id.phone ?: JSONObject.NULL)
            val req = Request.Builder()
                .url(siteUrl + "/api/auth/register")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            http.newCall(req).execute().close()
        } catch (e: IOException) {
            // best effort, like the website
        }

        // 2) Supabase sign-up (same metadata the website sends)
        val meta = JSONObject()
            .put("full_name", name)
            .put("education_level", educationLevel)
            .put("phone", id.phone ?: JSONObject.NULL)
        val signUpBody = JSONObject()
            .put("email", id.email)
            .put("password", password)
            .put("data", meta)
        val (signUpCode, signUpText) = post("/auth/v1/signup", signUpBody)
        val signUpFailure: SupabaseAuthResult.Failure? =
            if (signUpCode in 200..299) null else parseFailure(signUpCode, signUpText)

        // 3) sign in to obtain real tokens
        val signedIn = tokenCall(
            "/auth/v1/token?grant_type=password",
            JSONObject().put("email", id.email).put("password", password)
        )
        if (signedIn is SupabaseAuthResult.Failure) {
            if (signUpFailure?.kind == AuthFailureKind.USER_EXISTS && signedIn.kind == AuthFailureKind.WRONG_CREDENTIALS) {
                SupabaseAuthResult.Failure(
                    AuthFailureKind.USER_EXISTS,
                    "An account with this email or phone number already exists. Please sign in."
                )
            } else if (signedIn.kind == AuthFailureKind.EMAIL_NOT_CONFIRMED) {
                SupabaseAuthResult.Failure(
                    AuthFailureKind.CONFIRM_EMAIL_SENT,
                    "Account created. Please confirm your email, then sign in."
                )
            } else {
                signedIn
            }
        } else if (signedIn is SupabaseAuthResult.Success) {
            registerDevice(context, signedIn)
        } else {
            signedIn
        }
    }

    /** Exchange a refresh token for a new session. Supabase rotates refresh tokens: always save the new one. */
    suspend fun refresh(refreshToken: String): SupabaseAuthResult = safely {
        if (refreshToken.isBlank()) {
            return@safely SupabaseAuthResult.Failure(AuthFailureKind.WRONG_CREDENTIALS, "Your session expired. Please sign in again.")
        }
        tokenCall(
            "/auth/v1/token?grant_type=refresh_token",
            JSONObject().put("refresh_token", refreshToken)
        )
    }


    /**
     * A random app-installation GUID is stored only in the app's private, non-backed-up directory.
     * It is not a hardware identifier and is reset if the app is uninstalled or its data is cleared.
     */
    private fun getOrCreateInstallationId(context: Context): String = synchronized(installationIdLock) {
        val file = File(context.noBackupFilesDir, "wt_installation_id")
        val stored = runCatching { file.readText().trim() }.getOrNull()
        if (!stored.isNullOrBlank()) {
            val valid = runCatching { UUID.fromString(stored).toString() == stored }.getOrDefault(false)
            if (valid) return@synchronized stored
        }
        val generated = UUID.randomUUID().toString()
        file.writeText(generated)
        generated
    }

    private fun registerDevice(context: Context, session: SupabaseAuthResult.Success): SupabaseAuthResult {
        val installationId = getOrCreateInstallationId(context)
        val deviceHash = MessageDigest.getInstance("SHA-256")
            .digest("${context.packageName}:$installationId".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80)
        val accessToken = JSONObject(session.sessionJson).optString("access_token")
        val body = JSONObject()
            .put("device_hash", deviceHash)
            .put("device_label", deviceLabel)
            .toString()
            .toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$supabaseUrl/functions/v1/register-device")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            val responseJson = runCatching { JSONObject(responseText) }.getOrNull()
            if (response.isSuccessful && responseJson?.optBoolean("ok") == true) return session

            // The password grant created a temporary Auth session before the device check.
            // Revoke that local session when registration is rejected or cannot be verified,
            // so the rejected installation does not retain a usable refresh session.
            revokeLocalAuthSession(accessToken)

            if (responseJson?.optString("reason") == "device_already_registered") {
                return SupabaseAuthResult.Failure(
                    AuthFailureKind.DEVICE_ALREADY_REGISTERED,
                    "This account is already registered on another phone. Contact Wisdom Tower Academy support to request a device reset."
                )
            }
            return SupabaseAuthResult.Failure(
                AuthFailureKind.NETWORK,
                "We couldn't verify this phone. Please check your connection and try again."
            )
        }
    }

    private fun revokeLocalAuthSession(accessToken: String) {
        runCatching {
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/logout?scope=local")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(ByteArray(0).toRequestBody(null))
                .build()
            http.newCall(request).execute().use { /* consume and close response */ }
        }
    }

    // ---------- internals ----------

    private suspend fun safely(block: suspend () -> SupabaseAuthResult): SupabaseAuthResult =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                SupabaseAuthResult.Failure(AuthFailureKind.NETWORK, "Check your internet connection and try again.")
            } catch (e: Exception) {
                SupabaseAuthResult.Failure(AuthFailureKind.OTHER, "Something went wrong. Please try again.")
            }
        }

    private fun post(path: String, body: JSONObject): Pair<Int, String> {
        val request = Request.Builder()
            .url(supabaseUrl + path)
            .addHeader("apikey", anonKey)
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        return http.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
    }

    private fun tokenCall(path: String, body: JSONObject): SupabaseAuthResult {
        val (code, text) = post(path, body)
        if (code !in 200..299) return parseFailure(code, text)
        val obj = JSONObject(text)
        val user = obj.optJSONObject("user")
        if (!obj.has("access_token") || user == null) {
            return SupabaseAuthResult.Failure(AuthFailureKind.OTHER, "Unexpected response from the server. Please try again.")
        }
        val meta = user.optJSONObject("user_metadata")
        return SupabaseAuthResult.Success(
            sessionJson = text,
            userId = user.str("id"),
            email = user.str("email").ifBlank { null },
            fullName = meta.str("full_name").ifBlank { null }
        )
    }

    private fun JSONObject?.str(key: String): String =
        if (this == null || !has(key) || isNull(key)) "" else optString(key)

    private fun parseFailure(httpCode: Int, body: String): SupabaseAuthResult.Failure {
        val obj = try {
            JSONObject(body)
        } catch (_: Exception) {
            null
        }
        val errorCode = obj.str("error_code").lowercase()
        val serverMsg = listOf(obj.str("msg"), obj.str("error_description"), obj.str("message"))
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val msg = serverMsg.lowercase()

        return when {
            errorCode.startsWith("refresh_token") || msg.contains("refresh token") ->
                SupabaseAuthResult.Failure(AuthFailureKind.WRONG_CREDENTIALS, "Your session expired. Please sign in again.")

            errorCode == "invalid_credentials" || msg.contains("invalid login credentials") || obj.str("error") == "invalid_grant" ->
                SupabaseAuthResult.Failure(AuthFailureKind.WRONG_CREDENTIALS, "Incorrect email/phone or password. Please try again.")

            errorCode == "email_not_confirmed" || msg.contains("email not confirmed") ->
                SupabaseAuthResult.Failure(AuthFailureKind.EMAIL_NOT_CONFIRMED, "Please confirm your email first, then sign in.")

            errorCode == "user_already_exists" || errorCode == "email_exists" || msg.contains("already registered") ->
                SupabaseAuthResult.Failure(AuthFailureKind.USER_EXISTS, "An account with this email or phone number already exists. Please sign in.")

            errorCode == "weak_password" || msg.contains("password should be") ->
                SupabaseAuthResult.Failure(AuthFailureKind.WEAK_PASSWORD, "Password must be at least 6 characters.")

            httpCode == 429 || errorCode.contains("rate_limit") ->
                SupabaseAuthResult.Failure(AuthFailureKind.OTHER, "Too many attempts. Please wait a moment and try again.")

            else ->
                SupabaseAuthResult.Failure(AuthFailureKind.OTHER, "Sign in could not be completed. Please try again.")
        }
    }
}
