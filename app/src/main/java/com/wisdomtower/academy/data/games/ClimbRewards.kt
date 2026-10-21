package com.wisdomtower.academy.data.games

/**
 * Tunable Climb constants: XP, stars, pass thresholds, timers.
 * Pure Kotlin — no Compose.
 */
object ClimbRewards {
    const val NORMAL_QUESTION_COUNT = 8
    const val BOSS_QUESTION_COUNT = 12
    const val NORMAL_HEARTS = 3
    const val BOSS_HEARTS = 4
    const val SECONDS_PER_QUESTION = 20
    const val EXTRA_TIME_SECONDS = 10

    /** Minimum accuracy (0..1) to pass a normal floor. */
    const val PASS_ACCURACY_NORMAL = 0.50f
    /** Minimum accuracy to pass a boss floor. */
    const val PASS_ACCURACY_BOSS = 0.60f

    const val STARS_3_ACCURACY = 0.90f
    const val STARS_2_ACCURACY = 0.70f

    const val XP_PER_CORRECT = 10
    const val XP_STREAK_BONUS = 2
    const val XP_STREAK_BONUS_CAP = 10
    const val XP_FIRST_CLEAR = 20
    const val XP_BOSS_MULTIPLIER = 2

    fun starsForAccuracy(accuracy: Float): Int {
        return when {
            accuracy >= STARS_3_ACCURACY -> 3
            accuracy >= STARS_2_ACCURACY -> 2
            else -> 1
        }
    }

    fun xpForCorrect(streakAfterThis: Int, boss: Boolean): Int {
        var xp = XP_PER_CORRECT
        if (streakAfterThis > 1) {
            val bonus = (streakAfterThis - 1) * XP_STREAK_BONUS
            xp += if (bonus > XP_STREAK_BONUS_CAP) XP_STREAK_BONUS_CAP else bonus
        }
        if (boss) xp *= XP_BOSS_MULTIPLIER
        return xp
    }
}
