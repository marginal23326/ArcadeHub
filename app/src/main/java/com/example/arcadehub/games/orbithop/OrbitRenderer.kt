package com.example.arcadehub.games.orbithop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

class OrbitRenderer {

    // --- PAINTS ---
    private val playerPaint = Paint().apply {
        color = OrbitConfig.COLOR_PLAYER
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val pivotCoreInactivePaint = Paint().apply {
        color = OrbitConfig.COLOR_PIVOT_CORE_INACTIVE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val pivotCoreTargetPaint = Paint().apply {
        color = OrbitConfig.COLOR_PIVOT_CORE_TARGET
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val pivotHaloInactivePaint = Paint().apply {
        color = OrbitConfig.COLOR_PIVOT_HALO_INACTIVE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val pivotHaloTargetPaint = Paint().apply {
        color = OrbitConfig.COLOR_PIVOT_HALO_TARGET
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val trailPaint = Paint().apply {
        color = OrbitConfig.COLOR_TRAIL
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val trajectoryPaint = Paint().apply {
        color = 0xFF888888.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 80f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val bestScorePaint = Paint().apply {
        color = OrbitConfig.COLOR_BEST_SCORE
        textSize = 40f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }

    private val overlayTextPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val pauseOverlayPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        alpha = 200
    }

    private val path = Path()

    fun draw(canvas: Canvas, physics: OrbitPhysics, width: Int, height: Int, bestScore: Int, isPaused: Boolean) {
        canvas.drawColor(OrbitConfig.BG_COLOR)

        val camY = physics.cameraY

        canvas.save()
        // Apply camera translation
        canvas.translate(0f, -camY)

        // 1. Draw Pivots
        val pivots = physics.pivots
        val size = pivots.size
        for (i in 0 until size) {
            val p = pivots[i]
            val haloPaint = if (p.isTarget) pivotHaloTargetPaint else pivotHaloInactivePaint
            val corePaint = if (p.isTarget) pivotCoreTargetPaint else pivotCoreInactivePaint
            canvas.drawCircle(p.x, p.y, OrbitConfig.CATCH_RADIUS, haloPaint)
            canvas.drawCircle(p.x, p.y, OrbitConfig.PIVOT_RADIUS, corePaint)
            if (p.isTarget || physics.player.currentPivot == p) {
                // If paused, stop the visual rotation calculation to look frozen
                val time = if(isPaused) 0 else System.currentTimeMillis()
                val angle = (time % 2000) / 2000f * 6.28f * p.dir
                val offset = OrbitConfig.PIVOT_RADIUS - 8f
                val dotX = p.x + offset * cos(angle).toFloat()
                val dotY = p.y + offset * sin(angle).toFloat()
                canvas.drawCircle(dotX, dotY, 4f, playerPaint)
            }
        }

        // 2. Trajectory
        if (physics.player.state == PlayerState.ATTACHED) {
            drawTrajectory(canvas, physics)
        }

        // 3. Trail
        if (physics.trailCount > 1) {
            path.rewind()
            var idx = (physics.trailHead - physics.trailCount + OrbitConfig.TRAIL_LENGTH) % OrbitConfig.TRAIL_LENGTH
            path.moveTo(physics.trailX[idx], physics.trailY[idx])
            for (k in 1 until physics.trailCount) {
                idx = (idx + 1) % OrbitConfig.TRAIL_LENGTH
                path.lineTo(physics.trailX[idx], physics.trailY[idx])
            }
            canvas.drawPath(path, trailPaint)
        }

        // 4. Player
        if (physics.player.state != PlayerState.DEAD) {
            canvas.drawCircle(physics.player.x, physics.player.y, OrbitConfig.PLAYER_RADIUS, playerPaint)
        }

        canvas.restore()

        // 5. HUD
        canvas.drawText(physics.score.toString(), width / 2f, 100f, scorePaint)
        canvas.drawText("BEST: $bestScore", width - 20f, 60f, bestScorePaint)

        // 6. Game Over Overlay
        if (physics.player.state == PlayerState.DEAD) {
            overlayTextPaint.textSize = 100f
            canvas.drawText("GAME OVER", width / 2f, height / 2f - 40f, overlayTextPaint)
            overlayTextPaint.textSize = 50f
            canvas.drawText("Score: ${physics.score}", width / 2f, height / 2f + 50f, overlayTextPaint)
            overlayTextPaint.textSize = 35f
            canvas.drawText("CENTER to Retry | BACK to Menu", width / 2f, height / 2f + 140f, overlayTextPaint)
        }
        else if (isPaused) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), pauseOverlayPaint)
            overlayTextPaint.textSize = 100f
            canvas.drawText("PAUSED", width / 2f, height / 2f - 40f, overlayTextPaint)
            overlayTextPaint.textSize = 40f
            canvas.drawText("CENTER to Resume", width / 2f, height / 2f + 60f, overlayTextPaint)
            canvas.drawText("BACK to Quit", width / 2f, height / 2f + 130f, overlayTextPaint)
        }
    }

    private fun drawTrajectory(canvas: Canvas, physics: OrbitPhysics) {
        val p = physics.player
        val piv = p.currentPivot ?: return

        val vx = -sin(p.angle) * OrbitConfig.FLY_SPEED * piv.dir
        val vy = cos(p.angle) * OrbitConfig.FLY_SPEED * piv.dir
        val previewTime = 0.25f

        canvas.drawLine(p.x, p.y, p.x + vx * previewTime, p.y + vy * previewTime, trajectoryPaint)
    }
}