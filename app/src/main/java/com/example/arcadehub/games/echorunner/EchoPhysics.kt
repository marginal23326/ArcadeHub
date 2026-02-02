package com.example.arcadehub.games.echorunner

import com.example.arcadehub.core.Constants
import kotlin.random.Random

class EchoPhysics {
    val player = Player()
    private val obstaclePool = Array(25) { Obstacle() }
    val particles = Array(EchoConfig.PARTICLE_COUNT) { Particle() }

    var state = GameState.MENU
    var score = 0
    private var speed = EchoConfig.BASE_SPEED
    private var timeInGame = 0f
    private var spawnTimer = 0f
    private var restartCooldown = 0f

    private var worldWidth = Constants.LOGIC_WIDTH
    private var worldHeight = Constants.LOGIC_HEIGHT

    fun reset() {
        state = GameState.MENU
        score = 0
        speed = EchoConfig.BASE_SPEED
        timeInGame = 0f
        spawnTimer = 0f
        restartCooldown = 0f

        player.x = worldWidth * 0.15f
        player.y = worldHeight / 2f - player.size / 2f
        player.isEcho = false

        obstaclePool.forEach { it.active = false }
        particles.forEach { it.active = false }
    }

    fun startGame() {
        if (state != GameState.PLAYING) {
            state = GameState.PLAYING
            player.isEcho = true
        }
    }

    /**
     * @param active True if holding button (Echo Dimension), False if released (Real Dimension)
     */
    fun setInput(active: Boolean) {
        if (state == GameState.MENU && active) startGame()
        else if (state == GameState.PLAYING) player.isEcho = active
        else if (state == GameState.GAMEOVER) {
            // Only restart if button is released first to prevent accidental restarts
            if (!active && restartCooldown <= 0f) {
                reset()
            }
        }
    }

    fun update(dt: Float) {
        // Limit dt to prevent tunnelling on lag spikes
        val safeDt = dt.coerceAtMost(0.1f)

        if (state == GameState.GAMEOVER) {
            if (restartCooldown > 0f) {
                restartCooldown -= safeDt
            }
            updateParticles(safeDt)
            return
        }

        if (state != GameState.PLAYING) return

        timeInGame += safeDt

        // 1. DYNAMIC SPEED
        val targetSpeed = EchoConfig.BASE_SPEED +
                (score * EchoConfig.SPEED_INC_PER_SCORE) +
                (timeInGame * EchoConfig.SPEED_INC_PER_SECOND)

        speed = targetSpeed.coerceAtMost(EchoConfig.MAX_SPEED)

        // 2. VISUAL GLITCH
        player.drawX = player.x
        val glitchChance = 0.8f + (speed / EchoConfig.MAX_SPEED) * 0.15f
        if (Random.nextFloat() > glitchChance) {
            player.drawX += (Random.nextFloat() - 0.5f) * 30f
        }

        // 3. SPAWNING
        val speedPercent = (speed - EchoConfig.BASE_SPEED) / (EchoConfig.MAX_SPEED - EchoConfig.BASE_SPEED)
        val currentInterval = EchoConfig.SPAWN_INTERVAL_START - (speedPercent * (EchoConfig.SPAWN_INTERVAL_START - EchoConfig.SPAWN_INTERVAL_MIN))

        spawnTimer -= safeDt
        if (spawnTimer <= 0) {
            spawnObstacle()
            spawnTimer = currentInterval
        }

        // 4. OBSTACLES
        for (obs in obstaclePool) {
            if (!obs.active) continue
            obs.x -= speed * safeDt

            // Score logic
            if (!obs.passed && obs.x + obs.w < player.x) {
                obs.passed = true
                score += 10
            }

            // Despawn
            if (obs.x + obs.w < 0) obs.active = false

            // Collision
            // AABB Collision Check
            if (player.x < obs.x + obs.w &&
                player.x + player.size > obs.x &&
                player.y < obs.y + obs.h &&
                player.y + player.size > obs.y) {

                val isRealCrash = (obs.type == DimensionType.REAL && !player.isEcho)
                val isEchoCrash = (obs.type == DimensionType.ECHO && player.isEcho)

                if (isRealCrash || isEchoCrash) gameOver()
            }
        }

        // 5. PARTICLES
        updateParticles(safeDt)
    }

    private fun updateParticles(dt: Float) {
        for (p in particles) {
            if (!p.active) continue
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= 2.0f * dt
            if (p.life <= 0) p.active = false
        }
    }

    private fun spawnObstacle() {
        val obs = obstaclePool.firstOrNull { !it.active } ?: return
        obs.active = true
        obs.passed = false
        obs.type = if (Random.nextBoolean()) DimensionType.REAL else DimensionType.ECHO
        obs.w = 90f

        // Random height but within the floor/ceiling bounds
        obs.h = Random.nextFloat() * 180f + 120f
        obs.x = worldWidth.toFloat()
        obs.y = (worldHeight / 2f) - (obs.h / 2f)

        // At high speeds, randomly offset Y to make it harder
        if (speed > 1500f) {
            obs.y += (Random.nextFloat() - 0.5f) * 150f
        }
    }

    private fun gameOver() {
        state = GameState.GAMEOVER
        restartCooldown = 1f // 1 sec of mandatory score viewing
        val color = if (player.isEcho) EchoConfig.COLOR_ECHO_FG else EchoConfig.COLOR_REAL_FG
        spawnExplosion(player.x + player.size/2, player.y + player.size/2, color)
    }

    private fun spawnExplosion(x: Float, y: Float, color: Int) {
        var count = 0
        for (p in particles) {
            if (count >= 30) break
            if (!p.active) {
                p.active = true
                p.x = x; p.y = y
                p.vx = (Random.nextFloat() - 0.5f) * 800f
                p.vy = (Random.nextFloat() - 0.5f) * 800f
                p.life = 1.0f
                p.color = color
                count++
            }
        }
    }

    fun getSpeed() = speed
    fun getActiveObstacles() = obstaclePool
    fun canRestart() = state == GameState.GAMEOVER && restartCooldown <= 0f
}