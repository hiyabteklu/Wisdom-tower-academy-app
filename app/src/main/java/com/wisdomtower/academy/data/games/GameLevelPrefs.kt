package com.wisdomtower.academy.data.games

import android.content.Context

/**
 * Remembers the last level the student picked, per game (docs/GAMES_COOL_PLAN.md slice G2).
 * Phone-side only (SharedPreferences). Never throws: on any problem it falls back to MEDIUM.
 */
object GameLevelPrefs {
    private const val FILE = "wta_game_levels"

    fun last(context: Context, game: String): GameLevel {
        val id: String? = try {
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("last_$game", null)
        } catch (e: Exception) {
            null
        }
        return GameLevel.fromId(id)
    }

    fun save(context: Context, game: String, level: GameLevel) {
        try {
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putString("last_$game", level.id)
                .apply()
        } catch (e: Exception) {
            // Not saving the level is harmless.
        }
    }
}
