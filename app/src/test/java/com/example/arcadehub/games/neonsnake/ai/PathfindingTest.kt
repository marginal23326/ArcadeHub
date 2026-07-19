package com.example.arcadehub.games.neonsnake.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathfindingTest {

    @Test
    fun `distance at the food cell itself is zero`() {
        val grid = AiGrid.create(5, 5)
        grid.set(2, 2, 1)
        val buffers = SearchBuffers(5, 5)

        val map = Pathfinding.foodDistanceMap(grid, buffers)
        assertEquals(0, map[grid.idx(2, 2)])
    }

    @Test
    fun `distance elsewhere is the manhattan distance to the nearest food`() {
        val grid = AiGrid.create(5, 5)
        grid.set(0, 0, 1)
        val buffers = SearchBuffers(5, 5)

        val map = Pathfinding.foodDistanceMap(grid, buffers)
        assertEquals(4, map[grid.idx(2, 2)])
        assertEquals(1, map[grid.idx(1, 0)])
    }

    @Test
    fun `multiple foods each contribute their own zero and the map takes the minimum`() {
        val grid = AiGrid.create(5, 1)
        grid.set(0, 0, 1)
        grid.set(4, 0, 1)
        val buffers = SearchBuffers(5, 1)

        val map = Pathfinding.foodDistanceMap(grid, buffers)
        assertEquals(0, map[0])
        assertEquals(1, map[1])
        assertEquals(2, map[2]) // equidistant from both ends
        assertEquals(1, map[3])
        assertEquals(0, map[4])
    }

    @Test
    fun `a wall blocks the direct path, forcing a longer route`() {
        val walls = BitBoard(BitBoard.wordsFor(3 * 3))
        // Wall off the middle column except the bottom cell, forcing a detour.
        walls.set(0 * 3 + 1)
        walls.set(1 * 3 + 1)
        val grid = AiGrid.create(3, 3, walls)
        grid.set(2, 0, 1)
        val buffers = SearchBuffers(3, 3)

        val map = Pathfinding.foodDistanceMap(grid, buffers)
        // Straight-line distance from (0,0) to (2,0) would be 2, but the wall forces a detour
        // down through (0,1)/(0,2)/(1,2)/(2,2) then back up to (2,0).
        assertTrue(map[grid.idx(0, 0)] > 2)
    }

    @Test
    fun `cells with no reachable food are marked unreachable`() {
        val walls = BitBoard(BitBoard.wordsFor(3 * 1))
        walls.set(1) // wall between the two halves of a 3x1 strip
        val grid = AiGrid.create(3, 1, walls)
        grid.set(2, 0, 1)
        val buffers = SearchBuffers(3, 1)

        val map = Pathfinding.foodDistanceMap(grid, buffers)
        assertEquals(Pathfinding.UNREACHABLE, map[grid.idx(0, 0)])
    }
}
