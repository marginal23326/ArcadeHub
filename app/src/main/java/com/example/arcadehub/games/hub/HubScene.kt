package com.example.arcadehub.games.hub

import android.graphics.Canvas
import com.example.arcadehub.R
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.core.Scene
import com.example.arcadehub.games.blockstack.BlockStackScene
import com.example.arcadehub.games.echorunner.EchoRunnerScene
import com.example.arcadehub.games.orbithop.OrbitHopScene
import com.example.arcadehub.games.neonsnake.NeonSnakeScene
import com.example.arcadehub.managers.SceneManager
import com.example.arcadehub.managers.SoundManager
import kotlin.system.exitProcess

object HubConfig {
    const val COLUMNS = 4
}

data class HubGameData(
    val title: String,
    val resId: Int,
    val sceneFactory: () -> Scene
)

class HubScene : Scene {
    private val renderer = HubRenderer()

    private val gameList = listOf(
        HubGameData("Block Stack", R.drawable.menu_blockstack) { BlockStackScene() },
        HubGameData("Echo Runner", R.drawable.menu_echorunner) { EchoRunnerScene() },
        HubGameData("Orbit Hop", R.drawable.menu_orbithop) { OrbitHopScene() },
        HubGameData("Neon Snake", R.drawable.menu_neonsnake) { NeonSnakeScene() }
    )

    companion object { private var lastSelection = 0 }
    private var selectedIndex = lastSelection

    override fun enter() {
        renderer.preloadBitmaps(gameList)
    }
    override fun update(dt: Float) = renderer.updateAnim(dt)
    override fun draw(canvas: Canvas) = renderer.draw(canvas, gameList, selectedIndex)

    override fun onInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

        val cols = minOf(HubConfig.COLUMNS, gameList.size.coerceAtLeast(1))
        val row = selectedIndex / cols

        when (action) {
            InputAction.LEFT -> {
                if (selectedIndex % cols > 0) {
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
        if (index in gameList.indices) {
            SceneManager.switchScene(gameList[index].sceneFactory())
        }
    }

    override fun exit() { }
}
