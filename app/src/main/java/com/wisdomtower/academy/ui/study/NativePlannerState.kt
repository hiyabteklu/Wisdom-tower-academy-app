package com.wisdomtower.academy.ui.study

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.reminders.PlannerReminderScheduler
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

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

/** What the planner holds right now, for [PlannerSync]: the blocks, the server version they are based on, and whether they still have to be sent. */
internal class PlannerSnapshot(val blocks: List<StudyBlock>, val version: Int, val dirty: Boolean)

/**
 * State of the native Study Planner.
 *
 * SERVER SYNC (owner decision 2026-10-20: "plans are only in the server, sync from both sides, the app keeps a copy
 * for offline use"): the row of the signed-in student in `public.study_plans` (`blocks` jsonb + `version`) is the
 * master copy. This object keeps the phone copy (SharedPreferences, same key and JSON shape as before, so the reminder
 * receivers still read it) plus three extra values: which student the copy belongs to, the server version it is based
 * on, and a "dirty" flag (edited on the phone, not sent yet). [PlannerSync] does the network part:
 *  - an edit on the phone is saved here at once (works offline), marked dirty and sent to the server;
 *  - a newer server version (an admin push, or another phone) REPLACES the phone copy;
 *  - if both changed, the server wins (an admin push overwrites the app);
 *  - another student signing in on the same phone starts clean (the copy is never shown to another account).
 *
 * Reminders (owner decision 2026-10-12): every change of the blocks sets one weekly alarm per block through
 * [PlannerReminderScheduler]; the first time a block is added the student is asked once to allow notifications.
 *
 * Difference from the website (owner rule "no fabricated content"): the native planner starts EMPTY.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object NativePlannerState {
    private const val PREFS = "wta_native_study_planner"
    private const val KEY = "wt_study_planner_v3"
    private const val KEY_OWNER = "owner_user_id"
    private const val KEY_VERSION = "server_version"
    private const val KEY_DIRTY = "dirty"

    var blocks by mutableStateOf<List<StudyBlock>>(emptyList())
        private set

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    // The screen's own context (an Activity), used only to show the notification permission question.
    private var screenContext: WeakReference<Context>? = null

    /** Student id the phone copy belongs to (null = not known yet). */
    private var ownerId: String? = null

    /** Server version the phone copy is based on (0 = the server has no row for this student yet). */
    private var serverVersion: Int = 0

    /** True while the phone copy has edits the server does not have yet. */
    private var dirty: Boolean = false

    /** Loads the saved blocks once, makes sure their alarms exist, and asks the server for news. Safe to call many times. */
    fun init(context: Context) {
        screenContext = WeakReference(context)
        ensureLoaded(context.applicationContext)
        PlannerSync.syncSoon()
    }

    /** Loads the phone copy (once). Also used when the app comes to the front without the planner screen open. */
    @Synchronized
    internal fun ensureLoaded(app: Context) {
        if (prefs != null) return
        appContext = app
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        blocks = loadStored(app)
        ownerId = p.getString(KEY_OWNER, null)
        serverVersion = p.getInt(KEY_VERSION, 0)
        dirty = p.getBoolean(KEY_DIRTY, false)
        AuthStateManager.currentUser.value?.id?.let { reconcileOwnerLocked(it) }
        PlannerReminderScheduler.sync(app, blocks)
    }

    /** Reads the saved blocks straight from the phone (also used by the reminder receivers when the app is not open). */
    fun loadStored(context: Context): List<StudyBlock> {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            parseBlocks(JSONArray(raw))
        } catch (e: Exception) {
            emptyList() // damaged saved text: start empty
        }
    }

    /** Blocks of a JSON array (phone copy or server row); entries without id or times are skipped. */
    internal fun parseBlocks(arr: JSONArray): List<StudyBlock> {
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
        return out
    }

    internal fun blocksToJson(list: List<StudyBlock>): JSONArray {
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
        return arr
    }

    /** `persistBlocks` of the website: replaces the list, saves it on the phone, updates the alarms and sends it to the server. */
    fun save(next: List<StudyBlock>) {
        val added = next.size > blocks.size
        synchronized(this) {
            blocks = next
            dirty = true
            if (ownerId == null) ownerId = AuthStateManager.currentUser.value?.id
            persistLocked()
        }
        appContext?.let { PlannerReminderScheduler.sync(it, next) }
        if (added) PlannerReminderScheduler.askNotificationPermissionOnce(screenContext?.get())
        PlannerSync.syncSoon()
    }

    // ---------- used by PlannerSync ----------

    /** Makes sure the phone copy belongs to student [uid]; another student's copy is dropped. */
    @Synchronized
    internal fun reconcileOwner(uid: String) {
        reconcileOwnerLocked(uid)
    }

    private fun reconcileOwnerLocked(uid: String) {
        val owner = ownerId
        if (owner == null) {
            // Old copy from before the server sync: it is offered to the server once (the server wins if it already has a plan).
            ownerId = uid
            if (blocks.isNotEmpty()) {
                dirty = true
                serverVersion = 0
            }
            persistLocked()
        } else if (owner != uid) {
            ownerId = uid
            blocks = emptyList()
            serverVersion = 0
            dirty = false
            persistLocked()
            appContext?.let { PlannerReminderScheduler.sync(it, emptyList()) }
        }
    }

    @Synchronized
    internal fun snapshot(): PlannerSnapshot = PlannerSnapshot(blocks, serverVersion, dirty)

    /** The server accepted [pushed] as version [newVersion]. Still dirty when the student edited again meanwhile. */
    @Synchronized
    internal fun markPushed(pushed: List<StudyBlock>, newVersion: Int) {
        serverVersion = newVersion
        dirty = blocks != pushed
        persistLocked()
    }

    /**
     * The server has a different version: it replaces the phone copy. A normal pull is skipped when the student has
     * unsent edits; [force] (the send was refused because the server changed) lets the server win anyway.
     * Call on the main thread (the blocks are screen state).
     */
    @Synchronized
    internal fun applyServer(next: List<StudyBlock>, version: Int, force: Boolean) {
        if (dirty && !force) return
        blocks = next
        serverVersion = version
        dirty = false
        persistLocked()
        appContext?.let { PlannerReminderScheduler.sync(it, next) }
    }

    private fun persistLocked() {
        prefs?.edit()
            ?.putString(KEY, blocksToJson(blocks).toString())
            ?.putString(KEY_OWNER, ownerId)
            ?.putInt(KEY_VERSION, serverVersion)
            ?.putBoolean(KEY_DIRTY, dirty)
            ?.apply()
    }

    /** Same id style as the website: `blk-<millis>-<4 random characters>`. */
    fun newId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val tail = (1..4).map { chars.random() }.joinToString("")
        return "blk-${System.currentTimeMillis()}-$tail"
    }
}
