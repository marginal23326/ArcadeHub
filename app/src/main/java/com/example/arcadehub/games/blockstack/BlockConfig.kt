package com.example.arcadehub.games.blockstack

import android.graphics.Color
import com.example.arcadehub.managers.SaveManager
import androidx.core.graphics.toColorInt

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
    val BG_COLOR = "#212121".toColorInt()
    val SHOP_BG_COLOR = "#101010".toColorInt()
    val SELECTION_COLOR = "#FFD700".toColorInt()

    val COLORS = listOf(
        "#F44336".toColorInt(), "#E91E63".toColorInt(), "#9C27B0".toColorInt(),
        "#673AB7".toColorInt(), "#3F51B5".toColorInt(), "#2196F3".toColorInt(),
        "#03A9F4".toColorInt(), "#00BCD4".toColorInt(), "#009688".toColorInt(),
        "#4CAF50".toColorInt(), "#8BC34A".toColorInt(), "#CDDC39".toColorInt(),
        "#FFEB3B".toColorInt(), "#FFC107".toColorInt(), "#FF9800".toColorInt(),
        "#FF5722".toColorInt()
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