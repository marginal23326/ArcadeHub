package com.example.arcadehub.games.neonsnake.ai

import kotlin.math.abs

private const val QUIESCENCE_MAX_EXTENSIONS = 8
private const val SCORE_MIN = -2_000_000_000

fun negamax(
    grid: AiGrid,
    me: SearchAgent,
    enemy: SearchAgent,
    distMap: IntArray?,
    depth: Int,
    alphaIn: Int,
    betaIn: Int,
    side: Int,
    currentHash: Long,
    qDepth: Int,
    ply: Int,
    ctx: SearchContext
): Int {
    var alpha = alphaIn
    var beta = betaIn

    ctx.nodesSearched++

    val originalAlpha = alpha
    val buffers = ctx.buffers
    val probe = buffers.ttProbe
    val ttHit = ctx.tt.probe(currentHash, probe)

    if (depth != ctx.rootDepth && ttHit && probe.depth >= depth) {
        when (probe.flag) {
            TT_FLAG_EXACT -> return probe.score
            TT_FLAG_LOWER -> if (probe.score > alpha) alpha = probe.score
            TT_FLAG_UPPER -> if (probe.score < beta) beta = probe.score
        }
        if (alpha >= beta) return probe.score
    }

    if (me.body.isEmpty() || me.health <= 0) {
        return ctx.cfg.scores.loss - depth
    }
    if (enemy.body.isEmpty() || enemy.health <= 0) {
        return ctx.cfg.scores.win + depth
    }

    if (depth == 0) {
        if (ctx.rootDepth >= 3 && qDepth < QUIESCENCE_MAX_EXTENSIONS &&
            MoveGeneration.shouldExtendLeaf(grid, me, enemy, ctx.cfg, buffers)
        ) {
            return negamax(grid, me, enemy, distMap, 1, alpha, beta, side, currentHash, qDepth + 1, ply + 1, ctx)
        }

        val score = if (side == 0) {
            Heuristics.evaluate(grid, me, enemy, distMap, ctx.cfg, buffers)
        } else {
            -Heuristics.evaluate(grid, enemy, me, distMap, ctx.cfg, buffers)
        }

        ctx.tt.set(currentHash, 0, score, TT_FLAG_EXACT, 255, 255, 255)
        return score
    }

    val isRoot = ctx.rootDepth == depth && side == 0
    val headX = me.body.headX()
    val headY = me.body.headY()
    val headIdx = grid.idx(headX, headY)

    val moveList = buffers.moveListPool[ply]
    MoveGeneration.getSafeNeighbors(grid, me, enemy, moveList)

    if (moveList.count == 0) {
        return ctx.cfg.scores.loss - depth
    }

    val hasPv = ttHit && probe.mvX != 255
    val pvX = if (hasPv) probe.mvX else -1
    val pvY = if (hasPv) probe.mvY else -1

    if (moveList.count > 1) {
        sortMoves(moveList, hasPv, pvX, pvY, ctx.historyTable[side], headIdx, grid, buffers.moveOrderDistScratch)
        if (isRoot && ctx.threadId > 0) {
            rotateLeft(moveList, ctx.threadId % moveList.count)
        }
    }

    var bestMoveX = moveList.x(0)
    var bestMoveY = moveList.y(0)
    var bestMoveDirInt = moveList.dirInt(0)
    var bestScore = SCORE_MIN
    var bestTieBreak = SCORE_MIN

    val moveCount = moveList.count
    for (i in 0 until moveCount) {
        val mvX = moveList.x(i)
        val mvY = moveList.y(i)
        val mvDirInt = moveList.dirInt(i)

        var collisionPenalty = 0
        var killThreatBonus = 0
        if (side == 0 && !enemy.body.isEmpty()) {
            val oppHeadX = enemy.body.headX()
            val oppHeadY = enemy.body.headY()
            if (abs(mvX - oppHeadX) + abs(mvY - oppHeadY) == 1) {
                val myLen = me.body.len
                val oppLen = enemy.body.len
                when {
                    oppLen > myLen -> collisionPenalty = ctx.cfg.scores.headOnCollision
                    oppLen == myLen -> collisionPenalty = ctx.cfg.scores.draw
                    else -> killThreatBonus = ctx.cfg.scores.killPressure
                }
            }
        }

        val tieBreak = if (isRoot) {
            MoveGeneration.rootTieBreaker(me, enemy, grid.width, grid.height, mvX, mvY)
        } else {
            0
        }

        val originalHeadVal = grid.get(mvX, mvY)
        val ateFood = originalHeadVal == 1

        var tailRestoreX = -1
        var tailRestoreY = -1
        var tailRestoreVal = 0
        var hasTailRestore = false
        var poppedTailPacked = 0
        var hasPoppedTail = false

        var nextHash = currentHash
        val oldHealth = me.health
        val newHealth = if (ateFood) 100 else oldHealth - 1

        me.health = newHealth
        me.body.pushFront(mvX, mvY)

        val cellId = 2 + side

        nextHash = ctx.zobrist.xorHealth(nextHash, side, oldHealth, newHealth)
        if (originalHeadVal != 0) {
            nextHash = ctx.zobrist.xorUnchecked(nextHash, mvX, mvY, originalHeadVal)
        }
        nextHash = ctx.zobrist.xorUnchecked(nextHash, mvX, mvY, cellId)

        if (!ateFood) {
            val tailPacked = me.body.popBack()
            poppedTailPacked = tailPacked
            hasPoppedTail = true
            val tailX = FastBody.unpackX(tailPacked)
            val tailY = FastBody.unpackY(tailPacked)
            if (tailX != mvX || tailY != mvY) {
                val originalTailVal = grid.get(tailX, tailY)
                if (originalTailVal == cellId) {
                    grid.clearUnchecked(tailX, tailY, originalTailVal)
                    tailRestoreX = tailX
                    tailRestoreY = tailY
                    tailRestoreVal = originalTailVal
                    hasTailRestore = true
                }
                nextHash = ctx.zobrist.xorUnchecked(nextHash, tailX, tailY, cellId)
            }
        }

        grid.replaceUnchecked(mvX, mvY, originalHeadVal, cellId)

        var rootBonus = 0
        if (isRoot) {
            MoveGeneration.getSafeNeighbors(grid, me, enemy, buffers.moveListPool[ply + 1])
            val continuationMoves = buffers.moveListPool[ply + 1].count
            if (continuationMoves == 0) {
                rootBonus += ctx.cfg.scores.trapDanger
            }

            val myLen = me.body.len
            val enemyLen = enemy.body.len
            val totalLen = myLen + enemyLen
            val totalArea = grid.width * grid.height
            val denseTailRace = myLen >= 20 && enemyLen >= 20 &&
                (totalLen * 100) / totalArea >= ctx.cfg.denseTailRaceOccupancyPercent

            if (denseTailRace && !enemy.body.isEmpty()) {
                val enemyHeadX = enemy.body.headX()
                val enemyHeadY = enemy.body.headY()
                if (continuationMoves == 1 && abs(mvX - enemyHeadX) + abs(mvY - enemyHeadY) <= 5) {
                    rootBonus -= abs(ctx.cfg.scores.territoryControl) * 120
                }
                if (mvX == enemy.body.lastX() && mvY == enemy.body.lastY()) {
                    rootBonus -= abs(ctx.cfg.scores.territoryControl) * 2
                }
            }
        }

        val childScore: Int
        if (i == 0) {
            childScore = negamax(grid, enemy, me, null, depth - 1, -beta, -alpha, 1 - side, nextHash, qDepth, ply + 1, ctx)
        } else {
            var needsFullSearch = true
            var lmrScore = 0

            if (depth >= 5 && !ateFood && collisionPenalty == 0 && killThreatBonus == 0) {
                val childLmrScore =
                    negamax(grid, enemy, me, null, depth - 2, -alpha - 1, -alpha, 1 - side, nextHash, qDepth, ply + 1, ctx)
                val tempModLmr = calcModScore(childLmrScore, collisionPenalty, killThreatBonus, ateFood, rootBonus, ctx.cfg)
                val isMassiveWin = tempModLmr > 50_000_000
                if (tempModLmr < alpha && !isMassiveWin) {
                    lmrScore = childLmrScore
                    needsFullSearch = false
                }
            }

            childScore = if (needsFullSearch) {
                val nullWindowScore =
                    negamax(grid, enemy, me, null, depth - 1, -alpha - 1, -alpha, 1 - side, nextHash, qDepth, ply + 1, ctx)
                val tempMod = calcModScore(nullWindowScore, collisionPenalty, killThreatBonus, ateFood, rootBonus, ctx.cfg)
                if (tempMod > alpha && tempMod < beta) {
                    negamax(grid, enemy, me, null, depth - 1, -beta, -alpha, 1 - side, nextHash, qDepth, ply + 1, ctx)
                } else {
                    nullWindowScore
                }
            } else {
                lmrScore
            }
        }

        val modifiedScore = calcModScore(childScore, collisionPenalty, killThreatBonus, ateFood, rootBonus, ctx.cfg)

        grid.replaceUnchecked(mvX, mvY, cellId, originalHeadVal)
        if (hasTailRestore) {
            grid.replaceUnchecked(tailRestoreX, tailRestoreY, 0, tailRestoreVal)
        }
        me.body.popFront()
        if (hasPoppedTail) {
            me.body.pushBackPacked(poppedTailPacked)
        }
        me.health = oldHealth

        if (modifiedScore > bestScore || (modifiedScore == bestScore && tieBreak > bestTieBreak)) {
            bestScore = modifiedScore
            bestMoveX = mvX
            bestMoveY = mvY
            bestMoveDirInt = mvDirInt
            bestTieBreak = tieBreak
        }
        if (bestScore > alpha) alpha = bestScore
        if (alpha >= beta) break
    }

    ctx.historyTable[side][headIdx * 4 + bestMoveDirInt] += depth * depth

    val ttFlag = when {
        bestScore <= originalAlpha -> TT_FLAG_UPPER
        bestScore >= beta -> TT_FLAG_LOWER
        else -> TT_FLAG_EXACT
    }
    ctx.tt.set(currentHash, depth, bestScore, ttFlag, bestMoveX, bestMoveY, bestMoveDirInt)

    if (isRoot) {
        ctx.rootBestMoveX = bestMoveX
        ctx.rootBestMoveY = bestMoveY
        ctx.rootBestMoveDirInt = bestMoveDirInt
    }

    return bestScore
}

private fun calcModScore(
    cScore: Int,
    collisionPenalty: Int,
    killThreatBonus: Int,
    ateFood: Boolean,
    rootBonus: Int,
    cfg: AiConfig
): Int {
    var ms = -cScore
    if (collisionPenalty < 0) ms = minOf(ms, collisionPenalty)

    val terminalBand = abs(ms) >= (abs(cfg.scores.win) / 10) * 9
    if (!terminalBand) {
        if (killThreatBonus > 0) ms = saturatingAdd(ms, killThreatBonus)
        if (ateFood && ms > -50_000_000) ms = saturatingAdd(ms, cfg.scores.eatReward)
    }
    return saturatingAdd(ms, rootBonus)
}

private fun sortMoves(
    moveList: MoveList,
    hasPv: Boolean,
    pvX: Int,
    pvY: Int,
    history: IntArray,
    headIdx: Int,
    grid: AiGrid,
    distScratch: IntArray
) {
    val moves = moveList.moves
    val count = moveList.count

    for (i in 0 until count) distScratch[i] = 1000
    val words = grid.food.words
    for (w in words.indices) {
        var v = words[w]
        while (v != 0L) {
            val bit = java.lang.Long.numberOfTrailingZeros(v)
            val idx = (w shl 6) or bit
            val fx = idx % grid.width
            val fy = idx / grid.width
            for (i in 0 until count) {
                val d = abs(MoveList.xOfPacked(moves[i]) - fx) + abs(MoveList.yOfPacked(moves[i]) - fy)
                if (d < distScratch[i]) distScratch[i] = d
            }
            v = v and (v - 1)
        }
    }

    for (i in 1 until count) {
        val key = moves[i]
        val keyDist = distScratch[i]
        var j = i - 1
        while (j >= 0 && compareMoves(moves[j], distScratch[j], key, keyDist, hasPv, pvX, pvY, history, headIdx, grid) > 0) {
            moves[j + 1] = moves[j]
            distScratch[j + 1] = distScratch[j]
            j--
        }
        moves[j + 1] = key
        distScratch[j + 1] = keyDist
    }
}

private fun compareMoves(
    a: Int,
    aDist: Int,
    b: Int,
    bDist: Int,
    hasPv: Boolean,
    pvX: Int,
    pvY: Int,
    history: IntArray,
    headIdx: Int,
    grid: AiGrid
): Int {
    val ax = MoveList.xOfPacked(a)
    val ay = MoveList.yOfPacked(a)
    val aDir = MoveList.dirIntOfPacked(a)
    val bx = MoveList.xOfPacked(b)
    val by = MoveList.yOfPacked(b)
    val bDir = MoveList.dirIntOfPacked(b)

    if (hasPv) {
        if (ax == pvX && ay == pvY) return -1
        if (bx == pvX && by == pvY) return 1
    }

    val histA = history[headIdx * 4 + aDir]
    val histB = history[headIdx * 4 + bDir]
    if (histA != histB) return if (histA > histB) -1 else 1 // descending history score

    if (aDist == bDist) {
        val cx = grid.width / 2.0
        val cy = grid.height / 2.0
        val ca = abs(ax - cx) + abs(ay - cy)
        val cb = abs(bx - cx) + abs(by - cy)
        return ca.compareTo(cb)
    }
    return aDist - bDist
}

private fun rotateLeft(moveList: MoveList, shift: Int) {
    if (shift == 0) return
    val moves = moveList.moves
    val count = moveList.count
    val rotated = IntArray(count)
    for (i in 0 until count) {
        rotated[i] = moves[(i + shift) % count]
    }
    for (i in 0 until count) {
        moves[i] = rotated[i]
    }
}
