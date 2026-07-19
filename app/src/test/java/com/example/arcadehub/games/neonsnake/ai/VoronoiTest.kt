package com.example.arcadehub.games.neonsnake.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoronoiTest {

    @Test
    fun `two heads at opposite corners split an even open board evenly`() {
        // 4x4 open board, heads on opposite corners: perfectly symmetric, so territory should
        // split evenly (16 cells total, 2 of them are the heads themselves - neither counts as
        // territory since compute_voronoi expands from, not including, the starting cell... see
        // below for the exact expected count).
        val grid = AiGrid.create(4, 4)
        val buffers = SearchBuffers(4, 4)

        val result = Voronoi.compute(grid, 0, 0, 3, 3, buffers)
        assertEquals(result.myCount, result.enemyCount)
    }

    @Test
    fun `a closer head claims more territory than a farther one`() {
        // My head is near the food-free open area; enemy head is further away.
        val grid = AiGrid.create(10, 10)
        val buffers = SearchBuffers(10, 10)

        val result = Voronoi.compute(grid, 4, 4, 9, 9, buffers)
        assertTrue(result.myCount > result.enemyCount)
    }

    @Test
    fun `walls and bodies are excluded from all territory`() {
        val walls = BitBoard(BitBoard.wordsFor(6 * 6))
        // Wall off the entire right half of the board.
        for (y in 0 until 6) for (x in 3 until 6) walls.set(y * 6 + x)
        val grid = AiGrid.create(6, 6, walls)
        val buffers = SearchBuffers(6, 6)

        val result = Voronoi.compute(grid, 0, 0, 1, 1, buffers)
        // Only the 3x6 left half (18 cells) is reachable at all, split between the two heads.
        assertTrue(result.myCount + result.enemyCount <= 18)
    }

    @Test
    fun `an equidistant cell is a tie and belongs to neither side`() {
        // 3x1 strip: heads at both ends, single cell in the middle is equidistant.
        val grid = AiGrid.create(3, 1)
        val buffers = SearchBuffers(3, 1)

        val result = Voronoi.compute(grid, 0, 0, 2, 0, buffers)
        assertEquals(0, result.myCount)
        assertEquals(0, result.enemyCount)
    }
}
