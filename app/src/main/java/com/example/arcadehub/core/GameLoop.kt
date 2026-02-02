package com.example.arcadehub.core

/**
 * Utility for tracking time across frames.
 */
class GameLoop {
    private var lastTime: Long = System.nanoTime()

    /**
     * Calculates delta time in seconds and updates lastTime.
     * Returns a 'safe' dt to prevent massive jumps during lag.
     */
    fun calculateDeltaTime(): Float {
        val now = System.nanoTime()
        val elapsed = now - lastTime
        lastTime = now

        // Convert nanoseconds to seconds
        var dt = elapsed / 1_000_000_000f

        if (dt > 0.1f) dt = 0.1f

        return dt
    }

    fun reset() {
        lastTime = System.nanoTime()
    }
}