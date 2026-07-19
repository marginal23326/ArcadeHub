package com.example.arcadehub.games.neonsnake.regression

import com.example.arcadehub.games.neonsnake.GridDir
import com.example.arcadehub.games.neonsnake.Point
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File

data class ScenarioPoint(val x: Int, val y: Int) {
    fun toPoint(boardHeight: Int): Point = Point(x, boardHeight - 1 - y)
}

data class ScenarioSnake(
    val id: String,
    val name: String,
    val body: List<ScenarioPoint>,
    val health: Int
)

data class ScenarioBoard(
    val width: Int,
    val height: Int,
    val food: List<ScenarioPoint>,
    val snakes: List<ScenarioSnake>
)

data class ScenarioExpectation(
    val kind: String,
    val direction: String? = null,
    val directions: List<String>? = null
) {
    fun passes(actual: GridDir): Boolean = when (kind) {
        "exact" -> parseDirection(requireNotNull(direction) { "exact expectation missing 'direction'" }) == actual
        "avoid" -> (directions ?: emptyList()).map(::parseDirection).none { it == actual }
        else -> error("unknown expectation kind: $kind")
    }

    fun describe(): String = when (kind) {
        "exact" -> direction.orEmpty()
        "avoid" -> "avoid ${directions.orEmpty().joinToString(",")}"
        else -> kind
    }
}

data class Scenario(
    val id: String,
    val name: String,
    val board: ScenarioBoard,
    @SerializedName("you_id") val youId: String,
    val expectation: ScenarioExpectation
) {
    fun toAiInputs(): ScenarioAiInputs? {
        val me = board.snakes.find { it.id == youId } ?: return null
        val enemy = board.snakes.find { it.id != youId }
            ?: ScenarioSnake(id = "enemy", name = "enemy", body = emptyList(), health = 0)

        val h = board.height
        return ScenarioAiInputs(
            meBody = me.body.map { it.toPoint(h) },
            meHealth = me.health,
            enemyBody = enemy.body.map { it.toPoint(h) },
            enemyHealth = enemy.health,
            food = board.food.map { it.toPoint(h) },
            width = board.width,
            height = board.height
        )
    }
}

data class ScenarioAiInputs(
    val meBody: List<Point>,
    val meHealth: Int,
    val enemyBody: List<Point>,
    val enemyHealth: Int,
    val food: List<Point>,
    val width: Int,
    val height: Int
)

data class NamedScenario(val fileName: String, val scenario: Scenario)

private fun parseDirection(raw: String): GridDir = when (raw.trim().lowercase()) {
    "up" -> GridDir.UP
    "down" -> GridDir.DOWN
    "left" -> GridDir.LEFT
    "right" -> GridDir.RIGHT
    else -> error("invalid direction: $raw")
}

object ScenarioLoader {
    private val gson = Gson()

    fun loadFromResourceDir(resourceDir: String): List<NamedScenario> {
        val dir = File("src/test/resources/$resourceDir")
        if (!dir.exists()) return emptyList()

        val files = dir.listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name }
            ?: emptyList()

        return files.map { file ->
            val scenario = gson.fromJson(file.readText(), Scenario::class.java)
            NamedScenario(file.name, scenario)
        }
    }
}
