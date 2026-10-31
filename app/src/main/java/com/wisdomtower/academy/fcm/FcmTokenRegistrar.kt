package com.wisdomtower.academy.fcm

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Registers this phone's FCM token with the website backend.
 *
 * 2026-10-31: the token used to be sent WITHOUT any sign-in information, so every row in `user_device_tokens` had
 * user_id = null and pushes addressed to one student reached nobody. Now the student's access token goes in the
 * `Authorization: Bearer` header (the server verifies it and links the token to that student). The link is
 * (re)made whenever the signed-in user or their token changes (see [startUserLinking]) and removed on sign-out
 * ("unlink": the phone still gets broadcast pushes). Nothing here blocks the UI; failures are only logged.
 */
object FcmTokenRegistrar {
    private const val TAG = "FcmTokenRegistrar"
    private const val PREFS_NAME = "wta_fcm_prefs"
    private const val KEY_LAST_SENT_TOKEN = "last_sent_token"
    private const val KEY_LAST_LINKED = "last_linked"
    private const val BACKEND_URL = "https://www.wisdom-tower-academy.live/api/notifications/register-device"

    @Volatile
    private var linkingStarted = false

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Resolves incoming notification payload path or URL into a complete destination URL.
     * If empty or null, defaults to /notifications.
     */
    fun resolveTargetUrl(raw: String?): String {
        if (raw.isNullOrBlank()) {
            return "https://www.wisdom-tower-academy.live/notifications"
        }
        val clean = raw.trim()
        return when {
            clean.startsWith("https://") || clean.startsWith("http://") -> clean
            clean.startsWith("/") -> "https://www.wisdom-tower-academy.live$clean"
            else -> "https://www.wisdom-tower-academy.live/$clean"
        }
    }

    /**
     * Checks and retrieves FCM token quietly in the background without blocking the UI.
     * Sends the token to the backend if needed, and starts watching the signed-in student so the token is
     * linked to them.
     */
    @Suppress("DEPRECATION")
    fun checkAndRegisterToken(context: Context) {
        val appContext = context.applicationContext
        startUserLinking(appContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (FirebaseApp.getApps(appContext).isEmpty()) {
                    FirebaseApp.initializeApp(appContext)
                }

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Fetching FCM registration token failed: ${task.exception?.message}")
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        Log.d(TAG, "Fetched FCM registration token successfully")
                        sendTokenToBackend(appContext, token)
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Quiet background FCM token retrieval skipped: ${e.message}")
            }
        }
    }

    /**
     * Sends the token to the backend quietly in the background. When a student is signed in, their access token
     * is sent too, so the server links this phone to them. Does not throw or block the UI on network failures.
     */
    fun sendTokenToBackend(context: Context, token: String, force: Boolean = false) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastToken = prefs.getString(KEY_LAST_SENT_TOKEN, null)

        if (!force && token == lastToken) {
            Log.d(TAG, "FCM token already synchronized with backend")
            return
        }

        val user = AuthStateManager.currentUser.value
        val userId = user?.id
        val access = user?.accessToken

        CoroutineScope(Dispatchers.IO).launch {
            if (postToBackend(token, access, null)) {
                prefs.edit().putString(KEY_LAST_SENT_TOKEN, token).apply()
                if (!userId.isNullOrBlank() && !access.isNullOrBlank()) {
                    prefs.edit().putString(KEY_LAST_LINKED, "$token|$userId").apply()
                }
            }
        }
    }

    /**
     * Watches the signed-in student: every time a user (or a refreshed token) appears, the current FCM token is
     * linked to that user; when the student signs out, the link is removed. Started once per process.
     * A signed-out state seen before any user was ever seen in this run is ignored (the saved session is
     * loaded a moment after the app starts), so a normal start never unlinks by mistake.
     */
    private fun startUserLinking(appContext: Context) {
        if (linkingStarted) return
        linkingStarted = true
        CoroutineScope(Dispatchers.IO).launch {
            var seenUser = false
            try {
                AuthStateManager.currentUser.collect { user ->
                    val id = user?.id
                    val access = user?.accessToken
                    if (!id.isNullOrBlank() && !access.isNullOrBlank()) {
                        seenUser = true
                        linkTokenToUser(appContext, id, access)
                    } else if (user == null && seenUser) {
                        seenUser = false
                        unlinkToken(appContext)
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "User linking stopped: ${e.message}")
            }
        }
    }

    private fun linkTokenToUser(appContext: Context, userId: String, accessToken: String) {
        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val token = task.result
                if (token.isNullOrBlank()) return@addOnCompleteListener
                val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val key = "$token|$userId"
                if (prefs.getString(KEY_LAST_LINKED, null) == key) return@addOnCompleteListener
                CoroutineScope(Dispatchers.IO).launch {
                    if (postToBackend(token, accessToken, null)) {
                        prefs.edit()
                            .putString(KEY_LAST_LINKED, key)
                            .putString(KEY_LAST_SENT_TOKEN, token)
                            .apply()
                        Log.d(TAG, "FCM token linked to the signed-in student")
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Linking FCM token skipped: ${e.message}")
        }
    }

    private fun unlinkToken(appContext: Context) {
        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val token = task.result
                if (token.isNullOrBlank()) return@addOnCompleteListener
                val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                CoroutineScope(Dispatchers.IO).launch {
                    if (postToBackend(token, null, "unlink")) {
                        prefs.edit().remove(KEY_LAST_LINKED).apply()
                        Log.d(TAG, "FCM token unlinked after sign-out")
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Unlinking FCM token skipped: ${e.message}")
        }
    }

    /** One POST to the register-device route. Returns true on an HTTP success. Never throws. */
    private fun postToBackend(token: String, accessToken: String?, action: String?): Boolean {
        return try {
            val json = JSONObject().apply {
                put("token", token)
                put("platform", "android")
                put("deviceName", Build.MODEL)
                if (action != null) put("action", action)
            }
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val builder = Request.Builder()
                .url(BACKEND_URL)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
            if (!accessToken.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer $accessToken")
            }
            httpClient.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token sent to backend (status: ${response.code})")
                    true
                } else {
                    Log.w(TAG, "Backend returned ${response.code} when registering FCM token")
                    false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Quiet background FCM registration note: ${e.message}")
            false
        }
    }
}
