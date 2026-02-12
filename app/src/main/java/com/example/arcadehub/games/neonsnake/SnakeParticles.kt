package com.example.arcadehub.games.neonsnake

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SnakeParticles {

    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var life: Float, var maxLife: Float,
        var color: Int,
        var size: Float
    )

    private val particles = ArrayList<Particle>()
    private val paint = Paint().apply { style = Paint.Style.FILL }

    fun spawnExplosion(x: Float, y: Float, color: Int, count: Int = 20) {
        repeat(count) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 15f + 5f // Speed variation
            val life = Random.nextFloat() * 0.4f + 0.2f // Life between 0.2s and 0.6s

            particles.add(Particle(
                x, y,
                cos(angle) * speed, sin(angle) * speed,
                life, life,
                color,
                Random.nextFloat() * 6f + 2f // Size variation
            ))
        }
    }

    fun update(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            if (p.life <= 0) {
                iterator.remove()
                continue
            }

            p.x += p.vx
            p.y += p.vy
            // Friction
            p.vx *= 0.95f
            p.vy *= 0.95f
        }
    }

    fun draw(canvas: Canvas) {
        particles.forEach { p ->
            paint.color = p.color
            // Fade out alpha based on life
            paint.alpha = ((p.life / p.maxLife) * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
    }

    fun clear() {
        particles.clear()
    }
}