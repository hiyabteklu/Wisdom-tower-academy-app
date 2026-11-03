package com.wisdomtower.academy.data.games

import kotlin.random.Random

/**
 * Match game logic (docs/GAMES_HUB_PLAN.md step 4, reworked by docs/GAMES_FEEL_PLAN.md slice M1, levels by
 * docs/GAMES_COOL_PLAN.md slice G1, random arrivals 2026-10-24, instant refill M1 of
 * docs/READER_EXAM_FLASHCARD_FIX_PLAN.md 2026-11-02).
 * Pure Kotlin, no Compose.
 *
 * The board has a fixed number of slots (MEDIUM: 4 pairs = 8 slots). The first board is full and holds whole pairs.
 *
 * 2026-11-02 (owner: after a match the slots stayed blank and the new cards came too slowly): when a pair is matched,
 * its two slots are refilled AT ONCE, inside [select], from the deck ([lastRefills] tells the screen which card took
 * which slot). The new cards are NOT the next pair: each one is picked on its own from the whole shuffled deck, so a
 * question does not arrive together with its own answer (about 4 times in 10 a pick is the partner of a card that is
 * already on the board, so matches keep coming). Which card goes to which of the two freed slots is random too.
 * Only when the deck has fewer cards than freed slots (the end of the set) do slots stay empty; the screen then hides them.
 *
 * The game can never get stuck: the last empty slot of the board is never filled in a way that leaves a FULL board
 * without a matchable pair, and every card in the deck has its partner either on the board or in the deck.
 * [spawnNext] / [canSpawn] (one card at a random moment, used 2026-10-24) are kept for compatibility but the screen no
 * longer calls them.
 */
data class MatchCard(
    val id: Int,
    val pairId: Int,
    val text: String,
    /** True if this card is the "a" (term) side; false is "b" (meaning). */
    val isTerm: Boolean
)

/** A card that was just brought onto the board, and the slot it took. */
data class MatchSpawn(val card: MatchCard, val slot: Int)

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
    private val validPairs = pairs.filter { it.a.isNotBlank() && it.b.isNotBlank() }
    private var nextCardId = 0

    /** Single cards that are not on the board yet, in random order. */
    private val deck = ArrayList<MatchCard>()

    /** Slots in fixed order. A null slot is empty (only at the very end of the set, when the deck has run out). */
    var slots: List<MatchCard?> = emptyList()
        private set
    var selectedId: Int? = null
        private set
    var phase: MatchPhase = MatchPhase.Playing
        private set
    var moves: Int = 0
        private set
    var matchedCount: Int = 0
        private set
    val pairsTotal: Int = validPairs.size

    /** The cards that are on the board right now (empty slots left out). */
    val board: List<MatchCard> get() = slots.filterNotNull()

    /** How many single cards are still waiting in the deck. 0 = nothing new will arrive. */
    val deckCount: Int get() = deck.size

    val level: GameLevel get() = tuning.level

    /** Matches in a row without a wrong pair. Resets to 0 on a wrong pair. */
    var streak: Int = 0
        private set
    var bestStreak: Int = 0
        private set

    /** The two cards of the last match (they are already gone from the board). Empty until a match. */
    var lastMatchedCards: List<MatchCard> = emptyList()
        private set

    /** Slot index of each card in [lastMatchedCards]. */
    var lastMatchedSlots: List<Int> = emptyList()
        private set

    /**
     * The cards that took the freed slots right after the last match (already on the board). Empty when the deck was
     * empty. The screen shows them once the matched pair has faded out.
     */
    var lastRefills: List<MatchSpawn> = emptyList()
        private set

    /** Both card ids of the last wrong pair (still on the board). Empty after any other action. */
    var lastWrongIds: List<Int> = emptyList()
        private set

    /** True while the deck still has cards and at least one slot is empty. */
    val canSpawn: Boolean get() = deck.isNotEmpty() && slots.any { it == null }

    init {
        val shuffledPairs = validPairs.shuffled(random)
        val cardsByPair = shuffledPairs.mapIndexed { index, pair ->
            val term = MatchCard(nextCardId++, index, pair.a, isTerm = true)
            val meaning = MatchCard(nextCardId++, index, pair.b, isTerm = false)
            listOf(term, meaning)
        }
        val take = minOf(tuning.pairsOnBoard.coerceAtLeast(1), cardsByPair.size)
        slots = cardsByPair.take(take).flatten().shuffled(random)
        deck.addAll(cardsByPair.drop(take).flatten().shuffled(random))
        if (slots.isEmpty()) {
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
            clearMatched(first, card)
            // Instant refill of the two freed slots (random single cards, not the next pair).
            lastRefills = refill(lastMatchedSlots)
            if (board.isEmpty() && deck.isEmpty()) {
                phase = MatchPhase.Complete(moves, pairsTotal)
                return MatchSelectResult.MatchedAndFinished
            }
            return MatchSelectResult.Matched
        }
        streak = 0
        lastWrongIds = listOf(first.id, card.id)
        return MatchSelectResult.Wrong
    }

    /** True if two cards on the board belong together right now. */
    fun hasMatchOnBoard(): Boolean {
        val cur = board
        return cur.any { a -> cur.any { b -> b.pairId == a.pairId && b.isTerm != a.isTerm } }
    }

    /**
     * Brings one random card from the deck into one random empty slot. Returns null when there is nothing to do.
     * Kept for compatibility (the screen no longer calls it since 2026-11-02, see [refill]).
     */
    fun spawnNext(): MatchSpawn? {
        if (phase !is MatchPhase.Playing) return null
        if (deck.isEmpty()) return null
        val free = slots.indices.filter { slots[it] == null }
        if (free.isEmpty()) return null
        return placeOne(free.random(random))
    }

    /**
     * Fills the given slots, one card each, from the deck, in random order. Stops when the deck is empty. Returns the
     * cards that were placed with their slots.
     */
    private fun refill(freed: List<Int>): List<MatchSpawn> {
        val out = ArrayList<MatchSpawn>()
        for (slot in freed.filter { it >= 0 && it < slots.size }.shuffled(random)) {
            if (deck.isEmpty()) break
            if (slots[slot] != null) continue
            out.add(placeOne(slot))
        }
        return out
    }

    /**
     * Picks ONE card from the deck on its own and puts it in [slot] (which must be empty). About 4 times in 10 the card
     * is the partner of a card already on the board, so matches keep coming; otherwise it is any card of the deck. The
     * last empty slot of the board is never filled in a way that leaves the full board unsolvable.
     */
    private fun placeOne(slot: Int): MatchSpawn {
        val onBoard = board
        val partners = deck.filter { d -> onBoard.any { it.pairId == d.pairId } }
        var pick: MatchCard = if (partners.isNotEmpty() && random.nextInt(100) < PARTNER_CHANCE_PERCENT) {
            partners.random(random)
        } else {
            deck.random(random)
        }
        // Filling the last empty slot: the full board must contain at least one pair that can be matched.
        val emptyCount = slots.count { it == null }
        if (emptyCount == 1 && !hasMatchOnBoard() && onBoard.none { it.pairId == pick.pairId }) {
            if (partners.isNotEmpty()) pick = partners.random(random)
        }
        deck.remove(pick)
        val next = slots.toMutableList()
        next[slot] = pick
        slots = next
        return MatchSpawn(pick, slot)
    }

    private fun clearMatched(first: MatchCard, second: MatchCard) {
        val slotA = slots.indexOfFirst { it?.id == first.id }
        val slotB = slots.indexOfFirst { it?.id == second.id }
        lastMatchedCards = listOf(first, second)
        lastMatchedSlots = listOf(slotA, slotB)
        val next = slots.toMutableList()
        if (slotA >= 0) next[slotA] = null
        if (slotB >= 0) next[slotB] = null
        slots = next
    }

    private companion object {
        const val PARTNER_CHANCE_PERCENT = 40
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
