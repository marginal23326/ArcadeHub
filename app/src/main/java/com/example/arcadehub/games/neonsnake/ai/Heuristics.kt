package com.example.arcadehub.games.neonsnake.ai

import kotlin.math.abs
import kotlin.math.pow

object Heuristics {

    fun evaluate(
        grid: AiGrid,
        me: SearchAgent,
        enemy: SearchAgent,
        distMap: IntArray?,
        cfg: AiConfig,
        buffers: SearchBuffers
    ): Int {
        val scores = cfg.scores

        if (me.health <= 0 || me.body.isEmpty()) return scores.loss
        if (enemy.health <= 0 || enemy.body.isEmpty()) return scores.win

        var score = 0

        val myLen = me.body.len
        val enemyLen = enemy.body.len
        val totalLen = myLen + enemyLen
        val totalArea = grid.width * grid.height
        val denseTailRace = myLen >= 20 && enemyLen >= 20 &&
            (totalLen * 100) >= (cfg.denseTailRaceOccupancyPercent * totalArea)

        score += myLen * scores.length

        val myHeadX = me.body.headX()
        val myHeadY = me.body.headY()
        val enemyHeadX = enemy.body.headX()
        val enemyHeadY = enemy.body.headY()

        // --- Corner/edge "pin" pressure: penalize being boxed against a wall near the enemy. ---
        if (me.health > 15) {
            val basePin = abs(scores.territoryControl) * 100
            var myPinPenalty = 0

            val dx = abs(myHeadX - enemyHeadX)
            val dy = abs(myHeadY - enemyHeadY)

            if (myHeadX == 0 || myHeadX == grid.width - 1) {
                val exDist = if (myHeadX == 0) enemyHeadX else grid.width - 1 - enemyHeadX
                if (exDist <= 3 && dy <= 4) myPinPenalty += basePin * (8 - (exDist + dy))
            }
            if (myHeadY == 0 || myHeadY == grid.height - 1) {
                val eyDist = if (myHeadY == 0) enemyHeadY else grid.height - 1 - enemyHeadY
                if (eyDist <= 3 && dx <= 4) myPinPenalty += basePin * (8 - (eyDist + dx))
            }

            var enemyPinPenalty = 0
            if (enemyHeadX == 0 || enemyHeadX == grid.width - 1) {
                val mxDist = if (enemyHeadX == 0) myHeadX else grid.width - 1 - myHeadX
                if (mxDist <= 3 && dy <= 4) enemyPinPenalty += basePin * (8 - (mxDist + dy))
            }
            if (enemyHeadY == 0 || enemyHeadY == grid.height - 1) {
                val myDist = if (enemyHeadY == 0) myHeadY else grid.height - 1 - myHeadY
                if (myDist <= 3 && dx <= 4) enemyPinPenalty += basePin * (8 - (myDist + dx))
            }

            score -= myPinPenalty
            score += enemyPinPenalty
        }

        // --- Voronoi territory control. Temporarily vacate my tail if it's about to move. ---
        var tailIsSafe = false
        var originalTailVal = 0
        val myTailX: Int
        val myTailY: Int
        if (!me.body.isEmpty()) {
            myTailX = me.body.lastX()
            myTailY = me.body.lastY()
            if (me.health < 100) {
                tailIsSafe = true
                originalTailVal = grid.get(myTailX, myTailY)
                grid.set(myTailX, myTailY, 0)
            }
        } else {
            myTailX = myHeadX
            myTailY = myHeadY
        }

        val voronoi = Voronoi.compute(grid, myHeadX, myHeadY, enemyHeadX, enemyHeadY, buffers)
        if (tailIsSafe) grid.set(myTailX, myTailY, originalTailVal)

        score += (voronoi.myCount - voronoi.enemyCount) * scores.territoryControl

        var iAmInDeathTrap = false

        if (voronoi.myCount < myLen) {
            val ff = FloodFill.run(grid, myHeadX, myHeadY, myLen + 2, me.body, enemy.body, buffers)
            val foodMod = if (ff.hasFood) 1 else 0
            val escapeTime = saturatingAdd(ff.minTurnsToClear, foodMod)
            val futureLen = myLen + foodMod

            if (ff.count < futureLen && ff.count < escapeTime) {
                iAmInDeathTrap = true
                val trapScore = if (denseTailRace) scores.trapDanger / 1000 else scores.trapDanger
                score += trapScore
            } else if (ff.count >= futureLen) {
                val tailX = me.body.lastX()
                val tailY = me.body.lastY()
                val distToTail = abs(myHeadX - tailX) + abs(myHeadY - tailY)
                if (distToTail <= 2 || escapeTime <= 2) {
                    score += scores.territoryControl * 5
                } else {
                    score += scores.strategicSqueeze
                }
            } else {
                score -= escapeTime * scores.territoryControl * 2
            }
        } else if (voronoi.myCount.toDouble() < totalArea.toDouble() * 0.2) {
            score += scores.tightSpot
        }

        if (!iAmInDeathTrap && voronoi.enemyCount < enemyLen) {
            val ff = FloodFill.run(grid, enemyHeadX, enemyHeadY, enemyLen + 2, enemy.body, me.body, buffers)
            val foodMod = if (ff.hasFood) 1 else 0
            val escapeTime = saturatingAdd(ff.minTurnsToClear, foodMod)
            val futureLen = enemyLen + foodMod

            if (ff.count < futureLen && ff.count < escapeTime) {
                val trapScore = if (denseTailRace) scores.enemyTrapped / 1000 else scores.enemyTrapped
                score += trapScore
            } else if (ff.count >= futureLen) {
                val tailX = enemy.body.lastX()
                val tailY = enemy.body.lastY()
                val distToTail = abs(enemyHeadX - tailX) + abs(enemyHeadY - tailY)
                if (distToTail <= 2 || escapeTime <= 2) {
                    score -= scores.territoryControl * 5
                } else {
                    score -= scores.strategicSqueeze
                }
            } else {
                score += escapeTime * scores.territoryControl * 2
            }
        }

        val distToOpp = abs(myHeadX - enemyHeadX) + abs(myHeadY - enemyHeadY)
        if (distToOpp == 1 && myLen > enemyLen) {
            score += scores.killPressure
        }

        if (grid.food.any()) {
            val closestDist = if (distMap != null) {
                distMap[myHeadY * grid.width + myHeadX]
            } else {
                var minDist = 9999
                val words = grid.food.words
                for (w in words.indices) {
                    var v = words[w]
                    while (v != 0L) {
                        val bit = java.lang.Long.numberOfTrailingZeros(v)
                        val idx = (w shl 6) or bit
                        val fx = idx % grid.width
                        val fy = idx / grid.width
                        val d = abs(myHeadX - fx) + abs(myHeadY - fy)
                        if (d < minDist) minDist = d
                        v = v and (v - 1)
                    }
                }
                minDist
            }

            if (closestDist > me.health) return scores.loss

            val buffer = me.health - closestDist
            val panicValue = if (buffer > 0) {
                scores.food.intensity * (scores.food.threshold / (buffer.toDouble() + 1.0)).pow(scores.food.exponent)
            } else {
                scores.food.intensity * 100.0
            }
            score -= (closestDist.toDouble() * panicValue).toInt()
        }

        if (myLen > enemyLen + 1) {
            score -= distToOpp * scores.aggression
        }

        return score
    }
}
