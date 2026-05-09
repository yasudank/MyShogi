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
