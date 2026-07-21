package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.GridDir
import java.util.concurrent.atomic.AtomicIntegerArray

const val TT_FLAG_EXACT = 0
const val TT_FLAG_LOWER = 1
const val TT_FLAG_UPPER = 2

private const val NO_MOVE = 255

class TtProbe {
    var found: Boolean = false
    var score: Int = 0
    var depth: Int = 0
    var flag: Int = 0
    var mvX: Int = NO_MOVE
    var mvY: Int = NO_MOVE
    var mvDir: Int = NO_MOVE

    fun hasMove(): Boolean = mvX != NO_MOVE && mvY != NO_MOVE
}

data class TtEntry(val score: Int, val depth: Int, val flag: Int, val mvX: Int, val mvY: Int, val mvDir: Int) {
    fun hasMove(): Boolean = mvX != NO_MOVE && mvY != NO_MOVE

    fun direction(): GridDir? = directionFromIndex(mvDir)
}

class TranspositionTable(initialEntries: Int) {

    @Volatile private var mask: Int = 0
    @Volatile private var seq: AtomicIntegerArray = AtomicIntegerArray(0)
    @Volatile private var key: LongArray = LongArray(0)
    @Volatile private var score: IntArray = IntArray(0)

    // Packed as generation:16 | depth:8 | flag:8 | mvX:8 | mvY:8 | mvDir:8 (56 of 64 bits used).
    @Volatile private var meta: LongArray = LongArray(0)

    var generation: Int = 0
        private set

    init {
        resizeLocked(nextPowerOfTwo(initialEntries.coerceAtLeast(1)))
    }

    private fun resizeLocked(newSize: Int) {
        seq = AtomicIntegerArray(newSize)
        key = LongArray(newSize)
        score = IntArray(newSize)
        meta = LongArray(newSize)
        mask = newSize - 1
    }

    @Synchronized
    fun prepareForSearch(requestedEntries: Int) {
        val size = nextPowerOfTwo(requestedEntries.coerceAtLeast(1))
        if (size > key.size) {
            resizeLocked(size)
        }
        generation = (generation + 1) and 0xFFFF
        if (generation == 0) {
            // 16-bit generation counter wrapped: a stale entry could alias a "current" one again.
            generation = 1
            for (i in 0 until seq.length()) seq.set(i, 0)
        }
    }

    private fun indexFor(hash: Long): Int = hash.toInt() and mask

    fun get(hash: Long): TtEntry? {
        val probe = TtProbe()
        return if (probeInto(hash, probe)) {
            TtEntry(probe.score, probe.depth, probe.flag, probe.mvX, probe.mvY, probe.mvDir)
        } else {
            null
        }
    }

    fun probe(hash: Long, out: TtProbe): Boolean = probeInto(hash, out)

    private fun probeInto(hash: Long, out: TtProbe): Boolean {
        val localSeq = seq
        val localKey = key
        val localScore = score
        val localMeta = meta
        val idx = hash.toInt() and mask

        var backoff = 0
        while (true) {
            val seq1 = localSeq.get(idx)
            if (seq1 and 1 != 0) {
                backoff++
                if (backoff > 10) {
                    out.found = false
                    return false
                }
                continue
            }

            val k = localKey[idx]
            val sc = localScore[idx]
            val m = localMeta[idx]
            val seq2 = localSeq.get(idx)

            if (seq1 == seq2) {
                if (k == hash && metaGeneration(m) == generation) {
                    out.found = true
                    out.score = sc
                    out.depth = metaDepth(m)
                    out.flag = metaFlag(m)
                    out.mvX = metaMvX(m)
                    out.mvY = metaMvY(m)
                    out.mvDir = metaMvDir(m)
                    return true
                }
                out.found = false
                return false
            }
            // Entry changed mid-read; retry.
        }
    }

    fun set(hash: Long, depth: Int, scoreValue: Int, flag: Int, mvX: Int, mvY: Int, mvDir: Int) {
        val localSeq = seq
        val localKey = key
        val localScore = score
        val localMeta = meta
        val idx = hash.toInt() and mask

        var s = localSeq.get(idx)
        while (true) {
            if (s and 1 != 0) return // another writer owns this slot right now; drop this write
            if (localSeq.compareAndSet(idx, s, s + 1)) break
            s = localSeq.get(idx)
        }

        val depthByte = depth.coerceIn(0, 255)
        val currentMeta = localMeta[idx]
        if (metaGeneration(currentMeta) != generation || localKey[idx] != hash || depthByte >= metaDepth(currentMeta)) {
            localKey[idx] = hash
            localScore[idx] = scoreValue
            localMeta[idx] = packMeta(generation, depthByte, flag, mvX, mvY, mvDir)
        }

        localSeq.set(idx, s + 2)
    }

    companion object {
        fun nextPowerOfTwo(v: Int): Int {
            var x = v - 1
            x = x or (x ushr 1)
            x = x or (x ushr 2)
            x = x or (x ushr 4)
            x = x or (x ushr 8)
            x = x or (x ushr 16)
            return x + 1
        }

        fun depthBasedEntries(maxDepth: Int): Int = when {
            maxDepth <= 4 -> 1 shl 13 // 8K entries (~192KB)
            maxDepth <= 8 -> 1 shl 15 // 32K entries (~768KB)
            maxDepth <= 12 -> 1 shl 17 // 128K entries (~3MB)
            maxDepth <= 20 -> 1 shl 19 // 512K entries (~12MB)
            else -> 1 shl 20 // 1M entries (~24MB)
        }

        private fun packMeta(generation: Int, depth: Int, flag: Int, mvX: Int, mvY: Int, mvDir: Int): Long =
            (generation.toLong() and 0xFFFFL shl 40) or
                (depth.toLong() and 0xFFL shl 32) or
                (flag.toLong() and 0xFFL shl 24) or
                (mvX.toLong() and 0xFFL shl 16) or
                (mvY.toLong() and 0xFFL shl 8) or
                (mvDir.toLong() and 0xFFL)

        private fun metaGeneration(m: Long): Int = ((m ushr 40) and 0xFFFFL).toInt()
        private fun metaDepth(m: Long): Int = ((m ushr 32) and 0xFFL).toInt()
        private fun metaFlag(m: Long): Int = ((m ushr 24) and 0xFFL).toInt()
        private fun metaMvX(m: Long): Int = ((m ushr 16) and 0xFFL).toInt()
        private fun metaMvY(m: Long): Int = ((m ushr 8) and 0xFFL).toInt()
        private fun metaMvDir(m: Long): Int = (m and 0xFFL).toInt()
    }
}

object SharedTranspositionTable {
    @Volatile private var instance: TranspositionTable? = null

    fun get(): TranspositionTable {
        val existing = instance
        if (existing != null) return existing
        synchronized(this) {
            val recheck = instance
            if (recheck != null) return recheck
            val created = TranspositionTable(1 shl 15)
            instance = created
            return created
        }
    }

    fun reset() {
        synchronized(this) {
            instance = null
        }
    }
}
