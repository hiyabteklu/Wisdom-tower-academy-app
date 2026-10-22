package com.wisdomtower.academy.data.games

/**
 * Game levels (docs/GAMES_COOL_PLAN.md slice G1). Pure Kotlin, no UI. MEDIUM is exactly how the games played before levels
 * existed, so every old call keeps working. All numbers are proposals: change them after a phone test.
 */
enum class GameLevel(val id: String, val label: String) {
    EASY("easy", "Easy"),
    MEDIUM("medium", "Medium"),
    HARD("hard", "Hard");

    companion object {
        /** Unknown or missing id = MEDIUM. */
        fun fromId(id: String?): GameLevel = values().firstOrNull { it.id == id?.trim()?.lowercase() } ?: MEDIUM
    }
}

/**
 * Wisdom Defense numbers of one level. The SCREEN passes [hearts] and [maxEnemies] to the engine's existing
 * `maxHearts` / `maxEnemies` parameters (slice G2); everything else is read by the engine itself.
 *
 * @param speedScale multiplies every enemy's walk speed (1.0 = about 25 s far edge to wall for a slime).
 * @param wrongLunge forward lunge of the nearest enemy after a wrong answer.
 * @param hintAfterWrong the item's `hint` is shown after the first wrong answer of a question.
 * @param armoredGolem a golem needs 2 correct answers (the first cracks its armor).
 * @param boss the last wave is one big golem that needs 3 correct answers (only when the set has 8+ questions).
 * @param scoreMultiplier applied to the final score.
 */
data class DefenseTuning(
    val level: GameLevel,
    val hearts: Int,
    val maxEnemies: Int,
    val speedScale: Float,
    val wrongLunge: Float,
    val hintAfterWrong: Boolean,
    val armoredGolem: Boolean,
    val boss: Boolean,
    val scoreMultiplier: Float
) {
    companion object {
        val EASY = DefenseTuning(GameLevel.EASY, 5, 2, 0.73f, 0.05f, true, false, false, 1f)
        val MEDIUM = DefenseTuning(GameLevel.MEDIUM, 3, 4, 1f, 0.08f, false, false, false, 1.5f)
        val HARD = DefenseTuning(GameLevel.HARD, 3, 5, 1.47f, 0.12f, false, true, true, 2f)

        fun forLevel(level: GameLevel): DefenseTuning = when (level) {
            GameLevel.EASY -> EASY
            GameLevel.MEDIUM -> MEDIUM
            GameLevel.HARD -> HARD
        }
    }
}

/**
 * Match numbers of one level (used by `MatchEngine` for [pairsOnBoard]; the rest is read by the screen in slice G3).
 *
 * @param wrongPairHint a wrong pair briefly shows which side was right (Easy).
 * @param parSecondsPerPair par time = pairs x this; null = no timer. A 3rd star on Hard needs moves AND time under par.
 * @param star3Factor 3 stars when moves <= ceil(pairs x this). @param star2Factor 2 stars up to pairs x this.
 */
data class MatchTuning(
    val level: GameLevel,
    val pairsOnBoard: Int,
    val wrongPairHint: Boolean,
    val parSecondsPerPair: Int?,
    val star3Factor: Float,
    val star2Factor: Float
) {
    companion object {
        val EASY = MatchTuning(GameLevel.EASY, 3, true, null, 1.5f, 2f)
        val MEDIUM = MatchTuning(GameLevel.MEDIUM, 4, false, null, 1.25f, 2f)
        val HARD = MatchTuning(GameLevel.HARD, 6, false, 8, 1.25f, 2f)

        fun forLevel(level: GameLevel): MatchTuning = when (level) {
            GameLevel.EASY -> EASY
            GameLevel.MEDIUM -> MEDIUM
            GameLevel.HARD -> HARD
        }
    }
}

/**
 * Optional per-item level tags (plan section 3). A set with NO tags uses every item on every level. With tags: the pool of a
 * level = items tagged for it (untagged = medium); fewer than [minCount] -> top up from the neighbouring levels (medium first).
 * Not used yet: reading the `level` key of an item comes with G2.
 */
object GameLevelPool {
    fun <T> pick(level: GameLevel, items: List<T>, minCount: Int, tagOf: (T) -> String?): List<T> {
        if (items.none { tagOf(it) != null }) return items
        fun levelOf(item: T): GameLevel = GameLevel.fromId(tagOf(item))
        val out = items.filter { levelOf(it) == level }.toMutableList()
        val order = when (level) {
            GameLevel.EASY -> listOf(GameLevel.MEDIUM, GameLevel.HARD)
            GameLevel.MEDIUM -> listOf(GameLevel.EASY, GameLevel.HARD)
            GameLevel.HARD -> listOf(GameLevel.MEDIUM, GameLevel.EASY)
        }
        for (other in order) {
            if (out.size >= minCount) break
            out.addAll(items.filter { levelOf(it) == other }.take(minCount - out.size))
        }
        return out
    }
}
