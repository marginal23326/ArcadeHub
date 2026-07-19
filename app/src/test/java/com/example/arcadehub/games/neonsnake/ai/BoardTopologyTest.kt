package com.example.arcadehub.games.neonsnake.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTopologyTest {

    // A small 4x3 board (12 cells, indices row-major: idx = y*width + x).
    private fun smallTopology(): BoardTopology {
        val numWords = BitBoard.wordsFor(4 * 3)
        return BoardTopology(4, 3, numWords)
    }

    @Test
    fun `validCells marks exactly the in-bounds cells`() {
        val topo = smallTopology()
        assertEquals(12, topo.validCells.countOnes())
        for (i in 0 until 12) assertTrue(topo.validCells[i])
        for (i in 12 until topo.numWords * 64) assertFalse(topo.validCells[i])
    }

    @Test
    fun `up shifts a bit by one row and drops off the top edge`() {
        val topo = smallTopology()
        val src = BitBoard(topo.numWords)
        src.set(1 * 4 + 1) // (x=1, y=1)
        val out = BitBoard(topo.numWords)
        topo.up(src, out)
        assertTrue(out[0 * 4 + 1]) // moved to (1, 0)

        // A cell in the top row has nowhere to go and disappears (matches Rust's edge masking).
        val topRow = BitBoard(topo.numWords)
        topRow.set(0 * 4 + 2)
        val topOut = BitBoard(topo.numWords)
        topo.up(topRow, topOut)
        assertTrue(topOut.isEmpty())
    }

    @Test
    fun `down shifts a bit by one row and drops off the bottom edge`() {
        val topo = smallTopology()
        val src = BitBoard(topo.numWords)
        src.set(1 * 4 + 1)
        val out = BitBoard(topo.numWords)
        topo.down(src, out)
        assertTrue(out[2 * 4 + 1]) // moved to (1, 2)

        val bottomRow = BitBoard(topo.numWords)
        bottomRow.set(2 * 4 + 2)
        val bottomOut = BitBoard(topo.numWords)
        topo.down(bottomRow, bottomOut)
        assertTrue(bottomOut.isEmpty())
    }

    @Test
    fun `left and right shifts do not wrap around row edges`() {
        val topo = smallTopology()
        val scratch = BitBoard(topo.numWords)

        // Rightmost cell of row 0 (x=3, y=0) shifted right must NOT wrap to (x=0, y=1).
        val rightEdge = BitBoard(topo.numWords)
        rightEdge.set(0 * 4 + 3)
        val rightOut = BitBoard(topo.numWords)
        topo.right(rightEdge, rightOut, scratch)
        assertTrue(rightOut.isEmpty())

        // Leftmost cell of row 1 (x=0, y=1) shifted left must NOT wrap to (x=3, y=0).
        val leftEdge = BitBoard(topo.numWords)
        leftEdge.set(1 * 4 + 0)
        val leftOut = BitBoard(topo.numWords)
        topo.left(leftEdge, leftOut, scratch)
        assertTrue(leftOut.isEmpty())

        // An interior cell shifts cleanly in both directions.
        val mid = BitBoard(topo.numWords)
        mid.set(1 * 4 + 1) // (1, 1)
        val midRight = BitBoard(topo.numWords)
        topo.right(mid, midRight, scratch)
        assertTrue(midRight[1 * 4 + 2])
        val midLeft = BitBoard(topo.numWords)
        topo.left(mid, midLeft, scratch)
        assertTrue(midLeft[1 * 4 + 0])
    }

    @Test
    fun `expandNeighbors is the union of all four directional shifts`() {
        val topo = smallTopology()
        val src = BitBoard(topo.numWords)
        src.set(1 * 4 + 1) // center-ish cell (1, 1)
        val out = BitBoard(topo.numWords)
        val scratchA = BitBoard(topo.numWords)
        val scratchB = BitBoard(topo.numWords)
        topo.expandNeighbors(src, out, scratchA, scratchB)

        assertEquals(4, out.countOnes())
        assertTrue(out[0 * 4 + 1]) // up
        assertTrue(out[2 * 4 + 1]) // down
        assertTrue(out[1 * 4 + 0]) // left
        assertTrue(out[1 * 4 + 2]) // right
    }

    @Test
    fun `withBlockedCells removes only the requested cells from validCells`() {
        val topo = smallTopology()
        val blocked = BitBoard(topo.numWords)
        blocked.set(1 * 4 + 1)
        val walled = topo.withBlockedCells(blocked)

        assertEquals(11, walled.validCells.countOnes())
        assertFalse(walled.validCells[1 * 4 + 1])
        // Original topology is untouched.
        assertTrue(topo.validCells[1 * 4 + 1])
    }
}
