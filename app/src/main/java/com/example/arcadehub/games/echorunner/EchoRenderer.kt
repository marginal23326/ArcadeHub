package com.example.arcadehub.games.echorunner

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.arcadehub.core.Constants
import java.util.*
import kotlin.math.abs

class EchoRenderer {

    private val fillBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
    private val fillWhite = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
    private val strokeBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 5f; isAntiAlias = true }
    private val strokeWhite = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 5f; isAntiAlias = true }

    private val textPaint = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val hudPaint = Paint().apply { textSize = 35f; typeface = Typeface.MONOSPACE; textAlign = Paint.Align.LEFT; isAntiAlias = true }

    // Caching
    private var cachedScore = -1
    private var cachedScoreStr = "0"
    private var cachedSpeedLevel = -1f
    private var cachedSpeedStr = "SPEED: 1.0x"
    private val particlePoints = FloatArray(EchoConfig.PARTICLE_COUNT * 2)

    fun draw(canvas: Canvas, physics: EchoPhysics, width: Int, height: Int, bestScore: Int) {
        val isEcho = physics.player.isEcho
        val fgColor = if (isEcho) EchoConfig.COLOR_ECHO_FG else EchoConfig.COLOR_REAL_FG

        canvas.drawColor(if (isEcho) EchoConfig.COLOR_ECHO_BG else EchoConfig.COLOR_REAL_BG)

        // Setup paints
        hudPaint.color = fgColor
        textPaint.color = fgColor
        val linePaint = if (isEcho) strokeWhite else strokeBlack
        val obsPaintSolid = if (isEcho) fillWhite else fillBlack
        val obsPaintGhost = if (isEcho) strokeWhite else strokeBlack

        // 1. Floor/Ceiling Lines
        val midY = height / 2f
        canvas.drawLine(0f, midY - 250f, width.toFloat(), midY - 250f, linePaint)
        canvas.drawLine(0f, midY + 250f, width.toFloat(), midY + 250f, linePaint)

        // 2. Obstacles
        val obstacles = physics.getActiveObstacles()
        for (obs in obstacles) {
            if (!obs.active) continue
            val isSolid = (obs.type == DimensionType.REAL && !isEcho) || (obs.type == DimensionType.ECHO && isEcho)

            if (isSolid) {
                canvas.drawRect(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h, obsPaintSolid)
            } else {
                canvas.drawRect(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h, obsPaintGhost)
                // Add an X to ghosts
                canvas.drawLine(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h, obsPaintGhost)
            }
        }

        // 3. Particles
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

        // 4. Player
        if (physics.state != GameState.GAMEOVER) {
            val player = physics.player
            canvas.drawRect(player.drawX, player.y, player.drawX + player.size, player.y + player.size, obsPaintSolid)
        }

        // 5. HUD
        updateStrings(physics.score, physics.getSpeed())

        // Speed in Top Center
        hudPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(cachedSpeedStr, width / 2f, 60f, hudPaint)

        // Best Score in Top Right
        hudPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("BEST: $bestScore", width - 30f, 60f, hudPaint)

        drawOverlays(canvas, physics, width, height)
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

    private fun drawOverlays(canvas: Canvas, physics: EchoPhysics, width: Int, height: Int) {
        if (physics.state == GameState.MENU) {
            textPaint.textSize = 100f
            canvas.drawText("ECHO RUNNER", width / 2f, height / 2f - 50f, textPaint)
            textPaint.textSize = 40f
            // Blink effect
            if ((System.currentTimeMillis() / 600) % 2 == 0L) {
                canvas.drawText("HOLD BUTTON TO ENTER ECHO", width / 2f, height / 2f + 50f, textPaint)
            }
        } else if (physics.state == GameState.PLAYING) {
            textPaint.textSize = 70f
            canvas.drawText(cachedScoreStr, width / 2f, 130f, textPaint)
        } else if (physics.state == GameState.GAMEOVER) {
            textPaint.textSize = 90f
            canvas.drawText("DIMENSION CRASH", width / 2f, height / 2f - 40f, textPaint)
            textPaint.textSize = 45f
            canvas.drawText("FINAL SCORE: $cachedScoreStr", width / 2f, height / 2f + 50f, textPaint)

            if (physics.canRestart()) {
                textPaint.textSize = 30f
                if ((System.currentTimeMillis() / 400) % 2 == 0L) {
                    canvas.drawText("RELEASE BUTTON TO RESET", width / 2f, height / 2f + 120f, textPaint)
                }
            }
        }
    }
}