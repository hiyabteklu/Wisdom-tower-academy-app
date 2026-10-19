package com.wisdomtower.academy.data.admin

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row of the `catalog_items` table (website-reference/src/lib/catalog.ts, type CatalogRow). */
data class CatalogItem(
    val id: String,
    val name: String,
    val shortName: String,
    val description: String,
    val priceEtb: Double,
    val href: String,
    val image: String,
    val includes: List<String>,
    val enrolledLabel: String,
    val group: String,
    val active: Boolean,
    val sortOrder: Int
)

sealed class CatalogResult {
    class Success(val items: List<CatalogItem>) : CatalogResult()
    class Failure(val message: String) : CatalogResult()
}

/**
 * ADMIN-native N6c (Pricing and Catalog). Same data and steps as the website CatalogPanel and lib/catalog.ts
 * (listAllCatalogForAdmin, upsertCatalogItem, deleteCatalogItem), with the admin's own login. Plain Supabase REST,
 * no API route, no secret.
 *  - list: every row of catalog_items (active or not), ordered by sort_order
 *  - save: upsert on id; the id is lower-cased and every character outside a-z 0-9 dash underscore becomes a dash
 *  - delete: delete the row by id
 * NOT carried over: Seed from static (needs the website's full static texts; use the website button for it).
 * Difference, on purpose: delete reports an error when no row was deleted (a silent refusal).
 * IMPORTANT (read in catalog.ts, function applyStaticPrice): for the packages defined in the website code, the
 * price, includes and description that students see come from the website code, not from these rows.
 * 2026-10-19: the missing-table message no longer tells admins to run the old setup script on the live database.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object CatalogRepository {
    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    suspend fun list(): CatalogResult {
        val reply = AdminSupabase.get("catalog_items?select=*&order=sort_order.asc")
        if (!reply.ok) {
            val msg = reply.errorText()
            val missingTable = msg.contains("relation") || msg.contains("does not exist")
            return CatalogResult.Failure(
                if (missingTable) "Table missing: tell the database owner. Do not run the old setup scripts." else msg
            )
        }
        return try {
            val arr = JSONArray(reply.body)
            val out = ArrayList<CatalogItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val includes = ArrayList<String>()
                val incArr = o.optJSONArray("includes")
                if (incArr != null) {
                    for (k in 0 until incArr.length()) {
                        val line = incArr.optString(k, "")
                        if (line.isNotEmpty()) includes.add(line)
                    }
                }
                val price = o.optDouble("price_etb", 0.0)
                out.add(
                    CatalogItem(
                        id = o.text("id") ?: continue,
                        name = o.text("name") ?: "",
                        shortName = o.text("short_name") ?: "",
                        description = o.text("description") ?: "",
                        priceEtb = if (price.isNaN()) 0.0 else price,
                        href = o.text("href") ?: "",
                        image = o.text("image") ?: "",
                        includes = includes,
                        enrolledLabel = o.text("enrolled_label") ?: "",
                        group = o.text("group_key") ?: "custom",
                        active = o.optBoolean("active", true),
                        sortOrder = o.optInt("sort_order", 0)
                    )
                )
            }
            CatalogResult.Success(out)
        } catch (e: Exception) {
            CatalogResult.Failure("Unexpected reply from the server")
        }
    }

    suspend fun save(input: CatalogItem, isNew: Boolean): GrantOutcome {
        val id = input.id.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "-")
        if (id.isEmpty() || input.name.trim().isEmpty()) return GrantOutcome(false, "ID and name are required")
        val name = input.name.trim()
        val includes = JSONArray()
        for (line in input.includes) {
            val t = line.trim()
            if (t.isNotEmpty()) includes.put(t)
        }
        val body = JSONObject()
        body.put("id", id)
        body.put("name", name)
        body.put("short_name", input.shortName.trim().ifEmpty { name })
        body.put("description", input.description.trim())
        body.put("price_etb", input.priceEtb)
        body.put("href", input.href.trim().ifEmpty { "/packages" })
        body.put("image", input.image.trim())
        body.put("includes", includes)
        body.put("enrolled_label", input.enrolledLabel.trim())
        body.put("group_key", input.group)
        body.put("active", input.active)
        body.put("sort_order", input.sortOrder)
        body.put("updated_at", nowIso())
        val reply = AdminSupabase.post(
            "catalog_items?on_conflict=id",
            body.toString(),
            "resolution=merge-duplicates,return=minimal"
        )
        if (!reply.ok) return GrantOutcome(false, reply.errorText())
        return GrantOutcome(true, if (isNew) "Package created" else "Package updated")
    }

    suspend fun delete(id: String): GrantOutcome {
        val reply = AdminSupabase.delete("catalog_items?id=eq.${Uri.encode(id)}")
        if (!reply.ok) return GrantOutcome(false, reply.errorText())
        val removed = try {
            JSONArray(reply.body).length()
        } catch (e: Exception) {
            0
        }
        if (removed == 0) return GrantOutcome(false, "Not deleted: no such package or no permission")
        return GrantOutcome(true, "Deleted")
    }
}
