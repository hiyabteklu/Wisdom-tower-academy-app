package com.wisdomtower.academy.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages user auth sessions and credentials using hardware-backed EncryptedSharedPreferences.
 * Never logs tokens or sensitive PII.
 */
class SecureSessionManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SecureSession", "Failed to init EncryptedSharedPreferences, falling back to private prefs", e)
            context.getSharedPreferences("${PREFS_FILENAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveSession(
        token: String,
        refreshToken: String?,
        userId: String,
        email: String,
        displayName: String = ""
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, displayName.ifBlank { email.substringBefore("@") })
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "anonymous_guest") ?: "anonymous_guest"

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserDisplayName(): String = prefs.getString(KEY_USER_NAME, "Scholar") ?: "Scholar"

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getAccessToken().isNullOrBlank()

    fun isGuestMode(): Boolean = prefs.getBoolean(KEY_GUEST_MODE, false)

    fun setGuestMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GUEST_MODE, enabled).apply()
    }

    /**
     * Wipes session tokens on logout, and optionally purges offline downloaded resources.
     */
    fun clearSession(wipeOfflineCache: Boolean = false) {
        prefs.edit().clear().apply()
        if (wipeOfflineCache) {
            SecureFileStorage(context).wipeAllSecureFiles()
        }
    }

    companion object {
        private const val PREFS_FILENAME = "wt_encrypted_session_store"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_GUEST_MODE = "is_guest_mode"
        private const val KEY_LAST_LOGIN = "last_login"
    }
}
