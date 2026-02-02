package com.example.arcadehub.games.blockstack

// --- Enums ---

enum class GameMode(val displayName: String) {
    LINEAR("Classic"),
    ORBIT("Orbit")
}

enum class AbilityType(val displayName: String, val cost: Int, val maxStock: Int) {
    SLO_MO("Slo-Mo", 50, 10),
    MAGNET("Magnet", 75, 10),
    WIDENER("Widen", 150, 5),
    SECOND_CHANCE("Revive", 100, 1)
}

enum class PlacementType { PERFECT, NORMAL, MISS }

// --- Data Classes ---

interface IGameBlock {
    val id: Int
}

data class Block(
    override val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val color: Int
) : IGameBlock

data class ArcBlock(
    override val id: Int,
    val radius: Float,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Int
) : IGameBlock

data class Debris(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val color: Int,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var rotation: Float = 0f,
    val rotSpeed: Float,
    val isRadial: Boolean = false,
    val startAngle: Float = 0f,
    val sweepAngle: Float = 0f
)

data class PlacementResult(
    val type: PlacementType,
    val accuracy: Float = 0f
)