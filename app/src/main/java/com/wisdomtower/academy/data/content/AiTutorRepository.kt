package com.wisdomtower.academy.data.content

import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One event of the AI tutor stream. */
sealed class TutorEvent {
    /** A piece of the answer text (append it). */
    data class Chunk(val text: String) : TutorEvent()

    /** The follow-up prompts that come at the end of an answer. */
    data class Suggestions(val items: List<String>) : TutorEvent()

    /** The server finished the answer. */
    object Done : TutorEvent()

    /** The request failed (no network, server error, broken stream). */
    object Failed : TutorEvent()
}

/**
 * Native client of the website's AI tutor route `/api/ai/tutor` (plan step N10, port of the request part of
 * website-reference/src/components/learning/AiTutor.tsx). The AI keys stay on the website server. Request body as the
 * website sends it: `{ messages: [{ role, content }...], courseContext? }` (`courseContext` is left out when empty, like
 * `JSON.stringify` drops `undefined`). The answer is a server-sent stream of lines
 * `data: {"type":"chunk","text":...}`, `data: {"type":"suggestions","suggestions":[...]}` and `data: {"type":"done"}`.
 *
 * U1b (2026-11-06, docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): the tutor is for signed-in students only. Every request now
 * carries the student's Supabase login token (`Authorization: Bearer ...`), renewed first when it is about to expire.
 * With no login the request is not sent at all and the stream ends with [TutorEvent.Failed]. The screen shows a
 * sign-in card before this is ever reached; this is the second lock. The website route will start REJECTING calls
 * without a valid token in slice U1c, so an app older than this change stops getting answers after that.
 *
 * Cancelling the collector cancels the HTTP call. Never logs the questions, answers or the token.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object AiTutorRepository {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /** [history] = (role, content) pairs, role is "user" or "assistant", oldest first. */
    fun stream(history: List<Pair<String, String>>, courseContext: String?): Flow<TutorEvent> = callbackFlow {
        // U1b: no login, no request. Renew the token first when it is close to expiring.
        AuthStateManager.refreshIfNeeded()
        val token = AuthStateManager.currentUser.value?.accessToken
        if (token.isNullOrBlank()) {
            trySend(TutorEvent.Failed)
            close()
            awaitClose { }
            return@callbackFlow
        }

        val messages = JSONArray()
        history.forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content))
        }
        val body = JSONObject().put("messages", messages)
        if (!courseContext.isNullOrBlank()) body.put("courseContext", courseContext)

        val base = BuildConfig.NEXT_PUBLIC_SITE_URL.trim().trimEnd('/')
        val request = Request.Builder()
            .url("$base/api/ai/tutor")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        val call = http.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                trySend(TutorEvent.Failed)
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { resp ->
                        val source = resp.body?.source()
                        if (!resp.isSuccessful || source == null) {
                            trySend(TutorEvent.Failed)
                        } else {
                            while (true) {
                                val line = source.readUtf8Line() ?: break
                                val event = parseLine(line) ?: continue
                                trySend(event)
                            }
                        }
                    }
                } catch (e: Exception) {
                    trySend(TutorEvent.Failed)
                } finally {
                    close()
                }
            }
        })

        awaitClose { call.cancel() }
    }.buffer(Channel.UNLIMITED)

    private fun parseLine(line: String): TutorEvent? {
        val t = line.trim()
        if (!t.startsWith("data:")) return null
        val json = t.removePrefix("data:").trim()
        if (json.isEmpty()) return null
        return try {
            val d = JSONObject(json)
            when (d.optString("type", "")) {
                "chunk" -> {
                    val text = d.optString("text", "")
                    if (text.isEmpty()) null else TutorEvent.Chunk(text)
                }

                "suggestions" -> {
                    val arr = d.optJSONArray("suggestions")
                    val items = ArrayList<String>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val s = arr.optString(i, "")
                            if (s.isNotBlank()) items.add(s)
                        }
                    }
                    TutorEvent.Suggestions(items)
                }

                "done" -> TutorEvent.Done
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
