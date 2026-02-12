package com.example.arcadehub.games.neonsnake

object SnakeMapGenerator {

    enum class MapType {
        EMPTY,
        PILLARS,
        ROOMS,
        CENTER_BOX
    }

    fun applyMap(grid: SnakeGrid, type: MapType) {
        val w = grid.width
        val h = grid.height

        // Clear grid first (set all to 0)
        grid.cells.fill(0)

        when (type) {
            MapType.EMPTY -> { /* Do nothing */ }

            MapType.PILLARS -> {
                // 4 Pillars near the corners
                placeWallRect(grid, 3, 2, 1, 2)
                placeWallRect(grid, w - 4, 2, 1, 2)
                placeWallRect(grid, 3, h - 4, 1, 2)
                placeWallRect(grid, w - 4, h - 4, 1, 2)
            }

            MapType.ROOMS -> {
                // A horizontal wall in middle with a gap
                val midY = h / 2
                for (x in 0 until w) {
                    if (x != w/2 && x != w/2 - 1) {
                        grid[x, midY] = 9 // 9 = Wall
                    }
                }
            }

            MapType.CENTER_BOX -> {
                // A box in the very center
                placeWallRect(grid, w/2 - 2, h/2 - 1, 4, 2)
            }
        }
    }

    private fun placeWallRect(grid: SnakeGrid, x: Int, y: Int, w: Int, h: Int) {
        for (i in 0 until w) {
            for (j in 0 until h) {
                grid[x + i, y + j] = 9 // 9 represents a Wall
            }
        }
    }
}