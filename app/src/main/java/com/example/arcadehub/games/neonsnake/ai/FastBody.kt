package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.Point

class FastBody private constructor(
    private val buffer: IntArray,
    private var headIdx: Int,
    var len: Int
) {
    constructor() : this(IntArray(MAX_BODY), 0, 0)

    fun copyOf(): FastBody = FastBody(buffer.copyOf(), headIdx, len)

    fun copyFrom(other: FastBody) {
        System.arraycopy(other.buffer, 0, buffer, 0, buffer.size)
        headIdx = other.headIdx
        len = other.len
    }

    fun clear() {
        headIdx = 0
        len = 0
    }

    fun isEmpty(): Boolean = len == 0

    fun headX(): Int = unpackX(buffer[headIdx])
    fun headY(): Int = unpackY(buffer[headIdx])

    fun x(index: Int): Int = unpackX(buffer[(headIdx + index) and MASK])
    fun y(index: Int): Int = unpackY(buffer[(headIdx + index) and MASK])

    fun lastX(): Int = x(len - 1)
    fun lastY(): Int = y(len - 1)

    fun pushFront(x: Int, y: Int) {
        headIdx = (headIdx - 1) and MASK
        buffer[headIdx] = pack(x, y)
        len++
    }

    fun popFront() {
        headIdx = (headIdx + 1) and MASK
        len--
    }

    /** Removes and returns the tail as a packed point; use [unpackX]/[unpackY] to decode. */
    fun popBack(): Int {
        val tailIdx = (headIdx + len - 1) and MASK
        len--
        return buffer[tailIdx]
    }

    fun pushBackPacked(packedPoint: Int) {
        val tailIdx = (headIdx + len) and MASK
        buffer[tailIdx] = packedPoint
        len++
    }

    fun pushBack(x: Int, y: Int) = pushBackPacked(pack(x, y))

    fun loadFrom(points: List<Point>) {
        headIdx = 0
        len = points.size
        for (i in points.indices) {
            buffer[i] = pack(points[i].x, points[i].y)
        }
    }

    fun forEachPoint(action: (x: Int, y: Int) -> Unit) {
        for (i in 0 until len) {
            val p = buffer[(headIdx + i) and MASK]
            action(unpackX(p), unpackY(p))
        }
    }

    companion object {
        const val MAX_BODY = 512
        const val MASK = MAX_BODY - 1

        private fun pack(x: Int, y: Int): Int = (x shl 16) or (y and 0xFFFF)
        fun unpackX(packed: Int): Int = packed shr 16
        fun unpackY(packed: Int): Int = (packed shl 16) shr 16 // sign-extend back from lower 16 bits
    }
}
