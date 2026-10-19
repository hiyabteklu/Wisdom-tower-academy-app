package com.wisdomtower.academy.data.admin

import org.json.JSONArray
import org.json.JSONObject

// ADMIN-native N8b. Kotlin port of website-reference/src/lib/notification-authoring.ts and the shape checks of
// notification-v2.ts. The admin types text with two tiny marks, double star for bold and double equals for highlight
// (they can nest), and this file turns the blocks into the structured content the app and the server read:
// { v: 1, blocks: [ { t: title | subtitle, text }, { t: p | callout, runs: [{ text, b?, hl? }] },
//   { t: list, items: [[runs]] }, { t: divider } ] }.
// The marks are never stored. The server checks the content again (sanitizeContent), this port only builds it.
// STATUS: `[~]` written, NOT compiled, NOT tested on a device.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

/** One block as the admin edits it: text holds the authoring marks (a list block: one item per line). */
class EditorBlock(val id: String, val t: String, val text: String)

class ContentRun(val text: String, val b: Boolean, val hl: Boolean)

sealed class ContentBlock {
    class Title(val text: String) : ContentBlock()
    class Subtitle(val text: String) : ContentBlock()
    class Para(val runs: List<ContentRun>) : ContentBlock()
    class Callout(val runs: List<ContentRun>) : ContentBlock()
    class Bullets(val items: List<List<ContentRun>>) : ContentBlock()
    object Divider : ContentBlock()
}

object NotificationAuthoring {
    /** Block types in the order of the website's add buttons: id to label. */
    val BLOCK_TYPES: List<Pair<String, String>> = listOf(
        "title" to "Title",
        "subtitle" to "Subtitle",
        "p" to "Paragraph",
        "callout" to "Callout",
        "list" to "Bullets",
        "divider" to "Divider"
    )

    private var counter = 0

    fun newId(): String {
        counter += 1
        return "b" + System.currentTimeMillis().toString(36) + counter
    }

    private val MARKS = Regex("\\*\\*([\\s\\S]+?)\\*\\*|==([\\s\\S]+?)==")
    private val SPACES = Regex("\\s+")

    private fun push(piece: String, b: Boolean, hl: Boolean, out: MutableList<ContentRun>) {
        if (piece.isNotEmpty()) out.add(ContentRun(piece, b, hl))
    }

    private fun walk(s: String, b: Boolean, hl: Boolean, out: MutableList<ContentRun>) {
        var last = 0
        for (m in MARKS.findAll(s)) {
            if (m.range.first > last) push(s.substring(last, m.range.first), b, hl, out)
            val bold = m.groups[1]
            val mark = m.groups[2]
            if (bold != null) walk(bold.value, true, hl, out)
            else if (mark != null) walk(mark.value, b, true, out)
            last = m.range.last + 1
        }
        if (last < s.length) push(s.substring(last), b, hl, out)
    }

    fun parseRuns(text: String): List<ContentRun> {
        val out = ArrayList<ContentRun>()
        walk(text, false, false, out)
        return out
    }

    /** Back to authoring marks (used when a waiting notification is opened for editing). */
    fun runsToText(runs: List<ContentRun>): String {
        val sb = StringBuilder()
        for (r in runs) {
            var s = r.text
            if (r.hl) s = "==" + s + "=="
            if (r.b) s = "**" + s + "**"
            sb.append(s)
        }
        return sb.toString()
    }

    /** Editor blocks to content blocks. Empty blocks are dropped (the server refuses empty ones). */
    fun toBlocks(editor: List<EditorBlock>): List<ContentBlock> {
        val out = ArrayList<ContentBlock>()
        for (block in editor) {
            val text = block.text.replace("\r\n", "\n")
            when (block.t) {
                "divider" -> out.add(ContentBlock.Divider)
                "title" -> {
                    val s = text.replace(SPACES, " ").trim()
                    if (s.isNotEmpty()) out.add(ContentBlock.Title(s))
                }
                "subtitle" -> {
                    val s = text.replace(SPACES, " ").trim()
                    if (s.isNotEmpty()) out.add(ContentBlock.Subtitle(s))
                }
                "p" -> {
                    val runs = parseRuns(text.trim())
                    if (runs.isNotEmpty()) out.add(ContentBlock.Para(runs))
                }
                "callout" -> {
                    val runs = parseRuns(text.trim())
                    if (runs.isNotEmpty()) out.add(ContentBlock.Callout(runs))
                }
                else -> {
                    val items = ArrayList<List<ContentRun>>()
                    for (line in text.split("\n")) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        val runs = parseRuns(trimmed)
                        if (runs.isNotEmpty()) items.add(runs)
                    }
                    if (items.isNotEmpty()) out.add(ContentBlock.Bullets(items))
                }
            }
        }
        return out
    }

    private fun runsJson(runs: List<ContentRun>): JSONArray {
        val arr = JSONArray()
        for (r in runs) {
            val o = JSONObject().put("text", r.text)
            if (r.b) o.put("b", true)
            if (r.hl) o.put("hl", true)
            arr.put(o)
        }
        return arr
    }

    private fun blockJson(block: ContentBlock): JSONObject = when (block) {
        is ContentBlock.Title -> JSONObject().put("t", "title").put("text", block.text)
        is ContentBlock.Subtitle -> JSONObject().put("t", "subtitle").put("text", block.text)
        is ContentBlock.Para -> JSONObject().put("t", "p").put("runs", runsJson(block.runs))
        is ContentBlock.Callout -> JSONObject().put("t", "callout").put("runs", runsJson(block.runs))
        is ContentBlock.Bullets -> {
            val items = JSONArray()
            for (item in block.items) items.put(runsJson(item))
            JSONObject().put("t", "list").put("items", items)
        }
        is ContentBlock.Divider -> JSONObject().put("t", "divider")
    }

    /** The value to send as content, or null when there is nothing to send (no text at all). */
    fun toJson(blocks: List<ContentBlock>): JSONObject? {
        val hasText = blocks.any { it !is ContentBlock.Divider }
        if (!hasText) return null
        val arr = JSONArray()
        for (b in blocks) arr.put(blockJson(b))
        return JSONObject().put("v", 1).put("blocks", arr)
    }

    private fun runsText(runs: List<ContentRun>): String {
        val sb = StringBuilder()
        for (r in runs) sb.append(r.text)
        return sb.toString()
    }

    /** Short plain preview (lock screen, push). Skips title blocks: the title is shown on its own. Same rule as the website. */
    fun preview(blocks: List<ContentBlock>, max: Int = 160): String {
        val parts = ArrayList<String>()
        for (block in blocks) {
            when (block) {
                is ContentBlock.Subtitle -> parts.add(block.text)
                is ContentBlock.Para -> parts.add(runsText(block.runs))
                is ContentBlock.Callout -> parts.add(runsText(block.runs))
                is ContentBlock.Bullets -> for (item in block.items) parts.add(runsText(item))
                else -> {}
            }
        }
        val flat = parts.joinToString(" ").replace(SPACES, " ").trim()
        return if (flat.length <= max) flat else flat.take(max - 1).trimEnd() + "\u2026"
    }

    private fun readRuns(arr: JSONArray?): List<ContentRun> {
        val out = ArrayList<ContentRun>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val text = o.optString("text", "")
            if (text.isNotEmpty()) out.add(ContentRun(text, o.optBoolean("b", false), o.optBoolean("hl", false)))
        }
        return out
    }

    /** Existing content (JSON text) to editor blocks, to edit a waiting notification. Null when it cannot be read. */
    fun fromJson(json: String): List<EditorBlock>? {
        return try {
            val root = JSONObject(json)
            if (root.optInt("v", 0) != 1) return null
            val arr = root.optJSONArray("blocks") ?: return null
            val out = ArrayList<EditorBlock>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val type = o.optString("t", "")
                when (type) {
                    "divider" -> out.add(EditorBlock(newId(), "divider", ""))
                    "title", "subtitle" -> out.add(EditorBlock(newId(), type, o.optString("text", "")))
                    "list" -> {
                        val items = o.optJSONArray("items") ?: JSONArray()
                        val lines = ArrayList<String>()
                        for (j in 0 until items.length()) lines.add(runsToText(readRuns(items.optJSONArray(j))))
                        out.add(EditorBlock(newId(), "list", lines.joinToString("\n")))
                    }
                    "p", "callout" -> out.add(EditorBlock(newId(), type, runsToText(readRuns(o.optJSONArray("runs")))))
                    else -> return null
                }
            }
            out
        } catch (e: Exception) {
            null
        }
    }
}
