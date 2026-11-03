package com.wisdomtower.academy.data.content

import android.content.Context
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** One published row of `public.learning_resources`, reduced to what My Status V2 needs. */
data class CatalogItem(
    val id: String,
    val packageId: String,
    val scopePath: String,
    /** "<picker package id>/<subject slug>" (same key as `ResourceScopeIndex`), null when the path is empty */
    val subjectKey: String?,
    /** books, short-notes, videos, flashcards, question-banks, exams, life-savers */
    val hub: String,
    val title: String?,
    val chapter: Int?,
    val contentType: String?,
    /** generated column: number of cards or questions, null when the database has none */
    val itemCount: Int?
)

data class CatalogIndex(val items: List<CatalogItem>, val fetchedAtMs: Long)

/**
 * My Status V2 (S1): the published catalog with hub, title and chapter, so the status can be shown per subject, chapter
 * and exam. One public read (anon key, published rows only, like `ResourceScopeIndex`, which stays untouched), cached in
 * the app files folder, older cache used offline. Never logs bodies, only HTTP status codes. Never throws.
 *
 * OFFLINE STATUS (O5b, 2026-10-23): [getSaved] returns the saved copy only (any age, never touches the network) so the
 * hub lists can draw their rows offline from it (see `LearningContentRepository.list`).
 *
 * OLD COPY FIRST (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md slice P1d): [get] no longer waits for the network when
 * a saved copy exists. A copy younger than 24 h is returned as before. A copy older than 24 h is ALSO returned at once, and
 * one silent refresh runs in the background (only one at a time) and replaces the saved copy for the next call. An explicit
 * `forceRefresh` still reads the server, but with the short client (4 s + 8 s) when a saved copy exists, and falls back to
 * the copy on failure. With no saved copy (or an empty one) the long client (15 s + 20 s) is used, as before.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object ResourceCatalogIndex {
    private const val TAG = "ResourceCatalogIndex"
    private const val FILE_NAME = "status_catalog_index.json"
    private const val MAX_AGE_MS = 24L * 60L * 60L * 1000L
    private const val PAGE = 1000

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** P1d: used when a saved copy exists, so a slow connection never holds the screen for long. */
    private val shortHttp: OkHttpClient = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshing = AtomicBoolean(false)

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    /**
     * The saved copy at once when there is one (a copy older than 24 h also starts one silent refresh); otherwise a new read.
     * Null only when nothing is available. An empty saved copy counts as "nothing saved".
     */
    suspend fun get(context: Context, forceRefresh: Boolean = false): CatalogIndex? = withContext(Dispatchers.IO) {
        val cached = readCache(context)?.takeIf { it.items.isNotEmpty() }
        val now = System.currentTimeMillis()
        if (cached != null && !forceRefresh) {
            if (now - cached.fetchedAtMs >= MAX_AGE_MS) refreshInBackground(context)
            return@withContext cached
        }
        val fetched = fetchAll(if (cached != null) shortHttp else http)
        if (fetched != null) {
            val index = CatalogIndex(fetched, now)
            writeCache(context, index)
            index
        } else {
            cached
        }
    }

    /** OFFLINE STATUS (O5b): the saved copy of any age, read from the phone only (no network). Null when nothing was saved yet. */
    suspend fun getSaved(context: Context): CatalogIndex? = withContext(Dispatchers.IO) { readCache(context) }

    /** P1d: one silent refresh at a time; a failed or empty answer leaves the saved copy as it is. */
    private fun refreshInBackground(context: Context) {
        if (!refreshing.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        backgroundScope.launch {
            try {
                val fetched = fetchAll(shortHttp)
                if (fetched != null && fetched.isNotEmpty()) {
                    writeCache(appContext, CatalogIndex(fetched, System.currentTimeMillis()))
                }
            } finally {
                refreshing.set(false)
            }
        }
    }

    private fun optStr(o: JSONObject, key: String): String? =
        if (o.isNull(key)) null else o.optString(key, "").ifBlank { null }

    private fun optIntOrNull(o: JSONObject, key: String): Int? =
        if (o.isNull(key)) null else o.optInt(key)

    private fun fetchAll(client: OkHttpClient): List<CatalogItem>? {
        return try {
            val out = ArrayList<CatalogItem>()
            var offset = 0
            while (true) {
                val url = "$supabaseUrl/rest/v1/learning_resources".toHttpUrl().newBuilder()
                    .addQueryParameter("select", "id,package_id,scope_path,hub,title,chapter,content_type,item_count")
                    .addQueryParameter("published", "eq.true")
                    .addQueryParameter("order", "id")
                    .addQueryParameter("limit", PAGE.toString())
                    .addQueryParameter("offset", offset.toString())
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("Accept", "application/json")
                    .get()
                    .build()
                val count = client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "read failed: HTTP ${resp.code}")
                        return null
                    }
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val id = o.optString("id", "")
                        if (id.isBlank()) continue
                        val pkg = o.optString("package_id", "")
                        val path = optStr(o, "scope_path").orEmpty()
                        out.add(
                            CatalogItem(
                                id = id,
                                packageId = pkg,
                                scopePath = path,
                                subjectKey = ResourceScopeIndex.subjectKey(pkg, path),
                                hub = o.optString("hub", ""),
                                title = optStr(o, "title"),
                                chapter = optIntOrNull(o, "chapter"),
                                contentType = optStr(o, "content_type"),
                                itemCount = optIntOrNull(o, "item_count")
                            )
                        )
                    }
                    arr.length()
                }
                if (count < PAGE) break
                offset += PAGE
            }
            out
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchAll failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun readCache(context: Context): CatalogIndex? {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return null
            val root = JSONObject(file.readText())
            val arr = root.getJSONArray("items")
            val list = ArrayList<CatalogItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    CatalogItem(
                        id = o.getString("id"),
                        packageId = o.optString("pkg", ""),
                        scopePath = o.optString("path", ""),
                        subjectKey = optStr(o, "key"),
                        hub = o.optString("hub", ""),
                        title = optStr(o, "title"),
                        chapter = optIntOrNull(o, "chapter"),
                        contentType = optStr(o, "type"),
                        itemCount = optIntOrNull(o, "count")
                    )
                )
            }
            CatalogIndex(list, root.optLong("t", 0L))
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(context: Context, index: CatalogIndex) {
        try {
            val arr = JSONArray()
            for (it in index.items) {
                val o = JSONObject()
                    .put("id", it.id)
                    .put("pkg", it.packageId)
                    .put("path", it.scopePath)
                    .put("hub", it.hub)
                if (it.subjectKey != null) o.put("key", it.subjectKey)
                if (it.title != null) o.put("title", it.title)
                if (it.chapter != null) o.put("chapter", it.chapter)
                if (it.contentType != null) o.put("type", it.contentType)
                if (it.itemCount != null) o.put("count", it.itemCount)
                arr.put(o)
            }
            val root = JSONObject().put("t", index.fetchedAtMs).put("items", arr)
            File(context.filesDir, FILE_NAME).writeText(root.toString())
        } catch (e: Exception) {
            Log.w(TAG, "cache write failed: ${e.javaClass.simpleName}")
        }
    }
}
