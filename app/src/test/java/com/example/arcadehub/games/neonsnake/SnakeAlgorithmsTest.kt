package com.example.arcadehub.games.neonsnake

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SnakeAlgorithmsTest {

    data class TestScenario(
        val id: Int,
        val width: Int,
        val height: Int,
        val gridData: List<Int>,
        val pHead: Point,
        val pBody: List<Point>,
        val aHead: Point,
        val expectedFloodFill: ExpectedFloodFill,
        val expectedVoronoi: ExpectedVoronoi
    )

    data class ExpectedFloodFill(val count: Int, val minTurnsToClear: Int, val hasFood: Boolean)
    data class ExpectedVoronoi(val myCount: Int, val enemyCount: Int)

    @Test
    fun testAlgorithmsMatchJS() {
        // Load JSON
        val jsonPath = "src/test/resources/algorithms_test_data.json"
        val jsonString = File(jsonPath).readText()
        val type = object : TypeToken<List<TestScenario>>() {}.type
        val scenarios: List<TestScenario> = Gson().fromJson(jsonString, type)

        for (scenario in scenarios) {
            // 1. Reconstruct Grid
            val grid = SnakeGrid(scenario.width, scenario.height)
            for (y in 0 until scenario.height) {
                for (x in 0 until scenario.width) {
                    val idx = y * scenario.width + x
                    grid.set(x, y, scenario.gridData[idx])
                }
            }

            // 2. Test FloodFill
            val ffResult = SnakeAlgorithms.floodFill(
                grid,
                scenario.pHead.x,
                scenario.pHead.y,
                100,
                scenario.pBody
            )

            assertEquals("Scenario ${scenario.id} FloodFill Count",
                scenario.expectedFloodFill.count, ffResult.count)
            assertEquals("Scenario ${scenario.id} FloodFill Turns",
                scenario.expectedFloodFill.minTurnsToClear, ffResult.minTurnsToClear)
            assertEquals("Scenario ${scenario.id} FloodFill Food",
                scenario.expectedFloodFill.hasFood, ffResult.hasFood)

            // 3. Test Voronoi
            val vResult = SnakeAlgorithms.computeVoronoi(
                grid,
                scenario.pHead,
                scenario.aHead
            )

            assertEquals("Scenario ${scenario.id} Voronoi MyCount",
                scenario.expectedVoronoi.myCount, vResult.myCount)
            assertEquals("Scenario ${scenario.id} Voronoi EnemyCount",
                scenario.expectedVoronoi.enemyCount, vResult.enemyCount)
        }
    }
}