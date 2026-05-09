package com.example.myshogi

object ShogiEvaluator {

    // 駒の点数テーブル（一般的な将棋の駒割りを参考）
    private val PIECE_VALUE = mapOf(
        PieceType.PAWN             to 100,
        PieceType.LANCE            to 300,
        PieceType.KNIGHT           to 300,
        PieceType.SILVER           to 500,
        PieceType.GOLD             to 600,
        PieceType.BISHOP           to 800,
        PieceType.ROOK             to 1000,
        PieceType.KING             to 100_000,
        PieceType.PROMOTED_PAWN    to 600,
        PieceType.PROMOTED_LANCE   to 600,
        PieceType.PROMOTED_KNIGHT  to 600,
        PieceType.PROMOTED_SILVER  to 600,
        PieceType.PROMOTED_BISHOP  to 1100,
        PieceType.PROMOTED_ROOK    to 1300,
    )

    /**
     * 局面のスコアを返す。
     * aiPlayer の有利が高いほど大きい正の値、不利なら負の値。
     */
    fun evaluate(state: GameState, aiPlayer: Player): Int {
        var score = 0

        // 盤上の駒
        for ((_, piece) in state.board) {
            val value = PIECE_VALUE[piece.type] ?: 0
            score += if (piece.owner == aiPlayer) value else -value
        }

        // 持ち駒（盤上より少し低く評価：打てる自由度を考慮して 0.8 倍）
        val ownHand = if (aiPlayer == Player.SENTE) state.capturedSente else state.capturedGote
        val oppHand = if (aiPlayer == Player.SENTE) state.capturedGote  else state.capturedSente
        for (type in ownHand) score += ((PIECE_VALUE[type] ?: 0) * 0.8).toInt()
        for (type in oppHand) score -= ((PIECE_VALUE[type] ?: 0) * 0.8).toInt()

        return score
    }
}

class ShogiAI(val aiPlayer: Player, val maxDepth: Int = 3) {

    /**
     * 現在局面から最善手を返す。合法手がない場合は null。
     */
    fun bestMove(state: GameState): Move? {
        var bestScore = Int.MIN_VALUE
        var bestMove: Move? = null

        for (move in getAllMoves(state, aiPlayer)) {
            val next = applyMove(state, move)
            val score = alphaBeta(next, maxDepth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    /**
     * アルファベータ枝刈りによる再帰探索。
     * isMaximizing = true  → aiPlayer の番（スコア最大化）
     * isMaximizing = false → 相手の番（スコア最小化）
     */
    private fun alphaBeta(
        state: GameState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean
    ): Int {
        // 葉ノード：評価値を返す
        if (depth == 0) return ShogiEvaluator.evaluate(state, aiPlayer)

        val currentPlayer = if (isMaximizing) aiPlayer else aiPlayer.opponent()
        val moves = getAllMoves(state, currentPlayer)

        // 合法手なし（詰み相当）
        if (moves.isEmpty()) return ShogiEvaluator.evaluate(state, aiPlayer)

        return if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            var a = alpha
            for (move in moves) {
                val score = alphaBeta(applyMove(state, move), depth - 1, a, beta, false)
                if (score > maxScore) maxScore = score
                if (score > a) a = score
                if (a >= beta) break  // ベータ枝刈り
            }
            maxScore
        } else {
            var minScore = Int.MAX_VALUE
            var b = beta
            for (move in moves) {
                val score = alphaBeta(applyMove(state, move), depth - 1, alpha, b, true)
                if (score < minScore) minScore = score
                if (score < b) b = score
                if (alpha >= b) break  // アルファ枝刈り
            }
            minScore
        }
    }
}
