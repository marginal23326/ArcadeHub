package com.example.arcadehub.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.arcadehub.managers.SceneManager

class CoreGameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    @Volatile private var isRunning = false
    private val surfaceHolder: SurfaceHolder = holder

    // Use GameLoop for delta-time calculation
    private val gameLoop = GameLoop()

    // Frame Timing variables for FPS display
    private var frames = 0
    private var lastFpsTime = 0L

    // CACHED STRING for FPS
    private val fpsBuilder = StringBuilder("FPS: 60")

    // Pre-calculated Scaling Factors (Optimization)
    private var scaleX = 1f
    private var scaleY = 1f

    private val fpsPaint = Paint().apply {
        color = Color.GREEN
        textSize = 30f
        typeface = Typeface.MONOSPACE
        isAntiAlias = false
    }

    init {
        holder.addCallback(this)

        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        val w = (screenW * Constants.RESOLUTION_SCALE).toInt().coerceAtLeast(1)
        val h = (screenH * Constants.RESOLUTION_SCALE).toInt().coerceAtLeast(1)

        // Calculate scale immediately
        scaleX = w.toFloat() / Constants.LOGIC_WIDTH.toFloat()
        scaleY = h.toFloat() / Constants.LOGIC_HEIGHT.toFloat()

        holder.setFixedSize(w, h)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Recalculate scale if surface size changes unexpectedly
        scaleX = width.toFloat() / Constants.LOGIC_WIDTH.toFloat()
        scaleY = height.toFloat() / Constants.LOGIC_HEIGHT.toFloat()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stop()
    }

    private fun start() {
        if (isRunning) return
        isRunning = true
        gameLoop.reset()               // initialize GameLoop's timing
        lastFpsTime = System.currentTimeMillis()
        thread = Thread(this)
        thread?.priority = Thread.MAX_PRIORITY // Give Game Loop CPU priority
        thread?.start()
    }

    private fun stop() {
        isRunning = false
        try { thread?.join() } catch (_: InterruptedException) { }
        thread = null
    }

    override fun run() {
        while (isRunning) {
            // 1. Calculate dt using GameLoop
            val dt = gameLoop.calculateDeltaTime()

            // 2. Update Logic
            SceneManager.update(dt)

            // 3. Render
            if (surfaceHolder.surface.isValid) {
                val canvas: Canvas? = surfaceHolder.lockHardwareCanvas()

                if (canvas != null) {
                    try {
                        canvas.save()
                        canvas.scale(scaleX, scaleY)

                        if (SceneManager.activeScene != null) {
                            SceneManager.draw(canvas)
                        } else {
                            canvas.drawColor(Color.BLACK)
                        }

                        // FPS
                        canvas.drawText(fpsBuilder, 0, fpsBuilder.length, 20f, 40f, fpsPaint)

                        canvas.restore()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    }
                }
            }

            // 4. FPS Calculation
            frames++
            if (System.currentTimeMillis() - lastFpsTime >= 1000) {
                fpsBuilder.setLength(0)
                fpsBuilder.append("FPS: ").append(frames)
                frames = 0
                lastFpsTime = System.currentTimeMillis()
            }
        }
    }
}
