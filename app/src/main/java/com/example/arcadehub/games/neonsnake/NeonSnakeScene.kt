package com.example.arcadehub.games.neonsnake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.arcadehub.core.BaseGameScene
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.GraphicsUtils
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.games.neonsnake.ai.AiEngine
import com.example.arcadehub.managers.SoundManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class NeonSnakeScene : BaseGameScene() {

    private val physics = SnakePhysics()
    private val renderer = SnakeRenderer()
    private val particles = SnakeParticles()

    private enum class State { MENU, PLAYING, REPLAY }
    private var currentState = State.MENU

    // Menu Selection
    // 0 = Level, 1 = Mode, 2 = Map
    private var menuRowIndex = 0
    private var selectedLevel = 3
    private var isRobotVsRobotMode = false
    private var selectedMap = SnakeMapGenerator.MapType.EMPTY

    // Screen Shake
    private var shakeTimer = 0f
    private var shakeMagnitude = 0f

    // Replay
    private var replayData: List<GameSnapshot> = emptyList()
    private var replayIndex = 0
    private var replayTimer = 0f
    private val REPLAY_SPEED = 0.85f
    private var menuPulseTime = 0f

    override val highScoreKey: String = "SNAKE_HIGHSCORE"
    private var finalScore = 0

    // Menu visuals
    private val menuBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val menuShadePaint = GraphicsUtils.createPaint(Color.BLACK, alpha = 55)
    private val menuPanelPaint = GraphicsUtils.createPaint(Color.argb(190, 10, 16, 24))
    private val menuPanelStrokePaint = GraphicsUtils.createPaint(Color.argb(130, 84, 108, 134), Paint.Style.STROKE, strokeWidth = 2f)

    private val menuTitlePaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 82f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val menuSubTitlePaint = GraphicsUtils.createPaint(Color.argb(235, 160, 178, 197), textSize = 34f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

    private val menuRowPaint = GraphicsUtils.createPaint(Color.argb(170, 17, 26, 36))
    private val menuRowActivePaint = GraphicsUtils.createPaint(Color.argb(205, 18, 40, 54))
    private val menuRowStrokePaint = GraphicsUtils.createPaint(Color.argb(95, 96, 120, 146), Paint.Style.STROKE, strokeWidth = 2f)
    private val menuRowActiveStrokePaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_P1_HEAD, Paint.Style.STROKE, strokeWidth = 3f)

    private val menuLabelPaint = GraphicsUtils.createPaint(Color.argb(220, 152, 174, 199), textSize = 28f, align = Paint.Align.LEFT, typeface = Typeface.DEFAULT_BOLD)
    private val menuValPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 46f, align = Paint.Align.RIGHT, typeface = Typeface.DEFAULT_BOLD)
    private val menuArrowPaint = GraphicsUtils.createPaint(SnakeConfig.COLOR_P1_HEAD, textSize = 50f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

    private val statsCardPaint = GraphicsUtils.createPaint(Color.argb(170, 12, 22, 31))
    private val statsCardStrokePaint = GraphicsUtils.createPaint(Color.argb(120, 84, 108, 134), Paint.Style.STROKE, strokeWidth = 2f)
    private val statsTextPaint = GraphicsUtils.createPaint(Color.argb(240, 212, 222, 232), textSize = 32f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val controlsPaint = GraphicsUtils.createPaint(Color.argb(200, 146, 161, 179), textSize = 28f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

    private val panelRect = RectF()
    private val rowRect = RectF()
    private val statsRect = RectF()
    private var menuBgGradient: LinearGradient? = null
    private var menuBgWidth = -1
    private var menuBgHeight = -1

    init {
        // Hook up Physics events to Visuals
        physics.onFoodEaten = { x, y, color ->
            val cx = (x * renderer.getCellSize()) + (renderer.getCellSize() / 2)
            val cy = (y * renderer.getCellSize()) + (renderer.getCellSize() / 2)
            particles.spawnExplosion(cx, cy, color, 15)
        }

        physics.onCollision = { x, y ->
            val cx = (x * renderer.getCellSize()) + (renderer.getCellSize() / 2)
            val cy = (y * renderer.getCellSize()) + (renderer.getCellSize() / 2)
            particles.spawnExplosion(cx, cy, Color.WHITE, 30) // Big explosion
            triggerShake(0.4f, 20f)
        }
    }

    private fun triggerShake(duration: Float, mag: Float) {
        shakeTimer = duration
        shakeMagnitude = mag
    }

    override fun resetGame() {
        currentState = State.MENU
        isGameStarted = false
        isGameOver = false
        isPaused = false
        score = 0
        menuRowIndex = 0
        menuPulseTime = 0f
        particles.clear()
    }

    override fun exit() {
        AiEngine.releaseResources()
    }

    override fun update(dt: Float) {
        // Update Shake
        if (shakeTimer > 0) {
            shakeTimer -= dt
            if (shakeTimer < 0) shakeTimer = 0f
        }

        // Update Particles
        particles.update(dt)
        if (currentState == State.MENU) {
            menuPulseTime += dt
        }

        when (currentState) {
            State.PLAYING -> {
                if (isPaused) return
                physics.update(dt)
                score = physics.player.score

                if (physics.isGameOver && !isGameOver) {
                    startReplayMode()
                }
            }
            State.REPLAY -> {
                replayTimer += dt
                if (replayTimer >= REPLAY_SPEED) {
                    replayTimer = 0f
                    replayIndex = (replayIndex + 1) % maxOf(1, replayData.size)
                }
            }
            else -> {}
        }
    }

    private fun startReplayMode() {
        this.isGameOver = true
        SoundManager.playGameOver()
        if (!isRobotVsRobotMode) {
            checkNewHighScore()

            when (physics.gameOutcome) {
                SnakeGameOutcome.P1_WIN -> SnakeStats.recordWin(selectedLevel)
                SnakeGameOutcome.AI_WIN -> SnakeStats.recordLoss(selectedLevel)
                else -> {}
            }
        }
        this.finalScore = physics.player.score

        replayData = physics.getReplayHistory()
        replayIndex = 0
        currentState = State.REPLAY
    }

    override fun draw(canvas: Canvas) {
        // Apply Screen Shake
        val saveCount = canvas.save()
        if (shakeTimer > 0) {
            val dx = (Random.nextFloat() - 0.5f) * shakeMagnitude
            val dy = (Random.nextFloat() - 0.5f) * shakeMagnitude
            canvas.translate(dx, dy)
        }

        when (currentState) {
            State.MENU -> drawLevelMenu(canvas)
            State.PLAYING -> {
                renderer.draw(canvas, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, highScore, isRobotVsRobotMode)
                particles.draw(canvas) // Draw particles on top
            }
            State.REPLAY -> {
                if (replayData.isNotEmpty()) {
                    renderer.drawReplayFrame(canvas, replayData[replayIndex], Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, physics, finalScore)
                    particles.draw(canvas)
                }
            }
        }

        canvas.restoreToCount(saveCount)

        if (isPaused) {
            GraphicsUtils.drawPauseMenu(canvas, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        }
    }

    private fun drawLevelMenu(canvas: Canvas) {
        val width = Constants.LOGIC_WIDTH
        val height = Constants.LOGIC_HEIGHT
        ensureMenuBackground(width, height)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), menuBgPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), 140f, menuShadePaint)
        canvas.drawRect(0f, height - 180f, width.toFloat(), height.toFloat(), menuShadePaint)

        val cx = width / 2f
        val panelWidth = (width * 0.78f).coerceAtMost(1350f)
        val panelHeight = (height * 0.74f).coerceAtMost(820f)
        val panelLeft = cx - panelWidth / 2f
        val panelTop = height * 0.12f
        val panelRight = panelLeft + panelWidth
        val panelBottom = panelTop + panelHeight
        panelRect.set(panelLeft, panelTop, panelRight, panelBottom)
        canvas.drawRoundRect(panelRect, 42f, 42f, menuPanelPaint)
        canvas.drawRoundRect(panelRect, 42f, 42f, menuPanelStrokePaint)

        canvas.drawText("NEON SNAKE", cx, panelTop + 90f, menuTitlePaint)
        canvas.drawText("Choose settings and launch", cx, panelTop + 136f, menuSubTitlePaint)

        val rowLeft = panelLeft + 42f
        val rowRight = panelRight - 42f
        val rowHeight = 122f
        val rowGap = 22f
        val firstRowTop = panelTop + 178f

        drawMenuRow(canvas, 0, "Difficulty", "Level $selectedLevel", rowLeft, firstRowTop, rowRight, firstRowTop + rowHeight)

        val modeText = if (isRobotVsRobotMode) "Bot vs Bot" else "Player vs Bot"
        val secondTop = firstRowTop + rowHeight + rowGap
        drawMenuRow(canvas, 1, "Game Mode", modeText, rowLeft, secondTop, rowRight, secondTop + rowHeight)

        val thirdTop = secondTop + rowHeight + rowGap
        drawMenuRow(canvas, 2, "Arena Map", formatMapName(selectedMap), rowLeft, thirdTop, rowRight, thirdTop + rowHeight)

        val wins = SnakeStats.getWins(selectedLevel)
        val losses = SnakeStats.getLosses(selectedLevel)
        val statsTop = panelBottom - 130f
        statsRect.set(rowLeft, statsTop, rowRight, statsTop + 76f)
        canvas.drawRoundRect(statsRect, 20f, 20f, statsCardPaint)
        canvas.drawRoundRect(statsRect, 20f, 20f, statsCardStrokePaint)

        statsTextPaint.color = when {
            wins > losses -> Color.rgb(106, 229, 166)
            losses > wins -> Color.rgb(255, 133, 133)
            else -> Color.argb(240, 212, 222, 232)
        }
        canvas.drawText("Level $selectedLevel  |  Wins $wins  |  Losses $losses  |  Best $highScore", cx, statsRect.centerY() + 11f, statsTextPaint)

        canvas.drawText(
            "UP/DOWN: Select   LEFT/RIGHT: Change   CENTER: Play   BACK: Hub",
            cx,
            height - 42f,
            controlsPaint
        )
    }

    private fun ensureMenuBackground(width: Int, height: Int) {
        if (menuBgWidth == width && menuBgHeight == height && menuBgGradient != null) return
        menuBgWidth = width
        menuBgHeight = height
        menuBgGradient = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            Color.rgb(6, 12, 20),
            Color.rgb(5, 7, 11),
            Shader.TileMode.CLAMP
        )
        menuBgPaint.shader = menuBgGradient
    }

    private fun formatMapName(mapType: SnakeMapGenerator.MapType): String {
        return when (mapType) {
            SnakeMapGenerator.MapType.EMPTY -> "Empty"
            SnakeMapGenerator.MapType.PILLARS -> "Pillars"
            SnakeMapGenerator.MapType.ROOMS -> "Rooms"
            SnakeMapGenerator.MapType.CENTER_BOX -> "Center Box"
        }
    }

    private fun drawMenuRow(
        canvas: Canvas,
        index: Int,
        label: String,
        value: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val isActive = menuRowIndex == index
        val pulse = ((sin(menuPulseTime * 4.3f) + 1f) * 0.5f)

        rowRect.set(left, top, right, bottom)
        if (isActive) {
            menuRowActivePaint.alpha = (190 + (pulse * 30f).toInt()).coerceIn(0, 255)
            menuRowActiveStrokePaint.alpha = (150 + (pulse * 105f).toInt()).coerceIn(0, 255)
            canvas.drawRoundRect(rowRect, 24f, 24f, menuRowActivePaint)
            canvas.drawRoundRect(rowRect, 24f, 24f, menuRowActiveStrokePaint)
        } else {
            canvas.drawRoundRect(rowRect, 24f, 24f, menuRowPaint)
            canvas.drawRoundRect(rowRect, 24f, 24f, menuRowStrokePaint)
        }

        menuLabelPaint.color = if (isActive) Color.WHITE else Color.argb(220, 152, 174, 199)
        menuValPaint.color = if (isActive) Color.WHITE else Color.argb(230, 196, 208, 222)

        val contentLeft = left + 34f
        val contentRight = right - 34f
        canvas.drawText(label, contentLeft, top + 42f, menuLabelPaint)
        canvas.drawText(value, contentRight, top + 93f, menuValPaint)

        if (isActive) {
            menuArrowPaint.alpha = (170 + (pulse * 85f).toInt()).coerceIn(0, 255)
            canvas.drawText("<", left + 18f, top + 92f, menuArrowPaint)
            canvas.drawText(">", right - 18f, top + 92f, menuArrowPaint)
        }
    }

    override fun onInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

        if (currentState == State.MENU) {
            if (action == InputAction.BACK) quitToHub()
            else handleMenuInput(action)
            return
        }

        if (isPaused) {
            if (action == InputAction.SELECT) isPaused = false
            else if (action == InputAction.BACK) resetGame()
            return
        }

        if (currentState == State.REPLAY) {
            handleGameOverInput(action)
            return
        }

        if (currentState == State.PLAYING) {
            if (action == InputAction.BACK) pauseGame()
            else if (isRobotVsRobotMode) handleRobotSpeedInput(action)
            else handleGameInput(action)
        }
    }


    private fun handleMenuInput(action: InputAction) {
        when (action) {
            InputAction.UP -> { menuRowIndex = max(0, menuRowIndex - 1); SoundManager.playSelect() }
            InputAction.DOWN -> { menuRowIndex = min(2, menuRowIndex + 1); SoundManager.playSelect() }
            InputAction.LEFT -> changeSetting(-1)
            InputAction.RIGHT -> changeSetting(1)
            InputAction.SELECT -> startGame()
            else -> {}
        }
    }

    private fun changeSetting(dir: Int) {
        SoundManager.playSelect()
        when (menuRowIndex) {
            0 -> selectedLevel = (selectedLevel + dir).coerceIn(1, 7)
            1 -> isRobotVsRobotMode = !isRobotVsRobotMode
            2 -> {
                val ord = selectedMap.ordinal + dir
                val vals = SnakeMapGenerator.MapType.entries.toTypedArray()
                selectedMap = vals[(ord + vals.size) % vals.size]
            }
        }
    }

    private fun handleRobotSpeedInput(action: InputAction) {
        val STEP = 0.05f
        if (action == InputAction.RIGHT) physics.speedDelay = max(0f, physics.speedDelay - STEP)
        if (action == InputAction.LEFT) physics.speedDelay = min(SnakeConfig.GAME_SPEED_SECONDS, physics.speedDelay + STEP)
    }

    private fun startGame() {
        physics.setDifficultyLevel(selectedLevel)
        physics.isAutoPilot = isRobotVsRobotMode
        physics.currentMapType = selectedMap
        physics.reset()

        isGameOver = false
        currentState = State.PLAYING
        isGameStarted = true
        SoundManager.playPerfect(1)

        renderer.updateDimensions(Constants.LOGIC_WIDTH)
    }

    override fun handleGameInput(action: InputAction, isDown: Boolean) { } // Unused interface method

    private fun handleGameInput(action: InputAction) {
        val dir = when (action) {
            InputAction.UP -> GridDir.UP
            InputAction.DOWN -> GridDir.DOWN
            InputAction.LEFT -> GridDir.LEFT
            InputAction.RIGHT -> GridDir.RIGHT
            else -> null
        }
        dir?.let { physics.processInput(it) }
    }

    override fun handleGameOverInput(action: InputAction) {
        when (action) {
            InputAction.SELECT -> startGame()
            InputAction.BACK -> { resetGame(); SoundManager.playSelect() }
            else -> {}
        }
    }
}
