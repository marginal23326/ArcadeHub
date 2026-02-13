package com.example.arcadehub.games.orbithop

import com.example.arcadehub.core.MathUtils
import com.example.arcadehub.managers.SoundManager
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class OrbitPhysics {
    val player = Player()
    val pivots = ArrayList<Pivot>(10)

    val trailX = FloatArray(OrbitConfig.TRAIL_LENGTH)
    val trailY = FloatArray(OrbitConfig.TRAIL_LENGTH)
    var trailHead = 0
        private set
    var trailCount = 0
        private set

    var score = 0
        private set
    var cameraY = 0f
        private set
    var combo = 0
        private set

    var beatProgress = 0f
        private set
    var beatPulse = 0f
        private set
    var beatsLeft = OrbitConfig.MAX_BEATS_PER_PIVOT
        private set
    var launchAlignment = 0f
        private set
    var lastTimingGrade = TimingGrade.GOOD
        private set
    var timingFlash = 0f
        private set

    private var currentPivot: Pivot? = null
    private var targetPivot: Pivot? = null

    private var screenWidth = 0
    private var beatInterval = OrbitConfig.BEAT_INTERVAL_START
    private var beatTimer = 0f
    private var attachTime = 0f
    private var missedBeatsOnPivot = 0

    private var flightTime = 0f
    private var flightBestDistSq = Float.MAX_VALUE

    private var nextPivotId = 0
    private val laneX = FloatArray(OrbitConfig.LANE_COUNT)

    fun reset(screenWidth: Int, screenHeight: Int) {
        this.screenWidth = screenWidth
        buildLanes(screenWidth)

        pivots.clear()
        score = 0
        combo = 0
        cameraY = 0f
        beatInterval = OrbitConfig.BEAT_INTERVAL_START
        beatTimer = 0f
        beatProgress = 0f
        beatPulse = 0f
        beatsLeft = OrbitConfig.MAX_BEATS_PER_PIVOT
        missedBeatsOnPivot = 0
        attachTime = 0f
        launchAlignment = 0f
        lastTimingGrade = TimingGrade.GOOD
        timingFlash = 0f
        nextPivotId = 0

        trailHead = 0
        trailCount = 0

        val startLane = OrbitConfig.LANE_COUNT / 2
        val startPivot = Pivot(
            id = nextPivotId++,
            x = laneX[startLane],
            y = screenHeight - 260f,
            isTarget = false,
            dir = if (Random.nextBoolean()) 1 else -1,
            lane = startLane
        )

        currentPivot = startPivot
        pivots.add(startPivot)

        player.x = startPivot.x
        player.y = startPivot.y
        player.vx = 0f
        player.vy = 0f
        player.angle = -PI.toFloat() / 2f
        player.state = PlayerState.ATTACHED
        player.currentPivot = startPivot

        snapPlayerToOrbit(startPivot)
        spawnNextPivot()
        launchAlignment = currentLaunchAlignment()
    }

    fun update(screenWidth: Int, screenHeight: Int, dt: Float) {
        if (player.state == PlayerState.DEAD) return

        if (screenWidth != this.screenWidth) {
            this.screenWidth = screenWidth
            buildLanes(screenWidth)
        }

        var remaining = dt.coerceIn(0f, 0.05f)
        while (remaining > 0f) {
            val step = min(remaining, 0.016f)
            updateStep(screenWidth, screenHeight, step)
            remaining -= step
            if (player.state == PlayerState.DEAD) break
        }
    }

    private fun updateStep(screenWidth: Int, screenHeight: Int, dt: Float) {
        when (player.state) {
            PlayerState.ATTACHED -> updateAttached(dt)
            PlayerState.FLYING -> updateFlying(screenWidth, screenHeight, dt)
            PlayerState.DEAD -> return
        }

        trailX[trailHead] = player.x
        trailY[trailHead] = player.y
        trailHead = (trailHead + 1) % OrbitConfig.TRAIL_LENGTH
        if (trailCount < OrbitConfig.TRAIL_LENGTH) trailCount++

        val targetCamY = player.y - (screenHeight * 0.72f)
        cameraY = MathUtils.lerp(cameraY, targetCamY, OrbitConfig.CAMERA_LERP * dt)

        beatPulse = (beatPulse - OrbitConfig.BEAT_PULSE_DECAY * dt).coerceAtLeast(0f)
        timingFlash = (timingFlash - 2.8f * dt).coerceAtLeast(0f)
    }

    private fun updateAttached(dt: Float) {
        val pivot = currentPivot ?: run {
            player.state = PlayerState.DEAD
            return
        }

        player.angle += OrbitConfig.ORBIT_ANGULAR_SPEED * pivot.dir * dt
        snapPlayerToOrbit(pivot)
        launchAlignment = currentLaunchAlignment()

        attachTime += dt
        beatTimer += dt

        while (beatTimer >= beatInterval && player.state == PlayerState.ATTACHED) {
            beatTimer -= beatInterval
            onBeat()
        }

        beatProgress = beatTimer / beatInterval
    }

    private fun onBeat() {
        beatPulse = 1f

        missedBeatsOnPivot++
        beatsLeft = (OrbitConfig.MAX_BEATS_PER_PIVOT - missedBeatsOnPivot).coerceAtLeast(0)

        if (missedBeatsOnPivot >= OrbitConfig.MAX_BEATS_PER_PIVOT) {
            player.state = PlayerState.DEAD
        }
    }

    private fun updateFlying(screenWidth: Int, screenHeight: Int, dt: Float) {
        val target = targetPivot ?: run {
            player.state = PlayerState.DEAD
            return
        }

        val prevX = player.x
        val prevY = player.y
        val toTargetX = target.x - player.x
        val toTargetY = target.y - player.y
        val rawDistSq = toTargetX * toTargetX + toTargetY * toTargetY

        if (rawDistSq <= OrbitConfig.GRAVITY_FIELD_RADIUS_SQ) {
            val gravDistSq = rawDistSq.coerceAtLeast(OrbitConfig.GRAVITY_MIN_DIST_SQ)
            val gravDist = sqrt(gravDistSq)
            val nx = toTargetX / gravDist
            val ny = toTargetY / gravDist
            val proximity = (1f - (gravDist / OrbitConfig.GRAVITY_FIELD_RADIUS)).coerceIn(0f, 1f)
            val nearBoost = 1f + OrbitConfig.GRAVITY_NEAR_BOOST * proximity * proximity * proximity
            val accel = ((OrbitConfig.GRAVITY_CONST / gravDistSq) * nearBoost)
                .coerceAtMost(OrbitConfig.GRAVITY_MAX_ACCEL)

            player.vx += nx * accel * dt
            player.vy += ny * accel * dt

            if (gravDist < OrbitConfig.GRAVITY_CAPTURE_RADIUS) {
                val captureT = (1f - (gravDist / OrbitConfig.GRAVITY_CAPTURE_RADIUS)).coerceIn(0f, 1f)
                val captureAccel = OrbitConfig.GRAVITY_CAPTURE_PULL * captureT * captureT * (1f + 1.4f * captureT)
                player.vx += nx * captureAccel * dt
                player.vy += ny * captureAccel * dt

                val tx = -ny
                val ty = nx
                val tangentialSpeed = player.vx * tx + player.vy * ty
                val tangentialDamp = (OrbitConfig.GRAVITY_CAPTURE_TANGENTIAL_DAMP * captureT * dt).coerceIn(0f, 0.92f)
                player.vx -= tx * tangentialSpeed * tangentialDamp
                player.vy -= ty * tangentialSpeed * tangentialDamp

                val radialSpeed = player.vx * nx + player.vy * ny
                if (radialSpeed < 0f) {
                    val radialRecover = (-radialSpeed) * (1.1f + 2.4f * captureT)
                    player.vx += nx * radialRecover
                    player.vy += ny * radialRecover
                }

                val speedSq = player.vx * player.vx + player.vy * player.vy
                if (speedSq > 1f) {
                    val speed = sqrt(speedSq)
                    val steerT = (OrbitConfig.GRAVITY_CAPTURE_CENTER_STEER * captureT * dt).coerceIn(0f, 0.85f)
                    player.vx = MathUtils.lerp(player.vx, nx * speed, steerT)
                    player.vy = MathUtils.lerp(player.vy, ny * speed, steerT)
                }
            }
        }

        player.x += player.vx * dt
        player.y += player.vy * dt
        flightTime += dt

        val segDx = player.x - prevX
        val segDy = player.y - prevY
        val segLenSq = segDx * segDx + segDy * segDy
        val segT = if (segLenSq > 0.0001f) {
            (((target.x - prevX) * segDx + (target.y - prevY) * segDy) / segLenSq).coerceIn(0f, 1f)
        } else {
            0f
        }
        val closestX = prevX + segDx * segT
        val closestY = prevY + segDy * segT
        val closestDx = target.x - closestX
        val closestDy = target.y - closestY
        val closestDistSq = closestDx * closestDx + closestDy * closestDy
        if (closestDistSq <= OrbitConfig.CATCH_RADIUS_SQ) {
            player.x = closestX
            player.y = closestY
            onPivotCatch(target)
            return
        }

        val dx = target.x - player.x
        val dy = target.y - player.y
        val distSq = dx * dx + dy * dy

        if (distSq < flightBestDistSq) {
            flightBestDistSq = distSq
        }

        if (distSq <= OrbitConfig.CATCH_RADIUS_SQ) {
            onPivotCatch(target)
            return
        }

        if (flightTime > OrbitConfig.MAX_FLIGHT_TIME) {
            player.state = PlayerState.DEAD
            return
        }

        if (player.x < -OrbitConfig.OUT_BOUNDS_MARGIN || player.x > screenWidth + OrbitConfig.OUT_BOUNDS_MARGIN) {
            player.state = PlayerState.DEAD
            return
        }

        if (player.y > cameraY + screenHeight + OrbitConfig.OUT_BOUNDS_MARGIN) {
            player.state = PlayerState.DEAD
            return
        }

        if (player.y < target.y - OrbitConfig.MISS_MARGIN_Y) {
            player.state = PlayerState.DEAD
        }
    }

    private fun executeLaunch(beatError: Float, alignment: Float) {
        val pivot = currentPivot ?: run {
            player.state = PlayerState.DEAD
            return
        }

        val launchX = -sin(player.angle) * pivot.dir
        val launchY = cos(player.angle) * pivot.dir

        player.state = PlayerState.FLYING
        player.vx = launchX * OrbitConfig.FLY_SPEED
        player.vy = launchY * OrbitConfig.FLY_SPEED
        player.currentPivot = null

        missedBeatsOnPivot = 0
        beatsLeft = OrbitConfig.MAX_BEATS_PER_PIVOT
        beatProgress = 0f

        lastTimingGrade = gradeLaunch(beatError, alignment)
        if (lastTimingGrade != TimingGrade.PERFECT) {
            combo = 0
        }

        flightTime = 0f
        flightBestDistSq = Float.MAX_VALUE

        SoundManager.playSwoosh()
    }

    private fun onPivotCatch(pivot: Pivot) {
        val pointsGained = if (lastTimingGrade == TimingGrade.PERFECT) 2 else 1
        score += pointsGained

        when (lastTimingGrade) {
            TimingGrade.PERFECT -> {
                combo += 1
                timingFlash = 1f
                SoundManager.playPerfect(combo)
            }
            TimingGrade.GOOD -> {
                combo = 0
                timingFlash = 0.6f
                SoundManager.playAttach(score)
            }
            TimingGrade.MISS -> {
                combo = 0
                timingFlash = 0.45f
                SoundManager.playAttach(score)
            }
        }

        beatInterval = (OrbitConfig.BEAT_INTERVAL_START - score * OrbitConfig.BEAT_ACCEL_PER_SCORE)
            .coerceAtLeast(OrbitConfig.BEAT_INTERVAL_MIN)

        pivot.isTarget = false
        currentPivot = pivot
        targetPivot = null

        player.state = PlayerState.ATTACHED
        player.currentPivot = pivot
        player.vx = 0f
        player.vy = 0f
        player.angle = atan2(player.y - pivot.y, player.x - pivot.x)
        snapPlayerToOrbit(pivot)

        attachTime = 0f
        beatTimer = 0f
        beatProgress = 0f
        missedBeatsOnPivot = 0
        beatsLeft = OrbitConfig.MAX_BEATS_PER_PIVOT
        launchAlignment = currentLaunchAlignment()

        spawnNextPivot()
    }

    fun tapAction(): LaunchInputResult {
        if (player.state != PlayerState.ATTACHED || currentPivot == null || targetPivot == null) {
            return LaunchInputResult.IGNORED
        }
        if (attachTime < OrbitConfig.MIN_ATTACH_TIME) {
            return LaunchInputResult.IGNORED
        }

        val alignmentNow = currentLaunchAlignment()
        launchAlignment = alignmentNow

        if (alignmentNow < OrbitConfig.MIN_LAUNCH_ALIGNMENT) {
            return LaunchInputResult.BAD_ANGLE
        }

        val nearestBeatError = min(beatTimer, beatInterval - beatTimer)
        executeLaunch(nearestBeatError, alignmentNow)
        return LaunchInputResult.LAUNCHED
    }

    private fun spawnNextPivot() {
        val from = currentPivot ?: return

        val nextLane = pickNextLane(from.lane)
        val baseGap = OrbitConfig.GAP_Y_BASE + score * OrbitConfig.GAP_Y_GROW_PER_SCORE
        val gap = baseGap.coerceAtMost(OrbitConfig.GAP_Y_MAX) + (Random.nextFloat() - 0.5f) * OrbitConfig.GAP_Y_JITTER

        val x = (laneX[nextLane] + (Random.nextFloat() - 0.5f) * OrbitConfig.LANE_JITTER_X)
            .coerceIn(OrbitConfig.HORIZONTAL_PADDING, screenWidth - OrbitConfig.HORIZONTAL_PADDING)
        val y = from.y - gap

        val nextPivot = Pivot(
            id = nextPivotId++,
            x = x,
            y = y,
            isTarget = true,
            dir = -from.dir,
            lane = nextLane
        )

        targetPivot = nextPivot
        pivots.add(nextPivot)

        while (pivots.size > 10) {
            pivots.removeAt(0)
        }
    }

    private fun pickNextLane(currentLane: Int): Int {
        var step = when (Random.nextInt(100)) {
            in 0..41 -> 0
            in 42..70 -> -1
            else -> 1
        }

        if (score > 18 && Random.nextInt(100) < 20) {
            step *= 2
        }

        var lane = (currentLane + step).coerceIn(0, OrbitConfig.LANE_COUNT - 1)

        if (lane == currentLane && score > 8 && Random.nextInt(100) < 35) {
            lane = (currentLane + if (currentLane < OrbitConfig.LANE_COUNT / 2) 1 else -1)
                .coerceIn(0, OrbitConfig.LANE_COUNT - 1)
        }

        return lane
    }

    private fun buildLanes(width: Int) {
        val usable = (width.toFloat() - OrbitConfig.HORIZONTAL_PADDING * 2f).coerceAtLeast(1f)
        val step = usable / (OrbitConfig.LANE_COUNT - 1)

        for (i in 0 until OrbitConfig.LANE_COUNT) {
            laneX[i] = OrbitConfig.HORIZONTAL_PADDING + i * step
        }
    }

    private fun snapPlayerToOrbit(pivot: Pivot) {
        player.x = pivot.x + OrbitConfig.CATCH_RADIUS * cos(player.angle)
        player.y = pivot.y + OrbitConfig.CATCH_RADIUS * sin(player.angle)
    }

    private fun currentLaunchAlignment(): Float {
        val pivot = currentPivot ?: return 0f
        val target = targetPivot ?: return 0f
        return computeLaunchAlignment(player.angle, pivot, target)
    }

    private fun computeLaunchAlignment(angle: Float, pivot: Pivot, target: Pivot): Float {
        val launchDirX = -sin(angle) * pivot.dir
        val launchDirY = cos(angle) * pivot.dir

        val px = pivot.x + OrbitConfig.CATCH_RADIUS * cos(angle)
        val py = pivot.y + OrbitConfig.CATCH_RADIUS * sin(angle)

        val toTargetX = target.x - px
        val toTargetY = target.y - py
        val dist = sqrt(toTargetX * toTargetX + toTargetY * toTargetY)
        if (dist < 1f) return 1f

        return (launchDirX * toTargetX + launchDirY * toTargetY) / dist
    }

    private fun gradeLaunch(beatError: Float, alignment: Float): TimingGrade {
        return when {
            beatError <= OrbitConfig.PERFECT_WINDOW && alignment >= OrbitConfig.PERFECT_LAUNCH_ALIGNMENT -> TimingGrade.PERFECT
            beatError <= OrbitConfig.GOOD_WINDOW && alignment >= OrbitConfig.MIN_LAUNCH_ALIGNMENT -> TimingGrade.GOOD
            else -> TimingGrade.MISS
        }
    }
}
