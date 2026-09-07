package com.harshnagar.ludo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class LudoView(context: Context) : View(context) {

    private enum class PlayerColor(
        val main: Int,
        val dark: Int,
        val start: Int,
        val title: String
    ) {
        GREEN(Color.rgb(20, 156, 91), Color.rgb(0, 105, 62), 39, "You"),
        YELLOW(Color.rgb(250, 205, 20), Color.rgb(184, 137, 0), 0, "Computer 2"),
        BLUE(Color.rgb(25, 139, 224), Color.rgb(0, 83, 160), 13, "Computer 3"),
        RED(Color.rgb(239, 48, 48), Color.rgb(175, 15, 15), 26, "Computer 4")
    }

    private data class Token(var progress: Int = -1)
    private data class Player(
        val color: PlayerColor,
        val tokens: MutableList<Token> = MutableList(4) { Token() }
    )

    private val players = PlayerColor.values().map { Player(it) }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans", Typeface.BOLD)
    }

    private var turn = 0
    private var dice = 0
    private var rolled = false
    private var gameOver = false
    private var winner = 0
    private var message = "Tap ROLL DICE to start"

    private var boardLeft = 0f
    private var boardTop = 0f
    private var cell = 0f
    private var boardSize = 0f

    // Standard 52-cell outer track in row/column coordinates of a 15x15 board.
    private val route = arrayOf(
        intArrayOf(6, 1), intArrayOf(6, 2), intArrayOf(6, 3), intArrayOf(6, 4), intArrayOf(6, 5),
        intArrayOf(5, 6), intArrayOf(4, 6), intArrayOf(3, 6), intArrayOf(2, 6), intArrayOf(1, 6), intArrayOf(0, 6),
        intArrayOf(0, 7), intArrayOf(0, 8),
        intArrayOf(1, 8), intArrayOf(2, 8), intArrayOf(3, 8), intArrayOf(4, 8), intArrayOf(5, 8),
        intArrayOf(6, 9), intArrayOf(6, 10), intArrayOf(6, 11), intArrayOf(6, 12), intArrayOf(6, 13), intArrayOf(6, 14),
        intArrayOf(7, 14), intArrayOf(8, 14),
        intArrayOf(8, 13), intArrayOf(8, 12), intArrayOf(8, 11), intArrayOf(8, 10), intArrayOf(8, 9),
        intArrayOf(9, 8), intArrayOf(10, 8), intArrayOf(11, 8), intArrayOf(12, 8), intArrayOf(13, 8), intArrayOf(14, 8),
        intArrayOf(14, 7), intArrayOf(14, 6),
        intArrayOf(13, 6), intArrayOf(12, 6), intArrayOf(11, 6), intArrayOf(10, 6), intArrayOf(9, 6),
        intArrayOf(8, 5), intArrayOf(8, 4), intArrayOf(8, 3), intArrayOf(8, 2), intArrayOf(8, 1), intArrayOf(8, 0),
        intArrayOf(7, 0), intArrayOf(6, 0)
    )

    private val safeSquares = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        reset()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        boardSize = min(w * 0.94f, h * 0.60f)
        boardLeft = (w - boardSize) / 2f
        boardTop = h * 0.13f
        cell = boardSize / 15f

        drawBackground(canvas, w, h)
        drawPlayerPanels(canvas, w)
        drawBoard(canvas)
        drawControls(canvas, w, h)
        if (gameOver) drawGameOver(canvas, w, h)
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            Color.rgb(8, 66, 125),
            Color.rgb(2, 24, 55),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = w * 0.045f
        labelPaint.color = Color.WHITE
        canvas.drawText("HARSH NAGAR  •  LUDO", w / 2f, h * 0.055f, labelPaint)
    }

    private fun drawPlayerPanels(canvas: Canvas, w: Float) {
        drawPlayerPanel(canvas, 16f, boardTop - 66f, w * 0.43f, players[1])
        drawPlayerPanel(canvas, w * 0.57f, boardTop - 66f, w - 16f, players[2])
    }

    private fun drawPlayerPanel(canvas: Canvas, left: Float, top: Float, right: Float, player: Player) {
        paint.color = player.color.main
        canvas.drawRoundRect(left, top, right, top + 52f, 16f, 16f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(left + 24f, top + 26f, 13f, paint)
        paint.color = player.color.main
        canvas.drawCircle(left + 24f, top + 26f, 8f, paint)

        labelPaint.textAlign = Paint.Align.LEFT
        labelPaint.textSize = 16f
        labelPaint.color = Color.WHITE
        canvas.drawText(player.color.title, left + 46f, top + 32f, labelPaint)
    }

    private fun drawBoard(canvas: Canvas) {
        paint.color = Color.WHITE
        canvas.drawRect(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize, paint)

        drawHome(canvas, 0, 0, PlayerColor.YELLOW)
        drawHome(canvas, 9, 0, PlayerColor.BLUE)
        drawHome(canvas, 0, 9, PlayerColor.GREEN)
        drawHome(canvas, 9, 9, PlayerColor.RED)

        drawTrackCells(canvas)
        drawHomeLanes(canvas)
        drawCenter(canvas)
        drawSafeSquares(canvas)
        drawTokens(canvas)
    }

    private fun drawHome(canvas: Canvas, col: Int, row: Int, color: PlayerColor) {
        paint.color = color.main
        canvas.drawRect(
            boardLeft + col * cell,
            boardTop + row * cell,
            boardLeft + (col + 6) * cell,
            boardTop + (row + 6) * cell,
            paint
        )

        paint.color = Color.WHITE
        canvas.drawRoundRect(
            boardLeft + (col + 1) * cell,
            boardTop + (row + 1) * cell,
            boardLeft + (col + 5) * cell,
            boardTop + (row + 5) * cell,
            cell * 0.28f,
            cell * 0.28f,
            paint
        )

        val spots = arrayOf(
            1.7f to 1.7f,
            4.3f to 1.7f,
            1.7f to 4.3f,
            4.3f to 4.3f
        )
        paint.color = color.main
        for ((x, y) in spots) {
            canvas.drawCircle(
                boardLeft + (col + x) * cell,
                boardTop + (row + y) * cell,
                cell * 0.34f,
                paint
            )
        }
    }

    private fun drawTrackCells(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(180, 180, 180)
        for (r in 0..14) {
            for (col in 0..14) {
                val x = boardLeft + col * cell
                val y = boardTop + r * cell
                canvas.drawRect(x, y, x + cell, y + cell, paint)
            }
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawHomeLanes(canvas: Canvas) {
        drawLane(canvas, PlayerColor.YELLOW, 6, 1, 1, 5)
        drawLane(canvas, PlayerColor.BLUE, 9, 6, 5, 1)
        drawLane(canvas, PlayerColor.RED, 8, 9, 1, 5)
        drawLane(canvas, PlayerColor.GREEN, 1, 8, 5, 1)
    }

    private fun drawLane(canvas: Canvas, color: PlayerColor, col: Int, row: Int, widthCells: Int, heightCells: Int) {
        paint.color = color.main
        canvas.drawRect(
            boardLeft + col * cell,
            boardTop + row * cell,
            boardLeft + (col + widthCells) * cell,
            boardTop + (row + heightCells) * cell,
            paint
        )
        paint.color = Color.argb(55, 255, 255, 255)
        for (r in 0 until heightCells) {
            for (c in 0 until widthCells) {
                val x = boardLeft + (col + c) * cell
                val y = boardTop + (row + r) * cell
                canvas.drawRect(x, y, x + cell, y + cell, paint)
            }
        }
    }

    private fun drawCenter(canvas: Canvas) {
        val cx = boardLeft + 7.5f * cell
        val cy = boardTop + 7.5f * cell
        triangle(canvas, cx, cy, 6f, 6f, 9f, 6f, PlayerColor.YELLOW.main)
        triangle(canvas, cx, cy, 9f, 6f, 9f, 9f, PlayerColor.RED.main)
        triangle(canvas, cx, cy, 9f, 9f, 6f, 9f, PlayerColor.GREEN.main)
        triangle(canvas, cx, cy, 6f, 9f, 6f, 6f, PlayerColor.BLUE.main)
    }

    private fun triangle(canvas: Canvas, cx: Float, cy: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val path = Path()
        path.moveTo(cx, cy)
        path.lineTo(boardLeft + x1 * cell, boardTop + y1 * cell)
        path.lineTo(boardLeft + x2 * cell, boardTop + y2 * cell)
        path.close()
        paint.color = color
        canvas.drawPath(path, paint)
    }

    private fun drawSafeSquares(canvas: Canvas) {
        for (index in safeSquares) {
            val q = route[index]
            drawStar(
                canvas,
                boardLeft + (q[1] + 0.5f) * cell,
                boardTop + (q[0] + 0.5f) * cell,
                cell * 0.22f
            )
        }
    }

    private fun drawStar(canvas: Canvas, x: Float, y: Float, radius: Float) {
        val path = Path()
        for (i in 0..9) {
            val angle = -Math.PI / 2 + i * Math.PI / 5
            val r = if (i % 2 == 0) radius else radius * 0.42f
            val px = x + cos(angle).toFloat() * r
            val py = y + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        paint.color = Color.rgb(115, 115, 115)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawTokens(canvas: Canvas) {
        for (player in players) {
            for (index in player.tokens.indices) {
                val point = tokenPoint(player.color, player.tokens[index].progress, index) ?: continue
                drawToken(canvas, point.first, point.second, player.color, index)
            }
        }
    }

    private fun tokenPoint(color: PlayerColor, progress: Int, index: Int): Pair<Float, Float>? {
        if (progress < 0) {
            val base = when (color) {
                PlayerColor.YELLOW -> 0 to 0
                PlayerColor.BLUE -> 9 to 0
                PlayerColor.GREEN -> 0 to 9
                PlayerColor.RED -> 9 to 9
            }
            val spots = arrayOf(
                1.7f to 1.7f,
                4.3f to 1.7f,
                1.7f to 4.3f,
                4.3f to 4.3f
            )
            val spot = spots[index]
            return boardLeft + (base.first + spot.first) * cell to
                boardTop + (base.second + spot.second) * cell
        }

        if (progress <= 51) {
            val global = (color.start + progress) % 52
            val q = route[global]
            return boardLeft + (q[1] + 0.5f) * cell to
                boardTop + (q[0] + 0.5f) * cell
        }

        val lane = progress - 52
        return when (color) {
            PlayerColor.YELLOW -> boardLeft + 6.5f * cell to boardTop + (5.5f - lane) * cell
            PlayerColor.BLUE -> boardLeft + (8.5f + lane) * cell to boardTop + 6.5f * cell
            PlayerColor.RED -> boardLeft + 8.5f * cell to boardTop + (8.5f + lane) * cell
            PlayerColor.GREEN -> boardLeft + (5.5f - lane) * cell to boardTop + 8.5f * cell
        }
    }

    private fun drawToken(canvas: Canvas, x: Float, y: Float, color: PlayerColor, index: Int) {
        paint.color = Color.argb(70, 0, 0, 0)
        canvas.drawCircle(x + 2f, y + 4f, cell * 0.34f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(x, y, cell * 0.31f, paint)
        paint.color = color.main
        canvas.drawCircle(x, y, cell * 0.245f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(x, y - cell * 0.08f, cell * 0.07f, paint)

        if (turn == 0 && color == PlayerColor.GREEN && rolled && canMove(index, dice)) {
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawCircle(x, y, cell * 0.39f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawControls(canvas: Canvas, w: Float, h: Float) {
        val y = h * 0.77f
        val panelH = 76f

        paint.color = PlayerColor.GREEN.main
        canvas.drawRoundRect(16f, y, w * 0.42f, y + panelH, 18f, 18f, paint)
        labelPaint.textAlign = Paint.Align.LEFT
        labelPaint.textSize = 18f
        labelPaint.color = Color.WHITE
        canvas.drawText("YOU", 32f, y + 28f, labelPaint)
        labelPaint.textSize = 13f
        canvas.drawText("Turn: ${players[turn].color.title}", 32f, y + 53f, labelPaint)

        paint.color = Color.WHITE
        canvas.drawRoundRect(w * 0.45f, y, w * 0.68f, y + panelH, 18f, 18f, paint)
        drawDice(canvas, w * 0.565f, y + panelH / 2f, dice)

        paint.color = Color.rgb(22, 112, 203)
        canvas.drawRoundRect(w * 0.71f, y, w - 16f, y + panelH, 18f, 18f, paint)
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = 15f
        labelPaint.color = Color.WHITE
        canvas.drawText(if (rolled) "SELECT TOKEN" else "ROLL DICE", w * 0.855f, y + 45f, labelPaint)

        if (!gameOver) {
            labelPaint.textSize = 13f
            labelPaint.color = Color.WHITE
            canvas.drawText(message, w / 2f, y + 101f, labelPaint)
        }
    }

    private fun drawDice(canvas: Canvas, x: Float, y: Float, value: Int) {
        paint.color = Color.WHITE
        canvas.drawRoundRect(x - 25f, y - 25f, x + 25f, y + 25f, 10f, 10f, paint)
        if (value == 0) return

        paint.color = Color.DKGRAY
        val d = 12f
        val dots = when (value) {
            1 -> arrayOf(0 to 0)
            2 -> arrayOf(-1 to -1, 1 to 1)
            3 -> arrayOf(-1 to -1, 0 to 0, 1 to 1)
            4 -> arrayOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)
            5 -> arrayOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1)
            else -> arrayOf(-1 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 1 to 1)
        }
        for ((dx, dy) in dots) {
            canvas.drawCircle(x + dx * d, y + dy * d, 4f, paint)
        }
    }

    private fun drawGameOver(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.argb(210, 0, 0, 0)
        canvas.drawRect(0f, 0f, w, h, paint)
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = 34f
        labelPaint.color = Color.WHITE
        canvas.drawText("${players[winner].color.title.uppercase()} WINS!", w / 2f, h * 0.45f, labelPaint)
        labelPaint.textSize = 18f
        canvas.drawText("Tap anywhere to play again", w / 2f, h * 0.51f, labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true

        if (gameOver) {
            reset()
            invalidate()
            return true
        }

        if (turn != 0) return true

        val x = event.x
        val y = event.y
        val diceTop = height * 0.77f

        if (!rolled && x in width * 0.45f..width * 0.68f && y in diceTop..(diceTop + 90f)) {
            rollDice()
            return true
        }

        if (rolled) {
            for (index in 0..3) {
                val token = players[0].tokens[index]
                val point = tokenPoint(PlayerColor.GREEN, token.progress, index) ?: continue
                val dx = x - point.first
                val dy = y - point.second
                if (dx * dx + dy * dy <= cell * cell * 0.75f && canMove(index, dice)) {
                    moveToken(0, index)
                    return true
                }
            }
        }
        return true
    }

    private fun rollDice() {
        if (gameOver || rolled || turn != 0) return
        dice = Random.nextInt(1, 7)
        rolled = true
        val legalMoves = legalMovesForCurrentPlayer()

        if (legalMoves.isEmpty()) {
            message = "No legal move"
            postDelayed({
                if (!gameOver && turn == 0 && rolled) finishTurn()
            }, 650L)
        } else {
            message = if (dice == 6) "Six! Choose a token" else "Choose a highlighted token"
        }
        invalidate()
    }

    private fun legalMovesForCurrentPlayer(): List<Int> =
        (0..3).filter { canMove(it, dice) }

    private fun canMove(tokenIndex: Int, die: Int): Boolean {
        if (die !in 1..6) return false
        val token = players[turn].tokens[tokenIndex]

        if (token.progress < 0) return die == 6
        if (token.progress + die > 56) return false
        return !blockedByOpponent(players[turn].color, token.progress, die)
    }

    private fun blockedByOpponent(color: PlayerColor, progress: Int, die: Int): Boolean {
        // Block rules apply only on the outer track.
        if (progress < 0 || progress > 51 || progress + die > 51) return false

        val target = (color.start + progress + die) % 52
        for (other in players) {
            if (other.color == color) continue
            val count = other.tokens.count {
                it.progress in 0..51 &&
                    (other.color.start + it.progress) % 52 == target
            }
            if (count >= 2) return true
        }
        return false
    }

    private fun moveToken(playerIndex: Int, tokenIndex: Int) {
        if (gameOver) return
        val player = players[playerIndex]
        val die = dice
        if (!canMove(tokenIndex, die)) return

        val token = player.tokens[tokenIndex]
        token.progress = if (token.progress < 0) 0 else token.progress + die

        captureOpponents(player)

        if (player.tokens.all { it.progress == 56 }) {
            winner = playerIndex
            gameOver = true
            rolled = false
            dice = 0
            message = "Game over"
            invalidate()
            return
        }

        if (die == 6) {
            rolled = false
            dice = 0
            message = "Six! Roll again"
            invalidate()
            if (playerIndex != 0) {
                postDelayed({ computerTurn() }, 500L)
            }
            return
        }

        finishTurn()
    }

    private fun captureOpponents(player: Player) {
        val movingToken = player.tokens.firstOrNull { it.progress in 0..51 } ?: return
        val target = (player.color.start + movingToken.progress) % 52

        // Capture every opposing token on the landing square, unless it is safe.
        if (target in safeSquares) return

        for (other in players) {
            if (other.color == player.color) continue
            for (token in other.tokens) {
                if (token.progress in 0..51 &&
                    (other.color.start + token.progress) % 52 == target) {
                    token.progress = -1
                }
            }
        }
    }

    private fun finishTurn() {
        rolled = false
        dice = 0
        turn = (turn + 1) % players.size
        message = if (turn == 0) "Tap ROLL DICE to start" else "${players[turn].color.title} is thinking..."
        invalidate()

        if (turn != 0 && !gameOver) {
            postDelayed({ computerTurn() }, 450L)
        }
    }

    private fun computerTurn() {
        if (gameOver || turn == 0 || rolled) return

        dice = Random.nextInt(1, 7)
        rolled = true
        val legalMoves = legalMovesForCurrentPlayer()

        if (legalMoves.isEmpty()) {
            rolled = false
            dice = 0
            postDelayed({ finishTurn() }, 550L)
            return
        }

        val player = players[turn]
        val chosen = legalMoves.maxByOrNull { scoreMove(player, it) } ?: legalMoves.first()
        message = "${player.color.title} rolled $dice"
        invalidate()

        postDelayed({
            if (!gameOver && turn != 0 && rolled) {
                moveToken(turn, chosen)
            }
        }, 600L)
    }

    private fun scoreMove(player: Player, index: Int): Int {
        val token = player.tokens[index]
        var score = 0

        if (token.progress < 0) score += 1000
        else score += token.progress * 4

        if (token.progress >= 0 && token.progress + dice == 56) score += 2000
        if (token.progress < 0 && dice == 6) score += 500

        if (token.progress in 0..51 && token.progress + dice <= 51) {
            val target = (player.color.start + token.progress + dice) % 52
            if (target !in safeSquares) {
                for (other in players) {
                    if (other.color != player.color && other.tokens.any {
                            it.progress in 0..51 &&
                                (other.color.start + it.progress) % 52 == target
                        }) {
                        score += 900
                    }
                }
            }
        }
        return score
    }

    private fun reset() {
        players.forEach { player ->
            player.tokens.forEach { it.progress = -1 }
        }
        turn = 0
        dice = 0
        rolled = false
        gameOver = false
        winner = 0
        message = "Tap ROLL DICE to start"
    }
}
