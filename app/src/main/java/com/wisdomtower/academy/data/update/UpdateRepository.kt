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
import java.io.File

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

    /** Downloads a published APK to app-private cache; partial files are never offered to Android. */
    suspend fun downloadApk(context: Context, release: AppRelease, onProgress: (Float?) -> Unit): File =
        withContext(Dispatchers.IO) {
            val uri = android.net.Uri.parse(release.apkUrl)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank()) {
                "The release APK link must use HTTPS."
            }
            val request = Request.Builder().url(release.apkUrl).get().build()
            http.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "APK download failed (HTTP ${response.code})." }
                val body = response.body ?: error("The APK download was empty.")
                val total = body.contentLength()
                val directory = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(directory, "wisdom-tower-update-${release.versionCode}.apk")
                val partial = File(directory, "update-${release.versionCode}.part")
                try {
                    body.byteStream().use { input ->
                        partial.outputStream().buffered().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var copied = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                copied += count
                                onProgress(if (total > 0L) (copied.toFloat() / total).coerceIn(0f, 1f) else null)
                            }
                        }
                    }
                    require(partial.length() > 4L) { "The downloaded APK is empty or incomplete." }
                    partial.inputStream().use { input ->
                        require(input.read() == 0x50 && input.read() == 0x4b) {
                            "The downloaded file is not a valid APK archive."
                        }
                    }
                    if (target.exists()) target.delete()
                    check(partial.renameTo(target)) { "Could not prepare the downloaded update." }
                    onProgress(1f)
                    target
                } catch (error: Exception) {
                    partial.delete()
                    throw error
                }
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
