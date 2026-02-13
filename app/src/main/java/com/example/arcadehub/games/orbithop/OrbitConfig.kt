package com.example.arcadehub.games.orbithop

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
    val BG_COLOR = "#08121D".toColorInt()
    val BG_ACCENT_TOP = "#0F2437".toColorInt()
    val BG_ACCENT_BOTTOM = "#050B13".toColorInt()

    const val COLOR_PLAYER = 0xFFF3FAFF.toInt()

    const val COLOR_PIVOT_CORE_INACTIVE = 0xFF2B394A.toInt()
    const val COLOR_PIVOT_CORE_TARGET = 0xFF37C9FF.toInt()
    const val COLOR_PIVOT_HALO_INACTIVE = 0xFF1A2938.toInt()
    const val COLOR_PIVOT_HALO_TARGET = 0xFF66DDFF.toInt()

    const val COLOR_TRAJECTORY = 0xAA99DFFD.toInt()
    const val COLOR_TRAIL = 0xFF6CD9FF.toInt()

    const val COLOR_SCORE_LABEL = 0xB3C7DBEE.toInt()
    const val COLOR_HUD_PANEL = 0x66304357.toInt()
    const val COLOR_BEST_SCORE = 0xFFF3D98E.toInt()

    const val COLOR_OVERLAY_BORDER = 0xFF284760.toInt()
    const val COLOR_TEXT_PRIMARY = 0xFFF2FAFF.toInt()
}
