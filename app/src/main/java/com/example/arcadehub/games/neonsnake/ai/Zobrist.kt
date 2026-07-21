package com.example.arcadehub.games.neonsnake.ai

class Zobrist private constructor(
    val width: Int,
    val height: Int,
    private val pieceTable: LongArray, // [cellIdx * 4 + piece], piece in 1..3
    private val healthTable: Array<LongArray> // [side][0..100]
) {
    fun computeHash(grid: AiGrid, myHealth: Int, enemyHealth: Int): Long {
        var h = 0L
        h = xorBits(h, grid.food, 1)
        h = xorBits(h, grid.myBody, 2)
        h = xorBits(h, grid.enemyBody, 3)
        h = h xor healthTable[0][clampHealth(myHealth)]
        h = h xor healthTable[1][clampHealth(enemyHealth)]
        return h
    }

    private fun xorBits(hashIn: Long, bb: BitBoard, piece: Int): Long {
        var h = hashIn
        val words = bb.words
        for (w in words.indices) {
            var v = words[w]
            while (v != 0L) {
                val bit = java.lang.Long.numberOfTrailingZeros(v)
                val idx = (w shl 6) or bit
                h = h xor pieceTable[idx * 4 + piece]
                v = v and (v - 1)
            }
        }
        return h
    }

    /** Caller must ensure `(x, y)` is in bounds and `piece` is in `1..3`. */
    fun xorUnchecked(currentHash: Long, x: Int, y: Int, piece: Int): Long {
        val idx = y * width + x
        return currentHash xor pieceTable[idx * 4 + piece]
    }

    fun xorHealth(currentHash: Long, side: Int, oldHealth: Int, newHealth: Int): Long {
        val table = healthTable[side]
        return currentHash xor table[clampHealth(oldHealth)] xor table[clampHealth(newHealth)]
    }

    companion object {
        private fun clampHealth(v: Int): Int = v.coerceIn(0, 100)

        fun build(width: Int, height: Int): Zobrist {
            val seed = GOLDEN_GAMMA xor (width.toLong() shl 32) xor (height.toLong() shl 8)
            val rng = SplitMix64(seed)

            val size = width * height
            val pieceTable = LongArray(size * 4)
            for (i in 0 until size) {
                pieceTable[i * 4 + 1] = rng.next()
                pieceTable[i * 4 + 2] = rng.next()
                pieceTable[i * 4 + 3] = rng.next()
            }

            val healthTable = arrayOf(LongArray(101), LongArray(101))
            for (i in 0..100) {
                healthTable[0][i] = rng.next()
                healthTable[1][i] = rng.next()
            }

            return Zobrist(width, height, pieceTable, healthTable)
        }
    }
}

object ZobristCache {
    @Volatile
    private var cached: Zobrist? = null

    fun forSize(width: Int, height: Int): Zobrist {
        val existing = cached
        if (existing != null && existing.width == width && existing.height == height) return existing
        synchronized(this) {
            val recheck = cached
            if (recheck != null && recheck.width == width && recheck.height == height) return recheck
            val built = Zobrist.build(width, height)
            cached = built
            return built
        }
    }
}

/** 0x9E3779B97F4A7C15 as a signed Long. */
private const val GOLDEN_GAMMA = -7046029254386353131L

/** SplitMix64 PRNG */
private class SplitMix64(seed: Long) {
    private var state: Long = seed

    fun next(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX1
        z = (z xor (z ushr 27)) * MIX2
        return z xor (z ushr 31)
    }

    private companion object {
        const val MIX1 = -4658895280553007687L // 0xBF58476D1CE4E5B9
        const val MIX2 = -7723592293110705685L // 0x94D049BB133111EB
    }
}
