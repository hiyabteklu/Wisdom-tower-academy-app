package com.wisdomtower.academy.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
 * DAY MODE IS SWITCHED ON (owner order 2026-10-19: "the transparent button becomes our day mode button, night mode is
 * the dark"). true: Settings offers Night (the dark look) and Day. The Transparent picture is no longer offered and a
 * saved Transparent choice falls back to Night. Nothing was deleted: the Transparent picture option and its file are
 * still in the code and assets, just unreachable. To switch day mode off again, change true to false (that brings back
 * Night + Transparent). See docs/DAY_MODE_PLAN.md section 7.
 */
private const val DAY_MODE_OFFERED = true

/**
 * The backgrounds the code knows. Offered today: Night and Day (see [AppBackground.options]).
 *
 * Night is the old Dark look (the constant is still named [Black] on purpose: the saved choice is the constant NAME, so
 * keeping it means a phone that already saved Dark keeps it). Day draws no picture: it is only the Day colour plus two
 * soft glows, so nothing is read from the assets folder for it.
 *
 * The picture background that was called "BG 2" and then "Transparent" ([Photo2], file `backgrounds/transparent.jpg`)
 * stays here on purpose (nothing is deleted) but is not offered while [DAY_MODE_OFFERED] is true, and a saved choice of
 * it loads as Night. The same is true of BG 1, BG 3 and Blue.
 *
 * The saved choice is the constant NAME. A saved name that is not one of the offered options falls back to the default,
 * Transparent ([Photo2]) since O1 (owner order 2026-10-23).
 * [base] is the plain colour under the look; it is also what native views that need one solid colour use, and what shows
 * while a picture is still loading.
 * [isLight] is true only for the Day look. It is what switches the whole app to day colours (see [AppThemeMode]).
 */
enum class AppBackgroundOption(
    val label: String,
    val base: Color,
    val glows: List<BackdropGlow> = emptyList(),
    /** Path of the picture inside the app's assets folder, or null for a plain / glow look. */
    val photoAsset: String? = null,
    /** True for a light look. Only [Day] is light. */
    val isLight: Boolean = false
) {
    // Was "Dark". Now "Night" (owner order 2026-10-19). Same constant name, same look.
    Black("Night", Color(0xFF000000)),
    Photo1("BG 1", Color(0xFF070C16), photoAsset = "backgrounds/bg_1.jpg"),
    // Was "BG 2". Was the "Transparent" look. Not offered now (the Transparent button became Day). File kept in assets.
    Photo2("Transparent", Color(0xFF070C16), photoAsset = "backgrounds/transparent.jpg"),
    Photo3("BG 3", Color(0xFF070C16), photoAsset = "backgrounds/bg_3.jpg"),
    Midnight(
        "Blue", Color(0xFF0A1530),
        listOf(BackdropGlow(0.5f, -0.05f, 0.95f, Color(0xFF2F6BFF), 0.20f))
    ),

    /**
     * DAY-IVORY2 day look (ON, see [DAY_MODE_OFFERED]): light warm ivory page E9E5DC, a pale sky glow at the top (DDEBF3)
     * and a champagne glow at the bottom (F1E3C4). No picture. [isLight] is true, so the day tokens apply everywhere.
     * The sage version (page A9C6C1, glows E3F1F5 0.45 and 7FA8A2 0.40) looked muddy and inverse and was replaced.
     */
    Day(
        "Day", Color(0xFFE9E5DC),
        listOf(
            BackdropGlow(0.5f, -0.05f, 0.95f, Color(0xFFDDEBF3), 0.50f),
            BackdropGlow(0.5f, 1.05f, 0.95f, Color(0xFFF1E3C4), 0.40f)
        ),
        isLight = true
    );

    /** The plain colour (same as [base]). */
    val color: Color get() = base

    /** The plain colour as #RRGGBB text (for the local WebView pages). */
    val hex: String
        get() = String.format(Locale.US, "#%06X", base.toArgb() and 0xFFFFFF)

    /**
     * The same look as a CSS background value for the local WebView pages (notes, quiz, tutor ...), so a page shows the
     * same look as the native screens. Glow sizes use vmax (1% of the longer viewport side), like [BackdropGlow.radius].
     * A picture is a file:///android_asset URL: if a page cannot read it, the plain colour at the end of the list shows
     * instead, so the page never goes blank.
     */
    val cssBackground: String
        get() {
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
 *
 * BG-START (2026-10-23): the change of [version] is now written on the MAIN thread (it used to be written from the
 * decoding thread), and [currentVersion] lets [appBackdrop] read it while COMPOSING, see there.
 */
private object BackdropPhotos {
    private val cache = HashMap<String, ImageBitmap>()
    private val loading = HashSet<String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var version by mutableStateOf(0)

    /** Changes every time a picture finishes loading. Read it while composing to recompose when that happens. */
    val currentVersion: Int
        get() = version

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
            if (bitmap != null) mainHandler.post { version += 1 }
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
 *
 * SEAM FIX (owner 2026-10-19: the picture showed cut into three slices, header, page and bottom bar, joined badly):
 * every screen draws its own copy of the background inside the area between the top bar and the bottom bar, so each
 * copy used to scale the picture to ITS OWN size. With no [option] the picture, shade and glows are now laid out for the
 * WHOLE window (the root layout) and shifted by the position of this box, so every copy shows the matching part of one
 * picture and the pieces line up. With an [option] (a small preview) the old behaviour stays: fitted to the box itself.
 * Before the first layout pass the box does not know its place yet and draws the old way for one frame.
 *
 * BG-START (2026-10-23, owner: at app start the picture only shows in the middle, the header, the status bar and the
 * bottom bar stay black until the day / night choice is touched): what the owner's screenshot shows is that the copy
 * drawn behind the bars (the root copy in Theme.kt) kept the plain colour while the copy in the middle (the loader
 * overlay in MainActivity) had the picture. HYPOTHESIS: the root copy was drawn once before the picture finished
 * decoding and was never told to draw again (the redraw relied on a state read inside the draw block only). Fix: every
 * copy now also reads the picture version while COMPOSING (`photoVersion`), and the draw block uses it, so each copy is
 * recomposed and redrawn when a picture becomes ready. Not seen on a phone.
 */
fun Modifier.appBackdrop(option: AppBackgroundOption? = null): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var screen by remember { mutableStateOf(Size.Zero) }
    // BG-START: read while composing, so this modifier is rebuilt (and redrawn) when a picture has finished loading.
    val photoVersion = BackdropPhotos.currentVersion
    this
        .onGloballyPositioned { coords ->
            val position = coords.positionInRoot()
            val root = coords.findRootCoordinates().size
            val rootSize = Size(root.width.toFloat(), root.height.toFloat())
            if (position != origin) origin = position
            if (rootSize != screen) screen = rootSize
        }
        .drawBehind {
            @Suppress("UNUSED_VARIABLE")
            val loadedPictures = photoVersion
            val opt = option ?: AppBackground.selected
            val aligned = option == null && screen.width > 0f && screen.height > 0f
            if (aligned) {
                clipRect {
                    translate(left = -origin.x, top = -origin.y) {
                        drawBackdrop(opt, screen.width, screen.height)
                    }
                }
            } else {
                drawBackdrop(opt, size.width, size.height)
            }
        }
}

/** Draws the whole look (colour, picture with shade, glows) into a rectangle of [w] x [h] pixels at the origin. */
private fun DrawScope.drawBackdrop(opt: AppBackgroundOption, w: Float, h: Float) {
    val full = Size(w, h)
    drawRect(color = opt.base, size = full)
    val photo = BackdropPhotos.get(opt)
    if (photo != null) {
        // "Cover": scale the picture to fill the rectangle and crop the overflow evenly; High quality filtering keeps it smooth.
        val scale = maxOf(w / photo.width, h / photo.height)
        val srcW = (w / scale).roundToInt().coerceIn(1, photo.width)
        val srcH = (h / scale).roundToInt().coerceIn(1, photo.height)
        drawImage(
            image = photo,
            srcOffset = IntOffset((photo.width - srcW) / 2, (photo.height - srcH) / 2),
            srcSize = IntSize(srcW, srcH),
            dstSize = IntSize(w.roundToInt(), h.roundToInt()),
            filterQuality = FilterQuality.High
        )
        drawRect(color = Color.Black.copy(alpha = PHOTO_SCRIM_ALPHA), size = full)
    }
    val longSide = maxOf(w, h)
    opt.glows.forEach { g ->
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(g.color.copy(alpha = g.alpha), g.color.copy(alpha = 0f)),
                center = Offset(w * g.x, h * g.y),
                radius = longSide * g.radius
            ),
            size = full
        )
    }
}

/**
 * The ONE holder of the chosen background. The choice is saved in SharedPreferences (key [KEY], the same key as before)
 * and exposed as Compose state, so every [AtmosphereBackground] redraws the moment [select] is used. Since D1 it also
 * decides day or night for the whole app: see [AppThemeMode].
 * Default: Transparent (the [AppBackgroundOption.Photo2] picture), since O1 (owner order 2026-10-23: "by default, the
 * transparent mode should be the mode, not dark"). A phone that already saved Night or Day keeps that choice; only a
 * phone with no saved choice, or a saved name that is not offered, gets the default. Only the options in [options] can
 * be chosen or loaded.
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
     * The options in display order: Night, then Day. If day mode is switched off (see [DAY_MODE_OFFERED]) the list is
     * Night, then Transparent again. To try another background, add its enum entry here (the Settings pill grows by
     * itself). Declared BEFORE [state] on purpose.
     */
    val options: List<AppBackgroundOption> =
        if (DAY_MODE_OFFERED) {
            listOf(AppBackgroundOption.Black, AppBackgroundOption.Day, AppBackgroundOption.Photo2)
        } else {
            listOf(AppBackgroundOption.Black, AppBackgroundOption.Photo2)
        }

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
        AppBackdrop.refreshRegisteredPages()
    }

    private fun readSaved(): AppBackgroundOption {
        // O1 (2026-10-23): default is Transparent (Photo2), no longer Night (Black).
        val default = AppBackgroundOption.Photo2
        val app = runCatching { WisdomTowerApplication.instance }.getOrNull() ?: return default
        val name = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY, null)
        return options.firstOrNull { it.name == name } ?: default
    }
}
