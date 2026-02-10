package com.example.arcadehub.games.neonsnake

object SnakeBrain {

    fun getSmartMove(
        me: SnakeEntity,
        enemy: SnakeEntity,
        foods: List<Point>,
        cols: Int,
        rows: Int,
        depth: Int
    ): GridDir {

        // 1. Setup Grid
        val grid = SnakeGrid.fromState(cols, rows, me.body, enemy.body, foods)

        // 2. Distance Map
        val distMap = SnakeAlgorithms.getFoodDistanceMap(grid, foods)

        // 3. State
        val state = SnakeHeuristics.State(me, enemy, foods, distMap)

        // 4. AlphaBeta Search
        val result = SnakeSearch.alphaBeta(grid, state, depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true)

        var moveDir = result.move

        // 5. FAILSAFE
        if (moveDir == null) {
            val head = me.head()
            val neighbors = listOf(
                SnakeSearch.Neighbor(head.x, head.y + 1, GridDir.UP),
                SnakeSearch.Neighbor(head.x, head.y - 1, GridDir.DOWN),
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