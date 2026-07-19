package com.example.arcadehub.games.neonsnake.ai

data class VoronoiResult(val myCount: Int, val enemyCount: Int)

object Voronoi {
    fun compute(
        grid: AiGrid,
        myHeadX: Int,
        myHeadY: Int,
        enemyHeadX: Int,
        enemyHeadY: Int,
        buffers: SearchBuffers
    ): VoronoiResult {
        val myFront = buffers.vMyFront
        val enFront = buffers.vEnFront
        myFront.clear()
        myFront.set(grid.idx(myHeadX, myHeadY))
        enFront.clear()
        enFront.set(grid.idx(enemyHeadX, enemyHeadY))

        val myTerritory = buffers.vMyTerritory
        val enTerritory = buffers.vEnTerritory
        myTerritory.clear()
        enTerritory.clear()

        val notSafe = buffers.vShiftScratchA
        grid.safeCellsInto(notSafe, buffers.vOccupiedScratch)
        notSafe.invertAssign()

        val visited = buffers.vVisited
        visited.setToOr(myFront, enFront)
        visited.orAssign(notSafe)

        val ties = buffers.vTies
        val active = buffers.vActive
        val unvisited = buffers.vUnvisited
        val newMyFront = buffers.vShiftScratchC
        val newEnFront = buffers.vShiftScratchD
        val expandScratchA = buffers.vShiftScratchA
        val expandScratchB = buffers.vShiftScratchB

        while (true) {
            unvisited.copyFrom(visited)
            unvisited.invertAssign()

            grid.topology.expandNeighbors(myFront, newMyFront, expandScratchA, expandScratchB)
            newMyFront.andAssign(unvisited)
            grid.topology.expandNeighbors(enFront, newEnFront, expandScratchA, expandScratchB)
            newEnFront.andAssign(unvisited)

            ties.setToAnd(newMyFront, newEnFront)
            newMyFront.xorAssign(ties)
            newEnFront.xorAssign(ties)

            active.setToOr(newMyFront, newEnFront)
            if (active.isEmpty()) break

            myTerritory.orAssign(newMyFront)
            enTerritory.orAssign(newEnFront)

            visited.orAssign(active)
            visited.orAssign(ties)

            myFront.copyFrom(newMyFront)
            enFront.copyFrom(newEnFront)
        }

        return VoronoiResult(myTerritory.countOnes(), enTerritory.countOnes())
    }
}
