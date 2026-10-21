package com.wisdomtower.academy.data.games

import kotlin.random.Random

data class DefenseEnemy(
    val id: Int,
    val questionIndex: Int,
    val kind: DefenseEnemyKind,
    var position: Float,
    val marchSeconds: Float,
    var hitsRemaining: Int,
    var alive: Boolean = true
)

sealed class DefenseEvent {
    data class Spawned(val enemyId: Int, val kind: DefenseEnemyKind) : DefenseEvent()
    data class Hit(val enemyId: Int, val cracked: Boolean) : DefenseEvent()
    data class Killed(val enemyId: Int, val scoreGained: Int) : DefenseEvent()
    data class CitadelDamaged(val amount: Int, val healthLeft: Int) : DefenseEvent()
    data class PowerUpUsed(val type: DefensePowerUp) : DefenseEvent()
    data class WaveCleared(val wave: Int) : DefenseEvent()
    object Victory : DefenseEvent()
    object Defeat : DefenseEvent()
}

sealed class DefensePhase {
    object Countdown : DefensePhase()
    object Marching : DefensePhase()
    data class Reveal(val wasCorrect: Boolean, val questionIndex: Int) : DefensePhase()
    object WaveBreak : DefensePhase()
    object Victory : DefensePhase()
    object Defeat : DefensePhase()
}

class DefenseEngine(
    private val questions: List<GameQuestion>,
    private val mode: DefenseMode,
    private val random: Random = Random.Default
) {
    private val shuffled: List<GameQuestion>
    private var nextQuestionCursor = 0

    var phase: DefensePhase = DefensePhase.Countdown
        private set
    var wave: Int = 1
        private set
    var citadelHealth: Int = DefenseTuning.CITADEL_HEALTH
        private set
    var score: Int = 0
        private set
    var streak: Int = 0
        private set
    var bestCombo: Int = 0
        private set
    var wavesCleared: Int = 0
        private set
    var correctCount: Int = 0
        private set
    var answeredCount: Int = 0
        private set

    var chronosCharges: Int = DefenseTuning.START_CHRONOS
        private set
    var logicCharges: Int = DefenseTuning.START_LOGIC
        private set
    var fortifyCharges: Int = 0
        private set
    var deflectCharges: Int = 0
        private set

    var freezeLeft: Float = 0f
        private set
    private var fortifyEarned = false
    private var deflectEarned = false

    private var enemyIdSeq = 0
    private var lead: DefenseEnemy? = null
    private val queue = ArrayList<DefenseEnemy>()
    private var waveQuestionIndices = ArrayList<Int>()
    private var waveCursor = 0

    private val events = ArrayList<DefenseEvent>()
    private val missed = ArrayList<Int>()
    private val hiddenChoices = ArrayList<Int>()

    private var countdownLeft = 3f
    private var accumulator = 0f

    val totalQuestions: Int get() = shuffled.size
    val remainingInRun: Int get() = (shuffled.size - nextQuestionCursor).coerceAtLeast(0)
    val countdownSeconds: Float get() = countdownLeft

    init {
        require(questions.isNotEmpty()) { "Defense needs at least one question" }
        shuffled = when (mode) {
            DefenseMode.ExamRun -> questions.map { GameQuestionParser.shuffled(it, random) }
            DefenseMode.Endless -> questions.shuffled(random).map { GameQuestionParser.shuffled(it, random) }
        }
        planWave()
    }

    fun currentQuestion(): GameQuestion? {
        val lead = lead ?: return null
        if (lead.questionIndex !in shuffled.indices) return null
        return shuffled[lead.questionIndex]
    }

    fun leadEnemy(): DefenseEnemy? = lead
    fun silhouettes(): List<DefenseEnemy> = queue.toList()
    fun hiddenChoiceIndices(): List<Int> = hiddenChoices.toList()
    fun missedIndices(): List<Int> = missed.toList()
    fun accuracy(): Float {
        if (answeredCount == 0) return 0f
        return correctCount.toFloat() / answeredCount.toFloat()
    }

    fun consumeEvents(): List<DefenseEvent> {
        val copy = events.toList()
        events.clear()
        return copy
    }

    fun startCountdown() {
        phase = DefensePhase.Countdown
        countdownLeft = 3f
    }

    fun tick(dt: Float): Boolean {
        if (phase is DefensePhase.Victory || phase is DefensePhase.Defeat) return false
        if (phase is DefensePhase.Reveal || phase is DefensePhase.WaveBreak) return true
        accumulator += dt
        while (accumulator >= DefenseTuning.FIXED_DT) {
            accumulator -= DefenseTuning.FIXED_DT
            step(DefenseTuning.FIXED_DT)
            if (phase is DefensePhase.Victory || phase is DefensePhase.Defeat) return false
            if (phase is DefensePhase.Reveal) break
        }
        return true
    }

    private fun step(dt: Float) {
        when (phase) {
            is DefensePhase.Countdown -> {
                countdownLeft -= dt
                if (countdownLeft <= 0f) {
                    phase = DefensePhase.Marching
                    spawnLeadIfNeeded()
                }
            }
            is DefensePhase.Marching -> {
                if (freezeLeft > 0f) {
                    freezeLeft = (freezeLeft - dt).coerceAtLeast(0f)
                    return
                }
                val enemy = lead ?: run {
                    spawnLeadIfNeeded()
                    return
                }
                val speed = DefenseTuning.LANE_LENGTH / enemy.marchSeconds * DefenseTuning.speedMult(enemy.kind)
                enemy.position = (enemy.position - speed * dt).coerceAtLeast(0f)
                if (enemy.position <= 0f) {
                    applyTimeout()
                }
            }
            else -> {}
        }
    }

    fun pick(choiceIndex: Int) {
        if (phase !is DefensePhase.Marching) return
        val enemy = lead ?: return
        val q = currentQuestion() ?: return
        if (choiceIndex in hiddenChoices) return
        val correct = choiceIndex == q.correctIndex
        answeredCount++
        if (correct) {
            correctCount++
            streak++
            if (streak > bestCombo) bestCombo = streak
            maybeEarnPowerUps()
            val cracked = enemy.kind == DefenseEnemyKind.Armored && enemy.hitsRemaining > 1
            if (cracked) {
                enemy.hitsRemaining--
                enemy.position = (enemy.position + DefenseTuning.ARMORED_KNOCKBACK).coerceAtMost(1f)
                events.add(DefenseEvent.Hit(enemy.id, cracked = true))
                advanceQuestionForArmored(enemy)
            } else {
                val timeFrac = enemy.position
                val gained = DefenseTuning.scoreForKill(streak, timeFrac)
                score += gained
                enemy.alive = false
                events.add(DefenseEvent.Hit(enemy.id, cracked = false))
                events.add(DefenseEvent.Killed(enemy.id, gained))
                lead = null
                hiddenChoices.clear()
                onEnemyCleared()
            }
        } else {
            streak = 0
            fortifyEarned = false
            deflectEarned = false
            missed.add(enemy.questionIndex)
            phase = DefensePhase.Reveal(false, enemy.questionIndex)
        }
    }

    fun acknowledgeReveal() {
        val rev = phase as? DefensePhase.Reveal ?: return
        if (!rev.wasCorrect) {
            val enemy = lead
            if (enemy != null) {
                val dmg = DefenseTuning.citadelDamage(enemy.kind)
                citadelHealth = (citadelHealth - dmg).coerceAtLeast(0)
                events.add(DefenseEvent.CitadelDamaged(dmg, citadelHealth))
                enemy.alive = false
                lead = null
                if (citadelHealth <= 0) {
                    phase = DefensePhase.Defeat
                    events.add(DefenseEvent.Defeat)
                    return
                }
            }
            hiddenChoices.clear()
            onEnemyCleared()
        } else {
            phase = DefensePhase.Marching
            spawnLeadIfNeeded()
        }
    }

    fun usePowerUp(type: DefensePowerUp): Boolean {
        when (type) {
            DefensePowerUp.ChronosStasis -> {
                if (phase !is DefensePhase.Marching) return false
                if (chronosCharges <= 0) return false
                chronosCharges--
                freezeLeft = DefenseTuning.CHRONOS_FREEZE_SECONDS
                events.add(DefenseEvent.PowerUpUsed(type))
                return true
            }
            DefensePowerUp.LogicFilter -> {
                if (phase !is DefensePhase.Marching) return false
                if (logicCharges <= 0) return false
                val q = currentQuestion() ?: return false
                if (hiddenChoices.isNotEmpty()) return false
                val wrong = q.choices.indices.filter { it != q.correctIndex }.shuffled(random)
                if (wrong.size < 2) return false
                hiddenChoices.clear()
                hiddenChoices.addAll(wrong.take(2))
                logicCharges--
                events.add(DefenseEvent.PowerUpUsed(type))
                return true
            }
            DefensePowerUp.FortifyCore -> {
                if (fortifyCharges <= 0) return false
                val cap = DefenseTuning.CITADEL_HEALTH + DefenseTuning.CITADEL_HEALTH_CAP_EXTRA
                if (citadelHealth >= cap) return false
                fortifyCharges--
                citadelHealth++
                events.add(DefenseEvent.PowerUpUsed(type))
                return true
            }
            DefensePowerUp.TacticalDeflection -> {
                if (phase !is DefensePhase.Marching) return false
                if (deflectCharges <= 0) return false
                val enemy = lead ?: return false
                deflectCharges--
                missed.add(enemy.questionIndex)
                enemy.alive = false
                lead = null
                hiddenChoices.clear()
                events.add(DefenseEvent.PowerUpUsed(type))
                onEnemyCleared()
                return true
            }
        }
    }

    private fun applyTimeout() {
        val enemy = lead ?: return
        answeredCount++
        streak = 0
        fortifyEarned = false
        deflectEarned = false
        missed.add(enemy.questionIndex)
        phase = DefensePhase.Reveal(false, enemy.questionIndex)
    }

    private fun onEnemyCleared() {
        waveCursor++
        if (waveCursor >= waveQuestionIndices.size) {
            wavesCleared++
            events.add(DefenseEvent.WaveCleared(wave))
            if (mode == DefenseMode.ExamRun && nextQuestionCursor >= shuffled.size) {
                phase = DefensePhase.Victory
                events.add(DefenseEvent.Victory)
                return
            }
            if (mode == DefenseMode.Endless && nextQuestionCursor >= shuffled.size) {
                nextQuestionCursor = 0
            }
            wave++
            planWave()
            phase = DefensePhase.Marching
            spawnLeadIfNeeded()
        } else {
            phase = DefensePhase.Marching
            spawnLeadIfNeeded()
        }
    }

    private fun planWave() {
        waveQuestionIndices.clear()
        waveCursor = 0
        val count = DefenseTuning.QUESTIONS_PER_WAVE
        for (i in 0 until count) {
            if (mode == DefenseMode.ExamRun && nextQuestionCursor >= shuffled.size) break
            if (mode == DefenseMode.Endless && nextQuestionCursor >= shuffled.size) {
                nextQuestionCursor = 0
            }
            waveQuestionIndices.add(nextQuestionCursor)
            nextQuestionCursor++
        }
        if (waveQuestionIndices.isEmpty() && mode == DefenseMode.ExamRun) {
            phase = DefensePhase.Victory
            events.add(DefenseEvent.Victory)
        }
    }

    private fun spawnLeadIfNeeded() {
        if (lead != null) return
        if (waveCursor >= waveQuestionIndices.size) {
            onEnemyCleared()
            return
        }
        val qIndex = waveQuestionIndices[waveCursor]
        val kind = DefenseTuning.enemyKind(wave, waveCursor, waveQuestionIndices.size)
        val seconds = DefenseTuning.secondsForWave(wave)
        val hits = if (kind == DefenseEnemyKind.Armored) DefenseTuning.ARMORED_HITS_NEEDED else 1
        enemyIdSeq++
        lead = DefenseEnemy(
            id = enemyIdSeq,
            questionIndex = qIndex,
            kind = kind,
            position = 1f,
            marchSeconds = seconds,
            hitsRemaining = hits
        )
        hiddenChoices.clear()
        events.add(DefenseEvent.Spawned(enemyIdSeq, kind))
        rebuildSilhouettes()
    }

    private fun rebuildSilhouettes() {
        queue.clear()
        val remaining = waveQuestionIndices.size - waveCursor - 1
        val show = remaining.coerceAtMost(DefenseTuning.SILHOUETTE_MAX)
        for (i in 0 until show) {
            val idx = waveCursor + 1 + i
            if (idx >= waveQuestionIndices.size) break
            val qIndex = waveQuestionIndices[idx]
            val kind = DefenseTuning.enemyKind(wave, idx, waveQuestionIndices.size)
            enemyIdSeq++
            queue.add(
                DefenseEnemy(
                    id = enemyIdSeq,
                    questionIndex = qIndex,
                    kind = kind,
                    position = 1f + (i + 1) * 0.08f,
                    marchSeconds = DefenseTuning.secondsForWave(wave),
                    hitsRemaining = if (kind == DefenseEnemyKind.Armored) 2 else 1
                )
            )
        }
    }

    private fun advanceQuestionForArmored(enemy: DefenseEnemy) {
        if (nextQuestionCursor < shuffled.size) {
            val newIdx = nextQuestionCursor
            nextQuestionCursor++
            lead = enemy.copy(questionIndex = newIdx.coerceAtMost(shuffled.lastIndex))
        }
        hiddenChoices.clear()
    }

    private fun maybeEarnPowerUps() {
        if (!fortifyEarned && streak >= DefenseTuning.STREAK_FOR_FORTIFY) {
            fortifyEarned = true
            if (fortifyCharges < DefenseTuning.MAX_POWERUP_STACK) fortifyCharges++
        }
        if (!deflectEarned && streak >= DefenseTuning.STREAK_FOR_DEFLECT) {
            deflectEarned = true
            if (deflectCharges < DefenseTuning.MAX_POWERUP_STACK) deflectCharges++
        }
    }
}
