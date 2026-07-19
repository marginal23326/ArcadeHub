package com.example.arcadehub.games.neonsnake.ai

object Pathfinding {
    const val UNREACHABLE = 1000

    fun foodDistanceMap(grid: AiGrid, buffers: SearchBuffers): IntArray {
        val distMap = IntArray(buffers.gridSize) { UNREACHABLE }

        val front = buffers.pfFront
        front.copyFrom(grid.food)
        val visited = buffers.pfVisited
        visited.copyFrom(front)
        val safeCells = buffers.pfSafeCells
        grid.safeCellsInto(safeCells, buffers.pfOccupiedScratch)

        val popScratch = buffers.pfPopScratch
        popScratch.copyFrom(front)
        var idx = popScratch.popFirst()
        while (idx != -1) {
            distMap[idx] = 0
            idx = popScratch.popFirst()
        }

        val expanded = buffers.pfExpanded
        val scratchA = buffers.pfShiftScratchA
        val scratchB = buffers.pfShiftScratchB

        var dist = 0
        while (front.any()) {
            dist++
            grid.topology.expandNeighbors(front, expanded, scratchA, scratchB)
            expanded.andAssign(safeCells)
            expanded.andNotAssign(visited)

            front.copyFrom(expanded)
            visited.orAssign(front)

            popScratch.copyFrom(front)
            idx = popScratch.popFirst()
            while (idx != -1) {
                distMap[idx] = dist
                idx = popScratch.popFirst()
            }
        }

        return distMap
    }
}
