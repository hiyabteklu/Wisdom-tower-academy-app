package com.wisdomtower.academy.ui.study

import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Two-way sync of the Study Planner with `public.study_plans` (one row per student: `blocks` jsonb, `version` int).
 * The student's own token is used (the table's security rules allow a student to read, insert and update only their
 * own row). Rules (owner decision 2026-10-20):
 *  - the server copy is the master; the phone keeps a copy for offline use ([NativePlannerState]);
 *  - unsent phone edits are sent with a version check (`PATCH ... where version = the one the edit is based on`), so an
 *    admin push in the meantime is never overwritten by an old phone copy: the send is refused and the server copy
 *    replaces the phone copy (an admin push overwrites the app);
 *  - no row on the server yet: the first send creates it (version 1);
 *  - offline or any network trouble: nothing is lost, the edits stay marked as unsent and are tried again at the next
 *    opening of the planner, the next edit or the next return to the app.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object PlannerSync {
    private const val TAG = "PlannerSync"

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** One sync at a time. */
    private val mutex = Mutex()

    private sealed interface PushResult {
        class Done(val version: Int) : PushResult
        object Conflict : PushResult
        object Offline : PushResult
    }

    /** Starts a sync in the background (returns at once). */
    fun syncSoon() {
        scope.launch { sync() }
    }

    /** Sends unsent edits, then takes a newer server copy. Never throws (network errors are logged and retried later). */
    suspend fun sync() {
        var again = false
        mutex.withLock {
            try {
                again = syncLocked()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "planner sync failed: ${e.javaClass.simpleName}")
            }
        }
        if (again) syncSoon()
    }

    /** Returns true when the student edited again while the send was running (another round is needed). */
    private suspend fun syncLocked(): Boolean {
        val uid = AuthStateManager.currentUser.value?.id ?: return false
        NativePlannerState.reconcileOwner(uid)
        val snap = NativePlannerState.snapshot()
        var force = false
        if (snap.dirty) {
            when (val r = push(uid, snap)) {
                is PushResult.Done -> {
                    NativePlannerState.markPushed(snap.blocks, r.version)
                    return NativePlannerState.snapshot().dirty
                }
                PushResult.Conflict -> force = true
                PushResult.Offline -> return false
            }
        }
        pull(uid, snap, force)
        return false
    }

    private suspend fun push(uid: String, snap: PlannerSnapshot): PushResult {
        return try {
            val arr = NativePlannerState.blocksToJson(snap.blocks)
            if (snap.version <= 0) {
                val body = JSONObject().put("user_id", uid).put("blocks", arr).put("version", 1).toString()
                val reply = send { token -> baseRequest(token).url(tableUrl()).post(body.toRequestBody(JSON_MEDIA)).build() }
                    ?: return PushResult.Offline
                when {
                    reply.first in 200..299 -> PushResult.Done(1)
                    reply.first == 409 -> PushResult.Conflict // a row already exists: the server copy wins
                    else -> PushResult.Offline
                }
            } else {
                val next = snap.version + 1
                val body = JSONObject().put("blocks", arr).put("version", next).put("updated_at", nowIso()).toString()
                val url = tableUrl().toHttpUrl().newBuilder()
                    .addQueryParameter("user_id", "eq.$uid")
                    .addQueryParameter("version", "eq.${snap.version}")
                    .build()
                val reply = send { token -> baseRequest(token).url(url).patch(body.toRequestBody(JSON_MEDIA)).build() }
                    ?: return PushResult.Offline
                if (reply.first !in 200..299) return PushResult.Offline
                // `return=representation`: the updated rows. None = the version no longer matches = the server changed.
                if (JSONArray(reply.second).length() > 0) PushResult.Done(next) else PushResult.Conflict
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "planner send failed: ${e.javaClass.simpleName}")
            PushResult.Offline
        }
    }

    private suspend fun pull(uid: String, snap: PlannerSnapshot, force: Boolean) {
        val url = tableUrl().toHttpUrl().newBuilder()
            .addQueryParameter("select", "blocks,version")
            .addQueryParameter("user_id", "eq.$uid")
            .build()
        val reply = send { token -> baseRequest(token).url(url).get().build() } ?: return
        if (reply.first !in 200..299) return
        val rows = JSONArray(reply.second)
        if (rows.length() == 0) {
            // No row: nothing to take, unless a row we knew was removed on the server.
            if (snap.version > 0 || force) applyOnMain(emptyList(), 0, force)
            return
        }
        val row = rows.getJSONObject(0)
        val version = row.optInt("version", 1)
        if (!force && version == snap.version) return
        val blocks = NativePlannerState.parseBlocks(row.optJSONArray("blocks") ?: JSONArray())
        applyOnMain(blocks, version, force)
    }

    private suspend fun applyOnMain(blocks: List<StudyBlock>, version: Int, force: Boolean) {
        withContext(Dispatchers.Main) { NativePlannerState.applyServer(blocks, version, force) }
    }

    // ---------- HTTP ----------

    private fun tableUrl(): String = "$supabaseUrl/rest/v1/study_plans"

    private fun baseRequest(token: String): Request.Builder = Request.Builder()
        .addHeader("apikey", anonKey)
        .addHeader("Authorization", "Bearer $token")
        .addHeader("Accept", "application/json")
        .addHeader("Prefer", "return=representation")

    /** Runs a request with the student's OWN token; one retry after a refresh on 401. Null = no usable session. */
    private suspend fun send(build: (String) -> Request): Pair<Int, String>? {
        AuthStateManager.refreshIfNeeded()
        val token = AuthStateManager.currentUser.value?.accessToken
        if (token.isNullOrBlank()) return null
        var reply = execute(build(token))
        if (reply.first == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
            val fresh = AuthStateManager.currentUser.value?.accessToken
            if (!fresh.isNullOrBlank()) reply = execute(build(fresh))
        }
        return reply
    }

    private fun execute(request: Request): Pair<Int, String> =
        http.newCall(request).execute().use { resp -> Pair(resp.code, resp.body?.string().orEmpty()) }

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }
}
