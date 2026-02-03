package com.example.arcadehub.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object GraphicsUtils {
    fun createPaint(
        color: Int,
        style: Paint.Style = Paint.Style.FILL,
        textSize: Float = 0f,
        strokeWidth: Float = 0f,
        align: Paint.Align = Paint.Align.LEFT,
        typeface: Typeface = Typeface.DEFAULT,
        alpha: Int = 255
    ): Paint {
        return Paint().apply {
            this.color = color
            this.style = style
            this.textSize = textSize
            this.strokeWidth = strokeWidth
            this.textAlign = align
            this.typeface = typeface
            this.alpha = alpha
            this.isAntiAlias = true
        }
    }

    private val overlayPaint = createPaint(Color.BLACK, alpha = 200)
    private val titlePaint = createPaint(Color.WHITE, textSize = 100f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val subTextPaint = createPaint(Color.WHITE, textSize = 40f, align = Paint.Align.CENTER)
    private val helpTextPaint = createPaint(Color.LTGRAY, textSize = 30f, align = Paint.Align.CENTER)

    fun drawOverlay(canvas: Canvas, width: Int, height: Int) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    fun drawPauseMenu(canvas: Canvas, width: Int, height: Int, scoreText: String? = null) {
        drawOverlay(canvas, width, height)
        val cx = width / 2f
        val cy = height / 2f

        // Optional background score for context
        if (scoreText != null) {
            val bgTextPaint = createPaint(Color.WHITE, textSize = 70f, align = Paint.Align.CENTER)
            canvas.drawText(scoreText, cx, 130f, bgTextPaint)
        }

        canvas.drawText("PAUSED", cx, cy - 40f, titlePaint)
        canvas.drawText("CENTER to Resume", cx, cy + 60f, subTextPaint)
        canvas.drawText("BACK to Quit", cx, cy + 130f, subTextPaint)
    }

    fun drawGameOverMenu(
        canvas: Canvas,
        width: Int,
        height: Int,
        title: String,
        scoreMsg: String,
        subMsg: String? = null,
        footerMsg: String = "CENTER to Retry | BACK to Menu"
    ) {
        val cx = width / 2f
        val cy = height / 2f

        canvas.drawText(title, cx, cy - 40f, titlePaint)
        canvas.drawText(scoreMsg, cx, cy + 50f, subTextPaint)

        var currentY = cy + 120f
        if (subMsg != null) {
            canvas.drawText(subMsg, cx, currentY, helpTextPaint)
            currentY += 50f
        }

        canvas.drawText(footerMsg, cx, currentY + 20f, helpTextPaint)
    }
}