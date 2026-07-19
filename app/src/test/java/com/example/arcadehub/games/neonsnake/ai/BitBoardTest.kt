package com.example.arcadehub.games.neonsnake.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitBoardTest {

    @Test
    fun `set, get, and unset a bit`() {
        val b = BitBoard(3)
        assertFalse(b[100])
        b.set(100)
        assertTrue(b[100])
        b.unset(100)
        assertFalse(b[100])
    }

    @Test
    fun `bits in different words are independent`() {
        val b = BitBoard(3)
        b.set(0)
        b.set(63)
        b.set(64)
        b.set(150)
        assertTrue(b[0])
        assertTrue(b[63])
        assertTrue(b[64])
        assertTrue(b[150])
        assertEquals(4, b.countOnes())
    }

    @Test
    fun `isEmpty and any reflect bit state`() {
        val b = BitBoard(3)
        assertTrue(b.isEmpty())
        assertFalse(b.any())
        b.set(75)
        assertFalse(b.isEmpty())
        assertTrue(b.any())
    }

    @Test
    fun `popFirst removes bits in ascending order and returns -1 when empty`() {
        val b = BitBoard(3)
        b.set(130)
        b.set(5)
        b.set(64)

        assertEquals(5, b.popFirst())
        assertEquals(64, b.popFirst())
        assertEquals(130, b.popFirst())
        assertEquals(-1, b.popFirst())
        assertTrue(b.isEmpty())
    }

    @Test
    fun `and, or, xor match plain set semantics`() {
        val a = BitBoard(2)
        val b = BitBoard(2)
        a.set(1); a.set(2); a.set(70)
        b.set(2); b.set(3); b.set(70)

        val and = a and b
        assertTrue(and[2] && and[70])
        assertFalse(and[1] || and[3])

        val or = a or b
        assertTrue(or[1] && or[2] && or[3] && or[70])

        val xor = a xor b
        assertTrue(xor[1] && xor[3])
        assertFalse(xor[2] || xor[70])
    }

    @Test
    fun `andNotAssign clears only bits present in the other board`() {
        val a = BitBoard(2)
        val b = BitBoard(2)
        a.set(1); a.set(2); a.set(3)
        b.set(2)

        a.andNotAssign(b)
        assertTrue(a[1] && a[3])
        assertFalse(a[2])
    }

    @Test
    fun `invertAssign flips every bit`() {
        val b = BitBoard(1)
        b.set(0)
        b.invertAssign()
        assertFalse(b[0])
        assertTrue(b[1])
        assertTrue(b[63])
    }

    @Test
    fun `copyOf is independent of the original`() {
        val a = BitBoard(2)
        a.set(10)
        val copy = a.copyOf()
        copy.set(20)
        assertFalse(a[20])
        assertTrue(copy[10] && copy[20])
    }

    @Test
    fun `wordsFor computes the minimum word count`() {
        assertEquals(1, BitBoard.wordsFor(1))
        assertEquals(1, BitBoard.wordsFor(64))
        assertEquals(2, BitBoard.wordsFor(65))
        assertEquals(3, BitBoard.wordsFor(144)) // this game's 16x9 board
    }
}
