package com.example.arcadehub.games.orbithop

enum class PlayerState { ATTACHED, FLYING, DEAD }

enum class TimingGrade { MISS, GOOD, PERFECT }

enum class LaunchInputResult { IGNORED, BAD_ANGLE, LAUNCHED }

data class Player(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angle: Float = 0f,
    var state: PlayerState = PlayerState.ATTACHED,
    var currentPivot: Pivot? = null
)

data class Pivot(
    val id: Int,
    val x: Float,
    val y: Float,
    var isTarget: Boolean,
    val dir: Int,
    val lane: Int
)
