package com.reelmonster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var tvCount: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvWeek: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvSecs: TextView
    private lateinit var chart: BarChart
    private lateinit var rg: RadioGroup

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> runOnUiThread { refresh() } }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(18).toFloat() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
    }

    private fun text(t: String, sp: Float, color: Int = Color.rgb(40, 30, 60), bold: Boolean = false) = TextView(this).apply {
        text = t; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Store.init(this)

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(40), dp(18), dp(24)) }
        col.addView(text("Reel Monster", 28f, bold = true))
        col.addView(text("It floats on Instagram & YouTube and counts your reels.", 14f, Color.GRAY).apply { setPadding(0, 0, 0, dp(14)) })

        // Today
        val today = card()
        today.addView(text("TODAY", 12f, Color.GRAY, true))
        tvCount = text("0", 64f, Color.rgb(60, 160, 80), true)
        tvTime = text("", 16f)
        tvWeek = text("", 13f, Color.GRAY)
        today.addView(tvCount); today.addView(tvTime); today.addView(tvWeek)
        col.addView(today)

        // Last 7 days
        val week = card()
        week.addView(text("LAST 7 DAYS", 12f, Color.GRAY, true))
        chart = BarChart(this)
        week.addView(chart, LinearLayout.LayoutParams(-1, dp(170)))
        col.addView(week)

        // Level
        val lvl = card()
        lvl.addView(text("LEVEL", 12f, Color.GRAY, true))
        rg = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val r1 = RadioButton(this).apply { id = 1; text = "Level 1 - Manual: tap the monster after each reel" }
        val r2 = RadioButton(this).apply { id = 2; text = "Level 2 - Automatic: it counts new reels itself" }
        rg.addView(r1); rg.addView(r2)
        rg.check(Store.mode)
        rg.setOnCheckedChangeListener { _, id -> Store.mode = id }
        lvl.addView(rg)
        lvl.addView(text("Long-press the monster to undo one. Drag it anywhere.", 12f, Color.GRAY))
        col.addView(lvl)

        // Time per reel
        val tp = card()
        tp.addView(text("TIME PER REEL (for 'time lost')", 12f, Color.GRAY, true))
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val minus = Button(this).apply { text = "-"; setOnClickListener { Store.secsPerReel -= 5 } }
        val plus = Button(this).apply { text = "+"; setOnClickListener { Store.secsPerReel += 5 } }
        tvSecs = text("", 16f).apply { gravity = Gravity.CENTER }
        row.addView(minus, LinearLayout.LayoutParams(dp(56), -2))
        row.addView(tvSecs, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(plus, LinearLayout.LayoutParams(dp(56), -2))
        tp.addView(row)
        col.addView(tp)

        // Monster on/off
        val st = card()
        tvStatus = text("", 15f, bold = true)
        st.addView(tvStatus)
        st.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        st.addView(text(
            "Turn on 'Reel Monster' there. If the switch is greyed out (Android 13+): Settings > Apps > Reel Monster > ⋮ > Allow restricted settings, then try again.",
            12f, Color.GRAY))
        col.addView(st)

        setContentView(ScrollView(this).apply { setBackgroundColor(Color.rgb(246, 243, 255)); addView(col) })
    }

    override fun onResume() { super.onResume(); Store.register(listener); refresh() }
    override fun onPause() { super.onPause(); Store.unregister(listener) }

    private fun fmt(sec: Int): String {
        val m = sec / 60
        return if (m >= 60) "${m / 60}h ${m % 60}m" else if (m >= 1) "$m min" else "$sec s"
    }

    private fun refresh() {
        val n = Store.today()
        val secs = Store.secsPerReel
        tvCount.text = n.toString()
        tvTime.text = "reels watched  -  about ${fmt(n * secs)} lost"
        val days = Store.last7()
        tvWeek.text = "This week: ${days.sumOf { it.second }} reels, about ${fmt(days.sumOf { it.second } * secs)}"
        chart.data = days
        chart.invalidate()
        tvSecs.text = "$secs seconds per reel"
        if (rg.checkedRadioButtonId != Store.mode) rg.check(Store.mode)
        val on = serviceOn()
        tvStatus.text = if (on) "Monster is ON - open Instagram or YouTube" else "Monster is OFF - enable it in Accessibility"
        tvStatus.setTextColor(if (on) Color.rgb(40, 150, 70) else Color.rgb(200, 60, 50))
    }

    private fun serviceOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return s.contains(packageName) && s.contains("ReelService")
    }

    class BarChart(c: Context) : View(c) {
        var data: List<Pair<String, Int>> = emptyList()
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(cv: Canvas) {
            if (data.isEmpty()) return
            val d = resources.displayMetrics.density
            val max = (data.maxOf { it.second }).coerceAtLeast(1)
            val slot = width / data.size.toFloat()
            val top = 20 * d; val bottom = height - 22 * d
            p.textAlign = Paint.Align.CENTER; p.textSize = 12 * d
            data.forEachIndexed { i, (label, v) ->
                val cx = slot * i + slot / 2
                val barH = (bottom - top) * v / max
                p.color = if (i == data.lastIndex) Color.rgb(60, 160, 80) else Color.rgb(190, 180, 230)
                cv.drawRoundRect(RectF(cx - slot * .3f, bottom - barH, cx + slot * .3f, bottom), 6 * d, 6 * d, p)
                p.color = Color.rgb(40, 30, 60)
                cv.drawText(v.toString(), cx, bottom - barH - 4 * d, p)
                p.color = Color.GRAY
                cv.drawText(label, cx, height - 5 * d, p)
            }
        }
    }
}
