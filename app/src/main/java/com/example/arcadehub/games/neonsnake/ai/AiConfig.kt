package com.example.arcadehub.games.neonsnake.ai

data class FoodCurveConfig(
    val intensity: Double,
    val threshold: Double,
    val exponent: Double
)

data class ScoreConfig(
    val win: Int,
    val loss: Int,
    val draw: Int,
    val trapDanger: Int,
    val strategicSqueeze: Int,
    val enemyTrapped: Int,
    val headOnCollision: Int,
    val tightSpot: Int,
    val length: Int,
    val eatReward: Int,
    val territoryControl: Int,
    val killPressure: Int,
    val food: FoodCurveConfig,
    val aggression: Int
)

data class RuntimeConfig(
    val threads: Int = 0,
    val hashEntries: Int = 0
)

data class AiConfig(
    val maxDepth: Int,
    val denseTailRaceOccupancyPercent: Int,
    val runtime: RuntimeConfig = RuntimeConfig(),
    val scores: ScoreConfig
) {
    companion object {
        fun default(maxDepth: Int = 16): AiConfig = AiConfig(
            maxDepth = maxDepth,
            denseTailRaceOccupancyPercent = 50,
            scores = ScoreConfig(
                win = 1_000_000_000,
                loss = -1_000_000_000,
                draw = -100_000_000,
                trapDanger = -413_704_270,
                strategicSqueeze = -18_960_904,
                enemyTrapped = 320_798_923,
                headOnCollision = -140_956_186,
                tightSpot = -76_753,
                length = 1_000,
                eatReward = 2_000,
                territoryControl = 3_265,
                killPressure = 66_319,
                food = FoodCurveConfig(
                    intensity = 3_303.092,
                    threshold = 19.357,
                    exponent = 1.968
                ),
                aggression = 7_596
            )
        )
    }
}
