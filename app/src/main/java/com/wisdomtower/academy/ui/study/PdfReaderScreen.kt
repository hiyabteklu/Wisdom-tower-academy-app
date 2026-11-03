package com.wisdomtower.academy.ui.study

import android.content.Context
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.BookFileStore
import com.wisdomtower.academy.data.content.LearningProgressRepository
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

private const val READER_PREFS = "pdf_reader_state"
private const val MAX_ZOOM = 3f

/**
 * Native book reader (plan step N4): downloads the PDF once (kept on the phone, so it opens offline later),
 * then draws the pages with Android's built-in `PdfRenderer`, one after the other, scrolling down.
 * Texts "Could not load this PDF." comes from the website's `HubContentView`.
 *
 * Last page (N4 part 2): when [resourceId] is given, the reader opens at the page the student reached last
 * (the copy kept on this phone first, so it also works offline; if there is none, the `last_page` saved in
 * `learning_progress` by another device or an earlier install), and saves the page, the percent read and the
 * reading seconds a few seconds after the page changes and when the reader closes (signed-in students only;
 * a signed-out student still gets the phone copy).
 *
 * Zoom (N4 part 3): pinch with two fingers to zoom from 1x to 3x, drag sideways to move across a zoomed page
 * (up and down is the normal scroll), double tap to zoom in to 2x or back to 1x. Pages are drawn at 1.5x the
 * screen width so a zoomed page stays reasonably sharp; at 3x it is softer (hypothesis, not tested).
 *
 * D6c (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. The grey box shown while a page is still drawing is a [WisdomInk] tint
 * (white 6 percent at night). The page itself stays white on purpose (it is the book), and the "Page n / m" pill keeps its
 * dark fill with white text in both modes because it sits on top of a white page.
 *
 * Owner request 2026-10-24: NO top row any more (back arrow + book title). The pages start right under the app bar and use
 * the whole height. Leaving is done with the phone Back button / gesture (handled by `StudyWorkspaceScreen`, it returns to the
 * list). [onBack] and [title] stay in the signature but are not drawn.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun PdfReaderScreen(
    title: String,
    url: String?,
    cacheKey: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    resourceId: String? = null
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf<Float?>(null) }
    var file by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(url, cacheKey, attempt) {
        error = null
        file = null
        progress = null
        if (url == null) {
            error = "Could not load this PDF."
            return@LaunchedEffect
        }
        try {
            file = BookFileStore.ensureFile(context, cacheKey, url) { progress = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            error = "Check your internet connection and try again."
        } catch (e: Exception) {
            error = "Could not load this PDF."
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            val readyFile = file
            val failure = error
            when {
                readyFile != null -> PdfPages(readyFile, resourceId, Modifier.fillMaxSize())

                failure != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = failure, fontSize = 13.sp, color = WisdomMuted)
                    if (url != null) {
                        Spacer(modifier = Modifier.size(14.dp))
                        Box(
                            modifier = Modifier.wisdomTouchFeel()
                                .clip(RoundedCornerShape(10.dp))
                                .background(WisdomCyan.copy(alpha = 0.12f))
                                .clickable { attempt += 1 }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(text = "Retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WisdomCyan)
                        }
                    }
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val p = progress
                    DownloadGauge(
                        state = DownloadState.DOWNLOADING,
                        progress = p,
                        size = 36.dp,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (p != null) "Downloading book\u2026 ${(p.coerceIn(0f, 1f) * 100).toInt()}%" else "Loading book\u2026",
                        fontSize = 13.sp,
                        color = WisdomMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPages(file: File, resourceId: String?, modifier: Modifier) {
    var renderer by remember(file) { mutableStateOf<PdfRenderer?>(null) }
    var failed by remember(file) { mutableStateOf(false) }
    val mutex = remember(file) { Mutex() }
    val cache = remember(file) {
        val kb = (Runtime.getRuntime().maxMemory() / 1024L / 6L).toInt().coerceAtLeast(16 * 1024)
        object : LruCache<Int, Bitmap>(kb) {
            override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount / 1024
        }
    }

    DisposableEffect(file) {
        var opened: PdfRenderer? = null
        var descriptor: ParcelFileDescriptor? = null
        try {
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            opened = PdfRenderer(descriptor)
            renderer = opened
        } catch (e: Exception) {
            failed = true
            runCatching { descriptor?.close() }
        }
        onDispose {
            renderer = null
            runCatching { opened?.close() }
            runCatching { descriptor?.close() }
            cache.evictAll()
        }
    }

    val ready = renderer
    if (failed) {
        Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text(text = "Could not load this PDF.", fontSize = 13.sp, color = WisdomMuted)
        }
        return
    }
    if (ready == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WisdomCyan)
        }
        return
    }

    val pageCount = ready.pageCount
    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
    val density = LocalDensity.current
    val viewWidthPx = (LocalConfiguration.current.screenWidthDp * density.density).coerceAtLeast(240f)
    // Pages are drawn 1.5x wider than the screen so that a zoomed page is still fairly sharp.
    val renderWidthPx = (viewWidthPx * 1.5f).toInt().coerceAtMost(1800)

    // --- Zoom (N4 part 3) -------------------------------------------------------------------------------
    var zoom by remember(file) { mutableFloatStateOf(1f) }
    var panX by remember(file) { mutableFloatStateOf(0f) }
    val pageWidthDp = with(density) { (viewWidthPx * zoom).toDp() }

    // --- Last page + progress (N4 part 2) ---------------------------------------------------------------
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(READER_PREFS, Context.MODE_PRIVATE) }
    // false until the saved page has been restored, so the first screen (page 1) never overwrites it
    var restored by remember(file) { mutableStateOf(false) }
    val sessionStart = remember(file) { longArrayOf(System.currentTimeMillis()) }
    val latestPage = rememberUpdatedState(currentPage)
    val latestRestored = rememberUpdatedState(restored)

    LaunchedEffect(file, pageCount) {
        val id = resourceId
        if (id != null) {
            val local = prefs.getInt("last_page_$id", 0)
            if (local in 2..pageCount) {
                listState.scrollToItem(local - 1)
            } else if (local == 0) {
                val remote = LearningProgressRepository.getLastPage(id)
                if (remote != null && remote in 2..pageCount &&
                    listState.firstVisibleItemIndex == 0 && !listState.isScrollInProgress
                ) {
                    listState.scrollToItem(remote - 1)
                }
            }
        }
        restored = true
    }

    LaunchedEffect(currentPage, restored) {
        val id = resourceId
        if (!restored || id == null) return@LaunchedEffect
        // Wait a moment: a quick fling over many pages is saved once, at the page where it stopped.
        delay(400)
        prefs.edit().putInt("last_page_$id", currentPage).apply()
        delay(2100)
        saveReading(id, currentPage, pageCount, sessionStart)
    }

    DisposableEffect(file, resourceId) {
        onDispose {
            val id = resourceId
            if (id != null && latestRestored.value) {
                val page = latestPage.value
                prefs.edit().putInt("last_page_$id", page).apply()
                // The screen is closing, so use a scope that is not cancelled with it.
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    saveReading(id, page, pageCount, sessionStart)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            // Double tap: 1x -> 2x -> 1x. Does not use up any other touch.
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (zoom > 1f) {
                            zoom = 1f
                            panX = 0f
                        } else {
                            zoom = 2f
                        }
                    }
                )
            }
            // Two fingers: zoom + move. One finger: only sideways movement while zoomed, never used up,
            // so the list still scrolls up and down.
            .pointerInput(viewWidthPx) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            val newZoom = (zoom * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            val pan = event.calculatePan()
                            zoom = newZoom
                            panX = (panX + pan.x).coerceIn(-(viewWidthPx * (newZoom - 1f)), 0f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pressed == 1 && zoom > 1f) {
                            val pan = event.calculatePan()
                            panX = (panX + pan.x).coerceIn(-(viewWidthPx * (zoom - 1f)), 0f)
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(count = pageCount, key = { it }) { index ->
                // The page is as wide as the screen times the zoom; panX slides it sideways.
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                    PdfPageImage(
                        index = index,
                        renderer = ready,
                        mutex = mutex,
                        renderWidthPx = renderWidthPx,
                        cache = cache,
                        modifier = Modifier
                            .requiredWidth(pageWidthDp)
                            .offset { IntOffset(panX.roundToInt(), 0) }
                    )
                }
            }
        }
        Text(
            text = "Page $currentPage / $pageCount",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x4D111B2E).copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * Sends the reading state to `learning_progress`: page, percent (page / pages, the server keeps the higher
 * value) and the seconds since the last save (at most 10 minutes per save, so a phone left open does not
 * count as reading). Does nothing when the student is signed out.
 */
private suspend fun saveReading(resourceId: String, page: Int, pageCount: Int, sessionStart: LongArray) {
    val now = System.currentTimeMillis()
    val seconds = ((now - sessionStart[0]) / 1000L).toInt().coerceIn(0, 600)
    sessionStart[0] = now
    val percent = (page * 100 / pageCount.coerceAtLeast(1)).coerceIn(0, 100)
    LearningProgressRepository.saveProgress(
        resourceId = resourceId,
        progressPct = percent,
        addSeconds = seconds,
        lastPage = page
    )
}

@Composable
private fun PdfPageImage(
    index: Int,
    renderer: PdfRenderer,
    mutex: Mutex,
    renderWidthPx: Int,
    cache: LruCache<Int, Bitmap>,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = cache.get(index), index, renderer, renderWidthPx) {
        if (value == null) {
            val rendered = withContext(Dispatchers.IO) {
                mutex.withLock { renderPage(renderer, index, renderWidthPx) }
            }
            if (rendered != null) cache.put(index, rendered)
            value = rendered
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .aspectRatio(0.707f)
                .background(WisdomInk.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = WisdomCyan, modifier = Modifier.size(24.dp))
        }
    }
}

/** Draws one page, white background, [widthPx] wide. Returns null when the page cannot be drawn. */
private fun renderPage(renderer: PdfRenderer, index: Int, widthPx: Int): Bitmap? = try {
    val page = renderer.openPage(index)
    try {
        val height = (widthPx * page.height.toFloat() / page.width.toFloat()).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bmp
    } finally {
        page.close()
    }
} catch (e: Exception) {
    null
}
