package com.example.arcadehub.games.neonsnake

enum class GridDir(val x: Int, val y: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0)
}

data class Point(val x: Int, val y: Int)

data class GameSnapshot(
    val playerBody: List<Point>,
    val aiBody: List<Point>,
    val food: Point,
    val pScore: Int,
    val aScore: Int,
    val pHeadColor: Int, // Store color in case we want to flash red on death later
    val aHeadColor: Int
)

class SnakeEntity(
    var body: ArrayList<Point>,
    var dir: GridDir,
    var nextDir: GridDir,
    var score: Int = 0,
    var isAlive: Boolean = true,
    var didEat: Boolean = false
) {
    fun head(): Point = body.first()
}