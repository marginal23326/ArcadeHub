package com.example.arcadehub.games.neonsnake

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SnakeBrainTest {

    data class BrainTestCase(
        val id: Int,
        val me: SnakeEntityData,
        val enemy: SnakeEntityData,
        val food: List<Point>,
        val width: Int,
        val height: Int,
        val depth: Int,
        val expectedMove: GridDir?
    )

    data class SnakeEntityData(val body: List<Point>, val health: Int)

    @Test
    fun testBrainMatchesJS() {
        val jsonString = File("src/test/resources/brain_test_data.json").readText()
        val type = object : TypeToken<List<BrainTestCase>>() {}.type
        val cases: List<BrainTestCase> = Gson().fromJson(jsonString, type)

        for (case in cases) {
            // Reconstruct Entities
            val me = SnakeEntity(ArrayList(case.me.body), GridDir.UP, 0, case.me.health)
            val enemy = SnakeEntity(ArrayList(case.enemy.body), GridDir.UP, 0, case.enemy.health)

            // Run Brain
            val actualMove = SnakeBrain.getSmartMove(
                me, enemy, case.food,
                case.width, case.height, case.depth
            )

            assertEquals("Scenario ${case.id} Brain Move", case.expectedMove, actualMove)
        }
    }
}