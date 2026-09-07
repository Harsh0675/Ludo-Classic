package com.harshnagar.ludo

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class StartDashboard(
    context: Context,
    private val onStartGame: () -> Unit
) : ScrollView(context) {

    private data class PlayerStyle(val color: Int, val label: String)

    private val styles = listOf(
        PlayerStyle(Color.rgb(18, 169, 94), "PLAYER 1"),
        PlayerStyle(Color.rgb(255, 197, 18), "PLAYER 2"),
        PlayerStyle(Color.rgb(30, 143, 224), "PLAYER 3"),
        PlayerStyle(Color.rgb(239, 52, 58), "PLAYER 4")
    )

    private val names = MutableList(4) { "Player ${it + 1}" }
    private val prefs = context.getSharedPreferences("ludo_players", Context.MODE_PRIVATE)
    private val nameViews = mutableListOf<TextView>()
    private val avatarViews = mutableListOf<TextView>()

    init {
        loadNames()
        setFillViewport(true)
        setBackgroundColor(Color.rgb(5, 31, 64))
        addView(buildContent(context))
    }

    private fun loadNames() {
        for (i in 0..3) {
            val saved = prefs.getString("player_$i", null)?.trim()
            if (!saved.isNullOrEmpty()) names[i] = saved
        }
    }

    private fun saveNames() {
        val editor = prefs.edit()
        names.forEachIndexed { index, name -> editor.putString("player_$index", name) }
        editor.apply()
    }

    private fun buildContent(context: Context): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(28), dp(18), dp(30))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(7, 72, 136), Color.rgb(2, 25, 58))
            )
        }

        val title = TextView(context).apply {
            text = "HARSH NAGAR  •  LUDO"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(5) })

        val subtitle = TextView(context).apply {
            text = "4 PLAYER OFFLINE"
            setTextColor(Color.rgb(190, 218, 242))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(22) })

        val cardGrid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        for (row in 0..1) {
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (col in 0..1) {
                val index = row * 2 + col
                rowLayout.addView(playerCard(context, index), LinearLayout.LayoutParams(0, dp(165), 1f).apply {
                    if (col == 0) rightMargin = dp(7) else leftMargin = dp(7)
                    bottomMargin = dp(12)
                })
            }
            cardGrid.addView(rowLayout, LinearLayout.LayoutParams(-1, -2))
        }
        root.addView(cardGrid, LinearLayout.LayoutParams(-1, -2))

        val hint = TextView(context).apply {
            text = "Tap EDIT to choose a name for each player"
            setTextColor(Color.rgb(198, 215, 232))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(16))
        }
        root.addView(hint, LinearLayout.LayoutParams(-1, -2))

        val start = Button(context).apply {
            text = "START OFFLINE GAME"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = rounded(Color.rgb(17, 119, 214), 18)
            setPadding(dp(18), 0, dp(18), 0)
            setOnClickListener {
                saveNames()
                Toast.makeText(context, "Pass the device between players", Toast.LENGTH_SHORT).show()
                onStartGame()
            }
        }
        root.addView(start, LinearLayout.LayoutParams(-1, dp(58)))

        val footer = TextView(context).apply {
            text = "No Wi-Fi • No internet • 4 people on one device"
            setTextColor(Color.rgb(160, 187, 210))
            textSize = 11f
            gravity = Gravity.CENTER
        }
        root.addView(footer, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        return root
    }

    private fun playerCard(context: Context, index: Int): View {
        val style = styles[index]
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8), dp(10), dp(8), dp(8))
            background = rounded(Color.argb(238, 255, 255, 255), 18)
            elevation = dp(3).toFloat()
        }

        val avatar = TextView(context).apply {
            gravity = Gravity.CENTER
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = circle(style.color)
        }
        avatarViews.add(avatar)
        card.addView(avatar, LinearLayout.LayoutParams(dp(48), dp(48)).apply { bottomMargin = dp(7) })

        val role = TextView(context).apply {
            text = style.label
            setTextColor(style.color)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        card.addView(role, LinearLayout.LayoutParams(-1, -2))

        val name = TextView(context).apply {
            text = names[index]
            setTextColor(Color.rgb(35, 45, 55))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
        }
        nameViews.add(name)
        card.addView(name, LinearLayout.LayoutParams(-1, dp(28)))

        val edit = Button(context).apply {
            text = "EDIT"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(style.color)
            isAllCaps = false
            background = rounded(Color.argb(28, Color.red(style.color), Color.green(style.color), Color.blue(style.color)), 12)
            setOnClickListener { showNameEditor(context, index) }
        }
        card.addView(edit, LinearLayout.LayoutParams(dp(90), dp(34)).apply { topMargin = dp(3) })

        return card
    }

    private fun showNameEditor(context: Context, index: Int) {
        val input = android.widget.EditText(context).apply {
            setText(names[index])
            setSelection(length())
            hint = "Player name"
            maxLines = 1
            setPadding(dp(12), 0, dp(12), 0)
        }
        val box = LinearLayout(context).apply {
            setPadding(dp(20), dp(4), dp(20), 0)
            addView(input, LinearLayout.LayoutParams(-1, dp(52)))
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Edit ${styles[index].label}")
            .setView(box)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text.toString().trim().replace("\\s+".toRegex(), " ")
                if (value.isEmpty()) {
                    input.error = "Enter a name"
                } else {
                    names[index] = value.take(16)
                    nameViews[index].text = names[index]
                    avatarViews[index].text = initials(names[index])
                    saveNames()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun initials(name: String): String = name.trim().split(" ").filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun circle(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
