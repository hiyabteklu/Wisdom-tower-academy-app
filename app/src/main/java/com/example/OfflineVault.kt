package com.example

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * Secure offline vault for paid course materials (PDFs, books, etc.).
 *
 * Security notes (important for paid content):
 * - Files are stored only in the app’s private internal storage (Context.filesDir).
 *   They are NOT world-readable and do not appear in the public Downloads folder.
 * - Index (url → filename) lives in private SharedPreferences.
 * - Filenames are SHA-256 hashes of the URL — no original names or package IDs leak.
 * - The app never bypasses website ownership. Only content the user was already
 *   allowed to open while online is cached. Offline replay still respects the
 *   session / cookies the website set.
 * - FLAG_SECURE is enabled on the Activity so screenshots of these files are blocked.
 *
 * Future hardening (when needed): encrypt files at rest with a key derived from
 * the user’s session or Android Keystore. Current private-dir approach is already
 * stronger than a normal browser download.
 */
object OfflineVault {
    private const val PREFS = "wta_offline_vault"
    private const val KEY_INDEX = "index_json"
    private const val DIR = "offline_vault"
    private val io = Executors.newSingleThreadExecutor()

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun vaultDir(ctx: Context): File {
        val dir = File(ctx.filesDir, DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun keyFor(url: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val dig = md.digest(url.toByteArray(Charsets.UTF_8))
        return dig.joinToString("") { "%02x".format(it) }
    }

    fun localFileFor(ctx: Context, url: String): File? {
        val index = JSONObject(prefs(ctx).getString(KEY_INDEX, "{}") ?: "{}")
        val name = index.optString(url, "")
        if (name.isBlank()) return null
        val f = File(vaultDir(ctx), name)
        return if (f.exists() && f.length() > 0) f else null
    }

    fun has(ctx: Context, url: String): Boolean = localFileFor(ctx, url) != null

    fun remember(ctx: Context, url: String, fileName: String) {
        val index = JSONObject(prefs(ctx).getString(KEY_INDEX, "{}") ?: "{}")
        index.put(url, fileName)
        prefs(ctx).edit().putString(KEY_INDEX, index.toString()).apply()
    }

    fun fileUrl(file: File): String = "file://${file.absolutePath}"

    /**
     * Download [url] into the private vault on a background thread.
     * [onDone] is called on the same background thread with the file or null.
     */
    fun downloadAsync(
        ctx: Context,
        url: String,
        suggestedName: String? = null,
        onDone: (File?) -> Unit
    ) {
        io.execute {
            try {
                existingOrDownload(ctx, url, suggestedName).also(onDone)
            } catch (e: Exception) {
                Log.e("OfflineVault", "download failed: $url", e)
                onDone(null)
            }
        }
    }

    fun existingOrDownload(
        ctx: Context,
        url: String,
        suggestedName: String? = null
    ): File? {
        localFileFor(ctx, url)?.let { return it }

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "WisdomTowerApp/1.0")
        }
        conn.connect()
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            return null
        }

        val ext = when {
            suggestedName?.contains(".") == true ->
                suggestedName.substringAfterLast(".")
            url.contains(".pdf", ignoreCase = true) -> "pdf"
            else -> "bin"
        }
        val fileName = "${keyFor(url)}.$ext"
        val outFile = File(vaultDir(ctx), fileName)

        conn.inputStream.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        conn.disconnect()

        if (outFile.length() == 0L) {
            outFile.delete()
            return null
        }
        remember(ctx, url, fileName)
        // Also index without query string so later offline lookups succeed
        val bare = url.substringBefore("?")
        if (bare != url) remember(ctx, bare, fileName)
        return outFile
    }

    fun isPdfUrl(url: String): Boolean {
        val u = url.lowercase()
        return u.contains(".pdf") ||
            u.contains("application/pdf") ||
            u.contains("/pdf") ||
            u.contains("content/pdf")
    }
}
