package com.wisdomtower.academy.data.content

import android.content.Context
import android.graphics.BitmapFactory
import com.wisdomtower.academy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** Cached remote Home banner. The bundled Academy picture is always the offline / default fallback. */
object HomeBannerRepository {
    const val BUNDLED_FALLBACK = "file:///android_asset/images/home/academy.jpg"
    private const val PREFS = "home_banner_cache"
    private const val SETTINGS_PATH = "academy_home_banner?select=image_url,version,is_default&id=eq.1&limit=1"

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /** Synchronous local-only lookup, so a cached banner is drawn on the first Home frame. */
    fun cachedOrFallback(context: Context): Any {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString("file_name", null) ?: return BUNDLED_FALLBACK
        val file = File(File(context.cacheDir, "home_banner"), name)
        return file.takeIf { it.isFile && it.length() > 0L } ?: BUNDLED_FALLBACK
    }

    /**
     * Checks one small settings row. The image is downloaded only when its version changes.
     * A network or parse failure leaves the current cached / bundled image untouched.
     */
    suspend fun refresh(context: Context, onImageChanged: (Any) -> Unit) = withContext(Dispatchers.IO) {
        val base = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
        val anon = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()
        if (base.isBlank() || anon.isBlank()) return@withContext

        val settingsRequest = Request.Builder()
            .url("$base/rest/v1/$SETTINGS_PATH")
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        val row = try {
            http.newCall(settingsRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext
                val rows = JSONArray(response.body?.string().orEmpty())
                if (rows.length() == 0) return@withContext
                rows.getJSONObject(0)
            }
        } catch (e: Exception) {
            return@withContext
        }

        val contextApp = context.applicationContext
        val prefs = contextApp.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val dir = File(contextApp.cacheDir, "home_banner").apply { mkdirs() }
        val isDefault = row.optBoolean("is_default", true)
        val imageUrl = row.optString("image_url").takeIf { it.startsWith("https://") }
        if (isDefault || imageUrl == null) {
            dir.listFiles()?.forEach { it.delete() }
            prefs.edit().clear().apply()
            withContext(Dispatchers.Main) { onImageChanged(BUNDLED_FALLBACK) }
            return@withContext
        }

        val version = row.optLong("version", 0L)
        if (version <= 0L) return@withContext
        val cachedName = prefs.getString("file_name", null)
        val cachedFile = cachedName?.let { File(dir, it) }
        if (prefs.getLong("version", -1L) == version && cachedFile?.isFile == true && cachedFile.length() > 0L) {
            return@withContext
        }

        val target = File(dir, "banner_$version.jpg")
        val part = File(dir, "banner_$version.part")
        try {
            val request = Request.Builder().url(imageUrl).get().build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext
                val body = response.body ?: return@withContext
                body.byteStream().use { input ->
                    FileOutputStream(part).use { output -> input.copyTo(output) }
                }
            }
            if (!part.isFile || part.length() <= 0L || part.length() > 500_000L) {
                part.delete()
                return@withContext
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(part.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                part.delete()
                return@withContext
            }
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            prefs.edit()
                .putLong("version", version)
                .putString("file_name", target.name)
                .apply()
            dir.listFiles()?.filter { it != target }?.forEach { it.delete() }
            withContext(Dispatchers.Main) { onImageChanged(target) }
        } catch (e: Exception) {
            part.delete()
        }
    }
}
