package com.example.arcadehub.games.blockstack

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.arcadehub.core.GraphicsUtils
import kotlin.random.Random

class BlockRenderer {
    private val shopItems = AbilityType.entries.toTypedArray()
    private val shopCostStrings = shopItems.associateWith { "$${it.cost}" }
    private val countStrings = Array(21) { "x$it" }
    private val ownStrings = Array(21) { "Own: $it" }

    private val abilityBoxRect = RectF()
    private val shopItemRect = RectF()
    private val startBtnRect = RectF()
    private val arcRect = RectF()
    private val modeBtnRect = RectF()

    private val abilityBoxPaint = GraphicsUtils.createPaint(Color.WHITE, Paint.Style.STROKE, strokeWidth = 3f)
    private val blockPaint = GraphicsUtils.createPaint(Color.WHITE, Paint.Style.FILL)

    private val titlePaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 80f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val scorePaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 100f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val bestScorePaint = GraphicsUtils.createPaint(Color.LTGRAY, textSize = 40f, align = Paint.Align.CENTER)

    private val feedbackPaint = GraphicsUtils.createPaint(Color.WHITE, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

    private val shopTextPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 30f, align = Paint.Align.CENTER)
    private val hudTextPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 30f, align = Paint.Align.LEFT, typeface = Typeface.DEFAULT_BOLD)
    private val hudCoinPaint = GraphicsUtils.createPaint(BlockConfig.COLOR_COIN, textSize = 35f, align = Paint.Align.RIGHT, typeface = Typeface.MONOSPACE)

    private val selectionPaint = GraphicsUtils.createPaint(BlockConfig.SELECTION_COLOR, Paint.Style.STROKE, strokeWidth = 8f)
    private val shopBoxPaint = GraphicsUtils.createPaint(BlockConfig.SHOP_BG_COLOR, Paint.Style.FILL)

    private val overlayPaint = GraphicsUtils.createPaint(Color.BLACK, alpha = 180)

    private var lastRenderedScore = -1
    private var scoreString = "0"

    fun draw(
        canvas: Canvas,
        scene: BlockStackScene,
        physics: PhysicsStrategy,
        screenWidth: Int,
        screenHeight: Int
    ) {
        canvas.drawColor(BlockConfig.BG_COLOR)

        if (!scene.isGameStarted) {
            drawStartScreen(canvas, scene, screenWidth, screenHeight)
        } else {
            if (scene.gameMode == GameMode.LINEAR) {
                drawLinearWorld(canvas, scene, physics as LinearPhysics, screenWidth, screenHeight)
            } else {
                drawRadialWorld(canvas, scene, physics as RadialPhysics, screenWidth, screenHeight)
            }

            drawHUD(canvas, scene, screenWidth)

            if (scene.isPaused) {
                GraphicsUtils.drawPauseMenu(canvas, screenWidth, screenHeight)
            }

            if (scene.isGameOver) {
                drawGameOverUI(canvas, scene, screenWidth, screenHeight)
            }
        }
    }

    private fun drawStartScreen(canvas: Canvas, scene: BlockStackScene, w: Int, h: Int) {
        val cx = w / 2f
        val cy = h / 2f

        titlePaint.textSize = 80f
        canvas.drawText("BLOCK STACK", cx, 120f, titlePaint)

        bestScorePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("BEST: ${scene.highScore}", w - 50f, 60f, bestScorePaint)

        // Mode Button
        val modeBtnWidth = 400f; val modeBtnHeight = 60f
        val modeBtnX = cx - modeBtnWidth / 2; val modeBtnY = 160f
        modeBtnRect.set(modeBtnX, modeBtnY, modeBtnX + modeBtnWidth, modeBtnY + modeBtnHeight)

        shopBoxPaint.color = Color.DKGRAY
        canvas.drawRect(modeBtnRect, shopBoxPaint)
        if (scene.shopSelectionIndex == -1) canvas.drawRect(modeBtnRect, selectionPaint)

        shopTextPaint.textSize = 35f
        shopTextPaint.color = Color.CYAN
        canvas.drawText("MODE: ${scene.gameMode.displayName.uppercase()} < >", cx, modeBtnY + 42f, shopTextPaint)

        shopTextPaint.color = BlockConfig.COLOR_COIN
        canvas.drawText("BANK: ${BlockEconomy.getCoins()}", cx, 250f, shopTextPaint)
        shopTextPaint.color = Color.WHITE

        // Shop Grid
        val itemSize = 180f; val padding = 40f
        val startX = cx - (2 * itemSize + 1.5f * padding)
        val startY = cy - 80f

        for (i in shopItems.indices) {
            val item = shopItems[i]
            val x = startX + (i * (itemSize + padding))
            shopItemRect.set(x, startY, x + itemSize, startY + itemSize)

            shopBoxPaint.color = BlockConfig.SHOP_BG_COLOR
            canvas.drawRect(shopItemRect, shopBoxPaint)
            if (scene.shopSelectionIndex == i) canvas.drawRect(shopItemRect, selectionPaint)

            shopTextPaint.textSize = 35f
            canvas.drawText(item.displayName, x + itemSize / 2, startY + 50f, shopTextPaint)

            shopTextPaint.textSize = 50f
            canvas.drawText(item.symbol, x + itemSize / 2, startY + 100f, shopTextPaint)
            shopTextPaint.textSize = 25f
            shopTextPaint.color = if (BlockEconomy.getCoins() >= item.cost) Color.YELLOW else Color.GRAY
            canvas.drawText(shopCostStrings[item] ?: "", x + itemSize / 2, startY + 140f, shopTextPaint)

            shopTextPaint.color = Color.WHITE
            val owned = BlockEconomy.getInventoryCount(item)
            val ownStr = if (owned < ownStrings.size) ownStrings[owned] else "Own: $owned"
            canvas.drawText(ownStr, x + itemSize / 2, startY + 170f, shopTextPaint)
        }

        // Start Button
        val btnWidth = 400f; val btnHeight = 80f; val btnX = cx - btnWidth / 2; val btnY = startY + itemSize + 80f
        startBtnRect.set(btnX, btnY, btnX + btnWidth, btnY + btnHeight)
        shopBoxPaint.color = if (scene.shopSelectionIndex == 4) Color.DKGRAY else BlockConfig.SHOP_BG_COLOR
        canvas.drawRect(startBtnRect, shopBoxPaint)
        if (scene.shopSelectionIndex == 4) canvas.drawRect(startBtnRect, selectionPaint)

        shopTextPaint.textSize = 40f
        canvas.drawText("START GAME", cx, btnY + 55f, shopTextPaint)

        shopTextPaint.textSize = 25f; shopTextPaint.color = Color.LTGRAY
        canvas.drawText("D-PAD: Move | CENTER: Select | Mode: Top Row L/R | BACK: Exit", cx, h - 50f, shopTextPaint)
    }

    private fun drawLinearWorld(canvas: Canvas, scene: BlockStackScene, physics: LinearPhysics, w: Int, h: Int) {
        blockPaint.style = Paint.Style.FILL
        blockPaint.strokeWidth = 0f

        val cx = w / 2f
        val cy = h / 2f
        var jitterX = 0f; var jitterY = 0f
        if (scene.shakeTimer > 0) {
            jitterX = (Random.nextFloat() - 0.5f) * BlockConfig.SHAKE_INTENSITY
            jitterY = (Random.nextFloat() - 0.5f) * BlockConfig.SHAKE_INTENSITY
        }

        canvas.save()
        canvas.translate(jitterX, jitterY)
        val cam = scene.cameraSystem
        if (cam.currentScale != 1f) canvas.scale(cam.currentScale, cam.currentScale, cx, cy)
        canvas.translate(cam.cameraOffsetX, cam.cameraOffsetY)

        val s = cam.currentScale
        val viewTop = (cy - (cy / s) - cam.cameraOffsetY) - BlockConfig.BLOCK_HEIGHT
        val viewBottom = (cy + (h - cy) / s - cam.cameraOffsetY)

        val debris = physics.debrisList
        for (i in 0 until debris.size) {
            val d = debris[i]
            if (d.y > viewTop && d.y < viewBottom) {
                canvas.save()
                canvas.rotate(d.rotation, d.x + d.width/2, d.y + BlockConfig.BLOCK_HEIGHT/2)
                blockPaint.color = d.color
                canvas.drawRect(d.x, d.y, d.x + d.width, d.y + BlockConfig.BLOCK_HEIGHT, blockPaint)
                canvas.restore()
            }
        }

        val stack = physics.stackList
        for (i in 0 until stack.size) {
            val block = stack[i]
            if (block.y > viewTop && block.y < viewBottom) {
                blockPaint.color = block.color
                canvas.drawRect(block.x, block.y, block.x + block.width, block.y + BlockConfig.BLOCK_HEIGHT, blockPaint)
            }
        }

        if (!scene.isGameOver) {
            blockPaint.color = BlockConfig.COLORS[scene.score % BlockConfig.COLORS.size]
            canvas.drawRect(physics.currentBlockX, physics.stackY, physics.currentBlockX + physics.currentBlockWidth, physics.stackY + BlockConfig.BLOCK_HEIGHT, blockPaint)
        }
        canvas.restore()
    }

    private fun drawRadialWorld(canvas: Canvas, scene: BlockStackScene, physics: RadialPhysics, w: Int, h: Int) {
        val cx = w / 2f
        val cy = h / 2f

        var jitterX = 0f; var jitterY = 0f
        if (scene.shakeTimer > 0) {
            jitterX = (Random.nextFloat() - 0.5f) * BlockConfig.SHAKE_INTENSITY
            jitterY = (Random.nextFloat() - 0.5f) * BlockConfig.SHAKE_INTENSITY
        }

        canvas.save()
        canvas.translate(jitterX, jitterY)
        val cam = scene.cameraSystem
        canvas.scale(cam.currentScale, cam.currentScale, cx, cy)
        canvas.translate(cam.cameraOffsetX, cam.cameraOffsetY)

        // Stack - Index Loop
        blockPaint.style = Paint.Style.STROKE
        blockPaint.strokeCap = Paint.Cap.BUTT
        val stack = physics.stackList
        for (i in 0 until stack.size) {
            val block = stack[i]
            blockPaint.color = block.color
            blockPaint.strokeWidth = BlockConfig.BLOCK_HEIGHT
            val r = block.radius
            arcRect.set(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(arcRect, block.startAngle, block.sweepAngle, false, blockPaint)
        }

        // Debris - Index Loop
        val debris = physics.debrisList
        for (i in 0 until debris.size) {
            val d = debris[i]
            if (d.isRadial) {
                blockPaint.style = Paint.Style.STROKE
                blockPaint.strokeWidth = d.height
                blockPaint.color = d.color
                val r = d.width
                val dcx = cx + d.x; val dcy = cy + d.y
                arcRect.set(dcx - r, dcy - r, dcx + r, dcy + r)
                canvas.save()
                canvas.rotate(d.rotation, dcx, dcy)
                canvas.drawArc(arcRect, d.startAngle, d.sweepAngle, false, blockPaint)
                canvas.restore()
            } else {
                canvas.save()
                canvas.translate(cx + d.x, cy + d.y)
                canvas.rotate(d.rotation)
                blockPaint.color = d.color
                blockPaint.style = Paint.Style.FILL
                canvas.drawRect(-d.width/2, -d.height/2, d.width/2, d.height/2, blockPaint)
                canvas.restore()
            }
        }

        // Current
        if (!scene.isGameOver) {
            blockPaint.style = Paint.Style.STROKE
            blockPaint.strokeWidth = BlockConfig.BLOCK_HEIGHT
            blockPaint.color = BlockConfig.COLORS[scene.score % BlockConfig.COLORS.size]
            val r = physics.currentRadius
            val start = physics.currentAngle - (physics.currentSweep / 2f)
            arcRect.set(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(arcRect, start, physics.currentSweep, false, blockPaint)
        }
        canvas.restore()
    }

    private fun drawHUD(canvas: Canvas, scene: BlockStackScene, w: Int) {
        if (scene.isGameOver) return
        val cx = w / 2f

        if (scene.score != lastRenderedScore) {
            scoreString = scene.score.toString()
            lastRenderedScore = scene.score
        }

        canvas.drawText(scoreString, cx, 130f, scorePaint)

        bestScorePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("BEST: ${scene.highScore}", cx, 180f, bestScorePaint)

        if (scene.feedbackAlpha > 0) {
            feedbackPaint.color = scene.feedbackColor
            feedbackPaint.alpha = scene.feedbackAlpha
            feedbackPaint.textSize = 80f
            canvas.drawText(scene.feedbackText, cx, 280f + (150f - scene.feedbackY), feedbackPaint)

            if (scene.feedbackSubText.isNotEmpty()) {
                feedbackPaint.textSize = 50f
                feedbackPaint.color = BlockConfig.COLOR_COIN
                feedbackPaint.alpha = scene.feedbackAlpha
                canvas.drawText(scene.feedbackSubText, cx, 340f + (150f - scene.feedbackY), feedbackPaint)
            }
        }

        canvas.drawText("+$${scene.runCoins}", w - 30f, 60f, hudCoinPaint)

        val revives = BlockEconomy.getInventoryCount(AbilityType.SECOND_CHANCE)
        if (revives > 0) {
            hudCoinPaint.color = Color.RED
            canvas.drawText("♥ $revives", w - 30f, 110f, hudCoinPaint)
        }

        val startY = 300f; val gap = 120f; val x = 30f
        drawAbilityRow(canvas, x, startY, AbilityType.SLO_MO, "◄", scene.activeSloMoTurns > 0)
        drawAbilityRow(canvas, x, startY + gap, AbilityType.MAGNET, "▲", scene.isMagnetActive)
        drawAbilityRow(canvas, x, startY + gap * 2, AbilityType.WIDENER, "►", false)
    }

    private fun drawAbilityRow(canvas: Canvas, x: Float, y: Float, type: AbilityType, key: String, isActive: Boolean) {
        val count = BlockEconomy.getInventoryCount(type)
        val boxSize = 80f
        abilityBoxRect.set(x, y, x + boxSize, y + boxSize)
        abilityBoxPaint.color = if (isActive) Color.GREEN else if (count > 0) Color.WHITE else Color.DKGRAY
        canvas.drawRect(abilityBoxRect, abilityBoxPaint)

        hudTextPaint.textSize = 40f; hudTextPaint.textAlign = Paint.Align.CENTER
        hudTextPaint.color = if (count > 0) Color.WHITE else Color.GRAY
        canvas.drawText(key, x + boxSize/2, y + boxSize/2 + 15f, hudTextPaint)

        hudTextPaint.textAlign = Paint.Align.LEFT; hudTextPaint.textSize = 30f
        hudTextPaint.color = if (isActive) Color.GREEN else if (count > 0) Color.WHITE else Color.GRAY
        canvas.drawText(type.displayName, x + boxSize + 20f, y + boxSize/2 - 5f, hudTextPaint)

        hudTextPaint.textSize = 24f; hudTextPaint.color = if (count > 0) Color.LTGRAY else Color.DKGRAY
        val countStr = if (count < countStrings.size) countStrings[count] else "x$count"
        canvas.drawText(countStr, x + boxSize + 20f, y + boxSize/2 + 25f, hudTextPaint)
    }

    private fun drawGameOverUI(canvas: Canvas, scene: BlockStackScene, w: Int, h: Int) {
        val cx = w / 2f
        val timeSinceDeath = System.currentTimeMillis() - scene.gameOverTimestamp
        val isOrbit = scene.gameMode == GameMode.ORBIT

        if (scene.isZoomedOut) {
            GraphicsUtils.drawOverlay(canvas, w, h)
            shopTextPaint.textSize = 35f; shopTextPaint.color = Color.WHITE
            canvas.drawText("▼ Reset Zoom | CENTER Retry | BACK Menu", cx, h - 60f, shopTextPaint)
            return
        }

        val startY = h / 3.5f
        titlePaint.textSize = 100f
        canvas.drawText("GAME OVER", cx, startY, titlePaint)
        shopTextPaint.textSize = 50f; shopTextPaint.color = Color.WHITE
        canvas.drawText("Score: ${scene.score}", cx, startY + 100f, shopTextPaint)
        shopTextPaint.color = BlockConfig.COLOR_COIN
        canvas.drawText("Earned: ${scene.runCoins} Coins", cx, startY + 160f, shopTextPaint)

        val statsY = startY + 280f; val spacing = 60f
        shopTextPaint.textSize = 40f
        shopTextPaint.color = BlockConfig.COLOR_PERFECT; canvas.drawText("Perfect: ${scene.perfectCount}", cx, statsY, shopTextPaint)
        shopTextPaint.color = BlockConfig.COLOR_GREAT; canvas.drawText("Great: ${scene.greatCount}", cx, statsY + spacing, shopTextPaint)
        shopTextPaint.color = BlockConfig.COLOR_NICE; canvas.drawText("Nice: ${scene.niceCount}", cx, statsY + spacing * 2, shopTextPaint)

        if (timeSinceDeath > BlockConfig.GAME_OVER_COOLDOWN_MS) {
            val footerH = 120f
            canvas.drawRect(0f, h - footerH - 40f, w.toFloat(), h.toFloat(), overlayPaint)

            shopTextPaint.textSize = 35f; shopTextPaint.color = Color.WHITE
            val viewText = if(isOrbit) "▼ Zoom Out" else "▼ View Tower"
            canvas.drawText("$viewText | CENTER Retry | BACK Menu", cx, h - 80f, shopTextPaint)
        }
    }
}