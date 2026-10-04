package com.wisdomtower.academy.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Unified Wisdom Tower Academy color tokens mirroring the live website (wisdom.* palette).
 * Source of truth: https://www.wisdom-tower-academy.live
 */
val WisdomCyan = Color(0xFF22E0FF) // Source of truth accent cyan (wisdom.cyan)
val WisdomCyanDark = Color(0xFF00C4E6)
val WisdomNavy = Color(0xFF060B15) // Deep obsidian navy page background
val WisdomCard = Color(0xFF0C1424) // Dark card surface (wisdom.card)
val WisdomCardBorder = Color(0x3322E0FF) // Soft 1px cyan card border
val WisdomCardBorderSubtle = Color(0x1F94A3B8) // Quiet hairline divider
val WisdomMuted = Color(0xFF94A3B8) // Slate muted text
val WisdomTextPrimary = Color(0xFFF8FAFC)
val WisdomDarkOnCyan = Color(0xFF070D17) // Dark contrast text on solid cyan pills

// Legacy theme mappings
val NavyPrimary = WisdomCyan
val NavyBackground = WisdomNavy
val NavySurface = WisdomCard
val NavyOnPrimary = WisdomDarkOnCyan
val NavyOnBackground = WisdomTextPrimary
val NavyOnSurface = WisdomTextPrimary
