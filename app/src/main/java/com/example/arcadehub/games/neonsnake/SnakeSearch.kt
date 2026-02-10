package com.example.arcadehub.games.neonsnake

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SnakeSearch {

    data class SearchResult(val score: Double, val move: GridDir?)
    data class Neighbor(val x: Int, val y: Int, val dir: GridDir)

    fun getSafeNeighbors(grid: SnakeGrid, head: Point, snake: SnakeEntity): List<Neighbor> {
        val moves = ArrayList<Neighbor>()

        // Android GridDir (Y is -1 for Up, but logic remains relative)
        val dirs = listOf(GridDir.UP, GridDir.DOWN, GridDir.LEFT, GridDir.RIGHT)

        var tailKey = ""
        var isTailStacked = false

        if (snake.body.size > 1) {
            val t = snake.body.last()
            val tPrev = snake.body[snake.body.size - 2]
            tailKey = "${t.x},${t.y}"
            if (t.x == tPrev.x && t.y == tPrev.y) {
                isTailStacked = true
            }
        }

        for (d in dirs) {
            val nx = head.x + d.x
            val ny = head.y + d.y

            // 1. Basic Safety
            var isSafe = grid.isSafe(nx, ny)

            // 2. Tail Chasing Override
            if (!isSafe && tailKey.isNotEmpty() && "$nx,$ny" == tailKey) {
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
        isMaximizing: Boolean
    ): SearchResult {

        // Terminal checks
        if (state.me.body.isEmpty() || state.me.health <= 0)
            return SearchResult(SnakeConfig.Scores.LOSS - depth, null)
        if (state.enemy.body.isEmpty() || state.enemy.health <= 0)
            return SearchResult(SnakeConfig.Scores.WIN + depth, null)
        if (depth == 0)
            return SearchResult(SnakeHeuristics.evaluate(grid, state), null)

        val currentSnake = if (isMaximizing) state.me else state.enemy
        val opponentSnake = if (isMaximizing) state.enemy else state.me
        val head = currentSnake.head()

        val moves = getSafeNeighbors(grid, head, currentSnake)

        if (moves.isEmpty()) {
            val score = if (isMaximizing) SnakeConfig.Scores.LOSS - depth else SnakeConfig.Scores.WIN + depth
            return SearchResult(score, null)
        }

        var bestMove: GridDir? = moves[0].dir
        var bestScore = if (isMaximizing) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY

        var currAlpha = alpha
        var currBeta = beta

        for (move in moves) {
            var collisionPenalty = 0.0

            // Head-to-head risk check
            if (isMaximizing) {
                val opponentHead = opponentSnake.head()
                val dist = abs(move.x - opponentHead.x) + abs(move.y - opponentHead.y)

                if (dist == 1) {
                    val myLen = currentSnake.body.size
                    val oppLen = opponentSnake.body.size

                    if (oppLen > myLen) {
                        collisionPenalty = SnakeConfig.Scores.HEAD_ON_COLLISION
                    } else if (oppLen == myLen) {
                        collisionPenalty = SnakeConfig.Scores.DRAW
                    }
                }
            }

            // Simulate Move
            val newGrid = grid.clone()
            val newHead = Point(move.x, move.y)

            // Construct new body
            val newCurrentBody = ArrayList<Point>()
            newCurrentBody.add(newHead)
            newCurrentBody.addAll(currentSnake.body)

            val ateFood = grid[move.x, move.y] == 1

            if (!ateFood) {
                val tail = newCurrentBody.removeAt(newCurrentBody.size - 1)
                // Remove tail from grid unless new head is on tail
                if (tail.x != newHead.x || tail.y != newHead.y) {
                    newGrid[tail.x, tail.y] = 0
                }
            }

            // Mark new head (2 for Me, 3 for Enemy)
            newGrid[move.x, move.y] = if (isMaximizing) 2 else 3

            // Next State
            val nextMe = if (isMaximizing)
                SnakeEntity(newCurrentBody, move.dir, 0, if(ateFood) 100 else state.me.health - 1)
            else state.me

            val nextEnemy = if (!isMaximizing)
                SnakeEntity(newCurrentBody, move.dir, 0, if(ateFood) 100 else state.enemy.health - 1)
            else state.enemy

            val nextFoods = if (ateFood) state.foods.filter { it.x != move.x || it.y != move.y } else state.foods

            val nextState = SnakeHeuristics.State(nextMe, nextEnemy, nextFoods, state.distMap)

            // Recurse
            val child = alphaBeta(newGrid, nextState, depth - 1, currAlpha, currBeta, !isMaximizing)

            val rawRecursionScore = child.score
            var modifiedScore = rawRecursionScore

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

        return SearchResult(bestScore, bestMove)
    }
}