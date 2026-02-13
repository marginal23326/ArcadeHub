package com.example.arcadehub.games.orbithop

import androidx.core.graphics.toColorInt

object OrbitConfig {
    const val TRAIL_LENGTH = 22

    // --- PHYSICS ---
    const val PIVOT_RADIUS = 26f
    const val CATCH_RADIUS = 72f
    const val CATCH_RADIUS_SQ = CATCH_RADIUS * CATCH_RADIUS
    const val PLAYER_RADIUS = 14f

    const val FLY_SPEED = 1240f
    const val ORBIT_ANGULAR_SPEED = 4.2f

    const val BEAT_INTERVAL_START = 0.86f
    const val BEAT_INTERVAL_MIN = 0.64f
    const val BEAT_ACCEL_PER_SCORE = 0.0015f
    const val BEAT_PULSE_DECAY = 3.2f

    const val MIN_ATTACH_TIME = 0.05f
    const val PERFECT_WINDOW = 0.075f
    const val GOOD_WINDOW = 0.16f
    const val MIN_LAUNCH_ALIGNMENT = 0.60f
    const val PERFECT_LAUNCH_ALIGNMENT = 0.90f

    const val MAX_BEATS_PER_PIVOT = 4
    const val MAX_FLIGHT_TIME = 1.45f
    const val MISS_MARGIN_Y = 220f
    const val OUT_BOUNDS_MARGIN = 80f
    const val GRAVITY_FIELD_RADIUS = 430f
    const val GRAVITY_FIELD_RADIUS_SQ = GRAVITY_FIELD_RADIUS * GRAVITY_FIELD_RADIUS
    const val GRAVITY_CONST = 190_000_000f
    const val GRAVITY_MAX_ACCEL = 34_000f
    const val GRAVITY_MIN_DIST_SQ = 1_600f
    const val GRAVITY_NEAR_BOOST = 2.8f
    const val GRAVITY_CAPTURE_RADIUS = CATCH_RADIUS + 56f
    const val GRAVITY_CAPTURE_PULL = 54_000f
    const val GRAVITY_CAPTURE_TANGENTIAL_DAMP = 28f
    const val GRAVITY_CAPTURE_CENTER_STEER = 13f
    const val FLIGHT_DRAG = 0.0f
    const val CAMERA_LERP = 5.2f

    const val LANE_COUNT = 5
    const val HORIZONTAL_PADDING = 120f
    const val GAP_Y_BASE = 255f
    const val GAP_Y_MAX = 340f
    const val GAP_Y_GROW_PER_SCORE = 2.2f
    const val GAP_Y_JITTER = 30f
    const val LANE_JITTER_X = 16f

    // --- COLORS ---
    val BG_COLOR = "#08111B".toColorInt()
    val BG_ACCENT_TOP = "#11283D".toColorInt()
    val BG_ACCENT_BOTTOM = "#060C14".toColorInt()

    const val COLOR_PLAYER = 0xFFF2F9FF.toInt()

    const val COLOR_PIVOT_CORE = 0xFF2F4257.toInt()
    const val COLOR_PIVOT_CURRENT = 0xFF6EAED7.toInt()
    const val COLOR_PIVOT_TARGET = 0xFF4AD7FF.toInt()
    const val COLOR_PIVOT_RING = 0xFF1E3246.toInt()
    const val COLOR_TARGET_RING = 0xFF79E5FF.toInt()

    const val COLOR_TRAIL = 0xAA8CE3FF.toInt()
    const val COLOR_LAUNCH_INDICATOR_GOOD = 0xFFB7EEFF.toInt()
    const val COLOR_LAUNCH_INDICATOR_BAD = 0xAA5B738B.toInt()

    const val COLOR_TEXT_PRIMARY = 0xFFF2FAFF.toInt()
    const val COLOR_TEXT_SECONDARY = 0xFFBED2E3.toInt()
    const val COLOR_SCORE = 0xFFF3FAFF.toInt()
    const val COLOR_BEST_SCORE = 0xFFEFDFA1.toInt()

    const val COLOR_BEAT_TRACK = 0x5530475A
    const val COLOR_BEAT_FILL = 0xFF58CBF3.toInt()
    const val COLOR_BEAT_FILL_ACTIVE = 0xFF98E7FF.toInt()

}
