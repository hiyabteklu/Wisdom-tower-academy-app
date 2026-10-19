package com.wisdomtower.academy.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Night: the six values the app always had (the approved look), plus (BG-THEME, 2026-10-17, owner: background literally
// everywhere, no pop up left) the Material "surface container" roles. Every default pop-up (AlertDialog, DropdownMenu,
// TimePicker / clock, DatePicker, ModalBottomSheet, ExposedDropdown, Snackbar-like surfaces) takes its fill from these, so ONE
// place changes all of them.
// BG-READ (2026-10-17): navy 70 percent became 82 percent, low 60 became 75, chip 20 became 25.
// BG-POPUP-2 (2026-10-17, owner: pop-up text is still not readable, it mixes with what is beneath): pop-ups must be READABLE
// first, so navy is now 95 percent (0xF2), the low variant 90 percent (0xE6), the chip 35 percent (0x59). The window
// behind a pop-up is dimmed by the system dialog scrim.
// To go back: D1 / BF / 40.
private val PopupGlassNavy = Color(0xF20F1829)
private val PopupGlassNavyLow = Color(0xE60F1829)
private val PopupGlassChip = Color(0x59FFFFFF)

// Day pop-ups (D1, docs/DAY_MODE_PLAN.md): the same ivory as the day cards, about 92 percent opaque (overlays are more
// opaque than cards, owner feedback 2026-10-17), the chip a slightly deeper solid ivory.
private val PopupGlassIvory = Color(0xEBF8F5EE)
private val PopupGlassIvoryLow = Color(0xE0F8F5EE)
private val PopupGlassIvoryChip = Color(0xFFE6E0D2)

private val DarkColorScheme =
  darkColorScheme(
    primary = WisdomDarkPalette.cyan,
    background = WisdomDarkPalette.navy,
    surface = WisdomDarkPalette.card,
    onPrimary = WisdomDarkPalette.onCyan,
    onBackground = WisdomDarkPalette.textPrimary,
    onSurface = WisdomDarkPalette.textPrimary,
    surfaceContainerLowest = PopupGlassNavyLow,
    surfaceContainerLow = PopupGlassNavyLow,
    surfaceContainer = PopupGlassNavy,
    surfaceContainerHigh = PopupGlassNavy,
    surfaceContainerHighest = PopupGlassChip,
    surfaceBright = PopupGlassChip,
    surfaceDim = PopupGlassNavy
  )

// Day (D1, docs/DAY_MODE_PLAN.md): a real light scheme, "Ivory & Navy". Built from the same palette the tokens in Color.kt use.
// `surface` is a solid ivory here (the card tokens in Color.kt are the frosted 82 percent version).
private val LightColorScheme =
  lightColorScheme(
    primary = WisdomLightPalette.cyan,
    onPrimary = WisdomLightPalette.onCyan,
    secondary = WisdomLightPalette.cyanDark,
    onSecondary = WisdomLightPalette.onCyan,
    background = WisdomLightPalette.navy,
    onBackground = WisdomLightPalette.textPrimary,
    surface = Color(0xFFF8F5EE),
    onSurface = WisdomLightPalette.textPrimary,
    surfaceVariant = Color(0xFFEFEADF),
    onSurfaceVariant = WisdomLightPalette.muted,
    outline = WisdomLightPalette.muted,
    error = WisdomLightPalette.rose,
    onError = WisdomLightPalette.onCyan,
    surfaceContainerLowest = PopupGlassIvoryLow,
    surfaceContainerLow = PopupGlassIvoryLow,
    surfaceContainer = PopupGlassIvory,
    surfaceContainerHigh = PopupGlassIvory,
    surfaceContainerHighest = PopupGlassIvoryChip,
    surfaceBright = PopupGlassIvoryChip,
    surfaceDim = PopupGlassIvory
  )

/**
 * Owner report 2026-10-12 (problem 5): many native texts are 10.5 to 12 sp and look tiny. One app-wide lift of
 * all `sp` text by 12 percent, on top of the phone's own font-size setting, but never above a total of 1.5
 * (a student who already chose a very large system font gets no extra growth). Layout sizes in `dp` are not
 * touched. If one screen clips its text because of this, fix that screen's fixed height, do not remove the lift.
 */
private const val APP_FONT_LIFT = 1.12f
private const val APP_FONT_SCALE_CAP = 1.5f

/**
 * S8 (docs/PLAY_STORE_UI_PLAN.md): on a tablet or a phone in landscape the app content is at most 720 dp wide and
 * centered. On a normal portrait phone (narrower than 720 dp) nothing changes. Dialogs and menus are separate windows and are not capped.
 * L1: the box that holds the capped content now paints the page colour, so the side margins follow day / night too
 * (before, they showed the window colour from `res/values/themes.xml`, which is navy).
 */
private val APP_CONTENT_MAX_WIDTH = 720.dp

/**
 * D1: day / night is the chosen background ([AppThemeMode], driven by [AppBackground]); it does not follow the phone's own
 * dark setting. Night (Dark) is the default.
 */
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = AppThemeMode.isDark,
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  SyncSystemBars(darkTheme)

  val systemDensity = LocalDensity.current
  val liftedDensity = Density(
    density = systemDensity.density,
    fontScale = minOf(systemDensity.fontScale * APP_FONT_LIFT, maxOf(systemDensity.fontScale, APP_FONT_SCALE_CAP))
  )

  CompositionLocalProvider(LocalDensity provides liftedDensity) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography) {
      Box(
        modifier = Modifier.fillMaxSize().appBackdrop(),
        contentAlignment = Alignment.TopCenter
      ) { // S8-width-cap
        Box(modifier = Modifier.widthIn(max = APP_CONTENT_MAX_WIDTH).fillMaxWidth().fillMaxHeight()) {
          content()
        }
      }
    }
  }
}

/**
 * L1: the status bar and the navigation bar follow day / night (their colour is navy in `themes.xml`, which would be a
 * dark strip above a light app). Night uses the same navy as before; day uses the page colour with dark icons.
 * Setting the bar colours is deprecated from Android 15 on (the bars are then transparent anyway), so the warning is
 * silenced; the icon colours use the AndroidX controller, which is not deprecated.
 */
@Suppress("DEPRECATION")
@Composable
private fun SyncSystemBars(dark: Boolean) {
  val view = LocalView.current
  if (view.isInEditMode) return
  val barColor = if (dark) WisdomDarkPalette.navy else WisdomLightPalette.navy
  SideEffect {
    val window = view.context.findHostActivity()?.window ?: return@SideEffect
    window.statusBarColor = barColor.toArgb()
    window.navigationBarColor = barColor.toArgb()
    val controller = WindowCompat.getInsetsController(window, view)
    controller.isAppearanceLightStatusBars = !dark
    controller.isAppearanceLightNavigationBars = !dark
  }
}

private fun Context.findHostActivity(): Activity? {
  var current: Context = this
  while (current is ContextWrapper) {
    if (current is Activity) return current
    current = current.baseContext
  }
  return null
}
