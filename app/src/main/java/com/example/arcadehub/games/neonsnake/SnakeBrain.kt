package com.example.arcadehub.games.neonsnake

import com.example.arcadehub.games.neonsnake.ai.AiConfig
import com.example.arcadehub.games.neonsnake.ai.AiEngine

object SnakeBrain {

    fun getSmartMove(
        me: SnakeEntity,
        enemy: SnakeEntity,
        foods: List<Point>,
        cols: Int,
        rows: Int,
        depth: Int,
        mapType: SnakeMapGenerator.MapType
    ): GridDir {
        return AiEngine.decideMove(me, enemy, foods, cols, rows, mapType, AiConfig.default(maxDepth = depth))
    }
}
