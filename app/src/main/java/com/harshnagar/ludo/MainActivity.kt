package com.harshnagar.ludo

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("ludo", MODE_PRIVATE) }
    private val defaultNames = arrayOf("Player 1", "Player 2", "Player 3", "Player 4")
    private val colors = intArrayOf(Color.rgb(16,170,94), Color.rgb(255,198,20), Color.rgb(24,143,224), Color.rgb(242,48,55))
    private val fields = ArrayList<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        showDashboard()
    }

    private fun showDashboard() {
        fields.clear()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 34, 28, 28)
            setBackgroundColor(Color.rgb(4, 31, 68))
        }

        val title = TextView(this).apply {
            text = "HARSH NAGAR • LUDO"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "4 PLAYER • OFFLINE • PASS & PLAY"
            textSize = 13f
            setTextColor(Color.rgb(190, 215, 240))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(Color.WHITE)
        }
        root.addView(card, LinearLayout.LayoutParams(-1, 0, 1f))

        val heading = TextView(this).apply {
            text = "Choose player names"
            textSize = 20f
            setTextColor(Color.rgb(25, 35, 45))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(4, 2, 4, 14)
        }
        card.addView(heading)

        for (i in 0..3) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val label = TextView(this).apply {
                text = "${i + 1}"
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setBackgroundColor(colors[i])
            }
            row.addView(label, LinearLayout.LayoutParams(48, 48))

            val field = EditText(this).apply {
                setSingleLine(true)
                textSize = 16f
                hint = defaultNames[i]
                setText(prefs.getString("player_$i", defaultNames[i]))
                setSelectAllOnFocus(true)
                setPadding(16, 0, 12, 0)
            }
            fields.add(field)
            val fp = LinearLayout.LayoutParams(0, 52, 1f)
            fp.setMargins(12, 0, 0, 10)
            row.addView(field, fp)
            card.addView(row)
        }

        val editHint = TextView(this).apply {
            text = "You can edit these names any time before starting a new game."
            textSize = 12f
            setTextColor(Color.rgb(95, 105, 115))
            setPadding(4, 4, 4, 10)
        }
        card.addView(editHint)

        val start = Button(this).apply {
            text = "START GAME"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(17, 119, 214))
            setOnClickListener { startGame() }
        }
        root.addView(start, LinearLayout.LayoutParams(-1, 56).apply { setMargins(0, 18, 0, 0) })

        val footer = TextView(this).apply {
            text = "No internet required"
            textSize = 12f
            setTextColor(Color.rgb(150, 180, 210))
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
        }
        root.addView(footer, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun startGame() {
        val names = fields.mapIndexed { index, field ->
            val clean = field.text.toString().trim()
            val name = if (clean.isBlank()) defaultNames[index] else clean.take(16)
            prefs.edit().putString("player_$index", name).apply()
            name
        }
        if (names.distinct().size != 4) {
            Toast.makeText(this, "Please use a different name for each player", Toast.LENGTH_SHORT).show()
            return
        }
        setContentView(LudoView(this, names))
    }

    override fun onBackPressed() {
        showDashboard()
    }
}
