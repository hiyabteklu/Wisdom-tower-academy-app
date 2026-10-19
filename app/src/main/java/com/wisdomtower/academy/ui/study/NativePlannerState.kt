package com.wisdomtower.academy.ui.study

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.reminders.PlannerReminderScheduler
import com.wisdomtower.academy.data.study.PlanFetchResult
import com.wisdomtower.academy.data.study.PlanSaveResult
import com.wisdomtower.academy.data.study.SupabasePlanClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/**
 * One block of the weekly timetable. Same shape as `StudyBlock` in
 * `website-reference/src/components/learning/StudyPlanner.tsx`: `day` 0 = Monday ... 6 = Sunday, `startTime` and
 * `endTime` as 24-hour "HH:MM", `color` = the website's palette class string (kept as it is so both sides can read it).
 */
data class StudyBlock(
    val id: String,
    val day: Int,
    val startTime: String,
    val endTime: String,
    val title: String,
    val color: String
)

/**
 * State of the native Study Planner.
 *
 * SERVER SYNC (owner order 2026-10-20): the plan lives in Supabase `public.study_plans` (one row per student), so it
 * survives a new phone / reinstall and admins can PUSH a ready-made schedule (RPC `admin_push_study_plan`, pushed
 * blocks have ids starting `adm-`). The phone copy (below) stays the working copy so the planner and the reminders
 * work offline:
 *  - every [save] writes the phone copy first, marks it "dirty" and tries to upload it in the background;
 *  - opening the planner ([init]) reads the server row; if the student has no unsent changes the server copy
 *    REPLACES the phone copy (this is how a pushed plan arrives); if there are unsent changes they are uploaded;
 *  - if an admin pushed while the student had unsent changes, both are merged (server blocks + the student's own
 *    blocks) and nothing is lost; the upload is guarded by the server `version` so a push is never overwritten;
 *  - the phone copy belongs to ONE student (`owner_user_id`); when another student signs in on the same phone the
 *    planner starts clean for them, plans are never mixed;
 *  - signed out: the planner stays local only; the plan is uploaded after the next sign-in.
 * Every change of the list (own edit or server copy) goes through [PlannerReminderScheduler.sync], so pushed blocks
 * get their reminders too.
 *
 * The phone copy is saved under the same key and JSON shape as the website (`wt_study_planner_v3`: a JSON array of
 * blocks), in its own preferences file. Reminders (owner decision 2026-10-12): one weekly alarm per block; the first
 * time a block is added the student is asked once to allow notifications (Android 13+).
 *
 * Difference from the website (owner rule "no fabricated content"): the website puts 7 invented sample blocks
 * ("Math - Calculus I" ...) into every new browser; the native planner starts EMPTY.
 *
 * STATUS: sync part written 2026-10-20, NOT compiled, NOT tested on a device.
 */
object NativePlannerState {
    private const val PREFS = "wta_native_study_planner"
    private const val KEY = "wt_study_planner_v3"
    private const val KEY_OWNER = "owner_user_id"
    private const val KEY_DIRTY = "unsent_changes"
    private const val KEY_VERSION = "server_version"

    /** Opening the planner again within this time does not ask the server again. */
    private const val MIN_SYNC_GAP_MS = 20_000L

    /** Prefix the server gives to blocks pushed by an admin (`admin_push_study_plan`). */
    private const val ADMIN_PREFIX = "adm-"

    var blocks by mutableStateOf<List<StudyBlock>>(emptyList())
        private set

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    // The screen's own context (an Activity), used only to show the notification permission question.
    private var screenContext: WeakReference<Context>? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    @Volatile
    private var lastSyncStartMs = 0L

    /** Counts the student's own edits; a sync result is only applied / the unsent flag only cleared if no edit happened meanwhile. */
    private val editCount = AtomicInteger(0)

    /** Loads the saved blocks once, makes sure their alarms exist, and asks the server for the latest plan. Safe to call many times. */
    fun init(context: Context) {
        screenContext = WeakReference(context)
        if (prefs == null) {
            val app = context.applicationContext
            appContext = app
            prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            blocks = loadStored(app)
            PlannerReminderScheduler.sync(app, blocks)
        }
        requestSync(force = false)
    }

    /** Asks the server now (also usable for a "refresh" action). Runs in the background; the list updates itself. */
    fun requestSync(force: Boolean = true) {
        if (prefs == null) return
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncStartMs < MIN_SYNC_GAP_MS) return
        lastSyncStartMs = now
        scope.launch {
            try {
                syncWithServer()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // never crash the planner because of the network; the phone copy stays and the next sync tries again
            }
        }
    }

    /** Reads the saved blocks straight from the phone (also used by the reminder receivers when the app is not open). */
    fun loadStored(context: Context): List<StudyBlock> {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<StudyBlock>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                val start = o.optString("startTime", "")
                val end = o.optString("endTime", "")
                if (id.isEmpty() || start.isEmpty() || end.isEmpty()) continue
                out.add(
                    StudyBlock(
                        id = id,
                        day = o.optInt("day", 0).coerceIn(0, 6),
                        startTime = start,
                        endTime = end,
                        title = o.optString("title", "Study Session"),
                        color = o.optString("color", "")
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList() // damaged saved text: start empty
        }
    }

    /** `persistBlocks` of the website: replaces the list, saves it on the phone, updates the reminder alarms, uploads it. */
    fun save(next: List<StudyBlock>) {
        val added = next.size > blocks.size
        blocks = next
        editCount.incrementAndGet()
        writeLocal(next, unsentChanges = true)
        appContext?.let { PlannerReminderScheduler.sync(it, next) }
        if (added) PlannerReminderScheduler.askNotificationPermissionOnce(screenContext?.get())
        requestSync(force = true)
    }

    /** Same id style as the website: `blk-<millis>-<4 random characters>`. */
    fun newId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val tail = (1..4).map { chars.random() }.joinToString("")
        return "blk-${System.currentTimeMillis()}-$tail"
    }

    // ---------- server sync ----------

    private fun writeLocal(list: List<StudyBlock>, unsentChanges: Boolean?) {
        val arr = JSONArray()
        list.forEach { b ->
            val o = JSONObject()
            o.put("id", b.id)
            o.put("day", b.day)
            o.put("startTime", b.startTime)
            o.put("endTime", b.endTime)
            o.put("title", b.title)
            o.put("color", b.color)
            arr.put(o)
        }
        val e = prefs?.edit() ?: return
        e.putString(KEY, arr.toString())
        if (unsentChanges != null) e.putBoolean(KEY_DIRTY, unsentChanges)
        e.apply()
    }

    /** Puts a list that came from the server (or a merge) on screen, on the phone and into the alarms. Not an own edit. */
    private suspend fun applyLocal(list: List<StudyBlock>) {
        withContext(Dispatchers.Main) { blocks = list }
        writeLocal(list, unsentChanges = null)
        appContext?.let { PlannerReminderScheduler.sync(it, list) }
    }

    private fun clearUnsentIfNoNewEdit(editsAtStart: Int) {
        if (editCount.get() == editsAtStart) prefs?.edit()?.putBoolean(KEY_DIRTY, false)?.apply()
    }

    /** The server's blocks plus the student's own blocks that the server does not have. Admin blocks that the server dropped stay dropped. */
    private fun merge(server: List<StudyBlock>, local: List<StudyBlock>): List<StudyBlock> {
        val serverIds = server.map { it.id }.toSet()
        return server + local.filter { it.id !in serverIds && !it.id.startsWith(ADMIN_PREFIX) }
    }

    private suspend fun syncWithServer() = syncMutex.withLock {
        val p = prefs ?: return@withLock
        AuthStateManager.refreshIfNeeded()
        val user = AuthStateManager.currentUser.value ?: return@withLock // signed out: local only
        val token = user.accessToken
        if (token.isNullOrBlank()) return@withLock
        val uid = user.id

        val owner = p.getString(KEY_OWNER, null)
        if (owner != null && owner != uid) {
            // Another student signed in on this phone: never mix two students' plans.
            editCount.incrementAndGet()
            p.edit().putString(KEY_OWNER, uid).putBoolean(KEY_DIRTY, false).remove(KEY_VERSION).apply()
            applyLocal(emptyList())
        } else if (owner == null) {
            // A plan made before sync existed (or while signed out) belongs to this student: upload it.
            p.edit().putString(KEY_OWNER, uid).putBoolean(KEY_DIRTY, blocks.isNotEmpty()).apply()
        }

        val editsAtStart = editCount.get()
        val dirty = p.getBoolean(KEY_DIRTY, false)
        val knownVersion = p.getInt(KEY_VERSION, -1)
        val local = blocks

        when (val r = SupabasePlanClient.fetch(uid, token)) {
            is PlanFetchResult.SessionExpired, is PlanFetchResult.Failed -> return@withLock

            is PlanFetchResult.NotFound -> {
                if (!dirty && local.isEmpty()) return@withLock // nothing here, nothing there
                if (SupabasePlanClient.insert(uid, token, local) == PlanSaveResult.Saved) {
                    p.edit().putInt(KEY_VERSION, 1).apply()
                    clearUnsentIfNoNewEdit(editsAtStart)
                }
                // Conflict (a row appeared meanwhile) or failure: the next sync reads the row and merges.
            }

            is PlanFetchResult.Found -> {
                if (!dirty) {
                    // Nothing unsent: the server copy is the truth (this is how an admin push arrives).
                    if (editCount.get() == editsAtStart && r.blocks != local) applyLocal(r.blocks)
                    p.edit().putInt(KEY_VERSION, r.version).apply()
                    return@withLock
                }
                // Unsent changes. If the server row moved since we last saw it (an admin push), merge first.
                val toSend = if (r.version == knownVersion) local else merge(r.blocks, local)
                if (toSend != local) {
                    if (editCount.get() != editsAtStart) return@withLock // edited meanwhile: next sync
                    applyLocal(toSend)
                }
                if (SupabasePlanClient.update(uid, token, toSend, r.version) == PlanSaveResult.Saved) {
                    p.edit().putInt(KEY_VERSION, r.version).apply()
                    clearUnsentIfNoNewEdit(editsAtStart)
                }
                // Conflict (changed again meanwhile) or failure: still marked unsent, the next sync tries again.
            }
        }
    }
}
