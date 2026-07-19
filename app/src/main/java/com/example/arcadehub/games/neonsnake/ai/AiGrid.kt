package com.example.arcadehub.games.neonsnake.ai

class AiGrid private constructor(
    val width: Int,
    val height: Int,
    val topology: BoardTopology,
    val food: BitBoard,
    val myBody: BitBoard,
    val enemyBody: BitBoard,
    val walls: BitBoard
) {
    val numWords: Int get() = topology.numWords

    fun copyOf(): AiGrid = AiGrid(width, height, topology, food.copyOf(), myBody.copyOf(), enemyBody.copyOf(), walls)

    fun copyFrom(other: AiGrid) {
        food.copyFrom(other.food)
        myBody.copyFrom(other.myBody)
        enemyBody.copyFrom(other.enemyBody)
    }

    fun idx(x: Int, y: Int): Int = y * width + x

    fun inBounds(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    /** 0 empty, 1 food, 2 my body, 3 enemy body, 9 wall/out-of-bounds. */
    fun get(x: Int, y: Int): Int {
        if (!inBounds(x, y)) return 9
        val i = idx(x, y)
        if (walls[i]) return 9
        if (myBody[i]) return 2
        if (enemyBody[i]) return 3
        if (food[i]) return 1
        return 0
    }

    fun set(x: Int, y: Int, value: Int) {
        if (!inBounds(x, y)) return
        val i = idx(x, y)
        food.unset(i)
        myBody.unset(i)
        enemyBody.unset(i)
        when (value) {
            1 -> food.set(i)
            2 -> myBody.set(i)
            3 -> enemyBody.set(i)
        }
    }

    /** Caller must ensure `(x, y)` is in bounds. */
    fun replaceUnchecked(x: Int, y: Int, oldValue: Int, newValue: Int) {
        val i = idx(x, y)
        when (oldValue) {
            1 -> food.unset(i)
            2 -> myBody.unset(i)
            3 -> enemyBody.unset(i)
        }
        when (newValue) {
            1 -> food.set(i)
            2 -> myBody.set(i)
            3 -> enemyBody.set(i)
        }
    }

    /** Caller must ensure `(x, y)` is in bounds. */
    fun clearUnchecked(x: Int, y: Int, oldValue: Int) {
        val i = idx(x, y)
        when (oldValue) {
            1 -> food.unset(i)
            2 -> myBody.unset(i)
            3 -> enemyBody.unset(i)
        }
    }

    /** True if `(x, y)` is on the board, not a wall, and not occupied by either body. */
    fun isSafe(x: Int, y: Int): Boolean {
        if (!inBounds(x, y)) return false
        val i = idx(x, y)
        return !walls[i] && !myBody[i] && !enemyBody[i]
    }

    fun occupiedInto(out: BitBoard) = out.setToOr(myBody, enemyBody)

    /** `validCells & !occupied` (walls are already excluded from validCells). */
    fun safeCellsInto(out: BitBoard, scratch: BitBoard) {
        occupiedInto(scratch)
        out.copyFrom(topology.validCells)
        out.andNotAssign(scratch)
    }

    companion object {
        fun create(width: Int, height: Int, wallCells: BitBoard? = null): AiGrid {
            val numWords = BitBoard.wordsFor(width * height)
            val base = BoardTopology(width, height, numWords)
            val walls = wallCells ?: BitBoard(numWords)
            val topology = if (walls.any()) base.withBlockedCells(walls) else base
            return AiGrid(width, height, topology, BitBoard(numWords), BitBoard(numWords), BitBoard(numWords), walls)
        }
    }
}
