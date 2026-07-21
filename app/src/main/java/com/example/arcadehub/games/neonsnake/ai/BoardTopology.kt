package com.example.arcadehub.games.neonsnake.ai

class BoardTopology(val width: Int, val height: Int, val numWords: Int) {

    val validCells: BitBoard = BitBoard(numWords)
    val notLeftEdge: BitBoard = BitBoard(numWords)
    val notRightEdge: BitBoard = BitBoard(numWords)

    init {
        notLeftEdge.fillAllOnes()
        notRightEdge.fillAllOnes()
        for (y in 0 until height) {
            notLeftEdge.unset(y * width)
            notRightEdge.unset(y * width + width - 1)
            for (x in 0 until width) {
                validCells.set(y * width + x)
            }
        }
    }

    /** Returns a copy of this topology with `blocked` cells removed from [validCells]. */
    fun withBlockedCells(blocked: BitBoard): BoardTopology {
        val copy = BoardTopology(width, height, numWords)
        copy.validCells.copyFrom(validCells)
        copy.validCells.andNotAssign(blocked)
        return copy
    }

    fun up(b: BitBoard, out: BitBoard) {
        val shift = width
        val inv = 64 - shift
        val w = b.words
        val o = out.words
        for (i in 0 until numWords - 1) {
            o[i] = (w[i] ushr shift) or (w[i + 1] shl inv)
        }
        o[numWords - 1] = w[numWords - 1] ushr shift
        out.andAssign(validCells)
    }

    fun down(b: BitBoard, out: BitBoard) {
        val shift = width
        val inv = 64 - shift
        val w = b.words
        val o = out.words
        for (i in numWords - 1 downTo 1) {
            o[i] = (w[i] shl shift) or (w[i - 1] ushr inv)
        }
        o[0] = w[0] shl shift
        out.andAssign(validCells)
    }

    fun left(b: BitBoard, out: BitBoard, scratch: BitBoard) {
        scratch.setToAnd(b, notLeftEdge)
        val w = scratch.words
        val o = out.words
        for (i in 0 until numWords - 1) {
            o[i] = (w[i] ushr 1) or (w[i + 1] shl 63)
        }
        o[numWords - 1] = w[numWords - 1] ushr 1
    }

    fun right(b: BitBoard, out: BitBoard, scratch: BitBoard) {
        scratch.setToAnd(b, notRightEdge)
        val w = scratch.words
        val o = out.words
        for (i in numWords - 1 downTo 1) {
            o[i] = (w[i] shl 1) or (w[i - 1] ushr 63)
        }
        o[0] = w[0] shl 1
    }

    /** `up(b) | down(b) | left(b) | right(b)`, written into [out]. Requires two scratch boards. */
    fun expandNeighbors(b: BitBoard, out: BitBoard, scratchA: BitBoard, scratchB: BitBoard) {
        up(b, out)
        down(b, scratchA)
        out.orAssign(scratchA)
        left(b, scratchA, scratchB)
        out.orAssign(scratchA)
        right(b, scratchA, scratchB)
        out.orAssign(scratchA)
    }
}
