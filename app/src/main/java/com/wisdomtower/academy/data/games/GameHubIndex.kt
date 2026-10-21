package com.wisdomtower.academy.data.games

import android.content.Context
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.admin.ADMIN_CONTENT_TREE
import com.wisdomtower.academy.data.admin.ADMIN_HUB_IDS
import com.wisdomtower.academy.data.admin.AdminNavNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One subject (course) of a package in the game hub. [scopePath] is the same text as `learning_resources.scope_path`. */
class GameHubSubject(val name: String, val scopePath: String)

/**
 * Subject lists for the game hub (docs/GAMES_HUB_PLAN.md, step 2b).
 *
 *  - [knownSubjects]: the subjects of a package from the admin content tree (the same ids and scope paths the
 *    admin locks and the hubs use, e.g. `grade/9/chemistry`, `freshman/physics`, `ece/sem-1/eceg3051`).
 *  - [packageSubjects]: for packages that are NOT in that tree (UAT, GAT, COC, Exit Exam, Remedial ...): the subjects
 *    that really have a published game set, read from `game_sets` by `package_id`. Nothing is invented: the name is
 *    the last part of the scope path made readable.
 *
 * STATUS: written, NOT compiled, NOT tested (plan step 2b).
 */
object GameHubIndex {
    private const val TAG = "GameHubIndex"
    private const val NO_INTERNET = "Check your internet connection and try again."
    private const val LOAD_FAILED = "Unable to load games right now. Please try again."
    private const val ENTRANCE_PACKAGE_ID = "entrance-questions"

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val fastHttp = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /** Subjects of [packageId] taken from the admin content tree; empty when the package is not in the tree. */
    fun knownSubjects(packageId: String): List<GameHubSubject> {
        if (packageId == ENTRANCE_PACKAGE_ID) return emptyList()
        val out = ArrayList<GameHubSubject>()
        fun walk(node: AdminNavNode) {
            val scope = node.scopePath
            if (node.packageId == packageId && scope != null && node.id !in ADMIN_HUB_IDS) {
                out.add(GameHubSubject(node.label, scope))
                return
            }
            node.children.forEach { walk(it) }
        }
        ADMIN_CONTENT_TREE.forEach { walk(it) }
        return out
    }

    /** Subjects of [packageId] that have at least one published game set (for packages outside the admin tree). */
    suspend fun packageSubjects(context: Context, packageId: String): GameSetsResult<List<GameHubSubject>> =
        withContext(Dispatchers.IO) {
            val cacheFile = cacheFile(context, "subjects_$packageId")
            try {
                val url = "$supabaseUrl/rest/v1/game_sets".toHttpUrl().newBuilder()
                    .addQueryParameter("select", "scope_path")
                    .addQueryParameter("package_id", "eq.$packageId")
                    .addQueryParameter("published", "eq.true")
                    .build().toString()
                val client = if (cacheFile.exists()) fastHttp else http
                val text = get(url, client)
                val rows = JSONArray(text)
                runCatching { cacheFile.writeText(text) }
                GameSetsResult.Success(parseSubjects(rows), false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "packageSubjects failed: ${e.javaClass.simpleName}")
                val cached = runCatching { if (cacheFile.exists()) JSONArray(cacheFile.readText()) else null }.getOrNull()
                if (cached != null) {
                    GameSetsResult.Success(parseSubjects(cached), true)
                } else {
                    GameSetsResult.Failure(if (e is IOException) NO_INTERNET else LOAD_FAILED)
                }
            }
        }

    private fun parseSubjects(rows: JSONArray): List<GameHubSubject> {
        val scopes = LinkedHashSet<String>()
        for (i in 0 until rows.length()) {
            val scope = rows.optJSONObject(i)?.optString("scope_path").orEmpty().trim()
            if (scope.isNotEmpty() && scope != "null") scopes.add(scope)
        }
        return scopes.sorted().map { GameHubSubject(readableName(it), it) }
    }

    /** `uat/mathematics` becomes `Mathematics`, `remedial/cpp-programming` becomes `Cpp Programming`. */
    private fun readableName(scopePath: String): String =
        scopePath.substringAfterLast('/')
            .split('-', '_')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

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

    private fun cacheFile(context: Context, key: String): File {
        val dir = File(context.filesDir, "game_sets_cache").apply { mkdirs() }
        return File(dir, key.replace(Regex("[^A-Za-z0-9._-]"), "_").take(150) + ".json")
    }
}
