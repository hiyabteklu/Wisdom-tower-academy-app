package com.wisdomtower.academy.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.wisdomtower.academy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

        // The app is locked only by a release that was actually published and seen (live or
        // cached). If the check cannot be completed and no release was ever cached, the app
        // opens: a failed check must never lock students out on its own.
        fun cachedOrOpen(): UpdateCheckResult =
            cached(prefs)?.let { UpdateCheckResult.Published(it) } ?: UpdateCheckResult.NoPublishedRelease

        try {
            if (supabaseUrl.isBlank() || anonKey.isBlank()) {
                return@withContext cachedOrOpen()
            }

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/app_versions?select=version_code,version_name,changelog,apk_url,telegram_url,min_version_code&published=eq.true&order=version_code.desc&limit=1")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Accept", "application/json")
                .get()
                .build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext cachedOrOpen()
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)
                if (array.length() == 0) {
                    // An accidental unpublish must not erase a previously cached mandatory release.
                    return@withContext cached(prefs)?.let { UpdateCheckResult.Published(it) }
                        ?: UpdateCheckResult.NoPublishedRelease
                }

                val row = array.getJSONObject(0)
                val release = row.toRelease()
                prefs.edit().putString(CACHED_RELEASE, row.toString()).apply()
                UpdateCheckResult.Published(release)
            }
        } catch (_: Exception) {
            cachedOrOpen()
        }
    }

    /**
     * Downloads the release APK into app-private cache. A partial download is never
     * passed to Android's package installer.
     */
    suspend fun downloadApk(
        context: Context,
        release: AppRelease,
        onProgress: (Float?) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val uri = Uri.parse(release.apkUrl)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) {
            "The release APK link must use HTTPS."
        }

        val request = Request.Builder().url(release.apkUrl).get().build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "APK download failed (HTTP ${response.code})." }
            val body = response.body ?: error("The APK download was empty.")
            val total = body.contentLength()
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            directory.listFiles()?.filter { it.isFile }?.forEach { it.delete() }

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
                            val value = if (total > 0L) {
                                (copied.toFloat() / total).coerceIn(0f, 1f)
                            } else null
                            withContext(Dispatchers.Main) { onProgress(value) }
                        }
                    }
                }

                require(partial.length() > 4L) { "The downloaded APK is empty or incomplete." }
                partial.inputStream().use { input ->
                    require(input.read() == 0x50 && input.read() == 0x4b) {
                        "The downloaded file is not a valid APK archive."
                    }
                }
                verifyDownloadedApk(context, partial, release)
                if (target.exists()) target.delete()
                check(partial.renameTo(target)) { "Could not prepare the downloaded update." }
                withContext(Dispatchers.Main) { onProgress(1f) }
                target
            } catch (error: Exception) {
                partial.delete()
                throw error
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyDownloadedApk(context: Context, file: File, release: AppRelease) {
        val flags = if (Build.VERSION.SDK_INT >= 28) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: error("Android could not read the downloaded APK.")
        require(archive.packageName == context.packageName) {
            "This APK is for a different app and cannot update Wisdom Tower Academy."
        }
        val archiveVersion = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else {
            archive.versionCode.toLong()
        }
        require(archiveVersion == release.versionCode.toLong()) {
            "The APK version does not match the published release. Please contact the app administrator."
        }
        require(archiveVersion > BuildConfig.VERSION_CODE) {
            "The downloaded APK is not newer than the installed app."
        }

        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        if (Build.VERSION.SDK_INT >= 28) {
            val installedInfo = installed.signingInfo
                ?: error("The installed app signing information is unavailable.")
            val candidateInfo = archive.signingInfo
                ?: error("The downloaded APK signing information is unavailable.")
            val installedSigners = installedInfo.apkContentsSigners.map { it.toByteArray().toList() }.toSet()
            val candidateSigners = candidateInfo.apkContentsSigners.map { it.toByteArray().toList() }.toSet()
            val sameSignerSet = installedSigners.isNotEmpty() && installedSigners == candidateSigners
            val validRotation = installedSigners.size == 1 &&
                candidateSigners.size == 1 &&
                candidateInfo.hasPastSigningCertificates() &&
                candidateInfo.signingCertificateHistory.any {
                    it.toByteArray().toList() in installedSigners
                }
            require(sameSignerSet || validRotation) {
                "The APK signature does not match the installed Wisdom Tower Academy app."
            }
        } else {
            val installedSigners = installed.signatures?.map { it.toByteArray().toList() }?.toSet().orEmpty()
            val candidateSigners = archive.signatures?.map { it.toByteArray().toList() }?.toSet().orEmpty()
            require(installedSigners.isNotEmpty() && installedSigners == candidateSigners) {
                "The APK signature does not match the installed Wisdom Tower Academy app."
            }
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
            require(telegramUri.scheme == "https" && (host == "t.me" || host == "telegram.me")) {
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
