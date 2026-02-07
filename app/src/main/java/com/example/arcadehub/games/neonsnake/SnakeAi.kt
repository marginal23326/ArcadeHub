package com.example.arcadehub.games.neonsnake

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min

object SnakeAi {

    val DIRS = listOf(
        GridDir.UP to Point(0, -1),
        GridDir.DOWN to Point(0, 1),
        GridDir.LEFT to Point(-1, 0),
        GridDir.RIGHT to Point(1, 0)
    )

    data class AiResult(val move: GridDir?, val score: Double)

    // --- MAIN ENTRY POINT ---
    fun getSmartMove(
        me: SnakeEntity,
        enemy: SnakeEntity,
        foods: List<Point>
    ): GridDir {
        // 1. Setup State
        val grid = LogicGrid.fromState(me.body, enemy.body, foods)

        // 2. Pre-calculate Distances
        val distMap = getFoodDistanceMap(grid, foods)

        // 3. Alpha-Beta Search
        val result = alphaBeta(
            grid,
            me, enemy, foods, distMap,
            SnakeConfig.MAX_SEARCH_DEPTH,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            true
        )

        // 4. Failsafe
        if (result.move == null) {
            val head = me.head()
            val safeMoves = DIRS.map { (dir, d) ->
                val nx = head.x + d.x
                val ny = head.y + d.y
                Triple(dir, nx, ny)
            }.filter { (_, nx, ny) ->
                grid.isSafe(nx, ny)
            }.map { (dir, nx, ny) ->
                dir to floodFill(grid, nx, ny, 100)
            }.sortedByDescending { it.second }

            if (safeMoves.isNotEmpty()) return safeMoves[0].first
            return GridDir.UP // Death is inevitable
        }

        return result.move
    }

    // --- SEARCH ---
    private fun alphaBeta(
        grid: LogicGrid,
        me: SnakeEntity, enemy: SnakeEntity, foods: List<Point>,
        distMap: IntArray,
        depth: Int,
        alpha: Double, beta: Double,
        isMaximizing: Boolean
    ): AiResult {
        if (me.health <= 0 || me.body.isEmpty()) return AiResult(null, SnakeConfig.Scores.LOSS - depth)
        if (enemy.health <= 0 || enemy.body.isEmpty()) return AiResult(null, SnakeConfig.Scores.WIN + depth)
        if (depth == 0) return AiResult(null, evaluate(grid, me, enemy, foods, distMap))

        val currentSnake = if (isMaximizing) me else enemy
        val opponentSnake = if (isMaximizing) enemy else me
        val head = currentSnake.head()

        val validMoves = getSafeNeighbors(grid, head, currentSnake)

        if (validMoves.isEmpty()) {
            val score = if (isMaximizing) SnakeConfig.Scores.LOSS - depth else SnakeConfig.Scores.WIN + depth
            return AiResult(null, score)
        }

        var bestMove: GridDir? = validMoves[0].first
        var bestScore = if (isMaximizing) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        var currAlpha = alpha
        var currBeta = beta

        for ((dir, pos) in validMoves) {
            var collisionPenalty = 0.0

            // Head-on collision check
            if (isMaximizing) {
                val oppHead = opponentSnake.head()
                val dist = abs(pos.x - oppHead.x) + abs(pos.y - oppHead.y)
                if (dist == 1 && opponentSnake.body.size >= currentSnake.body.size) {
                    collisionPenalty = SnakeConfig.Scores.HEAD_ON_COLLISION
                }
            }

            // Simulate Move
            val newGrid = grid.clone()

            val ateFood = grid[pos.x, pos.y] == 1

            // Construct new body for simulation
            val newBody = ArrayList<Point>()
            newBody.add(pos)
            newBody.addAll(currentSnake.body)

            if (!ateFood) {
                val tail = newBody.removeAt(newBody.size - 1)
                // Clear tail from grid IF it's not the new head (loop logic)
                if (tail != pos) newGrid[tail.x, tail.y] = 0
            }

            // Set new head on grid
            newGrid[pos.x, pos.y] = if (isMaximizing) 2 else 3

            // Create Next State Entities
            val nextMe = if (isMaximizing)
                SnakeEntity(newBody, dir, 0, if(ateFood) 100 else me.health - 1)
            else me

            val nextEnemy = if (!isMaximizing)
                SnakeEntity(newBody, dir, 0, if(ateFood) 100 else enemy.health - 1)
            else enemy

            val nextFoods = if (ateFood) foods.filter { it != pos } else foods

            // Recurse
            val result = alphaBeta(newGrid, nextMe, nextEnemy, nextFoods, distMap, depth - 1, currAlpha, currBeta, !isMaximizing)

            var modifiedScore = result.score
            if (isMaximizing) {
                if (collisionPenalty != 0.0) modifiedScore = min(modifiedScore, collisionPenalty)
                if (ateFood) modifiedScore += SnakeConfig.Scores.EAT_REWARD

                if (modifiedScore > bestScore) {
                    bestScore = modifiedScore
                    bestMove = dir
                }
                currAlpha = max(currAlpha, bestScore)
            } else {
                if (ateFood) modifiedScore -= SnakeConfig.Scores.EAT_REWARD

                if (modifiedScore < bestScore) {
                    bestScore = modifiedScore
                    bestMove = dir
                }
                currBeta = min(currBeta, bestScore)
            }

            if (currBeta <= currAlpha) break
        }

        return AiResult(bestMove, bestScore)
    }

    private fun getSafeNeighbors(grid: LogicGrid, head: Point, snake: SnakeEntity): List<Pair<GridDir, Point>> {
        val moves = ArrayList<Pair<GridDir, Point>>()

        // Tail chasing logic
        val tail = snake.body.last()
        val isTailStacked = snake.body.size > 1 && snake.body[snake.body.size-2] == tail

        for ((dir, d) in DIRS) {
            val nx = head.x + d.x
            val ny = head.y + d.y

            var isSafe = grid.isSafe(nx, ny)

            // Tail override
            if (!isSafe && nx == tail.x && ny == tail.y) {
                // If we are about to eat food, tail doesn't move, so it's NOT safe
                val isEating = grid[nx, ny] == 1
                if (!isTailStacked && !isEating) isSafe = true
            }

            if (isSafe) moves.add(dir to Point(nx, ny))
        }
        return moves
    }

    // --- HEURISTICS ---
    private fun evaluate(
        grid: LogicGrid,
        me: SnakeEntity, enemy: SnakeEntity,
        foods: List<Point>, distMap: IntArray
    ): Double {
        var score = 0.0

        // 1. Length
        score += me.body.size * SnakeConfig.Scores.LENGTH

        // 2. My Space (FloodFill)
        val myHead = me.head()
        val mySpace = floodFill(grid, myHead.x, myHead.y, max(50, me.body.size * 2))

        if (mySpace < me.body.size) {
            score += SnakeConfig.Scores.TRAP_DANGER * (10.0 / (mySpace + 1))
        } else {
            score += mySpace * 5
        }

        // 3. Enemy Space (Control)
        val enemyHead = enemy.head()
        val enemySpace = floodFill(grid, enemyHead.x, enemyHead.y, 100)

        if (enemySpace < enemy.body.size) {
            score += 200_000.0 // Enemy Trapped Bonus
        } else {
            score -= (enemySpace * 100.0) // Squeeze them
        }

        // 4. Kill Pressure
        val distToOpp = manhattan(myHead, enemyHead)
        if (distToOpp == 1 && me.body.size > enemy.body.size) {
            score += 150_000.0
        }

        // 5. Food (The "Rope" Formula)
        if (foods.isNotEmpty()) {
            var closestDist = 9999
            // Use pre-calculated map if valid for head pos, otherwise manual manhattan
            if (myHead.x in 0 until grid.width && myHead.y in 0 until grid.height) {
                val idx = myHead.y * grid.width + myHead.x
                val mapDist = distMap[idx]
                if (mapDist < 1000) closestDist = mapDist
            }

            if (closestDist == 9999) {
                // Fallback if map failed or unreachable
                foods.forEach { closestDist = min(closestDist, manhattan(myHead, it)) }
            }

            if (closestDist < 1000) {
                val rope = me.health - closestDist
                // Curve Formula: Intensity * atan(rope / exponent)
                val curve = SnakeConfig.Scores.FOOD_CURVE_INTENSITY *
                        atan(rope / SnakeConfig.Scores.FOOD_CURVE_EXPONENT)
                score += curve
            } else {
                score += SnakeConfig.Scores.LOSS / 2
            }
        }

        // 6. Aggression
        if (me.body.size > enemy.body.size + 1) {
            score -= (distToOpp * SnakeConfig.Scores.AGGRESSION)
        }

        return score
    }

    // --- PATHFINDING ---
    private fun getFoodDistanceMap(grid: LogicGrid, foods: List<Point>): IntArray {
        val distMap = IntArray(grid.width * grid.height) { 1000 }
        val queue = ArrayDeque<Triple<Int, Int, Int>>()

        foods.forEach {
            val idx = it.y * grid.width + it.x
            if (idx in distMap.indices) {
                distMap[idx] = 0
                queue.add(Triple(it.x, it.y, 0))
            }
        }

        while (queue.isNotEmpty()) {
            val (cx, cy, d) = queue.removeFirst()

            val neighbors = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
            for ((dx, dy) in neighbors) {
                val nx = cx + dx
                val ny = cy + dy

                if (nx in 0 until grid.width && ny in 0 until grid.height) {
                    val idx = ny * grid.width + nx
                    // If not visited AND safe
                    if (distMap[idx] == 1000 && grid.isSafe(nx, ny)) {
                        distMap[idx] = d + 1
                        queue.add(Triple(nx, ny, d + 1))
                    }
                }
            }
        }
        return distMap
    }

    // --- FLOODFILL ---
    private fun floodFill(grid: LogicGrid, sx: Int, sy: Int, maxDepth: Int): Int {
        val visited = BooleanArray(grid.width * grid.height)
        val queue = ArrayDeque<Point>()

        queue.add(Point(sx, sy))
        visited[sy * grid.width + sx] = true
        var count = 0

        // Include start node in count
        count++

        while(queue.isNotEmpty()) {
            if (count >= maxDepth) return count
            val (cx, cy) = queue.removeFirst()

            val neighbors = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
            for ((dx, dy) in neighbors) {
                val nx = cx + dx
                val ny = cy + dy

                if (nx in 0 until grid.width && ny in 0 until grid.height) {
                    val idx = ny * grid.width + nx
                    if (!visited[idx] && grid.isSafe(nx, ny)) {
                        visited[idx] = true
                        queue.add(Point(nx, ny))
                        count++
                    }
                }
            }
        }
        return count
    }

    private fun manhattan(p1: Point, p2: Point) = abs(p1.x - p2.x) + abs(p1.y - p2.y)
}