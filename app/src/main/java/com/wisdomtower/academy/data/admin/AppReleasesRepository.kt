package com.wisdomtower.academy.data.admin

import org.json.JSONArray
import org.json.JSONObject

data class AdminAppRelease(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val apkUrl: String,
    val telegramUrl: String?,
    val minVersionCode: Int,
    val published: Boolean
)

sealed class AppReleasesResult {
    data class Success(val releases: List<AdminAppRelease>) : AppReleasesResult()
    data class Failure(val message: String) : AppReleasesResult()
}

/** Release editor uses the signed-in admin's JWT. RLS on public.app_versions remains the authorization boundary. */
object AppReleasesRepository {
    suspend fun list(): AppReleasesResult {
        val reply = AdminSupabase.get(
            "app_versions?select=version_code,version_name,changelog,apk_url,telegram_url,min_version_code,published&order=version_code.desc&limit=30"
        )
        if (!reply.ok) return AppReleasesResult.Failure(reply.errorText())
        return try {
            val rows = JSONArray(reply.body)
            val releases = (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                AdminAppRelease(
                    versionCode = row.getInt("version_code"),
                    versionName = row.optString("version_name"),
                    changelog = row.optString("changelog"),
                    apkUrl = row.optString("apk_url"),
                    telegramUrl = row.optString("telegram_url").takeIf { it.isNotBlank() },
                    minVersionCode = row.getInt("min_version_code"),
                    published = row.optBoolean("published")
                )
            }
            AppReleasesResult.Success(releases)
        } catch (_: Exception) {
            AppReleasesResult.Failure("The release list returned an unexpected response.")
        }
    }

    suspend fun save(release: AdminAppRelease): AppReleasesResult {
        if (release.versionCode <= 0) return AppReleasesResult.Failure("Version code must be greater than zero.")
        if (release.versionName.isBlank()) return AppReleasesResult.Failure("Enter a version name.")
        if (release.minVersionCode < 0 || release.minVersionCode > release.versionCode) {
            return AppReleasesResult.Failure("Minimum supported version must be between zero and this release's version code.")
        }
        if (!isHttpsUrl(release.apkUrl)) return AppReleasesResult.Failure("APK link must be a valid HTTPS URL.")
        val telegram = release.telegramUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (telegram != null) {
            val uri = android.net.Uri.parse(telegram)
            val host = uri.host?.lowercase()
            if (uri.scheme != "https" || (host != "t.me" && host != "telegram.me")) {
                return AppReleasesResult.Failure("Telegram link must use https://t.me or https://telegram.me.")
            }
        }

        val json = JSONObject().apply {
            put("version_code", release.versionCode)
            put("version_name", release.versionName.trim())
            put("changelog", release.changelog.trim())
            put("apk_url", release.apkUrl.trim())
            put("telegram_url", telegram ?: JSONObject.NULL)
            put("min_version_code", release.minVersionCode)
            put("published", release.published)
        }
        val reply = AdminSupabase.post(
            "app_versions?on_conflict=version_code",
            json.toString(),
            "resolution=merge-duplicates,return=representation"
        )
        if (!reply.ok) return AppReleasesResult.Failure(reply.errorText())
        if (reply.body.trim().startsWith("[") && JSONArray(reply.body).length() == 0) {
            return AppReleasesResult.Failure("The database did not confirm the release change. Check admin permissions.")
        }
        return list()
    }

    private fun isHttpsUrl(value: String): Boolean {
        val uri = android.net.Uri.parse(value.trim())
        return uri.scheme == "https" && !uri.host.isNullOrBlank()
    }
}
