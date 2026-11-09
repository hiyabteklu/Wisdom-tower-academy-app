package com.wisdomtower.academy.data.games

import android.content.Context
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.TreeMap
import java.util.concurrent.TimeUnit

/** One chapter of a subject that has at least one published game set, and which games it has. */
data class GameSetChapter(
    val chapter: Int,
    val title: String,
    val hasDefense: Boolean,
    val hasMatch: Boolean
)

/** Wisdom Defense item: a short question and exactly 4 choices. `correct` is a 0-based index. */
data class GameSetDefenseItem(
    val question: String,
    val choices: List<String>,
    val correct: Int,
    val hint: String?
)

/** Match item: a term and its meaning. */
data class GameSetMatchPair(val a: String, val b: String)

/** One published game set (one chapter, one game). Only the list that fits [game] is filled. */
data class GameSetData(
    val id: String,
    val game: String,
    val chapter: Int,
    val chapterTitle: String,
    val title: String,
    val defenseItems: List<GameSetDefenseItem>,
    val matchPairs: List<GameSetMatchPair>
)

sealed class GameSetsResult<out T> {
    data class Success<T>(val value: T, val fromCache: Boolean) : GameSetsResult<T>()
    data class Failure(val message: String) : GameSetsResult<Nothing>()
}

/**
 * Reads the dedicated game question sets from `public.game_sets` (docs/GAMES_HUB_PLAN.md, step 2a).
 * Nothing here is invented: a chapter without a set simply has no entry, and an item that does not match the
 * documented shape is skipped.
 *
 * 2026-11-09 (docs/PLAN_2026-11-09_GAMES_OWNERSHIP.md, step B2): the app no longer reads the table directly. It calls
 * two database functions (they return the same row shapes as before, so parsing and the saved copies are unchanged):
 *  - `game_set_chapters(p_scope_paths)`: chapter, chapter_title, game. No `items`. Public key is enough.
 *  - `get_game_set(p_scope_paths, p_chapter, p_game)`: one set WITH `items`, only when the caller may see the chapter
 *    (`content_guard.resource_visible`, hub `games`, lock key `chapter:<scope>:games:<n>`, then scope / package).
 *    The student's own token is sent when signed in, so an owner of the package gets chapters 3 and up; a locked
 *    chapter returns an empty list (parsed as "no usable set"). Nothing locked is ever saved on the phone.
 * Once every student runs a build with this code, the open read rule on the table can be closed (database step B2b).
 *
 * Item shapes (owner's JSON, pasted in bulk through the admin dashboard):
 *  - defense: [{"q":"...","choices":["a","b","c","d"],"correct":0,"hint":"..."}]
 *  - match:   [{"a":"term","b":"meaning"}]
 *
 * `scopePath` has the SAME format as `learning_resources.scope_path` (including the ECE quirk of
 * docs/CONTENT_SHAPES.md). Chapter lists never download `items`; a set is loaded only when a chapter is opened.
 * Every good answer is saved in the app's private storage; offline the saved copy is used (`fromCache = true`).
 *
 * 2026-10-22 (owner order, game cards and downloads): [isDownloaded], [download] and [removeDownload] let the chapter
 * list show the same download icon as the learning lists. A "downloaded" chapter is simply a good saved copy of its set
 * (the same file [loadSet] already writes and reads), so playing a chapter online also saves it.
 *
 * STATUS: [~] written, NOT compiled, NOT tested.
 */
object GameSetsRepository {
    const val GAME_DEFENSE = "defense"
    const val GAME_MATCH = "match"

    private const val TAG = "GameSets"
    private const val NO_INTERNET = "Check your internet connection and try again."
    private const val LOAD_FAILED = "Unable to load games right now. Please try again."

    private val ECE_SCOPE = Regex("^ece/(sem-[12])/([^/]+)$")

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Used when a saved copy exists, so a slow network does not keep the student waiting. */
    private val fastHttp = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /** Chapters (with the games each one has) of one subject, from published `game_sets` rows. No `items` are loaded. */
    suspend fun chapters(context: Context, scopePath: String): GameSetsResult<List<GameSetChapter>> =
        withContext(Dispatchers.IO) {
            val cacheFile = cacheFile(context, "chapters_$scopePath")
            try {
                val client = if (cacheFile.exists()) fastHttp else http
                val text = rpc("game_set_chapters", chaptersBody(scopePath), client, withLogin = false)
                val rows = JSONArray(text)
                writeCache(cacheFile, text)
                GameSetsResult.Success(parseChapters(rows), false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "chapters failed: ${e.javaClass.simpleName}")
                val cached = readCache(cacheFile)?.let { runCatching { JSONArray(it) }.getOrNull() }
                if (cached != null) {
                    GameSetsResult.Success(parseChapters(cached), true)
                } else {
                    GameSetsResult.Failure(if (e is IOException) NO_INTERNET else LOAD_FAILED)
                }
            }
        }

    /**
     * The published set of one chapter for one game ([GAME_DEFENSE] or [GAME_MATCH]).
     * Success(null) means there is no usable set (or the chapter is locked for this student): the screen shows
     * "coming soon" for that game only. The screen checks the lock first (`GameAccess`), this is the server side.
     */
    suspend fun loadSet(
        context: Context,
        scopePath: String,
        chapter: Int,
        game: String
    ): GameSetsResult<GameSetData?> = withContext(Dispatchers.IO) {
        if (game != GAME_DEFENSE && game != GAME_MATCH) return@withContext GameSetsResult.Success(null, false)
        val cacheFile = setCacheFile(context, scopePath, chapter, game)
        try {
            val client = if (cacheFile.exists()) fastHttp else http
            val text = rpc("get_game_set", setBody(scopePath, chapter, game), client, withLogin = true)
            val rows = JSONArray(text)
            val set = parseSet(rows, game)
            // Only a usable set is saved; an empty answer (locked chapter) must not overwrite or create a copy.
            if (set != null) writeCache(cacheFile, text)
            GameSetsResult.Success(set, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "loadSet failed: ${e.javaClass.simpleName}")
            val cached = readCache(cacheFile)?.let { runCatching { JSONArray(it) }.getOrNull() }
            if (cached != null) {
                GameSetsResult.Success(parseSet(cached, game), true)
            } else {
                GameSetsResult.Failure(if (e is IOException) NO_INTERNET else LOAD_FAILED)
            }
        }
    }

    // ---------- offline copy (download icon) ----------

    /** True when a usable saved copy of this chapter's set for [game] is on the phone. */
    suspend fun isDownloaded(context: Context, scopePath: String, chapter: Int, game: String): Boolean =
        withContext(Dispatchers.IO) {
            val text = readCache(setCacheFile(context, scopePath, chapter, game)) ?: return@withContext false
            runCatching { parseSet(JSONArray(text), game) != null }.getOrDefault(false)
        }

    /** Fetches the set from the server and saves it. Returns true only when a usable set was saved (never a locked one). */
    suspend fun download(context: Context, scopePath: String, chapter: Int, game: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val text = rpc("get_game_set", setBody(scopePath, chapter, game), http, withLogin = true)
                if (parseSet(JSONArray(text), game) == null) {
                    false
                } else {
                    writeCache(setCacheFile(context, scopePath, chapter, game), text)
                    true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "download failed: ${e.javaClass.simpleName}")
                false
            }
        }

    /** Deletes the saved copy of this chapter's set. */
    suspend fun removeDownload(context: Context, scopePath: String, chapter: Int, game: String) {
        withContext(Dispatchers.IO) {
            runCatching { setCacheFile(context, scopePath, chapter, game).delete() }
        }
    }

    // ---------- database function calls ----------

    /** `scope_path` values to match, with the ECE and freshman-math quirks of `LearningContentRepository`. */
    private fun scopePaths(scopePath: String): List<String> {
        val ece = ECE_SCOPE.matchEntire(scopePath)
        return when {
            ece != null -> {
                val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
                listOf(scopePath, "ece/$other/${ece.groupValues[2]}")
            }
            scopePath == "freshman/math-natural" || scopePath == "freshman/mathematics" ->
                listOf("freshman/math-natural", "freshman/mathematics")
            else -> listOf(scopePath)
        }
    }

    private fun chaptersBody(scopePath: String): JSONObject =
        JSONObject().put("p_scope_paths", JSONArray(scopePaths(scopePath)))

    private fun setBody(scopePath: String, chapter: Int, game: String): JSONObject =
        JSONObject()
            .put("p_scope_paths", JSONArray(scopePaths(scopePath)))
            .put("p_chapter", chapter)
            .put("p_game", game)

    /**
     * POST `rpc/<name>`. With [withLogin] the signed-in student's own token is sent (refreshed first; one retry after a
     * 401), otherwise, or when signed out, the public key. Network trouble throws IOException; any other HTTP error
     * throws IllegalStateException.
     */
    private suspend fun rpc(name: String, body: JSONObject, client: OkHttpClient, withLogin: Boolean): String {
        var token: String? = null
        if (withLogin) {
            AuthStateManager.refreshIfNeeded()
            token = AuthStateManager.currentUser.value?.accessToken?.takeIf { it.isNotBlank() }
        }
        var reply = postRpc(name, body, token ?: anonKey, client)
        if (reply.first == 401 && token != null && AuthStateManager.refreshIfNeeded(force = true)) {
            val fresh = AuthStateManager.currentUser.value?.accessToken
            if (!fresh.isNullOrBlank()) reply = postRpc(name, body, fresh, client)
        }
        if (reply.first !in 200..299) throw IllegalStateException("HTTP ${reply.first}")
        return reply.second
    }

    private fun postRpc(name: String, body: JSONObject, bearer: String, client: OkHttpClient): Pair<Int, String> {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/$name")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $bearer")
            .addHeader("Accept", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        client.newCall(request).execute().use { resp ->
            return Pair(resp.code, resp.body?.string().orEmpty())
        }
    }

    // ---------- parsing ----------

    private class ChapterAcc(var title: String, var defense: Boolean, var match: Boolean)

    private fun parseChapters(rows: JSONArray): List<GameSetChapter> {
        val acc = TreeMap<Int, ChapterAcc>()
        for (row in rows.objects()) {
            if (row.isNull("chapter")) continue
            val entry = acc.getOrPut(row.optInt("chapter")) { ChapterAcc("", false, false) }
            val title = row.nstr("chapter_title")
            if (entry.title.isEmpty() && title != null) entry.title = title
            when (row.optString("game")) {
                GAME_DEFENSE -> entry.defense = true
                GAME_MATCH -> entry.match = true
            }
        }
        return acc.map { (chapter, e) -> GameSetChapter(chapter, e.title, e.defense, e.match) }
    }

    private fun parseSet(rows: JSONArray, game: String): GameSetData? {
        val row = rows.objects().firstOrNull() ?: return null
        val items = row.optJSONArray("items") ?: return null
        val defense = ArrayList<GameSetDefenseItem>()
        val pairs = ArrayList<GameSetMatchPair>()
        for (item in items.objects()) {
            if (game == GAME_DEFENSE) {
                val q = item.nstr("q") ?: continue
                val choicesJson = item.optJSONArray("choices") ?: continue
                val choices = (0 until choicesJson.length()).map { choicesJson.optString(it).trim() }
                val correct = item.optInt("correct", -1)
                if (choices.size != 4 || choices.any { it.isEmpty() } || correct !in 0..3) continue
                defense.add(GameSetDefenseItem(q, choices, correct, item.nstr("hint")))
            } else {
                val a = item.nstr("a") ?: continue
                val b = item.nstr("b") ?: continue
                pairs.add(GameSetMatchPair(a, b))
            }
        }
        if (defense.isEmpty() && pairs.isEmpty()) return null
        return GameSetData(
            id = row.optString("id"),
            game = game,
            chapter = row.optInt("chapter"),
            chapterTitle = row.nstr("chapter_title") ?: "",
            title = row.nstr("title") ?: "",
            defenseItems = defense,
            matchPairs = pairs
        )
    }

    private fun JSONObject.nstr(key: String): String? {
        if (isNull(key)) return null
        val v = optString(key).trim()
        return if (v == "null") null else v.ifBlank { null }
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }

    // ---------- small file cache (app private storage, no secrets) ----------

    private fun setCacheFile(context: Context, scopePath: String, chapter: Int, game: String): File =
        cacheFile(context, "set_${scopePath}_${chapter}_$game")

    private fun cacheFile(context: Context, key: String): File {
        val dir = File(context.filesDir, "game_sets_cache").apply { mkdirs() }
        return File(dir, key.replace(Regex("[^A-Za-z0-9._-]"), "_").take(150) + ".json")
    }

    private fun writeCache(file: File, text: String) {
        runCatching { file.writeText(text) }
    }

    private fun readCache(file: File): String? =
        runCatching { if (file.exists()) file.readText() else null }.getOrNull()
}
