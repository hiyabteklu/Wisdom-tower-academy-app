package com.wisdomtower.academy.data.admin

import android.content.Context
import android.net.Uri
import com.wisdomtower.academy.OfflineVault
import com.wisdomtower.academy.data.content.LearningContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row of the `learning_resources` table (website-reference/src/lib/content.ts, type LearningResource). */
data class AdminContentItem(
    val id: String,
    val packageId: String,
    val scopePath: String,
    val hub: String,
    val title: String,
    val chapter: Int?,
    val sortOrder: Int,
    val contentType: String,
    val storagePath: String?,
    val bodyMd: String?,
    /** The meta column as compact JSON text. Pretty printing happens only when the editor opens. */
    val metaRaw: String,
    val published: Boolean,
    val sizeBytes: Long? = null
)

/** What the editor hands to save(): the same fields the website ResourceInput carries. */
class AdminContentInput(
    val packageId: String,
    val scopePath: String,
    val hub: String,
    val title: String,
    val chapter: Int?,
    val sortOrder: Int,
    val contentType: String,
    val storagePath: String?,
    val bodyMd: String?,
    val metaText: String,
    val published: Boolean
)

sealed class AdminContentResult {
    class Success(val items: List<AdminContentItem>) : AdminContentResult()
    class Failure(val message: String) : AdminContentResult()
}

/**
 * ADMIN-native N7 (Course Content). Same data and steps as the website ContentPanel and lib/content.ts
 * (listResources, upsertResource, deleteResource), with the admin's own login. Plain Supabase REST, no API route,
 * no secret.
 *  - list: rows of learning_resources for one scope and one hub, ordered by sort_order. Same scope rules as the
 *    website: an ECE course also matches its twin in the other semester, freshman math has two scope names,
 *    short-notes also matches the old hub name references. Not carried over: the background auto-heal write and
 *    the same-origin API fallback (an admin reads the table directly).
 *  - save: new row = insert; existing row = update by id (the website upserts; the result is the same).
 *  - delete: delete by id.
 * NOT carried over: choosing a file on the phone and uploading it (the website posts it to its API route
 * /api/storage/upload, which has no app-side caller yet). The Appwrite File ID field, which the website offers for
 * large files, IS here and is the way to attach a PDF from the app.
 * Difference, on purpose: delete and update report an error when no row was changed (a silent refusal).
 * 2026-10-19: the missing-table message no longer tells admins to run the old setup script on the live database.
 *
 * SECURITY S4 (2026-10-20): the database no longer lets any login read `body_md` and `meta` straight from the table.
 * The admin LIST therefore reads the admin-only view `learning_resources_admin` (all columns, rows only for an admin).
 * Writes still go to the table; save and delete ask only `select=id` back, because the default "return the whole row"
 * would need SELECT on `body_md` and `meta`.
 *
 * SECURITY S5c (2026-10-20): Life Savers are PAID PDFs and live in the PRIVATE Appwrite bucket `paid-pdfs`. The editor
 * still hands over `appwrite:<FILE_ID>` (the Appwrite File ID field), but [save] turns it into `appwrite-paid:<FILE_ID>`
 * for the hub `life-savers` ([paidIfNeeded]), so the app asks the Edge Function `book-url` for a short-lived address
 * ([com.wisdomtower.academy.data.content.LearningContentRepository.paidBookUrl]) instead of a public one. Free books keep
 * `appwrite:<FILE_ID>`. [appwriteIdOf] and [isAppwritePath] understand both prefixes, so an existing paid item shows its
 * File ID again in the editor.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object AdminContentRepository {
    private const val APPWRITE_PREFIX = "appwrite:"
    private const val APPWRITE_PAID_PREFIX = "appwrite-paid:"
    private val ECE_SCOPE = Regex("^ece/(sem-[12])/([^/]+)\$")

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    // ---- hub rules (HUB_CONTENT_DEFAULTS in website-reference/src/data/admin-nav.ts) ----

    fun contentTypeFor(hub: String, fallback: String): String = when (hub) {
        "books", "life-savers" -> "pdf"
        "short-notes" -> "markdown"
        "videos" -> "video_url"
        "flashcards" -> "flashcard_deck"
        "question-banks" -> "quiz"
        "exams" -> "exam"
        else -> fallback.ifEmpty { "markdown" }
    }

    /** The website's own hint for the hub, except where it talks about choosing a file (the app has no file chooser yet). */
    fun hubHint(hub: String): String = when (hub) {
        "books" ->
            "Paste the Appwrite File ID of the PDF (upload it in Appwrite Console > Storage). " +
                "Choosing a file on the phone is not in the app yet."
        "life-savers" ->
            "Life Savers are PAID. Upload the PDF into the PRIVATE bucket paid-pdfs (Appwrite Console > Storage > paid-pdfs), " +
                "never into the public books bucket, then paste its File ID. The app saves it as appwrite-paid automatically. " +
                "Choosing a file on the phone is not in the app yet."
        "short-notes" ->
            "Markdown notes in the text box (Amharic OK). Images: paste image URLs in markdown. No file upload required."
        "videos" ->
            "Paste a YouTube/Vimeo URL. An optional file is attached with its Appwrite File ID."
        "flashcards" ->
            "JSON in meta only (stored in Supabase DB, not Appwrite). Example: { cards: [{front, back}] }."
        "question-banks" ->
            "JSON in meta only (Supabase DB). Example: { questions: [{prompt, choices, correct, solution}] }."
        "exams" ->
            "JSON in meta only (Supabase DB). For rich solutions with images, put markdown image URLs in solution text."
        else -> ""
    }

    private val FLASHCARD_TEMPLATE = """
{
  "cards": [
    {
      "front": "Term",
      "back": "Definition"
    }
  ]
}
""".trimIndent()

    private fun quizTemplate(withDuration: Boolean): String {
        val duration = if (withDuration) "  \"durationMin\": 60,\n" else ""
        return "{\n" + duration +
            "  \"questions\": [\n" +
            "    {\n" +
            "      \"prompt\": \"Sample question?\",\n" +
            "      \"choices\": [\n" +
            "        \"A\",\n" +
            "        \"B\",\n" +
            "        \"C\",\n" +
            "        \"D\"\n" +
            "      ],\n" +
            "      \"correct\": 0,\n" +
            "      \"solution\": \"Explanation…\"\n" +
            "    }\n" +
            "  ]\n" +
            "}"
    }

    /** What a new item starts with (openNew in ContentPanel.tsx). */
    fun defaultMeta(hub: String): String = when (hub) {
        "flashcards" -> FLASHCARD_TEMPLATE
        "question-banks" -> quizTemplate(false)
        "exams" -> quizTemplate(true)
        else -> "{}"
    }

    /** Pretty JSON for the editor. Android escapes the slash as backslash-slash, which is undone so links stay readable. */
    fun prettyMeta(raw: String): String = try {
        JSONObject(raw).toString(2).replace("\\/", "/")
    } catch (e: Exception) {
        raw
    }

    /** The File ID inside a free (`appwrite:`) or paid (`appwrite-paid:`) storage path, or "" for any other path. */
    fun appwriteIdOf(storagePath: String?): String = when {
        storagePath == null -> ""
        storagePath.startsWith(APPWRITE_PAID_PREFIX) -> storagePath.substring(APPWRITE_PAID_PREFIX.length)
        storagePath.startsWith(APPWRITE_PREFIX) -> storagePath.substring(APPWRITE_PREFIX.length)
        else -> ""
    }

    fun appwritePath(fileId: String): String = APPWRITE_PREFIX + fileId

    fun isAppwritePath(storagePath: String?): Boolean =
        storagePath != null && (storagePath.startsWith(APPWRITE_PREFIX) || storagePath.startsWith(APPWRITE_PAID_PREFIX))

    /**
     * Security slice S5c: the hub `life-savers` holds PAID PDFs, which live in the private bucket. A free-style path
     * `appwrite:<id>` coming from the editor becomes `appwrite-paid:<id>` there; every other hub and every other kind of
     * path is left exactly as it is.
     */
    private fun paidIfNeeded(hub: String, storage: String): String =
        if (hub == "life-savers" && storage.startsWith(APPWRITE_PREFIX)) {
            APPWRITE_PAID_PREFIX + storage.substring(APPWRITE_PREFIX.length)
        } else {
            storage
        }

    // ---- reading ----

    private fun scopeFilter(scopePath: String): String {
        val ece = ECE_SCOPE.find(scopePath)
        if (ece != null) {
            val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
            return "in.(" + Uri.encode(scopePath) + "," + Uri.encode("ece/" + other + "/" + ece.groupValues[2]) + ")"
        }
        if (scopePath == "freshman/math-natural" || scopePath == "freshman/mathematics") {
            return "in.(" + Uri.encode("freshman/math-natural") + "," + Uri.encode("freshman/mathematics") + ")"
        }
        return "eq." + Uri.encode(scopePath)
    }

    private fun parse(body: String): List<AdminContentItem>? {
        return try {
            val arr = JSONArray(body)
            val out = ArrayList<AdminContentItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.text("id") ?: continue
                val rawHub = o.text("hub") ?: ""
                val metaValue = o.opt("meta")
                out.add(
                    AdminContentItem(
                        id = id,
                        packageId = o.text("package_id") ?: "",
                        scopePath = o.text("scope_path") ?: "",
                        hub = if (rawHub == "references") "short-notes" else rawHub,
                        title = o.text("title") ?: "",
                        chapter = if (o.isNull("chapter")) null else o.optInt("chapter"),
                        sortOrder = o.optInt("sort_order", 0),
                        contentType = o.text("content_type") ?: "",
                        storagePath = o.text("storage_path"),
                        bodyMd = o.text("body_md"),
                        metaRaw = if (metaValue is JSONObject) metaValue.toString() else "{}",
                        published = o.optBoolean("published", false),
                        sizeBytes = if (o.isNull("size_bytes")) null else o.optLong("size_bytes").takeIf { it > 0L }
                    )
                )
            }
            out
        } catch (e: Exception) {
            null
        }
    }

    private fun failure(reply: AdminHttpResult): AdminContentResult.Failure {
        val msg = reply.errorText()
        val missingTable = msg.contains("relation") || msg.contains("does not exist")
        return AdminContentResult.Failure(
            if (missingTable) "Table missing: tell the database owner. Do not run the old setup scripts." else msg
        )
    }

    suspend fun list(scopePath: String, hub: String): AdminContentResult {
        val hubFilter = if (hub == "short-notes") "in.(short-notes,references)" else "eq." + Uri.encode(hub)
        // Security S4: body_md and meta are hidden from the public columns of learning_resources, so the admin reads
        // the full rows through the admin-only view learning_resources_admin (empty for non-admins).
        val base = "learning_resources_admin?select=*&order=sort_order.asc&hub=" + hubFilter
        val reply = AdminSupabase.get(base + "&scope_path=" + scopeFilter(scopePath))
        if (!reply.ok) return failure(reply)
        var items: List<AdminContentItem> =
            parse(reply.body) ?: return AdminContentResult.Failure("Unexpected reply from the server")

        val ece = ECE_SCOPE.find(scopePath)
        if (ece != null) {
            if (items.isEmpty()) {
                // Same fallback as the website: look for the course slug anywhere in the scope path.
                val slug = ece.groupValues[2]
                val fallback = AdminSupabase.get(base + "&scope_path=ilike.*" + Uri.encode(slug) + "*")
                if (fallback.ok) {
                    val more = parse(fallback.body)
                    if (more != null && more.isNotEmpty()) items = more
                }
            }
            val packageId = if (ece.groupValues[1] == "sem-1") "ece-y3-sem-1" else "ece-y3-sem-2"
            items = items.map { it.copy(scopePath = scopePath, packageId = packageId) }
        }
        return AdminContentResult.Success(items)
    }

    // ---- writing ----

    /** Empty text = empty object. Anything that is not exactly one JSON object gives null. */
    private fun parseMeta(text: String): JSONObject? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return JSONObject()
        return try {
            val tokener = JSONTokener(trimmed)
            val value = tokener.nextValue()
            if (value is JSONObject && tokener.nextClean() == '\u0000') value else null
        } catch (e: Exception) {
            null
        }
    }

    /** itemId empty = new item. */
    suspend fun save(itemId: String, input: AdminContentInput, context: Context): GrantOutcome {
        val meta: JSONObject = parseMeta(input.metaText) ?: return GrantOutcome(false, "Meta JSON is invalid")

        // Security S5c: a Life Savers file id is stored as appwrite-paid:<id> (private bucket), see paidIfNeeded.
        val storage = paidIfNeeded(input.hub, input.storagePath?.trim().orEmpty())
        val bodyMd = input.bodyMd.orEmpty()
        val sizeBytes = if (input.hub == "books" && storage.startsWith(APPWRITE_PREFIX)) {
            withContext(Dispatchers.IO) {
                val url = LearningContentRepository.appwriteViewUrl(storage)
                if (url == null) 0L else OfflineVault.probeSizeOnline(context.applicationContext, url)
            }.takeIf { it > 0L }
        } else null
        val body = JSONObject()
        body.put("package_id", input.packageId)
        body.put("scope_path", input.scopePath)
        body.put("hub", input.hub)
        body.put("title", input.title.trim().ifEmpty { "Untitled" })
        body.put("chapter", input.chapter ?: JSONObject.NULL)
        body.put("sort_order", input.sortOrder)
        body.put("content_type", input.contentType)
        body.put("storage_path", if (storage.isEmpty()) JSONObject.NULL else storage)
        body.put("size_bytes", sizeBytes ?: JSONObject.NULL)
        body.put("body_md", if (bodyMd.isEmpty()) JSONObject.NULL else bodyMd)
        body.put("meta", meta)
        body.put("published", input.published)
        body.put("updated_at", nowIso())

        if (itemId.isEmpty()) {
            val reply = AdminSupabase.post("learning_resources", body.toString(), "return=minimal")
            if (!reply.ok) return GrantOutcome(false, reply.errorText())
            return GrantOutcome(true, "Saved")
        }
        // Security S4: ask for the id column only back (select=id). The default "give me the whole row back" would
        // need SELECT on body_md and meta, which the database no longer grants.
        val reply = AdminSupabase.patch("learning_resources?id=eq." + Uri.encode(itemId) + "&select=id", body.toString())
        if (!reply.ok) return GrantOutcome(false, reply.errorText())
        val changed = try {
            JSONArray(reply.body).length()
        } catch (e: Exception) {
            0
        }
        if (changed == 0) return GrantOutcome(false, "Not saved: item not found or no permission")
        return GrantOutcome(true, "Saved")
    }

    /**
     * Backfill old free book rows with a HEAD-only size check. Called when an admin opens the Books hub.
     * It never downloads a PDF body.
     */
    suspend fun backfillBookSizes(context: Context): Int = withContext(Dispatchers.IO) {
        val reply = AdminSupabase.get(
            "learning_resources_admin?select=id,storage_path,size_bytes&hub=eq.books&size_bytes=is.null&order=sort_order.asc"
        )
        if (!reply.ok) return@withContext 0
        val rows = try { JSONArray(reply.body) } catch (e: Exception) { return@withContext 0 }
        var updated = 0
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: continue
            val path = row.optString("storage_path").takeIf { it.startsWith(APPWRITE_PREFIX) } ?: continue
            val url = LearningContentRepository.appwriteViewUrl(path) ?: continue
            val size = OfflineVault.probeSizeOnline(context.applicationContext, url)
            if (size <= 0L) continue
            val patch = AdminSupabase.patch(
                "learning_resources?id=eq." + Uri.encode(id) + "&select=id",
                JSONObject().put("size_bytes", size).toString()
            )
            if (patch.ok && runCatching { JSONArray(patch.body).length() }.getOrDefault(0) > 0) updated++
        }
        updated
    }


    suspend fun delete(itemId: String): GrantOutcome {
        // Security S4: id column only back, same reason as in save().
        val reply = AdminSupabase.delete("learning_resources?id=eq." + Uri.encode(itemId) + "&select=id")
        if (!reply.ok) return GrantOutcome(false, reply.errorText())
        val removed = try {
            JSONArray(reply.body).length()
        } catch (e: Exception) {
            0
        }
        if (removed == 0) return GrantOutcome(false, "Not deleted: no such item or no permission")
        return GrantOutcome(true, "Deleted")
    }
}
