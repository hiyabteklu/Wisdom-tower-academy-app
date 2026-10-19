package com.wisdomtower.academy.data.admin

import android.net.Uri
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row of `free_resource_pages` (website-reference/src/lib/free-resources.ts, type FreeResourcePage). */
data class FreeResourcePage(
    val id: String,
    val slug: String,
    val title: String,
    val subtitle: String,
    val bodyMd: String,
    val published: Boolean
)

/**
 * One row of `free_resource_items`. metaJson is the whole `meta` column as JSON text, so keys the editor does not
 * show (for example provider or level, read by the student app) are kept when an item is saved.
 */
data class FreeResourceItem(
    val id: String,
    val pageSlug: String,
    val kind: String,
    val title: String,
    val subtitle: String,
    val bodyMd: String,
    val imagePath: String,
    val externalUrl: String,
    val deadline: String,
    val metaJson: String,
    val sortOrder: Int,
    val featured: Boolean,
    val published: Boolean
)

/** What the editor hands to saveItem. id null = new item. */
class FreeResourceDraft(
    val id: String?,
    val pageSlug: String,
    val kind: String,
    val title: String,
    val subtitle: String,
    val bodyMd: String,
    val imagePath: String?,
    val externalUrl: String,
    val deadline: String,
    val meta: JSONObject,
    val sortOrder: Int,
    val featured: Boolean,
    val published: Boolean
)

sealed class FreeResourcesResult {
    class Success(val pages: List<FreeResourcePage>, val items: List<FreeResourceItem>) : FreeResourcesResult()
    class Failure(val message: String) : FreeResourcesResult()
}

/**
 * ADMIN-native N10 (Free Resources). Same data and steps as the website FreeResourcesPanel and lib/free-resources.ts
 * (listFreeResourcePages, listFreeResourceItems, upsertFreeResourcePage, upsertFreeResourceItem,
 * deleteFreeResourceItem), with the admin's own login. Plain Supabase REST, no API route, no secret.
 *  - load: all page rows plus the items of one page slug, ordered like the website (sort_order, newest first)
 *  - save page: upsert on slug; sort_order is (position in the tab list + 1) times 10, like the website
 *  - save item: upsert on id (insert when there is no id)
 *  - delete item: delete by id
 * Differences, on purpose: the page upsert does not send meta, cover_path or updated_by (the website sends empty
 * values, which would wipe them; nothing in either panel edits them); every write asks for the saved rows back so a
 * silent refusal (HTTP 200 with zero rows) is reported; the deadline text is checked as a real date before sending.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object FreeResourcesRepository {
    /** Tab order of the website panel (FREE_RESOURCE_SLUGS). */
    val SLUGS: List<String> = listOf(
        "success-stories",
        "scholarships",
        "universities",
        "departments",
        "campus-life",
        "study-techniques"
    )

    /** Pages whose public body stays in the website code; admin only adds notes (NOTE_ONLY_SLUGS). */
    private val NOTE_ONLY: Set<String> = setOf("universities", "departments", "campus-life", "study-techniques")

    fun labelFor(slug: String): String = when (slug) {
        "success-stories" -> "Success Stories"
        "study-techniques" -> "Study Techniques notes"
        "campus-life" -> "Campus Life notes"
        "universities" -> "University notes"
        "departments" -> "Department notes"
        "scholarships" -> "Scholarships"
        else -> slug
    }

    /** PAGE_ITEM_KIND of the website. */
    fun kindFor(slug: String): String? = when (slug) {
        "success-stories" -> "success_story"
        "scholarships" -> "scholarship"
        "universities" -> "university"
        "departments" -> "department"
        "campus-life" -> "general"
        "study-techniques" -> "tip"
        else -> null
    }

    fun isNoteOnly(slug: String): Boolean = NOTE_ONLY.contains(slug)

    fun addLabel(kind: String?): String = when (kind) {
        "success_story" -> "Add story"
        "scholarship" -> "Add opportunity"
        else -> "Add note"
    }

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    private fun countRows(body: String): Int = try {
        JSONArray(body).length()
    } catch (e: Exception) {
        0
    }

    private fun friendly(message: String): String {
        val missingTable = message.contains("relation") || message.contains("does not exist")
        return if (missingTable) {
            "Table missing: run website-reference/docs/free-resources-setup.sql in Supabase SQL Editor."
        } else {
            message
        }
    }

    private fun metaJsonOf(o: JSONObject): String {
        val raw = o.opt("meta")
        if (raw is JSONObject) return raw.toString()
        if (raw is String) {
            return try {
                JSONObject(raw).toString()
            } catch (e: Exception) {
                "{}"
            }
        }
        return "{}"
    }

    /** One page row set plus the items of this slug. Both reads run at the same time. */
    suspend fun load(slug: String): FreeResourcesResult = coroutineScope {
        val pagesCall = async { AdminSupabase.get("free_resource_pages?select=*&order=sort_order.asc") }
        val itemsCall = async {
            AdminSupabase.get(
                "free_resource_items?select=*&page_slug=eq.${Uri.encode(slug)}&order=sort_order.asc,created_at.desc"
            )
        }
        val pagesReply = pagesCall.await()
        val itemsReply = itemsCall.await()
        if (!pagesReply.ok) return@coroutineScope FreeResourcesResult.Failure(friendly(pagesReply.errorText()))
        if (!itemsReply.ok) return@coroutineScope FreeResourcesResult.Failure(friendly(itemsReply.errorText()))
        try {
            val pageArr = JSONArray(pagesReply.body)
            val pages = ArrayList<FreeResourcePage>(pageArr.length())
            for (i in 0 until pageArr.length()) {
                val o = pageArr.getJSONObject(i)
                pages.add(
                    FreeResourcePage(
                        id = o.text("id") ?: continue,
                        slug = o.text("slug") ?: continue,
                        title = o.text("title") ?: "",
                        subtitle = o.text("subtitle") ?: "",
                        bodyMd = o.text("body_md") ?: "",
                        published = o.optBoolean("published", false)
                    )
                )
            }
            val itemArr = JSONArray(itemsReply.body)
            val items = ArrayList<FreeResourceItem>(itemArr.length())
            for (i in 0 until itemArr.length()) {
                val o = itemArr.getJSONObject(i)
                items.add(
                    FreeResourceItem(
                        id = o.text("id") ?: continue,
                        pageSlug = o.text("page_slug") ?: slug,
                        kind = o.text("kind") ?: "general",
                        title = o.text("title") ?: "",
                        subtitle = o.text("subtitle") ?: "",
                        bodyMd = o.text("body_md") ?: "",
                        imagePath = o.text("image_path") ?: "",
                        externalUrl = o.text("external_url") ?: "",
                        deadline = (o.text("deadline") ?: "").take(10),
                        metaJson = metaJsonOf(o),
                        sortOrder = o.optInt("sort_order", 0),
                        featured = o.optBoolean("featured", false),
                        published = o.optBoolean("published", false)
                    )
                )
            }
            FreeResourcesResult.Success(pages, items)
        } catch (e: Exception) {
            FreeResourcesResult.Failure("Unexpected reply from the server")
        }
    }

    // ---- meta helpers (same default fields as emptyMetaFor on the website) ----

    /** Default meta keys of a new item (the website's emptyMetaFor). */
    fun emptyMeta(kind: String, slug: String): Map<String, String> {
        if (kind == "success_story") {
            return linkedMapOf("studentName" to "", "program" to "", "result" to "", "year" to "", "quote" to "")
        }
        if (kind == "scholarship") {
            return linkedMapOf("organization" to "", "amount" to "", "eligibility" to "", "status" to "open")
        }
        if (kind == "university" || slug == "universities") return linkedMapOf("universityId" to "")
        if (kind == "department" || slug == "departments") return linkedMapOf("departmentId" to "")
        return emptyMap()
    }

    /** One text field of the meta column. Empty when the key is missing or null. */
    fun metaText(metaJson: String, key: String): String {
        return try {
            val o = JSONObject(metaJson)
            if (o.isNull(key)) "" else o.optString(key, "")
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * The meta object to save: the default keys of this kind, then every key the item already has (so unknown keys
     * survive), then the values the editor changed. Same order of precedence as the website.
     */
    fun buildMeta(kind: String, slug: String, originalJson: String, edits: Map<String, String>): JSONObject {
        val out = JSONObject()
        for ((key, value) in emptyMeta(kind, slug)) out.put(key, value)
        try {
            val original = JSONObject(originalJson)
            val keys = original.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                out.put(key, original.get(key))
            }
        } catch (e: Exception) {
            // an unreadable old meta is replaced by the defaults and the edits
        }
        for ((key, value) in edits) out.put(key, value)
        return out
    }

    private fun isRealDate(text: String): Boolean {
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(text)) return false
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            f.isLenient = false
            f.parse(text) != null
        } catch (e: Exception) {
            false
        }
    }

    // ---- writes ----

    suspend fun savePage(
        existingId: String?,
        slug: String,
        title: String,
        subtitle: String,
        bodyMd: String,
        published: Boolean
    ): GrantOutcome {
        val sub = subtitle.trim()
        val body = JSONObject()
        if (existingId != null) body.put("id", existingId)
        body.put("slug", slug)
        body.put("title", title.trim().ifEmpty { labelFor(slug) })
        body.put("subtitle", if (sub.isEmpty()) JSONObject.NULL else sub)
        body.put("body_md", bodyMd)
        body.put("published", published)
        body.put("sort_order", (SLUGS.indexOf(slug) + 1) * 10)
        body.put("updated_at", nowIso())
        val reply = AdminSupabase.post(
            "free_resource_pages?on_conflict=slug",
            body.toString(),
            "resolution=merge-duplicates,return=representation"
        )
        if (!reply.ok) return GrantOutcome(false, friendly(reply.errorText()))
        if (countRows(reply.body) == 0) return GrantOutcome(false, "Not saved: no permission to change the page")
        return GrantOutcome(true, if (published) "Page saved and published" else "Page saved (draft)")
    }

    suspend fun saveItem(draft: FreeResourceDraft): GrantOutcome {
        val deadline = draft.deadline.trim()
        if (deadline.isNotEmpty() && !isRealDate(deadline)) {
            return GrantOutcome(false, "Deadline must be a real date like 2026-11-30")
        }
        val sub = draft.subtitle.trim()
        val url = draft.externalUrl.trim()
        val image = draft.imagePath?.trim().orEmpty()
        val body = JSONObject()
        if (draft.id != null) body.put("id", draft.id)
        body.put("page_slug", draft.pageSlug)
        body.put("kind", draft.kind)
        body.put("title", draft.title.trim().ifEmpty { "Untitled" })
        body.put("subtitle", if (sub.isEmpty()) JSONObject.NULL else sub)
        body.put("body_md", draft.bodyMd)
        body.put("image_path", if (image.isEmpty()) JSONObject.NULL else image)
        body.put("external_url", if (url.isEmpty()) JSONObject.NULL else url)
        body.put("deadline", if (deadline.isEmpty()) JSONObject.NULL else deadline)
        body.put("meta", draft.meta)
        body.put("sort_order", draft.sortOrder)
        body.put("featured", draft.featured)
        body.put("published", draft.published)
        body.put("updated_at", nowIso())
        val reply = if (draft.id != null) {
            AdminSupabase.post(
                "free_resource_items?on_conflict=id",
                body.toString(),
                "resolution=merge-duplicates,return=representation"
            )
        } else {
            AdminSupabase.post("free_resource_items", body.toString(), "return=representation")
        }
        if (!reply.ok) return GrantOutcome(false, friendly(reply.errorText()))
        if (countRows(reply.body) == 0) return GrantOutcome(false, "Not saved: no permission to change items")
        return GrantOutcome(true, if (draft.published) "Item published" else "Item saved as draft")
    }

    suspend fun deleteItem(id: String): GrantOutcome {
        val reply = AdminSupabase.delete("free_resource_items?id=eq.${Uri.encode(id)}")
        if (!reply.ok) return GrantOutcome(false, reply.errorText())
        if (countRows(reply.body) == 0) return GrantOutcome(false, "Not deleted: no such item or no permission")
        return GrantOutcome(true, "Deleted")
    }
}
