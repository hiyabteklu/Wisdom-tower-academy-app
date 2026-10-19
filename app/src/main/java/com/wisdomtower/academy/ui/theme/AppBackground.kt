package com.wisdomtower.academy.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.wisdomtower.academy.WisdomTowerApplication
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One soft, blurred colour glow of a background (no dots, no stars, no hard edges). [x] and [y] are the centre as a
 * fraction of the screen (0 to 1, may be a little outside), [radius] is a fraction of the LONGER screen side, [alpha] is
 * the strength at the centre (it fades smoothly to 0 at the edge). Low alpha keeps the glass cards readable.
 */
class BackdropGlow(val x: Float, val y: Float, val radius: Float, val color: Color, val alpha: Float)

/** How dark the shade over a picture background is (0 = none, 1 = black). Keeps white text and glass cards readable. */
private const val PHOTO_SCRIM_ALPHA = 0.35f

/** Longest side, in pixels, a picture is decoded at. The picture files are bigger than any phone screen needs. */
private const val PHOTO_MAX_SIDE = 1280

/**
 * The backgrounds the code knows. The student can choose only the ones in [AppBackground.options]: Dark ([Black]) and,
 * since D1 (owner order 2026-10-19, docs/DAY_MODE_PLAN.md), Day ([Day]), which replaces BG 2 in the picker. The other
 * entries stay here on purpose (nothing is deleted) but are not offered and are never loaded from a saved choice.
 *
 * Choosing [Day] also switches the whole app to day colours ([AppThemeMode.isDark] is false then).
 * The saved choice is the constant NAME. A saved name that is not one of the offered options (for example an old
 * "Photo2") falls back to the default, Dark.
 * [base] is the plain colour under the look; it is also what native views that need one solid colour use, and what shows
 * while a picture is still loading.
 */
enum class AppBackgroundOption(
    val label: String,
    val base: Color,
    val glows: List<BackdropGlow> = emptyList(),
    /** Path of the picture inside the app's assets folder, or null for a plain / glow look. */
    val photoAsset: String? = null
) {
    Black("Dark", Color(0xFF000000)),
    Photo1("BG 1", Color(0xFF070C16), photoAsset = "backgrounds/bg_1.jpg"),
    Photo2("BG 2", Color(0xFF070C16), photoAsset = "backgrounds/bg_2.jpg"),
    Photo3("BG 3", Color(0xFF070C16), photoAsset = "backgrounds/bg_3.jpg"),
    Midnight(
        "Blue", Color(0xFF0A1530),
        listOf(BackdropGlow(0.5f, -0.05f, 0.95f, Color(0xFF2F6BFF), 0.20f))
    ),

    /**
     * DAY (D1, docs/DAY_MODE_PLAN.md): warm ivory-stone page with two very soft glows, pale sky at the top and champagne
     * at the bottom. Values are the starting point; the owner judges them on a phone.
     */
    Day(
        "Day", Color(0xFFE9E5DC),
        listOf(
            BackdropGlow(0.5f, -0.08f, 0.95f, Color(0xFFBBD7EE), 0.55f),
            BackdropGlow(0.5f, 1.08f, 0.95f, Color(0xFFF2DDAE), 0.50f)
        )
    );

    /**
     * The plain colour used by the local WebView pages and the native views that need one solid colour. For [Day] it is
     * still the dark colour until slice D8 gives the web pages (notes, quiz, tutor ...) a day look: their text is light.
     */
    val color: Color get() = if (this == Day) Black.base else base

    /** The plain colour as #RRGGBB text (for the local WebView pages). Same rule as [color]. */
    val hex: String
        get() = String.format(Locale.US, "#%06X", color.toArgb() and 0xFFFFFF)

    /**
     * The same look as a CSS background value for the local WebView pages (notes, quiz, tutor ...), so a page shows the
     * same look as the native screens. Glow sizes use vmax (1% of the longer viewport side), like [BackdropGlow.radius].
     * A picture is a file:///android_asset URL: if a page cannot read it, the plain colour at the end of the list shows
     * instead, so the page never goes blank.
     * [Day] returns the Dark look until slice D8 (see [color]).
     */
    val cssBackground: String
        get() {
            if (this == Day) return Black.cssBackground
            val layers = ArrayList<String>()
            if (photoAsset != null) {
                val shade = "rgba(0,0,0," + num(PHOTO_SCRIM_ALPHA) + ")"
                layers.add("linear-gradient(" + shade + "," + shade + ")")
                layers.add("url(file:///android_asset/" + photoAsset + ") center / cover no-repeat")
            }
            glows.forEach { g ->
                val c = rgb(g.color)
                layers.add(
                    "radial-gradient(circle " + pct(g.radius) + "vmax at " + pct(g.x) + "% " + pct(g.y) + "%," +
                        "rgba(" + c + "," + num(g.alpha) + "),rgba(" + c + ",0))"
                )
            }
            layers.add(hex)
            return layers.joinToString(",")
        }
}

private fun rgb(c: Color): String =
    (c.red * 255f).roundToInt().toString() + "," + (c.green * 255f).roundToInt() + "," + (c.blue * 255f).roundToInt()

private fun num(v: Float): String = String.format(Locale.US, "%.3f", v)

private fun pct(v: Float): String = String.format(Locale.US, "%.1f", v * 100f)

/**
 * Loads each picture once, on a background thread (never while a screen is drawing), shrinks it, and keeps it. [get]
 * returns null until the picture is ready; when it becomes ready the Compose state [version] changes, so every
 * background that read it redraws by itself.
 */
private object BackdropPhotos {
    private val cache = HashMap<String, ImageBitmap>()
    private val loading = HashSet<String>()
    private var version by mutableStateOf(0)

    fun get(option: AppBackgroundOption): ImageBitmap? {
        val asset = option.photoAsset ?: return null
        if (version < 0) return null // reads the state so a finished load triggers a redraw
        synchronized(this) {
            cache[asset]?.let { return it }
            if (!loading.add(asset)) return null
        }
        Thread {
            val bitmap = runCatching { decode(asset) }.getOrNull()
            synchronized(this) {
                if (bitmap != null) cache[asset] = bitmap
                loading.remove(asset)
            }
            if (bitmap != null) version += 1
        }.start()
        return null
    }

    private fun decode(asset: String): ImageBitmap? {
        val assets = WisdomTowerApplication.instance.assets
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assets.open(asset).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return assets.open(asset).use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
    }
}

/**
 * Draws a background: the plain colour, then the picture covering the screen with a dark shade over it, then
 * the soft glows. With no [option] it draws the one the student chose and follows later changes at once (the
 * choice is Compose state read while drawing). Settings passes each option to draw its preview.
 */
fun Modifier.appBackdrop(option: AppBackgroundOption? = null): Modifier = this.drawBehind {
    val opt = option ?: AppBackground.selected
    drawRect(color = opt.base)
    val photo = BackdropPhotos.get(opt)
    if (photo != null) {
        // "Cover": scale the picture to fill the screen and crop the overflow evenly; High quality filtering keeps it smooth.
        val scale = maxOf(size.width / photo.width, size.height / photo.height)
        val srcW = (size.width / scale).roundToInt().coerceIn(1, photo.width)
        val srcH = (size.height / scale).roundToInt().coerceIn(1, photo.height)
        drawImage(
            image = photo,
            srcOffset = IntOffset((photo.width - srcW) / 2, (photo.height - srcH) / 2),
            srcSize = IntSize(srcW, srcH),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High
        )
        drawRect(color = Color.Black.copy(alpha = PHOTO_SCRIM_ALPHA))
    }
    val longSide = maxOf(size.width, size.height)
    opt.glows.forEach { g ->
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(g.color.copy(alpha = g.alpha), g.color.copy(alpha = 0f)),
                center = Offset(size.width * g.x, size.height * g.y),
                radius = longSide * g.radius
            )
        )
    }
}

/**
 * The ONE holder of the chosen background. The choice is saved in SharedPreferences (key [KEY], the same key as before)
 * and exposed as Compose state, so every [AtmosphereBackground] redraws the moment [select] is used.
 * Default: Dark. Only the options in [options] can be chosen or loaded.
 *
 * The saved value is read once when this object is first used (the Application already exists by then), so no state is
 * written while a screen is composing.
 *
 * IMPORTANT (this order caused a startup crash once): properties of an `object` are set up top to bottom, so anything
 * that [readSaved] uses must be a constant or the enum itself, never a property declared further down in this object.
 * [options] is declared first and [readSaved] reads it, which is why it must stay above [state].
 */
object AppBackground {
    private const val PREFS_NAME = "wt_app_background_prefs"
    private const val KEY = "wt_app_background"

    /**
     * D1 switch (docs/DAY_MODE_PLAN.md section 7, publishing gate). While day mode is unfinished (slices D2 to D8) the
     * Day option must not reach students: set this to false and the picker offers Dark only (an old saved Day falls back
     * to Dark). It is true on the branch `feature/day-mode-d1` so the owner can look at the foundation on a phone.
     */
    const val DAY_MODE_ENABLED = true

    /** The offered options in display order: Dark, then Day when [DAY_MODE_ENABLED]. Declared BEFORE [state] on purpose. */
    val options: List<AppBackgroundOption> =
        if (DAY_MODE_ENABLED) listOf(AppBackgroundOption.Black, AppBackgroundOption.Day)
        else listOf(AppBackgroundOption.Black)

    private var state by mutableStateOf(readSaved())

    /** The option in use now. Reading it inside a composable or a draw block makes that follow later changes. */
    val selected: AppBackgroundOption
        get() = state

    /** Applies the option to the whole app at once and saves it. */
    fun select(context: Context, option: AppBackgroundOption) {
        state = option
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, option.name)
            .apply()
    }

    private fun readSaved(): AppBackgroundOption {
        val default = AppBackgroundOption.Black
        val app = runCatching { WisdomTowerApplication.instance }.getOrNull() ?: return default
        val name = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY, null)
        return options.firstOrNull { it.name == name } ?: default
    }
}
