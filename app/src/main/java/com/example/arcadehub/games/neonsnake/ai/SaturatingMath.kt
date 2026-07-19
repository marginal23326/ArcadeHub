package com.example.arcadehub.games.neonsnake.ai

fun saturatingAdd(a: Int, b: Int): Int {
    val sum = a.toLong() + b.toLong()
    return when {
        sum > Int.MAX_VALUE -> Int.MAX_VALUE
        sum < Int.MIN_VALUE -> Int.MIN_VALUE
        else -> sum.toInt()
    }
}
