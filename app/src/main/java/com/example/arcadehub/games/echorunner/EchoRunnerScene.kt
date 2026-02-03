package com.example.arcadehub.games.echorunner

import android.graphics.Canvas
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.core.Scene
import com.example.arcadehub.games.hub.HubScene
import com.example.arcadehub.managers.SaveManager
import com.example.arcadehub.managers.SceneManager
import com.example.arcadehub.managers.SoundManager

class EchoRunnerScene : Scene {

    private val physics = EchoPhysics()
    private val renderer = EchoRenderer()

    private var bestScore = 0

    var isGameStarted = false
    var isGameOver = false
    var isPaused = false
    private var restartCooldown = 0f

    override fun enter() {
        physics.reset()
        bestScore = SaveManager.getInt("ECHO_HIGHSCORE", 0)
        isGameStarted = false
        isGameOver = false
        isPaused = false
    }

    override fun update(dt: Float) {
        if (isPaused) return

        if (!isGameStarted) return

        physics.update(dt)

        if (physics.isDead && !isGameOver) {
            isGameOver = true
            restartCooldown = 1f
            if (physics.score > bestScore) {
                bestScore = physics.score
                SaveManager.setInt("ECHO_HIGHSCORE", bestScore)
            }
        }

        if (isGameOver && restartCooldown > 0f) {
            restartCooldown -= dt
        }
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(canvas, this, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, bestScore)
    }

    override fun onInput(action: InputAction, isDown: Boolean) {
        if (isPaused) {
            if (isDown && action == InputAction.SELECT) {
                isPaused = false
                SoundManager.playSelect()
            } else if (isDown && action == InputAction.BACK) {
                SceneManager.switchScene(HubScene())
            }
            return
        }

        if (!isGameStarted) {
            if (isDown && (action == InputAction.SELECT || action == InputAction.UP || action == InputAction.DOWN)) {
                isGameStarted = true
                // Ensure player starts in Echo mode if they held the button to start
                physics.setInput(true)
            } else if (isDown && action == InputAction.BACK) {
                SceneManager.switchScene(HubScene())
            }
            return
        }

        if (isGameOver) {
            // Only restart if cooldown is done AND button is released (to prevent accidental restarts)
            if (!isDown && restartCooldown <= 0f && (action == InputAction.SELECT || action == InputAction.UP || action == InputAction.DOWN)) {
                physics.reset()
                isGameOver = false
            } else if (isDown && action == InputAction.BACK) {
                SceneManager.switchScene(HubScene())
            }
            return
        }

        when (action) {
            InputAction.SELECT,
            InputAction.UP,
            InputAction.DOWN -> {
                physics.setInput(isDown)
            }
            InputAction.BACK -> {
                if (isDown) {
                    isPaused = true
                    SoundManager.playSelect()
                }
            }
            else -> {}
        }
    }

    override fun exit() {}
}