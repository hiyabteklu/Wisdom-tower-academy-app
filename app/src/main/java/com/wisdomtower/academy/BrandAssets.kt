package com.wisdomtower.academy

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * Universal high-speed centered circular animated loader.
 */
@Composable
fun CustomCenteredLoader(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        FuturisticGearRings(
            size = 110.dp,
            brandSize = 64.dp,
            numTicks = 16,
            tickInnerRatio = 0.68f,
            tickOuterRatio = 0.88f,
            tickWidth = 3.dp
        )
    }
}

/**
 * High-speed dual orbital neon rings with outward thick gear ticks / teeth rotating between them.
 */
@Composable
fun FuturisticGearRings(
    size: Dp,
    brandSize: Dp,
    numTicks: Int,
    tickInnerRatio: Float,
    tickOuterRatio: Float,
    tickWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val transition = rememberInfiniteTransition(label = "futuristicGear")
        val fastSpin by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fastSpin"
        )
        val gearSpin by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gearSpin"
        )
        val counterSpin by transition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(950, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "counterSpin"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f - 3.dp.toPx()
            val innerRadius = outerRadius * 0.62f
            val rGearInner = outerRadius * tickInnerRatio
            val rGearOuter = outerRadius * tickOuterRatio
            val tickPx = tickWidth.toPx()

            // 1. Futuristic Gear base track circle
            drawCircle(
                color = Color(0x3300E5FF),
                radius = rGearInner,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Outward thick gear teeth / ticks spinning fast
            for (i in 0 until numTicks) {
                val tickAngle = gearSpin + (i * 360f / numTicks)
                val rad = Math.toRadians(tickAngle.toDouble())
                val cos = Math.cos(rad).toFloat()
                val sin = Math.sin(rad).toFloat()
                val p1 = Offset(centerOffset.x + rGearInner * cos, centerOffset.y + rGearInner * sin)
                val p2 = Offset(centerOffset.x + rGearOuter * cos, centerOffset.y + rGearOuter * sin)
                val tickColor = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFF38BDF8)
                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = tickPx,
                    cap = StrokeCap.Round
                )
            }

            // 3. Outer Neon Cyan Arc spinning fast (clockwise)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0x0000E5FF),
                        Color(0x5500E5FF),
                        Color(0xFF00E5FF),
                        Color(0xFF38BDF8)
                    )
                ),
                startAngle = fastSpin,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(centerOffset.x - outerRadius, centerOffset.y - outerRadius),
                size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Inner Neon Violet Arc spinning fast in opposite direction (counter-clockwise)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0x00818CF8),
                        Color(0x55818CF8),
                        Color(0xFF818CF8),
                        Color(0xFF00E5FF)
                    )
                ),
                startAngle = counterSpin,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(centerOffset.x - innerRadius, centerOffset.y - innerRadius),
                size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Brand animated GIF in center
        BrandLoader(size = brandSize, showCard = false)
    }
}
