package com.example.arcadehub.games.neonsnake

class SnakeGrid(val width: Int, val height: Int) {
    // 0: Empty, 1: Food, 2: My Body, 3: Enemy Body, 4: Heads
    private val cells = IntArray(width * height)

    private fun idx(x: Int, y: Int) = y * width + x

    operator fun get(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 9 // Out of bounds
        return cells[idx(x, y)]
    }

    operator fun set(x: Int, y: Int, value: Int) {
        if (x in 0 until width && y in 0 until height) {
            cells[idx(x, y)] = value
        }
    }

    fun isSafe(x: Int, y: Int): Boolean {
        val v = get(x, y)
        // Safe: Empty (0) or Food (1)
        return v == 0 || v == 1
    }

    fun clone(): SnakeGrid {
        val newGrid = SnakeGrid(width, height)
        System.arraycopy(this.cells, 0, newGrid.cells, 0, this.cells.size)
        return newGrid
    }

    companion object {
        fun fromState(
            cols: Int, rows: Int,
            pBody: List<Point>, aBody: List<Point>, foods: List<Point>
        ): SnakeGrid {
            val g = SnakeGrid(cols, rows)

            // Place Food
            foods.forEach { g[it.x, it.y] = 1 }

            pBody.forEachIndexed { i, p -> g[p.x, p.y] = if (i == 0) 4 else 2 }
            aBody.forEachIndexed { i, p -> g[p.x, p.y] = if (i == 0) 4 else 3 }

            return g
        }
    }
}