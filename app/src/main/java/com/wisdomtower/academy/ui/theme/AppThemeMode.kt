package com.wisdomtower.academy.ui.theme

/**
 * DAY MODE RETURNS (owner order 2026-10-19, slice D1 of docs/DAY_MODE_PLAN.md). Night is the default and stays exactly as
 * it was. Day is the "Ivory & Navy" look, chosen with the sun icon in Settings > Background ([AppBackgroundOption.Day]).
 *
 * There is no separate day / night setting: the saved background choice decides. [isDark] is false only while the Day
 * background is the selected one, so one saved value drives the page, the colour tokens in Color.kt, the Material scheme and
 * the status bars in Theme.kt. [AppBackground.selected] is Compose state, so everything that reads [isDark] while composing
 * or drawing follows a change at once. A token read once at class load (a top-level `val x = WisdomCyan`) will not follow;
 * the screen slices D2 to D8 fix those places.
 *
 * (History: day mode was cancelled on 2026-10-17 because the glass cards are see-through dark panels. Day cards are now
 * frosted ivory, see the plan. The old pale palette and slices L1 to L9 stay ignored.)
 */
object AppThemeMode {
    /** True unless the student chose the Day background. */
    val isDark: Boolean
        get() = !AppBackground.selected.isLight
}
