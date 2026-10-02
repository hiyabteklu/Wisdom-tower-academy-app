package com.wisdomtower.academy.fcm

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object FcmTokenRegistrar {
    private const val TAG = "FcmTokenRegistrar"
    private const val PREFS_NAME = "wta_fcm_prefs"
    private const val KEY_LAST_SENT_TOKEN = "last_sent_token"
    private const val BACKEND_URL = "https://www.wisdom-tower-academy.live/api/notifications/register-device"

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
     * Sends the token to the backend if needed.
     */
    @Suppress("DEPRECATION")
    fun checkAndRegisterToken(context: Context) {
        val appContext = context.applicationContext
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
     * Sends the token to the backend POST /api/fcm-token quietly in the background.
     * Does not throw or block the UI on network failures.
     */
    fun sendTokenToBackend(context: Context, token: String, force: Boolean = false) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastToken = prefs.getString(KEY_LAST_SENT_TOKEN, null)

        if (!force && token == lastToken) {
            Log.d(TAG, "FCM token already synchronized with backend")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Escape simple JSON characters
                val safeToken = token.replace("\"", "\\\"")
                val safeModel = Build.MODEL.replace("\"", "\\\"")
                val safeRelease = Build.VERSION.RELEASE.replace("\"", "\\\"")

                val jsonBody = """
                    {
                        "token": "$safeToken",
                        "platform": "android",
                        "deviceName": "$safeModel"
                    }
                """.trimIndent()

                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(BACKEND_URL)
                    .post(requestBody)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token registered to backend (status: ${response.code})")
                        prefs.edit().putString(KEY_LAST_SENT_TOKEN, token).apply()
                    } else {
                        Log.w(TAG, "Backend returned ${response.code} when registering FCM token")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Quiet background FCM registration note: ${e.message}")
            }
        }
    }
}
