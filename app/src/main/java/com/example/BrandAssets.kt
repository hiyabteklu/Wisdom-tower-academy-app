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

/**
 * Official Wisdom Tower brand assets.
 *
 * Logo + loader GIF are loaded from the repo (public raw URLs) so they
 * work without extra Gradle copy tasks. Coil caches them on disk after
 * the first load.
 *
 * Files on main: logo.png, animation.gif
 */
private const val LOGO_URL =
    "https://raw.githubusercontent.com/hiyabteklu/Wisdom-tower-academy-app/main/logo.png"
private const val LOADER_URL =
    "https://raw.githubusercontent.com/hiyabteklu/Wisdom-tower-academy-app/main/animation.gif"

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

/** Static brand logo for the native app header. */
@Composable
fun BrandLogo(
    size: Dp = 34.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val loader = rememberBrandImageLoader()
    val model = remember {
        ImageRequest.Builder(context)
            .data(LOGO_URL)
            .crossfade(true)
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
 * Large on first load / splash; smaller for subsequent page loads.
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
            .data(LOADER_URL)
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
