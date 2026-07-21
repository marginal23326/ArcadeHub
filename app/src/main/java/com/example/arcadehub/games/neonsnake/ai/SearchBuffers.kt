package com.example.arcadehub.games.neonsnake.ai

class SearchBuffers(width: Int, height: Int) {
    val gridSize = width * height
    private val numWords = BitBoard.wordsFor(gridSize)

    // --- Flood fill (floodfill.rs) ---
    val ffFront = BitBoard(numWords)
    val ffVisited = BitBoard(numWords)
    val ffExpanded = BitBoard(numWords)
    val ffHits = BitBoard(numWords)
    val ffBodyMask = BitBoard(numWords)
    val ffMyMask = BitBoard(numWords)
    val ffEnMask = BitBoard(numWords)
    val ffSafeCells = BitBoard(numWords)
    val ffOccupiedScratch = BitBoard(numWords)
    val ffShiftScratchA = BitBoard(numWords)
    val ffShiftScratchB = BitBoard(numWords)
    val vanishMap = IntArray(gridSize)

    // --- Voronoi ---
    val vMyFront = BitBoard(numWords)
    val vEnFront = BitBoard(numWords)
    val vMyTerritory = BitBoard(numWords)
    val vEnTerritory = BitBoard(numWords)
    val vVisited = BitBoard(numWords)
    val vUnvisited = BitBoard(numWords)
    val vTies = BitBoard(numWords)
    val vActive = BitBoard(numWords)
    val vOccupiedScratch = BitBoard(numWords)
    val vShiftScratchA = BitBoard(numWords)
    val vShiftScratchB = BitBoard(numWords)
    val vShiftScratchC = BitBoard(numWords)
    val vShiftScratchD = BitBoard(numWords)

    // --- Pathfinding ---
    val pfFront = BitBoard(numWords)
    val pfExpanded = BitBoard(numWords)
    val pfVisited = BitBoard(numWords)
    val pfSafeCells = BitBoard(numWords)
    val pfOccupiedScratch = BitBoard(numWords)
    val pfPopScratch = BitBoard(numWords)
    val pfShiftScratchA = BitBoard(numWords)
    val pfShiftScratchB = BitBoard(numWords)

    // --- Move generation ---
    val moveGenOccupiedScratch = BitBoard(numWords)

    val moveListPool = Array(MAX_PLY) { MoveList() }

    val moveOrderDistScratch = IntArray(4)

    // --- Transposition table probes ---
    val ttProbe = TtProbe()

    val leafCheckMoveListA = MoveList()
    val leafCheckMoveListB = MoveList()

    companion object {
        const val MAX_PLY = 256
    }
}

class MoveList {
    val moves = IntArray(4)
    var count = 0

    fun clear() {
        count = 0
    }

    fun push(x: Int, y: Int, dirInt: Int) {
        moves[count] = (x shl 16) or (y shl 8) or dirInt
        count++
    }

    fun swap(i: Int, j: Int) {
        val t = moves[i]
        moves[i] = moves[j]
        moves[j] = t
    }

    fun x(i: Int): Int = moves[i] ushr 16
    fun y(i: Int): Int = (moves[i] ushr 8) and 0xFF
    fun dirInt(i: Int): Int = moves[i] and 0xFF

    companion object {
        fun xOfPacked(packed: Int): Int = packed ushr 16
        fun yOfPacked(packed: Int): Int = (packed ushr 8) and 0xFF
        fun dirIntOfPacked(packed: Int): Int = packed and 0xFF
    }
}
