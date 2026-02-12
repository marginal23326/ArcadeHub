package com.example.arcadehub.games.neonsnake

import com.example.arcadehub.managers.SoundManager
import kotlin.math.abs
import kotlin.random.Random

class SnakePhysics {

    lateinit var player: SnakeEntity
    lateinit var ai: SnakeEntity
    var foods = ArrayList<Point>()
    val grid = SnakeGrid(SnakeConfig.COLS, SnakeConfig.ROWS) // Keep a persistent grid ref

    // New Visual Triggers
    var onFoodEaten: ((x: Int, y: Int, color: Int) -> Unit)? = null
    var onCollision: ((x: Int, y: Int) -> Unit)? = null

    // Game Settings
    var isAutoPilot = false
    var speedDelay = SnakeConfig.GAME_SPEED_SECONDS
    var currentMapType = SnakeMapGenerator.MapType.EMPTY

    private var moveTimer = 0f
    var isGameOver = false
    var gameOverReason = ""

    private val inputQueue = ArrayDeque<GridDir>()
    private val historyBuffer = ArrayDeque<GameSnapshot>()
    private val MAX_HISTORY = 10
    private var aiSearchDepth = 6

    fun setDifficultyLevel(level: Int) {
        aiSearchDepth = level * 2
    }

    fun getDifficultyLevel(): Int = aiSearchDepth / 2

    fun reset() {
        isGameOver = false
        moveTimer = 0f
        speedDelay = SnakeConfig.GAME_SPEED_SECONDS

        inputQueue.clear()
        historyBuffer.clear()
        foods.clear()

        // 1. Generate Map (Walls)
        SnakeMapGenerator.applyMap(grid, currentMapType)

        // 2. Spawn Player (Find safe spot if map blocks default)
        player = SnakeEntity(
            body = findSafeSpawn(4, 6),
            dir = GridDir.UP,
            health = SnakeConfig.INITIAL_HEALTH
        )

        // 3. Spawn AI
        ai = SnakeEntity(
            body = findSafeSpawn(11, 2),
            dir = GridDir.DOWN,
            health = SnakeConfig.INITIAL_HEALTH
        )

        // 4. Mark bodies on grid so food placement doesn't overlap immediately
        updateGridWithBodies()

        repeat(SnakeConfig.FOOD_COUNT) { placeFood() }
    }

    // Helper to spawn snake away from walls
    private fun findSafeSpawn(preferredX: Int, preferredY: Int): ArrayList<Point> {
        // Try preferred
        if (grid.isSafe(preferredX, preferredY) && grid.isSafe(preferredX, preferredY + 1)) {
            return arrayListOf(Point(preferredX, preferredY), Point(preferredX, preferredY + 1), Point(preferredX, preferredY + 2))
        }
        // Fallback: search for empty spot
        for (x in 2 until SnakeConfig.COLS - 2) {
            for (y in 2 until SnakeConfig.ROWS - 2) {
                if (grid[x, y] == 0 && grid[x, y+1] == 0 && grid[x, y+2] == 0) {
                    return arrayListOf(Point(x, y), Point(x, y+1), Point(x, y+2))
                }
            }
        }
        // Should not happen in designed maps
        return arrayListOf(Point(preferredX, preferredY))
    }

    fun processInput(newDir: GridDir) {
        if (isAutoPilot) return
        val lastDir = if (inputQueue.isNotEmpty()) inputQueue.last() else player.dir
        if ((lastDir.x + newDir.x == 0) && (lastDir.y + newDir.y == 0)) return
        if (lastDir == newDir) return
        if (inputQueue.size < 2) inputQueue.add(newDir)
    }

    fun update(dt: Float) {
        if (isGameOver) return
        moveTimer += dt
        if (moveTimer >= speedDelay) {
            moveTimer = 0f
            tick()
        }
    }

    private fun tick() {
        // AI Logic
        if (isAutoPilot) {
            player.dir = SnakeBrain.getSmartMove(player, ai, foods, SnakeConfig.COLS, SnakeConfig.ROWS, aiSearchDepth)
        } else if (inputQueue.isNotEmpty()) {
            player.dir = inputQueue.removeFirst()
        }

        ai.dir = SnakeBrain.getSmartMove(ai, player, foods, SnakeConfig.COLS, SnakeConfig.ROWS, aiSearchDepth)

        val pAlive = moveSnake(player)
        val aAlive = moveSnake(ai)

        // Sync grid for collision checks next frame
        updateGridWithBodies()

        if (pAlive && aAlive) checkCollisions()
        else finalizeGameOver()

        recordSnapshot()
    }

    private fun updateGridWithBodies() {
        // Refresh grid from static map
        SnakeMapGenerator.applyMap(grid, currentMapType)

        // Add dynamic elements
        foods.forEach { grid[it.x, it.y] = 1 }
        player.body.forEach { grid[it.x, it.y] = 2 }
        ai.body.forEach { grid[it.x, it.y] = 3 }
    }

    private fun moveSnake(snake: SnakeEntity): Boolean {
        if (!snake.isAlive) return false

        val head = snake.head()
        val nx = head.x + snake.dir.x
        val ny = head.y + snake.dir.y

        // 1. Wall Death (Bounds OR Map Obstacles)
        if (nx !in 0 until SnakeConfig.COLS || ny !in 0 until SnakeConfig.ROWS || grid[nx, ny] == 9) {
            snake.isAlive = false
            onCollision?.invoke(head.x, head.y) // Trigger shake/particles at old head
            return false
        }

        val newHead = Point(nx, ny)

        // 2. Check Food
        val foodIdx = foods.indexOfFirst { it == newHead }
        val ate = foodIdx != -1

        snake.body.add(0, newHead)

        if (ate) {
            foods.removeAt(foodIdx)
            snake.score++
            snake.health = 100
            placeFood()

            // Visual Trigger
            val color = if(snake === player) SnakeConfig.COLOR_P1_HEAD else SnakeConfig.COLOR_AI_HEAD
            onFoodEaten?.invoke(nx, ny, color)

            if (snake === player) SoundManager.playPerfect(0)
            else SoundManager.playPlaceVariation()
        } else {
            snake.body.removeAt(snake.body.size - 1)
            snake.health -= 1
        }

        if (snake.health <= 0) {
            snake.isAlive = false
            return false
        }

        return true
    }

    private fun checkCollisions() {
        val pHead = player.head()
        val aHead = ai.head()

        // Standard collision logic
        if (ai.body.any { it == pHead }) { player.isAlive = false; onCollision?.invoke(pHead.x, pHead.y) }
        if (player.body.any { it == aHead }) { ai.isAlive = false; onCollision?.invoke(aHead.x, aHead.y) }
        if (player.body.drop(1).any { it == pHead }) { player.isAlive = false; onCollision?.invoke(pHead.x, pHead.y) }
        if (ai.body.drop(1).any { it == aHead }) { ai.isAlive = false; onCollision?.invoke(aHead.x, aHead.y) }

        if (pHead == aHead) {
            onCollision?.invoke(pHead.x, pHead.y)
            if (player.body.size > ai.body.size) ai.isAlive = false
            else if (ai.body.size > player.body.size) player.isAlive = false
            else { player.isAlive = false; ai.isAlive = false }
        }

        if (!player.isAlive || !ai.isAlive) finalizeGameOver()
    }

    private fun finalizeGameOver() {
        if (isGameOver) return
        isGameOver = true
        gameOverReason = when {
            !player.isAlive && !ai.isAlive -> "DRAW"
            !player.isAlive -> "RED ROBOT WON"
            else -> if (isAutoPilot) "BLUE ROBOT WON" else "YOU WON"
        }
    }

    private fun placeFood() {
        var attempts = 0
        while (attempts < 200) {
            val rx = Random.nextInt(SnakeConfig.COLS)
            val ry = Random.nextInt(SnakeConfig.ROWS)

            // Check Map Wall (9)
            if (grid[rx, ry] == 9) { attempts++; continue }

            val p = Point(rx, ry)
            val occupied = player.body.contains(p) || ai.body.contains(p) || foods.contains(p)
            if (occupied) { attempts++; continue }

            var tooClose = false
            for (f in foods) {
                if (abs(f.x - rx) + abs(f.y - ry) < SnakeConfig.MIN_FOOD_SPACING) {
                    tooClose = true; break
                }
            }

            if (tooClose && attempts < 150) { attempts++; continue }

            foods.add(p)
            return
        }
    }

    private fun recordSnapshot() {
        val snapshot = GameSnapshot(
            playerBody = ArrayList(player.body),
            aiBody = ArrayList(ai.body),
            foods = ArrayList(foods),
            pScore = player.score,
            aScore = ai.score,
            pHealth = player.health,
            aHealth = ai.health
        )
        historyBuffer.add(snapshot)
        if (historyBuffer.size > MAX_HISTORY) historyBuffer.removeFirst()
    }

    fun getReplayHistory(): List<GameSnapshot> = historyBuffer.toList()
}