package com.example.arcadehub.games.orbithop

import android.graphics.Color
import androidx.core.graphics.toColorInt

object OrbitConfig {
    const val TRAIL_LENGTH = 30

    // --- PHYSICS ---
    const val PIVOT_RADIUS = 30f
    const val CATCH_RADIUS = 75f
    const val CATCH_RADIUS_SQ = CATCH_RADIUS * CATCH_RADIUS
    const val PLAYER_RADIUS = 15f

    const val FLY_SPEED = 1600f
    const val ORBIT_SPEED_START = 3.5f
    const val ORBIT_SPEED_INC = 0.2f
    const val ORBIT_SPEED_MAX = 9.0f

    const val MIN_Y_DIST = 350f
    const val MAX_Y_DIST = 500f
    const val HORIZONTAL_PADDING = 100f
    const val MAX_X_HOP = 350f
    const val MISS_MARGIN_Y = 300f

    // --- COLORS ---
    val BG_COLOR = "#1A1A1A".toColorInt()
    val COLOR_PLAYER = Color.WHITE

    const val COLOR_PIVOT_CORE_INACTIVE = 0xFF444444.toInt()
    const val COLOR_PIVOT_CORE_TARGET = 0xFF00D2FF.toInt()

    const val COLOR_PIVOT_HALO_INACTIVE = 0xFF2A2A2A.toInt()
    const val COLOR_PIVOT_HALO_TARGET = 0xFF004455.toInt()

    const val COLOR_TRAJECTORY = 0xFF888888.toInt()

    const val COLOR_TRAIL = 0xFF00D2FF.toInt()
    const val COLOR_BEST_SCORE = 0xFFFFD700.toInt()
}