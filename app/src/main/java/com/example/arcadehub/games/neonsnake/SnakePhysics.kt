package com.example.arcadehub.games.neonsnake

import com.example.arcadehub.managers.SoundManager
import kotlin.random.Random

class SnakePhysics {

    lateinit var player: SnakeEntity
    lateinit var ai: SnakeEntity
    var foods = ArrayList<Point>()

    private var moveTimer = 0f
    var isGameOver = false
    var gameOverReason = ""

    // --- New Features ---
    var isAutoPilot = false // Robot vs Robot mode
    var speedDelay = SnakeConfig.GAME_SPEED_SECONDS // Variable speed

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
        // Reset speed to default on new game
        speedDelay = SnakeConfig.GAME_SPEED_SECONDS

        inputQueue.clear()
        historyBuffer.clear()
        foods.clear()

        player = SnakeEntity(
            body = arrayListOf(Point(4, 6), Point(4, 7), Point(4, 8)),
            dir = GridDir.UP,
            health = SnakeConfig.INITIAL_HEALTH
        )

        ai = SnakeEntity(
            body = arrayListOf(Point(11, 2), Point(11, 1), Point(11, 0)),
            dir = GridDir.DOWN,
            health = SnakeConfig.INITIAL_HEALTH
        )

        repeat(SnakeConfig.FOOD_COUNT) { placeFood() }
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

        // Use variable speedDelay instead of constant
        if (moveTimer >= speedDelay) {
            moveTimer = 0f
            tick()
        }
    }

    private fun tick() {
        // 1. Determine Player Move
        if (isAutoPilot) {
            // Robot Logic for Player (Blue)
            // We use the same depth as the enemy for a fair fight
            val playerMove = SnakeBrain.getSmartMove(
                player,
                ai,
                foods,
                SnakeConfig.COLS,
                SnakeConfig.ROWS,
                aiSearchDepth
            )
            player.dir = playerMove
        } else {
            // Human Logic
            if (inputQueue.isNotEmpty()) player.dir = inputQueue.removeFirst()
        }

        // 2. Determine AI Move (Red)
        val aiMove = SnakeBrain.getSmartMove(
            ai,
            player,
            foods,
            SnakeConfig.COLS,
            SnakeConfig.ROWS,
            aiSearchDepth
        )
        ai.dir = aiMove

        // 3. Move Entities
        val pAlive = moveSnake(player)
        val aAlive = moveSnake(ai)

        // 4. Resolve State
        if (pAlive && aAlive) checkCollisions()
        else finalizeGameOver()

        recordSnapshot()
    }

    // Returns true if still alive after move (checks starvation and walls)
    private fun moveSnake(snake: SnakeEntity): Boolean {
        if (!snake.isAlive) return false

        val head = snake.head()
        val nx = head.x + snake.dir.x
        val ny = head.y + snake.dir.y

        // 1. Wall Death
        if (nx !in 0 until SnakeConfig.COLS || ny !in 0 until SnakeConfig.ROWS) {
            snake.isAlive = false
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
            snake.health = 100 // Reset Health
            placeFood() // Spawn replacement

            if (snake === player) SoundManager.playPerfect(0)
            else SoundManager.playPlaceVariation()
        } else {
            snake.body.removeAt(snake.body.size - 1)
            snake.health -= 1 // Decay Health
        }

        // 3. Starvation check
        if (snake.health <= 0) {
            snake.isAlive = false
            return false
        }

        return true
    }

    private fun checkCollisions() {
        val pHead = player.head()
        val aHead = ai.head()

        // Body Collisions (Player hits AI)
        if (ai.body.any { it == pHead }) player.isAlive = false
        // Body Collisions (AI hits Player)
        if (player.body.any { it == aHead }) ai.isAlive = false

        // Self Collisions
        // Start from index 1 to avoid head checking itself
        if (player.body.drop(1).any { it == pHead }) player.isAlive = false
        if (ai.body.drop(1).any { it == aHead }) ai.isAlive = false

        // Head to Head
        if (pHead == aHead) {
            if (player.body.size > ai.body.size) ai.isAlive = false
            else if (ai.body.size > player.body.size) player.isAlive = false
            else {
                player.isAlive = false
                ai.isAlive = false
            }
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
        while (attempts < 100) {
            val rx = Random.nextInt(SnakeConfig.COLS)
            val ry = Random.nextInt(SnakeConfig.ROWS)
            val p = Point(rx, ry)

            val occupied = player.body.contains(p) || ai.body.contains(p) || foods.contains(p)
            if (!occupied) {
                foods.add(p)
                return
            }
            attempts++
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