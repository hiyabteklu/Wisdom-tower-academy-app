package com.wisdomtower.academy.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Unified Wisdom Tower Academy color tokens mirroring the live website (src/app/globals.css).
 * Source of truth: https://www.wisdom-tower-academy.live
 *
 * L1 (docs/DAY_NIGHT_PLAN.md, owner order 2026-10-17): every token below is MODE AWARE. The names did not change, so
 * every screen that already uses them follows the day / night switch ([AppThemeMode]) with no edit. In night mode each
 * token returns exactly the value it always had (the approved look). In day mode it returns the light value from
 * [WisdomLightPalette].
 *
 * D1 (docs/DAY_MODE_PLAN.md, owner order 2026-10-19): the day values are the premium "Ivory & Navy" palette, NOT the old
 * pale one (page F4F7FB, white cards). Day cards are NOT see-through: they are frosted (about 82 to 88 percent opaque) with a
 * hairline border. Text is deep navy ink (never pure black). The accent is a deep teal-cyan with white text on it. The
 * contrast figures in the plan are hand estimates (hypothesis) until the D9 audit measures them.
 * Gold (badges, leaderboard, Pro: fill F1DFB4, text 5C3F0A) has no token yet; it arrives with the slice that first uses it.
 *
 * DAY-SAGE (owner order 2026-10-19: "I hate the white background, black thumbnails on white are awkward"): the page and the
 * frosted surfaces of the day look were retinted to a sage-teal family (page A9C6C1, card DCEAE7, elevated E4F0ED, deep
 * C9DCD8, muted 3F4E64).
 *
 * DAY-IVORY2 (owner order 2026-10-19, same day: the sage page looked muddy and inverse, the bars and the cards were lighter
 * than the page): BACK TO IVORY. The page is the light warm ivory E9E5DC, the cards are frosted ivory F8F5EE, elevated
 * FBF9F4, deep E4DFD4 (the same alphas as before), muted 4A5A70. The cards are lighter than the page, like the bars. The
 * black cover thumbnails are not a reason to colour the page: the card already clips them with its rounded corners, a
 * hairline edge and the soft navy shadow. The sage values were: page A9C6C1, card DCEAE7, elevated E4F0ED, deep C9DCD8,
 * muted 3F4E64, gradient E2EFEC to D6E6E2. The same three card values are repeated in WisdomGlass.kt
 * (WisdomGlassFill / Strong / Quiet) and the page colour in AppBackground.kt (Day); change them together.
 *
 * BG-TOKENS (2026-10-17, owner: the chosen background must show literally everywhere): the three NIGHT fill tokens
 * [card], [cardElevated] and [surfaceDeep] and the night [WisdomCardSurfaceGradient] are see-through, so every screen that
 * paints its cards, panels and surfaces with `WisdomCard`, `WisdomCardElevated`, `WisdomDark` or the card gradient lets
 * the chosen background show through. [navy] and [background] stay solid on purpose (the status and navigation bars are
 * painted with [navy]). Their RGB values are unchanged.
 * BG-STRENGTH (2026-10-17, owner: the glass is good but too faint against the textured background, raise slightly):
 * alpha 0x66 (40 percent) became 0x85 (52 percent); the gradient 0x66 / 0x73 became 0x85 / 0x8F. To go back, set them to
 * 66 / 66 / 66 and 66 / 73.
 *
 * Two tokens cover what `Color.White` / `Color.Black` were used for in screens:
 *  - [WisdomInk]   = the foreground colour (white at night, deep navy by day). Use it for text, icons and white tints.
 *  - [WisdomOnInk] = text on top of an [WisdomInk] fill (black at night, white by day), e.g. a selected chip.
 * Rule for the screen sweeps: never write `Color.White` / `Color.Black` for text or tints again; use these two.
 *
 * A token is a getter, so it must be read while composing or drawing. A value read once at class load (a top-level
 * `val x = WisdomCyan`, or `remember { }` without keys) will NOT follow the switch; the sweeps fix those places.
 */
internal object WisdomDarkPalette {
    val cyan = Color(0xFF22E0FF)
    val cyanDark = Color(0xFF00C4E6)
    val background = Color(0xFF070C16)
    val surfaceDeep = Color(0x850A101C)
    val navy = Color(0xFF060B15)
    val card = Color(0x850C1424)
    val cardElevated = Color(0x85162134)
    val cardBorder = Color(0x3322E0FF)
    val cardBorderSubtle = Color(0x17FFFFFF)
    val borderWhite = Color(0x1AFFFFFF)
    val muted = Color(0xFFAAB6C8)
    val textPrimary = Color(0xFFF4F7FB)
    val onCyan = Color(0xFF070D17)
    val ink = Color(0xFFFFFFFF)
    val onInk = Color(0xFF000000)
    val purple = Color(0xFFA855F7)
    val violet = Color(0xFF8B5CF6)
    val amber = Color(0xFFF59E0B)
    val emerald = Color(0xFF10B981)
    val rose = Color(0xFFF43F5E)
    val sky = Color(0xFF38BDF8)
    val orange = Color(0xFFF97316)
    val indigo = Color(0xFF6366F1)
    val fuchsia = Color(0xFFD946EF)
}

/**
 * Day values, "Ivory & Navy" (restored by DAY-IVORY2, see the file comment). Values are the starting point: the owner sees
 * them on a phone and they change on the owner's word.
 *  - page: light warm ivory E9E5DC (solid; also the status and navigation bars)
 *  - card: frosted ivory F8F5EE at 82 percent (alpha D1), elevated card FBF9F4 at 85 percent (D9), deep surface E4DFD4 at 82 percent
 *  - borders: deep navy 0B1B33 at 12 percent (hairline) and 14 percent; the soft accent border is the teal at 20 percent
 *  - text: deep navy ink 0B1B33; muted 4A5A70
 *  - accent: deep teal-cyan 0B7285, white text on it
 * The category accents are the darker day versions, chosen by hand to stay readable on light cards (hypothesis, D9 measures
 * them); amber, emerald and orange are one step darker than the old pale palette because the old values were too light.
 */
internal object WisdomLightPalette {
    val cyan = Color(0xFF0B7285)
    val cyanDark = Color(0xFF095A69)
    val background = Color(0xFFE9E5DC)
    val surfaceDeep = Color(0xD1E4DFD4)
    val navy = Color(0xFFE9E5DC)
    val card = Color(0xD1F8F5EE)
    val cardElevated = Color(0xD9FBF9F4)
    val cardBorder = Color(0x330B7285)
    val cardBorderSubtle = Color(0x1F0B1B33)
    val borderWhite = Color(0x240B1B33)
    val muted = Color(0xFF4A5A70)
    val textPrimary = Color(0xFF0B1B33)
    val onCyan = Color(0xFFFFFFFF)
    val ink = Color(0xFF0B1B33)
    val onInk = Color(0xFFFFFFFF)
    val purple = Color(0xFF7E22CE)
    val violet = Color(0xFF6D28D9)
    val amber = Color(0xFF92400E)
    val emerald = Color(0xFF065F46)
    val rose = Color(0xFFBE123C)
    val sky = Color(0xFF0369A1)
    val orange = Color(0xFF9A3412)
    val indigo = Color(0xFF4338CA)
    val fuchsia = Color(0xFFA21CAF)
}

private fun pick(dark: Color, light: Color): Color = if (AppThemeMode.isDark) dark else light

// Primary Palette
val WisdomCyan: Color get() = pick(WisdomDarkPalette.cyan, WisdomLightPalette.cyan) // night: --cyan #22e0ff; day: deep teal-cyan for contrast
val WisdomCyanDark: Color get() = pick(WisdomDarkPalette.cyanDark, WisdomLightPalette.cyanDark)
val WisdomBackground: Color get() = pick(WisdomDarkPalette.background, WisdomLightPalette.background) // root canvas (--background: #070c16)
val WisdomDark: Color get() = pick(WisdomDarkPalette.surfaceDeep, WisdomLightPalette.surfaceDeep) // secondary background (--wt-dark: #0a101c)
val WisdomNavy: Color get() = pick(WisdomDarkPalette.navy, WisdomLightPalette.navy) // page / status background
val WisdomCard: Color get() = pick(WisdomDarkPalette.card, WisdomLightPalette.card) // surface card (--wt-card); day: frosted ivory
val WisdomCardElevated: Color get() = pick(WisdomDarkPalette.cardElevated, WisdomLightPalette.cardElevated) // card-modern surface
val WisdomCardBorder: Color get() = pick(WisdomDarkPalette.cardBorder, WisdomLightPalette.cardBorder) // soft accent card border
val WisdomCardBorderSubtle: Color get() = pick(WisdomDarkPalette.cardBorderSubtle, WisdomLightPalette.cardBorderSubtle) // 1px border
val WisdomBorderWhite: Color get() = pick(WisdomDarkPalette.borderWhite, WisdomLightPalette.borderWhite) // night: white 10%; day: navy ink 14%
val WisdomMuted: Color get() = pick(WisdomDarkPalette.muted, WisdomLightPalette.muted) // slate muted text (--wt-muted)
val WisdomTextPrimary: Color get() = pick(WisdomDarkPalette.textPrimary, WisdomLightPalette.textPrimary) // (--foreground)
val WisdomDarkOnCyan: Color get() = pick(WisdomDarkPalette.onCyan, WisdomLightPalette.onCyan) // text on a solid cyan button (dark at night, white by day)

// New in L1: foreground and its opposite (replace Color.White / Color.Black for text, icons and tints in screens)
val WisdomInk: Color get() = pick(WisdomDarkPalette.ink, WisdomLightPalette.ink)
val WisdomOnInk: Color get() = pick(WisdomDarkPalette.onInk, WisdomLightPalette.onInk)

// Semantic & Category Accents (day values are darker so they stay readable on light cards)
val WisdomAccentPurple: Color get() = pick(WisdomDarkPalette.purple, WisdomLightPalette.purple)
val WisdomAccentViolet: Color get() = pick(WisdomDarkPalette.violet, WisdomLightPalette.violet)
val WisdomAccentAmber: Color get() = pick(WisdomDarkPalette.amber, WisdomLightPalette.amber)
val WisdomAccentEmerald: Color get() = pick(WisdomDarkPalette.emerald, WisdomLightPalette.emerald)
val WisdomAccentRose: Color get() = pick(WisdomDarkPalette.rose, WisdomLightPalette.rose)
val WisdomAccentSky: Color get() = pick(WisdomDarkPalette.sky, WisdomLightPalette.sky)
val WisdomAccentOrange: Color get() = pick(WisdomDarkPalette.orange, WisdomLightPalette.orange)
val WisdomAccentIndigo: Color get() = pick(WisdomDarkPalette.indigo, WisdomLightPalette.indigo)
val WisdomAccentFuchsia: Color get() = pick(WisdomDarkPalette.fuchsia, WisdomLightPalette.fuchsia)

// Gradient Brushes
val WisdomHeroGradientBrush: Brush
    get() = if (AppThemeMode.isDark) {
        Brush.horizontalGradient(colors = listOf(Color(0xFF22E0FF), Color(0xFF38BDF8), Color(0xFF818CF8)))
    } else {
        Brush.horizontalGradient(colors = listOf(Color(0xFF0B7285), Color(0xFF0369A1), Color(0xFF4338CA)))
    }

// Day: frosted ivory, 88 percent at the top (E0) to 82 percent at the bottom (D1). NOT see-through like the night glass.
val WisdomCardSurfaceGradient: Brush
    get() = if (AppThemeMode.isDark) {
        Brush.verticalGradient(colors = listOf(Color(0x85162134), Color(0x8F0E1624)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xE0F8F5EE), Color(0xD1F3EFE6)))
    }

val WisdomPrimaryBtnGradient: Brush
    get() = if (AppThemeMode.isDark) {
        Brush.horizontalGradient(colors = listOf(Color(0xFF22E0FF), Color(0xFF00C4E6)))
    } else {
        Brush.horizontalGradient(colors = listOf(Color(0xFF0B7285), Color(0xFF095A69)))
    }

// Material 3 mappings
val NavyPrimary: Color get() = WisdomCyan
val NavyBackground: Color get() = WisdomNavy
val NavySurface: Color get() = WisdomCard
val NavyOnPrimary: Color get() = WisdomDarkOnCyan
val NavyOnBackground: Color get() = WisdomTextPrimary
val NavyOnSurface: Color get() = WisdomTextPrimary
