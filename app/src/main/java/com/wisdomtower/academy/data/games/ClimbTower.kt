package com.wisdomtower.academy.data.games

import com.wisdomtower.academy.data.content.CourseLocks
import com.wisdomtower.academy.data.content.HubGate
import com.wisdomtower.academy.data.content.LearningResource

/**
 * Builds the Climb tower from the LIGHT question-banks list.
 * Sealed floors stay visible; they never block or reorder the climb.
 * Pure Kotlin — no Compose.
 */
enum class ClimbFloorState {
    Cleared,
    Current,
    Available,
    Sealed
}

data class ClimbFloor(
    /** 1-based position in the tower. */
    val number: Int,
    /** Stable key: "chapter:<n>" or "bank:<resourceId>". */
    val key: String,
    val title: String,
    val isBoss: Boolean,
    val sealed: Boolean,
    /** Gate when sealed; null when open. */
    val sealGate: HubGate?,
    /** Banks that form this floor's question pool (light rows). */
    val banks: List<LearningResource>,
    /** Sum of itemCount when known; else bank count as fallback. */
    val questionHint: Int
)

data class ClimbTowerModel(
    val scopePath: String,
    val floors: List<ClimbFloor>,
    /** Lowest not-cleared OPEN floor number, or null if all open floors cleared / none. */
    val currentFloorNumber: Int?
)

object ClimbTowerBuilder {

    private const val HUB = "question-banks"

    /**
     * Group light [banks] into floors. [clearedKeys] from GameStore (keys with stars > 0).
     * [locks] decides sealed; null locks = everything open.
     */
    fun build(
        scopePath: String,
        banks: List<LearningResource>,
        locks: CourseLocks?,
        clearedKeys: Set<String>
    ): ClimbTowerModel {
        if (banks.isEmpty()) {
            return ClimbTowerModel(scopePath, emptyList(), null)
        }

        val chaptered = banks.filter { it.chapter != null }.groupBy { it.chapter!! }
        val unchaptered = banks.filter { it.chapter == null }.sortedBy { it.sortOrder }

        val floorSpecs = ArrayList<Triple<String, String, List<LearningResource>>>()

        val chapters = chaptered.keys.sorted()
        for (ch in chapters) {
            val group = chaptered[ch]!!.sortedBy { it.sortOrder }
            val key = "chapter:$ch"
            val title = "Chapter $ch"
            floorSpecs.add(Triple(key, title, group))
        }
        for (bank in unchaptered) {
            val key = "bank:${bank.id}"
            val title = bank.title
            floorSpecs.add(Triple(key, title, listOf(bank)))
        }

        val total = floorSpecs.size
        val floors = ArrayList<ClimbFloor>(total)
        for (i in floorSpecs.indices) {
            val (key, title, group) = floorSpecs[i]
            val number = i + 1
            val isBoss = number % 5 == 0
            val sealGate = floorSealGate(locks, group)
            val sealed = sealGate != null && sealGate != HubGate.Open
            val hint = group.sumOf { it.itemCount ?: 0 }.let { sum ->
                if (sum > 0) sum else group.size
            }
            floors.add(
                ClimbFloor(
                    number = number,
                    key = key,
                    title = title,
                    isBoss = isBoss,
                    sealed = sealed,
                    sealGate = if (sealed) sealGate else null,
                    banks = group,
                    questionHint = hint
                )
            )
        }

        val current = floors
            .filter { !it.sealed }
            .firstOrNull { it.key !in clearedKeys }
            ?.number

        return ClimbTowerModel(scopePath, floors, current)
    }

    /**
     * A floor is sealed if EVERY bank is locked.
     * If any bank is open, the floor is playable.
     */
    private fun floorSealGate(locks: CourseLocks?, group: List<LearningResource>): HubGate? {
        if (locks == null || group.isEmpty()) return null
        var anyOpen = false
        var firstClosed: HubGate? = null
        for (bank in group) {
            val gate = locks.itemGate(HUB, bank.chapter, bank.id)
            if (gate == null || gate == HubGate.Open) {
                anyOpen = true
            } else if (firstClosed == null) {
                firstClosed = gate
            }
        }
        return if (anyOpen) null else firstClosed
    }

    /** Playable banks on a floor (exclude individually locked items when possible). */
    fun openBanks(floor: ClimbFloor, locks: CourseLocks?): List<LearningResource> {
        if (locks == null) return floor.banks
        return floor.banks.filter { bank ->
            val gate = locks.itemGate(HUB, bank.chapter, bank.id)
            gate == null || gate == HubGate.Open
        }
    }
}
