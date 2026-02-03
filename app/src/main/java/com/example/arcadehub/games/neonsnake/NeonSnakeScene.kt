package com.example.arcadehub.games.neonsnake

import android.graphics.Canvas
import com.example.arcadehub.core.BaseGameScene
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.managers.SoundManager

class NeonSnakeScene : BaseGameScene() {

    private val physics = SnakePhysics()
    private val renderer = SnakeRenderer()

    override val highScoreKey: String = "SNAKE_HIGHSCORE"

    override fun resetGame() {
        physics.reset()
        isGameStarted = true
        isGameOver = false
        isPaused = false
        score = 0
    }

    override fun update(dt: Float) {
        if (isPaused) return

        physics.update(dt)
        score = physics.player.score

        if (physics.isGameOver && !isGameOver) {
            isGameOver = true
            SoundManager.playGameOver()
            checkNewHighScore()
        }
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(canvas, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, highScore)
        if (isPaused) {
            com.example.arcadehub.core.GraphicsUtils.drawPauseMenu(canvas, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        }
    }

    override fun handleGameInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

        // Map Input directly to GridDir and let Physics Queue handle validation
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