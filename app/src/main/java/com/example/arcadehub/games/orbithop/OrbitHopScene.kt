package com.example.arcadehub.games.orbithop

import android.graphics.Canvas
import com.example.arcadehub.core.BaseGameScene
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.managers.SoundManager

class OrbitHopScene : BaseGameScene() {

    private val physics = OrbitPhysics()
    private val renderer = OrbitRenderer()

    override val highScoreKey = "ORBIT_HIGHSCORE"
    private var isGameOverSoundPlayed = false

    override fun resetGame() {
        physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        isGameOverSoundPlayed = false
        isGameStarted = true
        isGameOver = false
        isPaused = false
        score = 0
    }

    override fun update(dt: Float) {
        if (isPaused) return

        physics.update(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, dt)
        score = physics.score

        if (physics.player.state == PlayerState.DEAD) {
            isGameOver = true
            if (!isGameOverSoundPlayed) {
                SoundManager.playGameOver()
                isGameOverSoundPlayed = true
                checkNewHighScore()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(
            canvas,
            physics,
            Constants.LOGIC_WIDTH,
            Constants.LOGIC_HEIGHT,
            highScore,
            isPaused
        )
    }

    override fun handleGameInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

        if (action == InputAction.SELECT) {
            when (physics.tapAction()) {
                LaunchInputResult.LAUNCHED -> Unit
                LaunchInputResult.BAD_ANGLE -> Unit
                LaunchInputResult.IGNORED -> Unit
            }
        }
    }
}
