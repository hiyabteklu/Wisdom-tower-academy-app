package com.example

import android.util.Base64
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
 * Logo (static PNG) for header; animated GIF for splash / loading overlays.
 * Base64 files live in assets/brand/ so the app works fully offline.
 */

private object BrandBytes {
    @Volatile private var logoCache: ByteArray? = null
    @Volatile private var loaderCache: ByteArray? = null

    fun logoPng(context: android.content.Context): ByteArray {
        logoCache?.let { return it }
        val bytes = loadAssetB64(context, "brand/logo.b64")
        logoCache = bytes
        return bytes
    }

    fun loaderGif(context: android.content.Context): ByteArray {
        loaderCache?.let { return it }
        val bytes = loadAssetB64(context, "brand/loader.b64")
        loaderCache = bytes
        return bytes
    }

    private fun loadAssetB64(context: android.content.Context, path: String): ByteArray {
        return try {
            context.assets.open(path).use { input ->
                val b64 = input.bufferedReader().readText().trim()
                Base64.decode(b64, Base64.DEFAULT)
            }
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
 * Source: public/images/brand/logo.png
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
