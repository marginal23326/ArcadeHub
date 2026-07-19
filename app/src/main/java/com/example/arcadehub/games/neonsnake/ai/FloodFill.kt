package com.example.arcadehub.games.neonsnake.ai

data class FloodFillResult(val count: Int, val minTurnsToClear: Int, val hasFood: Boolean)

const val TURNS_NEVER_CLEARS = Int.MAX_VALUE

object FloodFill {
    fun run(
        grid: AiGrid,
        startX: Int,
        startY: Int,
        maxDepth: Int,
        myBody: FastBody?,
        enemyBody: FastBody?,
        buffers: SearchBuffers
    ): FloodFillResult {
        if (!grid.inBounds(startX, startY)) {
            return FloodFillResult(0, TURNS_NEVER_CLEARS, false)
        }

        val front = buffers.ffFront
        val visited = buffers.ffVisited
        front.clear()
        front.set(grid.idx(startX, startY))
        visited.copyFrom(front)

        val safeCells = buffers.ffSafeCells
        grid.safeCellsInto(safeCells, buffers.ffOccupiedScratch)
        val foodCells = grid.food

        val vanishMap = buffers.vanishMap
        java.util.Arrays.fill(vanishMap, 0, buffers.gridSize, 0)

        val myMask = buffers.ffMyMask
        myMask.clear()
        if (myBody != null) {
            val len = myBody.len
            for (i in 0 until len) {
                val x = myBody.x(i)
                val y = myBody.y(i)
                if (grid.inBounds(x, y)) {
                    val idx = grid.idx(x, y)
                    myMask.set(idx)
                    vanishMap[idx] = len - i
                }
            }
        }

        val enMask = buffers.ffEnMask
        enMask.clear()
        if (enemyBody != null) {
            val len = enemyBody.len
            for (i in 0 until len) {
                val x = enemyBody.x(i)
                val y = enemyBody.y(i)
                if (grid.inBounds(x, y)) {
                    val idx = grid.idx(x, y)
                    enMask.set(idx)
                    val v = len - i
                    if (v > vanishMap[idx]) vanishMap[idx] = v
                }
            }
        }

        val bodyMask = buffers.ffBodyMask
        bodyMask.setToOr(myMask, enMask)

        val expanded = buffers.ffExpanded
        val hits = buffers.ffHits
        val scratchA = buffers.ffShiftScratchA
        val scratchB = buffers.ffShiftScratchB

        var count = 1
        var minTurnsToClear = TURNS_NEVER_CLEARS
        var hasFood = false

        var depth = 1
        while (depth <= maxDepth) {
            if (!hasFood) {
                scratchA.setToAnd(visited, foodCells)
                if (scratchA.any()) hasFood = true
            }

            grid.topology.expandNeighbors(front, expanded, scratchA, scratchB)
            expanded.andNotAssign(visited)

            hits.setToAnd(expanded, bodyMask)
            var hitIdx = hits.popFirst()
            while (hitIdx != -1) {
                val escapeTime = maxOf(depth, vanishMap[hitIdx])
                if (escapeTime < minTurnsToClear) minTurnsToClear = escapeTime
                hitIdx = hits.popFirst()
            }

            front.setToAnd(expanded, safeCells)
            if (front.isEmpty()) break

            visited.orAssign(front)
            count += front.countOnes()

            depth++
        }

        return FloodFillResult(count, minTurnsToClear, hasFood)
    }
}
