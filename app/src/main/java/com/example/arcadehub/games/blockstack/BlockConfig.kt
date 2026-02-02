package com.example.arcadehub.games.blockstack

import android.graphics.Color
import com.example.arcadehub.managers.SaveManager

object BlockConfig {
    // --- DIMENSIONS (Based on 1920x1080 Logic) ---
    const val BLOCK_HEIGHT = 75f

    // --- ORBIT MODE SPECIFICS ---
    const val INITIAL_RADIUS = 200f
    const val INITIAL_ARC_LENGTH = 100f // Degrees

    // --- PHYSICS ---
    const val BASE_SPEED = 15f
    const val SPEED_INCREMENT = 0.5f
    const val MAX_SPEED_SCORE = 35
    const val GRAVITY = 2.5f

    // --- ACCURACY ---
    const val PERFECT_TOLERANCE_PIXELS = 30f
    const val PERFECT_TOLERANCE_DEGREES = 5f
    const val THRESHOLD_PERFECT_EARLY = 0.98f
    const val THRESHOLD_PERFECT_LATE = 0.90f
    const val THRESHOLD_GREAT = 0.75f
    const val THRESHOLD_NICE = 0.50f
    const val PERFECT_PIVOT_SCORE = 30

    // --- REWARDS ---
    const val COIN_REWARD_NICE = 1
    const val COIN_REWARD_GREAT = 3
    const val COIN_REWARD_PERFECT_BASE = 4

    // --- ABILITIES ---
    const val SLOMO_DURATION_TURNS = 3
    const val SLOMO_FACTOR = 0.25f
    const val MAGNET_TOLERANCE_MULTIPLIER = 5.0f
    const val WIDENER_PERCENT = 0.20f

    // --- COLORS ---
    val BG_COLOR = Color.parseColor("#212121")
    val SHOP_BG_COLOR = Color.parseColor("#101010")
    val SELECTION_COLOR = Color.parseColor("#FFD700")

    val COLORS = listOf(
        Color.parseColor("#F44336"), Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"),
        Color.parseColor("#673AB7"), Color.parseColor("#3F51B5"), Color.parseColor("#2196F3"),
        Color.parseColor("#03A9F4"), Color.parseColor("#00BCD4"), Color.parseColor("#009688"),
        Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A"), Color.parseColor("#CDDC39"),
        Color.parseColor("#FFEB3B"), Color.parseColor("#FFC107"), Color.parseColor("#FF9800"),
        Color.parseColor("#FF5722")
    )

    const val COLOR_PERFECT = 0xFF00FFFF.toInt()
    const val COLOR_GREAT = 0xFF00FF00.toInt()
    const val COLOR_NICE = 0xFFFFFFFF.toInt()
    const val COLOR_COIN = 0xFFFFD700.toInt()

    const val SHAKE_DURATION = 15
    const val SHAKE_INTENSITY = 20f
    const val GAME_OVER_COOLDOWN_MS = 500L
    const val ZOOM_SMOOTHING = 0.1f
}

// --- ECONOMY HELPER ---
object BlockEconomy {
    private const val PREF_COINS = "BS_COINS"
    private const val PREF_PREFIX_INV = "BS_INV_"

    fun getCoins(): Int = SaveManager.getInt(PREF_COINS, 0)

    fun addCoins(amount: Int) {
        val current = getCoins()
        SaveManager.setInt(PREF_COINS, current + amount)
    }

    fun getInventoryCount(type: AbilityType): Int {
        return SaveManager.getInt(PREF_PREFIX_INV + type.name, 0)
    }

    fun buyItem(type: AbilityType): Boolean {
        val coins = getCoins()
        val stock = getInventoryCount(type)

        if (coins >= type.cost && stock < type.maxStock) {
            SaveManager.setInt(PREF_COINS, coins - type.cost)
            SaveManager.setInt(PREF_PREFIX_INV + type.name, stock + 1)
            return true
        }
        return false
    }

    fun useItem(type: AbilityType): Boolean {
        val stock = getInventoryCount(type)
        if (stock > 0) {
            SaveManager.setInt(PREF_PREFIX_INV + type.name, stock - 1)
            return true
        }
        return false
    }
}