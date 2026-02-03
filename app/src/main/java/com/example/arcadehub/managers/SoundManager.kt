package com.example.arcadehub.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.random.Random
import com.example.arcadehub.R

object SoundManager {
    private lateinit var soundPool: SoundPool
    private var soundsLoaded = false

    var sfxPlace = 0
    var sfxPerfect = 0
    var sfxSelect = 0
    var sfxError = 0
    var sfxSwoosh = 0
    var sfxAttach = 0

    private val sfxGameOverList = mutableListOf<Int>()

    fun init(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        fun load(resId: Int) = soundPool.load(context, resId, 1)

        sfxPlace = load(R.raw.place)
        sfxPerfect = load(R.raw.perfect)
        sfxSelect = load(R.raw.select)
        sfxError = load(R.raw.error)
        sfxSwoosh = load(R.raw.swoosh)
        sfxAttach = load(R.raw.attach)

        listOf(R.raw.gameover1, R.raw.gameover2, R.raw.gameover3, R.raw.gameover4)
            .forEach { sfxGameOverList.add(load(it)) }

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundsLoaded = true
        }
    }

    fun playSound(soundId: Int, pitch: Float = 1f, volume: Float = 1f) {
        if (!soundsLoaded || soundId == 0) return
        soundPool.play(soundId, volume, volume, 1, 0, pitch)
    }

    fun playSelect() = playSound(sfxSelect)

    fun playGameOver() {
        if (!soundsLoaded || sfxGameOverList.isEmpty()) return
        playSound(sfxGameOverList.random())
    }

    fun playPerfect(streak: Int) {
        val pitch = (1.0f + (streak * 0.05f)).coerceIn(1.0f, 2.0f)
        playSound(sfxPerfect, pitch)
    }

    fun playPlaceVariation() {
        playSound(sfxPlace, 0.95f + Random.nextFloat() * 0.1f)
    }

    fun playSwoosh() {
        playSound(sfxSwoosh, 1.0f + (Random.nextFloat() * 0.2f - 0.1f), 0.8f)
    }

    fun playAttach(score: Int) {
        playSound(sfxAttach, 1.0f + ((score % 5) * 0.05f))
    }

    fun cleanup() = soundPool.release()
}