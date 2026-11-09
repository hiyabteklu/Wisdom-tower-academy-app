package com.wisdomtower.academy.data.games

import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.data.content.HubGate

/**
 * Package ownership for games (docs/PLAN_2026-11-09_GAMES_OWNERSHIP.md, step A).
 *
 * A game chapter uses the SAME lock rows and the SAME ownership answer as the learning hubs ([ContentAccess]).
 * Lock key of a game chapter: `chapter:<scope path>:games:<chapter no>` (one row covers Match and Wisdom Defense of
 * that chapter). The most specific row wins, then the package row decides:
 *  - Telegram paid build (PAID_UI): item row, else chapter row, else package row, then owned or not.
 *  - Play build: chapter row, else the hub / course / package mode (a non-owner is closed on a require_purchase package).
 * Owner rule (2026-11-09): chapter 1 and 2 are open rows in `content_locks`, the rest follows the package lock.
 *
 * Nothing here reads `game_sets`; it only answers "may this student open this chapter".
 *
 * STATUS: written, NOT compiled, NOT tested on a phone.
 */
object GameAccess {
    /** Hub id used in the lock keys of game chapters. */
    const val HUB_ID = "games"

    /**
     * Website-style route of a game scope path, so [ContentAccess.resolveLocks] can work out the package and the lock
     * scope the same way the learning hubs do. Null for a scope the lock logic does not know (no lock info: open).
     */
    fun routeForScope(scopePath: String): String? {
        val p = scopePath.trim().trim('/').split('/')
        return when (p.firstOrNull()) {
            "freshman" -> if (p.size >= 2) "/academy/freshman/${p[1]}" else null
            "grade" -> if (p.size >= 3) "/academy/grades/${p[1]}/${p[2]}" else null
            "remedial" -> if (p.size >= 2) "/academy/remedial/${p[1]}" else null
            "ece" -> if (p.size >= 3) "/special-packages/electrical-computer-engineering/${p[1]}/${p[2]}" else null
            else -> null
        }
    }

    /**
     * Gate of each chapter in [chapters] for the subject at [scopePath].
     *
     * `signedIn = true` is passed on purpose: [ContentAccess] opens a require_purchase course for a signed-OUT student
     * (the learning screens show their own sign-in message). Games have no such screen, so a signed-out student must
     * not get locked chapters. With no signed-in user nothing is owned, so locked chapters come back as
     * [HubGate.PackageRequired] and open rows (chapter 1 and 2) stay [HubGate.Open].
     */
    suspend fun gates(scopePath: String, chapters: List<Int>): Map<Int, HubGate> {
        val route = routeForScope(scopePath) ?: return chapters.associateWith { HubGate.Open }
        val locks = ContentAccess.resolveLocks(route, signedIn = true)
        return chapters.associateWith { n ->
            locks.itemGate(HUB_ID, n, "") ?: locks.hubGate(HUB_ID)
        }
    }

    /** Gate of one chapter. This is the authoritative check, done again right before a set is loaded. */
    suspend fun gate(scopePath: String, chapter: Int): HubGate =
        gates(scopePath, listOf(chapter))[chapter] ?: HubGate.Open

    /** Text for a closed chapter (same wording as the learning hubs, no price, no purchase button). */
    fun message(gate: HubGate): String = when (gate) {
        HubGate.ComingSoon -> "${ContentAccess.COMING_SOON_TITLE}\n\n${ContentAccess.COMING_SOON_BODY}"
        HubGate.PackageRequired -> "${ContentAccess.PURCHASE_TITLE}\n\n${ContentAccess.PURCHASE_BODY}"
        HubGate.Open -> ""
    }

    /** Short label shown on a closed chapter card, or null when the chapter is open. */
    fun label(gate: HubGate): String? = when (gate) {
        HubGate.ComingSoon -> "Coming soon"
        HubGate.PackageRequired -> "Package required"
        HubGate.Open -> null
    }
}
