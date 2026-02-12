package com.example.arcadehub.games.neonsnake

import org.junit.Test
import java.io.File

class SnakeTraceTest {

    @Test
    fun runTraceTestCase2() {
        val file = File("src/test/resources/scenarios/test_case_2.json")
        if (!file.exists()) {
            println("test_case_2.json not found!")
            return
        }

        println("--- KOTLIN TRACE START ---")
        val jsonString = file.readText()
        val scenario = SnakeTestAdapter.loadScenario(jsonString)

        // Init Engine
        SnakeZobrist.init(scenario.width, scenario.height)
        SnakeTT.clear()

        val grid = SnakeGrid(scenario.width, scenario.height)
        scenario.me.body.forEachIndexed { i, p -> grid[p.x, p.y] = if (i == 0) 4 else 2 }
        scenario.enemy.body.forEachIndexed { i, p -> grid[p.x, p.y] = if (i == 0) 4 else 3 }
        scenario.foods.forEach { p -> grid[p.x, p.y] = 1 }

        val distMap = SnakeAlgorithms.getFoodDistanceMap(grid, scenario.foods)
        val state = SnakeHeuristics.State(scenario.me, scenario.enemy, scenario.foods, distMap)

        // Replicate Flaw
        if (scenario.me.body.isNotEmpty()) grid[scenario.me.head().x, scenario.me.head().y] = 2
        if (scenario.enemy.body.isNotEmpty()) grid[scenario.enemy.head().x, scenario.enemy.head().y] = 3

        val initialHash = SnakeZobrist.computeHash(grid, scenario.me.health, scenario.enemy.health)

        // Run Search at Depth 1
        SnakeSearch.alphaBeta(
            grid, state, 1,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
            true, 1, initialHash
        )
    }
}