package com.example.arcadehub.games.neonsnake

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SnakeAiRegressionTest {

    @Test
    fun runJsScenarios() {
        val scenarioFolder = File("src/test/resources/scenarios")
        val files = scenarioFolder.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()

        println("\n🚀 RUNNING ANDROID AI REGRESSION SUITE (${files.size} scenarios)\n")

        var passed = 0
        var failed = 0

        files.forEach { file ->
            try {
                val jsonString = file.readText()
                val scenario = SnakeTestAdapter.loadScenario(jsonString)

                val bestMove = SnakeBrain.getSmartMove(
                    scenario.me,
                    scenario.enemy,
                    scenario.foods,
                    scenario.width,
                    scenario.height,
                    depth = 1
                )

                val actualMoveName = bestMove.name.lowercase()
                val expectedMoveName = scenario.expectedMove?.lowercase()

                if (expectedMoveName != null && expectedMoveName != "unknown") {
                    if (actualMoveName == expectedMoveName) {
                        println("✅ [PASS] ${file.name}: AI chose $actualMoveName")
                        passed++
                    } else {
                        println("❌ [FAIL] ${file.name}: Expected $expectedMoveName, Got $actualMoveName")
                        failed++
                    }
                } else {
                    val avoidListNames = scenario.avoidList.map { it.lowercase() }
                    if (avoidListNames.isNotEmpty()) {
                        if (avoidListNames.contains(actualMoveName)) {
                            println("❌ [FAIL] ${file.name}: AI chose $actualMoveName (Avoid: $avoidListNames)")
                            failed++
                        } else {
                            println("✅ [PASS] ${file.name}: AI avoided $avoidListNames (chose $actualMoveName)")
                            passed++
                        }
                    } else {
                        println("⚠️ [SKIP] ${file.name}: No expectations or avoid list provided.")
                    }
                }
            } catch (e: Exception) {
                println("❌ [CRASH] ${file.name}: ${e.message}")
                failed++
            }
        }

        println("\n--- RESULTS ---")
        println("Passed: $passed")
        println("Failed: $failed\n")

        assertTrue("AI regression suite failed. See logs for details.", failed == 0)
    }
}