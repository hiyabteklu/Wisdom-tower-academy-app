package com.wisdomtower.academy.ui.games.defense

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.wisdomtower.academy.data.games.GameLevel
import com.wisdomtower.academy.ui.theme.WisdomCyan
import kotlin.math.abs
import kotlin.math.sin

/**
 * Wisdom Defense art (docs/GAMES_FEEL_PLAN.md slice D2): Canvas monsters, perspective, lane, empire wall.
 * Pure drawing helpers, no state. [clock] is seconds since the game started (animation only).
 * Effects (particles, trail, shake, vignette) come in D3.
 *
 * Slice W2 (docs/GAMES_COOL_PLAN.md): level skins ([defenseSkin]: Easy friendly green, Medium purple, Hard ember),
 * a portal flash when an enemy enters at the far edge, a blurred second pair of bat wings, stomp dust under the golem,
 * armor plates on an armored golem (they come off as the armor is cracked), and the Hard boss: a bigger golem with a
 * health bar. All of it is drawn from data the engine already has (armor, boss); no engine change.
 */

private fun Color.a(f: Float): Color = copy(alpha = (alpha * f).coerceIn(0f, 1f))

/** Colours of the three enemy kinds for one level. */
internal class DefenseSkin(
    val slimeTop: Color,
    val slimeBottom: Color,
    val batWing: Color,
    val batTop: Color,
    val batBottom: Color,
    val golemStone: Color,
    val golemLight: Color,
    val golemDark: Color
)

private val SKIN_EASY = DefenseSkin(
    slimeTop = Color(0xFF4ADE80),
    slimeBottom = Color(0xFF15803D),
    batWing = Color(0xFF0D9488),
    batTop = Color(0xFF5EEAD4),
    batBottom = Color(0xFF0F766E),
    golemStone = Color(0xFF64748B),
    golemLight = Color(0xFF94A3B8),
    golemDark = Color(0xFF334155)
)

private val SKIN_MEDIUM = DefenseSkin(
    slimeTop = Color(0xFFC084FC),
    slimeBottom = Color(0xFF7E22CE),
    batWing = Color(0xFF6D28D9),
    batTop = Color(0xFFA78BFA),
    batBottom = Color(0xFF5B21B6),
    golemStone = Color(0xFF6D6A8A),
    golemLight = Color(0xFF9C99B8),
    golemDark = Color(0xFF37364F)
)

private val SKIN_HARD = DefenseSkin(
    slimeTop = Color(0xFFFB923C),
    slimeBottom = Color(0xFFB91C1C),
    batWing = Color(0xFF991B1B),
    batTop = Color(0xFFFB7185),
    batBottom = Color(0xFF9F1239),
    golemStone = Color(0xFF78716C),
    golemLight = Color(0xFFA8A29E),
    golemDark = Color(0xFF44403C)
)

internal fun defenseSkin(level: GameLevel): DefenseSkin = when (level) {
    GameLevel.EASY -> SKIN_EASY
    GameLevel.MEDIUM -> SKIN_MEDIUM
    GameLevel.HARD -> SKIN_HARD
}

/** Y of an enemy's feet. progress 0 = far edge, 1 = empire wall. */
internal fun defenseLaneY(h: Float, progress: Float): Float = h * (0.12f + progress * 0.7f)

/** X of an enemy: stays near the lane centre, spreads a little as it comes closer. */
internal fun defenseLaneX(w: Float, id: Int, progress: Float): Float =
    w * 0.5f + sin(id * 1.7f) * w * (0.03f + 0.07f * progress.coerceIn(0f, 1f))

/** Perspective: small far away (0.5), big at the wall (1.3). */
internal fun defenseScale(progress: Float): Float = 0.5f + 0.8f * progress.coerceIn(0f, 1f)

/** Base size unit of an enemy at this distance. */
internal fun defenseUnit(w: Float, h: Float, progress: Float): Float =
    minOf(w, h) * 0.065f * defenseScale(progress)

/** Where shots start (the empire crystal). */
internal fun defenseMuzzleY(h: Float): Float = h * 0.70f

/** Horizon glow, lane in perspective with moving centre dashes. */
internal fun DrawScope.drawDefenseField(w: Float, h: Float, clock: Float) {
    val cx = w * 0.5f
    val topHalf = w * 0.11f
    val botHalf = w * 0.40f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x3322D3EE), Color.Transparent),
            startY = 0f,
            endY = h * 0.45f
        ),
        size = Size(w, h * 0.45f)
    )
    val lane = Path().apply {
        moveTo(cx - topHalf, 0f)
        lineTo(cx + topHalf, 0f)
        lineTo(cx + botHalf, h)
        lineTo(cx - botHalf, h)
        close()
    }
    drawPath(
        lane,
        Brush.verticalGradient(
            colors = listOf(Color(0x1A22D3EE), Color(0x550E7490)),
            startY = 0f,
            endY = h
        )
    )
    drawLine(WisdomCyan.a(0.45f), Offset(cx - topHalf, 0f), Offset(cx - botHalf, h), strokeWidth = 3f)
    drawLine(WisdomCyan.a(0.45f), Offset(cx + topHalf, 0f), Offset(cx + botHalf, h), strokeWidth = 3f)
    val n = 8
    val phase = (clock * 0.35f) % 1f
    for (i in 0 until n) {
        val t0 = (i + phase) / n
        val t1 = t0 + (0.30f / n) * (0.5f + t0)
        drawLine(
            Color.White.a(0.10f + 0.18f * t0),
            Offset(cx, h * t0),
            Offset(cx, h * t1),
            strokeWidth = 2f + 4f * t0
        )
    }

    // Short inset lane markers add a sense of road scale and forward motion without
    // turning the battlefield into a busy grid. Their spacing expands with perspective.
    val roadPhase = (clock * 0.48f) % 1f
    for (i in 0 until 7) {
        val t0 = (i + roadPhase) / 7f
        val t1 = (t0 + (0.13f / 7f) * (0.45f + t0)).coerceAtMost(1f)
        val half0 = topHalf + (botHalf - topHalf) * t0
        val half1 = topHalf + (botHalf - topHalf) * t1
        val inset0 = half0 * 0.78f
        val inset1 = half1 * 0.78f
        val markerAlpha = 0.035f + 0.075f * t0
        for (side in floatArrayOf(-1f, 1f)) {
            drawLine(
                Color(0xFF67E8F9).a(markerAlpha),
                Offset(cx + side * inset0, h * t0),
                Offset(cx + side * inset1, h * t1),
                strokeWidth = 0.8f + 1.4f * t0
            )
        }
    }
}

/** Empire: battlement wall, tower with a glowing crystal, pulsing shield arch. */
internal fun DrawScope.drawDefenseEmpire(w: Float, h: Float, clock: Float) {
    val cx = w * 0.5f
    val wallTop = h * 0.84f
    val wallH = h * 0.10f
    val wallW = w * 0.64f
    val left = cx - wallW / 2f
    val pulse = 0.5f + 0.5f * sin(clock * 2.2f)

    // shield arch
    val archH = wallW * 0.5f
    val archTop = Offset(left - w * 0.02f, wallTop - archH / 2f)
    val archSize = Size(wallW + w * 0.04f, archH)
    drawArc(WisdomCyan.a(0.05f + 0.05f * pulse), 180f, 180f, true, archTop, archSize)
    drawArc(WisdomCyan.a(0.30f + 0.25f * pulse), 180f, 180f, false, archTop, archSize, style = Stroke(width = 3f))

    // wall body and top edge
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E3A5F), Color(0xFF0F1F36)),
            startY = wallTop,
            endY = wallTop + wallH
        ),
        topLeft = Offset(left, wallTop),
        size = Size(wallW, wallH),
        cornerRadius = CornerRadius(6f)
    )
    drawLine(WisdomCyan.a(0.7f), Offset(left, wallTop), Offset(left + wallW, wallTop), strokeWidth = 3f)
    drawLine(
        Color.White.a(0.08f),
        Offset(left, wallTop + wallH * 0.5f),
        Offset(left + wallW, wallTop + wallH * 0.5f),
        strokeWidth = 2f
    )

    // battlements
    val mw = wallW / 17f
    for (i in 0 until 9) {
        drawRoundRect(
            Color(0xFF1E3A5F),
            Offset(left + i * 2f * mw, wallTop - mw * 0.9f),
            Size(mw, mw * 0.9f),
            CornerRadius(3f)
        )
    }

    // tower
    val tw = wallW * 0.12f
    val th = wallH * 1.3f
    drawRoundRect(
        Color(0xFF16304F),
        Offset(cx - tw / 2f, wallTop - th),
        Size(tw, th),
        CornerRadius(4f)
    )
    drawRoundRect(
        Color(0xFF22507A),
        Offset(cx - tw * 0.65f, wallTop - th - tw * 0.15f),
        Size(tw * 1.3f, tw * 0.3f),
        CornerRadius(3f)
    )

    // crystal
    val cr = wallW * 0.035f
    val cy = wallTop - th - cr * 1.4f - 3f * sin(clock * 2.5f)
    val glowR = cr * 3.2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WisdomCyan.a(0.45f + 0.2f * pulse), Color.Transparent),
            center = Offset(cx, cy),
            radius = glowR
        ),
        radius = glowR,
        center = Offset(cx, cy)
    )
    val crystal = Path().apply {
        moveTo(cx, cy - cr * 1.3f)
        lineTo(cx + cr * 0.8f, cy)
        lineTo(cx, cy + cr * 1.3f)
        lineTo(cx - cr * 0.8f, cy)
        close()
    }
    drawPath(
        crystal,
        Brush.verticalGradient(
            colors = listOf(Color(0xFFA5F3FC), Color(0xFF0891B2)),
            startY = cy - cr * 1.3f,
            endY = cy + cr * 1.3f
        )
    )
}

/** Ring on the ground under the enemy the next shot will hit. */
internal fun DrawScope.drawDefenseTargetRing(x: Float, footY: Float, unit: Float, clock: Float) {
    val pulse = 0.5f + 0.5f * sin(clock * 6f)
    drawOval(
        WisdomCyan.a(0.5f + 0.35f * pulse),
        Offset(x - unit * 1.5f, footY - unit * 0.3f),
        Size(unit * 3f, unit * 0.6f),
        style = Stroke(width = 3f)
    )
}

/**
 * One enemy. [kind] 0 slime, 1 bat, 2 golem. [footY] is where it touches the ground.
 * [alpha] fades it (death), [flash] 0..1 paints a white hit flash.
 * W2: [skin] colours (see [defenseSkin]); [armor] > 0 draws armor plates on a golem (2 = chest and shoulders, 1 = chest
 * only); [boss] draws the golem 1.6 times bigger with a health bar filled to [bossFrac] (0..1); [spawnT] 0..1 is the
 * strength of the portal flash while the enemy enters at the far edge (0 = none).
 */
internal fun DrawScope.drawDefenseEnemy(
    kind: Int,
    x: Float,
    footY: Float,
    unit: Float,
    clock: Float,
    seed: Int,
    alpha: Float = 1f,
    flash: Float = 0f,
    skin: DefenseSkin = SKIN_MEDIUM,
    armor: Int = 0,
    boss: Boolean = false,
    bossFrac: Float = 1f,
    spawnT: Float = 0f
) {
    val u = if (boss) unit * 1.6f else unit
    val shadowHalf = if (kind == 1) 1.0f else 1.6f
    drawOval(
        Color.Black.a(0.35f * alpha),
        Offset(x - u * shadowHalf, footY - u * 0.12f),
        Size(u * shadowHalf * 2f, u * 0.34f)
    )
    // Portal glow behind the enemy while it enters (the boss gets a bigger red one).
    val portalColor = if (boss) Color(0xFFF43F5E) else skin.slimeTop
    if (spawnT > 0f) {
        val c = Offset(x, footY - u * 1.0f)
        val pr = u * (if (boss) 3.2f else 1.2f + 1.6f * spawnT)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(portalColor.a(0.6f * spawnT * alpha), Color.Transparent),
                center = c,
                radius = pr
            ),
            radius = pr,
            center = c
        )
    }
    when (kind) {
        1 -> drawBat(x, footY, u, clock, seed, alpha, skin)
        2 -> drawGolem(x, footY, u, clock, seed, alpha, skin, armor)
        else -> drawSlime(x, footY, u, clock, seed, alpha, skin)
    }
    if (spawnT > 0f) {
        val c = Offset(x, footY - u * 1.0f)
        drawCircle(
            Color.White.a(0.7f * spawnT * alpha),
            u * (0.9f + 1.4f * (1f - spawnT)),
            c,
            style = Stroke(width = 3f)
        )
    }
    if (boss && alpha > 0.5f) {
        val bw = u * 3.0f
        val bh = u * 0.22f
        val left = x - bw / 2f
        val top = footY - u * 2.55f - u * 0.55f
        drawRoundRect(Color.Black.a(0.55f * alpha), Offset(left - 2f, top - 2f), Size(bw + 4f, bh + 4f), CornerRadius(bh))
        drawRoundRect(
            Color(0xFFF43F5E).a(alpha),
            Offset(left, top),
            Size(bw * bossFrac.coerceIn(0f, 1f), bh),
            CornerRadius(bh)
        )
    }
    if (flash > 0f) {
        drawCircle(Color.White.a(0.55f * flash), u * 1.6f, Offset(x, footY - u * 1.2f))
    }
}

private fun DrawScope.drawSlime(x: Float, foot: Float, u: Float, clk: Float, seed: Int, al: Float, skin: DefenseSkin) {
    val bob = sin(clk * 5f + seed)
    val bw = u * 1.15f
    val bh = u * 1.5f * (1f + 0.07f * bob)
    val top = foot - bh
    val body = Path().apply {
        moveTo(x - bw, foot)
        cubicTo(x - bw * 1.05f, top + bh * 0.15f, x - bw * 0.5f, top, x, top)
        cubicTo(x + bw * 0.5f, top, x + bw * 1.05f, top + bh * 0.15f, x + bw, foot)
        close()
    }
    drawPath(
        body,
        Brush.verticalGradient(
            colors = listOf(skin.slimeTop.a(al), skin.slimeBottom.a(al)),
            startY = top,
            endY = foot
        )
    )
    // A restrained rim catches the light and separates the slime from the road at small sizes.
    drawPath(
        body,
        Color.White.a(0.28f * al),
        style = Stroke(width = (u * 0.045f).coerceAtLeast(1f))
    )
    drawOval(
        Color.White.a(0.35f * al),
        Offset(x - bw * 0.6f, top + bh * 0.12f),
        Size(bw * 0.55f, bh * 0.2f)
    )
    val ink = Color(0xFF111827).a(al)
    for (s in floatArrayOf(-1f, 1f)) {
        val ex = x + s * bw * 0.38f
        val ey = top + bh * 0.48f
        drawCircle(Color.White.a(al), u * 0.24f, Offset(ex, ey))
        drawCircle(ink, u * 0.11f, Offset(ex, ey + u * 0.05f))
        drawLine(
            ink,
            Offset(ex + s * u * 0.32f, ey - u * 0.34f),
            Offset(ex - s * u * 0.2f, ey - u * 0.18f),
            strokeWidth = u * 0.09f
        )
    }
}

private fun DrawScope.drawBat(x: Float, foot: Float, u: Float, clk: Float, seed: Int, al: Float, skin: DefenseSkin) {
    val cy = foot - u * 1.7f + u * 0.3f * sin(clk * 7f + seed)
    val flap = sin(clk * 14f + seed)
    val wingCol = skin.batWing.a(al)

    fun wingPath(s: Float, fl: Float): Path = Path().apply {
        moveTo(x + s * u * 0.4f, cy - u * 0.1f)
        quadraticBezierTo(
            x + s * u * 1.4f, cy - u * 0.9f + fl * u * 0.5f,
            x + s * u * 2.1f, cy - u * 0.2f + fl * u * 0.8f
        )
        quadraticBezierTo(x + s * u * 1.5f, cy + u * 0.1f, x + s * u * 1.2f, cy + u * 0.6f)
        quadraticBezierTo(x + s * u * 0.85f, cy + u * 0.2f, x + s * u * 0.4f, cy + u * 0.45f)
        close()
    }

    for (s in floatArrayOf(-1f, 1f)) {
        // Faint second wing at another flap phase: reads as wing blur.
        drawPath(wingPath(s, sin(clk * 14f + seed + 1.3f)), wingCol.a(0.30f))
        val wing = wingPath(s, flap)
        drawPath(wing, wingCol)
        // Fine edge light keeps the thin wings readable against the dark battlefield.
        drawPath(wing, Color(0xFFE9D5FF).a(0.48f * al), style = Stroke(width = (u * 0.035f).coerceAtLeast(0.8f)))
    }
    val bodyTop = skin.batTop.a(al)
    val bodyBottom = skin.batBottom.a(al)
    for (s in floatArrayOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(x + s * u * 0.15f, cy - u * 0.5f)
            lineTo(x + s * u * 0.55f, cy - u * 0.5f)
            lineTo(x + s * u * 0.45f, cy - u * 0.95f)
            close()
        }
        drawPath(ear, bodyBottom)
    }
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(bodyTop, bodyBottom),
            startY = cy - u * 0.6f,
            endY = cy + u * 0.6f
        ),
        radius = u * 0.6f,
        center = Offset(x, cy)
    )
    for (s in floatArrayOf(-1f, 1f)) {
        drawCircle(Color(0xFFF43F5E).a(0.35f * al), u * 0.24f, Offset(x + s * u * 0.24f, cy - u * 0.08f))
        drawCircle(Color(0xFFF43F5E).a(al), u * 0.13f, Offset(x + s * u * 0.24f, cy - u * 0.08f))
        val fang = Path().apply {
            moveTo(x + s * u * 0.2f, cy + u * 0.2f)
            lineTo(x + s * u * 0.08f, cy + u * 0.2f)
            lineTo(x + s * u * 0.14f, cy + u * 0.42f)
            close()
        }
        drawPath(fang, Color.White.a(al))
    }
}

private fun DrawScope.drawGolem(
    x0: Float,
    foot: Float,
    u: Float,
    clk: Float,
    seed: Int,
    al: Float,
    skin: DefenseSkin,
    armor: Int
) {
    val step = sin(clk * 3f + seed)
    val x = x0 + step * u * 0.05f
    val f = foot - abs(step) * u * 0.06f
    val stone = skin.golemStone.a(al)
    val dark = skin.golemDark.a(al)
    val light = skin.golemLight.a(al)
    val crack = Color(0xFF1E293B).a(0.8f * al)
    val bodyTop = f - u * 1.9f

    // Stomp dust when a foot hits the ground.
    val contact = abs(step)
    if (contact < 0.15f) {
        val k = 1f - contact / 0.15f
        val dust = Color(0xFFCBD5E1).a(0.28f * k * al)
        drawCircle(dust, u * (0.12f + 0.22f * (1f - k)), Offset(x - u * 1.05f, foot - u * 0.05f))
        drawCircle(dust, u * (0.12f + 0.22f * (1f - k)), Offset(x + u * 1.05f, foot - u * 0.05f))
    }

    // legs
    drawRoundRect(dark, Offset(x - u * 0.75f, f - u * 0.5f), Size(u * 0.6f, u * 0.5f), CornerRadius(u * 0.1f))
    drawRoundRect(dark, Offset(x + u * 0.15f, f - u * 0.5f), Size(u * 0.6f, u * 0.5f), CornerRadius(u * 0.1f))
    // arms
    drawRoundRect(dark, Offset(x - u * 1.3f, bodyTop + u * 0.55f), Size(u * 0.4f, u * 0.85f), CornerRadius(u * 0.1f))
    drawRoundRect(dark, Offset(x + u * 0.9f, bodyTop + u * 0.55f), Size(u * 0.4f, u * 0.85f), CornerRadius(u * 0.1f))
    // body
    drawRoundRect(
        brush = Brush.verticalGradient(colors = listOf(light, stone), startY = bodyTop, endY = bodyTop + u * 1.45f),
        topLeft = Offset(x - u * 0.95f, bodyTop),
        size = Size(u * 1.9f, u * 1.45f),
        cornerRadius = CornerRadius(u * 0.2f)
    )
    // A narrow cool highlight gives the stone torso a bevel without making it look metallic.
    drawLine(
        Color.White.a(0.22f * al),
        Offset(x - u * 0.76f, bodyTop + u * 0.18f),
        Offset(x - u * 0.76f, bodyTop + u * 1.12f),
        strokeWidth = u * 0.045f
    )
    drawLine(
        Color(0xFF0F172A).a(0.22f * al),
        Offset(x + u * 0.78f, bodyTop + u * 0.22f),
        Offset(x + u * 0.78f, bodyTop + u * 1.20f),
        strokeWidth = u * 0.05f
    )
    // shoulders
    drawRoundRect(light, Offset(x - u * 1.35f, bodyTop - u * 0.05f), Size(u * 0.6f, u * 0.6f), CornerRadius(u * 0.15f))
    drawRoundRect(light, Offset(x + u * 0.75f, bodyTop - u * 0.05f), Size(u * 0.6f, u * 0.6f), CornerRadius(u * 0.15f))
    // head
    drawRoundRect(light, Offset(x - u * 0.5f, bodyTop - u * 0.65f), Size(u * 1.0f, u * 0.75f), CornerRadius(u * 0.15f))
    for (s in floatArrayOf(-1f, 1f)) {
        val c = Offset(x + s * u * 0.2f, bodyTop - u * 0.28f)
        drawCircle(Color(0xFFFB923C).a(0.35f * al), u * 0.2f, c)
        drawCircle(Color(0xFFFDBA74).a(al), u * 0.09f, c)
    }
    // cracks and core
    drawLine(crack, Offset(x - u * 0.2f, bodyTop + u * 0.2f), Offset(x + u * 0.05f, bodyTop + u * 0.7f), strokeWidth = u * 0.05f)
    drawLine(crack, Offset(x + u * 0.5f, bodyTop + u * 0.3f), Offset(x + u * 0.35f, bodyTop + u * 0.8f), strokeWidth = u * 0.05f)
    drawCircle(Color(0xFFFB923C).a(0.45f * al), u * 0.28f, Offset(x, bodyTop + u * 0.95f))
    drawCircle(Color(0xFFFDBA74).a(al), u * 0.12f, Offset(x, bodyTop + u * 0.95f))

    // Armor plates (only while armor is left): shoulders go first, the chest plate on the last armor point.
    if (armor >= 2) {
        val gold = Color(0xFFFBBF24).a(al)
        val plate = Color(0xFFE2E8F0).a(0.75f * al)
        drawRoundRect(plate, Offset(x - u * 1.42f, bodyTop - u * 0.12f), Size(u * 0.74f, u * 0.5f), CornerRadius(u * 0.12f))
        drawRoundRect(plate, Offset(x + u * 0.68f, bodyTop - u * 0.12f), Size(u * 0.74f, u * 0.5f), CornerRadius(u * 0.12f))
        drawRoundRect(gold, Offset(x - u * 1.42f, bodyTop - u * 0.12f), Size(u * 0.74f, u * 0.5f), CornerRadius(u * 0.12f), style = Stroke(width = u * 0.07f))
        drawRoundRect(gold, Offset(x + u * 0.68f, bodyTop - u * 0.12f), Size(u * 0.74f, u * 0.5f), CornerRadius(u * 0.12f), style = Stroke(width = u * 0.07f))
    }
    if (armor >= 1) {
        val gold = Color(0xFFFBBF24).a(al)
        val plate = Color(0xFFE2E8F0).a(0.75f * al)
        drawRoundRect(plate, Offset(x - u * 0.8f, bodyTop + u * 0.2f), Size(u * 1.6f, u * 0.85f), CornerRadius(u * 0.14f))
        drawRoundRect(gold, Offset(x - u * 0.8f, bodyTop + u * 0.2f), Size(u * 1.6f, u * 0.85f), CornerRadius(u * 0.14f), style = Stroke(width = u * 0.07f))
        drawCircle(Color(0xFFFB923C).a(0.45f * al), u * 0.28f, Offset(x, bodyTop + u * 0.95f))
    }
}
