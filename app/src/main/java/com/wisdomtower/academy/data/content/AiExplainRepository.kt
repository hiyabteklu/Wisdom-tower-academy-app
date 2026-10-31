package com.wisdomtower.academy.data.content

import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.WisdomTowerApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * AI helpers of the native learning screens (plan step N10). Both call the website route `/api/ai/explain`
 * (website-reference/src/app/api/ai/explain/route.ts): the AI keys stay on the website server, the route needs
 * no sign-in, and it keeps its own shared cache of answers (table `ai_explanations`).
 *  - [explain]: "Explain with AI" of the quiz screen. The body is the one the website's `QuizExamViewer.tsx` sends:
 *    `{ mode: "question", text, question, choices, studentAnswer, correctAnswer, solution, resourceId }`.
 *    Fallbacks: the route's `explanation`, else its `error`, else "-"; "AI unavailable" when unreachable.
 *  - [summarize]: the "Summary" button of a short note. The body is the one the website's `NotesViewer.tsx` sends:
 *    `{ text: first 8000 characters of the note, resourceId, mode: "summarize" }`. Fallbacks as the website:
 *    `explanation`, else `error`, else "No summary returned."; "Could not reach AI. Try again later." when unreachable.
 *
 * THREE-LAYER CACHE (2026-10-31):
 *  1. This phone: a real answer is kept in a private file under filesDir/ai_explain_cache. The same question (or
 *     the same note) on another day, or with no internet, is answered from there without any network call.
 *  2. The database (website route): an answer any other student already got is returned from `ai_explanations`.
 *  3. The AI: only when neither layer has it.
 * Both the phone key and the server key are a FINGERPRINT OF THE CONTENT (question text, the choices in order, the
 * correct answer, the solution; for a summary the note text). So when a question is edited or replaced, its
 * fingerprint changes: the old answer is not found on the phone or on the server and the AI is asked again.
 * The student's own pick is not part of the key (the stored answer is shared). An answer flagged `fallback` by the
 * server ("AI unavailable") is never kept. Bump [CACHE_VERSION] to make every phone forget its copies.
 *
 * Never throws (except cancellation), never logs the question or note text.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object AiExplainRepository {
    private const val UNAVAILABLE = "AI unavailable"
    private const val SUMMARY_EMPTY = "No summary returned."
    private const val SUMMARY_UNREACHABLE = "Could not reach AI. Try again later."

    /** Keep in step with PROMPT_VERSION of the website route when the tutor prompt changes on purpose. */
    private const val CACHE_VERSION = "v2"
    private const val CACHE_DIR = "ai_explain_cache"
    private const val MAX_ENTRIES = 1500

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    suspend fun explain(payloadJson: String): String = post(payloadJson, "-", UNAVAILABLE)

    suspend fun summarize(noteText: String, resourceId: String): String {
        val payload = JSONObject()
            .put("text", noteText.take(8000))
            .put("resourceId", resourceId)
            .put("mode", "summarize")
            .toString()
        return post(payload, SUMMARY_EMPTY, SUMMARY_UNREACHABLE)
    }

    private suspend fun post(payloadJson: String, emptyText: String, unreachableText: String): String =
        withContext(Dispatchers.IO) {
            // Layer 1: this phone
            val key = deviceKey(payloadJson)
            if (key != null) {
                val local = readLocal(key)
                if (local != null) return@withContext local
            }

            // Layers 2 and 3: the website route (database cache first, then the AI)
            try {
                val base = BuildConfig.NEXT_PUBLIC_SITE_URL.trim().trimEnd('/')
                val request = Request.Builder()
                    .url("$base/api/ai/explain")
                    .post(payloadJson.toRequestBody(JSON_MEDIA))
                    .build()
                http.newCall(request).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    val data = try {
                        JSONObject(raw)
                    } catch (e: Exception) {
                        null
                    }
                    if (data == null) {
                        unreachableText
                    } else {
                        val text = data.optString("explanation", "").trim()
                        val error = data.optString("error", "").trim()
                        val isFallback = data.optBoolean("fallback", false)
                        if (text.isNotEmpty() && !isFallback && resp.isSuccessful && key != null) {
                            writeLocal(key, text)
                        }
                        when {
                            text.isNotEmpty() -> text
                            error.isNotEmpty() -> error
                            else -> emptyText
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                unreachableText
            }
        }

    // ---------------------------------------------------------------------------------------------
    // Phone cache (one small text file per answer, named by the content fingerprint)
    // ---------------------------------------------------------------------------------------------

    private fun norm(s: String): String = s.replace(Regex("\\s+"), " ").trim()

    /** Fingerprint of the content the answer is about, or null when there is nothing to key on (then nothing is kept). */
    private fun deviceKey(payloadJson: String): String? {
        return try {
            val p = JSONObject(payloadJson)
            val mode = p.optString("mode", "notes")
            val text = p.optString("text", "").ifBlank { p.optString("question", "") }
            if (text.isBlank()) return null
            val canonical = if (mode == "question") {
                val choices = p.optJSONArray("choices")
                val choiceList = ArrayList<String>()
                if (choices != null) {
                    for (i in 0 until choices.length()) choiceList.add(norm(choices.optString(i, "")))
                }
                listOf(
                    CACHE_VERSION,
                    mode,
                    norm(text).take(8000),
                    choiceList.joinToString("\u001F"),
                    norm(p.optString("correctAnswer", "")),
                    norm(p.optString("solution", "")).take(3000)
                ).joinToString("\u001E")
            } else {
                listOf(CACHE_VERSION, mode, norm(text).take(8000)).joinToString("\u001E")
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun cacheDir(): File? {
        return try {
            val dir = File(WisdomTowerApplication.instance.filesDir, CACHE_DIR)
            if (!dir.exists()) dir.mkdirs()
            if (dir.isDirectory) dir else null
        } catch (e: Exception) {
            null
        }
    }

    private fun readLocal(key: String): String? {
        return try {
            val dir = cacheDir() ?: return null
            val file = File(dir, "$key.txt")
            if (!file.isFile) return null
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) return null
            file.setLastModified(System.currentTimeMillis()) // recently used answers are kept longest
            text
        } catch (e: Exception) {
            null
        }
    }

    private fun writeLocal(key: String, text: String) {
        try {
            val dir = cacheDir() ?: return
            val tmp = File(dir, "$key.tmp")
            tmp.writeText(text, Charsets.UTF_8)
            val target = File(dir, "$key.txt")
            if (!tmp.renameTo(target)) {
                target.writeText(text, Charsets.UTF_8)
                tmp.delete()
            }
            prune(dir)
        } catch (e: Exception) {
            // best effort: the answer is simply not kept on the phone
        }
    }

    /** Keeps the newest [MAX_ENTRIES] answers; answers of edited or removed questions age out here. */
    private fun prune(dir: File) {
        try {
            val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: return
            if (files.size <= MAX_ENTRIES) return
            files.sortedBy { it.lastModified() }
                .take(files.size - MAX_ENTRIES)
                .forEach { it.delete() }
        } catch (e: Exception) {
            // ignore
        }
    }
}
