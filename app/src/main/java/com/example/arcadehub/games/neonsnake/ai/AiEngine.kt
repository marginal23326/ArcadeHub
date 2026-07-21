package com.example.arcadehub.games.neonsnake.ai

import com.example.arcadehub.games.neonsnake.GridDir
import com.example.arcadehub.games.neonsnake.Point
import com.example.arcadehub.games.neonsnake.SnakeEntity
import com.example.arcadehub.games.neonsnake.SnakeGrid
import com.example.arcadehub.games.neonsnake.SnakeMapGenerator
import java.util.concurrent.ConcurrentHashMap

object AiEngine {

    fun decideMove(
        me: SnakeEntity,
        enemy: SnakeEntity,
        foods: List<Point>,
        cols: Int,
        rows: Int,
        mapType: SnakeMapGenerator.MapType,
        cfg: AiConfig
    ): GridDir {
        val layout = layoutFor(cols, rows, mapType)
        val grid = AiGrid.create(cols, rows, layout.walls, layout.topology)

        for (f in foods) grid.set(f.x, f.y, 1)
        for (p in me.body) grid.set(p.x, p.y, 2)
        for (p in enemy.body) grid.set(p.x, p.y, 3)

        val meAgent = SearchAgent()
        meAgent.body.loadFrom(me.body)
        meAgent.health = me.health

        val enemyAgent = SearchAgent()
        enemyAgent.body.loadFrom(enemy.body)
        enemyAgent.health = enemy.health

        val callerBuffers = threadState(cols, rows).buffers

        val distMap = if (cfg.maxDepth <= 0) Pathfinding.foodDistanceMap(grid, callerBuffers) else null

        val zobrist = ZobristCache.forSize(cols, rows)
        val initialHash = zobrist.computeHash(grid, meAgent.health, enemyAgent.health)

        val tt = SharedTranspositionTable.get()
        tt.prepareForSearch(resolveTtEntries(cfg))

        val threadCount = resolveThreadCount(cfg)
        val rootResults = arrayOfNulls<IntArray>(threadCount) // [x, y, dirInt] per thread

        AiWorkerPool.runAll(threadCount) { threadId ->
            val state = threadState(cols, rows)
            resetHistory(state.historyTable)

            val gridCopy = if (threadCount == 1) grid else grid.copyOf()
            val meCopy = if (threadCount == 1) meAgent else meAgent.copyOf()
            val enemyCopy = if (threadCount == 1) enemyAgent else enemyAgent.copyOf()

            val ctx = SearchContext(cfg.maxDepth, state.historyTable, cfg, tt, zobrist, state.buffers, threadId)
            negamax(
                gridCopy, meCopy, enemyCopy, distMap,
                cfg.maxDepth, -2_000_000_000, 2_000_000_000, 0, initialHash, 0, 0, ctx
            )
            rootResults[threadId] = intArrayOf(ctx.rootBestMoveX, ctx.rootBestMoveY, ctx.rootBestMoveDirInt)
        }

        val primary = rootResults[0]
        if (primary != null && primary[0] != -1) {
            val dir = directionFromIndex(primary[2])
            if (dir != null) return dir
        }

        return fallbackMove(grid, meAgent, callerBuffers)
    }

    private fun fallbackMove(grid: AiGrid, me: SearchAgent, buffers: SearchBuffers): GridDir {
        if (me.body.isEmpty()) return GridDir.UP

        val headX = me.body.headX()
        val headY = me.body.headY()

        var bestDir: GridDir? = null
        var bestCount = -1

        for (dir in arrayOf(GridDir.UP, GridDir.DOWN, GridDir.LEFT, GridDir.RIGHT)) {
            val nx = headX + dir.x
            val ny = headY + dir.y
            if (grid.isSafe(nx, ny)) {
                val ff = FloodFill.run(grid, nx, ny, 100, me.body, null, buffers)
                if (ff.count > bestCount) {
                    bestCount = ff.count
                    bestDir = dir
                }
            }
        }

        return bestDir ?: GridDir.UP
    }

    private fun resolveTtEntries(cfg: AiConfig): Int =
        if (cfg.runtime.hashEntries > 0) cfg.runtime.hashEntries else TranspositionTable.depthBasedEntries(cfg.maxDepth)

    private fun resolveThreadCount(cfg: AiConfig): Int {
        if (cfg.runtime.threads > 0) return cfg.runtime.threads
        val cores = Runtime.getRuntime().availableProcessors()
        return (cores - 1).coerceAtLeast(1)
    }

    private fun resetHistory(table: Array<IntArray>) {
        table[0].fill(0)
        table[1].fill(0)
    }

    private class PerThreadState(val width: Int, val height: Int) {
        val historyTable = arrayOf(IntArray(width * height * 4), IntArray(width * height * 4))
        val buffers = SearchBuffers(width, height)
    }

    private val threadLocalState = ThreadLocal<PerThreadState>()

    private fun threadState(width: Int, height: Int): PerThreadState {
        val existing = threadLocalState.get()
        if (existing != null && existing.width == width && existing.height == height) return existing
        val fresh = PerThreadState(width, height)
        threadLocalState.set(fresh)
        return fresh
    }

    private class MapLayout(val walls: BitBoard, val topology: BoardTopology)

    private val layoutCache = ConcurrentHashMap<Triple<Int, Int, SnakeMapGenerator.MapType>, MapLayout>()

    private fun layoutFor(cols: Int, rows: Int, mapType: SnakeMapGenerator.MapType): MapLayout {
        val key = Triple(cols, rows, mapType)
        return layoutCache.getOrPut(key) {
            val tempGrid = SnakeGrid(cols, rows)
            SnakeMapGenerator.applyMap(tempGrid, mapType)
            val bits = BitBoard(BitBoard.wordsFor(cols * rows))
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    if (tempGrid[x, y] == 9) bits.set(y * cols + x)
                }
            }
            val base = BoardTopology(cols, rows, bits.numWords)
            val topology = if (bits.any()) base.withBlockedCells(bits) else base
            MapLayout(bits, topology)
        }
    }

    fun releaseResources() {
        AiWorkerPool.shutdown()
        SharedTranspositionTable.reset()
        threadLocalState.remove()
    }
}
