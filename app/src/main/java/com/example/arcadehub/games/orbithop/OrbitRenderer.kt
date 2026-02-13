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

    private val bgTopPaint = GraphicsUtils.createPaint(OrbitConfig.BG_ACCENT_TOP, alpha = 88)
    private val bgBottomPaint = GraphicsUtils.createPaint(OrbitConfig.BG_ACCENT_BOTTOM, alpha = 120)

    private val pivotRingPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_PIVOT_RING,
        Paint.Style.STROKE,
        strokeWidth = 4f
    )
    private val targetRingPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_TARGET_RING,
        Paint.Style.STROKE,
        strokeWidth = 4f
    )
    private val targetPulsePaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_TARGET_RING,
        Paint.Style.STROKE,
        strokeWidth = 2.5f,
        alpha = 110
    )

    private val pivotCorePaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_CORE)
    private val currentPivotCorePaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_CURRENT)
    private val targetPivotCorePaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PIVOT_TARGET)

    private val playerPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PLAYER)
    private val directionDotPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_PLAYER, alpha = 185)

    private val trailPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_TRAIL,
        Paint.Style.STROKE,
        strokeWidth = 5.5f
    ).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val launchIndicatorGoodPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_LAUNCH_INDICATOR_GOOD,
        Paint.Style.STROKE,
        strokeWidth = 4f
    ).apply {
        strokeCap = Paint.Cap.ROUND
    }
    private val launchIndicatorBadPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_LAUNCH_INDICATOR_BAD,
        Paint.Style.STROKE,
        strokeWidth = 4f
    ).apply {
        strokeCap = Paint.Cap.ROUND
    }

    private val scorePaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_SCORE,
        textSize = 80f,
        align = Paint.Align.CENTER,
        typeface = Typeface.DEFAULT_BOLD
    )
    private val labelPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_TEXT_SECONDARY,
        textSize = 28f,
        align = Paint.Align.CENTER,
        typeface = Typeface.DEFAULT_BOLD
    )
    private val bestPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_BEST_SCORE,
        textSize = 30f,
        align = Paint.Align.RIGHT,
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    )
    private val comboPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_TEXT_PRIMARY,
        textSize = 30f,
        align = Paint.Align.LEFT,
        typeface = Typeface.DEFAULT_BOLD
    )

    private val beatTrackPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_BEAT_TRACK)
    private val beatFillPaint = GraphicsUtils.createPaint(OrbitConfig.COLOR_BEAT_FILL)

    private val timingFlashPaint = GraphicsUtils.createPaint(
        OrbitConfig.COLOR_TEXT_PRIMARY,
        textSize = 34f,
        align = Paint.Align.CENTER,
        typeface = Typeface.DEFAULT_BOLD
    )

    private val menuTheme = GraphicsUtils.MenuTheme.ORBIT

    private val trailPath = Path()
    private val beatRect = RectF()
    private val beatFillRect = RectF()

    fun draw(
        canvas: Canvas,
        physics: OrbitPhysics,
        width: Int,
        height: Int,
        bestScore: Int,
        isPaused: Boolean
    ) {
        val animTime = System.currentTimeMillis()

        drawBackground(canvas, width, height)

        canvas.save()
        canvas.translate(0f, -physics.cameraY)

        drawPivots(canvas, physics, animTime)
        drawLaunchIndicator(canvas, physics)
        drawTrail(canvas, physics)
        drawPlayer(canvas, physics)

        canvas.restore()

        drawHud(canvas, physics, width, bestScore)

        if (physics.player.state == PlayerState.DEAD) {
            GraphicsUtils.drawGameOverMenu(
                canvas = canvas,
                width = width,
                height = height,
                title = "FLOW BROKEN",
                scoreMsg = "Score ${physics.score}   Best $bestScore",
                subMsg = "Launch with arrow direction and ride gravity pull.",
                footerMsg = "CENTER  Retry | BACK  Return to Hub",
                theme = menuTheme
            )
        } else if (isPaused) {
            GraphicsUtils.drawPauseMenu(
                canvas = canvas,
                width = width,
                height = height,
                title = "PAUSED",
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
        canvas.drawRect(0f, 0f, widthF, heightF * 0.56f, bgTopPaint)
        canvas.drawRect(0f, heightF * 0.42f, widthF, heightF, bgBottomPaint)
    }

    private fun drawPivots(canvas: Canvas, physics: OrbitPhysics, animTime: Long) {
        val pivots = physics.pivots

        for (i in pivots.indices) {
            val pivot = pivots[i]
            val isCurrent = physics.player.currentPivot === pivot
            val isTarget = pivot.isTarget

            val ringPaint = if (isTarget) targetRingPaint else pivotRingPaint
            val corePaint = when {
                isTarget -> targetPivotCorePaint
                isCurrent -> currentPivotCorePaint
                else -> pivotCorePaint
            }

            canvas.drawCircle(pivot.x, pivot.y, OrbitConfig.CATCH_RADIUS, ringPaint)
            canvas.drawCircle(pivot.x, pivot.y, OrbitConfig.PIVOT_RADIUS, corePaint)

            if (isTarget) {
                targetPulsePaint.alpha = (100 + physics.beatPulse * 120f).toInt().coerceIn(60, 220)
                val pulseRadius = OrbitConfig.CATCH_RADIUS + 8f + physics.beatPulse * 10f
                canvas.drawCircle(pivot.x, pivot.y, pulseRadius, targetPulsePaint)
            }

            val angle = (animTime % 1800L) / 1800f * 6.283185f * pivot.dir
            val dotRadius = OrbitConfig.PIVOT_RADIUS - 8f
            val dotX = pivot.x + dotRadius * cos(angle)
            val dotY = pivot.y + dotRadius * sin(angle)
            canvas.drawCircle(dotX, dotY, 3.6f, directionDotPaint)
        }
    }

    private fun drawLaunchIndicator(canvas: Canvas, physics: OrbitPhysics) {
        if (physics.player.state != PlayerState.ATTACHED) return

        val pivot = physics.player.currentPivot ?: return
        val dirX = -sin(physics.player.angle) * pivot.dir
        val dirY = cos(physics.player.angle) * pivot.dir

        val startX = physics.player.x
        val startY = physics.player.y
        val endX = startX + dirX * 84f
        val endY = startY + dirY * 84f

        val paint = if (physics.launchAlignment >= OrbitConfig.MIN_LAUNCH_ALIGNMENT) {
            launchIndicatorGoodPaint
        } else {
            launchIndicatorBadPaint
        }

        canvas.drawLine(startX, startY, endX, endY, paint)
        canvas.drawCircle(endX, endY, 4.5f, paint)
    }

    private fun drawTrail(canvas: Canvas, physics: OrbitPhysics) {
        if (physics.trailCount < 2) return

        trailPath.rewind()
        var idx = (physics.trailHead - physics.trailCount + OrbitConfig.TRAIL_LENGTH) % OrbitConfig.TRAIL_LENGTH
        trailPath.moveTo(physics.trailX[idx], physics.trailY[idx])

        for (k in 1 until physics.trailCount) {
            idx = (idx + 1) % OrbitConfig.TRAIL_LENGTH
            trailPath.lineTo(physics.trailX[idx], physics.trailY[idx])
        }

        canvas.drawPath(trailPath, trailPaint)
    }

    private fun drawPlayer(canvas: Canvas, physics: OrbitPhysics) {
        if (physics.player.state == PlayerState.DEAD) return
        canvas.drawCircle(physics.player.x, physics.player.y, OrbitConfig.PLAYER_RADIUS, playerPaint)
    }

    private fun drawHud(canvas: Canvas, physics: OrbitPhysics, width: Int, bestScore: Int) {
        val centerX = width * 0.5f

        canvas.drawText("SCORE", centerX, 40f, labelPaint)
        canvas.drawText(physics.score.toString(), centerX, 106f, scorePaint)

        canvas.drawText("BEST $bestScore", width - 36f, 52f, bestPaint)

        if (physics.combo > 1) {
            canvas.drawText("COMBO x${physics.combo}", 34f, 52f, comboPaint)
        }

        val barWidth = 300f
        val barHeight = 14f
        val barLeft = centerX - barWidth * 0.5f
        val barTop = 122f

        beatRect.set(barLeft, barTop, barLeft + barWidth, barTop + barHeight)
        canvas.drawRoundRect(beatRect, 10f, 10f, beatTrackPaint)

        beatFillPaint.color = if (physics.launchAlignment >= OrbitConfig.MIN_LAUNCH_ALIGNMENT) {
            OrbitConfig.COLOR_BEAT_FILL_ACTIVE
        } else {
            OrbitConfig.COLOR_BEAT_FILL
        }

        val fillRight = barLeft + barWidth * physics.beatProgress.coerceIn(0f, 1f)
        if (fillRight > barLeft + 1f) {
            beatFillRect.set(barLeft, barTop, fillRight, barTop + barHeight)
            canvas.drawRoundRect(beatFillRect, 10f, 10f, beatFillPaint)
        }

        val status = if (physics.launchAlignment >= OrbitConfig.MIN_LAUNCH_ALIGNMENT) {
            "ANGLE OK  |  MISS IN ${physics.beatsLeft}"
        } else {
            "WRONG ANGLE  |  MISS IN ${physics.beatsLeft}"
        }
        canvas.drawText(status, centerX, 168f, labelPaint)

        if (physics.timingFlash > 0f) {
            val alpha = (physics.timingFlash * 255f).toInt().coerceIn(0, 255)
            timingFlashPaint.alpha = alpha
            timingFlashPaint.color = when (physics.lastTimingGrade) {
                TimingGrade.PERFECT -> 0xFFB8F8FF.toInt()
                TimingGrade.GOOD -> 0xFFE4F7FF.toInt()
                TimingGrade.MISS -> 0xFFC8D9E8.toInt()
            }

            val text = when (physics.lastTimingGrade) {
                TimingGrade.PERFECT -> "PERFECT +2"
                TimingGrade.GOOD -> "GOOD"
                TimingGrade.MISS -> "MISS"
            }
            canvas.drawText(text, centerX, 208f, timingFlashPaint)
        }
    }
}
