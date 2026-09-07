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
    private enum class PlayerColor(val main: Int, val start: Int, val title: String) {
        GREEN(Color.rgb(16, 170, 94), 39, "You"),
        YELLOW(Color.rgb(255, 198, 20), 0, "Computer 2"),
        BLUE(Color.rgb(24, 143, 224), 13, "Computer 3"),
        RED(Color.rgb(242, 48, 55), 26, "Computer 4")
    }

    private enum class State { WAIT_ROLL, SELECT_TOKEN, MOVING, AI_THINK, GAME_OVER }
    private data class Token(var progress: Int = -1)
    private data class Player(val color: PlayerColor, val tokens: MutableList<Token> = MutableList(4) { Token() })

    private val players = PlayerColor.values().map { Player(it) }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans", Typeface.BOLD)
    }

    private var turn = 0
    private var dice = 0
    private var state = State.WAIT_ROLL
    private var winner = -1
    private var message = "Your turn • Roll the dice"
    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardSize = 0f
    private var cell = 0f
    private var controlsTop = 0f
    private var movingToken = -1
    private var moveTarget = -1

    private val route = arrayOf(
        intArrayOf(6, 1), intArrayOf(6, 2), intArrayOf(6, 3), intArrayOf(6, 4), intArrayOf(6, 5),
        intArrayOf(5, 6), intArrayOf(4, 6), intArrayOf(3, 6), intArrayOf(2, 6), intArrayOf(1, 6), intArrayOf(0, 6),
        intArrayOf(0, 7), intArrayOf(0, 8), intArrayOf(1, 8), intArrayOf(2, 8), intArrayOf(3, 8), intArrayOf(4, 8), intArrayOf(5, 8),
        intArrayOf(6, 9), intArrayOf(6, 10), intArrayOf(6, 11), intArrayOf(6, 12), intArrayOf(6, 13), intArrayOf(6, 14),
        intArrayOf(7, 14), intArrayOf(8, 14), intArrayOf(8, 13), intArrayOf(8, 12), intArrayOf(8, 11), intArrayOf(8, 10), intArrayOf(8, 9),
        intArrayOf(9, 8), intArrayOf(10, 8), intArrayOf(11, 8), intArrayOf(12, 8), intArrayOf(13, 8), intArrayOf(14, 8),
        intArrayOf(14, 7), intArrayOf(14, 6), intArrayOf(13, 6), intArrayOf(12, 6), intArrayOf(11, 6), intArrayOf(10, 6), intArrayOf(9, 6),
        intArrayOf(8, 5), intArrayOf(8, 4), intArrayOf(8, 3), intArrayOf(8, 2), intArrayOf(8, 1), intArrayOf(8, 0), intArrayOf(7, 0), intArrayOf(6, 0)
    )
    private val safeSquares = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        reset()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        boardSize = min(w * 0.94f, h * 0.58f)
        boardLeft = (w - boardSize) / 2f
        boardTop = h * 0.125f
        cell = boardSize / 15f
        controlsTop = min(boardTop + boardSize + 26f, h - 190f)

        drawBackground(c, w, h)
        drawHeader(c, w)
        drawPlayerPanels(c, w)
        drawBoard(c)
        drawControls(c, w, h)
        if (state == State.GAME_OVER) drawGameOver(c, w, h)
    }

    private fun drawBackground(c: Canvas, w: Float, h: Float) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            Color.rgb(7, 70, 132), Color.rgb(2, 25, 58), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    private fun drawHeader(c: Canvas, w: Float) {
        text.textAlign = Paint.Align.CENTER
        text.textSize = w * 0.045f
        text.color = Color.WHITE
        c.drawText("HARSH NAGAR  •  LUDO", w / 2f, w * 0.105f, text)
    }

    private fun drawPlayerPanels(c: Canvas, w: Float) {
        val top = boardTop - 54f
        drawPanel(c, 14f, top, w * 0.44f, players[1])
        drawPanel(c, w * 0.56f, top, w - 14f, players[2])
    }

    private fun drawPanel(c: Canvas, left: Float, top: Float, right: Float, player: Player) {
        paint.color = Color.argb(35, 0, 0, 0)
        c.drawRoundRect(left, top + 4f, right, top + 55f, 16f, 16f, paint)
        paint.color = player.color.main
        c.drawRoundRect(left, top, right, top + 51f, 16f, 16f, paint)
        paint.color = Color.WHITE
        c.drawCircle(left + 23f, top + 25.5f, 12f, paint)
        paint.color = player.color.main
        c.drawCircle(left + 23f, top + 25.5f, 7f, paint)
        text.textAlign = Paint.Align.LEFT
        text.textSize = 13f
        text.color = Color.WHITE
        c.drawText(player.color.title, left + 42f, top + 31f, text)
    }

    private fun drawBoard(c: Canvas) {
        paint.color = Color.argb(45, 0, 0, 0)
        c.drawRoundRect(boardLeft + 4f, boardTop + 6f, boardLeft + boardSize + 4f, boardTop + boardSize + 6f, 8f, 8f, paint)
        paint.color = Color.WHITE
        c.drawRect(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize, paint)

        drawHome(c, 0, 0, PlayerColor.YELLOW)
        drawHome(c, 9, 0, PlayerColor.BLUE)
        drawHome(c, 0, 9, PlayerColor.GREEN)
        drawHome(c, 9, 9, PlayerColor.RED)

        drawLane(c, PlayerColor.YELLOW, 6, 1, 1, 5)
        drawLane(c, PlayerColor.BLUE, 9, 6, 5, 1)
        drawLane(c, PlayerColor.RED, 8, 9, 1, 5)
        drawLane(c, PlayerColor.GREEN, 1, 8, 5, 1)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, cell * 0.012f)
        paint.color = Color.rgb(190, 195, 200)
        for (r in 0..14) for (col in 0..14) {
            val x = boardLeft + col * cell
            val y = boardTop + r * cell
            c.drawRect(x, y, x + cell, y + cell, paint)
        }
        paint.style = Paint.Style.FILL

        val cx = boardLeft + 7.5f * cell
        val cy = boardTop + 7.5f * cell
        triangle(c, cx, cy, 6f, 6f, 9f, 6f, PlayerColor.YELLOW.main)
        triangle(c, cx, cy, 9f, 6f, 9f, 9f, PlayerColor.RED.main)
        triangle(c, cx, cy, 9f, 9f, 6f, 9f, PlayerColor.GREEN.main)
        triangle(c, cx, cy, 6f, 9f, 6f, 6f, PlayerColor.BLUE.main)

        for (index in safeSquares) {
            val q = route[index]
            drawStar(c, boardLeft + (q[1] + 0.5f) * cell, boardTop + (q[0] + 0.5f) * cell, cell * 0.19f)
        }
        drawTokens(c)
    }

    private fun drawHome(c: Canvas, col: Int, row: Int, color: PlayerColor) {
        paint.color = color.main
        c.drawRect(boardLeft + col * cell, boardTop + row * cell, boardLeft + (col + 6) * cell, boardTop + (row + 6) * cell, paint)
        paint.color = Color.WHITE
        c.drawRoundRect(
            boardLeft + (col + 0.95f) * cell, boardTop + (row + 0.95f) * cell,
            boardLeft + (col + 5.05f) * cell, boardTop + (row + 5.05f) * cell,
            cell * 0.28f, cell * 0.28f, paint
        )
        val spots = arrayOf(1.7f to 1.7f, 4.3f to 1.7f, 1.7f to 4.3f, 4.3f to 4.3f)
        paint.color = Color.argb(28, 0, 0, 0)
        for ((x, y) in spots) c.drawCircle(boardLeft + (col + x) * cell + 2f, boardTop + (row + y) * cell + 3f, cell * 0.35f, paint)
    }

    private fun drawLane(c: Canvas, color: PlayerColor, col: Int, row: Int, widthCells: Int, heightCells: Int) {
        paint.color = color.main
        c.drawRect(boardLeft + col * cell, boardTop + row * cell, boardLeft + (col + widthCells) * cell, boardTop + (row + heightCells) * cell, paint)
        paint.color = Color.argb(55, 255, 255, 255)
        for (r in 0 until heightCells) for (x in 0 until widthCells) {
            c.drawRect(boardLeft + (col + x) * cell, boardTop + (row + r) * cell, boardLeft + (col + x + 1) * cell, boardTop + (row + r + 1) * cell, paint)
        }
    }

    private fun triangle(c: Canvas, cx: Float, cy: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val p = Path()
        p.moveTo(cx, cy)
        p.lineTo(boardLeft + x1 * cell, boardTop + y1 * cell)
        p.lineTo(boardLeft + x2 * cell, boardTop + y2 * cell)
        p.close()
        paint.color = color
        c.drawPath(p, paint)
    }

    private fun drawStar(c: Canvas, x: Float, y: Float, radius: Float) {
        val path = Path()
        for (i in 0..9) {
            val angle = -Math.PI / 2 + i * Math.PI / 5
            val r = if (i % 2 == 0) radius else radius * 0.42f
            val px = x + cos(angle).toFloat() * r
            val py = y + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        paint.color = Color.rgb(110, 115, 120)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, cell * 0.025f)
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawTokens(c: Canvas) {
        for (player in players) for (index in player.tokens.indices) {
            val point = tokenPoint(player.color, player.tokens[index].progress, index) ?: continue
            drawToken(c, point.first, point.second, player.color, index)
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
            val spots = arrayOf(1.7f to 1.7f, 4.3f to 1.7f, 1.7f to 4.3f, 4.3f to 4.3f)
            val spot = spots[index]
            return boardLeft + (base.first + spot.first) * cell to boardTop + (base.second + spot.second) * cell
        }
        if (progress <= 51) {
            val q = route[(color.start + progress) % 52]
            return boardLeft + (q[1] + 0.5f) * cell to boardTop + (q[0] + 0.5f) * cell
        }
        val n = progress - 52
        return when (color) {
            PlayerColor.YELLOW -> boardLeft + 6.5f * cell to boardTop + (5.5f - n) * cell
            PlayerColor.BLUE -> boardLeft + (8.5f + n) * cell to boardTop + 6.5f * cell
            PlayerColor.RED -> boardLeft + 8.5f * cell to boardTop + (8.5f + n) * cell
            PlayerColor.GREEN -> boardLeft + (5.5f - n) * cell to boardTop + 8.5f * cell
        }
    }

    private fun drawToken(c: Canvas, x: Float, y: Float, color: PlayerColor, index: Int) {
        val selected = state == State.SELECT_TOKEN && turn == 0 && color == PlayerColor.GREEN && canMove(index, dice)
        paint.color = Color.argb(60, 0, 0, 0)
        c.drawCircle(x + 2f, y + 4f, cell * 0.35f, paint)
        paint.color = Color.WHITE
        c.drawCircle(x, y, cell * 0.31f, paint)
        paint.color = color.main
        c.drawCircle(x, y, cell * 0.245f, paint)
        paint.color = Color.WHITE
        c.drawCircle(x, y - cell * 0.08f, cell * 0.065f, paint)
        if (selected) {
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxOf(2f, cell * 0.035f)
            c.drawCircle(x, y, cell * 0.40f, paint)
            paint.style = Paint.Style.FILL
        }
        if (state == State.MOVING && turn == 0 && index == movingToken && color == PlayerColor.GREEN) {
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            c.drawCircle(x, y, cell * 0.40f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawControls(c: Canvas, w: Float, h: Float) {
        val panelH = min(74f, h * 0.065f)
        val gap = 10f
        val left = 14f
        val right = w - 14f
        val diceLeft = w * 0.43f
        val buttonLeft = w * 0.70f
        val y = controlsTop

        paint.color = Color.argb(45, 0, 0, 0)
        c.drawRoundRect(left, y + 4f, w * 0.39f, y + panelH + 4f, 16f, 16f, paint)
        paint.color = players[turn].color.main
        c.drawRoundRect(left, y, w * 0.39f, y + panelH, 16f, 16f, paint)
        text.textAlign = Paint.Align.LEFT
        text.textSize = 12f
        text.color = Color.WHITE
        c.drawText(if (turn == 0) "YOU" else players[turn].color.title.uppercase(), left + 16f, y + 25f, text)
        text.textSize = 10f
        c.drawText(if (turn == 0) "Your turn" else "Computer turn", left + 16f, y + 48f, text)

        paint.color = Color.WHITE
        c.drawRoundRect(diceLeft, y, w * 0.67f, y + panelH, 16f, 16f, paint)
        drawDice(c, (diceLeft + w * 0.67f) / 2f, y + panelH / 2f, dice)

        paint.color = if (state == State.WAIT_ROLL && turn == 0) Color.rgb(17, 119, 214) else Color.rgb(95, 113, 130)
        c.drawRoundRect(buttonLeft, y, right, y + panelH, 16f, 16f, paint)
        text.textAlign = Paint.Align.CENTER
        text.textSize = 12f
        text.color = Color.WHITE
        val buttonLabel = when (state) {
            State.WAIT_ROLL -> if (turn == 0) "ROLL DICE" else "WAIT"
            State.SELECT_TOKEN -> "SELECT TOKEN"
            State.MOVING -> "MOVING…"
            State.AI_THINK -> "THINKING…"
            State.GAME_OVER -> "PLAY AGAIN"
        }
        c.drawText(buttonLabel, (buttonLeft + right) / 2f, y + panelH * 0.61f, text)

        if (state != State.GAME_OVER) {
            text.textAlign = Paint.Align.CENTER
            text.textSize = 11f
            text.color = Color.WHITE
            c.drawText(message, w / 2f, y + panelH + gap + 17f, text)
        }
    }

    private fun drawDice(c: Canvas, x: Float, y: Float, value: Int) {
        val size = min(cell * 0.72f, 50f)
        paint.color = Color.rgb(248, 249, 250)
        c.drawRoundRect(x - size / 2f, y - size / 2f, x + size / 2f, y + size / 2f, 10f, 10f, paint)
        if (value !in 1..6) return
        paint.color = Color.rgb(45, 55, 65)
        val d = size * 0.25f
        val r = size * 0.075f
        val dots = when (value) {
            1 -> arrayOf(0 to 0)
            2 -> arrayOf(-1 to -1, 1 to 1)
            3 -> arrayOf(-1 to -1, 0 to 0, 1 to 1)
            4 -> arrayOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)
            5 -> arrayOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1)
            else -> arrayOf(-1 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 1 to 1)
        }
        for ((dx, dy) in dots) c.drawCircle(x + dx * d, y + dy * d, r, paint)
    }

    private fun drawGameOver(c: Canvas, w: Float, h: Float) {
        paint.color = Color.argb(205, 0, 0, 0)
        c.drawRect(0f, 0f, w, h, paint)
        val boxL = w * 0.09f
        val boxR = w * 0.91f
        val boxT = h * 0.36f
        val boxB = h * 0.62f
        paint.color = Color.WHITE
        c.drawRoundRect(boxL, boxT, boxR, boxB, 28f, 28f, paint)
        paint.color = players[winner].color.main
        c.drawRoundRect(boxL, boxT, boxR, boxT + 18f, 28f, 28f, paint)
        text.textAlign = Paint.Align.CENTER
        text.textSize = min(w * 0.065f, 34f)
        text.color = players[winner].color.main
        c.drawText(if (winner == 0) "YOU WIN!" else "${players[winner].color.title.uppercase()} WINS!", w / 2f, h * 0.47f, text)
        text.textSize = 15f
        text.color = Color.DKGRAY
        c.drawText("Great game!", w / 2f, h * 0.515f, text)
        paint.color = Color.rgb(17, 119, 214)
        c.drawRoundRect(w * 0.27f, h * 0.545f, w * 0.73f, h * 0.595f, 14f, 14f, paint)
        text.textSize = 13f
        text.color = Color.WHITE
        c.drawText("PLAY AGAIN", w / 2f, h * 0.578f, text)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x
        val y = event.y

        if (state == State.GAME_OVER) {
            reset()
            invalidate()
            return true
        }
        if (turn != 0 || state == State.MOVING || state == State.AI_THINK) return true

        val diceLeft = width * 0.43f
        val diceRight = width * 0.67f
        if (state == State.WAIT_ROLL && x in diceLeft..diceRight && y in controlsTop..(controlsTop + 100f)) {
            rollHumanDice()
            return true
        }

        if (state == State.SELECT_TOKEN) {
            for (index in 0..3) {
                val point = tokenPoint(PlayerColor.GREEN, players[0].tokens[index].progress, index) ?: continue
                val dx = x - point.first
                val dy = y - point.second
                if (dx * dx + dy * dy <= cell * cell * 0.9f && canMove(index, dice)) {
                    startMove(0, index)
                    return true
                }
            }
        }
        return true
    }

    private fun rollHumanDice() {
        if (state != State.WAIT_ROLL || turn != 0) return
        dice = Random.nextInt(1, 7)
        val legal = legalMoves()
        if (legal.isEmpty()) {
            state = State.SELECT_TOKEN
            message = "No legal move • Passing turn"
            invalidate()
            postDelayed({
                if (state == State.SELECT_TOKEN && turn == 0) finishTurn()
            }, 700L)
        } else {
            state = State.SELECT_TOKEN
            message = if (dice == 6) "You rolled 6 • Choose a token" else "Choose a highlighted token"
            invalidate()
        }
    }

    private fun legalMoves(): List<Int> = (0..3).filter { canMove(it, dice) }

    private fun canMove(index: Int, die: Int): Boolean {
        if (die !in 1..6 || index !in 0..3) return false
        val token = players[turn].tokens[index]
        if (token.progress == 56) return false
        if (token.progress < 0) return die == 6
        if (token.progress + die > 56) return false
        return !blockedByOpponent(players[turn].color, token.progress, die)
    }

    private fun blockedByOpponent(color: PlayerColor, progress: Int, die: Int): Boolean {
        if (progress !in 0..51 || progress + die > 51) return false
        val target = (color.start + progress + die) % 52
        return players.any { other ->
            other.color != color && other.tokens.count {
                it.progress in 0..51 && (other.color.start + it.progress) % 52 == target
            } >= 2
        }
    }

    private fun startMove(playerIndex: Int, tokenIndex: Int) {
        if (state == State.MOVING || state == State.GAME_OVER) return
        if (playerIndex != turn || !canMove(tokenIndex, dice)) return
        movingToken = tokenIndex
        moveTarget = if (players[playerIndex].tokens[tokenIndex].progress < 0) 0 else players[playerIndex].tokens[tokenIndex].progress + dice
        state = State.MOVING
        message = "Moving token…"
        animateStep(playerIndex, tokenIndex)
    }

    private fun animateStep(playerIndex: Int, tokenIndex: Int) {
        if (state != State.MOVING || playerIndex != turn) return
        val token = players[playerIndex].tokens[tokenIndex]
        val next = if (token.progress < 0) 0 else token.progress + 1
        token.progress = next
        invalidate()
        if (next >= moveTarget) {
            postDelayed({ completeMove(playerIndex, tokenIndex) }, 90L)
        } else {
            postDelayed({ animateStep(playerIndex, tokenIndex) }, 105L)
        }
    }

    private fun completeMove(playerIndex: Int, tokenIndex: Int) {
        if (state != State.MOVING || playerIndex != turn) return
        val player = players[playerIndex]
        captureOpponents(player, player.tokens[tokenIndex])
        movingToken = -1
        moveTarget = -1

        if (player.tokens.all { it.progress == 56 }) {
            winner = playerIndex
            state = State.GAME_OVER
            dice = 0
            message = "Game over"
            invalidate()
            return
        }

        val rolledSix = dice == 6
        dice = 0
        if (rolledSix) {
            state = State.WAIT_ROLL
            message = if (turn == 0) "Six! Roll again" else "Six! Computer rolls again"
            invalidate()
            if (turn != 0) postDelayed({ computerTurn() }, 450L)
        } else {
            finishTurn()
        }
    }

    private fun captureOpponents(player: Player, movingToken: Token) {
        if (movingToken.progress !in 0..51) return
        val target = (player.color.start + movingToken.progress) % 52
        if (target in safeSquares) return
        for (other in players) {
            if (other.color == player.color) continue
            for (token in other.tokens) {
                if (token.progress in 0..51 && (other.color.start + token.progress) % 52 == target) {
                    token.progress = -1
                }
            }
        }
    }

    private fun finishTurn() {
        if (state == State.GAME_OVER) return
        dice = 0
        movingToken = -1
        moveTarget = -1
        turn = (turn + 1) % players.size
        if (turn == 0) {
            state = State.WAIT_ROLL
            message = "Your turn • Roll the dice"
        } else {
            state = State.AI_THINK
            message = "${players[turn].color.title} is thinking…"
            postDelayed({ computerTurn() }, 500L)
        }
        invalidate()
    }

    private fun computerTurn() {
        if (state == State.GAME_OVER || turn == 0 || state != State.AI_THINK) return
        dice = Random.nextInt(1, 7)
        val legal = legalMoves()
        if (legal.isEmpty()) {
            message = "${players[turn].color.title} rolled $dice • No move"
            invalidate()
            postDelayed({ if (state == State.AI_THINK) finishTurn() }, 650L)
            return
        }
        val player = players[turn]
        val chosen = legal.maxByOrNull { scoreMove(player, it) } ?: legal.first()
        message = "${player.color.title} rolled $dice"
        invalidate()
        postDelayed({
            if (state == State.AI_THINK && turn != 0) startMove(turn, chosen)
        }, 550L)
    }

    private fun scoreMove(player: Player, index: Int): Int {
        val token = player.tokens[index]
        var score = if (token.progress < 0) 700 else token.progress * 5
        if (token.progress >= 0 && token.progress + dice == 56) score += 3000
        if (token.progress < 0 && dice == 6) score += 800
        if (token.progress in 0..51 && token.progress + dice <= 51) {
            val target = (player.color.start + token.progress + dice) % 52
            if (target !in safeSquares) {
                for (other in players) if (other.color != player.color) {
                    if (other.tokens.any { it.progress in 0..51 && (other.color.start + it.progress) % 52 == target }) score += 1400
                }
            }
        }
        return score
    }

    private fun reset() {
        removeCallbacks(null)
        players.forEach { player -> player.tokens.forEach { it.progress = -1 } }
        turn = 0
        dice = 0
        state = State.WAIT_ROLL
        winner = -1
        movingToken = -1
        moveTarget = -1
        message = "Your turn • Roll the dice"
    }
}
