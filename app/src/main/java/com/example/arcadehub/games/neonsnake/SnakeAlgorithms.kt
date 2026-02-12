package com.example.arcadehub.games.neonsnake

import kotlin.math.abs

object SnakeAlgorithms {

    private var ffVisited: IntArray? = null
    private var ffQueue: IntArray? = null
    private var ffBodyMap: IntArray? = null
    private var ffDirtyIndices: IntArray? = null
    private var ffVisitedGen = 0

    private var vQx: IntArray? = null
    private var vQy: IntArray? = null
    private var vQOwner: IntArray? = null
    private var vQDist: IntArray? = null
    private var vDists: IntArray? = null
    private var vOwners: IntArray? = null
    private var vVisited: IntArray? = null
    private var vSearchId = 0

    private var dmDistMap: IntArray? = null
    private var dmQueue: IntArray? = null

    private fun ensureCacheSize(width: Int, height: Int) {
        val size = width * height

        // FloodFill Cache Check
        if (ffVisited == null || ffVisited!!.size < size) {
            ffVisited = IntArray(size)
            ffQueue = IntArray(size)
            ffBodyMap = IntArray(size) { -1 }
            ffDirtyIndices = IntArray(size)
            ffVisitedGen = 0
        }

        // Voronoi Cache Check
        if (vVisited == null || vVisited!!.size < size) {
            vQx = IntArray(size); vQy = IntArray(size); vQOwner = IntArray(size); vQDist = IntArray(size)
            vDists = IntArray(size); vOwners = IntArray(size); vVisited = IntArray(size)
            vSearchId = 0
        }

        // Distance Map Cache Check
        if (dmDistMap == null || dmDistMap!!.size < size) {
            dmDistMap = IntArray(size)
            dmQueue = IntArray(size)
        }
    }

    data class FloodFillResult(val count: Int, val minTurnsToClear: Int, val hasFood: Boolean)
    data class VoronoiResult(val myCount: Int, val enemyCount: Int)

    fun manhattan(p1: Point, p2: Point) = abs(p1.x - p2.x) + abs(p1.y - p2.y)

    fun floodFill(
        grid: SnakeGrid,
        startX: Int,
        startY: Int,
        maxDepth: Int,
        snakeBody: List<Point>? = null
    ): FloodFillResult {
        ensureCacheSize(grid.width, grid.height)

        val width = grid.width
        val height = grid.height
        val cells = grid.cells

        val visited = ffVisited!!
        val queue = ffQueue!!
        val bodyMap = ffBodyMap!!
        val dirtyIndices = ffDirtyIndices!!

        ffVisitedGen++
        if (ffVisitedGen > 2000000000) {
            visited.fill(0)
            ffVisitedGen = 1
        }

        var dirtyCount = 0
        if (snakeBody != null) {
            val size = snakeBody.size
            for (i in 0 until size) {
                val part = snakeBody[i]
                if (part.x in 0 until width && part.y in 0 until height) {
                    val idx = part.y * width + part.x
                    bodyMap[idx] = i
                    dirtyIndices[dirtyCount++] = idx
                }
            }
        }

        var qHead = 0
        var qTail = 0
        val startIdx = startY * width + startX

        queue[qTail++] = startIdx
        visited[startIdx] = ffVisitedGen

        var count = 1
        var minTurnsToClear = Int.MAX_VALUE
        var hasFood = false

        while (qHead < qTail) {
            if (count >= maxDepth) break

            val currIdx = queue[qHead++]

            if (!hasFood && cells[currIdx] == 1) hasFood = true

            val cx = currIdx % width
            val cy = currIdx / width

            // Inline Neighbor Checks
            // UP
            if (cy > 0) {
                val idx = currIdx - width
                if (visited[idx] != ffVisitedGen) {
                    val valAt = cells[idx]
                    if (valAt == 0 || valAt == 1) {
                        visited[idx] = ffVisitedGen
                        queue[qTail++] = idx
                        count++
                    } else if (snakeBody != null) {
                        val bodyIndex = bodyMap[idx]
                        if (bodyIndex != -1) {
                            val turns = snakeBody.size - bodyIndex
                            if (turns < minTurnsToClear) minTurnsToClear = turns
                            visited[idx] = ffVisitedGen
                        }
                    }
                }
            }
            // DOWN
            if (cy < height - 1) {
                val idx = currIdx + width
                if (visited[idx] != ffVisitedGen) {
                    val valAt = cells[idx]
                    if (valAt == 0 || valAt == 1) {
                        visited[idx] = ffVisitedGen
                        queue[qTail++] = idx
                        count++
                    } else if (snakeBody != null) {
                        val bodyIndex = bodyMap[idx]
                        if (bodyIndex != -1) {
                            val turns = snakeBody.size - bodyIndex
                            if (turns < minTurnsToClear) minTurnsToClear = turns
                            visited[idx] = ffVisitedGen
                        }
                    }
                }
            }
            // LEFT
            if (cx > 0) {
                val idx = currIdx - 1
                if (visited[idx] != ffVisitedGen) {
                    val valAt = cells[idx]
                    if (valAt == 0 || valAt == 1) {
                        visited[idx] = ffVisitedGen
                        queue[qTail++] = idx
                        count++
                    } else if (snakeBody != null) {
                        val bodyIndex = bodyMap[idx]
                        if (bodyIndex != -1) {
                            val turns = snakeBody.size - bodyIndex
                            if (turns < minTurnsToClear) minTurnsToClear = turns
                            visited[idx] = ffVisitedGen
                        }
                    }
                }
            }
            // RIGHT
            if (cx < width - 1) {
                val idx = currIdx + 1
                if (visited[idx] != ffVisitedGen) {
                    val valAt = cells[idx]
                    if (valAt == 0 || valAt == 1) {
                        visited[idx] = ffVisitedGen
                        queue[qTail++] = idx
                        count++
                    } else if (snakeBody != null) {
                        val bodyIndex = bodyMap[idx]
                        if (bodyIndex != -1) {
                            val turns = snakeBody.size - bodyIndex
                            if (turns < minTurnsToClear) minTurnsToClear = turns
                            visited[idx] = ffVisitedGen
                        }
                    }
                }
            }
        }

        // Cleanup body map
        for (i in 0 until dirtyCount) {
            bodyMap[dirtyIndices[i]] = -1
        }

        return FloodFillResult(count, minTurnsToClear, hasFood)
    }

    fun computeVoronoi(grid: SnakeGrid, myHead: Point, enemyHead: Point): VoronoiResult {
        ensureCacheSize(grid.width, grid.height)
        val width = grid.width
        val height = grid.height
        val cells = grid.cells

        val visited = vVisited!!
        val dists = vDists!!
        val owners = vOwners!!
        val queueX = vQx!!; val queueY = vQy!!; val queueOwner = vQOwner!!; val queueDist = vQDist!!

        vSearchId++
        if (vSearchId > 2000000000) {
            visited.fill(0)
            vSearchId = 1
        }

        var head = 0
        var tail = 0

        val mIdx = myHead.y * width + myHead.x
        dists[mIdx] = 0
        owners[mIdx] = 1
        visited[mIdx] = vSearchId
        queueX[tail] = myHead.x; queueY[tail] = myHead.y; queueOwner[tail] = 1; queueDist[tail] = 0
        tail++

        val eIdx = enemyHead.y * width + enemyHead.x
        dists[eIdx] = 0
        owners[eIdx] = 2
        visited[eIdx] = vSearchId
        queueX[tail] = enemyHead.x; queueY[tail] = enemyHead.y; queueOwner[tail] = 2; queueDist[tail] = 0
        tail++

        var myCount = 0
        var enemyCount = 0

        while (head < tail) {
            val cx = queueX[head]; val cy = queueY[head]; val co = queueOwner[head]; val cd = queueDist[head]
            head++

            val nd = cd + 1
            val currIdx = cy * width + cx

            // 1. UP
            if (cy > 0) {
                val idx = currIdx - width
                val v = cells[idx]
                if (v == 0 || v == 1) {
                    if (visited[idx] != vSearchId) {
                        visited[idx] = vSearchId; dists[idx] = nd; owners[idx] = co
                        queueX[tail] = cx; queueY[tail] = cy - 1; queueOwner[tail] = co; queueDist[tail] = nd; tail++
                        if (co == 1) myCount++ else enemyCount++
                    } else if (dists[idx] == nd && owners[idx] != co && owners[idx] != 3) {
                        if (owners[idx] == 1) myCount-- else if (owners[idx] == 2) enemyCount--
                        owners[idx] = 3
                    }
                }
            }
            // 2. DOWN
            if (cy < height - 1) {
                val idx = currIdx + width
                val v = cells[idx]
                if (v == 0 || v == 1) {
                    if (visited[idx] != vSearchId) {
                        visited[idx] = vSearchId; dists[idx] = nd; owners[idx] = co
                        queueX[tail] = cx; queueY[tail] = cy + 1; queueOwner[tail] = co; queueDist[tail] = nd; tail++
                        if (co == 1) myCount++ else enemyCount++
                    } else if (dists[idx] == nd && owners[idx] != co && owners[idx] != 3) {
                        if (owners[idx] == 1) myCount-- else if (owners[idx] == 2) enemyCount--
                        owners[idx] = 3
                    }
                }
            }
            // 3. LEFT
            if (cx > 0) {
                val idx = currIdx - 1
                val v = cells[idx]
                if (v == 0 || v == 1) {
                    if (visited[idx] != vSearchId) {
                        visited[idx] = vSearchId; dists[idx] = nd; owners[idx] = co
                        queueX[tail] = cx - 1; queueY[tail] = cy; queueOwner[tail] = co; queueDist[tail] = nd; tail++
                        if (co == 1) myCount++ else enemyCount++
                    } else if (dists[idx] == nd && owners[idx] != co && owners[idx] != 3) {
                        if (owners[idx] == 1) myCount-- else if (owners[idx] == 2) enemyCount--
                        owners[idx] = 3
                    }
                }
            }
            // 4. RIGHT
            if (cx < width - 1) {
                val idx = currIdx + 1
                val v = cells[idx]
                if (v == 0 || v == 1) {
                    if (visited[idx] != vSearchId) {
                        visited[idx] = vSearchId; dists[idx] = nd; owners[idx] = co
                        queueX[tail] = cx + 1; queueY[tail] = cy; queueOwner[tail] = co; queueDist[tail] = nd; tail++
                        if (co == 1) myCount++ else enemyCount++
                    } else if (dists[idx] == nd && owners[idx] != co && owners[idx] != 3) {
                        if (owners[idx] == 1) myCount-- else if (owners[idx] == 2) enemyCount--
                        owners[idx] = 3
                    }
                }
            }
        }
        return VoronoiResult(myCount, enemyCount)
    }

    fun getFoodDistanceMap(grid: SnakeGrid, foods: List<Point>): IntArray {
        ensureCacheSize(grid.width, grid.height)
        val width = grid.width
        val height = grid.height
        val totalSize = width * height
        val cells = grid.cells

        val distMap = dmDistMap!!
        val queue = dmQueue!!

        distMap.fill(1000, 0, totalSize)

        var head = 0
        var tail = 0

        for (f in foods) {
            val idx = f.y * width + f.x
            if (idx in 0 until totalSize) {
                distMap[idx] = 0
                queue[tail++] = idx
            }
        }

        while (head < tail) {
            val currIdx = queue[head++]
            val nextDist = distMap[currIdx] + 1

            val cx = currIdx % width
            val cy = currIdx / width

            // UP
            if (cy > 0) {
                val idx = currIdx - width
                if (distMap[idx] == 1000) {
                    val v = cells[idx]
                    if (v == 0 || v == 1) {
                        distMap[idx] = nextDist
                        queue[tail++] = idx
                    }
                }
            }
            // DOWN
            if (cy < height - 1) {
                val idx = currIdx + width
                if (distMap[idx] == 1000) {
                    val v = cells[idx]
                    if (v == 0 || v == 1) {
                        distMap[idx] = nextDist
                        queue[tail++] = idx
                    }
                }
            }
            // LEFT
            if (cx > 0) {
                val idx = currIdx - 1
                if (distMap[idx] == 1000) {
                    val v = cells[idx]
                    if (v == 0 || v == 1) {
                        distMap[idx] = nextDist
                        queue[tail++] = idx
                    }
                }
            }
            // RIGHT
            if (cx < width - 1) {
                val idx = currIdx + 1
                if (distMap[idx] == 1000) {
                    val v = cells[idx]
                    if (v == 0 || v == 1) {
                        distMap[idx] = nextDist
                        queue[tail++] = idx
                    }
                }
            }
        }
        return distMap.copyOf(totalSize)
    }
}