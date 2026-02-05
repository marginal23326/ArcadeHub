package com.example.arcadehub.games.neonsnake

import com.example.arcadehub.games.neonsnake.SnakeConfig.TILE_COUNT_X
import com.example.arcadehub.games.neonsnake.SnakeConfig.TILE_COUNT_Y
import com.example.arcadehub.managers.SoundManager
import kotlin.random.Random

class SnakePhysics {

    lateinit var player: SnakeEntity
    lateinit var ai: SnakeEntity
    var food = Point(15, 9)

    private var moveTimer = 0f
    var isGameOver = false
    var gameOverReason = ""

    private val inputQueue = ArrayDeque<GridDir>()

    // AI BRAIN
    private var brain: SnakeBrain = PassiveBrain() // Default
    private val historyBuffer = ArrayDeque<GameSnapshot>()
    private val MAX_HISTORY = 20

    fun setDifficulty(isAggressive: Boolean) {
        brain = if (isAggressive) HunterBrain() else PassiveBrain()
    }

    fun getBrainName() = brain.getName()

    fun reset() {
        isGameOver = false
        moveTimer = 0f
        inputQueue.clear()
        historyBuffer.clear()

        player = SnakeEntity(
            body = arrayListOf(Point(5, 14), Point(5, 15), Point(5, 16)),
            dir = GridDir.UP,
            nextDir = GridDir.UP
        )

        ai = SnakeEntity(
            body = arrayListOf(Point(26, 3), Point(26, 2), Point(26, 1)),
            dir = GridDir.DOWN,
            nextDir = GridDir.DOWN
        )

        spawnFood()
    }

    fun processInput(newDir: GridDir) {
        val lastPlannedDir = if (inputQueue.isNotEmpty()) inputQueue.last() else player.nextDir
        val isOpposite = (lastPlannedDir.x + newDir.x == 0) && (lastPlannedDir.y + newDir.y == 0)
        val isSame = lastPlannedDir == newDir

        if (!isOpposite && !isSame) {
            if (inputQueue.size < 2) inputQueue.add(newDir)
        }
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
        recordSnapshot()

        if (inputQueue.isNotEmpty()) {
            player.nextDir = inputQueue.removeFirst()
        }

        player.dir = player.nextDir

        // ASK BRAIN FOR MOVE
        ai.dir = brain.getMove(this)

        moveSnake(player)
        moveSnake(ai)
        checkCollisions()
    }

    private fun recordSnapshot() {
        val snapshot = GameSnapshot(
            playerBody = ArrayList(player.body), // Deep copy list
            aiBody = ArrayList(ai.body),         // Deep copy list
            food = food.copy(),
            pScore = player.score,
            aScore = ai.score,
            pHeadColor = SnakeConfig.COLOR_P1_HEAD,
            aHeadColor = SnakeConfig.COLOR_AI_HEAD
        )

        historyBuffer.add(snapshot)
        if (historyBuffer.size > MAX_HISTORY) {
            historyBuffer.removeFirst()
        }
    }

    fun getReplayHistory(): List<GameSnapshot> {
        val frames = 10
        val size = historyBuffer.size
        if (size <= frames) return historyBuffer.toList()

        // Convert to list and take the tail
        return historyBuffer.toList().takeLast(frames)
    }

    private fun moveSnake(snake: SnakeEntity) {
        if (!snake.isAlive) return

        val head = snake.head()
        val newHead = Point(head.x + snake.dir.x, head.y + snake.dir.y)

        snake.body.add(0, newHead)

        if (newHead == food) {
            snake.score++
            snake.didEat = true
            spawnFood()

            if (snake == player) {
                SoundManager.playPerfect(0)
            } else {
                SoundManager.playPlaceVariation()
            }
        } else {
            snake.didEat = false
            snake.body.removeAt(snake.body.size - 1)
        }
    }

    private fun checkCollisions() {
        if (!player.isAlive && !ai.isAlive) return

        val pHead = player.head()
        val aHead = ai.head()

        // 1. Wall & Self Collisions
        if (isOutOfBounds(pHead) || checkBodyCollision(pHead, player.body, 1)) player.isAlive = false
        if (isOutOfBounds(aHead) || checkBodyCollision(aHead, ai.body, 1)) ai.isAlive = false

        // 2. Enemy Body Collisions (Index 1+ to allow head-on comparison below)
        if (checkBodyCollision(pHead, ai.body, 1)) player.isAlive = false
        if (checkBodyCollision(aHead, player.body, 1)) ai.isAlive = false

        // 3. Head-to-Head Resolution
        if (pHead == aHead) {
            if (player.isAlive && ai.isAlive) {
                if (player.body.size > ai.body.size) ai.isAlive = false
                else if (ai.body.size > player.body.size) player.isAlive = false
                else { player.isAlive = false; ai.isAlive = false }
            } else {
                player.isAlive = false; ai.isAlive = false
            }
        }

        // 4. Game Over State
        if (!player.isAlive || !ai.isAlive) {
            isGameOver = true
            gameOverReason = when {
                !player.isAlive && !ai.isAlive -> "DRAW"
                !player.isAlive -> "ROBOT WON"
                else -> "YOU WON"
            }

            recordSnapshot()
        }
    }


    private fun isOutOfBounds(p: Point) = p.x !in 0..<TILE_COUNT_X || p.y < 0 || p.y >= TILE_COUNT_Y

    private fun checkBodyCollision(head: Point, body: List<Point>, skipIndex: Int): Boolean {
        for (i in skipIndex until body.size) {
            if (head == body[i]) return true
        }
        return false
    }

    private fun spawnFood() {
        var attempts = 0
        while (attempts < 100) {
            val fx = Random.nextInt(TILE_COUNT_X)
            val fy = Random.nextInt(TILE_COUNT_Y)
            if (isOccupied(fx, fy)) {
                attempts++
                continue
            }
            food = Point(fx, fy)
            break
        }
    }

    private fun isOccupied(x: Int, y: Int): Boolean {
        return player.body.any { it.x == x && it.y == y } || ai.body.any { it.x == x && it.y == y }
    }
}