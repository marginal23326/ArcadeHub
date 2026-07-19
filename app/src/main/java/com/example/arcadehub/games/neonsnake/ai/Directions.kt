package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.GridDir

fun directionIndex(dir: GridDir): Int = when (dir) {
    GridDir.UP -> 0
    GridDir.DOWN -> 1
    GridDir.LEFT -> 2
    GridDir.RIGHT -> 3
}

fun directionFromIndex(index: Int): GridDir? = when (index) {
    0 -> GridDir.UP
    1 -> GridDir.DOWN
    2 -> GridDir.LEFT
    3 -> GridDir.RIGHT
    else -> null
}
