package com.wisdomtower.academy.data.content

import android.content.Context
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.admin.AdminSupabase
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
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Reads the learning content (books, notes, flashcards, question banks, exams) straight from the SAME
 * Supabase table as the website, `public.learning_resources`.
 *
 * HARDENING STAGE 1 (2026-10-20, owner order: locks must be enforced by the database, not only drawn by the app):
 *  - LISTS ([list]) read only the light columns (title, chapter, type, `item_count`...) with the public key. Every
 *    published material is always listed. A list never asks for `body_md` or `meta` any more.
 *  - OPENING one material ([getResource], [fetchItem]) goes through the database function `open_resource(id)` with
 *    the student's own login. The database applies the lock rows, the Paid mode switch and the student's purchases
 *    or grants, and returns the full row (note text, cards, questions) or a refusal. A refused material is never
 *    saved on the phone, and a saved copy of a material that is locked now is deleted at the next refresh.
 *  - If the function is not reachable for a reason other than being offline (for example an old database), the
 *    old direct read is used, so a missing function never blanks the app. STAGE 2 (not done yet, after this
 *    version is installed by the students): take `body_md` and `meta` away from the public key so the direct
 *    read stops working for everybody.
 *
 * Query rules copied from `listResources` in `website-reference/src/lib/content.ts`:
 *  - hub `short-notes` also matches the old hub name `references`;
 *  - an ECE course is looked up under BOTH `ece/sem-1/<slug>` and `ece/sem-2/<slug>` (the real data stores
 *    every ECE row under `ece/sem-1/...`, also for Semester 2 courses, see `docs/CONTENT_SHAPES.md`);
 *    if nothing is found, fall back to `scope_path` containing the slug;
 *  - `freshman/math-natural` and `freshman/mathematics` are the same subject;
 *  - published rows only, ordered by `sort_order`.
 *
 * Offline: every successful answer is saved as a small JSON file in the app's private storage; when the
 * phone cannot reach the server the saved copy is returned (`fromCache = true`).
 *
 * F4 (2026-10-12, owner report problem 4: "opening material should be instant"): [getResource] is now
 * cache-first (a saved item opens at once and is refreshed quietly in the background for the next visit;
 * `refresh = true` forces a real download, used by the Download buttons), and [list] gives up on a slow
 * network after a few seconds when it has a saved copy, instead of waiting 15 seconds.
 *
 * The sign-in check (real content needs a signed-in student) belongs to the screen that calls this.
 *
 * STATUS: written, NOT compiled, NOT tested on a device (plan step N1, `docs/NATIVE_CONTENT_PLAN.md`).
 */
object LearningContentRepository {
    private const val TAG = "LearningContent"
    private const val NO_INTERNET = "Check your internet connection and try again."
    private const val LOAD_FAILED = "Unable to load materials right now. Please try again."
    private const val LOCKED_MESSAGE = "This material is locked right now."

    /** The database refused to open one material (locked, or the package is not owned). */
    private class ResourceLockedException : Exception(LOCKED_MESSAGE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Used when a saved copy exists: a slow or missing network must not keep the student waiting. */
    private val fastHttp = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /** Quiet background refreshes of saved items (never tied to a screen). */
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val ECE_SCOPE = Regex("^ece/(sem-[12])/([^/]+)$")
    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val APPWRITE_ID_REGEX = Regex("^[A-Za-z0-9._-]{1,64}$")

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    /** Columns of a light list (no note text, no quiz/flashcard JSON; `item_count` = number of cards / questions). */
    private const val LIGHT_COLUMNS =
        "id,package_id,scope_path,hub,title,chapter,sort_order,content_type,storage_path,published,item_count"

    /**
     * Lists the published resources of one course/subject ([scopePath], for example `ece/sem-2/eceg3082`,
     * `freshman/physics`, `grade/9/biology`) in one hub ([hub], for example `short-notes`).
     * HARDENING STAGE 1: [full] is kept so old callers still compile, but a list is ALWAYS light now; the heavy
     * columns are only returned by [getResource] through the database check.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun list(
        context: Context,
        scopePath: String,
        hub: String,
        full: Boolean = false
    ): ContentListResult = withContext(Dispatchers.IO) {
        val cacheFile = cacheFile(context, "list_${scopePath}_${hub}_light")
        try {
            val client = if (cacheFile.exists()) fastHttp else http
            val rows = fetchRows(scopePath, hub, client)
            writeCache(cacheFile, rows.toString())
            ContentListResult.Success(rows.objects().map(::rowToResource), false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "list failed: ${e.javaClass.simpleName}")
            val cached = readCache(cacheFile)?.let { runCatching { JSONArray(it) }.getOrNull() }
            if (cached != null) {
                ContentListResult.Success(cached.objects().map(::rowToResource), true)
            } else {
                ContentListResult.Failure(if (e is IOException) NO_INTERNET else LOAD_FAILED)
            }
        }
    }

    /**
     * Loads one resource with its full body and meta (note text, flashcards, questions), through the database
     * check ([fetchItem]).
     * Cache-first: a saved copy is returned at once (`fromCache = false`, it is not necessarily offline) and
     * refreshed quietly in the background. With [refresh] = true the server is asked first (a Download button):
     * the result has `fromCache = true` only when the server could not be reached and the old copy was used.
     * A material the database refuses is a Failure with a plain message and is never saved.
     */
    suspend fun getResource(context: Context, id: String, refresh: Boolean = false): ContentItemResult =
        withContext(Dispatchers.IO) {
            if (!UUID_REGEX.matches(id)) return@withContext ContentItemResult.NotFound
            val cacheFile = cacheFile(context, "item_$id")
            if (!refresh) {
                val saved = readCache(cacheFile)?.let { runCatching { JSONObject(it) }.getOrNull() }
                if (saved != null) {
                    refreshScope.launch {
                        try {
                            fetchItem(id, cacheFile, fastHttp)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w(TAG, "background refresh failed: ${e.javaClass.simpleName}")
                        }
                    }
                    return@withContext ContentItemResult.Found(rowToResource(saved), false)
                }
            }
            try {
                val row = fetchItem(id, cacheFile, http) ?: return@withContext ContentItemResult.NotFound
                ContentItemResult.Found(rowToResource(row), false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ResourceLockedException) {
                ContentItemResult.Failure(LOCKED_MESSAGE)
            } catch (e: Exception) {
                Log.w(TAG, "getResource failed: ${e.javaClass.simpleName}")
                val cached = readCache(cacheFile)?.let { runCatching { JSONObject(it) }.getOrNull() }
                if (cached != null) {
                    ContentItemResult.Found(rowToResource(cached), true)
                } else {
                    ContentItemResult.Failure(if (e is IOException) NO_INTERNET else LOAD_FAILED)
                }
            }
        }

    /**
     * The address of a book file on Appwrite (same URL the website's `/api/content/pdf` proxy reads), or null
     * when [storagePath] is not an `appwrite:<FILE_ID>` path. Uses only the public `NEXT_PUBLIC_APPWRITE_*`
     * values from BuildConfig. The id may be followed by `|...`; that part is cut off.
     */
    fun appwriteViewUrl(storagePath: String?): String? {
        if (storagePath == null || !storagePath.startsWith("appwrite:")) return null
        val fileId = storagePath.removePrefix("appwrite:").substringBefore('|').trim()
        if (!APPWRITE_ID_REGEX.matches(fileId)) return null
        val endpoint = BuildConfig.NEXT_PUBLIC_APPWRITE_ENDPOINT.trim().trimEnd('/')
        val project = BuildConfig.NEXT_PUBLIC_APPWRITE_PROJECT_ID.trim()
        val bucket = BuildConfig.NEXT_PUBLIC_APPWRITE_BUCKET_ID.trim()
        if (endpoint.isEmpty() || project.isEmpty() || bucket.isEmpty()) return null
        return "$endpoint/storage/buckets/$bucket/files/$fileId/view?project=$project"
    }

    /**
     * T4 (2026-10-12): Returns the item count for each hub of one course/subject ([scopePath]).
     * Only queries `select=hub` (very lightweight). Result is cached locally so subsequent visits
     * and offline mode show real counts instantly.
     */
    suspend fun getHubCounts(context: Context, scopePath: String): Map<String, Int> =
        withContext(Dispatchers.IO) {
            val cacheFile = cacheFile(context, "counts_$scopePath")
            val cachedMap = readCache(cacheFile)?.let { raw ->
                runCatching {
                    val json = JSONObject(raw)
                    val map = mutableMapOf<String, Int>()
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = json.optInt(k, 0)
                    }
                    map
                }.getOrNull()
            }

            try {
                val hubFilter = "in.(\"books\",\"short-notes\",\"references\",\"flashcards\",\"question-banks\",\"exams\",\"life-savers\")"
                val url = "$supabaseUrl/rest/v1/learning_resources".toHttpUrl().newBuilder()
                    .addQueryParameter("select", "hub")
                    .addQueryParameter("scope_path", scopeFilter(scopePath))
                    .addQueryParameter("hub", hubFilter)
                    .addQueryParameter("published", "eq.true")
                    .build().toString()
                var rows = JSONArray(get(url, fastHttp))
                val ece = ECE_SCOPE.matchEntire(scopePath)
                if (rows.length() == 0 && ece != null) {
                    val fallbackUrl = "$supabaseUrl/rest/v1/learning_resources".toHttpUrl().newBuilder()
                        .addQueryParameter("select", "hub")
                        .addQueryParameter("scope_path", "ilike.*${ece.groupValues[2]}*")
                        .addQueryParameter("hub", hubFilter)
                        .addQueryParameter("published", "eq.true")
                        .build().toString()
                    rows = JSONArray(get(fallbackUrl, fastHttp))
                }
                val counts = mutableMapOf<String, Int>()
                for (i in 0 until rows.length()) {
                    val obj = rows.optJSONObject(i) ?: continue
                    val rawHub = obj.optString("hub")
                    val h = if (rawHub == "references") "short-notes" else rawHub
                    counts[h] = (counts[h] ?: 0) + 1
                }
                val json = JSONObject()
                counts.forEach { (k, v) -> json.put(k, v) }
                writeCache(cacheFile, json.toString())
                counts
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                cachedMap ?: emptyMap()
            }
        }

    // ---------- internals ----------

    /**
     * Asks the DATABASE to open one published item (`open_resource`, with the student's own login) and saves the
     * row in [cacheFile]. Returns the row, or null when the item no longer exists or is no longer published (the
     * saved copy is then deleted). Throws [ResourceLockedException] when the database refuses it (the saved copy is
     * deleted too). Offline throws IOException. When the function cannot be used for another reason (not signed in,
     * function missing, server error), the old direct read is used.
     */
    private suspend fun fetchItem(id: String, cacheFile: File, client: OkHttpClient): JSONObject? {
        val door = AdminSupabase.post(
            "rpc/open_resource",
            JSONObject().put("p_id", id).toString(),
            "return=representation"
        )
        if (door.code == -1) throw IOException("Offline")
        if (door.ok) {
            val reply = runCatching { JSONObject(door.body) }.getOrNull()
            when (reply?.optString("status")) {
                "ok" -> {
                    val row = reply.getJSONObject("row")
                    writeCache(cacheFile, row.toString())
                    return row
                }
                "locked" -> {
                    runCatching { cacheFile.delete() }
                    throw ResourceLockedException()
                }
                "not_found" -> {
                    runCatching { cacheFile.delete() }
                    return null
                }
                else -> Log.w(TAG, "open_resource: status ${reply?.optString("status")}, using direct read")
            }
        } else {
            Log.w(TAG, "open_resource: HTTP ${door.code}, using direct read")
        }
        return fetchItemDirect(id, cacheFile, client)
    }

    /** The old direct read with the public key (stage 1 only; stage 2 removes the heavy columns from the public key). */
    private fun fetchItemDirect(id: String, cacheFile: File, client: OkHttpClient): JSONObject? {
        val url = "$supabaseUrl/rest/v1/learning_resources".toHttpUrl().newBuilder()
            .addQueryParameter("select", "*")
            .addQueryParameter("id", "eq.$id")
            .addQueryParameter("published", "eq.true")
            .addQueryParameter("limit", "1")
            .build().toString()
        val rows = JSONArray(get(url, client))
        if (rows.length() == 0) {
            runCatching { cacheFile.delete() }
            return null
        }
        val row = rows.getJSONObject(0)
        writeCache(cacheFile, row.toString())
        return row
    }

    private fun fetchRows(scopePath: String, hub: String, client: OkHttpClient): JSONArray {
        val hubFilter = if (hub == "short-notes") "in.(\"short-notes\",\"references\")" else "eq.$hub"
        var rows = queryRows(scopeFilter(scopePath), hubFilter, client)
        val ece = ECE_SCOPE.matchEntire(scopePath)
        if (rows.length() == 0 && ece != null) {
            rows = queryRows("ilike.*${ece.groupValues[2]}*", hubFilter, client)
        }
        return rows
    }

    /** The PostgREST filter for `scope_path`, with the ECE and math quirks of the website. */
    private fun scopeFilter(scopePath: String): String {
        val ece = ECE_SCOPE.matchEntire(scopePath)
        return when {
            ece != null -> {
                val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
                "in.(\"$scopePath\",\"ece/$other/${ece.groupValues[2]}\")"
            }
            scopePath == "freshman/math-natural" || scopePath == "freshman/mathematics" ->
                "in.(\"freshman/math-natural\",\"freshman/mathematics\")"
            else -> "eq.$scopePath"
        }
    }

    private fun queryRows(scopeFilter: String, hubFilter: String, client: OkHttpClient): JSONArray {
        val url = "$supabaseUrl/rest/v1/learning_resources".toHttpUrl().newBuilder()
            .addQueryParameter("select", LIGHT_COLUMNS)
            .addQueryParameter("scope_path", scopeFilter)
            .addQueryParameter("hub", hubFilter)
            .addQueryParameter("published", "eq.true")
            .addQueryParameter("order", "sort_order.asc")
            .build().toString()
        return JSONArray(get(url, client))
    }

    /** GET with the public key. Network trouble throws IOException; any other HTTP error throws IllegalStateException. */
    private fun get(url: String, client: OkHttpClient): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()
        return client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            text
        }
    }

    private fun rowToResource(row: JSONObject): LearningResource {
        val rawHub = row.optString("hub")
        return LearningResource(
            id = row.optString("id"),
            packageId = row.optString("package_id"),
            scopePath = row.optString("scope_path"),
            hub = if (rawHub == "references") "short-notes" else rawHub,
            title = row.optString("title"),
            chapter = if (row.isNull("chapter")) null else row.optInt("chapter"),
            sortOrder = row.optInt("sort_order", 0),
            contentType = row.optString("content_type"),
            storagePath = row.nstr("storage_path"),
            bodyMd = row.nstr("body_md"),
            metaJson = row.optJSONObject("meta")?.toString() ?: "{}",
            itemCount = if (row.has("item_count") && !row.isNull("item_count")) row.optInt("item_count") else null
        )
    }

    private fun JSONObject.nstr(key: String): String? {
        if (isNull(key)) return null
        val v = optString(key)
        return if (v == "null") null else v.ifBlank { null }
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }

    // ---------- small file cache (app private storage, no secrets) ----------

    private fun cacheFile(context: Context, key: String): File {
        val dir = File(context.filesDir, "content_cache").apply { mkdirs() }
        return File(dir, key.replace(Regex("[^A-Za-z0-9._-]"), "_").take(150) + ".json")
    }

    private fun writeCache(file: File, text: String) {
        runCatching { file.writeText(text) }
    }

    private fun readCache(file: File): String? =
        runCatching { if (file.exists()) file.readText() else null }.getOrNull()
}
