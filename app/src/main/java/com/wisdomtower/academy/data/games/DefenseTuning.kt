package com.wisdomtower.academy.data.games

/**
 * All Defense numbers in one place. Pure Kotlin — no Compose.
 */
object DefenseTuning {
    const val CITADEL_HEALTH = 5
    const val CITADEL_HEALTH_CAP_EXTRA = 2

    const val BASE_SECONDS = 25f
    const val SECONDS_SHRINK_PER_WAVE = 1.2f
    const val MIN_SECONDS = 10f

    const val SCOUT_SPEED_MULT = 1.25f
    const val ARMORED_SPEED_MULT = 0.75f
    const val STANDARD_SPEED_MULT = 1f

    const val QUESTIONS_PER_WAVE = 5
    const val ARMORED_HITS_NEEDED = 2
    const val ARMORED_KNOCKBACK = 0.35f

    const val SCORE_BASE = 100
    const val SCORE_TIME_BONUS_MAX = 50

    const val COMBO_X2_AT = 3
    const val COMBO_X3_AT = 5
    const val COMBO_X4_AT = 8

    const val CHRONOS_FREEZE_SECONDS = 12f
    const val START_CHRONOS = 1
    const val START_LOGIC = 1
    const val MAX_POWERUP_STACK = 2
    const val STREAK_FOR_FORTIFY = 6
    const val STREAK_FOR_DEFLECT = 10

    const val FIXED_DT = 1f / 60f
    const val LANE_LENGTH = 1f

    const val SILHOUETTE_MAX = 6

    fun secondsForWave(wave: Int): Float {
        val w = if (wave < 1) 1 else wave
        val s = BASE_SECONDS - (w - 1) * SECONDS_SHRINK_PER_WAVE
        return if (s < MIN_SECONDS) MIN_SECONDS else s
    }

    fun comboMultiplier(streak: Int): Int {
        return when {
            streak >= COMBO_X4_AT -> 4
            streak >= COMBO_X3_AT -> 3
            streak >= COMBO_X2_AT -> 2
            else -> 1
        }
    }

    fun scoreForKill(streakAfter: Int, timeLeftFrac: Float): Int {
        val mult = comboMultiplier(streakAfter)
        val timeBonus = (SCORE_TIME_BONUS_MAX * timeLeftFrac.coerceIn(0f, 1f)).toInt()
        return SCORE_BASE * mult + timeBonus
    }

    fun enemyKind(wave: Int, indexInWave: Int, questionsInWave: Int): DefenseEnemyKind {
        val isLast = indexInWave == questionsInWave - 1
        if (wave > 0 && wave % 3 == 0 && isLast) return DefenseEnemyKind.Armored
        if (wave >= 2 && indexInWave % 3 == 0) return DefenseEnemyKind.Scout
        return DefenseEnemyKind.Standard
    }

    fun speedMult(kind: DefenseEnemyKind): Float {
        return when (kind) {
            DefenseEnemyKind.Scout -> SCOUT_SPEED_MULT
            DefenseEnemyKind.Armored -> ARMORED_SPEED_MULT
            DefenseEnemyKind.Standard -> STANDARD_SPEED_MULT
        }
    }

    fun citadelDamage(kind: DefenseEnemyKind): Int {
        return when (kind) {
            DefenseEnemyKind.Armored -> 2
            else -> 1
        }
    }
}

enum class DefenseEnemyKind {
    Scout,
    Standard,
    Armored
}

enum class DefensePowerUp {
    ChronosStasis,
    LogicFilter,
    FortifyCore,
    TacticalDeflection
}

enum class DefenseMode {
    ExamRun,
    Endless
}
