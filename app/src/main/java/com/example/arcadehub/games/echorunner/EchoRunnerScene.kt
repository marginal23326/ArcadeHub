package com.example.arcadehub.games.echorunner

import android.graphics.Canvas
import com.example.arcadehub.core.BaseGameScene
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.InputAction

class EchoRunnerScene : BaseGameScene() {

    private val physics = EchoPhysics()
    private val renderer = EchoRenderer()

    override val highScoreKey = "ECHO_HIGHSCORE"
    private var restartCooldown = 0f

    override fun resetGame() {
        physics.reset()
        isGameStarted = false
        isGameOver = false
        isPaused = false
        score = 0 // Sync base score with physics
    }

    override fun update(dt: Float) {
        if (isPaused) return

        if (!isGameStarted) return

        physics.update(dt)
        score = physics.score // Keep base score updated for UI

        if (physics.isDead && !isGameOver) {
            isGameOver = true
            restartCooldown = 1f
            checkNewHighScore()
        }

        if (isGameOver && restartCooldown > 0f) {
            restartCooldown -= dt
        }
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(canvas, this, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, highScore)
    }

    // Override because EchoRunner has specific start logic (Echo mode on start)
    override fun handleStartInput(action: InputAction) {
        if (action == InputAction.BACK) {
            quitToHub()
            return
        }
        if (action == InputAction.SELECT || action == InputAction.UP || action == InputAction.DOWN) {
            isGameStarted = true
            physics.setInput(true) // Start in Echo mode
        }
    }

    override fun handleGameInput(action: InputAction, isDown: Boolean) {
        if (action == InputAction.SELECT || action == InputAction.UP || action == InputAction.DOWN) {
            physics.setInput(isDown)
        }
    }

    // Override because of the restart cooldown logic
    override fun handleGameOverInput(action: InputAction) {
        if (restartCooldown <= 0f && (action == InputAction.SELECT || action == InputAction.UP || action == InputAction.DOWN)) {
            resetGame()
            physics.reset()
            isGameStarted = true // Immediately restart
        } else if (action == InputAction.BACK) {
            quitToHub()
        }
    }
}