package com.example.arcadehub.games.neonsnake

import com.google.gson.Gson
import com.google.gson.JsonObject

object SnakeTestAdapter {
    private val gson = Gson()

    data class Scenario(
        val name: String,
        val expectedMove: String?,
        val avoidList: List<String>,
        val me: SnakeEntity,
        val enemy: SnakeEntity,
        val foods: List<Point>,
        val width: Int,
        val height: Int
    )

    fun loadScenario(jsonString: String): Scenario {
        val root = gson.fromJson(jsonString, JsonObject::class.java)
        val raw = if (root.has("rawRequest")) root.getAsJsonObject("rawRequest") else root
        val board = raw.getAsJsonObject("board")
        val you = raw.getAsJsonObject("you")

        val width = board.get("width").asInt
        val height = board.get("height").asInt

        val yFlip = { y: Int -> (height - 1) - y }

        val foodList = board.getAsJsonArray("food").map {
            val f = it.asJsonObject
            Point(f.get("x").asInt, yFlip(f.get("y").asInt))
        }

        val pBody = you.getAsJsonArray("body").map {
            val b = it.asJsonObject
            Point(b.get("x").asInt, yFlip(b.get("y").asInt))
        }
        val player = SnakeEntity(ArrayList(pBody), GridDir.UP, health = you.get("health").asInt)

        val snakes = board.getAsJsonArray("snakes")
        var enemy = SnakeEntity(ArrayList(), GridDir.UP)
        val myId = you.get("id")?.asString ?: ""

        for (s in snakes) {
            val sObj = s.asJsonObject
            if ((sObj.get("id")?.asString ?: "") != myId) {
                val eBody = sObj.getAsJsonArray("body").map {
                    val b = it.asJsonObject
                    Point(b.get("x").asInt, yFlip(b.get("y").asInt))
                }
                enemy = SnakeEntity(ArrayList(eBody), GridDir.UP, health = sObj.get("health").asInt)
                break
            }
        }

        val expected = if (root.has("expectedMove")) root.get("expectedMove").asString else null
        val avoid = if (root.has("avoidList")) {
            root.getAsJsonArray("avoidList").map { it.asString }
        } else emptyList()

        return Scenario(
            name = root.get("name").asString,
            expectedMove = expected,
            avoidList = avoid,
            me = player,
            enemy = enemy,
            foods = foodList,
            width = width,
            height = height
        )
    }
}