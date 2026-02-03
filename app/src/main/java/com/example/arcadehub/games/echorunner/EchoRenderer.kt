package com.example.arcadehub.games.echorunner

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.arcadehub.core.GraphicsUtils
import java.util.Locale
import kotlin.math.abs

class EchoRenderer {

    // Paint creation
    private val fillBlack = GraphicsUtils.createPaint(Color.BLACK)
    private val fillWhite = GraphicsUtils.createPaint(Color.WHITE)
    private val strokeBlack = GraphicsUtils.createPaint(Color.BLACK, Paint.Style.STROKE, strokeWidth = 5f)
    private val strokeWhite = GraphicsUtils.createPaint(Color.WHITE, Paint.Style.STROKE, strokeWidth = 5f)

    private val textPaint = GraphicsUtils.createPaint(Color.WHITE, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val hudPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 35f, typeface = Typeface.MONOSPACE)

    private var cachedScore = -1
    private var cachedScoreStr = "0"
    private var cachedSpeedLevel = -1f
    private var cachedSpeedStr = "SPEED: 1.0x"
    private val particlePoints = FloatArray(EchoConfig.PARTICLE_COUNT * 2)

    fun draw(canvas: Canvas, scene: EchoRunnerScene, physics: EchoPhysics, width: Int, height: Int, bestScore: Int) {
        val isEcho = physics.player.isEcho
        val fgColor = if (isEcho) EchoConfig.COLOR_ECHO_FG else EchoConfig.COLOR_REAL_FG

        canvas.drawColor(if (isEcho) EchoConfig.COLOR_ECHO_BG else EchoConfig.COLOR_REAL_BG)

        hudPaint.color = fgColor
        textPaint.color = fgColor
        val linePaint = if (isEcho) strokeWhite else strokeBlack
        val obsPaintSolid = if (isEcho) fillWhite else fillBlack
        val obsPaintGhost = if (isEcho) strokeWhite else strokeBlack

        val midY = height / 2f
        canvas.drawLine(0f, midY - 250f, width.toFloat(), midY - 250f, linePaint)
        canvas.drawLine(0f, midY + 250f, width.toFloat(), midY + 250f, linePaint)

        val obstacles = physics.getActiveObstacles()
        for (obs in obstacles) {
            if (!obs.active) continue
            val isSolid = (obs.type == DimensionType.REAL && !isEcho) || (obs.type == DimensionType.ECHO && isEcho)
            if (isSolid) {
                canvas.drawRect(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h, obsPaintSolid)
            } else {
                canvas.drawRect(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h, obsPaintGhost)
                canvas.drawLine(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h, obsPaintGhost)
            }
        }

        var pCount = 0
        for (part in physics.particles) {
            if (!part.active) continue
            particlePoints[pCount++] = part.x
            particlePoints[pCount++] = part.y
        }
        if (pCount > 0) {
            obsPaintSolid.strokeWidth = 10f
            canvas.drawPoints(particlePoints, 0, pCount, obsPaintSolid)
        }

        if (scene.isGameStarted && !scene.isGameOver) {
            val player = physics.player
            canvas.drawRect(player.drawX, player.y, player.drawX + player.size, player.y + player.size, obsPaintSolid)
        }

        updateStrings(physics.score, physics.getSpeed())

        hudPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(cachedSpeedStr, width / 2f, 60f, hudPaint)
        hudPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("BEST: $bestScore", width - 30f, 60f, hudPaint)

        drawOverlays(canvas, scene, physics, width, height)
    }

    private fun updateStrings(score: Int, speed: Float) {
        if (score != cachedScore) {
            cachedScore = score
            cachedScoreStr = score.toString()
        }
        val speedMultiplier = speed / EchoConfig.BASE_SPEED
        if (abs(speedMultiplier - cachedSpeedLevel) > 0.05f) {
            cachedSpeedLevel = speedMultiplier
            cachedSpeedStr = "SPEED: ${String.format(Locale.US, "%.1fx", speedMultiplier)}"
        }
    }

    private fun drawOverlays(canvas: Canvas, scene: EchoRunnerScene, physics: EchoPhysics, width: Int, height: Int) {
        if (!scene.isGameStarted) {
            textPaint.textSize = 100f
            canvas.drawText("ECHO RUNNER", width / 2f, height / 2f - 50f, textPaint)
            textPaint.textSize = 40f
            if ((System.currentTimeMillis() / 600) % 2 == 0L) {
                canvas.drawText("HOLD BUTTON TO ENTER ECHO", width / 2f, height / 2f + 50f, textPaint)
            }
        }
        else if (scene.isPaused) {
            GraphicsUtils.drawPauseMenu(canvas, width, height, cachedScoreStr)
        }
        else if (scene.isGameOver) {
            val sub = if ((System.currentTimeMillis() / 400) % 2 == 0L) "RELEASE BUTTON TO RESET" else null
            GraphicsUtils.drawGameOverMenu(
                canvas, width, height,
                "DIMENSION CRASH",
                "FINAL SCORE: $cachedScoreStr",
                sub
            )
        }
        else {
            textPaint.textSize = 70f
            canvas.drawText(cachedScoreStr, width / 2f, 130f, textPaint)
        }
    }
}