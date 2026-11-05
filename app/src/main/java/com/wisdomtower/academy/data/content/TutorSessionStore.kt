package com.wisdomtower.academy.data.content

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One chat message (same fields as the website's AiTutor `Message`). [role] is "user" or "assistant". */
data class TutorMessage(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: String,
    val suggestions: List<String> = emptyList()
)

/** One saved chat (same fields as the website's AiTutor `ChatSession`). */
data class TutorSession(
    val id: String,
    val title: String,
    val messages: List<TutorMessage>,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Keeps the AI tutor chats on this phone (plan step N10). The website keeps them in the browser
 * (`wt_ai_tutor_sessions_v2` / `wt_ai_tutor_active_session_id_v2`); natively they live in the app's private
 * SharedPreferences file `wt_ai_tutor` (never uploaded). Only the 50 newest chats are kept. Never logs the content.
 *
 * 2026-11-05: empty chats (no messages) are no longer written, except the active one, so the tutor opening on a new chat
 * every time (drawer, Learning tab, "Ask AI tutor" in a note) cannot fill the 50 slots with empty chats and push real
 * conversations out.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object TutorSessionStore {
    private const val PREFS = "wt_ai_tutor"
    private const val KEY_SESSIONS = "sessions_v2"
    private const val KEY_ACTIVE = "active_session_id_v2"
    private const val MAX_SESSIONS = 50

    /** A fresh empty chat titled "New chat" (the website's `createNewSession`). */
    fun newSession(): TutorSession {
        val now = System.currentTimeMillis()
        return TutorSession(
            id = "session-" + now + "-" + UUID.randomUUID().toString().take(5),
            title = "New chat",
            messages = emptyList(),
            createdAt = now,
            updatedAt = now
        )
    }

    /** The saved chats and the id of the active one, or null when nothing (usable) is saved. */
    fun load(context: Context): Pair<List<TutorSession>, String>? {
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_SESSIONS, null)
            if (raw == null) {
                null
            } else {
                val arr = JSONArray(raw)
                val list = ArrayList<TutorSession>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(sessionFromJson(o))
                }
                if (list.isEmpty()) {
                    null
                } else {
                    val stored = prefs.getString(KEY_ACTIVE, null)
                    val active = if (stored != null && list.any { it.id == stored }) stored else list[0].id
                    list to active
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun save(context: Context, sessions: List<TutorSession>, activeId: String) {
        try {
            val arr = JSONArray()
            sessions
                .filter { it.messages.isNotEmpty() || it.id == activeId }
                .take(MAX_SESSIONS)
                .forEach { arr.put(sessionToJson(it)) }
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SESSIONS, arr.toString())
                .putString(KEY_ACTIVE, activeId)
                .apply()
        } catch (e: Exception) {
            // best effort: the chat simply is not kept
        }
    }

    private fun sessionToJson(s: TutorSession): JSONObject {
        val msgs = JSONArray()
        s.messages.forEach { m ->
            msgs.put(
                JSONObject()
                    .put("id", m.id)
                    .put("role", m.role)
                    .put("content", m.content)
                    .put("timestamp", m.timestamp)
                    .put("suggestions", JSONArray(m.suggestions))
            )
        }
        return JSONObject()
            .put("id", s.id)
            .put("title", s.title)
            .put("messages", msgs)
            .put("createdAt", s.createdAt)
            .put("updatedAt", s.updatedAt)
    }

    private fun sessionFromJson(o: JSONObject): TutorSession {
        val msgs = ArrayList<TutorMessage>()
        val arr = o.optJSONArray("messages")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                val sugs = ArrayList<String>()
                val sa = m.optJSONArray("suggestions")
                if (sa != null) {
                    for (j in 0 until sa.length()) {
                        val s = sa.optString(j, "")
                        if (s.isNotBlank()) sugs.add(s)
                    }
                }
                msgs.add(
                    TutorMessage(
                        id = m.optString("id", "m$i"),
                        role = if (m.optString("role", "") == "user") "user" else "assistant",
                        content = m.optString("content", ""),
                        timestamp = m.optString("timestamp", ""),
                        suggestions = sugs
                    )
                )
            }
        }
        val now = System.currentTimeMillis()
        return TutorSession(
            id = o.optString("id", "session-$now"),
            title = o.optString("title", "New chat"),
            messages = msgs,
            createdAt = o.optLong("createdAt", now),
            updatedAt = o.optLong("updatedAt", now)
        )
    }
}
