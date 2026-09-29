package com.example

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * High-speed offline vault for web assets, HTML pages, hubs, thumbnails,
 * notes, text bodies, and static Next.js resources.
 *
 * Ensures that anything visited online (or pre-cached on app launch)
 * opens completely offline without prompting for network connection.
 */
object WebCacheVault {
    private const val TAG = "WebCacheVault"
    private const val DIR = "wta_web_vault"
    private const val INDEX_FILE = "web_index_v2.json"
    private const val PREFS = "wta_web_cache_prefs"

    val io = Executors.newFixedThreadPool(2)
    private var initialized = false

    data class EntryMeta(
        val url: String,
        val mime: String,
        val encoding: String?,
        val size: Long,
        val timestamp: Long
    )

    private val index = ConcurrentHashMap<String, EntryMeta>()

    // 1x1 transparent PNG for non-cached image fallback offline
    val EMPTY_PNG = byteArrayOf(
        137.toByte(), 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82,
        0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, -60, -119, 0, 0, 0, 10,
        73, 68, 65, 84, 120, -100, 99, 0, 1, 0, 0, 5, 0, 1, 13, 10, 45, -76,
        0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126
    )

    val CORE_HUBS = listOf(
        "https://www.wisdom-tower-academy.live/",
        "https://www.wisdom-tower-academy.live/learning",
        "https://www.wisdom-tower-academy.live/packages",
        "https://www.wisdom-tower-academy.live/account",
        "https://www.wisdom-tower-academy.live/settings",
        "https://www.wisdom-tower-academy.live/academy",
        "https://www.wisdom-tower-academy.live/academy/grades/12/books",
        "https://www.wisdom-tower-academy.live/academy/grades/12/short-notes",
        "https://www.wisdom-tower-academy.live/academy/grades/12/flashcards",
        "https://www.wisdom-tower-academy.live/academy/grades/12/question-banks",
        "https://www.wisdom-tower-academy.live/academy/grades/12/exams",
        "https://www.wisdom-tower-academy.live/academy/grades/11/books",
        "https://www.wisdom-tower-academy.live/academy/freshman/books",
        "https://www.wisdom-tower-academy.live/academy/freshman/short-notes",
        "https://www.wisdom-tower-academy.live/academy/freshman/flashcards",
        "https://www.wisdom-tower-academy.live/academy/freshman/question-banks",
        "https://www.wisdom-tower-academy.live/academy/freshman/exams",
        "https://www.wisdom-tower-academy.live/academy/freshman",
        "https://www.wisdom-tower-academy.live/academy/scholarships",
        "https://www.wisdom-tower-academy.live/academy/success-stories",
        "https://www.wisdom-tower-academy.live/academy/study-techniques",
        "https://www.wisdom-tower-academy.live/academy/campus-life",
        "https://www.wisdom-tower-academy.live/academy/departments",
        "https://www.wisdom-tower-academy.live/academy/universities",
        "https://www.wisdom-tower-academy.live/academy/coc",
        "https://www.wisdom-tower-academy.live/about",
        "https://www.wisdom-tower-academy.live/contact"
    )

    fun vaultDir(ctx: Context): File {
        val dir = File(ctx.filesDir, DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    @Synchronized
    fun init(ctx: Context) {
        if (initialized) return
        initialized = true
        io.execute {
            try {
                val f = File(vaultDir(ctx), INDEX_FILE)
                if (f.exists()) {
                    val raw = f.readText(Charsets.UTF_8)
                    val json = JSONObject(raw)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val obj = json.getJSONObject(k)
                        index[k] = EntryMeta(
                            url = obj.optString("url", ""),
                            mime = obj.optString("mime", "text/html"),
                            encoding = obj.optString("encoding", "utf-8").takeIf { it.isNotBlank() },
                            size = obj.optLong("size", 0L),
                            timestamp = obj.optLong("ts", System.currentTimeMillis())
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading web index", e)
            }
        }
    }

    private fun persistIndex(ctx: Context) {
        try {
            val json = JSONObject()
            for ((k, meta) in index) {
                val o = JSONObject()
                o.put("url", meta.url)
                o.put("mime", meta.mime)
                o.put("encoding", meta.encoding ?: "")
                o.put("size", meta.size)
                o.put("ts", meta.timestamp)
                json.put(k, o)
            }
            val f = File(vaultDir(ctx), INDEX_FILE)
            f.writeText(json.toString(), Charsets.UTF_8)
        } catch (_: Exception) {}
    }

    fun keyFor(url: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val dig = md.digest(url.trim().toByteArray(Charsets.UTF_8))
        return dig.joinToString("") { "%02x".format(it) }
    }

    fun normalizeUrl(url: String): String {
        val clean = url.trim()
        val withoutHash = clean.substringBefore('#')
        return if (withoutHash.startsWith("/")) {
            "https://www.wisdom-tower-academy.live$withoutHash"
        } else {
            withoutHash
        }
    }

    fun baseRouteKey(url: String): String {
        val norm = normalizeUrl(url)
        val withoutQuery = norm.substringBefore('?')
        return keyFor(withoutQuery)
    }

    fun isCacheable(url: String): Boolean {
        if (url.isBlank()) return false
        val clean = url.lowercase().trim()
        if (clean.startsWith("blob:") || clean.startsWith("data:") || clean.startsWith("file:")) return false
        if (clean.contains("/api/auth/signout") || clean.contains("logout")) return false
        if (OfflineVault.isPdfUrl(url)) return false // Handled specifically by OfflineVault

        val uri = try { Uri.parse(clean) } catch (_: Exception) { null }
        val host = uri?.host ?: ""

        val isWtaHost = host.contains("wisdom-tower-academy.live") ||
            host.contains("wisdomtower.tech")

        val isImageOrMedia = clean.endsWith(".png") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") ||
            clean.endsWith(".webp") || clean.endsWith(".svg") || clean.endsWith(".gif") ||
            clean.endsWith(".ico") || clean.contains("/_next/image") || clean.contains("supabase.co/storage") ||
            clean.contains("images.unsplash.com")

        val isStaticAsset = clean.contains("/_next/static/") || clean.endsWith(".js") ||
            clean.endsWith(".css") || clean.endsWith(".woff2") || clean.endsWith(".woff")

        val isDataApi = clean.contains("/api/") || clean.contains("supabase.co/rest/v1")

        return isWtaHost || isImageOrMedia || isStaticAsset || isDataApi
    }

    fun has(ctx: Context, url: String): Boolean {
        if (!initialized) init(ctx)
        val norm = normalizeUrl(url)
        val k1 = keyFor(norm)
        if (index.containsKey(k1)) {
            val f = File(vaultDir(ctx), "$k1.body")
            if (f.exists() && f.length() > 0) return true
        }
        val k2 = baseRouteKey(norm)
        if (index.containsKey(k2)) {
            val f = File(vaultDir(ctx), "$k2.body")
            if (f.exists() && f.length() > 0) return true
        }
        return false
    }

    fun hasAnyPage(ctx: Context): Boolean {
        if (!initialized) init(ctx)
        return index.values.any { it.mime.contains("html") && it.size > 0 }
    }

    fun save(ctx: Context, url: String, mime: String, encoding: String?, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val norm = normalizeUrl(url)
        val key = keyFor(norm)
        val dir = vaultDir(ctx)
        val bodyFile = File(dir, "$key.body")
        try {
            FileOutputStream(bodyFile).use { it.write(bytes) }
            val meta = EntryMeta(
                url = norm,
                mime = mime,
                encoding = encoding,
                size = bytes.size.toLong(),
                timestamp = System.currentTimeMillis()
            )
            index[key] = meta

            // If it's a page navigation route (HTML), also index under base route without query parameters
            if (mime.contains("html") || norm.contains("wisdom-tower-academy.live")) {
                val baseKey = baseRouteKey(norm)
                if (baseKey != key) {
                    val baseFile = File(dir, "$baseKey.body")
                    if (!baseFile.exists() || baseFile.length() == 0L) {
                        try {
                            FileOutputStream(baseFile).use { it.write(bytes) }
                            index[baseKey] = meta
                        } catch (_: Exception) {}
                    }
                }
            }

            io.execute { persistIndex(ctx) }
        } catch (e: Exception) {
            Log.e(TAG, "save failed for $url", e)
        }
    }

    fun getCachedResponse(ctx: Context, url: String): WebResourceResponse? {
        if (!initialized) init(ctx)
        val norm = normalizeUrl(url)
        var key = keyFor(norm)
        var meta = index[key]
        var bodyFile = File(vaultDir(ctx), "$key.body")

        if (meta == null || !bodyFile.exists() || bodyFile.length() == 0L) {
            // Check base route key (ignoring query strings like ?_rsc=...)
            key = baseRouteKey(norm)
            meta = index[key]
            bodyFile = File(vaultDir(ctx), "$key.body")
        }

        if (meta == null || !bodyFile.exists() || bodyFile.length() == 0L) {
            return null
        }

        return try {
            val headers = HashMap<String, String>().apply {
                put("Access-Control-Allow-Origin", "*")
                put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                put("Access-Control-Allow-Headers", "*")
                put("Cache-Control", "public, max-age=31536000, immutable")
                put("Content-Type", meta.mime)
                put("Content-Length", bodyFile.length().toString())
                put("X-WTA-Cache", "VAULT-HIT")
            }
            WebResourceResponse(
                meta.mime,
                meta.encoding ?: "utf-8",
                200,
                "OK",
                headers,
                FileInputStream(bodyFile)
            )
        } catch (e: Exception) {
            Log.e(TAG, "getCachedResponse error for $url", e)
            null
        }
    }

    /**
     * Simultaneously streams network bytes to WebView and persists them to WebCacheVault.
     */
    fun fetchAndCache(ctx: Context, req: WebResourceRequest): WebResourceResponse? {
        val u = req.url?.toString() ?: return null
        val fullUrl = normalizeUrl(u)
        val method = req.method?.uppercase() ?: "GET"
        if (method != "GET" && method != "HEAD") return null

        return try {
            val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "WisdomTowerApp/1.0 (Linux; Android)")
                try {
                    val cookies = CookieManager.getInstance().getCookie(fullUrl)
                    if (!cookies.isNullOrBlank()) {
                        setRequestProperty("Cookie", cookies)
                    }
                } catch (_: Exception) {}

                req.requestHeaders?.forEach { (k, v) ->
                    if (!k.equals("Cookie", ignoreCase = true) &&
                        !k.equals("User-Agent", ignoreCase = true) &&
                        !k.equals("Host", ignoreCase = true)
                    ) {
                        setRequestProperty(k, v)
                    }
                }
            }
            conn.connect()
            val code = conn.responseCode
            if (code in 200..299) {
                val rawMime = conn.contentType ?: "text/html; charset=utf-8"
                val mime = rawMime.substringBefore(';').trim()
                val encoding = if (rawMime.contains("charset=", ignoreCase = true)) {
                    rawMime.substringAfter("charset=", "utf-8").substringBefore(';').trim()
                } else "utf-8"

                val headers = HashMap<String, String>().apply {
                    put("Access-Control-Allow-Origin", "*")
                    put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                    put("Access-Control-Allow-Headers", "*")
                    put("Cache-Control", "public, max-age=31536000, immutable")
                    put("Content-Type", rawMime)
                    put("X-WTA-Cache", "NETWORK-STREAM")
                }

                val key = keyFor(fullUrl)
                val tempFile = File(vaultDir(ctx), "$key.tmp")
                val finalFile = File(vaultDir(ctx), "$key.body")

                val stream = WebStreamingCacheInputStream(
                    source = conn.inputStream,
                    tempFile = tempFile,
                    finalFile = finalFile
                ) { length ->
                    val meta = EntryMeta(
                        url = fullUrl,
                        mime = mime,
                        encoding = encoding,
                        size = length,
                        timestamp = System.currentTimeMillis()
                    )
                    index[key] = meta
                    if (mime.contains("html") || fullUrl.contains("wisdom-tower-academy.live")) {
                        val baseKey = baseRouteKey(fullUrl)
                        if (baseKey != key) {
                            val baseFile = File(vaultDir(ctx), "$baseKey.body")
                            try {
                                if (finalFile.exists()) finalFile.copyTo(baseFile, overwrite = true)
                                index[baseKey] = meta
                            } catch (_: Exception) {}
                        }
                    }
                    io.execute { persistIndex(ctx) }
                }

                WebResourceResponse(mime, encoding, 200, "OK", headers, stream)
            } else {
                conn.disconnect()
                // If network returned error, fall back to cached copy if available
                getCachedResponse(ctx, fullUrl)
            }
        } catch (_: Exception) {
            // Network failure: fall back to cached copy
            getCachedResponse(ctx, fullUrl)
        }
    }

    /**
     * Asynchronously downloads and stores any cacheable URL to the vault in the background.
     */
    fun cacheUrlAsync(ctx: Context, fullUrl: String) {
        if (fullUrl.isBlank()) return
        val norm = normalizeUrl(fullUrl)
        if (has(ctx, norm)) return
        io.execute {
            try {
                if (!initialized) init(ctx)
                if (has(ctx, norm)) return@execute
                val conn = (URL(norm).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "WisdomTowerApp/1.0 (Linux; Android)")
                }
                conn.connect()
                if (conn.responseCode in 200..299) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    val rawMime = conn.contentType ?: when {
                        norm.contains(".png") -> "image/png"
                        norm.contains(".jpg") || norm.contains(".jpeg") -> "image/jpeg"
                        norm.contains(".webp") -> "image/webp"
                        norm.contains(".svg") -> "image/svg+xml"
                        norm.contains(".css") -> "text/css"
                        norm.contains(".js") -> "application/javascript"
                        else -> "application/octet-stream"
                    }
                    val mime = rawMime.substringBefore(';').trim()
                    save(ctx, norm, mime, null, bytes)
                }
                conn.disconnect()
            } catch (_: Exception) {}
        }
    }

    /**
     * Pre-caches all key academy hubs and their core static scripts & stylesheets
     * in the background when app is online.
     */
    fun precacheHubsAsync(ctx: Context) {
        io.execute {
            try {
                if (!initialized) init(ctx)
                for (hub in CORE_HUBS) {
                    try {
                        val conn = (URL(hub).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 6_000
                            readTimeout = 10_000
                            setRequestProperty("User-Agent", "WisdomTowerApp/1.0 (Linux; Android)")
                            try {
                                val cookies = CookieManager.getInstance().getCookie(hub)
                                if (!cookies.isNullOrBlank()) setRequestProperty("Cookie", cookies)
                            } catch (_: Exception) {}
                        }
                        conn.connect()
                        if (conn.responseCode in 200..299) {
                            val bytes = conn.inputStream.use { it.readBytes() }
                            val rawMime = conn.contentType ?: "text/html"
                            val mime = rawMime.substringBefore(';').trim()
                            save(ctx, hub, mime, "utf-8", bytes)

                            // Extract Next.js script chunks, stylesheets, and images to cache them too
                            val html = String(bytes, Charsets.UTF_8)
                            extractAndPrecacheAssets(ctx, html)
                        }
                        conn.disconnect()
                    } catch (e: Exception) {
                        Log.d(TAG, "Hub pre-cache skipped for $hub: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "precacheHubsAsync encountered error", e)
            }
        }
    }

    private fun extractAndPrecacheAssets(ctx: Context, html: String) {
        val staticAssets = mutableSetOf<String>()

        // Find scripts: src="(/_next/static/[^"]+)"
        val scriptPattern = Pattern.compile("""src="(/_next/static/[^"]+)"""")
        val scriptMatcher = scriptPattern.matcher(html)
        while (scriptMatcher.find()) {
            scriptMatcher.group(1)?.let { staticAssets.add(it) }
        }

        // Find css: href="(/_next/static/[^"]+\.css[^"]*)""""
        val cssPattern = Pattern.compile("""href="(/_next/static/[^"]+\.css[^"]*)"""")
        val cssMatcher = cssPattern.matcher(html)
        while (cssMatcher.find()) {
            cssMatcher.group(1)?.let { staticAssets.add(it) }
        }

        // Find images: src="([^"]+\.(?:png|jpg|jpeg|webp|svg|gif|ico)[^"]*)""""
        val imgPattern = Pattern.compile("""src="([^"]+\.(?:png|jpg|jpeg|webp|svg|gif|ico)[^"]*)"""")
        val imgMatcher = imgPattern.matcher(html)
        while (imgMatcher.find()) {
            val u = imgMatcher.group(1) ?: continue
            if (!u.startsWith("data:") && !u.startsWith("blob:")) {
                staticAssets.add(u)
            }
        }

        // Find Next.js optimized images: src="(/_next/image\?[^"]+)""""
        val nextImgPattern = Pattern.compile("""src="(/_next/image\?[^"]+)"""")
        val nextImgMatcher = nextImgPattern.matcher(html)
        while (nextImgMatcher.find()) {
            val u = nextImgMatcher.group(1) ?: continue
            staticAssets.add(u)
        }

        // Find srcset thumbnails
        val srcsetPattern = Pattern.compile("""srcset="([^"]+)"""")
        val srcsetMatcher = srcsetPattern.matcher(html)
        while (srcsetMatcher.find()) {
            val setStr = srcsetMatcher.group(1) ?: continue
            for (p in setStr.split(',')) {
                val candidate = p.trim().substringBefore(' ').trim()
                if (candidate.isNotBlank() && !candidate.startsWith("data:") && !candidate.startsWith("blob:")) {
                    staticAssets.add(candidate)
                }
            }
        }

        // Download each asset sequentially in background without choking bandwidth
        for (rel in staticAssets) {
            val fullUrl = normalizeUrl(rel)
            if (has(ctx, fullUrl)) continue
            try {
                val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "WisdomTowerApp/1.0 (Linux; Android)")
                }
                conn.connect()
                if (conn.responseCode in 200..299) {
                    val assetBytes = conn.inputStream.use { it.readBytes() }
                    val rawMime = conn.contentType ?: when {
                        fullUrl.contains(".js") -> "application/javascript"
                        fullUrl.contains(".css") -> "text/css"
                        fullUrl.contains(".png") -> "image/png"
                        fullUrl.contains(".jpg") || fullUrl.contains(".jpeg") -> "image/jpeg"
                        fullUrl.contains(".webp") -> "image/webp"
                        fullUrl.contains(".svg") -> "image/svg+xml"
                        else -> "application/octet-stream"
                    }
                    val mime = rawMime.substringBefore(';').trim()
                    save(ctx, fullUrl, mime, null, assetBytes)
                }
                conn.disconnect()
            } catch (_: Exception) {}
        }
    }
}

/**
 * Streams bytes to the consumer in real time while spooling to a file in WebCacheVault.
 */
class WebStreamingCacheInputStream(
    private val source: InputStream,
    private val tempFile: File,
    private val finalFile: File,
    private val onComplete: (Long) -> Unit
) : InputStream() {
    private val fos = FileOutputStream(tempFile)
    private var totalBytes = 0L
    private var closed = false

    override fun read(): Int {
        val b = source.read()
        if (b != -1) {
            fos.write(b)
            totalBytes++
        } else {
            finish()
        }
        return b
    }

    override fun read(b: ByteArray): Int = read(b, 0, b.size)

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = source.read(b, off, len)
        if (n > 0) {
            fos.write(b, off, n)
            totalBytes += n
        } else if (n == -1) {
            finish()
        }
        return n
    }

    override fun available(): Int = source.available()

    private fun finish() {
        if (!closed) {
            closed = true
            try {
                fos.flush()
                fos.close()
                if (tempFile.exists() && tempFile.length() > 0) {
                    tempFile.renameTo(finalFile)
                    onComplete(finalFile.length())
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
                if (!closed) {
                    tempFile.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
