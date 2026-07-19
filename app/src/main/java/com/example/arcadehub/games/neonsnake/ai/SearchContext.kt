package com.example.arcadehub.games.neonsnake.ai

class SearchContext(
    val rootDepth: Int,
    val historyTable: Array<IntArray>, // [side][headIdx * 4 + dirInt]
    val cfg: AiConfig,
    val tt: TranspositionTable,
    val zobrist: Zobrist,
    val buffers: SearchBuffers,
    val threadId: Int
) {
    var nodesSearched: Long = 0

    var rootBestMoveX: Int = -1
    var rootBestMoveY: Int = -1
    var rootBestMoveDirInt: Int = -1
}
