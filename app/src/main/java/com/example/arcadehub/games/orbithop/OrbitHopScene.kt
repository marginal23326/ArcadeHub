package com.example.arcadehub.games.orbithop

import android.graphics.Canvas
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.core.Scene
import com.example.arcadehub.games.hub.HubScene
import com.example.arcadehub.managers.SaveManager
import com.example.arcadehub.managers.SceneManager
import com.example.arcadehub.managers.SoundManager

class OrbitHopScene : Scene {

    private val physics = OrbitPhysics()
    private val renderer = OrbitRenderer()

    private var highScore = 0
    private var isGameOverSoundPlayed = false
    private var isPaused = false // <--- NEW FLAG

    override fun enter() {
        physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        highScore = SaveManager.getInt("ORBIT_HIGHSCORE", 0)
        isGameOverSoundPlayed = false
        isPaused = false
    }

    override fun update(dt: Float) {
        if (isPaused) return // Skip updates if paused

        physics.update(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, dt)

        if (physics.player.state == PlayerState.DEAD) {
            if (!isGameOverSoundPlayed) {
                SoundManager.playGameOver()
                isGameOverSoundPlayed = true
            }
            if (physics.score > highScore) {
                highScore = physics.score
                SaveManager.saveHighScore("ORBIT_HIGHSCORE", highScore)
            }
        }
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(canvas, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, highScore, isPaused)
    }

    override fun onInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

        // --- PAUSE HANDLING ---
        if (isPaused) {
            when (action) {
                InputAction.SELECT -> {
                    isPaused = false
                    SoundManager.playSelect()
                }
                InputAction.BACK -> {
                    SceneManager.switchScene(HubScene())
                }
                else -> {}
            }
            return
        }

        when (action) {
            InputAction.SELECT,
            InputAction.UP,
            InputAction.DOWN,
            InputAction.LEFT,
            InputAction.RIGHT -> {
                if (physics.player.state == PlayerState.DEAD) {
                    physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
                    isGameOverSoundPlayed = false
                    SoundManager.playSelect()
                } else {
                    if (physics.tapAction()) {
                        SoundManager.playLaunch()
                    }
                }
            }
            InputAction.BACK -> {
                // If Playing (Attached or Flying), Pause. Otherwise (Dead), Quit.
                if (physics.player.state == PlayerState.DEAD) {
                    SceneManager.switchScene(HubScene())
                } else {
                    isPaused = true
                    SoundManager.playSelect()
                }
            }
        }
    }

    override fun exit() {}
}