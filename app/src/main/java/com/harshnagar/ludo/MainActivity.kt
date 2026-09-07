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
            setPadding(24, 42, 24, 24)
            setBackgroundColor(Color.rgb(9, 18, 35))
        }
        root.addView(label("LUDO", 38f, Color.WHITE), LinearLayout.LayoutParams(-1, 55))
        root.addView(label("CLASSIC • PASS & PLAY", 13f, Color.rgb(156, 178, 208)), LinearLayout.LayoutParams(-1, 38))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 18)
            setBackgroundColor(Color.WHITE)
        }
        root.addView(card, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(0, 18, 0, 0) })

        val title = label("Game setup", 23f, Color.rgb(25, 34, 48))
        title.gravity = Gravity.LEFT
        card.addView(title, LinearLayout.LayoutParams(-1, 42))
        val sub = label("Choose how many people will play on this phone.", 13f, Color.rgb(100, 110, 125))
        sub.gravity = Gravity.LEFT
        card.addView(sub, LinearLayout.LayoutParams(-1, 42))

        val selector = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL; gravity = Gravity.CENTER }
        for (n in 2..4) {
            val rb = RadioButton(this).apply { text = "$n Players"; textSize = 14f; setTextColor(Color.DKGRAY); id = n }
            selector.addView(rb, RadioGroup.LayoutParams(0, 48, 1f))
        }
        selector.check(4)
        selector.setOnCheckedChangeListener { _, id -> playerCount = id; rebuildNames(card, selector) }
        card.addView(selector)
        rebuildNames(card, selector)

        val start = Button(this).apply {
            text = "START GAME  →"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(28, 105, 210))
            setOnClickListener { startGame() }
        }
        root.addView(start, LinearLayout.LayoutParams(-1, 58).apply { setMargins(0, 18, 0, 0) })
        root.addView(label("100% offline • No account • No internet", 12f, Color.rgb(130, 155, 185)), LinearLayout.LayoutParams(-1, 30))
        setContentView(root)
    }

    private fun rebuildNames(card: LinearLayout, selector: RadioGroup) {
        while (card.childCount > 2) card.removeViewAt(2)
        card.addView(selector, 2)
        fields.clear()
        for (i in 0 until playerCount) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val dot = TextView(this).apply {
                text = "●"; textSize = 23f; gravity = Gravity.CENTER; setTextColor(colors[i])
            }
            row.addView(dot, LinearLayout.LayoutParams(42, 48))
            val edit = EditText(this).apply {
                setSingleLine(true); textSize = 16f; hint = defaults[i]
                setText(prefs.getString("player_$i", defaults[i])); setSelectAllOnFocus(true)
                setPadding(10, 0, 8, 0)
            }
            fields.add(edit)
            row.addView(edit, LinearLayout.LayoutParams(0, 52, 1f))
            card.addView(row)
        }
    }

    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
        setTypeface(null, Typeface.BOLD)
    }

    private fun startGame() {
        val names = fields.mapIndexed { i, field ->
            val name = field.text.toString().trim().ifBlank { defaults[i] }.take(14)
            prefs.edit().putString("player_$i", name).apply()
            name
        }
        if (names.distinct().size != names.size) {
            Toast.makeText(this, "Player names must be different", Toast.LENGTH_SHORT).show(); return
        }
        setContentView(LudoView(this, names))
    }

    override fun onBackPressed() { showMenu() }
}
