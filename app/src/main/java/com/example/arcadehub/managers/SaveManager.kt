package com.example.arcadehub.managers

import android.content.Context
import android.content.SharedPreferences

object SaveManager {
    private const val PREFS_NAME = "ArcadeHubPrefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Generic Getters/Setters ---

    fun getInt(key: String, default: Int = 0): Int {
        return prefs.getInt(key, default)
    }

    fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return prefs.getBoolean(key, default)
    }

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    // --- Helper for High Scores ---

    /**
     * Updates the high score only if the new score is higher.
     * Returns true if a new high score was set.
     */
    fun saveHighScore(key: String, score: Int): Boolean {
        val current = getInt(key, 0)
        if (score > current) {
            setInt(key, score)
            return true
        }
        return false
    }
}