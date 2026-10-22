package com.wisdomtower.academy.ui.games.defense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlin.math.sin

/**
 * Wisdom Defense cockpit (docs/GAMES_COOL_PLAN.md slice W3).
 * A small turret sits at the bottom centre; the four answers are bullets in an arch above and around it:
 * A lower left, B upper left, C upper right, D lower right. Tap a bullet to fire it (it lifts off while the shot
 * flies). Press and hold a bullet to read the full answer in a pop-up above the cockpit; letting go closes it and
 * fires nothing. The letters A to D stay on the bullets and the bullets have button semantics (accessibility).
 * Below [COCKPIT_MIN_WIDTH] the screen keeps the old 2x2 cards instead.
 * Pure UI, no engine change: the caller passes engine state in and gets [onFire] back.
 */

/** Narrower than this the screen uses the 2x2 answer cards (the bullets would get too small). */
internal val COCKPIT_MIN_WIDTH = 320.dp

private val COCKPIT_HEIGHT = 144.dp
private val BULLET_HEIGHT = 60.dp

/** Horizontal centre of each bullet as a fraction of the cockpit width (0 = middle). A, B, C, D. */
private val BULLET_CENTERS = floatArrayOf(-0.285f, -0.215f, 0.215f, 0.285f)

@Composable
internal fun DefenseCockpit(
    choices: List<String>,
    locked: Set<Int>,
    firingIndex: Int?,
    shotProgress: Float?,
    clock: Float,
    reduceMotion: Boolean,
    onFire: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var held by remember { mutableStateOf<Int?>(null) }
    // The gesture block below is not restarted on every frame, so it must always call the newest lambda.
    val fireNow by rememberUpdatedState(onFire)

    BoxWithConstraints(modifier.fillMaxWidth().height(COCKPIT_HEIGHT)) {
        val w = maxWidth
        val bulletW = w * 0.40f
        val topY = 4.dp
        val lowY = topY + BULLET_HEIGHT + 10.dp

        // Turret between the two lower bullets.
        Canvas(Modifier.align(Alignment.BottomCenter).size(w * 0.16f, 76.dp)) {
            drawDefenseTurret(shotProgress, clock, reduceMotion)
        }

        for (i in 0..3) {
            val isLocked = i in locked
            val launch = if (firingIndex == i) (shotProgress ?: 0f) else 0f
            val upper = i == 1 || i == 2
            val shape = RoundedCornerShape(24.dp)
            val bg = when {
                isLocked -> Color(0x33222A36)
                launch > 0f -> Color(0xCC14566B)
                else -> Color(0xCC0E3A4A)
            }
            val edge = when {
                isLocked -> GameTheme.wrong.copy(alpha = 0.55f)
                launch > 0f -> Color.White
                else -> WisdomCyan
            }
            val letter = ('A' + i).toString()
            Row(
                modifier = Modifier
                    .offset(x = w / 2 + w * BULLET_CENTERS[i] - bulletW / 2, y = if (upper) topY else lowY)
                    .size(bulletW, BULLET_HEIGHT)
                    .graphicsLayer {
                        translationY = -launch * 16.dp.toPx()
                        alpha = (if (isLocked) 0.55f else 1f) * (1f - 0.5f * launch)
                    }
                    .clip(shape)
                    .background(bg)
                    .border(2.dp, edge, shape)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        onClick {
                            if (!isLocked) fireNow(i)
                            true
                        }
                    }
                    .pointerInput(i, isLocked) {
                        detectTapGestures(
                            onPress = {
                                tryAwaitRelease()
                                if (held == i) held = null
                            },
                            onLongPress = { held = i },
                            onTap = { if (!isLocked) fireNow(i) }
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isLocked) Color(0x33FFFFFF) else WisdomCyan.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLocked) "✕" else letter,
                        color = if (isLocked) GameTheme.wrong else WisdomCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(6.dp))
                Text(
                    text = choices[i],
                    color = if (isLocked) WisdomMuted else WisdomInk,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Press and hold: the full answer, just above the cockpit.
        val h = held
        if (h != null && h in choices.indices) {
            val popShape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { translationY = -size.height - 8.dp.toPx() }
                    .clip(popShape)
                    .background(Color(0xF00E1A2B))
                    .border(1.dp, WisdomCyan.copy(alpha = 0.6f), popShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "${'A' + h}. ${choices[h]}",
                    color = WisdomInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * The turret: a base, a barrel that kicks back when a shot is fired ([shotProgress] 0..1 while it flies, null
 * otherwise), a pulsing core and a short muzzle flash. With [reduceMotion] there is no kick and no pulse.
 */
internal fun DrawScope.drawDefenseTurret(shotProgress: Float?, clock: Float, reduceMotion: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val recoil = if (shotProgress == null || reduceMotion) 0f else (1f - shotProgress * 3f).coerceIn(0f, 1f)
    val flash = if (shotProgress == null) 0f else (1f - shotProgress * 4f).coerceIn(0f, 1f)
    val pulse = if (reduceMotion) 0.5f else 0.5f + 0.5f * sin(clock * 3f)
    val kick = recoil * h * 0.10f

    // base
    drawRoundRect(Color(0xFF16304F), Offset(0f, h * 0.68f), Size(w, h * 0.32f), CornerRadius(10f))
    drawRoundRect(
        WisdomCyan.copy(alpha = 0.55f),
        Offset(0f, h * 0.68f),
        Size(w, h * 0.32f),
        CornerRadius(10f),
        style = Stroke(width = 2f)
    )

    // barrel
    val bw = w * 0.22f
    val top = h * 0.14f + kick
    drawRoundRect(Color(0xFF22507A), Offset(cx - bw / 2f, top), Size(bw, h * 0.62f - kick), CornerRadius(6f))
    drawRoundRect(
        WisdomCyan.copy(alpha = 0.7f),
        Offset(cx - bw / 2f, top),
        Size(bw, h * 0.62f - kick),
        CornerRadius(6f),
        style = Stroke(width = 2f)
    )

    // core
    drawCircle(WisdomCyan.copy(alpha = 0.35f + 0.25f * pulse), w * 0.14f, Offset(cx, h * 0.80f))
    drawCircle(Color(0xFFA5F3FC), w * 0.07f, Offset(cx, h * 0.80f))

    // muzzle flash at the barrel tip
    if (flash > 0f) {
        val r = w * (0.25f + 0.35f * flash)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f * flash), WisdomCyan.copy(alpha = 0.4f * flash), Color.Transparent),
                center = Offset(cx, top),
                radius = r
            ),
            radius = r,
            center = Offset(cx, top)
        )
    }
}
