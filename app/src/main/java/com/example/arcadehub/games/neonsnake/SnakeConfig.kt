package com.example.arcadehub.games.neonsnake

import android.graphics.Color
import androidx.core.graphics.toColorInt

object SnakeConfig {
    // Grid (Optimized for 1920x1080 - 16:9 Ratio)
    // 48 * 40px = 1920 width
    // 24 * 40px = 960 height (leaving 120px for HUD)
    const val TILE_COUNT_X = 32
    const val TILE_COUNT_Y = 18

    // Physics
    const val GAME_SPEED_SECONDS = 0.28f

    // AI Weights
    const val AI_SCORE_TUNNEL_DEATH = -80_000
    const val AI_SCORE_TRAPPED = -100_000
    const val AI_SCORE_HEAD_ON_COLLISION = -150_000
    const val AI_SCORE_SPACE_MULTIPLIER = 20

    // Visuals
    val COLOR_BG = "#050505".toColorInt()
    val COLOR_GRID = "#111111".toColorInt()

    val COLOR_P1_HEAD = Color.WHITE
    val COLOR_P1_BODY = Color.CYAN

    val COLOR_AI_HEAD = Color.WHITE
    val COLOR_AI_BODY = "#FF0055".toColorInt() // Neon Pink

    val COLOR_FOOD = Color.YELLOW
    val COLOR_BEST_TEXT = Color.LTGRAY
}