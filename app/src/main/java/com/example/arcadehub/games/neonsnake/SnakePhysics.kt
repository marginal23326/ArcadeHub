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

    private val inputQueue = ArrayDeque<GridDir>()
    private val historyBuffer = ArrayDeque<GameSnapshot>()
    private val MAX_HISTORY = 20
    private var aiSearchDepth = 6

    fun setDifficultyLevel(level: Int) {
        aiSearchDepth = level * 2
    }

    fun getDifficultyLevel(): Int = aiSearchDepth / 2

    fun reset() {
        isGameOver = false
        moveTimer = 0f
        inputQueue.clear()
        historyBuffer.clear()
        foods.clear()

        player = SnakeEntity(
            body = arrayListOf(Point(4, 4), Point(4, 5), Point(4, 6)),
            dir = GridDir.UP,
            health = SnakeConfig.INITIAL_HEALTH
        )

        ai = SnakeEntity(
            body = arrayListOf(Point(11, 4), Point(11, 5), Point(11, 6)),
            dir = GridDir.DOWN,
            health = SnakeConfig.INITIAL_HEALTH
        )

        repeat(SnakeConfig.FOOD_COUNT) { placeFood() }
    }

    fun processInput(newDir: GridDir) {
        val lastDir = if (inputQueue.isNotEmpty()) inputQueue.last() else player.dir
        if ((lastDir.x + newDir.x == 0) && (lastDir.y + newDir.y == 0)) return
        if (lastDir == newDir) return
        if (inputQueue.size < 2) inputQueue.add(newDir)
    }

    fun update(dt: Float) {
        if (isGameOver) return
        moveTimer += dt
        if (moveTimer >= SnakeConfig.GAME_SPEED_SECONDS) {
            moveTimer = 0f
            tick()
        }
    }

    private fun tick() {
        if (inputQueue.isNotEmpty()) player.dir = inputQueue.removeFirst()

        val aiMove = SnakeAi.getSmartMove(ai, player, foods, aiSearchDepth)
        ai.dir = aiMove

        val pAlive = moveSnake(player)
        val aAlive = moveSnake(ai)

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
            !player.isAlive -> "ROBOT WON"
            else -> "YOU WON"
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