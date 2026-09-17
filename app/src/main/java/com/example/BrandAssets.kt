package com.example

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import java.io.IOException

/**
 * Official Wisdom Tower brand assets.
 *
 * - Header: static logo.png (from repo root, copied into assets/brand/)
 * - Splash / loading: animation.gif (transparent BG, big on first load, small after)
 *
 * Files live at repo root (logo.png, animation.gif) and are copied into the APK
 * assets by the copyBrandAssets Gradle task.
 */

private object BrandBytes {
    @Volatile private var logoCache: ByteArray? = null
    @Volatile private var loaderCache: ByteArray? = null

    fun logoPng(context: android.content.Context): ByteArray {
        logoCache?.let { return it }
        val bytes = loadAsset(context, "brand/logo.png")
        logoCache = bytes
        return bytes
    }

    fun loaderGif(context: android.content.Context): ByteArray {
        loaderCache?.let { return it }
        val bytes = loadAsset(context, "brand/animation.gif")
        loaderCache = bytes
        return bytes
    }

    private fun loadAsset(context: android.content.Context, path: String): ByteArray {
        return try {
            context.assets.open(path).use { it.readBytes() }
        } catch (e: IOException) {
            ByteArray(0)
        }
    }
}

@Composable
fun rememberBrandImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
            }
            .build()
    }
}

/**
 * Static brand logo for the native app header.
 * Replaces the old generic two-bar Compose canvas mark.
 */
@Composable
fun BrandLogo(
    size: Dp = 34.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val loader = rememberBrandImageLoader()
    val model = remember {
        ImageRequest.Builder(context)
            .data(BrandBytes.logoPng(context))
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = "Wisdom Tower Academy",
        imageLoader = loader,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

/**
 * Animated brand loader GIF (transparent background).
 * Large size for splash / first load; smaller for subsequent page loads.
 */
@Composable
fun BrandLoader(
    size: Dp = 120.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val loader = rememberBrandImageLoader()
    val model = remember {
        ImageRequest.Builder(context)
            .data(BrandBytes.loaderGif(context))
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = "Loading",
        imageLoader = loader,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}
