package com.example.arcadehub.games.neonsnake

import android.graphics.Color
import androidx.core.graphics.toColorInt

object SnakeConfig {
    // --- BOARD SETTINGS ---
    const val COLS = 16
    const val ROWS = 9

    // Slowed down to compensate for smaller grid and "Grandmaster" feel
    const val GAME_SPEED_SECONDS = 0.45f

    const val INITIAL_HEALTH = 100
    const val FOOD_COUNT = 2

    object Scores {
        const val WIN = 1_000_000_000.0
        const val LOSS = -1_000_000_000.0

        const val TRAP_DANGER = -1_000_000.0
        const val HEAD_ON_COLLISION = -750_000.0

        const val LENGTH = 1_000.0
        const val EAT_REWARD = 50_000.0
        const val AGGRESSION = 100.0

        // Food Curve Constants
        const val FOOD_CURVE_INTENSITY = 70.0
        const val FOOD_CURVE_EXPONENT = 8.0
    }

    // --- VISUALS ---
    val COLOR_BG = "#050505".toColorInt()
    val COLOR_GRID = "#1a1a1a".toColorInt()

    val COLOR_P1_HEAD = "#00ccff".toColorInt() // Cyan
    val COLOR_P1_BODY = "#0099cc".toColorInt()
    val COLOR_P1_TAIL = "#004466".toColorInt() // Darker Cyan

    val COLOR_AI_HEAD = "#ff0055".toColorInt() // Pink/Red
    val COLOR_AI_BODY = "#cc0044".toColorInt()
    val COLOR_AI_TAIL = "#660022".toColorInt() // Darker Red

    val COLOR_FOOD = Color.YELLOW
}

object SnakeStats {
    fun getWins(level: Int): Int {
        return com.example.arcadehub.managers.SaveManager.getInt("SNAKE_L${level}_WINS", 0)
    }

    fun getLosses(level: Int): Int {
        return com.example.arcadehub.managers.SaveManager.getInt("SNAKE_L${level}_LOSSES", 0)
    }

    fun recordWin(level: Int) {
        val key = "SNAKE_L${level}_WINS"
        val current = com.example.arcadehub.managers.SaveManager.getInt(key, 0)
        com.example.arcadehub.managers.SaveManager.setInt(key, current + 1)
    }

    fun recordLoss(level: Int) {
        val key = "SNAKE_L${level}_LOSSES"
        val current = com.example.arcadehub.managers.SaveManager.getInt(key, 0)
        com.example.arcadehub.managers.SaveManager.setInt(key, current + 1)
    }
}