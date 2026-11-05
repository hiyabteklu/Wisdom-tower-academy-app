package com.wisdomtower.academy.data.update

import android.content.Context
import android.net.Uri
import com.wisdomtower.academy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val changelog: List<String>,
    val apkUrl: String,
    val telegramUrl: String?,
    val minVersionCode: Int
) {
    val isUpdateAvailable: Boolean get() = versionCode > BuildConfig.VERSION_CODE

    // Every published newer release is mandatory. min_version_code also protects older
    // builds if the release metadata is ever staged with a later minimum.
    val isForced: Boolean get() =
        isUpdateAvailable || BuildConfig.VERSION_CODE < minVersionCode
}

sealed class UpdateCheckResult {
    data class Published(val release: AppRelease) : UpdateCheckResult()
    object NoPublishedRelease : UpdateCheckResult()
    object Unavailable : UpdateCheckResult()
}

/** Reads published release metadata and keeps the last successful row for offline checks. */
object UpdateRepository {
    private const val PREFS = "wta_app_updates"
    private const val CACHED_RELEASE = "cached_release"
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val supabaseUrl get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    suspend fun checkLatest(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun cachedOrUnavailable(): UpdateCheckResult =
            cached(prefs)?.let { UpdateCheckResult.Published(it) } ?: UpdateCheckResult.Unavailable

        try {
            if (supabaseUrl.isBlank() || anonKey.isBlank()) {
                return@withContext cachedOrUnavailable()
            }

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/app_versions?select=version_code,version_name,changelog,apk_url,telegram_url,min_version_code&published=eq.true&order=version_code.desc&limit=1")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Accept", "application/json")
                .get()
                .build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext cachedOrUnavailable()
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)
                if (array.length() == 0) return@withContext UpdateCheckResult.NoPublishedRelease

                val row = array.getJSONObject(0)
                val release = row.toRelease()
                prefs.edit().putString(CACHED_RELEASE, row.toString()).apply()
                UpdateCheckResult.Published(release)
            }
        } catch (_: Exception) {
            cachedOrUnavailable()
        }
    }

    private fun cached(prefs: android.content.SharedPreferences): AppRelease? =
        try { prefs.getString(CACHED_RELEASE, null)?.let { JSONObject(it).toRelease() } }
        catch (_: Exception) { null }

    private fun JSONObject.toRelease(): AppRelease {
        val releaseVersionCode = getInt("version_code")
        val minimumVersionCode = getInt("min_version_code")
        require(releaseVersionCode > 0) { "The published release has an invalid version code." }
        require(minimumVersionCode >= 0 && minimumVersionCode <= releaseVersionCode) {
            "The published minimum version is invalid. Contact the app administrator."
        }

        val apk = getString("apk_url").trim()
        val apkUri = Uri.parse(apk)
        require(apkUri.scheme == "https" && !apkUri.host.isNullOrBlank()) {
            "The published APK link is invalid. Contact the app administrator."
        }

        val telegram = optString("telegram_url", "").trim().takeIf { it.isNotEmpty() }
        if (telegram != null) {
            val telegramUri = Uri.parse(telegram)
            val host = telegramUri.host?.lowercase()
            require(telegramUri.scheme == "https" && host in setOf("t.me", "telegram.me")) {
                "The published Telegram link is invalid. Contact the app administrator."
            }
        }

        val lines = getString("changelog")
            .lines()
            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotBlank() }

        return AppRelease(
            versionCode = releaseVersionCode,
            versionName = getString("version_name"),
            changelog = lines,
            apkUrl = apk,
            telegramUrl = telegram,
            minVersionCode = minimumVersionCode
        )
    }
}
