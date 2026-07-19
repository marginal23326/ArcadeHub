package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FastBodyTest {

    @Test
    fun `loadFrom preserves head-first order`() {
        val body = FastBody()
        body.loadFrom(listOf(Point(5, 5), Point(5, 6), Point(5, 7)))

        assertEquals(3, body.len)
        assertEquals(5, body.headX())
        assertEquals(5, body.headY())
        assertEquals(5, body.lastX())
        assertEquals(7, body.lastY())
        assertEquals(5, body.x(1))
        assertEquals(6, body.y(1))
    }

    @Test
    fun `pushFront then popFront is a no-op on the rest of the body`() {
        val body = FastBody()
        body.loadFrom(listOf(Point(1, 1), Point(1, 2)))

        body.pushFront(1, 0)
        assertEquals(3, body.len)
        assertEquals(1, body.headX())
        assertEquals(0, body.headY())

        body.popFront()
        assertEquals(2, body.len)
        assertEquals(1, body.headX())
        assertEquals(1, body.headY())
    }

    @Test
    fun `popBack then pushBackPacked restores the tail exactly (apply-undo symmetry)`() {
        val body = FastBody()
        body.loadFrom(listOf(Point(3, 3), Point(3, 4), Point(3, 5)))

        val tail = body.popBack()
        assertEquals(2, body.len)
        assertEquals(3, FastBody.unpackX(tail))
        assertEquals(5, FastBody.unpackY(tail))

        body.pushBackPacked(tail)
        assertEquals(3, body.len)
        assertEquals(3, body.lastX())
        assertEquals(5, body.lastY())
    }

    @Test
    fun `growing move - pushFront without popBack - increases length by one`() {
        val body = FastBody()
        body.loadFrom(listOf(Point(0, 0), Point(0, 1)))
        body.pushFront(0, -1) // "ate food": no popBack this turn
        assertEquals(3, body.len)
        assertEquals(0, body.lastX())
        assertEquals(1, body.lastY())
    }

    @Test
    fun `copyOf is independent of the original`() {
        val body = FastBody()
        body.loadFrom(listOf(Point(2, 2), Point(2, 3)))
        val copy = body.copyOf()

        copy.pushFront(2, 1)
        assertEquals(2, body.len)
        assertEquals(3, copy.len)
    }

    @Test
    fun `forEachPoint visits every segment head-to-tail`() {
        val body = FastBody()
        val points = listOf(Point(0, 0), Point(1, 0), Point(2, 0))
        body.loadFrom(points)

        val visited = mutableListOf<Pair<Int, Int>>()
        body.forEachPoint { x, y -> visited.add(x to y) }

        assertEquals(points.map { it.x to it.y }, visited)
    }

    @Test
    fun `unpackX and unpackY round-trip negative coordinates`() {
        // Coordinates are never actually negative in this game, but the pack scheme uses a
        // sign-extended 16-bit Y field, so it should still round-trip correctly if it ever were.
        val body = FastBody()
        body.pushFront(5, -3)
        val packed = body.popBack()
        assertEquals(5, FastBody.unpackX(packed))
        assertEquals(-3, FastBody.unpackY(packed))
        assertTrue(body.isEmpty())
    }
}
