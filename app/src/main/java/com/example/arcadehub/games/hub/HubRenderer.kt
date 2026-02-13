package com.example.arcadehub.games.hub

import android.graphics.*
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.MathUtils
import com.example.arcadehub.managers.SceneManager
import kotlin.math.sin
import androidx.core.graphics.scale

class HubRenderer {

    private val columns = HubConfig.COLUMNS
    private val iconSize = 220f
    private val cardPadding = 22f
    private val horizontalGap = 44f
    private val verticalGap = 88f
    private val gridTop = 320f
    private val cardCorner = 26f
    private val tileSize = iconSize + (cardPadding * 2f)

    private var scrollY = 0f
    private var targetScrollY = 0f

    private val backgroundPaint = Paint()
    private val vignettePaint = Paint()

    private val topAccentPaint = Paint().apply {
        color = 0xFF4DE3C5.toInt()
        isAntiAlias = true
    }

    private val cardFillPaint = Paint().apply {
        color = 0xFF151B25.toInt()
        isAntiAlias = true
    }

    private val cardStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = 0xFF2A3548.toInt()
        isAntiAlias = true
    }

    private val selectedStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = 0xFF5AE8CD.toInt()
        isAntiAlias = true
    }

    private val selectedInnerStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xAAE9FFFB.toInt()
        isAntiAlias = true
    }

    private val bitmapPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    private val headerPaint = Paint().apply {
        color = Color.WHITE
        textSize = 74f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val subtitlePaint = Paint().apply {
        color = 0xFFC0CBD8.toInt()
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        isAntiAlias = true
    }

    private val selectedTitlePaint = Paint().apply {
        color = 0xFFE9FFFA.toInt()
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        isAntiAlias = true
    }

    private val cardTitlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 31f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        isAntiAlias = true
    }

    private val cardTitleMutedPaint = Paint().apply {
        color = 0xFF93A2B7.toInt()
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        isAntiAlias = true
    }

    private val dividerPaint = Paint().apply {
        color = 0xFF2D3847.toInt()
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val pageDotPaint = Paint().apply {
        color = 0xFF3C495E.toInt()
        isAntiAlias = true
    }

    private val pageDotActivePaint = Paint().apply {
        color = 0xFF68EDD3.toInt()
        isAntiAlias = true
    }

    private val helpBarPaint = Paint().apply {
        color = 0xDD0A0F16.toInt()
        isAntiAlias = true
    }

    private val helpTextPaint = Paint().apply {
        textSize = 29f
        color = 0xFFD2DBE7.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        isAntiAlias = true
    }

    private var time = 0f
    private val bitmapCache = HashMap<Int, Bitmap>()
    private val tempRect = RectF()

    private var shadersReady = false

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

    private fun ensureShaders() {
        if (shadersReady) return

        val width = Constants.LOGIC_WIDTH.toFloat()
        val height = Constants.LOGIC_HEIGHT.toFloat()

        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height,
            intArrayOf(
                0xFF0A1018.toInt(),
                0xFF0E141F.toInt(),
                0xFF070B12.toInt()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )

        vignettePaint.shader = RadialGradient(
            width / 2f,
            height * 0.22f,
            width * 0.9f,
            0x3327B99E,
            0x00000000,
            Shader.TileMode.CLAMP
        )

        shadersReady = true
    }

    private fun cardCenterX(column: Int): Float {
        val gridWidth = (columns * tileSize) + ((columns - 1) * horizontalGap)
        val startX = ((Constants.LOGIC_WIDTH - gridWidth) / 2f) + (tileSize / 2f)
        return startX + (column * (tileSize + horizontalGap))
    }

    fun draw(canvas: Canvas, games: List<HubGameData>, selectedIndex: Int) {
        ensureShaders()

        val width = Constants.LOGIC_WIDTH.toFloat()
        val height = Constants.LOGIC_HEIGHT.toFloat()

        canvas.drawRect(0f, 0f, width, height, backgroundPaint)
        canvas.drawRect(0f, 0f, width, height, vignettePaint)

        topAccentPaint.alpha = (112 + ((sin(time * 1.6f) + 1f) * 36f)).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, width, 6f, topAccentPaint)

        canvas.drawText("Arcade Hub", width / 2f, 112f, headerPaint)
        canvas.drawText("Pick a game and press OK", width / 2f, 158f, subtitlePaint)
        canvas.drawLine((width / 2f) - 230f, 188f, (width / 2f) + 230f, 188f, dividerPaint)

        games.getOrNull(selectedIndex)?.let {
            canvas.drawText(it.title, width / 2f, 250f, selectedTitlePaint)
        }

        val totalRows = ((games.size + columns - 1) / columns).coerceAtLeast(1)
        val rowHeight = tileSize + verticalGap
        val selectedRow = selectedIndex / columns
        val selectedY = gridTop + (selectedRow * rowHeight)
        val preferredY = Constants.LOGIC_HEIGHT * 0.58f

        val contentBottom = gridTop + ((totalRows - 1) * rowHeight) + tileSize
        val minScroll = minOf(0f, (Constants.LOGIC_HEIGHT - 180f) - contentBottom)
        val maxScroll = 90f
        targetScrollY = (preferredY - selectedY).coerceIn(minScroll, maxScroll)

        canvas.save()
        canvas.translate(0f, scrollY)

        for (i in games.indices) {
            val col = i % columns
            val row = i / columns

            val x = cardCenterX(col)
            val y = gridTop + row * rowHeight

            val cullSize = tileSize * 0.9f
            val screenY = y + scrollY
            if (screenY < -cullSize || screenY > height + cullSize) continue

            val isSelected = (i == selectedIndex)
            val pulse = if (isSelected) (sin(time * 4.6f) * 0.014f) else 0f
            val scale = (if (isSelected) 1.03f else 0.95f) + pulse

            val bitmap = bitmapCache[games[i].resId]
            val halfTile = tileSize / 2f

            canvas.save()
            canvas.translate(x, y)
            canvas.scale(scale, scale)

            tempRect.set(-halfTile, -halfTile, halfTile, halfTile)
            cardFillPaint.alpha = if (isSelected) 225 else 160
            canvas.drawRoundRect(tempRect, cardCorner, cardCorner, cardFillPaint)

            if (isSelected) {
                canvas.drawRoundRect(tempRect, cardCorner, cardCorner, selectedStrokePaint)
                tempRect.inset(8f, 8f)
                canvas.drawRoundRect(tempRect, cardCorner - 7f, cardCorner - 7f, selectedInnerStrokePaint)
                tempRect.inset(-8f, -8f)
            } else {
                canvas.drawRoundRect(tempRect, cardCorner, cardCorner, cardStrokePaint)
            }

            if (bitmap != null) {
                bitmapPaint.alpha = if (isSelected) 255 else 205
                canvas.drawBitmap(bitmap, -iconSize / 2f, -iconSize / 2f, bitmapPaint)
            }

            val labelY = halfTile + 42f
            if (isSelected) {
                canvas.drawText(games[i].title, 0f, labelY, cardTitlePaint)
            } else {
                canvas.drawText(games[i].title, 0f, labelY, cardTitleMutedPaint)
            }

            canvas.restore()
        }
        canvas.restore()

        if (totalRows > 1) {
            val dotSpacing = 26f
            val dotsWidth = (totalRows - 1) * dotSpacing
            val dotStartX = (width / 2f) - (dotsWidth / 2f)
            val dotY = height - 122f

            for (row in 0 until totalRows) {
                val dotX = dotStartX + (row * dotSpacing)
                if (row == selectedRow) {
                    canvas.drawCircle(dotX, dotY, 6.5f, pageDotActivePaint)
                } else {
                    canvas.drawCircle(dotX, dotY, 4.5f, pageDotPaint)
                }
            }
        }

        tempRect.set((width / 2f) - 470f, height - 92f, (width / 2f) + 470f, height - 26f)
        canvas.drawRoundRect(tempRect, 28f, 28f, helpBarPaint)
        canvas.drawText("D-pad Move   OK Play   Back Exit", width / 2f, height - 47f, helpTextPaint)
    }
}
