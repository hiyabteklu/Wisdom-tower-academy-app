package com.wisdomtower.academy.ui.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomCyan

// D6k (docs/DAY_MODE_PLAN.md, 2026-10-19): the saved check mark and the retry icon are icons and need 3:1 against the
// ivory cards. Night = the exact old literals (34D399, F87171). Day = the darker emerald / rose tokens of the day palette.
// Getters, so they are read while composing (and follow the switch).
private val GaugeDoneColor: Color get() = if (AppThemeMode.isDark) Color(0xFF34D399) else WisdomAccentEmerald
private val GaugeFailedColor: Color get() = if (AppThemeMode.isDark) Color(0xFFF87171) else WisdomAccentRose

/**
 * Download states for offline learning materials (books, notes, flashcards, exams, question banks).
 */
enum class DownloadState {
    IDLE,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

/**
 * T5 (2026-10-12): Real circular download gauge.
 *
 * Owner words:
 * "The download progress for all: a real circular gauge, not big, just as it is the spinning
 * circle there, but instead of spinning indefinitely it shows the real progress and the percent
 * inside the circle until it changes into a check mark."
 *
 * - When [state] is [DownloadState.DOWNLOADING]:
 *   - If [progress] is known (0f..1f): shows a determinate circular gauge with the percent
 *     (e.g. "45%") inside the circle.
 *   - If [progress] is unknown (null or < 0f): shows an indeterminate spinner with NO percent (G4).
 * - When [state] is [DownloadState.DOWNLOADED]: turns into the existing green check mark.
 * - When [state] is [DownloadState.FAILED]: turns into a clear red retry icon.
 * - When [state] is [DownloadState.IDLE]: shows the standard download arrow.
 *
 * M6 (docs/UI_MOTION_PLAN.md): the arc EASES (250 ms) to the real progress instead of jumping (the percent text always shows
 * the real value, it never counts up on its own), and the check mark POPS with a short spring, only when a download that
 * was running has just finished (items that are already saved when the list opens do not pop).
 *
 * D6k (day mode): only the two default colours of the check mark and the retry icon changed, see [GaugeDoneColor] and
 * [GaugeFailedColor]. Night is identical. The arc, the percent and the arrow use [WisdomCyan], which already follows the switch.
 */
@Composable
fun DownloadGauge(
    state: DownloadState,
    progress: Float?,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    strokeWidth: Dp = 2.dp,
    color: Color = WisdomCyan,
    downloadedColor: Color = GaugeDoneColor,
    failedColor: Color = GaugeFailedColor,
    trackColor: Color = WisdomCyan.copy(alpha = 0.2f)
) {
    val iconSize = if (size > 22.dp) 22.dp else size

    // M6: the drawn arc follows the real value with a short ease
    val shownProgress by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(250),
        label = "gaugeProgress"
    )

    // M6: check mark pop, only after a run that was seen downloading
    val pop = remember { Animatable(1f) }
    var wasDownloading by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == DownloadState.DOWNLOADING) {
            wasDownloading = true
        } else if (state == DownloadState.DOWNLOADED && wasDownloading) {
            wasDownloading = false
            pop.snapTo(0.55f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            DownloadState.DOWNLOADED -> {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Saved offline. Tap to remove",
                    tint = downloadedColor,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        }
                )
            }

            DownloadState.FAILED -> {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Download failed. Tap to retry",
                    tint = failedColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            DownloadState.IDLE -> {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download for offline use",
                    tint = color,
                    modifier = Modifier.size(iconSize)
                )
            }

            DownloadState.DOWNLOADING -> {
                if (progress != null && progress >= 0f) {
                    val p = progress.coerceIn(0f, 1f)
                    val pct = (p * 100).toInt()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val drawn = shownProgress
                        val strokePx = strokeWidth.toPx()
                        val halfStroke = strokePx / 2f
                        val arcSize = Size(
                            this.size.width - strokePx,
                            this.size.height - strokePx
                        )
                        val topLeft = Offset(halfStroke, halfStroke)
                        // Background track circle
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx)
                        )
                        // Determinate progress arc (eased toward the real value)
                        if (drawn > 0f) {
                            drawArc(
                                color = color,
                                startAngle = -90f,
                                sweepAngle = 360f * drawn,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
                    if (size >= 18.dp) {
                        Text(
                            text = "$pct%",
                            fontSize = if (pct >= 100) 12.sp else if (size >= 24.dp) 12.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            maxLines = 1
                        )
                    }
                } else {
                    // Total unknown: keep indefinite spinner and show NO percent (rule G4)
                    CircularProgressIndicator(
                        modifier = Modifier.size(size),
                        color = color,
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}

/**
 * Convenience overload deriving [DownloadState] from flags.
 */
@Composable
fun DownloadGauge(
    isDownloaded: Boolean,
    progress: Float?,
    failed: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    strokeWidth: Dp = 2.dp,
    color: Color = WisdomCyan,
    downloadedColor: Color = GaugeDoneColor,
    failedColor: Color = GaugeFailedColor,
    trackColor: Color = WisdomCyan.copy(alpha = 0.2f)
) {
    val state = when {
        isDownloaded -> DownloadState.DOWNLOADED
        progress != null -> DownloadState.DOWNLOADING
        failed -> DownloadState.FAILED
        else -> DownloadState.IDLE
    }
    DownloadGauge(
        state = state,
        progress = progress,
        modifier = modifier,
        size = size,
        strokeWidth = strokeWidth,
        color = color,
        downloadedColor = downloadedColor,
        failedColor = failedColor,
        trackColor = trackColor
    )
}
