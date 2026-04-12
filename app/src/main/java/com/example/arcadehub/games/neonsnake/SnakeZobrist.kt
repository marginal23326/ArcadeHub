package com.example.arcadehub.games.neonsnake

import kotlin.random.Random

object SnakeZobrist {
    private var table: Array<Array<LongArray>>? = null
    private val myHealthTable = LongArray(101)
    private val enemyHealthTable = LongArray(101)

    private var isInit = false
    private var tableWidth = 0
    private var tableHeight = 0

    fun init(width: Int, height: Int) {
        if (isInit && tableWidth == width && tableHeight == height) return

        tableWidth = width
        tableHeight = height
        val random = Random(42) // Fixed seed for reproducibility or just Random.Default

        table = Array(width) {
            Array(height) {
                LongArray(4) { // Indices 1, 2, 3 used for Food, Me, Enemy
                    random.nextLong()
                }
            }
        }

        for (i in 0..100) {
            myHealthTable[i] = random.nextLong()
            enemyHealthTable[i] = random.nextLong()
        }

        isInit = true
    }

    fun computeHash(grid: SnakeGrid, myHealth: Int, enemyHealth: Int): Long {
        if (!isInit) init(grid.width, grid.height)

        var h = 0L
        val w = grid.width
        val hgt = grid.height

        for (x in 0 until w) {
            for (y in 0 until hgt) {
                val piece = grid[x, y]
                if (piece in 1..3) {
                    h = h xor table!![x][y][piece]
                }
            }
        }

        val myIdx = myHealth.coerceIn(0, 100)
        val enIdx = enemyHealth.coerceIn(0, 100)

        h = h xor myHealthTable[myIdx]
        h = h xor enemyHealthTable[enIdx]

        return h
    }

    fun xorPiece(currentHash: Long, x: Int, y: Int, piece: Int): Long {
        if (piece !in 1..3) return currentHash
        return currentHash xor table!![x][y][piece]
    }

    fun xorHealth(currentHash: Long, oldHealth: Int, newHealth: Int, isMe: Boolean): Long {
        val hTable = if (isMe) myHealthTable else enemyHealthTable

        val oIdx = oldHealth.coerceIn(0, 100)
        val nIdx = newHealth.coerceIn(0, 100)

        return currentHash xor hTable[oIdx] xor hTable[nIdx]
    }
}