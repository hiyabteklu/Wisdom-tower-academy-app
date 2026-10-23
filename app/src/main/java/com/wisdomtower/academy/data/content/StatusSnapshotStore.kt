package com.wisdomtower.academy.data.content

import android.util.Log
import com.wisdomtower.academy.WisdomTowerApplication
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One saved copy: the rows as JSON and the time (ms since 1970) the server last answered. */
class SavedSnapshot(val rows: JSONArray, val savedAtMs: Long)

/**
 * OFFLINE STATUS (docs/OFFLINE_STATUS_PLAN.md, slice O1): the "latest sync" of the student's status data, saved on the
 * phone so the status screens can draw offline. One small file per student and kind (`progress`, `exams`, `daylog`) in the
 * app's private files folder `status_snapshots/`. A copy is only returned for the SAME user id, and saving for one student
 * deletes the copies of every other id. Only what the server last answered is saved (no invented values). Never logs
 * bodies or tokens. Never throws.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object StatusSnapshotStore {
    private const val TAG = "StatusSnapshot"
    private const val DIR = "status_snapshots"

    private fun safe(text: String): String = text.replace(Regex("[^A-Za-z0-9-]"), "_").take(64)

    private fun dir(): File? = try {
        File(WisdomTowerApplication.instance.filesDir, DIR).apply { mkdirs() }
    } catch (e: Exception) {
        null
    }

    /** Saves [rows] for [userId] and [kind], stamped with the current time. Writes a temp file first, then renames it. */
    fun save(userId: String, kind: String, rows: JSONArray) {
        try {
            val folder = dir() ?: return
            val prefix = safe(userId) + "_"
            folder.listFiles()?.forEach { f ->
                if (!f.name.startsWith(prefix)) runCatching { f.delete() }
            }
            val target = File(folder, prefix + safe(kind) + ".json")
            val temp = File(folder, prefix + safe(kind) + ".tmp")
            val root = JSONObject().put("t", System.currentTimeMillis()).put("rows", rows)
            temp.writeText(root.toString())
            if (!temp.renameTo(target)) {
                target.writeText(root.toString())
                runCatching { temp.delete() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "save failed: ${e.javaClass.simpleName}")
        }
    }

    /** The saved copy of [userId] and [kind], or null when there is none (or it cannot be read). */
    fun load(userId: String, kind: String): SavedSnapshot? {
        return try {
            val folder = dir() ?: return null
            val file = File(folder, safe(userId) + "_" + safe(kind) + ".json")
            if (!file.exists()) return null
            val root = JSONObject(file.readText())
            SavedSnapshot(root.getJSONArray("rows"), root.optLong("t", 0L))
        } catch (e: Exception) {
            Log.w(TAG, "load failed: ${e.javaClass.simpleName}")
            null
        }
    }
}
