package com.wisdomtower.academy.ui.games.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/**
 * Pooled particles: no allocation in the hot frame path when the pool is warm.
 */
class GameParticleSystem(private val capacity: Int) {

    private val x = FloatArray(capacity)
    private val y = FloatArray(capacity)
    private val vx = FloatArray(capacity)
    private val vy = FloatArray(capacity)
    private val life = FloatArray(capacity)
    private val maxLife = FloatArray(capacity)
    private val size = FloatArray(capacity)
    private val colors = Array(capacity) { Color.White }
    private var count = 0

    fun clear() {
        count = 0
    }

    fun burst(
        originX: Float,
        originY: Float,
        color: Color,
        amount: Int,
        speed: Float = 180f,
        lifeSec: Float = 0.45f,
        random: Random = Random.Default
    ) {
        val n = amount.coerceAtMost(capacity - count)
        for (i in 0 until n) {
            val idx = count
            val angle = random.nextFloat() * (Math.PI * 2).toFloat()
            val spd = speed * (0.4f + random.nextFloat() * 0.6f)
            x[idx] = originX
            y[idx] = originY
            vx[idx] = kotlin.math.cos(angle) * spd
            vy[idx] = kotlin.math.sin(angle) * spd
            maxLife[idx] = lifeSec * (0.6f + random.nextFloat() * 0.4f)
            life[idx] = maxLife[idx]
            size[idx] = 3f + random.nextFloat() * 4f
            colors[idx] = color
            count++
        }
    }

    fun update(dt: Float) {
        var i = 0
        while (i < count) {
            life[i] -= dt
            if (life[i] <= 0f) {
                val last = count - 1
                if (i != last) {
                    x[i] = x[last]; y[i] = y[last]
                    vx[i] = vx[last]; vy[i] = vy[last]
                    life[i] = life[last]; maxLife[i] = maxLife[last]
                    size[i] = size[last]; colors[i] = colors[last]
                }
                count--
            } else {
                x[i] += vx[i] * dt
                y[i] += vy[i] * dt
                vy[i] += 120f * dt
                i++
            }
        }
    }

    fun draw(scope: DrawScope) {
        for (i in 0 until count) {
            val a = (life[i] / maxLife[i]).coerceIn(0f, 1f)
            scope.drawCircle(
                color = colors[i].copy(alpha = a),
                radius = size[i],
                center = Offset(x[i], y[i])
            )
        }
    }
}
