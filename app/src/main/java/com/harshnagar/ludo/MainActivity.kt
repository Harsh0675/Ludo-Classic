package com.harshnagar.ludo

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("ludo", MODE_PRIVATE) }
    private val defaults = arrayOf("Red", "Green", "Yellow", "Blue")
    private val colors = intArrayOf(
        Color.rgb(238, 63, 73), Color.rgb(31, 181, 108),
        Color.rgb(248, 190, 38), Color.rgb(55, 126, 232)
    )
    private val fields = ArrayList<EditText>()
    private var playerCount = 4

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        showMenu()
    }

    private fun showMenu() {
        fields.clear()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(22), dp(18), dp(14))
            setBackgroundColor(Color.rgb(7, 18, 35))
        }
        root.addView(label("LUDO", 32f, Color.WHITE), LinearLayout.LayoutParams(-1, dp(46)))
        root.addView(label("CLASSIC  •  PASS & PLAY", 12f, Color.rgb(157, 180, 211)), LinearLayout.LayoutParams(-1, dp(30)))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(14) })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }
        scroll.addView(card, FrameLayout.LayoutParams(-1, -2))

        val title = label("Game Setup", 22f, Color.rgb(24, 35, 50)).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        card.addView(title, LinearLayout.LayoutParams(-1, dp(38)))
        val sub = label("Choose how many people will play on this phone.", 13f, Color.rgb(94, 106, 122)).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
        }
        card.addView(sub, LinearLayout.LayoutParams(-1, dp(34)))

        val selector = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        for (n in 2..4) {
            val rb = RadioButton(this).apply {
                text = "$n Players"
                textSize = 14f
                setTextColor(Color.rgb(45, 53, 65))
                id = n
                includeFontPadding = false
                minHeight = 0
                buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(28, 105, 210))
            }
            selector.addView(rb, RadioGroup.LayoutParams(0, dp(46), 1f))
        }
        selector.check(playerCount)
        selector.setOnCheckedChangeListener { _, id ->
            if (id in 2..4) {
                playerCount = id
                rebuildNames(card, selector)
            }
        }
        card.addView(selector, LinearLayout.LayoutParams(-1, dp(46)))
        rebuildNames(card, selector)

        val start = Button(this).apply {
            text = "START GAME"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setAllCaps(false)
            setBackgroundColor(Color.rgb(25, 105, 215))
            minHeight = 0
            stateListAnimator = null
            setOnClickListener { startGame() }
        }
        root.addView(start, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(14) })
        root.addView(label("100% offline  •  No account  •  No internet", 11f, Color.rgb(132, 157, 187)), LinearLayout.LayoutParams(-1, dp(28)))
        setContentView(root)
    }

    private fun rebuildNames(card: LinearLayout, selector: RadioGroup) {
        while (card.childCount > 2) card.removeViewAt(2)
        card.addView(selector, 2, LinearLayout.LayoutParams(-1, dp(46)))
        fields.clear()
        for (i in 0 until playerCount) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val dot = TextView(this).apply {
                text = "●"
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(colors[i])
                includeFontPadding = false
            }
            row.addView(dot, LinearLayout.LayoutParams(dp(34), dp(52)))
            val edit = EditText(this).apply {
                setSingleLine(true)
                textSize = 16f
                hint = defaults[i]
                setText(prefs.getString("player_$i", defaults[i]))
                setSelectAllOnFocus(true)
                setTextColor(Color.rgb(32, 39, 49))
                setHintTextColor(Color.rgb(145, 153, 164))
                setPadding(dp(8), 0, 0, 0)
                includeFontPadding = false
            }
            fields.add(edit)
            row.addView(edit, LinearLayout.LayoutParams(0, dp(52), 1f))
            card.addView(row)
        }
    }

    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        setTypeface(null, Typeface.BOLD)
        includeFontPadding = false
    }

    private fun startGame() {
        val names = fields.mapIndexed { i, field ->
            val name = field.text.toString().trim().ifBlank { defaults[i] }.take(14)
            prefs.edit().putString("player_$i", name).apply()
            name
        }
        if (names.distinct().size != names.size) {
            Toast.makeText(this, "Player names must be different", Toast.LENGTH_SHORT).show()
            return
        }
        setContentView(LudoView(this, names))
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        showMenu()
    }
}
