package com.wisdomtower.academy.data.games

import android.content.Context
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One finished game, ready to be saved. [stars] 0..3, [score] 0..100000 (the server clamps both again). */
data class GameResultInput(
    val game: String,
    val scopePath: String,
    val chapter: Int,
    val level: GameLevel,
    val stars: Int,
    val score: Int,
    val won: Boolean
)

enum class GameSaveStatus {
    /** The server has it. */
    Saved,
    /** No internet (or server busy): kept on the phone, sent next time a game screen opens. */
    Pending,
    /** Signed out: nothing is saved (no guest rows). */
    NotSignedIn,
    /** The server refused it for good; it is not retried. */
    Failed
}

/** [newBest] is true when this result beats the best the student already had on this phone for the same chapter and level. */
data class GameSaveOutcome(val status: GameSaveStatus, val newBest: Boolean)

/** Best of one game + chapter + level, kept on the phone (see [GameResultsRepository.localBest]). */
data class GameLocalBest(val stars: Int, val score: Int, val plays: Int)

/**
 * Saves game results (docs/GAMES_COOL_PLAN.md slice R2) through `public.save_game_result` (applied in R1).
 * Same pattern as `StudyDayLogReader`: own token, refresh when needed, one retry after a 401, only HTTP codes logged,
 * never throws. A signed-out student saves nothing. If the internet fails, the result waits in a small list on the phone
 * (SharedPreferences, max 50, tagged with the student's id so it is never sent under another account) and is sent by
 * [flushPending].
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object GameResultsRepository {
    private const val TAG = "GameResults"
    private const val PREFS = "wta_game_results"
    private const val KEY_PENDING = "pending"
    private const val MAX_PENDING = 50

    private val lock = Mutex()

    private enum class Post { Ok, Unauthorized, Retry, Rejected }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    /** Saves one result. Also tries to send older waiting results first. */
    suspend fun save(context: Context, result: GameResultInput): GameSaveOutcome = withContext(Dispatchers.IO) {
        try {
            AuthStateManager.refreshIfNeeded()
            val user = AuthStateManager.currentUser.value
            val token = user?.accessToken
            if (user == null || token.isNullOrBlank() || !UUID_REGEX.matches(user.id)) {
                return@withContext GameSaveOutcome(GameSaveStatus.NotSignedIn, false)
            }
            val userId = user.id
            val newBest = lock.withLock { recordLocal(context, userId, result) }
            flushPending(context)
            val status = when (sendOne(result)) {
                Post.Ok -> GameSaveStatus.Saved
                Post.Rejected -> GameSaveStatus.Failed
                else -> {
                    enqueue(context, userId, result)
                    GameSaveStatus.Pending
                }
            }
            GameSaveOutcome(status, newBest)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "save failed: ${e.javaClass.simpleName}")
            GameSaveOutcome(GameSaveStatus.Failed, false)
        }
    }

    /** Sends the waiting results of the signed-in student. Stops at the first one that cannot be sent. Never throws. */
    suspend fun flushPending(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                AuthStateManager.refreshIfNeeded()
                val user = AuthStateManager.currentUser.value
                val token = user?.accessToken
                if (user == null || token.isNullOrBlank() || !UUID_REGEX.matches(user.id)) return@withContext
                val userId = user.id
                val mine: List<Pair<Long, GameResultInput>> = lock.withLock { readPending(context, userId) }
                if (mine.isEmpty()) return@withContext
                val done = HashSet<Long>()
                for ((id, item) in mine) {
                    val out = sendOne(item)
                    if (out == Post.Ok || out == Post.Rejected) done.add(id) else break
                }
                if (done.isNotEmpty()) lock.withLock { removePending(context, done) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "flush failed: ${e.javaClass.simpleName}")
            }
        }
    }

    /** The best of this game + chapter + level kept on this phone for the signed-in student, or null. */
    fun localBest(context: Context, game: String, scopePath: String, chapter: Int, level: GameLevel): GameLocalBest? {
        val userId = AuthStateManager.currentUser.value?.id ?: return null
        val raw = try {
            prefs(context).getString(bestKey(userId, game, scopePath, chapter, level), null)
        } catch (e: Exception) {
            null
        } ?: return null
        val p = raw.split(",")
        return GameLocalBest(p.getOrNull(0)?.toIntOrNull() ?: 0, p.getOrNull(1)?.toIntOrNull() ?: 0, p.getOrNull(2)?.toIntOrNull() ?: 0)
    }

    // ---------- network ----------

    private suspend fun sendOne(r: GameResultInput): Post {
        val token = AuthStateManager.currentUser.value?.accessToken
        if (token.isNullOrBlank()) return Post.Retry
        var out = post(token, r)
        if (out == Post.Unauthorized && AuthStateManager.refreshIfNeeded(force = true)) {
            val token2 = AuthStateManager.currentUser.value?.accessToken
            if (!token2.isNullOrBlank()) out = post(token2, r)
        }
        return if (out == Post.Unauthorized) Post.Retry else out
    }

    private fun post(token: String, r: GameResultInput): Post {
        val body = JSONObject()
            .put("p_game", r.game)
            .put("p_scope_path", r.scopePath)
            .put("p_chapter", r.chapter)
            .put("p_level", r.level.id)
            .put("p_stars", r.stars)
            .put("p_score", r.score)
            .put("p_won", r.won)
            .toString()
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/save_game_result")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            http.newCall(request).execute().use { resp ->
                when {
                    resp.isSuccessful -> Post.Ok
                    resp.code == 401 -> Post.Unauthorized
                    resp.code == 408 || resp.code == 429 || resp.code >= 500 -> Post.Retry
                    else -> {
                        Log.w(TAG, "save rejected: HTTP ${resp.code}")
                        Post.Rejected
                    }
                }
            }
        } catch (e: IOException) {
            Post.Retry
        }
    }

    // ---------- phone-side copy ----------

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun bestKey(userId: String, game: String, scopePath: String, chapter: Int, level: GameLevel): String =
        "best_${userId}_${game}_${scopePath}_${chapter}_${level.id}"

    /** Updates the phone-side best; true when the result beats an earlier best (a first play is not a "new best"). */
    private fun recordLocal(context: Context, userId: String, r: GameResultInput): Boolean {
        return try {
            val sp = prefs(context)
            val key = bestKey(userId, r.game, r.scopePath, r.chapter, r.level)
            val old = sp.getString(key, null)
            val p = old?.split(",")
            val oldStars = p?.getOrNull(0)?.toIntOrNull() ?: 0
            val oldScore = p?.getOrNull(1)?.toIntOrNull() ?: 0
            val oldPlays = p?.getOrNull(2)?.toIntOrNull() ?: 0
            val newBest = old != null && (r.stars > oldStars || (r.stars == oldStars && r.score > oldScore))
            val stars = maxOf(oldStars, r.stars.coerceIn(0, 3))
            val score = maxOf(oldScore, r.score.coerceIn(0, 100000))
            sp.edit().putString(key, "$stars,$score,${oldPlays + 1}").apply()
            newBest
        } catch (e: Exception) {
            false
        }
    }

    private fun enqueue(context: Context, userId: String, r: GameResultInput) {
        try {
            val arr = readPendingArray(context)
            val o = JSONObject()
                .put("id", System.currentTimeMillis() * 1000 + (arr.length() % 1000))
                .put("user", userId)
                .put("game", r.game)
                .put("scope", r.scopePath)
                .put("chapter", r.chapter)
                .put("level", r.level.id)
                .put("stars", r.stars)
                .put("score", r.score)
                .put("won", r.won)
            arr.put(o)
            val out = JSONArray()
            val start = maxOf(0, arr.length() - MAX_PENDING)
            for (i in start until arr.length()) out.put(arr.get(i))
            prefs(context).edit().putString(KEY_PENDING, out.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "enqueue failed: ${e.javaClass.simpleName}")
        }
    }

    private fun readPendingArray(context: Context): JSONArray = try {
        JSONArray(prefs(context).getString(KEY_PENDING, null) ?: "[]")
    } catch (e: Exception) {
        JSONArray()
    }

    private fun readPending(context: Context, userId: String): List<Pair<Long, GameResultInput>> {
        val arr = readPendingArray(context)
        val out = ArrayList<Pair<Long, GameResultInput>>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("user") != userId) continue
            out.add(
                o.optLong("id") to GameResultInput(
                    game = o.optString("game"),
                    scopePath = o.optString("scope"),
                    chapter = o.optInt("chapter"),
                    level = GameLevel.fromId(o.optString("level")),
                    stars = o.optInt("stars"),
                    score = o.optInt("score"),
                    won = o.optBoolean("won")
                )
            )
        }
        return out
    }

    private fun removePending(context: Context, ids: Set<Long>) {
        try {
            val arr = readPendingArray(context)
            val out = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optLong("id") !in ids) out.put(o)
            }
            prefs(context).edit().putString(KEY_PENDING, out.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "remove pending failed: ${e.javaClass.simpleName}")
        }
    }
}
