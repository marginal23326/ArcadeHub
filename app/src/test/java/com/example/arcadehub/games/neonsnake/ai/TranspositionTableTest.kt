package com.example.arcadehub.games.neonsnake.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranspositionTableTest {

    @Test
    fun `set then get round-trips every field`() {
        val tt = TranspositionTable(1 shl 10)
        tt.prepareForSearch(1 shl 10)

        tt.set(12345L, 7, -42, TT_FLAG_EXACT, 3, 4, 2)
        val entry = tt.get(12345L)

        assertTrue(entry != null)
        assertEquals(-42, entry!!.score)
        assertEquals(7, entry.depth)
        assertEquals(TT_FLAG_EXACT, entry.flag)
        assertEquals(3, entry.mvX)
        assertEquals(4, entry.mvY)
        assertEquals(2, entry.mvDir)
        assertTrue(entry.hasMove())
    }

    @Test
    fun `probe is an allocation-free equivalent of get`() {
        val tt = TranspositionTable(1 shl 10)
        tt.prepareForSearch(1 shl 10)
        tt.set(999L, 5, 100, TT_FLAG_LOWER, 1, 1, 0)

        val out = TtProbe()
        val hit = tt.probe(999L, out)

        assertTrue(hit)
        assertEquals(100, out.score)
        assertEquals(5, out.depth)
        assertEquals(TT_FLAG_LOWER, out.flag)
    }

    @Test
    fun `a lookup for a hash that was never stored misses`() {
        val tt = TranspositionTable(1 shl 10)
        tt.prepareForSearch(1 shl 10)
        assertNull(tt.get(424242L))
    }

    @Test
    fun `a deeper write replaces a shallower entry at the same slot`() {
        val tt = TranspositionTable(1 shl 10)
        tt.prepareForSearch(1 shl 10)

        tt.set(1L, 3, 10, TT_FLAG_EXACT, 0, 0, 0)
        tt.set(1L, 8, 20, TT_FLAG_EXACT, 0, 0, 1)

        val entry = tt.get(1L)!!
        assertEquals(8, entry.depth)
        assertEquals(20, entry.score)
    }

    @Test
    fun `a shallower write does not replace a deeper entry at the same slot`() {
        val tt = TranspositionTable(1 shl 10)
        tt.prepareForSearch(1 shl 10)

        tt.set(1L, 8, 20, TT_FLAG_EXACT, 0, 0, 1)
        tt.set(1L, 3, 10, TT_FLAG_EXACT, 0, 0, 0)

        val entry = tt.get(1L)!!
        assertEquals(8, entry.depth)
        assertEquals(20, entry.score)
    }

    @Test
    fun `prepareForSearch bumps the generation and ages out old entries`() {
        val tt = TranspositionTable(1 shl 10)
        tt.prepareForSearch(1 shl 10)
        tt.set(1L, 5, 10, TT_FLAG_EXACT, 0, 0, 0)
        assertTrue(tt.get(1L) != null)

        tt.prepareForSearch(1 shl 10) // starts a new "search" / generation
        assertNull(tt.get(1L))
    }

    @Test
    fun `prepareForSearch grows the table when a larger size is requested`() {
        val tt = TranspositionTable(1 shl 8)
        tt.prepareForSearch(1 shl 16)
        // Should not throw, and entries should still round-trip after growing.
        tt.set(77L, 1, 1, TT_FLAG_EXACT, 0, 0, 0)
        assertTrue(tt.get(77L) != null)
    }

    @Test
    fun `nextPowerOfTwo rounds up correctly`() {
        assertEquals(1, TranspositionTable.nextPowerOfTwo(1))
        assertEquals(16, TranspositionTable.nextPowerOfTwo(9))
        assertEquals(16, TranspositionTable.nextPowerOfTwo(16))
        assertEquals(32, TranspositionTable.nextPowerOfTwo(17))
    }

    @Test
    fun `depthBasedEntries matches the strongsnake tiering exactly`() {
        assertEquals(1 shl 15, TranspositionTable.depthBasedEntries(4))
        assertEquals(1 shl 17, TranspositionTable.depthBasedEntries(8))
        assertEquals(1 shl 19, TranspositionTable.depthBasedEntries(12))
        assertEquals(1 shl 21, TranspositionTable.depthBasedEntries(16))
        assertEquals(1 shl 21, TranspositionTable.depthBasedEntries(20))
        assertEquals(1 shl 22, TranspositionTable.depthBasedEntries(21))
    }

    @Test
    fun `no-move sentinel round-trips as hasMove false`() {
        val tt = TranspositionTable(1 shl 10)
        tt.prepareForSearch(1 shl 10)
        tt.set(5L, 0, 7, TT_FLAG_EXACT, 255, 255, 255)
        val entry = tt.get(5L)!!
        assertFalse(entry.hasMove())
    }
}
