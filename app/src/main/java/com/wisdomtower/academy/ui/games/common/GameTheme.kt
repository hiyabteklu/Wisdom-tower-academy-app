package com.wisdomtower.academy.ui.games.common

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted

/**
 * Zone palettes and low-end switch for Climb / Defense.
 * Brand accents use existing theme tokens; zone colours are fixed night-look game art.
 */
object GameTheme {

    val cyan: Color get() = WisdomCyan
    val amber: Color get() = WisdomAccentAmber
    val muted: Color get() = WisdomMuted
    val gold = Color(0xFFF1DFB4)
    val goldDeep = Color(0xFF5C3F0A)
    val heart = Color(0xFFF43F5E)
    val correct = Color(0xFF10B981)
    val wrong = Color(0xFFF43F5E)

    enum class Zone {
        StoneFoundation,
        LibraryHall,
        ClockworkGallery,
        Observatory,
        SkyCrown
    }

    data class ZonePalette(
        val wall: Color,
        val wallDark: Color,
        val accent: Color,
        val skyTop: Color,
        val skyBottom: Color
    )

    fun zonePalette(zone: Zone): ZonePalette = when (zone) {
        Zone.StoneFoundation -> ZonePalette(
            wall = Color(0xFF3A3A42),
            wallDark = Color(0xFF222228),
            accent = Color(0xFF6B8F71),
            skyTop = Color(0xFF1A1F2E),
            skyBottom = Color(0xFF0C1018)
        )
        Zone.LibraryHall -> ZonePalette(
            wall = Color(0xFF4A3428),
            wallDark = Color(0xFF2A1C14),
            accent = Color(0xFFC4A35A),
            skyTop = Color(0xFF1E1620),
            skyBottom = Color(0xFF0E0A12)
        )
        Zone.ClockworkGallery -> ZonePalette(
            wall = Color(0xFF3A3A48),
            wallDark = Color(0xFF1E1E28),
            accent = Color(0xFFD4A017),
            skyTop = Color(0xFF161A28),
            skyBottom = Color(0xFF0A0C14)
        )
        Zone.Observatory -> ZonePalette(
            wall = Color(0xFF1E2A3A),
            wallDark = Color(0xFF0E1620),
            accent = Color(0xFF7EB8DA),
            skyTop = Color(0xFF0A1020),
            skyBottom = Color(0xFF04060E)
        )
        Zone.SkyCrown -> ZonePalette(
            wall = Color(0xFF2A2840),
            wallDark = Color(0xFF14122A),
            accent = Color(0xFFE8C87A),
            skyTop = Color(0xFF1A1040),
            skyBottom = Color(0xFF08041A)
        )
    }

    fun zoneForFloor(floorNumber: Int, totalFloors: Int): Zone {
        if (totalFloors >= 41) {
            return when {
                floorNumber <= 10 -> Zone.StoneFoundation
                floorNumber <= 20 -> Zone.LibraryHall
                floorNumber <= 30 -> Zone.ClockworkGallery
                floorNumber <= 40 -> Zone.Observatory
                else -> Zone.SkyCrown
            }
        }
        if (totalFloors <= 0) return Zone.StoneFoundation
        val band = ((floorNumber - 1).toFloat() / totalFloors.toFloat() * 5f).toInt().coerceIn(0, 4)
        return Zone.entries[band]
    }

    fun isLowEnd(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null) {
            if (am.isLowRamDevice) return true
            if (am.memoryClass <= 128) return true
        }
        if (Build.VERSION.SDK_INT < 26) return true
        return false
    }

    fun reduceMotion(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    fun particleCap(context: Context): Int = if (isLowEnd(context)) 24 else 80
}
