package com.wisdomtower.academy.data.games

import kotlin.random.Random

/**
 * Match game logic (docs/GAMES_HUB_PLAN.md step 4, reworked by docs/GAMES_FEEL_PLAN.md slice M1, levels by
 * docs/GAMES_COOL_PLAN.md slice G1).
 * Pure Kotlin, no Compose.
 *
 * Up to [MatchTuning.pairsOnBoard] pairs (MEDIUM: 4 pairs, 8 cards) on the board at once. Cards keep their SLOT: when a pair is
 * matched, the next pair (if any) takes exactly the two slots the matched cards had, so the other cards never move. When no
 * pairs are left to bring in, the two slots simply close up.
 */
data class MatchCard(
    val id: Int,
    val pairId: Int,
    val text: String,
    /** True if this card is the "a" (term) side; false is "b" (meaning). */
    val isTerm: Boolean
)

sealed class MatchPhase {
    object Playing : MatchPhase()
    data class Complete(val moves: Int, val pairsTotal: Int) : MatchPhase()
}

class MatchEngine(
    pairs: List<GameSetMatchPair>,
    private val random: Random = Random.Default,
    /** G1: level numbers. MEDIUM = how the game played before levels. */
    private val tuning: MatchTuning = MatchTuning.MEDIUM
) {
    private val remaining = ArrayList(pairs.filter { it.a.isNotBlank() && it.b.isNotBlank() })
    private var nextPairId = 0
    private var nextCardId = 0

    /** Cards in slot order. The order only changes when slots close at the end of the set. */
    var board: List<MatchCard> = emptyList()
        private set
    var selectedId: Int? = null
        private set
    var phase: MatchPhase = MatchPhase.Playing
        private set
    var moves: Int = 0
        private set
    var matchedCount: Int = 0
        private set
    val pairsTotal: Int = remaining.size

    val level: GameLevel get() = tuning.level

    /** Matches in a row without a wrong pair. Resets to 0 on a wrong pair. */
    var streak: Int = 0
        private set
    var bestStreak: Int = 0
        private set

    /** The two cards of the last match (they are already gone from [board]). Empty until a match. */
    var lastMatchedCards: List<MatchCard> = emptyList()
        private set

    /** Slot index (position in the board at the time) of each card in [lastMatchedCards]. */
    var lastMatchedSlots: List<Int> = emptyList()
        private set

    /** Card ids of the new cards that took the matched slots (empty when the set ran out). */
    var lastNewCardIds: List<Int> = emptyList()
        private set

    /** Both card ids of the last wrong pair (still on the board). Empty after any other action. */
    var lastWrongIds: List<Int> = emptyList()
        private set

    init {
        fillInitialBoard()
        if (board.isEmpty()) {
            phase = MatchPhase.Complete(0, 0)
        }
    }

    fun select(cardId: Int): MatchSelectResult {
        if (phase !is MatchPhase.Playing) return MatchSelectResult.Ignored
        val card = board.find { it.id == cardId } ?: return MatchSelectResult.Ignored
        lastWrongIds = emptyList()
        val current = selectedId
        if (current == null) {
            selectedId = cardId
            return MatchSelectResult.Highlighted
        }
        if (current == cardId) {
            selectedId = null
            return MatchSelectResult.Cleared
        }
        val first = board.find { it.id == current } ?: run {
            selectedId = cardId
            return MatchSelectResult.Highlighted
        }
        moves++
        selectedId = null
        if (first.pairId == card.pairId && first.isTerm != card.isTerm) {
            matchedCount++
            streak++
            if (streak > bestStreak) bestStreak = streak
            replaceMatched(first, card)
            if (board.isEmpty() && remaining.isEmpty()) {
                phase = MatchPhase.Complete(moves, pairsTotal)
                return MatchSelectResult.MatchedAndFinished
            }
            return MatchSelectResult.Matched
        }
        streak = 0
        lastWrongIds = listOf(first.id, card.id)
        return MatchSelectResult.Wrong
    }

    private fun fillInitialBoard() {
        val take = minOf(tuning.pairsOnBoard.coerceAtLeast(1), remaining.size)
        val added = ArrayList<MatchCard>()
        repeat(take) { added.addAll(nextPairCards()) }
        board = added.shuffled(random)
    }

    /** Next pair as two cards (term and meaning), in no fixed order. */
    private fun nextPairCards(): List<MatchCard> {
        val pair = remaining.removeAt(0)
        val pid = nextPairId++
        val term = MatchCard(nextCardId++, pid, pair.a, isTerm = true)
        val meaning = MatchCard(nextCardId++, pid, pair.b, isTerm = false)
        return if (random.nextBoolean()) listOf(term, meaning) else listOf(meaning, term)
    }

    private fun replaceMatched(first: MatchCard, second: MatchCard) {
        val slotA = board.indexOfFirst { it.id == first.id }
        val slotB = board.indexOfFirst { it.id == second.id }
        lastMatchedCards = listOf(first, second)
        lastMatchedSlots = listOf(slotA, slotB)
        if (remaining.isEmpty()) {
            board = board.filter { it.id != first.id && it.id != second.id }
            lastNewCardIds = emptyList()
            return
        }
        val fresh = nextPairCards()
        val next = board.toMutableList()
        next[slotA] = fresh[0]
        next[slotB] = fresh[1]
        board = next
        lastNewCardIds = fresh.map { it.id }
    }
}

enum class MatchSelectResult {
    Ignored,
    Highlighted,
    Cleared,
    Matched,
    MatchedAndFinished,
    Wrong
}
