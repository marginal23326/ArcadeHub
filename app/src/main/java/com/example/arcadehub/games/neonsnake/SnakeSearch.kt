package com.example.arcadehub.games.neonsnake

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SnakeSearch {

    data class SearchResult(val score: Double, val move: GridDir?)
    data class Neighbor(val x: Int, val y: Int, val dir: GridDir)

    fun getSafeNeighbors(grid: SnakeGrid, head: Point, snake: SnakeEntity): MutableList<Neighbor> {
        val moves = ArrayList<Neighbor>(4)
        val bodyLen = snake.body.size

        var tailX = -1; var tailY = -1
        var isTailStacked = false

        if (bodyLen > 1) {
            val tail = snake.body.last()
            val tailPrev = snake.body[bodyLen - 2]
            tailX = tail.x
            tailY = tail.y
            isTailStacked = (tailX == tailPrev.x && tailY == tailPrev.y)
        }

        val dirs = arrayOf(GridDir.DOWN, GridDir.UP, GridDir.LEFT, GridDir.RIGHT)

        for (d in dirs) {
            val nx = head.x + d.x
            val ny = head.y + d.y
            var isSafe = grid.isSafe(nx, ny)

            if (!isSafe && nx == tailX && ny == tailY) {
                val isEatingFood = (grid[nx, ny] == 1)
                if (!isTailStacked && !isEatingFood) {
                    isSafe = true
                }
            }

            if (isSafe) moves.add(Neighbor(nx, ny, d))
        }
        return moves
    }

    fun alphaBeta(
        grid: SnakeGrid,
        state: SnakeHeuristics.State,
        depth: Int,
        alpha: Double,
        beta: Double,
        isMaximizing: Boolean,
        rootDepth: Int = depth,
        currentHash: Long = 0L
    ): SearchResult {

        // 1. Transposition Table Lookup
        if (depth != rootDepth) {
            val ttEntry = SnakeTT.get(currentHash)
            if (ttEntry != null && ttEntry.depth >= depth) {
                var newAlpha = alpha
                var newBeta = beta
                when (ttEntry.flag) {
                    SnakeTT.EXACT -> return SearchResult(ttEntry.score, ttEntry.move)
                    SnakeTT.LOWERBOUND -> newAlpha = max(alpha, ttEntry.score)
                    SnakeTT.UPPERBOUND -> newBeta = min(beta, ttEntry.score)
                }
                if (newAlpha >= newBeta) {
                    return SearchResult(ttEntry.score, ttEntry.move)
                }
            }
        }

        // 2. Terminal Conditions
        if (state.me.body.isEmpty() || state.me.health <= 0)
            return SearchResult(SnakeConfig.Scores.LOSS - depth, null)
        if (state.enemy.body.isEmpty() || state.enemy.health <= 0)
            return SearchResult(SnakeConfig.Scores.WIN + depth, null)
        if (depth == 0)
            return SearchResult(SnakeHeuristics.evaluate(grid, state), null)

        // 3. Move Generation
        val currentSnake = if (isMaximizing) state.me else state.enemy
        val opponentSnake = if (isMaximizing) state.enemy else state.me
        val head = currentSnake.head()

        val moves = getSafeNeighbors(grid, head, currentSnake)

        if (moves.isEmpty()) {
            val score = if (isMaximizing) SnakeConfig.Scores.LOSS - depth else SnakeConfig.Scores.WIN + depth
            return SearchResult(score, null)
        }

        // 4. Move Sorting
        val ttEntry = SnakeTT.get(currentHash)
        val pvMove = ttEntry?.move

        if (moves.size > 1) {
            val foods = state.foods
            val cx = grid.width / 2.0
            val cy = grid.height / 2.0

            moves.sortWith(compareBy<Neighbor> {
                // Primary sort
                if (pvMove != null) it.dir != pvMove else false
            }.thenBy { neighbor ->
                // Closest distance to any food
                var minFoodDist = 1000
                if (foods.isNotEmpty()) {
                    for (food in foods) {
                        val dist = abs(neighbor.x - food.x) + abs(neighbor.y - food.y)
                        if (dist < minFoodDist) minFoodDist = dist
                    }
                }
                minFoodDist
            }.thenBy { neighbor ->
                // Distance to center as a tie-breaker
                abs(neighbor.x - cx) + abs(neighbor.y - cy)
            })
        }

        var bestMove: GridDir? = moves[0].dir
        var bestScore = if (isMaximizing) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        var currAlpha = alpha
        var currBeta = beta

        // 5. Search Loop
        for (move in moves) {
            var collisionPenalty = 0.0

            if (isMaximizing) {
                val opponentHead = opponentSnake.head()
                val dist = abs(move.x - opponentHead.x) + abs(move.y - opponentHead.y)

                if (dist == 1) {
                    val myLen = currentSnake.body.size
                    val oppLen = opponentSnake.body.size

                    if (oppLen > myLen) collisionPenalty = SnakeConfig.Scores.HEAD_ON_COLLISION
                    else if (oppLen == myLen) collisionPenalty = SnakeConfig.Scores.DRAW
                }
            }

            // --- DO MOVE & INCREMENTAL HASH ---
            val originalHeadVal = grid[move.x, move.y]
            val ateFood = (originalHeadVal == 1)
            var nextHash = currentHash

            val oldHealth = if (isMaximizing) state.me.health else state.enemy.health
            val newHealth = if (ateFood) 100 else oldHealth - 1
            nextHash = SnakeZobrist.xorHealth(nextHash, oldHealth, newHealth, isMaximizing)

            // XOR out what was there (Empty=0, Food=1)
            nextHash = SnakeZobrist.xorPiece(nextHash, move.x, move.y, originalHeadVal)
            // XOR in new head (2=Me, 3=Enemy)
            val myId = if (isMaximizing) 2 else 3
            nextHash = SnakeZobrist.xorPiece(nextHash, move.x, move.y, myId)

            val newHead = Point(move.x, move.y)
            val newCurrentBody = ArrayList<Point>(currentSnake.body.size + 1)
            newCurrentBody.add(newHead)
            newCurrentBody.addAll(currentSnake.body)

            var didModifyTail = false
            var tailX = -1; var tailY = -1
            var originalTailVal = 0

            if (!ateFood) {
                val tail = newCurrentBody.removeAt(newCurrentBody.size - 1)
                if (tail.x != newHead.x || tail.y != newHead.y) {
                    tailX = tail.x; tailY = tail.y
                    originalTailVal = grid[tailX, tailY] // Should be 2 or 3
                    grid[tailX, tailY] = 0
                    didModifyTail = true
                    nextHash = SnakeZobrist.xorPiece(nextHash, tailX, tailY, myId)
                }
            }

            grid[move.x, move.y] = myId

            val nextMe = if (isMaximizing) SnakeEntity(newCurrentBody, move.dir, 0, newHealth) else state.me
            val nextEnemy = if (!isMaximizing) SnakeEntity(newCurrentBody, move.dir, 0, newHealth) else state.enemy
            val nextFoods = if (ateFood) state.foods.filter { it.x != move.x || it.y != move.y } else state.foods
            val nextState = SnakeHeuristics.State(nextMe, nextEnemy, nextFoods, null)

            // Recurse
            val child = alphaBeta(grid, nextState, depth - 1, currAlpha, currBeta, !isMaximizing, rootDepth, nextHash)

            // --- UNDO MOVE (Backtracking) ---
            grid[move.x, move.y] = originalHeadVal
            if (didModifyTail) {
                grid[tailX, tailY] = originalTailVal
            }

            // --- SCORING ---
            var modifiedScore = child.score
            if (isMaximizing && collisionPenalty != 0.0) {
                modifiedScore = min(modifiedScore, collisionPenalty)
            }
            if (ateFood) {
                val DEATH_THRESHOLD = -50_000_000.0
                if (isMaximizing) {
                    if (modifiedScore > DEATH_THRESHOLD) modifiedScore += SnakeConfig.Scores.EAT_REWARD
                } else {
                    if (modifiedScore < -DEATH_THRESHOLD) modifiedScore -= SnakeConfig.Scores.EAT_REWARD
                }
            }

            if (isMaximizing) {
                if (modifiedScore > bestScore) {
                    bestScore = modifiedScore
                    bestMove = move.dir
                }
                currAlpha = max(currAlpha, bestScore)
            } else {
                if (modifiedScore < bestScore) {
                    bestScore = modifiedScore
                    bestMove = move.dir
                }
                currBeta = min(currBeta, bestScore)
            }

            if (currBeta <= currAlpha) break
        }

        // 6. Store in Transposition Table
        val ttFlag = when {
            bestScore <= alpha -> SnakeTT.UPPERBOUND
            bestScore >= beta -> SnakeTT.LOWERBOUND
            else -> SnakeTT.EXACT
        }
        SnakeTT.set(currentHash, depth, bestScore, ttFlag, bestMove)

        return SearchResult(bestScore, bestMove)
    }
}