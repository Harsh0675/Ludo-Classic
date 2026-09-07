package com.harshnagar.ludo

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.min
import kotlin.random.Random

class LudoView(context: Context, private val playerNames: List<String>) : View(context) {
    private enum class P(val c: Int, val start: Int) {
        RED(Color.rgb(245, 55, 70), 26), GREEN(Color.rgb(20, 180, 105), 39),
        YELLOW(Color.rgb(250, 190, 25), 0), BLUE(Color.rgb(30, 120, 235), 13)
    }
    private data class Token(var pos: Int = -1)
    private enum class Phase { ROLL, PICK, MOVING, WIN }

    private val active = playerNames.size.coerceIn(2, 4)
    private val players = P.values().map { Array(4) { Token() } }
    private val diceValues = IntArray(4)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
    private var turn = 0
    private var phase = Phase.ROLL
    private var winner = -1
    private var left = 0f
    private var top = 0f
    private var cell = 0f
    private var board = 0f
    private var destination = -1

    private val route = arrayOf(
        intArrayOf(6,0),intArrayOf(6,1),intArrayOf(6,2),intArrayOf(6,3),intArrayOf(6,4),intArrayOf(6,5),
        intArrayOf(5,6),intArrayOf(4,6),intArrayOf(3,6),intArrayOf(2,6),intArrayOf(1,6),intArrayOf(0,6),
        intArrayOf(0,7),intArrayOf(0,8),intArrayOf(1,8),intArrayOf(2,8),intArrayOf(3,8),intArrayOf(4,8),
        intArrayOf(5,8),intArrayOf(6,9),intArrayOf(6,10),intArrayOf(6,11),intArrayOf(6,12),intArrayOf(6,13),
        intArrayOf(6,14),intArrayOf(7,14),intArrayOf(8,14),intArrayOf(8,13),intArrayOf(8,12),intArrayOf(8,11),
        intArrayOf(8,10),intArrayOf(8,9),intArrayOf(9,8),intArrayOf(10,8),intArrayOf(11,8),intArrayOf(12,8),
        intArrayOf(13,8),intArrayOf(14,8),intArrayOf(14,7),intArrayOf(14,6),intArrayOf(13,6),intArrayOf(12,6),
        intArrayOf(11,6),intArrayOf(10,6),intArrayOf(9,6),intArrayOf(8,5),intArrayOf(8,4),intArrayOf(8,3),
        intArrayOf(8,2),intArrayOf(8,1),intArrayOf(8,0),intArrayOf(7,0),intArrayOf(6,0)
    )
    private val safe = setOf(0,8,13,21,26,34,39,47)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.color = Color.rgb(7, 17, 32); c.drawRect(0f, 0f, w, h, paint)
        board = min(w * .94f, h * .69f)
        left = (w - board) / 2f; top = h * .09f; cell = board / 15f
        title(c, "LUDO CLASSIC", w/2f, h*.045f, 22f, Color.WHITE)
        title(c, "PLAY  •  ENJOY  •  TOGETHER", w/2f, h*.073f, 10f, Color.rgb(165,185,215))
        drawBoard(c)
        drawTurnBar(c, w, h)
        if (phase == Phase.WIN) drawWin(c, w, h)
    }

    private fun drawBoard(c: Canvas) {
        paint.color = Color.WHITE
        c.drawRoundRect(left, top, left+board, top+board, 18f, 18f, paint)
        home(c, 0, 0, P.YELLOW, 0)
        home(c, 9, 0, P.BLUE, 1)
        home(c, 0, 9, P.GREEN, 2)
        home(c, 9, 9, P.RED, 3)
        lane(c, P.YELLOW, 6, 1, 1, 5)
        lane(c, P.BLUE, 9, 6, 5, 1)
        lane(c, P.RED, 8, 9, 1, 5)
        lane(c, P.GREEN, 1, 8, 5, 1)
        paint.color = Color.rgb(210,216,225); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f
        for (r in 0..14) for (q in 0..14)
            c.drawRect(left+q*cell, top+r*cell, left+(q+1)*cell, top+(r+1)*cell, paint)
        paint.style = Paint.Style.FILL
        drawCenter(c)
        for (i in 0 until active) drawTokens(c, i)
    }

    private fun home(c: Canvas, col: Int, row: Int, p: P, pi: Int) {
        paint.color = p.c
        c.drawRect(left+col*cell, top+row*cell, left+(col+6)*cell, top+(row+6)*cell, paint)
        // Token tray
        paint.color = Color.WHITE
        c.drawRoundRect(left+(col+.45f)*cell, top+(row+.7f)*cell, left+(col+3.75f)*cell, top+(row+5.3f)*cell, cell*.35f, cell*.35f, paint)
        val s = arrayOf(1.35f to 1.7f, 2.85f to 1.7f, 1.35f to 4.25f, 2.85f to 4.25f)
        for (v in s) { paint.color = p.c; c.drawCircle(left+(col+v.first)*cell, top+(row+v.second)*cell, cell*.27f, paint) }
        // Player-owned dice panel inside the same home
        val dx = left+(col+4.05f)*cell; val dy = top+(row+1.45f)*cell; val dw = 1.5f*cell; val dh = 3.1f*cell
        paint.color = if (pi == turn) Color.WHITE else Color.argb(220,255,255,255)
        c.drawRoundRect(dx, dy, dx+dw, dy+dh, cell*.22f, cell*.22f, paint)
        title(c, playerNames[pi].take(6), dx+dw/2f, dy+15f, 7.5f, p.c)
        drawDice(c, dx+dw/2f, dy+1.55f*cell, diceValues[pi], p.c, pi == turn)
        title(c, if (pi == turn) "TAP" else "DICE", dx+dw/2f, dy+dh-9f, 6.5f, p.c)
    }

    private fun lane(c: Canvas, p: P, col: Int, row: Int, cw: Int, rh: Int) {
        paint.color = p.c; c.drawRect(left+col*cell, top+row*cell, left+(col+cw)*cell, top+(row+rh)*cell, paint)
    }

    private fun drawCenter(c: Canvas) {
        val x=left+7.5f*cell; val y=top+7.5f*cell
        val paths = arrayOf(
            Triple(P.YELLOW, 6, 6), Triple(P.BLUE, 9, 6), Triple(P.RED, 9, 9), Triple(P.GREEN, 6, 9)
        )
        for ((p,xx,yy) in paths) {
            val path=Path(); path.moveTo(x,y); path.lineTo(left+xx*cell,top+yy*cell)
            val ex=if(xx==6) 9 else 6; val ey=if(yy==6) 9 else 6
            path.lineTo(left+ex*cell,top+ey*cell); path.close(); paint.color=p.c; c.drawPath(path,paint)
        }
        paint.color=Color.WHITE; c.drawCircle(x,y,cell*.18f,paint)
    }

    private fun drawTokens(c: Canvas, pi: Int) {
        val p=P.values()[pi]
        for (i in 0..3) {
            val q=position(p,players[pi][i].pos,i)
            paint.color=Color.WHITE; c.drawCircle(q.first,q.second,cell*.31f,paint)
            paint.color=p.c; c.drawCircle(q.first,q.second,cell*.23f,paint)
            if (phase==Phase.PICK && pi==turn && canMove(i,diceValues[turn])) {
                paint.color=Color.WHITE; paint.style=Paint.Style.STROKE; paint.strokeWidth=3f
                c.drawCircle(q.first,q.second,cell*.39f,paint); paint.style=Paint.Style.FILL
            }
        }
    }

    private fun position(p:P,pos:Int,i:Int):Pair<Float,Float>{
        if(pos<0){
            val bx=if(p==P.BLUE||p==P.RED)9f else 0f; val by=if(p==P.RED||p==P.BLUE)9f else 0f
            val s=arrayOf(1.35f to 1.7f,2.85f to 1.7f,1.35f to 4.25f,2.85f to 4.25f)
            return left+(bx+s[i].first)*cell to top+(by+s[i].second)*cell
        }
        if(pos<=51){val q=route[(p.start+pos)%52];return left+(q[1]+.5f)*cell to top+(q[0]+.5f)*cell}
        if(pos==56)return left+7.5f*cell to top+7.5f*cell
        val n=pos-52
        return when(p){
            P.YELLOW->left+6.5f*cell to top+(5.5f-n)*cell
            P.BLUE->left+(8.5f+n)*cell to top+6.5f*cell
            P.RED->left+8.5f*cell to top+(8.5f+n)*cell
            P.GREEN->left+(5.5f-n)*cell to top+8.5f*cell
        }
    }

    private fun drawDice(c:Canvas,x:Float,y:Float,v:Int,dotColor:Int,activeDice:Boolean){
        paint.color=dotColor; val d=8f; val r=2.8f
        if(v !in 1..6){title(c,"•",x,y+5f,18f,dotColor);return}
        fun dot(a:Float,b:Float){c.drawCircle(x+a,y+b,r,paint)}
        if(v%2==0){dot(-d,-d);dot(d,d)}
        if(v>=3){dot(-d,d);dot(d,-d)}
        if(v%2==1)dot(0f,0f)
        if(v==6){dot(-d,0f);dot(d,0f)}
        if(activeDice){paint.color=Color.argb(120,255,255,255);paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;c.drawCircle(x,y,cell*.7f,paint);paint.style=Paint.Style.FILL}
    }

    private fun drawTurnBar(c:Canvas,w:Float,h:Float){
        val y=top+board+10f; paint.color=P.values()[turn].c
        c.drawRoundRect(14f,y,w-14f,y+54f,15f,15f,paint)
        title(c,"${playerNames[turn]}’s Turn",w/2f,y+23f,16f,Color.WHITE)
        title(c,if(phase==Phase.ROLL)"Tap your dice in your home to roll" else if(phase==Phase.PICK)"Choose a highlighted token" else "Moving…",w/2f,y+43f,9f,Color.WHITE)
    }

    private fun drawWin(c:Canvas,w:Float,h:Float){
        paint.color=Color.argb(235,4,10,20);c.drawRect(0f,0f,w,h,paint)
        val l=w*.09f;val r=w*.91f;val t=h*.28f;val b=h*.68f;paint.color=Color.WHITE;c.drawRoundRect(l,t,r,b,24f,24f,paint)
        val p=P.values()[winner];paint.color=p.c;c.drawRect(l,t,r,t+14f,paint)
        title(c,"WINNER",w/2f,t+90f,28f,p.c);title(c,playerNames[winner],w/2f,t+132f,24f,Color.DKGRAY)
        title(c,"All four tokens reached the center!",w/2f,t+165f,11f,Color.DKGRAY)
        paint.color=p.c;c.drawRoundRect(w*.24f,b-50f,w*.76f,b-12f,14f,14f,paint);title(c,"NEW GAME",w/2f,b-25f,13f,Color.WHITE)
    }

    private fun title(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,align:Paint.Align=Paint.Align.CENTER){bold.textSize=size;bold.color=color;bold.textAlign=align;c.drawText(s,x,y,bold)}

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action!=MotionEvent.ACTION_UP)return true
        val x=e.x;val y=e.y
        if(phase==Phase.WIN){reset();invalidate();return true}
        if(phase==Phase.ROLL){
            for(i in 0 until active){
                val col=if(i==0)0 else if(i==1)9 else if(i==2)0 else 9
                val row=if(i==0)0 else if(i==1)0 else if(i==2)9 else 9
                val dx=left+(col+4.05f)*cell; val dy=top+(row+1.45f)*cell
                val dw=1.5f*cell; val dh=3.1f*cell
                if(x>=dx&&x<=dx+dw&&y>=dy&&y<=dy+dh){
                    if(i==turn) roll() else Toast.makeText(context,"It’s ${playerNames[turn]}’s turn",Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }
        if(phase==Phase.PICK)for(i in 0..3){val q=position(P.values()[turn],players[turn][i].pos,i);if((x-q.first)*(x-q.first)+(y-q.second)*(y-q.second)<cell*cell&&canMove(i,diceValues[turn])){move(turn,i);return true}}
        return true
    }

    private fun roll(){
        diceValues[turn]=Random.nextInt(1,7)
        if((0..3).none{canMove(it,diceValues[turn])})postDelayed({nextTurn()},600) else {phase=Phase.PICK;invalidate()}
    }
    private fun canMove(i:Int,d:Int):Boolean{
        val t=players[turn][i];if(d !in 1..6||t.pos==56)return false
        if(t.pos<0)return d==6;if(t.pos+d>56)return false
        if(t.pos<=51&&t.pos+d<=51){val a=(P.values()[turn].start+t.pos+d)%52;for(j in 0 until active)if(j!=turn&&players[j].count{it.pos in 0..51&&(P.values()[j].start+it.pos)%52==a}>=2)return false}
        return true
    }
    private fun move(pi:Int,ti:Int){destination=if(players[pi][ti].pos<0)0 else players[pi][ti].pos+diceValues[pi];phase=Phase.MOVING;step(pi,ti)}
    private fun step(pi:Int,ti:Int){if(phase!=Phase.MOVING)return;val t=players[pi][ti];t.pos=if(t.pos<0)0 else t.pos+1;invalidate();if(t.pos<destination)postDelayed({step(pi,ti)},45)else postDelayed({complete(pi,ti)},100)}
    private fun complete(pi:Int,ti:Int){capture(pi,players[pi][ti]);if(players[pi].all{it.pos==56}){winner=pi;phase=Phase.WIN;invalidate();return};val extra=diceValues[pi]==6;diceValues[pi]=0;if(extra){phase=Phase.ROLL;invalidate()}else nextTurn()}
    private fun capture(pi:Int,t:Token){if(t.pos !in 0..51)return;val p=P.values()[pi];val a=(p.start+t.pos)%52;if(a in safe)return;for(j in 0 until active)if(j!=pi)for(e in players[j])if(e.pos in 0..51&&(P.values()[j].start+e.pos)%52==a)e.pos=-1}
    private fun nextTurn(){diceValues[turn]=0;destination=-1;turn=(turn+1)%active;phase=Phase.ROLL;invalidate()}
    private fun reset(){for(j in 0 until active)for(t in players[j])t.pos=-1;for(i in diceValues.indices)diceValues[i]=0;turn=0;winner=-1;destination=-1;phase=Phase.ROLL}
}
