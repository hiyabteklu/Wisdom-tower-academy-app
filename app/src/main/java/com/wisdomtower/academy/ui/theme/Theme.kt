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

// Shared mode-aware popup fills live in WisdomGlass.kt and are used by both Material defaults and custom overlays.

private val DarkColorScheme: androidx.compose.material3.ColorScheme
  get() = darkColorScheme(
    primary = WisdomDarkPalette.cyan,
    background = WisdomDarkPalette.navy,
    surface = WisdomDarkPalette.card,
    onPrimary = WisdomDarkPalette.onCyan,
    onBackground = WisdomDarkPalette.textPrimary,
    onSurface = WisdomDarkPalette.textPrimary,
    surfaceContainerLowest = WisdomPopupFillLow,
    surfaceContainerLow = WisdomPopupFillLow,
    surfaceContainer = WisdomPopupFill,
    surfaceContainerHigh = WisdomPopupFill,
    surfaceContainerHighest = WisdomPopupSurfaceElevated,
    surfaceBright = WisdomPopupSurfaceElevated,
    surfaceDim = WisdomPopupFill
  )

// Day (D1): a real light scheme built from the "Ivory & Navy" palette in Color.kt (deep navy text on ivory, deep teal accent
// with white text on it) and the ivory overlay roles above.
private val LightColorScheme: androidx.compose.material3.ColorScheme
  get() = lightColorScheme(
    primary = WisdomLightPalette.cyan,
    onPrimary = WisdomLightPalette.onCyan,
    secondary = WisdomLightPalette.cyanDark,
    onSecondary = WisdomLightPalette.onCyan,
    background = WisdomLightPalette.navy,
    onBackground = WisdomLightPalette.textPrimary,
    surface = WisdomLightPalette.card,
    onSurface = WisdomLightPalette.textPrimary,
    surfaceVariant = WisdomLightPalette.cardElevated,
    onSurfaceVariant = WisdomLightPalette.muted,
    outline = WisdomLightPalette.muted,
    outlineVariant = WisdomLightPalette.borderWhite,
    error = WisdomLightPalette.rose,
    onError = WisdomLightPalette.onCyan,
    surfaceContainerLowest = WisdomPopupFillLow,
    surfaceContainerLow = WisdomPopupFillLow,
    surfaceContainer = WisdomPopupFill,
    surfaceContainerHigh = WisdomPopupFill,
    surfaceContainerHighest = WisdomPopupSurfaceElevated,
    surfaceBright = WisdomPopupSurfaceElevated,
    surfaceDim = WisdomPopupFill
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
 * D1: day / night follows the saved background choice ([AppThemeMode], [AppBackground]): the sun icon in Settings >
 * Background selects Day, the moon selects Dark. It does not follow the phone's own dark setting. Night is the default.
 * `darkTheme` is read here while composing, so choosing a background re-themes the whole app at once.
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
 * L1 / D1: the status bar and the navigation bar follow day / night (their colour is navy in `themes.xml`, which would be a
 * dark strip above a light app). Night uses the same navy as before; day uses the solid ivory page colour with dark icons.
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
