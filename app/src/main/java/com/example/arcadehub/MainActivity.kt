package com.example.arcadehub

import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.arcadehub.core.CoreGameView
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.games.hub.HubScene
import com.example.arcadehub.managers.SaveManager
import com.example.arcadehub.managers.SceneManager
import com.example.arcadehub.managers.SoundManager

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: CoreGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC

        // Initialize Managers
        SaveManager.init(this)
        SoundManager.init(this)
        SceneManager.init(this)

        SceneManager.switchScene(HubScene())

        gameView = CoreGameView(this)
        setContentView(gameView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                SceneManager.onInput(InputAction.BACK, true)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.cleanup()
    }

    private fun mapKeyCode(keyCode: Int): InputAction? {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE -> InputAction.SELECT

            KeyEvent.KEYCODE_BUTTON_B -> InputAction.BACK

            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> InputAction.UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> InputAction.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> InputAction.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> InputAction.RIGHT
            else -> null
        }
    }

    private fun handleKeyEvent(keyCode: Int, event: KeyEvent?, isDown: Boolean): Boolean {
        if (isDown && event?.repeatCount != 0) return super.onKeyDown(keyCode, event)

        mapKeyCode(keyCode)?.let { action ->
            SceneManager.onInput(action, isDown)
            return true
        }
        return if (isDown) super.onKeyDown(keyCode, event) else super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = handleKeyEvent(keyCode, event, true)
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = handleKeyEvent(keyCode, event, false)
}