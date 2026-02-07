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
    private val barBgPaint = GraphicsUtils.createPaint(Color.DKGRAY)
    private val barFillPaint = Paint().apply { style = Paint.Style.FILL }

    private val rect = RectF()

    fun draw(canvas: Canvas, physics: SnakePhysics, width: Int, height: Int, highScore: Int) {
        drawInternal(
            canvas, width,
            physics.player, physics.ai, physics.foods,
            highScore
        )

        if (physics.isGameOver) {
            drawOverlay(canvas, width, height, physics.gameOverReason, physics.player.score, physics.getDifficultyLevel())
        }
    }

    fun drawReplayFrame(canvas: Canvas, snap: GameSnapshot, width: Int, height: Int, physics: SnakePhysics) {
        val p = SnakeEntity(ArrayList(snap.playerBody), GridDir.UP, snap.pScore, snap.pHealth)
        val a = SnakeEntity(ArrayList(snap.aiBody), GridDir.DOWN, snap.aScore, snap.aHealth)

        drawInternal(canvas, width, p, a, snap.foods, 0)
        drawOverlay(canvas, width, height, physics.gameOverReason, snap.pScore, physics.getDifficultyLevel())

        GraphicsUtils.createPaint(Color.RED, textSize = 30f, align = Paint.Align.LEFT, typeface = Typeface.DEFAULT_BOLD).also {
            canvas.drawText("REPLAY MODE", 40f, height - 40f, it)
        }
    }

    private fun drawInternal(
        canvas: Canvas, width: Int,
        player: SnakeEntity, ai: SnakeEntity, foods: List<Point>,
        highScore: Int
    ) {
        canvas.drawColor(SnakeConfig.COLOR_BG)

        // 16x9 Aspect Ratio setup
        val cellSize = width.toFloat() / SnakeConfig.COLS
        val gridH = cellSize * SnakeConfig.ROWS

        // Centering vertically if logic height < screen height (likely on 16:9 screen it fits perfectly)
        val offsetY = 0f

        // Draw Grid
        for (i in 0..SnakeConfig.COLS) {
            val x = i * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + gridH, gridPaint)
        }
        for (i in 0..SnakeConfig.ROWS) {
            val y = offsetY + i * cellSize
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        // Draw Foods
        foods.forEach { f ->
            drawCell(canvas, f.x, f.y, cellSize, offsetY, foodPaint, true, 0f)
        }

        // Draw Snakes
        drawSnake(canvas, player.body, cellSize, offsetY, SnakeConfig.COLOR_P1_HEAD, SnakeConfig.COLOR_P1_BODY)
        drawSnake(canvas, ai.body, cellSize, offsetY, SnakeConfig.COLOR_AI_HEAD, SnakeConfig.COLOR_AI_BODY)

        // Draw HUD (Score + Health)
        drawHud(canvas, player, ai, width, gridH, highScore)
    }

    private fun drawHud(canvas: Canvas, p: SnakeEntity, a: SnakeEntity, width: Int, gridBottom: Float, highScore: Int) {
        val y = gridBottom + 50f
        val barW = 150f
        val barH = 16f

        // 1. Player Left
        hudTextPaint.textAlign = Paint.Align.LEFT
        hudTextPaint.color = SnakeConfig.COLOR_P1_HEAD
        canvas.drawText("P1: ${p.score}", 30f, y, hudTextPaint)
        drawHealthBar(canvas, 140f, y - 12f, barW, barH, p.health, Color.CYAN)

        // 2. BEST SCORE (Centered)
        hudTextPaint.textAlign = Paint.Align.CENTER
        hudTextPaint.color = Color.YELLOW
        canvas.drawText("BEST: $highScore", width / 2f, y, hudTextPaint)

        // 3. AI Right
        hudTextPaint.textAlign = Paint.Align.RIGHT
        hudTextPaint.color = SnakeConfig.COLOR_AI_HEAD
        canvas.drawText("AI: ${a.score}", width - 30f, y, hudTextPaint)
        drawHealthBar(canvas, width - 140f - barW, y - 12f, barW, barH, a.health, Color.MAGENTA)
    }


    private fun drawHealthBar(c: Canvas, x: Float, y: Float, w: Float, h: Float, hp: Int, color: Int) {
        c.drawRect(x, y, x + w, y + h, barBgPaint)
        val fillPct = (hp / 100f).coerceIn(0f, 1f)
        barFillPaint.color = color
        c.drawRect(x, y, x + (w * fillPct), y + h, barFillPaint)
    }

    private fun drawSnake(canvas: Canvas, body: List<Point>, cellSize: Float, offY: Float, hCol: Int, bCol: Int) {
        body.forEachIndexed { index, point ->
            val isHead = index == 0
            snakePaint.color = if (isHead) hCol else lerpColor(bCol, Color.BLACK, index.toFloat() / body.size)
            if (isHead) snakePaint.setShadowLayer(20f, 0f, 0f, hCol) else snakePaint.clearShadowLayer()

            val shrink = if (isHead) 0f else (index.toFloat() / body.size) * (cellSize * 0.25f)
            drawCell(canvas, point.x, point.y, cellSize, offY, snakePaint, isHead, shrink)
        }
    }

    private fun drawCell(c: Canvas, gx: Int, gy: Int, size: Float, offY: Float, paint: Paint, isHead: Boolean, shrink: Float) {
        val pad = 4f + shrink
        rect.set(
            gx * size + pad,
            offY + gy * size + pad,
            (gx + 1) * size - pad,
            offY + (gy + 1) * size - pad
        )
        val r = if (isHead) 16f else 8f
        c.drawRoundRect(rect, r, r, paint)
    }

    private fun drawOverlay(canvas: Canvas, w: Int, h: Int, title: String, score: Int, level: Int) {
        GraphicsUtils.drawGameOverMenu(
            canvas, w, h,
            title = title,
            scoreMsg = "FINAL SCORE: $score",
            subMsg = "AI LEVEL: $level (DEPTH ${level * 2})",
            footerMsg = "CENTER to Retry | BACK to Hub"
        )
    }

    private fun lerpColor(s: Int, e: Int, f: Float): Int {
        val r = (Color.red(s) + (Color.red(e) - Color.red(s)) * f).toInt()
        val g = (Color.green(s) + (Color.green(e) - Color.green(s)) * f).toInt()
        val b = (Color.blue(s) + (Color.blue(e) - Color.blue(s)) * f).toInt()
        return Color.rgb(r, g, b)
    }
}