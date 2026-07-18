package com.example.arcadehub.games.neonsnake

import androidx.core.graphics.toColorInt

object SnakeConfig {
    // --- BOARD SETTINGS ---
    const val COLS = 16
    const val ROWS = 9
    const val GAME_SPEED_SECONDS = 0.45f
    const val INITIAL_HEALTH = 100
    const val MIN_FOOD = 2
    const val FOOD_SPAWN_CHANCE_PERCENT = 15

    object Scores {
        const val WIN = 1_000_000_000.0
        const val LOSS = -1_000_000_000.0
        const val DRAW = -100_000_000.0

        const val TRAP_DANGER = -648_732_664.0
        const val STRATEGIC_SQUEEZE = -31_596_121.0
        const val ENEMY_TRAPPED = 165_629_600.0

        const val HEAD_ON_COLLISION = -263_709_044.0
        const val TIGHT_SPOT = -24_780.0

        const val LENGTH = 1_000.0
        const val EAT_REWARD = 250_507.0

        const val TERRITORY_CONTROL = 3_292.0
        const val KILL_PRESSURE = 253_847.0

        const val FOOD_CURVE_INTENSITY = 5.0
        const val FOOD_CURVE_EXPONENT = 75.0

        const val AGGRESSION = 2_378.0
    }

    // --- VISUALS ---
    val COLOR_BG = "#090D12".toColorInt()
    val COLOR_BOARD_BORDER = "#30363D".toColorInt()
    val COLOR_GRID = "#21262D".toColorInt()
    val COLOR_P1_HEAD = "#58A6FF".toColorInt()
    val COLOR_P1_BODY = "#1F6FEB".toColorInt()
    val COLOR_AI_HEAD = "#FF7B72".toColorInt()
    val COLOR_AI_BODY = "#D73A49".toColorInt()
    val COLOR_FOOD = "#3FB950".toColorInt()
    val COLOR_FOOD_OUTLINE = "#7EE787".toColorInt()
    val COLOR_SNAKE_EYE = "#0D1117".toColorInt()
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
