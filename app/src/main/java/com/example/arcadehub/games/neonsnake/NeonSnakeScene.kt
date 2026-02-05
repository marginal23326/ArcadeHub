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

    private enum class State { SELECT_DIFFICULTY, PLAYING, REPLAY }
    private var currentState = State.SELECT_DIFFICULTY
    private var selectedDifficulty = 0

    private var replayData: List<GameSnapshot> = emptyList()
    private var replayIndex = 0
    private var replayTimer = 0f
    private val REPLAY_SPEED = 0.5f // 0.5s per tick

    override val highScoreKey: String = "SNAKE_HIGHSCORE"

    // UI Paints
    private val menuTitlePaint = GraphicsUtils.createPaint(Color.CYAN, textSize = 80f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val menuOptPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 50f, align = Paint.Align.CENTER)
    private val menuSelPaint = GraphicsUtils.createPaint(Color.YELLOW, textSize = 55f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

    override fun resetGame() {
        currentState = State.SELECT_DIFFICULTY
        isGameStarted = false
        isGameOver = false
        isPaused = false
        score = 0
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
                // Loop through history slowly
                replayTimer += dt
                if (replayTimer >= REPLAY_SPEED) {
                    replayTimer = 0f
                    replayIndex++
                    if (replayIndex >= replayData.size) {
                        replayIndex = 0 // Loop back
                    }
                }
            }
            else -> {}
        }
    }

    private fun startReplayMode() {
        isGameOver = true
        SoundManager.playGameOver()
        checkNewHighScore()

        replayData = physics.getReplayHistory()
        replayIndex = 0
        replayTimer = 0f
        currentState = State.REPLAY
    }

    override fun draw(canvas: Canvas) {
        when (currentState) {
            State.SELECT_DIFFICULTY -> drawDifficultyMenu(canvas)
            State.PLAYING -> renderer.draw(canvas, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, highScore)
            State.REPLAY -> {
                // Ensure we have data, otherwise fallback to standard draw
                if (replayData.isNotEmpty()) {
                    renderer.drawReplayFrame(
                        canvas,
                        replayData[replayIndex],
                        Constants.LOGIC_WIDTH,
                        Constants.LOGIC_HEIGHT,
                        highScore,
                        physics // Pass physics just for the Game Over text content
                    )
                }
            }
        }

        if (isPaused) {
            GraphicsUtils.drawPauseMenu(canvas, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        }
    }

    private fun drawDifficultyMenu(canvas: Canvas) {
        canvas.drawColor(SnakeConfig.COLOR_BG)
        val cx = Constants.LOGIC_WIDTH / 2f
        val cy = Constants.LOGIC_HEIGHT / 2f

        canvas.drawText("SELECT AI PERSONALITY", cx, cy - 150f, menuTitlePaint)

        val opt1 = "STANDARD (PASSIVE)"
        val opt2 = "HUNTER (AGGRESSIVE)"

        if (selectedDifficulty == 0) {
            canvas.drawText("> $opt1 <", cx, cy, menuSelPaint)
            canvas.drawText(opt2, cx, cy + 100f, menuOptPaint)
        } else {
            canvas.drawText(opt1, cx, cy, menuOptPaint)
            canvas.drawText("> $opt2 <", cx, cy + 100f, menuSelPaint)
        }

        GraphicsUtils.createPaint(Color.LTGRAY, textSize = 30f, align = Paint.Align.CENTER).also {
            canvas.drawText("UP/DOWN to Select  |  CENTER to Start", cx, cy + 300f, it)
        }
    }

    override fun onInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

        // STATE: Personality Menu
        if (currentState == State.SELECT_DIFFICULTY) {
            if (action == InputAction.BACK) {
                quitToHub() // Only exit to main hub from the snake menu
            } else {
                handleStartInput(action)
            }
            return
        }

        // STATE: Paused
        if (isPaused) {
            when (action) {
                InputAction.SELECT -> {
                    isPaused = false
                    SoundManager.playSelect()
                }
                InputAction.BACK -> {
                    resetGame() // Return to personality menu
                    SoundManager.playSelect()
                }
                else -> {}
            }
            return
        }

        // STATE: Replay / Game Over
        if (currentState == State.REPLAY) {
            handleGameOverInput(action)
            return
        }

        // STATE: Playing
        if (currentState == State.PLAYING) {
            if (action == InputAction.BACK) {
                pauseGame()
            } else {
                handleGameInput(action, true)
            }
        }
    }

    override fun handleGameOverInput(action: InputAction) {
        when (action) {
            InputAction.SELECT -> {
                // Restart immediately with the same AI
                startGame()
            }
            InputAction.BACK -> {
                // Return to the Personality Menu
                resetGame()
                SoundManager.playSelect()
            }
            else -> {}
        }
    }

    override fun handleStartInput(action: InputAction) {
        // Override default start behavior to handle menu
        if (currentState == State.SELECT_DIFFICULTY) {
            when (action) {
                InputAction.UP, InputAction.DOWN -> {
                    selectedDifficulty = if (selectedDifficulty == 0) 1 else 0
                    SoundManager.playSelect()
                }
                InputAction.SELECT -> {
                    startGame()
                }
                InputAction.BACK -> quitToHub()
                else -> {}
            }
        }
    }

    private fun startGame() {
        physics.setDifficulty(selectedDifficulty == 1)
        physics.reset()
        isGameOver = false
        currentState = State.PLAYING
        isGameStarted = true
        SoundManager.playPerfect(1)
    }

    override fun handleGameInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

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