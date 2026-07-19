package com.example.arcadehub.games.neonsnake.regression

import com.example.arcadehub.games.neonsnake.GridDir
import com.example.arcadehub.games.neonsnake.SnakeBrain
import com.example.arcadehub.games.neonsnake.SnakeEntity
import com.example.arcadehub.games.neonsnake.SnakeMapGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioRegressionTest {

    private val depth: Int
        get() = System.getProperty("snake.regressionDepth")?.toIntOrNull() ?: DEFAULT_DEPTH

    @Test
    fun runScenarioCorpus() {
        val scenarios = ScenarioLoader.loadFromResourceDir("scenarios")
        assertTrue("No scenarios found under src/test/resources/scenarios", scenarios.isNotEmpty())

        println()
        println("RUNNING KOTLIN AI REGRESSION SUITE (${scenarios.size} scenarios, depth=$depth)")
        println()

        var passed = 0
        var failed = 0
        var skipped = 0
        val failures = StringBuilder()
        val started = System.nanoTime()

        for (named in scenarios) {
            val inputs = named.scenario.toAiInputs()
            if (inputs == null) {
                skipped++
                println("SKIP ${named.fileName}: you_id '${named.scenario.youId}' not found among snakes")
                continue
            }

            val me = SnakeEntity(ArrayList(inputs.meBody), GridDir.UP, health = inputs.meHealth)
            val enemy = SnakeEntity(ArrayList(inputs.enemyBody), GridDir.UP, health = inputs.enemyHealth)

            val actual = SnakeBrain.getSmartMove(
                me, enemy, inputs.food,
                inputs.width, inputs.height,
                depth,
                SnakeMapGenerator.MapType.EMPTY
            )

            if (named.scenario.expectation.passes(actual)) {
                passed++
                println("PASS ${named.fileName} -> ${actual.name.lowercase()}")
            } else {
                failed++
                val line = "FAIL ${named.fileName} [${inputs.width}x${inputs.height}]: " +
                    "expected ${named.scenario.expectation.describe()}, got ${actual.name.lowercase()}"
                println(line)
                failures.appendLine(line)
            }
        }

        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        println()
        println("--- RESULTS ---")
        println("Passed:  $passed")
        println("Failed:  $failed")
        println("Skipped: $skipped")
        println("Time:    ${elapsedMs}ms (${scenarios.size} scenarios)")
        println()

        assertTrue(
            "AI regression suite failed ($failed/${scenarios.size} scenarios) at depth $depth:\n$failures",
            failed == 0
        )
    }

    companion object {
        const val DEFAULT_DEPTH = 6
    }
}
