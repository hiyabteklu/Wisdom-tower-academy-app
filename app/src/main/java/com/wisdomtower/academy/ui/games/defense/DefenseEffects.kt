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
import com.wisdomtower.academy.data.games.WisdomDefensePhase
import com.wisdomtower.academy.data.games.WisdomEvent
import com.wisdomtower.academy.ui.games.common.GameParticleSystem
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomCyan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Wisdom Defense effects (docs/GAMES_FEEL_PLAN.md slice D3).
 * Plain Kotlin state (not Compose state): the screen calls [update] once per frame and the
 * draw functions inside its Canvas. Reads engine events with [WisdomDefenseEngine.drainEvents].
 * Respects reduce-motion (no shake, far fewer particles) and low-end (no shake, half particles).
 *
 * Slice W4 (docs/GAMES_COOL_PLAN.md): streak counter and COMBO pill, banners ("Double!", "Triple!", "Unstoppable!",
 * "WAVE n / 3", "BOSS"), gold edge pulse on a streak, floating score numbers, a bigger shockwave ring on a wall hit and
 * a short slow-motion on the last kill ([timeScale], the screen multiplies the engine step with it). Nothing here
 * changes the engine: the streak is counted from the engine events (Killed adds one, WrongHit and WallHit reset it).
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
        const val BANNER_TIME = 0.90f
        const val COMBO_POP_TIME = 0.30f
        const val EDGE_TIME = 0.50f
        /** Real seconds of slow-motion on the last kill, and how fast the game runs during it. */
        const val SLOW_TIME = 0.40f
        const val SLOW_SCALE = 0.40f
        private val GOLD = Color(0xFFFBBF24)
        private val BOSS_RED = Color(0xFFF43F5E)
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

    /** W4: correct kills in a row (a wrong answer or a heart lost sets it back to 0). */
    var streak = 0
        private set
    var bestStreak = 0
        private set
    private var comboPopT = 0f
    private var edgeT = 0f
    private var edgeColor = GOLD
    private var slowT = 0f
    private var bannerAge = BANNER_TIME
    private var bannerText = ""
    private var bannerColor = GOLD
    private var bannerPriority = 0
    private var lastScore = 0
    private var pendingScorePos: Offset? = null
    private var total = 0
    private var lastWave = 0
    private var bossShown = false

    /** W4: 1 normally, [SLOW_SCALE] during the slow-motion of the last kill. Multiply the engine step with it. */
    val timeScale: Float get() = if (slowT > 0f) SLOW_SCALE else 1f

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

    /** Call once per frame after engine.tick(dt). [dt] is the real frame time (not slowed down). */
    fun update(dt: Float, engine: WisdomDefenseEngine) {
        val step = dt.coerceIn(0f, 0.1f)
        if (total == 0) total = engine.questionsLeft
        pendingScorePos = null
        for (ev in engine.drainEvents()) handle(ev, engine)
        trackScore(engine)
        trackWaves(engine)

        particles.update(step)
        shakeT = (shakeT - step).coerceAtLeast(0f)
        vignetteT = (vignetteT - step).coerceAtLeast(0f)
        wallFlashT = (wallFlashT - step).coerceAtLeast(0f)
        heartBreakT = (heartBreakT - step).coerceAtLeast(0f)
        wrongFlashT = (wrongFlashT - step).coerceAtLeast(0f)
        muzzleT = (muzzleT - step).coerceAtLeast(0f)
        comboPopT = (comboPopT - step).coerceAtLeast(0f)
        edgeT = (edgeT - step).coerceAtLeast(0f)
        slowT = (slowT - step).coerceAtLeast(0f)
        if (bannerAge < BANNER_TIME) bannerAge += step

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
                pendingScorePos = Offset(x, cy - u * 0.5f)

                // W4: streak, banner, edge pulse, slow-motion on the very last kill.
                streak++
                if (streak > bestStreak) bestStreak = streak
                comboPopT = COMBO_POP_TIME
                if (streak >= 2) {
                    val word = when (streak) {
                        2 -> "Double!"
                        3 -> "Triple!"
                        else -> "Unstoppable!"
                    }
                    showBanner(word, GOLD, 1)
                }
                if (streak >= 3) pulseEdge(GOLD)
                if (!reduceMotion && !lowEnd && engine.questionsLeft == 0 && engine.enemies.isEmpty()) {
                    slowT = SLOW_TIME
                }
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
                streak = 0
            }
            is WisdomEvent.WallHit -> {
                val d = engine.dying.lastOrNull { it.kind == ev.kind && it.progress >= 1f }
                val id = d?.id ?: 0
                val x = defenseLaneX(w, id, 1f)
                val wallY = h * 0.84f
                burst(x, wallY, GameTheme.heart, 16, 1.1f)
                burst(x, wallY, Color(0xFFFB923C), 8, 0.9f)
                rings.add(Ring(x, wallY, GameTheme.heart, w * 0.12f))
                // W4: a second, bigger white shockwave.
                rings.add(Ring(x, wallY, Color.White, w * 0.30f))
                addText(x, wallY - h * 0.06f, "Heart lost", GameTheme.heart)
                wallFlashT = WALL_FLASH_TIME
                vignetteT = VIGNETTE_TIME
                heartBreakT = HEART_BREAK_TIME
                if (!reduceMotion && !lowEnd) shakeT = SHAKE_TIME
                streak = 0
            }
        }
    }

    /** W4: a floating "+score" for every score gain (kill, armor hit, boss), at the enemy that gave it. */
    private fun trackScore(engine: WisdomDefenseEngine) {
        val s = engine.score
        val gain = s - lastScore
        lastScore = s
        if (gain <= 0 || engine.phase !is WisdomDefensePhase.Playing || w <= 0f || h <= 0f) return
        val pos = pendingScorePos ?: engine.enemies.maxByOrNull { it.progress }?.let { e ->
            val u = defenseUnit(w, h, e.progress)
            Offset(defenseLaneX(w, e.id, e.progress), defenseLaneY(h, e.progress) - u * 1.7f)
        } ?: Offset(w * 0.5f, h * 0.40f)
        addText(pos.x, pos.y, "+$gain", GOLD)
    }

    /** W4: "WAVE n / 3" when the kills pass a third of the questions, and "BOSS" when the boss walks in. */
    private fun trackWaves(engine: WisdomDefenseEngine) {
        if (w <= 0f || h <= 0f) return
        if (total >= 6) {
            val wave = min(engine.killed * 3 / total, 2) + 1
            if (wave > lastWave) {
                lastWave = wave
                showBanner("WAVE $wave / 3", WisdomCyan, 2)
            }
        }
        if (!bossShown && engine.enemies.any { it.boss }) {
            bossShown = true
            showBanner("BOSS", BOSS_RED, 3)
            pulseEdge(BOSS_RED)
        }
    }

    /** A new banner replaces the one on screen unless the one on screen is more important. */
    private fun showBanner(text: String, color: Color, priority: Int) {
        if (bannerAge < BANNER_TIME && priority < bannerPriority) return
        bannerText = text
        bannerColor = color
        bannerPriority = priority
        bannerAge = 0f
    }

    private fun pulseEdge(color: Color) {
        edgeColor = color
        edgeT = EDGE_TIME
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

    /** Effects on top of everything, not shaken: vignette, edge pulse, combo pill, banner and floating text. */
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

            // W4: glow along the screen edge on a streak (and when the boss walks in).
            if (edgeT > 0f) {
                val a = edgeT / EDGE_TIME
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(edgeColor.copy(alpha = 0f), edgeColor.copy(alpha = (if (reduceMotion) 0.18f else 0.32f) * a)),
                        center = Offset(sw * 0.5f, sh * 0.5f),
                        radius = max(sw, sh) * 0.72f
                    ),
                    size = size
                )
            }

            // W4: combo pill, top left of the battlefield, while the streak is 2 or more.
            if (streak >= 2) {
                val pop = if (reduceMotion) 0f else comboPopT / COMBO_POP_TIME
                val cw = 96.dp.toPx()
                val ch = 28.dp.toPx()
                val x = 10.dp.toPx()
                val y = 8.dp.toPx()
                val g = 6.dp.toPx()
                drawRoundRect(
                    GOLD.fade(0.20f + 0.35f * pop),
                    Offset(x - g, y - g),
                    Size(cw + 2f * g, ch + 2f * g),
                    CornerRadius(ch)
                )
                drawRoundRect(Color(0xCC0E1A2B), Offset(x, y), Size(cw, ch), CornerRadius(ch / 2f))
                drawRoundRect(GOLD, Offset(x, y), Size(cw, ch), CornerRadius(ch / 2f), style = Stroke(width = 2.dp.toPx()))
                paint.color = GOLD.toArgb()
                paint.textSize = 13.sp.toPx() * (1f + 0.15f * pop)
                drawIntoCanvas { c -> c.nativeCanvas.drawText("COMBO x$streak", x + cw / 2f, y + ch * 0.68f, paint) }
            }

            // W4: banner in the upper part of the battlefield.
            if (bannerAge < BANNER_TIME) {
                val p = (bannerAge / BANNER_TIME).coerceIn(0f, 1f)
                val pop = if (reduceMotion) 1f else 1f + 0.35f * (1f - (p * 5f).coerceIn(0f, 1f))
                val alpha = if (p > 0.6f) ((1f - p) / 0.4f).coerceIn(0f, 1f) else 1f
                paint.color = bannerColor.copy(alpha = alpha).toArgb()
                paint.textSize = 30.sp.toPx() * pop
                drawIntoCanvas { c -> c.nativeCanvas.drawText(bannerText, sw * 0.5f, sh * 0.24f, paint) }
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
