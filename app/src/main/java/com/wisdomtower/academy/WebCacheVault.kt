package com.wisdomtower.academy

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
    private const val INDEX_FILE = "web_index_v4.json"
    private const val LEGACY_INDEX_FILE_V3 = "web_index_v3.json"
    private const val LEGACY_INDEX_FILE_V2 = "web_index_v2.json"
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
        loadIndexDirect(ctx)
        initialized = true
    }

    private fun loadIndexDirect(ctx: Context) {
        val dir = vaultDir(ctx)
        try {
            val v4File = File(dir, INDEX_FILE)
            if (v4File.exists()) {
                val raw = v4File.readText(Charsets.UTF_8)
                val json = JSONObject(raw)
                val keys = json.keys()
                var dirty = false
                while (keys.hasNext()) {
                    val k = keys.next()
                    val obj = json.getJSONObject(k)
                    val url = obj.optString("url", "")
                    // Purge any dynamic list/data API from index
                    if (isDynamicListOrDataApi(url)) {
                        dirty = true
                        try { File(dir, "$k.body").delete() } catch (_: Exception) {}
                        continue
                    }
                    val mime = obj.optString("mime", "text/html")
                    // Check cached HTML bodies: purge if containing degraded/error states
                    if (mime.contains("html")) {
                        val bodyFile = File(dir, "$k.body")
                        if (bodyFile.exists()) {
                            val content = try { bodyFile.readText(Charsets.UTF_8) } catch (_: Exception) { "" }
                            if (isDegradedOrEmptyHtml(content)) {
                                dirty = true
                                try { bodyFile.delete() } catch (_: Exception) {}
                                continue
                            }
                        }
                    }
                    index[k] = EntryMeta(
                        url = url,
                        mime = mime,
                        encoding = obj.optString("encoding", "utf-8").takeIf { it.isNotBlank() },
                        size = obj.optLong("size", 0L),
                        timestamp = obj.optLong("ts", System.currentTimeMillis())
                    )
                }
                if (dirty) {
                    persistIndex(ctx)
                }
                return
            }

            // One-time self-healing migration from v3: purge stale server-rendered HTML shells
            val v3File = File(dir, LEGACY_INDEX_FILE_V3)
            if (v3File.exists()) {
                Log.i(TAG, "Migrating web cache index from v3 to v4 (purging stale header/footer HTML)")
                val raw = v3File.readText(Charsets.UTF_8)
                val json = JSONObject(raw)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val obj = json.getJSONObject(k)
                    val url = obj.optString("url", "")
                    val mime = obj.optString("mime", "text/html")

                    // Strictly reject dynamic list/data APIs
                    if (isDynamicListOrDataApi(url)) {
                        try { File(dir, "$k.body").delete() } catch (_: Exception) {}
                        continue
                    }

                    // Pre-v4 HTML contains server-rendered header/footer shells; purge to prevent visual flash
                    if (mime.contains("html") || url.endsWith(".html") || (!url.contains(".") && !url.contains("/_next/"))) {
                        try { File(dir, "$k.body").delete() } catch (_: Exception) {}
                        continue
                    }

                    index[k] = EntryMeta(
                        url = url,
                        mime = mime,
                        encoding = obj.optString("encoding", "utf-8").takeIf { it.isNotBlank() },
                        size = obj.optLong("size", 0L),
                        timestamp = obj.optLong("ts", System.currentTimeMillis())
                    )
                }
                persistIndex(ctx)
                try { v3File.delete() } catch (_: Exception) {}
                try { File(dir, LEGACY_INDEX_FILE_V2).delete() } catch (_: Exception) {}
                Log.i(TAG, "Web index v4 migration complete with ${index.size} verified entries")
                return
            }

            // One-time self-healing migration from legacy v2: sanitize poisoned entries
            val v2File = File(dir, LEGACY_INDEX_FILE_V2)
            if (v2File.exists()) {
                Log.i(TAG, "Migrating web cache index from v2 to v4 with anti-poisoning sanitization")
                val raw = v2File.readText(Charsets.UTF_8)
                val json = JSONObject(raw)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val obj = json.getJSONObject(k)
                    val url = obj.optString("url", "")
                    val mime = obj.optString("mime", "text/html")

                    // Strictly reject dynamic list/data APIs
                    if (isDynamicListOrDataApi(url)) {
                        try { File(dir, "$k.body").delete() } catch (_: Exception) {}
                        continue
                    }

                    // Pre-v4 HTML contains server-rendered header/footer shells; purge
                    if (mime.contains("html") || url.endsWith(".html") || (!url.contains(".") && !url.contains("/_next/"))) {
                        try { File(dir, "$k.body").delete() } catch (_: Exception) {}
                        continue
                    }

                    index[k] = EntryMeta(
                        url = url,
                        mime = mime,
                        encoding = obj.optString("encoding", "utf-8").takeIf { it.isNotBlank() },
                        size = obj.optLong("size", 0L),
                        timestamp = obj.optLong("ts", System.currentTimeMillis())
                    )
                }
                persistIndex(ctx)
                try { v2File.delete() } catch (_: Exception) {}
                Log.i(TAG, "Web index v4 migration complete from v2 with ${index.size} verified entries")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading web index", e)
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
        val clean = url.trim().substringBefore('#')
        val withHost = if (clean.startsWith("/")) {
            "https://www.wisdom-tower-academy.live$clean"
        } else {
            clean
        }
        return withHost.replace("https://wisdom-tower-academy.live", "https://www.wisdom-tower-academy.live")
            .replace("http://wisdom-tower-academy.live", "https://www.wisdom-tower-academy.live")
            .replace("http://www.wisdom-tower-academy.live", "https://www.wisdom-tower-academy.live")
    }

    fun candidateKeysFor(url: String): List<String> {
        val norm = normalizeUrl(url)
        val withoutQuery = norm.substringBefore('?')
        val cleanWithoutSlash = withoutQuery.removeSuffix("/")
        val withSlash = "$cleanWithoutSlash/"
        val keys = mutableListOf<String>()
        keys.add(keyFor(norm))
        keys.add(keyFor(withoutQuery))
        keys.add(keyFor(cleanWithoutSlash))
        keys.add(keyFor(withSlash))

        // If it refers to root / home
        if (cleanWithoutSlash == "https://www.wisdom-tower-academy.live" || cleanWithoutSlash.isEmpty()) {
            keys.add(keyFor("https://www.wisdom-tower-academy.live/"))
            keys.add(keyFor("https://www.wisdom-tower-academy.live"))
            keys.add(keyFor("/"))
            keys.add(keyFor(""))
        }
        return keys.distinct()
    }

    fun baseRouteKey(url: String): String {
        val norm = normalizeUrl(url)
        val withoutQuery = norm.substringBefore('?')
        return keyFor(withoutQuery)
    }

    /**
     * Identifies dynamic catalog, material list, package, and database queries.
     *
     * Why dynamic list APIs must not be vaulted:
     * When material lists or package indexes change on the server, serving a stale or
     * mismatched cached API JSON response offline can cause the web UI to conclude that
     * there is "no published material", effectively hiding usable cached HTML pages and PDFs.
     * Dynamic list APIs must always be fetched fresh online and never persisted to the vault.
     */
    fun isDynamicListOrDataApi(url: String): Boolean {
        if (url.isBlank()) return false
        val clean = url.lowercase().trim()

        // Supabase endpoints (rest, graphql, functions, auth - excluding media storage)
        if (clean.contains("supabase.co") && !clean.contains("supabase.co/storage")) return true

        // Next.js dynamic client page props and React Server Component payloads
        if (clean.contains("/_next/data/") ||
            clean.contains("_rsc=") || clean.contains("?_rsc") ||
            clean.contains("&_rsc") || clean.contains("/_rsc") ||
            clean.contains("/__next")
        ) {
            return true
        }

        // Dynamic API endpoints (materials, packages, catalogs, published content, stats)
        if (clean.contains("/api/")) {
            val isStaticAsset = clean.endsWith(".js") || clean.endsWith(".css") ||
                clean.endsWith(".png") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") ||
                clean.endsWith(".webp") || clean.endsWith(".svg") || clean.endsWith(".woff2") ||
                clean.endsWith(".woff") || clean.endsWith(".ico")
            if (!isStaticAsset) {
                return true
            }
        }

        if (clean.contains("/graphql")) return true

        return false
    }

    /**
     * Detects error fallbacks, empty hub shells, or degraded states that must NEVER
     * overwrite a rich offline cache or be frozen into the vault.
     */
    fun isDegradedOrEmptyHtml(html: String): Boolean {
        if (html.isBlank() || html.length < 300) return true
        val lower = html.lowercase()
        // Website-owned string when hub list is offline or empty
        if (lower.contains("open this hub once online to cache it")) return true
        // Website-owned fallback states for empty or failed dynamic catalog
        if (lower.contains("no published material")) return true
        if (lower.contains("no materials found")) return true
        // Next.js / client error strings
        if (lower.contains("temporary display issue")) return true
        if (lower.contains("application error: a client-side exception")) return true
        if (lower.contains("chunkloaderror") || lower.contains("loading chunk")) return true
        if (lower.contains("err_internet_disconnected") || lower.contains("err_name_not_resolved")) return true
        return false
    }

    fun isCacheable(url: String): Boolean {
        if (url.isBlank()) return false
        val clean = url.lowercase().trim()
        if (clean.startsWith("blob:") || clean.startsWith("data:") || clean.startsWith("file:")) return false
        if (clean.contains("/api/auth/signout") || clean.contains("logout")) return false
        if (OfflineVault.isPdfUrl(url)) return false // Handled specifically by OfflineVault
        if (isDynamicListOrDataApi(url)) return false // Dynamic list / data APIs must never be persisted

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

        return isWtaHost || isImageOrMedia || isStaticAsset
    }

    fun has(ctx: Context, url: String): Boolean {
        if (!initialized) init(ctx)
        val dir = vaultDir(ctx)
        for (k in candidateKeysFor(url)) {
            if (index.containsKey(k)) {
                val f = File(dir, "$k.body")
                if (f.exists() && f.length() > 0L) return true
            }
            val f = File(dir, "$k.body")
            if (f.exists() && f.length() > 0L) return true
        }
        return false
    }

    fun hasAnyPage(ctx: Context): Boolean {
        if (!initialized) init(ctx)
        if (index.values.any { (it.mime.contains("html") || it.url.contains("wisdom-tower-academy.live")) && it.size > 0 }) {
            return true
        }
        val dir = vaultDir(ctx)
        val bodyFiles = dir.listFiles { f -> f.extension == "body" && f.length() > 0 }
        return !bodyFiles.isNullOrEmpty()
    }

    fun saveHtmlPage(ctx: Context, url: String, html: String) {
        if (html.isBlank() || url.isBlank()) return
        if (isDynamicListOrDataApi(url)) return
        if (isDegradedOrEmptyHtml(html)) {
            Log.d(TAG, "Refusing to vault degraded or empty HTML for $url")
            return
        }
        val norm = normalizeUrl(url)
        val bytes = html.toByteArray(Charsets.UTF_8)
        save(ctx, norm, "text/html", "utf-8", bytes)
        io.execute {
            try {
                extractAndPrecacheAssets(ctx, html)
            } catch (_: Exception) {}
        }
    }

    fun save(ctx: Context, url: String, mime: String, encoding: String?, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        if (isDynamicListOrDataApi(url)) return
        val norm = normalizeUrl(url)

        if (mime.startsWith("text/html")) {
            val text = try { String(bytes, Charsets.UTF_8) } catch (_: Exception) { "" }
            if (isDegradedOrEmptyHtml(text)) {
                Log.d(TAG, "Refusing to save degraded/empty HTML bytes for $norm")
                return
            }
        }

        val dir = vaultDir(ctx)
        val meta = EntryMeta(
            url = norm,
            mime = mime,
            encoding = encoding,
            size = bytes.size.toLong(),
            timestamp = System.currentTimeMillis()
        )
        val keys = candidateKeysFor(norm)
        for (k in keys) {
            try {
                val bodyFile = File(dir, "$k.body")
                if (!bodyFile.exists() || bodyFile.length() != bytes.size.toLong()) {
                    FileOutputStream(bodyFile).use { it.write(bytes) }
                }
                index[k] = meta
            } catch (_: Exception) {}
        }
        io.execute { persistIndex(ctx) }
    }

    const val CRITICAL_CHROME_STYLE =
        "<style id=\"wta-critical-hide\">" +
        "html,body{background-color:#060B15!important;color-scheme:dark!important;}" +
        "header:not([data-ai-tutor-header]),header:not([data-ai-tutor-header]).fixed.top-0,header:not([data-ai-tutor-header])[data-site-header],.site-header,[data-site-header],[role=\"banner\"]:not([data-ai-tutor-header])," +
        "nav[aria-label=\"Main\"],nav.hidden.md\\:flex,.site-nav,.site-navigation,[data-site-nav]," +
        "body > footer,footer.site-footer,[data-site-footer],[role=\"contentinfo\"],footer:not([data-ai-tutor-root] footer)," +
        ".hide-on-app,.app-hidden,[data-hide-on-app],[data-hide-app],.web-only,[data-web-only]," +
        "#nprogress,.nprogress,#nprogress .bar,[data-nprogress],#nextjs-toploader,.nextjs-toploader," +
        "nextjs-portal,[data-nextjs-dialog-overlay],[data-nextjs-toast]," +
        "header:not([data-ai-tutor-header]) button[aria-label*=\"menu\" i],button[aria-label*=\"menu\" i],.mobile-menu,[data-mobile-menu]," +
        "nav[aria-label*=\"mobile\" i],[data-bottom-nav],.bottom-nav,nav.fixed.bottom-0," +
        "img.wta-img-broken,img:not([src]),img[src=\"\"]" +
        "{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;opacity:0!important;pointer-events:none!important;margin:0!important;padding:0!important;}" +
        "div:has(#wt-ai-tutor-input){bottom:0!important;}" +
        "[data-ai-tutor-root] footer,#wt-ai-tutor-input,[role=\"dialog\"] footer" +
        "{display:block!important;visibility:visible!important;height:auto!important;max-height:none!important;opacity:1!important;pointer-events:auto!important;}" +
        "</style>"

    fun getCachedResponse(ctx: Context, url: String): WebResourceResponse? {
        val cleanUrl = url.lowercase()
        if (cleanUrl.contains("animation.gif")) {
            return try {
                val stream = ctx.assets.open("brand/animation.gif")
                val headers = mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "Content-Type" to "image/gif",
                    "Cache-Control" to "public, max-age=31536000, immutable"
                )
                WebResourceResponse("image/gif", null, 200, "OK", headers, stream)
            } catch (_: Exception) { null }
        }
        if (cleanUrl.contains("brand/logo.png") || cleanUrl.endsWith("/logo.png")) {
            return try {
                val stream = ctx.assets.open("brand/logo.png")
                val headers = mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "Content-Type" to "image/png",
                    "Cache-Control" to "public, max-age=31536000, immutable"
                )
                WebResourceResponse("image/png", null, 200, "OK", headers, stream)
            } catch (_: Exception) { null }
        }

        if (!initialized) init(ctx)
        val dir = vaultDir(ctx)
        var matchedKey: String? = null
        var matchedMeta: EntryMeta? = null
        var matchedFile: File? = null

        for (k in candidateKeysFor(url)) {
            val f = File(dir, "$k.body")
            if (f.exists() && f.length() > 0L) {
                matchedKey = k
                matchedMeta = index[k]
                matchedFile = f
                break
            }
        }

        if (matchedFile == null) {
            return null
        }

        val norm = normalizeUrl(url)
        val mime = matchedMeta?.mime ?: when {
            norm.endsWith(".css") || norm.contains(".css?") -> "text/css"
            norm.endsWith(".js") || norm.contains(".js?") -> "application/javascript"
            norm.endsWith(".png") -> "image/png"
            norm.endsWith(".jpg") || norm.endsWith(".jpeg") -> "image/jpeg"
            norm.endsWith(".webp") -> "image/webp"
            norm.endsWith(".svg") -> "image/svg+xml"
            else -> "text/html"
        }
        val encoding = matchedMeta?.encoding ?: if (mime.startsWith("text/") || mime.contains("javascript")) "utf-8" else null

        val cacheControlHeader = when {
            norm.contains("/_next/static/") -> "public, max-age=31536000, immutable"
            mime.startsWith("text/html") -> "no-cache, must-revalidate"
            mime.contains("json") -> "no-cache, must-revalidate"
            else -> "public, max-age=86400"
        }

        return try {
            val headers = HashMap<String, String>().apply {
                put("Access-Control-Allow-Origin", "*")
                put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                put("Access-Control-Allow-Headers", "*")
                put("Cache-Control", cacheControlHeader)
                put("Content-Type", mime)
                put("X-WTA-Cache", "VAULT-HIT")
            }
            val stream: InputStream = if (mime.startsWith("text/html")) {
                val raw = matchedFile.readText(Charsets.UTF_8)
                val injected = if (raw.contains("<head", ignoreCase = true)) {
                    raw.replaceFirst(Regex("(?i)<head[^>]*>"), "$0$CRITICAL_CHROME_STYLE")
                } else if (raw.contains("<html", ignoreCase = true)) {
                    raw.replaceFirst(Regex("(?i)<html[^>]*>"), "$0<head>$CRITICAL_CHROME_STYLE</head>")
                } else {
                    "$CRITICAL_CHROME_STYLE$raw"
                }
                headers["Content-Type"] = "text/html; charset=utf-8"
                ByteArrayInputStream(injected.toByteArray(Charsets.UTF_8))
            } else {
                headers["Content-Length"] = matchedFile.length().toString()
                FileInputStream(matchedFile)
            }
            WebResourceResponse(
                mime,
                encoding,
                200,
                "OK",
                headers,
                stream
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
                connectTimeout = 15_000
                readTimeout = 25_000
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

                val cacheControlHeader = when {
                    fullUrl.contains("/_next/static/") -> "public, max-age=31536000, immutable"
                    mime.startsWith("text/html") -> "no-cache, must-revalidate"
                    mime.contains("json") -> "no-cache, must-revalidate"
                    else -> "public, max-age=86400"
                }

                val headers = HashMap<String, String>().apply {
                    put("Access-Control-Allow-Origin", "*")
                    put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                    put("Access-Control-Allow-Headers", "*")
                    put("Cache-Control", cacheControlHeader)
                    put("Content-Type", rawMime)
                    put("X-WTA-Cache", "NETWORK-STREAM")
                }

                if (isDynamicListOrDataApi(fullUrl)) {
                    // Online: allow dynamic list response to flow directly to WebView without disk persistence
                    return WebResourceResponse(mime, encoding, code, "OK", headers, conn.inputStream)
                }

                val key = keyFor(fullUrl)
                val tempFile = File(vaultDir(ctx), "$key.tmp")
                val finalFile = File(vaultDir(ctx), "$key.body")

                val stream = WebStreamingCacheInputStream(
                    source = conn.inputStream,
                    tempFile = tempFile,
                    finalFile = finalFile
                ) { length ->
                    if (mime.contains("html")) {
                        val content = try { finalFile.readText(Charsets.UTF_8) } catch (_: Exception) { "" }
                        if (isDegradedOrEmptyHtml(content)) {
                            Log.d(TAG, "Discarding degraded streamed HTML for $fullUrl")
                            try { finalFile.delete() } catch (_: Exception) {}
                            return@WebStreamingCacheInputStream
                        }
                    }
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
        if (isDynamicListOrDataApi(fullUrl)) return
        val norm = normalizeUrl(fullUrl)
        if (has(ctx, norm)) return
        io.execute {
            try {
                if (!initialized) init(ctx)
                if (has(ctx, norm)) return@execute
                val conn = (URL(norm).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
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
                            connectTimeout = 12_000
                            readTimeout = 20_000
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
