package com.example.arcadehub.managers

import android.graphics.Canvas
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.core.Scene

object SceneManager {
    @Volatile
    var activeScene: Scene? = null
        private set

    var logicWidth = Constants.LOGIC_WIDTH
    var logicHeight = Constants.LOGIC_HEIGHT

    fun switchScene(newScene: Scene) {
        // This ensures 'physics', 'renderer', etc. are ready before the Game Loop tries to use them.
        newScene.enter()
        val oldScene = activeScene
        activeScene = newScene
        oldScene?.exit()
    }

    fun update(dt: Float) {
        activeScene?.update(dt)
    }

    fun draw(canvas: Canvas) {
        activeScene?.draw(canvas)
    }

    fun onInput(action: InputAction, isDown: Boolean) {
        activeScene?.onInput(action, isDown)
    }
}