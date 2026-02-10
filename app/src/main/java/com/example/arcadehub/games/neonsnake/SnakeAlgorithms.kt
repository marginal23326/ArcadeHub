package com.example.arcadehub.games.neonsnake

import java.util.ArrayDeque
import kotlin.math.abs

object SnakeAlgorithms {

    data class FloodFillResult(val count: Int, val minTurnsToClear: Int, val hasFood: Boolean)
    data class VoronoiResult(val myCount: Int, val enemyCount: Int)

    fun manhattan(p1: Point, p2: Point) = abs(p1.x - p2.x) + abs(p1.y - p2.y)

    fun floodFill(grid: SnakeGrid, startX: Int, startY: Int, maxDepth: Int, snakeBody: List<Point>? = null): FloodFillResult {
        val width = grid.width
        val height = grid.height
        val visited = BooleanArray(width * height)
        val queue = ArrayDeque<Point>()

        var count = 0
        var minTurnsToClear = Int.MAX_VALUE
        var hasFood = false

        // Map body for O(1) lookup to calculate turns to clear
        val bodyMap = IntArray(width * height) { -1 }
        snakeBody?.forEachIndexed { i, part ->
            if (part.x in 0 until width && part.y in 0 until height) {
                bodyMap[part.y * width + part.x] = i
            }
        }

        queue.add(Point(startX, startY))
        visited[startY * width + startX] = true
        count++

        while (!queue.isEmpty()) {
            if (count >= maxDepth) return FloodFillResult(count, minTurnsToClear, hasFood)

            val curr = queue.poll()!!

            if (!hasFood && grid[curr.x, curr.y] == 1) {
                hasFood = true
            }

            val neighbors = listOf(
                Point(curr.x, curr.y - 1), Point(curr.x, curr.y + 1),
                Point(curr.x - 1, curr.y), Point(curr.x + 1, curr.y)
            )

            for (n in neighbors) {
                if (n.x in 0 until width && n.y in 0 until height) {
                    val idx = n.y * width + n.x
                    if (visited[idx]) continue

                    if (grid.isSafe(n.x, n.y)) {
                        visited[idx] = true
                        queue.add(n)
                        count++
                    } else if (snakeBody != null) {
                        val bodyIndex = bodyMap[idx]
                        if (bodyIndex != -1) {
                            val turns = snakeBody.size - bodyIndex
                            if (turns < minTurnsToClear) minTurnsToClear = turns
                            visited[idx] = true // Treated as visited boundary
                        }
                    }
                }
            }
        }
        return FloodFillResult(count, minTurnsToClear, hasFood)
    }

    fun computeVoronoi(grid: SnakeGrid, myHead: Point, enemyHead: Point): VoronoiResult {
        val width = grid.width
        val height = grid.height

        val dists = IntArray(width * height) { -1 }
        // 0: None, 1: Me, 2: Enemy, 3: Tie
        val owners = IntArray(width * height) { 0 }

        data class Node(val x: Int, val y: Int, val owner: Int, val dist: Int)
        val queue = ArrayDeque<Node>()

        // Add Me
        val myIdx = myHead.y * width + myHead.x
        if (myIdx in dists.indices) {
            dists[myIdx] = 0
            owners[myIdx] = 1
            queue.add(Node(myHead.x, myHead.y, 1, 0))
        }

        // Add Enemy
        val enIdx = enemyHead.y * width + enemyHead.x
        if (enIdx in dists.indices) {
            dists[enIdx] = 0
            owners[enIdx] = 2
            queue.add(Node(enemyHead.x, enemyHead.y, 2, 0))
        }

        var myCount = 0
        var enemyCount = 0

        while (!queue.isEmpty()) {
            val curr = queue.poll()!!

            val neighbors = listOf(
                Point(curr.x, curr.y - 1), Point(curr.x, curr.y + 1),
                Point(curr.x - 1, curr.y), Point(curr.x + 1, curr.y)
            )

            for (n in neighbors) {
                if (n.x in 0 until width && n.y in 0 until height) {
                    val idx = n.y * width + n.x

                    if (grid.isSafe(n.x, n.y)) {
                        // Case 1: Unvisited
                        if (dists[idx] == -1) {
                            dists[idx] = curr.dist + 1
                            owners[idx] = curr.owner
                            queue.add(Node(n.x, n.y, curr.owner, curr.dist + 1))

                            if (curr.owner == 1) myCount++
                            else if (curr.owner == 2) enemyCount++
                        }
                        // Case 2: Visited by opponent at SAME distance -> Tie
                        else if (dists[idx] == curr.dist + 1 && owners[idx] != curr.owner && owners[idx] != 3) {
                            if (owners[idx] == 1) myCount--
                            else if (owners[idx] == 2) enemyCount--

                            owners[idx] = 3 // Tie
                        }
                    }
                }
            }
        }
        return VoronoiResult(myCount, enemyCount)
    }

    fun getFoodDistanceMap(grid: SnakeGrid, foods: List<Point>): IntArray {
        val width = grid.width
        val height = grid.height
        val distMap = IntArray(width * height) { 1000 }

        data class Node(val x: Int, val y: Int, val d: Int)
        val queue = ArrayDeque<Node>()

        foods.forEach { f ->
            val idx = f.y * width + f.x
            if (idx in distMap.indices) {
                distMap[idx] = 0
                queue.add(Node(f.x, f.y, 0))
            }
        }

        while (!queue.isEmpty()) {
            val curr = queue.poll()!!
            val neighbors = listOf(
                Point(curr.x, curr.y - 1), Point(curr.x, curr.y + 1),
                Point(curr.x - 1, curr.y), Point(curr.x + 1, curr.y)
            )

            for (n in neighbors) {
                if (n.x in 0 until width && n.y in 0 until height) {
                    val idx = n.y * width + n.x
                    if (distMap[idx] == 1000 && grid.isSafe(n.x, n.y)) {
                        distMap[idx] = curr.d + 1
                        queue.add(Node(n.x, n.y, curr.d + 1))
                    }
                }
            }
        }
        return distMap
    }
}