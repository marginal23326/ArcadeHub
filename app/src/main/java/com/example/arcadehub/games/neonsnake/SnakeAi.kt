package com.example.arcadehub.games.neonsnake

import com.example.arcadehub.games.neonsnake.SnakeConfig.TILE_COUNT_X
import com.example.arcadehub.games.neonsnake.SnakeConfig.TILE_COUNT_Y
import kotlin.math.abs

// --- INTERFACE ---
interface SnakeBrain {
    fun getName(): String
    fun getMove(gameState: SnakePhysics): GridDir
}

// --- CONFIGURATION DATA ---
data class BrainConfig(
    val name: String,
    val suffocationPenaltyWeight: Double, // Weight for getting trapped
    val headOnFear: Double,               // Penalty for head-on collision risk (when smaller)
    val headOnAggression: Double,         // Bonus for head-on collision risk (when larger)
    val voronoiWeight: Double,            // Weight for territory control
    val hunterCrowdingWeight: Double,     // Bonus for getting closer to enemy head (0.0 = disabled)
    val foodGreedMultiplier: Double       // Multiplier for food value when larger (1.0 = normal, <1.0 = ignore food)
)

// --- CONSTANTS ---
private object AiWeights {
    const val WIN = 1_000_000.0
    const val LOSS = -1_000_000.0
}

// --- BASE CLASS (LOGIC ENGINE) ---
abstract class BaseSnakeBrain(private val config: BrainConfig) : SnakeBrain {

    override fun getName() = config.name

    protected fun evaluate(state: SimState, depth: Int): Double {
        if (state.aBody.isEmpty()) return AiWeights.LOSS - depth
        if (state.pBody.isEmpty()) return AiWeights.WIN + depth

        var score = 0.0
        val aHead = state.aBody[0]
        val pHead = state.pBody[0]

        // 1. SUFFOCATION CHECK
        val requiredSpace = state.aBody.size + 2
        val availableSpace = getReachableSpace(state, true, requiredSpace)

        if (availableSpace < requiredSpace) {
            // Apply configured penalty weight
            return AiWeights.LOSS + (availableSpace * config.suffocationPenaltyWeight)
        }

        // 2. Head-on Danger
        val dEnemy = dist(aHead, pHead)
        if (dEnemy <= 1) {
            if (state.aBody.size <= state.pBody.size) {
                score -= config.headOnFear
            } else {
                score += config.headOnAggression
            }
        }

        // 3. Voronoi (Territory)
        score += getVoronoiScore(state) * config.voronoiWeight

        // 4. Hunter Mode (Crowding)
        // If configured (weight > 0) and we are larger, try to get closer
        if (config.hunterCrowdingWeight > 0 && state.aBody.size > state.pBody.size) {
            score -= (dEnemy * config.hunterCrowdingWeight)
        }

        // 5. Food & Eating
        if (state.aiAte) score += 20_000.0
        if (state.food.x != -1) {
            val dFood = dist(aHead, state.food)
            val dEnemyFood = dist(pHead, state.food)

            var foodValue = ((TILE_COUNT_X + TILE_COUNT_Y) - dFood) * 50.0

            // Apply Greed Multiplier (Passive = 1.0, Hunter = 0.65)
            if (state.aBody.size > state.pBody.size) {
                foodValue *= config.foodGreedMultiplier
            }

            // Punish chasing lost causes
            if (dEnemyFood <= dFood && state.pBody.size >= state.aBody.size) {
                foodValue = -500.0
            }

            score += foodValue
        }

        return score
    }

    // --- DECISION LOGIC ---
    override fun getMove(gameState: SnakePhysics): GridDir {
        val state = SimState(gameState)
        val pPredX = state.pBody.first().x + state.pDir.x
        val pPredY = state.pBody.first().y + state.pDir.y

        val currentAiLen = state.aBody.size
        val currentPlayerLen = state.pBody.size

        var bestMove = state.aDir
        var bestVal = Double.NEGATIVE_INFINITY

        val moves = listOf(GridDir.UP, GridDir.DOWN, GridDir.LEFT, GridDir.RIGHT).filter { d ->
            val head = state.aBody[0]
            val neck = if (state.aBody.size > 1) state.aBody[1] else null
            !(neck != null && head.x + d.x == neck.x && head.y + d.y == neck.y)
        }.sortedBy { d ->
            val nx = state.aBody[0].x + d.x
            val ny = state.aBody[0].y + d.y
            abs(nx - state.food.x) + abs(ny - state.food.y)
        }

        for (move in moves) {
            val nextState = state.clone()
            nextState.applyMove(isAi = true, move = move)

            var value: Double

            if (nextState.aBody.isEmpty()) {
                value = AiWeights.LOSS
            } else {
                val myHead = nextState.aBody[0]
                val isDirectCollision = (myHead.x == pPredX && myHead.y == pPredY)
                val mySpace = getReachableSpace(nextState, true, currentAiLen + 2)
                val enemySpace = getReachableSpace(nextState, false, currentPlayerLen + 1)

                if (isDirectCollision && currentPlayerLen >= currentAiLen) {
                    value = AiWeights.LOSS
                } else if (mySpace < currentAiLen) {
                    value = AiWeights.LOSS + (mySpace * 100)
                } else if (enemySpace < currentPlayerLen) {
                    value = AiWeights.WIN + mySpace
                } else if (playerCanCaptureAiHead(nextState) && currentPlayerLen >= currentAiLen) {
                    value = -500_000.0
                } else {
                    value = minimax(nextState, 3, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false)
                }
            }

            if (value > bestVal) {
                bestVal = value
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(state: SimState, depth: Int, alpha: Double, beta: Double, isMaximizing: Boolean): Double {
        if (state.aBody.isEmpty()) return AiWeights.LOSS - depth
        if (state.pBody.isEmpty()) return AiWeights.WIN + depth
        if (depth == 0) return evaluate(state, depth)

        var currAlpha = alpha
        var currBeta = beta

        // Initialize based on who is playing
        var bestVal = if (isMaximizing) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        val activeBody = if (isMaximizing) state.aBody else state.pBody

        val moves = listOf(GridDir.UP, GridDir.DOWN, GridDir.LEFT, GridDir.RIGHT)

        for (move in moves) {
            // 1. Check 180 turn (Neck check)
            val head = activeBody[0]
            if (activeBody.size > 1 && head.x + move.x == activeBody[1].x && head.y + move.y == activeBody[1].y) continue

            // 2. Simulate Move
            val nextState = state.clone()
            // isMaximizing is true for AI, false for Player, which matches the isAi parameter
            nextState.applyMove(isAi = isMaximizing, move = move)

            // 3. Recurse (Flip turn)
            val eval = minimax(nextState, depth - 1, currAlpha, currBeta, !isMaximizing)

            // 4. Update Alpha/Beta
            if (isMaximizing) {
                bestVal = maxOf(bestVal, eval)
                currAlpha = maxOf(currAlpha, eval)
                if (currBeta <= currAlpha) break
            } else {
                bestVal = minOf(bestVal, eval)
                currBeta = minOf(currBeta, eval)
                if (currBeta <= currAlpha) break
            }
        }
        return bestVal
    }

    // --- UTILS ---
    private fun getVoronoiScore(state: SimState): Int {
        if (state.pBody.isEmpty()) return 1000
        if (state.aBody.isEmpty()) return -1000

        val grid = IntArray(TILE_COUNT_X * TILE_COUNT_Y)
        val mark = { body: List<Point> -> for (p in body) if (p.x in 0 until TILE_COUNT_X && p.y in 0 until TILE_COUNT_Y) grid[p.y * TILE_COUNT_X + p.x] = 3 }
        mark(state.pBody); mark(state.aBody)

        val q = ArrayDeque<Triple<Int, Int, Int>>()
        if(state.aBody.isNotEmpty()) q.add(Triple(state.aBody[0].x, state.aBody[0].y, 1))
        if(state.pBody.isNotEmpty()) q.add(Triple(state.pBody[0].x, state.pBody[0].y, 2))

        var aiCount = 0; var pCount = 0
        while (q.isNotEmpty()) {
            val (cx, cy, owner) = q.removeFirst()
            val idx = cy * TILE_COUNT_X + cx
            if (idx !in grid.indices || (grid[idx] != 0 && grid[idx] != owner)) continue
            if (grid[idx] == 0) {
                grid[idx] = owner
                if (owner == 1) aiCount++ else pCount++
                listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0).forEach { (dx, dy) ->
                    val nx = cx + dx; val ny = cy + dy
                    if (nx in 0 until TILE_COUNT_X && ny in 0 until TILE_COUNT_Y && grid[ny * TILE_COUNT_X + nx] == 0) q.add(Triple(nx, ny, owner))
                }
            }
        }
        return aiCount - pCount
    }

    private fun getReachableSpace(state: SimState, isAi: Boolean, maxCheck: Int): Int {
        val head = if (isAi) state.aBody[0] else state.pBody[0]
        val grid = BooleanArray(TILE_COUNT_X * TILE_COUNT_Y)
        val mark = { body: List<Point> -> for (i in 0 until body.size - 1) { val p = body[i]; if (p.x in 0 until TILE_COUNT_X && p.y in 0 until TILE_COUNT_Y) grid[p.y * TILE_COUNT_X + p.x] = true } }
        mark(state.aBody); mark(state.pBody)

        val queue = IntArray(TILE_COUNT_X * TILE_COUNT_Y)
        var qHead = 0; var qTail = 0; var count = 0
        val startIdx = head.y * TILE_COUNT_X + head.x
        if (startIdx in grid.indices) { grid[startIdx] = true; queue[qHead++] = startIdx }

        while (qHead > qTail) {
            if (count >= maxCheck) return maxCheck
            val curr = queue[qTail++]; count++
            val cx = curr % TILE_COUNT_X; val cy = curr / TILE_COUNT_X
            listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0).forEach { (dx, dy) ->
                val nx = cx + dx; val ny = cy + dy
                val nIdx = ny * TILE_COUNT_X + nx
                if (nx in 0 until TILE_COUNT_X && ny in 0 until TILE_COUNT_Y && !grid[nIdx]) { grid[nIdx] = true; queue[qHead++] = nIdx }
            }
        }
        return count
    }

    private fun playerCanCaptureAiHead(state: SimState): Boolean {
        if (state.pBody.isEmpty() || state.aBody.isEmpty()) return false
        val pHead = state.pBody[0]
        val pNeck = if (state.pBody.size > 1) state.pBody[1] else null
        val target = state.aBody[0]
        return listOf(GridDir.UP, GridDir.DOWN, GridDir.LEFT, GridDir.RIGHT).any { d ->
            val nx = pHead.x + d.x; val ny = pHead.y + d.y
            (pNeck == null || nx != pNeck.x || ny != pNeck.y) && nx == target.x && ny == target.y
        }
    }

    protected fun dist(p1: Point, p2: Point) = abs(p1.x - p2.x) + abs(p1.y - p2.y)
}

// --- CONCRETE BRAINS ---

class PassiveBrain : BaseSnakeBrain(
    BrainConfig(
        name = "STANDARD (PASSIVE)",
        suffocationPenaltyWeight = 100.0,
        headOnFear = 50_000.0,
        headOnAggression = 20_000.0,
        voronoiWeight = 100.0,
        hunterCrowdingWeight = 0.0, // Disabled
        foodGreedMultiplier = 1.0   // Normal food value
    )
)

class HunterBrain : BaseSnakeBrain(
    BrainConfig(
        name = "HUNTER (AGGRESSIVE)",
        suffocationPenaltyWeight = 1000.0,
        headOnFear = 500_000.0,
        headOnAggression = 50_000.0,
        voronoiWeight = 500.0,
        hunterCrowdingWeight = 200.0, // Active
        foodGreedMultiplier = 0.65    // Care less about food when big
    )
)

// --- SIMULATION STATE ---
class SimState(
    var pBody: ArrayList<Point>,
    var aBody: ArrayList<Point>,
    var pDir: GridDir,
    var aDir: GridDir,
    var food: Point,
    var aiAte: Boolean = false
) {
    constructor(phys: SnakePhysics) : this(
        ArrayList(phys.player.body),
        ArrayList(phys.ai.body),
        phys.player.dir,
        phys.ai.dir,
        phys.food
    )

    fun clone(): SimState {
        return SimState(ArrayList(pBody), ArrayList(aBody), pDir, aDir, food, aiAte)
    }

    fun applyMove(isAi: Boolean, move: GridDir) {
        val body = if (isAi) aBody else pBody
        if (body.isEmpty()) return

        val head = body[0]
        val nx = head.x + move.x
        val ny = head.y + move.y

        if (nx !in 0 until TILE_COUNT_X || ny !in 0 until TILE_COUNT_Y) { body.clear(); return }
        if (pBody.any { it.x == nx && it.y == ny } || aBody.any { it.x == nx && it.y == ny }) { body.clear(); return }

        val newHead = Point(nx, ny)
        body.add(0, newHead)

        if (nx == food.x && ny == food.y) {
            food = Point(-1, -1)
            if (isAi) aiAte = true
        } else {
            body.removeAt(body.size - 1)
        }

        if (isAi) aDir = move else pDir = move
    }
}