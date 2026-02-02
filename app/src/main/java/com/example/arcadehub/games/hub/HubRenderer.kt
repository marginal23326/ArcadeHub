package com.example.arcadehub.games.hub

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.arcadehub.core.Constants
import kotlin.math.sin
import androidx.core.graphics.toColorInt

class HubRenderer {

    // Center coordinates based on our Logical Resolution (1920x1080)
    private val centerX = Constants.LOGIC_WIDTH / 2f
    private val centerY = Constants.LOGIC_HEIGHT / 2f

    // Paints
    private val titlePaint = Paint().apply {
        color = Color.CYAN
        textSize = 120f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        // Add a glow effect
        setShadowLayer(20f, 0f, 0f, Color.CYAN)
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val selectionPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.YELLOW
        isAntiAlias = true
    }

    private val selectionFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = "#33FFFF00".toColorInt() // Transparent Yellow
    }

    private val helpPaint = Paint().apply {
        color = Color.GRAY
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Animation state
    private var time = 0f
    private val selectionRect = RectF()

    fun updateAnim(dt: Float) {
        time += dt
    }

    fun draw(canvas: Canvas, items: List<String>, selectedIndex: Int) {
        canvas.drawColor(Constants.COLOR_BG)

        // Draw Title
        canvas.drawText("ARCADE HUB", centerX, 200f, titlePaint)

        // Draw Menu Items
        val startY = 450f
        val gap = 140f
        val boxWidth = 600f
        val boxHeight = 100f

        for (i in items.indices) {
            val y = startY + (i * gap)

            if (i == selectedIndex) {
                // Pulse effect on selection
                val pulse = (sin(time * 5f) + 1f) / 2f // 0.0 to 1.0
                val alpha = 150 + (pulse * 105).toInt()
                selectionPaint.alpha = alpha

                // Draw Selection Box
                selectionRect.set(centerX - boxWidth/2, y - boxHeight/2 - 20, centerX + boxWidth/2, y + boxHeight/2 - 20)
                canvas.drawRoundRect(selectionRect, 20f, 20f, selectionFillPaint)
                canvas.drawRoundRect(selectionRect, 20f, 20f, selectionPaint)

                textPaint.color = Color.YELLOW
                textPaint.textSize = 70f // Slight zoom
            } else {
                textPaint.color = Color.LTGRAY
                textPaint.textSize = 60f
            }

            canvas.drawText(items[i], centerX, y, textPaint)
        }

        // Footer
        canvas.drawText("D-PAD: Navigate  |  CENTER: Select  |  BACK: Exit", centerX, Constants.LOGIC_HEIGHT - 60f, helpPaint)
    }
}