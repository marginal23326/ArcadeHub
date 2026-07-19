package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.GridDir
import com.example.arcadehub.games.neonsnake.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveGenerationTest {

    private fun agentAt(points: List<Point>, health: Int = 100): SearchAgent {
        val a = SearchAgent()
        a.body.loadFrom(points)
        a.health = health
        return a
    }

    @Test
    fun `an open board offers all four directions`() {
        val grid = AiGrid.create(10, 10)
        val me = agentAt(listOf(Point(5, 5), Point(5, 6)))
        val enemy = agentAt(listOf(Point(0, 0)))
        val out = MoveList()

        MoveGeneration.getSafeNeighbors(grid, me, enemy, out)
        assertEquals(4, out.count)
    }

    @Test
    fun `walls, my own body, and the enemy body are all excluded`() {
        val walls = BitBoard(BitBoard.wordsFor(5 * 5))
        walls.set(2 * 5 + 3) // wall to the right of (2,2)
        walls.set(2 * 5 + 1) // wall to the left of (2,2)
        val grid = AiGrid.create(5, 5, walls)
        // Straight 3-segment body: (2,3) blocks "down" and is genuinely mid-body (not the tail),
        // so the tail-follow exception in getSafeNeighbors correctly does not apply to it.
        val me = agentAt(listOf(Point(2, 2), Point(2, 3), Point(2, 4)))
        for (i in 0 until me.body.len) grid.set(me.body.x(i), me.body.y(i), 2)
        val enemy = agentAt(listOf(Point(2, 1))) // enemy head blocks up
        grid.set(enemy.body.headX(), enemy.body.headY(), 3)

        val out = MoveList()
        MoveGeneration.getSafeNeighbors(grid, me, enemy, out)
        assertEquals(0, out.count) // up=enemy, down=own body (mid-body), left=wall, right=wall
    }

    @Test
    fun `stepping into my own tail is safe when the tail is about to vacate`() {
        // Coiled 4-segment body: (2,2) head -> (2,3) -> (1,3) -> (1,2) tail. The tail is exactly
        // one step left of the head, which only happens when the snake has folded back on itself.
        val me = agentAt(listOf(Point(2, 2), Point(2, 3), Point(1, 3), Point(1, 2)))
        val enemy = agentAt(listOf(Point(4, 4)))

        // Wall off "up" and "right"; "down" is already blocked by the mid-body segment at
        // (2,3), leaving "left" (into the tail) as the only candidate.
        val walls = BitBoard(BitBoard.wordsFor(5 * 5))
        walls.set(1 * 5 + 2) // up: (2,1)
        walls.set(2 * 5 + 3) // right: (3,2)
        val grid = AiGrid.create(5, 5, walls)
        for (i in 0 until me.body.len) grid.set(me.body.x(i), me.body.y(i), 2)

        val out = MoveList()
        MoveGeneration.getSafeNeighbors(grid, me, enemy, out)
        assertEquals(1, out.count)
        assertEquals(GridDir.LEFT, directionFromIndex(out.dirInt(0)))
    }

    @Test
    fun `stepping into a stacked tail (just ate) is unsafe`() {
        // Same coiled shape, but the tail is "stacked" (duplicated), as happens for one tick
        // right after eating - the cell will not actually vacate this turn.
        val me = agentAt(listOf(Point(2, 2), Point(2, 3), Point(1, 3), Point(1, 2), Point(1, 2)))
        val enemy = agentAt(listOf(Point(4, 4)))

        val walls = BitBoard(BitBoard.wordsFor(5 * 5))
        walls.set(1 * 5 + 2) // up
        walls.set(2 * 5 + 3) // right
        val grid = AiGrid.create(5, 5, walls)
        for (i in 0 until me.body.len) grid.set(me.body.x(i), me.body.y(i), 2)

        val out = MoveList()
        MoveGeneration.getSafeNeighbors(grid, me, enemy, out)
        assertEquals(0, out.count)
    }

    @Test
    fun `an empty body yields no moves`() {
        val grid = AiGrid.create(5, 5)
        val me = SearchAgent()
        val enemy = agentAt(listOf(Point(0, 0)))
        val out = MoveList()
        MoveGeneration.getSafeNeighbors(grid, me, enemy, out)
        assertEquals(0, out.count)
    }

    @Test
    fun `rootTieBreaker penalizes stepping into a shorter-or-equal enemy's head`() {
        // Enemy head at (3,2); candidate move (3,1) is exactly one step from it.
        val me = agentAt(listOf(Point(2, 2), Point(2, 3)))
        val equalEnemy = agentAt(listOf(Point(3, 2), Point(3, 3)))
        val equalScore = MoveGeneration.rootTieBreaker(me, equalEnemy, 10, 10, 3, 1)
        assertTrue(equalScore < 0)

        val shorterEnemy = agentAt(listOf(Point(3, 2)))
        val biggerMe = agentAt(listOf(Point(2, 2), Point(2, 3), Point(2, 4)))
        val winScore = MoveGeneration.rootTieBreaker(biggerMe, shorterEnemy, 10, 10, 3, 1)
        assertTrue(winScore > 0)
    }

    @Test
    fun `shouldExtendLeaf is false for short snakes regardless of board occupancy`() {
        val grid = AiGrid.create(4, 4)
        val me = agentAt(listOf(Point(0, 0)))
        val enemy = agentAt(listOf(Point(3, 3)))
        val buffers = SearchBuffers(4, 4)
        val cfg = AiConfig.default()

        assertFalse(MoveGeneration.shouldExtendLeaf(grid, me, enemy, cfg, buffers))
    }
}
