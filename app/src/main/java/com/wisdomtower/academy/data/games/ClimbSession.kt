package com.wisdomtower.academy.data.games

import kotlin.random.Random

/**
 * One floor run: state machine for Tower Climb.
 * Pure Kotlin — no Compose. Inject [random] for tests.
 */
enum class ClimbPowerUp {
    FiftyFifty,
    ExtraTime
}

sealed class ClimbSessionState {
    object Loading : ClimbSessionState()
    data class Question(val index: Int, val timeLeft: Float) : ClimbSessionState()
    data class Reveal(val wasCorrect: Boolean, val index: Int) : ClimbSessionState()
    data class FloorComplete(
        val stars: Int,
        val accuracy: Float,
        val xpGained: Int,
        val correct: Int,
        val total: Int,
        val firstClear: Boolean
    ) : ClimbSessionState()
    object Failed : ClimbSessionState()
}

class ClimbSession(
    private val floor: ClimbFloor,
    private val pool: List<GameQuestion>,
    private val atticIds: Set<String>,
    private val alreadyCleared: Boolean,
    private val random: Random = Random.Default
) {
    val isBoss: Boolean = floor.isBoss
    val totalQuestions: Int
    val maxHearts: Int = if (isBoss) ClimbRewards.BOSS_HEARTS else ClimbRewards.NORMAL_HEARTS

    private val runQuestions: List<GameQuestion>
    private val answers = ArrayList<Boolean>()
    private var hearts: Int
    private var streak: Int = 0
    private var bestStreak: Int = 0
    private var xp: Int = 0
    private var fiftyFiftyLeft: Int
    private var extraTimeLeft: Int
    private var hiddenForCurrent: List<Int> = emptyList()
    private var timeLeft: Float = ClimbRewards.SECONDS_PER_QUESTION.toFloat()
    private var index: Int = 0

    var state: ClimbSessionState = ClimbSessionState.Loading
        private set

    init {
        val target = if (isBoss) ClimbRewards.BOSS_QUESTION_COUNT else ClimbRewards.NORMAL_QUESTION_COUNT
        runQuestions = drawQuestions(pool, atticIds, target, random)
        totalQuestions = runQuestions.size
        hearts = maxHearts
        fiftyFiftyLeft = if (isBoss) 0 else 1
        extraTimeLeft = if (isBoss) 0 else 1
        if (totalQuestions == 0) {
            state = ClimbSessionState.Failed
        }
    }

    fun currentQuestion(): GameQuestion? {
        if (index !in runQuestions.indices) return null
        return runQuestions[index]
    }

    fun heartsRemaining(): Int = hearts
    fun currentStreak(): Int = streak
    fun bestStreakInRun(): Int = bestStreak
    fun fiftyFiftyCharges(): Int = fiftyFiftyLeft
    fun extraTimeCharges(): Int = extraTimeLeft
    fun hiddenChoices(): List<Int> = hiddenForCurrent
    fun xpSoFar(): Int = xp

    /** Start the first question (call after Loading UI is ready). */
    fun start() {
        if (state !is ClimbSessionState.Loading && state !is ClimbSessionState.Failed) return
        if (totalQuestions == 0) {
            state = ClimbSessionState.Failed
            return
        }
        index = 0
        timeLeft = ClimbRewards.SECONDS_PER_QUESTION.toFloat()
        hiddenForCurrent = emptyList()
        state = ClimbSessionState.Question(0, timeLeft)
    }

    /** Advance timer; returns true if a timeout was triggered. */
    fun tick(dt: Float): Boolean {
        val q = state as? ClimbSessionState.Question ?: return false
        timeLeft = (q.timeLeft - dt).coerceAtLeast(0f)
        if (timeLeft <= 0f) {
            timeout()
            return true
        }
        state = ClimbSessionState.Question(q.index, timeLeft)
        return false
    }

    fun pick(choiceIndex: Int) {
        val qState = state as? ClimbSessionState.Question ?: return
        val q = runQuestions.getOrNull(qState.index) ?: return
        if (choiceIndex in hiddenForCurrent) return
        val correct = choiceIndex == q.correctIndex
        applyAnswer(correct)
    }

    fun timeout() {
        if (state !is ClimbSessionState.Question) return
        applyAnswer(false)
    }

    fun usePowerUp(type: ClimbPowerUp): Boolean {
        val qState = state as? ClimbSessionState.Question ?: return false
        val q = runQuestions.getOrNull(qState.index) ?: return false
        when (type) {
            ClimbPowerUp.FiftyFifty -> {
                if (fiftyFiftyLeft <= 0 || isBoss) return false
                if (hiddenForCurrent.isNotEmpty()) return false
                val wrong = (q.choices.indices.filter { it != q.correctIndex }).shuffled(random)
                if (wrong.size < 2) return false
                hiddenForCurrent = wrong.take(2)
                fiftyFiftyLeft--
                return true
            }
            ClimbPowerUp.ExtraTime -> {
                if (extraTimeLeft <= 0 || isBoss) return false
                timeLeft += ClimbRewards.EXTRA_TIME_SECONDS
                extraTimeLeft--
                state = ClimbSessionState.Question(qState.index, timeLeft)
                return true
            }
        }
    }

    /** After reveal "Got it" — next question or finish. */
    fun acknowledge() {
        val rev = state as? ClimbSessionState.Reveal ?: return
        if (hearts <= 0) {
            state = ClimbSessionState.Failed
            return
        }
        val next = rev.index + 1
        if (next >= totalQuestions) {
            finishFloor()
            return
        }
        index = next
        timeLeft = ClimbRewards.SECONDS_PER_QUESTION.toFloat()
        hiddenForCurrent = emptyList()
        state = ClimbSessionState.Question(next, timeLeft)
    }

    private fun applyAnswer(correct: Boolean) {
        val qState = state as? ClimbSessionState.Question ?: return
        answers.add(correct)
        if (correct) {
            streak++
            if (streak > bestStreak) bestStreak = streak
            xp += ClimbRewards.xpForCorrect(streak, isBoss)
        } else {
            streak = 0
            hearts--
        }
        state = ClimbSessionState.Reveal(correct, qState.index)
    }

    private fun finishFloor() {
        val correctCount = answers.count { it }
        val total = answers.size.coerceAtLeast(1)
        val accuracy = correctCount.toFloat() / total.toFloat()
        val need = if (isBoss) ClimbRewards.PASS_ACCURACY_BOSS else ClimbRewards.PASS_ACCURACY_NORMAL
        if (hearts <= 0 || accuracy < need) {
            state = ClimbSessionState.Failed
            return
        }
        val stars = ClimbRewards.starsForAccuracy(accuracy)
        var gained = xp
        val firstClear = !alreadyCleared
        if (firstClear) gained += ClimbRewards.XP_FIRST_CLEAR
        state = ClimbSessionState.FloorComplete(
            stars = stars,
            accuracy = accuracy,
            xpGained = gained,
            correct = correctCount,
            total = total,
            firstClear = firstClear
        )
    }

    companion object {
        /**
         * Draw up to [target] questions without repeats.
         * Prefer Attic ids first, then the rest shuffled.
         */
        fun drawQuestions(
            pool: List<GameQuestion>,
            atticIds: Set<String>,
            target: Int,
            random: Random
        ): List<GameQuestion> {
            if (pool.isEmpty() || target <= 0) return emptyList()
            val preferred = pool.filter { it.id in atticIds }.shuffled(random)
            val rest = pool.filter { it.id !in atticIds }.shuffled(random)
            val ordered = preferred + rest
            val take = if (ordered.size < target) ordered.size else target
            return ordered.take(take).map { GameQuestionParser.shuffled(it, random) }
        }
    }
}
