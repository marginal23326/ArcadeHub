package com.example.arcadehub.games.hub

import android.graphics.Canvas
import com.example.arcadehub.R
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.core.Scene
import com.example.arcadehub.games.blockstack.BlockStackScene
import com.example.arcadehub.games.echorunner.EchoRunnerScene
import com.example.arcadehub.games.orbithop.OrbitHopScene
import com.example.arcadehub.managers.SceneManager
import com.example.arcadehub.managers.SoundManager
import kotlin.system.exitProcess

object HubConfig {
    const val COLUMNS = 5
}

data class HubGameData(val title: String, val resId: Int)

class HubScene : Scene {
    private val renderer = HubRenderer()

    private val gameList = listOf(
        HubGameData("BLOCK STACK", R.drawable.menu_blockstack),
        HubGameData("ECHO RUNNER", R.drawable.menu_echorunner),
        HubGameData("ORBIT HOP", R.drawable.menu_orbithop),
    )

    companion object { private var lastSelection = 0 }
    private var selectedIndex = lastSelection

    override fun enter() { }
    override fun update(dt: Float) = renderer.updateAnim(dt)
    override fun draw(canvas: Canvas) = renderer.draw(canvas, gameList, selectedIndex)

    override fun onInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

        val cols = HubConfig.COLUMNS
        val row = selectedIndex / cols
        val col = selectedIndex % cols

        when (action) {
            InputAction.LEFT -> {
                if (col > 0) {
                    selectedIndex--
                    SoundManager.playSelect()
                }
            }
            InputAction.RIGHT -> {
                if (selectedIndex < gameList.size - 1) {
                    selectedIndex++
                    SoundManager.playSelect()
                }
            }
            InputAction.UP -> {
                if (row > 0) {
                    selectedIndex -= cols
                    SoundManager.playSelect()
                }
            }
            InputAction.DOWN -> {
                if (selectedIndex + cols < gameList.size) {
                    selectedIndex += cols
                    SoundManager.playSelect()
                }
            }
            InputAction.SELECT -> {
                SoundManager.playPerfect(1)
                launchGame(selectedIndex)
            }
            InputAction.BACK -> exitProcess(0)
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

    override fun exit() { }
}