package com.example.arcadehub.core

object MathUtils {
    fun getAngleDiff(angle: Float, reference: Float): Float {
        var diff = angle - reference
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return diff
    }

    fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }

    fun distSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return dx * dx + dy * dy
    }

    fun isAABBCollision(
        x1: Float, y1: Float, w1: Float, h1: Float,
        x2: Float, y2: Float, w2: Float, h2: Float
    ): Boolean {
        return x1 < x2 + w2 &&
                x1 + w1 > x2 &&
                y1 < y2 + h2 &&
                y1 + h1 > y2
    }
}