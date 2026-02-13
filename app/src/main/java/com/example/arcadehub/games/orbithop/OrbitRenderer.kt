package com.example.arcadehub.games.orbithop

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.arcadehub.core.GraphicsUtils
import kotlin.math.cos
import kotlin.math.sin

class OrbitRenderer {

    private val bgTopPaint = GraphicsUtils.createPaint(OrbitConfig.BG_ACCENT_TOP, alpha = 85)
    private val bgBottomPaint = GraphicsUtils.createPaint(OrbitConfig.BG_ACCENT_BOTTOM, alpha = 110)

    private val playerPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PLAYER)
    private val pivotCoreInactivePaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_CORE_INACTIVE)
    private val pivotCoreTargetPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_CORE_TARGET)
    private val pivotHaloInactivePaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_HALO_INACTIVE, Paint.Style.STROKE, strokeWidth = 4f)
    private val pivotHaloTargetPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_HALO_TARGET, Paint.Style.STROKE, strokeWidth = 4f)

    private val trailPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_TRAIL, Paint.Style.STROKE, strokeWidth = 6f).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val trajectoryPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_TRAJECTORY, Paint.Style.STROKE, strokeWidth = 2.5f).apply {
        strokeCap = Paint.Cap.ROUND
    }

    private val scorePaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_TEXT_PRIMARY,
        textSize = 82f,
        align = Paint.Align.CENTER,
        typeface = Typeface.DEFAULT_BOLD
    )
    private val scoreLabelPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_SCORE_LABEL,
        textSize = 28f,
        align = Paint.Align.CENTER,
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    )
    private val bestScorePaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_BEST_SCORE,
        textSize = 34f,
        align = Paint.Align.RIGHT,
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    )
    private val hudPanelPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_HUD_PANEL)
    private val hudPanelStrokePaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_OVERLAY_BORDER,
        Paint.Style.STROKE,
        strokeWidth = 2f
    )
    private val menuTheme = GraphicsUtils.MenuTheme.ORBIT

    private val path = Path()
    private val panelRect = RectF()
    private var pausedAnimTime = 0L

    fun draw(
        canvas: Canvas,
        physics: OrbitPhysics,
        width: Int,
        height: Int,
        bestScore: Int,
        isPaused: Boolean
    ) {
        val animTime = if (isPaused) {
            pausedAnimTime
        } else {
            System.currentTimeMillis().also { pausedAnimTime = it }
        }

        drawBackground(canvas, width, height)

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
                val angle = (animTime % 2200L) / 2200f * 6.283185f * p.dir
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

        drawHud(canvas, physics.score, bestScore, width)

        if (physics.player.state == PlayerState.DEAD) {
            GraphicsUtils.drawGameOverMenu(
                canvas = canvas,
                width = width,
                height = height,
                title = "RUN OVER",
                scoreMsg = "Score ${physics.score}   Best $bestScore",
                footerMsg = "CENTER  Retry | BACK  Return to Hub",
                theme = menuTheme
            )
        } else if (isPaused) {
            GraphicsUtils.drawPauseMenu(
                canvas = canvas,
                width = width,
                height = height,
                primaryAction = "CENTER  Resume",
                secondaryAction = "BACK  Quit to Hub",
                theme = menuTheme
            )
        }
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        val widthF = width.toFloat()
        val heightF = height.toFloat()

        canvas.drawColor(OrbitConfig.BG_COLOR)
        canvas.drawRect(0f, 0f, widthF, heightF * 0.58f, bgTopPaint)
        canvas.drawRect(0f, heightF * 0.42f, widthF, heightF, bgBottomPaint)
    }

    private fun drawHud(canvas: Canvas, score: Int, bestScore: Int, width: Int) {
        val centerX = width / 2f
        canvas.drawText("SCORE", centerX, 42f, scoreLabelPaint)
        canvas.drawText(score.toString(), centerX, 110f, scorePaint)

        panelRect.set(width - 300f, 20f, width - 20f, 94f)
        canvas.drawRoundRect(panelRect, 22f, 22f, hudPanelPaint)
        canvas.drawRoundRect(panelRect, 22f, 22f, hudPanelStrokePaint)
        canvas.drawText("BEST $bestScore", width - 42f, 68f, bestScorePaint)
    }

    private fun drawTrajectory(canvas: Canvas, physics: OrbitPhysics) {
        val p = physics.player
        val piv = p.currentPivot ?: return
        val vx = -sin(p.angle) * OrbitConfig.FLY_SPEED * piv.dir
        val vy = cos(p.angle) * OrbitConfig.FLY_SPEED * piv.dir
        canvas.drawLine(p.x, p.y, p.x + vx * 0.25f, p.y + vy * 0.25f, trajectoryPaint)
    }
}
