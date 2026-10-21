package com.wisdomtower.academy.ui.games.defense

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.WisdomDefenseEngine
import com.wisdomtower.academy.data.games.WisdomEvent
import com.wisdomtower.academy.ui.games.common.GameParticleSystem
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomCyan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Wisdom Defense effects (docs/GAMES_FEEL_PLAN.md slice D3).
 * Plain Kotlin state (not Compose state): the screen calls [update] once per frame and the
 * draw functions inside its Canvas. Reads engine events with [WisdomDefenseEngine.drainEvents].
 * Respects reduce-motion (no shake, far fewer particles) and low-end (no shake, half particles).
 */
internal class DefenseEffects(
    private val particles: GameParticleSystem,
    private val lowEnd: Boolean,
    private val reduceMotion: Boolean
) {
    companion object {
        const val SHAKE_TIME = 0.35f
        const val VIGNETTE_TIME = 0.80f
        const val WALL_FLASH_TIME = 0.45f
        const val HEART_BREAK_TIME = 0.60f
        const val WRONG_FLASH_TIME = 0.30f
        const val MUZZLE_TIME = 0.15f
        const val TEXT_TIME = 0.85f
        const val RING_TIME = 0.40f
        const val SHIELD_TIME = 0.40f
    }

    /** Canvas size in px, set by the screen with onSizeChanged. Effects are skipped while 0. */
    var w = 0f
    var h = 0f
    /** Screen density, set by the screen. */
    var dens = 1f

    var shakeT = 0f
        private set
    var vignetteT = 0f
        private set
    var wallFlashT = 0f
        private set
    var heartBreakT = 0f
        private set
    var wrongFlashT = 0f
        private set
    var muzzleT = 0f
        private set

    private class FloatText(val x: Float, val y: Float, val text: String, val color: Color, var age: Float = 0f)
    private class Ring(val x: Float, val y: Float, val color: Color, val maxR: Float, var age: Float = 0f)
    private class ShieldBreak(val x: Float, val foot: Float, val unit: Float, var age: Float = 0f)

    private val texts = ArrayList<FloatText>()
    private val rings = ArrayList<Ring>()
    private val shields = ArrayList<ShieldBreak>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 0f, 2f, 0xAA000000.toInt())
    }

    fun setSize(width: Float, height: Float) {
        w = width
        h = height
    }

    /** Call when the player taps an answer and the shot leaves the crystal. */
    fun fireMuzzle() {
        muzzleT = MUZZLE_TIME
    }

    /** Call once per frame after engine.tick(dt). */
    fun update(dt: Float, engine: WisdomDefenseEngine) {
        val step = dt.coerceIn(0f, 0.1f)
        for (ev in engine.drainEvents()) handle(ev, engine)

        particles.update(step)
        shakeT = (shakeT - step).coerceAtLeast(0f)
        vignetteT = (vignetteT - step).coerceAtLeast(0f)
        wallFlashT = (wallFlashT - step).coerceAtLeast(0f)
        heartBreakT = (heartBreakT - step).coerceAtLeast(0f)
        wrongFlashT = (wrongFlashT - step).coerceAtLeast(0f)
        muzzleT = (muzzleT - step).coerceAtLeast(0f)

        for (t in texts) t.age += step
        texts.removeAll { it.age >= TEXT_TIME }
        for (r in rings) r.age += step
        rings.removeAll { it.age >= RING_TIME }
        for (s in shields) s.age += step
        shields.removeAll { it.age >= SHIELD_TIME }
    }

    private fun handle(ev: WisdomEvent, engine: WisdomDefenseEngine) {
        if (w <= 0f || h <= 0f) return
        when (ev) {
            is WisdomEvent.Killed -> {
                val d = engine.dying.lastOrNull { it.kind == ev.kind && abs(it.progress - ev.progress) < 0.001f }
                val id = d?.id ?: 0
                val x = defenseLaneX(w, id, ev.progress)
                val foot = defenseLaneY(h, ev.progress)
                val u = defenseUnit(w, h, ev.progress)
                val cy = foot - u * 1.2f
                val col = kindColor(ev.kind)
                burst(x, cy, col, 14, 1f)
                burst(x, cy, Color.White, 6, 0.8f)
                rings.add(Ring(x, cy, col, u * 2.6f))
                addText(x, cy - u * 1.6f, "Correct!", GameTheme.correct)
            }
            is WisdomEvent.WrongHit -> {
                val e = engine.enemies.minByOrNull { abs(it.progress - ev.progress) }
                val id = e?.id ?: 0
                val x = defenseLaneX(w, id, ev.progress)
                val foot = defenseLaneY(h, ev.progress)
                val u = defenseUnit(w, h, ev.progress)
                shields.add(ShieldBreak(x, foot, u))
                burst(x, foot - u * 1.0f, WisdomCyan, 10, 1f)
                wrongFlashT = WRONG_FLASH_TIME
                addText(x, foot - u * 3.0f, "Wrong", GameTheme.wrong)
            }
            is WisdomEvent.WallHit -> {
                val d = engine.dying.lastOrNull { it.kind == ev.kind && it.progress >= 1f }
                val id = d?.id ?: 0
                val x = defenseLaneX(w, id, 1f)
                val wallY = h * 0.84f
                burst(x, wallY, GameTheme.heart, 16, 1.1f)
                burst(x, wallY, Color(0xFFFB923C), 8, 0.9f)
                rings.add(Ring(x, wallY, GameTheme.heart, w * 0.12f))
                addText(x, wallY - h * 0.06f, "Heart lost", GameTheme.heart)
                wallFlashT = WALL_FLASH_TIME
                vignetteT = VIGNETTE_TIME
                heartBreakT = HEART_BREAK_TIME
                if (!reduceMotion && !lowEnd) shakeT = SHAKE_TIME
            }
        }
    }

    private fun burst(x: Float, y: Float, color: Color, amount: Int, speedMul: Float) {
        val n = (if (reduceMotion) amount / 3 else if (lowEnd) amount / 2 else amount).coerceAtLeast(1)
        particles.burst(x, y, color, n, speed = 90f * dens * speedMul, lifeSec = 0.5f)
    }

    private fun addText(x: Float, y: Float, text: String, color: Color) {
        if (texts.size >= 6) texts.removeAt(0)
        texts.add(FloatText(x.coerceIn(w * 0.14f, w * 0.86f), y, text, color))
    }

    private fun kindColor(kind: Int): Color = when (kind) {
        1 -> Color(0xFFA78BFA)
        2 -> Color(0xFF94A3B8)
        else -> Color(0xFF4ADE80)
    }

    /** Wall shake: shift the whole world (not the HUD, not the overlay) by this. */
    fun shakeOffset(clock: Float): Offset {
        if (shakeT <= 0f) return Offset.Zero
        val k = shakeT / SHAKE_TIME
        val amp = 8f * dens * k
        return Offset(sin(clock * 90f) * amp, cos(clock * 73f) * amp * 0.6f)
    }

    /** Effects that live in the world (they shake with it): wall flash, rings, shields, muzzle, particles. */
    fun drawWorld(scope: DrawScope) {
        with(scope) {
            val sw = size.width
            val sh = size.height

            if (wallFlashT > 0f) {
                val a = wallFlashT / WALL_FLASH_TIME
                val wallW = sw * 0.64f
                val left = sw * 0.5f - wallW / 2f
                val wallTop = sh * 0.84f
                val wallH = sh * 0.10f
                drawRoundRect(
                    GameTheme.wrong.fade(0.55f * a),
                    Offset(left, wallTop),
                    Size(wallW, wallH),
                    CornerRadius(6f)
                )
                drawArc(
                    GameTheme.wrong.fade(0.8f * a), 180f, 180f, false,
                    Offset(left - sw * 0.02f, wallTop - wallW * 0.25f),
                    Size(wallW + sw * 0.04f, wallW * 0.5f),
                    style = Stroke(width = 5f)
                )
            }

            for (r in rings) {
                val t = (r.age / RING_TIME).coerceIn(0f, 1f)
                drawCircle(
                    color = r.color.fade(0.7f * (1f - t)),
                    radius = r.maxR * (0.3f + 0.7f * t),
                    center = Offset(r.x, r.y),
                    style = Stroke(width = 1f + 4f * (1f - t))
                )
            }

            for (s in shields) {
                val t = (s.age / SHIELD_TIME).coerceIn(0f, 1f)
                val a = 1f - t
                val cx = s.x
                val cy = s.foot - s.unit * 1.1f
                val rad = s.unit * 1.7f * (1f + 0.35f * t)
                drawArc(
                    WisdomCyan.fade(0.9f * a), 20f, 140f, false,
                    Offset(cx - rad, cy - rad),
                    Size(rad * 2f, rad * 2f),
                    style = Stroke(width = 4.dp.toPx() * (1f - 0.6f * t))
                )
                for (i in 0 until 5) {
                    val ang = (20f + 35f * i) * (PI.toFloat() / 180f)
                    val dx = cos(ang)
                    val dy = sin(ang)
                    val out = rad + s.unit * (0.3f + 0.7f * t)
                    drawLine(
                        Color.White.fade(0.8f * a),
                        Offset(cx + dx * rad, cy + dy * rad),
                        Offset(cx + dx * out, cy + dy * out),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            if (muzzleT > 0f) {
                val a = muzzleT / MUZZLE_TIME
                val c = Offset(sw * 0.5f, defenseMuzzleY(sh))
                drawCircle(WisdomCyan.fade(0.55f * a), 16.dp.toPx() * (1.2f - 0.4f * a), c)
                drawCircle(Color.White.fade(0.85f * a), 6.dp.toPx(), c)
            }

            particles.draw(this)
        }
    }

    /** Effects on top of everything, not shaken: red vignette and floating text. */
    fun drawOverlay(scope: DrawScope) {
        with(scope) {
            val sw = size.width
            val sh = size.height

            if (vignetteT > 0f) {
                val a = vignetteT / VIGNETTE_TIME
                val red = GameTheme.wrong
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(red.copy(alpha = 0f), red.copy(alpha = 0.45f * a)),
                        center = Offset(sw * 0.5f, sh * 0.5f),
                        radius = max(sw, sh) * 0.75f
                    ),
                    size = size
                )
            }

            for (t in texts) {
                val p = (t.age / TEXT_TIME).coerceIn(0f, 1f)
                val y = t.y - 36.dp.toPx() * p
                paint.color = t.color.copy(alpha = (1f - p * p).coerceIn(0f, 1f)).toArgb()
                paint.textSize = 17.sp.toPx() * (1f + 0.3f * (1f - p))
                drawIntoCanvas { c -> c.nativeCanvas.drawText(t.text, t.x, y, paint) }
            }
        }
    }
}

private fun Color.fade(f: Float): Color = copy(alpha = (alpha * f).coerceIn(0f, 1f))
