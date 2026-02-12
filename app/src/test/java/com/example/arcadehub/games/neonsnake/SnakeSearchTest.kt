package com.example.arcadehub.games.neonsnake

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.math.abs

class SnakeSearchTest {

    data class TestCase(
        val id: Int,
        val me: SnakeEntityData,
        val enemy: SnakeEntityData,
        val food: List<Point>,
        val width: Int,
        val height: Int,
        val depth: Int,
        val expected: ExpectedResult
    )

    data class SnakeEntityData(val body: List<Point>, val health: Int)
    data class ExpectedResult(val score: Double, val move: GridDir?)

    @Test
    fun testSearchMatchesJS() {
        val jsonString = File("src/test/resources/search_test_data.json").readText()
        val type = object : TypeToken<List<TestCase>>() {}.type
        val testCases: List<TestCase> = Gson().fromJson(jsonString, type)

        // Mock the SnakeHeuristics.evaluate function for this test to match JS
        // This is a complex dependency, so we'll just trust our previous tests
        // and ensure the search tree logic is the same.

        for (case in testCases) {
            SnakeZobrist.init(case.width, case.height)
            SnakeTT.clear()

            val grid = SnakeGrid(case.width, case.height)
            case.me.body.forEachIndexed { i, p -> grid[p.x, p.y] = if (i == 0) 4 else 2 }
            case.enemy.body.forEachIndexed { i, p -> grid[p.x, p.y] = if (i == 0) 4 else 3 }
            case.food.forEach { p -> grid[p.x, p.y] = 1 }

            val distMap = SnakeAlgorithms.getFoodDistanceMap(grid, case.food)

            val meEntity = SnakeEntity(ArrayList(case.me.body), GridDir.UP, 0, case.me.health)
            val enemyEntity = SnakeEntity(ArrayList(case.enemy.body), GridDir.UP, 0, case.enemy.health)
            val state = SnakeHeuristics.State(meEntity, enemyEntity, case.food, distMap)

            // For hashing, heads are just body parts of a certain type
            if (state.me.body.isNotEmpty()) grid[state.me.head().x, state.me.head().y] = 2
            if (state.enemy.body.isNotEmpty()) grid[state.enemy.head().x, state.enemy.head().y] = 3

            val initialHash = SnakeZobrist.computeHash(grid, state.me.health, state.enemy.health)

            // Revert grid state for search start
            if (state.me.body.isNotEmpty()) grid[state.me.head().x, state.me.head().y] = 4
            if (state.enemy.body.isNotEmpty()) grid[state.enemy.head().x, state.enemy.head().y] = 4

            val result = SnakeSearch.alphaBeta(
                grid, state, case.depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true, case.depth, initialHash
            )

            assertEquals("Scenario ${case.id} Best Move", case.expected.move, result.move)
            // Compare scores with a tolerance for floating point differences
            val scoreDiff = abs(case.expected.score - result.score)
            assert(scoreDiff < 1.0) { "Scenario ${case.id} Score Mismatch. JS: ${case.expected.score}, KT: ${result.score}" }
        }
    }

    @Test
    fun debugSearchDivergence() {
        // This test is for manually comparing log output with js_log.txt
        // It only runs the scenario that was failing.
        val jsonString = File("src/test/resources/search_test_data.json").readText()
        val type = object : TypeToken<List<TestCase>>() {}.type
        val allCases: List<TestCase> = Gson().fromJson(jsonString, type)
        val case = allCases.first { it.id == 1 }

        println("\n--- STARTING KOTLIN SCENARIO ${case.id} ---\n")

        SnakeZobrist.init(case.width, case.height)
        SnakeTT.clear()

        val grid = SnakeGrid(case.width, case.height)
        case.me.body.forEachIndexed { i, p -> grid[p.x, p.y] = if (i == 0) 4 else 2 }
        case.enemy.body.forEachIndexed { i, p -> grid[p.x, p.y] = if (i == 0) 4 else 3 }
        case.food.forEach { p -> grid[p.x, p.y] = 1 }

        val distMap = SnakeAlgorithms.getFoodDistanceMap(grid, case.food)
        val meEntity = SnakeEntity(ArrayList(case.me.body), GridDir.UP, 0, case.me.health)
        val enemyEntity = SnakeEntity(ArrayList(case.enemy.body), GridDir.UP, 0, case.enemy.health)
        val state = SnakeHeuristics.State(meEntity, enemyEntity, case.food, distMap)

        // REPLICATE THE FLAW: Change heads to body parts for hashing
        if (state.me.body.isNotEmpty()) grid[state.me.head().x, state.me.head().y] = 2
        if (state.enemy.body.isNotEmpty()) grid[state.enemy.head().x, state.enemy.head().y] = 3

        val initialHash = SnakeZobrist.computeHash(grid, state.me.health, state.enemy.health)

        // Pass the flawed grid into search, just like the JS version
        val result = SnakeSearch.alphaBeta(
            grid, state, case.depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true, case.depth, initialHash
        )

        // The assertion will still fail, but we can now analyze the logs
        assertEquals("Scenario ${case.id} Best Move", case.expected.move, result.move)
    }
}