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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.padding
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Wisdom Defense cockpit (docs/GAMES_COOL_PLAN.md slice W3, rebuilt 2026-10-24).
 *
 * Layout: ONE cannon in the middle, two answers on its left (A over B) and two on its right (C over D). The two sides
 * are exact mirrors of each other (same size, cut corners mirrored, letter chip on the outer edge). Thin energy
 * cables run from the answer panels to the cannon base. Tap an answer to fire it (the panel lifts and flashes while the
 * shot flies). Press and hold an answer to read the full text in a pop-up above the cockpit; letting go closes it and
 * fires nothing. The answers keep button semantics (accessibility).
 *
 * The cannon is the only gun in the game. Its barrel is taller than the cockpit and reaches up over the wall into the
 * battlefield (it is drawn after the battlefield, so it covers it). The barrel turns toward the nearest enemy (the
 * screen writes the angle into [aim]), kicks back and flashes when a shot flies, and the shot is drawn from the barrel
 * tip ([defenseCannonTip]). Below [COCKPIT_MIN_WIDTH] the screen keeps the old 2x2 cards instead.
 * Pure UI, no engine change: the caller passes engine state in and gets [onFire] back.
 */

/** Narrower than this the screen uses the 2x2 answer cards (the panels would get too small). */
internal val COCKPIT_MIN_WIDTH = 320.dp

/** Space the screen leaves between the battlefield and the cockpit (the screen uses it as top padding). */
internal val COCKPIT_TOP_PADDING = 8.dp

private val COCKPIT_HEIGHT = 146.dp
private val BUTTON_HEIGHT = 60.dp
private val BUTTON_GAP = 8.dp

/** How far the barrel reaches above the top of the cockpit. */
private val CANNON_OVERHANG = 46.dp

/** Barrel pivot, measured down from the top of the overhang (so 42 dp below the top of the cockpit). */
private val CANNON_PIVOT_Y = 88.dp

/** Barrel length from the pivot to the muzzle. */
private val CANNON_BARREL_LEN = 84.dp

/** The barrel never turns more than this many degrees to either side. */
private const val CANNON_MAX_AIM = 25f

/** Width of each side column of answers as a fraction of the cockpit width. */
private const val SIDE_FRACTION = 0.385f

/**
 * Barrel pivot in battlefield canvas coordinates ([w] x [h] is the battlefield canvas). It sits below the canvas, in
 * the cockpit, because the canvas is directly above the cockpit (which starts [COCKPIT_TOP_PADDING] lower).
 */
internal fun DrawScope.defenseCannonPivot(w: Float, h: Float): Offset =
    Offset(w * 0.5f, h + (COCKPIT_TOP_PADDING - CANNON_OVERHANG + CANNON_PIVOT_Y).toPx())

/** Muzzle position in battlefield canvas coordinates for a barrel turned [aimDeg] degrees (0 = straight up). */
internal fun DrawScope.defenseCannonTip(w: Float, h: Float, aimDeg: Float): Offset {
    val p = defenseCannonPivot(w, h)
    val a = aimDeg * 0.017453292f
    val len = CANNON_BARREL_LEN.toPx()
    return Offset(p.x + sin(a) * len, p.y - cos(a) * len)
}

/** Angle in degrees (0 = straight up, positive = to the right) from the pivot to a point, kept within the aim limit. */
internal fun defenseAimDegrees(pivot: Offset, targetX: Float, targetY: Float): Float {
    val dx = targetX - pivot.x
    val dy = pivot.y - targetY
    val deg = atan2(dx, dy) * 57.29578f
    return deg.coerceIn(-CANNON_MAX_AIM, CANNON_MAX_AIM)
}

@Composable
internal fun DefenseCockpit(
    choices: List<String>,
    locked: Set<Int>,
    firingIndex: Int?,
    shotProgress: Float?,
    clock: Float,
    reduceMotion: Boolean,
    onFire: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** One float, the current barrel angle in degrees. The battlefield canvas writes it; the cannon reads it. */
    aim: FloatArray = FloatArray(1)
) {
    var held by remember { mutableStateOf<Int?>(null) }
    // The gesture block below is not restarted on every frame, so it must always call the newest lambda.
    val fireNow by rememberUpdatedState(onFire)

    BoxWithConstraints(modifier.fillMaxWidth().height(COCKPIT_HEIGHT)) {
        val w = maxWidth
        val sideW = w * SIDE_FRACTION
        val centerW = w - sideW - sideW
        val cannonW = centerW - 4.dp
        val topY = (COCKPIT_HEIGHT - BUTTON_HEIGHT - BUTTON_HEIGHT - BUTTON_GAP) / 2

        // Console behind everything: metal plate and energy cables.
        Canvas(Modifier.matchParentSize()) {
            drawCockpitConsole(clock, reduceMotion, topY.toPx(), BUTTON_HEIGHT.toPx(), BUTTON_GAP.toPx())
        }

        for (i in 0..3) {
            val right = i >= 2
            val row = i % 2
            val isLocked = i in locked
            val launch = if (firingIndex == i) (shotProgress ?: 0f) else 0f
            val shape = if (right) {
                CutCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomEnd = 12.dp, bottomStart = 0.dp)
            } else {
                CutCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 0.dp, bottomStart = 12.dp)
            }
            val bgBrush = when {
                isLocked -> Brush.verticalGradient(listOf(Color(0x44222A36), Color(0x44141A24)))
                launch > 0f -> Brush.verticalGradient(listOf(Color(0xEE2A7C99), Color(0xEE14566B)))
                else -> Brush.verticalGradient(listOf(Color(0xE61B4A66), Color(0xE60B2A3C)))
            }
            val pulse = if (reduceMotion) 0.5f else 0.5f + 0.5f * sin(clock * 2.4f + i)
            val edge = when {
                isLocked -> GameTheme.wrong.copy(alpha = 0.55f)
                launch > 0f -> Color.White
                else -> WisdomCyan.copy(alpha = 0.60f + 0.30f * pulse)
            }
            val letter = ('A' + i).toString()
            Row(
                modifier = Modifier
                    .offset(x = if (right) w - sideW else 0.dp, y = if (row == 0) topY else topY + BUTTON_HEIGHT + BUTTON_GAP)
                    .size(sideW, BUTTON_HEIGHT)
                    .graphicsLayer {
                        translationY = -launch * 8.dp.toPx()
                        alpha = (if (isLocked) 0.55f else 1f) * (1f - 0.4f * launch)
                    }
                    .clip(shape)
                    .background(brush = bgBrush, shape = shape)
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
                // Left side: chip then text. Right side is the mirror: text then chip.
                if (!right) {
                    AnswerChip(letter, isLocked)
                    Spacer(Modifier.size(6.dp))
                }
                Text(
                    text = choices[i],
                    color = if (isLocked) WisdomMuted else WisdomInk,
                    fontSize = 11.5.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (right) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                if (right) {
                    Spacer(Modifier.size(6.dp))
                    AnswerChip(letter, isLocked)
                }
            }
        }

        // The cannon. Its canvas is as tall as the cockpit; the barrel is drawn above the canvas (Compose does not clip
        // a Canvas), over the bottom of the battlefield.
        Canvas(Modifier.align(Alignment.BottomCenter).size(cannonW, COCKPIT_HEIGHT)) {
            drawDefenseCannon(shotProgress, clock, reduceMotion, aim[0])
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

@Composable
private fun AnswerChip(letter: String, locked: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (locked) Color(0x33FFFFFF) else WisdomCyan.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (locked) "\u2715" else letter,
            color = if (locked) GameTheme.wrong else WisdomCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * The console plate behind the answers: dark metal with a glowing rim, and two energy cables from the answer panels
 * to the cannon base with a bright pulse that travels toward the cannon.
 */
private fun DrawScope.drawCockpitConsole(
    clock: Float,
    reduceMotion: Boolean,
    topY: Float,
    buttonH: Float,
    gap: Float
) {
    val w = size.width
    val h = size.height
    val d = 1.dp.toPx()
    val r = 18f * d
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF111C2E), Color(0xFF0A1220))),
        cornerRadius = CornerRadius(r)
    )
    drawRoundRect(
        color = WisdomCyan.copy(alpha = 0.30f),
        cornerRadius = CornerRadius(r),
        style = Stroke(width = 1.5f * d)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.05f),
        topLeft = Offset(3f * d, 3f * d),
        size = Size(w - 6f * d, h - 6f * d),
        cornerRadius = CornerRadius(r - 3f * d),
        style = Stroke(width = 1f * d)
    )

    // Cables at the middle height of both answer rows, both sides.
    val ys = floatArrayOf(topY + buttonH / 2f, topY + buttonH + gap + buttonH / 2f)
    val leftX = w * SIDE_FRACTION
    val rightX = w * (1f - SIDE_FRACTION)
    val midX = w * 0.5f
    val stop = w * 0.09f
    for ((k, y) in ys.withIndex()) {
        drawLine(WisdomCyan.copy(alpha = 0.22f), Offset(leftX, y), Offset(midX - stop, y), strokeWidth = 2f * d)
        drawLine(WisdomCyan.copy(alpha = 0.22f), Offset(rightX, y), Offset(midX + stop, y), strokeWidth = 2f * d)
        if (!reduceMotion) {
            val t = (clock * 0.7f + k * 0.5f) % 1f
            val lx = leftX + (midX - stop - leftX) * t
            val rx = rightX - (rightX - midX - stop) * t
            val glow = Color(0xFFA5F3FC).copy(alpha = 0.9f * (1f - 0.5f * t))
            drawCircle(glow, 2.5f * d, Offset(lx, y))
            drawCircle(glow, 2.5f * d, Offset(rx, y))
        }
    }
}

/**
 * The cannon, drawn in a canvas [size] wide and [COCKPIT_HEIGHT] tall; the barrel goes above the canvas.
 * Parts: lit base plate with bolts, angular housing with cylinder shading, vents and a pulsing core, then the barrel
 * (turned [aimDeg] around the pivot) with a mantlet, energy rings and a muzzle brake. While a shot flies ([shotProgress]
 * 0..1, null otherwise) the barrel kicks back and the muzzle flashes. With [reduceMotion] there is no kick and no pulse.
 */
internal fun DrawScope.drawDefenseCannon(shotProgress: Float?, clock: Float, reduceMotion: Boolean, aimDeg: Float) {
    val d = 1.dp.toPx()
    val cw = size.width
    val ch = size.height
    val cx = cw / 2f
    val pivot = Offset(cx, (CANNON_PIVOT_Y - CANNON_OVERHANG).toPx())
    val barrelLen = CANNON_BARREL_LEN.toPx()
    val recoil = if (shotProgress == null || reduceMotion) 0f else (1f - shotProgress * 3f).coerceIn(0f, 1f)
    val flash = if (shotProgress == null) 0f else (1f - shotProgress * 4f).coerceIn(0f, 1f)
    val pulse = if (reduceMotion) 0.5f else 0.5f + 0.5f * sin(clock * 3f)
    val kick = recoil * 9f * d

    // Glow on the floor under the gun.
    val glowC = Offset(cx, ch - 20f * d)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WisdomCyan.copy(alpha = 0.22f + 0.10f * pulse), WisdomCyan.copy(alpha = 0f)),
            center = glowC,
            radius = cw * 0.75f
        ),
        radius = cw * 0.75f,
        center = glowC
    )

    // Base plate.
    val baseTop = ch - 34f * d
    val base = Path().apply {
        moveTo(cx - cw * 0.40f, baseTop)
        lineTo(cx + cw * 0.40f, baseTop)
        lineTo(cx + cw * 0.50f, ch - 2f * d)
        lineTo(cx - cw * 0.50f, ch - 2f * d)
        close()
    }
    drawPath(base, Brush.verticalGradient(listOf(Color(0xFF1C3552), Color(0xFF0B1626)), startY = baseTop, endY = ch))
    drawPath(base, WisdomCyan.copy(alpha = 0.55f), style = Stroke(width = 1.5f * d))
    for (sx in floatArrayOf(-0.38f, -0.18f, 0.18f, 0.38f)) {
        drawCircle(Color(0xFF8FB3D1).copy(alpha = 0.7f), 1.6f * d, Offset(cx + cw * sx, baseTop + 12f * d))
    }
    drawLine(
        WisdomCyan.copy(alpha = 0.35f + 0.25f * pulse),
        Offset(cx - cw * 0.34f, baseTop + 22f * d),
        Offset(cx + cw * 0.34f, baseTop + 22f * d),
        strokeWidth = 2f * d
    )

    // Housing.
    val hTop = pivot.y + 4f * d
    val hBot = baseTop + 2f * d
    val hh = hBot - hTop
    val housing = Path().apply {
        moveTo(cx - cw * 0.26f, hTop)
        lineTo(cx + cw * 0.26f, hTop)
        lineTo(cx + cw * 0.44f, hTop + hh * 0.30f)
        lineTo(cx + cw * 0.40f, hBot)
        lineTo(cx - cw * 0.40f, hBot)
        lineTo(cx - cw * 0.44f, hTop + hh * 0.30f)
        close()
    }
    drawPath(
        housing,
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF0D1B2E), Color(0xFF2F628F), Color(0xFF0D1B2E)),
            startX = cx - cw * 0.44f,
            endX = cx + cw * 0.44f
        )
    )
    drawPath(housing, WisdomCyan.copy(alpha = 0.6f), style = Stroke(width = 1.5f * d))
    for (s in floatArrayOf(-1f, 1f)) {
        drawLine(
            Color.White.copy(alpha = 0.12f),
            Offset(cx + s * cw * 0.17f, hTop + hh * 0.34f),
            Offset(cx + s * cw * 0.17f, hBot),
            strokeWidth = 1f * d
        )
        for (k in 0..2) {
            val y = hTop + hh * (0.42f + 0.13f * k)
            drawLine(
                Color(0xFF06101C),
                Offset(cx + s * cw * 0.36f, y),
                Offset(cx + s * cw * 0.24f, y),
                strokeWidth = 2f * d
            )
        }
    }
    // Core.
    val coreC = Offset(cx, hTop + hh * 0.66f)
    drawCircle(WisdomCyan.copy(alpha = 0.30f + 0.25f * pulse), cw * 0.13f, coreC)
    drawCircle(Color(0xFFA5F3FC), cw * 0.065f, coreC)
    drawCircle(Color.White.copy(alpha = 0.8f), cw * 0.025f, coreC)

    // Barrel group, turned around the pivot.
    rotate(aimDeg, pivot) {
        val bw = cw * 0.20f
        val top = pivot.y - barrelLen + kick
        val tubeH = pivot.y - top + 4f * d
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF0F2A44), Color(0xFF4A93C4), Color(0xFF0F2A44)),
                startX = cx - bw / 2f,
                endX = cx + bw / 2f
            ),
            topLeft = Offset(cx - bw / 2f, top),
            size = Size(bw, tubeH),
            cornerRadius = CornerRadius(3f * d)
        )
        drawRoundRect(
            color = WisdomCyan.copy(alpha = 0.6f),
            topLeft = Offset(cx - bw / 2f, top),
            size = Size(bw, tubeH),
            cornerRadius = CornerRadius(3f * d),
            style = Stroke(width = 1.2f * d)
        )
        // Narrow metallic highlight gives the barrel a clearer cylindrical, machined finish.
        drawLine(
            Color.White.copy(alpha = 0.24f),
            Offset(cx - bw * 0.22f, top + 7f * d),
            Offset(cx - bw * 0.22f, pivot.y - 13f * d),
            strokeWidth = 1.5f * d
        )
        // Energy rings along the tube.
        for (k in 0..2) {
            val y = top + 26f * d + k * 15f * d
            val a = if (reduceMotion) 0.7f else 0.35f + 0.55f * (0.5f + 0.5f * sin(clock * 5f - k * 1.2f))
            drawRoundRect(
                color = Color(0xFFA5F3FC).copy(alpha = a),
                topLeft = Offset(cx - bw * 0.62f, y),
                size = Size(bw * 1.24f, 3f * d),
                cornerRadius = CornerRadius(1.5f * d)
            )
        }
        // Muzzle brake with two slots.
        val mw = bw * 1.55f
        drawRoundRect(
            color = Color(0xFF1B3B5C),
            topLeft = Offset(cx - mw / 2f, top - 2f * d),
            size = Size(mw, 14f * d),
            cornerRadius = CornerRadius(3f * d)
        )
        drawRoundRect(
            color = WisdomCyan.copy(alpha = 0.7f),
            topLeft = Offset(cx - mw / 2f, top - 2f * d),
            size = Size(mw, 14f * d),
            cornerRadius = CornerRadius(3f * d),
            style = Stroke(width = 1.2f * d)
        )
        for (s in floatArrayOf(-1f, 1f)) {
            drawLine(
                Color(0xFF06101C),
                Offset(cx + s * mw * 0.26f, top + 1f * d),
                Offset(cx + s * mw * 0.26f, top + 9f * d),
                strokeWidth = 2f * d
            )
        }
        // Mantlet where the barrel enters the housing.
        drawRoundRect(
            color = Color(0xFF1B3B5C),
            topLeft = Offset(cx - cw * 0.27f, pivot.y - 8f * d),
            size = Size(cw * 0.54f, 16f * d),
            cornerRadius = CornerRadius(6f * d)
        )
        drawRoundRect(
            color = WisdomCyan.copy(alpha = 0.6f),
            topLeft = Offset(cx - cw * 0.27f, pivot.y - 8f * d),
            size = Size(cw * 0.54f, 16f * d),
            cornerRadius = CornerRadius(6f * d),
            style = Stroke(width = 1.2f * d)
        )
        // Muzzle flash.
        if (flash > 0f) {
            val tip = Offset(cx, top - 2f * d)
            val fr = (16f + 30f * flash) * d
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f * flash),
                        WisdomCyan.copy(alpha = 0.45f * flash),
                        WisdomCyan.copy(alpha = 0f)
                    ),
                    center = tip,
                    radius = fr
                ),
                radius = fr,
                center = tip
            )
        }
    }
}
