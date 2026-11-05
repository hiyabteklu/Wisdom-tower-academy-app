package com.wisdomtower.academy.data.content

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thrown when the server refuses a PAID file (locked, package required, or the student is not signed in).
 * It is an [IllegalStateException], so every existing `catch (e: Exception)` of the PDF screens treats it as a
 * plain failure; a screen that wants a better text can catch it first and show [message].
 */
class BookLockedException(message: String) : IllegalStateException(message)

/**
 * Reads the learning content (books, notes, flashcards, question banks, exams) straight from the SAME
 * Supabase table as the website, `public.learning_resources`, with the public anon key from BuildConfig.
 * Published rows are readable by everyone (policy `lr_public_select_published`, see
 * `docs/SUPABASE_NOTES.md`), so no secret and no website page is involved.
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
 * OFFLINE STATUS (O5b, 2026-10-23, docs/OFFLINE_STATUS_PLAN.md): when a hub list was never opened on this phone (no saved
 * list file) and the server cannot be reached, [list] now builds the rows from the saved catalog
 * ([ResourceCatalogIndex.getSaved]: id, title, chapter, hub, type, item count) and returns them with `fromCache = true`,
 * instead of the "Check your internet connection" failure. The Books hub is left out on purpose (a catalog row has no
 * file address). Such a row opens only when the item itself was saved before; otherwise the reader says it needs the
 * internet. A saved list file is still used first, exactly as before. Online behaviour is unchanged.
 *
 * F4 / R8 (2026-11-05): [getResource] uses the saved item immediately when Android has no validated network. When online,
 * it asks the checked server door BEFORE displaying a protected cached copy; a newly locked, deleted, device-revoked or
 * unsupported-version item is refused and its local JSON copy is removed. The short fastHttp timeout keeps this check
 * bounded. `refresh = true` still forces a server read, used by the Download buttons.
 *
 * P1a (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md, owner problem 8): [listSaved] returns the saved list of a hub
 * with NO network, so the hub list screen can draw it at once and then call [list] in the background (which already uses
 * the short `fastHttp` pair when a saved list file exists). [list] itself is unchanged.
 *
 * P1b (2026-11-03): [getHubCountsSaved] returns the saved hub counts of a course with NO network, so the course page can
 * put the counts on the hub cards at once and then call [getHubCounts] in the background. [getHubCounts] is unchanged.
 *
 * The sign-in check (real content needs a signed-in student) belongs to the screen that calls this.
 *
 * SECURITY S1 (2026-10-20, `docs/SECURITY_PLAN_CONTENT_ACCESS.md`): a single material (note text, cards, questions)
 * is now asked from the database's CHECKED DOOR, `rpc/open_resource`, with the student's own login token. The
 * server applies the lock rows, the Paid mode switch and the student's purchases / grants and refuses a locked
 * material. While [LEGACY_DIRECT_READ] is true the old direct read is still used as a fallback (signed-out token,
 * missing function, server error) so nothing breaks before plan slice S4. S4 sets it to false and revokes the
 * direct read in the database. A locked answer also deletes the saved copy of that item.
 *
 * SECURITY S2 (2026-10-20): lists are ALWAYS light now (no `body_md`, no `meta`): the heavy content of a material
 * only comes through [getResource] (the checked door). The count of cards / questions shown in a list row comes from
 * the database column `item_count` ([LearningResource.itemCount]). The `full` argument of [list] is kept so old
 * callers compile, but it is ignored. `storage_path` (book file address) is still in the light list: slice S5.
 *
 * SECURITY S4 (2026-10-20): done. The database now refuses the public key and every student login a read of
 * `body_md` and `meta` of `learning_resources` (column privileges), so [LEGACY_DIRECT_READ] is false and the heavy
 * content only comes through `rpc/open_resource`.
 *
 * SECURITY S5b (2026-10-20, app side of the paid PDFs): a paid PDF has `storage_path` = `appwrite-paid:<FILE_ID>`
 * (private Appwrite bucket `paid-pdfs`, no public address works). [appwriteViewUrl] gives such a path the MARKER
 * address `wtpaid://<FILE_ID>` (never a real URL). [BookFileStore.ensureFile] swaps the marker for a real address
 * made by the Supabase Edge Function `book-url` ([paidBookUrl], called with the student's own login token, valid
 * about 10 minutes) only when the file is not saved on the phone yet. Free books (`appwrite:<FILE_ID>`, public
 * bucket) work exactly as before.
 *
 * STATUS: written, NOT compiled, NOT tested on a device (plan step N1, `docs/NATIVE_CONTENT_PLAN.md`; S1, S2, S4, S5b, P1a and P1b not compiled).
 */
object LearningContentRepository {
    private const val TAG = "LearningContent"
    private const val NO_INTERNET = "Check your internet connection and try again."
    private const val LOAD_FAILED = "Unable to load materials right now. Please try again."
    private const val LOCKED_MESSAGE = "This material is locked. Unlock it with your package or ask the admin."
    private const val DEVICE_NOT_ACTIVE_MESSAGE = "This account is registered on another device. Contact Wisdom Tower Academy support to request a device reset."
    private const val SIGN_IN_MESSAGE = "Sign in to open this paid file."

    /** `storage_path` prefix of a paid PDF in the private Appwrite bucket (free books use `appwrite:`). */
    private const val PAID_PATH_PREFIX = "appwrite-paid:"

    /**
     * Marker "address" that [appwriteViewUrl] returns for a paid PDF. It is not a real URL: [BookFileStore.ensureFile]
     * recognises it and asks [paidBookUrl] for the real, short-lived address (the resource id is the cache key).
     */
    const val PAID_URL_PREFIX = "wtpaid://"

    /**
     * Slice S1 kept the old direct read as a fallback. Slice S4 (2026-10-20) sets this to false: the database no longer
     * lets the public key or a student login read `body_md` / `meta` of `learning_resources` (migration
     * `s4_hide_body_md_and_meta_from_public_columns`), so the direct read would only fail. Heavy content comes ONLY
     * through the checked door `rpc/open_resource`.
     */
    private const val LEGACY_DIRECT_READ = false

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Used when a saved copy exists: a slow or missing network must not keep the student waiting. */
    private val fastHttp = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /** Access-only checks should fail quickly so a genuinely offline saved PDF can still open. */
    private val fastAccessHttp = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val ECE_SCOPE = Regex("^ece/(sem-[12])/([^/]+)$")
    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val APPWRITE_ID_REGEX = Regex("^[A-Za-z0-9._-]{1,64}$")

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    /** A cached protected item is only treated as offline when Android has no validated network. */
    private fun hasValidatedNetwork(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Columns of a list (no note text, no quiz/flashcard JSON; `item_count` replaces counting inside `meta`). */
    private const val LIGHT_COLUMNS =
        "id,package_id,scope_path,hub,title,chapter,sort_order,content_type,storage_path,published,item_count,size_bytes,description"

    /** What the checked door answered for one material. */
    private sealed class Door {
        class Row(val row: JSONObject) : Door()
        object Gone : Door()
        object Locked : Door()
        object DeviceNotActive : Door()
        data class UpdateRequired(val minimumVersionCode: Int) : Door()
    }

    /**
     * Lists the published resources of one course/subject ([scopePath], for example `ece/sem-2/eceg3082`,
     * `freshman/physics`, `grade/9/biology`) in one hub ([hub], for example `short-notes`).
     * Rows are always light (security slice S2). [full] is IGNORED: heavy content (`body_md`, `meta`) only comes
     * through [getResource].
     *
     * OFFLINE STATUS (O5b): a failed read with no saved list file falls back to rows built from the saved catalog
     * ([catalogRows]), returned with `fromCache = true`; only when that finds nothing is the old failure returned.
     */
    suspend fun list(
        context: Context,
        scopePath: String,
        hub: String,
        @Suppress("UNUSED_PARAMETER") full: Boolean = false
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
                val fromCatalog = runCatching { catalogRows(context, scopePath, hub) }.getOrDefault(emptyList())
                if (fromCatalog.isNotEmpty()) {
                    ContentListResult.Success(fromCatalog, true)
                } else {
                    ContentListResult.Failure(if (e is IOException) NO_INTERNET else LOAD_FAILED)
                }
            }
        }
    }

    /**
     * P1a (2026-11-03): the saved list of one hub, read from the phone only (NO network, never waits). It is the same
     * file [list] writes after every successful read and falls back to after a failed one, so a hub list screen can
     * draw it at once and call [list] afterwards to refresh. Returns null when no list was ever saved for this course
     * and hub, when the file cannot be read, or when the saved list is empty (an old empty answer is not shown as
     * "nothing published" before the server has been asked). Rows are the same light rows as [list] gives; no lock
     * decision is made here (the screen still applies the lock rows and the checked door decides on open).
     */
    suspend fun listSaved(context: Context, scopePath: String, hub: String): List<LearningResource>? =
        withContext(Dispatchers.IO) {
            val raw = readCache(cacheFile(context, "list_${scopePath}_${hub}_light")) ?: return@withContext null
            val rows = runCatching { JSONArray(raw).objects().map(::rowToResource) }.getOrNull()
            if (rows.isNullOrEmpty()) null else rows
        }

    /**
     * OFFLINE STATUS (O5b): rows for a hub that was never opened on this phone, built from the saved catalog
     * ([ResourceCatalogIndex.getSaved], no network). Same scope and hub matching as [fetchRows] (ECE sem-1 / sem-2,
     * slug fallback, math alias, `references` = `short-notes`). The Books hub returns nothing (a catalog row has no
     * `storage_path`). Order: chapter (none last), then title (the real `sort_order` is not in the catalog). Fields the
     * catalog does not have stay empty: no file address, no body, empty meta. Empty list = nothing saved or no match.
     */
    private suspend fun catalogRows(context: Context, scopePath: String, hub: String): List<LearningResource> {
        if (hub == "books") return emptyList()
        val index = ResourceCatalogIndex.getSaved(context) ?: return emptyList()
        val ece = ECE_SCOPE.matchEntire(scopePath)
        val accepted: Set<String> = when {
            ece != null -> {
                val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
                setOf(scopePath, "ece/$other/${ece.groupValues[2]}")
            }
            scopePath == "freshman/math-natural" || scopePath == "freshman/mathematics" ->
                setOf("freshman/math-natural", "freshman/mathematics")
            else -> setOf(scopePath)
        }
        fun hubMatches(h: String): Boolean =
            if (hub == "short-notes") h == "short-notes" || h == "references" else h == hub
        var matches = index.items.filter { it.scopePath in accepted && hubMatches(it.hub) }
        if (matches.isEmpty() && ece != null) {
            val slug = ece.groupValues[2].lowercase()
            matches = index.items.filter { it.scopePath.lowercase().contains(slug) && hubMatches(it.hub) }
        }
        return matches
            .sortedWith(compareBy<CatalogItem>({ it.chapter ?: Int.MAX_VALUE }, { it.title.orEmpty() }))
            .mapIndexed { i, c ->
                LearningResource(
                    id = c.id,
                    packageId = c.packageId,
                    scopePath = c.scopePath,
                    hub = if (c.hub == "references") "short-notes" else c.hub,
                    title = c.title.orEmpty(),
                    chapter = c.chapter,
                    sortOrder = i,
                    contentType = c.contentType.orEmpty(),
                    storagePath = null,
                    bodyMd = null,
                    metaJson = "{}",
                    itemCount = c.itemCount
                )
            }
    }

    /**
     * Loads one resource with its full body and meta (note text, flashcards, questions).
     * Offline: a saved copy opens immediately when Android has no validated network. Online: the checked server door is
     * consulted before a saved copy is shown, so current locks, entitlement/device changes and version gates take effect
     * on this open. A refused material gives [ContentItemResult.Failure] and its saved copy is deleted.
     * With [refresh] = true the server is asked first; a network IOException may still fall back to an existing copy.
     */
    suspend fun getResource(context: Context, id: String, refresh: Boolean = false): ContentItemResult =
        withContext(Dispatchers.IO) {
            if (!UUID_REGEX.matches(id)) return@withContext ContentItemResult.NotFound
            val cacheFile = cacheFile(context, "item_$id")
            if (!refresh) {
                val saved = readCache(cacheFile)?.let { runCatching { JSONObject(it) }.getOrNull() }
                if (saved != null) {
                    // Offline opens stay instant. When the phone is online, ask the server BEFORE displaying a
                    // protected cached copy so a new lock, refund, deletion or minimum-version change takes effect.
                    if (!hasValidatedNetwork(context)) {
                        return@withContext ContentItemResult.Found(rowToResource(saved), true)
                    }
                    try {
                        return@withContext when (val door = fetchItem(id, cacheFile, fastHttp)) {
                            is Door.Row -> ContentItemResult.Found(rowToResource(door.row), false)
                            is Door.Gone -> ContentItemResult.NotFound
                            is Door.Locked -> ContentItemResult.Failure(LOCKED_MESSAGE)
                            is Door.DeviceNotActive -> ContentItemResult.Failure(DEVICE_NOT_ACTIVE_MESSAGE)
                            is Door.UpdateRequired -> ContentItemResult.Failure(
                                "Please update Wisdom Tower Academy to continue opening this material (minimum version code: ${door.minimumVersionCode})."
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        return@withContext ContentItemResult.Found(rowToResource(saved), true)
                    } catch (e: Exception) {
                        Log.w(TAG, "saved item access check failed: ${e.javaClass.simpleName}")
                        return@withContext ContentItemResult.Failure(LOAD_FAILED)
                    }
                }
            }
            try {
                when (val door = fetchItem(id, cacheFile, http)) {
                    is Door.Row -> ContentItemResult.Found(rowToResource(door.row), false)
                    is Door.Gone -> ContentItemResult.NotFound
                    is Door.Locked -> ContentItemResult.Failure(LOCKED_MESSAGE)
                    is Door.DeviceNotActive -> ContentItemResult.Failure(DEVICE_NOT_ACTIVE_MESSAGE)
                    is Door.UpdateRequired -> ContentItemResult.Failure("Please update Wisdom Tower Academy to continue opening this material (minimum version code: ${door.minimumVersionCode}).")
                }
            } catch (e: CancellationException) {
                throw e
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
     * The address of a book file, or null when [storagePath] is not a file path:
     *  - `appwrite:<FILE_ID>` (free book, public bucket): the plain Appwrite address (same URL the website's
     *    `/api/content/pdf` proxy reads). Uses only the public `NEXT_PUBLIC_APPWRITE_*` values from BuildConfig.
     *  - `appwrite-paid:<FILE_ID>` (paid PDF, private bucket, security slice S5b): NOT a real address but the marker
     *    `wtpaid://<FILE_ID>` ([PAID_URL_PREFIX]); [BookFileStore.ensureFile] turns it into a real short-lived
     *    address through [paidBookUrl] when the file has to be downloaded.
     * The id may be followed by `|...`; that part is cut off.
     */
    fun appwriteViewUrl(storagePath: String?): String? {
        if (storagePath == null) return null
        if (storagePath.startsWith(PAID_PATH_PREFIX)) {
            val paidId = storagePath.removePrefix(PAID_PATH_PREFIX).substringBefore('|').trim()
            return if (APPWRITE_ID_REGEX.matches(paidId)) "$PAID_URL_PREFIX$paidId" else null
        }
        if (!storagePath.startsWith("appwrite:")) return null
        val fileId = storagePath.removePrefix("appwrite:").substringBefore('|').trim()
        if (!APPWRITE_ID_REGEX.matches(fileId)) return null
        val endpoint = BuildConfig.NEXT_PUBLIC_APPWRITE_ENDPOINT.trim().trimEnd('/')
        val project = BuildConfig.NEXT_PUBLIC_APPWRITE_PROJECT_ID.trim()
        val bucket = BuildConfig.NEXT_PUBLIC_APPWRITE_BUCKET_ID.trim()
        if (endpoint.isEmpty() || project.isEmpty() || bucket.isEmpty()) return null
        return "$endpoint/storage/buckets/$bucket/files/$fileId/view?project=$project"
    }

    /**
     * Security slice S5b: the real, short-lived (about 10 minutes) address of a PAID PDF. Asks the Supabase Edge
     * Function `book-url` with the student's own login token; the function applies the lock rows, Paid mode and
     * purchases (`open_book`) and only then makes an Appwrite file token. [resourceId] is the `learning_resources` id
     * (the same id the app already uses as the book's cache key).
     *
     * Throws [BookLockedException] when the server refuses (locked / package required / not signed in), IOException for
     * network trouble (the caller shows its "check your internet" text), IllegalStateException for anything else.
     */
    suspend fun paidBookUrl(resourceId: String): String = withContext(Dispatchers.IO) {
        if (!UUID_REGEX.matches(resourceId)) throw IllegalStateException("Bad file id")
        AuthStateManager.refreshIfNeeded()
        val token = AuthStateManager.currentUser.value?.accessToken
        if (token.isNullOrBlank()) throw BookLockedException(SIGN_IN_MESSAGE)

        var reply = callBookUrl(resourceId, token)
        if (reply.first == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
            val fresh = AuthStateManager.currentUser.value?.accessToken
            if (!fresh.isNullOrBlank()) reply = callBookUrl(resourceId, fresh)
        }
        val answer = runCatching { JSONObject(reply.second) }.getOrNull()
        val status = answer?.optString("status").orEmpty()
        when {
            reply.first in 200..299 && status == "ok" -> {
                val url = answer?.optString("url").orEmpty()
                if (url.startsWith("https://")) url else throw IllegalStateException("Bad file address")
            }
            reply.first == 401 -> throw BookLockedException(SIGN_IN_MESSAGE)
            status == "device_not_active" -> throw BookLockedException(DEVICE_NOT_ACTIVE_MESSAGE)
            reply.first == 403 || status == "locked" -> throw BookLockedException(LOCKED_MESSAGE)
            else -> {
                Log.w(TAG, "book-url failed: HTTP ${reply.first}")
                throw IllegalStateException("HTTP ${reply.first}")
            }
        }
    }

    sealed class PaidBookAccessCheck {
        object Allowed : PaidBookAccessCheck()
        object Unavailable : PaidBookAccessCheck()
        data class Denied(val message: String) : PaidBookAccessCheck()
    }

    /**
     * Re-checks a saved paid PDF against current server entitlement before opening it while online.
     * A locked/deleted/device-revoked item is denied and its app-managed copy is deleted by BookFileStore.
     * Offline copies remain usable only when Android has no validated network. When online, a signed-in account
     * and a successful server check are required; an unavailable check must not silently bypass revocation.
     */
    suspend fun checkPaidBookAccess(context: Context, resourceId: String): PaidBookAccessCheck = withContext(Dispatchers.IO) {
        if (!UUID_REGEX.matches(resourceId)) return@withContext PaidBookAccessCheck.Denied("This material is no longer available.")
        if (!hasValidatedNetwork(context)) return@withContext PaidBookAccessCheck.Unavailable
        try {
            AuthStateManager.refreshIfNeeded()
            var token = AuthStateManager.currentUser.value?.accessToken
            if (token.isNullOrBlank()) return@withContext PaidBookAccessCheck.Denied(SIGN_IN_MESSAGE)

            var reply = callBookUrl(resourceId, token, fastAccessHttp, checkOnly = true)
            if (reply.first == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
                token = AuthStateManager.currentUser.value?.accessToken
                if (!token.isNullOrBlank()) reply = callBookUrl(resourceId, token, fastAccessHttp, checkOnly = true)
            }
            val answer = runCatching { JSONObject(reply.second) }.getOrNull()
            val status = answer?.optString("status").orEmpty()
            when {
                reply.first in 200..299 && status == "ok" -> PaidBookAccessCheck.Allowed
                status == "locked" -> PaidBookAccessCheck.Denied(LOCKED_MESSAGE)
                status == "device_not_active" -> PaidBookAccessCheck.Denied(DEVICE_NOT_ACTIVE_MESSAGE)
                status == "update_required" -> PaidBookAccessCheck.Denied(
                    "Please update Wisdom Tower Academy to continue opening this material."
                )
                status == "not_found" || status == "not_paid_file" || reply.first == 404 ->
                    PaidBookAccessCheck.Denied("This material is no longer available.")
                else -> PaidBookAccessCheck.Denied(
                    "We couldn't verify access to this saved material. Check your connection and try again."
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            PaidBookAccessCheck.Denied(
                "We couldn't verify access to this saved material. Check your connection and try again."
            )
        }
    }

    /** One POST to the Edge Function `book-url`. Returns the HTTP code and the body text. */
    private fun callBookUrl(
        resourceId: String,
        token: String,
        client: OkHttpClient = http,
        checkOnly: Boolean = false
    ): Pair<Int, String> {
        val body = JSONObject()
            .put("id", resourceId)
            .put("client_version", BuildConfig.VERSION_CODE)
            .put("check_only", checkOnly)
            .toString().toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$supabaseUrl/functions/v1/book-url")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()
        return client.newCall(request).execute().use { resp ->
            resp.code to resp.body?.string().orEmpty()
        }
    }

    /**
     * P1b (2026-11-03): the saved item counts of one course, read from the phone only (NO network, never waits). Same
     * file [getHubCounts] writes after every successful read (`counts_<scopePath>`). Returns null when no counts were
     * ever saved for this course or the file cannot be read. An empty saved map is returned as null too (an old empty
     * answer must not hide the real numbers). The course page draws these at once and then calls [getHubCounts].
     */
    suspend fun getHubCountsSaved(context: Context, scopePath: String): Map<String, Int>? =
        withContext(Dispatchers.IO) {
            val raw = readCache(cacheFile(context, "counts_$scopePath")) ?: return@withContext null
            val map = runCatching {
                val json = JSONObject(raw)
                val out = mutableMapOf<String, Int>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    out[k] = json.optInt(k, 0)
                }
                out.toMap()
            }.getOrNull()
            if (map.isNullOrEmpty()) null else map
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
     * Asks the checked door ([askDoor]) for one item and keeps [cacheFile] in step: the row is saved when the
     * door opens, the saved copy is deleted when the item is gone (no longer published) or locked.
     */
    private suspend fun fetchItem(id: String, cacheFile: File, client: OkHttpClient): Door {
        val door = askDoor(id, client)
        when (door) {
            is Door.Row -> writeCache(cacheFile, door.row.toString())
            else -> runCatching { cacheFile.delete() }
        }
        return door
    }

    /**
     * Security slice S1: `rpc/open_resource` with the student's own token. The server answers
     * `{status: ok|locked|not_found|not_signed_in, row?}`. Without a token, or on an unexpected answer or HTTP error,
     * the old direct read is used while [LEGACY_DIRECT_READ] is true; afterwards such a case is refused.
     * Network trouble throws IOException (the caller falls back to the saved copy, as before).
     */
    private suspend fun askDoor(id: String, client: OkHttpClient): Door {
        AuthStateManager.refreshIfNeeded()
        val token = AuthStateManager.currentUser.value?.accessToken
        if (token.isNullOrBlank()) return legacyOrRefuse(id, client)

        var reply = callOpenResourceV2(id, token, client)
        if (reply.first == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
            val fresh = AuthStateManager.currentUser.value?.accessToken
            if (!fresh.isNullOrBlank()) reply = callOpenResourceV2(id, fresh, client)
        }
        if (reply.first !in 200..299) {
            Log.w(TAG, "open_resource failed: HTTP ${reply.first}")
            return legacyOrRefuse(id, client)
        }
        val answer = try {
            JSONObject(reply.second)
        } catch (e: Exception) {
            return legacyOrRefuse(id, client)
        }
        return when (answer.optString("status")) {
            "ok" -> answer.optJSONObject("row")?.let { Door.Row(it) } ?: Door.Gone
            "not_found" -> Door.Gone
            "locked" -> Door.Locked
            "device_not_active" -> Door.DeviceNotActive
            "update_required" -> Door.UpdateRequired(answer.optInt("min_version_code", 0))
            else -> legacyOrRefuse(id, client)
        }
    }

    /** Version-aware checked door used by replacement builds. Legacy clients keep calling open_resource(uuid). */
    private fun callOpenResourceV2(id: String, token: String, client: OkHttpClient): Pair<Int, String> {
        val body = JSONObject()
            .put("p_id", id)
            .put("p_client_version", BuildConfig.VERSION_CODE)
            .toString().toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/open_resource_v2")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()
        return client.newCall(request).execute().use { resp ->
            resp.code to resp.body?.string().orEmpty()
        }
    }

    /** The old direct read (public key), only while [LEGACY_DIRECT_READ] is true; otherwise the item is refused. */
    private fun legacyOrRefuse(id: String, client: OkHttpClient): Door {
        if (!LEGACY_DIRECT_READ) return Door.Locked
        val url = "$supabaseUrl/rest/v1/learning_resources".toHttpUrl().newBuilder()
            .addQueryParameter("select", "*")
            .addQueryParameter("id", "eq.$id")
            .addQueryParameter("published", "eq.true")
            .addQueryParameter("limit", "1")
            .build().toString()
        val rows = JSONArray(get(url, client))
        return if (rows.length() == 0) Door.Gone else Door.Row(rows.getJSONObject(0))
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
            itemCount = if (row.isNull("item_count")) null else row.optInt("item_count"),
            sizeBytes = if (row.isNull("size_bytes")) null else row.optLong("size_bytes").takeIf { it > 0L },
            description = row.nstr("description")
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
