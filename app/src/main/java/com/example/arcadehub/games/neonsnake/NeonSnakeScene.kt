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

class NeonSnakeScene : BaseGameScene() {

    private val physics = SnakePhysics()
    private val renderer = SnakeRenderer()

    // 1. Updated State Enum
    private enum class State { MENU, PLAYING, REPLAY }

    private var currentState = State.MENU

    // 2. New Logic: Level 1 to 5
    private var selectedLevel = 3

    private var replayData: List<GameSnapshot> = emptyList()
    private var replayIndex = 0
    private var replayTimer = 0f
    private val REPLAY_SPEED = 0.85f

    override val highScoreKey: String = "SNAKE_HIGHSCORE"
    private var finalScore = 0

    // UI Paints
    private val menuTitlePaint = GraphicsUtils.createPaint(Color.CYAN, textSize = 70f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val menuValPaint = GraphicsUtils.createPaint(Color.YELLOW, textSize = 90f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val menuSubPaint = GraphicsUtils.createPaint(Color.LTGRAY, textSize = 35f, align = Paint.Align.CENTER)
    private val statsPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 35f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

    override fun resetGame() {
        currentState = State.MENU
        isGameStarted = false
        isGameOver = false
        isPaused = false
        score = 0
        // Don't reset selectedLevel, keep user preference
    }

    override fun update(dt: Float) {
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
        checkNewHighScore()
        this.finalScore = physics.player.score

        if (physics.gameOverReason == "YOU WON") {
            SnakeStats.recordWin(selectedLevel)
        } else if (physics.gameOverReason == "ROBOT WON") {
            SnakeStats.recordLoss(selectedLevel)
        }

        replayData = physics.getReplayHistory()
        replayIndex = 0
        currentState = State.REPLAY
    }

    override fun draw(canvas: Canvas) {
        when (currentState) {
            State.MENU -> drawLevelMenu(canvas)
            State.PLAYING -> {
                renderer.draw(canvas, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, highScore)
            }
            State.REPLAY -> {
                if (replayData.isNotEmpty()) {
                    renderer.drawReplayFrame(
                        canvas,
                        replayData[replayIndex],
                        Constants.LOGIC_WIDTH,
                        Constants.LOGIC_HEIGHT,
                        physics,
                        finalScore
                    )
                }
            }
        }

        if (isPaused) {
            GraphicsUtils.drawPauseMenu(canvas, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        }
    }

    // 3. New Menu Drawing Logic
    private fun drawLevelMenu(canvas: Canvas) {
        canvas.drawColor(SnakeConfig.COLOR_BG)
        val cx = Constants.LOGIC_WIDTH / 2f
        val cy = Constants.LOGIC_HEIGHT / 2f

        canvas.drawText("ROBOT DIFFICULTY", cx, cy - 120f, menuTitlePaint)

        // Draw Arrows and Value
        val showLeft = if (selectedLevel > 1) "<" else " "
        val showRight = if (selectedLevel < 5) ">" else " "
        val label = "$showLeft   LEVEL $selectedLevel   $showRight"

        canvas.drawText(label, cx, cy + 20f, menuValPaint)

        // Description
        val description = when (selectedLevel) {
            1 -> "Novice: The robot is still learning."
            2 -> "Skilled: He's getting the hang of it!"
            3 -> "Expert: A very smart opponent."
            4 -> "Master: He can see into the future!"
            5 -> "Grandmaster: The ultimate challenge!"
            else -> ""
        }
        canvas.drawText(description, cx, cy + 110f, menuSubPaint)

        val wins = SnakeStats.getWins(selectedLevel)
        val losses = SnakeStats.getLosses(selectedLevel)

        val statColor = if (wins >= losses) Color.GREEN else Color.RED
        statsPaint.color = statColor

        canvas.drawText("PLAYER WINS: $wins   |   ROBOT WINS: $losses", cx, cy + 200f, statsPaint)

        // Controls
        GraphicsUtils.createPaint(Color.DKGRAY, textSize = 30f, align = Paint.Align.CENTER).also {
            canvas.drawText("LEFT / RIGHT to change  |  CENTER to Play", cx, cy + 300f, it)
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
            when (action) {
                InputAction.SELECT -> { isPaused = false; SoundManager.playSelect() }
                InputAction.BACK -> { resetGame(); SoundManager.playSelect() }
                else -> {}
            }
            return
        }

        if (currentState == State.REPLAY) {
            handleGameOverInput(action)
            return
        }

        if (currentState == State.PLAYING) {
            if (action == InputAction.BACK) pauseGame()
            else handleGameInput(action)
        }
    }

    override fun handleGameOverInput(action: InputAction) {
        when (action) {
            InputAction.SELECT -> startGame() // Instant retry with same settings
            InputAction.BACK -> { resetGame(); SoundManager.playSelect() }
            else -> {}
        }
    }

    // 4. New Menu Input Handling
    private fun handleMenuInput(action: InputAction) {
        when (action) {
            InputAction.LEFT -> {
                if (selectedLevel > 1) {
                    selectedLevel--
                    SoundManager.playSelect()
                }
            }
            InputAction.RIGHT -> {
                if (selectedLevel < 5) {
                    selectedLevel++
                    SoundManager.playSelect()
                }
            }
            InputAction.SELECT -> {
                startGame()
            }
            else -> {}
        }
    }

    private fun startGame() {
        physics.setDifficultyLevel(selectedLevel)
        physics.reset()

        isGameOver = false
        currentState = State.PLAYING
        isGameStarted = true
        SoundManager.playPerfect(1)
    }

    override fun handleGameInput(action: InputAction, isDown: Boolean) { }

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
}