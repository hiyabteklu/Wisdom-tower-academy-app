package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.GifDecoder
import coil.request.ImageRequest
import java.io.IOException

/**
 * Brand assets from APK assets/brand/ (instant, offline-safe).
 * logo.png + animation.gif copied from repo root at build time.
 */
object BrandBytes {
    @Volatile private var logoCache: ByteArray? = null
    @Volatile private var gifCache: ByteArray? = null

    fun preload(context: android.content.Context) {
        if (logoCache == null) {
            logoCache = read(context, "brand/logo.png")
        }
        if (gifCache == null) {
            gifCache = read(context, "brand/animation.gif")
        }
    }

    fun logo(context: android.content.Context): ByteArray {
        logoCache?.let { return it }
        val b = read(context, "brand/logo.png")
        logoCache = b
        return b
    }

    fun gif(context: android.content.Context): ByteArray {
        gifCache?.let { return it }
        val b = read(context, "brand/animation.gif")
        gifCache = b
        return b
    }

    private fun read(context: android.content.Context, path: String): ByteArray {
        return try {
            context.assets.open(path).use { it.readBytes() }
        } catch (_: IOException) {
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .build()
    }
}

@Composable
fun WisdomTowerGifLoader(
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    isAmoled: Boolean = false,
) {
    val context = LocalContext.current
    val loader = rememberBrandImageLoader()
    val model = remember(context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/brand/animation.gif")
            .memoryCacheKey("brand_animation_gif")
            .crossfade(false)
            .allowHardware(true)
            .build()
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = "Wisdom Tower Academy Loading",
            imageLoader = loader,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size),
        )
    }
}

/** Header logo as a rounded square. */
@Composable
fun BrandLogo(
    size: Dp = 34.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val loader = rememberBrandImageLoader()
    val model = remember {
        ImageRequest.Builder(context)
            .data(BrandBytes.logo(context))
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = "Wisdom Tower Academy",
        imageLoader = loader,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp)),
    )
}

/**
 * Animated brand loader. Soft card only appears after the GIF is decoded
 * so the empty card never flashes before the animation.
 * Pass showCard = false when the parent already provides a card surface.
 */
@Composable
fun BrandLoader(
    size: Dp = 120.dp,
    modifier: Modifier = Modifier,
    showCard: Boolean = true,
) {
    val context = LocalContext.current
    val loader = rememberBrandImageLoader()
    val model = remember(context) {
        val gifBytes = BrandBytes.gif(context)
        val dataSrc: Any = if (gifBytes.isNotEmpty()) gifBytes else "file:///android_asset/brand/animation.gif"
        ImageRequest.Builder(context)
            .data(dataSrc)
            .memoryCacheKey("brand_animation_gif")
            .crossfade(false)
            .allowHardware(true)
            .build()
    }
    var ready by remember { mutableStateOf(true) }
    val pad = if (size > 100.dp) 28.dp else 16.dp
    val radius = if (size > 100.dp) 28.dp else 20.dp

    Box(
        modifier = modifier.then(
            if (showCard && ready) {
                Modifier
                    .clip(RoundedCornerShape(radius))
                    .background(Color(0xE6111827))
                    .padding(pad)
            } else {
                Modifier
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = "Loading",
            imageLoader = loader,
            contentScale = ContentScale.Fit,
            onState = { state ->
                if (state is AsyncImagePainter.State.Success) ready = true
            },
            modifier = Modifier.size(size),
        )
    }
}
