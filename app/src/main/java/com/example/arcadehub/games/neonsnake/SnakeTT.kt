package com.example.arcadehub.games.neonsnake

object SnakeTT {
    const val EXACT = 0
    const val LOWERBOUND = 1
    const val UPPERBOUND = 2

    data class Entry(
        val depth: Int,
        val score: Double,
        val flag: Int,
        val move: GridDir?
    )

    private val table = HashMap<Long, Entry>(16384)

    fun clear() {
        table.clear()
    }

    fun set(hash: Long, depth: Int, score: Double, flag: Int, move: GridDir?) {
        val existing = table[hash]
        // Replacement strategy: Always replace if current search is deeper or equal
        if (existing == null || existing.depth <= depth) {
            table[hash] = Entry(depth, score, flag, move)
        }
    }

    fun get(hash: Long): Entry? = table[hash]
}