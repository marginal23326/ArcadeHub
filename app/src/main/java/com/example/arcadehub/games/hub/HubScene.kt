package com.example.arcadehub.games.hub

import android.graphics.Canvas
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.core.Scene
import com.example.arcadehub.games.blockstack.BlockStackScene
import com.example.arcadehub.games.echorunner.EchoRunnerScene
import com.example.arcadehub.games.orbithop.OrbitHopScene
import com.example.arcadehub.managers.SceneManager
import com.example.arcadehub.managers.SoundManager
import kotlin.system.exitProcess

class HubScene : Scene {

    private val renderer = HubRenderer()

    // The list of available games
    private val gameTitles = listOf(
        "BLOCK STACK",
        "ECHO RUNNER",
        "ORBIT HOP"
    )

    companion object {
        private var lastSelection = 0
    }

    private var selectedIndex = lastSelection

    override fun enter() {
        // Reset selection or play menu music here
    }

    override fun update(dt: Float) {
        // The menu is static, but we could animate the background here
        renderer.updateAnim(dt)
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(canvas, gameTitles, selectedIndex)
    }

    override fun onInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return // Only react to button presses, ignore releases

        when (action) {
            InputAction.UP, InputAction.LEFT -> {
                selectedIndex--
                if (selectedIndex < 0) selectedIndex = gameTitles.size - 1
                SoundManager.playSelect()
            }
            InputAction.DOWN, InputAction.RIGHT -> {
                selectedIndex++
                if (selectedIndex >= gameTitles.size) selectedIndex = 0
                SoundManager.playSelect()
            }
            InputAction.SELECT -> {
                SoundManager.playPerfect(1)
                launchGame(selectedIndex)
            }
            InputAction.BACK -> {
                exitProcess(0)
            }
        }
    }

    private fun launchGame(index: Int) {
        lastSelection = index

        val nextScene = when (index) {
            0 -> BlockStackScene()
            1 -> EchoRunnerScene()
            2 -> OrbitHopScene()
            else -> BlockStackScene()
        }
        SceneManager.switchScene(nextScene)
    }

    override fun exit() {
        // Cleanup if needed
    }
}