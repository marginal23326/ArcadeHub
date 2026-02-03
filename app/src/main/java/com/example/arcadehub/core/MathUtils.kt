package com.example.arcadehub.core

object MathUtils {
    fun getAngleDiff(angle: Float, reference: Float): Float {
        var diff = angle - reference
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return diff
    }
}