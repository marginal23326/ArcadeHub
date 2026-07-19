package com.example.arcadehub.games.neonsnake.ai

class BitBoard(val words: LongArray) {

    constructor(numWords: Int) : this(LongArray(numWords))

    inline val numWords: Int get() = words.size

    fun copyOf(): BitBoard = BitBoard(words.copyOf())

    fun copyFrom(other: BitBoard) {
        System.arraycopy(other.words, 0, words, 0, words.size)
    }

    fun clear() {
        words.fill(0L)
    }

    fun fillAllOnes() {
        words.fill(-1L)
    }

    fun set(idx: Int) {
        words[idx ushr 6] = words[idx ushr 6] or (1L shl (idx and 63))
    }

    fun unset(idx: Int) {
        words[idx ushr 6] = words[idx ushr 6] and (1L shl (idx and 63)).inv()
    }

    operator fun get(idx: Int): Boolean =
        (words[idx ushr 6] ushr (idx and 63)) and 1L == 1L

    fun countOnes(): Int {
        var count = 0
        for (w in words) count += java.lang.Long.bitCount(w)
        return count
    }

    fun isEmpty(): Boolean {
        for (w in words) if (w != 0L) return false
        return true
    }

    fun any(): Boolean = !isEmpty()

    /** Removes and returns the index of the lowest set bit, or -1 if this board is empty. */
    fun popFirst(): Int {
        for (i in words.indices) {
            val w = words[i]
            if (w != 0L) {
                val bit = java.lang.Long.numberOfTrailingZeros(w)
                words[i] = w and (w - 1) // clear the lowest set bit
                return (i shl 6) or bit
            }
        }
        return -1
    }

    infix fun and(other: BitBoard): BitBoard {
        val out = LongArray(words.size)
        for (i in words.indices) out[i] = words[i] and other.words[i]
        return BitBoard(out)
    }

    infix fun or(other: BitBoard): BitBoard {
        val out = LongArray(words.size)
        for (i in words.indices) out[i] = words[i] or other.words[i]
        return BitBoard(out)
    }

    infix fun xor(other: BitBoard): BitBoard {
        val out = LongArray(words.size)
        for (i in words.indices) out[i] = words[i] xor other.words[i]
        return BitBoard(out)
    }

    fun inverted(): BitBoard {
        val out = LongArray(words.size)
        for (i in words.indices) out[i] = words[i].inv()
        return BitBoard(out)
    }

    fun andAssign(other: BitBoard) {
        for (i in words.indices) words[i] = words[i] and other.words[i]
    }

    fun orAssign(other: BitBoard) {
        for (i in words.indices) words[i] = words[i] or other.words[i]
    }

    fun xorAssign(other: BitBoard) {
        for (i in words.indices) words[i] = words[i] xor other.words[i]
    }

    fun invertAssign() {
        for (i in words.indices) words[i] = words[i].inv()
    }

    /** `this &= !other` */
    fun andNotAssign(other: BitBoard) {
        for (i in words.indices) words[i] = words[i] and other.words[i].inv()
    }

    fun setToAnd(a: BitBoard, b: BitBoard) {
        for (i in words.indices) words[i] = a.words[i] and b.words[i]
    }

    fun setToOr(a: BitBoard, b: BitBoard) {
        for (i in words.indices) words[i] = a.words[i] or b.words[i]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BitBoard) return false
        return words.contentEquals(other.words)
    }

    override fun hashCode(): Int = words.contentHashCode()

    override fun toString(): String = buildString {
        append("BitBoard(")
        for (i in words.indices.reversed()) append(java.lang.Long.toHexString(words[i]).padStart(16, '0'))
        append(')')
    }

    companion object {
        fun empty(numWords: Int) = BitBoard(numWords)

        fun withBit(numWords: Int, idx: Int): BitBoard {
            val b = BitBoard(numWords)
            b.set(idx)
            return b
        }

        /** Number of 64-bit words needed to store `bits` bits. */
        fun wordsFor(bits: Int): Int = (bits + 63) ushr 6
    }
}
