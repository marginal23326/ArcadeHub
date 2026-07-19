package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicsTest {

    private fun agentAt(points: List<Point>, health: Int = 100): SearchAgent {
        val a = SearchAgent()
        a.body.loadFrom(points)
        a.health = health
        return a
    }

    @Test
    fun `an empty or dead me is an immediate loss`() {
        val grid = AiGrid.create(8, 8)
        val cfg = AiConfig.default()
        val buffers = SearchBuffers(8, 8)
        val enemy = agentAt(listOf(Point(4, 4)))

        val deadMe = agentAt(listOf(Point(1, 1)), health = 0)
        assertEquals(cfg.scores.loss, Heuristics.evaluate(grid, deadMe, enemy, null, cfg, buffers))

        val emptyMe = SearchAgent()
        assertEquals(cfg.scores.loss, Heuristics.evaluate(grid, emptyMe, enemy, null, cfg, buffers))
    }

    @Test
    fun `an empty or dead enemy is an immediate win`() {
        val grid = AiGrid.create(8, 8)
        val cfg = AiConfig.default()
        val buffers = SearchBuffers(8, 8)
        val me = agentAt(listOf(Point(4, 4)))

        val deadEnemy = agentAt(listOf(Point(1, 1)), health = 0)
        assertEquals(cfg.scores.win, Heuristics.evaluate(grid, me, deadEnemy, null, cfg, buffers))
    }

    @Test
    fun `starving with food unreachable in time is a loss`() {
        val grid = AiGrid.create(8, 8)
        grid.set(7, 7, 1) // far-away food
        val cfg = AiConfig.default()
        val buffers = SearchBuffers(8, 8)

        val me = agentAt(listOf(Point(0, 0)), health = 1) // can't possibly reach (7,7) in one turn
        val enemy = agentAt(listOf(Point(4, 4)))

        assertEquals(cfg.scores.loss, Heuristics.evaluate(grid, me, enemy, null, cfg, buffers))
    }

    @Test
    fun `a fully symmetric position scores close to zero`() {
        // Board 9 wide (odd, so there's a true center column) x 5 tall, heads mirrored around
        // the vertical center line, identical length and health on both sides.
        val grid = AiGrid.create(9, 5)
        val me = agentAt(listOf(Point(2, 2), Point(1, 2)))
        val enemy = agentAt(listOf(Point(6, 2), Point(7, 2)))
        for (i in 0 until me.body.len) grid.set(me.body.x(i), me.body.y(i), 2)
        for (i in 0 until enemy.body.len) grid.set(enemy.body.x(i), enemy.body.y(i), 3)

        val cfg = AiConfig.default()
        val buffers = SearchBuffers(9, 5)
        val score = Heuristics.evaluate(grid, me, enemy, null, cfg, buffers)

        // Not required to be exactly zero (health-based food panic isn't perfectly symmetric
        // without food on the board), but should be nowhere near a win/loss/trap magnitude.
        assertTrue(kotlin.math.abs(score) < 1_000_000)
    }

    @Test
    fun `being adjacent to a shorter enemy head grants kill pressure`() {
        val grid = AiGrid.create(8, 8)
        val cfg = AiConfig.default()
        val buffers = SearchBuffers(8, 8)

        val me = agentAt(listOf(Point(2, 2), Point(2, 3), Point(2, 4))) // len 3
        val enemyAdjacent = agentAt(listOf(Point(3, 2))) // len 1, one step from me
        val enemyFar = agentAt(listOf(Point(7, 7)))
        for (i in 0 until me.body.len) grid.set(me.body.x(i), me.body.y(i), 2)

        val gridAdjacent = grid.copyOf()
        gridAdjacent.set(3, 2, 3)
        val gridFar = grid.copyOf()
        gridFar.set(7, 7, 3)

        val scoreAdjacent = Heuristics.evaluate(gridAdjacent, me, enemyAdjacent, null, cfg, buffers)
        val scoreFar = Heuristics.evaluate(gridFar, me, enemyFar, null, cfg, buffers)

        assertTrue(scoreAdjacent > scoreFar)
    }
}
