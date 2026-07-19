package com.example.arcadehub.games.neonsnake.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ZobristTest {

    @Test
    fun `same board state always hashes the same`() {
        val z = Zobrist.build(8, 8)
        val grid = AiGrid.create(8, 8)
        grid.set(2, 2, 1)
        grid.set(3, 3, 2)
        grid.set(4, 4, 3)

        val h1 = z.computeHash(grid, 80, 60)
        val h2 = z.computeHash(grid, 80, 60)
        assertEquals(h1, h2)
    }

    @Test
    fun `different board states hash differently`() {
        val z = Zobrist.build(8, 8)
        val gridA = AiGrid.create(8, 8)
        gridA.set(2, 2, 1)
        val gridB = AiGrid.create(8, 8)
        gridB.set(3, 3, 1)

        assertNotEquals(z.computeHash(gridA, 100, 100), z.computeHash(gridB, 100, 100))
    }

    @Test
    fun `different health hashes differently`() {
        val z = Zobrist.build(8, 8)
        val grid = AiGrid.create(8, 8)
        grid.set(0, 0, 2)

        assertNotEquals(z.computeHash(grid, 90, 50), z.computeHash(grid, 91, 50))
    }

    @Test
    fun `xorUnchecked incrementally matches a full recompute after placing a piece`() {
        val z = Zobrist.build(8, 8)
        val grid = AiGrid.create(8, 8)
        grid.set(1, 1, 2) // pre-existing head

        val baseHash = z.computeHash(grid, 100, 100)

        // Incrementally add a food cell via xorUnchecked...
        val incremental = z.xorUnchecked(baseHash, 5, 5, 1)

        // ...and compare against a full recompute of the same resulting state.
        grid.set(5, 5, 1)
        val fullRecompute = z.computeHash(grid, 100, 100)

        assertEquals(fullRecompute, incremental)
    }

    @Test
    fun `xorUnchecked is its own inverse (apply then undo returns the original hash)`() {
        val z = Zobrist.build(8, 8)
        val grid = AiGrid.create(8, 8)
        val base = z.computeHash(grid, 100, 100)

        val applied = z.xorUnchecked(base, 3, 4, 3)
        val undone = z.xorUnchecked(applied, 3, 4, 3)
        assertEquals(base, undone)
    }

    @Test
    fun `xorHealth matches a full recompute after a health change`() {
        val z = Zobrist.build(8, 8)
        val grid = AiGrid.create(8, 8)
        grid.set(0, 0, 2)

        val before = z.computeHash(grid, 100, 100)
        val incremental = z.xorHealth(before, 0, 100, 99)
        val fullRecompute = z.computeHash(grid, 99, 100)

        assertEquals(fullRecompute, incremental)
    }

    @Test
    fun `ZobristCache returns the same instance for the same board size`() {
        val a = ZobristCache.forSize(16, 9)
        val b = ZobristCache.forSize(16, 9)
        assertEquals(a, b)
    }
}
