package com.example.arcadehub.managers

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.core.Scene

object SceneManager {
    @Volatile
    var activeScene: Scene? = null
        private set

    lateinit var resources: Resources

    fun init(context: Context) {
        resources = context.resources
    }

    fun switchScene(newScene: Scene) {
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