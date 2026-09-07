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
        GREEN(Color.rgb(20, 156, 91), 39, "You"),
        YELLOW(Color.rgb(250, 205, 20), 0, "Computer 2"),
        BLUE(Color.rgb(25, 139, 224), 13, "Computer 3"),
        RED(Color.rgb(239, 48, 48), 26, "Computer 4")
    }

    private data class Token(var progress: Int = -1)
    private data class Player(val color: PlayerColor, val tokens: MutableList<Token> = MutableList(4) { Token() })

    private val players = PlayerColor.values().map { Player(it) }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans", Typeface.BOLD) }

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

    private val route = arrayOf(
        intArrayOf(6,1), intArrayOf(6,2), intArrayOf(6,3), intArrayOf(6,4), intArrayOf(6,5),
        intArrayOf(5,6), intArrayOf(4,6), intArrayOf(3,6), intArrayOf(2,6), intArrayOf(1,6), intArrayOf(0,6),
        intArrayOf(0,7), intArrayOf(0,8), intArrayOf(1,8), intArrayOf(2,8), intArrayOf(3,8), intArrayOf(4,8), intArrayOf(5,8),
        intArrayOf(6,9), intArrayOf(6,10), intArrayOf(6,11), intArrayOf(6,12), intArrayOf(6,13), intArrayOf(6,14),
        intArrayOf(7,14), intArrayOf(8,14), intArrayOf(8,13), intArrayOf(8,12), intArrayOf(8,11), intArrayOf(8,10), intArrayOf(8,9),
        intArrayOf(9,8), intArrayOf(10,8), intArrayOf(11,8), intArrayOf(12,8), intArrayOf(13,8), intArrayOf(14,8),
        intArrayOf(14,7), intArrayOf(14,6), intArrayOf(13,6), intArrayOf(12,6), intArrayOf(11,6), intArrayOf(10,6), intArrayOf(9,6),
        intArrayOf(8,5), intArrayOf(8,4), intArrayOf(8,3), intArrayOf(8,2), intArrayOf(8,1), intArrayOf(8,0), intArrayOf(7,0), intArrayOf(6,0)
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
        boardSize = min(w * .94f, h * .60f)
        boardLeft = (w - boardSize) / 2f
        boardTop = h * .13f
        cell = boardSize / 15f
        drawBackground(c, w, h)
        drawPanels(c, w)
        drawBoard(c)
        drawControls(c, w, h)
        if (gameOver) drawGameOver(c, w, h)
    }

    private fun drawBackground(c: Canvas, w: Float, h: Float) {
        paint.shader = LinearGradient(0f, 0f, 0f, h, Color.rgb(8,66,125), Color.rgb(2,24,55), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        text.textAlign = Paint.Align.CENTER
        text.textSize = w * .045f
        text.color = Color.WHITE
        c.drawText("HARSH NAGAR  •  LUDO", w / 2f, h * .055f, text)
    }

    private fun drawPanels(c: Canvas, w: Float) {
        drawPanel(c, 16f, boardTop - 66f, w * .43f, players[1])
        drawPanel(c, w * .57f, boardTop - 66f, w - 16f, players[2])
    }

    private fun drawPanel(c: Canvas, l: Float, t: Float, r: Float, p: Player) {
        paint.color = p.color.main
        c.drawRoundRect(l, t, r, t + 52f, 16f, 16f, paint)
        paint.color = Color.WHITE
        c.drawCircle(l + 24f, t + 26f, 13f, paint)
        paint.color = p.color.main
        c.drawCircle(l + 24f, t + 26f, 8f, paint)
        text.textAlign = Paint.Align.LEFT
        text.textSize = 16f
        text.color = Color.WHITE
        c.drawText(p.color.title, l + 46f, t + 32f, text)
    }

    private fun drawBoard(c: Canvas) {
        paint.color = Color.WHITE
        c.drawRect(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize, paint)
        drawHome(c, 0, 0, PlayerColor.YELLOW)
        drawHome(c, 9, 0, PlayerColor.BLUE)
        drawHome(c, 0, 9, PlayerColor.GREEN)
        drawHome(c, 9, 9, PlayerColor.RED)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(180,180,180)
        for (r in 0..14) for (col in 0..14) {
            val x = boardLeft + col * cell
            val y = boardTop + r * cell
            c.drawRect(x, y, x + cell, y + cell, paint)
        }
        paint.style = Paint.Style.FILL
        drawLane(c, PlayerColor.YELLOW, 6, 1, 1, 5)
        drawLane(c, PlayerColor.BLUE, 9, 6, 5, 1)
        drawLane(c, PlayerColor.RED, 8, 9, 1, 5)
        drawLane(c, PlayerColor.GREEN, 1, 8, 5, 1)
        val cx = boardLeft + 7.5f * cell
        val cy = boardTop + 7.5f * cell
        triangle(c, cx, cy, 6f,6f,9f,6f,PlayerColor.YELLOW.main)
        triangle(c, cx, cy, 9f,6f,9f,9f,PlayerColor.RED.main)
        triangle(c, cx, cy, 9f,9f,6f,9f,PlayerColor.GREEN.main)
        triangle(c, cx, cy, 6f,9f,6f,6f,PlayerColor.BLUE.main)
        for (i in safeSquares) {
            val q = route[i]
            drawStar(c, boardLeft + (q[1] + .5f) * cell, boardTop + (q[0] + .5f) * cell, cell * .22f)
        }
        drawTokens(c)
    }

    private fun drawHome(c: Canvas, col: Int, row: Int, color: PlayerColor) {
        paint.color = color.main
        c.drawRect(boardLeft + col*cell, boardTop + row*cell, boardLeft + (col+6)*cell, boardTop + (row+6)*cell, paint)
        paint.color = Color.WHITE
        c.drawRoundRect(boardLeft + (col+1)*cell, boardTop + (row+1)*cell, boardLeft + (col+5)*cell, boardTop + (row+5)*cell, cell*.28f, cell*.28f, paint)
        val spots = arrayOf(1.7f to 1.7f, 4.3f to 1.7f, 1.7f to 4.3f, 4.3f to 4.3f)
        paint.color = color.main
        for ((x,y) in spots) c.drawCircle(boardLeft + (col+x)*cell, boardTop + (row+y)*cell, cell*.34f, paint)
    }

    private fun drawLane(c: Canvas, color: PlayerColor, col: Int, row: Int, wc: Int, hc: Int) {
        paint.color = color.main
        c.drawRect(boardLeft + col*cell, boardTop + row*cell, boardLeft + (col+wc)*cell, boardTop + (row+hc)*cell, paint)
        paint.color = Color.argb(55,255,255,255)
        for (r in 0 until hc) for (x in 0 until wc) c.drawRect(boardLeft + (col+x)*cell, boardTop + (row+r)*cell, boardLeft + (col+x+1)*cell, boardTop + (row+r+1)*cell, paint)
    }

    private fun triangle(c: Canvas, cx: Float, cy: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val p = Path()
        p.moveTo(cx,cy); p.lineTo(boardLeft+x1*cell,boardTop+y1*cell); p.lineTo(boardLeft+x2*cell,boardTop+y2*cell); p.close()
        paint.color = color
        c.drawPath(p, paint)
    }

    private fun drawStar(c: Canvas, x: Float, y: Float, r: Float) {
        val p = Path()
        for (i in 0..9) {
            val a = -Math.PI/2 + i*Math.PI/5
            val rr = if (i%2==0) r else r*.42f
            val px = x + cos(a).toFloat()*rr
            val py = y + sin(a).toFloat()*rr
            if (i==0) p.moveTo(px,py) else p.lineTo(px,py)
        }
        p.close(); paint.color = Color.rgb(115,115,115); paint.style = Paint.Style.STROKE; paint.strokeWidth=2f; c.drawPath(p,paint); paint.style=Paint.Style.FILL
    }

    private fun drawTokens(c: Canvas) {
        for (player in players) for (i in player.tokens.indices) {
            val point = tokenPoint(player.color, player.tokens[i].progress, i) ?: continue
            drawToken(c, point.first, point.second, player.color, i)
        }
    }

    private fun tokenPoint(color: PlayerColor, progress: Int, index: Int): Pair<Float,Float>? {
        if (progress < 0) {
            val base = when(color) {
                PlayerColor.YELLOW -> 0 to 0
                PlayerColor.BLUE -> 9 to 0
                PlayerColor.GREEN -> 0 to 9
                PlayerColor.RED -> 9 to 9
            }
            val spots = arrayOf(1.7f to 1.7f,4.3f to 1.7f,1.7f to 4.3f,4.3f to 4.3f)
            val s = spots[index]
            return boardLeft+(base.first+s.first)*cell to boardTop+(base.second+s.second)*cell
        }
        if (progress <= 51) {
            val q = route[(color.start+progress)%52]
            return boardLeft+(q[1]+.5f)*cell to boardTop+(q[0]+.5f)*cell
        }
        val n = progress-52
        return when(color) {
            PlayerColor.YELLOW -> boardLeft+6.5f*cell to boardTop+(5.5f-n)*cell
            PlayerColor.BLUE -> boardLeft+(8.5f+n)*cell to boardTop+6.5f*cell
            PlayerColor.RED -> boardLeft+8.5f*cell to boardTop+(8.5f+n)*cell
            PlayerColor.GREEN -> boardLeft+(5.5f-n)*cell to boardTop+8.5f*cell
        }
    }

    private fun drawToken(c: Canvas, x: Float, y: Float, color: PlayerColor, index: Int) {
        paint.color = Color.argb(70,0,0,0); c.drawCircle(x+2f,y+4f,cell*.34f,paint)
        paint.color = Color.WHITE; c.drawCircle(x,y,cell*.31f,paint)
        paint.color = color.main; c.drawCircle(x,y,cell*.245f,paint)
        paint.color = Color.WHITE; c.drawCircle(x,y-cell*.08f,cell*.07f,paint)
        if (turn==0 && color==PlayerColor.GREEN && rolled && canMove(index,dice)) {
            paint.color=Color.WHITE; paint.style=Paint.Style.STROKE; paint.strokeWidth=3f; c.drawCircle(x,y,cell*.39f,paint); paint.style=Paint.Style.FILL
        }
    }

    private fun drawControls(c: Canvas, w: Float, h: Float) {
        val y=h*.77f; val ph=76f
        paint.color=PlayerColor.GREEN.main; c.drawRoundRect(16f,y,w*.42f,y+ph,18f,18f,paint)
        text.textAlign=Paint.Align.LEFT; text.textSize=18f; text.color=Color.WHITE; c.drawText("YOU",32f,y+28f,text)
        text.textSize=13f; c.drawText("Turn: ${players[turn].color.title}",32f,y+53f,text)
        paint.color=Color.WHITE; c.drawRoundRect(w*.45f,y,w*.68f,y+ph,18f,18f,paint); drawDice(c,w*.565f,y+ph/2f,dice)
        paint.color=Color.rgb(22,112,203); c.drawRoundRect(w*.71f,y,w-16f,y+ph,18f,18f,paint)
        text.textAlign=Paint.Align.CENTER; text.textSize=15f; text.color=Color.WHITE; c.drawText(if(rolled)"SELECT TOKEN" else "ROLL DICE",w*.855f,y+45f,text)
        if(!gameOver){ text.textSize=13f; c.drawText(message,w/2f,y+101f,text) }
    }

    private fun drawDice(c: Canvas, x: Float, y: Float, value: Int) {
        paint.color=Color.WHITE; c.drawRoundRect(x-25f,y-25f,x+25f,y+25f,10f,10f,paint); if(value==0)return
        paint.color=Color.DKGRAY; val d=12f
        val dots=when(value){1->arrayOf(0 to 0);2->arrayOf(-1 to -1,1 to 1);3->arrayOf(-1 to -1,0 to 0,1 to 1);4->arrayOf(-1 to -1,1 to -1,-1 to 1,1 to 1);5->arrayOf(-1 to -1,1 to -1,0 to 0,-1 to 1,1 to 1);else->arrayOf(-1 to -1,1 to -1,-1 to 0,1 to 0,-1 to 1,1 to 1)}
        for((dx,dy) in dots)c.drawCircle(x+dx*d,y+dy*d,4f,paint)
    }

    private fun drawGameOver(c: Canvas,w: Float,h: Float){
        paint.color=Color.argb(210,0,0,0); c.drawRect(0f,0f,w,h,paint); text.textAlign=Paint.Align.CENTER; text.color=Color.WHITE; text.textSize=34f
        c.drawText("${players[winner].color.title.uppercase()} WINS!",w/2f,h*.45f,text); text.textSize=18f; c.drawText("Tap anywhere to play again",w/2f,h*.51f,text)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(e.action!=MotionEvent.ACTION_UP)return true
        if(gameOver){reset();invalidate();return true}
        if(turn!=0)return true
        val x=e.x; val y=e.y; val top=height*.77f
        if(!rolled && x in width*.45f..width*.68f && y in top..(top+90f)){rollDice();return true}
        if(rolled)for(i in 0..3){val point=tokenPoint(PlayerColor.GREEN,players[0].tokens[i].progress,i)?:continue;val dx=x-point.first;val dy=y-point.second;if(dx*dx+dy*dy<=cell*cell*.75f&&canMove(i,dice)){moveToken(0,i);return true}}
        return true
    }

    private fun rollDice(){
        if(gameOver||rolled||turn!=0)return
        dice=Random.nextInt(1,7); rolled=true
        if(legalMoves().isEmpty()){
            message="No legal move"
            postDelayed({if(!gameOver&&turn==0&&rolled)finishTurn()},650L)
        }else message=if(dice==6)"Six! Choose a token" else "Choose a highlighted token"
        invalidate()
    }

    private fun legalMoves()=(0..3).filter{canMove(it,dice)}

    private fun canMove(index:Int,die:Int):Boolean{
        if(die !in 1..6)return false
        val token=players[turn].tokens[index]
        if(token.progress<0)return die==6
        if(token.progress+die>56)return false
        return !blockedByOpponent(players[turn].color,token.progress,die)
    }

    private fun blockedByOpponent(color:PlayerColor,progress:Int,die:Int):Boolean{
        if(progress<0||progress>51||progress+die>51)return false
        val target=(color.start+progress+die)%52
        return players.any{other->other.color!=color&&other.tokens.count{it.progress in 0..51&&(other.color.start+it.progress)%52==target}>=2}
    }

    private fun moveToken(playerIndex:Int,tokenIndex:Int){
        if(gameOver)return
        val player=players[playerIndex]; val die=dice
        if(!canMove(tokenIndex,die))return
        val token=player.tokens[tokenIndex]
        token.progress=if(token.progress<0)0 else token.progress+die
        captureOpponents(player,token)
        if(player.tokens.all{it.progress==56}){winner=playerIndex;gameOver=true;rolled=false;dice=0;message="Game over";invalidate();return}
        if(die==6){rolled=false;dice=0;message="Six! Roll again";invalidate();if(playerIndex!=0)postDelayed({computerTurn()},500L);return}
        finishTurn()
    }

    private fun captureOpponents(player:Player,movingToken:Token){
        if(movingToken.progress !in 0..51)return
        val target=(player.color.start+movingToken.progress)%52
        if(target in safeSquares)return
        for(other in players)if(other.color!=player.color)for(token in other.tokens)if(token.progress in 0..51&&(other.color.start+token.progress)%52==target)token.progress=-1
    }

    private fun finishTurn(){
        rolled=false;dice=0;turn=(turn+1)%players.size;message=if(turn==0)"Tap ROLL DICE to start" else "${players[turn].color.title} is thinking...";invalidate()
        if(turn!=0&&!gameOver)postDelayed({computerTurn()},450L)
    }

    private fun computerTurn(){
        if(gameOver||turn==0||rolled)return
        dice=Random.nextInt(1,7);rolled=true
        val legal=legalMoves()
        if(legal.isEmpty()){rolled=false;dice=0;postDelayed({finishTurn()},550L);return}
        val player=players[turn];val chosen=legal.maxByOrNull{scoreMove(player,it)}?:legal.first();message="${player.color.title} rolled $dice";invalidate()
        postDelayed({if(!gameOver&&turn!=0&&rolled)moveToken(turn,chosen)},600L)
    }

    private fun scoreMove(player:Player,index:Int):Int{
        val token=player.tokens[index];var score=if(token.progress<0)1000 else token.progress*4
        if(token.progress>=0&&token.progress+dice==56)score+=2000
        if(token.progress<0&&dice==6)score+=500
        if(token.progress in 0..51&&token.progress+dice<=51){val target=(player.color.start+token.progress+dice)%52;if(target !in safeSquares)for(other in players)if(other.color!=player.color&&other.tokens.any{it.progress in 0..51&&(other.color.start+it.progress)%52==target})score+=900}
        return score
    }

    private fun reset(){players.forEach{it.tokens.forEach{token->token.progress=-1}};turn=0;dice=0;rolled=false;gameOver=false;winner=0;message="Tap ROLL DICE to start"}
}
