package com.example.arcadehub.games.orbithop

import com.example.arcadehub.managers.SoundManager
import kotlin.math.*
import kotlin.random.Random

class OrbitPhysics {
    val player = Player()
    val pivots = ArrayList<Pivot>()

    // Ring Buffer for Trail
    val trailX = FloatArray(OrbitConfig.TRAIL_LENGTH)
    val trailY = FloatArray(OrbitConfig.TRAIL_LENGTH)
    var trailHead = 0
    var trailCount = 0

    var score = 0
        private set
    var cameraY = 0f
        private set

    var currentOrbitSpeed = OrbitConfig.ORBIT_SPEED_START

    fun reset(screenWidth: Int, screenHeight: Int) {
        pivots.clear()
        score = 0
        cameraY = 0f
        currentOrbitSpeed = OrbitConfig.ORBIT_SPEED_START
        trailCount = 0
        trailHead = 0

        val startX = screenWidth / 2f
        val startY = screenHeight - 300f

        val startPivot = Pivot(0, startX, startY, isTarget = false, dir = if(Random.nextBoolean()) 1 else -1)
        pivots.add(startPivot)

        player.x = startX
        player.y = startY
        player.vx = 0f
        player.vy = 0f
        player.angle = -PI.toFloat() / 2f
        player.state = PlayerState.ATTACHED
        player.currentPivot = startPivot

        spawnNextPivot(screenWidth)
    }

    private fun spawnNextPivot(screenWidth: Int) {
        val last = pivots.last()
        val distY = Random.nextFloat() * (OrbitConfig.MAX_Y_DIST - OrbitConfig.MIN_Y_DIST) + OrbitConfig.MIN_Y_DIST
        val newY = last.y - distY

        var newX = Random.nextFloat() * (screenWidth - OrbitConfig.HORIZONTAL_PADDING * 2) + OrbitConfig.HORIZONTAL_PADDING

        newX = newX.coerceIn((last.x - OrbitConfig.MAX_X_HOP), (last.x + OrbitConfig.MAX_X_HOP))
        newX = newX.coerceIn(OrbitConfig.HORIZONTAL_PADDING, screenWidth.toFloat() - OrbitConfig.HORIZONTAL_PADDING)

        val nextPivot = Pivot(
            id = last.id + 1,
            x = newX,
            y = newY,
            isTarget = true,
            dir = if (Random.nextBoolean()) 1 else -1
        )
        pivots.add(nextPivot)

        if (pivots.size > 8) pivots.removeAt(0)
    }

    fun update(screenWidth: Int, screenHeight: Int, dt: Float) {
        if (player.state == PlayerState.DEAD) return

        val steps = if (dt > 0.06f) 3 else 1
        val stepDt = dt / steps

        for (s in 0 until steps) {
            updateStep(screenWidth, screenHeight, stepDt)
            if (player.state == PlayerState.DEAD) break
        }
    }

    private fun updateStep(screenWidth: Int, screenHeight: Int, dt: Float) {
        if (player.state == PlayerState.ATTACHED) {
            val p = player.currentPivot ?: return
            player.angle += currentOrbitSpeed * p.dir * dt
            player.x = p.x + OrbitConfig.CATCH_RADIUS * cos(player.angle)
            player.y = p.y + OrbitConfig.CATCH_RADIUS * sin(player.angle)
        } else if (player.state == PlayerState.FLYING) {
            player.x += player.vx * dt
            player.y += player.vy * dt

            var activeTarget: Pivot? = null

            for (i in pivots.indices.reversed()) {
                val p = pivots[i]
                if (!p.isTarget) continue

                activeTarget = p

                val dx = player.x - p.x
                val dy = player.y - p.y
                val distSq = dx*dx + dy*dy

                if (distSq <= OrbitConfig.CATCH_RADIUS_SQ) {
                    onPivotCatch(p, screenWidth)
                    return
                }
            }

            checkBounds(screenWidth, screenHeight, activeTarget)
        }

        trailX[trailHead] = player.x
        trailY[trailHead] = player.y
        trailHead = (trailHead + 1) % OrbitConfig.TRAIL_LENGTH
        if (trailCount < OrbitConfig.TRAIL_LENGTH) trailCount++

        val targetCamY = player.y - (screenHeight * 0.75f)
        cameraY += (targetCamY - cameraY) * (5.0f * dt)
    }

    private fun checkBounds(screenWidth: Int, screenHeight: Int, target: Pivot?) {
        if (player.x < -OrbitConfig.PLAYER_RADIUS || player.x > screenWidth + OrbitConfig.PLAYER_RADIUS) {
            player.state = PlayerState.DEAD
        }
        if (player.y > cameraY + screenHeight + OrbitConfig.PLAYER_RADIUS) {
            player.state = PlayerState.DEAD
        }
        if (target != null) {
            if (player.y < target.y - OrbitConfig.MISS_MARGIN_Y) {
                player.state = PlayerState.DEAD
            }
        }
    }

    private fun onPivotCatch(p: Pivot, screenWidth: Int) {
        score++

        SoundManager.playAttach(score)

        val speedBoost = (score * OrbitConfig.ORBIT_SPEED_INC)
        currentOrbitSpeed = (OrbitConfig.ORBIT_SPEED_START + speedBoost).coerceAtMost(OrbitConfig.ORBIT_SPEED_MAX)

        player.state = PlayerState.ATTACHED
        player.currentPivot = p

        val dx = player.x - p.x
        val dy = player.y - p.y
        player.angle = atan2(dy, dx)

        p.isTarget = false
        spawnNextPivot(screenWidth)
    }

    fun tapAction(): Boolean {
        if (player.state == PlayerState.ATTACHED) {
            val p = player.currentPivot ?: return false
            player.state = PlayerState.FLYING
            player.vx = -sin(player.angle) * OrbitConfig.FLY_SPEED * p.dir
            player.vy = cos(player.angle) * OrbitConfig.FLY_SPEED * p.dir
            player.currentPivot = null
            p.passed = true
            return true
        }
        return false
    }
}