package com.harshnagar.ludo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.random.Random

class LudoView(context: Context, private val playerNames: List<String>) : View(context) {
    private enum class P(val color: Int, val start: Int) {
        GREEN(Color.rgb(16, 170, 94), 39),
        YELLOW(Color.rgb(255, 198, 20), 0),
        BLUE(Color.rgb(24, 143, 224), 13),
        RED(Color.rgb(242, 48, 55), 26)
    }
    private data class Token(var progress: Int = -1)
    private data class Player(val p: P, val tokens: MutableList<Token> = MutableList(4) { Token() })
    private enum class State { ROLL, SELECT, MOVING, WON }

    private val players = P.values().map { Player(it) }
    private val names = (0..3).map { i -> playerNames.getOrNull(i)?.takeIf { it.isNotBlank() } ?: "Player ${i + 1}" }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
    private var turn = 0
    private var dice = 0
    private var state = State.ROLL
    private var winner = -1
    private var board = 0f
    private var left = 0f
    private var top = 0f
    private var cell = 0f
    private var controlY = 0f
    private var moving = -1
    private var target = -1
    private var celebrationAt = 0L

    private val route = arrayOf(
        intArrayOf(6,1), intArrayOf(6,2), intArrayOf(6,3), intArrayOf(6,4), intArrayOf(6,5), intArrayOf(5,6), intArrayOf(4,6), intArrayOf(3,6), intArrayOf(2,6), intArrayOf(1,6), intArrayOf(0,6),
        intArrayOf(0,7), intArrayOf(0,8), intArrayOf(1,8), intArrayOf(2,8), intArrayOf(3,8), intArrayOf(4,8), intArrayOf(5,8), intArrayOf(6,9), intArrayOf(6,10), intArrayOf(6,11), intArrayOf(6,12), intArrayOf(6,13), intArrayOf(6,14),
        intArrayOf(7,14), intArrayOf(8,14), intArrayOf(8,13), intArrayOf(8,12), intArrayOf(8,11), intArrayOf(8,10), intArrayOf(8,9), intArrayOf(9,8), intArrayOf(10,8), intArrayOf(11,8), intArrayOf(12,8), intArrayOf(13,8), intArrayOf(14,8),
        intArrayOf(14,7), intArrayOf(14,6), intArrayOf(13,6), intArrayOf(12,6), intArrayOf(11,6), intArrayOf(10,6), intArrayOf(9,6), intArrayOf(8,5), intArrayOf(8,4), intArrayOf(8,3), intArrayOf(8,2), intArrayOf(8,1), intArrayOf(8,0), intArrayOf(7,0), intArrayOf(6,0)
    )
    private val safe = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    init { reset() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        board = min(w * 0.94f, h * 0.58f)
        left = (w - board) / 2f
        top = h * 0.125f
        cell = board / 15f
        controlY = min(top + board + 26f, h - 175f)
        paint.color = Color.rgb(4, 35, 75)
        c.drawRect(0f, 0f, w, h, paint)
        drawText(c, "LUDO CLASSIC", w / 2f, h * 0.055f, w * 0.045f, Color.WHITE, Paint.Align.CENTER)
        drawText(c, "HARSH NAGAR • 4 PLAYER OFFLINE", w / 2f, h * 0.095f, w * 0.024f, Color.rgb(185, 215, 240), Paint.Align.CENTER)
        drawBoard(c)
        drawControls(c, w, h)
        if (state == State.WON) drawWinner(c, w, h)
    }

    private fun drawBoard(c: Canvas) {
        paint.color = Color.WHITE
        c.drawRect(left, top, left + board, top + board, paint)
        home(c, 0, 0, P.YELLOW)
        home(c, 9, 0, P.BLUE)
        home(c, 0, 9, P.GREEN)
        home(c, 9, 9, P.RED)
        lane(c, P.YELLOW, 6, 1, 1, 5)
        lane(c, P.BLUE, 9, 6, 5, 1)
        lane(c, P.RED, 8, 9, 1, 5)
        lane(c, P.GREEN, 1, 8, 5, 1)
        paint.color = Color.LTGRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        for (r in 0..14) for (col in 0..14) c.drawRect(left + col * cell, top + r * cell, left + (col + 1) * cell, top + (r + 1) * cell, paint)
        paint.style = Paint.Style.FILL
        drawTokens(c)
    }

    private fun home(c: Canvas, col: Int, row: Int, p: P) {
        paint.color = p.color
        c.drawRect(left + col * cell, top + row * cell, left + (col + 6) * cell, top + (row + 6) * cell, paint)
        paint.color = Color.WHITE
        c.drawRoundRect(left + (col + 1) * cell, top + (row + 1) * cell, left + (col + 5) * cell, top + (row + 5) * cell, cell * .25f, cell * .25f, paint)
    }

    private fun lane(c: Canvas, p: P, col: Int, row: Int, cw: Int, rh: Int) {
        paint.color = p.color
        c.drawRect(left + col * cell, top + row * cell, left + (col + cw) * cell, top + (row + rh) * cell, paint)
    }

    private fun drawTokens(c: Canvas) {
        for (pi in players.indices) for (ti in 0..3) {
            val pos = point(pi, players[pi].tokens[ti].progress, ti)
            paint.color = Color.WHITE
            c.drawCircle(pos.first, pos.second, cell * .30f, paint)
            paint.color = players[pi].p.color
            c.drawCircle(pos.first, pos.second, cell * .22f, paint)
            if (state == State.SELECT && pi == turn && canMove(ti, dice)) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.WHITE
                c.drawCircle(pos.first, pos.second, cell * .38f, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    private fun point(pi: Int, progress: Int, index: Int): Pair<Float, Float> {
        val p = players[pi].p
        if (progress < 0) {
            val bx = if (p == P.BLUE || p == P.RED) 9f else 0f
            val by = if (p == P.GREEN || p == P.RED) 9f else 0f
            val spots = arrayOf(1.7f to 1.7f, 4.3f to 1.7f, 1.7f to 4.3f, 4.3f to 4.3f)
            return left + (bx + spots[index].first) * cell to top + (by + spots[index].second) * cell
        }
        if (progress <= 51) {
            val q = route[(p.start + progress) % 52]
            return left + (q[1] + .5f) * cell to top + (q[0] + .5f) * cell
        }
        val n = progress - 52
        return when (p) {
            P.YELLOW -> left + 6.5f * cell to top + (5.5f - n) * cell
            P.BLUE -> left + (8.5f + n) * cell to top + 6.5f * cell
            P.RED -> left + 8.5f * cell to top + (8.5f + n) * cell
            P.GREEN -> left + (5.5f - n) * cell to top + 8.5f * cell
        }
    }

    private fun drawControls(c: Canvas, w: Float, h: Float) {
        paint.color = players[turn].p.color
        c.drawRoundRect(14f, controlY, w * .39f, controlY + 68f, 14f, 14f, paint)
        drawText(c, names[turn], 28f, controlY + 27f, 13f, Color.WHITE, Paint.Align.LEFT)
        drawText(c, if (state == State.ROLL) "Roll the dice" else "Choose a token", 28f, controlY + 49f, 10f, Color.WHITE, Paint.Align.LEFT)
        paint.color = Color.WHITE
        c.drawRoundRect(w * .43f, controlY, w * .67f, controlY + 68f, 14f, 14f, paint)
        drawDice(c, w * .55f, controlY + 34f, dice)
        paint.color = if (state == State.ROLL) Color.rgb(17, 119, 214) else Color.rgb(95, 110, 125)
        c.drawRoundRect(w * .70f, controlY, w - 14f, controlY + 68f, 14f, 14f, paint)
        drawText(c, if (state == State.ROLL) "ROLL DICE" else "SELECT TOKEN", w * .85f, controlY + 41f, 12f, Color.WHITE, Paint.Align.CENTER)
    }

    private fun drawDice(c: Canvas, x: Float, y: Float, value: Int) {
        if (value !in 1..6) return
        paint.color = Color.rgb(35, 45, 55)
        c.drawCircle(x, y, 6f, paint)
        if (value % 2 == 0) { c.drawCircle(x - 12, y - 12, 4f, paint); c.drawCircle(x + 12, y + 12, 4f, paint) }
        if (value >= 3) { c.drawCircle(x - 12, y + 12, 4f, paint); c.drawCircle(x + 12, y - 12, 4f, paint) }
        if (value == 6) { c.drawCircle(x - 12, y, 4f, paint); c.drawCircle(x + 12, y, 4f, paint) }
    }

    private fun drawWinner(c: Canvas, w: Float, h: Float) {
        paint.color = Color.argb(225, 0, 5, 20)
        c.drawRect(0f, 0f, w, h, paint)
        val l = w * .08f
        val r = w * .92f
        val t = h * .22f
        val b = h * .75f
        paint.color = Color.WHITE
        c.drawRoundRect(l, t, r, b, 28f, 28f, paint)
        paint.color = players[winner].p.color
        c.drawRoundRect(l, t, r, t + 18f, 28f, 28f, paint)
        val cx = w / 2f
        val crown = Path()
        crown.moveTo(cx - 50f, t + 90f)
        crown.lineTo(cx - 38f, t + 48f)
        crown.lineTo(cx - 15f, t + 72f)
        crown.lineTo(cx, t + 38f)
        crown.lineTo(cx + 15f, t + 72f)
        crown.lineTo(cx + 38f, t + 48f)
        crown.lineTo(cx + 50f, t + 90f)
        crown.close()
        paint.color = Color.rgb(255, 195, 20)
        c.drawPath(crown, paint)
        drawText(c, "WINNER!", cx, t + 170f, min(w * .075f, 38f), players[winner].p.color, Paint.Align.CENTER)
        drawText(c, names[winner], cx, t + 215f, min(w * .06f, 30f), Color.DKGRAY, Paint.Align.CENTER)
        drawText(c, "What a game! 🎉", cx, t + 247f, 16f, Color.DKGRAY, Paint.Align.CENTER)
        paint.color = players[winner].p.color
        c.drawRoundRect(w * .22f, b - 52f, w * .78f, b - 8f, 14f, 14f, paint)
        drawText(c, "PLAY AGAIN", cx, b - 23f, 14f, Color.WHITE, Paint.Align.CENTER)
    }

    private fun drawText(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, align: Paint.Align) {
        text.textSize = size
        text.color = color
        text.textAlign = align
        c.drawText(s, x, y, text)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        val x = e.x
        val y = e.y
        if (state == State.WON) { reset(); invalidate(); return true }
        if (state == State.ROLL && x in width * .70f..(width - 14f) && y in controlY..(controlY + 80f)) { roll(); return true }
        if (state == State.SELECT) for (i in 0..3) {
            val q = point(turn, players[turn].tokens[i].progress, i)
            val dx = x - q.first
            val dy = y - q.second
            if (dx * dx + dy * dy < cell * cell && canMove(i, dice)) { move(turn, i); return true }
        }
        return true
    }

    private fun roll() {
        dice = Random.nextInt(1, 7)
        if ((0..3).none { canMove(it, dice) }) {
            postDelayed({ finishTurn() }, 500)
        } else {
            state = State.SELECT
            invalidate()
        }
    }

    private fun canMove(index: Int, d: Int): Boolean {
        val t = players[turn].tokens[index]
        if (d !in 1..6 || t.progress == 56) return false
        if (t.progress < 0) return d == 6
        if (t.progress + d > 56) return false
        if (t.progress <= 51 && t.progress + d <= 51) {
            val targetAbs = (players[turn].p.start + t.progress + d) % 52
            val blocked = players.any { other ->
                other.p != players[turn].p && other.tokens.count { it.progress in 0..51 && (other.p.start + it.progress) % 52 == targetAbs } >= 2
            }
            if (blocked) return false
        }
        return true
    }

    private fun move(pi: Int, ti: Int) {
        moving = ti
        target = if (players[pi].tokens[ti].progress < 0) 0 else players[pi].tokens[ti].progress + dice
        state = State.MOVING
        step(pi, ti)
    }

    private fun step(pi: Int, ti: Int) {
        if (state != State.MOVING) return
        val token = players[pi].tokens[ti]
        token.progress = if (token.progress < 0) 0 else token.progress + 1
        invalidate()
        if (token.progress < target) postDelayed({ step(pi, ti) }, 70) else postDelayed({ complete(pi, ti) }, 80)
    }

    private fun complete(pi: Int, ti: Int) {
        if (state != State.MOVING) return
        val player = players[pi]
        capture(player, player.tokens[ti])
        moving = -1
        target = -1
        if (player.tokens.all { it.progress == 56 }) {
            winner = pi
            state = State.WON
            celebrationAt = System.currentTimeMillis()
            invalidate()
            return
        }
        val extra = dice == 6
        dice = 0
        if (extra) { state = State.ROLL; invalidate() } else finishTurn()
    }

    private fun capture(player: Player, token: Token) {
        if (token.progress !in 0..51) return
        val absolute = (player.p.start + token.progress) % 52
        if (absolute in safe) return
        players.filter { it.p != player.p }.forEach { other ->
            other.tokens.forEach { enemy ->
                if (enemy.progress in 0..51 && (other.p.start + enemy.progress) % 52 == absolute) enemy.progress = -1
            }
        }
    }

    private fun finishTurn() {
        if (state == State.WON) return
        dice = 0
        moving = -1
        target = -1
        turn = (turn + 1) % 4
        state = State.ROLL
        invalidate()
    }

    private fun reset() {
        players.forEach { it.tokens.forEach { token -> token.progress = -1 } }
        turn = 0
        dice = 0
        state = State.ROLL
        winner = -1
        moving = -1
        target = -1
        celebrationAt = 0L
    }
}
