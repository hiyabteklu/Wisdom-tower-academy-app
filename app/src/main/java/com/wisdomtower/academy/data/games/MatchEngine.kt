package com.wisdomtower.academy.data.games

import kotlin.random.Random

/**
 * Match game logic (docs/GAMES_HUB_PLAN.md step 4). Pure Kotlin — no Compose.
 * Up to 4 pairs (8 cards) on the board at once; matched pairs leave and new ones fill in.
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
    private val random: Random = Random.Default
) {
    private val remaining = ArrayList(pairs.filter { it.a.isNotBlank() && it.b.isNotBlank() })
    private var nextPairId = 0
    private var nextCardId = 0

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

    init {
        fillBoard()
        if (board.isEmpty()) {
            phase = MatchPhase.Complete(0, 0)
        }
    }

    fun select(cardId: Int): MatchSelectResult {
        if (phase !is MatchPhase.Playing) return MatchSelectResult.Ignored
        val card = board.find { it.id == cardId } ?: return MatchSelectResult.Ignored
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
        if (first.pairId == card.pairId && first.isTerm != card.isTerm) {
            matchedCount++
            selectedId = null
            board = board.filter { it.pairId != first.pairId }
            fillBoard()
            if (board.isEmpty() && remaining.isEmpty()) {
                phase = MatchPhase.Complete(moves, pairsTotal)
                return MatchSelectResult.MatchedAndFinished
            }
            return MatchSelectResult.Matched
        }
        selectedId = null
        return MatchSelectResult.Wrong
    }

    private fun fillBoard() {
        val presentPairs = board.map { it.pairId }.toSet()
        val need = 4 - presentPairs.size
        if (need <= 0 || remaining.isEmpty()) {
            board = board.shuffled(random)
            return
        }
        val take = minOf(need, remaining.size)
        val added = ArrayList<MatchCard>()
        repeat(take) {
            val pair = remaining.removeAt(0)
            val pid = nextPairId++
            added.add(MatchCard(nextCardId++, pid, pair.a, isTerm = true))
            added.add(MatchCard(nextCardId++, pid, pair.b, isTerm = false))
        }
        board = (board + added).shuffled(random)
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
