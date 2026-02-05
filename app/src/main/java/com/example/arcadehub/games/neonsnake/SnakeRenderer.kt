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

    private val snakePaint = Paint().apply { isAntiAlias = true }

    private val hudTextPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 40f, typeface = Typeface.DEFAULT_BOLD)
    private val bestScorePaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_BEST_TEXT, textSize = 35f, align = Paint.Align.CENTER)

    private val rect = RectF()

    fun draw(canvas: Canvas, physics: SnakePhysics, width: Int, height: Int, highScore: Int) {
        drawInternal(
            canvas, width, highScore,
            physics.player.body, physics.ai.body, physics.food,
            physics.player.score, physics.ai.score
        )

        // Draw Game Over Overlay here if needed, or in Scene
        if (physics.isGameOver) {
            drawGameOverOverlay(canvas, width, height, physics)
        }
    }

    fun drawReplayFrame(canvas: Canvas, snapshot: GameSnapshot, width: Int, height: Int, highScore: Int, physics: SnakePhysics) {
        drawInternal(
            canvas, width, highScore,
            snapshot.playerBody, snapshot.aiBody, snapshot.food,
            snapshot.pScore, snapshot.aScore
        )

        // Force draw the overlay on top of the replay
        drawGameOverOverlay(canvas, width, height, physics)

        GraphicsUtils.createPaint(Color.RED, textSize = 30f, align = Paint.Align.LEFT, typeface = Typeface.DEFAULT_BOLD).also {
            canvas.drawText("🔴 REPLAY", 40f, height - 40f, it)
        }
    }

    private fun drawInternal(
        canvas: Canvas, width: Int, highScore: Int,
        pBody: List<Point>, aBody: List<Point>, food: Point,
        pScore: Int, aScore: Int
    ) {
        canvas.drawColor(SnakeConfig.COLOR_BG)

        val cellSize = width.toFloat() / SnakeConfig.TILE_COUNT_X
        val gridW = cellSize * SnakeConfig.TILE_COUNT_X
        val gridH = cellSize * SnakeConfig.TILE_COUNT_Y
        val hudY = 60f

        // HUD
        hudTextPaint.textAlign = Paint.Align.LEFT
        hudTextPaint.color = Color.CYAN
        canvas.drawText("PLAYER: $pScore", 30f, hudY, hudTextPaint)

        canvas.drawText("BEST: $highScore", width / 2f, hudY, bestScorePaint)

        hudTextPaint.textAlign = Paint.Align.RIGHT
        hudTextPaint.color = SnakeConfig.COLOR_AI_BODY
        canvas.drawText("ROBOT: $aScore", width - 30f, hudY, hudTextPaint)

        // Grid
        for (i in 0..SnakeConfig.TILE_COUNT_X) {
            val x = i * cellSize
            canvas.drawLine(x, 0f, x, gridH, gridPaint)
        }
        for (i in 0..SnakeConfig.TILE_COUNT_Y) {
            val y = i * cellSize
            canvas.drawLine(0f, y, gridW, y, gridPaint)
        }

        // Food
        drawCell(canvas, food.x, food.y, cellSize, foodPaint, isHead = true, shrink = 0f)

        // 2. NON-REPETITIVE CALLS
        drawSnake(canvas, pBody, cellSize, SnakeConfig.COLOR_P1_HEAD, SnakeConfig.COLOR_P1_BODY, SnakeConfig.COLOR_P1_TAIL)
        drawSnake(canvas, aBody, cellSize, SnakeConfig.COLOR_AI_HEAD, SnakeConfig.COLOR_AI_BODY, SnakeConfig.COLOR_AI_TAIL)
    }

    private fun drawGameOverOverlay(canvas: Canvas, width: Int, height: Int, physics: SnakePhysics) {
        GraphicsUtils.drawGameOverMenu(
            canvas, width, height,
            title = physics.gameOverReason,
            scoreMsg = "SCORE: ${physics.player.score}",
            subMsg = "OPPONENT: ${physics.getBrainName()}",
            footerMsg = "CENTER to Reboot | BACK to Menu"
        )
    }

    private fun drawSnake(canvas: Canvas, body: List<Point>, cellSize: Float, hCol: Int, bCol: Int, tCol: Int) {
        body.forEachIndexed { index, point ->
            val isHead = index == 0

            // Calculate Color Gradient
            snakePaint.color = if (isHead) hCol else lerpColor(bCol, tCol, index.toFloat() / body.size)

            // Apply Glow to head only
            if (isHead) snakePaint.setShadowLayer(15f, 0f, 0f, Color.WHITE) else snakePaint.clearShadowLayer()

            // Calculate Tapering (Tail is 20% smaller than head)
            val shrink = if (isHead) 0f else (index.toFloat() / body.size) * (cellSize * 0.2f)

            drawCell(canvas, point.x, point.y, cellSize, snakePaint, isHead, shrink)
        }
    }

    private fun drawCell(canvas: Canvas, gx: Int, gy: Int, cellSize: Float, paint: Paint, isHead: Boolean, shrink: Float) {
        val padding = 2f + (shrink / 2f)
        rect.set(
            gx * cellSize + padding,
            gy * cellSize + padding,
            (gx + 1) * cellSize - padding,
            (gy + 1) * cellSize - padding
        )
        val radius = if (isHead) 12f else 6f
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun lerpColor(s: Int, e: Int, f: Float): Int {
        val r = (Color.red(s) + (Color.red(e) - Color.red(s)) * f).toInt()
        val g = (Color.green(s) + (Color.green(e) - Color.green(s)) * f).toInt()
        val b = (Color.blue(s) + (Color.blue(e) - Color.blue(s)) * f).toInt()
        return Color.rgb(r, g, b)
    }
}