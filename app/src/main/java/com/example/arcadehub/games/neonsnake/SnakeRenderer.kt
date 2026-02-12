package com.example.arcadehub.games.neonsnake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.arcadehub.core.GraphicsUtils
import kotlin.math.roundToInt

class SnakeRenderer {

    private val gridPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_GRID, Paint.Style.STROKE, strokeWidth = 2f)
    private val foodPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_FOOD).apply {
        setShadowLayer(15f, 0f, 0f, SnakeConfig.COLOR_FOOD)
    }

    private val snakePaint = Paint().apply { isAntiAlias = true }
    private val hudTextPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 40f, typeface = Typeface.DEFAULT_BOLD)
    private val barBgPaint = GraphicsUtils.createPaint(Color.DKGRAY)
    private val barFillPaint = Paint().apply { style = Paint.Style.FILL }
    private val healthNumPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 20f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD).apply {
        setShadowLayer(2f, 0f, 0f, Color.BLACK)
    }
    private val rect = RectF()

    fun draw(
        canvas: Canvas,
        physics: SnakePhysics,
        width: Int,
        height: Int,
        highScore: Int,
        isRobotMode: Boolean
    ) {
        drawInternal(
            canvas, width,
            physics.player, physics.ai, physics.foods,
            highScore, isRobotMode, physics.speedDelay
        )

        if (physics.isGameOver) {
            drawOverlay(canvas, width, height, physics.gameOverReason, physics.player.score, physics.getDifficultyLevel())
        }
    }

    fun drawReplayFrame(
        canvas: Canvas, snap: GameSnapshot, width: Int, height: Int,
        physics: SnakePhysics, finalScore: Int
    ) {
        // Create temp entities for drawing the replay moment
        val p = SnakeEntity(ArrayList(snap.playerBody), GridDir.UP, snap.pScore, snap.pHealth)
        val a = SnakeEntity(ArrayList(snap.aiBody), GridDir.DOWN, snap.aScore, snap.aHealth)

        // Draw the replay frame
        drawInternal(canvas, width, p, a, snap.foods, 0, false, 0f)

        drawOverlay(canvas, width, height, physics.gameOverReason, finalScore, physics.getDifficultyLevel())

        // Replay Text
        GraphicsUtils.createPaint(Color.RED, textSize = 30f, align = Paint.Align.LEFT, typeface = Typeface.DEFAULT_BOLD).also {
            canvas.drawText("REPLAY MODE", 40f, height - 40f, it)
        }
    }

    private fun drawInternal(
        canvas: Canvas, width: Int,
        player: SnakeEntity, ai: SnakeEntity, foods: List<Point>,
        highScore: Int, isRobotMode: Boolean, speedDelay: Float
    ) {
        canvas.drawColor(SnakeConfig.COLOR_BG)

        // 16x9 Aspect Ratio setup
        val cellSize = width.toFloat() / SnakeConfig.COLS
        val gridH = cellSize * SnakeConfig.ROWS

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
        drawSnake(canvas, player.body, cellSize, offsetY, SnakeConfig.COLOR_P1_HEAD, SnakeConfig.COLOR_P1_BODY, SnakeConfig.COLOR_P1_TAIL)
        drawSnake(canvas, ai.body, cellSize, offsetY, SnakeConfig.COLOR_AI_HEAD, SnakeConfig.COLOR_AI_BODY, SnakeConfig.COLOR_AI_TAIL)

        // CRITICAL: Draw HUD (Score + Health)
        drawHud(canvas, player, ai, width, highScore, isRobotMode, speedDelay)
    }

    private fun drawHud(
        canvas: Canvas, p: SnakeEntity, a: SnakeEntity,
        width: Int,
        highScore: Int, isRobotMode: Boolean, speedDelay: Float
    ) {
        val headerHeight = 70f
        val y = 45f

        val barW = 250f
        val barH = 24f

        val sideMargin = 250f

        val headerPaint = Paint().apply { color = Color.BLACK; alpha = 180 }
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)

        // --- PLAYER 1 (LEFT) ---
        hudTextPaint.textAlign = Paint.Align.LEFT
        hudTextPaint.color = SnakeConfig.COLOR_P1_HEAD
        val pLabel = if (isRobotMode) "BOT" else "P1"
        canvas.drawText("$pLabel: ${p.score}", sideMargin, y + 10f, hudTextPaint)

        // Space for text is 160f, bar follows
        drawHealthBar(canvas, sideMargin + 160f, y - 12f, barW, barH, p.health, SnakeConfig.COLOR_P1_BODY)

        // --- CENTER INFO ---
        hudTextPaint.textAlign = Paint.Align.CENTER
        hudTextPaint.color = Color.YELLOW

        if (isRobotMode) {
            // Show Speed Delay in Robot Mode
            val ms = (speedDelay * 1000).roundToInt()
            canvas.drawText("DELAY: ${ms}ms", width / 2f, y + 10f, hudTextPaint)
        } else {
            // Show High Score in Human Mode
            canvas.drawText("BEST: $highScore", width / 2f, y + 10f, hudTextPaint)
        }

        // --- AI (RIGHT) ---
        hudTextPaint.textAlign = Paint.Align.RIGHT
        hudTextPaint.color = SnakeConfig.COLOR_AI_HEAD
        canvas.drawText("AI: ${a.score}", width - sideMargin, y + 10f, hudTextPaint)

        // Space for text is 160f, bar is placed to the left of the text
        drawHealthBar(canvas, width - sideMargin - 160f - barW, y - 12f, barW, barH, a.health, SnakeConfig.COLOR_AI_BODY)
    }

    private fun drawHealthBar(c: Canvas, x: Float, y: Float, w: Float, h: Float, hp: Int, color: Int) {
        // Background (Gray)
        c.drawRect(x, y, x + w, y + h, barBgPaint)

        // Foreground (Health Color)
        val fillPct = (hp / 100f).coerceIn(0f, 1f)
        barFillPaint.color = color
        c.drawRect(x, y, x + (w * fillPct), y + h, barFillPaint)

        // Health Number Text (Centered in bar)
        val cx = x + (w / 2f)
        val cy = y + (h / 2f) + 8f // approx vertical center
        c.drawText("$hp", cx, cy, healthNumPaint)
    }

    private fun drawSnake(canvas: Canvas, body: List<Point>, cellSize: Float, offY: Float, hCol: Int, bCol: Int, tCol: Int) {
        body.forEachIndexed { index, point ->
            val isHead = index == 0
            snakePaint.color = if (isHead) hCol else lerpColor(bCol, tCol, index.toFloat() / body.size)

            if (isHead) snakePaint.setShadowLayer(20f, 0f, 0f, hCol)
            else snakePaint.clearShadowLayer()

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