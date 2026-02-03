package com.example.arcadehub.games.hub

import android.graphics.*
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.MathUtils
import com.example.arcadehub.managers.SceneManager
import kotlin.math.sin
import androidx.core.graphics.createBitmap

class HubRenderer {

    private val columns = HubConfig.COLUMNS

    private val iconSize = 300f
    private val horizontalGap = 60f
    private val verticalGap = 120f // Extra space for text below icon
    private val startX = 100f
    private val startY = 250f

    // Scrolling
    private var scrollY = 0f
    private var targetScrollY = 0f

    // Paints
    private val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
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

    private var time = 0f
    private val bitmapCache = HashMap<Int, Bitmap>()

    fun updateAnim(dt: Float) {
        time += dt
        // Smoothly interpolate scroll
        scrollY = MathUtils.lerp(scrollY, targetScrollY, 0.1f)
    }

    fun draw(canvas: Canvas, games: List<HubGameData>, selectedIndex: Int) {
        canvas.drawColor(Constants.COLOR_BG)

        // Draw Header
        val headerPaint = Paint(textPaint).apply {
            textSize = 80f
            color = Color.CYAN
            setShadowLayer(20f, 0f, 0f, Color.CYAN)
        }
        canvas.drawText("ARCADE HUB", Constants.LOGIC_WIDTH / 2f, 120f, headerPaint)

        // Update target scroll based on selection
        val selectedRow = selectedIndex / columns
        targetScrollY = -(selectedRow * (iconSize + verticalGap)) + 200f

        canvas.save()
        canvas.translate(0f, scrollY)

        for (i in games.indices) {
            val col = i % columns
            val row = i / columns

            val x = startX + col * (iconSize + horizontalGap) + (iconSize / 2f)
            val y = startY + row * (iconSize + verticalGap) + (iconSize / 2f)

            // Culling: Don't draw if off-screen (performance for 50+ games)
            val screenY = y + scrollY
            if (screenY < -iconSize || screenY > Constants.LOGIC_HEIGHT + iconSize) continue

            val isSelected = (i == selectedIndex)
            val pulse = if (isSelected) (sin(time * 5f) * 0.05f) else 0f
            val scale = (if (isSelected) 1.15f else 0.9f) + pulse.toFloat()

            val bitmap = getBitmap(games[i].resId)

            canvas.save()
            canvas.translate(x, y)
            canvas.scale(scale, scale)

            // Image
            paint.alpha = if (isSelected) 255 else 160
            canvas.drawBitmap(bitmap, -iconSize / 2f, -iconSize / 2f, paint)

            // Border
            if (isSelected) {
                canvas.drawRect(-iconSize/2f, -iconSize/2f, iconSize/2f, iconSize/2f, selectionBorderPaint)
            }

            // Title below icon
            textPaint.color = if (isSelected) Color.WHITE else Color.GRAY
            canvas.drawText(games[i].title, 0f, (iconSize / 2f) + 45f, textPaint)

            canvas.restore()
        }
        canvas.restore()

        // Help bar at the very bottom (Fixed position)
        canvas.drawRect(0f, 1000f, 1920f, 1080f, Paint().apply { color = Color.BLACK; alpha = 150 })
        val helpPaint = Paint(textPaint).apply { textSize = 28f; color = Color.LTGRAY }
        canvas.drawText("D-PAD: Navigate  |  CENTER: Play  |  BACK: Exit", Constants.LOGIC_WIDTH/2f, 1050f, helpPaint)
    }

    private fun getBitmap(resId: Int): Bitmap {
        bitmapCache[resId]?.let { return it }
        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = BitmapFactory.decodeResource(SceneManager.resources, resId, options)
            ?: createBitmap(iconSize.toInt(), iconSize.toInt())
        bitmapCache[resId] = bitmap
        return bitmap
    }
}