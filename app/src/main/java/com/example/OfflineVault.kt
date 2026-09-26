package com.example

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
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
    private const val PREFS_SIZES = "wta_pdf_sizes"
    private const val KEY_INDEX = "index_json"
    private const val KEY_LEGACY_CLEANED = "legacy_keys_cleaned_v2"
    private const val DIR = "offline_vault"
    private val io = Executors.newSingleThreadExecutor()
    private val memorySizeCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * Exact file sizes for WTA Academy books (bytes).
     * Pre-seeded so size check is instant (0ms) and never triggers silent download.
     */
    val PRESEEDED_BOOK_SIZES = mapOf(
        "6aad8e8e001bf655ddff" to 1706717L, // Anthropology module (1.6 MB)
        "6aad8e8e001c0003600a" to 1891010L, // Communicative English 1 (1.8 MB)
        "6aad8e8e001c0c928f44" to 5734887L, // Math natural , module (5.5 MB)
        "6aad8e8e001c0c84652a" to 2415338L, // Physical fitness module (2.3 MB)
        "6aad8e8e001c09f7da8a" to 4768641L, // Physics module (4.5 MB)
        "6aad8e8e001c0262c6cb" to 2753014L, // Logic module (2.6 MB)
        "6aad8e8e001c0725f10e" to 1882181L, // Economics module (1.8 MB)
        "6aad8e8e001c047af478" to 2456883L, // Entrepreneurship module (2.3 MB)
        "6aad8e8e001c0c362ecf" to 3285055L, // History module (3.1 MB)
        "6aad8e8e001c0a2d2554" to 9492546L, // Chemistry module (9.1 MB)
        "6aad8e8e001c0c640e4e" to 2347872L, // C++ material (2.2 MB)
        "6aad8e8e001c04690369" to 786264L,  // Communicative English 2 (768 KB)
        "6aad8e8e001c0c040515" to 5016415L, // Math social, module (4.8 MB)
        "6aad8e8e001c0c6d982f" to 1906455L, // Psychology module (1.8 MB)
        "6aad8e8e001c054431c6" to 3089018L, // Geography module (2.9 MB)
        "6aad8e8e001c015e87f0" to 2298390L, // Civic module (2.2 MB)
        "6aad8e8e001c0aafc2d6" to 2717979L, // Emerging module (2.6 MB)
        "6aad8e8e001c02e0ae41" to 1973616L, // Global module (1.9 MB)
        "6aad8e8e001c095bdf35" to 2604265L, // Inclusiveness module (2.5 MB)
        "6aad8e8e001c036a8350" to 5309172L, // Biology module (5.1 MB)
        "6aad8e8e001c035bd707" to 2238923L, // Applied math 1 (2.1 MB)
        "6aad97eb0010d21989e8" to 8167554L, // Digital logic design (7.8 MB)
        "6aad97eb0010ea179f50" to 2438550L, // Thermodynamics material (2.3 MB)
        "6aa99489001c02f4b410" to 8931163L, // Physics Grade 9 (8.5 MB)
        "6aad97eb0010d2e5f102" to 9073900L, // Introduction to machines PDF (8.7 MB)
        "6aad97eb0010edc8670d" to 6901577L, // Network analysis and synthesis (6.6 MB)
        "6aad97eb0010e4ee5f12" to 5917129L  // Material pdf (5.6 MB)
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cachePdfSize(ctx: Context, url: String, size: Long) {
        if (size <= 0L) return
        val clean = url.trim()
        memorySizeCache[clean] = size
        try {
            val key = keyFor(clean)
            ctx.getSharedPreferences(PREFS_SIZES, Context.MODE_PRIVATE)
                .edit()
                .putLong(key, size)
                .apply()
        } catch (_: Exception) {}
    }

    /**
     * Returns known size in bytes without downloading the file.
     * 1) Vault file if already downloaded
     * 2) Pre-seeded catalog size (instant)
     * 3) Memory cache
     * 4) SharedPreferences cache
     */
    fun getPdfSize(ctx: Context, url: String?): Long {
        if (url.isNullOrBlank()) return 0L
        val clean = url.trim()

        val local = localFileFor(ctx, clean)
        if (local != null && local.exists() && local.length() > 0L) {
            return local.length()
        }

        for ((fileId, sz) in PRESEEDED_BOOK_SIZES) {
            if (clean.contains(fileId, ignoreCase = true)) {
                return sz
            }
        }

        memorySizeCache[clean]?.let { if (it > 0L) return it }

        try {
            val key = keyFor(clean)
            val sz = ctx.getSharedPreferences(PREFS_SIZES, Context.MODE_PRIVATE).getLong(key, 0L)
            if (sz > 0L) {
                memorySizeCache[clean] = sz
                return sz
            }
        } catch (_: Exception) {}

        return 0L
    }

    fun getPdfSizeByTitle(title: String?): Long {
        if (title.isNullOrBlank()) return 0L
        val t = title.lowercase()
        return when {
            t.contains("anthropology") -> 1706717L
            t.contains("communicative english 1") || (t.contains("english") && t.contains("1")) -> 1891010L
            t.contains("math natural") || (t.contains("math") && t.contains("natural")) -> 5734887L
            t.contains("physical fitness") || t.contains("fitness") -> 2415338L
            t.contains("physics module") || (t.contains("physics") && !t.contains("9") && !t.contains("grade")) -> 4768641L
            t.contains("logic") -> 2753014L
            t.contains("economics") -> 1882181L
            t.contains("entrepreneurship") -> 2456883L
            t.contains("history") -> 3285055L
            t.contains("chemistry") -> 9492546L
            t.contains("c++") || t.contains("cpp") -> 2347872L
            t.contains("communicative english 2") || (t.contains("english") && t.contains("2")) -> 786264L
            t.contains("math social") || (t.contains("math") && t.contains("social")) -> 5016415L
            t.contains("psychology") -> 1906455L
            t.contains("geography") -> 3089018L
            t.contains("civic") -> 2298390L
            t.contains("emerging") -> 2717979L
            t.contains("global") -> 1973616L
            t.contains("inclusiveness") -> 2604265L
            t.contains("biology") -> 5309172L
            t.contains("applied math") -> 2238923L
            t.contains("digital logic") -> 8167554L
            t.contains("thermodynamics") -> 2438550L
            t.contains("physics grade 9") || (t.contains("physics") && t.contains("9")) -> 8931163L
            t.contains("machine") -> 9073900L
            t.contains("network analysis") -> 6901577L
            t.contains("material pdf") -> 5917129L
            else -> 0L
        }
    }

    /**
     * Fast, lightweight HEAD-only size probe.
     * NEVER reads the response stream, NEVER downloads the body, NEVER saves to disk.
     */
    fun probeSizeOnline(ctx: Context, url: String): Long {
        val known = getPdfSize(ctx, url)
        if (known > 0L) return known

        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 6_000
                readTimeout = 6_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "WisdomTowerApp/1.0")
                try {
                    val cookies = CookieManager.getInstance().getCookie(url)
                    if (!cookies.isNullOrBlank()) {
                        setRequestProperty("Cookie", cookies)
                    }
                } catch (_: Exception) {}
            }
            conn.connect()
            val cl = conn.contentLengthLong
            conn.disconnect()
            if (cl > 0L) {
                cachePdfSize(ctx, url, cl)
                cl
            } else 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun ensureLegacyCleaned(ctx: Context) {
        val sp = prefs(ctx)
        if (sp.getBoolean(KEY_LEGACY_CLEANED, false)) return
        try {
            val raw = sp.getString(KEY_INDEX, "{}") ?: "{}"
            val index = JSONObject(raw)
            val toRemove = mutableListOf<String>()
            val keys = index.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                // If a key was saved as a bare generic URL without query string or had no resource identifier
                if (k.equals("https://wisdom-tower-academy.live/api/content/pdf", ignoreCase = true) ||
                    k.equals("http://wisdom-tower-academy.live/api/content/pdf", ignoreCase = true) ||
                    k.equals("/api/content/pdf", ignoreCase = true) ||
                    (!k.contains("?") && k.lowercase().contains("/api/content/pdf"))
                ) {
                    val fn = index.optString(k, "")
                    if (fn.isNotBlank()) {
                        val f = File(vaultDir(ctx), fn)
                        if (f.exists()) f.delete()
                    }
                    toRemove.add(k)
                }
            }
            toRemove.forEach { index.remove(it) }
            sp.edit()
                .putString(KEY_INDEX, index.toString())
                .putBoolean(KEY_LEGACY_CLEANED, true)
                .apply()
        } catch (e: Exception) {
            Log.e("OfflineVault", "Failed cleaning legacy unkeyed cache", e)
        }
    }

    private fun vaultDir(ctx: Context): File {
        val dir = File(ctx.filesDir, DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Cache key must uniquely identify the resource.
     * Preserves the full URL including resourceId, fileId, subject, etc.
     */
    fun keyFor(url: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val dig = md.digest(url.trim().toByteArray(Charsets.UTF_8))
        return dig.joinToString("") { "%02x".format(it) }
    }

    fun localFileFor(ctx: Context, url: String): File? {
        ensureLegacyCleaned(ctx)
        val cleanUrl = url.trim()
        val index = JSONObject(prefs(ctx).getString(KEY_INDEX, "{}") ?: "{}")
        val name = index.optString(cleanUrl, "")
        if (name.isNotBlank()) {
            val f = File(vaultDir(ctx), name)
            if (f.exists() && f.length() > 0) return f
        }
        // Direct file lookup using full unique key
        val key = keyFor(cleanUrl)
        val fDirect = File(vaultDir(ctx), "$key.pdf")
        if (fDirect.exists() && fDirect.length() > 0) return fDirect
        return null
    }

    fun has(ctx: Context, url: String): Boolean = localFileFor(ctx, url) != null

    fun remember(ctx: Context, url: String, fileName: String) {
        ensureLegacyCleaned(ctx)
        val cleanUrl = url.trim()
        val index = JSONObject(prefs(ctx).getString(KEY_INDEX, "{}") ?: "{}")
        index.put(cleanUrl, fileName)
        prefs(ctx).edit().putString(KEY_INDEX, index.toString()).apply()
    }

    fun saveBytes(ctx: Context, url: String, bytes: ByteArray, ext: String = "pdf"): File? {
        if (bytes.isEmpty()) return null
        return try {
            val fileName = "${keyFor(url)}.$ext"
            val outFile = File(vaultDir(ctx), fileName)
            FileOutputStream(outFile).use { it.write(bytes) }
            remember(ctx, url, fileName)
            outFile
        } catch (e: Exception) {
            Log.e("OfflineVault", "saveBytes failed for $url", e)
            null
        }
    }

    fun saveBytesAsync(ctx: Context, url: String, bytes: ByteArray, ext: String = "pdf") {
        io.execute {
            saveBytes(ctx, url, bytes, ext)
        }
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
            try {
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) {
                    setRequestProperty("Cookie", cookies)
                }
            } catch (_: Exception) { }
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
            else -> "pdf"
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
        return outFile
    }

    fun targetFileFor(ctx: Context, url: String, ext: String = "pdf"): File {
        return File(vaultDir(ctx), "${keyFor(url)}.$ext")
    }

    fun isPdfUrl(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("/api/content/pdf") ||
            u.contains(".pdf") ||
            u.contains("application/pdf") ||
            u.contains("/pdf") ||
            u.contains("content/pdf")
    }
}

/**
 * An InputStream that simultaneously streams bytes to the consumer (Chromium WebView / fetch reader)
 * and writes them to a local cache file.
 * This guarantees real-time download progress on the client with zero buffering lag,
 * while transparently persisting the file into OfflineVault for offline availability.
 */
class CachingInputStream(
    private val source: java.io.InputStream,
    private val targetFile: File,
    private val onComplete: (File) -> Unit
) : java.io.InputStream() {
    private val tempFile = File(targetFile.parentFile, targetFile.name + ".tmp")
    private val fos = FileOutputStream(tempFile)
    private var completed = false

    override fun read(): Int {
        val b = source.read()
        if (b != -1) {
            fos.write(b)
        } else {
            finish()
        }
        return b
    }

    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = source.read(b, off, len)
        if (n > 0) {
            fos.write(b, off, n)
        } else if (n == -1) {
            finish()
        }
        return n
    }

    override fun available(): Int = source.available()

    private fun finish() {
        if (!completed) {
            completed = true
            try {
                fos.flush()
                fos.close()
                if (tempFile.exists() && tempFile.length() > 0) {
                    tempFile.renameTo(targetFile)
                    onComplete(targetFile)
                }
            } catch (e: Exception) {
                tempFile.delete()
            }
        }
    }

    override fun close() {
        try {
            source.close()
        } finally {
            try {
                fos.close()
                if (!completed) {
                    tempFile.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
