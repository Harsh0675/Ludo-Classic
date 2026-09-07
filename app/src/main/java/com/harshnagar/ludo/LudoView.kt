package com.harshnagar.ludo

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class LudoView(context: Context) : View(context) {

    private enum class P(val c: Int, val dark: Int, val start: Int, val name: String) {
        GREEN(Color.rgb(20, 156, 91), Color.rgb(0, 105, 62), 39, "You"),
        YELLOW(Color.rgb(250, 205, 20), Color.rgb(184, 137, 0), 0, "Computer 2"),
        BLUE(Color.rgb(25, 139, 224), Color.rgb(0, 83, 160), 13, "Computer 3"),
        RED(Color.rgb(239, 48, 48), Color.rgb(175, 15, 15), 26, "Computer 4")
    }

    private data class Token(var pos: Int = -1)
    private data class Player(val p: P, val tokens: MutableList<Token> = MutableList(4) { Token() })

    private val players = P.values().map { Player(it) }
    private var turn = 0
    private var dice = 0
    private var rolled = false
    private var gameOver = false
    private var message = "Tap the dice to roll"
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans", Typeface.BOLD) }

    private var boardLeft = 0f
    private var boardTop = 0f
    private var cell = 0f

    private val route = arrayOf(
        intArrayOf(6,1), intArrayOf(6,2), intArrayOf(6,3), intArrayOf(6,4), intArrayOf(6,5),
        intArrayOf(5,6), intArrayOf(4,6), intArrayOf(3,6), intArrayOf(2,6), intArrayOf(1,6), intArrayOf(0,6),
        intArrayOf(0,7), intArrayOf(0,8),
        intArrayOf(1,8), intArrayOf(2,8), intArrayOf(3,8), intArrayOf(4,8), intArrayOf(5,8),
        intArrayOf(6,9), intArrayOf(6,10), intArrayOf(6,11), intArrayOf(6,12), intArrayOf(6,13), intArrayOf(6,14),
        intArrayOf(7,14), intArrayOf(8,14),
        intArrayOf(8,13), intArrayOf(8,12), intArrayOf(8,11), intArrayOf(8,10), intArrayOf(8,9),
        intArrayOf(9,8), intArrayOf(10,8), intArrayOf(11,8), intArrayOf(12,8), intArrayOf(13,8), intArrayOf(14,8),
        intArrayOf(14,7), intArrayOf(14,6),
        intArrayOf(13,6), intArrayOf(12,6), intArrayOf(11,6), intArrayOf(10,6), intArrayOf(9,6),
        intArrayOf(8,5), intArrayOf(8,4), intArrayOf(8,3), intArrayOf(8,2), intArrayOf(8,1), intArrayOf(8,0),
        intArrayOf(7,0), intArrayOf(6,0)
    )

    private val safe = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    init {
        setBackgroundColor(Color.rgb(7, 48, 95))
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val top = h * 0.13f
        val boardSize = min(w * 0.94f, h * 0.60f)
        boardLeft = (w - boardSize) / 2f
        boardTop = top
        cell = boardSize / 15f

        drawBackground(c, w, h)
        drawTopPanels(c, w)
        drawBoard(c)
        drawBottomPanel(c, w, h)
        if (gameOver) drawOverlay(c, w, h)
    }

    private fun drawBackground(c: Canvas, w: Float, h: Float) {
        val bg = LinearGradient(0f, 0f, 0f, h, Color.rgb(8, 66, 125), Color.rgb(2, 24, 55), Shader.TileMode.CLAMP)
        paint.shader = bg
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        text.textSize = w * .045f
        text.color = Color.WHITE
        text.textAlign = Paint.Align.CENTER
        c.drawText("HARSH NAGAR  •  LUDO", w / 2f, h * .055f, text)
    }

    private fun drawTopPanels(c: Canvas, w: Float) {
        drawPlayerPanel(c, 18f, boardTop - 76f, w * .43f, players[1])
        drawPlayerPanel(c, w * .57f, boardTop - 76f, w - 18f, players[2])
    }

    private fun drawPlayerPanel(c: Canvas, l: Float, t: Float, r: Float, pl: Player) {
        paint.color = pl.p.c
        c.drawRoundRect(l, t, r, t + 58f, 18f, 18f, paint)
        paint.color = Color.WHITE
        c.drawCircle(l + 28f, t + 29f, 14f, paint)
        paint.color = pl.p.c
        c.drawCircle(l + 28f, t + 29f, 9f, paint)
        text.textSize = 17f
        text.color = Color.WHITE
        text.textAlign = Paint.Align.LEFT
        c.drawText(pl.p.name, l + 52f, t + 36f, text)
    }

    private fun drawBoard(c: Canvas) {
        paint.color = Color.rgb(245, 245, 245)
        c.drawRect(boardLeft, boardTop, boardLeft + 15 * cell, boardTop + 15 * cell, paint)

        drawHome(c, 0, 0, P.YELLOW)
        drawHome(c, 9, 0, P.BLUE)
        drawHome(c, 0, 9, P.GREEN)
        drawHome(c, 9, 9, P.RED)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(185, 185, 185)
        for (r in 0..14) for (col in 0..14) {
            val x = boardLeft + col * cell
            val y = boardTop + r * cell
            c.drawRect(x, y, x + cell, y + cell, paint)
        }
        paint.style = Paint.Style.FILL

        drawLane(c, P.YELLOW)
        drawLane(c, P.BLUE)
        drawLane(c, P.RED)
        drawLane(c, P.GREEN)

        val cx = boardLeft + 7.5f * cell
        val cy = boardTop + 7.5f * cell
        tri(c, cx, cy, boardLeft + 6 * cell, boardTop + 6 * cell, boardLeft + 9 * cell, boardTop + 6 * cell, P.YELLOW.c)
        tri(c, cx, cy, boardLeft + 9 * cell, boardTop + 6 * cell, boardLeft + 9 * cell, boardTop + 9 * cell, P.RED.c)
        tri(c, cx, cy, boardLeft + 9 * cell, boardTop + 9 * cell, boardLeft + 6 * cell, boardTop + 9 * cell, P.GREEN.c)
        tri(c, cx, cy, boardLeft + 6 * cell, boardTop + 9 * cell, boardLeft + 6 * cell, boardTop + 6 * cell, P.BLUE.c)

        for (i in safe) {
            val q = route[i]
            drawStar(c, boardLeft + (q[1] + .5f) * cell, boardTop + (q[0] + .5f) * cell, cell * .24f)
        }
        drawTokens(c)
    }

    private fun drawHome(c: Canvas, col: Int, row: Int, p: P) {
        paint.color = p.c
        c.drawRect(boardLeft + col * cell, boardTop + row * cell, boardLeft + (col + 6) * cell, boardTop + (row + 6) * cell, paint)
        paint.color = Color.WHITE
        c.drawRoundRect(boardLeft + (col + 1) * cell, boardTop + (row + 1) * cell, boardLeft + (col + 5) * cell, boardTop + (row + 5) * cell, 14f, 14f, paint)
        paint.color = p.c
        val spots = arrayOf(1 to 1, 4 to 1, 1 to 4, 4 to 4)
        for ((dx, dy) in spots) c.drawCircle(boardLeft + (col + dx) * cell, boardTop + (row + dy) * cell, cell * .34f, paint)
        text.textSize = cell * .35f
        text.color = Color.WHITE
        text.textAlign = Paint.Align.CENTER
        val label = if (p == P.GREEN) "YOU" else p.name.uppercase()
        c.drawText(label, boardLeft + (col + 3) * cell, boardTop + (row + 6.65f) * cell, text)
    }

    private fun drawLane(c: Canvas, p: P) {
        paint.color = p.c
        when (p) {
            P.YELLOW -> c.drawRect(boardLeft + 6 * cell, boardTop + 1 * cell, boardLeft + 7 * cell, boardTop + 6 * cell, paint)
            P.BLUE -> c.drawRect(boardLeft + 9 * cell, boardTop + 6 * cell, boardLeft + 14 * cell, boardTop + 7 * cell, paint)
            P.RED -> c.drawRect(boardLeft + 8 * cell, boardTop + 9 * cell, boardLeft + 9 * cell, boardTop + 14 * cell, paint)
            P.GREEN -> c.drawRect(boardLeft + 1 * cell, boardTop + 8 * cell, boardLeft + 6 * cell, boardTop + 9 * cell, paint)
        }
    }

    private fun tri(c: Canvas, cx: Float, cy: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val path = Path().apply { moveTo(cx, cy); lineTo(x1, y1); lineTo(x2, y2); close() }
        paint.color = color
        c.drawPath(path, paint)
    }

    private fun drawStar(c: Canvas, x: Float, y: Float, r: Float) {
        val path = Path()
        for (i in 0..9) {
            val a = -Math.PI / 2 + i * Math.PI / 5
            val rr = if (i % 2 == 0) r else r * .42
            val px = x + cos(a).toFloat() * rr
            val py = y + sin(a).toFloat() * rr
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        paint.color = Color.rgb(150, 150, 150)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawTokens(c: Canvas) {
        for (pl in players) for ((idx, t) in pl.tokens.withIndex()) {
            val pt = tokenPoint(pl.p, t.pos, idx) ?: continue
            drawToken(c, pt.first, pt.second, pl.p, idx)
        }
    }

    private fun tokenPoint(p: P, pos: Int, index: Int): Pair<Float, Float>? {
        if (pos < 0) {
            val base = when (p) {
                P.YELLOW -> 0 to 0
                P.BLUE -> 9 to 0
                P.GREEN -> 0 to 9
                P.RED -> 9 to 9
            }
            val spots = arrayOf(0f to 0f, 0f to 3f, 3f to 0f, 3f to 3f)
            val s = spots[index]
            return Pair(boardLeft + (base.first + 1.7f + s.first) * cell, boardTop + (base.second + 1.7f + s.second) * cell)
        }
        if (pos <= 51) {
            val global = (p.start + pos) % 52
            val q = route[global]
            return Pair(boardLeft + (q[1] + .5f) * cell, boardTop + (q[0] + .5f) * cell)
        }
        val n = pos - 52
        return when (p) {
            P.YELLOW -> Pair(boardLeft + 6.5f * cell, boardTop + (5.5f - n) * cell)
            P.BLUE -> Pair(boardLeft + (8.5f + n) * cell, boardTop + 6.5f * cell)
            P.RED -> Pair(boardLeft + 8.5f * cell, boardTop + (8.5f + n) * cell)
            P.GREEN -> Pair(boardLeft + (5.5f - n) * cell, boardTop + 8.5f * cell)
        }
    }

    private fun drawToken(c: Canvas, x: Float, y: Float, p: P, index: Int) {
        paint.color = Color.argb(65, 0, 0, 0)
        c.drawCircle(x + 2, y + 4, cell * .34f, paint)
        paint.color = Color.WHITE
        c.drawCircle(x, y, cell * .31f, paint)
        paint.color = p.c
        c.drawCircle(x, y - cell * .03f, cell * .25f, paint)
        paint.color = Color.WHITE
        c.drawCircle(x, y - cell * .13f, cell * .07f, paint)
        if (p == players[turn].p && rolled && canMove(index, dice)) {
            paint.color = Color.argb(180, 255, 255, 255)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            c.drawCircle(x, y, cell * .39f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawBottomPanel(c: Canvas, w: Float, h: Float) {
        val y = h * .77f
        paint.color = players[0].p.c
        c.drawRoundRect(18f, y, w * .42f, y + 74f, 18f, 18f, paint)
        text.textAlign = Paint.Align.LEFT
        text.textSize = 18f
        text.color = Color.WHITE
        c.drawText("YOU", 35f, y + 27f, text)
        text.textSize = 13f
        c.drawText("Turn: ${players[turn].p.name}", 35f, y + 51f, text)

        paint.color = Color.WHITE
        c.drawRoundRect(w * .45f, y, w * .68f, y + 74f, 18f, 18f, paint)
        drawDice(c, w * .565f, y + 37f, dice)

        paint.color = Color.rgb(22, 112, 203)
        c.drawRoundRect(w * .71f, y, w - 18f, y + 74f, 18f, 18f, paint)
        text.textAlign = Paint.Align.CENTER
        text.textSize = 16f
        text.color = Color.WHITE
        c.drawText(if (rolled) "Choose a token" else "ROLL DICE", w * .855f, y + 44f, text)

        if (turn == 0 && !rolled && !gameOver) {
            text.textSize = 14f
            c.drawText(message, w / 2f, y + 100f, text)
        }
    }

    private fun drawDice(c: Canvas, x: Float, y: Float, v: Int) {
        paint.color = Color.WHITE
        c.drawRoundRect(x - 25, y - 25, x + 25, y + 25, 10f, 10f, paint)
        if (v == 0) return
        paint.color = Color.DKGRAY
        val d = 12f
        val dots = when (v) {
            1 -> arrayOf(0 to 0)
            2 -> arrayOf(-1 to -1, 1 to 1)
            3 -> arrayOf(-1 to -1, 0 to 0, 1 to 1)
            4 -> arrayOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)
            5 -> arrayOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1)
            else -> arrayOf(-1 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 1 to 1)
        }
        for ((a, b) in dots) c.drawCircle(x + a * d, y + b * d, 4f, paint)
    }

    private fun drawOverlay(c: Canvas, w: Float, h: Float) {
        paint.color = Color.argb(205, 0, 0, 0)
        c.drawRect(0f, 0f, w, h, paint)
        text.textAlign = Paint.Align.CENTER
        text.textSize = 34f
        text.color = Color.WHITE
        c.drawText("${players[turn].p.name} WINS!", w / 2f, h * .45f, text)
        text.textSize = 18f
        c.drawText("Tap anywhere to start a new game", w / 2f, h * .51f, text)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        if (gameOver) {
            reset()
            invalidate()
            return true
        }
        val x = e.x
        val y = e.y
        val h = height.toFloat()
        val diceY = h * .77f
        if (turn == 0 && !rolled && x > width * .43f && x < width * .70f && y > diceY && y < diceY + 90f) {
            roll()
            return true
        }
        if (turn == 0 && rolled) {
            for (i in 0..3) {
                val pt = tokenPoint(P.GREEN, players[0].tokens[i].pos, i) ?: continue
                if ((x - pt.first) * (x - pt.first) + (y - pt.second) * (y - pt.second) < cell * cell * .55f && canMove(i, dice)) {
                    moveToken(0, i)
                    return true
                }
            }
        }
        return true
    }

    private fun roll() {
        dice = Random.nextInt(1, 7)
        rolled = true
        val moves = (0..3).filter { canMove(it, dice) }
        if (moves.isEmpty()) {
            message = "No legal move"
            postDelayed({ finishTurn() }, 650)
        } else message = "Select a highlighted token"
        invalidate()
    }

    private fun canMove(i: Int, d: Int): Boolean {
        if (d == 0) return false
        val t = players[turn].tokens[i]
        if (t.pos < 0) return d == 6
        return t.pos + d <= 56 && !blocked(players[turn].p, t.pos, d)
    }

    private fun blocked(p: P, pos: Int, d: Int): Boolean {
        if (pos < 0 || pos + d > 51) return false
        val target = (p.start + pos + d) % 52
        for (pl in players) {
            if (pl.p == p) continue
            val count = pl.tokens.count { it.pos in 0..51 && (pl.p.start + it.pos) % 52 == target }
            if (count >= 2) return true
        }
        return false
    }

    private fun moveToken(playerIndex: Int, tokenIndex: Int) {
        val pl = players[playerIndex]
        val t = pl.tokens[tokenIndex]
        if (t.pos < 0) t.pos = 0 else t.pos += dice

        if (t.pos in 0..51) {
            val target = (pl.p.start + t.pos) % 52
            if (target !in safe) {
                for (other in players) {
                    if (other.p == pl.p) continue
                    for (ot in other.tokens) {
                        if (ot.pos in 0..51 && (other.p.start + ot.pos) % 52 == target) {
                            ot.pos = -1
                            break
                        }
                    }
                }
            }
        }

        val won = pl.tokens.all { it.pos == 56 }
        if (won) {
            gameOver = true
            invalidate()
            return
        }

        if (dice == 6) {
            rolled = false
            dice = 0
            message = "Six! Roll again"
            invalidate()
        } else finishTurn()
    }

    private fun finishTurn() {
        rolled = false
        dice = 0
        turn = (turn + 1) % 4
        message = if (turn == 0) "Tap the dice to roll" else "${players[turn].p.name} is thinking..."
        invalidate()
        if (turn != 0) postDelayed({ computerTurn() }, 500)
    }

    private fun computerTurn() {
        if (gameOver || turn == 0) return
        roll()
        val legal = (0..3).filter { canMove(it, dice) }
        if (legal.isEmpty()) {
            postDelayed({ finishTurn() }, 600)
            return
        }
        val p = players[turn]
        val best = legal.maxByOrNull { scoreMove(p, it) } ?: legal.first()
        postDelayed({
            if (!gameOver && turn != 0 && rolled) moveToken(turn, best)
        }, 550)
    }

    private fun scoreMove(pl: Player, i: Int): Int {
        val t = pl.tokens[i]
        var score = if (t.pos < 0) 1000 else t.pos * 3
        if (t.pos < 0 && dice == 6) score += 500
        if (t.pos >= 0 && t.pos + dice == 56) score += 900
        if (t.pos in 0..51) {
            val target = (pl.p.start + t.pos + dice) % 52
            if (target !in safe) {
                for (o in players) if (o.p != pl.p && o.tokens.any { it.pos in 0..51 && (o.p.start + it.pos) % 52 == target }) score += 700
            }
        }
        return score
    }

    private fun reset() {
        players.forEach { it.tokens.forEach { t -> t.pos = -1 } }
        turn = 0
        dice = 0
        rolled = false
        gameOver = false
        message = "Tap the dice to roll"
    }
}
