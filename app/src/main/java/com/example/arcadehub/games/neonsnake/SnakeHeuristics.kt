package com.example.arcadehub.games.neonsnake

import kotlin.math.atan

object SnakeHeuristics {

    data class State(
        val me: SnakeEntity,
        val enemy: SnakeEntity,
        val foods: List<Point>,
        val distMap: IntArray?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as State

            if (me != other.me) return false
            if (enemy != other.enemy) return false
            if (foods != other.foods) return false
            if (distMap != null) {
                if (other.distMap == null) return false
                if (!distMap.contentEquals(other.distMap)) return false
            } else if (other.distMap != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = me.hashCode()
            result = 31 * result + enemy.hashCode()
            result = 31 * result + foods.hashCode()
            result = 31 * result + (distMap?.contentHashCode() ?: 0)
            return result
        }
    }

    fun evaluate(grid: SnakeGrid, state: State): Double {
        val me = state.me
        val enemy = state.enemy

        if (me.health <= 0) return SnakeConfig.Scores.LOSS
        if (enemy.health <= 0) return SnakeConfig.Scores.WIN

        var score = 0.0

        // 1. Length
        score += me.body.size * SnakeConfig.Scores.LENGTH

        val myHead = me.head()
        val enemyHead = enemy.head()

        // Handle Tail Safety
        var tailIsSafe = false
        var originalTailVal = 0
        val tail = me.body.last()
        if (me.health < 100) {
            tailIsSafe = true
            originalTailVal = grid[tail.x, tail.y]
            grid[tail.x, tail.y] = 0
        }

        // 2. Voronoi Territory Control
        val voronoi = SnakeAlgorithms.computeVoronoi(grid, myHead, enemyHead)

        // Restore tail
        if (tailIsSafe) grid[tail.x, tail.y] = originalTailVal

        // A. Territory Score
        score += (voronoi.myCount - voronoi.enemyCount) * SnakeConfig.Scores.TERRITORY_CONTROL

        var iAmInDeathTrap = false

        if (voronoi.myCount < me.body.size) {
            val ffResult = SnakeAlgorithms.floodFill(grid, myHead.x, myHead.y, me.body.size + 2, me.body)

            val physicalSpace = ffResult.count
            val adjustedEscapeTime = ffResult.minTurnsToClear + (if (ffResult.hasFood) 1 else 0)
            val futureLength = me.body.size + (if (ffResult.hasFood) 1 else 0)

            val isTrapped = (physicalSpace < futureLength) && (physicalSpace < adjustedEscapeTime)

            if (isTrapped) {
                iAmInDeathTrap = true
                score += SnakeConfig.Scores.TRAP_DANGER
            } else if (physicalSpace >= futureLength) {
                score += SnakeConfig.Scores.STRATEGIC_SQUEEZE
            }
        } else if (voronoi.myCount < grid.width * grid.height * 0.2) {
            score += SnakeConfig.Scores.TIGHT_SPOT
        }

        // 3. Enemy Trap Detection
        if (!iAmInDeathTrap && voronoi.enemyCount < enemy.body.size) {
            val enFF = SnakeAlgorithms.floodFill(grid, enemyHead.x, enemyHead.y, enemy.body.size + 2, enemy.body)

            val enSpace = enFF.count
            val enTimeToEscape = enFF.minTurnsToClear
            val enFutureLen = enemy.body.size + (if (enFF.hasFood) 1 else 0)

            if (enSpace < enFutureLen && enSpace < enTimeToEscape) {
                score += SnakeConfig.Scores.ENEMY_TRAPPED
            }
        }

        // 4. Tactical Kill Pressure
        val distToOpp = SnakeAlgorithms.manhattan(myHead, enemyHead)
        if (distToOpp == 1 && me.body.size > enemy.body.size) {
            score += SnakeConfig.Scores.KILL_PRESSURE
        }

        // 5. Food
        var foodScore = 0.0
        if (state.foods.isNotEmpty()) {
            var closestDist = 9999
            if (state.distMap != null) {
                val headIdx = myHead.y * grid.width + myHead.x
                if (headIdx in state.distMap.indices) {
                    closestDist = state.distMap[headIdx]
                }
            } else {
                state.foods.forEach { f ->
                    val d = SnakeAlgorithms.manhattan(myHead, f)
                    if (d < closestDist) closestDist = d
                }
            }

            if (closestDist > me.health) {
                return SnakeConfig.Scores.LOSS // Starvation
            }

            if (closestDist < 1000) {
                val rope = me.health - closestDist
                foodScore = SnakeConfig.Scores.FOOD_CURVE_INTENSITY *
                        atan(rope / SnakeConfig.Scores.FOOD_CURVE_EXPONENT)
            } else {
                foodScore = SnakeConfig.Scores.LOSS / 2.0
            }
        }
        score += foodScore

        // 6. Aggression
        var aggroScore = 0.0
        if (me.body.size > enemy.body.size + 1) {
            aggroScore = -(distToOpp * SnakeConfig.Scores.AGGRESSION)
        }
        score += aggroScore

        return score
    }
}