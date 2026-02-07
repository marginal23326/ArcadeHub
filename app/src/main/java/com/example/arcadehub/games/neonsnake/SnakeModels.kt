package com.example.arcadehub.games.neonsnake

// --- GEOMETRY ---
enum class GridDir(val x: Int, val y: Int) {
    UP(0, 1),
    DOWN(0, -1),
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

// --- LOGIC GRID ---
class LogicGrid(val width: Int, val height: Int) {
    // 0: Empty, 1: Food, 2: My Body, 3: Enemy Body, 4: Heads
    private val cells = IntArray(width * height)

    private fun idx(x: Int, y: Int) = y * width + x

    operator fun get(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 9 // Out of bounds
        return cells[idx(x, y)]
    }

    operator fun set(x: Int, y: Int, `val`: Int) {
        if (x in 0 until width && y in 0 until height) {
            cells[idx(x, y)] = `val`
        }
    }

    fun isSafe(x: Int, y: Int): Boolean {
        val v = get(x, y)
        return v == 0 || v == 1
    }

    fun clone(): LogicGrid {
        val newGrid = LogicGrid(width, height)
        System.arraycopy(this.cells, 0, newGrid.cells, 0, this.cells.size)
        return newGrid
    }

    companion object {
        fun fromState(pBody: List<Point>, aBody: List<Point>, foods: List<Point>): LogicGrid {
            val g = LogicGrid(SnakeConfig.COLS, SnakeConfig.ROWS)

            foods.forEach { g[it.x, it.y] = 1 }

            pBody.forEachIndexed { i, p -> g[p.x, p.y] = if (i == 0) 4 else 2 } // 2 = Me (Player/AI depending on context)
            aBody.forEachIndexed { i, p -> g[p.x, p.y] = if (i == 0) 4 else 3 } // 3 = Enemy

            return g
        }
    }
}