package com.example.arcadehub.games.neonsnake

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SnakeHashingTest {

    @Test
    fun testHashingAndTT() {
        val jsonString = File("src/test/resources/hashing_test_data.json").readText()
        val data = Gson().fromJson(jsonString, JsonObject::class.java)

        // 1. Sync Zobrist Tables
        val width = 16
        val height = 9
        val syncTable = data.getAsJsonArray("syncTable")
        val syncMyH = data.getAsJsonArray("syncMyH")
        val syncEnH = data.getAsJsonArray("syncEnH")

        val ktTable = Array(width) { x ->
            Array(height) { y ->
                val pieces = syncTable[x].asJsonArray[y].asJsonObject
                longArrayOf(
                    0L,
                    java.lang.Long.parseUnsignedLong(pieces.get("1").asString.removePrefix("0x"), 16),
                    java.lang.Long.parseUnsignedLong(pieces.get("2").asString.removePrefix("0x"), 16),
                    java.lang.Long.parseUnsignedLong(pieces.get("3").asString.removePrefix("0x"), 16)
                )
            }
        }
        val ktMyH = LongArray(101) { i -> java.lang.Long.parseUnsignedLong(syncMyH[i].asString.removePrefix("0x"), 16) }
        val ktEnH = LongArray(101) { i -> java.lang.Long.parseUnsignedLong(syncEnH[i].asString.removePrefix("0x"), 16) }

        SnakeZobrist.setManualTables(ktTable, ktMyH, ktEnH)

        // 2. Verify Hashes
        val cases = data.getAsJsonArray("testCases")
        for (element in cases) {
            val case = element.asJsonObject
            val cells = case.getAsJsonArray("cells").map { it.asInt }
            val grid = SnakeGrid(width, height)
            cells.forEachIndexed { i, v -> grid.set(i % width, i / width, v) }

            val ktHash = SnakeZobrist.computeHash(grid, case.get("myHp").asInt, case.get("enHp").asInt)
            val expectedHash = java.lang.Long.parseUnsignedLong(case.get("expectedHash").asString, 10)

            assertEquals("Hash mismatch for case", expectedHash, ktHash)
        }

        // 3. Verify TT logic
        SnakeTT.clear()
        val ttData = data.getAsJsonObject("ttTest")
        val hash = java.lang.Long.parseUnsignedLong(ttData.get("hash").asString.removePrefix("0x"), 16)

        SnakeTT.set(hash, ttData.get("depth").asInt, ttData.get("score").asDouble, ttData.get("flag").asInt, GridDir.UP)

        val entry = SnakeTT.get(hash)
        assertEquals(ttData.get("depth").asInt, entry?.depth)
        assertEquals(ttData.get("score").asDouble, entry?.score ?: 0.0, 0.001)
        assertEquals(GridDir.UP, entry?.move)

        // Test replacement strategy (higher depth replaces)
        SnakeTT.set(hash, 10, 2000.0, SnakeTT.EXACT, GridDir.DOWN)
        assertEquals(10, SnakeTT.get(hash)?.depth)

        // Lower depth should NOT replace
        SnakeTT.set(hash, 2, 50.0, SnakeTT.EXACT, GridDir.LEFT)
        assertEquals(10, SnakeTT.get(hash)?.depth)
    }
}