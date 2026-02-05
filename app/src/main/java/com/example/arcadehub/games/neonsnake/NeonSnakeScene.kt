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

    private enum class State { SELECT_DIFFICULTY, PLAYING }
    private var currentState = State.SELECT_DIFFICULTY
    private var selectedDifficulty = 0 // 0 = Standard, 1 = Aggressive

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
        // Don't reset physics yet, wait for selection
    }

    override fun update(dt: Float) {
        if (currentState == State.PLAYING && !isPaused) {
            physics.update(dt)
            score = physics.player.score

            if (physics.isGameOver && !isGameOver) {
                isGameOver = true
                SoundManager.playGameOver()
                checkNewHighScore()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (currentState == State.SELECT_DIFFICULTY) {
            drawDifficultyMenu(canvas)
        } else {
            renderer.draw(canvas, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, highScore)
            if (isPaused) {
                GraphicsUtils.drawPauseMenu(canvas, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
            }
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