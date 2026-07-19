package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.GridDir
import kotlin.math.abs

private val ALL_DIRS = arrayOf(GridDir.UP, GridDir.DOWN, GridDir.LEFT, GridDir.RIGHT)

object MoveGeneration {
    fun getSafeNeighbors(grid: AiGrid, me: SearchAgent, enemy: SearchAgent, out: MoveList) {
        out.clear()
        val myBody = me.body
        if (myBody.isEmpty()) return

        val oppBody = enemy.body
        val headX = myBody.headX()
        val headY = myBody.headY()

        var myTailX = -1
        var myTailY = -1
        var hasMyTail = false
        var myTailStacked = false
        if (myBody.len > 1) {
            myTailX = myBody.lastX()
            myTailY = myBody.lastY()
            val prevX = myBody.x(myBody.len - 2)
            val prevY = myBody.y(myBody.len - 2)
            hasMyTail = true
            myTailStacked = myTailX == prevX && myTailY == prevY
        }

        var oppTailX = -1
        var oppTailY = -1
        var hasOppTail = false
        var oppTailStacked = false
        var enemyCanEat = false
        if (oppBody.len > 1) {
            oppTailX = oppBody.lastX()
            oppTailY = oppBody.lastY()
            val prevX = oppBody.x(oppBody.len - 2)
            val prevY = oppBody.y(oppBody.len - 2)
            hasOppTail = true
            oppTailStacked = oppTailX == prevX && oppTailY == prevY

            val ehX = oppBody.headX()
            val ehY = oppBody.headY()
            enemyCanEat = isFoodAt(grid, ehX, ehY) ||
                isFoodAt(grid, ehX, ehY + 1) || isFoodAt(grid, ehX, ehY - 1) ||
                isFoodAt(grid, ehX - 1, ehY) || isFoodAt(grid, ehX + 1, ehY)
        }

        for (dir in ALL_DIRS) {
            val nx = headX + dir.x
            val ny = headY + dir.y
            var isSafe = false
            if (grid.inBounds(nx, ny)) {
                val idx = grid.idx(nx, ny)
                isSafe = !grid.myBody[idx] && !grid.enemyBody[idx] && !grid.walls[idx]
            }

            if (!isSafe) {
                if (hasMyTail && nx == myTailX && ny == myTailY && !myTailStacked) {
                    isSafe = true
                }
                if (!isSafe && hasOppTail && nx == oppTailX && ny == oppTailY && !oppTailStacked && !enemyCanEat) {
                    isSafe = true
                }
            }

            if (isSafe) {
                out.push(nx, ny, directionIndex(dir))
            }
        }
    }

    private fun isFoodAt(grid: AiGrid, x: Int, y: Int): Boolean {
        if (!grid.inBounds(x, y)) return false
        return grid.food[grid.idx(x, y)]
    }

    fun rootTieBreaker(me: SearchAgent, enemy: SearchAgent, cols: Int, rows: Int, mvX: Int, mvY: Int): Int {
        if (me.body.isEmpty()) return 0

        val myLen = me.body.len
        val enemyLen = enemy.body.len

        var moveIntoEnemyTailPenalty = 0
        if (!enemy.body.isEmpty()) {
            if (mvX == enemy.body.lastX() && mvY == enemy.body.lastY()) moveIntoEnemyTailPenalty = 5
        }

        var headContactBias = 0
        if (!enemy.body.isEmpty()) {
            val ehX = enemy.body.headX()
            val ehY = enemy.body.headY()
            val dist = abs(mvX - ehX) + abs(mvY - ehY)
            if (dist == 1) {
                headContactBias += when {
                    myLen > enemyLen -> 20
                    myLen < enemyLen -> -10000
                    else -> -5000
                }
            }
        }

        var tailBias = 0
        if (cols > 0 && rows > 0 && myLen >= 20 && enemyLen >= 20) {
            val totalLen = myLen + enemyLen
            val totalArea = cols * rows
            if ((totalLen * 100) / totalArea >= 40) {
                val tailDist = abs(mvX - me.body.lastX()) + abs(mvY - me.body.lastY())
                tailBias = -(tailDist * 10)
            }
        }

        return tailBias + headContactBias - moveIntoEnemyTailPenalty
    }

    fun shouldExtendLeaf(grid: AiGrid, me: SearchAgent, enemy: SearchAgent, cfg: AiConfig, buffers: SearchBuffers): Boolean {
        val myLen = me.body.len
        val enemyLen = enemy.body.len
        if (myLen < 20 || enemyLen < 20) return false

        val totalLen = myLen + enemyLen
        val totalArea = grid.width * grid.height
        if ((totalLen * 100) / totalArea < cfg.denseTailRaceOccupancyPercent) return false

        getSafeNeighbors(grid, me, enemy, buffers.leafCheckMoveListA)
        if (buffers.leafCheckMoveListA.count <= 2) return true

        getSafeNeighbors(grid, enemy, me, buffers.leafCheckMoveListB)
        return buffers.leafCheckMoveListB.count <= 2
    }
}
