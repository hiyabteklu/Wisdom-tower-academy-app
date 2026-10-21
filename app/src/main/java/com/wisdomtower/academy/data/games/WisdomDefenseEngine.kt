package com.wisdomtower.academy.data.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Wisdom Defense logic (docs/GAMES_HUB_PLAN.md step 5, feel rules in docs/GAMES_FEEL_PLAN.md slice D1).
 * Pure Kotlin, no Compose. Distance: 0 = far edge, 1 = empire wall.
 * Enemies walk all the time (also while a shot flies).
 */
data class WisdomEnemy(
    val id: Int,
    /** 0 = just appeared far away, 1 = at the empire. */
    var progress: Float,
    val speed: Float,
    /** Look: 0 slime, 1 bat, 2 golem (art arrives in slice D2). */
    val kind: Int = 0,
    /** Smooth push still to apply: negative = pushed back, positive = lunge forward. */
    var shift: Float = 0f
)

/** An enemy that is playing its death animation (not targetable). [t] goes 0..1. */
data class WisdomDyingEnemy(
    val id: Int,
    val progress: Float,
    val kind: Int,
    var age: Float = 0f
) {
    val t: Float get() = (age / WisdomDefenseEngine.DEATH_TIME).coerceIn(0f, 1f)
}

/** Things the screen can react to with effects. Read them with [WisdomDefenseEngine.drainEvents]. */
sealed class WisdomEvent {
    data class Killed(val progress: Float, val kind: Int) : WisdomEvent()
    data class WrongHit(val progress: Float) : WisdomEvent()
    data class WallHit(val kind: Int) : WisdomEvent()
}

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
    companion object {
        /** Seconds for the shot to fly. */
        const val SHOT_TIME = 0.30f
        /** Seconds of the death animation. */
        const val DEATH_TIME = 0.35f
        /** Seconds before another answer can be fired after a wrong one. */
        const val WRONG_COOLDOWN = 0.40f
        /** Push back of the other enemies after a kill. */
        const val KILL_PUSH_BACK = 0.10f
        /** Forward lunge of the nearest enemy after a wrong answer. */
        const val WRONG_LUNGE = 0.08f
        /** Push back of the other enemies after one hit the wall (mercy). */
        const val WALL_MERCY_PUSH = 0.15f
        /** How fast a push is applied (progress per second). */
        const val SHIFT_RATE = 0.30f
        /** Seconds before a new enemy walks in after a kill. */
        const val SPAWN_DELAY_AFTER_KILL = 0.5f
        /** Seconds between two normal spawns. */
        const val SPAWN_GAP = 1.0f
        /** A new enemy only enters when the last one is at least this far in. */
        const val MIN_ENTRY_GAP = 0.18f
        /** A follower may not get closer than this to the enemy in front. */
        const val MIN_FOLLOW_GAP = 0.06f
        private val START_POSITIONS = floatArrayOf(0.45f, 0.27f, 0.09f)
    }

    private val queue = ArrayList(items.filter { it.choices.size == 4 && it.correct in 0..3 })
    private var nextEnemyId = 1
    private var spawnDelay = 0f
    private var cooldown = 0f
    private val eventList = ArrayList<WisdomEvent>()

    /** Enemies that are walking (targetable). */
    var enemies: List<WisdomEnemy> = emptyList()
        private set
    /** Enemies in their death animation, draw them with [WisdomDyingEnemy.t]. */
    var dying: List<WisdomDyingEnemy> = emptyList()
        private set
    var current: GameSetDefenseItem? = null
        private set
    /** Answers already tried wrongly for the current question (greyed out and locked). */
    var lockedChoices: Set<Int> = emptySet()
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

    /** Number of questions not yet answered correctly, including the current one. */
    val questionsLeft: Int get() = queue.size + (if (current != null) 1 else 0)

    init {
        pullQuestion()
        if (current == null) {
            phase = WisdomDefensePhase.Won(0, hearts)
        } else {
            val count = minOf(START_POSITIONS.size, maxEnemies, questionsLeft)
            for (i in 0 until count) spawn(START_POSITIONS[i])
            spawnDelay = SPAWN_GAP
        }
    }

    fun drainEvents(): List<WisdomEvent> {
        if (eventList.isEmpty()) return emptyList()
        val out = ArrayList(eventList)
        eventList.clear()
        return out
    }

    fun tick(dt: Float) {
        if (phase !is WisdomDefensePhase.Playing) return
        val step = dt.coerceIn(0f, 0.1f)

        shotProgress?.let { p ->
            val np = p + step / SHOT_TIME
            if (np >= 1f) finishShot() else shotProgress = np
        }
        if (phase !is WisdomDefensePhase.Playing) return

        if (cooldown > 0f) cooldown = (cooldown - step).coerceAtLeast(0f)

        if (dying.isNotEmpty()) {
            for (d in dying) d.age += step
            dying = dying.filter { it.age < DEATH_TIME }
        }

        // Everybody walks, also while a shot flies.
        val hit = ArrayList<WisdomEnemy>()
        for (e in enemies) {
            var move = e.speed * step
            if (e.shift != 0f) {
                val m = minOf(abs(e.shift), SHIFT_RATE * step)
                val signed = if (e.shift > 0f) m else -m
                e.shift -= signed
                move += signed
            }
            e.progress = (e.progress + move).coerceIn(0f, 1.05f)
        }
        // Followers never run into the enemy in front.
        val sorted = enemies.sortedByDescending { it.progress }
        for (i in 1 until sorted.size) {
            val cap = sorted[i - 1].progress - MIN_FOLLOW_GAP
            if (sorted[i].progress > cap) sorted[i].progress = cap.coerceAtLeast(0f)
        }
        for (e in enemies) if (e.progress >= 1f) hit.add(e)

        if (hit.isNotEmpty()) {
            hearts = (hearts - hit.size).coerceAtLeast(0)
            for (e in hit) {
                dying = dying + WisdomDyingEnemy(e.id, 1f, e.kind)
                addEvent(WisdomEvent.WallHit(e.kind))
            }
            if (hearts <= 0) {
                phase = WisdomDefensePhase.Lost(killed)
                enemies = emptyList()
                return
            }
            val rest = enemies.filter { it.progress < 1f }
            for (e in rest) e.shift -= WALL_MERCY_PUSH
            enemies = rest
        }

        spawnDelay -= step
        if (spawnDelay <= 0f) trySpawn()
        checkWin()
    }

    fun fire(choiceIndex: Int): Boolean {
        if (phase !is WisdomDefensePhase.Playing) return false
        if (shotProgress != null) return false
        if (cooldown > 0f) return false
        if (current == null) return false
        if (enemies.isEmpty()) return false
        if (choiceIndex !in 0..3) return false
        if (choiceIndex in lockedChoices) return false
        lastShotChoice = choiceIndex
        lastShotCorrect = choiceIndex == current!!.correct
        shotProgress = 0f
        return true
    }

    private fun finishShot() {
        val correct = lastShotCorrect == true
        val choice = lastShotChoice
        shotProgress = null
        val nearest = enemies.maxByOrNull { it.progress }
        if (correct) {
            if (nearest != null) {
                enemies = enemies.filter { it.id != nearest.id }
                dying = dying + WisdomDyingEnemy(nearest.id, nearest.progress, nearest.kind)
                killed++
                addEvent(WisdomEvent.Killed(nearest.progress, nearest.kind))
                // Push everyone else back, smoothly.
                for (e in enemies) e.shift -= KILL_PUSH_BACK
            }
            pullQuestion()
            spawnDelay = SPAWN_DELAY_AFTER_KILL
            checkWin()
        } else {
            if (choice != null) lockedChoices = lockedChoices + choice
            if (nearest != null) {
                nearest.shift += WRONG_LUNGE
                addEvent(WisdomEvent.WrongHit(nearest.progress))
            }
            cooldown = WRONG_COOLDOWN
        }
        lastShotCorrect = null
        lastShotChoice = null
    }

    private fun pullQuestion() {
        current = if (queue.isEmpty()) null else queue.removeAt(0)
        lockedChoices = emptySet()
    }

    private fun spawn(progress: Float) {
        val kind = random.nextInt(3)
        val base = when (kind) {
            1 -> 0.050f // bat: fast
            2 -> 0.032f // golem: slow
            else -> 0.040f // slime
        }
        val speed = base + (random.nextFloat() - 0.5f) * 0.008f
        enemies = enemies + WisdomEnemy(nextEnemyId++, progress, speed, kind)
    }

    private fun trySpawn() {
        if (phase !is WisdomDefensePhase.Playing) return
        // Never more enemies than questions left, so every enemy can be killed.
        if (enemies.size >= minOf(maxEnemies, questionsLeft)) return
        // Enter from the far edge only when there is room.
        if (enemies.any { it.progress < MIN_ENTRY_GAP }) return
        spawn(0f)
        spawnDelay = SPAWN_GAP
    }

    private fun addEvent(e: WisdomEvent) {
        if (eventList.size >= 32) eventList.removeAt(0)
        eventList.add(e)
    }

    private fun checkWin() {
        if (phase !is WisdomDefensePhase.Playing) return
        if (current == null && queue.isEmpty() && enemies.isEmpty() && dying.isEmpty()) {
            phase = WisdomDefensePhase.Won(killed, hearts)
        }
    }
}
