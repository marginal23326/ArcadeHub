package com.example.arcadehub.games.orbithop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.example.arcadehub.core.GraphicsUtils
import kotlin.math.cos
import kotlin.math.sin

class OrbitRenderer {

    private val playerPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PLAYER)
    private val pivotCoreInactivePaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_CORE_INACTIVE)
    private val pivotCoreTargetPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_CORE_TARGET)

    private val pivotHaloInactivePaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_HALO_INACTIVE, Paint.Style.STROKE, strokeWidth = 4f)
    private val pivotHaloTargetPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_HALO_TARGET, Paint.Style.STROKE, strokeWidth = 6f)

    private val trailPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_TRAIL, Paint.Style.STROKE, strokeWidth = 6f).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val trajectoryPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_TRAJECTORY, Paint.Style.STROKE, strokeWidth = 3f)

    private val scorePaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 80f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val bestScorePaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_BEST_SCORE, textSize = 40f, align = Paint.Align.RIGHT, typeface = Typeface.MONOSPACE)

    private val path = Path()

    fun draw(canvas: Canvas, physics: OrbitPhysics, width: Int, height: Int, bestScore: Int, isPaused: Boolean) {
        canvas.drawColor(OrbitConfig.BG_COLOR)

        val camY = physics.cameraY
        canvas.save()
        canvas.translate(0f, -camY)

        val pivots = physics.pivots
        for (i in 0 until pivots.size) {
            val p = pivots[i]
            val haloPaint = if (p.isTarget) pivotHaloTargetPaint else pivotHaloInactivePaint
            val corePaint = if (p.isTarget) pivotCoreTargetPaint else pivotCoreInactivePaint
            canvas.drawCircle(p.x, p.y, OrbitConfig.CATCH_RADIUS, haloPaint)
            canvas.drawCircle(p.x, p.y, OrbitConfig.PIVOT_RADIUS, corePaint)
            if (p.isTarget || physics.player.currentPivot == p) {
                val time = if(isPaused) 0 else System.currentTimeMillis()
                val angle = (time % 2000) / 2000f * 6.28f * p.dir
                val offset = OrbitConfig.PIVOT_RADIUS - 8f
                val dotX = p.x + offset * cos(angle).toFloat()
                val dotY = p.y + offset * sin(angle).toFloat()
                canvas.drawCircle(dotX, dotY, 4f, playerPaint)
            }
        }

        if (physics.player.state == PlayerState.ATTACHED) {
            drawTrajectory(canvas, physics)
        }

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

        if (physics.player.state != PlayerState.DEAD) {
            canvas.drawCircle(physics.player.x, physics.player.y, OrbitConfig.PLAYER_RADIUS, playerPaint)
        }

        canvas.restore()

        canvas.drawText(physics.score.toString(), width / 2f, 100f, scorePaint)
        canvas.drawText("BEST: $bestScore", width - 20f, 60f, bestScorePaint)

        if (physics.player.state == PlayerState.DEAD) {
            GraphicsUtils.drawGameOverMenu(
                canvas, width, height,
                "GAME OVER",
                "Score: ${physics.score}"
            )
        }
        else if (isPaused) {
            GraphicsUtils.drawPauseMenu(canvas, width, height)
        }
    }

    private fun drawTrajectory(canvas: Canvas, physics: OrbitPhysics) {
        val p = physics.player
        val piv = p.currentPivot ?: return
        val vx = -sin(p.angle) * OrbitConfig.FLY_SPEED * piv.dir
        val vy = cos(p.angle) * OrbitConfig.FLY_SPEED * piv.dir
        canvas.drawLine(p.x, p.y, p.x + vx * 0.25f, p.y + vy * 0.25f, trajectoryPaint)
    }
}