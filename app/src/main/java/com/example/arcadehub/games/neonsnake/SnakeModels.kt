package com.example.arcadehub.games.neonsnake

// --- GEOMETRY ---
enum class GridDir(val x: Int, val y: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);
}

data class Point(val x: Int, val y: Int)

// --- ENTITIES ---
class SnakeEntity(
    var body: ArrayList<Point>,
    var dir: GridDir,
    var score: Int = 0,
    var health: Int = 100,
    var isAlive: Boolean = true
) {
    fun head(): Point = body.first()
}

data class GameSnapshot(
    val playerBody: List<Point>,
    val aiBody: List<Point>,
    val foods: List<Point>,
    val pScore: Int,
    val aScore: Int,
    val pHealth: Int,
    val aHealth: Int
)