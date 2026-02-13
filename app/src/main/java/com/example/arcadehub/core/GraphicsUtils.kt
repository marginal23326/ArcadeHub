package com.example.arcadehub.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

object GraphicsUtils {
    data class MenuTheme(
        val overlayColor: Int,
        val panelColor: Int,
        val borderColor: Int,
        val titleColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int,
        val accentTextColor: Int,
        val panelHorizontalInsetRatio: Float = 0.15f,
        val panelTopInsetRatio: Float = 0.22f,
        val panelBottomInsetRatio: Float = 0.22f,
        val panelCornerRadius: Float = 34f
    ) {
        companion object {
            val MODERN_DARK = MenuTheme(
                overlayColor = 0x48050B14.toInt(),
                panelColor = 0x660B1624.toInt(),
                borderColor = 0xFF284760.toInt(),
                titleColor = 0xFFF2FAFF.toInt(),
                primaryTextColor = 0xFFF2FAFF.toInt(),
                secondaryTextColor = 0xFFC5D8E8.toInt(),
                accentTextColor = 0xFFE7F5FF.toInt()
            )

            val ORBIT = MenuTheme(
                overlayColor = 0x48050B14.toInt(),
                panelColor = 0x660B1624.toInt(),
                borderColor = 0xFF2C587A.toInt(),
                titleColor = 0xFFF2FAFF.toInt(),
                primaryTextColor = 0xFFF2FAFF.toInt(),
                secondaryTextColor = 0xFFCBE3F2.toInt(),
                accentTextColor = 0xFFBCEBFF.toInt()
            )
        }
    }

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

    private val panelRect = RectF()
    private val overlayPaint = createPaint(Color.BLACK, alpha = 200)
    private val panelPaint = createPaint(Color.BLACK, alpha = 220)
    private val panelStrokePaint = createPaint(Color.DKGRAY, Paint.Style.STROKE, strokeWidth = 3f)

    private val titlePaint = createPaint(Color.WHITE, textSize = 74f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val primaryTextPaint = createPaint(Color.WHITE, textSize = 38f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val secondaryTextPaint = createPaint(Color.LTGRAY, textSize = 30f, align = Paint.Align.CENTER)
    private val accentTextPaint = createPaint(Color.WHITE, textSize = 56f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

    private fun applyTheme(theme: MenuTheme) {
        overlayPaint.color = theme.overlayColor
        overlayPaint.alpha = (theme.overlayColor ushr 24) and 0xFF
        panelPaint.color = theme.panelColor
        panelPaint.alpha = (theme.panelColor ushr 24) and 0xFF
        panelStrokePaint.color = theme.borderColor
        titlePaint.color = theme.titleColor
        primaryTextPaint.color = theme.primaryTextColor
        secondaryTextPaint.color = theme.secondaryTextColor
        accentTextPaint.color = theme.accentTextColor
    }

    fun drawOverlay(canvas: Canvas, width: Int, height: Int) {
        drawOverlay(canvas, width, height, MenuTheme.MODERN_DARK)
    }

    fun drawOverlay(canvas: Canvas, width: Int, height: Int, theme: MenuTheme) {
        applyTheme(theme)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    private fun drawPanel(canvas: Canvas, width: Int, height: Int, theme: MenuTheme) {
        applyTheme(theme)
        drawOverlay(canvas, width, height, theme)
        panelRect.set(
            width * theme.panelHorizontalInsetRatio,
            height * theme.panelTopInsetRatio,
            width * (1f - theme.panelHorizontalInsetRatio),
            height * (1f - theme.panelBottomInsetRatio)
        )
        canvas.drawRoundRect(panelRect, theme.panelCornerRadius, theme.panelCornerRadius, panelPaint)
        canvas.drawRoundRect(panelRect, theme.panelCornerRadius, theme.panelCornerRadius, panelStrokePaint)
    }

    fun drawStartMenu(
        canvas: Canvas,
        width: Int,
        height: Int,
        title: String,
        subtitle: String? = null,
        primaryAction: String = "CENTER  Start",
        secondaryAction: String = "BACK  Return to Hub",
        hint: String? = null,
        theme: MenuTheme = MenuTheme.MODERN_DARK
    ) {
        drawPanel(canvas, width, height, theme)

        val cx = width / 2f
        var y = panelRect.top + 96f

        canvas.drawText(title, cx, y, titlePaint)
        if (subtitle != null) {
            y += 58f
            canvas.drawText(subtitle, cx, y, secondaryTextPaint)
        }
        y += 96f
        canvas.drawText(primaryAction, cx, y, primaryTextPaint)
        y += 52f
        canvas.drawText(secondaryAction, cx, y, secondaryTextPaint)
        if (hint != null) {
            y += 62f
            canvas.drawText(hint, cx, y, secondaryTextPaint)
        }
    }

    fun drawPauseMenu(
        canvas: Canvas,
        width: Int,
        height: Int,
        scoreText: String? = null,
        title: String = "PAUSED",
        primaryAction: String = "CENTER  Resume",
        secondaryAction: String = "BACK  Quit to Hub",
        theme: MenuTheme = MenuTheme.MODERN_DARK
    ) {
        drawPanel(canvas, width, height, theme)
        val cx = width / 2f
        var y = panelRect.top + 120f

        canvas.drawText(title, cx, y, titlePaint)
        y += 88f

        if (scoreText != null) {
            canvas.drawText(scoreText, cx, y, accentTextPaint)
            y += 82f
        }

        canvas.drawText(primaryAction, cx, y, primaryTextPaint)
        y += 52f
        canvas.drawText(secondaryAction, cx, y, secondaryTextPaint)
    }

    fun drawGameOverMenu(
        canvas: Canvas,
        width: Int,
        height: Int,
        title: String,
        scoreMsg: String,
        subMsg: String? = null,
        footerMsg: String = "CENTER to Retry | BACK to Menu",
        theme: MenuTheme = MenuTheme.MODERN_DARK
    ) {
        drawPanel(canvas, width, height, theme)
        val cx = width / 2f
        var y = panelRect.top + 112f

        canvas.drawText(title, cx, y, titlePaint)
        y += 98f
        canvas.drawText(scoreMsg, cx, y, accentTextPaint)

        if (subMsg != null) {
            y += 62f
            canvas.drawText(subMsg, cx, y, secondaryTextPaint)
        }

        y += 74f
        canvas.drawText(footerMsg, cx, y, secondaryTextPaint)
    }
}
