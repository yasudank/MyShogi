package com.example.myshogi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import java.io.Serializable

enum class Player {
    SENTE, GOTE;
    fun opponent() = if (this == SENTE) GOTE else SENTE
}

enum class PieceType(val label: String) {
    PAWN("歩"),
    LANCE("香"),
    KNIGHT("桂"),
    SILVER("銀"),
    GOLD("金"),
    BISHOP("角"),
    ROOK("飛"),
    KING("玉"),
    PROMOTED_PAWN("と"),
    PROMOTED_LANCE("杏"),
    PROMOTED_KNIGHT("圭"),
    PROMOTED_SILVER("全"),
    PROMOTED_BISHOP("馬"),
    PROMOTED_ROOK("龍")
}

data class Piece(
    val type: PieceType,
    val owner: Player
) : Serializable

data class Position(val row: Int, val col: Int) : Serializable

data class GameState(
    val board: Map<Position, Piece>,
    val turn: Player,
    val capturedSente: List<PieceType>,
    val capturedGote: List<PieceType>
) : Serializable

class ShogiGame {
    var board by mutableStateOf<Map<Position, Piece>>(emptyMap())
    var turn by mutableStateOf(Player.SENTE)
    var capturedSente by mutableStateOf<List<PieceType>>(emptyList())
    var capturedGote by mutableStateOf<List<PieceType>>(emptyList())
    
    var history by mutableStateOf<List<GameState>>(emptyList())
    var mattaCountSente by mutableStateOf(3)
    var mattaCountGote by mutableStateOf(3)
    var winner by mutableStateOf<Player?>(null)

    companion object {
        val Saver: Saver<ShogiGame, *> = listSaver(
            save = { game ->
                listOf(
                    game.board,
                    game.turn,
                    game.capturedSente,
                    game.capturedGote,
                    game.history,
                    game.mattaCountSente,
                    game.mattaCountGote,
                    game.winner?.name
                )
            },
            restore = { list ->
                ShogiGame().apply {
                    board = list[0] as Map<Position, Piece>
                    turn = list[1] as Player
                    capturedSente = list[2] as List<PieceType>
                    capturedGote = list[3] as List<PieceType>
                    history = list[4] as List<GameState>
                    mattaCountSente = list[5] as Int
                    mattaCountGote = list[6] as Int
                    winner = (list[7] as String?)?.let { Player.valueOf(it) }
                }
            }
        )
    }

    init {
        setupInitialBoard()
    }

    fun saveState() {
        history = history + GameState(board, turn, capturedSente, capturedGote)
    }

    fun canUndo(): Boolean {
        if (history.isEmpty()) return false
        val lastPlayer = history.last().turn // This is actually the player who IS about to move in the last state
        // Wait, if it's Sente's turn now, the last move was made by Gote.
        // If we want to undo Gote's move, we go back 1 state.
        // But the user usually wants to undo THEIR OWN last move.
        // If Sente wants to undo their move, they need to go back 2 states (Gote's move and Sente's move).
        // Let's keep it simple: "Matta" undoes exactly ONE half-move (the immediate previous one).
        // The person whose turn it is now is the one who "calls" Matta? 
        // Usually Matta is "I want to take back the move I just made".
        // So if it's Gote's turn, Sente just moved and wants to take it back.
        val playerWhoJustMoved = turn.opponent()
        return if (playerWhoJustMoved == Player.SENTE) mattaCountSente > 0 else mattaCountGote > 0
    }

    fun undoMove() {
        if (history.isNotEmpty() && winner == null) {
            val playerWhoJustMoved = turn.opponent()
            if (playerWhoJustMoved == Player.SENTE) {
                if (mattaCountSente > 0) mattaCountSente-- else return
            } else {
                if (mattaCountGote > 0) mattaCountGote-- else return
            }

            val lastState = history.last()
            board = lastState.board
            turn = lastState.turn
            capturedSente = lastState.capturedSente
            capturedGote = lastState.capturedGote
            history = history.dropLast(1)
        }
    }

    fun resetGame() {
        board = emptyMap()
        turn = Player.SENTE
        capturedSente = emptyList()
        capturedGote = emptyList()
        history = emptyList()
        mattaCountSente = 3
        mattaCountGote = 3
        winner = null
        setupInitialBoard()
    }

    private fun setupInitialBoard() {
        val initialBoard = mutableMapOf<Position, Piece>()
        
        // Gote pieces (top, rows 0-2)
        val goteRows = listOf(
            listOf(PieceType.LANCE, PieceType.KNIGHT, PieceType.SILVER, PieceType.GOLD, PieceType.KING, PieceType.GOLD, PieceType.SILVER, PieceType.KNIGHT, PieceType.LANCE),
            null,
            List(9) { PieceType.PAWN }
        )
        
        for (r in 0..2) {
            val types = goteRows[r]
            if (r == 1) {
                initialBoard[Position(1, 1)] = Piece(PieceType.ROOK, Player.GOTE)
                initialBoard[Position(1, 7)] = Piece(PieceType.BISHOP, Player.GOTE)
            } else if (types != null) {
                for (c in 0..8) {
                    initialBoard[Position(r, c)] = Piece(types[c], Player.GOTE)
                }
            }
        }
        // Actually gote bishop/rook are different positions in standard shogi but let's just place them.
        // Wait, gote rook is at (1, 1) and bishop at (1, 7) from gote's perspective.
        // In 0-indexed board:
        // Gote Rook: (1, 1), Bishop: (1, 7)
        // Sente Rook: (7, 7), Bishop: (7, 1)

        // Sente pieces (bottom, rows 6-8)
        for (i in 0..8) initialBoard[Position(6, i)] = Piece(PieceType.PAWN, Player.SENTE)
        initialBoard[Position(7, 1)] = Piece(PieceType.BISHOP, Player.SENTE)
        initialBoard[Position(7, 7)] = Piece(PieceType.ROOK, Player.SENTE)
        
        val senteLastRow = listOf(PieceType.LANCE, PieceType.KNIGHT, PieceType.SILVER, PieceType.GOLD, PieceType.KING, PieceType.GOLD, PieceType.SILVER, PieceType.KNIGHT, PieceType.LANCE)
        for (c in 0..8) {
            initialBoard[Position(8, c)] = Piece(senteLastRow[c], Player.SENTE)
        }

        board = initialBoard
    }

    fun getValidMoves(pos: Position): List<Position> {
        val piece = board[pos] ?: return emptyList()
        val moves = mutableListOf<Position>()
        val directions = getDirections(piece.type, piece.owner)

        for (dir in directions) {
            var currRow = pos.row + dir.first
            var currCol = pos.col + dir.second
            
            while (currRow in 0..8 && currCol in 0..8) {
                val targetPos = Position(currRow, currCol)
                val targetPiece = board[targetPos]
                
                if (targetPiece == null) {
                    moves.add(targetPos)
                } else {
                    if (targetPiece.owner != piece.owner) {
                        moves.add(targetPos)
                    }
                    break
                }
                
                if (!isRanged(piece.type, dir)) break
                
                currRow += dir.first
                currCol += dir.second
            }
        }
        return moves
    }

    private fun getDirections(type: PieceType, owner: Player): List<Pair<Int, Int>> {
        val forward = if (owner == Player.SENTE) -1 else 1
        return when (type) {
            PieceType.PAWN -> listOf(forward to 0)
            PieceType.LANCE -> listOf(forward to 0)
            PieceType.KNIGHT -> listOf(forward * 2 to -1, forward * 2 to 1)
            PieceType.SILVER -> listOf(forward to 0, forward to -1, forward to 1, -forward to -1, -forward to 1)
            PieceType.GOLD, PieceType.PROMOTED_PAWN, PieceType.PROMOTED_LANCE, PieceType.PROMOTED_KNIGHT, PieceType.PROMOTED_SILVER -> 
                listOf(forward to 0, forward to -1, forward to 1, 0 to -1, 0 to 1, -forward to 0)
            PieceType.BISHOP -> listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
            PieceType.ROOK -> listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
            PieceType.KING -> listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
            PieceType.PROMOTED_BISHOP -> listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1, 1 to 0, -1 to 0, 0 to 1, 0 to -1)
            PieceType.PROMOTED_ROOK -> listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
        }
    }

    private fun isRanged(type: PieceType, dir: Pair<Int, Int>): Boolean {
        return when (type) {
            PieceType.LANCE -> true
            PieceType.BISHOP, PieceType.PROMOTED_BISHOP -> dir.first != 0 && dir.second != 0
            PieceType.ROOK, PieceType.PROMOTED_ROOK -> dir.first == 0 || dir.second == 0
            else -> false
        }
    }

    fun getValidDrops(type: PieceType, owner: Player): List<Position> {
        val drops = mutableListOf<Position>()
        for (r in 0..8) {
            for (c in 0..8) {
                val pos = Position(r, c)
                if (board[pos] == null) {
                    // Check basic drop rules (Nifu, dead-end)
                    if (isLegalDrop(type, owner, pos)) {
                        drops.add(pos)
                    }
                }
            }
        }
        return drops
    }

    private fun isLegalDrop(type: PieceType, owner: Player, pos: Position): Boolean {
        // Dead end check
        if (type == PieceType.PAWN || type == PieceType.LANCE) {
            if (owner == Player.SENTE && pos.row == 0) return false
            if (owner == Player.GOTE && pos.row == 8) return false
        }
        if (type == PieceType.KNIGHT) {
            if (owner == Player.SENTE && pos.row <= 1) return false
            if (owner == Player.GOTE && pos.row >= 7) return false
        }
        // Nifu (Two pawns in one column)
        if (type == PieceType.PAWN) {
            for (r in 0..8) {
                val p = board[Position(r, pos.col)]
                if (p != null && p.owner == owner && p.type == PieceType.PAWN) return false
            }
        }
        return true
    }

    fun isCheck(player: Player): Boolean {
        val kingPos = board.entries.find { it.value.type == PieceType.KING && it.value.owner == player }?.key ?: return false
        val opponent = player.opponent()
        
        for (entry in board) {
            if (entry.value.owner == opponent) {
                val moves = getValidMoves(entry.key)
                if (kingPos in moves) return true
            }
        }
        return false
    }

    fun canPromote(piece: Piece, from: Position, to: Position): Boolean {
        if (piece.type.name.startsWith("PROMOTED") || piece.type == PieceType.GOLD || piece.type == PieceType.KING) return false
        
        return if (piece.owner == Player.SENTE) {
            from.row <= 2 || to.row <= 2
        } else {
            from.row >= 6 || to.row >= 6
        }
    }
    
    fun mustPromote(piece: Piece, to: Position): Boolean {
        return when (piece.type) {
            PieceType.PAWN, PieceType.LANCE -> (piece.owner == Player.SENTE && to.row == 0) || (piece.owner == Player.GOTE && to.row == 8)
            PieceType.KNIGHT -> (piece.owner == Player.SENTE && to.row <= 1) || (piece.owner == Player.GOTE && to.row >= 7)
            else -> false
        }
    }
}
