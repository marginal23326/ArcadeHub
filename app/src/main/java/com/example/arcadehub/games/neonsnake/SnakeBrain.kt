package com.example.arcadehub.games.neonsnake

object SnakeBrain {

    fun getSmartMove(
        me: SnakeEntity,
        enemy: SnakeEntity,
        foods: List<Point>,
        cols: Int,
        rows: Int,
        depth: Int,
        mapType: SnakeMapGenerator.MapType
    ): GridDir {

        // 1. Initialize & Clear
        SnakeZobrist.init(cols, rows)
        SnakeTT.clear()

        // 2. Setup Grid with Map Walls
        val grid = SnakeGrid(cols, rows)

        SnakeMapGenerator.applyMap(grid, mapType)

        foods.forEach { grid[it.x, it.y] = 1 }
        me.body.forEach { p -> grid[p.x, p.y] = 2 }
        enemy.body.forEach { p -> grid[p.x, p.y] = 3 }

        // 3. Distance Map
        val distMap = SnakeAlgorithms.getFoodDistanceMap(grid, foods)

        // 4. State
        val state = SnakeHeuristics.State(me, enemy, foods, distMap)

        // 5. Compute Initial Hash
        val initialHash = SnakeZobrist.computeHash(grid, me.health, enemy.health)

        // 6. AlphaBeta Search
        val result = SnakeSearch.alphaBeta(
            grid, state, depth,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
            true, depth, initialHash
        )

        var moveDir = result.move

        // 7. FAILSAFE
        if (moveDir == null) {
            val head = me.head()

            // Android Coords: y+1 is DOWN, y-1 is UP.
            val neighbors = listOf(
                SnakeSearch.Neighbor(head.x, head.y + 1, GridDir.DOWN),
                SnakeSearch.Neighbor(head.x, head.y - 1, GridDir.UP),
                SnakeSearch.Neighbor(head.x - 1, head.y, GridDir.LEFT),
                SnakeSearch.Neighbor(head.x + 1, head.y, GridDir.RIGHT)
            )

            val safeMoves = neighbors
                .filter { grid.isSafe(it.x, it.y) }
                .map {
                    val space = SnakeAlgorithms.floodFill(grid, it.x, it.y, 100).count
                    Pair(it, space)
                }
                .sortedByDescending { it.second }

            moveDir = if (safeMoves.isNotEmpty()) {
                safeMoves[0].first.dir
            } else {
                GridDir.UP // Total death
            }
        }

        return moveDir
    }
}