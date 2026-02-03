package com.example.arcadehub.core

import com.example.arcadehub.games.hub.HubScene
import com.example.arcadehub.managers.SaveManager
import com.example.arcadehub.managers.SceneManager
import com.example.arcadehub.managers.SoundManager

abstract class BaseGameScene : Scene {

    var isGameStarted = false
    var isGameOver = false
    var isPaused = false

    var score = 0
    var highScore = 0

    abstract val highScoreKey: String

    override fun enter() {
        highScore = SaveManager.getInt(highScoreKey, 0)
        resetGame()
    }

    // Standard Pause/Back Logic
    override fun onInput(action: InputAction, isDown: Boolean) {
        if (isPaused && isDown) {
            when (action) {
                InputAction.SELECT -> {
                    isPaused = false
                    SoundManager.playSelect()
                }
                InputAction.BACK -> quitToHub()
                else -> {}
            }
            return
        }

        if (isGameOver && isDown) {
            handleGameOverInput(action)
            return
        }

        if (!isGameStarted) {
            if (isDown) handleStartInput(action)
            return
        }

        // Standard Back behavior for active games
        if (isDown && action == InputAction.BACK) {
            pauseGame()
            return
        }

        handleGameInput(action, isDown)
    }

    protected fun checkNewHighScore() {
        if (score > highScore) {
            highScore = score
            SaveManager.saveHighScore(highScoreKey, highScore)
        }
    }

    protected fun pauseGame() {
        isPaused = true
        SoundManager.playSelect()
    }

    protected fun quitToHub() {
        // Save before quitting
        checkNewHighScore()
        SceneManager.switchScene(HubScene())
    }

    // Implementation specific
    abstract fun resetGame()
    abstract fun handleGameInput(action: InputAction, isDown: Boolean)

    open fun handleStartInput(action: InputAction) {
        // Default behavior: Any interaction starts game
        if (action == InputAction.SELECT) {
            isGameStarted = true
        } else if (action == InputAction.BACK) {
            quitToHub()
        }
    }

    open fun handleGameOverInput(action: InputAction) {
        when (action) {
            InputAction.SELECT -> resetGame()
            InputAction.BACK -> quitToHub()
            else -> {}
        }
    }

    override fun exit() {}
}