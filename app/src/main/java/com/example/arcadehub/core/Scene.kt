package com.example.arcadehub.core

import android.graphics.Canvas

interface Scene {
    /** Called when the scene becomes active */
    fun enter()

    /** Game logic update. dt = delta time in seconds */
    fun update(dt: Float)

    /** Render the frame */
    fun draw(canvas: Canvas)

    /**
     * Handle controller input
     * @param action The mapped action (JUMP, SELECT, etc)
     * @param isDown True if button pressed, False if released
     */
    fun onInput(action: InputAction, isDown: Boolean)

    /** Called when the scene is removed/switched */
    fun exit()
}