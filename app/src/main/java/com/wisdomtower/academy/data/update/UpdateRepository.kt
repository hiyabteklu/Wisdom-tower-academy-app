package com.wisdomtower.academy.data.update

import android.content.Context
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
    val minVersionCode: Int
) {
    val isUpdateAvailable: Boolean get() = versionCode > BuildConfig.VERSION_CODE
    val isForced: Boolean get() = BuildConfig.VERSION_CODE < minVersionCode
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

    suspend fun latest(context: Context): AppRelease? = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        try {
            if (supabaseUrl.isBlank() || anonKey.isBlank()) return@withContext cached(prefs)
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/app_versions?select=version_code,version_name,changelog,apk_url,min_version_code&published=eq.true&order=version_code.desc&limit=1")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Accept", "application/json")
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext cached(prefs)
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)
                if (array.length() == 0) return@withContext cached(prefs)
                val row = array.getJSONObject(0)
                val release = row.toRelease()
                prefs.edit().putString(CACHED_RELEASE, row.toString()).apply()
                release
            }
        } catch (_: Exception) {
            cached(prefs)
        }
    }

    private fun cached(prefs: android.content.SharedPreferences): AppRelease? =
        try { prefs.getString(CACHED_RELEASE, null)?.let { JSONObject(it).toRelease() } }
        catch (_: Exception) { null }

    private fun JSONObject.toRelease(): AppRelease {
        val lines = getString("changelog")
            .lines()
            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotBlank() }
        return AppRelease(
            versionCode = getInt("version_code"),
            versionName = getString("version_name"),
            changelog = lines,
            apkUrl = getString("apk_url"),
            minVersionCode = getInt("min_version_code")
        )
    }
}
