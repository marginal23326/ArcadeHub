package com.example.arcadehub.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.random.Random
import com.example.arcadehub.R

object SoundManager {
    private lateinit var soundPool: SoundPool
    private var soundsLoaded = false

    // Sound IDs
    var sfxPlace = 0
    var sfxPerfect = 0
    var sfxSelect = 0
    var sfxError = 0
    var sfxLaunch = 0
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

        // Load sounds
        sfxPlace = soundPool.load(context, R.raw.place, 1)
        sfxPerfect = soundPool.load(context, R.raw.perfect, 1)
        sfxSelect = soundPool.load(context, R.raw.select, 1)
        sfxError = soundPool.load(context, R.raw.error, 1)
        sfxLaunch = soundPool.load(context, R.raw.launch, 1)
        sfxAttach = soundPool.load(context, R.raw.attach, 1)

        // Load game over sounds
        sfxGameOverList.add(soundPool.load(context, R.raw.gameover1, 1))
        sfxGameOverList.add(soundPool.load(context, R.raw.gameover2, 1))
        sfxGameOverList.add(soundPool.load(context, R.raw.gameover3, 1))
        sfxGameOverList.add(soundPool.load(context, R.raw.gameover4, 1))

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundsLoaded = true
        }
    }

    fun playSound(soundId: Int, pitch: Float = 1f, volume: Float = 1f) {
        if (!soundsLoaded || soundId == 0) return
        soundPool.play(soundId, volume, volume, 1, 0, pitch)
    }

    fun playSelect() {
        playSound(sfxSelect)
    }

    fun playGameOver() {
        if (!soundsLoaded || sfxGameOverList.isEmpty()) return
        val randomIndex = Random.nextInt(sfxGameOverList.size)
        playSound(sfxGameOverList[randomIndex])
    }

    fun playPerfect(streak: Int) {
        val pitch = (1.0f + (streak * 0.05f)).coerceIn(1.0f, 2.0f)
        playSound(sfxPerfect, pitch)
    }

    fun playPlaceVariation() {
        val pitch = 0.95f + Random.nextFloat() * 0.1f
        playSound(sfxPlace, pitch)
    }

    // --- NEW HELPERS ---

    fun playLaunch() {
        val pitch = 1.0f + (Random.nextFloat() * 0.2f - 0.1f)
        playSound(sfxLaunch, pitch, 0.8f)
    }

    fun playAttach(score: Int) {
        val pitch = 1.0f + ((score % 5) * 0.05f)
        playSound(sfxAttach, pitch, 1.0f)
    }

    fun cleanup() {
        soundPool.release()
    }
}