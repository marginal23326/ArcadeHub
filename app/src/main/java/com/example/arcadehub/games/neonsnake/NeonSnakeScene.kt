package com.example.arcadehub.games.neonsnake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.arcadehub.core.BaseGameScene
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.GraphicsUtils
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.managers.SoundManager
import kotlin.math.max
import kotlin.math.min
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

    override val highScoreKey: String = "SNAKE_HIGHSCORE"
    private var finalScore = 0

    // Paints
    private val menuTitlePaint = GraphicsUtils.createPaint(Color.CYAN, textSize = 70f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val menuValPaint = GraphicsUtils.createPaint(Color.YELLOW, textSize = 80f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val menuLabelPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 40f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val statsPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 35f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

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
        particles.clear()
    }

    override fun update(dt: Float) {
        // Update Shake
        if (shakeTimer > 0) {
            shakeTimer -= dt
            if (shakeTimer < 0) shakeTimer = 0f
        }

        // Update Particles
        particles.update(dt)

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
        canvas.drawColor(SnakeConfig.COLOR_BG)
        val cx = Constants.LOGIC_WIDTH / 2f
        val cy = Constants.LOGIC_HEIGHT / 2f

        // Compact spacing to fit 3 rows
        val startY = cy - 200f
        val rowH = 140f

        canvas.drawText("SNAKE SETUP", cx, startY, menuTitlePaint)

        // --- ROW 0: Level ---
        drawMenuRow(canvas, 0, "DIFFICULTY", "LEVEL $selectedLevel", cx, startY + 80f)

        // --- ROW 1: Mode ---
        val modeText = if (isRobotVsRobotMode) "ROBOT BATTLE" else "PLAYER vs ROBOT"
        drawMenuRow(canvas, 1, "GAME MODE", modeText, cx, startY + 80f + rowH)

        // --- ROW 2: Map ---
        drawMenuRow(canvas, 2, "ARENA MAP", selectedMap.name, cx, startY + 80f + rowH * 2)

        // Stats
        val wins = SnakeStats.getWins(selectedLevel)
        val losses = SnakeStats.getLosses(selectedLevel)
        statsPaint.color = if (wins >= losses) Color.GREEN else Color.RED
        canvas.drawText("WINS: $wins  |  LOSSES: $losses", cx, cy + 340f, statsPaint)

        // Controls
        GraphicsUtils.createPaint(Color.DKGRAY, textSize = 28f, align = Paint.Align.CENTER).also {
            canvas.drawText("UP/DOWN Select | LEFT/RIGHT Change | CENTER Play", cx, cy + 380f, it)
        }
    }

    private fun drawMenuRow(c: Canvas, index: Int, label: String, value: String, x: Float, y: Float) {
        val isActive = (menuRowIndex == index)
        menuLabelPaint.color = if (isActive) Color.CYAN else Color.GRAY
        menuValPaint.color = if (isActive) Color.YELLOW else Color.DKGRAY

        c.drawText(label, x, y, menuLabelPaint)
        val showL = if (isActive) "< " else "  "
        val showR = if (isActive) " >" else "  "
        c.drawText("$showL$value$showR", x, y + 70f, menuValPaint)
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
