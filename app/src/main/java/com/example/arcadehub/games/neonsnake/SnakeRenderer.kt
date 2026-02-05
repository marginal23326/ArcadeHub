package com.example.arcadehub.games.neonsnake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.arcadehub.core.GraphicsUtils

class SnakeRenderer {

    private val gridPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_GRID, Paint.Style.STROKE, strokeWidth = 2f)
    private val foodPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_FOOD).apply {
        setShadowLayer(15f, 0f, 0f, SnakeConfig.COLOR_FOOD)
    }

    private val p1HeadPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_P1_HEAD).apply { setShadowLayer(10f, 0f, 0f, Color.WHITE) }
    private val p1BodyPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_P1_BODY)

    private val aiHeadPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_AI_HEAD).apply { setShadowLayer(10f, 0f, 0f, Color.WHITE) }
    private val aiBodyPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_AI_BODY)

    private val hudTextPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 40f, typeface = Typeface.DEFAULT_BOLD)
    private val bestScorePaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_BEST_TEXT, textSize = 35f, align = Paint.Align.CENTER)

    private val rect = RectF()

    fun draw(canvas: Canvas, physics: SnakePhysics, width: Int, height: Int, highScore: Int) {
        canvas.drawColor(SnakeConfig.COLOR_BG)

        val cellSize = width.toFloat() / SnakeConfig.TILE_COUNT_X
        val gridW = cellSize * SnakeConfig.TILE_COUNT_X
        val gridH = cellSize * SnakeConfig.TILE_COUNT_Y

        val offsetX = 0f
        val offsetY = 0f

        val hudY = 60f

        hudTextPaint.textAlign = Paint.Align.LEFT
        hudTextPaint.color = Color.CYAN
        canvas.drawText("PLAYER: ${physics.player.score}", 30f, hudY, hudTextPaint)

        canvas.drawText("BEST: $highScore", width / 2f, hudY, bestScorePaint)

        hudTextPaint.textAlign = Paint.Align.RIGHT
        hudTextPaint.color = SnakeConfig.COLOR_AI_BODY
        canvas.drawText("ROBOT: ${physics.ai.score}", width - 30f, hudY, hudTextPaint)

        // Draw Board
        canvas.save()
        canvas.translate(offsetX, offsetY)

        // Draw Grid Lines
        for (i in 0..SnakeConfig.TILE_COUNT_X) {
            val x = i * cellSize
            canvas.drawLine(x, 0f, x, gridH, gridPaint)
        }
        for (i in 0..SnakeConfig.TILE_COUNT_Y) {
            val y = i * cellSize
            canvas.drawLine(0f, y, gridW, y, gridPaint)
        }

        // Draw Food
        drawCell(canvas, physics.food.x, physics.food.y, cellSize, foodPaint, true)

        // Draw Player
        physics.player.body.forEachIndexed { index, point ->
            val paint = if (index == 0) p1HeadPaint else p1BodyPaint
            drawCell(canvas, point.x, point.y, cellSize, paint, index == 0)
        }

        // Draw AI
        physics.ai.body.forEachIndexed { index, point ->
            val paint = if (index == 0) aiHeadPaint else aiBodyPaint
            drawCell(canvas, point.x, point.y, cellSize, paint, index == 0)
        }

        canvas.restore()

        // Game Over Overlay
        if (physics.isGameOver) {
            GraphicsUtils.drawGameOverMenu(
                canvas, width, height,
                title = physics.gameOverReason,
                scoreMsg = "SCORE: ${physics.player.score}", // Secondary text
                subMsg = "OPPONENT: ${physics.getBrainName()}", // Small detail text
                footerMsg = "CENTER to Reboot | BACK to Menu"
            )
        }
    }

    private fun drawCell(canvas: Canvas, gx: Int, gy: Int, cellSize: Float, paint: Paint, isRound: Boolean) {
        val padding = 2f
        rect.set(gx * cellSize + padding, gy * cellSize + padding, (gx + 1) * cellSize - padding, (gy + 1) * cellSize - padding)
        if (isRound) {
            canvas.drawRoundRect(rect, 8f, 8f, paint)
        } else {
            canvas.drawRect(rect, paint)
        }
    }
}