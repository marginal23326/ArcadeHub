package com.example.arcadehub.games.blockstack

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class CameraSystem {
    var cameraOffsetX = 0f
    var cameraOffsetY = 0f

    var targetCameraOffsetX = 0f
    var targetCameraOffsetY = 0f

    var currentScale = 1f
    var targetScale = 1f

    fun reset() {
        cameraOffsetX = 0f; cameraOffsetY = 0f
        targetCameraOffsetX = 0f; targetCameraOffsetY = 0f
        currentScale = 1f; targetScale = 1f
    }

    fun update(dtScale: Float) {
        val diffY = targetCameraOffsetY - cameraOffsetY
        if (abs(diffY) > 1f) {
            var step = diffY * 0.05f * dtScale

            val maxCamSpeed = 25f * dtScale
            step = step.coerceIn(-maxCamSpeed, maxCamSpeed)

            val minCamSpeed = 4f * dtScale
            if (abs(step) < minCamSpeed) step = if (diffY > 0) minCamSpeed else -minCamSpeed

            if (abs(step) > abs(diffY)) cameraOffsetY = targetCameraOffsetY
            else cameraOffsetY += step
        } else {
            cameraOffsetY = targetCameraOffsetY
        }

        val diffX = targetCameraOffsetX - cameraOffsetX
        if (abs(diffX) > 1f) {
            var step = diffX * 0.05f * dtScale
            val maxCamSpeed = 25f * dtScale
            step = step.coerceIn(-maxCamSpeed, maxCamSpeed)
            if (abs(step) > abs(diffX)) cameraOffsetX = targetCameraOffsetX
            else cameraOffsetX += step
        } else {
            cameraOffsetX = targetCameraOffsetX
        }

        val diffScale = targetScale - currentScale
        if (abs(diffScale) > 0.001f) {
            currentScale += diffScale * BlockConfig.ZOOM_SMOOTHING * dtScale
        } else {
            currentScale = targetScale
        }
    }
}

// --- PHYSICS STRATEGY INTERFACE ---
interface PhysicsStrategy {
    val debrisList: ArrayList<Debris>
    fun reset(screenWidth: Int, screenHeight: Int)
    fun update(screenWidth: Int, screenHeight: Int, dtScale: Float)
    fun calculatePlacement(score: Int, isMagnetActive: Boolean, isWidenerActive: Boolean): PlacementResult
    fun spawnNewBlock(screenWidth: Int, score: Int, activeSloMoTurns: Int)
    fun updateSpeed(score: Int, activeSloMoTurns: Int)

    fun getCameraTargetX(): Float
    fun getCameraTargetY(): Float

    fun getCameraScaleTarget(screenHeight: Int, isGameOver: Boolean, isZoomedOut: Boolean): Float

    fun applyRevive(screenWidth: Int)
    fun triggerWidener(screenWidth: Int): Boolean
}

abstract class BaseBlockPhysics : PhysicsStrategy {
    override val debrisList = ArrayList<Debris>()
    var currentSpeed = BlockConfig.BASE_SPEED

    override fun update(screenWidth: Int, screenHeight: Int, dtScale: Float) {
        // 1. Run specific block logic
        onUpdatePhysics(screenWidth, screenHeight, dtScale)

        // 2. Debris Logic
        for (i in debrisList.size - 1 downTo 0) {
            val d = debrisList[i]
            d.vy += BlockConfig.GRAVITY * dtScale
            d.x += d.vx * dtScale
            d.y += d.vy * dtScale
            d.rotation += d.rotSpeed * dtScale

            if (d.y > 3000f || d.y > screenHeight + 500f) {
                debrisList.removeAt(i)
            }
        }
    }

    override fun updateSpeed(score: Int, activeSloMoTurns: Int) {
        val speedMultiplier = min(score, BlockConfig.MAX_SPEED_SCORE)
        var spd = BlockConfig.BASE_SPEED + (speedMultiplier * BlockConfig.SPEED_INCREMENT)
        if (activeSloMoTurns > 0) spd *= BlockConfig.SLOMO_FACTOR
        currentSpeed = spd
    }

    abstract fun onUpdatePhysics(screenWidth: Int, screenHeight: Int, dtScale: Float)
}

// --- LINEAR PHYSICS (Classic) ---
class LinearPhysics : BaseBlockPhysics() {
    var currentBlockX = 0f
    var currentBlockWidth = 0f
    var movingRight = true
    var stackY = 0f
    var previousBlockX = 0f
    var previousBlockWidth = 0f

    val stackList = ArrayList<Block>()

    override fun reset(screenWidth: Int, screenHeight: Int) {
        stackList.clear()
        debrisList.clear()
        previousBlockWidth = screenWidth / 2f
        previousBlockX = (screenWidth - previousBlockWidth) / 2
        stackY = screenHeight.toFloat() - BlockConfig.BLOCK_HEIGHT
        stackList.add(Block(0, previousBlockX, stackY, previousBlockWidth, android.graphics.Color.LTGRAY))
        spawnNewBlock(screenWidth, 0, 0)
    }

    override fun onUpdatePhysics(screenWidth: Int, screenHeight: Int, dtScale: Float) {
        val step = currentSpeed * dtScale
        if (movingRight) {
            currentBlockX += step
            if (currentBlockX + currentBlockWidth >= screenWidth) {
                currentBlockX = screenWidth - currentBlockWidth
                movingRight = false
            }
        } else {
            currentBlockX -= step
            if (currentBlockX <= 0) {
                currentBlockX = 0f
                movingRight = true
            }
        }
    }

    override fun calculatePlacement(score: Int, isMagnetActive: Boolean, isWidenerActive: Boolean): PlacementResult {
        val currentLeft = currentBlockX
        val currentRight = currentBlockX + currentBlockWidth
        val prevLeft = previousBlockX
        val prevRight = previousBlockX + previousBlockWidth
        val overlapStart = max(currentLeft, prevLeft)
        val overlapEnd = min(currentRight, prevRight)
        val actualOverlapWidth = overlapEnd - overlapStart

        if (actualOverlapWidth <= 0) return PlacementResult(PlacementType.MISS)

        val currentCenter = currentLeft + (currentBlockWidth / 2)
        val prevCenter = prevLeft + (previousBlockWidth / 2)
        val deviation = abs(currentCenter - prevCenter)
        val accuracy = actualOverlapWidth / previousBlockWidth

        var tolerance = BlockConfig.PERFECT_TOLERANCE_PIXELS
        if (isMagnetActive) tolerance *= BlockConfig.MAGNET_TOLERANCE_MULTIPLIER

        val currentPercentThreshold = if (score >= BlockConfig.PERFECT_PIVOT_SCORE) BlockConfig.THRESHOLD_PERFECT_LATE else BlockConfig.THRESHOLD_PERFECT_EARLY
        var isPerfect = false
        if (!isWidenerActive && (deviation <= tolerance || accuracy >= currentPercentThreshold)) {
            currentBlockX = previousBlockX
            isPerfect = true
        }

        val finalWidth = if (isPerfect) previousBlockWidth else if (isWidenerActive) currentBlockWidth else actualOverlapWidth
        val newX = if (isPerfect) previousBlockX else if (isWidenerActive) currentBlockX else overlapStart

        if (!isPerfect && !isWidenerActive) {
            val currentColor = BlockConfig.COLORS[score % BlockConfig.COLORS.size]
            // Debris creation
            if (currentLeft < prevLeft) debrisList.add(Debris(currentLeft, stackY, prevLeft - currentLeft, BlockConfig.BLOCK_HEIGHT, currentColor, 0f, -5f, 0f, 5f, false))
            if (currentRight > prevRight) debrisList.add(Debris(prevRight, stackY, currentRight - prevRight, BlockConfig.BLOCK_HEIGHT, currentColor, 0f, -5f, 0f, 5f, false))
        }

        val placedColor = BlockConfig.COLORS[score % BlockConfig.COLORS.size]
        stackList.add(Block(score, newX, stackY, finalWidth, placedColor))
        previousBlockX = newX; previousBlockWidth = finalWidth; currentBlockWidth = finalWidth

        return PlacementResult(if (isPerfect) PlacementType.PERFECT else PlacementType.NORMAL, accuracy)
    }

    override fun spawnNewBlock(screenWidth: Int, score: Int, activeSloMoTurns: Int) {
        stackY -= BlockConfig.BLOCK_HEIGHT
        currentBlockWidth = previousBlockWidth
        movingRight = Random.nextBoolean()
        currentBlockX = if (movingRight) -currentBlockWidth else screenWidth.toFloat()
        updateSpeed(score, activeSloMoTurns)
    }

    override fun getCameraTargetX(): Float = 0f
    override fun getCameraTargetY(): Float {
        return if (stackList.size > 5) (stackList.size - 5) * BlockConfig.BLOCK_HEIGHT else 0f
    }

    override fun getCameraScaleTarget(screenHeight: Int, isGameOver: Boolean, isZoomedOut: Boolean): Float {
        if (isGameOver && isZoomedOut) {
            val topY = stackList.lastOrNull()?.y ?: 0f
            val bottomY = stackList.firstOrNull()?.y ?: 0f
            val stackHeight = abs(bottomY - topY) + (BlockConfig.BLOCK_HEIGHT * 4)
            val availableHeight = screenHeight.toFloat() * 0.8f
            val calcScale = availableHeight / stackHeight
            return if (calcScale < 1f) calcScale else 1f
        }
        return 1f
    }

    override fun applyRevive(screenWidth: Int) { currentBlockX = (screenWidth - currentBlockWidth) / 2 }
    override fun triggerWidener(screenWidth: Int): Boolean {
        if (currentBlockWidth >= screenWidth/2f - 1f) return false
        currentBlockWidth = (currentBlockWidth + screenWidth * BlockConfig.WIDENER_PERCENT).coerceAtMost(screenWidth/2f)
        return true
    }
}

// --- RADIAL PHYSICS (Orbit) ---
class RadialPhysics : BaseBlockPhysics() {
    var currentAngle = 0f
    var currentSweep = BlockConfig.INITIAL_ARC_LENGTH
    var currentRadius = BlockConfig.INITIAL_RADIUS
    var spinClockwise = true
    var prevAngle = 0f
    var prevSweep = BlockConfig.INITIAL_ARC_LENGTH

    val stackList = ArrayList<ArcBlock>()

    override fun reset(screenWidth: Int, screenHeight: Int) {
        stackList.clear()
        debrisList.clear()
        currentRadius = BlockConfig.INITIAL_RADIUS
        prevSweep = BlockConfig.INITIAL_ARC_LENGTH
        prevAngle = 270f
        stackList.add(ArcBlock(0, currentRadius, prevAngle - prevSweep/2, prevSweep, android.graphics.Color.LTGRAY))
        spawnNewBlock(screenWidth, 0, 0)
    }

    override fun onUpdatePhysics(screenWidth: Int, screenHeight: Int, dtScale: Float) {
        val step = currentSpeed * 1f * dtScale * 0.1f
        if (spinClockwise) currentAngle = (currentAngle + step) % 360f
        else {
            currentAngle = (currentAngle - step)
            if (currentAngle < 0) currentAngle += 360f
        }
    }

    override fun calculatePlacement(score: Int, isMagnetActive: Boolean, isWidenerActive: Boolean): PlacementResult {
        val targetStart = prevAngle - (prevSweep / 2f)
        val currentStartAbs = currentAngle - (currentSweep / 2f)
        fun getRelativeAngle(angle: Float, reference: Float): Float {
            var diff = angle - reference
            while (diff > 180) diff -= 360
            while (diff < -180) diff += 360
            return diff
        }
        val relCurrentStart = getRelativeAngle(currentStartAbs, targetStart)
        val relCurrentEnd = relCurrentStart + currentSweep
        val relTargetStart = 0f; val relTargetEnd = prevSweep
        val overlapStart = max(relCurrentStart, relTargetStart)
        val overlapEnd = min(relCurrentEnd, relTargetEnd)
        val overlapSweep = overlapEnd - overlapStart
        if (overlapSweep <= 0) return PlacementResult(PlacementType.MISS)
        val angularDiff = abs(getRelativeAngle(currentAngle, prevAngle))
        var tolerance = BlockConfig.PERFECT_TOLERANCE_DEGREES
        if (isMagnetActive) tolerance *= BlockConfig.MAGNET_TOLERANCE_MULTIPLIER
        var isPerfect = false
        val threshold = if (score >= BlockConfig.PERFECT_PIVOT_SCORE) 0.96f else 0.92f
        val accuracy = overlapSweep / prevSweep
        if (!isWidenerActive && (angularDiff <= tolerance || accuracy >= threshold)) isPerfect = true
        val finalSweep: Float; val newAngle: Float
        if (isPerfect) { finalSweep = prevSweep; newAngle = prevAngle }
        else if (isWidenerActive) { finalSweep = currentSweep; newAngle = currentAngle }
        else { finalSweep = overlapSweep; newAngle = (targetStart + overlapStart + (finalSweep / 2f))
            val currentColor = BlockConfig.COLORS[score % BlockConfig.COLORS.size]
            synchronized(debrisList) {
                if (relCurrentStart < relTargetStart) spawnRadialDebris(targetStart + relCurrentStart, relTargetStart - relCurrentStart, currentColor)
                if (relCurrentEnd > relTargetEnd) spawnRadialDebris(targetStart + relTargetEnd, relCurrentEnd - relTargetEnd, currentColor)
            }
        }
        val placedColor = BlockConfig.COLORS[score % BlockConfig.COLORS.size]
        synchronized(stackList) { stackList.add(ArcBlock(score, currentRadius, newAngle - (finalSweep / 2f), finalSweep, placedColor)) }
        prevAngle = newAngle; prevSweep = finalSweep; currentSweep = finalSweep
        return PlacementResult(if (isPerfect) PlacementType.PERFECT else PlacementType.NORMAL, accuracy)
    }

    private fun spawnRadialDebris(startAngle: Float, sweep: Float, color: Int) {
        if (sweep < 0.5f) return
        val midAngle = startAngle + (sweep / 2f)
        val rad = Math.toRadians(midAngle.toDouble())
        val dirX = cos(rad).toFloat()
        val dirY = sin(rad).toFloat()
        debrisList.add(Debris(0f, 0f, currentRadius, BlockConfig.BLOCK_HEIGHT, color, dirX * 5f, dirY * 5f - 2f, 0f, 5f, true, startAngle, sweep))
    }

    override fun spawnNewBlock(screenWidth: Int, score: Int, activeSloMoTurns: Int) {
        currentRadius += BlockConfig.BLOCK_HEIGHT
        currentSweep = prevSweep
        spinClockwise = Random.nextBoolean()
        currentAngle = (prevAngle + 180) % 360f
        updateSpeed(score, activeSloMoTurns)
    }

    override fun getCameraTargetX(): Float {
        val rad = Math.toRadians(prevAngle.toDouble())
        val tx = cos(rad).toFloat() * currentRadius
        val ty = sin(rad).toFloat() * currentRadius
        return -tx
    }

    override fun getCameraTargetY(): Float {
        val rad = Math.toRadians(prevAngle.toDouble())
        val ty = sin(rad).toFloat() * currentRadius
        return -ty
    }

    override fun getCameraScaleTarget(screenHeight: Int, isGameOver: Boolean, isZoomedOut: Boolean): Float {
        val neededSpace = currentRadius + (BlockConfig.BLOCK_HEIGHT * 3)
        val availableHeight = screenHeight * 0.85f
        val fitScale = if (neededSpace > availableHeight) availableHeight / neededSpace else 1f
        return when {
            isZoomedOut -> fitScale * 0.5f
            isGameOver -> fitScale
            else -> fitScale.coerceAtLeast(0.45f)
        }
    }

    override fun applyRevive(screenWidth: Int) { currentAngle = prevAngle }
    override fun triggerWidener(screenWidth: Int): Boolean {
        if (currentSweep >= 180f) return false
        currentSweep = (currentSweep + 360f * BlockConfig.WIDENER_PERCENT).coerceAtMost(180f)
        return true
    }
}