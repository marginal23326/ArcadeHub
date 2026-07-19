package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloodFillTest {

    @Test
    fun `open board counts every empty cell`() {
        val grid = AiGrid.create(5, 5)
        val buffers = SearchBuffers(5, 5)

        val result = FloodFill.run(grid, 2, 2, 25, null, null, buffers)
        assertEquals(25, result.count)
        assertEquals(TURNS_NEVER_CLEARS, result.minTurnsToClear)
    }

    @Test
    fun `flood fill stops at walls and bodies`() {
        val walled = AiGrid.create(5, 5, wallColumn(5, 5, 2))
        val buffers = SearchBuffers(5, 5)

        val result = FloodFill.run(walled, 0, 0, 25, null, null, buffers)
        assertEquals(10, result.count) // the 2x5 half containing (0,0), wall column excluded
        assertEquals(TURNS_NEVER_CLEARS, result.minTurnsToClear)
    }

    @Test
    fun `hasFood is true only when food is within the explored region`() {
        val grid = AiGrid.create(5, 5)
        grid.set(4, 4, 1)
        val buffers = SearchBuffers(5, 5)

        val near = FloodFill.run(grid, 4, 3, 25, null, null, buffers)
        assertTrue(near.hasFood)

        val gridNoFood = AiGrid.create(5, 5)
        val far = FloodFill.run(gridNoFood, 0, 0, 25, null, null, buffers)
        assertTrue(!far.hasFood)
    }

    @Test
    fun `a body segment on the frontier reports its vanish time`() {
        val grid = AiGrid.create(6, 1)
        // Body occupies (2,0)..(5,0); head effectively "trapped" behind it from (1,0).
        val body = FastBody()
        body.loadFrom(listOf(Point(2, 0), Point(3, 0), Point(4, 0), Point(5, 0)))
        for (i in 0 until body.len) grid.set(body.x(i), body.y(i), 2)

        val buffers = SearchBuffers(6, 1)
        val result = FloodFill.run(grid, 0, 0, 6, body, null, buffers)

        // Only (0,0) and (1,0) are open; the flood fill should report count=2 and note that the
        // nearest body segment (index 0, vanishing in len-0=4 turns) bounds the region.
        assertEquals(2, result.count)
        assertEquals(4, result.minTurnsToClear)
    }

    private fun wallColumn(width: Int, height: Int, x: Int): BitBoard {
        val bits = BitBoard(BitBoard.wordsFor(width * height))
        for (y in 0 until height) bits.set(y * width + x)
        return bits
    }
}
