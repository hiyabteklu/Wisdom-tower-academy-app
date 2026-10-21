package com.wisdomtower.academy.data.games

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * One quiz/exam question for Tower Climb or Tower Defense.
 * [id] is stable: "<bankId>:<index>" so Attic and progress can key by it.
 * [prompt], [choices], [solution] stay as rich text (math, highlights) — never stripped.
 */
data class GameQuestion(
    val id: String,
    val bankId: String,
    val bankTitle: String,
    val chapter: Int?,
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    val solution: String,
    val keepOrder: Boolean
)

/**
 * Parse questions from a bank/exam [metaJson]. Skips blank prompts, fewer than 2 choices,
 * or correct index out of range. Returns the valid list and the skip count.
 *
 * Fair shuffle: [shuffled] permutes choices and remaps [correctIndex] so the right letter
 * is spread across A–D over many questions (owner bug: ~80% sat at A when order was fixed).
 * Do NOT shuffle when any choice matches "all/none/both/neither of the above" style text.
 */
object GameQuestionParser {

    private val KEEP_ORDER_REGEX = Regex(
        """(?i)^(all|none|both|neither)\s+(of\s+)?(the\s+)?(above|these|them)$|^(both)\s+[a-d]\s+and\s+[a-d]$"""
    )

    fun parse(
        metaJson: String,
        bankId: String,
        bankTitle: String,
        chapter: Int?
    ): Pair<List<GameQuestion>, Int> {
        val root = try {
            JSONObject(metaJson)
        } catch (_: Exception) {
            return emptyList<GameQuestion>() to 0
        }
        val arr: JSONArray = root.optJSONArray("questions") ?: return emptyList<GameQuestion>() to 0
        val out = ArrayList<GameQuestion>(arr.length())
        var skipped = 0
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: run {
                skipped++
                continue
            }
            val prompt = obj.optString("prompt", "").trim()
            if (prompt.isEmpty()) {
                skipped++
                continue
            }
            val choicesArr = obj.optJSONArray("choices")
            if (choicesArr == null || choicesArr.length() < 2) {
                skipped++
                continue
            }
            val choices = ArrayList<String>(choicesArr.length())
            for (c in 0 until choicesArr.length()) {
                choices.add(choicesArr.optString(c, ""))
            }
            val correct = obj.optInt("correct", -1)
            if (correct < 0 || correct >= choices.size) {
                skipped++
                continue
            }
            val solution = obj.optString("solution", "")
            val keepOrder = choices.any { KEEP_ORDER_REGEX.matches(it.trim()) }
            out.add(
                GameQuestion(
                    id = "$bankId:$i",
                    bankId = bankId,
                    bankTitle = bankTitle,
                    chapter = chapter,
                    prompt = prompt,
                    choices = choices,
                    correctIndex = correct,
                    solution = solution,
                    keepOrder = keepOrder
                )
            )
        }
        return out to skipped
    }

    /**
     * Permute choices and remap correctIndex.
     * Example: correct=0, perm [2,0,1,3] → new correctIndex = 1 (where 0 landed).
     * When [keepOrder] is true, returns [q] unchanged.
     */
    fun shuffled(q: GameQuestion, random: Random): GameQuestion {
        if (q.keepOrder || q.choices.size < 2) return q
        val n = q.choices.size
        val order = (0 until n).toMutableList()
        for (i in n - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = order[i]
            order[i] = order[j]
            order[j] = tmp
        }
        val newChoices = ArrayList<String>(n)
        var newCorrect = 0
        for (newIdx in 0 until n) {
            val oldIdx = order[newIdx]
            newChoices.add(q.choices[oldIdx])
            if (oldIdx == q.correctIndex) newCorrect = newIdx
        }
        return q.copy(choices = newChoices, correctIndex = newCorrect)
    }
}
