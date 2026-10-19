package com.wisdomtower.academy.ui.theme

/**
 * DAY MODE RETURNS (owner order 2026-10-19, docs/DAY_MODE_PLAN.md, slice D1). It replaces the "night only" version of
 * 2026-10-17 (the glass design needs a dark page; the day look therefore uses frosted ivory cards, see the plan).
 *
 * There is NO second saved choice: day / night is simply the chosen background. [isDark] is false only while the Day
 * background ([AppBackgroundOption.Day]) is selected; every other option is night. [AppBackground.selected] is Compose
 * state, so everything that reads [isDark] while composing or drawing follows a change at once. Night values are exactly
 * the ones the app always had.
 */
object AppThemeMode {
    val isDark: Boolean
        get() = AppBackground.selected != AppBackgroundOption.Day
}
