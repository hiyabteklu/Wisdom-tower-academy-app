package com.wisdomtower.academy.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * L2 (docs/DAY_NIGHT_PLAN.md): surface of a text field (Settings fields and later the sign-in fields). Night: white 6 percent
 * glass, as the Settings fields always had. D2 (docs/DAY_MODE_PLAN.md): day is a solid light ivory FBF9F4 (not the old cool
 * white), so a field stands out on the frosted ivory card and the dark text and label have strong contrast. A getter: read it
 * while composing, not once at class load.
 */
val WisdomFieldSurface: Color
    get() = if (AppThemeMode.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFFBF9F4)
