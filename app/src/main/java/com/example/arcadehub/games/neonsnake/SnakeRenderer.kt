package com.example.arcadehub.games.neonsnake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.arcadehub.core.GraphicsUtils
import kotlin.math.roundToInt

class SnakeRenderer {

    private val boardBorderPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_BOARD_BORDER, Paint.Style.STROKE, strokeWidth = 4f)
    private val gridPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_GRID, Paint.Style.STROKE, strokeWidth = 2f)
    private val wallPaint = GraphicsUtils.createPaint(Color.DKGRAY).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2f
    }
    private val wallOutlinePaint = GraphicsUtils.createPaint(Color.GRAY, Paint.Style.STROKE, strokeWidth = 2f)

    private val foodFillPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_FOOD)
    private val foodOutlinePaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_FOOD_OUTLINE, Paint.Style.STROKE, strokeWidth = 3f)
    private val snakeBodyPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeCap = Paint.Cap.BUTT }
    private val snakeJointPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val snakeHeadPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val snakeEyePaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_SNAKE_EYE)
    private val headRect = RectF()
    private val hudTextPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 40f, typeface = Typeface.DEFAULT_BOLD)
    private val barBgPaint = GraphicsUtils.createPaint(Color.DKGRAY)
    private val barFillPaint = Paint().apply { style = Paint.Style.FILL }
    private val healthNumPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 20f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD).apply {
        setShadowLayer(2f, 0f, 0f, Color.BLACK)
    }
    private val rect = RectF()

    private var cachedCellSize = 0f

    fun updateDimensions(width: Int) {
        cachedCellSize = width.toFloat() / SnakeConfig.COLS
    }

    fun getCellSize(): Float = cachedCellSize

    fun draw(
        canvas: Canvas,
        physics: SnakePhysics,
        width: Int,
        height: Int,
        highScore: Int,
        isRobotMode: Boolean
    ) {
        if (cachedCellSize == 0f) updateDimensions(width)
        drawInternal(
            canvas, width,
            physics.player, physics.ai, physics.foods,
            physics.grid, highScore, isRobotMode, physics.speedDelay
        )

        if (physics.isGameOver) {
            drawOverlay(canvas, width, height, physics.gameOverReason, physics.player.score, physics.getDifficultyLevel())
        }
    }

    fun drawReplayFrame(
        canvas: Canvas, snap: GameSnapshot, width: Int, height: Int,
        physics: SnakePhysics, finalScore: Int
    ) {
        val p = SnakeEntity(ArrayList(snap.playerBody), GridDir.UP, snap.pScore, snap.pHealth)
        val a = SnakeEntity(ArrayList(snap.aiBody), GridDir.DOWN, snap.aScore, snap.aHealth)

        drawInternal(canvas, width, p, a, snap.foods, physics.grid, 0, false, 0f)

        drawOverlay(canvas, width, height, physics.gameOverReason, finalScore, physics.getDifficultyLevel())
        GraphicsUtils.createPaint(Color.RED, textSize = 30f, align = Paint.Align.LEFT, typeface = Typeface.DEFAULT_BOLD).also {
            canvas.drawText("REPLAY MODE", 40f, height - 40f, it)
        }
    }

    private fun drawInternal(
        canvas: Canvas, width: Int,
        player: SnakeEntity, ai: SnakeEntity, foods: List<Point>,
        grid: SnakeGrid, // Added grid
        highScore: Int, isRobotMode: Boolean, speedDelay: Float
    ) {
        canvas.drawColor(SnakeConfig.COLOR_BG)

        val cellSize = width.toFloat() / SnakeConfig.COLS
        val gridH = cellSize * SnakeConfig.ROWS
        val offsetY = 0f

        // Draw Grid Lines
        for (i in 0..SnakeConfig.COLS) {
            val x = i * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + gridH, gridPaint)
        }
        for (i in 0..SnakeConfig.ROWS) {
            val y = offsetY + i * cellSize
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        // Draw Board Border
        canvas.drawRect(0f, offsetY, width.toFloat(), offsetY + gridH, boardBorderPaint)

        // Draw Walls
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                if (grid[x, y] == 9) { // 9 is Wall
                    drawCell(canvas, x, y, cellSize, offsetY, wallPaint)
                    drawCell(canvas, x, y, cellSize, offsetY, wallOutlinePaint)
                }
            }
        }

        // Draw Foods
        foods.forEach { f -> drawFood(canvas, f.x, f.y, cellSize, offsetY) }

        // Draw Snakes
        drawSnake(canvas, player.body, cellSize, offsetY, SnakeConfig.COLOR_P1_HEAD, SnakeConfig.COLOR_P1_BODY)
        drawSnake(canvas, ai.body, cellSize, offsetY, SnakeConfig.COLOR_AI_HEAD, SnakeConfig.COLOR_AI_BODY)

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
        drawHealthBar(canvas, sideMargin + 160f, y - 12f, barW, barH, p.health, SnakeConfig.COLOR_P1_BODY)

        hudTextPaint.textAlign = Paint.Align.CENTER
        hudTextPaint.color = Color.YELLOW
        if (isRobotMode) {
            val ms = (speedDelay * 1000).roundToInt()
            canvas.drawText("DELAY: ${ms}ms", width / 2f, y + 10f, hudTextPaint)
        } else {
            canvas.drawText("BEST: $highScore", width / 2f, y + 10f, hudTextPaint)
        }

        // --- AI (RIGHT) ---
        hudTextPaint.textAlign = Paint.Align.RIGHT
        hudTextPaint.color = SnakeConfig.COLOR_AI_HEAD
        canvas.drawText("AI: ${a.score}", width - sideMargin, y + 10f, hudTextPaint)
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

    /** Center of a grid cell, in pixels. */
    private fun cellCenterX(gx: Int, size: Float) = gx * size + size / 2f
    private fun cellCenterY(gy: Int, size: Float, offY: Float) = offY + gy * size + size / 2f

    private fun drawSnake(canvas: Canvas, body: List<Point>, cellSize: Float, offY: Float, headCol: Int, bodyCol: Int) {
        if (body.isEmpty()) return

        val strokeWidth = cellSize * 0.55f
        val jointRadius = strokeWidth / 2f

        // Body: one continuous thick line with round joints, GitHub-snake style.
        if (body.size > 1) {
            snakeBodyPaint.color = bodyCol
            snakeBodyPaint.strokeWidth = strokeWidth
            snakeJointPaint.color = bodyCol

            for (i in 0 until body.size - 1) {
                val x1 = cellCenterX(body[i].x, cellSize)
                val y1 = cellCenterY(body[i].y, cellSize, offY)
                val x2 = cellCenterX(body[i + 1].x, cellSize)
                val y2 = cellCenterY(body[i + 1].y, cellSize, offY)
                canvas.drawLine(x1, y1, x2, y2, snakeBodyPaint)
            }
            for (i in 1 until body.size) {
                val cx = cellCenterX(body[i].x, cellSize)
                val cy = cellCenterY(body[i].y, cellSize, offY)
                canvas.drawCircle(cx, cy, jointRadius, snakeJointPaint)
            }
        }

        // Head: rounded square, drawn over the body.
        val head = body[0]
        val hx = cellCenterX(head.x, cellSize)
        val hy = cellCenterY(head.y, cellSize, offY)
        val headSize = cellSize * 0.8f
        headRect.set(hx - headSize / 2f, hy - headSize / 2f, hx + headSize / 2f, hy + headSize / 2f)
        snakeHeadPaint.color = headCol
        canvas.drawRoundRect(headRect, cellSize * 0.25f, cellSize * 0.25f, snakeHeadPaint)

        // Eyes: two dots offset in the direction of travel.
        val (dx, dy) = if (body.size > 1) {
            (head.x - body[1].x) to (head.y - body[1].y)
        } else {
            0 to 0
        }
        val (ox1, oy1, ox2, oy2) = when {
            dx > 0 -> floatArrayOf(0.2f, -0.2f, 0.2f, 0.2f)
            dx < 0 -> floatArrayOf(-0.2f, -0.2f, -0.2f, 0.2f)
            dy > 0 -> floatArrayOf(-0.2f, 0.2f, 0.2f, 0.2f)
            dy < 0 -> floatArrayOf(-0.2f, -0.2f, 0.2f, -0.2f)
            else -> floatArrayOf(0.2f, -0.2f, -0.2f, -0.2f)
        }

        val eyeRadius = cellSize * 0.12f
        canvas.drawCircle(hx + ox1 * cellSize, hy + oy1 * cellSize, eyeRadius, snakeEyePaint)
        canvas.drawCircle(hx + ox2 * cellSize, hy + oy2 * cellSize, eyeRadius, snakeEyePaint)
    }

    private fun drawFood(c: Canvas, gx: Int, gy: Int, size: Float, offY: Float) {
        val cx = cellCenterX(gx, size)
        val cy = cellCenterY(gy, size, offY)
        val radius = size * 0.35f
        c.drawCircle(cx, cy, radius, foodFillPaint)
        c.drawCircle(cx, cy, radius, foodOutlinePaint)
    }

    private fun drawCell(c: Canvas, gx: Int, gy: Int, size: Float, offY: Float, paint: Paint) {
        val pad = 4f
        rect.set(
            gx * size + pad,
            offY + gy * size + pad,
            (gx + 1) * size - pad,
            offY + (gy + 1) * size - pad
        )
        c.drawRoundRect(rect, 8f, 8f, paint)
    }

    private fun drawOverlay(canvas: Canvas, w: Int, h: Int, title: String, score: Int, level: Int) {
        GraphicsUtils.drawGameOverMenu(
            canvas, w, h,
            title = title,
            scoreMsg = "FINAL SCORE: $score",
            subMsg = "AI LEVEL: $level",
            footerMsg = "CENTER to Retry | BACK to Hub"
        )
    }
}
