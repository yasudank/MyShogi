package com.example.myshogi

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GameMode { HUMAN_VS_HUMAN, HUMAN_VS_COMPUTER }

sealed class Selection : java.io.Serializable {
    data class Board(val pos: Position) : Selection()
    data class Captured(val type: PieceType, val owner: Player) : Selection()
}

data class PromotionRequest(
    val from: Position,
    val to: Position,
    val piece: Piece
) : java.io.Serializable

val ShogiPieceShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width * 0.9f, size.height * 0.25f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    lineTo(size.width * 0.1f, size.height * 0.25f)
    close()
}

@Composable
fun ShogiScreen() {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.koma_oto) }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    val playSound = {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.prepare()
        }
        mediaPlayer.start()
    }

    val game = rememberSaveable(saver = ShogiGame.Saver) { ShogiGame() }
    val selection = rememberSaveable { mutableStateOf<Selection?>(null) }
    val promotionRequest = rememberSaveable { mutableStateOf<PromotionRequest?>(null) }
    val showResetDialog = rememberSaveable { mutableStateOf(false) }
    val showCheckDialog = remember { mutableStateOf<Player?>(null) }
    val gameMode = rememberSaveable { mutableStateOf(GameMode.HUMAN_VS_HUMAN) }
    val isComputerThinking = remember { mutableStateOf(false) }
    val ai = remember { ShogiAI(aiPlayer = Player.GOTE, maxDepth = 3) }

    // 王手チェックとAIターン処理
    LaunchedEffect(game.turn, game.board) {
        if (game.winner == null && game.isCheck(game.turn)) {
            showCheckDialog.value = game.turn
            delay(2000)
            showCheckDialog.value = null
        } else {
            showCheckDialog.value = null
        }

        if (gameMode.value == GameMode.HUMAN_VS_COMPUTER
            && game.turn == Player.GOTE
            && game.winner == null
        ) {
            isComputerThinking.value = true
            delay(300)
            val state = GameState(game.board, game.turn, game.capturedSente, game.capturedGote)
            val move = withContext(Dispatchers.Default) { ai.bestMove(state) }
            if (move != null && game.winner == null) {
                game.saveState()
                val capturedKing = when (move) {
                    is Move.BoardMove -> state.board[move.to]?.type == PieceType.KING
                    else -> false
                }
                val next = applyMove(state, move)
                game.board = next.board
                game.capturedSente = next.capturedSente
                game.capturedGote = next.capturedGote
                if (capturedKing) game.winner = Player.GOTE
                game.turn = next.turn
                playSound()
            }
            isComputerThinking.value = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5DEB3),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Gote Info & Captured
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val isGoteTurn = game.turn == Player.GOTE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .rotate(180f)
                        .widthIn(min = 140.dp)
                        .height(40.dp)
                        .background(
                            Color.Black.copy(alpha = if (isGoteTurn) 0.2f else 0.05f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isGoteTurn) Color.Red else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "後手 (Gote)",
                        fontWeight = if (isGoteTurn) FontWeight.ExtraBold else FontWeight.Normal,
                        fontFamily = FontFamily.Serif
                    )
                    
                    if (gameMode.value == GameMode.HUMAN_VS_HUMAN
                        && game.turn == Player.SENTE && game.history.isNotEmpty() && game.mattaCountGote > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { game.undoMove() },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("待った (${game.mattaCountGote})", fontSize = 12.sp, fontFamily = FontFamily.Serif)
                        }
                    } else if (gameMode.value == GameMode.HUMAN_VS_HUMAN && game.mattaCountGote < 3) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("残:${game.mattaCountGote}", fontSize = 10.sp, color = Color.Gray)
                    } else if (gameMode.value == GameMode.HUMAN_VS_COMPUTER && isComputerThinking.value) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("考え中…", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Serif)
                    }
                }
                CapturedPiecesView(game.capturedGote, Player.GOTE, selection.value) { 
                    if (game.winner == null) selection.value = it 
                }
            }

            // Board
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .border(2.dp, Color.Black)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mokume),
                    contentDescription = "Board Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                val boardSize = maxWidth
                val cellSize = boardSize / 9

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 1.dp.toPx()
                    val boardWidth = size.width
                    val boardHeight = size.height
                    for (i in 0..9) {
                        val offset = i * (boardWidth / 9f)
                        drawLine(Color.Black, Offset(offset, 0f), Offset(offset, boardHeight), strokeWidth = stroke)
                        drawLine(Color.Black, Offset(0f, offset), Offset(boardWidth, offset), strokeWidth = stroke)
                    }
                    val dotRadius = 4.dp.toPx()
                    val dotPositions = listOf(3f, 6f)
                    for (r in dotPositions) {
                        for (c in dotPositions) {
                            drawCircle(Color.Black, dotRadius, Offset(c * (boardWidth / 9f), r * (boardHeight / 9f)))
                        }
                    }
                }

                val currentSelection = selection.value
                val validMoves = remember(currentSelection, game.board, game.turn, game.winner) {
                    if (game.winner != null) {
                        emptyList()
                    } else {
                        when (currentSelection) {
                            is Selection.Board -> game.getValidMoves(currentSelection.pos)
                            is Selection.Captured -> game.getValidDrops(currentSelection.type, currentSelection.owner)
                            else -> emptyList()
                        }
                    }
                }

                for (row in 0..8) {
                    for (col in 0..8) {
                        val pos = Position(row, col)
                        val piece = game.board[pos]
                        val isSelected = currentSelection is Selection.Board && (currentSelection.pos == pos)
                        val isValidMove = pos in validMoves
                        
                        Box(
                            modifier = Modifier
                                .offset(x = cellSize * col, y = cellSize * row)
                                .size(cellSize)
                                .clickable(enabled = game.winner == null && !isComputerThinking.value) {
                                    handleBoardClick(pos, game, selection.value, validMoves, { selection.value = it }, { promotionRequest.value = it }, playSound)
                                }
                                .background(
                                    when {
                                        isSelected -> Color.Yellow.copy(alpha = 0.4f)
                                        isValidMove -> Color.Green.copy(alpha = 0.3f)
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (piece != null) {
                                PieceView(piece)
                            }
                        }
                    }
                }
            }

            // Sente Info & Captured
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CapturedPiecesView(game.capturedSente, Player.SENTE, selection.value) { 
                    if (game.winner == null) selection.value = it 
                }
                val isSenteTurn = game.turn == Player.SENTE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(min = 140.dp)
                        .height(40.dp)
                        .background(
                            Color.Black.copy(alpha = if (isSenteTurn) 0.2f else 0.05f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isSenteTurn) Color.Red else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "先手 (Sente)",
                        fontWeight = if (isSenteTurn) FontWeight.ExtraBold else FontWeight.Normal,
                        fontFamily = FontFamily.Serif
                    )

                    if (game.turn == Player.GOTE && game.history.isNotEmpty() && game.mattaCountSente > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { game.undoMove() },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("待った (${game.mattaCountSente})", fontSize = 12.sp, fontFamily = FontFamily.Serif)
                        }
                    } else if (game.mattaCountSente < 3) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("残:${game.mattaCountSente}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            gameMode.value = GameMode.HUMAN_VS_HUMAN
                            game.resetGame()
                            selection.value = null
                            promotionRequest.value = null
                            isComputerThinking.value = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        border = if (gameMode.value == GameMode.HUMAN_VS_HUMAN)
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null
                    ) {
                        Text("人対人", fontSize = 14.sp, fontFamily = FontFamily.Serif)
                    }
                    OutlinedButton(
                        onClick = {
                            gameMode.value = GameMode.HUMAN_VS_COMPUTER
                            game.resetGame()
                            selection.value = null
                            promotionRequest.value = null
                            isComputerThinking.value = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        border = if (gameMode.value == GameMode.HUMAN_VS_COMPUTER)
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null
                    ) {
                        Text("対コンピュータ", fontSize = 14.sp, fontFamily = FontFamily.Serif)
                    }
                    OutlinedButton(
                        onClick = { showResetDialog.value = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("最初からやり直す", fontSize = 14.sp, fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
    }

    if (showResetDialog.value) {
        ResetConfirmationDialog(
            onConfirm = {
                game.resetGame()
                selection.value = null
                promotionRequest.value = null
                isComputerThinking.value = false
                showResetDialog.value = false
            },
            onDismiss = { showResetDialog.value = false }
        )
    }

    promotionRequest.value?.let { request ->
        PromotionDialog(
            piece = request.piece,
            onConfirm = { promote ->
                executeMove(request.from, request.to, game, promote)
                playSound()
                promotionRequest.value = null
            },
            onDismiss = {
                promotionRequest.value = null
            }
        )
    }

    showCheckDialog.value?.let { player ->
        CheckDialog(player) { showCheckDialog.value = null }
    }

    game.winner?.let { winner ->
        GameOverDialog(winner) { game.resetGame() }
    }
}

@Composable
fun CheckDialog(player: Player, onDismiss: () -> Unit) {
    val rotation = if (player == Player.GOTE) 180f else 0f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.rotate(rotation),
            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Text(
                text = "王手！",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

@Composable
fun GameOverDialog(winner: Player, onReset: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("対局終了", fontFamily = FontFamily.Serif) },
        text = {
            Text(
                text = "${if (winner == Player.SENTE) "先手" else "後手"} の勝ちです！",
                fontFamily = FontFamily.Serif
            )
        },
        confirmButton = {
            Button(onClick = onReset) {
                Text("新しく対局を始める", fontFamily = FontFamily.Serif)
            }
        }
    )
}

@Composable
fun ResetConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("対局のリセット", fontFamily = FontFamily.Serif) },
        text = { Text("現在の対局を破棄して、最初からやり直しますか？", fontFamily = FontFamily.Serif) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("やり直す", fontFamily = FontFamily.Serif)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", fontFamily = FontFamily.Serif)
            }
        }
    )
}

@Composable
fun PromotionDialog(piece: Piece, onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val rotation = if (piece.owner == Player.GOTE) 180f else 0f
    AlertDialog(
        modifier = Modifier.rotate(rotation),
        onDismissRequest = onDismiss,
        title = { Text("成りますか？", fontFamily = FontFamily.Serif) },
        text = { Text("${piece.type.label} を成りますか？", fontFamily = FontFamily.Serif) },
        confirmButton = {
            Button(onClick = { onConfirm(true) }) { Text("成る", fontFamily = FontFamily.Serif) }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(false) }) { Text("成らない", fontFamily = FontFamily.Serif) }
        }
    )
}

@Composable
fun PieceView(piece: Piece) {
    val resId = when (piece.owner) {
        Player.SENTE -> when (piece.type) {
            PieceType.PAWN -> R.drawable.black_pawn
            PieceType.LANCE -> R.drawable.black_lance
            PieceType.KNIGHT -> R.drawable.black_knight
            PieceType.SILVER -> R.drawable.black_silver
            PieceType.GOLD -> R.drawable.black_gold
            PieceType.BISHOP -> R.drawable.black_bishop
            PieceType.ROOK -> R.drawable.black_rook
            PieceType.KING -> R.drawable.black_king
            PieceType.PROMOTED_PAWN -> R.drawable.black_prom_pawn
            PieceType.PROMOTED_LANCE -> R.drawable.black_prom_lance
            PieceType.PROMOTED_KNIGHT -> R.drawable.black_prom_knight
            PieceType.PROMOTED_SILVER -> R.drawable.black_prom_silver
            PieceType.PROMOTED_BISHOP -> R.drawable.black_horse
            PieceType.PROMOTED_ROOK -> R.drawable.black_dragon
        }
        Player.GOTE -> when (piece.type) {
            PieceType.PAWN -> R.drawable.white_pawn
            PieceType.LANCE -> R.drawable.white_lance
            PieceType.KNIGHT -> R.drawable.white_knight
            PieceType.SILVER -> R.drawable.white_silver
            PieceType.GOLD -> R.drawable.white_gold
            PieceType.BISHOP -> R.drawable.white_bishop
            PieceType.ROOK -> R.drawable.white_rook
            PieceType.KING -> R.drawable.white_king2
            PieceType.PROMOTED_PAWN -> R.drawable.white_prom_pawn
            PieceType.PROMOTED_LANCE -> R.drawable.white_prom_lance
            PieceType.PROMOTED_KNIGHT -> R.drawable.white_prom_knight
            PieceType.PROMOTED_SILVER -> R.drawable.white_prom_silver
            PieceType.PROMOTED_BISHOP -> R.drawable.white_horse
            PieceType.PROMOTED_ROOK -> R.drawable.white_dragon
        }
    }

    Image(
        painter = painterResource(id = resId),
        contentDescription = piece.type.label,
        modifier = Modifier
            .padding(1.dp)
            .fillMaxSize()
    )
}

@Composable
fun CapturedPiecesView(
    captured: List<PieceType>,
    owner: Player,
    selection: Selection?,
    onSelectionChange: (Selection?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(4.dp)
            .background(Color.Black.copy(alpha = 0.1f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        captured.forEach { type ->
            val isSelected = selection is Selection.Captured && selection.type == type && selection.owner == owner
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(48.dp)
                    .clickable {
                        if (isSelected) {
                            onSelectionChange(null)
                        } else {
                            onSelectionChange(Selection.Captured(type, owner))
                        }
                    }
                    .background(
                        if (isSelected) Color.Yellow.copy(alpha = 0.4f) else Color.Transparent,
                        shape = ShogiPieceShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                PieceView(Piece(type, owner))
            }
        }
    }
}

fun handleBoardClick(
    pos: Position,
    game: ShogiGame,
    selection: Selection?,
    validMoves: List<Position>,
    onSelectionChange: (Selection?) -> Unit,
    onPromotionRequest: (PromotionRequest?) -> Unit,
    onMoveExecuted: () -> Unit
) {
    if (game.winner != null) return
    when (selection) {
        null -> {
            val piece = game.board[pos]
            if (piece != null && piece.owner == game.turn) {
                onSelectionChange(Selection.Board(pos))
            }
        }
        is Selection.Board -> {
            if (selection.pos == pos) {
                onSelectionChange(null)
            } else if (pos in validMoves) {
                val piece = game.board[selection.pos]!!
                if (game.canPromote(piece, selection.pos, pos)) {
                    if (game.mustPromote(piece, pos)) {
                        executeMove(selection.pos, pos, game, true)
                        onMoveExecuted()
                        onSelectionChange(null)
                    } else {
                        onPromotionRequest(PromotionRequest(selection.pos, pos, piece))
                        onSelectionChange(null)
                    }
                } else {
                    executeMove(selection.pos, pos, game, false)
                    onMoveExecuted()
                    onSelectionChange(null)
                }
            } else {
                val targetPiece = game.board[pos]
                if (targetPiece?.owner == game.turn) {
                    onSelectionChange(Selection.Board(pos))
                }
            }
        }
        is Selection.Captured -> {
            if (selection.owner == game.turn && pos in validMoves) {
                game.saveState()
                // Drop
                val newBoard = game.board.toMutableMap()
                newBoard[pos] = Piece(selection.type, game.turn)
                if (game.turn == Player.SENTE) {
                    game.capturedSente = game.capturedSente.toMutableList().apply { remove(selection.type) }
                } else {
                    game.capturedGote = game.capturedGote.toMutableList().apply { remove(selection.type) }
                }
                game.board = newBoard
                game.turn = game.turn.opponent()
                onMoveExecuted()
                onSelectionChange(null)
            }
        }
    }
}

fun executeMove(from: Position, to: Position, game: ShogiGame, promote: Boolean) {
    game.saveState()
    val selectedPiece = game.board[from]!!
    val targetPiece = game.board[to]
    
    val newBoard = game.board.toMutableMap()
    if (targetPiece != null) {
        if (targetPiece.type == PieceType.KING) {
            game.winner = game.turn
        }
        if (game.turn == Player.SENTE) {
            game.capturedSente += demote(targetPiece.type)
        } else {
            game.capturedGote += demote(targetPiece.type)
        }
    }
    newBoard.remove(from)
    
    val finalPiece = if (promote) {
        Piece(promote(selectedPiece.type), selectedPiece.owner)
    } else {
        selectedPiece
    }

    newBoard[to] = finalPiece
    game.board = newBoard
    game.turn = game.turn.opponent()
}


