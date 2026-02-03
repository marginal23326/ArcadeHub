package com.example.arcadehub.games.hub

import android.graphics.*
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.MathUtils
import com.example.arcadehub.managers.SceneManager
import kotlin.math.sin
import androidx.core.graphics.scale

class HubRenderer {

    private val columns = HubConfig.COLUMNS
    private val iconSize = 300f
    private val horizontalGap = 60f
    private val verticalGap = 120f
    private val startX = 100f
    private val startY = 250f

    private var scrollY = 0f
    private var targetScrollY = 0f

    // Move all Paints to properties to avoid allocation in draw()
    private val bitmapPaint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 34f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val selectionBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.CYAN
        isAntiAlias = true
        setShadowLayer(15f, 0f, 0f, Color.CYAN)
    }

    private val headerPaint = Paint().apply {
        textSize = 80f
        color = Color.CYAN
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        setShadowLayer(20f, 0f, 0f, Color.CYAN)
    }

    private val helpBarPaint = Paint().apply { color = Color.BLACK; alpha = 150 }
    private val helpTextPaint = Paint().apply {
        textSize = 28f
        color = Color.LTGRAY
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private var time = 0f
    private val bitmapCache = HashMap<Int, Bitmap>()

    fun preloadBitmaps(games: List<HubGameData>) {
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        for (game in games) {
            if (!bitmapCache.containsKey(game.resId)) {
                val raw = BitmapFactory.decodeResource(SceneManager.resources, game.resId, options)
                if (raw != null) {
                    val scaled = raw.scale(iconSize.toInt(), iconSize.toInt())
                    bitmapCache[game.resId] = scaled
                    if (raw != scaled) raw.recycle()
                }
            }
        }
    }

    fun updateAnim(dt: Float) {
        time += dt
        scrollY = MathUtils.lerp(scrollY, targetScrollY, 0.15f)
    }

    fun draw(canvas: Canvas, games: List<HubGameData>, selectedIndex: Int) {
        canvas.drawColor(Constants.COLOR_BG)

        // Draw Header using pre-allocated Paint
        canvas.drawText("ARCADE HUB", Constants.LOGIC_WIDTH / 2f, 120f, headerPaint)

        val selectedRow = selectedIndex / columns
        targetScrollY = -(selectedRow * (iconSize + verticalGap)) + 200f

        canvas.save()
        canvas.translate(0f, scrollY)

        for (i in games.indices) {
            val col = i % columns
            val row = i / columns

            val x = startX + col * (iconSize + horizontalGap) + (iconSize / 2f)
            val y = startY + row * (iconSize + verticalGap) + (iconSize / 2f)

            // Culling (Don't draw off-screen icons)
            val screenY = y + scrollY
            if (screenY < -iconSize || screenY > Constants.LOGIC_HEIGHT + iconSize) continue

            val isSelected = (i == selectedIndex)
            val pulse = if (isSelected) (sin(time * 5f) * 0.05f) else 0f
            val scale = (if (isSelected) 1.15f else 0.9f) + pulse

            // Safe retrieval from cache (already scaled and decoded)
            val bitmap = bitmapCache[games[i].resId]

            canvas.save()
            canvas.translate(x, y)
            canvas.scale(scale, scale)

            if (bitmap != null) {
                bitmapPaint.alpha = if (isSelected) 255 else 160
                canvas.drawBitmap(bitmap, -iconSize / 2f, -iconSize / 2f, bitmapPaint)
            }

            if (isSelected) {
                canvas.drawRect(-iconSize/2f, -iconSize/2f, iconSize/2f, iconSize/2f, selectionBorderPaint)
            }

            textPaint.color = if (isSelected) Color.WHITE else Color.GRAY
            canvas.drawText(games[i].title, 0f, (iconSize / 2f) + 45f, textPaint)

            canvas.restore()
        }
        canvas.restore()

        // Help bar
        canvas.drawRect(0f, 1000f, 1920f, 1080f, helpBarPaint)
        canvas.drawText("D-PAD: Navigate  |  CENTER: Play  |  BACK: Exit", Constants.LOGIC_WIDTH/2f, 1050f, helpTextPaint)
    }
}