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

    override fun enter() {
        physics.reset()
        bestScore = SaveManager.getInt("ECHO_HIGHSCORE", 0)
    }

    override fun update(dt: Float) {
        physics.update(dt)

        if (physics.state == GameState.GAMEOVER) {
            if (physics.score > bestScore) {
                bestScore = physics.score
                SaveManager.setInt("ECHO_HIGHSCORE", bestScore)
            }
        }
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(canvas, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, bestScore)
    }

    override fun onInput(action: InputAction, isDown: Boolean) {
        when (action) {
            InputAction.SELECT,
            InputAction.UP,
            InputAction.DOWN -> {
                // Any main button triggers dimension shift
                physics.setInput(isDown)

                // Audio feedback on press
                if (isDown && physics.state == GameState.PLAYING) {
                    // Optional: Play a sound when shifting dimensions
                    // SoundManager.playPlaceVariation()
                }
            }
            InputAction.BACK -> {
                if (isDown) {
                    SceneManager.switchScene(HubScene())
                }
            }
            else -> {}
        }
    }

    override fun exit() {
        // Cleanup if needed
    }
}