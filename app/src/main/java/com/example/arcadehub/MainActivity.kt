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
        SaveManager.init(this)
        SoundManager.init(this)

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.repeatCount != 0) return super.onKeyDown(keyCode, event)
        val action = mapKeyCode(keyCode)
        if (action != null) {
            SceneManager.onInput(action, true) // isDown = true
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val action = mapKeyCode(keyCode)
        if (action != null) {
            SceneManager.onInput(action, false) // isDown = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}