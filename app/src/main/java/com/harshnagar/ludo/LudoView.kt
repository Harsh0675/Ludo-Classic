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

class LudoView(context: Context, private val playerNames: List<String>) : View(context) {
    private enum class PlayerColor(val main: Int, val start: Int) {
        GREEN(Color.rgb(16,170,94),39), YELLOW(Color.rgb(255,198,20),0),
        BLUE(Color.rgb(24,143,224),13), RED(Color.rgb(242,48,55),26)
    }
    private enum class State { WAIT_ROLL, SELECT_TOKEN, MOVING, GAME_OVER }
    private data class Token(var progress:Int=-1)
    private data class Player(val color:PlayerColor,val tokens:MutableList<Token> = MutableList(4){Token()})

    private val players=PlayerColor.values().map{Player(it)}
    private val names=(0..3).map{i -> playerNames.getOrNull(i)?.takeIf{it.isNotBlank()} ?: "Player ${i+1}"}
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{typeface=Typeface.create("sans",Typeface.BOLD)}
    private var turn=0; private var dice=0; private var state=State.WAIT_ROLL; private var winner=-1
    private var message="${names[0]} • Roll the dice"; private var boardLeft=0f; private var boardTop=0f
    private var boardSize=0f; private var cell=0f; private var controlsTop=0f; private var movingToken=-1; private var moveTarget=-1
    private var celebrationStart=0L

    private val route=arrayOf(
        intArrayOf(6,1),intArrayOf(6,2),intArrayOf(6,3),intArrayOf(6,4),intArrayOf(6,5),intArrayOf(5,6),intArrayOf(4,6),intArrayOf(3,6),intArrayOf(2,6),intArrayOf(1,6),intArrayOf(0,6),
        intArrayOf(0,7),intArrayOf(0,8),intArrayOf(1,8),intArrayOf(2,8),intArrayOf(3,8),intArrayOf(4,8),intArrayOf(5,8),intArrayOf(6,9),intArrayOf(6,10),intArrayOf(6,11),intArrayOf(6,12),intArrayOf(6,13),intArrayOf(6,14),
        intArrayOf(7,14),intArrayOf(8,14),intArrayOf(8,13),intArrayOf(8,12),intArrayOf(8,11),intArrayOf(8,10),intArrayOf(8,9),intArrayOf(9,8),intArrayOf(10,8),intArrayOf(11,8),intArrayOf(12,8),intArrayOf(13,8),intArrayOf(14,8),
        intArrayOf(14,7),intArrayOf(14,6),intArrayOf(13,6),intArrayOf(12,6),intArrayOf(11,6),intArrayOf(10,6),intArrayOf(9,6),intArrayOf(8,5),intArrayOf(8,4),intArrayOf(8,3),intArrayOf(8,2),intArrayOf(8,1),intArrayOf(8,0),intArrayOf(7,0),intArrayOf(6,0))
    private val safe=setOf(0,8,13,21,26,34,39,47)

    init{setLayerType(View.LAYER_TYPE_SOFTWARE,null);reset()}

    override fun onDraw(c:Canvas){
        val w=width.toFloat(); val h=height.toFloat(); boardSize=min(w*.94f,h*.58f); boardLeft=(w-boardSize)/2f; boardTop=h*.125f; cell=boardSize/15f; controlsTop=min(boardTop+boardSize+26f,h-190f)
        drawBackground(c,w,h); drawHeader(c,w); drawPanels(c,w); drawBoard(c); drawControls(c,w,h); if(state==State.GAME_OVER) drawCelebration(c,w,h)
        if(state==State.GAME_OVER){postInvalidateDelayed(35L)}
    }
    private fun drawBackground(c:Canvas,w:Float,h:Float){paint.shader=LinearGradient(0f,0f,0f,h,Color.rgb(7,70,132),Color.rgb(2,25,58),Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,paint);paint.shader=null}
    private fun drawHeader(c:Canvas,w:Float){text.textAlign=Paint.Align.CENTER;text.textSize=w*.045f;text.color=Color.WHITE;c.drawText("LUDO CLASSIC",w/2f,w*.07f,text);text.textSize=w*.025f;text.color=Color.rgb(180,215,245);c.drawText("HARSH NAGAR • 4 PLAYER OFFLINE",w/2f,w*.105f,text)}
    private fun drawPanels(c:Canvas,w:Float){val top=boardTop-48f;drawPanel(c,12f,top,w*.47f,0);drawPanel(c,w*.53f,top,w-12f,1)}
    private fun drawPanel(c:Canvas,l:Float,t:Float,r:Float,i:Int){val p=players[i];paint.color=Color.argb(55,0,0,0);c.drawRoundRect(l,t+4,r,t+52,16f,16f,paint);paint.color=p.color.main;c.drawRoundRect(l,t,r,t+48,16f,16f,paint);paint.color=Color.WHITE;c.drawCircle(l+22,t+24,12f,paint);paint.color=p.color.main;c.drawCircle(l+22,t+24,8f,paint);text.textAlign=Paint.Align.LEFT;text.textSize=12f;text.color=Color.WHITE;c.drawText(names[i],l+40,t+29,text)}

    private fun drawBoard(c:Canvas){paint.color=Color.argb(55,0,0,0);c.drawRoundRect(boardLeft+4,boardTop+6,boardLeft+boardSize+4,boardTop+boardSize+6,10f,10f,paint);paint.color=Color.WHITE;c.drawRect(boardLeft,boardTop,boardLeft+boardSize,boardTop+boardSize,paint)
        drawHome(c,0,0,PlayerColor.YELLOW);drawHome(c,9,0,PlayerColor.BLUE);drawHome(c,0,9,PlayerColor.GREEN);drawHome(c,9,9,PlayerColor.RED)
        drawLane(c,PlayerColor.YELLOW,6,1,1,5);drawLane(c,PlayerColor.BLUE,9,6,5,1);drawLane(c,PlayerColor.RED,8,9,1,5);drawLane(c,PlayerColor.GREEN,1,8,5,1)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=maxOf(1f,cell*.012f);paint.color=Color.rgb(185,190,198);for(r in 0..14)for(col in 0..14)c.drawRect(boardLeft+col*cell,boardTop+r*cell,boardLeft+(col+1)*cell,boardTop+(r+1)*cell,paint);paint.style=Paint.Style.FILL
        val cx=boardLeft+7.5f*cell;val cy=boardTop+7.5f*cell;triangle(c,cx,cy,6f,6f,9f,6f,PlayerColor.YELLOW.main);triangle(c,cx,cy,9f,6f,9f,9f,PlayerColor.RED.main);triangle(c,cx,cy,9f,9f,6f,9f,PlayerColor.GREEN.main);triangle(c,cx,cy,6f,9f,6f,6f,PlayerColor.BLUE.main)
        for(i in safe){val q=route[i];drawStar(c,boardLeft+(q[1]+.5f)*cell,boardTop+(q[0]+.5f)*cell,cell*.18f)};drawTokens(c)
    }
    private fun drawHome(c:Canvas,col:Int,row:Int,color:PlayerColor){paint.color=color.main;c.drawRect(boardLeft+col*cell,boardTop+row*cell,boardLeft+(col+6)*cell,boardTop+(row+6)*cell,paint);paint.color=Color.WHITE;c.drawRoundRect(boardLeft+(col+.95f)*cell,boardTop+(row+.95f)*cell,boardLeft+(col+5.05f)*cell,boardTop+(row+5.05f)*cell,cell*.28f,cell*.28f,paint)}
    private fun drawLane(c:Canvas,color:PlayerColor,col:Int,row:Int,w:Int,h:Int){paint.color=color.main;c.drawRect(boardLeft+col*cell,boardTop+row*cell,boardLeft+(col+w)*cell,boardTop+(row+h)*cell,paint)}
    private fun triangle(c:Canvas,cx:Float,cy:Float,x1:Float,y1:Float,x2:Float,y2:Float,color:Int){val p=Path();p.moveTo(cx,cy);p.lineTo(boardLeft+x1*cell,boardTop+y1*cell);p.lineTo(boardLeft+x2*cell,boardTop+y2*cell);p.close();paint.color=color;c.drawPath(p,paint)}
    private fun drawStar(c:Canvas,x:Float,y:Float,r:Float){val p=Path();for(i in 0..9){val a=-Math.PI/2+i*Math.PI/5;val rr=if(i%2==0)r:r*.42f;val px=x+cos(a).toFloat()*rr;val py=y+sin(a).toFloat()*rr;if(i==0)p.moveTo(px,py)else p.lineTo(px,py)};p.close();paint.color=Color.rgb(105,110,118);paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;c.drawPath(p,paint);paint.style=Paint.Style.FILL}

    private fun drawTokens(c:Canvas){for(pi in players.indices)for(ti in 0..3){val q=tokenPoint(pi,players[pi].tokens[ti].progress,ti)?:continue;drawToken(c,q.first,q.second,pi,ti)}}
    private fun tokenPoint(pi:Int,progress:Int,index:Int):Pair<Float,Float>?{val color=players[pi].color;if(progress<0){val base=when(color){PlayerColor.YELLOW->0 to 0;PlayerColor.BLUE->9 to 0;PlayerColor.GREEN->0 to 9;PlayerColor.RED->9 to 9};val spots=arrayOf(1.7f to 1.7f,4.3f to 1.7f,1.7f to 4.3f,4.3f to 4.3f);val s=spots[index];return boardLeft+(base.first+s.first)*cell to boardTop+(base.second+s.second)*cell};if(progress<=51){val q=route[(color.start+progress)%52];return boardLeft+(q[1]+.5f)*cell to boardTop+(q[0]+.5f)*cell};val n=progress-52;return when(color){PlayerColor.YELLOW->boardLeft+6.5f*cell to boardTop+(5.5f-n)*cell;PlayerColor.BLUE->boardLeft+(8.5f+n)*cell to boardTop+6.5f*cell;PlayerColor.RED->boardLeft+8.5f*cell to boardTop+(8.5f+n)*cell;PlayerColor.GREEN->boardLeft+(5.5f-n)*cell to boardTop+8.5f*cell}}
    private fun drawToken(c:Canvas,x:Float,y:Float,pi:Int,index:Int){val p=players[pi];val selectable=state==State.SELECT_TOKEN&&turn==pi&&canMove(index,dice);paint.color=Color.argb(65,0,0,0);c.drawCircle(x+2,y+4,cell*.35f,paint);paint.color=Color.WHITE;c.drawCircle(x,y,cell*.31f,paint);paint.color=p.color.main;c.drawCircle(x,y,cell*.245f,paint);paint.color=Color.WHITE;c.drawCircle(x,y-cell*.08f,cell*.065f,paint);if(selectable||state==State.MOVING&&turn==pi&&index==movingToken){paint.style=Paint.Style.STROKE;paint.strokeWidth=maxOf(2f,cell*.035f);paint.color=Color.WHITE;c.drawCircle(x,y,cell*.4f,paint);paint.style=Paint.Style.FILL}}

    private fun drawControls(c:Canvas,w:Float,h:Float){val ph=min(74f,h*.065f);val y=controlsTop;val left=14f;val right=w-14f;val dl=w*.43f;val br=w*.70f;paint.color=players[turn].color.main;c.drawRoundRect(left,y,w*.39f,y+ph,16f,16f,paint);text.textAlign=Paint.Align.LEFT;text.textSize=12f;text.color=Color.WHITE;c.drawText(names[turn],left+16,y+25,text);text.textSize=10f;c.drawText(if(state==State.WAIT_ROLL)"Your turn" else "Choose a token",left+16,y+48,text);paint.color=Color.WHITE;c.drawRoundRect(dl,y,w*.67f,y+ph,16f,16f,paint);drawDice(c,(dl+w*.67f)/2,y+ph/2,dice);paint.color=if(state==State.WAIT_ROLL)Color.rgb(17,119,214)else Color.rgb(95,113,130);c.drawRoundRect(br,y,right,y+ph,16f,16f,paint);text.textAlign=Paint.Align.CENTER;text.textSize=12f;c.drawText(if(state==State.WAIT_ROLL)"ROLL DICE" else if(state==State.GAME_OVER)"PLAY AGAIN" else "SELECT TOKEN",(br+right)/2,y+ph*.61f,text);if(state!=State.GAME_OVER){text.textSize=11f;c.drawText(message,w/2,y+ph+27,text)}}
    private fun drawDice(c:Canvas,x:Float,y:Float,v:Int){val s=min(cell*.72f,50f);paint.color=Color.WHITE;c.drawRoundRect(x-s/2,y-s/2,x+s/2,y+s/2,10f,10f,paint);if(v !in 1..6)return;paint.color=Color.rgb(40,50,60);val d=s*.25f;val r=s*.075f;val dots=when(v){1->arrayOf(0 to 0);2->arrayOf(-1 to -1,1 to 1);3->arrayOf(-1 to -1,0 to 0,1 to 1);4->arrayOf(-1 to -1,1 to -1,-1 to 1,1 to 1);5->arrayOf(-1 to -1,1 to -1,0 to 0,-1 to 1,1 to 1);else->arrayOf(-1 to -1,1 to -1,-1 to 0,1 to 0,-1 to 1,1 to 1)};for((dx,dy)in dots)c.drawCircle(x+dx*d,y+dy*d,r,paint)}

    private fun drawCelebration(c:Canvas,w:Float,h:Float){paint.color=Color.argb(215,0,5,20);c.drawRect(0f,0f,w,h,paint);val elapsed=(System.currentTimeMillis()-celebrationStart)/1000f
        for(i in 0 until 42){val seed=i*37;val x=((seed*13)%1000)/1000f*w;val fall=((elapsed*(35+(i%5)*12)+seed)%1400)/1000f*h;val y=(fall-.15f*h)%h;paint.color=when(i%4){0->Color.rgb(255,198,20);1->Color.rgb(16,170,94);2->Color.rgb(24,143,224);else->Color.rgb(242,48,55)};c.save();c.rotate(((i*29+elapsed*80)%360),x,y);c.drawRect(x-3,y-7,x+3,y+7,paint);c.restore()}
        val l=w*.08f;val r=w*.92f;val t=h*.23f;val b=h*.72f;paint.color=Color.WHITE;c.drawRoundRect(l,t,r,b,30f,30f,paint);paint.color=players[winner].color.main;c.drawRoundRect(l,t,r,t+20,30f,30f,paint)
        paint.color=Color.rgb(255,196,25);val cx=w/2;val crownY=t+76;val cp=Path();cp.moveTo(cx-58,crownY+10);cp.lineTo(cx-45,crownY-25);cp.lineTo(cx-18,crownY-2);cp.lineTo(cx,crownY-37);cp.lineTo(cx+18,crownY-2);cp.lineTo(cx+45,crownY-25);cp.lineTo(cx+58,crownY+10);cp.close();c.drawPath(cp,paint);c.drawRoundRect(cx-58,crownY+5,cx+58,crownY+20,5f,5f,paint)
        text.textAlign=Paint.Align.CENTER;text.textSize=min(w*.075f,38f);text.color=players[winner].color.main;c.drawText("WINNER!",cx,t+170,text);text.textSize=min(w*.06f,31f);text.color=Color.rgb(35,45,55);c.drawText(names[winner],cx,t+212,text);text.textSize=15f;text.color=Color.DKGRAY;c.drawText("What a game! 🎉",cx,t+242,text)
        paint.color=players[winner].color.main;c.drawCircle(cx,b-82,34f,paint);paint.color=Color.WHITE;c.drawCircle(cx,b-91,8f,paint);c.drawRoundRect(cx-15,b-84,cx+15,b-57,8f,8f,paint)
        paint.color=Color.rgb(17,119,214);c.drawRoundRect(w*.22f,b-48,w*.78f,b-2,16f,16f,paint);text.textSize=14f;text.color=Color.WHITE;c.drawText("PLAY AGAIN",cx,b-19,text)
    }

    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action!=MotionEvent.ACTION_UP)return true;val x=e.x;val y=e.y;if(state==State.GAME_OVER){if(y>height*.65f){reset();invalidate()}return true};if(turn<0||state==State.MOVING)return true;val dl=width*.43f;val dr=width*.67f;if(state==State.WAIT_ROLL&&x in dl..dr&&y in controlsTop..controlsTop+100){rollDice();return true};if(state==State.SELECT_TOKEN){for(i in 0..3){val q=tokenPoint(turn,players[turn].tokens[i].progress,i)?:continue;val dx=x-q.first;val dy=y-q.second;if(dx*dx+dy*dy<=cell*cell*.95f&&canMove(i,dice)){startMove(turn,i);return true}}};return true}
    private fun rollDice(){if(state!=State.WAIT_ROLL)return;dice=Random.nextInt(1,7);val legal=legalMoves();if(legal.isEmpty()){message="No legal move • Turn passes";state=State.SELECT_TOKEN;invalidate();postDelayed({if(state==State.SELECT_TOKEN)finishTurn()},700)}else{state=State.SELECT_TOKEN;message=if(dice==6)"You rolled 6 • Choose a token" else "Choose a highlighted token";invalidate()}}
    private fun legalMoves()=(0..3).filter{canMove(it,dice)}
    private fun canMove(i:Int,d:Int):Boolean{if(d !in 1..6)return false;val t=players[turn].tokens[i];if(t.progress==56)return false;if(t.progress<0)return d==6;if(t.progress+d>56)return false;return !blocked(players[turn].color,t.progress,d)}
    private fun blocked(color:PlayerColor,progress:Int,d:Int):Boolean{if(progress !in 0..51||progress+d>51)return false;val target=(color.start+progress+d)%52;return players.any{p->p.color!=color&&p.tokens.count{it.progress in 0..51&&(p.color.start+it.progress)%52==target}>=2}}
    private fun startMove(pi:Int,ti:Int){if(state!=State.SELECT_TOKEN||pi!=turn||!canMove(ti,dice))return;movingToken=ti;moveTarget=if(players[pi].tokens[ti].progress<0)0 else players[pi].tokens[ti].progress+dice;state=State.MOVING;message="Moving…";animateStep(pi,ti)}
    private fun animateStep(pi:Int,ti:Int){if(state!=State.MOVING||pi!=turn)return;val t=players[pi].tokens[ti];t.progress=if(t.progress<0)0 else t.progress+1;invalidate();if(t.progress>=moveTarget)postDelayed({completeMove(pi,ti)},85)else postDelayed({animateStep(pi,ti)},95)}
    private fun completeMove(pi:Int,ti:Int){if(state!=State.MOVING||pi!=turn)return;val p=players[pi];capture(p,p.tokens[ti]);movingToken=-1;moveTarget=-1;if(p.tokens.all{it.progress==56}){winner=pi;state=State.GAME_OVER;dice=0;celebrationStart=System.currentTimeMillis();message="${names[pi]} wins!";invalidate();return};val six=dice==6;dice=0;if(six){state=State.WAIT_ROLL;message="Six! ${names[turn]} rolls again"}else finishTurn();invalidate()}
    private fun capture(p:Player,t:Token){if(t.progress !in 0..51)return;val target=(p.color.start+t.progress)%52;if(target in safe)return;for(o in players)if(o.color!=p.color)for(x in o.tokens)if(x.progress in 0..51&&(o.color.start+x.progress)%52==target)x.progress=-1}
    private fun finishTurn(){if(state==State.GAME_OVER)return;dice=0;movingToken=-1;moveTarget=-1;turn=(turn+1)%4;state=State.WAIT_ROLL;message="${names[turn]} • Pass the phone and roll";invalidate()}
    private fun reset(){players.forEach{p->p.tokens.forEach{it.progress=-1}};turn=0;dice=0;state=State.WAIT_ROLL;winner=-1;movingToken=-1;moveTarget=-1;message="${names[0]} • Roll the dice"}
}
