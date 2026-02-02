package com.example.arcadehub.games.echorunner

import android.graphics.Color

object EchoConfig {
    // --- DIFFICULTY ---
    const val BASE_SPEED = 700f
    const val SPEED_INC_PER_SECOND = 15f   // Increases speed every second
    const val SPEED_INC_PER_SCORE = 2f     // Increases speed for every point
    const val MAX_SPEED = 2400f

    // Density: How often blocks spawn
    const val SPAWN_INTERVAL_START = 1.4f
    const val SPAWN_INTERVAL_MIN = 0.45f   // Very tight gaps at high speed

    const val PLAYER_SIZE = 80f

    const val COLOR_REAL_BG = Color.WHITE
    const val COLOR_REAL_FG = Color.BLACK
    const val COLOR_ECHO_BG = Color.BLACK
    const val COLOR_ECHO_FG = Color.WHITE
    const val PARTICLE_COUNT = 60
}