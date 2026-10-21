package com.wisdomtower.academy.data.games

import kotlin.random.Random

/**
 * Wisdom Defense logic (docs/GAMES_HUB_PLAN.md §1a, step 5). Pure Kotlin — no Compose.
 * Enemies approach from different distances; correct answer on the nearest kills only that enemy.
 */
data class DefenseEnemy(
    val id: Int,
    /** 0 = just appeared far away, 1 = at the empire. */
    var progress: Float,
    val speed: Float
)

sealed class WisdomDefensePhase {
    object Playing : WisdomDefensePhase()
    data class Won(val killed: Int, val heartsLeft: Int) : WisdomDefensePhase()
    data class Lost(val killed: Int) : WisdomDefensePhase()
}

class WisdomDefenseEngine(
    items: List<GameSetDefenseItem>,
    private val random: Random = Random.Default,
    val maxHearts: Int = 3,
    private val maxEnemies: Int = 4
) {
    private val queue = ArrayList(items.filter { it.choices.size == 4 && it.correct in 0..3 })
    private var nextEnemyId = 1

    var enemies: List<DefenseEnemy> = emptyList()
        private set
    var current: GameSetDefenseItem? = null
        private set
    var hearts: Int = maxHearts
        private set
    var killed: Int = 0
        private set
    var phase: WisdomDefensePhase = WisdomDefensePhase.Playing
        private set
    var shotProgress: Float? = null
        private set
    var lastShotCorrect: Boolean? = null
        private set
    var lastShotChoice: Int? = null
        private set

    init {
        pullQuestion()
        ensureEnemies()
        if (current == null) {
            phase = WisdomDefensePhase.Won(0, hearts)
        }
    }

    fun tick(dt: Float) {
        if (phase !is WisdomDefensePhase.Playing) return

        shotProgress?.let { p ->
            val np = p + dt * 3.2f
            if (np >= 1f) {
                finishShot()
            } else {
                shotProgress = np
            }
        }

        if (shotProgress != null) return

        val alive = ArrayList<DefenseEnemy>()
        for (e in enemies) {
            e.progress = (e.progress + e.speed * dt).coerceAtMost(1.05f)
            if (e.progress >= 1f) {
                hearts -= 1
                if (hearts <= 0) {
                    phase = WisdomDefensePhase.Lost(killed)
                    enemies = emptyList()
                    return
                }
            } else {
                alive.add(e)
            }
        }
        enemies = alive
        ensureEnemies()
        checkWin()
    }

    fun fire(choiceIndex: Int): Boolean {
        if (phase !is WisdomDefensePhase.Playing) return false
        if (shotProgress != null) return false
        if (current == null) return false
        if (enemies.isEmpty()) return false
        if (choiceIndex !in 0..3) return false
        lastShotChoice = choiceIndex
        lastShotCorrect = choiceIndex == current!!.correct
        shotProgress = 0f
        return true
    }

    private fun finishShot() {
        val correct = lastShotCorrect == true
        shotProgress = null
        if (correct) {
            val nearest = enemies.maxByOrNull { it.progress }
            if (nearest != null) {
                enemies = enemies.filter { it.id != nearest.id }
                killed++
            }
            pullQuestion()
            ensureEnemies()
            checkWin()
        }
        lastShotCorrect = null
        lastShotChoice = null
    }

    private fun pullQuestion() {
        current = if (queue.isEmpty()) null else queue.removeAt(0)
    }

    private fun ensureEnemies() {
        if (phase !is WisdomDefensePhase.Playing) return
        if (current == null && queue.isEmpty()) return
        while (enemies.size < maxEnemies && (current != null || queue.isNotEmpty())) {
            val start = 0.05f + random.nextFloat() * 0.45f
            val speed = 0.04f + random.nextFloat() * 0.05f
            enemies = enemies + DefenseEnemy(nextEnemyId++, start, speed)
        }
    }

    private fun checkWin() {
        if (phase !is WisdomDefensePhase.Playing) return
        if (current == null && queue.isEmpty() && enemies.isEmpty()) {
            phase = WisdomDefensePhase.Won(killed, hearts)
        }
    }
}
