package com.example.arcadehub.games.neonsnake

import com.example.arcadehub.games.neonsnake.SnakeConfig.TILE_COUNT_X
import com.example.arcadehub.games.neonsnake.SnakeConfig.TILE_COUNT_Y
import kotlin.math.abs
import kotlin.random.Random

class SnakePhysics {

    lateinit var player: SnakeEntity
    lateinit var ai: SnakeEntity
    var food = Point(15, 10)

    private var moveTimer = 0f
    var isGameOver = false
    var gameOverReason = ""

    // Input Buffer
    private val inputQueue = ArrayDeque<GridDir>()

    // Reusable structures
    private val collisionGrid = BooleanArray(TILE_COUNT_X * TILE_COUNT_Y)
    private val floodStack = IntArray(TILE_COUNT_X * TILE_COUNT_Y)

    fun reset() {
        isGameOver = false
        moveTimer = 0f
        inputQueue.clear()

        // P1 starts Bottom Left (32x18 coords)
        player = SnakeEntity(
            body = arrayListOf(Point(5, 14), Point(5, 15), Point(5, 16)),
            dir = GridDir.UP,
            nextDir = GridDir.UP
        )

        // AI starts Top Right (32x18 coords)
        ai = SnakeEntity(
            body = arrayListOf(Point(26, 3), Point(26, 2), Point(26, 1)),
            dir = GridDir.DOWN,
            nextDir = GridDir.DOWN
        )

        spawnFood()
    }

    /**
     * Handles buffering inputs to ensure quick sequences (e.g. Left -> Down)
     * are executed sequentially over ticks rather than overwriting each other.
     */
    fun processInput(newDir: GridDir) {
        // The reference direction is the last one in the queue,
        // or the snake's currently executing direction if queue is empty.
        val lastPlannedDir = if (inputQueue.isNotEmpty()) inputQueue.last() else player.nextDir

        // 1. Prevent 180-degree turns (Moving opposite to current path)
        val isOpposite = (lastPlannedDir.x + newDir.x == 0) && (lastPlannedDir.y + newDir.y == 0)

        // 2. Prevent spamming the same direction
        val isSame = lastPlannedDir == newDir

        if (!isOpposite && !isSame) {
            // Limit buffer to 2 inputs to prevent uncontrolled "runaway" input lag
            if (inputQueue.size < 2) {
                inputQueue.add(newDir)
            }
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
        // CONSUME INPUT
        if (inputQueue.isNotEmpty()) {
            player.nextDir = inputQueue.removeFirst()
        }

        player.dir = player.nextDir
        ai.dir = getSmartMove()

        moveSnake(player)
        moveSnake(ai)
        checkCollisions()
    }

    private fun moveSnake(snake: SnakeEntity) {
        if (!snake.isAlive) return

        val head = snake.head()
        val newHead = Point(head.x + snake.dir.x, head.y + snake.dir.y)

        snake.body.add(0, newHead) // Unshift

        if (newHead == food) {
            snake.score++
            snake.didEat = true
            spawnFood()
        } else {
            snake.didEat = false
            snake.body.removeAt(snake.body.size - 1) // Pop
        }
    }

    private fun checkCollisions() {
        if (!player.isAlive && !ai.isAlive) return

        val pHead = player.head()
        val aHead = ai.head()

        // Check bounds & body collisions
        if (isOutOfBounds(pHead) || checkBodyCollision(pHead, player.body, 1) || checkBodyCollision(pHead, ai.body, 0)) {
            player.isAlive = false
        }

        if (isOutOfBounds(aHead) || checkBodyCollision(aHead, ai.body, 1) || checkBodyCollision(aHead, player.body, 0)) {
            ai.isAlive = false
        }

        // Head to Head
        if (pHead == aHead) {
            player.isAlive = false
            ai.isAlive = false
        }

        if (!player.isAlive || !ai.isAlive) {
            isGameOver = true
            gameOverReason = when {
                !player.isAlive && !ai.isAlive -> "DRAW: HEAD-ON COLLISION"
                !player.isAlive -> "AI WINS: ELIMINATED"
                else -> "YOU WIN: AI CRASHED"
            }
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
        while (true) {
            val fx = Random.nextInt(TILE_COUNT_X)
            val fy = Random.nextInt(TILE_COUNT_Y)
            if (isOccupied(fx, fy)) continue
            food = Point(fx, fy)
            break
        }
    }

    // --- AI LOGIC ---

    private fun getSmartMove(): GridDir {
        val head = ai.head()
        val pHead = player.head()

        val moves = listOf(GridDir.UP, GridDir.DOWN, GridDir.LEFT, GridDir.RIGHT)
        var bestMove = ai.dir
        var bestScore = Int.MIN_VALUE.toDouble()

        val distP_Food = abs(pHead.x - food.x) + abs(pHead.y - food.y)
        val playerAboutToEat = distP_Food <= 1

        for (move in moves) {
            val nX = head.x + move.x
            val nY = head.y + move.y
            var currentScore = 0.0

            if (nX !in 0..<TILE_COUNT_X || nY < 0 || nY >= TILE_COUNT_Y) continue
            if (isOccupied(nX, nY, ignoreTailOf = if(ai.didEat) null else ai, playerMightGrow = playerAboutToEat)) continue

            var exits = 0
            val dirs = listOf(Pair(0,1), Pair(0,-1), Pair(1,0), Pair(-1,0))
            for ((dx, dy) in dirs) {
                if (!isOccupied(nX + dx, nY + dy, playerMightGrow = playerAboutToEat)) exits++
            }
            if (exits <= 2) {
                val distToPlayerHead = abs(nX - pHead.x) + abs(nY - pHead.y)
                if (distToPlayerHead < 4) currentScore += SnakeConfig.AI_SCORE_TUNNEL_DEATH
            }

            val space = floodFill(nX, nY, playerAboutToEat)
            if (space < ai.body.size + 2) currentScore += SnakeConfig.AI_SCORE_TRAPPED
            else currentScore += (space * SnakeConfig.AI_SCORE_SPACE_MULTIPLIER)

            val pNextHeadX = pHead.x + player.dir.x
            val pNextHeadY = pHead.y + player.dir.y
            if (nX == pNextHeadX && nY == pNextHeadY) {
                currentScore += SnakeConfig.AI_SCORE_HEAD_ON_COLLISION
            }

            val distF = abs(nX - food.x) + abs(nY - food.y)
            if (distF < 10) {
                currentScore += (10 - distF) * 1500
            }

            val predictPx = pHead.x + player.dir.x * 3
            val predictPy = pHead.y + player.dir.y * 3
            val distI = abs(nX - predictPx) + abs(nY - predictPy)
            currentScore += (5000 - distI * 400)

            if (currentScore > bestScore) {
                bestScore = currentScore
                bestMove = move
            }
        }

        return bestMove
    }

    private fun isOccupied(x: Int, y: Int, ignoreTailOf: SnakeEntity? = null, playerMightGrow: Boolean = false): Boolean {
        if (x < 0 || x >= TILE_COUNT_X || y < 0 || y >= TILE_COUNT_Y) return true

        val pLimit = if (playerMightGrow) player.body.size else player.body.size - 1
        for (i in 0 until pLimit) {
            if (player.body[i].x == x && player.body[i].y == y) return true
        }

        val aLimit = if (ai === ignoreTailOf) ai.body.size - 1 else ai.body.size
        for (i in 0 until aLimit) {
            if (ai.body[i].x == x && ai.body[i].y == y) return true
        }
        return false
    }

    private fun floodFill(startX: Int, startY: Int, playerMightGrow: Boolean): Int {
        collisionGrid.fill(false)
        markSnakeOnGrid(player, playerMightGrow)
        markSnakeOnGrid(ai, false)

        var count = 0
        var head = 0
        var tail = 0

        val startIdx = startY * TILE_COUNT_X + startX
        if (collisionGrid[startIdx]) return 0

        floodStack[head++] = startIdx
        collisionGrid[startIdx] = true
        count++

        while (head > tail && count < 200) {
            val current = floodStack[tail++]
            val cx = current % TILE_COUNT_X
            val cy = current / TILE_COUNT_X

            checkAndPush(cx + 1, cy, { head }, { head++ }, { count++ })
            checkAndPush(cx - 1, cy, { head }, { head++ }, { count++ })
            checkAndPush(cx, cy + 1, { head }, { head++ }, { count++ })
            checkAndPush(cx, cy - 1, { head }, { head++ }, { count++ })
        }
        return count
    }

    private inline fun checkAndPush(x: Int, y: Int, refHead: () -> Int, incHead: () -> Unit, incCount: () -> Unit) {
        if (x in 0 until TILE_COUNT_X && y in 0 until TILE_COUNT_Y) {
            val idx = y * TILE_COUNT_X + x
            if (!collisionGrid[idx]) {
                collisionGrid[idx] = true
                floodStack[refHead()] = idx
                incHead()
                incCount()
            }
        }
    }

    private fun markSnakeOnGrid(snake: SnakeEntity, mightGrow: Boolean) {
        val limit = if (mightGrow || snake.didEat) snake.body.size else snake.body.size - 1
        for (i in 0 until limit) {
            val p = snake.body[i]
            if (p.x in 0 until TILE_COUNT_X && p.y in 0 until TILE_COUNT_Y) {
                collisionGrid[p.y * TILE_COUNT_X + p.x] = true
            }
        }
    }
}